package com.gamecontrol.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateGenreRequest {

    @NotBlank
    private String name;
}