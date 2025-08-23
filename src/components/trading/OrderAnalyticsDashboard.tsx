import React, { useState, useEffect } from 'react';
import { BarChart3, TrendingUp, Clock, DollarSign, Target, AlertTriangle, Download, Filter, Calendar, RefreshCw, PieChart, Activity } from 'lucide-react';

interface DateRange {
  start: Date;
  end: Date;
}

interface OrderAnalyticsFilters {
  symbol?: string;
  orderType?: string;
  status?: string;
  minAmount?: number;
  maxAmount?: number;
  broker?: string;
}

interface SlippageMetric {
  symbol: string;
  averageSlippage: number;
  slippageStandardDeviation: number;
  slippageCount: number;
  date: string;
}

interface ExecutionQualityMetric {
  broker: string;
  averageExecutionTime: number;
  fillRate: number;
  averageSlippage: number;
  totalOrders: number;
  date: string;
}

interface BrokerPerformance {
  broker: string;
  totalOrders: number;
  fillRate: number;
  averageExecutionTime: number;
  averageSlippage: number;
  totalFees: number;
  avgFeePerTrade: number;
  uptime: number;
  errorRate: number;
}

interface OrderSizeDistribution {
  range: string;
  count: number;
  percentage: number;
  totalValue: number;
}

interface TradingTimeAnalysis {
  hour: number;
  orderCount: number;
  successRate: number;
  averageSlippage: number;
  totalVolume: number;
}

interface SymbolFrequency {
  symbol: string;
  count: number;
  percentage: number;
  successRate: number;
  totalVolume: number;
}

interface OrderAnalytics {
  summary: {
    totalOrders: number;
    fillRate: number;
    averageExecutionTime: number;
    totalVolume: number;
    totalFees: number;
    averageSlippage: number;
    successfulOrders: number;
    failedOrders: number;
  };
  performance: {
    slippageAnalysis: SlippageMetric[];
    executionQuality: ExecutionQualityMetric[];
    brokerComparison: BrokerPerformance[];
  };
  patterns: {
    orderSizeDistribution: OrderSizeDistribution[];
    tradingTimeAnalysis: TradingTimeAnalysis[];
    symbolFrequency: SymbolFrequency[];
  };
}

interface MetricCardProps {
  title: string;
  value: number | undefined;
  format: 'number' | 'percentage' | 'duration' | 'currency';
  suffix?: string;
  trend?: number;
  icon?: React.ReactNode;
}

function MetricCard({ title, value, format, suffix, trend, icon }: MetricCardProps) {
  const formatValue = (val: number | undefined) => {
    if (val === undefined) return '-';
    
    switch (format) {
      case 'number':
        return val.toLocaleString();
      case 'percentage':
        return `${val.toFixed(2)}%`;
      case 'duration':
        return `${val.toFixed(0)}${suffix || ''}`;
      case 'currency':
        return `₹${val.toLocaleString(undefined, { minimumFractionDigits: 2 })}`;
      default:
        return val.toString();
    }
  };

  const getTrendColor = (trendValue?: number) => {
    if (trendValue === undefined) return '';
    return trendValue >= 0 ? 'text-green-400' : 'text-red-400';
  };

  return (
    <div className="glass-card rounded-xl p-4">
      <div className="flex items-center justify-between mb-2">
        <div className="flex items-center space-x-2">
          {icon && <div className="text-slate-400">{icon}</div>}
          <span className="text-sm text-slate-400">{title}</span>
        </div>
        {trend !== undefined && (
          <div className={`text-xs ${getTrendColor(trend)}`}>
            {trend >= 0 ? '+' : ''}{trend.toFixed(1)}%
          </div>
        )}
      </div>
      <div className="text-2xl font-bold text-white">
        {formatValue(value)}
      </div>
    </div>
  );
}

interface AnalyticsFiltersProps {
  filters: OrderAnalyticsFilters;
  onFiltersChange: (filters: OrderAnalyticsFilters) => void;
  dateRange: DateRange;
  onDateRangeChange: (range: DateRange) => void;
}

