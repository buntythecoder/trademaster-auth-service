import React, { useState, useEffect, useCallback } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import {
  Users, CreditCard, TrendingUp, TrendingDown, AlertTriangle, DollarSign,
  Calendar, Clock, Star, Crown, Zap, Building, Eye, EyeOff, RefreshCw,
  Search, Filter, Download, Edit3, Save, X, Check, AlertCircle, Info,
  BarChart3, PieChart, LineChart, Award, Target, Shield, Percent,
  ArrowUp, ArrowDown, ChevronRight, Settings, Bell, Mail, Phone,
  UserCheck, UserX, UserPlus, Package, Gift, Sparkles, Flame,
  Plus, Minus, PlayCircle, PauseCircle, StopCircle, MoreVertical
} from 'lucide-react'

// Types
interface Subscription {
  id: string
  userId: string
  userEmail: string
  userName: string
  plan: 'free' | 'pro' | 'ai-premium' | 'enterprise'
  status: 'active' | 'cancelled' | 'expired' | 'suspended' | 'trial'
  billingCycle: 'monthly' | 'quarterly' | 'annual'
  currentPeriodStart: Date
  currentPeriodEnd: Date
  nextBillingDate?: Date
  amount: number
  currency: string
  usageStats: {
    apiCalls: { used: number; limit: number }
    trades: { used: number; limit: number }
    portfolios: { used: number; limit: number }
    alerts: { used: number; limit: number }
    brokerConnections: { used: number; limit: number }
  }
  churnRisk: 'low' | 'medium' | 'high' | 'critical'
  lifetimeValue: number
  totalPaid: number
  createdAt: Date
  lastPaymentDate?: Date
  autoRenewal: boolean
  trialEndsAt?: Date
  cancelledAt?: Date
  features: string[]
  discounts?: {
    code: string
    type: 'percentage' | 'fixed'
    value: number
    description: string
  }[]
}

interface SubscriptionAnalytics {
  totalRevenue: number
  monthlyRecurringRevenue: number
  annualRecurringRevenue: number
  churnRate: number
  customerLifetimeValue: number
  totalSubscribers: number
  newSubscriptions: number
  cancelledSubscriptions: number
  upgradeRate: number
  downgradeRate: number
  revenueByPlan: Record<string, number>
  churnByPlan: Record<string, number>
  usageMetrics: {
    averageUsage: Record<string, number>
    topFeatures: Array<{ feature: string; usage: number }>
  }
}

interface UsageAlert {
  id: string
  userId: string
  userEmail: string
  featureType: string
  usagePercentage: number
  threshold: number
  severity: 'info' | 'warning' | 'critical'
  message: string
  createdAt: Date
  resolved: boolean
}

interface AdvancedSubscriptionManagementProps {
  initialTab?: 'dashboard' | 'subscriptions' | 'analytics' | 'usage' | 'billing' | 'churn'
}

