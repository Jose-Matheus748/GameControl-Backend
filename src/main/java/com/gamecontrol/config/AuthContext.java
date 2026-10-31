package com.gamecontrol.config;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

/** Acesso ao usuário autenticado (uid do Firebase) da requisição atual. */
public final class AuthContext {

    private AuthContext() {
    }

    /**
     * Uid de quem fez a requisição, ou {@code null} se a requisição é anônima
     * (sem token, ou com token inválido/expirado).
     */
    public static String uidOuNulo() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao == null
                || !autenticacao.isAuthenticated()
                || autenticacao.getPrincipal() == null
                || "anonymousUser".equals(autenticacao.getPrincipal())) {
            return null;
        }
        return autenticacao.getPrincipal().toString();
    }

    /**
     * Uid de quem fez a requisição. Responde 401 se a requisição é anônima, para
     * que operações que agem "em nome de alguém" nunca confiem em um id vindo do cliente.
     */
    public static String uidObrigatorio() {
        String uid = uidOuNulo();
        if (uid == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Não autenticado.");
        }
        return uid;
    }
}
