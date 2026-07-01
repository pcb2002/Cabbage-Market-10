package com.example.cabbagemarket10.domain.itemLike.repository;

import com.example.cabbagemarket10.domain.itemLike.entity.ItemLike;
import com.example.cabbagemarket10.domain.itemLike.entity.ItemLikeId;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemLikeRepository extends JpaRepository<ItemLike, ItemLikeId> {

    Optional<ItemLike> findByClient_IdAndItem_Id(Long clientId, Long itemId);
}
