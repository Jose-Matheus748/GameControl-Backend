package com.gamecontrol.controller;

import com.gamecontrol.dto.CreateCommentRequest;
import com.gamecontrol.dto.GameCommentDTO;
import com.gamecontrol.service.GameCommentService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/gamecomments")
public class GameCommentsController {

    private final GameCommentService service;

    public GameCommentsController(GameCommentService service) {
        this.service = service;
    }

    @PostMapping
    public GameCommentDTO create(@RequestBody CreateCommentRequest req) {
        return service.createComment(req);
    }

    @GetMapping("/game/{gameId}")
    public List<GameCommentDTO> byGame(@PathVariable String gameId) {
        return service.getCommentsByGame(gameId);
    }

    @GetMapping("/user/{userId}")
    public List<GameCommentDTO> byUser(@PathVariable String userId) {
        return service.getCommentsByUser(userId);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        service.deleteComment(id);
    }
}