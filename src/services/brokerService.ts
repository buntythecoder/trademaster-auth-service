import { TradingServiceFactory } from './tradingProfiles';

export type IndianBrokerType = 'zerodha' | 'upstox' | 'angel_one' | 'icici_direct' | 'groww' | 'iifl';

export interface BrokerCredentials {
  brokerId: string;
  brokerType: IndianBrokerType;
  apiKey: string;
  secretKey: string;
  accountId: string;
  totpKey?: string; // For 2FA
  sandbox: boolean;
  additionalConfig?: Record<string, any>;
}

export interface BrokerConnection {
  id: string;
  name: string;
  brokerType: IndianBrokerType;
  displayName: string;
  status: 'connected' | 'disconnected' | 'connecting' | 'error' | 'authenticating';
  isDefault: boolean;
  capabilities: string[];
  lastConnected?: Date;
  accountId: string;
  connectionConfig: Record<string, any>;
  performance: {
    avgExecutionTime: number;
    successRate: number;
    totalOrders: number;
  };
  balance?: {
    availableMargin: number;
    usedMargin: number;
    totalBalance: number;
  };
}

export interface BrokerPosition {
  brokerId: string;
  brokerName: string;
  symbol: string;
  quantity: number;
  avgPrice: number;
  currentPrice: number;
  pnl: number;
  pnlPercent: number;
  dayPnl: number;
  dayPnlPercent: number;
  lastUpdated: Date;
}

export interface AggregatedPortfolio {
  totalValue: number;
  totalPnl: number;
  totalPnlPercent: number;
  dayPnl: number;
  dayPnlPercent: number;
  positions: BrokerPosition[];
  brokerWiseBreakdown: {
    [brokerId: string]: {
      brokerName: string;
      totalValue: number;
      pnl: number;
      pnlPercent: number;
      positionCount: number;
    };
  };
}

export class MultiBrokerService {
  private static connectedBrokers: Map<string, BrokerConnection> = new Map();
  private static defaultBrokerId: string | null = null;

  // Connection Management
  static async connectBroker(credentials: BrokerCredentials): Promise<boolean> {
    try {
      // Simulate broker API authentication
      console.log(`Connecting to ${credentials.brokerType} with account ${credentials.accountId}`);
      
      // In real implementation, this would:
      // 1. Validate credentials with broker API
      // 2. Establish session
      // 3. Get account details
      
      const connection: BrokerConnection = {
        id: credentials.brokerId,
        name: `${credentials.brokerType}-${credentials.accountId}`,
        brokerType: credentials.brokerType,
        displayName: this.getBrokerDisplayName(credentials.brokerType),
        status: 'connected',
        isDefault: this.connectedBrokers.size === 0, // First broker becomes default
        capabilities: this.getBrokerCapabilities(credentials.brokerType),
        lastConnected: new Date(),
        accountId: credentials.accountId,
        connectionConfig: {
          apiKey: credentials.apiKey,
          sandbox: credentials.sandbox,
          ...credentials.additionalConfig
        },
        performance: {
          avgExecutionTime: 200 + Math.random() * 100,
          successRate: 98 + Math.random() * 2,
          totalOrders: Math.floor(Math.random() * 1000)
        },
        balance: {
          availableMargin: 50000 + Math.random() * 100000,
          usedMargin: Math.random() * 25000,
          totalBalance: 75000 + Math.random() * 150000
        }
      };

      this.connectedBrokers.set(credentials.brokerId, connection);
      
      if (connection.isDefault) {
        this.defaultBrokerId = credentials.brokerId;
      }

      return true;
    } catch (error) {
      console.error('Failed to connect broker:', error);
      return false;
    }
  }

  static async disconnectBroker(brokerId: string): Promise<boolean> {
    try {
      const broker = this.connectedBrokers.get(brokerId);
      if (broker) {
        broker.status = 'disconnected';
        this.connectedBrokers.set(brokerId, broker);
        
        // If this was the default broker, select another one
        if (this.defaultBrokerId === brokerId) {
          const connectedBrokers = Array.from(this.connectedBrokers.values())
            .filter(b => b.status === 'connected');
          this.defaultBrokerId = connectedBrokers.length > 0 ? connectedBrokers[0].id : null;
        }
      }
      return true;
    } catch (error) {
      console.error('Failed to disconnect broker:', error);
      return false;
    }
  }

  static setDefaultBroker(brokerId: string): boolean {
    try {
      // Remove default from all brokers
      this.connectedBrokers.forEach((broker, id) => {
        broker.isDefault = false;
        this.connectedBrokers.set(id, broker);
      });

      // Set new default
      const broker = this.connectedBrokers.get(brokerId);
      if (broker && broker.status === 'connected') {
        broker.isDefault = true;
        this.connectedBrokers.set(brokerId, broker);
        this.defaultBrokerId = brokerId;
        
        // Update trading service to use this broker
        TradingServiceFactory.setProfile('backend'); // Assume backend integrates with brokers
        
        return true;
      }
      return false;
    } catch (error) {
      console.error('Failed to set default broker:', error);
      return false;
    }
  }

  // Data Retrieval
  static getConnectedBrokers(): BrokerConnection[] {
    return Array.from(this.connectedBrokers.values());
  }

  static getDefaultBroker(): BrokerConnection | null {
    if (this.defaultBrokerId) {
      return this.connectedBrokers.get(this.defaultBrokerId) || null;
    }
    return null;
  }

