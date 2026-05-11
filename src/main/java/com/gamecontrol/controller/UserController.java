package com.gamecontrol.controller;

import com.gamecontrol.dto.AuthResponse;
import com.gamecontrol.dto.request.CreateUserRequest;
import com.gamecontrol.dto.request.LoginRequest;
import com.gamecontrol.dto.UserDTO;
import com.gamecontrol.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<UserDTO>> listarUsuarios() {
        return ResponseEntity.ok(userService.listarUsuarios());
    }

    @PostMapping
    public ResponseEntity<UserDTO> cadastrarUsuario(@Valid @RequestBody CreateUserRequest corpo) {
        UserDTO criado = userService.cadastrarUsuario(corpo);
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest corpo) {
        return ResponseEntity.ok(userService.login(corpo));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> buscarPorId(@PathVariable String id) {
        return ResponseEntity.ok(userService.buscarUsuarioPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> atualizarUsuario(
            @PathVariable String id,
            @RequestBody UserDTO usuarioAtualizado
    ) {
        UserDTO atualizado = userService.atualizarUsuario(id, usuarioAtualizado);
        return ResponseEntity.ok(atualizado);
    }

    @PutMapping("/{id}/profile-picture")
    public ResponseEntity<UserDTO> updateProfilePicture(
            @PathVariable String id,
            @RequestBody Map<String, String> body
    ) {
        String base64Image = body.get("profilePictureUrl");
        UserDTO atualizado = userService.atualizarFotoPerfil(id, base64Image);
        return ResponseEntity.ok(atualizado);
    }

    @PostMapping("/{userId}/follow/{targetUserId}")
    public ResponseEntity<Void> follow(
            @PathVariable String userId,
            @PathVariable String targetUserId) {
        userService.followUser(userId, targetUserId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{userId}/follow/{targetUserId}")
    public ResponseEntity<Void> unfollow(
            @PathVariable String userId,
            @PathVariable String targetUserId) {
        userService.unfollowUser(userId, targetUserId);
        return ResponseEntity.noContent().build();
    }
}
