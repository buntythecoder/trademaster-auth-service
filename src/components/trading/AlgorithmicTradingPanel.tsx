import React, { useState } from 'react'
import { 
  Brain,
  Zap,
  Target,
  TrendingUp,
  TrendingDown,
  Play,
  Pause,
  Square,
  Settings,
  BarChart3,
  Activity,
  Clock,
  Shield,
  AlertTriangle,
  CheckCircle,
  Calendar
} from 'lucide-react'

interface AlgorithmicStrategy {
  id: string
  name: string
  description: string
  type: 'momentum' | 'mean_reversion' | 'arbitrage' | 'pairs_trading' | 'grid'
  status: 'running' | 'paused' | 'stopped'
  pnl: number
  pnlPercent: number
  totalTrades: number
  winRate: number
  maxDrawdown: number
  sharpe: number
  symbols: string[]
  parameters: Record<string, any>
}

interface BacktestResult {
  strategy: string
  period: string
  totalReturn: number
  maxDrawdown: number
  sharpeRatio: number
  winRate: number
  totalTrades: number
  profitFactor: number
  avgTrade: number
}

const mockStrategies: AlgorithmicStrategy[] = [
  {
    id: '1',
    name: 'Momentum Breakout',
    description: 'Trades on price breakouts above resistance levels with volume confirmation',
    type: 'momentum',
    status: 'running',
    pnl: 12450,
    pnlPercent: 8.3,
    totalTrades: 24,
    winRate: 62.5,
    maxDrawdown: -2.1,
    sharpe: 1.85,
    symbols: ['RELIANCE', 'TCS', 'HDFC'],
    parameters: {
      lookback_period: 20,
      volume_threshold: 1.5,
      risk_per_trade: 1.0
    }
  },
  {
    id: '2',
    name: 'Mean Reversion RSI',
    description: 'Mean reversion strategy using RSI oversold/overbought levels',
    type: 'mean_reversion',
    status: 'paused',
    pnl: -850,
    pnlPercent: -1.2,
    totalTrades: 18,
    winRate: 55.6,
    maxDrawdown: -3.2,
    sharpe: 0.45,
    symbols: ['INFY', 'WIPRO', 'HCLTECH'],
    parameters: {
      rsi_period: 14,
      oversold_level: 30,
      overbought_level: 70,
      risk_per_trade: 0.5
    }
  },
  {
    id: '3',
    name: 'Pairs Trading',
    description: 'Statistical arbitrage between correlated stock pairs',
    type: 'pairs_trading',
    status: 'running',
    pnl: 3200,
    pnlPercent: 2.1,
    totalTrades: 12,
    winRate: 75.0,
    maxDrawdown: -1.5,
    sharpe: 2.12,
    symbols: ['TCS', 'INFY'],
    parameters: {
      zscore_entry: 2.0,
      zscore_exit: 0.0,
      lookback_period: 30,
      risk_per_trade: 1.5
    }
  }
]

const mockBacktestResults: BacktestResult[] = [
  {
    strategy: 'Momentum Breakout',
    period: '6M',
    totalReturn: 15.4,
    maxDrawdown: -4.2,
    sharpeRatio: 1.85,
    winRate: 62.5,
    totalTrades: 48,
    profitFactor: 1.8,
    avgTrade: 0.32
  },
  {
    strategy: 'Mean Reversion RSI',
    period: '6M',
    totalReturn: 8.9,
    maxDrawdown: -6.1,
    sharpeRatio: 1.22,
    winRate: 58.3,
    totalTrades: 72,
    profitFactor: 1.4,
    avgTrade: 0.12
  },
  {
    strategy: 'Pairs Trading',
    period: '6M',
    totalReturn: 12.1,
    maxDrawdown: -2.8,
    sharpeRatio: 2.12,
    winRate: 75.0,
    totalTrades: 24,
    profitFactor: 2.3,
    avgTrade: 0.51
  }
]

