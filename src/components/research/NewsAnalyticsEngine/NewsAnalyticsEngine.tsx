import React, { useState, useEffect } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import {
  Newspaper, TrendingUp, TrendingDown, Clock, Globe, AlertCircle,
  ThumbsUp, ThumbsDown, Eye, MessageSquare, Share2, Filter,
  Search, Calendar, Tag, BookOpen, ExternalLink, Play,
  BarChart3, PieChart, Activity, Zap, Target, Award,
  ChevronUp, ChevronDown, Star, Heart, Bookmark, RefreshCw
} from 'lucide-react'

// Types for News Analytics
interface NewsArticle {
  id: string
  title: string
  summary: string
  content?: string
  source: NewsSource
  author?: string
  publishedAt: Date
  sentiment: {
    score: number // -1 to 1
    label: 'VERY_NEGATIVE' | 'NEGATIVE' | 'NEUTRAL' | 'POSITIVE' | 'VERY_POSITIVE'
    confidence: number // 0 to 1
  }
  impact: {
    market: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'
    sector?: string[]
    symbols?: string[]
    score: number // 0 to 1
  }
  categories: NewsCategory[]
  tags: string[]
  metrics: {
    views: number
    likes: number
    shares: number
    comments: number
  }
  url: string
  imageUrl?: string
  isBreaking: boolean
  isPremium: boolean
  relevanceScore: number
}

interface NewsSource {
  id: string
  name: string
  logo?: string
  credibility: number // 0 to 1
  bias?: 'LEFT' | 'CENTER' | 'RIGHT'
  category: 'MAINSTREAM' | 'FINANCIAL' | 'TECH' | 'REGULATORY' | 'ANALYSIS'
  verified: boolean
}

interface NewsCategory {
  id: string
  name: string
  color: string
  icon?: string
}

interface SentimentAnalysis {
  overall: {
    score: number
    distribution: {
      positive: number
      negative: number
      neutral: number
    }
    trend: 'IMPROVING' | 'DECLINING' | 'STABLE'
    confidence: number
  }
  byTimeframe: {
    period: '1H' | '6H' | '24H' | '7D'
    score: number
    change: number
  }[]
  bySymbol: {
    symbol: string
    score: number
    volume: number
    articles: number
  }[]
  bySector: {
    sector: string
    score: number
    articles: number
    momentum: number
  }[]
}

interface MarketImpact {
  score: number
  level: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'
  affected: {
    symbols: string[]
    sectors: string[]
    indices: string[]
  }
  prediction: {
    direction: 'BULLISH' | 'BEARISH' | 'NEUTRAL'
    confidence: number
    timeframe: '1D' | '1W' | '1M'
  }
  historicalCorrelation: number
}

interface NewsFilter {
  categories: string[]
  sentiment: string[]
  impact: string[]
  sources: string[]
  timeframe: string
  symbols: string[]
}

// Mock data
const mockSources: NewsSource[] = [
  {
    id: '1',
    name: 'Economic Times',
    logo: '/logos/et.png',
    credibility: 0.85,
    bias: 'CENTER',
    category: 'FINANCIAL',
    verified: true
  },
  {
    id: '2',
    name: 'Bloomberg',
    logo: '/logos/bloomberg.png',
    credibility: 0.92,
    bias: 'CENTER',
    category: 'FINANCIAL',
    verified: true
  },
  {
    id: '3',
    name: 'Reuters',
    logo: '/logos/reuters.png',
    credibility: 0.90,
    bias: 'CENTER',
    category: 'MAINSTREAM',
    verified: true
  },
  {
    id: '4',
    name: 'Moneycontrol',
    logo: '/logos/mc.png',
    credibility: 0.78,
    bias: 'CENTER',
    category: 'FINANCIAL',
    verified: true
  }
]

const mockCategories: NewsCategory[] = [
  { id: '1', name: 'Market News', color: 'blue', icon: 'BarChart3' },
  { id: '2', name: 'Earnings', color: 'green', icon: 'TrendingUp' },
  { id: '3', name: 'Policy', color: 'purple', icon: 'Award' },
  { id: '4', name: 'Technology', color: 'cyan', icon: 'Zap' },
  { id: '5', name: 'Banking', color: 'yellow', icon: 'Target' },
  { id: '6', name: 'Energy', color: 'orange', icon: 'Activity' }
]

