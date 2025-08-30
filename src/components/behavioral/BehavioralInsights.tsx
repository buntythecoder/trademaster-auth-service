import React, { useState } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { 
  Brain, TrendingUp, TrendingDown, Target, AlertCircle,
  BarChart3, Clock, Zap, Eye, Award, RefreshCw, Calendar,
  ThermometerSun, Activity, Lightbulb, ArrowUp,
  ChevronRight, Filter, Users, Star
} from 'lucide-react'

export interface TradingPattern {
  id: string
  type: 'impulsive' | 'overconfident' | 'risk-averse' | 'revenge-trading' | 'fomo' | 'disciplined'
  frequency: number // per week
  impact: 'positive' | 'negative' | 'neutral'
  description: string
  examples: string[]
  recommendation: string
  severity: 'low' | 'medium' | 'high' | 'critical'
}

export interface BehavioralMetric {
  id: string
  name: string
  value: number
  unit: string
  trend: 'up' | 'down' | 'stable'
  benchmark: number
  description: string
  category: 'discipline' | 'emotion' | 'risk' | 'performance'
}

export interface BehavioralGoal {
  id: string
  title: string
  target: number
  current: number
  unit: string
  deadline: Date
  status: 'on-track' | 'behind' | 'ahead' | 'completed'
  category: string
}

interface BehavioralInsightsProps {
  patterns: TradingPattern[]
  metrics: BehavioralMetric[]
  goals: BehavioralGoal[]
  timeframe: '7d' | '30d' | '90d'
  onTimeframeChange: (timeframe: '7d' | '30d' | '90d') => void
  className?: string
}

const patternConfig = {
  impulsive: {
    icon: Zap,
    color: 'text-red-400 bg-red-500/20',
    severity: { low: 'yellow', medium: 'orange', high: 'red', critical: 'red' }
  },
  overconfident: {
    icon: TrendingUp,
    color: 'text-yellow-400 bg-yellow-500/20',
    severity: { low: 'yellow', medium: 'orange', high: 'red', critical: 'red' }
  },
  'risk-averse': {
    icon: Target,
    color: 'text-blue-400 bg-blue-500/20',
    severity: { low: 'blue', medium: 'blue', high: 'yellow', critical: 'orange' }
  },
  'revenge-trading': {
    icon: AlertCircle,
    color: 'text-red-400 bg-red-500/20',
    severity: { low: 'yellow', medium: 'orange', high: 'red', critical: 'red' }
  },
  fomo: {
    icon: ArrowUp,
    color: 'text-purple-400 bg-purple-500/20',
    severity: { low: 'purple', medium: 'purple', high: 'red', critical: 'red' }
  },
  disciplined: {
    icon: Award,
    color: 'text-green-400 bg-green-500/20',
    severity: { low: 'green', medium: 'green', high: 'green', critical: 'green' }
  }
}

const metricConfig = {
  discipline: {
    icon: Target,
    color: 'text-blue-400',
    label: 'Discipline'
  },
  emotion: {
    icon: Brain,
    color: 'text-purple-400',
    label: 'Emotion Control'
  },
  risk: {
    icon: AlertCircle,
    color: 'text-orange-400',
    label: 'Risk Management'
  },
  performance: {
    icon: TrendingUp,
    color: 'text-green-400',
    label: 'Performance'
  }
}

