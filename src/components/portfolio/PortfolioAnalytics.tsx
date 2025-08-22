import React, { useState } from 'react'
import { 
  PieChart, 
  TrendingUp, 
  TrendingDown,
  DollarSign,
  Target,
  Activity,
  BarChart3,
  Calendar,
  Download,
  Filter,
  RefreshCw,
  AlertTriangle,
  Shield,
  Zap
} from 'lucide-react'
import { RiskMeter } from './RiskMeter'
import { PriceAlerts } from '../market/PriceAlerts'
import { TaxReporting } from './TaxReporting'

interface HoldingData {
  symbol: string
  name: string
  quantity: number
  avgPrice: number
  currentPrice: number
  totalValue: number
  dayChange: number
  dayChangePercent: number
  totalGainLoss: number
  totalGainLossPercent: number
  allocation: number
  sector: string
}

interface PerformanceMetric {
  period: string
  returns: number
  benchmark: number
  alpha: number
  volatility: number
}

const mockHoldings: HoldingData[] = [
  {
    symbol: 'RELIANCE',
    name: 'Reliance Industries Limited',
    quantity: 10,
    avgPrice: 2520.50,
    currentPrice: 2547.30,
    totalValue: 25473,
    dayChange: 234,
    dayChangePercent: 0.93,
    totalGainLoss: 268,
    totalGainLossPercent: 1.06,
    allocation: 35.2,
    sector: 'Energy'
  },
  {
    symbol: 'TCS',
    name: 'Tata Consultancy Services',
    quantity: 5,
    avgPrice: 3680.20,
    currentPrice: 3642.80,
    totalValue: 18214,
    dayChange: -95,
    dayChangePercent: -0.52,
    totalGainLoss: -187,
    totalGainLossPercent: -1.02,
    allocation: 25.1,
    sector: 'IT'
  },
  {
    symbol: 'HDFCBANK',
    name: 'HDFC Bank Limited',
    quantity: 15,
    avgPrice: 1545.30,
    currentPrice: 1567.25,
    totalValue: 23509,
    dayChange: 192,
    dayChangePercent: 0.82,
    totalGainLoss: 329,
    totalGainLossPercent: 1.42,
    allocation: 32.4,
    sector: 'Banking'
  },
  {
    symbol: 'INFY',
    name: 'Infosys Limited',
    quantity: 8,
    avgPrice: 1420.35,
    currentPrice: 1423.60,
    totalValue: 11389,
    dayChange: 66,
    dayChangePercent: 0.58,
    totalGainLoss: 26,
    totalGainLossPercent: 0.23,
    allocation: 7.3,
    sector: 'IT'
  }
]

const performanceData: PerformanceMetric[] = [
  { period: '1D', returns: 1.2, benchmark: 0.8, alpha: 0.4, volatility: 2.1 },
  { period: '1W', returns: 3.8, benchmark: 2.9, alpha: 0.9, volatility: 1.8 },
  { period: '1M', returns: 7.2, benchmark: 5.1, alpha: 2.1, volatility: 2.3 },
  { period: '3M', returns: 15.6, benchmark: 12.3, alpha: 3.3, volatility: 2.7 },
  { period: '1Y', returns: 24.8, benchmark: 18.9, alpha: 5.9, volatility: 3.2 }
]

