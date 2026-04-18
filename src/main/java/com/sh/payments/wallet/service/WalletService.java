package com.sh.payments.wallet.service;

import com.sh.payments.wallet.dto.TransferResponse;
import com.sh.payments.wallet.model.Wallet;

import java.math.BigDecimal;
import java.util.UUID;

public interface WalletService {
    Wallet createWalletForUser(UUID userId);

    Wallet deposit(UUID walletId, BigDecimal amount);       // ← ADD

    Wallet withdraw(UUID walletId, BigDecimal amount);      // ← ADD

    BigDecimal getBalance(UUID walletId);                   // ← ADD

    TransferResponse transfer(UUID fromWalletId, UUID toWalletId, BigDecimal amount); // ← ADD
}