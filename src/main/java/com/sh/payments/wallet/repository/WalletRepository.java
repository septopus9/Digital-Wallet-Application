package com.sh.payments.wallet.repository;

import com.sh.payments.wallet.model.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    boolean existsByUserId(UUID userId);    // ← ADD THIS ONE LINE
}