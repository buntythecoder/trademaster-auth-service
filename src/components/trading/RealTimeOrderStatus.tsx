import React, { useState, useEffect, useRef } from 'react';
import { Check, Clock, X, AlertTriangle, TrendingUp, DollarSign, Activity } from 'lucide-react';

interface OrderStatusUpdate {
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

interface ExecutionReport {
  id: string;
  quantity: number;
  price: number;
  timestamp: Date;
  exchange: string;
  fees: number;
  tradeId: string;
  counterparty?: string;
}

type OrderStatus = 
  | 'PENDING'
  | 'ACKNOWLEDGED' 
  | 'PARTIALLY_FILLED'
  | 'FILLED'
  | 'CANCELLED'
  | 'REJECTED'
  | 'EXPIRED';

interface RealTimeOrderStatusProps {
  orderId: string;
  initialOrder?: any;
  onStatusChange?: (status: OrderStatusUpdate) => void;
}

const WS_BASE_URL = import.meta.env.VITE_WS_URL || 'ws://localhost:8080/ws';

export function RealTimeOrderStatus({ 
  orderId, 
  initialOrder, 
  onStatusChange 
}: RealTimeOrderStatusProps) {
  const [orderStatus, setOrderStatus] = useState<OrderStatusUpdate | null>(null);
  const [executionHistory, setExecutionHistory] = useState<ExecutionReport[]>([]);
  const [connectionStatus, setConnectionStatus] = useState<'connecting' | 'connected' | 'disconnected'>('connecting');
  const [error, setError] = useState<string | null>(null);
  const wsRef = useRef<WebSocket | null>(null);
  const reconnectTimeoutRef = useRef<NodeJS.Timeout>();

  useEffect(() => {
    connectWebSocket();
    
    return () => {
      if (wsRef.current) {
        wsRef.current.close();
      }
      if (reconnectTimeoutRef.current) {
        clearTimeout(reconnectTimeoutRef.current);
      }
    };
  }, [orderId]);

  const connectWebSocket = () => {
    try {
      if (wsRef.current?.readyState === WebSocket.OPEN) {
        return;
      }

      setConnectionStatus('connecting');
      wsRef.current = new WebSocket(`${WS_BASE_URL}/orders/${orderId}/status`);
      
      wsRef.current.onopen = () => {
        console.log(`WebSocket connected for order ${orderId}`);
        setConnectionStatus('connected');
        setError(null);
      };

      wsRef.current.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data);
          
          if (data.type === 'ORDER_UPDATE') {
            const update: OrderStatusUpdate = {
              ...data.payload,
              lastUpdateTime: new Date(data.payload.lastUpdateTime)
            };
            
            setOrderStatus(update);
            
            if (update.executionReports && update.executionReports.length > 0) {
              const newExecutions = update.executionReports.map(report => ({
                ...report,
                timestamp: new Date(report.timestamp)
              }));
              
              setExecutionHistory(prev => {
                const existingIds = new Set(prev.map(r => r.id));
                const filtered = newExecutions.filter(r => !existingIds.has(r.id));
                return [...prev, ...filtered].sort(
                  (a, b) => b.timestamp.getTime() - a.timestamp.getTime()
                );
              });
            }
            
            onStatusChange?.(update);
          }
        } catch (err) {
          console.error('Error parsing WebSocket message:', err);
        }
      };

      wsRef.current.onclose = (event) => {
        console.log(`WebSocket closed for order ${orderId}:`, event.code, event.reason);
        setConnectionStatus('disconnected');
        
        // Attempt to reconnect unless it was intentionally closed
        if (event.code !== 1000) {
          reconnectTimeoutRef.current = setTimeout(() => {
            connectWebSocket();
          }, 3000);
        }
      };

