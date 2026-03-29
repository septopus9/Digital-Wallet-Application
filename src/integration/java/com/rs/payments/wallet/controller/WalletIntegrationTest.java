package com.rs.payments.wallet.controller;

import com.rs.payments.wallet.BaseIntegrationTest;
import com.rs.payments.wallet.dto.CreateWalletRequest;
import com.rs.payments.wallet.model.User;
import com.rs.payments.wallet.model.Wallet;
import com.rs.payments.wallet.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

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

}
