package com.unla.gestionUsuario.repository;

import java.util.Optional;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.unla.gestionUsuario.entities.User;

@Repository
public interface UserRepository extends CrudRepository<User, Integer>{

	public Optional<User> findById(String id);
}
