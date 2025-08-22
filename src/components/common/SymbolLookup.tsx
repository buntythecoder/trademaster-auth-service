import React, { useState, useEffect, useRef } from 'react'
import { Search, TrendingUp, TrendingDown, Star, Globe } from 'lucide-react'

interface SymbolData {
  symbol: string
  name: string
  exchange: 'NSE' | 'BSE' | 'MCX'
  sector: string
  price: number
  change: number
  changePercent: number
  volume: string
  marketCap: string
  isFavorite?: boolean
}

interface SymbolLookupProps {
  onSymbolSelect: (symbol: SymbolData) => void
  placeholder?: string
  value?: string
  className?: string
  showDetails?: boolean
  maxResults?: number
  exchanges?: ('NSE' | 'BSE' | 'MCX')[]
}

// Mock symbol database - in real app this would come from API
const mockSymbols: SymbolData[] = [
  {
    symbol: 'RELIANCE',
    name: 'Reliance Industries Limited',
    exchange: 'NSE',
    sector: 'Oil & Gas',
    price: 2547.30,
    change: 23.45,
    changePercent: 0.93,
    volume: '1.2M',
    marketCap: '17.2L Cr',
    isFavorite: true
  },
  {
    symbol: 'TCS',
    name: 'Tata Consultancy Services',
    exchange: 'NSE',
    sector: 'IT Services',
    price: 3642.80,
    change: -18.90,
    changePercent: -0.52,
    volume: '890K',
    marketCap: '13.4L Cr',
    isFavorite: true
  },
  {
    symbol: 'HDFCBANK',
    name: 'HDFC Bank Limited',
    exchange: 'NSE',
    sector: 'Banking',
    price: 1567.25,
    change: 12.80,
    changePercent: 0.82,
    volume: '2.1M',
    marketCap: '11.8L Cr'
  },
  {
    symbol: 'INFY',
    name: 'Infosys Limited',
    exchange: 'NSE',
    sector: 'IT Services',
    price: 1423.60,
    change: 8.25,
    changePercent: 0.58,
    volume: '1.8M',
    marketCap: '5.9L Cr'
  },
  {
    symbol: 'ICICIBANK',
    name: 'ICICI Bank Limited',
    exchange: 'NSE',
    sector: 'Banking',
    price: 945.70,
    change: -7.45,
    changePercent: -0.78,
    volume: '3.2M',
    marketCap: '6.6L Cr'
  },
  {
    symbol: 'LT',
    name: 'Larsen & Toubro Limited',
    exchange: 'NSE',
    sector: 'Construction',
    price: 2834.55,
    change: 45.30,
    changePercent: 1.62,
    volume: '756K',
    marketCap: '3.9L Cr'
  },
  {
    symbol: 'SBIN',
    name: 'State Bank of India',
    exchange: 'NSE',
    sector: 'Banking',
    price: 578.90,
    change: -3.45,
    changePercent: -0.59,
    volume: '4.2M',
    marketCap: '5.2L Cr'
  },
  {
    symbol: 'WIPRO',
    name: 'Wipro Limited',
    exchange: 'NSE',
    sector: 'IT Services',
    price: 456.80,
    change: 2.15,
    changePercent: 0.47,
    volume: '2.8M',
    marketCap: '2.5L Cr'
  },
  {
    symbol: 'MARUTI',
    name: 'Maruti Suzuki India Limited',
    exchange: 'NSE',
    sector: 'Automobile',
    price: 10234.50,
    change: 125.30,
    changePercent: 1.24,
    volume: '345K',
    marketCap: '3.1L Cr'
  },
  {
    symbol: 'BAJFINANCE',
    name: 'Bajaj Finance Limited',
    exchange: 'NSE',
    sector: 'Financial Services',
    price: 6789.45,
    change: -89.55,
    changePercent: -1.30,
    volume: '678K',
    marketCap: '4.2L Cr'
  },
  // Add more mock symbols for demonstration
  {
    symbol: 'ADANIPORTS',
    name: 'Adani Ports & Special Economic Zone',
    exchange: 'NSE',
    sector: 'Infrastructure',
    price: 789.60,
    change: 15.30,
    changePercent: 1.98,
    volume: '1.9M',
    marketCap: '1.6L Cr'
  },
  {
    symbol: 'ASIANPAINT',
    name: 'Asian Paints Limited',
    exchange: 'NSE',
    sector: 'Chemicals',
    price: 3145.80,
    change: -25.45,
    changePercent: -0.80,
    volume: '543K',
    marketCap: '3.0L Cr'
  },
  {
    symbol: 'AXISBANK',
    name: 'Axis Bank Limited',
    exchange: 'NSE',
    sector: 'Banking',
    price: 1078.25,
    change: 18.75,
    changePercent: 1.77,
    volume: '3.4M',
    marketCap: '3.3L Cr'
  }
]

