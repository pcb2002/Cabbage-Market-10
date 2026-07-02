package com.example.cabbagemarket10.domain.search.controller;

import com.example.cabbagemarket10.domain.search.dto.request.ItemSearchRequest;
import com.example.cabbagemarket10.domain.search.dto.response.SearchItemResponse;
import com.example.cabbagemarket10.domain.search.service.SearchService;
import com.example.cabbagemarket10.global.common.CommonResponse;
import com.example.cabbagemarket10.global.common.PageResponse;
import com.example.cabbagemarket10.global.security.jwt.AuthenticatedClient;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
            @AuthenticationPrincipal AuthenticatedClient authenticatedClient,
            HttpServletRequest httpServletRequest,
            Pageable pageable
    ) {
        Long clientId = clientIdOf(authenticatedClient);
        searchService.recordKeyword(request.keyword(), clientId, sessionIdOf(httpServletRequest, clientId, request.keyword()));
        Page<SearchItemResponse> items = searchService.searchItemsV1(request, pageable, clientId);
 
        return CommonResponse.success(
                HttpStatus.OK,
                PageResponse.from(items)
        ).toResponseEntity();
    }

    @GetMapping("/v2/items/search")
    public ResponseEntity<CommonResponse<PageResponse<SearchItemResponse>>> searchItemsV2(
            @Valid @ModelAttribute ItemSearchRequest request,
            @AuthenticationPrincipal AuthenticatedClient authenticatedClient,
            HttpServletRequest httpServletRequest,
            Pageable pageable
    ) {
        Long clientId = clientIdOf(authenticatedClient);
        searchService.recordKeyword(request.keyword(), clientId, sessionIdOf(httpServletRequest, clientId, request.keyword()));
        Page<SearchItemResponse> items = searchService.searchItemsV2(request, pageable, clientId);

        return CommonResponse.success(
                HttpStatus.OK,
                PageResponse.from(items)
        ).toResponseEntity();
    }

    private Long clientIdOf(AuthenticatedClient authenticatedClient) {
        return authenticatedClient == null ? null : authenticatedClient.clientId();
    }

    private String sessionIdOf(HttpServletRequest request, Long clientId, String keyword) {
        if (clientId != null || keyword == null || keyword.isBlank()) {
            return null;
        }
        return request.getSession().getId();
    }
}
