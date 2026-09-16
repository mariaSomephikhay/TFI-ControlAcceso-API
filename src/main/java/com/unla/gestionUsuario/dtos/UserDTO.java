package com.unla.gestionUsuario.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor @AllArgsConstructor
@Setter
@Getter
@ToString
public class UserDTO {
	private String id;
	private String password;
}
