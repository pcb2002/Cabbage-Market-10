package com.example.cabbagemarket10.application.facade;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.cabbagemarket10.domain.auction.facade.AuctionFacade;
import com.example.cabbagemarket10.domain.auction.repository.AuctionBidHistoryRepository;
import com.example.cabbagemarket10.domain.auction.entity.AuctionStatus;
import com.example.cabbagemarket10.domain.auction.repository.AuctionStatusRepository;
import com.example.cabbagemarket10.domain.category.entity.Category;
import com.example.cabbagemarket10.domain.category.repository.CategoryRepository;
import com.example.cabbagemarket10.domain.client.entity.Client;
import com.example.cabbagemarket10.domain.client.repository.ClientRepository;
import com.example.cabbagemarket10.domain.item.entity.Item;
import com.example.cabbagemarket10.domain.item.enums.ConditionType;
import com.example.cabbagemarket10.domain.item.enums.TradeStatus;
import com.example.cabbagemarket10.domain.item.enums.TradeType;
import com.example.cabbagemarket10.domain.item.repository.ItemRepository;
import com.example.cabbagemarket10.global.exception.BusinessException;
import com.example.cabbagemarket10.global.exception.ErrorCode;
import java.io.IOException;
import java.net.ServerSocket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import redis.embedded.RedisServer;

@SpringBootTest
@ActiveProfiles("redis-test")
class AuctionFacadeConcurrencyIntegrationTest {

    private static final int REDIS_PORT = findAvailablePort();
    private static RedisServer redisServer;

    @Autowired
    private AuctionFacade auctionFacade;

    @Autowired
    private AuctionStatusRepository auctionStatusRepository;

    @Autowired
    private AuctionBidHistoryRepository auctionBidHistoryRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ClientRepository clientRepository;

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) throws IOException {
        startEmbeddedRedis();
        registry.add("spring.data.redis.host", () -> "127.0.0.1");
        registry.add("spring.data.redis.port", () -> REDIS_PORT);
        registry.add("spring.data.redis.password", () -> "");
        registry.add("app.auction.redis-lock.enabled", () -> true);
    }

    @AfterAll
    static void stopEmbeddedRedis() throws IOException {
        if (redisServer != null && redisServer.isActive()) {
            redisServer.stop();
        }
    }

    @BeforeEach
    void setUp() {
        auctionBidHistoryRepository.deleteAllInBatch();
        auctionStatusRepository.deleteAllInBatch();
        itemRepository.deleteAllInBatch();
        categoryRepository.deleteAllInBatch();
        clientRepository.deleteAllInBatch();
    }

    @DisplayName("같은 상품에 동시 입찰해도 Redis 락으로 최고 입찰가와 최고 입찰자를 일관되게 갱신한다")
    @Test
    void concurrentBidsAreSerializedByRedisLock() throws Exception {
        int threadCount = 8;
        Client seller = clientRepository.save(client("seller"));
        List<Client> bidders = saveBidders(threadCount);
        Item item = itemRepository.saveAndFlush(item(seller));
        auctionStatusRepository.saveAndFlush(auctionStatus(item, 10_000L));

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger();
        List<Future<Throwable>> futures = new ArrayList<>();

        for (int index = 0; index < threadCount; index++) {
            Client bidder = bidders.get(index);
            long bidPrice = 11_000L + (index * 1_000L);
            futures.add(executorService.submit(() -> bidAtSameTime(item.getId(), bidder.getId(), bidPrice,
                    readyLatch, startLatch, successCount)));
        }

        assertThat(readyLatch.await(3, TimeUnit.SECONDS)).isTrue();
        startLatch.countDown();

        List<Throwable> unexpectedFailures = new ArrayList<>();
        for (Future<Throwable> future : futures) {
            Throwable throwable = future.get(10, TimeUnit.SECONDS);
            if (throwable != null && !isExpectedLowerBidFailure(throwable)) {
                unexpectedFailures.add(throwable);
            }
        }
        executorService.shutdown();

        AuctionStatus result = auctionStatusRepository.findById(item.getId()).orElseThrow();
        Client highestBidder = bidders.get(threadCount - 1);

        assertThat(unexpectedFailures).isEmpty();
        assertThat(successCount.get()).isGreaterThanOrEqualTo(1);
        assertThat(result.getCurrentBid()).isEqualTo(18_000L);
        assertThat(result.getCurrentBidderId()).isEqualTo(highestBidder.getId());
    }

    private Throwable bidAtSameTime(
            Long itemId,
            Long bidderId,
            Long bidPrice,
            CountDownLatch readyLatch,
            CountDownLatch startLatch,
            AtomicInteger successCount
    ) {
        readyLatch.countDown();
        try {
            startLatch.await();
            auctionFacade.bidItem(itemId, bidderId, bidPrice);
            successCount.incrementAndGet();
            return null;
        } catch (Throwable throwable) {
            return throwable;
        }
    }

    private boolean isExpectedLowerBidFailure(Throwable throwable) {
        return throwable instanceof BusinessException businessException
                && businessException.getErrorCode() == ErrorCode.INVALID_BID_PRICE;
    }

    private List<Client> saveBidders(int count) {
        List<Client> bidders = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            bidders.add(clientRepository.save(client("bidder" + index)));
        }
        return bidders;
    }

    private Client client(String prefix) {
        String suffix = prefix + "-" + System.nanoTime();
        String displayName = prefix.length() > 20 ? prefix.substring(0, 20) : prefix;
        return Client.create(
                suffix + "@example.com",
                "encodedPassword",
                displayName,
                displayName,
                "010-1234-5678");
    }

    private Item item(Client seller) {
        Category category = categoryRepository.save(Category.builder()
                .name("auction-category-" + System.nanoTime())
                .sortOrder(1)
                .isActive(true)
                .build());

        return Item.builder()
                .category(category)
                .seller(seller)
                .tradeType(TradeType.AUCTION)
                .title("auction item")
                .description("auction description")
                .initialPrice(10_000L)
                .conditionType(ConditionType.USED)
                .tradeStatus(TradeStatus.ON_SALE)
                .isDraft(false)
                .build();
    }

    private AuctionStatus auctionStatus(Item item, Long currentBid) {
        return AuctionStatus.builder()
                .item(item)
                .currentBid(currentBid)
                .closeDate(LocalDateTime.now().plusDays(1))
                .build();
    }

    private static void startEmbeddedRedis() throws IOException {
        if (redisServer != null && redisServer.isActive()) {
            return;
        }
        redisServer = RedisServer.newRedisServer()
                .bind("127.0.0.1")
                .port(REDIS_PORT)
                .setting("save \"\"")
                .setting("appendonly no")
                .build();
        redisServer.start();
    }

    private static int findAvailablePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException exception) {
            throw new IllegalStateException("embedded Redis port를 할당할 수 없습니다.", exception);
        }
    }
}
