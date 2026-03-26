package com.gameControl.controller;

import com.gameControl.model.GameComment;
import com.gameControl.services.GameCommentService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/gamecomments")
public class GameCommentsController {
    private final GameCommentService gameCommentService;

    public GameCommentsController(GameCommentService gameCommentService) {
        this.gameCommentService = gameCommentService;
    }

    @PostMapping
    public GameComment createComment(@RequestParam Long userId,
                                     @RequestParam Long gameId,
                                     @RequestParam String content) {
        return gameCommentService.createComment(userId, gameId, content);
    }

    @GetMapping("/game/{gameId}")
    public List<GameComment> getCommentsByGame(@PathVariable Long gameId) {
        return gameCommentService.getCommentsByGame(gameId);
    }

    @GetMapping("/user/{userId}")
    public List<GameComment> getCommentsByUser(@PathVariable Long userId) {
        return gameCommentService.getCommentsByUser(userId);
    }

    @DeleteMapping("/{commentId}")
    public boolean deleteComment(@PathVariable Long commentId) {
        return gameCommentService.deleteComment(commentId);
    }
}
