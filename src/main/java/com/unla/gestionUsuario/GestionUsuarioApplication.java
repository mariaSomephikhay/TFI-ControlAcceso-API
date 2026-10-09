package com.unla.gestionUsuario;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class GestionUsuarioApplication {

	public static void main(String[] args) {
		SpringApplication.run(GestionUsuarioApplication.class, args);
	}

}
