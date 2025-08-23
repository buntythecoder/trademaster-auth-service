import React, { useState, useEffect } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { 
  Trophy,
  Star,
  Target,
  Shield,
  Zap,
  TrendingUp,
  Award,
  Crown,
  Gem,
  Flame,
  CheckCircle,
  Lock,
  X,
  Gift,
  BarChart3,
  DollarSign,
  Users,
  BookOpen,
  Settings,
  Camera,
  MessageSquare,
  Calendar,
  Clock
} from 'lucide-react'
import { cn } from '@/lib/utils'

interface Achievement {
  id: string
  title: string
  description: string
  icon: React.ReactNode
  category: 'onboarding' | 'trading' | 'portfolio' | 'social' | 'learning' | 'security'
  difficulty: 'bronze' | 'silver' | 'gold' | 'platinum' | 'diamond'
  points: number
  unlocked: boolean
  unlockedAt?: Date
  progress?: {
    current: number
    target: number
    unit: string
  }
  requirements?: string[]
  reward?: {
    type: 'badge' | 'feature' | 'discount' | 'bonus'
    value: string
  }
  isNew?: boolean
}

interface AchievementNotificationProps {
  achievement: Achievement
  onClose: () => void
}

interface AchievementSystemProps {
  achievements: Achievement[]
  onAchievementUnlock?: (achievementId: string) => void
}

// Achievement definitions
export const ACHIEVEMENTS: Record<string, Omit<Achievement, 'unlocked' | 'unlockedAt' | 'isNew'>> = {
  // Onboarding Achievements
  'welcome-aboard': {
    id: 'welcome-aboard',
    title: 'Welcome Aboard!',
    description: 'Successfully joined TradeMaster platform',
    icon: <Trophy className="w-6 h-6" />,
    category: 'onboarding',
    difficulty: 'bronze',
    points: 100,
    reward: { type: 'badge', value: 'New Member' }
  },
  'goal-setter': {
    id: 'goal-setter',
    title: 'Goal Setter',
    description: 'Set your trading goals during onboarding',
    icon: <Target className="w-6 h-6" />,
    category: 'onboarding',
    difficulty: 'bronze',
    points: 150,
    reward: { type: 'feature', value: 'Goal Tracking Dashboard' }
  },
  'security-champion': {
    id: 'security-champion',
    title: 'Security Champion',
    description: 'Enable two-factor authentication',
    icon: <Shield className="w-6 h-6" />,
    category: 'security',
    difficulty: 'silver',
    points: 250,
    reward: { type: 'discount', value: '10% off premium features' }
  },
  'verified-trader': {
    id: 'verified-trader',
    title: 'Verified Trader',
    description: 'Complete KYC verification process',
    icon: <CheckCircle className="w-6 h-6" />,
    category: 'onboarding',
    difficulty: 'gold',
    points: 500,
    reward: { type: 'feature', value: 'Higher trading limits' }
  },

  // Trading Achievements
  'first-trade': {
    id: 'first-trade',
    title: 'First Trade',
    description: 'Execute your first successful trade',
    icon: <Zap className="w-6 h-6" />,
    category: 'trading',
    difficulty: 'bronze',
    points: 200,
    reward: { type: 'bonus', value: '₹10 cashback' }
  },
  'day-trader': {
    id: 'day-trader',
    title: 'Day Trader',
    description: 'Complete 10 trades in a single day',
    icon: <TrendingUp className="w-6 h-6" />,
    category: 'trading',
    difficulty: 'silver',
    points: 750,
    progress: { current: 0, target: 10, unit: 'trades' },
    reward: { type: 'feature', value: 'Advanced order types' }
  },
  'profit-master': {
    id: 'profit-master',
    title: 'Profit Master',
    description: 'Achieve 10% portfolio gain',
    icon: <Crown className="w-6 h-6" />,
    category: 'trading',
    difficulty: 'gold',
    points: 1000,
    progress: { current: 0, target: 10, unit: '% gain' },
    reward: { type: 'badge', value: 'Profit Master' }
  },
  'diamond-hands': {
    id: 'diamond-hands',
    title: 'Diamond Hands',
    description: 'Hold a position for 30+ days',
    icon: <Gem className="w-6 h-6" />,
    category: 'trading',
    difficulty: 'platinum',
    points: 1500,
    progress: { current: 0, target: 30, unit: 'days' },
    reward: { type: 'badge', value: 'Diamond Hands' }
  },

  // Portfolio Achievements
  'diversified-investor': {
    id: 'diversified-investor',
    title: 'Diversified Investor',
    description: 'Hold stocks from 5 different sectors',
    icon: <BarChart3 className="w-6 h-6" />,
    category: 'portfolio',
    difficulty: 'silver',
    points: 600,
    progress: { current: 0, target: 5, unit: 'sectors' },
    reward: { type: 'feature', value: 'Sector analysis tools' }
  },
  'high-roller': {
    id: 'high-roller',
    title: 'High Roller',
    description: 'Portfolio value exceeds ₹1 Lakh',
    icon: <DollarSign className="w-6 h-6" />,
    category: 'portfolio',
    difficulty: 'gold',
    points: 1200,
    progress: { current: 0, target: 100000, unit: '₹' },
    reward: { type: 'feature', value: 'Premium analytics' }
  },

  // Learning Achievements
  'student': {
    id: 'student',
    title: 'Student',
    description: 'Complete your first tutorial',
    icon: <BookOpen className="w-6 h-6" />,
    category: 'learning',
    difficulty: 'bronze',
    points: 100,
    reward: { type: 'feature', value: 'Advanced tutorials' }
  },
  'knowledge-seeker': {
    id: 'knowledge-seeker',
    title: 'Knowledge Seeker',
    description: 'Complete all onboarding tutorials',
    icon: <Star className="w-6 h-6" />,
    category: 'learning',
    difficulty: 'silver',
    points: 500,
    progress: { current: 0, target: 5, unit: 'tutorials' },
    reward: { type: 'badge', value: 'Knowledge Seeker' }
  },

  // Social Achievements
  'early-adopter': {
    id: 'early-adopter',
    title: 'Early Adopter',
    description: 'Join TradeMaster in beta phase',
    icon: <Flame className="w-6 h-6" />,
    category: 'social',
    difficulty: 'platinum',
    points: 2000,
    reward: { type: 'badge', value: 'Early Adopter' }
  }
}

