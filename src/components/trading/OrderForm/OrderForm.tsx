import React, { useState, useEffect, useMemo } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { 
  TrendingUp, 
  TrendingDown, 
  Plus, 
  Minus,
  Shield,
  AlertTriangle,
  CheckCircle,
  Clock,
  ChevronDown
} from 'lucide-react'
import { useConnectionStatus } from '@/hooks/useWebSocket'
import { MultiBrokerService } from '../../../services/brokerService'

interface OrderRequest {
  symbol: string
  side: 'BUY' | 'SELL'
  orderType: 'MARKET' | 'LIMIT' | 'STOP_LOSS' | 'BRACKET'
  quantity: number
  price?: number
  stopLoss?: number
  target?: number
  validity: 'DAY' | 'IOC' | 'GTD'
  brokerAccount: string
}

interface RiskLimits {
  maxPositionSize: number
  maxPortfolioExposure: number
  minStopLossPercent: number
  maxOrderValue: number
}

interface BrokerAccount {
  id: string
  brokerName: 'ZERODHA' | 'UPSTOX' | 'ANGEL' | 'PAYTM_MONEY'
  displayName: string
  balance: number
  marginAvailable: number
  status: 'ACTIVE' | 'INACTIVE' | 'MAINTENANCE'
}

interface OrderFormProps {
  symbol: string
  companyName?: string
  currentPrice: number
  orderTypes: string[]
  maxQuantity: number
  availableBalance: number
  riskLimits: RiskLimits
  brokerAccounts: BrokerAccount[]
  onOrderSubmit: (order: OrderRequest) => Promise<void>
  className?: string
}

type OrderSubmissionState = 
  | { status: 'idle' }
  | { status: 'validating'; progress: number }
  | { status: 'submitting'; orderId?: string }
  | { status: 'success'; orderId: string }
  | { status: 'error'; error: string; retryable: boolean }

const QuantityStepper: React.FC<{
  value: number
  min: number
  max: number
  step: number
  quickValues: number[]
  onValueChange: (value: number) => void
}> = ({ value, min, max, step, quickValues, onValueChange }) => {
  
  const handleDecrement = () => {
    onValueChange(Math.max(min, value - step))
  }

  const handleIncrement = () => {
    onValueChange(Math.min(max, value + step))
  }

  return (
    <div className="space-y-4">
      {/* Stepper Controls */}
      <div className="flex items-center justify-center gap-4">
        <motion.button
          whileTap={{ scale: 0.9 }}
          onClick={handleDecrement}
          disabled={value <= min}
          className="w-12 h-12 rounded-full bg-slate-700 hover:bg-slate-600 disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center text-white text-xl transition-all duration-200"
        >
          <Minus className="h-5 w-5" />
        </motion.button>
        
        <div className="flex-1 max-w-24">
          <input
            type="number"
            value={value}
            onChange={(e) => onValueChange(Math.max(min, Math.min(max, Number(e.target.value) || 0)))}
            className="w-full h-12 text-center text-lg font-semibold bg-slate-800 border border-slate-600 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent"
            min={min}
            max={max}
          />
        </div>

        <motion.button
          whileTap={{ scale: 0.9 }}
          onClick={handleIncrement}
          disabled={value >= max}
          className="w-12 h-12 rounded-full bg-slate-700 hover:bg-slate-600 disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center text-white text-xl transition-all duration-200"
        >
          <Plus className="h-5 w-5" />
        </motion.button>
      </div>

      {/* Quick Values */}
      <div className="grid grid-cols-4 gap-2">
        {quickValues.map((quickValue) => (
          <motion.button
            key={quickValue}
            whileTap={{ scale: 0.95 }}
            onClick={() => onValueChange(Math.min(max, quickValue))}
            className={`h-11 rounded-lg text-sm font-medium transition-all duration-200 ${
              value === quickValue
                ? 'bg-purple-600 text-white'
                : 'bg-slate-800 text-slate-300 hover:bg-slate-700 hover:text-white'
            }`}
          >
            {quickValue === max ? 'Max' : quickValue}
          </motion.button>
        ))}
      </div>
    </div>
  )
}

