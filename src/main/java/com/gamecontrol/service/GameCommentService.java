package com.gamecontrol.service;

import com.gamecontrol.dto.request.CreateCommentRequest;
import com.gamecontrol.dto.GameCommentDTO;
import com.google.cloud.firestore.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GameCommentService {
    
    private final Firestore firestore;
    private final String commentsCollection;
    private final String usersCollection;

    public GameCommentService(
            Firestore firestore,
            @Value("${firebase.collection.gamecomments}") String commentsCollection,
            @Value("${firebase.collection.users}") String usersCollection
    ) {
        this.firestore = firestore;
        this.commentsCollection = commentsCollection;
        this.usersCollection = usersCollection;
    }

    public GameCommentDTO createComment(CreateCommentRequest req) {
        try {
            Map<String, Object> dados = GameCommentFirestoreMapper.paraDocumento(req);

            DocumentReference ref = firestore.collection(commentsCollection).document();
            ref.set(dados).get();

            DocumentSnapshot doc = ref.get().get();

            DocumentSnapshot userDoc = firestore.collection(usersCollection)
                .document(req.getUserId())
                .get()
                .get();

            return GameCommentFirestoreMapper.paraDto(
                doc,
                userDoc.getString("username")
            );

        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public List<GameCommentDTO> getCommentsByGame(String gameId) {
        try {
            QuerySnapshot resultado = firestore.collection(commentsCollection)
                .whereEqualTo("gameId", gameId)
                .get()
                .get();

            return montarLista(resultado);

        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public List<GameCommentDTO> getCommentsByUser(String userId) {
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
        firestore.collection(commentsCollection)
            .document(commentId)
            .delete();
    }

    private List<GameCommentDTO> montarLista(QuerySnapshot resultado) throws Exception {
        List<GameCommentDTO> lista = new ArrayList<>();

        for (DocumentSnapshot doc : resultado.getDocuments()) {
            String userId = doc.getString("userId");

            DocumentSnapshot userDoc = firestore.collection(usersCollection)
                .document(userId)
                .get()
                .get();

            lista.add(GameCommentFirestoreMapper.paraDto(
                doc,
                userDoc.getString("username")
            ));
        }

        return lista;
    }
}