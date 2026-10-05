package com.gamecontrol.service;

import com.gamecontrol.dto.NotificationDTO;
import com.gamecontrol.enums.NotificationType;
import com.google.cloud.firestore.DocumentSnapshot;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

final class NotificationFirestoreMapper {

    private NotificationFirestoreMapper() {
    }

    static Map<String, Object> paraDocumento(
            String recipientId,
            String actorId,
            String actorUsername,
            NotificationType type,
            String message
    ) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("recipientId", recipientId);
        dados.put("actorId", actorId);
        dados.put("actorUsername", actorUsername);
        dados.put("type", type.name());
        dados.put("message", message);
        dados.put("read", false);
        dados.put("createdAt", Instant.now().toString());
        return dados;
    }

    static NotificationDTO paraDto(DocumentSnapshot documento) {
        NotificationDTO dto = new NotificationDTO();
        dto.setId(documento.getId());
        dto.setRecipientId(documento.getString("recipientId"));
        dto.setActorId(documento.getString("actorId"));
        dto.setActorUsername(documento.getString("actorUsername"));
        dto.setType(lerTipo(documento));
        dto.setMessage(documento.getString("message"));
        dto.setRead(Boolean.TRUE.equals(documento.getBoolean("read")));
        dto.setCreatedAt(documento.getString("createdAt"));
        return dto;
    }

    private static NotificationType lerTipo(DocumentSnapshot documento) {
        String tipo = documento.getString("type");
        if (tipo == null) {
            return null;
        }
        try {
            return NotificationType.valueOf(tipo);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
