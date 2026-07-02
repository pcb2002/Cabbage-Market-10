package com.example.cabbagemarket10.global.init;

import com.example.cabbagemarket10.domain.auction.entity.AuctionStatus;
import com.example.cabbagemarket10.domain.auction.repository.AuctionStatusRepository;
import com.example.cabbagemarket10.domain.category.entity.Category;
import com.example.cabbagemarket10.domain.category.repository.CategoryRepository;
import com.example.cabbagemarket10.domain.chat.entity.ChatMessage;
import com.example.cabbagemarket10.domain.chat.entity.ChatRoom;
import com.example.cabbagemarket10.domain.chat.entity.MessageType;
import com.example.cabbagemarket10.domain.chat.repository.ChatMessageRepository;
import com.example.cabbagemarket10.domain.chat.repository.ChatRoomRepository;
import com.example.cabbagemarket10.domain.client.entity.Client;
import com.example.cabbagemarket10.domain.client.repository.ClientRepository;
import com.example.cabbagemarket10.domain.follow.entity.Follow;
import com.example.cabbagemarket10.domain.follow.repository.FollowRepository;
import com.example.cabbagemarket10.domain.inquiry.entity.InquiryLog;
import com.example.cabbagemarket10.domain.inquiry.repository.InquiryLogRepository;
import com.example.cabbagemarket10.domain.item.entity.Item;
import com.example.cabbagemarket10.domain.item.enums.ConditionType;
import com.example.cabbagemarket10.domain.item.enums.TradeStatus;
import com.example.cabbagemarket10.domain.item.enums.TradeType;
import com.example.cabbagemarket10.domain.item.repository.ItemRepository;
import com.example.cabbagemarket10.domain.itemImage.entity.ItemImage;
import com.example.cabbagemarket10.domain.itemImage.repository.ItemImageRepository;
import com.example.cabbagemarket10.domain.itemLike.entity.ItemLike;
import com.example.cabbagemarket10.domain.itemLike.repository.ItemLikeRepository;
import com.example.cabbagemarket10.domain.review.entity.Review;
import com.example.cabbagemarket10.domain.review.repository.ReviewRepository;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Profile("local")
@Order(100)
@RequiredArgsConstructor
public class LocalSampleDataInit implements CommandLineRunner {

    private static final String SAMPLE_SELLER_EMAIL = "local-seller@example.com";
    private static final String SAMPLE_PASSWORD = "Password1!";

    private final PasswordEncoder passwordEncoder;
    private final CategoryRepository categoryRepository;
    private final ClientRepository clientRepository;
    private final ItemRepository itemRepository;
    private final ItemImageRepository itemImageRepository;
    private final ItemLikeRepository itemLikeRepository;
    private final AuctionStatusRepository auctionStatusRepository;
    private final FollowRepository followRepository;
    private final InquiryLogRepository inquiryLogRepository;
    private final ReviewRepository reviewRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;

    @Override
    @Transactional
    public void run(String @NonNull ... args) {
        if (clientRepository.countByEmailIncludingDeleted(SAMPLE_SELLER_EMAIL) > 0) {
            log.info("로컬 샘플 데이터가 이미 존재하여 초기화를 건너뜁니다. email={}", SAMPLE_SELLER_EMAIL);
            return;
        }

        Map<String, Category> categories = ensureCategories();
        List<Client> clients = createClients();
        Client seller = clients.get(0);
        Client buyer = clients.get(1);
        Client bidder = clients.get(2);
        Client reviewer = clients.get(3);

        List<Item> items = createItems(categories, seller, buyer);
        createItemImages(items);
        createAuctionStatuses(items, bidder);
        createLikes(items, buyer, bidder, reviewer);
        createFollows(seller, buyer, bidder, reviewer);
        createInquiries(items, seller, buyer, bidder);
        createReviews(items, seller, buyer, reviewer);
        createChatRooms(items, seller, buyer, bidder);

        log.info("로컬 샘플 데이터 세팅 완료. 계정 비밀번호는 모두 {} 입니다.", SAMPLE_PASSWORD);
    }

