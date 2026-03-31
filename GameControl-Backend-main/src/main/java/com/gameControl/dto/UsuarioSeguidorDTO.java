package com.gameControl.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioSeguidorDTO {
    private Long id;
    private String nomeUsuario;
    private String urlFotoPerfil;
}
