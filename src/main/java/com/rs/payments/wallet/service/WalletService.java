package com.rs.payments.wallet.service;

import com.rs.payments.wallet.model.Wallet;

import java.math.BigDecimal;
import java.util.UUID;

public interface WalletService {
    Wallet createWalletForUser(UUID userId);

    Wallet deposit(UUID walletId, BigDecimal amount);       // ← ADD

    Wallet withdraw(UUID walletId, BigDecimal amount);      // ← ADD

    BigDecimal getBalance(UUID walletId);                   // ← ADD
}