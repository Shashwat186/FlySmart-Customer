package smart.fly.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import jakarta.servlet.http.HttpSession;
import smart.fly.service.UserService;
import smart.fly.user.model.User;

import java.util.Base64;

@Controller
@RequestMapping("/passenger")
public class UserController {

	private final UserService userService;

	@Autowired
	public UserController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping("/profile")
	public String showProfile(HttpSession session, Model model) {
		User sessionUser = (User) session.getAttribute("user");
		if (sessionUser == null) {
			return "redirect:/auth/login";
		}

		User user = userService.getUserById(sessionUser.getUserId());
		if (user == null) {
			return "redirect:/auth/login";
		}

		model.addAttribute("user", user);

		
		if (user.getProfilePhoto() != null && user.getProfilePhoto().length > 0) {
			String profilePhotoBase64 = Base64.getEncoder().encodeToString(user.getProfilePhoto());
			model.addAttribute("profilePhotoBase64", profilePhotoBase64);
		}

		return "passenger/profile"; // Maps to profile.jsp
	}

	// Old URL kept for existing links; only the logged-in user's own profile can be viewed.
	@GetMapping("/profile/{userId}")
	public String showProfileById(@PathVariable int userId, HttpSession session, Model model) {
		User sessionUser = (User) session.getAttribute("user");
		if (sessionUser == null) {
			return "redirect:/auth/login";
		}
		if (sessionUser.getUserId() != userId) {
			return "redirect:/passenger/profile";
		}

		return showProfile(session, model);
	}
}