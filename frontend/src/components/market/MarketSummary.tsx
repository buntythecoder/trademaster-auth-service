import React, { useEffect } from 'react';
import { useAppSelector, useAppDispatch } from '../../store';
import { fetchMarketSummary } from '../../store/slices/marketDataSlice';
import { formatCurrency, formatPercentage } from '../../utils/formatting';
import LoadingSpinner from '../common/LoadingSpinner';

interface MarketIndex {
  symbol: string;
  name: string;
  value: number;
  change: number;
  changePercent: number;
}

interface MarketSummaryStats {
  totalMarketCap: number;
  totalVolume: number;
  advancers: number;
  decliners: number;
  unchanged: number;
  marketSentiment: 'bullish' | 'bearish' | 'neutral';
}

const MARKET_INDICES: MarketIndex[] = [
  {
    symbol: 'SPY',
    name: 'S&P 500',
    value: 4180.35,
    change: 25.67,
    changePercent: 0.62
  },
  {
    symbol: 'QQQ',
    name: 'NASDAQ',
    value: 13842.12,
    change: -45.23,
    changePercent: -0.33
  },
  {
    symbol: 'DIA',
    name: 'Dow Jones',
    value: 33875.40,
    change: 156.78,
    changePercent: 0.46
  },
];

const MarketSummary: React.FC = () => {
  const dispatch = useAppDispatch();
  const { summary, isLoading } = useAppSelector(state => state.marketData);

  useEffect(() => {
    dispatch(fetchMarketSummary());
  }, [dispatch]);

  // Mock market stats for demo
  const marketStats: MarketSummaryStats = {
    totalMarketCap: 45678900000000,
    totalVolume: 12345678900,
    advancers: 2156,
    decliners: 1834,
    unchanged: 234,
    marketSentiment: 'bullish'
  };

  const getSentimentColor = (sentiment: string) => {
    switch (sentiment) {
      case 'bullish': return 'text-bull bg-bull/10';
      case 'bearish': return 'text-bear bg-bear/10';
      default: return 'text-warning bg-warning/10';
    }
  };

  const getSentimentIcon = (sentiment: string) => {
    switch (sentiment) {
      case 'bullish':
        return (
          <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
            <path fillRule="evenodd" d="M5.293 7.707a1 1 0 010-1.414l4-4a1 1 0 011.414 0l4 4a1 1 0 01-1.414 1.414L11 5.414V17a1 1 0 11-2 0V5.414L6.707 7.707a1 1 0 01-1.414 0z" clipRule="evenodd" />
          </svg>
        );
      case 'bearish':
        return (
          <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
            <path fillRule="evenodd" d="M14.707 12.293a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 111.414-1.414L9 14.586V3a1 1 0 012 0v11.586l2.293-2.293a1 1 0 011.414 0z" clipRule="evenodd" />
          </svg>
        );
      default:
        return (
          <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
            <path fillRule="evenodd" d="M3 10a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1z" clipRule="evenodd" />
          </svg>
        );
    }
  };

  return (
    <div style={{
      background: 'linear-gradient(135deg, rgba(255, 255, 255, 0.1) 0%, rgba(255, 255, 255, 0.05) 100%)',
      backdropFilter: 'blur(30px)',
      border: '1px solid rgba(255, 255, 255, 0.2)',
      borderRadius: '24px',
      boxShadow: '0 20px 60px rgba(0, 0, 0, 0.1), inset 0 1px 0 rgba(255, 255, 255, 0.1)',
      overflow: 'hidden'
    }}>
      {/* Header */}
      <div style={{
        padding: '24px 32px',
        borderBottom: '1px solid rgba(255, 255, 255, 0.1)'
      }}>
        <div style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between'
        }}>
          <h2 style={{
            fontSize: '28px',
            fontWeight: 700,
            background: 'linear-gradient(135deg, #8B5CF6, #06B6D4)',
            WebkitBackgroundClip: 'text',
            backgroundClip: 'text',
            WebkitTextFillColor: 'transparent',
            margin: 0
          }}>Market Summary</h2>
          {isLoading && <LoadingSpinner size="sm" />}
        </div>
      </div>

      <div style={{
        padding: '32px',
        display: 'flex',
        flexDirection: 'column',
        gap: '32px'
      }}>
        {/* Major Indices */}
        <div>
          <h3 style={{
            fontSize: '18px',
            fontWeight: 600,
            color: 'rgba(203, 213, 225, 0.9)',
            marginBottom: '20px'
          }}>Major Indices</h3>
          <div style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))',
            gap: '20px'
          }}>
            {MARKET_INDICES.map((index) => {
              const isPositive = index.changePercent >= 0;
              const color = isPositive ? '#10B981' : '#EF4444';
              return (
                <div
                  key={index.symbol}
                  style={{
                    background: 'linear-gradient(135deg, rgba(255, 255, 255, 0.08) 0%, rgba(255, 255, 255, 0.03) 100%)',
                    backdropFilter: 'blur(20px)',
                    border: `1px solid rgba(${color.slice(1).match(/.{2}/g)?.map(hex => parseInt(hex, 16)).join(', ')}, 0.2)`,
                    borderRadius: '16px',
                    padding: '20px',
                    boxShadow: `0 10px 30px rgba(${color.slice(1).match(/.{2}/g)?.map(hex => parseInt(hex, 16)).join(', ')}, 0.1)`,
                    transition: 'all 0.3s cubic-bezier(0.16, 1, 0.3, 1)',
                    cursor: 'pointer'
                  }}
                  onMouseEnter={(e) => {
                    e.currentTarget.style.transform = 'translateY(-2px)';
                    e.currentTarget.style.boxShadow = `0 20px 40px rgba(${color.slice(1).match(/.{2}/g)?.map(hex => parseInt(hex, 16)).join(', ')}, 0.2)`;
                  }}
                  onMouseLeave={(e) => {
                    e.currentTarget.style.transform = 'translateY(0)';
                    e.currentTarget.style.boxShadow = `0 10px 30px rgba(${color.slice(1).match(/.{2}/g)?.map(hex => parseInt(hex, 16)).join(', ')}, 0.1)`;
                  }}
                >
                  <div style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    marginBottom: '12px'
                  }}>
                    <div>
                      <h4 style={{
                        fontWeight: 600,
                        color: 'white',
                        fontSize: '16px',
                        margin: 0
                      }}>{index.name}</h4>
                      <p style={{
                        fontSize: '12px',
                        color: 'rgba(203, 213, 225, 0.6)',
                        margin: '4px 0 0 0'
                      }}>{index.symbol}</p>
                    </div>
                    <div style={{
                      padding: '6px 12px',
                      borderRadius: '8px',
                      fontSize: '12px',
                      fontWeight: 600,
                      background: `linear-gradient(135deg, ${color}20, ${color}10)`,
                      color: color,
                      border: `1px solid ${color}40`
                    }}>
                      {isPositive ? '+' : ''}{formatPercentage(index.changePercent)}
                    </div>
                  </div>
                  <div style={{
                    fontSize: '20px',
                    fontWeight: 700,
                    color: 'white',
                    marginBottom: '8px'
                  }}>
                    {formatCurrency(index.value)}
                  </div>
                  <div style={{
                    fontSize: '14px',
                    fontWeight: 600,
                    color: color
                  }}>
                    {isPositive ? '+' : ''}{formatCurrency(index.change)}
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* Market Statistics */}
        <div>
          <h3 style={{
            fontSize: '18px',
            fontWeight: 600,
            color: 'rgba(203, 213, 225, 0.9)',
            marginBottom: '20px'
          }}>Market Statistics</h3>
          <div style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
            gap: '20px'
          }}>
            <div style={{
              textAlign: 'center',
              background: 'linear-gradient(135deg, rgba(139, 92, 246, 0.1), rgba(139, 92, 246, 0.05))',
              backdropFilter: 'blur(20px)',
              border: '1px solid rgba(139, 92, 246, 0.2)',
              borderRadius: '16px',
              padding: '24px 20px'
            }}>
              <div style={{
                fontSize: '28px',
                fontWeight: 700,
                color: 'white',
                marginBottom: '8px'
              }}>
                {formatCurrency(marketStats.totalMarketCap, { compact: true })}
              </div>
              <div style={{
                fontSize: '14px',
                color: 'rgba(203, 213, 225, 0.7)'
              }}>Market Cap</div>
            </div>
            
            <div style={{
              textAlign: 'center',
              background: 'linear-gradient(135deg, rgba(6, 182, 212, 0.1), rgba(6, 182, 212, 0.05))',
              backdropFilter: 'blur(20px)',
              border: '1px solid rgba(6, 182, 212, 0.2)',
              borderRadius: '16px',
              padding: '24px 20px'
            }}>
              <div style={{
                fontSize: '28px',
                fontWeight: 700,
                color: 'white',
                marginBottom: '8px'
              }}>
                {(marketStats.totalVolume / 1000000000).toFixed(1)}B
              </div>
              <div style={{
                fontSize: '14px',
                color: 'rgba(203, 213, 225, 0.7)'
              }}>Volume</div>
            </div>
            
            <div style={{
              textAlign: 'center',
              background: 'linear-gradient(135deg, rgba(16, 185, 129, 0.1), rgba(16, 185, 129, 0.05))',
              backdropFilter: 'blur(20px)',
              border: '1px solid rgba(16, 185, 129, 0.2)',
              borderRadius: '16px',
              padding: '24px 20px'
            }}>
              <div style={{
                fontSize: '28px',
                fontWeight: 700,
                color: '#10B981',
                marginBottom: '8px'
              }}>
                {marketStats.advancers}
              </div>
              <div style={{
                fontSize: '14px',
                color: 'rgba(203, 213, 225, 0.7)'
              }}>Advancers</div>
            </div>
            
            <div style={{
              textAlign: 'center',
              background: 'linear-gradient(135deg, rgba(239, 68, 68, 0.1), rgba(239, 68, 68, 0.05))',
              backdropFilter: 'blur(20px)',
              border: '1px solid rgba(239, 68, 68, 0.2)',
              borderRadius: '16px',
              padding: '24px 20px'
            }}>
              <div style={{
                fontSize: '28px',
                fontWeight: 700,
                color: '#EF4444',
                marginBottom: '8px'
              }}>
                {marketStats.decliners}
              </div>
              <div style={{
                fontSize: '14px',
                color: 'rgba(203, 213, 225, 0.7)'
              }}>Decliners</div>
            </div>
          </div>
        </div>

        {/* Market Sentiment */}
        <div style={{
          borderTop: '1px solid rgba(255, 255, 255, 0.1)',
          paddingTop: '24px'
        }}>
          <div style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            marginBottom: '20px'
          }}>
            <div>
              <h3 style={{
                fontSize: '18px',
                fontWeight: 600,
                color: 'rgba(203, 213, 225, 0.9)',
                margin: 0
              }}>Market Sentiment</h3>
              <p style={{
                fontSize: '14px',
                color: 'rgba(203, 213, 225, 0.6)',
                margin: '4px 0 0 0'
              }}>
                Based on advance/decline ratio and volume analysis
              </p>
            </div>
            
            <div style={{
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              padding: '12px 20px',
              borderRadius: '12px',
              background: marketStats.marketSentiment === 'bullish' 
                ? 'linear-gradient(135deg, rgba(16, 185, 129, 0.2), rgba(16, 185, 129, 0.1))'
                : marketStats.marketSentiment === 'bearish'
                ? 'linear-gradient(135deg, rgba(239, 68, 68, 0.2), rgba(239, 68, 68, 0.1))'
                : 'linear-gradient(135deg, rgba(245, 158, 11, 0.2), rgba(245, 158, 11, 0.1))',
              border: marketStats.marketSentiment === 'bullish'
                ? '1px solid rgba(16, 185, 129, 0.3)'
                : marketStats.marketSentiment === 'bearish'
                ? '1px solid rgba(239, 68, 68, 0.3)'
                : '1px solid rgba(245, 158, 11, 0.3)'
            }}>
              <div style={{
                color: marketStats.marketSentiment === 'bullish'
                  ? '#10B981'
                  : marketStats.marketSentiment === 'bearish'
                  ? '#EF4444'
                  : '#F59E0B'
              }}>
                {getSentimentIcon(marketStats.marketSentiment)}
              </div>
              <span style={{
                fontWeight: 600,
                textTransform: 'capitalize',
                color: marketStats.marketSentiment === 'bullish'
                  ? '#10B981'
                  : marketStats.marketSentiment === 'bearish'
                  ? '#EF4444'
                  : '#F59E0B'
              }}>
                {marketStats.marketSentiment}
              </span>
            </div>
          </div>

          {/* Advance/Decline Ratio Bar */}
          <div style={{ marginTop: '16px' }}>
            <div style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              fontSize: '14px',
              color: 'rgba(203, 213, 225, 0.7)',
              marginBottom: '8px'
            }}>
              <span>Advancers vs Decliners</span>
              <span>
                {((marketStats.advancers / (marketStats.advancers + marketStats.decliners)) * 100).toFixed(1)}% advancing
              </span>
            </div>
            <div style={{
              width: '100%',
              background: 'rgba(255, 255, 255, 0.1)',
              borderRadius: '8px',
              height: '8px',
              overflow: 'hidden'
            }}>
              <div
                style={{
                  background: 'linear-gradient(90deg, #10B981, #06B6D4)',
                  height: '100%',
                  borderRadius: '8px',
                  transition: 'all 0.3s ease',
                  width: `${(marketStats.advancers / (marketStats.advancers + marketStats.decliners)) * 100}%`,
                  boxShadow: '0 0 10px rgba(16, 185, 129, 0.4)'
                }}
              />
            </div>
          </div>
        </div>

        {/* Market Status */}
        <div style={{
          borderTop: '1px solid rgba(255, 255, 255, 0.1)',
          paddingTop: '20px'
        }}>
          <div style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            fontSize: '16px',
            marginBottom: '12px'
          }}>
            <span style={{ color: 'rgba(203, 213, 225, 0.7)' }}>Market Status</span>
            <div style={{
              display: 'flex',
              alignItems: 'center',
              gap: '8px'
            }}>
              <div style={{
                width: '8px',
                height: '8px',
                background: '#10B981',
                borderRadius: '50%',
                animation: 'pulse 2s infinite'
              }}></div>
              <span style={{
                color: '#10B981',
                fontWeight: 600
              }}>Market Open</span>
            </div>
          </div>
          
          <div style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            fontSize: '14px',
            color: 'rgba(203, 213, 225, 0.6)'
          }}>
            <span>Last Updated</span>
            <span>{new Date().toLocaleTimeString()}</span>
          </div>
        </div>
      </div>
      
      <style>{`
        @keyframes pulse {
          0%, 100% {
            opacity: 1;
          }
          50% {
            opacity: 0.5;
          }
        }
      `}</style>
    </div>
  );
};

export default MarketSummary;