package com.gameControl.repository;

import com.gameControl.model.JogoComentario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JogoComentarioRepository extends JpaRepository<JogoComentario, Long> {
    List<JogoComentario> findByJogoId(Long jogoId);
    List<JogoComentario> findByUsuarioId(Long usuarioId);
}
