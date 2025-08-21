import React from 'react';
import { MarketData } from '../../types';
import { formatCurrency, formatPercentage } from '../../utils/formatting';

interface MarketCardProps {
  marketData: MarketData;
  onClick?: () => void;
  isSelected?: boolean;
}

const MarketCard: React.FC<MarketCardProps> = ({ 
  marketData, 
  onClick, 
  isSelected = false 
}) => {
  const isPositive = marketData.changePercent >= 0;
  const color = isPositive ? '#10B981' : '#EF4444';
  const selectedColor = isSelected ? '#8B5CF6' : color;

  return (
    <div
      style={{
        background: 'linear-gradient(135deg, rgba(255, 255, 255, 0.1) 0%, rgba(255, 255, 255, 0.05) 100%)',
        backdropFilter: 'blur(30px)',
        border: `2px solid ${isSelected ? 'rgba(139, 92, 246, 0.4)' : `rgba(${color.slice(1).match(/.{2}/g)?.map(hex => parseInt(hex, 16)).join(', ')}, 0.2)`}`,
        borderRadius: '20px',
        boxShadow: `0 20px 60px rgba(${selectedColor.slice(1).match(/.{2}/g)?.map(hex => parseInt(hex, 16)).join(', ')}, 0.15), inset 0 1px 0 rgba(255, 255, 255, 0.1)`,
        cursor: onClick ? 'pointer' : 'default',
        transition: 'all 0.4s cubic-bezier(0.16, 1, 0.3, 1)',
        overflow: 'hidden',
        position: 'relative'
      }}
      onClick={onClick}
      onMouseEnter={(e) => {
        if (onClick) {
          e.currentTarget.style.transform = 'translateY(-8px)';
          e.currentTarget.style.boxShadow = `0 30px 80px rgba(${selectedColor.slice(1).match(/.{2}/g)?.map(hex => parseInt(hex, 16)).join(', ')}, 0.25), inset 0 1px 0 rgba(255, 255, 255, 0.2)`;
        }
      }}
      onMouseLeave={(e) => {
        if (onClick) {
          e.currentTarget.style.transform = 'translateY(0)';
          e.currentTarget.style.boxShadow = `0 20px 60px rgba(${selectedColor.slice(1).match(/.{2}/g)?.map(hex => parseInt(hex, 16)).join(', ')}, 0.15), inset 0 1px 0 rgba(255, 255, 255, 0.1)`;
        }
      }}
    >
      {/* Floating orb background effect */}
      <div style={{
        position: 'absolute',
        top: '-20px',
        right: '-20px',
        width: '80px',
        height: '80px',
        background: `radial-gradient(circle, ${color}15 0%, transparent 70%)`,
        borderRadius: '50%',
        filter: 'blur(20px)'
      }} />
      
      <div style={{ padding: '28px', position: 'relative' }}>
        <div style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          marginBottom: '20px'
        }}>
          <div>
            <h3 style={{
              fontSize: '22px',
              fontWeight: 700,
              color: 'white',
              margin: 0,
              marginBottom: '4px'
            }}>
              {marketData.symbol}
            </h3>
            <p style={{
              fontSize: '14px',
              color: 'rgba(203, 213, 225, 0.7)',
              margin: 0
            }}>{marketData.name}</p>
          </div>
          <div style={{
            padding: '8px 16px',
            borderRadius: '12px',
            background: `linear-gradient(135deg, ${color}25, ${color}10)`,
            border: `1px solid ${color}40`,
            backdropFilter: 'blur(10px)'
          }}>
            <span style={{
              fontSize: '14px',
              fontWeight: 600,
              color: color
            }}>
              {isPositive ? '+' : ''}{formatPercentage(marketData.changePercent)}
            </span>
          </div>
        </div>

        <div style={{
          display: 'flex',
          flexDirection: 'column',
          gap: '16px'
        }}>
          <div style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center'
          }}>
            <span style={{
              fontSize: '14px',
              color: 'rgba(203, 213, 225, 0.7)'
            }}>Price</span>
            <span style={{
              fontSize: '24px',
              fontWeight: 700,
              color: 'white'
            }}>
              {formatCurrency(marketData.currentPrice)}
            </span>
          </div>

          <div style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center'
          }}>
            <span style={{
              fontSize: '14px',
              color: 'rgba(203, 213, 225, 0.7)'
            }}>Change</span>
            <span style={{
              fontWeight: 600,
              color: color,
              fontSize: '16px'
            }}>
              {isPositive ? '+' : ''}{formatCurrency(marketData.change)}
            </span>
          </div>

          <div style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center'
          }}>
            <span style={{
              fontSize: '14px',
              color: 'rgba(203, 213, 225, 0.7)'
            }}>Volume</span>
            <span style={{
              fontSize: '14px',
              fontWeight: 600,
              color: 'rgba(203, 213, 225, 0.9)'
            }}>
              {marketData.volume.toLocaleString()}
            </span>
          </div>

          {marketData.marketCap && (
            <div style={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center'
            }}>
              <span style={{
                fontSize: '14px',
                color: 'rgba(203, 213, 225, 0.7)'
              }}>Market Cap</span>
              <span style={{
                fontSize: '14px',
                fontWeight: 600,
                color: 'rgba(203, 213, 225, 0.9)'
              }}>
                {formatCurrency(marketData.marketCap, { compact: true })}
              </span>
            </div>
          )}
        </div>

        <div style={{
          marginTop: '20px',
          paddingTop: '16px',
          borderTop: '1px solid rgba(255, 255, 255, 0.1)'
        }}>
          <div style={{
            display: 'flex',
            justifyContent: 'space-between',
            fontSize: '12px',
            color: 'rgba(203, 213, 225, 0.6)'
          }}>
            <span>High: {formatCurrency(marketData.dayHigh)}</span>
            <span>Low: {formatCurrency(marketData.dayLow)}</span>
          </div>
        </div>
      </div>
    </div>
  );
};

export default MarketCard;