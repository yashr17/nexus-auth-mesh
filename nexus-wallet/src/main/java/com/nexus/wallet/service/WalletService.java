package com.nexus.wallet.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nexus.common.exception.DuplicateResourceException;
import com.nexus.common.exception.ResourceNotFoundException;
import com.nexus.common.exception.TransactionException;
import com.nexus.wallet.entity.Transaction;
import com.nexus.wallet.entity.TransactionStatus;
import com.nexus.wallet.entity.Wallet;
import com.nexus.wallet.repository.TransactionRepository;
import com.nexus.wallet.repository.WalletRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    
    @Transactional
    public Wallet createWallet(String userId) {
        walletRepository.findByUserId(userId).ifPresent(w -> {
            throw new DuplicateResourceException("Wallet already exists for user: " + userId, HttpStatus.BAD_REQUEST);
        });

       Wallet wallet = Wallet.builder()
            .userId(userId)
            .balance(BigDecimal.valueOf(100))
            .build();
        return walletRepository.save(wallet);
    }   

    public Wallet getWallet(String userId) {
        return walletRepository.findByUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for user: " + userId));
    }

    public BigDecimal getBalance(String userId) {
        Wallet wallet = getWallet(userId);
        return wallet.getBalance();
    }

    @Transactional
    public Transaction transferFunds(String senderId, String receiverId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new TransactionException("Transfer amount must be greater than zero", HttpStatus.BAD_REQUEST);
        }

        if (senderId.equals(receiverId)) {
            throw new TransactionException("Self transfers are not allowed", HttpStatus.BAD_REQUEST);
        }

       Wallet sender = getWallet(senderId);
       Wallet receiver = getWallet(receiverId);

        if (sender.getBalance().compareTo(amount) < 0) {
            throw new TransactionException("Insufficient funds in wallet", HttpStatus.BAD_REQUEST);
        }

        sender.setBalance(sender.getBalance().subtract(amount));
        receiver.setBalance(receiver.getBalance().add(amount));

        walletRepository.save(sender);
        walletRepository.save(receiver);

        Transaction transaction = Transaction.builder()
            .senderId(senderId)
            .receiverId(receiverId)
            .amount(amount)
            .timestamp(LocalDateTime.now())
            .status(TransactionStatus.SUCCESS)
            .build();
        return transactionRepository.save(transaction);
    }
}
