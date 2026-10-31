package smart.fly.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import smart.fly.support.AbstractMySqlIntegrationTest;
import smart.fly.user.model.User;

/**
 * Drives the booking journey the way the browser does (search → passenger form → payment page →
 * pay), through the real controllers, services and SQL, with one HTTP session carried across steps.
 *
 * <p>The tests named after a known bug are characterization tests: they pin down what the code does
 * TODAY, including the wrong behaviour, so the fix has to be a deliberate change to the test.
 */
@Sql({ "/db/clean.sql", "/db/seed-flights.sql" })
class BookingFlowIntegrationTest extends AbstractMySqlIntegrationTest {

	// Seed data: flight 1 (SW-101, DEL → BOM), Economy = seat class 1 with 10 of 100 seats left at 5000.
	private static final String FLIGHT_ID = "1";
	private static final String ECONOMY = "1";
	private static final int ECONOMY_SEATS_LEFT = 10;

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private MockMvc mockMvc;
	private MockHttpSession session;

	@BeforeEach
	void setUp() {
		// Built by hand rather than with @AutoConfigureMockMvc: adding that annotation here would
		// change the context configuration and start a second Spring context and MySQL container.
		mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
		session = loggedInAs(1);
	}

	@Test
	void searchBookAndPayWithUpi_savesBookingPassengersPaymentAndSeats() throws Exception {
		searchFlights().andExpect(model().attribute("flights", hasSize(1)));
		openPassengerForm();
		submitPassengers("15000");

		pay("UPI", Map.of("upiId", "asha@upi"))
				.andExpect(view().name("booking/confirm"))
				.andExpect(model().attribute("flightNumber", "SW-101"))
				.andExpect(model().attribute("airlineName", "SkyWays"))
				.andExpect(model().attribute("departureAirportCode", "DEL"))
				.andExpect(model().attribute("arrivalAirportCode", "BOM"))
				.andExpect(model().attribute("numberOfSeats", 3))
				.andExpect(model().attribute("totalPrice", 15000.0));

		Map<String, Object> booking = jdbcTemplate.queryForMap("SELECT * FROM booking");
		assertEquals("BOOKED", booking.get("status"));
		assertEquals(3, booking.get("Number_of_Seats"));
		assertEquals(2, booking.get("number_of_adults"));
		assertEquals(1, booking.get("number_of_children"));
		assertEquals(1, booking.get("User_ID"));

		// Asha has an account (seed user 1), Ravi does not; the child has an age and no email.
		assertEquals(Boolean.TRUE, passengerColumn("Asha", "Is_Registered"));
		assertEquals(Boolean.FALSE, passengerColumn("Ravi", "Is_Registered"));
		assertEquals(8, passengerColumn("Kiran", "age"));
		assertEquals(3, count("passenger"));

		Map<String, Object> payment = jdbcTemplate.queryForMap("SELECT * FROM payment");
		assertEquals("UPI", payment.get("Payment_Mode"));
		assertEquals("asha@upi", payment.get("upi_id"));
		assertEquals(15000.0, payment.get("Amount"));

		assertEquals(ECONOMY_SEATS_LEFT - 3, economySeatsLeft());
		assertNull(session.getAttribute("flightId"), "Booking data should be cleared from the session");
	}

	@Test
	void knownBug_priceIsTakenFromTheBrowserNotTheDatabase() throws Exception {
		// 3 Economy seats cost 3 x 5000 = 15000, but the server trusts the hidden "price" form field.
		searchFlights();
		openPassengerForm();
		submitPassengers("1");

		pay("UPI", Map.of("upiId", "asha@upi")).andExpect(view().name("booking/confirm"));

		assertEquals(1.0, jdbcTemplate.queryForObject("SELECT total_price FROM booking", Double.class));
		assertEquals(1.0, jdbcTemplate.queryForObject("SELECT Amount FROM payment", Double.class));
	}

