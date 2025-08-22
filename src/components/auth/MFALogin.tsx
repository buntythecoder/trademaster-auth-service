import React, { useState } from 'react'
import { Shield, ArrowLeft, Clock, AlertTriangle, CheckCircle } from 'lucide-react'
import { useToast } from '../../contexts/ToastContext'

interface MFALoginProps {
  email: string
  onMFAComplete: () => void
  onBack: () => void
}

export function MFALogin({ email, onMFAComplete, onBack }: MFALoginProps) {
  const [mfaCode, setMfaCode] = useState(['', '', '', '', '', ''])
  const [isVerifying, setIsVerifying] = useState(false)
  const [error, setError] = useState('')
  const [resendCooldown, setResendCooldown] = useState(0)
  const { success, error: showError } = useToast()

  const handleCodeChange = (index: number, value: string) => {
    if (value.length > 1) return
    
    const newCode = [...mfaCode]
    newCode[index] = value
    setMfaCode(newCode)
    
    // Auto-focus next input
    if (value && index < 5) {
      const nextInput = document.getElementById(`mfa-${index + 1}`)
      nextInput?.focus()
    }
  }

  const handleKeyDown = (index: number, e: React.KeyboardEvent) => {
    if (e.key === 'Backspace' && !mfaCode[index] && index > 0) {
      const prevInput = document.getElementById(`mfa-${index - 1}`)
      prevInput?.focus()
    }
  }

  const handleVerify = async () => {
    const code = mfaCode.join('')
    if (code.length !== 6) {
      setError('Please enter a complete 6-digit code')
      return
    }

    setIsVerifying(true)
    setError('')

    try {
      // Simulate MFA verification
      await new Promise(resolve => setTimeout(resolve, 1500))
      
      // For demo purposes, accept any 6-digit code
      if (code === '123456' || code.match(/^\d{6}$/)) {
        success('MFA Verified', 'Login successful!')
        onMFAComplete()
      } else {
        setError('Invalid verification code')
      }
    } catch (err) {
      setError('Verification failed. Please try again.')
    } finally {
      setIsVerifying(false)
    }
  }

  const handleResend = async () => {
    setResendCooldown(30)
    success('Code Sent', 'New verification code sent to your authenticator app')
    
    const countdown = setInterval(() => {
      setResendCooldown(prev => {
        if (prev <= 1) {
          clearInterval(countdown)
          return 0
        }
        return prev - 1
      })
    }, 1000)
  }

  return (
    <div className="min-h-screen hero-gradient flex items-center justify-center p-4 relative overflow-hidden">
      {/* Background Effects */}
      <div className="absolute inset-0">
        <div className="absolute top-20 left-10 w-72 h-72 bg-purple-500/10 rounded-full blur-3xl" />
        <div className="absolute bottom-20 right-10 w-96 h-96 bg-cyan-500/10 rounded-full blur-3xl" />
        <div className="absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 w-[600px] h-[600px] bg-gradient-radial from-purple-500/5 to-transparent rounded-full" />
      </div>

      <div className="relative z-10 w-full max-w-md">
        <div className="glass-card rounded-2xl p-8 space-y-6">
          {/* Header */}
          <div className="text-center space-y-4">
            <div className="flex items-center justify-center w-16 h-16 bg-gradient-to-br from-purple-500 to-cyan-500 rounded-2xl mx-auto">
              <Shield className="w-8 h-8 text-white" />
            </div>
            <div>
              <h1 className="text-2xl font-bold gradient-text">Two-Factor Authentication</h1>
              <p className="text-slate-400 text-sm mt-2">
                Enter the 6-digit code from your authenticator app
              </p>
              <p className="text-slate-500 text-xs mt-1">
                Signed in as: {email}
              </p>
            </div>
          </div>

          {/* MFA Code Input */}
          <div className="space-y-4">
            <div className="flex justify-center space-x-3">
              {mfaCode.map((digit, index) => (
                <input
                  key={index}
                  id={`mfa-${index}`}
                  type="text"
                  maxLength={1}
                  value={digit}
                  onChange={(e) => handleCodeChange(index, e.target.value)}
                  onKeyDown={(e) => handleKeyDown(index, e)}
                  className="w-12 h-12 text-center text-lg font-semibold rounded-xl bg-slate-800/50 border border-slate-600/50 text-white focus:border-purple-500/50 focus:ring-1 focus:ring-purple-500/30 focus:outline-none transition-all"
                  placeholder="0"
                />
              ))}
            </div>

            {error && (
              <div className="flex items-center justify-center space-x-2 text-red-400 text-sm">
                <AlertTriangle className="w-4 h-4" />
                <span>{error}</span>
              </div>
            )}
          </div>

          {/* Actions */}
          <div className="space-y-4">
            <button
              onClick={handleVerify}
              disabled={isVerifying || mfaCode.join('').length !== 6}
              className="w-full cyber-button py-4 rounded-2xl font-semibold text-lg transition-all disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {isVerifying ? (
                <div className="flex items-center justify-center space-x-2">
                  <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                  <span>Verifying...</span>
                </div>
              ) : (
                <div className="flex items-center justify-center space-x-2">
                  <CheckCircle className="w-5 h-5" />
                  <span>Verify Code</span>
                </div>
              )}
            </button>

            <div className="text-center space-y-3">
              <button
                onClick={handleResend}
                disabled={resendCooldown > 0}
                className="text-sm text-slate-400 hover:text-purple-400 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {resendCooldown > 0 ? (
                  <div className="flex items-center justify-center space-x-2">
                    <Clock className="w-4 h-4" />
                    <span>Resend code in {resendCooldown}s</span>
                  </div>
                ) : (
                  'Resend verification code'
                )}
              </button>

              <button
                onClick={onBack}
                className="flex items-center justify-center space-x-2 text-sm text-slate-400 hover:text-white transition-colors mx-auto"
              >
                <ArrowLeft className="w-4 h-4" />
                <span>Back to login</span>
              </button>
            </div>
          </div>

          {/* Help Text */}
          <div className="text-center">
            <p className="text-xs text-slate-500">
              Can't access your authenticator? Contact support for help.
            </p>
          </div>
        </div>
      </div>
    </div>
  )
}