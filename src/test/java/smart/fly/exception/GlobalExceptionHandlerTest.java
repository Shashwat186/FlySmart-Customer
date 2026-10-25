package smart.fly.exception;

import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleAuthException_shouldReturnUnauthorizedWithMessage() {
        // Act
        ResponseEntity<String> response = handler.handleAuthException(new AuthenticationException("Invalid login"));

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Invalid login", response.getBody());
    }

    static Stream<RuntimeException> businessExceptions() {
        return Stream.of(new BookingException("No seats"), new FlightManagementException("Flight not found"),
                new FeedbackException("Already reviewed"));
    }

    @ParameterizedTest
    @MethodSource("businessExceptions")
    void handleBusinessExceptions_shouldReturnBadRequestWithMessage(RuntimeException ex) {
        // Act
        ResponseEntity<String> response = handler.handleBusinessExceptions(ex);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(ex.getMessage(), response.getBody());
    }

    @Test
    void handleGenericException_shouldReturnInternalServerErrorWithoutLeakingDetails() {
        // Act
        ResponseEntity<String> response = handler.handleGenericException(new IllegalStateException("secret SQL"));

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("An unexpected error occurred", response.getBody());
    }
}
