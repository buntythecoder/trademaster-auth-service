import React, { useState } from 'react'
import { 
  TrendingUp, TrendingDown, DollarSign, Activity, 
  Plus, Minus, BarChart3, Clock, AlertTriangle, X, 
  Zap, Target, Shield, ArrowUpRight, ArrowDownRight,
  Users, Star, Award
} from 'lucide-react'
import { useTrading } from '../../hooks/useTrading'
import type { PlaceOrderRequest } from '../../types'
import LoadingSpinner from '../common/LoadingSpinner'
import { toast } from 'react-hot-toast'

const TradingInterface: React.FC = () => {
  const {
    positions,
    orders,
    portfolios,
    selectedPortfolioId,
    setSelectedPortfolioId,
    account,
    loading,
    error,
    placeOrder,
    cancelOrder,
    closePosition
  } = useTrading()
  
  // Trading state
  const [selectedSymbol, setSelectedSymbol] = useState('RELIANCE')
  const [orderType, setOrderType] = useState<'MARKET' | 'LIMIT'>('MARKET')
  const [side, setSide] = useState<'BUY' | 'SELL'>('BUY')
  const [quantity, setQuantity] = useState<number>(0)
  const [price, setPrice] = useState<number>(0)
  const [orderLoading, setOrderLoading] = useState(false)

  const handlePlaceOrder = async () => {
    if (!selectedPortfolioId) {
      toast.error('Please select a portfolio first')
      return
    }

    if (quantity <= 0) {
      toast.error('Please enter a valid quantity')
      return
    }

    if (orderType === 'LIMIT' && price <= 0) {
      toast.error('Please enter a valid price for limit orders')
      return
    }

    setOrderLoading(true)
    
    try {
      const orderRequest: PlaceOrderRequest = {
        symbol: selectedSymbol,
        quantity: quantity,
        orderType: orderType,
        side: side,
        price: orderType === 'LIMIT' ? price : undefined,
        timeInForce: 'DAY'
      }
      
      await placeOrder(orderRequest)
      
      // Reset form after successful order
      setQuantity(0)
      setPrice(0)
      
      toast.success(`${side} order placed successfully for ${selectedSymbol}`)
    } catch (err: any) {
      toast.error(err.message || 'Failed to place order')
    } finally {
      setOrderLoading(false)
    }
  }

  const handleCancelOrder = async (orderId: string) => {
    try {
      await cancelOrder(orderId)
      toast.success('Order cancelled successfully')
    } catch (err: any) {
      toast.error(err.message || 'Failed to cancel order')
    }
  }

  const handleClosePosition = async (positionId: string, symbol: string) => {
    try {
      await closePosition(positionId)
      toast.success(`Position closed successfully for ${symbol}`)
    } catch (err: any) {
      toast.error(err.message || 'Failed to close position')
    }
  }

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'FILLED': return { color: '#10B981', bg: 'rgba(16, 185, 129, 0.1)' }
      case 'PENDING': return { color: '#F59E0B', bg: 'rgba(245, 158, 11, 0.1)' }
      case 'CANCELLED': return { color: '#EF4444', bg: 'rgba(239, 68, 68, 0.1)' }
      case 'PARTIALLY_FILLED': return { color: '#8B5CF6', bg: 'rgba(139, 92, 246, 0.1)' }
      default: return { color: '#6B7280', bg: 'rgba(107, 114, 128, 0.1)' }
    }
  }

  const getPnlColor = (pnl: number) => {
    return pnl >= 0 ? '#10B981' : '#EF4444'
  }

  // Premium Card Component
  const PremiumCard: React.FC<{ children: React.ReactNode; className?: string }> = ({ children, className = '' }) => (
    <div style={{
      background: 'linear-gradient(135deg, rgba(255, 255, 255, 0.1) 0%, rgba(255, 255, 255, 0.05) 100%)',
      backdropFilter: 'blur(30px)',
      border: '1px solid rgba(255, 255, 255, 0.2)',
      borderRadius: '20px',
      boxShadow: '0 20px 60px rgba(0, 0, 0, 0.1), inset 0 1px 0 rgba(255, 255, 255, 0.1)',
      overflow: 'hidden'
    }} className={className}>
      {children}
    </div>
  )

  if (loading && !positions.length && !orders.length) {
    return (
      <div style={{
        minHeight: '100vh',
        background: 'radial-gradient(ellipse at top, #0f0f23 0%, #1a0b2e 25%, #0a0a0a 100%)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center'
      }}>
        <LoadingSpinner />
      </div>
    )
  }

  return (
    <div style={{
      minHeight: '100vh',
      background: 'radial-gradient(ellipse at top, #0f0f23 0%, #1a0b2e 25%, #0a0a0a 100%)',
      padding: '20px',
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
        maxWidth: '1600px',
        margin: '0 auto',
        position: 'relative',
        zIndex: 2
      }}>
        {/* Header */}
        <div style={{ marginBottom: '32px' }}>
          <PremiumCard>
            <div style={{ padding: '24px' }}>
              <div style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                marginBottom: '24px'
              }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '24px' }}>
                  <div>
                    <h1 style={{
                      fontSize: 'clamp(1.8rem, 4vw, 2.5rem)',
                      fontWeight: 800,
                      color: 'white',
                      marginBottom: '8px',
                      background: 'linear-gradient(135deg, #8B5CF6, #06B6D4, #10B981)',
                      WebkitBackgroundClip: 'text',
                      backgroundClip: 'text',
                      WebkitTextFillColor: 'transparent',
                      backgroundSize: '200% 200%',
                      animation: 'gradientMove 4s ease-in-out infinite'
                    }}>
                      Trading Terminal
                    </h1>
                    <p style={{
                      color: 'rgba(203, 213, 225, 0.8)',
                      fontSize: '16px',
                      margin: 0
                    }}>
                      Advanced AI-powered trading interface
                    </p>
                  </div>
                  
                  {/* Portfolio Selector */}
                  <div style={{
                    background: 'rgba(255, 255, 255, 0.1)',
                    borderRadius: '12px',
                    padding: '16px',
                    border: '1px solid rgba(255, 255, 255, 0.2)'
                  }}>
                    <label style={{
                      color: 'rgba(203, 213, 225, 0.8)',
                      fontSize: '12px',
                      fontWeight: 500,
                      display: 'block',
                      marginBottom: '8px'
                    }}>Portfolio</label>
                    <select 
                      value={selectedPortfolioId || ''}
                      onChange={(e) => setSelectedPortfolioId(e.target.value || null)}
                      style={{
                        background: 'rgba(255, 255, 255, 0.1)',
                        border: '1px solid rgba(255, 255, 255, 0.2)',
                        borderRadius: '8px',
                        padding: '8px 12px',
                        color: 'white',
                        fontSize: '14px',
                        fontWeight: 600,
                        outline: 'none',
                        cursor: 'pointer'
                      }}
                    >
                      <option value="" style={{ background: '#0f0f23', color: 'white' }}>Select Portfolio</option>
                      {portfolios.map((portfolio) => (
                        <option key={portfolio.id} value={portfolio.id} style={{ background: '#0f0f23', color: 'white' }}>
                          {portfolio.name}
                        </option>
                      ))}
                    </select>
                  </div>
                </div>
                
                <div style={{ display: 'flex', alignItems: 'center', gap: '20px' }}>
                  <div style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '8px',
                    padding: '8px 16px',
                    background: 'rgba(16, 185, 129, 0.2)',
                    borderRadius: '12px',
                    border: '1px solid rgba(16, 185, 129, 0.3)'
                  }}>
                    <Activity style={{ width: '16px', height: '16px', color: '#10B981' }} />
                    <span style={{ color: '#10B981', fontSize: '14px', fontWeight: 600 }}>Market Open</span>
                  </div>
                  <div style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '6px',
                    color: 'rgba(203, 213, 225, 0.8)',
                    fontSize: '14px'
                  }}>
                    <Clock style={{ width: '14px', height: '14px' }} />
                    <span>Last Updated: {new Date().toLocaleTimeString()}</span>
                  </div>
                </div>
              </div>

              {/* Account Summary */}
              {account && (
                <div style={{
                  display: 'grid',
                  gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
                  gap: '16px',
                  paddingTop: '20px',
                  borderTop: '1px solid rgba(255, 255, 255, 0.1)'
                }}>
                  {[
                    { label: 'Account Value', value: `₹${account.accountValue.toLocaleString()}`, icon: DollarSign, color: '#8B5CF6' },
                    { label: 'Buying Power', value: `₹${account.buyingPower.toLocaleString()}`, icon: Zap, color: '#06B6D4' },
                    { label: 'Day P&L', value: `₹${account.dayPnl.toLocaleString()}`, icon: account.dayPnl >= 0 ? TrendingUp : TrendingDown, color: account.dayPnl >= 0 ? '#10B981' : '#EF4444' },
                    { label: 'Total P&L', value: `₹${account.totalPnl.toLocaleString()}`, icon: account.totalPnl >= 0 ? ArrowUpRight : ArrowDownRight, color: account.totalPnl >= 0 ? '#10B981' : '#EF4444' },
                    { label: 'Cash', value: `₹${account.cash.toLocaleString()}`, icon: Shield, color: '#F59E0B' },
                  ].map((item, index) => (
                    <div key={index} style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: '12px',
                      padding: '12px',
                      background: 'rgba(255, 255, 255, 0.05)',
                      borderRadius: '12px',
                      border: '1px solid rgba(255, 255, 255, 0.1)'
                    }}>
                      <div style={{
                        width: '40px',
                        height: '40px',
                        background: `linear-gradient(135deg, ${item.color}, ${item.color}dd)`,
                        borderRadius: '10px',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center'
                      }}>
                        <item.icon style={{ width: '20px', height: '20px', color: 'white' }} />
                      </div>
                      <div>
                        <div style={{
                          color: 'rgba(203, 213, 225, 0.8)',
                          fontSize: '12px',
                          fontWeight: 500,
                          marginBottom: '2px'
                        }}>{item.label}</div>
                        <div style={{
                          color: 'white',
                          fontSize: '16px',
                          fontWeight: 700
                        }}>{item.value}</div>
                      </div>
                    </div>
                  ))}
                </div>
              )}

              {error && (
                <div style={{
                  marginTop: '20px',
                  padding: '16px',
                  background: 'rgba(239, 68, 68, 0.1)',
                  border: '1px solid rgba(239, 68, 68, 0.3)',
                  borderRadius: '12px',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '12px'
                }}>
                  <AlertTriangle style={{ width: '20px', height: '20px', color: '#EF4444' }} />
                  <span style={{ color: '#EF4444', fontSize: '14px', fontWeight: 500 }}>{error}</span>
                </div>
              )}
            </div>
          </PremiumCard>
        </div>

        <div style={{
          display: 'grid',
          gridTemplateColumns: '400px 1fr',
          gap: '32px',
          alignItems: 'start'
        }}>
          {/* Order Entry Panel */}
          <div>
            <PremiumCard>
              <div style={{ padding: '28px' }}>
                <h2 style={{
                  color: 'white',
                  fontSize: '20px',
                  fontWeight: 700,
                  marginBottom: '24px',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '8px'
                }}>
                  <Target style={{ width: '20px', height: '20px', color: '#8B5CF6' }} />
                  Place Order
                </h2>
                
                {/* Symbol Selection */}
                <div style={{ marginBottom: '20px' }}>
                  <label style={{
                    color: 'rgba(203, 213, 225, 0.8)',
                    fontSize: '14px',
                    fontWeight: 500,
                    display: 'block',
                    marginBottom: '8px'
                  }}>Symbol</label>
                  <select 
                    value={selectedSymbol}
                    onChange={(e) => setSelectedSymbol(e.target.value)}
                    style={{
                      width: '100%',
                      padding: '12px 16px',
                      background: 'rgba(255, 255, 255, 0.1)',
                      border: '1px solid rgba(255, 255, 255, 0.2)',
                      borderRadius: '12px',
                      color: 'white',
                      fontSize: '16px',
                      fontWeight: 600,
                      outline: 'none',
                      cursor: 'pointer'
                    }}
                  >
                    <option value="RELIANCE" style={{ background: '#0f0f23', color: 'white' }}>RELIANCE</option>
                    <option value="TCS" style={{ background: '#0f0f23', color: 'white' }}>TCS</option>
                    <option value="HDFC" style={{ background: '#0f0f23', color: 'white' }}>HDFC</option>
                    <option value="INFY" style={{ background: '#0f0f23', color: 'white' }}>INFY</option>
                    <option value="ICICIBANK" style={{ background: '#0f0f23', color: 'white' }}>ICICIBANK</option>
                  </select>
                </div>

                {/* Buy/Sell Toggle */}
                <div style={{ marginBottom: '20px' }}>
                  <div style={{
                    display: 'flex',
                    borderRadius: '12px',
                    overflow: 'hidden',
                    border: '1px solid rgba(255, 255, 255, 0.2)'
                  }}>
                    <button
                      onClick={() => setSide('BUY')}
                      style={{
                        flex: 1,
                        padding: '12px',
                        fontSize: '16px',
                        fontWeight: 700,
                        transition: 'all 0.3s ease',
                        border: 'none',
                        cursor: 'pointer',
                        ...(side === 'BUY' 
                          ? {
                              background: 'linear-gradient(135deg, #10B981, #059669)',
                              color: 'white'
                            }
                          : {
                              background: 'rgba(255, 255, 255, 0.05)',
                              color: 'rgba(203, 213, 225, 0.8)'
                            }
                        )
                      }}
                    >
                      BUY
                    </button>
                    <button
                      onClick={() => setSide('SELL')}
                      style={{
                        flex: 1,
                        padding: '12px',
                        fontSize: '16px',
                        fontWeight: 700,
                        transition: 'all 0.3s ease',
                        border: 'none',
                        cursor: 'pointer',
                        ...(side === 'SELL' 
                          ? {
                              background: 'linear-gradient(135deg, #EF4444, #DC2626)',
                              color: 'white'
                            }
                          : {
                              background: 'rgba(255, 255, 255, 0.05)',
                              color: 'rgba(203, 213, 225, 0.8)'
                            }
                        )
                      }}
                    >
                      SELL
                    </button>
                  </div>
                </div>

                {/* Order Type */}
                <div style={{ marginBottom: '20px' }}>
                  <label style={{
                    color: 'rgba(203, 213, 225, 0.8)',
                    fontSize: '14px',
                    fontWeight: 500,
                    display: 'block',
                    marginBottom: '8px'
                  }}>Order Type</label>
                  <div style={{ display: 'flex', gap: '8px' }}>
                    {['MARKET', 'LIMIT'].map((type) => (
                      <button
                        key={type}
                        onClick={() => setOrderType(type as 'MARKET' | 'LIMIT')}
                        style={{
                          flex: 1,
                          padding: '12px',
                          fontSize: '14px',
                          fontWeight: 600,
                          borderRadius: '8px',
                          border: 'none',
                          cursor: 'pointer',
                          transition: 'all 0.3s ease',
                          ...(orderType === type 
                            ? {
                                background: 'linear-gradient(135deg, #8B5CF6, #7C3AED)',
                                color: 'white'
                              }
                            : {
                                background: 'rgba(255, 255, 255, 0.1)',
                                color: 'rgba(203, 213, 225, 0.8)'
                              }
                          )
                        }}
                      >
                        {type}
                      </button>
                    ))}
                  </div>
                </div>

                {/* Quantity */}
                <div style={{ marginBottom: '20px' }}>
                  <label style={{
                    color: 'rgba(203, 213, 225, 0.8)',
                    fontSize: '14px',
                    fontWeight: 500,
                    display: 'block',
                    marginBottom: '8px'
                  }}>Quantity</label>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <button
                      onClick={() => setQuantity(Math.max(0, quantity - 1))}
                      style={{
                        width: '40px',
                        height: '40px',
                        borderRadius: '8px',
                        background: 'rgba(255, 255, 255, 0.1)',
                        border: '1px solid rgba(255, 255, 255, 0.2)',
                        color: 'white',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        cursor: 'pointer',
                        transition: 'all 0.3s ease'
                      }}
                    >
                      <Minus style={{ width: '16px', height: '16px' }} />
                    </button>
                    <input
                      type="number"
                      value={quantity}
                      onChange={(e) => setQuantity(Number(e.target.value))}
                      style={{
                        flex: 1,
                        padding: '12px',
                        background: 'rgba(255, 255, 255, 0.1)',
                        border: '1px solid rgba(255, 255, 255, 0.2)',
                        borderRadius: '8px',
                        color: 'white',
                        fontSize: '16px',
                        textAlign: 'center',
                        outline: 'none'
                      }}
                      min="0"
                    />
                    <button
                      onClick={() => setQuantity(quantity + 1)}
                      style={{
                        width: '40px',
                        height: '40px',
                        borderRadius: '8px',
                        background: 'rgba(255, 255, 255, 0.1)',
                        border: '1px solid rgba(255, 255, 255, 0.2)',
                        color: 'white',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        cursor: 'pointer',
                        transition: 'all 0.3s ease'
                      }}
                    >
                      <Plus style={{ width: '16px', height: '16px' }} />
                    </button>
                  </div>
                </div>

                {/* Price (for limit orders) */}
                {orderType === 'LIMIT' && (
                  <div style={{ marginBottom: '20px' }}>
                    <label style={{
                      color: 'rgba(203, 213, 225, 0.8)',
                      fontSize: '14px',
                      fontWeight: 500,
                      display: 'block',
                      marginBottom: '8px'
                    }}>Price (₹)</label>
                    <input
                      type="number"
                      value={price}
                      onChange={(e) => setPrice(Number(e.target.value))}
                      style={{
                        width: '100%',
                        padding: '12px 16px',
                        background: 'rgba(255, 255, 255, 0.1)',
                        border: '1px solid rgba(255, 255, 255, 0.2)',
                        borderRadius: '12px',
                        color: 'white',
                        fontSize: '16px',
                        outline: 'none'
                      }}
                      step="0.05"
                      min="0"
                    />
                  </div>
                )}

                {/* Order Summary */}
                <div style={{
                  marginBottom: '24px',
                  padding: '20px',
                  background: 'rgba(255, 255, 255, 0.05)',
                  borderRadius: '12px',
                  border: '1px solid rgba(255, 255, 255, 0.1)'
                }}>
                  <h3 style={{
                    color: 'white',
                    fontSize: '16px',
                    fontWeight: 600,
                    marginBottom: '16px'
                  }}>Order Summary</h3>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                    {[
                      { label: 'Symbol', value: selectedSymbol },
                      { label: 'Side', value: side, color: side === 'BUY' ? '#10B981' : '#EF4444' },
                      { label: 'Quantity', value: quantity.toString() },
                      { label: 'Type', value: orderType },
                      ...(orderType === 'LIMIT' ? [{ label: 'Price', value: `₹${price.toFixed(2)}` }] : [])
                    ].map((item, index) => (
                      <div key={index} style={{
                        display: 'flex',
                        justifyContent: 'space-between',
                        alignItems: 'center'
                      }}>
                        <span style={{
                          color: 'rgba(203, 213, 225, 0.8)',
                          fontSize: '14px'
                        }}>{item.label}:</span>
                        <span style={{
                          color: item.color || 'white',
                          fontSize: '14px',
                          fontWeight: 600
                        }}>{item.value}</span>
                      </div>
                    ))}
                  </div>
                </div>

                {/* Place Order Button */}
                <button
                  onClick={handlePlaceOrder}
                  disabled={orderLoading || quantity === 0 || (orderType === 'LIMIT' && price === 0) || !selectedPortfolioId}
                  style={{
                    width: '100%',
                    padding: '16px',
                    borderRadius: '12px',
                    border: 'none',
                    fontSize: '16px',
                    fontWeight: 700,
                    cursor: 'pointer',
                    transition: 'all 0.3s ease',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    gap: '8px',
                    background: side === 'BUY' 
                      ? 'linear-gradient(135deg, #10B981, #059669)'
                      : 'linear-gradient(135deg, #EF4444, #DC2626)',
                    color: 'white',
                    boxShadow: `0 8px 20px rgba(${side === 'BUY' ? '16, 185, 129' : '239, 68, 68'}, 0.3)`,
                    opacity: (orderLoading || quantity === 0 || (orderType === 'LIMIT' && price === 0) || !selectedPortfolioId) ? 0.5 : 1
                  }}
                >
                  {orderLoading ? (
                    <>
                      <LoadingSpinner size="sm" />
                      <span>Placing Order...</span>
                    </>
                  ) : (
                    <>
                      <Zap style={{ width: '20px', height: '20px' }} />
                      <span>Place {side} Order</span>
                    </>
                  )}
                </button>
              </div>
            </PremiumCard>
          </div>

          {/* Positions and Orders */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '32px' }}>
            {/* Positions */}
            <PremiumCard>
              <div style={{ padding: '28px' }}>
                <h2 style={{
                  color: 'white',
                  fontSize: '20px',
                  fontWeight: 700,
                  marginBottom: '24px',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '8px'
                }}>
                  <BarChart3 style={{ width: '20px', height: '20px', color: '#10B981' }} />
                  Open Positions
                </h2>
                
                {positions.length === 0 ? (
                  <div style={{
                    textAlign: 'center',
                    padding: '60px 20px',
                    color: 'rgba(203, 213, 225, 0.6)'
                  }}>
                    <BarChart3 style={{ width: '48px', height: '48px', margin: '0 auto 16px', opacity: 0.5 }} />
                    <p style={{ fontSize: '16px', fontWeight: 500, marginBottom: '8px' }}>No positions found</p>
                    <p style={{ fontSize: '14px' }}>Start trading to see your positions here</p>
                  </div>
                ) : (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                    {positions.map((position) => (
                      <div key={position.id} style={{
                        padding: '20px',
                        background: 'rgba(255, 255, 255, 0.05)',
                        borderRadius: '12px',
                        border: '1px solid rgba(255, 255, 255, 0.1)',
                        transition: 'all 0.3s ease'
                      }}>
                        <div style={{
                          display: 'grid',
                          gridTemplateColumns: '1fr 1fr 1fr 1fr 1fr auto',
                          gap: '16px',
                          alignItems: 'center'
                        }}>
                          <div>
                            <div style={{
                              display: 'flex',
                              alignItems: 'center',
                              gap: '8px',
                              marginBottom: '4px'
                            }}>
                              <span style={{ color: 'white', fontSize: '16px', fontWeight: 700 }}>{position.symbol}</span>
                              {position.side === 'LONG' ? (
                                <TrendingUp style={{ width: '16px', height: '16px', color: '#10B981' }} />
                              ) : (
                                <TrendingDown style={{ width: '16px', height: '16px', color: '#EF4444' }} />
                              )}
                            </div>
                            <div style={{ color: 'rgba(203, 213, 225, 0.8)', fontSize: '12px' }}>
                              {position.quantity} shares
                            </div>
                          </div>
                          
                          <div>
                            <div style={{ color: 'rgba(203, 213, 225, 0.8)', fontSize: '12px', marginBottom: '4px' }}>Avg Price</div>
                            <div style={{ color: 'white', fontSize: '14px', fontWeight: 600 }}>
                              ₹{position.averagePrice?.toFixed(2) || '0.00'}
                            </div>
                          </div>
                          
                          <div>
                            <div style={{ color: 'rgba(203, 213, 225, 0.8)', fontSize: '12px', marginBottom: '4px' }}>Current</div>
                            <div style={{ color: 'white', fontSize: '14px', fontWeight: 600 }}>
                              ₹{position.currentPrice?.toFixed(2) || '0.00'}
                            </div>
                          </div>
                          
                          <div>
                            <div style={{ color: 'rgba(203, 213, 225, 0.8)', fontSize: '12px', marginBottom: '4px' }}>P&L</div>
                            <div style={{
                              color: getPnlColor(position.unrealizedPnl || 0),
                              fontSize: '16px',
                              fontWeight: 700,
                              display: 'flex',
                              alignItems: 'center',
                              gap: '4px'
                            }}>
                              {(position.unrealizedPnl || 0) >= 0 ? (
                                <ArrowUpRight style={{ width: '14px', height: '14px' }} />
                              ) : (
                                <ArrowDownRight style={{ width: '14px', height: '14px' }} />
                              )}
                              ₹{position.unrealizedPnl?.toFixed(2) || '0.00'}
                            </div>
                          </div>
                          
                          <div>
                            <div style={{ color: 'rgba(203, 213, 225, 0.8)', fontSize: '12px', marginBottom: '4px' }}>Market Value</div>
                            <div style={{ color: 'white', fontSize: '14px', fontWeight: 600 }}>
                              ₹{position.marketValue?.toFixed(2) || '0.00'}
                            </div>
                          </div>
                          
                          <button
                            onClick={() => handleClosePosition(position.id, position.symbol)}
                            style={{
                              padding: '8px 16px',
                              background: 'linear-gradient(135deg, #EF4444, #DC2626)',
                              border: 'none',
                              borderRadius: '8px',
                              color: 'white',
                              fontSize: '12px',
                              fontWeight: 600,
                              cursor: 'pointer',
                              transition: 'all 0.3s ease'
                            }}
                          >
                            Close
                          </button>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </PremiumCard>

            {/* Orders */}
            <PremiumCard>
              <div style={{ padding: '28px' }}>
                <h2 style={{
                  color: 'white',
                  fontSize: '20px',
                  fontWeight: 700,
                  marginBottom: '24px',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '8px'
                }}>
                  <Clock style={{ width: '20px', height: '20px', color: '#F59E0B' }} />
                  Recent Orders
                </h2>
                
                {orders.length === 0 ? (
                  <div style={{
                    textAlign: 'center',
                    padding: '60px 20px',
                    color: 'rgba(203, 213, 225, 0.6)'
                  }}>
                    <Clock style={{ width: '48px', height: '48px', margin: '0 auto 16px', opacity: 0.5 }} />
                    <p style={{ fontSize: '16px', fontWeight: 500, marginBottom: '8px' }}>No orders found</p>
                    <p style={{ fontSize: '14px' }}>Place an order to see it here</p>
                  </div>
                ) : (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                    {orders.map((order) => {
                      const statusStyle = getStatusColor(order.status)
                      return (
                        <div key={order.id} style={{
                          padding: '20px',
                          background: 'rgba(255, 255, 255, 0.05)',
                          borderRadius: '12px',
                          border: '1px solid rgba(255, 255, 255, 0.1)',
                          transition: 'all 0.3s ease'
                        }}>
                          <div style={{
                            display: 'grid',
                            gridTemplateColumns: '1fr 1fr 1fr 1fr 1fr 1fr auto',
                            gap: '16px',
                            alignItems: 'center'
                          }}>
                            <div>
                              <div style={{ color: 'white', fontSize: '16px', fontWeight: 700, marginBottom: '4px' }}>
                                {order.symbol}
                              </div>
                              <div style={{
                                padding: '2px 8px',
                                borderRadius: '4px',
                                fontSize: '10px',
                                fontWeight: 600,
                                background: order.side === 'BUY' ? 'rgba(16, 185, 129, 0.2)' : 'rgba(239, 68, 68, 0.2)',
                                color: order.side === 'BUY' ? '#10B981' : '#EF4444',
                                display: 'inline-block'
                              }}>
                                {order.side}
                              </div>
                            </div>
                            
                            <div>
                              <div style={{ color: 'rgba(203, 213, 225, 0.8)', fontSize: '12px', marginBottom: '4px' }}>Quantity</div>
                              <div style={{ color: 'white', fontSize: '14px', fontWeight: 600 }}>{order.quantity}</div>
                            </div>
                            
                            <div>
                              <div style={{ color: 'rgba(203, 213, 225, 0.8)', fontSize: '12px', marginBottom: '4px' }}>Price</div>
                              <div style={{ color: 'white', fontSize: '14px', fontWeight: 600 }}>
                                ₹{order.price?.toFixed(2) || '0.00'}
                              </div>
                            </div>
                            
                            <div>
                              <div style={{ color: 'rgba(203, 213, 225, 0.8)', fontSize: '12px', marginBottom: '4px' }}>Type</div>
                              <div style={{ color: 'white', fontSize: '14px', fontWeight: 600 }}>{order.orderType}</div>
                            </div>
                            
                            <div>
                              <div style={{ color: 'rgba(203, 213, 225, 0.8)', fontSize: '12px', marginBottom: '4px' }}>Status</div>
                              <div style={{
                                padding: '4px 8px',
                                borderRadius: '6px',
                                fontSize: '12px',
                                fontWeight: 600,
                                background: statusStyle.bg,
                                color: statusStyle.color,
                                display: 'inline-block'
                              }}>
                                {order.status}
                              </div>
                            </div>
                            
                            <div>
                              <div style={{ color: 'rgba(203, 213, 225, 0.8)', fontSize: '12px', marginBottom: '4px' }}>Time</div>
                              <div style={{ color: 'rgba(203, 213, 225, 0.8)', fontSize: '12px' }}>
                                {new Date(order.createdAt || order.timestamp || Date.now()).toLocaleTimeString()}
                              </div>
                            </div>
                            
                            <div>
                              {(order.status === 'PENDING' || order.status === 'PARTIALLY_FILLED') && (
                                <button
                                  onClick={() => handleCancelOrder(order.id)}
                                  style={{
                                    padding: '8px 12px',
                                    background: 'linear-gradient(135deg, #F59E0B, #D97706)',
                                    border: 'none',
                                    borderRadius: '8px',
                                    color: 'white',
                                    fontSize: '12px',
                                    fontWeight: 600,
                                    cursor: 'pointer',
                                    transition: 'all 0.3s ease',
                                    display: 'flex',
                                    alignItems: 'center',
                                    gap: '4px'
                                  }}
                                >
                                  <X style={{ width: '12px', height: '12px' }} />
                                  Cancel
                                </button>
                              )}
                            </div>
                          </div>
                        </div>
                      )
                    })}
                  </div>
                )}
              </div>
            </PremiumCard>
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
  )
}

export default TradingInterface