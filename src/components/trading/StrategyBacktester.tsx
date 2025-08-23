import React, { useState } from 'react'
import { 
  BarChart3,
  TrendingUp,
  TrendingDown,
  Calendar,
  Target,
  Activity,
  DollarSign,
  AlertTriangle,
  CheckCircle,
  Play,
  Download,
  Settings,
  Zap,
  Clock
} from 'lucide-react'

interface BacktestParameters {
  strategy: string
  symbols: string[]
  startDate: string
  endDate: string
  initialCapital: number
  riskPerTrade: number
  maxPositions: number
  commission: number
  slippage: number
}

interface BacktestResult {
  totalReturn: number
  annualizedReturn: number
  maxDrawdown: number
  sharpeRatio: number
  sortinoRatio: number
  calmarRatio: number
  winRate: number
  totalTrades: number
  avgWinTrade: number
  avgLossTrade: number
  profitFactor: number
  expectancy: number
  volatility: number
  beta: number
  alpha: number
}

interface TradeResult {
  id: number
  symbol: string
  entryDate: string
  exitDate: string
  entryPrice: number
  exitPrice: number
  quantity: number
  pnl: number
  pnlPercent: number
  holdingPeriod: number
  side: 'long' | 'short'
}

const mockBacktestResult: BacktestResult = {
  totalReturn: 24.8,
  annualizedReturn: 18.6,
  maxDrawdown: -8.2,
  sharpeRatio: 1.85,
  sortinoRatio: 2.42,
  calmarRatio: 2.27,
  winRate: 62.5,
  totalTrades: 48,
  avgWinTrade: 3.4,
  avgLossTrade: -1.8,
  profitFactor: 1.89,
  expectancy: 0.52,
  volatility: 12.3,
  beta: 0.85,
  alpha: 5.9
}

const mockTrades: TradeResult[] = [
  {
    id: 1,
    symbol: 'RELIANCE',
    entryDate: '2024-01-15',
    exitDate: '2024-01-28',
    entryPrice: 2420.50,
    exitPrice: 2487.30,
    quantity: 10,
    pnl: 668.0,
    pnlPercent: 2.76,
    holdingPeriod: 13,
    side: 'long'
  },
  {
    id: 2,
    symbol: 'TCS',
    entryDate: '2024-02-03',
    exitDate: '2024-02-18',
    entryPrice: 3680.20,
    exitPrice: 3542.80,
    quantity: 5,
    pnl: -687.0,
    pnlPercent: -3.73,
    holdingPeriod: 15,
    side: 'long'
  },
  {
    id: 3,
    symbol: 'HDFC',
    entryDate: '2024-02-25',
    exitDate: '2024-03-08',
    entryPrice: 1545.30,
    exitPrice: 1623.45,
    quantity: 15,
    pnl: 1172.25,
    pnlPercent: 5.05,
    holdingPeriod: 12,
    side: 'long'
  }
]

