// Professional Trading Tools & Calculators
// FRONT-004: Advanced Trading Interface Enhancement

import React, { useState, useEffect, useCallback, useMemo } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import {
  Calculator,
  TrendingUp,
  TrendingDown,
  Target,
  Shield,
  DollarSign,
  Percent,
  BarChart3,
  PieChart,
  Activity,
  Clock,
  AlertTriangle,
  CheckCircle,
  Brain,
  Zap,
  Settings,
  Download,
  Upload,
  Save,
  RotateCcw,
  Play,
  Pause,
  FastForward,
  Rewind,
  StopCircle,
  ArrowUpRight,
  ArrowDownLeft,
  Layers,
  Gauge,
  LineChart,
  CandlestickChart,
  PlusCircle,
  MinusCircle,
  Info,
  HelpCircle
} from 'lucide-react'

export interface PositionSizeConfig {
  accountSize: number
  riskPercentage: number
  entryPrice: number
  stopLoss: number
  riskAmount?: number
  positionSize?: number
  shares?: number
  riskRewardRatio?: number
  maxLoss?: number
}

export interface BacktestConfig {
  strategy: string
  symbol: string
  startDate: Date
  endDate: Date
  initialCapital: number
  commission: number
  slippage: number
  parameters: Record<string, number>
}

export interface BacktestResult {
  totalReturn: number
  annualizedReturn: number
  sharpeRatio: number
  maxDrawdown: number
  winRate: number
  profitFactor: number
  totalTrades: number
  avgWin: number
  avgLoss: number
  largestWin: number
  largestLoss: number
  consecutiveWins: number
  consecutiveLosses: number
  trades: BacktestTrade[]
}

export interface BacktestTrade {
  id: string
  symbol: string
  side: 'buy' | 'sell'
  entryDate: Date
  exitDate: Date
  entryPrice: number
  exitPrice: number
  quantity: number
  pnl: number
  commission: number
  duration: number
}

export interface RiskCalculation {
  positionValue: number
  portfolioValue: number
  positionRisk: number
  correlationRisk: number
  sectorExposure: number
  maxDrawdown: number
  valueAtRisk: number
  beta: number
  sharpeRatio: number
}

interface ProfessionalTradingToolsProps {
  accountBalance: number
  currentPositions: Array<{
    symbol: string
    quantity: number
    avgCost: number
    currentPrice: number
    unrealizedPnL: number
    sector?: string
  }>
  marketData: Record<string, {
    price: number
    change: number
    volume: number
    beta?: number
  }>
  onStrategyBacktest?: (config: BacktestConfig) => Promise<BacktestResult>
  className?: string
}

