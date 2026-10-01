package com.unla.gestionUsuario.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.unla.gestionUsuario.entities.User;

@Repository
public interface UserRepository extends CrudRepository<User, String>{
}
