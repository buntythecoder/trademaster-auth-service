import React, { useState, useEffect, useCallback } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Layers, Clock, Target, BarChart3, Settings, 
  TrendingUp, Activity, Zap, Shield, Brain,
  Play, Pause, Square, AlertTriangle, CheckCircle,
  Sliders, Gauge, Timer, Waves, Filter,
  Eye, Edit, Copy, Trash2, MoreVertical,
  ArrowUpDown, Calendar, DollarSign, Users
} from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Input } from '@/components/ui/input';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';

interface TWAPOrder {
  id: string;
  symbol: string;
  side: 'BUY' | 'SELL';
  totalQuantity: number;
  duration: number; // in minutes
  sliceSize: number;
  interval: number; // in seconds
  startTime: Date;
  endTime: Date;
  executed: number;
  remaining: number;
  averagePrice: number;
  status: 'ACTIVE' | 'PAUSED' | 'COMPLETED' | 'CANCELLED';
  randomization: number; // percentage
  priceLimit?: number;
  urgency: 'LOW' | 'MEDIUM' | 'HIGH';
  adaptiveSlicing: boolean;
}

interface VWAPOrder {
  id: string;
  symbol: string;
  side: 'BUY' | 'SELL';
  totalQuantity: number;
  participationRate: number; // percentage of market volume
  targetVWAP: number;
  currentVWAP: number;
  executed: number;
  remaining: number;
  averagePrice: number;
  status: 'ACTIVE' | 'PAUSED' | 'COMPLETED' | 'CANCELLED';
  lookbackPeriod: number; // in minutes
  maxParticipation: number;
  minParticipation: number;
  aggressiveness: 'PASSIVE' | 'NEUTRAL' | 'AGGRESSIVE';
}

interface IcebergOrder {
  id: string;
  symbol: string;
  side: 'BUY' | 'SELL';
  totalQuantity: number;
  visibleQuantity: number;
  executedQuantity: number;
  remainingQuantity: number;
  price: number;
  averagePrice: number;
  status: 'ACTIVE' | 'PAUSED' | 'COMPLETED' | 'CANCELLED';
  refreshSize: number;
  priceVariance: number; // in ticks
  timeVariance: number; // in seconds
  hiddenQuantity: number;
}

interface POVOrder {
  id: string;
  symbol: string;
  side: 'BUY' | 'SELL';
  totalQuantity: number;
  targetPercentage: number; // percentage of volume
  maxPercentage: number;
  minPercentage: number;
  executed: number;
  remaining: number;
  averagePrice: number;
  status: 'ACTIVE' | 'PAUSED' | 'COMPLETED' | 'CANCELLED';
  lookbackWindow: number; // in minutes
  aggressiveness: 'LOW' | 'MEDIUM' | 'HIGH';
  marketImpactLimit: number; // in bps
}

interface InstitutionalOrderTypesProps {
  userId: string;
  onOrderCreate?: (order: any) => void;
  onOrderUpdate?: (order: any) => void;
}

