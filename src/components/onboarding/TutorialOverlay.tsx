import React, { useState, useEffect, useRef } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { 
  X, 
  ChevronRight, 
  ChevronLeft, 
  Target, 
  Lightbulb,
  Play,
  Pause,
  RotateCcw,
  CheckCircle,
  ArrowDown,
  ArrowUp,
  ArrowLeft,
  ArrowRight,
  MousePointer,
  Zap
} from 'lucide-react'
import { cn } from '@/lib/utils'

interface TutorialStep {
  id: string
  title: string
  content: string
  target: string // CSS selector
  position: 'top' | 'bottom' | 'left' | 'right' | 'center'
  action?: 'click' | 'hover' | 'scroll' | 'type'
  actionText?: string
  spotlight?: boolean
  arrow?: boolean
  delay?: number
  skippable?: boolean
}

interface TutorialTour {
  id: string
  title: string
  description: string
  category: 'dashboard' | 'trading' | 'portfolio' | 'features'
  steps: TutorialStep[]
  estimatedTime: string
  difficulty: 'beginner' | 'intermediate' | 'advanced'
  prerequisites?: string[]
}

interface TutorialOverlayProps {
  tour?: TutorialTour
  currentStep?: number
  isActive: boolean
  onComplete: (tourId: string) => void
  onSkip: (tourId: string) => void
  onClose: () => void
  autoPlay?: boolean
}

// Predefined tutorial tours
export const TUTORIAL_TOURS: Record<string, TutorialTour> = {
  dashboard: {
    id: 'dashboard',
    title: 'Dashboard Overview',
    description: 'Learn about your trading dashboard and key features',
    category: 'dashboard',
    estimatedTime: '3 min',
    difficulty: 'beginner',
    steps: [
      {
        id: 'welcome',
        title: 'Welcome to Your Dashboard!',
        content: 'This is your central hub for all trading activities. Let\'s explore the key features.',
        target: '[data-tour="dashboard-main"]',
        position: 'center',
        spotlight: true,
        skippable: false
      },
      {
        id: 'portfolio-summary',
        title: 'Portfolio Summary',
        content: 'View your total portfolio value, daily P&L, and performance at a glance.',
        target: '[data-tour="portfolio-summary"]',
        position: 'bottom',
        arrow: true,
        spotlight: true
      },
      {
        id: 'watchlist',
        title: 'Your Watchlist',
        content: 'Track your favorite stocks and monitor real-time price movements.',
        target: '[data-tour="watchlist"]',
        position: 'left',
        arrow: true,
        spotlight: true,
        action: 'click',
        actionText: 'Click to add stocks to your watchlist'
      },
      {
        id: 'quick-trade',
        title: 'Quick Trade Panel',
        content: 'Place orders quickly without leaving the dashboard.',
        target: '[data-tour="quick-trade"]',
        position: 'right',
        arrow: true,
        spotlight: true
      },
      {
        id: 'market-overview',
        title: 'Market Overview',
        content: 'Stay updated with market indices and sector performance.',
        target: '[data-tour="market-overview"]',
        position: 'top',
        arrow: true,
        spotlight: true
      }
    ]
  },
  trading: {
    id: 'trading',
    title: 'Placing Your First Order',
    description: 'Step-by-step guide to place your first trade',
    category: 'trading',
    estimatedTime: '5 min',
    difficulty: 'beginner',
    steps: [
      {
        id: 'search-stock',
        title: 'Search for a Stock',
        content: 'Use the search bar to find the stock you want to trade.',
        target: '[data-tour="symbol-search"]',
        position: 'bottom',
        arrow: true,
        spotlight: true,
        action: 'type',
        actionText: 'Try searching for "RELIANCE"'
      },
      {
        id: 'select-stock',
        title: 'Select Your Stock',
        content: 'Click on the stock from the search results to load its details.',
        target: '[data-tour="search-results"]',
        position: 'bottom',
        action: 'click',
        actionText: 'Click on the stock'
      },
      {
        id: 'order-type',
        title: 'Choose Order Type',
        content: 'Select between Market, Limit, or Stop Loss orders based on your strategy.',
        target: '[data-tour="order-type"]',
        position: 'right',
        arrow: true,
        spotlight: true
      },
      {
        id: 'quantity',
        title: 'Enter Quantity',
        content: 'Specify how many shares you want to buy or sell.',
        target: '[data-tour="quantity-input"]',
        position: 'right',
        arrow: true,
        action: 'type',
        actionText: 'Enter the number of shares'
      },
      {
        id: 'price',
        title: 'Set Price (Limit Orders)',
        content: 'For limit orders, set the price at which you want to execute the trade.',
        target: '[data-tour="price-input"]',
        position: 'right',
        arrow: true
      },
      {
        id: 'review-order',
        title: 'Review Your Order',
        content: 'Always review your order details before placing the trade.',
        target: '[data-tour="order-summary"]',
        position: 'left',
        spotlight: true
      },
      {
        id: 'place-order',
        title: 'Place Your Order',
        content: 'Click the buy or sell button to place your order.',
        target: '[data-tour="place-order-btn"]',
        position: 'top',
        arrow: true,
        spotlight: true,
        action: 'click',
        actionText: 'Click to place order'
      }
    ]
  },
  portfolio: {
    id: 'portfolio',
    title: 'Portfolio Analytics',
    description: 'Understand your portfolio performance and analytics',
    category: 'portfolio',
    estimatedTime: '4 min',
    difficulty: 'intermediate',
    steps: [
      {
        id: 'holdings',
        title: 'Your Holdings',
        content: 'View all your current stock positions with real-time values and P&L.',
        target: '[data-tour="holdings-table"]',
        position: 'top',
        spotlight: true
      },
      {
        id: 'performance-chart',
        title: 'Performance Chart',
        content: 'Track your portfolio performance over different time periods.',
        target: '[data-tour="performance-chart"]',
        position: 'bottom',
        arrow: true,
        spotlight: true
      },
      {
        id: 'allocation',
        title: 'Asset Allocation',
        content: 'See how your investments are distributed across different sectors and assets.',
        target: '[data-tour="asset-allocation"]',
        position: 'left',
        arrow: true
      },
      {
        id: 'metrics',
        title: 'Key Metrics',
        content: 'Important portfolio metrics like total returns, Sharpe ratio, and volatility.',
        target: '[data-tour="portfolio-metrics"]',
        position: 'right',
        arrow: true
      }
    ]
  }
}

