package smart.fly.flight.search.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import smart.fly.dto.FlightSearchDTO;
import smart.fly.support.AbstractMySqlIntegrationTest;

@Sql({ "/db/clean.sql", "/db/seed-flights.sql" })
class FlightSearchRepositoryIntegrationTest extends AbstractMySqlIntegrationTest {

	private static final Date SEARCH_DATE = Date.valueOf("2030-01-01");

	@Autowired
	private FlightSearchRepository repository;

	@Test
	void searchFlights_returnsOneRowPerSeatClassAndSkipsCancelledFlights() {
		List<FlightSearchDTO> results = repository.searchFlights(1, 2, SEARCH_DATE, null, 1);

		// Flight 1 has Economy and Business rows; cancelled flight 2 must not appear.
		assertEquals(2, results.size());
		assertTrue(results.stream().allMatch(f -> "SW-101".equals(f.getFlightNumber())));
	}

	@Test
	void searchFlights_mapsJoinedColumnsForChosenSeatClass() {
		List<FlightSearchDTO> results = repository.searchFlights(1, 2, SEARCH_DATE, 1, 3);

		assertEquals(1, results.size());
		FlightSearchDTO flight = results.get(0);
		assertEquals(5000.0, flight.getPrice());
		assertEquals(130, flight.getDuration());
		assertEquals("SW", flight.getAirline().getCode());
		assertEquals("DEL", flight.getDepartureAirport().getCode());
		assertEquals("BOM", flight.getArrivalAirport().getCode());
	}

	@Test
	void searchFlights_excludesSeatClassWithTooFewSeats() {
		// Business on flight 1 has only 2 seats left.
		assertTrue(repository.searchFlights(1, 2, SEARCH_DATE, 2, 3).isEmpty());
	}

	@Test
	void searchFlights_excludesFlightsBeforeSearchDate() {
		assertTrue(repository.searchFlights(1, 2, Date.valueOf("2030-02-01"), null, 1).isEmpty());
	}
}
