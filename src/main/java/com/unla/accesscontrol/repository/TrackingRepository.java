package com.unla.accesscontrol.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.unla.accesscontrol.entities.Tracking;

public interface TrackingRepository extends JpaRepository<Tracking, Long> {
    @Modifying
    @Query("update Tracking t set t.car = null where t.car.id = :carId")
    void detachCar(@Param("carId") Long carId);
}
