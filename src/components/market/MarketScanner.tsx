import React, { useState, useEffect } from 'react'
import { Search, Filter, TrendingUp, TrendingDown, BarChart3, Zap, Star, Eye, RefreshCw } from 'lucide-react'
import { SymbolLookup } from '../common/SymbolLookup'

interface ScanResult {
  symbol: string
  name: string
  price: number
  change: number
  changePercent: number
  volume: number
  avgVolume: number
  marketCap: string
  sector: string
  pe: number
  pb: number
  rsi: number
  sma20: number
  sma50: number
  high52w: number
  low52w: number
  atr: number
  ema: number
  volumeRatio: number
}

interface ScanCriteria {
  priceRange: { min: number; max: number }
  changePercent: { min: number; max: number }
  volume: { min: number; max: number }
  marketCap: 'any' | 'large' | 'mid' | 'small'
  sector: string
  rsi: { min: number; max: number }
  pe: { min: number; max: number }
  technicalPattern: string
  sortBy: 'changePercent' | 'volume' | 'price' | 'marketCap' | 'rsi'
  sortOrder: 'asc' | 'desc'
}

interface MarketScannerProps {
  height?: number
}

const mockScanResults: ScanResult[] = [
  {
    symbol: 'RELIANCE',
    name: 'Reliance Industries Limited',
    price: 2547.30,
    change: 23.45,
    changePercent: 0.93,
    volume: 1200000,
    avgVolume: 980000,
    marketCap: '17.2L Cr',
    sector: 'Oil & Gas',
    pe: 24.5,
    pb: 2.1,
    rsi: 58.3,
    sma20: 2520.45,
    sma50: 2485.20,
    high52w: 2856.30,
    low52w: 2220.45,
    atr: 45.2,
    ema: 2535.80,
    volumeRatio: 1.22
  },
  {
    symbol: 'TCS',
    name: 'Tata Consultancy Services',
    price: 3642.80,
    change: -18.90,
    changePercent: -0.52,
    volume: 890000,
    avgVolume: 1100000,
    marketCap: '13.4L Cr',
    sector: 'IT Services',
    pe: 28.7,
    pb: 12.4,
    rsi: 45.2,
    sma20: 3680.25,
    sma50: 3725.60,
    high52w: 4080.15,
    low52w: 3100.25,
    atr: 68.4,
    ema: 3655.90,
    volumeRatio: 0.81
  },
  {
    symbol: 'HDFCBANK',
    name: 'HDFC Bank Limited',
    price: 1567.25,
    change: 12.80,
    changePercent: 0.82,
    volume: 2100000,
    avgVolume: 1850000,
    marketCap: '11.8L Cr',
    sector: 'Banking',
    pe: 19.8,
    pb: 2.8,
    rsi: 62.7,
    sma20: 1545.30,
    sma50: 1520.75,
    high52w: 1770.25,
    low52w: 1363.55,
    atr: 28.9,
    ema: 1558.45,
    volumeRatio: 1.14
  },
  {
    symbol: 'ADANIPORTS',
    name: 'Adani Ports & SEZ',
    price: 789.60,
    change: 25.30,
    changePercent: 3.31,
    volume: 5600000,
    avgVolume: 2800000,
    marketCap: '1.6L Cr',
    sector: 'Infrastructure',
    pe: 15.2,
    pb: 1.8,
    rsi: 78.5,
    sma20: 745.20,
    sma50: 720.30,
    high52w: 850.40,
    low52w: 620.15,
    atr: 18.5,
    ema: 768.90,
    volumeRatio: 2.00
  },
  {
    symbol: 'BAJFINANCE',
    name: 'Bajaj Finance Limited',
    price: 6789.45,
    change: -89.55,
    changePercent: -1.30,
    volume: 678000,
    avgVolume: 850000,
    marketCap: '4.2L Cr',
    sector: 'Financial Services',
    pe: 22.4,
    pb: 4.2,
    rsi: 35.8,
    sma20: 6920.75,
    sma50: 7125.40,
    high52w: 8050.30,
    low52w: 6120.25,
    atr: 125.6,
    ema: 6845.20,
    volumeRatio: 0.80
  }
]

