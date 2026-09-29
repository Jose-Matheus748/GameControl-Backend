package com.gamecontrol.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResumoDTO {

    private String id;
    private String username;
    private String profilePictureUrl;
    private long followersCount;
}
