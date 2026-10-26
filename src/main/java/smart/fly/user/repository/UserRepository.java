package smart.fly.user.repository;



import smart.fly.user.model.User;

public interface UserRepository {
	User findByEmail(String email);

	User findById(int userId);

	User save(User user);

	void update(User user);

	void changePassword(int userId, String newPasswordHash, String newSalt);

	boolean emailExists(String email);

	void updateProfilePhoto(int userId, byte[] profilePhoto);

}
