package com.gameControl.repository;


import com.gameControl.model.UsuarioPlayList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UsuarioPlayListRepository extends JpaRepository<UsuarioPlayList, Long> {

    // Buscar todas as playlists de um usuário específico
    List<UsuarioPlayList> findByUsuarioId(Long usuarioId);
}
