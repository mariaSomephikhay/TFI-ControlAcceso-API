package com.unla.accesscontrol.entities;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "tracking_history")
public class TrackingHistory {
    // Reuse tracking.id when archiving a completed stay.
    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "detected_plate", nullable = false, length = 16)
    private String detectedPlate;

    @Column(name = "entered_at", nullable = false, columnDefinition = "DATETIME(6)")
    private LocalDateTime enteredAt;

    @Column(name = "exited_at", nullable = false, columnDefinition = "DATETIME(6)")
    private LocalDateTime exitedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "authorization_method", nullable = false, columnDefinition = "ENUM('LICENSE_PLATE','CARD')")
    private AuthorizationMethod authorizationMethod;
}
