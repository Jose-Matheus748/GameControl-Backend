package com.gamecontrol.service;

import com.gamecontrol.dto.UserPostDTO;
import com.gamecontrol.dto.request.CreateUserPostRequest;
import com.google.cloud.firestore.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class UserPostService {

    private final Firestore firestore;
    private final String postsCollection;
    private final String usersCollection;

    public UserPostService(
            Firestore firestore,
            @Value("${firebase.collection.posts}") String postsCollection,
            @Value("${firebase.collection.users}") String usersCollection
    ) {
        this.firestore = firestore;
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
                    userDoc.getString("username")
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
                    userDoc.getString("username")
            ));
        }

        return lista;
    }
}