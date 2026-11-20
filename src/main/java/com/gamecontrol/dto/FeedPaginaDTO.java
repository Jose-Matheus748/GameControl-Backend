package com.gamecontrol.dto;

import java.util.List;

public record FeedPaginaDTO<T>(
        List<T> itens,
        String proximoCursor,
        boolean temMais
) {
}