function AnalyticsFilters({ 
  filters, 
  onFiltersChange, 
  dateRange, 
  onDateRangeChange 
}: AnalyticsFiltersProps) {
  const [showFilters, setShowFilters] = useState(false);

  const formatDateForInput = (date: Date) => {
    return date.toISOString().split('T')[0];
  };

  const handleDateChange = (field: 'start' | 'end', value: string) => {
    const newDate = new Date(value);
    onDateRangeChange({
      ...dateRange,
      [field]: newDate
    });
  };

  const clearFilters = () => {
    onFiltersChange({});
  };

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div className="flex items-center space-x-4">
          <div className="flex items-center space-x-2">
            <Calendar className="w-4 h-4 text-slate-400" />
            <input
              type="date"
              value={formatDateForInput(dateRange.start)}
              onChange={(e) => handleDateChange('start', e.target.value)}
              className="px-3 py-2 bg-slate-800/50 border border-slate-600/50 rounded-lg text-white text-sm"
            />
            <span className="text-slate-400">to</span>
            <input
              type="date"
              value={formatDateForInput(dateRange.end)}
              onChange={(e) => handleDateChange('end', e.target.value)}
              className="px-3 py-2 bg-slate-800/50 border border-slate-600/50 rounded-lg text-white text-sm"
            />
          </div>
          
          <button
            onClick={() => setShowFilters(!showFilters)}
            className={`px-3 py-2 rounded-lg text-sm font-medium transition-colors flex items-center space-x-2 ${
              showFilters || Object.keys(filters).length > 0
                ? 'bg-purple-500/20 text-purple-400 border border-purple-500/50'
                : 'bg-slate-700/50 text-slate-400 hover:text-white border border-slate-600/50'
            }`}
          >
            <Filter className="w-4 h-4" />
            <span>Filters</span>
            {Object.keys(filters).length > 0 && (
              <span className="bg-purple-500 text-white px-1.5 py-0.5 rounded-full text-xs">
                {Object.keys(filters).length}
              </span>
            )}
          </button>
        </div>

        <button
          onClick={clearFilters}
          className="px-3 py-2 bg-slate-700/50 hover:bg-slate-600/50 text-slate-400 hover:text-white rounded-lg text-sm transition-colors"
        >
          Clear Filters
        </button>
      </div>

      {showFilters && (
        <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-6 gap-3 p-4 bg-slate-800/30 rounded-lg border border-slate-700/50">
          <div>
            <label className="block text-xs text-slate-400 mb-1">Symbol</label>
            <input
              type="text"
              value={filters.symbol || ''}
              onChange={(e) => onFiltersChange({ ...filters, symbol: e.target.value || undefined })}
              className="w-full px-2 py-1 bg-slate-700/50 border border-slate-600/50 rounded text-white text-sm"
              placeholder="RELIANCE"
            />
          </div>
          
          <div>
            <label className="block text-xs text-slate-400 mb-1">Order Type</label>
            <select
              value={filters.orderType || ''}
              onChange={(e) => onFiltersChange({ ...filters, orderType: e.target.value || undefined })}
              className="w-full px-2 py-1 bg-slate-700/50 border border-slate-600/50 rounded text-white text-sm"
            >
              <option value="">All</option>
              <option value="MARKET">Market</option>
              <option value="LIMIT">Limit</option>
              <option value="STOP">Stop</option>
              <option value="STOP_LIMIT">Stop Limit</option>
            </select>
          </div>
          
          <div>
            <label className="block text-xs text-slate-400 mb-1">Status</label>
            <select
              value={filters.status || ''}
              onChange={(e) => onFiltersChange({ ...filters, status: e.target.value || undefined })}
              className="w-full px-2 py-1 bg-slate-700/50 border border-slate-600/50 rounded text-white text-sm"
            >
              <option value="">All</option>
              <option value="FILLED">Filled</option>
              <option value="PARTIALLY_FILLED">Partially Filled</option>
              <option value="CANCELLED">Cancelled</option>
              <option value="REJECTED">Rejected</option>
            </select>
          </div>
          
          <div>
            <label className="block text-xs text-slate-400 mb-1">Min Amount</label>
            <input
              type="number"
              value={filters.minAmount || ''}
              onChange={(e) => onFiltersChange({ ...filters, minAmount: parseFloat(e.target.value) || undefined })}
              className="w-full px-2 py-1 bg-slate-700/50 border border-slate-600/50 rounded text-white text-sm"
              placeholder="₹0"
            />
          </div>
          
          <div>
            <label className="block text-xs text-slate-400 mb-1">Max Amount</label>
            <input
              type="number"
              value={filters.maxAmount || ''}
              onChange={(e) => onFiltersChange({ ...filters, maxAmount: parseFloat(e.target.value) || undefined })}
              className="w-full px-2 py-1 bg-slate-700/50 border border-slate-600/50 rounded text-white text-sm"
              placeholder="₹999999"
            />
          </div>
          
          <div>
            <label className="block text-xs text-slate-400 mb-1">Broker</label>
            <select
              value={filters.broker || ''}
              onChange={(e) => onFiltersChange({ ...filters, broker: e.target.value || undefined })}
              className="w-full px-2 py-1 bg-slate-700/50 border border-slate-600/50 rounded text-white text-sm"
            >
              <option value="">All</option>
              <option value="ZERODHA">Zerodha</option>
              <option value="UPSTOX">Upstox</option>
              <option value="ANGEL">Angel One</option>
              <option value="ICICI">ICICI Direct</option>
            </select>
          </div>
        </div>
      )}
    </div>
  );
}