    private Map<String, Category> ensureCategories() {
        if (categoryRepository.count() == 0) {
            categoryRepository.saveAll(List.of(
                    Category.builder().name("패션/의류").sortOrder(1).isActive(true).build(),
                    Category.builder().name("디지털/가전").sortOrder(2).isActive(true).build(),
                    Category.builder().name("도서/음반").sortOrder(3).isActive(true).build(),
                    Category.builder().name("스포츠/레저").sortOrder(4).isActive(true).build(),
                    Category.builder().name("생활/주방").sortOrder(5).isActive(true).build(),
                    Category.builder().name("취미/게임").sortOrder(6).isActive(true).build(),
                    Category.builder().name("뷰티/미용").sortOrder(7).isActive(true).build(),
                    Category.builder().name("기타").sortOrder(8).isActive(true).build()
            ));
        }

        return categoryRepository.findAll().stream()
                .sorted(Comparator.comparing(Category::getSortOrder))
                .collect(Collectors.toMap(Category::getName, Function.identity(), (left, right) -> left));
    }

    private List<Client> createClients() {
        String encodedPassword = passwordEncoder.encode(SAMPLE_PASSWORD);
        return clientRepository.saveAll(List.of(
                Client.create(SAMPLE_SELLER_EMAIL, encodedPassword, "미니멀라이프", "판매자", "010-1111-1111"),
                Client.create("local-buyer@example.com", encodedPassword, "배추도사", "구매자", "010-2222-2222"),
                Client.create("local-bidder@example.com", encodedPassword, "입찰왕", "입찰자", "010-3333-3333"),
                Client.create("local-reviewer@example.com", encodedPassword, "후기장인", "리뷰어", "010-4444-4444")
        ));
    }

    private List<Item> createItems(Map<String, Category> categories, Client seller, Client buyer) {
        Category digital = category(categories, "디지털/가전");
        Category living = category(categories, "생활/주방");
        Category hobby = category(categories, "취미/게임");
        Category book = category(categories, "도서/음반");
        Category fashion = category(categories, "패션/의류");

        Item soldKeyboard = Item.builder()
                .category(digital)
                .seller(seller)
                .tradeType(TradeType.DIRECT)
                .title("커스텀 기계식 키보드 HHKB 레이아웃")
                .description("윤활 작업 완료된 커스텀 키보드입니다. 조용하고 단단한 타건감입니다.")
                .initialPrice(280_000L)
                .conditionType(ConditionType.USED)
                .tradeStatus(TradeStatus.SOLD_OUT)
                .isDraft(false)
                .build();
        soldKeyboard.updateStatus(TradeStatus.SOLD_OUT, buyer);

        return itemRepository.saveAll(List.of(
                Item.builder()
                        .category(digital)
                        .seller(seller)
                        .tradeType(TradeType.DIRECT)
                        .title("라이카 M10-P 블랙 페인트 에디션")
                        .description("상태 좋은 카메라입니다. 생활 흠집은 거의 없고 렌즈 캡과 박스를 함께 드립니다.")
                        .initialPrice(8_500_000L)
                        .conditionType(ConditionType.USED)
                        .tradeStatus(TradeStatus.ON_SALE)
                        .isDraft(false)
                        .build(),
                Item.builder()
                        .category(digital)
                        .seller(seller)
                        .tradeType(TradeType.DIRECT)
                        .title("소니 WH-1000XM5 실버")
                        .description("노이즈 캔슬링이 좋은 헤드폰입니다. 실사용 기간은 6개월입니다.")
                        .initialPrice(320_000L)
                        .conditionType(ConditionType.USED)
                        .tradeStatus(TradeStatus.RESERVED)
                        .isDraft(false)
                        .build(),
                soldKeyboard,
                Item.builder()
                        .category(living)
                        .seller(seller)
                        .tradeType(TradeType.AUCTION)
                        .title("아르떼미데 네시노 테이블 램프")
                        .description("따뜻한 빛감의 테이블 램프입니다. 경매 상품으로 현재 입찰 중입니다.")
                        .initialPrice(185_000L)
                        .conditionType(ConditionType.USED)
                        .tradeStatus(TradeStatus.ON_SALE)
                        .isDraft(false)
                        .build(),
                Item.builder()
                        .category(living)
                        .seller(seller)
                        .tradeType(TradeType.DIRECT)
                        .title("펠로우 오드 그라인더 Gen 2")
                        .description("커피 그라인더입니다. 분쇄 균일도가 좋고 구성품 모두 있습니다.")
                        .initialPrice(350_000L)
                        .conditionType(ConditionType.USED)
                        .tradeStatus(TradeStatus.ON_SALE)
                        .isDraft(false)
                        .build(),
                Item.builder()
                        .category(book)
                        .seller(seller)
                        .tradeType(TradeType.DIRECT)
                        .title("브라운 계산기 ET66 복각판")
                        .description("미개봉 복각판 계산기입니다. 책상 위 소품으로도 좋습니다.")
                        .initialPrice(45_000L)
                        .conditionType(ConditionType.NEW)
                        .tradeStatus(TradeStatus.ON_SALE)
                        .isDraft(false)
                        .build(),
                Item.builder()
                        .category(hobby)
                        .seller(seller)
                        .tradeType(TradeType.DIRECT)
                        .title("벨로라인 클래식 시티 바이크")
                        .description("가벼운 출퇴근용 시티 바이크입니다.")
                        .initialPrice(210_000L)
                        .conditionType(ConditionType.USED)
                        .tradeStatus(TradeStatus.ON_SALE)
                        .isDraft(false)
                        .build(),
                Item.builder()
                        .category(fashion)
                        .seller(seller)
                        .tradeType(TradeType.DIRECT)
                        .title("빈티지 코튼 재킷")
                        .description("봄가을에 입기 좋은 빈티지 코튼 재킷입니다.")
                        .initialPrice(65_000L)
                        .conditionType(ConditionType.USED)
                        .tradeStatus(TradeStatus.ON_SALE)
                        .isDraft(true)
                        .build()
        ));
    }

