import React, { useState, useEffect } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import {
  Search, Filter, TrendingUp, TrendingDown, Star, Target, Zap,
  BarChart3, Activity, Volume2, Clock, Award, AlertTriangle,
  ChevronUp, ChevronDown, Play, Pause, Settings, Download,
  RefreshCw, Save, Plus, Minus, Eye, EyeOff, BookOpen,
  Globe, MapPin, Calendar, CheckCircle, ArrowUpRight, ArrowDownRight
} from 'lucide-react'

// Types for Market Scanner
interface ScanCriteria {
  id: string
  name: string
  description: string
  category: 'TECHNICAL' | 'FUNDAMENTAL' | 'VOLUME' | 'MOMENTUM' | 'VALUE' | 'GROWTH'
  operator: 'GT' | 'LT' | 'EQ' | 'BETWEEN' | 'CROSS_ABOVE' | 'CROSS_BELOW'
  value: number | [number, number]
  unit?: string
  enabled: boolean
}

interface ScanResult {
  symbol: string
  companyName: string
  sector: string
  marketCap: number
  price: number
  change: number
  changePercent: number
  volume: number
  avgVolume: number
  score: number // Match score 0-100
  metrics: {
    technical: {
      rsi: number
      sma20: number
      sma50: number
      macd: number
      bollinger: { upper: number; middle: number; lower: number }
    }
    fundamental: {
      pe: number
      pb: number
      roe: number
      debt: number
      revenue: number
      growth: number
    }
    momentum: {
      priceChange1d: number
      priceChange1w: number
      priceChange1m: number
      volumeRatio: number
    }
  }
  alerts: {
    type: 'BREAKOUT' | 'VOLUME_SURGE' | 'OVERSOLD' | 'OVERBOUGHT' | 'NEWS'
    message: string
    severity: 'LOW' | 'MEDIUM' | 'HIGH'
  }[]
  lastUpdated: Date
}

interface SavedScan {
  id: string
  name: string
  description: string
  criteria: ScanCriteria[]
  category: 'MY_SCANS' | 'PREDEFINED' | 'COMMUNITY'
  author?: string
  popularity?: number
  lastRun?: Date
  resultCount?: number
}

interface ScanPreset {
  id: string
  name: string
  description: string
  category: 'MOMENTUM' | 'VALUE' | 'GROWTH' | 'SWING' | 'INTRADAY'
  criteria: Omit<ScanCriteria, 'id' | 'enabled'>[]
  popularity: number
}

// Mock data
const mockPresets: ScanPreset[] = [
  {
    id: '1',
    name: 'Momentum Breakouts',
    description: 'Stocks breaking above 20-day high with strong volume',
    category: 'MOMENTUM',
    criteria: [
      { name: 'Price vs 20-day High', description: 'Price breaking above 20-day high', category: 'TECHNICAL', operator: 'CROSS_ABOVE', value: 0, unit: '%' },
      { name: 'Volume Surge', description: 'Volume > 150% of average', category: 'VOLUME', operator: 'GT', value: 1.5, unit: 'x' },
      { name: 'RSI', description: 'RSI between 50-80', category: 'TECHNICAL', operator: 'BETWEEN', value: [50, 80] }
    ],
    popularity: 89
  },
  {
    id: '2',
    name: 'Undervalued Growth',
    description: 'Fundamentally strong stocks at attractive valuations',
    category: 'VALUE',
    criteria: [
      { name: 'P/E Ratio', description: 'P/E below industry average', category: 'FUNDAMENTAL', operator: 'LT', value: 20 },
      { name: 'Revenue Growth', description: 'Revenue growth > 15%', category: 'FUNDAMENTAL', operator: 'GT', value: 15, unit: '%' },
      { name: 'ROE', description: 'Return on Equity > 15%', category: 'FUNDAMENTAL', operator: 'GT', value: 15, unit: '%' }
    ],
    popularity: 76
  },
  {
    id: '3',
    name: 'Oversold Bounce',
    description: 'Oversold stocks showing signs of reversal',
    category: 'SWING',
    criteria: [
      { name: 'RSI', description: 'RSI below 30', category: 'TECHNICAL', operator: 'LT', value: 30 },
      { name: 'Support Level', description: 'Price near support level', category: 'TECHNICAL', operator: 'LT', value: 5, unit: '%' },
      { name: 'Volume', description: 'Volume increase', category: 'VOLUME', operator: 'GT', value: 1.2, unit: 'x' }
    ],
    popularity: 68
  }
]

