package com.gameControl.services;

import com.gameControl.model.Jogo;
import com.gameControl.model.UsuarioPlayList;
import com.gameControl.model.Usuario;
import com.gameControl.repository.JogoRepository;
import com.gameControl.repository.UsuarioPlayListRepository;
import com.gameControl.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UsuarioPlayListService {
    private final UsuarioPlayListRepository usuarioPlayListRepository;
    private final UsuarioRepository usuarioRepository;
    private final JogoRepository jogoRepository;

    public UsuarioPlayListService(UsuarioPlayListRepository usuarioPlayListRepository, UsuarioRepository usuarioRepository, JogoRepository jogoRepository) {
        this.usuarioPlayListRepository = usuarioPlayListRepository;
        this.usuarioRepository = usuarioRepository;
        this.jogoRepository = jogoRepository;
    }

    public List<UsuarioPlayList> listarPlaylistsPorUsuario(Long usuarioId) {
        return usuarioPlayListRepository.findByUsuarioId(usuarioId);
    }

    public Optional<UsuarioPlayList> buscarPlaylistPorID(Long id) {
        return usuarioPlayListRepository.findById(id);
    }

    public UsuarioPlayList criarPlaylist(Long usuarioId, UsuarioPlayList usuarioPlayList) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

        usuarioPlayList.setUsuario(usuario);
        return usuarioPlayListRepository.save(usuarioPlayList);
    }

    public UsuarioPlayList atualizarPlaylist(Long id, UsuarioPlayList updatedUsuarioPlayList) {
        return usuarioPlayListRepository.findById(id)
                .map(usuarioPlayList -> {
                    usuarioPlayList.setNome(updatedUsuarioPlayList.getNome());
                    usuarioPlayList.setDescricao(updatedUsuarioPlayList.getDescricao());

                    return usuarioPlayListRepository.save(usuarioPlayList);
                }).orElseThrow(() -> new RuntimeException("Playlist não encontrada."));
    }

    public boolean deletarPlaylist(Long id) {
        if (usuarioPlayListRepository.existsById(id)) {
            usuarioPlayListRepository.deleteById(id);
            return true;
        }

        return false;
    }

    // --- Métodos de Manipulação de Jogos na Playlist ---

    public UsuarioPlayList adicionarJogo(Long playlistId, Long gameId) {
        UsuarioPlayList usuarioPlayList = usuarioPlayListRepository.findById(playlistId)
                .orElseThrow(() -> new RuntimeException("Playlist não encontrada."));

        Jogo jogo = jogoRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Jogo não encontrado."));

        if (usuarioPlayList.getJogos().contains(jogo)) {
            throw new RuntimeException("Jogo já está na playlist.");
        }

        usuarioPlayList.getJogos().add(jogo);
        return usuarioPlayListRepository.save(usuarioPlayList);
    }

    public UsuarioPlayList removerJogo(Long playlistId, Long gameId) {
        UsuarioPlayList usuarioPlayList = usuarioPlayListRepository.findById(playlistId)
                .orElseThrow(() -> new RuntimeException("Playlist não encontrada."));

        Jogo jogo = jogoRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Jogo não encontrado."));

        // Remove o jogo da lista de jogos da playlist
        boolean removed = usuarioPlayList.getJogos().removeIf(g -> g.getId().equals(gameId));

        if (!removed) {
            throw new RuntimeException("Jogo não está na playlist.");
        }

        return usuarioPlayListRepository.save(usuarioPlayList);
    }
}
