package com.gameControl.repository;


import com.gameControl.model.Playlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlaylistRepository extends JpaRepository<Playlist, Long> {

    // Buscar todas as playlists de um usuário específico
    List<Playlist> findByUserId(Long userId);
}
