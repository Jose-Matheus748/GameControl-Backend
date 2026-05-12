package com.gamecontrol.service;

import com.gamecontrol.dto.PostCommentDTO;
import com.gamecontrol.dto.request.CreatePostCommentRequest;
import com.google.cloud.firestore.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class PostCommentService {

    private final Firestore firestore;
    private final String commentsCollection;
    private final String usersCollection;
    private final String postsCollection;

    public PostCommentService(
            Firestore firestore,
            @Value("${firebase.collection.postcomments}") String commentsCollection,
            @Value("${firebase.collection.users}") String usersCollection,
            @Value("${firebase.collection.posts}") String postsCollection
    ) {
        this.firestore = firestore;
        this.commentsCollection = commentsCollection;
        this.usersCollection = usersCollection;
        this.postsCollection = postsCollection;
    }

    public PostCommentDTO createComment(CreatePostCommentRequest req) {
        try {
            Map<String, Object> dados = PostCommentFirestoreMapper.paraDocumento(req);

            // Salva o comentário
            DocumentReference commentRef = firestore.collection(commentsCollection).document();
            commentRef.set(dados).get();

            // Atualiza o array commentIds no post
            DocumentReference postRef = firestore.collection(postsCollection).document(req.getPostId());
            postRef.update("commentIds", FieldValue.arrayUnion(commentRef.getId())).get();

            DocumentSnapshot doc = commentRef.get().get();

            DocumentSnapshot userDoc = firestore.collection(usersCollection)
                    .document(req.getUserId())
                    .get()
                    .get();

            return PostCommentFirestoreMapper.paraDto(doc, userDoc.getString("username"));

        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public List<PostCommentDTO> getCommentsByPost(String postId) {
        try {
            QuerySnapshot resultado = firestore.collection(commentsCollection)
                    .whereEqualTo("postId", postId)
                    .orderBy("createdAt", Query.Direction.ASCENDING)
                    .get()
                    .get();

            return montarLista(resultado);

        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public List<PostCommentDTO> getCommentsByUser(String userId) {
        try {
            QuerySnapshot resultado = firestore.collection(commentsCollection)
                    .whereEqualTo("userId", userId)
                    .get()
                    .get();

            return montarLista(resultado);

        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public void deleteComment(String commentId) {
        try {
            // Busca o comentário para saber o postId antes de deletar
            DocumentSnapshot doc = firestore.collection(commentsCollection)
                    .document(commentId)
                    .get()
                    .get();

            String postId = doc.getString("postId");

            // Remove o ID do array no post
            if (postId != null) {
                firestore.collection(postsCollection)
                        .document(postId)
                        .update("commentIds", FieldValue.arrayRemove(commentId));
            }

            firestore.collection(commentsCollection).document(commentId).delete();

        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private List<PostCommentDTO> montarLista(QuerySnapshot resultado) throws Exception {
        List<PostCommentDTO> lista = new ArrayList<>();

        for (DocumentSnapshot doc : resultado.getDocuments()) {
            String userId = doc.getString("userId");

            DocumentSnapshot userDoc = firestore.collection(usersCollection)
                    .document(userId)
                    .get()
                    .get();

            lista.add(PostCommentFirestoreMapper.paraDto(doc, userDoc.getString("username")));
        }

        return lista;
    }
}