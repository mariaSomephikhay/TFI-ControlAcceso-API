package com.unla.gestionUsuario.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import com.unla.gestionUsuario.entities.UserLog;

@Repository
public interface UserLogRepository extends CrudRepository<UserLog, Integer> {

}
