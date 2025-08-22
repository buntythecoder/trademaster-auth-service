import React, { useState } from 'react'
import { Shield, AlertTriangle, TrendingUp, TrendingDown, Info } from 'lucide-react'

interface RiskMetrics {
  portfolioValue: number
  volatility: number
  beta: number
  sharpeRatio: number
  maxDrawdown: number
  valueAtRisk: number
  portfolioRisk: 'Low' | 'Medium' | 'High' | 'Critical'
  riskScore: number
}

interface RiskMeterProps {
  metrics?: Partial<RiskMetrics>
  height?: number
}

const defaultMetrics: RiskMetrics = {
  portfolioValue: 2547850.75,
  volatility: 18.5,
  beta: 1.15,
  sharpeRatio: 1.8,
  maxDrawdown: -12.3,
  valueAtRisk: -45680.25,
  portfolioRisk: 'Medium',
  riskScore: 65
}

export function RiskMeter({ metrics = {}, height = 400 }: RiskMeterProps) {
  const [selectedMetric, setSelectedMetric] = useState<string>('overview')
  const combinedMetrics = { ...defaultMetrics, ...metrics }

  const getRiskColor = (risk: string) => {
    switch (risk) {
      case 'Low': return 'text-green-400'
      case 'Medium': return 'text-yellow-400'
      case 'High': return 'text-orange-400'
      case 'Critical': return 'text-red-400'
      default: return 'text-slate-400'
    }
  }

  const getRiskBgColor = (risk: string) => {
    switch (risk) {
      case 'Low': return 'bg-green-500/20'
      case 'Medium': return 'bg-yellow-500/20'
      case 'High': return 'bg-orange-500/20'
      case 'Critical': return 'bg-red-500/20'
      default: return 'bg-slate-500/20'
    }
  }

  const riskMetrics = [
    {
      id: 'volatility',
      name: 'Volatility',
      value: `${combinedMetrics.volatility}%`,
      description: 'Price fluctuation measure',
      status: combinedMetrics.volatility > 20 ? 'High' : combinedMetrics.volatility > 10 ? 'Medium' : 'Low',
      icon: TrendingUp
    },
    {
      id: 'beta',
      name: 'Beta',
      value: combinedMetrics.beta.toFixed(2),
      description: 'Market correlation',
      status: combinedMetrics.beta > 1.5 ? 'High' : combinedMetrics.beta > 1 ? 'Medium' : 'Low',
      icon: TrendingDown
    },
    {
      id: 'sharpe',
      name: 'Sharpe Ratio',
      value: combinedMetrics.sharpeRatio.toFixed(2),
      description: 'Risk-adjusted returns',
      status: combinedMetrics.sharpeRatio > 1.5 ? 'Low' : combinedMetrics.sharpeRatio > 1 ? 'Medium' : 'High',
      icon: Shield
    },
    {
      id: 'drawdown',
      name: 'Max Drawdown',
      value: `${combinedMetrics.maxDrawdown}%`,
      description: 'Largest peak-to-trough decline',
      status: Math.abs(combinedMetrics.maxDrawdown) > 20 ? 'High' : Math.abs(combinedMetrics.maxDrawdown) > 10 ? 'Medium' : 'Low',
      icon: AlertTriangle
    }
  ]

  const CircularProgressBar = ({ percentage, size = 120, strokeWidth = 8 }: { percentage: number, size?: number, strokeWidth?: number }) => {
    const radius = (size - strokeWidth) / 2
    const circumference = radius * 2 * Math.PI
    const offset = circumference - (percentage / 100) * circumference

    return (
      <div className="relative flex items-center justify-center">
        <svg width={size} height={size} className="transform -rotate-90">
          <circle
            cx={size / 2}
            cy={size / 2}
            r={radius}
            stroke="rgba(148, 163, 184, 0.2)"
            strokeWidth={strokeWidth}
            fill="transparent"
          />
          <circle
            cx={size / 2}
            cy={size / 2}
            r={radius}
            stroke={
              percentage > 80 ? '#ef4444' :
              percentage > 60 ? '#f59e0b' :
              percentage > 40 ? '#eab308' :
              '#10b981'
            }
            strokeWidth={strokeWidth}
            fill="transparent"
            strokeDasharray={circumference}
            strokeDashoffset={offset}
            strokeLinecap="round"
            className="transition-all duration-1000 ease-out"
          />
        </svg>
        <div className="absolute inset-0 flex flex-col items-center justify-center">
          <span className="text-2xl font-bold text-white">{percentage}</span>
          <span className="text-xs text-slate-400">Risk Score</span>
        </div>
      </div>
    )
  }

  return (
    <div className="glass-card rounded-2xl p-6">
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div>
          <h3 className="text-lg font-bold text-white flex items-center">
            <Shield className="w-5 h-5 mr-2 text-cyan-400" />
            Risk Analysis
          </h3>
          <div className="flex items-center space-x-4 mt-1">
            <span className="text-sm text-slate-400">Portfolio Value: ₹{combinedMetrics.portfolioValue.toLocaleString()}</span>
            <div className={`px-2 py-1 rounded-lg text-xs font-semibold ${getRiskBgColor(combinedMetrics.portfolioRisk)} ${getRiskColor(combinedMetrics.portfolioRisk)}`}>
              {combinedMetrics.portfolioRisk} Risk
            </div>
          </div>
        </div>
      </div>

      {/* Risk Score Visualization */}
      <div className="flex items-center justify-center mb-8">
        <CircularProgressBar percentage={combinedMetrics.riskScore} />
      </div>

      {/* Value at Risk */}
      <div className="mb-6 p-4 rounded-xl bg-slate-800/30 border border-red-500/30">
        <div className="flex items-center justify-between">
          <div>
            <div className="text-sm text-slate-400">Value at Risk (1 Day, 95%)</div>
            <div className="text-xl font-bold text-red-400">₹{Math.abs(combinedMetrics.valueAtRisk).toLocaleString()}</div>
          </div>
          <AlertTriangle className="w-6 h-6 text-red-400" />
        </div>
        <div className="text-xs text-slate-500 mt-2">
          Potential loss with 95% confidence in one trading day
        </div>
      </div>

      {/* Risk Metrics Grid */}
      <div className="grid gap-3 md:grid-cols-2">
        {riskMetrics.map((metric) => (
          <div 
            key={metric.id}
            onClick={() => setSelectedMetric(metric.id)}
            className={`p-3 rounded-xl transition-all cursor-pointer ${
              selectedMetric === metric.id
                ? 'bg-purple-500/20 border border-purple-500/50'
                : 'bg-slate-800/30 hover:bg-slate-700/50'
            }`}
          >
            <div className="flex items-center justify-between mb-2">
              <div className="flex items-center space-x-2">
                <metric.icon className="w-4 h-4 text-cyan-400" />
                <span className="text-sm font-medium text-white">{metric.name}</span>
              </div>
              <div className={`px-2 py-0.5 rounded text-xs font-medium ${
                getRiskBgColor(metric.status)} ${getRiskColor(metric.status)
              }`}>
                {metric.status}
              </div>
            </div>
            <div className="text-lg font-bold text-white mb-1">{metric.value}</div>
            <div className="text-xs text-slate-400">{metric.description}</div>
          </div>
        ))}
      </div>

      {/* Risk Management Actions */}
      <div className="mt-6 space-y-3">
        <div className="text-sm font-semibold text-white mb-3">Risk Management Actions</div>
        <div className="grid gap-2">
          <button className="flex items-center justify-between p-3 rounded-xl bg-slate-800/30 hover:bg-slate-700/50 transition-colors group">
            <div className="flex items-center space-x-3">
              <div className="p-2 rounded-lg bg-green-500/20">
                <Shield className="w-4 h-4 text-green-400" />
              </div>
              <div className="text-left">
                <div className="text-sm font-medium text-white group-hover:text-green-400 transition-colors">Set Stop-Loss Orders</div>
                <div className="text-xs text-slate-400">Protect against major losses</div>
              </div>
            </div>
          </button>
          
          <button className="flex items-center justify-between p-3 rounded-xl bg-slate-800/30 hover:bg-slate-700/50 transition-colors group">
            <div className="flex items-center space-x-3">
              <div className="p-2 rounded-lg bg-purple-500/20">
                <TrendingUp className="w-4 h-4 text-purple-400" />
              </div>
              <div className="text-left">
                <div className="text-sm font-medium text-white group-hover:text-purple-400 transition-colors">Diversify Portfolio</div>
                <div className="text-xs text-slate-400">Reduce concentration risk</div>
              </div>
            </div>
          </button>

          <button className="flex items-center justify-between p-3 rounded-xl bg-slate-800/30 hover:bg-slate-700/50 transition-colors group">
            <div className="flex items-center space-x-3">
              <div className="p-2 rounded-lg bg-orange-500/20">
                <Info className="w-4 h-4 text-orange-400" />
              </div>
              <div className="text-left">
                <div className="text-sm font-medium text-white group-hover:text-orange-400 transition-colors">Risk Report</div>
                <div className="text-xs text-slate-400">Generate detailed analysis</div>
              </div>
            </div>
          </button>
        </div>
      </div>
    </div>
  )
}