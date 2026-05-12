package com.gamecontrol.service;

import com.gamecontrol.dto.UserPostDTO;
import com.gamecontrol.dto.request.CreateUserPostRequest;
import com.google.cloud.firestore.DocumentSnapshot;

import java.time.LocalDateTime;
import java.util.*;

final class UserPostFirestoreMapper {

    private UserPostFirestoreMapper() {}

    static Map<String, Object> paraDocumento(CreateUserPostRequest req) {
        Map<String, Object> dados = new HashMap<>();
        dados.put("userId", req.getUserId());
        dados.put("text", req.getText());
        dados.put("likes", 0);
        dados.put("commentIds", new ArrayList<>());
        dados.put("createdAt", LocalDateTime.now().toString());
        return dados;
    }

    static UserPostDTO paraDto(DocumentSnapshot doc, String username, String profilePictureUrl) {
        UserPostDTO dto = new UserPostDTO();
        dto.setId(doc.getId());
        dto.setUserId(doc.getString("userId"));
        dto.setText(doc.getString("text"));
        dto.setLikes(doc.getLong("likes") != null ? doc.getLong("likes").intValue() : 0);
        dto.setCommentIds((List<String>) doc.get("commentIds"));
        dto.setCreatedAt(doc.getString("createdAt"));
        dto.setUsername(username);
        dto.setProfilePictureUrl(profilePictureUrl);
        return dto;
    }
}