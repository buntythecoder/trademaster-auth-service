// Advanced Order Management System
// FRONT-004: Advanced Trading Interface Enhancement

import React, { useState, useEffect, useCallback, useMemo, useRef } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import {
  Plus,
  Minus,
  Settings,
  Target,
  Shield,
  Clock,
  AlertTriangle,
  CheckCircle,
  X,
  Play,
  Pause,
  StopCircle,
  TrendingUp,
  TrendingDown,
  BarChart3,
  Layers,
  Zap,
  ArrowUpDown,
  DollarSign,
  Percent,
  Calculator,
  Link2,
  Unlink2,
  Copy,
  Edit3,
  Trash2,
  Eye,
  EyeOff,
  RefreshCw,
  Filter,
  Search
} from 'lucide-react'

export interface AdvancedOrderConfig {
  id: string
  type: 'market' | 'limit' | 'stop' | 'stop-limit' | 'oco' | 'bracket' | 'iceberg' | 'trailing-stop'
  symbol: string
  side: 'buy' | 'sell'
  quantity: number
  price?: number
  stopPrice?: number
  trailingAmount?: number
  trailingPercent?: number
  timeInForce: 'day' | 'gtc' | 'ioc' | 'fok'
  conditions?: OrderCondition[]
  legs?: OrderLeg[]
  parentOrderId?: string
  childOrders?: string[]
  status: 'draft' | 'pending' | 'active' | 'filled' | 'cancelled' | 'rejected' | 'expired'
  createdAt: Date
  updatedAt: Date
  executionPriority: 'normal' | 'high' | 'urgent'
  riskControls: RiskControl[]
}

export interface OrderCondition {
  id: string
  type: 'price' | 'time' | 'volume' | 'indicator' | 'news'
  operator: 'greater' | 'less' | 'equals' | 'between'
  value: number | string
  reference: string
  enabled: boolean
}

export interface OrderLeg {
  id: string
  symbol: string
  side: 'buy' | 'sell'
  quantity: number
  price?: number
  ratio: number
  orderType: 'market' | 'limit'
}

export interface RiskControl {
  id: string
  type: 'max-position' | 'max-loss' | 'max-exposure' | 'correlation-check'
  threshold: number
  action: 'warn' | 'block' | 'reduce'
  enabled: boolean
}

export interface OrderTemplate {
  id: string
  name: string
  description: string
  orderConfig: Partial<AdvancedOrderConfig>
  category: 'scalping' | 'swing' | 'momentum' | 'arbitrage' | 'custom'
  tags: string[]
}

interface AdvancedOrderManagementProps {
  symbol?: string
  currentPrice?: number
  availableCash?: number
  currentPositions?: Array<{
    symbol: string
    quantity: number
    avgCost: number
    unrealizedPnL: number
  }>
  onOrderSubmit: (order: AdvancedOrderConfig) => Promise<void>
  onOrderModify: (orderId: string, changes: Partial<AdvancedOrderConfig>) => Promise<void>
  onOrderCancel: (orderId: string) => Promise<void>
  className?: string
}

const defaultOrderTemplates: OrderTemplate[] = [
  {
    id: 'scalp-quick',
    name: 'Quick Scalp',
    description: 'Fast entry/exit with tight stops',
    category: 'scalping',
    tags: ['quick', 'tight-stops'],
    orderConfig: {
      type: 'bracket',
      timeInForce: 'day',
      riskControls: [
        {
          id: '1',
          type: 'max-loss',
          threshold: 100,
          action: 'block',
          enabled: true
        }
      ]
    }
  },
  {
    id: 'swing-setup',
    name: 'Swing Trade Setup',
    description: 'Medium-term position with wide stops',
    category: 'swing',
    tags: ['medium-term', 'wide-stops'],
    orderConfig: {
      type: 'oco',
      timeInForce: 'gtc',
      riskControls: [
        {
          id: '1',
          type: 'max-position',
          threshold: 10000,
          action: 'warn',
          enabled: true
        }
      ]
    }
  },
  {
    id: 'momentum-breakout',
    name: 'Momentum Breakout',
    description: 'Breakout entry with trailing stop',
    category: 'momentum',
    tags: ['breakout', 'trailing'],
    orderConfig: {
      type: 'trailing-stop',
      timeInForce: 'day',
      trailingPercent: 2.0,
      riskControls: [
        {
          id: '1',
          type: 'max-exposure',
          threshold: 50000,
          action: 'warn',
          enabled: true
        }
      ]
    }
  }
]

