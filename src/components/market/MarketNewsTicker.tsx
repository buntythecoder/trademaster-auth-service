import React, { useState, useEffect } from 'react'
import { Newspaper, TrendingUp, TrendingDown, Clock, ExternalLink, Filter, Pause, Play } from 'lucide-react'

interface NewsItem {
  id: string
  headline: string
  summary: string
  timestamp: string
  source: string
  category: 'market' | 'company' | 'economy' | 'policy' | 'global'
  impact: 'high' | 'medium' | 'low'
  sentiment: 'positive' | 'negative' | 'neutral'
  relatedSymbols: string[]
  url?: string
}

interface MarketNewsTickerProps {
  height?: number
  autoScroll?: boolean
  showFilters?: boolean
}

const mockNews: NewsItem[] = [
  {
    id: '1',
    headline: 'RBI Announces New Interest Rate Policy',
    summary: 'Reserve Bank of India maintains repo rate at 6.5%, citing inflation concerns',
    timestamp: '2024-01-15T14:30:00Z',
    source: 'Economic Times',
    category: 'policy',
    impact: 'high',
    sentiment: 'neutral',
    relatedSymbols: ['HDFCBANK', 'ICICIBANK', 'SBIN']
  },
  {
    id: '2',
    headline: 'Reliance Industries Reports Strong Q3 Results',
    summary: 'Net profit increases 25% YoY driven by petrochemicals and retail growth',
    timestamp: '2024-01-15T13:45:00Z',
    source: 'Business Standard',
    category: 'company',
    impact: 'medium',
    sentiment: 'positive',
    relatedSymbols: ['RELIANCE']
  },
  {
    id: '3',
    headline: 'IT Sector Faces Headwinds Amid Global Slowdown',
    summary: 'Major IT companies report declining growth rates as global demand weakens',
    timestamp: '2024-01-15T12:20:00Z',
    source: 'Mint',
    category: 'market',
    impact: 'medium',
    sentiment: 'negative',
    relatedSymbols: ['TCS', 'INFY', 'WIPRO']
  },
  {
    id: '4',
    headline: 'FII Outflows Continue for Third Consecutive Week',
    summary: 'Foreign institutional investors withdraw ₹3,500 crores from Indian equities',
    timestamp: '2024-01-15T11:15:00Z',
    source: 'MoneyControl',
    category: 'market',
    impact: 'high',
    sentiment: 'negative',
    relatedSymbols: []
  },
  {
    id: '5',
    headline: 'Auto Sector Shows Signs of Recovery',
    summary: 'Passenger vehicle sales increase 8% MoM driven by festive demand',
    timestamp: '2024-01-15T10:30:00Z',
    source: 'Financial Express',
    category: 'market',
    impact: 'medium',
    sentiment: 'positive',
    relatedSymbols: ['MARUTI', 'TATAMOTORS', 'M&M']
  },
  {
    id: '6',
    headline: 'New GST Rates Announced for Select Industries',
    summary: 'Government reduces GST on renewable energy equipment from 18% to 12%',
    timestamp: '2024-01-15T09:45:00Z',
    source: 'Hindu BusinessLine',
    category: 'policy',
    impact: 'medium',
    sentiment: 'positive',
    relatedSymbols: ['ADANIGREEN', 'SUZLON']
  }
]

