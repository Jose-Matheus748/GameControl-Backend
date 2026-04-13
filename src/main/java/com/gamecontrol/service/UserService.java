package com.gamecontrol.service;

import com.gamecontrol.dto.AuthResponse;
import com.gamecontrol.dto.CreateUserRequest;
import com.gamecontrol.dto.LoginRequest;
import com.gamecontrol.dto.UserDTO;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Service
public class UserService {

    private final Firestore firestore;
    private final String nomeColecaoUsuarios;

    public UserService(Firestore firestore, @Value("${firebase.collection.users}") String nomeColecaoUsuarios) {
        this.firestore = firestore;
        this.nomeColecaoUsuarios = nomeColecaoUsuarios;
    }

    public List<UserDTO> listarUsuarios() {
        try {
            QuerySnapshot resultado = firestore.collection(nomeColecaoUsuarios).get().get();
            List<UserDTO> usuarios = new ArrayList<>();
            for (QueryDocumentSnapshot documento : resultado.getDocuments()) {
                usuarios.add(UserFirestoreMapper.paraDto(documento));
            }
            return usuarios;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Operação no Firestore interrompida.", e);
        } catch (ExecutionException e) {
            Throwable causa = e.getCause();
            if (causa instanceof RuntimeException re) {
                throw re;
            }
            throw new IllegalStateException(
                    causa != null ? causa.getMessage() : "Falha ao acessar o Firestore.",
                    e
            );
        }
    }

    /**
     * Cria usuário no Firestore. A senha é gravada como enviada — em produção use hash (ex.: BCrypt).
     */
    public UserDTO cadastrarUsuario(CreateUserRequest requisicao) {
        try {
            requisicao.setEmail(requisicao.getEmail().trim().toLowerCase(Locale.ROOT));
            if (buscarDocumentoPorEmail(requisicao.getEmail()).isPresent()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado.");
            }
            Map<String, Object> dados = UserFirestoreMapper.paraDocumento(requisicao);
            DocumentReference referencia = firestore.collection(nomeColecaoUsuarios).document();
            referencia.set(dados).get();
            DocumentSnapshot salvo = referencia.get().get();
            return UserFirestoreMapper.paraDto(salvo);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Operação no Firestore interrompida.", e);
        } catch (ExecutionException e) {
            Throwable causa = e.getCause();
            if (causa instanceof RuntimeException re) {
                throw re;
            }
            throw new IllegalStateException(
                    causa != null ? causa.getMessage() : "Falha ao acessar o Firestore.",
                    e
            );
        }
    }

    /**
     * Login: compara senha em texto plano com o valor no Firestore (sem hash).
     * O token é apenas um identificador opaco (UUID), sem JWT.
     */
    public AuthResponse login(LoginRequest requisicao) {
        try {
            Optional<QueryDocumentSnapshot> documentoOpt = buscarDocumentoPorEmail(requisicao.getEmail());
            if (documentoOpt.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas.");
            }
            QueryDocumentSnapshot documento = documentoOpt.get();
            String senhaArmazenada = lerSenhaComoTexto(documento);
            if (senhaArmazenada == null || !senhaArmazenada.equals(requisicao.getPassword())) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas.");
            }
            UserDTO usuario = UserFirestoreMapper.paraDto(documento);
            String token = UUID.randomUUID().toString();
            return new AuthResponse(usuario, token);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Operação no Firestore interrompida.", e);
        } catch (ExecutionException e) {
            Throwable causa = e.getCause();
            if (causa instanceof RuntimeException re) {
                throw re;
            }
            throw new IllegalStateException(
                    causa != null ? causa.getMessage() : "Falha ao acessar o Firestore.",
                    e
            );
        }
    }

    /**
     * Firestore compara strings com case-sensitive. Aceita e-mail em qualquer capitalização
     * e documentos antigos sem o campo {@code emailLower}.
     */
    private Optional<QueryDocumentSnapshot> buscarDocumentoPorEmail(String emailBruto)
            throws ExecutionException, InterruptedException {
        if (emailBruto == null || emailBruto.isBlank()) {
            return Optional.empty();
        }
        String trimmed = emailBruto.trim();
        String normalizado = trimmed.toLowerCase(Locale.ROOT);

        QuerySnapshot resultado = firestore.collection(nomeColecaoUsuarios)
                .whereEqualTo("email", normalizado)
                .limit(1)
                .get()
                .get();
        if (!resultado.isEmpty()) {
            return Optional.of(resultado.getDocuments().get(0));
        }

        if (!normalizado.equals(trimmed)) {
            resultado = firestore.collection(nomeColecaoUsuarios)
                    .whereEqualTo("email", trimmed)
                    .limit(1)
                    .get()
                    .get();
            if (!resultado.isEmpty()) {
                return Optional.of(resultado.getDocuments().get(0));
            }
        }

        resultado = firestore.collection(nomeColecaoUsuarios)
                .whereEqualTo("emailLower", normalizado)
                .limit(1)
                .get()
                .get();
        if (!resultado.isEmpty()) {
            return Optional.of(resultado.getDocuments().get(0));
        }

        QuerySnapshot todos = firestore.collection(nomeColecaoUsuarios).get().get();
        for (QueryDocumentSnapshot doc : todos.getDocuments()) {
            String emailDoc = doc.getString("email");
            if (emailDoc != null && emailDoc.trim().equalsIgnoreCase(trimmed)) {
                return Optional.of(doc);
            }
        }
        return Optional.empty();
    }

    private static String lerSenhaComoTexto(QueryDocumentSnapshot documento) {
        Object valor = documento.get("password");
        if (valor == null) {
            return null;
        }
        return valor instanceof String s ? s : String.valueOf(valor);
    }

    public UserDTO buscarUsuarioPorId(String id) {
        try {
            DocumentSnapshot documento = firestore.collection(nomeColecaoUsuarios)
                    .document(id)
                    .get()
                    .get();
            if (!documento.exists()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado.");
            }
            return UserFirestoreMapper.paraDto(documento);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Operação no Firestore interrompida.", e);
        } catch (ExecutionException e) {
            Throwable causa = e.getCause();
            if (causa instanceof RuntimeException re) {
                throw re;
            }
            throw new IllegalStateException(
                    causa != null ? causa.getMessage() : "Falha ao acessar o Firestore.",
                    e
            );
        }
    }

    public UserDTO atualizarUsuario(String id, UserDTO dadosAtualizados) {
        try {
            DocumentReference referencia = firestore
                    .collection(nomeColecaoUsuarios)
                    .document(id);

            DocumentSnapshot documento = referencia.get().get();

            if (!documento.exists()) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Usuário não encontrado."
                );
            }

            referencia.update(
                    "username", dadosAtualizados.getUsername(),
                    "bio", dadosAtualizados.getBio(),
                    "country", dadosAtualizados.getCountry()
            ).get();

            DocumentSnapshot atualizado = referencia.get().get();

            return UserFirestoreMapper.paraDto(atualizado);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Operação interrompida.", e);

        } catch (ExecutionException e) {
            throw new IllegalStateException("Erro ao atualizar usuário.", e);
        }
    }
}
