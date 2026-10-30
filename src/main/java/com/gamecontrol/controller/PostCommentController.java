package com.gamecontrol.controller;

import com.gamecontrol.config.AuthContext;
import com.gamecontrol.dto.PostCommentDTO;
import com.gamecontrol.dto.request.CreatePostCommentRequest;
import com.gamecontrol.dto.request.UpdatePostCommentRequest;
import com.gamecontrol.service.PostCommentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/postcomments")
public class PostCommentController {

    private final PostCommentService service;

    public PostCommentController(PostCommentService service) {
        this.service = service;
    }

    @PostMapping
    public PostCommentDTO create(@RequestBody CreatePostCommentRequest req) {
        return service.createComment(req);
    }

    @GetMapping("/post/{postId}")
    public List<PostCommentDTO> byPost(@PathVariable String postId) {
        return service.getCommentsByPost(postId);
    }

    @GetMapping("/user/{userId}")
    public List<PostCommentDTO> byUser(@PathVariable String userId) {
        return service.getCommentsByUser(userId);
    }

    /** Edita o conteúdo do comentário. Só o autor do comentário pode editar. */
    @PutMapping("/{id}")
    public PostCommentDTO update(
            @PathVariable String id,
            @Valid @RequestBody UpdatePostCommentRequest req
    ) {
        return service.updateComment(id, req, AuthContext.uidOuNulo());
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        service.deleteComment(id);
    }
}