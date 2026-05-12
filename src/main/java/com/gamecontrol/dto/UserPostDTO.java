package com.gamecontrol.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserPostDTO {

    private String id;
    private String userId;
    private String username;
    private String text;
    private List<String> likedUserIds;
    private Integer likesCount;
    private List<String> commentIds;
    private String createdAt;
    private String profilePictureUrl;
}