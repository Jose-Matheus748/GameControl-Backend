package com.gameControl.dto;

import lombok.Data;

import java.time.LocalDate;

import org.springframework.web.multipart.MultipartFile;

@Data
public class GameDTO {
    private Long id;
    private String title;
    private String developer;
    private String publisher;
    private String genres;
    private String platforms;
    private LocalDate releaseDate;
    private String coverImageUrl;
    private String description;
    private String externalLink;
    private Double averageRating;
    private Long ratingCount;
    private MultipartFile coverImage;
}
