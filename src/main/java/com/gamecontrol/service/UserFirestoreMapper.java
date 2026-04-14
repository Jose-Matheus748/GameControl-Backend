package com.gamecontrol.service;

import com.gamecontrol.dto.request.CreateUserRequest;
import com.gamecontrol.dto.UserDTO;
import com.gamecontrol.enums.Role;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentSnapshot;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.Map;

final class UserFirestoreMapper {

    private UserFirestoreMapper() {
    }

    static Map<String, Object> paraDocumento(CreateUserRequest requisicao) {
        Map<String, Object> dados = new LinkedHashMap<>();
        String email = requisicao.getEmail().trim().toLowerCase(Locale.ROOT);
        dados.put("email", email);
        dados.put("emailLower", email);
        dados.put("password", requisicao.getPassword());
        dados.put("username", requisicao.getUsername().trim());

        putIfPresent(dados, "bio", requisicao.getBio());
        putIfPresent(dados, "profilePictureUrl", requisicao.getProfilePictureUrl());

        if (requisicao.getBirthDate() != null) {
            dados.put("birthDate", requisicao.getBirthDate().toString());
        }

        putIfPresent(dados, "country", requisicao.getCountry());
        Role papel = requisicao.getRole() != null ? requisicao.getRole() : Role.USER;
        dados.put("role", papel.name());

        return dados;
    }

    static UserDTO paraDto(DocumentSnapshot documento) {
        UserDTO dto = new UserDTO();

        dto.setId(documento.getId());
        dto.setEmail(documento.getString("email"));
        dto.setUsername(documento.getString("username"));
        dto.setBio(documento.getString("bio"));
        dto.setProfilePictureUrl(documento.getString("profilePictureUrl"));
        dto.setCountry(documento.getString("country"));
        dto.setBirthDate(lerDataNascimento(documento));
        dto.setRole(lerPapel(documento));

        return dto;
    }

    private static LocalDate lerDataNascimento(DocumentSnapshot documento) {
        String texto = documento.getString("birthDate");
        if (texto != null && !texto.isBlank()) {
            return LocalDate.parse(texto);
        }

        Timestamp ts = documento.getTimestamp("birthDate");
        if (ts != null) {
            return Instant.ofEpochSecond(ts.getSeconds(), ts.getNanos()).atZone(ZoneOffset.UTC).toLocalDate();
        }

        return null;
    }

    private static Role lerPapel(DocumentSnapshot documento) {
        String nome = documento.getString("role");
        
        if (nome == null || nome.isBlank()) {
            return Role.USER;
        }
        try {
            return Role.valueOf(nome);
        } catch (IllegalArgumentException e) {
            return Role.USER;
        }
    }

    private static void putIfPresent(Map<String, Object> mapa, String chave, String valor) {
        if (valor != null && !valor.isBlank()) {
            mapa.put(chave, valor.trim());
        }
    }
}
