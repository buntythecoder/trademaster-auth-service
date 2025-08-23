import React, { useState, useEffect } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { 
  Bell,
  BellRing,
  X,
  Filter,
  TrendingUp,
  TrendingDown,
  AlertTriangle,
  CheckCircle,
  Info,
  DollarSign,
  Target,
  Activity,
  Calendar,
  Clock,
  User,
  Settings,
  MoreHorizontal,
  Archive,
  Trash2,
  Eye,
  EyeOff
} from 'lucide-react'
import { cn } from '@/lib/utils'
import type { Notification } from '../../types/notifications'

interface NotificationCenterProps {
  isOpen: boolean
  onClose: () => void
  notifications: Notification[]
  onMarkAsRead: (id: string) => void
  onMarkAllAsRead: () => void
  onDelete: (id: string) => void
  onClearAll: () => void
}

const generateMockNotifications = (): Notification[] => {
  const now = new Date()
  return [
    {
      id: '1',
      type: 'price_alert',
      title: 'Price Alert Triggered',
      message: 'RELIANCE has crossed your target price of ₹2,550. Current price: ₹2,557.30',
      timestamp: new Date(now.getTime() - 5 * 60 * 1000), // 5 minutes ago
      read: false,
      priority: 'high',
      category: 'Alerts',
      data: { symbol: 'RELIANCE', targetPrice: 2550, currentPrice: 2557.30 }
    },
    {
      id: '2',
      type: 'order_update',
      title: 'Order Executed',
      message: 'Your buy order for 25 shares of TCS at ₹3,640 has been successfully executed.',
      timestamp: new Date(now.getTime() - 15 * 60 * 1000), // 15 minutes ago
      read: false,
      priority: 'medium',
      category: 'Orders',
      data: { symbol: 'TCS', quantity: 25, price: 3640, side: 'BUY' }
    },
    {
      id: '3',
      type: 'portfolio_update',
      title: 'Daily P&L Summary',
      message: 'Your portfolio gained ₹3,247 (+1.34%) today. Great performance!',
      timestamp: new Date(now.getTime() - 2 * 60 * 60 * 1000), // 2 hours ago
      read: true,
      priority: 'low',
      category: 'Portfolio',
      data: { pnl: 3247, pnlPercent: 1.34 }
    },
    {
      id: '4',
      type: 'market_news',
      title: 'Market Update',
      message: 'NIFTY 50 reaches new all-time high. Tech stocks leading the rally.',
      timestamp: new Date(now.getTime() - 3 * 60 * 60 * 1000), // 3 hours ago
      read: true,
      priority: 'medium',
      category: 'Market',
      data: { index: 'NIFTY50', change: 2.1 }
    },
    {
      id: '5',
      type: 'achievement',
      title: 'Achievement Unlocked!',
      message: 'Congratulations! You\'ve earned the "Consistent Trader" badge for 30 consecutive days of trading.',
      timestamp: new Date(now.getTime() - 6 * 60 * 60 * 1000), // 6 hours ago
      read: false,
      priority: 'low',
      category: 'Achievements',
      data: { badge: 'consistent_trader', streak: 30 }
    },
    {
      id: '6',
      type: 'system',
      title: 'Maintenance Notice',
      message: 'Scheduled maintenance on Sunday 2:00 AM - 4:00 AM IST. Trading will be unavailable.',
      timestamp: new Date(now.getTime() - 24 * 60 * 60 * 1000), // 1 day ago
      read: true,
      priority: 'medium',
      category: 'System'
    },
    {
      id: '7',
      type: 'price_alert',
      title: 'Stop Loss Triggered',
      message: 'Your stop loss for HDFC Bank at ₹1,580 has been triggered. Position closed.',
      timestamp: new Date(now.getTime() - 2 * 24 * 60 * 60 * 1000), // 2 days ago
      read: true,
      priority: 'critical',
      category: 'Alerts',
      data: { symbol: 'HDFCBANK', stopPrice: 1580, action: 'SELL' }
    }
  ]
}

const getNotificationIcon = (type: Notification['type'], priority: Notification['priority']) => {
  const iconClass = cn(
    'w-5 h-5',
    priority === 'critical' ? 'text-red-400' :
    priority === 'high' ? 'text-orange-400' :
    priority === 'medium' ? 'text-yellow-400' :
    'text-blue-400'
  )

  switch (type) {
    case 'price_alert':
      return <Target className={iconClass} />
    case 'order_update':
      return <CheckCircle className={iconClass} />
    case 'portfolio_update':
      return <TrendingUp className={iconClass} />
    case 'market_news':
      return <Activity className={iconClass} />
    case 'achievement':
      return <CheckCircle className={iconClass} />
    case 'system':
      return <Info className={iconClass} />
    default:
      return <Bell className={iconClass} />
  }
}