export function AlgorithmicTradingPanel() {
  const [activeTab, setActiveTab] = useState<'strategies' | 'backtest' | 'create'>('strategies')
  const [strategies, setStrategies] = useState(mockStrategies)
  const [selectedStrategy, setSelectedStrategy] = useState<AlgorithmicStrategy | null>(null)
  const [backtestResults] = useState(mockBacktestResults)

  const handleStrategyAction = (strategyId: string, action: 'start' | 'pause' | 'stop') => {
    setStrategies(prev => prev.map(strategy => 
      strategy.id === strategyId 
        ? { ...strategy, status: action === 'start' ? 'running' : action === 'pause' ? 'paused' : 'stopped' }
        : strategy
    ))
  }

  const getStrategyIcon = (type: AlgorithmicStrategy['type']) => {
    switch (type) {
      case 'momentum': return TrendingUp
      case 'mean_reversion': return Target
      case 'arbitrage': return Activity
      case 'pairs_trading': return BarChart3
      case 'grid': return Settings
      default: return Brain
    }
  }

  const getStrategyColor = (type: AlgorithmicStrategy['type']) => {
    switch (type) {
      case 'momentum': return 'purple'
      case 'mean_reversion': return 'cyan'
      case 'arbitrage': return 'green'
      case 'pairs_trading': return 'orange'
      case 'grid': return 'blue'
      default: return 'gray'
    }
  }

  const totalPnL = strategies.reduce((sum, strategy) => sum + strategy.pnl, 0)
  const avgWinRate = strategies.reduce((sum, strategy) => sum + strategy.winRate, 0) / strategies.length
  const runningStrategies = strategies.filter(s => s.status === 'running').length

  return (
    <div className="space-y-6">
      {/* Header with Summary Stats */}
      <div className="glass-card rounded-2xl p-6">
        <div className="flex items-center justify-between mb-6">
          <div>
            <h2 className="text-2xl font-bold gradient-text">Algorithmic Trading</h2>
            <p className="text-slate-400">Automated trading strategies and backtesting</p>
          </div>
          <div className="flex items-center space-x-2">
            <div className="w-3 h-3 rounded-full bg-green-400 animate-pulse" />
            <span className="text-sm font-medium text-green-400">{runningStrategies} Active</span>
          </div>
        </div>

        <div className="grid gap-4 md:grid-cols-4">
          <div className="glass-card p-4 rounded-xl bg-slate-800/30">
            <div className="flex items-center space-x-3 mb-2">
              <div className="p-2 rounded-lg bg-gradient-to-br from-purple-500/20 to-purple-600/20">
                <Brain className="h-5 w-5 text-purple-400" />
              </div>
              <div>
                <div className="text-sm text-slate-400">Total P&L</div>
                <div className={`text-lg font-bold ${totalPnL >= 0 ? 'text-green-400' : 'text-red-400'}`}>
                  {totalPnL >= 0 ? '+' : ''}₹{totalPnL.toLocaleString()}
                </div>
              </div>
            </div>
          </div>
          
          <div className="glass-card p-4 rounded-xl bg-slate-800/30">
            <div className="flex items-center space-x-3 mb-2">
              <div className="p-2 rounded-lg bg-gradient-to-br from-green-500/20 to-green-600/20">
                <Target className="h-5 w-5 text-green-400" />
              </div>
              <div>
                <div className="text-sm text-slate-400">Avg Win Rate</div>
                <div className="text-lg font-bold text-green-400">{avgWinRate.toFixed(1)}%</div>
              </div>
            </div>
          </div>

          <div className="glass-card p-4 rounded-xl bg-slate-800/30">
            <div className="flex items-center space-x-3 mb-2">
              <div className="p-2 rounded-lg bg-gradient-to-br from-cyan-500/20 to-cyan-600/20">
                <Activity className="h-5 w-5 text-cyan-400" />
              </div>
              <div>
                <div className="text-sm text-slate-400">Active Strategies</div>
                <div className="text-lg font-bold text-cyan-400">{runningStrategies}/{strategies.length}</div>
              </div>
            </div>
          </div>

          <div className="glass-card p-4 rounded-xl bg-slate-800/30">
            <div className="flex items-center space-x-3 mb-2">
              <div className="p-2 rounded-lg bg-gradient-to-br from-orange-500/20 to-orange-600/20">
                <BarChart3 className="h-5 w-5 text-orange-400" />
              </div>
              <div>
                <div className="text-sm text-slate-400">Total Trades</div>
                <div className="text-lg font-bold text-orange-400">
                  {strategies.reduce((sum, s) => sum + s.totalTrades, 0)}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Tab Navigation */}
      <div className="flex items-center space-x-6 mb-6">
        {[
          { key: 'strategies', label: 'Active Strategies', icon: Brain },
          { key: 'backtest', label: 'Backtesting', icon: BarChart3 },
          { key: 'create', label: 'Create Strategy', icon: Settings },
        ].map(({ key, label, icon: Icon }) => (
          <button
            key={key}
            onClick={() => setActiveTab(key as any)}
            className={`flex items-center space-x-2 px-4 py-2 rounded-xl transition-all ${
              activeTab === key
                ? 'bg-purple-500/20 text-purple-400'
                : 'text-slate-400 hover:text-white'
            }`}
          >
            <Icon className="w-4 h-4" />
            <span>{label}</span>
          </button>
        ))}
      </div>

      {/* Active Strategies Tab */}
      {activeTab === 'strategies' && (
        <div className="space-y-4">
          {strategies.map((strategy) => {
            const StrategyIcon = getStrategyIcon(strategy.type)
            const colorClass = getStrategyColor(strategy.type)
            
            return (
              <div key={strategy.id} className="glass-card rounded-2xl p-6 hover:scale-[1.01] transition-all">
                <div className="flex items-center justify-between mb-4">
                  <div className="flex items-center space-x-4">
                    <div className={`p-3 rounded-xl bg-gradient-to-br from-${colorClass}-500/20 to-${colorClass}-600/20`}>
                      <StrategyIcon className={`h-6 w-6 text-${colorClass}-400`} />
                    </div>
                    <div>
                      <h3 className="text-lg font-bold text-white">{strategy.name}</h3>
                      <p className="text-sm text-slate-400">{strategy.description}</p>
                      <div className="flex items-center space-x-3 mt-1">
                        <span className="text-xs px-2 py-1 rounded-full bg-slate-700/50 text-slate-300 capitalize">
                          {strategy.type.replace('_', ' ')}
                        </span>
                        <span className="text-xs text-slate-500">
                          {strategy.symbols.join(', ')}
                        </span>
                      </div>
                    </div>
                  </div>
                  
                  <div className="flex items-center space-x-4">
                    <div className={`px-3 py-1.5 rounded-xl text-sm font-semibold ${
                      strategy.status === 'running' 
                        ? 'bg-green-500/20 text-green-400' 
                        : strategy.status === 'paused'
                        ? 'bg-yellow-500/20 text-yellow-400'
                        : 'bg-gray-500/20 text-gray-400'
                    }`}>
                      {strategy.status}
                    </div>
                    
                    <div className="flex items-center space-x-2">
                      {strategy.status !== 'running' && (
                        <button
                          onClick={() => handleStrategyAction(strategy.id, 'start')}
                          className="p-2 rounded-xl bg-green-500/20 text-green-400 hover:bg-green-500/30 transition-colors"
                          title="Start Strategy"
                        >
                          <Play className="w-4 h-4" />
                        </button>
                      )}
                      
                      {strategy.status === 'running' && (
                        <button
                          onClick={() => handleStrategyAction(strategy.id, 'pause')}
                          className="p-2 rounded-xl bg-yellow-500/20 text-yellow-400 hover:bg-yellow-500/30 transition-colors"
                          title="Pause Strategy"
                        >
                          <Pause className="w-4 h-4" />
                        </button>
                      )}
                      
                      <button
                        onClick={() => handleStrategyAction(strategy.id, 'stop')}
                        className="p-2 rounded-xl bg-red-500/20 text-red-400 hover:bg-red-500/30 transition-colors"
                        title="Stop Strategy"
                      >
                        <Square className="w-4 h-4" />
                      </button>
                      
                      <button
                        onClick={() => setSelectedStrategy(strategy)}
                        className="p-2 rounded-xl bg-purple-500/20 text-purple-400 hover:bg-purple-500/30 transition-colors"
                        title="Strategy Settings"
                      >
                        <Settings className="w-4 h-4" />
                      </button>
                    </div>
                  </div>
                </div>

                <div className="grid gap-4 md:grid-cols-6">
                  <div className="text-center">
                    <div className={`text-lg font-bold ${strategy.pnl >= 0 ? 'text-green-400' : 'text-red-400'}`}>
                      {strategy.pnl >= 0 ? '+' : ''}₹{strategy.pnl.toLocaleString()}
                    </div>
                    <div className="text-sm text-slate-400">P&L</div>
                  </div>
                  
                  <div className="text-center">
                    <div className={`text-lg font-bold ${strategy.pnlPercent >= 0 ? 'text-green-400' : 'text-red-400'}`}>
                      {strategy.pnlPercent >= 0 ? '+' : ''}{strategy.pnlPercent}%
                    </div>
                    <div className="text-sm text-slate-400">Return</div>
                  </div>
                  
                  <div className="text-center">
                    <div className="text-lg font-bold text-white">{strategy.totalTrades}</div>
                    <div className="text-sm text-slate-400">Trades</div>
                  </div>
                  
                  <div className="text-center">
                    <div className="text-lg font-bold text-cyan-400">{strategy.winRate.toFixed(1)}%</div>
                    <div className="text-sm text-slate-400">Win Rate</div>
                  </div>
                  
                  <div className="text-center">
                    <div className="text-lg font-bold text-orange-400">{strategy.sharpe.toFixed(2)}</div>
                    <div className="text-sm text-slate-400">Sharpe</div>
                  </div>
                  
                  <div className="text-center">
                    <div className="text-lg font-bold text-red-400">{strategy.maxDrawdown.toFixed(1)}%</div>
                    <div className="text-sm text-slate-400">Max DD</div>
                  </div>
                </div>
              </div>
            )
          })}
        </div>
      )}

      {/* Backtesting Tab */}
      {activeTab === 'backtest' && (
        <div className="space-y-6">
          <div className="glass-card rounded-2xl p-6">
            <h3 className="text-xl font-bold text-white mb-6">Strategy Backtesting Results</h3>
            
            <div className="space-y-4">
              {backtestResults.map((result, index) => (
                <div key={index} className="glass-card p-4 rounded-xl bg-slate-800/30">
                  <div className="flex items-center justify-between mb-4">
                    <div>
                      <h4 className="font-semibold text-white">{result.strategy}</h4>
                      <div className="text-sm text-slate-400">Backtest Period: {result.period}</div>
                    </div>
                    <div className={`px-3 py-1.5 rounded-xl text-sm font-semibold ${
                      result.totalReturn >= 0 ? 'bg-green-500/20 text-green-400' : 'bg-red-500/20 text-red-400'
                    }`}>
                      {result.totalReturn >= 0 ? '+' : ''}{result.totalReturn}% Total Return
                    </div>
                  </div>
                  
                  <div className="grid gap-4 md:grid-cols-7 text-sm">
                    <div className="text-center">
                      <div className="text-red-400 font-semibold">{result.maxDrawdown}%</div>
                      <div className="text-slate-400">Max DD</div>
                    </div>
                    <div className="text-center">
                      <div className="text-purple-400 font-semibold">{result.sharpeRatio.toFixed(2)}</div>
                      <div className="text-slate-400">Sharpe</div>
                    </div>
                    <div className="text-center">
                      <div className="text-cyan-400 font-semibold">{result.winRate.toFixed(1)}%</div>
                      <div className="text-slate-400">Win Rate</div>
                    </div>
                    <div className="text-center">
                      <div className="text-white font-semibold">{result.totalTrades}</div>
                      <div className="text-slate-400">Trades</div>
                    </div>
                    <div className="text-center">
                      <div className="text-green-400 font-semibold">{result.profitFactor.toFixed(1)}</div>
                      <div className="text-slate-400">Profit Factor</div>
                    </div>
                    <div className="text-center">
                      <div className="text-orange-400 font-semibold">{result.avgTrade.toFixed(2)}%</div>
                      <div className="text-slate-400">Avg Trade</div>
                    </div>
                    <div className="text-center">
                      <button 
                        onClick={() => alert('Detailed backtest report generation coming soon!')}
                        className="cyber-button-sm px-3 py-1.5 text-xs"
                      >
                        View Details
                      </button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
          
          <div className="glass-card rounded-2xl p-6">
            <h3 className="text-xl font-bold text-white mb-4">Run New Backtest</h3>
            <div className="grid gap-4 md:grid-cols-3">
              <div>
                <label className="block text-sm font-medium text-slate-300 mb-2">Strategy Type</label>
                <select className="cyber-input w-full py-3 rounded-xl text-white">
                  <option>Momentum Breakout</option>
                  <option>Mean Reversion</option>
                  <option>Pairs Trading</option>
                  <option>Grid Trading</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-300 mb-2">Time Period</label>
                <select className="cyber-input w-full py-3 rounded-xl text-white">
                  <option>Last 3 Months</option>
                  <option>Last 6 Months</option>
                  <option>Last 1 Year</option>
                  <option>Last 2 Years</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-300 mb-2">Symbols</label>
                <input 
                  type="text" 
                  placeholder="RELIANCE, TCS, HDFC"
                  className="cyber-input w-full py-3 rounded-xl text-white"
                />
              </div>
            </div>
            <button 
              onClick={() => alert('Backtesting engine starting... This will analyze historical data and generate comprehensive performance metrics.')}
              className="cyber-button mt-4 px-6 py-3 rounded-xl"
            >
              <BarChart3 className="w-4 h-4 mr-2" />
              Start Backtest
            </button>
          </div>
        </div>
      )}

      {/* Create Strategy Tab */}
      {activeTab === 'create' && (
        <div className="glass-card rounded-2xl p-6">
          <h3 className="text-xl font-bold text-white mb-6">Create New Strategy</h3>
          
          <div className="space-y-6">
            <div className="grid gap-6 md:grid-cols-2">
              <div>
                <label className="block text-sm font-medium text-slate-300 mb-2">Strategy Name</label>
                <input 
                  type="text" 
                  placeholder="My Custom Strategy"
                  className="cyber-input w-full py-3 rounded-xl text-white"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-300 mb-2">Strategy Type</label>
                <select className="cyber-input w-full py-3 rounded-xl text-white">
                  <option>Momentum</option>
                  <option>Mean Reversion</option>
                  <option>Arbitrage</option>
                  <option>Pairs Trading</option>
                  <option>Grid Trading</option>
                </select>
              </div>
            </div>
            
            <div>
              <label className="block text-sm font-medium text-slate-300 mb-2">Description</label>
              <textarea 
                placeholder="Describe your trading strategy logic..."
                className="cyber-input w-full py-3 rounded-xl text-white h-24 resize-none"
              />
            </div>
            
            <div className="grid gap-6 md:grid-cols-3">
              <div>
                <label className="block text-sm font-medium text-slate-300 mb-2">Symbols</label>
                <input 
                  type="text" 
                  placeholder="RELIANCE, TCS"
                  className="cyber-input w-full py-3 rounded-xl text-white"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-300 mb-2">Risk Per Trade (%)</label>
                <input 
                  type="number" 
                  step="0.1"
                  placeholder="1.0"
                  className="cyber-input w-full py-3 rounded-xl text-white"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-300 mb-2">Max Positions</label>
                <input 
                  type="number" 
                  placeholder="5"
                  className="cyber-input w-full py-3 rounded-xl text-white"
                />
              </div>
            </div>
            
            <div className="glass-card p-4 rounded-xl bg-slate-800/30">
              <h4 className="font-semibold text-white mb-4">Strategy Parameters</h4>
              <div className="grid gap-4 md:grid-cols-2">
                <div>
                  <label className="block text-sm font-medium text-slate-300 mb-2">Lookback Period</label>
                  <input 
                    type="number" 
                    placeholder="20"
                    className="cyber-input w-full py-2 rounded-xl text-white"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-300 mb-2">Signal Threshold</label>
                  <input 
                    type="number" 
                    step="0.01"
                    placeholder="0.02"
                    className="cyber-input w-full py-2 rounded-xl text-white"
                  />
                </div>
              </div>
            </div>
            
            <div className="flex items-center space-x-4">
              <button 
                onClick={() => alert('Strategy validation and deployment coming soon! This will test your strategy parameters and deploy it for live trading.')}
                className="cyber-button px-6 py-3 rounded-xl"
              >
                <CheckCircle className="w-4 h-4 mr-2" />
                Create & Deploy
              </button>
              <button 
                onClick={() => alert('Strategy backtesting coming soon! This will test your strategy on historical data first.')}
                className="glass-card border border-purple-500/50 hover:border-purple-400/70 text-purple-400 px-6 py-3 rounded-xl transition-colors"
              >
                <BarChart3 className="w-4 h-4 mr-2" />
                Backtest First
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Strategy Configuration Modal */}
      {selectedStrategy && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="glass-card rounded-2xl p-6 max-w-2xl w-full mx-4 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between mb-6">
              <h3 className="text-xl font-bold text-white">{selectedStrategy.name} Settings</h3>
              <button
                onClick={() => setSelectedStrategy(null)}
                className="p-2 rounded-xl text-slate-400 hover:text-white transition-colors"
              >
                ✕
              </button>
            </div>
            
            <div className="space-y-6">
              <div className="grid gap-4 md:grid-cols-2">
                <div>
                  <label className="block text-sm font-medium text-slate-300 mb-2">Risk Per Trade (%)</label>
                  <input 
                    type="number" 
                    step="0.1"
                    value={selectedStrategy.parameters.risk_per_trade || 1.0}
                    className="cyber-input w-full py-3 rounded-xl text-white"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-300 mb-2">Lookback Period</label>
                  <input 
                    type="number" 
                    value={selectedStrategy.parameters.lookback_period || 20}
                    className="cyber-input w-full py-3 rounded-xl text-white"
                  />
                </div>
              </div>
              
              {selectedStrategy.type === 'momentum' && (
                <div className="grid gap-4 md:grid-cols-2">
                  <div>
                    <label className="block text-sm font-medium text-slate-300 mb-2">Volume Threshold</label>
                    <input 
                      type="number" 
                      step="0.1"
                      value={selectedStrategy.parameters.volume_threshold || 1.5}
                      className="cyber-input w-full py-3 rounded-xl text-white"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-slate-300 mb-2">Breakout Threshold (%)</label>
                    <input 
                      type="number" 
                      step="0.01"
                      placeholder="2.0"
                      className="cyber-input w-full py-3 rounded-xl text-white"
                    />
                  </div>
                </div>
              )}
              
              {selectedStrategy.type === 'mean_reversion' && (
                <div className="grid gap-4 md:grid-cols-3">
                  <div>
                    <label className="block text-sm font-medium text-slate-300 mb-2">RSI Period</label>
                    <input 
                      type="number" 
                      value={selectedStrategy.parameters.rsi_period || 14}
                      className="cyber-input w-full py-3 rounded-xl text-white"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-slate-300 mb-2">Oversold Level</label>
                    <input 
                      type="number" 
                      value={selectedStrategy.parameters.oversold_level || 30}
                      className="cyber-input w-full py-3 rounded-xl text-white"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-slate-300 mb-2">Overbought Level</label>
                    <input 
                      type="number" 
                      value={selectedStrategy.parameters.overbought_level || 70}
                      className="cyber-input w-full py-3 rounded-xl text-white"
                    />
                  </div>
                </div>
              )}
              
              <div className="flex items-center justify-end space-x-4">
                <button
                  onClick={() => setSelectedStrategy(null)}
                  className="glass-card border border-slate-600/50 text-slate-400 px-6 py-3 rounded-xl transition-colors"
                >
                  Cancel
                </button>
                <button
                  onClick={() => {
                    alert('Strategy parameters updated successfully!')
                    setSelectedStrategy(null)
                  }}
                  className="cyber-button px-6 py-3 rounded-xl"
                >
                  Update Strategy
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}