package com.gamecontrol.dto;

import java.util.List;

public record GameReviewsPageDTO(
        GameDTO game,
        List<ReviewDTO> reviews,
        ReviewDTO userReview,
        Double average,
        String displayAverage
) {}