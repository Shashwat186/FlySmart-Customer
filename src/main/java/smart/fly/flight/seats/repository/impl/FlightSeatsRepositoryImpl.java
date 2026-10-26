package smart.fly.flight.seats.repository.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import smart.fly.flight.model.Flight;
import smart.fly.flight.seats.model.FlightSeats;
import smart.fly.flight.seats.repository.FlightSeatsRepository;
import smart.fly.seatclass.model.SeatClass;

import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.List;

@Repository
public class FlightSeatsRepositoryImpl implements FlightSeatsRepository {

	private final JdbcTemplate jdbcTemplate;

	@Autowired
	public FlightSeatsRepositoryImpl(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public FlightSeats findByFlightAndClass(int flightId, int seatClassId) {
		String sql = "SELECT * FROM flight_seats WHERE flight_id = ? AND seat_class_id = ?";
		return jdbcTemplate.queryForObject(sql, new FlightSeatsRowMapper(), flightId, seatClassId);
	}

	@Override
	public List<FlightSeats> findByFlight(int flightId) {
		String sql = "SELECT * FROM flight_seats WHERE flight_id = ?";
		return jdbcTemplate.query(sql, new FlightSeatsRowMapper(), flightId);
	}

	@Override
	public void updateAvailableSeats(int flightSeatId, int newAvailableSeats) {
		String sql = "UPDATE flight_seats SET available_seats = ? WHERE flight_seat_id = ?";
		jdbcTemplate.update(sql, newAvailableSeats, flightSeatId);
	}

	private static class FlightSeatsRowMapper implements RowMapper<FlightSeats> {
		@Override
		public FlightSeats mapRow(ResultSet rs, int rowNum) throws SQLException {
			FlightSeats flightSeats = new FlightSeats();
			flightSeats.setFlightSeatId(rs.getInt("flight_seat_id"));

			Flight flight = new Flight();
			flight.setFlightId(rs.getInt("flight_id"));
			flightSeats.setFlight(flight);

			SeatClass seatClass = new SeatClass();
			seatClass.setSeatClassId(rs.getInt("seat_class_id"));
			flightSeats.setSeatClass(seatClass);

			flightSeats.setAvailableSeats(rs.getInt("available_seats"));
			flightSeats.setTotalSeats(rs.getInt("total_seats"));
			flightSeats.setPrice(rs.getDouble("price"));

			return flightSeats;
		}
	}

	@Override
	public FlightSeats findByid(int id) {
		String sql = "SELECT * FROM flight_seats WHERE flight_seat_id = ?";
		return jdbcTemplate.queryForObject(sql, new FlightSeatsRowMapper(), id);
	}

}