const getArrowIcon = (position: string) => {
  switch (position) {
    case 'top': return <ArrowDown className="w-5 h-5" />
    case 'bottom': return <ArrowUp className="w-5 h-5" />
    case 'left': return <ArrowRight className="w-5 h-5" />
    case 'right': return <ArrowLeft className="w-5 h-5" />
    default: return <Target className="w-5 h-5" />
  }
}

const getTooltipPosition = (target: Element, position: string) => {
  const rect = target.getBoundingClientRect()
  const scrollTop = window.pageYOffset || document.documentElement.scrollTop
  const scrollLeft = window.pageXOffset || document.documentElement.scrollLeft
  
  const tooltipWidth = 320
  const tooltipHeight = 200
  const offset = 20

  switch (position) {
    case 'top':
      return {
        top: rect.top + scrollTop - tooltipHeight - offset,
        left: rect.left + scrollLeft + (rect.width - tooltipWidth) / 2
      }
    case 'bottom':
      return {
        top: rect.bottom + scrollTop + offset,
        left: rect.left + scrollLeft + (rect.width - tooltipWidth) / 2
      }
    case 'left':
      return {
        top: rect.top + scrollTop + (rect.height - tooltipHeight) / 2,
        left: rect.left + scrollLeft - tooltipWidth - offset
      }
    case 'right':
      return {
        top: rect.top + scrollTop + (rect.height - tooltipHeight) / 2,
        left: rect.right + scrollLeft + offset
      }
    case 'center':
      return {
        top: window.innerHeight / 2 - tooltipHeight / 2 + scrollTop,
        left: window.innerWidth / 2 - tooltipWidth / 2 + scrollLeft
      }
    default:
      return { top: 0, left: 0 }
  }
}

