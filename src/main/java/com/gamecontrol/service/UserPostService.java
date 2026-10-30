package com.gamecontrol.service;

import com.gamecontrol.dto.UserPostDTO;
import com.gamecontrol.dto.request.CreateUserPostRequest;
import com.gamecontrol.dto.request.UpdateUserPostRequest;
import com.google.cloud.firestore.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
public class UserPostService {

    private static final Logger log = LoggerFactory.getLogger(UserPostService.class);

    private final Firestore firestore;
    private final NotificationService notificationService;
    private final ProfilePrivacyService privacyService;
    private final String postsCollection;
    private final String usersCollection;

    public UserPostService(
            Firestore firestore,
            NotificationService notificationService,
            ProfilePrivacyService privacyService,
            @Value("${firebase.collection.posts}") String postsCollection,
            @Value("${firebase.collection.users}") String usersCollection
    ) {
        this.firestore = firestore;
        this.notificationService = notificationService;
        this.privacyService = privacyService;
        this.postsCollection = postsCollection;
        this.usersCollection = usersCollection;
    }

    public UserPostDTO createPost(CreateUserPostRequest req) {
        try {
            Map<String, Object> dados = UserPostFirestoreMapper.paraDocumento(req);

            DocumentReference ref = firestore.collection(postsCollection).document();
            ref.set(dados).get();

            DocumentSnapshot doc = ref.get().get();

            DocumentSnapshot userDoc = firestore.collection(usersCollection)
                    .document(req.getUserId())
                    .get()
                    .get();

            return UserPostFirestoreMapper.paraDto(
                    doc,
                    userDoc.getString("username"),
                    userDoc.getString("profilePictureUrl")
            );

        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** Feed geral: posts de perfis privados só aparecem para quem segue (e para o próprio dono). */
    public List<UserPostDTO> getAllPosts(String viewerId) {
        try {
            QuerySnapshot resultado = firestore.collection(postsCollection)
                    .get()
                    .get();

            return montarLista(resultado, viewerId);

        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** Posts de um perfil. Perfil privado visto por quem não segue: 403. */
    public List<UserPostDTO> getPostsByUser(String userId, String viewerId) {
        privacyService.exigirAcesso(userId, viewerId);
        try {
            QuerySnapshot resultado = firestore.collection(postsCollection)
                    .whereEqualTo("userId", userId)
                    .get()
                    .get();

            return montarLista(resultado, viewerId);

        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Edita o texto de um post e marca {@code edited = true} (sem guardar data da edição).
     * 401 se anônimo, 404 se o post não existe, 403 se quem pede não é o autor.
     */
    public UserPostDTO updatePost(String postId, UpdateUserPostRequest req, String uid) {
        if (uid == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Autenticação necessária.");
        }
        if (req.getText() == null || req.getText().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O texto do post não pode ser vazio.");
        }
        try {
            DocumentReference ref = firestore.collection(postsCollection).document(postId);
            DocumentSnapshot atual = ref.get().get();

            if (!atual.exists()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Post não encontrado.");
            }
            if (!uid.equals(atual.getString("userId"))) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Só o autor pode editar este post.");
            }

            Map<String, Object> alteracoes = new HashMap<>();
            alteracoes.put("text", req.getText());
            alteracoes.put("edited", true);
            ref.update(alteracoes).get();

            DocumentSnapshot doc = ref.get().get();
            DocumentSnapshot userDoc = firestore.collection(usersCollection)
                    .document(uid)
                    .get()
                    .get();

            return UserPostFirestoreMapper.paraDto(
                    doc,
                    userDoc.getString("username"),
                    userDoc.getString("profilePictureUrl")
            );

        } catch (ResponseStatusException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Operação interrompida.", e);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public void deletePost(String postId) {
        firestore.collection(postsCollection)
                .document(postId)
                .delete();
    }

    private List<UserPostDTO> montarLista(QuerySnapshot resultado, String viewerId) throws Exception {
        List<UserPostDTO> lista = new ArrayList<>();

        for (DocumentSnapshot doc : resultado.getDocuments()) {
            String userId = doc.getString("userId");

            DocumentSnapshot userDoc = firestore.collection(usersCollection)
                    .document(userId)
                    .get()
                    .get();

            if (!UserFirestoreMapper.podeVerConteudo(userDoc, viewerId)) {
                continue;
            }

            lista.add(UserPostFirestoreMapper.paraDto(
                    doc,
                    userDoc.getString("username"),
                    userDoc.getString("profilePictureUrl")
            ));
        }

        return lista;
    }

    public void toggleLike(String postId, String userId) {
        try {

            DocumentReference ref =
                    firestore.collection(postsCollection)
                            .document(postId);

            DocumentSnapshot doc = ref.get().get();

            List<String> likes =
                    (List<String>) doc.get("likedUserIds");

            if (likes == null) {
                likes = new ArrayList<>();
            }

            boolean acabouDeCurtir = !likes.contains(userId);
            if (acabouDeCurtir) {
                likes.add(userId);
            } else {
                likes.remove(userId);
            }

            ref.update("likedUserIds", likes).get();

            // Só notifica ao curtir (não ao descurtir).
            if (acabouDeCurtir) {
                notificarCurtida(doc, postId, userId);
            }

        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** Falha aqui não pode desfazer a curtida, que já foi gravada. */
    private void notificarCurtida(DocumentSnapshot post, String postId, String likerId) {
        try {
            String donoId = post.getString("userId");
            if (donoId == null || donoId.equals(likerId)) {
                return;
            }
            DocumentSnapshot liker = firestore.collection(usersCollection)
                    .document(likerId)
                    .get()
                    .get();
            notificationService.notificarCurtida(likerId, liker.getString("username"), donoId, postId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.warn("Não foi possível notificar curtida do post {}: {}", postId, e.getMessage());
        }
    }
}