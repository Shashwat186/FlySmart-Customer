package smart.fly.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ValidationUtilTest {

    @ParameterizedTest
    @ValueSource(strings = { "user@example.com", "first.last@mail.co.in", "user-name@sub.domain.org", "a_b@x.io" })
    void validateEmail_shouldAccept_validEmails(String email) {
        // Act & Assert
        assertDoesNotThrow(() -> ValidationUtil.validateEmail(email));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "plainaddress", "@example.com", "user@", "user@example", "user@example.toolong",
            "user name@example.com" })
    void validateEmail_shouldReject_invalidEmails(String email) {
        // Act
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ValidationUtil.validateEmail(email));

        // Assert
        assertEquals("Invalid email format", ex.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = { "9876543210", "+919876543210", "123456789012345" })
    void validatePhone_shouldAccept_validPhones(String phone) {
        // Act & Assert
        assertDoesNotThrow(() -> ValidationUtil.validatePhone(phone));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "12345", "1234567890123456", "98765abcde", "98765-43210", "++9876543210" })
    void validatePhone_shouldReject_invalidPhones(String phone) {
        // Act
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ValidationUtil.validatePhone(phone));

        // Assert
        assertEquals("Invalid phone number", ex.getMessage());
    }
}
