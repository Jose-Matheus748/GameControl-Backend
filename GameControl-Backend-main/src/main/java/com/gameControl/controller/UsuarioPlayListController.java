package com.gameControl.controller;

import com.gameControl.model.UsuarioPlayList;
import com.gameControl.services.UsuarioPlayListService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/playlists")
public class UsuarioPlayListController {
    private final UsuarioPlayListService usuarioPlayListService;

    public UsuarioPlayListController(UsuarioPlayListService usuarioPlayListService) {
        this.usuarioPlayListService = usuarioPlayListService;
    }

    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<UsuarioPlayList>> listarPlaylistsPorUsuario(@PathVariable Long usuarioId) {
        List<UsuarioPlayList> usuarioPlayLists = usuarioPlayListService.listarPlaylistsPorUsuario(usuarioId);
        return ResponseEntity.ok(usuarioPlayLists);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioPlayList> buscarPlaylistPorID(@PathVariable Long id) {
        Optional<UsuarioPlayList> playlist = usuarioPlayListService.buscarPlaylistPorID(id);
        return playlist.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> criarPlaylist(
            @RequestParam Long usuarioId,
            @Valid @RequestBody UsuarioPlayList usuarioPlayList) {
        try {
            UsuarioPlayList novaUsuarioPlayList = usuarioPlayListService.criarPlaylist(usuarioId, usuarioPlayList);
            return ResponseEntity.status(HttpStatus.CREATED).body(novaUsuarioPlayList);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioPlayList> atualizarPlaylist(@PathVariable Long id, @Valid @RequestBody UsuarioPlayList usuarioPlayList) {
        try {
            UsuarioPlayList usuarioPlayListAtualizada = usuarioPlayListService.atualizarPlaylist(id, usuarioPlayList);
            return ResponseEntity.ok(usuarioPlayListAtualizada);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarPlaylist(@PathVariable Long id) {
        boolean deletado = usuarioPlayListService.deletarPlaylist(id);
        if (deletado) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{playlistId}/jogos/{jogoId}")
    public ResponseEntity<UsuarioPlayList> adicionarJogo(@PathVariable Long playlistId, @PathVariable Long gameId) {
        try {
            UsuarioPlayList usuarioPlayListAtualizada = usuarioPlayListService.adicionarJogo(playlistId, gameId);
            return ResponseEntity.ok(usuarioPlayListAtualizada);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{playlistId}/jogos/{jogoId}")
    public ResponseEntity<UsuarioPlayList> removerJogo(@PathVariable Long playlistId, @PathVariable Long gameId) {
        try {
            UsuarioPlayList usuarioPlayListAtualizada = usuarioPlayListService.removerJogo(playlistId, gameId);
            return ResponseEntity.ok(usuarioPlayListAtualizada);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}