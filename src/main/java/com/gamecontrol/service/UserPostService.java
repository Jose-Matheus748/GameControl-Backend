package com.gamecontrol.service;

import com.gamecontrol.dto.UserPostDTO;
import com.gamecontrol.dto.request.CreateUserPostRequest;
import com.google.cloud.firestore.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class UserPostService {

    private static final Logger log = LoggerFactory.getLogger(UserPostService.class);

    private final Firestore firestore;
    private final NotificationService notificationService;
    private final String postsCollection;
    private final String usersCollection;

    public UserPostService(
            Firestore firestore,
            NotificationService notificationService,
            @Value("${firebase.collection.posts}") String postsCollection,
            @Value("${firebase.collection.users}") String usersCollection
    ) {
        this.firestore = firestore;
        this.notificationService = notificationService;
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

    public List<UserPostDTO> getAllPosts() {
        try {
            QuerySnapshot resultado = firestore.collection(postsCollection)
                    .get()
                    .get();

            return montarLista(resultado);

        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public List<UserPostDTO> getPostsByUser(String userId) {
        try {
            QuerySnapshot resultado = firestore.collection(postsCollection)
                    .whereEqualTo("userId", userId)
                    .get()
                    .get();

            return montarLista(resultado);

        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public void deletePost(String postId) {
        firestore.collection(postsCollection)
                .document(postId)
                .delete();
    }

    private List<UserPostDTO> montarLista(QuerySnapshot resultado) throws Exception {
        List<UserPostDTO> lista = new ArrayList<>();

        for (DocumentSnapshot doc : resultado.getDocuments()) {
            String userId = doc.getString("userId");

            DocumentSnapshot userDoc = firestore.collection(usersCollection)
                    .document(userId)
                    .get()
                    .get();

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