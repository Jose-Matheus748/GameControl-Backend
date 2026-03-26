package com.gameControl.services;

import com.gameControl.model.Game;
import com.gameControl.model.GameComment;
import com.gameControl.model.User;
import com.gameControl.repository.GameCommentRepository;
import com.gameControl.repository.GameRepository;
import com.gameControl.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GameCommentService {
    private final GameCommentRepository gameCommentRepository;
    private final UserRepository userRepository;
    private final GameRepository gameRepository;

    public GameCommentService(GameCommentRepository gameCommentRepository,
                              UserRepository userRepository,
                              GameRepository gameRepository) {
        this.gameCommentRepository = gameCommentRepository;
        this.userRepository = userRepository;
        this.gameRepository = gameRepository;
    }

    public GameComment createComment(Long userId, Long gameId, String content) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Jogo não encontrado."));

        GameComment gameComment = new GameComment();
        gameComment.setContent(content);
        gameComment.setUser(user);
        gameComment.setGame(game);

        return gameCommentRepository.save(gameComment);
    }

    public List<GameComment> getCommentsByGame(Long gameId) {
        return gameCommentRepository.findByGameId(gameId);
    }

    public List<GameComment> getCommentsByUser(Long userId) {
        return gameCommentRepository.findByUserId(userId);
    }

    public boolean deleteComment(Long commentId) {
        if (gameCommentRepository.existsById(commentId)) {
            gameCommentRepository.deleteById(commentId);
            return true;
        }
        return false;
    }
}