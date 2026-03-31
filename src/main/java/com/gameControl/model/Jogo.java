package com.gameControl.model;

import java.time.LocalDate;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Entity
@Table(name = "jogos")
@Data
public class Jogo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String titulo;

    @Column(length = 2000)
    private String descricao;

    private String desenvolvedor;

    private String editora;

    private LocalDate dataLancamento;

    @Column(length = 500)
    private String urlCapa;
    private String generos;

    @OneToMany(mappedBy = "jogo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Avaliacao> avaliacoes;

    public Jogo() {}

    public Jogo(String titulo, String descricao) {
        this.titulo = titulo;
        this.descricao = descricao;
    }

    public Jogo(String titulo, String descricao, String desenvolvedor, String editora,
                LocalDate dataLancamento, String urlCapa, String generos) {
        this.titulo = titulo;
        this.descricao = descricao;
        this.desenvolvedor = desenvolvedor;
        this.editora = editora;
        this.dataLancamento = dataLancamento;
        this.urlCapa = urlCapa;
        this.generos = generos;
    }
}