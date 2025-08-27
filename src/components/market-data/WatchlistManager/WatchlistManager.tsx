import React, { useState, useEffect } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { 
  Plus, 
  Search, 
  Star, 
  TrendingUp, 
  TrendingDown, 
  Eye,
  EyeOff,
  Settings,
  MoreVertical,
  X,
  Heart
} from 'lucide-react'
import { useConnectionStatus } from '@/hooks/useWebSocket'

interface WatchlistSymbol {
  id: string
  symbol: string
  name: string
  price: number
  change: number
  changePercent: number
  volume: number
  marketCap?: number
  sector?: string
  isFavorite: boolean
  isHidden: boolean
  alerts?: {
    priceAbove?: number
    priceBelow?: number
    volumeAbove?: number
  }
}

interface WatchlistCategory {
  id: string
  name: string
  color: string
  symbols: string[]
}

interface WatchlistManagerProps {
  symbols: WatchlistSymbol[]
  categories?: WatchlistCategory[]
  onAddSymbol?: (symbol: string) => void
  onRemoveSymbol?: (symbolId: string) => void
  onToggleFavorite?: (symbolId: string) => void
  onToggleVisibility?: (symbolId: string) => void
  onSetAlert?: (symbolId: string, alerts: any) => void
  compactMode?: boolean
  className?: string
}

const SymbolSearchModal: React.FC<{
  isOpen: boolean
  onClose: () => void
  onSelectSymbol: (symbol: string) => void
}> = ({ isOpen, onClose, onSelectSymbol }) => {
  const [searchQuery, setSearchQuery] = useState('')
  const [searchResults, setSearchResults] = useState<WatchlistSymbol[]>([])

  // Mock search results
  const mockSearchResults: WatchlistSymbol[] = [
    {
      id: 'TATASTEEL',
      symbol: 'TATASTEEL',
      name: 'Tata Steel Limited',
      price: 124.50,
      change: 2.30,
      changePercent: 1.88,
      volume: 18475920,
      sector: 'Steel',
      isFavorite: false,
      isHidden: false
    },
    {
      id: 'BAJFINANCE',
      symbol: 'BAJFINANCE',
      name: 'Bajaj Finance Limited',
      price: 6789.25,
      change: -45.80,
      changePercent: -0.67,
      volume: 1847592,
      sector: 'Financial Services',
      isFavorite: false,
      isHidden: false
    },
    {
      id: 'MARUTI',
      symbol: 'MARUTI',
      name: 'Maruti Suzuki India Limited',
      price: 9876.40,
      change: 123.60,
      changePercent: 1.27,
      volume: 584720,
      sector: 'Automobiles',
      isFavorite: false,
      isHidden: false
    }
  ]

  useEffect(() => {
    if (searchQuery.length > 1) {
      const filtered = mockSearchResults.filter(symbol =>
        symbol.symbol.toLowerCase().includes(searchQuery.toLowerCase()) ||
        symbol.name.toLowerCase().includes(searchQuery.toLowerCase())
      )
      setSearchResults(filtered)
    } else {
      setSearchResults([])
    }
  }, [searchQuery])

  if (!isOpen) return null

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      className="fixed inset-0 bg-black/50 backdrop-blur-sm z-50 flex items-center justify-center p-4"
      onClick={onClose}
    >
      <motion.div
        initial={{ scale: 0.9, opacity: 0 }}
        animate={{ scale: 1, opacity: 1 }}
        exit={{ scale: 0.9, opacity: 0 }}
        className="bg-slate-800 rounded-2xl border border-slate-700 w-full max-w-md max-h-96 overflow-hidden"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="p-4 border-b border-slate-700">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-lg font-bold text-white">Add Symbol</h3>
            <button
              onClick={onClose}
              className="p-2 hover:bg-slate-700 rounded-lg transition-colors"
            >
              <X className="h-4 w-4 text-slate-400" />
            </button>
          </div>
          
          <div className="relative">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-slate-400" />
            <input
              type="text"
              placeholder="Search symbols..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-4 py-2 bg-slate-700 border border-slate-600 rounded-lg text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent"
              autoFocus
            />
          </div>
        </div>

        <div className="max-h-64 overflow-y-auto">
          <AnimatePresence>
            {searchResults.map((symbol, index) => (
              <motion.button
                key={symbol.id}
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -10 }}
                transition={{ delay: index * 0.05 }}
                onClick={() => {
                  onSelectSymbol(symbol.symbol)
                  onClose()
                }}
                className="w-full p-4 hover:bg-slate-700 transition-colors text-left border-b border-slate-700/50 last:border-b-0"
              >
                <div className="flex items-center justify-between">
                  <div>
                    <div className="font-semibold text-white">{symbol.symbol}</div>
                    <div className="text-sm text-slate-400 truncate">{symbol.name}</div>
                    {symbol.sector && (
                      <div className="text-xs text-slate-500">{symbol.sector}</div>
                    )}
                  </div>
                  <div className="text-right">
                    <div className="font-semibold text-white">₹{symbol.price.toFixed(2)}</div>
                    <div className={`text-sm flex items-center ${
                      symbol.change >= 0 ? 'text-green-400' : 'text-red-400'
                    }`}>
                      {symbol.change >= 0 ? (
                        <TrendingUp className="h-3 w-3 mr-1" />
                      ) : (
                        <TrendingDown className="h-3 w-3 mr-1" />
                      )}
                      {symbol.changePercent.toFixed(2)}%
                    </div>
                  </div>
                </div>
              </motion.button>
            ))}
          </AnimatePresence>

          {searchQuery.length > 1 && searchResults.length === 0 && (
            <div className="p-8 text-center text-slate-400">
              <Search className="h-12 w-12 mx-auto mb-3 opacity-50" />
              <p>No symbols found for "{searchQuery}"</p>
              <p className="text-sm mt-1">Try different keywords</p>
            </div>
          )}
        </div>
      </motion.div>
    </motion.div>
  )
}

