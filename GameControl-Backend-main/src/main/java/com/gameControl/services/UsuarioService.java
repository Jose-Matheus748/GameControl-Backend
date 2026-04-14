package com.gameControl.services;

import java.util.List;
import java.util.Optional;

import com.gameControl.model.Usuario;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.gameControl.dto.UsuarioDTO;
import com.gameControl.repository.UsuarioRepository;

@Service
public class UsuarioService {
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UsuarioDTO> listarUsuarios() {
        List<Usuario> usuariosListados = usuarioRepository.findAll();

        return usuariosListados.stream()
        .map(this::toDTO)
        .toList();
    }

    public Optional<UsuarioDTO> buscarUsuarioPorID(Long id) {
        return usuarioRepository.findById(id).map(this::toDTO);
    }

    public UsuarioDTO criarUsuario(Usuario usuario) {
        String senhaCriptografada = passwordEncoder.encode(usuario.getSenha());
        usuario.setSenha(senhaCriptografada);
        Usuario usuarioSalvo = usuarioRepository.save(usuario);
        return toDTO(usuarioSalvo);
    }

    public UsuarioDTO atualizarUsuario(Long id, Usuario updatedUsuario) {
        return usuarioRepository.findById(id)
        .map( user -> {
            user.setNomeUsuario(updatedUsuario.getNomeUsuario());


            if (updatedUsuario.getSenha() != null && !updatedUsuario.getSenha().isEmpty()) {
                user.setSenha(passwordEncoder.encode(updatedUsuario.getSenha()));
            }

            user.setEmail(updatedUsuario.getEmail());
            user.setBio(updatedUsuario.getBio());
            user.setDataNascimento(updatedUsuario.getDataNascimento());
            user.setPais(updatedUsuario.getPais());
            user.setUrlFotoPerfil(updatedUsuario.getUrlFotoPerfil());
            user.setRole(updatedUsuario.getRole());

            Usuario usuarioSaved = usuarioRepository.save(user);
            return toDTO(usuarioSaved);
        }).orElseThrow(() -> new RuntimeException("Usuário não encontrado."));
    }

    public boolean deleteUsuario(Long id) {
        if (usuarioRepository.existsById(id)) {
            usuarioRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public Optional<UsuarioDTO> login(String email, String password) {
        return usuarioRepository.findByEmail(email)
                .filter(user -> passwordEncoder.matches(password, user.getSenha())) // Compara o recheio
                .map(this::toDTO);
    }

    private UsuarioDTO toDTO(Usuario usuario) {
        UsuarioDTO dto = new UsuarioDTO();
        dto.setId(usuario.getId());
        dto.setEmail(usuario.getEmail());
        dto.setNomeUsuario(usuario.getNomeUsuario());
        dto.setBio(usuario.getBio());
        dto.setUrlFotoPerfil(usuario.getUrlFotoPerfil());
        dto.setDataNascimento(usuario.getDataNascimento());
        dto.setPais(usuario.getPais());
        dto.setRole(usuario.getRole());
        return dto;
    }
}
