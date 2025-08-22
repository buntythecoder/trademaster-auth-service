import React, { useState, useEffect } from 'react'
import { Calendar, Clock, TrendingUp, AlertTriangle, Globe, IndianRupee, ChevronLeft, ChevronRight } from 'lucide-react'

interface EconomicEvent {
  id: string
  title: string
  description: string
  country: string
  category: 'monetary_policy' | 'inflation' | 'employment' | 'gdp' | 'trade' | 'corporate'
  importance: 'high' | 'medium' | 'low'
  scheduled: string
  actual?: string | number
  forecast?: string | number
  previous?: string | number
  impact: 'positive' | 'negative' | 'neutral'
  currency: string
  relatedInstruments: string[]
}

interface EconomicCalendarProps {
  height?: number
  showFilters?: boolean
}

const mockEvents: EconomicEvent[] = [
  {
    id: '1',
    title: 'RBI Monetary Policy Decision',
    description: 'Reserve Bank of India announces policy rates and monetary stance',
    country: 'India',
    category: 'monetary_policy',
    importance: 'high',
    scheduled: '2024-01-16T10:00:00Z',
    forecast: '6.50%',
    previous: '6.50%',
    impact: 'neutral',
    currency: 'INR',
    relatedInstruments: ['NIFTY', 'BANKNIFTY', 'USDINR']
  },
  {
    id: '2',
    title: 'India CPI Inflation (YoY)',
    description: 'Consumer Price Index year-over-year inflation rate',
    country: 'India',
    category: 'inflation',
    importance: 'high',
    scheduled: '2024-01-17T17:30:00Z',
    actual: '5.69%',
    forecast: '5.80%',
    previous: '5.55%',
    impact: 'positive',
    currency: 'INR',
    relatedInstruments: ['NIFTY', 'FMCG', 'CONSUMER']
  },
  {
    id: '3',
    title: 'US Federal Reserve Meeting',
    description: 'FOMC meeting and interest rate decision',
    country: 'United States',
    category: 'monetary_policy',
    importance: 'high',
    scheduled: '2024-01-18T19:30:00Z',
    forecast: '5.50%',
    previous: '5.50%',
    impact: 'neutral',
    currency: 'USD',
    relatedInstruments: ['NIFTY', 'IT', 'USDINR']
  },
  {
    id: '4',
    title: 'India GDP Growth (QoQ)',
    description: 'Gross Domestic Product quarterly growth rate',
    country: 'India',
    category: 'gdp',
    importance: 'high',
    scheduled: '2024-01-19T17:30:00Z',
    forecast: '6.8%',
    previous: '7.6%',
    impact: 'negative',
    currency: 'INR',
    relatedInstruments: ['NIFTY', 'BANKNIFTY', 'INFRASTRUCTURE']
  },
  {
    id: '5',
    title: 'India Trade Balance',
    description: 'Difference between exports and imports',
    country: 'India',
    category: 'trade',
    importance: 'medium',
    scheduled: '2024-01-20T17:30:00Z',
    forecast: '-$20.5B',
    previous: '-$19.8B',
    impact: 'negative',
    currency: 'INR',
    relatedInstruments: ['USDINR', 'EXPORT', 'COMMODITIES']
  },
  {
    id: '6',
    title: 'Reliance Industries Q3 Results',
    description: 'Quarterly earnings announcement',
    country: 'India',
    category: 'corporate',
    importance: 'medium',
    scheduled: '2024-01-21T15:30:00Z',
    forecast: '₹18,500 Cr',
    previous: '₹17,200 Cr',
    impact: 'positive',
    currency: 'INR',
    relatedInstruments: ['RELIANCE', 'ENERGY', 'NIFTY50']
  },
  {
    id: '7',
    title: 'India Industrial Production',
    description: 'Index of Industrial Production (IIP) monthly change',
    country: 'India',
    category: 'employment',
    importance: 'medium',
    scheduled: '2024-01-22T17:30:00Z',
    forecast: '3.2%',
    previous: '2.4%',
    impact: 'positive',
    currency: 'INR',
    relatedInstruments: ['NIFTY', 'MANUFACTURING', 'INFRASTRUCTURE']
  }
]

