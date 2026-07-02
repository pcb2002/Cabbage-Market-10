package com.example.cabbagemarket10.domain.search.service;

import com.example.cabbagemarket10.domain.search.dto.request.ItemSearchRequest;
import com.example.cabbagemarket10.domain.search.dto.response.SearchItemResponse;
import com.example.cabbagemarket10.domain.search.repository.ItemSearchRepository;
import com.example.cabbagemarket10.global.config.cache.ItemSearchCacheConfig;
import com.example.cabbagemarket10.global.exception.BusinessException;
import com.example.cabbagemarket10.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final ItemSearchRepository itemSearchRepository;

    @Transactional(readOnly = true)
    public Page<SearchItemResponse> searchItemsV1(
            ItemSearchRequest request,
            Pageable pageable,
            Long clientId
    ) {
        return searchItems(request, pageable, clientId);
    }

    @Cacheable(
            cacheNames = ItemSearchCacheConfig.ITEM_SEARCH_V2_CACHE,
            key = "T(com.example.cabbagemarket10.domain.search.service.ItemSearchCacheKey).from(#request, #pageable, #clientId)",
            condition = "#clientId == null"
    )
    @Transactional(readOnly = true)
    public Page<SearchItemResponse> searchItemsV2(
            ItemSearchRequest request,
            Pageable pageable,
            Long clientId
    ) {
        return searchItems(request, pageable, clientId);
    }

    private Page<SearchItemResponse> searchItems(
            ItemSearchRequest request,
            Pageable pageable,
            Long clientId
    ) {
        // tradeStatus/tradeType/conditionType 의 잘못된 값은 @ModelAttribute 바인딩 단계에서
        // 걸러져 BindException 으로 GlobalExceptionHandler 가 처리한다.
        validatePriceRange(request.minPrice(), request.maxPrice());
        validateLikedOnly(request.likedOnly(), clientId);

        return itemSearchRepository.searchItemsV1(
                clientId,
                request.keyword(),
                request.categoryId(),
                request.tradeStatus(),
                request.tradeType(),
                request.conditionType(),
                Boolean.TRUE.equals(request.likedOnly()),
                request.minPrice(),
                request.maxPrice(),
                pageable
        );
    }

    private void validatePriceRange(Long minPrice, Long maxPrice) {
        if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "최소 가격은 최대 가격보다 클 수 없습니다.");
        }
    }

    private void validateLikedOnly(Boolean likedOnly, Long clientId) {
        if (Boolean.TRUE.equals(likedOnly) && clientId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "좋아요한 상품만 검색하려면 로그인이 필요합니다.");
        }
    }
}
