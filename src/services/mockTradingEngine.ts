// Mock Trading Engine - Simulates real backend trading functionality
// This will be easily replaceable when real backend is ready

export interface MockBrokerAccount {
  id: string
  brokerId: string
  brokerName: string
  displayName: string
  balance: number
  marginAvailable: number
  marginUsed: number
  status: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED'
  lastUpdated: Date
}

export interface MockOrder {
  id: string
  brokerId: string
  symbol: string
  side: 'BUY' | 'SELL'
  orderType: 'MARKET' | 'LIMIT' | 'STOP_LOSS' | 'BRACKET'
  quantity: number
  price?: number
  stopPrice?: number
  targetPrice?: number
  status: 'PENDING' | 'PLACED' | 'PARTIAL' | 'FILLED' | 'CANCELLED' | 'REJECTED'
  filledQuantity: number
  remainingQuantity: number
  avgFillPrice?: number
  totalAmount: number
  fees: number
  timestamp: Date
  lastUpdated: Date
  rejectReason?: string
}

export interface MockPosition {
  id: string
  brokerId: string
  symbol: string
  companyName: string
  quantity: number
  avgPrice: number
  currentPrice: number
  marketValue: number
  totalInvested: number
  realizedPnL: number
  unrealizedPnL: number
  totalPnL: number
  pnlPercent: number
  dayPnL: number
  dayPnLPercent: number
  sector: string
  assetType: 'EQUITY' | 'ETF' | 'MUTUAL_FUND' | 'BOND'
  lastUpdated: Date
}

export interface MockTrade {
  id: string
  orderId: string
  brokerId: string
  symbol: string
  side: 'BUY' | 'SELL'
  quantity: number
  price: number
  amount: number
  fees: number
  timestamp: Date
}

export interface RiskLimits {
  maxOrderValue: number
  maxPositionSize: number
  maxDayTradingLimit: number
  maxPortfolioExposure: number
  minStopLossPercent: number
}

export interface MockProfile {
  userId: string
  name: string
  accounts: MockBrokerAccount[]
  positions: MockPosition[]
  orders: MockOrder[]
  trades: MockTrade[]
  riskLimits: RiskLimits
  totalBalance: number
  totalMarginUsed: number
  totalMarginAvailable: number
  dayTradingBuyingPower: number
  lastUpdated: Date
}

class MockTradingEngine {
  private profiles = new Map<string, MockProfile>()
  private orderCounter = 1
  private tradeCounter = 1
  private positionCounter = 1

  constructor() {
    this.initializeMockData()
  }

  private initializeMockData() {
    // Create demo user profile
    const demoProfile: MockProfile = {
      userId: 'demo-user',
      name: 'Demo Trader',
      accounts: [
        {
          id: 'acc-zerodha-001',
          brokerId: 'zerodha',
          brokerName: 'ZERODHA',
          displayName: 'Zerodha Account (****1234)',
          balance: 500000,
          marginAvailable: 750000,
          marginUsed: 150000,
          status: 'ACTIVE',
          lastUpdated: new Date()
        },
        {
          id: 'acc-upstox-001',
          brokerId: 'upstox',
          brokerName: 'UPSTOX',
          displayName: 'Upstox Account (****5678)',
          balance: 250000,
          marginAvailable: 375000,
          marginUsed: 75000,
          status: 'ACTIVE',
          lastUpdated: new Date()
        },
        {
          id: 'acc-angelone-001',
          brokerId: 'angelone',
          brokerName: 'ANGEL ONE',
          displayName: 'Angel One Account (****9012)',
          balance: 300000,
          marginAvailable: 450000,
          marginUsed: 100000,
          status: 'ACTIVE',
          lastUpdated: new Date()
        }
      ],
      positions: [
        {
          id: 'pos-001',
          brokerId: 'zerodha',
          symbol: 'RELIANCE',
          companyName: 'Reliance Industries Limited',
          quantity: 100,
          avgPrice: 2320.50,
          currentPrice: 2456.75,
          marketValue: 245675,
          totalInvested: 232050,
          realizedPnL: 0,
          unrealizedPnL: 13625,
          totalPnL: 13625,
          pnlPercent: 5.87,
          dayPnL: 1200,
          dayPnLPercent: 0.49,
          sector: 'Oil & Gas',
          assetType: 'EQUITY',
          lastUpdated: new Date()
        },
        {
          id: 'pos-002',
          brokerId: 'upstox',
          symbol: 'TCS',
          companyName: 'Tata Consultancy Services Limited',
          quantity: 50,
          avgPrice: 3850.25,
          currentPrice: 3789.40,
          marketValue: 189470,
          totalInvested: 192512.50,
          realizedPnL: 0,
          unrealizedPnL: -3042.5,
          totalPnL: -3042.5,
          pnlPercent: -1.58,
          dayPnL: -850,
          dayPnLPercent: -0.44,
          sector: 'Information Technology',
          assetType: 'EQUITY',
          lastUpdated: new Date()
        },
        {
          id: 'pos-003',
          brokerId: 'angelone',
          symbol: 'HDFCBANK',
          companyName: 'HDFC Bank Limited',
          quantity: 75,
          avgPrice: 1645.80,
          currentPrice: 1678.90,
          marketValue: 125917.50,
          totalInvested: 123435,
          realizedPnL: 500,
          unrealizedPnL: 2482.5,
          totalPnL: 2982.5,
          pnlPercent: 2.42,
          dayPnL: 350,
          dayPnLPercent: 0.28,
          sector: 'Banking',
          assetType: 'EQUITY',
          lastUpdated: new Date()
        }
      ],
      orders: [],
      trades: [],
      riskLimits: {
        maxOrderValue: 1000000,
        maxPositionSize: 500000,
        maxDayTradingLimit: 2000000,
        maxPortfolioExposure: 80,
        minStopLossPercent: 5
      },
      totalBalance: 1050000,
      totalMarginUsed: 325000,
      totalMarginAvailable: 1575000,
      dayTradingBuyingPower: 2000000,
      lastUpdated: new Date()
    }

    this.profiles.set('demo-user', demoProfile)
  }

