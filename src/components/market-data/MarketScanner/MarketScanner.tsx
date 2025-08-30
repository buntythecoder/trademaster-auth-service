// Market Scanner & Screener Interface
// FRONT-003: Real-time Market Data Enhancement

import React, { useState, useEffect, useCallback, useMemo } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import {
  Search,
  Filter,
  TrendingUp,
  TrendingDown,
  BarChart3,
  Zap,
  Target,
  Star,
  RefreshCw,
  Download,
  Eye,
  Plus,
  Minus,
  ChevronDown,
  ChevronUp,
  AlertCircle,
  Calendar,
  DollarSign,
  Activity,
  Volume2
} from 'lucide-react'
import { useRealTimeMarketData } from '../../../hooks/useRealTimeMarketData'

export interface ScannerFilter {
  id: string
  name: string
  category: 'price' | 'volume' | 'technical' | 'fundamental'
  operator: 'greater' | 'less' | 'between' | 'equals'
  value: number | [number, number]
  enabled: boolean
}

export interface ScanResult {
  symbol: string
  name: string
  price: number
  change: number
  changePercent: number
  volume: number
  marketCap: number
  pe: number
  score: number
  signals: string[]
  sector: string
  lastUpdated: Date
}

export interface ScanPreset {
  id: string
  name: string
  description: string
  filters: ScannerFilter[]
  category: 'momentum' | 'value' | 'growth' | 'breakout' | 'custom'
}

const defaultPresets: ScanPreset[] = [
  {
    id: 'high-momentum',
    name: 'High Momentum',
    description: 'Stocks with strong upward momentum and high volume',
    category: 'momentum',
    filters: [
      {
        id: '1',
        name: 'Price Change %',
        category: 'price',
        operator: 'greater',
        value: 5,
        enabled: true
      },
      {
        id: '2',
        name: 'Volume Ratio',
        category: 'volume',
        operator: 'greater',
        value: 1.5,
        enabled: true
      }
    ]
  },
  {
    id: 'value-picks',
    name: 'Value Picks',
    description: 'Undervalued stocks with good fundamentals',
    category: 'value',
    filters: [
      {
        id: '3',
        name: 'P/E Ratio',
        category: 'fundamental',
        operator: 'less',
        value: 15,
        enabled: true
      },
      {
        id: '4',
        name: 'Price Change %',
        category: 'price',
        operator: 'between',
        value: [-2, 2],
        enabled: true
      }
    ]
  },
  {
    id: 'breakout-stocks',
    name: 'Breakout Candidates',
    description: 'Stocks showing breakout patterns with volume confirmation',
    category: 'breakout',
    filters: [
      {
        id: '5',
        name: '52W High %',
        category: 'technical',
        operator: 'greater',
        value: 90,
        enabled: true
      },
      {
        id: '6',
        name: 'Volume Spike',
        category: 'volume',
        operator: 'greater',
        value: 2,
        enabled: true
      }
    ]
  }
]

const mockScanResults: ScanResult[] = [
  {
    symbol: 'RELIANCE',
    name: 'Reliance Industries Ltd',
    price: 2456.75,
    change: 34.50,
    changePercent: 1.42,
    volume: 2847293,
    marketCap: 1664000,
    pe: 12.5,
    score: 85,
    signals: ['Bullish', 'High Volume'],
    sector: 'Oil & Gas',
    lastUpdated: new Date()
  },
  {
    symbol: 'TCS',
    name: 'Tata Consultancy Services',
    price: 3789.40,
    change: -42.15,
    changePercent: -1.10,
    volume: 1583647,
    marketCap: 1379000,
    pe: 28.3,
    score: 72,
    signals: ['Support Level'],
    sector: 'IT Services',
    lastUpdated: new Date()
  },
  {
    symbol: 'HDFCBANK',
    name: 'HDFC Bank Ltd',
    price: 1687.25,
    change: 18.90,
    changePercent: 1.13,
    volume: 3847392,
    marketCap: 932000,
    pe: 18.7,
    score: 78,
    signals: ['Momentum', 'Volume Spike'],
    sector: 'Banking',
    lastUpdated: new Date()
  },
  {
    symbol: 'INFY',
    name: 'Infosys Ltd',
    price: 1456.80,
    change: -8.45,
    changePercent: -0.58,
    volume: 2947583,
    marketCap: 608000,
    pe: 24.1,
    score: 68,
    signals: ['Oversold'],
    sector: 'IT Services',
    lastUpdated: new Date()
  }
]

