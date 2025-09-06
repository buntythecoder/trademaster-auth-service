import React, { useState, useEffect, useCallback } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import {
  Users, Settings, Shield, Activity, FileText, DollarSign, AlertTriangle,
  Search, Filter, Download, Edit3, Trash2, Plus, Eye, EyeOff, RefreshCw,
  Server, Database, Wifi, HardDrive, Cpu, CheckCircle, XCircle,
  Clock, TrendingUp, TrendingDown, Bell, Mail, Phone, Calendar, MapPin,
  CreditCard, Building, Globe, Star, Flag, Hash, Lock, Unlock, UserCheck,
  UserX, UserPlus, MoreVertical, Save, X, Check, AlertCircle, Info
} from 'lucide-react'

// Types
interface User {
  id: string
  email: string
  firstName: string
  lastName: string
  phone?: string
  country: string
  kycStatus: 'pending' | 'approved' | 'rejected'
  subscriptionTier: 'free' | 'pro' | 'ai-premium' | 'enterprise'
  status: 'active' | 'suspended' | 'banned'
  lastLogin: Date
  createdAt: Date
  totalTrades: number
  portfolioValue: number
  riskScore: number
  brokerConnections: string[]
  flags: string[]
}

interface SystemService {
  id: string
  name: string
  type: 'microservice' | 'database' | 'cache' | 'gateway' | 'message-queue'
  status: 'healthy' | 'degraded' | 'down' | 'maintenance'
  uptime: string
  responseTime: number
  memoryUsage: number
  cpuUsage: number
  lastCheck: Date
  endpoint?: string
  version: string
  replicas?: number
}

interface AuditLog {
  id: string
  timestamp: Date
  adminUser: string
  action: string
  targetType: 'user' | 'system' | 'payment' | 'subscription' | 'security'
  targetId: string
  details: string
  severity: 'info' | 'warning' | 'error' | 'critical'
  ipAddress: string
}

interface PaymentTransaction {
  id: string
  userId: string
  userEmail: string
  amount: number
  currency: string
  status: 'pending' | 'completed' | 'failed' | 'refunded' | 'disputed'
  paymentMethod: 'razorpay' | 'stripe' | 'upi' | 'netbanking' | 'card'
  subscriptionId?: string
  createdAt: Date
  gatewayTransactionId: string
  refundAmount?: number
  disputeReason?: string
}

interface SystemConfig {
  id: string
  service: string
  key: string
  value: string
  type: 'string' | 'number' | 'boolean' | 'json'
  description: string
  isSecret: boolean
  lastModified: Date
  modifiedBy: string
}

interface AdvancedAdminManagementProps {
  initialTab?: 'dashboard' | 'users' | 'payments' | 'system' | 'config' | 'audit' | 'health'
}

