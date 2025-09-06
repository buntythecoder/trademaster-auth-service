import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Zap, Target, BarChart3, Clock, TrendingUp, Settings,
  Route, Shuffle, Brain, Activity, AlertTriangle,
  CheckCircle, ArrowRight, RefreshCw, Play, Pause,
  Filter, Eye, MoreVertical, Layers, Network,
  Gauge, Sliders, MapPin, Atom
} from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';

interface Venue {
  id: string;
  name: string;
  type: 'EXCHANGE' | 'DARK_POOL' | 'ECN' | 'MARKET_MAKER';
  latency: number; // ms
  fillRate: number; // percentage
  averageSlippage: number; // basis points
  liquidityScore: number; // 0-100
  cost: number; // bps
  isActive: boolean;
  marketShare: number; // percentage
  connectivity: 'DIRECT' | 'FIX' | 'API';
  lastUpdate: Date;
}

interface RoutingAlgorithm {
  id: string;
  name: string;
  description: string;
  type: 'SMART' | 'VWAP' | 'TWAP' | 'IMPLEMENTATION_SHORTFALL' | 'ARRIVAL_PRICE' | 'LIQUIDITY_SEEKING';
  parameters: Record<string, any>;
  performance: {
    averageSlippage: number;
    fillRate: number;
    executionTime: number;
    costSavings: number;
  };
  isActive: boolean;
  priority: number;
}

interface RoutingRule {
  id: string;
  name: string;
  condition: {
    symbol?: string;
    orderSize?: { min?: number; max?: number };
    volatility?: { min?: number; max?: number };
    spread?: { max?: number };
    timeOfDay?: { start?: string; end?: string };
    marketCondition?: 'NORMAL' | 'VOLATILE' | 'QUIET';
  };
  action: {
    algorithm: string;
    venueSelection: string[];
    maxSlicing: number;
    timeLimit: number;
  };
  priority: number;
  isActive: boolean;
}

interface RoutingDecision {
  orderId: string;
  symbol: string;
  quantity: number;
  algorithm: string;
  venues: Array<{
    venue: string;
    percentage: number;
    expectedSlippage: number;
    estimatedFill: number;
  }>;
  reasoning: string[];
  confidence: number;
  estimatedCost: number;
  estimatedTime: number;
  riskScore: number;
}

interface SmartOrderRoutingProps {
  userId: string;
  onRoutingDecision?: (decision: RoutingDecision) => void;
  onAlgorithmUpdate?: (algorithm: RoutingAlgorithm) => void;
}

