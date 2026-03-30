package com.example.demo.service;

import com.example.demo.common.BizAssert;
import com.example.demo.common.BusinessException;
import com.example.demo.common.ErrorCodes;
import com.example.demo.config.AppProperties;
import com.example.demo.dto.UserDtos;
import com.example.demo.model.ForumEnums.ExpChangeType;
import com.example.demo.model.ForumModels.LevelRuleRecord;
import com.example.demo.model.ForumModels.UserAccount;
import com.example.demo.repository.ExperienceRepository;
import com.example.demo.repository.UserRepository;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExperienceService {

    private final ExperienceRepository experienceRepository;
    private final UserRepository userRepository;
    private final UserAccessService userAccessService;
    private final AppProperties appProperties;
    private final CoinService coinService;

    public ExperienceService(ExperienceRepository experienceRepository, UserRepository userRepository,
                             UserAccessService userAccessService, AppProperties appProperties, CoinService coinService) {
        this.experienceRepository = experienceRepository;
        this.userRepository = userRepository;
        this.userAccessService = userAccessService;
        this.appProperties = appProperties;
        this.coinService = coinService;
    }

    @Transactional
    public UserDtos.CheckInView checkIn() {
        UserAccount currentUser = userAccessService.requireCurrentUser();
        userAccessService.assertCanOperate(currentUser);
        LocalDate today = LocalDate.now();
        BizAssert.isTrue(!experienceRepository.hasCheckedIn(currentUser.id(), today), ErrorCodes.CONFLICT, "今天已经签到过了");
        int expGain = appProperties.getExp().getSignIn();
        long coinGain = appProperties.getCoin().getSignIn();
        experienceRepository.createCheckIn(currentUser.id(), today, expGain);
        ExpResult result = grantExpInternal(currentUser.id(), ExpChangeType.SIGN_IN, expGain, null, null, null, "每日签到");
        coinService.grantSignInReward(currentUser.id(), coinGain);
        var checkIn = experienceRepository.findTodayCheckIn(currentUser.id(), today)
            .orElseThrow(() -> new BusinessException(ErrorCodes.SERVER_ERROR, "签到记录写入失败"));
        return new UserDtos.CheckInView(true, checkIn.createdAt(), expGain, coinGain, result.level(), result.totalExp(), result.currentLevelExp());
    }

    public UserDtos.CheckInView getTodayStatus() {
        UserAccount currentUser = userAccessService.requireCurrentUser();
        currentUser = userAccessService.normalize(currentUser);
        var checkIn = experienceRepository.findTodayCheckIn(currentUser.id(), LocalDate.now()).orElse(null);
        return new UserDtos.CheckInView(
            checkIn != null,
            checkIn == null ? null : checkIn.createdAt(),
            checkIn == null ? 0 : checkIn.expGain(),
            checkIn == null ? 0L : appProperties.getCoin().getSignIn(),
            currentUser.level(),
            currentUser.totalExp(),
            currentUser.currentLevelExp()
        );
    }

    public com.example.demo.common.PageResult<UserDtos.ExpLogView> pageMyExpLogs(long page, long pageSize) {
        var currentUser = userAccessService.requireCurrentUser();
        var result = experienceRepository.pageExpLogs(currentUser.id(), page, pageSize);
        return new com.example.demo.common.PageResult<>(
            result.total(),
            result.page(),
            result.pageSize(),
            result.list().stream().map(item -> new UserDtos.ExpLogView(
                item.id(),
                item.changeType(),
                item.changeExp(),
                item.totalExpAfter(),
                item.levelAfter(),
                item.postId(),
                item.commentId(),
                item.operatorUserId(),
                item.remark(),
                item.createdAt()
            )).toList()
        );
    }

    @Transactional
    public UserDtos.UserProfileView adminAdjustExp(Long userId, UserDtos.AdminAdjustExpRequest request) {
        var admin = userAccessService.requireAdmin();
        BizAssert.isTrue(request.changeExp() != 0, ErrorCodes.BAD_REQUEST, "经验调整值不能为 0");
        grantExpInternal(userId, ExpChangeType.ADMIN_ADJUST, request.changeExp(), null, null, admin.id(), request.remark());
        return ViewMapper.toProfileView(userAccessService.requireById(userId));
    }

    @Transactional
    public ExpResult grantExp(Long userId, ExpChangeType changeType, int exp, Long postId, Long commentId,
                              Long operatorUserId, String remark) {
        return grantExpInternal(userId, changeType, exp, postId, commentId, operatorUserId, remark);
    }

    public boolean hasPostExpLog(Long postId, ExpChangeType changeType) {
        return experienceRepository.existsPostExpLog(postId, changeType.value());
    }

    public int expValueFor(ExpChangeType changeType) {
        return switch (changeType) {
            case SIGN_IN -> appProperties.getExp().getSignIn();
            case CREATE_POST -> appProperties.getExp().getCreatePost();
            case CREATE_COMMENT -> appProperties.getExp().getCreateComment();
            case CREATE_REPLY -> appProperties.getExp().getCreateReply();
            case POST_PINNED -> appProperties.getExp().getPostPinned();
            case POST_FEATURED -> appProperties.getExp().getPostFeatured();
            case ADMIN_ADJUST -> 0;
        };
    }

    private ExpResult grantExpInternal(Long userId, ExpChangeType changeType, int exp, Long postId, Long commentId,
                                       Long operatorUserId, String remark) {
        var lockedUser = userRepository.lockById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "用户不存在"));
        int newTotalExp = Math.max(lockedUser.totalExp() + exp, 0);
        LevelProgress progress = calculateLevelProgress(newTotalExp);
        userRepository.updateExp(userId, newTotalExp, progress.currentLevelExp(), progress.level());
        experienceRepository.createExpLog(userId, changeType.value(), exp, newTotalExp, progress.level(),
            postId, commentId, operatorUserId, remark);
        return new ExpResult(progress.level(), newTotalExp, progress.currentLevelExp());
    }

    private LevelProgress calculateLevelProgress(int totalExp) {
        List<LevelRuleRecord> rules = experienceRepository.listLevelRules().stream()
            .sorted(Comparator.comparing(LevelRuleRecord::level))
            .toList();
        int level = 1;
        int currentLevelExp = totalExp;
        for (LevelRuleRecord rule : rules) {
            if (rule.level() >= 6) {
                break;
            }
            if (currentLevelExp >= rule.upgradeNeedExp()) {
                currentLevelExp -= rule.upgradeNeedExp();
                level = rule.level() + 1;
            } else {
                break;
            }
        }
        return new LevelProgress(level, currentLevelExp);
    }

    private record LevelProgress(int level, int currentLevelExp) {
    }

    public record ExpResult(int level, int totalExp, int currentLevelExp) {
    }
}
