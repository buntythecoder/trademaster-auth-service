// Enhanced Symbol Search with Autocomplete
// FRONT-003: Real-time Market Data Enhancement

import React, { useState, useEffect, useCallback, useRef, useMemo } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import {
  Search,
  TrendingUp,
  TrendingDown,
  Star,
  Clock,
  Building,
  Globe,
  Zap,
  X,
  Filter,
  BarChart3,
  ArrowRight,
  Bookmark,
  History
} from 'lucide-react'

export interface SymbolSearchResult {
  symbol: string
  name: string
  exchange: string
  sector: string
  country: string
  price: number
  change: number
  changePercent: number
  volume: number
  marketCap: number
  currency: string
  type: 'stock' | 'etf' | 'index' | 'crypto' | 'forex'
  description: string
  lastUpdated: Date
}

export interface SearchFilter {
  exchange?: string[]
  sector?: string[]
  country?: string[]
  type?: string[]
  priceRange?: [number, number]
  marketCapRange?: [number, number]
}

const mockSearchResults: SymbolSearchResult[] = [
  {
    symbol: 'RELIANCE',
    name: 'Reliance Industries Limited',
    exchange: 'NSE',
    sector: 'Oil & Gas',
    country: 'India',
    price: 2456.75,
    change: 34.50,
    changePercent: 1.42,
    volume: 2847293,
    marketCap: 1664000,
    currency: 'INR',
    type: 'stock',
    description: 'Largest private sector company in India by market cap',
    lastUpdated: new Date()
  },
  {
    symbol: 'TCS',
    name: 'Tata Consultancy Services',
    exchange: 'NSE',
    sector: 'IT Services',
    country: 'India',
    price: 3789.40,
    change: -42.15,
    changePercent: -1.10,
    volume: 1583647,
    marketCap: 1379000,
    currency: 'INR',
    type: 'stock',
    description: 'Leading global IT services, consulting and business solutions',
    lastUpdated: new Date()
  },
  {
    symbol: 'AAPL',
    name: 'Apple Inc.',
    exchange: 'NASDAQ',
    sector: 'Technology',
    country: 'USA',
    price: 195.89,
    change: 2.34,
    changePercent: 1.21,
    volume: 45827394,
    marketCap: 3042000,
    currency: 'USD',
    type: 'stock',
    description: 'Technology company specializing in consumer electronics and software',
    lastUpdated: new Date()
  },
  {
    symbol: 'TSLA',
    name: 'Tesla, Inc.',
    exchange: 'NASDAQ',
    sector: 'Automotive',
    country: 'USA',
    price: 248.50,
    change: -5.67,
    changePercent: -2.23,
    volume: 89473625,
    marketCap: 789000,
    currency: 'USD',
    type: 'stock',
    description: 'Electric vehicle and clean energy company',
    lastUpdated: new Date()
  },
  {
    symbol: 'BTC-USD',
    name: 'Bitcoin',
    exchange: 'Crypto',
    sector: 'Cryptocurrency',
    country: 'Global',
    price: 43250.00,
    change: 1250.00,
    changePercent: 2.98,
    volume: 28473924,
    marketCap: 847000,
    currency: 'USD',
    type: 'crypto',
    description: 'Leading cryptocurrency and digital asset',
    lastUpdated: new Date()
  }
]

interface EnhancedSymbolSearchProps {
  onSelect: (symbol: SymbolSearchResult) => void
  placeholder?: string
  maxResults?: number
  showFilters?: boolean
  className?: string
}

