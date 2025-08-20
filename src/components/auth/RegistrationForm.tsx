import React, { useState, useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useNavigate, Link } from 'react-router-dom'
import { 
  User, 
  Mail, 
  Phone, 
  Lock, 
  Eye, 
  EyeOff, 
  CheckCircle, 
  XCircle, 
  TrendingUp,
  ArrowLeft,
  Shield,
  Sparkles,
  Zap
} from 'lucide-react'
import { cn } from '@/lib/utils'
import { ParticleSystem } from '@/components/effects/ParticleSystem'
import { FloatingShapes } from '@/components/effects/FloatingShapes'

const registrationSchema = z.object({
  firstName: z.string().min(2, 'First name must be at least 2 characters'),
  lastName: z.string().min(2, 'Last name must be at least 2 characters'),
  email: z.string().email('Please enter a valid email address'),
  phone: z.string().min(10, 'Please enter a valid phone number'),
  password: z.string().min(8, 'Password must be at least 8 characters'),
})

type RegistrationFormData = z.infer<typeof registrationSchema>

interface ValidationState {
  isValid: boolean;
  isDirty: boolean;
  strength?: {
    score: number;
    feedback: string[];
    color: string;
    label: string;
  };
}

interface FormValidationStates {
  firstName: ValidationState;
  lastName: ValidationState;
  email: ValidationState;
  phone: ValidationState;
  password: ValidationState;
}

// Password strength calculation
const calculatePasswordStrength = (password: string) => {
  if (!password) return { score: 0, feedback: [], color: 'slate', label: 'Enter password' };
  
  let score = 0;
  const feedback: string[] = [];
  
  if (password.length >= 8) score++;
  else feedback.push('At least 8 characters');
  
  if (/[a-z]/.test(password)) score++;
  else feedback.push('Include lowercase letter');
  
  if (/[A-Z]/.test(password)) score++;
  else feedback.push('Include uppercase letter');
  
  if (/\d/.test(password)) score++;
  else feedback.push('Include number');
  
  if (/[^\w\s]/.test(password)) score++;
  else feedback.push('Include special character');
  
  const colors = ['red', 'orange', 'yellow', 'blue', 'green'];
  const labels = ['Very Weak', 'Weak', 'Fair', 'Good', 'Strong'];
  
  return {
    score,
    feedback,
    color: colors[Math.min(score - 1, 4)] || 'slate',
    label: labels[Math.min(score - 1, 4)] || 'Very Weak'
  };
};

// Email validation
const validateEmail = (email: string) => {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return {
    isValid: emailRegex.test(email),
    message: emailRegex.test(email) ? 'Valid email' : 'Invalid email format'
  };
};

// Name validation
const validateName = (name: string) => {
  return {
    isValid: name.length >= 2 && /^[a-zA-Z\s]+$/.test(name),
    message: name.length >= 2 && /^[a-zA-Z\s]+$/.test(name) ? 'Valid name' : 'Invalid name'
  };
};

// Phone formatting
const formatPhoneNumber = (value: string) => {
  const cleaned = value.replace(/\D/g, '');
  if (cleaned.length <= 10) {
    const formatted = cleaned.replace(/(\d{0,5})(\d{0,5})/, '$1 $2').trim();
    return `+91 ${formatted}`;
  }
  return value;
};

export interface RegistrationFormProps {
  onSubmit?: (data: RegistrationFormData) => Promise<void>;
  isLoading?: boolean;
  className?: string;
}

// Custom hook for real-time validation
const useRealtimeValidation = (watch: any) => {
  const [validationStates, setValidationStates] = useState<FormValidationStates>({
    firstName: { isValid: false, isDirty: false },
    lastName: { isValid: false, isDirty: false },
    email: { isValid: false, isDirty: false },
    phone: { isValid: false, isDirty: false },
    password: { 
      isValid: false, 
      isDirty: false,
      strength: calculatePasswordStrength('')
    },
  });

  const watchedFields = watch(['firstName', 'lastName', 'email', 'phone', 'password']);

  useEffect(() => {
    const [firstName, lastName, email, phone, password] = watchedFields;

    const timeoutId = setTimeout(() => {
      setValidationStates(prev => ({
        firstName: {
          ...prev.firstName,
          isDirty: !!firstName,
          isValid: firstName ? validateName(firstName).isValid : false,
        },
        lastName: {
          ...prev.lastName,
          isDirty: !!lastName,
          isValid: lastName ? validateName(lastName).isValid : false,
        },
        email: {
          ...prev.email,
          isDirty: !!email,
          isValid: email ? validateEmail(email).isValid : false,
        },
        phone: {
          ...prev.phone,
          isDirty: !!phone,
          isValid: !!phone && phone.length >= 13,
        },
        password: {
          ...prev.password,
          isDirty: !!password,
          isValid: password ? calculatePasswordStrength(password).score >= 3 : false,
          strength: calculatePasswordStrength(password || ''),
        },
      }));
    }, 300);

    return () => clearTimeout(timeoutId);
  }, [watchedFields]);

  return validationStates;
};

