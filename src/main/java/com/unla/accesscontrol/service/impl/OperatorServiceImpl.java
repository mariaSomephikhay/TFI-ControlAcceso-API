package com.unla.accesscontrol.service.impl;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.unla.accesscontrol.entities.AuthenticationEvent;
import com.unla.accesscontrol.entities.User;
import com.unla.accesscontrol.repository.AuthenticationEventRepository;
import com.unla.accesscontrol.repository.LegacyOperatorIdRepository;
import com.unla.accesscontrol.repository.OperatorRepository;
import com.unla.accesscontrol.exceptions.UserException;
import com.unla.accesscontrol.exceptions.UserException.Type;
import com.unla.accesscontrol.service.OperatorService;

@Service
public class OperatorServiceImpl implements OperatorService {
    private final OperatorRepository users;
    private final AuthenticationEventRepository events;
    private final LegacyOperatorIdRepository legacyIds;

    public OperatorServiceImpl(OperatorRepository users, AuthenticationEventRepository events,
            LegacyOperatorIdRepository legacyIds) {
        this.users = users;
        this.events = events;
        this.legacyIds = legacyIds;
    }

    @Override
    @Transactional(rollbackFor = UserException.class)
    public void create(String username, String cardUid, String password) throws UserException {
        if (users.existsByUsernameOrCardUid(username, cardUid) || legacyIds.existsById(cardUid)) {
            throw UserException.of(Type.USER_ALREADY_EXISTS);
        }

        User operator = new User();
        operator.setUsername(username);
        operator.setCardUid(cardUid);
        operator.setPassword(password);
        try {
            users.saveAndFlush(operator);
        } catch (DataIntegrityViolationException duplicate) {
            throw new UserException(Type.USER_ALREADY_EXISTS, duplicate);
        }
    }

    @Override
    @Transactional(noRollbackFor = UserException.class)
    public void login(String identifier, String password, boolean legacyRequest) throws UserException {
        var match = legacyRequest ? users.findByLegacyIdOrCardUidForUpdate(identifier)
                : users.findByCardUidForUpdate(identifier);
        User operator = match
                .orElseThrow(() -> UserException.of(Type.USER_NOT_FOUND));

        if (!operator.isActive()) {
            throw UserException.of(Type.BLOCK_USER);
        }

        if (!operator.getPassword().equals(password)) {
            int failedAttempts = operator.getFailedAttempts() + 1;
            operator.setFailedAttempts(failedAttempts);
            if (failedAttempts >= 3) {
                operator.setActive(false);
            }
            throw UserException.of(Type.INVALID_PASSWORD);
        }

        events.save(new AuthenticationEvent(operator));
    }
}
