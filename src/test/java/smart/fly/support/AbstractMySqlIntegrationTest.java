package smart.fly.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base class for tests that need the full Spring context and a real database.
 *
 * <p>Every subclass shares one identical context configuration, so Spring's test context cache
 * reuses a single application context, and therefore a single MySQL container, for the whole run.
 * Adding @MockBean or different properties to a subclass creates a new context and a new container.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(MySqlTestcontainersConfiguration.class)
public abstract class AbstractMySqlIntegrationTest {
}
