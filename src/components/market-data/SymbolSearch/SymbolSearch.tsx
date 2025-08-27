import React, { useState, useEffect, useRef, useCallback } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { Search, TrendingUp, TrendingDown, Star, Clock, ArrowRight } from 'lucide-react'

interface SearchResult {
  symbol: string
  name: string
  exchange: 'NSE' | 'BSE' | 'NYSE' | 'NASDAQ'
  sector: string
  price?: number
  change?: number
  changePercent?: number
  marketCap?: number
  volume?: number
  isFavorite?: boolean
  lastSearched?: Date
}

interface SymbolSearchProps {
  onSymbolSelect: (symbol: string) => void
  onAddToWatchlist?: (symbol: SearchResult) => void
  placeholder?: string
  maxResults?: number
  recentSearches?: boolean
  showFavorites?: boolean
  className?: string
}

export const SymbolSearch: React.FC<SymbolSearchProps> = ({
  onSymbolSelect,
  onAddToWatchlist,
  placeholder = "Search symbols, companies...",
  maxResults = 8,
  recentSearches = true,
  showFavorites = true,
  className = ''
}) => {
  const [query, setQuery] = useState('')
  const [results, setResults] = useState<SearchResult[]>([])
  const [recentSymbols, setRecentSymbols] = useState<SearchResult[]>([])
  const [favorites, setFavorites] = useState<SearchResult[]>([])
  const [isOpen, setIsOpen] = useState(false)
  const [selectedIndex, setSelectedIndex] = useState(0)
  const [loading, setLoading] = useState(false)
  
  const searchRef = useRef<HTMLDivElement>(null)
  const inputRef = useRef<HTMLInputElement>(null)
  const debounceRef = useRef<NodeJS.Timeout>()

  // Mock data for symbol search
  const mockSymbols: SearchResult[] = [
    {
      symbol: 'RELIANCE',
      name: 'Reliance Industries Limited',
      exchange: 'NSE',
      sector: 'Oil & Gas',
      price: 2456.75,
      change: 34.50,
      changePercent: 1.42,
      marketCap: 1664000,
      volume: 2847293
    },
    {
      symbol: 'TCS',
      name: 'Tata Consultancy Services',
      exchange: 'NSE',
      sector: 'Information Technology',
      price: 3789.40,
      change: -42.15,
      changePercent: -1.10,
      marketCap: 1379000,
      volume: 1583647
    },
    {
      symbol: 'HDFCBANK',
      name: 'HDFC Bank Limited',
      exchange: 'NSE',
      sector: 'Banking',
      price: 1687.25,
      change: 18.90,
      changePercent: 1.13,
      marketCap: 932000,
      volume: 3847392
    },
    {
      symbol: 'INFY',
      name: 'Infosys Limited',
      exchange: 'NSE',
      sector: 'Information Technology',
      price: 1456.80,
      change: -8.45,
      changePercent: -0.58,
      marketCap: 608000,
      volume: 2947583
    },
    {
      symbol: 'ICICIBANK',
      name: 'ICICI Bank Limited',
      exchange: 'NSE',
      sector: 'Banking',
      price: 987.35,
      change: 12.75,
      changePercent: 1.31,
      marketCap: 691000,
      volume: 4738291
    },
    {
      symbol: 'WIPRO',
      name: 'Wipro Limited',
      exchange: 'NSE',
      sector: 'Information Technology',
      price: 445.60,
      change: -3.20,
      changePercent: -0.71,
      marketCap: 243000,
      volume: 1847392
    },
    {
      symbol: 'TATASTEEL',
      name: 'Tata Steel Limited',
      exchange: 'NSE',
      sector: 'Steel',
      price: 134.25,
      change: 2.15,
      changePercent: 1.63,
      marketCap: 167000,
      volume: 8374928
    },
    {
      symbol: 'BAJFINANCE',
      name: 'Bajaj Finance Limited',
      exchange: 'NSE',
      sector: 'Financial Services',
      price: 6789.50,
      change: 89.30,
      changePercent: 1.33,
      marketCap: 419000,
      volume: 743628
    },
    {
      symbol: 'AAPL',
      name: 'Apple Inc.',
      exchange: 'NASDAQ',
      sector: 'Technology',
      price: 195.89,
      change: 2.34,
      changePercent: 1.21,
      marketCap: 3042000,
      volume: 45738291
    },
    {
      symbol: 'MSFT',
      name: 'Microsoft Corporation',
      exchange: 'NASDAQ',
      sector: 'Technology',
      price: 378.85,
      change: -4.21,
      changePercent: -1.10,
      marketCap: 2814000,
      volume: 23847392
    }
  ]

  // Initialize with favorites and recent searches
  useEffect(() => {
    const storedFavorites = localStorage.getItem('symbolSearchFavorites')
    const storedRecent = localStorage.getItem('symbolSearchRecent')
    
    if (storedFavorites) {
      setFavorites(JSON.parse(storedFavorites))
    } else {
      // Set default favorites
      const defaultFavorites = mockSymbols.slice(0, 3).map(s => ({
        ...s,
        isFavorite: true
      }))
      setFavorites(defaultFavorites)
    }
    
    if (storedRecent) {
      setRecentSymbols(JSON.parse(storedRecent))
    }
  }, [])

  // Debounced search function
  const performSearch = useCallback((searchQuery: string) => {
    if (!searchQuery.trim()) {
      setResults([])
      setIsOpen(false)
      return
    }

    setLoading(true)
    setIsOpen(true)

    // Simulate API delay
    setTimeout(() => {
      const filtered = mockSymbols.filter(symbol =>
        symbol.symbol.toLowerCase().includes(searchQuery.toLowerCase()) ||
        symbol.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
        symbol.sector.toLowerCase().includes(searchQuery.toLowerCase())
      ).slice(0, maxResults)

      setResults(filtered)
      setSelectedIndex(0)
      setLoading(false)
    }, 150)
  }, [maxResults])

  // Handle search input
  const handleSearch = (value: string) => {
    setQuery(value)
    
    if (debounceRef.current) {
      clearTimeout(debounceRef.current)
    }
    
    debounceRef.current = setTimeout(() => {
      performSearch(value)
    }, 300)
  }

  // Handle symbol selection
  const handleSelectSymbol = (symbol: SearchResult) => {
    // Add to recent searches
    const updatedRecent = [
      { ...symbol, lastSearched: new Date() },
      ...recentSymbols.filter(s => s.symbol !== symbol.symbol)
    ].slice(0, 5)
    
    setRecentSymbols(updatedRecent)
    localStorage.setItem('symbolSearchRecent', JSON.stringify(updatedRecent))
    
    onSymbolSelect(symbol.symbol)
    setQuery('')
    setIsOpen(false)
    setResults([])
  }

  // Toggle favorite
  const toggleFavorite = (symbol: SearchResult, event: React.MouseEvent) => {
    event.stopPropagation()
    
    const updatedFavorites = symbol.isFavorite
      ? favorites.filter(f => f.symbol !== symbol.symbol)
      : [...favorites, { ...symbol, isFavorite: true }]
    
    setFavorites(updatedFavorites)
    localStorage.setItem('symbolSearchFavorites', JSON.stringify(updatedFavorites))
    
    // Update results if symbol is in current results
    setResults(prev => prev.map(r => 
      r.symbol === symbol.symbol 
        ? { ...r, isFavorite: !symbol.isFavorite }
        : r
    ))
  }

  // Keyboard navigation
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (!isOpen) return
      
      switch (e.key) {
        case 'ArrowDown':
          e.preventDefault()
          setSelectedIndex(prev => 
            prev < results.length - 1 ? prev + 1 : 0
          )
          break
        case 'ArrowUp':
          e.preventDefault()
          setSelectedIndex(prev => 
            prev > 0 ? prev - 1 : results.length - 1
          )
          break
        case 'Enter':
          e.preventDefault()
          if (results[selectedIndex]) {
            handleSelectSymbol(results[selectedIndex])
          }
          break
        case 'Escape':
          setIsOpen(false)
          setQuery('')
          inputRef.current?.blur()
          break
      }
    }

    document.addEventListener('keydown', handleKeyDown)
    return () => document.removeEventListener('keydown', handleKeyDown)
  }, [isOpen, results, selectedIndex])

  // Click outside to close
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (searchRef.current && !searchRef.current.contains(event.target as Node)) {
        setIsOpen(false)
      }
    }

    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [])

  const formatMarketCap = (value: number) => {
    if (value >= 100000) return `₹${(value / 100000).toFixed(1)}L Cr`
    if (value >= 1000) return `₹${(value / 1000).toFixed(1)}K Cr`
    return `₹${value} Cr`
  }

  const formatVolume = (value: number) => {
    if (value >= 10000000) return `${(value / 10000000).toFixed(1)}Cr`
    if (value >= 100000) return `${(value / 100000).toFixed(1)}L`
    if (value >= 1000) return `${(value / 1000).toFixed(1)}K`
    return value.toString()
  }

  const ResultItem: React.FC<{ symbol: SearchResult; isSelected: boolean }> = ({ 
    symbol, isSelected 
  }) => {
    const isPositive = symbol.change ? symbol.change >= 0 : false
    const isFav = favorites.some(f => f.symbol === symbol.symbol)

    return (
      <motion.div
        className={`flex items-center justify-between p-3 rounded-xl cursor-pointer transition-all duration-200 ${
          isSelected ? 'bg-purple-600/30 border border-purple-400/50' : 'hover:bg-slate-700/50'
        }`}
        onClick={() => handleSelectSymbol(symbol)}
        initial={{ opacity: 0, x: -10 }}
        animate={{ opacity: 1, x: 0 }}
        whileHover={{ scale: 1.02 }}
      >
        <div className="flex items-center space-x-3 flex-1">
          <div className="flex-1">
            <div className="flex items-center space-x-2">
              <span className="font-semibold text-white text-sm">{symbol.symbol}</span>
              <span className="text-xs px-2 py-0.5 rounded bg-slate-600/50 text-slate-300">
                {symbol.exchange}
              </span>
            </div>
            <div className="text-xs text-slate-400 truncate max-w-48">
              {symbol.name}
            </div>
            <div className="text-xs text-slate-500 mt-1">
              {symbol.sector} • Vol: {symbol.volume ? formatVolume(symbol.volume) : 'N/A'}
            </div>
          </div>
          
          {symbol.price && (
            <div className="text-right">
              <div className="font-semibold text-white text-sm">
                ₹{symbol.price.toLocaleString('en-IN')}
              </div>
              <div className={`flex items-center text-xs ${
                isPositive ? 'text-green-400' : 'text-red-400'
              }`}>
                {isPositive ? (
                  <TrendingUp className="w-3 h-3 mr-1" />
                ) : (
                  <TrendingDown className="w-3 h-3 mr-1" />
                )}
                {isPositive ? '+' : ''}₹{Math.abs(symbol.change || 0).toFixed(2)}
                <span className="ml-1">
                  ({isPositive ? '+' : ''}{symbol.changePercent?.toFixed(2)}%)
                </span>
              </div>
              {symbol.marketCap && (
                <div className="text-xs text-slate-500">
                  {formatMarketCap(symbol.marketCap)}
                </div>
              )}
            </div>
          )}
        </div>

        <div className="flex items-center space-x-2 ml-3">
          <button
            onClick={(e) => toggleFavorite({ ...symbol, isFavorite: isFav }, e)}
            className={`p-1 rounded-lg transition-colors ${
              isFav ? 'text-yellow-400 hover:text-yellow-300' : 'text-slate-500 hover:text-yellow-400'
            }`}
          >
            <Star className={`w-4 h-4 ${isFav ? 'fill-current' : ''}`} />
          </button>
          
          {onAddToWatchlist && (
            <button
              onClick={(e) => {
                e.stopPropagation()
                onAddToWatchlist(symbol)
              }}
              className="p-1 rounded-lg text-slate-500 hover:text-purple-400 transition-colors"
              title="Add to watchlist"
            >
              <ArrowRight className="w-4 h-4" />
            </button>
          )}
        </div>
      </motion.div>
    )
  }

  return (
    <div ref={searchRef} className={`relative ${className}`}>
      {/* Search Input */}
      <div className="relative">
        <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-5 h-5 text-slate-400" />
        <input
          ref={inputRef}
          type="text"
          value={query}
          onChange={(e) => handleSearch(e.target.value)}
          onFocus={() => query && setIsOpen(true)}
          placeholder={placeholder}
          className="w-full pl-10 pr-4 py-3 glass-input rounded-xl text-white placeholder-slate-400 focus:ring-2 focus:ring-purple-400/50 transition-all"
        />
        {loading && (
          <div className="absolute right-3 top-1/2 transform -translate-y-1/2">
            <div className="w-5 h-5 border-2 border-purple-400 border-t-transparent rounded-full animate-spin" />
          </div>
        )}
      </div>

      {/* Results Dropdown */}
      <AnimatePresence>
        {isOpen && (
          <motion.div
            className="absolute top-full left-0 right-0 mt-2 glass-card rounded-xl shadow-2xl z-50 max-h-96 overflow-hidden"
            initial={{ opacity: 0, y: -10, scale: 0.95 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: -10, scale: 0.95 }}
            transition={{ duration: 0.2, ease: [0.16, 1, 0.3, 1] }}
          >
            <div className="max-h-96 overflow-y-auto">
              {/* Search Results */}
              {results.length > 0 && (
                <div className="p-3">
                  <div className="text-sm font-medium text-slate-400 mb-2 px-1">
                    Search Results ({results.length})
                  </div>
                  <div className="space-y-1">
                    {results.map((symbol, index) => (
                      <ResultItem
                        key={`search-${symbol.symbol}`}
                        symbol={symbol}
                        isSelected={index === selectedIndex}
                      />
                    ))}
                  </div>
                </div>
              )}

              {/* No Results */}
              {query && results.length === 0 && !loading && (
                <div className="p-6 text-center text-slate-400">
                  <Search className="w-12 h-12 mx-auto mb-3 opacity-50" />
                  <p>No symbols found for "{query}"</p>
                  <p className="text-sm mt-1">Try searching by symbol, company name, or sector</p>
                </div>
              )}

              {/* Favorites */}
              {!query && showFavorites && favorites.length > 0 && (
                <div className="p-3 border-b border-slate-700/50">
                  <div className="text-sm font-medium text-slate-400 mb-2 px-1 flex items-center">
                    <Star className="w-4 h-4 mr-1 text-yellow-400" />
                    Favorites
                  </div>
                  <div className="space-y-1">
                    {favorites.slice(0, 3).map((symbol) => (
                      <ResultItem
                        key={`fav-${symbol.symbol}`}
                        symbol={symbol}
                        isSelected={false}
                      />
                    ))}
                  </div>
                </div>
              )}

              {/* Recent Searches */}
              {!query && recentSearches && recentSymbols.length > 0 && (
                <div className="p-3">
                  <div className="text-sm font-medium text-slate-400 mb-2 px-1 flex items-center">
                    <Clock className="w-4 h-4 mr-1" />
                    Recent Searches
                  </div>
                  <div className="space-y-1">
                    {recentSymbols.map((symbol) => (
                      <ResultItem
                        key={`recent-${symbol.symbol}`}
                        symbol={symbol}
                        isSelected={false}
                      />
                    ))}
                  </div>
                </div>
              )}

              {/* Empty State */}
              {!query && favorites.length === 0 && recentSymbols.length === 0 && (
                <div className="p-6 text-center text-slate-400">
                  <Search className="w-12 h-12 mx-auto mb-3 opacity-50" />
                  <p>Start typing to search for symbols</p>
                  <p className="text-sm mt-1">Search by symbol, company name, or sector</p>
                </div>
              )}
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  )
}

export default SymbolSearch