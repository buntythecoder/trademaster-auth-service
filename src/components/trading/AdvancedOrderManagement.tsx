import React, { useState, useEffect, useCallback } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import {
  Route, TrendingUp, TrendingDown, AlertTriangle, CheckCircle, Clock, 
  X, RefreshCw, Eye, Settings, BarChart3, Activity, Zap, Target, 
  ArrowRight, ArrowLeft, Shuffle, GitBranch, Filter, Search, 
  ExternalLink, Copy, Check, Info, AlertCircle, Play, Pause, 
  FastForward, Rewind, MoreVertical, Edit3, Trash2, Plus,
  Building2, Star, Flag, Timer, Database, Monitor, Server,
  Network, Wifi, WifiOff, Signal, Bell, Volume2, VolumeX,
  ChevronDown, ChevronUp, ChevronRight, Hash, DollarSign
} from 'lucide-react'
import { IndianBrokerType } from '../../services/brokerService'

// Types
interface OrderRouting {
  id: string
  orderId: string
  symbol: string
  orderType: 'market' | 'limit' | 'stop_loss' | 'stop_limit'
  side: 'buy' | 'sell'
  quantity: number
  price?: number
  stopPrice?: number
  originalBroker?: IndianBrokerType
  targetBroker: IndianBrokerType
  routingReason: 'best_execution' | 'cost_optimization' | 'availability' | 'speed' | 'manual' | 'fallback'
  routingScore: number
  estimatedExecutionTime: number
  estimatedCost: number
  createdAt: Date
  status: 'pending' | 'routed' | 'executing' | 'completed' | 'failed' | 'cancelled'
}

interface BrokerRoutingMetrics {
  brokerId: IndianBrokerType
  brokerName: string
  executionSpeed: number // ms
  successRate: number // %
  avgCost: number // basis points
  capacity: number // max orders per minute
  currentLoad: number // current orders
  reliability: number // uptime %
  latency: number // ms
  lastUpdated: Date
  isPreferred: boolean
  status: 'online' | 'degraded' | 'offline' | 'maintenance'
  // FRONT-017 Enhancements
  fillRate: number // % of orders filled
  averageSlippage: number // basis points
  priceImprovement: number // basis points
  rejectRate: number // % of orders rejected
  partialFillRate: number // % of orders partially filled
  avgOrderValue: number // rupees
  peakLatency: number // worst case latency in ms
  uptimeToday: number // % uptime today
  errorsByType: Record<string, number> // error distribution
  specializations: string[] // what this broker is good for
}

interface OrderTranslation {
  id: string
  orderId: string
  originalFormat: any
  translatedFormat: any
  sourceBroker: 'trademaster'
  targetBroker: IndianBrokerType
  translationStatus: 'pending' | 'translating' | 'completed' | 'failed' | 'validation_error'
  validationErrors: string[]
  translationTime: number // ms
  createdAt: Date
  completedAt?: Date
}

interface FailedOrder {
  id: string
  orderId: string
  symbol: string
  broker: IndianBrokerType
  errorType: 'validation' | 'execution' | 'network' | 'authentication' | 'limit_exceeded' | 'market_closed' | 'price_rejection' | 'quantity_freeze' | 'margin_shortage' | 'symbol_ban'
  errorMessage: string
  errorCode?: string
  failedAt: Date
  retryAttempts: number
  maxRetries: number
  nextRetryAt?: Date
  canRetry: boolean
  canModify: boolean
  suggestedAction: string
  originalOrder: any
  // FRONT-017 Broker-Specific Enhancements
  brokerSpecificError?: {
    zerodha?: { rmsError?: boolean; freezeQty?: number; availableMargin?: number }
    upstox?: { positionBlocked?: boolean; priceOutOfRange?: boolean }
    angel_one?: { sessionExpired?: boolean; instrumentBlocked?: boolean }
    icici_direct?: { riskValidationFailed?: boolean; tradingHalted?: boolean }
    groww?: { kycIncomplete?: boolean; accountRestricted?: boolean }
    iifl?: { marginCallPending?: boolean; corporateActionPending?: boolean }
  }
  alternativeBrokers?: IndianBrokerType[] // brokers that could handle this order
  modificationSuggestions?: {
    priceRange?: { min: number; max: number }
    quantityLimit?: number
    alternativeSymbol?: string
    timeRestriction?: string
  }
  recoveryStrategies?: {
    strategy: 'retry' | 'modify' | 'split' | 'reroute' | 'cancel'
    description: string
    likelihood: number // % success rate
    estimatedTime: number // minutes
  }[]
}

interface OrderExecutionMetrics {
  orderId: string
  symbol: string
  broker: IndianBrokerType
  submittedAt: Date
  acknowledgedAt?: Date
  filledAt?: Date
  totalExecutionTime: number // ms
  fillPrice?: number
  slippage: number // basis points
  fillQuality: 'excellent' | 'good' | 'fair' | 'poor'
  marketImpact: number // basis points
  venue: string
  // FRONT-017 Advanced Analytics
  executionPhases: {
    submission: number // ms to submit
    acknowledgment: number // ms to acknowledge
    routing: number // ms for routing decision
    translation: number // ms for order translation
    brokerProcessing: number // ms at broker
    matching: number // ms for order matching
    confirmation: number // ms for confirmation
  }
  qualityMetrics: {
    priceImprovement: number // basis points
    executionShortfall: number // basis points vs VWAP
    timingCost: number // basis points due to market movement
    opportunityCost: number // basis points vs benchmark
  }
  crossBrokerComparison: {
    bestAvailablePrice: number
    competitorLatency: number // ms
    relativeCost: number // basis points vs best
    marketShareAtTime: number // % of volume
  }
  alertsTriggered: string[] // any alerts during execution
}

interface SmartRoutingConfig {
  id: string
  name: string
  description: string
  isActive: boolean
  priority: number
  conditions: {
    symbolPattern?: string
    orderSizeMin?: number
    orderSizeMax?: number
    orderTypes?: string[]
    timeOfDay?: { start: string; end: string }
    volatilityThreshold?: number
  }
  routing: {
    brokerPreferences: { broker: IndianBrokerType; weight: number }[]
    maxSlippage: number
    maxLatency: number
    costWeight: number
    speedWeight: number
    reliabilityWeight: number
  }
}

