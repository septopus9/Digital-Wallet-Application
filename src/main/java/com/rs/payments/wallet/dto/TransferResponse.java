package com.rs.payments.wallet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response containing transfer details")
public class TransferResponse {

    @Schema(description = "Source wallet ID")
    private UUID fromWalletId;

    @Schema(description = "Destination wallet ID")
    private UUID toWalletId;

    @Schema(description = "Amount transferred")
    private BigDecimal amount;

    @Schema(description = "Balance of the source wallet after transfer")
    private BigDecimal fromWalletBalance;

}
