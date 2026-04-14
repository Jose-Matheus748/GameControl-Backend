package com.gamecontrol.controller;

import com.gamecontrol.dto.GenreDTO;
import com.gamecontrol.service.GenreService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/genres")
public class GenreController {

    private final GenreService genreService;

    public GenreController(GenreService genreService) {
        this.genreService = genreService;
    }

    @GetMapping
    public ResponseEntity<List<GenreDTO>> listarGeneros() {
        return ResponseEntity.ok(genreService.listarGeneros());
    }
}