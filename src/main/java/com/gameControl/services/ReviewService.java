package com.gameControl.services;

import com.gameControl.model.Game;
import com.gameControl.model.Review;
import com.gameControl.model.User;
import com.gameControl.repository.GameRepository;
import com.gameControl.repository.ReviewRepository;
import com.gameControl.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final GameRepository gameRepository;
    private final UserRepository userRepository;

    public ReviewService(ReviewRepository reviewRepository, GameRepository gameRepository, UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.gameRepository = gameRepository;
        this.userRepository = userRepository;
    }

    public List<Review> listarReviewsPorJogo(Long gameId) {
        return reviewRepository.findByGameId(gameId);
    }

    public Optional<Review> buscarReviewPorID(Long id) {
        return reviewRepository.findById(id);
    }

    public Review criarReview(Long gameId, Long userId, Review review) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Jogo não encontrado."));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

        if (reviewRepository.findByGameIdAndUserId(gameId, userId).isPresent()) {
            throw new RuntimeException("Usuário já avaliou este jogo.");
        }

        review.setGame(game);
        review.setUser(user);
        review.setReviewDate(LocalDateTime.now());

        Review reviewSalva = reviewRepository.save(review);

        // ** FUTURA LÓGICA: ATUALIZAR A MÉDIA GERAL DO JOGO **

        return reviewSalva;
    }

    public Review atualizarReview(Long id, Review updatedReview) {
        return reviewRepository.findById(id)
                .map( review -> {
                    review.setRating(updatedReview.getRating());
                    review.setComment(updatedReview.getComment());
                    review.setReviewDate(LocalDateTime.now());

                    Review reviewSalva = reviewRepository.save(review);

                    // ** FUTURA LÓGICA: RECALCULAR A MÉDIA GERAL DO JOGO **

                    return reviewSalva;
                }).orElseThrow(() -> new RuntimeException("Avaliação não encontrada."));
    }

    public boolean deletarReview(Long id) {
        if (reviewRepository.existsById(id)) {
            reviewRepository.deleteById(id);

            // ** FUTURA LÓGICA: RECALCULAR A MÉDIA GERAL DO JOGO **

            return true;
        }

        return false;
    }
}
