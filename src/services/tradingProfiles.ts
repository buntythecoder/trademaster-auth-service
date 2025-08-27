// Trading service profiles for development and production
export type TradingProfile = 'mock' | 'backend';

export interface Order {
  id: string;
  symbol: string;
  type: 'BUY' | 'SELL';
  orderType: 'MARKET' | 'LIMIT' | 'STOP_LOSS' | 'BRACKET';
  quantity: number;
  price?: number;
  stopLoss?: number;
  target?: number;
  status: 'PENDING' | 'EXECUTED' | 'CANCELLED' | 'REJECTED';
  timestamp: Date;
  brokerId?: string;
  brokerName?: string;
  executedPrice?: number;
  executedQuantity?: number;
  charges?: number;
  pnl?: number;
}

export interface Position {
  id: string;
  symbol: string;
  quantity: number;
  avgPrice: number;
  currentPrice: number;
  pnl: number;
  pnlPercent: number;
  dayPnl: number;
  dayPnlPercent: number;
  brokerId: string;
  brokerName: string;
  lastUpdated: Date;
}

export interface RiskMetrics {
  availableMargin: number;
  usedMargin: number;
  marginUtilization: number;
  maxRiskPerTrade: number;
  dailyLossLimit: number;
  currentDayPnL: number;
  riskScore: number; // 0-100
}

export interface EnhancedRiskMetrics {
  portfolioRisk: number;
  positionRisk: number;
  leverage: number;
  drawdown: number;
  volatility: number;
  sharpeRatio: number;
  var: number; // Value at Risk
  riskLevel: 'low' | 'medium' | 'high' | 'extreme';
}

// Mock data generator
export class MockTradingService {
  private static mockOrders: Order[] = [
    {
      id: 'ORD-001',
      symbol: 'RELIANCE',
      type: 'BUY',
      orderType: 'MARKET',
      quantity: 10,
      price: 2547,
      status: 'EXECUTED',
      timestamp: new Date(Date.now() - 3600000),
      brokerId: 'zerodha',
      brokerName: 'Zerodha',
      executedPrice: 2549,
      executedQuantity: 10,
      charges: 45.2,
      pnl: 234
    },
    {
      id: 'ORD-002',
      symbol: 'TCS',
      type: 'SELL',
      orderType: 'LIMIT',
      quantity: 5,
      price: 3650,
      status: 'PENDING',
      timestamp: new Date(Date.now() - 1800000),
      brokerId: 'groww',
      brokerName: 'Groww'
    },
    {
      id: 'ORD-003',
      symbol: 'HDFC BANK',
      type: 'BUY',
      orderType: 'BRACKET',
      quantity: 15,
      price: 1567,
      stopLoss: 1540,
      target: 1595,
      status: 'EXECUTED',
      timestamp: new Date(Date.now() - 7200000),
      brokerId: 'angel',
      brokerName: 'Angel One',
      executedPrice: 1569,
      executedQuantity: 15,
      charges: 62.8,
      pnl: 156
    }
  ];

  private static mockPositions: Position[] = [
    {
      id: 'POS-001',
      symbol: 'RELIANCE',
      quantity: 25,
      avgPrice: 2543,
      currentPrice: 2567,
      pnl: 600,
      pnlPercent: 0.94,
      dayPnl: 234,
      dayPnlPercent: 0.37,
      brokerId: 'zerodha',
      brokerName: 'Zerodha',
      lastUpdated: new Date()
    },
    {
      id: 'POS-002',
      symbol: 'TCS',
      quantity: 12,
      avgPrice: 3698,
      currentPrice: 3642,
      pnl: -672,
      pnlPercent: -1.51,
      dayPnl: -89,
      dayPnlPercent: -0.24,
      brokerId: 'groww',
      brokerName: 'Groww',
      lastUpdated: new Date()
    },
    {
      id: 'POS-003',
      symbol: 'HDFC BANK',
      quantity: 30,
      avgPrice: 1545,
      currentPrice: 1578,
      pnl: 990,
      pnlPercent: 2.14,
      dayPnl: 156,
      dayPnlPercent: 0.34,
      brokerId: 'angel',
      brokerName: 'Angel One',
      lastUpdated: new Date()
    }
  ];

  static getRiskMetrics(): RiskMetrics {
    return {
      availableMargin: 125000,
      usedMargin: 78500,
      marginUtilization: 62.8,
      maxRiskPerTrade: 15000,
      dailyLossLimit: 25000,
      currentDayPnL: 4250,
      riskScore: 65
    };
  }

  static getEnhancedRiskMetrics(): EnhancedRiskMetrics {
    const riskScore = Math.floor(Math.random() * 40) + 35; // 35-75
    let riskLevel: 'low' | 'medium' | 'high' | 'extreme';
    
    if (riskScore < 40) riskLevel = 'low';
    else if (riskScore < 60) riskLevel = 'medium';
    else if (riskScore < 80) riskLevel = 'high';
    else riskLevel = 'extreme';

    return {
      portfolioRisk: riskScore,
      positionRisk: Math.floor(Math.random() * 20) + 25, // 25-45
      leverage: Math.random() * 2 + 1.5, // 1.5-3.5
      drawdown: Math.random() * 10 + 5, // 5-15
      volatility: Math.random() * 15 + 10, // 10-25
      sharpeRatio: Math.random() * 1.5 + 0.5, // 0.5-2.0
      var: Math.floor(Math.random() * 20000) + 10000, // 10k-30k
      riskLevel
    };
  }

