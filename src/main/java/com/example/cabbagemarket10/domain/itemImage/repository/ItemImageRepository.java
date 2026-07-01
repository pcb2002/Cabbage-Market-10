package com.example.cabbagemarket10.domain.itemImage.repository;

import com.example.cabbagemarket10.domain.itemImage.entity.ItemImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItemImageRepository extends JpaRepository<ItemImage, Long> {

    // 특정 상품의 이미지 목록을 정렬 순서대로 조회
    List<ItemImage> findByItemIdOrderBySortOrderAsc(Long itemId);

    // 특정 상품의 썸네일 이미지 조회
    ItemImage findByItemIdAndIsThumbnailTrue(Long itemId);

    // 특정 상품의 모든 이미지 삭제 (Cascade로 해결 가능하지만, 명시적 삭제 필요 시 사용)
    void deleteByItemId(Long itemId);

    int countByItem_Id(Long itemId);

    /**
     * 잘못된 이미지 ID 검증 및 해당 상품에 속한 올바른 이미지인지 쌍으로 검증합니다.
     */
    Optional<ItemImage> findByIdAndItem_Id(Long imageId, Long itemId);

    /**
     * 특정 상품에 속한 모든 이미지의 대표(썸네일) 설정을 일괄적으로 해제(false)합니다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ItemImage ii SET ii.isThumbnail = false WHERE ii.item.id = :itemId")
    void updateIsThumbnailFalseByItemId(@Param("itemId") Long itemId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ItemImage ii SET ii.isThumbnail = true WHERE ii.id = :imageId AND ii.item.id = :itemId")
    int updateIsThumbnailTrueByIdAndItemId(@Param("imageId") Long imageId, @Param("itemId") Long itemId);
}
