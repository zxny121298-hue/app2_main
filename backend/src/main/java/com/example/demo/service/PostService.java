package com.example.demo.service;

import com.example.demo.common.BizAssert;
import com.example.demo.common.BusinessException;
import com.example.demo.common.ErrorCodes;
import com.example.demo.common.RequestValidator;
import com.example.demo.dto.PostDtos;
import com.example.demo.model.ForumEnums.BoardStatus;
import com.example.demo.model.ForumEnums.ExpChangeType;
import com.example.demo.model.ForumEnums.NotificationEventType;
import com.example.demo.model.ForumEnums.NotificationTargetType;
import com.example.demo.model.ForumEnums.PostStatus;
import com.example.demo.repository.BoardRepository;
import com.example.demo.repository.PostRepository;
import com.example.demo.security.SecurityUtils;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final BoardRepository boardRepository;
    private final UserAccessService userAccessService;
    private final ExperienceService experienceService;
    private final NotificationService notificationService;
    private final CoinService coinService;

    public PostService(PostRepository postRepository, BoardRepository boardRepository, UserAccessService userAccessService,
                       ExperienceService experienceService, NotificationService notificationService,
                       CoinService coinService) {
        this.postRepository = postRepository;
        this.boardRepository = boardRepository;
        this.userAccessService = userAccessService;
        this.experienceService = experienceService;
        this.notificationService = notificationService;
        this.coinService = coinService;
    }

    @Transactional
    public PostDtos.PostDetailView createPost(PostDtos.CreatePostRequest request) {
        var currentUser = userAccessService.requireCurrentUser();
        userAccessService.assertCanSpeak(currentUser);
        RequestValidator.requireContentOrImages(request.contentText(), request.imageUrls(), "帖子正文和图片不能同时为空");
        var board = boardRepository.findById(request.boardId())
            .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "板块不存在"));
        BizAssert.isTrue(BoardStatus.ENABLED.value().equals(board.status()), ErrorCodes.BAD_REQUEST, "板块已停用");
        Long postId = postRepository.createPost(request.boardId(), currentUser.id(), request.title().trim(), trimToNull(request.contentText()));
        postRepository.insertPostImages(postId, sanitizeImages(request.imageUrls()));
        experienceService.grantExp(currentUser.id(), ExpChangeType.CREATE_POST,
            experienceService.expValueFor(ExpChangeType.CREATE_POST), postId, null, null, "发帖获得经验");
        return getPostDetail(postId);
    }

    public com.example.demo.common.PageResult<PostDtos.PostCardView> pagePosts(Long boardId, long page, long pageSize) {
        Long currentUserId = optionalCurrentUserId();
        var rows = postRepository.pagePosts(currentUserId, boardId, null, page, pageSize);
        return mapPostPage(rows);
    }

    public com.example.demo.common.PageResult<PostDtos.PostCardView> pageSearchPosts(String keyword, long page, long pageSize) {
        Long currentUserId = optionalCurrentUserId();
        return mapPostPage(postRepository.pageSearchPosts(currentUserId, keyword, page, pageSize));
    }

    public com.example.demo.common.PageResult<PostDtos.PostCardView> pageAdminPosts(Long boardId, String keyword,
                                                                                     Boolean pinned, Boolean featured,
                                                                                     long page, long pageSize) {
        userAccessService.requireAdmin();
        return mapPostPage(postRepository.pageAdminPosts(boardId, trimToNull(keyword), pinned, featured, page, pageSize));
    }

    public PostDtos.PostDetailView getPostDetail(Long postId) {
        Long currentUserId = optionalCurrentUserId();
        var row = postRepository.findViewById(postId, currentUserId)
            .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "帖子不存在"));
        BizAssert.isTrue(PostStatus.NORMAL.value().equals(row.status()), ErrorCodes.NOT_FOUND, "帖子不存在");
        Map<Long, List<String>> imageMap = postRepository.listImagesByPostIds(List.of(postId));
        return ViewMapper.toPostDetailView(row, imageMap.getOrDefault(postId, Collections.emptyList()));
    }

    public com.example.demo.common.PageResult<PostDtos.PostCardView> pageMyPosts(long page, long pageSize) {
        var currentUser = userAccessService.requireCurrentUser();
        var rows = postRepository.pagePosts(currentUser.id(), null, currentUser.id(), page, pageSize);
        return mapPostPage(rows);
    }

    public com.example.demo.common.PageResult<PostDtos.PostCardView> pageUserPosts(Long userId, long page, long pageSize) {
        userAccessService.requireById(userId);
        var rows = postRepository.pagePosts(optionalCurrentUserId(), null, userId, page, pageSize);
        return mapPostPage(rows);
    }

    public com.example.demo.common.PageResult<PostDtos.PostCardView> pageFavoritePosts(long page, long pageSize) {
        var currentUser = userAccessService.requireCurrentUser();
        return mapPostPage(postRepository.pageFavoritePosts(currentUser.id(), page, pageSize));
    }

    @Transactional
    public void deletePost(Long postId) {
        var admin = userAccessService.requireAdmin();
        var post = postRepository.lockById(postId)
            .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "帖子不存在"));
        BizAssert.isTrue(PostStatus.NORMAL.value().equals(post.status()), ErrorCodes.BAD_REQUEST, "帖子已删除");
        postRepository.softDelete(postId, admin.id(), LocalDateTime.now());
    }

    @Transactional
    public void togglePinned(Long postId, boolean pinned) {
        var admin = userAccessService.requireAdmin();
        var post = postRepository.lockById(postId)
            .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "帖子不存在"));
        BizAssert.isTrue(PostStatus.NORMAL.value().equals(post.status()), ErrorCodes.BAD_REQUEST, "帖子已删除");
        if (post.isPinned() == pinned) {
            return;
        }
        postRepository.updatePinState(postId, pinned, admin.id(), LocalDateTime.now());
        if (pinned && !experienceService.hasPostExpLog(postId, ExpChangeType.POST_PINNED)) {
            experienceService.grantExp(post.userId(), ExpChangeType.POST_PINNED,
                experienceService.expValueFor(ExpChangeType.POST_PINNED), postId, null, admin.id(), "帖子首次置顶");
        }
    }

    @Transactional
    public void toggleFeatured(Long postId, boolean featured) {
        var admin = userAccessService.requireAdmin();
        var post = postRepository.lockById(postId)
            .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "帖子不存在"));
        BizAssert.isTrue(PostStatus.NORMAL.value().equals(post.status()), ErrorCodes.BAD_REQUEST, "帖子已删除");
        if (post.isFeatured() == featured) {
            return;
        }
        postRepository.updateFeaturedState(postId, featured, admin.id(), LocalDateTime.now());
        if (featured && !experienceService.hasPostExpLog(postId, ExpChangeType.POST_FEATURED)) {
            experienceService.grantExp(post.userId(), ExpChangeType.POST_FEATURED,
                experienceService.expValueFor(ExpChangeType.POST_FEATURED), postId, null, admin.id(), "帖子首次设为精华");
        }
    }

    @Transactional
    public void likePost(Long postId) {
        var currentUser = userAccessService.requireCurrentUser();
        userAccessService.assertCanOperate(currentUser);
        var post = postRepository.findById(postId)
            .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "帖子不存在"));
        BizAssert.isTrue(PostStatus.NORMAL.value().equals(post.status()), ErrorCodes.BAD_REQUEST, "帖子已删除");
        BizAssert.isTrue(!postRepository.existsPostLike(postId, currentUser.id()), ErrorCodes.CONFLICT, "已点赞该帖子");
        postRepository.insertPostLike(postId, currentUser.id());
        postRepository.incrementPostLikeCount(postId, 1);
        notificationService.createNotification(post.userId(), currentUser.id(),
            NotificationEventType.POST_LIKE.value(), NotificationTargetType.POST.value(), postId,
            null, null, postId, null, null);
    }

    @Transactional
    public void unlikePost(Long postId) {
        var currentUser = userAccessService.requireCurrentUser();
        userAccessService.assertCanOperate(currentUser);
        if (!postRepository.existsPostLike(postId, currentUser.id())) {
            return;
        }
        postRepository.deletePostLike(postId, currentUser.id());
        postRepository.incrementPostLikeCount(postId, -1);
    }

    @Transactional
    public void favoritePost(Long postId) {
        var currentUser = userAccessService.requireCurrentUser();
        userAccessService.assertCanOperate(currentUser);
        var post = postRepository.findById(postId)
            .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "帖子不存在"));
        BizAssert.isTrue(PostStatus.NORMAL.value().equals(post.status()), ErrorCodes.BAD_REQUEST, "帖子已删除");
        BizAssert.isTrue(!postRepository.existsPostFavorite(postId, currentUser.id()), ErrorCodes.CONFLICT, "已收藏该帖子");
        postRepository.insertPostFavorite(postId, currentUser.id());
    }

    @Transactional
    public void unfavoritePost(Long postId) {
        var currentUser = userAccessService.requireCurrentUser();
        userAccessService.assertCanOperate(currentUser);
        postRepository.deletePostFavorite(postId, currentUser.id());
    }

    @Transactional
    public void rewardPost(Long postId, PostDtos.RewardRequest request) {
        coinService.rewardPost(postId, request.coinAmount());
    }

    private com.example.demo.common.PageResult<PostDtos.PostCardView> mapPostPage(com.example.demo.common.PageResult<PostRepository.PostViewRow> rows) {
        List<Long> ids = rows.list().stream().map(PostRepository.PostViewRow::id).toList();
        Map<Long, List<String>> imageMap = postRepository.listImagesByPostIds(ids);
        return new com.example.demo.common.PageResult<>(
            rows.total(),
            rows.page(),
            rows.pageSize(),
            rows.list().stream()
                .map(row -> ViewMapper.toPostCardView(row, imageMap.getOrDefault(row.id(), Collections.emptyList())))
                .toList()
        );
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
}
