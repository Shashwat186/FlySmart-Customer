package smart.fly.service;

import org.springframework.stereotype.Service;

import smart.fly.user.model.User;
import smart.fly.user.repository.UserRepository;


@Service
public class UserService {
	private final UserRepository userRepository;

	public UserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public User getUserById(int userId) {
		return userRepository.findById(userId);
	}

}
