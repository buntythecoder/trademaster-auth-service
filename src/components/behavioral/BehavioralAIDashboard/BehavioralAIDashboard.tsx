import React, { useState, useEffect, useMemo, useCallback } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import {
  Brain, TrendingUp, TrendingDown, AlertTriangle, CheckCircle, Lightbulb,
  BarChart3, LineChart, PieChart, Activity, Settings, Users, Star,
  Smile, Meh, Frown, RefreshCw, Play, Pause, X, ChevronDown,
  Target, Zap, Shield, Clock, DollarSign, ArrowUpDown, Bell,
  Eye, Heart, Timer, Gauge, Bell as AlertIcon
} from 'lucide-react'

// Types and Interfaces
interface EmotionState {
  emotion: 'fear' | 'greed' | 'confidence' | 'anxiety' | 'euphoria' | 'neutral' | 'frustration' | 'excitement'
  intensity: number // 0-100
  timestamp: Date
  triggers: string[]
  context: 'market_open' | 'position_entry' | 'position_exit' | 'loss_streak' | 'win_streak' | 'market_volatility' | 'news_event'
}

interface BehavioralPattern {
  id: string
  type: 'revenge_trading' | 'fomo' | 'loss_aversion' | 'overconfidence' | 'herding' | 'anchoring' | 'confirmation_bias'
  severity: 'low' | 'medium' | 'high' | 'critical'
  frequency: number
  lastDetected: Date
  description: string
  impact: {
    financialLoss: number
    missedOpportunities: number
    streakBreaker: number
  }
  recommendations: string[]
}

interface TradingPsychologyScore {
  overall: number // 0-100
  discipline: number
  emotionalControl: number
  riskManagement: number
  patternRecognition: number
  decisionMaking: number
  adaptability: number
  trend: 'improving' | 'declining' | 'stable'
  benchmarkPercentile: number
}

interface InterventionAlert {
  id: string
  type: 'warning' | 'critical' | 'suggestion' | 'achievement'
  title: string
  message: string
  timestamp: Date
  actionRequired: boolean
  suggestions: string[]
  relatedPatterns: string[]
  isRead: boolean
  effectiveness?: number
}

// Mock data
const mockPsychologyScore: TradingPsychologyScore = {
  overall: 78,
  discipline: 82,
  emotionalControl: 73,
  riskManagement: 85,
  patternRecognition: 76,
  decisionMaking: 79,
  adaptability: 71,
  trend: 'improving',
  benchmarkPercentile: 85
}

const mockBehavioralPatterns: BehavioralPattern[] = [
  {
    id: '1',
    type: 'fomo',
    severity: 'medium',
    frequency: 12,
    lastDetected: new Date('2024-01-15'),
    description: 'Tendency to enter trades during high volatility periods without proper analysis',
    impact: {
      financialLoss: 2350,
      missedOpportunities: 3,
      streakBreaker: 2
    },
    recommendations: [
      'Wait for 15-minute confirmation before entering trades',
      'Set predetermined entry criteria',
      'Use smaller position sizes during volatile periods'
    ]
  },
  {
    id: '2',
    type: 'loss_aversion',
    severity: 'high',
    frequency: 8,
    lastDetected: new Date('2024-01-18'),
    description: 'Holding losing positions too long and cutting winning positions early',
    impact: {
      financialLoss: 4200,
      missedOpportunities: 6,
      streakBreaker: 1
    },
    recommendations: [
      'Implement strict stop-loss rules',
      'Use trailing stops for winning positions',
      'Practice letting winners run'
    ]
  }
]

const mockInterventionAlerts: InterventionAlert[] = [
  {
    id: '1',
    type: 'warning',
    title: 'Emotional Trading Alert',
    message: 'Detected 3 consecutive emotional trades in the last hour. Consider taking a break.',
    timestamp: new Date(),
    actionRequired: true,
    suggestions: ['Take a 15-minute break', 'Review your trading plan', 'Practice breathing exercises'],
    relatedPatterns: ['fomo', 'revenge_trading'],
    isRead: false
  },
  {
    id: '2',
    type: 'achievement',
    title: 'Discipline Improvement',
    message: 'Great job! You\'ve followed your trading plan for 5 consecutive days.',
    timestamp: new Date('2024-01-20'),
    actionRequired: false,
    suggestions: ['Continue this consistency', 'Consider increasing position size gradually'],
    relatedPatterns: [],
    isRead: true,
    effectiveness: 92
  }
]