export const InstitutionalOrderTypes: React.FC<InstitutionalOrderTypesProps> = ({
  userId,
  onOrderCreate,
  onOrderUpdate
}) => {
  const [twapOrders, setTwapOrders] = useState<TWAPOrder[]>([]);
  const [vwapOrders, setVwapOrders] = useState<VWAPOrder[]>([]);
  const [icebergOrders, setIcebergOrders] = useState<IcebergOrder[]>([]);
  const [povOrders, setPovOrders] = useState<POVOrder[]>([]);
  const [activeTab, setActiveTab] = useState<'twap' | 'vwap' | 'iceberg' | 'pov' | 'create'>('twap');
  const [selectedOrderType, setSelectedOrderType] = useState<'TWAP' | 'VWAP' | 'ICEBERG' | 'POV'>('TWAP');
  const [isSimulating, setIsSimulating] = useState(true);
  
  // Form state for creating new orders
  const [newOrder, setNewOrder] = useState({
    symbol: 'RELIANCE',
    side: 'BUY' as 'BUY' | 'SELL',
    totalQuantity: 1000,
    // TWAP specific
    duration: 60,
    sliceSize: 50,
    interval: 60,
    randomization: 0.2,
    // VWAP specific
    participationRate: 0.15,
    lookbackPeriod: 30,
    aggressiveness: 'NEUTRAL' as 'PASSIVE' | 'NEUTRAL' | 'AGGRESSIVE',
    // Iceberg specific
    visibleQuantity: 100,
    refreshSize: 80,
    priceVariance: 1,
    // POV specific
    targetPercentage: 0.1,
    maxPercentage: 0.25,
    minPercentage: 0.05
  });

  useEffect(() => {
    initializeMockData();
    if (isSimulating) {
      const interval = setInterval(simulateOrderUpdates, 2000);
      return () => clearInterval(interval);
    }
  }, [isSimulating]);

  const initializeMockData = () => {
    const mockTwapOrders: TWAPOrder[] = [
      {
        id: 'TWAP-001',
        symbol: 'RELIANCE',
        side: 'BUY',
        totalQuantity: 5000,
        duration: 120,
        sliceSize: 50,
        interval: 60,
        startTime: new Date(Date.now() - 1800000),
        endTime: new Date(Date.now() + 5400000),
        executed: 2500,
        remaining: 2500,
        averagePrice: 2449.25,
        status: 'ACTIVE',
        randomization: 0.2,
        priceLimit: 2455.00,
        urgency: 'MEDIUM',
        adaptiveSlicing: true
      },
      {
        id: 'TWAP-002',
        symbol: 'TCS',
        side: 'SELL',
        totalQuantity: 3000,
        duration: 90,
        sliceSize: 40,
        interval: 45,
        startTime: new Date(Date.now() - 3600000),
        endTime: new Date(Date.now() + 1800000),
        executed: 2200,
        remaining: 800,
        averagePrice: 3456.80,
        status: 'ACTIVE',
        randomization: 0.15,
        urgency: 'HIGH',
        adaptiveSlicing: true
      }
    ];

    const mockVwapOrders: VWAPOrder[] = [
      {
        id: 'VWAP-001',
        symbol: 'INFY',
        side: 'BUY',
        totalQuantity: 8000,
        participationRate: 0.12,
        targetVWAP: 1456.50,
        currentVWAP: 1455.20,
        executed: 4800,
        remaining: 3200,
        averagePrice: 1454.85,
        status: 'ACTIVE',
        lookbackPeriod: 30,
        maxParticipation: 0.25,
        minParticipation: 0.05,
        aggressiveness: 'NEUTRAL'
      }
    ];

    const mockIcebergOrders: IcebergOrder[] = [
      {
        id: 'ICE-001',
        symbol: 'HDFC',
        side: 'BUY',
        totalQuantity: 10000,
        visibleQuantity: 200,
        executedQuantity: 3500,
        remainingQuantity: 6500,
        price: 1650.50,
        averagePrice: 1649.75,
        status: 'ACTIVE',
        refreshSize: 150,
        priceVariance: 2,
        timeVariance: 30,
        hiddenQuantity: 9800
      }
    ];

    const mockPovOrders: POVOrder[] = [
      {
        id: 'POV-001',
        symbol: 'ICICI',
        side: 'SELL',
        totalQuantity: 6000,
        targetPercentage: 0.08,
        maxPercentage: 0.15,
        minPercentage: 0.03,
        executed: 2400,
        remaining: 3600,
        averagePrice: 945.20,
        status: 'ACTIVE',
        lookbackWindow: 20,
        aggressiveness: 'MEDIUM',
        marketImpactLimit: 10
      }
    ];

    setTwapOrders(mockTwapOrders);
    setVwapOrders(mockVwapOrders);
    setIcebergOrders(mockIcebergOrders);
    setPovOrders(mockPovOrders);
  };

  const simulateOrderUpdates = () => {
    // Simulate TWAP order updates
    setTwapOrders(prev => prev.map(order => {
      if (order.status === 'ACTIVE' && order.remaining > 0) {
        const sliceExecution = Math.min(order.sliceSize, order.remaining);
        const executed = order.executed + sliceExecution;
        const remaining = order.remaining - sliceExecution;
        
        return {
          ...order,
          executed,
          remaining,
          status: remaining === 0 ? 'COMPLETED' as const : order.status,
          averagePrice: order.averagePrice + (Math.random() - 0.5) * 2
        };
      }
      return order;
    }));

    // Simulate VWAP order updates
    setVwapOrders(prev => prev.map(order => {
      if (order.status === 'ACTIVE' && order.remaining > 0) {
        const volumeExecution = Math.floor(Math.random() * 100) + 50;
        const executed = Math.min(order.executed + volumeExecution, order.totalQuantity);
        const remaining = order.totalQuantity - executed;
        
        return {
          ...order,
          executed,
          remaining,
          status: remaining === 0 ? 'COMPLETED' as const : order.status,
          currentVWAP: order.currentVWAP + (Math.random() - 0.5) * 1
        };
      }
      return order;
    }));

    // Simulate Iceberg order updates
    setIcebergOrders(prev => prev.map(order => {
      if (order.status === 'ACTIVE' && order.remainingQuantity > 0) {
        const execution = Math.floor(Math.random() * order.visibleQuantity * 0.5);
        const executedQuantity = order.executedQuantity + execution;
        const remainingQuantity = order.remainingQuantity - execution;
        
        return {
          ...order,
          executedQuantity,
          remainingQuantity,
          status: remainingQuantity === 0 ? 'COMPLETED' as const : order.status,
          averagePrice: order.averagePrice + (Math.random() - 0.5) * 1
        };
      }
      return order;
    }));

    // Simulate POV order updates
    setPovOrders(prev => prev.map(order => {
      if (order.status === 'ACTIVE' && order.remaining > 0) {
        const povExecution = Math.floor(Math.random() * 80) + 20;
        const executed = Math.min(order.executed + povExecution, order.totalQuantity);
        const remaining = order.totalQuantity - executed;
        
        return {
          ...order,
          executed,
          remaining,
          status: remaining === 0 ? 'COMPLETED' as const : order.status,
          averagePrice: order.averagePrice + (Math.random() - 0.5) * 1.5
        };
      }
      return order;
    }));
  };

  const handleOrderAction = (orderId: string, action: 'PAUSE' | 'RESUME' | 'CANCEL') => {
    const updateStatus = (status: 'PAUSED' | 'ACTIVE' | 'CANCELLED') => {
      setTwapOrders(prev => prev.map(o => o.id === orderId ? { ...o, status } : o));
      setVwapOrders(prev => prev.map(o => o.id === orderId ? { ...o, status } : o));
      setIcebergOrders(prev => prev.map(o => o.id === orderId ? { ...o, status } : o));
      setPovOrders(prev => prev.map(o => o.id === orderId ? { ...o, status } : o));
    };

    switch (action) {
      case 'PAUSE':
        updateStatus('PAUSED');
        break;
      case 'RESUME':
        updateStatus('ACTIVE');
        break;
      case 'CANCEL':
        updateStatus('CANCELLED');
        break;
    }
  };

  const createNewOrder = () => {
    const baseOrder = {
      id: `${selectedOrderType}-${Date.now()}`,
      symbol: newOrder.symbol,
      side: newOrder.side,
      totalQuantity: newOrder.totalQuantity,
      status: 'ACTIVE' as const
    };

    switch (selectedOrderType) {
      case 'TWAP':
        const twapOrder: TWAPOrder = {
          ...baseOrder,
          duration: newOrder.duration,
          sliceSize: newOrder.sliceSize,
          interval: newOrder.interval,
          startTime: new Date(),
          endTime: new Date(Date.now() + newOrder.duration * 60000),
          executed: 0,
          remaining: newOrder.totalQuantity,
          averagePrice: 0,
          randomization: newOrder.randomization,
          urgency: 'MEDIUM',
          adaptiveSlicing: true
        };
        setTwapOrders(prev => [...prev, twapOrder]);
        onOrderCreate?.(twapOrder);
        break;

      case 'VWAP':
        const vwapOrder: VWAPOrder = {
          ...baseOrder,
          participationRate: newOrder.participationRate,
          targetVWAP: 0,
          currentVWAP: 0,
          executed: 0,
          remaining: newOrder.totalQuantity,
          averagePrice: 0,
          lookbackPeriod: newOrder.lookbackPeriod,
          maxParticipation: 0.25,
          minParticipation: 0.05,
          aggressiveness: newOrder.aggressiveness
        };
        setVwapOrders(prev => [...prev, vwapOrder]);
        onOrderCreate?.(vwapOrder);
        break;

      case 'ICEBERG':
        const icebergOrder: IcebergOrder = {
          ...baseOrder,
          visibleQuantity: newOrder.visibleQuantity,
          executedQuantity: 0,
          remainingQuantity: newOrder.totalQuantity,
          price: 0,
          averagePrice: 0,
          refreshSize: newOrder.refreshSize,
          priceVariance: newOrder.priceVariance,
          timeVariance: 30,
          hiddenQuantity: newOrder.totalQuantity - newOrder.visibleQuantity
        };
        setIcebergOrders(prev => [...prev, icebergOrder]);
        onOrderCreate?.(icebergOrder);
        break;

      case 'POV':
        const povOrder: POVOrder = {
          ...baseOrder,
          targetPercentage: newOrder.targetPercentage,
          maxPercentage: newOrder.maxPercentage,
          minPercentage: newOrder.minPercentage,
          executed: 0,
          remaining: newOrder.totalQuantity,
          averagePrice: 0,
          lookbackWindow: 20,
          aggressiveness: 'MEDIUM',
          marketImpactLimit: 10
        };
        setPovOrders(prev => [...prev, povOrder]);
        onOrderCreate?.(povOrder);
        break;
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'ACTIVE': return 'bg-green-100 text-green-800 border-green-200';
      case 'PAUSED': return 'bg-yellow-100 text-yellow-800 border-yellow-200';
      case 'COMPLETED': return 'bg-blue-100 text-blue-800 border-blue-200';
      case 'CANCELLED': return 'bg-red-100 text-red-800 border-red-200';
      default: return 'bg-gray-100 text-gray-800 border-gray-200';
    }
  };

  const getProgressPercentage = (executed: number, total: number) => {
    return Math.min((executed / total) * 100, 100);
  };

  return (
    <div className="p-6 space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Institutional Order Types</h1>
          <p className="text-gray-600">Advanced algorithmic order execution strategies</p>
        </div>
        <div className="flex items-center space-x-3">
          <Button
            variant={isSimulating ? "destructive" : "default"}
            onClick={() => setIsSimulating(!isSimulating)}
            className="flex items-center space-x-2"
          >
            {isSimulating ? <Pause className="h-4 w-4" /> : <Play className="h-4 w-4" />}
            <span>{isSimulating ? 'Stop' : 'Start'} Simulation</span>
          </Button>
          <Button 
            onClick={() => setActiveTab('create')} 
            className="flex items-center space-x-2"
          >
            <Layers className="h-4 w-4" />
            <span>New Order</span>
          </Button>
        </div>
      </div>

      {/* Key Metrics */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <Card>
          <CardContent className="p-6">
            <div className="flex items-center">
              <Activity className="h-8 w-8 text-blue-600" />
              <div className="ml-4">
                <p className="text-sm font-medium text-gray-600">Active Orders</p>
                <p className="text-2xl font-bold text-gray-900">
                  {[...twapOrders, ...vwapOrders, ...icebergOrders, ...povOrders]
                    .filter(order => order.status === 'ACTIVE').length}
                </p>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-6">
            <div className="flex items-center">
              <Target className="h-8 w-8 text-green-600" />
              <div className="ml-4">
                <p className="text-sm font-medium text-gray-600">Avg Fill Rate</p>
                <p className="text-2xl font-bold text-gray-900">94.2%</p>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-6">
            <div className="flex items-center">
              <Clock className="h-8 w-8 text-purple-600" />
              <div className="ml-4">
                <p className="text-sm font-medium text-gray-600">Avg Duration</p>
                <p className="text-2xl font-bold text-gray-900">85 min</p>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-6">
            <div className="flex items-center">
              <TrendingUp className="h-8 w-8 text-orange-600" />
              <div className="ml-4">
                <p className="text-sm font-medium text-gray-600">Market Impact</p>
                <p className="text-2xl font-bold text-gray-900">1.8 bps</p>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Main Content */}
      <Tabs value={activeTab} onValueChange={(value) => setActiveTab(value as any)}>
        <TabsList className="grid w-full grid-cols-5">
          <TabsTrigger value="twap" className="flex items-center space-x-2">
            <Timer className="h-4 w-4" />
            <span>TWAP</span>
          </TabsTrigger>
          <TabsTrigger value="vwap" className="flex items-center space-x-2">
            <BarChart3 className="h-4 w-4" />
            <span>VWAP</span>
          </TabsTrigger>
          <TabsTrigger value="iceberg" className="flex items-center space-x-2">
            <Waves className="h-4 w-4" />
            <span>Iceberg</span>
          </TabsTrigger>
          <TabsTrigger value="pov" className="flex items-center space-x-2">
            <Gauge className="h-4 w-4" />
            <span>POV</span>
          </TabsTrigger>
          <TabsTrigger value="create" className="flex items-center space-x-2">
            <Layers className="h-4 w-4" />
            <span>Create</span>
          </TabsTrigger>
        </TabsList>

        <TabsContent value="twap" className="space-y-4">
          <div className="space-y-4">
            {twapOrders.map((order) => (
              <Card key={order.id}>
                <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                  <div className="flex items-center space-x-3">
                    <CardTitle className="text-sm font-medium">{order.id}</CardTitle>
                    <Badge className={getStatusColor(order.status)}>{order.status}</Badge>
                    <Badge variant={order.side === 'BUY' ? 'default' : 'destructive'}>
                      {order.side}
                    </Badge>
                  </div>
                  <div className="flex items-center space-x-2">
                    {order.status === 'ACTIVE' && (
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => handleOrderAction(order.id, 'PAUSE')}
                      >
                        <Pause className="h-4 w-4" />
                      </Button>
                    )}
                    {order.status === 'PAUSED' && (
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => handleOrderAction(order.id, 'RESUME')}
                      >
                        <Play className="h-4 w-4" />
                      </Button>
                    )}
                    <Button
                      size="sm"
                      variant="destructive"
                      onClick={() => handleOrderAction(order.id, 'CANCEL')}
                    >
                      <Square className="h-4 w-4" />
                    </Button>
                  </div>
                </CardHeader>
                <CardContent>
                  <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
                    <div className="space-y-2">
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Symbol</span>
                        <span className="text-sm font-medium">{order.symbol}</span>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Total Quantity</span>
                        <span className="text-sm font-medium">{order.totalQuantity.toLocaleString()}</span>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Slice Size</span>
                        <span className="text-sm font-medium">{order.sliceSize}</span>
                      </div>
                    </div>
                    
                    <div className="space-y-2">
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Duration</span>
                        <span className="text-sm font-medium">{order.duration} min</span>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Interval</span>
                        <span className="text-sm font-medium">{order.interval}s</span>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Randomization</span>
                        <span className="text-sm font-medium">{(order.randomization * 100).toFixed(0)}%</span>
                      </div>
                    </div>
                    
                    <div className="space-y-2">
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Executed</span>
                        <span className="text-sm font-medium">{order.executed.toLocaleString()}</span>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Remaining</span>
                        <span className="text-sm font-medium">{order.remaining.toLocaleString()}</span>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Avg Price</span>
                        <span className="text-sm font-medium">₹{order.averagePrice.toFixed(2)}</span>
                      </div>
                    </div>
                    
                    <div className="space-y-2">
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Progress</span>
                        <span className="text-sm font-medium">
                          {getProgressPercentage(order.executed, order.totalQuantity).toFixed(1)}%
                        </span>
                      </div>
                      <div className="w-full bg-gray-200 rounded-full h-2">
                        <div
                          className="bg-blue-600 h-2 rounded-full"
                          style={{ width: `${getProgressPercentage(order.executed, order.totalQuantity)}%` }}
                        />
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Time Left</span>
                        <span className="text-sm font-medium">
                          {Math.max(0, Math.floor((order.endTime.getTime() - Date.now()) / 60000))} min
                        </span>
                      </div>
                    </div>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        </TabsContent>

        <TabsContent value="vwap" className="space-y-4">
          <div className="space-y-4">
            {vwapOrders.map((order) => (
              <Card key={order.id}>
                <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                  <div className="flex items-center space-x-3">
                    <CardTitle className="text-sm font-medium">{order.id}</CardTitle>
                    <Badge className={getStatusColor(order.status)}>{order.status}</Badge>
                    <Badge variant={order.side === 'BUY' ? 'default' : 'destructive'}>
                      {order.side}
                    </Badge>
                  </div>
                  <div className="flex items-center space-x-2">
                    {order.status === 'ACTIVE' && (
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => handleOrderAction(order.id, 'PAUSE')}
                      >
                        <Pause className="h-4 w-4" />
                      </Button>
                    )}
                    {order.status === 'PAUSED' && (
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => handleOrderAction(order.id, 'RESUME')}
                      >
                        <Play className="h-4 w-4" />
                      </Button>
                    )}
                    <Button
                      size="sm"
                      variant="destructive"
                      onClick={() => handleOrderAction(order.id, 'CANCEL')}
                    >
                      <Square className="h-4 w-4" />
                    </Button>
                  </div>
                </CardHeader>
                <CardContent>
                  <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
                    <div className="space-y-2">
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Symbol</span>
                        <span className="text-sm font-medium">{order.symbol}</span>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Total Quantity</span>
                        <span className="text-sm font-medium">{order.totalQuantity.toLocaleString()}</span>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Participation Rate</span>
                        <span className="text-sm font-medium">{(order.participationRate * 100).toFixed(1)}%</span>
                      </div>
                    </div>
                    
                    <div className="space-y-2">
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Target VWAP</span>
                        <span className="text-sm font-medium">₹{order.targetVWAP.toFixed(2)}</span>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Current VWAP</span>
                        <span className="text-sm font-medium">₹{order.currentVWAP.toFixed(2)}</span>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Performance</span>
                        <span className={`text-sm font-medium ${
                          order.currentVWAP < order.targetVWAP ? 'text-green-600' : 'text-red-600'
                        }`}>
                          {order.currentVWAP < order.targetVWAP ? 'Outperforming' : 'Underperforming'}
                        </span>
                      </div>
                    </div>
                    
                    <div className="space-y-2">
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Executed</span>
                        <span className="text-sm font-medium">{order.executed.toLocaleString()}</span>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Remaining</span>
                        <span className="text-sm font-medium">{order.remaining.toLocaleString()}</span>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Avg Price</span>
                        <span className="text-sm font-medium">₹{order.averagePrice.toFixed(2)}</span>
                      </div>
                    </div>
                    
                    <div className="space-y-2">
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Progress</span>
                        <span className="text-sm font-medium">
                          {getProgressPercentage(order.executed, order.totalQuantity).toFixed(1)}%
                        </span>
                      </div>
                      <div className="w-full bg-gray-200 rounded-full h-2">
                        <div
                          className="bg-green-600 h-2 rounded-full"
                          style={{ width: `${getProgressPercentage(order.executed, order.totalQuantity)}%` }}
                        />
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Aggressiveness</span>
                        <Badge variant="outline">{order.aggressiveness}</Badge>
                      </div>
                    </div>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        </TabsContent>

        <TabsContent value="iceberg" className="space-y-4">
          <div className="space-y-4">
            {icebergOrders.map((order) => (
              <Card key={order.id}>
                <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                  <div className="flex items-center space-x-3">
                    <CardTitle className="text-sm font-medium">{order.id}</CardTitle>
                    <Badge className={getStatusColor(order.status)}>{order.status}</Badge>
                    <Badge variant={order.side === 'BUY' ? 'default' : 'destructive'}>
                      {order.side}
                    </Badge>
                  </div>
                  <div className="flex items-center space-x-2">
                    {order.status === 'ACTIVE' && (
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => handleOrderAction(order.id, 'PAUSE')}
                      >
                        <Pause className="h-4 w-4" />
                      </Button>
                    )}
                    {order.status === 'PAUSED' && (
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => handleOrderAction(order.id, 'RESUME')}
                      >
                        <Play className="h-4 w-4" />
                      </Button>
                    )}
                    <Button
                      size="sm"
                      variant="destructive"
                      onClick={() => handleOrderAction(order.id, 'CANCEL')}
                    >
                      <Square className="h-4 w-4" />
                    </Button>
                  </div>
                </CardHeader>
                <CardContent>
                  <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
                    <div className="space-y-2">
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Symbol</span>
                        <span className="text-sm font-medium">{order.symbol}</span>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Total Quantity</span>
                        <span className="text-sm font-medium">{order.totalQuantity.toLocaleString()}</span>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Visible Size</span>
                        <span className="text-sm font-medium">{order.visibleQuantity}</span>
                      </div>
                    </div>
                    
                    <div className="space-y-2">
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Hidden Quantity</span>
                        <span className="text-sm font-medium">{order.hiddenQuantity.toLocaleString()}</span>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Refresh Size</span>
                        <span className="text-sm font-medium">{order.refreshSize}</span>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Price Variance</span>
                        <span className="text-sm font-medium">{order.priceVariance} ticks</span>
                      </div>
                    </div>
                    
                    <div className="space-y-2">
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Executed</span>
                        <span className="text-sm font-medium">{order.executedQuantity.toLocaleString()}</span>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Remaining</span>
                        <span className="text-sm font-medium">{order.remainingQuantity.toLocaleString()}</span>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Avg Price</span>
                        <span className="text-sm font-medium">₹{order.averagePrice.toFixed(2)}</span>
                      </div>
                    </div>
                    
                    <div className="space-y-2">
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Progress</span>
                        <span className="text-sm font-medium">
                          {getProgressPercentage(order.executedQuantity, order.totalQuantity).toFixed(1)}%
                        </span>
                      </div>
                      <div className="w-full bg-gray-200 rounded-full h-2">
                        <div
                          className="bg-purple-600 h-2 rounded-full"
                          style={{ width: `${getProgressPercentage(order.executedQuantity, order.totalQuantity)}%` }}
                        />
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Market Impact</span>
                        <span className="text-sm font-medium text-green-600">Minimal</span>
                      </div>
                    </div>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        </TabsContent>

        <TabsContent value="pov" className="space-y-4">
          <div className="space-y-4">
            {povOrders.map((order) => (
              <Card key={order.id}>
                <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                  <div className="flex items-center space-x-3">
                    <CardTitle className="text-sm font-medium">{order.id}</CardTitle>
                    <Badge className={getStatusColor(order.status)}>{order.status}</Badge>
                    <Badge variant={order.side === 'BUY' ? 'default' : 'destructive'}>
                      {order.side}
                    </Badge>
                  </div>
                  <div className="flex items-center space-x-2">
                    {order.status === 'ACTIVE' && (
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => handleOrderAction(order.id, 'PAUSE')}
                      >
                        <Pause className="h-4 w-4" />
                      </Button>
                    )}
                    {order.status === 'PAUSED' && (
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => handleOrderAction(order.id, 'RESUME')}
                      >
                        <Play className="h-4 w-4" />
                      </Button>
                    )}
                    <Button
                      size="sm"
                      variant="destructive"
                      onClick={() => handleOrderAction(order.id, 'CANCEL')}
                    >
                      <Square className="h-4 w-4" />
                    </Button>
                  </div>
                </CardHeader>
                <CardContent>
                  <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
                    <div className="space-y-2">
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Symbol</span>
                        <span className="text-sm font-medium">{order.symbol}</span>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Total Quantity</span>
                        <span className="text-sm font-medium">{order.totalQuantity.toLocaleString()}</span>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Target %</span>
                        <span className="text-sm font-medium">{(order.targetPercentage * 100).toFixed(1)}%</span>
                      </div>
                    </div>
                    
                    <div className="space-y-2">
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Max %</span>
                        <span className="text-sm font-medium">{(order.maxPercentage * 100).toFixed(1)}%</span>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Min %</span>
                        <span className="text-sm font-medium">{(order.minPercentage * 100).toFixed(1)}%</span>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Market Impact Limit</span>
                        <span className="text-sm font-medium">{order.marketImpactLimit} bps</span>
                      </div>
                    </div>
                    
                    <div className="space-y-2">
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Executed</span>
                        <span className="text-sm font-medium">{order.executed.toLocaleString()}</span>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Remaining</span>
                        <span className="text-sm font-medium">{order.remaining.toLocaleString()}</span>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Avg Price</span>
                        <span className="text-sm font-medium">₹{order.averagePrice.toFixed(2)}</span>
                      </div>
                    </div>
                    
                    <div className="space-y-2">
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Progress</span>
                        <span className="text-sm font-medium">
                          {getProgressPercentage(order.executed, order.totalQuantity).toFixed(1)}%
                        </span>
                      </div>
                      <div className="w-full bg-gray-200 rounded-full h-2">
                        <div
                          className="bg-orange-600 h-2 rounded-full"
                          style={{ width: `${getProgressPercentage(order.executed, order.totalQuantity)}%` }}
                        />
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Aggressiveness</span>
                        <Badge variant="outline">{order.aggressiveness}</Badge>
                      </div>
                    </div>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        </TabsContent>

        <TabsContent value="create" className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center space-x-2">
                <Layers className="h-5 w-5" />
                <span>Create New Institutional Order</span>
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-6">
              {/* Order Type Selection */}
              <div className="grid grid-cols-4 gap-4">
                {['TWAP', 'VWAP', 'ICEBERG', 'POV'].map((type) => (
                  <Button
                    key={type}
                    variant={selectedOrderType === type ? 'default' : 'outline'}
                    onClick={() => setSelectedOrderType(type as any)}
                    className="h-16 flex flex-col items-center justify-center"
                  >
                    {type === 'TWAP' && <Timer className="h-6 w-6 mb-1" />}
                    {type === 'VWAP' && <BarChart3 className="h-6 w-6 mb-1" />}
                    {type === 'ICEBERG' && <Waves className="h-6 w-6 mb-1" />}
                    {type === 'POV' && <Gauge className="h-6 w-6 mb-1" />}
                    <span className="text-sm font-medium">{type}</span>
                  </Button>
                ))}
              </div>

              {/* Basic Order Parameters */}
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">Symbol</label>
                  <Input
                    value={newOrder.symbol}
                    onChange={(e) => setNewOrder({ ...newOrder, symbol: e.target.value })}
                    placeholder="Symbol"
                  />
                </div>
                
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">Side</label>
                  <select
                    value={newOrder.side}
                    onChange={(e) => setNewOrder({ ...newOrder, side: e.target.value as 'BUY' | 'SELL' })}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                  >
                    <option value="BUY">BUY</option>
                    <option value="SELL">SELL</option>
                  </select>
                </div>
                
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">Total Quantity</label>
                  <Input
                    type="number"
                    value={newOrder.totalQuantity}
                    onChange={(e) => setNewOrder({ ...newOrder, totalQuantity: parseInt(e.target.value) })}
                    placeholder="Total Quantity"
                  />
                </div>
              </div>

              {/* Order-specific Parameters */}
              {selectedOrderType === 'TWAP' && (
                <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">Duration (min)</label>
                    <Input
                      type="number"
                      value={newOrder.duration}
                      onChange={(e) => setNewOrder({ ...newOrder, duration: parseInt(e.target.value) })}
                      placeholder="Duration"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">Slice Size</label>
                    <Input
                      type="number"
                      value={newOrder.sliceSize}
                      onChange={(e) => setNewOrder({ ...newOrder, sliceSize: parseInt(e.target.value) })}
                      placeholder="Slice Size"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">Interval (sec)</label>
                    <Input
                      type="number"
                      value={newOrder.interval}
                      onChange={(e) => setNewOrder({ ...newOrder, interval: parseInt(e.target.value) })}
                      placeholder="Interval"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">Randomization</label>
                    <Input
                      type="number"
                      step="0.1"
                      max="1"
                      min="0"
                      value={newOrder.randomization}
                      onChange={(e) => setNewOrder({ ...newOrder, randomization: parseFloat(e.target.value) })}
                      placeholder="0.0-1.0"
                    />
                  </div>
                </div>
              )}

              {selectedOrderType === 'VWAP' && (
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">Participation Rate</label>
                    <Input
                      type="number"
                      step="0.01"
                      max="1"
                      min="0"
                      value={newOrder.participationRate}
                      onChange={(e) => setNewOrder({ ...newOrder, participationRate: parseFloat(e.target.value) })}
                      placeholder="0.0-1.0"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">Lookback Period (min)</label>
                    <Input
                      type="number"
                      value={newOrder.lookbackPeriod}
                      onChange={(e) => setNewOrder({ ...newOrder, lookbackPeriod: parseInt(e.target.value) })}
                      placeholder="Lookback Period"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">Aggressiveness</label>
                    <select
                      value={newOrder.aggressiveness}
                      onChange={(e) => setNewOrder({ ...newOrder, aggressiveness: e.target.value as any })}
                      className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                    >
                      <option value="PASSIVE">PASSIVE</option>
                      <option value="NEUTRAL">NEUTRAL</option>
                      <option value="AGGRESSIVE">AGGRESSIVE</option>
                    </select>
                  </div>
                </div>
              )}

              {selectedOrderType === 'ICEBERG' && (
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">Visible Quantity</label>
                    <Input
                      type="number"
                      value={newOrder.visibleQuantity}
                      onChange={(e) => setNewOrder({ ...newOrder, visibleQuantity: parseInt(e.target.value) })}
                      placeholder="Visible Quantity"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">Refresh Size</label>
                    <Input
                      type="number"
                      value={newOrder.refreshSize}
                      onChange={(e) => setNewOrder({ ...newOrder, refreshSize: parseInt(e.target.value) })}
                      placeholder="Refresh Size"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">Price Variance (ticks)</label>
                    <Input
                      type="number"
                      value={newOrder.priceVariance}
                      onChange={(e) => setNewOrder({ ...newOrder, priceVariance: parseInt(e.target.value) })}
                      placeholder="Price Variance"
                    />
                  </div>
                </div>
              )}

              {selectedOrderType === 'POV' && (
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">Target %</label>
                    <Input
                      type="number"
                      step="0.01"
                      max="1"
                      min="0"
                      value={newOrder.targetPercentage}
                      onChange={(e) => setNewOrder({ ...newOrder, targetPercentage: parseFloat(e.target.value) })}
                      placeholder="0.0-1.0"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">Max %</label>
                    <Input
                      type="number"
                      step="0.01"
                      max="1"
                      min="0"
                      value={newOrder.maxPercentage}
                      onChange={(e) => setNewOrder({ ...newOrder, maxPercentage: parseFloat(e.target.value) })}
                      placeholder="0.0-1.0"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">Min %</label>
                    <Input
                      type="number"
                      step="0.01"
                      max="1"
                      min="0"
                      value={newOrder.minPercentage}
                      onChange={(e) => setNewOrder({ ...newOrder, minPercentage: parseFloat(e.target.value) })}
                      placeholder="0.0-1.0"
                    />
                  </div>
                </div>
              )}

              <div className="flex justify-end space-x-4">
                <Button variant="outline">
                  Cancel
                </Button>
                <Button onClick={createNewOrder} className="flex items-center space-x-2">
                  <Layers className="h-4 w-4" />
                  <span>Create {selectedOrderType} Order</span>
                </Button>
              </div>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  );
};

export default InstitutionalOrderTypes;