const SearchResultItem: React.FC<{
  result: SymbolSearchResult
  onClick: () => void
  onAddToWatchlist: (e: React.MouseEvent) => void
  isHighlighted: boolean
}> = ({ result, onClick, onAddToWatchlist, isHighlighted }) => {
  const isPositive = result.change >= 0

  const typeIcons = {
    stock: BarChart3,
    etf: Globe,
    index: TrendingUp,
    crypto: Zap,
    forex: Globe
  }
  
  const TypeIcon = typeIcons[result.type] || BarChart3

  return (
    <motion.div
      className={`p-4 cursor-pointer border-l-4 transition-all duration-200 ${
        isHighlighted
          ? 'bg-purple-500/10 border-purple-500 shadow-lg'
          : 'bg-slate-800/30 border-transparent hover:bg-slate-700/40 hover:border-slate-600'
      }`}
      onClick={onClick}
      whileHover={{ scale: 1.02 }}
      transition={{ duration: 0.1 }}
    >
      <div className="flex items-start justify-between">
        <div className="flex items-start space-x-3">
          <div className={`p-2 rounded-lg ${
            result.type === 'stock' ? 'bg-blue-500/20 text-blue-400' :
            result.type === 'crypto' ? 'bg-yellow-500/20 text-yellow-400' :
            result.type === 'etf' ? 'bg-green-500/20 text-green-400' :
            'bg-purple-500/20 text-purple-400'
          }`}>
            <TypeIcon className="w-4 h-4" />
          </div>
          
          <div className="flex-1">
            <div className="flex items-center space-x-2 mb-1">
              <h4 className="font-bold text-white text-lg">{result.symbol}</h4>
              <span className="text-xs px-2 py-1 rounded-full bg-slate-700 text-slate-300">
                {result.exchange}
              </span>
              <span className="text-xs text-slate-400">{result.country}</span>
            </div>
            
            <h5 className="text-sm font-medium text-slate-200 mb-1 leading-tight">
              {result.name}
            </h5>
            
            <p className="text-xs text-slate-400 mb-2 line-clamp-1">
              {result.description}
            </p>
            
            <div className="flex items-center space-x-4 text-xs">
              <span className="text-slate-400">{result.sector}</span>
              <span className="text-slate-400">
                Vol: {(result.volume / 1000).toFixed(1)}K
              </span>
              <span className="text-slate-400">
                MCap: {result.currency} {(result.marketCap / 1000).toFixed(1)}K Cr
              </span>
            </div>
          </div>
        </div>
        
        <div className="flex items-center space-x-3">
          <div className="text-right">
            <div className="font-bold text-white text-lg">
              {result.currency} {result.price.toLocaleString()}
            </div>
            <div className={`text-sm font-medium ${
              isPositive ? 'text-green-400' : 'text-red-400'
            }`}>
              {isPositive ? '+' : ''}{result.change.toFixed(2)} ({result.changePercent.toFixed(2)}%)
            </div>
          </div>
          
          <button
            onClick={onAddToWatchlist}
            className="p-2 rounded-lg bg-slate-700/50 text-slate-400 hover:text-yellow-400 hover:bg-slate-600/50 transition-all duration-200 opacity-0 group-hover:opacity-100"
            title="Add to Watchlist"
          >
            <Star className="w-4 h-4" />
          </button>
        </div>
      </div>
    </motion.div>
  )
}

