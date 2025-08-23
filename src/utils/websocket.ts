import { OrderStatusUpdate, ExecutionReport, MarketDataUpdate } from '../types/trading';

export interface WebSocketConfig {
  url: string;
  reconnectAttempts?: number;
  reconnectInterval?: number;
  pingInterval?: number;
}

export interface WebSocketCallbacks {
  onConnect?: () => void;
  onDisconnect?: () => void;
  onError?: (error: Error) => void;
  onMessage?: (data: any) => void;
  onOrderUpdate?: (update: OrderStatusUpdate) => void;
  onExecutionReport?: (report: ExecutionReport) => void;
  onMarketData?: (data: MarketDataUpdate) => void;
}

export class WebSocketManager {
  private ws: WebSocket | null = null;
  private config: WebSocketConfig;
  private callbacks: WebSocketCallbacks;
  private reconnectAttempts: number = 0;
  private reconnectTimer: NodeJS.Timeout | null = null;
  private pingTimer: NodeJS.Timeout | null = null;
  private isConnecting: boolean = false;
  private shouldReconnect: boolean = true;

  constructor(config: WebSocketConfig, callbacks: WebSocketCallbacks = {}) {
    this.config = {
      reconnectAttempts: 5,
      reconnectInterval: 3000,
      pingInterval: 30000,
      ...config
    };
    this.callbacks = callbacks;
  }

  public connect(): void {
    if (this.isConnecting || (this.ws && this.ws.readyState === WebSocket.CONNECTING)) {
      return;
    }

    this.isConnecting = true;
    
    try {
      this.ws = new WebSocket(this.config.url);
      this.setupEventHandlers();
    } catch (error) {
      this.isConnecting = false;
      this.handleError(new Error(`Failed to create WebSocket connection: ${error}`));
    }
  }

  public disconnect(): void {
    this.shouldReconnect = false;
    this.clearTimers();
    
    if (this.ws) {
      this.ws.close(1000, 'Client disconnect');
      this.ws = null;
    }
  }

  public send(data: any): boolean {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      try {
        this.ws.send(JSON.stringify(data));
        return true;
      } catch (error) {
        this.handleError(new Error(`Failed to send message: ${error}`));
        return false;
      }
    }
    return false;
  }

  public subscribe(channel: string, params?: any): boolean {
    return this.send({
      action: 'subscribe',
      channel,
      params
    });
  }

  public unsubscribe(channel: string): boolean {
    return this.send({
      action: 'unsubscribe',
      channel
    });
  }

  public getReadyState(): number | null {
    return this.ws ? this.ws.readyState : null;
  }

  public isConnected(): boolean {
    return this.ws?.readyState === WebSocket.OPEN;
  }

  private setupEventHandlers(): void {
    if (!this.ws) return;

    this.ws.onopen = () => {
      this.isConnecting = false;
      this.reconnectAttempts = 0;
      this.startPing();
      this.callbacks.onConnect?.();
    };

    this.ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        this.handleMessage(data);
      } catch (error) {
        this.handleError(new Error(`Failed to parse message: ${error}`));
      }
    };

    this.ws.onclose = (event) => {
      this.isConnecting = false;
      this.clearTimers();
      
      if (event.code !== 1000 && this.shouldReconnect) {
        this.attemptReconnect();
      }
      
      this.callbacks.onDisconnect?.();
    };

    this.ws.onerror = (event) => {
      this.isConnecting = false;
      this.handleError(new Error(`WebSocket error: ${event}`));
    };
  }

  private handleMessage(data: any): void {
    this.callbacks.onMessage?.(data);

    // Route specific message types
    switch (data.type) {
      case 'order_update':
        this.callbacks.onOrderUpdate?.(data.payload);
        break;
      case 'execution_report':
        this.callbacks.onExecutionReport?.(data.payload);
        break;
      case 'market_data':
        this.callbacks.onMarketData?.(data.payload);
        break;
      case 'pong':
        // Handle ping response
        break;
      default:
        // Handle unknown message types
        break;
    }
  }

  private handleError(error: Error): void {
    console.error('WebSocket error:', error);
    this.callbacks.onError?.(error);
  }

  private attemptReconnect(): void {
    if (this.reconnectAttempts >= (this.config.reconnectAttempts || 5)) {
      this.handleError(new Error('Max reconnection attempts reached'));
      return;
    }

    this.reconnectAttempts++;
    this.reconnectTimer = setTimeout(() => {
      console.log(`Reconnection attempt ${this.reconnectAttempts}...`);
      this.connect();
    }, this.config.reconnectInterval);
  }

  private startPing(): void {
    if (this.config.pingInterval && this.config.pingInterval > 0) {
      this.pingTimer = setInterval(() => {
        this.send({ type: 'ping', timestamp: Date.now() });
      }, this.config.pingInterval);
    }
  }

  private clearTimers(): void {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
    
    if (this.pingTimer) {
      clearInterval(this.pingTimer);
      this.pingTimer = null;
    }
  }
}

// Trading-specific WebSocket manager
export class TradingWebSocketManager extends WebSocketManager {
  constructor(baseUrl: string, callbacks: WebSocketCallbacks = {}) {
    super({
      url: `${baseUrl}/ws/trading`,
      reconnectAttempts: 10,
      reconnectInterval: 2000,
      pingInterval: 30000
    }, callbacks);
  }

  public subscribeToOrderUpdates(orderId?: string): boolean {
    return this.subscribe('orders', orderId ? { orderId } : undefined);
  }

  public subscribeToExecutionReports(portfolioId?: string): boolean {
    return this.subscribe('executions', portfolioId ? { portfolioId } : undefined);
  }

  public subscribeToMarketData(symbols: string[]): boolean {
    return this.subscribe('market_data', { symbols });
  }

  public subscribeToPositionUpdates(accountId: string): boolean {
    return this.subscribe('positions', { accountId });
  }
}

// Hook for using WebSocket in React components
export const useWebSocket = (config: WebSocketConfig, callbacks: WebSocketCallbacks) => {
  const wsManager = new WebSocketManager(config, callbacks);
  
  React.useEffect(() => {
    wsManager.connect();
    return () => wsManager.disconnect();
  }, []);

  return {
    send: wsManager.send.bind(wsManager),
    subscribe: wsManager.subscribe.bind(wsManager),
    unsubscribe: wsManager.unsubscribe.bind(wsManager),
    isConnected: wsManager.isConnected.bind(wsManager),
    disconnect: wsManager.disconnect.bind(wsManager)
  };
};