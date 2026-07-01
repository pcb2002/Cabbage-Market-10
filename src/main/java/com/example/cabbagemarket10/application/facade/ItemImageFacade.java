package com.example.cabbagemarket10.application.facade;

import com.example.cabbagemarket10.domain.item.entity.Item;
import com.example.cabbagemarket10.domain.item.service.ItemService;
import com.example.cabbagemarket10.domain.itemImage.dto.response.ItemImageUploadResponse;
import com.example.cabbagemarket10.domain.itemImage.service.ItemImageService;
import com.example.cabbagemarket10.global.exception.BusinessException;
import com.example.cabbagemarket10.global.exception.ErrorCode;
import com.example.cabbagemarket10.global.util.StorageUploadUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ItemImageFacade {

    private static final String ITEM_IMAGE_DIR = "items";

    private final ItemService itemService;
    private final ItemImageService itemImageService;
    private final StorageUploadUtils storageUploadUtils;

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "webp");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 장당 최대 5MB

    @Transactional
    public ItemImageUploadResponse uploadItemImages(Long itemId, List<MultipartFile> files, Long clientId) {
        // 1. HTTP 1차 벨리데이션 및 파일 유효성 검증
        if (files == null || files.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        for (MultipartFile file : files) {
            validateFile(file);
        }

        // 2. Item 도메인 서비스를 통한 상품 조회 및 권한(Seller 일치여부) 검증
        Item item = itemService.getValidatedItem(itemId, clientId);

        // 3. ItemImage 도메인 서비스를 통해 기존 등록된 이미지 개수 조회 및 노출 순서 기준점 마련
        int currentImageCount = itemImageService.countImagesByItemId(itemId);
        int nextSortOrder = currentImageCount + 1;

        List<ItemImageUploadResponse.ImageInfo> uploadedImageInfos = new ArrayList<>();
        List<String> uploadedUrls = new ArrayList<>();

        try {
            // 4. 스토리지 파일 업로드와 도메인 저장을 순차적으로 수행
            for (int i = 0; i < files.size(); i++) {
                MultipartFile file = files.get(i);

                // 외부 인프라 연동 (클라우드 스토리지 파일 업로드)
                String imageUrl = storageUploadUtils.upload(file, ITEM_IMAGE_DIR);
                uploadedUrls.add(imageUrl);

                // 핵심 분기 비즈니스 규칙 적용: 기존 등록이 0개이고 현재 배열의 index 0일 때만 썸네일로 지정
                boolean isThumbnail = (currentImageCount == 0 && i == 0);
                int sortOrder = nextSortOrder + i;

                // ItemImage 도메인 서비스를 호출하여 엔티티 저장
                ItemImageUploadResponse.ImageInfo info = itemImageService.saveItemImage(item, imageUrl, sortOrder, isThumbnail);
                uploadedImageInfos.add(info);
            }
        } catch (Exception e) {
            // 스토리지 업로드 혹은 DB 처리 중 하나라도 실패할 시 스토리지에 먼저 올라간 파일들 삭제 조치
            for (String url : uploadedUrls) {
                try { storageUploadUtils.delete(url, ITEM_IMAGE_DIR); } catch (Exception ignored) {}
            }
            // @Transactional에 의해 DB 작업도 통롤백되며 비즈니스 예외 전파
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        return ItemImageUploadResponse.of(itemId, uploadedImageInfos);
    }

    private void validateFile(MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }
}
