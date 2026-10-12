package com.gamecontrol.dto;

public record TopRatedGameDTO(GameDTO game, double averageRating, int reviewCount) {
}