const getPriorityBadge = (priority: Notification['priority']) => {
  const colors = {
    critical: 'bg-red-500/20 text-red-400 border-red-500/30',
    high: 'bg-orange-500/20 text-orange-400 border-orange-500/30',
    medium: 'bg-yellow-500/20 text-yellow-400 border-yellow-500/30',
    low: 'bg-blue-500/20 text-blue-400 border-blue-500/30'
  }

  return (
    <span className={cn('px-2 py-1 text-xs font-medium rounded-full border', colors[priority])}>
      {priority.toUpperCase()}
    </span>
  )
}

export const NotificationCenter: React.FC<NotificationCenterProps> = ({
  isOpen,
  onClose,
  notifications: propNotifications,
  onMarkAsRead,
  onMarkAllAsRead,
  onDelete,
  onClearAll
}) => {
  const [notifications, setNotifications] = useState<Notification[]>(() => 
    propNotifications.length > 0 ? propNotifications : generateMockNotifications()
  )
  const [filterType, setFilterType] = useState<'all' | Notification['type']>('all')
  const [showRead, setShowRead] = useState(true)

  useEffect(() => {
    if (propNotifications.length > 0) {
      setNotifications(propNotifications)
    }
  }, [propNotifications])

  const filteredNotifications = notifications.filter(notification => {
    if (filterType !== 'all' && notification.type !== filterType) return false
    if (!showRead && notification.read) return false
    return true
  })

  const unreadCount = notifications.filter(n => !n.read).length
  const categories = ['all', 'price_alert', 'order_update', 'portfolio_update', 'market_news', 'achievement', 'system'] as const

  const handleMarkAsRead = (id: string) => {
    setNotifications(prev => 
      prev.map(n => n.id === id ? { ...n, read: true } : n)
    )
    onMarkAsRead(id)
  }

  const handleDelete = (id: string) => {
    setNotifications(prev => prev.filter(n => n.id !== id))
    onDelete(id)
  }

  const handleMarkAllAsRead = () => {
    setNotifications(prev => prev.map(n => ({ ...n, read: true })))
    onMarkAllAsRead()
  }

  const handleClearAll = () => {
    setNotifications([])
    onClearAll()
  }

  const formatTimeAgo = (timestamp: Date) => {
    const now = new Date()
    const diff = now.getTime() - timestamp.getTime()
    
    const minutes = Math.floor(diff / (1000 * 60))
    const hours = Math.floor(diff / (1000 * 60 * 60))
    const days = Math.floor(diff / (1000 * 60 * 60 * 24))
    
    if (days > 0) return `${days}d ago`
    if (hours > 0) return `${hours}h ago`
    if (minutes > 0) return `${minutes}m ago`
    return 'Just now'
  }

  if (!isOpen) return null

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.95 }}
      animate={{ opacity: 1, scale: 1 }}
      exit={{ opacity: 0, scale: 0.95 }}
      className="fixed top-4 right-4 w-[720px] max-w-[95vw] h-[600px] max-h-[90vh] rounded-2xl z-50 overflow-hidden flex flex-col border border-purple-500/30 shadow-2xl"
      style={{
        background: 'rgba(15, 13, 35, 0.95)',
        backdropFilter: 'blur(20px)',
      }}
    >
      {/* Header */}
      <div className="p-4 border-b border-slate-700/50">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center space-x-3">
            <div className="p-2 bg-blue-500/20 rounded-xl">
              <BellRing className="w-5 h-5 text-blue-400" />
            </div>
            <div>
              <h3 className="text-lg font-bold text-white">Notifications</h3>
              <p className="text-sm text-slate-400">
                {unreadCount} unread of {notifications.length} total
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-2 hover:bg-slate-700/50 rounded-lg transition-colors"
          >
            <X className="w-5 h-5 text-slate-400" />
          </button>
        </div>

        {/* Actions */}
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <button
              onClick={handleMarkAllAsRead}
              className="px-3 py-2 text-sm text-blue-400 hover:text-blue-300 transition-colors"
              disabled={unreadCount === 0}
            >
              Mark All Read
            </button>
            <button
              onClick={handleClearAll}
              className="px-3 py-2 text-sm text-red-400 hover:text-red-300 transition-colors"
            >
              Clear All
            </button>
          </div>
          
          <button
            onClick={() => setShowRead(!showRead)}
            className="p-2 hover:bg-slate-700/50 rounded-lg transition-colors"
            title={showRead ? 'Hide read notifications' : 'Show read notifications'}
          >
            {showRead ? (
              <Eye className="w-4 h-4 text-slate-400" />
            ) : (
              <EyeOff className="w-4 h-4 text-slate-400" />
            )}
          </button>
        </div>
      </div>

      {/* Filters */}
      <div className="p-4 border-b border-slate-700/50">
        <div className="flex items-center flex-wrap gap-2">
          {categories.map(category => (
            <button
              key={category}
              onClick={() => setFilterType(category)}
              className={cn(
                'px-3 py-2 text-sm rounded-lg transition-colors',
                filterType === category
                  ? 'bg-purple-500/20 text-purple-300 border border-purple-500/30'
                  : 'text-slate-400 hover:text-white hover:bg-slate-700/50'
              )}
            >
              {category === 'all' ? 'All' : category.replace('_', ' ').toUpperCase()}
            </button>
          ))}
        </div>
      </div>

      {/* Notifications List */}
      <div className="flex-1 overflow-y-auto custom-scrollbar">
        <AnimatePresence mode="popLayout">
          {filteredNotifications.length === 0 ? (
            <div className="p-8 text-center text-slate-400">
              <Bell className="w-12 h-12 mx-auto mb-3 opacity-50" />
              <div className="text-lg font-medium mb-1">No notifications</div>
              <div className="text-sm">You're all caught up!</div>
            </div>
          ) : (
            filteredNotifications.map(notification => (
              <motion.div
                key={notification.id}
                layout
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -20 }}
                className={cn(
                  'p-4 border-b border-slate-700/30 hover:bg-slate-700/30 transition-colors',
                  !notification.read && 'bg-slate-800/30'
                )}
              >
                <div className="flex items-start space-x-3 min-w-0">
                  {/* Icon */}
                  <div className="flex-shrink-0 mt-0.5">
                    {getNotificationIcon(notification.type, notification.priority)}
                  </div>

                  {/* Content - Full width with proper constraints */}
                  <div className="flex-1 min-w-0 overflow-hidden">
                    {/* Header row with title and priority */}
                    <div className="flex items-start justify-between gap-3 mb-2">
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-2 mb-1">
                          <h4 className={cn(
                            'text-sm font-semibold break-words line-clamp-1',
                            notification.read ? 'text-slate-300' : 'text-white'
                          )}>
                            {notification.title}
                          </h4>
                          {!notification.read && (
                            <div className="w-2 h-2 bg-blue-400 rounded-full flex-shrink-0"></div>
                          )}
                        </div>
                        
                        {/* Message with proper text wrapping */}
                        <p className={cn(
                          'text-sm leading-relaxed break-words hyphens-auto',
                          notification.read ? 'text-slate-400' : 'text-slate-300'
                        )}>
                          {notification.message}
                        </p>
                      </div>

                      {/* Priority Badge - fixed width */}
                      <div className="flex-shrink-0">
                        {getPriorityBadge(notification.priority)}
                      </div>
                    </div>

                    {/* Footer with proper flex constraints */}
                    <div className="flex items-center justify-between gap-2 min-w-0">
                      <div className="flex items-center gap-2 text-xs text-slate-500 min-w-0 flex-1">
                        <span className="flex items-center gap-1 flex-shrink-0">
                          <Clock className="w-3 h-3" />
                          <span className="whitespace-nowrap">{formatTimeAgo(notification.timestamp)}</span>
                        </span>
                        <span className="flex-shrink-0">•</span>
                        <span className="truncate">{notification.category}</span>
                      </div>

                      {/* Actions - fixed width */}
                      <div className="flex items-center gap-1 flex-shrink-0">
                        {!notification.read && (
                          <button
                            onClick={() => handleMarkAsRead(notification.id)}
                            className="p-1 hover:bg-slate-600/50 rounded transition-colors flex-shrink-0"
                            title="Mark as read"
                          >
                            <CheckCircle className="w-4 h-4 text-slate-500 hover:text-green-400" />
                          </button>
                        )}
                        <button
                          onClick={() => handleDelete(notification.id)}
                          className="p-1 hover:bg-slate-600/50 rounded transition-colors flex-shrink-0"
                          title="Delete notification"
                        >
                          <Trash2 className="w-4 h-4 text-slate-500 hover:text-red-400" />
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              </motion.div>
            ))
          )}
        </AnimatePresence>
      </div>

      {/* Footer */}
      {filteredNotifications.length > 0 && (
        <div className="p-4 border-t border-slate-700/50 bg-slate-800/30">
          <div className="flex items-center justify-between text-sm text-slate-400">
            <div className="flex items-center space-x-4">
              <span>Showing {filteredNotifications.length} of {notifications.length}</span>
              {unreadCount > 0 && (
                <span className="px-2 py-1 bg-blue-500/20 text-blue-400 rounded-full text-xs">
                  {unreadCount} unread
                </span>
              )}
            </div>
            <button className="text-purple-400 hover:text-purple-300 transition-colors px-3 py-1 rounded-lg hover:bg-purple-500/10">
              Settings
            </button>
          </div>
        </div>
      )}
    </motion.div>
  )
}

export default NotificationCenter