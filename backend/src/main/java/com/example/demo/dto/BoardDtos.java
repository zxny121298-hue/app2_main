package com.example.demo.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public final class BoardDtos {

    private BoardDtos() {
    }

    public record UpsertBoardRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 255) String description,
        @NotNull @Min(0) @Max(999999) Integer sortOrder,
        @NotBlank String status
    ) {
    }

    public record UpdateBoardStatusRequest(@NotBlank String status) {
    }

    public record BoardView(
        Long id,
        String name,
        String description,
        Integer sortOrder,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
    }
}
