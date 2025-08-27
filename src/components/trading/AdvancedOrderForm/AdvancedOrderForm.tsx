import React, { useState, useEffect, useMemo } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { 
  TrendingUp, 
  TrendingDown,
  Shield,
  Target,
  AlertTriangle,
  Clock,
  Zap,
  Calculator,
  DollarSign,
  Activity,
  CheckCircle,
  Settings,
  Info,
  ArrowRight
} from 'lucide-react'

export type OrderSide = 'buy' | 'sell'
export type OrderType = 'market' | 'limit' | 'stop_loss' | 'stop_limit' | 'bracket' | 'cover' | 'iceberg'
export type OrderValidity = 'day' | 'ioc' | 'gtc'
export type ProductType = 'delivery' | 'intraday' | 'margin'

export interface OrderRequest {
  symbol: string
  side: OrderSide
  orderType: OrderType
  productType: ProductType
  quantity: number
  price?: number
  stopPrice?: number
  targetPrice?: number
  stopLossPrice?: number
  validity: OrderValidity
  disclosedQuantity?: number
  broker: string
  riskAmount: number
  expectedReturn: number
  marginRequired: number
}

export interface RiskCalculation {
  maxLoss: number
  maxGain: number
  riskRewardRatio: number
  marginRequired: number
  leverage: number
  positionSize: number
}

export interface AdvancedOrderFormProps {
  symbol: string
  currentPrice: number
  availableBalance: number
  brokers: string[]
  onSubmitOrder: (order: OrderRequest) => void
  onCalculateRisk: (params: any) => RiskCalculation
  marketHours: boolean
  compactMode?: boolean
  className?: string
}

