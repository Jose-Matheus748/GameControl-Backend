package com.gamecontrol.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateCommentRequest {

    @NotBlank
    private String userId;

    @NotBlank
    private String gameId;

    @NotBlank
    private String content;
}