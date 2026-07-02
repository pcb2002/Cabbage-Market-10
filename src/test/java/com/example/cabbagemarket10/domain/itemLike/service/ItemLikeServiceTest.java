package com.example.cabbagemarket10.domain.itemLike.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.example.cabbagemarket10.domain.client.entity.Client;
import com.example.cabbagemarket10.domain.item.entity.Item;
import com.example.cabbagemarket10.domain.item.enums.ConditionType;
import com.example.cabbagemarket10.domain.item.enums.TradeStatus;
import com.example.cabbagemarket10.domain.item.enums.TradeType;
import com.example.cabbagemarket10.domain.itemLike.entity.ItemLike;
import com.example.cabbagemarket10.domain.itemLike.repository.ItemLikeRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ItemLikeServiceTest {

    @Mock
    private ItemLikeRepository itemLikeRepository;

    @InjectMocks
    private ItemLikeService itemLikeService;

    @DisplayName("회원과 상품으로 기존 좋아요를 조회한다")
    @Test
    void 회원과_상품으로_기존_좋아요를_조회한다() {
        ItemLike itemLike = ItemLike.builder()
                .client(client(1L))
                .item(item(10L))
                .build();
        given(itemLikeRepository.findByClient_IdAndItem_Id(1L, 10L))
                .willReturn(Optional.of(itemLike));

        Optional<ItemLike> result = itemLikeService.findByClientIdAndItemId(1L, 10L);

        assertThat(result).contains(itemLike);
    }

    @DisplayName("좋아요를 저장한다")
    @Test
    void 좋아요를_저장한다() {
        Client client = client(1L);
        Item item = item(10L);

        itemLikeService.save(client, item);

        verify(itemLikeRepository).save(org.mockito.ArgumentMatchers.any(ItemLike.class));
    }

    @DisplayName("좋아요를 삭제한다")
    @Test
    void 좋아요를_삭제한다() {
        ItemLike itemLike = ItemLike.builder()
                .client(client(1L))
                .item(item(10L))
                .build();

        itemLikeService.delete(itemLike);

        verify(itemLikeRepository).delete(itemLike);
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
