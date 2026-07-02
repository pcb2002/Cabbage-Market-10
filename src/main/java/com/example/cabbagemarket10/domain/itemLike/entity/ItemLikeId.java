package com.example.cabbagemarket10.domain.itemLike.entity;

import java.io.Serializable;
import java.util.Objects;

public class ItemLikeId implements Serializable {

    private Long client;
    private Long item;

    protected ItemLikeId() {
    }

    public ItemLikeId(Long client, Long item) {
        this.client = client;
        this.item = item;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ItemLikeId itemLikeId)) {
            return false;
        }
        return Objects.equals(client, itemLikeId.client)
                && Objects.equals(item, itemLikeId.item);
    }

    @Override
    public int hashCode() {
        return Objects.hash(client, item);
    }
}
