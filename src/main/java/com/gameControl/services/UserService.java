package com.gameControl.services;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.gameControl.dto.UserDTO;
import com.gameControl.model.User;
import com.gameControl.repository.UserRepository;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<UserDTO> listarUsuarios() {
        List<User> usuariosListados = userRepository.findAll();

        return usuariosListados.stream()
        .map(this::toDTO)
        .toList();
    }

    public Optional<UserDTO> buscarUsuarioPorID(Long id) {
        return userRepository.findById(id).map(this::toDTO);
    }

    public UserDTO criarUsuario(User user) {
        User usuarioSalvo = userRepository.save(user);
        return toDTO(usuarioSalvo);
    }

    public UserDTO atualizarUsuario(Long id, User updatedUser) {
        return userRepository.findById(id)
        .map( user -> {
            user.setUsername(updatedUser.getUsername());
            user.setPassword(updatedUser.getPassword());
            user.setEmail(updatedUser.getEmail());
            user.setBio(updatedUser.getBio());
            user.setBirthDate(updatedUser.getBirthDate());
            user.setCountry(updatedUser.getCountry());
            user.setProfilePictureUrl(updatedUser.getProfilePictureUrl());
            user.setRole(updatedUser.getRole());

            User userSaved = userRepository.save(user);
            return toDTO(userSaved);
        }).orElseThrow(() -> new RuntimeException("Usuário não enconrado."));
    }

    public boolean deleteUsuario(Long id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return true;
        }
        
        return false;
    }

    // TODO: Colocar cryptografia.
    public Optional<UserDTO> login(String email, String password) {
        return userRepository.findByEmail(email)
            .filter(user -> user.getPassword().equals(password))
            .map(UserDTO::new);
    }

    private UserDTO toDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setUsername(user.getUsername());
        dto.setBio(user.getBio());
        dto.setProfilePictureUrl(user.getProfilePictureUrl());
        dto.setBirthDate(user.getBirthDate());
        dto.setCountry(user.getCountry());
        dto.setRole(user.getRole());

        return dto;
    }
}