export function StrategyBacktester() {
  const [backtestParams, setBacktestParams] = useState<BacktestParameters>({
    strategy: 'momentum_breakout',
    symbols: ['RELIANCE', 'TCS', 'HDFC'],
    startDate: '2023-01-01',
    endDate: '2024-01-01',
    initialCapital: 100000,
    riskPerTrade: 2.0,
    maxPositions: 5,
    commission: 0.1,
    slippage: 0.05
  })
  
  const [backtestResult, setBacktestResult] = useState<BacktestResult | null>(null)
  const [trades, setTrades] = useState<TradeResult[]>([])
  const [isRunning, setIsRunning] = useState(false)
  const [activeView, setActiveView] = useState<'setup' | 'results' | 'trades' | 'performance'>('setup')

  const runBacktest = async () => {
    setIsRunning(true)
    // Simulate backtesting process
    setTimeout(() => {
      setBacktestResult(mockBacktestResult)
      setTrades(mockTrades)
      setActiveView('results')
      setIsRunning(false)
    }, 3000)
  }

  const exportResults = () => {
    alert('Exporting backtest results to CSV/PDF... This will include all trades, performance metrics, and charts.')
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="glass-card rounded-2xl p-6">
        <div className="flex items-center justify-between mb-4">
          <div>
            <h2 className="text-2xl font-bold gradient-text">Strategy Backtester</h2>
            <p className="text-slate-400">Test your trading strategies on historical data</p>
          </div>
          <div className="flex items-center space-x-4">
            {backtestResult && (
              <button
                onClick={exportResults}
                className="flex items-center space-x-2 glass-card px-4 py-2 rounded-xl text-slate-400 hover:text-white transition-colors"
              >
                <Download className="w-4 h-4" />
                <span>Export</span>
              </button>
            )}
            {!isRunning && (
              <button
                onClick={runBacktest}
                className="cyber-button px-6 py-2 rounded-xl"
              >
                <Play className="w-4 h-4 mr-2" />
                Run Backtest
              </button>
            )}
          </div>
        </div>

        {/* Progress Bar */}
        {isRunning && (
          <div className="mb-4">
            <div className="flex items-center justify-between mb-2">
              <span className="text-sm text-slate-400">Running backtest...</span>
              <span className="text-sm text-purple-400">Processing historical data</span>
            </div>
            <div className="w-full bg-slate-700 rounded-full h-2">
              <div className="h-2 bg-gradient-to-r from-purple-500 to-cyan-500 rounded-full animate-pulse" style={{ width: '100%' }} />
            </div>
          </div>
        )}
      </div>

      {/* Tab Navigation */}
      <div className="flex items-center space-x-6 mb-6">
        {[
          { key: 'setup', label: 'Setup', icon: Settings },
          { key: 'results', label: 'Results', icon: BarChart3, disabled: !backtestResult },
          { key: 'trades', label: 'Trades', icon: Activity, disabled: !backtestResult },
          { key: 'performance', label: 'Performance', icon: TrendingUp, disabled: !backtestResult },
        ].map(({ key, label, icon: Icon, disabled }) => (
          <button
            key={key}
            onClick={() => !disabled && setActiveView(key as any)}
            disabled={disabled}
            className={`flex items-center space-x-2 px-4 py-2 rounded-xl transition-all ${
              activeView === key
                ? 'bg-purple-500/20 text-purple-400'
                : disabled 
                ? 'text-slate-600 cursor-not-allowed'
                : 'text-slate-400 hover:text-white'
            }`}
          >
            <Icon className="w-4 h-4" />
            <span>{label}</span>
          </button>
        ))}
      </div>

      {/* Setup Tab */}
      {activeView === 'setup' && (
        <div className="space-y-6">
          <div className="glass-card rounded-2xl p-6">
            <h3 className="text-xl font-bold text-white mb-6">Backtest Configuration</h3>
            
            <div className="space-y-6">
              <div className="grid gap-6 md:grid-cols-2">
                <div>
                  <label className="block text-sm font-medium text-slate-300 mb-2">Strategy Type</label>
                  <select
                    value={backtestParams.strategy}
                    onChange={(e) => setBacktestParams(prev => ({ ...prev, strategy: e.target.value }))}
                    className="cyber-input w-full py-3 rounded-xl text-white"
                  >
                    <option value="momentum_breakout">Momentum Breakout</option>
                    <option value="mean_reversion">Mean Reversion</option>
                    <option value="pairs_trading">Pairs Trading</option>
                    <option value="grid_trading">Grid Trading</option>
                    <option value="custom">Custom Strategy</option>
                  </select>
                </div>
                
                <div>
                  <label className="block text-sm font-medium text-slate-300 mb-2">Symbols</label>
                  <input
                    type="text"
                    value={backtestParams.symbols.join(', ')}
                    onChange={(e) => setBacktestParams(prev => ({ 
                      ...prev, 
                      symbols: e.target.value.split(',').map(s => s.trim()) 
                    }))}
                    className="cyber-input w-full py-3 rounded-xl text-white"
                    placeholder="RELIANCE, TCS, HDFC"
                  />
                </div>
              </div>

              <div className="grid gap-6 md:grid-cols-2">
                <div>
                  <label className="block text-sm font-medium text-slate-300 mb-2">Start Date</label>
                  <input
                    type="date"
                    value={backtestParams.startDate}
                    onChange={(e) => setBacktestParams(prev => ({ ...prev, startDate: e.target.value }))}
                    className="cyber-input w-full py-3 rounded-xl text-white"
                  />
                </div>
                
                <div>
                  <label className="block text-sm font-medium text-slate-300 mb-2">End Date</label>
                  <input
                    type="date"
                    value={backtestParams.endDate}
                    onChange={(e) => setBacktestParams(prev => ({ ...prev, endDate: e.target.value }))}
                    className="cyber-input w-full py-3 rounded-xl text-white"
                  />
                </div>
              </div>

              <div className="grid gap-6 md:grid-cols-3">
                <div>
                  <label className="block text-sm font-medium text-slate-300 mb-2">Initial Capital (₹)</label>
                  <input
                    type="number"
                    value={backtestParams.initialCapital}
                    onChange={(e) => setBacktestParams(prev => ({ ...prev, initialCapital: parseFloat(e.target.value) }))}
                    className="cyber-input w-full py-3 rounded-xl text-white"
                  />
                </div>
                
                <div>
                  <label className="block text-sm font-medium text-slate-300 mb-2">Risk Per Trade (%)</label>
                  <input
                    type="number"
                    step="0.1"
                    value={backtestParams.riskPerTrade}
                    onChange={(e) => setBacktestParams(prev => ({ ...prev, riskPerTrade: parseFloat(e.target.value) }))}
                    className="cyber-input w-full py-3 rounded-xl text-white"
                  />
                </div>
                
                <div>
                  <label className="block text-sm font-medium text-slate-300 mb-2">Max Positions</label>
                  <input
                    type="number"
                    value={backtestParams.maxPositions}
                    onChange={(e) => setBacktestParams(prev => ({ ...prev, maxPositions: parseInt(e.target.value) }))}
                    className="cyber-input w-full py-3 rounded-xl text-white"
                  />
                </div>
              </div>

              <div className="glass-card p-4 rounded-xl bg-slate-800/30">
                <h4 className="font-semibold text-white mb-4">Trading Costs</h4>
                <div className="grid gap-4 md:grid-cols-2">
                  <div>
                    <label className="block text-sm font-medium text-slate-300 mb-2">Commission (%)</label>
                    <input
                      type="number"
                      step="0.01"
                      value={backtestParams.commission}
                      onChange={(e) => setBacktestParams(prev => ({ ...prev, commission: parseFloat(e.target.value) }))}
                      className="cyber-input w-full py-2 rounded-xl text-white"
                    />
                  </div>
                  
                  <div>
                    <label className="block text-sm font-medium text-slate-300 mb-2">Slippage (%)</label>
                    <input
                      type="number"
                      step="0.01"
                      value={backtestParams.slippage}
                      onChange={(e) => setBacktestParams(prev => ({ ...prev, slippage: parseFloat(e.target.value) }))}
                      className="cyber-input w-full py-2 rounded-xl text-white"
                    />
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Results Tab */}
      {activeView === 'results' && backtestResult && (
        <div className="space-y-6">
          {/* Performance Summary */}
          <div className="glass-card rounded-2xl p-6">
            <h3 className="text-xl font-bold text-white mb-6">Backtest Results Summary</h3>
            
            <div className="grid gap-6 md:grid-cols-4">
              <div className="glass-card p-4 rounded-xl bg-slate-800/30">
                <div className="flex items-center space-x-3 mb-2">
                  <DollarSign className="h-5 w-5 text-green-400" />
                  <div>
                    <div className="text-sm text-slate-400">Total Return</div>
                    <div className="text-lg font-bold text-green-400">+{backtestResult.totalReturn}%</div>
                  </div>
                </div>
              </div>
              
              <div className="glass-card p-4 rounded-xl bg-slate-800/30">
                <div className="flex items-center space-x-3 mb-2">
                  <TrendingUp className="h-5 w-5 text-purple-400" />
                  <div>
                    <div className="text-sm text-slate-400">Sharpe Ratio</div>
                    <div className="text-lg font-bold text-purple-400">{backtestResult.sharpeRatio}</div>
                  </div>
                </div>
              </div>
              
              <div className="glass-card p-4 rounded-xl bg-slate-800/30">
                <div className="flex items-center space-x-3 mb-2">
                  <AlertTriangle className="h-5 w-5 text-red-400" />
                  <div>
                    <div className="text-sm text-slate-400">Max Drawdown</div>
                    <div className="text-lg font-bold text-red-400">{backtestResult.maxDrawdown}%</div>
                  </div>
                </div>
              </div>
              
              <div className="glass-card p-4 rounded-xl bg-slate-800/30">
                <div className="flex items-center space-x-3 mb-2">
                  <Target className="h-5 w-5 text-cyan-400" />
                  <div>
                    <div className="text-sm text-slate-400">Win Rate</div>
                    <div className="text-lg font-bold text-cyan-400">{backtestResult.winRate}%</div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* Detailed Metrics */}
          <div className="glass-card rounded-2xl p-6">
            <h3 className="text-xl font-bold text-white mb-6">Detailed Performance Metrics</h3>
            
            <div className="grid gap-6 md:grid-cols-2">
              <div className="space-y-4">
                <h4 className="font-semibold text-purple-400">Return Metrics</h4>
                <div className="space-y-3">
                  <div className="flex justify-between">
                    <span className="text-slate-400">Annualized Return:</span>
                    <span className="text-green-400 font-semibold">{backtestResult.annualizedReturn}%</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-400">Alpha:</span>
                    <span className="text-purple-400 font-semibold">{backtestResult.alpha}%</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-400">Beta:</span>
                    <span className="text-cyan-400 font-semibold">{backtestResult.beta}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-400">Volatility:</span>
                    <span className="text-orange-400 font-semibold">{backtestResult.volatility}%</span>
                  </div>
                </div>
              </div>

              <div className="space-y-4">
                <h4 className="font-semibold text-cyan-400">Risk Metrics</h4>
                <div className="space-y-3">
                  <div className="flex justify-between">
                    <span className="text-slate-400">Sortino Ratio:</span>
                    <span className="text-green-400 font-semibold">{backtestResult.sortinoRatio}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-400">Calmar Ratio:</span>
                    <span className="text-purple-400 font-semibold">{backtestResult.calmarRatio}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-400">Profit Factor:</span>
                    <span className="text-cyan-400 font-semibold">{backtestResult.profitFactor}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-400">Expectancy:</span>
                    <span className="text-orange-400 font-semibold">{backtestResult.expectancy}%</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* Trade Statistics */}
          <div className="glass-card rounded-2xl p-6">
            <h3 className="text-xl font-bold text-white mb-6">Trade Statistics</h3>
            
            <div className="grid gap-6 md:grid-cols-3">
              <div className="glass-card p-4 rounded-xl bg-slate-800/30">
                <div className="text-center">
                  <div className="text-2xl font-bold text-white mb-1">{backtestResult.totalTrades}</div>
                  <div className="text-sm text-slate-400">Total Trades</div>
                </div>
              </div>
              
              <div className="glass-card p-4 rounded-xl bg-slate-800/30">
                <div className="text-center">
                  <div className="text-2xl font-bold text-green-400 mb-1">+{backtestResult.avgWinTrade}%</div>
                  <div className="text-sm text-slate-400">Avg Winning Trade</div>
                </div>
              </div>
              
              <div className="glass-card p-4 rounded-xl bg-slate-800/30">
                <div className="text-center">
                  <div className="text-2xl font-bold text-red-400 mb-1">{backtestResult.avgLossTrade}%</div>
                  <div className="text-sm text-slate-400">Avg Losing Trade</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Trades Tab */}
      {activeView === 'trades' && (
        <div className="glass-card rounded-2xl p-6">
          <h3 className="text-xl font-bold text-white mb-6">Trade History</h3>
          
          <div className="space-y-3">
            {trades.map((trade) => (
              <div key={trade.id} className="glass-card p-4 rounded-xl bg-slate-800/30">
                <div className="flex items-center justify-between">
                  <div className="flex items-center space-x-4">
                    <div className={`p-2 rounded-xl ${
                      trade.pnl >= 0 
                        ? 'bg-green-500/20 text-green-400' 
                        : 'bg-red-500/20 text-red-400'
                    }`}>
                      {trade.pnl >= 0 ? <TrendingUp className="w-4 h-4" /> : <TrendingDown className="w-4 h-4" />}
                    </div>
                    <div>
                      <div className="font-semibold text-white">{trade.symbol}</div>
                      <div className="text-sm text-slate-400">
                        {trade.entryDate} → {trade.exitDate} ({trade.holdingPeriod} days)
                      </div>
                    </div>
                  </div>
                  
                  <div className="text-right">
                    <div className="grid gap-2 md:grid-cols-4 text-sm">
                      <div>
                        <div className="text-slate-400">Entry</div>
                        <div className="text-white">₹{trade.entryPrice}</div>
                      </div>
                      <div>
                        <div className="text-slate-400">Exit</div>
                        <div className="text-white">₹{trade.exitPrice}</div>
                      </div>
                      <div>
                        <div className="text-slate-400">P&L</div>
                        <div className={`font-semibold ${trade.pnl >= 0 ? 'text-green-400' : 'text-red-400'}`}>
                          {trade.pnl >= 0 ? '+' : ''}₹{trade.pnl}
                        </div>
                      </div>
                      <div>
                        <div className="text-slate-400">Return</div>
                        <div className={`font-semibold ${trade.pnlPercent >= 0 ? 'text-green-400' : 'text-red-400'}`}>
                          {trade.pnlPercent >= 0 ? '+' : ''}{trade.pnlPercent.toFixed(2)}%
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Performance Tab */}
      {activeView === 'performance' && (
        <div className="space-y-6">
          <div className="glass-card rounded-2xl p-6">
            <h3 className="text-xl font-bold text-white mb-6">Performance Analysis</h3>
            
            <div className="bg-slate-800/50 rounded-2xl p-8 h-80 flex items-center justify-center">
              <div className="text-center">
                <BarChart3 className="w-16 h-16 text-purple-400 mx-auto mb-4" />
                <h4 className="text-lg font-semibold text-white mb-2">Performance Charts</h4>
                <p className="text-slate-400">Equity curve, drawdown analysis, and monthly returns</p>
              </div>
            </div>
          </div>

          {/* Risk Analysis */}
          <div className="glass-card rounded-2xl p-6">
            <h3 className="text-xl font-bold text-white mb-6">Risk Analysis</h3>
            
            <div className="grid gap-6 md:grid-cols-2">
              <div className="space-y-4">
                <h4 className="font-semibold text-orange-400">Drawdown Analysis</h4>
                <div className="space-y-2">
                  <div className="flex justify-between">
                    <span className="text-slate-400">Maximum Drawdown:</span>
                    <span className="text-red-400 font-semibold">{backtestResult.maxDrawdown}%</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-400">Avg Drawdown:</span>
                    <span className="text-orange-400">-3.2%</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-400">Recovery Factor:</span>
                    <span className="text-green-400">3.02</span>
                  </div>
                </div>
              </div>

              <div className="space-y-4">
                <h4 className="font-semibold text-purple-400">Monthly Performance</h4>
                <div className="text-sm text-slate-400">
                  Best Month: +8.4% (Mar 2024)<br/>
                  Worst Month: -2.1% (Aug 2024)<br/>
                  Positive Months: 9 out of 12<br/>
                  Consistency Score: 87%
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}