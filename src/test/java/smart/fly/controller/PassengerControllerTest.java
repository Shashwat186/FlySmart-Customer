package smart.fly.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import smart.fly.booking.model.Booking;
import smart.fly.booking.repository.BookingRepository;
import smart.fly.service.BookingService;
import smart.fly.service.FeedbackService;
import smart.fly.service.PassengerService;
import smart.fly.service.UserService;
import smart.fly.user.model.User;

@WebMvcTest(PassengerController.class)
class PassengerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PassengerService passengerService;

    @MockitoBean
    private BookingService bookingService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private BookingRepository bookingRepository;

    @MockitoBean
    private FeedbackService feedbackService;

    private User user;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUserId(1);
        user.setName("Asha");
        session = new MockHttpSession();
        session.setAttribute("user", user);
    }

    // ---------- dashboard ----------

    @Test
    void showDashboard_shouldReturnDashboardWithSessionUser() throws Exception {
        mockMvc.perform(get("/passenger/dashboard").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("passenger/dashboard"))
                .andExpect(model().attribute("passenger", user));
    }

    @Test
    void showDashboard_shouldFailWithServerError_whenNotLoggedIn() throws Exception {
        // Known bug: the handler has no login check and throws a NullPointerException,
        // which GlobalExceptionHandler turns into a 500. It should redirect to /auth/login.
        mockMvc.perform(get("/passenger/dashboard")).andExpect(status().isInternalServerError());
    }

    // ---------- bookings ----------

    @Test
    void showBookings_shouldRedirectToLogin_whenNotLoggedIn() throws Exception {
        mockMvc.perform(get("/passenger/bookings")).andExpect(redirectedUrl("/auth/login"));

        verifyNoInteractions(bookingRepository, feedbackService);
    }

    @Test
    void showBookings_shouldPopulateUpcomingAndPreviousBookings() throws Exception {
        // Arrange
        List<Booking> upcoming = List.of(new Booking());
        when(bookingRepository.findUpcomingBookingsByUserId(eq(1), any(LocalDateTime.class))).thenReturn(upcoming);
        when(bookingRepository.findPreviousBookingsByUserId(eq(1), any(LocalDateTime.class))).thenReturn(List.of());
        when(feedbackService.getBookingsWithFeedback(1)).thenReturn(List.of(5, 6));

        // Act & Assert
        mockMvc.perform(get("/passenger/bookings").session(session))
                .andExpect(view().name("passenger/bookings"))
                .andExpect(model().attribute("upcomingBookings", upcoming))
                .andExpect(model().attribute("previousBookings", List.of()))
                .andExpect(model().attribute("bookingsWithFeedback", List.of(5, 6)))
                .andExpect(model().attribute("upcomingBookingsEmpty", false))
                .andExpect(model().attribute("previousBookingsEmpty", true))
                .andExpect(model().attributeDoesNotExist("feedbackMessage", "feedbackError"));
    }

    @Test
    void showBookings_shouldMoveFeedbackMessagesFromSessionToModel() throws Exception {
        // Arrange
        session.setAttribute("feedbackMessage", "Thanks!");
        session.setAttribute("feedbackError", "Oops");
        when(bookingRepository.findUpcomingBookingsByUserId(eq(1), any(LocalDateTime.class))).thenReturn(List.of());
        when(bookingRepository.findPreviousBookingsByUserId(eq(1), any(LocalDateTime.class))).thenReturn(List.of());

        // Act
        mockMvc.perform(get("/passenger/bookings").session(session))
                .andExpect(model().attribute("feedbackMessage", "Thanks!"))
                .andExpect(model().attribute("feedbackError", "Oops"));

        // Assert
        assertNull(session.getAttribute("feedbackMessage"));
        assertNull(session.getAttribute("feedbackError"));
    }

    @Test
    void viewBookingDetails_shouldExposeBooking() throws Exception {
        // Arrange
        Booking booking = new Booking();
        booking.setBookingId(9);
        when(bookingService.getBookingById(9)).thenReturn(booking);

        // Act & Assert
        mockMvc.perform(get("/passenger/bookings/9").session(session))
                .andExpect(view().name("passenger/booking-details"))
                .andExpect(model().attribute("booking", booking));
    }

    // ---------- cancel ----------

    @Test
    void cancelBooking_shouldRedirectToLogin_whenNotLoggedIn() throws Exception {
        mockMvc.perform(get("/passenger/cancel/9")).andExpect(redirectedUrl("/auth/login"));

        verifyNoInteractions(bookingService);
    }

    @Test
    void cancelBooking_shouldFlashSuccess_whenCancelled() throws Exception {
        // Arrange
        when(bookingService.cancelBooking(9, 1)).thenReturn(true);

        // Act & Assert
        mockMvc.perform(get("/passenger/cancel/9").session(session))
                .andExpect(redirectedUrl("/passenger/bookings"))
                .andExpect(flash().attribute("message", "Booking ID 9 has been cancelled successfully."));
    }

    @Test
    void cancelBooking_shouldFlashError_whenCancellationFails() throws Exception {
        // Arrange
        when(bookingService.cancelBooking(9, 1)).thenReturn(false);

        // Act & Assert
        mockMvc.perform(get("/passenger/cancel/9").session(session))
                .andExpect(redirectedUrl("/passenger/bookings"))
                .andExpect(flash().attribute("error", "Failed to cancel booking ID 9. Please try again."));
    }

    @Test
    void cancelBooking_shouldFlashError_whenServiceThrows() throws Exception {
        // Arrange
        when(bookingService.cancelBooking(9, 1)).thenThrow(new RuntimeException("DB down"));

        // Act & Assert
        mockMvc.perform(get("/passenger/cancel/9").session(session))
                .andExpect(redirectedUrl("/passenger/bookings"))
                .andExpect(flash().attribute("error", "An error occurred while cancelling booking ID 9: DB down"));
    }

    // ---------- feedback ----------

    @Test
    void submitFeedback_shouldRedirectToLogin_whenNotLoggedIn() throws Exception {
        mockMvc.perform(post("/passenger/submit-feedback").param("bookingId", "9").param("rating", "5")
                .param("feedbackContent", "Great"))
                .andExpect(redirectedUrl("/auth/login"));

        verifyNoInteractions(feedbackService);
    }

    @Test
    void submitFeedback_shouldSetSuccessMessage_whenSubmitted() throws Exception {
        // Arrange
        when(feedbackService.submitFeedback(1, 9, 5, "Great")).thenReturn(true);

        // Act
        mockMvc.perform(post("/passenger/submit-feedback").session(session).param("bookingId", "9")
                .param("rating", "5").param("feedbackContent", "Great"))
                .andExpect(redirectedUrl("/passenger/bookings"));

        // Assert
        assertEquals("Feedback submitted successfully!", session.getAttribute("feedbackMessage"));
        assertNull(session.getAttribute("feedbackError"));
    }

    @Test
    void submitFeedback_shouldSetErrorMessage_whenSubmissionFails() throws Exception {
        // Arrange
        when(feedbackService.submitFeedback(1, 9, 5, "Great")).thenReturn(false);

        // Act
        mockMvc.perform(post("/passenger/submit-feedback").session(session).param("bookingId", "9")
                .param("rating", "5").param("feedbackContent", "Great"))
                .andExpect(redirectedUrl("/passenger/bookings"));

        // Assert
        assertEquals("Failed to submit feedback.", session.getAttribute("feedbackError"));
        assertNull(session.getAttribute("feedbackMessage"));
    }
}