export const BehavioralInsights: React.FC<BehavioralInsightsProps> = ({
  patterns,
  metrics,
  goals,
  timeframe,
  onTimeframeChange,
  className = ''
}) => {
  const [activeTab, setActiveTab] = useState<'patterns' | 'metrics' | 'goals'>('patterns')
  const [selectedPattern, setSelectedPattern] = useState<TradingPattern | null>(null)

  // Calculate overall behavioral score
  const calculateBehavioralScore = () => {
    const disciplineMetrics = metrics.filter(m => m.category === 'discipline')
    const emotionMetrics = metrics.filter(m => m.category === 'emotion')
    const riskMetrics = metrics.filter(m => m.category === 'risk')
    
    const avgDiscipline = disciplineMetrics.reduce((sum, m) => sum + (m.value / m.benchmark), 0) / disciplineMetrics.length || 0
    const avgEmotion = emotionMetrics.reduce((sum, m) => sum + (m.value / m.benchmark), 0) / emotionMetrics.length || 0
    const avgRisk = riskMetrics.reduce((sum, m) => sum + (m.value / m.benchmark), 0) / riskMetrics.length || 0
    
    return Math.round((avgDiscipline + avgEmotion + avgRisk) * 33.33)
  }

  const behavioralScore = calculateBehavioralScore()

  // Get critical patterns that need attention
  const criticalPatterns = patterns.filter(p => p.severity === 'critical' || p.severity === 'high')

  return (
    <div className={`space-y-6 ${className}`}>
      {/* Header with overall score */}
      <div className="glass-card rounded-2xl p-6">
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center space-x-4">
            <div className="w-16 h-16 rounded-2xl bg-gradient-to-r from-purple-600 to-pink-600 flex items-center justify-center">
              <Brain className="w-8 h-8 text-white" />
            </div>
            <div>
              <h2 className="text-2xl font-bold text-white">Behavioral Analysis</h2>
              <p className="text-slate-400">Trading psychology insights and patterns</p>
            </div>
          </div>

          <div className="text-right">
            <div className="text-3xl font-bold text-white mb-1">{behavioralScore}</div>
            <div className="text-sm text-slate-400">Behavioral Score</div>
            <div className={`text-xs px-2 py-1 rounded-lg mt-1 ${
              behavioralScore >= 80 ? 'bg-green-500/20 text-green-400' :
              behavioralScore >= 60 ? 'bg-yellow-500/20 text-yellow-400' :
              'bg-red-500/20 text-red-400'
            }`}>
              {behavioralScore >= 80 ? 'Excellent' : behavioralScore >= 60 ? 'Good' : 'Needs Improvement'}
            </div>
          </div>
        </div>

        {/* Timeframe selector */}
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <Calendar className="w-4 h-4 text-slate-400" />
            <span className="text-sm text-slate-400">Timeframe:</span>
          </div>
          <div className="flex space-x-2">
            {(['7d', '30d', '90d'] as const).map((period) => (
              <button
                key={period}
                onClick={() => onTimeframeChange(period)}
                className={`px-3 py-2 rounded-lg text-sm font-medium transition-colors ${
                  timeframe === period
                    ? 'bg-blue-600 text-white'
                    : 'bg-slate-700/50 text-slate-300 hover:bg-slate-600/50'
                }`}
              >
                {period === '7d' ? '7 Days' : period === '30d' ? '30 Days' : '90 Days'}
              </button>
            ))}
          </div>
        </div>

        {/* Critical alerts */}
        {criticalPatterns.length > 0 && (
          <div className="mt-4 p-4 bg-red-500/10 border border-red-400/30 rounded-xl">
            <div className="flex items-start space-x-3">
              <AlertCircle className="w-5 h-5 text-red-400 flex-shrink-0 mt-0.5" />
              <div>
                <h4 className="font-medium text-red-400 mb-1">Attention Needed</h4>
                <p className="text-sm text-slate-300">
                  {criticalPatterns.length} behavioral pattern{criticalPatterns.length > 1 ? 's' : ''} requiring immediate attention
                </p>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Tab navigation */}
      <div className="flex space-x-1 bg-slate-800/50 rounded-xl p-1">
        {[
          { id: 'patterns', label: 'Patterns', icon: Activity },
          { id: 'metrics', label: 'Metrics', icon: BarChart3 },
          { id: 'goals', label: 'Goals', icon: Target }
        ].map(({ id, label, icon: Icon }) => (
          <button
            key={id}
            onClick={() => setActiveTab(id as typeof activeTab)}
            className={`flex-1 flex items-center justify-center space-x-2 px-4 py-3 rounded-lg font-medium transition-all ${
              activeTab === id
                ? 'bg-blue-600 text-white shadow-lg'
                : 'text-slate-400 hover:text-white hover:bg-slate-700/50'
            }`}
          >
            <Icon className="w-4 h-4" />
            <span>{label}</span>
          </button>
        ))}
      </div>

      {/* Tab content */}
      <AnimatePresence mode="wait">
        {/* Patterns Tab */}
        {activeTab === 'patterns' && (
          <motion.div
            key="patterns"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -20 }}
            className="space-y-4"
          >
            {patterns.map((pattern) => {
              const config = patternConfig[pattern.type]
              const IconComponent = config.icon
              
              return (
                <motion.div
                  key={pattern.id}
                  layoutId={pattern.id}
                  className={`glass-card rounded-xl p-6 cursor-pointer transition-all hover:shadow-lg ${
                    pattern.severity === 'critical' || pattern.severity === 'high' 
                      ? 'border border-red-400/30' 
                      : 'border border-slate-600/30'
                  }`}
                  onClick={() => setSelectedPattern(pattern)}
                >
                  <div className="flex items-start space-x-4">
                    <div className={`w-12 h-12 rounded-xl flex items-center justify-center ${config.color}`}>
                      <IconComponent className="w-6 h-6" />
                    </div>
                    
                    <div className="flex-1">
                      <div className="flex items-center justify-between mb-2">
                        <h3 className="font-semibold text-white capitalize">
                          {pattern.type.replace('-', ' ')}
                        </h3>
                        <div className="flex items-center space-x-2">
                          <span className={`px-2 py-1 rounded-lg text-xs font-medium capitalize ${
                            pattern.severity === 'critical' ? 'bg-red-500/20 text-red-400' :
                            pattern.severity === 'high' ? 'bg-orange-500/20 text-orange-400' :
                            pattern.severity === 'medium' ? 'bg-yellow-500/20 text-yellow-400' :
                            'bg-green-500/20 text-green-400'
                          }`}>
                            {pattern.severity}
                          </span>
                          <ChevronRight className="w-4 h-4 text-slate-400" />
                        </div>
                      </div>
                      
                      <p className="text-slate-400 text-sm mb-3">{pattern.description}</p>
                      
                      <div className="flex items-center justify-between">
                        <div className="flex items-center space-x-4 text-sm">
                          <span className="text-slate-300">
                            Frequency: <span className="font-medium">{pattern.frequency}/week</span>
                          </span>
                          <span className={`font-medium ${
                            pattern.impact === 'positive' ? 'text-green-400' :
                            pattern.impact === 'negative' ? 'text-red-400' :
                            'text-slate-400'
                          }`}>
                            {pattern.impact}
                          </span>
                        </div>
                        
                        {pattern.severity === 'critical' && (
                          <div className="flex items-center space-x-1 text-red-400 text-sm">
                            <AlertCircle className="w-3 h-3" />
                            <span>Urgent</span>
                          </div>
                        )}
                      </div>
                    </div>
                  </div>
                </motion.div>
              )
            })}
          </motion.div>
        )}

        {/* Metrics Tab */}
        {activeTab === 'metrics' && (
          <motion.div
            key="metrics"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -20 }}
            className="grid grid-cols-1 md:grid-cols-2 gap-6"
          >
            {Object.entries(
              metrics.reduce((acc, metric) => {
                if (!acc[metric.category]) acc[metric.category] = []
                acc[metric.category].push(metric)
                return acc
              }, {} as Record<string, BehavioralMetric[]>)
            ).map(([category, categoryMetrics]) => {
              const config = metricConfig[category as keyof typeof metricConfig]
              const IconComponent = config.icon
              
              return (
                <div key={category} className="glass-card rounded-xl p-6">
                  <div className="flex items-center space-x-3 mb-4">
                    <div className={`w-10 h-10 rounded-lg bg-slate-700/50 flex items-center justify-center`}>
                      <IconComponent className={`w-5 h-5 ${config.color}`} />
                    </div>
                    <h3 className="font-semibold text-white">{config.label}</h3>
                  </div>
                  
                  <div className="space-y-4">
                    {categoryMetrics.map((metric) => (
                      <div key={metric.id} className="space-y-2">
                        <div className="flex items-center justify-between">
                          <span className="text-sm font-medium text-slate-300">{metric.name}</span>
                          <div className="flex items-center space-x-2">
                            <span className="font-semibold text-white">
                              {metric.value}{metric.unit}
                            </span>
                            {metric.trend === 'up' ? (
                              <TrendingUp className="w-4 h-4 text-green-400" />
                            ) : metric.trend === 'down' ? (
                              <TrendingDown className="w-4 h-4 text-red-400" />
                            ) : (
                              <Activity className="w-4 h-4 text-slate-400" />
                            )}
                          </div>
                        </div>
                        
                        <div className="space-y-1">
                          <div className="flex justify-between text-xs text-slate-400">
                            <span>vs Benchmark: {metric.benchmark}{metric.unit}</span>
                            <span>
                              {((metric.value / metric.benchmark) * 100).toFixed(0)}%
                            </span>
                          </div>
                          <div className="w-full bg-slate-700/50 rounded-full h-2">
                            <div 
                              className={`h-2 rounded-full transition-all duration-1000 ${
                                metric.value >= metric.benchmark ? 'bg-green-500' : 'bg-yellow-500'
                              }`}
                              style={{ 
                                width: `${Math.min((metric.value / metric.benchmark) * 100, 100)}%` 
                              }}
                            />
                          </div>
                        </div>
                        
                        <p className="text-xs text-slate-400">{metric.description}</p>
                      </div>
                    ))}
                  </div>
                </div>
              )
            })}
          </motion.div>
        )}

        {/* Goals Tab */}
        {activeTab === 'goals' && (
          <motion.div
            key="goals"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -20 }}
            className="space-y-4"
          >
            {goals.map((goal) => {
              const progress = (goal.current / goal.target) * 100
              const daysLeft = Math.ceil((goal.deadline.getTime() - Date.now()) / (1000 * 60 * 60 * 24))
              
              return (
                <div key={goal.id} className="glass-card rounded-xl p-6">
                  <div className="flex items-start justify-between mb-4">
                    <div>
                      <h3 className="font-semibold text-white mb-1">{goal.title}</h3>
                      <p className="text-sm text-slate-400">{goal.category}</p>
                    </div>
                    
                    <div className={`px-3 py-1 rounded-lg text-sm font-medium ${
                      goal.status === 'completed' ? 'bg-green-500/20 text-green-400' :
                      goal.status === 'ahead' ? 'bg-blue-500/20 text-blue-400' :
                      goal.status === 'on-track' ? 'bg-yellow-500/20 text-yellow-400' :
                      'bg-red-500/20 text-red-400'
                    }`}>
                      {goal.status.replace('-', ' ')}
                    </div>
                  </div>
                  
                  <div className="space-y-3">
                    <div className="flex items-center justify-between">
                      <span className="text-sm text-slate-300">Progress</span>
                      <span className="text-sm font-medium text-white">
                        {goal.current}{goal.unit} / {goal.target}{goal.unit}
                      </span>
                    </div>
                    
                    <div className="w-full bg-slate-700/50 rounded-full h-3">
                      <div 
                        className={`h-3 rounded-full transition-all duration-1000 ${
                          progress >= 100 ? 'bg-green-500' :
                          progress >= 75 ? 'bg-blue-500' :
                          progress >= 50 ? 'bg-yellow-500' :
                          'bg-red-500'
                        }`}
                        style={{ width: `${Math.min(progress, 100)}%` }}
                      />
                    </div>
                    
                    <div className="flex items-center justify-between text-sm text-slate-400">
                      <span>{Math.round(progress)}% complete</span>
                      <span>{daysLeft > 0 ? `${daysLeft} days left` : 'Deadline passed'}</span>
                    </div>
                  </div>
                </div>
              )
            })}
            
            {goals.length === 0 && (
              <div className="text-center py-12">
                <Target className="w-16 h-16 mx-auto mb-4 text-slate-500" />
                <h3 className="text-xl font-medium text-white mb-2">No Goals Set</h3>
                <p className="text-slate-400 mb-4">Set behavioral goals to track your trading psychology improvement</p>
                <button className="px-6 py-3 bg-blue-600 hover:bg-blue-700 rounded-xl font-medium text-white transition-colors">
                  Create Goal
                </button>
              </div>
            )}
          </motion.div>
        )}
      </AnimatePresence>

      {/* Pattern detail modal */}
      <AnimatePresence>
        {selectedPattern && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4"
            onClick={() => setSelectedPattern(null)}
          >
            <motion.div
              initial={{ opacity: 0, scale: 0.9, y: 20 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.9, y: 20 }}
              className="glass-card rounded-2xl p-6 max-w-2xl w-full max-h-[80vh] overflow-y-auto"
              onClick={(e) => e.stopPropagation()}
            >
              <div className="flex items-center justify-between mb-6">
                <h3 className="text-2xl font-bold text-white capitalize">
                  {selectedPattern.type.replace('-', ' ')} Pattern
                </h3>
                <button
                  onClick={() => setSelectedPattern(null)}
                  className="p-2 hover:bg-slate-700/50 rounded-lg text-slate-400 hover:text-white transition-colors"
                >
                  ×
                </button>
              </div>
              
              <div className="space-y-6">
                <div>
                  <h4 className="font-semibold text-white mb-2">Pattern Analysis</h4>
                  <p className="text-slate-300 mb-4">{selectedPattern.description}</p>
                  
                  <div className="grid grid-cols-2 gap-4 p-4 bg-slate-800/50 rounded-xl">
                    <div>
                      <span className="text-sm text-slate-400">Frequency</span>
                      <div className="text-lg font-semibold text-white">
                        {selectedPattern.frequency}/week
                      </div>
                    </div>
                    <div>
                      <span className="text-sm text-slate-400">Impact</span>
                      <div className={`text-lg font-semibold capitalize ${
                        selectedPattern.impact === 'positive' ? 'text-green-400' :
                        selectedPattern.impact === 'negative' ? 'text-red-400' :
                        'text-slate-400'
                      }`}>
                        {selectedPattern.impact}
                      </div>
                    </div>
                  </div>
                </div>
                
                {selectedPattern.examples.length > 0 && (
                  <div>
                    <h4 className="font-semibold text-white mb-3">Recent Examples</h4>
                    <div className="space-y-2">
                      {selectedPattern.examples.map((example, index) => (
                        <div key={index} className="p-3 bg-slate-800/50 rounded-lg">
                          <p className="text-slate-300 text-sm">{example}</p>
                        </div>
                      ))}
                    </div>
                  </div>
                )}
                
                <div>
                  <h4 className="font-semibold text-white mb-2">AI Recommendation</h4>
                  <div className="p-4 bg-blue-500/10 border border-blue-400/30 rounded-xl">
                    <div className="flex items-start space-x-3">
                      <Lightbulb className="w-5 h-5 text-blue-400 flex-shrink-0 mt-0.5" />
                      <p className="text-slate-300">{selectedPattern.recommendation}</p>
                    </div>
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

export default BehavioralInsights