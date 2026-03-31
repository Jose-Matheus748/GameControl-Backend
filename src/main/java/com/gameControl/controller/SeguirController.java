package com.gameControl.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gameControl.dto.UsuarioSeguidorDTO;
import com.gameControl.services.SeguirService;

@RestController
@RequestMapping("/api/seguir")
public class SeguirController {
    private final SeguirService seguirService;

    public SeguirController(SeguirService seguirService) {
        this.seguirService = seguirService;
    }

    @PostMapping("/{seguidorId}/seguir/{seguidoId}")
    public ResponseEntity<String> seguirUsuario(@PathVariable Long seguidorId, @PathVariable Long seguidoId) {
        seguirService.seguirUsuario(seguidorId, seguidoId);
        return ResponseEntity.ok("Usuário seguido com sucesso.");
    }

    @DeleteMapping("/{seguidorId}/deixar-de-seguir/{seguidoId}")
    public ResponseEntity<String> deixarDeSeguir(@PathVariable Long seguidorId, @PathVariable Long seguidoId) {
        seguirService.deixarDeSeguir(seguidorId, seguidoId);
        return ResponseEntity.ok("Usuário deixado de seguir com sucesso.");
    }

    @GetMapping("/{usuarioId}/seguidores")
    public ResponseEntity<List<UsuarioSeguidorDTO>> listarSeguidores(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(seguirService.listarSeguidores(usuarioId));
    }

    @GetMapping("/{usuarioId}/seguindo")
    public ResponseEntity<List<UsuarioSeguidorDTO>> listarSeguindo(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(seguirService.listarSeguindo(usuarioId));
    }
}