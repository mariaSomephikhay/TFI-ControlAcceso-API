package com.unla.gestionUsuario.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.unla.gestionUsuario.dtos.UserDTO;
import com.unla.gestionUsuario.entities.User;

@Mapper(componentModel = "spring")
public interface IUserMapper {
	
	@Mapping(target = "state", ignore = true)
	@Mapping(target = "blockAmount", ignore = true)
    User dtoToUser(UserDTO dto);

    UserDTO userToDto(User user);
}
