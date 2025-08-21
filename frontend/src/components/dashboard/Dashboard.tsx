import React, { useEffect } from 'react';
import { useAppDispatch, useAppSelector } from '../../store';
import { fetchPortfolios, selectPortfolios } from '../../store/slices/portfolioSlice';
import { selectCurrentUser } from '../../store/slices/authSlice';
import DashboardLayout from '../layout/DashboardLayout';
import { TrendingUp, TrendingDown, DollarSign, Activity, BarChart3, ArrowUpRight, ArrowDownRight, Clock, Target, Shield, Zap } from 'lucide-react';

// Premium Stats Card Component
const StatsCard: React.FC<{
  title: string;
  value: string;
  change: string;
  changeType: 'positive' | 'negative' | 'neutral';
  icon: React.ReactNode;
  color: string;
  gradient: string;
}> = ({ title, value, change, changeType, icon, color, gradient }) => (
  <div style={{
    background: 'linear-gradient(135deg, rgba(255, 255, 255, 0.1) 0%, rgba(255, 255, 255, 0.05) 100%)',
    backdropFilter: 'blur(30px)',
    border: `2px solid rgba(${color.slice(1).match(/.{2}/g)?.map(hex => parseInt(hex, 16)).join(', ')}, 0.2)`,
    borderRadius: '20px',
    padding: '24px',
    position: 'relative',
    overflow: 'hidden',
    boxShadow: `0 20px 60px rgba(${color.slice(1).match(/.{2}/g)?.map(hex => parseInt(hex, 16)).join(', ')}, 0.15), inset 0 1px 0 rgba(255, 255, 255, 0.1)`,
    transition: 'all 0.4s cubic-bezier(0.16, 1, 0.3, 1)',
    cursor: 'pointer'
  }}
  onMouseEnter={(e) => {
    e.currentTarget.style.transform = 'translateY(-8px) scale(1.02)'
    e.currentTarget.style.borderColor = `rgba(${color.slice(1).match(/.{2}/g)?.map(hex => parseInt(hex, 16)).join(', ')}, 0.4)`
    e.currentTarget.style.boxShadow = `0 30px 80px rgba(${color.slice(1).match(/.{2}/g)?.map(hex => parseInt(hex, 16)).join(', ')}, 0.25), inset 0 1px 0 rgba(255, 255, 255, 0.2)`
  }}
  onMouseLeave={(e) => {
    e.currentTarget.style.transform = 'translateY(0) scale(1)'
    e.currentTarget.style.borderColor = `rgba(${color.slice(1).match(/.{2}/g)?.map(hex => parseInt(hex, 16)).join(', ')}, 0.2)`
    e.currentTarget.style.boxShadow = `0 20px 60px rgba(${color.slice(1).match(/.{2}/g)?.map(hex => parseInt(hex, 16)).join(', ')}, 0.15), inset 0 1px 0 rgba(255, 255, 255, 0.1)`
  }}
  >
    {/* Background Gradient */}
    <div style={{
      position: 'absolute',
      top: 0,
      right: 0,
      width: '100px',
      height: '100px',
      background: gradient,
      opacity: 0.1,
      borderRadius: '50%',
      transform: 'translate(30px, -30px)',
      filter: 'blur(20px)'
    }} />
    
    <div style={{
      display: 'flex',
      alignItems: 'flex-start',
      justifyContent: 'space-between',
      position: 'relative',
      zIndex: 2
    }}>
      <div style={{ flex: 1 }}>
        <p style={{
          color: 'rgba(203, 213, 225, 0.8)',
          fontSize: '14px',
          fontWeight: 500,
          marginBottom: '8px'
        }}>{title}</p>
        <p style={{
          color: 'white',
          fontSize: '32px',
          fontWeight: 'bold',
          marginBottom: '12px',
          lineHeight: 1
        }}>{value}</p>
        <div style={{
          display: 'flex',
          alignItems: 'center',
          gap: '6px'
        }}>
          {changeType === 'positive' ? (
            <ArrowUpRight style={{ width: '16px', height: '16px', color: '#10B981' }} />
          ) : changeType === 'negative' ? (
            <ArrowDownRight style={{ width: '16px', height: '16px', color: '#EF4444' }} />
          ) : (
            <Activity style={{ width: '16px', height: '16px', color: '#6B7280' }} />
          )}
          <span style={{
            color: changeType === 'positive' ? '#10B981' : changeType === 'negative' ? '#EF4444' : '#6B7280',
            fontSize: '14px',
            fontWeight: 600
          }}>{change}</span>
        </div>
      </div>
      
      <div style={{
        width: '60px',
        height: '60px',
        background: gradient,
        borderRadius: '16px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        color: 'white',
        boxShadow: `0 10px 30px rgba(${color.slice(1).match(/.{2}/g)?.map(hex => parseInt(hex, 16)).join(', ')}, 0.3)`
      }}>
        {icon}
      </div>
    </div>
  </div>
);

