package com.example.cabbagemarket10.domain.item.service;

import com.example.cabbagemarket10.domain.auction.entity.AuctionStatus;
import com.example.cabbagemarket10.domain.auction.repository.AuctionStatusRepository;
import com.example.cabbagemarket10.domain.category.entity.Category;
import com.example.cabbagemarket10.domain.category.repository.CategoryRepository;
import com.example.cabbagemarket10.domain.client.entity.Client;
import com.example.cabbagemarket10.domain.client.repository.ClientRepository;
import com.example.cabbagemarket10.domain.item.dto.request.ItemCreateRequest;
import com.example.cabbagemarket10.domain.item.entity.Item;
import com.example.cabbagemarket10.domain.item.enums.TradeStatus;
import com.example.cabbagemarket10.domain.item.repository.ItemRepository;
import com.example.cabbagemarket10.global.exception.BusinessException;
import com.example.cabbagemarket10.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemService {

    private final ItemRepository itemRepository;
    private final AuctionStatusRepository auctionStatusRepository;
    private final ClientRepository clientRepository;
    private final CategoryRepository categoryRepository;

    @Transactional // 두 테이블(Item, AuctionStatus)에 대한 CUD 작업이므로 필수
    public Long createItem(Long sellerId, ItemCreateRequest request) {

        // 1. 연관관계 엔티티 조회 및 검증
        Client seller = clientRepository.findById(sellerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CLIENT_NOT_FOUND));

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        // 2. Item 엔티티 생성 (Acceptance Criteria: isDraft = false)
        Item item = Item.builder()
                .seller(seller)
                .category(category)
                .title(request.title())
                .description(request.description())
                .initialPrice(request.initialPrice())
                .tradeType(request.tradeType())
                .conditionType(request.conditionType())
                .isDraft(false) // 임시 저장이 아닌 정식 등록이므로 강제 세팅
                .tradeStatus(TradeStatus.valueOf("ON_SALE")) // 기본 판매 상태 세팅
                .build();

        // Item을 먼저 저장하여 생성된 ID(PK)를 확보 (AuctionStatus 매핑 시 사용됨)
        itemRepository.save(item);

        // 3. 1:1 식별 관계인 AuctionStatus 데이터 동시 생성
        AuctionStatus auctionStatus = AuctionStatus.builder()
                .item(item)
                // 초기 입찰가는 상품의 시작가(initialPrice)로 세팅
                .currentBid(request.initialPrice())
                .closeDate(request.closeDate())
                .build();

        auctionStatusRepository.save(auctionStatus);

        return item.getId();
    }
}