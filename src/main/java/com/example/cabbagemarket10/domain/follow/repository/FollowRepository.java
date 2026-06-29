package com.example.cabbagemarket10.domain.follow.repository;

import com.example.cabbagemarket10.domain.follow.entity.Follow;
import com.example.cabbagemarket10.domain.follow.entity.FollowId;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FollowRepository extends JpaRepository<Follow, FollowId> {

    // followerId가 followingId를 이미 팔로우 중인지 여부
    boolean existsByFollower_IdAndFollowing_Id(Long followerId, Long followingId);

    // 특정 팔로우 관계 단건 조회 (언팔로우 시 삭제 대상 조회 등)
    Optional<Follow> findByFollower_IdAndFollowing_Id(Long followerId, Long followingId);

    // followingId를 팔로우하는 회원 수 (팔로워 수)
    long countByFollowing_Id(Long followingId);

    // followerId가 팔로우하는 회원 수 (팔로잉 수)
    long countByFollower_Id(Long followerId);
}