export const AdvancedSubscriptionManagement: React.FC<AdvancedSubscriptionManagementProps> = ({ initialTab = 'dashboard' }) => {
  const [activeTab, setActiveTab] = useState<'dashboard' | 'subscriptions' | 'analytics' | 'usage' | 'billing' | 'churn'>(initialTab)
  const [searchTerm, setSearchTerm] = useState('')
  const [selectedFilters, setSelectedFilters] = useState<Record<string, any>>({})
  const [loading, setLoading] = useState(false)
  const [notification, setNotification] = useState<{ type: 'success' | 'error' | 'warning' | 'info', message: string } | null>(null)

  // Mock data states
  const [subscriptions, setSubscriptions] = useState<Subscription[]>([])
  const [analytics, setAnalytics] = useState<SubscriptionAnalytics | null>(null)
  const [usageAlerts, setUsageAlerts] = useState<UsageAlert[]>([])

  // Load mock data
  useEffect(() => {
    loadMockData()
  }, [])

  const loadMockData = useCallback(() => {
    // Mock Subscriptions
    const mockSubscriptions: Subscription[] = Array.from({ length: 150 }, (_, i) => ({
      id: `sub-${i + 1}`,
      userId: `user-${i + 1}`,
      userEmail: `user${i + 1}@example.com`,
      userName: `User ${i + 1}`,
      plan: ['free', 'pro', 'ai-premium', 'enterprise'][Math.floor(Math.random() * 4)] as any,
      status: ['active', 'cancelled', 'expired', 'suspended', 'trial'][Math.floor(Math.random() * 5)] as any,
      billingCycle: ['monthly', 'quarterly', 'annual'][Math.floor(Math.random() * 3)] as any,
      currentPeriodStart: new Date(Date.now() - Math.floor(Math.random() * 30) * 24 * 60 * 60 * 1000),
      currentPeriodEnd: new Date(Date.now() + Math.floor(Math.random() * 30) * 24 * 60 * 60 * 1000),
      nextBillingDate: Math.random() > 0.2 ? new Date(Date.now() + Math.floor(Math.random() * 30) * 24 * 60 * 60 * 1000) : undefined,
      amount: [0, 999, 2499, 9999][Math.floor(Math.random() * 4)],
      currency: 'INR',
      usageStats: {
        apiCalls: { used: Math.floor(Math.random() * 10000), limit: 10000 },
        trades: { used: Math.floor(Math.random() * 100), limit: 100 },
        portfolios: { used: Math.floor(Math.random() * 5), limit: 5 },
        alerts: { used: Math.floor(Math.random() * 50), limit: 50 },
        brokerConnections: { used: Math.floor(Math.random() * 3), limit: 3 }
      },
      churnRisk: ['low', 'medium', 'high', 'critical'][Math.floor(Math.random() * 4)] as any,
      lifetimeValue: Math.floor(Math.random() * 50000) + 1000,
      totalPaid: Math.floor(Math.random() * 25000),
      createdAt: new Date(Date.now() - Math.floor(Math.random() * 365) * 24 * 60 * 60 * 1000),
      lastPaymentDate: Math.random() > 0.3 ? new Date(Date.now() - Math.floor(Math.random() * 30) * 24 * 60 * 60 * 1000) : undefined,
      autoRenewal: Math.random() > 0.3,
      trialEndsAt: Math.random() > 0.8 ? new Date(Date.now() + 7 * 24 * 60 * 60 * 1000) : undefined,
      cancelledAt: Math.random() > 0.9 ? new Date(Date.now() - Math.floor(Math.random() * 7) * 24 * 60 * 60 * 1000) : undefined,
      features: ['Real-time data', 'Advanced analytics', 'Multi-broker', 'API access', 'Priority support'].slice(0, Math.floor(Math.random() * 5) + 1),
      discounts: Math.random() > 0.7 ? [{
        code: 'EARLY20',
        type: 'percentage',
        value: 20,
        description: 'Early bird discount'
      }] : undefined
    }))

    // Mock Analytics
    const mockAnalytics: SubscriptionAnalytics = {
      totalRevenue: 8450000,
      monthlyRecurringRevenue: 2850000,
      annualRecurringRevenue: 34200000,
      churnRate: 4.2,
      customerLifetimeValue: 15750,
      totalSubscribers: 12847,
      newSubscriptions: 324,
      cancelledSubscriptions: 89,
      upgradeRate: 8.3,
      downgradeRate: 2.1,
      revenueByPlan: {
        free: 0,
        pro: 1250000,
        'ai-premium': 4200000,
        enterprise: 3000000
      },
      churnByPlan: {
        free: 12.5,
        pro: 6.8,
        'ai-premium': 3.2,
        enterprise: 1.4
      },
      usageMetrics: {
        averageUsage: {
          apiCalls: 7250,
          trades: 68,
          portfolios: 3.2,
          alerts: 34,
          brokerConnections: 2.1
        },
        topFeatures: [
          { feature: 'Real-time P&L', usage: 94.2 },
          { feature: 'Multi-broker Trading', usage: 87.6 },
          { feature: 'Advanced Analytics', usage: 72.3 },
          { feature: 'Risk Management', usage: 65.1 },
          { feature: 'API Access', usage: 43.7 }
        ]
      }
    }

    // Mock Usage Alerts
    const mockUsageAlerts: UsageAlert[] = Array.from({ length: 25 }, (_, i) => ({
      id: `alert-${i + 1}`,
      userId: `user-${Math.floor(Math.random() * 150) + 1}`,
      userEmail: `user${Math.floor(Math.random() * 150) + 1}@example.com`,
      featureType: ['API Calls', 'Trades', 'Portfolios', 'Alerts', 'Broker Connections'][Math.floor(Math.random() * 5)],
      usagePercentage: Math.floor(Math.random() * 40) + 60,
      threshold: [75, 85, 95][Math.floor(Math.random() * 3)],
      severity: ['info', 'warning', 'critical'][Math.floor(Math.random() * 3)] as any,
      message: `Usage at ${Math.floor(Math.random() * 40) + 60}% of limit`,
      createdAt: new Date(Date.now() - Math.floor(Math.random() * 7) * 24 * 60 * 60 * 1000),
      resolved: Math.random() > 0.6
    }))

    setSubscriptions(mockSubscriptions)
    setAnalytics(mockAnalytics)
    setUsageAlerts(mockUsageAlerts)
  }, [])

  // Show notification
  const showNotification = (type: 'success' | 'error' | 'warning' | 'info', message: string) => {
    setNotification({ type, message })
    setTimeout(() => setNotification(null), 5000)
  }

  // Subscription Management Functions
  const upgradeSubscription = async (subscriptionId: string, newPlan: string) => {
    setLoading(true)
    try {
      await new Promise(resolve => setTimeout(resolve, 2000))
      setSubscriptions(prev => prev.map(sub => 
        sub.id === subscriptionId 
          ? { ...sub, plan: newPlan as any, amount: newPlan === 'pro' ? 999 : newPlan === 'ai-premium' ? 2499 : 9999 }
          : sub
      ))
      showNotification('success', `Subscription upgraded to ${newPlan} successfully`)
    } catch (error) {
      showNotification('error', 'Failed to upgrade subscription')
    } finally {
      setLoading(false)
    }
  }

  const cancelSubscription = async (subscriptionId: string) => {
    setLoading(true)
    try {
      await new Promise(resolve => setTimeout(resolve, 1500))
      setSubscriptions(prev => prev.map(sub => 
        sub.id === subscriptionId 
          ? { ...sub, status: 'cancelled', cancelledAt: new Date(), autoRenewal: false }
          : sub
      ))
      showNotification('success', 'Subscription cancelled successfully')
    } catch (error) {
      showNotification('error', 'Failed to cancel subscription')
    } finally {
      setLoading(false)
    }
  }

  const resolveUsageAlert = async (alertId: string) => {
    setLoading(true)
    try {
      await new Promise(resolve => setTimeout(resolve, 1000))
      setUsageAlerts(prev => prev.map(alert => 
        alert.id === alertId ? { ...alert, resolved: true } : alert
      ))
      showNotification('success', 'Usage alert resolved')
    } catch (error) {
      showNotification('error', 'Failed to resolve alert')
    } finally {
      setLoading(false)
    }
  }

  // Filter functions
  const filteredSubscriptions = subscriptions.filter(sub => {
    const matchesSearch = searchTerm === '' || 
      sub.userEmail.toLowerCase().includes(searchTerm.toLowerCase()) ||
      sub.userName.toLowerCase().includes(searchTerm.toLowerCase()) ||
      sub.id.toLowerCase().includes(searchTerm.toLowerCase())
    
    const matchesFilters = Object.entries(selectedFilters).every(([key, value]) => {
      if (!value) return true
      return (sub as any)[key] === value
    })
    
    return matchesSearch && matchesFilters
  })

  const filteredAlerts = usageAlerts.filter(alert => {
    const matchesSearch = searchTerm === '' ||
      alert.userEmail.toLowerCase().includes(searchTerm.toLowerCase()) ||
      alert.featureType.toLowerCase().includes(searchTerm.toLowerCase()) ||
      alert.message.toLowerCase().includes(searchTerm.toLowerCase())
    
    return matchesSearch
  })

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-purple-900 to-slate-900 p-6">
      {/* Notification */}
      <AnimatePresence>
        {notification && (
          <motion.div
            initial={{ opacity: 0, y: -50 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -50 }}
            className={`fixed top-4 right-4 z-50 p-4 rounded-xl shadow-lg max-w-md ${
              notification.type === 'success' ? 'bg-green-500/20 border border-green-500/30 text-green-400' :
              notification.type === 'error' ? 'bg-red-500/20 border border-red-500/30 text-red-400' :
              notification.type === 'warning' ? 'bg-yellow-500/20 border border-yellow-500/30 text-yellow-400' :
              'bg-blue-500/20 border border-blue-500/30 text-blue-400'
            }`}
          >
            <div className="flex items-center space-x-2">
              {notification.type === 'success' && <Check className="w-5 h-5" />}
              {notification.type === 'error' && <X className="w-5 h-5" />}
              {notification.type === 'warning' && <AlertTriangle className="w-5 h-5" />}
              {notification.type === 'info' && <Info className="w-5 h-5" />}
              <span className="font-medium">{notification.message}</span>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Header */}
      <div className="mb-8">
        <h1 className="text-4xl font-bold bg-gradient-to-r from-purple-400 to-cyan-400 bg-clip-text text-transparent mb-2">
          Advanced Subscription Management
        </h1>
        <p className="text-slate-400 text-lg">
          Complete subscription lifecycle management and revenue optimization
        </p>
      </div>

      {/* Navigation Tabs */}
      <div className="mb-8">
        <div className="flex flex-wrap gap-2 p-1 bg-slate-800/50 rounded-xl border border-slate-700/50">
          {[
            { id: 'dashboard', label: 'Dashboard', icon: BarChart3 },
            { id: 'subscriptions', label: 'Subscriptions', icon: CreditCard },
            { id: 'analytics', label: 'Analytics', icon: TrendingUp },
            { id: 'usage', label: 'Usage Monitoring', icon: AlertTriangle },
            { id: 'billing', label: 'Billing Management', icon: DollarSign },
            { id: 'churn', label: 'Churn Management', icon: Users }
          ].map(tab => {
            const Icon = tab.icon
            return (
              <motion.button
                key={tab.id}
                whileHover={{ scale: 1.02 }}
                whileTap={{ scale: 0.98 }}
                onClick={() => setActiveTab(tab.id as any)}
                className={`flex items-center space-x-2 px-4 py-3 rounded-lg font-medium transition-all duration-200 ${
                  activeTab === tab.id
                    ? 'bg-gradient-to-r from-purple-500 to-cyan-500 text-white shadow-lg'
                    : 'text-slate-400 hover:text-white hover:bg-slate-700/50'
                }`}
              >
                <Icon className="w-4 h-4" />
                <span className="whitespace-nowrap">{tab.label}</span>
              </motion.button>
            )
          })}
        </div>
      </div>

      {/* Search and Filters */}
      {(activeTab === 'subscriptions' || activeTab === 'usage') && (
        <div className="mb-6 flex flex-col sm:flex-row gap-4">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-slate-400 w-4 h-4" />
            <input
              type="text"
              placeholder={`Search ${activeTab}...`}
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-10 pr-4 py-3 bg-slate-800/50 border border-slate-700/50 rounded-xl text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-purple-500/50"
            />
          </div>
          
          {activeTab === 'subscriptions' && (
            <div className="flex gap-2">
              <select
                value={selectedFilters.plan || ''}
                onChange={(e) => setSelectedFilters(prev => ({ ...prev, plan: e.target.value || null }))}
                className="px-4 py-3 bg-slate-800/50 border border-slate-700/50 rounded-xl text-white focus:outline-none focus:ring-2 focus:ring-purple-500/50"
              >
                <option value="">All Plans</option>
                <option value="free">Free</option>
                <option value="pro">Pro</option>
                <option value="ai-premium">AI Premium</option>
                <option value="enterprise">Enterprise</option>
              </select>
              
              <select
                value={selectedFilters.status || ''}
                onChange={(e) => setSelectedFilters(prev => ({ ...prev, status: e.target.value || null }))}
                className="px-4 py-3 bg-slate-800/50 border border-slate-700/50 rounded-xl text-white focus:outline-none focus:ring-2 focus:ring-purple-500/50"
              >
                <option value="">All Status</option>
                <option value="active">Active</option>
                <option value="cancelled">Cancelled</option>
                <option value="expired">Expired</option>
                <option value="suspended">Suspended</option>
                <option value="trial">Trial</option>
              </select>
              
              <select
                value={selectedFilters.churnRisk || ''}
                onChange={(e) => setSelectedFilters(prev => ({ ...prev, churnRisk: e.target.value || null }))}
                className="px-4 py-3 bg-slate-800/50 border border-slate-700/50 rounded-xl text-white focus:outline-none focus:ring-2 focus:ring-purple-500/50"
              >
                <option value="">All Risk Levels</option>
                <option value="low">Low Risk</option>
                <option value="medium">Medium Risk</option>
                <option value="high">High Risk</option>
                <option value="critical">Critical Risk</option>
              </select>
            </div>
          )}
        </div>
      )}

      {/* Content */}
      <div className="space-y-6">
        {activeTab === 'dashboard' && analytics && <DashboardTab analytics={analytics} subscriptions={subscriptions} />}
        {activeTab === 'subscriptions' && <SubscriptionsTab subscriptions={filteredSubscriptions} onUpgrade={upgradeSubscription} onCancel={cancelSubscription} loading={loading} />}
        {activeTab === 'analytics' && analytics && <AnalyticsTab analytics={analytics} />}
        {activeTab === 'usage' && <UsageMonitoringTab alerts={filteredAlerts} subscriptions={subscriptions} onResolveAlert={resolveUsageAlert} loading={loading} />}
        {activeTab === 'billing' && <BillingManagementTab subscriptions={subscriptions} />}
        {activeTab === 'churn' && <ChurnManagementTab subscriptions={subscriptions} />}
      </div>
    </div>
  )
}