const RiskIndicator: React.FC<{
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'
  portfolioExposure: number
  orderValue: number
}> = ({ riskLevel, portfolioExposure, orderValue }) => {
  const getRiskColor = () => {
    switch (riskLevel) {
      case 'LOW': return 'text-green-400'
      case 'MEDIUM': return 'text-yellow-400'
      case 'HIGH': return 'text-orange-400'
      case 'CRITICAL': return 'text-red-400'
    }
  }

  const getRiskDots = () => {
    const totalDots = 5
    const filledDots = riskLevel === 'LOW' ? 1 : riskLevel === 'MEDIUM' ? 2 : riskLevel === 'HIGH' ? 4 : 5
    
    return Array.from({ length: totalDots }, (_, i) => (
      <div
        key={i}
        className={`w-2 h-2 rounded-full ${
          i < filledDots 
            ? riskLevel === 'LOW' ? 'bg-green-400' 
              : riskLevel === 'MEDIUM' ? 'bg-yellow-400'
              : riskLevel === 'HIGH' ? 'bg-orange-400'
              : 'bg-red-400'
            : 'bg-slate-600'
        }`}
      />
    ))
  }

  return (
    <div className="bg-slate-800/50 rounded-xl p-4 border border-slate-700">
      <div className="flex items-center justify-between mb-3">
        <div className="flex items-center space-x-2">
          <Shield className={`h-4 w-4 ${getRiskColor()}`} />
          <span className="text-sm font-medium text-slate-300">Risk Level</span>
        </div>
        <div className="flex space-x-1">
          {getRiskDots()}
        </div>
      </div>
      
      <div className="space-y-2 text-sm">
        <div className={`font-semibold ${getRiskColor()}`}>
          {riskLevel} RISK
        </div>
        <div className="text-slate-400">
          {portfolioExposure.toFixed(1)}% of portfolio • ₹{orderValue.toLocaleString('en-IN')}
        </div>
        
        {riskLevel === 'HIGH' || riskLevel === 'CRITICAL' ? (
          <div className="flex items-start space-x-2 mt-3 p-3 bg-red-900/20 border border-red-700/50 rounded-lg">
            <AlertTriangle className="h-4 w-4 text-red-400 mt-0.5 flex-shrink-0" />
            <div className="text-red-300 text-xs">
              {riskLevel === 'CRITICAL' 
                ? 'This order exceeds safe risk limits. Consider reducing position size.'
                : 'High concentration risk. Consider diversifying across sectors.'
              }
            </div>
          </div>
        ) : null}
      </div>
    </div>
  )
}

