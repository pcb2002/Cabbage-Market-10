package com.example.cabbagemarket10.domain.itemImage.repository;

import com.example.cabbagemarket10.domain.itemImage.entity.ItemImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemImageRepository extends JpaRepository<ItemImage, Long> {

    // 특정 상품의 이미지 목록을 정렬 순서대로 조회
    List<ItemImage> findByItemIdOrderBySortOrderAsc(Long itemId);

    // 특정 상품의 썸네일 이미지 조회
    ItemImage findByItemIdAndIsThumbnailTrue(Long itemId);

    // 특정 상품의 모든 이미지 삭제 (Cascade로 해결 가능하지만, 명시적 삭제 필요 시 사용)
    void deleteByItemId(Long itemId);

    int countByItem_Id(Long itemId);
}