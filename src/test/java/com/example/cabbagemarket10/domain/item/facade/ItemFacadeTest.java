package com.example.cabbagemarket10.domain.item.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.cabbagemarket10.domain.auction.entity.AuctionStatus;
import com.example.cabbagemarket10.domain.auction.service.AuctionStatusService;
import com.example.cabbagemarket10.domain.category.entity.Category;
import com.example.cabbagemarket10.domain.category.service.CategoryService;
import com.example.cabbagemarket10.domain.client.entity.Client;
import com.example.cabbagemarket10.domain.client.service.ClientService;
import com.example.cabbagemarket10.domain.item.dto.request.ItemCreateRequest;
import com.example.cabbagemarket10.domain.item.dto.response.ItemCreateResponse;
import com.example.cabbagemarket10.domain.item.entity.Item;
import com.example.cabbagemarket10.domain.item.enums.ConditionType;
import com.example.cabbagemarket10.domain.item.enums.TradeStatus;
import com.example.cabbagemarket10.domain.item.enums.TradeType;
import com.example.cabbagemarket10.domain.item.service.ItemService;
import com.example.cabbagemarket10.domain.itemImage.service.ItemImageService;
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

    @InjectMocks
    private ItemFacade itemFacade;

    @DisplayName("auction item creation returns full item response")
    @Test
    void createAuctionItemReturnsFullResponse() {
        LocalDateTime closeDate = LocalDateTime.of(2026, 7, 10, 12, 0);
        LocalDateTime createdAt = LocalDateTime.of(2026, 7, 2, 9, 30);
        Client seller = client(1L);
        Category category = category(2L);
        ItemCreateRequest request = new ItemCreateRequest(
                category.getId(),
                "auction cabbage",
                TradeType.AUCTION,
                ConditionType.NEW,
                "fresh cabbage",
                12000L,
                closeDate);
        Item item = item(10L, seller, category, TradeType.AUCTION, createdAt);
        AuctionStatus auctionStatus = AuctionStatus.builder()
                .item(item)
                .currentBid(12000L)
                .closeDate(closeDate)
                .build();

        given(clientService.getClient(seller.getId())).willReturn(seller);
        given(categoryService.getCategory(category.getId())).willReturn(category);
        given(itemService.saveItem(seller, category, request)).willReturn(item);
        given(auctionStatusService.createAuctionStatus(item, request.initialPrice(), request.closeDate()))
                .willReturn(auctionStatus);

        ItemCreateResponse response = itemFacade.createItem(seller.getId(), request);

        assertThat(response.itemId()).isEqualTo(item.getId());
        assertThat(response.sellerId()).isEqualTo(seller.getId());
        assertThat(response.categoryId()).isEqualTo(category.getId());
        assertThat(response.tradeType()).isEqualTo(TradeType.AUCTION);
        assertThat(response.title()).isEqualTo("auction cabbage");
        assertThat(response.description()).isEqualTo("fresh cabbage");
        assertThat(response.initialPrice()).isEqualTo(12000L);
        assertThat(response.currentBid()).isEqualTo(12000L);
        assertThat(response.conditionType()).isEqualTo(ConditionType.NEW);
        assertThat(response.tradeStatus()).isEqualTo(TradeStatus.ON_SALE);
        assertThat(response.viewCount()).isZero();
        assertThat(response.likeCount()).isZero();
        assertThat(response.inquiryCount()).isZero();
        assertThat(response.isDraft()).isFalse();
        assertThat(response.closeDate()).isEqualTo(closeDate);
        assertThat(response.createdAt()).isEqualTo(createdAt);
    }

    @DisplayName("direct item creation does not create auction status")
    @Test
    void createDirectItemDoesNotCreateAuctionStatus() {
        Client seller = client(1L);
        Category category = category(2L);
        ItemCreateRequest request = new ItemCreateRequest(
                category.getId(),
                "direct cabbage",
                TradeType.DIRECT,
                ConditionType.USED,
                "stored cabbage",
                8000L,
                null);
        Item item = item(11L, seller, category, TradeType.DIRECT, LocalDateTime.of(2026, 7, 2, 10, 0));

        given(clientService.getClient(seller.getId())).willReturn(seller);
        given(categoryService.getCategory(category.getId())).willReturn(category);
        given(itemService.saveItem(seller, category, request)).willReturn(item);

        ItemCreateResponse response = itemFacade.createItem(seller.getId(), request);

        assertThat(response.itemId()).isEqualTo(item.getId());
        assertThat(response.currentBid()).isNull();
        assertThat(response.closeDate()).isNull();
        verify(auctionStatusService, never()).createAuctionStatus(item, request.initialPrice(), request.closeDate());
    }

    private Client client(Long id) {
        Client client = Client.create(
                "seller%d@example.com".formatted(id),
                "encodedPassword",
                "seller%d".formatted(id),
                "seller%d".formatted(id),
                "010-1234-5678");
        ReflectionTestUtils.setField(client, "id", id);
        return client;
    }

    private Category category(Long id) {
        Category category = Category.builder()
                .name("category%d".formatted(id))
                .sortOrder(1)
                .isActive(true)
                .build();
        ReflectionTestUtils.setField(category, "id", id);
        return category;
    }

    private Item item(Long id, Client seller, Category category, TradeType tradeType, LocalDateTime createdAt) {
        Item item = Item.builder()
                .seller(seller)
                .category(category)
                .tradeType(tradeType)
                .title(tradeType == TradeType.AUCTION ? "auction cabbage" : "direct cabbage")
                .description(tradeType == TradeType.AUCTION ? "fresh cabbage" : "stored cabbage")
                .initialPrice(tradeType == TradeType.AUCTION ? 12000L : 8000L)
                .conditionType(tradeType == TradeType.AUCTION ? ConditionType.NEW : ConditionType.USED)
                .tradeStatus(TradeStatus.ON_SALE)
                .isDraft(false)
                .build();
        ReflectionTestUtils.setField(item, "id", id);
        ReflectionTestUtils.setField(item, "createdAt", createdAt);
        return item;
    }
}
