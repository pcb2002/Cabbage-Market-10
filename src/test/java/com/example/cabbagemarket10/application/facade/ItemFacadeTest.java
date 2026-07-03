package com.example.cabbagemarket10.application.facade;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.cabbagemarket10.domain.auction.service.AuctionStatusService;
import com.example.cabbagemarket10.domain.category.entity.Category;
import com.example.cabbagemarket10.domain.category.service.CategoryService;
import com.example.cabbagemarket10.domain.client.entity.Client;
import com.example.cabbagemarket10.domain.client.service.ClientService;
import com.example.cabbagemarket10.domain.item.dto.request.ItemCreateRequest;
import com.example.cabbagemarket10.domain.item.dto.request.ItemStatusUpdateRequest;
import com.example.cabbagemarket10.domain.item.dto.response.ItemStatusUpdateResponse;
import com.example.cabbagemarket10.domain.item.entity.Item;
import com.example.cabbagemarket10.domain.item.enums.ConditionType;
import com.example.cabbagemarket10.domain.item.enums.TradeStatus;
import com.example.cabbagemarket10.domain.item.enums.TradeType;
import com.example.cabbagemarket10.domain.item.facade.ItemFacade;
import com.example.cabbagemarket10.domain.item.service.ItemService;
import com.example.cabbagemarket10.domain.itemImage.service.ItemImageService;
import com.example.cabbagemarket10.global.config.cache.SearchCacheEvictionService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ItemFacadeTest {

    @Mock
    private ClientService clientService;

    @Mock
    private CategoryService categoryService;

    @Mock
    private ItemService itemService;

    @Mock
    private AuctionStatusService auctionStatusService;

    @Mock
    private ItemImageService itemImageService;

    @Mock
    private SearchCacheEvictionService searchCacheEvictionService;

    @InjectMocks
    private ItemFacade itemFacade;

    @DisplayName("상품 생성 후 검색 캐시를 비운다")
    @Test
    void 상품_생성_후_검색_캐시를_비운다() {
        Client seller = client(1L);
        Category category = Category.builder().name("채소").sortOrder(1).isActive(true).build();
        Item item = item(10L, seller, false);
        ItemCreateRequest request = new ItemCreateRequest(
                1L, "배추", TradeType.AUCTION, ConditionType.USED, "설명", 10_000L, LocalDateTime.now().plusDays(1));

        given(clientService.getClient(1L)).willReturn(seller);
        given(categoryService.getCategory(1L)).willReturn(category);
        given(itemService.saveItem(seller, category, request)).willReturn(item);

        itemFacade.createItem(1L, request);

        verify(searchCacheEvictionService).evictItemSearchV2AfterCommit();
    }

    @DisplayName("상품 상태 변경 후 검색 캐시를 비운다")
    @Test
    void 상품_상태_변경_후_검색_캐시를_비운다() {
        Client seller = client(1L);
        Item item = item(10L, seller, false);
        ItemStatusUpdateRequest request = new ItemStatusUpdateRequest(TradeStatus.SOLD_OUT, 2L);
        ItemStatusUpdateResponse response = new ItemStatusUpdateResponse(10L, TradeStatus.SOLD_OUT, 2L, LocalDateTime.now());
        Client buyer = client(2L);

        given(itemService.getItemValidatingAuthor(10L, 1L)).willReturn(item);
        given(clientService.getClient(2L)).willReturn(buyer);
        given(itemService.updateItemStatus(item, TradeStatus.SOLD_OUT, buyer)).willReturn(response);

        itemFacade.updateItemStatus(10L, 1L, request);

        verify(searchCacheEvictionService).evictItemSearchV2AfterCommit();
    }

    @DisplayName("게시 상품 삭제 후 검색 캐시를 비운다")
    @Test
    void 게시_상품_삭제_후_검색_캐시를_비운다() {
        Client seller = client(1L);
        Item item = item(10L, seller, false);

        given(itemService.getItemValidatingAuthor(10L, 1L)).willReturn(item);

        itemFacade.deleteItem(10L, 1L);

        verify(itemService).softDelete(item);
        verify(searchCacheEvictionService).evictItemSearchV2AfterCommit();
    }

    @DisplayName("임시저장 상품 hard delete는 검색 캐시를 비우지 않는다")
    @Test
    void 임시저장_상품_hard_delete는_검색_캐시를_비우지_않는다() {
        Client seller = client(1L);
        Item item = item(10L, seller, true);

        given(itemService.getItemValidatingAuthor(10L, 1L)).willReturn(item);

        itemFacade.deleteItem(10L, 1L);

        verify(auctionStatusService).deleteByItemId(10L);
        verify(itemImageService).deleteByItemId(10L);
        verify(itemService).hardDeleteById(10L);
        verify(searchCacheEvictionService, never()).evictItemSearchV2AfterCommit();
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

    private Item item(Long itemId, Client seller, boolean isDraft) {
        Item item = Item.builder()
                .seller(seller)
                .category(Category.builder().name("채소").sortOrder(1).isActive(true).build())
                .tradeType(TradeType.DIRECT)
                .title("상품")
                .description("설명")
                .initialPrice(10_000L)
                .conditionType(ConditionType.USED)
                .tradeStatus(TradeStatus.ON_SALE)
                .isDraft(isDraft)
                .build();
        ReflectionTestUtils.setField(item, "id", itemId);
        return item;
    }
}
