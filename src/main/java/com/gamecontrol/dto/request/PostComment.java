package com.gamecontrol.dto.request;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostComment {
    private String id;
    private String postId;
    private String userId;
    private String content;
    private String createdAt;
}
