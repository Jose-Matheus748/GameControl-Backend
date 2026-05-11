package com.gamecontrol.service;

import com.gamecontrol.dto.request.CreateCommentRequest;
import com.gamecontrol.dto.GameCommentDTO;
import com.google.cloud.firestore.DocumentSnapshot;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

final class GameCommentFirestoreMapper {

    private GameCommentFirestoreMapper() {}

    static Map<String, Object> paraDocumento(CreateCommentRequest req) {
        Map<String, Object> dados = new HashMap<>();
        dados.put("userId", req.getUserId());
        dados.put("gameId", req.getGameId());
        dados.put("postId", req.getPostId());
        dados.put("content", req.getContent());
        dados.put("createdAt", LocalDateTime.now().toString());
        return dados;
    }

    static GameCommentDTO paraDto(DocumentSnapshot doc, String username) {
        GameCommentDTO dto = new GameCommentDTO();
        dto.setId(doc.getId());
        dto.setUserId(doc.getString("userId"));
        dto.setGameId(doc.getString("gameId"));
        dto.setPostId(doc.getString("postId"));
        dto.setContent(doc.getString("content"));
        dto.setCreatedAt(doc.getString("createdAt"));
        dto.setUsername(username);
        return dto;
    }
}