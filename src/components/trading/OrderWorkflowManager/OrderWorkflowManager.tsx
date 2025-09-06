import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  GitBranch, Layers, Play, Pause, Square, CheckCircle,
  AlertTriangle, Clock, Target, Activity, Zap, Settings,
  ArrowRight, ArrowDown, MoreVertical, Copy, Edit, Trash2,
  Plus, Minus, RotateCcw, FastForward, Rewind, Eye,
  Brain, Shield, Users, Timer, Network, Filter,
  Calendar, DollarSign, TrendingUp, TrendingDown
} from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Input } from '@/components/ui/input';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';

interface OrderLeg {
  id: string;
  symbol: string;
  side: 'BUY' | 'SELL';
  quantity: number;
  orderType: 'MARKET' | 'LIMIT' | 'STOP' | 'CONDITIONAL';
  price?: number;
  stopPrice?: number;
  condition?: {
    type: 'PRICE' | 'TIME' | 'VOLUME' | 'FILL' | 'CUSTOM';
    operator: 'GT' | 'LT' | 'EQ' | 'GTE' | 'LTE';
    value: number | string;
    symbol?: string;
  };
  status: 'PENDING' | 'ACTIVE' | 'FILLED' | 'CANCELLED' | 'WAITING';
  executedQuantity: number;
  averagePrice: number;
  dependsOn?: string[]; // IDs of legs this depends on
  triggerAfter?: Date;
  maxSlippage?: number;
  timeInForce: 'DAY' | 'GTC' | 'IOC' | 'FOK';
  venue?: string;
  algorithm?: string;
  priority: number;
}

interface WorkflowStrategy {
  id: string;
  name: string;
  description: string;
  type: 'PAIRS_TRADING' | 'SPREAD' | 'STRADDLE' | 'STRANGLE' | 'BUTTERFLY' | 'CALENDAR' | 'CUSTOM';
  legs: OrderLeg[];
  status: 'DRAFT' | 'ACTIVE' | 'PAUSED' | 'COMPLETED' | 'CANCELLED';
  createdAt: Date;
  startTime?: Date;
  completionTime?: Date;
  totalValue: number;
  pnl: number;
  riskScore: number;
  maxDrawdown: number;
  successRate: number;
  executionMode: 'SEQUENTIAL' | 'PARALLEL' | 'CONDITIONAL' | 'SMART';
  riskParameters: {
    maxLoss: number;
    maxPositionSize: number;
    correlationLimit: number;
    hedgeRatio?: number;
  };
}

interface WorkflowTemplate {
  id: string;
  name: string;
  category: 'ARBITRAGE' | 'SPREAD' | 'MOMENTUM' | 'MEAN_REVERSION' | 'PAIRS' | 'OPTIONS';
  description: string;
  legs: Partial<OrderLeg>[];
  parameters: Record<string, any>;
  riskProfile: 'LOW' | 'MEDIUM' | 'HIGH';
  complexity: number;
  successRate: number;
  averageReturn: number;
}

interface WorkflowManagerProps {
  userId: string;
  onStrategyCreate?: (strategy: WorkflowStrategy) => void;
  onStrategyUpdate?: (strategy: WorkflowStrategy) => void;
  onLegExecution?: (leg: OrderLeg) => void;
}