const OrderTypeSelector: React.FC<{
  value: AdvancedOrderConfig['type']
  onChange: (type: AdvancedOrderConfig['type']) => void
  disabled?: boolean
}> = ({ value, onChange, disabled }) => {
  const orderTypes = [
    { value: 'market', label: 'Market', icon: Zap, description: 'Execute immediately at current price' },
    { value: 'limit', label: 'Limit', icon: Target, description: 'Execute at specific price or better' },
    { value: 'stop', label: 'Stop', icon: Shield, description: 'Market order triggered at stop price' },
    { value: 'stop-limit', label: 'Stop Limit', icon: Layers, description: 'Limit order triggered at stop price' },
    { value: 'oco', label: 'OCO', icon: Link2, description: 'One-Cancels-Other order pair' },
    { value: 'bracket', label: 'Bracket', icon: Target, description: 'Entry with profit target and stop loss' },
    { value: 'iceberg', label: 'Iceberg', icon: EyeOff, description: 'Large order split into smaller chunks' },
    { value: 'trailing-stop', label: 'Trailing Stop', icon: TrendingUp, description: 'Dynamic stop that follows price' }
  ] as const

  return (
    <div className="grid grid-cols-2 gap-2">
      {orderTypes.map(({ value: typeValue, label, icon: Icon, description }) => (
        <button
          key={typeValue}
          onClick={() => !disabled && onChange(typeValue)}
          disabled={disabled}
          className={`p-3 rounded-lg text-left transition-all duration-200 ${
            value === typeValue
              ? 'bg-purple-500/20 border-purple-500/50 ring-1 ring-purple-500/30'
              : 'bg-slate-800/30 border-slate-700/50 hover:bg-slate-700/40'
          } border ${disabled ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer'}`}
          title={description}
        >
          <div className="flex items-center space-x-2 mb-1">
            <Icon className="w-4 h-4 text-purple-400" />
            <span className="font-medium text-white text-sm">{label}</span>
          </div>
          <p className="text-xs text-slate-400 line-clamp-2">{description}</p>
        </button>
      ))}
    </div>
  )
}