const WatchlistItem: React.FC<{
  symbol: WatchlistSymbol
  onToggleFavorite: (id: string) => void
  onToggleVisibility: (id: string) => void
  onRemove: (id: string) => void
  compactMode: boolean
}> = ({ symbol, onToggleFavorite, onToggleVisibility, onRemove, compactMode }) => {
  const [showActions, setShowActions] = useState(false)
  const isPositive = symbol.change >= 0

  return (
    <motion.div
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, y: -10 }}
      className={`relative p-3 rounded-xl bg-slate-800/30 hover:bg-slate-700/40 transition-all duration-300 border border-slate-700/50 ${
        symbol.isHidden ? 'opacity-50' : ''
      }`}
      onMouseEnter={() => setShowActions(true)}
      onMouseLeave={() => setShowActions(false)}
    >
      <div className="flex items-center justify-between">
        {/* Symbol Info */}
        <div className="flex-1">
          <div className="flex items-center space-x-2">
            <span className="font-semibold text-white">{symbol.symbol}</span>
            {symbol.isFavorite && (
              <Heart className="h-3 w-3 text-red-400 fill-current" />
            )}
            {symbol.alerts && Object.keys(symbol.alerts).length > 0 && (
              <div className="w-2 h-2 bg-yellow-400 rounded-full animate-pulse" />
            )}
          </div>
          {!compactMode && (
            <div className="text-xs text-slate-400 truncate max-w-32">
              {symbol.name}
            </div>
          )}
          {symbol.sector && !compactMode && (
            <div className="text-xs text-slate-500">{symbol.sector}</div>
          )}
        </div>

        {/* Price Info */}
        <div className="text-right">
          <div className="font-bold text-white">
            ₹{symbol.price.toFixed(2)}
          </div>
          <div className={`flex items-center text-xs ${
            isPositive ? 'text-green-400' : 'text-red-400'
          }`}>
            {isPositive ? (
              <TrendingUp className="h-3 w-3 mr-1" />
            ) : (
              <TrendingDown className="h-3 w-3 mr-1" />
            )}
            {isPositive ? '+' : ''}₹{Math.abs(symbol.change).toFixed(2)}
            <span className="ml-1">({symbol.changePercent.toFixed(2)}%)</span>
          </div>
        </div>

        {/* Action Buttons */}
        <AnimatePresence>
          {showActions && (
            <motion.div
              initial={{ opacity: 0, scale: 0.8 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.8 }}
              className="absolute right-2 top-2 flex items-center space-x-1 bg-slate-900/90 backdrop-blur-sm rounded-lg p-1"
            >
              <button
                onClick={() => onToggleFavorite(symbol.id)}
                className={`p-1 rounded hover:scale-110 transition-all ${
                  symbol.isFavorite ? 'text-red-400' : 'text-slate-400 hover:text-red-400'
                }`}
              >
                <Heart className={`h-3 w-3 ${symbol.isFavorite ? 'fill-current' : ''}`} />
              </button>
              
              <button
                onClick={() => onToggleVisibility(symbol.id)}
                className="p-1 rounded text-slate-400 hover:text-blue-400 hover:scale-110 transition-all"
              >
                {symbol.isHidden ? <Eye className="h-3 w-3" /> : <EyeOff className="h-3 w-3" />}
              </button>
              
              <button
                onClick={() => onRemove(symbol.id)}
                className="p-1 rounded text-slate-400 hover:text-red-400 hover:scale-110 transition-all"
              >
                <X className="h-3 w-3" />
              </button>
            </motion.div>
          )}
        </AnimatePresence>
      </div>

      {/* Volume (if not compact) */}
      {!compactMode && (
        <div className="mt-2 pt-2 border-t border-slate-700/50">
          <div className="flex justify-between text-xs">
            <span className="text-slate-400">Volume:</span>
            <span className="text-slate-300">{(symbol.volume / 1000).toFixed(0)}K</span>
          </div>
        </div>
      )}
    </motion.div>
  )
}