const mockResults: ScanResult[] = [
  {
    symbol: 'RELIANCE',
    companyName: 'Reliance Industries Ltd',
    sector: 'Energy',
    marketCap: 1850000000000, // 18.5L Cr
    price: 2456.75,
    change: 45.80,
    changePercent: 1.90,
    volume: 2340000,
    avgVolume: 1800000,
    score: 92,
    metrics: {
      technical: {
        rsi: 68.5,
        sma20: 2420.30,
        sma50: 2380.75,
        macd: 12.5,
        bollinger: { upper: 2480.0, middle: 2440.0, lower: 2400.0 }
      },
      fundamental: {
        pe: 12.5,
        pb: 1.8,
        roe: 14.2,
        debt: 2.1,
        revenue: 654000,
        growth: 18.5
      },
      momentum: {
        priceChange1d: 1.90,
        priceChange1w: 4.2,
        priceChange1m: 12.8,
        volumeRatio: 1.3
      }
    },
    alerts: [
      { type: 'BREAKOUT', message: 'Price broke above 20-day resistance', severity: 'HIGH' },
      { type: 'VOLUME_SURGE', message: 'Volume 30% above average', severity: 'MEDIUM' }
    ],
    lastUpdated: new Date()
  },
  {
    symbol: 'TCS',
    companyName: 'Tata Consultancy Services',
    sector: 'IT',
    marketCap: 1320000000000,
    price: 3245.80,
    change: -18.40,
    changePercent: -0.56,
    volume: 890000,
    avgVolume: 1200000,
    score: 78,
    metrics: {
      technical: {
        rsi: 45.2,
        sma20: 3280.50,
        sma50: 3310.25,
        macd: -5.8,
        bollinger: { upper: 3320.0, middle: 3280.0, lower: 3240.0 }
      },
      fundamental: {
        pe: 28.5,
        pb: 12.8,
        roe: 45.2,
        debt: 0.1,
        revenue: 234000,
        growth: 8.5
      },
      momentum: {
        priceChange1d: -0.56,
        priceChange1w: -2.1,
        priceChange1m: 5.8,
        volumeRatio: 0.74
      }
    },
    alerts: [
      { type: 'OVERSOLD', message: 'RSI approaching oversold territory', severity: 'MEDIUM' }
    ],
    lastUpdated: new Date()
  },
  {
    symbol: 'HDFC',
    companyName: 'HDFC Bank Limited',
    sector: 'Banking',
    marketCap: 920000000000,
    price: 1678.45,
    change: 12.30,
    changePercent: 0.74,
    volume: 1560000,
    avgVolume: 1400000,
    score: 85,
    metrics: {
      technical: {
        rsi: 58.7,
        sma20: 1665.20,
        sma50: 1642.80,
        macd: 8.2,
        bollinger: { upper: 1690.0, middle: 1665.0, lower: 1640.0 }
      },
      fundamental: {
        pe: 18.5,
        pb: 2.8,
        roe: 16.8,
        debt: 8.9,
        revenue: 89000,
        growth: 12.5
      },
      momentum: {
        priceChange1d: 0.74,
        priceChange1w: 3.2,
        priceChange1m: 8.9,
        volumeRatio: 1.11
      }
    },
    alerts: [
      { type: 'BREAKOUT', message: 'Moving above key resistance', severity: 'MEDIUM' }
    ],
    lastUpdated: new Date()
  }
]

const mockSavedScans: SavedScan[] = [
  {
    id: '1',
    name: 'My Custom Momentum Scan',
    description: 'Custom scan for momentum stocks with specific criteria',
    criteria: [],
    category: 'MY_SCANS',
    lastRun: new Date(Date.now() - 2 * 60 * 60 * 1000),
    resultCount: 23
  },
  {
    id: '2',
    name: 'Large Cap Value Picks',
    description: 'Large cap stocks with value characteristics',
    criteria: [],
    category: 'MY_SCANS',
    lastRun: new Date(Date.now() - 6 * 60 * 60 * 1000),
    resultCount: 15
  }
]

interface MarketScannerProProps {
  className?: string
}

