// Portfolio Analytics Service - FRONT-003 Implementation
// Advanced portfolio analytics with mock data integration

import { mockTradingEngine, MockPosition, MockTrade } from './mockTradingEngine'

export interface PortfolioPerformance {
  date: Date
  totalValue: number
  totalPnL: number
  totalPnLPercent: number
  dayPnL: number
  dayPnLPercent: number
  cashBalance: number
  investedAmount: number
}

export interface AssetAllocation {
  category: string
  value: number
  percentage: number
  change: number
  changePercent: number
  color: string
}

export interface SectorAllocation {
  sector: string
  value: number
  percentage: number
  positions: number
  avgReturn: number
  risk: 'LOW' | 'MEDIUM' | 'HIGH'
  color: string
}

export interface RiskMetrics {
  sharpeRatio: number
  volatility: number
  beta: number
  alpha: number
  maxDrawdown: number
  var95: number // Value at Risk 95%
  var99: number // Value at Risk 99%
  informationRatio: number
  treynorRatio: number
  calmarRatio: number
}

export interface PerformanceComparison {
  period: '1D' | '1W' | '1M' | '3M' | '6M' | '1Y' | 'YTD' | 'ALL'
  portfolio: {
    return: number
    volatility: number
    sharpeRatio: number
  }
  benchmarks: {
    [key: string]: {
      name: string
      return: number
      volatility: number
      sharpeRatio: number
    }
  }
}

export interface HoldingAnalysis {
  symbol: string
  companyName: string
  currentValue: number
  allocation: number
  performance: {
    return1D: number
    return1W: number
    return1M: number
    return3M: number
    return1Y: number
    volatility: number
    beta: number
    sharpeRatio: number
  }
  risk: {
    var95: number
    maxDrawdown: number
    correlation: number
  }
  fundamentals: {
    pe: number
    pb: number
    marketCap: number
    sector: string
    industry: string
  }
}

export interface CorrelationMatrix {
  symbols: string[]
  correlations: number[][]
  riskContributions: number[]
}

export interface DiversificationMetrics {
  effectiveNumberOfStocks: number
  concentrationRatio: number
  herfindahlIndex: number
  diversificationRatio: number
  sectorConcentration: Record<string, number>
  currencyExposure: Record<string, number>
}

class PortfolioAnalyticsService {
  private performanceHistory: PortfolioPerformance[] = []
  
  constructor() {
    this.initializeMockData()
  }

  private initializeMockData() {
    // Generate 365 days of historical performance data
    const today = new Date()
    let totalValue = 500000 // Starting portfolio value
    let totalPnL = 0
    
    for (let i = 365; i >= 0; i--) {
      const date = new Date(today)
      date.setDate(date.getDate() - i)
      
      // Simulate daily returns with some volatility
      const dailyReturn = (Math.random() - 0.48) * 0.02 // Slight positive bias
      const dayChange = totalValue * dailyReturn
      
      totalValue += dayChange
      totalPnL += dayChange
      
      this.performanceHistory.push({
        date,
        totalValue,
        totalPnL,
        totalPnLPercent: (totalPnL / 500000) * 100,
        dayPnL: dayChange,
        dayPnLPercent: dailyReturn * 100,
        cashBalance: 50000 + Math.random() * 100000,
        investedAmount: totalValue - (50000 + Math.random() * 100000)
      })
    }
  }

  // Get portfolio performance history
  getPerformanceHistory(
    userId: string,
    period: '1D' | '1W' | '1M' | '3M' | '6M' | '1Y' | 'YTD' | 'ALL' = 'ALL'
  ): PortfolioPerformance[] {
    const endDate = new Date()
    let startDate = new Date()

    switch (period) {
      case '1D':
        startDate.setDate(endDate.getDate() - 1)
        break
      case '1W':
        startDate.setDate(endDate.getDate() - 7)
        break
      case '1M':
        startDate.setMonth(endDate.getMonth() - 1)
        break
      case '3M':
        startDate.setMonth(endDate.getMonth() - 3)
        break
      case '6M':
        startDate.setMonth(endDate.getMonth() - 6)
        break
      case '1Y':
        startDate.setFullYear(endDate.getFullYear() - 1)
        break
      case 'YTD':
        startDate = new Date(endDate.getFullYear(), 0, 1)
        break
      case 'ALL':
        return this.performanceHistory
    }

    return this.performanceHistory.filter(perf => perf.date >= startDate)
  }

