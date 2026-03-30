package com.example.demo.service;

import com.example.demo.common.BizAssert;
import com.example.demo.common.ErrorCodes;
import com.example.demo.common.PageResult;
import com.example.demo.dto.BoardDtos;
import com.example.demo.model.ForumEnums.BoardStatus;
import com.example.demo.repository.BoardRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BoardService {

    private final BoardRepository boardRepository;
    private final UserAccessService userAccessService;

    public BoardService(BoardRepository boardRepository, UserAccessService userAccessService) {
        this.boardRepository = boardRepository;
        this.userAccessService = userAccessService;
    }

    public List<BoardDtos.BoardView> listBoards(boolean includeDisabled) {
        if (includeDisabled) {
            userAccessService.requireAdmin();
        }
        return boardRepository.listBoards(includeDisabled).stream()
            .map(item -> new BoardDtos.BoardView(item.id(), item.name(), item.description(), item.sortOrder(), item.status(), item.createdAt(), item.updatedAt()))
            .toList();
    }

    public PageResult<BoardDtos.BoardView> pageSearchBoards(String keyword, long page, long pageSize) {
        var rows = boardRepository.pageSearchBoards(keyword, page, pageSize);
        return new PageResult<>(
            rows.total(),
            rows.page(),
            rows.pageSize(),
            rows.list().stream()
                .map(item -> new BoardDtos.BoardView(item.id(), item.name(), item.description(), item.sortOrder(), item.status(), item.createdAt(), item.updatedAt()))
                .toList()
        );
    }

    @Transactional
    public BoardDtos.BoardView createBoard(BoardDtos.UpsertBoardRequest request) {
        userAccessService.requireAdmin();
        validateStatus(request.status());
        BizAssert.isTrue(!boardRepository.existsByName(request.name().trim()), ErrorCodes.CONFLICT, "板块名称已存在");
        Long id = boardRepository.createBoard(request.name().trim(), trimToNull(request.description()), request.sortOrder(), request.status());
        var board = boardRepository.findById(id).orElseThrow();
        return new BoardDtos.BoardView(board.id(), board.name(), board.description(), board.sortOrder(), board.status(), board.createdAt(), board.updatedAt());
    }

    @Transactional
    public BoardDtos.BoardView updateBoard(Long boardId, BoardDtos.UpsertBoardRequest request) {
        userAccessService.requireAdmin();
        validateStatus(request.status());
        boardRepository.findById(boardId).orElseThrow(() -> new com.example.demo.common.BusinessException(ErrorCodes.NOT_FOUND, "板块不存在"));
        BizAssert.isTrue(!boardRepository.existsByNameExcludingId(request.name().trim(), boardId), ErrorCodes.CONFLICT, "板块名称已存在");
        boardRepository.updateBoard(boardId, request.name().trim(), trimToNull(request.description()), request.sortOrder(), request.status());
        var board = boardRepository.findById(boardId).orElseThrow();
        return new BoardDtos.BoardView(board.id(), board.name(), board.description(), board.sortOrder(), board.status(), board.createdAt(), board.updatedAt());
    }

    @Transactional
    public void updateStatus(Long boardId, BoardDtos.UpdateBoardStatusRequest request) {
        userAccessService.requireAdmin();
        validateStatus(request.status());
        boardRepository.findById(boardId).orElseThrow(() -> new com.example.demo.common.BusinessException(ErrorCodes.NOT_FOUND, "板块不存在"));
        boardRepository.updateStatus(boardId, request.status());
    }

    private void validateStatus(String status) {
        BizAssert.isTrue(
            BoardStatus.ENABLED.value().equals(status) || BoardStatus.DISABLED.value().equals(status),
            ErrorCodes.BAD_REQUEST,
            "板块状态不合法"
        );
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
