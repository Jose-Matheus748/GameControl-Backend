package com.gamecontrol.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import lombok.Data;

import java.time.LocalDate;

/**
 * Dados de perfil enviados pelo cliente após o cadastro no Firebase Authentication.
 *
 * Este DTO não recebe e-mail, senha, UID ou papel (role):
 * a identidade deve vir do Firebase ID Token validado pelo backend,
 * e o papel deve ser definido exclusivamente pelo servidor.
 */
@Data
public class CreateUserProfileRequest {

    @NotBlank(message = "O nome de usuário é obrigatório.")
    private String username;

    private String bio;

    private String profilePictureUrl;

    @Past(message = "A data de nascimento deve estar no passado.")
    private LocalDate birthDate;

    private String country;
}