const BehavioralAIDashboard: React.FC = () => {
  const [activeTab, setActiveTab] = useState(0)
  const [isSessionActive, setIsSessionActive] = useState(false)
  const [currentEmotion, setCurrentEmotion] = useState<EmotionState>({
    emotion: 'confidence',
    intensity: 75,
    timestamp: new Date(),
    triggers: ['successful_trade', 'market_trend_confirmation'],
    context: 'position_entry'
  })

  const tabs = [
    'Psychology Overview',
    'Emotional State',
    'Behavioral Patterns',
    'AI Interventions',
    'Progress & Goals',
    'Peer Comparison',
    'AI Coaching Session'
  ]

  const getEmotionIcon = (emotion: EmotionState['emotion']) => {
    switch (emotion) {
      case 'confidence': return <Smile className="h-6 w-6 text-green-400" />
      case 'anxiety': case 'fear': return <Frown className="h-6 w-6 text-red-400" />
      case 'neutral': return <Meh className="h-6 w-6 text-slate-400" />
      default: return <Brain className="h-6 w-6 text-purple-400" />
    }
  }

  const getSeverityColor = (severity: BehavioralPattern['severity']) => {
    switch (severity) {
      case 'critical': return 'text-red-500 bg-red-500/20'
      case 'high': return 'text-orange-500 bg-orange-500/20'
      case 'medium': return 'text-yellow-500 bg-yellow-500/20'
      case 'low': return 'text-green-500 bg-green-500/20'
    }
  }

  const getAlertTypeIcon = (type: InterventionAlert['type']) => {
    switch (type) {
      case 'critical': return <AlertTriangle className="h-5 w-5 text-red-400" />
      case 'warning': return <AlertIcon className="h-5 w-5 text-yellow-400" />
      case 'suggestion': return <Lightbulb className="h-5 w-5 text-blue-400" />
      case 'achievement': return <CheckCircle className="h-5 w-5 text-green-400" />
    }
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-purple-900 to-slate-900 p-6">
      <div className="max-w-7xl mx-auto">
        {/* Header */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="mb-8"
        >
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-4">
              <div className="p-3 rounded-xl bg-purple-500/20 backdrop-blur-sm">
                <Brain className="h-8 w-8 text-purple-400" />
              </div>
              <div>
                <h1 className="text-3xl font-bold text-white">Behavioral AI Dashboard</h1>
                <p className="text-slate-300 mt-1">AI-powered trading psychology insights and coaching</p>
              </div>
            </div>
            <div className="flex items-center space-x-4">
              <motion.button
                whileHover={{ scale: 1.05 }}
                whileTap={{ scale: 0.95 }}
                onClick={() => setIsSessionActive(!isSessionActive)}
                className={`px-4 py-2 rounded-lg font-medium transition-all ${
                  isSessionActive
                    ? 'bg-red-500/20 text-red-400 hover:bg-red-500/30'
                    : 'bg-green-500/20 text-green-400 hover:bg-green-500/30'
                }`}
              >
                {isSessionActive ? (
                  <>
                    <Pause className="h-4 w-4 mr-2 inline" />
                    End Session
                  </>
                ) : (
                  <>
                    <Play className="h-4 w-4 mr-2 inline" />
                    Start AI Session
                  </>
                )}
              </motion.button>
              <button className="p-2 rounded-lg bg-slate-700/50 hover:bg-slate-600/50 transition-colors">
                <RefreshCw className="h-5 w-5 text-slate-300" />
              </button>
            </div>
          </div>
        </motion.div>

        {/* Navigation Tabs */}
        <div className="mb-8">
          <div className="flex space-x-1 rounded-xl bg-slate-800/50 p-1 backdrop-blur-sm">
            {tabs.map((tab, index) => (
              <button
                key={index}
                onClick={() => setActiveTab(index)}
                className={`px-4 py-3 text-sm font-medium rounded-lg transition-all ${
                  activeTab === index
                    ? 'bg-purple-500/20 text-purple-300 shadow-lg'
                    : 'text-slate-400 hover:text-slate-200 hover:bg-slate-700/50'
                }`}
              >
                {tab}
              </button>
            ))}
          </div>
        </div>

        {/* Tab Content */}
        <AnimatePresence mode="wait">
          <motion.div
            key={activeTab}
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -20 }}
            transition={{ duration: 0.3 }}
          >
            {activeTab === 0 && <PsychologyOverviewTab score={mockPsychologyScore} />}
            {activeTab === 1 && <EmotionalStateTab emotion={currentEmotion} />}
            {activeTab === 2 && <BehavioralPatternsTab patterns={mockBehavioralPatterns} />}
            {activeTab === 3 && <InterventionsTab alerts={mockInterventionAlerts} />}
            {activeTab === 4 && <ProgressGoalsTab />}
            {activeTab === 5 && <PeerComparisonTab />}
            {activeTab === 6 && <CoachingSessionTab isActive={isSessionActive} />}
          </motion.div>
        </AnimatePresence>
      </div>
    </div>
  )
}

