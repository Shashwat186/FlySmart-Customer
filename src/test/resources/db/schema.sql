-- Production schema, captured from Aiven (MySQL 8.4.8) on 2026-10-28 with SHOW CREATE TABLE.
-- Structure only, no data. flyway_schema_history is left out: it belongs to Flyway, not the app.
-- Tables are ordered so that every foreign key points at a table created above it.
-- Re-capture this file whenever the production schema changes, until migrations live in this repo.

CREATE TABLE `user` (
  `User_ID` int NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `email` varchar(255) NOT NULL,
  `phone` varchar(255) NOT NULL,
  `Role` enum('Passenger','Manager','Owner') NOT NULL,
  `Gender` enum('Male','Female','Other') DEFAULT NULL,
  `dob` date DEFAULT NULL,
  `password_hash` varchar(64) NOT NULL,
  `password_salt` varchar(40) NOT NULL,
  `profile_photo` longblob,
  PRIMARY KEY (`User_ID`),
  UNIQUE KEY `Email` (`email`),
  UNIQUE KEY `Phone` (`phone`)
);

CREATE TABLE `business_owner` (
  `BO_ID` int NOT NULL AUTO_INCREMENT,
  `User_ID` int NOT NULL,
  PRIMARY KEY (`BO_ID`),
  UNIQUE KEY `User_ID` (`User_ID`),
  CONSTRAINT `business_owner_ibfk_1` FOREIGN KEY (`User_ID`) REFERENCES `user` (`User_ID`) ON DELETE CASCADE
);

CREATE TABLE `flight_manager` (
  `FM_ID` int NOT NULL AUTO_INCREMENT,
  `BO_ID` int NOT NULL,
  `User_ID` int NOT NULL,
  PRIMARY KEY (`FM_ID`),
  UNIQUE KEY `User_ID` (`User_ID`),
  KEY `BO_ID` (`BO_ID`),
  CONSTRAINT `flight_manager_ibfk_1` FOREIGN KEY (`BO_ID`) REFERENCES `business_owner` (`BO_ID`) ON DELETE CASCADE,
  CONSTRAINT `flight_manager_ibfk_2` FOREIGN KEY (`User_ID`) REFERENCES `user` (`User_ID`) ON DELETE CASCADE
);

CREATE TABLE `airline` (
  `Airline_ID` int NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `code` varchar(255) NOT NULL,
  `contact_info` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`Airline_ID`),
  UNIQUE KEY `Code` (`code`)
);

CREATE TABLE `airport` (
  `Airport_ID` int NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `code` varchar(255) NOT NULL,
  `contact_info` varchar(255) DEFAULT NULL,
  `Location` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`Airport_ID`),
  UNIQUE KEY `Code` (`code`),
  UNIQUE KEY `name_UNIQUE` (`name`)
);

CREATE TABLE `seat_class` (
  `Seat_Class_ID` int NOT NULL AUTO_INCREMENT,
  `Class_Name` enum('Economy','Business','First Class') NOT NULL,
  `price` double NOT NULL,
  PRIMARY KEY (`Seat_Class_ID`)
);

CREATE TABLE `flight` (
  `flight_Id` int NOT NULL AUTO_INCREMENT,
  `flight_number` varchar(255) NOT NULL,
  `Airline_ID` int NOT NULL,
  `Departure_Airport_ID` int NOT NULL,
  `Arrival_Airport_ID` int NOT NULL,
  `Departure_Date_Time` datetime NOT NULL,
  `Arrival_Date_Time` datetime NOT NULL,
  `Status` enum('Scheduled','Delayed','Cancelled','Completed') NOT NULL,
  `FM_ID` int NOT NULL,
  PRIMARY KEY (`flight_Id`),
  UNIQUE KEY `Flight_Number` (`flight_number`),
  KEY `Airline_ID` (`Airline_ID`),
  KEY `Departure_Airport_ID` (`Departure_Airport_ID`),
  KEY `Arrival_Airport_ID` (`Arrival_Airport_ID`),
  KEY `FM_ID` (`FM_ID`),
  CONSTRAINT `flight_ibfk_1` FOREIGN KEY (`Airline_ID`) REFERENCES `airline` (`Airline_ID`) ON DELETE CASCADE,
  CONSTRAINT `flight_ibfk_2` FOREIGN KEY (`Departure_Airport_ID`) REFERENCES `airport` (`Airport_ID`) ON DELETE CASCADE,
  CONSTRAINT `flight_ibfk_3` FOREIGN KEY (`Arrival_Airport_ID`) REFERENCES `airport` (`Airport_ID`) ON DELETE CASCADE,
  CONSTRAINT `flight_ibfk_4` FOREIGN KEY (`FM_ID`) REFERENCES `flight_manager` (`FM_ID`) ON DELETE CASCADE
);

