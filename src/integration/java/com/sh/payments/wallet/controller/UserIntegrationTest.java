package com.sh.payments.wallet.controller;

import com.sh.payments.wallet.BaseIntegrationTest;
import com.sh.payments.wallet.dto.CreateUserRequest;
import com.sh.payments.wallet.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

class UserIntegrationTest extends BaseIntegrationTest {

    private final RestTemplate restTemplate = new RestTemplate();

    private String url() {
        return "http://localhost:" + port + "/users";
    }

    @Test
    @DisplayName("Should create user and return 201")
    void shouldCreateUser() {

        // unique suffix prevents collision with other tests
        String unique = UUID.randomUUID().toString().substring(0, 8);

        CreateUserRequest request = new CreateUserRequest(
                "testuser_" + unique, unique + "@example.com");

        ResponseEntity<User> response = restTemplate.postForEntity(url(), request, User.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED); // change here OK to CREATED
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isNotNull();
        assertThat(response.getBody().getUsername()).isEqualTo("testuser_" + unique);


    }

    @Test
    @DisplayName("Should ignore id provided in request body")
    void shouldCreateUserAndIgnoreProvidedId() {
        UUID providedId = UUID.randomUUID();
        // Constructing a JSON string that includes an ID to verify it's ignored or not mapped
        String jsonRequest = String.format(
                "{\"id\":\"%s\", \"username\":\"testuser2_%s\", \"email\":\"test2_%s@example.com\"}",
                providedId,
                UUID.randomUUID().toString().substring(0, 8),
                UUID.randomUUID().toString().substring(0, 8));


        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(jsonRequest, headers);

        String url = "http://localhost:" + port + "/users";
        ResponseEntity<User> response = restTemplate.postForEntity(url, entity, User.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED); // ← was OK
        assertThat(response.getBody().getId()).isNotEqualTo(providedId);
    }

    @Test
    @DisplayName("Should return 400 when fields are blank")
    void shouldReturnBadRequestWhenUserInvalid() {
        CreateUserRequest request = new CreateUserRequest("", ""); // Blank fields

        String url = "http://localhost:" + port + "/users";
        try {
            restTemplate.postForEntity(url, request, User.class);
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    // ── NEW test 1 ────────────────────────────────────────────────────────
    @Test
    @DisplayName("Should return 409 when username already exists")
    void shouldReturn409ForDuplicateUsername() {
        String unique = UUID.randomUUID().toString().substring(0,8);

        // create the user first time — succeeds
        restTemplate.postForEntity(url(),
                new CreateUserRequest("dup_" + unique, "first_" + unique + "@test.com"),
                User.class);

        // create again with same username — must fail with 409
        assertThatThrownBy(() -> restTemplate.postForEntity(url(),
                new CreateUserRequest("dup_" + unique, "second_" + unique + "@test.com"),
                User.class))
                .satisfies(e -> assertThat(((HttpClientErrorException) e).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));
    }

    // ── NEW test 2 ────────────────────────────────────────────────────────
    @Test
    @DisplayName("Should return 409 when email already exists")
    void shouldReturn409ForDuplicateEmail() {
        String unique = UUID.randomUUID().toString().substring(0, 8);

        // create with shared email first time — succeeds
        restTemplate.postForEntity(url(),
                new CreateUserRequest("user1_" + unique, "shared_" + unique + "@test.com"),
                User.class);

        // create again with same email but different username — must fail with 409
        assertThatThrownBy(() -> restTemplate.postForEntity(url(),
                new CreateUserRequest("user2_" + unique, "shared_" + unique + "@test.com"),
                User.class))
                .satisfies(e -> assertThat(((HttpClientErrorException) e).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));
    }


}
