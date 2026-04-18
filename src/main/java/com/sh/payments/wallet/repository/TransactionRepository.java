package com.sh.payments.wallet.repository;

import java.util.UUID;
import com.sh.payments.wallet.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
}