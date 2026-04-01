package com.gamecontrol.controller;

import com.gamecontrol.dto.FollowRequest;
import com.gamecontrol.service.FollowService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/follow")
public class FollowController {

    private final FollowService followService;

    public FollowController(FollowService followService) {
        this.followService = followService;
    }

    @PostMapping
    public ResponseEntity<Void> follow(@RequestBody FollowRequest request) {
        try {
            followService.follow(request.getFollowerId(), request.getFollowingId());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping
    public ResponseEntity<Void> unfollow(@RequestBody FollowRequest request) {
        try {
            followService.unfollow(request.getFollowerId(), request.getFollowingId());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
