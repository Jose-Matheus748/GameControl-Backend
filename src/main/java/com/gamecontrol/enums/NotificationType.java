package com.gamecontrol.enums;

/**
 * Tipos de notificação do app. Novos eventos entram aqui e ganham um método
 * próprio no {@code NotificationService}. O app usa o valor (enviado também no
 * campo "type" do push) para decidir para qual tela navegar ao tocar nela.
 */
public enum NotificationType {
    NEW_FOLLOWER,
    POST_LIKED,
    POST_COMMENTED,
    FOLLOW_REQUEST,
    FOLLOW_REQUEST_ACCEPTED
}