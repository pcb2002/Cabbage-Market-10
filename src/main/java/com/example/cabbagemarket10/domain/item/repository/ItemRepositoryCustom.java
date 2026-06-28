package com.example.cabbagemarket10.domain.item.repository;

import com.example.cabbagemarket10.domain.item.dto.response.ItemListItemResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ItemRepositoryCustom {
    Page<ItemListItemResponse> searchItems(Long categoryId, String tradeStatus, Pageable pageable);
}