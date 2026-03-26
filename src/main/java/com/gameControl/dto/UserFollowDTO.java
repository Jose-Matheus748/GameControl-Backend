package com.gameControl.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserFollowDTO {
    private Long id;
    private String username;
    private String profilePictureUrl;
}
