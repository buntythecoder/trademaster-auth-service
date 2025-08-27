import { useState, useEffect } from 'react'
import { useWebSocket } from './useWebSocket'

export interface ConsolidatedPortfolio {
  totalValue: number
  dayChange: number
  dayChangePercent: number
  brokerBreakdown: BrokerBreakdown[]
  lastUpdated: Date
  connectionCount: number
}

export interface BrokerBreakdown {
  brokerId: string
  brokerName: string
  value: number
  dayChange: number
  dayChangePercent: number
  connectionStatus: 'connected' | 'disconnected' | 'connecting'
  positions: number
  lastUpdated: Date
}

export interface PortfolioUpdate {
  portfolioData: ConsolidatedPortfolio
  timestamp: Date
}

/**
 * Hook for consolidated portfolio data across multiple brokers
 * Used by MultiBrokerPortfolioWidget for real-time updates
 */
export const useConsolidatedPortfolio = () => {
  const [portfolioData, setPortfolioData] = useState<ConsolidatedPortfolio | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const { connectionStatus, service } = useWebSocket()

  useEffect(() => {
    // Subscribe to real-time portfolio updates
    const handlePortfolioUpdate = (update: PortfolioUpdate) => {
      setPortfolioData(prev => ({
        ...prev,
        ...update.portfolioData,
        lastUpdated: new Date(update.timestamp)
      }))
      setIsLoading(false)
    }

    // Set up WebSocket subscription
    service.on('portfolioUpdate', handlePortfolioUpdate)
    
    // Initial data load - using mock data for development
    const mockPortfolio: ConsolidatedPortfolio = {
      totalValue: 845230,
      dayChange: 12450,
      dayChangePercent: 1.49,
      brokerBreakdown: [
        {
          brokerId: 'zerodha',
          brokerName: 'Zerodha',
          value: 425000,
          dayChange: 8200,
          dayChangePercent: 1.96,
          connectionStatus: 'connected',
          positions: 12,
          lastUpdated: new Date()
        },
        {
          brokerId: 'groww',
          brokerName: 'Groww',
          value: 280000,
          dayChange: 3500,
          dayChangePercent: 1.27,
          connectionStatus: 'connected',
          positions: 8,
          lastUpdated: new Date()
        },
        {
          brokerId: 'angel',
          brokerName: 'Angel One',
          value: 140230,
          dayChange: 750,
          dayChangePercent: 0.54,
          connectionStatus: 'connecting',
          positions: 5,
          lastUpdated: new Date()
        }
      ],
      lastUpdated: new Date(),
      connectionCount: 3
    }

    // Set initial mock data
    setPortfolioData(mockPortfolio)
    setIsLoading(false)

    return () => {
      service.off('portfolioUpdate', handlePortfolioUpdate)
    }
  }, [service])

  return { 
    portfolioData, 
    isLoading, 
    isConnected: connectionStatus === 'CONNECTED' 
  }
}