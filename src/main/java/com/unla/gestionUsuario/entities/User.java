package com.unla.gestionUsuario.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity 
@Getter @Setter @NoArgsConstructor
@Table(name="user")
public class User {
	@Id
	@Column(name="user_id", nullable=false)
	private String id;
	
	@Column(name="password", nullable=false)
	private String password;
	
	@Column(name="state", nullable=false)
	private boolean state;
	
	@Column(name="block_amount", nullable=false)
	private int blockAmount;
}
