package com.example.demo.controller;

import com.example.demo.common.ApiResponse;
import com.example.demo.dto.AiDtos;
import com.example.demo.service.AiChatService;
import com.example.demo.service.UserAccessService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/ai")
public class AiController {

    private final UserAccessService userAccessService;
    private final AiChatService aiChatService;

    public AiController(UserAccessService userAccessService, AiChatService aiChatService) {
        this.userAccessService = userAccessService;
        this.aiChatService = aiChatService;
    }

    @PostMapping("/chat")
    public ApiResponse<AiDtos.AiChatReply> chat(@Valid @RequestBody AiDtos.AiChatRequest request) {
        userAccessService.requireCurrentUser();
        return ApiResponse.ok(aiChatService.chat(request));
    }
}
