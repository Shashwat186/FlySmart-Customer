package smart.fly.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Base64;

import org.junit.jupiter.api.Test;

class PasswordUtilTest {

    private final PasswordUtil passwordUtil = new PasswordUtil();

    @Test
    void generateSalt_shouldReturnBase64EncodedSixteenBytes() {
        // Act
        String salt = passwordUtil.generateSalt();

        // Assert
        assertEquals(16, Base64.getDecoder().decode(salt).length);
    }

    @Test
    void generateSalt_shouldReturnDifferentValuesOnEachCall() {
        // Act
        String first = passwordUtil.generateSalt();
        String second = passwordUtil.generateSalt();

        // Assert
        assertNotEquals(first, second);
    }

    @Test
    void hashPassword_shouldBeDeterministic_forSamePasswordAndSalt() {
        // Arrange
        String salt = passwordUtil.generateSalt();

        // Act
        String first = passwordUtil.hashPassword("secret123", salt);
        String second = passwordUtil.hashPassword("secret123", salt);

        // Assert
        assertEquals(first, second);
    }

    @Test
    void hashPassword_shouldDiffer_forDifferentSalts() {
        // Act
        String first = passwordUtil.hashPassword("secret123", passwordUtil.generateSalt());
        String second = passwordUtil.hashPassword("secret123", passwordUtil.generateSalt());

        // Assert
        assertNotEquals(first, second);
    }

    @Test
    void hashPassword_shouldNotReturnPlainPassword() {
        // Act
        String hash = passwordUtil.hashPassword("secret123", passwordUtil.generateSalt());

        // Assert
        assertNotEquals("secret123", hash);
        assertEquals(32, Base64.getDecoder().decode(hash).length); // SHA-256 digest size
    }

    @Test
    void hashPassword_shouldThrow_whenSaltIsNotBase64() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> passwordUtil.hashPassword("secret123", "not base64!"));
    }

    @Test
    void verifyPassword_shouldReturnTrue_forCorrectPassword() {
        // Arrange
        String salt = passwordUtil.generateSalt();
        String hash = passwordUtil.hashPassword("secret123", salt);

        // Act & Assert
        assertTrue(passwordUtil.verifyPassword("secret123", hash, salt));
    }

    @Test
    void verifyPassword_shouldReturnFalse_forWrongPassword() {
        // Arrange
        String salt = passwordUtil.generateSalt();
        String hash = passwordUtil.hashPassword("secret123", salt);

        // Act & Assert
        assertFalse(passwordUtil.verifyPassword("wrong123", hash, salt));
    }

    @Test
    void verifyPassword_shouldReturnFalse_forWrongSalt() {
        // Arrange
        String hash = passwordUtil.hashPassword("secret123", passwordUtil.generateSalt());

        // Act & Assert
        assertFalse(passwordUtil.verifyPassword("secret123", hash, passwordUtil.generateSalt()));
    }
}
