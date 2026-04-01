package com.gamecontrol.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
public class CreatePlaylistRequest {

    @NotBlank
    @Size(max = 100)
    private String nome;

    @Size(max = 500)
    private String descricao;

    private String usuarioId; // Preenchido automaticamente pela Service
    private List<String> jogosIds = new ArrayList<>();
    private String documentId;
    private Instant syncedAt;
}