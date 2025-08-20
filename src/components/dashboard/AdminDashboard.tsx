import React from 'react'
import { Users, FileText, TrendingUp, Shield, Bell, Settings, Activity, AlertTriangle, CheckCircle } from 'lucide-react'

export function AdminDashboard() {
  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="text-center mb-12">
        <h1 className="text-4xl font-bold gradient-text mb-4">Admin Command Center</h1>
        <p className="text-slate-400 text-lg">
          Monitor platform performance and manage user operations
        </p>
      </div>

      {/* Stats Cards */}
      <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-4">
        <div className="glass-card p-6 rounded-2xl hover:scale-105 transition-all duration-300 group">
          <div className="flex items-center justify-between mb-4">
            <div className="p-3 rounded-xl bg-gradient-to-br from-purple-500/20 to-purple-600/20">
              <Users className="h-6 w-6 text-purple-400" />
            </div>
            <div className="text-right">
              <div className="text-2xl font-bold text-white">2,847</div>
              <div className="text-sm text-green-400 flex items-center justify-end">
                <TrendingUp className="h-3 w-3 mr-1" />
                +12%
              </div>
            </div>
          </div>
          <h3 className="text-purple-400 font-semibold mb-1">Total Users</h3>
          <p className="text-slate-400 text-sm">from last month</p>
        </div>

        <div className="glass-card p-6 rounded-2xl hover:scale-105 transition-all duration-300 group">
          <div className="flex items-center justify-between mb-4">
            <div className="p-3 rounded-xl bg-gradient-to-br from-orange-500/20 to-orange-600/20">
              <FileText className="h-6 w-6 text-orange-400" />
            </div>
            <div className="text-right">
              <div className="text-2xl font-bold text-white">47</div>
              <div className="text-sm text-red-400 flex items-center justify-end">
                <AlertTriangle className="h-3 w-3 mr-1" />
                -3
              </div>
            </div>
          </div>
          <h3 className="text-orange-400 font-semibold mb-1">Pending KYC</h3>
          <p className="text-slate-400 text-sm">from yesterday</p>
        </div>

        <div className="glass-card p-6 rounded-2xl hover:scale-105 transition-all duration-300 group">
          <div className="flex items-center justify-between mb-4">
            <div className="p-3 rounded-xl bg-gradient-to-br from-cyan-500/20 to-cyan-600/20">
              <TrendingUp className="h-6 w-6 text-cyan-400" />
            </div>
            <div className="text-right">
              <div className="text-2xl font-bold text-white">1,329</div>
              <div className="text-sm text-green-400 flex items-center justify-end">
                <Activity className="h-3 w-3 mr-1" />
                +8%
              </div>
            </div>
          </div>
          <h3 className="text-cyan-400 font-semibold mb-1">Active Trades</h3>
          <p className="text-slate-400 text-sm">from last hour</p>
        </div>

        <div className="glass-card p-6 rounded-2xl hover:scale-105 transition-all duration-300 group">
          <div className="flex items-center justify-between mb-4">
            <div className="p-3 rounded-xl bg-gradient-to-br from-red-500/20 to-red-600/20">
              <Shield className="h-6 w-6 text-red-400" />
            </div>
            <div className="text-right">
              <div className="text-2xl font-bold text-white">3</div>
              <div className="text-sm text-yellow-400">
                Medium Priority
              </div>
            </div>
          </div>
          <h3 className="text-red-400 font-semibold mb-1">Security Alerts</h3>
          <p className="text-slate-400 text-sm">requires attention</p>
        </div>
      </div>

      {/* Recent Activity */}
      <div className="grid gap-6 md:grid-cols-2">
        <div className="glass-card p-6 rounded-2xl">
          <h3 className="text-xl font-bold text-white mb-6 flex items-center">
            <Activity className="w-5 h-5 mr-2 text-purple-400" />
            Recent User Activity
          </h3>
          <div className="space-y-4">
            {[
              { user: 'John Doe', action: 'Completed KYC verification', time: '2 min ago', status: 'success' },
              { user: 'Jane Smith', action: 'Updated trading preferences', time: '5 min ago', status: 'info' },
              { user: 'Mike Johnson', action: 'Failed login attempt', time: '8 min ago', status: 'warning' },
              { user: 'Sarah Wilson', action: 'Document upload pending', time: '12 min ago', status: 'pending' },
            ].map((activity, index) => (
              <div key={index} className="flex items-center justify-between p-3 rounded-xl bg-slate-800/30 hover:bg-slate-700/30 transition-colors">
                <div className="flex items-center space-x-3">
                  <div className={`w-2 h-2 rounded-full ${
                    activity.status === 'success' ? 'bg-green-400' :
                    activity.status === 'warning' ? 'bg-red-400' :
                    activity.status === 'info' ? 'bg-blue-400' :
                    'bg-yellow-400'
                  }`} />
                  <div>
                    <p className="text-sm font-medium text-white">{activity.user}</p>
                    <p className="text-xs text-slate-400">{activity.action}</p>
                  </div>
                </div>
                <div className="text-right">
                  <p className="text-xs text-slate-400">{activity.time}</p>
                  <span className={`text-xs px-2 py-1 rounded-full ${
                    activity.status === 'success' ? 'bg-green-500/20 text-green-400' :
                    activity.status === 'warning' ? 'bg-red-500/20 text-red-400' :
                    activity.status === 'info' ? 'bg-blue-500/20 text-blue-400' :
                    'bg-yellow-500/20 text-yellow-400'
                  }`}>
                    {activity.status}
                  </span>
                </div>
              </div>
            ))}
          </div>
        </div>

        <div className="glass-card p-6 rounded-2xl">
          <h3 className="text-xl font-bold text-white mb-6 flex items-center">
            <Shield className="w-5 h-5 mr-2 text-cyan-400" />
            System Health
          </h3>
          <div className="space-y-4">
            {[
              { service: 'Authentication Service', status: 'healthy', uptime: '99.9%' },
              { service: 'Trading Engine', status: 'healthy', uptime: '99.8%' },
              { service: 'Market Data Feed', status: 'degraded', uptime: '98.2%' },
              { service: 'File Storage', status: 'healthy', uptime: '99.9%' },
            ].map((service, index) => (
              <div key={index} className="flex items-center justify-between p-3 rounded-xl bg-slate-800/30 hover:bg-slate-700/30 transition-colors">
                <div className="flex items-center space-x-3">
                  <CheckCircle className={`w-4 h-4 ${
                    service.status === 'healthy' ? 'text-green-400' :
                    service.status === 'degraded' ? 'text-yellow-400' :
                    'text-red-400'
                  }`} />
                  <div>
                    <p className="text-sm font-medium text-white">{service.service}</p>
                    <p className="text-xs text-slate-400">Uptime: {service.uptime}</p>
                  </div>
                </div>
                <span className={`text-xs px-3 py-1 rounded-full font-medium ${
                  service.status === 'healthy' ? 'bg-green-500/20 text-green-400' :
                  service.status === 'degraded' ? 'bg-yellow-500/20 text-yellow-400' :
                  'bg-red-500/20 text-red-400'
                }`}>
                  {service.status}
                </span>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Action Buttons */}
      <div className="flex gap-4 justify-center">
        <button className="cyber-button px-6 py-3 rounded-xl font-semibold flex items-center space-x-2">
          <Bell className="w-4 h-4" />
          <span>View Notifications</span>
        </button>
        <button className="glass-card px-6 py-3 rounded-xl font-semibold text-white hover:text-purple-300 transition-colors border border-purple-500/50 hover:border-purple-400/70 flex items-center space-x-2">
          <Settings className="w-4 h-4" />
          <span>System Settings</span>
        </button>
      </div>
    </div>
  )
}