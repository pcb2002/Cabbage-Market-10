package com.example.cabbagemarket10.application.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.cabbagemarket10.domain.client.entity.Client;
import com.example.cabbagemarket10.domain.item.entity.Item;
import com.example.cabbagemarket10.domain.item.enums.ConditionType;
import com.example.cabbagemarket10.domain.item.enums.TradeStatus;
import com.example.cabbagemarket10.domain.item.enums.TradeType;
import com.example.cabbagemarket10.domain.item.service.ItemService;
import com.example.cabbagemarket10.domain.itemImage.dto.response.ItemImageUploadResponse;
import com.example.cabbagemarket10.domain.itemImage.service.ItemImageService;
import com.example.cabbagemarket10.global.exception.BusinessException;
import com.example.cabbagemarket10.global.exception.ErrorCode;
import com.example.cabbagemarket10.global.util.StorageUploadUtils;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class ItemImageFacadeTest {

    @Mock
    private ItemService itemService;

    @Mock
    private ItemImageService itemImageService;

    @Mock
    private StorageUploadUtils storageUploadUtils;

    @InjectMocks
    private ItemImageFacade itemImageFacade;

    @DisplayName("item image upload stores files under items directory and saves image metadata")
    @Test
    void uploadItemImagesStoresFilesAndSavesImageMetadata() {
        Long itemId = 1L;
        Long clientId = 10L;
        Item item = itemWithSellerId(clientId);
        MockMultipartFile firstFile = imageFile("first.jpg");
        MockMultipartFile secondFile = imageFile("second.png");
        String firstUrl = "https://test-bucket.s3.ap-northeast-2.amazonaws.com/items/first.jpg";
        String secondUrl = "https://test-bucket.s3.ap-northeast-2.amazonaws.com/items/second.png";

        given(itemService.getValidatedItem(itemId, clientId)).willReturn(item);
        given(itemImageService.countImagesByItemId(itemId)).willReturn(0);
        given(storageUploadUtils.upload(firstFile, "items")).willReturn(firstUrl);
        given(storageUploadUtils.upload(secondFile, "items")).willReturn(secondUrl);
        given(itemImageService.saveItemImage(item, firstUrl, 1, true))
                .willReturn(new ItemImageUploadResponse.ImageInfo(100L, firstUrl, 1, true));
        given(itemImageService.saveItemImage(item, secondUrl, 2, false))
                .willReturn(new ItemImageUploadResponse.ImageInfo(101L, secondUrl, 2, false));

        ItemImageUploadResponse response = itemImageFacade.uploadItemImages(
                itemId,
                List.of(firstFile, secondFile),
                clientId);

        assertThat(response.itemId()).isEqualTo(itemId);
        assertThat(response.uploadedImages()).hasSize(2);
        assertThat(response.uploadedImages().get(0).imageUrl()).isEqualTo(firstUrl);
        assertThat(response.uploadedImages().get(0).isThumbnail()).isTrue();
        assertThat(response.uploadedImages().get(1).sortOrder()).isEqualTo(2);
        assertThat(response.uploadedImages().get(1).isThumbnail()).isFalse();
        verify(storageUploadUtils).upload(firstFile, "items");
        verify(storageUploadUtils).upload(secondFile, "items");
    }

    @DisplayName("item image upload appends sort order and does not create thumbnail when images already exist")
    @Test
    void uploadItemImagesAppendsAfterExistingImages() {
        Long itemId = 1L;
        Long clientId = 10L;
        Item item = itemWithSellerId(clientId);
        MockMultipartFile file = imageFile("next.webp");
        String imageUrl = "https://test-bucket.s3.ap-northeast-2.amazonaws.com/items/next.webp";

        given(itemService.getValidatedItem(itemId, clientId)).willReturn(item);
        given(itemImageService.countImagesByItemId(itemId)).willReturn(3);
        given(storageUploadUtils.upload(file, "items")).willReturn(imageUrl);
        given(itemImageService.saveItemImage(item, imageUrl, 4, false))
                .willReturn(new ItemImageUploadResponse.ImageInfo(200L, imageUrl, 4, false));

        ItemImageUploadResponse response = itemImageFacade.uploadItemImages(itemId, List.of(file), clientId);

        assertThat(response.uploadedImages()).singleElement().satisfies(image -> {
            assertThat(image.sortOrder()).isEqualTo(4);
            assertThat(image.isThumbnail()).isFalse();
        });
        verify(itemImageService).saveItemImage(item, imageUrl, 4, false);
    }

    @DisplayName("item image upload rejects unsupported extensions before storage upload")
    @Test
    void uploadItemImagesRejectsUnsupportedExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "malware.exe",
                "application/octet-stream",
                new byte[] {1});

        assertThatThrownBy(() -> itemImageFacade.uploadItemImages(1L, List.of(file), 10L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));

        verify(itemService, never()).getValidatedItem(1L, 10L);
        verify(storageUploadUtils, never()).upload(any(MultipartFile.class), any(String.class));
    }

    @DisplayName("item image upload deletes already uploaded files when metadata save fails")
    @Test
    void uploadItemImagesDeletesUploadedFilesWhenSaveFails() {
        Long itemId = 1L;
        Long clientId = 10L;
        Item item = itemWithSellerId(clientId);
        MockMultipartFile file = imageFile("rollback.jpg");
        String imageUrl = "https://test-bucket.s3.ap-northeast-2.amazonaws.com/items/rollback.jpg";

        given(itemService.getValidatedItem(itemId, clientId)).willReturn(item);
        given(itemImageService.countImagesByItemId(itemId)).willReturn(0);
        given(storageUploadUtils.upload(file, "items")).willReturn(imageUrl);
        given(itemImageService.saveItemImage(item, imageUrl, 1, true))
                .willThrow(new RuntimeException("database failure"));

        assertThatThrownBy(() -> itemImageFacade.uploadItemImages(itemId, List.of(file), clientId))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR));

        verify(storageUploadUtils).delete(imageUrl, "items");
    }

    private MockMultipartFile imageFile(String filename) {
        return new MockMultipartFile(
                "files",
                filename,
                "image/jpeg",
                new byte[] {1, 2, 3});
    }

    private Item itemWithSellerId(Long sellerId) {
        Client seller = Client.create(
                "seller@example.com",
                "encodedPassword",
                "seller",
                "seller",
                "010-1234-5678");
        org.springframework.test.util.ReflectionTestUtils.setField(seller, "id", sellerId);

        return Item.builder()
                .seller(seller)
                .title("item")
                .description("description")
                .initialPrice(10000L)
                .tradeType(TradeType.DIRECT)
                .conditionType(ConditionType.USED)
                .tradeStatus(TradeStatus.ON_SALE)
                .isDraft(false)
                .build();
    }
}
