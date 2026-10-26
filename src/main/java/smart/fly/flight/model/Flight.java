package smart.fly.flight.model;

import java.sql.Timestamp;


import smart.fly.airline.model.Airline;
import smart.fly.airport.model.Airport;

public class Flight {
	private int flightId;
	private String flightNumber;
	private Timestamp departureDateTime;
	private Timestamp arrivalDateTime;
	private String status;

	// Object references (will be populated manually in RowMapper)
	private Airline airline;
	private Airport departureAirport;
	private Airport arrivalAirport;

	public int getFlightId() {
		return flightId;
	}

	public void setFlightId(int flightId) {
		this.flightId = flightId;
	}

	public String getFlightNumber() {
		return flightNumber;
	}

	public void setFlightNumber(String flightNumber) {
		this.flightNumber = flightNumber;
	}

	public Timestamp getDepartureDateTime() {
		return departureDateTime;
	}

	public void setDepartureDateTime(Timestamp departureDateTime) {
		this.departureDateTime = departureDateTime;
	}

	public Timestamp getArrivalDateTime() {
		return arrivalDateTime;
	}

	public void setArrivalDateTime(Timestamp arrivalDateTime) {
		this.arrivalDateTime = arrivalDateTime;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Airline getAirline() {
		return airline;
	}

	public void setAirline(Airline airline) {
		this.airline = airline;
	}

	public Airport getDepartureAirport() {
		return departureAirport;
	}

	public void setDepartureAirport(Airport departureAirport) {
		this.departureAirport = departureAirport;
	}

	public Airport getArrivalAirport() {
		return arrivalAirport;
	}

	public void setArrivalAirport(Airport arrivalAirport) {
		this.arrivalAirport = arrivalAirport;
	}

	public Flight() {
		super();
	}

}