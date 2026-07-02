package com.example.cabbagemarket10.domain.search.service;

import com.example.cabbagemarket10.domain.item.enums.ConditionType;
import com.example.cabbagemarket10.domain.item.enums.TradeStatus;
import com.example.cabbagemarket10.domain.item.enums.TradeType;
import com.example.cabbagemarket10.domain.search.dto.request.ItemSearchRequest;
import java.util.stream.Collectors;
import org.springframework.data.domain.Pageable;

public record ItemSearchCacheKey(
        Long clientId,
        String keyword,
        Long categoryId,
        TradeStatus tradeStatus,
        TradeType tradeType,
        ConditionType conditionType,
        boolean likedOnly,
        Long minPrice,
        Long maxPrice,
        int page,
        int size,
        String sort
) {

    public static ItemSearchCacheKey from(ItemSearchRequest request, Pageable pageable, Long clientId) {
        return new ItemSearchCacheKey(
                clientId,
                normalizeKeyword(request.keyword()),
                request.categoryId(),
                request.tradeStatus(),
                request.tradeType(),
                request.conditionType(),
                Boolean.TRUE.equals(request.likedOnly()),
                request.minPrice(),
                request.maxPrice(),
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSort().stream()
                        .map(order -> order.getProperty() + ":" + order.getDirection().name())
                        .collect(Collectors.joining(",", "[", "]"))
        );
    }

    private static String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }

        String trimmedKeyword = keyword.trim();
        return trimmedKeyword.isEmpty() ? null : trimmedKeyword;
    }
}
