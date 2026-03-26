package com.gameControl.repository;

import com.gameControl.model.GameComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GameCommentRepository extends JpaRepository<GameComment, Long> {
    List<GameComment> findByGameId(Long gameId);
    List<GameComment> findByUserId(Long userId);
}
