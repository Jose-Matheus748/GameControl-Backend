package com.gamecontrol.service;

import com.google.cloud.firestore.*;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
public class FollowService {

    private final Firestore firestore;

    public FollowService(Firestore firestore) {
        this.firestore = firestore;
    }

    public void follow(String followerId, String followedId) throws ExecutionException, InterruptedException {
        String docId = followerId + "_" + followedId;
        Map<String, Object> data = new HashMap<>();
        data.put("followerId", followerId);
        data.put("followedId", followedId);
        data.put("createdAt", FieldValue.serverTimestamp());

        firestore.collection("follows").document(docId).set(data, SetOptions.merge()).get();
    }

    public List<String> getSeguidores(String userId) throws ExecutionException, InterruptedException {
        QuerySnapshot query = firestore.collection("follows")
                .whereEqualTo("followedId", userId)
                .get().get();

        return query.getDocuments().stream()
                .map(doc -> doc.getString("followerId"))
                .collect(Collectors.toList());
    }

    public List<String> getSeguindo(String userId) throws ExecutionException, InterruptedException {
        QuerySnapshot query = firestore.collection("follows")
                .whereEqualTo("followerId", userId)
                .get().get();

        return query.getDocuments().stream()
                .map(doc -> doc.getString("followedId"))
                .collect(Collectors.toList());
    }
}