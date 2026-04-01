package com.gamecontrol.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FollowDTO {
    private String id;
    private String followerId;
    private String followedId;
    private String userId;
    private String username;
    private String userPhoto;
}