package com.trademaster.marketdata.repository;

import com.trademaster.marketdata.entity.EconomicEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Economic Event Repository
 * 
 * Provides data access methods for economic calendar events
 * with optimized queries for market analysis.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Repository
public interface EconomicEventRepository extends JpaRepository<EconomicEvent, Long> {
    
    /**
     * Find event by external event ID
     */
    Optional<EconomicEvent> findByEventId(String eventId);
    
    /**
     * Get events for a specific date range
     */
    @Query("SELECT e FROM EconomicEvent e WHERE e.eventDate BETWEEN :startDate AND :endDate " +
           "ORDER BY e.eventDate ASC, e.importance DESC")
    List<EconomicEvent> findEventsByDateRange(
        @Param("startDate") LocalDateTime startDate, 
        @Param("endDate") LocalDateTime endDate);
    
    /**
     * Get events for a specific date range with pagination
     */
    @Query("SELECT e FROM EconomicEvent e WHERE e.eventDate BETWEEN :startDate AND :endDate " +
           "ORDER BY e.eventDate ASC, e.importance DESC")
    Page<EconomicEvent> findEventsByDateRange(
        @Param("startDate") LocalDateTime startDate, 
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable);
    
    /**
     * Get today's events
     */
    @Query("SELECT e FROM EconomicEvent e WHERE DATE(e.eventDate) = CURRENT_DATE " +
           "ORDER BY e.eventDate ASC, e.importance DESC")
    List<EconomicEvent> findTodaysEvents();
    
    /**
     * Get upcoming events (next 7 days)
     */
    @Query("SELECT e FROM EconomicEvent e WHERE e.eventDate > CURRENT_TIMESTAMP " +
           "AND e.eventDate <= :endDate " +
           "ORDER BY e.eventDate ASC, e.importance DESC")
    List<EconomicEvent> findUpcomingEvents(@Param("endDate") LocalDateTime endDate);
    
    /**
     * Get events by importance level
     */
    @Query("SELECT e FROM EconomicEvent e WHERE e.importance = :importance " +
           "AND e.eventDate BETWEEN :startDate AND :endDate " +
           "ORDER BY e.eventDate ASC")
    List<EconomicEvent> findEventsByImportance(
        @Param("importance") EconomicEvent.EventImportance importance,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate);
    
    /**
     * Get high importance events (HIGH and CRITICAL)
     */
    @Query("SELECT e FROM EconomicEvent e WHERE e.importance IN ('HIGH', 'CRITICAL') " +
           "AND e.eventDate BETWEEN :startDate AND :endDate " +
           "ORDER BY e.eventDate ASC, e.importance DESC")
    List<EconomicEvent> findHighImportanceEvents(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate);
    
    /**
     * Get events by country
     */
    @Query("SELECT e FROM EconomicEvent e WHERE e.country = :country " +
           "AND e.eventDate BETWEEN :startDate AND :endDate " +
           "ORDER BY e.eventDate ASC, e.importance DESC")
    List<EconomicEvent> findEventsByCountry(
        @Param("country") String country,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate);
    
    /**
     * Get events by multiple countries
     */
    @Query("SELECT e FROM EconomicEvent e WHERE e.country IN :countries " +
           "AND e.eventDate BETWEEN :startDate AND :endDate " +
           "ORDER BY e.eventDate ASC, e.importance DESC")
    List<EconomicEvent> findEventsByCountries(
        @Param("countries") List<String> countries,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate);
    
    /**
     * Get events by category
     */
    @Query("SELECT e FROM EconomicEvent e WHERE e.category = :category " +
           "AND e.eventDate BETWEEN :startDate AND :endDate " +
           "ORDER BY e.eventDate ASC")
    List<EconomicEvent> findEventsByCategory(
        @Param("category") String category,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate);
    
    /**
     * Get market moving events (high impact score or high importance)
     */
    @Query("SELECT e FROM EconomicEvent e WHERE " +
           "(e.marketImpactScore >= :minImpactScore OR e.importance IN ('HIGH', 'CRITICAL')) " +
           "AND e.eventDate BETWEEN :startDate AND :endDate " +
           "ORDER BY e.eventDate ASC, e.marketImpactScore DESC")
    List<EconomicEvent> findMarketMovingEvents(
        @Param("minImpactScore") java.math.BigDecimal minImpactScore,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate);
    
    /**
     * Get released events with surprises (actual vs forecast deviation)
     */
    @Query("SELECT e FROM EconomicEvent e WHERE e.status = 'RELEASED' " +
           "AND e.actualValue IS NOT NULL AND e.forecastValue IS NOT NULL " +
           "AND ABS((e.actualValue - e.forecastValue) / e.forecastValue) >= :minSurprisePercent " +
           "AND e.eventDate BETWEEN :startDate AND :endDate " +
           "ORDER BY ABS((e.actualValue - e.forecastValue) / e.forecastValue) DESC")
    List<EconomicEvent> findEventsWithSurprises(
        @Param("minSurprisePercent") java.math.BigDecimal minSurprisePercent,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate);
    
    /**
     * Get events by status
     */
    List<EconomicEvent> findByStatusAndEventDateBetweenOrderByEventDateAsc(
        EconomicEvent.EventStatus status,
        LocalDateTime startDate,
        LocalDateTime endDate);
    
