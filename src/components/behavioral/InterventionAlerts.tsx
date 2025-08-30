import React, { useState, useEffect } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { 
  AlertTriangle, Brain, Heart, Clock, Target, X, CheckCircle,
  Pause, Coffee, TrendingDown, Shield, Lightbulb, Activity,
  MessageCircle, Users, Star, Bell, Volume2, VolumeX
} from 'lucide-react'

export interface InterventionAlert {
  id: string
  type: 'emotional' | 'behavioral' | 'risk' | 'performance' | 'educational'
  severity: 'low' | 'medium' | 'high' | 'critical'
  title: string
  description: string
  trigger: string
  recommendation: string
  actions: InterventionAction[]
  timestamp: Date
  dismissed?: boolean
  acknowledged?: boolean
  effectiveness?: number // 0-100, post-intervention feedback
}

export interface InterventionAction {
  id: string
  type: 'pause' | 'reduce-position' | 'review-strategy' | 'take-break' | 'seek-help' | 'education'
  label: string
  description: string
  duration?: number // in minutes
  impact: 'immediate' | 'short-term' | 'long-term'
}

interface InterventionAlertsProps {
  alerts: InterventionAlert[]
  onAlertDismiss: (alertId: string) => void
  onAlertAcknowledge: (alertId: string) => void
  onActionTaken: (alertId: string, actionId: string) => void
  soundEnabled?: boolean
  onSoundToggle?: () => void
  className?: string
}

const alertTypeConfig = {
  emotional: {
    icon: Heart,
    color: 'from-red-500 to-pink-500',
    bgColor: 'bg-red-500/10',
    borderColor: 'border-red-400/30',
    textColor: 'text-red-400'
  },
  behavioral: {
    icon: Brain,
    color: 'from-purple-500 to-blue-500',
    bgColor: 'bg-purple-500/10',
    borderColor: 'border-purple-400/30',
    textColor: 'text-purple-400'
  },
  risk: {
    icon: Shield,
    color: 'from-orange-500 to-red-500',
    bgColor: 'bg-orange-500/10',
    borderColor: 'border-orange-400/30',
    textColor: 'text-orange-400'
  },
  performance: {
    icon: TrendingDown,
    color: 'from-yellow-500 to-orange-500',
    bgColor: 'bg-yellow-500/10',
    borderColor: 'border-yellow-400/30',
    textColor: 'text-yellow-400'
  },
  educational: {
    icon: Lightbulb,
    color: 'from-blue-500 to-cyan-500',
    bgColor: 'bg-blue-500/10',
    borderColor: 'border-blue-400/30',
    textColor: 'text-blue-400'
  }
}

const severityConfig = {
  low: { priority: 1, pulse: false, autoHide: 30000 },
  medium: { priority: 2, pulse: true, autoHide: 45000 },
  high: { priority: 3, pulse: true, autoHide: 60000 },
  critical: { priority: 4, pulse: true, autoHide: null }
}

const actionConfig = {
  pause: {
    icon: Pause,
    color: 'bg-blue-600 hover:bg-blue-700',
    description: 'Take a moment to pause and reflect'
  },
  'reduce-position': {
    icon: TrendingDown,
    color: 'bg-orange-600 hover:bg-orange-700',
    description: 'Consider reducing your position size'
  },
  'review-strategy': {
    icon: Target,
    color: 'bg-purple-600 hover:bg-purple-700',
    description: 'Review your trading strategy and rules'
  },
  'take-break': {
    icon: Coffee,
    color: 'bg-green-600 hover:bg-green-700',
    description: 'Take a break from trading'
  },
  'seek-help': {
    icon: Users,
    color: 'bg-pink-600 hover:bg-pink-700',
    description: 'Connect with trading community or mentor'
  },
  education: {
    icon: Lightbulb,
    color: 'bg-cyan-600 hover:bg-cyan-700',
    description: 'Learn more about this topic'
  }
}

