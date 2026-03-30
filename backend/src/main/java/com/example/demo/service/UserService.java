package com.example.demo.service;

import com.example.demo.common.BizAssert;
import com.example.demo.common.ErrorCodes;
import com.example.demo.dto.UserDtos;
import com.example.demo.model.ForumEnums.UserRole;
import com.example.demo.model.ForumEnums.UserStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserAccessService userAccessService;
    private final com.example.demo.repository.UserRepository userRepository;

    public UserService(UserAccessService userAccessService, com.example.demo.repository.UserRepository userRepository) {
        this.userAccessService = userAccessService;
        this.userRepository = userRepository;
    }

    public UserDtos.UserProfileView getCurrentProfile() {
        return ViewMapper.toProfileView(userAccessService.requireCurrentUser());
    }

    public UserDtos.UserProfileView getUserProfile(Long userId) {
        return ViewMapper.toProfileView(userAccessService.requireById(userId));
    }

    public com.example.demo.common.PageResult<UserDtos.UserProfileView> pageAdminUsers(String keyword, String status,
                                                                                       String role, long page,
                                                                                       long pageSize) {
        userAccessService.requireAdmin();
        var rows = userRepository.pageAdminUsers(
            trimToNull(keyword),
            normalizeStatus(status),
            normalizeRole(role),
            page,
            pageSize
        );
        return new com.example.demo.common.PageResult<>(
            rows.total(),
            rows.page(),
            rows.pageSize(),
            rows.list().stream().map(ViewMapper::toProfileView).toList()
        );
    }

    public com.example.demo.common.PageResult<UserDtos.UserProfileView> pageSearchUsers(String keyword, long page, long pageSize) {
        var rows = userRepository.pageSearchUsers(keyword, page, pageSize);
        return new com.example.demo.common.PageResult<>(
            rows.total(),
            rows.page(),
            rows.pageSize(),
            rows.list().stream().map(ViewMapper::toProfileView).toList()
        );
    }

    @Transactional
    public UserDtos.UserProfileView updateCurrentProfile(UserDtos.UpdateProfileRequest request) {
        var currentUser = userAccessService.requireCurrentUser();
        userAccessService.assertCanOperate(currentUser);
        userRepository.updateProfile(
            currentUser.id(),
            trimToNull(request.nickname()),
            trimToNull(request.avatarUrl()),
            trimToNull(request.bio())
        );
        return ViewMapper.toProfileView(userAccessService.requireById(currentUser.id()));
    }

    @Transactional
    public void banUser(Long userId, UserDtos.AdminBanRequest request) {
        userAccessService.requireAdmin();
        userAccessService.requireById(userId);
        userRepository.banUser(userId, request.bannedUntilAt(), trimToNull(request.reason()));
    }

    @Transactional
    public void unbanUser(Long userId) {
        userAccessService.requireAdmin();
        userAccessService.requireById(userId);
        userRepository.unbanUser(userId);
    }

    @Transactional
    public void muteUser(Long userId, UserDtos.AdminMuteRequest request) {
        userAccessService.requireAdmin();
        userAccessService.requireById(userId);
        userRepository.muteUser(userId, request.mutedUntilAt(), trimToNull(request.reason()));
    }

    @Transactional
    public void unmuteUser(Long userId) {
        userAccessService.requireAdmin();
        userAccessService.requireById(userId);
        userRepository.unmuteUser(userId);
    }

    private String normalizeStatus(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        BizAssert.isTrue(
            UserStatus.ACTIVE.value().equals(normalized) || UserStatus.BANNED.value().equals(normalized),
            ErrorCodes.BAD_REQUEST,
            "用户状态不合法"
        );
        return normalized;
    }

    private String normalizeRole(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        BizAssert.isTrue(
            UserRole.ADMIN.value().equals(normalized) || UserRole.USER.value().equals(normalized),
            ErrorCodes.BAD_REQUEST,
            "用户角色不合法"
        );
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
