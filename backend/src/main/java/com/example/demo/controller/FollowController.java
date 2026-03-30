package com.example.demo.controller;

import com.example.demo.common.ApiResponse;
import com.example.demo.dto.FollowDtos;
import com.example.demo.service.FollowService;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api")
public class FollowController {

    private final FollowService followService;

    public FollowController(FollowService followService) {
        this.followService = followService;
    }

    @PostMapping("/users/{userId}/follow")
    public ApiResponse<Void> follow(@PathVariable Long userId) {
        followService.follow(userId);
        return ApiResponse.ok();
    }

    @DeleteMapping("/users/{userId}/follow")
    public ApiResponse<Void> unfollow(@PathVariable Long userId) {
        followService.unfollow(userId);
        return ApiResponse.ok();
    }

    @GetMapping("/follows")
    public ApiResponse<com.example.demo.common.PageResult<FollowDtos.FollowRelationView>> pageFollows(
        @RequestParam(defaultValue = "1") @Min(1) long page,
        @RequestParam(defaultValue = "20") @Min(1) long pageSize
    ) {
        return ApiResponse.ok(followService.pageMyFollows(page, pageSize));
    }

    @GetMapping("/fans")
    public ApiResponse<com.example.demo.common.PageResult<FollowDtos.FollowRelationView>> pageFans(
        @RequestParam(defaultValue = "1") @Min(1) long page,
        @RequestParam(defaultValue = "20") @Min(1) long pageSize
    ) {
        return ApiResponse.ok(followService.pageMyFans(page, pageSize));
    }

    @GetMapping("/users/{userId}/followed")
    public ApiResponse<FollowDtos.FollowStatusView> isFollowing(@PathVariable Long userId) {
        return ApiResponse.ok(followService.isFollowing(userId));
    }
}
