package com.gameControl.services;

import java.util.List;
import java.util.stream.Collectors;

import com.gameControl.model.Usuario;
import org.springframework.stereotype.Service;

import com.gameControl.dto.UsuarioSeguidorDTO;
import com.gameControl.model.Seguir;
import com.gameControl.repository.SeguirRepository;
import com.gameControl.repository.UsuarioRepository;

@Service
public class SeguirService {
    private final SeguirRepository seguirRepository;
    private final UsuarioRepository usuarioRepository;

    public SeguirService(SeguirRepository seguirRepository, UsuarioRepository usuarioRepository) {
        this.seguirRepository = seguirRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public void seguirUsuario(Long seguidorId, Long seguidoId) {
        if (seguidorId.equals(seguidoId)) {
            throw new IllegalArgumentException("Um usuário não pode seguir a si mesmo.");
        }

        Usuario seguido = usuarioRepository.findById(seguidoId)
                .orElseThrow(() -> new RuntimeException("Usuário seguidor não encontrado."));

        Usuario seguidor = usuarioRepository.findById(seguidoId)
                .orElseThrow(() -> new RuntimeException("Usuário a ser seguido não encontrado."));

        seguirRepository.findBySeguidorAndSeguido(seguidor, seguido)
                .ifPresent(f -> { throw new RuntimeException("Usuário já está seguindo este perfil."); });

        Seguir seguir = Seguir.builder()
                .seguidor(seguidor)
                .seguido(seguido)
                .build();

        seguirRepository.save(seguir);
    }

    public void deixarDeSeguir(Long seguidorId, Long seguidoId) {
        Usuario seguidor = usuarioRepository.findById(seguidorId)
                .orElseThrow(() -> new RuntimeException("Usuário seguidor não encontrado."));
        Usuario seguido = usuarioRepository.findById(seguidoId)
                .orElseThrow(() -> new RuntimeException("Usuário a ser seguido não encontrado."));

        Seguir seguir = seguirRepository.findBySeguidorAndSeguido(seguidor, seguido)
                .orElseThrow(() -> new RuntimeException("Usuário não está seguindo este perfil."));

        seguirRepository.delete(seguir);
    }

    // public List<Seguir> listarSeguidores(Long usuarioId) {
    //     Usuario usuario = usuarioRepository.findById(usuarioId)
    //         .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));
    //     return seguirRepository.findBySeguido(usuario);
    // }

    // public List<Seguir> listarSeguindo(Long usuarioId) {
    //     Usuario usuario = usuarioRepository.findById(usuarioId)
    //         .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));
    //     return seguirRepository.findBySeguidor(usuario);
    // }

    public List<UsuarioSeguidorDTO> listarSeguidores(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        return seguirRepository.findBySeguido(usuario)
                .stream()
                .map(s -> new UsuarioSeguidorDTO(
                        s.getSeguidor().getId(),
                        s.getSeguidor().getNomeUsuario(),
                        s.getSeguidor().getUrlFotoPerfil()
                ))
                .collect(Collectors.toList());
    }

    public List<UsuarioSeguidorDTO> listarSeguindo(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        return seguirRepository.findBySeguidor(usuario)
                .stream()
                .map(s -> new UsuarioSeguidorDTO(
                        s.getSeguido().getId(),
                        s.getSeguido().getNomeUsuario(),
                        s.getSeguido().getUrlFotoPerfil()
                ))
                .collect(Collectors.toList());
    }
}
