package com.example.demo.config;

import com.openai.client.OpenAIClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "forum.llm", name = "enabled", havingValue = "true")
public class LlmAvailabilityLogger {

    private static final Logger log = LoggerFactory.getLogger(LlmAvailabilityLogger.class);

    private final ObjectProvider<OpenAIClient> openAIClient;

    public LlmAvailabilityLogger(ObjectProvider<OpenAIClient> openAIClient) {
        this.openAIClient = openAIClient;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void warnIfEnabledButNoClient() {
        if (openAIClient.getIfAvailable() == null) {
            log.warn(
                "forum.llm.enabled=true 但未配置有效的 forum.llm.api-key（请设置环境变量 FORUM_LLM_API_KEY，或使用 application-local.yml）。LLM 客户端未创建，相关功能不可用。");
        }
    }
}
