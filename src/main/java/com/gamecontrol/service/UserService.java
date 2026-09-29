package com.gamecontrol.service;

import com.gamecontrol.dto.AuthResponse;
import com.gamecontrol.dto.request.ChangePasswordRequest;
import com.gamecontrol.dto.request.CreateUserRequest;
import com.gamecontrol.dto.request.LoginRequest;
import com.gamecontrol.dto.UserDTO;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteBatch;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final PasswordEncoder passwordEncoder;

    public UserService(
            Firestore firestore,
            PasswordEncoder passwordEncoder,
            @Value("${firebase.collection.users}") String nomeColecaoUsuarios
    ) {
        this.firestore = firestore;
        this.passwordEncoder = passwordEncoder;
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
     * Cria usuário no Firestore com senha criptografada via BCrypt.
     */
    public UserDTO cadastrarUsuario(CreateUserRequest requisicao) {
        try {
            requisicao.setEmail(
                    requisicao.getEmail()
                            .trim()
                            .toLowerCase(Locale.ROOT)
            );

            if (buscarDocumentoPorEmail(requisicao.getEmail()).isPresent()) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "E-mail já cadastrado."
                );
            }

            String senhaHash =
                    passwordEncoder.encode(requisicao.getPassword());

            Map<String, Object> dados =
                    UserFirestoreMapper.paraDocumento(
                            requisicao,
                            senhaHash
                    );

            DocumentReference referencia =
                    firestore.collection(nomeColecaoUsuarios).document();

            referencia.set(dados).get();

            return UserFirestoreMapper.paraDto(
                    referencia.get().get()
            );

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Login: compara senha em texto plano enviada pelo usuário
     * com a senha criptografada armazenada no Firestore usando BCrypt.
     */
    public AuthResponse login(LoginRequest requisicao) {
        try {
            Optional<QueryDocumentSnapshot> documentoOpt = buscarDocumentoPorEmail(requisicao.getEmail());
            if (documentoOpt.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas.");
            }
            QueryDocumentSnapshot documento = documentoOpt.get();
            String senhaArmazenada = lerSenhaComoTexto(documento);
            if (senhaArmazenada == null ||
                    !passwordEncoder.matches(
                            requisicao.getPassword(),
                            senhaArmazenada
                    )) {
                throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Credenciais inválidas."
                );
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

    private static String lerSenhaComoTexto(DocumentSnapshot documento) {
        Object valor = documento.get("password");
        if (valor == null) {
            return null;
        }
        return valor instanceof String s ? s : String.valueOf(valor);
    }

    /**
     * Altera a senha do usuário após validar a senha atual.
     * <p>Tratamentos: usuário inexistente (404), senha atual incorreta (401),
     * nova senha igual à atual (400) e confirmação divergente quando enviada (400).
     */
    public void alterarSenha(String id, ChangePasswordRequest requisicao) {
        if (requisicao.getConfirmNewPassword() != null
                && !requisicao.getConfirmNewPassword().isBlank()
                && !requisicao.getNewPassword().equals(requisicao.getConfirmNewPassword())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A confirmação da nova senha não coincide."
            );
        }

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

            String senhaArmazenada = lerSenhaComoTexto(documento);
            if (senhaArmazenada == null
                    || !passwordEncoder.matches(requisicao.getCurrentPassword(), senhaArmazenada)) {
                throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Senha atual incorreta."
                );
            }

            if (passwordEncoder.matches(requisicao.getNewPassword(), senhaArmazenada)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "A nova senha deve ser diferente da senha atual."
                );
            }

            String novaSenhaHash = passwordEncoder.encode(requisicao.getNewPassword());
            referencia.update("password", novaSenhaHash).get();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Operação interrompida.", e);
        } catch (ExecutionException e) {
            Throwable causa = e.getCause();
            if (causa instanceof ResponseStatusException rse) {
                throw rse;
            }
            throw new IllegalStateException("Erro ao alterar senha.", e);
        }
    }

    /**
     * Remove o usuário e limpa as referências dele nas listas
     * {@code following}/{@code followers} de quem seguia ou era seguido por ele.
     */
    public void deletarUsuario(String id) {
        try {
            DocumentReference referencia = firestore.collection(nomeColecaoUsuarios).document(id);
            DocumentSnapshot documento = referencia.get().get();

            if (!documento.exists()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado.");
            }

            QuerySnapshot quemSeguiaEsteUsuario = firestore.collection(nomeColecaoUsuarios)
                    .whereArrayContains("following", id)
                    .get()
                    .get();

            QuerySnapshot quemEraSeguidoPorEsteUsuario = firestore.collection(nomeColecaoUsuarios)
                    .whereArrayContains("followers", id)
                    .get()
                    .get();

            WriteBatch batch = firestore.batch();

            for (QueryDocumentSnapshot doc : quemSeguiaEsteUsuario.getDocuments()) {
                batch.update(doc.getReference(), "following", FieldValue.arrayRemove(id));
            }
            for (QueryDocumentSnapshot doc : quemEraSeguidoPorEsteUsuario.getDocuments()) {
                batch.update(doc.getReference(), "followers", FieldValue.arrayRemove(id));
            }

            batch.delete(referencia);
            batch.commit().get();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Operação interrompida.", e);
        } catch (ExecutionException e) {
            Throwable causa = e.getCause();
            if (causa instanceof ResponseStatusException rse) {
                throw rse;
            }
            throw new IllegalStateException("Erro ao excluir usuário.", e);
        }
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

    public UserDTO atualizarFotoPerfil(String id, String profilePictureUrl) {
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

            referencia.update("profilePictureUrl", profilePictureUrl).get();

            DocumentSnapshot atualizado = referencia.get().get();
            return UserFirestoreMapper.paraDto(atualizado);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Operação interrompida.", e);

        } catch (ExecutionException e) {
            throw new IllegalStateException("Erro ao atualizar foto de perfil.", e);
        }
    }

    /**
     * {@code followerId} passa a seguir {@code followedId}: atualiza arrays {@code following} / {@code followers} nos docs de usuário.
     */
    public void followUser(String followerId, String followedId) {
        if (followerId == null || followedId == null
                || followerId.isBlank() || followedId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ids inválidos.");
        }
        String f = followerId.trim();
        String d = followedId.trim();
        if (f.equals(d)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não é possível seguir a si mesmo.");
        }
        try {
            DocumentReference refFollower = firestore.collection(nomeColecaoUsuarios).document(f);
            DocumentReference refFollowed = firestore.collection(nomeColecaoUsuarios).document(d);
            DocumentSnapshot snapFollower = refFollower.get().get();
            DocumentSnapshot snapFollowed = refFollowed.get().get();
            if (!snapFollower.exists() || !snapFollowed.exists()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado.");
            }
            WriteBatch batch = firestore.batch();
            batch.update(refFollower, "following", FieldValue.arrayUnion(d));
            batch.update(refFollowed, "followers", FieldValue.arrayUnion(f));
            batch.commit().get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Operação interrompida.", e);
        } catch (ExecutionException e) {
            Throwable causa = e.getCause();
            if (causa instanceof ResponseStatusException rse) {
                throw rse;
            }
            throw new IllegalStateException("Erro ao seguir usuário.", e);
        }
    }

    public void unfollowUser(String followerId, String followedId) {
        if (followerId == null || followedId == null
                || followerId.isBlank() || followedId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ids inválidos.");
        }
        String f = followerId.trim();
        String d = followedId.trim();
        if (f.equals(d)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ids inválidos.");
        }
        try {
            DocumentReference refFollower = firestore.collection(nomeColecaoUsuarios).document(f);
            DocumentReference refFollowed = firestore.collection(nomeColecaoUsuarios).document(d);
            DocumentSnapshot snapFollower = refFollower.get().get();
            DocumentSnapshot snapFollowed = refFollowed.get().get();
            if (!snapFollower.exists() || !snapFollowed.exists()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado.");
            }
            WriteBatch batch = firestore.batch();
            batch.update(refFollower, "following", FieldValue.arrayRemove(d));
            batch.update(refFollowed, "followers", FieldValue.arrayRemove(f));
            batch.commit().get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Operação interrompida.", e);
        } catch (ExecutionException e) {
            Throwable causa = e.getCause();
            if (causa instanceof ResponseStatusException rse) {
                throw rse;
            }
            throw new IllegalStateException("Erro ao deixar de seguir.", e);
        }
    }
}
