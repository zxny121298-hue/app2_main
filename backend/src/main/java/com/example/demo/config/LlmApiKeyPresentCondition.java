package com.example.demo.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

/**
 * 仅在配置了非空 {@code forum.llm.api-key}（含环境变量 FORUM_LLM_API_KEY）时启用 OpenAI 客户端。
 */
public class LlmApiKeyPresentCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String key = context.getEnvironment().getProperty("forum.llm.api-key");
        return StringUtils.hasText(key == null ? null : key.trim());
    }
}
