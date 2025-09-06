import React, { useState, useEffect, useCallback } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import {
  Bell, Mail, MessageSquare, Smartphone, Send, Users, Eye, Settings,
  BarChart3, TrendingUp, Clock, CheckCircle, XCircle, AlertTriangle,
  Search, Filter, Download, Edit3, Trash2, Plus, Play, Pause, Copy,
  Monitor, Globe, Target, Zap, Award, Calendar, Hash, User, Building,
  Percent, ArrowUp, ArrowDown, RefreshCw, Save, X, Check, Info,
  FileText, Image, Link, Code, Palette, Type, Layout, Sparkles
} from 'lucide-react'

// Types
interface NotificationTemplate {
  id: string
  name: string
  type: 'email' | 'sms' | 'push' | 'in_app'
  category: 'marketing' | 'transactional' | 'system' | 'trading' | 'security'
  subject: string
  content: string
  htmlContent?: string
  isActive: boolean
  createdAt: Date
  updatedAt: Date
  createdBy: string
  variables: string[]
  metrics: {
    sent: number
    delivered: number
    opened: number
    clicked: number
    bounced: number
    complained: number
  }
}

interface NotificationCampaign {
  id: string
  name: string
  templateId: string
  templateName: string
  type: 'email' | 'sms' | 'push' | 'in_app'
  status: 'draft' | 'scheduled' | 'running' | 'completed' | 'cancelled'
  audience: {
    totalRecipients: number
    segmentName: string
    filters: Record<string, any>
  }
  schedule: {
    sendAt?: Date
    timezone: string
    recurring?: {
      frequency: 'daily' | 'weekly' | 'monthly'
      endDate?: Date
    }
  }
  metrics: {
    sent: number
    delivered: number
    opened: number
    clicked: number
    bounced: number
    unsubscribed: number
    deliveryRate: number
    openRate: number
    clickRate: number
  }
  createdAt: Date
  createdBy: string
}

interface NotificationAnalytics {
  totalSent: number
  totalDelivered: number
  totalOpened: number
  totalClicked: number
  averageDeliveryRate: number
  averageOpenRate: number
  averageClickRate: number
  topPerformingTemplates: Array<{
    templateName: string
    openRate: number
    clickRate: number
    sent: number
  }>
  performanceByType: Record<string, {
    sent: number
    delivered: number
    openRate: number
    clickRate: number
  }>
  recentActivity: Array<{
    id: string
    type: string
    action: string
    timestamp: Date
    details: string
  }>
}

interface UserPreference {
  id: string
  userId: string
  userEmail: string
  userName: string
  preferences: {
    email: {
      marketing: boolean
      transactional: boolean
      system: boolean
      trading: boolean
      security: boolean
    }
    sms: {
      marketing: boolean
      transactional: boolean
      system: boolean
      trading: boolean
      security: boolean
    }
    push: {
      marketing: boolean
      transactional: boolean
      system: boolean
      trading: boolean
      security: boolean
    }
    inApp: {
      marketing: boolean
      transactional: boolean
      system: boolean
      trading: boolean
      security: boolean
    }
  }
  frequency: 'immediate' | 'hourly' | 'daily' | 'weekly'
  timezone: string
  language: string
  updatedAt: Date
}

interface NotificationManagementProps {
  initialTab?: 'dashboard' | 'templates' | 'campaigns' | 'analytics' | 'preferences' | 'settings'
}