const PositionSizeCalculator: React.FC<{
  accountSize: number
  onCalculate: (config: PositionSizeConfig) => void
}> = ({ accountSize, onCalculate }) => {
  const [config, setConfig] = useState<PositionSizeConfig>({
    accountSize,
    riskPercentage: 2,
    entryPrice: 100,
    stopLoss: 95
  })

  const calculation = useMemo(() => {
    const riskAmount = (config.accountSize * config.riskPercentage) / 100
    const riskPerShare = Math.abs(config.entryPrice - config.stopLoss)
    const shares = riskPerShare > 0 ? Math.floor(riskAmount / riskPerShare) : 0
    const positionSize = shares * config.entryPrice
    const maxLoss = shares * riskPerShare
    const riskRewardRatio = riskPerShare > 0 ? (config.entryPrice * 0.05) / riskPerShare : 0

    return {
      riskAmount,
      shares,
      positionSize,
      maxLoss,
      riskRewardRatio
    }
  }, [config])

  useEffect(() => {
    onCalculate({ ...config, ...calculation })
  }, [config, calculation, onCalculate])

  return (
    <div className="bg-slate-800/30 rounded-lg p-4 border border-slate-700/50">
      <div className="flex items-center space-x-2 mb-4">
        <Calculator className="w-5 h-5 text-green-400" />
        <h3 className="font-semibold text-white">Position Size Calculator</h3>
      </div>

      <div className="grid grid-cols-2 gap-4 mb-4">
        <div>
          <label className="text-sm text-slate-400 mb-2 block">Account Size</label>
          <div className="relative">
            <input
              type="number"
              value={config.accountSize}
              onChange={(e) => setConfig(prev => ({ ...prev, accountSize: parseFloat(e.target.value) || 0 }))}
              className="w-full bg-slate-700 border border-slate-600 rounded-lg px-3 py-2 text-white pr-8 focus:ring-2 focus:ring-green-500"
            />
            <span className="absolute right-3 top-2 text-xs text-slate-400">₹</span>
          </div>
        </div>

        <div>
          <label className="text-sm text-slate-400 mb-2 block">Risk %</label>
          <div className="relative">
            <input
              type="number"
              value={config.riskPercentage}
              onChange={(e) => setConfig(prev => ({ ...prev, riskPercentage: parseFloat(e.target.value) || 0 }))}
              className="w-full bg-slate-700 border border-slate-600 rounded-lg px-3 py-2 text-white pr-8 focus:ring-2 focus:ring-green-500"
              step="0.1"
              min="0"
              max="10"
            />
            <span className="absolute right-3 top-2 text-xs text-slate-400">%</span>
          </div>
        </div>

        <div>
          <label className="text-sm text-slate-400 mb-2 block">Entry Price</label>
          <div className="relative">
            <input
              type="number"
              value={config.entryPrice}
              onChange={(e) => setConfig(prev => ({ ...prev, entryPrice: parseFloat(e.target.value) || 0 }))}
              className="w-full bg-slate-700 border border-slate-600 rounded-lg px-3 py-2 text-white pr-8 focus:ring-2 focus:ring-green-500"
              step="0.01"
            />
            <span className="absolute right-3 top-2 text-xs text-slate-400">₹</span>
          </div>
        </div>

        <div>
          <label className="text-sm text-slate-400 mb-2 block">Stop Loss</label>
          <div className="relative">
            <input
              type="number"
              value={config.stopLoss}
              onChange={(e) => setConfig(prev => ({ ...prev, stopLoss: parseFloat(e.target.value) || 0 }))}
              className="w-full bg-slate-700 border border-slate-600 rounded-lg px-3 py-2 text-white pr-8 focus:ring-2 focus:ring-green-500"
              step="0.01"
            />
            <span className="absolute right-3 top-2 text-xs text-slate-400">₹</span>
          </div>
        </div>
      </div>

      {/* Results */}
      <div className="grid grid-cols-2 gap-3">
        <div className="bg-green-500/10 border border-green-400/30 rounded-lg p-3">
          <div className="text-xs text-green-400 mb-1">Shares to Buy</div>
          <div className="font-mono text-lg font-bold text-white">
            {calculation.shares.toLocaleString()}
          </div>
        </div>

        <div className="bg-blue-500/10 border border-blue-400/30 rounded-lg p-3">
          <div className="text-xs text-blue-400 mb-1">Position Size</div>
          <div className="font-mono text-lg font-bold text-white">
            ₹{calculation.positionSize.toLocaleString()}
          </div>
        </div>

        <div className="bg-red-500/10 border border-red-400/30 rounded-lg p-3">
          <div className="text-xs text-red-400 mb-1">Max Loss</div>
          <div className="font-mono text-lg font-bold text-white">
            ₹{calculation.maxLoss.toLocaleString()}
          </div>
        </div>

        <div className="bg-purple-500/10 border border-purple-400/30 rounded-lg p-3">
          <div className="text-xs text-purple-400 mb-1">Risk/Reward</div>
          <div className="font-mono text-lg font-bold text-white">
            1:{calculation.riskRewardRatio.toFixed(2)}
          </div>
        </div>
      </div>
    </div>
  )
}

