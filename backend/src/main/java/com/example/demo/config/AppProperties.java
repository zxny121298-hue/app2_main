package com.example.demo.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "forum")
public class AppProperties {

    private final Auth auth = new Auth();
    private final Exp exp = new Exp();
    private final Coin coin = new Coin();
    private final Llm llm = new Llm();

    public Auth getAuth() {
        return auth;
    }

    public Exp getExp() {
        return exp;
    }

    public Coin getCoin() {
        return coin;
    }

    public Llm getLlm() {
        return llm;
    }

    public static class Auth {

        private String secret = "replace-with-your-own-secret";
        private long expiresDays = 30;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public long getExpiresDays() {
            return expiresDays;
        }

        public void setExpiresDays(long expiresDays) {
            this.expiresDays = expiresDays;
        }
    }

    public static class Exp {

        private int signIn = 5;
        private int createPost = 10;
        private int createComment = 5;
        private int createReply = 5;
        private int postPinned = 20;
        private int postFeatured = 50;

        public int getSignIn() {
            return signIn;
        }

        public void setSignIn(int signIn) {
            this.signIn = signIn;
        }

        public int getCreatePost() {
            return createPost;
        }

        public void setCreatePost(int createPost) {
            this.createPost = createPost;
        }

        public int getCreateComment() {
            return createComment;
        }

        public void setCreateComment(int createComment) {
            this.createComment = createComment;
        }

        public int getCreateReply() {
            return createReply;
        }

        public void setCreateReply(int createReply) {
            this.createReply = createReply;
        }

        public int getPostPinned() {
            return postPinned;
        }

        public void setPostPinned(int postPinned) {
            this.postPinned = postPinned;
        }

        public int getPostFeatured() {
            return postFeatured;
        }

        public void setPostFeatured(int postFeatured) {
            this.postFeatured = postFeatured;
        }
    }

    public static class Coin {

        private long signIn = 5;

        public long getSignIn() {
            return signIn;
        }

        public void setSignIn(long signIn) {
            this.signIn = signIn;
        }
    }

    /**
     * SCNet 等平台提供的 OpenAI 兼容 Chat Completions，文档见
     * https://www.scnet.cn/ac/openapi/doc/2.0/moduleapi/tutorial/apicall.html
     * 使用 OpenAI Java SDK 时 baseUrl 不要含路径末尾的 /v1，由 SDK 自动拼接。
     */
    public static class Llm {

        private boolean enabled = false;
        private String apiKey = "";
        private String baseUrl = "https://api.scnet.cn/api/llm";
        private String model = "DeepSeek-R1-Distill-Qwen-7B";
        private Duration connectTimeout = Duration.ofSeconds(15);
        private Duration readTimeout = Duration.ofSeconds(90);
        /**
         * 为 true 时 OkHttp 使用 Proxy.NO_PROXY，忽略 JVM/系统的 HTTP(S)/SOCKS 代理。
         * 堆栈若出现 SocksSocketImpl 且 Connect timed out、而 curl 正常，可设为 true。
         */
        private boolean directConnection = false;

        public boolean isDirectConnection() {
            return directConnection;
        }

        public void setDirectConnection(boolean directConnection) {
            this.directConnection = directConnection;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public Duration getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public Duration getReadTimeout() {
            return readTimeout;
        }

        public void setReadTimeout(Duration readTimeout) {
            this.readTimeout = readTimeout;
        }
    }
}
