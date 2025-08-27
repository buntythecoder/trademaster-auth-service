import React, { useState } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { 
  Target, 
  Calendar, 
  TrendingUp, 
  AlertTriangle, 
  CheckCircle2,
  Plus,
  Edit3,
  Trash2,
  DollarSign,
  Clock
} from 'lucide-react'
import { PortfolioData, PortfolioGoal } from '../../hooks/usePortfolioWebSocket'
import { useToast } from '../../contexts/ToastContext'
import { cn } from '../../lib/utils'

interface PortfolioGoalTrackerProps {
  portfolio: PortfolioData | null
}

export function PortfolioGoalTracker({ portfolio }: PortfolioGoalTrackerProps) {
  const [selectedGoal, setSelectedGoal] = useState<string | null>(null)
  const [showAddGoal, setShowAddGoal] = useState(false)
  const { success, info } = useToast()

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      notation: amount > 1000000 ? 'compact' : 'standard',
      minimumFractionDigits: 0,
      maximumFractionDigits: 1,
    }).format(amount)
  }

  const calculateMonthsRemaining = (targetDate: Date) => {
    const now = new Date()
    const diffTime = targetDate.getTime() - now.getTime()
    const diffMonths = Math.ceil(diffTime / (1000 * 60 * 60 * 24 * 30))
    return Math.max(0, diffMonths)
  }

  const calculateRequiredMonthlyInvestment = (goal: PortfolioGoal) => {
    const monthsRemaining = calculateMonthsRemaining(goal.targetDate)
    if (monthsRemaining === 0) return 0
    
    const remainingAmount = goal.targetValue - goal.currentValue
    return Math.max(0, remainingAmount / monthsRemaining)
  }

  const getGoalStatus = (goal: PortfolioGoal) => {
    const progress = (goal.currentValue / goal.targetValue) * 100
    const monthsRemaining = calculateMonthsRemaining(goal.targetDate)
    const requiredMonthly = calculateRequiredMonthlyInvestment(goal)
    
    if (progress >= 100) {
      return { status: 'completed', message: 'Goal achieved!', color: 'green' }
    } else if (monthsRemaining === 0) {
      return { status: 'overdue', message: 'Target date passed', color: 'red' }
    } else if (requiredMonthly <= goal.monthlyInvestment * 1.1) {
      return { status: 'on_track', message: 'On track', color: 'green' }
    } else if (requiredMonthly <= goal.monthlyInvestment * 1.5) {
      return { status: 'warning', message: 'Needs attention', color: 'yellow' }
    } else {
      return { status: 'behind', message: 'Behind schedule', color: 'red' }
    }
  }

  const editGoal = (goalId: string) => {
    info('Goal Editor', 'Opening goal editor...')
  }

  const deleteGoal = (goalId: string) => {
    info('Goal Deleted', 'Goal has been removed from tracking')
  }

  const addNewGoal = () => {
    setShowAddGoal(true)
    info('New Goal', 'Opening goal creation form...')
  }

  if (!portfolio || !portfolio.goals?.length) {
    return (
      <div className="glass-card rounded-2xl p-6">
        <div className="text-center space-y-4">
          <div className="w-16 h-16 bg-slate-700/50 rounded-full flex items-center justify-center mx-auto">
            <Target className="w-8 h-8 text-slate-500" />
          </div>
          <div>
            <h3 className="text-lg font-semibold text-white mb-2">Set Your Financial Goals</h3>
            <p className="text-slate-400 mb-4">Track your progress toward important financial milestones</p>
            <button
              onClick={addNewGoal}
              className="cyber-button px-6 py-3 rounded-xl flex items-center space-x-2 mx-auto"
            >
              <Plus className="w-4 h-4" />
              <span>Create First Goal</span>
            </button>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      {portfolio.goals.map((goal, index) => {
        const progress = (goal.currentValue / goal.targetValue) * 100
        const monthsRemaining = calculateMonthsRemaining(goal.targetDate)
        const requiredMonthly = calculateRequiredMonthlyInvestment(goal)
        const goalStatus = getGoalStatus(goal)
        const isSelected = selectedGoal === goal.id

        return (
          <motion.div
            key={goal.id}
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: index * 0.1 }}
            className={cn(
              "glass-card rounded-2xl p-6 cursor-pointer transition-all",
              isSelected && "ring-2 ring-cyan-500/50"
            )}
            onClick={() => setSelectedGoal(isSelected ? null : goal.id)}
          >
            {/* Goal Header */}
            <div className="flex items-center justify-between mb-6">
              <div className="flex items-center space-x-3">
                <div className={cn(
                  "p-3 rounded-xl",
                  goalStatus.color === 'green' && "bg-green-500/20 text-green-400",
                  goalStatus.color === 'yellow' && "bg-yellow-500/20 text-yellow-400",
                  goalStatus.color === 'red' && "bg-red-500/20 text-red-400"
                )}>
                  <Target className="w-6 h-6" />
                </div>
                <div>
                  <h3 className="text-xl font-bold text-white">{goal.name}</h3>
                  <div className="flex items-center space-x-4 text-sm text-slate-400">
                    <div className="flex items-center space-x-1">
                      <Calendar className="w-4 h-4" />
                      <span>{goal.targetDate.toLocaleDateString('en-IN')}</span>
                    </div>
                    <div className={cn(
                      "px-2 py-1 rounded text-xs font-medium",
                      goalStatus.color === 'green' && "bg-green-500/20 text-green-400",
                      goalStatus.color === 'yellow' && "bg-yellow-500/20 text-yellow-400",
                      goalStatus.color === 'red' && "bg-red-500/20 text-red-400"
                    )}>
                      {goalStatus.message}
                    </div>
                  </div>
                </div>
              </div>

              {/* Goal Actions */}
              <div className="flex items-center space-x-2">
                <button
                  onClick={(e) => {
                    e.stopPropagation()
                    editGoal(goal.id)
                  }}
                  className="p-2 rounded-lg glass-card text-slate-400 hover:text-white transition-colors"
                >
                  <Edit3 className="w-4 h-4" />
                </button>
                <button
                  onClick={(e) => {
                    e.stopPropagation()
                    deleteGoal(goal.id)
                  }}
                  className="p-2 rounded-lg glass-card text-slate-400 hover:text-red-400 transition-colors"
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              </div>
            </div>

            {/* Progress Section */}
            <div className="space-y-4">
              {/* Target Amount and Current Value */}
              <div className="grid grid-cols-2 gap-6">
                <div className="text-center">
                  <div className="text-3xl font-bold text-white mb-1">
                    {formatCurrency(goal.targetValue)}
                  </div>
                  <div className="text-sm text-slate-400">Target Amount</div>
                </div>
                <div className="text-center">
                  <div className="text-3xl font-bold text-cyan-400 mb-1">
                    {formatCurrency(goal.currentValue)}
                  </div>
                  <div className="text-sm text-slate-400">Current Value</div>
                </div>
              </div>

              {/* Progress Bar */}
              <div className="space-y-2">
                <div className="flex justify-between text-sm">
                  <span className="text-slate-400">Progress</span>
                  <span className={cn(
                    "font-medium",
                    goalStatus.color === 'green' && "text-green-400",
                    goalStatus.color === 'yellow' && "text-yellow-400",
                    goalStatus.color === 'red' && "text-red-400"
                  )}>
                    {Math.min(progress, 100).toFixed(1)}% Complete
                  </span>
                </div>
                
                <div className="relative">
                  <div className="w-full bg-slate-700 rounded-full h-4 overflow-hidden">
                    <motion.div
                      className={cn(
                        "h-full rounded-full transition-all duration-1000",
                        goalStatus.color === 'green' && "bg-gradient-to-r from-green-500 to-green-400",
                        goalStatus.color === 'yellow' && "bg-gradient-to-r from-yellow-500 to-yellow-400",
                        goalStatus.color === 'red' && "bg-gradient-to-r from-red-500 to-red-400"
                      )}
                      initial={{ width: 0 }}
                      animate={{ width: `${Math.min(progress, 100)}%` }}
                      transition={{ duration: 1, delay: index * 0.2 }}
                    />
                  </div>
                  
                  {/* Progress indicators */}
                  <div className="absolute -bottom-6 left-0 right-0 flex justify-between text-xs text-slate-500">
                    <span>0%</span>
                    <span>25%</span>
                    <span>50%</span>
                    <span>75%</span>
                    <span>100%</span>
                  </div>
                </div>
              </div>

              {/* Time and Investment Stats */}
              <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 pt-4">
                <div className="text-center">
                  <div className="text-lg font-bold text-white mb-1">
                    {monthsRemaining}
                  </div>
                  <div className="text-xs text-slate-400">Months Left</div>
                </div>
                
                <div className="text-center">
                  <div className="text-lg font-bold text-cyan-400 mb-1">
                    {formatCurrency(goal.monthlyInvestment)}
                  </div>
                  <div className="text-xs text-slate-400">Current SIP</div>
                </div>
                
                <div className="text-center">
                  <div className={cn(
                    "text-lg font-bold mb-1",
                    requiredMonthly <= goal.monthlyInvestment ? "text-green-400" : "text-orange-400"
                  )}>
                    {formatCurrency(requiredMonthly)}
                  </div>
                  <div className="text-xs text-slate-400">Required SIP</div>
                </div>
                
                <div className="text-center">
                  <div className="text-lg font-bold text-purple-400 mb-1">
                    {formatCurrency(goal.targetValue - goal.currentValue)}
                  </div>
                  <div className="text-xs text-slate-400">Remaining</div>
                </div>
              </div>
            </div>

            {/* Recommendation */}
            <div className="mt-6 pt-6 border-t border-slate-700/50">
              <div className={cn(
                "p-4 rounded-xl border",
                goalStatus.status === 'completed' && "bg-green-500/10 border-green-500/20",
                goalStatus.status === 'on_track' && "bg-green-500/10 border-green-500/20",
                goalStatus.status === 'warning' && "bg-yellow-500/10 border-yellow-500/20",
                goalStatus.status === 'behind' && "bg-red-500/10 border-red-500/20",
                goalStatus.status === 'overdue' && "bg-red-500/10 border-red-500/20"
              )}>
                <div className="flex items-start space-x-3">
                  <div className={cn(
                    goalStatus.status === 'completed' && "text-green-400",
                    goalStatus.status === 'on_track' && "text-green-400",
                    goalStatus.status === 'warning' && "text-yellow-400",
                    (goalStatus.status === 'behind' || goalStatus.status === 'overdue') && "text-red-400"
                  )}>
                    {goalStatus.status === 'completed' ? (
                      <CheckCircle2 className="w-5 h-5" />
                    ) : goalStatus.status === 'on_track' ? (
                      <TrendingUp className="w-5 h-5" />
                    ) : (
                      <AlertTriangle className="w-5 h-5" />
                    )}
                  </div>
                  
                  <div className="flex-1 min-w-0">
                    <div className={cn(
                      "font-medium mb-1",
                      goalStatus.status === 'completed' && "text-green-400",
                      goalStatus.status === 'on_track' && "text-green-400",
                      goalStatus.status === 'warning' && "text-yellow-400",
                      (goalStatus.status === 'behind' || goalStatus.status === 'overdue') && "text-red-400"
                    )}>
                      {goalStatus.status === 'completed' && 'Congratulations! Goal Achieved! 🎉'}
                      {goalStatus.status === 'on_track' && 'Excellent! You\'re on track to meet your goal.'}
                      {goalStatus.status === 'warning' && `Consider increasing your SIP to ${formatCurrency(requiredMonthly)}`}
                      {goalStatus.status === 'behind' && `Increase your SIP to ${formatCurrency(requiredMonthly)} to stay on track`}
                      {goalStatus.status === 'overdue' && 'Target date has passed. Consider revising your goal timeline.'}
                    </div>
                    
                    <div className="text-sm text-slate-300">
                      {goalStatus.status === 'completed' && 'You can now set a new financial goal or increase this target.'}
                      {goalStatus.status === 'on_track' && `Keep investing ${formatCurrency(goal.monthlyInvestment)} monthly to achieve your target.`}
                      {goalStatus.status === 'warning' && 'A slight increase in monthly investment will help you stay on track.'}
                      {goalStatus.status === 'behind' && 'Significant increase in monthly investment is needed to meet the timeline.'}
                      {goalStatus.status === 'overdue' && 'Review and update your goal with a realistic timeline and investment amount.'}
                    </div>
                  </div>
                </div>
              </div>
            </div>

            {/* Expanded Details */}
            <AnimatePresence>
              {isSelected && (
                <motion.div
                  initial={{ opacity: 0, height: 0 }}
                  animate={{ opacity: 1, height: 'auto' }}
                  exit={{ opacity: 0, height: 0 }}
                  className="mt-6 pt-6 border-t border-slate-700/50 space-y-4"
                >
                  {/* Projection Chart Placeholder */}
                  <div className="glass-card p-4 rounded-xl">
                    <h4 className="text-white font-medium mb-3 flex items-center space-x-2">
                      <TrendingUp className="w-4 h-4 text-cyan-400" />
                      <span>Growth Projection</span>
                    </h4>
                    <div className="h-32 bg-slate-900/50 rounded-lg flex items-center justify-center">
                      <div className="text-slate-400 text-sm">Interactive projection chart would appear here</div>
                    </div>
                  </div>

                  {/* Goal Milestones */}
                  <div className="glass-card p-4 rounded-xl">
                    <h4 className="text-white font-medium mb-3 flex items-center space-x-2">
                      <Clock className="w-4 h-4 text-purple-400" />
                      <span>Milestones</span>
                    </h4>
                    <div className="space-y-3">
                      {[25, 50, 75, 100].map((milestone) => {
                        const milestoneValue = (goal.targetValue * milestone) / 100
                        const isReached = goal.currentValue >= milestoneValue
                        
                        return (
                          <div key={milestone} className="flex items-center space-x-3">
                            <div className={cn(
                              "w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold",
                              isReached 
                                ? "bg-green-500/20 text-green-400 border-2 border-green-500" 
                                : "bg-slate-700 text-slate-400 border-2 border-slate-600"
                            )}>
                              {isReached ? '✓' : milestone}
                            </div>
                            <div className="flex-1">
                              <div className="text-white text-sm font-medium">
                                {milestone}% - {formatCurrency(milestoneValue)}
                              </div>
                              <div className="text-slate-400 text-xs">
                                {isReached ? 'Achieved' : 'Pending'}
                              </div>
                            </div>
                          </div>
                        )
                      })}
                    </div>
                  </div>
                </motion.div>
              )}
            </AnimatePresence>
          </motion.div>
        )
      })}

      {/* Add New Goal Button */}
      <div className="text-center">
        <button
          onClick={addNewGoal}
          className="glass-card px-6 py-3 rounded-xl text-slate-400 hover:text-white transition-colors flex items-center space-x-2 mx-auto"
        >
          <Plus className="w-4 h-4" />
          <span>Add New Goal</span>
        </button>
      </div>
    </div>
  )
}