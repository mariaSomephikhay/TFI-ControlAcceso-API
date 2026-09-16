package com.unla.gestionUsuario.entities;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity 
@Getter @Setter @NoArgsConstructor
@Table(name="user_log")
public class UserLog {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;
	
	@Column(name="event", nullable=false)
	private String event;
	
	@Column(name="description", nullable=false)
	private String description;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="user_id", nullable=true)
	private User user;
	
	@CreationTimestamp
	@Column(name="created_at", nullable=false)
	private LocalDateTime createdAt;
	
}
