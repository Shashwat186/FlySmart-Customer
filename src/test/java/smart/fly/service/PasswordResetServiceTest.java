package smart.fly.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import smart.fly.util.PasswordUtil;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    private static final String DELETE_BY_EMAIL = "DELETE FROM password_reset_tokens WHERE email = ?";
    private static final String INSERT_TOKEN = "INSERT INTO password_reset_tokens (email, token, expiry_date) VALUES (?, ?, ?)";
    private static final String UPDATE_PASSWORD = "UPDATE user SET password_hash = ?, password_salt = ? WHERE email = ?";
    private static final String SELECT_TOKEN = "SELECT email, expiry_date FROM password_reset_tokens WHERE token = ?";
    private static final String SELECT_EMAIL = "SELECT email FROM password_reset_tokens WHERE token = ?";

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private UserService userService;

    @Mock
    private PasswordUtil passwordUtil;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private final String email = "user@example.com";
    private final String token = "token-123";

    // ---------- createResetToken ----------

    @Test
    void createResetToken_shouldDeleteOldTokensBeforeInsertingNewOne() {
        // Act
        passwordResetService.createResetToken(email);

        // Assert
        InOrder inOrder = inOrder(jdbcTemplate);
        inOrder.verify(jdbcTemplate).update(DELETE_BY_EMAIL, email);
        inOrder.verify(jdbcTemplate).update(eq(INSERT_TOKEN), eq(email), anyString(), any(Timestamp.class));
    }

    @Test
    void createResetToken_shouldInsertUuidTokenExpiringInOneHour() {
        // Arrange
        long before = System.currentTimeMillis();

        // Act
        passwordResetService.createResetToken(email);

        // Assert
        long after = System.currentTimeMillis();
        ArgumentCaptor<Object> tokenCaptor = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<Object> expiryCaptor = ArgumentCaptor.forClass(Object.class);
        verify(jdbcTemplate).update(eq(INSERT_TOKEN), eq(email), tokenCaptor.capture(), expiryCaptor.capture());

        assertDoesNotThrow(() -> UUID.fromString((String) tokenCaptor.getValue()));
        long expiry = ((Timestamp) expiryCaptor.getValue()).getTime();
        assertTrue(expiry >= before + 3_600_000L && expiry <= after + 3_600_000L);
    }

    // ---------- updatePassword ----------

    @Test
    void updatePassword_shouldStoreNewHashAndSalt_andReturnTrue() {
        // Arrange
        when(passwordUtil.generateSalt()).thenReturn("newSalt");
        when(passwordUtil.hashPassword("newPass1", "newSalt")).thenReturn("newHash");
        when(jdbcTemplate.update(UPDATE_PASSWORD, "newHash", "newSalt", email)).thenReturn(1);

        // Act & Assert
        assertTrue(passwordResetService.updatePassword(email, "newPass1"));
    }

    @Test
    void updatePassword_shouldReturnFalse_whenNoUserUpdated() {
        // Arrange
        when(passwordUtil.generateSalt()).thenReturn("newSalt");
        when(passwordUtil.hashPassword("newPass1", "newSalt")).thenReturn("newHash");
        when(jdbcTemplate.update(UPDATE_PASSWORD, "newHash", "newSalt", email)).thenReturn(0);

        // Act & Assert
        assertFalse(passwordResetService.updatePassword(email, "newPass1"));
    }

    @Test
    void updatePassword_shouldReturnFalse_whenDatabaseFails() {
        // Arrange
        when(passwordUtil.generateSalt()).thenReturn("newSalt");
        when(passwordUtil.hashPassword("newPass1", "newSalt")).thenReturn("newHash");
        when(jdbcTemplate.update(UPDATE_PASSWORD, "newHash", "newSalt", email))
                .thenThrow(new DataAccessResourceFailureException("DB down"));

        // Act & Assert
        assertFalse(passwordResetService.updatePassword(email, "newPass1"));
    }

    // ---------- validateToken ----------

    @Test
    void validateToken_shouldReturnTrue_forFutureTimestampExpiry() {
        // Arrange
        Timestamp future = Timestamp.valueOf(LocalDateTime.now().plusMinutes(30));
        when(jdbcTemplate.queryForMap(SELECT_TOKEN, token)).thenReturn(Map.of("email", email, "expiry_date", future));

        // Act & Assert
        assertTrue(passwordResetService.validateToken(token));
    }

    @Test
    void validateToken_shouldReturnTrue_forFutureLocalDateTimeExpiry() {
        // Arrange
        LocalDateTime future = LocalDateTime.now().plusMinutes(30);
        when(jdbcTemplate.queryForMap(SELECT_TOKEN, token)).thenReturn(Map.of("email", email, "expiry_date", future));

        // Act & Assert
        assertTrue(passwordResetService.validateToken(token));
    }

    @Test
    void validateToken_shouldReturnFalse_whenExpired() {
        // Arrange
        Timestamp past = Timestamp.valueOf(LocalDateTime.now().minusMinutes(1));
        when(jdbcTemplate.queryForMap(SELECT_TOKEN, token)).thenReturn(Map.of("email", email, "expiry_date", past));

        // Act & Assert
        assertFalse(passwordResetService.validateToken(token));
    }

    @Test
    void validateToken_shouldReturnFalse_whenExpiryHasUnexpectedType() {
        // Arrange
        when(jdbcTemplate.queryForMap(SELECT_TOKEN, token))
                .thenReturn(Map.of("email", email, "expiry_date", "2099-01-01"));

        // Act & Assert
        assertFalse(passwordResetService.validateToken(token));
    }

    @Test
    void validateToken_shouldReturnFalse_whenTokenNotFound() {
        // Arrange
        when(jdbcTemplate.queryForMap(SELECT_TOKEN, token)).thenThrow(new EmptyResultDataAccessException(1));

        // Act & Assert
        assertFalse(passwordResetService.validateToken(token));
    }

    // ---------- getEmailFromToken / deleteToken ----------

    @Test
    void getEmailFromToken_shouldReturnEmail_whenTokenExists() {
        // Arrange
        when(jdbcTemplate.queryForObject(SELECT_EMAIL, String.class, token)).thenReturn(email);

        // Act & Assert
        assertEquals(email, passwordResetService.getEmailFromToken(token));
    }

    @Test
    void getEmailFromToken_shouldReturnNull_whenTokenNotFound() {
        // Arrange
        when(jdbcTemplate.queryForObject(SELECT_EMAIL, String.class, token))
                .thenThrow(new EmptyResultDataAccessException(1));

        // Act & Assert
        assertNull(passwordResetService.getEmailFromToken(token));
    }

    @Test
    void deleteToken_shouldDeleteByToken() {
        // Act
        passwordResetService.deleteToken(token);

        // Assert
        verify(jdbcTemplate).update("DELETE FROM password_reset_tokens WHERE token = ?", token);
    }
}
