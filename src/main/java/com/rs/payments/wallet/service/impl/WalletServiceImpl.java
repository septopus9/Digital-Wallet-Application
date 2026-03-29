package com.rs.payments.wallet.service.impl;

import com.rs.payments.wallet.exception.BadRequestException;
import com.rs.payments.wallet.exception.ResourceNotFoundException;
import com.rs.payments.wallet.model.User;
import com.rs.payments.wallet.model.Wallet;
import com.rs.payments.wallet.repository.TransactionRepository;
import com.rs.payments.wallet.repository.UserRepository;
import com.rs.payments.wallet.repository.WalletRepository;
import com.rs.payments.wallet.service.WalletService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
}