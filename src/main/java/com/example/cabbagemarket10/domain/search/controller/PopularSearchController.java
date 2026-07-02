package com.example.cabbagemarket10.domain.search.controller;

import com.example.cabbagemarket10.domain.search.dto.response.PopularKeywordsResponse;
import com.example.cabbagemarket10.domain.search.service.PopularSearchService;
import com.example.cabbagemarket10.global.common.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class PopularSearchController {

    private final PopularSearchService popularSearchService;

    @GetMapping("/popular")
    public ResponseEntity<CommonResponse<PopularKeywordsResponse>> getPopularKeywords() {
        return CommonResponse.success(HttpStatus.OK, popularSearchService.getPopularKeywords())
                .toResponseEntity();
    }
}
