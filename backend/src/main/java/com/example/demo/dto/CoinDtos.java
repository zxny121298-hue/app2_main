package com.example.demo.dto;

import java.time.LocalDateTime;

public final class CoinDtos {

    private CoinDtos() {
    }

    public record CoinBalanceView(Long coinBalance) {
    }

    public record CoinLedgerView(
        Long id,
        String changeType,
        Long changeAmount,
        Long balanceAfter,
        Long relatedUserId,
        Long rewardId,
        String description,
        LocalDateTime createdAt
    ) {
    }
}
