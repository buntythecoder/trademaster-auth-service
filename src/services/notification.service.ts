import type { Notification } from '../types/notifications'

export interface NotificationSubscription {
  id: string
  type: 'price_alert' | 'order_update' | 'portfolio_update' | 'market_news' | 'system'
  enabled: boolean
  conditions: any
}

export interface PriceAlert {
  symbol: string
  type: 'above' | 'below' | 'percent_change'
  value: number
  currentPrice?: number
  enabled: boolean
}

class NotificationService {
  private notifications: Notification[] = []
  private subscribers: ((notifications: Notification[]) => void)[] = []
  private priceAlerts: PriceAlert[] = []
  private notificationPermission: NotificationPermission = 'default'

  constructor() {
    this.initializeService()
  }

  private async initializeService() {
    // Request notification permission
    if ('Notification' in window) {
      this.notificationPermission = await Notification.requestPermission()
    }

    // Load stored notifications and alerts
    this.loadStoredData()
  }

  private loadStoredData() {
    try {
      const storedNotifications = localStorage.getItem('trademaster_notifications')
      if (storedNotifications) {
        this.notifications = JSON.parse(storedNotifications).map((n: any) => ({
          ...n,
          timestamp: new Date(n.timestamp)
        }))
      }

      const storedAlerts = localStorage.getItem('trademaster_price_alerts')
      if (storedAlerts) {
        this.priceAlerts = JSON.parse(storedAlerts)
      }
    } catch (error) {
      console.error('Error loading stored notification data:', error)
    }
  }

  private saveData() {
    try {
      localStorage.setItem('trademaster_notifications', JSON.stringify(this.notifications))
      localStorage.setItem('trademaster_price_alerts', JSON.stringify(this.priceAlerts))
    } catch (error) {
      console.error('Error saving notification data:', error)
    }
  }

