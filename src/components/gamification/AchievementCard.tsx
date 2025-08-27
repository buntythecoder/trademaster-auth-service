import React from 'react'
import { motion } from 'framer-motion'
import { 
  Award, Star, Trophy, Target, Zap, Shield, 
  TrendingUp, DollarSign, Clock, Users,
  Lock, CheckCircle, BarChart3, Calendar
} from 'lucide-react'

export interface Achievement {
  id: string
  title: string
  description: string
  category: 'trading' | 'learning' | 'social' | 'milestone' | 'streak'
  rarity: 'common' | 'rare' | 'epic' | 'legendary'
  points: number
  icon: string
  unlocked: boolean
  unlockedAt?: Date
  progress: number
  maxProgress: number
  requirements?: string[]
  reward?: {
    type: 'points' | 'badge' | 'discount' | 'feature'
    value: string
  }
}

interface AchievementCardProps {
  achievement: Achievement
  showProgress?: boolean
  interactive?: boolean
  onClaim?: (achievement: Achievement) => void
  className?: string
}

const iconMap = {
  award: Award,
  star: Star,
  trophy: Trophy,
  target: Target,
  zap: Zap,
  shield: Shield,
  trending: TrendingUp,
  dollar: DollarSign,
  clock: Clock,
  users: Users,
  chart: BarChart3,
  calendar: Calendar
}

const rarityConfig = {
  common: {
    gradient: 'from-slate-500 to-slate-600',
    glow: 'shadow-slate-500/20',
    border: 'border-slate-400/30',
    text: 'text-slate-300'
  },
  rare: {
    gradient: 'from-blue-500 to-blue-600',
    glow: 'shadow-blue-500/30',
    border: 'border-blue-400/30',
    text: 'text-blue-300'
  },
  epic: {
    gradient: 'from-purple-500 to-purple-600',
    glow: 'shadow-purple-500/30',
    border: 'border-purple-400/30',
    text: 'text-purple-300'
  },
  legendary: {
    gradient: 'from-yellow-500 to-orange-500',
    glow: 'shadow-yellow-500/30',
    border: 'border-yellow-400/30',
    text: 'text-yellow-300'
  }
}

export const AchievementCard: React.FC<AchievementCardProps> = ({
  achievement,
  showProgress = true,
  interactive = true,
  onClaim,
  className = ''
}) => {
  const IconComponent = iconMap[achievement.icon as keyof typeof iconMap] || Award
  const rarity = rarityConfig[achievement.rarity]
  const progressPercent = (achievement.progress / achievement.maxProgress) * 100

  const handleClaim = () => {
    if (achievement.unlocked && onClaim) {
      onClaim(achievement)
    }
  }

  return (
    <motion.div
      whileHover={interactive ? { scale: 1.02, y: -2 } : {}}
      whileTap={interactive ? { scale: 0.98 } : {}}
      className={`relative glass-card rounded-2xl p-4 transition-all duration-300 ${
        achievement.unlocked 
          ? `${rarity.glow} ${rarity.border}` 
          : 'border-slate-600/30'
      } ${interactive ? 'cursor-pointer hover:shadow-lg' : ''} ${className}`}
      onClick={handleClaim}
    >
      {/* Rarity indicator */}
      <div className={`absolute top-2 right-2 px-2 py-1 rounded-lg text-xs font-medium capitalize ${
        achievement.unlocked ? rarity.text : 'text-slate-500'
      } bg-black/20`}>
        {achievement.rarity}
      </div>

      {/* Achievement icon */}
      <div className="flex items-center space-x-4 mb-3">
        <div className={`relative w-12 h-12 rounded-xl flex items-center justify-center ${
          achievement.unlocked 
            ? `bg-gradient-to-r ${rarity.gradient}` 
            : 'bg-slate-700/50'
        }`}>
          {achievement.unlocked ? (
            <IconComponent className="w-6 h-6 text-white" />
          ) : (
            <Lock className="w-6 h-6 text-slate-500" />
          )}
          
          {/* Glow effect for unlocked achievements */}
          {achievement.unlocked && (
            <div className={`absolute inset-0 rounded-xl bg-gradient-to-r ${rarity.gradient} opacity-20 blur-sm`} />
          )}
        </div>
        
        <div className="flex-1">
          <div className="flex items-center space-x-2">
            <h3 className={`font-semibold ${
              achievement.unlocked ? 'text-white' : 'text-slate-400'
            }`}>
              {achievement.title}
            </h3>
            {achievement.unlocked && (
              <CheckCircle className="w-4 h-4 text-green-400" />
            )}
          </div>
          <p className="text-sm text-slate-400 mt-1">
            {achievement.description}
          </p>
        </div>
      </div>

      {/* Progress bar */}
      {showProgress && achievement.maxProgress > 1 && (
        <div className="mb-3">
          <div className="flex items-center justify-between mb-1">
            <span className="text-xs text-slate-400">Progress</span>
            <span className="text-xs text-slate-300">
              {achievement.progress}/{achievement.maxProgress}
            </span>
          </div>
          <div className="w-full bg-slate-700/50 rounded-full h-2">
            <motion.div
              initial={{ width: 0 }}
              animate={{ width: `${Math.min(progressPercent, 100)}%` }}
              transition={{ duration: 1, ease: 'easeOut' }}
              className={`h-2 rounded-full ${
                achievement.unlocked 
                  ? `bg-gradient-to-r ${rarity.gradient}` 
                  : 'bg-slate-600'
              }`}
            />
          </div>
        </div>
      )}

      {/* Reward info */}
      {achievement.reward && (
        <div className="flex items-center space-x-2 mb-3">
          <div className="w-6 h-6 rounded-lg bg-yellow-500/20 flex items-center justify-center">
            <Star className="w-3 h-3 text-yellow-400" />
          </div>
          <span className="text-sm text-yellow-400">
            +{achievement.points} points
          </span>
          {achievement.reward.type !== 'points' && (
            <span className="text-sm text-slate-400">
              • {achievement.reward.value}
            </span>
          )}
        </div>
      )}

      {/* Unlock date */}
      {achievement.unlocked && achievement.unlockedAt && (
        <div className="text-xs text-slate-500">
          Unlocked {achievement.unlockedAt.toLocaleDateString()}
        </div>
      )}

      {/* Requirements (for locked achievements) */}
      {!achievement.unlocked && achievement.requirements && (
        <div className="mt-3 p-2 bg-slate-800/50 rounded-lg">
          <h4 className="text-xs font-medium text-slate-300 mb-1">Requirements:</h4>
          <ul className="text-xs text-slate-400 space-y-1">
            {achievement.requirements.map((req, index) => (
              <li key={index} className="flex items-start space-x-2">
                <span>•</span>
                <span>{req}</span>
              </li>
            ))}
          </ul>
        </div>
      )}

      {/* Shine effect for legendary achievements */}
      {achievement.unlocked && achievement.rarity === 'legendary' && (
        <motion.div
          animate={{
            background: [
              'linear-gradient(45deg, transparent 30%, rgba(255,255,255,0.1) 50%, transparent 70%)',
              'linear-gradient(45deg, transparent 30%, rgba(255,255,255,0.2) 50%, transparent 70%)',
              'linear-gradient(45deg, transparent 30%, rgba(255,255,255,0.1) 50%, transparent 70%)'
            ]
          }}
          transition={{ duration: 2, repeat: Infinity, repeatDelay: 3 }}
          className="absolute inset-0 rounded-2xl pointer-events-none"
        />
      )}
    </motion.div>
  )
}

export default AchievementCard