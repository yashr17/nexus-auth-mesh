package com.nexus.wallet.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nexus.wallet.dto.TransferRequest;
import com.nexus.wallet.entity.Transaction;
import com.nexus.wallet.entity.Wallet;
import com.nexus.wallet.service.WalletService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/wallet")
public class WalletController {

    private final WalletService walletService;

    @PostMapping("/create")
    public ResponseEntity<Wallet> createWallet(@RequestHeader("X-User-Id") String userId) {
        Wallet wallet = walletService.createWallet(userId);
        return ResponseEntity.ok(wallet);
    }

    @GetMapping("/balance")
    public ResponseEntity<Wallet> getWalletBalance(@RequestHeader("X-User-Id") String userId) {
        Wallet wallet = walletService.getWallet(userId);
        return ResponseEntity.ok(wallet);
    }

    @PostMapping("/transfer")
    public ResponseEntity<Transaction> transferFunds(
            @RequestHeader("X-User-Id") String senderId, @RequestBody TransferRequest transferRequest) {
        Transaction transaction = walletService.transferFunds(senderId, transferRequest.getTo(),
                transferRequest.getAmount());
        return ResponseEntity.ok(transaction);
    }
}
