package com.example.cabbagemarket10.domain.search.controller;

import com.example.cabbagemarket10.domain.search.dto.request.ItemSearchRequest;
import com.example.cabbagemarket10.domain.search.dto.response.SearchItemResponse;
import com.example.cabbagemarket10.domain.search.service.SearchService;
import com.example.cabbagemarket10.global.common.CommonResponse;
import com.example.cabbagemarket10.global.common.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/v1/items/search")
    public ResponseEntity<CommonResponse<PageResponse<SearchItemResponse>>> searchItemsV1(
            @Valid @ModelAttribute ItemSearchRequest request,
            Pageable pageable
    ) {
        Page<SearchItemResponse> items = searchService.searchItemsV1(request, pageable);
 
        return CommonResponse.success(
                HttpStatus.OK,
                PageResponse.from(items)
        ).toResponseEntity();
    }

    @GetMapping("/v2/items/search")
    public ResponseEntity<CommonResponse<PageResponse<SearchItemResponse>>> searchItemsV2(
            @Valid @ModelAttribute ItemSearchRequest request,
            Pageable pageable
    ) {
        Page<SearchItemResponse> items = searchService.searchItemsV2(request, pageable);

        return CommonResponse.success(
                HttpStatus.OK,
                PageResponse.from(items)
        ).toResponseEntity();
    }
}