export const OrderForm: React.FC<OrderFormProps> = ({
  symbol,
  companyName,
  currentPrice,
  orderTypes,
  maxQuantity,
  availableBalance,
  riskLimits,
  brokerAccounts,
  onOrderSubmit,
  className = ''
}) => {
  const [side, setSide] = useState<'BUY' | 'SELL'>('BUY')
  const [orderType, setOrderType] = useState<'MARKET' | 'LIMIT' | 'STOP_LOSS' | 'BRACKET'>('MARKET')
  const [quantity, setQuantity] = useState(100)
  const [price, setPrice] = useState<number>(currentPrice)
  const [stopLoss, setStopLoss] = useState<number>()
  const [target, setTarget] = useState<number>()
  const [validity, setValidity] = useState<'DAY' | 'IOC' | 'GTD'>('DAY')
  const [selectedBroker, setSelectedBroker] = useState(brokerAccounts[0]?.id || '')
  const [submissionState, setSubmissionState] = useState<OrderSubmissionState>({ status: 'idle' })

  const { isConnected } = useConnectionStatus()

  // Mock company name if not provided
  const displayCompanyName = companyName || getCompanyName(symbol)

  function getCompanyName(sym: string): string {
    const names: Record<string, string> = {
      'RELIANCE': 'Reliance Industries Ltd',
      'TCS': 'Tata Consultancy Services',
      'HDFCBANK': 'HDFC Bank Limited',
      'INFY': 'Infosys Limited',
      'ICICIBANK': 'ICICI Bank Limited',
      'WIPRO': 'Wipro Limited'
    }
    return names[sym] || `${sym} Limited`
  }

  // Order calculations
  const orderPrice = orderType === 'MARKET' ? currentPrice : price
  const estimatedValue = quantity * orderPrice
  const canAfford = estimatedValue <= availableBalance
  
  // Risk calculation
  const portfolioExposure = (estimatedValue / availableBalance) * 100
  const riskLevel: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL' = 
    portfolioExposure < 5 ? 'LOW' :
    portfolioExposure < 15 ? 'MEDIUM' :
    portfolioExposure < 30 ? 'HIGH' : 'CRITICAL'

  // Quick quantity values
  const quickQuantities = useMemo(() => {
    const baseAmounts = [25, 50, 100]
    const maxAffordableQty = Math.floor(availableBalance / orderPrice)
    const safeMaxQty = Math.min(maxQuantity, maxAffordableQty)
    
    return [...baseAmounts.filter(q => q <= safeMaxQty), safeMaxQty].slice(0, 4)
  }, [availableBalance, orderPrice, maxQuantity])

  // Validation
  const validationErrors = useMemo(() => {
    const errors: string[] = []
    
    if (quantity <= 0) errors.push('Quantity must be greater than 0')
    if (quantity > maxQuantity) errors.push('Quantity exceeds maximum limit')
    if (!canAfford) errors.push('Insufficient balance for this order')
    if (orderType === 'LIMIT' && (!price || price <= 0)) errors.push('Valid limit price required')
    if (orderType === 'BRACKET' && (!stopLoss || !target)) errors.push('Stop loss and target required for bracket orders')
    if (!selectedBroker) errors.push('Please select a broker account')
    
    return errors
  }, [quantity, maxQuantity, canAfford, orderType, price, stopLoss, target, selectedBroker])

  const isValidOrder = validationErrors.length === 0 && isConnected

  const handleSubmit = async () => {
    if (!isValidOrder) return

    const order: OrderRequest = {
      symbol,
      side,
      orderType,
      quantity,
      price: orderType === 'MARKET' ? undefined : price,
      stopLoss,
      target,
      validity,
      brokerAccount: selectedBroker
    }

    try {
      setSubmissionState({ status: 'submitting' })
      await onOrderSubmit(order)
      setSubmissionState({ status: 'success', orderId: `ORD${Date.now()}` })
      
      // Reset form after successful submission
      setTimeout(() => {
        setSubmissionState({ status: 'idle' })
        setQuantity(100)
        if (orderType === 'LIMIT') setPrice(currentPrice)
        setStopLoss(undefined)
        setTarget(undefined)
      }, 3000)
    } catch (error) {
      setSubmissionState({ 
        status: 'error', 
        error: error instanceof Error ? error.message : 'Order submission failed',
        retryable: true
      })
    }
  }

  // Update price when current price changes (for market orders)
  useEffect(() => {
    if (orderType === 'MARKET') {
      setPrice(currentPrice)
    }
  }, [currentPrice, orderType])

  return (
    <motion.div 
      className={`glass-widget-card rounded-2xl overflow-hidden ${className}`}
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.6 }}
    >
      {/* Header */}
      <div className="p-6 border-b border-slate-700/50">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-xl font-bold text-white">{symbol}</h2>
            <p className="text-sm text-slate-400">{displayCompanyName}</p>
          </div>
          <div className="text-right">
            <div className="text-lg font-bold text-white">₹{currentPrice.toFixed(2)}</div>
            <div className="flex items-center text-sm text-slate-400">
              <div className={`w-2 h-2 rounded-full mr-2 ${
                isConnected ? 'bg-green-400' : 'bg-red-400'
              }`} />
              NSE • Last: {new Date().toLocaleTimeString('en-IN', { 
                hour: '2-digit', 
                minute: '2-digit' 
              })}
            </div>
          </div>
        </div>
      </div>

      <div className="p-6 space-y-6">
        {/* Buy/Sell Toggle */}
        <div className="grid grid-cols-2 gap-2 p-1 bg-slate-800 rounded-xl">
          <motion.button
            whileTap={{ scale: 0.98 }}
            onClick={() => setSide('BUY')}
            className={`h-14 rounded-lg font-semibold text-lg transition-all duration-300 ${
              side === 'BUY'
                ? 'bg-green-600 text-white shadow-lg shadow-green-600/30'
                : 'text-slate-400 hover:text-white hover:bg-slate-700'
            }`}
          >
            <div className="flex items-center justify-center space-x-2">
              <TrendingUp className="h-5 w-5" />
              <span>BUY</span>
            </div>
          </motion.button>
          
          <motion.button
            whileTap={{ scale: 0.98 }}
            onClick={() => setSide('SELL')}
            className={`h-14 rounded-lg font-semibold text-lg transition-all duration-300 ${
              side === 'SELL'
                ? 'bg-red-600 text-white shadow-lg shadow-red-600/30'
                : 'text-slate-400 hover:text-white hover:bg-slate-700'
            }`}
          >
            <div className="flex items-center justify-center space-x-2">
              <TrendingDown className="h-5 w-5" />
              <span>SELL</span>
            </div>
          </motion.button>
        </div>

        {/* Quantity Selection */}
        <div className="space-y-3">
          <label className="block text-sm font-medium text-slate-300">
            Quantity
          </label>
          <QuantityStepper
            value={quantity}
            min={1}
            max={maxQuantity}
            step={1}
            quickValues={quickQuantities}
            onValueChange={setQuantity}
          />
        </div>

        {/* Order Type Selection */}
        <div className="space-y-3">
          <label className="block text-sm font-medium text-slate-300">
            Order Type
          </label>
          <div className="relative">
            <select
              value={orderType}
              onChange={(e) => setOrderType(e.target.value as any)}
              className="w-full h-12 bg-slate-800 border border-slate-600 rounded-lg text-white px-4 pr-10 appearance-none focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent"
            >
              <option value="MARKET">Market Order</option>
              <option value="LIMIT">Limit Order</option>
              <option value="STOP_LOSS">Stop Loss Order</option>
              <option value="BRACKET">Bracket Order</option>
            </select>
            <ChevronDown className="absolute right-3 top-1/2 transform -translate-y-1/2 h-5 w-5 text-slate-400 pointer-events-none" />
          </div>
        </div>

        {/* Price Input (for Limit Orders) */}
        <AnimatePresence>
          {orderType === 'LIMIT' && (
            <motion.div
              initial={{ opacity: 0, height: 0 }}
              animate={{ opacity: 1, height: 'auto' }}
              exit={{ opacity: 0, height: 0 }}
              className="space-y-3"
            >
              <label className="block text-sm font-medium text-slate-300">
                Limit Price
              </label>
              <input
                type="number"
                value={price}
                onChange={(e) => setPrice(Number(e.target.value) || 0)}
                step={0.05}
                className="w-full h-12 bg-slate-800 border border-slate-600 rounded-lg text-white px-4 text-lg font-semibold focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent"
                placeholder="₹0.00"
              />
            </motion.div>
          )}
        </AnimatePresence>

        {/* Bracket Order Fields */}
        <AnimatePresence>
          {orderType === 'BRACKET' && (
            <motion.div
              initial={{ opacity: 0, height: 0 }}
              animate={{ opacity: 1, height: 'auto' }}
              exit={{ opacity: 0, height: 0 }}
              className="space-y-4"
            >
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-slate-300 mb-2">
                    Stop Loss
                  </label>
                  <input
                    type="number"
                    value={stopLoss || ''}
                    onChange={(e) => setStopLoss(Number(e.target.value) || undefined)}
                    step={0.05}
                    className="w-full h-12 bg-slate-800 border border-slate-600 rounded-lg text-white px-4 focus:outline-none focus:ring-2 focus:ring-red-500 focus:border-transparent"
                    placeholder="₹0.00"
                  />
                </div>
                
                <div>
                  <label className="block text-sm font-medium text-slate-300 mb-2">
                    Target
                  </label>
                  <input
                    type="number"
                    value={target || ''}
                    onChange={(e) => setTarget(Number(e.target.value) || undefined)}
                    step={0.05}
                    className="w-full h-12 bg-slate-800 border border-slate-600 rounded-lg text-white px-4 focus:outline-none focus:ring-2 focus:ring-green-500 focus:border-transparent"
                    placeholder="₹0.00"
                  />
                </div>
              </div>
            </motion.div>
          )}
        </AnimatePresence>

        {/* Order Summary */}
        <div className="bg-slate-800/50 rounded-xl p-4 space-y-2">
          <div className="flex justify-between text-sm">
            <span className="text-slate-400">Est. Value:</span>
            <span className="font-semibold text-white">₹{estimatedValue.toLocaleString('en-IN')}</span>
          </div>
          <div className="flex justify-between text-sm">
            <span className="text-slate-400">Available:</span>
            <span className={`font-semibold ${canAfford ? 'text-green-400' : 'text-red-400'}`}>
              ₹{availableBalance.toLocaleString('en-IN')}
            </span>
          </div>
        </div>

        {/* Risk Assessment */}
        <RiskIndicator
          riskLevel={riskLevel}
          portfolioExposure={portfolioExposure}
          orderValue={estimatedValue}
        />

        {/* Validation Errors */}
        <AnimatePresence>
          {validationErrors.length > 0 && (
            <motion.div
              initial={{ opacity: 0, height: 0 }}
              animate={{ opacity: 1, height: 'auto' }}
              exit={{ opacity: 0, height: 0 }}
              className="bg-red-900/20 border border-red-700/50 rounded-lg p-4"
            >
              {validationErrors.map((error, index) => (
                <div key={index} className="flex items-center space-x-2 text-red-300 text-sm">
                  <AlertTriangle className="h-4 w-4 flex-shrink-0" />
                  <span>{error}</span>
                </div>
              ))}
            </motion.div>
          )}
        </AnimatePresence>

        {/* Submit Button */}
        <motion.button
          whileTap={isValidOrder ? { scale: 0.98 } : {}}
          onClick={handleSubmit}
          disabled={!isValidOrder || submissionState.status === 'submitting'}
          className={`w-full h-14 rounded-xl font-bold text-lg transition-all duration-300 ${
            submissionState.status === 'submitting'
              ? 'bg-slate-600 text-slate-300 cursor-not-allowed'
              : submissionState.status === 'success'
              ? 'bg-green-600 text-white'
              : submissionState.status === 'error'
              ? 'bg-red-600 text-white'
              : isValidOrder
              ? side === 'BUY' 
                ? 'bg-green-600 hover:bg-green-700 text-white shadow-lg shadow-green-600/30'
                : 'bg-red-600 hover:bg-red-700 text-white shadow-lg shadow-red-600/30'
              : 'bg-slate-600 text-slate-400 cursor-not-allowed'
          }`}
        >
          <div className="flex items-center justify-center space-x-2">
            {submissionState.status === 'submitting' && (
              <div className="w-5 h-5 border-2 border-slate-300 border-t-transparent rounded-full animate-spin" />
            )}
            {submissionState.status === 'success' && (
              <CheckCircle className="h-5 w-5" />
            )}
            {submissionState.status === 'error' && (
              <AlertTriangle className="h-5 w-5" />
            )}
            {submissionState.status === 'idle' && side === 'BUY' && (
              <TrendingUp className="h-5 w-5" />
            )}
            {submissionState.status === 'idle' && side === 'SELL' && (
              <TrendingDown className="h-5 w-5" />
            )}
            
            <span>
              {submissionState.status === 'submitting' ? 'Placing Order...' :
               submissionState.status === 'success' ? 'Order Placed!' :
               submissionState.status === 'error' ? 'Retry Order' :
               `Place ${side} Order`}
            </span>
          </div>
          
          {submissionState.status === 'idle' && (
            <div className="text-sm font-normal opacity-80">
              ₹{estimatedValue.toLocaleString('en-IN')}
            </div>
          )}
        </motion.button>
      </div>
    </motion.div>
  )
}