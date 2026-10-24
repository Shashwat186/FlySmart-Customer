package smart.fly.airport.repository;

import java.util.List;

import smart.fly.airport.model.Airport;

public interface AirportRepository {
	Airport findById(int airportId);

	Airport findByCode(String code);

	List<Airport> findAll();

	List<Airport> findByLocation(String location);

	List<Airport> findAllForSearch();
}
