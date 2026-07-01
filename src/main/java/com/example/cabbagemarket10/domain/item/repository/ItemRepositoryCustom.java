package com.example.cabbagemarket10.domain.item.repository;

import com.example.cabbagemarket10.domain.item.dto.response.ItemDetailResponse;
import com.example.cabbagemarket10.domain.item.dto.response.ItemListItemResponse;
import com.example.cabbagemarket10.domain.item.dto.response.MyItemListItemResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface ItemRepositoryCustom {
    Page<ItemListItemResponse> searchItems(Long categoryId, String tradeStatus, Pageable pageable);

    Page<MyItemListItemResponse> findMyItems(Long sellerId, String tradeStatus, Pageable pageable);

    Optional<ItemDetailResponse> findItemDetail(Long itemId);

    int incrementViewCount(Long itemId);
}
