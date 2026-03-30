package com.example.demo.service;

import com.example.demo.common.BizAssert;
import com.example.demo.common.BusinessException;
import com.example.demo.common.ErrorCodes;
import com.example.demo.model.ForumEnums.UserRole;
import com.example.demo.model.ForumEnums.UserStatus;
import com.example.demo.model.ForumModels.UserAccount;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.AuthPrincipal;
import com.example.demo.security.SecurityUtils;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class UserAccessService {

    private final UserRepository userRepository;

    public UserAccessService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserAccount requireCurrentUser() {
        AuthPrincipal principal = SecurityUtils.requireUser();
        return normalizeAndRequire(principal.userId());
    }

    public UserAccount requireAdmin() {
        UserAccount currentUser = requireCurrentUser();
        BizAssert.isTrue(UserRole.ADMIN.value().equals(currentUser.role()), ErrorCodes.FORBIDDEN, "仅管理员可操作");
        return currentUser;
    }

    public UserAccount requireById(Long userId) {
        return normalizeAndRequire(userId);
    }

    public void assertCanLogin(UserAccount user) {
        UserAccount normalized = normalize(user);
        if (isBanned(normalized)) {
            throw new BusinessException(ErrorCodes.FORBIDDEN, "账号已被封禁");
        }
    }

    public void assertCanOperate(UserAccount user) {
        UserAccount normalized = normalize(user);
        if (isBanned(normalized)) {
            throw new BusinessException(ErrorCodes.FORBIDDEN, "账号已被封禁，无法执行该操作");
        }
    }

    public void assertCanSpeak(UserAccount user) {
        UserAccount normalized = normalize(user);
        if (isBanned(normalized)) {
            throw new BusinessException(ErrorCodes.FORBIDDEN, "账号已被封禁，无法执行该操作");
        }
        if (isMuted(normalized)) {
            throw new BusinessException(ErrorCodes.FORBIDDEN, "账号已被禁言，无法执行该操作");
        }
    }

    public boolean isBanned(UserAccount user) {
        return UserStatus.BANNED.value().equals(user.status())
            && (user.bannedUntilAt() == null || user.bannedUntilAt().isAfter(LocalDateTime.now()));
    }

    public boolean isMuted(UserAccount user) {
        return user.mutedUntilAt() != null && user.mutedUntilAt().isAfter(LocalDateTime.now());
    }

    public UserAccount normalize(UserAccount user) {
        if (user == null) {
            throw new BusinessException(ErrorCodes.NOT_FOUND, "用户不存在");
        }
        boolean changed = false;
        LocalDateTime now = LocalDateTime.now();
        if (UserStatus.BANNED.value().equals(user.status()) && user.bannedUntilAt() != null && !user.bannedUntilAt().isAfter(now)) {
            userRepository.unbanUser(user.id());
            changed = true;
        }
        if (user.mutedUntilAt() != null && !user.mutedUntilAt().isAfter(now)) {
            userRepository.unmuteUser(user.id());
            changed = true;
        }
        if (changed) {
            return userRepository.findById(user.id())
                .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "用户不存在"));
        }
        return user;
    }

    private UserAccount normalizeAndRequire(Long userId) {
        UserAccount user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "用户不存在"));
        return normalize(user);
    }
}
