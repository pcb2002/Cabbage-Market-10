package com.example.cabbagemarket10.domain.search.service;

import com.example.cabbagemarket10.domain.search.dto.request.ItemSearchRequest;
import com.example.cabbagemarket10.domain.search.dto.response.SearchItemResponse;
import com.example.cabbagemarket10.domain.search.repository.ItemSearchRepository;
import com.example.cabbagemarket10.global.exception.BusinessException;
import com.example.cabbagemarket10.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
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
            Pageable pageable
    ) {
        // tradeStatus/tradeType/conditionType 의 잘못된 값은 @ModelAttribute 바인딩 단계에서
        // 걸러져 BindException 으로 GlobalExceptionHandler 가 처리한다.
        validatePriceRange(request.minPrice(), request.maxPrice());

        return itemSearchRepository.searchItemsV1(
                request.keyword(),
                request.categoryId(),
                request.tradeStatus(),
                request.tradeType(),
                request.conditionType(),
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
}
