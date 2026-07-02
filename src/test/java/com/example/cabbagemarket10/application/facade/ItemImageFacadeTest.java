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
import com.example.cabbagemarket10.domain.itemImage.entity.ItemImage;
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
import org.springframework.test.util.ReflectionTestUtils;
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

    @DisplayName("상품이미지_업로드는_파일을_저장하고_이미지_메타데이터를_저장한다")
    @Test
    void 상품이미지_업로드는_파일을_저장하고_이미지_메타데이터를_저장한다() {
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

    @DisplayName("상품이미지_업로드는_기존_이미지_다음_순서로_추가한다")
    @Test
    void 상품이미지_업로드는_기존_이미지_다음_순서로_추가한다() {
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

    @DisplayName("지원하지_않는_확장자의_상품이미지_업로드는_거부된다")
    @Test
    void 지원하지_않는_확장자의_상품이미지_업로드는_거부된다() {
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

    @DisplayName("상품이미지_저장에_실패하면_업로드된_파일을_삭제한다")
    @Test
    void 상품이미지_저장에_실패하면_업로드된_파일을_삭제한다() {
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

    @DisplayName("상품이미지_삭제는_저장소_파일과_이미지_메타데이터를_삭제한다")
    @Test
    void 상품이미지_삭제는_저장소_파일과_이미지_메타데이터를_삭제한다() {
        Long itemId = 1L;
        Long imageId = 100L;
        Long clientId = 10L;
        String imageUrl = "https://test-bucket.s3.ap-northeast-2.amazonaws.com/items/delete.jpg";
        Item item = itemWithIdAndSellerId(itemId, clientId);
        ItemImage itemImage = itemImage(item, imageId, imageUrl, false);

        given(itemService.getValidatedItem(itemId, clientId)).willReturn(item);
        given(itemImageService.getValidItemImage(imageId, itemId)).willReturn(itemImage);

        itemImageFacade.deleteItemImage(itemId, imageId, clientId);

        verify(storageUploadUtils).delete(imageUrl, "items");
        verify(itemImageService).delete(itemImage);
    }

    @DisplayName("검증에_실패한_상품이미지_삭제는_저장소_파일을_건드리지_않는다")
    @Test
    void 검증에_실패한_상품이미지_삭제는_저장소_파일을_건드리지_않는다() {
        Long itemId = 1L;
        Long imageId = 100L;
        Long clientId = 10L;
        Item item = itemWithIdAndSellerId(itemId, clientId);

        given(itemService.getValidatedItem(itemId, clientId)).willReturn(item);
        given(itemImageService.getValidItemImage(imageId, itemId))
                .willThrow(new BusinessException(ErrorCode.CANNOT_DELETE_THUMBNAIL));

        assertThatThrownBy(() -> itemImageFacade.deleteItemImage(itemId, imageId, clientId))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CANNOT_DELETE_THUMBNAIL));

        verify(storageUploadUtils, never()).delete(any(String.class), any(String.class));
        verify(itemImageService, never()).delete(any(ItemImage.class));
    }

    private MockMultipartFile imageFile(String filename) {
        return new MockMultipartFile(
                "files",
                filename,
                "image/jpeg",
                new byte[] {1, 2, 3});
    }

    private Item itemWithSellerId(Long sellerId) {
        return itemWithIdAndSellerId(null, sellerId);
    }

    private Item itemWithIdAndSellerId(Long itemId, Long sellerId) {
        Client seller = Client.create(
                "seller@example.com",
                "encodedPassword",
                "seller",
                "seller",
                "010-1234-5678");
        ReflectionTestUtils.setField(seller, "id", sellerId);

        Item item = Item.builder()
                .seller(seller)
                .title("item")
                .description("description")
                .initialPrice(10000L)
                .tradeType(TradeType.DIRECT)
                .conditionType(ConditionType.USED)
                .tradeStatus(TradeStatus.ON_SALE)
                .isDraft(false)
                .build();
        ReflectionTestUtils.setField(item, "id", itemId);
        return item;
    }

    private ItemImage itemImage(Item item, Long imageId, String imageUrl, boolean isThumbnail) {
        ItemImage itemImage = ItemImage.builder()
                .item(item)
                .imageUrl(imageUrl)
                .sortOrder(1)
                .isThumbnail(isThumbnail)
                .build();
        ReflectionTestUtils.setField(itemImage, "id", imageId);
        return itemImage;
    }
}
