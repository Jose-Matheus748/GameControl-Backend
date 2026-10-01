package com.gamecontrol.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.gamecontrol.enums.NotificationType;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationDTO {

    private String id;
    /** Usuário que recebe a notificação. */
    private String recipientId;
    /** Usuário que gerou o evento (ex.: quem começou a seguir). */
    private String actorId;
    private String actorUsername;
    private NotificationType type;
    private String message;
    private boolean read;
    /** Data em ISO-8601 (UTC). */
    private String createdAt;
}
