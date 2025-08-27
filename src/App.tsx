import React from 'react'
import { BrowserRouter as Router, Routes, Route, Navigate, Link } from 'react-router-dom'
import { LoginForm } from './components/auth/LoginForm'
import { RegistrationForm } from './components/auth/RegistrationForm'
import { AdminDashboard } from './components/dashboard/AdminDashboard'
import { TraderDashboard } from './components/dashboard/TraderDashboard'
import { ProfileDashboard } from './components/profile/ProfileDashboard'
import { HeroSection } from './components/landing/HeroSection'
import { ProtectedRoute } from './components/auth/ProtectedRoute'
import { MarketDataDashboard } from './pages/MarketDataDashboard'
import { TradingInterface } from './pages/TradingInterface'
import { MobileTradingInterface } from './pages/MobileTradingInterface'
import { PortfolioAnalyticsDashboard } from './pages/PortfolioAnalyticsDashboard'
import { MultiBrokerInterface } from './components/trading/MultiBrokerInterface'
import { RiskManagementDashboard } from './components/risk/RiskManagementDashboard'
import AdminAgentDashboard from './components/agentos/AgentDashboard'
import TraderTaskInterface from './components/trader/TraderTaskInterface'
import { OnboardingWizard } from './components/onboarding/OnboardingWizard'
import { TutorialManager } from './components/onboarding/TutorialOverlay'
import { AchievementSystem, ACHIEVEMENTS } from './components/onboarding/AchievementSystem'
import { NotFoundPage } from './components/error/NotFoundPage'
import { UnauthorizedPage } from './components/error/UnauthorizedPage'
import { ForbiddenPage } from './components/error/ForbiddenPage'
import { ServerErrorPage } from './components/error/ServerErrorPage'
import { NetworkErrorPage } from './components/error/NetworkErrorPage'
import { useAuthStore } from './stores/auth.store'
import { ThemeProvider } from './contexts/ThemeContext'
import { ToastProvider } from './contexts/ToastContext'
import { ThemeToggle } from './components/ui/ThemeToggle'
import { PageLayout } from './components/layout/PageLayout'
import { PWAInstallPrompt, PWAUpdatePrompt, ConnectionStatus, ConnectionRestoredNotification } from './components/pwa'
import { LogOut, User, TrendingUp, PieChart, BarChart3 } from 'lucide-react'
import './index.css'