  // Get user profile
  getProfile(userId: string): MockProfile | null {
    return this.profiles.get(userId) || null
  }

  // Get all broker accounts for user
  getBrokerAccounts(userId: string): MockBrokerAccount[] {
    const profile = this.profiles.get(userId)
    return profile?.accounts || []
  }

  // Get positions for user
  getPositions(userId: string, brokerId?: string): MockPosition[] {
    const profile = this.profiles.get(userId)
    if (!profile) return []
    
    return brokerId 
      ? profile.positions.filter(pos => pos.brokerId === brokerId)
      : profile.positions
  }

  // Get orders for user
  getOrders(userId: string, brokerId?: string, limit?: number): MockOrder[] {
    const profile = this.profiles.get(userId)
    if (!profile) return []
    
    let orders = brokerId 
      ? profile.orders.filter(order => order.brokerId === brokerId)
      : profile.orders

    // Sort by timestamp descending
    orders = orders.sort((a, b) => b.timestamp.getTime() - a.timestamp.getTime())
    
    return limit ? orders.slice(0, limit) : orders
  }

  // Get trades for user
  getTrades(userId: string, brokerId?: string, limit?: number): MockTrade[] {
    const profile = this.profiles.get(userId)
    if (!profile) return []
    
    let trades = brokerId 
      ? profile.trades.filter(trade => trade.brokerId === brokerId)
      : profile.trades

    // Sort by timestamp descending
    trades = trades.sort((a, b) => b.timestamp.getTime() - a.timestamp.getTime())
    
    return limit ? trades.slice(0, limit) : trades
  }

  // Place order
  async placeOrder(userId: string, orderRequest: {
    brokerId: string
    symbol: string
    side: 'BUY' | 'SELL'
    orderType: 'MARKET' | 'LIMIT' | 'STOP_LOSS' | 'BRACKET'
    quantity: number
    price?: number
    stopPrice?: number
    targetPrice?: number
  }): Promise<MockOrder> {
    const profile = this.profiles.get(userId)
    if (!profile) {
      throw new Error('User profile not found')
    }

    // Simulate order validation delay
    await new Promise(resolve => setTimeout(resolve, 100))

    // Get current market price (mock)
    const currentPrice = this.getCurrentPrice(orderRequest.symbol)
    const orderPrice = orderRequest.orderType === 'MARKET' ? currentPrice : (orderRequest.price || currentPrice)
    const totalAmount = orderRequest.quantity * orderPrice
    const fees = totalAmount * 0.0003 // 0.03% brokerage

    // Risk validation
    const account = profile.accounts.find(acc => acc.brokerId === orderRequest.brokerId)
    if (!account) {
      throw new Error('Broker account not found')
    }

    if (totalAmount > account.balance) {
      throw new Error('Insufficient balance')
    }

    if (totalAmount > profile.riskLimits.maxOrderValue) {
      throw new Error('Order value exceeds maximum limit')
    }

    // Create order
    const order: MockOrder = {
      id: `ORD-${this.orderCounter++}`,
      brokerId: orderRequest.brokerId,
      symbol: orderRequest.symbol,
      side: orderRequest.side,
      orderType: orderRequest.orderType,
      quantity: orderRequest.quantity,
      price: orderRequest.price,
      stopPrice: orderRequest.stopPrice,
      targetPrice: orderRequest.targetPrice,
      status: 'PENDING',
      filledQuantity: 0,
      remainingQuantity: orderRequest.quantity,
      totalAmount,
      fees,
      timestamp: new Date(),
      lastUpdated: new Date()
    }

    profile.orders.push(order)

    // Simulate order processing
    setTimeout(() => this.processOrder(userId, order.id), 500 + Math.random() * 2000)

    return order
  }

