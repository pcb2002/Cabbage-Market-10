package com.example.cabbagemarket10.domain.category.entity;

import com.example.cabbagemarket10.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "category")
public class Category extends BaseEntity { // createdAt, updatedAt 등 공통 필드 상속

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private Integer sortOrder;

    @Column(nullable = false)
    private Boolean isActive;

    @Builder
    public Category(String name, Integer sortOrder, Boolean isActive) {
        this.name = name;
        this.sortOrder = sortOrder != null ? sortOrder : 0;
        this.isActive = isActive != null ? isActive : true;
    }

    // 비즈니스 로직: 카테고리 정보 수정
    public void updateCategory(String name, Integer sortOrder, Boolean isActive) {
        if (name != null) this.name = name;
        if (sortOrder != null) this.sortOrder = sortOrder;
        if (isActive != null) this.isActive = isActive;
    }
}
