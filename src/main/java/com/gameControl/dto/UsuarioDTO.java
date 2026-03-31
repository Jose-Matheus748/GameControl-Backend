package com.gameControl.dto;

import java.time.LocalDate;

import com.gameControl.enums.Role;
import com.gameControl.model.Usuario;

public class UsuarioDTO {
    private Long id;
    private String email;
    private String nomeUsuario;
    private String bio;
    private String urlFotoPerfil;
    private LocalDate dataNascimento;
    private String pais;
    private Role role;

    public UsuarioDTO() {}

    public UsuarioDTO(Usuario usuario) {
        this.id = usuario.getId();
        this.email = usuario.getEmail();
        this.nomeUsuario = usuario.getNomeUsuario();
        this.bio = usuario.getBio();
        this.urlFotoPerfil = usuario.getUrlFotoPerfil();
        this.dataNascimento = usuario.getDataNascimento();
        this.pais = usuario.getPais();
        this.role = usuario.getRole();
    }

    public UsuarioDTO(Long id, String email, String nomeUsuario, String bio, String urlFotoPerfil,
                      LocalDate dataNascimento, String pais, Role role) {
        this.id = id;
        this.email = email;
        this.nomeUsuario = nomeUsuario;
        this.bio = bio;
        this.urlFotoPerfil = urlFotoPerfil;
        this.dataNascimento = dataNascimento;
        this.pais = pais;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNomeUsuario() {
        return nomeUsuario;
    }

    public void setNomeUsuario(String nomeUsuario) {
        this.nomeUsuario = nomeUsuario;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getUrlFotoPerfil() {
        return urlFotoPerfil;
    }

    public void setUrlFotoPerfil(String urlFotoPerfil) {
        this.urlFotoPerfil = urlFotoPerfil;
    }

    public LocalDate getDataNascimento() {
        return dataNascimento;
    }

    public void setDataNascimento(LocalDate dataNascimento) {
        this.dataNascimento = dataNascimento;
    }

    public String getPais() {
        return pais;
    }

    public void setPais(String pais) {
        this.pais = pais;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}