interface MarketScannerProps {
  className?: string
}

const FilterCard: React.FC<{
  filter: ScannerFilter
  onUpdate: (filter: ScannerFilter) => void
  onRemove: () => void
}> = ({ filter, onUpdate, onRemove }) => {
  const [isExpanded, setIsExpanded] = useState(false)

  const categoryColors = {
    price: 'text-green-400 bg-green-500/10',
    volume: 'text-blue-400 bg-blue-500/10',
    technical: 'text-purple-400 bg-purple-500/10',
    fundamental: 'text-orange-400 bg-orange-500/10'
  }

  return (
    <motion.div
      className="bg-slate-800/30 rounded-xl p-4 border border-slate-700/50"
      initial={{ opacity: 0, scale: 0.95 }}
      animate={{ opacity: 1, scale: 1 }}
      exit={{ opacity: 0, scale: 0.95 }}
      transition={{ duration: 0.2 }}
    >
      <div className="flex items-center justify-between">
        <div className="flex items-center space-x-3">
          <button
            onClick={() => onUpdate({ ...filter, enabled: !filter.enabled })}
            className={`w-5 h-5 rounded border-2 transition-colors ${
              filter.enabled 
                ? 'bg-purple-500 border-purple-500' 
                : 'border-slate-500 hover:border-slate-400'
            }`}
          />
          
          <div>
            <h4 className="text-sm font-medium text-white">{filter.name}</h4>
            <span className={`text-xs px-2 py-1 rounded-full ${categoryColors[filter.category]}`}>
              {filter.category}
            </span>
          </div>
        </div>
        
        <div className="flex items-center space-x-2">
          <button
            onClick={() => setIsExpanded(!isExpanded)}
            className="p-1 rounded text-slate-400 hover:text-white transition-colors"
          >
            {isExpanded ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
          </button>
          
          <button
            onClick={onRemove}
            className="p-1 rounded text-slate-400 hover:text-red-400 transition-colors"
          >
            <Minus className="w-4 h-4" />
          </button>
        </div>
      </div>
      
      <AnimatePresence>
        {isExpanded && (
          <motion.div
            className="mt-4 pt-4 border-t border-slate-700/50"
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: 'auto' }}
            exit={{ opacity: 0, height: 0 }}
            transition={{ duration: 0.2 }}
          >
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="text-xs text-slate-400 mb-2 block">Operator</label>
                <select
                  value={filter.operator}
                  onChange={(e) => onUpdate({ 
                    ...filter, 
                    operator: e.target.value as ScannerFilter['operator'] 
                  })}
                  className="w-full bg-slate-700 border border-slate-600 rounded-lg px-3 py-2 text-sm text-white focus:ring-2 focus:ring-purple-500"
                >
                  <option value="greater">Greater than</option>
                  <option value="less">Less than</option>
                  <option value="between">Between</option>
                  <option value="equals">Equals</option>
                </select>
              </div>
              
              <div>
                <label className="text-xs text-slate-400 mb-2 block">Value</label>
                {filter.operator === 'between' ? (
                  <div className="flex space-x-2">
                    <input
                      type="number"
                      value={Array.isArray(filter.value) ? filter.value[0] : 0}
                      onChange={(e) => {
                        const currentValue = Array.isArray(filter.value) ? filter.value : [0, 0]
                        onUpdate({ 
                          ...filter, 
                          value: [parseFloat(e.target.value), currentValue[1]] 
                        })
                      }}
                      className="flex-1 bg-slate-700 border border-slate-600 rounded-lg px-2 py-2 text-sm text-white focus:ring-2 focus:ring-purple-500"
                      placeholder="Min"
                    />
                    <input
                      type="number"
                      value={Array.isArray(filter.value) ? filter.value[1] : 0}
                      onChange={(e) => {
                        const currentValue = Array.isArray(filter.value) ? filter.value : [0, 0]
                        onUpdate({ 
                          ...filter, 
                          value: [currentValue[0], parseFloat(e.target.value)] 
                        })
                      }}
                      className="flex-1 bg-slate-700 border border-slate-600 rounded-lg px-2 py-2 text-sm text-white focus:ring-2 focus:ring-purple-500"
                      placeholder="Max"
                    />
                  </div>
                ) : (
                  <input
                    type="number"
                    value={Array.isArray(filter.value) ? filter.value[0] : filter.value}
                    onChange={(e) => onUpdate({ 
                      ...filter, 
                      value: parseFloat(e.target.value) 
                    })}
                    className="w-full bg-slate-700 border border-slate-600 rounded-lg px-3 py-2 text-sm text-white focus:ring-2 focus:ring-purple-500"
                  />
                )}
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </motion.div>
  )
}

const ScanResultCard: React.FC<{
  result: ScanResult
  onWatchlist: () => void
  onAnalyze: () => void
}> = ({ result, onWatchlist, onAnalyze }) => {
  const isPositive = result.change >= 0

  return (
    <motion.div
      className="bg-slate-800/30 rounded-xl p-4 border border-slate-700/50 hover:bg-slate-700/20 transition-colors"
      whileHover={{ scale: 1.02 }}
      transition={{ duration: 0.2 }}
    >
      <div className="flex items-start justify-between mb-3">
        <div>
          <div className="flex items-center space-x-2">
            <h4 className="text-lg font-bold text-white">{result.symbol}</h4>
            <div className={`px-2 py-1 rounded-full text-xs font-medium ${
              result.score >= 80 ? 'bg-green-500/20 text-green-400' :
              result.score >= 60 ? 'bg-yellow-500/20 text-yellow-400' :
              'bg-red-500/20 text-red-400'
            }`}>
              {result.score}
            </div>
          </div>
          <p className="text-sm text-slate-400 truncate">{result.name}</p>
          <span className="text-xs text-slate-500">{result.sector}</span>
        </div>
        
        <div className="text-right">
          <div className="text-xl font-bold text-white">
            ₹{result.price.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
          </div>
          <div className={`text-sm font-medium ${
            isPositive ? 'text-green-400' : 'text-red-400'
          }`}>
            {isPositive ? '+' : ''}₹{result.change.toFixed(2)} ({result.changePercent.toFixed(2)}%)
          </div>
        </div>
      </div>
      
      <div className="grid grid-cols-3 gap-4 mb-4 text-sm">
        <div>
          <div className="text-slate-400">Volume</div>
          <div className="text-white font-mono">
            {(result.volume / 1000).toFixed(1)}K
          </div>
        </div>
        <div>
          <div className="text-slate-400">Market Cap</div>
          <div className="text-white font-mono">
            ₹{(result.marketCap / 1000).toFixed(1)}K Cr
          </div>
        </div>
        <div>
          <div className="text-slate-400">P/E</div>
          <div className="text-white font-mono">
            {result.pe.toFixed(1)}
          </div>
        </div>
      </div>
      
      <div className="flex flex-wrap gap-2 mb-4">
        {result.signals.map((signal, index) => (
          <span
            key={index}
            className={`px-2 py-1 rounded-full text-xs font-medium ${
              signal === 'Bullish' || signal === 'Momentum' ? 'bg-green-500/20 text-green-400' :
              signal === 'Bearish' ? 'bg-red-500/20 text-red-400' :
              signal === 'High Volume' || signal === 'Volume Spike' ? 'bg-blue-500/20 text-blue-400' :
              'bg-purple-500/20 text-purple-400'
            }`}
          >
            {signal}
          </span>
        ))}
      </div>
      
      <div className="flex items-center justify-between">
        <div className="text-xs text-slate-500">
          Updated: {result.lastUpdated.toLocaleTimeString()}
        </div>
        
        <div className="flex items-center space-x-2">
          <button
            onClick={onWatchlist}
            className="p-2 rounded-lg bg-slate-700/50 text-slate-400 hover:text-yellow-400 hover:bg-slate-600/50 transition-all duration-200"
            title="Add to Watchlist"
          >
            <Star className="w-4 h-4" />
          </button>
          
          <button
            onClick={onAnalyze}
            className="p-2 rounded-lg bg-purple-500/20 text-purple-400 hover:bg-purple-500/30 transition-all duration-200"
            title="Analyze"
          >
            <Eye className="w-4 h-4" />
          </button>
        </div>
      </div>
    </motion.div>
  )
}

export const MarketScanner: React.FC<MarketScannerProps> = ({ className = '' }) => {
  const [selectedPreset, setSelectedPreset] = useState<ScanPreset>(defaultPresets[0])
  const [customFilters, setCustomFilters] = useState<ScannerFilter[]>(selectedPreset.filters)
  const [scanResults, setScanResults] = useState<ScanResult[]>(mockScanResults)
  const [isScanning, setIsScanning] = useState(false)
  const [sortBy, setSortBy] = useState<keyof ScanResult>('score')
  const [sortDirection, setSortDirection] = useState<'asc' | 'desc'>('desc')
  const [showFilters, setShowFilters] = useState(true)

  // Simulated real-time updates
  useEffect(() => {
    const interval = setInterval(() => {
      setScanResults(prev => prev.map(result => ({
        ...result,
        price: result.price * (0.99 + Math.random() * 0.02),
        change: (Math.random() - 0.5) * 10,
        changePercent: (Math.random() - 0.5) * 2,
        volume: result.volume * (0.8 + Math.random() * 0.4),
        score: Math.max(0, Math.min(100, result.score + (Math.random() - 0.5) * 5)),
        lastUpdated: new Date()
      })))
    }, 5000)

    return () => clearInterval(interval)
  }, [])

  const runScan = useCallback(() => {
    setIsScanning(true)
    
    // Simulate scan delay
    setTimeout(() => {
      // In real implementation, this would call the backend API with filters
      setScanResults([...mockScanResults].sort(() => Math.random() - 0.5))
      setIsScanning(false)
    }, 2000)
  }, [customFilters])

  const sortedResults = useMemo(() => {
    return [...scanResults].sort((a, b) => {
      const aVal = a[sortBy]
      const bVal = b[sortBy]
      
      if (typeof aVal === 'number' && typeof bVal === 'number') {
        return sortDirection === 'desc' ? bVal - aVal : aVal - bVal
      }
      
      const aStr = String(aVal)
      const bStr = String(bVal)
      return sortDirection === 'desc' ? bStr.localeCompare(aStr) : aStr.localeCompare(bStr)
    })
  }, [scanResults, sortBy, sortDirection])

  const addFilter = useCallback(() => {
    const newFilter: ScannerFilter = {
      id: Date.now().toString(),
      name: 'New Filter',
      category: 'price',
      operator: 'greater',
      value: 0,
      enabled: true
    }
    setCustomFilters(prev => [...prev, newFilter])
  }, [])

  const updateFilter = useCallback((id: string, updatedFilter: ScannerFilter) => {
    setCustomFilters(prev => prev.map(f => f.id === id ? updatedFilter : f))
  }, [])

  const removeFilter = useCallback((id: string) => {
    setCustomFilters(prev => prev.filter(f => f.id !== id))
  }, [])

  return (
    <div className={`space-y-6 ${className}`}>
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center space-x-3">
          <Search className="w-6 h-6 text-purple-400" />
          <h2 className="text-2xl font-bold text-white">Market Scanner</h2>
          <span className="text-sm text-slate-400">
            Real-time stock screening
          </span>
        </div>
        
        <div className="flex items-center space-x-3">
          <button
            onClick={() => setShowFilters(!showFilters)}
            className="cyber-button-sm px-4 py-2"
          >
            <Filter className="w-4 h-4 mr-2" />
            {showFilters ? 'Hide Filters' : 'Show Filters'}
          </button>
          
          <button
            onClick={runScan}
            disabled={isScanning}
            className="cyber-button px-6 py-2 disabled:opacity-50"
          >
            {isScanning ? (
              <RefreshCw className="w-4 h-4 mr-2 animate-spin" />
            ) : (
              <Zap className="w-4 h-4 mr-2" />
            )}
            {isScanning ? 'Scanning...' : 'Run Scan'}
          </button>
        </div>
      </div>

      <div className="grid grid-cols-12 gap-6">
        {/* Filters Panel */}
        <AnimatePresence>
          {showFilters && (
            <motion.div
              className="col-span-4"
              initial={{ opacity: 0, x: -100 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: -100 }}
              transition={{ duration: 0.3 }}
            >
              <div className="glass-card rounded-2xl p-6 space-y-6">
                <div className="flex items-center justify-between">
                  <h3 className="text-lg font-semibold text-white">Scan Filters</h3>
                  <button
                    onClick={addFilter}
                    className="cyber-button-sm p-2"
                    title="Add Filter"
                  >
                    <Plus className="w-4 h-4" />
                  </button>
                </div>

                {/* Presets */}
                <div>
                  <h4 className="text-sm font-medium text-slate-300 mb-3">Presets</h4>
                  <div className="space-y-2">
                    {defaultPresets.map(preset => (
                      <button
                        key={preset.id}
                        onClick={() => {
                          setSelectedPreset(preset)
                          setCustomFilters(preset.filters)
                        }}
                        className={`w-full text-left p-3 rounded-lg transition-colors ${
                          selectedPreset.id === preset.id
                            ? 'bg-purple-500/20 border border-purple-500/50'
                            : 'bg-slate-800/30 border border-slate-700/50 hover:bg-slate-700/30'
                        }`}
                      >
                        <div className="font-medium text-white text-sm">{preset.name}</div>
                        <div className="text-xs text-slate-400 mt-1">{preset.description}</div>
                      </button>
                    ))}
                  </div>
                </div>

                {/* Custom Filters */}
                <div>
                  <h4 className="text-sm font-medium text-slate-300 mb-3">Active Filters</h4>
                  <div className="space-y-3">
                    <AnimatePresence>
                      {customFilters.map(filter => (
                        <FilterCard
                          key={filter.id}
                          filter={filter}
                          onUpdate={(updatedFilter) => updateFilter(filter.id, updatedFilter)}
                          onRemove={() => removeFilter(filter.id)}
                        />
                      ))}
                    </AnimatePresence>
                  </div>
                </div>
              </div>
            </motion.div>
          )}
        </AnimatePresence>

        {/* Results Panel */}
        <div className={showFilters ? 'col-span-8' : 'col-span-12'}>
          <div className="glass-card rounded-2xl p-6">
            {/* Results Header */}
            <div className="flex items-center justify-between mb-6">
              <div className="flex items-center space-x-4">
                <h3 className="text-lg font-semibold text-white">
                  Scan Results ({sortedResults.length})
                </h3>
                
                <div className="flex items-center space-x-2">
                  <span className="text-sm text-slate-400">Sort by:</span>
                  <select
                    value={sortBy}
                    onChange={(e) => setSortBy(e.target.value as keyof ScanResult)}
                    className="bg-slate-700 border border-slate-600 rounded px-3 py-1 text-sm text-white focus:ring-2 focus:ring-purple-500"
                  >
                    <option value="score">Score</option>
                    <option value="changePercent">Change %</option>
                    <option value="volume">Volume</option>
                    <option value="marketCap">Market Cap</option>
                    <option value="pe">P/E Ratio</option>
                  </select>
                  
                  <button
                    onClick={() => setSortDirection(prev => prev === 'desc' ? 'asc' : 'desc')}
                    className="p-1 rounded text-slate-400 hover:text-white transition-colors"
                  >
                    {sortDirection === 'desc' ? <ChevronDown className="w-4 h-4" /> : <ChevronUp className="w-4 h-4" />}
                  </button>
                </div>
              </div>
              
              <button
                className="cyber-button-sm px-4 py-2"
                title="Export Results"
              >
                <Download className="w-4 h-4 mr-2" />
                Export
              </button>
            </div>

            {/* Results Grid */}
            {isScanning ? (
              <div className="flex items-center justify-center h-64">
                <div className="text-center">
                  <RefreshCw className="w-8 h-8 mx-auto mb-4 text-purple-400 animate-spin" />
                  <p className="text-slate-400">Scanning market data...</p>
                  <p className="text-sm text-slate-500">This may take a few moments</p>
                </div>
              </div>
            ) : (
              <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
                <AnimatePresence mode="popLayout">
                  {sortedResults.map((result, index) => (
                    <motion.div
                      key={result.symbol}
                      initial={{ opacity: 0, y: 20 }}
                      animate={{ opacity: 1, y: 0 }}
                      exit={{ opacity: 0, y: -20 }}
                      transition={{ duration: 0.3, delay: index * 0.05 }}
                    >
                      <ScanResultCard
                        result={result}
                        onWatchlist={() => console.log('Add to watchlist:', result.symbol)}
                        onAnalyze={() => console.log('Analyze:', result.symbol)}
                      />
                    </motion.div>
                  ))}
                </AnimatePresence>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}

export default MarketScanner