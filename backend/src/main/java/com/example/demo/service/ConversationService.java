package com.example.demo.service;

import com.example.demo.common.BizAssert;
import com.example.demo.common.BusinessException;
import com.example.demo.common.ErrorCodes;
import com.example.demo.common.RequestValidator;
import com.example.demo.dto.ConversationDtos;
import com.example.demo.model.ForumEnums.NotificationEventType;
import com.example.demo.model.ForumEnums.NotificationTargetType;
import com.example.demo.model.ForumModels.ConversationMemberRecord;
import com.example.demo.repository.ConversationRepository;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final UserAccessService userAccessService;
    private final NotificationService notificationService;

    public ConversationService(ConversationRepository conversationRepository, UserAccessService userAccessService,
                               NotificationService notificationService) {
        this.conversationRepository = conversationRepository;
        this.userAccessService = userAccessService;
        this.notificationService = notificationService;
    }

    @Transactional
    public ConversationDtos.ConversationDetailView createOrReuseConversation(ConversationDtos.CreateConversationRequest request) {
        var currentUser = userAccessService.requireCurrentUser();
        userAccessService.assertCanOperate(currentUser);
        BizAssert.isTrue(!currentUser.id().equals(request.targetUserId()), ErrorCodes.BAD_REQUEST,
            "Cannot create a conversation with yourself");
        var targetUser = userAccessService.requireById(request.targetUserId());
        long user1Id = Math.min(currentUser.id(), targetUser.id());
        long user2Id = Math.max(currentUser.id(), targetUser.id());
        Long conversationId = conversationRepository.findByUserPair(user1Id, user2Id)
            .map(conversation -> {
                var currentMember = conversationRepository.lockMember(conversation.id(), currentUser.id())
                    .orElseThrow(() -> new BusinessException(ErrorCodes.SERVER_ERROR, "Conversation member missing"));
                if (currentMember.isDeleted()) {
                    conversationRepository.restoreConversationMember(conversation.id(), currentUser.id());
                }
                return conversation.id();
            })
            .orElseGet(() -> {
                Long newId = conversationRepository.createConversation(user1Id, user2Id);
                conversationRepository.createConversationMember(newId, user1Id);
                conversationRepository.createConversationMember(newId, user2Id);
                return newId;
            });
        return getConversationDetail(conversationId, 1, 20);
    }

    @Transactional
    public ConversationDtos.MessageView sendMessage(Long conversationId, ConversationDtos.SendMessageRequest request) {
        var currentUser = userAccessService.requireCurrentUser();
        userAccessService.assertCanSpeak(currentUser);
        RequestValidator.requireContentOrImages(request.contentText(), request.imageUrls(),
            "Message content and images cannot both be empty");
        var conversation = conversationRepository.lockById(conversationId)
            .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "Conversation not found"));
        requireActiveMember(conversationId, currentUser.id(), true);
        Long peerUserId = conversation.user1Id().equals(currentUser.id()) ? conversation.user2Id() : conversation.user1Id();
        conversationRepository.lockMember(conversationId, peerUserId)
            .orElseThrow(() -> new BusinessException(ErrorCodes.FORBIDDEN, "Conversation member missing"));
        int sequenceNo = conversationRepository.nextSequenceNo(conversationId);
        Long messageId = conversationRepository.createMessage(conversationId, currentUser.id(), sequenceNo, trimToNull(request.contentText()));
        conversationRepository.insertMessageImages(messageId, sanitizeImages(request.imageUrls()));
        LocalDateTime now = LocalDateTime.now();
        conversationRepository.updateConversationLastMessage(conversationId, messageId, now);
        conversationRepository.updateSenderMemberAfterSend(conversationId, currentUser.id(), messageId, sequenceNo, now);
        conversationRepository.updateRecipientMemberAfterSend(conversationId, peerUserId);
        notificationService.createNotification(peerUserId, currentUser.id(),
            NotificationEventType.PRIVATE_MESSAGE.value(), NotificationTargetType.CONVERSATION.value(), conversationId,
            conversationId, messageId, null, null, null);
        Map<Long, List<String>> imageMap = conversationRepository.listImagesByMessageIds(List.of(messageId));
        return new ConversationDtos.MessageView(messageId, conversationId, sequenceNo, currentUser.id(),
            trimToNull(request.contentText()), imageMap.getOrDefault(messageId, Collections.emptyList()), now);
    }

    public com.example.demo.common.PageResult<ConversationDtos.ConversationListView> pageConversations(long page, long pageSize) {
        var currentUser = userAccessService.requireCurrentUser();
        var rows = conversationRepository.pageConversations(currentUser.id(), page, pageSize);
        List<Long> messageIds = rows.list().stream()
            .map(ConversationRepository.ConversationSummaryRow::lastMessageId)
            .filter(java.util.Objects::nonNull)
            .toList();
        Map<Long, List<String>> imageMap = conversationRepository.listImagesByMessageIds(messageIds);
        return new com.example.demo.common.PageResult<>(
            rows.total(),
            rows.page(),
            rows.pageSize(),
            rows.list().stream().map(row -> new ConversationDtos.ConversationListView(
                row.id(),
                ViewMapper.toSimpleView(row.peerUserId(), row.peerUsername(), row.peerNickname(), row.peerAvatarUrl(),
                    row.peerBio(), row.peerRole(), row.peerStatus(), row.peerLevel()),
                row.lastMessageId() == null ? null : new ConversationDtos.MessageView(
                    row.lastMessageId(),
                    row.id(),
                    row.lastSequenceNo(),
                    row.lastSenderUserId(),
                    row.lastContentText(),
                    imageMap.getOrDefault(row.lastMessageId(), Collections.emptyList()),
                    row.lastMessageCreatedAt()
                ),
                row.unreadCount(),
                row.pinned(),
                row.muted(),
                row.updatedAt()
            )).toList()
        );
    }

    public ConversationDtos.ConversationDetailView getConversationDetail(Long conversationId, long page, long pageSize) {
        var currentUser = userAccessService.requireCurrentUser();
        var conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "Conversation not found"));
        var member = requireActiveMember(conversationId, currentUser.id(), false);
        Long peerUserId = conversation.user1Id().equals(currentUser.id()) ? conversation.user2Id() : conversation.user1Id();
        var peerUser = userAccessService.requireById(peerUserId);
        var messagePage = conversationRepository.pageMessages(conversationId, page, pageSize);
        List<ConversationRepository.MessageRow> orderedRows = new java.util.ArrayList<>(messagePage.list());
        Collections.reverse(orderedRows);
        Map<Long, List<String>> imageMap = conversationRepository.listImagesByMessageIds(orderedRows.stream().map(ConversationRepository.MessageRow::id).toList());
        List<ConversationDtos.MessageView> messages = orderedRows.stream()
            .map(row -> ViewMapper.toMessageView(row, imageMap.getOrDefault(row.id(), Collections.emptyList())))
            .toList();
        return new ConversationDtos.ConversationDetailView(
            conversationId,
            ViewMapper.toSimpleView(peerUser),
            member.unreadCount(),
            member.isPinned(),
            member.isMuted(),
            member.lastReadMessageId(),
            member.lastReadSequenceNo(),
            member.lastReadAt(),
            messages
        );
    }

    @Transactional
    public void markRead(Long conversationId) {
        var currentUser = userAccessService.requireCurrentUser();
        userAccessService.assertCanOperate(currentUser);
        var member = requireActiveMember(conversationId, currentUser.id(), true);
        if (member.unreadCount() == 0) {
            return;
        }
        var conversation = conversationRepository.lockById(conversationId)
            .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "Conversation not found"));
        if (conversation.lastMessageId() == null) {
            return;
        }
        var lastMessage = conversationRepository.findByMessageId(conversation.lastMessageId())
            .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "Message not found"));
        conversationRepository.markRead(conversationId, currentUser.id(), lastMessage.id(), lastMessage.sequenceNo(), LocalDateTime.now());
    }

    @Transactional
    public void updatePinned(Long conversationId, boolean pinned) {
        var currentUser = userAccessService.requireCurrentUser();
        userAccessService.assertCanOperate(currentUser);
        requireActiveMember(conversationId, currentUser.id(), false);
        conversationRepository.updatePinned(conversationId, currentUser.id(), pinned, LocalDateTime.now());
    }

    @Transactional
    public void updateMuted(Long conversationId, boolean muted) {
        var currentUser = userAccessService.requireCurrentUser();
        userAccessService.assertCanOperate(currentUser);
        requireActiveMember(conversationId, currentUser.id(), false);
        conversationRepository.updateMuted(conversationId, currentUser.id(), muted);
    }

    @Transactional
    public void deleteConversation(Long conversationId) {
        var currentUser = userAccessService.requireCurrentUser();
        userAccessService.assertCanOperate(currentUser);
        requireActiveMember(conversationId, currentUser.id(), false);
        conversationRepository.softDeleteConversation(conversationId, currentUser.id(), LocalDateTime.now());
    }

    private ConversationMemberRecord requireActiveMember(Long conversationId, Long userId, boolean lock) {
        var member = (lock ? conversationRepository.lockMember(conversationId, userId)
            : conversationRepository.findMember(conversationId, userId))
            .orElseThrow(() -> new BusinessException(ErrorCodes.FORBIDDEN, "Conversation access denied"));
        BizAssert.isTrue(!member.isDeleted(), ErrorCodes.NOT_FOUND, "Conversation not found");
        return member;
    }

    private List<String> sanitizeImages(List<String> imageUrls) {
        if (imageUrls == null) {
            return List.of();
        }
        return imageUrls.stream().filter(url -> url != null && !url.isBlank()).map(String::trim).toList();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