    private Category category(Map<String, Category> categories, String name) {
        return categories.getOrDefault(name, categories.values().iterator().next());
    }

    private void createItemImages(List<Item> items) {
        String[][] images = {
                {
                        "https://images.unsplash.com/photo-1512790182412-b19e6d62bc39?auto=format&fit=crop&w=720&q=80",
                        "https://images.unsplash.com/photo-1516321318423-f06f85e504b3?auto=format&fit=crop&w=720&q=80"
                },
                {
                        "https://images.unsplash.com/photo-1546435770-a3e426bf472b?auto=format&fit=crop&w=720&q=80"
                },
                {
                        "https://images.unsplash.com/photo-1618384887929-16ec33fab9ef?auto=format&fit=crop&w=720&q=80"
                },
                {
                        "https://images.unsplash.com/photo-1507473885765-e6ed057f782c?auto=format&fit=crop&w=720&q=80",
                        "https://images.unsplash.com/photo-1540932239986-30128078f3c5?auto=format&fit=crop&w=720&q=80"
                },
                {
                        "https://images.unsplash.com/photo-1517668808822-9ebb02f2a0e6?auto=format&fit=crop&w=720&q=80"
                },
                {
                        "https://images.unsplash.com/photo-1564473185935-58113cba1e80?auto=format&fit=crop&w=720&q=80"
                },
                {
                        "https://images.unsplash.com/photo-1507035895480-2b3156c31fc8?auto=format&fit=crop&w=720&q=80"
                },
                {
                        "https://images.unsplash.com/photo-1523398002811-999ca8dec234?auto=format&fit=crop&w=720&q=80"
                }
        };

        for (int itemIndex = 0; itemIndex < items.size(); itemIndex++) {
            for (int imageIndex = 0; imageIndex < images[itemIndex].length; imageIndex++) {
                itemImageRepository.save(ItemImage.builder()
                        .item(items.get(itemIndex))
                        .imageUrl(images[itemIndex][imageIndex])
                        .sortOrder(imageIndex + 1)
                        .isThumbnail(imageIndex == 0)
                        .build());
            }
        }
    }