export const NotificationManagement: React.FC<NotificationManagementProps> = ({ initialTab = 'dashboard' }) => {
  const [activeTab, setActiveTab] = useState<'dashboard' | 'templates' | 'campaigns' | 'analytics' | 'preferences' | 'settings'>(initialTab)
  const [searchTerm, setSearchTerm] = useState('')
  const [selectedFilters, setSelectedFilters] = useState<Record<string, any>>({})
  const [loading, setLoading] = useState(false)
  const [notification, setNotification] = useState<{ type: 'success' | 'error' | 'warning' | 'info', message: string } | null>(null)

  // Mock data states
  const [templates, setTemplates] = useState<NotificationTemplate[]>([])
  const [campaigns, setCampaigns] = useState<NotificationCampaign[]>([])
  const [analytics, setAnalytics] = useState<NotificationAnalytics | null>(null)
  const [userPreferences, setUserPreferences] = useState<UserPreference[]>([])

  // Load mock data
  useEffect(() => {
    loadMockData()
  }, [])

  const loadMockData = useCallback(() => {
    // Mock Templates
    const mockTemplates: NotificationTemplate[] = [
      {
        id: 'template-1',
        name: 'Welcome Email',
        type: 'email',
        category: 'transactional',
        subject: 'Welcome to TradeMaster - Your Trading Journey Begins!',
        content: 'Welcome {{firstName}}! We\'re excited to have you join TradeMaster...',
        htmlContent: '<h1>Welcome {{firstName}}!</h1><p>We\'re excited to have you join TradeMaster...</p>',
        isActive: true,
        createdAt: new Date('2024-01-15'),
        updatedAt: new Date('2024-01-20'),
        createdBy: 'admin@trademaster.com',
        variables: ['firstName', 'lastName', 'email'],
        metrics: { sent: 2847, delivered: 2821, opened: 1963, clicked: 847, bounced: 26, complained: 3 }
      },
      {
        id: 'template-2',
        name: 'Trading Alert',
        type: 'push',
        category: 'trading',
        subject: 'Price Alert Triggered',
        content: 'Your price alert for {{symbol}} has been triggered at ₹{{price}}',
        isActive: true,
        createdAt: new Date('2024-01-10'),
        updatedAt: new Date('2024-01-25'),
        createdBy: 'trader@trademaster.com',
        variables: ['symbol', 'price', 'change', 'percentage'],
        metrics: { sent: 15642, delivered: 15580, opened: 12456, clicked: 8942, bounced: 62, complained: 5 }
      },
      {
        id: 'template-3',
        name: 'Monthly Newsletter',
        type: 'email',
        category: 'marketing',
        subject: 'TradeMaster Monthly Insights - {{month}} {{year}}',
        content: 'Dear {{firstName}}, here are your monthly trading insights...',
        htmlContent: '<h1>Monthly Insights</h1><p>Dear {{firstName}}, here are your monthly trading insights...</p>',
        isActive: true,
        createdAt: new Date('2024-01-05'),
        updatedAt: new Date('2024-01-30'),
        createdBy: 'marketing@trademaster.com',
        variables: ['firstName', 'month', 'year', 'performance'],
        metrics: { sent: 8451, delivered: 8327, opened: 3764, clicked: 1247, bounced: 124, complained: 18 }
      },
      {
        id: 'template-4',
        name: 'Security Alert SMS',
        type: 'sms',
        category: 'security',
        subject: 'Security Alert',
        content: 'TradeMaster Alert: Login detected from {{location}} at {{time}}. If this wasn\'t you, secure your account immediately.',
        isActive: true,
        createdAt: new Date('2024-01-12'),
        updatedAt: new Date('2024-01-28'),
        createdBy: 'security@trademaster.com',
        variables: ['location', 'time', 'ipAddress'],
        metrics: { sent: 342, delivered: 341, opened: 341, clicked: 89, bounced: 1, complained: 0 }
      }
    ]

    // Mock Campaigns
    const mockCampaigns: NotificationCampaign[] = [
      {
        id: 'campaign-1',
        name: 'Q1 Trading Performance Review',
        templateId: 'template-3',
        templateName: 'Monthly Newsletter',
        type: 'email',
        status: 'completed',
        audience: {
          totalRecipients: 8451,
          segmentName: 'Active Traders',
          filters: { subscription: 'pro', lastLogin: '30_days', tradingActivity: 'active' }
        },
        schedule: {
          sendAt: new Date('2024-01-31T09:00:00Z'),
          timezone: 'Asia/Kolkata'
        },
        metrics: {
          sent: 8451,
          delivered: 8327,
          opened: 3764,
          clicked: 1247,
          bounced: 124,
          unsubscribed: 18,
          deliveryRate: 98.5,
          openRate: 45.2,
          clickRate: 33.1
        },
        createdAt: new Date('2024-01-25'),
        createdBy: 'marketing@trademaster.com'
      },
      {
        id: 'campaign-2',
        name: 'Weekend Market Analysis Push',
        templateId: 'template-2',
        templateName: 'Trading Alert',
        type: 'push',
        status: 'scheduled',
        audience: {
          totalRecipients: 12847,
          segmentName: 'All Active Users',
          filters: { pushEnabled: true, subscription: 'any' }
        },
        schedule: {
          sendAt: new Date(Date.now() + 2 * 24 * 60 * 60 * 1000),
          timezone: 'Asia/Kolkata',
          recurring: {
            frequency: 'weekly',
            endDate: new Date(Date.now() + 90 * 24 * 60 * 60 * 1000)
          }
        },
        metrics: {
          sent: 0,
          delivered: 0,
          opened: 0,
          clicked: 0,
          bounced: 0,
          unsubscribed: 0,
          deliveryRate: 0,
          openRate: 0,
          clickRate: 0
        },
        createdAt: new Date(),
        createdBy: 'system@trademaster.com'
      }
    ]

    // Mock Analytics
    const mockAnalytics: NotificationAnalytics = {
      totalSent: 27282,
      totalDelivered: 27069,
      totalOpened: 18524,
      totalClicked: 11125,
      averageDeliveryRate: 99.2,
      averageOpenRate: 68.4,
      averageClickRate: 60.1,
      topPerformingTemplates: [
        { templateName: 'Trading Alert', openRate: 79.9, clickRate: 71.8, sent: 15642 },
        { templateName: 'Welcome Email', openRate: 69.6, clickRate: 43.2, sent: 2847 },
        { templateName: 'Security Alert SMS', openRate: 100.0, clickRate: 26.1, sent: 342 },
        { templateName: 'Monthly Newsletter', openRate: 45.2, clickRate: 33.1, sent: 8451 }
      ],
      performanceByType: {
        email: { sent: 11298, delivered: 11148, openRate: 54.2, clickRate: 37.8 },
        push: { sent: 15642, delivered: 15580, openRate: 79.9, clickRate: 71.8 },
        sms: { sent: 342, delivered: 341, openRate: 100.0, clickRate: 26.1 }
      },
      recentActivity: [
        {
          id: 'activity-1',
          type: 'campaign',
          action: 'Campaign "Q1 Performance Review" completed',
          timestamp: new Date(Date.now() - 2 * 60 * 60 * 1000),
          details: '8,451 emails sent with 45.2% open rate'
        },
        {
          id: 'activity-2',
          type: 'template',
          action: 'Template "Trading Alert" updated',
          timestamp: new Date(Date.now() - 4 * 60 * 60 * 1000),
          details: 'Updated push notification content'
        },
        {
          id: 'activity-3',
          type: 'system',
          action: 'Bulk SMS delivery completed',
          timestamp: new Date(Date.now() - 6 * 60 * 60 * 1000),
          details: '342 SMS messages delivered successfully'
        }
      ]
    }

    // Mock User Preferences
    const mockPreferences: UserPreference[] = Array.from({ length: 50 }, (_, i) => ({
      id: `pref-${i + 1}`,
      userId: `user-${i + 1}`,
      userEmail: `user${i + 1}@example.com`,
      userName: `User ${i + 1}`,
      preferences: {
        email: {
          marketing: Math.random() > 0.3,
          transactional: true,
          system: true,
          trading: Math.random() > 0.2,
          security: true
        },
        sms: {
          marketing: Math.random() > 0.7,
          transactional: Math.random() > 0.4,
          system: Math.random() > 0.5,
          trading: Math.random() > 0.3,
          security: true
        },
        push: {
          marketing: Math.random() > 0.4,
          transactional: Math.random() > 0.2,
          system: Math.random() > 0.3,
          trading: Math.random() > 0.1,
          security: true
        },
        inApp: {
          marketing: Math.random() > 0.2,
          transactional: true,
          system: true,
          trading: true,
          security: true
        }
      },
      frequency: ['immediate', 'hourly', 'daily', 'weekly'][Math.floor(Math.random() * 4)] as any,
      timezone: 'Asia/Kolkata',
      language: 'en',
      updatedAt: new Date(Date.now() - Math.floor(Math.random() * 30) * 24 * 60 * 60 * 1000)
    }))

    setTemplates(mockTemplates)
    setCampaigns(mockCampaigns)
    setAnalytics(mockAnalytics)
    setUserPreferences(mockPreferences)
  }, [])

  // Show notification
  const showNotification = (type: 'success' | 'error' | 'warning' | 'info', message: string) => {
    setNotification({ type, message })
    setTimeout(() => setNotification(null), 5000)
  }

  // Notification Management Functions
  const sendTestNotification = async (templateId: string) => {
    setLoading(true)
    try {
      await new Promise(resolve => setTimeout(resolve, 2000))
      showNotification('success', 'Test notification sent successfully')
    } catch (error) {
      showNotification('error', 'Failed to send test notification')
    } finally {
      setLoading(false)
    }
  }

  const pauseCampaign = async (campaignId: string) => {
    setLoading(true)
    try {
      await new Promise(resolve => setTimeout(resolve, 1500))
      setCampaigns(prev => prev.map(campaign => 
        campaign.id === campaignId 
          ? { ...campaign, status: campaign.status === 'running' ? 'cancelled' : 'scheduled' }
          : campaign
      ))
      showNotification('success', 'Campaign status updated successfully')
    } catch (error) {
      showNotification('error', 'Failed to update campaign status')
    } finally {
      setLoading(false)
    }
  }

  const duplicateTemplate = async (templateId: string) => {
    setLoading(true)
    try {
      await new Promise(resolve => setTimeout(resolve, 1000))
      const originalTemplate = templates.find(t => t.id === templateId)
      if (originalTemplate) {
        const newTemplate: NotificationTemplate = {
          ...originalTemplate,
          id: `template-${templates.length + 1}`,
          name: `${originalTemplate.name} (Copy)`,
          createdAt: new Date(),
          updatedAt: new Date(),
          metrics: { sent: 0, delivered: 0, opened: 0, clicked: 0, bounced: 0, complained: 0 }
        }
        setTemplates(prev => [...prev, newTemplate])
      }
      showNotification('success', 'Template duplicated successfully')
    } catch (error) {
      showNotification('error', 'Failed to duplicate template')
    } finally {
      setLoading(false)
    }
  }

  // Filter functions
  const filteredTemplates = templates.filter(template => {
    const matchesSearch = searchTerm === '' || 
      template.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      template.subject.toLowerCase().includes(searchTerm.toLowerCase()) ||
      template.category.toLowerCase().includes(searchTerm.toLowerCase())
    
    const matchesFilters = Object.entries(selectedFilters).every(([key, value]) => {
      if (!value) return true
      return (template as any)[key] === value
    })
    
    return matchesSearch && matchesFilters
  })

  const filteredCampaigns = campaigns.filter(campaign => {
    const matchesSearch = searchTerm === '' ||
      campaign.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      campaign.templateName.toLowerCase().includes(searchTerm.toLowerCase()) ||
      campaign.audience.segmentName.toLowerCase().includes(searchTerm.toLowerCase())
    
    return matchesSearch
  })

  const filteredPreferences = userPreferences.filter(pref => {
    const matchesSearch = searchTerm === '' ||
      pref.userEmail.toLowerCase().includes(searchTerm.toLowerCase()) ||
      pref.userName.toLowerCase().includes(searchTerm.toLowerCase())
    
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
          Notification & Communication Management
        </h1>
        <p className="text-slate-400 text-lg">
          Complete communication platform for user engagement and retention
        </p>
      </div>

      {/* Navigation Tabs */}
      <div className="mb-8">
        <div className="flex flex-wrap gap-2 p-1 bg-slate-800/50 rounded-xl border border-slate-700/50">
          {[
            { id: 'dashboard', label: 'Dashboard', icon: BarChart3 },
            { id: 'templates', label: 'Templates', icon: FileText },
            { id: 'campaigns', label: 'Campaigns', icon: Send },
            { id: 'analytics', label: 'Analytics', icon: TrendingUp },
            { id: 'preferences', label: 'User Preferences', icon: Settings },
            { id: 'settings', label: 'System Settings', icon: Monitor }
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
      {(activeTab === 'templates' || activeTab === 'campaigns' || activeTab === 'preferences') && (
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
          
          {activeTab === 'templates' && (
            <div className="flex gap-2">
              <select
                value={selectedFilters.type || ''}
                onChange={(e) => setSelectedFilters(prev => ({ ...prev, type: e.target.value || null }))}
                className="px-4 py-3 bg-slate-800/50 border border-slate-700/50 rounded-xl text-white focus:outline-none focus:ring-2 focus:ring-purple-500/50"
              >
                <option value="">All Types</option>
                <option value="email">Email</option>
                <option value="sms">SMS</option>
                <option value="push">Push</option>
                <option value="in_app">In-App</option>
              </select>
              
              <select
                value={selectedFilters.category || ''}
                onChange={(e) => setSelectedFilters(prev => ({ ...prev, category: e.target.value || null }))}
                className="px-4 py-3 bg-slate-800/50 border border-slate-700/50 rounded-xl text-white focus:outline-none focus:ring-2 focus:ring-purple-500/50"
              >
                <option value="">All Categories</option>
                <option value="marketing">Marketing</option>
                <option value="transactional">Transactional</option>
                <option value="system">System</option>
                <option value="trading">Trading</option>
                <option value="security">Security</option>
              </select>
            </div>
          )}
        </div>
      )}

      {/* Content */}
      <div className="space-y-6">
        {activeTab === 'dashboard' && analytics && <DashboardTab analytics={analytics} templates={templates} campaigns={campaigns} />}
        {activeTab === 'templates' && <TemplatesTab templates={filteredTemplates} onSendTest={sendTestNotification} onDuplicate={duplicateTemplate} loading={loading} />}
        {activeTab === 'campaigns' && <CampaignsTab campaigns={filteredCampaigns} onPause={pauseCampaign} loading={loading} />}
        {activeTab === 'analytics' && analytics && <AnalyticsTab analytics={analytics} />}
        {activeTab === 'preferences' && <PreferencesTab preferences={filteredPreferences} />}
        {activeTab === 'settings' && <SettingsTab />}
      </div>
    </div>
  )
}

// Dashboard Tab Component
interface DashboardTabProps {
  analytics: NotificationAnalytics
  templates: NotificationTemplate[]
  campaigns: NotificationCampaign[]
}

const DashboardTab: React.FC<DashboardTabProps> = ({ analytics, templates, campaigns }) => {
  const activeCampaigns = campaigns.filter(c => c.status === 'running').length
  const scheduledCampaigns = campaigns.filter(c => c.status === 'scheduled').length
  const activeTemplates = templates.filter(t => t.isActive).length

  return (
    <div className="space-y-6">
      {/* Key Metrics */}
      <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-4">
        <div className="glass-card p-6 rounded-2xl hover:scale-105 transition-all duration-300">
          <div className="flex items-center justify-between mb-4">
            <div className="p-3 rounded-xl bg-gradient-to-br from-blue-500/20 to-blue-600/20">
              <Send className="h-6 w-6 text-blue-400" />
            </div>
            <div className="text-right">
              <div className="text-2xl font-bold text-white">{(analytics.totalSent / 1000).toFixed(0)}K</div>
              <div className="text-sm text-blue-400 flex items-center justify-end">
                <TrendingUp className="h-3 w-3 mr-1" />
                +24%
              </div>
            </div>
          </div>
          <h3 className="text-blue-400 font-semibold mb-1">Total Sent</h3>
          <p className="text-slate-400 text-sm">this month</p>
        </div>

        <div className="glass-card p-6 rounded-2xl hover:scale-105 transition-all duration-300">
          <div className="flex items-center justify-between mb-4">
            <div className="p-3 rounded-xl bg-gradient-to-br from-green-500/20 to-green-600/20">
              <CheckCircle className="h-6 w-6 text-green-400" />
            </div>
            <div className="text-right">
              <div className="text-2xl font-bold text-white">{analytics.averageDeliveryRate}%</div>
              <div className="text-sm text-green-400 flex items-center justify-end">
                <TrendingUp className="h-3 w-3 mr-1" />
                +0.8%
              </div>
            </div>
          </div>
          <h3 className="text-green-400 font-semibold mb-1">Delivery Rate</h3>
          <p className="text-slate-400 text-sm">average across all channels</p>
        </div>

        <div className="glass-card p-6 rounded-2xl hover:scale-105 transition-all duration-300">
          <div className="flex items-center justify-between mb-4">
            <div className="p-3 rounded-xl bg-gradient-to-br from-purple-500/20 to-purple-600/20">
              <Eye className="h-6 w-6 text-purple-400" />
            </div>
            <div className="text-right">
              <div className="text-2xl font-bold text-white">{analytics.averageOpenRate}%</div>
              <div className="text-sm text-purple-400 flex items-center justify-end">
                <TrendingUp className="h-3 w-3 mr-1" />
                +5.2%
              </div>
            </div>
          </div>
          <h3 className="text-purple-400 font-semibold mb-1">Open Rate</h3>
          <p className="text-slate-400 text-sm">user engagement</p>
        </div>

        <div className="glass-card p-6 rounded-2xl hover:scale-105 transition-all duration-300">
          <div className="flex items-center justify-between mb-4">
            <div className="p-3 rounded-xl bg-gradient-to-br from-cyan-500/20 to-cyan-600/20">
              <Target className="h-6 w-6 text-cyan-400" />
            </div>
            <div className="text-right">
              <div className="text-2xl font-bold text-white">{analytics.averageClickRate}%</div>
              <div className="text-sm text-cyan-400 flex items-center justify-end">
                <TrendingUp className="h-3 w-3 mr-1" />
                +3.1%
              </div>
            </div>
          </div>
          <h3 className="text-cyan-400 font-semibold mb-1">Click Rate</h3>
          <p className="text-slate-400 text-sm">action conversion</p>
        </div>
      </div>

      {/* Performance by Channel */}
      <div className="grid gap-6 md:grid-cols-2">
        <div className="glass-card p-6 rounded-2xl">
          <h3 className="text-xl font-bold text-white mb-6 flex items-center">
            <BarChart3 className="w-5 h-5 mr-2 text-orange-400" />
            Performance by Channel
          </h3>
          <div className="space-y-4">
            {Object.entries(analytics.performanceByType).map(([type, metrics]) => (
              <div key={type} className="p-4 bg-slate-800/30 rounded-xl">
                <div className="flex items-center justify-between mb-2">
                  <div className="flex items-center space-x-3">
                    <div className={`p-2 rounded-lg ${
                      type === 'email' ? 'bg-blue-500/20' :
                      type === 'push' ? 'bg-green-500/20' :
                      type === 'sms' ? 'bg-purple-500/20' :
                      'bg-gray-500/20'
                    }`}>
                      {type === 'email' && <Mail className="w-4 h-4 text-blue-400" />}
                      {type === 'push' && <Bell className="w-4 h-4 text-green-400" />}
                      {type === 'sms' && <MessageSquare className="w-4 h-4 text-purple-400" />}
                    </div>
                    <span className="text-white font-medium capitalize">{type}</span>
                  </div>
                  <div className="text-slate-400 text-sm">{metrics.sent.toLocaleString()} sent</div>
                </div>
                <div className="flex space-x-4">
                  <div className="text-center">
                    <p className="text-sm text-slate-400">Open Rate</p>
                    <p className="text-lg font-semibold text-white">{metrics.openRate}%</p>
                  </div>
                  <div className="text-center">
                    <p className="text-sm text-slate-400">Click Rate</p>
                    <p className="text-lg font-semibold text-white">{metrics.clickRate}%</p>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>

        <div className="glass-card p-6 rounded-2xl">
          <h3 className="text-xl font-bold text-white mb-6 flex items-center">
            <Award className="w-5 h-5 mr-2 text-yellow-400" />
            Top Performing Templates
          </h3>
          <div className="space-y-4">
            {analytics.topPerformingTemplates.map((template, index) => (
              <div key={template.templateName} className="flex items-center justify-between p-4 bg-slate-800/30 rounded-xl">
                <div className="flex items-center space-x-3">
                  <div className={`w-8 h-8 rounded-full flex items-center justify-center font-bold text-white ${
                    index === 0 ? 'bg-gradient-to-r from-yellow-500 to-orange-500' :
                    index === 1 ? 'bg-gradient-to-r from-gray-400 to-gray-600' :
                    index === 2 ? 'bg-gradient-to-r from-orange-600 to-red-600' :
                    'bg-slate-600'
                  }`}>
                    {index + 1}
                  </div>
                  <div>
                    <p className="text-white font-medium">{template.templateName}</p>
                    <p className="text-slate-400 text-sm">{template.sent.toLocaleString()} sent</p>
                  </div>
                </div>
                <div className="text-right">
                  <p className="text-white font-semibold">{template.openRate}% open</p>
                  <p className="text-slate-400 text-sm">{template.clickRate}% click</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Recent Activity */}
      <div className="glass-card p-6 rounded-2xl">
        <h3 className="text-xl font-bold text-white mb-6 flex items-center">
          <Clock className="w-5 h-5 mr-2 text-green-400" />
          Recent Activity
        </h3>
        <div className="space-y-4">
          {analytics.recentActivity.map((activity) => (
            <div key={activity.id} className="flex items-start space-x-4 p-4 bg-slate-800/30 rounded-xl hover:bg-slate-700/30 transition-colors">
              <div className={`p-2 rounded-lg ${
                activity.type === 'campaign' ? 'bg-blue-500/20' :
                activity.type === 'template' ? 'bg-purple-500/20' :
                'bg-green-500/20'
              }`}>
                {activity.type === 'campaign' && <Send className="w-4 h-4 text-blue-400" />}
                {activity.type === 'template' && <FileText className="w-4 h-4 text-purple-400" />}
                {activity.type === 'system' && <Monitor className="w-4 h-4 text-green-400" />}
              </div>
              <div className="flex-1">
                <p className="text-white font-medium">{activity.action}</p>
                <p className="text-slate-400 text-sm">{activity.details}</p>
                <p className="text-slate-500 text-xs mt-1">
                  {activity.timestamp.toLocaleString()}
                </p>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}

// Placeholder components for other tabs
const TemplatesTab: React.FC<any> = ({ templates, onSendTest, onDuplicate, loading }) => {
  return (
    <div className="glass-card rounded-2xl overflow-hidden">
      <div className="p-6 border-b border-slate-700/50">
        <h3 className="text-xl font-bold text-white flex items-center">
          <FileText className="w-5 h-5 mr-2 text-purple-400" />
          Notification Templates ({templates.length})
        </h3>
      </div>
      <div className="p-6">
        <p className="text-slate-400">Template management interface coming soon...</p>
      </div>
    </div>
  )
}

const CampaignsTab: React.FC<any> = ({ campaigns, onPause, loading }) => {
  return (
    <div className="glass-card rounded-2xl overflow-hidden">
      <div className="p-6 border-b border-slate-700/50">
        <h3 className="text-xl font-bold text-white flex items-center">
          <Send className="w-5 h-5 mr-2 text-cyan-400" />
          Notification Campaigns ({campaigns.length})
        </h3>
      </div>
      <div className="p-6">
        <p className="text-slate-400">Campaign management interface coming soon...</p>
      </div>
    </div>
  )
}

const AnalyticsTab: React.FC<any> = ({ analytics }) => {
  return (
    <div className="glass-card rounded-2xl p-6">
      <h3 className="text-xl font-bold text-white mb-6 flex items-center">
        <TrendingUp className="w-5 h-5 mr-2 text-green-400" />
        Notification Analytics
      </h3>
      <p className="text-slate-400">Advanced analytics interface coming soon...</p>
    </div>
  )
}

const PreferencesTab: React.FC<any> = ({ preferences }) => {
  return (
    <div className="glass-card rounded-2xl p-6">
      <h3 className="text-xl font-bold text-white mb-6 flex items-center">
        <Settings className="w-5 h-5 mr-2 text-orange-400" />
        User Preferences ({preferences.length})
      </h3>
      <p className="text-slate-400">User preference management interface coming soon...</p>
    </div>
  )
}

const SettingsTab: React.FC = () => {
  return (
    <div className="glass-card rounded-2xl p-6">
      <h3 className="text-xl font-bold text-white mb-6 flex items-center">
        <Monitor className="w-5 h-5 mr-2 text-blue-400" />
        System Settings
      </h3>
      <p className="text-slate-400">System settings interface coming soon...</p>
    </div>
  )
}

export default NotificationManagement