package smart.fly.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.servlet.ModelAndView;

import smart.fly.adult.model.Adult;
import smart.fly.booking.search.repository.BookingSearchRepository;
import smart.fly.child.model.Child;
import smart.fly.flight.details.FlightDetails;
import smart.fly.passenger.model.Passenger;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentControllerTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    @Mock
    private BookingSearchRepository bookingRepository;

    private PaymentController paymentController;
    private MockHttpSession session;
    private FlightDetails flightDetails;

    private final int bookingId = 77;
    private final int flightId = 5;
    private final int seatClassId = 1;
    private final int userId = 1;

    @BeforeEach
    void setUp() throws SQLException {
        paymentController = new PaymentController(dataSource, bookingRepository);

        session = new MockHttpSession();
        session.setAttribute("flightId", flightId);
        session.setAttribute("seatClassId", seatClassId);
        session.setAttribute("numberOfAdults", 1);
        session.setAttribute("numberOfChildren", 1);
        session.setAttribute("adults", List.of(new Adult("Asha", "asha@example.com")));
        session.setAttribute("children", List.of(new Child("Mini", 6)));
        session.setAttribute("totalPrice", 3000.0);
        session.setAttribute("loggedInUserId", userId);

        flightDetails = new FlightDetails();
        flightDetails.setFlightNumber("AI101");
        flightDetails.setAirlineName("Air India");
        flightDetails.setDepartureAirportCode("DEL");
        flightDetails.setArrivalAirportCode("BOM");
        flightDetails.setDepartureDateTime("2026-11-01 10:00");

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true); // lead adult is a registered user
        when(bookingRepository.createBooking(flightId, seatClassId, 2, 3000.0, userId, 1, 1)).thenReturn(bookingId);
        when(bookingRepository.getFlightDetails(flightId)).thenReturn(flightDetails);
    }

    private ModelAndView pay(String mode, String cardType) {
        return paymentController.processPayment(mode, "4111111111111111", "Asha", "12/28", cardType, "asha@upi",
                "Paytm", session);
    }

    @Test
    void processPayment_shouldPersistBookingInOrderAndCommit() throws SQLException {
        // Act
        pay("Credit Card", "VISA");

        // Assert
        InOrder inOrder = inOrder(bookingRepository, connection);
        inOrder.verify(connection).setAutoCommit(false);
        inOrder.verify(bookingRepository).createBooking(flightId, seatClassId, 2, 3000.0, userId, 1, 1);
        inOrder.verify(bookingRepository).verifyBookingExists(bookingId);
        inOrder.verify(bookingRepository).createPassenger(bookingId, "Asha", "asha@example.com", null, true, userId);
        inOrder.verify(bookingRepository).createPassenger(bookingId, "Mini", null, 6, false, userId);
        inOrder.verify(bookingRepository).createPayment(eq(bookingId), eq("Credit Card"), eq("4111111111111111"),
                eq("Asha"), eq("12/28"), eq("VISA"), startsWith("DUMMY_"), eq("asha@upi"), eq("Paytm"), eq(3000.0));
        inOrder.verify(bookingRepository).updateFlightSeats(flightId, seatClassId, 2);
        inOrder.verify(bookingRepository).getFlightDetails(flightId);
        inOrder.verify(connection).commit();
        verify(connection, never()).rollback();
    }

    @Test
    void processPayment_shouldMarkAdultUnregistered_whenEmailNotFound() throws SQLException {
        // Arrange
        when(resultSet.next()).thenReturn(false);

        // Act
        pay("Credit Card", "VISA");

        // Assert
        verify(bookingRepository).createPassenger(bookingId, "Asha", "asha@example.com", null, false, userId);
    }

    @Test
    void processPayment_shouldReturnConfirmationViewWithBookingDetails() {
        // Act
        ModelAndView mv = pay("Credit Card", "VISA");

        // Assert
        assertEquals("booking/confirm", mv.getViewName());
        assertEquals(bookingId, mv.getModel().get("bookingId"));
        assertEquals("AI101", mv.getModel().get("flightNumber"));
        assertEquals("Air India", mv.getModel().get("airlineName"));
        assertEquals("DEL", mv.getModel().get("departureAirportCode"));
        assertEquals("BOM", mv.getModel().get("arrivalAirportCode"));
        assertEquals("2026-11-01 10:00", mv.getModel().get("departure_date"));
        assertEquals(2, mv.getModel().get("numberOfSeats"));
        assertEquals(3000.0, mv.getModel().get("totalPrice"));
        assertEquals("Credit Card", mv.getModel().get("paymentMode"));

        @SuppressWarnings("unchecked")
        List<Passenger> passengers = (List<Passenger>) mv.getModel().get("passengers");
        assertEquals(2, passengers.size());
        assertEquals("Asha", passengers.get(0).getName());
        assertEquals("Mini", passengers.get(1).getName());
        assertEquals(6, passengers.get(1).getAge());
    }

    @Test
    void processPayment_shouldClearBookingDataFromSession_whenSuccessful() {
        // Act
        pay("Credit Card", "VISA");

        // Assert
        for (String attr : List.of("flightId", "seatClassId", "numberOfAdults", "numberOfChildren", "adults",
                "children", "totalPrice")) {
            assertNull(session.getAttribute(attr), attr + " should be removed from session");
        }
        assertEquals(userId, session.getAttribute("loggedInUserId"));
    }

    @Test
    void processPayment_shouldStoreCardType_forDebitCard() throws SQLException {
        // Act
        pay("Debit Card", "MASTERCARD");

        // Assert
        verify(bookingRepository).createPayment(anyInt(), eq("Debit Card"), any(), any(), any(), eq("MASTERCARD"),
                anyString(), any(), any(), anyDouble());
    }

    @Test
    void processPayment_shouldNotStoreCardType_forNonCardPayments() throws SQLException {
        // Act
        pay("UPI", "VISA");

        // Assert
        verify(bookingRepository).createPayment(anyInt(), eq("UPI"), any(), any(), any(), isNull(), anyString(),
                any(), any(), anyDouble());
    }

    @Test
    void processPayment_shouldRollbackAndShowFailure_whenDatabaseFails() throws SQLException {
        // Arrange
        doThrow(new SQLException("insert failed")).when(bookingRepository).updateFlightSeats(flightId, seatClassId, 2);

        // Act
        ModelAndView mv = pay("Credit Card", "VISA");

        // Assert
        assertEquals("paymentFailed", mv.getViewName());
        verify(connection).rollback();
        verify(connection, never()).commit();
        assertEquals(flightId, session.getAttribute("flightId")); // session kept so the user can retry
    }

    @Test
    void processPayment_shouldShowFailure_whenConnectionUnavailable() throws SQLException {
        // Arrange
        when(dataSource.getConnection()).thenThrow(new SQLException("no connection"));

        // Act
        ModelAndView mv = pay("Credit Card", "VISA");

        // Assert
        assertEquals("paymentFailed", mv.getViewName());
        verifyNoInteractions(bookingRepository);
    }

    @Test
    void processPayment_shouldCloseConnection() throws SQLException {
        // Act
        pay("Credit Card", "VISA");

        // Assert
        verify(connection, atLeastOnce()).close();
    }
}