// Tab Components
const PsychologyOverviewTab: React.FC<{ score: TradingPsychologyScore }> = ({ score }) => (
  <div className="grid grid-cols-1 lg:grid-cols-2 xl:grid-cols-3 gap-6">
    {/* Overall Score Card */}
    <motion.div
      initial={{ opacity: 0, scale: 0.9 }}
      animate={{ opacity: 1, scale: 1 }}
      className="lg:col-span-2 xl:col-span-1"
    >
      <div className="bg-gradient-to-br from-slate-800/50 to-slate-900/50 rounded-xl p-6 backdrop-blur-sm border border-slate-700/50">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-lg font-semibold text-white">Psychology Score</h3>
          <Gauge className="h-5 w-5 text-purple-400" />
        </div>
        <div className="text-center">
          <div className="relative w-32 h-32 mx-auto mb-4">
            <svg className="w-32 h-32 transform -rotate-90">
              <circle
                cx="64"
                cy="64"
                r="56"
                stroke="currentColor"
                strokeWidth="8"
                fill="transparent"
                className="text-slate-700"
              />
              <circle
                cx="64"
                cy="64"
                r="56"
                stroke="currentColor"
                strokeWidth="8"
                fill="transparent"
                strokeDasharray={`${2 * Math.PI * 56}`}
                strokeDashoffset={`${2 * Math.PI * 56 * (1 - score.overall / 100)}`}
                className="text-purple-400"
              />
            </svg>
            <div className="absolute inset-0 flex items-center justify-center">
              <span className="text-3xl font-bold text-white">{score.overall}</span>
            </div>
          </div>
          <p className="text-slate-300 mb-2">Overall Psychology Score</p>
          <div className={`inline-flex items-center px-3 py-1 rounded-full text-sm ${
            score.trend === 'improving' ? 'bg-green-500/20 text-green-400' :
            score.trend === 'declining' ? 'bg-red-500/20 text-red-400' :
            'bg-slate-500/20 text-slate-400'
          }`}>
            {score.trend === 'improving' ? <TrendingUp className="h-4 w-4 mr-1" /> :
             score.trend === 'declining' ? <TrendingDown className="h-4 w-4 mr-1" /> :
             <Activity className="h-4 w-4 mr-1" />}
            {score.trend}
          </div>
        </div>
      </div>
    </motion.div>

    {/* Score Breakdown */}
    <motion.div
      initial={{ opacity: 0, scale: 0.9 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ delay: 0.1 }}
      className="lg:col-span-2"
    >
      <div className="bg-gradient-to-br from-slate-800/50 to-slate-900/50 rounded-xl p-6 backdrop-blur-sm border border-slate-700/50">
        <h3 className="text-lg font-semibold text-white mb-4">Score Breakdown</h3>
        <div className="space-y-4">
          {[
            { label: 'Discipline', value: score.discipline, icon: Shield },
            { label: 'Emotional Control', value: score.emotionalControl, icon: Heart },
            { label: 'Risk Management', value: score.riskManagement, icon: Target },
            { label: 'Pattern Recognition', value: score.patternRecognition, icon: Eye },
            { label: 'Decision Making', value: score.decisionMaking, icon: Brain },
            { label: 'Adaptability', value: score.adaptability, icon: Zap }
          ].map((item, index) => (
            <div key={index} className="flex items-center justify-between">
              <div className="flex items-center space-x-3">
                <item.icon className="h-5 w-5 text-purple-400" />
                <span className="text-slate-300">{item.label}</span>
              </div>
              <div className="flex items-center space-x-3">
                <div className="w-24 h-2 bg-slate-700 rounded-full overflow-hidden">
                  <div
                    className="h-full bg-gradient-to-r from-purple-500 to-cyan-400"
                    style={{ width: `${item.value}%` }}
                  />
                </div>
                <span className="text-white font-medium w-8">{item.value}</span>
              </div>
            </div>
          ))}
        </div>
      </div>
    </motion.div>
  </div>
)

