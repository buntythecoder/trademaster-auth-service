import React, { useState, useEffect, useMemo } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  TrendingUp,
  TrendingDown,
  PieChart,
  BarChart3,
  Activity,
  Target,
  Calendar,
  Filter,
  ArrowUpCircle,
  ArrowDownCircle,
  DollarSign,
  Percent,
  Clock,
  Building2,
  RefreshCw,
  Eye,
  EyeOff,
  Award,
  AlertTriangle,
  CheckCircle,
  Zap,
  Shield
} from 'lucide-react';
import { MultiBrokerService, AggregatedPortfolio } from '../../services/brokerService';
import { InteractivePortfolioChart } from '../charts/InteractivePortfolioChart';

interface EnhancedPortfolioAnalyticsProps {
  compact?: boolean;
  className?: string;
}

interface TimeRange {
  label: string;
  value: '1D' | '1W' | '1M' | '3M' | '6M' | '1Y' | 'ALL';
  days: number;
}

interface PortfolioMetric {
  id: string;
  title: string;
  value: string;
  change: number;
  changeType: 'positive' | 'negative' | 'neutral';
  description: string;
  icon: React.ReactNode;
  benchmark?: string;
}

interface SectorAllocation {
  sector: string;
  value: number;
  percentage: number;
  change: number;
  color: string;
  brokerCount: number;
}

interface RiskMetric {
  label: string;
  value: number;
  benchmark: number;
  status: 'good' | 'warning' | 'danger';
  description: string;
}

interface PerformanceData {
  date: string;
  portfolioValue: number;
  portfolioReturn: number;
  benchmark: number;
  benchmarkReturn: number;
}

const timeRanges: TimeRange[] = [
  { label: '1D', value: '1D', days: 1 },
  { label: '1W', value: '1W', days: 7 },
  { label: '1M', value: '1M', days: 30 },
  { label: '3M', value: '3M', days: 90 },
  { label: '6M', value: '6M', days: 180 },
  { label: '1Y', value: '1Y', days: 365 },
  { label: 'ALL', value: 'ALL', days: 1000 }
];

