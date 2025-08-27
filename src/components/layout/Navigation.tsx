import React, { useState, useEffect } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuthStore } from '../../stores/auth.store'
import { ThemeToggle } from '../ui/ThemeToggle'
import { SymbolLookup } from '../common/SymbolLookup'
import { NotificationBell } from '../notifications/NotificationBell'
import { notificationService } from '../../services/notification.service'
import type { Notification } from '../../types/notifications'
import { LogOut, User, TrendingUp, PieChart, BarChart3, Home, Settings, Shield, Trophy, Bot, Smartphone } from 'lucide-react'

interface NavigationProps {
  title?: string
  showWelcome?: boolean
  onSymbolSelect?: (symbol: string) => void
}

export function Navigation({ title = "TradeMaster", showWelcome = false, onSymbolSelect }: NavigationProps) {
  const { user } = useAuthStore()
  const location = useLocation()
  const navigate = useNavigate()
  const [notifications, setNotifications] = useState<Notification[]>([])
  
  useEffect(() => {
    const unsubscribe = notificationService.subscribe((newNotifications) => {
      setNotifications(newNotifications)
    })
    
    return unsubscribe
  }, [])
  
  const handleNotificationAction = (action: 'read' | 'delete' | 'clear', id?: string) => {
    if (action === 'read' && id) {
      notificationService.markAsRead(id)
    } else if (action === 'read' && !id) {
      notificationService.markAllAsRead()
    } else if (action === 'delete' && id) {
      notificationService.deleteNotification(id)
    } else if (action === 'clear') {
      notificationService.clearAllNotifications()
    }
  }
  
  const isActive = (path: string) => location.pathname === path
  
  const navigationItems = [
    {
      path: '/dashboard',
      label: 'Dashboard',
      icon: Home,
      color: 'purple'
    },
    ...(user?.role !== 'ADMIN' ? [
      {
        path: '/market-data',
        label: 'Market',
        icon: TrendingUp,
        color: 'cyan'
      },
      {
        path: '/trading',
        label: 'Trading',
        icon: BarChart3,
        color: 'green'
      },
      {
        path: '/mobile-trading',
        label: 'Mobile',
        icon: Smartphone,
        color: 'indigo'
      },
      {
        path: '/portfolio',
        label: 'Portfolio',
        icon: PieChart,
        color: 'orange'
      },
      {
        path: '/brokers',
        label: 'Brokers',
        icon: Settings,
        color: 'blue'
      },
      {
        path: '/risk',
        label: 'Risk',
        icon: Shield,
        color: 'red'
      },
      {
        path: '/gamification',
        label: 'Rewards',
        icon: Trophy,
        color: 'yellow'
      },
      {
        path: '/agents',
        label: 'Agents',
        icon: Bot,
        color: 'purple'
      }
    ] : [
      {
        path: '/admin/users',
        label: 'Users',
        icon: User,
        color: 'cyan'
      },
      {
        path: '/admin/system',
        label: 'System',
        icon: Settings,
        color: 'orange'
      }
    ]),
    {
      path: '/profile',
      label: 'Profile',
      icon: User,
      color: 'indigo'
    }
  ]
  
  const getItemClasses = (item: any) => {
    const baseClasses = "flex items-center space-x-2 transition-colors p-2 rounded-lg text-sm font-medium"
    
    if (isActive(item.path)) {
      const colorMap = {
        purple: 'text-purple-400 bg-purple-500/20',
        cyan: 'text-cyan-400 bg-cyan-500/20',
        green: 'text-green-400 bg-green-500/20',
        orange: 'text-orange-400 bg-orange-500/20',
        blue: 'text-blue-400 bg-blue-500/20',
        indigo: 'text-indigo-400 bg-indigo-500/20'
      }
      return `${baseClasses} ${colorMap[item.color as keyof typeof colorMap]}`
    }
    
    const hoverColorMap = {
      purple: 'text-slate-400 hover:text-purple-400 hover:bg-purple-500/10',
      cyan: 'text-slate-400 hover:text-cyan-400 hover:bg-cyan-500/10',
      green: 'text-slate-400 hover:text-green-400 hover:bg-green-500/10',
      orange: 'text-slate-400 hover:text-orange-400 hover:bg-orange-500/10',
      blue: 'text-slate-400 hover:text-blue-400 hover:bg-blue-500/10',
      indigo: 'text-slate-400 hover:text-indigo-400 hover:bg-indigo-500/10'
    }
    
    return `${baseClasses} ${hoverColorMap[item.color as keyof typeof hoverColorMap]}`
  }

  return (
    <header className="glass-card-dark border-b border-slate-700/50 backdrop-blur-xl">
      <div className="container mx-auto flex h-16 items-center justify-between px-6">
        {/* Left Section */}
        <div className="flex items-center space-x-4">
          <Link 
            to="/dashboard" 
            className="text-xl font-bold gradient-text hover:opacity-80 transition-opacity"
          >
            {title}
          </Link>
          {showWelcome && user && (
            <span className="text-sm text-slate-400">
              Welcome back, <span className="text-purple-400 font-medium">{user.firstName}</span>
            </span>
          )}
        </div>

        {/* Center Section - Global Symbol Search */}
        <div className="hidden md:block flex-1 max-w-md mx-8">
          <SymbolLookup
            placeholder="Search any symbol..."
            className="w-full"
            maxResults={5}
            showDetails={false}
            onSymbolSelect={(symbolData) => {
              console.log('Global symbol selected:', symbolData.symbol)
              // Navigate to market data with selected symbol
              if (onSymbolSelect) {
                onSymbolSelect(symbolData.symbol)
              }
              // Always navigate to market-data page when symbol is selected
              if (location.pathname !== '/market-data') {
                navigate('/market-data', { state: { selectedSymbol: symbolData.symbol } })
              }
            }}
          />
        </div>

        {/* Navigation Items - Moved to Right */}
        <nav className="hidden lg:flex items-center space-x-1">
          {navigationItems.map((item) => {
            const Icon = item.icon
            return (
              <Link
                key={item.path}
                to={item.path}
                className={getItemClasses(item)}
              >
                <Icon className="w-4 h-4" />
                <span>{item.label}</span>
              </Link>
            )
          })}
        </nav>

        {/* Right Section */}
        <div className="flex items-center space-x-3">
          <NotificationBell 
            notifications={notifications}
            onNotificationAction={handleNotificationAction}
          />
          <ThemeToggle />
          
          {/* Mobile Navigation Dropdown */}
          <div className="md:hidden relative group">
            <button className="flex items-center space-x-2 text-slate-400 hover:text-white transition-colors p-2 rounded-lg hover:bg-slate-800/50">
              <BarChart3 className="w-4 h-4" />
              <span className="text-sm">Menu</span>
            </button>
            
            <div className="absolute right-0 top-full mt-2 w-48 glass-card rounded-xl border border-slate-600/50 opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all duration-200 z-50">
              <div className="p-2 space-y-1">
                {navigationItems.map((item) => {
                  const Icon = item.icon
                  return (
                    <Link
                      key={item.path}
                      to={item.path}
                      className={getItemClasses(item)}
                    >
                      <Icon className="w-4 h-4" />
                      <span>{item.label}</span>
                    </Link>
                  )
                })}
              </div>
            </div>
          </div>
          
          {/* User Role Badge */}
          {user?.role && (
            <div className="hidden sm:flex items-center space-x-2 px-3 py-1 rounded-lg bg-slate-800/50 border border-slate-600/30">
              <div className={`w-2 h-2 rounded-full ${
                user.role === 'ADMIN' ? 'bg-red-400' : 'bg-green-400'
              }`} />
              <span className="text-xs font-medium text-slate-300">{user.role}</span>
            </div>
          )}
          
          {/* Logout Button */}
          <button
            onClick={() => useAuthStore.getState().logout()}
            className="flex items-center space-x-2 text-slate-400 hover:text-red-400 transition-colors p-2 rounded-lg hover:bg-red-500/10"
            title="Sign out"
          >
            <LogOut className="w-4 h-4" />
            <span className="hidden sm:inline text-sm">Logout</span>
          </button>
        </div>
      </div>
    </header>
  )
}