  // Get asset allocation breakdown
  getAssetAllocation(userId: string): AssetAllocation[] {
    const positions = mockTradingEngine.getPositions(userId)
    const totalValue = positions.reduce((sum, pos) => sum + pos.marketValue, 0)

    // Group by asset type
    const assetTypes = positions.reduce((acc, pos) => {
      acc[pos.assetType] = (acc[pos.assetType] || 0) + pos.marketValue
      return acc
    }, {} as Record<string, number>)

    const colors = {
      'EQUITY': '#10B981',
      'ETF': '#3B82F6', 
      'MUTUAL_FUND': '#8B5CF6',
      'BOND': '#F59E0B',
      'CASH': '#6B7280'
    }

    const allocations: AssetAllocation[] = Object.entries(assetTypes).map(([category, value]) => ({
      category,
      value,
      percentage: totalValue > 0 ? (value / totalValue) * 100 : 0,
      change: value * (Math.random() - 0.5) * 0.02, // Mock change
      changePercent: (Math.random() - 0.5) * 2,
      color: colors[category as keyof typeof colors] || '#6B7280'
    }))

    // Add cash allocation
    const profile = mockTradingEngine.getProfile(userId)
    if (profile) {
      const cashBalance = profile.accounts.reduce((sum, acc) => sum + acc.balance, 0)
      allocations.push({
        category: 'CASH',
        value: cashBalance,
        percentage: totalValue > 0 ? (cashBalance / (totalValue + cashBalance)) * 100 : 0,
        change: 0,
        changePercent: 0,
        color: colors.CASH
      })
    }

    return allocations.sort((a, b) => b.value - a.value)
  }

  // Get sector allocation breakdown
  getSectorAllocation(userId: string): SectorAllocation[] {
    const positions = mockTradingEngine.getPositions(userId)
    const totalValue = positions.reduce((sum, pos) => sum + pos.marketValue, 0)

    // Group by sector
    const sectors = positions.reduce((acc, pos) => {
      if (!acc[pos.sector]) {
        acc[pos.sector] = {
          value: 0,
          positions: [],
          returns: []
        }
      }
      acc[pos.sector].value += pos.marketValue
      acc[pos.sector].positions.push(pos)
      acc[pos.sector].returns.push(pos.pnlPercent)
      return acc
    }, {} as Record<string, { value: number, positions: MockPosition[], returns: number[] }>)

    const sectorColors = {
      'Information Technology': '#3B82F6',
      'Banking': '#10B981', 
      'Oil & Gas': '#F59E0B',
      'Healthcare': '#EF4444',
      'Telecommunications': '#8B5CF6',
      'FMCG': '#06B6D4',
      'Automobiles': '#F97316',
      'Real Estate': '#84CC16',
      'Metals': '#6B7280',
      'Other': '#9CA3AF'
    }

    return Object.entries(sectors).map(([sector, data]) => {
      const avgReturn = data.returns.reduce((sum, ret) => sum + ret, 0) / data.returns.length || 0
      const volatility = Math.sqrt(
        data.returns.reduce((sum, ret) => sum + Math.pow(ret - avgReturn, 2), 0) / data.returns.length || 0
      )

      return {
        sector,
        value: data.value,
        percentage: totalValue > 0 ? (data.value / totalValue) * 100 : 0,
        positions: data.positions.length,
        avgReturn,
        risk: volatility < 5 ? 'LOW' : volatility < 15 ? 'MEDIUM' : 'HIGH',
        color: sectorColors[sector as keyof typeof sectorColors] || sectorColors.Other
      }
    }).sort((a, b) => b.value - a.value)
  }

