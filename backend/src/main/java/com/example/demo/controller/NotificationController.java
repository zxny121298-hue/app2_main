package com.example.demo.controller;

import com.example.demo.common.ApiResponse;
import com.example.demo.dto.NotificationDtos;
import com.example.demo.service.NotificationService;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/groups")
    public ApiResponse<com.example.demo.common.PageResult<NotificationDtos.NotificationGroupView>> pageGroups(
        @RequestParam(defaultValue = "1") @Min(1) long page,
        @RequestParam(defaultValue = "20") @Min(1) long pageSize
    ) {
        return ApiResponse.ok(notificationService.pageGroups(page, pageSize));
    }

    @GetMapping("/groups/{groupId}/items")
    public ApiResponse<com.example.demo.common.PageResult<NotificationDtos.NotificationItemView>> pageGroupItems(
        @PathVariable Long groupId,
        @RequestParam(defaultValue = "1") @Min(1) long page,
        @RequestParam(defaultValue = "20") @Min(1) long pageSize
    ) {
        return ApiResponse.ok(notificationService.pageGroupItems(groupId, page, pageSize));
    }

    @PostMapping("/items/{itemId}/read")
    public ApiResponse<Void> markItemRead(@PathVariable Long itemId) {
        notificationService.markItemRead(itemId);
        return ApiResponse.ok();
    }

    @PostMapping("/groups/{groupId}/read")
    public ApiResponse<Void> markGroupRead(@PathVariable Long groupId) {
        notificationService.markGroupRead(groupId);
        return ApiResponse.ok();
    }

    @PostMapping("/read-all")
    public ApiResponse<Void> markAllRead() {
        notificationService.markAllRead();
        return ApiResponse.ok();
    }

    @GetMapping("/unread-count")
    public ApiResponse<NotificationDtos.NotificationUnreadView> getUnreadCount() {
        return ApiResponse.ok(notificationService.getUnreadCount());
    }
}
