package com.example.cabbagemarket10.application.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.example.cabbagemarket10.domain.client.entity.Client;
import com.example.cabbagemarket10.domain.client.service.ClientService;
import com.example.cabbagemarket10.domain.item.dto.response.ItemLikeToggleResponse;
import com.example.cabbagemarket10.domain.item.entity.Item;
import com.example.cabbagemarket10.domain.item.enums.ConditionType;
import com.example.cabbagemarket10.domain.item.enums.TradeStatus;
import com.example.cabbagemarket10.domain.item.enums.TradeType;
import com.example.cabbagemarket10.domain.item.facade.ItemLikeFacade;
import com.example.cabbagemarket10.domain.item.service.ItemService;
import com.example.cabbagemarket10.domain.itemLike.entity.ItemLike;
import com.example.cabbagemarket10.domain.itemLike.service.ItemLikeService;
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
class ItemLikeFacadeTest {

    @Mock
    private ItemService itemService;

    @Mock
    private ClientService clientService;

    @Mock
    private ItemLikeService itemLikeService;

    @InjectMocks
    private ItemLikeFacade itemLikeFacade;

    @DisplayName("좋아요가 없으면 상품 좋아요를 등록하고 좋아요 수를 증가시킨다")
    @Test
    void 좋아요가_없으면_상품_좋아요를_등록하고_좋아요_수를_증가시킨다() {
        Item item = item(10L);
        Client client = client(1L);

        given(itemService.getItemForUpdate(10L)).willReturn(item);
        given(clientService.getClient(1L)).willReturn(client);
        given(itemLikeService.findByClientIdAndItemId(1L, 10L)).willReturn(Optional.empty());
        given(itemService.getLikeCount(10L)).willReturn(1L);

        ItemLikeToggleResponse response = itemLikeFacade.toggle(10L, 1L);

        assertThat(response.itemId()).isEqualTo(10L);
        assertThat(response.liked()).isTrue();
        assertThat(response.likeCount()).isEqualTo(1L);
        verify(itemLikeService).save(client, item);
        verify(itemService).incrementLikeCount(10L);
    }

    @DisplayName("기존 좋아요가 있으면 상품 좋아요를 취소하고 좋아요 수를 감소시킨다")
    @Test
    void 기존_좋아요가_있으면_상품_좋아요를_취소하고_좋아요_수를_감소시킨다() {
        Item item = item(10L);
        Client client = client(1L);
        ItemLike itemLike = ItemLike.builder()
                .client(client)
                .item(item)
                .build();

        given(itemService.getItemForUpdate(10L)).willReturn(item);
        given(clientService.getClient(1L)).willReturn(client);
        given(itemLikeService.findByClientIdAndItemId(1L, 10L)).willReturn(Optional.of(itemLike));
        given(itemService.getLikeCount(10L)).willReturn(1L);

        ItemLikeToggleResponse response = itemLikeFacade.toggle(10L, 1L);

        assertThat(response.liked()).isFalse();
        assertThat(response.likeCount()).isEqualTo(1L);
        verify(itemLikeService).delete(itemLike);
        verify(itemService).decrementLikeCount(10L);
    }

    @DisplayName("회원이 없으면 CLIENT_NOT_FOUND 예외가 발생한다")
    @Test
    void 회원이_없으면_CLIENT_NOT_FOUND_예외가_발생한다() {
        Item item = item(10L);

        given(itemService.getItemForUpdate(10L)).willReturn(item);
        given(clientService.getClient(1L))
                .willThrow(new BusinessException(ErrorCode.CLIENT_NOT_FOUND));

        assertThatThrownBy(() -> itemLikeFacade.toggle(10L, 1L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CLIENT_NOT_FOUND));
    }

    private Item item(Long itemId) {
        Item item = Item.builder()
                .category(null)
                .seller(client(2L))
                .tradeType(TradeType.DIRECT)
                .title("좋아요 상품")
                .description("좋아요 테스트 상품")
                .initialPrice(1000L)
                .conditionType(ConditionType.USED)
                .tradeStatus(TradeStatus.ON_SALE)
                .isDraft(false)
                .build();
        ReflectionTestUtils.setField(item, "id", itemId);
        return item;
    }

    private Client client(Long clientId) {
        Client client = Client.create(
                "client%d@example.com".formatted(clientId),
                "encodedPassword",
                "회원%d".formatted(clientId),
                "회원%d".formatted(clientId),
                "010-1234-5678");
        ReflectionTestUtils.setField(client, "id", clientId);
        return client;
    }
}
