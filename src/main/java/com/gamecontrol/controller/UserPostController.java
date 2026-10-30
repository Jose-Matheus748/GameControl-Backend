package com.gamecontrol.controller;

import com.gamecontrol.config.AuthContext;
import com.gamecontrol.dto.UserPostDTO;
import com.gamecontrol.dto.request.CreateUserPostRequest;
import com.gamecontrol.dto.request.UpdateUserPostRequest;
import com.gamecontrol.service.UserPostService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
public class UserPostController {

    private final UserPostService service;

    public UserPostController(UserPostService service) {
        this.service = service;
    }

    @PostMapping
    public UserPostDTO create(@RequestBody CreateUserPostRequest req) {
        return service.createPost(req);
    }

    @GetMapping
    public List<UserPostDTO> getAll() {
        return service.getAllPosts(AuthContext.uidOuNulo());
    }

    @GetMapping("/user/{userId}")
    public List<UserPostDTO> getByUser(@PathVariable String userId) {
        return service.getPostsByUser(userId, AuthContext.uidOuNulo());
    }

    /** Edita o texto do post. Só o autor pode editar. */
    @PutMapping("/{id}")
    public UserPostDTO update(
            @PathVariable String id,
            @Valid @RequestBody UpdateUserPostRequest req
    ) {
        return service.updatePost(id, req, AuthContext.uidOuNulo());
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        service.deletePost(id);
    }

    @PostMapping("/{postId}/like/{userId}")
    public void toggleLike(
            @PathVariable String postId,
            @PathVariable String userId
    ) {
        service.toggleLike(postId, userId);
    }
}