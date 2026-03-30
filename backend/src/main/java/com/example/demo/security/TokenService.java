package com.example.demo.security;

import com.example.demo.common.BusinessException;
import com.example.demo.common.ErrorCodes;
import com.example.demo.config.AppProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class TokenService {

    private static final String HEADER_JSON = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";

    private final ObjectMapper objectMapper;
    private final AppProperties properties;

    public TokenService(ObjectMapper objectMapper, AppProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public String generateToken(AuthPrincipal principal) {
        try {
            String header = base64Url(HEADER_JSON.getBytes(StandardCharsets.UTF_8));
            long exp = Instant.now().plus(properties.getAuth().getExpiresDays(), ChronoUnit.DAYS).getEpochSecond();
            String payloadJson = objectMapper.writeValueAsString(Map.of(
                "userId", principal.userId(),
                "username", principal.username(),
                "role", principal.role(),
                "tokenVersion", principal.tokenVersion(),
                "exp", exp
            ));
            String payload = base64Url(payloadJson.getBytes(StandardCharsets.UTF_8));
            String signature = sign(header + "." + payload);
            return header + "." + payload + "." + signature;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCodes.SERVER_ERROR, "生成登录令牌失败");
        }
    }

    public TokenPayload parseToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new BusinessException(ErrorCodes.UNAUTHORIZED, "无效登录令牌");
            }
            String content = parts[0] + "." + parts[1];
            String expectedSignature = sign(content);
            if (!expectedSignature.equals(parts[2])) {
                throw new BusinessException(ErrorCodes.UNAUTHORIZED, "登录令牌签名无效");
            }
            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            Map<String, Object> payload = objectMapper.readValue(payloadBytes, new TypeReference<>() {
            });
            long exp = ((Number) payload.get("exp")).longValue();
            int tokenVersion = payload.get("tokenVersion") == null
                ? 0
                : ((Number) payload.get("tokenVersion")).intValue();
            if (Instant.now().getEpochSecond() > exp) {
                throw new BusinessException(ErrorCodes.UNAUTHORIZED, "登录令牌已过期");
            }
            return new TokenPayload(
                ((Number) payload.get("userId")).longValue(),
                String.valueOf(payload.get("username")),
                String.valueOf(payload.get("role")),
                tokenVersion,
                exp
            );
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCodes.UNAUTHORIZED, "无效登录令牌");
        }
    }

    private String sign(String content) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(properties.getAuth().getSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return base64Url(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
    }

    private String base64Url(byte[] content) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(content);
    }
}
