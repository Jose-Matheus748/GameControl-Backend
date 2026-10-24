package com.gamecontrol.dto;

/**
 * Relação de quem consulta com outro usuário, para o app decidir o botão.
 *
 * @param status             FOLLOWING (já segue), REQUESTED (pedido pendente) ou NONE
 * @param retryAfterSeconds  se o último pedido foi recusado, quantos segundos faltam para
 *                           poder pedir de novo (0 = já pode)
 */
public record FollowStatusDTO(String status, long retryAfterSeconds) {
}
