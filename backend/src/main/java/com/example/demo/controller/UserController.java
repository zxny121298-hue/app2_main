package com.example.demo.controller;

import com.example.demo.common.ApiResponse;
import com.example.demo.dto.UserDtos;
import com.example.demo.service.AuthService;
import com.example.demo.service.CoinService;
import com.example.demo.service.ExperienceService;
import com.example.demo.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
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
public class UserController {

    private final UserService userService;
    private final AuthService authService;
    private final ExperienceService experienceService;
    private final CoinService coinService;

    public UserController(UserService userService, AuthService authService,
                          ExperienceService experienceService, CoinService coinService) {
        this.userService = userService;
        this.authService = authService;
        this.experienceService = experienceService;
        this.coinService = coinService;
    }

    @GetMapping("/users/me")
    public ApiResponse<UserDtos.UserProfileView> getCurrentProfile() {
        return ApiResponse.ok(userService.getCurrentProfile());
    }

    @GetMapping("/users/{userId}")
    public ApiResponse<UserDtos.UserProfileView> getUserProfile(@PathVariable Long userId) {
        return ApiResponse.ok(userService.getUserProfile(userId));
    }

    @GetMapping("/admin/users")
    public ApiResponse<com.example.demo.common.PageResult<UserDtos.UserProfileView>> pageAdminUsers(
        @RequestParam(defaultValue = "1") @Min(1) long page,
        @RequestParam(defaultValue = "20") @Min(1) long pageSize,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String role
    ) {
        return ApiResponse.ok(userService.pageAdminUsers(keyword, status, role, page, pageSize));
    }

    @PutMapping("/users/me")
    public ApiResponse<UserDtos.UserProfileView> updateProfile(@Valid @RequestBody UserDtos.UpdateProfileRequest request) {
        return ApiResponse.ok(userService.updateCurrentProfile(request));
    }

    @PutMapping("/users/me/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody UserDtos.ChangePasswordRequest request) {
        authService.changePassword(request);
        return ApiResponse.ok();
    }

    @PostMapping("/check-ins")
    public ApiResponse<UserDtos.CheckInView> checkIn() {
        return ApiResponse.ok(experienceService.checkIn());
    }

    @GetMapping("/check-ins/today")
    public ApiResponse<UserDtos.CheckInView> getTodayCheckIn() {
        return ApiResponse.ok(experienceService.getTodayStatus());
    }

    @GetMapping("/users/me/exp-logs")
    public ApiResponse<com.example.demo.common.PageResult<UserDtos.ExpLogView>> pageMyExpLogs(
        @RequestParam(defaultValue = "1") @Min(1) long page,
        @RequestParam(defaultValue = "20") @Min(1) long pageSize
    ) {
        return ApiResponse.ok(experienceService.pageMyExpLogs(page, pageSize));
    }

    @PostMapping("/admin/users/{userId}/ban")
    public ApiResponse<Void> banUser(@PathVariable Long userId, @Valid @RequestBody UserDtos.AdminBanRequest request) {
        userService.banUser(userId, request);
        return ApiResponse.ok();
    }

    @PostMapping("/admin/users/{userId}/unban")
    public ApiResponse<Void> unbanUser(@PathVariable Long userId) {
        userService.unbanUser(userId);
        return ApiResponse.ok();
    }

    @PostMapping("/admin/users/{userId}/mute")
    public ApiResponse<Void> muteUser(@PathVariable Long userId, @Valid @RequestBody UserDtos.AdminMuteRequest request) {
        userService.muteUser(userId, request);
        return ApiResponse.ok();
    }

    @PostMapping("/admin/users/{userId}/unmute")
    public ApiResponse<Void> unmuteUser(@PathVariable Long userId) {
        userService.unmuteUser(userId);
        return ApiResponse.ok();
    }

    @PostMapping("/admin/users/{userId}/exp-adjust")
    public ApiResponse<UserDtos.UserProfileView> adjustExp(@PathVariable Long userId,
                                                           @Valid @RequestBody UserDtos.AdminAdjustExpRequest request) {
        return ApiResponse.ok(experienceService.adminAdjustExp(userId, request));
    }

    @PostMapping("/admin/users/{userId}/coin-adjust")
    public ApiResponse<UserDtos.UserProfileView> adjustCoin(@PathVariable Long userId,
                                                            @Valid @RequestBody UserDtos.AdminAdjustCoinRequest request) {
        return ApiResponse.ok(coinService.adminAdjustCoin(userId, request));
    }
}
