package com.example.demo.security;

import com.example.demo.common.BusinessException;
import com.example.demo.common.ErrorCodes;
import com.example.demo.model.ForumEnums.UserRole;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static AuthPrincipal getCurrentUserOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthPrincipal authPrincipal) {
            return authPrincipal;
        }
        return null;
    }

    public static AuthPrincipal requireUser() {
        AuthPrincipal principal = getCurrentUserOrNull();
        if (principal == null) {
            throw new BusinessException(ErrorCodes.UNAUTHORIZED, "请先登录");
        }
        return principal;
    }

    public static AuthPrincipal requireAdmin() {
        AuthPrincipal principal = requireUser();
        if (!UserRole.ADMIN.value().equals(principal.role())) {
            throw new BusinessException(ErrorCodes.FORBIDDEN, "仅管理员可操作");
        }
        return principal;
    }
}
