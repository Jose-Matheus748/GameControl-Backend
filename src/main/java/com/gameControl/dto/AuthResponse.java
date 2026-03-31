package com.gameControl.dto;

public class AuthResponse {
    private UsuarioDTO user;
    private String token;

    public AuthResponse() {}

    public AuthResponse(UsuarioDTO user, String token) {
        this.user = user;
        this.token = token;
    }

    public UsuarioDTO getUser() {
        return user;
    }

    public void setUser(UsuarioDTO user) {
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
