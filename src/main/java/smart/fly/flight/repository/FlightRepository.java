package smart.fly.flight.repository;

import smart.fly.dto.FlightSearchDTO;
import smart.fly.flight.model.Flight;

import java.sql.Date;
import java.util.List;

public interface FlightRepository {
	Flight findById(int flightId);

	Flight findByNumber(String flightNumber);

	List<FlightSearchDTO> searchFlights(int departureAirportId, int arrivalAirportId, Date departureDate,
			Integer seatClassId);

	List<Flight> findAllForCustomers();

}
