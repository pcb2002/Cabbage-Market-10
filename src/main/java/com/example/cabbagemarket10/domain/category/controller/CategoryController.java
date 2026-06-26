package com.example.cabbagemarket10.domain.category.controller;

import com.example.cabbagemarket10.domain.category.dto.CategoryResponse;
import com.example.cabbagemarket10.domain.category.service.CategoryService;
import com.example.cabbagemarket10.global.common.CommonResponse;
import com.example.cabbagemarket10.global.common.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<CommonResponse<PageResponse<CategoryResponse>>> getCategories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<CategoryResponse> categories = categoryService.getCategories(page, size);
        return CommonResponse.success(HttpStatus.OK, PageResponse.from(categories)).toResponseEntity();
    }
}
