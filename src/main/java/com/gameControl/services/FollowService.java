package com.gameControl.services;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.gameControl.dto.UserFollowDTO;
import com.gameControl.model.Follow;
import com.gameControl.model.User;
import com.gameControl.repository.FollowRepository;
import com.gameControl.repository.UserRepository;

@Service
public class FollowService {
    private final FollowRepository followRepository;
    private final UserRepository userRepository;

    public FollowService(FollowRepository followRepository, UserRepository userRepository) {
        this.followRepository = followRepository;
        this.userRepository = userRepository;
    }

    public void followUser(Long followerId, Long followedId) {
        if (followerId.equals(followedId)) {
            throw new IllegalArgumentException("Um usuário não pode seguir a si mesmo.");
        }

        User follower = userRepository.findById(followerId)
            .orElseThrow(() -> new RuntimeException("Usuário seguidor não encontrado."));

        User followed = userRepository.findById(followedId)
            .orElseThrow(() -> new RuntimeException("Usuário a ser seguido não encontrado."));

        followRepository.findByFollowerAndFollowed(follower, followed)
            .ifPresent(f -> { throw new RuntimeException("Usuário já está seguindo este perfil."); });

        Follow follow = Follow.builder()
            .follower(follower)
            .followed(followed)
            .build();

        followRepository.save(follow);
    }

    public void unfollowUser(Long followerId, Long followedId) {
        User follower = userRepository.findById(followerId)
            .orElseThrow(() -> new RuntimeException("Usuário seguidor não encontrado."));
        User followed = userRepository.findById(followedId)
            .orElseThrow(() -> new RuntimeException("Usuário a ser seguido não encontrado."));

        Follow follow = followRepository.findByFollowerAndFollowed(follower, followed)
            .orElseThrow(() -> new RuntimeException("Usuário não está seguindo este perfil."));

        followRepository.delete(follow);
    }

    // public List<Follow> getFollowers(Long userId) {
    //     User user = userRepository.findById(userId)
    //         .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));
    //     return followRepository.findByFollowed(user);
    // }

    // public List<Follow> getFollowing(Long userId) {
    //     User user = userRepository.findById(userId)
    //         .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));
    //     return followRepository.findByFollower(user);
    // }

    public List<UserFollowDTO> getFollowers(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        return followRepository.findByFollowed(user)
                .stream()
                .map(f -> new UserFollowDTO(
                        f.getFollower().getId(),
                        f.getFollower().getUsername(),
                        f.getFollower().getProfilePictureUrl()
                ))
                .collect(Collectors.toList());
    }

    public List<UserFollowDTO> getFollowing(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        return followRepository.findByFollower(user)
                .stream()
                .map(f -> new UserFollowDTO(
                        f.getFollowed().getId(),
                        f.getFollowed().getUsername(),
                        f.getFollowed().getProfilePictureUrl()
                ))
                .collect(Collectors.toList());
    }
}
