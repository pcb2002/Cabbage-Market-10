package com.example.cabbagemarket10.domain.category.repository;

import com.example.cabbagemarket10.domain.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // 정렬 순서대로 전체 조회
    List<Category> findAllByOrderBySortOrderAsc();
}
