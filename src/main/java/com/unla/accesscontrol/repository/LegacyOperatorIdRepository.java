package com.unla.accesscontrol.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.unla.accesscontrol.entities.LegacyOperatorId;

public interface LegacyOperatorIdRepository extends JpaRepository<LegacyOperatorId, String> {
}