function App() {
  const { isAuthenticated, user } = useAuthStore()

  // Mock achievements data - in real app, this would come from a store/API
  const mockAchievements = Object.values(ACHIEVEMENTS).map(achievement => ({
    ...achievement,
    unlocked: ['welcome-aboard', 'goal-setter', 'student'].includes(achievement.id),
    unlockedAt: ['welcome-aboard', 'goal-setter', 'student'].includes(achievement.id) ? new Date() : undefined,
    isNew: false
  }))

  return (
    <ThemeProvider>
      <ToastProvider>
        <Router>
        <div className="min-h-screen">
        {/* PWA Components - Global */}
        <PWAInstallPrompt />
        <PWAUpdatePrompt />
        <ConnectionRestoredNotification />
        
        {/* Tutorial Manager - Global */}
        <TutorialManager />
        
        {/* Achievement System - Global */}
        {isAuthenticated && (
          <AchievementSystem 
            achievements={mockAchievements}
            onAchievementUnlock={(id) => console.log('Achievement unlocked:', id)}
          />
        )}
        
        <Routes>
          {/* Landing Page */}
          <Route
            path="/"
            element={
              isAuthenticated ? (
                <Navigate to="/dashboard" replace />
              ) : (
                <div className="relative">
                  <HeroSection />
                  {/* Floating Nav */}
                  <nav className="absolute top-6 right-6 z-20">
                    <div className="glass-card rounded-2xl px-6 py-3">
                      <div className="flex items-center space-x-4">
                        <ThemeToggle />
                        <Link
                          to="/login"
                          className="text-slate-300 hover:text-white text-sm font-medium transition-colors"
                        >
                          Sign In
                        </Link>
                        <Link
                          to="/register"
                          className="cyber-button-sm px-4 py-2 text-xs font-medium rounded-xl"
                        >
                          Get Started
                        </Link>
                      </div>
                    </div>
                  </nav>
                </div>
              )
            }
          />

          {/* Authentication Routes */}
          <Route
            path="/login"
            element={
              isAuthenticated ? (
                <Navigate to="/dashboard" replace />
              ) : (
                <LoginForm />
              )
            }
          />
          <Route
            path="/register"
            element={
              isAuthenticated ? (
                <Navigate to="/onboarding" replace />
              ) : (
                <RegistrationForm 
                  onSubmit={async (data) => {
                    console.log('Registration data:', data)
                    // Handle registration logic here
                  }}
                />
              )
            }
          />

          {/* Epic 3: Onboarding Route */}
          <Route
            path="/onboarding"
            element={
              <ProtectedRoute>
                <OnboardingWizard />
              </ProtectedRoute>
            }
          />

          {/* Protected Routes */}
          <Route
            path="/dashboard"
            element={
              <ProtectedRoute>
                <PageLayout showWelcome={true}>
                  {user?.role === 'ADMIN' ? (
                    <AdminDashboard />
                  ) : (
                    <TraderDashboard />
                  )}
                </PageLayout>
              </ProtectedRoute>
            }
          />

          {/* Epic 2: Market Data Route */}
          <Route
            path="/market-data"
            element={
              <ProtectedRoute>
                <PageLayout>
                  <MarketDataDashboard />
                </PageLayout>
              </ProtectedRoute>
            }
          />

          {/* Epic 2: Trading Interface Route */}
          <Route
            path="/trading"
            element={
              <ProtectedRoute>
                <PageLayout>
                  <TradingInterface />
                </PageLayout>
              </ProtectedRoute>
            }
          />

          {/* FRONT-006: Mobile Trading Interface Route */}
          <Route
            path="/mobile-trading"
            element={
              <ProtectedRoute>
                <MobileTradingInterface />
              </ProtectedRoute>
            }
          />

          {/* Epic 2: Portfolio Analytics Route */}
          <Route
            path="/portfolio"
            element={
              <ProtectedRoute>
                <PortfolioAnalyticsDashboard />
              </ProtectedRoute>
            }
          />

          {/* Analytics Route (alias for portfolio) */}
          <Route
            path="/analytics"
            element={
              <ProtectedRoute>
                <PageLayout>
                  <PortfolioAnalyticsDashboard />
                </PageLayout>
              </ProtectedRoute>
            }
          />

          {/* Profile Route */}
          <Route
            path="/profile"
            element={
              <ProtectedRoute>
                <PageLayout showWelcome={true}>
                  <ProfileDashboard />
                </PageLayout>
              </ProtectedRoute>
            }
          />

          {/* Multi-Broker Interface Route */}
          <Route
            path="/brokers"
            element={
              <ProtectedRoute>
                <PageLayout>
                  <MultiBrokerInterface />
                </PageLayout>
              </ProtectedRoute>
            }
          />

          {/* Risk Management Route */}
          <Route
            path="/risk"
            element={
              <ProtectedRoute>
                <PageLayout>
                  <RiskManagementDashboard />
                </PageLayout>
              </ProtectedRoute>
            }
          />

          {/* Agent OS Dashboard Route - Role-based */}
          <Route
            path="/agents"
            element={
              <ProtectedRoute>
                <PageLayout>
                  {user?.role === 'ADMIN' ? (
                    <AdminAgentDashboard />
                  ) : (
                    <TraderTaskInterface />
                  )}
                </PageLayout>
              </ProtectedRoute>
            }
          />

          {/* Admin Routes */}
          <Route
            path="/admin/users"
            element={
              <ProtectedRoute>
                <PageLayout>
                  <div className="space-y-6">
                    <h1 className="text-3xl font-bold text-white">User Management</h1>
                    <p className="text-slate-400">Manage user accounts and permissions</p>
                    <div className="glass-card p-6 rounded-2xl">
                      <p className="text-white">User management interface coming soon...</p>
                    </div>
                  </div>
                </PageLayout>
              </ProtectedRoute>
            }
          />

          <Route
            path="/admin/system"
            element={
              <ProtectedRoute>
                <PageLayout>
                  <div className="space-y-6">
                    <h1 className="text-3xl font-bold text-white">System Configuration</h1>
                    <p className="text-slate-400">Configure system settings and broker connections</p>
                    <MultiBrokerInterface />
                  </div>
                </PageLayout>
              </ProtectedRoute>
            }
          />

          {/* Error Pages */}
          <Route path="/401" element={<UnauthorizedPage />} />
          <Route path="/403" element={<ForbiddenPage />} />
          <Route path="/500" element={<ServerErrorPage />} />
          <Route path="/network-error" element={<NetworkErrorPage />} />
          
          {/* 404 - Must be last */}
          <Route path="*" element={<NotFoundPage />} />
        </Routes>
        </div>
      </Router>
      </ToastProvider>
    </ThemeProvider>
  )
}

export default App