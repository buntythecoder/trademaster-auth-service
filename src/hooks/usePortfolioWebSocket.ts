import { useState, useEffect, useRef, useCallback } from 'react'

export interface HoldingPosition {
  symbol: string
  companyName: string
  quantity: number
  avgPrice: number
  currentPrice: number
  marketValue: number
  dayChange: number
  dayChangePercent: number
  totalReturn: number
  totalReturnPercent: number
  allocation: number
  sector: string
  assetType: 'EQUITY' | 'ETF' | 'MUTUAL_FUND'
  lastUpdated: Date
}

export interface PortfolioSummary {
  currentValue: number
  totalInvested: number
  totalGains: number
  totalLosses: number
  unrealizedPnL: number
  realizedPnL: number
  portfolioReturn: number
  annualizedReturn: number
  dayChange: number
  dayChangePercent: number
  lastUpdated: Date
  isMarketOpen: boolean
}

export interface PortfolioData {
  summary: PortfolioSummary
  holdings: HoldingPosition[]
  performance: PerformanceDataPoint[]
  taxInfo: TaxInfo
  goals: PortfolioGoal[]
}

export interface PerformanceDataPoint {
  timestamp: Date
  portfolioValue: number
  invested: number
  returns: number
  benchmarkValue?: number
}

export interface TaxInfo {
  shortTermGains: number
  longTermGains: number
  shortTermLosses: number
  longTermLosses: number
  netShortTerm: number
  netLongTerm: number
  dividendIncome: number
  projectedTax: number
  taxYear: string
}

export interface PortfolioGoal {
  id: string
  name: string
  targetValue: number
  targetDate: Date
  currentValue: number
  monthlyInvestment: number
  projectedValue: number
  onTrack: boolean
}

const WS_URL = 'ws://localhost:8080/portfolio-ws'

