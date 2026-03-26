package com.gameControl.controller;

import com.gameControl.dto.GameDTO;
import com.gameControl.model.Game;
import com.gameControl.services.GameService;
import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/games")
public class GameController {
    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @GetMapping
    public ResponseEntity<List<GameDTO>> listarJogos() {
        List<GameDTO> jogos = gameService.listarJogos();
        return ResponseEntity.ok(jogos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GameDTO> buscarJogoPorID(@PathVariable Long id) {
        return gameService.buscarJogoPorID(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<GameDTO> cadastrarJogo(
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String developer,
            @RequestParam(required = false) String publisher,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate releaseDate,
            @RequestParam(required = false) String genres,
            @RequestParam(required = false) String externalLink,
            @RequestParam(required = false) MultipartFile coverImage
    ) {
        GameDTO gameDTO = new GameDTO();
        gameDTO.setTitle(title);
        gameDTO.setDescription(description);
        gameDTO.setDeveloper(developer);
        gameDTO.setPublisher(publisher);
        gameDTO.setReleaseDate(releaseDate);
        gameDTO.setGenres(genres);
        gameDTO.setExternalLink(externalLink);
        gameDTO.setCoverImage(coverImage);

        GameDTO novoJogo = gameService.cadastrarJogo(gameDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(novoJogo);
    }

    @PutMapping("/{id}")
    public ResponseEntity<GameDTO> atualizarJogo(@PathVariable Long id, @Valid @RequestBody Game game) {
        try {
            GameDTO jogoAtualizado = gameService.atualizarJogo(id, game);
            return ResponseEntity.ok(jogoAtualizado);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarJogo(@PathVariable Long id) {
        boolean deletado = gameService.deletarJogo(id);
        if (deletado) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
