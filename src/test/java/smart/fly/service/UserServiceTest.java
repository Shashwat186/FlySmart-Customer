package smart.fly.service;
 
import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;

import org.mockito.Mock;

import org.mockito.junit.jupiter.MockitoExtension;

import smart.fly.user.model.User;

import smart.fly.user.repository.UserRepository;
 

 
import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.Mockito.*;
 
@ExtendWith(MockitoExtension.class)

class UserServiceTest {
 
    @Mock

    private UserRepository userRepository;
 
    @InjectMocks

    private UserService userService;
 
    private User testUser;
 
    @BeforeEach

    void setUp() {

        testUser = new User();

        testUser.setUserId(1);

        testUser.setRole("PASSENGER");

    }
 
    @Test

    void getUserById_shouldReturnUser_whenUserExists() {

        // Arrange

        when(userRepository.findById(1)).thenReturn(testUser);
 
        // Act

        User result = userService.getUserById(1);
 
        // Assert

        assertNotNull(result);

        assertEquals(testUser, result);

        verify(userRepository, times(1)).findById(1);

    }
 
    @Test

    void getUserById_shouldReturnNull_whenUserDoesNotExist() {

        // Arrange

        when(userRepository.findById(999)).thenReturn(null);
 
        // Act

        User result = userService.getUserById(999);
 
        // Assert

        assertNull(result);

        verify(userRepository, times(1)).findById(999);

    }

}
