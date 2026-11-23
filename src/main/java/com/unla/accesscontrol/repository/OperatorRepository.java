package com.unla.accesscontrol.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.unla.accesscontrol.entities.User;

import jakarta.persistence.LockModeType;

public interface OperatorRepository extends JpaRepository<User, Long> {
    boolean existsByUsernameOrCardUid(String username, String cardUid);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.cardUid = :cardUid")
    Optional<User> findByCardUidForUpdate(@Param("cardUid") String cardUid);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select u from User u where u.cardUid = :identifier or u.id in (
                select a.user.id from LegacyOperatorId a where a.legacyId = :identifier
            )
            """)
    Optional<User> findByLegacyIdOrCardUidForUpdate(@Param("identifier") String identifier);
}
