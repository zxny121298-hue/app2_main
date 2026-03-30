package com.example.demo.controller;

import com.example.demo.common.ApiResponse;
import com.example.demo.dto.CommentDtos;
import com.example.demo.dto.PostDtos;
import com.example.demo.service.CommentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping("/posts/{postId}/comments")
    public ApiResponse<CommentDtos.CommentView> createComment(@PathVariable Long postId,
                                                             @Valid @RequestBody CommentDtos.CreateCommentRequest request) {
        return ApiResponse.ok(commentService.createComment(postId, request));
    }

    @GetMapping("/posts/{postId}/comments")
    public ApiResponse<com.example.demo.common.PageResult<CommentDtos.CommentView>> pageComments(
        @PathVariable Long postId,
        @RequestParam(defaultValue = "1") @Min(1) long page,
        @RequestParam(defaultValue = "20") @Min(1) long pageSize
    ) {
        return ApiResponse.ok(commentService.pageComments(postId, page, pageSize));
    }

    @GetMapping("/favorites/comments")
    public ApiResponse<com.example.demo.common.PageResult<CommentDtos.CommentView>> pageFavoriteComments(
        @RequestParam(defaultValue = "1") @Min(1) long page,
        @RequestParam(defaultValue = "20") @Min(1) long pageSize
    ) {
        return ApiResponse.ok(commentService.pageFavoriteComments(page, pageSize));
    }

    @PostMapping("/comments/{commentId}/like")
    public ApiResponse<Void> likeComment(@PathVariable Long commentId) {
        commentService.likeComment(commentId);
        return ApiResponse.ok();
    }

    @DeleteMapping("/comments/{commentId}/like")
    public ApiResponse<Void> unlikeComment(@PathVariable Long commentId) {
        commentService.unlikeComment(commentId);
        return ApiResponse.ok();
    }

    @PostMapping("/comments/{commentId}/favorite")
    public ApiResponse<Void> favoriteComment(@PathVariable Long commentId) {
        commentService.favoriteComment(commentId);
        return ApiResponse.ok();
    }

    @DeleteMapping("/comments/{commentId}/favorite")
    public ApiResponse<Void> unfavoriteComment(@PathVariable Long commentId) {
        commentService.unfavoriteComment(commentId);
        return ApiResponse.ok();
    }

    @PostMapping("/comments/{commentId}/reward")
    public ApiResponse<Void> rewardComment(@PathVariable Long commentId, @Valid @RequestBody PostDtos.RewardRequest request) {
        commentService.rewardComment(commentId, request);
        return ApiResponse.ok();
    }
}
