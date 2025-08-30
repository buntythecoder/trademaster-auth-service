import React, { useState, useEffect } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { 
  Heart, Brain, TrendingUp, TrendingDown, AlertTriangle,
  Smile, Frown, Meh, Target, Zap, Activity, Clock,
  ThermometerSun, BarChart3, Eye, User
} from 'lucide-react'

export interface EmotionState {
  primary: 'fear' | 'greed' | 'confidence' | 'anxiety' | 'neutral' | 'excitement' | 'frustration'
  intensity: number // 0-100
  timestamp: Date
  triggers: string[]
  confidence: number // AI confidence in detection
}

export interface EmotionHistory {
  timestamp: Date
  emotion: EmotionState['primary']
  intensity: number
  tradingDecision?: 'buy' | 'sell' | 'hold' | 'none'
  outcome?: 'positive' | 'negative' | 'neutral'
}

interface EmotionTrackerProps {
  currentEmotion?: EmotionState
  emotionHistory: EmotionHistory[]
  onEmotionUpdate?: (emotion: EmotionState) => void
  className?: string
}

const emotionConfig = {
  fear: {
    color: 'text-red-400 bg-red-500/20',
    icon: AlertTriangle,
    label: 'Fear',
    description: 'Loss aversion and risk avoidance',
    advice: 'Consider taking a break and reviewing your risk management'
  },
  greed: {
    color: 'text-yellow-400 bg-yellow-500/20',
    icon: TrendingUp,
    label: 'Greed',
    description: 'Excessive risk-taking and overconfidence',
    advice: 'Scale back position sizes and stick to your strategy'
  },
  confidence: {
    color: 'text-green-400 bg-green-500/20',
    icon: Target,
    label: 'Confidence',
    description: 'Balanced and focused mindset',
    advice: 'Great mindset! Maintain your disciplined approach'
  },
  anxiety: {
    color: 'text-orange-400 bg-orange-500/20',
    icon: Heart,
    label: 'Anxiety',
    description: 'Stress and overthinking patterns',
    advice: 'Practice deep breathing and review your trading plan'
  },
  neutral: {
    color: 'text-slate-400 bg-slate-500/20',
    icon: Meh,
    label: 'Neutral',
    description: 'Calm and analytical state',
    advice: 'Perfect state for making objective decisions'
  },
  excitement: {
    color: 'text-purple-400 bg-purple-500/20',
    icon: Zap,
    label: 'Excitement',
    description: 'High energy and optimism',
    advice: 'Channel this energy into careful analysis'
  },
  frustration: {
    color: 'text-red-400 bg-red-500/20',
    icon: Frown,
    label: 'Frustration',
    description: 'Impatience and negative emotions',
    advice: 'Step away from trading until you feel more balanced'
  }
}

