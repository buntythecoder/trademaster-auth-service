import React from 'react'
import { ArrowRight, TrendingUp, Shield, Zap, BarChart3 } from 'lucide-react'
import { useNavigate } from 'react-router-dom'

const ParticleSystem = () => {
  const particles = Array.from({ length: 50 }, (_, i) => (
    <div
      key={i}
      className="particle"
      style={{
        left: `${Math.random() * 100}%`,
        animationDelay: `${Math.random() * 15}s`,
        animationDuration: `${15 + Math.random() * 10}s`
      }}
    />
  ))

  return <div className="particles">{particles}</div>
}

const FloatingShapes = () => {
  const shapes = Array.from({ length: 8 }, (_, i) => (
    <div
      key={i}
      className="geometric-shape"
      style={{
        left: `${Math.random() * 100}%`,
        animationDelay: `${Math.random() * 20}s`,
        width: `${20 + Math.random() * 40}px`,
        height: `${20 + Math.random() * 40}px`,
        background: `linear-gradient(135deg, rgba(102, 126, 234, ${0.1 + Math.random() * 0.2}), rgba(118, 75, 162, ${0.1 + Math.random() * 0.2}))`,
        borderRadius: Math.random() > 0.5 ? '50%' : '8px',
        border: '1px solid rgba(139, 92, 246, 0.2)'
      }}
    />
  ))

  return <div className="floating-shapes">{shapes}</div>
}

export function HeroSection() {
  const navigate = useNavigate()
  
  const features = [
    {
      icon: <BarChart3 className="w-6 h-6" />,
      title: "Smart Analytics",
      description: "AI-powered trading insights and performance metrics"
    },
    {
      icon: <Zap className="w-6 h-6" />,
      title: "Real-Time Trading",
      description: "Lightning-fast execution with live market data"
    },
    {
      icon: <TrendingUp className="w-6 h-6" />,
      title: "Portfolio Management",
      description: "Advanced tools to manage your trading portfolio"
    },
    {
      icon: <Shield className="w-6 h-6" />,
      title: "Risk Management",
      description: "Automated risk controls and compliance monitoring"
    }
  ]

  return (
    <div className="relative min-h-screen hero-gradient overflow-hidden">
      <ParticleSystem />
      <FloatingShapes />
      
      {/* Main Content */}
      <div className="relative z-10 flex flex-col items-center justify-center min-h-screen px-4 py-20">
        {/* TradeMaster Logo and Brand */}
        <div className="mb-8 animate-fade-in">
          <div className="flex items-center justify-center w-20 h-20 mb-6 mx-auto glass-card rounded-2xl">
            <TrendingUp className="w-10 h-10 text-purple-400" />
          </div>
          <div className="flex items-center justify-center space-x-2 text-purple-400 text-sm font-medium uppercase tracking-wider">
            <TrendingUp className="w-4 h-4" />
            <span>AI-Powered Trading Platform</span>
          </div>
        </div>

        {/* Main Heading */}
        <div className="text-center max-w-6xl mx-auto mb-8 animate-fade-up animate-delay-200">
          <h1 className="text-5xl md:text-7xl lg:text-8xl font-bold mb-6 leading-tight">
            <span className="gradient-text">Trade</span>
            <br />
            <span className="text-white">Master </span>
            <span className="gradient-text">Pro</span>
          </h1>
          
          <p className="text-xl md:text-2xl text-slate-300 mb-12 max-w-3xl mx-auto leading-relaxed">
            Advanced AI trading platform for Indian retail traders
            <br />
            with intelligent analytics and automated strategies
          </p>
        </div>

        {/* Feature Cards */}
        <div className="grid grid-cols-2 md:grid-cols-4 gap-6 mb-12 max-w-4xl mx-auto animate-fade-up animate-delay-400">
          {features.map((feature, index) => (
            <div 
              key={index}
              className="glass-card p-6 rounded-2xl text-center hover:scale-105 transition-all duration-300 group"
              style={{ animationDelay: `${0.1 * index}s` }}
            >
              <div className="flex items-center justify-center w-12 h-12 mx-auto mb-4 text-purple-400 group-hover:text-cyan-400 transition-colors">
                {feature.icon}
              </div>
              <h3 className="text-purple-400 text-sm font-semibold mb-2">{feature.title}</h3>
              <p className="text-slate-400 text-xs leading-relaxed">{feature.description}</p>
            </div>
          ))}
        </div>

        {/* CTA Buttons */}
        <div className="flex flex-col sm:flex-row gap-4 animate-fade-up animate-delay-600">
          <button 
            onClick={() => navigate('/register')}
            className="cyber-button px-8 py-4 rounded-2xl font-semibold text-lg flex items-center justify-center space-x-2 group"
          >
            <span>Start Trading</span>
            <ArrowRight className="w-5 h-5 group-hover:translate-x-1 transition-transform" />
          </button>
          
          <button 
            onClick={() => navigate('/login')}
            className="glass-card px-8 py-4 rounded-2xl font-semibold text-lg text-white hover:text-purple-300 transition-colors border-2 border-transparent hover:border-purple-500/50"
          >
            Sign In
          </button>
        </div>

        {/* Bottom Stats */}
        <div className="mt-20 grid grid-cols-1 md:grid-cols-3 gap-8 max-w-2xl mx-auto text-center animate-fade-up animate-delay-800">
          <div>
            <div className="text-2xl font-bold gradient-text mb-2">25K+</div>
            <div className="text-slate-400 text-sm">Active Traders</div>
          </div>
          <div>
            <div className="text-2xl font-bold gradient-text mb-2">₹50Cr+</div>
            <div className="text-slate-400 text-sm">Trading Volume</div>
          </div>
          <div>
            <div className="text-2xl font-bold gradient-text mb-2">99.9%</div>
            <div className="text-slate-400 text-sm">Uptime SLA</div>
          </div>
        </div>
      </div>

      {/* Decorative Elements */}
      <div className="absolute top-20 left-10 w-32 h-32 bg-gradient-to-br from-purple-500/10 to-pink-500/10 rounded-full blur-3xl animate-pulse" />
      <div className="absolute bottom-20 right-10 w-40 h-40 bg-gradient-to-br from-blue-500/10 to-purple-500/10 rounded-full blur-3xl animate-pulse" style={{ animationDelay: '2s' }} />
      <div className="absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 w-96 h-96 bg-gradient-to-br from-purple-500/5 to-pink-500/5 rounded-full blur-3xl" />
    </div>
  )
}