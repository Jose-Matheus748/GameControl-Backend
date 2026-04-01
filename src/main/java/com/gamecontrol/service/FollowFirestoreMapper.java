package com.gamecontrol.service;

import com.gamecontrol.dto.FollowDTO;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;

@Component
public class FollowFirestoreMapper {

    public Map<String, Object> toMap(String followerId, String followedId) {
        Map<String, Object> data = new HashMap<>();
        data.put("followerId", followerId);
        data.put("followedId", followedId);
        data.put("createdAt", com.google.cloud.firestore.FieldValue.serverTimestamp());
        return data;
    }

    public FollowDTO toDTO(QueryDocumentSnapshot document) {
        return FollowDTO.builder()
                .id(document.getId())
                .followerId(document.getString("followerId"))
                .followedId(document.getString("followedId"))
                .build();
    }
}