import { useState, useEffect, useCallback } from 'react';
import { useWebSocket } from './useWebSocket';
import { OrderStatusUpdate, ExecutionReport, MarketDataUpdate } from '../types/trading';

interface UseTradingWebSocketOptions {
  baseUrl: string;
  autoConnect?: boolean;
  onOrderUpdate?: (update: OrderStatusUpdate) => void;
  onExecutionReport?: (report: ExecutionReport) => void;
  onMarketData?: (data: MarketDataUpdate) => void;
}

interface UseTradingWebSocketReturn {
  isConnected: boolean;
  isConnecting: boolean;
  error: Error | null;
  connect: () => void;
  disconnect: () => void;
  subscribeToOrderUpdates: (orderId?: string) => boolean;
  subscribeToExecutionReports: (portfolioId?: string) => boolean;
  subscribeToMarketData: (symbols: string[]) => boolean;
  subscribeToPositionUpdates: (accountId: string) => boolean;
  unsubscribeFromOrderUpdates: () => boolean;
  unsubscribeFromExecutionReports: () => boolean;
  unsubscribeFromMarketData: () => boolean;
  unsubscribeFromPositionUpdates: () => boolean;
  orderUpdates: OrderStatusUpdate[];
  executionReports: ExecutionReport[];
  marketData: Record<string, MarketDataUpdate>;
  connectionStatus: 'disconnected' | 'connecting' | 'connected' | 'error';
}

export const useTradingWebSocket = (options: UseTradingWebSocketOptions): UseTradingWebSocketReturn => {
  const [orderUpdates, setOrderUpdates] = useState<OrderStatusUpdate[]>([]);
  const [executionReports, setExecutionReports] = useState<ExecutionReport[]>([]);
  const [marketData, setMarketData] = useState<Record<string, MarketDataUpdate>>({});
  const [connectionStatus, setConnectionStatus] = useState<'disconnected' | 'connecting' | 'connected' | 'error'>('disconnected');

  const handleMessage = useCallback((data: any) => {
    switch (data.type) {
      case 'order_update':
        const orderUpdate = data.payload as OrderStatusUpdate;
        setOrderUpdates(prev => [orderUpdate, ...prev.slice(0, 99)]); // Keep last 100 updates
        options.onOrderUpdate?.(orderUpdate);
        break;
        
      case 'execution_report':
        const executionReport = data.payload as ExecutionReport;
        setExecutionReports(prev => [executionReport, ...prev.slice(0, 99)]); // Keep last 100 reports
        options.onExecutionReport?.(executionReport);
        break;
        
      case 'market_data':
        const marketDataUpdate = data.payload as MarketDataUpdate;
        setMarketData(prev => ({
          ...prev,
          [marketDataUpdate.symbol]: marketDataUpdate
        }));
        options.onMarketData?.(marketDataUpdate);
        break;
        
      default:
        break;
    }
  }, [options]);

  const {
    isConnected,
    isConnecting,
    error,
    connect,
    disconnect,
    subscribe,
    unsubscribe
  } = useWebSocket({
    url: `${options.baseUrl}/ws/trading`,
    reconnectAttempts: 10,
    reconnectInterval: 2000,
    pingInterval: 30000,
    autoConnect: options.autoConnect,
    onConnect: () => {
      setConnectionStatus('connected');
    },
    onDisconnect: () => {
      setConnectionStatus('disconnected');
    },
    onError: () => {
      setConnectionStatus('error');
    },
    onMessage: handleMessage
  });

  // Update connection status based on WebSocket state
  useEffect(() => {
    if (isConnecting) {
      setConnectionStatus('connecting');
    } else if (isConnected) {
      setConnectionStatus('connected');
    } else if (error) {
      setConnectionStatus('error');
    } else {
      setConnectionStatus('disconnected');
    }
  }, [isConnected, isConnecting, error]);

  const subscribeToOrderUpdates = useCallback((orderId?: string): boolean => {
    return subscribe('orders', orderId ? { orderId } : undefined);
  }, [subscribe]);

  const subscribeToExecutionReports = useCallback((portfolioId?: string): boolean => {
    return subscribe('executions', portfolioId ? { portfolioId } : undefined);
  }, [subscribe]);

  const subscribeToMarketData = useCallback((symbols: string[]): boolean => {
    return subscribe('market_data', { symbols });
  }, [subscribe]);

  const subscribeToPositionUpdates = useCallback((accountId: string): boolean => {
    return subscribe('positions', { accountId });
  }, [subscribe]);

  const unsubscribeFromOrderUpdates = useCallback((): boolean => {
    return unsubscribe('orders');
  }, [unsubscribe]);

  const unsubscribeFromExecutionReports = useCallback((): boolean => {
    return unsubscribe('executions');
  }, [unsubscribe]);

  const unsubscribeFromMarketData = useCallback((): boolean => {
    return unsubscribe('market_data');
  }, [unsubscribe]);

  const unsubscribeFromPositionUpdates = useCallback((): boolean => {
    return unsubscribe('positions');
  }, [unsubscribe]);

  // Clear data when disconnected
  useEffect(() => {
    if (!isConnected) {
      setOrderUpdates([]);
      setExecutionReports([]);
      setMarketData({});
    }
  }, [isConnected]);

  return {
    isConnected,
    isConnecting,
    error,
    connect,
    disconnect,
    subscribeToOrderUpdates,
    subscribeToExecutionReports,
    subscribeToMarketData,
    subscribeToPositionUpdates,
    unsubscribeFromOrderUpdates,
    unsubscribeFromExecutionReports,
    unsubscribeFromMarketData,
    unsubscribeFromPositionUpdates,
    orderUpdates,
    executionReports,
    marketData,
    connectionStatus
  };
};