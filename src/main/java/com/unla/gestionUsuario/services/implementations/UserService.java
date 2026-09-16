package com.unla.gestionUsuario.services.implementations;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.unla.gestionUsuario.entities.User;
import com.unla.gestionUsuario.entities.UserLog;
import com.unla.gestionUsuario.exceptions.UserException;
import com.unla.gestionUsuario.repository.UserRepository;
import com.unla.gestionUsuario.repository.UserLogRepository;
import com.unla.gestionUsuario.service.IUserService;

@Service
public class UserService implements IUserService{
	
	@Autowired
	private UserRepository userRepositorio;
	
	@Autowired
	private UserLogRepository UserLogRepository;
	
	@Override
	public User getUserById(String id) throws UserException {
		return userRepositorio.findById(id).orElseThrow(() -> UserException.of(UserException.Type.USER_NOT_FOUND));
	}
	
	@Override
	public User loginUser(User user) throws Exception{
		User userLogin = this.getUserById(user.getId());
		if(userLogin.isState()) {
			if(userLogin.getPassword().equals(user.getPassword())) {
				UserLog userLog = new UserLog();
				userLog.setUser(userLogin);
				userLog.setEvent("0");
				userLog.setDescription("Usuario logeado");
				UserLogRepository.save(userLog);
			}else {
				this.invalidPasswordCount(userLogin);
				throw UserException.of(UserException.Type.INVALID_PASSWORD);
			}
		}else {
			throw UserException.of(UserException.Type.BLOCK_USER);
		}
		
		return userLogin;
	}

	@Override
	public User createUser(User user) throws Exception {
		
		user.setBlockAmount(0);
		user.setState(true);
		
		userRepositorio.save(user);
		
		return user;
	}
	
	public void invalidPasswordCount(User user) {
		int blockAmount = user.getBlockAmount()+1;
		user.setBlockAmount(blockAmount);
		
		if(blockAmount>2) {
			user.setState(false);
		}
		
		userRepositorio.save(user);
	}
	
	public void resetInvalidPasswordCount() {
		//TODO
	}
}
