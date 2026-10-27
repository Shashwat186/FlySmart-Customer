-- Empties every table so each test starts from a known state.
-- Rolling back a test transaction would not work here: the repositories under test open their own
-- connections from the DataSource, outside any Spring-managed transaction, so their writes commit.
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE feedback;
TRUNCATE TABLE payment;
TRUNCATE TABLE passenger;
TRUNCATE TABLE booking;
TRUNCATE TABLE flight_seats;
TRUNCATE TABLE flight;
TRUNCATE TABLE seat_class;
TRUNCATE TABLE airport;
TRUNCATE TABLE airline;
TRUNCATE TABLE flight_manager;
TRUNCATE TABLE business_owner;
TRUNCATE TABLE password_reset_tokens;
TRUNCATE TABLE action_log;
TRUNCATE TABLE `user`;
SET FOREIGN_KEY_CHECKS = 1;
