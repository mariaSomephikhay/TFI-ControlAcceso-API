package com.unla.accesscontrol.service;

import com.unla.accesscontrol.exceptions.UserException;

public interface OperatorService {
    void create(String username, String cardUid, String password) throws UserException;

    void login(String identifier, String password, boolean legacyRequest) throws UserException;
}
