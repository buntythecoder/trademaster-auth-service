import React from 'react'
import { Link, useLocation } from 'react-router-dom'
import { useAuthStore } from '../../stores/auth.store'
import { ThemeToggle } from '../ui/ThemeToggle'
import { LogOut, User, TrendingUp, PieChart, BarChart3, Home } from 'lucide-react'

interface NavigationProps {
  title?: string
  showWelcome?: boolean
}

export function Navigation({ title = "TradeMaster", showWelcome = false }: NavigationProps) {
  const { user } = useAuthStore()
  const location = useLocation()
  
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
        label: 'Trade',
        icon: BarChart3,
        color: 'green'
      },
      {
        path: '/portfolio',
        label: 'Portfolio',
        icon: PieChart,
        color: 'orange'
      }
    ] : []),
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
        indigo: 'text-indigo-400 bg-indigo-500/20'
      }
      return `${baseClasses} ${colorMap[item.color as keyof typeof colorMap]}`
    }
    
    const hoverColorMap = {
      purple: 'text-slate-400 hover:text-purple-400 hover:bg-purple-500/10',
      cyan: 'text-slate-400 hover:text-cyan-400 hover:bg-cyan-500/10',
      green: 'text-slate-400 hover:text-green-400 hover:bg-green-500/10',
      orange: 'text-slate-400 hover:text-orange-400 hover:bg-orange-500/10',
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

        {/* Center Section - Navigation */}
        <nav className="hidden md:flex items-center space-x-1">
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