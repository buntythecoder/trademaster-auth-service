import { apiClient } from './apiClient';

// Types for Trading Service
export interface OrderRequest {
  symbol: string;
  side: 'BUY' | 'SELL';
  orderType: 'MARKET' | 'LIMIT' | 'STOP' | 'STOP_LIMIT';
  quantity: number;
  price?: number;
  stopPrice?: number;
  timeInForce?: 'DAY' | 'GTC' | 'IOC' | 'FOK';
  notes?: string;
}

export interface OrderResponse {
  orderId: string;
  symbol: string;
  side: 'BUY' | 'SELL';
  orderType: 'MARKET' | 'LIMIT' | 'STOP' | 'STOP_LIMIT';
  status: OrderStatus;
  quantity: number;
  filledQuantity: number;
  remainingQuantity: number;
  price?: number;
  stopPrice?: number;
  averagePrice?: number;
  fees?: number;
  createdAt: Date;
  updatedAt: Date;
  timeInForce: string;
  broker?: string;
  exchange?: string;
  clientOrderId?: string;
  errorMessage?: string;
}

export type OrderStatus = 
  | 'PENDING'
  | 'ACKNOWLEDGED' 
  | 'PARTIALLY_FILLED'
  | 'FILLED'
  | 'CANCELLED'
  | 'REJECTED'
  | 'EXPIRED';

export interface OrderModificationRequest {
  orderId: string;
  newQuantity?: number;
  newPrice?: number;
  newStopPrice?: number;
  modificationType: 'QUANTITY' | 'PRICE' | 'STOP_PRICE' | 'CANCEL';
  reason?: string;
}

export interface OrderHistoryFilters {
  startDate?: Date;
  endDate?: Date;
  symbol?: string;
  status?: OrderStatus;
  orderType?: string;
  side?: 'BUY' | 'SELL';
  minAmount?: number;
  maxAmount?: number;
  limit?: number;
  offset?: number;
}

export interface BulkOrderRequest {
  orders: OrderRequest[];
  executionStrategy: 'SIMULTANEOUS' | 'SEQUENTIAL' | 'SCHEDULED';
  scheduledTime?: Date;
  positionSizing: 'EQUAL_WEIGHT' | 'PERCENTAGE' | 'FIXED_AMOUNT';
  totalAmount?: number;
  delayBetweenOrders?: number;
  stopOnFailure?: boolean;
  riskChecks?: boolean;
}

export interface BulkExecutionStatus {
  requestId: string;
  totalOrders: number;
  successfulOrders: number;
  failedOrders: number;
  pendingOrders: number;
  executionProgress: number;
  orders: BulkOrderStatus[];
  startTime: Date;
  endTime?: Date;
  totalValue?: number;
  totalFees?: number;
}

export interface BulkOrderStatus {
  orderId: string;
  symbol: string;
  status: 'PENDING' | 'SUBMITTED' | 'FILLED' | 'REJECTED' | 'CANCELLED';
  errorMessage?: string;
  submittedAt?: Date;
  filledAt?: Date;
  executionPrice?: number;
  fees?: number;
}

export interface AdvancedOrderRequest {
  orderType: 'BRACKET' | 'OCO' | 'TRAILING_STOP' | 'ICEBERG' | 'TWAP';
  symbol: string;
  side: 'BUY' | 'SELL';
  quantity: number;
  
  // Bracket Order
  entryPrice?: number;
  takeProfitPrice?: number;
  stopLossPrice?: number;
  
  // OCO Order
  ocoOrders?: [OrderRequest, OrderRequest];
  
  // Trailing Stop
  trailAmount?: number;
  trailPercent?: number;
  
  // Iceberg Order
  displayQuantity?: number;
  
  // TWAP Order
  twapDuration?: number; // minutes
  twapSlices?: number;
  
  // Common fields
  timeInForce?: 'DAY' | 'GTC' | 'IOC' | 'FOK';
  notes?: string;
}

export interface DateRange {
  start: Date;
  end: Date;
}

export interface OrderAnalyticsFilters {
  symbol?: string;
  orderType?: string;
  status?: string;
  minAmount?: number;
  maxAmount?: number;
  broker?: string;
}

