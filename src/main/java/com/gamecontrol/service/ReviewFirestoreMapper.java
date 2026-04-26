package com.gamecontrol.service;

import com.gamecontrol.dto.ReviewDTO;
import com.gamecontrol.dto.request.CreateReviewRequest;
import com.google.cloud.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

final class ReviewFirestoreMapper {

    private ReviewFirestoreMapper() {}

    static ReviewDTO fromSnapshot(DocumentSnapshot snap) {
        if (!snap.exists()) return null;

        ReviewDTO dto = new ReviewDTO();
        dto.setId(snap.getId());
        dto.setUserId(snap.getString("userId"));
        dto.setUserName(snap.getString("userName"));
        dto.setGameId(snap.getString("gameId"));
        dto.setRating(snap.getDouble("rating"));
        dto.setDescription(snap.getString("description"));
        dto.setCreatedAt(snap.getString("createdAt"));

        return dto;
    }

    static Map<String, Object> toMap(CreateReviewRequest req) {
        Map<String, Object> map = new HashMap<>();

        map.put("userId", req.getUserId());
        map.put("gameId", req.getGameId());
        map.put("rating", req.getRating());
        map.put("description", req.getDescription());

        return map;
    }

    static Map<String, Object> patchMap(CreateReviewRequest req) {
        Map<String, Object> map = new HashMap<>();

        if (req.getRating() != null) {
            map.put("rating", req.getRating());
        }

        if (req.getDescription() != null) {
            map.put("description", req.getDescription());
        }

        return map;
    }
}