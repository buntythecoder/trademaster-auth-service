import React from 'react'
import { 
  TrendingUp, 
  TrendingDown, 
  DollarSign, 
  PieChart, 
  Upload,
  CheckCircle,
  Clock,
  AlertTriangle,
  Activity,
  Target
} from 'lucide-react'

export function TraderDashboard() {
  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="text-center mb-12">
        <h1 className="text-4xl font-bold gradient-text mb-4">Trading Dashboard</h1>
        <p className="text-slate-400 text-lg">
          Monitor your portfolio and execute intelligent trading strategies
        </p>
      </div>

      {/* KYC Status Banner */}
      <div className="glass-card p-6 rounded-2xl border border-orange-500/50 bg-gradient-to-r from-orange-500/10 to-amber-500/10">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-4">
            <div className="p-3 rounded-xl bg-orange-500/20">
              <AlertTriangle className="h-6 w-6 text-orange-400" />
            </div>
            <div>
              <h3 className="font-bold text-orange-400 text-lg">
                Complete Your KYC Verification
              </h3>
              <p className="text-sm text-slate-400">
                Upload required documents to unlock full trading capabilities
              </p>
            </div>
          </div>
          <div className="flex items-center space-x-6">
            <div className="text-center">
              <div className="text-3xl font-bold text-orange-400 mb-1">75%</div>
              <div className="text-xs text-slate-400">Complete</div>
            </div>
            <button className="cyber-button px-6 py-3 rounded-xl font-semibold flex items-center space-x-2">
              <Upload className="w-4 h-4" />
              <span>Upload Documents</span>
            </button>
          </div>
        </div>
      </div>

      {/* Portfolio Stats */}
      <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-4">
        <div className="glass-card p-6 rounded-2xl hover:scale-105 transition-all duration-300 group">
          <div className="flex items-center justify-between mb-4">
            <div className="p-3 rounded-xl bg-gradient-to-br from-green-500/20 to-green-600/20">
              <DollarSign className="h-6 w-6 text-green-400" />
            </div>
            <div className="text-right">
              <div className="text-2xl font-bold text-white">₹2,45,847</div>
              <div className="text-sm text-green-400 flex items-center justify-end">
                <TrendingUp className="h-3 w-3 mr-1" />
                +5.2%
              </div>
            </div>
          </div>
          <h3 className="text-green-400 font-semibold mb-1">Portfolio Value</h3>
          <p className="text-slate-400 text-sm">from last week</p>
        </div>

        <div className="glass-card p-6 rounded-2xl hover:scale-105 transition-all duration-300 group">
          <div className="flex items-center justify-between mb-4">
            <div className="p-3 rounded-xl bg-gradient-to-br from-cyan-500/20 to-cyan-600/20">
              <TrendingUp className="h-6 w-6 text-cyan-400" />
            </div>
            <div className="text-right">
              <div className="text-2xl font-bold text-green-400">+₹3,247</div>
              <div className="text-sm text-green-400">
                +1.34% gain
              </div>
            </div>
          </div>
          <h3 className="text-cyan-400 font-semibold mb-1">Today's P&L</h3>
          <p className="text-slate-400 text-sm">profitable day</p>
        </div>

        <div className="glass-card p-6 rounded-2xl hover:scale-105 transition-all duration-300 group">
          <div className="flex items-center justify-between mb-4">
            <div className="p-3 rounded-xl bg-gradient-to-br from-purple-500/20 to-purple-600/20">
              <PieChart className="h-6 w-6 text-purple-400" />
            </div>
            <div className="text-right">
              <div className="text-2xl font-bold text-white">12</div>
              <div className="text-sm text-slate-400">
                8 profit, 4 loss
              </div>
            </div>
          </div>
          <h3 className="text-purple-400 font-semibold mb-1">Active Positions</h3>
          <p className="text-slate-400 text-sm">live trades</p>
        </div>

        <div className="glass-card p-6 rounded-2xl hover:scale-105 transition-all duration-300 group">
          <div className="flex items-center justify-between mb-4">
            <div className="p-3 rounded-xl bg-gradient-to-br from-blue-500/20 to-blue-600/20">
              <Target className="h-6 w-6 text-blue-400" />
            </div>
            <div className="text-right">
              <div className="text-2xl font-bold text-white">₹45,230</div>
              <div className="text-sm text-blue-400">
                ready to deploy
              </div>
            </div>
          </div>
          <h3 className="text-blue-400 font-semibold mb-1">Available Cash</h3>
          <p className="text-slate-400 text-sm">buying power</p>
        </div>
      </div>

      {/* KYC Progress and Recent Activity */}
      <div className="grid gap-6 md:grid-cols-2">
        <div className="glass-card p-6 rounded-2xl">
          <h3 className="text-xl font-bold text-white mb-6 flex items-center">
            <CheckCircle className="w-5 h-5 mr-2 text-green-400" />
            KYC Verification Progress
          </h3>
          <div className="space-y-4">
            <div className="flex items-center justify-between p-3 rounded-xl bg-slate-800/30">
              <span className="text-sm font-medium text-white">Personal Information</span>
              <div className="flex items-center space-x-2">
                <div className="w-24 bg-slate-700 rounded-full h-2">
                  <div className="bg-green-400 h-2 rounded-full w-full"></div>
                </div>
                <CheckCircle className="h-4 w-4 text-green-400" />
              </div>
            </div>
            
            <div className="flex items-center justify-between p-3 rounded-xl bg-slate-800/30">
              <span className="text-sm font-medium text-white">Identity Documents</span>
              <div className="flex items-center space-x-2">
                <div className="w-24 bg-slate-700 rounded-full h-2">
                  <div className="bg-green-400 h-2 rounded-full w-full"></div>
                </div>
                <CheckCircle className="h-4 w-4 text-green-400" />
              </div>
            </div>
            
            <div className="flex items-center justify-between p-3 rounded-xl bg-slate-800/30">
              <span className="text-sm font-medium text-white">Address Verification</span>
              <div className="flex items-center space-x-2">
                <div className="w-24 bg-slate-700 rounded-full h-2">
                  <div className="bg-yellow-400 h-2 rounded-full w-3/4"></div>
                </div>
                <Clock className="h-4 w-4 text-yellow-400" />
              </div>
            </div>
            
            <div className="flex items-center justify-between p-3 rounded-xl bg-slate-800/30">
              <span className="text-sm font-medium text-white">Bank Verification</span>
              <div className="flex items-center space-x-2">
                <div className="w-24 bg-slate-700 rounded-full h-2">
                  <div className="bg-red-400 h-2 rounded-full w-1/4"></div>
                </div>
                <AlertTriangle className="h-4 w-4 text-red-400" />
              </div>
            </div>
          </div>
          
          <button className="cyber-button w-full py-3 rounded-xl font-semibold flex items-center justify-center space-x-2 mt-6">
            <Upload className="w-4 h-4" />
            <span>Complete KYC Process</span>
          </button>
        </div>

        <div className="glass-card p-6 rounded-2xl">
          <h3 className="text-xl font-bold text-white mb-6 flex items-center">
            <Activity className="w-5 h-5 mr-2 text-cyan-400" />
            Recent Trades
          </h3>
          <div className="space-y-4">
            {[
              { stock: 'RELIANCE', action: 'BUY', quantity: '10', price: '₹2,547', pl: '+₹234', time: '10:30 AM' },
              { stock: 'TCS', action: 'SELL', quantity: '5', price: '₹3,642', pl: '-₹89', time: '11:15 AM' },
              { stock: 'HDFC BANK', action: 'BUY', quantity: '15', price: '₹1,567', pl: '+₹156', time: '2:45 PM' },
              { stock: 'INFY', action: 'SELL', quantity: '8', price: '₹1,423', pl: '+₹92', time: '3:20 PM' },
            ].map((trade, index) => (
              <div key={index} className="flex items-center justify-between p-3 rounded-xl bg-slate-800/30 hover:bg-slate-700/30 transition-colors">
                <div className="flex items-center space-x-3">
                  <div className={`w-2 h-2 rounded-full ${
                    trade.action === 'BUY' ? 'bg-green-400' : 'bg-red-400'
                  }`} />
                  <div>
                    <p className="text-sm font-medium text-white">{trade.stock}</p>
                    <p className="text-xs text-slate-400">
                      {trade.action} {trade.quantity} @ {trade.price}
                    </p>
                  </div>
                </div>
                <div className="text-right">
                  <p className="text-xs text-slate-400">{trade.time}</p>
                  <p className={`text-sm font-medium ${
                    trade.pl.startsWith('+') ? 'text-green-400' : 'text-red-400'
                  }`}>
                    {trade.pl}
                  </p>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Action Buttons */}
      <div className="flex gap-4 justify-center">
        <button className="cyber-button px-6 py-3 rounded-xl font-semibold flex items-center space-x-2">
          <TrendingUp className="w-4 h-4" />
          <span>Start Trading</span>
        </button>
        <button className="glass-card px-6 py-3 rounded-xl font-semibold text-white hover:text-purple-300 transition-colors border border-purple-500/50 hover:border-purple-400/70 flex items-center space-x-2">
          <PieChart className="w-4 h-4" />
          <span>View Portfolio</span>
        </button>
        <button className="glass-card px-6 py-3 rounded-xl font-semibold text-white hover:text-cyan-300 transition-colors border border-cyan-500/50 hover:border-cyan-400/70 flex items-center space-x-2">
          <Upload className="w-4 h-4" />
          <span>Upload Documents</span>
        </button>
      </div>
    </div>
  )
}