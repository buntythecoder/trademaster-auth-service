export interface Notification {
  id: string
  type: 'price_alert' | 'order_update' | 'portfolio_update' | 'market_news' | 'system' | 'achievement'
  title: string
  message: string
  timestamp: Date
  read: boolean
  priority: 'low' | 'medium' | 'high' | 'critical'
  data?: any
  actionUrl?: string
  category: string
}