// Premium Quick Action Card
const QuickActionCard: React.FC<{
  title: string;
  description: string;
  buttonText: string;
  onClick: () => void;
  icon: React.ReactNode;
  color: string;
  gradient: string;
}> = ({ title, description, buttonText, onClick, icon, color, gradient }) => (
  <div style={{
    background: 'linear-gradient(135deg, rgba(255, 255, 255, 0.1) 0%, rgba(255, 255, 255, 0.05) 100%)',
    backdropFilter: 'blur(30px)',
    border: '1px solid rgba(255, 255, 255, 0.2)',
    borderRadius: '20px',
    padding: '28px',
    position: 'relative',
    overflow: 'hidden',
    transition: 'all 0.4s cubic-bezier(0.16, 1, 0.3, 1)',
    cursor: 'pointer',
    boxShadow: '0 20px 60px rgba(0, 0, 0, 0.1), inset 0 1px 0 rgba(255, 255, 255, 0.1)'
  }}
  onMouseEnter={(e) => {
    e.currentTarget.style.transform = 'translateY(-8px) scale(1.02)'
    e.currentTarget.style.borderColor = `rgba(${color.slice(1).match(/.{2}/g)?.map(hex => parseInt(hex, 16)).join(', ')}, 0.4)`
    e.currentTarget.style.boxShadow = `0 30px 80px rgba(${color.slice(1).match(/.{2}/g)?.map(hex => parseInt(hex, 16)).join(', ')}, 0.2), inset 0 1px 0 rgba(255, 255, 255, 0.2)`
  }}
  onMouseLeave={(e) => {
    e.currentTarget.style.transform = 'translateY(0) scale(1)'
    e.currentTarget.style.borderColor = 'rgba(255, 255, 255, 0.2)'
    e.currentTarget.style.boxShadow = '0 20px 60px rgba(0, 0, 0, 0.1), inset 0 1px 0 rgba(255, 255, 255, 0.1)'
  }}
  onClick={onClick}
  >
    <div style={{
      display: 'flex',
      alignItems: 'flex-start',
      gap: '20px',
      position: 'relative',
      zIndex: 2
    }}>
      <div style={{
        width: '56px',
        height: '56px',
        background: gradient,
        borderRadius: '16px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        color: 'white',
        flexShrink: 0,
        boxShadow: `0 10px 30px rgba(${color.slice(1).match(/.{2}/g)?.map(hex => parseInt(hex, 16)).join(', ')}, 0.3)`
      }}>
        {icon}
      </div>
      
      <div style={{ flex: 1 }}>
        <h3 style={{
          color: 'white',
          fontSize: '20px',
          fontWeight: 700,
          marginBottom: '8px'
        }}>{title}</h3>
        <p style={{
          color: 'rgba(203, 213, 225, 0.8)',
          fontSize: '14px',
          lineHeight: 1.5,
          marginBottom: '20px'
        }}>{description}</p>
        <button style={{
          background: gradient,
          border: 'none',
          borderRadius: '12px',
          padding: '12px 20px',
          color: 'white',
          fontSize: '14px',
          fontWeight: 600,
          cursor: 'pointer',
          transition: 'all 0.3s ease',
          boxShadow: `0 8px 20px rgba(${color.slice(1).match(/.{2}/g)?.map(hex => parseInt(hex, 16)).join(', ')}, 0.3)`
        }}>
          {buttonText}
        </button>
      </div>
    </div>
  </div>
);

