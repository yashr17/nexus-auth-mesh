package com.nexus.wallet.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nexus.wallet.entity.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findBySenderIdOrReceiverIdOrderByTimestampDesc(String senderId, String receiverId);
}
