package com.unla.accesscontrol;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.unla.accesscontrol.entities.AuthenticationEvent;
import com.unla.accesscontrol.entities.User;
import com.unla.accesscontrol.repository.AuthenticationEventRepository;
import com.unla.accesscontrol.repository.LegacyOperatorIdRepository;
import com.unla.accesscontrol.repository.OperatorRepository;
import com.unla.accesscontrol.service.OperatorService;
import com.unla.accesscontrol.exceptions.UserException;
import com.unla.accesscontrol.exceptions.UserException.Type;

class OperatorServiceTests {
    private final OperatorRepository users = mock(OperatorRepository.class);
    private final AuthenticationEventRepository events = mock(AuthenticationEventRepository.class);
    private final LegacyOperatorIdRepository legacyIds = mock(LegacyOperatorIdRepository.class);
    private final OperatorService service = new OperatorService(users, events, legacyIds);
    private final User stored = new User();

    @BeforeEach
    void setUp() {
        stored.setPassword("0123");
        when(users.findByCardUidForUpdate("A01")).thenReturn(Optional.of(stored));
    }

    @Test
    void createsActiveOperatorWithNoFailedAttempts() throws Exception {
        service.create("admin", "A01", "0123");
        ArgumentCaptor<User> user = ArgumentCaptor.forClass(User.class);
        verify(users).saveAndFlush(user.capture());
        assertEquals("admin", user.getValue().getUsername());
        assertEquals("A01", user.getValue().getCardUid());
        assertTrue(user.getValue().isActive());
        assertEquals(0, user.getValue().getFailedAttempts());
    }

    @Test
    void rejectsDuplicateWithoutSaving() {
        when(users.existsByUsernameOrCardUid("admin", "A01")).thenReturn(true);
        assertEquals(Type.USER_ALREADY_EXISTS, assertThrows(UserException.class,
                () -> service.create("admin", "A01", "0123")).getTipo());
        verify(users, never()).saveAndFlush(any());
    }

    @Test
    void rejectsUidReservedAsLegacyIdentifier() {
        when(legacyIds.existsById("legacy")).thenReturn(true);
        assertEquals(Type.USER_ALREADY_EXISTS, assertThrows(UserException.class,
                () -> service.create("admin", "legacy", "0123")).getTipo());
        verify(users, never()).saveAndFlush(any());
    }

    @Test
    void logsSuccessfulLegacyLoginWithoutResettingFailedAttempts() throws Exception {
        stored.setFailedAttempts(1);
        when(users.findByLegacyIdOrCardUidForUpdate("old-id")).thenReturn(Optional.of(stored));
        service.login("old-id", "0123", true);
        assertEquals(1, stored.getFailedAttempts());
        verify(events).save(any(AuthenticationEvent.class));
    }

    @Test
    void threeWrongPinsBlockFurtherAttempts() {
        for (int attempt = 1; attempt <= 3; attempt++) {
            var error = assertThrows(UserException.class, () -> service.login("A01", "wrong", false));
            assertEquals(Type.INVALID_PASSWORD, error.getTipo());
            assertEquals(attempt, stored.getFailedAttempts());
            assertEquals(attempt < 3, stored.isActive());
        }
        assertEquals(Type.BLOCK_USER, assertThrows(UserException.class,
                () -> service.login("A01", "0123", false)).getTipo());
        verifyNoInteractions(events);
    }

    @Test
    void unknownUserIsRejected() {
        assertEquals(Type.USER_NOT_FOUND, assertThrows(UserException.class,
                () -> service.login("missing", "0123", false)).getTipo());
        verifyNoInteractions(events);
    }
}
