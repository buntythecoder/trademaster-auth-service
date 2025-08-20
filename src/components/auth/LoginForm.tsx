import React from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useAuthStore } from '../../stores/auth.store'
import { Eye, EyeOff, Mail, Lock, CheckCircle, XCircle, TrendingUp } from 'lucide-react'
import { Link } from 'react-router-dom'

const loginSchema = z.object({
  email: z.string().email('Please enter a valid email address'),
  password: z.string().min(6, 'Password must be at least 6 characters'),
})

type LoginFormData = z.infer<typeof loginSchema>

export function LoginForm() {
  const [showPassword, setShowPassword] = React.useState(false)
  const { login, isLoading } = useAuthStore()

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
    watch
  } = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
  })

  const watchedFields = watch()

  const onSubmit = async (data: LoginFormData) => {
    try {
      await login(data.email, data.password)
    } catch (error) {
      console.error('Login failed:', error)
    }
  }

  const getFieldStatus = (fieldName: keyof LoginFormData) => {
    const hasError = errors[fieldName]
    const hasValue = watchedFields[fieldName]
    
    if (hasError) return 'error'
    if (hasValue && !hasError) return 'success'
    return 'default'
  }

  return (
    <div className="min-h-screen hero-gradient flex items-center justify-center p-4 relative overflow-hidden">
      {/* Background Effects */}
      <div className="absolute inset-0">
        <div className="absolute top-20 left-10 w-32 h-32 bg-gradient-to-br from-purple-500/10 to-pink-500/10 rounded-full blur-3xl animate-pulse" />
        <div className="absolute bottom-20 right-10 w-40 h-40 bg-gradient-to-br from-blue-500/10 to-purple-500/10 rounded-full blur-3xl animate-pulse" style={{ animationDelay: '2s' }} />
        <div className="absolute top-1/2 left-1/4 w-24 h-24 bg-gradient-to-br from-green-500/10 to-blue-500/10 rounded-full blur-2xl animate-pulse" style={{ animationDelay: '4s' }} />
      </div>

      <div className="relative z-10 w-full max-w-md mx-auto animate-fade-up">
        {/* Header */}
        <div className="text-center mb-8">
          <div className="flex items-center justify-center w-16 h-16 mb-4 mx-auto glass-card rounded-2xl">
            <TrendingUp className="w-8 h-8 text-purple-400" />
          </div>
          <h1 className="text-3xl font-bold gradient-text mb-2">Welcome Back</h1>
          <p className="text-slate-400">Sign in to your TradeMaster account</p>
        </div>

        {/* Login Form */}
        <div className="glass-card rounded-3xl p-8">
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
            {/* Email Field */}
            <div className="space-y-2">
              <label htmlFor="email" className="block text-sm font-medium text-slate-300 mb-2">
                Email Address
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                  <Mail className={`w-5 h-5 ${
                    getFieldStatus('email') === 'success' ? 'text-green-400' :
                    getFieldStatus('email') === 'error' ? 'text-red-400' :
                    'text-slate-400'
                  }`} />
                </div>
                <input
                  id="email"
                  type="email"
                  placeholder="john@example.com"
                  {...register('email')}
                  className={`cyber-input w-full pl-12 pr-12 py-4 rounded-2xl text-white placeholder-slate-400 ${
                    getFieldStatus('email') === 'success' ? 'success' :
                    getFieldStatus('email') === 'error' ? 'error' : ''
                  }`}
                />
                <div className="absolute inset-y-0 right-0 pr-4 flex items-center">
                  {getFieldStatus('email') === 'success' && (
                    <CheckCircle className="w-5 h-5 text-green-400" />
                  )}
                  {getFieldStatus('email') === 'error' && (
                    <XCircle className="w-5 h-5 text-red-400" />
                  )}
                </div>
              </div>
              {errors.email && (
                <p className="text-sm text-red-400 flex items-center space-x-2 animate-slide-in">
                  <XCircle className="w-4 h-4" />
                  <span>{errors.email.message}</span>
                </p>
              )}
              {getFieldStatus('email') === 'success' && (
                <p className="text-sm text-green-400 flex items-center space-x-2 animate-slide-in">
                  <CheckCircle className="w-4 h-4" />
                  <span>Looks good!</span>
                </p>
              )}
            </div>

            {/* Password Field */}
            <div className="space-y-2">
              <label htmlFor="password" className="block text-sm font-medium text-slate-300 mb-2">
                Password
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                  <Lock className={`w-5 h-5 ${
                    getFieldStatus('password') === 'success' ? 'text-green-400' :
                    getFieldStatus('password') === 'error' ? 'text-red-400' :
                    'text-slate-400'
                  }`} />
                </div>
                <input
                  id="password"
                  type={showPassword ? 'text' : 'password'}
                  placeholder="Enter your password"
                  {...register('password')}
                  className={`cyber-input w-full pl-12 pr-16 py-4 rounded-2xl text-white placeholder-slate-400 ${
                    getFieldStatus('password') === 'success' ? 'success' :
                    getFieldStatus('password') === 'error' ? 'error' : ''
                  }`}
                />
                <div className="absolute inset-y-0 right-0 flex items-center space-x-2 pr-4">
                  {getFieldStatus('password') === 'success' && (
                    <CheckCircle className="w-5 h-5 text-green-400" />
                  )}
                  {getFieldStatus('password') === 'error' && (
                    <XCircle className="w-5 h-5 text-red-400" />
                  )}
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="text-slate-400 hover:text-white transition-colors"
                  >
                    {showPassword ? (
                      <EyeOff className="w-5 h-5" />
                    ) : (
                      <Eye className="w-5 h-5" />
                    )}
                  </button>
                </div>
              </div>
              {errors.password && (
                <p className="text-sm text-red-400 flex items-center space-x-2 animate-slide-in">
                  <XCircle className="w-4 h-4" />
                  <span>{errors.password.message}</span>
                </p>
              )}
              {getFieldStatus('password') === 'success' && (
                <p className="text-sm text-green-400 flex items-center space-x-2 animate-slide-in">
                  <CheckCircle className="w-4 h-4" />
                  <span>Looks good!</span>
                </p>
              )}
            </div>

            {/* Submit Button */}
            <button
              type="submit"
              disabled={isSubmitting || isLoading}
              className="cyber-button w-full py-4 rounded-2xl font-semibold text-lg disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {isSubmitting || isLoading ? (
                <div className="flex items-center justify-center space-x-2">
                  <div className="loading-dots">
                    <div className="loading-dot"></div>
                    <div className="loading-dot"></div>
                    <div className="loading-dot"></div>
                  </div>
                  <span>Signing In...</span>
                </div>
              ) : (
                'Sign In'
              )}
            </button>

            {/* Demo Credentials */}
            <div className="text-center space-y-2 p-4 glass-card rounded-2xl">
              <p className="text-sm text-slate-400 mb-3">Demo Credentials:</p>
              <div className="grid grid-cols-2 gap-4 text-xs">
                <div>
                  <p className="text-orange-400 font-medium">Admin</p>
                  <p className="text-slate-500">admin@trademaster.com</p>
                  <p className="text-slate-500">admin123</p>
                </div>
                <div>
                  <p className="text-blue-400 font-medium">Trader</p>
                  <p className="text-slate-500">trader@trademaster.com</p>
                  <p className="text-slate-500">trader123</p>
                </div>
              </div>
            </div>

            {/* Footer Links */}
            <div className="text-center text-sm space-y-2">
              <div>
                <span className="text-slate-400">Don't have an account? </span>
                <Link
                  to="/register"
                  className="text-purple-400 hover:text-purple-300 font-medium transition-colors"
                >
                  Sign up
                </Link>
              </div>
              <div>
                <a
                  href="#"
                  className="text-slate-400 hover:text-slate-300 transition-colors"
                >
                  Forgot your password?
                </a>
              </div>
            </div>
          </form>
        </div>
      </div>
    </div>
  )
}