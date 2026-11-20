package com.gamecontrol.service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * Regras puras da paginação do feed (sem Firestore), para poderem ser testadas isoladamente.
 */
final class FeedPaginacao {

    static final int LIMITE_PADRAO = 10;
    static final int LIMITE_MAXIMO = 50;

    /**
     * Máximo de ids por consulta {@code whereIn}. O Firestore aceita até 30 hoje (já foi 10);
     * usar 10 funciona em qualquer versão, ao custo de mais consultas em paralelo.
     */
    static final int MAX_IDS_POR_CONSULTA = 10;

    private static final char SEPARADOR = '|';

    private FeedPaginacao() {
    }

    /** Posição de um post na ordenação do feed: {@code createdAt} e, no empate, o id. */
    record Cursor(String createdAt, String id) {
    }

    /** Limites inválidos (zero, negativo) viram o padrão; valores altos são cortados. */
    static int normalizarLimite(int pedido) {
        if (pedido <= 0) {
            return LIMITE_PADRAO;
        }
        return Math.min(pedido, LIMITE_MAXIMO);
    }

    static String codificarCursor(String createdAt, String id) {
        String bruto = createdAt + SEPARADOR + id;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bruto.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * @return o cursor, ou {@code null} se {@code texto} for nulo/vazio (primeira página)
     * @throws IllegalArgumentException se o texto não for um cursor válido
     */
    static Cursor decodificarCursor(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String bruto;
        try {
            bruto = new String(Base64.getUrlDecoder().decode(texto), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Cursor inválido.", e);
        }
        int separador = bruto.indexOf(SEPARADOR);
        if (separador <= 0 || separador == bruto.length() - 1) {
            throw new IllegalArgumentException("Cursor inválido.");
        }
        return new Cursor(bruto.substring(0, separador), bruto.substring(separador + 1));
    }

    /** Divide em lotes de no máximo {@code tamanho} itens, preservando a ordem. */
    static <T> List<List<T>> dividirEmLotes(Collection<T> itens, int tamanho) {
        if (tamanho <= 0) {
            throw new IllegalArgumentException("O tamanho do lote deve ser positivo.");
        }
        List<List<T>> lotes = new ArrayList<>();
        List<T> atual = new ArrayList<>(tamanho);
        for (T item : itens) {
            atual.add(item);
            if (atual.size() == tamanho) {
                lotes.add(atual);
                atual = new ArrayList<>(tamanho);
            }
        }
        if (!atual.isEmpty()) {
            lotes.add(atual);
        }
        return lotes;
    }

    /**
     * Junta resultados de várias consultas (uma por lote de autores) e devolve os
     * {@code quantidade} mais recentes. Vale porque cada consulta já trouxe, ordenados, os
     * seus próprios {@code quantidade} mais recentes: o topo da união está contido neles.
     *
     * <p>Ordem: {@code createdAt} decrescente (nulo por último) e, no empate, id decrescente —
     * a mesma ordem das consultas, para o cursor continuar valendo.
     */
    static <T> List<T> mesclarMaisRecentes(
            List<? extends List<? extends T>> listas,
            Function<T, String> createdAt,
            Function<T, String> id,
            int quantidade
    ) {
        Comparator<T> maisRecentePrimeiro = Comparator
                .comparing(createdAt, Comparator.nullsLast(Comparator.<String>reverseOrder()))
                .thenComparing(id, Comparator.nullsLast(Comparator.<String>reverseOrder()));

        List<T> todos = new ArrayList<>();
        for (List<? extends T> lista : listas) {
            todos.addAll(lista);
        }
        todos.sort(maisRecentePrimeiro);
        return new ArrayList<>(todos.subList(0, Math.min(quantidade, todos.size())));
    }
}
