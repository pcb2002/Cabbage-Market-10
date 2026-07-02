package com.example.cabbagemarket10.domain.itemImage.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.example.cabbagemarket10.domain.item.entity.Item;
import com.example.cabbagemarket10.domain.item.enums.ConditionType;
import com.example.cabbagemarket10.domain.item.enums.TradeStatus;
import com.example.cabbagemarket10.domain.item.enums.TradeType;
import com.example.cabbagemarket10.domain.itemImage.entity.ItemImage;
import com.example.cabbagemarket10.domain.itemImage.repository.ItemImageRepository;
import com.example.cabbagemarket10.global.exception.BusinessException;
import com.example.cabbagemarket10.global.exception.ErrorCode;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ItemImageServiceTest {

    @Mock
    private ItemImageRepository itemImageRepository;

    @InjectMocks
    private ItemImageService itemImageService;

    @DisplayName("유효한_상품이미지를_조회하면_상품에_속한_일반_이미지를_반환한다")
    @Test
    void 유효한_상품이미지를_조회하면_상품에_속한_일반_이미지를_반환한다() {
        Item item = item(1L);
        ItemImage image = itemImage(10L, item, false);
        given(itemImageRepository.findById(10L)).willReturn(Optional.of(image));

        ItemImage result = itemImageService.getValidItemImage(10L, 1L);

        assertThat(result).isSameAs(image);
    }

    @DisplayName("이미지가_없으면_이미지_없음_예외가_발생한다")
    @Test
    void 이미지가_없으면_이미지_없음_예외가_발생한다() {
        given(itemImageRepository.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> itemImageService.getValidItemImage(10L, 1L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.IMAGE_NOT_FOUND));
    }

    @DisplayName("다른_상품의_이미지이면_권한_예외가_발생한다")
    @Test
    void 다른_상품의_이미지이면_권한_예외가_발생한다() {
        ItemImage image = itemImage(10L, item(2L), false);
        given(itemImageRepository.findById(10L)).willReturn(Optional.of(image));

        assertThatThrownBy(() -> itemImageService.getValidItemImage(10L, 1L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    @DisplayName("대표_이미지이면_조회할_수_없다")
    @Test
    void 대표_이미지이면_조회할_수_없다() {
        ItemImage image = itemImage(10L, item(1L), true);
        given(itemImageRepository.findById(10L)).willReturn(Optional.of(image));

        assertThatThrownBy(() -> itemImageService.getValidItemImage(10L, 1L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CANNOT_DELETE_THUMBNAIL));
    }

    private Item item(Long itemId) {
        Item item = Item.builder()
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

    private ItemImage itemImage(Long imageId, Item item, boolean isThumbnail) {
        ItemImage image = ItemImage.builder()
                .item(item)
                .imageUrl("https://cdn.example.com/items/%d.jpg".formatted(imageId))
                .sortOrder(1)
                .isThumbnail(isThumbnail)
                .build();
        ReflectionTestUtils.setField(image, "id", imageId);
        return image;
    }
}