  // Calculate comprehensive risk metrics
  getRiskMetrics(userId: string): RiskMetrics {
    const positions = mockTradingEngine.getPositions(userId)
    const performanceData = this.getPerformanceHistory(userId, '1Y')
    
    if (performanceData.length === 0) {
      return {
        sharpeRatio: 0,
        volatility: 0,
        beta: 0,
        alpha: 0,
        maxDrawdown: 0,
        var95: 0,
        var99: 0,
        informationRatio: 0,
        treynorRatio: 0,
        calmarRatio: 0
      }
    }

    // Calculate daily returns
    const returns = performanceData.slice(1).map((perf, i) => 
      ((perf.totalValue - performanceData[i].totalValue) / performanceData[i].totalValue) * 100
    )

    // Basic statistics
    const avgReturn = returns.reduce((sum, ret) => sum + ret, 0) / returns.length || 0
    const variance = returns.reduce((sum, ret) => sum + Math.pow(ret - avgReturn, 2), 0) / returns.length || 0
    const volatility = Math.sqrt(variance) * Math.sqrt(252) // Annualized volatility

    // Sharpe ratio (assuming 5% risk-free rate)
    const riskFreeRate = 5
    const excessReturn = (avgReturn * 252) - riskFreeRate
    const sharpeRatio = volatility > 0 ? excessReturn / volatility : 0

    // Beta calculation (vs market, assuming market return ~12%)
    const marketReturns = returns.map(() => (Math.random() - 0.48) * 0.8) // Mock market returns
    const covariance = returns.reduce((sum, ret, i) => 
      sum + (ret - avgReturn) * (marketReturns[i] - 0.12), 0
    ) / returns.length || 0
    const marketVariance = marketReturns.reduce((sum, ret) => 
      sum + Math.pow(ret - 0.12, 2), 0
    ) / marketReturns.length || 1
    const beta = marketVariance > 0 ? covariance / marketVariance : 1

    // Alpha calculation
    const expectedReturn = riskFreeRate + beta * (12 - riskFreeRate) // CAPM expected return
    const alpha = (avgReturn * 252) - expectedReturn

    // Maximum drawdown
    let peak = performanceData[0].totalValue
    let maxDrawdown = 0
    performanceData.forEach(perf => {
      if (perf.totalValue > peak) {
        peak = perf.totalValue
      } else {
        const drawdown = ((peak - perf.totalValue) / peak) * 100
        maxDrawdown = Math.max(maxDrawdown, drawdown)
      }
    })

    // Value at Risk calculations
    const sortedReturns = returns.sort((a, b) => a - b)
    const var95 = sortedReturns[Math.floor(sortedReturns.length * 0.05)] || 0
    const var99 = sortedReturns[Math.floor(sortedReturns.length * 0.01)] || 0

    // Information ratio (vs benchmark)
    const benchmarkReturns = returns.map(() => 0.8 + Math.random() * 0.4) // Mock benchmark
    const trackingError = Math.sqrt(
      returns.reduce((sum, ret, i) => sum + Math.pow(ret - benchmarkReturns[i], 2), 0) / returns.length
    ) * Math.sqrt(252)
    const informationRatio = trackingError > 0 ? (avgReturn * 252 - 12) / trackingError : 0

    // Treynor ratio
    const treynorRatio = beta !== 0 ? excessReturn / beta : 0

    // Calmar ratio
    const calmarRatio = maxDrawdown > 0 ? (avgReturn * 252) / maxDrawdown : 0

    return {
      sharpeRatio: Number(sharpeRatio.toFixed(2)),
      volatility: Number(volatility.toFixed(2)),
      beta: Number(beta.toFixed(2)),
      alpha: Number(alpha.toFixed(2)),
      maxDrawdown: Number(maxDrawdown.toFixed(2)),
      var95: Number(var95.toFixed(2)),
      var99: Number(var99.toFixed(2)),
      informationRatio: Number(informationRatio.toFixed(2)),
      treynorRatio: Number(treynorRatio.toFixed(2)),
      calmarRatio: Number(calmarRatio.toFixed(2))
    }
  }

