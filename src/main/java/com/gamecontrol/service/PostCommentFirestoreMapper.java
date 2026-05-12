package com.gamecontrol.service;

import com.gamecontrol.dto.PostCommentDTO;
import com.gamecontrol.dto.request.CreatePostCommentRequest;
import com.google.cloud.firestore.DocumentSnapshot;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

final class PostCommentFirestoreMapper {

    private PostCommentFirestoreMapper() {}

    static Map<String, Object> paraDocumento(CreatePostCommentRequest req) {
        Map<String, Object> dados = new HashMap<>();
        dados.put("userId", req.getUserId());
        dados.put("postId", req.getPostId());
        dados.put("content", req.getContent());
        dados.put("createdAt", LocalDateTime.now().toString());
        return dados;
    }

    static PostCommentDTO paraDto(DocumentSnapshot doc, String username) {
        PostCommentDTO dto = new PostCommentDTO();
        dto.setId(doc.getId());
        dto.setUserId(doc.getString("userId"));
        dto.setPostId(doc.getString("postId"));
        dto.setContent(doc.getString("content"));
        dto.setCreatedAt(doc.getString("createdAt"));
        dto.setUsername(username);
        return dto;
    }
}