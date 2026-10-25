package smart.fly.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import smart.fly.exception.AuthenticationException;
import smart.fly.passenger.model.Passenger;
import smart.fly.passenger.repository.PassengerRepository;
import smart.fly.user.model.User;
import smart.fly.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class PassengerServiceImplTest {

    @Mock
    private PassengerRepository passengerRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PassengerServiceImpl passengerService;

    private Passenger passenger;
    private User user;

    @BeforeEach
    void setUp() {
        passenger = new Passenger();
        passenger.setPassengerId(3);
        passenger.setName("Asha");
        passenger.setEmail("asha@example.com");
        passenger.setPhone("9876543210");

        user = new User();
        user.setUserId(1);
        user.setName("Asha");
        user.setEmail("asha@example.com");
        user.setPhone("9876543210");
    }

    // ---------- save ----------

    @Test
    void save_shouldPersistPassenger_whenEmailAndPhoneValid() {
        // Act
        Passenger result = passengerService.save(passenger);

        // Assert
        assertSame(passenger, result);
        verify(passengerRepository).save(passenger);
    }

    @Test
    void save_shouldThrowAndNotPersist_whenEmailInvalid() {
        // Arrange
        passenger.setEmail("not-an-email");

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> passengerService.save(passenger));
        verify(passengerRepository, never()).save(any());
    }

    @Test
    void save_shouldThrowAndNotPersist_whenPhoneInvalid() {
        // Arrange
        passenger.setPhone("123");

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> passengerService.save(passenger));
        verify(passengerRepository, never()).save(any());
    }

    // ---------- registerPassenger ----------

    @Test
    void registerPassenger_shouldThrow_whenEmailAlreadyRegistered() {
        // Arrange
        when(userRepository.emailExists(user.getEmail())).thenReturn(true);

        // Act
        AuthenticationException ex = assertThrows(AuthenticationException.class,
                () -> passengerService.registerPassenger(passenger, user, "secret123"));

        // Assert
        assertEquals("Email already registered", ex.getMessage());
        verify(userRepository, never()).save(any());
        verify(passengerRepository, never()).save(any());
    }

    @Test
    void registerPassenger_shouldThrow_whenUserEmailInvalid() {
        // Arrange
        user.setEmail("bad");

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> passengerService.registerPassenger(passenger, user, "secret123"));
        verifyNoInteractions(userRepository, passengerRepository);
    }

    @Test
    void registerPassenger_shouldSaveUserAsPassengerAndLinkProfile() {
        // Arrange
        User savedUser = new User();
        savedUser.setUserId(42);
        when(userRepository.emailExists(user.getEmail())).thenReturn(false);
        when(userRepository.save(user)).thenReturn(savedUser);

        // Act
        Passenger result = passengerService.registerPassenger(passenger, user, "secret123");

        // Assert
        assertEquals("PASSENGER", user.getRole());
        assertSame(savedUser, result.getUser());
        assertTrue(result.isRegistered());
        verify(passengerRepository).save(passenger);
    }

    // ---------- finders ----------

    @Test
    void findByUserId_shouldReturnPassenger_whenFound() {
        // Arrange
        when(passengerRepository.findByUserId(1)).thenReturn(passenger);

        // Act & Assert
        assertSame(passenger, passengerService.findByUserId(1));
    }

    @Test
    void findByUserId_shouldThrow_whenNotFound() {
        // Arrange
        when(passengerRepository.findByUserId(1)).thenReturn(null);

        // Act
        AuthenticationException ex = assertThrows(AuthenticationException.class,
                () -> passengerService.findByUserId(1));

        // Assert
        assertEquals("Passenger profile not found", ex.getMessage());
    }

    @Test
    void findById_shouldReturnPassenger_whenFound() {
        // Arrange
        when(passengerRepository.findById(3)).thenReturn(passenger);

        // Act & Assert
        assertSame(passenger, passengerService.findById(3));
    }

    @Test
    void findById_shouldThrow_whenNotFound() {
        // Arrange
        when(passengerRepository.findById(3)).thenReturn(null);

        // Act & Assert
        assertThrows(AuthenticationException.class, () -> passengerService.findById(3));
    }

    @Test
    void findByEmail_shouldDelegateToRepository() {
        // Arrange
        when(passengerRepository.findByEmail("asha@example.com")).thenReturn(passenger);

        // Act & Assert
        assertSame(passenger, passengerService.findByEmail("asha@example.com"));
    }

    @Test
    void findByPhone_shouldDelegateToRepository() {
        // Arrange
        when(passengerRepository.findByPhone("9876543210")).thenReturn(passenger);

        // Act & Assert
        assertSame(passenger, passengerService.findByPhone("9876543210"));
    }

    // ---------- addUnregisteredPassenger ----------

    @Test
    void addUnregisteredPassenger_shouldMarkAsNotRegisteredAndSave() {
        // Arrange
        passenger.setRegistered(true);

        // Act
        Passenger result = passengerService.addUnregisteredPassenger(passenger);

        // Assert
        assertFalse(result.isRegistered());
        verify(passengerRepository).save(passenger);
    }

    // ---------- linkToRegisteredUser ----------

    @Test
    void linkToRegisteredUser_shouldThrow_whenPassengerNotFound() {
        // Arrange
        when(passengerRepository.findById(3)).thenReturn(null);

        // Act
        AuthenticationException ex = assertThrows(AuthenticationException.class,
                () -> passengerService.linkToRegisteredUser(3, 1));

        // Assert
        assertEquals("Passenger not found", ex.getMessage());
        verify(passengerRepository, never()).linkToUser(anyInt(), anyInt());
    }

    @Test
    void linkToRegisteredUser_shouldThrow_whenUserNotFound() {
        // Arrange
        when(passengerRepository.findById(3)).thenReturn(passenger);
        when(userRepository.findById(1)).thenReturn(null);

        // Act
        AuthenticationException ex = assertThrows(AuthenticationException.class,
                () -> passengerService.linkToRegisteredUser(3, 1));

        // Assert
        assertEquals("User not found", ex.getMessage());
        verify(passengerRepository, never()).linkToUser(anyInt(), anyInt());
    }

    @Test
    void linkToRegisteredUser_shouldLink_whenBothExist() {
        // Arrange
        when(passengerRepository.findById(3)).thenReturn(passenger);
        when(userRepository.findById(1)).thenReturn(user);

        // Act
        passengerService.linkToRegisteredUser(3, 1);

        // Assert
        verify(passengerRepository).linkToUser(3, 1);
    }

    // ---------- updatePassengerProfile ----------

    @Test
    void updatePassengerProfile_shouldThrow_whenPassengerNotFound() {
        // Arrange
        when(passengerRepository.findById(3)).thenReturn(null);

        // Act & Assert
        assertThrows(AuthenticationException.class, () -> passengerService.updatePassengerProfile(passenger));
        verify(passengerRepository, never()).update(any());
    }

    @Test
    void updatePassengerProfile_shouldKeepRegistrationAndUserFromExistingRecord() {
        // Arrange
        Passenger existing = new Passenger();
        existing.setRegistered(true);
        existing.setUser(user);
        when(passengerRepository.findById(3)).thenReturn(existing);

        Passenger update = new Passenger();
        update.setPassengerId(3);
        update.setName("Asha K");
        update.setRegistered(false);

        // Act
        passengerService.updatePassengerProfile(update);

        // Assert
        ArgumentCaptor<Passenger> captor = ArgumentCaptor.forClass(Passenger.class);
        verify(passengerRepository).update(captor.capture());
        assertEquals("Asha K", captor.getValue().getName());
        assertTrue(captor.getValue().isRegistered());
        assertSame(user, captor.getValue().getUser());
    }

    // ---------- ensurePassengerProfile ----------

    @Test
    void ensurePassengerProfile_shouldReturnExistingProfile() {
        // Arrange
        when(passengerRepository.findByUserId(1)).thenReturn(passenger);

        // Act
        Passenger result = passengerService.ensurePassengerProfile(user);

        // Assert
        assertSame(passenger, result);
        verify(passengerRepository, never()).save(any());
    }

    @Test
    void ensurePassengerProfile_shouldCreateProfileFromUser_whenMissing() {
        // Arrange
        when(passengerRepository.findByUserId(1)).thenReturn(null);

        // Act
        Passenger result = passengerService.ensurePassengerProfile(user);

        // Assert
        assertEquals(user.getName(), result.getName());
        assertEquals(user.getEmail(), result.getEmail());
        assertEquals(user.getPhone(), result.getPhone());
        assertSame(user, result.getUser());
        assertTrue(result.isRegistered());
        verify(passengerRepository).save(result);
    }
}
