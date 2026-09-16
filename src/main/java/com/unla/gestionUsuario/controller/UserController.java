package com.unla.gestionUsuario.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.unla.gestionUsuario.dtos.UserDTO;
import com.unla.gestionUsuario.mapper.IUserMapper;
import com.unla.gestionUsuario.services.implementations.UserService;

@RestController
@RequestMapping("/users")
public class UserController {

	@Autowired
	UserService userService;
	
	@Autowired
	IUserMapper userMapper;
	
	@PostMapping("/new")
	public ResponseEntity<Object> newUser (@RequestBody UserDTO newUser){	
		try {	
			userService.createUser(userMapper.dtoToUser(newUser));
			return ResponseEntity.status(HttpStatus.CREATED).body("Usuario creado exitosamente");
		}catch(Exception e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error al crear el usuario, " + e.getMessage());
		}
	}
	
	@PostMapping("/login")
	public ResponseEntity<Object> loginUser (@RequestBody UserDTO newUser){	
		try {	
			userService.loginUser(userMapper.dtoToUser(newUser));
			return ResponseEntity.status(HttpStatus.OK).body("Usuario logeado correctamente");
		}catch(Exception e) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Error al iniciar sesion, " + e.getMessage());
		}
	}
}
