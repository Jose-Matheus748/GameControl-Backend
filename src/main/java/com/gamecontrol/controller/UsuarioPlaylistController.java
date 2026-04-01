package com.gamecontrol.controller;

import com.gamecontrol.dto.CreatePlaylistRequest;
import com.gamecontrol.dto.UsuarioPlayListDTO;
import com.gamecontrol.service.UsuarioPlaylistService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/usuario-playlists")
public class UsuarioPlaylistController {

    private final UsuarioPlaylistService usuarioPlaylistService;

    public UsuarioPlaylistController(UsuarioPlaylistService usuarioPlaylistService) {
        this.usuarioPlaylistService = usuarioPlaylistService;
    }

    /**
     * Cria uma nova playlist.
     * O usuarioId é extraído do parâmetro da URL (?usuarioId=...)
     * e repassado para a Service preencher o objeto automaticamente.
     */
    @PostMapping
    public ResponseEntity<?> criarPlaylist(
            @RequestParam String usuarioId,
            @Valid @RequestBody CreatePlaylistRequest request) {
        try {
            // A Service recebe o ID do parâmetro e os dados do corpo
            UsuarioPlayListDTO nova = usuarioPlaylistService.criarPlaylist(usuarioId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(nova);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<UsuarioPlayListDTO>> listarPlaylistsPorUsuario(@PathVariable String usuarioId) {
        return ResponseEntity.ok(usuarioPlaylistService.listarPlaylistsPorUsuario(usuarioId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioPlayListDTO> buscarPlaylistPorID(@PathVariable String id) {
        return usuarioPlaylistService.buscarPlaylistPorID(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioPlayListDTO> atualizarPlaylist(
            @PathVariable String id,
            @Valid @RequestBody UsuarioPlayListDTO dto) {
        try {
            return ResponseEntity.ok(usuarioPlaylistService.atualizarPlaylist(id, dto));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarPlaylist(@PathVariable String id) {
        if (usuarioPlaylistService.deletarPlaylist(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/{playlistId}/jogos/{jogoId}")
    public ResponseEntity<UsuarioPlayListDTO> adicionarJogo(
            @PathVariable String playlistId,
            @PathVariable String jogoId) {
        try {
            return ResponseEntity.ok(usuarioPlaylistService.adicionarJogo(playlistId, jogoId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{playlistId}/jogos/{jogoId}")
    public ResponseEntity<UsuarioPlayListDTO> removerJogo(
            @PathVariable String playlistId,
            @PathVariable String jogoId) {
        try {
            return ResponseEntity.ok(usuarioPlaylistService.removerJogo(playlistId, jogoId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}