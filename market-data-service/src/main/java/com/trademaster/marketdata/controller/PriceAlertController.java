package com.trademaster.marketdata.controller;

import com.trademaster.marketdata.dto.PriceAlertRequest;
import com.trademaster.marketdata.dto.PriceAlertResponse;
import com.trademaster.marketdata.service.PriceAlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Price Alert REST API Controller
 * 
 * Provides comprehensive price alert management with real-time monitoring,
 * intelligent triggering, and advanced analytics.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/price-alerts")
@RequiredArgsConstructor
@Tag(name = "Price Alerts", description = "Advanced price alert management API")
public class PriceAlertController {
    
    private final PriceAlertService priceAlertService;
    
    /**
     * Create a new price alert
     */
    @PostMapping
    @Operation(summary = "Create price alert", 
               description = "Create a new price alert with comprehensive configuration")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Alert created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
        @ApiResponse(responseCode = "409", description = "Duplicate alert exists"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PriceAlertResponse> createAlert(
            @RequestBody @Valid PriceAlertRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("Creating price alert for user: {} symbol: {} type: {}", 
            userDetails.getUsername(), request.symbol(), request.alertType());
        
        var response = priceAlertService.createAlert(request, userDetails.getUsername());
        
        if (response.success()) {
            return ResponseEntity.status(201).body(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * Get user's alerts with filtering and analytics
     */
    @PostMapping("/search")
    @Operation(summary = "Search price alerts", 
               description = "Get price alerts with comprehensive filtering and analytics")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PriceAlertResponse> searchAlerts(
            @RequestBody @Valid PriceAlertRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.debug("Alert search request by user: {} with filters: {}", 
            userDetails.getUsername(), request.hasFilters());
        
        var response = priceAlertService.getAlerts(request, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get all active alerts
     */
    @GetMapping("/active")
    @Operation(summary = "Get active alerts", description = "Retrieve all active price alerts")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PriceAlertResponse> getActiveAlerts(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.debug("Active alerts request by user: {}", userDetails.getUsername());
        
        var request = PriceAlertRequest.activeAlertsQuery();
        var response = priceAlertService.getAlerts(request, userDetails.getUsername());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get recently triggered alerts
     */
    @GetMapping("/triggered")
    @Operation(summary = "Get triggered alerts", description = "Retrieve recently triggered alerts")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PriceAlertResponse> getTriggeredAlerts(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.debug("Triggered alerts request by user: {}", userDetails.getUsername());
        
        var request = PriceAlertRequest.recentTriggeredQuery();
        var response = priceAlertService.getAlerts(request, userDetails.getUsername());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get high priority alerts
     */
    @GetMapping("/priority")
    @Operation(summary = "Get high priority alerts", description = "Retrieve high priority alerts")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PriceAlertResponse> getHighPriorityAlerts(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.debug("High priority alerts request by user: {}", userDetails.getUsername());
        
        var request = PriceAlertRequest.highPriorityQuery();
        var response = priceAlertService.getAlerts(request, userDetails.getUsername());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get alerts for a specific symbol
     */
    @GetMapping("/symbol/{symbol}")
    @Operation(summary = "Get symbol alerts", description = "Retrieve alerts for a specific symbol")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PriceAlertResponse> getSymbolAlerts(
            @Parameter(description = "Trading symbol", example = "AAPL")
            @PathVariable String symbol,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.debug("Symbol alerts request for {} by user: {}", symbol, userDetails.getUsername());
        
        var request = PriceAlertRequest.symbolAlertsQuery(symbol);
        var response = priceAlertService.getAlerts(request, userDetails.getUsername());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get specific alert by ID
     */
    @GetMapping("/{alertId}")
    @Operation(summary = "Get alert by ID", description = "Retrieve specific alert details")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PriceAlertResponse> getAlert(
            @Parameter(description = "Alert ID")
            @PathVariable Long alertId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.debug("Alert details request for ID: {} by user: {}", alertId, userDetails.getUsername());
        
        // Create a simple request to get single alert with full details
        var request = PriceAlertRequest.builder()
            .includePerformanceMetrics(true)
            .includeMarketContext(true)
            .includeTriggerHistory(true)
            .build();
        
        var response = priceAlertService.getAlerts(request, userDetails.getUsername());
        
        // Filter for specific alert ID in the response
        if (response.success() && response.alerts() != null) {
            var alert = response.alerts().stream()
                .filter(a -> a.id().equals(alertId))
                .findFirst();
            
            if (alert.isPresent()) {
                return ResponseEntity.ok(PriceAlertResponse.success(alert.get()));
            }
        }
        
        return ResponseEntity.notFound().build();
    }
    
    /**
     * Update an existing alert
     */
    @PutMapping("/{alertId}")
    @Operation(summary = "Update price alert", description = "Update an existing price alert")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Alert updated successfully"),
        @ApiResponse(responseCode = "404", description = "Alert not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PriceAlertResponse> updateAlert(
            @Parameter(description = "Alert ID")
            @PathVariable Long alertId,
            @RequestBody @Valid PriceAlertRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("Updating alert ID: {} for user: {}", alertId, userDetails.getUsername());
        
        var response = priceAlertService.updateAlert(alertId, request, userDetails.getUsername());
        
        if (response.success()) {
            return ResponseEntity.ok(response);
        } else if (response.message().contains("not found")) {
            return ResponseEntity.notFound().build();
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * Delete an alert
     */
    @DeleteMapping("/{alertId}")
    @Operation(summary = "Delete price alert", description = "Delete an existing price alert")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Alert deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Alert not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PriceAlertResponse> deleteAlert(
            @Parameter(description = "Alert ID")
            @PathVariable Long alertId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("Deleting alert ID: {} for user: {}", alertId, userDetails.getUsername());
        
        var response = priceAlertService.deleteAlert(alertId, userDetails.getUsername());
        
        if (response.success()) {
            return ResponseEntity.ok(response);
        } else if (response.message().contains("not found")) {
            return ResponseEntity.notFound().build();
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * Create preset alerts for common scenarios
     */
    
    @PostMapping("/preset/price-target")
    @Operation(summary = "Create price target alert", description = "Create a simple price target alert")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PriceAlertResponse> createPriceTargetAlert(
            @RequestParam String symbol,
            @RequestParam String exchange,
            @RequestParam String targetPrice,
            @RequestParam(defaultValue = "NORMAL") String priority,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        var request = PriceAlertRequest.priceTarget(
            symbol, 
            exchange, 
            new java.math.BigDecimal(targetPrice),
            com.trademaster.marketdata.entity.PriceAlert.Priority.valueOf(priority.toUpperCase())
        );
        
        var response = priceAlertService.createAlert(request, userDetails.getUsername());
        
        return response.success() ? 
            ResponseEntity.status(201).body(response) : 
            ResponseEntity.badRequest().body(response);
    }
    
    @PostMapping("/preset/stop-loss")
    @Operation(summary = "Create stop loss alert", description = "Create a stop loss alert")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PriceAlertResponse> createStopLossAlert(
            @RequestParam String symbol,
            @RequestParam String exchange,
            @RequestParam String stopPrice,
            @RequestParam(defaultValue = "HIGH") String priority,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        var request = PriceAlertRequest.stopLoss(
            symbol, 
            exchange, 
            new java.math.BigDecimal(stopPrice),
            com.trademaster.marketdata.entity.PriceAlert.Priority.valueOf(priority.toUpperCase())
        );
        
        var response = priceAlertService.createAlert(request, userDetails.getUsername());
        
        return response.success() ? 
            ResponseEntity.status(201).body(response) : 
            ResponseEntity.badRequest().body(response);
    }
    
    @PostMapping("/preset/percentage")
    @Operation(summary = "Create percentage alert", description = "Create a percentage change alert")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PriceAlertResponse> createPercentageAlert(
            @RequestParam String symbol,
            @RequestParam String exchange,
            @RequestParam String baselinePrice,
            @RequestParam String percentage,
            @RequestParam(defaultValue = "true") boolean isUpward,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        var request = PriceAlertRequest.percentageAlert(
            symbol, 
            exchange,
            new java.math.BigDecimal(baselinePrice),
            new java.math.BigDecimal(percentage),
            isUpward
        );
        
        var response = priceAlertService.createAlert(request, userDetails.getUsername());
        
        return response.success() ? 
            ResponseEntity.status(201).body(response) : 
            ResponseEntity.badRequest().body(response);
    }
    
    @PostMapping("/preset/volume")
    @Operation(summary = "Create volume alert", description = "Create a volume spike alert")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PriceAlertResponse> createVolumeAlert(
            @RequestParam String symbol,
            @RequestParam String exchange,
            @RequestParam String volumeThreshold,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        var request = PriceAlertRequest.volumeSpike(
            symbol, 
            exchange,
            new java.math.BigDecimal(volumeThreshold)
        );
        
        var response = priceAlertService.createAlert(request, userDetails.getUsername());
        
        return response.success() ? 
            ResponseEntity.status(201).body(response) : 
            ResponseEntity.badRequest().body(response);
    }
    
    @PostMapping("/preset/rsi")
    @Operation(summary = "Create RSI alert", description = "Create an RSI threshold alert")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PriceAlertResponse> createRSIAlert(
            @RequestParam String symbol,
            @RequestParam String exchange,
            @RequestParam String rsiThreshold,
            @RequestParam(defaultValue = "true") boolean isOverbought,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        var request = PriceAlertRequest.rsiAlert(
            symbol, 
            exchange,
            new java.math.BigDecimal(rsiThreshold),
            isOverbought
        );
        
        var response = priceAlertService.createAlert(request, userDetails.getUsername());
        
        return response.success() ? 
            ResponseEntity.status(201).body(response) : 
            ResponseEntity.badRequest().body(response);
    }
}