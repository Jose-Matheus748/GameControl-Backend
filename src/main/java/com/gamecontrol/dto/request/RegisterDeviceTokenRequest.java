package com.gamecontrol.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterDeviceTokenRequest {

    @NotBlank
    private String userId;

    /** Token do Firebase Cloud Messaging gerado pelo aparelho. */
    @NotBlank
    private String token;

    /** "android" ou "ios" — apenas informativo. */
    private String platform;
}