export const EmotionTracker: React.FC<EmotionTrackerProps> = ({
  currentEmotion,
  emotionHistory,
  onEmotionUpdate,
  className = ''
}) => {
  const [showHistory, setShowHistory] = useState(false)
  const [selectedTimeframe, setSelectedTimeframe] = useState<'1h' | '24h' | '7d' | '30d'>('24h')

  const currentConfig = currentEmotion ? emotionConfig[currentEmotion.primary] : emotionConfig.neutral
  const IconComponent = currentConfig.icon

  // Calculate emotion trends
  const getEmotionTrend = () => {
    const recent = emotionHistory.slice(-10)
    const older = emotionHistory.slice(-20, -10)
    
    const recentAvg = recent.reduce((sum, h) => sum + h.intensity, 0) / recent.length || 0
    const olderAvg = older.reduce((sum, h) => sum + h.intensity, 0) / older.length || 0
    
    return recentAvg - olderAvg
  }

  const trend = getEmotionTrend()

  // Get emotion distribution for the selected timeframe
  const getEmotionDistribution = () => {
    const timeframes = {
      '1h': 3600000,
      '24h': 86400000,
      '7d': 604800000,
      '30d': 2592000000
    }
    
    const cutoff = Date.now() - timeframes[selectedTimeframe]
    const relevantHistory = emotionHistory.filter(h => h.timestamp.getTime() > cutoff)
    
    const distribution: Record<string, number> = {}
    relevantHistory.forEach(h => {
      distribution[h.emotion] = (distribution[h.emotion] || 0) + 1
    })
    
    const total = relevantHistory.length
    return Object.entries(distribution).map(([emotion, count]) => ({
      emotion: emotion as EmotionState['primary'],
      percentage: total > 0 ? (count / total) * 100 : 0,
      count
    })).sort((a, b) => b.percentage - a.percentage)
  }

  const emotionDistribution = getEmotionDistribution()

  return (
    <div className={`space-y-6 ${className}`}>
      {/* Current Emotion Display */}
      <div className="glass-card rounded-2xl p-6">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-xl font-semibold text-white flex items-center space-x-2">
            <Brain className="w-6 h-6" />
            <span>Current Emotional State</span>
          </h3>
          
          <div className="flex items-center space-x-2 text-sm text-slate-400">
            <Activity className="w-4 h-4" />
            <span>Live Analysis</span>
          </div>
        </div>

        {currentEmotion ? (
          <div className="space-y-4">
            {/* Emotion visualization */}
            <div className="flex items-center space-x-6">
              <motion.div
                animate={{ scale: [1, 1.1, 1] }}
                transition={{ duration: 2, repeat: Infinity }}
                className={`w-20 h-20 rounded-2xl flex items-center justify-center ${currentConfig.color}`}
              >
                <IconComponent className="w-10 h-10" />
              </motion.div>
              
              <div className="flex-1">
                <div className="flex items-center space-x-3 mb-2">
                  <h4 className="text-2xl font-bold text-white">{currentConfig.label}</h4>
                  <span className="text-lg font-medium text-slate-300">
                    {currentEmotion.intensity}%
                  </span>
                </div>
                
                <p className="text-slate-400 mb-3">{currentConfig.description}</p>
                
                {/* Intensity bar */}
                <div className="space-y-2">
                  <div className="flex justify-between text-sm text-slate-400">
                    <span>Intensity</span>
                    <span>{currentEmotion.intensity}% confident</span>
                  </div>
                  <div className="w-full bg-slate-700/50 rounded-full h-3">
                    <motion.div
                      initial={{ width: 0 }}
                      animate={{ width: `${currentEmotion.intensity}%` }}
                      className={`h-3 rounded-full bg-gradient-to-r ${
                        currentEmotion.intensity > 70 ? 'from-red-500 to-red-600' :
                        currentEmotion.intensity > 40 ? 'from-yellow-500 to-orange-500' :
                        'from-green-500 to-green-600'
                      }`}
                    />
                  </div>
                </div>
              </div>
            </div>

            {/* AI Advice */}
            <div className="p-4 bg-blue-500/10 border border-blue-400/30 rounded-xl">
              <div className="flex items-start space-x-3">
                <div className="w-8 h-8 rounded-lg bg-blue-500/20 flex items-center justify-center flex-shrink-0 mt-0.5">
                  <Brain className="w-4 h-4 text-blue-400" />
                </div>
                <div>
                  <h5 className="font-medium text-blue-400 mb-1">AI Coaching Advice</h5>
                  <p className="text-sm text-slate-300">{currentConfig.advice}</p>
                </div>
              </div>
            </div>

            {/* Triggers */}
            {currentEmotion.triggers.length > 0 && (
              <div className="space-y-2">
                <h5 className="font-medium text-white">Detected Triggers:</h5>
                <div className="flex flex-wrap gap-2">
                  {currentEmotion.triggers.map((trigger, index) => (
                    <span
                      key={index}
                      className="px-3 py-1 bg-slate-700/50 text-slate-300 rounded-lg text-sm"
                    >
                      {trigger}
                    </span>
                  ))}
                </div>
              </div>
            )}

            {/* Trend indicator */}
            <div className="flex items-center justify-between pt-3 border-t border-slate-700/50">
              <span className="text-sm text-slate-400">24h Trend</span>
              <div className="flex items-center space-x-2">
                {trend > 10 ? (
                  <>
                    <TrendingUp className="w-4 h-4 text-red-400" />
                    <span className="text-sm text-red-400">Intensifying</span>
                  </>
                ) : trend < -10 ? (
                  <>
                    <TrendingDown className="w-4 h-4 text-green-400" />
                    <span className="text-sm text-green-400">Calming</span>
                  </>
                ) : (
                  <>
                    <Activity className="w-4 h-4 text-slate-400" />
                    <span className="text-sm text-slate-400">Stable</span>
                  </>
                )}
              </div>
            </div>
          </div>
        ) : (
          <div className="text-center py-8">
            <Eye className="w-12 h-12 mx-auto mb-3 text-slate-500" />
            <p className="text-slate-400">No emotion data available</p>
            <p className="text-sm text-slate-500 mt-1">Start trading to enable emotion tracking</p>
          </div>
        )}
      </div>

      {/* Emotion Distribution */}
      <div className="glass-card rounded-2xl p-6">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-xl font-semibold text-white">Emotion Distribution</h3>
          
          <div className="flex items-center space-x-2">
            <select
              value={selectedTimeframe}
              onChange={(e) => setSelectedTimeframe(e.target.value as typeof selectedTimeframe)}
              className="bg-slate-800 border border-slate-600 rounded-lg px-3 py-2 text-white text-sm"
            >
              <option value="1h">Last Hour</option>
              <option value="24h">Last 24 Hours</option>
              <option value="7d">Last 7 Days</option>
              <option value="30d">Last 30 Days</option>
            </select>
          </div>
        </div>

        {emotionDistribution.length > 0 ? (
          <div className="space-y-3">
            {emotionDistribution.map(({ emotion, percentage, count }) => {
              const config = emotionConfig[emotion]
              const EmotionIcon = config.icon
              
              return (
                <div key={emotion} className="flex items-center space-x-4">
                  <div className={`w-8 h-8 rounded-lg flex items-center justify-center ${config.color}`}>
                    <EmotionIcon className="w-4 h-4" />
                  </div>
                  
                  <div className="flex-1">
                    <div className="flex items-center justify-between mb-1">
                      <span className="font-medium text-white">{config.label}</span>
                      <span className="text-sm text-slate-400">{count} times ({percentage.toFixed(1)}%)</span>
                    </div>
                    <div className="w-full bg-slate-700/50 rounded-full h-2">
                      <div 
                        className={`h-2 rounded-full bg-gradient-to-r ${
                          emotion === 'confidence' || emotion === 'neutral' ? 'from-green-500 to-green-600' :
                          emotion === 'fear' || emotion === 'frustration' ? 'from-red-500 to-red-600' :
                          'from-yellow-500 to-orange-500'
                        }`}
                        style={{ width: `${percentage}%` }}
                      />
                    </div>
                  </div>
                </div>
              )
            })}
          </div>
        ) : (
          <div className="text-center py-8">
            <BarChart3 className="w-12 h-12 mx-auto mb-3 text-slate-500" />
            <p className="text-slate-400">No emotion data for this period</p>
          </div>
        )}
      </div>

      {/* Historical Timeline */}
      <div className="glass-card rounded-2xl p-6">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-xl font-semibold text-white">Emotion Timeline</h3>
          <button
            onClick={() => setShowHistory(!showHistory)}
            className="text-blue-400 hover:text-blue-300 transition-colors text-sm"
          >
            {showHistory ? 'Hide Timeline' : 'Show Timeline'}
          </button>
        </div>

        <AnimatePresence>
          {showHistory && (
            <motion.div
              initial={{ opacity: 0, height: 0 }}
              animate={{ opacity: 1, height: 'auto' }}
              exit={{ opacity: 0, height: 0 }}
              className="space-y-3"
            >
              {emotionHistory.slice(-10).reverse().map((entry, index) => {
                const config = emotionConfig[entry.emotion]
                const EntryIcon = config.icon
                
                return (
                  <motion.div
                    key={index}
                    initial={{ opacity: 0, x: -20 }}
                    animate={{ opacity: 1, x: 0 }}
                    transition={{ delay: index * 0.1 }}
                    className="flex items-center space-x-4 p-3 bg-slate-800/30 rounded-lg"
                  >
                    <div className={`w-8 h-8 rounded-lg flex items-center justify-center ${config.color}`}>
                      <EntryIcon className="w-4 h-4" />
                    </div>
                    
                    <div className="flex-1">
                      <div className="flex items-center justify-between">
                        <span className="font-medium text-white">{config.label}</span>
                        <span className="text-sm text-slate-400">
                          {entry.timestamp.toLocaleTimeString()}
                        </span>
                      </div>
                      <div className="flex items-center space-x-4 text-sm text-slate-400">
                        <span>Intensity: {entry.intensity}%</span>
                        {entry.tradingDecision && entry.tradingDecision !== 'none' && (
                          <span className="capitalize">{entry.tradingDecision}</span>
                        )}
                        {entry.outcome && (
                          <span className={`${
                            entry.outcome === 'positive' ? 'text-green-400' :
                            entry.outcome === 'negative' ? 'text-red-400' :
                            'text-slate-400'
                          }`}>
                            {entry.outcome}
                          </span>
                        )}
                      </div>
                    </div>
                  </motion.div>
                )
              })}
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    </div>
  )
}

export default EmotionTracker