package com.gamecontrol.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FollowDTO {
    private String id;
    private String followerId;
    private String followingId;
    private String createdAt;
}