export const TutorialOverlay: React.FC<TutorialOverlayProps> = ({
  tour,
  currentStep = 0,
  isActive,
  onComplete,
  onSkip,
  onClose,
  autoPlay = false
}) => {
  const [step, setStep] = useState(currentStep)
  const [isPlaying, setIsPlaying] = useState(autoPlay)
  const [tooltipPosition, setTooltipPosition] = useState({ top: 0, left: 0 })
  const [targetHighlight, setTargetHighlight] = useState<DOMRect | null>(null)
  const tooltipRef = useRef<HTMLDivElement>(null)
  const overlayRef = useRef<HTMLDivElement>(null)

  const currentStepData = tour?.steps[step]

  useEffect(() => {
    if (!isActive || !currentStepData) return

    const updatePosition = () => {
      const targetElement = document.querySelector(currentStepData.target)
      if (targetElement) {
        const position = getTooltipPosition(targetElement, currentStepData.position)
        setTooltipPosition(position)
        setTargetHighlight(targetElement.getBoundingClientRect())

        // Scroll target into view
        targetElement.scrollIntoView({ 
          behavior: 'smooth', 
          block: 'center',
          inline: 'center'
        })
      }
    }

    // Initial position
    updatePosition()

    // Update on scroll/resize
    const handleUpdate = () => {
      setTimeout(updatePosition, 100)
    }

    window.addEventListener('scroll', handleUpdate)
    window.addEventListener('resize', handleUpdate)

    return () => {
      window.removeEventListener('scroll', handleUpdate)
      window.removeEventListener('resize', handleUpdate)
    }
  }, [currentStepData, isActive, step])

  useEffect(() => {
    if (isPlaying && currentStepData?.delay) {
      const timer = setTimeout(() => {
        handleNext()
      }, currentStepData.delay)
      return () => clearTimeout(timer)
    }
  }, [step, isPlaying, currentStepData])

  const handleNext = () => {
    if (!tour) return
    
    if (step < tour.steps.length - 1) {
      setStep(step + 1)
    } else {
      handleComplete()
    }
  }

  const handlePrev = () => {
    if (step > 0) {
      setStep(step - 1)
    }
  }

  const handleComplete = () => {
    if (tour) {
      onComplete(tour.id)
    }
  }

  const handleSkip = () => {
    if (tour) {
      onSkip(tour.id)
    }
  }

  const handleRestart = () => {
    setStep(0)
    setIsPlaying(false)
  }

  // Strict check - absolutely no overlay unless all conditions are met
  if (!isActive || !tour || !currentStepData || !tour.steps || tour.steps.length === 0) {
    return null
  }

  // Additional safety check - must have a valid target element
  const targetElement = currentStepData.target ? document.querySelector(currentStepData.target) : null
  if (currentStepData.target && !targetElement) {
    // If target is specified but not found, don't show overlay
    return null
  }

  return (
    <AnimatePresence>
      <div 
        ref={overlayRef} 
        className="fixed inset-0 z-50 pointer-events-none"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Dark Overlay with Spotlight - Only show when tutorial is truly active */}
        <div className="absolute inset-0 bg-black/80 pointer-events-auto">
          {currentStepData.spotlight && targetHighlight && (
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              className="absolute rounded-lg border-4 border-cyan-400/50 bg-transparent"
              style={{
                top: targetHighlight.top - 8,
                left: targetHighlight.left - 8,
                width: targetHighlight.width + 16,
                height: targetHighlight.height + 16,
                boxShadow: '0 0 0 9999px rgba(0, 0, 0, 0.8)',
                pointerEvents: 'none'
              }}
            />
          )}
        </div>

        {/* Tutorial Tooltip */}
        <motion.div
          ref={tooltipRef}
          initial={{ opacity: 0, scale: 0.9 }}
          animate={{ opacity: 1, scale: 1 }}
          exit={{ opacity: 0, scale: 0.9 }}
          className="absolute z-10"
          style={{
            top: tooltipPosition.top,
            left: Math.max(20, Math.min(tooltipPosition.left, window.innerWidth - 340))
          }}
        >
          <div className="glass-card rounded-2xl p-6 w-80 border border-cyan-400/30 pointer-events-auto">
            {/* Header */}
            <div className="flex items-start justify-between mb-4">
              <div className="flex items-center gap-3">
                <div className="p-2 bg-cyan-400/20 rounded-xl">
                  <Lightbulb className="w-5 h-5 text-cyan-400" />
                </div>
                <div>
                  <h3 className="font-semibold text-white">{currentStepData.title}</h3>
                  <p className="text-xs text-slate-400">
                    Step {step + 1} of {tour.steps.length}
                  </p>
                </div>
              </div>
              <button
                onClick={onClose}
                className="text-slate-400 hover:text-white transition-colors"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Content */}
            <div className="mb-6">
              <p className="text-slate-300 text-sm leading-relaxed">
                {currentStepData.content}
              </p>
              
              {currentStepData.action && currentStepData.actionText && (
                <div className="mt-4 p-3 bg-blue-400/10 rounded-lg border border-blue-400/20">
                  <div className="flex items-center gap-2 text-blue-400">
                    <MousePointer className="w-4 h-4" />
                    <span className="text-sm font-medium">Try it:</span>
                  </div>
                  <p className="text-blue-300 text-sm mt-1">
                    {currentStepData.actionText}
                  </p>
                </div>
              )}
            </div>

            {/* Progress Bar */}
            <div className="mb-4">
              <div className="flex justify-between text-xs text-slate-400 mb-2">
                <span>Progress</span>
                <span>{Math.round(((step + 1) / tour.steps.length) * 100)}%</span>
              </div>
              <div className="h-2 bg-slate-800 rounded-full overflow-hidden">
                <motion.div
                  initial={{ width: 0 }}
                  animate={{ width: `${((step + 1) / tour.steps.length) * 100}%` }}
                  className="h-full bg-gradient-to-r from-cyan-400 to-blue-500"
                />
              </div>
            </div>

            {/* Controls */}
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <button
                  onClick={() => setIsPlaying(!isPlaying)}
                  className="p-2 hover:bg-slate-700/50 rounded-lg transition-colors"
                  title={isPlaying ? 'Pause' : 'Play'}
                >
                  {isPlaying ? (
                    <Pause className="w-4 h-4 text-slate-400" />
                  ) : (
                    <Play className="w-4 h-4 text-slate-400" />
                  )}
                </button>
                <button
                  onClick={handleRestart}
                  className="p-2 hover:bg-slate-700/50 rounded-lg transition-colors"
                  title="Restart"
                >
                  <RotateCcw className="w-4 h-4 text-slate-400" />
                </button>
              </div>

              <div className="flex items-center gap-2">
                {currentStepData.skippable !== false && (
                  <button
                    onClick={handleSkip}
                    className="px-3 py-2 text-sm text-slate-400 hover:text-white transition-colors"
                  >
                    Skip Tour
                  </button>
                )}
                
                <button
                  onClick={handlePrev}
                  disabled={step === 0}
                  className="cyber-button cyber-button-ghost px-3 py-2 disabled:opacity-50"
                >
                  <ChevronLeft className="w-4 h-4" />
                </button>
                
                <button
                  onClick={handleNext}
                  className="cyber-button cyber-button-primary px-4 py-2"
                >
                  {step === tour.steps.length - 1 ? (
                    <>
                      <CheckCircle className="w-4 h-4 mr-1" />
                      Complete
                    </>
                  ) : (
                    <>
                      Next
                      <ChevronRight className="w-4 h-4 ml-1" />
                    </>
                  )}
                </button>
              </div>
            </div>
          </div>

          {/* Arrow */}
          {currentStepData.arrow && (
            <div 
              className={cn(
                "absolute text-cyan-400",
                currentStepData.position === 'top' && "bottom-0 left-1/2 transform -translate-x-1/2 translate-y-full",
                currentStepData.position === 'bottom' && "top-0 left-1/2 transform -translate-x-1/2 -translate-y-full",
                currentStepData.position === 'left' && "right-0 top-1/2 transform translate-x-full -translate-y-1/2",
                currentStepData.position === 'right' && "left-0 top-1/2 transform -translate-x-full -translate-y-1/2"
              )}
            >
              {getArrowIcon(currentStepData.position)}
            </div>
          )}
        </motion.div>
      </div>
    </AnimatePresence>
  )
}

