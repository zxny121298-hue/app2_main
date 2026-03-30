package com.example.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 启动时检查 LLM 配置是否真正注入（不打印密钥内容，只打印长度）。
 * 用于排查：Windows「用户变量」已设，但 Cursor/IDE 启动的 Java 进程读不到环境变量 → SCNet 仍返回 401。
 */
@Component
@Order(100)
public class LlmEnvironmentDiagnostics implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LlmEnvironmentDiagnostics.class);

    private final AppProperties appProperties;

    public LlmEnvironmentDiagnostics(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        var llm = appProperties.getLlm();
        if (!llm.isEnabled()) {
            return;
        }
        String key = llm.getApiKey();
        int len = key == null ? 0 : key.trim().length();
        if (!StringUtils.hasText(key)) {
            log.warn("""
                forum.llm.enabled=true 但 api-key 为空。调用模型将失败（401 等）。
                处理办法（任选其一）：
                1) 在「运行后端的终端」里先执行: set FORUM_LLM_API_KEY=你的sk-密钥 再 mvn spring-boot:run
                2) 在 IDE/Cursor 的 Java 运行配置里手动添加环境变量 FORUM_LLM_API_KEY（不要只依赖 Windows 用户变量，部分启动方式读不到）
                3) 使用 application-local.yml 写入 api-key，并设置 spring.profiles.active=local
                """);
            return;
        }
        log.info("forum.llm 已加载：baseUrl={}，model={}，apiKey 长度={}（不记录明文）",
            llm.getBaseUrl(), llm.getModel(), len);
        if (len < 20) {
            log.warn("apiKey 长度过短，请确认是否复制完整。");
        }
        warnIfEnvOverridesYamlApiKey();
        String base = llm.getBaseUrl() == null ? "" : llm.getBaseUrl();
        if (base.contains("volces.com")) {
            log.info("火山方舟：若报「API key format is incorrect」且 curl 用 yml 里同一串 Key 却正常，优先检查下方环境变量是否覆盖了 yml。");
        }
    }

    /**
     * 这些变量在 Spring 中绑定到 forum.llm.api-key，优先级高于 application-local.yml。
     */
    private static void warnIfEnvOverridesYamlApiKey() {
        String[] names = {"FORUM_LLM_API_KEY", "LLM_API_KEY", "SCNET_API_KEY"};
        for (String n : names) {
            String v = System.getenv(n);
            if (StringUtils.hasText(v)) {
                log.warn(
                    "环境变量 {} 已设置（值长度={}），会覆盖 application-local.yml 中的 forum.llm.api-key。"
                        + " 若方舟返回 Key 格式错误，请核对该变量是否为控制台可用密钥，或在 PowerShell 中 Remove-Item Env:{} -ErrorAction SilentlyContinue 后重启后端。",
                    n, v.trim().length(), n);
            }
        }
    }
}
