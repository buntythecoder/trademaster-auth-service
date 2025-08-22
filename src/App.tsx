import React from 'react'
import { BrowserRouter as Router, Routes, Route, Navigate, Link } from 'react-router-dom'
import { LoginForm } from './components/auth/LoginForm'
import { RegistrationForm } from './components/auth/RegistrationForm'
import { AdminDashboard } from './components/dashboard/AdminDashboard'
import { TraderDashboard } from './components/dashboard/TraderDashboard'
import { ProfileDashboard } from './components/profile/ProfileDashboard'
import { HeroSection } from './components/landing/HeroSection'
import { ProtectedRoute } from './components/auth/ProtectedRoute'
import { MarketDataDashboard } from './components/market/MarketDataDashboard'
import { TradingInterface } from './components/trading/TradingInterface'
import { PortfolioAnalytics } from './components/portfolio/PortfolioAnalytics'
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
import { LogOut, User, TrendingUp, PieChart, BarChart3 } from 'lucide-react'
import './index.css'

function App() {
  const { isAuthenticated, user } = useAuthStore()

  return (
    <ThemeProvider>
      <ToastProvider>
        <Router>
        <div className="min-h-screen">
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
                <Navigate to="/dashboard" replace />
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

          {/* Epic 2: Portfolio Analytics Route */}
          <Route
            path="/portfolio"
            element={
              <ProtectedRoute>
                <PageLayout>
                  <PortfolioAnalytics />
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