import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '../../store';
import { login, clearError, selectIsAuthLoading, selectAuthError } from '../../store/slices/authSlice';
import { addNotification } from '../../store/slices/uiSlice';
import type { LoginRequest } from '../../types';

interface LoginFormProps {
  onRegisterClick?: () => void;
}

export const LoginForm: React.FC<LoginFormProps> = ({ onRegisterClick }) => {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const isLoading = useAppSelector(selectIsAuthLoading);
  const error = useAppSelector(selectAuthError);

  const [formData, setFormData] = useState<LoginRequest>({
    username: '',
    password: '',
  });

  const [validationErrors, setValidationErrors] = useState<Record<string, string>>({});

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
    
    // Clear auth error
    if (error) {
      dispatch(clearError());
    }
  };

  const validateForm = (): boolean => {
    const errors: Record<string, string> = {};

    if (!formData.username.trim()) {
      errors.username = 'Username is required';
    }

    if (!formData.password) {
      errors.password = 'Password is required';
    } else if (formData.password.length < 6) {
      errors.password = 'Password must be at least 6 characters';
    }

    setValidationErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!validateForm()) {
      return;
    }

    try {
      const result = await dispatch(login(formData));
      
      if (login.fulfilled.match(result)) {
        dispatch(addNotification({
          type: 'success',
          title: 'Login Successful',
          message: `Welcome back, ${result.payload.user.firstName}!`,
        }));
        navigate('/dashboard');
      }
    } catch (error) {
      // Error is handled by Redux slice
      console.error('Login failed:', error);
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
          }}>Sign in to TradeMaster</h2>
          <p style={{
            color: 'rgba(203, 213, 225, 0.8)',
            fontSize: '16px',
            margin: 0
          }}>Welcome back! Please sign in to your account.</p>
        </div>

        <form onSubmit={handleSubmit} style={{
          display: 'flex',
          flexDirection: 'column',
          gap: '24px'
        }}>
          {/* Username Field */}
          <div>
            <label htmlFor="username" style={{
              display: 'block',
              fontSize: '14px',
              fontWeight: 600,
              color: 'rgba(203, 213, 225, 0.9)',
              marginBottom: '8px'
            }}>
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
              style={{
                width: '100%',
                padding: '16px 20px',
                background: 'linear-gradient(135deg, rgba(255, 255, 255, 0.05), rgba(255, 255, 255, 0.02))',
                backdropFilter: 'blur(10px)',
                border: `1px solid ${validationErrors.username ? '#EF4444' : 'rgba(255, 255, 255, 0.2)'}`,
                borderRadius: '12px',
                color: 'white',
                fontSize: '16px',
                outline: 'none',
                transition: 'all 0.3s ease',
                boxShadow: validationErrors.username ? '0 0 0 1px #EF4444' : 'none'
              }}
              placeholder="Enter your username"
              disabled={isLoading}
              onFocus={(e) => {
                if (!validationErrors.username) {
                  e.target.style.borderColor = 'rgba(139, 92, 246, 0.5)';
                  e.target.style.boxShadow = '0 0 20px rgba(139, 92, 246, 0.2)';
                }
              }}
              onBlur={(e) => {
                if (!validationErrors.username) {
                  e.target.style.borderColor = 'rgba(255, 255, 255, 0.2)';
                  e.target.style.boxShadow = 'none';
                }
              }}
            />
            {validationErrors.username && (
              <p style={{
                color: '#EF4444',
                fontSize: '14px',
                marginTop: '6px',
                margin: '6px 0 0 0'
              }}>{validationErrors.username}</p>
            )}
          </div>

          {/* Password Field */}
          <div>
            <label htmlFor="password" style={{
              display: 'block',
              fontSize: '14px',
              fontWeight: 600,
              color: 'rgba(203, 213, 225, 0.9)',
              marginBottom: '8px'
            }}>
              Password
            </label>
            <input
              id="password"
              name="password"
              type="password"
              autoComplete="current-password"
              required
              value={formData.password}
              onChange={handleInputChange}
              style={{
                width: '100%',
                padding: '16px 20px',
                background: 'linear-gradient(135deg, rgba(255, 255, 255, 0.05), rgba(255, 255, 255, 0.02))',
                backdropFilter: 'blur(10px)',
                border: `1px solid ${validationErrors.password ? '#EF4444' : 'rgba(255, 255, 255, 0.2)'}`,
                borderRadius: '12px',
                color: 'white',
                fontSize: '16px',
                outline: 'none',
                transition: 'all 0.3s ease',
                boxShadow: validationErrors.password ? '0 0 0 1px #EF4444' : 'none'
              }}
              placeholder="Enter your password"
              disabled={isLoading}
              onFocus={(e) => {
                if (!validationErrors.password) {
                  e.target.style.borderColor = 'rgba(139, 92, 246, 0.5)';
                  e.target.style.boxShadow = '0 0 20px rgba(139, 92, 246, 0.2)';
                }
              }}
              onBlur={(e) => {
                if (!validationErrors.password) {
                  e.target.style.borderColor = 'rgba(255, 255, 255, 0.2)';
                  e.target.style.boxShadow = 'none';
                }
              }}
            />
            {validationErrors.password && (
              <p style={{
                color: '#EF4444',
                fontSize: '14px',
                marginTop: '6px',
                margin: '6px 0 0 0'
              }}>{validationErrors.password}</p>
            )}
          </div>

          {/* Error Display */}
          {error && (
            <div style={{
              background: 'linear-gradient(135deg, rgba(239, 68, 68, 0.1), rgba(239, 68, 68, 0.05))',
              border: '1px solid rgba(239, 68, 68, 0.3)',
              borderRadius: '12px',
              padding: '16px'
            }}>
              <div style={{ display: 'flex' }}>
                <div style={{ flexShrink: 0 }}>
                  <svg style={{
                    width: '20px',
                    height: '20px',
                    color: '#EF4444'
                  }} fill="currentColor" viewBox="0 0 20 20">
                    <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clipRule="evenodd" />
                  </svg>
                </div>
                <div style={{ marginLeft: '12px' }}>
                  <p style={{
                    fontSize: '14px',
                    color: '#EF4444',
                    margin: 0
                  }}>{error}</p>
                </div>
              </div>
            </div>
          )}

          {/* Submit Button */}
          <button
            type="submit"
            disabled={isLoading}
            style={{
              width: '100%',
              padding: '16px',
              background: 'linear-gradient(135deg, #8B5CF6, #7C3AED)',
              border: 'none',
              borderRadius: '12px',
              color: 'white',
              fontSize: '16px',
              fontWeight: 600,
              cursor: isLoading ? 'not-allowed' : 'pointer',
              opacity: isLoading ? 0.7 : 1,
              transition: 'all 0.3s ease',
              position: 'relative',
              boxShadow: '0 8px 25px rgba(139, 92, 246, 0.3)'
            }}
            onMouseEnter={(e) => {
              if (!isLoading) {
                e.currentTarget.style.transform = 'translateY(-2px)';
                e.currentTarget.style.boxShadow = '0 12px 35px rgba(139, 92, 246, 0.4)';
              }
            }}
            onMouseLeave={(e) => {
              if (!isLoading) {
                e.currentTarget.style.transform = 'translateY(0)';
                e.currentTarget.style.boxShadow = '0 8px 25px rgba(139, 92, 246, 0.3)';
              }
            }}
          >
            {isLoading ? (
              <div style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center'
              }}>
                <svg style={{
                  animation: 'spin 1s linear infinite',
                  width: '16px',
                  height: '16px',
                  color: 'white',
                  marginRight: '8px'
                }} fill="none" viewBox="0 0 24 24">
                  <circle style={{ opacity: 0.25 }} cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                  <path style={{ opacity: 0.75 }} fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                </svg>
                Signing in...
              </div>
            ) : (
              'Sign in'
            )}
          </button>

          {/* Register Link */}
          <div style={{
            textAlign: 'center',
            paddingTop: '20px',
            borderTop: '1px solid rgba(255, 255, 255, 0.1)'
          }}>
            <p style={{
              fontSize: '14px',
              color: 'rgba(203, 213, 225, 0.8)',
              margin: 0
            }}>
              Don't have an account?{' '}
              <button
                type="button"
                onClick={onRegisterClick}
                style={{
                  fontWeight: 600,
                  color: '#8B5CF6',
                  background: 'none',
                  border: 'none',
                  cursor: isLoading ? 'not-allowed' : 'pointer',
                  textDecoration: 'underline',
                  opacity: isLoading ? 0.5 : 1,
                  transition: 'color 0.3s ease',
                  fontSize: '14px'
                }}
                disabled={isLoading}
                onMouseEnter={(e) => {
                  if (!isLoading) e.currentTarget.style.color = '#7C3AED';
                }}
                onMouseLeave={(e) => {
                  if (!isLoading) e.currentTarget.style.color = '#8B5CF6';
                }}
              >
                Sign up here
              </button>
            </p>
          </div>
        </form>
      </div>
      
      <style>{`
        @keyframes spin {
          from { transform: rotate(0deg); }
          to { transform: rotate(360deg); }
        }
        
        input::placeholder {
          color: rgba(203, 213, 225, 0.5);
        }
      `}</style>
    </div>
  );
};

export default LoginForm;