import React, { useState, useEffect, useCallback } from 'react'
import { 
  Users, 
  Settings, 
  Activity, 
  FileText, 
  DollarSign, 
  CreditCard,
  Shield,
  AlertTriangle,
  CheckCircle,
  XCircle,
  Search,
  Filter,
  Download,
  Edit,
  Trash2,
  Plus,
  Eye,
  RefreshCw,
  BarChart3,
  TrendingUp,
  Clock,
  Database,
  Server,
  Wifi,
  WifiOff,
  UserCheck,
  UserX,
  Bell,
  Lock,
  Unlock,
  Calendar,
  Mail,
  Phone,
  MapPin,
  Building,
  Briefcase,
  Star,
  Crown,
  Zap
} from 'lucide-react'

// Admin Management Types
interface User {
  id: string
  email: string
  name: string
  phone: string
  status: 'active' | 'suspended' | 'pending' | 'blocked'
  role: 'admin' | 'user' | 'premium' | 'enterprise'
  subscription: {
    plan: string
    status: 'active' | 'expired' | 'cancelled'
    expiresAt: Date
    renewsAt?: Date
  }
  kyc: {
    status: 'pending' | 'approved' | 'rejected' | 'not_submitted'
    documents?: string[]
    reviewedAt?: Date
    reviewedBy?: string
  }
  createdAt: Date
  lastLoginAt?: Date
  totalRevenue: number
  brokerConnections: number
  tradingVolume: number
  riskScore: number
}

interface ServiceStatus {
  name: string
  status: 'healthy' | 'warning' | 'error' | 'maintenance'
  uptime: number
  responseTime: number
  lastCheck: Date
  version: string
  instances: number
  memoryUsage: number
  cpuUsage: number
  errorRate: number
}

interface SystemMetrics {
  totalUsers: number
  activeUsers: number
  totalRevenue: number
  monthlyRevenue: number
  subscriptionRevenue: number
  totalTransactions: number
  successfulTransactions: number
  pendingKycReviews: number
  systemAlerts: number
  apiCalls: number
  errorRate: number
  averageResponseTime: number
}

interface AuditLog {
  id: string
  timestamp: Date
  userId: string
  userEmail: string
  action: string
  resource: string
  details: string
  ipAddress: string
  userAgent: string
  severity: 'low' | 'medium' | 'high' | 'critical'
  status: 'success' | 'failed' | 'pending'
}

interface AdminProps {
  onUserUpdate?: (userId: string, updates: Partial<User>) => void
  onSystemAlert?: (alert: any) => void
  onKycReview?: (userId: string, decision: 'approve' | 'reject', notes: string) => void
}

