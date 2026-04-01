package com.gamecontrol.service;

import com.google.cloud.firestore.*;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
public class FollowService {

    private final Firestore firestore;

    public FollowService(Firestore firestore) {
        this.firestore = firestore;
    }

    public void follow(String followerId, String followingId) throws ExecutionException, InterruptedException {
        if (followerId.equals(followingId)) {
            throw new IllegalArgumentException("Cannot follow yourself");
        }

        String docId = followerId + "_" + followingId;
        DocumentReference docRef = firestore.collection("follows").document(docId);

        Map<String, Object> data = new HashMap<>();
        data.put("followerId", followerId);
        data.put("followingId", followingId);
        data.put("createdAt", FieldValue.serverTimestamp());

        docRef.set(data, SetOptions.merge()).get();
    }

    public void unfollow(String followerId, String followingId) throws ExecutionException, InterruptedException {
        String docId = followerId + "_" + followingId;
        firestore.collection("follows").document(docId).delete().get();
    }
}
