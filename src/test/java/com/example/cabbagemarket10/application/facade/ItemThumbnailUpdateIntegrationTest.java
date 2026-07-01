package com.example.cabbagemarket10.application.facade;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.cabbagemarket10.domain.category.entity.Category;
import com.example.cabbagemarket10.domain.category.repository.CategoryRepository;
import com.example.cabbagemarket10.domain.client.entity.Client;
import com.example.cabbagemarket10.domain.client.repository.ClientRepository;
import com.example.cabbagemarket10.domain.item.entity.Item;
import com.example.cabbagemarket10.domain.item.enums.ConditionType;
import com.example.cabbagemarket10.domain.item.enums.TradeStatus;
import com.example.cabbagemarket10.domain.item.enums.TradeType;
import com.example.cabbagemarket10.domain.item.repository.ItemRepository;
import com.example.cabbagemarket10.domain.itemImage.dto.response.ItemThumbnailUpdateResponse;
import com.example.cabbagemarket10.domain.itemImage.entity.ItemImage;
import com.example.cabbagemarket10.domain.itemImage.repository.ItemImageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class ItemThumbnailUpdateIntegrationTest {

    @Autowired
    private ItemImageFacade itemImageFacade;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ItemImageRepository itemImageRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        deleteIfExists("chat_message");
        deleteIfExists("chat_room");
        deleteIfExists("inquiry_log");
        deleteIfExists("item_image");
        deleteIfExists("follow");
        deleteIfExists("review");
        deleteIfExists("auction_status");
        deleteIfExists("item");
        deleteIfExists("category");
        deleteIfExists("client");
    }

    @DisplayName("updating thumbnail stores existing thumbnail false and selected image true")
    @Test
    void updateItemThumbnailPersistsOnlySelectedImageAsThumbnail() {
        Client seller = clientRepository.save(Client.create(
                "seller@example.com",
                "encodedPassword",
                "seller",
                "seller",
                "010-1234-5678"));
        Category category = categoryRepository.save(Category.builder()
                .name("vegetable")
                .sortOrder(1)
                .isActive(true)
                .build());
        Item item = itemRepository.save(Item.builder()
                .seller(seller)
                .category(category)
                .title("cabbage")
                .description("fresh cabbage")
                .initialPrice(10000L)
                .tradeType(TradeType.DIRECT)
                .conditionType(ConditionType.USED)
                .tradeStatus(TradeStatus.ON_SALE)
                .isDraft(false)
                .build());
        ItemImage existingThumbnail = itemImageRepository.save(ItemImage.builder()
                .item(item)
                .imageUrl("https://cdn.example.com/items/old.jpg")
                .sortOrder(1)
                .isThumbnail(true)
                .build());
        ItemImage selectedImage = itemImageRepository.save(ItemImage.builder()
                .item(item)
                .imageUrl("https://cdn.example.com/items/new.jpg")
                .sortOrder(2)
                .isThumbnail(false)
                .build());

        ItemThumbnailUpdateResponse response = itemImageFacade.updateItemThumbnail(
                item.getId(),
                selectedImage.getId(),
                seller.getId());

        assertThat(response.itemId()).isEqualTo(item.getId());
        assertThat(response.thumbnailImageId()).isEqualTo(selectedImage.getId());
        assertThat(response.thumbnailUrl()).isEqualTo(selectedImage.getImageUrl());

        Boolean oldThumbnail = jdbcTemplate.queryForObject(
                "select is_thumbnail from item_image where id = ?",
                Boolean.class,
                existingThumbnail.getId());
        Boolean newThumbnail = jdbcTemplate.queryForObject(
                "select is_thumbnail from item_image where id = ?",
                Boolean.class,
                selectedImage.getId());

        assertThat(oldThumbnail).isFalse();
        assertThat(newThumbnail).isTrue();
    }

    private void deleteIfExists(String tableName) {
        try {
            jdbcTemplate.update("delete from " + tableName);
        } catch (DataAccessException ignored) {
        }
    }
}
