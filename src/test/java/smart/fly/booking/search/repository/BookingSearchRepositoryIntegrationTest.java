package smart.fly.booking.search.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.SQLException;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

import smart.fly.flight.details.FlightDetails;
import smart.fly.support.AbstractMySqlIntegrationTest;

/**
 * Runs the SQL behind the payment flow (PaymentController.processPayment) against real MySQL.
 * The existing PaymentControllerTest mocks this repository, so none of this SQL was ever executed.
 */
@Sql({ "/db/clean.sql", "/db/seed-flights.sql" })
class BookingSearchRepositoryIntegrationTest extends AbstractMySqlIntegrationTest {

	@Autowired
	private BookingSearchRepository repository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void getFlightDetails_joinsAirlineAndAirports() throws SQLException {
		// Regression test: this query used `Airline`/`Airport` and failed on case-sensitive MySQL.
		FlightDetails details = repository.getFlightDetails(1);

		assertNotNull(details);
		assertEquals("SW-101", details.getFlightNumber());
		assertEquals("SkyWays", details.getAirlineName());
		assertEquals("SW", details.getAirlineCode());
		assertEquals("DEL", details.getDepartureAirportCode());
		assertEquals("Indira Gandhi International", details.getDepartureAirportName());
		assertEquals("BOM", details.getArrivalAirportCode());
		assertEquals("Scheduled", details.getFlightStatus());
		assertTrue(details.getDepartureDateTime().startsWith("2030-01-15 09:00"));
	}

	@Test
	void getFlightDetails_returnsNullForUnknownFlight() throws SQLException {
		assertNull(repository.getFlightDetails(999));
	}

	@Test
	void createBooking_insertsBookedRowAndReturnsGeneratedId() throws SQLException {
		int bookingId = repository.createBooking(1, 1, 3, 15000.0, 1, 2, 1);

		assertTrue(bookingId > 0);
		Map<String, Object> row = jdbcTemplate.queryForMap("SELECT * FROM booking WHERE Booking_ID = ?", bookingId);
		assertEquals("BOOKED", row.get("status"));
		assertEquals(3, row.get("Number_of_Seats"));
		assertEquals(15000.0, row.get("total_price"));
		assertEquals(1, row.get("User_ID"));
	}

	@Test
	void createPassenger_storesAdultAndChild() throws SQLException {
		int bookingId = repository.createBooking(1, 1, 2, 10000.0, 1, 1, 1);

		repository.createPassenger(bookingId, "Asha", "asha@example.com", null, true, 1);
		repository.createPassenger(bookingId, "Kiran", null, 8, false, 1);

		Map<String, Object> child = jdbcTemplate
				.queryForMap("SELECT email, age, Is_Registered FROM passenger WHERE name = 'Kiran'");
		assertNull(child.get("email"));
		assertEquals(8, child.get("age"));
		assertEquals(Boolean.FALSE, child.get("Is_Registered"));
		assertEquals(2, countRows("passenger WHERE Booking_ID = " + bookingId));
	}

	@Test
	void createPayment_acceptsEachPaymentModeAllowedBySchema() throws SQLException {
		int bookingId = repository.createBooking(1, 1, 1, 5000.0, 1, 1, 0);

		repository.createPayment(bookingId, "UPI", null, null, null, null, "TXN-1", "asha@upi", null, 5000.0);
		repository.createPayment(bookingId, "Credit Card", "4111111111111111", "Asha", "12/30", "Credit", "TXN-2",
				null, null, 5000.0);

		assertEquals(2, countRows("payment WHERE Booking_ID = " + bookingId));
	}

	@Test
	void updateFlightSeats_decrementsAvailableSeats() throws SQLException {
		repository.updateFlightSeats(1, 1, 3);

		assertEquals(7, availableSeats(1, 1));
	}

	@Test
	void updateFlightSeats_doesNotGuardAgainstOverselling() throws SQLException {
		// Characterization test: pins down CURRENT behaviour, which is a known bug. Business class on
		// flight 1 has 2 seats left, yet booking 5 succeeds and leaves -3. When the seat update is made
		// atomic (UPDATE ... WHERE Available_Seats >= ?), change this test to expect a rejection.
		repository.updateFlightSeats(1, 2, 5);

		assertEquals(-3, availableSeats(1, 2));
	}

	private int availableSeats(int flightId, int seatClassId) {
		return jdbcTemplate.queryForObject(
				"SELECT Available_Seats FROM flight_seats WHERE Flight_ID = ? AND Seat_Class_ID = ?", Integer.class,
				flightId, seatClassId);
	}

	private int countRows(String fromClause) {
		return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + fromClause, Integer.class);
	}
}