  // Notification Management
  addNotification(notification: Omit<Notification, 'id' | 'timestamp'>): string {
    const id = `notif_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
    
    const newNotification: Notification = {
      ...notification,
      id,
      timestamp: new Date()
    }

    this.notifications.unshift(newNotification)

    // Limit to 100 notifications
    if (this.notifications.length > 100) {
      this.notifications = this.notifications.slice(0, 100)
    }

    // Show browser notification for high priority alerts
    if (notification.priority === 'high' || notification.priority === 'critical') {
      this.showBrowserNotification(newNotification)
    }

    this.saveData()
    this.notifySubscribers()
    
    return id
  }

  private showBrowserNotification(notification: Notification) {
    if (this.notificationPermission !== 'granted' || !('Notification' in window)) {
      return
    }

    const options: NotificationOptions = {
      body: notification.message,
      icon: '/favicon.ico',
      badge: '/favicon.ico',
      tag: notification.type,
      requireInteraction: notification.priority === 'critical',
      silent: false
    }

    try {
      const browserNotification = new Notification(notification.title, options)
      
      browserNotification.onclick = () => {
        window.focus()
        browserNotification.close()
        
        // Navigate to relevant page if actionUrl is provided
        if (notification.actionUrl) {
          window.location.href = notification.actionUrl
        }
      }

      // Auto close after 5 seconds for non-critical notifications
      if (notification.priority !== 'critical') {
        setTimeout(() => browserNotification.close(), 5000)
      }
    } catch (error) {
      console.error('Error showing browser notification:', error)
    }
  }

  getNotifications(): Notification[] {
    return [...this.notifications]
  }

  markAsRead(id: string) {
    this.notifications = this.notifications.map(n => 
      n.id === id ? { ...n, read: true } : n
    )
    this.saveData()
    this.notifySubscribers()
  }

  markAllAsRead() {
    this.notifications = this.notifications.map(n => ({ ...n, read: true }))
    this.saveData()
    this.notifySubscribers()
  }

  deleteNotification(id: string) {
    this.notifications = this.notifications.filter(n => n.id !== id)
    this.saveData()
    this.notifySubscribers()
  }

  clearAllNotifications() {
    this.notifications = []
    this.saveData()
    this.notifySubscribers()
  }

  // Price Alerts Management
  addPriceAlert(alert: PriceAlert): void {
    this.priceAlerts.push(alert)
    this.saveData()
  }

  removePriceAlert(symbol: string, type: PriceAlert['type'], value: number): void {
    this.priceAlerts = this.priceAlerts.filter(
      alert => !(alert.symbol === symbol && alert.type === type && alert.value === value)
    )
    this.saveData()
  }

  getPriceAlerts(): PriceAlert[] {
    return [...this.priceAlerts]
  }

  checkPriceAlerts(symbol: string, currentPrice: number): void {
    const triggeredAlerts = this.priceAlerts.filter(alert => {
      if (!alert.enabled || alert.symbol !== symbol) return false

      switch (alert.type) {
        case 'above':
          return currentPrice >= alert.value
        case 'below':
          return currentPrice <= alert.value
        case 'percent_change':
          if (!alert.currentPrice) return false
          const changePercent = Math.abs((currentPrice - alert.currentPrice) / alert.currentPrice) * 100
          return changePercent >= alert.value
        default:
          return false
      }
    })

    triggeredAlerts.forEach(alert => {
      const message = this.formatAlertMessage(alert, currentPrice)
      
      this.addNotification({
        type: 'price_alert',
        title: 'Price Alert Triggered',
        message,
        read: false,
        priority: 'high',
        category: 'Alerts',
        data: {
          symbol: alert.symbol,
          alertType: alert.type,
          targetPrice: alert.value,
          currentPrice
        }
      })

      // Remove the alert after triggering
      this.removePriceAlert(alert.symbol, alert.type, alert.value)
    })
  }

  private formatAlertMessage(alert: PriceAlert, currentPrice: number): string {
    const symbol = alert.symbol
    const currency = '₹'
    
    switch (alert.type) {
      case 'above':
        return `${symbol} has crossed above ${currency}${alert.value}. Current price: ${currency}${currentPrice.toFixed(2)}`
      case 'below':
        return `${symbol} has dropped below ${currency}${alert.value}. Current price: ${currency}${currentPrice.toFixed(2)}`
      case 'percent_change':
        const change = alert.currentPrice ? 
          ((currentPrice - alert.currentPrice) / alert.currentPrice * 100).toFixed(2) : '0'
        return `${symbol} has moved ${change}% from your alert price. Current: ${currency}${currentPrice.toFixed(2)}`
      default:
        return `Price alert for ${symbol}: ${currency}${currentPrice.toFixed(2)}`
    }
  }

  // Market Event Notifications
  onOrderUpdate(orderData: any): void {
    const status = orderData.status.toLowerCase()
    let priority: Notification['priority'] = 'medium'
    let title = 'Order Update'
    
    if (status === 'filled') {
      title = 'Order Executed'
      priority = 'medium'
    } else if (status === 'cancelled' || status === 'rejected') {
      title = 'Order Failed'
      priority = 'high'
    }

    const message = `Your ${orderData.side.toLowerCase()} order for ${orderData.quantity} shares of ${orderData.symbol} ${
      status === 'filled' ? 'has been successfully executed' :
      status === 'cancelled' ? 'has been cancelled' :
      status === 'rejected' ? 'has been rejected' :
      `is now ${status}`
    }${orderData.price ? ` at ${orderData.price}` : ''}.`

    this.addNotification({
      type: 'order_update',
      title,
      message,
      read: false,
      priority,
      category: 'Orders',
      data: orderData
    })
  }

  onPortfolioUpdate(portfolioData: any): void {
    // Only notify for significant changes
    if (Math.abs(portfolioData.dayPnLPercent) >= 2) {
      const isPositive = portfolioData.dayPnL >= 0
      const title = isPositive ? 'Portfolio Gains' : 'Portfolio Alert'
      const priority: Notification['priority'] = isPositive ? 'low' : 'medium'

      this.addNotification({
        type: 'portfolio_update',
        title,
        message: `Your portfolio ${isPositive ? 'gained' : 'lost'} ₹${Math.abs(portfolioData.dayPnL).toLocaleString()} (${portfolioData.dayPnLPercent >= 0 ? '+' : ''}${portfolioData.dayPnLPercent.toFixed(2)}%) today.`,
        read: false,
        priority,
        category: 'Portfolio',
        data: portfolioData
      })
    }
  }

  onMarketAlert(alertData: any): void {
    this.addNotification({
      type: 'market_news',
      title: alertData.title || 'Market Update',
      message: alertData.message,
      read: false,
      priority: alertData.priority || 'medium',
      category: 'Market',
      data: alertData
    })
  }

  // Achievement Notifications
  onAchievementUnlocked(achievement: any): void {
    this.addNotification({
      type: 'achievement',
      title: 'Achievement Unlocked!',
      message: `Congratulations! You've earned the "${achievement.title}" badge${achievement.description ? ': ' + achievement.description : '.'}`,
      read: false,
      priority: 'low',
      category: 'Achievements',
      data: achievement
    })
  }

  // Subscription Management
  subscribe(callback: (notifications: Notification[]) => void): () => void {
    this.subscribers.push(callback)
    
    // Immediately call with current notifications
    callback(this.notifications)
    
    // Return unsubscribe function
    return () => {
      this.subscribers = this.subscribers.filter(sub => sub !== callback)
    }
  }

  private notifySubscribers(): void {
    this.subscribers.forEach(callback => {
      try {
        callback(this.notifications)
      } catch (error) {
        console.error('Error notifying subscriber:', error)
      }
    })
  }

  // System Notifications
  addSystemNotification(message: string, priority: Notification['priority'] = 'medium'): void {
    this.addNotification({
      type: 'system',
      title: 'System Notice',
      message,
      read: false,
      priority,
      category: 'System'
    })
  }

  // Utility Methods
  getUnreadCount(): number {
    return this.notifications.filter(n => !n.read).length
  }

  getNotificationsByType(type: Notification['type']): Notification[] {
    return this.notifications.filter(n => n.type === type)
  }

  hasHighPriorityUnread(): boolean {
    return this.notifications.some(n => 
      !n.read && (n.priority === 'high' || n.priority === 'critical')
    )
  }
}

// Export singleton instance
export const notificationService = new NotificationService()
export default notificationService