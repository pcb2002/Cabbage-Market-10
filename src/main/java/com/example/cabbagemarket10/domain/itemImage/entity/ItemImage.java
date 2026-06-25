package com.example.cabbagemarket10.domain.itemImage.entity;

import com.example.cabbagemarket10.common.entity.BaseEntity;
import com.example.cabbagemarket10.domain.item.entity.Item;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "item_image")
public class ItemImage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(nullable = false)
    private String imageUrl;

    @Column(nullable = false)
    private Integer sortOrder;

    @Column(nullable = false)
    private Boolean isThumbnail;

    @Builder
    public ItemImage(Item item, String imageUrl, Integer sortOrder, Boolean isThumbnail) {
        this.item = item;
        this.imageUrl = imageUrl;
        this.sortOrder = (sortOrder != null) ? sortOrder : 0;
        this.isThumbnail = (isThumbnail != null) ? isThumbnail : false;
    }

    // 썸네일 여부 변경 메서드
    public void updateThumbnail(boolean isThumbnail) {
        this.isThumbnail = isThumbnail;
    }
}