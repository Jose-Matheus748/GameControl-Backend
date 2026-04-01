package com.gamecontrol.service;

import com.gamecontrol.dto.FollowDTO;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class FollowFirestoreMapper {

    public Map<String, Object> toMap(String followerId, String followingId) {
        Map<String, Object> data = new HashMap<>();
        data.put("followerId", followerId);
        data.put("followingId", followingId);
        data.put("createdAt", com.google.cloud.firestore.FieldValue.serverTimestamp());
        return data;
    }

    public FollowDTO toDTO(QueryDocumentSnapshot document) {
        return FollowDTO.builder()
                .id(document.getId())
                .followerId(document.getString("followerId"))
                .followingId(document.getString("followingId"))
                .createdAt(document.contains("createdAt") && document.get("createdAt") != null
                        ? document.getTimestamp("createdAt").toString()
                        : null)
                .build();
    }
}
