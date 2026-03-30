package com.example.demo.controller;

import com.example.demo.common.ApiResponse;
import com.example.demo.dto.BoardDtos;
import com.example.demo.service.BoardService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class BoardController {

    private final BoardService boardService;

    public BoardController(BoardService boardService) {
        this.boardService = boardService;
    }

    @GetMapping("/boards")
    public ApiResponse<java.util.List<BoardDtos.BoardView>> listBoards(@RequestParam(defaultValue = "false") boolean includeDisabled) {
        return ApiResponse.ok(boardService.listBoards(includeDisabled));
    }

    @PostMapping("/admin/boards")
    public ApiResponse<BoardDtos.BoardView> createBoard(@Valid @RequestBody BoardDtos.UpsertBoardRequest request) {
        return ApiResponse.ok(boardService.createBoard(request));
    }

    @PutMapping("/admin/boards/{boardId}")
    public ApiResponse<BoardDtos.BoardView> updateBoard(@PathVariable Long boardId, @Valid @RequestBody BoardDtos.UpsertBoardRequest request) {
        return ApiResponse.ok(boardService.updateBoard(boardId, request));
    }

    @PutMapping("/admin/boards/{boardId}/status")
    public ApiResponse<Void> updateBoardStatus(@PathVariable Long boardId, @Valid @RequestBody BoardDtos.UpdateBoardStatusRequest request) {
        boardService.updateStatus(boardId, request);
        return ApiResponse.ok();
    }
}
