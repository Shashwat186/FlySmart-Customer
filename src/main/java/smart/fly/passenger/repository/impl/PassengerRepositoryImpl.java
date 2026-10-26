package smart.fly.passenger.repository.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import smart.fly.passenger.model.Passenger;
import smart.fly.passenger.repository.PassengerRepository;


import java.sql.ResultSet;
import java.sql.SQLException;


@Repository
public class PassengerRepositoryImpl implements PassengerRepository {

	private final JdbcTemplate jdbcTemplate;

	@Autowired
	public PassengerRepositoryImpl(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public Passenger findById(int passengerId) {
		String sql = "SELECT * FROM passenger WHERE passenger_id = ?";
		return jdbcTemplate.queryForObject(sql, new PassengerRowMapper(), passengerId);
	}

	@Override
	public Passenger findByEmail(String email) {
		String sql = "SELECT * FROM passenger WHERE email = ?";
		return jdbcTemplate.queryForObject(sql, new PassengerRowMapper(), email);
	}

	@Override
	public Passenger findByPhone(String phone) {
		String sql = "SELECT * FROM passenger WHERE phone = ?";
		return jdbcTemplate.queryForObject(sql, new PassengerRowMapper(), phone);
	}

	@Override
	public Passenger findByUserId(int userId) {
		String sql = "SELECT * FROM user WHERE User_ID = ?";
		try {
			return jdbcTemplate.queryForObject(sql, new PassengerRowMapper());
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	@Override
	public void save(Passenger passenger) {
		String sql = "INSERT INTO passenger (name, email, phone, loyalty_points, is_registered, user_id) "
				+ "VALUES (?, ?, ?, ?, ?, ?)";
		jdbcTemplate.update(sql, passenger.getName(), passenger.getEmail(), passenger.getPhone(),
				passenger.getLoyaltyPoints(), passenger.isRegistered(), 
				passenger.getUser() != null ? passenger.getUser().getUserId() : null);
	}

	@Override
	public void update(Passenger passenger) {
		String sql = "UPDATE passenger SET name = ?, email = ?, phone = ?, loyalty_points = ?, "
				+ "is_registered = ?, user_id = ? WHERE passenger_id = ?";
		jdbcTemplate.update(sql, passenger.getName(), passenger.getEmail(), passenger.getPhone(),
				passenger.getLoyaltyPoints(), passenger.isRegistered(), 
				passenger.getUser() != null ? passenger.getUser().getUserId() : null, passenger.getPassengerId());
	}

	@Override
	public void linkToUser(int passengerId, int userId) {
		String sql = "UPDATE passenger SET user_id = ?, is_registered = TRUE WHERE passenger_id = ?";
		jdbcTemplate.update(sql, userId, passengerId);
	}

	// In PassengerRepositoryImpl.java

	private static class PassengerRowMapper implements RowMapper<Passenger> {
		@Override
		public Passenger mapRow(ResultSet rs, int rowNum) throws SQLException {
			Passenger passenger = new Passenger();
			passenger.setPassengerId(rs.getInt("passenger_id"));
			passenger.setName(rs.getString("name"));
			passenger.setEmail(rs.getString("email"));
			passenger.setPhone(rs.getString("phone"));
			passenger.setLoyaltyPoints(rs.getInt("loyalty_points"));
			passenger.setRegistered(rs.getBoolean("is_registered"));
			

			return passenger;
		}

	}
}
