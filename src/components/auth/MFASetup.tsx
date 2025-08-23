import React, { useState } from 'react'
import { Shield, Smartphone, Mail, Key, CheckCircle, AlertTriangle, Copy, QrCode } from 'lucide-react'
import { useToast } from '../../contexts/ToastContext'

type MFAMethod = 'sms' | 'email' | 'totp'

interface MFASetupProps {
  onComplete: (method: MFAMethod, verified: boolean) => void
  onCancel: () => void
}

export function MFASetup({ onComplete, onCancel }: MFASetupProps) {
  const [selectedMethod, setSelectedMethod] = useState<MFAMethod>('totp')
  const [step, setStep] = useState<'select' | 'setup' | 'verify'>('select')
  const [verificationCode, setVerificationCode] = useState('')
  const [isVerifying, setIsVerifying] = useState(false)
  const [backupCodes] = useState([
    '1A2B-3C4D', '5E6F-7G8H', '9I0J-1K2L', '3M4N-5O6P',
    '7Q8R-9S0T', '1U2V-3W4X', '5Y6Z-7A8B', '9C0D-1E2F'
  ])
  const { success, error, info } = useToast()

  // Mock TOTP secret for demonstration
  const totpSecret = 'JBSWY3DPEHPK3PXP'
  const qrCodeUrl = `otpauth://totp/TradeMaster:user@example.com?secret=${totpSecret}&issuer=TradeMaster`

  const handleMethodSelect = (method: MFAMethod) => {
    setSelectedMethod(method)
    setStep('setup')
  }

  const handleVerification = async () => {
    setIsVerifying(true)
    
    // Simulate API call
    await new Promise(resolve => setTimeout(resolve, 2000))
    
    // Mock verification - accept 123456 as valid
    if (verificationCode === '123456' || verificationCode.length === 6) {
      success('MFA Enabled Successfully', 'Your account is now protected with multi-factor authentication')
      setIsVerifying(false)
      onComplete(selectedMethod, true)
    } else {
      error('Verification Failed', 'Invalid verification code. Please try again.')
      setIsVerifying(false)
    }
  }

  const copyToClipboard = (text: string) => {
    navigator.clipboard.writeText(text)
    info('Copied to clipboard', 'Secret key copied successfully')
  }

  const copyBackupCodes = () => {
    const codesText = backupCodes.join('\n')
    navigator.clipboard.writeText(codesText)
    success('Backup codes copied', 'Store these codes in a safe place')
  }

  const methods = [
    {
      id: 'totp' as MFAMethod,
      name: 'Authenticator App',
      description: 'Use Google Authenticator, Authy, or similar apps',
      icon: Key,
      recommended: true,
      setup: 'Install an authenticator app and scan the QR code'
    },
    {
      id: 'sms' as MFAMethod,
      name: 'SMS Verification',
      description: 'Receive codes via text message',
      icon: Smartphone,
      recommended: false,
      setup: 'We\'ll send verification codes to your phone'
    },
    {
      id: 'email' as MFAMethod,
      name: 'Email Verification',
      description: 'Receive codes via email',
      icon: Mail,
      recommended: false,
      setup: 'We\'ll send verification codes to your email'
    }
  ]

  if (step === 'select') {
    return (
      <div className="glass-card rounded-2xl p-8 max-w-2xl mx-auto">
        <div className="text-center mb-8">
          <div className="flex items-center justify-center w-16 h-16 mb-4 mx-auto glass-card rounded-2xl">
            <Shield className="w-8 h-8 text-green-400" />
          </div>
          <h2 className="text-2xl font-bold text-white mb-2">Enable Two-Factor Authentication</h2>
          <p className="text-slate-400">
            Add an extra layer of security to protect your trading account
          </p>
        </div>

        <div className="space-y-4 mb-8">
          {methods.map((method) => (
            <div
              key={method.id}
              onClick={() => handleMethodSelect(method.id)}
              className="flex items-start space-x-4 p-4 rounded-xl bg-slate-800/30 hover:bg-slate-700/50 cursor-pointer transition-all group"
            >
              <div className={`p-3 rounded-xl ${
                method.recommended 
                  ? 'bg-green-500/20 text-green-400' 
                  : 'bg-slate-600/50 text-slate-400'
              }`}>
                <method.icon className="w-5 h-5" />
              </div>
              <div className="flex-1">
                <div className="flex items-center space-x-2">
                  <h3 className="font-semibold text-white group-hover:text-purple-400 transition-colors">
                    {method.name}
                  </h3>
                  {method.recommended && (
                    <span className="px-2 py-1 text-xs font-medium bg-green-500/20 text-green-400 rounded-lg">
                      Recommended
                    </span>
                  )}
                </div>
                <p className="text-slate-400 text-sm mt-1">{method.description}</p>
                <p className="text-slate-500 text-xs mt-2">{method.setup}</p>
              </div>
            </div>
          ))}
        </div>

        <div className="flex space-x-4">
          <button
            onClick={onCancel}
            className="flex-1 py-3 px-6 rounded-xl font-semibold glass-card text-white hover:text-purple-300 transition-colors border border-purple-500/50 hover:border-purple-400/70"
          >
            Skip for Now
          </button>
        </div>
      </div>
    )
  }

  if (step === 'setup') {
    const currentMethod = methods.find(m => m.id === selectedMethod)!

    return (
      <div className="glass-card rounded-2xl p-8 max-w-2xl mx-auto">
        <div className="text-center mb-8">
          <div className={`flex items-center justify-center w-16 h-16 mb-4 mx-auto glass-card rounded-2xl`}>
            <currentMethod.icon className="w-8 h-8 text-purple-400" />
          </div>
          <h2 className="text-2xl font-bold text-white mb-2">Setup {currentMethod.name}</h2>
          <p className="text-slate-400">{currentMethod.setup}</p>
        </div>

        {selectedMethod === 'totp' && (
          <div className="space-y-6 mb-8">
            {/* QR Code Section */}
            <div className="text-center">
              <div className="inline-block p-6 bg-white rounded-2xl mb-4">
                <div className="w-48 h-48 bg-slate-100 rounded-xl flex items-center justify-center">
                  <div className="text-center">
                    <QrCode className="w-16 h-16 text-slate-400 mx-auto mb-2" />
                    <p className="text-slate-600 text-sm">QR Code</p>
                    <p className="text-slate-500 text-xs">Scan with your app</p>
                  </div>
                </div>
              </div>
              <p className="text-slate-400 text-sm mb-4">
                Scan this QR code with your authenticator app
              </p>
            </div>

            {/* Manual Setup */}
            <div className="glass-card rounded-xl p-4 bg-slate-800/30">
              <h4 className="font-semibold text-white mb-3">Can't scan? Enter manually:</h4>
              <div className="flex items-center space-x-2">
                <code className="flex-1 p-2 bg-slate-700/50 rounded text-sm font-mono text-slate-300 break-all">
                  {totpSecret}
                </code>
                <button
                  onClick={() => copyToClipboard(totpSecret)}
                  className="p-2 rounded-lg hover:bg-slate-600/50 transition-colors text-slate-400 hover:text-white"
                >
                  <Copy className="w-4 h-4" />
                </button>
              </div>
            </div>

            {/* Backup Codes */}
            <div className="glass-card rounded-xl p-4 bg-slate-800/30">
              <div className="flex items-center justify-between mb-3">
                <h4 className="font-semibold text-white">Backup Recovery Codes</h4>
                <button
                  onClick={copyBackupCodes}
                  className="flex items-center space-x-1 text-sm text-purple-400 hover:text-purple-300"
                >
                  <Copy className="w-4 h-4" />
                  <span>Copy All</span>
                </button>
              </div>
              <div className="grid grid-cols-2 gap-2 text-sm font-mono">
                {backupCodes.map((code, index) => (
                  <div key={index} className="p-2 bg-slate-700/30 rounded text-slate-300 text-center">
                    {code}
                  </div>
                ))}
              </div>
              <div className="mt-3 p-3 rounded-lg bg-yellow-500/10 border border-yellow-500/30">
                <div className="flex items-start space-x-2">
                  <AlertTriangle className="w-4 h-4 text-yellow-400 mt-0.5 flex-shrink-0" />
                  <p className="text-yellow-400 text-xs">
                    <strong>Important:</strong> Save these backup codes in a secure location. 
                    Each code can only be used once to access your account if you lose your device.
                  </p>
                </div>
              </div>
            </div>
          </div>
        )}

        {selectedMethod === 'sms' && (
          <div className="text-center mb-8">
            <div className="glass-card rounded-xl p-6 bg-slate-800/30 mb-4">
              <Smartphone className="w-12 h-12 text-cyan-400 mx-auto mb-4" />
              <p className="text-white font-semibold mb-2">Phone Number Verification</p>
              <p className="text-slate-400 text-sm">
                We'll send verification codes to your registered phone number: 
                <span className="text-white font-mono ml-1">+91 ****-***-123</span>
              </p>
            </div>
          </div>
        )}

        {selectedMethod === 'email' && (
          <div className="text-center mb-8">
            <div className="glass-card rounded-xl p-6 bg-slate-800/30 mb-4">
              <Mail className="w-12 h-12 text-blue-400 mx-auto mb-4" />
              <p className="text-white font-semibold mb-2">Email Verification</p>
              <p className="text-slate-400 text-sm">
                We'll send verification codes to your registered email: 
                <span className="text-white font-mono ml-1">user@example.com</span>
              </p>
            </div>
          </div>
        )}

        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-slate-300 mb-2">
              Enter verification code to test
            </label>
            <input
              type="text"
              value={verificationCode}
              onChange={(e) => setVerificationCode(e.target.value.replace(/\D/g, '').slice(0, 6))}
              placeholder="123456"
              className="cyber-input w-full py-3 rounded-xl text-white text-center font-mono text-lg tracking-widest"
              maxLength={6}
            />
            <p className="text-slate-500 text-xs mt-2 text-center">
              {selectedMethod === 'totp' && 'Enter the 6-digit code from your authenticator app'}
              {selectedMethod === 'sms' && 'Enter the 6-digit code sent to your phone'}
              {selectedMethod === 'email' && 'Enter the 6-digit code sent to your email'}
            </p>
          </div>

          <div className="flex space-x-4">
            <button
              onClick={() => setStep('select')}
              className="flex-1 py-3 px-6 rounded-xl font-semibold glass-card text-white hover:text-purple-300 transition-colors border border-purple-500/50 hover:border-purple-400/70"
            >
              Back
            </button>
            <button
              onClick={handleVerification}
              disabled={verificationCode.length !== 6 || isVerifying}
              className="flex-1 cyber-button py-3 px-6 rounded-xl font-semibold disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {isVerifying ? (
                <div className="flex items-center justify-center space-x-2">
                  <div className="loading-dots">
                    <div className="loading-dot"></div>
                    <div className="loading-dot"></div>
                    <div className="loading-dot"></div>
                  </div>
                  <span>Verifying...</span>
                </div>
              ) : (
                'Verify & Enable'
              )}
            </button>
          </div>
        </div>

        {/* Demo hint */}
        <div className="mt-6 text-center p-3 rounded-lg bg-purple-500/10 border border-purple-500/30">
          <p className="text-purple-400 text-sm">
            💡 <strong>Demo:</strong> Use code <code className="font-mono bg-purple-500/20 px-1 rounded">123456</code> to test verification
          </p>
        </div>
      </div>
    )
  }

  return null
}