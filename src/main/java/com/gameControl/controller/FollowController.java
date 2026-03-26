package com.gameControl.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gameControl.dto.UserFollowDTO;
import com.gameControl.services.FollowService;

@RestController
@RequestMapping("/api/follow")
public class FollowController {
    private final FollowService followService;

    public FollowController(FollowService followService) {
        this.followService = followService;
    }

    @PostMapping("/{followerId}/follow/{followedId}")
    public ResponseEntity<String> followUser(@PathVariable Long followerId, @PathVariable Long followedId) {
        followService.followUser(followerId, followedId);
        return ResponseEntity.ok("Usuário seguido com sucesso.");
    }

    @DeleteMapping("/{followerId}/unfollow/{followedId}")
    public ResponseEntity<String> unfollowUser(@PathVariable Long followerId, @PathVariable Long followedId) {
        followService.unfollowUser(followerId, followedId);
        return ResponseEntity.ok("Usuário deixado de seguir com sucesso.");
    }

    @GetMapping("/{userId}/followers")
    public ResponseEntity<List<UserFollowDTO>> getFollowers(@PathVariable Long userId) {
        return ResponseEntity.ok(followService.getFollowers(userId));
    }

    @GetMapping("/{userId}/following")
    public ResponseEntity<List<UserFollowDTO>> getFollowing(@PathVariable Long userId) {
        return ResponseEntity.ok(followService.getFollowing(userId));
    }
}