export const AdvancedOrderManagement: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'routing' | 'dashboard' | 'translation' | 'failed' | 'metrics' | 'config'>('routing')
  const [orderRoutings, setOrderRoutings] = useState<OrderRouting[]>([])
  const [brokerMetrics, setBrokerMetrics] = useState<BrokerRoutingMetrics[]>([])
  const [orderTranslations, setOrderTranslations] = useState<OrderTranslation[]>([])
  const [failedOrders, setFailedOrders] = useState<FailedOrder[]>([])
  const [executionMetrics, setExecutionMetrics] = useState<OrderExecutionMetrics[]>([])
  const [routingConfigs, setRoutingConfigs] = useState<SmartRoutingConfig[]>([])
  const [loading, setLoading] = useState(true)
  const [searchTerm, setSearchTerm] = useState('')
  const [selectedBroker, setSelectedBroker] = useState<IndianBrokerType | 'all'>('all')
  const [notification, setNotification] = useState<{type: 'success' | 'error' | 'warning' | 'info', message: string} | null>(null)
  // FRONT-017 Enhanced State
  const [selectedRouting, setSelectedRouting] = useState<OrderRouting | null>(null)
  const [routingVisualization, setRoutingVisualization] = useState<'flow' | 'performance' | 'cost'>('flow')
  const [recoveryMode, setRecoveryMode] = useState<'assisted' | 'automatic' | 'manual'>('assisted')
  const [performanceTimeframe, setPerformanceTimeframe] = useState<'1h' | '4h' | '1d' | '1w'>('4h')
  const [realTimeUpdates, setRealTimeUpdates] = useState(true)

  // Broker configurations
  const brokerConfigs = {
    zerodha: { name: 'Zerodha Kite', icon: '🚀', color: 'blue' },
    upstox: { name: 'Upstox Pro', icon: '📈', color: 'green' },
    angel_one: { name: 'Angel One', icon: '👼', color: 'orange' },
    icici_direct: { name: 'ICICI Direct', icon: '🏦', color: 'purple' },
    groww: { name: 'Groww', icon: '🌱', color: 'emerald' },
    iifl: { name: 'IIFL Securities', icon: '💼', color: 'indigo' }
  }

  // Load mock data
  useEffect(() => {
    loadMockData()
    const interval = setInterval(() => {
      updateRealTimeData()
    }, 5000) // Update every 5 seconds

    return () => clearInterval(interval)
  }, [])

  const loadMockData = useCallback(() => {
    setLoading(true)

    // Mock Order Routings
    const mockOrderRoutings: OrderRouting[] = Array.from({ length: 25 }, (_, i) => ({
      id: `routing-${i + 1}`,
      orderId: `ORD${Date.now() + i}`,
      symbol: ['RELIANCE', 'TCS', 'HDFC BANK', 'INFOSYS', 'ICICI BANK'][Math.floor(Math.random() * 5)],
      orderType: ['market', 'limit', 'stop_loss', 'stop_limit'][Math.floor(Math.random() * 4)] as any,
      side: Math.random() > 0.5 ? 'buy' : 'sell',
      quantity: Math.floor(Math.random() * 500) + 10,
      price: Math.random() > 0.3 ? 1000 + Math.random() * 2000 : undefined,
      stopPrice: Math.random() > 0.7 ? 900 + Math.random() * 1000 : undefined,
      targetBroker: Object.keys(brokerConfigs)[Math.floor(Math.random() * 6)] as IndianBrokerType,
      routingReason: ['best_execution', 'cost_optimization', 'availability', 'speed', 'manual'][Math.floor(Math.random() * 5)] as any,
      routingScore: 70 + Math.random() * 30,
      estimatedExecutionTime: 100 + Math.random() * 300,
      estimatedCost: Math.random() * 10,
      createdAt: new Date(Date.now() - Math.random() * 3600000),
      status: ['pending', 'routed', 'executing', 'completed', 'failed'][Math.floor(Math.random() * 5)] as any
    }))

    // Mock Broker Metrics with FRONT-017 Enhancements
    const mockBrokerMetrics: BrokerRoutingMetrics[] = Object.entries(brokerConfigs).map(([brokerId, config]) => {
      const base = {
        brokerId: brokerId as IndianBrokerType,
        brokerName: config.name,
        executionSpeed: 150 + Math.random() * 200,
        successRate: 95 + Math.random() * 5,
        avgCost: Math.random() * 15,
        capacity: 100 + Math.random() * 400,
        currentLoad: Math.random() * 300,
        reliability: 98 + Math.random() * 2,
        latency: 10 + Math.random() * 50,
        lastUpdated: new Date(),
        isPreferred: Math.random() > 0.7,
        status: Math.random() > 0.1 ? 'online' : (Math.random() > 0.5 ? 'degraded' : 'offline') as any,
        // FRONT-017 Enhanced Fields
        fillRate: 92 + Math.random() * 8,
        averageSlippage: Math.random() * 5,
        priceImprovement: Math.random() * 2,
        rejectRate: Math.random() * 3,
        partialFillRate: Math.random() * 15,
        avgOrderValue: 50000 + Math.random() * 200000,
        peakLatency: (10 + Math.random() * 50) * 2,
        uptimeToday: 98 + Math.random() * 2,
        errorsByType: {
          'validation': Math.floor(Math.random() * 10),
          'network': Math.floor(Math.random() * 5),
          'authentication': Math.floor(Math.random() * 3),
          'execution': Math.floor(Math.random() * 8),
          'limit_exceeded': Math.floor(Math.random() * 4)
        }
      }
      
      // Broker-specific specializations
      const specializations = {
        zerodha: ['equity', 'options', 'futures', 'high_frequency'],
        upstox: ['equity', 'derivatives', 'algorithmic', 'mobile_trading'],
        angel_one: ['equity', 'commodity', 'mutual_funds', 'research'],
        icici_direct: ['equity', 'bonds', 'institutional', 'wealth_management'],
        groww: ['equity', 'mutual_funds', 'sip', 'beginner_friendly'],
        iifl: ['equity', 'commodity', 'currency', 'margin_trading']
      }
      
      return {
        ...base,
        specializations: specializations[brokerId as keyof typeof specializations] || ['equity']
      }
    })

    // Mock Order Translations
    const mockTranslations: OrderTranslation[] = Array.from({ length: 15 }, (_, i) => ({
      id: `translation-${i + 1}`,
      orderId: `ORD${Date.now() + i}`,
      originalFormat: {
        symbol: 'RELIANCE',
        action: 'BUY',
        quantity: 100,
        orderType: 'LIMIT',
        price: 2500
      },
      translatedFormat: {
        symbol: 'RELIANCE-EQ',
        transaction_type: 'B',
        quantity: 100,
        order_type: 'L',
        price: 2500.0
      },
      sourceBroker: 'trademaster',
      targetBroker: Object.keys(brokerConfigs)[Math.floor(Math.random() * 6)] as IndianBrokerType,
      translationStatus: ['pending', 'translating', 'completed', 'failed'][Math.floor(Math.random() * 4)] as any,
      validationErrors: Math.random() > 0.8 ? ['Invalid symbol format', 'Price out of circuit limits'] : [],
      translationTime: 10 + Math.random() * 50,
      createdAt: new Date(Date.now() - Math.random() * 1800000),
      completedAt: Math.random() > 0.3 ? new Date(Date.now() - Math.random() * 1800000) : undefined
    }))

    // Mock Failed Orders with FRONT-017 Enhanced Error Handling
    const mockFailedOrders: FailedOrder[] = Array.from({ length: 12 }, (_, i) => {
      const broker = Object.keys(brokerConfigs)[Math.floor(Math.random() * 6)] as IndianBrokerType
      const errorTypes = ['validation', 'execution', 'network', 'authentication', 'limit_exceeded', 'price_rejection', 'quantity_freeze', 'margin_shortage', 'symbol_ban']
      const errorType = errorTypes[Math.floor(Math.random() * errorTypes.length)] as any
      
      const getBrokerSpecificError = (broker: IndianBrokerType, errorType: string) => {
        const errors: any = {}
        if (broker === 'zerodha') {
          errors.zerodha = {
            rmsError: errorType === 'validation',
            freezeQty: errorType === 'quantity_freeze' ? 100 : undefined,
            availableMargin: errorType === 'margin_shortage' ? 25000 : undefined
          }
        } else if (broker === 'upstox') {
          errors.upstox = {
            positionBlocked: errorType === 'execution',
            priceOutOfRange: errorType === 'price_rejection'
          }
        } else if (broker === 'angel_one') {
          errors.angel_one = {
            sessionExpired: errorType === 'authentication',
            instrumentBlocked: errorType === 'symbol_ban'
          }
        }
        return errors
      }
      
      const getRecoveryStrategies = (errorType: string) => {
        const strategies = {
          validation: [
            { strategy: 'modify' as const, description: 'Reduce quantity or adjust price', likelihood: 85, estimatedTime: 2 },
            { strategy: 'reroute' as const, description: 'Try alternative broker', likelihood: 70, estimatedTime: 5 }
          ],
          execution: [
            { strategy: 'retry' as const, description: 'Retry with same parameters', likelihood: 60, estimatedTime: 1 },
            { strategy: 'split' as const, description: 'Split into smaller orders', likelihood: 80, estimatedTime: 10 }
          ],
          network: [
            { strategy: 'retry' as const, description: 'Automatic retry after network recovery', likelihood: 90, estimatedTime: 3 }
          ],
          price_rejection: [
            { strategy: 'modify' as const, description: 'Adjust price within circuit limits', likelihood: 95, estimatedTime: 1 }
          ]
        }
        return strategies[errorType as keyof typeof strategies] || [
          { strategy: 'retry' as const, description: 'Generic retry strategy', likelihood: 50, estimatedTime: 5 }
        ]
      }
      
      return {
        id: `failed-${i + 1}`,
        orderId: `FAIL${Date.now() + i}`,
        symbol: ['RELIANCE', 'TCS', 'HDFC BANK', 'INFOSYS', 'ICICI BANK'][Math.floor(Math.random() * 5)],
        broker,
        errorType,
        errorMessage: {
          validation: 'Order validation failed: Insufficient funds',
          execution: 'Execution failed: Market conditions changed',
          network: 'Network timeout during execution',
          authentication: 'Authentication token expired',
          limit_exceeded: 'Daily order limit exceeded',
          price_rejection: 'Price rejected: Outside circuit limits',
          quantity_freeze: 'Quantity frozen by exchange',
          margin_shortage: 'Insufficient margin for position',
          symbol_ban: 'Symbol banned for trading'
        }[errorType] || 'Unknown error occurred',
        errorCode: `${broker.toUpperCase()}_ERR_${Math.floor(Math.random() * 9999)}`,
        failedAt: new Date(Date.now() - Math.random() * 7200000),
        retryAttempts: Math.floor(Math.random() * 3),
        maxRetries: 3,
        nextRetryAt: Math.random() > 0.5 ? new Date(Date.now() + Math.random() * 3600000) : undefined,
        canRetry: !['symbol_ban', 'limit_exceeded'].includes(errorType),
        canModify: ['validation', 'price_rejection', 'quantity_freeze', 'margin_shortage'].includes(errorType),
        suggestedAction: {
          validation: 'Check account balance and reduce quantity',
          execution: 'Retry with market order or different broker',
          network: 'Check connection and retry automatically',
          authentication: 'Re-authenticate and retry order',
          limit_exceeded: 'Wait for daily limit reset',
          price_rejection: 'Adjust price within circuit limits',
          quantity_freeze: 'Reduce quantity below freeze limit',
          margin_shortage: 'Add funds or reduce position size',
          symbol_ban: 'Wait for ban removal or trade different symbol'
        }[errorType] || 'Contact support for assistance',
        originalOrder: { symbol: ['RELIANCE', 'TCS', 'HDFC BANK'][Math.floor(Math.random() * 3)], quantity: 50 + Math.floor(Math.random() * 200), price: 2000 + Math.random() * 1000 },
        // FRONT-017 Enhanced Fields
        brokerSpecificError: getBrokerSpecificError(broker, errorType),
        alternativeBrokers: (Object.keys(brokerConfigs) as IndianBrokerType[]).filter(b => b !== broker).slice(0, 2),
        modificationSuggestions: errorType === 'price_rejection' ? {
          priceRange: { min: 2450, max: 2550 },
          quantityLimit: undefined,
          timeRestriction: undefined
        } : errorType === 'quantity_freeze' ? {
          quantityLimit: 99,
          priceRange: undefined,
          timeRestriction: undefined
        } : undefined,
        recoveryStrategies: getRecoveryStrategies(errorType)
      }
    })

    // Mock Execution Metrics with FRONT-017 Advanced Analytics
    const mockExecutionMetrics: OrderExecutionMetrics[] = Array.from({ length: 20 }, (_, i) => {
      const submittedAt = new Date(Date.now() - Math.random() * 3600000)
      const acknowledgedAt = new Date(submittedAt.getTime() + 100 + Math.random() * 500)
      const filledAt = new Date(acknowledgedAt.getTime() + 1000 + Math.random() * 4000)
      const totalTime = filledAt.getTime() - submittedAt.getTime()
      
      return {
        orderId: `EXEC${Date.now() + i}`,
        symbol: ['RELIANCE', 'TCS', 'HDFC BANK', 'INFOSYS'][Math.floor(Math.random() * 4)],
        broker: Object.keys(brokerConfigs)[Math.floor(Math.random() * 6)] as IndianBrokerType,
        submittedAt,
        acknowledgedAt,
        filledAt,
        totalExecutionTime: totalTime,
        fillPrice: 2000 + Math.random() * 1000,
        slippage: Math.random() * 20,
        fillQuality: ['excellent', 'good', 'fair', 'poor'][Math.floor(Math.random() * 4)] as any,
        marketImpact: Math.random() * 15,
        venue: ['NSE', 'BSE'][Math.floor(Math.random() * 2)],
        // FRONT-017 Enhanced Analytics
        executionPhases: {
          submission: 50 + Math.random() * 100,
          acknowledgment: 20 + Math.random() * 80,
          routing: 30 + Math.random() * 70,
          translation: 15 + Math.random() * 35,
          brokerProcessing: 100 + Math.random() * 300,
          matching: 200 + Math.random() * 500,
          confirmation: 10 + Math.random() * 40
        },
        qualityMetrics: {
          priceImprovement: Math.random() * 5,
          executionShortfall: Math.random() * 8,
          timingCost: Math.random() * 3,
          opportunityCost: Math.random() * 6
        },
        crossBrokerComparison: {
          bestAvailablePrice: 2000 + Math.random() * 1000,
          competitorLatency: 150 + Math.random() * 200,
          relativeCost: Math.random() * 10,
          marketShareAtTime: 10 + Math.random() * 40
        },
        alertsTriggered: Math.random() > 0.7 ? ['High slippage detected', 'Unusual execution time'] : []
      }
    })

    // Mock Smart Routing Configs
    const mockRoutingConfigs: SmartRoutingConfig[] = [
      {
        id: 'config-1',
        name: 'High-Frequency Trading',
        description: 'Optimized for speed and low latency execution',
        isActive: true,
        priority: 1,
        conditions: {
          orderSizeMin: 1,
          orderSizeMax: 1000,
          orderTypes: ['market', 'limit']
        },
        routing: {
          brokerPreferences: [
            { broker: 'zerodha', weight: 0.4 },
            { broker: 'upstox', weight: 0.35 },
            { broker: 'angel_one', weight: 0.25 }
          ],
          maxSlippage: 5,
          maxLatency: 100,
          costWeight: 0.2,
          speedWeight: 0.6,
          reliabilityWeight: 0.2
        }
      },
      {
        id: 'config-2',
        name: 'Large Orders',
        description: 'Cost-optimized routing for large institutional orders',
        isActive: true,
        priority: 2,
        conditions: {
          orderSizeMin: 10000,
          orderTypes: ['limit', 'iceberg']
        },
        routing: {
          brokerPreferences: [
            { broker: 'icici_direct', weight: 0.5 },
            { broker: 'iifl', weight: 0.3 },
            { broker: 'zerodha', weight: 0.2 }
          ],
          maxSlippage: 15,
          maxLatency: 500,
          costWeight: 0.6,
          speedWeight: 0.2,
          reliabilityWeight: 0.2
        }
      }
    ]

    setOrderRoutings(mockOrderRoutings)
    setBrokerMetrics(mockBrokerMetrics)
    setOrderTranslations(mockTranslations)
    setFailedOrders(mockFailedOrders)
    setExecutionMetrics(mockExecutionMetrics)
    setRoutingConfigs(mockRoutingConfigs)
    setLoading(false)
  }, [])

  const updateRealTimeData = () => {
    // Update routing statuses
    setOrderRoutings(prev => prev.map(routing => {
      if (routing.status === 'pending' && Math.random() > 0.7) {
        return { ...routing, status: 'routed' }
      }
      if (routing.status === 'routed' && Math.random() > 0.6) {
        return { ...routing, status: 'executing' }
      }
      if (routing.status === 'executing' && Math.random() > 0.5) {
        return { ...routing, status: Math.random() > 0.1 ? 'completed' : 'failed' }
      }
      return routing
    }))

    // Update broker metrics
    setBrokerMetrics(prev => prev.map(metric => ({
      ...metric,
      currentLoad: Math.max(0, metric.currentLoad + (Math.random() - 0.5) * 20),
      latency: Math.max(5, metric.latency + (Math.random() - 0.5) * 10),
      lastUpdated: new Date()
    })))
  }

  // Filtering and searching
  const filteredOrderRoutings = orderRoutings.filter(routing => {
    const matchesSearch = searchTerm === '' || 
      routing.symbol.toLowerCase().includes(searchTerm.toLowerCase()) ||
      routing.orderId.toLowerCase().includes(searchTerm.toLowerCase())
    
    const matchesBroker = selectedBroker === 'all' || routing.targetBroker === selectedBroker
    
    return matchesSearch && matchesBroker
  })

  // Actions
  const retryOrder = async (orderId: string) => {
    setFailedOrders(prev => prev.map(order => 
      order.id === orderId 
        ? { ...order, retryAttempts: order.retryAttempts + 1 }
        : order
    ))
    showNotification('info', 'Order retry initiated')
  }

  const cancelOrder = async (routingId: string) => {
    setOrderRoutings(prev => prev.map(routing => 
      routing.id === routingId 
        ? { ...routing, status: 'cancelled' }
        : routing
    ))
    showNotification('success', 'Order cancelled successfully')
  }

  const showNotification = (type: 'success' | 'error' | 'warning' | 'info', message: string) => {
    setNotification({ type, message })
    setTimeout(() => setNotification(null), 5000)
  }

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'completed': case 'online': return 'text-green-400'
      case 'executing': case 'routed': case 'degraded': return 'text-yellow-400'
      case 'failed': case 'offline': return 'text-red-400'
      case 'pending': case 'translating': return 'text-blue-400'
      default: return 'text-slate-400'
    }
  }

  const getStatusBg = (status: string) => {
    switch (status) {
      case 'completed': case 'online': return 'bg-green-500/20'
      case 'executing': case 'routed': case 'degraded': return 'bg-yellow-500/20'
      case 'failed': case 'offline': return 'bg-red-500/20'
      case 'pending': case 'translating': return 'bg-blue-500/20'
      default: return 'bg-slate-500/20'
    }
  }

  const tabs = [
    { id: 'routing', label: 'Order Routing', icon: Route, count: orderRoutings.length },
    { id: 'dashboard', label: 'Visual Dashboard', icon: Monitor, count: 0 },
    { id: 'translation', label: 'Translation Status', icon: GitBranch, count: orderTranslations.length },
    { id: 'failed', label: 'Failed Orders', icon: AlertTriangle, count: failedOrders.length },
    { id: 'metrics', label: 'Execution Analytics', icon: BarChart3, count: executionMetrics.length },
    { id: 'config', label: 'Routing Config', icon: Settings, count: routingConfigs.length }
  ]

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-purple-900 to-slate-900 p-6">
      {/* Notification */}
      <AnimatePresence>
        {notification && (
          <motion.div
            initial={{ opacity: 0, y: -50 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -50 }}
            className={`fixed top-4 right-4 z-50 p-4 rounded-xl shadow-lg max-w-md ${
              notification.type === 'success' ? 'bg-green-500/20 border border-green-500/30 text-green-400' :
              notification.type === 'error' ? 'bg-red-500/20 border border-red-500/30 text-red-400' :
              notification.type === 'warning' ? 'bg-yellow-500/20 border border-yellow-500/30 text-yellow-400' :
              'bg-blue-500/20 border border-blue-500/30 text-blue-400'
            }`}
          >
            <div className="flex items-center space-x-2">
              {notification.type === 'success' && <CheckCircle className="w-5 h-5" />}
              {notification.type === 'error' && <AlertCircle className="w-5 h-5" />}
              {notification.type === 'warning' && <AlertTriangle className="w-5 h-5" />}
              {notification.type === 'info' && <Info className="w-5 h-5" />}
              <span className="font-medium">{notification.message}</span>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Header */}
      <div className="mb-8">
        <h1 className="text-4xl font-bold bg-gradient-to-r from-purple-400 to-cyan-400 bg-clip-text text-transparent mb-2">
          Advanced Order Management & Routing
        </h1>
        <p className="text-slate-400 text-lg">
          Intelligent order routing, execution monitoring, and broker optimization
        </p>
      </div>

      {/* Navigation Tabs */}
      <div className="mb-8">
        <div className="flex flex-wrap gap-2 p-1 bg-slate-800/50 rounded-xl border border-slate-700/50">
          {tabs.map(tab => {
            const Icon = tab.icon
            return (
              <motion.button
                key={tab.id}
                whileHover={{ scale: 1.02 }}
                whileTap={{ scale: 0.98 }}
                onClick={() => setActiveTab(tab.id as any)}
                className={`flex items-center space-x-2 px-4 py-3 rounded-lg font-medium transition-all duration-200 ${
                  activeTab === tab.id
                    ? 'bg-gradient-to-r from-purple-500 to-cyan-500 text-white shadow-lg'
                    : 'text-slate-400 hover:text-white hover:bg-slate-700/50'
                }`}
              >
                <Icon className="w-4 h-4" />
                <span className="whitespace-nowrap">{tab.label}</span>
                {tab.count > 0 && (
                  <span className="px-2 py-1 text-xs bg-slate-700/50 rounded-full">
                    {tab.count}
                  </span>
                )}
              </motion.button>
            )
          })}
        </div>
      </div>

      {/* Search and Filters */}
      {(activeTab === 'routing' || activeTab === 'translation' || activeTab === 'failed') && (
        <div className="mb-6 flex flex-col sm:flex-row gap-4">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-slate-400 w-4 h-4" />
            <input
              type="text"
              placeholder={`Search ${activeTab}...`}
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-10 pr-4 py-3 bg-slate-800/50 border border-slate-700/50 rounded-xl text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-purple-500/50"
            />
          </div>
          
          <select
            value={selectedBroker}
            onChange={(e) => setSelectedBroker(e.target.value as any)}
            className="px-4 py-3 bg-slate-800/50 border border-slate-700/50 rounded-xl text-white focus:outline-none focus:ring-2 focus:ring-purple-500/50"
          >
            <option value="all">All Brokers</option>
            {Object.entries(brokerConfigs).map(([brokerId, config]) => (
              <option key={brokerId} value={brokerId}>
                {config.name}
              </option>
            ))}
          </select>
        </div>
      )}

      {/* Content */}
      <div className="space-y-6">
        {activeTab === 'routing' && (
          <OrderRoutingTab 
            orderRoutings={filteredOrderRoutings}
            brokerMetrics={brokerMetrics}
            brokerConfigs={brokerConfigs}
            onCancelOrder={cancelOrder}
            loading={loading}
          />
        )}
        
        {activeTab === 'dashboard' && (
          <VisualRoutingDashboard 
            orderRoutings={orderRoutings}
            brokerMetrics={brokerMetrics}
            executionMetrics={executionMetrics}
            brokerConfigs={brokerConfigs}
            visualization={routingVisualization}
            onVisualizationChange={setRoutingVisualization}
            loading={loading}
          />
        )}
        
        {activeTab === 'translation' && (
          <TranslationStatusTab 
            translations={orderTranslations.filter(t => 
              searchTerm === '' || 
              t.orderId.toLowerCase().includes(searchTerm.toLowerCase())
            )}
            brokerConfigs={brokerConfigs}
            loading={loading}
          />
        )}
        
        {activeTab === 'failed' && (
          <FailedOrdersTab 
            failedOrders={failedOrders.filter(f => 
              (searchTerm === '' || 
               f.orderId.toLowerCase().includes(searchTerm.toLowerCase()) ||
               f.symbol.toLowerCase().includes(searchTerm.toLowerCase())) &&
              (selectedBroker === 'all' || f.broker === selectedBroker)
            )}
            brokerConfigs={brokerConfigs}
            onRetryOrder={retryOrder}
            loading={loading}
          />
        )}
        
        {activeTab === 'metrics' && (
          <ExecutionMetricsTab 
            metrics={executionMetrics}
            brokerConfigs={brokerConfigs}
            loading={loading}
          />
        )}
        
        {activeTab === 'config' && (
          <RoutingConfigTab 
            configs={routingConfigs}
            brokerConfigs={brokerConfigs}
            loading={loading}
          />
        )}
      </div>
    </div>
  )
}