// Dashboard Tab Component
interface DashboardTabProps {
  analytics: SubscriptionAnalytics
  subscriptions: Subscription[]
}

const DashboardTab: React.FC<DashboardTabProps> = ({ analytics, subscriptions }) => {
  const activeSubscriptions = subscriptions.filter(s => s.status === 'active').length
  const trialSubscriptions = subscriptions.filter(s => s.status === 'trial').length
  const churnRiskSubscriptions = subscriptions.filter(s => s.churnRisk === 'high' || s.churnRisk === 'critical').length

  return (
    <div className="space-y-6">
      {/* Key Metrics */}
      <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-4">
        <div className="glass-card p-6 rounded-2xl hover:scale-105 transition-all duration-300">
          <div className="flex items-center justify-between mb-4">
            <div className="p-3 rounded-xl bg-gradient-to-br from-green-500/20 to-green-600/20">
              <DollarSign className="h-6 w-6 text-green-400" />
            </div>
            <div className="text-right">
              <div className="text-2xl font-bold text-white">₹{(analytics.monthlyRecurringRevenue / 100000).toFixed(1)}L</div>
              <div className="text-sm text-green-400 flex items-center justify-end">
                <TrendingUp className="h-3 w-3 mr-1" />
                +18%
              </div>
            </div>
          </div>
          <h3 className="text-green-400 font-semibold mb-1">Monthly Recurring Revenue</h3>
          <p className="text-slate-400 text-sm">from active subscriptions</p>
        </div>

        <div className="glass-card p-6 rounded-2xl hover:scale-105 transition-all duration-300">
          <div className="flex items-center justify-between mb-4">
            <div className="p-3 rounded-xl bg-gradient-to-br from-blue-500/20 to-blue-600/20">
              <Users className="h-6 w-6 text-blue-400" />
            </div>
            <div className="text-right">
              <div className="text-2xl font-bold text-white">{activeSubscriptions.toLocaleString()}</div>
              <div className="text-sm text-blue-400 flex items-center justify-end">
                <TrendingUp className="h-3 w-3 mr-1" />
                +235
              </div>
            </div>
          </div>
          <h3 className="text-blue-400 font-semibold mb-1">Active Subscribers</h3>
          <p className="text-slate-400 text-sm">paying customers</p>
        </div>

        <div className="glass-card p-6 rounded-2xl hover:scale-105 transition-all duration-300">
          <div className="flex items-center justify-between mb-4">
            <div className="p-3 rounded-xl bg-gradient-to-br from-yellow-500/20 to-yellow-600/20">
              <AlertTriangle className="h-6 w-6 text-yellow-400" />
            </div>
            <div className="text-right">
              <div className="text-2xl font-bold text-white">{analytics.churnRate}%</div>
              <div className="text-sm text-yellow-400 flex items-center justify-end">
                <TrendingDown className="h-3 w-3 mr-1" />
                -0.3%
              </div>
            </div>
          </div>
          <h3 className="text-yellow-400 font-semibold mb-1">Churn Rate</h3>
          <p className="text-slate-400 text-sm">monthly churn</p>
        </div>

        <div className="glass-card p-6 rounded-2xl hover:scale-105 transition-all duration-300">
          <div className="flex items-center justify-between mb-4">
            <div className="p-3 rounded-xl bg-gradient-to-br from-purple-500/20 to-purple-600/20">
              <Star className="h-6 w-6 text-purple-400" />
            </div>
            <div className="text-right">
              <div className="text-2xl font-bold text-white">₹{(analytics.customerLifetimeValue / 1000).toFixed(0)}K</div>
              <div className="text-sm text-purple-400 flex items-center justify-end">
                <TrendingUp className="h-3 w-3 mr-1" />
                +12%
              </div>
            </div>
          </div>
          <h3 className="text-purple-400 font-semibold mb-1">Customer LTV</h3>
          <p className="text-slate-400 text-sm">lifetime value</p>
        </div>
      </div>

      {/* Revenue by Plan */}
      <div className="grid gap-6 md:grid-cols-2">
        <div className="glass-card p-6 rounded-2xl">
          <h3 className="text-xl font-bold text-white mb-6 flex items-center">
            <PieChart className="w-5 h-5 mr-2 text-cyan-400" />
            Revenue Distribution
          </h3>
          <div className="space-y-4">
            {Object.entries(analytics.revenueByPlan).map(([plan, revenue]) => {
              const percentage = (revenue / analytics.totalRevenue * 100).toFixed(1)
              return (
                <div key={plan} className="flex items-center justify-between">
                  <div className="flex items-center space-x-3">
                    <div className={`w-3 h-3 rounded-full ${
                      plan === 'enterprise' ? 'bg-purple-500' :
                      plan === 'ai-premium' ? 'bg-cyan-500' :
                      plan === 'pro' ? 'bg-blue-500' :
                      'bg-gray-500'
                    }`} />
                    <span className="text-white capitalize">{plan.replace('-', ' ')}</span>
                  </div>
                  <div className="text-right">
                    <div className="text-white font-medium">₹{(revenue / 100000).toFixed(1)}L</div>
                    <div className="text-slate-400 text-sm">{percentage}%</div>
                  </div>
                </div>
              )
            })}
          </div>
        </div>

        <div className="glass-card p-6 rounded-2xl">
          <h3 className="text-xl font-bold text-white mb-6 flex items-center">
            <LineChart className="w-5 h-5 mr-2 text-green-400" />
            Key Performance Indicators
          </h3>
          <div className="space-y-4">
            <div className="flex items-center justify-between p-4 bg-slate-800/30 rounded-xl">
              <div>
                <p className="text-slate-400">New Subscriptions</p>
                <p className="text-white text-xl font-bold">{analytics.newSubscriptions}</p>
              </div>
              <div className="text-green-400">
                <ArrowUp className="w-5 h-5" />
              </div>
            </div>
            
            <div className="flex items-center justify-between p-4 bg-slate-800/30 rounded-xl">
              <div>
                <p className="text-slate-400">Upgrade Rate</p>
                <p className="text-white text-xl font-bold">{analytics.upgradeRate}%</p>
              </div>
              <div className="text-blue-400">
                <TrendingUp className="w-5 h-5" />
              </div>
            </div>
            
            <div className="flex items-center justify-between p-4 bg-slate-800/30 rounded-xl">
              <div>
                <p className="text-slate-400">Trial Conversions</p>
                <p className="text-white text-xl font-bold">{trialSubscriptions}</p>
              </div>
              <div className="text-purple-400">
                <Target className="w-5 h-5" />
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Recent Activity */}
      <div className="glass-card p-6 rounded-2xl">
        <h3 className="text-xl font-bold text-white mb-6 flex items-center">
          <Clock className="w-5 h-5 mr-2 text-orange-400" />
          Recent Subscription Activity
        </h3>
        <div className="space-y-4">
          {subscriptions.slice(0, 8).map((sub) => (
            <div key={sub.id} className="flex items-center justify-between p-4 bg-slate-800/30 rounded-xl hover:bg-slate-700/30 transition-colors">
              <div className="flex items-center space-x-4">
                <div className={`w-3 h-3 rounded-full ${
                  sub.status === 'active' ? 'bg-green-400' :
                  sub.status === 'trial' ? 'bg-blue-400' :
                  sub.status === 'cancelled' ? 'bg-red-400' :
                  'bg-yellow-400'
                }`} />
                <div>
                  <p className="text-white font-medium">{sub.userEmail}</p>
                  <p className="text-slate-400 text-sm capitalize">{sub.plan} • {sub.status}</p>
                </div>
              </div>
              <div className="text-right">
                <p className="text-white font-medium">₹{sub.amount.toLocaleString()}</p>
                <p className="text-slate-400 text-sm">{sub.billingCycle}</p>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}

// Additional tab components would be implemented here...
// For brevity, I'll include just the structure for the remaining tabs

const SubscriptionsTab: React.FC<any> = ({ subscriptions, onUpgrade, onCancel, loading }) => {
  return (
    <div className="glass-card rounded-2xl overflow-hidden">
      <div className="p-6 border-b border-slate-700/50">
        <h3 className="text-xl font-bold text-white flex items-center">
          <CreditCard className="w-5 h-5 mr-2 text-purple-400" />
          Subscription Management ({subscriptions.length} subscriptions)
        </h3>
      </div>
      {/* Subscriptions table would go here */}
      <div className="p-6">
        <p className="text-slate-400">Subscription management interface coming soon...</p>
      </div>
    </div>
  )
}

const AnalyticsTab: React.FC<any> = ({ analytics }) => {
  return (
    <div className="glass-card rounded-2xl p-6">
      <h3 className="text-xl font-bold text-white mb-6 flex items-center">
        <BarChart3 className="w-5 h-5 mr-2 text-cyan-400" />
        Subscription Analytics
      </h3>
      <p className="text-slate-400">Advanced analytics interface coming soon...</p>
    </div>
  )
}

const UsageMonitoringTab: React.FC<any> = ({ alerts, subscriptions, onResolveAlert, loading }) => {
  return (
    <div className="glass-card rounded-2xl p-6">
      <h3 className="text-xl font-bold text-white mb-6 flex items-center">
        <AlertTriangle className="w-5 h-5 mr-2 text-yellow-400" />
        Usage Monitoring ({alerts.length} alerts)
      </h3>
      <p className="text-slate-400">Usage monitoring interface coming soon...</p>
    </div>
  )
}

const BillingManagementTab: React.FC<any> = ({ subscriptions }) => {
  return (
    <div className="glass-card rounded-2xl p-6">
      <h3 className="text-xl font-bold text-white mb-6 flex items-center">
        <DollarSign className="w-5 h-5 mr-2 text-green-400" />
        Billing Management
      </h3>
      <p className="text-slate-400">Billing management interface coming soon...</p>
    </div>
  )
}

const ChurnManagementTab: React.FC<any> = ({ subscriptions }) => {
  return (
    <div className="glass-card rounded-2xl p-6">
      <h3 className="text-xl font-bold text-white mb-6 flex items-center">
        <Users className="w-5 h-5 mr-2 text-red-400" />
        Churn Management
      </h3>
      <p className="text-slate-400">Churn management interface coming soon...</p>
    </div>
  )
}

export default AdvancedSubscriptionManagement