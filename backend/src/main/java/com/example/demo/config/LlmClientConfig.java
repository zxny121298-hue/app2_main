package com.example.demo.config;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.Timeout;
import java.net.Proxy;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "forum.llm", name = "enabled", havingValue = "true")
@Conditional(LlmApiKeyPresentCondition.class)
public class LlmClientConfig {

    private static final Logger log = LoggerFactory.getLogger(LlmClientConfig.class);

    @Bean
    public OpenAIClient openAIClient(AppProperties properties) {
        var llm = properties.getLlm();
        String base = llm.getBaseUrl() == null ? "" : llm.getBaseUrl().trim();
        if (base.endsWith("/v1") || base.endsWith("/v1/")) {
            throw new IllegalStateException(
                "forum.llm.base-url 不应包含 /v1（OpenAI Java SDK 会自动追加），请改为例如 https://api.scnet.cn/api/llm");
        }
        if (base.contains("/chat/completions")) {
            throw new IllegalStateException(
                "forum.llm.base-url 不要包含 /chat/completions（SDK 会自动追加）。火山方舟应写至 /api/v3，例如 https://ark.cn-beijing.volces.com/api/v3");
        }
        String key = llm.getApiKey() == null ? "" : llm.getApiKey().trim();
        // 避免 yml 里写了 "Bearer xxx" 导致 SDK 再拼一层 Bearer
        if (key.length() > 7 && key.regionMatches(true, 0, "Bearer ", 0, 7)) {
            key = key.substring(7).trim();
        }
        log.info("OpenAI 兼容客户端已创建：baseUrl={}，apiKey 长度={}，directConnection={}",
            base, key.length(), llm.isDirectConnection());
        // 勿只传 Duration：SDK 会当成「整次 request 上限」，connect/read 默认与 request 耦合，慢 TLS（如部分网络下 >10s）易触发 getsockopt 超时。
        Duration connect = llm.getConnectTimeout();
        Duration read = llm.getReadTimeout();
        Duration request = connect.plus(read).plus(connect);
        Timeout timeout =
            Timeout.builder().connect(connect).read(read).write(read).request(request).build();
        var b = OpenAIOkHttpClient.builder().apiKey(key).baseUrl(base).timeout(timeout);
        if (llm.isDirectConnection()) {
            b.proxy(Proxy.NO_PROXY);
        }
        return b.build();
    }
}
