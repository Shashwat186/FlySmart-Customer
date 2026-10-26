package smart.fly.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import smart.fly.service.UserService;
import smart.fly.user.model.User;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    private MockHttpSession session;
    private User user;

    @BeforeEach
    void setUp() {
        User sessionUser = new User();
        sessionUser.setUserId(1);
        session = new MockHttpSession();
        session.setAttribute("user", sessionUser);

        user = new User();
        user.setUserId(1);
    }

    @Test
    void showProfile_shouldRedirectToLogin_whenNotLoggedIn() throws Exception {
        mockMvc.perform(get("/passenger/profile")).andExpect(redirectedUrl("/auth/login"));

        verifyNoInteractions(userService);
    }

    @Test
    void showProfile_shouldLoadSessionUserWithEncodedPhoto_whenUserHasPhoto() throws Exception {
        // Arrange
        byte[] photo = { 1, 2, 3, 4 };
        user.setProfilePhoto(photo);
        when(userService.getUserById(1)).thenReturn(user);

        // Act & Assert
        mockMvc.perform(get("/passenger/profile").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("passenger/profile"))
                .andExpect(model().attribute("user", user))
                .andExpect(model().attribute("profilePhotoBase64", Base64.getEncoder().encodeToString(photo)));
    }

    @Test
    void showProfile_shouldNotExposePhoto_whenUserHasNoPhoto() throws Exception {
        // Arrange
        when(userService.getUserById(1)).thenReturn(user);

        // Act & Assert
        mockMvc.perform(get("/passenger/profile").session(session))
                .andExpect(view().name("passenger/profile"))
                .andExpect(model().attributeDoesNotExist("profilePhotoBase64"));
    }

    @Test
    void showProfile_shouldNotExposePhoto_whenPhotoIsEmpty() throws Exception {
        // Arrange
        user.setProfilePhoto(new byte[0]);
        when(userService.getUserById(1)).thenReturn(user);

        // Act & Assert
        mockMvc.perform(get("/passenger/profile").session(session))
                .andExpect(model().attributeDoesNotExist("profilePhotoBase64"));
    }

    @Test
    void showProfile_shouldRedirectToLogin_whenUserNoLongerExists() throws Exception {
        // Arrange
        when(userService.getUserById(1)).thenReturn(null);

        // Act & Assert
        mockMvc.perform(get("/passenger/profile").session(session))
                .andExpect(redirectedUrl("/auth/login"));
    }

    @Test
    void showProfileById_shouldShowProfile_whenIdIsOwn() throws Exception {
        // Arrange
        when(userService.getUserById(1)).thenReturn(user);

        // Act & Assert
        mockMvc.perform(get("/passenger/profile/1").session(session))
                .andExpect(view().name("passenger/profile"))
                .andExpect(model().attribute("user", user));
    }

    @Test
    void showProfileById_shouldRedirectToOwnProfile_whenIdBelongsToAnotherUser() throws Exception {
        mockMvc.perform(get("/passenger/profile/2").session(session))
                .andExpect(redirectedUrl("/passenger/profile"));

        verifyNoInteractions(userService);
    }

    @Test
    void showProfileById_shouldRedirectToLogin_whenNotLoggedIn() throws Exception {
        mockMvc.perform(get("/passenger/profile/1")).andExpect(redirectedUrl("/auth/login"));

        verifyNoInteractions(userService);
    }
}
