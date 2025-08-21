package com.trademaster.trading.controller;

import com.trademaster.trading.dto.OrderRequest;
import com.trademaster.trading.dto.OrderResponse;
import com.trademaster.trading.model.OrderStatus;
import com.trademaster.trading.security.TradingUserPrincipal;
import com.trademaster.trading.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Trading Controller
 * 
 * REST API endpoints for order placement, management, and tracking.
 * Uses Java 24 Virtual Threads for unlimited scalability with simple blocking I/O.
 * 
 * Performance Targets (Java 24 + Virtual Threads):
 * - Order placement: <50ms response time
 * - Order queries: <25ms response time  
 * - Concurrent support: 10,000+ users (unlimited Virtual Threads)
 * - Memory usage: ~8KB per thread (vs 2MB platform threads)
 * 
 * Security:
 * - JWT authentication required for all endpoints
 * - User-specific data isolation
 * - Rate limiting and input validation
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "Trading API", description = "Order placement and management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class TradingController {
    
    private final OrderService orderService;
    
    /**
     * Place a new order (Blocking I/O with Virtual Threads)
     */
    @PostMapping
    @Operation(summary = "Place new order", 
               description = "Submit a new trading order with validation and risk checks")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Order placed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid order request"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions or risk check failed"),
        @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    public ResponseEntity<OrderResponse> placeOrder(
            @Valid @RequestBody OrderRequest orderRequest,
            @AuthenticationPrincipal TradingUserPrincipal principal) {
        
        Long userId = principal.getUserId();
        
        log.info("Placing order for user {}: {} {} {} @ {}", 
                userId, orderRequest.getSide(), orderRequest.getQuantity(), 
                orderRequest.getSymbol(), orderRequest.getLimitPrice());
        
        try {
            // Simple blocking call - Virtual Thread handles concurrency
            OrderResponse orderResponse = orderService.placeOrder(orderRequest, userId);
            
            log.info("Order placed successfully: {}", orderResponse.getOrderId());
            return ResponseEntity.status(HttpStatus.CREATED).body(orderResponse);
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid order request for user {}: {}", userId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Failed to place order for user {}: {}", userId, e.getMessage());
            throw new RuntimeException("Order placement failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get order by ID (High-performance lookup)
     */
    @GetMapping("/{orderId}")
    @Operation(summary = "Get order details", 
               description = "Retrieve detailed information about a specific order")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order found"),
        @ApiResponse(responseCode = "404", description = "Order not found"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<OrderResponse> getOrder(
            @PathVariable String orderId,
            @AuthenticationPrincipal TradingUserPrincipal principal) {
        
        Long userId = principal.getUserId();
        
        // Blocking call with Virtual Thread - no performance penalty
        OrderResponse orderResponse = orderService.getOrder(orderId, userId);
        
        if (orderResponse == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(orderResponse);
    }
    
    /**
     * Get user's order history (Paginated with Virtual Threads)
     */
    @GetMapping
    @Operation(summary = "Get order history", 
               description = "Retrieve paginated list of user's orders")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Orders retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public List<OrderResponse> getOrderHistory(
            @Parameter(description = "Page number (0-based)") 
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 100)") 
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Filter by order status") 
            @RequestParam(required = false) OrderStatus status,
            @Parameter(description = "Filter by symbol") 
            @RequestParam(required = false) String symbol,
            @AuthenticationPrincipal TradingUserPrincipal principal) {
        
        Long userId = principal.getUserId();
        
        // Validate page size
        if (size > 100) {
            size = 100;
        }
        
        Pageable pageable = PageRequest.of(page, size);
        
        // Simple conditional logic with blocking calls
        if (status != null && symbol != null) {
            return orderService.getOrdersByUserSymbolAndStatus(userId, symbol, status);
        } else if (status != null) {
            return orderService.getOrdersByUserAndStatus(userId, status);
        } else if (symbol != null) {
            return orderService.getOrdersByUserAndSymbol(userId, symbol, pageable);
        } else {
            return orderService.getOrdersByUser(userId, pageable);
        }
    }
    
    /**
     * Get active orders for user (Optimized query)
     */
    @GetMapping("/active")
    @Operation(summary = "Get active orders", 
               description = "Retrieve all active orders (ACKNOWLEDGED, PARTIALLY_FILLED)")
    @ApiResponse(responseCode = "200", description = "Active orders retrieved successfully")
    public List<OrderResponse> getActiveOrders(
            @AuthenticationPrincipal TradingUserPrincipal principal) {
        
        Long userId = principal.getUserId();
        
        // Fast blocking query with Virtual Thread
        return orderService.getActiveOrders(userId);
    }
    
    /**
     * Modify an existing order (Async with Virtual Threads)
     */
    @PutMapping("/{orderId}")
    @Operation(summary = "Modify order", 
               description = "Modify quantity or price of an existing order")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order modified successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid modification request"),
        @ApiResponse(responseCode = "404", description = "Order not found"),
        @ApiResponse(responseCode = "409", description = "Order cannot be modified in current status")
    })
    public CompletableFuture<ResponseEntity<OrderResponse>> modifyOrder(
            @PathVariable String orderId,
            @Valid @RequestBody OrderRequest modificationRequest,
            @AuthenticationPrincipal TradingUserPrincipal principal) {
        
        Long userId = principal.getUserId();
        
        log.info("Modifying order {} for user {}", orderId, userId);
        
        // Async processing with Virtual Threads for high concurrency
        return CompletableFuture.supplyAsync(() -> {
            try {
                OrderResponse orderResponse = orderService.modifyOrder(orderId, modificationRequest, userId);
                
                if (orderResponse == null) {
                    return ResponseEntity.notFound().<OrderResponse>build();
                }
                
                log.info("Order modified successfully: {}", orderId);
                return ResponseEntity.ok(orderResponse);
                
            } catch (IllegalArgumentException | IllegalStateException e) {
                log.warn("Failed to modify order {}: {}", orderId, e.getMessage());
                throw e;
            } catch (Exception e) {
                log.error("Unexpected error modifying order {}: {}", orderId, e.getMessage());
                throw new RuntimeException("Order modification failed: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * Cancel an order (Fast blocking operation)
     */
    @DeleteMapping("/{orderId}")
    @Operation(summary = "Cancel order", 
               description = "Cancel an existing order")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order cancelled successfully"),
        @ApiResponse(responseCode = "404", description = "Order not found"),
        @ApiResponse(responseCode = "409", description = "Order cannot be cancelled in current status")
    })
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable String orderId,
            @AuthenticationPrincipal TradingUserPrincipal principal) {
        
        Long userId = principal.getUserId();
        
        log.info("Cancelling order {} for user {}", orderId, userId);
        
        try {
            OrderResponse orderResponse = orderService.cancelOrder(orderId, userId);
            
            if (orderResponse == null) {
                return ResponseEntity.notFound().build();
            }
            
            log.info("Order cancelled successfully: {}", orderId);
            return ResponseEntity.ok(orderResponse);
            
        } catch (IllegalStateException e) {
            log.warn("Cannot cancel order {}: {}", orderId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Failed to cancel order {}: {}", orderId, e.getMessage());
            throw new RuntimeException("Order cancellation failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get order status (Ultra-fast lightweight endpoint)
     */
    @GetMapping("/{orderId}/status")
    @Operation(summary = "Get order status", 
               description = "Get current status of an order (lightweight)")
    @ApiResponse(responseCode = "200", description = "Order status retrieved")
    public ResponseEntity<String> getOrderStatus(
            @PathVariable String orderId,
            @AuthenticationPrincipal TradingUserPrincipal principal) {
        
        Long userId = principal.getUserId();
        
        // Ultra-fast status lookup
        OrderStatus status = orderService.getOrderStatus(orderId, userId);
        
        if (status == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(status.name());
    }
    
    /**
     * Get order count for user (Dashboard widget optimization)
     */
    @GetMapping("/count")
    @Operation(summary = "Get order counts", 
               description = "Get count of orders by status for user dashboard")
    @ApiResponse(responseCode = "200", description = "Order counts retrieved")
    public ResponseEntity<Map<String, Long>> getOrderCounts(
            @AuthenticationPrincipal TradingUserPrincipal principal) {
        
        Long userId = principal.getUserId();
        
        // Fast aggregation query
        Map<String, Long> counts = orderService.getOrderCounts(userId);
        
        return ResponseEntity.ok(counts);
    }
    
    /**
     * Bulk order operations (Virtual Thread optimization)
     */
    @PostMapping("/bulk")
    @Operation(summary = "Place multiple orders", 
               description = "Submit multiple orders for batch processing")
    @ApiResponse(responseCode = "200", description = "Bulk orders processed")
    public CompletableFuture<ResponseEntity<List<OrderResponse>>> placeBulkOrders(
            @Valid @RequestBody List<OrderRequest> orderRequests,
            @AuthenticationPrincipal TradingUserPrincipal principal) {
        
        Long userId = principal.getUserId();
        
        log.info("Processing {} bulk orders for user {}", orderRequests.size(), userId);
        
        // Parallel processing with Virtual Threads - unlimited scalability
        return CompletableFuture.supplyAsync(() -> {
            List<OrderResponse> responses = orderRequests.parallelStream()
                .map(request -> {
                    try {
                        return orderService.placeOrder(request, userId);
                    } catch (Exception e) {
                        log.error("Failed to process bulk order: {}", e.getMessage());
                        // Return error response or null based on requirements
                        return null;
                    }
                })
                .filter(response -> response != null)
                .toList();
                
            log.info("Processed {}/{} bulk orders successfully for user {}", 
                    responses.size(), orderRequests.size(), userId);
                    
            return ResponseEntity.ok(responses);
        });
    }
}