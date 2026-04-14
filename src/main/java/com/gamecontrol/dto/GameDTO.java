package com.gamecontrol.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GameDTO {

    private String id;
    private String coverImageUrl;
    private String description;
    private String developer;
    private List<String> genreIds; // banco
    private List<GenreDTO> genres; // resposta
    private Long igdbId;
    private Double igdbPopularityValue;
    private String igdbUrl;
    private String publisher;
    private Long rank;
    private Double rating;
    private Long ratingCount;
    private String releaseDate;
    private String slug;
    private Instant syncedAt;
    private String title;
    private Double totalRating;
    private Long totalRatingCount;
    private Integer themeId;
}