export const SmartOrderRouting: React.FC<SmartOrderRoutingProps> = ({
  userId,
  onRoutingDecision,
  onAlgorithmUpdate
}) => {
  const [venues, setVenues] = useState<Venue[]>([]);
  const [algorithms, setAlgorithms] = useState<RoutingAlgorithm[]>([]);
  const [routingRules, setRoutingRules] = useState<RoutingRule[]>([]);
  const [recentDecisions, setRecentDecisions] = useState<RoutingDecision[]>([]);
  const [activeTab, setActiveTab] = useState<'overview' | 'venues' | 'algorithms' | 'rules' | 'decisions'>('overview');
  const [isSimulating, setIsSimulating] = useState(false);
  const [selectedAlgorithm, setSelectedAlgorithm] = useState<string>('SMART');
  const [routingMetrics, setRoutingMetrics] = useState({
    totalRouted: 15420,
    avgSlippageSavings: 0.15,
    bestExecutionRate: 0.94,
    avgExecutionTime: 2.3,
    venueSpread: 12,
    algorithmEfficiency: 0.87
  });

  // Initialize with mock data
  useEffect(() => {
    initializeMockData();
    if (isSimulating) {
      const interval = setInterval(simulateRouting, 3000);
      return () => clearInterval(interval);
    }
  }, [isSimulating]);

  const initializeMockData = () => {
    const mockVenues: Venue[] = [
      {
        id: 'NSE',
        name: 'National Stock Exchange',
        type: 'EXCHANGE',
        latency: 0.8,
        fillRate: 0.95,
        averageSlippage: 2.5,
        liquidityScore: 95,
        cost: 1.2,
        isActive: true,
        marketShare: 45,
        connectivity: 'DIRECT',
        lastUpdate: new Date()
      },
      {
        id: 'BSE',
        name: 'Bombay Stock Exchange',
        type: 'EXCHANGE',
        latency: 1.2,
        fillRate: 0.88,
        averageSlippage: 3.2,
        liquidityScore: 82,
        cost: 1.5,
        isActive: true,
        marketShare: 25,
        connectivity: 'FIX',
        lastUpdate: new Date()
      },
      {
        id: 'CITADEL_DP',
        name: 'Citadel Dark Pool',
        type: 'DARK_POOL',
        latency: 2.1,
        fillRate: 0.75,
        averageSlippage: 1.8,
        liquidityScore: 70,
        cost: 0.8,
        isActive: true,
        marketShare: 15,
        connectivity: 'API',
        lastUpdate: new Date()
      },
      {
        id: 'GOLDMAN_MM',
        name: 'Goldman Sachs MM',
        type: 'MARKET_MAKER',
        latency: 1.5,
        fillRate: 0.92,
        averageSlippage: 2.0,
        liquidityScore: 85,
        cost: 1.0,
        isActive: true,
        marketShare: 10,
        connectivity: 'DIRECT',
        lastUpdate: new Date()
      },
      {
        id: 'INSTINET_ECN',
        name: 'Instinet ECN',
        type: 'ECN',
        latency: 3.2,
        fillRate: 0.80,
        averageSlippage: 2.8,
        liquidityScore: 65,
        cost: 1.3,
        isActive: false,
        marketShare: 5,
        connectivity: 'FIX',
        lastUpdate: new Date(Date.now() - 3600000)
      }
    ];

    const mockAlgorithms: RoutingAlgorithm[] = [
      {
        id: 'SMART',
        name: 'Smart Order Router',
        description: 'AI-powered routing with real-time market analysis',
        type: 'SMART',
        parameters: {
          maxVenues: 5,
          slippageThreshold: 0.05,
          timeHorizon: 300,
          riskTolerance: 'MEDIUM'
        },
        performance: {
          averageSlippage: 1.8,
          fillRate: 0.94,
          executionTime: 2.1,
          costSavings: 0.23
        },
        isActive: true,
        priority: 1
      },
      {
        id: 'VWAP',
        name: 'Volume Weighted Average Price',
        description: 'Matches historical volume patterns',
        type: 'VWAP',
        parameters: {
          participationRate: 0.15,
          timeFrame: 900,
          minFillSize: 100,
          aggressiveness: 'MEDIUM'
        },
        performance: {
          averageSlippage: 2.2,
          fillRate: 0.91,
          executionTime: 12.5,
          costSavings: 0.18
        },
        isActive: true,
        priority: 2
      },
      {
        id: 'TWAP',
        name: 'Time Weighted Average Price',
        description: 'Spreads order evenly over time',
        type: 'TWAP',
        parameters: {
          duration: 1800,
          sliceSize: 50,
          minInterval: 30,
          randomization: 0.2
        },
        performance: {
          averageSlippage: 2.5,
          fillRate: 0.89,
          executionTime: 18.2,
          costSavings: 0.15
        },
        isActive: true,
        priority: 3
      },
      {
        id: 'IMPLEMENTATION_SHORTFALL',
        name: 'Implementation Shortfall',
        description: 'Minimizes implementation shortfall cost',
        type: 'IMPLEMENTATION_SHORTFALL',
        parameters: {
          riskAversion: 0.5,
          volatility: 0.25,
          permanentImpact: 0.1,
          temporaryImpact: 0.05
        },
        performance: {
          averageSlippage: 1.9,
          fillRate: 0.93,
          executionTime: 8.7,
          costSavings: 0.21
        },
        isActive: true,
        priority: 4
      }
    ];

    const mockRules: RoutingRule[] = [
      {
        id: 'LARGE_ORDER_RULE',
        name: 'Large Order Dark Pool Route',
        condition: {
          orderSize: { min: 10000 },
          spread: { max: 0.05 }
        },
        action: {
          algorithm: 'SMART',
          venueSelection: ['CITADEL_DP', 'NSE'],
          maxSlicing: 10,
          timeLimit: 1800
        },
        priority: 1,
        isActive: true
      },
      {
        id: 'VOLATILE_MARKET_RULE',
        name: 'Volatile Market Protection',
        condition: {
          volatility: { min: 0.3 },
          marketCondition: 'VOLATILE'
        },
        action: {
          algorithm: 'IMPLEMENTATION_SHORTFALL',
          venueSelection: ['NSE', 'GOLDMAN_MM'],
          maxSlicing: 20,
          timeLimit: 600
        },
        priority: 2,
        isActive: true
      },
      {
        id: 'SMALL_ORDER_RULE',
        name: 'Small Order Fast Execution',
        condition: {
          orderSize: { max: 1000 },
          timeOfDay: { start: '09:15', end: '15:30' }
        },
        action: {
          algorithm: 'SMART',
          venueSelection: ['NSE', 'BSE'],
          maxSlicing: 1,
          timeLimit: 60
        },
        priority: 3,
        isActive: true
      }
    ];

    const mockDecisions: RoutingDecision[] = [
      {
        orderId: 'ORD-001',
        symbol: 'RELIANCE',
        quantity: 5000,
        algorithm: 'SMART',
        venues: [
          { venue: 'NSE', percentage: 60, expectedSlippage: 1.5, estimatedFill: 3000 },
          { venue: 'CITADEL_DP', percentage: 30, expectedSlippage: 0.8, estimatedFill: 1500 },
          { venue: 'GOLDMAN_MM', percentage: 10, expectedSlippage: 1.2, estimatedFill: 500 }
        ],
        reasoning: [
          'Large order size triggered dark pool routing',
          'NSE selected for primary liquidity',
          'Dark pool routing reduces market impact',
          'Goldman MM provides price improvement'
        ],
        confidence: 0.92,
        estimatedCost: 125.50,
        estimatedTime: 180,
        riskScore: 0.15
      },
      {
        orderId: 'ORD-002',
        symbol: 'TCS',
        quantity: 800,
        algorithm: 'VWAP',
        venues: [
          { venue: 'NSE', percentage: 70, expectedSlippage: 2.1, estimatedFill: 560 },
          { venue: 'BSE', percentage: 30, expectedSlippage: 2.8, estimatedFill: 240 }
        ],
        reasoning: [
          'VWAP selected for price-sensitive execution',
          'NSE provides best historical volume match',
          'BSE routing for completion assurance',
          'Medium volatility environment detected'
        ],
        confidence: 0.87,
        estimatedCost: 89.20,
        estimatedTime: 450,
        riskScore: 0.22
      }
    ];

    setVenues(mockVenues);
    setAlgorithms(mockAlgorithms);
    setRoutingRules(mockRules);
    setRecentDecisions(mockDecisions);
  };

  const simulateRouting = () => {
    const symbols = ['RELIANCE', 'TCS', 'INFY', 'HDFC', 'ICICI'];
    const algorithms = ['SMART', 'VWAP', 'TWAP'];
    
    const newDecision: RoutingDecision = {
      orderId: `ORD-${Math.random().toString(36).substr(2, 6).toUpperCase()}`,
      symbol: symbols[Math.floor(Math.random() * symbols.length)],
      quantity: Math.floor(Math.random() * 10000) + 100,
      algorithm: algorithms[Math.floor(Math.random() * algorithms.length)],
      venues: [
        {
          venue: 'NSE',
          percentage: Math.floor(Math.random() * 40) + 40,
          expectedSlippage: Math.random() * 2 + 1,
          estimatedFill: Math.floor(Math.random() * 5000) + 1000
        },
        {
          venue: 'CITADEL_DP',
          percentage: Math.floor(Math.random() * 30) + 10,
          expectedSlippage: Math.random() * 1.5 + 0.5,
          estimatedFill: Math.floor(Math.random() * 2000) + 500
        }
      ],
      reasoning: [
        'Real-time liquidity analysis performed',
        'Market impact minimization prioritized',
        'Optimal venue selection based on current conditions'
      ],
      confidence: Math.random() * 0.2 + 0.8,
      estimatedCost: Math.random() * 200 + 50,
      estimatedTime: Math.floor(Math.random() * 300) + 60,
      riskScore: Math.random() * 0.3 + 0.1
    };

    setRecentDecisions(prev => [newDecision, ...prev.slice(0, 9)]);
    onRoutingDecision?.(newDecision);
  };

  const getVenueTypeColor = (type: Venue['type']) => {
    switch (type) {
      case 'EXCHANGE': return 'bg-blue-100 text-blue-800';
      case 'DARK_POOL': return 'bg-purple-100 text-purple-800';
      case 'ECN': return 'bg-green-100 text-green-800';
      case 'MARKET_MAKER': return 'bg-yellow-100 text-yellow-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  };

  const getAlgorithmTypeColor = (type: RoutingAlgorithm['type']) => {
    switch (type) {
      case 'SMART': return 'bg-indigo-100 text-indigo-800';
      case 'VWAP': return 'bg-green-100 text-green-800';
      case 'TWAP': return 'bg-orange-100 text-orange-800';
      case 'IMPLEMENTATION_SHORTFALL': return 'bg-red-100 text-red-800';
      case 'ARRIVAL_PRICE': return 'bg-purple-100 text-purple-800';
      case 'LIQUIDITY_SEEKING': return 'bg-teal-100 text-teal-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  };

  const getRiskScoreColor = (risk: number) => {
    if (risk < 0.2) return 'bg-green-100 text-green-800';
    if (risk < 0.4) return 'bg-yellow-100 text-yellow-800';
    return 'bg-red-100 text-red-800';
  };

  return (
    <div className="p-6 space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Smart Order Routing</h1>
          <p className="text-gray-600">Intelligent order execution with AI-powered venue selection</p>
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
          <Button variant="outline" className="flex items-center space-x-2">
            <Settings className="h-4 w-4" />
            <span>Configure</span>
          </Button>
        </div>
      </div>

      {/* Key Metrics */}
      <div className="grid grid-cols-1 md:grid-cols-6 gap-4">
        <Card>
          <CardContent className="p-6">
            <div className="flex items-center">
              <Route className="h-8 w-8 text-blue-600" />
              <div className="ml-4">
                <p className="text-sm font-medium text-gray-600">Orders Routed</p>
                <p className="text-2xl font-bold text-gray-900">
                  {routingMetrics.totalRouted.toLocaleString()}
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
                <p className="text-sm font-medium text-gray-600">Slippage Savings</p>
                <p className="text-2xl font-bold text-gray-900">
                  {(routingMetrics.avgSlippageSavings * 100).toFixed(2)}%
                </p>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-6">
            <div className="flex items-center">
              <CheckCircle className="h-8 w-8 text-purple-600" />
              <div className="ml-4">
                <p className="text-sm font-medium text-gray-600">Best Execution</p>
                <p className="text-2xl font-bold text-gray-900">
                  {(routingMetrics.bestExecutionRate * 100).toFixed(0)}%
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
                  {routingMetrics.avgExecutionTime.toFixed(1)}s
                </p>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-6">
            <div className="flex items-center">
              <Network className="h-8 w-8 text-indigo-600" />
              <div className="ml-4">
                <p className="text-sm font-medium text-gray-600">Active Venues</p>
                <p className="text-2xl font-bold text-gray-900">
                  {routingMetrics.venueSpread}
                </p>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-6">
            <div className="flex items-center">
              <Brain className="h-8 w-8 text-red-600" />
              <div className="ml-4">
                <p className="text-sm font-medium text-gray-600">AI Efficiency</p>
                <p className="text-2xl font-bold text-gray-900">
                  {(routingMetrics.algorithmEfficiency * 100).toFixed(0)}%
                </p>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Main Content */}
      <Tabs value={activeTab} onValueChange={(value) => setActiveTab(value as any)}>
        <TabsList className="grid w-full grid-cols-5">
          <TabsTrigger value="overview" className="flex items-center space-x-2">
            <Activity className="h-4 w-4" />
            <span>Overview</span>
          </TabsTrigger>
          <TabsTrigger value="venues" className="flex items-center space-x-2">
            <MapPin className="h-4 w-4" />
            <span>Venues</span>
          </TabsTrigger>
          <TabsTrigger value="algorithms" className="flex items-center space-x-2">
            <Brain className="h-4 w-4" />
            <span>Algorithms</span>
          </TabsTrigger>
          <TabsTrigger value="rules" className="flex items-center space-x-2">
            <Filter className="h-4 w-4" />
            <span>Rules</span>
          </TabsTrigger>
          <TabsTrigger value="decisions" className="flex items-center space-x-2">
            <Eye className="h-4 w-4" />
            <span>Decisions</span>
          </TabsTrigger>
        </TabsList>

        <TabsContent value="overview" className="space-y-6">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Real-time Routing Flow */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center space-x-2">
                  <Route className="h-5 w-5" />
                  <span>Live Routing Flow</span>
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-4">
                  <div className="flex items-center justify-between p-4 border border-blue-200 bg-blue-50 rounded-lg">
                    <div className="flex items-center space-x-3">
                      <div className="w-3 h-3 bg-blue-600 rounded-full animate-pulse" />
                      <div>
                        <div className="font-medium">Order Analysis</div>
                        <div className="text-sm text-blue-600">Processing market conditions...</div>
                      </div>
                    </div>
                    <ArrowRight className="h-5 w-5 text-blue-600" />
                  </div>
                  
                  <div className="flex items-center justify-between p-4 border border-green-200 bg-green-50 rounded-lg">
                    <div className="flex items-center space-x-3">
                      <div className="w-3 h-3 bg-green-600 rounded-full" />
                      <div>
                        <div className="font-medium">Venue Selection</div>
                        <div className="text-sm text-green-600">NSE (60%) • Dark Pool (30%)</div>
                      </div>
                    </div>
                    <ArrowRight className="h-5 w-5 text-green-600" />
                  </div>
                  
                  <div className="flex items-center justify-between p-4 border border-purple-200 bg-purple-50 rounded-lg">
                    <div className="flex items-center space-x-3">
                      <div className="w-3 h-3 bg-purple-600 rounded-full" />
                      <div>
                        <div className="font-medium">Execution</div>
                        <div className="text-sm text-purple-600">SMART algorithm active</div>
                      </div>
                    </div>
                    <CheckCircle className="h-5 w-5 text-purple-600" />
                  </div>
                </div>
              </CardContent>
            </Card>

            {/* Performance Metrics */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center space-x-2">
                  <BarChart3 className="h-5 w-5" />
                  <span>Algorithm Performance</span>
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-4">
                  {algorithms.filter(alg => alg.isActive).map((algorithm) => (
                    <div key={algorithm.id} className="flex items-center justify-between">
                      <div className="flex items-center space-x-3">
                        <Badge className={getAlgorithmTypeColor(algorithm.type)}>
                          {algorithm.name}
                        </Badge>
                        <div className="text-sm text-gray-600">
                          {algorithm.performance.fillRate * 100}% fill rate
                        </div>
                      </div>
                      <div className="flex items-center space-x-2">
                        <div className="text-sm font-medium text-green-600">
                          {algorithm.performance.costSavings.toFixed(2)}% savings
                        </div>
                        <div className="w-20 bg-gray-200 rounded-full h-2">
                          <div
                            className="bg-green-600 h-2 rounded-full"
                            style={{ width: `${algorithm.performance.fillRate * 100}%` }}
                          />
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </CardContent>
            </Card>
          </div>

          {/* Recent Routing Decisions */}
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center space-x-2">
                <Eye className="h-5 w-5" />
                <span>Recent Routing Decisions</span>
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                {recentDecisions.slice(0, 3).map((decision) => (
                  <div key={decision.orderId} className="p-4 border border-gray-200 rounded-lg">
                    <div className="flex items-center justify-between mb-3">
                      <div className="flex items-center space-x-4">
                        <Badge variant="outline">{decision.orderId}</Badge>
                        <span className="font-medium">{decision.symbol}</span>
                        <span className="text-gray-600">{decision.quantity.toLocaleString()} shares</span>
                        <Badge className={getAlgorithmTypeColor('SMART')}>
                          {decision.algorithm}
                        </Badge>
                      </div>
                      <div className="flex items-center space-x-2">
                        <Badge className={getRiskScoreColor(decision.riskScore)}>
                          Risk: {(decision.riskScore * 100).toFixed(0)}%
                        </Badge>
                        <span className="text-sm text-gray-600">
                          Confidence: {(decision.confidence * 100).toFixed(0)}%
                        </span>
                      </div>
                    </div>
                    
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      <div>
                        <h4 className="font-medium text-gray-900 mb-2">Venue Allocation</h4>
                        <div className="space-y-2">
                          {decision.venues.map((venue, index) => (
                            <div key={index} className="flex items-center justify-between">
                              <span className="text-sm">{venue.venue}</span>
                              <div className="flex items-center space-x-2">
                                <span className="text-sm font-medium">{venue.percentage}%</span>
                                <div className="w-16 bg-gray-200 rounded-full h-1">
                                  <div
                                    className="bg-blue-600 h-1 rounded-full"
                                    style={{ width: `${venue.percentage}%` }}
                                  />
                                </div>
                              </div>
                            </div>
                          ))}
                        </div>
                      </div>
                      
                      <div>
                        <h4 className="font-medium text-gray-900 mb-2">Reasoning</h4>
                        <ul className="text-sm text-gray-600 space-y-1">
                          {decision.reasoning.slice(0, 3).map((reason, index) => (
                            <li key={index} className="flex items-start space-x-2">
                              <span className="text-blue-600">•</span>
                              <span>{reason}</span>
                            </li>
                          ))}
                        </ul>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="venues" className="space-y-4">
          <div className="grid grid-cols-1 lg:grid-cols-2 xl:grid-cols-3 gap-6">
            {venues.map((venue) => (
              <Card key={venue.id} className={!venue.isActive ? 'opacity-60' : ''}>
                <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                  <CardTitle className="text-sm font-medium">
                    {venue.name}
                  </CardTitle>
                  <Badge className={getVenueTypeColor(venue.type)}>
                    {venue.type.replace('_', ' ')}
                  </Badge>
                </CardHeader>
                <CardContent>
                  <div className="space-y-3">
                    <div className="flex items-center justify-between">
                      <span className="text-sm text-gray-600">Market Share</span>
                      <span className="text-sm font-medium">{venue.marketShare}%</span>
                    </div>
                    
                    <div className="flex items-center justify-between">
                      <span className="text-sm text-gray-600">Fill Rate</span>
                      <span className="text-sm font-medium">{(venue.fillRate * 100).toFixed(1)}%</span>
                    </div>
                    
                    <div className="flex items-center justify-between">
                      <span className="text-sm text-gray-600">Avg Slippage</span>
                      <span className="text-sm font-medium">{venue.averageSlippage.toFixed(1)} bps</span>
                    </div>
                    
                    <div className="flex items-center justify-between">
                      <span className="text-sm text-gray-600">Latency</span>
                      <span className="text-sm font-medium">{venue.latency.toFixed(1)} ms</span>
                    </div>
                    
                    <div className="flex items-center justify-between">
                      <span className="text-sm text-gray-600">Liquidity Score</span>
                      <div className="flex items-center space-x-2">
                        <span className="text-sm font-medium">{venue.liquidityScore}/100</span>
                        <div className="w-12 bg-gray-200 rounded-full h-2">
                          <div
                            className="bg-blue-600 h-2 rounded-full"
                            style={{ width: `${venue.liquidityScore}%` }}
                          />
                        </div>
                      </div>
                    </div>
                    
                    <div className="flex items-center justify-between pt-2">
                      <Badge variant={venue.isActive ? 'default' : 'destructive'}>
                        {venue.isActive ? 'ACTIVE' : 'INACTIVE'}
                      </Badge>
                      <span className="text-xs text-gray-400">
                        {venue.connectivity}
                      </span>
                    </div>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        </TabsContent>

        <TabsContent value="algorithms" className="space-y-4">
          <div className="space-y-6">
            {algorithms.map((algorithm) => (
              <Card key={algorithm.id} className={!algorithm.isActive ? 'opacity-60' : ''}>
                <CardHeader>
                  <div className="flex items-center justify-between">
                    <div className="flex items-center space-x-3">
                      <Badge className={getAlgorithmTypeColor(algorithm.type)}>
                        {algorithm.name}
                      </Badge>
                      <span className="text-sm text-gray-600">Priority {algorithm.priority}</span>
                    </div>
                    <div className="flex items-center space-x-2">
                      <Badge variant={algorithm.isActive ? 'default' : 'destructive'}>
                        {algorithm.isActive ? 'ACTIVE' : 'INACTIVE'}
                      </Badge>
                      <Button size="sm" variant="outline">
                        <Settings className="h-4 w-4" />
                      </Button>
                    </div>
                  </div>
                  <p className="text-sm text-gray-600">{algorithm.description}</p>
                </CardHeader>
                <CardContent>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    <div>
                      <h4 className="font-medium text-gray-900 mb-3">Performance Metrics</h4>
                      <div className="space-y-2">
                        <div className="flex items-center justify-between">
                          <span className="text-sm text-gray-600">Average Slippage</span>
                          <span className="text-sm font-medium">{algorithm.performance.averageSlippage.toFixed(2)} bps</span>
                        </div>
                        <div className="flex items-center justify-between">
                          <span className="text-sm text-gray-600">Fill Rate</span>
                          <span className="text-sm font-medium">{(algorithm.performance.fillRate * 100).toFixed(1)}%</span>
                        </div>
                        <div className="flex items-center justify-between">
                          <span className="text-sm text-gray-600">Execution Time</span>
                          <span className="text-sm font-medium">{algorithm.performance.executionTime.toFixed(1)}s</span>
                        </div>
                        <div className="flex items-center justify-between">
                          <span className="text-sm text-gray-600">Cost Savings</span>
                          <span className="text-sm font-medium text-green-600">
                            {(algorithm.performance.costSavings * 100).toFixed(2)}%
                          </span>
                        </div>
                      </div>
                    </div>
                    
                    <div>
                      <h4 className="font-medium text-gray-900 mb-3">Configuration</h4>
                      <div className="space-y-2">
                        {Object.entries(algorithm.parameters).map(([key, value]) => (
                          <div key={key} className="flex items-center justify-between">
                            <span className="text-sm text-gray-600">{key.replace(/([A-Z])/g, ' $1').toLowerCase()}</span>
                            <span className="text-sm font-medium">{String(value)}</span>
                          </div>
                        ))}
                      </div>
                    </div>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        </TabsContent>

        <TabsContent value="rules" className="space-y-4">
          <div className="space-y-6">
            {routingRules.map((rule) => (
              <Card key={rule.id} className={!rule.isActive ? 'opacity-60' : ''}>
                <CardHeader>
                  <div className="flex items-center justify-between">
                    <div className="flex items-center space-x-3">
                      <h3 className="font-medium">{rule.name}</h3>
                      <Badge variant="outline">Priority {rule.priority}</Badge>
                    </div>
                    <div className="flex items-center space-x-2">
                      <Badge variant={rule.isActive ? 'default' : 'destructive'}>
                        {rule.isActive ? 'ACTIVE' : 'INACTIVE'}
                      </Badge>
                      <Button size="sm" variant="outline">
                        <Edit className="h-4 w-4" />
                      </Button>
                    </div>
                  </div>
                </CardHeader>
                <CardContent>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    <div>
                      <h4 className="font-medium text-gray-900 mb-3">Conditions</h4>
                      <div className="space-y-2">
                        {Object.entries(rule.condition).map(([key, value]) => (
                          <div key={key} className="text-sm">
                            <span className="text-gray-600">{key.replace(/([A-Z])/g, ' $1').toLowerCase()}:</span>
                            <span className="ml-2 font-medium">{JSON.stringify(value)}</span>
                          </div>
                        ))}
                      </div>
                    </div>
                    
                    <div>
                      <h4 className="font-medium text-gray-900 mb-3">Actions</h4>
                      <div className="space-y-2">
                        <div className="text-sm">
                          <span className="text-gray-600">Algorithm:</span>
                          <span className="ml-2 font-medium">{rule.action.algorithm}</span>
                        </div>
                        <div className="text-sm">
                          <span className="text-gray-600">Venues:</span>
                          <span className="ml-2">{rule.action.venueSelection.join(', ')}</span>
                        </div>
                        <div className="text-sm">
                          <span className="text-gray-600">Max Slicing:</span>
                          <span className="ml-2 font-medium">{rule.action.maxSlicing}</span>
                        </div>
                        <div className="text-sm">
                          <span className="text-gray-600">Time Limit:</span>
                          <span className="ml-2 font-medium">{rule.action.timeLimit}s</span>
                        </div>
                      </div>
                    </div>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        </TabsContent>

        <TabsContent value="decisions" className="space-y-4">
          <Card>
            <CardContent className="p-0">
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead className="bg-gray-50 border-b">
                    <tr>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                        Order ID
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                        Symbol
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                        Algorithm
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                        Primary Venue
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                        Confidence
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                        Risk Score
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                        Est. Cost
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                        Actions
                      </th>
                    </tr>
                  </thead>
                  <tbody className="bg-white divide-y divide-gray-200">
                    {recentDecisions.map((decision) => (
                      <tr key={decision.orderId} className="hover:bg-gray-50">
                        <td className="px-6 py-4 whitespace-nowrap">
                          <Badge variant="outline">{decision.orderId}</Badge>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <div className="font-medium">{decision.symbol}</div>
                          <div className="text-sm text-gray-500">{decision.quantity.toLocaleString()} shares</div>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <Badge className={getAlgorithmTypeColor('SMART')}>
                            {decision.algorithm}
                          </Badge>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <div className="font-medium">{decision.venues[0].venue}</div>
                          <div className="text-sm text-gray-500">{decision.venues[0].percentage}% allocation</div>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <div className="flex items-center">
                            <span className="text-sm font-medium">{(decision.confidence * 100).toFixed(0)}%</span>
                            <div className="ml-2 w-16 bg-gray-200 rounded-full h-2">
                              <div
                                className="bg-green-600 h-2 rounded-full"
                                style={{ width: `${decision.confidence * 100}%` }}
                              />
                            </div>
                          </div>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <Badge className={getRiskScoreColor(decision.riskScore)}>
                            {(decision.riskScore * 100).toFixed(0)}%
                          </Badge>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <span className="text-sm font-medium">₹{decision.estimatedCost.toFixed(2)}</span>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                          <div className="flex items-center space-x-2">
                            <Button size="sm" variant="ghost">
                              <Eye className="h-4 w-4" />
                            </Button>
                            <Button size="sm" variant="ghost">
                              <MoreVertical className="h-4 w-4" />
                            </Button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  );
};

export default SmartOrderRouting;