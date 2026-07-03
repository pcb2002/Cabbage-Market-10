package com.example.cabbagemarket10.domain.item.repository;

import com.example.cabbagemarket10.domain.item.entity.Item;
import com.example.cabbagemarket10.domain.item.enums.TradeStatus;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long>, ItemRepositoryCustom {
    // 1. 특정 카테고리에 속한 상품 목록 조회 (페이징)
    Page<Item> findByCategoryId(Long categoryId, Pageable pageable);

    // 2. 특정 판매자가 등록한 상품 목록 조회 (마이페이지 등에서 활용)
    // Item 엔티티에 sellerId가 있다고 가정
    Page<Item> findBySellerId(Long sellerId, Pageable pageable);

    // 3. 상품명으로 검색 (제목 검색)
    Page<Item> findByTitleContaining(String title, Pageable pageable);

    // 4. 판매 상태별 조회 (판매중, 예약중, 판매완료 필터링)
    Page<Item> findByTradeStatus(TradeStatus tradeStatus, Pageable pageable);

    long countBySellerIdAndTradeStatusAndIsDraftFalse(Long sellerId, TradeStatus tradeStatus);

    // 5. 상세 조회 시, 삭제되지 않은 상품만 명시적으로 가져오기
    // (JPA @Where 덕분에 자동 필터링 되지만, 명시적 처리가 필요할 때 사용)
    Optional<Item> findById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Item i where i.id = :itemId and i.isDraft = false")
    Optional<Item> findByIdForUpdate(@Param("itemId") Long itemId);

    @Modifying
    @Query(value = "delete from item where id = :itemId", nativeQuery = true)
    void hardDeleteById(Long itemId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Item i
            set i.likeCount = i.likeCount + 1
            where i.id = :itemId
            """)
    int incrementLikeCount(@Param("itemId") Long itemId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Item i
            set i.likeCount = case
                when i.likeCount > 0 then i.likeCount - 1
                else 0
            end
            where i.id = :itemId
            """)
    int decrementLikeCount(@Param("itemId") Long itemId);

    @Query("""
            select i.likeCount
            from Item i
            where i.id = :itemId
            """)
    Long findLikeCountById(@Param("itemId") Long itemId);
}
