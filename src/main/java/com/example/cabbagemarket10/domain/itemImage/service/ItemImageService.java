package com.example.cabbagemarket10.domain.itemImage.service;

import com.example.cabbagemarket10.domain.item.entity.Item;
import com.example.cabbagemarket10.domain.itemImage.dto.response.ItemImageUploadResponse;
import com.example.cabbagemarket10.domain.itemImage.entity.ItemImage;
import com.example.cabbagemarket10.domain.itemImage.repository.ItemImageRepository;
import com.example.cabbagemarket10.global.exception.BusinessException;
import com.example.cabbagemarket10.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemImageService {

    private final ItemImageRepository itemImageRepository;

    public ItemImage findByIdAndItemId(Long imageId, Long itemId) {
        return itemImageRepository.findByIdAndItem_Id(imageId, itemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    public int countImagesByItemId(Long itemId) {
        return itemImageRepository.countByItem_Id(itemId);
    }

    @Transactional
    public ItemImageUploadResponse.ImageInfo saveItemImage(Item item, String imageUrl, int sortOrder, boolean isThumbnail) {
        ItemImage itemImage = ItemImage.builder()
                .item(item)
                .imageUrl(imageUrl)
                .sortOrder(sortOrder)
                .isThumbnail(isThumbnail)
                .build();

        ItemImage savedImage = itemImageRepository.save(itemImage);

        return new ItemImageUploadResponse.ImageInfo(
                savedImage.getId(),
                savedImage.getImageUrl(),
                savedImage.getSortOrder(),
                savedImage.getIsThumbnail()
        );
    }

    @Transactional
    public void demoteThumbnailsByItemId(Long itemId) {
        itemImageRepository.updateIsThumbnailFalseByItemId(itemId);
    }

    @Transactional
    public void promoteToThumbnail(Long imageId, Long itemId) {
        int updatedRows = itemImageRepository.updateIsThumbnailTrueByIdAndItemId(imageId, itemId);
        if (updatedRows == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
    }

    /**
     * 이미지 유효성 및 대표 이미지(썸네일) 차단 방어 로직 수행
     */
    public ItemImage getValidItemImage(Long imageId, Long itemId) {
        ItemImage itemImage = itemImageRepository.findById(imageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.IMAGE_NOT_FOUND));

        // 요청 경로의 itemId와 실제 이미지의 상위 itemId 정합성 검증
        if (!itemImage.getItem().getId().equals(itemId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 엔티티 내부 캡슐화 로직 호출 : 대표 이미지(isThumbnail == true) 삭제 시 400 반환
        itemImage.validateNotThumbnail();

        return itemImage;
    }

    /**
     * DB 레코드 물리 삭제
     */
    @Transactional
    public void delete(ItemImage itemImage) {
        itemImageRepository.delete(itemImage);
    }

    @Transactional
    public void deleteByItemId(Long itemId) {
        itemImageRepository.deleteByItemId(itemId);
    }
}