export function PortfolioAnalytics() {
  const [selectedPeriod, setSelectedPeriod] = useState('1M')
  const [viewMode, setViewMode] = useState<'overview' | 'holdings' | 'performance' | 'risk' | 'alerts' | 'taxes'>('overview')

  const totalValue = mockHoldings.reduce((sum, holding) => sum + holding.totalValue, 0)
  const totalDayChange = mockHoldings.reduce((sum, holding) => sum + holding.dayChange, 0)
  const totalDayChangePercent = (totalDayChange / (totalValue - totalDayChange)) * 100
  const totalGainLoss = mockHoldings.reduce((sum, holding) => sum + holding.totalGainLoss, 0)
  const totalGainLossPercent = (totalGainLoss / (totalValue - totalGainLoss)) * 100

  const sectorAllocation = mockHoldings.reduce((acc, holding) => {
    const sector = holding.sector
    acc[sector] = (acc[sector] || 0) + holding.allocation
    return acc
  }, {} as Record<string, number>)

  const getSectorColor = (sector: string) => {
    const colors = {
      'Energy': '#F59E0B',
      'IT': '#8B5CF6',
      'Banking': '#06B6D4',
      'Healthcare': '#EF4444',
      'Consumer': '#10B981'
    }
    return colors[sector as keyof typeof colors] || '#6B7280'
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold gradient-text mb-2">Portfolio Analytics</h1>
          <p className="text-slate-400">Comprehensive portfolio analysis and insights</p>
        </div>
        <div className="flex items-center space-x-4">
          <button className="flex items-center space-x-2 glass-card px-4 py-2 rounded-xl text-slate-400 hover:text-white transition-colors">
            <Download className="w-4 h-4" />
            <span>Export</span>
          </button>
          <button className="flex items-center space-x-2 cyber-button px-4 py-2 rounded-xl">
            <RefreshCw className="w-4 h-4" />
            <span>Refresh</span>
          </button>
        </div>
      </div>

      {/* Portfolio Summary Cards */}
      <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-4">
        <div className="glass-card p-6 rounded-2xl hover:scale-105 transition-all duration-300">
          <div className="flex items-center justify-between mb-4">
            <div className="p-3 rounded-xl bg-gradient-to-br from-purple-500/20 to-purple-600/20">
              <DollarSign className="h-6 w-6 text-purple-400" />
            </div>
            <div className="text-right">
              <div className="text-2xl font-bold text-white">₹{totalValue.toLocaleString()}</div>
              <div className={`text-sm flex items-center justify-end ${
                totalDayChange >= 0 ? 'text-green-400' : 'text-red-400'
              }`}>
                {totalDayChange >= 0 ? <TrendingUp className="h-3 w-3 mr-1" /> : <TrendingDown className="h-3 w-3 mr-1" />}
                {totalDayChangePercent >= 0 ? '+' : ''}{totalDayChangePercent.toFixed(2)}%
              </div>
            </div>
          </div>
          <h3 className="text-purple-400 font-semibold mb-1">Total Value</h3>
          <p className="text-slate-400 text-sm">Today's change: ₹{totalDayChange.toLocaleString()}</p>
        </div>

        <div className="glass-card p-6 rounded-2xl hover:scale-105 transition-all duration-300">
          <div className="flex items-center justify-between mb-4">
            <div className="p-3 rounded-xl bg-gradient-to-br from-green-500/20 to-green-600/20">
              <TrendingUp className="h-6 w-6 text-green-400" />
            </div>
            <div className="text-right">
              <div className={`text-2xl font-bold ${
                totalGainLoss >= 0 ? 'text-green-400' : 'text-red-400'
              }`}>
                {totalGainLoss >= 0 ? '+' : ''}₹{totalGainLoss.toLocaleString()}
              </div>
              <div className={`text-sm ${
                totalGainLoss >= 0 ? 'text-green-400' : 'text-red-400'
              }`}>
                {totalGainLossPercent >= 0 ? '+' : ''}{totalGainLossPercent.toFixed(2)}%
              </div>
            </div>
          </div>
          <h3 className="text-green-400 font-semibold mb-1">Total Returns</h3>
          <p className="text-slate-400 text-sm">unrealized gains</p>
        </div>

        <div className="glass-card p-6 rounded-2xl hover:scale-105 transition-all duration-300">
          <div className="flex items-center justify-between mb-4">
            <div className="p-3 rounded-xl bg-gradient-to-br from-cyan-500/20 to-cyan-600/20">
              <Target className="h-6 w-6 text-cyan-400" />
            </div>
            <div className="text-right">
              <div className="text-2xl font-bold text-white">{mockHoldings.length}</div>
              <div className="text-sm text-slate-400">
                across {Object.keys(sectorAllocation).length} sectors
              </div>
            </div>
          </div>
          <h3 className="text-cyan-400 font-semibold mb-1">Holdings</h3>
          <p className="text-slate-400 text-sm">diversified portfolio</p>
        </div>

        <div className="glass-card p-6 rounded-2xl hover:scale-105 transition-all duration-300">
          <div className="flex items-center justify-between mb-4">
            <div className="p-3 rounded-xl bg-gradient-to-br from-orange-500/20 to-orange-600/20">
              <Activity className="h-6 w-6 text-orange-400" />
            </div>
            <div className="text-right">
              <div className="text-2xl font-bold text-white">24.8%</div>
              <div className="text-sm text-green-400">
                vs 18.9% benchmark
              </div>
            </div>
          </div>
          <h3 className="text-orange-400 font-semibold mb-1">Annual Return</h3>
          <p className="text-slate-400 text-sm">outperforming market</p>
        </div>
      </div>

      {/* View Mode Tabs */}
      <div className="flex items-center space-x-2 mb-6">
        {[
          { key: 'overview', label: 'Overview', icon: PieChart },
          { key: 'holdings', label: 'Holdings', icon: BarChart3 },
          { key: 'performance', label: 'Performance', icon: TrendingUp },
          { key: 'risk', label: 'Risk Analysis', icon: Shield },
          { key: 'alerts', label: 'Price Alerts', icon: AlertTriangle },
          { key: 'taxes', label: 'Tax Reporting', icon: Calendar }
        ].map(({ key, label, icon: Icon }) => (
          <button
            key={key}
            onClick={() => setViewMode(key as any)}
            className={`flex items-center space-x-2 px-4 py-2 rounded-xl transition-all ${
              viewMode === key
                ? 'bg-purple-500/20 text-purple-400'
                : 'text-slate-400 hover:text-white'
            }`}
          >
            <Icon className="w-4 h-4" />
            <span>{label}</span>
          </button>
        ))}
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        {/* Main Content */}
        <div className="lg:col-span-2">
          {viewMode === 'overview' && (
            <div className="space-y-6">
              {/* Asset Allocation Chart */}
              <div className="glass-card rounded-2xl p-6">
                <h3 className="text-xl font-bold text-white mb-6">Asset Allocation</h3>
                <div className="grid gap-6 md:grid-cols-2">
                  <div className="space-y-4">
                    {mockHoldings.map((holding) => (
                      <div key={holding.symbol} className="flex items-center justify-between p-3 rounded-xl bg-slate-800/30">
                        <div className="flex items-center space-x-3">
                          <div 
                            className="w-4 h-4 rounded-full"
                            style={{ backgroundColor: getSectorColor(holding.sector) }}
                          />
                          <div>
                            <div className="font-semibold text-white">{holding.symbol}</div>
                            <div className="text-sm text-slate-400">{holding.sector}</div>
                          </div>
                        </div>
                        <div className="text-right">
                          <div className="text-white font-semibold">{holding.allocation.toFixed(1)}%</div>
                          <div className="text-sm text-slate-400">₹{holding.totalValue.toLocaleString()}</div>
                        </div>
                      </div>
                    ))}
                  </div>
                  <div className="flex items-center justify-center">
                    <div className="w-48 h-48 rounded-full border-8 border-purple-500/20 flex items-center justify-center">
                      <div className="text-center">
                        <div className="text-2xl font-bold text-white">₹{(totalValue / 1000).toFixed(0)}K</div>
                        <div className="text-sm text-slate-400">Total Value</div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              {/* Recent Performance */}
              <div className="glass-card rounded-2xl p-6">
                <h3 className="text-xl font-bold text-white mb-6">Performance Overview</h3>
                <div className="grid gap-4 md:grid-cols-5">
                  {performanceData.map((metric) => (
                    <div key={metric.period} className="text-center p-4 rounded-xl bg-slate-800/30">
                      <div className="text-lg font-bold text-white">{metric.period}</div>
                      <div className={`text-xl font-bold mt-2 ${
                        metric.returns >= 0 ? 'text-green-400' : 'text-red-400'
                      }`}>
                        {metric.returns >= 0 ? '+' : ''}{metric.returns}%
                      </div>
                      <div className="text-sm text-slate-400 mt-1">vs {metric.benchmark}%</div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          )}

          {viewMode === 'holdings' && (
            <div className="glass-card rounded-2xl p-6">
              <h3 className="text-xl font-bold text-white mb-6">Holdings Details</h3>
              <div className="space-y-4">
                {mockHoldings.map((holding) => (
                  <div key={holding.symbol} className="p-4 rounded-xl bg-slate-800/30 hover:bg-slate-700/30 transition-colors">
                    <div className="grid gap-4 md:grid-cols-6">
                      <div>
                        <div className="font-semibold text-white">{holding.symbol}</div>
                        <div className="text-sm text-slate-400">{holding.sector}</div>
                      </div>
                      <div className="text-right">
                        <div className="text-white">{holding.quantity}</div>
                        <div className="text-sm text-slate-400">Qty</div>
                      </div>
                      <div className="text-right">
                        <div className="text-white">₹{holding.avgPrice}</div>
                        <div className="text-sm text-slate-400">Avg Price</div>
                      </div>
                      <div className="text-right">
                        <div className="text-white">₹{holding.currentPrice}</div>
                        <div className="text-sm text-slate-400">LTP</div>
                      </div>
                      <div className="text-right">
                        <div className="text-white font-semibold">₹{holding.totalValue.toLocaleString()}</div>
                        <div className="text-sm text-slate-400">Value</div>
                      </div>
                      <div className="text-right">
                        <div className={`font-semibold ${
                          holding.totalGainLoss >= 0 ? 'text-green-400' : 'text-red-400'
                        }`}>
                          {holding.totalGainLoss >= 0 ? '+' : ''}₹{holding.totalGainLoss}
                        </div>
                        <div className={`text-sm ${
                          holding.totalGainLossPercent >= 0 ? 'text-green-400' : 'text-red-400'
                        }`}>
                          {holding.totalGainLossPercent >= 0 ? '+' : ''}{holding.totalGainLossPercent.toFixed(2)}%
                        </div>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {viewMode === 'performance' && (
            <div className="glass-card rounded-2xl p-6">
              <h3 className="text-xl font-bold text-white mb-6">Performance Analysis</h3>
              <div className="bg-slate-800/50 rounded-2xl p-8 h-80 flex items-center justify-center">
                <div className="text-center">
                  <BarChart3 className="w-16 h-16 text-purple-400 mx-auto mb-4" />
                  <h3 className="text-lg font-semibold text-white mb-2">Performance Chart</h3>
                  <p className="text-slate-400">Interactive performance visualization</p>
                </div>
              </div>
            </div>
          )}

          {viewMode === 'risk' && (
            <div className="space-y-6">
              <div className="glass-card rounded-2xl p-6">
                <h3 className="text-xl font-bold text-white mb-6">Risk Analysis</h3>
                <div className="space-y-6">
                  <div className="grid gap-4 md:grid-cols-2">
                    <div className="p-4 rounded-xl bg-slate-800/30">
                      <div className="flex items-center space-x-2 mb-2">
                        <Shield className="w-5 h-5 text-green-400" />
                        <span className="font-semibold text-white">Portfolio Beta</span>
                      </div>
                      <div className="text-2xl font-bold text-green-400">0.85</div>
                      <div className="text-sm text-slate-400">Lower than market risk</div>
                    </div>
                    <div className="p-4 rounded-xl bg-slate-800/30">
                      <div className="flex items-center space-x-2 mb-2">
                        <AlertTriangle className="w-5 h-5 text-yellow-400" />
                        <span className="font-semibold text-white">Volatility</span>
                      </div>
                      <div className="text-2xl font-bold text-yellow-400">12.5%</div>
                      <div className="text-sm text-slate-400">30-day volatility</div>
                    </div>
                  </div>
                  
                  <div className="p-4 rounded-xl bg-slate-800/30">
                    <h4 className="font-semibold text-white mb-4">Risk Metrics</h4>
                    <div className="space-y-3">
                      <div className="flex items-center justify-between">
                        <span className="text-slate-400">Sharpe Ratio</span>
                        <span className="text-white font-semibold">1.85</span>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-slate-400">Max Drawdown</span>
                        <span className="text-red-400 font-semibold">-8.2%</span>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-slate-400">VaR (95%)</span>
                        <span className="text-orange-400 font-semibold">-2.1%</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
              
              {/* Risk Management Tools */}
              <RiskMeter />
            </div>
          )}

          {viewMode === 'alerts' && (
            <div>
              <PriceAlerts />
            </div>
          )}

          {viewMode === 'taxes' && (
            <div>
              <TaxReporting />
            </div>
          )}
        </div>

        {/* Sidebar - Sector Allocation */}
        <div className="space-y-6">
          <div className="glass-card rounded-2xl p-6">
            <h3 className="text-lg font-bold text-white mb-6">Sector Allocation</h3>
            <div className="space-y-4">
              {Object.entries(sectorAllocation).map(([sector, allocation]) => (
                <div key={sector} className="space-y-2">
                  <div className="flex items-center justify-between">
                    <span className="text-white font-medium">{sector}</span>
                    <span className="text-slate-400">{allocation.toFixed(1)}%</span>
                  </div>
                  <div className="w-full bg-slate-700 rounded-full h-2">
                    <div 
                      className="h-2 rounded-full transition-all duration-300"
                      style={{ 
                        width: `${allocation}%`,
                        backgroundColor: getSectorColor(sector)
                      }}
                    />
                  </div>
                </div>
              ))}
            </div>
          </div>

          <div className="glass-card rounded-2xl p-6">
            <h3 className="text-lg font-bold text-white mb-6">Quick Actions</h3>
            <div className="space-y-3">
              <button className="cyber-button w-full py-3 rounded-xl text-sm">
                Rebalance Portfolio
              </button>
              <button className="glass-card border border-purple-500/50 hover:border-purple-400/70 text-purple-400 w-full py-3 rounded-xl text-sm transition-colors">
                Generate Report
              </button>
              <button className="glass-card border border-cyan-500/50 hover:border-cyan-400/70 text-cyan-400 w-full py-3 rounded-xl text-sm transition-colors">
                Tax Analysis
              </button>
            </div>
          </div>
        </div>
      </div>

    </div>
  )
}