  static async getAggregatedPortfolio(): Promise<AggregatedPortfolio> {
    const connectedBrokers = Array.from(this.connectedBrokers.values())
      .filter(broker => broker.status === 'connected');

    const allPositions: BrokerPosition[] = [];
    const brokerBreakdown: AggregatedPortfolio['brokerWiseBreakdown'] = {};

    for (const broker of connectedBrokers) {
      // In real implementation, fetch positions from each broker API
      const brokerPositions = await this.getBrokerPositions(broker.id);
      allPositions.push(...brokerPositions);

      // Calculate broker-wise totals
      const brokerValue = brokerPositions.reduce((sum, pos) => sum + (pos.quantity * pos.currentPrice), 0);
      const brokerPnl = brokerPositions.reduce((sum, pos) => sum + pos.pnl, 0);

      brokerBreakdown[broker.id] = {
        brokerName: broker.displayName,
        totalValue: brokerValue,
        pnl: brokerPnl,
        pnlPercent: brokerValue > 0 ? (brokerPnl / brokerValue) * 100 : 0,
        positionCount: brokerPositions.length
      };
    }

    const totalValue = allPositions.reduce((sum, pos) => sum + (pos.quantity * pos.currentPrice), 0);
    const totalPnl = allPositions.reduce((sum, pos) => sum + pos.pnl, 0);
    const dayPnl = allPositions.reduce((sum, pos) => sum + pos.dayPnl, 0);

    return {
      totalValue,
      totalPnl,
      totalPnlPercent: totalValue > 0 ? (totalPnl / totalValue) * 100 : 0,
      dayPnl,
      dayPnlPercent: totalValue > 0 ? (dayPnl / totalValue) * 100 : 0,
      positions: allPositions,
      brokerWiseBreakdown: brokerBreakdown
    };
  }

  private static async getBrokerPositions(brokerId: string): Promise<BrokerPosition[]> {
    const broker = this.connectedBrokers.get(brokerId);
    if (!broker) return [];

    // Mock positions for each broker
    const mockSymbols = ['RELIANCE', 'TCS', 'HDFC BANK', 'INFOSYS', 'ICICI BANK', 'SBI'];
    const positions: BrokerPosition[] = [];

    for (let i = 0; i < Math.floor(Math.random() * 4) + 1; i++) {
      const symbol = mockSymbols[Math.floor(Math.random() * mockSymbols.length)];
      const quantity = Math.floor(Math.random() * 50) + 10;
      const avgPrice = 1000 + Math.random() * 2000;
      const currentPrice = avgPrice + (Math.random() - 0.5) * 200;
      const pnl = (currentPrice - avgPrice) * quantity;

      positions.push({
        brokerId: broker.id,
        brokerName: broker.displayName,
        symbol,
        quantity,
        avgPrice,
        currentPrice,
        pnl,
        pnlPercent: ((currentPrice - avgPrice) / avgPrice) * 100,
        dayPnl: pnl * (0.3 + Math.random() * 0.4), // 30-70% of total PnL
        dayPnlPercent: ((currentPrice - avgPrice) / avgPrice) * 100 * (0.3 + Math.random() * 0.4),
        lastUpdated: new Date()
      });
    }

    return positions;
  }

  // Helper Methods
  private static getBrokerDisplayName(brokerType: IndianBrokerType): string {
    const names = {
      'zerodha': 'Zerodha Kite',
      'upstox': 'Upstox Pro',
      'angel_one': 'Angel One',
      'icici_direct': 'ICICI Direct',
      'groww': 'Groww',
      'iifl': 'IIFL Securities'
    };
    return names[brokerType];
  }

  private static getBrokerCapabilities(brokerType: IndianBrokerType): string[] {
    const capabilities = {
      'zerodha': ['stocks', 'futures', 'options', 'commodity', 'currency'],
      'upstox': ['stocks', 'futures', 'options', 'commodity', 'currency'],
      'angel_one': ['stocks', 'futures', 'options', 'commodity', 'mutual_funds'],
      'icici_direct': ['stocks', 'futures', 'options', 'mutual_funds', 'bonds'],
      'groww': ['stocks', 'mutual_funds', 'etfs', 'gold'],
      'iifl': ['stocks', 'futures', 'options', 'commodity', 'bonds']
    };
    return capabilities[brokerType];
  }

  // Order Routing
  static async placeOrder(orderData: any, brokerId?: string): Promise<any> {
    const targetBroker = brokerId ? this.connectedBrokers.get(brokerId) : this.getDefaultBroker();
    
    if (!targetBroker || targetBroker.status !== 'connected') {
      throw new Error('No connected broker available for order placement');
    }

    console.log(`Routing order to ${targetBroker.displayName}:`, orderData);
    
    // In real implementation, this would route to the specific broker API
    // For now, use the existing trading service
    return TradingServiceFactory.placeOrder(orderData);
  }

  // Mock data for development
  static initializeMockBrokers(): void {
    const mockBrokers: BrokerConnection[] = [
      {
        id: 'zerodha-1',
        name: 'Zerodha-Primary',
        brokerType: 'zerodha',
        displayName: 'Zerodha Kite - Main Account',
        status: 'connected',
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
        brokerType: 'angel_one',
        displayName: 'Angel One - Options Trading',
        status: 'connected',
        isDefault: false,
        capabilities: ['stocks', 'futures', 'options', 'commodity'],
        lastConnected: new Date(Date.now() - 3 * 60 * 1000),
        accountId: 'AN567890',
        connectionConfig: { sandbox: false },
        performance: { avgExecutionTime: 220, successRate: 98.7, totalOrders: 1230 },
        balance: { availableMargin: 85000, usedMargin: 32000, totalBalance: 117000 }
      }
    ];

    mockBrokers.forEach(broker => {
      this.connectedBrokers.set(broker.id, broker);
    });

    this.defaultBrokerId = 'zerodha-1';
  }
}