export function SymbolLookup({
  onSymbolSelect,
  placeholder = "Search symbols...",
  value = "",
  className = "",
  showDetails = true,
  maxResults = 8,
  exchanges = ['NSE', 'BSE', 'MCX']
}: SymbolLookupProps) {
  const [searchTerm, setSearchTerm] = useState(value)
  const [suggestions, setSuggestions] = useState<SymbolData[]>([])
  const [showSuggestions, setShowSuggestions] = useState(false)
  const [selectedIndex, setSelectedIndex] = useState(-1)
  const [isLoading, setIsLoading] = useState(false)
  
  const inputRef = useRef<HTMLInputElement>(null)
  const suggestionsRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    setSearchTerm(value)
  }, [value])

  useEffect(() => {
    const searchSymbols = async () => {
      if (searchTerm.length < 1) {
        setSuggestions([])
        setShowSuggestions(false)
        return
      }

      setIsLoading(true)
      
      // Simulate API delay
      await new Promise(resolve => setTimeout(resolve, 150))
      
      const filtered = mockSymbols
        .filter(symbol => 
          exchanges.includes(symbol.exchange) &&
          (symbol.symbol.toLowerCase().includes(searchTerm.toLowerCase()) ||
           symbol.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
           symbol.sector.toLowerCase().includes(searchTerm.toLowerCase()))
        )
        .slice(0, maxResults)
        .sort((a, b) => {
          // Prioritize exact symbol matches
          const aSymbolMatch = a.symbol.toLowerCase().startsWith(searchTerm.toLowerCase())
          const bSymbolMatch = b.symbol.toLowerCase().startsWith(searchTerm.toLowerCase())
          
          if (aSymbolMatch && !bSymbolMatch) return -1
          if (!aSymbolMatch && bSymbolMatch) return 1
          
          // Then prioritize favorites
          if (a.isFavorite && !b.isFavorite) return -1
          if (!a.isFavorite && b.isFavorite) return 1
          
          // Then sort by market cap (descending)
          const aMarketCap = parseFloat(a.marketCap.replace(/[^\d.]/g, ''))
          const bMarketCap = parseFloat(b.marketCap.replace(/[^\d.]/g, ''))
          return bMarketCap - aMarketCap
        })

      setSuggestions(filtered)
      setShowSuggestions(true)
      setSelectedIndex(-1)
      setIsLoading(false)
    }

    const debounceTimer = setTimeout(searchSymbols, 200)
    return () => clearTimeout(debounceTimer)
  }, [searchTerm, exchanges, maxResults])

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setSearchTerm(e.target.value)
  }

  const handleSymbolSelect = (symbol: SymbolData) => {
    setSearchTerm(symbol.symbol)
    setShowSuggestions(false)
    onSymbolSelect(symbol)
  }

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (!showSuggestions || suggestions.length === 0) return

    switch (e.key) {
      case 'ArrowDown':
        e.preventDefault()
        setSelectedIndex(prev => 
          prev < suggestions.length - 1 ? prev + 1 : 0
        )
        break
      case 'ArrowUp':
        e.preventDefault()
        setSelectedIndex(prev => 
          prev > 0 ? prev - 1 : suggestions.length - 1
        )
        break
      case 'Enter':
        e.preventDefault()
        if (selectedIndex >= 0 && selectedIndex < suggestions.length) {
          handleSymbolSelect(suggestions[selectedIndex])
        } else if (suggestions.length === 1) {
          handleSymbolSelect(suggestions[0])
        }
        break
      case 'Escape':
        setShowSuggestions(false)
        setSelectedIndex(-1)
        break
    }
  }

  const handleInputFocus = () => {
    if (suggestions.length > 0) {
      setShowSuggestions(true)
    }
  }

  const handleInputBlur = (e: React.FocusEvent) => {
    // Delay hiding suggestions to allow for clicks
    setTimeout(() => {
      if (!suggestionsRef.current?.contains(document.activeElement)) {
        setShowSuggestions(false)
      }
    }, 150)
  }

  const getExchangeColor = (exchange: string) => {
    switch (exchange) {
      case 'NSE': return 'text-blue-400'
      case 'BSE': return 'text-green-400'
      case 'MCX': return 'text-orange-400'
      default: return 'text-slate-400'
    }
  }

  const getExchangeBg = (exchange: string) => {
    switch (exchange) {
      case 'NSE': return 'bg-blue-500/20'
      case 'BSE': return 'bg-green-500/20'
      case 'MCX': return 'bg-orange-500/20'
      default: return 'bg-slate-500/20'
    }
  }

  return (
    <div className={`relative ${className}`}>
      {/* Search Input */}
      <div className="relative">
        <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
          <Search className={`w-4 h-4 ${isLoading ? 'animate-pulse text-purple-400' : 'text-slate-400'}`} />
        </div>
        
        <input
          ref={inputRef}
          type="text"
          value={searchTerm}
          onChange={handleInputChange}
          onKeyDown={handleKeyDown}
          onFocus={handleInputFocus}
          onBlur={handleInputBlur}
          placeholder={placeholder}
          className="cyber-input w-full pl-10 pr-4 py-3 text-white placeholder-slate-400 rounded-xl"
          autoComplete="off"
        />
        
        {isLoading && (
          <div className="absolute inset-y-0 right-0 pr-3 flex items-center">
            <div className="loading-dots">
              <div className="loading-dot"></div>
              <div className="loading-dot"></div>
              <div className="loading-dot"></div>
            </div>
          </div>
        )}
      </div>

      {/* Suggestions Dropdown */}
      {showSuggestions && suggestions.length > 0 && (
        <div
          ref={suggestionsRef}
          className="absolute z-50 w-full mt-2 max-h-96 overflow-y-auto glass-card rounded-xl border border-slate-700/50 shadow-xl"
        >
          {suggestions.map((symbol, index) => (
            <div
              key={`${symbol.symbol}-${symbol.exchange}`}
              onClick={() => handleSymbolSelect(symbol)}
              className={`p-4 cursor-pointer transition-all border-b border-slate-700/30 last:border-b-0 ${
                index === selectedIndex 
                  ? 'bg-purple-500/20 border-purple-500/50' 
                  : 'hover:bg-slate-700/30'
              }`}
            >
              <div className="flex items-start justify-between">
                <div className="flex-1 min-w-0">
                  {/* Symbol and Name */}
                  <div className="flex items-center space-x-3 mb-1">
                    <div className="flex items-center space-x-2">
                      <span className="font-semibold text-white text-lg">{symbol.symbol}</span>
                      {symbol.isFavorite && (
                        <Star className="w-4 h-4 text-yellow-400 fill-current" />
                      )}
                    </div>
                    <div className={`px-2 py-0.5 rounded text-xs font-medium ${
                      getExchangeBg(symbol.exchange)} ${getExchangeColor(symbol.exchange)
                    }`}>
                      {symbol.exchange}
                    </div>
                  </div>
                  
                  <div className="text-slate-300 text-sm mb-2 truncate">
                    {symbol.name}
                  </div>
                  
                  {showDetails && (
                    <div className="flex items-center space-x-4 text-xs text-slate-400">
                      <span className="px-2 py-1 bg-slate-700/30 rounded">{symbol.sector}</span>
                      <span>Vol: {symbol.volume}</span>
                      <span>MCap: {symbol.marketCap}</span>
                    </div>
                  )}
                </div>
                
                {/* Price and Change */}
                <div className="text-right ml-4 flex-shrink-0">
                  <div className="text-white font-semibold">
                    ₹{symbol.price.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                  </div>
                  <div className={`flex items-center text-sm ${
                    symbol.change >= 0 ? 'text-green-400' : 'text-red-400'
                  }`}>
                    {symbol.change >= 0 ? (
                      <TrendingUp className="w-3 h-3 mr-1" />
                    ) : (
                      <TrendingDown className="w-3 h-3 mr-1" />
                    )}
                    <span>{symbol.change >= 0 ? '+' : ''}{symbol.change.toFixed(2)}</span>
                    <span className="ml-1">({symbol.changePercent.toFixed(2)}%)</span>
                  </div>
                </div>
              </div>
            </div>
          ))}
          
          {/* No Results */}
          {suggestions.length === 0 && searchTerm.length > 0 && !isLoading && (
            <div className="p-6 text-center text-slate-400">
              <Globe className="w-8 h-8 mx-auto mb-2 opacity-50" />
              <p>No symbols found matching "{searchTerm}"</p>
              <p className="text-sm text-slate-500 mt-1">Try a different search term</p>
            </div>
          )}
        </div>
      )}
    </div>
  )
}