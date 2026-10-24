package com.gamecontrol.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/** Pedido de seguir pendente, como aparece na lista de quem recebeu o pedido. */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FollowRequestDTO {

    /** Usuário que pediu para seguir. */
    private String requesterId;
    private String requesterUsername;
    private String requesterProfilePictureUrl;
    /** Data do pedido em ISO-8601 (UTC). */
    private String createdAt;
}
