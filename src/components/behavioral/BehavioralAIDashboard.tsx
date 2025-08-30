import React, { useState, useEffect } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { 
  Brain, Heart, TrendingUp, AlertTriangle, Settings, Eye,
  Activity, Target, Clock, BarChart3, Shield, Users,
  ChevronRight, Lightbulb, Zap, Coffee, Award, X
} from 'lucide-react'
import { EmotionTracker, type EmotionState, type EmotionHistory } from './EmotionTracker'
import { BehavioralInsights, type TradingPattern, type BehavioralMetric } from './BehavioralInsights'
import { InterventionAlerts, type InterventionAlert } from './InterventionAlerts'

interface BehavioralAIDashboardProps {
  className?: string
}

export const BehavioralAIDashboard: React.FC<BehavioralAIDashboardProps> = ({
  className = ''
}) => {
  const [activeTab, setActiveTab] = useState<'overview' | 'emotions' | 'patterns' | 'interventions'>('overview')
  const [soundEnabled, setSoundEnabled] = useState(true)
  const [showSettings, setShowSettings] = useState(false)
  
  // Mock current emotion state
  const [currentEmotion] = useState<EmotionState>({
    primary: 'confidence',
    intensity: 72,
    timestamp: new Date(),
    triggers: ['consistent profits', 'following strategy'],
    confidence: 89
  })

  // Mock emotion history
  const [emotionHistory] = useState<EmotionHistory[]>([
    { timestamp: new Date(Date.now() - 3600000), emotion: 'anxiety', intensity: 65, tradingDecision: 'hold', outcome: 'positive' },
    { timestamp: new Date(Date.now() - 7200000), emotion: 'confidence', intensity: 80, tradingDecision: 'buy', outcome: 'positive' },
    { timestamp: new Date(Date.now() - 10800000), emotion: 'greed', intensity: 45, tradingDecision: 'sell', outcome: 'negative' },
    { timestamp: new Date(Date.now() - 14400000), emotion: 'fear', intensity: 85, tradingDecision: 'sell', outcome: 'neutral' },
    { timestamp: new Date(Date.now() - 18000000), emotion: 'neutral', intensity: 30, tradingDecision: 'hold', outcome: 'positive' }
  ])

  // Mock trading patterns
  const [tradingPatterns] = useState<TradingPattern[]>([
    {
      id: '1',
      type: 'disciplined',
      frequency: 8,
      impact: 'positive',
      description: 'Following predefined entry and exit rules consistently',
      examples: ['Using stop-losses', 'Position sizing rules', 'Risk management'],
      recommendation: 'Continue maintaining this excellent discipline',
      severity: 'low'
    },
    {
      id: '2',
      type: 'fomo',
      frequency: 3,
      impact: 'negative',
      description: 'Fear of missing out leading to impulsive entries',
      examples: ['Chasing breakouts', 'Entering without confirmation'],
      recommendation: 'Use alerts instead of constant monitoring',
      severity: 'medium'
    }
  ])

  // Mock behavioral metrics
  const [behavioralMetrics] = useState<BehavioralMetric[]>([
    {
      id: '1',
      name: 'Emotional Control',
      value: 78,
      maxValue: 100,
      trend: 'improving',
      description: 'Your ability to manage emotions during trading',
      category: 'emotional'
    },
    {
      id: '2',
      name: 'Discipline Score',
      value: 85,
      maxValue: 100,
      trend: 'stable',
      description: 'How well you follow your trading rules',
      category: 'behavioral'
    },
    {
      id: '3',
      name: 'Risk Awareness',
      value: 92,
      maxValue: 100,
      trend: 'improving',
      description: 'Your understanding and management of trading risks',
      category: 'risk'
    }
  ])

  // Mock intervention alerts
  const [interventionAlerts] = useState<InterventionAlert[]>([
    {
      id: '1',
      type: 'behavioral',
      severity: 'medium',
      title: 'Pattern Alert: Overtrading Detected',
      description: 'Your trading frequency has increased 40% this week, which may indicate emotional decision-making.',
      trigger: 'Increased trade frequency',
      recommendation: 'Consider taking a 30-minute break and reviewing your trading plan.',
      actions: [
        {
          id: '1',
          type: 'pause',
          label: 'Take 30min Break',
          description: 'Step away from trading for 30 minutes',
          duration: 30,
          impact: 'immediate'
        },
        {
          id: '2',
          type: 'review-strategy',
          label: 'Review Plan',
          description: 'Review your trading strategy and rules',
          impact: 'short-term'
        }
      ],
      timestamp: new Date(),
      acknowledged: false
    }
  ])

  const handleAlertDismiss = (alertId: string) => {
    console.log('Dismissing alert:', alertId)
  }

  const handleAlertAcknowledge = (alertId: string) => {
    console.log('Acknowledging alert:', alertId)
  }

  const handleActionTaken = (alertId: string, actionId: string) => {
    console.log('Action taken:', { alertId, actionId })
  }

  const handleEmotionUpdate = (emotion: EmotionState) => {
    console.log('Emotion updated:', emotion)
  }

  const tabs = [
    {
      id: 'overview',
      label: 'Overview',
      icon: Eye,
      description: 'High-level behavioral insights'
    },
    {
      id: 'emotions',
      label: 'Emotions',
      icon: Heart,
      description: 'Real-time emotion tracking'
    },
    {
      id: 'patterns',
      label: 'Patterns',
      icon: Brain,
      description: 'Trading behavior analysis'
    },
    {
      id: 'interventions',
      label: 'Coaching',
      icon: Lightbulb,
      description: 'AI coaching and alerts'
    }
  ]

  const overviewStats = [
    {
      label: 'Emotional Control',
      value: '78%',
      change: '+5%',
      trend: 'up',
      icon: Heart,
      color: 'text-purple-400'
    },
    {
      label: 'Discipline Score',
      value: '85%',
      change: '±0%',
      trend: 'stable',
      icon: Target,
      color: 'text-green-400'
    },
    {
      label: 'Risk Awareness',
      value: '92%',
      change: '+8%',
      trend: 'up',
      icon: Shield,
      color: 'text-blue-400'
    },
    {
      label: 'Pattern Recognition',
      value: '67%',
      change: '+12%',
      trend: 'up',
      icon: Activity,
      color: 'text-cyan-400'
    }
  ]

  return (
    <div className={`min-h-screen bg-slate-950 ${className}`}>
      {/* Header */}
      <div className="sticky top-0 z-10 glass-card-dark border-b border-slate-700/50 backdrop-blur-xl">
        <div className="container mx-auto px-6 py-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-4">
              <div className="w-12 h-12 rounded-xl bg-gradient-to-r from-purple-500 to-pink-500 flex items-center justify-center">
                <Brain className="w-6 h-6 text-white" />
              </div>
              <div>
                <h1 className="text-2xl font-bold gradient-text">Behavioral AI Dashboard</h1>
                <p className="text-slate-400">AI-powered trading psychology insights</p>
              </div>
            </div>

            <div className="flex items-center space-x-3">
              <div className="flex items-center space-x-2 px-3 py-2 bg-slate-800/50 rounded-lg border border-slate-600/30">
                <div className="w-2 h-2 bg-green-400 rounded-full animate-pulse" />
                <span className="text-sm text-slate-300">AI Active</span>
              </div>
              <button 
                onClick={() => setShowSettings(true)}
                className="p-2 hover:bg-slate-700/50 rounded-lg text-slate-400 hover:text-white transition-colors"
                title="Settings"
              >
                <Settings className="w-5 h-5" />
              </button>
            </div>
          </div>

          {/* Tab Navigation */}
          <div className="flex space-x-1 mt-6 bg-slate-800/30 p-1 rounded-xl">
            {tabs.map((tab) => {
              const Icon = tab.icon
              const isActive = activeTab === tab.id
              
              return (
                <button
                  key={tab.id}
                  onClick={() => setActiveTab(tab.id as any)}
                  className={`flex items-center space-x-2 px-4 py-3 rounded-lg transition-all font-medium text-sm relative ${
                    isActive
                      ? 'bg-slate-700/80 text-white shadow-lg'
                      : 'text-slate-400 hover:text-white hover:bg-slate-700/30'
                  }`}
                >
                  <Icon className="w-4 h-4" />
                  <span>{tab.label}</span>
                  {isActive && (
                    <motion.div
                      layoutId="activeTab"
                      className="absolute inset-0 bg-gradient-to-r from-purple-500/10 to-pink-500/10 rounded-lg border border-purple-400/30"
                    />
                  )}
                </button>
              )
            })}
          </div>
        </div>
      </div>

      {/* Content */}
      <div className="container mx-auto px-6 py-8">
        {/* Overview Tab */}
        {activeTab === 'overview' && (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className="space-y-8"
          >
            {/* Stats Grid */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
              {overviewStats.map((stat) => {
                const Icon = stat.icon
                return (
                  <motion.div
                    key={stat.label}
                    whileHover={{ scale: 1.02 }}
                    className="glass-card rounded-xl p-6"
                  >
                    <div className="flex items-center justify-between mb-4">
                      <div className={`w-10 h-10 rounded-lg bg-slate-800/50 flex items-center justify-center ${stat.color}`}>
                        <Icon className="w-5 h-5" />
                      </div>
                      <span className={`text-sm font-medium ${
                        stat.trend === 'up' ? 'text-green-400' :
                        stat.trend === 'down' ? 'text-red-400' : 'text-slate-400'
                      }`}>
                        {stat.change}
                      </span>
                    </div>
                    <h3 className="text-2xl font-bold text-white mb-1">{stat.value}</h3>
                    <p className="text-slate-400 text-sm">{stat.label}</p>
                  </motion.div>
                )
              })}
            </div>

            {/* Quick Insights */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
              {/* Current State */}
              <div className="glass-card rounded-xl p-6">
                <h3 className="text-xl font-semibold text-white mb-4 flex items-center space-x-2">
                  <Activity className="w-5 h-5 text-purple-400" />
                  <span>Current State</span>
                </h3>
                <div className="space-y-4">
                  <div className="flex items-center space-x-4">
                    <div className="w-12 h-12 rounded-xl bg-gradient-to-r from-green-500 to-green-600 flex items-center justify-center">
                      <Heart className="w-6 h-6 text-white" />
                    </div>
                    <div className="flex-1">
                      <h4 className="font-semibold text-white">{currentEmotion.primary.charAt(0).toUpperCase() + currentEmotion.primary.slice(1)}</h4>
                      <p className="text-sm text-slate-400">Intensity: {currentEmotion.intensity}%</p>
                    </div>
                    <div className="text-right">
                      <span className="text-green-400 font-medium">Optimal</span>
                      <p className="text-xs text-slate-400">for trading</p>
                    </div>
                  </div>
                  
                  <div className="p-3 bg-green-500/10 border border-green-400/30 rounded-lg">
                    <p className="text-sm text-slate-300">
                      Great mindset! Your confidence level is balanced and you're following your strategy well.
                    </p>
                  </div>
                </div>
              </div>

              {/* Recent Patterns */}
              <div className="glass-card rounded-xl p-6">
                <h3 className="text-xl font-semibold text-white mb-4 flex items-center space-x-2">
                  <TrendingUp className="w-5 h-5 text-cyan-400" />
                  <span>Recent Patterns</span>
                </h3>
                <div className="space-y-3">
                  {tradingPatterns.slice(0, 2).map((pattern) => (
                    <div key={pattern.id} className="p-3 bg-slate-800/30 rounded-lg">
                      <div className="flex items-center justify-between mb-2">
                        <span className="font-medium text-white capitalize">
                          {pattern.type.replace('-', ' ')}
                        </span>
                        <span className={`text-xs px-2 py-1 rounded ${
                          pattern.impact === 'positive' ? 'bg-green-500/20 text-green-400' :
                          pattern.impact === 'negative' ? 'bg-red-500/20 text-red-400' :
                          'bg-slate-500/20 text-slate-400'
                        }`}>
                          {pattern.impact}
                        </span>
                      </div>
                      <p className="text-sm text-slate-400">
                        {pattern.frequency}x this week
                      </p>
                    </div>
                  ))}
                </div>
              </div>
            </div>

            {/* Active Alerts Preview */}
            {interventionAlerts.length > 0 && (
              <div className="glass-card rounded-xl p-6">
                <div className="flex items-center justify-between mb-4">
                  <h3 className="text-xl font-semibold text-white flex items-center space-x-2">
                    <AlertTriangle className="w-5 h-5 text-orange-400" />
                    <span>Active Coaching Alerts</span>
                  </h3>
                  <button 
                    onClick={() => setActiveTab('interventions')}
                    className="text-blue-400 hover:text-blue-300 text-sm flex items-center space-x-1"
                  >
                    <span>View All</span>
                    <ChevronRight className="w-4 h-4" />
                  </button>
                </div>
                <div className="space-y-3">
                  {interventionAlerts.slice(0, 2).map((alert) => (
                    <div key={alert.id} className="p-4 bg-orange-500/10 border border-orange-400/30 rounded-lg">
                      <h4 className="font-medium text-white mb-1">{alert.title}</h4>
                      <p className="text-sm text-slate-300 mb-2">{alert.description}</p>
                      <div className="flex items-center space-x-2 text-xs text-orange-400">
                        <Clock className="w-3 h-3" />
                        <span>{alert.timestamp.toLocaleTimeString()}</span>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </motion.div>
        )}

        {/* Emotions Tab */}
        {activeTab === 'emotions' && (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
          >
            <EmotionTracker
              currentEmotion={currentEmotion}
              emotionHistory={emotionHistory}
              onEmotionUpdate={handleEmotionUpdate}
            />
          </motion.div>
        )}

        {/* Patterns Tab */}
        {activeTab === 'patterns' && (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
          >
            <BehavioralInsights
              tradingPatterns={tradingPatterns}
              behavioralMetrics={behavioralMetrics}
            />
          </motion.div>
        )}

        {/* Interventions Tab */}
        {activeTab === 'interventions' && (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
          >
            <InterventionAlerts
              alerts={interventionAlerts}
              onAlertDismiss={handleAlertDismiss}
              onAlertAcknowledge={handleAlertAcknowledge}
              onActionTaken={handleActionTaken}
              soundEnabled={soundEnabled}
              onSoundToggle={() => setSoundEnabled(!soundEnabled)}
            />
          </motion.div>
        )}
      </div>

      {/* Settings Modal */}
      <AnimatePresence>
        {showSettings && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4"
            onClick={() => setShowSettings(false)}
          >
            <motion.div
              initial={{ opacity: 0, scale: 0.9, y: 20 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.9, y: 20 }}
              className="glass-card rounded-2xl p-6 max-w-md w-full"
              onClick={(e) => e.stopPropagation()}
            >
              <div className="flex items-center justify-between mb-6">
                <h3 className="text-xl font-bold text-white">AI Settings</h3>
                <button
                  onClick={() => setShowSettings(false)}
                  className="p-2 hover:bg-slate-700/50 rounded-lg text-slate-400 hover:text-white transition-colors"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>

              <div className="space-y-6">
                {/* Sound Settings */}
                <div>
                  <h4 className="font-semibold text-white mb-3">Notifications</h4>
                  <div className="flex items-center justify-between p-3 bg-slate-800/30 rounded-lg">
                    <div>
                      <p className="font-medium text-white">Sound Alerts</p>
                      <p className="text-sm text-slate-400">Play sounds for critical alerts</p>
                    </div>
                    <button
                      onClick={() => setSoundEnabled(!soundEnabled)}
                      className={`relative w-12 h-6 rounded-full transition-colors ${
                        soundEnabled ? 'bg-blue-500' : 'bg-slate-600'
                      }`}
                    >
                      <motion.div
                        className="absolute top-1 w-4 h-4 bg-white rounded-full"
                        animate={{
                          left: soundEnabled ? 28 : 4
                        }}
                        transition={{ type: 'spring', stiffness: 300, damping: 20 }}
                      />
                    </button>
                  </div>
                </div>

                {/* AI Sensitivity */}
                <div>
                  <h4 className="font-semibold text-white mb-3">AI Sensitivity</h4>
                  <div className="space-y-3">
                    <div className="flex items-center justify-between p-3 bg-slate-800/30 rounded-lg">
                      <div>
                        <p className="font-medium text-white">Emotion Tracking</p>
                        <p className="text-sm text-slate-400">How sensitive emotion detection is</p>
                      </div>
                      <select className="bg-slate-700 border border-slate-600 rounded-lg px-3 py-1 text-white text-sm">
                        <option value="low">Low</option>
                        <option value="medium" selected>Medium</option>
                        <option value="high">High</option>
                      </select>
                    </div>

                    <div className="flex items-center justify-between p-3 bg-slate-800/30 rounded-lg">
                      <div>
                        <p className="font-medium text-white">Intervention Frequency</p>
                        <p className="text-sm text-slate-400">How often AI provides coaching</p>
                      </div>
                      <select className="bg-slate-700 border border-slate-600 rounded-lg px-3 py-1 text-white text-sm">
                        <option value="minimal">Minimal</option>
                        <option value="balanced" selected>Balanced</option>
                        <option value="frequent">Frequent</option>
                      </select>
                    </div>
                  </div>
                </div>

                {/* Data & Privacy */}
                <div>
                  <h4 className="font-semibold text-white mb-3">Data & Privacy</h4>
                  <div className="space-y-3">
                    <button className="w-full text-left p-3 bg-slate-800/30 rounded-lg hover:bg-slate-700/30 transition-colors">
                      <p className="font-medium text-white">Export Data</p>
                      <p className="text-sm text-slate-400">Download your behavioral data</p>
                    </button>
                    
                    <button className="w-full text-left p-3 bg-slate-800/30 rounded-lg hover:bg-slate-700/30 transition-colors">
                      <p className="font-medium text-white">Reset Analytics</p>
                      <p className="text-sm text-slate-400">Clear all behavioral history</p>
                    </button>
                  </div>
                </div>
              </div>

              <div className="mt-6 pt-4 border-t border-slate-700/50">
                <button
                  onClick={() => setShowSettings(false)}
                  className="w-full py-2 px-4 bg-blue-600 hover:bg-blue-700 rounded-lg text-white font-medium transition-colors"
                >
                  Save Settings
                </button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  )
}

export default BehavioralAIDashboard