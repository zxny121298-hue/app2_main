package com.example.demo.common;

import java.util.List;

public final class RequestValidator {

    private RequestValidator() {
    }

    public static void requireContentOrImages(String contentText, List<String> imageUrls, String message) {
        boolean hasText = contentText != null && !contentText.isBlank();
        boolean hasImages = imageUrls != null && imageUrls.stream().anyMatch(url -> url != null && !url.isBlank());
        BizAssert.isTrue(hasText || hasImages, ErrorCodes.BAD_REQUEST, message);
    }
}
