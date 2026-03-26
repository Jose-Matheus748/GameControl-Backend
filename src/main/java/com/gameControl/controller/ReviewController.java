package com.gameControl.controller;

import com.gameControl.model.Review;
import com.gameControl.services.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {
    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/game/{gameId}")
    public ResponseEntity<List<Review>> listarReviewsPorJogo(@PathVariable Long gameId) {
        List<Review> reviews = reviewService.listarReviewsPorJogo(gameId);
        return ResponseEntity.ok(reviews);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Review> buscarReviewPorID(@PathVariable Long id) {
        Optional<Review> review = reviewService.buscarReviewPorID(id);
        return review.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Review> criarReview(
            @RequestParam Long gameId,
            @RequestParam Long userId,
            @Valid @RequestBody Review review) {
        try {
            Review novaReview = reviewService.criarReview(gameId, userId, review);
            return ResponseEntity.status(HttpStatus.CREATED).body(novaReview);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Review> atualizarReview(@PathVariable Long id, @Valid @RequestBody Review review) {
        try {
            Review reviewAtualizada = reviewService.atualizarReview(id, review);
            return ResponseEntity.ok(reviewAtualizada);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarReview(@PathVariable Long id) {
        boolean deletado = reviewService.deletarReview(id);
        if (deletado) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
