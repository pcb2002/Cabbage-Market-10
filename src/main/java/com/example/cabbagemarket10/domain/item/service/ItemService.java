package com.example.cabbagemarket10.domain.item.service;

import com.example.cabbagemarket10.domain.auction.entity.AuctionStatus;
import com.example.cabbagemarket10.domain.auction.repository.AuctionStatusRepository;
import com.example.cabbagemarket10.domain.category.entity.Category;
import com.example.cabbagemarket10.domain.category.repository.CategoryRepository;
import com.example.cabbagemarket10.domain.client.entity.Client;
import com.example.cabbagemarket10.domain.client.repository.ClientRepository;
import com.example.cabbagemarket10.domain.item.dto.request.ItemCreateRequest;
import com.example.cabbagemarket10.domain.item.dto.request.ItemDraftRequest;
import com.example.cabbagemarket10.domain.item.dto.response.ItemDraftResponse;
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

    @Transactional
    public ItemDraftResponse createItemDraft(Long sellerId, ItemDraftRequest request) {

        // 1. 판매자 조회 (이건 필수)
        Client seller = clientRepository.findById(sellerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CLIENT_NOT_FOUND));

        // 2. 카테고리 조회 (선택적으로 처리)
        Category category = null;
        if (request.categoryId() != null) {
            category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        }

        // 3. Item 임시저장 엔티티 생성
        Item item = Item.builder()
                .seller(seller)
                .category(category)
                .title(request.title())
                .description(request.description())
                .initialPrice(request.initialPrice())
                .tradeType(request.tradeType())
                .conditionType(request.conditionType())
                .isDraft(true) // 핵심: 작성 중인 문서임을 식별
                .tradeStatus(TradeStatus.ON_SALE) // 임시저장 상태이므로 대기 상태 등으로 설정
                .build();

        itemRepository.save(item);

        // 4. 경매 상태(AuctionStatus) 선택적 저장
        // 임시저장 단계에서도 closeDate나 initialPrice가 입력되었다면 빈 껍데기를 만들어 둘 수 있습니다.
        if (request.closeDate() != null || request.initialPrice() != null) {
            AuctionStatus auctionStatus = AuctionStatus.builder()
                    .item(item)
                    .currentBid(request.initialPrice() != null ? request.initialPrice() : 0L)
                    .closeDate(request.closeDate())
                    .build();
            auctionStatusRepository.save(auctionStatus);
        }

        return ItemDraftResponse.from(item);
    }
}