export interface OrderAnalytics {
  summary: {
    totalOrders: number;
    fillRate: number;
    averageExecutionTime: number;
    totalVolume: number;
    totalFees: number;
    averageSlippage: number;
    successfulOrders: number;
    failedOrders: number;
  };
  performance: {
    slippageAnalysis: SlippageMetric[];
    executionQuality: ExecutionQualityMetric[];
    brokerComparison: BrokerPerformance[];
  };
  patterns: {
    orderSizeDistribution: OrderSizeDistribution[];
    tradingTimeAnalysis: TradingTimeAnalysis[];
    symbolFrequency: SymbolFrequency[];
  };
}

export interface SlippageMetric {
  symbol: string;
  averageSlippage: number;
  slippageStandardDeviation: number;
  slippageCount: number;
  date: string;
}

export interface ExecutionQualityMetric {
  broker: string;
  averageExecutionTime: number;
  fillRate: number;
  averageSlippage: number;
  totalOrders: number;
  date: string;
}

export interface BrokerPerformance {
  broker: string;
  totalOrders: number;
  fillRate: number;
  averageExecutionTime: number;
  averageSlippage: number;
  totalFees: number;
  avgFeePerTrade: number;
  uptime: number;
  errorRate: number;
}

export interface OrderSizeDistribution {
  range: string;
  count: number;
  percentage: number;
  totalValue: number;
}

export interface TradingTimeAnalysis {
  hour: number;
  orderCount: number;
  successRate: number;
  averageSlippage: number;
  totalVolume: number;
}

export interface SymbolFrequency {
  symbol: string;
  count: number;
  percentage: number;
  successRate: number;
  totalVolume: number;
}

export interface OrderStatusUpdate {
  orderId: string;
  status: OrderStatus;
  filledQuantity: number;
  averagePrice: number;
  remainingQuantity: number;
  lastUpdateTime: Date;
  executionReports: ExecutionReport[];
  fees?: number;
  slippage?: number;
  queuePosition?: number;
}

export interface ExecutionReport {
  id: string;
  quantity: number;
  price: number;
  timestamp: Date;
  exchange: string;
  fees: number;
  tradeId: string;
  counterparty?: string;
}

const WS_BASE_URL = import.meta.env.VITE_WS_URL || 'ws://localhost:8080/ws';

/**
 * Enhanced Trading Service with comprehensive order management capabilities
 * 
 * Features:
 * - Real-time order tracking and updates
 * - Advanced order types (Bracket, OCO, Trailing Stop, Iceberg, TWAP)
 * - Bulk order management with execution strategies
 * - Comprehensive order analytics and reporting
 * - Order modification and cancellation
 * - WebSocket connections for live updates
 */
export class TradingService {
  private wsConnections: Map<string, WebSocket> = new Map();

  constructor() {
    this.setupErrorHandlers();
  }

  private setupErrorHandlers() {
    // Global error handler for WebSocket connections
    window.addEventListener('beforeunload', () => {
      this.closeAllWebSocketConnections();
    });
  }

  // ============ Basic Order Operations ============

  /**
   * Place a new order
   */
  async placeOrder(order: OrderRequest): Promise<OrderResponse> {
    try {
      const response = await apiClient.post('/api/v1/orders', {
        ...order,
        timestamp: new Date().toISOString()
      });
      
      return {
        ...response.data,
        createdAt: new Date(response.data.createdAt),
        updatedAt: new Date(response.data.updatedAt)
      };
    } catch (error: any) {
      throw new Error(`Order placement failed: ${error.response?.data?.message || error.message}`);
    }
  }

  /**
   * Get detailed order information
   */
  async getOrderDetails(orderId: string): Promise<OrderResponse> {
    try {
      const response = await apiClient.get(`/api/v1/orders/${orderId}`);
      return {
        ...response.data,
        createdAt: new Date(response.data.createdAt),
        updatedAt: new Date(response.data.updatedAt)
      };
    } catch (error: any) {
      throw new Error(`Failed to fetch order details: ${error.response?.data?.message || error.message}`);
    }
  }

