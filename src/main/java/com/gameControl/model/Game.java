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
@Table(name = "games")
@Data
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String title;

    @Column(length = 2000)
    private String description;
    private String developer;
    private String publisher;
    private LocalDate releaseDate;

    @Column(length = 500)
    private String coverImageUrl;
    private String genres;

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Review> reviews;

    public Game() {}

    public Game(String title, String description) {
        this.title = title;
        this.description = description;
    }
    
    public Game(String title, String description, String developer, String publisher,
                LocalDate releaseDate, String coverImageUrl, String genres) {
        this.title = title;
        this.description = description;
        this.developer = developer;
        this.publisher = publisher;
        this.releaseDate = releaseDate;
        this.coverImageUrl = coverImageUrl;
        this.genres = genres;
    }
}
