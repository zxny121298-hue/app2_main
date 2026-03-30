package com.example.demo.security;

public record TokenPayload(Long userId, String username, String role, Integer tokenVersion, long exp) {
}