  /**
   * Get order history with filtering
   */
  async getOrderHistory(filters: OrderHistoryFilters = {}): Promise<OrderResponse[]> {
    try {
      const params = {
        ...filters,
        startDate: filters.startDate?.toISOString(),
        endDate: filters.endDate?.toISOString()
      };

      const response = await apiClient.get('/api/v1/orders', { params });
      
      return response.data.map((order: any) => ({
        ...order,
        createdAt: new Date(order.createdAt),
        updatedAt: new Date(order.updatedAt)
      }));
    } catch (error: any) {
      throw new Error(`Failed to fetch order history: ${error.response?.data?.message || error.message}`);
    }
  }

  /**
   * Get all active orders
   */
  async getActiveOrders(): Promise<OrderResponse[]> {
    try {
      const response = await apiClient.get('/api/v1/orders/active');
      return response.data.map((order: any) => ({
        ...order,
        createdAt: new Date(order.createdAt),
        updatedAt: new Date(order.updatedAt)
      }));
    } catch (error: any) {
      throw new Error(`Failed to fetch active orders: ${error.response?.data?.message || error.message}`);
    }
  }

  /**
   * Get order status
   */
  async getOrderStatus(orderId: string): Promise<string> {
    try {
      const response = await apiClient.get(`/api/v1/orders/${orderId}/status`);
      return response.data.status;
    } catch (error: any) {
      throw new Error(`Failed to fetch order status: ${error.response?.data?.message || error.message}`);
    }
  }

  /**
   * Get order counts by status
   */
  async getOrderCounts(): Promise<Record<string, number>> {
    try {
      const response = await apiClient.get('/api/v1/orders/count');
      return response.data;
    } catch (error: any) {
      throw new Error(`Failed to fetch order counts: ${error.response?.data?.message || error.message}`);
    }
  }

  // ============ Order Modification ============

  /**
   * Modify an existing order
   */
  async modifyOrder(orderId: string, modification: OrderModificationRequest): Promise<OrderResponse> {
    try {
      const response = await apiClient.put(`/api/v1/orders/${orderId}`, {
        ...modification,
        timestamp: new Date().toISOString()
      });
      
      return {
        ...response.data,
        createdAt: new Date(response.data.createdAt),
        updatedAt: new Date(response.data.updatedAt)
      };
    } catch (error: any) {
      throw new Error(`Order modification failed: ${error.response?.data?.message || error.message}`);
    }
  }

  /**
   * Cancel an order
   */
  async cancelOrder(orderId: string): Promise<OrderResponse> {
    try {
      const response = await apiClient.delete(`/api/v1/orders/${orderId}`, {
        data: { timestamp: new Date().toISOString() }
      });
      
      return {
        ...response.data,
        createdAt: new Date(response.data.createdAt),
        updatedAt: new Date(response.data.updatedAt)
      };
    } catch (error: any) {
      throw new Error(`Order cancellation failed: ${error.response?.data?.message || error.message}`);
    }
  }

  /**
   * Cancel multiple orders
   */
  async cancelMultipleOrders(orderIds: string[]): Promise<{ successful: string[]; failed: string[] }> {
    try {
      const response = await apiClient.post('/api/v1/orders/cancel-multiple', {
        orderIds,
        timestamp: new Date().toISOString()
      });
      return response.data;
    } catch (error: any) {
      throw new Error(`Bulk cancellation failed: ${error.response?.data?.message || error.message}`);
    }
  }

  // ============ Bulk Order Operations ============

  /**
   * Place multiple orders with execution strategy
   */
  async placeBulkOrders(bulkRequest: BulkOrderRequest): Promise<BulkExecutionStatus> {
    try {
      const response = await apiClient.post('/api/v1/orders/bulk', {
        ...bulkRequest,
        scheduledTime: bulkRequest.scheduledTime?.toISOString(),
        timestamp: new Date().toISOString()
      });
      
      return {
        ...response.data,
        startTime: new Date(response.data.startTime),
        endTime: response.data.endTime ? new Date(response.data.endTime) : undefined,
        orders: response.data.orders.map((order: any) => ({
          ...order,
          submittedAt: order.submittedAt ? new Date(order.submittedAt) : undefined,
          filledAt: order.filledAt ? new Date(order.filledAt) : undefined
        }))
      };
    } catch (error: any) {
      throw new Error(`Bulk order placement failed: ${error.response?.data?.message || error.message}`);
    }
  }

