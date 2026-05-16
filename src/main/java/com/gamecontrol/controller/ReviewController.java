package com.gamecontrol.controller;

import com.gamecontrol.dto.GameReviewsPageDTO;
import com.gamecontrol.dto.ReviewDTO;
import com.gamecontrol.dto.request.CreateReviewRequest;
import com.gamecontrol.service.ReviewService;
import com.google.api.Http;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.concurrent.ExecutionException;


@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @PostMapping
    public ResponseEntity<?> saveReview(
            @RequestBody CreateReviewRequest request,
            @RequestParam String userId) {
        try {
            ReviewDTO savedReview = reviewService.saveReview(request, userId);
            return ResponseEntity.ok(savedReview);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Erro ao salvar avaliação.");
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReviewDTO> getReviewById(@PathVariable String id) throws InterruptedException, ExecutionException {
        ReviewDTO review = reviewService.getReviewById(id);
        return review != null ? ResponseEntity.ok(review) : ResponseEntity.notFound().build();
    }

    @GetMapping("/game/{gameId}")
    public ResponseEntity<List<ReviewDTO>> getReviewsByGame(@PathVariable String gameId)
            throws Exception {

        return ResponseEntity.ok(reviewService.getReviewsByGame(gameId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> updateReview(@PathVariable String id, @Valid @RequestBody CreateReviewRequest request) throws InterruptedException, ExecutionException {
        String result = reviewService.updateReview(id, request);
        if (result.contains("Erro")) {
            return ResponseEntity.status(404).body(result);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/game/{gameId}/average")
    public ResponseEntity<Double> getAverageRating(@PathVariable String gameId){
        try {
            Double average = reviewService.getAverageRating(gameId);
            return ResponseEntity.ok(average);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteReview(
            @PathVariable String id,
            @RequestParam String userId) {
        try {
            String response = reviewService.deleteReview(id, userId);
            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Erro ao processar a exclusão.");
        }
    }

    @GetMapping("/{gameId}/reviews-page")
    public ResponseEntity<GameReviewsPageDTO> getReviewPage(
            @PathVariable String gameId,
            @RequestParam(required = false) String userId) {
        try {
            GameReviewsPageDTO pageData = reviewService.getReviewPage(gameId, userId);
            return ResponseEntity.ok(pageData);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}   