    private void createAuctionStatuses(List<Item> items, Client bidder) {
        Item auctionItem = items.get(3);
        AuctionStatus auctionStatus = AuctionStatus.builder()
                .item(auctionItem)
                .currentBid(auctionItem.getInitialPrice())
                .closeDate(LocalDateTime.now().plusDays(7))
                .build();
        auctionStatus.updateBid(211_000L, bidder.getId(), LocalDateTime.now());
        auctionStatusRepository.save(auctionStatus);
    }

    private void createLikes(List<Item> items, Client buyer, Client bidder, Client reviewer) {
        saveLike(buyer, items.get(0));
        saveLike(buyer, items.get(3));
        saveLike(bidder, items.get(0));
        saveLike(bidder, items.get(1));
        saveLike(reviewer, items.get(4));
        saveLike(reviewer, items.get(6));
    }

    private void saveLike(Client client, Item item) {
        item.incrementLikeCount();
        itemLikeRepository.save(ItemLike.builder()
                .client(client)
                .item(item)
                .build());
    }

    private void createFollows(Client seller, Client buyer, Client bidder, Client reviewer) {
        followRepository.saveAll(List.of(
                Follow.builder().follower(buyer).following(seller).build(),
                Follow.builder().follower(bidder).following(seller).build(),
                Follow.builder().follower(reviewer).following(seller).build(),
                Follow.builder().follower(seller).following(buyer).build()
        ));
    }

    private void createInquiries(List<Item> items, Client seller, Client buyer, Client bidder) {
        InquiryLog cameraQuestion = inquiryLogRepository.save(InquiryLog.builder()
                .item(items.get(0))
                .author(buyer)
                .title("직거래 가능 지역 문의")
                .description("주말에 합정역 근처에서 직거래 가능할까요?")
                .status("OPEN")
                .build());
        inquiryLogRepository.save(InquiryLog.builder()
                .item(items.get(0))
                .author(seller)
                .targetInquiry(cameraQuestion)
                .title("답변")
                .description("네, 토요일 오후 합정역 3번 출구에서 가능합니다.")
                .status("ANSWERED")
                .build());
        inquiryLogRepository.save(InquiryLog.builder()
                .item(items.get(3))
                .author(bidder)
                .title("경매 마감 문의")
                .description("즉시 구매는 어렵고 경매로만 진행하시나요?")
                .status("OPEN")
                .build());
    }

    private void createReviews(List<Item> items, Client seller, Client buyer, Client reviewer) {
        reviewRepository.saveAll(List.of(
                Review.builder()
                        .item(items.get(2))
                        .reviewer(buyer)
                        .reviewee(seller)
                        .rating(5)
                        .content("상품 상태 설명이 정확했고 거래가 빨랐습니다.")
                        .isDeleted(false)
                        .build(),
                Review.builder()
                        .item(items.get(2))
                        .reviewer(reviewer)
                        .reviewee(buyer)
                        .rating(4)
                        .content("약속 시간을 잘 지켜주셨어요.")
                        .isDeleted(false)
                        .build()
        ));
    }

    private void createChatRooms(List<Item> items, Client seller, Client buyer, Client bidder) {
        ChatRoom cameraRoom = chatRoomRepository.save(ChatRoom.builder()
                .item(items.get(0))
                .createdBy(buyer)
                .build());
        chatMessageRepository.saveAll(List.of(
                ChatMessage.builder()
                        .chatRoom(cameraRoom)
                        .sender(buyer)
                        .messageType(MessageType.TEXT)
                        .content("안녕하세요, 구매 가능한가요?")
                        .build(),
                ChatMessage.builder()
                        .chatRoom(cameraRoom)
                        .sender(seller)
                        .messageType(MessageType.TEXT)
                        .content("네, 가능합니다. 구성품은 모두 보관 중입니다.")
                        .build()
        ));

        ChatRoom auctionRoom = chatRoomRepository.save(ChatRoom.builder()
                .item(items.get(3))
                .createdBy(bidder)
                .build());
        chatMessageRepository.save(ChatMessage.builder()
                .chatRoom(auctionRoom)
                .sender(bidder)
                .messageType(MessageType.TEXT)
                .content("램프 실사용 사진을 더 볼 수 있을까요?")
                .build());
    }
}