export const OrderWorkflowManager: React.FC<WorkflowManagerProps> = ({
  userId,
  onStrategyCreate,
  onStrategyUpdate,
  onLegExecution
}) => {
  const [strategies, setStrategies] = useState<WorkflowStrategy[]>([]);
  const [templates, setTemplates] = useState<WorkflowTemplate[]>([]);
  const [selectedStrategy, setSelectedStrategy] = useState<WorkflowStrategy | null>(null);
  const [activeTab, setActiveTab] = useState<'active' | 'templates' | 'create' | 'analytics'>('active');
  const [isSimulating, setIsSimulating] = useState(true);
  const [selectedTemplate, setSelectedTemplate] = useState<string>('');
  
  // Strategy builder state
  const [newStrategy, setNewStrategy] = useState<Partial<WorkflowStrategy>>({
    name: '',
    description: '',
    type: 'CUSTOM',
    legs: [],
    executionMode: 'SEQUENTIAL',
    riskParameters: {
      maxLoss: 10000,
      maxPositionSize: 100000,
      correlationLimit: 0.8
    }
  });

  useEffect(() => {
    initializeMockData();
    if (isSimulating) {
      const interval = setInterval(simulateStrategyUpdates, 3000);
      return () => clearInterval(interval);
    }
  }, [isSimulating]);

  const initializeMockData = () => {
    const mockStrategies: WorkflowStrategy[] = [
      {
        id: 'STRAT-001',
        name: 'RELIANCE-TCS Pairs Trade',
        description: 'Long RELIANCE, Short TCS spread arbitrage',
        type: 'PAIRS_TRADING',
        legs: [
          {
            id: 'LEG-001',
            symbol: 'RELIANCE',
            side: 'BUY',
            quantity: 1000,
            orderType: 'LIMIT',
            price: 2450.00,
            status: 'FILLED',
            executedQuantity: 1000,
            averagePrice: 2449.50,
            timeInForce: 'GTC',
            venue: 'NSE',
            priority: 1
          },
          {
            id: 'LEG-002',
            symbol: 'TCS',
            side: 'SELL',
            quantity: 800,
            orderType: 'LIMIT',
            price: 3456.00,
            status: 'ACTIVE',
            executedQuantity: 600,
            averagePrice: 3456.20,
            dependsOn: ['LEG-001'],
            timeInForce: 'GTC',
            venue: 'NSE',
            priority: 2
          }
        ],
        status: 'ACTIVE',
        createdAt: new Date(Date.now() - 1800000),
        startTime: new Date(Date.now() - 1800000),
        totalValue: 5214800,
        pnl: 2450,
        riskScore: 0.25,
        maxDrawdown: 1200,
        successRate: 0.85,
        executionMode: 'SEQUENTIAL',
        riskParameters: {
          maxLoss: 15000,
          maxPositionSize: 200000,
          correlationLimit: 0.7,
          hedgeRatio: 0.8
        }
      },
      {
        id: 'STRAT-002',
        name: 'NIFTY Iron Condor',
        description: 'Options strategy with 4-leg iron condor setup',
        type: 'BUTTERFLY',
        legs: [
          {
            id: 'LEG-003',
            symbol: 'NIFTY_24800_CE',
            side: 'SELL',
            quantity: 50,
            orderType: 'LIMIT',
            price: 125.50,
            status: 'FILLED',
            executedQuantity: 50,
            averagePrice: 125.25,
            timeInForce: 'DAY',
            priority: 1
          },
          {
            id: 'LEG-004',
            symbol: 'NIFTY_25000_CE',
            side: 'BUY',
            quantity: 50,
            orderType: 'LIMIT',
            price: 75.25,
            status: 'FILLED',
            executedQuantity: 50,
            averagePrice: 75.50,
            timeInForce: 'DAY',
            priority: 1
          },
          {
            id: 'LEG-005',
            symbol: 'NIFTY_24600_PE',
            side: 'SELL',
            quantity: 50,
            orderType: 'LIMIT',
            price: 118.75,
            status: 'WAITING',
            executedQuantity: 0,
            averagePrice: 0,
            condition: {
              type: 'FILL',
              operator: 'EQ',
              value: 100,
              symbol: 'LEG-003,LEG-004'
            },
            timeInForce: 'DAY',
            priority: 2
          },
          {
            id: 'LEG-006',
            symbol: 'NIFTY_24400_PE',
            side: 'BUY',
            quantity: 50,
            orderType: 'LIMIT',
            price: 68.50,
            status: 'WAITING',
            executedQuantity: 0,
            averagePrice: 0,
            dependsOn: ['LEG-005'],
            timeInForce: 'DAY',
            priority: 3
          }
        ],
        status: 'ACTIVE',
        createdAt: new Date(Date.now() - 3600000),
        startTime: new Date(Date.now() - 3600000),
        totalValue: 62500,
        pnl: 1250,
        riskScore: 0.35,
        maxDrawdown: 500,
        successRate: 0.72,
        executionMode: 'CONDITIONAL',
        riskParameters: {
          maxLoss: 5000,
          maxPositionSize: 100000,
          correlationLimit: 0.9
        }
      }
    ];

    const mockTemplates: WorkflowTemplate[] = [
      {
        id: 'TMPL-001',
        name: 'Basic Pairs Trading',
        category: 'PAIRS',
        description: 'Statistical arbitrage between two correlated stocks',
        legs: [
          {
            symbol: 'STOCK_A',
            side: 'BUY',
            quantity: 1000,
            orderType: 'LIMIT',
            priority: 1
          },
          {
            symbol: 'STOCK_B',
            side: 'SELL',
            quantity: 800,
            orderType: 'LIMIT',
            dependsOn: ['LEG-001'],
            priority: 2
          }
        ],
        parameters: {
          hedgeRatio: 0.8,
          entryZScore: 2.0,
          exitZScore: 0.5,
          lookbackPeriod: 30
        },
        riskProfile: 'MEDIUM',
        complexity: 3,
        successRate: 0.68,
        averageReturn: 0.08
      },
      {
        id: 'TMPL-002',
        name: 'Long Straddle',
        category: 'OPTIONS',
        description: 'Long call and put options at same strike',
        legs: [
          {
            symbol: 'OPTION_CALL',
            side: 'BUY',
            quantity: 100,
            orderType: 'LIMIT',
            priority: 1
          },
          {
            symbol: 'OPTION_PUT',
            side: 'BUY',
            quantity: 100,
            orderType: 'LIMIT',
            priority: 1
          }
        ],
        parameters: {
          strike: 0,
          expiry: 30,
          impliedVolatility: 0.25
        },
        riskProfile: 'HIGH',
        complexity: 4,
        successRate: 0.55,
        averageReturn: 0.15
      },
      {
        id: 'TMPL-003',
        name: 'Calendar Spread',
        category: 'SPREAD',
        description: 'Buy long-term option, sell short-term option',
        legs: [
          {
            symbol: 'OPTION_LONG',
            side: 'BUY',
            quantity: 50,
            orderType: 'LIMIT',
            priority: 1
          },
          {
            symbol: 'OPTION_SHORT',
            side: 'SELL',
            quantity: 50,
            orderType: 'LIMIT',
            priority: 1
          }
        ],
        parameters: {
          longExpiry: 60,
          shortExpiry: 30,
          strike: 0,
          timeDecay: 0.05
        },
        riskProfile: 'LOW',
        complexity: 2,
        successRate: 0.75,
        averageReturn: 0.06
      }
    ];

    setStrategies(mockStrategies);
    setTemplates(mockTemplates);
  };

  const simulateStrategyUpdates = () => {
    setStrategies(prevStrategies => 
      prevStrategies.map(strategy => {
        if (strategy.status === 'ACTIVE') {
          const updatedLegs = strategy.legs.map(leg => {
            // Simulate leg execution progress
            if (leg.status === 'ACTIVE' && leg.executedQuantity < leg.quantity) {
              const additionalExecution = Math.min(
                Math.floor(Math.random() * 20 + 5),
                leg.quantity - leg.executedQuantity
              );
              
              const newExecutedQuantity = leg.executedQuantity + additionalExecution;
              const newStatus = newExecutedQuantity >= leg.quantity ? 'FILLED' : 'ACTIVE';
              
              return {
                ...leg,
                executedQuantity: newExecutedQuantity,
                status: newStatus,
                averagePrice: leg.averagePrice + (Math.random() - 0.5) * 2
              };
            }
            
            // Simulate conditional leg activation
            if (leg.status === 'WAITING' && leg.dependsOn) {
              const dependencies = strategy.legs.filter(l => leg.dependsOn?.includes(l.id));
              const allDependenciesFilled = dependencies.every(d => d.status === 'FILLED');
              
              if (allDependenciesFilled) {
                return { ...leg, status: 'ACTIVE' };
              }
            }
            
            return leg;
          });

          // Update strategy PnL
          const filledLegs = updatedLegs.filter(leg => leg.status === 'FILLED');
          const totalPnL = filledLegs.reduce((pnl, leg) => {
            const marketPrice = leg.averagePrice + (Math.random() - 0.5) * 10;
            const legPnL = leg.side === 'BUY' 
              ? (marketPrice - leg.averagePrice) * leg.executedQuantity
              : (leg.averagePrice - marketPrice) * leg.executedQuantity;
            return pnl + legPnL;
          }, 0);

          // Check if strategy is complete
          const allLegsFilled = updatedLegs.every(leg => leg.status === 'FILLED');
          const newStatus = allLegsFilled ? 'COMPLETED' : 'ACTIVE';

          return {
            ...strategy,
            legs: updatedLegs,
            pnl: totalPnL,
            status: newStatus,
            completionTime: newStatus === 'COMPLETED' ? new Date() : undefined
          };
        }
        
        return strategy;
      })
    );
  };

  const handleStrategyAction = (strategyId: string, action: 'START' | 'PAUSE' | 'CANCEL' | 'RESUME') => {
    setStrategies(prevStrategies =>
      prevStrategies.map(strategy => {
        if (strategy.id === strategyId) {
          let newStatus = strategy.status;
          let startTime = strategy.startTime;

          switch (action) {
            case 'START':
              newStatus = 'ACTIVE';
              startTime = new Date();
              break;
            case 'PAUSE':
              newStatus = 'PAUSED';
              break;
            case 'RESUME':
              newStatus = 'ACTIVE';
              break;
            case 'CANCEL':
              newStatus = 'CANCELLED';
              break;
          }

          return { ...strategy, status: newStatus, startTime };
        }
        return strategy;
      })
    );
  };

  const handleLegAction = (strategyId: string, legId: string, action: 'CANCEL' | 'MODIFY') => {
    setStrategies(prevStrategies =>
      prevStrategies.map(strategy => {
        if (strategy.id === strategyId) {
          const updatedLegs = strategy.legs.map(leg => {
            if (leg.id === legId) {
              switch (action) {
                case 'CANCEL':
                  return { ...leg, status: 'CANCELLED' };
                case 'MODIFY':
                  // Implementation for leg modification would go here
                  return leg;
                default:
                  return leg;
              }
            }
            return leg;
          });
          
          return { ...strategy, legs: updatedLegs };
        }
        return strategy;
      })
    );
  };

  const createStrategyFromTemplate = (template: WorkflowTemplate) => {
    const strategyLegs: OrderLeg[] = template.legs.map((legTemplate, index) => ({
      id: `LEG-${Date.now()}-${index}`,
      symbol: legTemplate.symbol || '',
      side: legTemplate.side || 'BUY',
      quantity: legTemplate.quantity || 100,
      orderType: legTemplate.orderType || 'LIMIT',
      status: 'PENDING',
      executedQuantity: 0,
      averagePrice: 0,
      dependsOn: legTemplate.dependsOn,
      timeInForce: legTemplate.timeInForce || 'GTC',
      priority: legTemplate.priority || 1
    }));

    const newStrategyFromTemplate: WorkflowStrategy = {
      id: `STRAT-${Date.now()}`,
      name: `${template.name} - ${new Date().toLocaleString()}`,
      description: template.description,
      type: 'CUSTOM',
      legs: strategyLegs,
      status: 'DRAFT',
      createdAt: new Date(),
      totalValue: 0,
      pnl: 0,
      riskScore: template.riskProfile === 'LOW' ? 0.2 : template.riskProfile === 'MEDIUM' ? 0.5 : 0.8,
      maxDrawdown: 0,
      successRate: template.successRate,
      executionMode: 'SEQUENTIAL',
      riskParameters: {
        maxLoss: 10000,
        maxPositionSize: 100000,
        correlationLimit: 0.8
      }
    };

    setStrategies(prev => [...prev, newStrategyFromTemplate]);
    onStrategyCreate?.(newStrategyFromTemplate);
  };

  const addLegToNewStrategy = () => {
    const newLeg: OrderLeg = {
      id: `LEG-${Date.now()}`,
      symbol: '',
      side: 'BUY',
      quantity: 100,
      orderType: 'LIMIT',
      status: 'PENDING',
      executedQuantity: 0,
      averagePrice: 0,
      timeInForce: 'GTC',
      priority: (newStrategy.legs?.length || 0) + 1
    };

    setNewStrategy(prev => ({
      ...prev,
      legs: [...(prev.legs || []), newLeg]
    }));
  };

  const updateLegInNewStrategy = (legId: string, updates: Partial<OrderLeg>) => {
    setNewStrategy(prev => ({
      ...prev,
      legs: prev.legs?.map(leg => leg.id === legId ? { ...leg, ...updates } : leg) || []
    }));
  };

  const removeLegFromNewStrategy = (legId: string) => {
    setNewStrategy(prev => ({
      ...prev,
      legs: prev.legs?.filter(leg => leg.id !== legId) || []
    }));
  };

  const createCustomStrategy = () => {
    if (!newStrategy.name || !newStrategy.legs?.length) return;

    const strategy: WorkflowStrategy = {
      id: `STRAT-${Date.now()}`,
      name: newStrategy.name,
      description: newStrategy.description || '',
      type: newStrategy.type || 'CUSTOM',
      legs: newStrategy.legs,
      status: 'DRAFT',
      createdAt: new Date(),
      totalValue: 0,
      pnl: 0,
      riskScore: 0.3,
      maxDrawdown: 0,
      successRate: 0.6,
      executionMode: newStrategy.executionMode || 'SEQUENTIAL',
      riskParameters: newStrategy.riskParameters || {
        maxLoss: 10000,
        maxPositionSize: 100000,
        correlationLimit: 0.8
      }
    };

    setStrategies(prev => [...prev, strategy]);
    onStrategyCreate?.(strategy);

    // Reset form
    setNewStrategy({
      name: '',
      description: '',
      type: 'CUSTOM',
      legs: [],
      executionMode: 'SEQUENTIAL',
      riskParameters: {
        maxLoss: 10000,
        maxPositionSize: 100000,
        correlationLimit: 0.8
      }
    });
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'ACTIVE': return 'bg-green-100 text-green-800 border-green-200';
      case 'FILLED': return 'bg-blue-100 text-blue-800 border-blue-200';
      case 'PAUSED': return 'bg-yellow-100 text-yellow-800 border-yellow-200';
      case 'COMPLETED': return 'bg-purple-100 text-purple-800 border-purple-200';
      case 'CANCELLED': return 'bg-red-100 text-red-800 border-red-200';
      case 'WAITING': return 'bg-orange-100 text-orange-800 border-orange-200';
      case 'PENDING': return 'bg-gray-100 text-gray-800 border-gray-200';
      case 'DRAFT': return 'bg-gray-100 text-gray-800 border-gray-200';
      default: return 'bg-gray-100 text-gray-800 border-gray-200';
    }
  };

  const getRiskColor = (riskScore: number) => {
    if (riskScore < 0.3) return 'bg-green-100 text-green-800';
    if (riskScore < 0.7) return 'bg-yellow-100 text-yellow-800';
    return 'bg-red-100 text-red-800';
  };

  return (
    <div className="p-6 space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Order Workflow Manager</h1>
          <p className="text-gray-600">Complex multi-leg strategy execution and management</p>
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
            <Plus className="h-4 w-4" />
            <span>New Strategy</span>
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
                <p className="text-sm font-medium text-gray-600">Active Strategies</p>
                <p className="text-2xl font-bold text-gray-900">
                  {strategies.filter(s => s.status === 'ACTIVE').length}
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
                <p className="text-sm font-medium text-gray-600">Success Rate</p>
                <p className="text-2xl font-bold text-gray-900">
                  {(strategies.reduce((acc, s) => acc + s.successRate, 0) / strategies.length * 100).toFixed(0)}%
                </p>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-6">
            <div className="flex items-center">
              <DollarSign className="h-8 w-8 text-purple-600" />
              <div className="ml-4">
                <p className="text-sm font-medium text-gray-600">Total P&L</p>
                <p className={`text-2xl font-bold ${
                  strategies.reduce((acc, s) => acc + s.pnl, 0) >= 0 ? 'text-green-600' : 'text-red-600'
                }`}>
                  ₹{strategies.reduce((acc, s) => acc + s.pnl, 0).toLocaleString()}
                </p>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-6">
            <div className="flex items-center">
              <Shield className="h-8 w-8 text-orange-600" />
              <div className="ml-4">
                <p className="text-sm font-medium text-gray-600">Avg Risk Score</p>
                <p className="text-2xl font-bold text-gray-900">
                  {(strategies.reduce((acc, s) => acc + s.riskScore, 0) / strategies.length).toFixed(2)}
                </p>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Main Content */}
      <Tabs value={activeTab} onValueChange={(value) => setActiveTab(value as any)}>
        <TabsList className="grid w-full grid-cols-4">
          <TabsTrigger value="active" className="flex items-center space-x-2">
            <Activity className="h-4 w-4" />
            <span>Active Strategies</span>
          </TabsTrigger>
          <TabsTrigger value="templates" className="flex items-center space-x-2">
            <Layers className="h-4 w-4" />
            <span>Templates</span>
          </TabsTrigger>
          <TabsTrigger value="create" className="flex items-center space-x-2">
            <Plus className="h-4 w-4" />
            <span>Create</span>
          </TabsTrigger>
          <TabsTrigger value="analytics" className="flex items-center space-x-2">
            <TrendingUp className="h-4 w-4" />
            <span>Analytics</span>
          </TabsTrigger>
        </TabsList>

        <TabsContent value="active" className="space-y-6">
          <div className="space-y-4">
            {strategies.map((strategy) => (
              <Card key={strategy.id} className="overflow-hidden">
                <CardHeader className="bg-gray-50">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center space-x-4">
                      <div>
                        <CardTitle className="text-lg">{strategy.name}</CardTitle>
                        <p className="text-sm text-gray-600 mt-1">{strategy.description}</p>
                      </div>
                      <Badge className={getStatusColor(strategy.status)}>
                        {strategy.status}
                      </Badge>
                      <Badge variant="outline">{strategy.type}</Badge>
                      <Badge className={getRiskColor(strategy.riskScore)}>
                        Risk: {(strategy.riskScore * 100).toFixed(0)}%
                      </Badge>
                    </div>
                    
                    <div className="flex items-center space-x-2">
                      {strategy.status === 'DRAFT' && (
                        <Button
                          size="sm"
                          onClick={() => handleStrategyAction(strategy.id, 'START')}
                          className="flex items-center space-x-1"
                        >
                          <Play className="h-4 w-4" />
                          <span>Start</span>
                        </Button>
                      )}
                      {strategy.status === 'ACTIVE' && (
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={() => handleStrategyAction(strategy.id, 'PAUSE')}
                          className="flex items-center space-x-1"
                        >
                          <Pause className="h-4 w-4" />
                          <span>Pause</span>
                        </Button>
                      )}
                      {strategy.status === 'PAUSED' && (
                        <Button
                          size="sm"
                          onClick={() => handleStrategyAction(strategy.id, 'RESUME')}
                          className="flex items-center space-x-1"
                        >
                          <Play className="h-4 w-4" />
                          <span>Resume</span>
                        </Button>
                      )}
                      <Button
                        size="sm"
                        variant="destructive"
                        onClick={() => handleStrategyAction(strategy.id, 'CANCEL')}
                        className="flex items-center space-x-1"
                      >
                        <Square className="h-4 w-4" />
                        <span>Cancel</span>
                      </Button>
                      <Button
                        size="sm"
                        variant="ghost"
                        onClick={() => setSelectedStrategy(strategy)}
                      >
                        <Eye className="h-4 w-4" />
                      </Button>
                    </div>
                  </div>
                  
                  {/* Strategy Metrics */}
                  <div className="grid grid-cols-5 gap-4 mt-4">
                    <div>
                      <p className="text-sm text-gray-600">Total Value</p>
                      <p className="font-medium">₹{strategy.totalValue.toLocaleString()}</p>
                    </div>
                    <div>
                      <p className="text-sm text-gray-600">P&L</p>
                      <p className={`font-medium ${strategy.pnl >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                        ₹{strategy.pnl.toLocaleString()}
                      </p>
                    </div>
                    <div>
                      <p className="text-sm text-gray-600">Success Rate</p>
                      <p className="font-medium">{(strategy.successRate * 100).toFixed(1)}%</p>
                    </div>
                    <div>
                      <p className="text-sm text-gray-600">Execution Mode</p>
                      <p className="font-medium">{strategy.executionMode}</p>
                    </div>
                    <div>
                      <p className="text-sm text-gray-600">Legs</p>
                      <p className="font-medium">
                        {strategy.legs.filter(leg => leg.status === 'FILLED').length} / {strategy.legs.length}
                      </p>
                    </div>
                  </div>
                </CardHeader>

                <CardContent className="p-6">
                  {/* Workflow Visualization */}
                  <div className="space-y-4">
                    <h4 className="font-medium text-gray-900">Execution Flow</h4>
                    <div className="flex items-center space-x-4 overflow-x-auto pb-2">
                      {strategy.legs
                        .sort((a, b) => a.priority - b.priority)
                        .map((leg, index) => (
                          <div key={leg.id} className="flex items-center space-x-2 flex-shrink-0">
                            <Card className="w-64 p-4">
                              <div className="flex items-center justify-between mb-2">
                                <div className="flex items-center space-x-2">
                                  <Badge variant={leg.side === 'BUY' ? 'default' : 'destructive'}>
                                    {leg.side}
                                  </Badge>
                                  <span className="font-medium">{leg.symbol}</span>
                                </div>
                                <Badge className={getStatusColor(leg.status)}>
                                  {leg.status}
                                </Badge>
                              </div>
                              
                              <div className="space-y-1 text-sm">
                                <div className="flex justify-between">
                                  <span className="text-gray-600">Quantity:</span>
                                  <span>{leg.quantity.toLocaleString()}</span>
                                </div>
                                <div className="flex justify-between">
                                  <span className="text-gray-600">Executed:</span>
                                  <span>{leg.executedQuantity.toLocaleString()}</span>
                                </div>
                                {leg.price && (
                                  <div className="flex justify-between">
                                    <span className="text-gray-600">Price:</span>
                                    <span>₹{leg.price.toFixed(2)}</span>
                                  </div>
                                )}
                                {leg.averagePrice > 0 && (
                                  <div className="flex justify-between">
                                    <span className="text-gray-600">Avg Price:</span>
                                    <span>₹{leg.averagePrice.toFixed(2)}</span>
                                  </div>
                                )}
                              </div>
                              
                              <div className="mt-3">
                                <div className="flex items-center justify-between text-xs text-gray-600 mb-1">
                                  <span>Progress</span>
                                  <span>{((leg.executedQuantity / leg.quantity) * 100).toFixed(0)}%</span>
                                </div>
                                <div className="w-full bg-gray-200 rounded-full h-2">
                                  <div
                                    className={`h-2 rounded-full ${
                                      leg.status === 'FILLED' ? 'bg-green-600' : 
                                      leg.status === 'ACTIVE' ? 'bg-blue-600' : 'bg-gray-400'
                                    }`}
                                    style={{ width: `${(leg.executedQuantity / leg.quantity) * 100}%` }}
                                  />
                                </div>
                              </div>

                              {leg.condition && (
                                <div className="mt-2 p-2 bg-orange-50 rounded text-xs">
                                  <div className="flex items-center space-x-1">
                                    <Clock className="h-3 w-3 text-orange-600" />
                                    <span className="text-orange-800">
                                      Waiting: {leg.condition.type} {leg.condition.operator} {leg.condition.value}
                                    </span>
                                  </div>
                                </div>
                              )}

                              {leg.dependsOn && leg.dependsOn.length > 0 && (
                                <div className="mt-2 p-2 bg-blue-50 rounded text-xs">
                                  <div className="flex items-center space-x-1">
                                    <GitBranch className="h-3 w-3 text-blue-600" />
                                    <span className="text-blue-800">
                                      Depends on: {leg.dependsOn.length} leg(s)
                                    </span>
                                  </div>
                                </div>
                              )}

                              <div className="mt-3 flex justify-between">
                                <Button
                                  size="sm"
                                  variant="ghost"
                                  onClick={() => handleLegAction(strategy.id, leg.id, 'MODIFY')}
                                >
                                  <Edit className="h-3 w-3" />
                                </Button>
                                <Button
                                  size="sm"
                                  variant="ghost"
                                  onClick={() => handleLegAction(strategy.id, leg.id, 'CANCEL')}
                                >
                                  <Trash2 className="h-3 w-3" />
                                </Button>
                              </div>
                            </Card>
                            
                            {index < strategy.legs.length - 1 && (
                              <ArrowRight className="h-6 w-6 text-gray-400" />
                            )}
                          </div>
                        ))}
                    </div>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        </TabsContent>

        <TabsContent value="templates" className="space-y-6">
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {templates.map((template) => (
              <Card key={template.id} className="hover:shadow-lg transition-shadow">
                <CardHeader>
                  <div className="flex items-center justify-between">
                    <CardTitle className="text-lg">{template.name}</CardTitle>
                    <Badge variant="outline">{template.category}</Badge>
                  </div>
                  <p className="text-sm text-gray-600">{template.description}</p>
                </CardHeader>
                <CardContent>
                  <div className="space-y-4">
                    <div className="grid grid-cols-2 gap-4 text-sm">
                      <div>
                        <span className="text-gray-600">Risk Profile:</span>
                        <div className="flex items-center space-x-2">
                          <Badge className={
                            template.riskProfile === 'LOW' ? 'bg-green-100 text-green-800' :
                            template.riskProfile === 'MEDIUM' ? 'bg-yellow-100 text-yellow-800' :
                            'bg-red-100 text-red-800'
                          }>
                            {template.riskProfile}
                          </Badge>
                        </div>
                      </div>
                      <div>
                        <span className="text-gray-600">Complexity:</span>
                        <div className="flex items-center space-x-1">
                          {[...Array(5)].map((_, i) => (
                            <div
                              key={i}
                              className={`w-2 h-2 rounded-full ${
                                i < template.complexity ? 'bg-blue-600' : 'bg-gray-300'
                              }`}
                            />
                          ))}
                        </div>
                      </div>
                      <div>
                        <span className="text-gray-600">Success Rate:</span>
                        <span className="font-medium text-green-600 ml-2">
                          {(template.successRate * 100).toFixed(0)}%
                        </span>
                      </div>
                      <div>
                        <span className="text-gray-600">Avg Return:</span>
                        <span className="font-medium text-blue-600 ml-2">
                          {(template.averageReturn * 100).toFixed(1)}%
                        </span>
                      </div>
                    </div>

                    <div>
                      <h4 className="font-medium text-sm mb-2">Strategy Legs ({template.legs.length})</h4>
                      <div className="space-y-1">
                        {template.legs.slice(0, 3).map((leg, index) => (
                          <div key={index} className="flex items-center justify-between text-xs bg-gray-50 p-2 rounded">
                            <div className="flex items-center space-x-2">
                              <Badge variant={leg.side === 'BUY' ? 'default' : 'destructive'} className="text-xs">
                                {leg.side}
                              </Badge>
                              <span>{leg.symbol}</span>
                            </div>
                            <span>{leg.quantity}</span>
                          </div>
                        ))}
                        {template.legs.length > 3 && (
                          <div className="text-xs text-gray-500 text-center">
                            +{template.legs.length - 3} more legs
                          </div>
                        )}
                      </div>
                    </div>

                    <Button 
                      onClick={() => createStrategyFromTemplate(template)}
                      className="w-full flex items-center space-x-2"
                    >
                      <Copy className="h-4 w-4" />
                      <span>Use Template</span>
                    </Button>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        </TabsContent>

        <TabsContent value="create" className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle>Create Custom Strategy</CardTitle>
            </CardHeader>
            <CardContent className="space-y-6">
              {/* Basic Strategy Info */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">Strategy Name</label>
                  <Input
                    value={newStrategy.name}
                    onChange={(e) => setNewStrategy({ ...newStrategy, name: e.target.value })}
                    placeholder="Enter strategy name"
                  />
                </div>
                
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">Strategy Type</label>
                  <select
                    value={newStrategy.type}
                    onChange={(e) => setNewStrategy({ ...newStrategy, type: e.target.value as any })}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                  >
                    <option value="CUSTOM">Custom</option>
                    <option value="PAIRS_TRADING">Pairs Trading</option>
                    <option value="SPREAD">Spread</option>
                    <option value="STRADDLE">Straddle</option>
                    <option value="STRANGLE">Strangle</option>
                    <option value="BUTTERFLY">Butterfly</option>
                    <option value="CALENDAR">Calendar</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">Description</label>
                <textarea
                  value={newStrategy.description}
                  onChange={(e) => setNewStrategy({ ...newStrategy, description: e.target.value })}
                  rows={3}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                  placeholder="Describe your strategy"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">Execution Mode</label>
                <select
                  value={newStrategy.executionMode}
                  onChange={(e) => setNewStrategy({ ...newStrategy, executionMode: e.target.value as any })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  <option value="SEQUENTIAL">Sequential</option>
                  <option value="PARALLEL">Parallel</option>
                  <option value="CONDITIONAL">Conditional</option>
                  <option value="SMART">Smart</option>
                </select>
              </div>

              {/* Strategy Legs */}
              <div>
                <div className="flex items-center justify-between mb-4">
                  <h3 className="text-lg font-medium">Strategy Legs</h3>
                  <Button onClick={addLegToNewStrategy} className="flex items-center space-x-2">
                    <Plus className="h-4 w-4" />
                    <span>Add Leg</span>
                  </Button>
                </div>

                <div className="space-y-4">
                  {newStrategy.legs?.map((leg, index) => (
                    <Card key={leg.id} className="p-4">
                      <div className="flex items-center justify-between mb-4">
                        <h4 className="font-medium">Leg {index + 1}</h4>
                        <Button
                          size="sm"
                          variant="destructive"
                          onClick={() => removeLegFromNewStrategy(leg.id)}
                        >
                          <Trash2 className="h-4 w-4" />
                        </Button>
                      </div>

                      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                        <div>
                          <label className="block text-sm font-medium text-gray-700 mb-2">Symbol</label>
                          <Input
                            value={leg.symbol}
                            onChange={(e) => updateLegInNewStrategy(leg.id, { symbol: e.target.value })}
                            placeholder="Symbol"
                          />
                        </div>

                        <div>
                          <label className="block text-sm font-medium text-gray-700 mb-2">Side</label>
                          <select
                            value={leg.side}
                            onChange={(e) => updateLegInNewStrategy(leg.id, { side: e.target.value as 'BUY' | 'SELL' })}
                            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                          >
                            <option value="BUY">BUY</option>
                            <option value="SELL">SELL</option>
                          </select>
                        </div>

                        <div>
                          <label className="block text-sm font-medium text-gray-700 mb-2">Quantity</label>
                          <Input
                            type="number"
                            value={leg.quantity}
                            onChange={(e) => updateLegInNewStrategy(leg.id, { quantity: parseInt(e.target.value) })}
                            placeholder="Quantity"
                          />
                        </div>

                        <div>
                          <label className="block text-sm font-medium text-gray-700 mb-2">Order Type</label>
                          <select
                            value={leg.orderType}
                            onChange={(e) => updateLegInNewStrategy(leg.id, { orderType: e.target.value as any })}
                            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                          >
                            <option value="MARKET">Market</option>
                            <option value="LIMIT">Limit</option>
                            <option value="STOP">Stop</option>
                            <option value="CONDITIONAL">Conditional</option>
                          </select>
                        </div>

                        {(leg.orderType === 'LIMIT' || leg.orderType === 'STOP') && (
                          <div>
                            <label className="block text-sm font-medium text-gray-700 mb-2">Price</label>
                            <Input
                              type="number"
                              step="0.01"
                              value={leg.price || ''}
                              onChange={(e) => updateLegInNewStrategy(leg.id, { price: parseFloat(e.target.value) })}
                              placeholder="Price"
                            />
                          </div>
                        )}

                        <div>
                          <label className="block text-sm font-medium text-gray-700 mb-2">Priority</label>
                          <Input
                            type="number"
                            value={leg.priority}
                            onChange={(e) => updateLegInNewStrategy(leg.id, { priority: parseInt(e.target.value) })}
                            placeholder="Priority"
                          />
                        </div>
                      </div>
                    </Card>
                  ))}
                </div>
              </div>

              <div className="flex justify-end space-x-4">
                <Button variant="outline">
                  Cancel
                </Button>
                <Button 
                  onClick={createCustomStrategy}
                  disabled={!newStrategy.name || !newStrategy.legs?.length}
                  className="flex items-center space-x-2"
                >
                  <Plus className="h-4 w-4" />
                  <span>Create Strategy</span>
                </Button>
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="analytics" className="space-y-6">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <Card>
              <CardHeader>
                <CardTitle>Strategy Performance</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-4">
                  {strategies.map((strategy) => (
                    <div key={strategy.id} className="flex items-center justify-between p-3 border rounded-lg">
                      <div>
                        <div className="font-medium">{strategy.name}</div>
                        <div className="text-sm text-gray-500">{strategy.type}</div>
                      </div>
                      <div className="text-right">
                        <div className={`font-medium ${strategy.pnl >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                          ₹{strategy.pnl.toLocaleString()}
                        </div>
                        <div className="text-sm text-gray-500">
                          {(strategy.successRate * 100).toFixed(0)}% success
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>Risk Distribution</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-4">
                  {['LOW', 'MEDIUM', 'HIGH'].map((risk) => {
                    const count = strategies.filter(s => 
                      risk === 'LOW' ? s.riskScore < 0.3 :
                      risk === 'MEDIUM' ? s.riskScore >= 0.3 && s.riskScore < 0.7 :
                      s.riskScore >= 0.7
                    ).length;
                    const percentage = (count / strategies.length) * 100;
                    
                    return (
                      <div key={risk} className="flex items-center justify-between">
                        <div className="flex items-center space-x-2">
                          <Badge className={
                            risk === 'LOW' ? 'bg-green-100 text-green-800' :
                            risk === 'MEDIUM' ? 'bg-yellow-100 text-yellow-800' :
                            'bg-red-100 text-red-800'
                          }>
                            {risk}
                          </Badge>
                          <span className="text-sm">{count} strategies</span>
                        </div>
                        <div className="flex items-center space-x-2">
                          <span className="text-sm font-medium">{percentage.toFixed(0)}%</span>
                          <div className="w-20 bg-gray-200 rounded-full h-2">
                            <div
                              className={`h-2 rounded-full ${
                                risk === 'LOW' ? 'bg-green-600' :
                                risk === 'MEDIUM' ? 'bg-yellow-600' :
                                'bg-red-600'
                              }`}
                              style={{ width: `${percentage}%` }}
                            />
                          </div>
                        </div>
                      </div>
                    );
                  })}
                </div>
              </CardContent>
            </Card>
          </div>
        </TabsContent>
      </Tabs>
    </div>
  );
};

export default OrderWorkflowManager;