const EmotionalStateTab: React.FC<{ emotion: EmotionState }> = ({ emotion }) => (
  <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
    <motion.div
      initial={{ opacity: 0, scale: 0.9 }}
      animate={{ opacity: 1, scale: 1 }}
      className="bg-gradient-to-br from-slate-800/50 to-slate-900/50 rounded-xl p-6 backdrop-blur-sm border border-slate-700/50"
    >
      <div className="flex items-center justify-between mb-6">
        <h3 className="text-lg font-semibold text-white">Current Emotional State</h3>
        <Timer className="h-5 w-5 text-purple-400" />
      </div>
      
      <div className="text-center mb-6">
        <div className="mb-4">
          {/* Emotion icon based on current emotion */}
          <div className="w-20 h-20 mx-auto rounded-full bg-gradient-to-br from-purple-500/20 to-cyan-500/20 flex items-center justify-center">
            <Brain className="h-10 w-10 text-purple-400" />
          </div>
        </div>
        <h4 className="text-xl font-bold text-white capitalize mb-2">{emotion.emotion}</h4>
        <p className="text-slate-300">Intensity: {emotion.intensity}%</p>
        <div className="w-full h-3 bg-slate-700 rounded-full mt-2 overflow-hidden">
          <div
            className="h-full bg-gradient-to-r from-purple-500 to-cyan-400"
            style={{ width: `${emotion.intensity}%` }}
          />
        </div>
      </div>

      <div className="space-y-3">
        <div>
          <p className="text-sm font-medium text-slate-300 mb-1">Context</p>
          <span className="inline-block px-3 py-1 bg-slate-700/50 rounded-full text-sm text-white capitalize">
            {emotion.context.replace('_', ' ')}
          </span>
        </div>
        <div>
          <p className="text-sm font-medium text-slate-300 mb-1">Triggers</p>
          <div className="flex flex-wrap gap-2">
            {emotion.triggers.map((trigger, index) => (
              <span
                key={index}
                className="inline-block px-2 py-1 bg-purple-500/20 text-purple-300 rounded text-xs"
              >
                {trigger.replace('_', ' ')}
              </span>
            ))}
          </div>
        </div>
      </div>
    </motion.div>

    <motion.div
      initial={{ opacity: 0, scale: 0.9 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ delay: 0.1 }}
      className="bg-gradient-to-br from-slate-800/50 to-slate-900/50 rounded-xl p-6 backdrop-blur-sm border border-slate-700/50"
    >
      <h3 className="text-lg font-semibold text-white mb-4">Emotional Timeline</h3>
      <div className="space-y-4">
        <p className="text-slate-400 text-center py-8">
          Real-time emotional tracking will be displayed here during active trading sessions.
        </p>
      </div>
    </motion.div>
  </div>
)

const BehavioralPatternsTab: React.FC<{ patterns: BehavioralPattern[] }> = ({ patterns }) => (
  <div className="space-y-6">
    {patterns.map((pattern, index) => (
      <motion.div
        key={pattern.id}
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: index * 0.1 }}
        className="bg-gradient-to-br from-slate-800/50 to-slate-900/50 rounded-xl p-6 backdrop-blur-sm border border-slate-700/50"
      >
        <div className="flex items-start justify-between mb-4">
          <div>
            <div className="flex items-center space-x-3 mb-2">
              <h3 className="text-lg font-semibold text-white capitalize">
                {pattern.type.replace('_', ' ')}
              </h3>
              <span className={`px-2 py-1 rounded-full text-xs font-medium ${getSeverityColor(pattern.severity)}`}>
                {pattern.severity}
              </span>
            </div>
            <p className="text-slate-300 text-sm">{pattern.description}</p>
          </div>
          <AlertTriangle className="h-6 w-6 text-yellow-400 flex-shrink-0" />
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-4">
          <div className="bg-slate-700/30 rounded-lg p-3">
            <p className="text-xs text-slate-400 mb-1">Financial Loss</p>
            <p className="text-lg font-semibold text-red-400">
              ₹{pattern.impact.financialLoss.toLocaleString()}
            </p>
          </div>
          <div className="bg-slate-700/30 rounded-lg p-3">
            <p className="text-xs text-slate-400 mb-1">Missed Opportunities</p>
            <p className="text-lg font-semibold text-orange-400">{pattern.impact.missedOpportunities}</p>
          </div>
          <div className="bg-slate-700/30 rounded-lg p-3">
            <p className="text-xs text-slate-400 mb-1">Frequency (30 days)</p>
            <p className="text-lg font-semibold text-cyan-400">{pattern.frequency}</p>
          </div>
        </div>

        <div>
          <p className="text-sm font-medium text-slate-300 mb-2">AI Recommendations</p>
          <ul className="space-y-1">
            {pattern.recommendations.map((rec, recIndex) => (
              <li key={recIndex} className="flex items-start space-x-2 text-sm text-slate-300">
                <CheckCircle className="h-4 w-4 text-green-400 mt-0.5 flex-shrink-0" />
                <span>{rec}</span>
              </li>
            ))}
          </ul>
        </div>
      </motion.div>
    ))}
  </div>
)

