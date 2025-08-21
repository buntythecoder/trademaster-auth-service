import React, { useState, useEffect } from 'react';
import { useAppSelector, useAppDispatch } from '../../store';
import { fetchMarketOverview } from '../../store/slices/marketDataSlice';
import MarketCard from './MarketCard';
import LoadingSpinner from '../common/LoadingSpinner';
import EmptyState from '../common/EmptyState';
import { MarketData } from '../../types';

interface MarketOverviewProps {
  watchlistSymbols?: string[];
  onSymbolSelect?: (symbol: string) => void;
  selectedSymbol?: string;
  showSearch?: boolean;
  maxItems?: number;
}

const MarketOverview: React.FC<MarketOverviewProps> = ({
  watchlistSymbols = ['AAPL', 'GOOGL', 'MSFT', 'AMZN', 'TSLA', 'NVDA'],
  onSymbolSelect,
  selectedSymbol,
  showSearch = true,
  maxItems,
}) => {
  const dispatch = useAppDispatch();
  const { 
    overview, 
    isLoading, 
    error,
    quotes 
  } = useAppSelector(state => state.marketData);
  
  const [searchTerm, setSearchTerm] = useState('');
  const [filteredData, setFilteredData] = useState<MarketData[]>([]);

  useEffect(() => {
    if (watchlistSymbols.length > 0) {
      dispatch(fetchMarketOverview(watchlistSymbols));
    }
  }, [dispatch, watchlistSymbols]);

  useEffect(() => {
    // Create market data array from quotes
    const marketDataArray = watchlistSymbols.map(symbol => {
      const quote = quotes[symbol];
      if (!quote) {
        return {
          symbol,
          name: symbol,
          currentPrice: 0,
          change: 0,
          changePercent: 0,
          volume: 0,
          dayHigh: 0,
          dayLow: 0,
          timestamp: new Date().toISOString(),
        } as MarketData;
      }
      return quote;
    }).filter(Boolean);

    // Apply search filter
    const filtered = searchTerm
      ? marketDataArray.filter(item =>
          item.symbol.toLowerCase().includes(searchTerm.toLowerCase()) ||
          item.name?.toLowerCase().includes(searchTerm.toLowerCase())
        )
      : marketDataArray;

    // Apply max items limit
    const limited = maxItems ? filtered.slice(0, maxItems) : filtered;
    
    setFilteredData(limited);
  }, [quotes, watchlistSymbols, searchTerm, maxItems]);

  const handleSymbolClick = (symbol: string) => {
    if (onSymbolSelect) {
      onSymbolSelect(symbol);
    }
  };

  if (isLoading && filteredData.length === 0) {
    return (
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-xl font-semibold text-gray-900">Market Overview</h2>
          <LoadingSpinner size="sm" />
        </div>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {Array.from({ length: 6 }).map((_, index) => (
            <div key={index} className="animate-pulse bg-gray-200 rounded-lg h-40" />
          ))}
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="space-y-4">
        <h2 className="text-xl font-semibold text-gray-900">Market Overview</h2>
        <div className="bg-red-50 border border-red-200 rounded-lg p-4">
          <div className="flex items-center">
            <div className="flex-shrink-0">
              <svg className="h-5 w-5 text-red-400" viewBox="0 0 20 20" fill="currentColor">
                <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clipRule="evenodd" />
              </svg>
            </div>
            <div className="ml-3">
              <h3 className="text-sm font-medium text-red-800">
                Market Data Error
              </h3>
              <div className="mt-2 text-sm text-red-700">
                <p>Unable to load market data. Please try again later.</p>
                <p className="mt-1 text-xs opacity-75">{error}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    );
  }

  if (filteredData.length === 0) {
    return (
      <div className="space-y-4">
        <h2 className="text-xl font-semibold text-gray-900">Market Overview</h2>
        <EmptyState
          title="No Market Data"
          description={searchTerm 
            ? `No symbols found matching "${searchTerm}"`
            : "No market data available at this time"
          }
          action={searchTerm ? {
            label: "Clear Search",
            onClick: () => setSearchTerm('')
          } : undefined}
        />
      </div>
    );
  }

  return (
    <div style={{
      display: 'flex',
      flexDirection: 'column',
      gap: '32px'
    }}>
      {/* Header */}
      <div style={{
        display: 'flex',
        flexDirection: 'column',
        gap: '20px',
        alignItems: 'center',
        justifyContent: 'space-between'
      }}>
        <div style={{ textAlign: 'center' }}>
          <h2 style={{
            fontSize: '32px',
            fontWeight: 700,
            background: 'linear-gradient(135deg, #8B5CF6, #06B6D4)',
            WebkitBackgroundClip: 'text',
            backgroundClip: 'text',
            WebkitTextFillColor: 'transparent',
            marginBottom: '8px'
          }}>Market Overview</h2>
          <p style={{
            fontSize: '16px',
            color: 'rgba(203, 213, 225, 0.8)',
            margin: 0
          }}>
            Real-time market data for your watchlist
          </p>
        </div>
        
        {/* Search */}
        {showSearch && (
          <div style={{
            position: 'relative',
            maxWidth: '400px',
            width: '100%'
          }}>
            <div style={{
              position: 'absolute',
              left: '16px',
              top: '50%',
              transform: 'translateY(-50%)',
              pointerEvents: 'none'
            }}>
              <svg style={{
                width: '18px',
                height: '18px',
                color: 'rgba(203, 213, 225, 0.5)'
              }} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="m21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
              </svg>
            </div>
            <input
              type="text"
              placeholder="Search symbols..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
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
                e.target.style.borderColor = 'rgba(139, 92, 246, 0.5)';
                e.target.style.boxShadow = '0 0 20px rgba(139, 92, 246, 0.2)';
              }}
              onBlur={(e) => {
                e.target.style.borderColor = 'rgba(255, 255, 255, 0.2)';
                e.target.style.boxShadow = 'none';
              }}
            />
          </div>
        )}
      </div>

      {/* Market Summary Stats */}
      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
        gap: '20px',
        marginBottom: '32px'
      }}>
        <div style={{
          background: 'linear-gradient(135deg, rgba(139, 92, 246, 0.1), rgba(139, 92, 246, 0.05))',
          backdropFilter: 'blur(20px)',
          border: '1px solid rgba(139, 92, 246, 0.2)',
          borderRadius: '16px',
          padding: '20px',
          textAlign: 'center',
          boxShadow: '0 10px 30px rgba(139, 92, 246, 0.1)'
        }}>
          <div style={{
            fontSize: '14px',
            color: '#8B5CF6',
            fontWeight: 600,
            marginBottom: '8px'
          }}>Total Symbols</div>
          <div style={{
            fontSize: '28px',
            fontWeight: 700,
            color: 'white'
          }}>
            {filteredData.length}
          </div>
        </div>
        
        <div style={{
          background: 'linear-gradient(135deg, rgba(16, 185, 129, 0.1), rgba(16, 185, 129, 0.05))',
          backdropFilter: 'blur(20px)',
          border: '1px solid rgba(16, 185, 129, 0.2)',
          borderRadius: '16px',
          padding: '20px',
          textAlign: 'center',
          boxShadow: '0 10px 30px rgba(16, 185, 129, 0.1)'
        }}>
          <div style={{
            fontSize: '14px',
            color: '#10B981',
            fontWeight: 600,
            marginBottom: '8px'
          }}>Gainers</div>
          <div style={{
            fontSize: '28px',
            fontWeight: 700,
            color: 'white'
          }}>
            {filteredData.filter(item => item.changePercent > 0).length}
          </div>
        </div>
        
        <div style={{
          background: 'linear-gradient(135deg, rgba(239, 68, 68, 0.1), rgba(239, 68, 68, 0.05))',
          backdropFilter: 'blur(20px)',
          border: '1px solid rgba(239, 68, 68, 0.2)',
          borderRadius: '16px',
          padding: '20px',
          textAlign: 'center',
          boxShadow: '0 10px 30px rgba(239, 68, 68, 0.1)'
        }}>
          <div style={{
            fontSize: '14px',
            color: '#EF4444',
            fontWeight: 600,
            marginBottom: '8px'
          }}>Losers</div>
          <div style={{
            fontSize: '28px',
            fontWeight: 700,
            color: 'white'
          }}>
            {filteredData.filter(item => item.changePercent < 0).length}
          </div>
        </div>
      </div>

      {/* Market Cards Grid */}
      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(350px, 1fr))',
        gap: '24px'
      }}>
        {filteredData.map((marketData, index) => (
          <div
            key={marketData.symbol}
            style={{
              animation: `fadeInUp 0.6s cubic-bezier(0.16, 1, 0.3, 1) ${index * 0.1}s both`
            }}
          >
            <MarketCard
              marketData={marketData}
              onClick={() => handleSymbolClick(marketData.symbol)}
              isSelected={selectedSymbol === marketData.symbol}
            />
          </div>
        ))}
      </div>

      {/* Loading indicator for additional data */}
      {isLoading && filteredData.length > 0 && (
        <div style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          padding: '20px 0'
        }}>
          <LoadingSpinner size="sm" />
          <span style={{
            marginLeft: '12px',
            fontSize: '16px',
            color: 'rgba(203, 213, 225, 0.8)'
          }}>Updating market data...</span>
        </div>
      )}

      {/* Show more button if limited */}
      {maxItems && filteredData.length === maxItems && watchlistSymbols.length > maxItems && (
        <div style={{ textAlign: 'center' }}>
          <button
            onClick={() => {/* Handle show more */}}
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '8px',
              padding: '16px 32px',
              background: 'linear-gradient(135deg, rgba(139, 92, 246, 0.1), rgba(139, 92, 246, 0.05))',
              backdropFilter: 'blur(20px)',
              border: '1px solid rgba(139, 92, 246, 0.3)',
              borderRadius: '16px',
              color: '#8B5CF6',
              fontSize: '16px',
              fontWeight: 600,
              cursor: 'pointer',
              transition: 'all 0.3s ease',
              outline: 'none'
            }}
            onMouseEnter={(e) => {
              e.currentTarget.style.background = 'linear-gradient(135deg, #8B5CF6, #7C3AED)';
              e.currentTarget.style.color = 'white';
              e.currentTarget.style.transform = 'translateY(-2px)';
              e.currentTarget.style.boxShadow = '0 10px 30px rgba(139, 92, 246, 0.3)';
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.background = 'linear-gradient(135deg, rgba(139, 92, 246, 0.1), rgba(139, 92, 246, 0.05))';
              e.currentTarget.style.color = '#8B5CF6';
              e.currentTarget.style.transform = 'translateY(0)';
              e.currentTarget.style.boxShadow = 'none';
            }}
          >
            View All Symbols
            <svg style={{
              width: '18px',
              height: '18px'
            }} fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="m9 5l7 7-7 7" />
            </svg>
          </button>
        </div>
      )}
      <style>{`
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
        
        input::placeholder {
          color: rgba(203, 213, 225, 0.5);
        }
      `}</style>
    </div>
  );
};

export default MarketOverview;