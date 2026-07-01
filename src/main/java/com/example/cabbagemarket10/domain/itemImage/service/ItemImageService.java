package com.example.cabbagemarket10.domain.itemImage.service;

import com.example.cabbagemarket10.domain.item.entity.Item;
import com.example.cabbagemarket10.domain.itemImage.dto.response.ItemImageUploadResponse;
import com.example.cabbagemarket10.domain.itemImage.entity.ItemImage;
import com.example.cabbagemarket10.domain.itemImage.repository.ItemImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemImageService {

    private final ItemImageRepository itemImageRepository;

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
}