package com.gamecontrol.controller;

import com.gamecontrol.dto.FollowRequest;
import com.gamecontrol.service.FollowService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/follow")
public class FollowController {

    private final FollowService followService;

    public FollowController(FollowService followService) {
        this.followService = followService;
    }

    /**
     * Registra que {@code followerId} passa a seguir {@code followedId}.
     * Grava em Firestore na coleção {@code follows}, documento id = followerId_followedId.
     */
    @PostMapping
    public ResponseEntity<Void> seguir(@RequestBody FollowRequest corpo) {
        if (corpo.getFollowerId() == null || corpo.getFollowerId().isBlank()
                || corpo.getFollowedId() == null || corpo.getFollowedId().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        if (corpo.getFollowerId().equals(corpo.getFollowedId())) {
            return ResponseEntity.badRequest().build();
        }
        try {
            followService.follow(corpo.getFollowerId().trim(), corpo.getFollowedId().trim());
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.internalServerError().build();
        } catch (ExecutionException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{userId}/seguidores")
    public ResponseEntity<List<String>> listarSeguidores(@PathVariable String userId) {
        try {
            return ResponseEntity.ok(followService.getSeguidores(userId));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{userId}/seguindo")
    public ResponseEntity<List<String>> listarSeguindo(@PathVariable String userId) {
        try {
            return ResponseEntity.ok(followService.getSeguindo(userId));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}