import React from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { Wifi, WifiOff, AlertCircle, RotateCcw, CheckCircle2 } from 'lucide-react'
import { ConnectionStatus as Status } from '../../services/WebSocketService'
import { cn } from '../../lib/utils'

interface ConnectionStatusProps {
  status: Status
  lastUpdate?: Date | null
  onReconnect?: () => void
  showDetails?: boolean
  className?: string
}

export function ConnectionStatus({ 
  status, 
  lastUpdate, 
  onReconnect,
  showDetails = false,
  className 
}: ConnectionStatusProps) {
  const getStatusConfig = (status: Status) => {
    switch (status) {
      case 'connected':
        return {
          icon: CheckCircle2,
          color: 'text-green-400',
          bgColor: 'bg-green-500/20',
          borderColor: 'border-green-500/30',
          message: 'Connected',
          description: 'Real-time data streaming active'
        }
      case 'connecting':
        return {
          icon: RotateCcw,
          color: 'text-blue-400',
          bgColor: 'bg-blue-500/20',
          borderColor: 'border-blue-500/30',
          message: 'Connecting',
          description: 'Establishing connection...'
        }
      case 'reconnecting':
        return {
          icon: RotateCcw,
          color: 'text-yellow-400',
          bgColor: 'bg-yellow-500/20',
          borderColor: 'border-yellow-500/30',
          message: 'Reconnecting',
          description: 'Attempting to restore connection'
        }
      case 'disconnected':
        return {
          icon: WifiOff,
          color: 'text-orange-400',
          bgColor: 'bg-orange-500/20',
          borderColor: 'border-orange-500/30',
          message: 'Disconnected',
          description: 'Real-time updates paused'
        }
      case 'error':
        return {
          icon: AlertCircle,
          color: 'text-red-400',
          bgColor: 'bg-red-500/20',
          borderColor: 'border-red-500/30',
          message: 'Connection Error',
          description: 'Unable to connect to server'
        }
      default:
        return {
          icon: WifiOff,
          color: 'text-slate-400',
          bgColor: 'bg-slate-500/20',
          borderColor: 'border-slate-500/30',
          message: 'Unknown',
          description: 'Connection status unknown'
        }
    }
  }

  const config = getStatusConfig(status)
  const Icon = config.icon

  const formatLastUpdate = (date: Date) => {
    const now = new Date()
    const diff = now.getTime() - date.getTime()
    
    if (diff < 1000) return 'just now'
    if (diff < 60000) return `${Math.floor(diff / 1000)}s ago`
    if (diff < 3600000) return `${Math.floor(diff / 60000)}m ago`
    return date.toLocaleTimeString()
  }

  if (!showDetails) {
    // Compact status indicator
    return (
      <motion.div
        className={cn(
          "flex items-center space-x-2 px-3 py-2 rounded-lg border",
          config.bgColor,
          config.borderColor,
          className
        )}
        animate={{ scale: status === 'connecting' || status === 'reconnecting' ? [1, 1.05, 1] : 1 }}
        transition={{ duration: 1, repeat: status === 'connecting' || status === 'reconnecting' ? Infinity : 0 }}
      >
        <Icon 
          className={cn(
            "w-4 h-4",
            config.color,
            (status === 'connecting' || status === 'reconnecting') && "animate-spin"
          )} 
        />
        <span className={cn("text-sm font-medium", config.color)}>
          {config.message}
        </span>
        {lastUpdate && (
          <span className="text-xs text-slate-400">
            • {formatLastUpdate(lastUpdate)}
          </span>
        )}
      </motion.div>
    )
  }

  // Detailed status display
  return (
    <AnimatePresence>
      <motion.div
        initial={{ opacity: 0, y: -10 }}
        animate={{ opacity: 1, y: 0 }}
        exit={{ opacity: 0, y: -10 }}
        className={cn(
          "glass-card p-4 rounded-xl border",
          config.borderColor,
          className
        )}
      >
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <div className={cn(
              "p-2 rounded-lg",
              config.bgColor
            )}>
              <Icon 
                className={cn(
                  "w-5 h-5",
                  config.color,
                  (status === 'connecting' || status === 'reconnecting') && "animate-spin"
                )} 
              />
            </div>
            <div>
              <div className={cn("font-medium", config.color)}>
                {config.message}
              </div>
              <div className="text-sm text-slate-400">
                {config.description}
              </div>
            </div>
          </div>

          {/* Reconnect button for error states */}
          {(status === 'error' || status === 'disconnected') && onReconnect && (
            <button
              onClick={onReconnect}
              className="cyber-button-sm px-3 py-2 text-xs rounded-lg flex items-center space-x-1"
            >
              <RotateCcw className="w-3 h-3" />
              <span>Retry</span>
            </button>
          )}
        </div>

        {/* Last update timestamp */}
        {lastUpdate && (
          <div className="mt-3 pt-3 border-t border-slate-700/50">
            <div className="flex items-center justify-between text-xs text-slate-400">
              <span>Last Update</span>
              <span>{formatLastUpdate(lastUpdate)}</span>
            </div>
          </div>
        )}

        {/* Connection quality indicator */}
        {status === 'connected' && (
          <div className="mt-3 pt-3 border-t border-slate-700/50">
            <div className="flex items-center justify-between text-xs">
              <span className="text-slate-400">Signal Strength</span>
              <div className="flex items-center space-x-1">
                {[1, 2, 3, 4].map((bar) => (
                  <div
                    key={bar}
                    className={cn(
                      "w-1 rounded-full",
                      bar === 1 ? "h-2" : bar === 2 ? "h-3" : bar === 3 ? "h-4" : "h-5",
                      "bg-green-400"
                    )}
                  />
                ))}
                <span className="text-green-400 ml-2">Excellent</span>
              </div>
            </div>
          </div>
        )}
      </motion.div>
    </AnimatePresence>
  )
}

// Mini connection indicator for status bar
export function ConnectionIndicator({ status }: { status: Status }) {
  const getColor = (status: Status) => {
    switch (status) {
      case 'connected': return 'bg-green-400'
      case 'connecting': 
      case 'reconnecting': return 'bg-yellow-400'
      case 'disconnected': return 'bg-orange-400'
      case 'error': return 'bg-red-400'
      default: return 'bg-slate-400'
    }
  }

  return (
    <div className="flex items-center space-x-2">
      <motion.div
        className={cn(
          "w-2 h-2 rounded-full",
          getColor(status)
        )}
        animate={{
          scale: status === 'connecting' || status === 'reconnecting' ? [1, 1.5, 1] : 1,
          opacity: status === 'connecting' || status === 'reconnecting' ? [1, 0.5, 1] : 1
        }}
        transition={{ 
          duration: 1, 
          repeat: status === 'connecting' || status === 'reconnecting' ? Infinity : 0 
        }}
      />
      <span className="text-xs text-slate-400 capitalize">{status}</span>
    </div>
  )
}