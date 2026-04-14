package com.gamecontrol.dto.request;

import com.gamecontrol.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateUserRequest {

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String password;

    @NotBlank
    private String username;

    private String bio;
    private String profilePictureUrl;

    @Past
    private LocalDate birthDate;

    private String country;

    private Role role = Role.USER;
}
