package com.gamecontrol.controller;

import com.gamecontrol.service.FollowService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/follow")
public class FollowController {

    private final FollowService followService;

    public FollowController(FollowService followService) {
        this.followService = followService;
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