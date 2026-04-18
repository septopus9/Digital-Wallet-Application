package com.sh.payments.wallet.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sh.payments.wallet.dto.TransferRequest;
import com.sh.payments.wallet.dto.TransferResponse;
import com.sh.payments.wallet.exception.BadRequestException;
import com.sh.payments.wallet.exception.GlobalExceptionHandler;
import com.sh.payments.wallet.exception.InsufficientFundsException;
import com.sh.payments.wallet.exception.ResourceNotFoundException;
import com.sh.payments.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
@ExtendWith(MockitoExtension.class)
public class TransferControllerTest {

    @Mock
    private WalletService walletService;

    @InjectMocks
    private TransferController transferController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private UUID fromId;
    private UUID toId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(transferController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        fromId = UUID.randomUUID();
        toId   = UUID.randomUUID();
    }


    @Nested
    @DisplayName("POST /transfers")
    class Transfer {

        @Test
        @DisplayName("Should return 200 with transfer details on success")
        void shouldReturn200OnSuccess() throws Exception {
            BigDecimal amount = BigDecimal.valueOf(50);
            TransferRequest request = new TransferRequest(fromId, toId, amount);
            TransferResponse response = new TransferResponse(
                    fromId, toId, amount, BigDecimal.valueOf(50));

            when(walletService.transfer(fromId, toId, amount)).thenReturn(response);

            mockMvc.perform(post("/transfers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.fromWalletId").value(fromId.toString()))
                    .andExpect(jsonPath("$.toWalletId").value(toId.toString()))
                    .andExpect(jsonPath("$.amount").value(50))
                    .andExpect(jsonPath("$.fromWalletBalance").value(50));
        }

        @Test
        @DisplayName("Should return 400 when transferring to same wallet")
        void shouldReturn400WhenSameWallet() throws Exception {
            TransferRequest request = new TransferRequest(fromId, toId, BigDecimal.TEN);

            when(walletService.transfer(fromId, toId, BigDecimal.TEN))
                    .thenThrow(new BadRequestException("Cannot transfer to the same wallet"));

            mockMvc.perform(post("/transfers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 on insufficient funds")
        void shouldReturn400WhenInsufficientFunds() throws Exception {
            TransferRequest request = new TransferRequest(fromId, toId, BigDecimal.valueOf(9999));

            when(walletService.transfer(fromId, toId, BigDecimal.valueOf(9999)))
                    .thenThrow(new InsufficientFundsException("Insufficient funds"));

            mockMvc.perform(post("/transfers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 404 when wallet not found")
        void shouldReturn404WhenWalletNotFound() throws Exception {
            TransferRequest request = new TransferRequest(fromId, toId, BigDecimal.TEN);

            when(walletService.transfer(fromId, toId, BigDecimal.TEN))
                    .thenThrow(new ResourceNotFoundException("Wallet not found"));

            mockMvc.perform(post("/transfers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 400 when amount is zero")
        void shouldReturn400WhenAmountIsZero() throws Exception {
            TransferRequest request = new TransferRequest(fromId, toId, BigDecimal.ZERO);

            mockMvc.perform(post("/transfers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
            // @Valid fires — service is never called
        }

        @Test
        @DisplayName("Should return 400 when fromWalletId is missing")
        void shouldReturn400WhenFromWalletIdMissing() throws Exception {
            // no fromWalletId in the JSON body
            mockMvc.perform(post("/transfers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"toWalletId\":\"" + toId + "\",\"amount\":10}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when toWalletId is missing")
        void shouldReturn400WhenToWalletIdMissing() throws Exception {
            mockMvc.perform(post("/transfers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"fromWalletId\":\"" + fromId + "\",\"amount\":10}"))
                    .andExpect(status().isBadRequest());
        }
    }
}
