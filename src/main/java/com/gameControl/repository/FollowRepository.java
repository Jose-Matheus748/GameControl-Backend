package com.gameControl.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.gameControl.model.Follow;
import com.gameControl.model.User;

@Repository
public interface FollowRepository extends JpaRepository<Follow, Long> {
    Optional<Follow> findByFollowerAndFollowed(User follower, User followed);
    List<Follow> findByFollower(User follower);
    List<Follow> findByFollowed(User followed);
}
