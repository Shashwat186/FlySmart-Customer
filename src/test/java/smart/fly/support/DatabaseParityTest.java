package smart.fly.support;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Guards the test setup itself: if any of these fail, the other integration tests are no longer
 * proving anything about production.
 */
class DatabaseParityTest extends AbstractMySqlIntegrationTest {

	@Autowired
	private DataSource dataSource;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void testsNeverConnectToAiven() throws SQLException {
		try (Connection connection = dataSource.getConnection()) {
			String url = connection.getMetaData().getURL();
			assertFalse(url.contains("aivencloud"), "Tests must use the Docker MySQL, but connected to " + url);
		}
	}

	@Test
	void runsSameMySqlVersionAsProduction() {
		String version = jdbcTemplate.queryForObject("SELECT VERSION()", String.class);
		assertTrue(version.startsWith("8.4."), "Expected MySQL 8.4.x like production, got " + version);
	}

	@Test
	void tableNamesAreCaseSensitiveLikeProduction() {
		assertEquals(0, jdbcTemplate.queryForObject("SELECT @@lower_case_table_names", Integer.class));

		// The exact mistake that broke payments in production.
		assertThrows(BadSqlGrammarException.class, () -> jdbcTemplate.queryForList("SELECT * FROM Airline"));
		assertDoesNotThrow(() -> jdbcTemplate.queryForList("SELECT * FROM airline"));
	}

	@Test
	void usesProductionSqlMode() {
		String sqlMode = jdbcTemplate.queryForObject("SELECT @@GLOBAL.sql_mode", String.class);
		for (String mode : MySqlTestcontainersConfiguration.PRODUCTION_SQL_MODE.split(",")) {
			assertTrue(sqlMode.contains(mode), "sql_mode is missing " + mode + ": " + sqlMode);
		}

		// With ANSI_QUOTES, "x" is an identifier, not a string. This query would pass on default MySQL.
		assertThrows(BadSqlGrammarException.class,
				() -> jdbcTemplate.queryForList("SELECT * FROM booking WHERE status = \"BOOKED\""));
	}
}