// Order Routing Tab Component
interface OrderRoutingTabProps {
  orderRoutings: OrderRouting[]
  brokerMetrics: BrokerRoutingMetrics[]
  brokerConfigs: any
  onCancelOrder: (routingId: string) => void
  loading: boolean
}

const OrderRoutingTab: React.FC<OrderRoutingTabProps> = ({ 
  orderRoutings, brokerMetrics, brokerConfigs, onCancelOrder, loading 
}) => {
  if (loading) {
    return <div className="flex items-center justify-center h-64">
      <Clock className="w-8 h-8 text-purple-400 animate-spin" />
    </div>
  }

  const routingStats = {
    pending: orderRoutings.filter(o => o.status === 'pending').length,
    executing: orderRoutings.filter(o => o.status === 'executing').length,
    completed: orderRoutings.filter(o => o.status === 'completed').length,
    failed: orderRoutings.filter(o => o.status === 'failed').length
  }

  return (
    <div className="space-y-6">
      {/* Routing Stats */}
      <div className="grid gap-6 md:grid-cols-4">
        <div className="glass-card p-6 rounded-2xl">
          <div className="flex items-center justify-between mb-4">
            <div className="p-3 rounded-xl bg-gradient-to-br from-blue-500/20 to-blue-600/20">
              <Clock className="h-6 w-6 text-blue-400" />
            </div>
            <div className="text-right">
              <div className="text-2xl font-bold text-white">{routingStats.pending}</div>
            </div>
          </div>
          <h3 className="text-blue-400 font-semibold mb-1">Pending Routing</h3>
          <p className="text-slate-400 text-sm">awaiting broker selection</p>
        </div>

        <div className="glass-card p-6 rounded-2xl">
          <div className="flex items-center justify-between mb-4">
            <div className="p-3 rounded-xl bg-gradient-to-br from-yellow-500/20 to-yellow-600/20">
              <Activity className="h-6 w-6 text-yellow-400" />
            </div>
            <div className="text-right">
              <div className="text-2xl font-bold text-white">{routingStats.executing}</div>
            </div>
          </div>
          <h3 className="text-yellow-400 font-semibold mb-1">Executing</h3>
          <p className="text-slate-400 text-sm">currently processing</p>
        </div>

        <div className="glass-card p-6 rounded-2xl">
          <div className="flex items-center justify-between mb-4">
            <div className="p-3 rounded-xl bg-gradient-to-br from-green-500/20 to-green-600/20">
              <CheckCircle className="h-6 w-6 text-green-400" />
            </div>
            <div className="text-right">
              <div className="text-2xl font-bold text-white">{routingStats.completed}</div>
            </div>
          </div>
          <h3 className="text-green-400 font-semibold mb-1">Completed</h3>
          <p className="text-slate-400 text-sm">successfully executed</p>
        </div>

        <div className="glass-card p-6 rounded-2xl">
          <div className="flex items-center justify-between mb-4">
            <div className="p-3 rounded-xl bg-gradient-to-br from-red-500/20 to-red-600/20">
              <AlertTriangle className="h-6 w-6 text-red-400" />
            </div>
            <div className="text-right">
              <div className="text-2xl font-bold text-white">{routingStats.failed}</div>
            </div>
          </div>
          <h3 className="text-red-400 font-semibold mb-1">Failed</h3>
          <p className="text-slate-400 text-sm">requires attention</p>
        </div>
      </div>

      {/* Broker Performance Overview */}
      <div className="glass-card rounded-2xl p-6">
        <h3 className="text-xl font-bold text-white mb-4 flex items-center">
          <Target className="w-5 h-5 mr-2 text-purple-400" />
          Broker Routing Performance
        </h3>
        
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {brokerMetrics.map((metric, index) => (
            <motion.div
              key={metric.brokerId}
              initial={{ opacity: 0, scale: 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
              transition={{ delay: index * 0.1 }}
              className={`p-4 rounded-xl border transition-all ${
                metric.isPreferred 
                  ? 'border-purple-500/50 bg-purple-500/10' 
                  : 'border-slate-700/50 bg-slate-800/30'
              }`}
            >
              <div className="flex items-center justify-between mb-3">
                <div className="flex items-center space-x-2">
                  <span className="text-lg">{brokerConfigs[metric.brokerId]?.icon}</span>
                  <span className="text-white font-medium">{metric.brokerName}</span>
                  {metric.isPreferred && <Star className="w-4 h-4 text-yellow-400" />}
                </div>
                <span className={`px-2 py-1 rounded text-xs font-medium ${
                  metric.status === 'online' ? 'bg-green-500/20 text-green-400' :
                  metric.status === 'degraded' ? 'bg-yellow-500/20 text-yellow-400' :
                  'bg-red-500/20 text-red-400'
                }`}>
                  {metric.status}
                </span>
              </div>
              
              <div className="grid grid-cols-2 gap-2 text-sm">
                <div>
                  <p className="text-slate-400">Speed</p>
                  <p className="text-white font-semibold">{Math.round(metric.executionSpeed)}ms</p>
                </div>
                <div>
                  <p className="text-slate-400">Success</p>
                  <p className="text-white font-semibold">{metric.successRate.toFixed(1)}%</p>
                </div>
                <div>
                  <p className="text-slate-400">Load</p>
                  <p className="text-white font-semibold">{Math.round(metric.currentLoad)}/{Math.round(metric.capacity)}</p>
                </div>
                <div>
                  <p className="text-slate-400">Cost</p>
                  <p className="text-white font-semibold">{metric.avgCost.toFixed(1)}bp</p>
                </div>
              </div>
            </motion.div>
          ))}
        </div>
      </div>

      {/* Order Routings Table */}
      <div className="glass-card rounded-2xl overflow-hidden">
        <div className="p-6 border-b border-slate-700/50">
          <h3 className="text-xl font-bold text-white flex items-center">
            <Route className="w-5 h-5 mr-2 text-cyan-400" />
            Order Routing Dashboard ({orderRoutings.length})
          </h3>
        </div>
        
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-slate-800/50">
              <tr>
                <th className="text-left p-4 text-slate-400 font-medium">Order</th>
                <th className="text-left p-4 text-slate-400 font-medium">Route</th>
                <th className="text-left p-4 text-slate-400 font-medium">Routing Score</th>
                <th className="text-left p-4 text-slate-400 font-medium">Status</th>
                <th className="text-left p-4 text-slate-400 font-medium">Estimated Time</th>
                <th className="text-left p-4 text-slate-400 font-medium">Cost</th>
                <th className="text-left p-4 text-slate-400 font-medium">Actions</th>
              </tr>
            </thead>
            <tbody>
              {orderRoutings.slice(0, 15).map((routing, index) => (
                <motion.tr
                  key={routing.id}
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: index * 0.05 }}
                  className="border-b border-slate-800/50 hover:bg-slate-800/30"
                >
                  <td className="p-4">
                    <div>
                      <div className="flex items-center space-x-2 mb-1">
                        <span className="text-white font-medium">{routing.symbol}</span>
                        <span className={`px-2 py-1 text-xs rounded ${
                          routing.side === 'buy' ? 'bg-green-500/20 text-green-400' : 'bg-red-500/20 text-red-400'
                        }`}>
                          {routing.side.toUpperCase()}
                        </span>
                      </div>
                      <p className="text-slate-400 text-sm">{routing.orderId}</p>
                      <p className="text-slate-400 text-xs">
                        {routing.quantity} @ {routing.price ? `₹${routing.price.toFixed(2)}` : 'Market'}
                      </p>
                    </div>
                  </td>
                  <td className="p-4">
                    <div className="flex items-center space-x-2">
                      <div className="flex items-center space-x-1">
                        <span className="text-slate-400 text-sm">TM</span>
                        <ArrowRight className="w-3 h-3 text-slate-400" />
                        <span className="text-lg">{brokerConfigs[routing.targetBroker]?.icon}</span>
                      </div>
                    </div>
                    <p className="text-slate-400 text-xs mt-1 capitalize">
                      {routing.routingReason.replace('_', ' ')}
                    </p>
                  </td>
                  <td className="p-4">
                    <div className="flex items-center space-x-2">
                      <div className="w-full bg-slate-700 rounded-full h-2">
                        <div
                          className={`h-2 rounded-full ${
                            routing.routingScore > 85 ? 'bg-green-500' :
                            routing.routingScore > 70 ? 'bg-yellow-500' :
                            'bg-red-500'
                          }`}
                          style={{ width: `${routing.routingScore}%` }}
                        />
                      </div>
                      <span className="text-white text-sm font-medium min-w-[3rem]">
                        {Math.round(routing.routingScore)}
                      </span>
                    </div>
                  </td>
                  <td className="p-4">
                    <span className={`px-3 py-1 rounded-full text-sm font-medium flex items-center space-x-1 w-fit ${
                      getStatusBg(routing.status)
                    } ${getStatusColor(routing.status)}`}>
                      {routing.status === 'executing' && <Clock className="w-3 h-3 animate-spin" />}
                      {routing.status === 'completed' && <CheckCircle className="w-3 h-3" />}
                      {routing.status === 'failed' && <AlertTriangle className="w-3 h-3" />}
                      <span className="capitalize">{routing.status}</span>
                    </span>
                  </td>
                  <td className="p-4">
                    <span className="text-white">{routing.estimatedExecutionTime}ms</span>
                  </td>
                  <td className="p-4">
                    <span className="text-white">{routing.estimatedCost.toFixed(1)}bp</span>
                  </td>
                  <td className="p-4">
                    <div className="flex items-center space-x-2">
                      <button
                        className="p-2 text-blue-400 hover:bg-blue-500/20 rounded-lg transition-colors"
                        title="View Details"
                      >
                        <Eye className="w-4 h-4" />
                      </button>
                      
                      {(routing.status === 'pending' || routing.status === 'routed') && (
                        <button
                          onClick={() => onCancelOrder(routing.id)}
                          className="p-2 text-red-400 hover:bg-red-500/20 rounded-lg transition-colors"
                          title="Cancel Order"
                        >
                          <X className="w-4 h-4" />
                        </button>
                      )}
                    </div>
                  </td>
                </motion.tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}

// Translation Status Tab Component
interface TranslationStatusTabProps {
  translations: OrderTranslation[]
  brokerConfigs: any
  loading: boolean
}

const TranslationStatusTab: React.FC<TranslationStatusTabProps> = ({ 
  translations, brokerConfigs, loading 
}) => {
  if (loading) {
    return <div className="flex items-center justify-center h-64">
      <Clock className="w-8 h-8 text-purple-400 animate-spin" />
    </div>
  }

  return (
    <div className="space-y-6">
      {/* Translation Stats */}
      <div className="grid gap-6 md:grid-cols-4">
        <div className="glass-card p-6 rounded-2xl">
          <div className="flex items-center justify-between mb-4">
            <div className="p-3 rounded-xl bg-gradient-to-br from-green-500/20 to-green-600/20">
              <CheckCircle className="h-6 w-6 text-green-400" />
            </div>
            <div className="text-right">
              <div className="text-2xl font-bold text-white">
                {translations.filter(t => t.translationStatus === 'completed').length}
              </div>
            </div>
          </div>
          <h3 className="text-green-400 font-semibold mb-1">Completed</h3>
          <p className="text-slate-400 text-sm">successful translations</p>
        </div>

        <div className="glass-card p-6 rounded-2xl">
          <div className="flex items-center justify-between mb-4">
            <div className="p-3 rounded-xl bg-gradient-to-br from-yellow-500/20 to-yellow-600/20">
              <Clock className="h-6 w-6 text-yellow-400" />
            </div>
            <div className="text-right">
              <div className="text-2xl font-bold text-white">
                {translations.filter(t => t.translationStatus === 'translating').length}
              </div>
            </div>
          </div>
          <h3 className="text-yellow-400 font-semibold mb-1">In Progress</h3>
          <p className="text-slate-400 text-sm">currently translating</p>
        </div>

        <div className="glass-card p-6 rounded-2xl">
          <div className="flex items-center justify-between mb-4">
            <div className="p-3 rounded-xl bg-gradient-to-br from-red-500/20 to-red-600/20">
              <AlertTriangle className="h-6 w-6 text-red-400" />
            </div>
            <div className="text-right">
              <div className="text-2xl font-bold text-white">
                {translations.filter(t => t.translationStatus === 'failed' || t.translationStatus === 'validation_error').length}
              </div>
            </div>
          </div>
          <h3 className="text-red-400 font-semibold mb-1">Failed</h3>
          <p className="text-slate-400 text-sm">translation errors</p>
        </div>

        <div className="glass-card p-6 rounded-2xl">
          <div className="flex items-center justify-between mb-4">
            <div className="p-3 rounded-xl bg-gradient-to-br from-blue-500/20 to-blue-600/20">
              <Timer className="h-6 w-6 text-blue-400" />
            </div>
            <div className="text-right">
              <div className="text-2xl font-bold text-white">
                {Math.round(translations.reduce((sum, t) => sum + t.translationTime, 0) / translations.length)}ms
              </div>
            </div>
          </div>
          <h3 className="text-blue-400 font-semibold mb-1">Avg Time</h3>
          <p className="text-slate-400 text-sm">translation speed</p>
        </div>
      </div>

      {/* Translation Details Table */}
      <div className="glass-card rounded-2xl overflow-hidden">
        <div className="p-6 border-b border-slate-700/50">
          <h3 className="text-xl font-bold text-white flex items-center">
            <GitBranch className="w-5 h-5 mr-2 text-orange-400" />
            Order Translation Status ({translations.length})
          </h3>
        </div>
        
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-slate-800/50">
              <tr>
                <th className="text-left p-4 text-slate-400 font-medium">Order</th>
                <th className="text-left p-4 text-slate-400 font-medium">Translation</th>
                <th className="text-left p-4 text-slate-400 font-medium">Status</th>
                <th className="text-left p-4 text-slate-400 font-medium">Time</th>
                <th className="text-left p-4 text-slate-400 font-medium">Errors</th>
                <th className="text-left p-4 text-slate-400 font-medium">Actions</th>
              </tr>
            </thead>
            <tbody>
              {translations.map((translation, index) => (
                <motion.tr
                  key={translation.id}
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: index * 0.05 }}
                  className="border-b border-slate-800/50 hover:bg-slate-800/30"
                >
                  <td className="p-4">
                    <div>
                      <p className="text-white font-medium">{translation.orderId}</p>
                      <p className="text-slate-400 text-sm">{translation.originalFormat.symbol}</p>
                    </div>
                  </td>
                  <td className="p-4">
                    <div className="flex items-center space-x-2">
                      <span className="text-slate-400 text-sm">TM</span>
                      <ArrowRight className="w-3 h-3 text-slate-400" />
                      <span className="text-lg">{brokerConfigs[translation.targetBroker]?.icon}</span>
                      <span className="text-white text-sm">{brokerConfigs[translation.targetBroker]?.name}</span>
                    </div>
                  </td>
                  <td className="p-4">
                    <span className={`px-3 py-1 rounded-full text-sm font-medium flex items-center space-x-1 w-fit ${
                      translation.translationStatus === 'completed' ? 'bg-green-500/20 text-green-400' :
                      translation.translationStatus === 'translating' ? 'bg-yellow-500/20 text-yellow-400' :
                      translation.translationStatus === 'failed' || translation.translationStatus === 'validation_error' ? 'bg-red-500/20 text-red-400' :
                      'bg-blue-500/20 text-blue-400'
                    }`}>
                      {translation.translationStatus === 'translating' && <Clock className="w-3 h-3 animate-spin" />}
                      {translation.translationStatus === 'completed' && <CheckCircle className="w-3 h-3" />}
                      {(translation.translationStatus === 'failed' || translation.translationStatus === 'validation_error') && <AlertTriangle className="w-3 h-3" />}
                      <span className="capitalize">{translation.translationStatus.replace('_', ' ')}</span>
                    </span>
                  </td>
                  <td className="p-4">
                    <span className="text-white">{translation.translationTime}ms</span>
                  </td>
                  <td className="p-4">
                    {translation.validationErrors.length > 0 ? (
                      <div className="space-y-1">
                        {translation.validationErrors.slice(0, 2).map((error, idx) => (
                          <p key={idx} className="text-red-400 text-sm">{error}</p>
                        ))}
                        {translation.validationErrors.length > 2 && (
                          <p className="text-slate-400 text-xs">+{translation.validationErrors.length - 2} more</p>
                        )}
                      </div>
                    ) : (
                      <span className="text-slate-400">None</span>
                    )}
                  </td>
                  <td className="p-4">
                    <div className="flex items-center space-x-2">
                      <button
                        className="p-2 text-blue-400 hover:bg-blue-500/20 rounded-lg transition-colors"
                        title="View Translation Details"
                      >
                        <Eye className="w-4 h-4" />
                      </button>
                    </div>
                  </td>
                </motion.tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}

// Failed Orders Tab Component
interface FailedOrdersTabProps {
  failedOrders: FailedOrder[]
  brokerConfigs: any
  onRetryOrder: (orderId: string) => void
  loading: boolean
}

const FailedOrdersTab: React.FC<FailedOrdersTabProps> = ({ 
  failedOrders, brokerConfigs, onRetryOrder, loading 
}) => {
  const [selectedOrder, setSelectedOrder] = useState<FailedOrder | null>(null)
  const [recoveryMode, setRecoveryMode] = useState<'assisted' | 'manual'>('assisted')
  
  if (loading) {
    return <div className="flex items-center justify-center h-64">
      <Clock className="w-8 h-8 text-purple-400 animate-spin" />
    </div>
  }

  // FRONT-017: Enhanced error stats with new error types
  const errorStats = {
    validation: failedOrders.filter(o => o.errorType === 'validation').length,
    execution: failedOrders.filter(o => o.errorType === 'execution').length,
    network: failedOrders.filter(o => o.errorType === 'network').length,
    authentication: failedOrders.filter(o => o.errorType === 'authentication').length,
    limit_exceeded: failedOrders.filter(o => o.errorType === 'limit_exceeded').length,
    price_rejection: failedOrders.filter(o => o.errorType === 'price_rejection').length,
    quantity_freeze: failedOrders.filter(o => o.errorType === 'quantity_freeze').length,
    margin_shortage: failedOrders.filter(o => o.errorType === 'margin_shortage').length,
    symbol_ban: failedOrders.filter(o => o.errorType === 'symbol_ban').length
  }
  
  // Get broker-specific error insights
  const getBrokerErrorInsight = (order: FailedOrder) => {
    const brokerError = order.brokerSpecificError?.[order.broker]
    if (!brokerError) return null
    
    const insights = []
    if (order.broker === 'zerodha') {
      const zerodhaError = brokerError as any
      if (zerodhaError.rmsError) insights.push('RMS validation failed')
      if (zerodhaError.freezeQty) insights.push(`Freeze qty: ${zerodhaError.freezeQty}`)
      if (zerodhaError.availableMargin) insights.push(`Available margin: ₹${zerodhaError.availableMargin}`)
    } else if (order.broker === 'upstox') {
      const upstoxError = brokerError as any
      if (upstoxError.positionBlocked) insights.push('Position blocked')
      if (upstoxError.priceOutOfRange) insights.push('Price out of range')
    } else if (order.broker === 'angel_one') {
      const angelError = brokerError as any
      if (angelError.sessionExpired) insights.push('Session expired')
      if (angelError.instrumentBlocked) insights.push('Instrument blocked')
    }
    
    return insights.length > 0 ? insights : null
  }

  return (
    <div className="space-y-6">
      {/* FRONT-017: Enhanced Error Type Distribution */}
      <div className="grid gap-3 grid-cols-2 md:grid-cols-3 lg:grid-cols-5 xl:grid-cols-9">
        {Object.entries(errorStats).map(([type, count]) => (
          <div key={type} className="glass-card p-4 rounded-xl">
            <div className="text-center">
              <div className="text-2xl font-bold text-white mb-1">{count}</div>
              <p className="text-slate-400 text-sm capitalize">{type.replace('_', ' ')}</p>
            </div>
          </div>
        ))}
      </div>

      {/* Failed Orders Table */}
      <div className="glass-card rounded-2xl overflow-hidden">
        <div className="p-6 border-b border-slate-700/50">
          <h3 className="text-xl font-bold text-white flex items-center">
            <AlertTriangle className="w-5 h-5 mr-2 text-red-400" />
            Failed Orders Recovery ({failedOrders.length})
          </h3>
        </div>
        
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-slate-800/50">
              <tr>
                <th className="text-left p-4 text-slate-400 font-medium">Order</th>
                <th className="text-left p-4 text-slate-400 font-medium">Broker</th>
                <th className="text-left p-4 text-slate-400 font-medium">Error</th>
                <th className="text-left p-4 text-slate-400 font-medium">Failed At</th>
                <th className="text-left p-4 text-slate-400 font-medium">Retries</th>
                <th className="text-left p-4 text-slate-400 font-medium">Suggested Action</th>
                <th className="text-left p-4 text-slate-400 font-medium">Actions</th>
              </tr>
            </thead>
            <tbody>
              {failedOrders.map((order, index) => (
                <motion.tr
                  key={order.id}
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: index * 0.05 }}
                  className="border-b border-slate-800/50 hover:bg-slate-800/30"
                >
                  <td className="p-4">
                    <div>
                      <p className="text-white font-medium">{order.orderId}</p>
                      <p className="text-slate-400 text-sm">{order.symbol}</p>
                    </div>
                  </td>
                  <td className="p-4">
                    <div className="flex items-center space-x-2">
                      <span className="text-lg">{brokerConfigs[order.broker]?.icon}</span>
                      <span className="text-white">{brokerConfigs[order.broker]?.name}</span>
                    </div>
                  </td>
                  <td className="p-4">
                    <div>
                      <span className={`px-2 py-1 rounded text-xs font-medium capitalize ${
                        order.errorType === 'validation' ? 'bg-orange-500/20 text-orange-400' :
                        order.errorType === 'execution' ? 'bg-red-500/20 text-red-400' :
                        order.errorType === 'network' ? 'bg-yellow-500/20 text-yellow-400' :
                        order.errorType === 'authentication' ? 'bg-purple-500/20 text-purple-400' :
                        order.errorType === 'price_rejection' ? 'bg-pink-500/20 text-pink-400' :
                        order.errorType === 'quantity_freeze' ? 'bg-cyan-500/20 text-cyan-400' :
                        order.errorType === 'margin_shortage' ? 'bg-indigo-500/20 text-indigo-400' :
                        order.errorType === 'symbol_ban' ? 'bg-gray-500/20 text-gray-400' :
                        'bg-blue-500/20 text-blue-400'
                      }`}>
                        {order.errorType.replace('_', ' ')}
                      </span>
                      <p className="text-slate-400 text-sm mt-1">{order.errorMessage}</p>
                      {order.errorCode && (
                        <p className="text-slate-500 text-xs">Code: {order.errorCode}</p>
                      )}
                      
                      {/* FRONT-017: Broker-Specific Error Details */}
                      {getBrokerErrorInsight(order) && (
                        <div className="mt-2 p-2 bg-slate-800/50 rounded text-xs">
                          <div className="text-yellow-400 font-semibold mb-1">{brokerConfigs[order.broker]?.name} Details:</div>
                          {getBrokerErrorInsight(order)!.map((insight, i) => (
                            <div key={i} className="text-slate-300 flex items-center gap-1">
                              <div className="w-1 h-1 bg-yellow-400 rounded-full" />
                              {insight}
                            </div>
                          ))}
                        </div>
                      )}
                      
                      {/* Alternative Brokers */}
                      {order.alternativeBrokers && order.alternativeBrokers.length > 0 && (
                        <div className="mt-2">
                          <div className="text-xs text-slate-400 mb-1">Alternative brokers:</div>
                          <div className="flex gap-1">
                            {order.alternativeBrokers.map(altBroker => (
                              <span key={altBroker} className="text-xs px-1 py-0.5 bg-green-500/20 text-green-400 rounded">
                                {brokerConfigs[altBroker]?.name || altBroker}
                              </span>
                            ))}
                          </div>
                        </div>
                      )}
                    </div>
                  </td>
                  <td className="p-4">
                    <p className="text-slate-300 text-sm">{order.failedAt.toLocaleString()}</p>
                  </td>
                  <td className="p-4">
                    <div>
                      <p className="text-white">{order.retryAttempts} / {order.maxRetries}</p>
                      {order.nextRetryAt && (
                        <p className="text-slate-400 text-xs">
                          Next: {order.nextRetryAt.toLocaleTimeString()}
                        </p>
                      )}
                    </div>
                  </td>
                  <td className="p-4">
                    <div className="space-y-2">
                      <p className="text-slate-300 text-sm">{order.suggestedAction}</p>
                      
                      {/* FRONT-017: Recovery Strategies */}
                      {order.recoveryStrategies && order.recoveryStrategies.length > 0 && (
                        <div className="space-y-1">
                          {order.recoveryStrategies.slice(0, 2).map((strategy, i) => (
                            <div key={i} className="flex items-center gap-2 text-xs">
                              <div className={`w-1.5 h-1.5 rounded-full ${
                                strategy.likelihood >= 80 ? 'bg-green-400' :
                                strategy.likelihood >= 60 ? 'bg-yellow-400' : 'bg-red-400'
                              }`} />
                              <span className="text-slate-400">{strategy.strategy}:</span>
                              <span className="text-white">{strategy.likelihood}% ({strategy.estimatedTime}min)</span>
                            </div>
                          ))}
                        </div>
                      )}
                      
                      {/* Modification Suggestions */}
                      {order.modificationSuggestions && (
                        <div className="mt-2 p-2 bg-blue-500/10 rounded text-xs">
                          <div className="text-blue-400 font-semibold mb-1">Suggested changes:</div>
                          {order.modificationSuggestions.priceRange && (
                            <div className="text-slate-300">
                              Price: ₹{order.modificationSuggestions.priceRange.min} - ₹{order.modificationSuggestions.priceRange.max}
                            </div>
                          )}
                          {order.modificationSuggestions.quantityLimit && (
                            <div className="text-slate-300">
                              Max quantity: {order.modificationSuggestions.quantityLimit}
                            </div>
                          )}
                        </div>
                      )}
                    </div>
                  </td>
                  <td className="p-4">
                    <div className="flex items-center space-x-2">
                      {/* Smart Retry - with best recovery strategy */}
                      {order.canRetry && order.recoveryStrategies && order.recoveryStrategies.length > 0 && (
                        <motion.button
                          whileHover={{ scale: 1.05 }}
                          whileTap={{ scale: 0.95 }}
                          onClick={() => onRetryOrder(order.id)}
                          className="flex items-center gap-1 px-2 py-1 text-xs bg-green-500/20 text-green-400 hover:bg-green-500/30 rounded-lg transition-colors"
                          title={`Smart retry using ${order.recoveryStrategies[0].strategy} strategy (${order.recoveryStrategies[0].likelihood}% success)`}
                        >
                          <Zap className="w-3 h-3" />
                          <span className="hidden sm:inline">Smart Retry</span>
                        </motion.button>
                      )}
                      
                      {/* Standard Retry */}
                      {order.canRetry && (!order.recoveryStrategies || order.recoveryStrategies.length === 0) && (
                        <button
                          onClick={() => onRetryOrder(order.id)}
                          className="p-2 text-green-400 hover:bg-green-500/20 rounded-lg transition-colors"
                          title="Retry Order"
                        >
                          <RefreshCw className="w-4 h-4" />
                        </button>
                      )}
                      
                      {/* Modify with suggestions */}
                      {order.canModify && (
                        <button
                          className="p-2 text-blue-400 hover:bg-blue-500/20 rounded-lg transition-colors"
                          title={order.modificationSuggestions ? 'Modify with AI suggestions' : 'Modify Order'}
                        >
                          <Edit3 className="w-4 h-4" />
                        </button>
                      )}
                      
                      {/* Reroute to alternative broker */}
                      {order.alternativeBrokers && order.alternativeBrokers.length > 0 && (
                        <button
                          className="p-2 text-purple-400 hover:bg-purple-500/20 rounded-lg transition-colors"
                          title={`Reroute to ${order.alternativeBrokers.map(b => brokerConfigs[b]?.name || b).join(', ')}`}
                        >
                          <Shuffle className="w-4 h-4" />
                        </button>
                      )}
                      
                      {/* Split order */}
                      {order.recoveryStrategies?.some(s => s.strategy === 'split') && (
                        <button
                          className="p-2 text-orange-400 hover:bg-orange-500/20 rounded-lg transition-colors"
                          title="Split into smaller orders"
                        >
                          <GitBranch className="w-4 h-4" />
                        </button>
                      )}

                      <button
                        onClick={() => setSelectedOrder(order)}
                        className="p-2 text-slate-400 hover:bg-slate-500/20 rounded-lg transition-colors"
                        title="View Recovery Details"
                      >
                        <Eye className="w-4 h-4" />
                      </button>
                    </div>
                  </td>
                </motion.tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
      
      {/* FRONT-017: Failed Order Recovery Modal */}
      <AnimatePresence>
        {selectedOrder && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center z-50 p-4"
            onClick={() => setSelectedOrder(null)}
          >
            <motion.div
              initial={{ scale: 0.9, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.9, opacity: 0 }}
              className="glass-card p-6 rounded-2xl max-w-2xl w-full max-h-[80vh] overflow-y-auto"
              onClick={(e) => e.stopPropagation()}
            >
              <div className="flex items-center justify-between mb-6">
                <h3 className="text-xl font-bold text-white flex items-center gap-2">
                  <AlertTriangle className="w-5 h-5 text-red-400" />
                  Order Recovery Assistant
                </h3>
                <button
                  onClick={() => setSelectedOrder(null)}
                  className="p-2 text-slate-400 hover:text-white hover:bg-slate-700/50 rounded-lg transition-colors"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>
              
              {/* Order Details */}
              <div className="grid grid-cols-2 gap-4 mb-6 p-4 bg-slate-800/30 rounded-xl">
                <div>
                  <div className="text-slate-400 text-sm">Order ID</div>
                  <div className="text-white font-mono">{selectedOrder.orderId}</div>
                </div>
                <div>
                  <div className="text-slate-400 text-sm">Symbol</div>
                  <div className="text-white font-semibold">{selectedOrder.symbol}</div>
                </div>
                <div>
                  <div className="text-slate-400 text-sm">Broker</div>
                  <div className="text-white">{brokerConfigs[selectedOrder.broker]?.name}</div>
                </div>
                <div>
                  <div className="text-slate-400 text-sm">Error Type</div>
                  <div className="text-red-400 capitalize">{selectedOrder.errorType.replace('_', ' ')}</div>
                </div>
              </div>
              
              {/* Recovery Strategies */}
              {selectedOrder.recoveryStrategies && selectedOrder.recoveryStrategies.length > 0 && (
                <div className="mb-6">
                  <h4 className="text-white font-semibold mb-3 flex items-center gap-2">
                    <Target className="w-4 h-4 text-green-400" />
                    AI Recovery Strategies
                  </h4>
                  <div className="space-y-3">
                    {selectedOrder.recoveryStrategies.map((strategy, i) => (
                      <motion.div
                        key={i}
                        whileHover={{ scale: 1.02 }}
                        className={`p-4 rounded-xl border cursor-pointer transition-all ${
                          strategy.likelihood >= 80 ? 'bg-green-500/10 border-green-500/30 hover:bg-green-500/20' :
                          strategy.likelihood >= 60 ? 'bg-yellow-500/10 border-yellow-500/30 hover:bg-yellow-500/20' :
                          'bg-red-500/10 border-red-500/30 hover:bg-red-500/20'
                        }`}
                      >
                        <div className="flex items-center justify-between mb-2">
                          <div className="flex items-center gap-2">
                            <div className={`w-2 h-2 rounded-full ${
                              strategy.likelihood >= 80 ? 'bg-green-400' :
                              strategy.likelihood >= 60 ? 'bg-yellow-400' : 'bg-red-400'
                            }`} />
                            <span className="text-white font-semibold capitalize">{strategy.strategy}</span>
                          </div>
                          <div className="flex items-center gap-3 text-sm">
                            <span className={`${
                              strategy.likelihood >= 80 ? 'text-green-400' :
                              strategy.likelihood >= 60 ? 'text-yellow-400' : 'text-red-400'
                            }`}>{strategy.likelihood}% success</span>
                            <span className="text-slate-400">{strategy.estimatedTime}min</span>
                          </div>
                        </div>
                        <p className="text-slate-300 text-sm">{strategy.description}</p>
                      </motion.div>
                    ))}
                  </div>
                </div>
              )}
              
              {/* Alternative Brokers */}
              {selectedOrder.alternativeBrokers && selectedOrder.alternativeBrokers.length > 0 && (
                <div className="mb-6">
                  <h4 className="text-white font-semibold mb-3 flex items-center gap-2">
                    <Shuffle className="w-4 h-4 text-purple-400" />
                    Alternative Brokers
                  </h4>
                  <div className="grid grid-cols-2 gap-3">
                    {selectedOrder.alternativeBrokers.map(brokerId => (
                      <div key={brokerId} className="p-3 bg-slate-800/30 rounded-lg flex items-center gap-3">
                        <span className="text-lg">{brokerConfigs[brokerId]?.icon}</span>
                        <div>
                          <div className="text-white font-semibold text-sm">{brokerConfigs[brokerId]?.name}</div>
                          <div className="text-slate-400 text-xs">Available for rerouting</div>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}
              
              {/* Action Buttons */}
              <div className="flex gap-3 pt-4 border-t border-slate-700/50">
                {selectedOrder.canRetry && (
                  <motion.button
                    whileHover={{ scale: 1.02 }}
                    whileTap={{ scale: 0.98 }}
                    onClick={() => {
                      onRetryOrder(selectedOrder.id)
                      setSelectedOrder(null)
                    }}
                    className="flex-1 flex items-center justify-center gap-2 py-3 bg-green-500/20 text-green-400 hover:bg-green-500/30 rounded-xl transition-colors"
                  >
                    <Zap className="w-4 h-4" />
                    Execute Best Strategy
                  </motion.button>
                )}
                
                {selectedOrder.canModify && (
                  <motion.button
                    whileHover={{ scale: 1.02 }}
                    whileTap={{ scale: 0.98 }}
                    className="flex-1 flex items-center justify-center gap-2 py-3 bg-blue-500/20 text-blue-400 hover:bg-blue-500/30 rounded-xl transition-colors"
                  >
                    <Edit3 className="w-4 h-4" />
                    Modify Order
                  </motion.button>
                )}
                
                <motion.button
                  whileHover={{ scale: 1.02 }}
                  whileTap={{ scale: 0.98 }}
                  onClick={() => setSelectedOrder(null)}
                  className="px-6 py-3 bg-slate-700/50 text-slate-300 hover:bg-slate-700 rounded-xl transition-colors"
                >
                  Close
                </motion.button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  )
}

// Execution Metrics Tab Component
interface ExecutionMetricsTabProps {
  metrics: OrderExecutionMetrics[]
  brokerConfigs: any
  loading: boolean
}

const ExecutionMetricsTab: React.FC<ExecutionMetricsTabProps> = ({ 
  metrics, brokerConfigs, loading 
}) => {
  if (loading) {
    return <div className="flex items-center justify-center h-64">
      <Clock className="w-8 h-8 text-purple-400 animate-spin" />
    </div>
  }

  const avgExecutionTime = metrics.reduce((sum, m) => sum + m.totalExecutionTime, 0) / metrics.length
  const avgSlippage = metrics.reduce((sum, m) => sum + m.slippage, 0) / metrics.length
  const fillQualityDistribution = {
    excellent: metrics.filter(m => m.fillQuality === 'excellent').length,
    good: metrics.filter(m => m.fillQuality === 'good').length,
    fair: metrics.filter(m => m.fillQuality === 'fair').length,
    poor: metrics.filter(m => m.fillQuality === 'poor').length
  }

  return (
    <div className="space-y-6">
      {/* Execution Overview */}
      <div className="grid gap-6 md:grid-cols-4">
        <div className="glass-card p-6 rounded-2xl">
          <div className="flex items-center justify-between mb-4">
            <div className="p-3 rounded-xl bg-gradient-to-br from-blue-500/20 to-blue-600/20">
              <Timer className="h-6 w-6 text-blue-400" />
            </div>
            <div className="text-right">
              <div className="text-2xl font-bold text-white">{Math.round(avgExecutionTime)}ms</div>
            </div>
          </div>
          <h3 className="text-blue-400 font-semibold mb-1">Avg Execution Time</h3>
          <p className="text-slate-400 text-sm">end-to-end latency</p>
        </div>

        <div className="glass-card p-6 rounded-2xl">
          <div className="flex items-center justify-between mb-4">
            <div className="p-3 rounded-xl bg-gradient-to-br from-orange-500/20 to-orange-600/20">
              <TrendingDown className="h-6 w-6 text-orange-400" />
            </div>
            <div className="text-right">
              <div className="text-2xl font-bold text-white">{avgSlippage.toFixed(1)}bp</div>
            </div>
          </div>
          <h3 className="text-orange-400 font-semibold mb-1">Avg Slippage</h3>
          <p className="text-slate-400 text-sm">price deviation</p>
        </div>

        <div className="glass-card p-6 rounded-2xl">
          <div className="flex items-center justify-between mb-4">
            <div className="p-3 rounded-xl bg-gradient-to-br from-green-500/20 to-green-600/20">
              <CheckCircle className="h-6 w-6 text-green-400" />
            </div>
            <div className="text-right">
              <div className="text-2xl font-bold text-white">
                {Math.round(((fillQualityDistribution.excellent + fillQualityDistribution.good) / metrics.length) * 100)}%
              </div>
            </div>
          </div>
          <h3 className="text-green-400 font-semibold mb-1">Good+ Fills</h3>
          <p className="text-slate-400 text-sm">quality executions</p>
        </div>

        <div className="glass-card p-6 rounded-2xl">
          <div className="flex items-center justify-between mb-4">
            <div className="p-3 rounded-xl bg-gradient-to-br from-purple-500/20 to-purple-600/20">
              <Activity className="h-6 w-6 text-purple-400" />
            </div>
            <div className="text-right">
              <div className="text-2xl font-bold text-white">{metrics.length}</div>
            </div>
          </div>
          <h3 className="text-purple-400 font-semibold mb-1">Total Orders</h3>
          <p className="text-slate-400 text-sm">executed today</p>
        </div>
      </div>

      {/* Execution Metrics Table */}
      <div className="glass-card rounded-2xl overflow-hidden">
        <div className="p-6 border-b border-slate-700/50">
          <h3 className="text-xl font-bold text-white flex items-center">
            <BarChart3 className="w-5 h-5 mr-2 text-green-400" />
            Order Execution Performance ({metrics.length})
          </h3>
        </div>
        
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-slate-800/50">
              <tr>
                <th className="text-left p-4 text-slate-400 font-medium">Order</th>
                <th className="text-left p-4 text-slate-400 font-medium">Broker</th>
                <th className="text-left p-4 text-slate-400 font-medium">Execution Time</th>
                <th className="text-left p-4 text-slate-400 font-medium">Fill Price</th>
                <th className="text-left p-4 text-slate-400 font-medium">Slippage</th>
                <th className="text-left p-4 text-slate-400 font-medium">Quality</th>
                <th className="text-left p-4 text-slate-400 font-medium">Market Impact</th>
              </tr>
            </thead>
            <tbody>
              {metrics.slice(0, 15).map((metric, index) => (
                <motion.tr
                  key={metric.orderId}
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: index * 0.05 }}
                  className="border-b border-slate-800/50 hover:bg-slate-800/30"
                >
                  <td className="p-4">
                    <div>
                      <p className="text-white font-medium">{metric.orderId}</p>
                      <p className="text-slate-400 text-sm">{metric.symbol}</p>
                    </div>
                  </td>
                  <td className="p-4">
                    <div className="flex items-center space-x-2">
                      <span className="text-lg">{brokerConfigs[metric.broker]?.icon}</span>
                      <div>
                        <p className="text-white text-sm">{brokerConfigs[metric.broker]?.name}</p>
                        <p className="text-slate-400 text-xs">{metric.venue}</p>
                      </div>
                    </div>
                  </td>
                  <td className="p-4">
                    <span className={`text-sm font-medium ${
                      metric.totalExecutionTime < 2000 ? 'text-green-400' :
                      metric.totalExecutionTime < 5000 ? 'text-yellow-400' :
                      'text-red-400'
                    }`}>
                      {Math.round(metric.totalExecutionTime)}ms
                    </span>
                  </td>
                  <td className="p-4">
                    {metric.fillPrice ? (
                      <span className="text-white">₹{metric.fillPrice.toFixed(2)}</span>
                    ) : (
                      <span className="text-slate-400">-</span>
                    )}
                  </td>
                  <td className="p-4">
                    <span className={`text-sm font-medium ${
                      metric.slippage < 5 ? 'text-green-400' :
                      metric.slippage < 15 ? 'text-yellow-400' :
                      'text-red-400'
                    }`}>
                      {metric.slippage.toFixed(1)}bp
                    </span>
                  </td>
                  <td className="p-4">
                    <span className={`px-2 py-1 rounded text-xs font-medium ${
                      metric.fillQuality === 'excellent' ? 'bg-green-500/20 text-green-400' :
                      metric.fillQuality === 'good' ? 'bg-blue-500/20 text-blue-400' :
                      metric.fillQuality === 'fair' ? 'bg-yellow-500/20 text-yellow-400' :
                      'bg-red-500/20 text-red-400'
                    }`}>
                      {metric.fillQuality}
                    </span>
                  </td>
                  <td className="p-4">
                    <span className="text-white text-sm">{metric.marketImpact.toFixed(1)}bp</span>
                  </td>
                </motion.tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}

// Routing Configuration Tab Component
interface RoutingConfigTabProps {
  configs: SmartRoutingConfig[]
  brokerConfigs: any
  loading: boolean
}

const RoutingConfigTab: React.FC<RoutingConfigTabProps> = ({ 
  configs, brokerConfigs, loading 
}) => {
  if (loading) {
    return <div className="flex items-center justify-center h-64">
      <Clock className="w-8 h-8 text-purple-400 animate-spin" />
    </div>
  }

  return (
    <div className="space-y-6">
      {/* Configuration Cards */}
      <div className="grid gap-6 md:grid-cols-2">
        {configs.map((config, index) => (
          <motion.div
            key={config.id}
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ delay: index * 0.1 }}
            className={`glass-card p-6 rounded-2xl border ${
              config.isActive 
                ? 'border-green-500/50 bg-green-500/5' 
                : 'border-slate-700/50'
            }`}
          >
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center space-x-3">
                <div className={`p-2 rounded-lg ${
                  config.isActive ? 'bg-green-500/20' : 'bg-slate-700/50'
                }`}>
                  <Settings className={`w-5 h-5 ${
                    config.isActive ? 'text-green-400' : 'text-slate-400'
                  }`} />
                </div>
                <div>
                  <h3 className="text-white font-semibold">{config.name}</h3>
                  <p className="text-slate-400 text-sm">Priority {config.priority}</p>
                </div>
              </div>
              <span className={`px-3 py-1 rounded-full text-sm font-medium ${
                config.isActive 
                  ? 'bg-green-500/20 text-green-400' 
                  : 'bg-slate-500/20 text-slate-400'
              }`}>
                {config.isActive ? 'Active' : 'Inactive'}
              </span>
            </div>

            <p className="text-slate-300 text-sm mb-4">{config.description}</p>

            <div className="space-y-3">
              <div>
                <h4 className="text-slate-400 text-sm mb-2">Broker Preferences</h4>
                <div className="space-y-1">
                  {config.routing.brokerPreferences.map((pref) => (
                    <div key={pref.broker} className="flex items-center justify-between">
                      <div className="flex items-center space-x-2">
                        <span className="text-sm">{brokerConfigs[pref.broker]?.icon}</span>
                        <span className="text-slate-300 text-sm">{brokerConfigs[pref.broker]?.name}</span>
                      </div>
                      <div className="flex items-center space-x-2">
                        <div className="w-16 bg-slate-700 rounded-full h-2">
                          <div
                            className="bg-blue-500 h-2 rounded-full"
                            style={{ width: `${pref.weight * 100}%` }}
                          />
                        </div>
                        <span className="text-slate-400 text-xs min-w-[3rem]">
                          {Math.round(pref.weight * 100)}%
                        </span>
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              <div className="grid grid-cols-3 gap-3 text-sm">
                <div>
                  <p className="text-slate-400">Max Slippage</p>
                  <p className="text-white font-semibold">{config.routing.maxSlippage}bp</p>
                </div>
                <div>
                  <p className="text-slate-400">Max Latency</p>
                  <p className="text-white font-semibold">{config.routing.maxLatency}ms</p>
                </div>
                <div>
                  <p className="text-slate-400">Weights</p>
                  <p className="text-white font-semibold text-xs">
                    C:{Math.round(config.routing.costWeight * 100)} 
                    S:{Math.round(config.routing.speedWeight * 100)} 
                    R:{Math.round(config.routing.reliabilityWeight * 100)}
                  </p>
                </div>
              </div>
            </div>
          </motion.div>
        ))}
      </div>
    </div>
  )
}

// FRONT-017: Visual Routing Dashboard Component
const VisualRoutingDashboard: React.FC<{
  orderRoutings: OrderRouting[]
  brokerMetrics: BrokerRoutingMetrics[]
  executionMetrics: OrderExecutionMetrics[]
  brokerConfigs: any
  visualization: 'flow' | 'performance' | 'cost'
  onVisualizationChange: (mode: 'flow' | 'performance' | 'cost') => void
  loading: boolean
}> = ({ orderRoutings, brokerMetrics, executionMetrics, brokerConfigs, visualization, onVisualizationChange, loading }) => {
  const [selectedBroker, setSelectedBroker] = useState<IndianBrokerType | 'all'>('all')
  const [timeframe, setTimeframe] = useState<'1h' | '4h' | '1d'>('4h')

  // Calculate routing flow metrics
  const routingFlowData = useMemo(() => {
    const brokerStats = brokerMetrics.reduce((acc, broker) => {
      acc[broker.brokerId] = {
        orders: orderRoutings.filter(o => o.targetBroker === broker.brokerId).length,
        success: Math.round(broker.successRate),
        avgCost: broker.avgCost,
        latency: broker.latency,
        load: Math.round((broker.currentLoad / broker.capacity) * 100),
        fillRate: broker.fillRate || 95,
        specializations: broker.specializations || []
      }
      return acc
    }, {} as Record<string, any>)

    return Object.entries(brokerStats).map(([brokerId, stats]) => ({
      brokerId: brokerId as IndianBrokerType,
      brokerName: brokerConfigs[brokerId]?.name || brokerId,
      icon: brokerConfigs[brokerId]?.icon || '🏢',
      color: brokerConfigs[brokerId]?.color || 'blue',
      ...stats
    }))
  }, [brokerMetrics, orderRoutings, brokerConfigs])

  // Performance heatmap data
  const performanceHeatmap = useMemo(() => {
    const metrics = ['latency', 'fillRate', 'successRate', 'avgCost']
    return brokerMetrics.map(broker => ({
      broker: broker.brokerId,
      metrics: {
        latency: Math.max(0, 100 - (broker.latency / 100) * 100), // Lower is better
        fillRate: broker.fillRate || 95,
        successRate: broker.successRate,
        avgCost: Math.max(0, 100 - (broker.avgCost / 20) * 100) // Lower is better
      }
    }))
  }, [brokerMetrics])

  // Cost analysis data
  const costAnalysisData = useMemo(() => {
    return brokerMetrics.map(broker => ({
      broker: broker.brokerId,
      brokerName: brokerConfigs[broker.brokerId]?.name || broker.brokerId,
      avgCost: broker.avgCost,
      avgOrderValue: broker.avgOrderValue || 100000,
      totalCostSavings: (broker.avgOrderValue || 100000) * (15 - broker.avgCost) / 10000,
      volume: Math.floor(Math.random() * 1000000)
    }))
  }, [brokerMetrics, brokerConfigs])

  const renderFlowVisualization = () => (
    <div className="space-y-6">
      {/* Broker Flow Network */}
      <div className="glass-card p-6 rounded-2xl">
        <div className="flex items-center justify-between mb-6">
          <h3 className="text-xl font-bold text-white flex items-center gap-2">
            <Network className="w-5 h-5 text-purple-400" />
            Order Routing Flow
          </h3>
          <div className="flex items-center gap-2 text-sm text-slate-400">
            <Activity className="w-4 h-4" />
            Real-time network view
          </div>
        </div>
        
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {/* TradeMaster Center Node */}
          <div className="lg:col-span-1 flex items-center justify-center">
            <div className="relative">
              <motion.div
                animate={{ scale: [1, 1.05, 1] }}
                transition={{ duration: 2, repeat: Infinity }}
                className="w-32 h-32 rounded-full bg-gradient-to-br from-purple-500/30 to-cyan-500/30 border-2 border-purple-400/50 flex items-center justify-center backdrop-blur-sm"
              >
                <div className="text-center">
                  <div className="text-2xl mb-1">🎯</div>
                  <div className="text-white font-bold text-sm">TradeMaster</div>
                  <div className="text-purple-400 text-xs">Router</div>
                </div>
              </motion.div>
              
              {/* Animated connection lines */}
              <div className="absolute inset-0 pointer-events-none">
                {routingFlowData.slice(0, 3).map((broker, i) => (
                  <motion.div
                    key={broker.brokerId}
                    className="absolute w-px bg-gradient-to-r from-purple-400/50 to-transparent"
                    style={{
                      height: '60px',
                      top: '50%',
                      right: i % 2 === 0 ? '-60px' : '-80px',
                      transform: `rotate(${(i - 1) * 30}deg)`,
                      transformOrigin: '0 50%'
                    }}
                    animate={{ opacity: [0.3, 0.8, 0.3] }}
                    transition={{ duration: 2, repeat: Infinity, delay: i * 0.5 }}
                  />
                ))}
              </div>
            </div>
          </div>
          
          {/* Broker Nodes */}
          <div className="lg:col-span-2">
            <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
              {routingFlowData.map(broker => (
                <motion.div
                  key={broker.brokerId}
                  whileHover={{ scale: 1.05 }}
                  className={`p-4 rounded-xl border bg-gradient-to-br ${
                    broker.load > 80 ? 'from-red-500/10 to-red-600/10 border-red-500/30' :
                    broker.load > 60 ? 'from-yellow-500/10 to-yellow-600/10 border-yellow-500/30' :
                    'from-green-500/10 to-green-600/10 border-green-500/30'
                  } backdrop-blur-sm relative overflow-hidden`}
                >
                  <div className="relative z-10">
                    <div className="flex items-center justify-between mb-2">
                      <span className="text-lg">{broker.icon}</span>
                      <div className={`w-2 h-2 rounded-full ${
                        broker.success > 95 ? 'bg-green-400' :
                        broker.success > 90 ? 'bg-yellow-400' : 'bg-red-400'
                      }`} />
                    </div>
                    <div className="text-white font-semibold text-sm mb-1">{broker.brokerName}</div>
                    <div className="grid grid-cols-2 gap-1 text-xs">
                      <div className="text-slate-400">Orders</div>
                      <div className="text-white font-mono">{broker.orders}</div>
                      <div className="text-slate-400">Load</div>
                      <div className={`font-mono ${
                        broker.load > 80 ? 'text-red-400' :
                        broker.load > 60 ? 'text-yellow-400' : 'text-green-400'
                      }`}>{broker.load}%</div>
                      <div className="text-slate-400">Latency</div>
                      <div className="text-white font-mono">{Math.round(broker.latency)}ms</div>
                    </div>
                  </div>
                  
                  {/* Load indicator */}
                  <div className="absolute bottom-0 left-0 right-0 h-1 bg-slate-700/50">
                    <motion.div
                      className={`h-full ${
                        broker.load > 80 ? 'bg-red-400' :
                        broker.load > 60 ? 'bg-yellow-400' : 'bg-green-400'
                      }`}
                      style={{ width: `${broker.load}%` }}
                      initial={{ width: 0 }}
                      animate={{ width: `${broker.load}%` }}
                      transition={{ duration: 1, delay: 0.5 }}
                    />
                  </div>
                </motion.div>
              ))}
            </div>
          </div>
        </div>
      </div>
      
      {/* Real-time Order Flow */}
      <div className="glass-card p-6 rounded-2xl">
        <h3 className="text-xl font-bold text-white mb-4 flex items-center gap-2">
          <ArrowRight className="w-5 h-5 text-cyan-400" />
          Live Order Stream
        </h3>
        
        <div className="space-y-2 max-h-80 overflow-y-auto custom-scrollbar">
          {orderRoutings.slice(-10).reverse().map((routing, i) => (
            <motion.div
              key={routing.id}
              initial={{ opacity: 0, x: -20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: i * 0.1 }}
              className="flex items-center justify-between p-3 rounded-lg bg-slate-800/30 border border-slate-700/30"
            >
              <div className="flex items-center gap-3">
                <div className={`w-2 h-2 rounded-full ${
                  routing.status === 'completed' ? 'bg-green-400' :
                  routing.status === 'executing' ? 'bg-yellow-400 animate-pulse' :
                  routing.status === 'failed' ? 'bg-red-400' : 'bg-blue-400'
                }`} />
                <div>
                  <div className="text-white font-semibold text-sm">{routing.symbol}</div>
                  <div className="text-slate-400 text-xs">{routing.orderId}</div>
                </div>
              </div>
              
              <div className="flex items-center gap-4 text-xs">
                <div className="text-center">
                  <div className="text-slate-400">Route</div>
                  <div className="text-white">{brokerConfigs[routing.targetBroker]?.name || routing.targetBroker}</div>
                </div>
                <div className="text-center">
                  <div className="text-slate-400">Score</div>
                  <div className="text-green-400 font-mono">{Math.round(routing.routingScore)}</div>
                </div>
                <div className="text-center">
                  <div className="text-slate-400">ETA</div>
                  <div className="text-white font-mono">{Math.round(routing.estimatedExecutionTime)}ms</div>
                </div>
              </div>
            </motion.div>
          ))}
        </div>
      </div>
    </div>
  )

  const renderPerformanceVisualization = () => (
    <div className="space-y-6">
      {/* Performance Heatmap */}
      <div className="glass-card p-6 rounded-2xl">
        <div className="flex items-center justify-between mb-6">
          <h3 className="text-xl font-bold text-white flex items-center gap-2">
            <Gauge className="w-5 h-5 text-green-400" />
            Broker Performance Heatmap
          </h3>
          <div className="flex items-center gap-2">
            <span className="text-xs text-slate-400">Better</span>
            <div className="flex gap-1">
              <div className="w-3 h-3 rounded bg-red-500/30" />
              <div className="w-3 h-3 rounded bg-yellow-500/30" />
              <div className="w-3 h-3 rounded bg-green-500/30" />
            </div>
          </div>
        </div>
        
        <div className="grid gap-4">
          <div className="grid grid-cols-5 gap-2 text-xs text-slate-400">
            <div></div>
            <div className="text-center">Latency</div>
            <div className="text-center">Fill Rate</div>
            <div className="text-center">Success</div>
            <div className="text-center">Cost</div>
          </div>
          
          {performanceHeatmap.map(broker => (
            <div key={broker.broker} className="grid grid-cols-5 gap-2 items-center">
              <div className="text-white font-semibold text-sm">
                {brokerConfigs[broker.broker]?.name || broker.broker}
              </div>
              {Object.entries(broker.metrics).map(([metric, value]) => (
                <div key={metric} className="text-center">
                  <div
                    className={`h-8 rounded flex items-center justify-center text-xs font-semibold ${
                      value >= 80 ? 'bg-green-500/20 text-green-400' :
                      value >= 60 ? 'bg-yellow-500/20 text-yellow-400' :
                      'bg-red-500/20 text-red-400'
                    }`}
                  >
                    {Math.round(value)}
                  </div>
                </div>
              ))}
            </div>
          ))}
        </div>
      </div>
      
      {/* Execution Time Breakdown */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="glass-card p-6 rounded-2xl">
          <h3 className="text-lg font-bold text-white mb-4 flex items-center gap-2">
            <Clock className="w-5 h-5 text-blue-400" />
            Execution Phase Analysis
          </h3>
          
          <div className="space-y-4">
            {executionMetrics.slice(0, 5).map(metric => (
              <div key={metric.orderId} className="border-l-2 border-purple-500/30 pl-4">
                <div className="flex justify-between items-center mb-2">
                  <span className="text-white font-semibold text-sm">{metric.symbol}</span>
                  <span className="text-slate-400 text-xs">{Math.round(metric.totalExecutionTime)}ms</span>
                </div>
                
                <div className="space-y-1">
                  {Object.entries(metric.executionPhases || {}).map(([phase, time]) => (
                    <div key={phase} className="flex justify-between text-xs">
                      <span className="text-slate-400 capitalize">{phase.replace(/([A-Z])/g, ' $1')}</span>
                      <span className="text-white font-mono">{Math.round(time as number)}ms</span>
                    </div>
                  ))}
                </div>
              </div>
            ))}
          </div>
        </div>
        
        <div className="glass-card p-6 rounded-2xl">
          <h3 className="text-lg font-bold text-white mb-4 flex items-center gap-2">
            <Target className="w-5 h-5 text-orange-400" />
            Quality Metrics
          </h3>
          
          <div className="space-y-4">
            {executionMetrics.slice(0, 5).map(metric => (
              <div key={metric.orderId} className="border-l-2 border-orange-500/30 pl-4">
                <div className="flex justify-between items-center mb-2">
                  <span className="text-white font-semibold text-sm">{metric.symbol}</span>
                  <span className={`text-xs px-2 py-1 rounded ${
                    metric.fillQuality === 'excellent' ? 'bg-green-500/20 text-green-400' :
                    metric.fillQuality === 'good' ? 'bg-blue-500/20 text-blue-400' :
                    metric.fillQuality === 'fair' ? 'bg-yellow-500/20 text-yellow-400' :
                    'bg-red-500/20 text-red-400'
                  }`}>{metric.fillQuality}</span>
                </div>
                
                <div className="grid grid-cols-2 gap-2 text-xs">
                  <div className="flex justify-between">
                    <span className="text-slate-400">Slippage</span>
                    <span className="text-white font-mono">{metric.slippage.toFixed(1)}bp</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-400">Impact</span>
                    <span className="text-white font-mono">{metric.marketImpact.toFixed(1)}bp</span>
                  </div>
                  {metric.qualityMetrics && (
                    <>
                      <div className="flex justify-between">
                        <span className="text-slate-400">Shortfall</span>
                        <span className="text-white font-mono">{metric.qualityMetrics.executionShortfall.toFixed(1)}bp</span>
                      </div>
                      <div className="flex justify-between">
                        <span className="text-slate-400">Improvement</span>
                        <span className="text-green-400 font-mono">+{metric.qualityMetrics.priceImprovement.toFixed(1)}bp</span>
                      </div>
                    </>
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  )

  const renderCostVisualization = () => (
    <div className="space-y-6">
      {/* Cost Comparison */}
      <div className="glass-card p-6 rounded-2xl">
        <div className="flex items-center justify-between mb-6">
          <h3 className="text-xl font-bold text-white flex items-center gap-2">
            <DollarSign className="w-5 h-5 text-green-400" />
            Cost Analysis & Savings
          </h3>
          <div className="text-sm text-slate-400">
            Basis points (bps)
          </div>
        </div>
        
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {costAnalysisData.map(broker => (
            <div key={broker.broker} className="p-4 rounded-xl bg-slate-800/30 border border-slate-700/30">
              <div className="flex items-center justify-between mb-3">
                <div className="flex items-center gap-2">
                  <span>{brokerConfigs[broker.broker]?.icon || '🏢'}</span>
                  <span className="text-white font-semibold text-sm">{broker.brokerName}</span>
                </div>
                <div className={`px-2 py-1 rounded text-xs ${
                  broker.avgCost < 5 ? 'bg-green-500/20 text-green-400' :
                  broker.avgCost < 10 ? 'bg-yellow-500/20 text-yellow-400' :
                  'bg-red-500/20 text-red-400'
                }`}>
                  {broker.avgCost.toFixed(1)} bps
                </div>
              </div>
              
              <div className="space-y-2 text-xs">
                <div className="flex justify-between">
                  <span className="text-slate-400">Avg Order Value</span>
                  <span className="text-white font-mono">₹{(broker.avgOrderValue / 1000).toFixed(0)}K</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-400">Daily Volume</span>
                  <span className="text-white font-mono">₹{(broker.volume / 100000).toFixed(1)}L</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-400">Cost Savings</span>
                  <span className="text-green-400 font-mono">₹{(broker.totalCostSavings / 1000).toFixed(1)}K</span>
                </div>
              </div>
              
              {/* Cost trend */}
              <div className="mt-3 h-2 bg-slate-700/50 rounded-full overflow-hidden">
                <motion.div
                  className="h-full bg-gradient-to-r from-green-400 to-blue-400"
                  style={{ width: `${100 - (broker.avgCost / 15) * 100}%` }}
                  initial={{ width: 0 }}
                  animate={{ width: `${100 - (broker.avgCost / 15) * 100}%` }}
                  transition={{ duration: 1, delay: 0.5 }}
                />
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  )

  return (
    <div className="space-y-6">
      {/* Visualization Controls */}
      <div className="flex flex-col sm:flex-row gap-4 items-start sm:items-center justify-between">
        <div className="flex gap-2">
          {[
            { id: 'flow', label: 'Flow Network', icon: Network },
            { id: 'performance', label: 'Performance', icon: Gauge },
            { id: 'cost', label: 'Cost Analysis', icon: DollarSign }
          ].map(mode => {
            const Icon = mode.icon
            return (
              <motion.button
                key={mode.id}
                whileHover={{ scale: 1.02 }}
                whileTap={{ scale: 0.98 }}
                onClick={() => onVisualizationChange(mode.id as any)}
                className={`flex items-center gap-2 px-4 py-2 rounded-lg font-medium text-sm transition-all ${
                  visualization === mode.id
                    ? 'bg-gradient-to-r from-purple-500 to-cyan-500 text-white shadow-lg'
                    : 'bg-slate-800/50 text-slate-400 hover:text-white hover:bg-slate-700/50'
                }`}
              >
                <Icon className="w-4 h-4" />
                {mode.label}
              </motion.button>
            )
          })}
        </div>
        
        <div className="flex items-center gap-2">
          <div className={`px-2 py-1 rounded text-xs ${
            loading ? 'bg-yellow-500/20 text-yellow-400' : 'bg-green-500/20 text-green-400'
          }`}>
            {loading ? '🔄 Updating' : '✓ Live'}
          </div>
        </div>
      </div>
      
      {/* Visualization Content */}
      <AnimatePresence mode="wait">
        <motion.div
          key={visualization}
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          exit={{ opacity: 0, y: -20 }}
          transition={{ duration: 0.3 }}
        >
          {visualization === 'flow' && renderFlowVisualization()}
          {visualization === 'performance' && renderPerformanceVisualization()}
          {visualization === 'cost' && renderCostVisualization()}
        </motion.div>
      </AnimatePresence>
    </div>
  )
}

export default AdvancedOrderManagement