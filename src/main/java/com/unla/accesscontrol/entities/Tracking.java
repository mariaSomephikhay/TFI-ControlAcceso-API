package com.unla.accesscontrol.entities;

import java.time.LocalDateTime;

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
import lombok.Setter;

@Getter
@Entity
@Table(name = "tracking")
public class Tracking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_id")
    private Car car;

    @Setter
    @Column(name = "detected_plate", nullable = false, length = 16)
    private String detectedPlate;

    @Setter
    @Column(name = "plate_registered", nullable = false, columnDefinition = "BOOLEAN")
    private boolean plateRegistered;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Setter
    @Column(name = "entered_at", nullable = false, columnDefinition = "DATETIME(6)")
    private LocalDateTime enteredAt;

    @Setter
    @Column(name = "exited_at", columnDefinition = "DATETIME(6)")
    private LocalDateTime exitedAt;
}