  // Process order (simulate exchange processing)
  private async processOrder(userId: string, orderId: string) {
    const profile = this.profiles.get(userId)
    if (!profile) return

    const order = profile.orders.find(o => o.id === orderId)
    if (!order || order.status !== 'PENDING') return

    // Update order status to PLACED
    order.status = 'PLACED'
    order.lastUpdated = new Date()

    // Simulate execution delay
    setTimeout(() => {
      const executionSuccess = Math.random() > 0.05 // 95% success rate

      if (executionSuccess) {
        this.executeOrder(userId, orderId)
      } else {
        this.rejectOrder(userId, orderId, 'Market conditions unfavorable')
      }
    }, 1000 + Math.random() * 3000)
  }

  // Execute order
  private executeOrder(userId: string, orderId: string) {
    const profile = this.profiles.get(userId)
    if (!profile) return

    const order = profile.orders.find(o => o.id === orderId)
    if (!order || order.status !== 'PLACED') return

    const currentPrice = this.getCurrentPrice(order.symbol)
    const executionPrice = order.orderType === 'MARKET' ? 
      currentPrice + (Math.random() - 0.5) * 0.5 : // Market order slippage
      order.price || currentPrice

    // Execute full order for simplicity (can add partial fills later)
    order.status = 'FILLED'
    order.filledQuantity = order.quantity
    order.remainingQuantity = 0
    order.avgFillPrice = executionPrice
    order.lastUpdated = new Date()

    // Create trade record
    const trade: MockTrade = {
      id: `TRD-${this.tradeCounter++}`,
      orderId: order.id,
      brokerId: order.brokerId,
      symbol: order.symbol,
      side: order.side,
      quantity: order.quantity,
      price: executionPrice,
      amount: order.quantity * executionPrice,
      fees: order.fees,
      timestamp: new Date()
    }

    profile.trades.push(trade)

    // Update position
    this.updatePosition(userId, order.brokerId, order.symbol, order.side, order.quantity, executionPrice)

    // Update account balance
    const account = profile.accounts.find(acc => acc.brokerId === order.brokerId)
    if (account) {
      if (order.side === 'BUY') {
        account.balance -= (trade.amount + trade.fees)
        account.marginUsed += trade.amount
      } else {
        account.balance += (trade.amount - trade.fees)
        account.marginUsed -= trade.amount
      }
      account.lastUpdated = new Date()
    }

    console.log(`Order ${orderId} executed: ${order.side} ${order.quantity} ${order.symbol} at ₹${executionPrice}`)
  }

  // Reject order
  private rejectOrder(userId: string, orderId: string, reason: string) {
    const profile = this.profiles.get(userId)
    if (!profile) return

    const order = profile.orders.find(o => o.id === orderId)
    if (!order) return

    order.status = 'REJECTED'
    order.rejectReason = reason
    order.lastUpdated = new Date()

    console.log(`Order ${orderId} rejected: ${reason}`)
  }

