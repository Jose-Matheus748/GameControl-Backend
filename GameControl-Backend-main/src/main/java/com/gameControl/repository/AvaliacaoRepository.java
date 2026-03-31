package com.gameControl.repository;

import com.gameControl.model.Avaliacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Long> {

    // Buscar todas as avaliações de um jogo específico
    List<Avaliacao> findByJogoId(Long jogoId);

    // Buscar a avaliação de um usuário para um jogo específico (para garantir que ele só avalie uma vez)
    Optional<Avaliacao> findByJogoIdAndUsuarioId(Long jogoId, Long usuarioId);
}
