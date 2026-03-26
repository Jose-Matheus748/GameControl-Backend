package com.gameControl.controller;

import com.gameControl.model.Playlist;
import com.gameControl.services.PlaylistService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/playlists")
public class PlaylistController {
    private final PlaylistService playlistService;

    public PlaylistController(PlaylistService playlistService) {
        this.playlistService = playlistService;
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Playlist>> listarPlaylistsPorUsuario(@PathVariable Long userId) {
        List<Playlist> playlists = playlistService.listarPlaylistsPorUsuario(userId);
        return ResponseEntity.ok(playlists);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Playlist> buscarPlaylistPorID(@PathVariable Long id) {
        Optional<Playlist> playlist = playlistService.buscarPlaylistPorID(id);
        return playlist.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> criarPlaylist(
            @RequestParam Long userId,
            @Valid @RequestBody Playlist playlist) {
        try {
            Playlist novaPlaylist = playlistService.criarPlaylist(userId, playlist);
            return ResponseEntity.status(HttpStatus.CREATED).body(novaPlaylist);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Playlist> atualizarPlaylist(@PathVariable Long id, @Valid @RequestBody Playlist playlist) {
        try {
            Playlist playlistAtualizada = playlistService.atualizarPlaylist(id, playlist);
            return ResponseEntity.ok(playlistAtualizada);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarPlaylist(@PathVariable Long id) {
        boolean deletado = playlistService.deletarPlaylist(id);
        if (deletado) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{playlistId}/games/{gameId}")
    public ResponseEntity<Playlist> adicionarJogo(@PathVariable Long playlistId, @PathVariable Long gameId) {
        try {
            Playlist playlistAtualizada = playlistService.adicionarJogo(playlistId, gameId);
            return ResponseEntity.ok(playlistAtualizada);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{playlistId}/games/{gameId}")
    public ResponseEntity<Playlist> removerJogo(@PathVariable Long playlistId, @PathVariable Long gameId) {
        try {
            Playlist playlistAtualizada = playlistService.removerJogo(playlistId, gameId);
            return ResponseEntity.ok(playlistAtualizada);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}