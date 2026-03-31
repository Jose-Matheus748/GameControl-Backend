package com.gameControl.dto;

import lombok.Data;

import java.time.LocalDate;

import org.springframework.web.multipart.MultipartFile;

@Data
public class JogoDTO {
    private Long id;
    private String titulo;
    private String desenvolvedor;
    private String editora;
    private String generos;
    private String plataformas;
    private LocalDate dataLancamento;
    private String urlCapa;
    private String descricao;
    private String linkExterno;
    private Double mediaAvaliacoes;
    private Long contagemAvaliacoes;
    private MultipartFile imagemCapa;
}
