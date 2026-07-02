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

    private static final String SAMPLE_MINJI_EMAIL = "local-minji@example.com";
    private static final String SAMPLE_JUN_EMAIL = "local-jun@example.com";
    private static final String SAMPLE_PASSWORD = "Password1!";

    private final PasswordEncoder passwordEncoder;
    private final CategoryRepository categoryRepository;
    private final ClientRepository clientRepository;
    private final ItemRepository itemRepository;
    private final ItemImageRepository itemImageRepository;
    private final ItemLikeRepository itemLikeRepository;
    private final AuctionStatusRepository auctionStatusRepository;
    private final InquiryLogRepository inquiryLogRepository;
    private final ReviewRepository reviewRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;

    @Override
    @Transactional
    public void run(String @NonNull ... args) {
        if (clientRepository.countByEmailIncludingDeleted(SAMPLE_MINJI_EMAIL) > 0
                || clientRepository.countByEmailIncludingDeleted(SAMPLE_JUN_EMAIL) > 0) {
            log.info("Local sample data already exists. emails={}, {}", SAMPLE_MINJI_EMAIL, SAMPLE_JUN_EMAIL);
            return;
        }

        List<Category> categories = ensureCategories();
        Client minji = clientRepository.save(
                Client.create(SAMPLE_MINJI_EMAIL, passwordEncoder.encode(SAMPLE_PASSWORD), "민지마켓", "김민지", "010-1111-1111"));
        Client jun = clientRepository.save(
                Client.create(SAMPLE_JUN_EMAIL, passwordEncoder.encode(SAMPLE_PASSWORD), "준이상점", "박준호", "010-2222-2222"));

        List<Item> minjiItems = createMinjiItems(categories, minji, jun);
        List<Item> junItems = createJunItems(categories, jun, minji);
        List<Item> allItems = List.of(
                minjiItems.get(0), minjiItems.get(1), minjiItems.get(2), minjiItems.get(3), minjiItems.get(4),
                junItems.get(0), junItems.get(1), junItems.get(2), junItems.get(3), junItems.get(4));

        createItemImages(allItems);
        createAuctionStatuses(minjiItems, junItems, minji, jun);
        createLikes(minjiItems, junItems, minji, jun);
        createInquiries(minjiItems, junItems, minji, jun);
        createReviews(minjiItems, junItems, minji, jun);
        createChatRooms(minjiItems, junItems, minji, jun);

        log.info("Local sample data created. accounts: {} / {}, password={}",
                SAMPLE_MINJI_EMAIL, SAMPLE_JUN_EMAIL, SAMPLE_PASSWORD);
    }

    private List<Category> ensureCategories() {
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
                .sorted(Comparator.comparing(Category::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                .toList();
    }

    private List<Item> createMinjiItems(List<Category> categories, Client seller, Client buyer) {
        Item camera = item(categories, 1, seller, TradeType.DIRECT, "라이카 M10-P 블랙 바디",
                "생활 흠집이 거의 없는 카메라입니다. 박스와 스트랩을 함께 드립니다.",
                8_500_000L, ConditionType.USED, TradeStatus.ON_SALE, false);
        Item headphones = item(categories, 1, seller, TradeType.DIRECT, "소니 WH-1000XM5 실버",
                "6개월 사용한 노이즈 캔슬링 헤드폰입니다. 케이스 포함입니다.",
                320_000L, ConditionType.USED, TradeStatus.RESERVED, false);
        Item keyboard = item(categories, 1, seller, TradeType.DIRECT, "커스텀 기계식 키보드",
                "윤활 작업 완료한 조용한 키보드입니다. 사무실용으로 좋습니다.",
                280_000L, ConditionType.USED, TradeStatus.SOLD_OUT, false);
        keyboard.updateStatus(TradeStatus.SOLD_OUT, buyer);
        Item lamp = item(categories, 4, seller, TradeType.AUCTION, "아르떼미데 테이블 램프",
                "상태 좋은 테이블 램프입니다. 로컬 경매 테스트용 상품입니다.",
                185_000L, ConditionType.USED, TradeStatus.ON_SALE, false);
        Item grinder = item(categories, 4, seller, TradeType.DIRECT, "펠로우 오드 그라인더 Gen 2",
                "분쇄 균일도가 좋은 커피 그라인더입니다. 구성품 모두 있습니다.",
                350_000L, ConditionType.USED, TradeStatus.ON_SALE, false);

        return itemRepository.saveAll(List.of(camera, headphones, keyboard, lamp, grinder));
    }

    private List<Item> createJunItems(List<Category> categories, Client seller, Client buyer) {
        Item jacket = item(categories, 0, seller, TradeType.DIRECT, "빈티지 코튼 재킷",
                "봄가을에 입기 좋은 빈티지 재킷입니다. 오염 없이 깨끗합니다.",
                65_000L, ConditionType.USED, TradeStatus.ON_SALE, false);
        Item calculator = item(categories, 2, seller, TradeType.DIRECT, "브라운 계산기 ET66 복각판",
                "미개봉 계산기입니다. 책상 소품으로도 좋습니다.",
                45_000L, ConditionType.NEW, TradeStatus.SOLD_OUT, false);
        calculator.updateStatus(TradeStatus.SOLD_OUT, buyer);
        Item bicycle = item(categories, 3, seller, TradeType.DIRECT, "브롬톤 클래식 시티 바이크",
                "가벼운 출퇴근용 접이식 자전거입니다. 정비 완료했습니다.",
                1_200_000L, ConditionType.USED, TradeStatus.ON_SALE, false);
        Item console = item(categories, 5, seller, TradeType.AUCTION, "닌텐도 스위치 OLED 세트",
                "본체, 독, 조이콘, 게임 타이틀 2개 포함입니다. 경매 테스트용입니다.",
                210_000L, ConditionType.USED, TradeStatus.ON_SALE, false);
        Item bag = item(categories, 0, seller, TradeType.DIRECT, "포터 탱커 숄더백",
                "사용감 적은 숄더백입니다. 데일리 가방으로 좋습니다.",
                120_000L, ConditionType.USED, TradeStatus.RESERVED, false);

        return itemRepository.saveAll(List.of(jacket, calculator, bicycle, console, bag));
    }

    private Item item(
            List<Category> categories,
            int categoryIndex,
            Client seller,
            TradeType tradeType,
            String title,
            String description,
            Long initialPrice,
            ConditionType conditionType,
            TradeStatus tradeStatus,
            boolean isDraft
    ) {
        return Item.builder()
                .category(category(categories, categoryIndex))
                .seller(seller)
                .tradeType(tradeType)
                .title(title)
                .description(description)
                .initialPrice(initialPrice)
                .conditionType(conditionType)
                .tradeStatus(tradeStatus)
                .isDraft(isDraft)
                .build();
    }

    private Category category(List<Category> categories, int index) {
        if (categories.isEmpty()) {
            throw new IllegalStateException("Local sample categories are required.");
        }
        return categories.get(Math.min(index, categories.size() - 1));
    }

    private void createItemImages(List<Item> items) {
        String[] imageUrls = {
                "https://images.unsplash.com/photo-1512790182412-b19e6d62bc39?auto=format&fit=crop&w=720&q=80",
                "https://images.unsplash.com/photo-1546435770-a3e426bf472b?auto=format&fit=crop&w=720&q=80",
                "https://images.unsplash.com/photo-1618384887929-16ec33fab9ef?auto=format&fit=crop&w=720&q=80",
                "https://images.unsplash.com/photo-1507473885765-e6ed057f782c?auto=format&fit=crop&w=720&q=80",
                "https://images.unsplash.com/photo-1517668808822-9ebb02f2a0e6?auto=format&fit=crop&w=720&q=80",
                "https://images.unsplash.com/photo-1523398002811-999ca8dec234?auto=format&fit=crop&w=720&q=80",
                "https://images.unsplash.com/photo-1564473185935-58113cba1e80?auto=format&fit=crop&w=720&q=80",
                "https://images.unsplash.com/photo-1507035895480-2b3156c31fc8?auto=format&fit=crop&w=720&q=80",
                "https://images.unsplash.com/photo-1606144042614-b2417e99c4e3?auto=format&fit=crop&w=720&q=80",
                "https://images.unsplash.com/photo-1590874103328-eac38a683ce7?auto=format&fit=crop&w=720&q=80"
        };

        for (int index = 0; index < items.size(); index++) {
            itemImageRepository.save(ItemImage.builder()
                    .item(items.get(index))
                    .imageUrl(imageUrls[index])
                    .sortOrder(1)
                    .isThumbnail(true)
                    .build());
        }
    }

    private void createAuctionStatuses(List<Item> minjiItems, List<Item> junItems, Client minji, Client jun) {
        AuctionStatus minjiAuction = AuctionStatus.builder()
                .item(minjiItems.get(3))
                .currentBid(minjiItems.get(3).getInitialPrice())
                .closeDate(LocalDateTime.now().plusDays(7))
                .build();
        minjiAuction.updateBid(211_000L, jun.getId(), LocalDateTime.now());

        AuctionStatus junAuction = AuctionStatus.builder()
                .item(junItems.get(3))
                .currentBid(junItems.get(3).getInitialPrice())
                .closeDate(LocalDateTime.now().plusDays(5))
                .build();
        junAuction.updateBid(230_000L, minji.getId(), LocalDateTime.now());

        auctionStatusRepository.saveAll(List.of(minjiAuction, junAuction));
    }

    private void createLikes(List<Item> minjiItems, List<Item> junItems, Client minji, Client jun) {
        saveLike(jun, minjiItems.get(0));
        saveLike(jun, minjiItems.get(3));
        saveLike(jun, minjiItems.get(4));
        saveLike(minji, junItems.get(0));
        saveLike(minji, junItems.get(2));
        saveLike(minji, junItems.get(3));
    }

    private void saveLike(Client client, Item item) {
        item.incrementLikeCount();
        itemLikeRepository.save(ItemLike.builder()
                .client(client)
                .item(item)
                .build());
    }

    private void createInquiries(List<Item> minjiItems, List<Item> junItems, Client minji, Client jun) {
        InquiryLog cameraQuestion = inquiryLogRepository.save(InquiryLog.builder()
                .item(minjiItems.get(0))
                .author(jun)
                .title("직거래 가능 시간 문의")
                .description("오늘 저녁이나 주말 오전에 직거래 가능할까요?")
                .status("OPEN")
                .build());
        inquiryLogRepository.save(InquiryLog.builder()
                .item(minjiItems.get(0))
                .author(minji)
                .targetInquiry(cameraQuestion)
                .title("답변")
                .description("주말 오전 10시 이후로 가능합니다.")
                .status("ANSWERED")
                .build());

        InquiryLog jacketQuestion = inquiryLogRepository.save(InquiryLog.builder()
                .item(junItems.get(0))
                .author(minji)
                .title("사이즈 문의")
                .description("평소 95 사이즈를 입는데 잘 맞을까요?")
                .status("OPEN")
                .build());
        inquiryLogRepository.save(InquiryLog.builder()
                .item(junItems.get(0))
                .author(jun)
                .targetInquiry(jacketQuestion)
                .title("답변")
                .description("95에서 100 사이즈까지 편하게 맞습니다.")
                .status("ANSWERED")
                .build());
    }

    private void createReviews(List<Item> minjiItems, List<Item> junItems, Client minji, Client jun) {
        reviewRepository.saveAll(List.of(
                Review.builder()
                        .item(minjiItems.get(2))
                        .reviewer(jun)
                        .reviewee(minji)
                        .rating(5)
                        .content("상품 설명이 정확했고 약속 시간도 잘 지켜주셨습니다.")
                        .isDeleted(false)
                        .build(),
                Review.builder()
                        .item(minjiItems.get(1))
                        .reviewer(jun)
                        .reviewee(minji)
                        .rating(4)
                        .content("응답이 빠르고 포장이 꼼꼼했습니다.")
                        .isDeleted(false)
                        .build(),
                Review.builder()
                        .item(junItems.get(1))
                        .reviewer(minji)
                        .reviewee(jun)
                        .rating(5)
                        .content("새 상품 그대로였고 거래가 편했습니다.")
                        .isDeleted(false)
                        .build(),
                Review.builder()
                        .item(junItems.get(4))
                        .reviewer(minji)
                        .reviewee(jun)
                        .rating(4)
                        .content("사진과 같은 상태였고 설명이 친절했습니다.")
                        .isDeleted(false)
                        .build()
        ));
    }

    private void createChatRooms(List<Item> minjiItems, List<Item> junItems, Client minji, Client jun) {
        ChatRoom minjiItemRoom = chatRoomRepository.save(ChatRoom.builder()
                .item(minjiItems.get(0))
                .createdBy(jun)
                .build());
        chatMessageRepository.saveAll(List.of(
                message(minjiItemRoom, jun, "안녕하세요. 라이카 아직 구매 가능할까요?"),
                message(minjiItemRoom, minji, "네, 가능합니다. 구성품은 모두 보관 중입니다."),
                message(minjiItemRoom, jun, "그럼 주말 오전에 직접 보고 거래하고 싶습니다."),
                message(minjiItemRoom, minji, "좋습니다. 토요일 오전 10시에 가능합니다.")
        ));

        ChatRoom junItemRoom = chatRoomRepository.save(ChatRoom.builder()
                .item(junItems.get(0))
                .createdBy(minji)
                .build());
        chatMessageRepository.saveAll(List.of(
                message(junItemRoom, minji, "재킷 실측 사이즈를 알 수 있을까요?"),
                message(junItemRoom, jun, "가슴 56cm, 총장 68cm 정도입니다."),
                message(junItemRoom, minji, "확인 감사합니다. 오늘 저녁 거래 가능하세요?"),
                message(junItemRoom, jun, "네, 7시 이후 가능합니다.")
        ));
    }

    private ChatMessage message(ChatRoom chatRoom, Client sender, String content) {
        return ChatMessage.builder()
                .chatRoom(chatRoom)
                .sender(sender)
                .messageType(MessageType.TEXT)
                .content(content)
                .build();
    }
}
