package com.gamecontrol.service;

import com.gamecontrol.dto.AuthResponse;
import com.gamecontrol.dto.request.ChangePasswordRequest;
import com.gamecontrol.dto.request.CreateUserRequest;
import com.gamecontrol.dto.request.LoginRequest;
import com.gamecontrol.dto.request.RefreshTokenRequest;
import com.gamecontrol.dto.UserDTO;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

@Service
public class UserService {

    private final Firestore firestore;
    private final String nomeColecaoUsuarios;
    private final NotificationService notificationService;
    private final FirebaseAuth firebaseAuth;
    private final FirebaseAuthRestClient authRestClient;

    public UserService(
            Firestore firestore,
            NotificationService notificationService,
            FirebaseAuth firebaseAuth,
            FirebaseAuthRestClient authRestClient,
            @Value("${firebase.collection.users}") String nomeColecaoUsuarios
    ) {
        this.firestore = firestore;
        this.notificationService = notificationService;
        this.firebaseAuth = firebaseAuth;
        this.authRestClient = authRestClient;
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

    public UserDTO cadastrarUsuario(CreateUserRequest requisicao) {
        String email = requisicao.getEmail().trim().toLowerCase(Locale.ROOT);
        requisicao.setEmail(email);

        try {
            UserRecord usuarioAuth = firebaseAuth.createUser(new UserRecord.CreateRequest()
                    .setEmail(email)
                    .setPassword(requisicao.getPassword())
                    .setDisplayName(requisicao.getUsername().trim()));

            DocumentReference referencia = firestore.collection(nomeColecaoUsuarios).document(usuarioAuth.getUid());
            try {
                referencia.set(UserFirestoreMapper.paraDocumento(requisicao)).get();
            } catch (InterruptedException | ExecutionException e) {
                removerDoAuthSilenciosamente(usuarioAuth.getUid());
                throw e;
            }

            return UserFirestoreMapper.paraDto(referencia.get().get());

        } catch (FirebaseAuthException e) {
            if (e.getAuthErrorCode() == AuthErrorCode.EMAIL_ALREADY_EXISTS) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado.");
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Operação no Firestore interrompida.", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Erro ao cadastrar usuário.", e);
        }
    }

    public AuthResponse login(LoginRequest requisicao) {
        FirebaseAuthRestClient.SessaoFirebase sessao = authRestClient.entrarComEmailESenha(
                requisicao.getEmail().trim().toLowerCase(Locale.ROOT),
                requisicao.getPassword()
        );
        return new AuthResponse(
                buscarUsuarioPorId(sessao.uid()),
                sessao.idToken(),
                sessao.refreshToken(),
                sessao.expiresIn()
        );
    }

    public AuthResponse renovarToken(RefreshTokenRequest requisicao) {
        FirebaseAuthRestClient.SessaoFirebase sessao = authRestClient.renovarToken(requisicao.getRefreshToken());
        return new AuthResponse(
                buscarUsuarioPorId(sessao.uid()),
                sessao.idToken(),
                sessao.refreshToken(),
                sessao.expiresIn()
        );
    }

    private void removerDoAuthSilenciosamente(String uid) {
        try {
            firebaseAuth.deleteUser(uid);
        } catch (FirebaseAuthException ignorada) {
        }
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

        if (requisicao.getNewPassword().equals(requisicao.getCurrentPassword())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A nova senha deve ser diferente da senha atual."
            );
        }

        UserDTO usuario = buscarUsuarioPorId(id);

        FirebaseAuthRestClient.SessaoFirebase sessao;
        try {
            sessao = authRestClient.entrarComEmailESenha(usuario.getEmail(), requisicao.getCurrentPassword());
        } catch (ResponseStatusException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Senha atual incorreta.");
            }
            throw e;
        }

        if (!id.equals(sessao.uid())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuário não corresponde às credenciais.");
        }

        try {
            firebaseAuth.updateUser(new UserRecord.UpdateRequest(id).setPassword(requisicao.getNewPassword()));
        } catch (FirebaseAuthException e) {
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
            // Evita notificar de novo se o usuário já seguia (a chamada é idempotente).
            List<String> seguidoresAtuais = UserFirestoreMapper.lerListaIds(snapFollowed, "followers");
            boolean jaSeguia = seguidoresAtuais.contains(f);

            WriteBatch batch = firestore.batch();
            batch.update(refFollower, "following", FieldValue.arrayUnion(d));
            batch.update(refFollowed, "followers", FieldValue.arrayUnion(f));
            batch.commit().get();

            if (!jaSeguia) {
                notificationService.notificarNovoSeguidor(f, snapFollower.getString("username"), d);
            }
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
