package com.example.demo.service;

import com.example.demo.common.BusinessException;
import com.example.demo.common.ErrorCodes;
import com.example.demo.dto.NotificationDtos;
import com.example.demo.repository.NotificationRepository;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserAccessService userAccessService;

    public NotificationService(NotificationRepository notificationRepository, UserAccessService userAccessService) {
        this.notificationRepository = notificationRepository;
        this.userAccessService = userAccessService;
    }

    @Transactional
    public void createNotification(Long recipientUserId, Long actorUserId, String eventType, String targetType, Long targetId,
                                   Long conversationId, Long messageId, Long postId, Long commentId, Long rewardId) {
        if (recipientUserId == null || actorUserId == null || recipientUserId.equals(actorUserId)) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        var group = notificationRepository.lockGroup(recipientUserId, eventType, targetType, targetId).orElse(null);
        Long groupId;
        if (group == null) {
            groupId = notificationRepository.createGroup(recipientUserId, eventType, targetType, targetId, actorUserId, now);
        } else {
            groupId = group.id();
            notificationRepository.incrementGroup(groupId, actorUserId, now);
        }
        notificationRepository.createItem(groupId, recipientUserId, actorUserId, eventType, targetType, targetId,
            conversationId, messageId, postId, commentId, rewardId);
    }

    public com.example.demo.common.PageResult<NotificationDtos.NotificationGroupView> pageGroups(long page, long pageSize) {
        var currentUser = userAccessService.requireCurrentUser();
        var rows = notificationRepository.pageGroups(currentUser.id(), page, pageSize);
        return new com.example.demo.common.PageResult<>(
            rows.total(),
            rows.page(),
            rows.pageSize(),
            rows.list().stream().map(ViewMapper::toNotificationGroupView).toList()
        );
    }

    public com.example.demo.common.PageResult<NotificationDtos.NotificationItemView> pageGroupItems(Long groupId, long page, long pageSize) {
        var currentUser = userAccessService.requireCurrentUser();
        notificationRepository.lockGroupById(groupId, currentUser.id())
            .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "通知分组不存在"));
        var rows = notificationRepository.pageItems(groupId, currentUser.id(), page, pageSize);
        return new com.example.demo.common.PageResult<>(
            rows.total(),
            rows.page(),
            rows.pageSize(),
            rows.list().stream().map(ViewMapper::toNotificationItemView).toList()
        );
    }

    @Transactional
    public void markItemRead(Long itemId) {
        var currentUser = userAccessService.requireCurrentUser();
        userAccessService.assertCanOperate(currentUser);
        var item = notificationRepository.lockItem(itemId, currentUser.id())
            .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "通知不存在"));
        if (item.isRead()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        notificationRepository.markItemRead(itemId, now);
        notificationRepository.decrementGroupUnread(item.groupId(), 1, now);
    }

    @Transactional
    public void markGroupRead(Long groupId) {
        var currentUser = userAccessService.requireCurrentUser();
        userAccessService.assertCanOperate(currentUser);
        var group = notificationRepository.lockGroupById(groupId, currentUser.id())
            .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "通知分组不存在"));
        if (group.unreadCount() <= 0) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        notificationRepository.markGroupItemsRead(groupId, currentUser.id(), now);
        notificationRepository.markGroupRead(groupId, now);
    }

    @Transactional
    public void markAllRead() {
        var currentUser = userAccessService.requireCurrentUser();
        userAccessService.assertCanOperate(currentUser);
        LocalDateTime now = LocalDateTime.now();
        notificationRepository.markAllItemsRead(currentUser.id(), now);
        notificationRepository.markAllGroupsRead(currentUser.id(), now);
    }

    public NotificationDtos.NotificationUnreadView getUnreadCount() {
        var currentUser = userAccessService.requireCurrentUser();
        return new NotificationDtos.NotificationUnreadView(notificationRepository.sumUnreadCount(currentUser.id()));
    }
}
