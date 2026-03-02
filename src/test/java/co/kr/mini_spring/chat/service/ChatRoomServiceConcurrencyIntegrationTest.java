package co.kr.mini_spring.chat.service;

import co.kr.mini_spring.chat.cache.ChatRoomAccessCacheService;
import co.kr.mini_spring.chat.domain.Conversation;
import co.kr.mini_spring.chat.domain.ConversationParticipant;
import co.kr.mini_spring.chat.domain.ConversationType;
import co.kr.mini_spring.chat.domain.repository.ChatPermissionQueryRepository;
import co.kr.mini_spring.chat.domain.repository.ChatRoomQueryRepository;
import co.kr.mini_spring.chat.domain.repository.ConversationParticipantRepository;
import co.kr.mini_spring.chat.domain.repository.ConversationRepository;
import co.kr.mini_spring.global.common.exception.BusinessException;
import co.kr.mini_spring.global.common.response.ResponseCode;
import co.kr.mini_spring.global.config.QuerydslConfig;
import co.kr.mini_spring.member.domain.MemberProvider;
import co.kr.mini_spring.member.domain.SocialMember;
import co.kr.mini_spring.member.domain.repository.SocialMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import({
        QuerydslConfig.class,
        ChatRoomQueryRepository.class,
        ChatPermissionQueryRepository.class,
        ChatRoomService.class
})
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ChatRoomServiceConcurrencyIntegrationTest {

    @Autowired
    private ChatRoomService chatRoomService;
    @Autowired
    private SocialMemberRepository socialMemberRepository;
    @Autowired
    private ConversationRepository conversationRepository;
    @Autowired
    private ConversationParticipantRepository conversationParticipantRepository;
    @MockBean
    private ChatRoomAccessCacheService chatRoomAccessCacheService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(chatRoomService, "maxGroupMembers", 2);
    }

    @Test
    void joinGroupRoom_동시요청시_정원을_초과하지_않는다() throws InterruptedException {
        SocialMember owner = saveMember("owner@test.com", "owner");
        SocialMember userA = saveMember("a@test.com", "userA");
        SocialMember userB = saveMember("b@test.com", "userB");

        Conversation room = conversationRepository.saveAndFlush(
                Conversation.builder()
                        .type(ConversationType.GROUP)
                        .ownerId(owner.getId())
                        .title("group-room")
                        .build()
        );
        conversationParticipantRepository.saveAndFlush(
                ConversationParticipant.builder()
                        .conversationId(room.getId())
                        .userId(owner.getId())
                        .build()
        );

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger roomFullCount = new AtomicInteger();
        List<Throwable> unexpected = Collections.synchronizedList(new ArrayList<>());

        submitJoinTask(pool, startLatch, doneLatch, room.getId(), userA.getId(), successCount, roomFullCount, unexpected);
        submitJoinTask(pool, startLatch, doneLatch, room.getId(), userB.getId(), successCount, roomFullCount, unexpected);

        startLatch.countDown();
        boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);

        assertThat(completed).isTrue();
        assertThat(unexpected).isEmpty();
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(roomFullCount.get()).isEqualTo(1);
        assertThat(conversationParticipantRepository.countByConversationIdAndDeletedAtIsNull(room.getId()))
                .isEqualTo(2); // owner 1 + 성공한 참여자 1
    }

    private void submitJoinTask(ExecutorService pool,
                                CountDownLatch startLatch,
                                CountDownLatch doneLatch,
                                Long roomId,
                                Long userId,
                                AtomicInteger successCount,
                                AtomicInteger roomFullCount,
                                List<Throwable> unexpected) {
        pool.submit(() -> {
            try {
                startLatch.await();
                chatRoomService.joinGroupRoom(roomId, userId);
                successCount.incrementAndGet();
            } catch (BusinessException e) {
                if (e.getResponseCode() == ResponseCode.CHAT_ROOM_FULL) {
                    roomFullCount.incrementAndGet();
                } else {
                    unexpected.add(e);
                }
            } catch (Throwable t) {
                unexpected.add(t);
            } finally {
                doneLatch.countDown();
            }
        });
    }

    private SocialMember saveMember(String email, String nickname) {
        return socialMemberRepository.saveAndFlush(
                SocialMember.builder()
                        .email(email)
                        .provider(MemberProvider.GOOGLE)
                        .oauthId(UUID.randomUUID().toString())
                        .name(nickname)
                        .nickname(nickname)
                        .build()
        );
    }
}