export const AdvancedAdminManagement: React.FC<AdvancedAdminManagementProps> = ({ initialTab = 'dashboard' }) => {
  const [activeTab, setActiveTab] = useState<'dashboard' | 'users' | 'payments' | 'system' | 'config' | 'audit' | 'health'>(initialTab)
  const [searchTerm, setSearchTerm] = useState('')
  const [selectedFilters, setSelectedFilters] = useState<Record<string, any>>({})
  const [loading, setLoading] = useState(false)
  const [notification, setNotification] = useState<{ type: 'success' | 'error' | 'warning' | 'info', message: string } | null>(null)

  // Mock data - replace with real API calls
  const [users, setUsers] = useState<User[]>([])
  const [systemServices, setSystemServices] = useState<SystemService[]>([])
  const [auditLogs, setAuditLogs] = useState<AuditLog[]>([])
  const [payments, setPayments] = useState<PaymentTransaction[]>([])
  const [systemConfigs, setSystemConfigs] = useState<SystemConfig[]>([])

  // Load mock data
  useEffect(() => {
    loadMockData()
  }, [])

  const loadMockData = useCallback(() => {
    // Mock Users
    const mockUsers: User[] = Array.from({ length: 50 }, (_, i) => ({
      id: `user-${i + 1}`,
      email: `user${i + 1}@example.com`,
      firstName: `User${i + 1}`,
      lastName: `LastName${i + 1}`,
      phone: `+91${Math.floor(Math.random() * 9000000000) + 1000000000}`,
      country: ['India', 'USA', 'UK', 'Canada', 'Australia'][Math.floor(Math.random() * 5)],
      kycStatus: ['pending', 'approved', 'rejected'][Math.floor(Math.random() * 3)] as any,
      subscriptionTier: ['free', 'pro', 'ai-premium', 'enterprise'][Math.floor(Math.random() * 4)] as any,
      status: ['active', 'suspended', 'banned'][Math.floor(Math.random() * 3)] as any,
      lastLogin: new Date(Date.now() - Math.floor(Math.random() * 30) * 24 * 60 * 60 * 1000),
      createdAt: new Date(Date.now() - Math.floor(Math.random() * 365) * 24 * 60 * 60 * 1000),
      totalTrades: Math.floor(Math.random() * 1000),
      portfolioValue: Math.floor(Math.random() * 10000000),
      riskScore: Math.floor(Math.random() * 100),
      brokerConnections: ['Zerodha', 'Upstox', 'Angel One'].slice(0, Math.floor(Math.random() * 3) + 1),
      flags: Math.random() > 0.8 ? ['High Risk', 'Large Volume'] : []
    }))

    // Mock System Services
    const mockServices: SystemService[] = [
      {
        id: 'auth-service',
        name: 'Authentication Service',
        type: 'microservice',
        status: 'healthy',
        uptime: '99.9%',
        responseTime: 45,
        memoryUsage: 68,
        cpuUsage: 23,
        lastCheck: new Date(),
        endpoint: 'https://auth.trademaster.com',
        version: '1.2.3',
        replicas: 3
      },
      {
        id: 'market-data-service',
        name: 'Market Data Service',
        type: 'microservice',
        status: 'degraded',
        uptime: '98.2%',
        responseTime: 120,
        memoryUsage: 85,
        cpuUsage: 67,
        lastCheck: new Date(),
        endpoint: 'https://market.trademaster.com',
        version: '2.1.0',
        replicas: 5
      },
      {
        id: 'trading-service',
        name: 'Trading Service',
        type: 'microservice',
        status: 'healthy',
        uptime: '99.7%',
        responseTime: 67,
        memoryUsage: 52,
        cpuUsage: 34,
        lastCheck: new Date(),
        endpoint: 'https://trading.trademaster.com',
        version: '1.5.2',
        replicas: 4
      },
      {
        id: 'postgres-primary',
        name: 'PostgreSQL Primary',
        type: 'database',
        status: 'healthy',
        uptime: '99.95%',
        responseTime: 12,
        memoryUsage: 72,
        cpuUsage: 28,
        lastCheck: new Date(),
        version: '14.2'
      },
      {
        id: 'redis-cache',
        name: 'Redis Cache',
        type: 'cache',
        status: 'healthy',
        uptime: '99.8%',
        responseTime: 3,
        memoryUsage: 45,
        cpuUsage: 15,
        lastCheck: new Date(),
        version: '7.0.5'
      },
      {
        id: 'kafka-cluster',
        name: 'Kafka Message Queue',
        type: 'message-queue',
        status: 'healthy',
        uptime: '99.6%',
        responseTime: 23,
        memoryUsage: 63,
        cpuUsage: 41,
        lastCheck: new Date(),
        version: '3.2.0'
      }
    ]

    // Mock Audit Logs
    const mockAuditLogs: AuditLog[] = Array.from({ length: 100 }, (_, i) => ({
      id: `audit-${i + 1}`,
      timestamp: new Date(Date.now() - Math.floor(Math.random() * 7) * 24 * 60 * 60 * 1000),
      adminUser: ['admin@trademaster.com', 'superadmin@trademaster.com', 'support@trademaster.com'][Math.floor(Math.random() * 3)],
      action: ['User Suspended', 'Payment Refunded', 'System Config Changed', 'KYC Approved', 'Security Alert Resolved'][Math.floor(Math.random() * 5)],
      targetType: ['user', 'system', 'payment', 'subscription', 'security'][Math.floor(Math.random() * 5)] as any,
      targetId: `target-${i + 1}`,
      details: `Detailed information about action ${i + 1}`,
      severity: ['info', 'warning', 'error', 'critical'][Math.floor(Math.random() * 4)] as any,
      ipAddress: `192.168.1.${Math.floor(Math.random() * 255)}`
    }))

    // Mock Payments
    const mockPayments: PaymentTransaction[] = Array.from({ length: 200 }, (_, i) => ({
      id: `payment-${i + 1}`,
      userId: `user-${Math.floor(Math.random() * 50) + 1}`,
      userEmail: `user${Math.floor(Math.random() * 50) + 1}@example.com`,
      amount: Math.floor(Math.random() * 50000) + 1000,
      currency: 'INR',
      status: ['pending', 'completed', 'failed', 'refunded', 'disputed'][Math.floor(Math.random() * 5)] as any,
      paymentMethod: ['razorpay', 'stripe', 'upi', 'netbanking', 'card'][Math.floor(Math.random() * 5)] as any,
      subscriptionId: Math.random() > 0.3 ? `sub-${i + 1}` : undefined,
      createdAt: new Date(Date.now() - Math.floor(Math.random() * 30) * 24 * 60 * 60 * 1000),
      gatewayTransactionId: `gw-${Math.random().toString(36).substring(7)}`
    }))

    // Mock System Config
    const mockConfigs: SystemConfig[] = [
      {
        id: 'config-1',
        service: 'authentication',
        key: 'JWT_EXPIRY',
        value: '24h',
        type: 'string',
        description: 'JWT token expiration time',
        isSecret: false,
        lastModified: new Date(),
        modifiedBy: 'admin@trademaster.com'
      },
      {
        id: 'config-2',
        service: 'trading',
        key: 'MAX_ORDER_SIZE',
        value: '10000000',
        type: 'number',
        description: 'Maximum order size in INR',
        isSecret: false,
        lastModified: new Date(),
        modifiedBy: 'admin@trademaster.com'
      },
      {
        id: 'config-3',
        service: 'payments',
        key: 'RAZORPAY_SECRET',
        value: '********************',
        type: 'string',
        description: 'Razorpay API secret key',
        isSecret: true,
        lastModified: new Date(),
        modifiedBy: 'superadmin@trademaster.com'
      }
    ]

    setUsers(mockUsers)
    setSystemServices(mockServices)
    setAuditLogs(mockAuditLogs)
    setPayments(mockPayments)
    setSystemConfigs(mockConfigs)
  }, [])

  // Show notification
  const showNotification = (type: 'success' | 'error' | 'warning' | 'info', message: string) => {
    setNotification({ type, message })
    setTimeout(() => setNotification(null), 5000)
  }

  // User Management Functions
  const suspendUser = async (userId: string) => {
    setLoading(true)
    try {
      // Mock API call
      await new Promise(resolve => setTimeout(resolve, 1000))
      setUsers(prev => prev.map(user => 
        user.id === userId ? { ...user, status: 'suspended' } : user
      ))
      showNotification('success', 'User suspended successfully')
    } catch (error) {
      showNotification('error', 'Failed to suspend user')
    } finally {
      setLoading(false)
    }
  }

  const activateUser = async (userId: string) => {
    setLoading(true)
    try {
      await new Promise(resolve => setTimeout(resolve, 1000))
      setUsers(prev => prev.map(user => 
        user.id === userId ? { ...user, status: 'active' } : user
      ))
      showNotification('success', 'User activated successfully')
    } catch (error) {
      showNotification('error', 'Failed to activate user')
    } finally {
      setLoading(false)
    }
  }

  const approveKYC = async (userId: string) => {
    setLoading(true)
    try {
      await new Promise(resolve => setTimeout(resolve, 1000))
      setUsers(prev => prev.map(user => 
        user.id === userId ? { ...user, kycStatus: 'approved' } : user
      ))
      showNotification('success', 'KYC approved successfully')
    } catch (error) {
      showNotification('error', 'Failed to approve KYC')
    } finally {
      setLoading(false)
    }
  }

  const rejectKYC = async (userId: string) => {
    setLoading(true)
    try {
      await new Promise(resolve => setTimeout(resolve, 1000))
      setUsers(prev => prev.map(user => 
        user.id === userId ? { ...user, kycStatus: 'rejected' } : user
      ))
      showNotification('success', 'KYC rejected successfully')
    } catch (error) {
      showNotification('error', 'Failed to reject KYC')
    } finally {
      setLoading(false)
    }
  }

  // Payment Management Functions
  const processRefund = async (paymentId: string, amount: number) => {
    setLoading(true)
    try {
      await new Promise(resolve => setTimeout(resolve, 2000))
      setPayments(prev => prev.map(payment => 
        payment.id === paymentId 
          ? { ...payment, status: 'refunded', refundAmount: amount }
          : payment
      ))
      showNotification('success', `Refund of ₹${amount.toLocaleString()} processed successfully`)
    } catch (error) {
      showNotification('error', 'Failed to process refund')
    } finally {
      setLoading(false)
    }
  }

  // System Config Functions
  const updateConfig = async (configId: string, newValue: string) => {
    setLoading(true)
    try {
      await new Promise(resolve => setTimeout(resolve, 1000))
      setSystemConfigs(prev => prev.map(config => 
        config.id === configId 
          ? { ...config, value: newValue, lastModified: new Date(), modifiedBy: 'admin@trademaster.com' }
          : config
      ))
      showNotification('success', 'Configuration updated successfully')
    } catch (error) {
      showNotification('error', 'Failed to update configuration')
    } finally {
      setLoading(false)
    }
  }

  // Filter and search functions
  const filteredUsers = users.filter(user => {
    const matchesSearch = searchTerm === '' || 
      user.email.toLowerCase().includes(searchTerm.toLowerCase()) ||
      user.firstName.toLowerCase().includes(searchTerm.toLowerCase()) ||
      user.lastName.toLowerCase().includes(searchTerm.toLowerCase())
    
    const matchesFilters = Object.entries(selectedFilters).every(([key, value]) => {
      if (!value) return true
      return (user as any)[key] === value
    })
    
    return matchesSearch && matchesFilters
  })

  const filteredPayments = payments.filter(payment => {
    const matchesSearch = searchTerm === '' ||
      payment.userEmail.toLowerCase().includes(searchTerm.toLowerCase()) ||
      payment.id.toLowerCase().includes(searchTerm.toLowerCase()) ||
      payment.gatewayTransactionId.toLowerCase().includes(searchTerm.toLowerCase())
    
    return matchesSearch
  })

  const filteredAuditLogs = auditLogs.filter(log => {
    const matchesSearch = searchTerm === '' ||
      log.adminUser.toLowerCase().includes(searchTerm.toLowerCase()) ||
      log.action.toLowerCase().includes(searchTerm.toLowerCase()) ||
      log.details.toLowerCase().includes(searchTerm.toLowerCase())
    
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
              {notification.type === 'success' && <CheckCircle className="w-5 h-5" />}
              {notification.type === 'error' && <XCircle className="w-5 h-5" />}
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
          Advanced Admin Management
        </h1>
        <p className="text-slate-400 text-lg">
          Complete administrative control and operational visibility
        </p>
      </div>

      {/* Navigation Tabs */}
      <div className="mb-8">
        <div className="flex flex-wrap gap-2 p-1 bg-slate-800/50 rounded-xl border border-slate-700/50">
          {[
            { id: 'dashboard', label: 'Dashboard', icon: Activity },
            { id: 'users', label: 'User Management', icon: Users },
            { id: 'payments', label: 'Payment Management', icon: DollarSign },
            { id: 'system', label: 'System Health', icon: Server },
            { id: 'config', label: 'Configuration', icon: Settings },
            { id: 'audit', label: 'Audit Logs', icon: FileText },
            { id: 'health', label: 'Health Monitoring', icon: Shield }
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
      {(activeTab === 'users' || activeTab === 'payments' || activeTab === 'audit') && (
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
          
          {activeTab === 'users' && (
            <div className="flex gap-2">
              <select
                value={selectedFilters.status || ''}
                onChange={(e) => setSelectedFilters(prev => ({ ...prev, status: e.target.value || null }))}
                className="px-4 py-3 bg-slate-800/50 border border-slate-700/50 rounded-xl text-white focus:outline-none focus:ring-2 focus:ring-purple-500/50"
              >
                <option value="">All Status</option>
                <option value="active">Active</option>
                <option value="suspended">Suspended</option>
                <option value="banned">Banned</option>
              </select>
              
              <select
                value={selectedFilters.kycStatus || ''}
                onChange={(e) => setSelectedFilters(prev => ({ ...prev, kycStatus: e.target.value || null }))}
                className="px-4 py-3 bg-slate-800/50 border border-slate-700/50 rounded-xl text-white focus:outline-none focus:ring-2 focus:ring-purple-500/50"
              >
                <option value="">All KYC</option>
                <option value="pending">Pending</option>
                <option value="approved">Approved</option>
                <option value="rejected">Rejected</option>
              </select>
              
              <select
                value={selectedFilters.subscriptionTier || ''}
                onChange={(e) => setSelectedFilters(prev => ({ ...prev, subscriptionTier: e.target.value || null }))}
                className="px-4 py-3 bg-slate-800/50 border border-slate-700/50 rounded-xl text-white focus:outline-none focus:ring-2 focus:ring-purple-500/50"
              >
                <option value="">All Tiers</option>
                <option value="free">Free</option>
                <option value="pro">Pro</option>
                <option value="ai-premium">AI Premium</option>
                <option value="enterprise">Enterprise</option>
              </select>
            </div>
          )}
        </div>
      )}

      {/* Content */}
      <div className="space-y-6">
        {activeTab === 'dashboard' && <DashboardTab />}
        {activeTab === 'users' && <UserManagementTab users={filteredUsers} onSuspend={suspendUser} onActivate={activateUser} onApproveKYC={approveKYC} onRejectKYC={rejectKYC} loading={loading} />}
        {activeTab === 'payments' && <PaymentManagementTab payments={filteredPayments} onRefund={processRefund} loading={loading} />}
        {activeTab === 'system' && <SystemHealthTab services={systemServices} />}
        {activeTab === 'config' && <ConfigurationTab configs={systemConfigs} onUpdate={updateConfig} loading={loading} />}
        {activeTab === 'audit' && <AuditLogsTab logs={filteredAuditLogs} />}
        {activeTab === 'health' && <HealthMonitoringTab services={systemServices} />}
      </div>
    </div>
  )
}

// Dashboard Tab Component
const DashboardTab: React.FC = () => {
  return (
    <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-4">
      {/* Overview Stats Cards */}
      <div className="glass-card p-6 rounded-2xl hover:scale-105 transition-all duration-300">
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

      <div className="glass-card p-6 rounded-2xl hover:scale-105 transition-all duration-300">
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
        <p className="text-slate-400 text-sm">requires attention</p>
      </div>

      <div className="glass-card p-6 rounded-2xl hover:scale-105 transition-all duration-300">
        <div className="flex items-center justify-between mb-4">
          <div className="p-3 rounded-xl bg-gradient-to-br from-cyan-500/20 to-cyan-600/20">
            <DollarSign className="h-6 w-6 text-cyan-400" />
          </div>
          <div className="text-right">
            <div className="text-2xl font-bold text-white">₹1.2Cr</div>
            <div className="text-sm text-green-400 flex items-center justify-end">
              <TrendingUp className="h-3 w-3 mr-1" />
              +24%
            </div>
          </div>
        </div>
        <h3 className="text-cyan-400 font-semibold mb-1">Monthly Revenue</h3>
        <p className="text-slate-400 text-sm">from subscriptions</p>
      </div>

      <div className="glass-card p-6 rounded-2xl hover:scale-105 transition-all duration-300">
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
  )
}

// User Management Tab Component
interface UserManagementTabProps {
  users: User[]
  onSuspend: (userId: string) => void
  onActivate: (userId: string) => void
  onApproveKYC: (userId: string) => void
  onRejectKYC: (userId: string) => void
  loading: boolean
}

const UserManagementTab: React.FC<UserManagementTabProps> = ({
  users, onSuspend, onActivate, onApproveKYC, onRejectKYC, loading
}) => {
  const [selectedUser, setSelectedUser] = useState<User | null>(null)

  return (
    <div className="space-y-6">
      {/* User Table */}
      <div className="glass-card rounded-2xl overflow-hidden">
        <div className="p-6 border-b border-slate-700/50">
          <h3 className="text-xl font-bold text-white flex items-center">
            <Users className="w-5 h-5 mr-2 text-purple-400" />
            User Management ({users.length} users)
          </h3>
        </div>
        
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-slate-800/50">
              <tr>
                <th className="text-left p-4 text-slate-400 font-medium">User</th>
                <th className="text-left p-4 text-slate-400 font-medium">Status</th>
                <th className="text-left p-4 text-slate-400 font-medium">KYC</th>
                <th className="text-left p-4 text-slate-400 font-medium">Subscription</th>
                <th className="text-left p-4 text-slate-400 font-medium">Portfolio Value</th>
                <th className="text-left p-4 text-slate-400 font-medium">Last Login</th>
                <th className="text-left p-4 text-slate-400 font-medium">Actions</th>
              </tr>
            </thead>
            <tbody>
              {users.slice(0, 20).map((user, index) => (
                <motion.tr
                  key={user.id}
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: index * 0.05 }}
                  className="border-b border-slate-800/50 hover:bg-slate-800/30"
                >
                  <td className="p-4">
                    <div className="flex items-center space-x-3">
                      <div className="w-10 h-10 rounded-full bg-gradient-to-br from-purple-500 to-cyan-500 flex items-center justify-center text-white font-medium">
                        {user.firstName.charAt(0)}
                      </div>
                      <div>
                        <p className="text-white font-medium">{user.firstName} {user.lastName}</p>
                        <p className="text-slate-400 text-sm">{user.email}</p>
                      </div>
                    </div>
                  </td>
                  <td className="p-4">
                    <span className={`px-3 py-1 rounded-full text-sm font-medium ${
                      user.status === 'active' ? 'bg-green-500/20 text-green-400' :
                      user.status === 'suspended' ? 'bg-yellow-500/20 text-yellow-400' :
                      'bg-red-500/20 text-red-400'
                    }`}>
                      {user.status}
                    </span>
                  </td>
                  <td className="p-4">
                    <span className={`px-3 py-1 rounded-full text-sm font-medium ${
                      user.kycStatus === 'approved' ? 'bg-green-500/20 text-green-400' :
                      user.kycStatus === 'pending' ? 'bg-yellow-500/20 text-yellow-400' :
                      'bg-red-500/20 text-red-400'
                    }`}>
                      {user.kycStatus}
                    </span>
                  </td>
                  <td className="p-4">
                    <span className={`px-3 py-1 rounded-full text-sm font-medium ${
                      user.subscriptionTier === 'enterprise' ? 'bg-purple-500/20 text-purple-400' :
                      user.subscriptionTier === 'ai-premium' ? 'bg-cyan-500/20 text-cyan-400' :
                      user.subscriptionTier === 'pro' ? 'bg-blue-500/20 text-blue-400' :
                      'bg-slate-500/20 text-slate-400'
                    }`}>
                      {user.subscriptionTier}
                    </span>
                  </td>
                  <td className="p-4 text-white">₹{user.portfolioValue.toLocaleString()}</td>
                  <td className="p-4 text-slate-400">{user.lastLogin.toLocaleDateString()}</td>
                  <td className="p-4">
                    <div className="flex items-center space-x-2">
                      <button
                        onClick={() => setSelectedUser(user)}
                        className="p-2 text-blue-400 hover:bg-blue-500/20 rounded-lg transition-colors"
                      >
                        <Eye className="w-4 h-4" />
                      </button>
                      
                      {user.status === 'active' ? (
                        <button
                          onClick={() => onSuspend(user.id)}
                          disabled={loading}
                          className="p-2 text-yellow-400 hover:bg-yellow-500/20 rounded-lg transition-colors disabled:opacity-50"
                        >
                          <UserX className="w-4 h-4" />
                        </button>
                      ) : (
                        <button
                          onClick={() => onActivate(user.id)}
                          disabled={loading}
                          className="p-2 text-green-400 hover:bg-green-500/20 rounded-lg transition-colors disabled:opacity-50"
                        >
                          <UserCheck className="w-4 h-4" />
                        </button>
                      )}
                      
                      {user.kycStatus === 'pending' && (
                        <>
                          <button
                            onClick={() => onApproveKYC(user.id)}
                            disabled={loading}
                            className="p-2 text-green-400 hover:bg-green-500/20 rounded-lg transition-colors disabled:opacity-50"
                          >
                            <Check className="w-4 h-4" />
                          </button>
                          <button
                            onClick={() => onRejectKYC(user.id)}
                            disabled={loading}
                            className="p-2 text-red-400 hover:bg-red-500/20 rounded-lg transition-colors disabled:opacity-50"
                          >
                            <X className="w-4 h-4" />
                          </button>
                        </>
                      )}
                    </div>
                  </td>
                </motion.tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* User Details Modal */}
      <AnimatePresence>
        {selectedUser && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 bg-black/50 backdrop-blur-sm z-50 flex items-center justify-center p-4"
            onClick={() => setSelectedUser(null)}
          >
            <motion.div
              initial={{ scale: 0.95 }}
              animate={{ scale: 1 }}
              exit={{ scale: 0.95 }}
              className="bg-slate-900 border border-slate-700 rounded-2xl p-6 max-w-2xl w-full max-h-[80vh] overflow-y-auto"
              onClick={(e) => e.stopPropagation()}
            >
              <div className="flex justify-between items-center mb-6">
                <h3 className="text-xl font-bold text-white">User Details</h3>
                <button
                  onClick={() => setSelectedUser(null)}
                  className="p-2 text-slate-400 hover:text-white transition-colors"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>
              
              <div className="grid gap-6 md:grid-cols-2">
                <div>
                  <h4 className="text-lg font-semibold text-white mb-4">Basic Information</h4>
                  <div className="space-y-3">
                    <div>
                      <span className="text-slate-400">Name:</span>
                      <span className="text-white ml-2">{selectedUser.firstName} {selectedUser.lastName}</span>
                    </div>
                    <div>
                      <span className="text-slate-400">Email:</span>
                      <span className="text-white ml-2">{selectedUser.email}</span>
                    </div>
                    <div>
                      <span className="text-slate-400">Phone:</span>
                      <span className="text-white ml-2">{selectedUser.phone}</span>
                    </div>
                    <div>
                      <span className="text-slate-400">Country:</span>
                      <span className="text-white ml-2">{selectedUser.country}</span>
                    </div>
                    <div>
                      <span className="text-slate-400">Member Since:</span>
                      <span className="text-white ml-2">{selectedUser.createdAt.toLocaleDateString()}</span>
                    </div>
                  </div>
                </div>
                
                <div>
                  <h4 className="text-lg font-semibold text-white mb-4">Account Status</h4>
                  <div className="space-y-3">
                    <div>
                      <span className="text-slate-400">Status:</span>
                      <span className={`ml-2 px-2 py-1 rounded text-sm ${
                        selectedUser.status === 'active' ? 'bg-green-500/20 text-green-400' :
                        selectedUser.status === 'suspended' ? 'bg-yellow-500/20 text-yellow-400' :
                        'bg-red-500/20 text-red-400'
                      }`}>
                        {selectedUser.status}
                      </span>
                    </div>
                    <div>
                      <span className="text-slate-400">KYC Status:</span>
                      <span className={`ml-2 px-2 py-1 rounded text-sm ${
                        selectedUser.kycStatus === 'approved' ? 'bg-green-500/20 text-green-400' :
                        selectedUser.kycStatus === 'pending' ? 'bg-yellow-500/20 text-yellow-400' :
                        'bg-red-500/20 text-red-400'
                      }`}>
                        {selectedUser.kycStatus}
                      </span>
                    </div>
                    <div>
                      <span className="text-slate-400">Subscription:</span>
                      <span className={`ml-2 px-2 py-1 rounded text-sm ${
                        selectedUser.subscriptionTier === 'enterprise' ? 'bg-purple-500/20 text-purple-400' :
                        selectedUser.subscriptionTier === 'ai-premium' ? 'bg-cyan-500/20 text-cyan-400' :
                        selectedUser.subscriptionTier === 'pro' ? 'bg-blue-500/20 text-blue-400' :
                        'bg-slate-500/20 text-slate-400'
                      }`}>
                        {selectedUser.subscriptionTier}
                      </span>
                    </div>
                    <div>
                      <span className="text-slate-400">Risk Score:</span>
                      <span className={`text-white ml-2 ${
                        selectedUser.riskScore > 70 ? 'text-red-400' :
                        selectedUser.riskScore > 40 ? 'text-yellow-400' :
                        'text-green-400'
                      }`}>
                        {selectedUser.riskScore}/100
                      </span>
                    </div>
                  </div>
                </div>
              </div>
              
              <div className="mt-6">
                <h4 className="text-lg font-semibold text-white mb-4">Trading Information</h4>
                <div className="grid gap-4 md:grid-cols-3">
                  <div className="glass-card p-4 rounded-xl">
                    <p className="text-slate-400 text-sm">Total Trades</p>
                    <p className="text-white text-xl font-bold">{selectedUser.totalTrades}</p>
                  </div>
                  <div className="glass-card p-4 rounded-xl">
                    <p className="text-slate-400 text-sm">Portfolio Value</p>
                    <p className="text-white text-xl font-bold">₹{selectedUser.portfolioValue.toLocaleString()}</p>
                  </div>
                  <div className="glass-card p-4 rounded-xl">
                    <p className="text-slate-400 text-sm">Broker Connections</p>
                    <p className="text-white text-xl font-bold">{selectedUser.brokerConnections.length}</p>
                  </div>
                </div>
              </div>
              
              {selectedUser.brokerConnections.length > 0 && (
                <div className="mt-6">
                  <h4 className="text-lg font-semibold text-white mb-4">Connected Brokers</h4>
                  <div className="flex flex-wrap gap-2">
                    {selectedUser.brokerConnections.map((broker) => (
                      <span key={broker} className="px-3 py-1 bg-blue-500/20 text-blue-400 rounded-full text-sm">
                        {broker}
                      </span>
                    ))}
                  </div>
                </div>
              )}
              
              {selectedUser.flags.length > 0 && (
                <div className="mt-6">
                  <h4 className="text-lg font-semibold text-white mb-4">Flags</h4>
                  <div className="flex flex-wrap gap-2">
                    {selectedUser.flags.map((flag) => (
                      <span key={flag} className="px-3 py-1 bg-red-500/20 text-red-400 rounded-full text-sm">
                        <Flag className="w-3 h-3 inline mr-1" />
                        {flag}
                      </span>
                    ))}
                  </div>
                </div>
              )}
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  )
}

// Payment Management Tab Component
interface PaymentManagementTabProps {
  payments: PaymentTransaction[]
  onRefund: (paymentId: string, amount: number) => void
  loading: boolean
}

const PaymentManagementTab: React.FC<PaymentManagementTabProps> = ({
  payments, onRefund, loading
}) => {
  const [selectedPayment, setSelectedPayment] = useState<PaymentTransaction | null>(null)
  const [refundAmount, setRefundAmount] = useState<number>(0)
  const [showRefundModal, setShowRefundModal] = useState(false)

  const handleRefund = () => {
    if (selectedPayment && refundAmount > 0) {
      onRefund(selectedPayment.id, refundAmount)
      setShowRefundModal(false)
      setSelectedPayment(null)
      setRefundAmount(0)
    }
  }

  const totalRevenue = payments
    .filter(p => p.status === 'completed')
    .reduce((sum, p) => sum + p.amount, 0)

  const pendingPayments = payments.filter(p => p.status === 'pending').length
  const failedPayments = payments.filter(p => p.status === 'failed').length

  return (
    <div className="space-y-6">
      {/* Payment Stats */}
      <div className="grid gap-6 md:grid-cols-4">
        <div className="glass-card p-6 rounded-2xl">
          <div className="flex items-center justify-between mb-4">
            <div className="p-3 rounded-xl bg-gradient-to-br from-green-500/20 to-green-600/20">
              <DollarSign className="h-6 w-6 text-green-400" />
            </div>
            <div className="text-right">
              <div className="text-2xl font-bold text-white">₹{totalRevenue.toLocaleString()}</div>
            </div>
          </div>
          <h3 className="text-green-400 font-semibold mb-1">Total Revenue</h3>
          <p className="text-slate-400 text-sm">from completed payments</p>
        </div>

        <div className="glass-card p-6 rounded-2xl">
          <div className="flex items-center justify-between mb-4">
            <div className="p-3 rounded-xl bg-gradient-to-br from-yellow-500/20 to-yellow-600/20">
              <Clock className="h-6 w-6 text-yellow-400" />
            </div>
            <div className="text-right">
              <div className="text-2xl font-bold text-white">{pendingPayments}</div>
            </div>
          </div>
          <h3 className="text-yellow-400 font-semibold mb-1">Pending Payments</h3>
          <p className="text-slate-400 text-sm">awaiting processing</p>
        </div>

        <div className="glass-card p-6 rounded-2xl">
          <div className="flex items-center justify-between mb-4">
            <div className="p-3 rounded-xl bg-gradient-to-br from-red-500/20 to-red-600/20">
              <XCircle className="h-6 w-6 text-red-400" />
            </div>
            <div className="text-right">
              <div className="text-2xl font-bold text-white">{failedPayments}</div>
            </div>
          </div>
          <h3 className="text-red-400 font-semibold mb-1">Failed Payments</h3>
          <p className="text-slate-400 text-sm">requires attention</p>
        </div>

        <div className="glass-card p-6 rounded-2xl">
          <div className="flex items-center justify-between mb-4">
            <div className="p-3 rounded-xl bg-gradient-to-br from-blue-500/20 to-blue-600/20">
              <RefreshCw className="h-6 w-6 text-blue-400" />
            </div>
            <div className="text-right">
              <div className="text-2xl font-bold text-white">{payments.filter(p => p.status === 'refunded').length}</div>
            </div>
          </div>
          <h3 className="text-blue-400 font-semibold mb-1">Refunded</h3>
          <p className="text-slate-400 text-sm">total refunds processed</p>
        </div>
      </div>

      {/* Payment Table */}
      <div className="glass-card rounded-2xl overflow-hidden">
        <div className="p-6 border-b border-slate-700/50">
          <h3 className="text-xl font-bold text-white flex items-center">
            <DollarSign className="w-5 h-5 mr-2 text-cyan-400" />
            Payment Transactions ({payments.length})
          </h3>
        </div>
        
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-slate-800/50">
              <tr>
                <th className="text-left p-4 text-slate-400 font-medium">Transaction ID</th>
                <th className="text-left p-4 text-slate-400 font-medium">User</th>
                <th className="text-left p-4 text-slate-400 font-medium">Amount</th>
                <th className="text-left p-4 text-slate-400 font-medium">Status</th>
                <th className="text-left p-4 text-slate-400 font-medium">Method</th>
                <th className="text-left p-4 text-slate-400 font-medium">Date</th>
                <th className="text-left p-4 text-slate-400 font-medium">Actions</th>
              </tr>
            </thead>
            <tbody>
              {payments.slice(0, 20).map((payment, index) => (
                <motion.tr
                  key={payment.id}
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: index * 0.05 }}
                  className="border-b border-slate-800/50 hover:bg-slate-800/30"
                >
                  <td className="p-4">
                    <div>
                      <p className="text-white font-mono text-sm">{payment.id}</p>
                      <p className="text-slate-400 text-xs">{payment.gatewayTransactionId}</p>
                    </div>
                  </td>
                  <td className="p-4">
                    <div>
                      <p className="text-white">{payment.userEmail}</p>
                      <p className="text-slate-400 text-sm">{payment.userId}</p>
                    </div>
                  </td>
                  <td className="p-4">
                    <div>
                      <p className="text-white font-semibold">₹{payment.amount.toLocaleString()}</p>
                      {payment.refundAmount && (
                        <p className="text-red-400 text-sm">Refunded: ₹{payment.refundAmount.toLocaleString()}</p>
                      )}
                    </div>
                  </td>
                  <td className="p-4">
                    <span className={`px-3 py-1 rounded-full text-sm font-medium ${
                      payment.status === 'completed' ? 'bg-green-500/20 text-green-400' :
                      payment.status === 'pending' ? 'bg-yellow-500/20 text-yellow-400' :
                      payment.status === 'failed' ? 'bg-red-500/20 text-red-400' :
                      payment.status === 'refunded' ? 'bg-blue-500/20 text-blue-400' :
                      'bg-purple-500/20 text-purple-400'
                    }`}>
                      {payment.status}
                    </span>
                  </td>
                  <td className="p-4">
                    <span className="text-slate-300 capitalize">{payment.paymentMethod}</span>
                  </td>
                  <td className="p-4 text-slate-400">{payment.createdAt.toLocaleDateString()}</td>
                  <td className="p-4">
                    <div className="flex items-center space-x-2">
                      <button
                        onClick={() => setSelectedPayment(payment)}
                        className="p-2 text-blue-400 hover:bg-blue-500/20 rounded-lg transition-colors"
                      >
                        <Eye className="w-4 h-4" />
                      </button>
                      
                      {(payment.status === 'completed' && !payment.refundAmount) && (
                        <button
                          onClick={() => {
                            setSelectedPayment(payment)
                            setRefundAmount(payment.amount)
                            setShowRefundModal(true)
                          }}
                          disabled={loading}
                          className="p-2 text-orange-400 hover:bg-orange-500/20 rounded-lg transition-colors disabled:opacity-50"
                        >
                          <RefreshCw className="w-4 h-4" />
                        </button>
                      )}
                    </div>
                  </td>
                </motion.tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Refund Modal */}
      <AnimatePresence>
        {showRefundModal && selectedPayment && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 bg-black/50 backdrop-blur-sm z-50 flex items-center justify-center p-4"
            onClick={() => setShowRefundModal(false)}
          >
            <motion.div
              initial={{ scale: 0.95 }}
              animate={{ scale: 1 }}
              exit={{ scale: 0.95 }}
              className="bg-slate-900 border border-slate-700 rounded-2xl p-6 max-w-md w-full"
              onClick={(e) => e.stopPropagation()}
            >
              <div className="flex justify-between items-center mb-6">
                <h3 className="text-xl font-bold text-white">Process Refund</h3>
                <button
                  onClick={() => setShowRefundModal(false)}
                  className="p-2 text-slate-400 hover:text-white transition-colors"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>
              
              <div className="space-y-4">
                <div>
                  <p className="text-slate-400 mb-2">Transaction ID:</p>
                  <p className="text-white font-mono">{selectedPayment.id}</p>
                </div>
                
                <div>
                  <p className="text-slate-400 mb-2">Original Amount:</p>
                  <p className="text-white font-semibold">₹{selectedPayment.amount.toLocaleString()}</p>
                </div>
                
                <div>
                  <label className="block text-slate-400 mb-2">Refund Amount:</label>
                  <input
                    type="number"
                    value={refundAmount}
                    onChange={(e) => setRefundAmount(Number(e.target.value))}
                    max={selectedPayment.amount}
                    min={0}
                    className="w-full p-3 bg-slate-800/50 border border-slate-700/50 rounded-xl text-white focus:outline-none focus:ring-2 focus:ring-purple-500/50"
                  />
                </div>
                
                <div className="flex space-x-3 pt-4">
                  <button
                    onClick={handleRefund}
                    disabled={loading || refundAmount <= 0 || refundAmount > selectedPayment.amount}
                    className="flex-1 py-3 bg-gradient-to-r from-orange-500 to-red-500 text-white rounded-xl font-medium hover:shadow-lg transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    {loading ? (
                      <div className="flex items-center justify-center">
                        <RefreshCw className="w-4 h-4 animate-spin mr-2" />
                        Processing...
                      </div>
                    ) : (
                      'Process Refund'
                    )}
                  </button>
                  <button
                    onClick={() => setShowRefundModal(false)}
                    className="px-6 py-3 border border-slate-600 text-slate-400 rounded-xl font-medium hover:bg-slate-800/50 transition-all duration-200"
                  >
                    Cancel
                  </button>
                </div>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  )
}

// System Health Tab Component
interface SystemHealthTabProps {
  services: SystemService[]
}

const SystemHealthTab: React.FC<SystemHealthTabProps> = ({ services }) => {
  const healthyServices = services.filter(s => s.status === 'healthy').length
  const degradedServices = services.filter(s => s.status === 'degraded').length
  const downServices = services.filter(s => s.status === 'down').length

  return (
    <div className="space-y-6">
      {/* Health Overview */}
      <div className="grid gap-6 md:grid-cols-4">
        <div className="glass-card p-6 rounded-2xl">
          <div className="flex items-center justify-between mb-4">
            <div className="p-3 rounded-xl bg-gradient-to-br from-green-500/20 to-green-600/20">
              <CheckCircle className="h-6 w-6 text-green-400" />
            </div>
            <div className="text-right">
              <div className="text-2xl font-bold text-white">{healthyServices}</div>
            </div>
          </div>
          <h3 className="text-green-400 font-semibold mb-1">Healthy Services</h3>
          <p className="text-slate-400 text-sm">operating normally</p>
        </div>

        <div className="glass-card p-6 rounded-2xl">
          <div className="flex items-center justify-between mb-4">
            <div className="p-3 rounded-xl bg-gradient-to-br from-yellow-500/20 to-yellow-600/20">
              <AlertTriangle className="h-6 w-6 text-yellow-400" />
            </div>
            <div className="text-right">
              <div className="text-2xl font-bold text-white">{degradedServices}</div>
            </div>
          </div>
          <h3 className="text-yellow-400 font-semibold mb-1">Degraded Services</h3>
          <p className="text-slate-400 text-sm">performance issues</p>
        </div>

        <div className="glass-card p-6 rounded-2xl">
          <div className="flex items-center justify-between mb-4">
            <div className="p-3 rounded-xl bg-gradient-to-br from-red-500/20 to-red-600/20">
              <XCircle className="h-6 w-6 text-red-400" />
            </div>
            <div className="text-right">
              <div className="text-2xl font-bold text-white">{downServices}</div>
            </div>
          </div>
          <h3 className="text-red-400 font-semibold mb-1">Down Services</h3>
          <p className="text-slate-400 text-sm">requires immediate attention</p>
        </div>

        <div className="glass-card p-6 rounded-2xl">
          <div className="flex items-center justify-between mb-4">
            <div className="p-3 rounded-xl bg-gradient-to-br from-blue-500/20 to-blue-600/20">
              <Activity className="h-6 w-6 text-blue-400" />
            </div>
            <div className="text-right">
              <div className="text-2xl font-bold text-white">{services.length}</div>
            </div>
          </div>
          <h3 className="text-blue-400 font-semibold mb-1">Total Services</h3>
          <p className="text-slate-400 text-sm">monitored services</p>
        </div>
      </div>

      {/* Services List */}
      <div className="glass-card rounded-2xl overflow-hidden">
        <div className="p-6 border-b border-slate-700/50">
          <h3 className="text-xl font-bold text-white flex items-center">
            <Server className="w-5 h-5 mr-2 text-blue-400" />
            System Services
          </h3>
        </div>
        
        <div className="p-6 space-y-4">
          {services.map((service, index) => (
            <motion.div
              key={service.id}
              initial={{ opacity: 0, x: -20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: index * 0.1 }}
              className="flex items-center justify-between p-4 rounded-xl bg-slate-800/30 hover:bg-slate-700/30 transition-colors"
            >
              <div className="flex items-center space-x-4">
                <div className={`w-3 h-3 rounded-full ${
                  service.status === 'healthy' ? 'bg-green-400' :
                  service.status === 'degraded' ? 'bg-yellow-400' :
                  service.status === 'down' ? 'bg-red-400' :
                  'bg-blue-400'
                }`} />
                
                <div>
                  <h4 className="text-white font-semibold">{service.name}</h4>
                  <div className="flex items-center space-x-4 text-sm text-slate-400">
                    <span>Version: {service.version}</span>
                    <span>Uptime: {service.uptime}</span>
                    {service.replicas && <span>Replicas: {service.replicas}</span>}
                    {service.endpoint && <span>Endpoint: {service.endpoint}</span>}
                  </div>
                </div>
              </div>
              
              <div className="flex items-center space-x-6">
                <div className="text-center">
                  <p className="text-sm text-slate-400">Response</p>
                  <p className={`font-semibold ${
                    service.responseTime < 100 ? 'text-green-400' :
                    service.responseTime < 300 ? 'text-yellow-400' :
                    'text-red-400'
                  }`}>
                    {service.responseTime}ms
                  </p>
                </div>
                
                <div className="text-center">
                  <p className="text-sm text-slate-400">Memory</p>
                  <p className={`font-semibold ${
                    service.memoryUsage < 70 ? 'text-green-400' :
                    service.memoryUsage < 85 ? 'text-yellow-400' :
                    'text-red-400'
                  }`}>
                    {service.memoryUsage}%
                  </p>
                </div>
                
                <div className="text-center">
                  <p className="text-sm text-slate-400">CPU</p>
                  <p className={`font-semibold ${
                    service.cpuUsage < 70 ? 'text-green-400' :
                    service.cpuUsage < 85 ? 'text-yellow-400' :
                    'text-red-400'
                  }`}>
                    {service.cpuUsage}%
                  </p>
                </div>
                
                <span className={`px-3 py-1 rounded-full text-sm font-medium ${
                  service.status === 'healthy' ? 'bg-green-500/20 text-green-400' :
                  service.status === 'degraded' ? 'bg-yellow-500/20 text-yellow-400' :
                  service.status === 'down' ? 'bg-red-500/20 text-red-400' :
                  'bg-blue-500/20 text-blue-400'
                }`}>
                  {service.status}
                </span>
              </div>
            </motion.div>
          ))}
        </div>
      </div>
    </div>
  )
}

// Configuration Tab Component
interface ConfigurationTabProps {
  configs: SystemConfig[]
  onUpdate: (configId: string, value: string) => void
  loading: boolean
}

const ConfigurationTab: React.FC<ConfigurationTabProps> = ({
  configs, onUpdate, loading
}) => {
  const [editingConfig, setEditingConfig] = useState<string | null>(null)
  const [editValue, setEditValue] = useState('')

  const handleEdit = (config: SystemConfig) => {
    setEditingConfig(config.id)
    setEditValue(config.isSecret ? '' : config.value)
  }

  const handleSave = () => {
    if (editingConfig && editValue.trim()) {
      onUpdate(editingConfig, editValue.trim())
      setEditingConfig(null)
      setEditValue('')
    }
  }

  const handleCancel = () => {
    setEditingConfig(null)
    setEditValue('')
  }

  return (
    <div className="space-y-6">
      {/* Configuration Table */}
      <div className="glass-card rounded-2xl overflow-hidden">
        <div className="p-6 border-b border-slate-700/50">
          <h3 className="text-xl font-bold text-white flex items-center">
            <Settings className="w-5 h-5 mr-2 text-purple-400" />
            System Configuration ({configs.length} settings)
          </h3>
        </div>
        
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-slate-800/50">
              <tr>
                <th className="text-left p-4 text-slate-400 font-medium">Service</th>
                <th className="text-left p-4 text-slate-400 font-medium">Key</th>
                <th className="text-left p-4 text-slate-400 font-medium">Value</th>
                <th className="text-left p-4 text-slate-400 font-medium">Type</th>
                <th className="text-left p-4 text-slate-400 font-medium">Last Modified</th>
                <th className="text-left p-4 text-slate-400 font-medium">Actions</th>
              </tr>
            </thead>
            <tbody>
              {configs.map((config, index) => (
                <motion.tr
                  key={config.id}
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: index * 0.05 }}
                  className="border-b border-slate-800/50 hover:bg-slate-800/30"
                >
                  <td className="p-4">
                    <span className="px-3 py-1 bg-blue-500/20 text-blue-400 rounded-full text-sm font-medium">
                      {config.service}
                    </span>
                  </td>
                  <td className="p-4">
                    <div>
                      <p className="text-white font-mono">{config.key}</p>
                      <p className="text-slate-400 text-sm">{config.description}</p>
                    </div>
                  </td>
                  <td className="p-4">
                    {editingConfig === config.id ? (
                      <div className="flex space-x-2">
                        <input
                          type={config.isSecret ? 'password' : 'text'}
                          value={editValue}
                          onChange={(e) => setEditValue(e.target.value)}
                          placeholder={config.isSecret ? 'Enter new value...' : ''}
                          className="flex-1 p-2 bg-slate-800/50 border border-slate-700/50 rounded-lg text-white text-sm focus:outline-none focus:ring-2 focus:ring-purple-500/50"
                        />
                        <button
                          onClick={handleSave}
                          disabled={loading || !editValue.trim()}
                          className="p-2 text-green-400 hover:bg-green-500/20 rounded-lg transition-colors disabled:opacity-50"
                        >
                          <Save className="w-4 h-4" />
                        </button>
                        <button
                          onClick={handleCancel}
                          className="p-2 text-red-400 hover:bg-red-500/20 rounded-lg transition-colors"
                        >
                          <X className="w-4 h-4" />
                        </button>
                      </div>
                    ) : (
                      <div className="flex items-center space-x-2">
                        <span className="text-white font-mono">
                          {config.isSecret ? '••••••••••••••••••••' : config.value}
                        </span>
                        {config.isSecret && (
                          <Lock className="w-3 h-3 text-yellow-400" />
                        )}
                      </div>
                    )}
                  </td>
                  <td className="p-4">
                    <span className={`px-2 py-1 rounded text-xs font-medium ${
                      config.type === 'string' ? 'bg-green-500/20 text-green-400' :
                      config.type === 'number' ? 'bg-blue-500/20 text-blue-400' :
                      config.type === 'boolean' ? 'bg-purple-500/20 text-purple-400' :
                      'bg-orange-500/20 text-orange-400'
                    }`}>
                      {config.type}
                    </span>
                  </td>
                  <td className="p-4">
                    <div>
                      <p className="text-slate-400 text-sm">{config.lastModified.toLocaleDateString()}</p>
                      <p className="text-slate-500 text-xs">by {config.modifiedBy}</p>
                    </div>
                  </td>
                  <td className="p-4">
                    {editingConfig !== config.id && (
                      <button
                        onClick={() => handleEdit(config)}
                        disabled={loading}
                        className="p-2 text-blue-400 hover:bg-blue-500/20 rounded-lg transition-colors disabled:opacity-50"
                      >
                        <Edit3 className="w-4 h-4" />
                      </button>
                    )}
                  </td>
                </motion.tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}

// Audit Logs Tab Component
interface AuditLogsTabProps {
  logs: AuditLog[]
}

const AuditLogsTab: React.FC<AuditLogsTabProps> = ({ logs }) => {
  return (
    <div className="space-y-6">
      {/* Audit Stats */}
      <div className="grid gap-6 md:grid-cols-4">
        {['info', 'warning', 'error', 'critical'].map((severity) => {
          const count = logs.filter(log => log.severity === severity).length
          return (
            <div key={severity} className="glass-card p-6 rounded-2xl">
              <div className="flex items-center justify-between mb-4">
                <div className={`p-3 rounded-xl bg-gradient-to-br ${
                  severity === 'info' ? 'from-blue-500/20 to-blue-600/20' :
                  severity === 'warning' ? 'from-yellow-500/20 to-yellow-600/20' :
                  severity === 'error' ? 'from-orange-500/20 to-orange-600/20' :
                  'from-red-500/20 to-red-600/20'
                }`}>
                  {severity === 'info' && <Info className="h-6 w-6 text-blue-400" />}
                  {severity === 'warning' && <AlertTriangle className="h-6 w-6 text-yellow-400" />}
                  {severity === 'error' && <XCircle className="h-6 w-6 text-orange-400" />}
                  {severity === 'critical' && <AlertCircle className="h-6 w-6 text-red-400" />}
                </div>
                <div className="text-right">
                  <div className="text-2xl font-bold text-white">{count}</div>
                </div>
              </div>
              <h3 className={`font-semibold mb-1 capitalize ${
                severity === 'info' ? 'text-blue-400' :
                severity === 'warning' ? 'text-yellow-400' :
                severity === 'error' ? 'text-orange-400' :
                'text-red-400'
              }`}>
                {severity} Logs
              </h3>
              <p className="text-slate-400 text-sm">past 7 days</p>
            </div>
          )
        })}
      </div>

      {/* Audit Logs Table */}
      <div className="glass-card rounded-2xl overflow-hidden">
        <div className="p-6 border-b border-slate-700/50">
          <h3 className="text-xl font-bold text-white flex items-center">
            <FileText className="w-5 h-5 mr-2 text-green-400" />
            Audit Logs ({logs.length})
          </h3>
        </div>
        
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-slate-800/50">
              <tr>
                <th className="text-left p-4 text-slate-400 font-medium">Timestamp</th>
                <th className="text-left p-4 text-slate-400 font-medium">Admin User</th>
                <th className="text-left p-4 text-slate-400 font-medium">Action</th>
                <th className="text-left p-4 text-slate-400 font-medium">Target</th>
                <th className="text-left p-4 text-slate-400 font-medium">Severity</th>
                <th className="text-left p-4 text-slate-400 font-medium">IP Address</th>
              </tr>
            </thead>
            <tbody>
              {logs.slice(0, 50).map((log, index) => (
                <motion.tr
                  key={log.id}
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: index * 0.02 }}
                  className="border-b border-slate-800/50 hover:bg-slate-800/30"
                >
                  <td className="p-4 text-slate-400 text-sm">
                    {log.timestamp.toLocaleString()}
                  </td>
                  <td className="p-4 text-white">{log.adminUser}</td>
                  <td className="p-4">
                    <div>
                      <p className="text-white font-medium">{log.action}</p>
                      <p className="text-slate-400 text-sm truncate max-w-xs">{log.details}</p>
                    </div>
                  </td>
                  <td className="p-4">
                    <div>
                      <span className={`px-2 py-1 rounded text-xs font-medium ${
                        log.targetType === 'user' ? 'bg-blue-500/20 text-blue-400' :
                        log.targetType === 'system' ? 'bg-green-500/20 text-green-400' :
                        log.targetType === 'payment' ? 'bg-purple-500/20 text-purple-400' :
                        log.targetType === 'subscription' ? 'bg-cyan-500/20 text-cyan-400' :
                        'bg-red-500/20 text-red-400'
                      }`}>
                        {log.targetType}
                      </span>
                      <p className="text-slate-400 text-xs mt-1">{log.targetId}</p>
                    </div>
                  </td>
                  <td className="p-4">
                    <span className={`px-3 py-1 rounded-full text-sm font-medium ${
                      log.severity === 'info' ? 'bg-blue-500/20 text-blue-400' :
                      log.severity === 'warning' ? 'bg-yellow-500/20 text-yellow-400' :
                      log.severity === 'error' ? 'bg-orange-500/20 text-orange-400' :
                      'bg-red-500/20 text-red-400'
                    }`}>
                      {log.severity}
                    </span>
                  </td>
                  <td className="p-4 text-slate-400 font-mono text-sm">{log.ipAddress}</td>
                </motion.tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}

// Health Monitoring Tab Component
interface HealthMonitoringTabProps {
  services: SystemService[]
}

const HealthMonitoringTab: React.FC<HealthMonitoringTabProps> = ({ services }) => {
  return (
    <div className="space-y-6">
      {/* System Overview */}
      <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
        <div className="glass-card p-6 rounded-2xl">
          <div className="flex items-center justify-between mb-4">
            <div className="p-3 rounded-xl bg-gradient-to-br from-green-500/20 to-green-600/20">
              <Cpu className="h-6 w-6 text-green-400" />
            </div>
            <div className="text-right">
              <div className="text-2xl font-bold text-white">
                {Math.round(services.reduce((sum, s) => sum + s.cpuUsage, 0) / services.length)}%
              </div>
            </div>
          </div>
          <h3 className="text-green-400 font-semibold mb-1">Average CPU Usage</h3>
          <p className="text-slate-400 text-sm">across all services</p>
        </div>

        <div className="glass-card p-6 rounded-2xl">
          <div className="flex items-center justify-between mb-4">
            <div className="p-3 rounded-xl bg-gradient-to-br from-blue-500/20 to-blue-600/20">
              <HardDrive className="h-6 w-6 text-blue-400" />
            </div>
            <div className="text-right">
              <div className="text-2xl font-bold text-white">
                {Math.round(services.reduce((sum, s) => sum + s.memoryUsage, 0) / services.length)}%
              </div>
            </div>
          </div>
          <h3 className="text-blue-400 font-semibold mb-1">Average Memory Usage</h3>
          <p className="text-slate-400 text-sm">across all services</p>
        </div>

        <div className="glass-card p-6 rounded-2xl">
          <div className="flex items-center justify-between mb-4">
            <div className="p-3 rounded-xl bg-gradient-to-br from-purple-500/20 to-purple-600/20">
              <Activity className="h-6 w-6 text-purple-400" />
            </div>
            <div className="text-right">
              <div className="text-2xl font-bold text-white">
                {Math.round(services.reduce((sum, s) => sum + s.responseTime, 0) / services.length)}ms
              </div>
            </div>
          </div>
          <h3 className="text-purple-400 font-semibold mb-1">Average Response Time</h3>
          <p className="text-slate-400 text-sm">across all services</p>
        </div>
      </div>

      {/* Detailed Service Health */}
      <div className="grid gap-6 md:grid-cols-2">
        {services.map((service, index) => (
          <motion.div
            key={service.id}
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ delay: index * 0.1 }}
            className="glass-card p-6 rounded-2xl"
          >
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center space-x-3">
                <div className={`w-3 h-3 rounded-full ${
                  service.status === 'healthy' ? 'bg-green-400' :
                  service.status === 'degraded' ? 'bg-yellow-400' :
                  service.status === 'down' ? 'bg-red-400' :
                  'bg-blue-400'
                }`} />
                <h3 className="text-white font-semibold">{service.name}</h3>
              </div>
              <span className={`px-2 py-1 rounded text-xs font-medium ${
                service.status === 'healthy' ? 'bg-green-500/20 text-green-400' :
                service.status === 'degraded' ? 'bg-yellow-500/20 text-yellow-400' :
                service.status === 'down' ? 'bg-red-500/20 text-red-400' :
                'bg-blue-500/20 text-blue-400'
              }`}>
                {service.status}
              </span>
            </div>

            <div className="space-y-3">
              <div className="flex justify-between items-center">
                <span className="text-slate-400">Uptime</span>
                <span className="text-white font-medium">{service.uptime}</span>
              </div>

              <div className="flex justify-between items-center">
                <span className="text-slate-400">Response Time</span>
                <span className={`font-medium ${
                  service.responseTime < 100 ? 'text-green-400' :
                  service.responseTime < 300 ? 'text-yellow-400' :
                  'text-red-400'
                }`}>
                  {service.responseTime}ms
                </span>
              </div>

              <div>
                <div className="flex justify-between items-center mb-1">
                  <span className="text-slate-400">CPU Usage</span>
                  <span className={`font-medium ${
                    service.cpuUsage < 70 ? 'text-green-400' :
                    service.cpuUsage < 85 ? 'text-yellow-400' :
                    'text-red-400'
                  }`}>
                    {service.cpuUsage}%
                  </span>
                </div>
                <div className="w-full bg-slate-700 rounded-full h-2">
                  <div
                    className={`h-2 rounded-full transition-all duration-300 ${
                      service.cpuUsage < 70 ? 'bg-green-500' :
                      service.cpuUsage < 85 ? 'bg-yellow-500' :
                      'bg-red-500'
                    }`}
                    style={{ width: `${service.cpuUsage}%` }}
                  />
                </div>
              </div>

              <div>
                <div className="flex justify-between items-center mb-1">
                  <span className="text-slate-400">Memory Usage</span>
                  <span className={`font-medium ${
                    service.memoryUsage < 70 ? 'text-green-400' :
                    service.memoryUsage < 85 ? 'text-yellow-400' :
                    'text-red-400'
                  }`}>
                    {service.memoryUsage}%
                  </span>
                </div>
                <div className="w-full bg-slate-700 rounded-full h-2">
                  <div
                    className={`h-2 rounded-full transition-all duration-300 ${
                      service.memoryUsage < 70 ? 'bg-green-500' :
                      service.memoryUsage < 85 ? 'bg-yellow-500' :
                      'bg-red-500'
                    }`}
                    style={{ width: `${service.memoryUsage}%` }}
                  />
                </div>
              </div>

              <div className="pt-2 border-t border-slate-700/50">
                <div className="flex justify-between items-center text-sm">
                  <span className="text-slate-400">Version</span>
                  <span className="text-slate-300">{service.version}</span>
                </div>
                <div className="flex justify-between items-center text-sm">
                  <span className="text-slate-400">Last Check</span>
                  <span className="text-slate-300">{service.lastCheck.toLocaleTimeString()}</span>
                </div>
              </div>
            </div>
          </motion.div>
        ))}
      </div>
    </div>
  )
}

export default AdvancedAdminManagement