// Tutorial Manager Component
export const TutorialManager: React.FC = () => {
  // Explicitly start with null - no auto-tours
  const [activeTour, setActiveTour] = useState<TutorialTour | null>(null)
  const [currentStep, setCurrentStep] = useState(0)
  const [completedTours, setCompletedTours] = useState<string[]>([])
  
  // Tutorial manager ready - no debug logging needed

  const startTour = (tourId: string) => {
    const tour = TUTORIAL_TOURS[tourId]
    if (tour) {
      setActiveTour(tour)
      setCurrentStep(0)
    }
  }

  const handleComplete = (tourId: string) => {
    setCompletedTours(prev => [...prev, tourId])
    setActiveTour(null)
    setCurrentStep(0)
  }

  const handleSkip = (tourId: string) => {
    setActiveTour(null)
    setCurrentStep(0)
  }

  const closeTour = () => {
    setActiveTour(null)
    setCurrentStep(0)
  }

  // Expose functions globally for triggering tours
  useEffect(() => {
    ;(window as any).startTutorial = startTour
  }, [])

  return (
    <TutorialOverlay
      tour={activeTour}
      currentStep={currentStep}
      isActive={!!activeTour}
      onComplete={handleComplete}
      onSkip={handleSkip}
      onClose={closeTour}
      autoPlay={false}
    />
  )
}

export default TutorialOverlay