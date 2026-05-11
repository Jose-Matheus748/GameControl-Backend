package com.gamecontrol.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GameCommentDTO {

    private String id;
    private String userId;
    private String username;
    private String gameId;
    private String content;
    private String createdAt;
    private String postId;
}