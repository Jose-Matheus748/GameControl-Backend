package com.gamecontrol.dto;

import java.util.List;

public record GameReviewsPageDTO(
        Object GameDTO,
        List<ReviewDTO> reviews,
        Double average,
        String displayAverage
) {}