import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { Building2, AlertCircle, CheckCircle, Activity } from 'lucide-react';
import { OrderForm } from './OrderForm/OrderForm';
import { MultiBrokerService, BrokerConnection } from '../../services/brokerService';
import { TradingServiceFactory } from '../../services/tradingProfiles';

interface MultiBrokerOrderFormProps {
  symbol: string;
  companyName?: string;
  currentPrice: number;
  className?: string;
}

interface OrderRequest {
  symbol: string;
  side: 'BUY' | 'SELL';
  orderType: 'MARKET' | 'LIMIT' | 'STOP_LOSS' | 'BRACKET';
  quantity: number;
  price?: number;
  stopLoss?: number;
  target?: number;
  validity: 'DAY' | 'IOC' | 'GTD';
  brokerAccount: string;
}

interface BrokerAccount {
  id: string;
  brokerName: 'ZERODHA' | 'UPSTOX' | 'ANGEL' | 'PAYTM_MONEY';
  displayName: string;
  balance: number;
  marginAvailable: number;
  status: 'ACTIVE' | 'INACTIVE' | 'MAINTENANCE';
}

export const MultiBrokerOrderForm: React.FC<MultiBrokerOrderFormProps> = ({
  symbol,
  companyName,
  currentPrice,
  className = ''
}) => {
  const [connectedBrokers, setConnectedBrokers] = useState<BrokerConnection[]>([]);
  const [brokerAccounts, setBrokerAccounts] = useState<BrokerAccount[]>([]);
  const [loading, setLoading] = useState(true);
  const [orderInProgress, setOrderInProgress] = useState(false);

  useEffect(() => {
    fetchConnectedBrokers();
  }, []);

  const fetchConnectedBrokers = async () => {
    try {
      setLoading(true);
      const brokers = MultiBrokerService.getConnectedBrokers();
      setConnectedBrokers(brokers);

      // Convert to OrderForm's expected format
      const accounts: BrokerAccount[] = brokers
        .filter(broker => broker.status === 'connected')
        .map(broker => ({
          id: broker.id,
          brokerName: mapBrokerType(broker.brokerType),
          displayName: broker.displayName,
          balance: broker.balance?.totalBalance || 0,
          marginAvailable: broker.balance?.availableMargin || 0,
          status: 'ACTIVE' as const
        }));

      setBrokerAccounts(accounts);
    } catch (error) {
      console.error('Failed to fetch connected brokers:', error);
    } finally {
      setLoading(false);
    }
  };

  const mapBrokerType = (brokerType: string): 'ZERODHA' | 'UPSTOX' | 'ANGEL' | 'PAYTM_MONEY' => {
    switch (brokerType) {
      case 'zerodha': return 'ZERODHA';
      case 'upstox': return 'UPSTOX';
      case 'angel_one': return 'ANGEL';
      case 'groww': return 'PAYTM_MONEY'; // Fallback
      default: return 'ZERODHA';
    }
  };

  const handleOrderSubmit = async (order: OrderRequest) => {
    try {
      setOrderInProgress(true);
      console.log('Submitting order through MultiBrokerService:', order);

      // Route order through the MultiBrokerService
      const result = await MultiBrokerService.placeOrder({
        symbol: order.symbol,
        type: order.side,
        orderType: order.orderType,
        quantity: order.quantity,
        price: order.price,
        stopLoss: order.stopLoss,
        target: order.target,
        brokerId: order.brokerAccount
      }, order.brokerAccount);

      console.log('Order placed successfully:', result);
      
      // Optionally refresh broker data after order
      await fetchConnectedBrokers();
      
    } catch (error) {
      console.error('Failed to place order:', error);
      throw error;
    } finally {
      setOrderInProgress(false);
    }
  };

  if (loading) {
    return (
      <div className={`glass-card p-6 rounded-2xl ${className}`}>
        <div className="flex items-center justify-center py-8">
          <Activity className="w-6 h-6 text-purple-400 animate-spin mr-3" />
          <span className="text-slate-400">Loading broker connections...</span>
        </div>
      </div>
    );
  }

  if (brokerAccounts.length === 0) {
    return (
      <motion.div
        className={`glass-card p-6 rounded-2xl text-center ${className}`}
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
      >
        <Building2 className="w-16 h-16 text-slate-400 mx-auto mb-4" />
        <h3 className="text-xl font-semibold text-white mb-2">No Connected Brokers</h3>
        <p className="text-slate-400 mb-4">
          Connect your broker accounts to start trading
        </p>
        <button
          onClick={fetchConnectedBrokers}
          className="cyber-button px-6 py-3 rounded-xl font-semibold"
        >
          Refresh Connections
        </button>
      </motion.div>
    );
  }

  // Calculate aggregated balance from all connected brokers
  const totalBalance = brokerAccounts.reduce((sum, account) => sum + account.marginAvailable, 0);
  const maxQuantity = Math.floor(totalBalance / currentPrice);

  return (
    <motion.div
      className={className}
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
    >
      {/* Broker Status Header */}
      <div className="glass-card p-4 rounded-2xl mb-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <Building2 className="w-5 h-5 text-purple-400" />
            <span className="font-semibold text-white">Connected Brokers</span>
          </div>
          <div className="flex items-center space-x-1">
            <CheckCircle className="w-4 h-4 text-green-400" />
            <span className="text-green-400 font-semibold">{brokerAccounts.length}</span>
          </div>
        </div>

        <div className="mt-3 flex flex-wrap gap-2">
          {brokerAccounts.map((account) => (
            <div
              key={account.id}
              className="px-3 py-1 rounded-lg bg-slate-800/50 text-xs"
            >
              <span className="text-white font-medium">{account.brokerName}</span>
              <span className="text-slate-400 ml-2">
                ₹{account.marginAvailable.toLocaleString('en-IN')}
              </span>
            </div>
          ))}
        </div>
      </div>

      {/* Enhanced Order Form */}
      <OrderForm
        symbol={symbol}
        companyName={companyName}
        currentPrice={currentPrice}
        orderTypes={['MARKET', 'LIMIT', 'STOP_LOSS', 'BRACKET']}
        maxQuantity={maxQuantity}
        availableBalance={totalBalance}
        riskLimits={{
          maxPositionSize: maxQuantity,
          maxPortfolioExposure: 100,
          minStopLossPercent: 5,
          maxOrderValue: totalBalance
        }}
        brokerAccounts={brokerAccounts}
        onOrderSubmit={handleOrderSubmit}
        className="glass-card p-6 rounded-2xl"
      />

      {/* Order Status */}
      {orderInProgress && (
        <motion.div
          className="glass-card p-4 rounded-2xl mt-4"
          initial={{ opacity: 0, scale: 0.95 }}
          animate={{ opacity: 1, scale: 1 }}
        >
          <div className="flex items-center space-x-3">
            <Activity className="w-5 h-5 text-purple-400 animate-spin" />
            <span className="text-white font-medium">Processing order through broker...</span>
          </div>
        </motion.div>
      )}
    </motion.div>
  );
};