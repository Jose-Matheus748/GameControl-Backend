package com.gamecontrol.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateUserPostRequest {

    @NotBlank
    private String userId;

    @NotBlank
    private String text;
}