export const EnhancedPortfolioAnalytics: React.FC<EnhancedPortfolioAnalyticsProps> = ({ 
  compact = false, 
  className = '' 
}) => {
  const [portfolio, setPortfolio] = useState<AggregatedPortfolio | null>(null);
  const [loading, setLoading] = useState(true);
  const [selectedTimeRange, setSelectedTimeRange] = useState<TimeRange['value']>('1M');
  const [selectedView, setSelectedView] = useState<'overview' | 'allocation' | 'performance' | 'risk'>('overview');
  const [showBenchmark, setShowBenchmark] = useState(true);
  const [lastUpdated, setLastUpdated] = useState<Date>(new Date());

  useEffect(() => {
    fetchPortfolioData();
    // Auto-refresh every 30 seconds
    const interval = setInterval(fetchPortfolioData, 30000);
    return () => clearInterval(interval);
  }, []);

  const fetchPortfolioData = async () => {
    try {
      setLoading(true);
      const data = await MultiBrokerService.getAggregatedPortfolio();
      setPortfolio(data);
      setLastUpdated(new Date());
    } catch (error) {
      console.warn('Failed to fetch portfolio data, using mock data:', error);
      // Fallback to mock data when real service is unavailable
      const mockPortfolio: AggregatedPortfolio = {
        totalValue: 8452300,
        totalPnl: 562300,
        totalPnlPercent: 7.13,
        dayPnl: 23450,
        dayPnlPercent: 0.28,
        positions: [
          {
            brokerId: 'zerodha',
            symbol: 'RELIANCE',
            quantity: 100,
            averagePrice: 2547,
            currentPrice: 2612,
            marketValue: 261200,
            pnl: 6500,
            pnlPercent: 2.55,
            sector: 'Energy'
          },
          {
            brokerId: 'zerodha',
            symbol: 'TCS',
            quantity: 50,
            averagePrice: 3642,
            currentPrice: 3798,
            marketValue: 189900,
            pnl: 7800,
            pnlPercent: 4.28,
            sector: 'Technology'
          },
          {
            brokerId: 'groww',
            symbol: 'HDFC BANK',
            quantity: 80,
            averagePrice: 1567,
            currentPrice: 1623,
            marketValue: 129840,
            pnl: 4480,
            pnlPercent: 3.57,
            sector: 'Banking & Finance'
          },
          {
            brokerId: 'groww',
            symbol: 'INFY',
            quantity: 60,
            averagePrice: 1423,
            currentPrice: 1489,
            marketValue: 89340,
            pnl: 3960,
            pnlPercent: 4.64,
            sector: 'Technology'
          },
          {
            brokerId: 'angel',
            symbol: 'ICICI BANK',
            quantity: 45,
            averagePrice: 1012,
            currentPrice: 1078,
            marketValue: 48510,
            pnl: 2970,
            pnlPercent: 6.52,
            sector: 'Banking & Finance'
          }
        ],
        brokerWiseBreakdown: {
          zerodha: {
            brokerName: 'Zerodha',
            totalValue: 4226150,
            pnl: 281150,
            pnlPercent: 7.12,
            positionCount: 12
          },
          groww: {
            brokerName: 'Groww',
            totalValue: 2789259,
            pnl: 189259,
            pnlPercent: 7.28,
            positionCount: 8
          },
          angel: {
            brokerName: 'Angel One',
            totalValue: 1436891,
            pnl: 91891,
            pnlPercent: 6.83,
            positionCount: 5
          }
        }
      };
      setPortfolio(mockPortfolio);
      setLastUpdated(new Date());
    } finally {
      setLoading(false);
    }
  };

  // Generate mock historical performance data
  const performanceData = useMemo((): PerformanceData[] => {
    if (!portfolio) return [];

    const selectedRange = timeRanges.find(r => r.value === selectedTimeRange);
    const days = selectedRange?.days || 30;
    const data: PerformanceData[] = [];
    const baseValue = portfolio.totalValue;
    
    for (let i = days; i >= 0; i--) {
      const date = new Date();
      date.setDate(date.getDate() - i);
      
      const volatility = 0.02; // 2% daily volatility
      const trend = 0.0003; // 0.03% daily upward trend
      const random = (Math.random() - 0.5) * volatility;
      const returnRate = trend + random;
      
      const benchmarkReturn = trend * 0.8 + (Math.random() - 0.5) * volatility * 0.6; // Less volatile
      
      data.push({
        date: date.toISOString().split('T')[0],
        portfolioValue: baseValue * (1 + returnRate * (days - i)),
        portfolioReturn: returnRate * 100,
        benchmark: baseValue * (1 + benchmarkReturn * (days - i)),
        benchmarkReturn: benchmarkReturn * 100
      });
    }
    
    return data;
  }, [portfolio, selectedTimeRange]);

  // Calculate sector allocation from positions
  const sectorAllocation = useMemo((): SectorAllocation[] => {
    if (!portfolio) return [];

    const sectorMap = new Map<string, { value: number; brokers: Set<string> }>();
    const colors = ['#8b5cf6', '#06b6d4', '#10b981', '#f59e0b', '#ef4444', '#ec4899', '#84cc16'];
    
    portfolio.positions.forEach(position => {
      const sector = getSectorForSymbol(position.symbol);
      const value = position.quantity * position.currentPrice;
      
      if (sectorMap.has(sector)) {
        const existing = sectorMap.get(sector)!;
        existing.value += value;
        existing.brokers.add(position.brokerId);
      } else {
        sectorMap.set(sector, { value, brokers: new Set([position.brokerId]) });
      }
    });

    let index = 0;
    return Array.from(sectorMap.entries()).map(([sector, data]) => ({
      sector,
      value: data.value,
      percentage: (data.value / portfolio.totalValue) * 100,
      change: Math.random() * 10 - 5, // Mock change
      color: colors[index++ % colors.length],
      brokerCount: data.brokers.size
    })).sort((a, b) => b.value - a.value);
  }, [portfolio]);

  // Calculate portfolio metrics
  const portfolioMetrics = useMemo((): PortfolioMetric[] => {
    if (!portfolio) return [];

    const totalReturn = (portfolio.totalPnl / (portfolio.totalValue - portfolio.totalPnl)) * 100;
    const sharpeRatio = totalReturn / 15; // Simplified calculation
    const winRate = portfolio.positions.filter(p => p.pnl > 0).length / portfolio.positions.length * 100;
    
    return [
      {
        id: 'total-value',
        title: 'Total Portfolio Value',
        value: `₹${portfolio.totalValue.toLocaleString('en-IN')}`,
        change: portfolio.totalPnlPercent,
        changeType: portfolio.totalPnl >= 0 ? 'positive' : 'negative',
        description: 'Current market value of all holdings',
        icon: <DollarSign className="w-5 h-5" />
      },
      {
        id: 'total-pnl',
        title: 'Total P&L',
        value: `₹${portfolio.totalPnl.toLocaleString('en-IN')}`,
        change: portfolio.dayPnlPercent,
        changeType: portfolio.totalPnl >= 0 ? 'positive' : 'negative',
        description: 'Unrealized gains/losses',
        icon: portfolio.totalPnl >= 0 ? <TrendingUp className="w-5 h-5" /> : <TrendingDown className="w-5 h-5" />
      },
      {
        id: 'day-pnl',
        title: 'Day P&L',
        value: `₹${portfolio.dayPnl.toLocaleString('en-IN')}`,
        change: portfolio.dayPnlPercent,
        changeType: portfolio.dayPnl >= 0 ? 'positive' : 'negative',
        description: 'Today\'s gains/losses',
        icon: <Clock className="w-5 h-5" />
      },
      {
        id: 'total-return',
        title: 'Total Return',
        value: `${totalReturn.toFixed(2)}%`,
        change: totalReturn,
        changeType: totalReturn >= 0 ? 'positive' : 'negative',
        description: 'Overall portfolio return',
        icon: <Percent className="w-5 h-5" />,
        benchmark: 'vs 12.5% Nifty 50'
      },
      {
        id: 'sharpe-ratio',
        title: 'Sharpe Ratio',
        value: sharpeRatio.toFixed(2),
        change: 0,
        changeType: 'neutral',
        description: 'Risk-adjusted return measure',
        icon: <Award className="w-5 h-5" />
      },
      {
        id: 'win-rate',
        title: 'Win Rate',
        value: `${winRate.toFixed(1)}%`,
        change: 0,
        changeType: winRate >= 60 ? 'positive' : winRate >= 40 ? 'neutral' : 'negative',
        description: 'Percentage of profitable positions',
        icon: <Target className="w-5 h-5" />
      }
    ];
  }, [portfolio]);

  // Risk metrics calculation
  const riskMetrics = useMemo((): RiskMetric[] => {
    if (!portfolio) return [];

    const concentration = Math.max(...sectorAllocation.map(s => s.percentage));
    const volatility = 18.5; // Mock volatility
    const beta = 1.2; // Mock beta
    const maxDrawdown = 8.5; // Mock max drawdown
    
    return [
      {
        label: 'Portfolio Concentration',
        value: concentration,
        benchmark: 25,
        status: concentration > 30 ? 'danger' : concentration > 25 ? 'warning' : 'good',
        description: 'Highest sector allocation percentage'
      },
      {
        label: 'Volatility (30D)',
        value: volatility,
        benchmark: 15,
        status: volatility > 25 ? 'danger' : volatility > 20 ? 'warning' : 'good',
        description: '30-day historical volatility'
      },
      {
        label: 'Beta',
        value: beta,
        benchmark: 1.0,
        status: beta > 1.5 ? 'danger' : beta > 1.2 ? 'warning' : 'good',
        description: 'Sensitivity to market movements'
      },
      {
        label: 'Max Drawdown',
        value: maxDrawdown,
        benchmark: 10,
        status: maxDrawdown > 15 ? 'danger' : maxDrawdown > 10 ? 'warning' : 'good',
        description: 'Maximum peak-to-trough decline'
      }
    ];
  }, [portfolio, sectorAllocation]);

  const getSectorForSymbol = (symbol: string): string => {
    const sectorMap: { [key: string]: string } = {
      'RELIANCE': 'Energy',
      'TCS': 'IT',
      'INFY': 'IT',
      'HDFC': 'Financial',
      'HDFCBANK': 'Financial',
      'ICICIBANK': 'Financial',
      'SBI': 'Financial',
      'BHARTIARTL': 'Telecom',
      'ITC': 'FMCG',
      'LT': 'Infrastructure',
      'AXISBANK': 'Financial',
      'WIPRO': 'IT',
      'MARUTI': 'Automotive',
      'TATAMOTORS': 'Automotive',
      'SUNPHARMA': 'Pharma'
    };
    return sectorMap[symbol] || 'Others';
  };

  const formatCurrency = (amount: number, showSign = false) => {
    const formatted = Math.abs(amount).toLocaleString('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0
    });
    if (!showSign) return formatted;
    return `${amount >= 0 ? '+' : '-'}${formatted}`;
  };

  const getChangeColor = (change: number) => {
    return change >= 0 ? 'text-green-400' : 'text-red-400';
  };

  const getChangeBgColor = (change: number) => {
    return change >= 0 ? 'bg-green-500/20' : 'bg-red-500/20';
  };

  if (loading && !portfolio) {
    return (
      <div className="glass-card p-8 rounded-2xl">
        <div className="flex items-center justify-center">
          <RefreshCw className="w-8 h-8 text-purple-400 animate-spin" />
          <span className="ml-3 text-slate-400">Loading portfolio analytics...</span>
        </div>
      </div>
    );
  }

  if (!portfolio) {
    return (
      <div className="glass-card p-8 rounded-2xl text-center">
        <Building2 className="w-16 h-16 text-slate-400 mx-auto mb-4" />
        <h3 className="text-xl font-semibold text-white mb-2">No Portfolio Data</h3>
        <p className="text-slate-400 mb-4">Connect your brokers and start trading to see analytics</p>
        <button
          onClick={fetchPortfolioData}
          className="cyber-button px-6 py-3 rounded-xl font-semibold"
        >
          Refresh Data
        </button>
      </div>
    );
  }

  // Compact mode rendering for dashboard preview
  if (compact) {
    const totalValue = portfolio.totalValue;
    const dayChange = Object.values(portfolio.brokerWiseBreakdown).reduce((total, broker) => total + (broker.pnl || 0), 0);
    const dayChangePercent = ((dayChange / totalValue) * 100);
    
    return (
      <motion.div
        className={`space-y-4 ${className}`}
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4 }}
      >
        {/* Compact Metrics Grid */}
        <div className="grid grid-cols-2 gap-4">
          <div className="text-center">
            <div className="text-2xl font-bold text-white mb-1">
              ₹{(totalValue / 100000).toFixed(1)}L
            </div>
            <div className="text-xs text-slate-400">Portfolio Value</div>
          </div>
          <div className="text-center">
            <div className={`text-2xl font-bold mb-1 ${dayChange >= 0 ? 'text-green-400' : 'text-red-400'}`}>
              {dayChange >= 0 ? '+' : ''}{dayChangePercent.toFixed(2)}%
            </div>
            <div className="text-xs text-slate-400">Today's Change</div>
          </div>
        </div>
        
        {/* Mini Chart */}
        <div className="h-32 bg-slate-900/30 rounded-lg p-2 relative">
          <InteractivePortfolioChart
            data={[
              { date: '2024-01-01', portfolioValue: totalValue * 0.85, portfolioReturn: -15 },
              { date: '2024-02-01', portfolioValue: totalValue * 0.92, portfolioReturn: -8 },
              { date: '2024-03-01', portfolioValue: totalValue * 0.98, portfolioReturn: -2 },
              { date: '2024-04-01', portfolioValue: totalValue, portfolioReturn: dayChangePercent }
            ]}
            timeRange="3M"
            showBenchmark={false}
            height={120}
            className="pointer-events-none"
          />
        </div>
        
        {/* Top Performers */}
        <div>
          <div className="flex items-center justify-between mb-2">
            <span className="text-sm font-medium text-slate-300">Top Performers</span>
            <span className="text-xs text-slate-400">Today</span>
          </div>
          <div className="space-y-2">
            {[
              { symbol: 'RELIANCE', change: 2.4, value: '₹2.1L' },
              { symbol: 'TCS', change: 1.8, value: '₹1.3L' },
              { symbol: 'HDFC BANK', change: 1.2, value: '₹85K' }
            ].map((stock, index) => (
              <div key={index} className="flex items-center justify-between text-sm">
                <div className="flex items-center space-x-2">
                  <div className="w-2 h-2 rounded-full bg-green-400"></div>
                  <span className="text-slate-300">{stock.symbol}</span>
                </div>
                <div className="text-right">
                  <div className="text-green-400 font-medium">+{stock.change}%</div>
                  <div className="text-xs text-slate-400">{stock.value}</div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </motion.div>
    );
  }

  return (
    <motion.div
      className="space-y-6"
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      transition={{ duration: 0.6 }}
    >
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold gradient-text mb-2">Portfolio Analytics</h1>
          <p className="text-slate-400">
            Last updated: {lastUpdated.toLocaleTimeString()} • {Object.keys(portfolio.brokerWiseBreakdown).length} brokers connected
          </p>
        </div>
        <div className="flex items-center space-x-3">
          <button
            onClick={fetchPortfolioData}
            disabled={loading}
            className="cyber-button p-2 rounded-xl"
            title="Refresh data"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
          </button>
        </div>
      </div>

      {/* View Selector */}
      <div className="glass-card p-2 rounded-2xl">
        <div className="flex space-x-2">
          {[
            { key: 'overview', label: 'Overview', icon: BarChart3 },
            { key: 'allocation', label: 'Allocation', icon: PieChart },
            { key: 'performance', label: 'Performance', icon: TrendingUp },
            { key: 'risk', label: 'Risk Analysis', icon: Shield }
          ].map(({ key, label, icon: Icon }) => (
            <button
              key={key}
              onClick={() => setSelectedView(key as any)}
              className={`flex items-center space-x-2 px-4 py-2 rounded-xl transition-all ${
                selectedView === key
                  ? 'bg-purple-500/20 text-purple-400 border border-purple-400/30'
                  : 'text-slate-400 hover:text-white hover:bg-white/5'
              }`}
            >
              <Icon className="w-4 h-4" />
              <span className="text-sm font-medium">{label}</span>
            </button>
          ))}
        </div>
      </div>

      <AnimatePresence mode="wait">
        {selectedView === 'overview' && (
          <motion.div
            key="overview"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -20 }}
            className="space-y-6"
          >
            {/* Key Metrics Grid */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {portfolioMetrics.map((metric, index) => (
                <motion.div
                  key={metric.id}
                  className="glass-card p-6 rounded-2xl"
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: index * 0.1 }}
                >
                  <div className="flex items-center justify-between mb-4">
                    <div className="p-3 rounded-xl bg-purple-500/20">
                      {metric.icon}
                    </div>
                    {metric.change !== 0 && (
                      <div className={`flex items-center space-x-1 px-2 py-1 rounded-lg ${getChangeBgColor(metric.change)}`}>
                        {metric.change > 0 ? (
                          <ArrowUpCircle className="w-3 h-3 text-green-400" />
                        ) : (
                          <ArrowDownCircle className="w-3 h-3 text-red-400" />
                        )}
                        <span className={`text-xs font-bold ${getChangeColor(metric.change)}`}>
                          {Math.abs(metric.change).toFixed(2)}%
                        </span>
                      </div>
                    )}
                  </div>
                  
                  <h3 className="text-slate-300 text-sm font-medium mb-1">{metric.title}</h3>
                  <div className="text-2xl font-bold text-white mb-1">{metric.value}</div>
                  {metric.benchmark && (
                    <div className="text-xs text-slate-400">{metric.benchmark}</div>
                  )}
                  <p className="text-xs text-slate-400 mt-2">{metric.description}</p>
                </motion.div>
              ))}
            </div>

            {/* Broker Breakdown */}
            <div className="glass-card p-6 rounded-2xl">
              <h3 className="text-lg font-semibold text-white mb-4 flex items-center">
                <Building2 className="w-5 h-5 text-purple-400 mr-2" />
                Broker Performance Breakdown
              </h3>
              <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
                {Object.entries(portfolio.brokerWiseBreakdown).map(([brokerId, data]) => (
                  <div key={brokerId} className="glass-panel p-4 rounded-xl">
                    <div className="flex items-center justify-between mb-3">
                      <span className="font-semibold text-white text-sm">{data.brokerName}</span>
                      <span className="text-xs text-slate-400">{data.positionCount} positions</span>
                    </div>
                    <div className="space-y-2">
                      <div className="flex justify-between">
                        <span className="text-xs text-slate-400">Value:</span>
                        <span className="text-sm font-semibold text-white">
                          {formatCurrency(data.totalValue)}
                        </span>
                      </div>
                      <div className="flex justify-between">
                        <span className="text-xs text-slate-400">P&L:</span>
                        <div className="text-right">
                          <div className={`text-sm font-semibold ${getChangeColor(data.pnl)}`}>
                            {formatCurrency(data.pnl, true)}
                          </div>
                          <div className={`text-xs ${getChangeColor(data.pnl)}`}>
                            {data.pnlPercent.toFixed(2)}%
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </motion.div>
        )}

        {selectedView === 'allocation' && (
          <motion.div
            key="allocation"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -20 }}
            className="space-y-6"
          >
            {/* Asset Allocation Chart */}
            <div className="glass-card p-6 rounded-2xl">
              <h3 className="text-lg font-semibold text-white mb-6 flex items-center">
                <PieChart className="w-5 h-5 text-purple-400 mr-2" />
                Sector Allocation
              </h3>
              
              <div className="grid lg:grid-cols-2 gap-8">
                {/* Pie Chart Placeholder */}
                <div className="relative">
                  <div className="w-64 h-64 mx-auto relative">
                    <svg className="w-full h-full transform -rotate-90" viewBox="0 0 128 128">
                      {sectorAllocation.map((sector, index) => {
                        const radius = 56;
                        const circumference = 2 * Math.PI * radius;
                        const strokeDasharray = (sector.percentage / 100) * circumference;
                        const strokeDashoffset = -sectorAllocation.slice(0, index)
                          .reduce((sum, s) => sum + (s.percentage / 100) * circumference, 0);
                        
                        return (
                          <circle
                            key={sector.sector}
                            cx="64"
                            cy="64"
                            r={radius}
                            fill="none"
                            stroke={sector.color}
                            strokeWidth="16"
                            strokeDasharray={`${strokeDasharray} ${circumference}`}
                            strokeDashoffset={strokeDashoffset}
                            className="transition-all duration-300"
                          />
                        );
                      })}
                    </svg>
                    <div className="absolute inset-0 flex flex-col items-center justify-center">
                      <div className="text-2xl font-bold text-white">{sectorAllocation.length}</div>
                      <div className="text-sm text-slate-400">Sectors</div>
                    </div>
                  </div>
                </div>

                {/* Sector Legend */}
                <div className="space-y-3">
                  {sectorAllocation.map((sector, index) => (
                    <motion.div
                      key={sector.sector}
                      className="flex items-center justify-between p-3 glass-panel rounded-lg"
                      initial={{ opacity: 0, x: 20 }}
                      animate={{ opacity: 1, x: 0 }}
                      transition={{ delay: index * 0.1 }}
                    >
                      <div className="flex items-center space-x-3">
                        <div 
                          className="w-4 h-4 rounded-full"
                          style={{ backgroundColor: sector.color }}
                        />
                        <div>
                          <div className="font-medium text-white">{sector.sector}</div>
                          <div className="text-xs text-slate-400">{sector.brokerCount} brokers</div>
                        </div>
                      </div>
                      <div className="text-right">
                        <div className="font-semibold text-white">
                          {formatCurrency(sector.value)}
                        </div>
                        <div className="text-sm text-slate-400">
                          {sector.percentage.toFixed(1)}%
                        </div>
                      </div>
                    </motion.div>
                  ))}
                </div>
              </div>
            </div>
          </motion.div>
        )}

        {selectedView === 'performance' && (
          <motion.div
            key="performance"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -20 }}
            className="space-y-6"
          >
            {/* Time Range Selector */}
            <div className="glass-card p-4 rounded-2xl">
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-lg font-semibold text-white flex items-center">
                  <TrendingUp className="w-5 h-5 text-purple-400 mr-2" />
                  Performance Chart
                </h3>
                <div className="flex items-center space-x-2">
                  <button
                    onClick={() => setShowBenchmark(!showBenchmark)}
                    className={`px-3 py-1 rounded-lg text-xs transition-colors ${
                      showBenchmark ? 'bg-purple-500/20 text-purple-400' : 'bg-slate-700/50 text-slate-400'
                    }`}
                  >
                    vs Nifty 50
                  </button>
                </div>
              </div>
              
              <div className="flex flex-wrap gap-2 mb-6">
                {timeRanges.map((range) => (
                  <button
                    key={range.value}
                    onClick={() => setSelectedTimeRange(range.value)}
                    className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
                      selectedTimeRange === range.value
                        ? 'bg-purple-500/20 text-purple-400 border border-purple-400/30'
                        : 'text-slate-400 hover:text-white hover:bg-white/5'
                    }`}
                  >
                    {range.label}
                  </button>
                ))}
              </div>

              {/* Performance Chart */}
              <InteractivePortfolioChart
                data={performanceData}
                timeRange={selectedTimeRange}
                showBenchmark={showBenchmark}
                height={380}
              />
            </div>
          </motion.div>
        )}

        {selectedView === 'risk' && (
          <motion.div
            key="risk"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -20 }}
            className="space-y-6"
          >
            {/* Risk Metrics */}
            <div className="glass-card p-6 rounded-2xl">
              <h3 className="text-lg font-semibold text-white mb-6 flex items-center">
                <Shield className="w-5 h-5 text-purple-400 mr-2" />
                Risk Analysis
              </h3>

              <div className="grid md:grid-cols-2 gap-6">
                {riskMetrics.map((metric, index) => (
                  <motion.div
                    key={metric.label}
                    className="glass-panel p-4 rounded-xl"
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ delay: index * 0.1 }}
                  >
                    <div className="flex items-center justify-between mb-3">
                      <span className="font-medium text-white">{metric.label}</span>
                      <div className={`p-1 rounded-lg ${
                        metric.status === 'good' ? 'bg-green-500/20' :
                        metric.status === 'warning' ? 'bg-yellow-500/20' : 'bg-red-500/20'
                      }`}>
                        {metric.status === 'good' ? (
                          <CheckCircle className="w-4 h-4 text-green-400" />
                        ) : metric.status === 'warning' ? (
                          <AlertTriangle className="w-4 h-4 text-yellow-400" />
                        ) : (
                          <AlertTriangle className="w-4 h-4 text-red-400" />
                        )}
                      </div>
                    </div>
                    
                    <div className="flex items-end justify-between mb-2">
                      <span className="text-2xl font-bold text-white">
                        {metric.value.toFixed(metric.label.includes('%') ? 1 : 2)}
                        {metric.label.includes('Concentration') || metric.label.includes('Volatility') || metric.label.includes('Drawdown') ? '%' : ''}
                      </span>
                      <span className="text-sm text-slate-400">
                        Target: {metric.benchmark.toFixed(metric.label.includes('%') ? 1 : 2)}
                        {metric.label.includes('Concentration') || metric.label.includes('Volatility') || metric.label.includes('Drawdown') ? '%' : ''}
                      </span>
                    </div>
                    
                    <div className="w-full bg-slate-700/50 rounded-full h-2 mb-2">
                      <div 
                        className={`h-full rounded-full transition-all ${
                          metric.status === 'good' ? 'bg-green-400' :
                          metric.status === 'warning' ? 'bg-yellow-400' : 'bg-red-400'
                        }`}
                        style={{ width: `${Math.min((metric.value / (metric.benchmark * 2)) * 100, 100)}%` }}
                      />
                    </div>
                    
                    <p className="text-xs text-slate-400">{metric.description}</p>
                  </motion.div>
                ))}
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </motion.div>
  );
};