  // Update position after trade
  private updatePosition(userId: string, brokerId: string, symbol: string, side: 'BUY' | 'SELL', quantity: number, price: number) {
    const profile = this.profiles.get(userId)
    if (!profile) return

    let position = profile.positions.find(pos => pos.brokerId === brokerId && pos.symbol === symbol)

    if (!position && side === 'BUY') {
      // Create new position
      position = {
        id: `pos-${this.positionCounter++}`,
        brokerId,
        symbol,
        companyName: this.getCompanyName(symbol),
        quantity: 0,
        avgPrice: 0,
        currentPrice: this.getCurrentPrice(symbol),
        marketValue: 0,
        totalInvested: 0,
        realizedPnL: 0,
        unrealizedPnL: 0,
        totalPnL: 0,
        pnlPercent: 0,
        dayPnL: 0,
        dayPnLPercent: 0,
        sector: this.getSector(symbol),
        assetType: 'EQUITY',
        lastUpdated: new Date()
      }
      profile.positions.push(position)
    }

    if (!position) return

    if (side === 'BUY') {
      // Add to position
      const newTotalInvested = position.totalInvested + (quantity * price)
      const newQuantity = position.quantity + quantity
      position.avgPrice = newTotalInvested / newQuantity
      position.quantity = newQuantity
      position.totalInvested = newTotalInvested
    } else if (side === 'SELL' && position.quantity > 0) {
      // Reduce position
      const sellValue = quantity * price
      const costBasis = quantity * position.avgPrice
      const realizedPnL = sellValue - costBasis

      position.quantity -= quantity
      position.totalInvested -= costBasis
      position.realizedPnL += realizedPnL

      // If position is closed
      if (position.quantity <= 0) {
        const index = profile.positions.findIndex(p => p.id === position.id)
        if (index >= 0) {
          profile.positions.splice(index, 1)
        }
        return
      }
    }

    // Update calculated fields
    position.currentPrice = this.getCurrentPrice(symbol)
    position.marketValue = position.quantity * position.currentPrice
    position.unrealizedPnL = position.marketValue - position.totalInvested
    position.totalPnL = position.realizedPnL + position.unrealizedPnL
    position.pnlPercent = position.totalInvested > 0 ? (position.totalPnL / position.totalInvested) * 100 : 0
    position.lastUpdated = new Date()
  }

  // Cancel order
  async cancelOrder(userId: string, orderId: string): Promise<boolean> {
    const profile = this.profiles.get(userId)
    if (!profile) return false

    const order = profile.orders.find(o => o.id === orderId)
    if (!order || !['PENDING', 'PLACED'].includes(order.status)) {
      return false
    }

    order.status = 'CANCELLED'
    order.lastUpdated = new Date()

    console.log(`Order ${orderId} cancelled`)
    return true
  }

  // Helper methods for mock data
  private getCurrentPrice(symbol: string): number {
    const basePrices: Record<string, number> = {
      'RELIANCE': 2456.75,
      'TCS': 3789.40,
      'HDFCBANK': 1678.90,
      'INFY': 1456.30,
      'ICICIBANK': 1123.45,
      'WIPRO': 567.80,
      'KOTAKBANK': 1834.20,
      'SBIN': 623.45,
      'BHARTIARTL': 901.25,
      'ITC': 456.70
    }

    const basePrice = basePrices[symbol] || 1000
    // Add some random variation (±1%)
    return basePrice * (0.99 + Math.random() * 0.02)
  }

  private getCompanyName(symbol: string): string {
    const names: Record<string, string> = {
      'RELIANCE': 'Reliance Industries Limited',
      'TCS': 'Tata Consultancy Services Limited',
      'HDFCBANK': 'HDFC Bank Limited',
      'INFY': 'Infosys Limited',
      'ICICIBANK': 'ICICI Bank Limited',
      'WIPRO': 'Wipro Limited',
      'KOTAKBANK': 'Kotak Mahindra Bank Limited',
      'SBIN': 'State Bank of India',
      'BHARTIARTL': 'Bharti Airtel Limited',
      'ITC': 'ITC Limited'
    }

    return names[symbol] || `${symbol} Limited`
  }

  private getSector(symbol: string): string {
    const sectors: Record<string, string> = {
      'RELIANCE': 'Oil & Gas',
      'TCS': 'Information Technology',
      'HDFCBANK': 'Banking',
      'INFY': 'Information Technology',
      'ICICIBANK': 'Banking',
      'WIPRO': 'Information Technology',
      'KOTAKBANK': 'Banking',
      'SBIN': 'Banking',
      'BHARTIARTL': 'Telecommunications',
      'ITC': 'FMCG'
    }

    return sectors[symbol] || 'Other'
  }

  // Update profile when price changes (called by WebSocket service)
  updatePrices(userId: string, priceUpdates: Record<string, number>) {
    const profile = this.profiles.get(userId)
    if (!profile) return

    profile.positions.forEach(position => {
      const newPrice = priceUpdates[position.symbol]
      if (newPrice && newPrice !== position.currentPrice) {
        const oldPrice = position.currentPrice
        position.currentPrice = newPrice
        position.marketValue = position.quantity * newPrice
        position.unrealizedPnL = position.marketValue - position.totalInvested
        position.totalPnL = position.realizedPnL + position.unrealizedPnL
        position.pnlPercent = position.totalInvested > 0 ? (position.totalPnL / position.totalInvested) * 100 : 0
        position.dayPnL = (newPrice - oldPrice) * position.quantity
        position.dayPnLPercent = ((newPrice - oldPrice) / oldPrice) * 100
        position.lastUpdated = new Date()
      }
    })

    profile.lastUpdated = new Date()
  }
}

// Singleton instance
export const mockTradingEngine = new MockTradingEngine()

export default mockTradingEngine