  // Get performance comparison with benchmarks
  getPerformanceComparison(
    userId: string,
    period: '1D' | '1W' | '1M' | '3M' | '6M' | '1Y' | 'YTD' | 'ALL' = '1Y'
  ): PerformanceComparison {
    const performanceData = this.getPerformanceHistory(userId, period)
    
    if (performanceData.length < 2) {
      return {
        period,
        portfolio: { return: 0, volatility: 0, sharpeRatio: 0 },
        benchmarks: {}
      }
    }

    const startValue = performanceData[0].totalValue
    const endValue = performanceData[performanceData.length - 1].totalValue
    const portfolioReturn = ((endValue - startValue) / startValue) * 100

    // Calculate portfolio volatility
    const returns = performanceData.slice(1).map((perf, i) => 
      ((perf.totalValue - performanceData[i].totalValue) / performanceData[i].totalValue) * 100
    )
    const avgReturn = returns.reduce((sum, ret) => sum + ret, 0) / returns.length || 0
    const volatility = Math.sqrt(
      returns.reduce((sum, ret) => sum + Math.pow(ret - avgReturn, 2), 0) / returns.length || 0
    )
    const sharpeRatio = volatility > 0 ? (avgReturn - 0.02) / volatility : 0 // Assuming 2% risk-free rate

    // Mock benchmark data
    const benchmarks = {
      'NIFTY50': {
        name: 'NIFTY 50',
        return: portfolioReturn * (0.8 + Math.random() * 0.4), // Similar but different performance
        volatility: volatility * (0.9 + Math.random() * 0.2),
        sharpeRatio: sharpeRatio * (0.8 + Math.random() * 0.4)
      },
      'SENSEX': {
        name: 'BSE SENSEX',
        return: portfolioReturn * (0.85 + Math.random() * 0.3),
        volatility: volatility * (0.95 + Math.random() * 0.1),
        sharpeRatio: sharpeRatio * (0.9 + Math.random() * 0.2)
      },
      'NIFTY100': {
        name: 'NIFTY 100',
        return: portfolioReturn * (0.9 + Math.random() * 0.2),
        volatility: volatility * (1.0 + Math.random() * 0.1),
        sharpeRatio: sharpeRatio * (0.85 + Math.random() * 0.3)
      }
    }

    return {
      period,
      portfolio: {
        return: Number(portfolioReturn.toFixed(2)),
        volatility: Number(volatility.toFixed(2)),
        sharpeRatio: Number(sharpeRatio.toFixed(2))
      },
      benchmarks
    }
  }

  // Get detailed holding analysis
  getHoldingAnalysis(userId: string): HoldingAnalysis[] {
    const positions = mockTradingEngine.getPositions(userId)
    
    return positions.map(position => ({
      symbol: position.symbol,
      companyName: position.companyName,
      currentValue: position.marketValue,
      allocation: position.marketValue, // Will be calculated as percentage in UI
      performance: {
        return1D: position.dayPnLPercent,
        return1W: position.dayPnLPercent * (7 + Math.random() * 3),
        return1M: position.pnlPercent * (0.8 + Math.random() * 0.4),
        return3M: position.pnlPercent * (1.2 + Math.random() * 0.6),
        return1Y: position.pnlPercent * (2 + Math.random()),
        volatility: Math.abs(position.dayPnLPercent) * (2 + Math.random() * 3),
        beta: 0.8 + Math.random() * 0.6,
        sharpeRatio: (Math.random() - 0.3) * 2
      },
      risk: {
        var95: position.dayPnLPercent * -2,
        maxDrawdown: Math.abs(position.pnlPercent) * (1 + Math.random()),
        correlation: -0.5 + Math.random() // Random correlation with portfolio
      },
      fundamentals: {
        pe: 15 + Math.random() * 20,
        pb: 1 + Math.random() * 3,
        marketCap: 50000 + Math.random() * 200000, // Crores
        sector: position.sector,
        industry: this.getIndustry(position.symbol)
      }
    }))
  }

