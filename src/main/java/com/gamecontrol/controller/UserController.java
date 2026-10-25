package com.gamecontrol.controller;

import com.gamecontrol.dto.request.FollowRequestDTO;
import com.gamecontrol.dto.FollowStatusDTO;
import com.gamecontrol.dto.UserDTO;
import com.gamecontrol.dto.request.ChangePasswordRequest;
import com.gamecontrol.dto.request.CreateUserProfileRequest;
import com.gamecontrol.enums.FollowResult;
import com.gamecontrol.service.FollowRequestService;
import com.gamecontrol.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final FollowRequestService followRequestService;

    public UserController(UserService userService, FollowRequestService followRequestService) {
        this.userService = userService;
        this.followRequestService = followRequestService;
    }

    @GetMapping
    public ResponseEntity<List<UserDTO>> listarUsuarios() {
        return ResponseEntity.ok(userService.listarUsuarios());
    }

    /**
     * Cria o perfil de uma conta que já foi cadastrada no Firebase Authentication pelo cliente.
     * O UID é obtido da autenticação validada, nunca do corpo da requisição.
     */
    @PostMapping
    public ResponseEntity<UserDTO> cadastrarUsuario(@Valid @RequestBody CreateUserProfileRequest corpo) {
        String uid = uidAutenticado();
        UserDTO criado = userService.cadastrarUsuario(corpo, uid);
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
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
        garantirDono(id);
        UserDTO atualizado = userService.atualizarUsuario(id, usuarioAtualizado);
        return ResponseEntity.ok(atualizado);
    }

    @PutMapping("/{id}/profile-picture")
    public ResponseEntity<UserDTO> updateProfilePicture(
            @PathVariable String id,
            @RequestBody Map<String, String> body
    ) {
        garantirDono(id);
        String base64Image = body.get("profilePictureUrl");
        UserDTO atualizado = userService.atualizarFotoPerfil(id, base64Image);
        return ResponseEntity.ok(atualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarUsuario(@PathVariable String id) {
        garantirDono(id);
        userService.deletarUsuario(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Mantido temporariamente para compatibilidade. No fluxo final, prefira atualizar a senha
     * diretamente pelo Firebase SDK do cliente, que trata a exigência de login recente.
     */
    @PutMapping("/{id}/change-password")
    public ResponseEntity<Void> alterarSenha(
            @PathVariable String id,
            @Valid @RequestBody ChangePasswordRequest corpo
    ) {
        garantirDono(id);
        userService.alterarSenha(id, corpo);
        return ResponseEntity.noContent().build();
    }

    /**
     * 201 = passou a seguir. 202 = o perfil é privado e ficou um pedido pendente
     * (corpo: {"status":"REQUESTED"}). 429 = pedido recusado há pouco, aguarde para tentar de novo.
     */
    @PostMapping("/{userId}/follow/{targetUserId}")
    public ResponseEntity<?> follow(
            @PathVariable String userId,
            @PathVariable String targetUserId
    ) {
        garantirDono(userId);
        FollowResult resultado = userService.followUser(userId, targetUserId);
        if (resultado == FollowResult.REQUESTED) {
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of("status", "REQUESTED"));
        }
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /** Relação de {@code userId} com {@code targetUserId}: FOLLOWING, REQUESTED ou NONE. */
    @GetMapping("/{userId}/follow-status/{targetUserId}")
    public ResponseEntity<FollowStatusDTO> statusSeguir(
            @PathVariable String userId,
            @PathVariable String targetUserId
    ) {
        garantirDono(userId);
        return ResponseEntity.ok(followRequestService.status(userId, targetUserId));
    }

    /** Pedidos para seguir pendentes recebidos por este usuário (perfil privado). */
    @GetMapping("/{userId}/follow-requests")
    public ResponseEntity<List<FollowRequestDTO>> listarPedidos(@PathVariable String userId) {
        garantirDono(userId);
        return ResponseEntity.ok(followRequestService.listarRecebidas(userId));
    }

    @PostMapping("/{userId}/follow-requests/{requesterId}/accept")
    public ResponseEntity<Void> aceitarPedido(
            @PathVariable String userId,
            @PathVariable String requesterId
    ) {
        garantirDono(userId);
        followRequestService.aceitar(userId, requesterId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{userId}/follow-requests/{requesterId}/reject")
    public ResponseEntity<Void> recusarPedido(
            @PathVariable String userId,
            @PathVariable String requesterId
    ) {
        garantirDono(userId);
        followRequestService.recusar(userId, requesterId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{userId}/follow/{targetUserId}")
    public ResponseEntity<Void> unfollow(
            @PathVariable String userId,
            @PathVariable String targetUserId
    ) {
        garantirDono(userId);
        userService.unfollowUser(userId, targetUserId);
        return ResponseEntity.noContent().build();
    }

    private String uidAutenticado() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao == null
                || !autenticacao.isAuthenticated()
                || autenticacao.getPrincipal() == null
                || "anonymousUser".equals(autenticacao.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Não autenticado.");
        }
        return autenticacao.getPrincipal().toString();
    }

    private void garantirDono(String id) {
        String uidAutenticado = uidAutenticado();
        if (!id.equals(uidAutenticado)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Você só pode alterar os seus próprios dados."
            );
        }
    }
}
