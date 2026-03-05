package com.nexus.wallet.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class TransferRequest {
    private String to;
    private BigDecimal amount;
}