export const MarketScannerPro: React.FC<MarketScannerProProps> = ({
  className = ''
}) => {
  // State management
  const [activeTab, setActiveTab] = useState<'scanner' | 'results' | 'alerts' | 'saved'>('scanner')
  const [selectedPreset, setSelectedPreset] = useState<string | null>(null)
  const [criteria, setCriteria] = useState<ScanCriteria[]>([])
  const [results, setResults] = useState<ScanResult[]>(mockResults)
  const [isScanning, setIsScanning] = useState(false)
  const [autoRefresh, setAutoRefresh] = useState(false)
  
  // Filter and sort
  const [sortBy, setSortBy] = useState<'score' | 'change' | 'volume' | 'marketCap'>('score')
  const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>('desc')
  const [filterSector, setFilterSector] = useState<string>('all')
  const [minScore, setMinScore] = useState(70)
  
  // UI state
  const [showCriteriaBuilder, setShowCriteriaBuilder] = useState(false)
  const [selectedResult, setSelectedResult] = useState<ScanResult | null>(null)

  // Auto-refresh simulation
  useEffect(() => {
    if (!autoRefresh) return

    const interval = setInterval(() => {
      setResults(prev => prev.map(result => ({
        ...result,
        price: result.price + (Math.random() - 0.5) * 10,
        change: (Math.random() - 0.5) * 20,
        changePercent: (Math.random() - 0.5) * 5,
        volume: result.volume + Math.floor((Math.random() - 0.5) * 100000),
        lastUpdated: new Date()
      })))
    }, 5000)

    return () => clearInterval(interval)
  }, [autoRefresh])

  const handlePresetSelect = (preset: ScanPreset) => {
    setSelectedPreset(preset.id)
    setCriteria(preset.criteria.map((c, i) => ({
      ...c,
      id: `${i}`,
      enabled: true
    })))
  }

  const handleScan = async () => {
    setIsScanning(true)
    // Simulate API call
    setTimeout(() => {
      setIsScanning(false)
      setActiveTab('results')
    }, 3000)
  }

  const addCriteria = (newCriteria: Omit<ScanCriteria, 'id' | 'enabled'>) => {
    const criteria: ScanCriteria = {
      ...newCriteria,
      id: Date.now().toString(),
      enabled: true
    }
    setCriteria(prev => [...prev, criteria])
  }

  const removeCriteria = (id: string) => {
    setCriteria(prev => prev.filter(c => c.id !== id))
  }

  const toggleCriteria = (id: string) => {
    setCriteria(prev => prev.map(c => 
      c.id === id ? { ...c, enabled: !c.enabled } : c
    ))
  }

  // Sort and filter results
  const filteredResults = results
    .filter(result => filterSector === 'all' || result.sector === filterSector)
    .filter(result => result.score >= minScore)
    .sort((a, b) => {
      const multiplier = sortOrder === 'asc' ? 1 : -1
      switch (sortBy) {
        case 'score': return (b.score - a.score) * multiplier
        case 'change': return (b.changePercent - a.changePercent) * multiplier
        case 'volume': return (b.volume - a.volume) * multiplier
        case 'marketCap': return (b.marketCap - a.marketCap) * multiplier
        default: return 0
      }
    })

  const getCategoryColor = (category: ScanCriteria['category']) => {
    const colors = {
      TECHNICAL: 'blue',
      FUNDAMENTAL: 'green',
      VOLUME: 'purple',
      MOMENTUM: 'orange',
      VALUE: 'yellow',
      GROWTH: 'cyan'
    }
    return colors[category]
  }

  const getAlertColor = (severity: 'LOW' | 'MEDIUM' | 'HIGH') => {
    const colors = {
      LOW: 'text-blue-400',
      MEDIUM: 'text-yellow-400',
      HIGH: 'text-red-400'
    }
    return colors[severity]
  }

  return (
    <div className={`space-y-6 ${className}`}>
      {/* Header */}
      <div className="glass-card rounded-2xl p-6">
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center space-x-4">
            <div className="w-12 h-12 bg-gradient-to-br from-blue-600 to-purple-600 rounded-xl flex items-center justify-center">
              <Search className="w-6 h-6 text-white" />
            </div>
            <div>
              <h1 className="text-2xl font-bold text-white mb-1">
                Market Scanner Pro
              </h1>
              <p className="text-slate-400">
                Advanced stock screening with technical and fundamental analysis
              </p>
            </div>
          </div>

          <div className="flex items-center space-x-4">
            <div className="flex items-center space-x-2">
              <span className="text-sm text-slate-400">Auto Refresh</span>
              <button
                onClick={() => setAutoRefresh(!autoRefresh)}
                className={`p-1 rounded-lg transition-colors ${
                  autoRefresh ? 'bg-green-600' : 'bg-slate-600'
                }`}
              >
                {autoRefresh ? (
                  <Play className="w-4 h-4 text-white" />
                ) : (
                  <Pause className="w-4 h-4 text-white" />
                )}
              </button>
            </div>
            
            <button
              onClick={handleScan}
              disabled={isScanning || criteria.length === 0}
              className="px-6 py-2 bg-gradient-to-r from-blue-600 to-purple-600 text-white rounded-lg font-medium hover:from-blue-700 hover:to-purple-700 transition-all disabled:opacity-50 disabled:cursor-not-allowed flex items-center space-x-2"
            >
              {isScanning ? (
                <>
                  <RefreshCw className="w-4 h-4 animate-spin" />
                  <span>Scanning...</span>
                </>
              ) : (
                <>
                  <Search className="w-4 h-4" />
                  <span>Run Scan</span>
                </>
              )}
            </button>
          </div>
        </div>

        {/* Quick Stats */}
        <div className="grid grid-cols-4 gap-6">
          <div className="text-center">
            <div className="text-2xl font-bold text-white mb-1">
              {results.length}
            </div>
            <p className="text-sm text-slate-400">Total Results</p>
          </div>
          
          <div className="text-center">
            <div className="text-2xl font-bold text-green-400 mb-1">
              {results.filter(r => r.changePercent > 0).length}
            </div>
            <p className="text-sm text-slate-400">Gainers</p>
          </div>

          <div className="text-center">
            <div className="text-2xl font-bold text-blue-400 mb-1">
              {criteria.filter(c => c.enabled).length}
            </div>
            <p className="text-sm text-slate-400">Active Criteria</p>
          </div>

          <div className="text-center">
            <div className="text-2xl font-bold text-purple-400 mb-1">
              {results.filter(r => r.alerts.some(a => a.severity === 'HIGH')).length}
            </div>
            <p className="text-sm text-slate-400">High Alerts</p>
          </div>
        </div>
      </div>

      {/* Navigation Tabs */}
      <div className="glass-card rounded-2xl p-6">
        <div className="flex items-center justify-between mb-6">
          <div className="flex space-x-1">
            {[
              { id: 'scanner', label: 'Scanner Builder', icon: Settings },
              { id: 'results', label: 'Scan Results', icon: BarChart3 },
              { id: 'alerts', label: 'Smart Alerts', icon: AlertTriangle },
              { id: 'saved', label: 'Saved Scans', icon: BookOpen }
            ].map(({ id, label, icon: Icon }) => (
              <button
                key={id}
                onClick={() => setActiveTab(id as typeof activeTab)}
                className={`px-4 py-2 rounded-xl flex items-center space-x-2 transition-all ${
                  activeTab === id
                    ? 'bg-gradient-to-r from-blue-600 to-purple-600 text-white'
                    : 'text-slate-400 hover:text-white hover:bg-slate-800/50'
                }`}
              >
                <Icon className="w-4 h-4" />
                <span className="text-sm font-medium">{label}</span>
              </button>
            ))}
          </div>
        </div>

        {/* Tab Content */}
        <div className="mt-6">
          <AnimatePresence mode="wait">
            {/* Scanner Builder Tab */}
            {activeTab === 'scanner' && (
              <motion.div
                key="scanner"
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -20 }}
                className="space-y-6"
              >
                {/* Preset Templates */}
                <div className="bg-slate-800/30 rounded-xl p-6">
                  <h3 className="font-bold text-white mb-4 flex items-center space-x-2">
                    <Star className="w-5 h-5 text-yellow-400" />
                    <span>Popular Scan Templates</span>
                  </h3>
                  
                  <div className="grid grid-cols-3 gap-4">
                    {mockPresets.map((preset) => (
                      <button
                        key={preset.id}
                        onClick={() => handlePresetSelect(preset)}
                        className={`p-4 rounded-xl text-left transition-all border ${
                          selectedPreset === preset.id
                            ? 'bg-blue-600/20 border-blue-600/50'
                            : 'bg-slate-700/30 border-slate-600/30 hover:border-slate-500/50'
                        }`}
                      >
                        <div className="flex items-center justify-between mb-2">
                          <h4 className="font-semibold text-white">{preset.name}</h4>
                          <div className="flex items-center space-x-1 text-xs text-slate-400">
                            <Star className="w-3 h-3" />
                            <span>{preset.popularity}</span>
                          </div>
                        </div>
                        
                        <p className="text-sm text-slate-400 mb-3">{preset.description}</p>
                        
                        <div className="flex items-center space-x-2">
                          <span className={`px-2 py-1 bg-${getCategoryColor(preset.category)}-600/20 text-${getCategoryColor(preset.category)}-400 rounded-lg text-xs`}>
                            {preset.category}
                          </span>
                          <span className="text-xs text-slate-500">
                            {preset.criteria.length} criteria
                          </span>
                        </div>
                      </button>
                    ))}
                  </div>
                </div>

                {/* Custom Criteria Builder */}
                <div className="bg-slate-800/30 rounded-xl p-6">
                  <div className="flex items-center justify-between mb-4">
                    <h3 className="font-bold text-white flex items-center space-x-2">
                      <Filter className="w-5 h-5 text-blue-400" />
                      <span>Custom Criteria</span>
                    </h3>
                    
                    <button
                      onClick={() => setShowCriteriaBuilder(!showCriteriaBuilder)}
                      className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center space-x-2"
                    >
                      <Plus className="w-4 h-4" />
                      <span>Add Criteria</span>
                    </button>
                  </div>

                  {/* Current Criteria */}
                  {criteria.length > 0 && (
                    <div className="space-y-3 mb-4">
                      {criteria.map((criterion) => (
                        <div
                          key={criterion.id}
                          className={`flex items-center justify-between p-4 rounded-lg border transition-all ${
                            criterion.enabled
                              ? 'bg-slate-700/30 border-slate-600/50'
                              : 'bg-slate-800/30 border-slate-700/30 opacity-50'
                          }`}
                        >
                          <div className="flex items-center space-x-4">
                            <button
                              onClick={() => toggleCriteria(criterion.id)}
                              className={`p-1 rounded transition-colors ${
                                criterion.enabled ? 'text-green-400' : 'text-slate-500'
                              }`}
                            >
                              {criterion.enabled ? <Eye className="w-4 h-4" /> : <EyeOff className="w-4 h-4" />}
                            </button>
                            
                            <div>
                              <div className="flex items-center space-x-2">
                                <span className="font-medium text-white">{criterion.name}</span>
                                <span className={`px-2 py-1 bg-${getCategoryColor(criterion.category)}-600/20 text-${getCategoryColor(criterion.category)}-400 rounded text-xs`}>
                                  {criterion.category}
                                </span>
                              </div>
                              <p className="text-sm text-slate-400">{criterion.description}</p>
                            </div>
                          </div>

                          <div className="flex items-center space-x-4">
                            <div className="text-right">
                              <div className="text-white font-medium">
                                {criterion.operator} {' '}
                                {Array.isArray(criterion.value) 
                                  ? `${criterion.value[0]} - ${criterion.value[1]}`
                                  : criterion.value
                                } {criterion.unit}
                              </div>
                            </div>
                            
                            <button
                              onClick={() => removeCriteria(criterion.id)}
                              className="text-slate-400 hover:text-red-400 transition-colors"
                            >
                              <Minus className="w-4 h-4" />
                            </button>
                          </div>
                        </div>
                      ))}
                    </div>
                  )}

                  {criteria.length === 0 && (
                    <div className="text-center py-8">
                      <Filter className="w-12 h-12 text-slate-400 mx-auto mb-4" />
                      <h4 className="font-semibold text-white mb-2">No Criteria Set</h4>
                      <p className="text-slate-400 mb-4">
                        Select a preset template or add custom criteria to start scanning
                      </p>
                    </div>
                  )}
                </div>
              </motion.div>
            )}

            {/* Scan Results Tab */}
            {activeTab === 'results' && (
              <motion.div
                key="results"
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -20 }}
                className="space-y-4"
              >
                {/* Filters and Controls */}
                <div className="flex items-center justify-between bg-slate-800/30 rounded-xl p-4">
                  <div className="flex items-center space-x-4">
                    <div className="flex items-center space-x-2">
                      <span className="text-sm text-slate-400">Sort by:</span>
                      <select
                        value={sortBy}
                        onChange={(e) => setSortBy(e.target.value as typeof sortBy)}
                        className="bg-slate-700 text-white rounded-lg px-3 py-1 text-sm border border-slate-600"
                      >
                        <option value="score">Match Score</option>
                        <option value="change">Price Change</option>
                        <option value="volume">Volume</option>
                        <option value="marketCap">Market Cap</option>
                      </select>
                      
                      <button
                        onClick={() => setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc')}
                        className="p-1 text-slate-400 hover:text-white transition-colors"
                      >
                        {sortOrder === 'asc' ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
                      </button>
                    </div>

                    <div className="flex items-center space-x-2">
                      <span className="text-sm text-slate-400">Min Score:</span>
                      <input
                        type="range"
                        min="0"
                        max="100"
                        value={minScore}
                        onChange={(e) => setMinScore(Number(e.target.value))}
                        className="w-20"
                      />
                      <span className="text-sm text-white w-8">{minScore}</span>
                    </div>
                  </div>

                  <div className="flex items-center space-x-2">
                    <button className="p-2 bg-slate-700 hover:bg-slate-600 rounded-lg transition-colors">
                      <Download className="w-4 h-4 text-white" />
                    </button>
                    <button className="p-2 bg-slate-700 hover:bg-slate-600 rounded-lg transition-colors">
                      <Save className="w-4 h-4 text-white" />
                    </button>
                  </div>
                </div>

                {/* Results List */}
                <div className="space-y-3">
                  {filteredResults.map((result, index) => (
                    <div
                      key={result.symbol}
                      className="bg-slate-800/30 rounded-xl p-6 border border-slate-700/30 hover:border-slate-600/50 transition-all cursor-pointer"
                      onClick={() => setSelectedResult(result)}
                    >
                      <div className="flex items-center justify-between">
                        <div className="flex items-center space-x-4">
                          <div className="text-center">
                            <div className="text-lg font-bold text-white">#{index + 1}</div>
                            <div className="text-xs text-slate-400">Rank</div>
                          </div>
                          
                          <div>
                            <div className="flex items-center space-x-2 mb-1">
                              <h3 className="font-bold text-white text-lg">{result.symbol}</h3>
                              <span className="px-2 py-1 bg-slate-700/50 rounded-lg text-xs text-slate-400">
                                {result.sector}
                              </span>
                            </div>
                            <p className="text-slate-400 text-sm">{result.companyName}</p>
                            <div className="text-xs text-slate-500">
                              Market Cap: ₹{(result.marketCap / 10000000).toFixed(1)}K Cr
                            </div>
                          </div>
                        </div>

                        <div className="grid grid-cols-4 gap-6 text-center">
                          <div>
                            <div className="text-xl font-bold text-white mb-1">
                              ₹{result.price.toLocaleString()}
                            </div>
                            <div className={`text-sm font-medium ${
                              result.changePercent >= 0 ? 'text-green-400' : 'text-red-400'
                            }`}>
                              {result.changePercent >= 0 ? '+' : ''}{result.changePercent.toFixed(2)}%
                            </div>
                          </div>

                          <div>
                            <div className="text-xl font-bold text-blue-400 mb-1">
                              {result.score}
                            </div>
                            <div className="text-xs text-slate-400">Match Score</div>
                          </div>

                          <div>
                            <div className="text-xl font-bold text-purple-400 mb-1">
                              {(result.volume / 1000).toFixed(0)}K
                            </div>
                            <div className="text-xs text-slate-400">Volume</div>
                            <div className={`text-xs ${
                              result.volume > result.avgVolume ? 'text-green-400' : 'text-slate-400'
                            }`}>
                              {((result.volume / result.avgVolume) * 100).toFixed(0)}% avg
                            </div>
                          </div>

                          <div>
                            <div className="flex items-center space-x-1">
                              {result.alerts.map((alert, i) => (
                                <div
                                  key={i}
                                  className={`w-2 h-2 rounded-full ${
                                    alert.severity === 'HIGH' ? 'bg-red-400' :
                                    alert.severity === 'MEDIUM' ? 'bg-yellow-400' :
                                    'bg-blue-400'
                                  }`}
                                  title={alert.message}
                                />
                              ))}
                            </div>
                            <div className="text-xs text-slate-400 mt-1">
                              {result.alerts.length} alerts
                            </div>
                          </div>
                        </div>
                      </div>

                      {/* Quick Metrics */}
                      <div className="mt-4 pt-4 border-t border-slate-700/30 grid grid-cols-6 gap-4 text-center text-sm">
                        <div>
                          <div className="text-white font-medium">
                            {result.metrics.technical.rsi.toFixed(1)}
                          </div>
                          <div className="text-slate-400 text-xs">RSI</div>
                        </div>
                        <div>
                          <div className="text-white font-medium">
                            {result.metrics.fundamental.pe.toFixed(1)}
                          </div>
                          <div className="text-slate-400 text-xs">P/E</div>
                        </div>
                        <div>
                          <div className="text-white font-medium">
                            {result.metrics.fundamental.roe.toFixed(1)}%
                          </div>
                          <div className="text-slate-400 text-xs">ROE</div>
                        </div>
                        <div>
                          <div className={`font-medium ${
                            result.metrics.momentum.priceChange1w >= 0 ? 'text-green-400' : 'text-red-400'
                          }`}>
                            {result.metrics.momentum.priceChange1w >= 0 ? '+' : ''}{result.metrics.momentum.priceChange1w.toFixed(1)}%
                          </div>
                          <div className="text-slate-400 text-xs">1W Change</div>
                        </div>
                        <div>
                          <div className={`font-medium ${
                            result.metrics.momentum.priceChange1m >= 0 ? 'text-green-400' : 'text-red-400'
                          }`}>
                            {result.metrics.momentum.priceChange1m >= 0 ? '+' : ''}{result.metrics.momentum.priceChange1m.toFixed(1)}%
                          </div>
                          <div className="text-slate-400 text-xs">1M Change</div>
                        </div>
                        <div>
                          <div className="text-white font-medium">
                            {result.metrics.momentum.volumeRatio.toFixed(2)}x
                          </div>
                          <div className="text-slate-400 text-xs">Vol Ratio</div>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </motion.div>
            )}

            {/* Smart Alerts Tab */}
            {activeTab === 'alerts' && (
              <motion.div
                key="alerts"
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -20 }}
                className="text-center py-12"
              >
                <AlertTriangle className="w-16 h-16 text-slate-400 mx-auto mb-4" />
                <h3 className="text-xl font-semibold text-white mb-2">
                  Smart Alert System
                </h3>
                <p className="text-slate-400 max-w-md mx-auto">
                  Set up intelligent alerts based on your scan criteria and market conditions.
                </p>
              </motion.div>
            )}

            {/* Saved Scans Tab */}
            {activeTab === 'saved' && (
              <motion.div
                key="saved"
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -20 }}
                className="space-y-4"
              >
                {mockSavedScans.map((scan) => (
                  <div
                    key={scan.id}
                    className="bg-slate-800/30 rounded-xl p-6 border border-slate-700/30 hover:border-slate-600/50 transition-all"
                  >
                    <div className="flex items-center justify-between">
                      <div>
                        <div className="flex items-center space-x-2 mb-2">
                          <h3 className="font-bold text-white">{scan.name}</h3>
                          <span className={`px-2 py-1 rounded-lg text-xs ${
                            scan.category === 'MY_SCANS' ? 'bg-blue-600/20 text-blue-400' :
                            scan.category === 'PREDEFINED' ? 'bg-green-600/20 text-green-400' :
                            'bg-purple-600/20 text-purple-400'
                          }`}>
                            {scan.category.replace('_', ' ')}
                          </span>
                        </div>
                        <p className="text-slate-400 text-sm mb-2">{scan.description}</p>
                        
                        {scan.lastRun && (
                          <div className="flex items-center space-x-4 text-xs text-slate-500">
                            <div className="flex items-center space-x-1">
                              <Clock className="w-3 h-3" />
                              <span>Last run: {scan.lastRun.toLocaleString()}</span>
                            </div>
                            {scan.resultCount && (
                              <div className="flex items-center space-x-1">
                                <Target className="w-3 h-3" />
                                <span>{scan.resultCount} results</span>
                              </div>
                            )}
                          </div>
                        )}
                      </div>

                      <div className="flex items-center space-x-2">
                        <button className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors">
                          Run Scan
                        </button>
                        <button className="p-2 bg-slate-700 text-white rounded-lg hover:bg-slate-600 transition-colors">
                          <Settings className="w-4 h-4" />
                        </button>
                      </div>
                    </div>
                  </div>
                ))}
              </motion.div>
            )}
          </AnimatePresence>
        </div>
      </div>

      {/* Result Detail Modal */}
      <AnimatePresence>
        {selectedResult && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 bg-black/50 backdrop-blur-sm z-50 flex items-center justify-center p-4"
            onClick={() => setSelectedResult(null)}
          >
            <motion.div
              initial={{ scale: 0.9, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.9, opacity: 0 }}
              className="bg-slate-900 rounded-2xl p-6 max-w-4xl w-full max-h-[90vh] overflow-y-auto"
              onClick={(e) => e.stopPropagation()}
            >
              <div className="flex items-center justify-between mb-6">
                <div>
                  <h2 className="text-2xl font-bold text-white">
                    {selectedResult.symbol} - {selectedResult.companyName}
                  </h2>
                  <p className="text-slate-400">
                    {selectedResult.sector} • Match Score: {selectedResult.score}/100
                  </p>
                </div>
                <button
                  onClick={() => setSelectedResult(null)}
                  className="p-2 text-slate-400 hover:text-white transition-colors"
                >
                  ✕
                </button>
              </div>

              <div className="grid grid-cols-3 gap-6">
                {/* Technical Metrics */}
                <div className="bg-slate-800/30 rounded-xl p-4">
                  <h3 className="font-semibold text-white mb-4">Technical Analysis</h3>
                  <div className="space-y-3">
                    <div className="flex justify-between">
                      <span className="text-slate-400">RSI</span>
                      <span className="text-white">{selectedResult.metrics.technical.rsi.toFixed(1)}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-slate-400">SMA 20</span>
                      <span className="text-white">₹{selectedResult.metrics.technical.sma20.toFixed(2)}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-slate-400">SMA 50</span>
                      <span className="text-white">₹{selectedResult.metrics.technical.sma50.toFixed(2)}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-slate-400">MACD</span>
                      <span className={`${selectedResult.metrics.technical.macd >= 0 ? 'text-green-400' : 'text-red-400'}`}>
                        {selectedResult.metrics.technical.macd.toFixed(2)}
                      </span>
                    </div>
                  </div>
                </div>

                {/* Fundamental Metrics */}
                <div className="bg-slate-800/30 rounded-xl p-4">
                  <h3 className="font-semibold text-white mb-4">Fundamentals</h3>
                  <div className="space-y-3">
                    <div className="flex justify-between">
                      <span className="text-slate-400">P/E Ratio</span>
                      <span className="text-white">{selectedResult.metrics.fundamental.pe.toFixed(1)}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-slate-400">P/B Ratio</span>
                      <span className="text-white">{selectedResult.metrics.fundamental.pb.toFixed(1)}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-slate-400">ROE</span>
                      <span className="text-white">{selectedResult.metrics.fundamental.roe.toFixed(1)}%</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-slate-400">Revenue Growth</span>
                      <span className="text-green-400">{selectedResult.metrics.fundamental.growth.toFixed(1)}%</span>
                    </div>
                  </div>
                </div>

                {/* Alerts & Momentum */}
                <div className="bg-slate-800/30 rounded-xl p-4">
                  <h3 className="font-semibold text-white mb-4">Alerts & Momentum</h3>
                  <div className="space-y-3">
                    {selectedResult.alerts.map((alert, index) => (
                      <div key={index} className="flex items-start space-x-2">
                        <div className={`w-2 h-2 rounded-full mt-2 ${
                          alert.severity === 'HIGH' ? 'bg-red-400' :
                          alert.severity === 'MEDIUM' ? 'bg-yellow-400' :
                          'bg-blue-400'
                        }`} />
                        <div>
                          <div className={`text-sm font-medium ${getAlertColor(alert.severity)}`}>
                            {alert.type.replace('_', ' ')}
                          </div>
                          <div className="text-xs text-slate-400">{alert.message}</div>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  )
}

export default MarketScannerPro