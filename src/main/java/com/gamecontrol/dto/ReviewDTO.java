package com.gamecontrol.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReviewDTO {

    private String id;
    private String userId;
    private String userName;
    private String gameId;
    private Double rating;
    private String description;
    private String createdAt;

}

