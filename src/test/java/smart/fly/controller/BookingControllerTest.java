package smart.fly.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import smart.fly.adult.model.Adult;
import smart.fly.airport.model.Airport;
import smart.fly.airport.repository.AirportRepository;
import smart.fly.child.model.Child;
import smart.fly.dto.FlightSearchDTO;
import smart.fly.flight.model.Flight;
import smart.fly.flight.repository.FlightRepository;
import smart.fly.flight.search.repository.FlightSearchRepository;
import smart.fly.seatclass.model.SeatClass;
import smart.fly.seatclass.repository.SeatClassRepository;
import smart.fly.service.BookingService;
import smart.fly.service.PassengerService;
import smart.fly.service.UserService;
import smart.fly.user.model.User;

@WebMvcTest(BookingController.class)
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookingService bookingService;

    @MockitoBean
    private PassengerService passengerService;

    @MockitoBean
    private AirportRepository airportRepository;

    @MockitoBean
    private FlightRepository flightRepository;

    @MockitoBean
    private SeatClassRepository seatClassRepository;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private DataSource dataSource;

    @MockitoBean
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private FlightSearchRepository flightSearchRepository;

    private List<Airport> airports;
    private SeatClass economy;
    private SeatClass business;

    @BeforeEach
    void setUp() {
        airports = List.of(new Airport(), new Airport());

        economy = new SeatClass();
        economy.setSeatClassId(1);
        business = new SeatClass();
        business.setSeatClassId(2);
    }

    // ---------- search ----------

    @Test
    void searchFlights_shouldOnlyLoadFormData_whenNoRouteGiven() throws Exception {
        // Arrange
        when(airportRepository.findAllForSearch()).thenReturn(airports);
        when(seatClassRepository.findAll()).thenReturn(List.of(economy, business));

        // Act & Assert
        mockMvc.perform(get("/booking/search"))
                .andExpect(status().isOk())
                .andExpect(view().name("booking/search"))
                .andExpect(model().attribute("airports", airports))
                .andExpect(model().attribute("seatClasses", List.of(economy, business)))
                .andExpect(model().attribute("currentDate", LocalDate.now().toString()))
                .andExpect(model().attributeDoesNotExist("flights", "searchParams", "error"));

        verifyNoInteractions(flightSearchRepository);
    }

    @Test
    void searchFlights_shouldSearchWithTotalPassengers_whenRouteGiven() throws Exception {
        // Arrange
        List<FlightSearchDTO> flights = List.of(new FlightSearchDTO());
        when(airportRepository.findAllForSearch()).thenReturn(airports);
        when(flightSearchRepository.searchFlights(1, 2, Date.valueOf("2026-11-01"), 1, 3)).thenReturn(flights);

        // Act & Assert
        mockMvc.perform(get("/booking/search").param("from", "1").param("to", "2").param("date", "2026-11-01")
                .param("seatClass", "1").param("adults", "2").param("children", "1"))
                .andExpect(view().name("booking/search"))
                .andExpect(model().attribute("flights", flights))
                .andExpect(model().attribute("searchParams", Map.of("from", 1, "to", 2, "date", "2026-11-01",
                        "seatClass", 1, "adults", 2, "children", 1)));
    }

    @Test
    void searchFlights_shouldDefaultToTodayAndOneAdult_whenDateAndPassengersMissing() throws Exception {
        // Arrange
        when(airportRepository.findAllForSearch()).thenReturn(airports);
        when(flightSearchRepository.searchFlights(1, 2, Date.valueOf(LocalDate.now()), null, 1))
                .thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/booking/search").param("from", "1").param("to", "2"))
                .andExpect(model().attribute("flights", List.of()));

        verify(flightSearchRepository).searchFlights(1, 2, Date.valueOf(LocalDate.now()), null, 1);
    }

    @Test
    void searchFlights_shouldShowFormatError_whenDateInvalid() throws Exception {
        // Arrange
        when(airportRepository.findAllForSearch()).thenReturn(airports);

        // Act & Assert
        mockMvc.perform(get("/booking/search").param("from", "1").param("to", "2").param("date", "01/11/2026"))
                .andExpect(view().name("booking/search"))
                .andExpect(model().attribute("error", "Invalid date format. Please use YYYY-MM-DD format."))
                .andExpect(model().attributeDoesNotExist("flights"));

        verifyNoInteractions(flightSearchRepository);
    }

    @Test
    void searchFlights_shouldShowValidationMessage_whenIllegalArgument() throws Exception {
        // Arrange
        when(airportRepository.findAllForSearch()).thenReturn(airports);
        when(flightSearchRepository.searchFlights(anyInt(), anyInt(), any(Date.class), any(), anyInt()))
                .thenThrow(new IllegalArgumentException("Departure and arrival airports must differ"));

        // Act & Assert
        mockMvc.perform(get("/booking/search").param("from", "1").param("to", "1"))
                .andExpect(model().attribute("error", "Departure and arrival airports must differ"));
    }

    @Test
    void searchFlights_shouldShowRootCause_whenSearchFails() throws Exception {
        // Arrange
        when(airportRepository.findAllForSearch()).thenReturn(airports);
        when(flightSearchRepository.searchFlights(anyInt(), anyInt(), any(Date.class), any(), anyInt()))
                .thenThrow(new RuntimeException("wrapper", new SQLException("db down")));

        // Act & Assert
        mockMvc.perform(get("/booking/search").param("from", "1").param("to", "2"))
                .andExpect(view().name("booking/search"))
                .andExpect(model().attribute("error", "Search failed: db down"));
    }

    // ---------- booking form ----------

    @Test
    void showBookingForm_shouldPopulateFlightSeatClassesAndAvailability() throws Exception {
        // Arrange
        Flight flight = new Flight();
        flight.setFlightId(5);
        when(flightRepository.findById(5)).thenReturn(flight);
        when(seatClassRepository.findAll()).thenReturn(List.of(economy, business));
        when(seatClassRepository.findByFlightIdWithAvailableSeats(5)).thenReturn(List.of(economy));
        when(seatClassRepository.getAvailableSeatsForFlightSeatClass(5, 1)).thenReturn(10);
        when(seatClassRepository.getAvailableSeatsForFlightSeatClass(5, 2)).thenReturn(null);

        // Act & Assert
        mockMvc.perform(get("/booking/new").param("flightId", "5").param("seatClassId", "1")
                .param("adults", "2").param("children", "1"))
                .andExpect(view().name("booking/form"))
                .andExpect(model().attribute("flight", flight))
                .andExpect(model().attribute("seatClasses", List.of(economy)))
                .andExpect(model().attribute("availableSeats", Map.of(1, 10, 2, 0)))
                .andExpect(model().attribute("selectedSeatClassId", 1))
                .andExpect(model().attribute("bookingRequest",
                        Matchers.allOf(Matchers.hasProperty("flightId", Matchers.is(5)),
                                Matchers.hasProperty("numberOfAdults", Matchers.is(2)),
                                Matchers.hasProperty("numberOfChildren", Matchers.is(1)))))
                .andExpect(model().attributeDoesNotExist("loggedInPassenger"));
    }

    @Test
    void showBookingForm_shouldExposeLoggedInPassenger_whenUserInSession() throws Exception {
        // Arrange
        User sessionUser = new User();
        sessionUser.setUserId(1);
        User freshUser = new User();
        freshUser.setUserId(1);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", sessionUser);
        when(flightRepository.findById(5)).thenReturn(new Flight());
        when(userService.getUserById(1)).thenReturn(freshUser);

        // Act & Assert
        mockMvc.perform(get("/booking/new").param("flightId", "5").session(session))
                .andExpect(view().name("booking/form"))
                .andExpect(model().attribute("loggedInPassenger", freshUser));
    }

    @Test
    void showBookingForm_shouldReturnErrorView_whenFlightNotFound() throws Exception {
        // Arrange
        when(flightRepository.findById(5)).thenReturn(null);

        // Act & Assert
        mockMvc.perform(get("/booking/new").param("flightId", "5"))
                .andExpect(view().name("error"))
                .andExpect(model().attribute("error", "Flight not found."));

        verifyNoInteractions(seatClassRepository);
    }

    // ---------- payment page ----------

    @Test
    @SuppressWarnings("unchecked")
    void showPaymentPage_shouldStoreTravellersAndPriceInSession() throws Exception {
        // Arrange
        MockHttpSession session = new MockHttpSession();

        // Act
        mockMvc.perform(post("/booking/payment").session(session)
                .param("flightId", "5").param("seatClassId", "1")
                .param("numberOfAdults", "2").param("numberOfChildren", "1").param("price", "4500.50")
                .param("adults[0].name", "Asha").param("adults[0].email", "asha@example.com")
                .param("adults[1].name", "Ravi").param("adults[1].email", "ravi@example.com")
                .param("children[0].name", "Mini").param("children[0].age", "6"))
                .andExpect(view().name("payment"));

        // Assert
        assertEquals(5, session.getAttribute("flightId"));
        assertEquals(1, session.getAttribute("seatClassId"));
        assertEquals(2, session.getAttribute("numberOfAdults"));
        assertEquals(1, session.getAttribute("numberOfChildren"));
        assertEquals(4500.50, session.getAttribute("totalPrice"));

        List<Adult> adults = (List<Adult>) session.getAttribute("adults");
        assertEquals(2, adults.size());
        assertEquals("Asha", adults.get(0).getName());
        assertEquals("asha@example.com", adults.get(0).getEmail());
        assertEquals("Ravi", adults.get(1).getName());

        List<Child> children = (List<Child>) session.getAttribute("children");
        assertEquals(1, children.size());
        assertEquals("Mini", children.get(0).getName());
        assertEquals(6, children.get(0).getAge());
    }

    @Test
    @SuppressWarnings("unchecked")
    void showPaymentPage_shouldSkipTravellersWithMissingDetails() throws Exception {
        // Arrange
        MockHttpSession session = new MockHttpSession();

        // Act
        mockMvc.perform(post("/booking/payment").session(session)
                .param("flightId", "5").param("seatClassId", "1")
                .param("numberOfAdults", "2").param("numberOfChildren", "1").param("price", "100")
                .param("adults[0].name", "Asha")
                .param("children[0].name", "Mini"))
                .andExpect(view().name("payment"));

        // Assert
        assertEquals(1, ((List<Adult>) session.getAttribute("adults")).size());
        assertTrue(((List<Child>) session.getAttribute("children")).isEmpty());
    }

    @Test
    void showPaymentPage_shouldDefaultPriceToZero_whenPriceNotNumeric() throws Exception {
        // Arrange
        MockHttpSession session = new MockHttpSession();

        // Act
        mockMvc.perform(post("/booking/payment").session(session)
                .param("flightId", "5").param("seatClassId", "1")
                .param("numberOfAdults", "1").param("numberOfChildren", "0").param("price", "abc")
                .param("adults[0].name", "Asha"))
                .andExpect(view().name("payment"));

        // Assert
        assertEquals(0.0, session.getAttribute("totalPrice"));
    }
}
