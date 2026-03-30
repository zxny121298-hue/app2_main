package com.example.demo.model;

import java.util.Arrays;

public final class ForumEnums {

    private ForumEnums() {
    }

    public interface DbValueEnum {
        String value();
    }

    public enum UserRole implements DbValueEnum {
        ADMIN("admin"),
        USER("user");

        private final String value;

        UserRole(String value) {
            this.value = value;
        }

        @Override
        public String value() {
            return value;
        }
    }

    public enum UserStatus implements DbValueEnum {
        ACTIVE("active"),
        BANNED("banned");

        private final String value;

        UserStatus(String value) {
            this.value = value;
        }

        @Override
        public String value() {
            return value;
        }
    }

    public enum BoardStatus implements DbValueEnum {
        ENABLED("enabled"),
        DISABLED("disabled");

        private final String value;

        BoardStatus(String value) {
            this.value = value;
        }

        @Override
        public String value() {
            return value;
        }
    }

    public enum PostStatus implements DbValueEnum {
        NORMAL("normal"),
        DELETED("deleted");

        private final String value;

        PostStatus(String value) {
            this.value = value;
        }

        @Override
        public String value() {
            return value;
        }
    }

    public enum NotificationEventType implements DbValueEnum {
        PRIVATE_MESSAGE("private_message"),
        POST_COMMENT("post_comment"),
        COMMENT_REPLY("comment_reply"),
        POST_LIKE("post_like"),
        COMMENT_LIKE("comment_like"),
        POST_REWARD("post_reward"),
        COMMENT_REWARD("comment_reward");

        private final String value;

        NotificationEventType(String value) {
            this.value = value;
        }

        @Override
        public String value() {
            return value;
        }
    }

    public enum NotificationTargetType implements DbValueEnum {
        CONVERSATION("conversation"),
        POST("post"),
        COMMENT("comment");

        private final String value;

        NotificationTargetType(String value) {
            this.value = value;
        }

        @Override
        public String value() {
            return value;
        }
    }

    public enum CoinChangeType implements DbValueEnum {
        SIGN_IN("sign_in"),
        REWARD_SEND("reward_send"),
        REWARD_RECEIVE("reward_receive"),
        ADMIN_ADJUST("admin_adjust"),
        SYSTEM_GRANT("system_grant"),
        SYSTEM_DEDUCT("system_deduct"),
        REFUND("refund");

        private final String value;

        CoinChangeType(String value) {
            this.value = value;
        }

        @Override
        public String value() {
            return value;
        }
    }

    public enum ExpChangeType implements DbValueEnum {
        SIGN_IN("sign_in"),
        CREATE_POST("create_post"),
        CREATE_COMMENT("create_comment"),
        CREATE_REPLY("create_reply"),
        POST_PINNED("post_pinned"),
        POST_FEATURED("post_featured"),
        ADMIN_ADJUST("admin_adjust");

        private final String value;

        ExpChangeType(String value) {
            this.value = value;
        }

        @Override
        public String value() {
            return value;
        }
    }

    public static <T extends Enum<T> & DbValueEnum> T fromValue(Class<T> enumType, String value) {
        return Arrays.stream(enumType.getEnumConstants())
            .filter(item -> item.value().equals(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("不支持的枚举值: " + value));
    }
}