export const WatchlistManager: React.FC<WatchlistManagerProps> = ({
  symbols,
  categories = [],
  onAddSymbol,
  onRemoveSymbol,
  onToggleFavorite,
  onToggleVisibility,
  onSetAlert,
  compactMode = false,
  className = ''
}) => {
  const [showSearch, setShowSearch] = useState(false)
  const [activeCategory, setActiveCategory] = useState<string>('all')
  const [showSettings, setShowSettings] = useState(false)
  const { isConnected } = useConnectionStatus()

  // Mock data when no symbols provided
  const mockSymbols: WatchlistSymbol[] = [
    {
      id: 'RELIANCE',
      symbol: 'RELIANCE',
      name: 'Reliance Industries Limited',
      price: 2456.75,
      change: 34.50,
      changePercent: 1.42,
      volume: 2847293,
      sector: 'Oil & Gas',
      isFavorite: true,
      isHidden: false,
      alerts: { priceAbove: 2500 }
    },
    {
      id: 'TCS',
      symbol: 'TCS',
      name: 'Tata Consultancy Services Limited',
      price: 3789.40,
      change: -42.15,
      changePercent: -1.10,
      volume: 1583647,
      sector: 'Information Technology',
      isFavorite: false,
      isHidden: false
    },
    {
      id: 'HDFCBANK',
      symbol: 'HDFCBANK',
      name: 'HDFC Bank Limited',
      price: 1687.25,
      change: 18.90,
      changePercent: 1.13,
      volume: 3847392,
      sector: 'Banking',
      isFavorite: true,
      isHidden: false
    },
    {
      id: 'INFY',
      symbol: 'INFY',
      name: 'Infosys Limited',
      price: 1456.80,
      change: -8.45,
      changePercent: -0.58,
      volume: 2947583,
      sector: 'Information Technology',
      isFavorite: false,
      isHidden: true
    }
  ]

  const displaySymbols = symbols.length > 0 ? symbols : mockSymbols

  // Filter symbols based on category
  const filteredSymbols = activeCategory === 'all' 
    ? displaySymbols
    : activeCategory === 'favorites'
    ? displaySymbols.filter(s => s.isFavorite)
    : displaySymbols.filter(s => 
        categories.find(c => c.id === activeCategory)?.symbols.includes(s.symbol)
      )

  const handleAddSymbol = (symbol: string) => {
    onAddSymbol?.(symbol)
  }

  const handleToggleFavorite = (symbolId: string) => {
    onToggleFavorite?.(symbolId)
  }

  const handleToggleVisibility = (symbolId: string) => {
    onToggleVisibility?.(symbolId)
  }

  const handleRemoveSymbol = (symbolId: string) => {
    onRemoveSymbol?.(symbolId)
  }

  return (
    <>
      <motion.div 
        className={`glass-widget-card rounded-2xl overflow-hidden ${className}`}
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.6, ease: [0.16, 1, 0.3, 1] }}
      >
        {/* Header */}
        <div className="flex items-center justify-between p-4 border-b border-slate-700/50">
          <div className="flex items-center space-x-3">
            <div className={`w-3 h-3 rounded-full ${
              isConnected ? 'bg-green-400 animate-pulse' : 'bg-red-400'
            }`} />
            <div>
              <h2 className="text-lg font-bold text-white">Watchlist</h2>
              <span className="text-sm text-slate-400">
                {filteredSymbols.length} symbols
              </span>
            </div>
          </div>
          
          <div className="flex items-center space-x-2">
            <button
              onClick={() => setShowSearch(true)}
              className="cyber-button-sm p-2 rounded-lg hover:scale-110 transition-all duration-300"
              title="Add Symbol"
            >
              <Plus className="h-4 w-4" />
            </button>
            
            <button
              onClick={() => setShowSettings(!showSettings)}
              className="cyber-button-sm p-2 rounded-lg hover:scale-110 transition-all duration-300"
              title="Settings"
            >
              <Settings className="h-4 w-4" />
            </button>
          </div>
        </div>

        {/* Category Tabs */}
        <div className="px-4 py-2 border-b border-slate-700/50">
          <div className="flex space-x-1 bg-slate-800/50 rounded-lg p-1">
            <button
              onClick={() => setActiveCategory('all')}
              className={`px-3 py-1 rounded-md text-xs font-medium transition-all ${
                activeCategory === 'all'
                  ? 'bg-purple-600 text-white'
                  : 'text-slate-300 hover:bg-slate-700'
              }`}
            >
              All ({displaySymbols.length})
            </button>
            
            <button
              onClick={() => setActiveCategory('favorites')}
              className={`px-3 py-1 rounded-md text-xs font-medium transition-all ${
                activeCategory === 'favorites'
                  ? 'bg-purple-600 text-white'
                  : 'text-slate-300 hover:bg-slate-700'
              }`}
            >
              <Heart className="h-3 w-3 inline mr-1" />
              Favorites ({displaySymbols.filter(s => s.isFavorite).length})
            </button>
          </div>
        </div>

        {/* Watchlist Items */}
        <div className="p-4 space-y-3 max-h-96 overflow-y-auto">
          <AnimatePresence mode="popLayout">
            {filteredSymbols.map((symbol) => (
              <WatchlistItem
                key={symbol.id}
                symbol={symbol}
                onToggleFavorite={handleToggleFavorite}
                onToggleVisibility={handleToggleVisibility}
                onRemove={handleRemoveSymbol}
                compactMode={compactMode}
              />
            ))}
          </AnimatePresence>

          {filteredSymbols.length === 0 && (
            <div className="text-center py-8 text-slate-400">
              <Star className="h-12 w-12 mx-auto mb-3 opacity-50" />
              <p>No symbols in this category</p>
              <button
                onClick={() => setShowSearch(true)}
                className="mt-2 text-purple-400 hover:text-purple-300 text-sm"
              >
                Add your first symbol
              </button>
            </div>
          )}
        </div>

        {/* Quick Stats */}
        {!compactMode && (
          <div className="p-4 border-t border-slate-700/50">
            <div className="grid grid-cols-3 gap-4 text-center text-sm">
              <div>
                <div className="text-green-400 font-semibold">
                  {displaySymbols.filter(s => s.change >= 0).length}
                </div>
                <div className="text-slate-400 text-xs">Gainers</div>
              </div>
              <div>
                <div className="text-red-400 font-semibold">
                  {displaySymbols.filter(s => s.change < 0).length}
                </div>
                <div className="text-slate-400 text-xs">Losers</div>
              </div>
              <div>
                <div className="text-yellow-400 font-semibold">
                  {displaySymbols.filter(s => s.alerts && Object.keys(s.alerts).length > 0).length}
                </div>
                <div className="text-slate-400 text-xs">Alerts</div>
              </div>
            </div>
          </div>
        )}
      </motion.div>

      {/* Symbol Search Modal */}
      <AnimatePresence>
        {showSearch && (
          <SymbolSearchModal
            isOpen={showSearch}
            onClose={() => setShowSearch(false)}
            onSelectSymbol={handleAddSymbol}
          />
        )}
      </AnimatePresence>
    </>
  )
}