export function MarketNewsTicker({ height = 400, autoScroll = true, showFilters = true }: MarketNewsTickerProps) {
  const [news, setNews] = useState<NewsItem[]>(mockNews)
  const [filteredNews, setFilteredNews] = useState<NewsItem[]>(mockNews)
  const [selectedCategory, setSelectedCategory] = useState<string>('all')
  const [selectedImpact, setSelectedImpact] = useState<string>('all')
  const [isScrolling, setIsScrolling] = useState(autoScroll)
  const [currentIndex, setCurrentIndex] = useState(0)

  useEffect(() => {
    // Simulate real-time news updates
    const interval = setInterval(() => {
      // Add a new mock news item occasionally
      if (Math.random() < 0.1) {
        const newItem: NewsItem = {
          id: Date.now().toString(),
          headline: `Breaking: Market Update at ${new Date().toLocaleTimeString()}`,
          summary: 'Latest market developments affecting various sectors and individual stocks',
          timestamp: new Date().toISOString(),
          source: 'TradeMaster News',
          category: 'market',
          impact: 'medium',
          sentiment: Math.random() > 0.5 ? 'positive' : 'negative',
          relatedSymbols: ['NIFTY', 'SENSEX']
        }
        setNews(prev => [newItem, ...prev.slice(0, 10)]) // Keep only last 10 items
      }
    }, 30000) // Check every 30 seconds

    return () => clearInterval(interval)
  }, [])

  useEffect(() => {
    // Filter news based on selected filters
    let filtered = news

    if (selectedCategory !== 'all') {
      filtered = filtered.filter(item => item.category === selectedCategory)
    }

    if (selectedImpact !== 'all') {
      filtered = filtered.filter(item => item.impact === selectedImpact)
    }

    setFilteredNews(filtered)
  }, [news, selectedCategory, selectedImpact])

  useEffect(() => {
    // Auto-scroll through news items
    if (isScrolling && filteredNews.length > 0) {
      const interval = setInterval(() => {
        setCurrentIndex(prev => (prev + 1) % filteredNews.length)
      }, 5000) // Change every 5 seconds

      return () => clearInterval(interval)
    }
  }, [isScrolling, filteredNews.length])

  const getSentimentColor = (sentiment: string) => {
    switch (sentiment) {
      case 'positive': return 'text-green-400'
      case 'negative': return 'text-red-400'
      case 'neutral': return 'text-slate-400'
      default: return 'text-slate-400'
    }
  }

  const getSentimentBg = (sentiment: string) => {
    switch (sentiment) {
      case 'positive': return 'bg-green-500/20'
      case 'negative': return 'bg-red-500/20'
      case 'neutral': return 'bg-slate-500/20'
      default: return 'bg-slate-500/20'
    }
  }

  const getImpactColor = (impact: string) => {
    switch (impact) {
      case 'high': return 'text-red-400'
      case 'medium': return 'text-yellow-400'
      case 'low': return 'text-green-400'
      default: return 'text-slate-400'
    }
  }

  const getCategoryColor = (category: string) => {
    switch (category) {
      case 'market': return 'text-blue-400'
      case 'company': return 'text-purple-400'
      case 'economy': return 'text-cyan-400'
      case 'policy': return 'text-orange-400'
      case 'global': return 'text-pink-400'
      default: return 'text-slate-400'
    }
  }

  const formatTime = (timestamp: string) => {
    const date = new Date(timestamp)
    const now = new Date()
    const diffInMinutes = Math.floor((now.getTime() - date.getTime()) / (1000 * 60))
    
    if (diffInMinutes < 60) {
      return `${diffInMinutes}m ago`
    } else if (diffInMinutes < 1440) {
      return `${Math.floor(diffInMinutes / 60)}h ago`
    } else {
      return date.toLocaleDateString('en-IN', { month: 'short', day: 'numeric' })
    }
  }

  const getSentimentIcon = (sentiment: string) => {
    switch (sentiment) {
      case 'positive': return TrendingUp
      case 'negative': return TrendingDown
      default: return Clock
    }
  }

  return (
    <div className="glass-card rounded-2xl p-6" style={{ height }}>
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div>
          <h3 className="text-lg font-bold text-white flex items-center">
            <Newspaper className="w-5 h-5 mr-2 text-cyan-400" />
            Market News
          </h3>
          <p className="text-sm text-slate-400 mt-1">
            Latest market updates • {filteredNews.length} articles
          </p>
        </div>
        
        <div className="flex items-center space-x-2">
          <button
            onClick={() => setIsScrolling(!isScrolling)}
            className={`p-2 rounded-xl transition-colors ${
              isScrolling
                ? 'bg-green-500/20 text-green-400'
                : 'bg-slate-700/50 text-slate-400 hover:text-white'
            }`}
            title={isScrolling ? 'Pause auto-scroll' : 'Resume auto-scroll'}
          >
            {isScrolling ? <Pause className="w-4 h-4" /> : <Play className="w-4 h-4" />}
          </button>
        </div>
      </div>

      {/* Filters */}
      {showFilters && (
        <div className="flex flex-wrap items-center gap-3 mb-6">
          <div className="flex items-center space-x-2">
            <Filter className="w-4 h-4 text-slate-400" />
            <span className="text-sm text-slate-400">Filters:</span>
          </div>
          
          <select
            value={selectedCategory}
            onChange={(e) => setSelectedCategory(e.target.value)}
            className="cyber-input px-3 py-1.5 text-sm rounded-xl"
          >
            <option value="all">All Categories</option>
            <option value="market">Market</option>
            <option value="company">Company</option>
            <option value="economy">Economy</option>
            <option value="policy">Policy</option>
            <option value="global">Global</option>
          </select>
          
          <select
            value={selectedImpact}
            onChange={(e) => setSelectedImpact(e.target.value)}
            className="cyber-input px-3 py-1.5 text-sm rounded-xl"
          >
            <option value="all">All Impact</option>
            <option value="high">High Impact</option>
            <option value="medium">Medium Impact</option>
            <option value="low">Low Impact</option>
          </select>
        </div>
      )}

      {/* Featured News (Auto-scrolling) */}
      {isScrolling && filteredNews.length > 0 && (
        <div className="mb-6 p-4 rounded-xl bg-gradient-to-r from-purple-500/10 to-cyan-500/10 border border-purple-500/30">
          <div className="flex items-start space-x-4">
            <div className="p-2 rounded-lg bg-purple-500/20">
              <Newspaper className="w-5 h-5 text-purple-400" />
            </div>
            <div className="flex-1">
              <div className="flex items-center space-x-3 mb-2">
                <h4 className="font-semibold text-white leading-tight">
                  {filteredNews[currentIndex]?.headline}
                </h4>
                <div className={`px-2 py-0.5 rounded text-xs font-medium ${
                  getSentimentBg(filteredNews[currentIndex]?.sentiment)
                } ${getSentimentColor(filteredNews[currentIndex]?.sentiment)}`}>
                  {filteredNews[currentIndex]?.sentiment}
                </div>
              </div>
              <p className="text-slate-300 text-sm mb-2">
                {filteredNews[currentIndex]?.summary}
              </p>
              <div className="flex items-center space-x-4 text-xs text-slate-400">
                <span>{filteredNews[currentIndex]?.source}</span>
                <span>{formatTime(filteredNews[currentIndex]?.timestamp)}</span>
                {filteredNews[currentIndex]?.relatedSymbols?.length > 0 && (
                  <div className="flex items-center space-x-1">
                    <span>Related:</span>
                    <div className="flex space-x-1">
                      {filteredNews[currentIndex].relatedSymbols.slice(0, 3).map(symbol => (
                        <span key={symbol} className="px-1.5 py-0.5 bg-slate-700/50 rounded text-cyan-400">
                          {symbol}
                        </span>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* News List */}
      <div className="space-y-3 max-h-64 overflow-y-auto custom-scrollbar">
        {filteredNews.map((item, index) => {
          const SentimentIcon = getSentimentIcon(item.sentiment)
          return (
            <div
              key={item.id}
              className={`p-4 rounded-xl border transition-all cursor-pointer ${
                index === currentIndex && isScrolling
                  ? 'bg-purple-500/10 border-purple-500/30'
                  : 'bg-slate-800/30 border-slate-700/50 hover:border-slate-600/70'
              }`}
            >
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <div className="flex items-start space-x-3 mb-2">
                    <div className={`p-1.5 rounded-lg ${getSentimentBg(item.sentiment)}`}>
                      <SentimentIcon className={`w-4 h-4 ${getSentimentColor(item.sentiment)}`} />
                    </div>
                    
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center space-x-2 mb-1">
                        <h4 className="font-medium text-white hover:text-purple-400 transition-colors leading-tight">
                          {item.headline}
                        </h4>
                        {item.url && (
                          <ExternalLink className="w-3 h-3 text-slate-400 hover:text-white transition-colors flex-shrink-0" />
                        )}
                      </div>
                      
                      <p className="text-slate-300 text-sm mb-2 line-clamp-2">
                        {item.summary}
                      </p>
                      
                      <div className="flex items-center flex-wrap gap-2 text-xs">
                        <span className="text-slate-400">{item.source}</span>
                        <span className="text-slate-500">•</span>
                        <span className="text-slate-400">{formatTime(item.timestamp)}</span>
                        
                        <div className={`px-2 py-0.5 rounded font-medium ${
                          getCategoryColor(item.category) === 'text-blue-400' ? 'bg-blue-500/20 text-blue-400' :
                          getCategoryColor(item.category) === 'text-purple-400' ? 'bg-purple-500/20 text-purple-400' :
                          getCategoryColor(item.category) === 'text-cyan-400' ? 'bg-cyan-500/20 text-cyan-400' :
                          getCategoryColor(item.category) === 'text-orange-400' ? 'bg-orange-500/20 text-orange-400' :
                          'bg-pink-500/20 text-pink-400'
                        }`}>
                          {item.category}
                        </div>
                        
                        <div className={`px-2 py-0.5 rounded font-medium ${
                          item.impact === 'high' ? 'bg-red-500/20 text-red-400' :
                          item.impact === 'medium' ? 'bg-yellow-500/20 text-yellow-400' :
                          'bg-green-500/20 text-green-400'
                        }`}>
                          {item.impact}
                        </div>
                      </div>
                      
                      {item.relatedSymbols.length > 0 && (
                        <div className="flex items-center space-x-2 mt-2">
                          <span className="text-xs text-slate-500">Related:</span>
                          <div className="flex flex-wrap gap-1">
                            {item.relatedSymbols.map(symbol => (
                              <span
                                key={symbol}
                                className="px-1.5 py-0.5 bg-slate-700/50 rounded text-xs text-cyan-400 hover:bg-cyan-500/20 cursor-pointer transition-colors"
                              >
                                {symbol}
                              </span>
                            ))}
                          </div>
                        </div>
                      )}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          )
        })}
        
        {filteredNews.length === 0 && (
          <div className="text-center py-8">
            <Newspaper className="w-12 h-12 text-slate-500 mx-auto mb-4" />
            <p className="text-slate-400">No news articles found</p>
            <p className="text-slate-500 text-sm">Try adjusting your filters</p>
          </div>
        )}
      </div>
    </div>
  )
}