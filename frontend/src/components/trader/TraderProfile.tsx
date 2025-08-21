import React from 'react'
import { TrendingUp, TrendingDown, Star, Award, Shield, Target, Users, Activity } from 'lucide-react'

interface TraderStats {
  totalPnL: number
  winRate: number
  totalTrades: number
  avgReturn: number
  maxDrawdown: number
  sharpeRatio: number
  followers: number
  rank: number
}

interface TraderProfileProps {
  name: string
  username: string
  avatar?: string
  stats: TraderStats
  badges: string[]
  isVerified: boolean
  isOnline: boolean
}

export const TraderProfile: React.FC<TraderProfileProps> = ({
  name,
  username,
  avatar,
  stats,
  badges,
  isVerified,
  isOnline
}) => {
  const getBadgeIcon = (badge: string) => {
    switch (badge.toLowerCase()) {
      case 'top trader': return <Award style={{ width: '16px', height: '16px' }} />
      case 'risk master': return <Shield style={{ width: '16px', height: '16px' }} />
      case 'strategy king': return <Target style={{ width: '16px', height: '16px' }} />
      case 'market leader': return <Star style={{ width: '16px', height: '16px' }} />
      default: return <Award style={{ width: '16px', height: '16px' }} />
    }
  }

  const getBadgeColor = (badge: string) => {
    switch (badge.toLowerCase()) {
      case 'top trader': return { bg: 'linear-gradient(135deg, #F59E0B, #D97706)', border: '#F59E0B' }
      case 'risk master': return { bg: 'linear-gradient(135deg, #10B981, #059669)', border: '#10B981' }
      case 'strategy king': return { bg: 'linear-gradient(135deg, #8B5CF6, #7C3AED)', border: '#8B5CF6' }
      case 'market leader': return { bg: 'linear-gradient(135deg, #06B6D4, #0891B2)', border: '#06B6D4' }
      default: return { bg: 'linear-gradient(135deg, #6B7280, #4B5563)', border: '#6B7280' }
    }
  }

  return (
    <div style={{
      background: 'linear-gradient(135deg, rgba(255, 255, 255, 0.1) 0%, rgba(255, 255, 255, 0.05) 100%)',
      backdropFilter: 'blur(30px)',
      border: '2px solid rgba(255, 255, 255, 0.1)',
      borderRadius: '24px',
      padding: '32px',
      boxShadow: '0 20px 60px rgba(0, 0, 0, 0.1), inset 0 1px 0 rgba(255, 255, 255, 0.1)',
      position: 'relative',
      overflow: 'hidden',
      maxWidth: '400px',
      transition: 'all 0.4s cubic-bezier(0.16, 1, 0.3, 1)'
    }}>
      {/* Background Pattern */}
      <div style={{
        position: 'absolute',
        top: 0,
        right: 0,
        width: '150px',
        height: '150px',
        background: 'radial-gradient(circle, rgba(139, 92, 246, 0.1) 0%, transparent 70%)',
        borderRadius: '50%',
        transform: 'translate(50px, -50px)',
        filter: 'blur(30px)'
      }} />

      {/* Header */}
      <div style={{
        display: 'flex',
        alignItems: 'center',
        marginBottom: '24px',
        position: 'relative',
        zIndex: 2
      }}>
        {/* Avatar */}
        <div style={{
          position: 'relative',
          marginRight: '16px'
        }}>
          <div style={{
            width: '80px',
            height: '80px',
            borderRadius: '20px',
            background: avatar ? `url(${avatar})` : 'linear-gradient(135deg, #8B5CF6, #06B6D4)',
            backgroundSize: 'cover',
            backgroundPosition: 'center',
            border: '3px solid rgba(139, 92, 246, 0.3)',
            boxShadow: '0 10px 30px rgba(139, 92, 246, 0.3)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: 'white',
            fontSize: '24px',
            fontWeight: 'bold'
          }}>
            {!avatar && name.charAt(0).toUpperCase()}
          </div>
          
          {/* Online Status */}
          {isOnline && (
            <div style={{
              position: 'absolute',
              bottom: '2px',
              right: '2px',
              width: '20px',
              height: '20px',
              background: 'linear-gradient(135deg, #10B981, #059669)',
              borderRadius: '50%',
              border: '3px solid rgba(16, 185, 129, 0.3)',
              boxShadow: '0 0 10px rgba(16, 185, 129, 0.5)'
            }} />
          )}
        </div>

        <div style={{ flex: 1 }}>
          <div style={{
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
            marginBottom: '4px'
          }}>
            <h3 style={{
              color: 'white',
              fontSize: '20px',
              fontWeight: 'bold',
              margin: 0
            }}>{name}</h3>
            {isVerified && (
              <div style={{
                background: 'linear-gradient(135deg, #06B6D4, #0891B2)',
                borderRadius: '50%',
                padding: '4px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center'
              }}>
                <Shield style={{ width: '14px', height: '14px', color: 'white' }} />
              </div>
            )}
          </div>
          <p style={{
            color: 'rgba(203, 213, 225, 0.7)',
            fontSize: '14px',
            margin: 0
          }}>@{username}</p>
          <div style={{
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
            marginTop: '8px'
          }}>
            <Star style={{ width: '16px', height: '16px', color: '#F59E0B' }} />
            <span style={{
              color: 'rgba(203, 213, 225, 0.8)',
              fontSize: '14px',
              fontWeight: 500
            }}>Rank #{stats.rank}</span>
          </div>
        </div>
      </div>

      {/* Badges */}
      <div style={{
        display: 'flex',
        flexWrap: 'wrap',
        gap: '8px',
        marginBottom: '24px',
        position: 'relative',
        zIndex: 2
      }}>
        {badges.map((badge, index) => {
          const colors = getBadgeColor(badge)
          return (
            <div
              key={index}
              style={{
                background: colors.bg,
                border: `1px solid ${colors.border}40`,
                borderRadius: '12px',
                padding: '6px 12px',
                display: 'flex',
                alignItems: 'center',
                gap: '6px',
                boxShadow: `0 4px 12px rgba(${colors.border.slice(1).match(/.{2}/g)?.map(hex => parseInt(hex, 16)).join(', ')}, 0.3)`
              }}
            >
              {getBadgeIcon(badge)}
              <span style={{
                color: 'white',
                fontSize: '12px',
                fontWeight: 600
              }}>{badge}</span>
            </div>
          )
        })}
      </div>

      {/* Main Stats */}
      <div style={{
        display: 'grid',
        gridTemplateColumns: '1fr 1fr',
        gap: '16px',
        marginBottom: '24px',
        position: 'relative',
        zIndex: 2
      }}>
        <div style={{
          background: 'rgba(16, 185, 129, 0.1)',
          border: '1px solid rgba(16, 185, 129, 0.2)',
          borderRadius: '16px',
          padding: '20px 16px',
          textAlign: 'center',
          boxShadow: '0 8px 20px rgba(16, 185, 129, 0.1)'
        }}>
          <div style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            marginBottom: '8px'
          }}>
            {stats.totalPnL >= 0 ? (
              <TrendingUp style={{ width: '20px', height: '20px', color: '#10B981' }} />
            ) : (
              <TrendingDown style={{ width: '20px', height: '20px', color: '#EF4444' }} />
            )}
          </div>
          <div style={{
            fontSize: '24px',
            fontWeight: 'bold',
            color: stats.totalPnL >= 0 ? '#10B981' : '#EF4444',
            marginBottom: '4px'
          }}>
            ₹{(stats.totalPnL / 100000).toFixed(1)}L
          </div>
          <div style={{
            color: 'rgba(203, 213, 225, 0.7)',
            fontSize: '12px'
          }}>Total P&L</div>
        </div>

        <div style={{
          background: 'rgba(139, 92, 246, 0.1)',
          border: '1px solid rgba(139, 92, 246, 0.2)',
          borderRadius: '16px',
          padding: '20px 16px',
          textAlign: 'center',
          boxShadow: '0 8px 20px rgba(139, 92, 246, 0.1)'
        }}>
          <div style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            marginBottom: '8px'
          }}>
            <Target style={{ width: '20px', height: '20px', color: '#8B5CF6' }} />
          </div>
          <div style={{
            fontSize: '24px',
            fontWeight: 'bold',
            color: '#8B5CF6',
            marginBottom: '4px'
          }}>
            {stats.winRate}%
          </div>
          <div style={{
            color: 'rgba(203, 213, 225, 0.7)',
            fontSize: '12px'
          }}>Win Rate</div>
        </div>
      </div>

      {/* Detailed Stats */}
      <div style={{
        display: 'grid',
        gridTemplateColumns: '1fr 1fr 1fr',
        gap: '16px',
        marginBottom: '24px',
        position: 'relative',
        zIndex: 2
      }}>
        <div style={{ textAlign: 'center' }}>
          <div style={{
            fontSize: '18px',
            fontWeight: 'bold',
            color: 'white',
            marginBottom: '4px'
          }}>
            {stats.totalTrades}
          </div>
          <div style={{
            color: 'rgba(203, 213, 225, 0.7)',
            fontSize: '11px'
          }}>Trades</div>
        </div>
        <div style={{ textAlign: 'center' }}>
          <div style={{
            fontSize: '18px',
            fontWeight: 'bold',
            color: 'white',
            marginBottom: '4px'
          }}>
            {stats.avgReturn}%
          </div>
          <div style={{
            color: 'rgba(203, 213, 225, 0.7)',
            fontSize: '11px'
          }}>Avg Return</div>
        </div>
        <div style={{ textAlign: 'center' }}>
          <div style={{
            fontSize: '18px',
            fontWeight: 'bold',
            color: 'white',
            marginBottom: '4px'
          }}>
            {stats.sharpeRatio}
          </div>
          <div style={{
            color: 'rgba(203, 213, 225, 0.7)',
            fontSize: '11px'
          }}>Sharpe</div>
        </div>
      </div>

      {/* Footer */}
      <div style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        paddingTop: '16px',
        borderTop: '1px solid rgba(255, 255, 255, 0.1)',
        position: 'relative',
        zIndex: 2
      }}>
        <div style={{
          display: 'flex',
          alignItems: 'center',
          gap: '8px'
        }}>
          <Users style={{ width: '16px', height: '16px', color: '#06B6D4' }} />
          <span style={{
            color: 'rgba(203, 213, 225, 0.8)',
            fontSize: '14px'
          }}>
            {stats.followers.toLocaleString()} followers
          </span>
        </div>
        <button style={{
          background: 'linear-gradient(135deg, #8B5CF6, #06B6D4)',
          border: 'none',
          borderRadius: '12px',
          padding: '8px 16px',
          color: 'white',
          fontSize: '12px',
          fontWeight: 600,
          cursor: 'pointer',
          transition: 'all 0.3s ease',
          boxShadow: '0 4px 12px rgba(139, 92, 246, 0.3)'
        }}>
          Follow
        </button>
      </div>
    </div>
  )
}