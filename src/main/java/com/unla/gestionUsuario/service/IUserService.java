package com.unla.gestionUsuario.service;

import com.unla.gestionUsuario.entities.User;

public interface IUserService {
	public User getUserById(String id) throws Exception;
	public User createUser(User user)throws Exception;
	public User loginUser(User user)throws Exception;
}
