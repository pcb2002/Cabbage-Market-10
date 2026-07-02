package com.example.cabbagemarket10.domain.search.repository;

import com.example.cabbagemarket10.domain.item.enums.ConditionType;
import com.example.cabbagemarket10.domain.item.enums.TradeStatus;
import com.example.cabbagemarket10.domain.item.enums.TradeType;
import com.example.cabbagemarket10.domain.search.dto.response.SearchItemResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ItemSearchRepository {

    Page<SearchItemResponse> searchItemsV1(
            Long clientId,
            String keyword,
            Long categoryId,
            TradeStatus tradeStatus,
            TradeType tradeType,
            ConditionType conditionType,
            boolean likedOnly,
            Long minPrice,
            Long maxPrice,
            Pageable pageable
    );
}
