package com.example.cabbagemarket10.domain.category.service;

import com.example.cabbagemarket10.domain.category.dto.CategoryResponse;
import com.example.cabbagemarket10.domain.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {
    private final CategoryRepository categoryRepository;

    public Page<CategoryResponse> getCategories(int page, int size) {
        // page는 0부터 시작, sortOrder 기준 정렬 및 페이징
        Pageable pageable = PageRequest.of(page, size, Sort.by("sortOrder").ascending());
        return categoryRepository.findAll(pageable).map(CategoryResponse::from);
    }
}