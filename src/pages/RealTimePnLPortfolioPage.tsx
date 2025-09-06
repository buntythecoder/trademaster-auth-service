import React from 'react'
import RealTimePnLPortfolioIntegration from '../components/trading/RealTimePnLPortfolioIntegration'

export function RealTimePnLPortfolioPage() {
  // Mock data for demonstration - in real implementation, this would come from backend
  const mockPositions = [
    {
      brokerId: 'zerodha-1',
      brokerName: 'Zerodha Kite - Main Account',
      symbol: 'RELIANCE',
      quantity: 50,
      avgPrice: 2450,
      currentPrice: 2520,
      pnl: 3500,
      pnlPercent: 2.86,
      dayPnl: 1200,
      dayPnlPercent: 0.98,
      lastUpdated: new Date()
    },
    {
      brokerId: 'zerodha-1',
      brokerName: 'Zerodha Kite - Main Account',
      symbol: 'TCS',
      quantity: 25,
      avgPrice: 3800,
      currentPrice: 3720,
      pnl: -2000,
      pnlPercent: -2.11,
      dayPnl: -800,
      dayPnlPercent: -0.84,
      lastUpdated: new Date()
    },
    {
      brokerId: 'angel-1',
      brokerName: 'Angel One - Options Trading',
      symbol: 'HDFC BANK',
      quantity: 30,
      avgPrice: 1680,
      currentPrice: 1720,
      pnl: 1200,
      pnlPercent: 2.38,
      dayPnl: 600,
      dayPnlPercent: 1.19,
      lastUpdated: new Date()
    },
    {
      brokerId: 'angel-1',
      brokerName: 'Angel One - Options Trading',
      symbol: 'INFOSYS',
      quantity: 40,
      avgPrice: 1420,
      currentPrice: 1380,
      pnl: -1600,
      pnlPercent: -2.82,
      dayPnl: -400,
      dayPnlPercent: -0.71,
      lastUpdated: new Date()
    },
    {
      brokerId: 'upstox-1',
      brokerName: 'Upstox Pro',
      symbol: 'ICICI BANK',
      quantity: 60,
      avgPrice: 1050,
      currentPrice: 1080,
      pnl: 1800,
      pnlPercent: 2.86,
      dayPnl: 900,
      dayPnlPercent: 1.43,
      lastUpdated: new Date()
    }
  ]

  const mockBrokers = [
    {
      id: 'zerodha-1',
      name: 'Zerodha-Primary',
      brokerType: 'zerodha' as const,
      displayName: 'Zerodha Kite - Main Account',
      status: 'connected' as const,
      isDefault: true,
      capabilities: ['stocks', 'futures', 'options', 'commodity'],
      lastConnected: new Date(),
      accountId: 'ZA1234',
      connectionConfig: {},
      performance: { avgExecutionTime: 180, successRate: 99.1, totalOrders: 2450 },
      balance: { availableMargin: 125000, usedMargin: 78500, totalBalance: 203500 }
    },
    {
      id: 'angel-1',
      name: 'Angel-Options',
      brokerType: 'angel_one' as const,
      displayName: 'Angel One - Options Trading',
      status: 'connected' as const,
      isDefault: false,
      capabilities: ['stocks', 'futures', 'options', 'commodity'],
      lastConnected: new Date(Date.now() - 3 * 60 * 1000),
      accountId: 'AN567890',
      connectionConfig: { sandbox: false },
      performance: { avgExecutionTime: 220, successRate: 98.7, totalOrders: 1230 },
      balance: { availableMargin: 85000, usedMargin: 32000, totalBalance: 117000 }
    },
    {
      id: 'upstox-1',
      name: 'Upstox-Trading',
      brokerType: 'upstox' as const,
      displayName: 'Upstox Pro',
      status: 'connected' as const,
      isDefault: false,
      capabilities: ['stocks', 'futures', 'options'],
      lastConnected: new Date(Date.now() - 10 * 60 * 1000),
      accountId: 'UP789012',
      connectionConfig: { sandbox: false },
      performance: { avgExecutionTime: 200, successRate: 98.9, totalOrders: 890 },
      balance: { availableMargin: 95000, usedMargin: 45000, totalBalance: 140000 }
    }
  ]

  const handleRefresh = () => {
    console.log('Refreshing P&L data...')
    // In real implementation, this would fetch fresh data from the backend
  }

  return (
    <div className="min-h-screen">
      <RealTimePnLPortfolioIntegration 
        positions={mockPositions}
        brokers={mockBrokers}
        onRefresh={handleRefresh}
        isLoading={false}
      />
    </div>
  )
}

export default RealTimePnLPortfolioPage