// Field status helper
const getFieldStatus = (field: string, validationStates: FormValidationStates) => {
  const state = validationStates[field as keyof FormValidationStates];
  if (!state.isDirty) return '';
  return state.isValid ? 'success' : 'error';
};

export function RegistrationForm({ onSubmit, isLoading = false, className }: RegistrationFormProps) {
  const navigate = useNavigate();
  const [showPassword, setShowPassword] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [currentStep, setCurrentStep] = useState(1);

  const {
    register,
    handleSubmit,
    watch,
    setValue,
    formState: { errors }
  } = useForm<RegistrationFormData>({
    resolver: zodResolver(registrationSchema),
    mode: 'onChange'
  });

  const validationStates = useRealtimeValidation(watch);

  const handleFormSubmit = async (data: RegistrationFormData) => {
    try {
      setIsSubmitting(true);
      console.log('Registration data:', data);
      
      // Simulate API call
      await new Promise(resolve => setTimeout(resolve, 2000));
      
      if (onSubmit) {
        await onSubmit(data);
      }
      
      // Navigate to success page or next step
      navigate('/auth/verify-email', { state: { email: data.email } });
    } catch (error) {
      console.error('Registration failed:', error);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handlePhoneChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const formatted = formatPhoneNumber(e.target.value);
    setValue('phone', formatted, { shouldValidate: true });
  };

  const completionPercentage = React.useMemo(() => {
    const fields = ['firstName', 'lastName', 'email', 'phone', 'password'] as const;
    const completedFields = fields.filter(field => 
      validationStates[field].isValid && validationStates[field].isDirty
    ).length;
    return (completedFields / fields.length) * 100;
  }, [validationStates]);

  const isFormValid = Object.values(validationStates).every(state => 
    state.isValid && state.isDirty
  );

  return (
    <div className="min-h-screen hero-gradient flex items-center justify-center p-4 relative overflow-hidden">
      <ParticleSystem />
      <FloatingShapes />
      
      {/* Back to Login */}
      <Link 
        to="/auth/login" 
        className="fixed top-6 left-6 z-50 cyber-button cyber-button-ghost flex items-center gap-2 px-4 py-2"
      >
        <ArrowLeft className="w-4 h-4" />
        <span>Back to Login</span>
      </Link>

      <div className="relative z-10 w-full max-w-md mx-auto animate-fade-in">
        <div className="glass-card rounded-3xl p-8">
          {/* Header */}
          <div className="text-center mb-8">
            <div className="flex items-center justify-center mb-4">
              <div className="cyber-glow w-12 h-12 rounded-xl bg-gradient-to-r from-blue-600 to-purple-600 flex items-center justify-center">
                <TrendingUp className="w-6 h-6 text-white" />
              </div>
            </div>
            <h1 className="text-3xl font-bold gradient-text mb-2">Join TradeMaster</h1>
            <p className="text-slate-400">Create your professional trading account</p>
          </div>

          {/* Progress Indicator */}
          <div className="mb-8">
            <div className="flex items-center justify-between text-sm mb-2">
              <span className="text-slate-400">Profile Setup</span>
              <span className="text-cyan-400 font-medium">{Math.round(completionPercentage)}% Complete</span>
            </div>
            <div className="h-2 bg-slate-800 rounded-full overflow-hidden">
              <div 
                className="h-full bg-gradient-to-r from-blue-500 to-cyan-400 rounded-full transition-all duration-500 cyber-glow-sm"
                style={{ width: `${completionPercentage}%` }}
              />
            </div>
          </div>

          {/* Registration Form */}
          <form onSubmit={handleSubmit(handleFormSubmit)} className="space-y-6">
            {/* Name Fields */}
            <div className="grid grid-cols-2 gap-4">
              <div className="relative">
                <User className="absolute left-4 top-4 h-5 w-5 text-slate-400" />
                <input
                  {...register('firstName')}
                  type="text"
                  placeholder="First Name"
                  className={`cyber-input w-full pl-12 pr-12 py-4 rounded-2xl text-white placeholder-slate-400 ${
                    getFieldStatus('firstName', validationStates) === 'success' ? 'success' :
                    getFieldStatus('firstName', validationStates) === 'error' ? 'error' : ''
                  }`}
                />
                {getFieldStatus('firstName', validationStates) === 'success' && (
                  <CheckCircle className="absolute right-4 top-4 h-5 w-5 text-green-400" />
                )}
                {getFieldStatus('firstName', validationStates) === 'error' && (
                  <XCircle className="absolute right-4 top-4 h-5 w-5 text-red-400" />
                )}
                {errors.firstName && (
                  <p className="text-red-400 text-sm mt-2 ml-2">{errors.firstName.message}</p>
                )}
              </div>

              <div className="relative">
                <User className="absolute left-4 top-4 h-5 w-5 text-slate-400" />
                <input
                  {...register('lastName')}
                  type="text"
                  placeholder="Last Name"
                  className={`cyber-input w-full pl-12 pr-12 py-4 rounded-2xl text-white placeholder-slate-400 ${
                    getFieldStatus('lastName', validationStates) === 'success' ? 'success' :
                    getFieldStatus('lastName', validationStates) === 'error' ? 'error' : ''
                  }`}
                />
                {getFieldStatus('lastName', validationStates) === 'success' && (
                  <CheckCircle className="absolute right-4 top-4 h-5 w-5 text-green-400" />
                )}
                {getFieldStatus('lastName', validationStates) === 'error' && (
                  <XCircle className="absolute right-4 top-4 h-5 w-5 text-red-400" />
                )}
                {errors.lastName && (
                  <p className="text-red-400 text-sm mt-2 ml-2">{errors.lastName.message}</p>
                )}
              </div>
            </div>

            {/* Email Field */}
            <div className="relative">
              <Mail className="absolute left-4 top-4 h-5 w-5 text-slate-400" />
              <input
                {...register('email')}
                type="email"
                placeholder="Email Address"
                className={`cyber-input w-full pl-12 pr-12 py-4 rounded-2xl text-white placeholder-slate-400 ${
                  getFieldStatus('email', validationStates) === 'success' ? 'success' :
                  getFieldStatus('email', validationStates) === 'error' ? 'error' : ''
                }`}
              />
              {getFieldStatus('email', validationStates) === 'success' && (
                <CheckCircle className="absolute right-4 top-4 h-5 w-5 text-green-400" />
              )}
              {getFieldStatus('email', validationStates) === 'error' && (
                <XCircle className="absolute right-4 top-4 h-5 w-5 text-red-400" />
              )}
              {errors.email && (
                <p className="text-red-400 text-sm mt-2 ml-2">{errors.email.message}</p>
              )}
            </div>

            {/* Phone Field */}
            <div className="relative">
              <Phone className="absolute left-4 top-4 h-5 w-5 text-slate-400" />
              <input
                {...register('phone')}
                type="tel"
                placeholder="+91 Phone Number"
                onChange={handlePhoneChange}
                className={`cyber-input w-full pl-12 pr-12 py-4 rounded-2xl text-white placeholder-slate-400 ${
                  getFieldStatus('phone', validationStates) === 'success' ? 'success' :
                  getFieldStatus('phone', validationStates) === 'error' ? 'error' : ''
                }`}
              />
              {getFieldStatus('phone', validationStates) === 'success' && (
                <CheckCircle className="absolute right-4 top-4 h-5 w-5 text-green-400" />
              )}
              {getFieldStatus('phone', validationStates) === 'error' && (
                <XCircle className="absolute right-4 top-4 h-5 w-5 text-red-400" />
              )}
              {errors.phone && (
                <p className="text-red-400 text-sm mt-2 ml-2">{errors.phone.message}</p>
              )}
            </div>

            {/* Password Field */}
            <div className="relative">
              <Lock className="absolute left-4 top-4 h-5 w-5 text-slate-400" />
              <input
                {...register('password')}
                type={showPassword ? 'text' : 'password'}
                placeholder="Create Password"
                className={`cyber-input w-full pl-12 pr-12 py-4 rounded-2xl text-white placeholder-slate-400 ${
                  getFieldStatus('password', validationStates) === 'success' ? 'success' :
                  getFieldStatus('password', validationStates) === 'error' ? 'error' : ''
                }`}
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                className="absolute right-4 top-4 text-slate-400 hover:text-white transition-colors"
              >
                {showPassword ? <EyeOff className="h-5 w-5" /> : <Eye className="h-5 w-5" />}
              </button>
              {errors.password && (
                <p className="text-red-400 text-sm mt-2 ml-2">{errors.password.message}</p>
              )}
              
              {/* Password Strength Indicator */}
              {validationStates.password.isDirty && validationStates.password.strength && (
                <div className="mt-3 p-3 bg-slate-800/50 rounded-lg border border-slate-700">
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-sm text-slate-400">Password Strength</span>
                    <span className={`text-sm font-medium text-${validationStates.password.strength.color}-400`}>
                      {validationStates.password.strength.label}
                    </span>
                  </div>
                  <div className="flex gap-1 mb-2">
                    {[1, 2, 3, 4, 5].map((level) => (
                      <div
                        key={level}
                        className={`h-1 flex-1 rounded-full ${
                          level <= validationStates.password.strength!.score
                            ? `bg-${validationStates.password.strength!.color}-400`
                            : 'bg-slate-600'
                        }`}
                      />
                    ))}
                  </div>
                  {validationStates.password.strength.feedback.length > 0 && (
                    <ul className="text-xs text-slate-400 space-y-1">
                      {validationStates.password.strength.feedback.map((item, index) => (
                        <li key={index} className="flex items-center gap-2">
                          <div className="w-1 h-1 bg-slate-500 rounded-full" />
                          {item}
                        </li>
                      ))}
                    </ul>
                  )}
                </div>
              )}
            </div>

            {/* Submit Button */}
            <button
              type="submit"
              disabled={!isFormValid || isSubmitting}
              className="cyber-button cyber-button-primary w-full py-4 text-lg font-semibold rounded-2xl transition-all duration-300 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {isSubmitting ? (
                <div className="flex items-center justify-center gap-3">
                  <div className="w-5 h-5 border-2 border-current border-t-transparent rounded-full animate-spin" />
                  <span>Creating Account...</span>
                </div>
              ) : (
                <div className="flex items-center justify-center gap-3">
                  <Sparkles className="w-5 h-5" />
                  <span>Create Trading Account</span>
                  <Zap className="w-5 h-5" />
                </div>
              )}
            </button>

            {/* Terms and Privacy */}
            <div className="text-center text-sm text-slate-400">
              By creating an account, you agree to our{' '}
              <Link to="/terms" className="text-cyan-400 hover:text-cyan-300 underline">
                Terms of Service
              </Link>
              {' '}and{' '}
              <Link to="/privacy" className="text-cyan-400 hover:text-cyan-300 underline">
                Privacy Policy
              </Link>
            </div>
          </form>

          {/* Already have account */}
          <div className="text-center mt-8 pt-6 border-t border-slate-700">
            <p className="text-slate-400">
              Already have an account?{' '}
              <Link 
                to="/auth/login" 
                className="text-cyan-400 hover:text-cyan-300 font-medium transition-colors"
              >
                Sign In
              </Link>
            </p>
          </div>

          {/* Security Features */}
          <div className="grid grid-cols-3 gap-4 mt-6">
            <div className="text-center p-3 bg-slate-800/50 rounded-xl border border-slate-700">
              <Shield className="w-6 h-6 text-green-400 mx-auto mb-1" />
              <div className="text-xs text-slate-400">SSL Encrypted</div>
            </div>
            <div className="text-center p-3 bg-slate-800/50 rounded-xl border border-slate-700">
              <Lock className="w-6 h-6 text-blue-400 mx-auto mb-1" />
              <div className="text-xs text-slate-400">Secure Storage</div>
            </div>
            <div className="text-center p-3 bg-slate-800/50 rounded-xl border border-slate-700">
              <CheckCircle className="w-6 h-6 text-purple-400 mx-auto mb-1" />
              <div className="text-xs text-slate-400">Verified Platform</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}