const FilterPanel: React.FC<{
  filters: SearchFilter
  onFiltersChange: (filters: SearchFilter) => void
  isVisible: boolean
}> = ({ filters, onFiltersChange, isVisible }) => {
  const exchanges = ['NSE', 'BSE', 'NASDAQ', 'NYSE', 'LSE', 'TSE']
  const sectors = ['Technology', 'Banking', 'Oil & Gas', 'IT Services', 'Automotive', 'Healthcare']
  const countries = ['India', 'USA', 'UK', 'Japan', 'Germany', 'China']
  const types = ['stock', 'etf', 'index', 'crypto', 'forex']

  if (!isVisible) return null

  return (
    <motion.div
      className="absolute top-full left-0 right-0 z-50 mt-2 p-4 bg-slate-800/95 backdrop-blur-sm border border-slate-700/50 rounded-xl shadow-2xl"
      initial={{ opacity: 0, y: -10 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, y: -10 }}
      transition={{ duration: 0.2 }}
    >
      <div className="grid grid-cols-2 gap-6">
        <div>
          <h4 className="text-sm font-medium text-white mb-3">Exchange</h4>
          <div className="flex flex-wrap gap-2">
            {exchanges.map(exchange => (
              <button
                key={exchange}
                onClick={() => {
                  const currentExchanges = filters.exchange || []
                  const newExchanges = currentExchanges.includes(exchange)
                    ? currentExchanges.filter(e => e !== exchange)
                    : [...currentExchanges, exchange]
                  onFiltersChange({ ...filters, exchange: newExchanges })
                }}
                className={`px-3 py-1 rounded-full text-xs transition-colors ${
                  filters.exchange?.includes(exchange)
                    ? 'bg-purple-500 text-white'
                    : 'bg-slate-700 text-slate-300 hover:bg-slate-600'
                }`}
              >
                {exchange}
              </button>
            ))}
          </div>
        </div>
        
        <div>
          <h4 className="text-sm font-medium text-white mb-3">Asset Type</h4>
          <div className="flex flex-wrap gap-2">
            {types.map(type => (
              <button
                key={type}
                onClick={() => {
                  const currentTypes = filters.type || []
                  const newTypes = currentTypes.includes(type)
                    ? currentTypes.filter(t => t !== type)
                    : [...currentTypes, type]
                  onFiltersChange({ ...filters, type: newTypes })
                }}
                className={`px-3 py-1 rounded-full text-xs capitalize transition-colors ${
                  filters.type?.includes(type)
                    ? 'bg-purple-500 text-white'
                    : 'bg-slate-700 text-slate-300 hover:bg-slate-600'
                }`}
              >
                {type}
              </button>
            ))}
          </div>
        </div>
        
        <div>
          <h4 className="text-sm font-medium text-white mb-3">Sector</h4>
          <div className="flex flex-wrap gap-2">
            {sectors.map(sector => (
              <button
                key={sector}
                onClick={() => {
                  const currentSectors = filters.sector || []
                  const newSectors = currentSectors.includes(sector)
                    ? currentSectors.filter(s => s !== sector)
                    : [...currentSectors, sector]
                  onFiltersChange({ ...filters, sector: newSectors })
                }}
                className={`px-3 py-1 rounded-full text-xs transition-colors ${
                  filters.sector?.includes(sector)
                    ? 'bg-purple-500 text-white'
                    : 'bg-slate-700 text-slate-300 hover:bg-slate-600'
                }`}
              >
                {sector}
              </button>
            ))}
          </div>
        </div>
        
        <div>
          <h4 className="text-sm font-medium text-white mb-3">Country</h4>
          <div className="flex flex-wrap gap-2">
            {countries.map(country => (
              <button
                key={country}
                onClick={() => {
                  const currentCountries = filters.country || []
                  const newCountries = currentCountries.includes(country)
                    ? currentCountries.filter(c => c !== country)
                    : [...currentCountries, country]
                  onFiltersChange({ ...filters, country: newCountries })
                }}
                className={`px-3 py-1 rounded-full text-xs transition-colors ${
                  filters.country?.includes(country)
                    ? 'bg-purple-500 text-white'
                    : 'bg-slate-700 text-slate-300 hover:bg-slate-600'
                }`}
              >
                {country}
              </button>
            ))}
          </div>
        </div>
      </div>
      
      <div className="flex items-center justify-between mt-4 pt-4 border-t border-slate-700/50">
        <span className="text-sm text-slate-400">
          {Object.values(filters).flat().filter(Boolean).length} filters active
        </span>
        
        <button
          onClick={() => onFiltersChange({})}
          className="text-sm text-purple-400 hover:text-purple-300 transition-colors"
        >
          Clear All
        </button>
      </div>
    </motion.div>
  )
}