    /**
     * Get events for specific time range (intraday)
     */
    @Query("SELECT e FROM EconomicEvent e WHERE e.eventDate BETWEEN :startTime AND :endTime " +
           "ORDER BY e.eventDate ASC, e.importance DESC")
    List<EconomicEvent> findEventsByTimeRange(
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime);
    
    /**
     * Get events affecting global markets
     */
    @Query("SELECT e FROM EconomicEvent e WHERE " +
           "(e.country IN ('USA', 'CHN', 'EUR') OR e.importance = 'CRITICAL') " +
           "AND e.eventDate BETWEEN :startDate AND :endDate " +
           "ORDER BY e.eventDate ASC, e.importance DESC")
    List<EconomicEvent> findGlobalMarketEvents(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate);
    
    /**
     * Search events by title or description
     */
    @Query("SELECT e FROM EconomicEvent e WHERE " +
           "(LOWER(e.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(e.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "AND e.eventDate BETWEEN :startDate AND :endDate " +
           "ORDER BY e.eventDate ASC, e.importance DESC")
    List<EconomicEvent> searchEventsByText(
        @Param("searchTerm") String searchTerm,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate);
    
    /**
     * Get events requiring data updates (released but no actual value)
     */
    @Query("SELECT e FROM EconomicEvent e WHERE e.status = 'RELEASED' " +
           "AND e.actualValue IS NULL " +
           "AND e.eventDate >= :cutoffDate " +
           "ORDER BY e.eventDate DESC")
    List<EconomicEvent> findEventsNeedingUpdates(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    /**
     * Get statistics by country for a date range
     */
    @Query("SELECT e.country, COUNT(e), " +
           "AVG(CASE WHEN e.marketImpactScore IS NOT NULL THEN e.marketImpactScore ELSE 0 END) " +
           "FROM EconomicEvent e WHERE e.eventDate BETWEEN :startDate AND :endDate " +
           "GROUP BY e.country ORDER BY COUNT(e) DESC")
    List<Object[]> getEventStatisticsByCountry(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate);
    
    /**
     * Get statistics by category for a date range
     */
    @Query("SELECT e.category, COUNT(e), " +
           "AVG(CASE WHEN e.marketImpactScore IS NOT NULL THEN e.marketImpactScore ELSE 0 END) " +
           "FROM EconomicEvent e WHERE e.eventDate BETWEEN :startDate AND :endDate " +
           "GROUP BY e.category ORDER BY COUNT(e) DESC")
    List<Object[]> getEventStatisticsByCategory(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate);
    
    /**
     * Get monthly event count for trend analysis
     */
    @Query("SELECT YEAR(e.eventDate), MONTH(e.eventDate), COUNT(e) " +
           "FROM EconomicEvent e WHERE e.eventDate BETWEEN :startDate AND :endDate " +
           "GROUP BY YEAR(e.eventDate), MONTH(e.eventDate) " +
           "ORDER BY YEAR(e.eventDate), MONTH(e.eventDate)")
    List<Object[]> getMonthlyEventCounts(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate);
    
    /**
     * Get events with revisions
     */
    @Query("SELECT e FROM EconomicEvent e WHERE e.revisionValue IS NOT NULL " +
           "AND e.eventDate BETWEEN :startDate AND :endDate " +
           "ORDER BY e.eventDate DESC")
    List<EconomicEvent> findEventsWithRevisions(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate);
    
    /**
     * Custom method to find events with complex filtering
     */
    @Query("SELECT e FROM EconomicEvent e WHERE " +
           "(:countries IS NULL OR e.country IN :countries) " +
           "AND (:categories IS NULL OR e.category IN :categories) " +
           "AND (:importance IS NULL OR e.importance = :importance) " +
           "AND (:status IS NULL OR e.status = :status) " +
           "AND e.eventDate BETWEEN :startDate AND :endDate " +
           "ORDER BY e.eventDate ASC, e.importance DESC")
    Page<EconomicEvent> findEventsWithFilters(
        @Param("countries") List<String> countries,
        @Param("categories") List<String> categories,
        @Param("importance") EconomicEvent.EventImportance importance,
        @Param("status") EconomicEvent.EventStatus status,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable);
    
    /**
     * Count events by importance for a date range
     */
    @Query("SELECT e.importance, COUNT(e) FROM EconomicEvent e " +
           "WHERE e.eventDate BETWEEN :startDate AND :endDate " +
           "GROUP BY e.importance")
    List<Object[]> countEventsByImportance(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find duplicate events by title and date (data quality check)
     */
    @Query("SELECT e.title, e.eventDate, COUNT(e) FROM EconomicEvent e " +
           "GROUP BY e.title, e.eventDate HAVING COUNT(e) > 1")
    List<Object[]> findDuplicateEvents();
    
    /**
     * Get recent high-impact events for correlation analysis
     */
    @Query("SELECT e FROM EconomicEvent e WHERE e.status = 'RELEASED' " +
           "AND e.marketImpactScore >= :minImpact " +
           "AND e.eventDate >= :since " +
           "ORDER BY e.eventDate DESC")
    List<EconomicEvent> findRecentHighImpactEvents(
        @Param("minImpact") java.math.BigDecimal minImpact,
        @Param("since") LocalDateTime since);
}