package com.gamecontrol.controller;

import com.gamecontrol.dto.ReviewDTO;
import com.gamecontrol.dto.request.CreateReviewRequest;
import com.gamecontrol.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @PostMapping
    public ResponseEntity<String> createReview(@Valid @RequestBody CreateReviewRequest request) throws InterruptedException, ExecutionException {
        return ResponseEntity.ok(reviewService.saveReview(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReviewDTO> getReviewById(@PathVariable String id) throws InterruptedException, ExecutionException {
        ReviewDTO review = reviewService.getReviewById(id);
        return review != null ? ResponseEntity.ok(review) : ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> updateReview(@PathVariable String id, @Valid @RequestBody CreateReviewRequest request) throws InterruptedException, ExecutionException {
        String result = reviewService.updateReview(id, request);
        if (result.contains("Erro")) {
            return ResponseEntity.status(404).body(result);
        }
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteReview(@PathVariable String id) {
        return ResponseEntity.ok(reviewService.deleteReview(id));
    }
}   