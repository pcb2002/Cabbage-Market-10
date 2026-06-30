package com.example.cabbagemarket10.domain.item.service;

import com.example.cabbagemarket10.domain.category.entity.Category;
import com.example.cabbagemarket10.domain.client.entity.Client;
import com.example.cabbagemarket10.domain.item.dto.request.ItemCreateRequest;
import com.example.cabbagemarket10.domain.item.dto.request.ItemDraftRequest;
import com.example.cabbagemarket10.domain.item.dto.response.ItemDetailResponse;
import com.example.cabbagemarket10.domain.item.dto.response.ItemListItemResponse;
import com.example.cabbagemarket10.domain.item.dto.response.ItemStatusUpdateResponse;
import com.example.cabbagemarket10.domain.item.entity.Item;
import com.example.cabbagemarket10.domain.item.enums.TradeStatus;
import com.example.cabbagemarket10.domain.item.repository.ItemRepository;
import com.example.cabbagemarket10.global.exception.BusinessException;
import com.example.cabbagemarket10.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;

    public Item saveItem(Client seller, Category category, ItemCreateRequest request) {
        Item item = Item.builder()
                .seller(seller)
                .category(category)
                .title(request.title())
                .description(request.description())
                .initialPrice(request.initialPrice())
                .tradeType(request.tradeType())
                .conditionType(request.conditionType())
                .isDraft(false)
                .tradeStatus(TradeStatus.ON_SALE)
                .build();
        return itemRepository.save(item);
    }

    public Item saveItemDraft(Client seller, Category category, ItemDraftRequest request) {
        Item item = Item.builder()
                .seller(seller)
                .category(category)
                .title(request.title())
                .description(request.description())
                .initialPrice(request.initialPrice())
                .tradeType(request.tradeType())
                .conditionType(request.conditionType())
                .isDraft(true)
                .tradeStatus(TradeStatus.ON_SALE)
                .build();
        return itemRepository.save(item);
    }

    @Transactional(readOnly = true)
    public Page<ItemListItemResponse> getItemList(Long categoryId, String tradeStatus, Pageable pageable) {
        return itemRepository.searchItems(categoryId, tradeStatus, pageable);
    }

    @Transactional
    public ItemDetailResponse getItemDetail(Long itemId) {
        int updatedRows = itemRepository.incrementViewCount(itemId);
        if (updatedRows == 0) {
            throw new BusinessException(ErrorCode.ITEM_NOT_FOUND);
        }

        return itemRepository.findItemDetail(itemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ITEM_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public Item getItem(Long itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ITEM_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public Item getItemValidatingAuthor(Long itemId, Long clientId) {
        Item item = getItem(itemId);

        // 작성자 권한 검증
        item.verifySeller(clientId);

        return item;
    }

    public void softDelete(Item item) {
        itemRepository.delete(item);
    }

    public void hardDeleteById(Long itemId) {
        itemRepository.hardDeleteById(itemId);
    }

    @Transactional
    public ItemStatusUpdateResponse updateItemStatus(Item item, TradeStatus tradeStatus, Client client) {
        item.verifySeller(item.getSeller().getId());
        item.validateStatusUpdatable();

        item.updateStatus(tradeStatus, client);
        itemRepository.flush();

        Long buyerId = item.getBuyer() == null ? null : item.getBuyer().getId();
        return new ItemStatusUpdateResponse(item.getId(), item.getTradeStatus(), buyerId, item.getUpdatedAt());
    }

    public void flush() {
        itemRepository.flush();
    }
}