const RiskControlPanel: React.FC<{
  controls: RiskControl[]
  onChange: (controls: RiskControl[]) => void
  availableCash: number
  currentPositions: Array<{ symbol: string; quantity: number; avgCost: number }>
}> = ({ controls, onChange, availableCash, currentPositions }) => {
  const addControl = () => {
    const newControl: RiskControl = {
      id: Date.now().toString(),
      type: 'max-loss',
      threshold: 500,
      action: 'warn',
      enabled: true
    }
    onChange([...controls, newControl])
  }

  const updateControl = (id: string, updates: Partial<RiskControl>) => {
    onChange(controls.map(control => 
      control.id === id ? { ...control, ...updates } : control
    ))
  }

  const removeControl = (id: string) => {
    onChange(controls.filter(control => control.id !== id))
  }

  const totalPositionValue = currentPositions.reduce((sum, pos) => sum + (pos.quantity * pos.avgCost), 0)

  return (
    <div className="bg-slate-800/30 rounded-lg p-4 border border-slate-700/50">
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center space-x-2">
          <Shield className="w-5 h-5 text-orange-400" />
          <h4 className="font-semibold text-white">Risk Controls</h4>
        </div>
        <button
          onClick={addControl}
          className="cyber-button-sm p-2"
          title="Add Risk Control"
        >
          <Plus className="w-4 h-4" />
        </button>
      </div>

      {/* Risk Summary */}
      <div className="grid grid-cols-3 gap-3 mb-4 p-3 bg-slate-900/30 rounded-lg">
        <div className="text-center">
          <div className="text-xs text-slate-400 mb-1">Available Cash</div>
          <div className="font-mono text-green-400">₹{availableCash.toLocaleString()}</div>
        </div>
        <div className="text-center">
          <div className="text-xs text-slate-400 mb-1">Position Value</div>
          <div className="font-mono text-blue-400">₹{totalPositionValue.toLocaleString()}</div>
        </div>
        <div className="text-center">
          <div className="text-xs text-slate-400 mb-1">Total Exposure</div>
          <div className="font-mono text-purple-400">₹{(availableCash + totalPositionValue).toLocaleString()}</div>
        </div>
      </div>

      <div className="space-y-3">
        <AnimatePresence>
          {controls.map((control) => (
            <motion.div
              key={control.id}
              initial={{ opacity: 0, height: 0 }}
              animate={{ opacity: 1, height: 'auto' }}
              exit={{ opacity: 0, height: 0 }}
              className="p-3 bg-slate-700/20 rounded-lg border border-slate-600/30"
            >
              <div className="flex items-center justify-between mb-3">
                <div className="flex items-center space-x-2">
                  <button
                    onClick={() => updateControl(control.id, { enabled: !control.enabled })}
                    className={`w-4 h-4 rounded border-2 transition-colors ${
                      control.enabled 
                        ? 'bg-orange-500 border-orange-500' 
                        : 'border-slate-500 hover:border-slate-400'
                    }`}
                  />
                  <select
                    value={control.type}
                    onChange={(e) => updateControl(control.id, { type: e.target.value as RiskControl['type'] })}
                    className="bg-slate-600 border border-slate-500 rounded px-2 py-1 text-sm text-white focus:ring-2 focus:ring-orange-500"
                  >
                    <option value="max-loss">Max Loss</option>
                    <option value="max-position">Max Position</option>
                    <option value="max-exposure">Max Exposure</option>
                    <option value="correlation-check">Correlation Check</option>
                  </select>
                </div>
                
                <button
                  onClick={() => removeControl(control.id)}
                  className="p-1 rounded text-slate-400 hover:text-red-400 transition-colors"
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-xs text-slate-400 mb-1 block">Threshold</label>
                  <div className="relative">
                    <input
                      type="number"
                      value={control.threshold}
                      onChange={(e) => updateControl(control.id, { threshold: parseFloat(e.target.value) })}
                      className="w-full bg-slate-600 border border-slate-500 rounded px-3 py-2 text-white text-sm focus:ring-2 focus:ring-orange-500"
                      placeholder="0"
                    />
                    <span className="absolute right-3 top-2 text-xs text-slate-400">₹</span>
                  </div>
                </div>

                <div>
                  <label className="text-xs text-slate-400 mb-1 block">Action</label>
                  <select
                    value={control.action}
                    onChange={(e) => updateControl(control.id, { action: e.target.value as RiskControl['action'] })}
                    className="w-full bg-slate-600 border border-slate-500 rounded px-3 py-2 text-white text-sm focus:ring-2 focus:ring-orange-500"
                  >
                    <option value="warn">Warn</option>
                    <option value="block">Block</option>
                    <option value="reduce">Reduce</option>
                  </select>
                </div>
              </div>
            </motion.div>
          ))}
        </AnimatePresence>

        {controls.length === 0 && (
          <div className="text-center py-8 text-slate-400">
            <Shield className="w-8 h-8 mx-auto mb-2 opacity-50" />
            <p className="text-sm">No risk controls configured</p>
            <p className="text-xs">Add controls to manage trading risk</p>
          </div>
        )}
      </div>
    </div>
  )
}

