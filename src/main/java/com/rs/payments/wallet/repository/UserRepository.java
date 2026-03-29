package com.rs.payments.wallet.repository;

import com.rs.payments.wallet.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}