package com.gameControl.repository;

import java.util.List;
import java.util.Optional;

import com.gameControl.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.gameControl.model.Seguir;

@Repository
public interface SeguirRepository extends JpaRepository<Seguir, Long> {
    Optional<Seguir> findBySeguidorAndSeguido(Usuario seguidor, Usuario seguido);
    List<Seguir> findBySeguidor(Usuario seguidor);
    List<Seguir> findBySeguido(Usuario seguido);
}
