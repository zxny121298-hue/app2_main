package com.example.demo.service;

import com.example.demo.common.BizAssert;
import com.example.demo.common.BusinessException;
import com.example.demo.common.ErrorCodes;
import com.example.demo.common.RequestValidator;
import com.example.demo.dto.CommentDtos;
import com.example.demo.dto.PostDtos;
import com.example.demo.model.ForumEnums.ExpChangeType;
import com.example.demo.model.ForumEnums.NotificationEventType;
import com.example.demo.model.ForumEnums.NotificationTargetType;
import com.example.demo.model.ForumEnums.PostStatus;
import com.example.demo.model.ForumModels.CommentRecord;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.PostRepository;
import com.example.demo.security.SecurityUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserAccessService userAccessService;
    private final ExperienceService experienceService;
    private final NotificationService notificationService;
    private final CoinService coinService;

    public CommentService(CommentRepository commentRepository, PostRepository postRepository,
                          UserAccessService userAccessService, ExperienceService experienceService,
                          NotificationService notificationService, CoinService coinService) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.userAccessService = userAccessService;
        this.experienceService = experienceService;
        this.notificationService = notificationService;
        this.coinService = coinService;
    }

    @Transactional
    public CommentDtos.CommentView createComment(Long postId, CommentDtos.CreateCommentRequest request) {
        var currentUser = userAccessService.requireCurrentUser();
        userAccessService.assertCanSpeak(currentUser);
        RequestValidator.requireContentOrImages(request.contentText(), request.imageUrls(),
            "Comment content and images cannot both be empty");
        var post = postRepository.findById(postId)
            .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "Post not found"));
        BizAssert.isTrue(PostStatus.NORMAL.value().equals(post.status()), ErrorCodes.BAD_REQUEST, "Post deleted");

        Long parentCommentId = request.parentCommentId();
        Long replyToUserId = null;
        if (parentCommentId != null) {
            var parent = commentRepository.findById(parentCommentId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "Parent comment not found"));
            BizAssert.isTrue(parent.postId().equals(postId), ErrorCodes.BAD_REQUEST,
                "Parent comment does not belong to this post");
            if (request.replyToUserId() != null) {
                BizAssert.isTrue(request.replyToUserId().equals(parent.userId()), ErrorCodes.BAD_REQUEST,
                    "replyToUserId does not match parent comment author");
            }
            replyToUserId = parent.userId();
        }

        Long commentId = commentRepository.createComment(postId, currentUser.id(), parentCommentId, replyToUserId,
            trimToNull(request.contentText()));
        commentRepository.insertCommentImages(commentId, sanitizeImages(request.imageUrls()));
        postRepository.incrementCommentCount(postId, 1);

        ExpChangeType expType = parentCommentId == null ? ExpChangeType.CREATE_COMMENT : ExpChangeType.CREATE_REPLY;
        experienceService.grantExp(currentUser.id(), expType, experienceService.expValueFor(expType), postId, commentId, null,
            parentCommentId == null ? "Comment created" : "Reply created");

        if (parentCommentId == null) {
            notificationService.createNotification(post.userId(), currentUser.id(),
                NotificationEventType.POST_COMMENT.value(), NotificationTargetType.POST.value(), postId,
                null, null, postId, commentId, null);
        } else {
            notificationService.createNotification(replyToUserId, currentUser.id(),
                NotificationEventType.COMMENT_REPLY.value(), NotificationTargetType.COMMENT.value(), parentCommentId,
                null, null, postId, commentId, null);
        }

        return findCommentView(postId, commentId);
    }

    public com.example.demo.common.PageResult<CommentDtos.CommentView> pageComments(Long postId, long page, long pageSize) {
        var post = postRepository.findById(postId)
            .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "Post not found"));
        BizAssert.isTrue(PostStatus.NORMAL.value().equals(post.status()), ErrorCodes.BAD_REQUEST, "Post deleted");
        Long currentUserId = optionalCurrentUserId();
        List<CommentRepository.CommentViewRow> rows = commentRepository.listByPostId(postId, currentUserId);
        Map<Long, List<String>> imageMap = commentRepository.listImagesByCommentIds(rows.stream().map(CommentRepository.CommentViewRow::id).toList());

        Map<Long, MutableCommentNode> nodes = new LinkedHashMap<>();
        for (var row : rows) {
            nodes.put(row.id(), new MutableCommentNode(row, imageMap.getOrDefault(row.id(), Collections.emptyList())));
        }
        List<MutableCommentNode> roots = new ArrayList<>();
        for (MutableCommentNode node : nodes.values()) {
            if (node.row.parentCommentId() == null) {
                roots.add(node);
            } else {
                MutableCommentNode parent = nodes.get(node.row.parentCommentId());
                if (parent != null) {
                    parent.children.add(node);
                } else {
                    roots.add(node);
                }
            }
        }
        long total = roots.size();
        int from = (int) Math.min((page - 1) * pageSize, roots.size());
        int to = (int) Math.min(from + pageSize, roots.size());
        List<CommentDtos.CommentView> list = roots.subList(from, to).stream().map(this::toCommentView).toList();
        return new com.example.demo.common.PageResult<>(total, page, pageSize, list);
    }

    public com.example.demo.common.PageResult<CommentDtos.CommentView> pageFavoriteComments(long page, long pageSize) {
        var currentUser = userAccessService.requireCurrentUser();
        var rows = commentRepository.pageFavoriteComments(currentUser.id(), page, pageSize);
        Map<Long, List<String>> imageMap = commentRepository.listImagesByCommentIds(rows.list().stream().map(CommentRepository.CommentViewRow::id).toList());
        return new com.example.demo.common.PageResult<>(
            rows.total(),
            rows.page(),
            rows.pageSize(),
            rows.list().stream().map(row -> buildFlatCommentView(row, imageMap.getOrDefault(row.id(), Collections.emptyList()))).toList()
        );
    }

    @Transactional
    public void likeComment(Long commentId) {
        var currentUser = userAccessService.requireCurrentUser();
        userAccessService.assertCanOperate(currentUser);
        var comment = requireActiveComment(commentId, true);
        BizAssert.isTrue(!commentRepository.existsCommentLike(commentId, currentUser.id()), ErrorCodes.CONFLICT,
            "Comment already liked");
        commentRepository.insertCommentLike(commentId, currentUser.id());
        commentRepository.incrementCommentLikeCount(commentId, 1);
        notificationService.createNotification(comment.userId(), currentUser.id(),
            NotificationEventType.COMMENT_LIKE.value(), NotificationTargetType.COMMENT.value(), commentId,
            null, null, comment.postId(), commentId, null);
    }

    @Transactional
    public void unlikeComment(Long commentId) {
        var currentUser = userAccessService.requireCurrentUser();
        userAccessService.assertCanOperate(currentUser);
        requireActiveComment(commentId, true);
        if (!commentRepository.existsCommentLike(commentId, currentUser.id())) {
            return;
        }
        commentRepository.deleteCommentLike(commentId, currentUser.id());
        commentRepository.incrementCommentLikeCount(commentId, -1);
    }

    @Transactional
    public void favoriteComment(Long commentId) {
        var currentUser = userAccessService.requireCurrentUser();
        userAccessService.assertCanOperate(currentUser);
        requireActiveComment(commentId, true);
        BizAssert.isTrue(!commentRepository.existsCommentFavorite(commentId, currentUser.id()), ErrorCodes.CONFLICT,
            "Comment already favorited");
        commentRepository.insertCommentFavorite(commentId, currentUser.id());
    }

    @Transactional
    public void unfavoriteComment(Long commentId) {
        var currentUser = userAccessService.requireCurrentUser();
        userAccessService.assertCanOperate(currentUser);
        requireActiveComment(commentId, true);
        commentRepository.deleteCommentFavorite(commentId, currentUser.id());
    }

    @Transactional
    public void rewardComment(Long commentId, PostDtos.RewardRequest request) {
        coinService.rewardComment(commentId, request.coinAmount());
    }

    private CommentDtos.CommentView findCommentView(Long postId, Long commentId) {
        var rows = commentRepository.listByPostId(postId, optionalCurrentUserId());
        Map<Long, List<String>> imageMap = commentRepository.listImagesByCommentIds(rows.stream().map(CommentRepository.CommentViewRow::id).toList());
        return rows.stream()
            .filter(row -> row.id().equals(commentId))
            .findFirst()
            .map(row -> buildFlatCommentView(row, imageMap.getOrDefault(commentId, Collections.emptyList())))
            .orElseThrow(() -> new BusinessException(ErrorCodes.SERVER_ERROR, "Failed to load comment"));
    }

    private CommentDtos.CommentView toCommentView(MutableCommentNode node) {
        return new CommentDtos.CommentView(
            node.row.id(),
            node.row.postId(),
            node.row.parentCommentId(),
            node.row.replyToUserId(),
            ViewMapper.toSimpleView(node.row.userId(), node.row.authorUsername(), node.row.authorNickname(), node.row.authorAvatarUrl(),
                node.row.authorBio(), node.row.authorRole(), node.row.authorStatus(), node.row.authorLevel()),
            ViewMapper.toSimpleView(node.row.replyToUserId(), node.row.replyUsername(), node.row.replyNickname(), node.row.replyAvatarUrl(),
                node.row.replyBio(), node.row.replyRole(), node.row.replyStatus(), node.row.replyLevel()),
            node.row.contentText(),
            node.imageUrls,
            node.row.likeCount(),
            node.row.rewardCoinCount(),
            node.row.liked(),
            node.row.favorited(),
            node.row.createdAt(),
            node.row.updatedAt(),
            node.children.stream().map(this::toCommentView).toList()
        );
    }

    private CommentDtos.CommentView buildFlatCommentView(CommentRepository.CommentViewRow row, List<String> imageUrls) {
        return new CommentDtos.CommentView(
            row.id(),
            row.postId(),
            row.parentCommentId(),
            row.replyToUserId(),
            ViewMapper.toSimpleView(row.userId(), row.authorUsername(), row.authorNickname(), row.authorAvatarUrl(),
                row.authorBio(), row.authorRole(), row.authorStatus(), row.authorLevel()),
            ViewMapper.toSimpleView(row.replyToUserId(), row.replyUsername(), row.replyNickname(), row.replyAvatarUrl(),
                row.replyBio(), row.replyRole(), row.replyStatus(), row.replyLevel()),
            row.contentText(),
            imageUrls,
            row.likeCount(),
            row.rewardCoinCount(),
            row.liked(),
            row.favorited(),
            row.createdAt(),
            row.updatedAt(),
            List.of()
        );
    }

    private CommentRecord requireActiveComment(Long commentId, boolean lockComment) {
        var comment = (lockComment ? commentRepository.lockById(commentId) : commentRepository.findById(commentId))
            .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "Comment not found"));
        var post = postRepository.lockById(comment.postId())
            .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "Post not found"));
        BizAssert.isTrue(PostStatus.NORMAL.value().equals(post.status()), ErrorCodes.BAD_REQUEST, "Post deleted");
        return comment;
    }

    private Long optionalCurrentUserId() {
        var principal = SecurityUtils.getCurrentUserOrNull();
        return principal == null ? null : principal.userId();
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

    private static class MutableCommentNode {
        private final CommentRepository.CommentViewRow row;
        private final List<String> imageUrls;
        private final List<MutableCommentNode> children = new ArrayList<>();

        private MutableCommentNode(CommentRepository.CommentViewRow row, List<String> imageUrls) {
            this.row = row;
            this.imageUrls = imageUrls;
        }
    }
}
