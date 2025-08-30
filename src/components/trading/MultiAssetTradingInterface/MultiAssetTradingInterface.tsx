// Multi-Asset Trading Interface
// FRONT-004: Advanced Trading Interface Enhancement

import React, { useState, useEffect, useCallback, useMemo } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import {
  Layers,
  TrendingUp,
  TrendingDown,
  DollarSign,
  Bitcoin,
  Coins,
  Globe,
  Calendar,
  Clock,
  Target,
  Shield,
  Zap,
  BarChart3,
  PieChart,
  Activity,
  Settings,
  RefreshCw,
  ArrowUpDown,
  Percent,
  Calculator,
  AlertTriangle,
  CheckCircle,
  Info,
  Eye,
  EyeOff,
  Plus,
  Minus,
  X,
  Search,
  Filter,
  Star,
  Bookmark
} from 'lucide-react'

export type AssetType = 'equity' | 'options' | 'futures' | 'crypto' | 'forex' | 'bonds'

export interface AssetContract {
  id: string
  symbol: string
  name: string
  type: AssetType
  exchange: string
  currency: string
  
  // Options specific
  underlyingSymbol?: string
  strikePrice?: number
  expiration?: Date
  optionType?: 'call' | 'put'
  impliedVolatility?: number
  delta?: number
  gamma?: number
  theta?: number
  vega?: number
  
  // Futures specific
  contractSize?: number
  deliveryDate?: Date
  tickSize?: number
  marginRequirement?: number
  
  // Crypto specific
  baseAsset?: string
  quoteAsset?: string
  
  // Common market data
  price: number
  bid?: number
  ask?: number
  change: number
  changePercent: number
  volume: number
  openInterest?: number
  lastTrade: Date
}

export interface MultiAssetOrder {
  id: string
  type: 'market' | 'limit' | 'stop' | 'stop-limit'
  asset: AssetContract
  side: 'buy' | 'sell'
  quantity: number
  price?: number
  stopPrice?: number
  strategy?: 'covered-call' | 'protective-put' | 'straddle' | 'strangle' | 'iron-condor' | 'butterfly' | 'collar'
  legs?: MultiAssetOrderLeg[]
  marginRequirement?: number
  commission?: number
  fees?: number
  status: 'draft' | 'pending' | 'filled' | 'cancelled' | 'rejected'
  createdAt: Date
}

export interface MultiAssetOrderLeg {
  id: string
  asset: AssetContract
  side: 'buy' | 'sell'
  quantity: number
  ratio: number
  price?: number
}

export interface OptionsChain {
  underlyingSymbol: string
  underlyingPrice: number
  expirations: OptionsExpiration[]
}

export interface OptionsExpiration {
  date: Date
  daysToExpiry: number
  strikes: OptionsStrike[]
}

export interface OptionsStrike {
  strike: number
  call?: OptionContract
  put?: OptionContract
}

export interface OptionContract {
  symbol: string
  strike: number
  expiration: Date
  type: 'call' | 'put'
  price: number
  bid: number
  ask: number
  volume: number
  openInterest: number
  impliedVolatility: number
  delta: number
  gamma: number
  theta: number
  vega: number
}

interface MultiAssetTradingInterfaceProps {
  selectedAssetType: AssetType
  onAssetTypeChange: (type: AssetType) => void
  availableAssets: AssetContract[]
  onOrderSubmit: (order: MultiAssetOrder) => Promise<void>
  className?: string
}