// Premium Recent Activity Component
const RecentActivity: React.FC = () => (
  <div style={{
    background: 'linear-gradient(135deg, rgba(255, 255, 255, 0.1) 0%, rgba(255, 255, 255, 0.05) 100%)',
    backdropFilter: 'blur(30px)',
    border: '1px solid rgba(255, 255, 255, 0.2)',
    borderRadius: '20px',
    boxShadow: '0 20px 60px rgba(0, 0, 0, 0.1), inset 0 1px 0 rgba(255, 255, 255, 0.1)',
    overflow: 'hidden'
  }}>
    <div style={{
      padding: '24px 24px 0 24px',
      borderBottom: '1px solid rgba(255, 255, 255, 0.1)'
    }}>
      <h3 style={{
        color: 'white',
        fontSize: '20px',
        fontWeight: 700,
        marginBottom: '20px'
      }}>Recent Activity</h3>
    </div>
    <div style={{ padding: '24px' }}>
      <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
        {[
          {
            action: 'Buy Order Executed',
            details: 'RELIANCE - 100 shares @ ₹2,450.25',
            time: '2 minutes ago',
            type: 'success',
            icon: <TrendingUp style={{ width: '16px', height: '16px' }} />
          },
          {
            action: 'Price Alert Triggered',
            details: 'TCS reached target price of ₹3,500',
            time: '15 minutes ago',
            type: 'info',
            icon: <Target style={{ width: '16px', height: '16px' }} />
          },
          {
            action: 'Sell Order Placed',
            details: 'HDFC - 50 shares @ ₹1,680.00',
            time: '1 hour ago',
            type: 'warning',
            icon: <TrendingDown style={{ width: '16px', height: '16px' }} />
          },
        ].map((activity, index) => (
          <div key={index} style={{
            display: 'flex',
            alignItems: 'flex-start',
            gap: '16px',
            padding: '16px',
            background: 'rgba(255, 255, 255, 0.05)',
            borderRadius: '12px',
            border: '1px solid rgba(255, 255, 255, 0.1)'
          }}>
            <div style={{
              width: '40px',
              height: '40px',
              borderRadius: '12px',
              background: activity.type === 'success' ? 'linear-gradient(135deg, #10B981, #059669)' : 
                         activity.type === 'info' ? 'linear-gradient(135deg, #06B6D4, #0891B2)' : 
                         'linear-gradient(135deg, #F59E0B, #D97706)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: 'white',
              flexShrink: 0
            }}>
              {activity.icon}
            </div>
            <div style={{ flex: 1 }}>
              <p style={{
                color: 'white',
                fontSize: '16px',
                fontWeight: 600,
                marginBottom: '4px'
              }}>{activity.action}</p>
              <p style={{
                color: 'rgba(203, 213, 225, 0.8)',
                fontSize: '14px',
                marginBottom: '6px'
              }}>{activity.details}</p>
              <div style={{
                display: 'flex',
                alignItems: 'center',
                gap: '6px'
              }}>
                <Clock style={{ width: '12px', height: '12px', color: 'rgba(203, 213, 225, 0.6)' }} />
                <p style={{
                  color: 'rgba(203, 213, 225, 0.6)',
                  fontSize: '12px'
                }}>{activity.time}</p>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  </div>
);

// Premium Market Overview Component
const MarketOverview: React.FC = () => (
  <div style={{
    background: 'linear-gradient(135deg, rgba(255, 255, 255, 0.1) 0%, rgba(255, 255, 255, 0.05) 100%)',
    backdropFilter: 'blur(30px)',
    border: '1px solid rgba(255, 255, 255, 0.2)',
    borderRadius: '20px',
    boxShadow: '0 20px 60px rgba(0, 0, 0, 0.1), inset 0 1px 0 rgba(255, 255, 255, 0.1)',
    overflow: 'hidden'
  }}>
    <div style={{
      padding: '24px 24px 0 24px',
      borderBottom: '1px solid rgba(255, 255, 255, 0.1)'
    }}>
      <h3 style={{
        color: 'white',
        fontSize: '20px',
        fontWeight: 700,
        marginBottom: '20px'
      }}>Market Overview</h3>
    </div>
    <div style={{ padding: '24px' }}>
      <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
        {[
          { name: 'NIFTY 50', value: '21,185.47', change: '+0.85%', positive: true },
          { name: 'SENSEX', value: '70,965.34', change: '+1.24%', positive: true },
          { name: 'BANK NIFTY', value: '45,852.53', change: '-0.32%', positive: false },
          { name: 'INDIA VIX', value: '18.45', change: '-2.15%', positive: true },
        ].map((index, i) => (
          <div key={i} style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            padding: '16px',
            background: 'rgba(255, 255, 255, 0.05)',
            borderRadius: '12px',
            border: '1px solid rgba(255, 255, 255, 0.1)'
          }}>
            <div>
              <p style={{
                color: 'white',
                fontSize: '16px',
                fontWeight: 600,
                marginBottom: '4px'
              }}>{index.name}</p>
              <p style={{
                color: 'rgba(203, 213, 225, 0.8)',
                fontSize: '18px',
                fontFamily: 'monospace',
                fontWeight: 'bold'
              }}>{index.value}</p>
            </div>
            <div style={{
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              padding: '6px 12px',
              borderRadius: '8px',
              background: index.positive ? 'rgba(16, 185, 129, 0.2)' : 'rgba(239, 68, 68, 0.2)'
            }}>
              {index.positive ? (
                <TrendingUp style={{ width: '16px', height: '16px', color: '#10B981' }} />
              ) : (
                <TrendingDown style={{ width: '16px', height: '16px', color: '#EF4444' }} />
              )}
              <span style={{
                color: index.positive ? '#10B981' : '#EF4444',
                fontSize: '14px',
                fontWeight: 600
              }}>{index.change}</span>
            </div>
          </div>
        ))}
      </div>
    </div>
  </div>
);

export const Dashboard: React.FC = () => {
  const dispatch = useAppDispatch();
  const currentUser = useAppSelector(selectCurrentUser);
  const portfolios = useAppSelector(selectPortfolios);

  useEffect(() => {
    dispatch(fetchPortfolios({}));
  }, [dispatch]);

  const handleQuickAction = (action: string) => {
    console.log('Quick action:', action);
  };

  return (
    <DashboardLayout>
      <div style={{
        minHeight: '100vh',
        background: 'radial-gradient(ellipse at top, #0f0f23 0%, #1a0b2e 25%, #0a0a0a 100%)',
        padding: '40px 20px',
        position: 'relative'
      }}>
        {/* Background Elements */}
        <div style={{
          position: 'absolute',
          top: '5%',
          left: '5%',
          width: '200px',
          height: '200px',
          background: 'radial-gradient(circle, rgba(139, 92, 246, 0.1) 0%, transparent 70%)',
          borderRadius: '50%',
          filter: 'blur(60px)'
        }} />
        <div style={{
          position: 'absolute',
          top: '10%',
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
          {/* Welcome Header */}
          <div style={{
            marginBottom: '48px',
            textAlign: 'center'
          }}>
            <h1 style={{
              fontSize: 'clamp(2rem, 5vw, 3rem)',
              fontWeight: 800,
              color: 'white',
              marginBottom: '16px',
              background: 'linear-gradient(135deg, #8B5CF6, #06B6D4, #10B981)',
              WebkitBackgroundClip: 'text',
              backgroundClip: 'text',
              WebkitTextFillColor: 'transparent',
              backgroundSize: '200% 200%',
              animation: 'gradientMove 4s ease-in-out infinite'
            }}>
              Welcome back, {currentUser?.firstName || 'Trader'}!
            </h1>
            <p style={{
              color: 'rgba(203, 213, 225, 0.8)',
              fontSize: '18px',
              maxWidth: '600px',
              margin: '0 auto'
            }}>
              Here's what's happening with your trading portfolio today
            </p>
          </div>

          {/* Key Metrics */}
          <div style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))',
            gap: '24px',
            marginBottom: '48px'
          }}>
            <StatsCard
              title="Portfolio Value"
              value="₹12,54,305"
              change="+₹28,402 (+2.32%)"
              changeType="positive"
              icon={<DollarSign style={{ width: '24px', height: '24px' }} />}
              color="#8B5CF6"
              gradient="linear-gradient(135deg, #8B5CF6, #7C3AED)"
            />
            <StatsCard
              title="Today's P&L"
              value="₹12,458"
              change="+₹2,458 (+24.5%)"
              changeType="positive"
              icon={<TrendingUp style={{ width: '24px', height: '24px' }} />}
              color="#10B981"
              gradient="linear-gradient(135deg, #10B981, #059669)"
            />
            <StatsCard
              title="Open Positions"
              value="12"
              change="+2 new positions"
              changeType="neutral"
              icon={<BarChart3 style={{ width: '24px', height: '24px' }} />}
              color="#06B6D4"
              gradient="linear-gradient(135deg, #06B6D4, #0891B2)"
            />
            <StatsCard
              title="Cash Available"
              value="₹1,54,200"
              change="-₹55,800 (trades)"
              changeType="negative"
              icon={<Activity style={{ width: '24px', height: '24px' }} />}
              color="#F59E0B"
              gradient="linear-gradient(135deg, #F59E0B, #D97706)"
            />
          </div>

          {/* Main Content Grid */}
          <div style={{
            display: 'grid',
            gridTemplateColumns: '2fr 1fr',
            gap: '32px',
            alignItems: 'start'
          }}>
            {/* Left Column */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: '32px' }}>
              {/* Quick Actions */}
              <div style={{
                display: 'grid',
                gridTemplateColumns: '1fr 1fr',
                gap: '24px'
              }}>
                <QuickActionCard
                  title="Place New Order"
                  description="Buy or sell stocks with real-time market data and AI recommendations"
                  buttonText="Start Trading"
                  onClick={() => handleQuickAction('trade')}
                  icon={<Zap style={{ width: '24px', height: '24px' }} />}
                  color="#8B5CF6"
                  gradient="linear-gradient(135deg, #8B5CF6, #7C3AED)"
                />
                <QuickActionCard
                  title="Analyze Portfolio"
                  description="View detailed performance metrics and risk analysis with AI insights"
                  buttonText="View Analytics"
                  onClick={() => handleQuickAction('analytics')}
                  icon={<Shield style={{ width: '24px', height: '24px' }} />}
                  color="#10B981"
                  gradient="linear-gradient(135deg, #10B981, #059669)"
                />
              </div>

              {/* Portfolio Performance Chart */}
              <div style={{
                background: 'linear-gradient(135deg, rgba(255, 255, 255, 0.1) 0%, rgba(255, 255, 255, 0.05) 100%)',
                backdropFilter: 'blur(30px)',
                border: '1px solid rgba(255, 255, 255, 0.2)',
                borderRadius: '20px',
                padding: '32px',
                boxShadow: '0 20px 60px rgba(0, 0, 0, 0.1), inset 0 1px 0 rgba(255, 255, 255, 0.1)'
              }}>
                <div style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  marginBottom: '24px'
                }}>
                  <h3 style={{
                    color: 'white',
                    fontSize: '24px',
                    fontWeight: 700
                  }}>Portfolio Performance</h3>
                  <div style={{ display: 'flex', gap: '8px' }}>
                    {['1D', '1W', '1M', '3M', '1Y'].map((period) => (
                      <button
                        key={period}
                        style={{
                          padding: '8px 16px',
                          fontSize: '14px',
                          fontWeight: 600,
                          color: 'rgba(203, 213, 225, 0.8)',
                          background: 'rgba(255, 255, 255, 0.1)',
                          border: '1px solid rgba(255, 255, 255, 0.2)',
                          borderRadius: '8px',
                          cursor: 'pointer',
                          transition: 'all 0.3s ease'
                        }}
                      >
                        {period}
                      </button>
                    ))}
                  </div>
                </div>
                <div style={{
                  height: '300px',
                  background: 'rgba(255, 255, 255, 0.05)',
                  borderRadius: '16px',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  border: '1px solid rgba(255, 255, 255, 0.1)'
                }}>
                  <div style={{ textAlign: 'center' }}>
                    <BarChart3 style={{
                      width: '60px',
                      height: '60px',
                      color: 'rgba(139, 92, 246, 0.5)',
                      marginBottom: '16px'
                    }} />
                    <p style={{
                      color: 'rgba(203, 213, 225, 0.8)',
                      fontSize: '18px',
                      fontWeight: 600,
                      marginBottom: '8px'
                    }}>Advanced Chart Coming Soon</p>
                    <p style={{
                      color: 'rgba(148, 163, 184, 0.6)',
                      fontSize: '14px'
                    }}>Interactive portfolio performance visualization</p>
                  </div>
                </div>
              </div>
            </div>

            {/* Right Column */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: '32px' }}>
              <MarketOverview />
              <RecentActivity />
            </div>
          </div>
        </div>

        <style>{`
          @keyframes gradientMove {
            0%, 100% { background-position: 0% 50%; }
            50% { background-position: 100% 50%; }
          }
        `}</style>
      </div>
    </DashboardLayout>
  );
};

export default Dashboard;