package com.gamecontrol.service;

import com.gamecontrol.dto.request.ChangePasswordRequest;
import com.gamecontrol.dto.request.CreateUserProfileRequest;
import com.gamecontrol.dto.NotificationPreferencesDTO;
import com.gamecontrol.dto.UserDTO;
import com.gamecontrol.enums.FollowResult;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteBatch;
import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final Firestore firestore;
    private final String nomeColecaoUsuarios;
    private final NotificationService notificationService;
    private final FollowRequestService followRequestService;
    private final FirebaseAuth firebaseAuth;

    public UserService(
            Firestore firestore,
            NotificationService notificationService,
            FollowRequestService followRequestService,
            FirebaseAuth firebaseAuth,
            @Value("${firebase.collection.users}") String nomeColecaoUsuarios
    ) {
        this.firestore = firestore;
        this.notificationService = notificationService;
        this.followRequestService = followRequestService;
        this.firebaseAuth = firebaseAuth;
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
     * Cria somente o perfil no Firestore para uma conta que já existe no Firebase Auth.
     * O UID vem do token validado no filtro; o e-mail é consultado no Firebase Admin SDK.
     * DocumentReference.create é atômico e impede que uma chamada repetida sobrescreva o perfil.
     */
    public UserDTO cadastrarUsuario(CreateUserProfileRequest requisicao, String uidAutenticado) {
        if (uidAutenticado == null || uidAutenticado.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado.");
        }

        final UserRecord usuarioFirebase;
        try {
            usuarioFirebase = firebaseAuth.getUser(uidAutenticado);
        } catch (FirebaseAuthException e) {
            if (e.getAuthErrorCode() == AuthErrorCode.USER_NOT_FOUND) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Conta autenticada não encontrada.");
            }
            throw new IllegalStateException("Não foi possível consultar a conta no Firebase Authentication.", e);
        }

        String email = usuarioFirebase.getEmail();
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A conta do Firebase Authentication não possui e-mail associado."
            );
        }

        DocumentReference referencia = firestore.collection(nomeColecaoUsuarios).document(uidAutenticado);
        Map<String, Object> dados = UserFirestoreMapper.paraDocumento(requisicao, email);

        try {
            referencia.create(dados).get();
            return UserFirestoreMapper.paraDto(referencia.get().get());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Criação do perfil interrompida.", e);
        } catch (ExecutionException e) {
            Throwable causa = e.getCause();
            if (documentoJaExiste(causa)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "O perfil deste usuário já foi criado.");
            }
            if (causa instanceof ResponseStatusException rse) {
                throw rse;
            }
            throw new IllegalStateException("Erro ao criar o perfil do usuário no Firestore.", e);
        }
    }

    private boolean documentoJaExiste(Throwable erro) {
        Throwable atual = erro;
        while (atual != null) {
            if (atual instanceof com.google.api.gax.rpc.AlreadyExistsException) {
                return true;
            }
            atual = atual.getCause();
        }
        return false;
    }

    /**
     * Atualiza a senha via Firebase Admin SDK. A rota deve ser restrita ao UID autenticado.
     * Para a experiência final do app, recomenda-se usar updatePassword() no Firebase SDK do cliente,
     * que também aplica a exigência de login recente quando necessário.
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
            firebaseAuth.updateUser(new UserRecord.UpdateRequest(id).setPassword(requisicao.getNewPassword()));
        } catch (FirebaseAuthException e) {
            if (e.getAuthErrorCode() == AuthErrorCode.USER_NOT_FOUND) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado.");
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não foi possível alterar a senha.", e);
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

            try {
                followRequestService.removerTodosDoUsuario(id);
            } catch (RuntimeException e) {
                log.warn("Não foi possível limpar os pedidos para seguir de {}: {}", id, e.getMessage());
            }

            try {
                firebaseAuth.deleteUser(id);
            } catch (FirebaseAuthException e) {
                if (e.getAuthErrorCode() != AuthErrorCode.USER_NOT_FOUND) {
                    throw new IllegalStateException("Erro ao excluir usuário do Firebase Auth.", e);
                }
            }

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

            // Só altera o que veio na requisição: assim, atualizar apenas as
            // preferências não apaga nome, bio nem país.
            Map<String, Object> campos = new LinkedHashMap<>();
            if (dadosAtualizados.getUsername() != null) {
                campos.put("username", dadosAtualizados.getUsername());
            }
            if (dadosAtualizados.getBio() != null) {
                campos.put("bio", dadosAtualizados.getBio());
            }
            if (dadosAtualizados.getCountry() != null) {
                campos.put("country", dadosAtualizados.getCountry());
            }
            NotificationPreferencesDTO preferencias = dadosAtualizados.getNotificationPreferences();
            if (preferencias != null) {
                String base = UserFirestoreMapper.CAMPO_PREFERENCIAS_NOTIFICACAO + ".";
                if (preferencias.getLikes() != null) {
                    campos.put(base + "likes", preferencias.getLikes());
                }
                if (preferencias.getComments() != null) {
                    campos.put(base + "comments", preferencias.getComments());
                }
                if (preferencias.getFollowers() != null) {
                    campos.put(base + "followers", preferencias.getFollowers());
                }
            }
            Boolean novoPrivado = dadosAtualizados.getIsPrivate();
            if (novoPrivado != null) {
                campos.put(UserFirestoreMapper.CAMPO_PERFIL_PRIVADO, novoPrivado);
            }
            boolean eraPrivado = UserFirestoreMapper.perfilPrivado(documento);

            if (!campos.isEmpty()) {
                referencia.update(campos).get();
            }

            // Perfil que deixa de ser privado: os pedidos pendentes são aceitos.
            if (eraPrivado && Boolean.FALSE.equals(novoPrivado)) {
                followRequestService.aceitarTodosPendentes(id);
            }

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
    public FollowResult followUser(String followerId, String followedId) {
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
            // Evita notificar de novo se o usuário já seguia (a chamada é idempotente).
            List<String> seguidoresAtuais = UserFirestoreMapper.lerListaIds(snapFollowed, "followers");
            boolean jaSeguia = seguidoresAtuais.contains(f);

            // Perfil privado: em vez de seguir direto, fica um pedido pendente de aprovação.
            if (!jaSeguia && UserFirestoreMapper.perfilPrivado(snapFollowed)) {
                followRequestService.solicitar(f, snapFollower.getString("username"), d);
                return FollowResult.REQUESTED;
            }

            WriteBatch batch = firestore.batch();
            batch.update(refFollower, "following", FieldValue.arrayUnion(d));
            batch.update(refFollowed, "followers", FieldValue.arrayUnion(f));
            batch.commit().get();

            if (!jaSeguia) {
                notificationService.notificarNovoSeguidor(f, snapFollower.getString("username"), d);
            }
            return FollowResult.FOLLOWING;
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

            // Se havia um pedido pendente, deixar de seguir também cancela o pedido.
            followRequestService.cancelarPendente(f, d);
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