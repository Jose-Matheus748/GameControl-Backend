package com.gameControl.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "playlists")
@Data
public class UsuarioPlayList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String nome;
    
    @Column(length = 500)
    private String descricao;

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    @JsonIgnoreProperties({"playlists"})
    private Usuario usuario;

    @ManyToMany
    @JoinTable(
            name = "playlist_jogos",
            joinColumns = @JoinColumn(name = "playlist_id"),
            inverseJoinColumns = @JoinColumn(name = "jogo_id")
    )
    @JsonIgnoreProperties({"reviews"})
    private List<Jogo> jogos = new ArrayList<>();

    public UsuarioPlayList() {}
}
