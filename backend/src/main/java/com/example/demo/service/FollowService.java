package com.example.demo.service;

import com.example.demo.common.BizAssert;
import com.example.demo.common.ErrorCodes;
import com.example.demo.dto.FollowDtos;
import com.example.demo.repository.SocialRepository;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FollowService {

    private final SocialRepository socialRepository;
    private final UserAccessService userAccessService;

    public FollowService(SocialRepository socialRepository, UserAccessService userAccessService) {
        this.socialRepository = socialRepository;
        this.userAccessService = userAccessService;
    }

    @Transactional
    public void follow(Long targetUserId) {
        var currentUser = userAccessService.requireCurrentUser();
        userAccessService.assertCanOperate(currentUser);
        BizAssert.isTrue(!currentUser.id().equals(targetUserId), ErrorCodes.BAD_REQUEST, "不能关注自己");
        userAccessService.requireById(targetUserId);
        BizAssert.isTrue(!socialRepository.existsFollow(currentUser.id(), targetUserId), ErrorCodes.CONFLICT, "已关注该用户");
        socialRepository.createFollow(currentUser.id(), targetUserId);
    }

    @Transactional
    public void unfollow(Long targetUserId) {
        var currentUser = userAccessService.requireCurrentUser();
        userAccessService.assertCanOperate(currentUser);
        socialRepository.deleteFollow(currentUser.id(), targetUserId);
    }

    public com.example.demo.common.PageResult<FollowDtos.FollowRelationView> pageMyFollows(long page, long pageSize) {
        var currentUser = userAccessService.requireCurrentUser();
        var rows = socialRepository.pageFollowees(currentUser.id(), page, pageSize);
        return new com.example.demo.common.PageResult<>(
            socialRepository.countFollowees(currentUser.id()),
            page,
            pageSize,
            rows.stream().map(row -> ViewMapper.toFollowRelationView(row, true)).toList()
        );
    }

    public com.example.demo.common.PageResult<FollowDtos.FollowRelationView> pageMyFans(long page, long pageSize) {
        var currentUser = userAccessService.requireCurrentUser();
        var rows = socialRepository.pageFollowers(currentUser.id(), page, pageSize);
        Set<Long> followBackIds = socialRepository.findFolloweeIdsIn(currentUser.id(), rows.stream().map(SocialRepository.FollowRow::id).toList());
        return new com.example.demo.common.PageResult<>(
            socialRepository.countFollowers(currentUser.id()),
            page,
            pageSize,
            rows.stream().map(row -> ViewMapper.toFollowRelationView(row, followBackIds.contains(row.id()))).toList()
        );
    }

    public FollowDtos.FollowStatusView isFollowing(Long targetUserId) {
        var currentUser = userAccessService.requireCurrentUser();
        return new FollowDtos.FollowStatusView(targetUserId, socialRepository.existsFollow(currentUser.id(), targetUserId));
    }
}
