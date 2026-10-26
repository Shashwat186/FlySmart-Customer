package smart.fly.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import smart.fly.booking.model.Booking;
import smart.fly.booking.repository.BookingRepository;
import smart.fly.exception.BookingException;
import smart.fly.flight.model.Flight;
import smart.fly.flight.repository.FlightRepository;
import smart.fly.flight.seats.model.FlightSeats;
import smart.fly.flight.seats.repository.FlightSeatsRepository;
import smart.fly.passenger.model.Passenger;
import smart.fly.passenger.repository.PassengerRepository;
import smart.fly.payment.model.Payment;
import smart.fly.payment.repository.PaymentRepository;
import smart.fly.seatclass.model.SeatClass;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private FlightRepository flightRepository;

    @Mock
    private PassengerRepository passengerRepository;

    @Mock
    private FlightSeatsRepository flightSeatsRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private BookingService bookingService;

    private final int passengerId = 7;
    private final int flightId = 10;
    private final int seatClassId = 2;
    private final int flightSeatId = 55;

    private Flight flight;
    private FlightSeats flightSeats;
    private SeatClass seatClass;
    private Passenger passenger;

    @BeforeEach
    void setUp() {
        flight = new Flight();
        flight.setFlightId(flightId);
        flight.setStatus("SCHEDULED");

        seatClass = new SeatClass();
        seatClass.setSeatClassId(seatClassId);

        flightSeats = new FlightSeats(flightSeatId, 20, 100, 1500.0);
        flightSeats.setSeatClass(seatClass);

        passenger = new Passenger();
        passenger.setPassengerId(passengerId);
    }

    // ---------- createBooking ----------

    @Test
    void createBooking_shouldSaveBookingDecrementSeatsAndSavePayment_whenSeatsAvailable() {
        // Arrange
        Payment payment = new Payment();
        when(flightRepository.findById(flightId)).thenReturn(flight);
        when(flightSeatsRepository.findByFlightAndClass(flightId, seatClassId)).thenReturn(flightSeats);
        when(passengerRepository.findById(passengerId)).thenReturn(passenger);

        // Act
        Booking result = bookingService.createBooking(passengerId, flightId, seatClassId, 3, payment);

        // Assert
        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(bookingCaptor.capture());
        Booking saved = bookingCaptor.getValue();
        assertSame(result, saved);
        assertSame(passenger, saved.getPassenger());
        assertSame(flight, saved.getFlight());
        assertSame(seatClass, saved.getSeatClass());
        assertEquals(3, saved.getNumberOfSeats());
        assertEquals(4500.0, saved.getTotalPrice());
        assertNotNull(saved.getBookingDateTime());

        verify(flightSeatsRepository).updateAvailableSeats(flightSeatId, 17);
        verify(paymentRepository).save(payment);
        assertSame(saved, payment.getBooking());
    }

    @Test
    void createBooking_shouldAllowBookingAllRemainingSeats() {
        // Arrange
        when(flightRepository.findById(flightId)).thenReturn(flight);
        when(flightSeatsRepository.findByFlightAndClass(flightId, seatClassId)).thenReturn(flightSeats);

        // Act
        bookingService.createBooking(passengerId, flightId, seatClassId, 20, new Payment());

        // Assert
        verify(flightSeatsRepository).updateAvailableSeats(flightSeatId, 0);
    }

    @Test
    void createBooking_shouldAcceptStatusStoredInDatabaseCase() {
        // Arrange: the flight table stores "Scheduled", not "SCHEDULED"
        flight.setStatus("Scheduled");
        when(flightRepository.findById(flightId)).thenReturn(flight);
        when(flightSeatsRepository.findByFlightAndClass(flightId, seatClassId)).thenReturn(flightSeats);

        // Act
        Booking booking = bookingService.createBooking(passengerId, flightId, seatClassId, 1, new Payment());

        // Assert
        assertNotNull(booking);
        verify(bookingRepository).save(booking);
    }

    @Test
    void createBooking_shouldThrow_whenFlightIsNotScheduled() {
        // Arrange
        flight.setStatus("CANCELLED");
        when(flightRepository.findById(flightId)).thenReturn(flight);

        // Act
        BookingException ex = assertThrows(BookingException.class,
                () -> bookingService.createBooking(passengerId, flightId, seatClassId, 1, new Payment()));

        // Assert
        assertEquals("Flight is not available for booking", ex.getMessage());
        verifyNoInteractions(flightSeatsRepository, bookingRepository, paymentRepository);
    }

    @Test
    void createBooking_shouldThrow_whenNotEnoughSeats() {
        // Arrange
        when(flightRepository.findById(flightId)).thenReturn(flight);
        when(flightSeatsRepository.findByFlightAndClass(flightId, seatClassId)).thenReturn(flightSeats);

        // Act
        BookingException ex = assertThrows(BookingException.class,
                () -> bookingService.createBooking(passengerId, flightId, seatClassId, 21, new Payment()));

        // Assert
        assertEquals("Not enough seats available", ex.getMessage());
        verify(flightSeatsRepository, never()).updateAvailableSeats(anyInt(), anyInt());
        verifyNoInteractions(bookingRepository, paymentRepository);
    }

    // ---------- cancelBooking(bookingId) ----------

    @Test
    void cancelBooking_shouldReturnSeatsToInventoryAndCancel() {
        // Arrange
        Booking booking = new Booking();
        booking.setBookingId(99);
        booking.setFlight(flight);
        booking.setSeatClass(seatClass);
        booking.setNumberOfSeats(4);
        when(bookingRepository.findById(99)).thenReturn(booking);
        when(flightSeatsRepository.findByFlightAndClass(flightId, seatClassId)).thenReturn(flightSeats);

        // Act
        bookingService.cancelBooking(99);

        // Assert
        verify(flightSeatsRepository).updateAvailableSeats(flightSeatId, 24);
        verify(bookingRepository).cancel(99);
    }

    // ---------- cancelBooking(bookingId, userId) ----------

    private Booking bookingOwnedBy(int userId, String status) {
        Booking booking = new Booking();
        booking.setBookingId(99);
        booking.setUserId(userId);
        booking.setStatus(status);
        return booking;
    }

    @Test
    void cancelBookingForUser_shouldReturnFalse_whenBookingNotFound() {
        // Arrange
        when(bookingRepository.findBookingDetailsById(99)).thenReturn(Optional.empty());

        // Act & Assert
        assertFalse(bookingService.cancelBooking(99, 1));
        verify(bookingRepository, never()).updateBookingStatus(anyInt(), anyString());
    }

    @Test
    void cancelBookingForUser_shouldReturnFalse_whenBookingBelongsToAnotherUser() {
        // Arrange
        when(bookingRepository.findBookingDetailsById(99)).thenReturn(Optional.of(bookingOwnedBy(2, "BOOKED")));

        // Act & Assert
        assertFalse(bookingService.cancelBooking(99, 1));
        verify(bookingRepository, never()).updateBookingStatus(anyInt(), anyString());
    }

    @Test
    void cancelBookingForUser_shouldReturnTrueWithoutUpdate_whenAlreadyCancelled() {
        // Arrange
        when(bookingRepository.findBookingDetailsById(99)).thenReturn(Optional.of(bookingOwnedBy(1, "cancelled")));

        // Act & Assert
        assertTrue(bookingService.cancelBooking(99, 1));
        verify(bookingRepository, never()).updateBookingStatus(anyInt(), anyString());
    }

    @Test
    void cancelBookingForUser_shouldReturnTrue_whenStatusUpdated() {
        // Arrange
        when(bookingRepository.findBookingDetailsById(99)).thenReturn(Optional.of(bookingOwnedBy(1, "BOOKED")));
        when(bookingRepository.updateBookingStatus(99, "CANCELLED")).thenReturn(1);

        // Act & Assert
        assertTrue(bookingService.cancelBooking(99, 1));
    }

    @Test
    void cancelBookingForUser_shouldReturnFalse_whenNoRowsUpdated() {
        // Arrange
        when(bookingRepository.findBookingDetailsById(99)).thenReturn(Optional.of(bookingOwnedBy(1, "BOOKED")));
        when(bookingRepository.updateBookingStatus(99, "CANCELLED")).thenReturn(0);

        // Act & Assert
        assertFalse(bookingService.cancelBooking(99, 1));
    }

    @Test
    void cancelBookingForUser_shouldReturnFalse_whenStatusIsNull() {
        // Arrange
        when(bookingRepository.findBookingDetailsById(99)).thenReturn(Optional.of(bookingOwnedBy(1, null)));

        // Act & Assert
        assertFalse(bookingService.cancelBooking(99, 1));
        verify(bookingRepository, never()).updateBookingStatus(anyInt(), anyString());
    }

    // ---------- simple delegations ----------

    @Test
    void getBookingById_shouldDelegateToRepository() {
        // Arrange
        Booking booking = new Booking();
        when(bookingRepository.findById(5)).thenReturn(booking);

        // Act & Assert
        assertSame(booking, bookingService.getBookingById(5));
    }

    @Test
    void getPassengerBookings_shouldDelegateToRepository() {
        // Arrange
        List<Booking> bookings = List.of(new Booking(), new Booking());
        when(bookingRepository.findByPassenger(passengerId)).thenReturn(bookings);

        // Act & Assert
        assertEquals(bookings, bookingService.getPassengerBookings(passengerId));
    }

    @Test
    void getFlightById_shouldDelegateToRepository() {
        // Arrange
        when(flightRepository.findById(flightId)).thenReturn(flight);

        // Act & Assert
        assertSame(flight, bookingService.getFlightById(flightId));
    }
}
