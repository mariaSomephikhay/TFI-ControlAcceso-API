package com.unla.gestionUsuario;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.unla.accesscontrol.entities.User;
import com.unla.accesscontrol.repository.OperatorRepository;

@SpringBootApplication(scanBasePackages = {"com.unla.gestionUsuario", "com.unla.accesscontrol"})
@ConfigurationPropertiesScan
@EntityScan(basePackageClasses = User.class)
@EnableJpaRepositories(basePackageClasses = OperatorRepository.class)
public class GestionUsuarioApplication {

	public static void main(String[] args) {
		SpringApplication.run(GestionUsuarioApplication.class, args);
	}

}
