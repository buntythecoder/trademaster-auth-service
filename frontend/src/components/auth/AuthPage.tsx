import React, { useState, useEffect } from 'react';
import { Navigate } from 'react-router-dom';
import { useAppSelector, useAppDispatch } from '../../store';
import { initializeAuth, selectIsAuthenticated, selectIsAuthInitialized } from '../../store/slices/authSlice';
import { LoginForm } from './LoginForm';
import { RegisterForm } from './RegisterForm';

type AuthMode = 'login' | 'register';

export const AuthPage: React.FC = () => {
  const dispatch = useAppDispatch();
  const isAuthenticated = useAppSelector(selectIsAuthenticated);
  const isInitialized = useAppSelector(selectIsAuthInitialized);
  const [authMode, setAuthMode] = useState<AuthMode>('login');

  // Initialize auth state on component mount
  useEffect(() => {
    if (!isInitialized) {
      dispatch(initializeAuth());
    }
  }, [dispatch, isInitialized]);

  // Redirect if already authenticated
  if (isInitialized && isAuthenticated) {
    return <Navigate to="/dashboard" replace />;
  }

  // Show loading while initializing
  if (!isInitialized) {
    return (
      <div style={{
        minHeight: '100vh',
        background: 'radial-gradient(ellipse at top, #0f0f23 0%, #1a0b2e 25%, #0a0a0a 100%)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center'
      }}>
        <div style={{
          background: 'linear-gradient(135deg, rgba(255, 255, 255, 0.1) 0%, rgba(255, 255, 255, 0.05) 100%)',
          backdropFilter: 'blur(30px)',
          border: '1px solid rgba(255, 255, 255, 0.2)',
          borderRadius: '20px',
          padding: '40px',
          maxWidth: '400px',
          width: '90%',
          margin: '0 20px'
        }}>
          <div style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            gap: '12px'
          }}>
            <svg style={{
              animation: 'spin 1s linear infinite',
              width: '32px',
              height: '32px',
              color: '#8B5CF6'
            }} fill="none" viewBox="0 0 24 24">
              <circle style={{ opacity: 0.25 }} cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
              <path style={{ opacity: 0.75 }} fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
            </svg>
            <span style={{
              fontSize: '18px',
              color: 'white',
              fontWeight: 600
            }}>Initializing TradeMaster...</span>
          </div>
        </div>
        
        <style>{`
          @keyframes spin {
            from { transform: rotate(0deg); }
            to { transform: rotate(360deg); }
          }
        `}</style>
      </div>
    );
  }

  return (
    <div style={{
      minHeight: '100vh',
      background: 'radial-gradient(ellipse at top, #0f0f23 0%, #1a0b2e 25%, #0a0a0a 100%)',
      position: 'relative'
    }}>
      {/* Particle System */}
      <div style={{
        position: 'absolute',
        top: 0,
        left: 0,
        width: '100%',
        height: '100%',
        pointerEvents: 'none',
        overflow: 'hidden',
        zIndex: 1
      }}>
        {Array.from({ length: 30 }, (_, i) => (
          <div
            key={i}
            style={{
              position: 'absolute',
              width: '3px',
              height: '3px',
              background: 'linear-gradient(45deg, #8B5CF6, #06B6D4, #10B981)',
              borderRadius: '50%',
              left: `${Math.random() * 100}%`,
              top: `${Math.random() * 100}%`,
              animationName: 'particleGlow',
              animationDuration: `${3 + Math.random() * 4}s`,
              animationIterationCount: 'infinite',
              animationTimingFunction: 'ease-in-out',
              animationDelay: `${Math.random() * 5}s`,
              boxShadow: '0 0 10px rgba(139, 92, 246, 0.5)'
            }}
          />
        ))}
      </div>

      {/* Geometric Elements */}
      <div style={{
        position: 'absolute',
        left: '10%',
        top: '15%',
        width: '60px',
        height: '60px',
        background: 'linear-gradient(135deg, #8B5CF640, #8B5CF620)',
        borderRadius: '50%',
        border: '1px solid #8B5CF630',
        opacity: 0.6,
        animationName: 'floatGeometric',
        animationDuration: '6s',
        animationDelay: '0s',
        animationIterationCount: 'infinite',
        animationTimingFunction: 'ease-in-out',
        backdropFilter: 'blur(10px)'
      }} />
      
      <div style={{
        position: 'absolute',
        right: '15%',
        top: '25%',
        width: '40px',
        height: '40px',
        background: 'linear-gradient(135deg, #06B6D440, #06B6D420)',
        borderRadius: '8px',
        border: '1px solid #06B6D430',
        opacity: 0.6,
        animationName: 'floatGeometric',
        animationDuration: '8s',
        animationDelay: '2s',
        animationIterationCount: 'infinite',
        animationTimingFunction: 'ease-in-out',
        backdropFilter: 'blur(10px)'
      }} />
      
      <div style={{
        position: 'absolute',
        left: '20%',
        bottom: '20%',
        width: '80px',
        height: '80px',
        background: 'linear-gradient(135deg, #10B98140, #10B98120)',
        borderRadius: '50%',
        border: '1px solid #10B98130',
        opacity: 0.6,
        animationName: 'floatGeometric',
        animationDuration: '10s',
        animationDelay: '4s',
        animationIterationCount: 'infinite',
        animationTimingFunction: 'ease-in-out',
        backdropFilter: 'blur(10px)'
      }} />

      {/* Enhanced Background Orbs */}
      <div style={{
        position: 'absolute',
        top: '10%',
        left: '5%',
        width: '300px',
        height: '300px',
        background: 'radial-gradient(circle, rgba(139, 92, 246, 0.15) 0%, transparent 70%)',
        borderRadius: '50%',
        filter: 'blur(80px)',
        animationName: 'pulse',
        animationDuration: '4s',
        animationIterationCount: 'infinite'
      }} />
      
      <div style={{
        position: 'absolute',
        top: '60%',
        right: '10%',
        width: '200px',
        height: '200px',
        background: 'radial-gradient(circle, rgba(6, 182, 212, 0.12) 0%, transparent 70%)',
        borderRadius: '50%',
        filter: 'blur(60px)',
        animationName: 'pulse',
        animationDuration: '6s',
        animationIterationCount: 'infinite',
        animationDelay: '2s'
      }} />
      
      <div style={{
        position: 'absolute',
        bottom: '10%',
        left: '30%',
        width: '150px',
        height: '150px',
        background: 'radial-gradient(circle, rgba(16, 185, 129, 0.1) 0%, transparent 70%)',
        borderRadius: '50%',
        filter: 'blur(50px)',
        animationName: 'pulse',
        animationDuration: '5s',
        animationIterationCount: 'infinite',
        animationDelay: '1s'
      }} />
      
      {/* Header */}
      <div style={{
        background: 'linear-gradient(135deg, rgba(255, 255, 255, 0.1) 0%, rgba(255, 255, 255, 0.05) 100%)',
        backdropFilter: 'blur(30px)',
        borderBottom: '1px solid rgba(255, 255, 255, 0.1)',
        position: 'relative',
        zIndex: 10
      }}>
        <div style={{
          maxWidth: '1400px',
          margin: '0 auto',
          padding: '0 20px'
        }}>
          <div style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            height: '80px'
          }}>
            {/* Logo */}
            <div style={{
              display: 'flex',
              alignItems: 'center',
              gap: '16px'
            }}>
              <div style={{
                background: 'linear-gradient(135deg, #8B5CF6, #7C3AED)',
                borderRadius: '12px',
                padding: '12px',
                boxShadow: '0 8px 25px rgba(139, 92, 246, 0.3)'
              }}>
                <svg style={{
                  width: '28px',
                  height: '28px',
                  color: 'white'
                }} fill="currentColor" viewBox="0 0 24 24">
                  <path d="M13 2.05v2.02c4.39.54 7.5 4.53 6.96 8.92-.47 3.96-3.96 7.14-7.92 7.54V22c6.05-.55 10.5-5.75 9.95-11.8-.47-5.37-4.63-9.53-9.99-10.15zM12.5 7v5.5l4.5 2.5-.8 1.4-5.2-2.9V7h1.5zm-6.5 5c0 3.31 2.69 6 6 6s6-2.69 6-6-2.69-6-6-6-6 2.69-6 6z"/>
                </svg>
              </div>
              <div>
                <h1 style={{
                  fontSize: '28px',
                  fontWeight: 700,
                  background: 'linear-gradient(135deg, #8B5CF6, #06B6D4, #10B981)',
                  WebkitBackgroundClip: 'text',
                  backgroundClip: 'text',
                  WebkitTextFillColor: 'transparent',
                  backgroundSize: '200% 200%',
                  animation: 'gradientMove 4s ease-in-out infinite',
                  margin: 0
                }}>TradeMaster</h1>
                <p style={{
                  fontSize: '14px',
                  color: 'rgba(203, 213, 225, 0.8)',
                  margin: 0
                }}>Professional Trading Platform</p>
              </div>
            </div>

            {/* Auth Mode Toggle */}
            <div style={{
              display: 'flex',
              alignItems: 'center',
              gap: '4px',
              background: 'rgba(255, 255, 255, 0.1)',
              backdropFilter: 'blur(10px)',
              borderRadius: '12px',
              padding: '6px',
              border: '1px solid rgba(255, 255, 255, 0.2)'
            }}>
              <button
                onClick={() => setAuthMode('login')}
                style={{
                  padding: '10px 20px',
                  fontSize: '14px',
                  fontWeight: 600,
                  borderRadius: '8px',
                  border: 'none',
                  cursor: 'pointer',
                  transition: 'all 0.3s ease',
                  background: authMode === 'login'
                    ? 'linear-gradient(135deg, rgba(255, 255, 255, 0.2), rgba(255, 255, 255, 0.1))'
                    : 'transparent',
                  color: authMode === 'login' ? 'white' : 'rgba(203, 213, 225, 0.8)',
                  boxShadow: authMode === 'login' ? '0 4px 15px rgba(139, 92, 246, 0.2)' : 'none'
                }}
              >
                Sign In
              </button>
              <button
                onClick={() => setAuthMode('register')}
                style={{
                  padding: '10px 20px',
                  fontSize: '14px',
                  fontWeight: 600,
                  borderRadius: '8px',
                  border: 'none',
                  cursor: 'pointer',
                  transition: 'all 0.3s ease',
                  background: authMode === 'register'
                    ? 'linear-gradient(135deg, rgba(255, 255, 255, 0.2), rgba(255, 255, 255, 0.1))'
                    : 'transparent',
                  color: authMode === 'register' ? 'white' : 'rgba(203, 213, 225, 0.8)',
                  boxShadow: authMode === 'register' ? '0 4px 15px rgba(139, 92, 246, 0.2)' : 'none'
                }}
              >
                Sign Up
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Main Content */}
      <div style={{
        flex: 1,
        display: 'flex',
        position: 'relative',
        zIndex: 2
      }}>
        {/* Left Side - Hero Content */}
        <div style={{
          display: 'none',
          '@media (min-width: 1024px)': {
            display: 'flex',
            flex: 1,
            flexDirection: 'column',
            justifyContent: 'center',
            padding: '0 60px'
          }
        }} className="hidden lg:flex lg:flex-1 lg:flex-col lg:justify-center lg:px-12">
          <div style={{
            maxWidth: '500px',
            animation: 'fadeInUp 1s cubic-bezier(0.16, 1, 0.3, 1)'
          }}>
            <h2 style={{
              fontSize: 'clamp(2.5rem, 5vw, 3.5rem)',
              fontWeight: 800,
              background: 'linear-gradient(135deg, #8B5CF6, #06B6D4, #10B981)',
              WebkitBackgroundClip: 'text',
              backgroundClip: 'text',
              WebkitTextFillColor: 'transparent',
              marginBottom: '24px',
              lineHeight: 1.1
            }}>
              Trade Smarter, Not Harder
            </h2>
            <p style={{
              fontSize: '18px',
              color: 'rgba(203, 213, 225, 0.9)',
              marginBottom: '40px',
              lineHeight: 1.6
            }}>
              Join thousands of traders who use TradeMaster's advanced analytics, 
              real-time market data, and intelligent portfolio management to maximize their returns.
            </p>
            
            {/* Features */}
            <div style={{
              display: 'flex',
              flexDirection: 'column',
              gap: '20px'
            }}>
              {[
                'Real-time market data and analytics',
                'Advanced portfolio management tools',
                'Risk management and alerts',
                'Professional-grade trading interface'
              ].map((feature, index) => (
                <div key={index} style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '16px',
                  animation: `fadeInUp 0.8s cubic-bezier(0.16, 1, 0.3, 1) ${index * 0.1 + 0.3}s both`
                }}>
                  <div style={{
                    background: 'linear-gradient(135deg, #10B981, #059669)',
                    borderRadius: '50%',
                    padding: '6px',
                    boxShadow: '0 4px 15px rgba(16, 185, 129, 0.3)'
                  }}>
                    <svg style={{
                      width: '16px',
                      height: '16px',
                      color: 'white'
                    }} fill="currentColor" viewBox="0 0 20 20">
                      <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                    </svg>
                  </div>
                  <span style={{
                    color: 'rgba(203, 213, 225, 0.9)',
                    fontSize: '16px',
                    fontWeight: 500
                  }}>{feature}</span>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Right Side - Auth Form */}
        <div style={{
          flex: 1,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          padding: '40px 20px',
          minHeight: 'calc(100vh - 80px)'
        }}>
          <div style={{
            width: '100%',
            maxWidth: '450px',
            animation: 'fadeInUp 1.2s cubic-bezier(0.16, 1, 0.3, 1)'
          }}>
            {authMode === 'login' ? (
              <LoginForm onRegisterClick={() => setAuthMode('register')} />
            ) : (
              <RegisterForm onLoginClick={() => setAuthMode('login')} />
            )}
          </div>
        </div>
      </div>

      {/* Footer */}
      <div style={{
        background: 'linear-gradient(135deg, rgba(255, 255, 255, 0.05) 0%, rgba(255, 255, 255, 0.02) 100%)',
        backdropFilter: 'blur(20px)',
        borderTop: '1px solid rgba(255, 255, 255, 0.1)',
        position: 'relative',
        zIndex: 10
      }}>
        <div style={{
          maxWidth: '1400px',
          margin: '0 auto',
          padding: '20px'
        }}>
          <div style={{
            display: 'flex',
            flexDirection: 'column',
            gap: '12px',
            alignItems: 'center',
            justifyContent: 'space-between',
            fontSize: '14px',
            color: 'rgba(203, 213, 225, 0.7)'
          }} className="sm:flex-row sm:gap-0">
            <div style={{ textAlign: 'center' }} className="sm:text-left">
              © 2024 TradeMaster. All rights reserved.
            </div>
            <div style={{
              display: 'flex',
              gap: '24px'
            }}>
              <a href="#" style={{
                color: 'rgba(203, 213, 225, 0.7)',
                textDecoration: 'none',
                transition: 'color 0.3s ease'
              }}
              onMouseEnter={(e) => e.currentTarget.style.color = 'white'}
              onMouseLeave={(e) => e.currentTarget.style.color = 'rgba(203, 213, 225, 0.7)'}
              >Privacy Policy</a>
              <a href="#" style={{
                color: 'rgba(203, 213, 225, 0.7)',
                textDecoration: 'none',
                transition: 'color 0.3s ease'
              }}
              onMouseEnter={(e) => e.currentTarget.style.color = 'white'}
              onMouseLeave={(e) => e.currentTarget.style.color = 'rgba(203, 213, 225, 0.7)'}
              >Terms of Service</a>
              <a href="#" style={{
                color: 'rgba(203, 213, 225, 0.7)',
                textDecoration: 'none',
                transition: 'color 0.3s ease'
              }}
              onMouseEnter={(e) => e.currentTarget.style.color = 'white'}
              onMouseLeave={(e) => e.currentTarget.style.color = 'rgba(203, 213, 225, 0.7)'}
              >Support</a>
            </div>
          </div>
        </div>
      </div>
      
      <style>{`
        @keyframes particleGlow {
          0%, 100% { 
            opacity: 0.3;
            transform: scale(1) rotate(0deg);
            filter: brightness(1);
          }
          50% { 
            opacity: 1;
            transform: scale(1.2) rotate(180deg);
            filter: brightness(1.5);
          }
        }
        
        @keyframes floatGeometric {
          0%, 100% {
            transform: translateY(0px) rotate(0deg);
          }
          25% {
            transform: translateY(-20px) rotate(90deg);
          }
          50% {
            transform: translateY(-10px) rotate(180deg);
          }
          75% {
            transform: translateY(-30px) rotate(270deg);
          }
        }
        
        @keyframes pulse {
          0%, 100% {
            transform: scale(1);
            opacity: 0.8;
          }
          50% {
            transform: scale(1.1);
            opacity: 0.4;
          }
        }
        
        @keyframes fadeInUp {
          from {
            opacity: 0;
            transform: translateY(30px);
          }
          to {
            opacity: 1;
            transform: translateY(0);
          }
        }
        
        @keyframes gradientMove {
          0%, 100% { background-position: 0% 50%; }
          50% { background-position: 100% 50%; }
        }
        
        @media (max-width: 1023px) {
          .lg\\:flex { display: flex !important; }
          .lg\\:flex-1 { flex: 1 !important; }
          .lg\\:flex-col { flex-direction: column !important; }
          .lg\\:justify-center { justify-content: center !important; }
          .lg\\:px-12 { padding-left: 3rem !important; padding-right: 3rem !important; }
        }
      `}</style>
    </div>
  );
};

export default AuthPage;