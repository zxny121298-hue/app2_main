package com.example.demo.service;

import com.example.demo.common.BizAssert;
import com.example.demo.common.ErrorCodes;
import com.example.demo.config.AppProperties;
import com.example.demo.dto.AuthDtos;
import com.example.demo.dto.UserDtos;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.AuthPrincipal;
import com.example.demo.security.TokenService;
import java.time.LocalDateTime;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserAccessService userAccessService;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final AppProperties appProperties;

    public AuthService(UserRepository userRepository, UserAccessService userAccessService,
                       PasswordEncoder passwordEncoder, TokenService tokenService, AppProperties appProperties) {
        this.userRepository = userRepository;
        this.userAccessService = userAccessService;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.appProperties = appProperties;
    }

    @Transactional
    public AuthDtos.LoginResponse register(AuthDtos.RegisterRequest request) {
        BizAssert.isTrue(
            userRepository.findByUsername(request.username().trim()).isEmpty(),
            ErrorCodes.CONFLICT,
            "用户名已存在"
        );
        String nickname = request.nickname() == null || request.nickname().isBlank()
            ? request.username().trim()
            : request.nickname().trim();
        Long userId = userRepository.createUser(
            request.username().trim(),
            nickname,
            passwordEncoder.encode(request.password())
        );
        userRepository.updateLastLoginAt(userId, LocalDateTime.now());
        var user = userAccessService.requireById(userId);
        String token = tokenService.generateToken(
            new AuthPrincipal(user.id(), user.username(), user.role(), user.tokenVersion())
        );
        return new AuthDtos.LoginResponse(
            "Bearer",
            token,
            appProperties.getAuth().getExpiresDays(),
            ViewMapper.toProfileView(user)
        );
    }

    @Transactional
    public AuthDtos.LoginResponse login(AuthDtos.LoginRequest request) {
        var user = userRepository.findByUsername(request.username().trim())
            .orElseThrow(() -> new com.example.demo.common.BusinessException(ErrorCodes.BAD_REQUEST, "用户名或密码错误"));
        userAccessService.assertCanLogin(user);
        BizAssert.isTrue(
            passwordEncoder.matches(request.password(), user.passwordHash()),
            ErrorCodes.BAD_REQUEST,
            "用户名或密码错误"
        );
        userRepository.updateLastLoginAt(user.id(), LocalDateTime.now());
        var refreshed = userAccessService.requireById(user.id());
        String token = tokenService.generateToken(
            new AuthPrincipal(refreshed.id(), refreshed.username(), refreshed.role(), refreshed.tokenVersion())
        );
        return new AuthDtos.LoginResponse(
            "Bearer",
            token,
            appProperties.getAuth().getExpiresDays(),
            ViewMapper.toProfileView(refreshed)
        );
    }

    @Transactional
    public void changePassword(UserDtos.ChangePasswordRequest request) {
        var currentUser = userAccessService.requireCurrentUser();
        var lockedUser = userRepository.lockById(currentUser.id())
            .orElseThrow(() -> new com.example.demo.common.BusinessException(ErrorCodes.NOT_FOUND, "User not found"));

        BizAssert.isTrue(
            passwordEncoder.matches(request.oldPassword(), lockedUser.passwordHash()),
            ErrorCodes.BAD_REQUEST,
            "Current password is incorrect"
        );
        BizAssert.isTrue(
            !passwordEncoder.matches(request.newPassword(), lockedUser.passwordHash()),
            ErrorCodes.BAD_REQUEST,
            "New password must be different from the current password"
        );

        userRepository.updatePasswordAndIncrementTokenVersion(
            currentUser.id(),
            passwordEncoder.encode(request.newPassword())
        );
    }
}
