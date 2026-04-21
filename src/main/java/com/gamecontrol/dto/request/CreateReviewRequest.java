package com.gamecontrol.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateReviewRequest {

    @NotBlank
    private String userId;

    @NotBlank
    private String gameId;

    @NotNull
    @Min(value = 0)
    @Max(value = 5)
    private Double rating;

    @NotBlank(message = "A descrição não pode estar vazia")
    private String description;
}

