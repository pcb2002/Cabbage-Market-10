package com.example.cabbagemarket10.domain.follow.entity;

import java.io.Serializable;
import java.util.Objects;

public class FollowId implements Serializable {

    private Long follower;
    private Long following;

    protected FollowId() {
    }

    public FollowId(Long follower, Long following) {
        this.follower = follower;
        this.following = following;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FollowId followId)) {
            return false;
        }
        return Objects.equals(follower, followId.follower)
                && Objects.equals(following, followId.following);
    }

    @Override
    public int hashCode() {
        return Objects.hash(follower, following);
    }
}
