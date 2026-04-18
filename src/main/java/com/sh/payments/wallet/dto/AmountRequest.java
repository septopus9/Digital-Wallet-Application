package com.sh.payments.wallet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request containing a monetary amount")
public class AmountRequest {

    @NotNull
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Schema(description = "The monetary amount", example = "50.00")
    private BigDecimal amount;



}
