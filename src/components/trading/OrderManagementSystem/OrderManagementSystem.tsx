import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Play, Pause, Square, AlertTriangle, CheckCircle, Clock, 
  TrendingUp, TrendingDown, Filter, Search, Settings,
  MoreVertical, Edit, Trash2, Copy, Eye, BarChart3,
  RefreshCw, Download, Upload, Bell, Target, Zap,
  ArrowUpDown, ArrowUp, ArrowDown, Activity, Users
} from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Input } from '@/components/ui/input';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';

interface OrderBook {
  symbol: string;
  bids: { price: number; quantity: number; count: number }[];
  asks: { price: number; quantity: number; count: number }[];
  lastTrade: { price: number; quantity: number; timestamp: Date };
  spread: number;
  midPrice: number;
}

interface Order {
  id: string;
  clientOrderId: string;
  symbol: string;
  side: 'BUY' | 'SELL';
  orderType: 'MARKET' | 'LIMIT' | 'STOP_LOSS' | 'STOP_LIMIT' | 'TWAP' | 'VWAP' | 'ICEBERG';
  quantity: number;
  price?: number;
  stopPrice?: number;
  averagePrice?: number;
  filledQuantity: number;
  remainingQuantity: number;
  status: 'PENDING' | 'WORKING' | 'PARTIALLY_FILLED' | 'FILLED' | 'CANCELLED' | 'REJECTED' | 'EXPIRED';
  timeInForce: 'DAY' | 'GTC' | 'IOC' | 'FOK';
  createdAt: Date;
  updatedAt: Date;
  brokerId: string;
  brokerName: string;
  venue?: string;
  executionAlgorithm?: string;
  slippage?: number;
  commission?: number;
  urgency: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
  tags: string[];
  parentOrderId?: string;
  childOrders?: string[];
}

interface ExecutionMetrics {
  totalOrders: number;
  filledOrders: number;
  fillRate: number;
  averageSlippage: number;
  averageExecutionTime: number;
  totalVolume: number;
  totalCommission: number;
  bestExecution: {
    savings: number;
    improvementRate: number;
  };
  venueBreakdown: { venue: string; percentage: number; avgSlippage: number }[];
}

interface OrderManagementProps {
  userId: string;
  onOrderUpdate?: (order: Order) => void;
  onExecutionMetrics?: (metrics: ExecutionMetrics) => void;
}

