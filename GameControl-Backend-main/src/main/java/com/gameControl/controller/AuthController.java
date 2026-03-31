package com.gameControl.controller;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gameControl.dto.AuthResponse;
import com.gameControl.dto.LoginDTO;
import com.gameControl.dto.UsuarioDTO;
import com.gameControl.services.UsuarioService;
import com.gameControl.utils.JwtUtil;


@RestController
@RequestMapping
public class AuthController {

    private final UsuarioService usuarioService;

    public AuthController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginDTO loginDTO) {
        Optional<UsuarioDTO> userOpt = usuarioService.login(loginDTO.getEmail(), loginDTO.getPassword());
        if(userOpt.isPresent()) {
            String token = JwtUtil.generateToken(userOpt.get());
            return ResponseEntity.ok(new AuthResponse(userOpt.get(), token));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credenciais inválidas");
    }

}
