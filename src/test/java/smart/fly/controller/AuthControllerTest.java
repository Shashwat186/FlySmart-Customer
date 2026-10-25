package smart.fly.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import smart.fly.exception.AuthenticationException;
import smart.fly.service.AuthService;
import smart.fly.service.PassengerService;
import smart.fly.service.PasswordResetService;
import smart.fly.service.UserService;
import smart.fly.user.model.User;
import smart.fly.user.repository.UserRepository;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private PassengerService passengerService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private PasswordResetService passwordResetService;

    @MockitoBean
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private UserService userService;

    private User passengerUser;

    @BeforeEach
    void setUp() {
        passengerUser = new User();
        passengerUser.setUserId(1);
        passengerUser.setEmail("user@example.com");
        passengerUser.setRole("PASSENGER");
    }

    // ---------- login ----------

    @Test
    void showLoginForm_shouldReturnLoginView() throws Exception {
        mockMvc.perform(get("/auth/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"))
                .andExpect(model().attributeExists("user"));
    }

    @Test
    void processLogin_shouldStoreUserInSessionAndRedirect_whenPassenger() throws Exception {
        // Arrange
        when(authService.authenticate("user@example.com", "secret123")).thenReturn(passengerUser);

        // Act & Assert
        mockMvc.perform(post("/auth/login").param("email", "user@example.com").param("password", "secret123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/passenger/dashboard"))
                .andExpect(request().sessionAttribute("user", passengerUser))
                .andExpect(request().sessionAttribute("loggedInUserId", 1));
    }

    @Test
    void processLogin_shouldAcceptPassengerRoleCaseInsensitively() throws Exception {
        // Arrange
        passengerUser.setRole("passenger");
        when(authService.authenticate("user@example.com", "secret123")).thenReturn(passengerUser);

        // Act & Assert
        mockMvc.perform(post("/auth/login").param("email", "user@example.com").param("password", "secret123"))
                .andExpect(redirectedUrl("/passenger/dashboard"));
    }

    @Test
    void processLogin_shouldRejectNonPassengerAccounts() throws Exception {
        // Arrange
        passengerUser.setRole("MANAGER");
        when(authService.authenticate("user@example.com", "secret123")).thenReturn(passengerUser);

        // Act & Assert
        mockMvc.perform(post("/auth/login").param("email", "user@example.com").param("password", "secret123"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"))
                .andExpect(model().attribute("error", "Invalid email or password"))
                .andExpect(model().attribute("email", "user@example.com"))
                .andExpect(request().sessionAttributeDoesNotExist("user"));
    }

    @Test
    void processLogin_shouldShowError_whenCredentialsInvalid() throws Exception {
        // Arrange
        when(authService.authenticate("user@example.com", "wrong"))
                .thenThrow(new AuthenticationException("Invalid email or password"));

        // Act & Assert
        mockMvc.perform(post("/auth/login").param("email", "user@example.com").param("password", "wrong"))
                .andExpect(view().name("auth/login"))
                .andExpect(model().attribute("error", "Invalid email or password"))
                .andExpect(model().attribute("email", "user@example.com"))
                .andExpect(request().sessionAttributeDoesNotExist("user"));
    }

    // ---------- register ----------

    @Test
    void showRegistrationForm_shouldReturnRegisterView() throws Exception {
        mockMvc.perform(get("/auth/register"))
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeExists("user"));
    }

    @Test
    void processRegistration_shouldRedirectToLogin_whenSuccessful() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/auth/register").param("name", "Asha").param("email", "asha@example.com")
                .param("phone", "9876543210").param("password", "secret123"))
                .andExpect(redirectedUrl("/auth/login"));

        verify(authService).registerUser(argThat(u -> "asha@example.com".equals(u.getEmail())), eq("secret123"));
    }

    @Test
    void processRegistration_shouldShowDuplicateMessage_whenEmailOrPhoneExists() throws Exception {
        // Arrange
        when(authService.registerUser(any(User.class), eq("secret123")))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry"));

        // Act & Assert
        mockMvc.perform(post("/auth/register").param("email", "asha@example.com").param("password", "secret123"))
                .andExpect(view().name("auth/register"))
                .andExpect(model().attribute("error", "An account with this email or phone number already exists"))
                .andExpect(model().attributeExists("user"));
    }

    @Test
    void processRegistration_shouldShowGenericError_whenAuthenticationExceptionThrown() throws Exception {
        // Arrange
        when(authService.registerUser(any(User.class), eq("secret123")))
                .thenThrow(new AuthenticationException("boom"));

        // Act & Assert
        mockMvc.perform(post("/auth/register").param("email", "asha@example.com").param("password", "secret123"))
                .andExpect(view().name("auth/register"))
                .andExpect(model().attribute("error", "An error occured"));
    }

    // ---------- forgot / reset password ----------

    @Test
    void showForgotPasswordForm_shouldReturnView() throws Exception {
        mockMvc.perform(get("/auth/forgot-password")).andExpect(view().name("auth/forgot-password"));
    }

    @Test
    void processForgotPassword_shouldCreateTokenAndExposeResetLink() throws Exception {
        // Arrange
        when(jdbcTemplate.queryForObject("SELECT token FROM password_reset_tokens WHERE email = ?", String.class,
                "user@example.com")).thenReturn("abc");

        // Act & Assert
        mockMvc.perform(post("/auth/forgot-password").param("email", "user@example.com"))
                .andExpect(view().name("auth/forgot-password"))
                .andExpect(model().attribute("resetLink", "/auth/reset-password?token=abc"))
                .andExpect(model().attributeExists("message"));

        verify(passwordResetService).createResetToken("user@example.com");
    }

    @Test
    void showResetPasswordForm_shouldRedirect_whenTokenInvalid() throws Exception {
        // Arrange
        when(passwordResetService.validateToken("bad")).thenReturn(false);

        // Act & Assert
        mockMvc.perform(get("/auth/reset-password").param("token", "bad"))
                .andExpect(redirectedUrl("/auth/forgot-password"));
    }

    @Test
    void showResetPasswordForm_shouldShowForm_whenTokenValid() throws Exception {
        // Arrange
        when(passwordResetService.validateToken("good")).thenReturn(true);

        // Act & Assert
        mockMvc.perform(get("/auth/reset-password").param("token", "good"))
                .andExpect(view().name("auth/reset-password"))
                .andExpect(model().attribute("token", "good"));
    }

    @Test
    void processResetPassword_shouldShowError_whenPasswordsDoNotMatch() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/auth/reset-password").param("token", "good").param("newPassword", "abc12345")
                .param("confirmPassword", "different"))
                .andExpect(view().name("auth/reset-password"))
                .andExpect(model().attribute("error", "Passwords do not match"))
                .andExpect(model().attribute("token", "good"));

        verifyNoInteractions(passwordResetService);
    }

    @Test
    void processResetPassword_shouldRedirect_whenTokenInvalid() throws Exception {
        // Arrange
        when(passwordResetService.validateToken("bad")).thenReturn(false);

        // Act & Assert
        mockMvc.perform(post("/auth/reset-password").param("token", "bad").param("newPassword", "abc12345")
                .param("confirmPassword", "abc12345"))
                .andExpect(redirectedUrl("/auth/forgot-password"));

        verify(passwordResetService, never()).updatePassword(anyString(), anyString());
    }

    @Test
    void processResetPassword_shouldUpdatePasswordAndDeleteToken_whenValid() throws Exception {
        // Arrange
        when(passwordResetService.validateToken("good")).thenReturn(true);
        when(passwordResetService.getEmailFromToken("good")).thenReturn("user@example.com");
        when(passwordResetService.updatePassword("user@example.com", "abc12345")).thenReturn(true);

        // Act & Assert
        mockMvc.perform(post("/auth/reset-password").param("token", "good").param("newPassword", "abc12345")
                .param("confirmPassword", "abc12345"))
                .andExpect(view().name("auth/login"))
                .andExpect(model().attribute("success", "Password reset successfully. You can now login."));

        verify(passwordResetService).deleteToken("good");
    }

    @Test
    void processResetPassword_shouldShowError_whenUpdateFails() throws Exception {
        // Arrange
        when(passwordResetService.validateToken("good")).thenReturn(true);
        when(passwordResetService.getEmailFromToken("good")).thenReturn("user@example.com");
        when(passwordResetService.updatePassword("user@example.com", "abc12345")).thenReturn(false);

        // Act & Assert
        mockMvc.perform(post("/auth/reset-password").param("token", "good").param("newPassword", "abc12345")
                .param("confirmPassword", "abc12345"))
                .andExpect(view().name("auth/reset-password"))
                .andExpect(model().attribute("error", "Failed to reset password"));

        verify(passwordResetService, never()).deleteToken(anyString());
    }

    // ---------- logout ----------

    @Test
    void logout_shouldInvalidateSessionAndRedirectToLogin() throws Exception {
        // Arrange
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", passengerUser);

        // Act
        mockMvc.perform(get("/auth/logout").session(session)).andExpect(redirectedUrl("/auth/login"));

        // Assert
        assertTrue(session.isInvalid());
    }

    // ---------- change password ----------

    @Test
    void showChangePasswordForm_shouldReturnView() throws Exception {
        mockMvc.perform(get("/auth/change-password")).andExpect(view().name("auth/change-password"));
    }

    @Test
    void processChangePassword_shouldRedirectToLogin_whenNotLoggedIn() throws Exception {
        mockMvc.perform(post("/auth/change-password").param("currentPassword", "old")
                .param("newPassword", "new12345").param("confirmPassword", "new12345"))
                .andExpect(redirectedUrl("/auth/login"));

        verifyNoInteractions(authService);
    }

    @Test
    void processChangePassword_shouldShowError_whenNewPasswordsDoNotMatch() throws Exception {
        // Arrange
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", passengerUser);

        // Act & Assert
        mockMvc.perform(post("/auth/change-password").session(session).param("currentPassword", "old")
                .param("newPassword", "new12345").param("confirmPassword", "other"))
                .andExpect(view().name("auth/change-password"))
                .andExpect(request().attribute("error", "New passwords do not match"));

        verifyNoInteractions(authService);
    }

    @Test
    void processChangePassword_shouldShowSuccess_whenPasswordChanged() throws Exception {
        // Arrange
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", passengerUser);

        // Act
        MvcResult result = mockMvc.perform(post("/auth/change-password").session(session)
                .param("currentPassword", "old").param("newPassword", "new12345")
                .param("confirmPassword", "new12345"))
                .andExpect(view().name("auth/change-password"))
                .andExpect(request().attribute("success", "Password changed successfully"))
                .andReturn();

        // Assert
        assertNull(result.getRequest().getAttribute("error"));
        verify(authService).changePassword(passengerUser, "old", "new12345");
    }

    @Test
    void processChangePassword_shouldShowError_whenCurrentPasswordWrong() throws Exception {
        // Arrange
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", passengerUser);
        doThrow(new AuthenticationException("Current password is incorrect")).when(authService)
                .changePassword(passengerUser, "wrong", "new12345");

        // Act & Assert
        mockMvc.perform(post("/auth/change-password").session(session).param("currentPassword", "wrong")
                .param("newPassword", "new12345").param("confirmPassword", "new12345"))
                .andExpect(view().name("auth/change-password"))
                .andExpect(request().attribute("error", "Current password is incorrect"));
    }
}