// Achievement notification component
const AchievementNotification: React.FC<AchievementNotificationProps> = ({ 
  achievement, 
  onClose 
}) => {
  const [isVisible, setIsVisible] = useState(true)

  useEffect(() => {
    const timer = setTimeout(() => {
      setIsVisible(false)
      setTimeout(onClose, 300)
    }, 5000)

    return () => clearTimeout(timer)
  }, [onClose])

  const getDifficultyColor = (difficulty: Achievement['difficulty']) => {
    switch (difficulty) {
      case 'bronze': return 'from-amber-600 to-orange-600'
      case 'silver': return 'from-gray-400 to-gray-600'
      case 'gold': return 'from-yellow-400 to-yellow-600'
      case 'platinum': return 'from-purple-400 to-purple-600'
      case 'diamond': return 'from-blue-400 to-cyan-400'
      default: return 'from-gray-400 to-gray-600'
    }
  }

  return (
    <AnimatePresence>
      {isVisible && (
        <motion.div
          initial={{ opacity: 0, scale: 0.8, y: 50 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          exit={{ opacity: 0, scale: 0.8, y: -50 }}
          className="fixed bottom-6 right-6 z-50 max-w-sm"
        >
          <div className="glass-card rounded-2xl p-6 border-2 border-yellow-400/50 bg-gradient-to-br from-yellow-400/10 to-orange-400/10">
            {/* Header */}
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center gap-3">
                <div className={cn(
                  "p-3 rounded-xl bg-gradient-to-br",
                  getDifficultyColor(achievement.difficulty),
                  "shadow-lg"
                )}>
                  {achievement.icon}
                </div>
                <div>
                  <h3 className="font-bold text-white text-lg">Achievement Unlocked!</h3>
                  <p className="text-yellow-400 text-sm font-medium capitalize">
                    {achievement.difficulty} • +{achievement.points} XP
                  </p>
                </div>
              </div>
              <button
                onClick={() => setIsVisible(false)}
                className="text-slate-400 hover:text-white transition-colors"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Content */}
            <div className="mb-4">
              <h4 className="font-semibold text-white text-xl mb-2">
                {achievement.title}
              </h4>
              <p className="text-slate-300 text-sm">
                {achievement.description}
              </p>
            </div>

            {/* Reward */}
            {achievement.reward && (
              <div className="bg-slate-800/50 rounded-lg p-3 border border-slate-700">
                <div className="flex items-center gap-2">
                  <Gift className="w-4 h-4 text-green-400" />
                  <span className="text-green-400 font-medium text-sm">Reward:</span>
                </div>
                <p className="text-slate-300 text-sm mt-1">
                  {achievement.reward.value}
                </p>
              </div>
            )}

            {/* Celebration Effect */}
            <motion.div
              initial={{ scale: 0 }}
              animate={{ scale: [0, 1.2, 1] }}
              transition={{ delay: 0.2, duration: 0.6 }}
              className="absolute -top-2 -right-2"
            >
              <div className="w-8 h-8 bg-yellow-400 rounded-full flex items-center justify-center">
                <Star className="w-4 h-4 text-white" />
              </div>
            </motion.div>
          </div>
        </motion.div>
      )}
    </AnimatePresence>
  )
}

// Main Achievement System component
export const AchievementSystem: React.FC<AchievementSystemProps> = ({ 
  achievements, 
  onAchievementUnlock 
}) => {
  const [notifications, setNotifications] = useState<Achievement[]>([])
  const [showAchievements, setShowAchievements] = useState(false)

  // Check for new achievements
  useEffect(() => {
    const newAchievements = achievements.filter(a => a.isNew && a.unlocked)
    if (newAchievements.length > 0) {
      setNotifications(prev => [...prev, ...newAchievements])
      
      // Mark as not new after showing notification
      setTimeout(() => {
        newAchievements.forEach(achievement => {
          onAchievementUnlock?.(achievement.id)
        })
      }, 5000)
    }
  }, [achievements, onAchievementUnlock])

  const removeNotification = (achievementId: string) => {
    setNotifications(prev => prev.filter(a => a.id !== achievementId))
  }

  const getProgressPercentage = (achievement: Achievement) => {
    if (!achievement.progress) return 100
    return Math.min((achievement.progress.current / achievement.progress.target) * 100, 100)
  }

  const getCategoryIcon = (category: Achievement['category']) => {
    switch (category) {
      case 'onboarding': return <Settings className="w-5 h-5" />
      case 'trading': return <TrendingUp className="w-5 h-5" />
      case 'portfolio': return <BarChart3 className="w-5 h-5" />
      case 'learning': return <BookOpen className="w-5 h-5" />
      case 'security': return <Shield className="w-5 h-5" />
      case 'social': return <Users className="w-5 h-5" />
      default: return <Trophy className="w-5 h-5" />
    }
  }

  const getDifficultyColor = (difficulty: Achievement['difficulty']) => {
    switch (difficulty) {
      case 'bronze': return 'text-amber-400 border-amber-400/30 bg-amber-400/10'
      case 'silver': return 'text-gray-300 border-gray-400/30 bg-gray-400/10'
      case 'gold': return 'text-yellow-400 border-yellow-400/30 bg-yellow-400/10'
      case 'platinum': return 'text-purple-400 border-purple-400/30 bg-purple-400/10'
      case 'diamond': return 'text-cyan-400 border-cyan-400/30 bg-cyan-400/10'
      default: return 'text-gray-400 border-gray-400/30 bg-gray-400/10'
    }
  }

  const totalXP = achievements
    .filter(a => a.unlocked)
    .reduce((sum, a) => sum + a.points, 0)

  const unlockedCount = achievements.filter(a => a.unlocked).length

  return (
    <>
      {/* Achievement Notifications */}
      <div className="fixed bottom-0 right-0 z-50 p-6 space-y-4">
        {notifications.map((achievement) => (
          <AchievementNotification
            key={achievement.id}
            achievement={achievement}
            onClose={() => removeNotification(achievement.id)}
          />
        ))}
      </div>

      {/* Achievement Panel (when opened) */}
      <AnimatePresence>
        {showAchievements && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 bg-black/80 z-40 flex items-center justify-center p-6"
            onClick={() => setShowAchievements(false)}
          >
            <motion.div
              initial={{ scale: 0.9, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.9, opacity: 0 }}
              className="glass-card rounded-3xl p-8 max-w-4xl w-full max-h-[90vh] overflow-y-auto"
              onClick={(e) => e.stopPropagation()}
            >
              {/* Header */}
              <div className="flex items-center justify-between mb-8">
                <div>
                  <h2 className="text-3xl font-bold gradient-text mb-2">Achievements</h2>
                  <div className="flex items-center gap-6 text-slate-400">
                    <span>{unlockedCount}/{achievements.length} Unlocked</span>
                    <span>•</span>
                    <span>{totalXP.toLocaleString()} XP Total</span>
                  </div>
                </div>
                <button
                  onClick={() => setShowAchievements(false)}
                  className="cyber-button cyber-button-ghost p-3"
                >
                  <X className="w-6 h-6" />
                </button>
              </div>

              {/* Achievement Grid */}
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {achievements.map((achievement) => (
                  <motion.div
                    key={achievement.id}
                    whileHover={{ scale: achievement.unlocked ? 1.02 : 1 }}
                    className={cn(
                      "glass-card p-6 rounded-2xl border-2 transition-all duration-300",
                      achievement.unlocked 
                        ? "border-green-400/30 bg-green-400/5" 
                        : "border-slate-700 opacity-70"
                    )}
                  >
                    {/* Achievement Icon */}
                    <div className="flex items-start justify-between mb-4">
                      <div className={cn(
                        "p-4 rounded-xl",
                        achievement.unlocked 
                          ? getDifficultyColor(achievement.difficulty)
                          : "bg-slate-700/50 text-slate-500"
                      )}>
                        {achievement.unlocked ? achievement.icon : <Lock className="w-6 h-6" />}
                      </div>
                      <div className="flex items-center gap-2">
                        {getCategoryIcon(achievement.category)}
                        <span className={cn(
                          "px-2 py-1 rounded-full text-xs font-medium capitalize border",
                          getDifficultyColor(achievement.difficulty)
                        )}>
                          {achievement.difficulty}
                        </span>
                      </div>
                    </div>

                    {/* Achievement Details */}
                    <div className="mb-4">
                      <h3 className={cn(
                        "font-semibold text-lg mb-2",
                        achievement.unlocked ? "text-white" : "text-slate-500"
                      )}>
                        {achievement.title}
                      </h3>
                      <p className="text-slate-400 text-sm">
                        {achievement.description}
                      </p>
                    </div>

                    {/* Progress Bar (if applicable) */}
                    {achievement.progress && !achievement.unlocked && (
                      <div className="mb-4">
                        <div className="flex justify-between text-xs text-slate-400 mb-2">
                          <span>Progress</span>
                          <span>
                            {achievement.progress.current.toLocaleString()} / {achievement.progress.target.toLocaleString()} {achievement.progress.unit}
                          </span>
                        </div>
                        <div className="h-2 bg-slate-800 rounded-full overflow-hidden">
                          <div 
                            className="h-full bg-gradient-to-r from-blue-500 to-cyan-400 transition-all duration-300"
                            style={{ width: `${getProgressPercentage(achievement)}%` }}
                          />
                        </div>
                      </div>
                    )}

                    {/* Reward */}
                    {achievement.reward && (
                      <div className={cn(
                        "p-3 rounded-lg border text-sm",
                        achievement.unlocked 
                          ? "bg-green-400/10 border-green-400/30" 
                          : "bg-slate-800/50 border-slate-700"
                      )}>
                        <div className="flex items-center gap-2 mb-1">
                          <Gift className={cn(
                            "w-4 h-4",
                            achievement.unlocked ? "text-green-400" : "text-slate-500"
                          )} />
                          <span className={cn(
                            "font-medium",
                            achievement.unlocked ? "text-green-400" : "text-slate-500"
                          )}>
                            Reward:
                          </span>
                        </div>
                        <p className="text-slate-300">
                          {achievement.reward.value}
                        </p>
                      </div>
                    )}

                    {/* XP Points */}
                    <div className="flex items-center justify-between mt-4 pt-4 border-t border-slate-700">
                      <span className="text-slate-400 text-sm">
                        +{achievement.points} XP
                      </span>
                      {achievement.unlocked && achievement.unlockedAt && (
                        <div className="flex items-center gap-1 text-xs text-slate-400">
                          <Clock className="w-3 h-3" />
                          {achievement.unlockedAt.toLocaleDateString()}
                        </div>
                      )}
                    </div>
                  </motion.div>
                ))}
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Achievement Button (for opening panel) */}
      <button
        onClick={() => setShowAchievements(true)}
        className="fixed bottom-6 left-6 cyber-button cyber-button-primary p-4 rounded-full shadow-lg"
        title="View Achievements"
      >
        <div className="relative">
          <Trophy className="w-6 h-6" />
          {notifications.length > 0 && (
            <div className="absolute -top-2 -right-2 w-5 h-5 bg-red-500 rounded-full flex items-center justify-center">
              <span className="text-white text-xs font-bold">
                {notifications.length}
              </span>
            </div>
          )}
        </div>
      </button>
    </>
  )
}

export default AchievementSystem