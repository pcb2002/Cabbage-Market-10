package com.example.cabbagemarket10.domain.category.repository;

import com.example.cabbagemarket10.domain.category.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // sortOrder 오름차순 정렬을 위한 페이징 조회
    Page<Category> findAllByOrderBySortOrderAsc(Pageable pageable);
}
