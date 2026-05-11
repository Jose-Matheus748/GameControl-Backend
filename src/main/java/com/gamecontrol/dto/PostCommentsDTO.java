package com.gamecontrol.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class PostCommentsDTO {
    private String id;
    private String postId;
    private String userId;
    private String content;
    private String createdAt;
    private String username;
}
