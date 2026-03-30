package com.example.demo.service;

import com.example.demo.common.BusinessException;
import com.example.demo.common.ErrorCodes;
import com.example.demo.config.AppProperties;
import com.example.demo.dto.AiDtos;
import com.openai.client.OpenAIClient;
import com.openai.errors.OpenAIServiceException;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 通过 OpenAI 兼容协议调用 SCNet 等平台的 Chat Completions。
 * 文档：https://www.scnet.cn/ac/openapi/doc/2.0/moduleapi/tutorial/apicall.html
 */
@Service
public class ScnetLlmService {

    private static final Logger log = LoggerFactory.getLogger(ScnetLlmService.class);

    private final AppProperties appProperties;
    private final OpenAIClient openAIClient;

    public ScnetLlmService(AppProperties appProperties, @Autowired(required = false) OpenAIClient openAIClient) {
        this.appProperties = appProperties;
        this.openAIClient = openAIClient;
    }

    public boolean isConfigured() {
        return openAIClient != null && appProperties.getLlm().isEnabled();
    }

    /**
     * 非流式对话补全；失败时返回 empty，由业务层降级。
     */
    public Optional<String> chatCompletion(String systemPrompt, String userMessage) {
        if (!isConfigured()) {
            return Optional.empty();
        }
        var llm = appProperties.getLlm();
        try {
            ChatCompletionCreateParams.Builder b = ChatCompletionCreateParams.builder()
                .model(llm.getModel());
            if (systemPrompt != null && !systemPrompt.isBlank()) {
                b = b.addSystemMessage(systemPrompt.trim());
            }
            ChatCompletion completion = openAIClient.chat().completions().create(b.addUserMessage(userMessage).build());
            if (completion.choices().isEmpty()) {
                return Optional.empty();
            }
            var content = completion.choices().get(0).message().content();
            return content.map(String::trim).filter(s -> !s.isEmpty());
        } catch (Exception e) {
            log.warn("LLM 调用失败 model={}: {}", llm.getModel(), e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 多轮对话：按顺序发送 system / user / assistant 消息。
     * 失败时抛出 {@link BusinessException}，message 中带可给前端展示的简要原因（便于排查 model/key 等）。
     */
    public String chatCompletionMessages(List<AiDtos.AiChatMessage> messages) {
        if (!isConfigured() || messages == null || messages.isEmpty()) {
            throw new BusinessException(ErrorCodes.SERVER_ERROR,
                "AI 未初始化：请确认 forum.llm.enabled=true 且已配置 FORUM_LLM_API_KEY。");
        }
        var llm = appProperties.getLlm();
        try {
            ChatCompletionCreateParams.Builder b = ChatCompletionCreateParams.builder()
                .model(llm.getModel());
            for (AiDtos.AiChatMessage m : messages) {
                String role = m.role() == null ? "" : m.role().trim().toLowerCase();
                String content = m.content() == null ? "" : m.content().trim();
                if (content.isEmpty()) {
                    continue;
                }
                b = switch (role) {
                    case "system" -> b.addSystemMessage(content);
                    case "assistant" -> b.addAssistantMessage(content);
                    case "user" -> b.addUserMessage(content);
                    default -> b;
                };
            }
            ChatCompletion completion = openAIClient.chat().completions().create(b.build());
            if (completion.choices().isEmpty()) {
                throw new BusinessException(ErrorCodes.SERVER_ERROR,
                    "模型未返回候选结果（choices 为空）。请将 forum.llm.model 改成与 SCNet 控制台「模型列表」完全一致的名称（含大小写、符号）。");
            }
            var choice = completion.choices().get(0);
            var text = choice.message().content();
            if (text.isPresent()) {
                String s = text.get().trim();
                if (!s.isEmpty()) {
                    return s;
                }
            }
            String finish = choice.finishReason() != null ? choice.finishReason().toString() : "unknown";
            log.warn("LLM 返回空正文 model={} finishReason={} message={}", llm.getModel(), finish, choice.message());
            throw new BusinessException(ErrorCodes.SERVER_ERROR,
                "模型返回空正文（finish_reason=" + finish
                    + "）。可尝试在 SCNet 换用非推理类对话模型，或查看后端日志中的 LLM 返回结构。");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("LLM 多轮调用失败 model={}", llm.getModel(), e);
            String detail = sanitizeClientMessage(e);
            String upstream = describeOpenAiHttpError(e);
            if (!upstream.isEmpty()) {
                log.warn("LLM 上游 HTTP 详情：{}", upstream);
                detail = detail + "；" + upstream;
            }
            String extra = "";
            String bu = appProperties.getLlm().getBaseUrl();
            if (bu != null
                && bu.contains("volces.com")
                && (detail.contains("API key format is incorrect") || detail.contains("format is incorrect"))) {
                extra = " 【火山方舟】须使用「创建 API Key」弹窗里只出现一次的完整 Secret；列表/详情里常见的 UUID 是 Key ID，不能当 Bearer 密钥。请新建 Key 并立刻复制 Secret 到 forum.llm.api-key，或参考 https://www.volcengine.com/docs/82379/1541594 。";
            }
            throw new BusinessException(ErrorCodes.SERVER_ERROR,
                "模型接口报错：" + detail
                    + extra
                    + "。请核对 forum.llm.api-key、额度、forum.llm.model 与 forum.llm.base-url；OpenAI 兼容网关一般不要在 base-url 末尾手写 /v1（由 SDK 自动拼接）。");
        }
    }

    /**
     * 从 OpenAI Java SDK 的 {@link OpenAIServiceException}（含 cause 链）提取状态码与响应体摘要，便于区分 401/403/欠费等。
     */
    private static String describeOpenAiHttpError(Throwable e) {
        OpenAIServiceException se = findOpenAIServiceException(e);
        if (se == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("http=").append(se.statusCode());
        try {
            var body = se.body();
            if (body != null) {
                String raw = body.toString().replace('\n', ' ').replace('\r', ' ').trim();
                if (!raw.isEmpty() && !"null".equalsIgnoreCase(raw)) {
                    if (raw.length() > 400) {
                        raw = raw.substring(0, 400) + "…";
                    }
                    sb.append(" body=").append(raw);
                }
            }
        } catch (Exception ignored) {
            // body() 解析失败时仍返回 statusCode
        }
        return sb.toString();
    }

    private static OpenAIServiceException findOpenAIServiceException(Throwable e) {
        Throwable t = e;
        int guard = 0;
        while (t != null && guard++ < 8) {
            if (t instanceof OpenAIServiceException ose) {
                return ose;
            }
            t = t.getCause();
        }
        return null;
    }

    private static String sanitizeClientMessage(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null && t.getCause() != t) {
            t = t.getCause();
        }
        String m = t.getMessage();
        if (m == null || m.isBlank()) {
            m = e.getClass().getSimpleName();
        }
        m = m.replace('\n', ' ').replace('\r', ' ').trim();
        if (m.length() > 500) {
            m = m.substring(0, 500) + "…";
        }
        return m;
    }
}