const OrderConditionsPanel: React.FC<{
  conditions: OrderCondition[]
  onChange: (conditions: OrderCondition[]) => void
}> = ({ conditions, onChange }) => {
  const addCondition = () => {
    const newCondition: OrderCondition = {
      id: Date.now().toString(),
      type: 'price',
      operator: 'greater',
      value: 0,
      reference: 'current_price',
      enabled: true
    }
    onChange([...conditions, newCondition])
  }

  const updateCondition = (id: string, updates: Partial<OrderCondition>) => {
    onChange(conditions.map(condition => 
      condition.id === id ? { ...condition, ...updates } : condition
    ))
  }

  const removeCondition = (id: string) => {
    onChange(conditions.filter(condition => condition.id !== id))
  }

  return (
    <div className="bg-slate-800/30 rounded-lg p-4 border border-slate-700/50">
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center space-x-2">
          <Settings className="w-5 h-5 text-blue-400" />
          <h4 className="font-semibold text-white">Order Conditions</h4>
        </div>
        <button
          onClick={addCondition}
          className="cyber-button-sm p-2"
          title="Add Condition"
        >
          <Plus className="w-4 h-4" />
        </button>
      </div>

      <div className="space-y-3">
        <AnimatePresence>
          {conditions.map((condition) => (
            <motion.div
              key={condition.id}
              initial={{ opacity: 0, height: 0 }}
              animate={{ opacity: 1, height: 'auto' }}
              exit={{ opacity: 0, height: 0 }}
              className="p-3 bg-slate-700/20 rounded-lg border border-slate-600/30"
            >
              <div className="flex items-center justify-between mb-3">
                <button
                  onClick={() => updateCondition(condition.id, { enabled: !condition.enabled })}
                  className={`w-4 h-4 rounded border-2 transition-colors ${
                    condition.enabled 
                      ? 'bg-blue-500 border-blue-500' 
                      : 'border-slate-500 hover:border-slate-400'
                  }`}
                />
                
                <button
                  onClick={() => removeCondition(condition.id)}
                  className="p-1 rounded text-slate-400 hover:text-red-400 transition-colors"
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-xs text-slate-400 mb-1 block">Type</label>
                  <select
                    value={condition.type}
                    onChange={(e) => updateCondition(condition.id, { type: e.target.value as OrderCondition['type'] })}
                    className="w-full bg-slate-600 border border-slate-500 rounded px-3 py-2 text-white text-sm focus:ring-2 focus:ring-blue-500"
                  >
                    <option value="price">Price</option>
                    <option value="time">Time</option>
                    <option value="volume">Volume</option>
                    <option value="indicator">Technical Indicator</option>
                    <option value="news">News Event</option>
                  </select>
                </div>

                <div>
                  <label className="text-xs text-slate-400 mb-1 block">Operator</label>
                  <select
                    value={condition.operator}
                    onChange={(e) => updateCondition(condition.id, { operator: e.target.value as OrderCondition['operator'] })}
                    className="w-full bg-slate-600 border border-slate-500 rounded px-3 py-2 text-white text-sm focus:ring-2 focus:ring-blue-500"
                  >
                    <option value="greater">Greater than</option>
                    <option value="less">Less than</option>
                    <option value="equals">Equals</option>
                    <option value="between">Between</option>
                  </select>
                </div>

                <div>
                  <label className="text-xs text-slate-400 mb-1 block">Value</label>
                  <input
                    type="number"
                    value={condition.value}
                    onChange={(e) => updateCondition(condition.id, { value: parseFloat(e.target.value) })}
                    className="w-full bg-slate-600 border border-slate-500 rounded px-3 py-2 text-white text-sm focus:ring-2 focus:ring-blue-500"
                    placeholder="0"
                  />
                </div>

                <div>
                  <label className="text-xs text-slate-400 mb-1 block">Reference</label>
                  <select
                    value={condition.reference}
                    onChange={(e) => updateCondition(condition.id, { reference: e.target.value })}
                    className="w-full bg-slate-600 border border-slate-500 rounded px-3 py-2 text-white text-sm focus:ring-2 focus:ring-blue-500"
                  >
                    <option value="current_price">Current Price</option>
                    <option value="day_high">Day High</option>
                    <option value="day_low">Day Low</option>
                    <option value="volume">Volume</option>
                    <option value="rsi">RSI</option>
                    <option value="macd">MACD</option>
                  </select>
                </div>
              </div>
            </motion.div>
          ))}
        </AnimatePresence>

        {conditions.length === 0 && (
          <div className="text-center py-8 text-slate-400">
            <Settings className="w-8 h-8 mx-auto mb-2 opacity-50" />
            <p className="text-sm">No conditions configured</p>
            <p className="text-xs">Add conditions for conditional order execution</p>
          </div>
        )}
      </div>
    </div>
  )
}