export const InterventionAlerts: React.FC<InterventionAlertsProps> = ({
  alerts,
  onAlertDismiss,
  onAlertAcknowledge,
  onActionTaken,
  soundEnabled = true,
  onSoundToggle,
  className = ''
}) => {
  const [selectedAlert, setSelectedAlert] = useState<InterventionAlert | null>(null)
  const [recentAlerts, setRecentAlerts] = useState<InterventionAlert[]>([])

  // Sort alerts by severity and timestamp
  const sortedAlerts = [...alerts]
    .filter(alert => !alert.dismissed)
    .sort((a, b) => {
      const severityDiff = severityConfig[b.severity].priority - severityConfig[a.severity].priority
      if (severityDiff !== 0) return severityDiff
      return b.timestamp.getTime() - a.timestamp.getTime()
    })

  // Play sound for new critical alerts
  useEffect(() => {
    const newCriticalAlerts = alerts.filter(alert => 
      alert.severity === 'critical' && 
      !alert.dismissed && 
      !recentAlerts.some(recent => recent.id === alert.id)
    )

    if (newCriticalAlerts.length > 0 && soundEnabled) {
      // Play alert sound (would integrate with actual audio API)
      console.log('Playing critical alert sound')
    }

    setRecentAlerts(alerts)
  }, [alerts, soundEnabled, recentAlerts])

  // Auto-hide alerts based on severity
  useEffect(() => {
    alerts.forEach(alert => {
      const config = severityConfig[alert.severity]
      if (config.autoHide && !alert.dismissed) {
        setTimeout(() => {
          onAlertDismiss(alert.id)
        }, config.autoHide)
      }
    })
  }, [alerts, onAlertDismiss])

  const handleActionClick = (alert: InterventionAlert, action: InterventionAction) => {
    onActionTaken(alert.id, action.id)
    
    if (action.type === 'pause' || action.type === 'take-break') {
      // Could integrate with actual timer/session management
      console.log(`Starting ${action.type} for ${action.duration} minutes`)
    }
  }

  if (sortedAlerts.length === 0) {
    return (
      <div className={`glass-card rounded-xl p-6 text-center ${className}`}>
        <CheckCircle className="w-12 h-12 mx-auto mb-3 text-green-400" />
        <h3 className="font-semibold text-white mb-1">All Clear</h3>
        <p className="text-slate-400 text-sm">No active intervention alerts</p>
      </div>
    )
  }

  return (
    <div className={`space-y-4 ${className}`}>
      {/* Header with controls */}
      <div className="flex items-center justify-between">
        <div className="flex items-center space-x-3">
          <AlertTriangle className="w-6 h-6 text-orange-400" />
          <div>
            <h3 className="font-semibold text-white">AI Interventions</h3>
            <p className="text-sm text-slate-400">{sortedAlerts.length} active alert{sortedAlerts.length !== 1 ? 's' : ''}</p>
          </div>
        </div>

        {onSoundToggle && (
          <button
            onClick={onSoundToggle}
            className={`p-2 rounded-lg transition-colors ${
              soundEnabled
                ? 'bg-blue-500/20 text-blue-400 hover:bg-blue-500/30'
                : 'bg-slate-700/50 text-slate-400 hover:bg-slate-600/50'
            }`}
          >
            {soundEnabled ? <Volume2 className="w-4 h-4" /> : <VolumeX className="w-4 h-4" />}
          </button>
        )}
      </div>

      {/* Alert list */}
      <div className="space-y-3">
        <AnimatePresence>
          {sortedAlerts.map((alert, index) => {
            const config = alertTypeConfig[alert.type]
            const IconComponent = config.icon
            const isPulse = severityConfig[alert.severity].pulse

            return (
              <motion.div
                key={alert.id}
                initial={{ opacity: 0, y: -20, scale: 0.95 }}
                animate={{ opacity: 1, y: 0, scale: 1 }}
                exit={{ opacity: 0, y: -20, scale: 0.95 }}
                transition={{ delay: index * 0.1 }}
                className={`glass-card rounded-xl p-4 border ${config.borderColor} ${config.bgColor} ${
                  isPulse ? 'animate-pulse' : ''
                }`}
              >
                <div className="flex items-start space-x-4">
                  {/* Alert icon */}
                  <div className={`w-10 h-10 rounded-lg bg-gradient-to-r ${config.color} flex items-center justify-center flex-shrink-0`}>
                    <IconComponent className="w-5 h-5 text-white" />
                  </div>

                  {/* Alert content */}
                  <div className="flex-1 min-w-0">
                    <div className="flex items-start justify-between mb-2">
                      <div>
                        <h4 className="font-semibold text-white">{alert.title}</h4>
                        <div className="flex items-center space-x-2 mt-1">
                          <span className={`px-2 py-1 rounded-lg text-xs font-medium capitalize ${
                            alert.severity === 'critical' ? 'bg-red-500/20 text-red-400' :
                            alert.severity === 'high' ? 'bg-orange-500/20 text-orange-400' :
                            alert.severity === 'medium' ? 'bg-yellow-500/20 text-yellow-400' :
                            'bg-blue-500/20 text-blue-400'
                          }`}>
                            {alert.severity}
                          </span>
                          <span className="text-xs text-slate-500">
                            {alert.timestamp.toLocaleTimeString()}
                          </span>
                        </div>
                      </div>

                      <button
                        onClick={() => onAlertDismiss(alert.id)}
                        className="p-1 hover:bg-slate-700/50 rounded text-slate-400 hover:text-white transition-colors"
                      >
                        <X className="w-4 h-4" />
                      </button>
                    </div>

                    <p className="text-slate-300 text-sm mb-3">{alert.description}</p>

                    {/* Trigger information */}
                    <div className="text-xs text-slate-400 mb-3">
                      <span className="font-medium">Triggered by:</span> {alert.trigger}
                    </div>

                    {/* AI Recommendation */}
                    <div className="p-3 bg-slate-800/50 rounded-lg mb-4">
                      <div className="flex items-start space-x-2">
                        <Brain className="w-4 h-4 text-blue-400 flex-shrink-0 mt-0.5" />
                        <div>
                          <p className="text-sm font-medium text-blue-400 mb-1">AI Recommendation</p>
                          <p className="text-sm text-slate-300">{alert.recommendation}</p>
                        </div>
                      </div>
                    </div>

                    {/* Quick actions */}
                    <div className="flex flex-wrap gap-2 mb-3">
                      {alert.actions.slice(0, 3).map((action) => {
                        const actionConfigItem = actionConfig[action.type]
                        const ActionIcon = actionConfigItem.icon
                        
                        return (
                          <motion.button
                            key={action.id}
                            whileHover={{ scale: 1.02 }}
                            whileTap={{ scale: 0.98 }}
                            onClick={() => handleActionClick(alert, action)}
                            className={`flex items-center space-x-2 px-3 py-2 rounded-lg text-white text-sm font-medium transition-colors ${actionConfigItem.color}`}
                          >
                            <ActionIcon className="w-4 h-4" />
                            <span>{action.label}</span>
                          </motion.button>
                        )
                      })}

                      {alert.actions.length > 3 && (
                        <button
                          onClick={() => setSelectedAlert(alert)}
                          className="px-3 py-2 bg-slate-700/50 hover:bg-slate-600/50 rounded-lg text-slate-300 text-sm font-medium transition-colors"
                        >
                          +{alert.actions.length - 3} more
                        </button>
                      )}
                    </div>

                    {/* Acknowledge button */}
                    {!alert.acknowledged && (
                      <button
                        onClick={() => onAlertAcknowledge(alert.id)}
                        className="w-full flex items-center justify-center space-x-2 py-2 border border-slate-600/50 rounded-lg text-slate-300 hover:text-white hover:border-slate-500/50 transition-colors"
                      >
                        <CheckCircle className="w-4 h-4" />
                        <span className="text-sm">I understand</span>
                      </button>
                    )}
                  </div>
                </div>
              </motion.div>
            )
          })}
        </AnimatePresence>
      </div>

      {/* Detailed alert modal */}
      <AnimatePresence>
        {selectedAlert && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4"
            onClick={() => setSelectedAlert(null)}
          >
            <motion.div
              initial={{ opacity: 0, scale: 0.9, y: 20 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.9, y: 20 }}
              className="glass-card rounded-2xl p-6 max-w-2xl w-full max-h-[80vh] overflow-y-auto"
              onClick={(e) => e.stopPropagation()}
            >
              <div className="flex items-center justify-between mb-6">
                <h3 className="text-2xl font-bold text-white">{selectedAlert.title}</h3>
                <button
                  onClick={() => setSelectedAlert(null)}
                  className="p-2 hover:bg-slate-700/50 rounded-lg text-slate-400 hover:text-white transition-colors"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>

              <div className="space-y-6">
                <div>
                  <h4 className="font-semibold text-white mb-2">All Available Actions</h4>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                    {selectedAlert.actions.map((action) => {
                      const actionConfigItem = actionConfig[action.type]
                      const ActionIcon = actionConfigItem.icon
                      
                      return (
                        <motion.button
                          key={action.id}
                          whileHover={{ scale: 1.02 }}
                          whileTap={{ scale: 0.98 }}
                          onClick={() => {
                            handleActionClick(selectedAlert, action)
                            setSelectedAlert(null)
                          }}
                          className={`p-4 rounded-xl text-left transition-colors ${actionConfigItem.color}`}
                        >
                          <div className="flex items-start space-x-3">
                            <ActionIcon className="w-5 h-5 text-white flex-shrink-0 mt-0.5" />
                            <div>
                              <h5 className="font-medium text-white mb-1">{action.label}</h5>
                              <p className="text-sm text-white/80 mb-2">{action.description}</p>
                              <div className="flex items-center space-x-3 text-xs text-white/60">
                                <span className="capitalize">Impact: {action.impact}</span>
                                {action.duration && (
                                  <span>{action.duration} minutes</span>
                                )}
                              </div>
                            </div>
                          </div>
                        </motion.button>
                      )
                    })}
                  </div>
                </div>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  )
}

export default InterventionAlerts