package com.sh.payments.wallet.controller;

import com.sh.payments.wallet.BaseIntegrationTest;
import com.sh.payments.wallet.dto.AmountRequest;
import com.sh.payments.wallet.dto.CreateWalletRequest;
import com.sh.payments.wallet.dto.TransferRequest;
import com.sh.payments.wallet.dto.TransferResponse;
import com.sh.payments.wallet.model.User;
import com.sh.payments.wallet.model.Wallet;
import com.sh.payments.wallet.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class TransferIntegrationTest extends BaseIntegrationTest {
    private final RestTemplate restTemplate = new RestTemplate();
    @Autowired
    private UserRepository userRepository;
    // Two funded wallets — set up fresh before every test
    private Wallet walletA;
    private Wallet walletB;

    @BeforeEach
    void setUp() {
        walletA = createWalletWithBalance(BigDecimal.valueOf(200));
        walletB = createWalletWithBalance(BigDecimal.valueOf(100));
    }

    @Nested
    @DisplayName("POST /transfers")
    class Transfer {

        @Test
        @DisplayName("Should transfer funds and update both balances")
        void shouldTransferFundsSuccessfully() {
            TransferRequest request = new TransferRequest(
                    walletA.getId(), walletB.getId(), BigDecimal.valueOf(80));

            ResponseEntity<TransferResponse> response = restTemplate.postForEntity(
                    url("/transfers"), request, TransferResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getAmount())
                    .isEqualByComparingTo(BigDecimal.valueOf(80));
            assertThat(response.getBody().getFromWalletBalance())
                    .isEqualByComparingTo(BigDecimal.valueOf(120));  // 200 - 80

            // verify both balances via GET /balance
            BigDecimal balanceA = getBalance(walletA.getId());
            BigDecimal balanceB = getBalance(walletB.getId());

            assertThat(balanceA).isEqualByComparingTo(BigDecimal.valueOf(120)); // 200 - 80
            assertThat(balanceB).isEqualByComparingTo(BigDecimal.valueOf(180)); // 100 + 80
        }

        @Test
        @DisplayName("Should return 400 and not change any balance on insufficient funds")
        void shouldNotChangeBalancesOnInsufficientFunds() {
            // walletA has 200, trying to transfer 999
            TransferRequest request = new TransferRequest(
                    walletA.getId(), walletB.getId(), BigDecimal.valueOf(999));

            assertThatThrownBy(() -> restTemplate.postForEntity(
                    url("/transfers"), request, String.class))
                    .satisfies(e -> assertThat(((HttpClientErrorException) e).getStatusCode())
                            .isEqualTo(HttpStatus.BAD_REQUEST));

            // balances must be completely unchanged — atomicity proven
            assertThat(getBalance(walletA.getId()))
                    .isEqualByComparingTo(BigDecimal.valueOf(200));
            assertThat(getBalance(walletB.getId()))
                    .isEqualByComparingTo(BigDecimal.valueOf(100));
        }

        @Test
        @DisplayName("Should return 400 when transferring to same wallet")
        void shouldReturn400WhenSameWallet() {
            TransferRequest request = new TransferRequest(
                    walletA.getId(), walletA.getId(), BigDecimal.TEN);

            assertThatThrownBy(() -> restTemplate.postForEntity(
                    url("/transfers"), request, String.class))
                    .satisfies(e -> assertThat(((HttpClientErrorException) e).getStatusCode())
                            .isEqualTo(HttpStatus.BAD_REQUEST));
        }

        @Test
        @DisplayName("Should return 404 when source wallet does not exist")
        void shouldReturn404WhenSourceWalletMissing() {
            TransferRequest request = new TransferRequest(
                    UUID.randomUUID(), walletB.getId(), BigDecimal.TEN);

            assertThatThrownBy(() -> restTemplate.postForEntity(
                    url("/transfers"), request, String.class))
                    .satisfies(e -> assertThat(((HttpClientErrorException) e).getStatusCode())
                            .isEqualTo(HttpStatus.NOT_FOUND));
        }

        @Test
        @DisplayName("Should return 404 when destination wallet does not exist")
        void shouldReturn404WhenDestinationWalletMissing() {
            TransferRequest request = new TransferRequest(
                    walletA.getId(), UUID.randomUUID(), BigDecimal.TEN);

            assertThatThrownBy(() -> restTemplate.postForEntity(
                    url("/transfers"), request, String.class))
                    .satisfies(e -> assertThat(((HttpClientErrorException) e).getStatusCode())
                            .isEqualTo(HttpStatus.NOT_FOUND));
        }

        @Test
        @DisplayName("Should return 400 when amount is zero")
        void shouldReturn400WhenAmountIsZero() {
            TransferRequest request = new TransferRequest(
                    walletA.getId(), walletB.getId(), BigDecimal.ZERO);

            assertThatThrownBy(() -> restTemplate.postForEntity(
                    url("/transfers"), request, String.class))
                    .satisfies(e -> assertThat(((HttpClientErrorException) e).getStatusCode())
                            .isEqualTo(HttpStatus.BAD_REQUEST));
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    // Creates a user + wallet + deposits the given balance
    private Wallet createWalletWithBalance(BigDecimal balance) {
        // create user
        String unique = UUID.randomUUID().toString().substring(0, 8);
        User user = new User();
        user.setUsername("user_" + unique);
        user.setEmail(unique + "@test.com");
        user = userRepository.save(user);

        // create wallet
        CreateWalletRequest walletReq = new CreateWalletRequest();
        walletReq.setUserId(user.getId());
        Wallet wallet = restTemplate.postForEntity(
                url("/wallets"), walletReq, Wallet.class).getBody();

        // deposit money if needed
        if (balance.compareTo(BigDecimal.ZERO) > 0) {
            restTemplate.postForEntity(
                    url("/wallets/" + wallet.getId() + "/deposit"),
                    new AmountRequest(balance),
                    Wallet.class);
        }
        return wallet;
    }

    // Calls GET /wallets/{id}/balance and returns the value
    private BigDecimal getBalance(UUID walletId) {
        return restTemplate.getForObject(
                url("/wallets/" + walletId + "/balance"),
                BigDecimal.class);
    }
}
