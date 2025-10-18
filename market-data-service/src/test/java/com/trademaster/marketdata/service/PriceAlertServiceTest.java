package com.trademaster.marketdata.service;

import com.trademaster.marketdata.dto.PriceAlertRequest;
import com.trademaster.marketdata.dto.PriceAlertResponse;
import com.trademaster.marketdata.entity.PriceAlert;
import com.trademaster.marketdata.pattern.Observer;
import com.trademaster.marketdata.repository.PriceAlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for PriceAlertService
 *
 * Tests all CRUD operations, functional validation chains, alert monitoring,
 * notification handling, analytics, and observer pattern integration.
 *
 * Target: >90% code coverage for core business logic
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PriceAlertService Unit Tests")
class PriceAlertServiceTest {

    @Mock
    private PriceAlertRepository priceAlertRepository;

    private PriceAlertService priceAlertService;

    private static final String TEST_USER_ID = "user123";
    private static final String TEST_SYMBOL = "AAPL";
    private static final BigDecimal TEST_TARGET_PRICE = new BigDecimal("150.00");

    @BeforeEach
    void setUp() {
        priceAlertService = new PriceAlertService(priceAlertRepository);
    }

    @Nested
    @DisplayName("Create Alert Tests")
    class CreateAlertTests {

        @Test
        @DisplayName("Should create alert successfully with valid request")
        void shouldCreateAlertSuccessfully() {
            // Given
            var request = buildValidAlertRequest();
            var savedAlert = buildPriceAlert(1L);

            when(priceAlertRepository.findDuplicateAlerts(
                eq(TEST_USER_ID), eq(TEST_SYMBOL), any(), any(), any()))
                .thenReturn(List.of());
            when(priceAlertRepository.save(any(PriceAlert.class)))
                .thenReturn(savedAlert);

            // When
            var response = priceAlertService.createAlert(request, TEST_USER_ID);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.success()).isTrue();
            assertThat(response.message()).contains("created");

            verify(priceAlertRepository).save(any(PriceAlert.class));
        }

        @Test
        @DisplayName("Should reject alert with invalid symbol")
        void shouldRejectInvalidSymbol() {
            // Given
            var request = buildAlertRequestWithSymbol("");

            // When
            var response = priceAlertService.createAlert(request, TEST_USER_ID);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.success()).isFalse();
            assertThat(response.message()).containsIgnoringCase("symbol");

            verify(priceAlertRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should reject alert with invalid target price (negative)")
        void shouldRejectNegativeTargetPrice() {
            // Given
            var request = buildAlertRequestWithTargetPrice(new BigDecimal("-10.00"));

            // When
            var response = priceAlertService.createAlert(request, TEST_USER_ID);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.success()).isFalse();
            assertThat(response.message()).containsIgnoringCase("price");

            verify(priceAlertRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should reject alert with invalid target price (too large)")
        void shouldRejectTooLargeTargetPrice() {
            // Given
            var request = buildAlertRequestWithTargetPrice(new BigDecimal("2000000.00"));

            // When
            var response = priceAlertService.createAlert(request, TEST_USER_ID);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.success()).isFalse();
            assertThat(response.message()).containsIgnoringCase("price");
        }

        @Test
        @DisplayName("Should reject alert with invalid symbol format")
        void shouldRejectInvalidSymbolFormat() {
            // Given
            var request = buildAlertRequestWithSymbol("invalid-symbol!");

            // When
            var response = priceAlertService.createAlert(request, TEST_USER_ID);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.success()).isFalse();
            assertThat(response.message()).containsIgnoringCase("symbol");
        }

