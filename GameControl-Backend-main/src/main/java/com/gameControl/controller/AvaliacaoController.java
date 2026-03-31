package com.gameControl.controller;

import com.gameControl.model.Avaliacao;
import com.gameControl.services.AvaliacaoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/avaliacoes")
public class AvaliacaoController {
    private final AvaliacaoService avaliacaoService;

    public AvaliacaoController(AvaliacaoService avaliacaoService) {
        this.avaliacaoService = avaliacaoService;
    }

    @GetMapping("/jogo/{jogoId}")
    public ResponseEntity<List<Avaliacao>> listarReviewsPorJogo(@PathVariable Long jogoId) {
        List<Avaliacao> avaliacoes = avaliacaoService.listarReviewsPorJogo(jogoId);
        return ResponseEntity.ok(avaliacoes);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Avaliacao> buscarReviewPorID(@PathVariable Long id) {
        Optional<Avaliacao> review = avaliacaoService.buscarReviewPorID(id);
        return review.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Avaliacao> criarAvaliacao(
            @RequestParam Long jogoId,
            @RequestParam Long usuarioId,
            @Valid @RequestBody Avaliacao avaliacao) {
        try {
            Avaliacao novaAvaliacao = avaliacaoService.criarReview(jogoId, usuarioId, avaliacao);
            return ResponseEntity.status(HttpStatus.CREATED).body(novaAvaliacao);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Avaliacao> atualizarReview(@PathVariable Long id, @Valid @RequestBody Avaliacao avaliacaoCorpo) {
        try {
            Avaliacao avaliacaoAtualizada = avaliacaoService.atualizarAvaliacao(id, avaliacaoCorpo);
            return ResponseEntity.ok(avaliacaoAtualizada);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarReview(@PathVariable Long id) {
        boolean deletado = avaliacaoService.deletarReview(id);
        if (deletado) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