export default function AdminDashboard({
  onUserUpdate,
  onSystemAlert,
  onKycReview
}: AdminProps) {
  // State Management
  const [activeTab, setActiveTab] = useState<'dashboard' | 'users' | 'system' | 'payments' | 'kyc' | 'audit' | 'settings'>('dashboard')
  const [users, setUsers] = useState<User[]>([])
  const [services, setServices] = useState<ServiceStatus[]>([])
  const [metrics, setMetrics] = useState<SystemMetrics | null>(null)
  const [auditLogs, setAuditLogs] = useState<AuditLog[]>([])
  const [searchTerm, setSearchTerm] = useState<string>('')
  const [filterStatus, setFilterStatus] = useState<string>('all')
  const [selectedUsers, setSelectedUsers] = useState<Set<string>>(new Set())
  const [isLoading, setIsLoading] = useState<boolean>(false)
  const [showUserModal, setShowUserModal] = useState<boolean>(false)
  const [selectedUser, setSelectedUser] = useState<User | null>(null)

  // Mock Data
  const mockUsers: User[] = [
    {
      id: 'user_1',
      email: 'john.trader@gmail.com',
      name: 'John Trader',
      phone: '+91-9876543210',
      status: 'active',
      role: 'premium',
      subscription: {
        plan: 'Professional',
        status: 'active',
        expiresAt: new Date('2024-12-31'),
        renewsAt: new Date('2024-12-31')
      },
      kyc: {
        status: 'approved',
        documents: ['aadhar', 'pan', 'bank_statement'],
        reviewedAt: new Date('2024-01-15'),
        reviewedBy: 'admin_1'
      },
      createdAt: new Date('2024-01-10'),
      lastLoginAt: new Date('2024-03-20'),
      totalRevenue: 2999,
      brokerConnections: 3,
      tradingVolume: 2500000,
      riskScore: 0.2
    },
    {
      id: 'user_2',
      email: 'sarah.investor@yahoo.com',
      name: 'Sarah Investor',
      phone: '+91-9876543211',
      status: 'active',
      role: 'enterprise',
      subscription: {
        plan: 'Enterprise',
        status: 'active',
        expiresAt: new Date('2024-11-30'),
        renewsAt: new Date('2024-11-30')
      },
      kyc: {
        status: 'approved',
        documents: ['aadhar', 'pan', 'bank_statement'],
        reviewedAt: new Date('2024-02-01'),
        reviewedBy: 'admin_1'
      },
      createdAt: new Date('2024-02-01'),
      lastLoginAt: new Date('2024-03-19'),
      totalRevenue: 9999,
      brokerConnections: 5,
      tradingVolume: 15000000,
      riskScore: 0.1
    },
    {
      id: 'user_3',
      email: 'mike.newbie@hotmail.com',
      name: 'Mike Newbie',
      phone: '+91-9876543212',
      status: 'pending',
      role: 'user',
      subscription: {
        plan: 'Basic',
        status: 'active',
        expiresAt: new Date('2024-04-01'),
        renewsAt: new Date('2024-04-01')
      },
      kyc: {
        status: 'pending',
        documents: ['aadhar', 'pan']
      },
      createdAt: new Date('2024-03-15'),
      lastLoginAt: new Date('2024-03-18'),
      totalRevenue: 999,
      brokerConnections: 1,
      tradingVolume: 50000,
      riskScore: 0.8
    }
  ]

  const mockServices: ServiceStatus[] = [
    {
      name: 'Agent Orchestration Service',
      status: 'healthy',
      uptime: 99.8,
      responseTime: 45,
      lastCheck: new Date(),
      version: '2.1.0',
      instances: 3,
      memoryUsage: 68,
      cpuUsage: 24,
      errorRate: 0.1
    },
    {
      name: 'Broker Auth Service',
      status: 'healthy',
      uptime: 99.5,
      responseTime: 120,
      lastCheck: new Date(),
      version: '1.8.2',
      instances: 2,
      memoryUsage: 82,
      cpuUsage: 35,
      errorRate: 0.2
    },
    {
      name: 'Payment Gateway Service',
      status: 'warning',
      uptime: 98.2,
      responseTime: 280,
      lastCheck: new Date(),
      version: '1.5.1',
      instances: 4,
      memoryUsage: 91,
      cpuUsage: 78,
      errorRate: 1.2
    },
    {
      name: 'Market Data Service',
      status: 'error',
      uptime: 85.3,
      responseTime: 1200,
      lastCheck: new Date(),
      version: '3.0.0-beta',
      instances: 2,
      memoryUsage: 95,
      cpuUsage: 88,
      errorRate: 8.5
    }
  ]

  const mockMetrics: SystemMetrics = {
    totalUsers: 15420,
    activeUsers: 8965,
    totalRevenue: 28500000,
    monthlyRevenue: 4200000,
    subscriptionRevenue: 3800000,
    totalTransactions: 125643,
    successfulTransactions: 119854,
    pendingKycReviews: 47,
    systemAlerts: 12,
    apiCalls: 2856432,
    errorRate: 0.8,
    averageResponseTime: 185
  }

  const mockAuditLogs: AuditLog[] = Array.from({ length: 100 }, (_, i) => ({
    id: `audit_${i + 1}`,
    timestamp: new Date(Date.now() - Math.random() * 30 * 24 * 60 * 60 * 1000),
    userId: `user_${Math.floor(Math.random() * 1000) + 1}`,
    userEmail: `user${i}@example.com`,
    action: ['LOGIN', 'LOGOUT', 'ORDER_PLACED', 'PAYMENT_PROCESSED', 'KYC_SUBMITTED', 'PROFILE_UPDATED'][Math.floor(Math.random() * 6)],
    resource: ['USER', 'ORDER', 'PAYMENT', 'KYC', 'BROKER'][Math.floor(Math.random() * 5)],
    details: `System action performed successfully #${i + 1}`,
    ipAddress: `192.168.${Math.floor(Math.random() * 255)}.${Math.floor(Math.random() * 255)}`,
    userAgent: 'Mozilla/5.0 (compatible)',
    severity: ['low', 'medium', 'high', 'critical'][Math.floor(Math.random() * 4)] as any,
    status: ['success', 'failed', 'pending'][Math.floor(Math.random() * 3)] as any
  }))

  // Initialize Data
  useEffect(() => {
    setUsers(mockUsers)
    setServices(mockServices)
    setMetrics(mockMetrics)
    setAuditLogs(mockAuditLogs)
  }, [])

  // Filter and Search
  const filteredUsers = users.filter(user => {
    const matchesSearch = user.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
                         user.email.toLowerCase().includes(searchTerm.toLowerCase()) ||
                         user.phone.includes(searchTerm)
    const matchesFilter = filterStatus === 'all' || user.status === filterStatus
    return matchesSearch && matchesFilter
  })

  // User Actions
  const handleUserAction = async (userId: string, action: 'suspend' | 'activate' | 'delete' | 'reset_password') => {
    setIsLoading(true)
    try {
      await new Promise(resolve => setTimeout(resolve, 1000)) // Simulate API call
      
      setUsers(prev => prev.map(user => {
        if (user.id === userId) {
          switch (action) {
            case 'suspend':
              return { ...user, status: 'suspended' as const }
            case 'activate':
              return { ...user, status: 'active' as const }
            case 'delete':
              return user // Handle deletion separately
            default:
              return user
          }
        }
        return user
      }))

      if (action === 'delete') {
        setUsers(prev => prev.filter(user => user.id !== userId))
      }

      onUserUpdate?.(userId, { status: action === 'suspend' ? 'suspended' : 'active' })
    } catch (error) {
      console.error('User action failed:', error)
    } finally {
      setIsLoading(false)
    }
  }

  const handleBulkAction = async (action: string) => {
    if (selectedUsers.size === 0) return
    
    setIsLoading(true)
    try {
      await new Promise(resolve => setTimeout(resolve, 1500))
      // Bulk action logic here
      setSelectedUsers(new Set())
    } catch (error) {
      console.error('Bulk action failed:', error)
    } finally {
      setIsLoading(false)
    }
  }

  const handleKycDecision = async (userId: string, decision: 'approve' | 'reject', notes: string) => {
    setIsLoading(true)
    try {
      await new Promise(resolve => setTimeout(resolve, 1000))
      
      setUsers(prev => prev.map(user => {
        if (user.id === userId) {
          return {
            ...user,
            kyc: {
              ...user.kyc,
              status: decision === 'approve' ? 'approved' : 'rejected',
              reviewedAt: new Date(),
              reviewedBy: 'current_admin'
            }
          }
        }
        return user
      }))

      onKycReview?.(userId, decision, notes)
    } catch (error) {
      console.error('KYC review failed:', error)
    } finally {
      setIsLoading(false)
    }
  }

  // Render Functions
  const renderDashboard = () => (
    <div className="space-y-6">
      {/* Metrics Overview */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="bg-gradient-to-br from-blue-500/20 to-blue-600/20 border border-blue-500/30 rounded-xl p-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-blue-300 text-sm">Total Users</p>
              <p className="text-2xl font-bold text-white">{metrics?.totalUsers.toLocaleString()}</p>
              <p className="text-blue-400 text-xs">{metrics?.activeUsers.toLocaleString()} active</p>
            </div>
            <Users className="w-8 h-8 text-blue-400" />
          </div>
        </div>
        
        <div className="bg-gradient-to-br from-green-500/20 to-green-600/20 border border-green-500/30 rounded-xl p-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-green-300 text-sm">Total Revenue</p>
              <p className="text-2xl font-bold text-white">₹{((metrics?.totalRevenue || 0) / 10000000).toFixed(1)}Cr</p>
              <p className="text-green-400 text-xs">₹{((metrics?.monthlyRevenue || 0) / 100000).toFixed(1)}L this month</p>
            </div>
            <DollarSign className="w-8 h-8 text-green-400" />
          </div>
        </div>
        
        <div className="bg-gradient-to-br from-purple-500/20 to-purple-600/20 border border-purple-500/30 rounded-xl p-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-purple-300 text-sm">Transactions</p>
              <p className="text-2xl font-bold text-white">{((metrics?.totalTransactions || 0) / 1000).toFixed(0)}K</p>
              <p className="text-purple-400 text-xs">{((metrics?.successfulTransactions || 0) / (metrics?.totalTransactions || 1) * 100).toFixed(1)}% success</p>
            </div>
            <Activity className="w-8 h-8 text-purple-400" />
          </div>
        </div>
        
        <div className="bg-gradient-to-br from-yellow-500/20 to-yellow-600/20 border border-yellow-500/30 rounded-xl p-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-yellow-300 text-sm">System Health</p>
              <p className="text-2xl font-bold text-white">{(100 - (metrics?.errorRate || 0)).toFixed(1)}%</p>
              <p className="text-yellow-400 text-xs">{metrics?.averageResponseTime}ms avg response</p>
            </div>
            <Server className="w-8 h-8 text-yellow-400" />
          </div>
        </div>
      </div>

      {/* Service Status */}
      <div className="bg-slate-800/40 backdrop-blur-sm border border-slate-600 rounded-xl p-6">
        <h3 className="text-lg font-semibold text-white mb-4">Service Health Monitoring</h3>
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          {services.map((service) => (
            <div key={service.name} className="bg-slate-700/30 rounded-lg p-4">
              <div className="flex items-center justify-between mb-2">
                <h4 className="font-medium text-white">{service.name}</h4>
                <div className={`flex items-center space-x-1 ${
                  service.status === 'healthy' ? 'text-green-400' :
                  service.status === 'warning' ? 'text-yellow-400' :
                  service.status === 'error' ? 'text-red-400' : 'text-blue-400'
                }`}>
                  {service.status === 'healthy' ? <CheckCircle className="w-4 h-4" /> :
                   service.status === 'warning' ? <AlertTriangle className="w-4 h-4" /> :
                   service.status === 'error' ? <XCircle className="w-4 h-4" /> :
                   <Clock className="w-4 h-4" />}
                  <span className="text-sm font-medium capitalize">{service.status}</span>
                </div>
              </div>
              
              <div className="grid grid-cols-2 gap-4 text-sm">
                <div>
                  <span className="text-slate-400">Uptime:</span>
                  <span className="text-white ml-2">{service.uptime}%</span>
                </div>
                <div>
                  <span className="text-slate-400">Response:</span>
                  <span className="text-white ml-2">{service.responseTime}ms</span>
                </div>
                <div>
                  <span className="text-slate-400">Memory:</span>
                  <span className="text-white ml-2">{service.memoryUsage}%</span>
                </div>
                <div>
                  <span className="text-slate-400">CPU:</span>
                  <span className="text-white ml-2">{service.cpuUsage}%</span>
                </div>
                <div>
                  <span className="text-slate-400">Version:</span>
                  <span className="text-white ml-2">{service.version}</span>
                </div>
                <div>
                  <span className="text-slate-400">Instances:</span>
                  <span className="text-white ml-2">{service.instances}</span>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Recent Activity */}
      <div className="bg-slate-800/40 backdrop-blur-sm border border-slate-600 rounded-xl p-6">
        <h3 className="text-lg font-semibold text-white mb-4">Recent System Activity</h3>
        <div className="space-y-3">
          {auditLogs.slice(0, 5).map((log) => (
            <div key={log.id} className="flex items-center space-x-4 p-3 bg-slate-700/20 rounded-lg">
              <div className={`w-2 h-2 rounded-full ${
                log.severity === 'critical' ? 'bg-red-400' :
                log.severity === 'high' ? 'bg-orange-400' :
                log.severity === 'medium' ? 'bg-yellow-400' : 'bg-green-400'
              }`} />
              <div className="flex-1">
                <div className="flex items-center space-x-2">
                  <span className="text-white font-medium">{log.action}</span>
                  <span className="text-slate-400">by</span>
                  <span className="text-cyan-400">{log.userEmail}</span>
                </div>
                <p className="text-slate-400 text-sm">{log.details}</p>
              </div>
              <span className="text-slate-500 text-xs">{log.timestamp.toLocaleTimeString()}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  )

  const renderUserManagement = () => (
    <div className="space-y-6">
      {/* Search and Filters */}
      <div className="flex items-center justify-between">
        <div className="flex items-center space-x-4">
          <div className="relative">
            <Search className="w-4 h-4 absolute left-3 top-3 text-slate-400" />
            <input
              type="text"
              placeholder="Search users..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="pl-10 pr-4 py-2 bg-slate-800/50 border border-slate-600 rounded-lg text-white placeholder-slate-400 focus:outline-none focus:border-cyan-500"
            />
          </div>
          
          <select
            value={filterStatus}
            onChange={(e) => setFilterStatus(e.target.value)}
            className="px-4 py-2 bg-slate-800/50 border border-slate-600 rounded-lg text-white focus:outline-none focus:border-cyan-500"
          >
            <option value="all">All Status</option>
            <option value="active">Active</option>
            <option value="suspended">Suspended</option>
            <option value="pending">Pending</option>
            <option value="blocked">Blocked</option>
          </select>
        </div>

        <div className="flex items-center space-x-2">
          {selectedUsers.size > 0 && (
            <div className="flex items-center space-x-2">
              <span className="text-sm text-slate-400">{selectedUsers.size} selected</span>
              <button
                onClick={() => handleBulkAction('suspend')}
                className="px-3 py-1 bg-orange-500/20 text-orange-400 border border-orange-500/30 rounded text-sm hover:bg-orange-500/30 transition-colors"
              >
                Suspend
              </button>
              <button
                onClick={() => handleBulkAction('activate')}
                className="px-3 py-1 bg-green-500/20 text-green-400 border border-green-500/30 rounded text-sm hover:bg-green-500/30 transition-colors"
              >
                Activate
              </button>
            </div>
          )}
          
          <button className="flex items-center space-x-2 px-4 py-2 bg-cyan-500/20 text-cyan-400 border border-cyan-500/30 rounded-lg hover:bg-cyan-500/30 transition-colors">
            <Download className="w-4 h-4" />
            <span>Export</span>
          </button>
        </div>
      </div>

      {/* Users Table */}
      <div className="bg-slate-800/40 backdrop-blur-sm border border-slate-600 rounded-xl overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left">
            <thead className="bg-slate-700/50 border-b border-slate-600">
              <tr>
                <th className="p-4">
                  <input
                    type="checkbox"
                    onChange={(e) => {
                      if (e.target.checked) {
                        setSelectedUsers(new Set(filteredUsers.map(user => user.id)))
                      } else {
                        setSelectedUsers(new Set())
                      }
                    }}
                    className="rounded border-slate-600 bg-slate-700 text-cyan-500 focus:ring-cyan-500"
                  />
                </th>
                <th className="p-4 text-slate-300 font-medium">User</th>
                <th className="p-4 text-slate-300 font-medium">Status</th>
                <th className="p-4 text-slate-300 font-medium">Subscription</th>
                <th className="p-4 text-slate-300 font-medium">KYC</th>
                <th className="p-4 text-slate-300 font-medium">Revenue</th>
                <th className="p-4 text-slate-300 font-medium">Risk Score</th>
                <th className="p-4 text-slate-300 font-medium">Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredUsers.map((user) => (
                <tr key={user.id} className="border-b border-slate-700 hover:bg-slate-700/20">
                  <td className="p-4">
                    <input
                      type="checkbox"
                      checked={selectedUsers.has(user.id)}
                      onChange={(e) => {
                        const newSelected = new Set(selectedUsers)
                        if (e.target.checked) {
                          newSelected.add(user.id)
                        } else {
                          newSelected.delete(user.id)
                        }
                        setSelectedUsers(newSelected)
                      }}
                      className="rounded border-slate-600 bg-slate-700 text-cyan-500 focus:ring-cyan-500"
                    />
                  </td>
                  <td className="p-4">
                    <div className="flex items-center space-x-3">
                      <div className={`w-10 h-10 rounded-full flex items-center justify-center ${
                        user.role === 'admin' ? 'bg-red-500/20' :
                        user.role === 'enterprise' ? 'bg-purple-500/20' :
                        user.role === 'premium' ? 'bg-blue-500/20' : 'bg-slate-500/20'
                      }`}>
                        {user.role === 'admin' && <Shield className="w-5 h-5 text-red-400" />}
                        {user.role === 'enterprise' && <Crown className="w-5 h-5 text-purple-400" />}
                        {user.role === 'premium' && <Star className="w-5 h-5 text-blue-400" />}
                        {user.role === 'user' && <Users className="w-5 h-5 text-slate-400" />}
                      </div>
                      <div>
                        <div className="text-white font-medium">{user.name}</div>
                        <div className="text-slate-400 text-sm">{user.email}</div>
                        <div className="text-slate-500 text-xs">{user.phone}</div>
                      </div>
                    </div>
                  </td>
                  <td className="p-4">
                    <span className={`px-2 py-1 rounded-full text-xs font-medium ${
                      user.status === 'active' ? 'bg-green-500/20 text-green-400' :
                      user.status === 'suspended' ? 'bg-orange-500/20 text-orange-400' :
                      user.status === 'pending' ? 'bg-yellow-500/20 text-yellow-400' :
                      'bg-red-500/20 text-red-400'
                    }`}>
                      {user.status.toUpperCase()}
                    </span>
                  </td>
                  <td className="p-4">
                    <div className="text-white text-sm">{user.subscription.plan}</div>
                    <div className={`text-xs ${
                      user.subscription.status === 'active' ? 'text-green-400' :
                      user.subscription.status === 'expired' ? 'text-red-400' : 'text-orange-400'
                    }`}>
                      {user.subscription.status} until {user.subscription.expiresAt.toLocaleDateString()}
                    </div>
                  </td>
                  <td className="p-4">
                    <span className={`px-2 py-1 rounded-full text-xs font-medium ${
                      user.kyc.status === 'approved' ? 'bg-green-500/20 text-green-400' :
                      user.kyc.status === 'rejected' ? 'bg-red-500/20 text-red-400' :
                      user.kyc.status === 'pending' ? 'bg-yellow-500/20 text-yellow-400' :
                      'bg-slate-500/20 text-slate-400'
                    }`}>
                      {user.kyc.status.toUpperCase()}
                    </span>
                  </td>
                  <td className="p-4">
                    <div className="text-white font-medium">₹{user.totalRevenue.toLocaleString()}</div>
                    <div className="text-slate-400 text-xs">{user.brokerConnections} brokers</div>
                  </td>
                  <td className="p-4">
                    <div className={`text-sm font-medium ${
                      user.riskScore < 0.3 ? 'text-green-400' :
                      user.riskScore < 0.7 ? 'text-yellow-400' : 'text-red-400'
                    }`}>
                      {(user.riskScore * 100).toFixed(0)}%
                    </div>
                  </td>
                  <td className="p-4">
                    <div className="flex items-center space-x-2">
                      <button
                        onClick={() => {
                          setSelectedUser(user)
                          setShowUserModal(true)
                        }}
                        className="p-1 text-slate-400 hover:text-white transition-colors"
                      >
                        <Eye className="w-4 h-4" />
                      </button>
                      <button
                        onClick={() => handleUserAction(user.id, user.status === 'active' ? 'suspend' : 'activate')}
                        className="p-1 text-slate-400 hover:text-yellow-400 transition-colors"
                      >
                        {user.status === 'active' ? <Lock className="w-4 h-4" /> : <Unlock className="w-4 h-4" />}
                      </button>
                      <button
                        onClick={() => handleUserAction(user.id, 'delete')}
                        className="p-1 text-slate-400 hover:text-red-400 transition-colors"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )

  const renderAuditLogs = () => (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h3 className="text-xl font-bold text-white">Audit Logs</h3>
        <button className="flex items-center space-x-2 px-4 py-2 bg-cyan-500/20 text-cyan-400 border border-cyan-500/30 rounded-lg hover:bg-cyan-500/30 transition-colors">
          <Download className="w-4 h-4" />
          <span>Export Logs</span>
        </button>
      </div>

      <div className="bg-slate-800/40 backdrop-blur-sm border border-slate-600 rounded-xl overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left">
            <thead className="bg-slate-700/50 border-b border-slate-600">
              <tr>
                <th className="p-4 text-slate-300 font-medium">Timestamp</th>
                <th className="p-4 text-slate-300 font-medium">User</th>
                <th className="p-4 text-slate-300 font-medium">Action</th>
                <th className="p-4 text-slate-300 font-medium">Resource</th>
                <th className="p-4 text-slate-300 font-medium">Status</th>
                <th className="p-4 text-slate-300 font-medium">Severity</th>
                <th className="p-4 text-slate-300 font-medium">IP Address</th>
              </tr>
            </thead>
            <tbody>
              {auditLogs.slice(0, 20).map((log) => (
                <tr key={log.id} className="border-b border-slate-700 hover:bg-slate-700/20">
                  <td className="p-4 text-slate-300 font-mono text-sm">
                    {log.timestamp.toLocaleString()}
                  </td>
                  <td className="p-4 text-cyan-400">{log.userEmail}</td>
                  <td className="p-4 text-white font-medium">{log.action}</td>
                  <td className="p-4 text-slate-300">{log.resource}</td>
                  <td className="p-4">
                    <span className={`px-2 py-1 rounded-full text-xs font-medium ${
                      log.status === 'success' ? 'bg-green-500/20 text-green-400' :
                      log.status === 'failed' ? 'bg-red-500/20 text-red-400' :
                      'bg-yellow-500/20 text-yellow-400'
                    }`}>
                      {log.status.toUpperCase()}
                    </span>
                  </td>
                  <td className="p-4">
                    <span className={`px-2 py-1 rounded-full text-xs font-medium ${
                      log.severity === 'critical' ? 'bg-red-500/20 text-red-400' :
                      log.severity === 'high' ? 'bg-orange-500/20 text-orange-400' :
                      log.severity === 'medium' ? 'bg-yellow-500/20 text-yellow-400' :
                      'bg-blue-500/20 text-blue-400'
                    }`}>
                      {log.severity.toUpperCase()}
                    </span>
                  </td>
                  <td className="p-4 text-slate-400 font-mono text-sm">{log.ipAddress}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )

  const renderSystemHealth = () => (
    <div className="space-y-6">
      {/* Service Status Grid */}
      <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
        {[
          { name: 'Trading Service', status: 'healthy', uptime: '99.98%', response: '45ms', load: '23%' },
          { name: 'Market Data Service', status: 'healthy', uptime: '99.95%', response: '12ms', load: '18%' },
          { name: 'Authentication Service', status: 'healthy', uptime: '100%', response: '8ms', load: '12%' },
          { name: 'Payment Gateway', status: 'degraded', uptime: '99.2%', response: '89ms', load: '67%' },
          { name: 'Broker API Gateway', status: 'healthy', uptime: '99.8%', response: '23ms', load: '34%' },
          { name: 'Database Cluster', status: 'healthy', uptime: '99.99%', response: '5ms', load: '28%' }
        ].map((service) => (
          <div key={service.name} className="bg-slate-800/40 backdrop-blur-sm border border-slate-600 rounded-xl p-6">
            <div className="flex items-center justify-between mb-4">
              <h4 className="text-white font-semibold">{service.name}</h4>
              <div className={`w-3 h-3 rounded-full ${
                service.status === 'healthy' ? 'bg-green-400' : 
                service.status === 'degraded' ? 'bg-yellow-400' : 'bg-red-400'
              }`} />
            </div>
            <div className="space-y-3 text-sm">
              <div className="flex justify-between">
                <span className="text-slate-400">Uptime</span>
                <span className="text-white font-medium">{service.uptime}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">Response Time</span>
                <span className="text-white font-medium">{service.response}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">CPU Load</span>
                <span className="text-white font-medium">{service.load}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">Status</span>
                <span className={`capitalize font-medium ${
                  service.status === 'healthy' ? 'text-green-400' : 
                  service.status === 'degraded' ? 'text-yellow-400' : 'text-red-400'
                }`}>
                  {service.status}
                </span>
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* System Metrics */}
      <div className="bg-slate-800/40 backdrop-blur-sm border border-slate-600 rounded-xl p-6">
        <h3 className="text-xl font-bold text-white mb-6">System Performance Metrics</h3>
        <div className="grid gap-6 md:grid-cols-4">
          <div className="text-center">
            <div className="text-3xl font-bold text-green-400 mb-2">99.97%</div>
            <div className="text-slate-400">Overall Uptime</div>
          </div>
          <div className="text-center">
            <div className="text-3xl font-bold text-blue-400 mb-2">28ms</div>
            <div className="text-slate-400">Avg Response</div>
          </div>
          <div className="text-center">
            <div className="text-3xl font-bold text-purple-400 mb-2">2.4M</div>
            <div className="text-slate-400">Requests/Day</div>
          </div>
          <div className="text-center">
            <div className="text-3xl font-bold text-cyan-400 mb-2">0.002%</div>
            <div className="text-slate-400">Error Rate</div>
          </div>
        </div>
      </div>
    </div>
  )

  const renderPaymentManagement = () => (
    <div className="space-y-6">
      <div className="grid gap-6 md:grid-cols-3">
        <div className="bg-slate-800/40 backdrop-blur-sm border border-slate-600 rounded-xl p-6">
          <div className="text-2xl font-bold text-green-400 mb-2">₹48.5L</div>
          <div className="text-slate-400">Total Revenue (30d)</div>
        </div>
        <div className="bg-slate-800/40 backdrop-blur-sm border border-slate-600 rounded-xl p-6">
          <div className="text-2xl font-bold text-blue-400 mb-2">94.7%</div>
          <div className="text-slate-400">Success Rate</div>
        </div>
        <div className="bg-slate-800/40 backdrop-blur-sm border border-slate-600 rounded-xl p-6">
          <div className="text-2xl font-bold text-purple-400 mb-2">15,420</div>
          <div className="text-slate-400">Transactions</div>
        </div>
      </div>
      
      <div className="bg-slate-800/40 backdrop-blur-sm border border-slate-600 rounded-xl p-6">
        <h4 className="text-white font-semibold mb-4">Recent Failed Transactions</h4>
        <div className="space-y-3">
          {[
            { id: 'TXN-2024-001', user: 'user1@email.com', amount: '₹2,500', reason: 'Insufficient balance', time: '2 hours ago' },
            { id: 'TXN-2024-002', user: 'user2@email.com', amount: '₹1,200', reason: 'Card declined', time: '4 hours ago' },
            { id: 'TXN-2024-003', user: 'user3@email.com', amount: '₹850', reason: 'Network timeout', time: '6 hours ago' }
          ].map((txn) => (
            <div key={txn.id} className="flex items-center justify-between p-4 bg-slate-700/30 rounded-lg">
              <div>
                <div className="text-white font-medium">{txn.id}</div>
                <div className="text-slate-400 text-sm">{txn.user}</div>
                <div className="text-red-400 text-sm">{txn.reason}</div>
              </div>
              <div className="text-right">
                <div className="text-white font-semibold">{txn.amount}</div>
                <div className="text-slate-400 text-sm">{txn.time}</div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  )

  const renderKYCReviews = () => (
    <div className="space-y-6">
      <div className="grid gap-4 md:grid-cols-4">
        <div className="bg-slate-800/40 backdrop-blur-sm border border-slate-600 rounded-xl p-6 text-center">
          <div className="text-2xl font-bold text-yellow-400 mb-2">47</div>
          <div className="text-slate-400">Pending Review</div>
        </div>
        <div className="bg-slate-800/40 backdrop-blur-sm border border-slate-600 rounded-xl p-6 text-center">
          <div className="text-2xl font-bold text-green-400 mb-2">1,847</div>
          <div className="text-slate-400">Approved</div>
        </div>
        <div className="bg-slate-800/40 backdrop-blur-sm border border-slate-600 rounded-xl p-6 text-center">
          <div className="text-2xl font-bold text-red-400 mb-2">23</div>
          <div className="text-slate-400">Rejected</div>
        </div>
        <div className="bg-slate-800/40 backdrop-blur-sm border border-slate-600 rounded-xl p-6 text-center">
          <div className="text-2xl font-bold text-blue-400 mb-2">2.4h</div>
          <div className="text-slate-400">Avg Review Time</div>
        </div>
      </div>
      
      <div className="bg-slate-800/40 backdrop-blur-sm border border-slate-600 rounded-xl p-6">
        <h4 className="text-white font-semibold mb-4">Pending KYC Reviews</h4>
        <div className="space-y-3">
          {[
            { id: 'KYC-001', user: 'Rajesh Kumar', email: 'rajesh.kumar@email.com', documents: 'PAN, Aadhaar', submitted: '2 hours ago', risk: 'low' },
            { id: 'KYC-002', user: 'Priya Sharma', email: 'priya.sharma@email.com', documents: 'PAN, Driving License', submitted: '4 hours ago', risk: 'medium' },
            { id: 'KYC-003', user: 'Arjun Singh', email: 'arjun.singh@email.com', documents: 'PAN, Passport', submitted: '6 hours ago', risk: 'low' }
          ].map((kyc) => (
            <div key={kyc.id} className="flex items-center justify-between p-4 bg-slate-700/30 rounded-lg">
              <div>
                <div className="text-white font-medium">{kyc.user}</div>
                <div className="text-slate-400 text-sm">{kyc.email}</div>
                <div className="text-cyan-400 text-sm">{kyc.documents}</div>
              </div>
              <div className="text-right">
                <div className={`text-sm font-medium mb-1 ${
                  kyc.risk === 'low' ? 'text-green-400' : kyc.risk === 'medium' ? 'text-yellow-400' : 'text-red-400'
                }`}>
                  {kyc.risk.toUpperCase()} RISK
                </div>
                <div className="text-slate-400 text-sm">{kyc.submitted}</div>
                <div className="flex space-x-2 mt-2">
                  <button className="px-3 py-1 bg-green-500/20 text-green-400 border border-green-500/30 rounded text-sm hover:bg-green-500/30 transition-colors">
                    Approve
                  </button>
                  <button className="px-3 py-1 bg-red-500/20 text-red-400 border border-red-500/30 rounded text-sm hover:bg-red-500/30 transition-colors">
                    Reject
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  )

  const renderSystemSettings = () => (
    <div className="space-y-6">
      <div className="bg-slate-800/40 backdrop-blur-sm border border-slate-600 rounded-xl p-6">
        <h4 className="text-white font-semibold mb-4">System Configuration</h4>
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <div>
              <div className="text-white font-medium">Maintenance Mode</div>
              <div className="text-slate-400 text-sm">Enable system maintenance mode</div>
            </div>
            <label className="relative inline-flex items-center cursor-pointer">
              <input type="checkbox" className="sr-only peer" />
              <div className="w-11 h-6 bg-slate-700 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-cyan-600"></div>
            </label>
          </div>
          <div className="flex items-center justify-between">
            <div>
              <div className="text-white font-medium">Auto Backup</div>
              <div className="text-slate-400 text-sm">Automated daily backups</div>
            </div>
            <label className="relative inline-flex items-center cursor-pointer">
              <input type="checkbox" className="sr-only peer" defaultChecked />
              <div className="w-11 h-6 bg-slate-700 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-cyan-600"></div>
            </label>
          </div>
          <div className="flex items-center justify-between">
            <div>
              <div className="text-white font-medium">Rate Limiting</div>
              <div className="text-slate-400 text-sm">API rate limiting protection</div>
            </div>
            <label className="relative inline-flex items-center cursor-pointer">
              <input type="checkbox" className="sr-only peer" defaultChecked />
              <div className="w-11 h-6 bg-slate-700 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-cyan-600"></div>
            </label>
          </div>
        </div>
      </div>
      
      <div className="bg-slate-800/40 backdrop-blur-sm border border-slate-600 rounded-xl p-6">
        <h4 className="text-white font-semibold mb-4">Security Settings</h4>
        <div className="grid gap-4 md:grid-cols-2">
          <div>
            <label className="text-white font-medium block mb-2">Session Timeout (minutes)</label>
            <input type="number" defaultValue="30" className="w-full bg-slate-700 text-white px-3 py-2 rounded-lg border border-slate-600 focus:border-cyan-500 focus:outline-none" />
          </div>
          <div>
            <label className="text-white font-medium block mb-2">Max Login Attempts</label>
            <input type="number" defaultValue="5" className="w-full bg-slate-700 text-white px-3 py-2 rounded-lg border border-slate-600 focus:border-cyan-500 focus:outline-none" />
          </div>
          <div>
            <label className="text-white font-medium block mb-2">Password Expiry (days)</label>
            <input type="number" defaultValue="90" className="w-full bg-slate-700 text-white px-3 py-2 rounded-lg border border-slate-600 focus:border-cyan-500 focus:outline-none" />
          </div>
          <div>
            <label className="text-white font-medium block mb-2">Two-Factor Authentication</label>
            <select className="w-full bg-slate-700 text-white px-3 py-2 rounded-lg border border-slate-600 focus:border-cyan-500 focus:outline-none">
              <option>Required for all users</option>
              <option>Required for admins only</option>
              <option>Optional</option>
              <option>Disabled</option>
            </select>
          </div>
        </div>
      </div>
    </div>
  )

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-purple-900 to-slate-900">
      {/* Header */}
      <div className="border-b border-slate-700 bg-slate-800/50 backdrop-blur-sm">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between h-16">
            <div className="flex items-center space-x-3">
              <div className="w-10 h-10 bg-gradient-to-r from-cyan-400 to-purple-400 rounded-lg flex items-center justify-center">
                <Shield className="w-6 h-6 text-black" />
              </div>
              <div>
                <h1 className="text-xl font-bold text-white">Admin Dashboard</h1>
                <p className="text-sm text-slate-400">System management and monitoring</p>
              </div>
            </div>

            <div className="flex items-center space-x-4">
              <div className="flex items-center space-x-2 bg-green-500/20 border border-green-500/30 rounded-lg px-3 py-1">
                <div className="w-2 h-2 bg-green-400 rounded-full animate-pulse" />
                <span className="text-green-400 text-sm font-medium">System Healthy</span>
              </div>
              <button className="p-2 text-slate-400 hover:text-white transition-colors">
                <Bell className="w-5 h-5" />
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Navigation Tabs */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
        <div className="flex items-center space-x-1 bg-slate-800/40 backdrop-blur-sm border border-slate-600 rounded-xl p-1 mb-6">
          {[
            { id: 'dashboard', label: 'Dashboard', icon: BarChart3 },
            { id: 'users', label: 'User Management', icon: Users },
            { id: 'system', label: 'System Health', icon: Server },
            { id: 'payments', label: 'Payments', icon: CreditCard },
            { id: 'kyc', label: 'KYC Reviews', icon: UserCheck },
            { id: 'audit', label: 'Audit Logs', icon: FileText },
            { id: 'settings', label: 'Settings', icon: Settings }
          ].map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id as any)}
              className={`flex items-center space-x-2 px-4 py-2 rounded-lg font-medium transition-all ${
                activeTab === tab.id
                  ? 'bg-gradient-to-r from-cyan-500 to-purple-500 text-black'
                  : 'text-slate-400 hover:text-white hover:bg-slate-700/50'
              }`}
            >
              <tab.icon className="w-4 h-4" />
              <span className="hidden sm:inline">{tab.label}</span>
            </button>
          ))}
        </div>

        {/* Tab Content */}
        <div className="space-y-6">
          {activeTab === 'dashboard' && renderDashboard()}
          {activeTab === 'users' && renderUserManagement()}
          {activeTab === 'audit' && renderAuditLogs()}
          {activeTab === 'system' && renderSystemHealth()}
          {activeTab === 'payments' && renderPaymentManagement()}
          {activeTab === 'kyc' && renderKYCReviews()}
          {activeTab === 'settings' && renderSystemSettings()}
        </div>
      </div>

      {/* Loading Overlay */}
      {isLoading && (
        <div className="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center z-50">
          <div className="bg-slate-800 border border-cyan-500/30 rounded-xl p-6 flex items-center space-x-4">
            <div className="animate-spin rounded-full h-8 w-8 border-2 border-cyan-400 border-t-transparent" />
            <span className="text-white font-medium">Processing request...</span>
          </div>
        </div>
      )}
    </div>
  )
}