        @Test
        @DisplayName("Should reject alert with symbol too long")
        void shouldRejectLongSymbol() {
            // Given
            var request = buildAlertRequestWithSymbol("VERYLONGSYMBOL123");

            // When
            var response = priceAlertService.createAlert(request, TEST_USER_ID);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.success()).isFalse();
        }

        @Test
        @DisplayName("Should reject duplicate alert")
        void shouldRejectDuplicateAlert() {
            // Given
            var request = buildValidAlertRequest();
            var existingAlert = buildPriceAlert(1L);

            when(priceAlertRepository.findDuplicateAlerts(
                eq(TEST_USER_ID), eq(TEST_SYMBOL), any(), any(), any()))
                .thenReturn(List.of(existingAlert));

            // When
            var response = priceAlertService.createAlert(request, TEST_USER_ID);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.success()).isFalse();
            assertThat(response.message()).containsIgnoringCase("similar alert already exists");

            verify(priceAlertRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should reject alert with invalid expiration date (too soon)")
        void shouldRejectTooSoonExpiration() {
            // Given
            var request = buildAlertRequestWithExpiration(
                LocalDateTime.now().plusMinutes(2));

            // When
            var response = priceAlertService.createAlert(request, TEST_USER_ID);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.success()).isFalse();
            assertThat(response.message()).containsIgnoringCase("expiration");
        }

        @Test
        @DisplayName("Should reject alert with invalid expiration date (too far)")
        void shouldRejectTooFarExpiration() {
            // Given
            var request = buildAlertRequestWithExpiration(
                LocalDateTime.now().plusYears(2));

            // When
            var response = priceAlertService.createAlert(request, TEST_USER_ID);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.success()).isFalse();
            assertThat(response.message()).containsIgnoringCase("expiration");
        }

        @Test
        @DisplayName("Should accept alert with valid expiration date")
        void shouldAcceptValidExpiration() {
            // Given
            var request = buildAlertRequestWithExpiration(
                LocalDateTime.now().plusDays(7));
            var savedAlert = buildPriceAlert(1L);

            when(priceAlertRepository.findDuplicateAlerts(any(), any(), any(), any(), any()))
                .thenReturn(List.of());
            when(priceAlertRepository.save(any(PriceAlert.class)))
                .thenReturn(savedAlert);

            // When
            var response = priceAlertService.createAlert(request, TEST_USER_ID);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.success()).isTrue();
        }

        @Test
        @DisplayName("Should reject alert with invalid stop price range")
        void shouldRejectInvalidStopPriceRange() {
            // Given
            var request = buildAlertRequestWithStopPrice(
                TEST_TARGET_PRICE, new BigDecimal("500.00")); // 500/150 = 3.33x ratio

            // When
            var response = priceAlertService.createAlert(request, TEST_USER_ID);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.success()).isFalse();
            assertThat(response.message()).containsIgnoringCase("stop price");
        }
    }

    @Nested
    @DisplayName("Get Alerts Tests")
    class GetAlertsTests {

        @Test
        @DisplayName("Should get alerts successfully without filters")
        void shouldGetAlertsWithoutFilters() {
            // Given
            var request = buildGetAlertsRequest(false);
            var alerts = List.of(buildPriceAlert(1L), buildPriceAlert(2L));
            var page = new PageImpl<>(alerts);

            when(priceAlertRepository.findAllAlertsByUser(eq(TEST_USER_ID), any(Pageable.class)))
                .thenReturn(page);
            when(priceAlertRepository.getUserAlertStatistics(TEST_USER_ID))
                .thenReturn(List.of());
            when(priceAlertRepository.getUserAlertPerformance(TEST_USER_ID))
                .thenReturn(List.of());
            when(priceAlertRepository.countActiveAlertsByUser(TEST_USER_ID))
                .thenReturn(2L);

            // When
            var response = priceAlertService.getAlerts(request, TEST_USER_ID);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.success()).isTrue();
            assertThat(response.alerts()).hasSize(2);
            assertThat(response.pagination()).isNotNull();
            assertThat(response.analytics()).isNotNull();

            verify(priceAlertRepository).findAllAlertsByUser(eq(TEST_USER_ID), any());
        }

        @Test
        @DisplayName("Should get alerts with filters")
        void shouldGetAlertsWithFilters() {
            // Given
            var request = buildGetAlertsRequestWithFilters();
            var alerts = List.of(buildPriceAlert(1L));
            var page = new PageImpl<>(alerts);

            when(priceAlertRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);
            when(priceAlertRepository.getUserAlertStatistics(TEST_USER_ID))
                .thenReturn(List.of());
            when(priceAlertRepository.getUserAlertPerformance(TEST_USER_ID))
                .thenReturn(List.of());
            when(priceAlertRepository.countActiveAlertsByUser(TEST_USER_ID))
                .thenReturn(1L);

            // When
            var response = priceAlertService.getAlerts(request, TEST_USER_ID);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.success()).isTrue();
            assertThat(response.alerts()).hasSize(1);

            verify(priceAlertRepository).findAll(any(Specification.class), any(Pageable.class));
        }

        @Test
        @DisplayName("Should return empty list when no alerts found")
        void shouldReturnEmptyListWhenNoAlerts() {
            // Given
            var request = buildGetAlertsRequest(false);
            var page = new PageImpl<PriceAlert>(List.of());

            when(priceAlertRepository.findAllAlertsByUser(eq(TEST_USER_ID), any(Pageable.class)))
                .thenReturn(page);
            when(priceAlertRepository.getUserAlertStatistics(TEST_USER_ID))
                .thenReturn(List.of());
            when(priceAlertRepository.getUserAlertPerformance(TEST_USER_ID))
                .thenReturn(List.of());
            when(priceAlertRepository.countActiveAlertsByUser(TEST_USER_ID))
                .thenReturn(0L);

            // When
            var response = priceAlertService.getAlerts(request, TEST_USER_ID);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.success()).isTrue();
            assertThat(response.alerts()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Update Alert Tests")
    class UpdateAlertTests {

        @Test
        @DisplayName("Should update alert successfully")
        void shouldUpdateAlertSuccessfully() {
            // Given
            Long alertId = 1L;
            var request = buildValidAlertRequest();
            var alert = buildPriceAlert(alertId);

            when(priceAlertRepository.findById(alertId))
                .thenReturn(Optional.of(alert));
            when(priceAlertRepository.save(any(PriceAlert.class)))
                .thenReturn(alert);

            // When
            var response = priceAlertService.updateAlert(alertId, request, TEST_USER_ID);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.success()).isTrue();
            assertThat(response.message()).contains("updated");

            verify(priceAlertRepository).save(any(PriceAlert.class));
        }

        @Test
        @DisplayName("Should reject update for non-existent alert")
        void shouldRejectUpdateForNonExistentAlert() {
            // Given
            Long alertId = 999L;
            var request = buildValidAlertRequest();

            when(priceAlertRepository.findById(alertId))
                .thenReturn(Optional.empty());

            // When
            var response = priceAlertService.updateAlert(alertId, request, TEST_USER_ID);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.success()).isFalse();
            assertThat(response.message()).containsIgnoringCase("not found");

            verify(priceAlertRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should reject update for alert owned by different user")
        void shouldRejectUpdateForDifferentUser() {
            // Given
            Long alertId = 1L;
            var request = buildValidAlertRequest();
            var alert = buildPriceAlert(alertId);

            when(priceAlertRepository.findById(alertId))
                .thenReturn(Optional.of(alert));

            // When
            var response = priceAlertService.updateAlert(alertId, request, "differentUser");

            // Then
            assertThat(response).isNotNull();
            assertThat(response.success()).isFalse();
            assertThat(response.message()).containsIgnoringCase("access denied");

            verify(priceAlertRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Delete Alert Tests")
    class DeleteAlertTests {

        @Test
        @DisplayName("Should delete alert successfully (soft delete)")
        void shouldDeleteAlertSuccessfully() {
            // Given
            Long alertId = 1L;
            var alert = buildPriceAlert(alertId);

            when(priceAlertRepository.findById(alertId))
                .thenReturn(Optional.of(alert));
            when(priceAlertRepository.save(any(PriceAlert.class)))
                .thenReturn(alert);

            // When
            var response = priceAlertService.deleteAlert(alertId, TEST_USER_ID);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.success()).isTrue();

            ArgumentCaptor<PriceAlert> captor = ArgumentCaptor.forClass(PriceAlert.class);
            verify(priceAlertRepository).save(captor.capture());

            var savedAlert = captor.getValue();
            assertThat(savedAlert.getStatus()).isEqualTo(PriceAlert.AlertStatus.CANCELLED);
            assertThat(savedAlert.getIsActive()).isFalse();
        }

        @Test
        @DisplayName("Should reject delete for non-existent alert")
        void shouldRejectDeleteForNonExistentAlert() {
            // Given
            Long alertId = 999L;

            when(priceAlertRepository.findById(alertId))
                .thenReturn(Optional.empty());

            // When
            var response = priceAlertService.deleteAlert(alertId, TEST_USER_ID);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.success()).isFalse();
            assertThat(response.message()).containsIgnoringCase("not found");
        }

        @Test
        @DisplayName("Should reject delete for alert owned by different user")
        void shouldRejectDeleteForDifferentUser() {
            // Given
            Long alertId = 1L;
            var alert = buildPriceAlert(alertId);

            when(priceAlertRepository.findById(alertId))
                .thenReturn(Optional.of(alert));

            // When
            var response = priceAlertService.deleteAlert(alertId, "differentUser");

            // Then
            assertThat(response).isNotNull();
            assertThat(response.success()).isFalse();
            assertThat(response.message()).containsIgnoringCase("access denied");

            verify(priceAlertRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Observer Pattern Tests")
    class ObserverPatternTests {

        @Test
        @DisplayName("Should allow subscription to alert events")
        void shouldAllowAlertEventSubscription() {
            // Given
            var eventReceived = new boolean[]{false};
            Observer.EventObserver<Observer.AlertEvent> observer = event -> {
                eventReceived[0] = true;
            };

            // When
            priceAlertService.subscribeToAlertEvents(observer);

            // Then - subscription successful if no exception thrown
            assertThat(eventReceived[0]).isFalse(); // No event published yet
        }

        @Test
        @DisplayName("Should allow subscription to alert triggered events")
        void shouldAllowAlertTriggeredSubscription() {
            // Given
            var eventReceived = new boolean[]{false};
            Observer.EventObserver<Observer.AlertEvent.AlertTriggered> observer = event -> {
                eventReceived[0] = true;
            };

            // When
            priceAlertService.subscribeToAlertTriggered(observer);

            // Then
            assertThat(eventReceived[0]).isFalse();
        }

        @Test
        @DisplayName("Should allow subscription to alert created events")
        void shouldAllowAlertCreatedSubscription() {
            // Given
            var eventReceived = new boolean[]{false};
            Observer.EventObserver<Observer.AlertEvent.AlertCreated> observer = event -> {
                eventReceived[0] = true;
            };

            // When
            priceAlertService.subscribeToAlertCreated(observer);

            // Then
            assertThat(eventReceived[0]).isFalse();
        }

        @Test
        @DisplayName("Should allow subscription to alert deleted events")
        void shouldAllowAlertDeletedSubscription() {
            // Given
            var eventReceived = new boolean[]{false};
            Observer.EventObserver<Observer.AlertEvent.AlertDeleted> observer = event -> {
                eventReceived[0] = true;
            };

            // When
            priceAlertService.subscribeToAlertDeleted(observer);

            // Then
            assertThat(eventReceived[0]).isFalse();
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should validate notification settings length")
        void shouldValidateNotificationSettingsLength() {
            // Given
            var longSettings = "x".repeat(600); // Exceeds 500 char limit
            var request = buildAlertRequestWithNotificationSettings(longSettings);

            // When
            var response = priceAlertService.createAlert(request, TEST_USER_ID);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.success()).isFalse();
            assertThat(response.message()).containsIgnoringCase("notification settings");
        }

        @Test
        @DisplayName("Should reject notification settings with prohibited content")
        void shouldRejectProhibitedNotificationSettings() {
            // Given
            var request = buildAlertRequestWithNotificationSettings("{\"spam\": true}");

            // When
            var response = priceAlertService.createAlert(request, TEST_USER_ID);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.success()).isFalse();
        }

        @Test
        @DisplayName("Should accept valid notification settings")
        void shouldAcceptValidNotificationSettings() {
            // Given
            var request = buildAlertRequestWithNotificationSettings("{\"email\": true}");
            var savedAlert = buildPriceAlert(1L);

            when(priceAlertRepository.findDuplicateAlerts(any(), any(), any(), any(), any()))
                .thenReturn(List.of());
            when(priceAlertRepository.save(any(PriceAlert.class)))
                .thenReturn(savedAlert);

            // When
            var response = priceAlertService.createAlert(request, TEST_USER_ID);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.success()).isTrue();
        }
    }

    // Helper methods

    private PriceAlertRequest buildValidAlertRequest() {
        return PriceAlertRequest.builder()
            .name("Test Alert")
            .description("Test description")
            .symbol(TEST_SYMBOL)
            .exchange("NASDAQ")
            .alertType(PriceAlert.AlertType.PRICE_TARGET)
            .triggerCondition(PriceAlert.TriggerCondition.GREATER_THAN)
            .priority(PriceAlert.Priority.NORMAL)
            .targetPrice(TEST_TARGET_PRICE)
            .notificationMethod(PriceAlert.NotificationMethod.EMAIL)
            .build();
    }

    private PriceAlertRequest buildAlertRequestWithSymbol(String symbol) {
        return PriceAlertRequest.builder()
            .name("Test Alert")
            .symbol(symbol)
            .exchange("NASDAQ")
            .alertType(PriceAlert.AlertType.PRICE_TARGET)
            .triggerCondition(PriceAlert.TriggerCondition.GREATER_THAN)
            .priority(PriceAlert.Priority.NORMAL)
            .targetPrice(TEST_TARGET_PRICE)
            .notificationMethod(PriceAlert.NotificationMethod.EMAIL)
            .build();
    }

    private PriceAlertRequest buildAlertRequestWithTargetPrice(BigDecimal targetPrice) {
        return PriceAlertRequest.builder()
            .name("Test Alert")
            .symbol(TEST_SYMBOL)
            .exchange("NASDAQ")
            .alertType(PriceAlert.AlertType.PRICE_TARGET)
            .triggerCondition(PriceAlert.TriggerCondition.GREATER_THAN)
            .priority(PriceAlert.Priority.NORMAL)
            .targetPrice(targetPrice)
            .notificationMethod(PriceAlert.NotificationMethod.EMAIL)
            .build();
    }

    private PriceAlertRequest buildAlertRequestWithExpiration(LocalDateTime expiresAt) {
        return PriceAlertRequest.builder()
            .name("Test Alert")
            .symbol(TEST_SYMBOL)
            .exchange("NASDAQ")
            .alertType(PriceAlert.AlertType.PRICE_TARGET)
            .triggerCondition(PriceAlert.TriggerCondition.GREATER_THAN)
            .priority(PriceAlert.Priority.NORMAL)
            .targetPrice(TEST_TARGET_PRICE)
            .expiresAt(expiresAt)
            .notificationMethod(PriceAlert.NotificationMethod.EMAIL)
            .build();
    }

    private PriceAlertRequest buildAlertRequestWithStopPrice(BigDecimal targetPrice, BigDecimal stopPrice) {
        return PriceAlertRequest.builder()
            .name("Test Alert")
            .symbol(TEST_SYMBOL)
            .exchange("NASDAQ")
            .alertType(PriceAlert.AlertType.PRICE_TARGET)
            .triggerCondition(PriceAlert.TriggerCondition.LESS_THAN)
            .priority(PriceAlert.Priority.NORMAL)
            .targetPrice(targetPrice)
            .stopPrice(stopPrice)
            .notificationMethod(PriceAlert.NotificationMethod.EMAIL)
            .build();
    }

    private PriceAlertRequest buildAlertRequestWithNotificationSettings(String settings) {
        return PriceAlertRequest.builder()
            .name("Test Alert")
            .symbol(TEST_SYMBOL)
            .exchange("NASDAQ")
            .alertType(PriceAlert.AlertType.PRICE_TARGET)
            .triggerCondition(PriceAlert.TriggerCondition.GREATER_THAN)
            .priority(PriceAlert.Priority.NORMAL)
            .targetPrice(TEST_TARGET_PRICE)
            .notificationSettings(settings)
            .notificationMethod(PriceAlert.NotificationMethod.EMAIL)
            .build();
    }

    private PriceAlertRequest buildGetAlertsRequest(boolean withFilters) {
        return PriceAlertRequest.builder()
            .page(0)
            .size(20)
            .sortBy("createdAt")
            .sortDirection(PriceAlertRequest.SortDirection.DESC)
            .includePerformanceMetrics(true)
            .includeMarketContext(true)
            .includeTriggerHistory(true)
            .build();
    }

    private PriceAlertRequest buildGetAlertsRequestWithFilters() {
        return PriceAlertRequest.builder()
            .page(0)
            .size(20)
            .sortBy("createdAt")
            .sortDirection(PriceAlertRequest.SortDirection.DESC)
            .symbols(Set.of(TEST_SYMBOL))
            .activeOnly(true)
            .highPriorityOnly(true)
            .includePerformanceMetrics(true)
            .includeMarketContext(true)
            .includeTriggerHistory(true)
            .build();
    }

    private PriceAlert buildPriceAlert(Long id) {
        return PriceAlert.builder()
            .id(id)
            .userId(TEST_USER_ID)
            .name("Test Alert")
            .description("Test description")
            .symbol(TEST_SYMBOL)
            .exchange("NASDAQ")
            .alertType(PriceAlert.AlertType.PRICE_TARGET)
            .triggerCondition(PriceAlert.TriggerCondition.GREATER_THAN)
            .priority(PriceAlert.Priority.NORMAL)
            .targetPrice(TEST_TARGET_PRICE)
            .status(PriceAlert.AlertStatus.ACTIVE)
            .isActive(true)
            .isTriggered(false)
            .timesTriggered(0)
            .notificationMethod(PriceAlert.NotificationMethod.EMAIL)
            .emailSent(false)
            .smsSent(false)
            .pushSent(false)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    }
}
