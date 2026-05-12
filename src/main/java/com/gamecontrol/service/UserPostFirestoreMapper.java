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
        dados.put("likedUserIds", new ArrayList<>());
        dados.put("commentIds", new ArrayList<>());
        dados.put("createdAt", LocalDateTime.now().toString());
        return dados;
    }

    static UserPostDTO paraDto(DocumentSnapshot doc, String username) {
        UserPostDTO dto = new UserPostDTO();
        dto.setId(doc.getId());
        dto.setUserId(doc.getString("userId"));
        dto.setText(doc.getString("text"));
        List<String> likedUsers =
                (List<String>) doc.get("likedUserIds");

        if (likedUsers == null) {
            likedUsers = new ArrayList<>();
        }
        dto.setLikedUserIds(likedUsers);
        dto.setCommentIds(
                (List<String>) doc.get("commentIds")
        );
        dto.setCreatedAt(doc.getString("createdAt"));
        dto.setUsername(username);
        dto.setLikesCount(likedUsers.size());

        return dto;
    }
}