package com.rs.payments.wallet.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Transaction entity")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Schema(description = "Unique identifier of the transaction", example = "b1f8e321-7c9b-46e2-8d1a-4f5a6b7c8d9e")
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "wallet_id", nullable = false) // ← ADD nullable=false
    @Schema(description = "Wallet associated with the transaction")
    private Wallet wallet;

    @Schema(description = "Amount of the transaction", example = "50.00")
    @Column(nullable = false, precision = 19, scale = 2)  // ← ADD @Column
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Schema(description = "Type of the transaction (DEBIT/CREDIT)")
    @Column(nullable = false)           // ← ADD nullable=false
    private TransactionType type;

    @Schema(description = "Timestamp of when the transaction occurred")
    @Column(nullable = false)                            // ← ADD nullable=false
    private LocalDateTime timestamp;

    @Schema(description = "Description of the transaction", example = "Payment for services")
    private String description;
}