package com.gamecontrol.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.gamecontrol.enums.Role;
import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDTO {

    private String id;
    private String email;
    private String username;
    private String bio;
    private String profilePictureUrl;
    private LocalDate birthDate;
    private String country;
    private Role role;
    /** IDs de usuários que seguem este perfil. */
    private List<String> followers = new ArrayList<>();
    /** IDs de usuários que este perfil segue. */
    private List<String> following = new ArrayList<>();
    private NotificationPreferencesDTO notificationPreferences;
    /**
     * Perfil privado: posts e playlists só aparecem para quem segue. Reviews continuam públicas.
     * É {@code Boolean} (e não {@code boolean}) de propósito: assim o JSON usa o nome "isPrivate".
     */
    private Boolean isPrivate;
}