package smart.fly.flight.seats.repository;

import java.util.List;

import smart.fly.flight.seats.model.FlightSeats;

public interface FlightSeatsRepository {
	FlightSeats findByFlightAndClass(int flightId, int seatClassId);

	List<FlightSeats> findByFlight(int flightId);

	void updateAvailableSeats(int flightSeatId, int newAvailableSeats);

	FlightSeats findByid(int id);

}