function BrokerComparisonTable({ data }: { data?: BrokerPerformance[] }) {
  if (!data || data.length === 0) {
    return (
      <div className="glass-card rounded-xl p-6 text-center">
        <BarChart3 className="w-12 h-12 text-slate-600 mx-auto mb-3" />
        <div className="text-slate-400">No broker data available</div>
      </div>
    );
  }

  return (
    <div className="glass-card rounded-xl p-6">
      <h3 className="text-lg font-semibold text-white mb-4 flex items-center">
        <BarChart3 className="w-5 h-5 mr-2" />
        Broker Performance Comparison
      </h3>
      
      <div className="overflow-x-auto">
        <table className="w-full">
          <thead>
            <tr className="text-left text-sm text-slate-400 border-b border-slate-700">
              <th className="pb-3">Broker</th>
              <th className="pb-3">Orders</th>
              <th className="pb-3">Fill Rate</th>
              <th className="pb-3">Avg Execution</th>
              <th className="pb-3">Avg Slippage</th>
              <th className="pb-3">Total Fees</th>
              <th className="pb-3">Uptime</th>
            </tr>
          </thead>
          <tbody>
            {data.map((broker, index) => (
              <tr key={broker.broker} className="text-sm border-b border-slate-800/50">
                <td className="py-3">
                  <div className="flex items-center space-x-3">
                    <div className={`w-3 h-3 rounded-full ${
                      index === 0 ? 'bg-green-400' : 
                      index === 1 ? 'bg-blue-400' : 
                      index === 2 ? 'bg-yellow-400' : 'bg-purple-400'
                    }`} />
                    <span className="text-white font-medium">{broker.broker}</span>
                  </div>
                </td>
                <td className="py-3 text-white">{broker.totalOrders.toLocaleString()}</td>
                <td className="py-3">
                  <span className={`${broker.fillRate >= 95 ? 'text-green-400' : broker.fillRate >= 90 ? 'text-yellow-400' : 'text-red-400'}`}>
                    {broker.fillRate.toFixed(1)}%
                  </span>
                </td>
                <td className="py-3 text-white">{broker.averageExecutionTime.toFixed(0)}ms</td>
                <td className="py-3">
                  <span className={`${Math.abs(broker.averageSlippage) <= 0.05 ? 'text-green-400' : 'text-red-400'}`}>
                    {(broker.averageSlippage * 100).toFixed(2)}%
                  </span>
                </td>
                <td className="py-3 text-white">₹{broker.totalFees.toLocaleString()}</td>
                <td className="py-3">
                  <span className={`${broker.uptime >= 99 ? 'text-green-400' : broker.uptime >= 95 ? 'text-yellow-400' : 'text-red-400'}`}>
                    {broker.uptime.toFixed(1)}%
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

interface ExportAnalyticsProps {
  dateRange: DateRange;
  filters: OrderAnalyticsFilters;
  onExport: (format: 'CSV' | 'PDF' | 'EXCEL') => void;
}

function ExportAnalytics({ dateRange, filters, onExport }: ExportAnalyticsProps) {
  return (
    <div className="flex items-center justify-between">
      <div className="text-sm text-slate-400">
        Export analytics data for the selected period and filters
      </div>
      <div className="flex space-x-2">
        <button
          onClick={() => onExport('CSV')}
          className="px-3 py-2 bg-slate-700/50 hover:bg-slate-600/50 text-white text-sm rounded-lg transition-colors flex items-center space-x-2"
        >
          <Download className="w-4 h-4" />
          <span>CSV</span>
        </button>
        <button
          onClick={() => onExport('EXCEL')}
          className="px-3 py-2 bg-slate-700/50 hover:bg-slate-600/50 text-white text-sm rounded-lg transition-colors flex items-center space-x-2"
        >
          <Download className="w-4 h-4" />
          <span>Excel</span>
        </button>
        <button
          onClick={() => onExport('PDF')}
          className="px-3 py-2 bg-slate-700/50 hover:bg-slate-600/50 text-white text-sm rounded-lg transition-colors flex items-center space-x-2"
        >
          <Download className="w-4 h-4" />
          <span>PDF</span>
        </button>
      </div>
    </div>
  );
}

// Simple chart components (in a real app, you'd use a charting library like recharts or d3)
function SimpleBarChart({ data, title }: { data: any[], title: string }) {
  if (!data || data.length === 0) {
    return (
      <div className="glass-card rounded-xl p-6 text-center">
        <BarChart3 className="w-12 h-12 text-slate-600 mx-auto mb-3" />
        <div className="text-slate-400">No data available</div>
      </div>
    );
  }

  const maxValue = Math.max(...data.map(d => d.value || d.count || d.orderCount || 0));

  return (
    <div className="glass-card rounded-xl p-6">
      <h4 className="text-lg font-semibold text-white mb-4">{title}</h4>
      <div className="space-y-3">
        {data.slice(0, 10).map((item, index) => (
          <div key={index} className="flex items-center space-x-3">
            <div className="w-16 text-sm text-slate-400 truncate">
              {item.symbol || item.range || item.hour || item.broker}
            </div>
            <div className="flex-1 bg-slate-700/50 rounded-full h-6 relative">
              <div
                className="bg-gradient-to-r from-purple-500 to-blue-500 h-6 rounded-full transition-all duration-300"
                style={{ 
                  width: `${((item.value || item.count || item.orderCount || 0) / maxValue) * 100}%` 
                }}
              />
              <div className="absolute inset-0 flex items-center justify-end pr-2">
                <span className="text-xs text-white font-medium">
                  {(item.value || item.count || item.orderCount || 0).toLocaleString()}
                </span>
              </div>
            </div>
            {item.percentage && (
              <div className="w-12 text-sm text-slate-400 text-right">
                {item.percentage.toFixed(1)}%
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}

export function OrderAnalyticsDashboard() {
  const [analytics, setAnalytics] = useState<OrderAnalytics | null>(null);
  const [dateRange, setDateRange] = useState<DateRange>({
    start: new Date(Date.now() - 30 * 24 * 60 * 60 * 1000), // 30 days ago
    end: new Date()
  });
  const [filters, setFilters] = useState<OrderAnalyticsFilters>({});
  const [loading, setLoading] = useState(false);

  const fetchAnalytics = async () => {
    setLoading(true);
    try {
      // Simulate API call - replace with actual service call
      await new Promise(resolve => setTimeout(resolve, 1000));
      
      const mockAnalytics: OrderAnalytics = {
        summary: {
          totalOrders: 1247,
          fillRate: 94.2,
          averageExecutionTime: 245,
          totalVolume: 12450000,
          totalFees: 24500,
          averageSlippage: 0.025,
          successfulOrders: 1175,
          failedOrders: 72
        },
        performance: {
          slippageAnalysis: [
            { symbol: 'RELIANCE', averageSlippage: 0.015, slippageStandardDeviation: 0.008, slippageCount: 145, date: '2024-01-01' },
            { symbol: 'TCS', averageSlippage: 0.012, slippageStandardDeviation: 0.006, slippageCount: 98, date: '2024-01-01' },
            { symbol: 'INFY', averageSlippage: 0.018, slippageStandardDeviation: 0.009, slippageCount: 87, date: '2024-01-01' }
          ],
          executionQuality: [
            { broker: 'ZERODHA', averageExecutionTime: 180, fillRate: 96.5, averageSlippage: 0.018, totalOrders: 456, date: '2024-01-01' },
            { broker: 'UPSTOX', averageExecutionTime: 220, fillRate: 94.2, averageSlippage: 0.022, totalOrders: 324, date: '2024-01-01' },
            { broker: 'ANGEL', averageExecutionTime: 250, fillRate: 92.8, averageSlippage: 0.025, totalOrders: 287, date: '2024-01-01' }
          ],
          brokerComparison: [
            {
              broker: 'ZERODHA',
              totalOrders: 456,
              fillRate: 96.5,
              averageExecutionTime: 180,
              averageSlippage: 0.018,
              totalFees: 9120,
              avgFeePerTrade: 20,
              uptime: 99.8,
              errorRate: 0.2
            },
            {
              broker: 'UPSTOX',
              totalOrders: 324,
              fillRate: 94.2,
              averageExecutionTime: 220,
              averageSlippage: 0.022,
              totalFees: 6480,
              avgFeePerTrade: 20,
              uptime: 99.2,
              errorRate: 0.8
            },
            {
              broker: 'ANGEL',
              totalOrders: 287,
              fillRate: 92.8,
              averageExecutionTime: 250,
              averageSlippage: 0.025,
              totalFees: 5740,
              avgFeePerTrade: 20,
              uptime: 98.9,
              errorRate: 1.1
            }
          ]
        },
        patterns: {
          orderSizeDistribution: [
            { range: '₹0-₹10K', count: 324, percentage: 26.0, totalValue: 1620000 },
            { range: '₹10K-₹50K', count: 445, percentage: 35.7, totalValue: 13350000 },
            { range: '₹50K-₹100K', count: 287, percentage: 23.0, totalValue: 21525000 },
            { range: '₹100K-₹500K', count: 156, percentage: 12.5, totalValue: 39000000 },
            { range: '₹500K+', count: 35, percentage: 2.8, totalValue: 35000000 }
          ],
          tradingTimeAnalysis: [
            { hour: 9, orderCount: 145, successRate: 89.6, averageSlippage: 0.035, totalVolume: 2890000 },
            { hour: 10, orderCount: 178, successRate: 94.9, averageSlippage: 0.028, totalVolume: 3560000 },
            { hour: 11, orderCount: 156, successRate: 96.2, averageSlippage: 0.022, totalVolume: 3120000 },
            { hour: 14, orderCount: 134, successRate: 95.5, averageSlippage: 0.024, totalVolume: 2680000 },
            { hour: 15, orderCount: 89, successRate: 92.1, averageSlippage: 0.031, totalVolume: 1780000 }
          ],
          symbolFrequency: [
            { symbol: 'RELIANCE', count: 145, percentage: 11.6, successRate: 95.2, totalVolume: 3625000 },
            { symbol: 'TCS', count: 98, percentage: 7.9, successRate: 96.9, totalVolume: 2450000 },
            { symbol: 'INFY', count: 87, percentage: 7.0, successRate: 94.3, totalVolume: 2175000 },
            { symbol: 'HDFC', count: 76, percentage: 6.1, successRate: 93.4, totalVolume: 1900000 },
            { symbol: 'ITC', count: 65, percentage: 5.2, successRate: 97.1, totalVolume: 1625000 }
          ]
        }
      };
      
      setAnalytics(mockAnalytics);
    } catch (error) {
      console.error('Failed to fetch analytics:', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAnalytics();
  }, [dateRange, filters]);

  const exportOrderAnalytics = (format: 'CSV' | 'PDF' | 'EXCEL') => {
    console.log(`Exporting analytics as ${format}`);
    // Implement export functionality
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-white">Order Analytics</h1>
          <p className="text-slate-400 mt-1">Comprehensive trading performance insights</p>
        </div>
        <button
          onClick={fetchAnalytics}
          disabled={loading}
          className="px-4 py-2 bg-purple-500 hover:bg-purple-600 disabled:opacity-50 text-white rounded-lg transition-colors flex items-center space-x-2"
        >
          <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
          <span>Refresh</span>
        </button>
      </div>

      {/* Analytics Filters */}
      <div className="glass-card rounded-xl p-4">
        <AnalyticsFilters
          filters={filters}
          onFiltersChange={setFilters}
          dateRange={dateRange}
          onDateRangeChange={setDateRange}
        />
      </div>

      {loading && (
        <div className="glass-card rounded-xl p-8 text-center">
          <div className="w-8 h-8 border-2 border-purple-500/30 border-t-purple-500 rounded-full animate-spin mx-auto mb-4" />
          <div className="text-white">Loading analytics...</div>
        </div>
      )}

      {!loading && analytics && (
        <>
          {/* Summary Cards */}
          <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-8 gap-4">
            <MetricCard
              title="Total Orders"
              value={analytics.summary.totalOrders}
              format="number"
              icon={<BarChart3 className="w-4 h-4" />}
            />
            <MetricCard
              title="Fill Rate"
              value={analytics.summary.fillRate}
              format="percentage"
              icon={<Target className="w-4 h-4" />}
            />
            <MetricCard
              title="Avg Execution"
              value={analytics.summary.averageExecutionTime}
              format="duration"
              suffix="ms"
              icon={<Clock className="w-4 h-4" />}
            />
            <MetricCard
              title="Total Volume"
              value={analytics.summary.totalVolume}
              format="currency"
              icon={<TrendingUp className="w-4 h-4" />}
            />
            <MetricCard
              title="Total Fees"
              value={analytics.summary.totalFees}
              format="currency"
              icon={<DollarSign className="w-4 h-4" />}
            />
            <MetricCard
              title="Avg Slippage"
              value={analytics.summary.averageSlippage * 100}
              format="percentage"
              icon={<Activity className="w-4 h-4" />}
            />
            <MetricCard
              title="Successful"
              value={analytics.summary.successfulOrders}
              format="number"
              icon={<TrendingUp className="w-4 h-4" />}
            />
            <MetricCard
              title="Failed"
              value={analytics.summary.failedOrders}
              format="number"
              icon={<AlertTriangle className="w-4 h-4" />}
            />
          </div>

          {/* Performance Charts */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <SimpleBarChart
              data={analytics.performance.slippageAnalysis.map(s => ({
                symbol: s.symbol,
                value: s.slippageCount
              }))}
              title="Slippage Analysis by Symbol"
            />
            <SimpleBarChart
              data={analytics.performance.executionQuality.map(e => ({
                broker: e.broker,
                value: e.totalOrders
              }))}
              title="Execution Quality by Broker"
            />
          </div>

          {/* Broker Comparison */}
          <BrokerComparisonTable data={analytics.performance.brokerComparison} />

          {/* Trading Patterns */}
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            <SimpleBarChart
              data={analytics.patterns.orderSizeDistribution}
              title="Order Size Distribution"
            />
            <SimpleBarChart
              data={analytics.patterns.tradingTimeAnalysis}
              title="Trading Time Analysis"
            />
            <SimpleBarChart
              data={analytics.patterns.symbolFrequency}
              title="Most Traded Symbols"
            />
          </div>

          {/* Export Options */}
          <div className="glass-card rounded-xl p-4">
            <ExportAnalytics
              dateRange={dateRange}
              filters={filters}
              onExport={exportOrderAnalytics}
            />
          </div>
        </>
      )}
    </div>
  );
}

export default OrderAnalyticsDashboard;