package com.rs.payments.wallet.repository;

import com.rs.payments.wallet.model.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    boolean existsByUserId(UUID userId);    // ← ADD THIS ONE LINE
}