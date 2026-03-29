package com.rs.payments.wallet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to transfer funds between wallets")
public class TransferRequest {

    @NotNull
    @Schema(description = "Source wallet ID")
    private UUID fromWalletId;

    @NotNull
    @Schema(description = "Destination wallet ID")
    private UUID toWalletId;

    @NotNull
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Schema(description = "Amount to transfer", example = "25.00")
    private BigDecimal amount;



}
