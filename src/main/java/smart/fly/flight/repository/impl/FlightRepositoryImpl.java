package smart.fly.flight.repository.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import smart.fly.airline.model.Airline;
import smart.fly.airport.model.Airport;
import smart.fly.dto.FlightSearchDTO;
import smart.fly.exception.FlightManagementException;
import smart.fly.flight.model.Flight;
import smart.fly.flight.repository.FlightRepository;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Repository
public class FlightRepositoryImpl implements FlightRepository {

	private final JdbcTemplate jdbcTemplate;

	@Autowired
	public FlightRepositoryImpl(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public Flight findById(int flightId) {
		try {
			String sql = "SELECT " + "f.flight_Id, f.flight_number, "
					+ "f.Departure_Date_Time, f.Arrival_Date_Time, f.Status, "
					+ "a.Airline_ID, a.name AS airline_name, a.code AS airline_code, "
					+ "dep.Airport_ID AS dep_id, dep.name AS departure_airport_name, dep.code AS departure_code, "
					+ "arr.Airport_ID AS arr_id, arr.name AS arrival_airport_name, arr.code AS arrival_code "
					+ "FROM flight f " + "JOIN airline a ON f.Airline_ID = a.Airline_ID "
					+ "JOIN airport dep ON f.Departure_Airport_ID = dep.Airport_ID "
					+ "JOIN airport arr ON f.Arrival_Airport_ID = arr.Airport_ID " + "WHERE f.flight_Id = ?";
			return jdbcTemplate.queryForObject(sql, new FlightRowMapper(), flightId);

		} catch (EmptyResultDataAccessException e) {
			throw new FlightManagementException("Flight not found with ID: " + flightId);
		}

	}

	@Override
	public Flight findByNumber(String flightNumber) {
		try {
			String sql = "SELECT " + "f.flight_Id, f.flight_number, "
					+ "f.Departure_Date_Time, f.Arrival_Date_Time, f.Status, "
					+ "a.Airline_ID, a.name AS airline_name, a.code AS airline_code, "
					+ "dep.Airport_ID AS dep_id, dep.name AS departure_airport_name, dep.code AS departure_code, "
					+ "arr.Airport_ID AS arr_id, arr.name AS arrival_airport_name, arr.code AS arrival_code "
					+ "FROM flight f " + "JOIN airline a ON f.Airline_ID = a.Airline_ID "
					+ "JOIN airport dep ON f.Departure_Airport_ID = dep.Airport_ID "
					+ "JOIN airport arr ON f.Arrival_Airport_ID = arr.Airport_ID " + "WHERE f.flight_number = ?";
			return jdbcTemplate.queryForObject(sql, new FlightRowMapper(), flightNumber);
		} catch (EmptyResultDataAccessException e) {
			throw new FlightManagementException("Flight not found with number: " + flightNumber);
		}
	}

	private static class FlightRowMapper implements RowMapper<Flight> {
		@Override
		public Flight mapRow(ResultSet rs, int rowNum) throws SQLException {
			Flight flight = new Flight();
			flight.setFlightId(rs.getInt("flight_Id"));
			flight.setFlightNumber(rs.getString("flight_number"));
			// Set Airline
			Airline airline = new Airline();
			airline.setAirlineId(rs.getInt("Airline_ID"));
			airline.setName(rs.getString("airline_name"));
			airline.setCode(rs.getString("airline_code"));
			flight.setAirline(airline);

			// Set Departure Airport
			Airport departure = new Airport();
			departure.setAirportId(rs.getInt("dep_id"));
			departure.setName(rs.getString("departure_airport_name"));
			departure.setCode(rs.getString("departure_code"));
			flight.setDepartureAirport(departure);

			// Set Arrival Airport
			Airport arrival = new Airport();
			arrival.setAirportId(rs.getInt("arr_id"));
			arrival.setName(rs.getString("arrival_airport_name"));
			arrival.setCode(rs.getString("arrival_code"));
			flight.setArrivalAirport(arrival);

			flight.setDepartureDateTime(rs.getTimestamp("Departure_Date_Time"));
			flight.setArrivalDateTime(rs.getTimestamp("Arrival_Date_Time"));
			flight.setStatus(rs.getString("Status"));

			return flight;
		}
	}

	@Override
	public List<Flight> findAllForCustomers() {
		String sql = "SELECT " + "f.flight_Id, f.flight_number,"
				+ "f.Departure_Date_Time, f.Arrival_Date_Time, f.Status, "
				+ "a.Airline_ID, a.name AS airline_name, a.code AS airline_code, "
				+ "dep.Airport_ID AS dep_id, dep.name AS departure_airport_name, dep.code AS departure_code, "
				+ "arr.Airport_ID AS arr_id, arr.name AS arrival_airport_name, arr.code AS arrival_code "
				+ "FROM flight f " + "JOIN airline a ON f.Airline_ID = a.Airline_ID "
				+ "JOIN airport dep ON f.Departure_Airport_ID = dep.Airport_ID "
				+ "JOIN airport arr ON f.Arrival_Airport_ID = arr.Airport_ID " + "ORDER BY f.Departure_Date_Time";
		return jdbcTemplate.query(sql, new FlightRowMapper());
	}

	@Override
	public List<FlightSearchDTO> searchFlights(int departureAirportId, int arrivalAirportId, Date departureDate,
			Integer seatClassId) {

		String sql = "SELECT " + "f.flight_Id, f.flight_number, "
				+ "f.Departure_Date_Time, f.Arrival_Date_Time, f.Status, "
				+ "a.Airline_ID, a.name AS airline_name, a.code AS airline_code, "
				+ "dep.Airport_ID AS dep_id, dep.name AS departure_airport_name, dep.code AS departure_code, "
				+ "arr.Airport_ID AS arr_id, arr.name AS arrival_airport_name, arr.code AS arrival_code, " + "fs.price "
				+ "FROM flight f " + "JOIN airline a ON f.Airline_ID = a.Airline_ID "
				+ "JOIN airport dep ON f.Departure_Airport_ID = dep.Airport_ID "
				+ "JOIN airport arr ON f.Arrival_Airport_ID = arr.Airport_ID "
				+ "JOIN flight_seats fs ON f.flight_Id = fs.Flight_ID " + "WHERE f.Departure_Airport_ID = ? "
				+ "AND f.Arrival_Airport_ID = ? " + "AND f.Status IN ('Scheduled', 'Delayed', 'Completed')"
				+ (departureDate != null ? "AND f.Departure_Date_Time >= ? " : "")
				+ (seatClassId != null ? "AND fs.Seat_Class_ID = ? " : "") + "AND fs.Available_Seats > 0 "
				+ "ORDER BY f.Departure_Date_Time";

		try {
			return jdbcTemplate.query(sql, new FlightSearchDTORowMapper(), departureAirportId, arrivalAirportId,
					new java.sql.Date(departureDate.getTime()), seatClassId != null ? seatClassId : null);
		} catch (DataAccessException e) {
			throw new RuntimeException("Error searching flights", e);
		}
	}

	private static class FlightSearchDTORowMapper implements RowMapper<FlightSearchDTO> {
		@Override
		public FlightSearchDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
			FlightSearchDTO dto = new FlightSearchDTO();
			dto.setFlightId(rs.getInt("flight_Id"));
			dto.setFlightNumber(rs.getString("flight_number"));

			// Set Airline
			Airline airline = new Airline();
			airline.setAirlineId(rs.getInt("Airline_ID"));
			airline.setName(rs.getString("airline_name"));
			airline.setCode(rs.getString("airline_code"));
			dto.setAirline(airline);

			// Set Airports
			dto.setDepartureAirport(mapAirport(rs, "dep"));
			dto.setArrivalAirport(mapAirport(rs, "arr"));

			LocalDateTime departureDateTime = rs.getObject("Departure_Date_Time", LocalDateTime.class);
			LocalDateTime arrivalDateTime = rs.getObject("Arrival_Date_Time", LocalDateTime.class);

			dto.setDepartureDateTime(departureDateTime); // Setting LocalDateTime
			dto.setArrivalDateTime(arrivalDateTime); // Setting LocalDateTime

			// Explicitly convert LocalDateTime to Date and set
			if (departureDateTime != null) {
				dto.setDepartureDate(Date.from(departureDateTime.atZone(ZoneId.systemDefault()).toInstant()));
			}
			if (arrivalDateTime != null) {
				dto.setArrivalDate(Date.from(arrivalDateTime.atZone(ZoneId.systemDefault()).toInstant()));
			}

			dto.setDuration(ChronoUnit.MINUTES.between(departureDateTime, arrivalDateTime));

			dto.setStatus(rs.getString("Status"));
			dto.setPrice(rs.getDouble("price"));

			return dto;
		}

		private Airport mapAirport(ResultSet rs, String prefix) throws SQLException {
			Airport airport = new Airport();
			airport.setAirportId(rs.getInt(prefix + "_id"));
			if (prefix.equals("dep")) {
				airport.setName(rs.getString("departure_airport_name"));
				airport.setCode(rs.getString("departure_code"));
			} else if (prefix.equals("arr")) {
				airport.setName(rs.getString("arrival_airport_name"));
				airport.setCode(rs.getString("arrival_code"));
			}
			return airport;
		}
	}

	private Airport mapAirport(ResultSet rs, String prefix) throws SQLException {
		Airport airport = new Airport();
		airport.setAirportId(rs.getInt(prefix + "_id"));
		if (prefix.equals("dep")) {
			airport.setName(rs.getString("departure_airport_name"));
			airport.setCode(rs.getString("departure_code"));
		} else if (prefix.equals("arr")) {
			airport.setName(rs.getString("arrival_airport_name"));
			airport.setCode(rs.getString("arrival_code"));
		}
		return airport;
	}
}
