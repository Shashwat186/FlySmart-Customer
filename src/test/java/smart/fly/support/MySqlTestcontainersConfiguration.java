package smart.fly.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Starts a throwaway MySQL in Docker that behaves like the Aiven production database.
 *
 * <p>{@link ServiceConnection} tells Spring Boot to build the DataSource from this container's
 * host, port and credentials, overriding spring.datasource.* (so the DB_URL in .env is ignored).
 */
@TestConfiguration(proxyBeanMethods = false)
public class MySqlTestcontainersConfiguration {

	// Same major/minor version as production (8.4.8).
	static final DockerImageName MYSQL_IMAGE = DockerImageName.parse("mysql:8.4");

	// Copied from Aiven's @@GLOBAL.sql_mode. ANSI_QUOTES, PIPES_AS_CONCAT and ONLY_FULL_GROUP_BY
	// change how SQL is parsed, so queries that pass on a default MySQL can fail in production.
	public static final String PRODUCTION_SQL_MODE = "REAL_AS_FLOAT,PIPES_AS_CONCAT,ANSI_QUOTES,IGNORE_SPACE,"
			+ "ONLY_FULL_GROUP_BY,ANSI,STRICT_ALL_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,"
			+ "ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION";

	@Bean
	@ServiceConnection
	@SuppressWarnings("resource") // Spring calls close() on the bean when the test context shuts down
	MySQLContainer<?> mysqlContainer() {
		// The official image runs on Linux, where lower_case_table_names=0 (case-sensitive table
		// names), exactly like Aiven. H2 and MySQL on Windows both hide case mistakes.
		return new MySQLContainer<>(MYSQL_IMAGE)
				.withCommand("--sql-mode=" + PRODUCTION_SQL_MODE)
				.withInitScript("db/schema.sql");
	}
}