export const OrderManagementSystem: React.FC<OrderManagementProps> = ({
  userId,
  onOrderUpdate,
  onExecutionMetrics
}) => {
  const [orders, setOrders] = useState<Order[]>([]);
  const [orderBooks, setOrderBooks] = useState<Record<string, OrderBook>>({});
  const [executionMetrics, setExecutionMetrics] = useState<ExecutionMetrics | null>(null);
  const [selectedOrders, setSelectedOrders] = useState<string[]>([]);
  const [filterStatus, setFilterStatus] = useState<string>('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [sortBy, setSortBy] = useState<'time' | 'symbol' | 'quantity' | 'price'>('time');
  const [sortDirection, setSortDirection] = useState<'asc' | 'desc'>('desc');
  const [showAdvancedFilters, setShowAdvancedFilters] = useState(false);
  const [realTimeUpdates, setRealTimeUpdates] = useState(true);
  const [activeTab, setActiveTab] = useState<'orders' | 'orderbook' | 'analytics' | 'alerts'>('orders');

  // Initialize with mock data
  useEffect(() => {
    initializeMockData();
    if (realTimeUpdates) {
      const interval = setInterval(simulateRealTimeUpdates, 1000);
      return () => clearInterval(interval);
    }
  }, [realTimeUpdates]);

  const initializeMockData = () => {
    const mockOrders: Order[] = [
      {
        id: 'ORD-001',
        clientOrderId: 'CLIENT-001',
        symbol: 'RELIANCE',
        side: 'BUY',
        orderType: 'LIMIT',
        quantity: 100,
        price: 2450.50,
        filledQuantity: 60,
        remainingQuantity: 40,
        status: 'PARTIALLY_FILLED',
        timeInForce: 'GTC',
        createdAt: new Date(Date.now() - 300000),
        updatedAt: new Date(Date.now() - 120000),
        brokerId: 'ZERODHA',
        brokerName: 'Zerodha',
        venue: 'NSE',
        executionAlgorithm: 'SMART',
        slippage: 0.02,
        commission: 15.50,
        urgency: 'MEDIUM',
        tags: ['swing-trade', 'nifty-50'],
        averagePrice: 2449.75
      },
      {
        id: 'ORD-002',
        clientOrderId: 'CLIENT-002',
        symbol: 'TCS',
        side: 'SELL',
        orderType: 'TWAP',
        quantity: 200,
        filledQuantity: 0,
        remainingQuantity: 200,
        status: 'WORKING',
        timeInForce: 'DAY',
        createdAt: new Date(Date.now() - 180000),
        updatedAt: new Date(Date.now() - 60000),
        brokerId: 'UPSTOX',
        brokerName: 'Upstox',
        venue: 'BSE',
        executionAlgorithm: 'TWAP',
        urgency: 'HIGH',
        tags: ['algorithmic', 'tech-sector']
      },
      {
        id: 'ORD-003',
        clientOrderId: 'CLIENT-003',
        symbol: 'INFY',
        side: 'BUY',
        orderType: 'ICEBERG',
        quantity: 500,
        price: 1456.25,
        filledQuantity: 150,
        remainingQuantity: 350,
        status: 'WORKING',
        timeInForce: 'GTC',
        createdAt: new Date(Date.now() - 900000),
        updatedAt: new Date(Date.now() - 30000),
        brokerId: 'ANGEL',
        brokerName: 'Angel One',
        venue: 'NSE',
        executionAlgorithm: 'ICEBERG',
        urgency: 'LOW',
        tags: ['large-order', 'tech-sector'],
        averagePrice: 1455.80
      }
    ];

    const mockOrderBooks: Record<string, OrderBook> = {
      'RELIANCE': {
        symbol: 'RELIANCE',
        bids: [
          { price: 2449.50, quantity: 150, count: 8 },
          { price: 2449.00, quantity: 230, count: 12 },
          { price: 2448.75, quantity: 180, count: 9 },
          { price: 2448.50, quantity: 300, count: 15 },
          { price: 2448.25, quantity: 120, count: 6 }
        ],
        asks: [
          { price: 2450.00, quantity: 100, count: 5 },
          { price: 2450.25, quantity: 180, count: 9 },
          { price: 2450.50, quantity: 220, count: 11 },
          { price: 2450.75, quantity: 160, count: 8 },
          { price: 2451.00, quantity: 200, count: 10 }
        ],
        lastTrade: { price: 2449.75, quantity: 50, timestamp: new Date() },
        spread: 0.50,
        midPrice: 2449.75
      }
    };

    const mockMetrics: ExecutionMetrics = {
      totalOrders: 15,
      filledOrders: 8,
      fillRate: 0.533,
      averageSlippage: 0.015,
      averageExecutionTime: 2.3,
      totalVolume: 12500000,
      totalCommission: 1250,
      bestExecution: {
        savings: 15750,
        improvementRate: 0.125
      },
      venueBreakdown: [
        { venue: 'NSE', percentage: 65, avgSlippage: 0.012 },
        { venue: 'BSE', percentage: 25, avgSlippage: 0.018 },
        { venue: 'MCX', percentage: 10, avgSlippage: 0.022 }
      ]
    };

    setOrders(mockOrders);
    setOrderBooks(mockOrderBooks);
    setExecutionMetrics(mockMetrics);
  };

  const simulateRealTimeUpdates = () => {
    setOrders(prevOrders => 
      prevOrders.map(order => {
        if (order.status === 'WORKING' || order.status === 'PARTIALLY_FILLED') {
          const shouldUpdate = Math.random() < 0.3;
          if (shouldUpdate) {
            const additionalFill = Math.min(
              Math.floor(Math.random() * 20 + 1),
              order.remainingQuantity
            );
            
            const newFilledQuantity = order.filledQuantity + additionalFill;
            const newRemainingQuantity = order.quantity - newFilledQuantity;
            const newStatus = newRemainingQuantity === 0 ? 'FILLED' : 'PARTIALLY_FILLED';
            
            return {
              ...order,
              filledQuantity: newFilledQuantity,
              remainingQuantity: newRemainingQuantity,
              status: newStatus,
              updatedAt: new Date()
            };
          }
        }
        return order;
      })
    );
  };

  const filteredAndSortedOrders = useMemo(() => {
    let filtered = orders;

    // Apply status filter
    if (filterStatus !== 'ALL') {
      filtered = filtered.filter(order => order.status === filterStatus);
    }

    // Apply search filter
    if (searchQuery) {
      filtered = filtered.filter(order =>
        order.symbol.toLowerCase().includes(searchQuery.toLowerCase()) ||
        order.id.toLowerCase().includes(searchQuery.toLowerCase()) ||
        order.brokerName.toLowerCase().includes(searchQuery.toLowerCase())
      );
    }

    // Apply sorting
    return filtered.sort((a, b) => {
      let comparison = 0;
      
      switch (sortBy) {
        case 'time':
          comparison = a.createdAt.getTime() - b.createdAt.getTime();
          break;
        case 'symbol':
          comparison = a.symbol.localeCompare(b.symbol);
          break;
        case 'quantity':
          comparison = a.quantity - b.quantity;
          break;
        case 'price':
          comparison = (a.price || 0) - (b.price || 0);
          break;
        default:
          return 0;
      }
      
      return sortDirection === 'asc' ? comparison : -comparison;
    });
  }, [orders, filterStatus, searchQuery, sortBy, sortDirection]);

  const handleBulkAction = (action: 'cancel' | 'modify' | 'clone') => {
    if (selectedOrders.length === 0) return;
    
    switch (action) {
      case 'cancel':
        setOrders(prevOrders =>
          prevOrders.map(order =>
            selectedOrders.includes(order.id) && 
            ['WORKING', 'PARTIALLY_FILLED', 'PENDING'].includes(order.status)
              ? { ...order, status: 'CANCELLED', updatedAt: new Date() }
              : order
          )
        );
        break;
      case 'clone':
        const ordersToClone = orders.filter(order => selectedOrders.includes(order.id));
        const clonedOrders = ordersToClone.map(order => ({
          ...order,
          id: `${order.id}-CLONE-${Date.now()}`,
          clientOrderId: `${order.clientOrderId}-CLONE`,
          status: 'PENDING' as const,
          filledQuantity: 0,
          remainingQuantity: order.quantity,
          createdAt: new Date(),
          updatedAt: new Date()
        }));
        setOrders(prevOrders => [...prevOrders, ...clonedOrders]);
        break;
    }
    
    setSelectedOrders([]);
  };

  const getStatusColor = (status: Order['status']) => {
    switch (status) {
      case 'FILLED': return 'bg-green-100 text-green-800 border-green-200';
      case 'PARTIALLY_FILLED': return 'bg-blue-100 text-blue-800 border-blue-200';
      case 'WORKING': return 'bg-yellow-100 text-yellow-800 border-yellow-200';
      case 'PENDING': return 'bg-gray-100 text-gray-800 border-gray-200';
      case 'CANCELLED': return 'bg-red-100 text-red-800 border-red-200';
      case 'REJECTED': return 'bg-red-100 text-red-800 border-red-200';
      case 'EXPIRED': return 'bg-orange-100 text-orange-800 border-orange-200';
      default: return 'bg-gray-100 text-gray-800 border-gray-200';
    }
  };

  const getUrgencyColor = (urgency: Order['urgency']) => {
    switch (urgency) {
      case 'URGENT': return 'bg-red-500';
      case 'HIGH': return 'bg-orange-500';
      case 'MEDIUM': return 'bg-yellow-500';
      case 'LOW': return 'bg-green-500';
      default: return 'bg-gray-500';
    }
  };

  return (
    <div className="p-6 space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Order Management System</h1>
          <p className="text-gray-600">Advanced order execution with real-time monitoring</p>
        </div>
        <div className="flex items-center space-x-3">
          <Button
            variant={realTimeUpdates ? "default" : "outline"}
            onClick={() => setRealTimeUpdates(!realTimeUpdates)}
            className="flex items-center space-x-2"
          >
            {realTimeUpdates ? <Pause className="h-4 w-4" /> : <Play className="h-4 w-4" />}
            <span>{realTimeUpdates ? 'Pause' : 'Resume'} Updates</span>
          </Button>
          <Button className="flex items-center space-x-2">
            <Download className="h-4 w-4" />
            <span>Export</span>
          </Button>
          <Button variant="outline" className="flex items-center space-x-2">
            <Settings className="h-4 w-4" />
            <span>Settings</span>
          </Button>
        </div>
      </div>

      {/* Quick Stats */}
      {executionMetrics && (
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          <Card>
            <CardContent className="p-6">
              <div className="flex items-center">
                <Activity className="h-8 w-8 text-blue-600" />
                <div className="ml-4">
                  <p className="text-sm font-medium text-gray-600">Fill Rate</p>
                  <p className="text-2xl font-bold text-gray-900">
                    {(executionMetrics.fillRate * 100).toFixed(1)}%
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
                  <p className="text-sm font-medium text-gray-600">Avg Slippage</p>
                  <p className="text-2xl font-bold text-gray-900">
                    {(executionMetrics.averageSlippage * 100).toFixed(2)}%
                  </p>
                </div>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardContent className="p-6">
              <div className="flex items-center">
                <Clock className="h-8 w-8 text-yellow-600" />
                <div className="ml-4">
                  <p className="text-sm font-medium text-gray-600">Avg Execution</p>
                  <p className="text-2xl font-bold text-gray-900">
                    {executionMetrics.averageExecutionTime.toFixed(1)}s
                  </p>
                </div>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardContent className="p-6">
              <div className="flex items-center">
                <TrendingUp className="h-8 w-8 text-purple-600" />
                <div className="ml-4">
                  <p className="text-sm font-medium text-gray-600">Best Execution</p>
                  <p className="text-2xl font-bold text-gray-900">
                    ₹{executionMetrics.bestExecution.savings.toLocaleString()}
                  </p>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      )}

      {/* Main Content */}
      <Tabs value={activeTab} onValueChange={(value) => setActiveTab(value as any)}>
        <TabsList className="grid w-full grid-cols-4">
          <TabsTrigger value="orders" className="flex items-center space-x-2">
            <Activity className="h-4 w-4" />
            <span>Orders</span>
          </TabsTrigger>
          <TabsTrigger value="orderbook" className="flex items-center space-x-2">
            <BarChart3 className="h-4 w-4" />
            <span>Order Book</span>
          </TabsTrigger>
          <TabsTrigger value="analytics" className="flex items-center space-x-2">
            <TrendingUp className="h-4 w-4" />
            <span>Analytics</span>
          </TabsTrigger>
          <TabsTrigger value="alerts" className="flex items-center space-x-2">
            <Bell className="h-4 w-4" />
            <span>Alerts</span>
          </TabsTrigger>
        </TabsList>

        <TabsContent value="orders" className="space-y-4">
          {/* Filters and Actions */}
          <Card>
            <CardContent className="p-6">
              <div className="flex flex-wrap items-center justify-between gap-4">
                <div className="flex items-center space-x-4">
                  <div className="relative">
                    <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 h-4 w-4" />
                    <Input
                      placeholder="Search orders..."
                      value={searchQuery}
                      onChange={(e) => setSearchQuery(e.target.value)}
                      className="pl-10 w-64"
                    />
                  </div>
                  
                  <select
                    value={filterStatus}
                    onChange={(e) => setFilterStatus(e.target.value)}
                    className="px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                  >
                    <option value="ALL">All Status</option>
                    <option value="WORKING">Working</option>
                    <option value="FILLED">Filled</option>
                    <option value="PARTIALLY_FILLED">Partially Filled</option>
                    <option value="CANCELLED">Cancelled</option>
                    <option value="PENDING">Pending</option>
                  </select>

                  <Button
                    variant="outline"
                    onClick={() => setSortDirection(sortDirection === 'asc' ? 'desc' : 'asc')}
                    className="flex items-center space-x-2"
                  >
                    {sortDirection === 'asc' ? <ArrowUp className="h-4 w-4" /> : <ArrowDown className="h-4 w-4" />}
                    <span>Sort</span>
                  </Button>
                </div>

                {selectedOrders.length > 0 && (
                  <div className="flex items-center space-x-2">
                    <Badge variant="outline" className="px-3 py-1">
                      {selectedOrders.length} selected
                    </Badge>
                    <Button
                      size="sm"
                      variant="destructive"
                      onClick={() => handleBulkAction('cancel')}
                      className="flex items-center space-x-1"
                    >
                      <Square className="h-4 w-4" />
                      <span>Cancel</span>
                    </Button>
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={() => handleBulkAction('clone')}
                      className="flex items-center space-x-1"
                    >
                      <Copy className="h-4 w-4" />
                      <span>Clone</span>
                    </Button>
                  </div>
                )}
              </div>
            </CardContent>
          </Card>

          {/* Orders Table */}
          <Card>
            <CardContent className="p-0">
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead className="bg-gray-50 border-b">
                    <tr>
                      <th className="px-6 py-3 text-left">
                        <input
                          type="checkbox"
                          onChange={(e) => {
                            if (e.target.checked) {
                              setSelectedOrders(filteredAndSortedOrders.map(order => order.id));
                            } else {
                              setSelectedOrders([]);
                            }
                          }}
                          checked={selectedOrders.length === filteredAndSortedOrders.length}
                        />
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                        Symbol
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                        Side/Type
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                        Quantity
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                        Price
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                        Status
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                        Venue
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                        Progress
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                        Actions
                      </th>
                    </tr>
                  </thead>
                  <tbody className="bg-white divide-y divide-gray-200">
                    <AnimatePresence>
                      {filteredAndSortedOrders.map((order) => (
                        <motion.tr
                          key={order.id}
                          initial={{ opacity: 0, y: 10 }}
                          animate={{ opacity: 1, y: 0 }}
                          exit={{ opacity: 0, y: -10 }}
                          className={`hover:bg-gray-50 ${selectedOrders.includes(order.id) ? 'bg-blue-50' : ''}`}
                        >
                          <td className="px-6 py-4 whitespace-nowrap">
                            <input
                              type="checkbox"
                              checked={selectedOrders.includes(order.id)}
                              onChange={(e) => {
                                if (e.target.checked) {
                                  setSelectedOrders([...selectedOrders, order.id]);
                                } else {
                                  setSelectedOrders(selectedOrders.filter(id => id !== order.id));
                                }
                              }}
                            />
                          </td>
                          <td className="px-6 py-4 whitespace-nowrap">
                            <div className="flex items-center">
                              <div className={`w-2 h-2 rounded-full mr-2 ${getUrgencyColor(order.urgency)}`} />
                              <div>
                                <div className="text-sm font-medium text-gray-900">{order.symbol}</div>
                                <div className="text-sm text-gray-500">{order.id}</div>
                              </div>
                            </div>
                          </td>
                          <td className="px-6 py-4 whitespace-nowrap">
                            <div className="flex items-center space-x-2">
                              {order.side === 'BUY' ? 
                                <TrendingUp className="h-4 w-4 text-green-600" /> : 
                                <TrendingDown className="h-4 w-4 text-red-600" />
                              }
                              <div>
                                <div className={`text-sm font-medium ${order.side === 'BUY' ? 'text-green-600' : 'text-red-600'}`}>
                                  {order.side}
                                </div>
                                <div className="text-xs text-gray-500">{order.orderType}</div>
                              </div>
                            </div>
                          </td>
                          <td className="px-6 py-4 whitespace-nowrap">
                            <div className="text-sm text-gray-900">
                              {order.filledQuantity.toLocaleString()} / {order.quantity.toLocaleString()}
                            </div>
                            <div className="text-xs text-gray-500">
                              {((order.filledQuantity / order.quantity) * 100).toFixed(1)}% filled
                            </div>
                          </td>
                          <td className="px-6 py-4 whitespace-nowrap">
                            <div className="text-sm text-gray-900">
                              ₹{order.price?.toFixed(2) || 'Market'}
                            </div>
                            {order.averagePrice && (
                              <div className="text-xs text-gray-500">
                                Avg: ₹{order.averagePrice.toFixed(2)}
                              </div>
                            )}
                          </td>
                          <td className="px-6 py-4 whitespace-nowrap">
                            <Badge className={getStatusColor(order.status)}>
                              {order.status.replace('_', ' ')}
                            </Badge>
                          </td>
                          <td className="px-6 py-4 whitespace-nowrap">
                            <div className="text-sm text-gray-900">{order.venue || order.brokerName}</div>
                            {order.executionAlgorithm && (
                              <div className="text-xs text-gray-500">{order.executionAlgorithm}</div>
                            )}
                          </td>
                          <td className="px-6 py-4 whitespace-nowrap">
                            <div className="w-full bg-gray-200 rounded-full h-2">
                              <div
                                className="bg-blue-600 h-2 rounded-full"
                                style={{ width: `${(order.filledQuantity / order.quantity) * 100}%` }}
                              />
                            </div>
                          </td>
                          <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                            <div className="flex items-center space-x-2">
                              <Button size="sm" variant="ghost">
                                <Eye className="h-4 w-4" />
                              </Button>
                              <Button size="sm" variant="ghost">
                                <Edit className="h-4 w-4" />
                              </Button>
                              <Button size="sm" variant="ghost">
                                <MoreVertical className="h-4 w-4" />
                              </Button>
                            </div>
                          </td>
                        </motion.tr>
                      ))}
                    </AnimatePresence>
                  </tbody>
                </table>
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="orderbook" className="space-y-4">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {Object.entries(orderBooks).map(([symbol, orderBook]) => (
              <Card key={symbol}>
                <CardHeader>
                  <CardTitle className="flex items-center justify-between">
                    <span>{symbol} Order Book</span>
                    <Badge className="bg-green-100 text-green-800">
                      ₹{orderBook.lastTrade.price.toFixed(2)}
                    </Badge>
                  </CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="grid grid-cols-2 gap-4">
                    {/* Bids */}
                    <div>
                      <h4 className="font-medium text-green-600 mb-2">Bids</h4>
                      <div className="space-y-1">
                        {orderBook.bids.map((bid, index) => (
                          <div key={index} className="flex justify-between text-sm">
                            <span className="text-green-600">₹{bid.price.toFixed(2)}</span>
                            <span className="text-gray-600">{bid.quantity}</span>
                            <span className="text-gray-400">({bid.count})</span>
                          </div>
                        ))}
                      </div>
                    </div>
                    
                    {/* Asks */}
                    <div>
                      <h4 className="font-medium text-red-600 mb-2">Asks</h4>
                      <div className="space-y-1">
                        {orderBook.asks.map((ask, index) => (
                          <div key={index} className="flex justify-between text-sm">
                            <span className="text-red-600">₹{ask.price.toFixed(2)}</span>
                            <span className="text-gray-600">{ask.quantity}</span>
                            <span className="text-gray-400">({ask.count})</span>
                          </div>
                        ))}
                      </div>
                    </div>
                  </div>
                  
                  <div className="mt-4 pt-4 border-t">
                    <div className="grid grid-cols-2 gap-4 text-sm">
                      <div>
                        <span className="text-gray-500">Spread:</span>
                        <span className="ml-2 font-medium">₹{orderBook.spread.toFixed(2)}</span>
                      </div>
                      <div>
                        <span className="text-gray-500">Mid Price:</span>
                        <span className="ml-2 font-medium">₹{orderBook.midPrice.toFixed(2)}</span>
                      </div>
                    </div>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        </TabsContent>

        <TabsContent value="analytics" className="space-y-6">
          {executionMetrics && (
            <>
              <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                <Card>
                  <CardHeader>
                    <CardTitle>Venue Performance</CardTitle>
                  </CardHeader>
                  <CardContent>
                    <div className="space-y-4">
                      {executionMetrics.venueBreakdown.map((venue, index) => (
                        <div key={venue.venue} className="flex items-center justify-between">
                          <div className="flex items-center space-x-3">
                            <div className="w-8 h-8 bg-blue-100 rounded-full flex items-center justify-center">
                              <span className="text-sm font-medium text-blue-600">
                                {venue.venue.charAt(0)}
                              </span>
                            </div>
                            <div>
                              <div className="font-medium">{venue.venue}</div>
                              <div className="text-sm text-gray-500">
                                Slippage: {(venue.avgSlippage * 100).toFixed(2)}%
                              </div>
                            </div>
                          </div>
                          <div className="text-right">
                            <div className="font-medium">{venue.percentage}%</div>
                            <div className="w-20 bg-gray-200 rounded-full h-2">
                              <div
                                className="bg-blue-600 h-2 rounded-full"
                                style={{ width: `${venue.percentage}%` }}
                              />
                            </div>
                          </div>
                        </div>
                      ))}
                    </div>
                  </CardContent>
                </Card>

                <Card>
                  <CardHeader>
                    <CardTitle>Best Execution Savings</CardTitle>
                  </CardHeader>
                  <CardContent>
                    <div className="text-center py-8">
                      <div className="text-4xl font-bold text-green-600 mb-2">
                        ₹{executionMetrics.bestExecution.savings.toLocaleString()}
                      </div>
                      <div className="text-gray-600 mb-4">Total savings this month</div>
                      <div className="flex items-center justify-center space-x-2 text-sm">
                        <TrendingUp className="h-4 w-4 text-green-600" />
                        <span className="text-green-600 font-medium">
                          {(executionMetrics.bestExecution.improvementRate * 100).toFixed(1)}%
                        </span>
                        <span className="text-gray-500">improvement rate</span>
                      </div>
                    </div>
                  </CardContent>
                </Card>
              </div>
            </>
          )}
        </TabsContent>

        <TabsContent value="alerts" className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center space-x-2">
                <Bell className="h-5 w-5" />
                <span>Order Alerts</span>
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                <div className="flex items-center space-x-4 p-4 border border-yellow-200 bg-yellow-50 rounded-lg">
                  <AlertTriangle className="h-5 w-5 text-yellow-600" />
                  <div className="flex-1">
                    <div className="font-medium text-yellow-800">High Slippage Alert</div>
                    <div className="text-sm text-yellow-600">
                      Order ORD-004 experienced 0.8% slippage on INFY trade
                    </div>
                  </div>
                  <div className="text-sm text-yellow-600">2 min ago</div>
                </div>

                <div className="flex items-center space-x-4 p-4 border border-green-200 bg-green-50 rounded-lg">
                  <CheckCircle className="h-5 w-5 text-green-600" />
                  <div className="flex-1">
                    <div className="font-medium text-green-800">Order Filled</div>
                    <div className="text-sm text-green-600">
                      TWAP order for TCS completed successfully
                    </div>
                  </div>
                  <div className="text-sm text-green-600">5 min ago</div>
                </div>

                <div className="flex items-center space-x-4 p-4 border border-blue-200 bg-blue-50 rounded-lg">
                  <Activity className="h-5 w-5 text-blue-600" />
                  <div className="flex-1">
                    <div className="font-medium text-blue-800">Market Impact Alert</div>
                    <div className="text-sm text-blue-600">
                      Large order detected for RELIANCE - consider ICEBERG execution
                    </div>
                  </div>
                  <div className="text-sm text-blue-600">8 min ago</div>
                </div>
              </div>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  );
};

export default OrderManagementSystem;