const AssetTypeSelector: React.FC<{
  value: AssetType
  onChange: (type: AssetType) => void
  assetCounts: Record<AssetType, number>
}> = ({ value, onChange, assetCounts }) => {
  const assetTypes = [
    { 
      type: 'equity' as AssetType, 
      label: 'Stocks', 
      icon: TrendingUp, 
      color: 'text-blue-400',
      description: 'Equity securities and ETFs'
    },
    { 
      type: 'options' as AssetType, 
      label: 'Options', 
      icon: Layers, 
      color: 'text-purple-400',
      description: 'Options contracts and strategies'
    },
    { 
      type: 'futures' as AssetType, 
      label: 'Futures', 
      icon: Calendar, 
      color: 'text-orange-400',
      description: 'Futures and commodities'
    },
    { 
      type: 'crypto' as AssetType, 
      label: 'Crypto', 
      icon: Bitcoin, 
      color: 'text-yellow-400',
      description: 'Cryptocurrencies and digital assets'
    },
    { 
      type: 'forex' as AssetType, 
      label: 'Forex', 
      icon: Globe, 
      color: 'text-green-400',
      description: 'Foreign exchange pairs'
    },
    { 
      type: 'bonds' as AssetType, 
      label: 'Bonds', 
      icon: Shield, 
      color: 'text-cyan-400',
      description: 'Government and corporate bonds'
    }
  ]

  return (
    <div className="bg-slate-800/30 rounded-lg p-4 border border-slate-700/50">
      <h3 className="font-semibold text-white mb-4">Asset Classes</h3>
      <div className="grid grid-cols-2 lg:grid-cols-3 gap-3">
        {assetTypes.map(({ type, label, icon: Icon, color, description }) => {
          const count = assetCounts[type] || 0
          const isSelected = value === type
          
          return (
            <button
              key={type}
              onClick={() => onChange(type)}
              className={`p-3 rounded-lg text-left transition-all duration-200 border ${
                isSelected
                  ? 'bg-slate-700/50 border-slate-600/50 ring-1 ring-purple-500/30'
                  : 'bg-slate-800/30 border-slate-700/30 hover:bg-slate-700/20 hover:border-slate-600/50'
              }`}
              title={description}
            >
              <div className="flex items-center space-x-2 mb-2">
                <Icon className={`w-5 h-5 ${color}`} />
                <span className="font-medium text-white">{label}</span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-xs text-slate-400">{count} available</span>
                {isSelected && (
                  <div className="w-2 h-2 bg-purple-400 rounded-full animate-pulse" />
                )}
              </div>
            </button>
          )
        })}
      </div>
    </div>
  )
}

