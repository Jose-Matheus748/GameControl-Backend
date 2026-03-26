package com.gameControl.controller;

import java.util.List;
import java.util.Optional;

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

import com.gameControl.dto.UserDTO;
import com.gameControl.model.User;
import com.gameControl.services.UserService;

@RestController
@RequestMapping("user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/")
    public List<UserDTO> listarUsuarios() {
        return userService.listarUsuarios();
    }

    @GetMapping("/{id}")
    public Optional<UserDTO> buscarUserPorId(@PathVariable Long id) {
        return userService.buscarUsuarioPorID(id);
    }

    @PutMapping("/{id}")
    public UserDTO atualizarUsuario(@PathVariable Long id, @RequestBody User user) {
        return userService.atualizarUsuario(id, user);
    }

    @PostMapping("/")
    public UserDTO criarUsuario(@RequestBody User user) {
        return userService.criarUsuario(user);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteUsuario(@PathVariable Long id) {
        boolean deleted = userService.deleteUsuario(id);
        if (deleted) {
            return ResponseEntity.ok("Usuário deletado com sucesso!");
        }
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuário não encontrado!");
    }
}
