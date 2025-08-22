package com.trademaster.auth.repository;

import com.trademaster.auth.entity.SessionSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SessionSettingsRepository extends JpaRepository<SessionSettings, String> {

    Optional<SessionSettings> findByUserId(String userId);

    @Query("SELECT s FROM SessionSettings s WHERE s.maxConcurrentSessions > :threshold")
    List<SessionSettings> findByMaxConcurrentSessionsGreaterThan(@Param("threshold") int threshold);

    @Query("SELECT s FROM SessionSettings s WHERE s.sessionTimeoutMinutes > :threshold")
    List<SessionSettings> findBySessionTimeoutMinutesGreaterThan(@Param("threshold") int threshold);

    @Query("SELECT s FROM SessionSettings s WHERE s.extendOnActivity = true")
    List<SessionSettings> findByExtendOnActivityTrue();

    @Query("SELECT s FROM SessionSettings s WHERE s.requireMfaOnNewDevice = true")
    List<SessionSettings> findByRequireMfaOnNewDeviceTrue();

    @Query("SELECT AVG(s.sessionTimeoutMinutes) FROM SessionSettings s")
    Double getAverageSessionTimeout();

    @Query("SELECT AVG(s.maxConcurrentSessions) FROM SessionSettings s")
    Double getAverageMaxConcurrentSessions();

    @Query("SELECT COUNT(s) FROM SessionSettings s WHERE s.requireMfaOnNewDevice = true")
    long countUsersRequiringMfaOnNewDevice();

    @Query("SELECT COUNT(s) FROM SessionSettings s WHERE s.extendOnActivity = true")
    long countUsersWithExtendOnActivity();
}