const EquityTrader: React.FC<{
  assets: AssetContract[]
  onOrderSubmit: (order: MultiAssetOrder) => Promise<void>
}> = ({ assets, onOrderSubmit }) => {
  const [selectedAsset, setSelectedAsset] = useState<AssetContract | null>(assets[0] || null)
  const [orderConfig, setOrderConfig] = useState({
    type: 'market' as const,
    side: 'buy' as const,
    quantity: 10,
    price: selectedAsset?.price || 0
  })

  useEffect(() => {
    if (selectedAsset) {
      setOrderConfig(prev => ({ ...prev, price: selectedAsset.price }))
    }
  }, [selectedAsset])

  const handleSubmitOrder = async () => {
    if (!selectedAsset) return

    const order: MultiAssetOrder = {
      id: Date.now().toString(),
      type: orderConfig.type,
      asset: selectedAsset,
      side: orderConfig.side,
      quantity: orderConfig.quantity,
      price: orderConfig.type !== 'market' ? orderConfig.price : undefined,
      status: 'draft',
      createdAt: new Date()
    }

    await onOrderSubmit(order)
  }

  return (
    <div className="space-y-4">
      {/* Asset Selection */}
      <div className="bg-slate-800/30 rounded-lg p-4 border border-slate-700/50">
        <h4 className="font-semibold text-white mb-3">Select Stock</h4>
        <div className="relative">
          <Search className="absolute left-3 top-3 w-4 h-4 text-slate-400" />
          <input
            type="text"
            placeholder="Search stocks..."
            className="w-full bg-slate-700 border border-slate-600 rounded-lg pl-10 pr-4 py-2 text-white focus:ring-2 focus:ring-blue-500"
          />
        </div>
        
        <div className="mt-3 max-h-48 overflow-y-auto space-y-2">
          {assets.slice(0, 5).map(asset => (
            <button
              key={asset.id}
              onClick={() => setSelectedAsset(asset)}
              className={`w-full p-3 rounded-lg text-left transition-colors ${
                selectedAsset?.id === asset.id
                  ? 'bg-blue-500/20 border border-blue-400/30'
                  : 'bg-slate-700/20 hover:bg-slate-600/30'
              }`}
            >
              <div className="flex items-center justify-between">
                <div>
                  <div className="font-medium text-white">{asset.symbol}</div>
                  <div className="text-sm text-slate-400">{asset.name}</div>
                </div>
                <div className="text-right">
                  <div className="font-mono text-white">₹{asset.price.toFixed(2)}</div>
                  <div className={`text-sm ${asset.change >= 0 ? 'text-green-400' : 'text-red-400'}`}>
                    {asset.change >= 0 ? '+' : ''}₹{asset.change.toFixed(2)} ({asset.changePercent.toFixed(2)}%)
                  </div>
                </div>
              </div>
            </button>
          ))}
        </div>
      </div>

      {/* Order Form */}
      {selectedAsset && (
        <div className="bg-slate-800/30 rounded-lg p-4 border border-slate-700/50">
          <h4 className="font-semibold text-white mb-3">Place Order - {selectedAsset.symbol}</h4>
          
          <div className="grid grid-cols-2 gap-4 mb-4">
            <div>
              <label className="text-sm text-slate-400 mb-2 block">Order Type</label>
              <select
                value={orderConfig.type}
                onChange={(e) => setOrderConfig(prev => ({ ...prev, type: e.target.value as any }))}
                className="w-full bg-slate-700 border border-slate-600 rounded-lg px-3 py-2 text-white focus:ring-2 focus:ring-blue-500"
              >
                <option value="market">Market</option>
                <option value="limit">Limit</option>
                <option value="stop">Stop</option>
                <option value="stop-limit">Stop Limit</option>
              </select>
            </div>

            <div>
              <label className="text-sm text-slate-400 mb-2 block">Side</label>
              <div className="flex rounded-lg overflow-hidden">
                <button
                  onClick={() => setOrderConfig(prev => ({ ...prev, side: 'buy' }))}
                  className={`flex-1 py-2 px-3 text-sm font-medium transition-colors ${
                    orderConfig.side === 'buy'
                      ? 'bg-green-500 text-white'
                      : 'bg-slate-700 text-slate-300 hover:bg-slate-600'
                  }`}
                >
                  Buy
                </button>
                <button
                  onClick={() => setOrderConfig(prev => ({ ...prev, side: 'sell' }))}
                  className={`flex-1 py-2 px-3 text-sm font-medium transition-colors ${
                    orderConfig.side === 'sell'
                      ? 'bg-red-500 text-white'
                      : 'bg-slate-700 text-slate-300 hover:bg-slate-600'
                  }`}
                >
                  Sell
                </button>
              </div>
            </div>

            <div>
              <label className="text-sm text-slate-400 mb-2 block">Quantity</label>
              <input
                type="number"
                value={orderConfig.quantity}
                onChange={(e) => setOrderConfig(prev => ({ ...prev, quantity: parseInt(e.target.value) || 0 }))}
                className="w-full bg-slate-700 border border-slate-600 rounded-lg px-3 py-2 text-white focus:ring-2 focus:ring-blue-500"
                min="1"
              />
            </div>

            {orderConfig.type !== 'market' && (
              <div>
                <label className="text-sm text-slate-400 mb-2 block">Price</label>
                <div className="relative">
                  <input
                    type="number"
                    value={orderConfig.price}
                    onChange={(e) => setOrderConfig(prev => ({ ...prev, price: parseFloat(e.target.value) || 0 }))}
                    className="w-full bg-slate-700 border border-slate-600 rounded-lg px-3 py-2 text-white pr-8 focus:ring-2 focus:ring-blue-500"
                    step="0.01"
                  />
                  <span className="absolute right-3 top-2 text-xs text-slate-400">₹</span>
                </div>
              </div>
            )}
          </div>

          <button
            onClick={handleSubmitOrder}
            className="w-full bg-gradient-to-r from-blue-500 to-purple-500 hover:from-blue-600 hover:to-purple-600 text-white font-medium py-3 rounded-lg transition-all duration-200"
          >
            {orderConfig.side === 'buy' ? 'Buy' : 'Sell'} {selectedAsset.symbol}
          </button>
        </div>
      )}
    </div>
  )
}