  static getOrders(): Promise<Order[]> {
    return new Promise(resolve => {
      setTimeout(() => resolve([...this.mockOrders]), 300);
    });
  }

  static getPositions(): Promise<Position[]> {
    return new Promise(resolve => {
      setTimeout(() => resolve([...this.mockPositions]), 200);
    });
  }

  static placeOrder(orderData: Partial<Order>): Promise<Order> {
    const newOrder: Order = {
      id: `ORD-${Date.now()}`,
      symbol: orderData.symbol!,
      type: orderData.type!,
      orderType: orderData.orderType!,
      quantity: orderData.quantity!,
      price: orderData.price,
      stopLoss: orderData.stopLoss,
      target: orderData.target,
      status: Math.random() > 0.1 ? 'EXECUTED' : 'PENDING',
      timestamp: new Date(),
      brokerId: orderData.brokerId || 'zerodha',
      brokerName: orderData.brokerName || 'Zerodha'
    };

    if (newOrder.status === 'EXECUTED') {
      newOrder.executedPrice = orderData.price! + (Math.random() - 0.5) * 10;
      newOrder.executedQuantity = orderData.quantity!;
      newOrder.charges = orderData.quantity! * 0.5;
    }

    this.mockOrders.unshift(newOrder);
    
    return new Promise(resolve => {
      setTimeout(() => resolve(newOrder), 800);
    });
  }

  static cancelOrder(orderId: string): Promise<boolean> {
    const orderIndex = this.mockOrders.findIndex(order => order.id === orderId);
    if (orderIndex !== -1) {
      this.mockOrders[orderIndex].status = 'CANCELLED';
      return Promise.resolve(true);
    }
    return Promise.resolve(false);
  }
}

// Backend trading service (to be implemented)
export class BackendTradingService {
  private static baseUrl = import.meta.env.VITE_TRADING_API_URL || 'http://localhost:8080/api/trading';

  static async getRiskMetrics(): Promise<RiskMetrics> {
    const response = await fetch(`${this.baseUrl}/risk-metrics`, {
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('authToken')}`,
        'Content-Type': 'application/json'
      }
    });
    return response.json();
  }

  static async getEnhancedRiskMetrics(): Promise<EnhancedRiskMetrics> {
    const response = await fetch(`${this.baseUrl}/risk-metrics/enhanced`, {
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('authToken')}`,
        'Content-Type': 'application/json'
      }
    });
    return response.json();
  }

  static async getOrders(): Promise<Order[]> {
    const response = await fetch(`${this.baseUrl}/orders`, {
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('authToken')}`,
        'Content-Type': 'application/json'
      }
    });
    return response.json();
  }

  static async getPositions(): Promise<Position[]> {
    const response = await fetch(`${this.baseUrl}/positions`, {
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('authToken')}`,
        'Content-Type': 'application/json'
      }
    });
    return response.json();
  }

  static async placeOrder(orderData: Partial<Order>): Promise<Order> {
    const response = await fetch(`${this.baseUrl}/orders`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('authToken')}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(orderData)
    });
    return response.json();
  }

  static async cancelOrder(orderId: string): Promise<boolean> {
    const response = await fetch(`${this.baseUrl}/orders/${orderId}/cancel`, {
      method: 'PUT',
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('authToken')}`,
        'Content-Type': 'application/json'
      }
    });
    return response.ok;
  }
}

// Trading service factory
export class TradingServiceFactory {
  private static currentProfile: TradingProfile = 
    (import.meta.env.VITE_TRADING_PROFILE as TradingProfile) || 'mock';

  static setProfile(profile: TradingProfile) {
    this.currentProfile = profile;
  }

  static getCurrentProfile(): TradingProfile {
    return this.currentProfile;
  }

  static getRiskMetrics(): Promise<RiskMetrics> {
    return this.currentProfile === 'mock' 
      ? Promise.resolve(MockTradingService.getRiskMetrics())
      : BackendTradingService.getRiskMetrics();
  }

  static getEnhancedRiskMetrics(): Promise<EnhancedRiskMetrics> {
    return this.currentProfile === 'mock'
      ? Promise.resolve(MockTradingService.getEnhancedRiskMetrics())
      : BackendTradingService.getEnhancedRiskMetrics();
  }

  static getOrders(): Promise<Order[]> {
    return this.currentProfile === 'mock'
      ? MockTradingService.getOrders()
      : BackendTradingService.getOrders();
  }

  static getPositions(): Promise<Position[]> {
    return this.currentProfile === 'mock'
      ? MockTradingService.getPositions()
      : BackendTradingService.getPositions();
  }

  static placeOrder(orderData: Partial<Order>): Promise<Order> {
    return this.currentProfile === 'mock'
      ? MockTradingService.placeOrder(orderData)
      : BackendTradingService.placeOrder(orderData);
  }

  static cancelOrder(orderId: string): Promise<boolean> {
    return this.currentProfile === 'mock'
      ? MockTradingService.cancelOrder(orderId)
      : BackendTradingService.cancelOrder(orderId);
  }
}