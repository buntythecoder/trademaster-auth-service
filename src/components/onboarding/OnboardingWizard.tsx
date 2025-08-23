import React, { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { motion, AnimatePresence } from 'framer-motion'
import { 
  User, 
  Shield, 
  CreditCard, 
  BookOpen, 
  Trophy, 
  ChevronRight, 
  ChevronLeft,
  CheckCircle,
  Upload,
  Camera,
  FileText,
  Target,
  TrendingUp,
  DollarSign,
  BarChart3,
  Settings,
  Bell,
  Smartphone,
  Mail,
  MessageSquare,
  Star,
  Gift,
  Award,
  Zap
} from 'lucide-react'
import { useAuthStore } from '@/stores/auth.store'
import { KYCDocumentUpload } from './KYCDocumentUpload'
import { MFASetup as AuthMFASetup } from '@/components/auth/MFASetup'
import { cn } from '@/lib/utils'

interface OnboardingStep {
  id: string
  title: string
  subtitle: string
  icon: React.ReactNode
  component: React.ComponentType<OnboardingStepProps>
  isOptional?: boolean
  estimatedTime: string
}

interface OnboardingStepProps {
  onNext: () => void
  onBack: () => void
  onComplete: (data: any) => void
  stepData: any
  isFirst: boolean
  isLast: boolean
}

interface OnboardingProgress {
  currentStep: number
  completedSteps: string[]
  stepData: Record<string, any>
  startedAt: Date
  achievements: string[]
  score: number
}

// Step 1: Welcome and Goal Setting
const WelcomeStep: React.FC<OnboardingStepProps> = ({ onNext, onComplete }) => {
  const { user } = useAuthStore()
  const [selectedGoals, setSelectedGoals] = useState<string[]>([])
  
  const tradingGoals = [
    { id: 'wealth-building', label: 'Build Long-term Wealth', icon: <TrendingUp className="w-6 h-6" />, description: 'Focus on steady growth and compound returns' },
    { id: 'active-trading', label: 'Active Day Trading', icon: <BarChart3 className="w-6 h-6" />, description: 'Short-term trades and quick profits' },
    { id: 'passive-income', label: 'Generate Passive Income', icon: <DollarSign className="w-6 h-6" />, description: 'Dividend stocks and income investing' },
    { id: 'learning', label: 'Learn Trading Skills', icon: <BookOpen className="w-6 h-6" />, description: 'Education and skill development first' },
  ]

  const handleNext = () => {
    onComplete({ goals: selectedGoals })
    onNext()
  }

  return (
    <div className="space-y-8">
      <div className="text-center">
        <div className="cyber-glow w-20 h-20 mx-auto mb-6 rounded-full bg-gradient-to-r from-blue-600 to-purple-600 flex items-center justify-center">
          <Trophy className="w-10 h-10 text-white" />
        </div>
        <h1 className="text-3xl font-bold gradient-text mb-4">
          Welcome to TradeMaster, {user?.firstName}! 🎉
        </h1>
        <p className="text-slate-400 text-lg">
          Let's personalize your trading journey and set you up for success
        </p>
      </div>

      <div className="space-y-6">
        <h2 className="text-xl font-semibold text-white mb-4">What are your primary trading goals?</h2>
        <div className="grid gap-4">
          {tradingGoals.map((goal) => (
            <motion.div
              key={goal.id}
              whileHover={{ scale: 1.02 }}
              whileTap={{ scale: 0.98 }}
              onClick={() => {
                if (selectedGoals.includes(goal.id)) {
                  setSelectedGoals(prev => prev.filter(id => id !== goal.id))
                } else {
                  setSelectedGoals(prev => [...prev, goal.id])
                }
              }}
              className={cn(
                "glass-card p-6 rounded-2xl cursor-pointer transition-all duration-300 border-2",
                selectedGoals.includes(goal.id) 
                  ? "border-cyan-400 bg-cyan-400/10" 
                  : "border-transparent hover:border-slate-600"
              )}
            >
              <div className="flex items-start gap-4">
                <div className={cn(
                  "p-3 rounded-xl",
                  selectedGoals.includes(goal.id) 
                    ? "bg-cyan-400/20 text-cyan-400" 
                    : "bg-slate-700/50 text-slate-400"
                )}>
                  {goal.icon}
                </div>
                <div className="flex-1">
                  <div className="flex items-center justify-between">
                    <h3 className="font-semibold text-white">{goal.label}</h3>
                    {selectedGoals.includes(goal.id) && (
                      <CheckCircle className="w-5 h-5 text-cyan-400" />
                    )}
                  </div>
                  <p className="text-slate-400 text-sm mt-1">{goal.description}</p>
                </div>
              </div>
            </motion.div>
          ))}
        </div>
      </div>

      <button
        onClick={handleNext}
        disabled={selectedGoals.length === 0}
        className="cyber-button w-full py-4 text-lg font-semibold rounded-2xl disabled:opacity-50 flex items-center justify-center"
      >
        Continue Setup <ChevronRight className="w-5 h-5 ml-2" />
      </button>
    </div>
  )
}

// Step 2: KYC Document Upload
const KYCStep: React.FC<OnboardingStepProps> = ({ onNext, onBack, onComplete }) => {
  const [kycData, setKycData] = useState({
    panCard: null,
    aadharCard: null,
    bankStatement: null,
    selfie: null
  })

  const handleKYCComplete = (data: any) => {
    setKycData(data)
    onComplete(data)
    onNext()
  }

  return (
    <div className="space-y-8">
      <div className="text-center">
        <Shield className="w-16 h-16 text-blue-400 mx-auto mb-4" />
        <h1 className="text-2xl font-bold text-white mb-2">Identity Verification</h1>
        <p className="text-slate-400">
          Complete KYC verification to unlock full trading features
        </p>
      </div>

      <KYCDocumentUpload
        onComplete={handleKYCComplete}
        className="bg-transparent"
      />

      <div className="flex gap-4">
        <button
          onClick={onBack}
          className="glass-card px-6 py-3 rounded-xl font-semibold text-white hover:text-purple-300 transition-colors border border-purple-500/50 hover:border-purple-400/70 flex-1 flex items-center justify-center"
        >
          <ChevronLeft className="w-5 h-5 mr-2" /> Back
        </button>
        <button
          onClick={onNext}
          className="cyber-button flex-1 py-3 rounded-xl flex items-center justify-center"
        >
          Skip for Now <ChevronRight className="w-5 h-5 ml-2" />
        </button>
      </div>
    </div>
  )
}

// Step 3: Security Setup (MFA)
const SecurityStep: React.FC<OnboardingStepProps> = ({ onNext, onBack, onComplete }) => {
  const handleMFAComplete = (method: any, verified: boolean) => {
    onComplete({ method, verified, completedAt: new Date() })
    onNext()
  }

  const handleMFASkip = () => {
    onComplete({ mfaSkipped: true, completedAt: new Date() })
    onNext()
  }

  return (
    <div className="space-y-8">
      <div className="text-center">
        <Shield className="w-16 h-16 text-green-400 mx-auto mb-4" />
        <h1 className="text-2xl font-bold text-white mb-2">Secure Your Account</h1>
        <p className="text-slate-400">
          Add an extra layer of security with two-factor authentication
        </p>
      </div>

      <AuthMFASetup
        onComplete={handleMFAComplete}
        onCancel={handleMFASkip}
      />

      <button
        onClick={onBack}
        className="glass-card px-6 py-3 rounded-xl font-semibold text-white hover:text-purple-300 transition-colors border border-purple-500/50 hover:border-purple-400/70 w-full flex items-center justify-center"
      >
        <ChevronLeft className="w-5 h-5 mr-2" /> Back
      </button>
    </div>
  )
}

// Step 4: Preference Setup
const PreferencesStep: React.FC<OnboardingStepProps> = ({ onNext, onBack, onComplete }) => {
  const [preferences, setPreferences] = useState({
    notifications: {
      email: true,
      push: true,
      sms: false,
      marketAlerts: true,
      priceAlerts: true,
      newsUpdates: false
    },
    tradingPreferences: {
      riskTolerance: 'moderate',
      investmentHorizon: 'medium',
      preferredMarkets: ['equity', 'mutual-funds']
    },
    dashboardLayout: 'comprehensive'
  })

  const riskLevels = [
    { id: 'conservative', label: 'Conservative', description: 'Minimal risk, steady returns' },
    { id: 'moderate', label: 'Moderate', description: 'Balanced approach' },
    { id: 'aggressive', label: 'Aggressive', description: 'Higher risk, higher rewards' }
  ]

  const investmentHorizons = [
    { id: 'short', label: 'Short-term', description: '< 1 year' },
    { id: 'medium', label: 'Medium-term', description: '1-5 years' },
    { id: 'long', label: 'Long-term', description: '5+ years' }
  ]

  const handleNext = () => {
    onComplete(preferences)
    onNext()
  }

  return (
    <div className="space-y-8">
      <div className="text-center">
        <Settings className="w-16 h-16 text-purple-400 mx-auto mb-4" />
        <h1 className="text-2xl font-bold text-white mb-2">Customize Your Experience</h1>
        <p className="text-slate-400">
          Set your preferences for a personalized trading experience
        </p>
      </div>

      <div className="space-y-8">
        {/* Risk Tolerance */}
        <div>
          <h3 className="text-lg font-semibold text-white mb-4">Risk Tolerance</h3>
          <div className="grid gap-3">
            {riskLevels.map((risk) => (
              <label key={risk.id} className="glass-card p-4 rounded-xl cursor-pointer hover:bg-slate-800/70">
                <div className="flex items-center gap-3">
                  <input
                    type="radio"
                    name="riskTolerance"
                    value={risk.id}
                    checked={preferences.tradingPreferences.riskTolerance === risk.id}
                    onChange={(e) => setPreferences(prev => ({
                      ...prev,
                      tradingPreferences: {
                        ...prev.tradingPreferences,
                        riskTolerance: e.target.value
                      }
                    }))}
                    className="w-4 h-4 text-cyan-400 focus:ring-cyan-400"
                  />
                  <div>
                    <div className="text-white font-medium">{risk.label}</div>
                    <div className="text-slate-400 text-sm">{risk.description}</div>
                  </div>
                </div>
              </label>
            ))}
          </div>
        </div>

        {/* Investment Horizon */}
        <div>
          <h3 className="text-lg font-semibold text-white mb-4">Investment Horizon</h3>
          <div className="grid gap-3">
            {investmentHorizons.map((horizon) => (
              <label key={horizon.id} className="glass-card p-4 rounded-xl cursor-pointer hover:bg-slate-800/70">
                <div className="flex items-center gap-3">
                  <input
                    type="radio"
                    name="investmentHorizon"
                    value={horizon.id}
                    checked={preferences.tradingPreferences.investmentHorizon === horizon.id}
                    onChange={(e) => setPreferences(prev => ({
                      ...prev,
                      tradingPreferences: {
                        ...prev.tradingPreferences,
                        investmentHorizon: e.target.value
                      }
                    }))}
                    className="w-4 h-4 text-cyan-400 focus:ring-cyan-400"
                  />
                  <div>
                    <div className="text-white font-medium">{horizon.label}</div>
                    <div className="text-slate-400 text-sm">{horizon.description}</div>
                  </div>
                </div>
              </label>
            ))}
          </div>
        </div>

        {/* Notifications */}
        <div>
          <h3 className="text-lg font-semibold text-white mb-4">Notification Preferences</h3>
          <div className="glass-card p-6 rounded-xl space-y-4">
            {[
              { key: 'email', label: 'Email Notifications', icon: <Mail className="w-5 h-5" /> },
              { key: 'push', label: 'Push Notifications', icon: <Smartphone className="w-5 h-5" /> },
              { key: 'marketAlerts', label: 'Market Alerts', icon: <TrendingUp className="w-5 h-5" /> },
              { key: 'priceAlerts', label: 'Price Alerts', icon: <Bell className="w-5 h-5" /> },
            ].map((notif) => (
              <label key={notif.key} className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                  {notif.icon}
                  <span className="text-white">{notif.label}</span>
                </div>
                <input
                  type="checkbox"
                  checked={preferences.notifications[notif.key as keyof typeof preferences.notifications]}
                  onChange={(e) => setPreferences(prev => ({
                    ...prev,
                    notifications: {
                      ...prev.notifications,
                      [notif.key]: e.target.checked
                    }
                  }))}
                  className="w-4 h-4 text-cyan-400 focus:ring-cyan-400 rounded"
                />
              </label>
            ))}
          </div>
        </div>
      </div>

      <div className="flex gap-4">
        <button
          onClick={onBack}
          className="glass-card px-6 py-3 rounded-xl font-semibold text-white hover:text-purple-300 transition-colors border border-purple-500/50 hover:border-purple-400/70 flex-1 flex items-center justify-center"
        >
          <ChevronLeft className="w-5 h-5 mr-2" /> Back
        </button>
        <button
          onClick={handleNext}
          className="cyber-button flex-1 py-3 rounded-xl flex items-center justify-center"
        >
          Continue <ChevronRight className="w-5 h-5 ml-2" />
        </button>
      </div>
    </div>
  )
}

// Step 5: Tutorial and Completion
const TutorialStep: React.FC<OnboardingStepProps> = ({ onNext, onBack, onComplete, isLast, stepData }) => {
  const [tutorialProgress, setTutorialProgress] = useState(0)
  const navigate = useNavigate()

  const tutorials = [
    { title: 'Dashboard Overview', description: 'Learn about your trading dashboard', duration: '2 min' },
    { title: 'Placing Your First Order', description: 'Step-by-step order placement', duration: '3 min' },
    { title: 'Portfolio Tracking', description: 'Monitor your investments', duration: '2 min' },
    { title: 'Risk Management', description: 'Protect your capital', duration: '4 min' },
  ]

  // Check completion status from previous steps
  const kycCompleted = stepData?.kyc?.panCard || stepData?.kyc?.aadharCard
  const mfaCompleted = stepData?.security?.method && stepData?.security?.verified && !stepData?.security?.mfaSkipped

  const achievements = [
    { id: 'welcome', title: 'Welcome Aboard!', description: 'Joined TradeMaster', icon: <Trophy className="w-6 h-6" />, earned: true },
    { id: 'goals', title: 'Goal Setter', description: 'Set your trading goals', icon: <Target className="w-6 h-6" />, earned: true },
    { id: 'verified', title: 'Identity Verified', description: 'Completed KYC process', icon: <Shield className="w-6 h-6" />, earned: !!kycCompleted },
    { id: 'secure', title: 'Security Champion', description: 'Enabled 2FA', icon: <Award className="w-6 h-6" />, earned: !!mfaCompleted },
    { id: 'ready', title: 'Ready to Trade', description: 'Completed onboarding', icon: <Star className="w-6 h-6" />, earned: false },
  ]

  const handleFinishOnboarding = () => {
    onComplete({ 
      tutorialCompleted: true, 
      achievements: achievements.filter(a => a.earned).map(a => a.id),
      completedAt: new Date()
    })
    navigate('/dashboard')
  }

  return (
    <div className="space-y-8">
      <div className="text-center">
        <div className="cyber-glow w-20 h-20 mx-auto mb-6 rounded-full bg-gradient-to-r from-green-600 to-blue-600 flex items-center justify-center">
          <CheckCircle className="w-10 h-10 text-white" />
        </div>
        <h1 className="text-3xl font-bold gradient-text mb-4">
          🎉 You're All Set!
        </h1>
        <p className="text-slate-400 text-lg">
          Your TradeMaster account is ready. Let's explore the platform!
        </p>
      </div>

      {/* Achievements */}
      <div className="space-y-4">
        <h2 className="text-xl font-semibold text-white">Your Achievements</h2>
        <div className="grid gap-3">
          {achievements.map((achievement) => (
            <motion.div
              key={achievement.id}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              className={cn(
                "glass-card p-4 rounded-xl transition-all duration-300",
                achievement.earned 
                  ? "border border-green-400/50 bg-green-400/5" 
                  : "border border-slate-700/30 opacity-50"
              )}
            >
              <div className="flex items-center gap-4">
                <div className={cn(
                  "p-3 rounded-xl",
                  achievement.earned 
                    ? "bg-green-400/20 text-green-400" 
                    : "bg-slate-700/50 text-slate-500"
                )}>
                  {achievement.icon}
                </div>
                <div className="flex-1">
                  <h3 className={cn(
                    "font-semibold",
                    achievement.earned ? "text-white" : "text-slate-500"
                  )}>
                    {achievement.title}
                  </h3>
                  <p className="text-slate-400 text-sm">{achievement.description}</p>
                </div>
                {achievement.earned && (
                  <CheckCircle className="w-6 h-6 text-green-400" />
                )}
              </div>
            </motion.div>
          ))}
        </div>
      </div>

      {/* Optional Tutorials */}
      <div className="space-y-4">
        <h2 className="text-xl font-semibold text-white">Quick Start Tutorials</h2>
        <div className="glass-card p-6 rounded-xl">
          <p className="text-slate-400 mb-4">
            Want to learn the basics? These quick tutorials will get you started.
          </p>
          <div className="grid gap-3">
            {tutorials.map((tutorial, index) => (
              <div key={index} className="flex items-center justify-between p-3 bg-slate-800/50 rounded-lg">
                <div className="flex items-center gap-3">
                  <BookOpen className="w-5 h-5 text-blue-400" />
                  <div>
                    <div className="text-white font-medium">{tutorial.title}</div>
                    <div className="text-slate-400 text-sm">{tutorial.description}</div>
                  </div>
                </div>
                <div className="text-slate-400 text-sm">{tutorial.duration}</div>
              </div>
            ))}
          </div>
        </div>
      </div>

      <div className="flex gap-4">
        <button
          onClick={() => {
            // Start tutorials
            navigate('/dashboard?tutorial=true')
          }}
          className="glass-card px-6 py-4 rounded-xl font-semibold text-white hover:text-purple-300 transition-colors border border-purple-500/50 hover:border-purple-400/70 flex-1 flex items-center justify-center"
        >
          <BookOpen className="w-5 h-5 mr-2" /> Start Tutorials
        </button>
        <button
          onClick={handleFinishOnboarding}
          className="cyber-button flex-1 py-4 rounded-xl flex items-center justify-center"
        >
          <Zap className="w-5 h-5 mr-2" /> Go to Dashboard
        </button>
      </div>
    </div>
  )
}

export const OnboardingWizard: React.FC = () => {
  const navigate = useNavigate()
  const { user } = useAuthStore()
  const [progress, setProgress] = useState<OnboardingProgress>({
    currentStep: 0,
    completedSteps: [],
    stepData: {},
    startedAt: new Date(),
    achievements: [],
    score: 0
  })

  const steps: OnboardingStep[] = [
    {
      id: 'welcome',
      title: 'Welcome & Goals',
      subtitle: 'Set your trading objectives',
      icon: <Trophy className="w-6 h-6" />,
      component: WelcomeStep,
      estimatedTime: '2 min'
    },
    {
      id: 'kyc',
      title: 'Identity Verification',
      subtitle: 'Upload KYC documents',
      icon: <Shield className="w-6 h-6" />,
      component: KYCStep,
      isOptional: true,
      estimatedTime: '5 min'
    },
    {
      id: 'security',
      title: 'Account Security',
      subtitle: 'Enable two-factor authentication',
      icon: <Shield className="w-6 h-6" />,
      component: SecurityStep,
      isOptional: true,
      estimatedTime: '3 min'
    },
    {
      id: 'preferences',
      title: 'Your Preferences',
      subtitle: 'Customize your experience',
      icon: <Settings className="w-6 h-6" />,
      component: PreferencesStep,
      estimatedTime: '4 min'
    },
    {
      id: 'tutorial',
      title: 'Ready to Trade',
      subtitle: 'Complete your setup',
      icon: <CheckCircle className="w-6 h-6" />,
      component: TutorialStep,
      estimatedTime: '2 min'
    }
  ]

  const currentStepData = steps[progress.currentStep]
  const CurrentStepComponent = currentStepData?.component

  const handleNext = () => {
    if (progress.currentStep < steps.length - 1) {
      setProgress(prev => ({
        ...prev,
        currentStep: prev.currentStep + 1,
        completedSteps: [...prev.completedSteps, steps[prev.currentStep].id],
        score: prev.score + 20
      }))
    }
  }

  const handleBack = () => {
    if (progress.currentStep > 0) {
      setProgress(prev => ({
        ...prev,
        currentStep: prev.currentStep - 1
      }))
    }
  }

  const handleComplete = (stepData: any) => {
    setProgress(prev => ({
      ...prev,
      stepData: {
        ...prev.stepData,
        [currentStepData.id]: stepData
      }
    }))
  }

  const completionPercentage = (progress.currentStep / steps.length) * 100

  // Redirect if user is not authenticated
  useEffect(() => {
    if (!user) {
      navigate('/login')
    }
  }, [user, navigate])

  if (!user) return null

  return (
    <div className="min-h-screen hero-gradient relative overflow-hidden">
      <div className="absolute inset-0 bg-gradient-to-br from-slate-900/50 via-blue-900/20 to-purple-900/50" />
      
      {/* Progress Header */}
      <div className="relative z-10 w-full">
        <div className="glass-card mx-6 mt-6 p-6 rounded-2xl border-b border-slate-700">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h1 className="text-2xl font-bold gradient-text">Account Setup</h1>
              <p className="text-slate-400">
                Step {progress.currentStep + 1} of {steps.length} • {currentStepData.estimatedTime}
              </p>
            </div>
            <div className="text-right">
              <div className="text-2xl font-bold text-cyan-400">{Math.round(completionPercentage)}%</div>
              <div className="text-slate-400 text-sm">Complete</div>
            </div>
          </div>

          {/* Progress Bar */}
          <div className="w-full bg-slate-800 rounded-full h-3 overflow-hidden">
            <motion.div
              initial={{ width: 0 }}
              animate={{ width: `${completionPercentage}%` }}
              transition={{ duration: 0.5, ease: "easeOut" }}
              className="h-full bg-gradient-to-r from-blue-500 to-cyan-400 cyber-glow-sm"
            />
          </div>

          {/* Step Indicators */}
          <div className="flex justify-between mt-6">
            {steps.map((step, index) => (
              <div 
                key={step.id}
                className={cn(
                  "flex flex-col items-center gap-2 flex-1",
                  index <= progress.currentStep ? "text-cyan-400" : "text-slate-500"
                )}
              >
                <div className={cn(
                  "w-10 h-10 rounded-full flex items-center justify-center border-2 transition-all duration-300",
                  index < progress.currentStep 
                    ? "bg-green-400 border-green-400 text-white" 
                    : index === progress.currentStep
                    ? "border-cyan-400 bg-cyan-400/10"
                    : "border-slate-600 bg-slate-800"
                )}>
                  {index < progress.currentStep ? (
                    <CheckCircle className="w-5 h-5" />
                  ) : (
                    step.icon
                  )}
                </div>
                <div className="text-center hidden sm:block">
                  <div className="text-sm font-medium">{step.title}</div>
                  <div className="text-xs opacity-75">{step.subtitle}</div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Step Content */}
      <div className="relative z-10 container mx-auto px-6 py-8">
        <div className="max-w-2xl mx-auto">
          <div className="glass-card p-8 rounded-3xl">
            <AnimatePresence mode="wait">
              {CurrentStepComponent && (
                <motion.div
                  key={progress.currentStep}
                  initial={{ opacity: 0, x: 20 }}
                  animate={{ opacity: 1, x: 0 }}
                  exit={{ opacity: 0, x: -20 }}
                  transition={{ duration: 0.3 }}
                >
                  <CurrentStepComponent
                    onNext={handleNext}
                    onBack={handleBack}
                    onComplete={handleComplete}
                    stepData={progress.stepData}
                    isFirst={progress.currentStep === 0}
                    isLast={progress.currentStep === steps.length - 1}
                  />
                </motion.div>
              )}
            </AnimatePresence>
          </div>
        </div>
      </div>

      {/* Skip Button */}
      <button
        onClick={() => navigate('/dashboard')}
        className="fixed top-6 right-6 glass-card px-4 py-2 text-sm z-50 font-semibold text-white hover:text-purple-300 transition-colors border border-purple-500/50 hover:border-purple-400/70 rounded-xl"
      >
        Skip Setup
      </button>
    </div>
  )
}

export default OnboardingWizard