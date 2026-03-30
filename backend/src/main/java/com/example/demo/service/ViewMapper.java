package com.example.demo.service;

import com.example.demo.dto.CoinDtos;
import com.example.demo.dto.ConversationDtos;
import com.example.demo.dto.FollowDtos;
import com.example.demo.dto.NotificationDtos;
import com.example.demo.dto.PostDtos;
import com.example.demo.dto.UserDtos;
import com.example.demo.model.ForumModels.UserAccount;
import com.example.demo.repository.CoinRepository;
import com.example.demo.repository.ConversationRepository;
import com.example.demo.repository.NotificationRepository;
import com.example.demo.repository.PostRepository;
import java.util.List;

public final class ViewMapper {

    private ViewMapper() {
    }

    public static UserDtos.UserProfileView toProfileView(UserAccount user) {
        return new UserDtos.UserProfileView(
            user.id(),
            user.username(),
            user.nickname(),
            user.avatarUrl(),
            user.bio(),
            user.role(),
            user.status(),
            user.bannedUntilAt(),
            user.banReason(),
            user.mutedUntilAt(),
            user.muteReason(),
            user.coinBalance(),
            user.level(),
            user.totalExp(),
            user.currentLevelExp(),
            user.createdAt(),
            user.lastLoginAt()
        );
    }

    public static UserDtos.UserSimpleView toSimpleView(UserAccount user) {
        return new UserDtos.UserSimpleView(
            user.id(),
            user.username(),
            user.nickname(),
            user.avatarUrl(),
            user.bio(),
            user.role(),
            user.status(),
            user.level()
        );
    }

    public static UserDtos.UserSimpleView toSimpleView(Long id, String username, String nickname, String avatarUrl,
                                                       String bio, String role, String status, Integer level) {
        if (id == null) {
            return null;
        }
        return new UserDtos.UserSimpleView(id, username, nickname, avatarUrl, bio, role, status, level);
    }

    public static PostDtos.PostCardView toPostCardView(PostRepository.PostViewRow row, List<String> imageUrls) {
        return new PostDtos.PostCardView(
            row.id(),
            row.boardId(),
            row.boardName(),
            toSimpleView(row.authorUserId(), row.authorUsername(), row.authorNickname(), row.authorAvatarUrl(),
                row.authorBio(), row.authorRole(), row.authorStatus(), row.authorLevel()),
            row.title(),
            row.contentText(),
            imageUrls,
            row.likeCount(),
            row.commentCount(),
            row.rewardCoinCount(),
            row.pinned(),
            row.featured(),
            row.status(),
            row.liked(),
            row.favorited(),
            row.createdAt()
        );
    }

    public static PostDtos.PostDetailView toPostDetailView(PostRepository.PostViewRow row, List<String> imageUrls) {
        return new PostDtos.PostDetailView(
            row.id(),
            row.boardId(),
            row.boardName(),
            toSimpleView(row.authorUserId(), row.authorUsername(), row.authorNickname(), row.authorAvatarUrl(),
                row.authorBio(), row.authorRole(), row.authorStatus(), row.authorLevel()),
            row.title(),
            row.contentText(),
            imageUrls,
            row.likeCount(),
            row.commentCount(),
            row.rewardCoinCount(),
            row.pinned(),
            row.pinnedAt(),
            row.pinnedByUserId(),
            row.featured(),
            row.featuredAt(),
            row.featuredByUserId(),
            row.status(),
            row.liked(),
            row.favorited(),
            row.createdAt(),
            row.updatedAt()
        );
    }

    public static FollowDtos.FollowRelationView toFollowRelationView(com.example.demo.repository.SocialRepository.FollowRow row,
                                                                    boolean following) {
        return new FollowDtos.FollowRelationView(
            toSimpleView(row.id(), row.username(), row.nickname(), row.avatarUrl(), row.bio(), row.role(), row.status(), row.level()),
            row.followedAt(),
            following
        );
    }

    public static CoinDtos.CoinLedgerView toCoinLedgerView(CoinRepository.CoinLedgerRow row) {
        return new CoinDtos.CoinLedgerView(
            row.id(),
            row.changeType(),
            row.changeAmount(),
            row.balanceAfter(),
            row.relatedUserId(),
            row.rewardId(),
            row.description(),
            row.createdAt()
        );
    }

    public static NotificationDtos.NotificationGroupView toNotificationGroupView(NotificationRepository.NotificationGroupRow row) {
        return new NotificationDtos.NotificationGroupView(
            row.id(),
            row.eventType(),
            row.targetType(),
            row.targetId(),
            row.totalCount(),
            row.unreadCount(),
            toSimpleView(row.actorUserId(), row.actorUsername(), row.actorNickname(), row.actorAvatarUrl(),
                row.actorBio(), row.actorRole(), row.actorStatus(), row.actorLevel()),
            row.latestAt(),
            row.lastReadAt()
        );
    }

    public static NotificationDtos.NotificationItemView toNotificationItemView(NotificationRepository.NotificationItemRow row) {
        return new NotificationDtos.NotificationItemView(
            row.id(),
            row.groupId(),
            row.eventType(),
            row.targetType(),
            row.targetId(),
            toSimpleView(row.actorUserId(), row.actorUsername(), row.actorNickname(), row.actorAvatarUrl(),
                row.actorBio(), row.actorRole(), row.actorStatus(), row.actorLevel()),
            row.conversationId(),
            row.messageId(),
            row.postId(),
            row.commentId(),
            row.rewardId(),
            row.read(),
            row.readAt(),
            row.createdAt()
        );
    }

    public static ConversationDtos.MessageView toMessageView(ConversationRepository.MessageRow row, List<String> imageUrls) {
        return new ConversationDtos.MessageView(
            row.id(),
            row.conversationId(),
            row.sequenceNo(),
            row.senderUserId(),
            row.contentText(),
            imageUrls,
            row.createdAt()
        );
    }
}