	@Test
	void knownBug_fullCardNumberIsStoredInPlainText() throws Exception {
		searchFlights();
		openPassengerForm();
		submitPassengers("15000");

		pay("Credit Card", Map.of("cardNumber", "4111 1111 1111 1111", "cardHolderName", "Asha", "expiryDate",
				"12/30")).andExpect(view().name("booking/confirm"));

		Map<String, Object> payment = jdbcTemplate.queryForMap("SELECT * FROM payment");
		assertEquals("4111 1111 1111 1111", payment.get("card_number"));
		assertEquals("12/30", payment.get("expiry_date"));
	}

	@Test
	void knownBug_failedPaymentLeavesHalfABookingBehind() throws Exception {
		searchFlights();
		openPassengerForm();
		submitPassengers("15000");

		// The page only offers Credit Card / Debit Card / UPI, but the server does not validate the
		// value, and the payment.Payment_Mode enum rejects anything else. So the payment insert fails
		// AFTER the booking and passengers were written.
		pay("Wallet", Map.of("walletName", "Paytm")).andExpect(view().name("paymentFailed"));

		// The controller calls rollback(), but on a connection that never wrote anything: each
		// repository call used its own auto-committed connection. A working transaction would leave
		// 0 bookings and 0 passengers here.
		assertEquals(1, count("booking"));
		assertEquals(3, count("passenger"));
		assertEquals(0, count("payment"));
		assertEquals(ECONOMY_SEATS_LEFT, economySeatsLeft());
	}

	// --- Steps of the journey, in the order the browser performs them ---

	private ResultActions searchFlights() throws Exception {
		return mockMvc.perform(get("/booking/search").session(session)
				.param("from", "1").param("to", "2").param("date", "2030-01-01")
				.param("seatClass", ECONOMY).param("adults", "2").param("children", "1"))
				.andExpect(view().name("booking/search"))
				// The controller turns SQL failures into an "error" message instead of failing.
				.andExpect(model().attributeDoesNotExist("error"));
	}

	private void openPassengerForm() throws Exception {
		mockMvc.perform(get("/booking/new").session(session)
				.param("flightId", FLIGHT_ID).param("seatClassId", ECONOMY)
				.param("adults", "2").param("children", "1"))
				.andExpect(view().name("booking/form"))
				.andExpect(model().attributeExists("flight", "seatClasses", "availableSeats"));
	}

	private void submitPassengers(String price) throws Exception {
		mockMvc.perform(post("/booking/payment").session(session)
				.param("flightId", FLIGHT_ID).param("seatClassId", ECONOMY)
				.param("numberOfAdults", "2").param("numberOfChildren", "1")
				.param("price", price)
				.param("adults[0].name", "Asha").param("adults[0].email", "asha@example.com")
				.param("adults[1].name", "Ravi").param("adults[1].email", "ravi@example.com")
				.param("children[0].name", "Kiran").param("children[0].age", "8"))
				.andExpect(view().name("payment"));
	}

	private ResultActions pay(String paymentMode, Map<String, String> details) throws Exception {
		var request = post("/booking/processPayment").session(session).param("paymentMode", paymentMode);
		details.forEach(request::param);
		return mockMvc.perform(request);
	}

	// --- Helpers ---

	private static MockHttpSession loggedInAs(int userId) {
		// Mirrors what AuthController.processLogin puts in the session.
		User user = new User();
		user.setUserId(userId);
		user.setRole("Passenger");
		MockHttpSession session = new MockHttpSession();
		session.setAttribute("user", user);
		session.setAttribute("loggedInUserId", userId);
		return session;
	}

	private Object passengerColumn(String name, String column) {
		return jdbcTemplate.queryForMap("SELECT * FROM passenger WHERE name = ?", name).get(column);
	}

	private int count(String table) {
		return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
	}

	private int economySeatsLeft() {
		return jdbcTemplate.queryForObject(
				"SELECT Available_Seats FROM flight_seats WHERE Flight_ID = 1 AND Seat_Class_ID = 1", Integer.class);
	}
}
