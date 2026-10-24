package com.gamecontrol.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

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
}
