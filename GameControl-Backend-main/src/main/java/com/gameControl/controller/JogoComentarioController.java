package com.gameControl.controller;

import com.gameControl.model.JogoComentario;
import com.gameControl.services.JogoComentarioService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jogoscomentarios")
public class JogoComentarioController {
    private final JogoComentarioService jogoComentarioService;

    public JogoComentarioController(JogoComentarioService jogoComentarioService) {
        this.jogoComentarioService = jogoComentarioService;
    }

    @PostMapping
    public JogoComentario criarComentario(@RequestParam Long usuarioId,
                                          @RequestParam Long jogoId,
                                          @RequestParam String conteudo) {
        return jogoComentarioService.criarComentario(usuarioId, jogoId, conteudo);
    }

    @GetMapping("/jogo/{jogoId}")
    public List<JogoComentario> listarComentariosPorJogo(@PathVariable Long jogoId) {
        return jogoComentarioService.listarComentariosPorJogo(jogoId);
    }

    @GetMapping("/usuario/{usuarioId}")
    public List<JogoComentario> listarComentariosPorUsuario(@PathVariable Long usuarioId) {
        return jogoComentarioService.listarComentariosPorUsuario(usuarioId);
    }

    @DeleteMapping("/{comentarioId}")
    public boolean deletarComentario(@PathVariable Long comentarioId) {
        return jogoComentarioService.deletarComentario(comentarioId);
    }
}