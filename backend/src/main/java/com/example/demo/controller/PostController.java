package com.example.demo.controller;

import com.example.demo.common.ApiResponse;
import com.example.demo.dto.PostDtos;
import com.example.demo.service.PostService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping("/posts")
    public ApiResponse<PostDtos.PostDetailView> createPost(@Valid @RequestBody PostDtos.CreatePostRequest request) {
        return ApiResponse.ok(postService.createPost(request));
    }

    @GetMapping("/posts")
    public ApiResponse<com.example.demo.common.PageResult<PostDtos.PostCardView>> pagePosts(
        @RequestParam(required = false) Long boardId,
        @RequestParam(defaultValue = "1") @Min(1) long page,
        @RequestParam(defaultValue = "20") @Min(1) long pageSize
    ) {
        return ApiResponse.ok(postService.pagePosts(boardId, page, pageSize));
    }

    @GetMapping("/posts/{postId}")
    public ApiResponse<PostDtos.PostDetailView> getPostDetail(@PathVariable Long postId) {
        return ApiResponse.ok(postService.getPostDetail(postId));
    }

    @GetMapping("/admin/posts")
    public ApiResponse<com.example.demo.common.PageResult<PostDtos.PostCardView>> pageAdminPosts(
        @RequestParam(required = false) Long boardId,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Boolean pinned,
        @RequestParam(required = false) Boolean featured,
        @RequestParam(defaultValue = "1") @Min(1) long page,
        @RequestParam(defaultValue = "20") @Min(1) long pageSize
    ) {
        return ApiResponse.ok(postService.pageAdminPosts(boardId, keyword, pinned, featured, page, pageSize));
    }

    @GetMapping("/posts/mine")
    public ApiResponse<com.example.demo.common.PageResult<PostDtos.PostCardView>> pageMyPosts(
        @RequestParam(defaultValue = "1") @Min(1) long page,
        @RequestParam(defaultValue = "20") @Min(1) long pageSize
    ) {
        return ApiResponse.ok(postService.pageMyPosts(page, pageSize));
    }

    @GetMapping("/users/{userId}/posts")
    public ApiResponse<com.example.demo.common.PageResult<PostDtos.PostCardView>> pageUserPosts(
        @PathVariable Long userId,
        @RequestParam(defaultValue = "1") @Min(1) long page,
        @RequestParam(defaultValue = "20") @Min(1) long pageSize
    ) {
        return ApiResponse.ok(postService.pageUserPosts(userId, page, pageSize));
    }

    @GetMapping("/favorites/posts")
    public ApiResponse<com.example.demo.common.PageResult<PostDtos.PostCardView>> pageFavoritePosts(
        @RequestParam(defaultValue = "1") @Min(1) long page,
        @RequestParam(defaultValue = "20") @Min(1) long pageSize
    ) {
        return ApiResponse.ok(postService.pageFavoritePosts(page, pageSize));
    }

    @DeleteMapping("/admin/posts/{postId}")
    public ApiResponse<Void> deletePost(@PathVariable Long postId) {
        postService.deletePost(postId);
        return ApiResponse.ok();
    }

    @PutMapping("/admin/posts/{postId}/pin")
    public ApiResponse<Void> togglePin(@PathVariable Long postId, @Valid @RequestBody PostDtos.TogglePinRequest request) {
        postService.togglePinned(postId, request.pinned());
        return ApiResponse.ok();
    }

    @PutMapping("/admin/posts/{postId}/feature")
    public ApiResponse<Void> toggleFeature(@PathVariable Long postId, @Valid @RequestBody PostDtos.ToggleFeaturedRequest request) {
        postService.toggleFeatured(postId, request.featured());
        return ApiResponse.ok();
    }

    @PostMapping("/posts/{postId}/like")
    public ApiResponse<Void> likePost(@PathVariable Long postId) {
        postService.likePost(postId);
        return ApiResponse.ok();
    }

    @DeleteMapping("/posts/{postId}/like")
    public ApiResponse<Void> unlikePost(@PathVariable Long postId) {
        postService.unlikePost(postId);
        return ApiResponse.ok();
    }

    @PostMapping("/posts/{postId}/favorite")
    public ApiResponse<Void> favoritePost(@PathVariable Long postId) {
        postService.favoritePost(postId);
        return ApiResponse.ok();
    }

    @DeleteMapping("/posts/{postId}/favorite")
    public ApiResponse<Void> unfavoritePost(@PathVariable Long postId) {
        postService.unfavoritePost(postId);
        return ApiResponse.ok();
    }

    @PostMapping("/posts/{postId}/reward")
    public ApiResponse<Void> rewardPost(@PathVariable Long postId, @Valid @RequestBody PostDtos.RewardRequest request) {
        postService.rewardPost(postId, request);
        return ApiResponse.ok();
    }
}
