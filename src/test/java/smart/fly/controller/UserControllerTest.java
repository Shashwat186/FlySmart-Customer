package smart.fly.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
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

    @Test
    void showProfile_shouldReturnProfileWithEncodedPhoto_whenUserHasPhoto() throws Exception {
        // Arrange
        byte[] photo = { 1, 2, 3, 4 };
        User user = new User();
        user.setUserId(1);
        user.setProfilePhoto(photo);
        when(userService.getUserById(1)).thenReturn(user);

        // Act & Assert
        mockMvc.perform(get("/passenger/profile/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("passenger/profile"))
                .andExpect(model().attribute("user", user))
                .andExpect(model().attribute("profilePhotoBase64", Base64.getEncoder().encodeToString(photo)));
    }

    @Test
    void showProfile_shouldNotExposePhoto_whenUserHasNoPhoto() throws Exception {
        // Arrange
        User user = new User();
        user.setUserId(1);
        when(userService.getUserById(1)).thenReturn(user);

        // Act & Assert
        mockMvc.perform(get("/passenger/profile/1"))
                .andExpect(view().name("passenger/profile"))
                .andExpect(model().attributeDoesNotExist("profilePhotoBase64"));
    }

    @Test
    void showProfile_shouldNotExposePhoto_whenPhotoIsEmpty() throws Exception {
        // Arrange
        User user = new User();
        user.setProfilePhoto(new byte[0]);
        when(userService.getUserById(1)).thenReturn(user);

        // Act & Assert
        mockMvc.perform(get("/passenger/profile/1"))
                .andExpect(model().attributeDoesNotExist("profilePhotoBase64"));
    }

    @Test
    void showProfile_shouldReturnErrorView_whenUserNotFound() throws Exception {
        // Arrange
        when(userService.getUserById(99)).thenReturn(null);

        // Act & Assert
        mockMvc.perform(get("/passenger/profile/99"))
                .andExpect(view().name("error"))
                .andExpect(model().attribute("error", "User not found"));
    }
}
