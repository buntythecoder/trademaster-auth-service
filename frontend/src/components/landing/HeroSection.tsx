import React from 'react'
import { ArrowRight, TrendingUp, Shield, Zap, BarChart3, Activity, Brain, Target, Users } from 'lucide-react'
import { useNavigate } from 'react-router-dom'

const GeometricElement = ({ size, color, delay, duration, left, top, shape = 'circle' }) => (
  <div
    style={{
      position: 'absolute',
      left,
      top,
      width: `${size}px`,
      height: `${size}px`,
      background: `linear-gradient(135deg, ${color}40, ${color}20)`,
      borderRadius: shape === 'circle' ? '50%' : '8px',
      border: `1px solid ${color}30`,
      opacity: 0.6,
      animationName: 'floatGeometric',
      animationDuration: `${duration}s`,
      animationDelay: `${delay}s`,
      animationIterationCount: 'infinite',
      animationTimingFunction: 'ease-in-out',
      backdropFilter: 'blur(10px)'
    }}
  />
)

const ParticleSystem = () => {
  const particles = Array.from({ length: 30 }, (_, i) => (
    <div
      key={i}
      style={{
        position: 'absolute',
        width: '3px',
        height: '3px',
        background: `linear-gradient(45deg, #8B5CF6, #06B6D4, #10B981)`,
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
  ))

  return (
    <div style={{
      position: 'absolute',
      top: 0,
      left: 0,
      width: '100%',
      height: '100%',
      pointerEvents: 'none',
      zIndex: 1,
      overflow: 'hidden'
    }}>
      {particles}
      <GeometricElement size={120} color="#8B5CF6" delay={0} duration={8} left="10%" top="20%" />
      <GeometricElement size={80} color="#06B6D4" delay={2} duration={12} left="85%" top="15%" shape="square" />
      <GeometricElement size={100} color="#10B981" delay={4} duration={10} left="15%" top="70%" />
      <GeometricElement size={60} color="#F59E0B" delay={1} duration={15} left="80%" top="75%" shape="square" />
    </div>
  )
}

const GlowingOrb = ({ size, color, left, top, intensity = 0.3 }) => (
  <div
    style={{
      position: 'absolute',
      left,
      top,
      width: `${size}px`,
      height: `${size}px`,
      background: `radial-gradient(circle, ${color}${Math.floor(intensity * 100).toString(16).padStart(2, '0')} 0%, transparent 70%)`,
      borderRadius: '50%',
      filter: 'blur(40px)',
      animationName: 'pulseGlow',
      animationDuration: '4s',
      animationIterationCount: 'infinite',
      animationTimingFunction: 'ease-in-out'
    }}
  />
)

export function HeroSection() {
  const navigate = useNavigate()
  
  const features = [
    {
      icon: <Brain style={{ width: '28px', height: '28px' }} />,
      title: "AI Analytics",
      description: "Advanced machine learning algorithms analyze market patterns and predict optimal trading opportunities in real-time",
      color: "#8B5CF6",
      gradient: "linear-gradient(135deg, #8B5CF6, #7C3AED)"
    },
    {
      icon: <Activity style={{ width: '28px', height: '28px' }} />,
      title: "Live Trading",
      description: "Execute trades instantly with zero-latency connections to major Indian exchanges including NSE and BSE",
      color: "#06B6D4",
      gradient: "linear-gradient(135deg, #06B6D4, #0891B2)"
    },
    {
      icon: <Target style={{ width: '28px', height: '28px' }} />,
      title: "Smart Strategies",
      description: "Automated trading strategies with customizable risk parameters and profit targets for consistent returns",
      color: "#10B981",
      gradient: "linear-gradient(135deg, #10B981, #059669)"
    },
    {
      icon: <Shield style={{ width: '28px', height: '28px' }} />,
      title: "Risk Shield",
      description: "Multi-layer risk management system with stop-loss automation and position sizing algorithms",
      color: "#F59E0B",
      gradient: "linear-gradient(135deg, #F59E0B, #D97706)"
    }
  ]

  return (
    <div style={{
      position: 'relative',
      minHeight: '100vh',
      background: 'radial-gradient(ellipse at top, #0f0f23 0%, #1a0b2e 25%, #0a0a0a 100%)',
      overflow: 'hidden'
    }}>
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
            transform: translateY(-40px) rotate(180deg);
          }
          75% {
            transform: translateY(-20px) rotate(270deg);
          }
        }
        
        @keyframes pulseGlow {
          0%, 100% {
            opacity: 0.3;
            transform: scale(1);
          }
          50% {
            opacity: 0.8;
            transform: scale(1.1);
          }
        }
        
        @keyframes gradientMove {
          0%, 100% { background-position: 0% 50%; }
          50% { background-position: 100% 50%; }
        }
        
        @keyframes fadeInUp {
          from { 
            opacity: 0; 
            transform: translateY(40px);
          }
          to { 
            opacity: 1; 
            transform: translateY(0);
          }
        }
        
        @keyframes scaleIn {
          from {
            opacity: 0;
            transform: scale(0.9);
          }
          to {
            opacity: 1;
            transform: scale(1);
          }
        }
      `}</style>
      
      <ParticleSystem />
      
      {/* Glowing Orbs */}
      <GlowingOrb size={200} color="#8B5CF6" left="5%" top="10%" intensity={0.15} />
      <GlowingOrb size={250} color="#06B6D4" left="85%" top="20%" intensity={0.12} />
      <GlowingOrb size={180} color="#10B981" left="10%" top="75%" intensity={0.18} />
      <GlowingOrb size={220} color="#F59E0B" left="80%" top="80%" intensity={0.14} />
      
      {/* Main Content */}
      <div style={{
        position: 'relative',
        zIndex: 10,
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        minHeight: '100vh',
        padding: '40px 16px',
        color: 'white'
      }}>
        {/* TradeMaster Logo and Brand */}
        <div style={{
          marginBottom: '48px',
          animationName: 'scaleIn',
          animationDuration: '0.8s',
          animationTimingFunction: 'cubic-bezier(0.16, 1, 0.3, 1)',
          textAlign: 'center'
        }}>
          <div style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            width: '100px',
            height: '100px',
            marginBottom: '32px',
            margin: '0 auto 32px auto',
            background: 'linear-gradient(135deg, rgba(139, 92, 246, 0.2), rgba(6, 182, 212, 0.1))',
            backdropFilter: 'blur(30px)',
            border: '2px solid rgba(139, 92, 246, 0.3)',
            boxShadow: '0 20px 60px rgba(139, 92, 246, 0.3), inset 0 1px 0 rgba(255, 255, 255, 0.1)',
            borderRadius: '24px',
            position: 'relative',
            overflow: 'hidden'
          }}>
            <div style={{
              position: 'absolute',
              inset: 0,
              background: 'linear-gradient(135deg, transparent 30%, rgba(139, 92, 246, 0.1) 50%, transparent 70%)',
              animationName: 'gradientMove',
              animationDuration: '3s',
              animationIterationCount: 'infinite'
            }} />
            <TrendingUp style={{ 
              width: '50px', 
              height: '50px', 
              color: '#8B5CF6',
              filter: 'drop-shadow(0 0 10px rgba(139, 92, 246, 0.5))',
              position: 'relative',
              zIndex: 1
            }} />
          </div>
          <div style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            gap: '12px',
            background: 'linear-gradient(135deg, #8B5CF6, #06B6D4, #10B981)',
            WebkitBackgroundClip: 'text',
            backgroundClip: 'text',
            WebkitTextFillColor: 'transparent',
            backgroundSize: '200% 200%',
            animationName: 'gradientMove',
            animationDuration: '4s',
            animationIterationCount: 'infinite',
            fontSize: '16px',
            fontWeight: 600,
            textTransform: 'uppercase',
            letterSpacing: '0.15em'
          }}>
            <Brain style={{ width: '20px', height: '20px', color: '#8B5CF6' }} />
            <span>Next-Gen AI Trading</span>
            <Activity style={{ width: '20px', height: '20px', color: '#06B6D4' }} />
          </div>
        </div>

        {/* Main Heading */}
        <div style={{
          textAlign: 'center',
          maxWidth: '1200px',
          margin: '0 auto 48px auto',
          animationName: 'fadeInUp',
          animationDuration: '1s',
          animationTimingFunction: 'cubic-bezier(0.16, 1, 0.3, 1)',
          animationDelay: '0.3s',
          animationFillMode: 'both'
        }}>
          <h1 style={{
            fontSize: 'clamp(2.5rem, 8vw, 5.5rem)',
            fontWeight: 800,
            marginBottom: '32px',
            lineHeight: 1.1,
            letterSpacing: '-0.02em'
          }}>
            <span style={{
              background: 'linear-gradient(135deg, #8B5CF6 0%, #06B6D4 35%, #10B981 70%, #F59E0B 100%)',
              WebkitBackgroundClip: 'text',
              backgroundClip: 'text',
              WebkitTextFillColor: 'transparent',
              backgroundSize: '300% 300%',
              animationName: 'gradientMove',
              animationDuration: '5s',
              animationTimingFunction: 'ease-in-out',
              animationIterationCount: 'infinite',
              display: 'inline-block',
              textShadow: '0 0 30px rgba(139, 92, 246, 0.3)'
            }}>TradeMaster</span>
            <br />
            <span style={{ 
              color: 'white',
              textShadow: '0 0 20px rgba(255, 255, 255, 0.1)'
            }}>Pro Elite</span>
          </h1>
          
          <div style={{
            fontSize: 'clamp(1.1rem, 3vw, 1.4rem)',
            color: 'rgba(203, 213, 225, 0.9)',
            marginBottom: '40px',
            maxWidth: '700px',
            margin: '0 auto 40px auto',
            lineHeight: 1.6,
            fontWeight: 400
          }}>
            <p style={{ marginBottom: '16px' }}>
              <span style={{ color: '#8B5CF6', fontWeight: 600 }}>India's Most Advanced</span> AI-powered trading platform
            </p>
            <p style={{ 
              fontSize: 'clamp(0.95rem, 2.2vw, 1.1rem)',
              color: 'rgba(148, 163, 184, 0.8)'
            }}>
              Institutional-grade algorithms • Real-time market analysis • Automated risk management
            </p>
          </div>
        </div>

        {/* Feature Cards */}
        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))',
          gap: '24px',
          marginBottom: '64px',
          maxWidth: '1200px',
          width: '100%',
          animationName: 'fadeInUp',
          animationDuration: '1s',
          animationTimingFunction: 'cubic-bezier(0.16, 1, 0.3, 1)',
          animationDelay: '0.6s',
          animationFillMode: 'both'
        }}>
          {features.map((feature, index) => (
            <div 
              key={index}
              style={{
                background: 'linear-gradient(135deg, rgba(255, 255, 255, 0.1) 0%, rgba(255, 255, 255, 0.05) 100%)',
                backdropFilter: 'blur(30px)',
                border: `2px solid rgba(${feature.color.slice(1).match(/.{2}/g).map(hex => parseInt(hex, 16)).join(', ')}, 0.2)`,
                boxShadow: `0 20px 60px rgba(${feature.color.slice(1).match(/.{2}/g).map(hex => parseInt(hex, 16)).join(', ')}, 0.15), inset 0 1px 0 rgba(255, 255, 255, 0.1)`,
                padding: '32px 24px',
                borderRadius: '20px',
                textAlign: 'left',
                transition: 'all 0.4s cubic-bezier(0.16, 1, 0.3, 1)',
                cursor: 'pointer',
                position: 'relative',
                overflow: 'hidden'
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.transform = 'translateY(-12px) scale(1.02)'
                e.currentTarget.style.background = `linear-gradient(135deg, rgba(${feature.color.slice(1).match(/.{2}/g).map(hex => parseInt(hex, 16)).join(', ')}, 0.1), rgba(255, 255, 255, 0.05))`
                e.currentTarget.style.borderColor = `rgba(${feature.color.slice(1).match(/.{2}/g).map(hex => parseInt(hex, 16)).join(', ')}, 0.4)`
                e.currentTarget.style.boxShadow = `0 30px 80px rgba(${feature.color.slice(1).match(/.{2}/g).map(hex => parseInt(hex, 16)).join(', ')}, 0.3), inset 0 1px 0 rgba(255, 255, 255, 0.2)`
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.transform = 'translateY(0) scale(1)'
                e.currentTarget.style.background = 'linear-gradient(135deg, rgba(255, 255, 255, 0.1) 0%, rgba(255, 255, 255, 0.05) 100%)'
                e.currentTarget.style.borderColor = `rgba(${feature.color.slice(1).match(/.{2}/g).map(hex => parseInt(hex, 16)).join(', ')}, 0.2)`
                e.currentTarget.style.boxShadow = `0 20px 60px rgba(${feature.color.slice(1).match(/.{2}/g).map(hex => parseInt(hex, 16)).join(', ')}, 0.15), inset 0 1px 0 rgba(255, 255, 255, 0.1)`
              }}
            >
              {/* Background Gradient Overlay */}
              <div style={{
                position: 'absolute',
                top: 0,
                right: 0,
                width: '100px',
                height: '100px',
                background: feature.gradient,
                opacity: 0.1,
                borderRadius: '50%',
                transform: 'translate(30px, -30px)',
                filter: 'blur(20px)'
              }} />
              
              <div style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                width: '64px',
                height: '64px',
                margin: '0 0 20px 0',
                background: feature.gradient,
                borderRadius: '16px',
                color: 'white',
                boxShadow: `0 10px 30px rgba(${feature.color.slice(1).match(/.{2}/g).map(hex => parseInt(hex, 16)).join(', ')}, 0.3)`,
                position: 'relative',
                zIndex: 2
              }}>
                {feature.icon}
              </div>
              <h3 style={{
                color: 'white',
                fontSize: '18px',
                fontWeight: 700,
                marginBottom: '12px',
                position: 'relative',
                zIndex: 2
              }}>{feature.title}</h3>
              <p style={{
                color: 'rgba(203, 213, 225, 0.8)',
                fontSize: '14px',
                lineHeight: 1.6,
                position: 'relative',
                zIndex: 2
              }}>{feature.description}</p>
            </div>
          ))}
        </div>

        {/* CTA Buttons */}
        <div style={{
          display: 'flex',
          flexDirection: window.innerWidth < 640 ? 'column' : 'row',
          gap: '20px',
          marginBottom: '64px',
          animationName: 'scaleIn',
          animationDuration: '1s',
          animationTimingFunction: 'cubic-bezier(0.16, 1, 0.3, 1)',
          animationDelay: '0.9s',
          animationFillMode: 'both'
        }}>
          <button 
            onClick={() => navigate('/auth')}
            style={{
              position: 'relative',
              background: 'linear-gradient(135deg, #8B5CF6 0%, #06B6D4 50%, #10B981 100%)',
              border: 'none',
              color: 'white',
              overflow: 'hidden',
              transition: 'all 0.4s cubic-bezier(0.16, 1, 0.3, 1)',
              padding: '18px 40px',
              borderRadius: '50px',
              fontWeight: 700,
              fontSize: '18px',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '12px',
              cursor: 'pointer',
              boxShadow: '0 20px 40px rgba(139, 92, 246, 0.3)',
              backgroundSize: '200% 200%',
              animationName: 'gradientMove',
              animationDuration: '3s',
              animationIterationCount: 'infinite',
              textTransform: 'uppercase',
              letterSpacing: '0.05em'
            }}
            onMouseEnter={(e) => {
              e.currentTarget.style.transform = 'translateY(-4px) scale(1.05)'
              e.currentTarget.style.boxShadow = '0 30px 60px rgba(139, 92, 246, 0.4)'
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.transform = 'translateY(0) scale(1)'
              e.currentTarget.style.boxShadow = '0 20px 40px rgba(139, 92, 246, 0.3)'
            }}
          >
            <span>Start Trading Now</span>
            <ArrowRight style={{ width: '22px', height: '22px' }} />
          </button>
          
          <button 
            onClick={() => navigate('/auth')}
            style={{
              background: 'linear-gradient(135deg, rgba(255, 255, 255, 0.1), rgba(255, 255, 255, 0.05))',
              backdropFilter: 'blur(30px)',
              border: '2px solid rgba(255, 255, 255, 0.2)',
              boxShadow: '0 20px 40px rgba(0, 0, 0, 0.1), inset 0 1px 0 rgba(255, 255, 255, 0.1)',
              padding: '18px 40px',
              borderRadius: '50px',
              fontWeight: 600,
              fontSize: '18px',
              color: 'white',
              transition: 'all 0.4s cubic-bezier(0.16, 1, 0.3, 1)',
              cursor: 'pointer'
            }}
            onMouseEnter={(e) => {
              e.currentTarget.style.transform = 'translateY(-4px)'
              e.currentTarget.style.background = 'linear-gradient(135deg, rgba(139, 92, 246, 0.2), rgba(255, 255, 255, 0.1))'
              e.currentTarget.style.borderColor = 'rgba(139, 92, 246, 0.4)'
              e.currentTarget.style.boxShadow = '0 30px 60px rgba(139, 92, 246, 0.2), inset 0 1px 0 rgba(255, 255, 255, 0.2)'
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.transform = 'translateY(0)'
              e.currentTarget.style.background = 'linear-gradient(135deg, rgba(255, 255, 255, 0.1), rgba(255, 255, 255, 0.05))'
              e.currentTarget.style.borderColor = 'rgba(255, 255, 255, 0.2)'
              e.currentTarget.style.boxShadow = '0 20px 40px rgba(0, 0, 0, 0.1), inset 0 1px 0 rgba(255, 255, 255, 0.1)'
            }}
          >
            Sign In
          </button>
        </div>

        {/* Bottom Stats */}
        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
          gap: '32px',
          maxWidth: '800px',
          width: '100%',
          textAlign: 'center',
          animationName: 'fadeInUp',
          animationDuration: '1s',
          animationTimingFunction: 'cubic-bezier(0.16, 1, 0.3, 1)',
          animationDelay: '1.2s',
          animationFillMode: 'both'
        }}>
          <div style={{
            background: 'linear-gradient(135deg, rgba(139, 92, 246, 0.1), rgba(139, 92, 246, 0.05))',
            backdropFilter: 'blur(20px)',
            border: '1px solid rgba(139, 92, 246, 0.2)',
            borderRadius: '16px',
            padding: '24px 16px',
            boxShadow: '0 10px 30px rgba(139, 92, 246, 0.1)'
          }}>
            <div style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              marginBottom: '8px',
              gap: '8px'
            }}>
              <Users style={{ width: '24px', height: '24px', color: '#8B5CF6' }} />
              <div style={{
                fontSize: '32px',
                fontWeight: 'bold',
                background: 'linear-gradient(135deg, #8B5CF6, #06B6D4)',
                WebkitBackgroundClip: 'text',
                backgroundClip: 'text',
                WebkitTextFillColor: 'transparent'
              }}>25K+</div>
            </div>
            <div style={{ color: 'rgba(203, 213, 225, 0.8)', fontSize: '14px', fontWeight: 500 }}>Active Traders</div>
          </div>
          <div style={{
            background: 'linear-gradient(135deg, rgba(6, 182, 212, 0.1), rgba(6, 182, 212, 0.05))',
            backdropFilter: 'blur(20px)',
            border: '1px solid rgba(6, 182, 212, 0.2)',
            borderRadius: '16px',
            padding: '24px 16px',
            boxShadow: '0 10px 30px rgba(6, 182, 212, 0.1)'
          }}>
            <div style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              marginBottom: '8px',
              gap: '8px'
            }}>
              <BarChart3 style={{ width: '24px', height: '24px', color: '#06B6D4' }} />
              <div style={{
                fontSize: '32px',
                fontWeight: 'bold',
                background: 'linear-gradient(135deg, #06B6D4, #10B981)',
                WebkitBackgroundClip: 'text',
                backgroundClip: 'text',
                WebkitTextFillColor: 'transparent'
              }}>₹50Cr+</div>
            </div>
            <div style={{ color: 'rgba(203, 213, 225, 0.8)', fontSize: '14px', fontWeight: 500 }}>Monthly Volume</div>
          </div>
          <div style={{
            background: 'linear-gradient(135deg, rgba(16, 185, 129, 0.1), rgba(16, 185, 129, 0.05))',
            backdropFilter: 'blur(20px)',
            border: '1px solid rgba(16, 185, 129, 0.2)',
            borderRadius: '16px',
            padding: '24px 16px',
            boxShadow: '0 10px 30px rgba(16, 185, 129, 0.1)'
          }}>
            <div style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              marginBottom: '8px',
              gap: '8px'
            }}>
              <Shield style={{ width: '24px', height: '24px', color: '#10B981' }} />
              <div style={{
                fontSize: '32px',
                fontWeight: 'bold',
                background: 'linear-gradient(135deg, #10B981, #F59E0B)',
                WebkitBackgroundClip: 'text',
                backgroundClip: 'text',
                WebkitTextFillColor: 'transparent'
              }}>99.9%</div>
            </div>
            <div style={{ color: 'rgba(203, 213, 225, 0.8)', fontSize: '14px', fontWeight: 500 }}>Uptime SLA</div>
          </div>
        </div>
      </div>

      {/* Enhanced Decorative Elements */}
      <div style={{
        position: 'absolute',
        top: '10%',
        left: '5%',
        width: '300px',
        height: '300px',
        background: 'radial-gradient(circle, rgba(139, 92, 246, 0.15) 0%, transparent 60%)',
        borderRadius: '50%',
        filter: 'blur(100px)',
        animationName: 'pulseGlow',
        animationDuration: '6s',
        animationIterationCount: 'infinite'
      }} />
      <div style={{
        position: 'absolute',
        top: '20%',
        right: '5%',
        width: '250px',
        height: '250px',
        background: 'radial-gradient(circle, rgba(6, 182, 212, 0.12) 0%, transparent 60%)',
        borderRadius: '50%',
        filter: 'blur(80px)',
        animationName: 'pulseGlow',
        animationDuration: '8s',
        animationIterationCount: 'infinite',
        animationDelay: '2s'
      }} />
      <div style={{
        position: 'absolute',
        bottom: '15%',
        left: '10%',
        width: '200px',
        height: '200px',
        background: 'radial-gradient(circle, rgba(16, 185, 129, 0.1) 0%, transparent 60%)',
        borderRadius: '50%',
        filter: 'blur(70px)',
        animationName: 'pulseGlow',
        animationDuration: '7s',
        animationIterationCount: 'infinite',
        animationDelay: '4s'
      }} />
      <div style={{
        position: 'absolute',
        bottom: '10%',
        right: '8%',
        width: '180px',
        height: '180px',
        background: 'radial-gradient(circle, rgba(245, 158, 11, 0.08) 0%, transparent 60%)',
        borderRadius: '50%',
        filter: 'blur(60px)',
        animationName: 'pulseGlow',
        animationDuration: '9s',
        animationIterationCount: 'infinite',
        animationDelay: '1s'
      }} />
    </div>
  )
}