const OrderTemplateSelector: React.FC<{
  templates: OrderTemplate[]
  onApplyTemplate: (template: OrderTemplate) => void
  onSaveAsTemplate: (order: AdvancedOrderConfig) => void
  currentOrder: AdvancedOrderConfig
}> = ({ templates, onApplyTemplate, onSaveAsTemplate, currentOrder }) => {
  const [showTemplates, setShowTemplates] = useState(false)
  const [newTemplateName, setNewTemplateName] = useState('')
  const [showSaveDialog, setShowSaveDialog] = useState(false)

  const handleSaveTemplate = () => {
    if (!newTemplateName.trim()) return

    const template: OrderTemplate = {
      id: Date.now().toString(),
      name: newTemplateName,
      description: `Custom template created on ${new Date().toLocaleDateString()}`,
      category: 'custom',
      tags: ['custom'],
      orderConfig: {
        type: currentOrder.type,
        timeInForce: currentOrder.timeInForce,
        conditions: currentOrder.conditions,
        riskControls: currentOrder.riskControls
      }
    }

    onSaveAsTemplate(template)
    setNewTemplateName('')
    setShowSaveDialog(false)
  }

  return (
    <div className="bg-slate-800/30 rounded-lg p-4 border border-slate-700/50">
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center space-x-2">
          <Copy className="w-5 h-5 text-green-400" />
          <h4 className="font-semibold text-white">Order Templates</h4>
        </div>
        <div className="flex items-center space-x-2">
          <button
            onClick={() => setShowSaveDialog(true)}
            className="cyber-button-sm px-3 py-1 text-xs"
            title="Save as Template"
          >
            Save
          </button>
          <button
            onClick={() => setShowTemplates(!showTemplates)}
            className="cyber-button-sm p-2"
            title="Show Templates"
          >
            <Eye className="w-4 h-4" />
          </button>
        </div>
      </div>

      <AnimatePresence>
        {showTemplates && (
          <motion.div
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: 'auto' }}
            exit={{ opacity: 0, height: 0 }}
            className="space-y-2 mb-4"
          >
            {templates.map((template) => (
              <button
                key={template.id}
                onClick={() => onApplyTemplate(template)}
                className="w-full text-left p-3 bg-slate-700/20 rounded-lg border border-slate-600/30 hover:bg-slate-600/30 transition-colors"
              >
                <div className="flex items-center justify-between mb-1">
                  <span className="font-medium text-white">{template.name}</span>
                  <span className={`text-xs px-2 py-1 rounded ${
                    template.category === 'scalping' ? 'bg-red-500/20 text-red-400' :
                    template.category === 'swing' ? 'bg-blue-500/20 text-blue-400' :
                    template.category === 'momentum' ? 'bg-green-500/20 text-green-400' :
                    'bg-purple-500/20 text-purple-400'
                  }`}>
                    {template.category}
                  </span>
                </div>
                <p className="text-sm text-slate-400 mb-2">{template.description}</p>
                <div className="flex flex-wrap gap-1">
                  {template.tags.map(tag => (
                    <span key={tag} className="text-xs px-2 py-1 bg-slate-600/30 text-slate-300 rounded">
                      {tag}
                    </span>
                  ))}
                </div>
              </button>
            ))}
          </motion.div>
        )}
      </AnimatePresence>

      {/* Save Template Dialog */}
      <AnimatePresence>
        {showSaveDialog && (
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            exit={{ opacity: 0, scale: 0.95 }}
            className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4"
            onClick={() => setShowSaveDialog(false)}
          >
            <div
              className="glass-card rounded-xl p-6 max-w-md w-full"
              onClick={(e) => e.stopPropagation()}
            >
              <h3 className="text-xl font-bold text-white mb-4">Save Order Template</h3>
              
              <div className="space-y-4">
                <div>
                  <label className="text-sm text-slate-400 mb-2 block">Template Name</label>
                  <input
                    type="text"
                    value={newTemplateName}
                    onChange={(e) => setNewTemplateName(e.target.value)}
                    className="w-full bg-slate-700 border border-slate-600 rounded-lg px-3 py-2 text-white focus:ring-2 focus:ring-green-500"
                    placeholder="Enter template name..."
                    autoFocus
                  />
                </div>

                <div className="flex items-center justify-end space-x-3">
                  <button
                    onClick={() => setShowSaveDialog(false)}
                    className="px-4 py-2 text-slate-400 hover:text-white transition-colors"
                  >
                    Cancel
                  </button>
                  <button
                    onClick={handleSaveTemplate}
                    disabled={!newTemplateName.trim()}
                    className="cyber-button px-4 py-2 disabled:opacity-50"
                  >
                    Save Template
                  </button>
                </div>
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  )
}

