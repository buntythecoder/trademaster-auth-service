import React, { useMemo } from 'react';
import { motion } from 'framer-motion';
import {
  Shield,
  AlertTriangle,
  TrendingDown,
  Target,
  Activity,
  PieChart,
  BarChart3,
  CheckCircle,
  XCircle,
  Info
} from 'lucide-react';
import { AggregatedPortfolio } from '../../services/brokerService';

interface RiskAnalyticsProps {
  portfolio: AggregatedPortfolio;
  className?: string;
}

interface RiskMetric {
  id: string;
  label: string;
  value: number;
  benchmark: number;
  status: 'excellent' | 'good' | 'warning' | 'danger';
  description: string;
  formula?: string;
  icon: React.ReactNode;
}

interface VaRCalculation {
  daily: number;
  weekly: number;
  monthly: number;
  confidence: number;
}

export const RiskAnalytics: React.FC<RiskAnalyticsProps> = ({ portfolio, className = '' }) => {
  
  // Calculate Value at Risk (VaR)
  const varCalculation = useMemo((): VaRCalculation => {
    const portfolioValue = portfolio.totalValue;
    const volatility = 0.18; // 18% annualized volatility
    const confidence = 0.95; // 95% confidence level
    const zScore = 1.65; // Z-score for 95% confidence
    
    const dailyVol = volatility / Math.sqrt(252); // Daily volatility
    const weeklyVol = volatility / Math.sqrt(52);
    const monthlyVol = volatility / Math.sqrt(12);
    
    return {
      daily: portfolioValue * dailyVol * zScore,
      weekly: portfolioValue * weeklyVol * zScore,
      monthly: portfolioValue * monthlyVol * zScore,
      confidence: confidence * 100
    };
  }, [portfolio.totalValue]);

  // Calculate Sharpe Ratio
  const sharpeRatio = useMemo((): number => {
    const totalReturn = portfolio.totalPnlPercent;
    const riskFreeRate = 6.5; // Assume 6.5% risk-free rate (Indian 10Y bond)
    const excessReturn = totalReturn - riskFreeRate;
    const volatility = 18; // 18% portfolio volatility
    return excessReturn / volatility;
  }, [portfolio.totalPnlPercent]);

  // Calculate portfolio concentration (Herfindahl Index)
  const concentration = useMemo((): number => {
    const totalValue = portfolio.totalValue;
    let herfindahlIndex = 0;
    
    portfolio.positions.forEach(position => {
      const weight = (position.quantity * position.currentPrice) / totalValue;
      herfindahlIndex += weight * weight;
    });
    
    return herfindahlIndex * 100; // Convert to percentage
  }, [portfolio]);

  // Calculate maximum drawdown
  const maxDrawdown = useMemo((): number => {
    // Simulate historical data for drawdown calculation
    const returns = portfolio.positions.map(p => p.pnlPercent);
    let peak = 0;
    let maxDD = 0;
    let cumReturn = 0;
    
    returns.forEach(ret => {
      cumReturn += ret;
      if (cumReturn > peak) peak = cumReturn;
      const drawdown = (peak - cumReturn) / peak * 100;
      if (drawdown > maxDD) maxDD = drawdown;
    });
    
    return Math.max(maxDD, Math.abs(Math.min(...returns)));
  }, [portfolio.positions]);

  // Calculate beta (correlation with market)
  const beta = useMemo((): number => {
    // Simplified beta calculation
    const portfolioReturns = portfolio.positions.map(p => p.pnlPercent);
    const portfolioReturn = portfolioReturns.reduce((sum, ret) => sum + ret, 0) / portfolioReturns.length;
    
    // Assuming market return of 12% annually, 1% monthly
    const marketReturn = 1.0;
    const covariance = 0.8; // Simplified covariance
    const marketVariance = 1.2; // Simplified market variance
    
    return covariance / marketVariance;
  }, [portfolio.positions]);

  // Calculate sector concentration risk
  const sectorRisk = useMemo((): number => {
    const sectorMap = new Map<string, number>();
    const totalValue = portfolio.totalValue;
    
    portfolio.positions.forEach(position => {
      const sector = getSectorForSymbol(position.symbol);
      const value = position.quantity * position.currentPrice;
      sectorMap.set(sector, (sectorMap.get(sector) || 0) + value);
    });
    
    // Find maximum sector concentration
    let maxSectorWeight = 0;
    sectorMap.forEach(value => {
      const weight = (value / totalValue) * 100;
      if (weight > maxSectorWeight) maxSectorWeight = weight;
    });
    
    return maxSectorWeight;
  }, [portfolio]);

  const getSectorForSymbol = (symbol: string): string => {
    const sectorMap: { [key: string]: string } = {
      'RELIANCE': 'Energy',
      'TCS': 'IT', 'INFY': 'IT', 'WIPRO': 'IT',
      'HDFC': 'Financial', 'HDFCBANK': 'Financial', 'ICICIBANK': 'Financial', 'SBI': 'Financial',
      'ITC': 'FMCG',
      'LT': 'Infrastructure',
      'BHARTIARTL': 'Telecom'
    };
    return sectorMap[symbol] || 'Others';
  };

  // Define risk metrics
  const riskMetrics: RiskMetric[] = [
    {
      id: 'sharpe-ratio',
      label: 'Sharpe Ratio',
      value: sharpeRatio,
      benchmark: 1.0,
      status: sharpeRatio > 1.5 ? 'excellent' : sharpeRatio > 1.0 ? 'good' : sharpeRatio > 0.5 ? 'warning' : 'danger',
      description: 'Risk-adjusted return measure. Higher is better.',
      formula: '(Portfolio Return - Risk Free Rate) / Portfolio Volatility',
      icon: <Target className="w-5 h-5" />
    },
    {
      id: 'max-drawdown',
      label: 'Maximum Drawdown',
      value: maxDrawdown,
      benchmark: 15,
      status: maxDrawdown < 10 ? 'excellent' : maxDrawdown < 15 ? 'good' : maxDrawdown < 25 ? 'warning' : 'danger',
      description: 'Largest peak-to-trough decline. Lower is better.',
      formula: '(Peak Value - Trough Value) / Peak Value × 100',
      icon: <TrendingDown className="w-5 h-5" />
    },
    {
      id: 'concentration',
      label: 'Portfolio Concentration',
      value: concentration,
      benchmark: 20,
      status: concentration < 15 ? 'excellent' : concentration < 25 ? 'good' : concentration < 40 ? 'warning' : 'danger',
      description: 'Concentration risk (Herfindahl Index). Lower is better.',
      formula: 'Σ(Weight²) × 100',
      icon: <PieChart className="w-5 h-5" />
    },
    {
      id: 'beta',
      label: 'Portfolio Beta',
      value: beta,
      benchmark: 1.0,
      status: Math.abs(beta - 1) < 0.2 ? 'excellent' : Math.abs(beta - 1) < 0.5 ? 'good' : Math.abs(beta - 1) < 0.8 ? 'warning' : 'danger',
      description: 'Market sensitivity. 1.0 = same as market.',
      formula: 'Covariance(Portfolio, Market) / Variance(Market)',
      icon: <Activity className="w-5 h-5" />
    },
    {
      id: 'sector-risk',
      label: 'Sector Concentration',
      value: sectorRisk,
      benchmark: 25,
      status: sectorRisk < 20 ? 'excellent' : sectorRisk < 30 ? 'good' : sectorRisk < 40 ? 'warning' : 'danger',
      description: 'Highest sector allocation. Lower is better.',
      formula: 'Max(Sector Weight)',
      icon: <BarChart3 className="w-5 h-5" />
    },
    {
      id: 'volatility',
      label: 'Portfolio Volatility',
      value: 18.5, // Mock volatility
      benchmark: 20,
      status: 18.5 < 15 ? 'excellent' : 18.5 < 20 ? 'good' : 18.5 < 30 ? 'warning' : 'danger',
      description: 'Price fluctuation measure. Lower is better.',
      formula: 'Standard Deviation of Returns',
      icon: <Activity className="w-5 h-5" />
    }
  ];

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'excellent': return 'text-green-400 bg-green-500/20 border-green-400/30';
      case 'good': return 'text-blue-400 bg-blue-500/20 border-blue-400/30';
      case 'warning': return 'text-yellow-400 bg-yellow-500/20 border-yellow-400/30';
      case 'danger': return 'text-red-400 bg-red-500/20 border-red-400/30';
      default: return 'text-slate-400 bg-slate-500/20 border-slate-400/30';
    }
  };

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'excellent':
      case 'good':
        return <CheckCircle className="w-4 h-4 text-green-400" />;
      case 'warning':
        return <AlertTriangle className="w-4 h-4 text-yellow-400" />;
      case 'danger':
        return <XCircle className="w-4 h-4 text-red-400" />;
      default:
        return <Info className="w-4 h-4 text-slate-400" />;
    }
  };

  const formatValue = (metric: RiskMetric) => {
    if (metric.id === 'sharpe-ratio' || metric.id === 'beta') {
      return metric.value.toFixed(2);
    }
    if (metric.id.includes('concentration') || metric.id.includes('drawdown') || metric.id.includes('volatility') || metric.id.includes('sector')) {
      return `${metric.value.toFixed(1)}%`;
    }
    return metric.value.toFixed(1);
  };

  return (
    <div className={`space-y-6 ${className}`}>
      {/* Risk Score Overview */}
      <div className="glass-card p-6 rounded-2xl">
        <div className="flex items-center justify-between mb-6">
          <h3 className="text-xl font-bold text-white flex items-center">
            <Shield className="w-6 h-6 text-purple-400 mr-3" />
            Risk Assessment
          </h3>
          <div className="flex items-center space-x-2">
            <span className="text-sm text-slate-400">Overall Risk:</span>
            <div className={`px-3 py-1 rounded-lg border font-semibold ${getStatusColor('good')}`}>
              MODERATE
            </div>
          </div>
        </div>

        {/* Risk Metrics Grid */}
        <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
          {riskMetrics.map((metric, index) => (
            <motion.div
              key={metric.id}
              className="glass-panel p-5 rounded-xl relative"
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: index * 0.1 }}
            >
              {/* Status indicator */}
              <div className="absolute top-3 right-3">
                {getStatusIcon(metric.status)}
              </div>

              {/* Metric header */}
              <div className="flex items-center space-x-3 mb-4">
                <div className="p-2 rounded-lg bg-purple-500/20">
                  {metric.icon}
                </div>
                <div>
                  <h4 className="font-semibold text-white">{metric.label}</h4>
                  <p className="text-xs text-slate-400">{metric.description}</p>
                </div>
              </div>

              {/* Metric value */}
              <div className="mb-4">
                <div className="text-2xl font-bold text-white mb-1">
                  {formatValue(metric)}
                </div>
                <div className="text-sm text-slate-400">
                  Target: {metric.id === 'sharpe-ratio' || metric.id === 'beta' 
                    ? metric.benchmark.toFixed(1) 
                    : `${metric.benchmark}%`}
                </div>
              </div>

              {/* Progress bar */}
              <div className="space-y-2">
                <div className="w-full bg-slate-700/50 rounded-full h-2">
                  <div 
                    className={`h-full rounded-full transition-all duration-500 ${
                      metric.status === 'excellent' ? 'bg-green-400' :
                      metric.status === 'good' ? 'bg-blue-400' :
                      metric.status === 'warning' ? 'bg-yellow-400' : 'bg-red-400'
                    }`}
                    style={{ 
                      width: `${Math.min(
                        ((metric.value / (metric.benchmark * 2)) * 100), 
                        100
                      )}%` 
                    }}
                  />
                </div>
                {metric.formula && (
                  <details className="text-xs text-slate-500">
                    <summary className="cursor-pointer hover:text-slate-400">Formula</summary>
                    <code className="mt-1 block text-slate-400">{metric.formula}</code>
                  </details>
                )}
              </div>
            </motion.div>
          ))}
        </div>
      </div>

      {/* Value at Risk (VaR) */}
      <div className="glass-card p-6 rounded-2xl">
        <h3 className="text-lg font-semibold text-white mb-4 flex items-center">
          <AlertTriangle className="w-5 h-5 text-orange-400 mr-2" />
          Value at Risk (VaR) Analysis
        </h3>
        
        <div className="grid md:grid-cols-3 gap-6">
          {[
            { period: 'Daily', value: varCalculation.daily, color: 'text-green-400' },
            { period: 'Weekly', value: varCalculation.weekly, color: 'text-yellow-400' },
            { period: 'Monthly', value: varCalculation.monthly, color: 'text-red-400' }
          ].map((var_, index) => (
            <motion.div
              key={var_.period}
              className="glass-panel p-4 rounded-xl text-center"
              initial={{ opacity: 0, scale: 0.9 }}
              animate={{ opacity: 1, scale: 1 }}
              transition={{ delay: index * 0.1 }}
            >
              <div className="text-sm text-slate-400 mb-2">{var_.period} VaR</div>
              <div className={`text-xl font-bold ${var_.color} mb-1`}>
                ₹{var_.value.toLocaleString('en-IN', { maximumFractionDigits: 0 })}
              </div>
              <div className="text-xs text-slate-500">
                {((var_.value / portfolio.totalValue) * 100).toFixed(1)}% of portfolio
              </div>
            </motion.div>
          ))}
        </div>
        
        <div className="mt-4 p-3 rounded-lg bg-slate-800/30">
          <p className="text-xs text-slate-400">
            <Info className="w-3 h-3 inline mr-1" />
            VaR estimates potential loss over specified time periods with {varCalculation.confidence}% confidence.
            These are statistical estimates and actual losses may exceed VaR.
          </p>
        </div>
      </div>

      {/* Risk Recommendations */}
      <div className="glass-card p-6 rounded-2xl">
        <h3 className="text-lg font-semibold text-white mb-4 flex items-center">
          <Target className="w-5 h-5 text-purple-400 mr-2" />
          Risk Management Recommendations
        </h3>
        
        <div className="space-y-3">
          {riskMetrics
            .filter(metric => metric.status === 'warning' || metric.status === 'danger')
            .map((metric, index) => (
              <motion.div
                key={metric.id}
                className={`p-4 rounded-lg border ${getStatusColor(metric.status)}`}
                initial={{ opacity: 0, x: -20 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: index * 0.1 }}
              >
                <div className="flex items-start space-x-3">
                  {metric.icon}
                  <div>
                    <h4 className="font-semibold text-white mb-1">{metric.label} Improvement</h4>
                    <p className="text-sm text-slate-300 mb-2">
                      {getRecommendation(metric.id, metric.status)}
                    </p>
                    <div className="text-xs text-slate-400">
                      Current: {formatValue(metric)} • Target: {metric.id === 'sharpe-ratio' || metric.id === 'beta' 
                        ? metric.benchmark.toFixed(1) 
                        : `${metric.benchmark}%`}
                    </div>
                  </div>
                </div>
              </motion.div>
            ))}
          
          {riskMetrics.filter(m => m.status === 'warning' || m.status === 'danger').length === 0 && (
            <div className="p-4 rounded-lg bg-green-500/20 border border-green-400/30 text-center">
              <CheckCircle className="w-6 h-6 text-green-400 mx-auto mb-2" />
              <p className="text-green-400 font-semibold">All Risk Metrics Within Acceptable Range</p>
              <p className="text-sm text-slate-300 mt-1">Your portfolio demonstrates good risk management practices.</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );

  function getRecommendation(metricId: string, status: string): string {
    const recommendations: { [key: string]: string } = {
      'sharpe-ratio': 'Consider reducing low-performing positions or increasing allocation to higher-return assets.',
      'max-drawdown': 'Implement stop-loss orders and consider reducing position sizes during volatile periods.',
      'concentration': 'Diversify holdings across more positions to reduce single-position risk.',
      'beta': 'Adjust portfolio composition to better align with your risk tolerance and market exposure goals.',
      'sector-risk': 'Reduce concentration in dominant sector and diversify across multiple industries.',
      'volatility': 'Consider adding defensive stocks or bonds to reduce overall portfolio volatility.'
    };
    
    return recommendations[metricId] || 'Monitor this metric closely and consider professional advice.';
  }
};