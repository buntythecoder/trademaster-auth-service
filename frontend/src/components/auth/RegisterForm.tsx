import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { authApi } from '../../lib/api';
import { useAppDispatch } from '../../store';
import { addNotification } from '../../store/slices/uiSlice';

interface RegisterFormProps {
  onLoginClick?: () => void;
}

interface RegisterFormData {
  username: string;
  email: string;
  password: string;
  confirmPassword: string;
  firstName: string;
  lastName: string;
}

export const RegisterForm: React.FC<RegisterFormProps> = ({ onLoginClick }) => {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();

  const [formData, setFormData] = useState<RegisterFormData>({
    username: '',
    email: '',
    password: '',
    confirmPassword: '',
    firstName: '',
    lastName: '',
  });

  const [validationErrors, setValidationErrors] = useState<Record<string, string>>({});
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
    
    // Clear validation error when user starts typing
    if (validationErrors[name]) {
      setValidationErrors(prev => {
        const updated = { ...prev };
        delete updated[name];
        return updated;
      });
    }
    
    // Clear general error
    if (error) {
      setError(null);
    }
  };

  const validateForm = (): boolean => {
    const errors: Record<string, string> = {};

    // Username validation
    if (!formData.username.trim()) {
      errors.username = 'Username is required';
    } else if (formData.username.length < 3) {
      errors.username = 'Username must be at least 3 characters';
    } else if (!/^[a-zA-Z0-9_]+$/.test(formData.username)) {
      errors.username = 'Username can only contain letters, numbers, and underscores';
    }

    // Email validation
    if (!formData.email.trim()) {
      errors.email = 'Email is required';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) {
      errors.email = 'Please enter a valid email address';
    }

    // First name validation
    if (!formData.firstName.trim()) {
      errors.firstName = 'First name is required';
    }

    // Last name validation
    if (!formData.lastName.trim()) {
      errors.lastName = 'Last name is required';
    }

    // Password validation
    if (!formData.password) {
      errors.password = 'Password is required';
    } else if (formData.password.length < 8) {
      errors.password = 'Password must be at least 8 characters';
    } else if (!/(?=.*[a-z])(?=.*[A-Z])(?=.*\d)/.test(formData.password)) {
      errors.password = 'Password must contain at least one uppercase letter, one lowercase letter, and one number';
    }

    // Confirm password validation
    if (!formData.confirmPassword) {
      errors.confirmPassword = 'Please confirm your password';
    } else if (formData.password !== formData.confirmPassword) {
      errors.confirmPassword = 'Passwords do not match';
    }

    setValidationErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!validateForm()) {
      return;
    }

    setIsLoading(true);
    setError(null);

    try {
      const { confirmPassword, ...registrationData } = formData;
      await authApi.register(registrationData);

      dispatch(addNotification({
        type: 'success',
        title: 'Registration Successful',
        message: 'Your account has been created successfully. Please sign in to continue.',
      }));

      // Switch to login form or navigate to login page
      if (onLoginClick) {
        onLoginClick();
      } else {
        navigate('/login');
      }
    } catch (error: any) {
      setError(error.response?.data?.message || 'Registration failed. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div style={{ width: '100%', maxWidth: '450px', margin: '0 auto' }}>
      <div style={{
        background: 'linear-gradient(135deg, rgba(255, 255, 255, 0.1) 0%, rgba(255, 255, 255, 0.05) 100%)',
        backdropFilter: 'blur(30px)',
        border: '1px solid rgba(255, 255, 255, 0.2)',
        borderRadius: '24px',
        padding: '40px',
        boxShadow: '0 20px 60px rgba(0, 0, 0, 0.1), inset 0 1px 0 rgba(255, 255, 255, 0.1)'
      }}>
        <div style={{ textAlign: 'center', marginBottom: '32px' }}>
          <h2 style={{
            fontSize: '28px',
            fontWeight: 700,
            background: 'linear-gradient(135deg, #8B5CF6, #06B6D4)',
            WebkitBackgroundClip: 'text',
            backgroundClip: 'text',
            WebkitTextFillColor: 'transparent',
            marginBottom: '8px'
          }}>Create your TradeMaster account</h2>
          <p style={{
            color: 'rgba(203, 213, 225, 0.8)',
            fontSize: '16px',
            margin: 0
          }}>Join TradeMaster and start trading smarter.</p>
        </div>

        <form onSubmit={handleSubmit} style={{
          display: 'flex',
          flexDirection: 'column',
          gap: '20px'
        }}>
          {/* First Name and Last Name Row */}
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label htmlFor="firstName" className="block text-sm font-medium text-gray-700 mb-1">
                First Name
              </label>
              <input
                id="firstName"
                name="firstName"
                type="text"
                autoComplete="given-name"
                required
                value={formData.firstName}
                onChange={handleInputChange}
                className={`input w-full ${validationErrors.firstName ? 'border-danger-500' : ''}`}
                placeholder="First name"
                disabled={isLoading}
              />
              {validationErrors.firstName && (
                <p className="text-danger-600 text-sm mt-1">{validationErrors.firstName}</p>
              )}
            </div>

            <div>
              <label htmlFor="lastName" className="block text-sm font-medium text-gray-700 mb-1">
                Last Name
              </label>
              <input
                id="lastName"
                name="lastName"
                type="text"
                autoComplete="family-name"
                required
                value={formData.lastName}
                onChange={handleInputChange}
                className={`input w-full ${validationErrors.lastName ? 'border-danger-500' : ''}`}
                placeholder="Last name"
                disabled={isLoading}
              />
              {validationErrors.lastName && (
                <p className="text-danger-600 text-sm mt-1">{validationErrors.lastName}</p>
              )}
            </div>
          </div>

          {/* Username Field */}
          <div>
            <label htmlFor="username" className="block text-sm font-medium text-gray-700 mb-1">
              Username
            </label>
            <input
              id="username"
              name="username"
              type="text"
              autoComplete="username"
              required
              value={formData.username}
              onChange={handleInputChange}
              className={`input w-full ${validationErrors.username ? 'border-danger-500' : ''}`}
              placeholder="Choose a username"
              disabled={isLoading}
            />
            {validationErrors.username && (
              <p className="text-danger-600 text-sm mt-1">{validationErrors.username}</p>
            )}
          </div>

          {/* Email Field */}
          <div>
            <label htmlFor="email" className="block text-sm font-medium text-gray-700 mb-1">
              Email Address
            </label>
            <input
              id="email"
              name="email"
              type="email"
              autoComplete="email"
              required
              value={formData.email}
              onChange={handleInputChange}
              className={`input w-full ${validationErrors.email ? 'border-danger-500' : ''}`}
              placeholder="Enter your email address"
              disabled={isLoading}
            />
            {validationErrors.email && (
              <p className="text-danger-600 text-sm mt-1">{validationErrors.email}</p>
            )}
          </div>

          {/* Password Field */}
          <div>
            <label htmlFor="password" className="block text-sm font-medium text-gray-700 mb-1">
              Password
            </label>
            <input
              id="password"
              name="password"
              type="password"
              autoComplete="new-password"
              required
              value={formData.password}
              onChange={handleInputChange}
              className={`input w-full ${validationErrors.password ? 'border-danger-500' : ''}`}
              placeholder="Create a strong password"
              disabled={isLoading}
            />
            {validationErrors.password && (
              <p className="text-danger-600 text-sm mt-1">{validationErrors.password}</p>
            )}
          </div>

          {/* Confirm Password Field */}
          <div>
            <label htmlFor="confirmPassword" className="block text-sm font-medium text-gray-700 mb-1">
              Confirm Password
            </label>
            <input
              id="confirmPassword"
              name="confirmPassword"
              type="password"
              autoComplete="new-password"
              required
              value={formData.confirmPassword}
              onChange={handleInputChange}
              className={`input w-full ${validationErrors.confirmPassword ? 'border-danger-500' : ''}`}
              placeholder="Confirm your password"
              disabled={isLoading}
            />
            {validationErrors.confirmPassword && (
              <p className="text-danger-600 text-sm mt-1">{validationErrors.confirmPassword}</p>
            )}
          </div>

          {/* Error Display */}
          {error && (
            <div className="bg-danger-50 border border-danger-200 rounded-md p-3">
              <div className="flex">
                <div className="flex-shrink-0">
                  <svg className="h-5 w-5 text-danger-400" fill="currentColor" viewBox="0 0 20 20">
                    <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clipRule="evenodd" />
                  </svg>
                </div>
                <div className="ml-3">
                  <p className="text-sm text-danger-800">{error}</p>
                </div>
              </div>
            </div>
          )}

          {/* Submit Button */}
          <button
            type="submit"
            disabled={isLoading}
            className="btn-primary w-full relative"
          >
            {isLoading ? (
              <div className="flex items-center justify-center">
                <svg className="animate-spin -ml-1 mr-2 h-4 w-4 text-white" fill="none" viewBox="0 0 24 24">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                </svg>
                Creating account...
              </div>
            ) : (
              'Create account'
            )}
          </button>

          {/* Login Link */}
          <div className="text-center pt-4 border-t border-gray-200">
            <p className="text-sm text-gray-600">
              Already have an account?{' '}
              <button
                type="button"
                onClick={onLoginClick}
                className="font-medium text-primary-600 hover:text-primary-500 focus:outline-none focus:underline transition-colors"
                disabled={isLoading}
              >
                Sign in here
              </button>
            </p>
          </div>
        </form>
      </div>
      
      <style>{`
        input::placeholder {
          color: rgba(203, 213, 225, 0.5);
        }
        
        .input {
          background: linear-gradient(135deg, rgba(255, 255, 255, 0.05), rgba(255, 255, 255, 0.02)) !important;
          backdrop-filter: blur(10px) !important;
          border: 1px solid rgba(255, 255, 255, 0.2) !important;
          border-radius: 12px !important;
          color: white !important;
          padding: 16px 20px !important;
          transition: all 0.3s ease !important;
        }
        
        .input:focus {
          border-color: rgba(139, 92, 246, 0.5) !important;
          box-shadow: 0 0 20px rgba(139, 92, 246, 0.2) !important;
          outline: none !important;
        }
        
        .btn-primary {
          background: linear-gradient(135deg, #8B5CF6, #7C3AED) !important;
          border: none !important;
          border-radius: 12px !important;
          color: white !important;
          font-weight: 600 !important;
          padding: 16px !important;
          transition: all 0.3s ease !important;
          box-shadow: 0 8px 25px rgba(139, 92, 246, 0.3) !important;
        }
        
        .btn-primary:hover:not(:disabled) {
          transform: translateY(-2px) !important;
          box-shadow: 0 12px 35px rgba(139, 92, 246, 0.4) !important;
        }
        
        label {
          color: rgba(203, 213, 225, 0.9) !important;
          font-weight: 600 !important;
          font-size: 14px !important;
        }
        
        .text-danger-600 {
          color: #EF4444 !important;
        }
      `}</style>
    </div>
  );
};

export default RegisterForm;