const mockNews: NewsArticle[] = [
  {
    id: '1',
    title: 'RBI Keeps Repo Rate Unchanged at 6.5%, Maintains Hawkish Stance',
    summary: 'The Reserve Bank of India maintained its benchmark repo rate at 6.5% for the eighth consecutive time, citing persistent inflation concerns and global economic uncertainties.',
    source: mockSources[0],
    author: 'Rajesh Kumar',
    publishedAt: new Date(Date.now() - 2 * 60 * 60 * 1000), // 2 hours ago
    sentiment: {
      score: -0.2,
      label: 'NEGATIVE',
      confidence: 0.78
    },
    impact: {
      market: 'HIGH',
      sector: ['Banking', 'NBFC', 'Real Estate'],
      symbols: ['HDFCBANK', 'ICICIBANK', 'BAJFINANCE'],
      score: 0.85
    },
    categories: [mockCategories[2], mockCategories[4]], // Policy, Banking
    tags: ['RBI', 'Interest Rates', 'Monetary Policy', 'Inflation'],
    metrics: {
      views: 15420,
      likes: 234,
      shares: 89,
      comments: 67
    },
    url: 'https://example.com/rbi-rate-decision',
    imageUrl: '/images/rbi-news.jpg',
    isBreaking: true,
    isPremium: false,
    relevanceScore: 0.92
  },
  {
    id: '2',
    title: 'Reliance Industries Q3 Results: Beats Estimates with 25% Jump in Net Profit',
    summary: 'Reliance Industries reported a 25% year-on-year increase in consolidated net profit to ₹18,951 crore, driven by strong performance in retail and digital segments.',
    source: mockSources[1],
    author: 'Sarah Johnson',
    publishedAt: new Date(Date.now() - 4 * 60 * 60 * 1000), // 4 hours ago
    sentiment: {
      score: 0.7,
      label: 'POSITIVE',
      confidence: 0.89
    },
    impact: {
      market: 'HIGH',
      sector: ['Oil & Gas', 'Retail', 'Telecom'],
      symbols: ['RELIANCE'],
      score: 0.78
    },
    categories: [mockCategories[1], mockCategories[0]], // Earnings, Market News
    tags: ['Earnings', 'Q3 Results', 'Beat Estimates', 'Retail Growth'],
    metrics: {
      views: 23780,
      likes: 456,
      shares: 123,
      comments: 89
    },
    url: 'https://example.com/reliance-q3-results',
    imageUrl: '/images/reliance-news.jpg',
    isBreaking: false,
    isPremium: true,
    relevanceScore: 0.88
  },
  {
    id: '3',
    title: 'Government Announces ₹1.3 Lakh Crore PLI Scheme Extension for Manufacturing',
    summary: 'The government has announced an extension of the Production Linked Incentive (PLI) scheme with additional allocation of ₹1.3 lakh crore to boost domestic manufacturing.',
    source: mockSources[2],
    author: 'Amit Sharma',
    publishedAt: new Date(Date.now() - 6 * 60 * 60 * 1000), // 6 hours ago
    sentiment: {
      score: 0.5,
      label: 'POSITIVE',
      confidence: 0.82
    },
    impact: {
      market: 'MEDIUM',
      sector: ['Manufacturing', 'Electronics', 'Pharmaceuticals'],
      symbols: ['TATAMOTORS', 'SUNPHARMA', 'WIPRO'],
      score: 0.65
    },
    categories: [mockCategories[2], mockCategories[0]], // Policy, Market News
    tags: ['PLI Scheme', 'Manufacturing', 'Government Policy', 'Make in India'],
    metrics: {
      views: 8900,
      likes: 167,
      shares: 45,
      comments: 23
    },
    url: 'https://example.com/pli-scheme-extension',
    isBreaking: false,
    isPremium: false,
    relevanceScore: 0.73
  }
]

