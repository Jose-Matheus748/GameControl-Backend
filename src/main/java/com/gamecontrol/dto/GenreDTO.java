package com.gamecontrol.dto;

import lombok.Data;
import java.util.List;

@Data
public class GenreDTO {
    private String id;
    private String name;
    private String slug;
    private List<String> gameIds;
}