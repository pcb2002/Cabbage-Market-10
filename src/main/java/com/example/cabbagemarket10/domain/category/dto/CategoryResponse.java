package com.example.cabbagemarket10.domain.category.dto;

import com.example.cabbagemarket10.domain.category.entity.Category;

/**
 * 카테고리 목록 조회 응답을 위한 DTO 클래스입니다.
 * <p>
 * 계층형 구조의 카테고리 정보를 클라이언트에 전달하기 위해 사용됩니다.
 * </p>
 *
 * @param id         카테고리의 고유 식별자(PK)
 * @param name       카테고리의 이름
 * @param sortOrder  카테고리 정렬 순서. 오름차순으로 정렬됩니다.
 * @param isActive   카테고리 활성화 상태. {@code true}이면 활성화, {@code false}이면 비활성화 상태입니다.
 */
public record CategoryResponse(
        Long id,
        String name,
        Integer sortOrder,
        Boolean isActive
) {
    /**
     * Category 엔티티를 CategoryResponse DTO로 변환하는 팩토리 메서드입니다.
     * * @param category 변환할 대상 카테고리 엔티티
     * @return Category 엔티티의 정보가 담긴 CategoryResponse 객체
     */
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getSortOrder(),
                category.getIsActive()
        );
    }
}