export const usePortfolioWebSocket = () => {
  const [portfolio, setPortfolio] = useState<PortfolioData | null>(null)
  const [isConnected, setIsConnected] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [lastUpdate, setLastUpdate] = useState<Date | null>(null)
  const wsRef = useRef<WebSocket | null>(null)
  const reconnectTimeoutRef = useRef<NodeJS.Timeout | null>(null)
  const reconnectAttemptsRef = useRef(0)

  const connect = useCallback(() => {
    try {
      if (wsRef.current?.readyState === WebSocket.OPEN) {
        return
      }

      const ws = new WebSocket(WS_URL)
      wsRef.current = ws

      ws.onopen = () => {
        console.log('Portfolio WebSocket connected')
        setIsConnected(true)
        setError(null)
        reconnectAttemptsRef.current = 0
        
        // Request initial portfolio data
        ws.send(JSON.stringify({ type: 'get_portfolio' }))
      }

      ws.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data)
          setLastUpdate(new Date())

          switch (data.type) {
            case 'portfolio_snapshot':
              setPortfolio(data.payload)
              break
              
            case 'price_update':
              setPortfolio(prev => {
                if (!prev) return prev
                
                // Update holdings with new prices
                const updatedHoldings = prev.holdings.map(holding => {
                  if (data.payload.symbols.includes(holding.symbol)) {
                    const newPrice = data.payload.prices[holding.symbol]
                    const marketValue = newPrice * holding.quantity
                    const totalReturn = marketValue - (holding.avgPrice * holding.quantity)
                    const totalReturnPercent = (totalReturn / (holding.avgPrice * holding.quantity)) * 100
                    const dayChange = (newPrice - holding.currentPrice) * holding.quantity
                    const dayChangePercent = ((newPrice - holding.currentPrice) / holding.currentPrice) * 100

                    return {
                      ...holding,
                      currentPrice: newPrice,
                      marketValue,
                      totalReturn,
                      totalReturnPercent,
                      dayChange,
                      dayChangePercent,
                      lastUpdated: new Date()
                    }
                  }
                  return holding
                })

                // Recalculate portfolio summary
                const currentValue = updatedHoldings.reduce((sum, holding) => sum + holding.marketValue, 0)
                const totalInvested = updatedHoldings.reduce((sum, holding) => sum + (holding.avgPrice * holding.quantity), 0)
                const unrealizedPnL = currentValue - totalInvested
                const portfolioReturn = (unrealizedPnL / totalInvested) * 100
                const dayChange = updatedHoldings.reduce((sum, holding) => sum + holding.dayChange, 0)
                const dayChangePercent = (dayChange / currentValue) * 100

                return {
                  ...prev,
                  holdings: updatedHoldings,
                  summary: {
                    ...prev.summary,
                    currentValue,
                    unrealizedPnL,
                    portfolioReturn,
                    dayChange,
                    dayChangePercent,
                    lastUpdated: new Date()
                  }
                }
              })
              break

            case 'error':
              setError(data.message)
              break

            default:
              console.log('Unknown message type:', data.type)
          }
        } catch (err) {
          console.error('Error parsing WebSocket message:', err)
        }
      }

      ws.onclose = () => {
        console.log('Portfolio WebSocket disconnected')
        setIsConnected(false)
        
        // Attempt to reconnect with exponential backoff
        const maxAttempts = 5
        if (reconnectAttemptsRef.current < maxAttempts) {
          const delay = Math.min(1000 * Math.pow(2, reconnectAttemptsRef.current), 30000)
          reconnectAttemptsRef.current++
          
          console.log(`Attempting to reconnect in ${delay}ms (attempt ${reconnectAttemptsRef.current}/${maxAttempts})`)
          
          reconnectTimeoutRef.current = setTimeout(() => {
            connect()
          }, delay)
        } else {
          setError('Unable to connect to portfolio service. Please refresh the page.')
        }
      }

      ws.onerror = (error) => {
        // Only log in development mode and reduce noise
        if (import.meta.env.DEV && attemptCount < 2) {
          console.warn('Portfolio WebSocket connection failed - Backend not available')
        }
        setError('Portfolio service unavailable')
      }

    } catch (err) {
      console.error('Error connecting to portfolio WebSocket:', err)
      setError('Failed to establish connection')
    }
  }, [])

  const disconnect = useCallback(() => {
    if (reconnectTimeoutRef.current) {
      clearTimeout(reconnectTimeoutRef.current)
      reconnectTimeoutRef.current = null
    }

    if (wsRef.current) {
      wsRef.current.close()
      wsRef.current = null
    }

    setIsConnected(false)
  }, [])

  useEffect(() => {
    connect()

    return () => {
      disconnect()
    }
  }, [connect, disconnect])

  // Mock data for development when WebSocket is not available
  useEffect(() => {
    if (!isConnected && !portfolio) {
      const timeout = setTimeout(() => {
        const mockPortfolio: PortfolioData = {
          summary: {
            currentValue: 1245678,
            totalInvested: 1122222,
            totalGains: 145678,
            totalLosses: 22222,
            unrealizedPnL: 123456,
            realizedPnL: 22222,
            portfolioReturn: 11.02,
            annualizedReturn: 15.5,
            dayChange: 23456,
            dayChangePercent: 1.92,
            lastUpdated: new Date(),
            isMarketOpen: false
          },
          holdings: [
            {
              symbol: 'RELIANCE',
              companyName: 'Reliance Industries Ltd',
              quantity: 100,
              avgPrice: 2300,
              currentPrice: 2345.60,
              marketValue: 234560,
              dayChange: 2560,
              dayChangePercent: 1.1,
              totalReturn: 4560,
              totalReturnPercent: 2.0,
              allocation: 18.8,
              sector: 'Energy',
              assetType: 'EQUITY',
              lastUpdated: new Date()
            },
            {
              symbol: 'INFY',
              companyName: 'Infosys Limited',
              quantity: 50,
              avgPrice: 1250,
              currentPrice: 1234.50,
              marketValue: 61725,
              dayChange: -775,
              dayChangePercent: -0.6,
              totalReturn: -775,
              totalReturnPercent: -1.2,
              allocation: 4.9,
              sector: 'Technology',
              assetType: 'EQUITY',
              lastUpdated: new Date()
            },
            {
              symbol: 'TCS',
              companyName: 'Tata Consultancy Services',
              quantity: 25,
              avgPrice: 3400,
              currentPrice: 3456.78,
              marketValue: 86420,
              dayChange: 1420,
              dayChangePercent: 1.7,
              totalReturn: 1420,
              totalReturnPercent: 1.7,
              allocation: 6.9,
              sector: 'Technology',
              assetType: 'EQUITY',
              lastUpdated: new Date()
            }
          ],
          performance: [
            { timestamp: new Date('2024-01-01'), portfolioValue: 1000000, invested: 950000, returns: 50000 },
            { timestamp: new Date('2024-02-01'), portfolioValue: 1050000, invested: 980000, returns: 70000 },
            { timestamp: new Date('2024-03-01'), portfolioValue: 1150000, invested: 1020000, returns: 130000 },
            { timestamp: new Date('2024-04-01'), portfolioValue: 1245678, invested: 1122222, returns: 123456 }
          ],
          taxInfo: {
            shortTermGains: 45000,
            longTermGains: 125000,
            shortTermLosses: 5000,
            longTermLosses: 8000,
            netShortTerm: 40000,
            netLongTerm: 117000,
            dividendIncome: 12000,
            projectedTax: 18750,
            taxYear: '2024-25'
          },
          goals: [
            {
              id: '1',
              name: 'Retirement Fund',
              targetValue: 5000000,
              targetDate: new Date('2030-12-31'),
              currentValue: 1245678,
              monthlyInvestment: 25000,
              projectedValue: 4800000,
              onTrack: true
            }
          ]
        }
        setPortfolio(mockPortfolio)
      }, 2000)

      return () => clearTimeout(timeout)
    }
  }, [isConnected, portfolio])

  return {
    portfolio,
    isConnected,
    error,
    lastUpdate,
    connect,
    disconnect
  }
}