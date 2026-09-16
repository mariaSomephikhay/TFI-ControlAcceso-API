package com.unla.gestionUsuario;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import com.unla.gestionUsuario.dtos.UserDTO;
import com.unla.gestionUsuario.entities.User;
import com.unla.gestionUsuario.entities.UserLog;
import com.unla.gestionUsuario.exceptions.UserException;
import com.unla.gestionUsuario.mapper.IUserMapper;
import com.unla.gestionUsuario.repository.UserRepository;
import com.unla.gestionUsuario.repository.UserLogRepository;
import com.unla.gestionUsuario.services.implementations.UserService;

class GestionUsuarioApplicationTests {
    private UserRepository users;
    private UserLogRepository logs;
    private UserService service;
    private User stored;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        logs = mock(UserLogRepository.class);
        service = new UserService();
        ReflectionTestUtils.setField(service, "userRepositorio", users);
        ReflectionTestUtils.setField(service, "UserLogRepository", logs);
        stored = new User();
        stored.setId("test-user");
        stored.setPassword("test-password");
        stored.setState(true);
        when(users.findById("test-user")).thenReturn(Optional.of(stored));
    }

    @Test
    void createsActiveUserWithNoFailedAttempts() throws Exception {
        stored.setState(false);
        stored.setBlockAmount(3);
        assertSame(stored, service.createUser(stored));
        assertTrue(stored.isState());
        assertEquals(0, stored.getBlockAmount());
        verify(users).save(stored);
    }

    @Test
    void successfulLoginRecordsAccess() throws Exception {
        assertSame(stored, service.loginUser(stored));
        ArgumentCaptor<UserLog> event = ArgumentCaptor.forClass(UserLog.class);
        verify(logs).save(event.capture());
        assertSame(stored, event.getValue().getUser());
        assertEquals("0", event.getValue().getEvent());
        assertEquals("Usuario logeado", event.getValue().getDescription());
    }

    @Test
    void threeWrongPasswordsBlockFurtherLogin() throws Exception {
        User request = new User();
        request.setId(stored.getId());
        request.setPassword("wrong-password");
        for (int attempt = 1; attempt <= 3; attempt++) {
            UserException error = assertThrows(UserException.class, () -> service.loginUser(request));
            assertEquals(UserException.Type.INVALID_PASSWORD, error.getTipo());
            assertEquals(attempt, stored.getBlockAmount());
            assertEquals(attempt < 3, stored.isState());
        }
        UserException error = assertThrows(UserException.class, () -> service.loginUser(stored));
        assertEquals(UserException.Type.BLOCK_USER, error.getTipo());
        verify(users, times(3)).save(stored);
        verify(logs, never()).save(any(UserLog.class));
    }

    @Test
    void unknownUserIsRejected() {
        User request = new User();
        request.setId("missing");
        UserException error = assertThrows(UserException.class, () -> service.loginUser(request));
        assertEquals(UserException.Type.USER_NOT_FOUND, error.getTipo());
        verifyNoInteractions(logs);
    }

    @Test
    void generatedMapperPreservesCredentials() {
        IUserMapper mapper = Mappers.getMapper(IUserMapper.class);
        User mapped = mapper.dtoToUser(new UserDTO("test-user", "test-password"));
        assertEquals("test-user", mapped.getId());
        assertEquals("test-password", mapped.getPassword());
        assertFalse(mapped.isState());
        assertEquals(0, mapped.getBlockAmount());
        UserDTO roundTrip = mapper.userToDto(mapped);
        assertEquals(mapped.getId(), roundTrip.getId());
        assertEquals(mapped.getPassword(), roundTrip.getPassword());
    }
}