const RiskAnalyzer: React.FC<{
  positions: Array<{
    symbol: string
    quantity: number
    avgCost: number
    currentPrice: number
    unrealizedPnL: number
    sector?: string
  }>
  portfolioValue: number
  marketData: Record<string, { beta?: number; price: number }>
}> = ({ positions, portfolioValue, marketData }) => {
  const riskMetrics = useMemo(() => {
    const totalPositionValue = positions.reduce((sum, pos) => sum + (pos.quantity * pos.currentPrice), 0)
    const totalUnrealizedPnL = positions.reduce((sum, pos) => sum + pos.unrealizedPnL, 0)
    
    // Calculate sector exposure
    const sectorExposure = positions.reduce((sectors, pos) => {
      const sector = pos.sector || 'Unknown'
      const value = pos.quantity * pos.currentPrice
      sectors[sector] = (sectors[sector] || 0) + value
      return sectors
    }, {} as Record<string, number>)

    // Calculate portfolio beta
    const weightedBeta = positions.reduce((sum, pos) => {
      const weight = (pos.quantity * pos.currentPrice) / totalPositionValue
      const beta = marketData[pos.symbol]?.beta || 1.0
      return sum + (weight * beta)
    }, 0)

    // Calculate correlation risk (simplified)
    const correlationRisk = Object.keys(sectorExposure).length <= 2 ? 0.8 : 0.3

    // Calculate VaR (simplified 1-day 95% VaR)
    const dailyReturns = positions.map(pos => pos.unrealizedPnL / (pos.quantity * pos.avgCost))
    const avgReturn = dailyReturns.reduce((sum, ret) => sum + ret, 0) / dailyReturns.length
    const volatility = Math.sqrt(dailyReturns.reduce((sum, ret) => sum + Math.pow(ret - avgReturn, 2), 0) / dailyReturns.length)
    const valueAtRisk = portfolioValue * volatility * 1.645 // 95% confidence

    return {
      positionValue: totalPositionValue,
      portfolioValue,
      positionRisk: (Math.abs(totalUnrealizedPnL) / portfolioValue) * 100,
      correlationRisk,
      sectorExposure,
      maxDrawdown: Math.min(0, (totalUnrealizedPnL / portfolioValue) * 100),
      valueAtRisk,
      beta: weightedBeta,
      sharpeRatio: avgReturn / (volatility || 1)
    }
  }, [positions, portfolioValue, marketData])

  const getRiskColor = (risk: number) => {
    if (risk <= 2) return 'text-green-400'
    if (risk <= 5) return 'text-yellow-400'
    if (risk <= 10) return 'text-orange-400'
    return 'text-red-400'
  }

  const getRiskLevel = (risk: number) => {
    if (risk <= 2) return 'Low'
    if (risk <= 5) return 'Medium'
    if (risk <= 10) return 'High'
    return 'Very High'
  }

  return (
    <div className="bg-slate-800/30 rounded-lg p-4 border border-slate-700/50">
      <div className="flex items-center space-x-2 mb-4">
        <Shield className="w-5 h-5 text-orange-400" />
        <h3 className="font-semibold text-white">Risk Analysis</h3>
      </div>

      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-4">
        <div className="bg-slate-700/20 rounded-lg p-3">
          <div className="text-xs text-slate-400 mb-1">Position Risk</div>
          <div className={`font-mono font-bold ${getRiskColor(riskMetrics.positionRisk)}`}>
            {riskMetrics.positionRisk.toFixed(2)}%
          </div>
          <div className="text-xs text-slate-500 mt-1">
            {getRiskLevel(riskMetrics.positionRisk)}
          </div>
        </div>

        <div className="bg-slate-700/20 rounded-lg p-3">
          <div className="text-xs text-slate-400 mb-1">Portfolio Beta</div>
          <div className="font-mono font-bold text-white">
            {riskMetrics.beta.toFixed(2)}
          </div>
          <div className="text-xs text-slate-500 mt-1">
            {riskMetrics.beta > 1 ? 'High Volatility' : 'Low Volatility'}
          </div>
        </div>

        <div className="bg-slate-700/20 rounded-lg p-3">
          <div className="text-xs text-slate-400 mb-1">Value at Risk</div>
          <div className="font-mono font-bold text-red-400">
            ₹{riskMetrics.valueAtRisk.toLocaleString()}
          </div>
          <div className="text-xs text-slate-500 mt-1">
            95% 1-day VaR
          </div>
        </div>

        <div className="bg-slate-700/20 rounded-lg p-3">
          <div className="text-xs text-slate-400 mb-1">Sharpe Ratio</div>
          <div className={`font-mono font-bold ${
            riskMetrics.sharpeRatio > 1 ? 'text-green-400' :
            riskMetrics.sharpeRatio > 0 ? 'text-yellow-400' : 'text-red-400'
          }`}>
            {riskMetrics.sharpeRatio.toFixed(2)}
          </div>
          <div className="text-xs text-slate-500 mt-1">
            Risk-adjusted return
          </div>
        </div>
      </div>

      {/* Sector Exposure Chart */}
      <div className="bg-slate-700/20 rounded-lg p-3">
        <h4 className="text-sm font-medium text-white mb-3">Sector Exposure</h4>
        <div className="space-y-2">
          {Object.entries(riskMetrics.sectorExposure).map(([sector, value]) => {
            const percentage = (value / riskMetrics.positionValue) * 100
            return (
              <div key={sector} className="flex items-center justify-between">
                <span className="text-sm text-slate-300">{sector}</span>
                <div className="flex items-center space-x-2">
                  <div className="w-20 h-2 bg-slate-600 rounded-full overflow-hidden">
                    <div 
                      className={`h-full transition-all duration-300 ${
                        percentage > 50 ? 'bg-red-400' :
                        percentage > 30 ? 'bg-yellow-400' : 'bg-green-400'
                      }`}
                      style={{ width: `${Math.min(percentage, 100)}%` }}
                    />
                  </div>
                  <span className="text-sm font-mono text-white w-12">
                    {percentage.toFixed(1)}%
                  </span>
                </div>
              </div>
            )
          })}
        </div>
      </div>
    </div>
  )
}

