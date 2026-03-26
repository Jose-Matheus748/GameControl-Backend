package com.gameControl.services;

import com.gameControl.model.Game;
import com.gameControl.model.Playlist;
import com.gameControl.model.User;
import com.gameControl.repository.GameRepository;
import com.gameControl.repository.PlaylistRepository;
import com.gameControl.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PlaylistService {
    private final PlaylistRepository playlistRepository;
    private final UserRepository userRepository;
    private final GameRepository gameRepository;

    public PlaylistService(PlaylistRepository playlistRepository, UserRepository userRepository, GameRepository gameRepository) {
        this.playlistRepository = playlistRepository;
        this.userRepository = userRepository;
        this.gameRepository = gameRepository;
    }

    public List<Playlist> listarPlaylistsPorUsuario(Long userId) {
        return playlistRepository.findByUserId(userId);
    }

    public Optional<Playlist> buscarPlaylistPorID(Long id) {
        return playlistRepository.findById(id);
    }

    public Playlist criarPlaylist(Long userId, Playlist playlist) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

        playlist.setUser(user);
        return playlistRepository.save(playlist);
    }

    public Playlist atualizarPlaylist(Long id, Playlist updatedPlaylist) {
        return playlistRepository.findById(id)
                .map( playlist -> {
                    playlist.setName(updatedPlaylist.getName());
                    playlist.setDescription(updatedPlaylist.getDescription());

                    return playlistRepository.save(playlist);
                }).orElseThrow(() -> new RuntimeException("Playlist não encontrada."));
    }

    public boolean deletarPlaylist(Long id) {
        if (playlistRepository.existsById(id)) {
            playlistRepository.deleteById(id);
            return true;
        }

        return false;
    }

    // --- Métodos de Manipulação de Jogos na Playlist ---

    public Playlist adicionarJogo(Long playlistId, Long gameId) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new RuntimeException("Playlist não encontrada."));

        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Jogo não encontrado."));

        if (playlist.getGames().contains(game)) {
            throw new RuntimeException("Jogo já está na playlist.");
        }

        playlist.getGames().add(game);
        return playlistRepository.save(playlist);
    }

    public Playlist removerJogo(Long playlistId, Long gameId) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new RuntimeException("Playlist não encontrada."));

        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Jogo não encontrado."));

        // Remove o jogo da lista de jogos da playlist
        boolean removed = playlist.getGames().removeIf(g -> g.getId().equals(gameId));

        if (!removed) {
            throw new RuntimeException("Jogo não está na playlist.");
        }

        return playlistRepository.save(playlist);
    }
}
