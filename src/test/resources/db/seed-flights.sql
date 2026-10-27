-- Minimal data for the search and booking flows. IDs are fixed so tests can refer to them.
-- Only single quotes for strings: production runs with ANSI_QUOTES, where "x" means a column named x.

INSERT INTO `user` (User_ID, name, email, phone, Role, password_hash, password_salt) VALUES
  (1, 'Asha Passenger', 'asha@example.com', '9000000001', 'Passenger', 'hash', 'salt'),
  (2, 'Omar Owner',     'omar@example.com', '9000000002', 'Owner',     'hash', 'salt'),
  (3, 'Meera Manager',  'meera@example.com', '9000000003', 'Manager',  'hash', 'salt');

INSERT INTO business_owner (BO_ID, User_ID) VALUES (1, 2);
INSERT INTO flight_manager (FM_ID, BO_ID, User_ID) VALUES (1, 1, 3);

INSERT INTO airline (Airline_ID, name, code) VALUES (1, 'SkyWays', 'SW');

INSERT INTO airport (Airport_ID, name, code, Location) VALUES
  (1, 'Indira Gandhi International', 'DEL', 'Delhi'),
  (2, 'Chhatrapati Shivaji Maharaj International', 'BOM', 'Mumbai');

INSERT INTO seat_class (Seat_Class_ID, Class_Name, price) VALUES
  (1, 'Economy', 5000),
  (2, 'Business', 12000);

-- Flight 1 is bookable; flight 2 on the same route is cancelled and must never show up in search.
INSERT INTO flight (flight_Id, flight_number, Airline_ID, Departure_Airport_ID, Arrival_Airport_ID,
                    Departure_Date_Time, Arrival_Date_Time, Status, FM_ID) VALUES
  (1, 'SW-101', 1, 1, 2, '2030-01-15 09:00:00', '2030-01-15 11:10:00', 'Scheduled', 1),
  (2, 'SW-102', 1, 1, 2, '2030-01-15 18:00:00', '2030-01-15 20:10:00', 'Cancelled', 1);

INSERT INTO flight_seats (Flight_Seat_ID, Flight_ID, Seat_Class_ID, Available_Seats, Total_Seats, price) VALUES
  (1, 1, 1, 10, 100, 5000),
  (2, 1, 2,  2,  20, 12000),
  (3, 2, 1, 50, 100, 5000);