const AdvancedOrderForm: React.FC<AdvancedOrderFormProps> = ({
  symbol,
  currentPrice,
  availableBalance,
  brokers,
  onSubmitOrder,
  onCalculateRisk,
  marketHours,
  compactMode = false,
  className = ''
}) => {
  const [formData, setFormData] = useState<Partial<OrderRequest>>({
    symbol,
    side: 'buy',
    orderType: 'limit',
    productType: 'delivery',
    quantity: 1,
    price: currentPrice,
    validity: 'day',
    broker: brokers[0] || 'Zerodha'
  })
  
  const [riskCalculation, setRiskCalculation] = useState<RiskCalculation | null>(null)
  const [isCalculating, setIsCalculating] = useState(false)
  const [showAdvanced, setShowAdvanced] = useState(false)
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [isSubmitting, setIsSubmitting] = useState(false)

  // Auto-calculate risk when relevant fields change
  useEffect(() => {
    if (formData.quantity && formData.price && formData.orderType !== 'market') {
      calculateRisk()
    }
  }, [formData.quantity, formData.price, formData.stopPrice, formData.targetPrice, formData.orderType])

  const calculateRisk = async () => {
    if (!formData.quantity || (!formData.price && formData.orderType !== 'market')) return

    setIsCalculating(true)
    
    // Mock risk calculation
    await new Promise(resolve => setTimeout(resolve, 300))
    
    const entryPrice = formData.orderType === 'market' ? currentPrice : (formData.price || currentPrice)
    const positionValue = (formData.quantity || 0) * entryPrice
    
    const mockCalculation: RiskCalculation = {
      maxLoss: formData.stopLossPrice 
        ? Math.abs(entryPrice - formData.stopLossPrice) * (formData.quantity || 0)
        : positionValue * 0.1, // 10% default risk
      maxGain: formData.targetPrice
        ? Math.abs(formData.targetPrice - entryPrice) * (formData.quantity || 0)
        : positionValue * 0.15, // 15% default target
      riskRewardRatio: 0,
      marginRequired: formData.productType === 'margin' ? positionValue * 0.2 : positionValue,
      leverage: formData.productType === 'margin' ? 5 : 1,
      positionSize: positionValue
    }

    if (mockCalculation.maxLoss > 0) {
      mockCalculation.riskRewardRatio = mockCalculation.maxGain / mockCalculation.maxLoss
    }

    setRiskCalculation(mockCalculation)
    setIsCalculating(false)
  }

  const validateForm = (): boolean => {
    const newErrors: Record<string, string> = {}

    if (!formData.quantity || formData.quantity <= 0) {
      newErrors.quantity = 'Quantity must be greater than 0'
    }

    if (formData.orderType !== 'market' && (!formData.price || formData.price <= 0)) {
      newErrors.price = 'Price must be greater than 0'
    }

    if (formData.orderType === 'stop_loss' && (!formData.stopPrice || formData.stopPrice <= 0)) {
      newErrors.stopPrice = 'Stop price is required for stop loss orders'
    }

    if (formData.orderType === 'bracket') {
      if (!formData.stopLossPrice) newErrors.stopLossPrice = 'Stop loss is required for bracket orders'
      if (!formData.targetPrice) newErrors.targetPrice = 'Target price is required for bracket orders'
    }

    if (riskCalculation && riskCalculation.marginRequired > availableBalance) {
      newErrors.balance = 'Insufficient balance for this order'
    }

    setErrors(newErrors)
    return Object.keys(newErrors).length === 0
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    
    if (!validateForm()) return

    setIsSubmitting(true)

    try {
      const orderRequest: OrderRequest = {
        symbol: formData.symbol || symbol,
        side: formData.side || 'buy',
        orderType: formData.orderType || 'limit',
        productType: formData.productType || 'delivery',
        quantity: formData.quantity || 0,
        price: formData.orderType === 'market' ? undefined : formData.price,
        stopPrice: formData.stopPrice,
        targetPrice: formData.targetPrice,
        stopLossPrice: formData.stopLossPrice,
        validity: formData.validity || 'day',
        disclosedQuantity: formData.disclosedQuantity,
        broker: formData.broker || brokers[0],
        riskAmount: riskCalculation?.maxLoss || 0,
        expectedReturn: riskCalculation?.maxGain || 0,
        marginRequired: riskCalculation?.marginRequired || 0
      }

      await onSubmitOrder(orderRequest)

      // Reset form on successful submission
      setFormData({
        symbol,
        side: 'buy',
        orderType: 'limit',
        productType: 'delivery',
        quantity: 1,
        price: currentPrice,
        validity: 'day',
        broker: brokers[0] || 'Zerodha'
      })
      setRiskCalculation(null)

    } finally {
      setIsSubmitting(false)
    }
  }

  const updateField = (field: keyof OrderRequest, value: any) => {
    setFormData(prev => ({
      ...prev,
      [field]: value
    }))
  }

  const formatCurrency = (value: number) => {
    return `₹${value.toLocaleString('en-IN', { 
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    })}`
  }

  const getOrderTypeDescription = (type: OrderType) => {
    switch (type) {
      case 'market':
        return 'Execute immediately at current market price'
      case 'limit':
        return 'Execute only at specified price or better'
      case 'stop_loss':
        return 'Trigger market order when price hits stop price'
      case 'stop_limit':
        return 'Trigger limit order when price hits stop price'
      case 'bracket':
        return 'Auto square-off with stop loss and target'
      case 'cover':
        return 'Intraday order with compulsory stop loss'
      case 'iceberg':
        return 'Large order split into smaller disclosed quantities'
      default:
        return ''
    }
  }

  const getRiskColor = (ratio: number) => {
    if (ratio >= 2) return 'text-green-400'
    if (ratio >= 1) return 'text-yellow-400'
    return 'text-red-400'
  }

  return (
    <div className={`glass-widget-card rounded-2xl ${className}`}>
      {/* Header */}
      <div className="p-6 border-b border-slate-700/50">
        <div className="flex items-center justify-between mb-4">
          <div>
            <h2 className="text-xl font-bold text-white">Place Order</h2>
            <div className="flex items-center space-x-2 text-sm text-slate-400 mt-1">
              <span>{symbol}</span>
              <span>•</span>
              <span className="text-white">₹{currentPrice.toFixed(2)}</span>
              <span>•</span>
              <span className={marketHours ? 'text-green-400' : 'text-red-400'}>
                {marketHours ? 'Market Open' : 'Market Closed'}
              </span>
            </div>
          </div>
          
          <button
            onClick={() => setShowAdvanced(!showAdvanced)}
            className="cyber-button-sm px-4 py-2 rounded-xl transition-all duration-300"
          >
            <Settings className="w-4 h-4 mr-2" />
            {showAdvanced ? 'Simple' : 'Advanced'}
          </button>
        </div>
      </div>

      <form onSubmit={handleSubmit} className="p-6 space-y-6">
        {/* Buy/Sell Toggle */}
        <div className="flex rounded-xl bg-slate-800/50 p-1">
          <button
            type="button"
            onClick={() => updateField('side', 'buy')}
            className={`flex-1 px-4 py-3 rounded-lg font-medium transition-all duration-300 ${
              formData.side === 'buy'
                ? 'bg-green-600 text-white shadow-lg shadow-green-600/25'
                : 'text-slate-400 hover:text-white'
            }`}
          >
            <TrendingUp className="w-4 h-4 mr-2 inline" />
            BUY
          </button>
          <button
            type="button"
            onClick={() => updateField('side', 'sell')}
            className={`flex-1 px-4 py-3 rounded-lg font-medium transition-all duration-300 ${
              formData.side === 'sell'
                ? 'bg-red-600 text-white shadow-lg shadow-red-600/25'
                : 'text-slate-400 hover:text-white'
            }`}
          >
            <TrendingDown className="w-4 h-4 mr-2 inline" />
            SELL
          </button>
        </div>

        {/* Order Type Selection */}
        <div>
          <label className="block text-sm font-medium text-slate-300 mb-2">Order Type</label>
          <select
            value={formData.orderType}
            onChange={(e) => updateField('orderType', e.target.value as OrderType)}
            className="w-full glass-input rounded-xl px-4 py-3 text-white"
          >
            <option value="market">Market Order</option>
            <option value="limit">Limit Order</option>
            <option value="stop_loss">Stop Loss</option>
            <option value="stop_limit">Stop Limit</option>
            <option value="bracket">Bracket Order</option>
            <option value="cover">Cover Order</option>
            {showAdvanced && <option value="iceberg">Iceberg Order</option>}
          </select>
          <div className="text-xs text-slate-400 mt-1 flex items-center">
            <Info className="w-3 h-3 mr-1" />
            {getOrderTypeDescription(formData.orderType || 'market')}
          </div>
        </div>

        {/* Quantity and Price */}
        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-slate-300 mb-2">Quantity</label>
            <input
              type="number"
              min="1"
              value={formData.quantity}
              onChange={(e) => updateField('quantity', parseInt(e.target.value) || 0)}
              className={`w-full glass-input rounded-xl px-4 py-3 text-white ${
                errors.quantity ? 'border-red-400' : ''
              }`}
              placeholder="Enter quantity"
            />
            {errors.quantity && <div className="text-red-400 text-xs mt-1">{errors.quantity}</div>}
          </div>

          {formData.orderType !== 'market' && (
            <div>
              <label className="block text-sm font-medium text-slate-300 mb-2">
                {formData.orderType === 'limit' ? 'Limit Price' : 'Price'}
              </label>
              <input
                type="number"
                step="0.05"
                min="0"
                value={formData.price}
                onChange={(e) => updateField('price', parseFloat(e.target.value) || 0)}
                className={`w-full glass-input rounded-xl px-4 py-3 text-white ${
                  errors.price ? 'border-red-400' : ''
                }`}
                placeholder="Enter price"
              />
              {errors.price && <div className="text-red-400 text-xs mt-1">{errors.price}</div>}
            </div>
          )}
        </div>

        {/* Stop Loss and Target (for bracket orders) */}
        {(formData.orderType === 'bracket' || formData.orderType === 'stop_loss' || showAdvanced) && (
          <AnimatePresence>
            <motion.div
              initial={{ opacity: 0, height: 0 }}
              animate={{ opacity: 1, height: 'auto' }}
              exit={{ opacity: 0, height: 0 }}
              className="space-y-4"
            >
              {formData.orderType === 'stop_loss' && (
                <div>
                  <label className="block text-sm font-medium text-slate-300 mb-2">Stop Price</label>
                  <input
                    type="number"
                    step="0.05"
                    min="0"
                    value={formData.stopPrice}
                    onChange={(e) => updateField('stopPrice', parseFloat(e.target.value) || 0)}
                    className={`w-full glass-input rounded-xl px-4 py-3 text-white ${
                      errors.stopPrice ? 'border-red-400' : ''
                    }`}
                    placeholder="Stop trigger price"
                  />
                  {errors.stopPrice && <div className="text-red-400 text-xs mt-1">{errors.stopPrice}</div>}
                </div>
              )}

              {(formData.orderType === 'bracket' || (showAdvanced && formData.orderType === 'limit')) && (
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-slate-300 mb-2">
                      <Shield className="w-4 h-4 inline mr-1" />
                      Stop Loss
                    </label>
                    <input
                      type="number"
                      step="0.05"
                      min="0"
                      value={formData.stopLossPrice}
                      onChange={(e) => updateField('stopLossPrice', parseFloat(e.target.value) || 0)}
                      className={`w-full glass-input rounded-xl px-4 py-3 text-white ${
                        errors.stopLossPrice ? 'border-red-400' : ''
                      }`}
                      placeholder="Stop loss price"
                    />
                    {errors.stopLossPrice && <div className="text-red-400 text-xs mt-1">{errors.stopLossPrice}</div>}
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-slate-300 mb-2">
                      <Target className="w-4 h-4 inline mr-1" />
                      Target
                    </label>
                    <input
                      type="number"
                      step="0.05"
                      min="0"
                      value={formData.targetPrice}
                      onChange={(e) => updateField('targetPrice', parseFloat(e.target.value) || 0)}
                      className={`w-full glass-input rounded-xl px-4 py-3 text-white ${
                        errors.targetPrice ? 'border-red-400' : ''
                      }`}
                      placeholder="Target price"
                    />
                    {errors.targetPrice && <div className="text-red-400 text-xs mt-1">{errors.targetPrice}</div>}
                  </div>
                </div>
              )}
            </motion.div>
          </AnimatePresence>
        )}

        {/* Advanced Options */}
        {showAdvanced && (
          <AnimatePresence>
            <motion.div
              initial={{ opacity: 0, height: 0 }}
              animate={{ opacity: 1, height: 'auto' }}
              exit={{ opacity: 0, height: 0 }}
              className="space-y-4"
            >
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-slate-300 mb-2">Product Type</label>
                  <select
                    value={formData.productType}
                    onChange={(e) => updateField('productType', e.target.value as ProductType)}
                    className="w-full glass-input rounded-xl px-4 py-3 text-white"
                  >
                    <option value="delivery">Delivery</option>
                    <option value="intraday">Intraday</option>
                    <option value="margin">Margin</option>
                  </select>
                </div>

                <div>
                  <label className="block text-sm font-medium text-slate-300 mb-2">Validity</label>
                  <select
                    value={formData.validity}
                    onChange={(e) => updateField('validity', e.target.value as OrderValidity)}
                    className="w-full glass-input rounded-xl px-4 py-3 text-white"
                  >
                    <option value="day">Day</option>
                    <option value="ioc">IOC (Immediate or Cancel)</option>
                    <option value="gtc">GTC (Good Till Cancelled)</option>
                  </select>
                </div>
              </div>

              {brokers.length > 1 && (
                <div>
                  <label className="block text-sm font-medium text-slate-300 mb-2">Broker</label>
                  <select
                    value={formData.broker}
                    onChange={(e) => updateField('broker', e.target.value)}
                    className="w-full glass-input rounded-xl px-4 py-3 text-white"
                  >
                    {brokers.map(broker => (
                      <option key={broker} value={broker}>{broker}</option>
                    ))}
                  </select>
                </div>
              )}

              {formData.orderType === 'iceberg' && (
                <div>
                  <label className="block text-sm font-medium text-slate-300 mb-2">Disclosed Quantity</label>
                  <input
                    type="number"
                    min="1"
                    max={formData.quantity}
                    value={formData.disclosedQuantity}
                    onChange={(e) => updateField('disclosedQuantity', parseInt(e.target.value) || 0)}
                    className="w-full glass-input rounded-xl px-4 py-3 text-white"
                    placeholder="Quantity to disclose"
                  />
                </div>
              )}
            </motion.div>
          </AnimatePresence>
        )}

        {/* Risk Analysis */}
        {riskCalculation && (
          <div className="glass-panel p-4 rounded-xl">
            <div className="flex items-center justify-between mb-3">
              <h3 className="font-semibold text-white flex items-center">
                <Calculator className="w-4 h-4 mr-2" />
                Risk Analysis
              </h3>
              {isCalculating && (
                <div className="w-4 h-4 border-2 border-purple-400 border-t-transparent rounded-full animate-spin" />
              )}
            </div>

            <div className="grid grid-cols-2 gap-4 text-sm">
              <div>
                <div className="text-slate-400 mb-1">Position Size</div>
                <div className="text-white font-medium">{formatCurrency(riskCalculation.positionSize)}</div>
              </div>

              <div>
                <div className="text-slate-400 mb-1">Margin Required</div>
                <div className="text-white font-medium">{formatCurrency(riskCalculation.marginRequired)}</div>
              </div>

              <div>
                <div className="text-slate-400 mb-1">Max Loss</div>
                <div className="text-red-400 font-medium">{formatCurrency(riskCalculation.maxLoss)}</div>
              </div>

              <div>
                <div className="text-slate-400 mb-1">Max Gain</div>
                <div className="text-green-400 font-medium">{formatCurrency(riskCalculation.maxGain)}</div>
              </div>
            </div>

            {riskCalculation.riskRewardRatio > 0 && (
              <div className="mt-3 flex items-center justify-between">
                <span className="text-slate-400 text-sm">Risk:Reward Ratio</span>
                <span className={`font-semibold ${getRiskColor(riskCalculation.riskRewardRatio)}`}>
                  1:{riskCalculation.riskRewardRatio.toFixed(2)}
                </span>
              </div>
            )}
          </div>
        )}

        {/* Balance Check */}
        <div className="flex items-center justify-between text-sm">
          <span className="text-slate-400">Available Balance:</span>
          <span className="text-white font-medium">{formatCurrency(availableBalance)}</span>
        </div>

        {errors.balance && (
          <div className="flex items-center space-x-2 text-red-400 text-sm">
            <AlertTriangle className="w-4 h-4" />
            <span>{errors.balance}</span>
          </div>
        )}

        {/* Submit Button */}
        <button
          type="submit"
          disabled={isSubmitting || !marketHours}
          className={`w-full px-6 py-4 rounded-xl font-semibold text-white transition-all duration-300 flex items-center justify-center space-x-2 ${
            formData.side === 'buy'
              ? 'bg-green-600 hover:bg-green-500 shadow-lg shadow-green-600/25'
              : 'bg-red-600 hover:bg-red-500 shadow-lg shadow-red-600/25'
          } ${
            (isSubmitting || !marketHours) ? 'opacity-50 cursor-not-allowed' : 'hover:scale-105'
          }`}
        >
          {isSubmitting ? (
            <>
              <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin" />
              <span>Placing Order...</span>
            </>
          ) : (
            <>
              <span>{formData.side?.toUpperCase()} {symbol}</span>
              <ArrowRight className="w-4 h-4" />
            </>
          )}
        </button>

        {!marketHours && (
          <div className="text-center text-yellow-400 text-sm flex items-center justify-center space-x-2">
            <Clock className="w-4 h-4" />
            <span>Orders can be placed when market is open</span>
          </div>
        )}
      </form>
    </div>
  )
}

export { AdvancedOrderForm }
export default AdvancedOrderForm