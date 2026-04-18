package com.sh.payments.wallet.controller;

import com.sh.payments.wallet.BaseIntegrationTest;
import com.sh.payments.wallet.dto.AmountRequest;
import com.sh.payments.wallet.dto.CreateWalletRequest;
import com.sh.payments.wallet.model.User;
import com.sh.payments.wallet.model.Wallet;
import com.sh.payments.wallet.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

class WalletIntegrationTest extends BaseIntegrationTest {

    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private UserRepository userRepository;


    private String url() {
        return "http://localhost:" + port + "/wallets";
    }


    // helper — creates a user directly in DB and returns it
    private User createUser() {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        User user = new User();
        user.setUsername("user_" + unique);
        user.setEmail(unique + "@example.com");
        return userRepository.save(user);
    }


    @Test
    @DisplayName("Should return 404 for non-existent user")
    void shouldCreateWalletForExistingUser() {
        User user = createUser();

        CreateWalletRequest request = new CreateWalletRequest();
        request.setUserId(user.getId());

        ResponseEntity<Wallet> response = restTemplate.postForEntity(url(), request, Wallet.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED); // change here OK to CREATED
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isNotNull();
        assertThat(response.getBody().getBalance()).isEqualByComparingTo("0");
        assertThat(response.getBody().getUser().getId()).isEqualTo(user.getId());
    }

    @Test
    @DisplayName("Should return 404 for non-existent user")
    void shouldReturnNotFoundForNonExistentUser() {
        CreateWalletRequest request = new CreateWalletRequest();
        request.setUserId(UUID.randomUUID());

        assertThatThrownBy(() -> restTemplate.postForEntity(url(), request, String.class))
                .satisfies(e -> assertThat(((HttpClientErrorException) e).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    // ── NEW test ──────────────────────────────────────────────────────────
    @Test
    @DisplayName("Should return 400 when user already has a wallet")
    void shouldReturn400WhenUserAlreadyHasWallet() {
        User user = createUser();

        CreateWalletRequest request = new CreateWalletRequest();
        request.setUserId(user.getId());

        // first request — succeeds
        restTemplate.postForEntity(url(), request, Wallet.class);

        // second request — same user, must return 400
        assertThatThrownBy(() -> restTemplate.postForEntity(url(), request, String.class))
                .satisfies(e -> assertThat(((HttpClientErrorException) e).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    // ── NEW: deposit integration tests ───────────────────────────────────────
    @Test
    @DisplayName("Should deposit funds and return updated balance")
    void shouldDepositFunds() {
        User user = createUser();
        Wallet wallet = createWallet(user.getId());

        String depositUrl = "http://localhost:" + port
                + "/wallets/" + wallet.getId() + "/deposit";

        ResponseEntity<Wallet> response = restTemplate.postForEntity(
                depositUrl, new AmountRequest(BigDecimal.valueOf(150)), Wallet.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getBalance())
                .isEqualByComparingTo(BigDecimal.valueOf(150));
    }

    @Test
    @DisplayName("Should return 400 when depositing zero")
    void shouldReturn400WhenDepositingZero() {
        User user = createUser();
        Wallet wallet = createWallet(user.getId());

        String depositUrl = "http://localhost:" + port
                + "/wallets/" + wallet.getId() + "/deposit";

        assertThatThrownBy(() -> restTemplate.postForEntity(
                depositUrl, new AmountRequest(BigDecimal.ZERO), String.class))
                .satisfies(e -> assertThat(((HttpClientErrorException) e).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("Should withdraw funds and return updated balance")
    void shouldWithdrawFunds() {
        User user = createUser();
        Wallet wallet = createWallet(user.getId());
        deposit(wallet.getId(), BigDecimal.valueOf(200)); // fund first

        String withdrawUrl = "http://localhost:" + port
                + "/wallets/" + wallet.getId() + "/withdraw";

        ResponseEntity<Wallet> response = restTemplate.postForEntity(
                withdrawUrl, new AmountRequest(BigDecimal.valueOf(75)), Wallet.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getBalance())
                .isEqualByComparingTo(BigDecimal.valueOf(125));
    }

    @Test
    @DisplayName("Should return 400 on insufficient funds")
    void shouldReturn400WhenInsufficientFunds() {
        User user = createUser();
        Wallet wallet = createWallet(user.getId());
        // wallet has 0 balance, trying to withdraw 100

        String withdrawUrl = "http://localhost:" + port
                + "/wallets/" + wallet.getId() + "/withdraw";

        assertThatThrownBy(() -> restTemplate.postForEntity(
                withdrawUrl, new AmountRequest(BigDecimal.valueOf(100)), String.class))
                .satisfies(e -> assertThat(((HttpClientErrorException) e).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("Should return correct balance")
    void shouldReturnBalance() {
        User user = createUser();
        Wallet wallet = createWallet(user.getId());
        deposit(wallet.getId(), BigDecimal.valueOf(300));

        String balanceUrl = "http://localhost:" + port
                + "/wallets/" + wallet.getId() + "/balance";

        ResponseEntity<BigDecimal> response = restTemplate.getForEntity(
                balanceUrl, BigDecimal.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualByComparingTo(BigDecimal.valueOf(300));
    }


    // helper — creates wallet via API
    private Wallet createWallet(UUID userId) {
        CreateWalletRequest req = new CreateWalletRequest();
        req.setUserId(userId);
        return restTemplate.postForEntity(url(), req, Wallet.class).getBody();
    }

    // helper — deposits money via API
    private void deposit(UUID walletId, BigDecimal amount) {
        String depositUrl = "http://localhost:" + port
                + "/wallets/" + walletId + "/deposit";
        restTemplate.postForEntity(depositUrl, new AmountRequest(amount), Wallet.class);
    }




}
