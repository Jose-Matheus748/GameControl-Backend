package com.gamecontrol.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UsuarioPlayListDTO {

    private String id;
    private String nome;
    private String descricao;
    private String usuarioId;
    private List<String> jogosIds = new ArrayList<>();

}