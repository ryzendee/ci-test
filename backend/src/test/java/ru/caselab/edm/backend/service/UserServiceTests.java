package ru.caselab.edm.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.caselab.edm.backend.dto.CreateUserDTO;
import ru.caselab.edm.backend.dto.UpdatePasswordDTO;
import ru.caselab.edm.backend.dto.UpdateUserDTO;
import ru.caselab.edm.backend.dto.UserDTO;
import ru.caselab.edm.backend.entity.User;
import ru.caselab.edm.backend.exceptions.ResourceNotFoundException;
import ru.caselab.edm.backend.exceptions.UserAlreadyExistsException;
import ru.caselab.edm.backend.mapper.UserMapper;
import ru.caselab.edm.backend.repository.UserRepository;
import ru.caselab.edm.backend.service.impl.UserServiceImpl;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UserServiceTests {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userId = UUID.randomUUID();
        user = User.builder()
                .id(userId)
                .login("test")
                .email("test@test.ru")
                .firstName("test")
                .lastName("test")
                .patronymic("test")
                .password("test")
                .build();
    }



    @Test
    void getAllUsers_ShouldReturnUserDTOList() {
        Page<User> users = new PageImpl<>(Collections.singletonList(user));
        UserDTO userDTO = new UserDTO(userId, "test", "test@test.ru", "test", "test", "test");

        when(userRepository.findAll(PageRequest.of(0, 10))).thenReturn(users);
        when(userMapper.toDTO(any(User.class))).thenReturn(userDTO);

        List<UserDTO> result = userService.getAllUsers(0, 10);

        verify(userRepository, times(1)).findAll(PageRequest.of(0, 10));
        verify(userMapper, times(1)).toDTO(any(User.class));
        assertEquals(1, result.size());
        assertEquals(userDTO, result.get(0));

    }

    @Test
    void getUserById_WhenUserExists_ShouldReturnUserDTO() {
        UserDTO userDTO = new UserDTO(userId, "test", "test@test.ru", "test", "test", "test");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userMapper.toDTO(user)).thenReturn(userDTO);

        UserDTO result = userService.getUserById(userId);

        verify(userRepository, times(1)).findById(userId);
        verify(userMapper, times(1)).toDTO(any(User.class));

        assertEquals(userDTO, result);
    }

    @Test
    void getUserById_WhenUserNotFound_ShouldThrowResourceNotFoundException() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getUserById(userId));
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    void createUser_WhenValidDate_ShouldReturnUserDTOOfCreatedUser() {
        CreateUserDTO createUserDTO = new CreateUserDTO("test", "test@test.ru", "test", "test", "test", "test");
        UserDTO userDTO = new UserDTO(userId, "test", "test@test.ru", "test", "test", "test");

        when(userRepository.existsByEmail("test@test.ru")).thenReturn(false);
        when(userRepository.existsByLogin("test")).thenReturn(false);
        when(passwordEncoder.encode("test")).thenReturn("encodedTest");
        when(userMapper.toDTO(any(User.class))).thenReturn(userDTO);

        UserDTO result = userService.createUser(createUserDTO);

        verify(userRepository, times(1)).existsByEmail("test@test.ru");
        verify(userRepository, times(1)).existsByLogin("test");
        verify(passwordEncoder, times(1)).encode("test");
        verify(userRepository, times(1)).save(any(User.class));
        verify(userMapper, times(1)).toDTO(any(User.class));
        assertEquals(userDTO, result);
    }

    @Test
    void createUser_WhenEmailExists_ShouldThrowUserAlreadyExistException() {
        CreateUserDTO createUserDTO = new CreateUserDTO("test", "test@test.ru", "test", "test", "test", "test");

        when(userRepository.existsByEmail("test@test.ru")).thenReturn(true);
        when(userRepository.existsByLogin("test")).thenReturn(false);

        assertThrows(UserAlreadyExistsException.class, () -> userService.createUser(createUserDTO));
        verify(userRepository, times(1)).existsByEmail("test@test.ru");
    }

    @Test
    void createUser_WhenLoginExists_ShouldThrowUserAlreadyExistException() {
        CreateUserDTO createUserDTO = new CreateUserDTO("test", "test@test.ru", "test", "test", "test", "test");

        when(userRepository.existsByEmail("test@test.ru")).thenReturn(false);
        when(userRepository.existsByLogin("test")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> userService.createUser(createUserDTO));
        verify(userRepository, times(1)).existsByLogin("test");
    }

    @Test
    void updateUser_WhenValidDateAndLoginEmailChanged_ShouldReturnUserDTOOfUpdatedUser() {
        UpdateUserDTO updateUserDTO = new UpdateUserDTO("newTest", "newTest@test.ru", "newTest", "newTest", "newTest");
        UserDTO userDTO = new UserDTO(userId, "newTest", "newTest@test.ru", "newTest", "newTest", "newTest");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("newTest@test.ru")).thenReturn(false);
        when(userRepository.existsByLogin("newTest")).thenReturn(false);
        when(userMapper.toDTO(any(User.class))).thenReturn(userDTO);

        UserDTO result = userService.updateUser(userId, updateUserDTO);

        verify(userRepository, times(1)).existsByEmail("newTest@test.ru");
        verify(userRepository, times(1)).existsByLogin("newTest");
        verify(userRepository, times(1)).save(any(User.class));
        verify(userMapper, times(1)).toDTO(any(User.class));
        assertEquals(userDTO, result);

    }

    @Test
    void updateUser_WhenValidDateAndLoginEmailNotChanged_ShouldReturnUserDTOOfUpdatedUser() {
        UpdateUserDTO updateUserDTO = new UpdateUserDTO("test", "test@test.ru", "newTest", "newTest", "newTest");
        UserDTO userDTO = new UserDTO(userId, "test", "test@test.ru", "newTest", "newTest", "newTest");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("testt@test.ru")).thenReturn(true);
        when(userRepository.existsByLogin("test")).thenReturn(true);
        when(userMapper.toDTO(any(User.class))).thenReturn(userDTO);

        UserDTO result = userService.updateUser(userId, updateUserDTO);

        verify(userRepository, times(0)).existsByEmail("test@test.ru");
        verify(userRepository, times(0)).existsByLogin("test");
        verify(userRepository, times(1)).save(any(User.class));
        verify(userMapper, times(1)).toDTO(any(User.class));
        assertEquals(userDTO, result);
    }

    @Test
    void updateUser_WhenEmailExists_ShouldThrowUserAlreadyExistException() {
        UpdateUserDTO updateUserDTO = new UpdateUserDTO("test", "newTest@test.ru", "newTest", "newTest", "newTest");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("newTest@test.ru")).thenReturn(true);
        when(userRepository.existsByLogin("test")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> userService.updateUser(userId, updateUserDTO));
        verify(userRepository, times(1)).existsByEmail("newTest@test.ru");
    }

    @Test
    void updateUser_WhenLoginExists_ShouldThrowUserAlreadyExistException() {
        UpdateUserDTO updateUserDTO = new UpdateUserDTO("newTest", "test@test.ru", "newTest", "newTest", "newTest");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("test@test.ru")).thenReturn(true);
        when(userRepository.existsByLogin("newTest")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> userService.updateUser(userId, updateUserDTO));
        verify(userRepository, times(1)).existsByLogin("newTest");
    }

    @Test
    void updateUser_WhenUserNotFound_ShouldThrowResourceNotFoundException() {
        UpdateUserDTO updateUserDTO = new UpdateUserDTO("newTest", "test@test.ru", "newTest", "newTest", "newTest");

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.updateUser(userId, updateUserDTO));
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    void updatePassword_WhenValidPassword_ShouldReturnVoid() {
        UpdatePasswordDTO updatePasswordDTO = new UpdatePasswordDTO("test", "newTest");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("test", user.getPassword())).thenReturn(true);
        when(passwordEncoder.encode("newTest")).thenReturn("newTestEncoded");

        userService.updatePassword(userId, updatePasswordDTO);

        verify(userRepository, times(1)).findById(userId);
        verify(passwordEncoder, times(1)).matches("test", "test");
        verify(passwordEncoder, times(1)).encode("newTest");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void updatePassword_WhenInvalidPassword_ShouldThrowBadCredentialsException() {
        UpdatePasswordDTO updatePasswordDTO = new UpdatePasswordDTO("invalidTest", "newTest");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("invalidTest", user.getPassword())).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> userService.updatePassword(userId, updatePasswordDTO));
        verify(userRepository, times(1)).findById(userId);
        verify(passwordEncoder, times(1)).matches("invalidTest", user.getPassword());
    }

    @Test
    void updatePassword_WhenUserNotFound_ShouldThrowResourceNotFoundException() {
        UpdatePasswordDTO updatePasswordDTO = new UpdatePasswordDTO("newTest", "newTest");

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.updatePassword(userId, updatePasswordDTO));
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    void deleteUser_UserExists_ShouldReturnVoid() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        userService.deleteUser(userId);

        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, times(1)).delete(user);
    }

    @Test
    void deleteUser_UserNotFound_ShouldThrowResourceNotFoundException() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.deleteUser(userId));
        verify(userRepository, times(1)).findById(userId);
    }

}
