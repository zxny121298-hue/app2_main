package com.example.demo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public final class AiDtos {

    private AiDtos() {
    }

    public record AiChatMessage(
        @NotBlank @Size(max = 16) String role,
        @NotBlank @Size(max = 8000) String content
    ) {
    }

    public record AiChatRequest(
        @NotEmpty @Size(max = 40) List<@Valid AiChatMessage> messages,
        /** 若在帖子详情页打开 AI，可传帖子 ID，服务端会注入该帖正文与部分评论。 */
        @Positive Long contextPostId,
        /** 为 false 时不注入「最近帖子」摘要；默认 null/true 均会注入（省 token 时可关）。 */
        Boolean includeRecentPostSummaries
    ) {
    }

    public record AiChatReply(
        String reply
    ) {
    }
}