const mockSentimentAnalysis: SentimentAnalysis = {
  overall: {
    score: 0.15,
    distribution: {
      positive: 45,
      negative: 25,
      neutral: 30
    },
    trend: 'IMPROVING',
    confidence: 0.78
  },
  byTimeframe: [
    { period: '1H', score: 0.12, change: 0.05 },
    { period: '6H', score: 0.18, change: 0.08 },
    { period: '24H', score: 0.15, change: -0.03 },
    { period: '7D', score: 0.22, change: 0.10 }
  ],
  bySymbol: [
    { symbol: 'RELIANCE', score: 0.7, volume: 15000, articles: 12 },
    { symbol: 'TCS', score: 0.3, volume: 8900, articles: 8 },
    { symbol: 'HDFCBANK', score: -0.2, volume: 12000, articles: 15 },
    { symbol: 'INFY', score: 0.4, volume: 7600, articles: 6 }
  ],
  bySector: [
    { sector: 'Banking', score: -0.15, articles: 28, momentum: -0.08 },
    { sector: 'IT', score: 0.25, articles: 18, momentum: 0.12 },
    { sector: 'Energy', score: 0.45, articles: 15, momentum: 0.22 },
    { sector: 'Pharma', score: 0.1, articles: 9, momentum: 0.05 }
  ]
}

interface NewsAnalyticsEngineProps {
  className?: string
}

