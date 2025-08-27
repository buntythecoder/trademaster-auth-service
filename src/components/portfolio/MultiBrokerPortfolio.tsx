import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { 
  TrendingUp, 
  TrendingDown, 
  Activity,
  PieChart,
  BarChart3,
  RefreshCw,
  Filter,
  Eye,
  EyeOff,
  Building2,
  ArrowUpRight,
  ArrowDownRight
} from 'lucide-react';
import { MultiBrokerService, AggregatedPortfolio, BrokerPosition } from '../../services/brokerService';

interface MultiBrokerPortfolioProps {
  className?: string;
  autoRefresh?: boolean;
  refreshInterval?: number; // in seconds
}

export const MultiBrokerPortfolio: React.FC<MultiBrokerPortfolioProps> = ({
  className = '',
  autoRefresh = true,
  refreshInterval = 30
}) => {
  const [portfolio, setPortfolio] = useState<AggregatedPortfolio | null>(null);
  const [loading, setLoading] = useState(true);
  const [lastUpdated, setLastUpdated] = useState<Date>(new Date());
  const [showBrokerBreakdown, setShowBrokerBreakdown] = useState(true);
  const [selectedBroker, setSelectedBroker] = useState<string | null>(null);
  const [sortBy, setSortBy] = useState<'pnl' | 'value' | 'symbol'>('pnl');
  const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>('desc');

  useEffect(() => {
    fetchPortfolioData();

    if (autoRefresh) {
      const interval = setInterval(() => {
        fetchPortfolioData();
      }, refreshInterval * 1000);

      return () => clearInterval(interval);
    }
  }, [autoRefresh, refreshInterval]);

  const fetchPortfolioData = async () => {
    try {
      setLoading(true);
      const data = await MultiBrokerService.getAggregatedPortfolio();
      setPortfolio(data);
      setLastUpdated(new Date());
    } catch (error) {
      console.error('Failed to fetch portfolio data:', error);
    } finally {
      setLoading(false);
    }
  };

  const getFilteredAndSortedPositions = (): BrokerPosition[] => {
    if (!portfolio) return [];

    let positions = [...portfolio.positions];

    // Filter by selected broker
    if (selectedBroker) {
      positions = positions.filter(pos => pos.brokerId === selectedBroker);
    }

    // Sort positions
    positions.sort((a, b) => {
      let aValue: number, bValue: number;

      switch (sortBy) {
        case 'pnl':
          aValue = a.pnl;
          bValue = b.pnl;
          break;
        case 'value':
          aValue = a.quantity * a.currentPrice;
          bValue = b.quantity * b.currentPrice;
          break;
        case 'symbol':
          return sortOrder === 'asc' ? a.symbol.localeCompare(b.symbol) : b.symbol.localeCompare(a.symbol);
        default:
          return 0;
      }

      return sortOrder === 'asc' ? aValue - bValue : bValue - aValue;
    });

    return positions;
  };

  const formatCurrency = (amount: number, showSign = true) => {
    const formatted = Math.abs(amount).toLocaleString('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0
    });

    if (!showSign) return formatted;
    
    const sign = amount >= 0 ? '+' : '-';
    return `${sign}${formatted}`;
  };

  const formatPercentage = (percent: number) => {
    const sign = percent >= 0 ? '+' : '';
    return `${sign}${percent.toFixed(2)}%`;
  };

  const getPnLColor = (value: number) => {
    return value >= 0 ? 'text-green-400' : 'text-red-400';
  };

  const getPnLBgColor = (value: number) => {
    return value >= 0 ? 'bg-green-500/20' : 'bg-red-500/20';
  };

  if (loading && !portfolio) {
    return (
      <div className={`glass-card p-8 rounded-2xl ${className}`}>
        <div className="flex items-center justify-center">
          <RefreshCw className="w-8 h-8 text-purple-400 animate-spin" />
          <span className="ml-3 text-slate-400">Loading portfolio data...</span>
        </div>
      </div>
    );
  }

  if (!portfolio) {
    return (
      <div className={`glass-card p-8 rounded-2xl text-center ${className}`}>
        <Building2 className="w-16 h-16 text-slate-400 mx-auto mb-4" />
        <h3 className="text-xl font-semibold text-white mb-2">No Portfolio Data</h3>
        <p className="text-slate-400 mb-4">Connect your brokers to view portfolio data</p>
        <button
          onClick={fetchPortfolioData}
          className="cyber-button px-6 py-3 rounded-xl font-semibold"
        >
          Refresh
        </button>
      </div>
    );
  }

  const filteredPositions = getFilteredAndSortedPositions();

  return (
    <motion.div
      className={`glass-card p-6 rounded-2xl ${className}`}
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.6 }}
    >
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div>
          <h2 className="text-2xl font-bold gradient-text mb-2">Multi-Broker Portfolio</h2>
          <div className="flex items-center space-x-4 text-sm text-slate-400">
            <span>Last updated: {lastUpdated.toLocaleTimeString()}</span>
            <div className="flex items-center space-x-1">
              <Activity className="w-3 h-3" />
              <span>{portfolio.positions.length} positions</span>
            </div>
          </div>
        </div>
        <div className="flex items-center space-x-3">
          <button
            onClick={() => setShowBrokerBreakdown(!showBrokerBreakdown)}
            className="p-2 rounded-xl bg-slate-700/50 text-slate-400 hover:text-white hover:bg-slate-600/50 transition-colors"
            title={showBrokerBreakdown ? 'Hide broker breakdown' : 'Show broker breakdown'}
          >
            {showBrokerBreakdown ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
          </button>
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

      {/* Portfolio Summary */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
        <div className="glass-panel p-4 rounded-xl">
          <div className="flex items-center justify-between mb-2">
            <span className="text-sm text-slate-400">Total Value</span>
            <PieChart className="w-4 h-4 text-cyan-400" />
          </div>
          <div className="text-xl font-bold text-white">
            {formatCurrency(portfolio.totalValue, false)}
          </div>
        </div>

        <div className="glass-panel p-4 rounded-xl">
          <div className="flex items-center justify-between mb-2">
            <span className="text-sm text-slate-400">Total P&L</span>
            {portfolio.totalPnl >= 0 ? 
              <ArrowUpRight className="w-4 h-4 text-green-400" /> : 
              <ArrowDownRight className="w-4 h-4 text-red-400" />
            }
          </div>
          <div className={`text-xl font-bold ${getPnLColor(portfolio.totalPnl)}`}>
            {formatCurrency(portfolio.totalPnl)}
          </div>
          <div className={`text-sm ${getPnLColor(portfolio.totalPnl)}`}>
            {formatPercentage(portfolio.totalPnlPercent)}
          </div>
        </div>

        <div className="glass-panel p-4 rounded-xl">
          <div className="flex items-center justify-between mb-2">
            <span className="text-sm text-slate-400">Day P&L</span>
            <TrendingUp className="w-4 h-4 text-orange-400" />
          </div>
          <div className={`text-xl font-bold ${getPnLColor(portfolio.dayPnl)}`}>
            {formatCurrency(portfolio.dayPnl)}
          </div>
          <div className={`text-sm ${getPnLColor(portfolio.dayPnl)}`}>
            {formatPercentage(portfolio.dayPnlPercent)}
          </div>
        </div>

        <div className="glass-panel p-4 rounded-xl">
          <div className="flex items-center justify-between mb-2">
            <span className="text-sm text-slate-400">Brokers</span>
            <Building2 className="w-4 h-4 text-purple-400" />
          </div>
          <div className="text-xl font-bold text-white">
            {Object.keys(portfolio.brokerWiseBreakdown).length}
          </div>
        </div>
      </div>

      {/* Broker Breakdown */}
      {showBrokerBreakdown && (
        <div className="mb-6">
          <h3 className="text-lg font-semibold text-white mb-3">Broker Breakdown</h3>
          <div className="grid gap-3 md:grid-cols-2 lg:grid-cols-3">
            {Object.entries(portfolio.brokerWiseBreakdown).map(([brokerId, data]) => (
              <motion.div
                key={brokerId}
                className={`glass-panel p-4 rounded-xl cursor-pointer transition-all ${
                  selectedBroker === brokerId ? 'ring-2 ring-purple-400/50 bg-purple-500/10' : 'hover:bg-white/5'
                }`}
                onClick={() => setSelectedBroker(selectedBroker === brokerId ? null : brokerId)}
                whileHover={{ scale: 1.02 }}
                whileTap={{ scale: 0.98 }}
              >
                <div className="flex items-center justify-between mb-2">
                  <span className="font-semibold text-white text-sm">{data.brokerName}</span>
                  <span className="text-xs text-slate-400">{data.positionCount} positions</span>
                </div>
                <div className="space-y-1">
                  <div className="flex justify-between items-center">
                    <span className="text-xs text-slate-400">Value:</span>
                    <span className="text-sm font-semibold text-white">
                      {formatCurrency(data.totalValue, false)}
                    </span>
                  </div>
                  <div className="flex justify-between items-center">
                    <span className="text-xs text-slate-400">P&L:</span>
                    <div className="text-right">
                      <div className={`text-sm font-semibold ${getPnLColor(data.pnl)}`}>
                        {formatCurrency(data.pnl)}
                      </div>
                      <div className={`text-xs ${getPnLColor(data.pnl)}`}>
                        {formatPercentage(data.pnlPercent)}
                      </div>
                    </div>
                  </div>
                </div>
              </motion.div>
            ))}
          </div>
        </div>
      )}

      {/* Filters and Controls */}
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center space-x-3">
          <Filter className="w-4 h-4 text-slate-400" />
          <select
            value={selectedBroker || ''}
            onChange={(e) => setSelectedBroker(e.target.value || null)}
            className="bg-slate-800/50 border border-slate-700/50 rounded-lg px-3 py-2 text-sm text-white focus:border-purple-400/50"
          >
            <option value="">All Brokers</option>
            {Object.entries(portfolio.brokerWiseBreakdown).map(([brokerId, data]) => (
              <option key={brokerId} value={brokerId}>{data.brokerName}</option>
            ))}
          </select>
        </div>

        <div className="flex items-center space-x-2">
          <select
            value={sortBy}
            onChange={(e) => setSortBy(e.target.value as 'pnl' | 'value' | 'symbol')}
            className="bg-slate-800/50 border border-slate-700/50 rounded-lg px-3 py-2 text-sm text-white focus:border-purple-400/50"
          >
            <option value="pnl">Sort by P&L</option>
            <option value="value">Sort by Value</option>
            <option value="symbol">Sort by Symbol</option>
          </select>
          <button
            onClick={() => setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc')}
            className="p-2 rounded-lg bg-slate-700/50 text-slate-400 hover:text-white hover:bg-slate-600/50 transition-colors"
          >
            {sortOrder === 'asc' ? <TrendingUp className="w-4 h-4" /> : <TrendingDown className="w-4 h-4" />}
          </button>
        </div>
      </div>

      {/* Positions Table */}
      <div className="space-y-2">
        <AnimatePresence>
          {filteredPositions.map((position, index) => (
            <motion.div
              key={`${position.brokerId}-${position.symbol}`}
              className="glass-panel p-4 rounded-xl"
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -20 }}
              transition={{ delay: index * 0.05 }}
            >
              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-4">
                  <div>
                    <h4 className="font-semibold text-white">{position.symbol}</h4>
                    <p className="text-sm text-slate-400">{position.brokerName}</p>
                  </div>
                  <div className="text-sm text-slate-300">
                    <div>Qty: {position.quantity}</div>
                    <div>Avg: ₹{position.avgPrice.toFixed(2)}</div>
                  </div>
                </div>

                <div className="text-right">
                  <div className="text-lg font-semibold text-white">
                    ₹{position.currentPrice.toFixed(2)}
                  </div>
                  <div className="text-sm text-slate-400">
                    {formatCurrency(position.quantity * position.currentPrice, false)}
                  </div>
                </div>

                <div className="text-right">
                  <div className={`text-lg font-bold ${getPnLColor(position.pnl)}`}>
                    {formatCurrency(position.pnl)}
                  </div>
                  <div className={`text-sm ${getPnLColor(position.pnl)}`}>
                    {formatPercentage(position.pnlPercent)}
                  </div>
                </div>

                <div className="text-right">
                  <div className={`text-sm font-semibold ${getPnLColor(position.dayPnl)}`}>
                    {formatCurrency(position.dayPnl)}
                  </div>
                  <div className={`text-xs ${getPnLColor(position.dayPnl)}`}>
                    {formatPercentage(position.dayPnlPercent)}
                  </div>
                  <div className="text-xs text-slate-400">Today</div>
                </div>
              </div>
            </motion.div>
          ))}
        </AnimatePresence>

        {filteredPositions.length === 0 && (
          <div className="text-center py-8">
            <BarChart3 className="w-16 h-16 text-slate-400 mx-auto mb-4" />
            <h3 className="text-lg font-semibold text-white mb-2">No Positions</h3>
            <p className="text-slate-400">
              {selectedBroker ? 'No positions for selected broker' : 'No positions found'}
            </p>
          </div>
        )}
      </div>
    </motion.div>
  );
};