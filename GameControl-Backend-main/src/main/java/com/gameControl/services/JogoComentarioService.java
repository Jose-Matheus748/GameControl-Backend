package com.gameControl.services;

import com.gameControl.model.Jogo;
import com.gameControl.model.JogoComentario;
import com.gameControl.model.Usuario;
import com.gameControl.repository.JogoComentarioRepository;
import com.gameControl.repository.JogoRepository;
import com.gameControl.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class JogoComentarioService {
    private final JogoComentarioRepository jogoComentarioRepository;
    private final UsuarioRepository usuarioRepository;
    private final JogoRepository jogoRepository;

    public JogoComentarioService(JogoComentarioRepository jogoComentarioRepository,
                                 UsuarioRepository usuarioRepository,
                                 JogoRepository jogoRepository) {
        this.jogoComentarioRepository = jogoComentarioRepository;
        this.usuarioRepository = usuarioRepository;
        this.jogoRepository = jogoRepository;
    }

    public JogoComentario criarComentario(Long usuarioId, Long jogoId, String conteudo) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

        Jogo jogo = jogoRepository.findById(jogoId)
                .orElseThrow(() -> new RuntimeException("Jogo não encontrado."));

        JogoComentario jogoComentario = new JogoComentario();
        jogoComentario.setConteudo(conteudo);
        jogoComentario.setUsuario(usuario);
        jogoComentario.setJogo(jogo);

        return jogoComentarioRepository.save(jogoComentario);
    }

    public List<JogoComentario> listarComentariosPorJogo(Long jogoId) {
        return jogoComentarioRepository.findByJogoId(jogoId);
    }

    public List<JogoComentario> listarComentariosPorUsuario(Long usuarioId) {

        return jogoComentarioRepository.findByUsuarioId(usuarioId);
    }

    public boolean deletarComentario(Long comentarioId) {
        if (jogoComentarioRepository.existsById(comentarioId)) {
            jogoComentarioRepository.deleteById(comentarioId);
            return true;
        }
        return false;
    }
}