export const NewsAnalyticsEngine: React.FC<NewsAnalyticsEngineProps> = ({
  className = ''
}) => {
  // State management
  const [activeTab, setActiveTab] = useState<'feed' | 'sentiment' | 'impact' | 'sources'>('feed')
  const [selectedTimeframe, setSelectedTimeframe] = useState<'1H' | '6H' | '24H' | '7D'>('24H')
  const [searchQuery, setSearchQuery] = useState('')
  const [showFilters, setShowFilters] = useState(false)
  const [selectedArticle, setSelectedArticle] = useState<NewsArticle | null>(null)
  
  // Data state
  const [news, setNews] = useState<NewsArticle[]>(mockNews)
  const [sentimentData] = useState<SentimentAnalysis>(mockSentimentAnalysis)
  const [isLoading, setIsLoading] = useState(false)
  
  // Filter state
  const [filters, setFilters] = useState<NewsFilter>({
    categories: [],
    sentiment: [],
    impact: [],
    sources: [],
    timeframe: '24H',
    symbols: []
  })

  // Real-time updates simulation
  useEffect(() => {
    const interval = setInterval(() => {
      setNews(prev => prev.map(article => ({
        ...article,
        metrics: {
          ...article.metrics,
          views: article.metrics.views + Math.floor(Math.random() * 10),
          likes: article.metrics.likes + (Math.random() > 0.8 ? 1 : 0)
        }
      })))
    }, 30000) // Update every 30 seconds

    return () => clearInterval(interval)
  }, [])

  // Filter news articles
  const filteredNews = news.filter(article => {
    if (searchQuery && !article.title.toLowerCase().includes(searchQuery.toLowerCase()) &&
        !article.summary.toLowerCase().includes(searchQuery.toLowerCase())) {
      return false
    }
    
    if (filters.categories.length > 0 && 
        !article.categories.some(cat => filters.categories.includes(cat.id))) {
      return false
    }
    
    if (filters.sentiment.length > 0 && !filters.sentiment.includes(article.sentiment.label)) {
      return false
    }
    
    if (filters.impact.length > 0 && !filters.impact.includes(article.impact.market)) {
      return false
    }
    
    return true
  }).sort((a, b) => {
    // Breaking news first, then by relevance score
    if (a.isBreaking && !b.isBreaking) return -1
    if (!a.isBreaking && b.isBreaking) return 1
    return b.relevanceScore - a.relevanceScore
  })

  const handleRefresh = () => {
    setIsLoading(true)
    setTimeout(() => setIsLoading(false), 2000) // Simulate API call
  }

  const getSentimentColor = (sentiment: NewsArticle['sentiment']) => {
    if (sentiment.score >= 0.5) return 'text-green-400'
    if (sentiment.score >= 0.1) return 'text-green-300'
    if (sentiment.score >= -0.1) return 'text-slate-400'
    if (sentiment.score >= -0.5) return 'text-red-300'
    return 'text-red-400'
  }

  const getSentimentBg = (sentiment: NewsArticle['sentiment']) => {
    if (sentiment.score >= 0.5) return 'bg-green-600/20 border-green-600/30'
    if (sentiment.score >= 0.1) return 'bg-green-600/10 border-green-600/20'
    if (sentiment.score >= -0.1) return 'bg-slate-600/20 border-slate-600/30'
    if (sentiment.score >= -0.5) return 'bg-red-600/10 border-red-600/20'
    return 'bg-red-600/20 border-red-600/30'
  }

  const getImpactColor = (impact: NewsArticle['impact']['market']) => {
    const colors = {
      LOW: 'text-slate-400',
      MEDIUM: 'text-yellow-400',
      HIGH: 'text-orange-400',
      CRITICAL: 'text-red-400'
    }
    return colors[impact]
  }

  return (
    <div className={`space-y-6 ${className}`}>
      {/* Header */}
      <div className="glass-card rounded-2xl p-6">
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center space-x-4">
            <div className="w-12 h-12 bg-gradient-to-br from-blue-600 to-purple-600 rounded-xl flex items-center justify-center">
              <Newspaper className="w-6 h-6 text-white" />
            </div>
            <div>
              <h1 className="text-2xl font-bold text-white mb-1">
                News Analytics Engine
              </h1>
              <p className="text-slate-400">
                AI-powered market news analysis with real-time sentiment tracking
              </p>
            </div>
          </div>

          <div className="flex items-center space-x-4">
            <div className="text-right">
              <div className="text-sm text-slate-400">Last Updated</div>
              <div className="text-white font-medium">
                {new Date().toLocaleTimeString()}
              </div>
            </div>
            
            <button
              onClick={handleRefresh}
              disabled={isLoading}
              className="p-2 bg-slate-800/50 hover:bg-slate-700/50 rounded-lg transition-colors disabled:opacity-50"
            >
              <RefreshCw className={`w-5 h-5 text-white ${isLoading ? 'animate-spin' : ''}`} />
            </button>
          </div>
        </div>

        {/* Quick Stats */}
        <div className="grid grid-cols-4 gap-6">
          <div className="text-center">
            <div className="text-2xl font-bold text-white mb-1">
              {news.length}
            </div>
            <p className="text-sm text-slate-400">Articles Today</p>
          </div>
          
          <div className="text-center">
            <div className={`text-2xl font-bold mb-1 ${
              sentimentData.overall.score >= 0 ? 'text-green-400' : 'text-red-400'
            }`}>
              {(sentimentData.overall.score * 100).toFixed(0)}
            </div>
            <p className="text-sm text-slate-400">Sentiment Score</p>
          </div>

          <div className="text-center">
            <div className="text-2xl font-bold text-orange-400 mb-1">
              {news.filter(n => n.impact.market === 'HIGH' || n.impact.market === 'CRITICAL').length}
            </div>
            <p className="text-sm text-slate-400">High Impact</p>
          </div>

          <div className="text-center">
            <div className="text-2xl font-bold text-blue-400 mb-1">
              {news.filter(n => n.isBreaking).length}
            </div>
            <p className="text-sm text-slate-400">Breaking News</p>
          </div>
        </div>
      </div>

      {/* Navigation and Controls */}
      <div className="glass-card rounded-2xl p-6">
        <div className="flex items-center justify-between mb-6">
          <div className="flex space-x-1">
            {[
              { id: 'feed', label: 'News Feed', icon: Newspaper },
              { id: 'sentiment', label: 'Sentiment Analysis', icon: Activity },
              { id: 'impact', label: 'Market Impact', icon: Target },
              { id: 'sources', label: 'Source Analytics', icon: Globe }
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

          {/* Search and Filters */}
          <div className="flex items-center space-x-4">
            <div className="relative">
              <Search className="w-4 h-4 absolute left-3 top-1/2 transform -translate-y-1/2 text-slate-400" />
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Search news..."
                className="pl-10 pr-4 py-2 bg-slate-800/50 border border-slate-700/50 rounded-lg text-white placeholder-slate-400 text-sm w-64 focus:border-blue-500 focus:outline-none"
              />
            </div>
            
            <button
              onClick={() => setShowFilters(!showFilters)}
              className={`p-2 rounded-lg transition-colors ${
                showFilters ? 'bg-blue-600' : 'bg-slate-800/50 hover:bg-slate-700/50'
              }`}
            >
              <Filter className="w-4 h-4 text-white" />
            </button>
          </div>
        </div>

        {/* Filters */}
        <AnimatePresence>
          {showFilters && (
            <motion.div
              initial={{ opacity: 0, height: 0 }}
              animate={{ opacity: 1, height: 'auto' }}
              exit={{ opacity: 0, height: 0 }}
              className="border-t border-slate-700/50 pt-4"
            >
              <div className="grid grid-cols-2 gap-6">
                <div>
                  <label className="text-sm text-slate-400 mb-2 block">Categories</label>
                  <div className="flex flex-wrap gap-2">
                    {mockCategories.map((category) => (
                      <button
                        key={category.id}
                        onClick={() => {
                          const isSelected = filters.categories.includes(category.id)
                          setFilters(prev => ({
                            ...prev,
                            categories: isSelected
                              ? prev.categories.filter(id => id !== category.id)
                              : [...prev.categories, category.id]
                          }))
                        }}
                        className={`px-3 py-1 rounded-lg text-xs font-medium transition-all ${
                          filters.categories.includes(category.id)
                            ? `bg-${category.color}-600 text-white`
                            : 'bg-slate-800/50 text-slate-400 hover:text-white'
                        }`}
                      >
                        {category.name}
                      </button>
                    ))}
                  </div>
                </div>

                <div>
                  <label className="text-sm text-slate-400 mb-2 block">Sentiment</label>
                  <div className="flex flex-wrap gap-2">
                    {['VERY_POSITIVE', 'POSITIVE', 'NEUTRAL', 'NEGATIVE', 'VERY_NEGATIVE'].map((sentiment) => (
                      <button
                        key={sentiment}
                        onClick={() => {
                          const isSelected = filters.sentiment.includes(sentiment)
                          setFilters(prev => ({
                            ...prev,
                            sentiment: isSelected
                              ? prev.sentiment.filter(s => s !== sentiment)
                              : [...prev.sentiment, sentiment]
                          }))
                        }}
                        className={`px-3 py-1 rounded-lg text-xs font-medium transition-all ${
                          filters.sentiment.includes(sentiment)
                            ? 'bg-blue-600 text-white'
                            : 'bg-slate-800/50 text-slate-400 hover:text-white'
                        }`}
                      >
                        {sentiment.replace('_', ' ')}
                      </button>
                    ))}
                  </div>
                </div>
              </div>
            </motion.div>
          )}
        </AnimatePresence>

        {/* Tab Content */}
        <div className="mt-6">
          <AnimatePresence mode="wait">
            {/* News Feed Tab */}
            {activeTab === 'feed' && (
              <motion.div
                key="feed"
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -20 }}
                className="space-y-4"
              >
                {filteredNews.map((article) => (
                  <div
                    key={article.id}
                    className="bg-slate-800/30 rounded-xl p-6 border border-slate-700/30 hover:border-slate-600/50 transition-all cursor-pointer"
                    onClick={() => setSelectedArticle(article)}
                  >
                    <div className="flex items-start space-x-4">
                      {article.imageUrl && (
                        <div className="w-24 h-24 rounded-lg overflow-hidden flex-shrink-0">
                          <div className="w-full h-full bg-gradient-to-br from-slate-600 to-slate-800 flex items-center justify-center">
                            <Newspaper className="w-8 h-8 text-slate-400" />
                          </div>
                        </div>
                      )}

                      <div className="flex-1">
                        {/* Article Header */}
                        <div className="flex items-start justify-between mb-3">
                          <div className="flex items-center space-x-2">
                            {article.isBreaking && (
                              <span className="px-2 py-1 bg-red-600 text-white text-xs font-bold rounded-lg animate-pulse">
                                BREAKING
                              </span>
                            )}
                            {article.isPremium && (
                              <span className="px-2 py-1 bg-yellow-600 text-white text-xs font-bold rounded-lg">
                                PREMIUM
                              </span>
                            )}
                          </div>
                          
                          <div className="text-right">
                            <div className="text-sm text-slate-400">
                              {new Date(article.publishedAt).toLocaleTimeString()}
                            </div>
                            <div className="text-xs text-slate-500">
                              {article.source.name}
                            </div>
                          </div>
                        </div>

                        {/* Article Title */}
                        <h3 className="font-bold text-white text-lg mb-2 line-clamp-2">
                          {article.title}
                        </h3>

                        {/* Article Summary */}
                        <p className="text-slate-300 text-sm mb-3 line-clamp-2">
                          {article.summary}
                        </p>

                        {/* Sentiment and Impact Indicators */}
                        <div className="flex items-center justify-between mb-4">
                          <div className="flex items-center space-x-4">
                            {/* Sentiment */}
                            <div className={`px-3 py-1 rounded-lg border text-sm font-medium ${getSentimentBg(article.sentiment)}`}>
                              <div className="flex items-center space-x-2">
                                {article.sentiment.score >= 0 ? (
                                  <TrendingUp className={`w-4 h-4 ${getSentimentColor(article.sentiment)}`} />
                                ) : (
                                  <TrendingDown className={`w-4 h-4 ${getSentimentColor(article.sentiment)}`} />
                                )}
                                <span className={getSentimentColor(article.sentiment)}>
                                  {article.sentiment.label.replace('_', ' ')}
                                </span>
                                <span className="text-xs text-slate-400">
                                  ({(article.sentiment.confidence * 100).toFixed(0)}%)
                                </span>
                              </div>
                            </div>

                            {/* Market Impact */}
                            <div className="flex items-center space-x-1">
                              <AlertCircle className={`w-4 h-4 ${getImpactColor(article.impact.market)}`} />
                              <span className={`text-sm font-medium ${getImpactColor(article.impact.market)}`}>
                                {article.impact.market} IMPACT
                              </span>
                            </div>
                          </div>

                          {/* Relevance Score */}
                          <div className="text-right">
                            <div className="text-sm text-slate-400">Relevance</div>
                            <div className="text-lg font-bold text-white">
                              {(article.relevanceScore * 100).toFixed(0)}%
                            </div>
                          </div>
                        </div>

                        {/* Categories and Tags */}
                        <div className="flex items-center justify-between">
                          <div className="flex items-center space-x-2">
                            {article.categories.slice(0, 3).map((category) => (
                              <span
                                key={category.id}
                                className={`px-2 py-1 bg-${category.color}-600/20 text-${category.color}-400 rounded-lg text-xs`}
                              >
                                {category.name}
                              </span>
                            ))}
                            {article.impact.symbols && article.impact.symbols.length > 0 && (
                              <div className="flex items-center space-x-1">
                                <Target className="w-3 h-3 text-blue-400" />
                                {article.impact.symbols.slice(0, 2).map((symbol) => (
                                  <span key={symbol} className="text-xs text-blue-400">
                                    {symbol}
                                  </span>
                                ))}
                              </div>
                            )}
                          </div>

                          {/* Engagement Metrics */}
                          <div className="flex items-center space-x-4">
                            <div className="flex items-center space-x-1 text-slate-400">
                              <Eye className="w-4 h-4" />
                              <span className="text-sm">{article.metrics.views.toLocaleString()}</span>
                            </div>
                            <div className="flex items-center space-x-1 text-slate-400">
                              <ThumbsUp className="w-4 h-4" />
                              <span className="text-sm">{article.metrics.likes}</span>
                            </div>
                            <div className="flex items-center space-x-1 text-slate-400">
                              <MessageSquare className="w-4 h-4" />
                              <span className="text-sm">{article.metrics.comments}</span>
                            </div>
                            <button className="text-slate-400 hover:text-white transition-colors">
                              <ExternalLink className="w-4 h-4" />
                            </button>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                ))}
              </motion.div>
            )}

            {/* Sentiment Analysis Tab */}
            {activeTab === 'sentiment' && (
              <motion.div
                key="sentiment"
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -20 }}
                className="grid grid-cols-2 gap-6"
              >
                {/* Overall Sentiment */}
                <div className="bg-slate-800/30 rounded-xl p-6">
                  <h3 className="font-bold text-white mb-4 flex items-center space-x-2">
                    <Activity className="w-5 h-5 text-blue-400" />
                    <span>Overall Market Sentiment</span>
                  </h3>

                  <div className="text-center mb-6">
                    <div className={`text-4xl font-bold mb-2 ${
                      sentimentData.overall.score >= 0 ? 'text-green-400' : 'text-red-400'
                    }`}>
                      {(sentimentData.overall.score * 100).toFixed(0)}
                    </div>
                    <p className="text-slate-400">
                      {sentimentData.overall.trend.charAt(0) + sentimentData.overall.trend.slice(1).toLowerCase()}
                    </p>
                  </div>

                  <div className="space-y-3">
                    <div className="flex justify-between text-sm">
                      <span className="text-green-400">Positive</span>
                      <span className="text-white">{sentimentData.overall.distribution.positive}%</span>
                    </div>
                    <div className="w-full bg-slate-700 rounded-full h-2">
                      <div 
                        className="bg-green-400 h-2 rounded-full"
                        style={{ width: `${sentimentData.overall.distribution.positive}%` }}
                      />
                    </div>

                    <div className="flex justify-between text-sm">
                      <span className="text-slate-400">Neutral</span>
                      <span className="text-white">{sentimentData.overall.distribution.neutral}%</span>
                    </div>
                    <div className="w-full bg-slate-700 rounded-full h-2">
                      <div 
                        className="bg-slate-400 h-2 rounded-full"
                        style={{ width: `${sentimentData.overall.distribution.neutral}%` }}
                      />
                    </div>

                    <div className="flex justify-between text-sm">
                      <span className="text-red-400">Negative</span>
                      <span className="text-white">{sentimentData.overall.distribution.negative}%</span>
                    </div>
                    <div className="w-full bg-slate-700 rounded-full h-2">
                      <div 
                        className="bg-red-400 h-2 rounded-full"
                        style={{ width: `${sentimentData.overall.distribution.negative}%` }}
                      />
                    </div>
                  </div>
                </div>

                {/* By Timeframe */}
                <div className="bg-slate-800/30 rounded-xl p-6">
                  <h3 className="font-bold text-white mb-4">Sentiment by Timeframe</h3>
                  
                  <div className="space-y-4">
                    {sentimentData.byTimeframe.map((item) => (
                      <div key={item.period} className="flex items-center justify-between">
                        <div>
                          <div className="text-white font-medium">{item.period}</div>
                          <div className={`text-sm ${
                            item.change >= 0 ? 'text-green-400' : 'text-red-400'
                          }`}>
                            {item.change >= 0 ? '+' : ''}{(item.change * 100).toFixed(1)}%
                          </div>
                        </div>
                        
                        <div className="text-right">
                          <div className={`text-lg font-bold ${
                            item.score >= 0 ? 'text-green-400' : 'text-red-400'
                          }`}>
                            {(item.score * 100).toFixed(0)}
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>

                {/* By Symbol */}
                <div className="bg-slate-800/30 rounded-xl p-6">
                  <h3 className="font-bold text-white mb-4">Top Symbols by Sentiment</h3>
                  
                  <div className="space-y-3">
                    {sentimentData.bySymbol.map((item) => (
                      <div key={item.symbol} className="flex items-center justify-between">
                        <div>
                          <div className="text-white font-medium">{item.symbol}</div>
                          <div className="text-sm text-slate-400">
                            {item.articles} articles • ₹{(item.volume / 1000).toFixed(1)}K volume
                          </div>
                        </div>
                        
                        <div className={`text-lg font-bold ${
                          item.score >= 0 ? 'text-green-400' : 'text-red-400'
                        }`}>
                          {(item.score * 100).toFixed(0)}
                        </div>
                      </div>
                    ))}
                  </div>
                </div>

                {/* By Sector */}
                <div className="bg-slate-800/30 rounded-xl p-6">
                  <h3 className="font-bold text-white mb-4">Sector Sentiment Analysis</h3>
                  
                  <div className="space-y-3">
                    {sentimentData.bySector.map((item) => (
                      <div key={item.sector} className="flex items-center justify-between">
                        <div>
                          <div className="text-white font-medium">{item.sector}</div>
                          <div className="text-sm text-slate-400">
                            {item.articles} articles
                          </div>
                        </div>
                        
                        <div className="text-right">
                          <div className={`text-lg font-bold ${
                            item.score >= 0 ? 'text-green-400' : 'text-red-400'
                          }`}>
                            {(item.score * 100).toFixed(0)}
                          </div>
                          <div className={`text-xs ${
                            item.momentum >= 0 ? 'text-green-400' : 'text-red-400'
                          }`}>
                            {item.momentum >= 0 ? '↗' : '↘'} {Math.abs(item.momentum * 100).toFixed(1)}%
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              </motion.div>
            )}

            {/* Market Impact Tab */}
            {activeTab === 'impact' && (
              <motion.div
                key="impact"
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -20 }}
                className="text-center py-12"
              >
                <Target className="w-16 h-16 text-slate-400 mx-auto mb-4" />
                <h3 className="text-xl font-semibold text-white mb-2">
                  Market Impact Analysis
                </h3>
                <p className="text-slate-400 max-w-md mx-auto">
                  Advanced market impact prediction and correlation analysis coming soon.
                </p>
              </motion.div>
            )}

            {/* Source Analytics Tab */}
            {activeTab === 'sources' && (
              <motion.div
                key="sources"
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -20 }}
                className="grid grid-cols-2 gap-6"
              >
                {mockSources.map((source) => (
                  <div
                    key={source.id}
                    className="bg-slate-800/30 rounded-xl p-6 border border-slate-700/30"
                  >
                    <div className="flex items-center justify-between mb-4">
                      <div className="flex items-center space-x-3">
                        <div className="w-10 h-10 bg-slate-700 rounded-lg flex items-center justify-center">
                          <Globe className="w-5 h-5 text-slate-400" />
                        </div>
                        <div>
                          <h3 className="font-semibold text-white">{source.name}</h3>
                          <p className="text-sm text-slate-400 capitalize">
                            {source.category.toLowerCase().replace('_', ' ')}
                          </p>
                        </div>
                      </div>
                      
                      {source.verified && (
                        <CheckCircle className="w-5 h-5 text-blue-400" />
                      )}
                    </div>

                    <div className="space-y-3">
                      <div className="flex justify-between">
                        <span className="text-sm text-slate-400">Credibility</span>
                        <span className="text-sm text-white">
                          {(source.credibility * 100).toFixed(0)}%
                        </span>
                      </div>
                      
                      <div className="w-full bg-slate-700 rounded-full h-2">
                        <div 
                          className="bg-blue-400 h-2 rounded-full"
                          style={{ width: `${source.credibility * 100}%` }}
                        />
                      </div>

                      {source.bias && (
                        <div className="flex justify-between">
                          <span className="text-sm text-slate-400">Bias</span>
                          <span className={`text-sm font-medium ${
                            source.bias === 'CENTER' ? 'text-green-400' : 'text-yellow-400'
                          }`}>
                            {source.bias}
                          </span>
                        </div>
                      )}

                      <div className="pt-2 border-t border-slate-700/50">
                        <div className="text-sm text-slate-400 mb-2">Today's Articles</div>
                        <div className="text-2xl font-bold text-white">
                          {news.filter(n => n.source.id === source.id).length}
                        </div>
                      </div>
                    </div>
                  </div>
                ))}
              </motion.div>
            )}
          </AnimatePresence>
        </div>
      </div>

      {/* Article Modal */}
      <AnimatePresence>
        {selectedArticle && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 bg-black/50 backdrop-blur-sm z-50 flex items-center justify-center p-4"
            onClick={() => setSelectedArticle(null)}
          >
            <motion.div
              initial={{ scale: 0.9, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.9, opacity: 0 }}
              className="bg-slate-900 rounded-2xl p-6 max-w-4xl w-full max-h-[90vh] overflow-y-auto"
              onClick={(e) => e.stopPropagation()}
            >
              <div className="flex items-center justify-between mb-6">
                <h2 className="text-2xl font-bold text-white line-clamp-2">
                  {selectedArticle.title}
                </h2>
                <button
                  onClick={() => setSelectedArticle(null)}
                  className="p-2 text-slate-400 hover:text-white transition-colors"
                >
                  ✕
                </button>
              </div>

              <div className="prose prose-invert max-w-none">
                <p className="text-slate-300 text-lg leading-relaxed">
                  {selectedArticle.summary}
                </p>
                
                {selectedArticle.content && (
                  <div className="mt-6">
                    <p className="text-slate-300">{selectedArticle.content}</p>
                  </div>
                )}
              </div>

              <div className="mt-6 pt-6 border-t border-slate-700">
                <div className="flex items-center justify-between">
                  <div className="text-sm text-slate-400">
                    <span>Published by {selectedArticle.source.name}</span>
                    {selectedArticle.author && <span> • {selectedArticle.author}</span>}
                    <span> • {selectedArticle.publishedAt.toLocaleString()}</span>
                  </div>
                  
                  <a
                    href={selectedArticle.url}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="flex items-center space-x-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
                  >
                    <span>Read Full Article</span>
                    <ExternalLink className="w-4 h-4" />
                  </a>
                </div>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  )
}

export default NewsAnalyticsEngine