package com.rs.payments.wallet.service.impl;

import com.rs.payments.wallet.exception.BadRequestException;
import com.rs.payments.wallet.exception.InsufficientFundsException;
import com.rs.payments.wallet.exception.ResourceNotFoundException;
import com.rs.payments.wallet.model.Transaction;
import com.rs.payments.wallet.model.TransactionType;
import com.rs.payments.wallet.model.User;
import com.rs.payments.wallet.model.Wallet;
import com.rs.payments.wallet.repository.TransactionRepository;
import com.rs.payments.wallet.repository.UserRepository;
import com.rs.payments.wallet.repository.WalletRepository;
import com.rs.payments.wallet.service.WalletService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class WalletServiceImpl implements WalletService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;

    private final TransactionRepository transactionRepository;  // ← ADD

    // ← UPDATE constructor to accept transactionRepository
    public WalletServiceImpl(UserRepository userRepository, WalletRepository walletRepository, TransactionRepository transactionRepository) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;  // ← ADD
    }

    @Override
    @Transactional    // wraps the whole method in one DB transaction
    public Wallet createWalletForUser(UUID userId) {
        User user =userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException(
                "User not found with id: " + userId));

        // ← ADD: prevent second wallet
        if (walletRepository.existsByUserId(userId)){
            throw new BadRequestException("User already has a wallet");
        }
        Wallet wallet = new Wallet();
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setUser(user);
        user.setWallet(wallet);

        user =userRepository.save(user);
        return user.getWallet();


    }


    // ── PRIVATE HELPER 1: find wallet or throw 404 ────────────────────────
    private Wallet findWalletById(UUID walletId) {
        return walletRepository.findById(walletId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Wallet not found with id: " + walletId));
    }

    // ── NEW METHOD 1: deposit ─────────────────────────────────────────────
    @Override
    @Transactional
    public Wallet deposit(UUID walletId, BigDecimal amount) {
        Wallet wallet = findWalletById(walletId);

        wallet.setBalance(wallet.getBalance().add(amount));
        wallet = walletRepository.save(wallet);

        recordTransaction(wallet, amount, TransactionType.DEPOSIT, "Deposit");
        return wallet;
    }

    // ── NEW METHOD 2: withdraw ────────────────────────────────────────────
    @Override
    @Transactional
    public Wallet withdraw(UUID walletId, BigDecimal amount) {
        Wallet wallet = findWalletById(walletId);

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(
                    "Insufficient funds: available "
                            + wallet.getBalance() + ", requested " + amount);
        }

        wallet.setBalance(wallet.getBalance().subtract(amount));
        wallet = walletRepository.save(wallet);

        recordTransaction(wallet, amount, TransactionType.WITHDRAWAL, "Withdrawal");
        return wallet;
    }

    // ── NEW METHOD 3: getBalance ──────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public BigDecimal getBalance(UUID walletId) {
        return findWalletById(walletId).getBalance();
    }





    // ── PRIVATE HELPER 2: save a transaction record ───────────────────────
    private void recordTransaction(Wallet wallet, BigDecimal amount,
                                   TransactionType type, String description) {
        Transaction transaction = Transaction.builder()
                .wallet(wallet)
                .amount(amount)
                .type(type)
                .timestamp(LocalDateTime.now())
                .description(description)
                .build();
        transactionRepository.save(transaction);
    }







}