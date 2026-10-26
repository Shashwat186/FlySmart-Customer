package smart.fly.passenger.repository;



import smart.fly.passenger.model.Passenger;

public interface PassengerRepository {
	Passenger findById(int passengerId);

	Passenger findByEmail(String email);

	Passenger findByPhone(String phone);

	Passenger findByUserId(int userId);

	void save(Passenger passenger);

	void update(Passenger passenger);

	void linkToUser(int passengerId, int userId);

}