const InterventionsTab: React.FC<{ alerts: InterventionAlert[] }> = ({ alerts }) => (
  <div className="space-y-4">
    {alerts.map((alert, index) => (
      <motion.div
        key={alert.id}
        initial={{ opacity: 0, x: -20 }}
        animate={{ opacity: 1, x: 0 }}
        transition={{ delay: index * 0.1 }}
        className={`bg-gradient-to-br from-slate-800/50 to-slate-900/50 rounded-xl p-6 backdrop-blur-sm border ${
          !alert.isRead ? 'border-purple-500/50' : 'border-slate-700/50'
        }`}
      >
        <div className="flex items-start justify-between mb-4">
          <div className="flex items-start space-x-3">
            {getAlertTypeIcon(alert.type)}
            <div>
              <h3 className="text-lg font-semibold text-white">{alert.title}</h3>
              <p className="text-slate-300 text-sm mt-1">{alert.message}</p>
            </div>
          </div>
          <div className="flex items-center space-x-2">
            <Clock className="h-4 w-4 text-slate-400" />
            <span className="text-xs text-slate-400">
              {alert.timestamp.toLocaleTimeString()}
            </span>
          </div>
        </div>

        {alert.suggestions.length > 0 && (
          <div className="mb-4">
            <p className="text-sm font-medium text-slate-300 mb-2">Suggestions</p>
            <ul className="space-y-1">
              {alert.suggestions.map((suggestion, sugIndex) => (
                <li key={sugIndex} className="flex items-start space-x-2 text-sm text-slate-300">
                  <Lightbulb className="h-4 w-4 text-yellow-400 mt-0.5 flex-shrink-0" />
                  <span>{suggestion}</span>
                </li>
              ))}
            </ul>
          </div>
        )}

        {alert.effectiveness && (
          <div className="flex items-center space-x-2 text-sm text-green-400">
            <CheckCircle className="h-4 w-4" />
            <span>Effectiveness: {alert.effectiveness}%</span>
          </div>
        )}
      </motion.div>
    ))}
  </div>
)

const ProgressGoalsTab: React.FC = () => (
  <div className="text-center py-20">
    <Target className="h-16 w-16 text-purple-400 mx-auto mb-4" />
    <h3 className="text-xl font-semibold text-white mb-2">Progress & Goals</h3>
    <p className="text-slate-400">Track your trading psychology improvement goals and milestones.</p>
    <p className="text-slate-500 text-sm mt-4">Feature coming soon...</p>
  </div>
)

const PeerComparisonTab: React.FC = () => (
  <div className="text-center py-20">
    <Users className="h-16 w-16 text-purple-400 mx-auto mb-4" />
    <h3 className="text-xl font-semibold text-white mb-2">Peer Comparison</h3>
    <p className="text-slate-400">Compare your trading psychology metrics with similar traders.</p>
    <p className="text-slate-500 text-sm mt-4">Feature coming soon...</p>
  </div>
)

const CoachingSessionTab: React.FC<{ isActive: boolean }> = ({ isActive }) => (
  <div className="text-center py-20">
    <Brain className="h-16 w-16 text-purple-400 mx-auto mb-4" />
    <h3 className="text-xl font-semibold text-white mb-2">AI Coaching Session</h3>
    <p className="text-slate-400 mb-6">
      {isActive 
        ? 'AI coaching session is active. Real-time guidance and feedback will appear here.'
        : 'Start an AI coaching session to receive personalized trading psychology guidance.'}
    </p>
    {isActive && (
      <div className="bg-gradient-to-br from-slate-800/50 to-slate-900/50 rounded-xl p-6 backdrop-blur-sm border border-slate-700/50 max-w-md mx-auto">
        <p className="text-slate-300 text-sm">AI Coach is monitoring your trading session...</p>
        <div className="flex justify-center mt-4">
          <div className="flex space-x-1">
            {[0, 1, 2].map((i) => (
              <motion.div
                key={i}
                className="w-2 h-2 bg-purple-400 rounded-full"
                animate={{
                  scale: [1, 1.5, 1],
                  opacity: [1, 0.5, 1]
                }}
                transition={{
                  duration: 1.5,
                  repeat: Infinity,
                  delay: i * 0.5
                }}
              />
            ))}
          </div>
        </div>
      </div>
    )}
  </div>
)

export default BehavioralAIDashboard