export const AdvancedOrderManagement: React.FC<AdvancedOrderManagementProps> = ({
  symbol = 'RELIANCE',
  currentPrice = 2456.75,
  availableCash = 50000,
  currentPositions = [],
  onOrderSubmit,
  onOrderModify,
  onOrderCancel,
  className = ''
}) => {
  const [orderConfig, setOrderConfig] = useState<AdvancedOrderConfig>({
    id: '',
    type: 'limit',
    symbol,
    side: 'buy',
    quantity: 10,
    price: currentPrice,
    timeInForce: 'day',
    conditions: [],
    status: 'draft',
    createdAt: new Date(),
    updatedAt: new Date(),
    executionPriority: 'normal',
    riskControls: []
  })

  const [templates, setTemplates] = useState<OrderTemplate[]>(defaultOrderTemplates)
  const [showAdvancedOptions, setShowAdvancedOptions] = useState(false)
  const [isCalculating, setIsCalculating] = useState(false)
  const [orderPreview, setOrderPreview] = useState<any>(null)

  // Calculate order preview
  const calculateOrderPreview = useCallback(() => {
    setIsCalculating(true)
    
    // Simulate calculation delay
    setTimeout(() => {
      const totalValue = orderConfig.quantity * (orderConfig.price || currentPrice)
      const estimatedFees = totalValue * 0.0025 // 0.25% fees
      const maxRisk = orderConfig.riskControls
        .filter(control => control.type === 'max-loss' && control.enabled)
        .reduce((max, control) => Math.max(max, control.threshold), 0)

      setOrderPreview({
        totalValue,
        estimatedFees,
        netAmount: totalValue + estimatedFees,
        maxRisk,
        riskReward: maxRisk > 0 ? (totalValue * 0.02) / maxRisk : 0, // Assuming 2% target
        marginRequired: orderConfig.side === 'buy' ? totalValue * 0.2 : 0
      })
      setIsCalculating(false)
    }, 500)
  }, [orderConfig, currentPrice])

  useEffect(() => {
    calculateOrderPreview()
  }, [calculateOrderPreview])

  const handleOrderSubmit = async () => {
    try {
      const finalOrder: AdvancedOrderConfig = {
        ...orderConfig,
        id: Date.now().toString(),
        status: 'pending',
        updatedAt: new Date()
      }
      await onOrderSubmit(finalOrder)
    } catch (error) {
      console.error('Failed to submit order:', error)
    }
  }

  const applyTemplate = (template: OrderTemplate) => {
    setOrderConfig(prev => ({
      ...prev,
      ...template.orderConfig,
      symbol: prev.symbol,
      quantity: prev.quantity,
      price: prev.price,
      updatedAt: new Date()
    }))
  }

  const saveAsTemplate = (order: any) => {
    setTemplates(prev => [...prev, order])
  }

  const isValidOrder = useMemo(() => {
    return orderConfig.quantity > 0 &&
           (orderConfig.type === 'market' || (orderConfig.price && orderConfig.price > 0)) &&
           orderConfig.symbol.trim().length > 0
  }, [orderConfig])

  return (
    <motion.div
      className={`glass-card rounded-2xl p-6 space-y-6 ${className}`}
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.6 }}
    >
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center space-x-3">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-r from-purple-500 to-blue-500 flex items-center justify-center">
            <BarChart3 className="w-5 h-5 text-white" />
          </div>
          <div>
            <h2 className="text-xl font-bold text-white">Advanced Order Management</h2>
            <p className="text-sm text-slate-400">Professional-grade order execution system</p>
          </div>
        </div>

        <div className="flex items-center space-x-2">
          <button
            onClick={() => setShowAdvancedOptions(!showAdvancedOptions)}
            className={`cyber-button-sm px-3 py-2 ${showAdvancedOptions ? 'bg-purple-500/20' : ''}`}
          >
            <Settings className="w-4 h-4 mr-2" />
            Advanced
          </button>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Left Column: Basic Order Configuration */}
        <div className="space-y-6">
          {/* Order Type Selection */}
          <div>
            <h3 className="text-lg font-semibold text-white mb-3">Order Type</h3>
            <OrderTypeSelector
              value={orderConfig.type}
              onChange={(type) => setOrderConfig(prev => ({ ...prev, type, updatedAt: new Date() }))}
            />
          </div>

          {/* Basic Parameters */}
          <div className="bg-slate-800/30 rounded-lg p-4 border border-slate-700/50">
            <h4 className="font-semibold text-white mb-4">Order Parameters</h4>
            
            <div className="grid grid-cols-2 gap-4">
              {/* Symbol */}
              <div>
                <label className="text-sm text-slate-400 mb-2 block">Symbol</label>
                <input
                  type="text"
                  value={orderConfig.symbol}
                  onChange={(e) => setOrderConfig(prev => ({ ...prev, symbol: e.target.value.toUpperCase() }))}
                  className="w-full bg-slate-700 border border-slate-600 rounded-lg px-3 py-2 text-white focus:ring-2 focus:ring-purple-500"
                  placeholder="SYMBOL"
                />
              </div>

              {/* Side */}
              <div>
                <label className="text-sm text-slate-400 mb-2 block">Side</label>
                <div className="flex rounded-lg overflow-hidden">
                  <button
                    onClick={() => setOrderConfig(prev => ({ ...prev, side: 'buy' }))}
                    className={`flex-1 py-2 px-3 text-sm font-medium transition-colors ${
                      orderConfig.side === 'buy'
                        ? 'bg-green-500 text-white'
                        : 'bg-slate-700 text-slate-300 hover:bg-slate-600'
                    }`}
                  >
                    Buy
                  </button>
                  <button
                    onClick={() => setOrderConfig(prev => ({ ...prev, side: 'sell' }))}
                    className={`flex-1 py-2 px-3 text-sm font-medium transition-colors ${
                      orderConfig.side === 'sell'
                        ? 'bg-red-500 text-white'
                        : 'bg-slate-700 text-slate-300 hover:bg-slate-600'
                    }`}
                  >
                    Sell
                  </button>
                </div>
              </div>

              {/* Quantity */}
              <div>
                <label className="text-sm text-slate-400 mb-2 block">Quantity</label>
                <input
                  type="number"
                  value={orderConfig.quantity}
                  onChange={(e) => setOrderConfig(prev => ({ ...prev, quantity: parseInt(e.target.value) || 0 }))}
                  className="w-full bg-slate-700 border border-slate-600 rounded-lg px-3 py-2 text-white focus:ring-2 focus:ring-purple-500"
                  placeholder="0"
                  min="1"
                />
              </div>

              {/* Price */}
              {orderConfig.type !== 'market' && (
                <div>
                  <label className="text-sm text-slate-400 mb-2 block">
                    {orderConfig.type === 'limit' ? 'Limit Price' : 'Price'}
                  </label>
                  <div className="relative">
                    <input
                      type="number"
                      value={orderConfig.price || ''}
                      onChange={(e) => setOrderConfig(prev => ({ ...prev, price: parseFloat(e.target.value) || undefined }))}
                      className="w-full bg-slate-700 border border-slate-600 rounded-lg px-3 py-2 text-white focus:ring-2 focus:ring-purple-500"
                      placeholder="0.00"
                      step="0.01"
                    />
                    <span className="absolute right-3 top-2 text-xs text-slate-400">₹</span>
                  </div>
                </div>
              )}

              {/* Time in Force */}
              <div className="col-span-2">
                <label className="text-sm text-slate-400 mb-2 block">Time in Force</label>
                <select
                  value={orderConfig.timeInForce}
                  onChange={(e) => setOrderConfig(prev => ({ ...prev, timeInForce: e.target.value as any }))}
                  className="w-full bg-slate-700 border border-slate-600 rounded-lg px-3 py-2 text-white focus:ring-2 focus:ring-purple-500"
                >
                  <option value="day">Day</option>
                  <option value="gtc">Good Till Cancelled</option>
                  <option value="ioc">Immediate or Cancel</option>
                  <option value="fok">Fill or Kill</option>
                </select>
              </div>
            </div>
          </div>

          {/* Order Preview */}
          <div className="bg-slate-800/30 rounded-lg p-4 border border-slate-700/50">
            <h4 className="font-semibold text-white mb-4 flex items-center space-x-2">
              <Calculator className="w-4 h-4" />
              <span>Order Preview</span>
              {isCalculating && <RefreshCw className="w-4 h-4 animate-spin" />}
            </h4>
            
            {orderPreview && (
              <div className="grid grid-cols-2 gap-3 text-sm">
                <div className="bg-slate-700/20 rounded p-2">
                  <div className="text-slate-400 mb-1">Total Value</div>
                  <div className="font-mono text-white">₹{orderPreview.totalValue.toLocaleString()}</div>
                </div>
                <div className="bg-slate-700/20 rounded p-2">
                  <div className="text-slate-400 mb-1">Est. Fees</div>
                  <div className="font-mono text-orange-400">₹{orderPreview.estimatedFees.toLocaleString()}</div>
                </div>
                <div className="bg-slate-700/20 rounded p-2">
                  <div className="text-slate-400 mb-1">Net Amount</div>
                  <div className="font-mono text-purple-400">₹{orderPreview.netAmount.toLocaleString()}</div>
                </div>
                <div className="bg-slate-700/20 rounded p-2">
                  <div className="text-slate-400 mb-1">Risk/Reward</div>
                  <div className="font-mono text-cyan-400">{orderPreview.riskReward.toFixed(2)}:1</div>
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Right Column: Advanced Options */}
        <div className="space-y-6">
          {/* Order Templates */}
          <OrderTemplateSelector
            templates={templates}
            onApplyTemplate={applyTemplate}
            onSaveAsTemplate={saveAsTemplate}
            currentOrder={orderConfig}
          />

          {/* Advanced Options */}
          <AnimatePresence>
            {showAdvancedOptions && (
              <motion.div
                initial={{ opacity: 0, height: 0 }}
                animate={{ opacity: 1, height: 'auto' }}
                exit={{ opacity: 0, height: 0 }}
                className="space-y-6"
              >
                {/* Risk Controls */}
                <RiskControlPanel
                  controls={orderConfig.riskControls}
                  onChange={(controls) => setOrderConfig(prev => ({ ...prev, riskControls: controls }))}
                  availableCash={availableCash}
                  currentPositions={currentPositions}
                />

                {/* Order Conditions */}
                <OrderConditionsPanel
                  conditions={orderConfig.conditions || []}
                  onChange={(conditions) => setOrderConfig(prev => ({ ...prev, conditions }))}
                />
              </motion.div>
            )}
          </AnimatePresence>
        </div>
      </div>

      {/* Action Buttons */}
      <div className="flex items-center justify-between pt-4 border-t border-slate-700/50">
        <div className="flex items-center space-x-2 text-sm text-slate-400">
          <span>Current Price: ₹{currentPrice.toLocaleString()}</span>
          <span>•</span>
          <span>Available: ₹{availableCash.toLocaleString()}</span>
        </div>

        <div className="flex items-center space-x-3">
          <button
            onClick={() => setOrderConfig(prev => ({ ...prev, status: 'draft', id: '', updatedAt: new Date() }))}
            className="px-4 py-2 text-slate-400 hover:text-white transition-colors"
          >
            Reset
          </button>
          
          <button
            onClick={handleOrderSubmit}
            disabled={!isValidOrder || isCalculating}
            className={`px-6 py-2 rounded-lg font-medium transition-all ${
              isValidOrder && !isCalculating
                ? 'bg-gradient-to-r from-purple-500 to-blue-500 hover:from-purple-600 hover:to-blue-600 text-white shadow-lg'
                : 'bg-slate-600 text-slate-400 cursor-not-allowed'
            }`}
          >
            {isCalculating ? (
              <div className="flex items-center space-x-2">
                <RefreshCw className="w-4 h-4 animate-spin" />
                <span>Calculating...</span>
              </div>
            ) : (
              `${orderConfig.side === 'buy' ? 'Buy' : 'Sell'} ${orderConfig.symbol}`
            )}
          </button>
        </div>
      </div>
    </motion.div>
  )
}

export default AdvancedOrderManagement