package com.gamecontrol.dto;

import java.util.List;

public record GameReviewsPageDTO(
        Object GameDTO,
        List<ReviewDTO> reviews,
        ReviewDTO userReview,
        Double average,
        String displayAverage
) {}