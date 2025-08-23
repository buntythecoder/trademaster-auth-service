import React, { useState, useEffect } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { Bell, BellRing } from 'lucide-react'
import { NotificationCenter } from './NotificationCenter'
import type { Notification } from '../../types/notifications'
import { cn } from '@/lib/utils'

interface NotificationBellProps {
  className?: string
  size?: 'sm' | 'md' | 'lg'
  notifications?: Notification[]
  onNotificationAction?: (action: 'read' | 'delete' | 'clear', id?: string) => void
}

export const NotificationBell: React.FC<NotificationBellProps> = ({
  className = '',
  size = 'md',
  notifications = [],
  onNotificationAction
}) => {
  const [isOpen, setIsOpen] = useState(false)
  const [hasNewNotifications, setHasNewNotifications] = useState(false)
  const [lastNotificationCount, setLastNotificationCount] = useState(0)

  const unreadCount = notifications.filter(n => !n.read).length

  // Check for new notifications
  useEffect(() => {
    if (notifications.length > lastNotificationCount && lastNotificationCount > 0) {
      setHasNewNotifications(true)
      
      // Reset the animation after a short delay
      const timer = setTimeout(() => {
        setHasNewNotifications(false)
      }, 2000)
      
      return () => clearTimeout(timer)
    }
    setLastNotificationCount(notifications.length)
  }, [notifications.length, lastNotificationCount])

  const sizeClasses = {
    sm: 'w-4 h-4',
    md: 'w-5 h-5',
    lg: 'w-6 h-6'
  }

  const handleToggle = () => {
    setIsOpen(!isOpen)
    setHasNewNotifications(false)
  }

  const handleMarkAsRead = (id: string) => {
    onNotificationAction?.('read', id)
  }

  const handleMarkAllAsRead = () => {
    onNotificationAction?.('read')
  }

  const handleDelete = (id: string) => {
    onNotificationAction?.('delete', id)
  }

  const handleClearAll = () => {
    onNotificationAction?.('clear')
  }

  return (
    <>
      {/* Bell Button */}
      <div className="relative">
        <motion.button
          onClick={handleToggle}
          className={cn(
            'relative p-2 rounded-lg transition-colors duration-200',
            'hover:bg-slate-700/50 focus:outline-none focus:ring-2 focus:ring-purple-500/20',
            hasNewNotifications && 'animate-pulse',
            isOpen ? 'bg-slate-700/50 text-purple-400' : 'text-slate-400 hover:text-white',
            className
          )}
          whileTap={{ scale: 0.95 }}
          animate={hasNewNotifications ? { 
            rotate: [0, -10, 10, -10, 0],
            transition: { duration: 0.6, repeat: 2 }
          } : {}}
        >
          {/* Bell Icon */}
          <motion.div
            animate={hasNewNotifications ? { scale: [1, 1.1, 1] } : {}}
            transition={{ duration: 0.3 }}
          >
            {hasNewNotifications ? (
              <BellRing className={sizeClasses[size]} />
            ) : (
              <Bell className={sizeClasses[size]} />
            )}
          </motion.div>

          {/* Notification Badge */}
          <AnimatePresence>
            {unreadCount > 0 && (
              <motion.div
                initial={{ scale: 0, opacity: 0 }}
                animate={{ scale: 1, opacity: 1 }}
                exit={{ scale: 0, opacity: 0 }}
                className="absolute -top-1 -right-1 min-w-[18px] h-[18px] bg-red-500 text-white text-xs font-bold rounded-full flex items-center justify-center border-2 border-slate-900"
              >
                {unreadCount > 99 ? '99+' : unreadCount}
              </motion.div>
            )}
          </AnimatePresence>

          {/* Pulse Ring for New Notifications */}
          <AnimatePresence>
            {hasNewNotifications && (
              <motion.div
                initial={{ scale: 1, opacity: 0.8 }}
                animate={{ 
                  scale: [1, 1.5, 2],
                  opacity: [0.8, 0.3, 0]
                }}
                exit={{ opacity: 0 }}
                transition={{ duration: 1.5, repeat: Infinity }}
                className="absolute inset-0 rounded-lg border-2 border-purple-400 pointer-events-none"
              />
            )}
          </AnimatePresence>
        </motion.button>
      </div>

      {/* Notification Center */}
      <AnimatePresence>
        {isOpen && (
          <NotificationCenter
            isOpen={isOpen}
            onClose={() => setIsOpen(false)}
            notifications={notifications}
            onMarkAsRead={handleMarkAsRead}
            onMarkAllAsRead={handleMarkAllAsRead}
            onDelete={handleDelete}
            onClearAll={handleClearAll}
          />
        )}
      </AnimatePresence>
    </>
  )
}

export default NotificationBell