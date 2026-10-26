package smart.fly.feedback.repository.impl;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import smart.fly.feedback.model.Feedback;
import smart.fly.feedback.repository.FeedbackRepository;


import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.List;

import javax.sql.DataSource;

@Repository
public class FeedbackRepositoryImpl implements FeedbackRepository {

	private final JdbcTemplate jdbcTemplate;
	private final DataSource dataSource;

	public FeedbackRepositoryImpl(JdbcTemplate jdbcTemplate, DataSource dataSource) {
		super();
		this.jdbcTemplate = jdbcTemplate;
		this.dataSource = dataSource;
	}

	@Override
	public boolean addFeedback(Feedback feedback) {
		String sql = "INSERT INTO feedback (User_ID, Booking_ID, Feedback_Date_Time, Rating, Feedback_Content, status) "
				+ "VALUES (?, ?, ?, ?, ?, ?)";
		int rowsAffected = jdbcTemplate.update(sql, feedback.getUserId(), feedback.getBookingId(),
				feedback.getFeedbackDateTime(), feedback.getRating(), feedback.getFeedbackContent(),
				feedback.getStatus());
		return rowsAffected > 0;
	}

	@Override
	public boolean checkFeedbackExists(int userId, int bookingId) {
		String sql = "SELECT COUNT(*) FROM feedback WHERE User_ID = ? AND Booking_ID = ?";
		Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId, bookingId);
		return count != null && count > 0;
	}

//    @Override
//    public List<Feedback> getFeedbacksByUser(int userId) {
//        String sql = "SELECT * FROM Feedback WHERE User_ID = ?";
//        return jdbcTemplate.query(sql, new FeedbackRowMapper(), userId);
//    }

	private static class FeedbackRowMapper implements RowMapper<Feedback> {
		@Override
		public Feedback mapRow(ResultSet rs, int rowNum) throws SQLException {
			Feedback feedback = new Feedback();
			feedback.setFeedbackId(rs.getInt("Feedback_ID"));
			feedback.setUserId(rs.getInt("User_ID"));
			feedback.setBookingId(rs.getInt("Booking_ID"));
			feedback.setFeedbackDateTime(rs.getTimestamp("Feedback_Date_Time").toLocalDateTime());
			feedback.setRating(rs.getInt("Rating"));
			feedback.setFeedbackContent(rs.getString("Feedback_Content"));
			feedback.setStatus(rs.getString("status"));
			return feedback;
		}
	}

	@Override
	public List<Integer> getBookingsWithFeedback(int userId) {
		String sql = "SELECT Booking_ID FROM feedback WHERE User_ID = ?";
		return jdbcTemplate.queryForList(sql, Integer.class, userId);
	}

	@Override
	public List<Feedback> getFeedbacksByUser(int userId) {
		String sql = "SELECT * FROM feedback WHERE User_ID = ?";
		return jdbcTemplate.query(sql, new FeedbackRowMapper(), userId);
	}
}
