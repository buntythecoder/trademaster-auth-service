import React from 'react'
import { BrowserRouter as Router, Routes, Route, Navigate, Link } from 'react-router-dom'
import { LoginForm } from './components/auth/LoginForm'
import { RegistrationForm } from './components/auth/RegistrationForm'
import { AdminDashboard } from './components/dashboard/AdminDashboard'
import { TraderDashboard } from './components/dashboard/TraderDashboard'
import { ProfileDashboard } from './components/profile/ProfileDashboard'
import { HeroSection } from './components/landing/HeroSection'
import { ProtectedRoute } from './components/auth/ProtectedRoute'
import { useAuthStore } from './stores/auth.store'
import { ThemeProvider } from './contexts/ThemeContext'
import { ThemeToggle } from './components/ui/ThemeToggle'
import { LogOut, User } from 'lucide-react'
import './index.css'

function App() {
  const { isAuthenticated, user } = useAuthStore()

  return (
    <ThemeProvider>
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
                <div className="min-h-screen bg-slate-900">
                  {/* Dashboard Header */}
                  <header className="glass-card-dark border-b border-slate-700/50">
                    <div className="container mx-auto flex h-16 items-center justify-between px-6">
                      <div className="flex items-center space-x-4">
                        <h1 className="text-xl font-bold gradient-text">TradeMaster</h1>
                        {user && (
                          <span className="text-sm text-slate-400">
                            Welcome back, <span className="text-purple-400">{user.firstName}</span>
                          </span>
                        )}
                      </div>
                      <div className="flex items-center space-x-4">
                        <ThemeToggle />
                        <Link
                          to="/profile"
                          className="flex items-center space-x-2 text-slate-400 hover:text-purple-400 transition-colors p-2 rounded-lg hover:bg-slate-800/50"
                        >
                          <User className="w-4 h-4" />
                          <span className="text-sm">Profile</span>
                        </Link>
                        <div className="flex items-center space-x-2 text-slate-400">
                          <span className="text-sm">{user?.role}</span>
                        </div>
                        <button
                          onClick={() => useAuthStore.getState().logout()}
                          className="flex items-center space-x-2 text-slate-400 hover:text-red-400 transition-colors p-2 rounded-lg hover:bg-slate-800/50"
                        >
                          <LogOut className="w-4 h-4" />
                          <span className="text-sm">Logout</span>
                        </button>
                      </div>
                    </div>
                  </header>

                  {/* Dashboard Content */}
                  <main className="container mx-auto px-6 py-8">
                    {user?.role === 'ADMIN' ? (
                      <AdminDashboard />
                    ) : (
                      <TraderDashboard />
                    )}
                  </main>
                </div>
              </ProtectedRoute>
            }
          />

          {/* Profile Route */}
          <Route
            path="/profile"
            element={
              <ProtectedRoute>
                <div className="min-h-screen bg-slate-900">
                  {/* Dashboard Header */}
                  <header className="glass-card-dark border-b border-slate-700/50">
                    <div className="container mx-auto flex h-16 items-center justify-between px-6">
                      <div className="flex items-center space-x-4">
                        <Link to="/dashboard" className="text-xl font-bold gradient-text hover:opacity-80 transition-opacity">
                          TradeMaster
                        </Link>
                        {user && (
                          <span className="text-sm text-slate-400">
                            Welcome back, <span className="text-purple-400">{user.firstName}</span>
                          </span>
                        )}
                      </div>
                      <div className="flex items-center space-x-4">
                        <ThemeToggle />
                        <Link
                          to="/dashboard"
                          className="flex items-center space-x-2 text-slate-400 hover:text-cyan-400 transition-colors p-2 rounded-lg hover:bg-slate-800/50"
                        >
                          <span className="text-sm">Dashboard</span>
                        </Link>
                        <Link
                          to="/profile"
                          className="flex items-center space-x-2 text-purple-400 bg-slate-800/50 p-2 rounded-lg"
                        >
                          <User className="w-4 h-4" />
                          <span className="text-sm">Profile</span>
                        </Link>
                        <div className="flex items-center space-x-2 text-slate-400">
                          <span className="text-sm">{user?.role}</span>
                        </div>
                        <button
                          onClick={() => useAuthStore.getState().logout()}
                          className="flex items-center space-x-2 text-slate-400 hover:text-red-400 transition-colors p-2 rounded-lg hover:bg-slate-800/50"
                        >
                          <LogOut className="w-4 h-4" />
                          <span className="text-sm">Logout</span>
                        </button>
                      </div>
                    </div>
                  </header>

                  {/* Profile Content */}
                  <main className="container mx-auto px-6 py-8">
                    <ProfileDashboard />
                  </main>
                </div>
              </ProtectedRoute>
            }
          />
        </Routes>
        </div>
      </Router>
    </ThemeProvider>
  )
}

export default App