  /**
   * Get bulk execution status
   */
  async getBulkExecutionStatus(requestId: string): Promise<BulkExecutionStatus> {
    try {
      const response = await apiClient.get(`/api/v1/orders/bulk/${requestId}/status`);
      return {
        ...response.data,
        startTime: new Date(response.data.startTime),
        endTime: response.data.endTime ? new Date(response.data.endTime) : undefined,
        orders: response.data.orders.map((order: any) => ({
          ...order,
          submittedAt: order.submittedAt ? new Date(order.submittedAt) : undefined,
          filledAt: order.filledAt ? new Date(order.filledAt) : undefined
        }))
      };
    } catch (error: any) {
      throw new Error(`Failed to fetch bulk execution status: ${error.response?.data?.message || error.message}`);
    }
  }

  /**
   * Cancel bulk execution
   */
  async cancelBulkExecution(requestId: string): Promise<void> {
    try {
      await apiClient.post(`/api/v1/orders/bulk/${requestId}/cancel`, {
        timestamp: new Date().toISOString()
      });
    } catch (error: any) {
      throw new Error(`Failed to cancel bulk execution: ${error.response?.data?.message || error.message}`);
    }
  }

  // ============ Advanced Order Types ============

  /**
   * Place advanced order (Bracket, OCO, Trailing Stop, Iceberg, TWAP)
   */
  async placeAdvancedOrder(orderRequest: AdvancedOrderRequest): Promise<OrderResponse> {
    try {
      const response = await apiClient.post('/api/v1/orders/advanced', {
        ...orderRequest,
        timestamp: new Date().toISOString()
      });
      
      return {
        ...response.data,
        createdAt: new Date(response.data.createdAt),
        updatedAt: new Date(response.data.updatedAt)
      };
    } catch (error: any) {
      throw new Error(`Advanced order placement failed: ${error.response?.data?.message || error.message}`);
    }
  }

  /**
   * Get advanced order details
   */
  async getAdvancedOrderDetails(orderId: string): Promise<any> {
    try {
      const response = await apiClient.get(`/api/v1/orders/advanced/${orderId}`);
      return response.data;
    } catch (error: any) {
      throw new Error(`Failed to fetch advanced order details: ${error.response?.data?.message || error.message}`);
    }
  }

  /**
   * Modify advanced order parameters
   */
  async modifyAdvancedOrder(orderId: string, updates: Partial<AdvancedOrderRequest>): Promise<OrderResponse> {
    try {
      const response = await apiClient.put(`/api/v1/orders/advanced/${orderId}`, {
        ...updates,
        timestamp: new Date().toISOString()
      });
      
      return {
        ...response.data,
        createdAt: new Date(response.data.createdAt),
        updatedAt: new Date(response.data.updatedAt)
      };
    } catch (error: any) {
      throw new Error(`Advanced order modification failed: ${error.response?.data?.message || error.message}`);
    }
  }

  // ============ Order Analytics ============

  /**
   * Get comprehensive order analytics
   */
  async getOrderAnalytics(
    dateRange: DateRange, 
    filters: OrderAnalyticsFilters = {}
  ): Promise<OrderAnalytics> {
    try {
      const params = {
        startDate: dateRange.start.toISOString(),
        endDate: dateRange.end.toISOString(),
        ...filters
      };

      const response = await apiClient.get('/api/v1/orders/analytics', { params });
      return response.data;
    } catch (error: any) {
      throw new Error(`Failed to fetch order analytics: ${error.response?.data?.message || error.message}`);
    }
  }

  /**
   * Export order analytics data
   */
  async exportOrderAnalytics(
    dateRange: DateRange, 
    filters: OrderAnalyticsFilters = {},
    format: 'CSV' | 'PDF' | 'EXCEL' = 'CSV'
  ): Promise<Blob> {
    try {
      const params = {
        startDate: dateRange.start.toISOString(),
        endDate: dateRange.end.toISOString(),
        format,
        ...filters
      };

      const response = await apiClient.get('/api/v1/orders/analytics/export', {
        params,
        responseType: 'blob'
      });
      
      return response.data;
    } catch (error: any) {
      throw new Error(`Failed to export analytics: ${error.response?.data?.message || error.message}`);
    }
  }

