package com.gamecontrol.controller;

import com.gamecontrol.dto.GameReviewsPageDTO;
import com.gamecontrol.dto.ReviewDTO;
import com.gamecontrol.dto.request.CreateReviewRequest;
import com.gamecontrol.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
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
    public ResponseEntity<ReviewDTO> createReview(
            @Valid @RequestBody CreateReviewRequest request
    ) {
        return ResponseEntity.ok(reviewService.saveReview(request));
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
    public ResponseEntity<String> deleteReview(@PathVariable String id) {
        return ResponseEntity.ok(reviewService.deleteReview(id));
    }

    @GetMapping("/{gameId}/reviews-page")
    public ResponseEntity<GameReviewsPageDTO> getReviewPage(@PathVariable String gameId) {
        try {
            GameReviewsPageDTO pageData = reviewService.getReviewPage(gameId);
            return ResponseEntity.ok(pageData);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}   