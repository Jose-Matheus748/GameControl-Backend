package com.gamecontrol.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreatePostCommentRequest {

    @NotBlank
    private String userId;

    @NotBlank
    private String postId;

    @NotBlank
    private String content;
}