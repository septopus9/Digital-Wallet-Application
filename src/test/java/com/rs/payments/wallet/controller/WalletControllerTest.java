package com.rs.payments.wallet.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rs.payments.wallet.dto.CreateWalletRequest;
import com.rs.payments.wallet.exception.GlobalExceptionHandler;
import com.rs.payments.wallet.exception.InsufficientFundsException;
import com.rs.payments.wallet.exception.ResourceNotFoundException;
import com.rs.payments.wallet.model.Wallet;
import com.rs.payments.wallet.service.WalletService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class WalletControllerTest {

    @Mock
    private WalletService walletService;

    @InjectMocks
    private WalletController walletController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private UUID walletId;

    @BeforeEach
    void setUp() {
        // wire in GlobalExceptionHandler so 400/404 work correctly in tests
        mockMvc = MockMvcBuilders.standaloneSetup(walletController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        walletId = UUID.randomUUID();
    }


    // ── existing test — fix 200 → 201 ─────────────────────────────────────
    @Nested
    @DisplayName("POST /wallets")
    class CreateWallet {

        @Test
        @DisplayName("Should return 201 when wallet created")
        void shouldReturn201OnCreate() throws Exception {
            UUID userId = UUID.randomUUID();
            CreateWalletRequest request = new CreateWalletRequest();
            request.setUserId(userId);

            Wallet wallet = new Wallet();
            wallet.setId(UUID.randomUUID());
            wallet.setBalance(BigDecimal.ZERO);

            when(walletService.createWalletForUser(userId)).thenReturn(wallet);

            mockMvc.perform(post("/wallets")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())        // 201
                    .andExpect(jsonPath("$.balance").value(0));
        }
    }

    // ── NEW: deposit tests ────────────────────────────────────────────────
    @Nested
    @DisplayName("POST /wallets/{id}/deposit")
    class Deposit {

        @Test
        @DisplayName("Should return 200 with updated balance on success")
        void shouldReturn200OnDeposit() throws Exception {
            Wallet updated = new Wallet();
            updated.setId(walletId);
            updated.setBalance(BigDecimal.valueOf(150));

            when(walletService.deposit(walletId, BigDecimal.valueOf(50)))
                    .thenReturn(updated);

            mockMvc.perform(post("/wallets/{id}/deposit", walletId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"amount\": 50}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.balance").value(150));
        }

        @Test
        @DisplayName("Should return 400 when amount is zero")
        void shouldReturn400WhenAmountIsZero() throws Exception {
            mockMvc.perform(post("/wallets/{id}/deposit", walletId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"amount\": 0}"))
                    .andExpect(status().isBadRequest());
            // @Valid catches this — service is never called
        }

        @Test
        @DisplayName("Should return 400 when amount is negative")
        void shouldReturn400WhenAmountIsNegative() throws Exception {
            mockMvc.perform(post("/wallets/{id}/deposit", walletId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"amount\": -10}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 404 when wallet not found")
        void shouldReturn404WhenWalletNotFound() throws Exception {
            when(walletService.deposit(walletId, BigDecimal.TEN))
                    .thenThrow(new ResourceNotFoundException("Wallet not found"));

            mockMvc.perform(post("/wallets/{id}/deposit", walletId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"amount\": 10}"))
                    .andExpect(status().isNotFound());
        }
    }

    // ── NEW: withdraw tests ───────────────────────────────────────────────
    @Nested
    @DisplayName("POST /wallets/{id}/withdraw")
    class Withdraw {

        @Test
        @DisplayName("Should return 200 with updated balance on success")
        void shouldReturn200OnWithdraw() throws Exception {
            Wallet updated = new Wallet();
            updated.setId(walletId);
            updated.setBalance(BigDecimal.valueOf(60));

            when(walletService.withdraw(walletId, BigDecimal.valueOf(40)))
                    .thenReturn(updated);

            mockMvc.perform(post("/wallets/{id}/withdraw", walletId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"amount\": 40}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.balance").value(60));
        }

        @Test
        @DisplayName("Should return 400 on insufficient funds")
        void shouldReturn400WhenInsufficientFunds() throws Exception {
            when(walletService.withdraw(walletId, BigDecimal.valueOf(999)))
                    .thenThrow(new InsufficientFundsException("Insufficient funds"));

            mockMvc.perform(post("/wallets/{id}/withdraw", walletId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"amount\": 999}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when amount is zero")
        void shouldReturn400WhenAmountIsZero() throws Exception {
            mockMvc.perform(post("/wallets/{id}/withdraw", walletId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"amount\": 0}"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ── NEW: balance tests ────────────────────────────────────────────────
    @Nested
    @DisplayName("GET /wallets/{id}/balance")
    class GetBalance {

        @Test
        @DisplayName("Should return 200 with balance")
        void shouldReturn200WithBalance() throws Exception {
            when(walletService.getBalance(walletId))
                    .thenReturn(BigDecimal.valueOf(100));

            mockMvc.perform(get("/wallets/{id}/balance", walletId))
                    .andExpect(status().isOk())
                    .andExpect(content().string("100"));
        }

        @Test
        @DisplayName("Should return 404 when wallet not found")
        void shouldReturn404WhenWalletNotFound() throws Exception {
            when(walletService.getBalance(walletId))
                    .thenThrow(new ResourceNotFoundException("Wallet not found"));

            mockMvc.perform(get("/wallets/{id}/balance", walletId))
                    .andExpect(status().isNotFound());
        }
    }




}
