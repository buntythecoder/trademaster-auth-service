import { useState, useEffect } from 'react'
import { useWebSocket } from './useWebSocket'

export interface TradingOpportunity {
  id: string
  symbol: string
  action: 'BUY' | 'SELL' | 'HOLD'
  confidence: number
  targetPrice: number
  currentPrice: number
  reasoning: string
  timeframe: string
  riskLevel: 'Low' | 'Medium' | 'High'
}

export interface PortfolioHealth {
  diversification: number
  riskScore: number
  performanceScore: number
  liquidityScore: number
}

export interface RiskAlert {
  id: string
  type: string
  severity: 'low' | 'medium' | 'high'
  message: string
  recommendation: string
}

export interface MarketConditions {
  trend: 'Bullish' | 'Bearish' | 'Neutral'
  volatility: string
  sentiment: string
  keyEvents: string[]
}

export interface SmartInsights {
  tradingOpportunities: TradingOpportunity[]
  portfolioHealth: PortfolioHealth
  riskAlerts: RiskAlert[]
  marketConditions: MarketConditions
  lastUpdated: Date
}

/**
 * Hook for AI-powered smart trading insights
 * Used by SmartInsightsPanel for intelligent recommendations
 */
export const useSmartInsights = () => {
  const [insights, setInsights] = useState<SmartInsights | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const { connectionStatus, service } = useWebSocket()

  useEffect(() => {
    // Subscribe to real-time insights updates
    const handleInsightsUpdate = (update: SmartInsights) => {
      setInsights(update)
      setIsLoading(false)
    }

    // Set up WebSocket subscription
    service.on('smartInsights', handleInsightsUpdate)
    
    // Initial mock data for development
    const mockInsights: SmartInsights = {
      tradingOpportunities: [
        {
          id: 'opp-1',
          symbol: 'RELIANCE',
          action: 'BUY',
          confidence: 87,
          targetPrice: 2650,
          currentPrice: 2547,
          reasoning: 'Strong Q3 results expected, oil prices stabilizing',
          timeframe: '2-3 weeks',
          riskLevel: 'Medium'
        },
        {
          id: 'opp-2',
          symbol: 'TCS',
          action: 'HOLD',
          confidence: 73,
          targetPrice: 3800,
          currentPrice: 3642,
          reasoning: 'Consistent performance, but valuations stretched',
          timeframe: '1-2 months',
          riskLevel: 'Low'
        },
        {
          id: 'opp-3',
          symbol: 'HDFC BANK',
          action: 'BUY',
          confidence: 82,
          targetPrice: 1650,
          currentPrice: 1567,
          reasoning: 'Banking sector recovery, strong deposit growth',
          timeframe: '3-4 weeks',
          riskLevel: 'Low'
        }
      ],
      portfolioHealth: {
        diversification: 78,
        riskScore: 65,
        performanceScore: 82,
        liquidityScore: 91
      },
      riskAlerts: [
        {
          id: 'risk-1',
          type: 'concentration',
          severity: 'medium',
          message: 'High concentration in IT sector (35% of portfolio)',
          recommendation: 'Consider diversifying into banking or pharma sectors'
        },
        {
          id: 'risk-2',
          type: 'volatility',
          severity: 'low',
          message: 'Market volatility within normal range',
          recommendation: 'Continue with current strategy'
        }
      ],
      marketConditions: {
        trend: 'Bullish',
        volatility: 'Medium',
        sentiment: 'Positive',
        keyEvents: ['RBI Policy Meet', 'Q3 Results Season', 'Budget 2024']
      },
      lastUpdated: new Date()
    }

    // Set initial mock data
    setInsights(mockInsights)
    setIsLoading(false)

    // Simulate periodic updates
    const updateInterval = setInterval(() => {
      if (mockInsights) {
        const updatedInsights = {
          ...mockInsights,
          lastUpdated: new Date(),
          // Randomly update confidence scores
          tradingOpportunities: mockInsights.tradingOpportunities.map(opp => ({
            ...opp,
            confidence: Math.max(60, Math.min(95, opp.confidence + (Math.random() - 0.5) * 10))
          }))
        }
        setInsights(updatedInsights)
      }
    }, 30000) // Update every 30 seconds

    return () => {
      service.off('smartInsights', handleInsightsUpdate)
      clearInterval(updateInterval)
    }
  }, [service])

  return { 
    insights, 
    isLoading, 
    isConnected: connectionStatus === 'CONNECTED' 
  }
}