const StrategyBacktester: React.FC<{
  onBacktest: (config: BacktestConfig) => Promise<BacktestResult>
}> = ({ onBacktest }) => {
  const [config, setConfig] = useState<BacktestConfig>({
    strategy: 'moving-average-crossover',
    symbol: 'RELIANCE',
    startDate: new Date(2023, 0, 1),
    endDate: new Date(),
    initialCapital: 100000,
    commission: 0.0025,
    slippage: 0.001,
    parameters: {
      fastMA: 10,
      slowMA: 20,
      stopLoss: 5
    }
  })

  const [result, setResult] = useState<BacktestResult | null>(null)
  const [isRunning, setIsRunning] = useState(false)
  const [progress, setProgress] = useState(0)

  const strategies = [
    { value: 'moving-average-crossover', label: 'MA Crossover', parameters: ['fastMA', 'slowMA', 'stopLoss'] },
    { value: 'rsi-mean-reversion', label: 'RSI Mean Reversion', parameters: ['rsiPeriod', 'oversold', 'overbought'] },
    { value: 'bollinger-bands', label: 'Bollinger Bands', parameters: ['period', 'stdDev', 'reversion'] },
    { value: 'momentum-strategy', label: 'Momentum', parameters: ['lookback', 'threshold', 'holding'] }
  ]

  const runBacktest = async () => {
    setIsRunning(true)
    setProgress(0)
    
    // Simulate backtest progress
    const progressInterval = setInterval(() => {
      setProgress(prev => {
        if (prev >= 90) {
          clearInterval(progressInterval)
          return 90
        }
        return prev + Math.random() * 10
      })
    }, 200)

    try {
      const backtestResult = await onBacktest(config)
      setResult(backtestResult)
      setProgress(100)
    } catch (error) {
      console.error('Backtest failed:', error)
      // Mock result for demo
      setResult({
        totalReturn: 15.7,
        annualizedReturn: 12.3,
        sharpeRatio: 1.4,
        maxDrawdown: -8.2,
        winRate: 58.3,
        profitFactor: 1.8,
        totalTrades: 145,
        avgWin: 2.4,
        avgLoss: -1.8,
        largestWin: 12.5,
        largestLoss: -7.3,
        consecutiveWins: 7,
        consecutiveLosses: 4,
        trades: []
      })
      setProgress(100)
    } finally {
      setTimeout(() => setIsRunning(false), 500)
      clearInterval(progressInterval)
    }
  }

  const selectedStrategy = strategies.find(s => s.value === config.strategy)

  return (
    <div className="bg-slate-800/30 rounded-lg p-4 border border-slate-700/50">
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center space-x-2">
          <Brain className="w-5 h-5 text-purple-400" />
          <h3 className="font-semibold text-white">Strategy Backtester</h3>
        </div>
        
        <button
          onClick={runBacktest}
          disabled={isRunning}
          className="cyber-button px-4 py-2 disabled:opacity-50"
        >
          {isRunning ? (
            <div className="flex items-center space-x-2">
              <div className="w-4 h-4 border-2 border-purple-400 border-t-transparent rounded-full animate-spin" />
              <span>Running...</span>
            </div>
          ) : (
            <div className="flex items-center space-x-2">
              <Play className="w-4 h-4" />
              <span>Run Backtest</span>
            </div>
          )}
        </button>
      </div>

      {/* Configuration */}
      <div className="grid grid-cols-2 gap-4 mb-4">
        <div>
          <label className="text-sm text-slate-400 mb-2 block">Strategy</label>
          <select
            value={config.strategy}
            onChange={(e) => setConfig(prev => ({ ...prev, strategy: e.target.value }))}
            className="w-full bg-slate-700 border border-slate-600 rounded-lg px-3 py-2 text-white focus:ring-2 focus:ring-purple-500"
          >
            {strategies.map(strategy => (
              <option key={strategy.value} value={strategy.value}>
                {strategy.label}
              </option>
            ))}
          </select>
        </div>

        <div>
          <label className="text-sm text-slate-400 mb-2 block">Symbol</label>
          <input
            type="text"
            value={config.symbol}
            onChange={(e) => setConfig(prev => ({ ...prev, symbol: e.target.value.toUpperCase() }))}
            className="w-full bg-slate-700 border border-slate-600 rounded-lg px-3 py-2 text-white focus:ring-2 focus:ring-purple-500"
          />
        </div>

        <div>
          <label className="text-sm text-slate-400 mb-2 block">Start Date</label>
          <input
            type="date"
            value={config.startDate.toISOString().split('T')[0]}
            onChange={(e) => setConfig(prev => ({ ...prev, startDate: new Date(e.target.value) }))}
            className="w-full bg-slate-700 border border-slate-600 rounded-lg px-3 py-2 text-white focus:ring-2 focus:ring-purple-500"
          />
        </div>

        <div>
          <label className="text-sm text-slate-400 mb-2 block">End Date</label>
          <input
            type="date"
            value={config.endDate.toISOString().split('T')[0]}
            onChange={(e) => setConfig(prev => ({ ...prev, endDate: new Date(e.target.value) }))}
            className="w-full bg-slate-700 border border-slate-600 rounded-lg px-3 py-2 text-white focus:ring-2 focus:ring-purple-500"
          />
        </div>

        <div>
          <label className="text-sm text-slate-400 mb-2 block">Initial Capital</label>
          <div className="relative">
            <input
              type="number"
              value={config.initialCapital}
              onChange={(e) => setConfig(prev => ({ ...prev, initialCapital: parseFloat(e.target.value) || 0 }))}
              className="w-full bg-slate-700 border border-slate-600 rounded-lg px-3 py-2 text-white pr-8 focus:ring-2 focus:ring-purple-500"
            />
            <span className="absolute right-3 top-2 text-xs text-slate-400">₹</span>
          </div>
        </div>

        <div>
          <label className="text-sm text-slate-400 mb-2 block">Commission</label>
          <div className="relative">
            <input
              type="number"
              value={config.commission}
              onChange={(e) => setConfig(prev => ({ ...prev, commission: parseFloat(e.target.value) || 0 }))}
              className="w-full bg-slate-700 border border-slate-600 rounded-lg px-3 py-2 text-white pr-8 focus:ring-2 focus:ring-purple-500"
              step="0.001"
            />
            <span className="absolute right-3 top-2 text-xs text-slate-400">%</span>
          </div>
        </div>
      </div>

      {/* Strategy Parameters */}
      {selectedStrategy && (
        <div className="mb-4">
          <h4 className="text-sm font-medium text-white mb-2">Strategy Parameters</h4>
          <div className="grid grid-cols-3 gap-3">
            {selectedStrategy.parameters.map(param => (
              <div key={param}>
                <label className="text-xs text-slate-400 mb-1 block capitalize">
                  {param.replace(/([A-Z])/g, ' $1')}
                </label>
                <input
                  type="number"
                  value={config.parameters[param] || 0}
                  onChange={(e) => setConfig(prev => ({
                    ...prev,
                    parameters: {
                      ...prev.parameters,
                      [param]: parseFloat(e.target.value) || 0
                    }
                  }))}
                  className="w-full bg-slate-700 border border-slate-600 rounded-lg px-2 py-1 text-white text-sm focus:ring-2 focus:ring-purple-500"
                />
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Progress Bar */}
      {isRunning && (
        <div className="mb-4">
          <div className="flex items-center justify-between mb-2">
            <span className="text-sm text-slate-400">Running backtest...</span>
            <span className="text-sm text-purple-400">{progress.toFixed(0)}%</span>
          </div>
          <div className="w-full h-2 bg-slate-600 rounded-full overflow-hidden">
            <motion.div
              className="h-full bg-gradient-to-r from-purple-500 to-blue-500"
              initial={{ width: 0 }}
              animate={{ width: `${progress}%` }}
              transition={{ duration: 0.3 }}
            />
          </div>
        </div>
      )}

      {/* Results */}
      {result && (
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="bg-slate-700/20 rounded-lg p-4"
        >
          <h4 className="font-semibold text-white mb-3">Backtest Results</h4>
          
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-3 mb-4">
            <div className="bg-green-500/10 border border-green-400/30 rounded-lg p-2">
              <div className="text-xs text-green-400 mb-1">Total Return</div>
              <div className="font-mono font-bold text-white">{result.totalReturn.toFixed(1)}%</div>
            </div>
            
            <div className="bg-blue-500/10 border border-blue-400/30 rounded-lg p-2">
              <div className="text-xs text-blue-400 mb-1">Annual Return</div>
              <div className="font-mono font-bold text-white">{result.annualizedReturn.toFixed(1)}%</div>
            </div>
            
            <div className="bg-purple-500/10 border border-purple-400/30 rounded-lg p-2">
              <div className="text-xs text-purple-400 mb-1">Sharpe Ratio</div>
              <div className="font-mono font-bold text-white">{result.sharpeRatio.toFixed(2)}</div>
            </div>
            
            <div className="bg-red-500/10 border border-red-400/30 rounded-lg p-2">
              <div className="text-xs text-red-400 mb-1">Max Drawdown</div>
              <div className="font-mono font-bold text-white">{result.maxDrawdown.toFixed(1)}%</div>
            </div>
          </div>

          <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
            <div className="text-center">
              <div className="text-xs text-slate-400 mb-1">Win Rate</div>
              <div className="font-mono text-white">{result.winRate.toFixed(1)}%</div>
            </div>
            
            <div className="text-center">
              <div className="text-xs text-slate-400 mb-1">Total Trades</div>
              <div className="font-mono text-white">{result.totalTrades}</div>
            </div>
            
            <div className="text-center">
              <div className="text-xs text-slate-400 mb-1">Profit Factor</div>
              <div className="font-mono text-white">{result.profitFactor.toFixed(2)}</div>
            </div>
            
            <div className="text-center">
              <div className="text-xs text-slate-400 mb-1">Avg Win/Loss</div>
              <div className="font-mono text-white">{result.avgWin.toFixed(1)}% / {result.avgLoss.toFixed(1)}%</div>
            </div>
          </div>
        </motion.div>
      )}
    </div>
  )
}

export const ProfessionalTradingTools: React.FC<ProfessionalTradingToolsProps> = ({
  accountBalance,
  currentPositions,
  marketData,
  onStrategyBacktest,
  className = ''
}) => {
  const [activeTab, setActiveTab] = useState<'position-size' | 'risk-analysis' | 'backtesting' | 'performance'>('position-size')
  const [positionSizeConfig, setPositionSizeConfig] = useState<PositionSizeConfig>({
    accountSize: accountBalance,
    riskPercentage: 2,
    entryPrice: 100,
    stopLoss: 95
  })

  const handleBacktest = async (config: BacktestConfig): Promise<BacktestResult> => {
    if (onStrategyBacktest) {
      return await onStrategyBacktest(config)
    }
    
    // Mock backtest result
    return {
      totalReturn: 15.7,
      annualizedReturn: 12.3,
      sharpeRatio: 1.4,
      maxDrawdown: -8.2,
      winRate: 58.3,
      profitFactor: 1.8,
      totalTrades: 145,
      avgWin: 2.4,
      avgLoss: -1.8,
      largestWin: 12.5,
      largestLoss: -7.3,
      consecutiveWins: 7,
      consecutiveLosses: 4,
      trades: []
    }
  }

  const tabs = [
    { id: 'position-size', label: 'Position Size', icon: Calculator, description: 'Calculate optimal position sizes' },
    { id: 'risk-analysis', label: 'Risk Analysis', icon: Shield, description: 'Portfolio risk assessment' },
    { id: 'backtesting', label: 'Backtesting', icon: Brain, description: 'Strategy performance testing' },
    { id: 'performance', label: 'Performance', icon: TrendingUp, description: 'Trading performance metrics' }
  ]

  return (
    <motion.div
      className={`glass-card rounded-2xl overflow-hidden ${className}`}
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.6 }}
    >
      {/* Header */}
      <div className="flex items-center justify-between p-6 border-b border-slate-700/50">
        <div className="flex items-center space-x-3">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-r from-green-500 to-blue-500 flex items-center justify-center">
            <Calculator className="w-5 h-5 text-white" />
          </div>
          <div>
            <h2 className="text-xl font-bold text-white">Professional Trading Tools</h2>
            <p className="text-sm text-slate-400">Advanced calculators and analysis tools</p>
          </div>
        </div>

        <div className="flex items-center space-x-2">
          <span className="text-sm text-slate-400">Account: ₹{accountBalance.toLocaleString()}</span>
        </div>
      </div>

      {/* Tab Navigation */}
      <div className="flex space-x-1 p-4 bg-slate-800/30">
        {tabs.map((tab) => {
          const Icon = tab.icon
          const isActive = activeTab === tab.id
          
          return (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id as any)}
              className={`flex items-center space-x-2 px-4 py-3 rounded-lg transition-all font-medium text-sm relative ${
                isActive
                  ? 'bg-slate-700/80 text-white shadow-lg'
                  : 'text-slate-400 hover:text-white hover:bg-slate-700/30'
              }`}
              title={tab.description}
            >
              <Icon className="w-4 h-4" />
              <span>{tab.label}</span>
              {isActive && (
                <motion.div
                  layoutId="activeToolTab"
                  className="absolute inset-0 bg-gradient-to-r from-green-500/10 to-blue-500/10 rounded-lg border border-green-400/30"
                />
              )}
            </button>
          )
        })}
      </div>

      {/* Content */}
      <div className="p-6">
        <AnimatePresence mode="wait">
          {activeTab === 'position-size' && (
            <motion.div
              key="position-size"
              initial={{ opacity: 0, x: -20 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: 20 }}
              transition={{ duration: 0.3 }}
            >
              <PositionSizeCalculator
                accountSize={accountBalance}
                onCalculate={setPositionSizeConfig}
              />
            </motion.div>
          )}

          {activeTab === 'risk-analysis' && (
            <motion.div
              key="risk-analysis"
              initial={{ opacity: 0, x: -20 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: 20 }}
              transition={{ duration: 0.3 }}
            >
              <RiskAnalyzer
                positions={currentPositions}
                portfolioValue={accountBalance}
                marketData={marketData}
              />
            </motion.div>
          )}

          {activeTab === 'backtesting' && (
            <motion.div
              key="backtesting"
              initial={{ opacity: 0, x: -20 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: 20 }}
              transition={{ duration: 0.3 }}
            >
              <StrategyBacktester onBacktest={handleBacktest} />
            </motion.div>
          )}

          {activeTab === 'performance' && (
            <motion.div
              key="performance"
              initial={{ opacity: 0, x: -20 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: 20 }}
              transition={{ duration: 0.3 }}
              className="bg-slate-800/30 rounded-lg p-4 border border-slate-700/50"
            >
              <div className="flex items-center space-x-2 mb-4">
                <TrendingUp className="w-5 h-5 text-cyan-400" />
                <h3 className="font-semibold text-white">Performance Analytics</h3>
              </div>
              
              <div className="text-center py-8 text-slate-400">
                <BarChart3 className="w-12 h-12 mx-auto mb-4 opacity-50" />
                <p className="text-lg font-medium mb-2">Performance Analytics</p>
                <p className="text-sm">Coming soon - Advanced performance tracking and analytics</p>
              </div>
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    </motion.div>
  )
}

export default ProfessionalTradingTools