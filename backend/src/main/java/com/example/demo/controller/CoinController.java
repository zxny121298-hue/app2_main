package com.example.demo.controller;

import com.example.demo.common.ApiResponse;
import com.example.demo.dto.CoinDtos;
import com.example.demo.service.CoinService;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/coins")
public class CoinController {

    private final CoinService coinService;

    public CoinController(CoinService coinService) {
        this.coinService = coinService;
    }

    @GetMapping("/balance")
    public ApiResponse<CoinDtos.CoinBalanceView> getBalance() {
        return ApiResponse.ok(coinService.getCurrentBalance());
    }

    @GetMapping("/ledgers")
    public ApiResponse<com.example.demo.common.PageResult<CoinDtos.CoinLedgerView>> pageLedgers(
        @RequestParam(defaultValue = "1") @Min(1) long page,
        @RequestParam(defaultValue = "20") @Min(1) long pageSize
    ) {
        return ApiResponse.ok(coinService.pageLedgers(page, pageSize));
    }
}
