package com.example.demo.controller;

import com.example.demo.common.ApiResponse;
import com.example.demo.dto.ConversationDtos;
import com.example.demo.service.ConversationService;
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
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @PostMapping("/conversations")
    public ApiResponse<ConversationDtos.ConversationDetailView> createConversation(
        @Valid @RequestBody ConversationDtos.CreateConversationRequest request
    ) {
        return ApiResponse.ok(conversationService.createOrReuseConversation(request));
    }

    @PostMapping("/conversations/{conversationId}/messages")
    public ApiResponse<ConversationDtos.MessageView> sendMessage(
        @PathVariable Long conversationId,
        @Valid @RequestBody ConversationDtos.SendMessageRequest request
    ) {
        return ApiResponse.ok(conversationService.sendMessage(conversationId, request));
    }

    @GetMapping("/conversations")
    public ApiResponse<com.example.demo.common.PageResult<ConversationDtos.ConversationListView>> pageConversations(
        @RequestParam(defaultValue = "1") @Min(1) long page,
        @RequestParam(defaultValue = "20") @Min(1) long pageSize
    ) {
        return ApiResponse.ok(conversationService.pageConversations(page, pageSize));
    }

    @GetMapping("/conversations/{conversationId}")
    public ApiResponse<ConversationDtos.ConversationDetailView> getConversationDetail(
        @PathVariable Long conversationId,
        @RequestParam(defaultValue = "1") @Min(1) long page,
        @RequestParam(defaultValue = "20") @Min(1) long pageSize
    ) {
        return ApiResponse.ok(conversationService.getConversationDetail(conversationId, page, pageSize));
    }

    @PostMapping("/conversations/{conversationId}/read")
    public ApiResponse<Void> markRead(@PathVariable Long conversationId) {
        conversationService.markRead(conversationId);
        return ApiResponse.ok();
    }

    @PutMapping("/conversations/{conversationId}/pin")
    public ApiResponse<Void> updatePinned(@PathVariable Long conversationId,
                                          @Valid @RequestBody ConversationDtos.ConversationSettingRequest request) {
        conversationService.updatePinned(conversationId, request.value());
        return ApiResponse.ok();
    }

    @PutMapping("/conversations/{conversationId}/mute")
    public ApiResponse<Void> updateMuted(@PathVariable Long conversationId,
                                         @Valid @RequestBody ConversationDtos.ConversationSettingRequest request) {
        conversationService.updateMuted(conversationId, request.value());
        return ApiResponse.ok();
    }

    @DeleteMapping("/conversations/{conversationId}")
    public ApiResponse<Void> deleteConversation(@PathVariable Long conversationId) {
        conversationService.deleteConversation(conversationId);
        return ApiResponse.ok();
    }
}