  // Get correlation matrix for positions
  getCorrelationMatrix(userId: string): CorrelationMatrix {
    const positions = mockTradingEngine.getPositions(userId)
    const symbols = positions.map(pos => pos.symbol)
    
    // Generate mock correlation matrix
    const correlations: number[][] = []
    const riskContributions: number[] = []
    
    symbols.forEach((symbol1, i) => {
      const row: number[] = []
      symbols.forEach((symbol2, j) => {
        if (i === j) {
          row.push(1) // Perfect correlation with self
        } else {
          // Generate realistic correlations (higher for same sector)
          const pos1 = positions[i]
          const pos2 = positions[j]
          const sameSector = pos1.sector === pos2.sector
          const baseCorr = sameSector ? 0.3 + Math.random() * 0.4 : -0.2 + Math.random() * 0.4
          row.push(Number(baseCorr.toFixed(2)))
        }
      })
      correlations.push(row)
      
      // Risk contribution (mock calculation)
      const position = positions[i]
      riskContributions.push(
        Number((Math.abs(position.pnlPercent) * position.marketValue / 100000).toFixed(2))
      )
    })

    return {
      symbols,
      correlations,
      riskContributions
    }
  }

  // Get diversification metrics
  getDiversificationMetrics(userId: string): DiversificationMetrics {
    const positions = mockTradingEngine.getPositions(userId)
    const totalValue = positions.reduce((sum, pos) => sum + pos.marketValue, 0)
    
    // Calculate weights
    const weights = positions.map(pos => pos.marketValue / totalValue)
    
    // Effective number of stocks (inverse of sum of squared weights)
    const effectiveNumberOfStocks = 1 / weights.reduce((sum, w) => sum + w * w, 0)
    
    // Concentration ratio (sum of top 3 holdings)
    const sortedWeights = [...weights].sort((a, b) => b - a)
    const concentrationRatio = sortedWeights.slice(0, 3).reduce((sum, w) => sum + w, 0) * 100
    
    // Herfindahl-Hirschman Index
    const herfindahlIndex = weights.reduce((sum, w) => sum + w * w, 0) * 10000
    
    // Diversification ratio (mock calculation)
    const diversificationRatio = Math.min(effectiveNumberOfStocks / positions.length, 1)
    
    // Sector concentration
    const sectorConcentration: Record<string, number> = {}
    positions.forEach(pos => {
      const weight = pos.marketValue / totalValue
      sectorConcentration[pos.sector] = (sectorConcentration[pos.sector] || 0) + weight * 100
    })
    
    // Currency exposure (assuming all INR for now)
    const currencyExposure = { INR: 100 }

    return {
      effectiveNumberOfStocks: Number(effectiveNumberOfStocks.toFixed(1)),
      concentrationRatio: Number(concentrationRatio.toFixed(1)),
      herfindahlIndex: Number(herfindahlIndex.toFixed(0)),
      diversificationRatio: Number(diversificationRatio.toFixed(2)),
      sectorConcentration,
      currencyExposure
    }
  }

  private getIndustry(symbol: string): string {
    const industries: Record<string, string> = {
      'RELIANCE': 'Oil Refining',
      'TCS': 'Software Services',
      'HDFCBANK': 'Private Banks',
      'INFY': 'Software Services',
      'ICICIBANK': 'Private Banks',
      'WIPRO': 'Software Services',
      'KOTAKBANK': 'Private Banks',
      'SBIN': 'Public Banks',
      'BHARTIARTL': 'Telecom Services',
      'ITC': 'Cigarettes & Tobacco'
    }
    return industries[symbol] || 'Other'
  }
}

// Singleton instance
export const portfolioAnalyticsService = new PortfolioAnalyticsService()

export default portfolioAnalyticsService