import React from 'react'
import { Search, Filter, TrendingUp, Users, Star } from 'lucide-react'
import { TraderProfile } from '../components/trader/TraderProfile'

const TradersPage: React.FC = () => {
  const topTraders = [
    {
      name: 'Rajesh Kumar',
      username: 'raj_trader_pro',
      stats: {
        totalPnL: 2500000,
        winRate: 87,
        totalTrades: 1543,
        avgReturn: 15.2,
        maxDrawdown: -8.5,
        sharpeRatio: 2.3,
        followers: 15600,
        rank: 1
      },
      badges: ['Top Trader', 'Strategy King'],
      isVerified: true,
      isOnline: true
    },
    {
      name: 'Priya Sharma',
      username: 'priya_quant',
      stats: {
        totalPnL: 1850000,
        winRate: 82,
        totalTrades: 987,
        avgReturn: 12.8,
        maxDrawdown: -6.2,
        sharpeRatio: 2.1,
        followers: 12400,
        rank: 2
      },
      badges: ['Risk Master', 'Market Leader'],
      isVerified: true,
      isOnline: false
    },
    {
      name: 'Amit Patel',
      username: 'amit_algo',
      stats: {
        totalPnL: 1650000,
        winRate: 79,
        totalTrades: 2103,
        avgReturn: 11.4,
        maxDrawdown: -9.1,
        sharpeRatio: 1.9,
        followers: 9800,
        rank: 3
      },
      badges: ['Strategy King'],
      isVerified: true,
      isOnline: true
    },
    {
      name: 'Sneha Reddy',
      username: 'sneha_scalper',
      stats: {
        totalPnL: 1420000,
        winRate: 75,
        totalTrades: 3201,
        avgReturn: 9.8,
        maxDrawdown: -7.3,
        sharpeRatio: 1.7,
        followers: 7200,
        rank: 4
      },
      badges: ['Risk Master'],
      isVerified: false,
      isOnline: true
    },
    {
      name: 'Vikash Singh',
      username: 'vikash_momentum',
      stats: {
        totalPnL: 1280000,
        winRate: 73,
        totalTrades: 1876,
        avgReturn: 8.9,
        maxDrawdown: -11.2,
        sharpeRatio: 1.5,
        followers: 6100,
        rank: 5
      },
      badges: ['Market Leader'],
      isVerified: false,
      isOnline: false
    },
    {
      name: 'Kavitha Nair',
      username: 'kavitha_swing',
      stats: {
        totalPnL: 1150000,
        winRate: 71,
        totalTrades: 1234,
        avgReturn: 7.6,
        maxDrawdown: -8.9,
        sharpeRatio: 1.4,
        followers: 5400,
        rank: 6
      },
      badges: ['Top Trader'],
      isVerified: true,
      isOnline: true
    }
  ]

  return (
    <div style={{
      minHeight: '100vh',
      background: 'radial-gradient(ellipse at top, #0f0f23 0%, #1a0b2e 25%, #0a0a0a 100%)',
      padding: '40px 20px',
      position: 'relative'
    }}>
      {/* Background Elements */}
      <div style={{
        position: 'absolute',
        top: '10%',
        left: '5%',
        width: '200px',
        height: '200px',
        background: 'radial-gradient(circle, rgba(139, 92, 246, 0.1) 0%, transparent 70%)',
        borderRadius: '50%',
        filter: 'blur(60px)'
      }} />
      <div style={{
        position: 'absolute',
        top: '20%',
        right: '5%',
        width: '150px',
        height: '150px',
        background: 'radial-gradient(circle, rgba(6, 182, 212, 0.08) 0%, transparent 70%)',
        borderRadius: '50%',
        filter: 'blur(50px)'
      }} />

      <div style={{
        maxWidth: '1400px',
        margin: '0 auto',
        position: 'relative',
        zIndex: 2
      }}>
        {/* Header */}
        <div style={{
          textAlign: 'center',
          marginBottom: '48px'
        }}>
          <h1 style={{
            fontSize: 'clamp(2rem, 5vw, 3.5rem)',
            fontWeight: 800,
            marginBottom: '16px',
            background: 'linear-gradient(135deg, #8B5CF6, #06B6D4, #10B981)',
            WebkitBackgroundClip: 'text',
            backgroundClip: 'text',
            WebkitTextFillColor: 'transparent',
            backgroundSize: '200% 200%',
            animation: 'gradientMove 4s ease-in-out infinite'
          }}>
            Top Traders
          </h1>
          <p style={{
            fontSize: '18px',
            color: 'rgba(203, 213, 225, 0.8)',
            marginBottom: '32px',
            maxWidth: '600px',
            margin: '0 auto 32px auto'
          }}>
            Follow India's most successful traders and learn from their strategies
          </p>

          {/* Stats Bar */}
          <div style={{
            display: 'flex',
            justifyContent: 'center',
            gap: '40px',
            flexWrap: 'wrap',
            marginBottom: '32px'
          }}>
            <div style={{
              display: 'flex',
              alignItems: 'center',
              gap: '8px'
            }}>
              <Users style={{ width: '20px', height: '20px', color: '#8B5CF6' }} />
              <span style={{ color: 'rgba(203, 213, 225, 0.8)', fontSize: '16px' }}>
                25,000+ Active Traders
              </span>
            </div>
            <div style={{
              display: 'flex',
              alignItems: 'center',
              gap: '8px'
            }}>
              <TrendingUp style={{ width: '20px', height: '20px', color: '#10B981' }} />
              <span style={{ color: 'rgba(203, 213, 225, 0.8)', fontSize: '16px' }}>
                ₹2.5Cr+ Total Profits
              </span>
            </div>
            <div style={{
              display: 'flex',
              alignItems: 'center',
              gap: '8px'
            }}>
              <Star style={{ width: '20px', height: '20px', color: '#F59E0B' }} />
              <span style={{ color: 'rgba(203, 213, 225, 0.8)', fontSize: '16px' }}>
                Elite Performance
              </span>
            </div>
          </div>
        </div>

        {/* Search and Filter Bar */}
        <div style={{
          display: 'flex',
          gap: '16px',
          marginBottom: '48px',
          flexWrap: 'wrap',
          justifyContent: 'center'
        }}>
          <div style={{
            position: 'relative',
            flex: '1',
            maxWidth: '400px'
          }}>
            <Search style={{
              position: 'absolute',
              left: '16px',
              top: '50%',
              transform: 'translateY(-50%)',
              width: '20px',
              height: '20px',
              color: 'rgba(203, 213, 225, 0.5)'
            }} />
            <input
              type="text"
              placeholder="Search traders by name or strategy..."
              style={{
                width: '100%',
                padding: '16px 16px 16px 48px',
                background: 'linear-gradient(135deg, rgba(255, 255, 255, 0.1), rgba(255, 255, 255, 0.05))',
                backdropFilter: 'blur(20px)',
                border: '1px solid rgba(255, 255, 255, 0.2)',
                borderRadius: '16px',
                color: 'white',
                fontSize: '16px',
                outline: 'none',
                transition: 'all 0.3s ease'
              }}
              onFocus={(e) => {
                e.target.style.borderColor = 'rgba(139, 92, 246, 0.5)'
                e.target.style.boxShadow = '0 0 20px rgba(139, 92, 246, 0.2)'
              }}
              onBlur={(e) => {
                e.target.style.borderColor = 'rgba(255, 255, 255, 0.2)'
                e.target.style.boxShadow = 'none'
              }}
            />
          </div>
          
          <button style={{
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
            padding: '16px 24px',
            background: 'linear-gradient(135deg, rgba(255, 255, 255, 0.1), rgba(255, 255, 255, 0.05))',
            backdropFilter: 'blur(20px)',
            border: '1px solid rgba(255, 255, 255, 0.2)',
            borderRadius: '16px',
            color: 'white',
            fontSize: '16px',
            cursor: 'pointer',
            transition: 'all 0.3s ease'
          }}
          onMouseEnter={(e) => {
            e.currentTarget.style.borderColor = 'rgba(139, 92, 246, 0.5)'
            e.currentTarget.style.transform = 'translateY(-2px)'
          }}
          onMouseLeave={(e) => {
            e.currentTarget.style.borderColor = 'rgba(255, 255, 255, 0.2)'
            e.currentTarget.style.transform = 'translateY(0)'
          }}>
            <Filter style={{ width: '20px', height: '20px' }} />
            Filter
          </button>
        </div>

        {/* Traders Grid */}
        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(380px, 1fr))',
          gap: '32px',
          maxWidth: '1200px',
          margin: '0 auto'
        }}>
          {topTraders.map((trader, index) => (
            <div
              key={index}
              style={{
                animation: `fadeInUp 0.8s cubic-bezier(0.16, 1, 0.3, 1) ${index * 0.1}s both`
              }}
            >
              <TraderProfile {...trader} />
            </div>
          ))}
        </div>
      </div>

      <style>{`
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
        
        @keyframes gradientMove {
          0%, 100% { background-position: 0% 50%; }
          50% { background-position: 100% 50%; }
        }
        
        input::placeholder {
          color: rgba(203, 213, 225, 0.5);
        }
      `}</style>
    </div>
  )
}

export default TradersPage