  /**
   * Get trading performance summary
   */
  async getTradingPerformanceSummary(dateRange: DateRange): Promise<any> {
    try {
      const params = {
        startDate: dateRange.start.toISOString(),
        endDate: dateRange.end.toISOString()
      };

      const response = await apiClient.get('/api/v1/orders/performance', { params });
      return response.data;
    } catch (error: any) {
      throw new Error(`Failed to fetch performance summary: ${error.response?.data?.message || error.message}`);
    }
  }

  // ============ WebSocket Connections ============

  /**
   * Connect to real-time order updates
   */
  connectToOrderUpdates(callback: (update: OrderStatusUpdate) => void): WebSocket {
    const wsKey = 'order-updates';
    
    // Close existing connection if any
    if (this.wsConnections.has(wsKey)) {
      this.wsConnections.get(wsKey)?.close();
    }

    try {
      const ws = new WebSocket(`${WS_BASE_URL}/orders/updates`);
      
      ws.onopen = () => {
        console.log('Connected to order updates WebSocket');
      };

      ws.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data);
          if (data.type === 'ORDER_UPDATE') {
            const update: OrderStatusUpdate = {
              ...data.payload,
              lastUpdateTime: new Date(data.payload.lastUpdateTime),
              executionReports: data.payload.executionReports?.map((report: any) => ({
                ...report,
                timestamp: new Date(report.timestamp)
              })) || []
            };
            callback(update);
          }
        } catch (error) {
          console.error('Error parsing order update:', error);
        }
      };

      ws.onclose = (event) => {
        console.log('Order updates WebSocket closed:', event.code, event.reason);
        this.wsConnections.delete(wsKey);
      };

      ws.onerror = (event) => {
        console.error('Order updates WebSocket error:', event);
      };

      this.wsConnections.set(wsKey, ws);
      return ws;
    } catch (error) {
      console.error('Failed to establish WebSocket connection:', error);
      throw new Error('Failed to connect to real-time order updates');
    }
  }

  /**
   * Connect to specific order status updates
   */
  connectToOrderStatus(orderId: string, callback: (update: OrderStatusUpdate) => void): WebSocket {
    const wsKey = `order-status-${orderId}`;
    
    // Close existing connection if any
    if (this.wsConnections.has(wsKey)) {
      this.wsConnections.get(wsKey)?.close();
    }

    try {
      const ws = new WebSocket(`${WS_BASE_URL}/orders/${orderId}/status`);
      
      ws.onopen = () => {
        console.log(`Connected to order ${orderId} status WebSocket`);
      };

      ws.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data);
          const update: OrderStatusUpdate = {
            ...data,
            lastUpdateTime: new Date(data.lastUpdateTime),
            executionReports: data.executionReports?.map((report: any) => ({
              ...report,
              timestamp: new Date(report.timestamp)
            })) || []
          };
          callback(update);
        } catch (error) {
          console.error('Error parsing order status update:', error);
        }
      };

      ws.onclose = (event) => {
        console.log(`Order ${orderId} status WebSocket closed:`, event.code, event.reason);
        this.wsConnections.delete(wsKey);
      };

      ws.onerror = (event) => {
        console.error(`Order ${orderId} status WebSocket error:`, event);
      };

      this.wsConnections.set(wsKey, ws);
      return ws;
    } catch (error) {
      console.error(`Failed to establish WebSocket connection for order ${orderId}:`, error);
      throw new Error('Failed to connect to order status updates');
    }
  }

  /**
   * Connect to bulk execution status updates
   */
  connectToBulkExecutionUpdates(requestId: string, callback: (status: BulkExecutionStatus) => void): WebSocket {
    const wsKey = `bulk-execution-${requestId}`;
    
    // Close existing connection if any
    if (this.wsConnections.has(wsKey)) {
      this.wsConnections.get(wsKey)?.close();
    }

    try {
      const ws = new WebSocket(`${WS_BASE_URL}/orders/bulk/${requestId}/status`);
      
      ws.onopen = () => {
        console.log(`Connected to bulk execution ${requestId} WebSocket`);
      };

      ws.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data);
          const status: BulkExecutionStatus = {
            ...data,
            startTime: new Date(data.startTime),
            endTime: data.endTime ? new Date(data.endTime) : undefined,
            orders: data.orders?.map((order: any) => ({
              ...order,
              submittedAt: order.submittedAt ? new Date(order.submittedAt) : undefined,
              filledAt: order.filledAt ? new Date(order.filledAt) : undefined
            })) || []
          };
          callback(status);
        } catch (error) {
          console.error('Error parsing bulk execution update:', error);
        }
      };

      ws.onclose = (event) => {
        console.log(`Bulk execution ${requestId} WebSocket closed:`, event.code, event.reason);
        this.wsConnections.delete(wsKey);
      };

      ws.onerror = (event) => {
        console.error(`Bulk execution ${requestId} WebSocket error:`, event);
      };

      this.wsConnections.set(wsKey, ws);
      return ws;
    } catch (error) {
      console.error(`Failed to establish WebSocket connection for bulk execution ${requestId}:`, error);
      throw new Error('Failed to connect to bulk execution updates');
    }
  }

  /**
   * Close a specific WebSocket connection
   */
  closeWebSocketConnection(key: string): void {
    const ws = this.wsConnections.get(key);
    if (ws) {
      ws.close(1000, 'Connection closed by client');
      this.wsConnections.delete(key);
    }
  }

  /**
   * Close all WebSocket connections
   */
  closeAllWebSocketConnections(): void {
    this.wsConnections.forEach((ws, key) => {
      ws.close(1000, 'All connections closed');
    });
    this.wsConnections.clear();
  }

  /**
   * Get list of active WebSocket connections
   */
  getActiveConnections(): string[] {
    return Array.from(this.wsConnections.keys());
  }

  // ============ Utility Methods ============

  /**
   * Validate order request before submission
   */
  validateOrderRequest(order: OrderRequest): { valid: boolean; errors: string[] } {
    const errors: string[] = [];

    if (!order.symbol || order.symbol.trim().length === 0) {
      errors.push('Symbol is required');
    }

    if (!order.side || !['BUY', 'SELL'].includes(order.side)) {
      errors.push('Valid side (BUY/SELL) is required');
    }

    if (!order.quantity || order.quantity <= 0) {
      errors.push('Quantity must be greater than 0');
    }

    if ((order.orderType === 'LIMIT' || order.orderType === 'STOP_LIMIT') && (!order.price || order.price <= 0)) {
      errors.push('Price is required for LIMIT and STOP_LIMIT orders');
    }

    if ((order.orderType === 'STOP' || order.orderType === 'STOP_LIMIT') && (!order.stopPrice || order.stopPrice <= 0)) {
      errors.push('Stop price is required for STOP and STOP_LIMIT orders');
    }

    return {
      valid: errors.length === 0,
      errors
    };
  }

  /**
   * Calculate estimated order value
   */
  calculateOrderValue(order: OrderRequest, currentPrice?: number): number {
    const price = order.price || currentPrice || 0;
    return order.quantity * price;
  }

  /**
   * Format order for display
   */
  formatOrderForDisplay(order: OrderResponse): string {
    return `${order.side} ${order.quantity} ${order.symbol} @ ${
      order.orderType === 'MARKET' ? 'Market' : `₹${order.price}`
    }`;
  }

  /**
   * Get order status color for UI
   */
  getOrderStatusColor(status: OrderStatus): string {
    switch (status) {
      case 'PENDING':
      case 'ACKNOWLEDGED':
        return 'yellow';
      case 'PARTIALLY_FILLED':
        return 'blue';
      case 'FILLED':
        return 'green';
      case 'CANCELLED':
      case 'EXPIRED':
        return 'gray';
      case 'REJECTED':
        return 'red';
      default:
        return 'gray';
    }
  }
}

// Create singleton instance
export const tradingService = new TradingService();

// Export default
export default tradingService;