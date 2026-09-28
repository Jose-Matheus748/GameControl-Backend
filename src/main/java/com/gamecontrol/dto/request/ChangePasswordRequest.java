package com.gamecontrol.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequest {

    @NotBlank(message = "A senha atual é obrigatória.")
    private String currentPassword;

    @NotBlank(message = "A nova senha é obrigatória.")
    @Size(min = 8, message = "A nova senha deve ter no mínimo 8 caracteres.")
    private String newPassword;

    /**
     * Opcional. Se enviado pelo cliente, deve ser igual a {@code newPassword}.
     * Útil quando o app mobile pede a confirmação da nova senha na mesma tela.
     */
    private String confirmNewPassword;
}