const OptionsTrader: React.FC<{
  assets: AssetContract[]
  onOrderSubmit: (order: MultiAssetOrder) => Promise<void>
}> = ({ assets, onOrderSubmit }) => {
  const [selectedUnderlying, setSelectedUnderlying] = useState<string>('RELIANCE')
  const [selectedExpiration, setSelectedExpiration] = useState<Date>(new Date(2024, 2, 15))
  const [selectedStrategy, setSelectedStrategy] = useState<'single' | 'covered-call' | 'protective-put' | 'straddle' | 'strangle'>('single')
  const [orderLegs, setOrderLegs] = useState<MultiAssetOrderLeg[]>([])

  // Mock options chain data
  const optionsChain: OptionsChain = {
    underlyingSymbol: selectedUnderlying,
    underlyingPrice: 2456.75,
    expirations: [
      {
        date: new Date(2024, 2, 15),
        daysToExpiry: 15,
        strikes: [
          {
            strike: 2400,
            call: {
              symbol: 'RELIANCE240315C2400',
              strike: 2400,
              expiration: new Date(2024, 2, 15),
              type: 'call',
              price: 78.50,
              bid: 78.00,
              ask: 79.00,
              volume: 1250,
              openInterest: 8340,
              impliedVolatility: 0.22,
              delta: 0.75,
              gamma: 0.004,
              theta: -2.1,
              vega: 0.85
            },
            put: {
              symbol: 'RELIANCE240315P2400',
              strike: 2400,
              expiration: new Date(2024, 2, 15),
              type: 'put',
              price: 21.25,
              bid: 20.75,
              ask: 21.75,
              volume: 890,
              openInterest: 5240,
              impliedVolatility: 0.21,
              delta: -0.25,
              gamma: 0.004,
              theta: -1.8,
              vega: 0.82
            }
          },
          {
            strike: 2450,
            call: {
              symbol: 'RELIANCE240315C2450',
              strike: 2450,
              expiration: new Date(2024, 2, 15),
              type: 'call',
              price: 45.30,
              bid: 44.80,
              ask: 45.80,
              volume: 2150,
              openInterest: 12450,
              impliedVolatility: 0.20,
              delta: 0.55,
              gamma: 0.005,
              theta: -2.3,
              vega: 0.92
            },
            put: {
              symbol: 'RELIANCE240315P2450',
              strike: 2450,
              expiration: new Date(2024, 2, 15),
              type: 'put',
              price: 38.75,
              bid: 38.25,
              ask: 39.25,
              volume: 1680,
              openInterest: 9830,
              impliedVolatility: 0.19,
              delta: -0.45,
              gamma: 0.005,
              theta: -2.0,
              vega: 0.88
            }
          },
          {
            strike: 2500,
            call: {
              symbol: 'RELIANCE240315C2500',
              strike: 2500,
              expiration: new Date(2024, 2, 15),
              type: 'call',
              price: 18.60,
              bid: 18.10,
              ask: 19.10,
              volume: 3420,
              openInterest: 18250,
              impliedVolatility: 0.18,
              delta: 0.28,
              gamma: 0.004,
              theta: -1.9,
              vega: 0.75
            },
            put: {
              symbol: 'RELIANCE240315P2500',
              strike: 2500,
              expiration: new Date(2024, 2, 15),
              type: 'put',
              price: 62.85,
              bid: 62.35,
              ask: 63.35,
              volume: 1240,
              openInterest: 7650,
              impliedVolatility: 0.17,
              delta: -0.72,
              gamma: 0.004,
              theta: -2.4,
              vega: 0.71
            }
          }
        ]
      }
    ]
  }

  const strategies = [
    { value: 'single', label: 'Single Option', description: 'Buy or sell individual options' },
    { value: 'covered-call', label: 'Covered Call', description: 'Own stock + sell call option' },
    { value: 'protective-put', label: 'Protective Put', description: 'Own stock + buy put option' },
    { value: 'straddle', label: 'Long Straddle', description: 'Buy call and put at same strike' },
    { value: 'strangle', label: 'Long Strangle', description: 'Buy call and put at different strikes' }
  ]

  return (
    <div className="space-y-4">
      {/* Strategy Selection */}
      <div className="bg-slate-800/30 rounded-lg p-4 border border-slate-700/50">
        <h4 className="font-semibold text-white mb-3">Options Strategy</h4>
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-3">
          {strategies.map(strategy => (
            <button
              key={strategy.value}
              onClick={() => setSelectedStrategy(strategy.value as any)}
              className={`p-3 rounded-lg text-left transition-colors border ${
                selectedStrategy === strategy.value
                  ? 'bg-purple-500/20 border-purple-400/30'
                  : 'bg-slate-700/20 border-slate-600/30 hover:bg-slate-600/30'
              }`}
            >
              <div className="font-medium text-white mb-1">{strategy.label}</div>
              <div className="text-xs text-slate-400">{strategy.description}</div>
            </button>
          ))}
        </div>
      </div>

      {/* Underlying Selection */}
      <div className="bg-slate-800/30 rounded-lg p-4 border border-slate-700/50">
        <h4 className="font-semibold text-white mb-3">Underlying Asset</h4>
        <div className="flex items-center space-x-4">
          <input
            type="text"
            value={selectedUnderlying}
            onChange={(e) => setSelectedUnderlying(e.target.value.toUpperCase())}
            className="flex-1 bg-slate-700 border border-slate-600 rounded-lg px-3 py-2 text-white focus:ring-2 focus:ring-purple-500"
            placeholder="Enter symbol..."
          />
          <div className="text-right">
            <div className="text-lg font-mono text-white">₹{optionsChain.underlyingPrice.toFixed(2)}</div>
            <div className="text-sm text-green-400">+24.50 (1.01%)</div>
          </div>
        </div>
      </div>

      {/* Options Chain */}
      <div className="bg-slate-800/30 rounded-lg p-4 border border-slate-700/50">
        <h4 className="font-semibold text-white mb-3">Options Chain</h4>
        
        {/* Expiration Selector */}
        <div className="mb-4">
          <label className="text-sm text-slate-400 mb-2 block">Expiration</label>
          <select
            value={selectedExpiration.toISOString()}
            onChange={(e) => setSelectedExpiration(new Date(e.target.value))}
            className="bg-slate-700 border border-slate-600 rounded-lg px-3 py-2 text-white focus:ring-2 focus:ring-purple-500"
          >
            {optionsChain.expirations.map(exp => (
              <option key={exp.date.toISOString()} value={exp.date.toISOString()}>
                {exp.date.toLocaleDateString()} ({exp.daysToExpiry} days)
              </option>
            ))}
          </select>
        </div>

        {/* Options Chain Table */}
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-slate-600">
                <th className="text-left py-2 text-slate-400">Calls</th>
                <th className="text-center py-2 text-slate-400">Strike</th>
                <th className="text-right py-2 text-slate-400">Puts</th>
              </tr>
            </thead>
            <tbody>
              {optionsChain.expirations[0]?.strikes.map(strike => (
                <tr key={strike.strike} className="border-b border-slate-700/50">
                  {/* Calls */}
                  <td className="py-2">
                    {strike.call && (
                      <div className="bg-green-500/10 rounded p-2 cursor-pointer hover:bg-green-500/20 transition-colors">
                        <div className="flex items-center justify-between mb-1">
                          <span className="font-mono text-green-400">₹{strike.call.price.toFixed(2)}</span>
                          <span className="text-xs text-slate-400">IV: {(strike.call.impliedVolatility * 100).toFixed(1)}%</span>
                        </div>
                        <div className="flex items-center justify-between text-xs text-slate-400">
                          <span>Vol: {strike.call.volume}</span>
                          <span>OI: {strike.call.openInterest}</span>
                        </div>
                        <div className="text-xs text-slate-500 mt-1">
                          Δ:{strike.call.delta.toFixed(2)} θ:{strike.call.theta.toFixed(1)}
                        </div>
                      </div>
                    )}
                  </td>

                  {/* Strike */}
                  <td className="py-2 text-center">
                    <div className={`font-mono font-bold ${
                      strike.strike <= optionsChain.underlyingPrice ? 'text-green-400' : 'text-red-400'
                    }`}>
                      {strike.strike}
                    </div>
                  </td>

                  {/* Puts */}
                  <td className="py-2">
                    {strike.put && (
                      <div className="bg-red-500/10 rounded p-2 cursor-pointer hover:bg-red-500/20 transition-colors">
                        <div className="flex items-center justify-between mb-1">
                          <span className="text-xs text-slate-400">IV: {(strike.put.impliedVolatility * 100).toFixed(1)}%</span>
                          <span className="font-mono text-red-400">₹{strike.put.price.toFixed(2)}</span>
                        </div>
                        <div className="flex items-center justify-between text-xs text-slate-400">
                          <span>OI: {strike.put.openInterest}</span>
                          <span>Vol: {strike.put.volume}</span>
                        </div>
                        <div className="text-xs text-slate-500 mt-1 text-right">
                          Δ:{strike.put.delta.toFixed(2)} θ:{strike.put.theta.toFixed(1)}
                        </div>
                      </div>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Strategy Builder */}
      {selectedStrategy !== 'single' && (
        <div className="bg-slate-800/30 rounded-lg p-4 border border-slate-700/50">
          <h4 className="font-semibold text-white mb-3">Strategy Builder</h4>
          <div className="text-center py-8 text-slate-400">
            <Layers className="w-12 h-12 mx-auto mb-4 opacity-50" />
            <p className="text-lg font-medium mb-2">Multi-Leg Strategy Builder</p>
            <p className="text-sm">Advanced options strategies coming soon</p>
          </div>
        </div>
      )}
    </div>
  )
}

const CryptoTrader: React.FC<{
  assets: AssetContract[]
  onOrderSubmit: (order: MultiAssetOrder) => Promise<void>
}> = ({ assets, onOrderSubmit }) => {
  const [selectedPair, setSelectedPair] = useState<AssetContract | null>(assets[0] || null)
  const [orderConfig, setOrderConfig] = useState({
    type: 'limit' as const,
    side: 'buy' as const,
    quantity: 0.1,
    price: selectedPair?.price || 0
  })

  // Mock crypto pairs
  const cryptoPairs = [
    {
      id: 'btc-usdt',
      symbol: 'BTC/USDT',
      name: 'Bitcoin / Tether USD',
      type: 'crypto' as AssetType,
      exchange: 'Binance',
      currency: 'USDT',
      baseAsset: 'BTC',
      quoteAsset: 'USDT',
      price: 43250.00,
      change: 1250.00,
      changePercent: 2.98,
      volume: 28473924,
      lastTrade: new Date()
    },
    {
      id: 'eth-usdt',
      symbol: 'ETH/USDT',
      name: 'Ethereum / Tether USD',
      type: 'crypto' as AssetType,
      exchange: 'Binance',
      currency: 'USDT',
      baseAsset: 'ETH',
      quoteAsset: 'USDT',
      price: 2680.50,
      change: -45.25,
      changePercent: -1.66,
      volume: 18395847,
      lastTrade: new Date()
    }
  ] as AssetContract[]

  return (
    <div className="space-y-4">
      {/* Trading Pairs */}
      <div className="bg-slate-800/30 rounded-lg p-4 border border-slate-700/50">
        <h4 className="font-semibold text-white mb-3 flex items-center space-x-2">
          <Bitcoin className="w-5 h-5 text-yellow-400" />
          <span>Crypto Pairs</span>
        </h4>
        
        <div className="space-y-2">
          {cryptoPairs.map(pair => (
            <button
              key={pair.id}
              onClick={() => setSelectedPair(pair)}
              className={`w-full p-3 rounded-lg text-left transition-colors ${
                selectedPair?.id === pair.id
                  ? 'bg-yellow-500/20 border border-yellow-400/30'
                  : 'bg-slate-700/20 hover:bg-slate-600/30'
              }`}
            >
              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-3">
                  <div className="w-8 h-8 rounded-full bg-yellow-500/20 flex items-center justify-center">
                    <Bitcoin className="w-4 h-4 text-yellow-400" />
                  </div>
                  <div>
                    <div className="font-medium text-white">{pair.symbol}</div>
                    <div className="text-sm text-slate-400">{pair.name}</div>
                  </div>
                </div>
                <div className="text-right">
                  <div className="font-mono text-white">${pair.price.toLocaleString()}</div>
                  <div className={`text-sm ${pair.change >= 0 ? 'text-green-400' : 'text-red-400'}`}>
                    {pair.change >= 0 ? '+' : ''}${pair.change.toLocaleString()} ({pair.changePercent.toFixed(2)}%)
                  </div>
                </div>
              </div>
            </button>
          ))}
        </div>
      </div>

      {/* Crypto Order Form */}
      {selectedPair && (
        <div className="bg-slate-800/30 rounded-lg p-4 border border-slate-700/50">
          <h4 className="font-semibold text-white mb-3">Trade {selectedPair.symbol}</h4>
          
          <div className="grid grid-cols-2 gap-4 mb-4">
            <div>
              <label className="text-sm text-slate-400 mb-2 block">Order Type</label>
              <select
                value={orderConfig.type}
                onChange={(e) => setOrderConfig(prev => ({ ...prev, type: e.target.value as any }))}
                className="w-full bg-slate-700 border border-slate-600 rounded-lg px-3 py-2 text-white focus:ring-2 focus:ring-yellow-500"
              >
                <option value="market">Market</option>
                <option value="limit">Limit</option>
                <option value="stop">Stop</option>
                <option value="stop-limit">Stop Limit</option>
              </select>
            </div>

            <div>
              <label className="text-sm text-slate-400 mb-2 block">Side</label>
              <div className="flex rounded-lg overflow-hidden">
                <button
                  onClick={() => setOrderConfig(prev => ({ ...prev, side: 'buy' }))}
                  className={`flex-1 py-2 px-3 text-sm font-medium transition-colors ${
                    orderConfig.side === 'buy'
                      ? 'bg-green-500 text-white'
                      : 'bg-slate-700 text-slate-300 hover:bg-slate-600'
                  }`}
                >
                  Buy
                </button>
                <button
                  onClick={() => setOrderConfig(prev => ({ ...prev, side: 'sell' }))}
                  className={`flex-1 py-2 px-3 text-sm font-medium transition-colors ${
                    orderConfig.side === 'sell'
                      ? 'bg-red-500 text-white'
                      : 'bg-slate-700 text-slate-300 hover:bg-slate-600'
                  }`}
                >
                  Sell
                </button>
              </div>
            </div>

            <div>
              <label className="text-sm text-slate-400 mb-2 block">Quantity ({selectedPair.baseAsset})</label>
              <input
                type="number"
                value={orderConfig.quantity}
                onChange={(e) => setOrderConfig(prev => ({ ...prev, quantity: parseFloat(e.target.value) || 0 }))}
                className="w-full bg-slate-700 border border-slate-600 rounded-lg px-3 py-2 text-white focus:ring-2 focus:ring-yellow-500"
                step="0.001"
                min="0.001"
              />
            </div>

            {orderConfig.type !== 'market' && (
              <div>
                <label className="text-sm text-slate-400 mb-2 block">Price ({selectedPair.quoteAsset})</label>
                <input
                  type="number"
                  value={orderConfig.price}
                  onChange={(e) => setOrderConfig(prev => ({ ...prev, price: parseFloat(e.target.value) || 0 }))}
                  className="w-full bg-slate-700 border border-slate-600 rounded-lg px-3 py-2 text-white focus:ring-2 focus:ring-yellow-500"
                  step="0.01"
                />
              </div>
            )}
          </div>

          {/* Order Summary */}
          <div className="bg-slate-700/20 rounded-lg p-3 mb-4">
            <div className="flex items-center justify-between mb-2">
              <span className="text-sm text-slate-400">Total</span>
              <span className="font-mono text-white">
                {(orderConfig.quantity * orderConfig.price).toFixed(2)} {selectedPair.quoteAsset}
              </span>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-sm text-slate-400">Estimated Fee (0.1%)</span>
              <span className="font-mono text-orange-400">
                {(orderConfig.quantity * orderConfig.price * 0.001).toFixed(4)} {selectedPair.quoteAsset}
              </span>
            </div>
          </div>

          <button
            onClick={async () => {
              const order: MultiAssetOrder = {
                id: Date.now().toString(),
                type: orderConfig.type,
                asset: selectedPair,
                side: orderConfig.side,
                quantity: orderConfig.quantity,
                price: orderConfig.type !== 'market' ? orderConfig.price : undefined,
                status: 'draft',
                createdAt: new Date()
              }
              await onOrderSubmit(order)
            }}
            className="w-full bg-gradient-to-r from-yellow-500 to-orange-500 hover:from-yellow-600 hover:to-orange-600 text-white font-medium py-3 rounded-lg transition-all duration-200"
          >
            {orderConfig.side === 'buy' ? 'Buy' : 'Sell'} {selectedPair.baseAsset}
          </button>
        </div>
      )}
    </div>
  )
}

export const MultiAssetTradingInterface: React.FC<MultiAssetTradingInterfaceProps> = ({
  selectedAssetType,
  onAssetTypeChange,
  availableAssets,
  onOrderSubmit,
  className = ''
}) => {
  // Calculate asset counts by type
  const assetCounts = useMemo(() => {
    const counts: Record<AssetType, number> = {
      equity: 0,
      options: 0,
      futures: 0,
      crypto: 0,
      forex: 0,
      bonds: 0
    }

    availableAssets.forEach(asset => {
      counts[asset.type]++
    })

    // Add mock counts for demo
    counts.equity = 150
    counts.options = 1250
    counts.futures = 85
    counts.crypto = 45
    counts.forex = 28
    counts.bonds = 12

    return counts
  }, [availableAssets])

  const filteredAssets = availableAssets.filter(asset => asset.type === selectedAssetType)

  const renderTradingInterface = () => {
    switch (selectedAssetType) {
      case 'equity':
        return <EquityTrader assets={filteredAssets} onOrderSubmit={onOrderSubmit} />
      case 'options':
        return <OptionsTrader assets={filteredAssets} onOrderSubmit={onOrderSubmit} />
      case 'crypto':
        return <CryptoTrader assets={filteredAssets} onOrderSubmit={onOrderSubmit} />
      case 'futures':
      case 'forex':
      case 'bonds':
        return (
          <div className="bg-slate-800/30 rounded-lg p-8 border border-slate-700/50">
            <div className="text-center text-slate-400">
              <Layers className="w-16 h-16 mx-auto mb-4 opacity-50" />
              <h3 className="text-xl font-medium mb-2 capitalize">{selectedAssetType} Trading</h3>
              <p className="text-sm">Advanced {selectedAssetType} trading interface coming soon</p>
            </div>
          </div>
        )
      default:
        return null
    }
  }

  return (
    <motion.div
      className={`glass-card rounded-2xl overflow-hidden ${className}`}
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.6 }}
    >
      {/* Header */}
      <div className="flex items-center justify-between p-6 border-b border-slate-700/50">
        <div className="flex items-center space-x-3">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-r from-purple-500 to-blue-500 flex items-center justify-center">
            <Layers className="w-5 h-5 text-white" />
          </div>
          <div>
            <h2 className="text-xl font-bold text-white">Multi-Asset Trading</h2>
            <p className="text-sm text-slate-400">Trade across multiple asset classes</p>
          </div>
        </div>

        <div className="flex items-center space-x-2">
          <div className="px-3 py-1 bg-slate-800/50 rounded-lg">
            <span className="text-sm text-slate-400">Active: </span>
            <span className="text-sm font-medium text-white capitalize">{selectedAssetType}</span>
          </div>
        </div>
      </div>

      <div className="p-6 space-y-6">
        {/* Asset Type Selector */}
        <AssetTypeSelector
          value={selectedAssetType}
          onChange={onAssetTypeChange}
          assetCounts={assetCounts}
        />

        {/* Trading Interface */}
        <AnimatePresence mode="wait">
          <motion.div
            key={selectedAssetType}
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            exit={{ opacity: 0, x: -20 }}
            transition={{ duration: 0.3 }}
          >
            {renderTradingInterface()}
          </motion.div>
        </AnimatePresence>
      </div>
    </motion.div>
  )
}

export default MultiAssetTradingInterface