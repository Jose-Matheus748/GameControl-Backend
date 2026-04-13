package com.gamecontrol.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.Instant;

@Data
public class CreateGameRequest {

    @NotBlank
    private String title;

    private String coverImageUrl;
    private String description;
    private String developer;
    private String genres;
    private Long igdbId;
    private Double igdbPopularityValue;
    private String igdbUrl;
    private String publisher;
    private Long rank;
    private Double rating;
    private Long ratingCount;
    private String releaseDate;
    private Integer themeId;
    /** Se informado, o documento é criado com este ID (ex.: slug único). */
    private String documentId;

    private String slug;
    private Instant syncedAt;
    private Double totalRating;
    private Long totalRatingCount;
}
