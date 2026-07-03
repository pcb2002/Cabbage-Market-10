package com.example.cabbagemarket10.domain.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.cabbagemarket10.domain.category.entity.Category;
import com.example.cabbagemarket10.domain.chat.dto.restful.ChatRoomDetail;
import com.example.cabbagemarket10.domain.chat.dto.restful.RoomCreate;
import com.example.cabbagemarket10.domain.chat.dto.websocket.ChatMessageDto;
import com.example.cabbagemarket10.domain.chat.dto.websocket.ChatMessageList;
import com.example.cabbagemarket10.domain.chat.entity.ChatMessage;
import com.example.cabbagemarket10.domain.chat.entity.ChatRoom;
import com.example.cabbagemarket10.domain.chat.entity.MessageType;
import com.example.cabbagemarket10.domain.chat.repository.ChatMessageRepository;
import com.example.cabbagemarket10.domain.chat.repository.ChatRoomRepository;
import com.example.cabbagemarket10.domain.client.entity.Client;
import com.example.cabbagemarket10.domain.client.repository.ClientRepository;
import com.example.cabbagemarket10.domain.item.entity.Item;
import com.example.cabbagemarket10.domain.item.enums.ConditionType;
import com.example.cabbagemarket10.domain.item.enums.TradeStatus;
import com.example.cabbagemarket10.domain.item.enums.TradeType;
import com.example.cabbagemarket10.domain.item.repository.ItemRepository;
import com.example.cabbagemarket10.global.common.response.PageResponse;
import com.example.cabbagemarket10.global.exception.BusinessException;
import com.example.cabbagemarket10.global.exception.ErrorCode;
import com.example.cabbagemarket10.global.security.jwt.AuthenticatedClient;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @InjectMocks
    private ChatService chatService;

    @DisplayName("회원과 상품이 존재하면 채팅방을 생성한다")
    @Test
    void 회원과_상품이_존재하면_채팅방을_생성한다() {
        Client buyer = client(1L, "buyer@example.com", "구매자", "김구매");
        Item item = item(client(2L, "seller@example.com", "판매자", "김판매"));

        given(clientRepository.findById(1L)).willReturn(Optional.of(buyer));
        given(itemRepository.findById(10L)).willReturn(Optional.of(item));
        given(chatRoomRepository.save(any(ChatRoom.class))).willAnswer(invocation -> {
            ChatRoom chatRoom = invocation.getArgument(0);
            ReflectionTestUtils.setField(chatRoom, "id", "room-1");
            return chatRoom;
        });

        RoomCreate response = chatService.createRoom(1L, 10L);

        ArgumentCaptor<ChatRoom> captor = ArgumentCaptor.forClass(ChatRoom.class);
        verify(chatRoomRepository).save(captor.capture());
        assertThat(captor.getValue().getCreatedBy()).isSameAs(buyer);
        assertThat(captor.getValue().getItem()).isSameAs(item);
        assertThat(response.roomId()).isEqualTo("room-1");
    }

    @DisplayName("메시지 전송 시 인증 회원을 발신자로 저장한다")
    @Test
    void 메시지_전송_시_인증_회원을_발신자로_저장한다() {
        Client sender = client(1L, "sender@example.com", "발신자", "김발신");
        ChatRoom chatRoom = chatRoom("room-1", sender);
        ChatMessageDto request = new ChatMessageDto("안녕하세요", MessageType.TEXT);
        AuthenticatedClient principal = new AuthenticatedClient(1L, "sender@example.com");

        given(clientRepository.findById(1L)).willReturn(Optional.of(sender));
        given(chatRoomRepository.findById("room-1")).willReturn(Optional.of(chatRoom));

        chatService.sendMessage("room-1", request, principal);

        ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository).save(captor.capture());
        ChatMessage savedMessage = captor.getValue();
        assertThat(savedMessage.getChatRoom()).isSameAs(chatRoom);
        assertThat(savedMessage.getSender()).isSameAs(sender);
        assertThat(savedMessage.getContent()).isEqualTo("안녕하세요");
        assertThat(savedMessage.getMessageType()).isEqualTo(MessageType.TEXT);
    }

    @DisplayName("메시지 전송 시 채팅방이 없으면 CHAT_ROOM_NOT_FOUND 예외가 발생한다")
    @Test
    void 메시지_전송_시_채팅방이_없으면_CHAT_ROOM_NOT_FOUND_예외가_발생한다() {
        Client sender = client(1L, "sender@example.com", "발신자", "김발신");
        AuthenticatedClient principal = new AuthenticatedClient(1L, "sender@example.com");
        given(clientRepository.findById(1L)).willReturn(Optional.of(sender));
        given(chatRoomRepository.findById("missing-room")).willReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.sendMessage(
                "missing-room",
                new ChatMessageDto("안녕하세요", MessageType.TEXT),
                principal))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CHAT_ROOM_NOT_FOUND));

        verify(chatMessageRepository, never()).save(any());
    }

    @DisplayName("메시지 작성자는 메시지를 삭제할 수 있다")
    @Test
    void 메시지_작성자는_메시지를_삭제할_수_있다() {
        Client sender = client(1L, "sender@example.com", "작성자", "김작성");
        ChatMessage chatMessage = chatMessage(sender);
        given(chatMessageRepository.findById(100L)).willReturn(Optional.of(chatMessage));

        chatService.deleteMessage(100L, 1L);

        verify(chatMessageRepository).delete(chatMessage);
    }

    @DisplayName("메시지 작성자가 아니면 메시지 삭제 시 CHAT_MESSAGE_NOT_PUBLISHER 예외가 발생한다")
    @Test
    void 메시지_작성자가_아니면_메시지_삭제_시_CHAT_MESSAGE_NOT_PUBLISHER_예외가_발생한다() {
        Client sender = client(1L, "sender@example.com", "작성자", "김작성");
        ChatMessage chatMessage = chatMessage(sender);
        given(chatMessageRepository.findById(100L)).willReturn(Optional.of(chatMessage));

        assertThatThrownBy(() -> chatService.deleteMessage(100L, 2L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CHAT_MESSAGE_NOT_PUBLISHER));

        verify(chatMessageRepository, never()).delete(any());
    }

    @DisplayName("메시지 삭제 시 메시지가 없으면 CHAT_MESSAGE_NOT_FOUND 예외가 발생한다")
    @Test
    void 메시지_삭제_시_메시지가_없으면_CHAT_MESSAGE_NOT_FOUND_예외가_발생한다() {
        given(chatMessageRepository.findById(100L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.deleteMessage(100L, 1L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CHAT_MESSAGE_NOT_FOUND));

        verify(chatMessageRepository, never()).delete(any());
    }

    @DisplayName("채팅방 참여자는 최신 메시지 50개를 최신순으로 조회한다")
    @Test
    void 채팅방_참여자는_최신_메시지_50개를_최신순으로_조회한다() {
        Client buyer = client(1L, "buyer@example.com", "구매자", "김구매");
        Client seller = client(2L, "seller@example.com", "판매자", "김판매");
        ChatRoom chatRoom = chatRoom("room-1", buyer, seller);
        ChatMessage oldest = chatMessage(
                1L,
                chatRoom,
                buyer,
                "첫 번째 메시지",
                LocalDateTime.of(2026, 7, 1, 10, 0));
        ChatMessage middle = chatMessage(
                2L,
                chatRoom,
                seller,
                "두 번째 메시지",
                LocalDateTime.of(2026, 7, 1, 10, 1));
        ChatMessage newest = chatMessage(
                3L,
                chatRoom,
                buyer,
                "세 번째 메시지",
                LocalDateTime.of(2026, 7, 1, 10, 2));
        Pageable requestPageable = PageRequest.of(0, 50);
        Pageable repositoryPageable = PageRequest.of(
                0,
                50,
                Sort.by("createdAt").descending());

        given(chatRoomRepository.findById("room-1")).willReturn(Optional.of(chatRoom));
        given(chatMessageRepository.findByChatRoomId(eq("room-1"), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(newest, middle, oldest), repositoryPageable, 60));

        ChatMessageList response = chatService.getRecentMessages("room-1", seller.getId(), requestPageable);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(chatMessageRepository).findByChatRoomId(eq("room-1"), pageableCaptor.capture());
        Pageable usedPageable = pageableCaptor.getValue();
        assertThat(usedPageable.getPageNumber()).isZero();
        assertThat(usedPageable.getPageSize()).isEqualTo(50);
        assertThat(usedPageable.getSort().getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(usedPageable.getSort().getOrderFor("id")).isNull();
        assertThat(response.content())
                .extracting("messageId")
                .containsExactly(3L, 2L, 1L);
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(50);
        assertThat(response.totalElements()).isEqualTo(60);
        assertThat(response.totalPages()).isEqualTo(2);
    }

    @DisplayName("채팅방이 없으면 메시지 목록 조회 시 CHAT_ROOM_NOT_FOUND 예외가 발생한다")
    @Test
    void 채팅방이_없으면_메시지_목록_조회_시_CHAT_ROOM_NOT_FOUND_예외가_발생한다() {
        Pageable pageable = PageRequest.of(0, 50);
        given(chatRoomRepository.findById("missing-room")).willReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.getRecentMessages("missing-room", 1L, pageable))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CHAT_ROOM_NOT_FOUND));

        verify(chatMessageRepository, never()).findByChatRoomId(any(), any());
    }

    @DisplayName("채팅방 참여자가 아니면 메시지 목록 조회 시 CLIENT_NOT_PARTICIPANT 예외가 발생한다")
    @Test
    void 채팅방_참여자가_아니면_메시지_목록_조회_시_CLIENT_NOT_PARTICIPANT_예외가_발생한다() {
        Client buyer = client(1L, "buyer@example.com", "구매자", "김구매");
        Client seller = client(2L, "seller@example.com", "판매자", "김판매");
        ChatRoom chatRoom = chatRoom("room-1", buyer, seller);
        Pageable pageable = PageRequest.of(0, 50);
        given(chatRoomRepository.findById("room-1")).willReturn(Optional.of(chatRoom));

        assertThatThrownBy(() -> chatService.getRecentMessages("room-1", 3L, pageable))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CLIENT_NOT_PARTICIPANT));

        verify(chatMessageRepository, never()).findByChatRoomId(any(), any());
    }

    @DisplayName("내 채팅방 목록은 마지막 메시지 시각 기준 최신순으로 조회한다")
    @Test
    void 내_채팅방_목록은_마지막_메시지_시각_기준_최신순으로_조회한다() {
        Pageable requestPageable = PageRequest.of(1, 20);
        Pageable repositoryPageable = PageRequest.of(
                1,
                20,
                Sort.by("lastMessageAt").descending());
        ChatRoomDetail firstRoom = new ChatRoomDetail(
                "room-2",
                200L,
                "자전거",
                LocalDateTime.of(2026, 7, 1, 11, 0));
        ChatRoomDetail secondRoom = new ChatRoomDetail(
                "room-1",
                100L,
                "노트북",
                LocalDateTime.of(2026, 7, 1, 10, 0));

        given(chatRoomRepository.findByClientId(eq(1L), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(firstRoom, secondRoom), repositoryPageable, 22));

        PageResponse<ChatRoomDetail> response = chatService.getMyChatRoom(1L, requestPageable);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(chatRoomRepository).findByClientId(eq(1L), pageableCaptor.capture());
        Pageable usedPageable = pageableCaptor.getValue();
        assertThat(usedPageable.getPageNumber()).isEqualTo(1);
        assertThat(usedPageable.getPageSize()).isEqualTo(20);
        assertThat(usedPageable.getSort().getOrderFor("lastMessageAt").getDirection())
                .isEqualTo(Sort.Direction.DESC);
        assertThat(response.getContent())
                .extracting(ChatRoomDetail::id)
                .containsExactly("room-2", "room-1");
        assertThat(response.getPage()).isEqualTo(1);
        assertThat(response.getSize()).isEqualTo(20);
        assertThat(response.getTotalElements()).isEqualTo(22);
        assertThat(response.getTotalPages()).isEqualTo(2);
    }

    private ChatMessage chatMessage(Client sender) {
        return ChatMessage.builder()
                .chatRoom(chatRoom("room-1", sender))
                .sender(sender)
                .messageType(MessageType.TEXT)
                .content("삭제할 메시지")
                .build();
    }

    private ChatMessage chatMessage(
            Long id,
            ChatRoom chatRoom,
            Client sender,
            String content,
            LocalDateTime createdAt
    ) {
        ChatMessage chatMessage = ChatMessage.builder()
                .chatRoom(chatRoom)
                .sender(sender)
                .messageType(MessageType.TEXT)
                .content(content)
                .build();
        ReflectionTestUtils.setField(chatMessage, "id", id);
        ReflectionTestUtils.setField(chatMessage, "createdAt", createdAt);
        return chatMessage;
    }

    private ChatRoom chatRoom(String id, Client creator) {
        return chatRoom(id, creator, creator);
    }

    private ChatRoom chatRoom(String id, Client creator, Client seller) {
        ChatRoom chatRoom = ChatRoom.builder()
                .item(item(seller))
                .createdBy(creator)
                .build();
        ReflectionTestUtils.setField(chatRoom, "id", id);
        return chatRoom;
    }

    private Item item(Client seller) {
        return Item.builder()
                .category(Category.builder()
                        .name("디지털/가전")
                        .sortOrder(1)
                        .isActive(true)
                        .build())
                .seller(seller)
                .tradeType(TradeType.DIRECT)
                .title("중고 노트북")
                .description("상태 좋은 노트북입니다.")
                .initialPrice(100_000L)
                .conditionType(ConditionType.USED)
                .tradeStatus(TradeStatus.ON_SALE)
                .isDraft(false)
                .build();
    }

    private Client client(Long id, String email, String nickname, String name) {
        Client client = Client.create(
                email,
                "encodedPassword",
                nickname,
                name,
                "010-1234-5678");
        ReflectionTestUtils.setField(client, "id", id);
        return client;
    }
}
