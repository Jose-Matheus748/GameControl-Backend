package com.gameControl.services;

import com.gameControl.model.Jogo;
import com.gameControl.model.Avaliacao;
import com.gameControl.model.Usuario;
import com.gameControl.repository.JogoRepository;
import com.gameControl.repository.AvaliacaoRepository;
import com.gameControl.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AvaliacaoService {
    private final AvaliacaoRepository avaliacaoRepository;
    private final JogoRepository jogoRepository;
    private final UsuarioRepository usuarioRepository;

    public AvaliacaoService(AvaliacaoRepository avaliacaoRepository, JogoRepository jogoRepository, UsuarioRepository usuarioRepository) {
        this.avaliacaoRepository = avaliacaoRepository;
        this.jogoRepository = jogoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public List<Avaliacao> listarReviewsPorJogo(Long jogoId) {
        return avaliacaoRepository.findByJogoId(jogoId);
    }

    public Optional<Avaliacao> buscarReviewPorID(Long id) {
        return avaliacaoRepository.findById(id);
    }

    public Avaliacao criarReview(Long jogoId, Long usuarioId, Avaliacao avaliacao) {
        Jogo jogo = jogoRepository.findById(jogoId)
                .orElseThrow(() -> new RuntimeException("Jogo não encontrado."));

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

        if (avaliacaoRepository.findByJogoIdAndUsuarioId(jogoId, usuarioId).isPresent()) {
            throw new RuntimeException("Usuário já avaliou este jogo.");
        }

        avaliacao.setJogo(jogo);
        avaliacao.setUsuario(usuario);
        avaliacao.setDataAvaliacao(LocalDateTime.now());

        Avaliacao avaliacaoSalva = avaliacaoRepository.save(avaliacao);

        // ** FUTURA LÓGICA: ATUALIZAR A MÉDIA GERAL DO JOGO **

        return avaliacaoSalva;
    }

    public Avaliacao atualizarAvaliacao(Long id, Avaliacao updatedAvaliacao) {
        return avaliacaoRepository.findById(id)
                .map(avaliacao -> {
                    avaliacao.setNota(updatedAvaliacao.getNota());
                    avaliacao.setComentario(updatedAvaliacao.getComentario());
                    avaliacao.setDataAvaliacao(LocalDateTime.now());

                    Avaliacao avaliacaoSalva = avaliacaoRepository.save(avaliacao);

                    // ** FUTURA LÓGICA: RECALCULAR A MÉDIA GERAL DO JOGO **

                    return avaliacaoSalva;
                }).orElseThrow(() -> new RuntimeException("Avaliação não encontrada."));
    }

    public boolean deletarReview(Long id) {
        if (avaliacaoRepository.existsById(id)) {
            avaliacaoRepository.deleteById(id);

            // ** FUTURA LÓGICA: RECALCULAR A MÉDIA GERAL DO JOGO **

            return true;
        }

        return false;
    }
}
