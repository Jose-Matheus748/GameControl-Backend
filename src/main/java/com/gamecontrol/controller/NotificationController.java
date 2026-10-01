package com.gamecontrol.controller;

import com.gamecontrol.dto.NotificationDTO;
import com.gamecontrol.dto.request.RegisterDeviceTokenRequest;
import com.gamecontrol.service.NotificationService;
import com.gamecontrol.service.PushNotificationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final PushNotificationService pushNotificationService;

    public NotificationController(
            NotificationService notificationService,
            PushNotificationService pushNotificationService
    ) {
        this.notificationService = notificationService;
        this.pushNotificationService = pushNotificationService;
    }

    @GetMapping("/user/{userId}")
    public List<NotificationDTO> listar(@PathVariable String userId) {
        return notificationService.listarDoUsuario(userId);
    }

    @GetMapping("/user/{userId}/unread-count")
    public Map<String, Long> contarNaoLidas(@PathVariable String userId) {
        return Map.of("count", notificationService.contarNaoLidas(userId));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> marcarComoLida(@PathVariable String id) {
        notificationService.marcarComoLida(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/user/{userId}/read-all")
    public ResponseEntity<Void> marcarTodasComoLidas(@PathVariable String userId) {
        notificationService.marcarTodasComoLidas(userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/device-tokens")
    public ResponseEntity<Void> registrarToken(@Valid @RequestBody RegisterDeviceTokenRequest corpo) {
        pushNotificationService.registrarToken(corpo.getUserId(), corpo.getToken(), corpo.getPlatform());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/device-tokens")
    public ResponseEntity<Void> removerToken(@RequestParam String token) {
        pushNotificationService.removerToken(token);
        return ResponseEntity.noContent().build();
    }
}