export const EnhancedSymbolSearch: React.FC<EnhancedSymbolSearchProps> = ({
  onSelect,
  placeholder = "Search stocks, ETFs, crypto...",
  maxResults = 10,
  showFilters = true,
  className = ''
}) => {
  const [searchQuery, setSearchQuery] = useState('')
  const [searchResults, setSearchResults] = useState<SymbolSearchResult[]>([])
  const [isSearching, setIsSearching] = useState(false)
  const [isDropdownOpen, setIsDropdownOpen] = useState(false)
  const [highlightedIndex, setHighlightedIndex] = useState(-1)
  const [searchHistory, setSearchHistory] = useState<SymbolSearchResult[]>([])
  const [filters, setFilters] = useState<SearchFilter>({})
  const [showFilterPanel, setShowFilterPanel] = useState(false)
  const [recentSymbols] = useState<SymbolSearchResult[]>(mockSearchResults.slice(0, 3))

  const searchRef = useRef<HTMLDivElement>(null)
  const inputRef = useRef<HTMLInputElement>(null)

  // Debounced search function
  const debouncedSearch = useCallback(
    (() => {
      let timeoutId: NodeJS.Timeout
      return (query: string) => {
        clearTimeout(timeoutId)
        timeoutId = setTimeout(() => {
          if (query.trim()) {
            setIsSearching(true)
            // Simulate API call delay
            setTimeout(() => {
              const filteredResults = mockSearchResults.filter(result => {
                const matchesQuery = 
                  result.symbol.toLowerCase().includes(query.toLowerCase()) ||
                  result.name.toLowerCase().includes(query.toLowerCase()) ||
                  result.sector.toLowerCase().includes(query.toLowerCase())

                const matchesFilters = 
                  (!filters.exchange?.length || filters.exchange.includes(result.exchange)) &&
                  (!filters.sector?.length || filters.sector.includes(result.sector)) &&
                  (!filters.country?.length || filters.country.includes(result.country)) &&
                  (!filters.type?.length || filters.type.includes(result.type))

                return matchesQuery && matchesFilters
              }).slice(0, maxResults)

              setSearchResults(filteredResults)
              setIsSearching(false)
              setIsDropdownOpen(true)
            }, 300)
          } else {
            setSearchResults([])
            setIsDropdownOpen(false)
          }
        }, 300)
      }
    })(),
    [filters, maxResults]
  )

  useEffect(() => {
    debouncedSearch(searchQuery)
  }, [searchQuery, debouncedSearch])

  // Keyboard navigation
  const handleKeyDown = useCallback((e: React.KeyboardEvent) => {
    if (!isDropdownOpen) return

    switch (e.key) {
      case 'ArrowDown':
        e.preventDefault()
        setHighlightedIndex(prev => 
          prev < searchResults.length - 1 ? prev + 1 : prev
        )
        break
      case 'ArrowUp':
        e.preventDefault()
        setHighlightedIndex(prev => prev > 0 ? prev - 1 : 0)
        break
      case 'Enter':
        e.preventDefault()
        if (highlightedIndex >= 0 && searchResults[highlightedIndex]) {
          handleSelect(searchResults[highlightedIndex])
        }
        break
      case 'Escape':
        setIsDropdownOpen(false)
        setHighlightedIndex(-1)
        break
    }
  }, [isDropdownOpen, searchResults, highlightedIndex])

  const handleSelect = useCallback((result: SymbolSearchResult) => {
    onSelect(result)
    setSearchQuery(result.symbol)
    setIsDropdownOpen(false)
    setHighlightedIndex(-1)
    
    // Add to search history
    setSearchHistory(prev => {
      const newHistory = [result, ...prev.filter(item => item.symbol !== result.symbol)]
      return newHistory.slice(0, 5) // Keep last 5 searches
    })
  }, [onSelect])

  const handleAddToWatchlist = useCallback((e: React.MouseEvent, symbol: SymbolSearchResult) => {
    e.stopPropagation()
    console.log('Add to watchlist:', symbol.symbol)
    // Implement watchlist functionality
  }, [])

  // Click outside handler
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (searchRef.current && !searchRef.current.contains(event.target as Node)) {
        setIsDropdownOpen(false)
        setShowFilterPanel(false)
      }
    }

    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [])

  const hasActiveFilters = Object.values(filters).some(filter => 
    Array.isArray(filter) ? filter.length > 0 : filter
  )

  return (
    <div ref={searchRef} className={`relative ${className}`}>
      <div className="relative">
        <div className="flex items-center">
          <div className="relative flex-1">
            <Search className="absolute left-4 top-1/2 transform -translate-y-1/2 text-slate-400 w-5 h-5 z-10" />
            
            <input
              ref={inputRef}
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              onKeyDown={handleKeyDown}
              onFocus={() => {
                if (searchQuery.trim()) {
                  setIsDropdownOpen(true)
                }
              }}
              placeholder={placeholder}
              className="w-full bg-slate-800/50 border border-slate-700/50 rounded-xl pl-12 pr-20 py-4 text-white placeholder:text-slate-400 focus:ring-2 focus:ring-purple-500 focus:border-transparent transition-all duration-200"
            />
            
            <div className="absolute right-2 top-1/2 transform -translate-y-1/2 flex items-center space-x-2">
              {showFilters && (
                <button
                  onClick={() => setShowFilterPanel(!showFilterPanel)}
                  className={`p-2 rounded-lg transition-colors ${
                    hasActiveFilters || showFilterPanel
                      ? 'bg-purple-500/20 text-purple-400'
                      : 'text-slate-400 hover:text-white hover:bg-slate-700/50'
                  }`}
                  title="Filter Results"
                >
                  <Filter className="w-4 h-4" />
                </button>
              )}
              
              {searchQuery && (
                <button
                  onClick={() => {
                    setSearchQuery('')
                    setSearchResults([])
                    setIsDropdownOpen(false)
                    inputRef.current?.focus()
                  }}
                  className="p-2 rounded-lg text-slate-400 hover:text-white hover:bg-slate-700/50 transition-colors"
                >
                  <X className="w-4 h-4" />
                </button>
              )}
            </div>
          </div>
        </div>

        {/* Filter Panel */}
        <AnimatePresence>
          {showFilterPanel && (
            <FilterPanel
              filters={filters}
              onFiltersChange={setFilters}
              isVisible={showFilterPanel}
            />
          )}
        </AnimatePresence>

        {/* Dropdown */}
        <AnimatePresence>
          {isDropdownOpen && (
            <motion.div
              className="absolute top-full left-0 right-0 z-40 mt-2 bg-slate-900/95 backdrop-blur-sm border border-slate-700/50 rounded-xl shadow-2xl overflow-hidden"
              initial={{ opacity: 0, y: -10 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -10 }}
              transition={{ duration: 0.2 }}
            >
              {isSearching ? (
                <div className="p-8 text-center">
                  <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-purple-500 mx-auto mb-4" />
                  <p className="text-slate-400">Searching...</p>
                </div>
              ) : searchResults.length > 0 ? (
                <div className="max-h-96 overflow-y-auto">
                  {searchResults.map((result, index) => (
                    <div key={result.symbol} className="group">
                      <SearchResultItem
                        result={result}
                        onClick={() => handleSelect(result)}
                        onAddToWatchlist={(e) => handleAddToWatchlist(e, result)}
                        isHighlighted={index === highlightedIndex}
                      />
                    </div>
                  ))}
                </div>
              ) : searchQuery.trim() ? (
                <div className="p-8 text-center">
                  <Search className="w-12 h-12 text-slate-600 mx-auto mb-4" />
                  <p className="text-slate-400 mb-2">No results found</p>
                  <p className="text-sm text-slate-500">
                    Try adjusting your search or filters
                  </p>
                </div>
              ) : (
                <div className="p-6">
                  {searchHistory.length > 0 && (
                    <div className="mb-6">
                      <div className="flex items-center mb-3">
                        <History className="w-4 h-4 text-slate-400 mr-2" />
                        <span className="text-sm text-slate-400">Recent Searches</span>
                      </div>
                      <div className="space-y-2">
                        {searchHistory.slice(0, 3).map(item => (
                          <button
                            key={item.symbol}
                            onClick={() => handleSelect(item)}
                            className="w-full text-left p-2 rounded-lg hover:bg-slate-800/50 transition-colors flex items-center justify-between group"
                          >
                            <div className="flex items-center space-x-2">
                              <Clock className="w-4 h-4 text-slate-500" />
                              <span className="text-white">{item.symbol}</span>
                              <span className="text-sm text-slate-400">{item.name}</span>
                            </div>
                            <ArrowRight className="w-4 h-4 text-slate-500 group-hover:text-slate-300" />
                          </button>
                        ))}
                      </div>
                    </div>
                  )}

                  <div>
                    <div className="flex items-center mb-3">
                      <Bookmark className="w-4 h-4 text-slate-400 mr-2" />
                      <span className="text-sm text-slate-400">Popular Symbols</span>
                    </div>
                    <div className="space-y-2">
                      {recentSymbols.map(item => (
                        <button
                          key={item.symbol}
                          onClick={() => handleSelect(item)}
                          className="w-full text-left p-2 rounded-lg hover:bg-slate-800/50 transition-colors flex items-center justify-between group"
                        >
                          <div className="flex items-center space-x-2">
                            <TrendingUp className="w-4 h-4 text-green-400" />
                            <span className="text-white">{item.symbol}</span>
                            <span className="text-sm text-slate-400">{item.exchange}</span>
                          </div>
                          <ArrowRight className="w-4 h-4 text-slate-500 group-hover:text-slate-300" />
                        </button>
                      ))}
                    </div>
                  </div>
                </div>
              )}
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    </div>
  )
}

export default EnhancedSymbolSearch