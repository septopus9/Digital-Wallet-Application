package com.sh.payments.wallet.service.impl;

import com.sh.payments.wallet.dto.TransferResponse;
import com.sh.payments.wallet.exception.BadRequestException;
import com.sh.payments.wallet.exception.InsufficientFundsException;
import com.sh.payments.wallet.exception.ResourceNotFoundException;
import com.sh.payments.wallet.model.Transaction;
import com.sh.payments.wallet.model.TransactionType;
import com.sh.payments.wallet.model.User;
import com.sh.payments.wallet.model.Wallet;
import com.sh.payments.wallet.repository.TransactionRepository;
import com.sh.payments.wallet.repository.UserRepository;
import com.sh.payments.wallet.repository.WalletRepository;
import com.sh.payments.wallet.service.WalletService;
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

    @Override
    @Transactional          // ← CRITICAL — wraps BOTH wallet updates in one DB transaction
    public TransferResponse transfer(UUID fromWalletId, UUID toWalletId, BigDecimal amount) {

        // Rule 1: cannot transfer to yourself
        if (fromWalletId.equals(toWalletId)){
            throw new BadRequestException("Cannot transfer to the same wallet");

        }

        // Load both wallets — throws 404 if either doesn't exist
        Wallet from = findWalletById(fromWalletId);
        Wallet to = findWalletById(toWalletId);

        // Rule 2: source wallet must have enough money
        if (from.getBalance().compareTo(amount)<0){
            throw new InsufficientFundsException("Insufficient funds: available "
                    + from.getBalance() + ", requested " + amount);

        }

        // Move the money
        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));


        // Save both wallets
        walletRepository.save(from);
        walletRepository.save(to);


        // Create two transaction records — one on each wallet
        recordTransaction(from,amount,TransactionType.TRANSFER_OUT,"Transfer to wallet "+toWalletId);
        recordTransaction(to,amount,TransactionType.TRANSFER_IN,"Transfer from wallet "+fromWalletId);


        return new TransferResponse(fromWalletId,toWalletId,amount,from.getBalance());
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