export function EconomicCalendar({ height = 500, showFilters = true }: EconomicCalendarProps) {
  const [events, setEvents] = useState<EconomicEvent[]>(mockEvents)
  const [filteredEvents, setFilteredEvents] = useState<EconomicEvent[]>(mockEvents)
  const [selectedDate, setSelectedDate] = useState(new Date())
  const [viewMode, setViewMode] = useState<'day' | 'week' | 'month'>('week')
  const [selectedImportance, setSelectedImportance] = useState<string>('all')
  const [selectedCountry, setSelectedCountry] = useState<string>('all')
  const [selectedCategory, setSelectedCategory] = useState<string>('all')

  useEffect(() => {
    // Filter events based on selected filters
    let filtered = events

    if (selectedImportance !== 'all') {
      filtered = filtered.filter(event => event.importance === selectedImportance)
    }

    if (selectedCountry !== 'all') {
      filtered = filtered.filter(event => event.country === selectedCountry)
    }

    if (selectedCategory !== 'all') {
      filtered = filtered.filter(event => event.category === selectedCategory)
    }

    // Filter by date range based on view mode
    const now = new Date()
    const startOfDay = new Date(selectedDate.getFullYear(), selectedDate.getMonth(), selectedDate.getDate())
    
    if (viewMode === 'day') {
      const endOfDay = new Date(startOfDay.getTime() + 24 * 60 * 60 * 1000)
      filtered = filtered.filter(event => {
        const eventDate = new Date(event.scheduled)
        return eventDate >= startOfDay && eventDate < endOfDay
      })
    } else if (viewMode === 'week') {
      const startOfWeek = new Date(startOfDay.getTime() - startOfDay.getDay() * 24 * 60 * 60 * 1000)
      const endOfWeek = new Date(startOfWeek.getTime() + 7 * 24 * 60 * 60 * 1000)
      filtered = filtered.filter(event => {
        const eventDate = new Date(event.scheduled)
        return eventDate >= startOfWeek && eventDate < endOfWeek
      })
    }

    setFilteredEvents(filtered.sort((a, b) => new Date(a.scheduled).getTime() - new Date(b.scheduled).getTime()))
  }, [events, selectedDate, viewMode, selectedImportance, selectedCountry, selectedCategory])

  const getImportanceColor = (importance: string) => {
    switch (importance) {
      case 'high': return 'text-red-400'
      case 'medium': return 'text-yellow-400'
      case 'low': return 'text-green-400'
      default: return 'text-slate-400'
    }
  }

  const getImportanceBg = (importance: string) => {
    switch (importance) {
      case 'high': return 'bg-red-500/20'
      case 'medium': return 'bg-yellow-500/20'
      case 'low': return 'bg-green-500/20'
      default: return 'bg-slate-500/20'
    }
  }

  const getImpactColor = (impact: string) => {
    switch (impact) {
      case 'positive': return 'text-green-400'
      case 'negative': return 'text-red-400'
      case 'neutral': return 'text-slate-400'
      default: return 'text-slate-400'
    }
  }

  const getImpactIcon = (impact: string) => {
    switch (impact) {
      case 'positive': return TrendingUp
      case 'negative': return AlertTriangle
      default: return Clock
    }
  }

  const getCategoryColor = (category: string) => {
    switch (category) {
      case 'monetary_policy': return 'text-purple-400'
      case 'inflation': return 'text-orange-400'
      case 'employment': return 'text-blue-400'
      case 'gdp': return 'text-green-400'
      case 'trade': return 'text-cyan-400'
      case 'corporate': return 'text-pink-400'
      default: return 'text-slate-400'
    }
  }

  const formatDateTime = (dateString: string) => {
    const date = new Date(dateString)
    return {
      date: date.toLocaleDateString('en-IN', { 
        month: 'short', 
        day: 'numeric',
        weekday: 'short'
      }),
      time: date.toLocaleTimeString('en-IN', { 
        hour: '2-digit', 
        minute: '2-digit',
        hour12: true 
      })
    }
  }

  const navigateDate = (direction: 'prev' | 'next') => {
    const newDate = new Date(selectedDate)
    
    if (viewMode === 'day') {
      newDate.setDate(newDate.getDate() + (direction === 'next' ? 1 : -1))
    } else if (viewMode === 'week') {
      newDate.setDate(newDate.getDate() + (direction === 'next' ? 7 : -7))
    } else if (viewMode === 'month') {
      newDate.setMonth(newDate.getMonth() + (direction === 'next' ? 1 : -1))
    }
    
    setSelectedDate(newDate)
  }

  const getDateRangeLabel = () => {
    if (viewMode === 'day') {
      return selectedDate.toLocaleDateString('en-IN', { 
        weekday: 'long',
        year: 'numeric',
        month: 'long',
        day: 'numeric'
      })
    } else if (viewMode === 'week') {
      const startOfWeek = new Date(selectedDate.getTime() - selectedDate.getDay() * 24 * 60 * 60 * 1000)
      const endOfWeek = new Date(startOfWeek.getTime() + 6 * 24 * 60 * 60 * 1000)
      return `${startOfWeek.toLocaleDateString('en-IN', { month: 'short', day: 'numeric' })} - ${endOfWeek.toLocaleDateString('en-IN', { month: 'short', day: 'numeric', year: 'numeric' })}`
    } else {
      return selectedDate.toLocaleDateString('en-IN', { year: 'numeric', month: 'long' })
    }
  }

  const isEventLive = (eventTime: string) => {
    const now = new Date()
    const eventDate = new Date(eventTime)
    const diffInMinutes = Math.abs((now.getTime() - eventDate.getTime()) / (1000 * 60))
    return diffInMinutes <= 30 // Consider live if within 30 minutes
  }

  return (
    <div className="glass-card rounded-2xl p-6" style={{ height }}>
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div>
          <h3 className="text-lg font-bold text-white flex items-center">
            <Calendar className="w-5 h-5 mr-2 text-cyan-400" />
            Economic Calendar
          </h3>
          <p className="text-sm text-slate-400 mt-1">
            {getDateRangeLabel()} • {filteredEvents.length} events
          </p>
        </div>
        
        <div className="flex items-center space-x-2">
          <button
            onClick={() => navigateDate('prev')}
            className="p-2 rounded-xl glass-card text-slate-400 hover:text-white transition-colors"
          >
            <ChevronLeft className="w-4 h-4" />
          </button>
          
          <div className="flex items-center space-x-1">
            {(['day', 'week', 'month'] as const).map(mode => (
              <button
                key={mode}
                onClick={() => setViewMode(mode)}
                className={`px-3 py-1.5 text-sm rounded-xl transition-colors capitalize ${
                  viewMode === mode
                    ? 'bg-purple-500/20 text-purple-400'
                    : 'text-slate-400 hover:text-white hover:bg-slate-700/50'
                }`}
              >
                {mode}
              </button>
            ))}
          </div>
          
          <button
            onClick={() => navigateDate('next')}
            className="p-2 rounded-xl glass-card text-slate-400 hover:text-white transition-colors"
          >
            <ChevronRight className="w-4 h-4" />
          </button>
        </div>
      </div>

      {/* Filters */}
      {showFilters && (
        <div className="grid gap-3 md:grid-cols-4 mb-6">
          <select
            value={selectedImportance}
            onChange={(e) => setSelectedImportance(e.target.value)}
            className="cyber-input px-3 py-2 text-sm rounded-xl"
          >
            <option value="all">All Importance</option>
            <option value="high">High Impact</option>
            <option value="medium">Medium Impact</option>
            <option value="low">Low Impact</option>
          </select>
          
          <select
            value={selectedCountry}
            onChange={(e) => setSelectedCountry(e.target.value)}
            className="cyber-input px-3 py-2 text-sm rounded-xl"
          >
            <option value="all">All Countries</option>
            <option value="India">India</option>
            <option value="United States">United States</option>
            <option value="European Union">European Union</option>
            <option value="China">China</option>
          </select>
          
          <select
            value={selectedCategory}
            onChange={(e) => setSelectedCategory(e.target.value)}
            className="cyber-input px-3 py-2 text-sm rounded-xl"
          >
            <option value="all">All Categories</option>
            <option value="monetary_policy">Monetary Policy</option>
            <option value="inflation">Inflation</option>
            <option value="employment">Employment</option>
            <option value="gdp">GDP</option>
            <option value="trade">Trade</option>
            <option value="corporate">Corporate</option>
          </select>
          
          <button
            onClick={() => setSelectedDate(new Date())}
            className="cyber-button px-4 py-2 text-sm rounded-xl"
          >
            Today
          </button>
        </div>
      )}

      {/* Events List */}
      <div className="space-y-3 max-h-80 overflow-y-auto custom-scrollbar">
        {filteredEvents.map((event) => {
          const { date, time } = formatDateTime(event.scheduled)
          const ImpactIcon = getImpactIcon(event.impact)
          const isLive = isEventLive(event.scheduled)

          return (
            <div
              key={event.id}
              className={`p-4 rounded-xl border transition-all ${
                isLive 
                  ? 'bg-purple-500/10 border-purple-500/50 animate-pulse' 
                  : 'bg-slate-800/30 border-slate-700/50 hover:border-slate-600/70'
              }`}
            >
              <div className="flex items-start justify-between">
                <div className="flex items-start space-x-4 flex-1">
                  {/* Time and Country */}
                  <div className="text-center min-w-[80px]">
                    <div className="text-white font-semibold text-sm">{time}</div>
                    <div className="text-slate-400 text-xs">{date}</div>
                    <div className="flex items-center justify-center mt-1">
                      <span className="text-xs text-slate-500">{event.country === 'India' ? '🇮🇳' : '🇺🇸'}</span>
                    </div>
                  </div>
                  
                  {/* Event Details */}
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center space-x-3 mb-2">
                      <h4 className="font-semibold text-white leading-tight">{event.title}</h4>
                      {isLive && (
                        <span className="px-2 py-0.5 bg-red-500 text-white text-xs font-bold rounded animate-pulse">
                          LIVE
                        </span>
                      )}
                      <div className={`px-2 py-0.5 rounded text-xs font-medium ${
                        getImportanceBg(event.importance)} ${getImportanceColor(event.importance)
                      }`}>
                        {event.importance.toUpperCase()}
                      </div>
                    </div>
                    
                    <p className="text-slate-300 text-sm mb-3">{event.description}</p>
                    
                    {/* Data Points */}
                    {(event.actual !== undefined || event.forecast !== undefined) && (
                      <div className="grid gap-2 md:grid-cols-3 text-sm mb-3">
                        {event.actual !== undefined && (
                          <div>
                            <div className="text-slate-400 text-xs">Actual</div>
                            <div className="font-semibold text-white">{event.actual}</div>
                          </div>
                        )}
                        {event.forecast !== undefined && (
                          <div>
                            <div className="text-slate-400 text-xs">Forecast</div>
                            <div className="font-semibold text-slate-300">{event.forecast}</div>
                          </div>
                        )}
                        {event.previous !== undefined && (
                          <div>
                            <div className="text-slate-400 text-xs">Previous</div>
                            <div className="font-semibold text-slate-300">{event.previous}</div>
                          </div>
                        )}
                      </div>
                    )}
                    
                    {/* Related Instruments */}
                    {event.relatedInstruments.length > 0 && (
                      <div className="flex items-center space-x-2 mb-2">
                        <span className="text-xs text-slate-500">Affects:</span>
                        <div className="flex flex-wrap gap-1">
                          {event.relatedInstruments.map(instrument => (
                            <span
                              key={instrument}
                              className="px-2 py-0.5 bg-slate-700/50 rounded text-xs text-cyan-400 hover:bg-cyan-500/20 cursor-pointer transition-colors"
                            >
                              {instrument}
                            </span>
                          ))}
                        </div>
                      </div>
                    )}
                    
                    {/* Category and Impact */}
                    <div className="flex items-center space-x-3 text-xs">
                      <div className={`px-2 py-1 rounded font-medium ${
                        event.category === 'monetary_policy' ? 'bg-purple-500/20 text-purple-400' :
                        event.category === 'inflation' ? 'bg-orange-500/20 text-orange-400' :
                        event.category === 'employment' ? 'bg-blue-500/20 text-blue-400' :
                        event.category === 'gdp' ? 'bg-green-500/20 text-green-400' :
                        event.category === 'trade' ? 'bg-cyan-500/20 text-cyan-400' :
                        'bg-pink-500/20 text-pink-400'
                      }`}>
                        {event.category.replace('_', ' ').toUpperCase()}
                      </div>
                      
                      <div className={`flex items-center space-x-1 ${getImpactColor(event.impact)}`}>
                        <ImpactIcon className="w-3 h-3" />
                        <span className="capitalize">{event.impact}</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          )
        })}
        
        {filteredEvents.length === 0 && (
          <div className="text-center py-8">
            <Calendar className="w-12 h-12 text-slate-500 mx-auto mb-4" />
            <p className="text-slate-400">No economic events found</p>
            <p className="text-slate-500 text-sm">Try adjusting your filters or date range</p>
          </div>
        )}
      </div>
    </div>
  )
}