const presetScans = [
  {
    name: 'Top Gainers',
    icon: TrendingUp,
    criteria: { changePercent: { min: 3, max: 100 }, sortBy: 'changePercent' as const, sortOrder: 'desc' as const }
  },
  {
    name: 'Top Losers', 
    icon: TrendingDown,
    criteria: { changePercent: { min: -100, max: -3 }, sortBy: 'changePercent' as const, sortOrder: 'asc' as const }
  },
  {
    name: 'High Volume',
    icon: BarChart3,
    criteria: { volume: { min: 2000000, max: 999999999 }, sortBy: 'volume' as const, sortOrder: 'desc' as const }
  },
  {
    name: 'Breakouts',
    icon: Zap,
    criteria: { rsi: { min: 70, max: 100 }, changePercent: { min: 2, max: 100 }, sortBy: 'changePercent' as const, sortOrder: 'desc' as const }
  }
]

export function MarketScanner({ height = 600 }: MarketScannerProps) {
  const [scanResults, setScanResults] = useState<ScanResult[]>(mockScanResults)
  const [filteredResults, setFilteredResults] = useState<ScanResult[]>(mockScanResults)
  const [isScanning, setIsScanning] = useState(false)
  const [showAdvancedFilters, setShowAdvancedFilters] = useState(false)
  const [selectedStock, setSelectedStock] = useState<ScanResult | null>(null)
  
  const [criteria, setCriteria] = useState<ScanCriteria>({
    priceRange: { min: 0, max: 10000 },
    changePercent: { min: -50, max: 50 },
    volume: { min: 0, max: 999999999 },
    marketCap: 'any',
    sector: 'all',
    rsi: { min: 0, max: 100 },
    pe: { min: 0, max: 100 },
    technicalPattern: 'all',
    sortBy: 'changePercent',
    sortOrder: 'desc'
  })

  useEffect(() => {
    // Apply filters to scan results
    let filtered = scanResults.filter(stock => {
      return (
        stock.price >= criteria.priceRange.min &&
        stock.price <= criteria.priceRange.max &&
        stock.changePercent >= criteria.changePercent.min &&
        stock.changePercent <= criteria.changePercent.max &&
        stock.volume >= criteria.volume.min &&
        stock.volume <= criteria.volume.max &&
        stock.rsi >= criteria.rsi.min &&
        stock.rsi <= criteria.rsi.max &&
        stock.pe >= criteria.pe.min &&
        stock.pe <= criteria.pe.max &&
        (criteria.sector === 'all' || stock.sector === criteria.sector)
      )
    })

    // Sort results
    filtered.sort((a, b) => {
      const aValue = a[criteria.sortBy]
      const bValue = b[criteria.sortBy]
      
      if (typeof aValue === 'string') {
        return criteria.sortOrder === 'asc' 
          ? aValue.localeCompare(bValue as string)
          : (bValue as string).localeCompare(aValue)
      }
      
      return criteria.sortOrder === 'asc' 
        ? (aValue as number) - (bValue as number)
        : (bValue as number) - (aValue as number)
    })

    setFilteredResults(filtered)
  }, [scanResults, criteria])

  const runPresetScan = (preset: any) => {
    setCriteria(prev => ({
      ...prev,
      ...preset.criteria
    }))
    runScan()
  }

  const runScan = async () => {
    setIsScanning(true)
    
    // Simulate API call
    await new Promise(resolve => setTimeout(resolve, 2000))
    
    // In real app, this would fetch from API based on criteria
    setIsScanning(false)
  }

  const getRSIColor = (rsi: number) => {
    if (rsi >= 70) return 'text-red-400'
    if (rsi <= 30) return 'text-green-400'
    return 'text-yellow-400'
  }

  const getTechnicalSignal = (stock: ScanResult) => {
    if (stock.price > stock.sma20 && stock.price > stock.sma50 && stock.rsi < 70) {
      return { signal: 'BUY', color: 'text-green-400', bg: 'bg-green-500/20' }
    } else if (stock.price < stock.sma20 && stock.price < stock.sma50 && stock.rsi > 30) {
      return { signal: 'SELL', color: 'text-red-400', bg: 'bg-red-500/20' }
    }
    return { signal: 'HOLD', color: 'text-yellow-400', bg: 'bg-yellow-500/20' }
  }

  return (
    <div className="glass-card rounded-2xl p-6" style={{ height }}>
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div>
          <h3 className="text-lg font-bold text-white flex items-center">
            <Search className="w-5 h-5 mr-2 text-cyan-400" />
            Market Scanner
          </h3>
          <p className="text-sm text-slate-400 mt-1">
            {filteredResults.length} stocks found • Last scan: {new Date().toLocaleTimeString('en-IN')}
          </p>
        </div>
        
        <div className="flex items-center space-x-2">
          <button
            onClick={() => setShowAdvancedFilters(!showAdvancedFilters)}
            className={`p-2 rounded-xl transition-colors ${
              showAdvancedFilters
                ? 'bg-purple-500/20 text-purple-400'
                : 'text-slate-400 hover:text-white hover:bg-slate-700/50'
            }`}
            title="Advanced Filters"
          >
            <Filter className="w-4 h-4" />
          </button>
          
          <button
            onClick={runScan}
            disabled={isScanning}
            className="cyber-button px-4 py-2 text-sm rounded-xl flex items-center space-x-2 disabled:opacity-50"
          >
            <RefreshCw className={`w-4 h-4 ${isScanning ? 'animate-spin' : ''}`} />
            <span>{isScanning ? 'Scanning...' : 'Scan'}</span>
          </button>
        </div>
      </div>

      {/* Preset Scans */}
      <div className="grid gap-3 md:grid-cols-4 mb-6">
        {presetScans.map((preset) => (
          <button
            key={preset.name}
            onClick={() => runPresetScan(preset)}
            className="flex items-center space-x-3 p-3 rounded-xl glass-card text-slate-400 hover:text-white hover:bg-slate-700/30 transition-all group"
          >
            <div className="p-2 rounded-lg bg-slate-600/50 group-hover:bg-purple-500/20 transition-colors">
              <preset.icon className="w-4 h-4 group-hover:text-purple-400 transition-colors" />
            </div>
            <span className="text-sm font-medium">{preset.name}</span>
          </button>
        ))}
      </div>

      {/* Advanced Filters */}
      {showAdvancedFilters && (
        <div className="mb-6 p-4 rounded-xl bg-slate-800/30 border border-purple-500/30">
          <h4 className="font-semibold text-white mb-4">Advanced Filters</h4>
          
          <div className="grid gap-4 md:grid-cols-3">
            {/* Price Range */}
            <div>
              <label className="block text-sm font-medium text-slate-300 mb-2">Price Range (Rs.)</label>
              <div className="flex space-x-2">
                <input
                  type="number"
                  placeholder="Min"
                  value={criteria.priceRange.min || ''}
                  onChange={(e) => setCriteria(prev => ({
                    ...prev,
                    priceRange: { ...prev.priceRange, min: parseFloat(e.target.value) || 0 }
                  }))}
                  className="cyber-input flex-1 py-2 text-sm rounded-xl"
                />
                <input
                  type="number"
                  placeholder="Max"
                  value={criteria.priceRange.max || ''}
                  onChange={(e) => setCriteria(prev => ({
                    ...prev,
                    priceRange: { ...prev.priceRange, max: parseFloat(e.target.value) || 10000 }
                  }))}
                  className="cyber-input flex-1 py-2 text-sm rounded-xl"
                />
              </div>
            </div>
            
            {/* Change % */}
            <div>
              <label className="block text-sm font-medium text-slate-300 mb-2">Change % Range</label>
              <div className="flex space-x-2">
                <input
                  type="number"
                  placeholder="Min"
                  value={criteria.changePercent.min || ''}
                  onChange={(e) => setCriteria(prev => ({
                    ...prev,
                    changePercent: { ...prev.changePercent, min: parseFloat(e.target.value) || -50 }
                  }))}
                  className="cyber-input flex-1 py-2 text-sm rounded-xl"
                />
                <input
                  type="number"
                  placeholder="Max"
                  value={criteria.changePercent.max || ''}
                  onChange={(e) => setCriteria(prev => ({
                    ...prev,
                    changePercent: { ...prev.changePercent, max: parseFloat(e.target.value) || 50 }
                  }))}
                  className="cyber-input flex-1 py-2 text-sm rounded-xl"
                />
              </div>
            </div>
            
            {/* RSI Range */}
            <div>
              <label className="block text-sm font-medium text-slate-300 mb-2">RSI Range</label>
              <div className="flex space-x-2">
                <input
                  type="number"
                  placeholder="Min"
                  value={criteria.rsi.min || ''}
                  onChange={(e) => setCriteria(prev => ({
                    ...prev,
                    rsi: { ...prev.rsi, min: parseFloat(e.target.value) || 0 }
                  }))}
                  className="cyber-input flex-1 py-2 text-sm rounded-xl"
                  min="0"
                  max="100"
                />
                <input
                  type="number"
                  placeholder="Max"
                  value={criteria.rsi.max || ''}
                  onChange={(e) => setCriteria(prev => ({
                    ...prev,
                    rsi: { ...prev.rsi, max: parseFloat(e.target.value) || 100 }
                  }))}
                  className="cyber-input flex-1 py-2 text-sm rounded-xl"
                  min="0"
                  max="100"
                />
              </div>
            </div>
            
            {/* Sector */}
            <div>
              <label className="block text-sm font-medium text-slate-300 mb-2">Sector</label>
              <select
                value={criteria.sector}
                onChange={(e) => setCriteria(prev => ({ ...prev, sector: e.target.value }))}
                className="cyber-input w-full py-2 text-sm rounded-xl"
              >
                <option value="all">All Sectors</option>
                <option value="Banking">Banking</option>
                <option value="IT Services">IT Services</option>
                <option value="Oil & Gas">Oil & Gas</option>
                <option value="Automobile">Automobile</option>
                <option value="Infrastructure">Infrastructure</option>
                <option value="Financial Services">Financial Services</option>
              </select>
            </div>
            
            {/* Market Cap */}
            <div>
              <label className="block text-sm font-medium text-slate-300 mb-2">Market Cap</label>
              <select
                value={criteria.marketCap}
                onChange={(e) => setCriteria(prev => ({ ...prev, marketCap: e.target.value as any }))}
                className="cyber-input w-full py-2 text-sm rounded-xl"
              >
                <option value="any">Any Size</option>
                <option value="large">Large Cap (Above Rs.20K Cr)</option>
                <option value="mid">Mid Cap (Rs.5K-20K Cr)</option>
                <option value="small">Small Cap (Below Rs.5K Cr)</option>
              </select>
            </div>
            
            {/* Sort By */}
            <div>
              <label className="block text-sm font-medium text-slate-300 mb-2">Sort By</label>
              <div className="flex space-x-2">
                <select
                  value={criteria.sortBy}
                  onChange={(e) => setCriteria(prev => ({ ...prev, sortBy: e.target.value as any }))}
                  className="cyber-input flex-1 py-2 text-sm rounded-xl"
                >
                  <option value="changePercent">Change %</option>
                  <option value="volume">Volume</option>
                  <option value="price">Price</option>
                  <option value="rsi">RSI</option>
                </select>
                <select
                  value={criteria.sortOrder}
                  onChange={(e) => setCriteria(prev => ({ ...prev, sortOrder: e.target.value as any }))}
                  className="cyber-input py-2 text-sm rounded-xl"
                >
                  <option value="desc">High to Low</option>
                  <option value="asc">Low to High</option>
                </select>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Results Table */}
      <div className="overflow-x-auto max-h-96 custom-scrollbar">
        {isScanning ? (
          <div className="flex items-center justify-center py-12">
            <div className="flex items-center space-x-3 text-purple-400">
              <div className="loading-dots">
                <div className="loading-dot"></div>
                <div className="loading-dot"></div>
                <div className="loading-dot"></div>
              </div>
              <span>Scanning market data...</span>
            </div>
          </div>
        ) : (
          <div className="space-y-2">
            {/* Table Header */}
            <div className="grid grid-cols-8 gap-4 p-3 bg-slate-800/30 rounded-xl text-sm font-medium text-slate-400 sticky top-0">
              <div>Symbol</div>
              <div className="text-right">Price</div>
              <div className="text-right">Change %</div>
              <div className="text-right">Volume</div>
              <div className="text-right">RSI</div>
              <div className="text-right">P/E</div>
              <div className="text-center">Signal</div>
              <div className="text-center">Actions</div>
            </div>

            {/* Table Rows */}
            {filteredResults.map((stock) => {
              const signal = getTechnicalSignal(stock)
              return (
                <div key={stock.symbol} className="grid grid-cols-8 gap-4 p-3 bg-slate-800/20 rounded-xl hover:bg-slate-700/30 transition-colors text-sm">
                  <div className="flex flex-col">
                    <span className="font-semibold text-white">{stock.symbol}</span>
                    <span className="text-xs text-slate-400 truncate">{stock.sector}</span>
                  </div>
                  
                  <div className="text-right">
                    <div className="font-semibold text-white">Rs.{stock.price.toFixed(2)}</div>
                    <div className="text-xs text-slate-400">{stock.marketCap}</div>
                  </div>
                  
                  <div className={`text-right font-semibold ${
                    stock.changePercent > 0 ? 'text-green-400' : 'text-red-400'
                  }`}>
                    {stock.changePercent > 0 ? '+' : ''}{stock.changePercent.toFixed(2)}%
                  </div>
                  
                  <div className="text-right">
                    <div className="text-white">{(stock.volume / 100000).toFixed(1)}L</div>
                    <div className={`text-xs ${
                      stock.volumeRatio > 1.5 ? 'text-green-400' :
                      stock.volumeRatio < 0.8 ? 'text-red-400' : 'text-slate-400'
                    }`}>
                      {stock.volumeRatio.toFixed(1)}x
                    </div>
                  </div>
                  
                  <div className={`text-right font-semibold ${getRSIColor(stock.rsi)}`}>
                    {stock.rsi.toFixed(1)}
                  </div>
                  
                  <div className="text-right text-white">
                    {stock.pe.toFixed(1)}
                  </div>
                  
                  <div className="text-center">
                    <span className={`px-2 py-1 rounded text-xs font-bold ${signal.bg} ${signal.color}`}>
                      {signal.signal}
                    </span>
                  </div>
                  
                  <div className="flex items-center justify-center space-x-1">
                    <button
                      onClick={() => setSelectedStock(stock)}
                      className="p-1.5 rounded-lg hover:bg-slate-600/50 text-slate-400 hover:text-white transition-colors"
                      title="View details"
                    >
                      <Eye className="w-4 h-4" />
                    </button>
                    <button
                      onClick={() => console.log('Add to watchlist:', stock.symbol)}
                      className="p-1.5 rounded-lg hover:bg-yellow-500/20 text-slate-400 hover:text-yellow-400 transition-colors"
                      title="Add to watchlist"
                    >
                      <Star className="w-4 h-4" />
                    </button>
                    <button
                      onClick={() => console.log('Trade:', stock.symbol)}
                      className="p-1.5 rounded-lg hover:bg-green-500/20 text-slate-400 hover:text-green-400 transition-colors"
                      title="Trade"
                    >
                      <BarChart3 className="w-4 h-4" />
                    </button>
                  </div>
                </div>
              )
            })}
            
            {filteredResults.length === 0 && (
              <div className="text-center py-8">
                <Search className="w-12 h-12 text-slate-500 mx-auto mb-4" />
                <p className="text-slate-400">No stocks match your criteria</p>
                <p className="text-slate-500 text-sm">Try adjusting your filters</p>
              </div>
            )}
          </div>
        )}
      </div>

      {/* Stock Details Modal */}
      {selectedStock && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50">
          <div className="glass-card rounded-2xl p-6 max-w-2xl w-full max-h-[80vh] overflow-y-auto">
            <div className="flex items-center justify-between mb-6">
              <div>
                <h3 className="text-lg font-bold text-white">{selectedStock.symbol}</h3>
                <p className="text-slate-400">{selectedStock.name}</p>
              </div>
              <button
                onClick={() => setSelectedStock(null)}
                className="p-2 rounded-lg hover:bg-slate-600/50 text-slate-400 hover:text-white"
              >
                ✕
              </button>
            </div>
            
            <div className="grid gap-4 md:grid-cols-2">
              <div>
                <label className="text-sm font-medium text-slate-300">Current Price</label>
                <div className="text-2xl font-bold text-white">Rs.{selectedStock.price.toFixed(2)}</div>
                <div className={`text-sm ${selectedStock.changePercent > 0 ? 'text-green-400' : 'text-red-400'}`}>
                  {selectedStock.changePercent > 0 ? '+' : ''}{selectedStock.change.toFixed(2)} ({selectedStock.changePercent.toFixed(2)}%)
                </div>
              </div>
              
              <div>
                <label className="text-sm font-medium text-slate-300">Technical Signal</label>
                <div className="flex items-center space-x-2">
                  {(() => {
                    const signal = getTechnicalSignal(selectedStock)
                    return (
                      <span className={`px-3 py-1 rounded font-bold ${signal.bg} ${signal.color}`}>
                        {signal.signal}
                      </span>
                    )
                  })()}
                </div>
              </div>
              
              <div>
                <label className="text-sm font-medium text-slate-300">Volume</label>
                <div className="text-white">{(selectedStock.volume / 100000).toFixed(1)}L</div>
                <div className="text-sm text-slate-400">Avg: {(selectedStock.avgVolume / 100000).toFixed(1)}L</div>
              </div>
              
              <div>
                <label className="text-sm font-medium text-slate-300">RSI (14)</label>
                <div className={`text-white ${getRSIColor(selectedStock.rsi)}`}>{selectedStock.rsi.toFixed(1)}</div>
              </div>
              
              <div>
                <label className="text-sm font-medium text-slate-300">P/E Ratio</label>
                <div className="text-white">{selectedStock.pe.toFixed(1)}</div>
              </div>
              
              <div>
                <label className="text-sm font-medium text-slate-300">P/B Ratio</label>
                <div className="text-white">{selectedStock.pb.toFixed(1)}</div>
              </div>
              
              <div>
                <label className="text-sm font-medium text-slate-300">52W High/Low</label>
                <div className="text-white">Rs.{selectedStock.high52w.toFixed(2)} / Rs.{selectedStock.low52w.toFixed(2)}</div>
              </div>
              
              <div>
                <label className="text-sm font-medium text-slate-300">Market Cap</label>
                <div className="text-white">{selectedStock.marketCap}</div>
              </div>
            </div>
            
            <div className="flex space-x-3 mt-6">
              <button className="flex-1 cyber-button py-2 rounded-xl text-sm">
                Add to Watchlist
              </button>
              <button className="flex-1 py-2 px-4 rounded-xl bg-green-500/20 text-green-400 hover:bg-green-500/30 transition-colors">
                Trade Now
              </button>
              <button
                onClick={() => setSelectedStock(null)}
                className="flex-1 glass-card py-2 rounded-xl text-slate-400 hover:text-white transition-colors"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}