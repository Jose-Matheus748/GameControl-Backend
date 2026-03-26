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
public class Playlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String name;
    
    @Column(length = 500)
    private String description;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"playlists"})
    private User user;

    @ManyToMany
    @JoinTable(
            name = "playlist_games",
            joinColumns = @JoinColumn(name = "playlist_id"),
            inverseJoinColumns = @JoinColumn(name = "game_id")
    )
    @JsonIgnoreProperties({"reviews"})
    private List<Game> games = new ArrayList<>();

    public Playlist() {}
}
