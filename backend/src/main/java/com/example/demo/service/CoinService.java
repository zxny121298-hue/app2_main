package com.example.demo.service;

import com.example.demo.common.BizAssert;
import com.example.demo.common.BusinessException;
import com.example.demo.common.ErrorCodes;
import com.example.demo.dto.CoinDtos;
import com.example.demo.dto.UserDtos;
import com.example.demo.model.ForumEnums.CoinChangeType;
import com.example.demo.model.ForumEnums.NotificationEventType;
import com.example.demo.model.ForumEnums.NotificationTargetType;
import com.example.demo.model.ForumEnums.PostStatus;
import com.example.demo.repository.CoinRepository;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.PostRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CoinService {

    private final UserAccessService userAccessService;
    private final UserRepository userRepository;
    private final CoinRepository coinRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final NotificationService notificationService;

    public CoinService(UserAccessService userAccessService, UserRepository userRepository, CoinRepository coinRepository,
                       PostRepository postRepository, CommentRepository commentRepository,
                       NotificationService notificationService) {
        this.userAccessService = userAccessService;
        this.userRepository = userRepository;
        this.coinRepository = coinRepository;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.notificationService = notificationService;
    }

    public CoinDtos.CoinBalanceView getCurrentBalance() {
        var currentUser = userAccessService.requireCurrentUser();
        return new CoinDtos.CoinBalanceView(userAccessService.requireById(currentUser.id()).coinBalance());
    }

    public com.example.demo.common.PageResult<CoinDtos.CoinLedgerView> pageLedgers(long page, long pageSize) {
        var currentUser = userAccessService.requireCurrentUser();
        var rows = coinRepository.pageCoinLedgers(currentUser.id(), page, pageSize);
        return new com.example.demo.common.PageResult<>(
            rows.total(),
            rows.page(),
            rows.pageSize(),
            rows.list().stream().map(ViewMapper::toCoinLedgerView).toList()
        );
    }

    @Transactional
    public long grantSignInReward(Long userId, long coinAmount) {
        BizAssert.isTrue(coinAmount >= 0L, ErrorCodes.BAD_REQUEST, "Check-in coin reward cannot be negative");
        if (coinAmount == 0L) {
            return userRepository.lockById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "User not found"))
                .coinBalance();
        }
        return adjustBalance(userId, coinAmount, CoinChangeType.SIGN_IN, null, null, "每日签到", null);
    }

    @Transactional
    public UserDtos.UserProfileView adminAdjustCoin(Long userId, UserDtos.AdminAdjustCoinRequest request) {
        var admin = userAccessService.requireAdmin();
        BizAssert.isTrue(request.changeAmount() != 0L, ErrorCodes.BAD_REQUEST, "Coin adjustment cannot be 0");
        adjustBalance(userId, request.changeAmount(), CoinChangeType.ADMIN_ADJUST, null, null, request.remark(), admin.id());
        return ViewMapper.toProfileView(userAccessService.requireById(userId));
    }

    @Transactional
    public void rewardPost(Long postId, long coinAmount) {
        var sender = userAccessService.requireCurrentUser();
        userAccessService.assertCanOperate(sender);
        var post = postRepository.lockById(postId)
            .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "Post not found"));
        BizAssert.isTrue(PostStatus.NORMAL.value().equals(post.status()), ErrorCodes.BAD_REQUEST, "Post deleted");
        transferReward(sender.id(), post.userId(), coinAmount,
            "Reward post #" + postId, "Received reward for post #" + postId,
            rewardId -> postRepository.incrementRewardCoinCount(postId, coinAmount),
            postId, null,
            NotificationEventType.POST_REWARD.value(), NotificationTargetType.POST.value(), postId
        );
    }

    @Transactional
    public void rewardComment(Long commentId, long coinAmount) {
        var sender = userAccessService.requireCurrentUser();
        userAccessService.assertCanOperate(sender);
        var comment = commentRepository.lockById(commentId)
            .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "Comment not found"));
        var post = postRepository.lockById(comment.postId())
            .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "Post not found"));
        BizAssert.isTrue(PostStatus.NORMAL.value().equals(post.status()), ErrorCodes.BAD_REQUEST, "Post deleted");
        transferReward(sender.id(), comment.userId(), coinAmount,
            "Reward comment #" + commentId, "Received reward for comment #" + commentId,
            rewardId -> commentRepository.incrementRewardCoinCount(commentId, coinAmount),
            null, commentId,
            NotificationEventType.COMMENT_REWARD.value(), NotificationTargetType.COMMENT.value(), commentId
        );
    }

    private void transferReward(Long senderUserId, Long recipientUserId, long coinAmount,
                                String senderDescription, String recipientDescription,
                                java.util.function.LongConsumer rewardTargetUpdater,
                                Long postId, Long commentId, String eventType, String targetType, Long targetId) {
        BizAssert.isTrue(!senderUserId.equals(recipientUserId), ErrorCodes.BAD_REQUEST, "Cannot reward yourself");
        BizAssert.isTrue(coinAmount > 0, ErrorCodes.BAD_REQUEST, "Reward amount must be greater than 0");

        var firstLockUserId = Math.min(senderUserId, recipientUserId);
        var secondLockUserId = Math.max(senderUserId, recipientUserId);
        var firstUser = userRepository.lockById(firstLockUserId)
            .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "User not found"));
        var secondUser = userRepository.lockById(secondLockUserId)
            .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "User not found"));
        var sender = senderUserId.equals(firstUser.id()) ? firstUser : secondUser;
        var recipient = recipientUserId.equals(firstUser.id()) ? firstUser : secondUser;

        BizAssert.isTrue(sender.coinBalance() >= coinAmount, ErrorCodes.BAD_REQUEST, "Insufficient coin balance");
        long senderNewBalance = sender.coinBalance() - coinAmount;
        long recipientNewBalance = recipient.coinBalance() + coinAmount;
        userRepository.updateCoinBalance(sender.id(), senderNewBalance);
        userRepository.updateCoinBalance(recipient.id(), recipientNewBalance);
        long rewardId = coinRepository.createReward(sender.id(), recipient.id(), postId, commentId, coinAmount);
        coinRepository.createCoinLedger(sender.id(), CoinChangeType.REWARD_SEND.value(), -coinAmount, senderNewBalance,
            recipient.id(), rewardId, senderDescription);
        coinRepository.createCoinLedger(recipient.id(), CoinChangeType.REWARD_RECEIVE.value(), coinAmount, recipientNewBalance,
            sender.id(), rewardId, recipientDescription);
        rewardTargetUpdater.accept(rewardId);
        notificationService.createNotification(recipient.id(), sender.id(), eventType, targetType, targetId, null, null, postId, commentId, rewardId);
    }

    private long adjustBalance(Long userId, long changeAmount, CoinChangeType changeType, Long relatedUserId,
                               Long rewardId, String description, Long operatorUserId) {
        var locked = userRepository.lockById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "User not found"));
        long newBalance = locked.coinBalance() + changeAmount;
        BizAssert.isTrue(newBalance >= 0, ErrorCodes.BAD_REQUEST, "Coin balance cannot be negative");
        userRepository.updateCoinBalance(userId, newBalance);
        String desc = description == null || description.isBlank() ? "Admin adjustment" : description.trim();
        coinRepository.createCoinLedger(userId, changeType.value(), changeAmount, newBalance, relatedUserId, rewardId,
            operatorUserId == null ? desc : desc + " (operator=" + operatorUserId + ")");
        return newBalance;
    }
}
