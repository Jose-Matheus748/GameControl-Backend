package com.gamecontrol.controller;

import com.gamecontrol.dto.request.CreateGenreRequest;
import com.gamecontrol.dto.GenreDTO;
import com.gamecontrol.service.GenreService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/genres")
public class GenreController {

    private final GenreService service;

    public GenreController(GenreService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<GenreDTO> create(@Valid @RequestBody CreateGenreRequest req) {
        return ResponseEntity.ok(service.createGenre(req));
    }

    @GetMapping
    public ResponseEntity<List<GenreDTO>> list() {
        return ResponseEntity.ok(service.listGenres());
    }
}