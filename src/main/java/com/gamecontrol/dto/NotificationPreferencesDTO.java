package com.gamecontrol.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationPreferencesDTO {

    /** Curtidas nas publicações do usuário. */
    private Boolean likes;
    /** Comentários nas publicações do usuário. */
    private Boolean comments;
    /** Novos seguidores. */
    private Boolean followers;
}