CREATE TABLE `flight_seats` (
  `Flight_Seat_ID` int NOT NULL AUTO_INCREMENT,
  `Flight_ID` int NOT NULL,
  `Seat_Class_ID` int NOT NULL,
  `Available_Seats` int NOT NULL,
  `Total_Seats` int NOT NULL,
  `price` double NOT NULL,
  PRIMARY KEY (`Flight_Seat_ID`),
  KEY `Flight_ID` (`Flight_ID`),
  KEY `Seat_Class_ID` (`Seat_Class_ID`),
  CONSTRAINT `flight_seats_ibfk_1` FOREIGN KEY (`Flight_ID`) REFERENCES `flight` (`flight_Id`) ON DELETE CASCADE,
  CONSTRAINT `flight_seats_ibfk_2` FOREIGN KEY (`Seat_Class_ID`) REFERENCES `seat_class` (`Seat_Class_ID`) ON DELETE CASCADE
);

CREATE TABLE `booking` (
  `Booking_ID` int NOT NULL AUTO_INCREMENT,
  `Flight_ID` int NOT NULL,
  `Seat_Class_ID` int NOT NULL,
  `Booking_Date_Time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `Number_of_Seats` int NOT NULL,
  `total_price` double NOT NULL,
  `number_of_adults` int DEFAULT NULL,
  `number_of_children` int DEFAULT NULL,
  `User_ID` int DEFAULT NULL,
  `status` enum('BOOKED','CANCELLED') DEFAULT 'BOOKED',
  PRIMARY KEY (`Booking_ID`),
  KEY `Flight_ID` (`Flight_ID`),
  KEY `Seat_Class_ID` (`Seat_Class_ID`),
  CONSTRAINT `booking_ibfk_2` FOREIGN KEY (`Flight_ID`) REFERENCES `flight` (`flight_Id`) ON DELETE CASCADE,
  CONSTRAINT `booking_ibfk_3` FOREIGN KEY (`Seat_Class_ID`) REFERENCES `seat_class` (`Seat_Class_ID`) ON DELETE CASCADE
);

CREATE TABLE `passenger` (
  `Passenger_ID` int NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `email` varchar(255) DEFAULT NULL,
  `phone` varchar(255) DEFAULT NULL,
  `Loyalty_Points` int DEFAULT '0',
  `Is_Registered` tinyint(1) DEFAULT '0',
  `Link_Reg_ID` int DEFAULT NULL,
  `Booking_ID` int DEFAULT NULL,
  `age` int DEFAULT NULL,
  PRIMARY KEY (`Passenger_ID`),
  KEY `FK_passengers_bookings` (`Booking_ID`),
  CONSTRAINT `FK_passengers_bookings` FOREIGN KEY (`Booking_ID`) REFERENCES `booking` (`Booking_ID`) ON DELETE CASCADE
);

CREATE TABLE `payment` (
  `Payment_ID` int NOT NULL AUTO_INCREMENT,
  `Booking_ID` int NOT NULL,
  `card_number` varchar(255) DEFAULT NULL,
  `card_holder_name` varchar(255) DEFAULT NULL,
  `expiry_date` varchar(10) DEFAULT NULL,
  `Payment_Date_Time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `Payment_Mode` enum('Credit Card','Debit Card','UPI') NOT NULL,
  `Transaction_ID` varchar(255) DEFAULT NULL,
  `upi_id` varchar(255) DEFAULT NULL,
  `Wallet_Name` varchar(100) DEFAULT NULL,
  `Card_Type` enum('Credit','Debit') DEFAULT NULL,
  `Amount` double NOT NULL,
  PRIMARY KEY (`Payment_ID`),
  KEY `Booking_ID` (`Booking_ID`),
  CONSTRAINT `payment_ibfk_1` FOREIGN KEY (`Booking_ID`) REFERENCES `booking` (`Booking_ID`) ON DELETE CASCADE
);

CREATE TABLE `feedback` (
  `Feedback_ID` int NOT NULL AUTO_INCREMENT,
  `User_ID` int NOT NULL,
  `Feedback_Date_Time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `Rating` int DEFAULT NULL,
  `Feedback_Content` text NOT NULL,
  `status` enum('unchecked','checked') DEFAULT 'unchecked',
  `Booking_ID` int NOT NULL,
  PRIMARY KEY (`Feedback_ID`),
  KEY `User_ID` (`User_ID`),
  KEY `FK_BookingFeedback` (`Booking_ID`),
  CONSTRAINT `feedback_ibfk_1` FOREIGN KEY (`User_ID`) REFERENCES `user` (`User_ID`) ON DELETE CASCADE,
  CONSTRAINT `FK_BookingFeedback` FOREIGN KEY (`Booking_ID`) REFERENCES `booking` (`Booking_ID`) ON DELETE CASCADE,
  CONSTRAINT `feedback_chk_1` CHECK ((`Rating` between 1 and 5))
);

CREATE TABLE `password_reset_tokens` (
  `id` int NOT NULL AUTO_INCREMENT,
  `email` varchar(255) NOT NULL,
  `token` varchar(255) NOT NULL,
  `expiry_date` datetime NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
);

CREATE TABLE `action_log` (
  `log_id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `action_performed` varchar(255) NOT NULL,
  `controller_name` varchar(100) NOT NULL,
  `timestamp` datetime NOT NULL,
  PRIMARY KEY (`log_id`)
);