      wsRef.current.onerror = (event) => {
        console.error('WebSocket error:', event);
        setError('Connection error occurred');
        setConnectionStatus('disconnected');
      };

    } catch (err) {
      console.error('Failed to establish WebSocket connection:', err);
      setError('Failed to connect to real-time updates');
      setConnectionStatus('disconnected');
    }
  };

  const getStatusColor = (status: OrderStatus) => {
    switch (status) {
      case 'PENDING':
      case 'ACKNOWLEDGED':
        return 'bg-yellow-500/20 text-yellow-400 border-yellow-500/50';
      case 'PARTIALLY_FILLED':
        return 'bg-blue-500/20 text-blue-400 border-blue-500/50';
      case 'FILLED':
        return 'bg-green-500/20 text-green-400 border-green-500/50';
      case 'CANCELLED':
      case 'EXPIRED':
        return 'bg-gray-500/20 text-gray-400 border-gray-500/50';
      case 'REJECTED':
        return 'bg-red-500/20 text-red-400 border-red-500/50';
      default:
        return 'bg-slate-500/20 text-slate-400 border-slate-500/50';
    }
  };

  const getStatusIcon = (status: OrderStatus) => {
    switch (status) {
      case 'PENDING':
      case 'ACKNOWLEDGED':
        return <Clock className="w-4 h-4" />;
      case 'PARTIALLY_FILLED':
        return <Activity className="w-4 h-4" />;
      case 'FILLED':
        return <Check className="w-4 h-4" />;
      case 'CANCELLED':
      case 'EXPIRED':
        return <X className="w-4 h-4" />;
      case 'REJECTED':
        return <AlertTriangle className="w-4 h-4" />;
      default:
        return <Clock className="w-4 h-4" />;
    }
  };

  const formatPrice = (price: number | undefined) => {
    if (price === undefined) return '-';
    return `₹${price.toFixed(2)}`;
  };

  const formatTime = (date: Date | undefined) => {
    if (!date) return '-';
    return date.toLocaleTimeString('en-IN', { 
      hour12: false,
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    });
  };

  const totalQuantity = orderStatus ? orderStatus.filledQuantity + orderStatus.remainingQuantity : 0;
  const fillPercentage = totalQuantity > 0 ? (orderStatus?.filledQuantity || 0) / totalQuantity * 100 : 0;

  return (
    <div className="space-y-4">
      {/* Connection Status */}
      <div className="flex items-center justify-between">
        <h3 className="text-lg font-semibold text-white">Order #{orderId}</h3>
        <div className="flex items-center space-x-2">
          <div className={`w-2 h-2 rounded-full ${
            connectionStatus === 'connected' ? 'bg-green-400' : 
            connectionStatus === 'connecting' ? 'bg-yellow-400' : 'bg-red-400'
          }`} />
          <span className="text-xs text-slate-400 capitalize">
            {connectionStatus}
          </span>
        </div>
      </div>

      {error && (
        <div className="bg-red-500/10 border border-red-500/20 rounded-lg p-3">
          <div className="flex items-center space-x-2">
            <AlertTriangle className="w-4 h-4 text-red-400" />
            <span className="text-red-400 text-sm">{error}</span>
          </div>
        </div>
      )}

      {/* Order Status Header */}
      <div className="glass-card rounded-xl p-6">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center space-x-3">
            <div className={`px-3 py-1 rounded-lg border text-sm font-medium flex items-center space-x-2 ${
              getStatusColor(orderStatus?.status || 'PENDING')
            }`}>
              {getStatusIcon(orderStatus?.status || 'PENDING')}
              <span>{orderStatus?.status || 'PENDING'}</span>
            </div>
            {orderStatus?.queuePosition && orderStatus.queuePosition > 0 && (
              <div className="text-xs text-slate-400">
                Queue position: #{orderStatus.queuePosition}
              </div>
            )}
          </div>
          <div className="text-right">
            <div className="text-sm text-slate-400">Last updated</div>
            <div className="text-white font-medium">
              {formatTime(orderStatus?.lastUpdateTime)}
            </div>
          </div>
        </div>

        {/* Execution Progress */}
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <div>
              <div className="text-2xl font-bold text-white">
                {orderStatus?.filledQuantity || 0} / {totalQuantity}
              </div>
              <div className="text-sm text-slate-400">Filled / Total Quantity</div>
            </div>
            <div className="text-right">
              <div className="text-2xl font-bold text-white">
                {fillPercentage.toFixed(1)}%
              </div>
              <div className="text-sm text-slate-400">Fill Rate</div>
            </div>
          </div>

          {/* Progress Bar */}
          <div className="w-full bg-slate-700/50 rounded-full h-3">
            <div
              className="bg-gradient-to-r from-purple-500 to-blue-500 h-3 rounded-full transition-all duration-300"
              style={{ width: `${Math.min(fillPercentage, 100)}%` }}
            />
          </div>

          {/* Price and Metrics */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 pt-4 border-t border-slate-700/50">
            <div>
              <div className="text-sm text-slate-400">Average Price</div>
              <div className="text-lg font-semibold text-white">
                {formatPrice(orderStatus?.averagePrice)}
              </div>
            </div>
            <div>
              <div className="text-sm text-slate-400">Total Fees</div>
              <div className="text-lg font-semibold text-white">
                {formatPrice(orderStatus?.fees)}
              </div>
            </div>
            <div>
              <div className="text-sm text-slate-400">Slippage</div>
              <div className={`text-lg font-semibold ${
                (orderStatus?.slippage || 0) > 0 ? 'text-red-400' : 'text-green-400'
              }`}>
                {orderStatus?.slippage ? `${orderStatus.slippage > 0 ? '+' : ''}${(orderStatus.slippage * 100).toFixed(2)}%` : '-'}
              </div>
            </div>
            <div>
              <div className="text-sm text-slate-400">Remaining</div>
              <div className="text-lg font-semibold text-white">
                {orderStatus?.remainingQuantity || 0}
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Execution History */}
      {executionHistory.length > 0 && (
        <div className="glass-card rounded-xl p-6">
          <h4 className="text-lg font-semibold text-white mb-4 flex items-center">
            <TrendingUp className="w-5 h-5 mr-2" />
            Execution History ({executionHistory.length})
          </h4>
          
          <div className="space-y-3">
            {executionHistory.slice(0, 10).map((execution) => (
              <div
                key={execution.id}
                className="flex items-center justify-between p-3 bg-slate-800/50 rounded-lg border border-slate-700/50"
              >
                <div className="flex items-center space-x-4">
                  <div className="w-2 h-2 bg-green-400 rounded-full" />
                  <div>
                    <div className="text-white font-medium">
                      {execution.quantity} @ {formatPrice(execution.price)}
                    </div>
                    <div className="text-xs text-slate-400">
                      {execution.exchange} • {formatTime(execution.timestamp)}
                    </div>
                  </div>
                </div>
                <div className="text-right">
                  <div className="text-white font-medium">
                    {formatPrice(execution.quantity * execution.price)}
                  </div>
                  <div className="text-xs text-slate-400">
                    Fee: {formatPrice(execution.fees)}
                  </div>
                </div>
              </div>
            ))}
            
            {executionHistory.length > 10 && (
              <div className="text-center">
                <button className="text-sm text-purple-400 hover:text-purple-300 transition-colors">
                  Show {executionHistory.length - 10} more executions
                </button>
              </div>
            )}
          </div>
        </div>
      )}

      {/* No executions yet */}
      {executionHistory.length === 0 && orderStatus && (
        <div className="glass-card rounded-xl p-6 text-center">
          <DollarSign className="w-12 h-12 text-slate-600 mx-auto mb-3" />
          <div className="text-slate-400">No executions yet</div>
          <div className="text-sm text-slate-500 mt-1">
            Executions will appear here as your order gets filled
          </div>
        </div>
      )}
    </div>
  );
}

export default RealTimeOrderStatus;