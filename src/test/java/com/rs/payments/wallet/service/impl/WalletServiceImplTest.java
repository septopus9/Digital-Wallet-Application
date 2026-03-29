package com.rs.payments.wallet.service.impl;


import com.rs.payments.wallet.dto.TransferResponse;
import com.rs.payments.wallet.exception.BadRequestException;
import com.rs.payments.wallet.exception.InsufficientFundsException;
import com.rs.payments.wallet.exception.ResourceNotFoundException;
import com.rs.payments.wallet.model.Transaction;
import com.rs.payments.wallet.model.TransactionType;
import com.rs.payments.wallet.model.User;
import com.rs.payments.wallet.model.Wallet;
import com.rs.payments.wallet.repository.TransactionRepository;
import com.rs.payments.wallet.repository.UserRepository;
import com.rs.payments.wallet.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class WalletServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private WalletServiceImpl walletService;

    @Mock
    private TransactionRepository transactionRepository;




    private UUID userId;
    private UUID walletId;
    private User user;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        userId   = UUID.randomUUID();
        walletId = UUID.randomUUID();

        user = new User();
        user.setId(userId);

        wallet = new Wallet();
        wallet.setId(walletId);
        wallet.setUser(user);
        wallet.setBalance(BigDecimal.valueOf(100));
    }

    @Nested
    @DisplayName("createWalletForUser")
    class CreateWallet {

        @Test
        @DisplayName("Should create wallet with zero balance")
        void shouldCreateWalletForExistingUser() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(walletRepository.existsByUserId(userId)).thenReturn(false);
            when(userRepository.save(user)).thenReturn(user);

            Wallet result = walletService.createWalletForUser(userId);

            assertNotNull(result);
            assertEquals(BigDecimal.ZERO, result.getBalance());
            verify(userRepository, times(1)).findById(userId);
            verify(userRepository, times(1)).save(user);
        }

        @Test
        @DisplayName("Should throw 404 when user not found")
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> walletService.createWalletForUser(userId));
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw 400 when user already has a wallet")
        void shouldThrowWhenUserAlreadyHasWallet() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(walletRepository.existsByUserId(userId)).thenReturn(true);

            assertThatThrownBy(() -> walletService.createWalletForUser(userId))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("User already has a wallet");
            verify(userRepository, never()).save(any());
        }
    }
    @Test
    @DisplayName("Should throw BadRequestException when user already has a wallet")
    void shouldThrowWhenUserAlreadyHasWallet() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(walletRepository.existsByUserId(userId)).thenReturn(true);  // ← wallet exists

        assertThatThrownBy(() -> walletService.createWalletForUser(userId))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("User already has a wallet");

        // save must NEVER be called — stopped before it
        verify(userRepository, never()).save(any());
    }


    // ════════════════════════════════════════════════════════════
    // deposit — NEW
    // ════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("deposit")
    class Deposit {

        @Test
        @DisplayName("Should add amount to balance and record DEPOSIT transaction")
        void shouldDepositAndUpdateBalance() {
            // wallet starts at 100, depositing 50 → should become 150
            when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
            when(walletRepository.save(wallet)).thenReturn(wallet);

            Wallet result = walletService.deposit(walletId, BigDecimal.valueOf(50));

            // balance updated correctly
            assertThat(result.getBalance())
                    .isEqualByComparingTo(BigDecimal.valueOf(150));

            // a DEPOSIT transaction was saved
            ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
            verify(transactionRepository).save(captor.capture());
            assertThat(captor.getValue().getType()).isEqualTo(TransactionType.DEPOSIT);
            assertThat(captor.getValue().getAmount())
                    .isEqualByComparingTo(BigDecimal.valueOf(50));
        }

        @Test
        @DisplayName("Should throw 404 when wallet not found")
        void shouldThrowWhenWalletNotFound() {
            when(walletRepository.findById(walletId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> walletService.deposit(walletId, BigDecimal.TEN))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }


    // ════════════════════════════════════════════════════════════
    // withdraw — NEW
    // ════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("withdraw")
    class Withdraw {

        @Test
        @DisplayName("Should subtract amount and record WITHDRAWAL transaction")
        void shouldWithdrawAndUpdateBalance() {
            // wallet starts at 100, withdrawing 40 → should become 60
            when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
            when(walletRepository.save(wallet)).thenReturn(wallet);

            Wallet result = walletService.withdraw(walletId, BigDecimal.valueOf(40));

            assertThat(result.getBalance())
                    .isEqualByComparingTo(BigDecimal.valueOf(60));

            ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
            verify(transactionRepository).save(captor.capture());
            assertThat(captor.getValue().getType()).isEqualTo(TransactionType.WITHDRAWAL);
        }

        @Test
        @DisplayName("Should allow withdrawal of exact balance")
        void shouldAllowWithdrawingExactBalance() {
            // withdraw exactly 100 from 100 → balance becomes 0
            when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
            when(walletRepository.save(wallet)).thenReturn(wallet);

            Wallet result = walletService.withdraw(walletId, BigDecimal.valueOf(100));

            assertThat(result.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should throw 400 when insufficient funds")
        void shouldThrowWhenInsufficientFunds() {
            // wallet has 100, trying to withdraw 200
            when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));

            assertThatThrownBy(() -> walletService.withdraw(walletId, BigDecimal.valueOf(200)))
                    .isInstanceOf(InsufficientFundsException.class)
                    .hasMessageContaining("Insufficient funds");

            // balance must NOT change, save must NOT be called
            verify(walletRepository, never()).save(any());
            verify(transactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw 404 when wallet not found")
        void shouldThrowWhenWalletNotFound() {
            when(walletRepository.findById(walletId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> walletService.withdraw(walletId, BigDecimal.TEN))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }


    // ════════════════════════════════════════════════════════════
// transfer — NEW for Step 5
// ════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("transfer")
    class Transfer {

        private UUID toWalletId;
        private Wallet toWallet;

        @BeforeEach
        void setUp() {
            toWalletId = UUID.randomUUID();
            toWallet   = new Wallet();
            toWallet.setId(toWalletId);
            toWallet.setBalance(BigDecimal.valueOf(50));
            // note: the outer setUp() already set wallet (fromWallet) with balance 100
        }

        @Test
        @DisplayName("Should transfer funds and create two transaction records")
        void shouldTransferFundsAndCreateTwoTransactions() {
            when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
            when(walletRepository.findById(toWalletId)).thenReturn(Optional.of(toWallet));
            when(walletRepository.save(wallet)).thenReturn(wallet);
            when(walletRepository.save(toWallet)).thenReturn(toWallet);

            TransferResponse response = walletService.transfer(
                    walletId, toWalletId, BigDecimal.valueOf(30));

            // response fields correct
            assertThat(response.getFromWalletId()).isEqualTo(walletId);
            assertThat(response.getToWalletId()).isEqualTo(toWalletId);
            assertThat(response.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(30));

            // balances updated correctly
            assertThat(wallet.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(70));   // 100-30
            assertThat(toWallet.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(80)); // 50+30

            // exactly TWO transaction records saved
            ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
            verify(transactionRepository, times(2)).save(captor.capture());

            // one TRANSFER_OUT and one TRANSFER_IN
            List<TransactionType> types = captor.getAllValues().stream()
                    .map(Transaction::getType)
                    .toList();
            assertThat(types).containsExactlyInAnyOrder(
                    TransactionType.TRANSFER_OUT,
                    TransactionType.TRANSFER_IN);
        }

        @Test
        @DisplayName("Should throw 400 and save nothing when insufficient funds")
        void shouldThrowAndSaveNothingWhenInsufficientFunds() {
            // wallet has 100, trying to transfer 500
            when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
            when(walletRepository.findById(toWalletId)).thenReturn(Optional.of(toWallet));

            assertThatThrownBy(() ->
                    walletService.transfer(walletId, toWalletId, BigDecimal.valueOf(500)))
                    .isInstanceOf(InsufficientFundsException.class)
                    .hasMessageContaining("Insufficient funds");

            // neither wallet was saved — nothing changed
            verify(walletRepository, never()).save(any());
            // no transaction records created
            verify(transactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw 400 when transferring to same wallet")
        void shouldThrowWhenSameWallet() {
            // fromWalletId == toWalletId
            assertThatThrownBy(() ->
                    walletService.transfer(walletId, walletId, BigDecimal.TEN))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Cannot transfer to the same wallet");

            // no DB calls at all
            verify(walletRepository, never()).findById(any());
            verify(walletRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw 404 when source wallet not found")
        void shouldThrowWhenSourceWalletNotFound() {
            when(walletRepository.findById(walletId)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    walletService.transfer(walletId, toWalletId, BigDecimal.TEN))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw 404 when destination wallet not found")
        void shouldThrowWhenDestinationWalletNotFound() {
            when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
            when(walletRepository.findById(toWalletId)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    walletService.transfer(walletId, toWalletId, BigDecimal.TEN))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }


}
