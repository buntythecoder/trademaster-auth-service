import React, { useState, useEffect, useCallback } from 'react'
import { 
  CreditCard, 
  Smartphone, 
  Building2, 
  Wallet, 
  CheckCircle, 
  XCircle, 
  AlertTriangle, 
  RefreshCw,
  IndianRupee,
  Shield,
  Clock,
  TrendingUp,
  Users,
  Calendar,
  BarChart3,
  PieChart,
  Activity,
  Zap,
  Globe,
  Lock,
  Bell,
  Settings,
  Plus,
  Download,
  Star,
  Crown
} from 'lucide-react'

// Payment Gateway Types
interface PaymentMethod {
  id: string
  name: string
  type: 'card' | 'upi' | 'netbanking' | 'wallet' | 'emi'
  icon: React.ReactNode
  processingFee: number
  convenienceFee: number
  isEnabled: boolean
  supportedCurrencies: string[]
  processingTime: string
  successRate: number
  dailyLimit: number
  monthlyLimit: number
}

interface PaymentGateway {
  id: string
  name: string
  provider: 'razorpay' | 'stripe' | 'payu' | 'ccavenue'
  isActive: boolean
  apiVersion: string
  supportedMethods: string[]
  processingFee: number
  settlementTime: string
  currency: string
  testMode: boolean
  webhookUrl: string
  merchantId: string
}

interface PaymentTransaction {
  id: string
  orderId: string
  amount: number
  currency: string
  status: 'pending' | 'processing' | 'success' | 'failed' | 'cancelled' | 'refunded'
  paymentMethod: string
  gateway: string
  customerEmail: string
  customerPhone: string
  createdAt: Date
  completedAt?: Date
  failureReason?: string
  retryAttempts: number
  refundAmount?: number
  fees: {
    gateway: number
    convenience: number
    gst: number
    total: number
  }
  metadata: {
    subscriptionId?: string
    planId?: string
    userId: string
    description: string
  }
}

interface SubscriptionPlan {
  id: string
  name: string
  description: string
  price: number
  currency: string
  billing: 'monthly' | 'quarterly' | 'yearly'
  features: string[]
  trialDays: number
  isPopular: boolean
  discount?: {
    percentage: number
    validUntil: Date
  }
}

interface PaymentAnalytics {
  totalRevenue: number
  todayRevenue: number
  monthlyRecurring: number
  successRate: number
  avgTransactionValue: number
  totalTransactions: number
  failedTransactions: number
  refundRate: number
  churnRate: number
  customerLifetimeValue: number
  topPaymentMethods: Array<{
    method: string
    percentage: number
    revenue: number
  }>
  revenueByGateway: Array<{
    gateway: string
    revenue: number
    transactions: number
    fees: number
  }>
  monthlyGrowth: number
  conversionRate: number
}

interface PaymentProps {
  onPaymentSuccess?: (transaction: PaymentTransaction) => void
  onPaymentFailure?: (error: string, transaction: PaymentTransaction) => void
  onSubscriptionUpdate?: (subscription: any) => void
}

export default function PaymentGatewayInterface({ 
  onPaymentSuccess, 
  onPaymentFailure,
  onSubscriptionUpdate 
}: PaymentProps) {
  // State Management
  const [activeTab, setActiveTab] = useState<'payments' | 'subscriptions' | 'analytics' | 'settings'>('payments')
  const [selectedGateway, setSelectedGateway] = useState<string>('razorpay')
  const [isLoading, setIsLoading] = useState<boolean>(false)
  const [paymentMethods, setPaymentMethods] = useState<PaymentMethod[]>([])
  const [transactions, setTransactions] = useState<PaymentTransaction[]>([])
  const [analytics, setAnalytics] = useState<PaymentAnalytics | null>(null)
  const [selectedPlan, setSelectedPlan] = useState<SubscriptionPlan | null>(null)
  const [retryQueue, setRetryQueue] = useState<PaymentTransaction[]>([])
  const [webhookLogs, setWebhookLogs] = useState<any[]>([])

  // Mock Data
  const mockPaymentMethods: PaymentMethod[] = [
    {
      id: 'upi',
      name: 'UPI',
      type: 'upi',
      icon: <Smartphone className="w-5 h-5" />,
      processingFee: 0,
      convenienceFee: 0,
      isEnabled: true,
      supportedCurrencies: ['INR'],
      processingTime: '< 1 minute',
      successRate: 97.5,
      dailyLimit: 100000,
      monthlyLimit: 1000000
    },
    {
      id: 'cards',
      name: 'Credit/Debit Cards',
      type: 'card',
      icon: <CreditCard className="w-5 h-5" />,
      processingFee: 2.95,
      convenienceFee: 1.18,
      isEnabled: true,
      supportedCurrencies: ['INR', 'USD'],
      processingTime: '2-3 minutes',
      successRate: 94.2,
      dailyLimit: 200000,
      monthlyLimit: 2000000
    },
    {
      id: 'netbanking',
      name: 'Net Banking',
      type: 'netbanking',
      icon: <Building2 className="w-5 h-5" />,
      processingFee: 2.5,
      convenienceFee: 0,
      isEnabled: true,
      supportedCurrencies: ['INR'],
      processingTime: '3-5 minutes',
      successRate: 91.8,
      dailyLimit: 500000,
      monthlyLimit: 5000000
    },
    {
      id: 'wallets',
      name: 'Digital Wallets',
      type: 'wallet',
      icon: <Wallet className="w-5 h-5" />,
      processingFee: 2.0,
      convenienceFee: 0,
      isEnabled: true,
      supportedCurrencies: ['INR'],
      processingTime: '< 1 minute',
      successRate: 96.1,
      dailyLimit: 50000,
      monthlyLimit: 500000
    }
  ]

  const mockGateways: PaymentGateway[] = [
    {
      id: 'razorpay',
      name: 'Razorpay',
      provider: 'razorpay',
      isActive: true,
      apiVersion: 'v1',
      supportedMethods: ['upi', 'cards', 'netbanking', 'wallets'],
      processingFee: 2.0,
      settlementTime: 'T+2',
      currency: 'INR',
      testMode: false,
      webhookUrl: 'https://api.trademaster.com/webhooks/razorpay',
      merchantId: 'rzp_test_merchant_123'
    },
    {
      id: 'stripe',
      name: 'Stripe',
      provider: 'stripe',
      isActive: true,
      apiVersion: '2023-10-16',
      supportedMethods: ['cards'],
      processingFee: 2.9,
      settlementTime: 'T+7',
      currency: 'USD',
      testMode: false,
      webhookUrl: 'https://api.trademaster.com/webhooks/stripe',
      merchantId: 'acct_stripe_123'
    }
  ]

  const subscriptionPlans: SubscriptionPlan[] = [
    {
      id: 'basic',
      name: 'Basic Plan',
      description: 'Essential trading features for individual traders',
      price: 999,
      currency: 'INR',
      billing: 'monthly',
      features: [
        'Real-time market data',
        'Basic charting tools',
        'Portfolio tracking',
        'Mobile app access',
        'Email support'
      ],
      trialDays: 7,
      isPopular: false
    },
    {
      id: 'professional',
      name: 'Professional Plan',
      description: 'Advanced tools for serious traders',
      price: 2999,
      currency: 'INR',
      billing: 'monthly',
      features: [
        'Everything in Basic',
        'Advanced analytics',
        'Multi-broker integration',
        'Risk management tools',
        'Priority support',
        'Tax optimization'
      ],
      trialDays: 14,
      isPopular: true,
      discount: {
        percentage: 20,
        validUntil: new Date('2024-12-31')
      }
    },
    {
      id: 'enterprise',
      name: 'Enterprise Plan',
      description: 'Complete solution for institutions',
      price: 9999,
      currency: 'INR',
      billing: 'monthly',
      features: [
        'Everything in Professional',
        'White-label solutions',
        'Dedicated account manager',
        'Custom integrations',
        'SLA guarantees',
        'Advanced reporting'
      ],
      trialDays: 30,
      isPopular: false
    }
  ]

  const mockAnalytics: PaymentAnalytics = {
    totalRevenue: 2850000,
    todayRevenue: 45000,
    monthlyRecurring: 180000,
    successRate: 94.7,
    avgTransactionValue: 2650,
    totalTransactions: 1245,
    failedTransactions: 67,
    refundRate: 2.1,
    churnRate: 3.8,
    customerLifetimeValue: 15420,
    topPaymentMethods: [
      { method: 'UPI', percentage: 52.3, revenue: 1490550 },
      { method: 'Cards', percentage: 28.7, revenue: 817950 },
      { method: 'Net Banking', percentage: 12.8, revenue: 364800 },
      { method: 'Wallets', percentage: 6.2, revenue: 176700 }
    ],
    revenueByGateway: [
      { gateway: 'Razorpay', revenue: 2280000, transactions: 998, fees: 45600 },
      { gateway: 'Stripe', revenue: 570000, transactions: 247, fees: 16530 }
    ],
    monthlyGrowth: 18.4,
    conversionRate: 87.3
  }

  // Initialize Data
  useEffect(() => {
    setPaymentMethods(mockPaymentMethods)
    setAnalytics(mockAnalytics)
    generateMockTransactions()
  }, [])

  const generateMockTransactions = useCallback(() => {
    const mockTransactions: PaymentTransaction[] = Array.from({ length: 50 }, (_, i) => ({
      id: `txn_${Date.now()}_${i}`,
      orderId: `ord_${Date.now()}_${i}`,
      amount: Math.floor(Math.random() * 10000) + 500,
      currency: 'INR',
      status: ['success', 'failed', 'pending', 'processing'][Math.floor(Math.random() * 4)] as any,
      paymentMethod: mockPaymentMethods[Math.floor(Math.random() * mockPaymentMethods.length)].name,
      gateway: mockGateways[Math.floor(Math.random() * mockGateways.length)].name,
      customerEmail: `user${i}@example.com`,
      customerPhone: `+919876543${String(i).padStart(3, '0')}`,
      createdAt: new Date(Date.now() - Math.random() * 30 * 24 * 60 * 60 * 1000),
      retryAttempts: Math.floor(Math.random() * 3),
      fees: {
        gateway: 0,
        convenience: 0,
        gst: 0,
        total: 0
      },
      metadata: {
        userId: `user_${i}`,
        description: `TradeMaster subscription payment #${i + 1}`
      }
    }))

    // Calculate fees
    mockTransactions.forEach(txn => {
      const gateway = mockGateways.find(g => g.name === txn.gateway)
      if (gateway) {
        txn.fees.gateway = (txn.amount * gateway.processingFee) / 100
        txn.fees.gst = txn.fees.gateway * 0.18
        txn.fees.total = txn.fees.gateway + txn.fees.convenience + txn.fees.gst
      }
    })

    setTransactions(mockTransactions)
  }, [])

  // Payment Processing Functions
  const initiatePayment = async (amount: number, planId: string, method: string) => {
    setIsLoading(true)
    try {
      // Simulate payment processing
      await new Promise(resolve => setTimeout(resolve, 2000))
      
      const transaction: PaymentTransaction = {
        id: `txn_${Date.now()}`,
        orderId: `ord_${Date.now()}`,
        amount,
        currency: 'INR',
        status: Math.random() > 0.1 ? 'success' : 'failed',
        paymentMethod: method,
        gateway: selectedGateway,
        customerEmail: 'user@example.com',
        customerPhone: '+919876543210',
        createdAt: new Date(),
        retryAttempts: 0,
        fees: {
          gateway: (amount * 2.0) / 100,
          convenience: 0,
          gst: (amount * 2.0 * 0.18) / 100,
          total: (amount * 2.0 * 1.18) / 100
        },
        metadata: {
          planId,
          userId: 'user_123',
          description: `TradeMaster ${planId} subscription`
        }
      }

      if (transaction.status === 'success') {
        onPaymentSuccess?.(transaction)
      } else {
        onPaymentFailure?.('Payment declined by bank', transaction)
      }

      setTransactions(prev => [transaction, ...prev])
    } catch (error) {
      console.error('Payment processing error:', error)
    } finally {
      setIsLoading(false)
    }
  }

  const retryFailedPayment = async (transactionId: string) => {
    const transaction = transactions.find(t => t.id === transactionId)
    if (!transaction) return

    setIsLoading(true)
    try {
      await new Promise(resolve => setTimeout(resolve, 1500))
      
      const updatedTransaction = {
        ...transaction,
        status: Math.random() > 0.3 ? 'success' : 'failed',
        retryAttempts: transaction.retryAttempts + 1,
        completedAt: new Date()
      } as PaymentTransaction

      setTransactions(prev => 
        prev.map(t => t.id === transactionId ? updatedTransaction : t)
      )

      if (updatedTransaction.status === 'success') {
        onPaymentSuccess?.(updatedTransaction)
      }
    } catch (error) {
      console.error('Retry payment error:', error)
    } finally {
      setIsLoading(false)
    }
  }

  // Render Functions
  const renderPaymentMethods = () => (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
      {paymentMethods.map((method) => (
        <div key={method.id} className="bg-gray-800/40 backdrop-blur-sm border border-cyan-500/20 rounded-xl p-4">
          <div className="flex items-center justify-between mb-3">
            <div className="flex items-center space-x-2">
              {method.icon}
              <span className="font-medium text-white">{method.name}</span>
            </div>
            <div className={`w-3 h-3 rounded-full ${method.isEnabled ? 'bg-green-400' : 'bg-red-400'}`} />
          </div>
          
          <div className="space-y-2 text-sm text-gray-300">
            <div className="flex justify-between">
              <span>Success Rate:</span>
              <span className="text-green-400">{method.successRate}%</span>
            </div>
            <div className="flex justify-between">
              <span>Processing Fee:</span>
              <span className="text-yellow-400">{method.processingFee}%</span>
            </div>
            <div className="flex justify-between">
              <span>Processing Time:</span>
              <span className="text-blue-400">{method.processingTime}</span>
            </div>
            <div className="flex justify-between">
              <span>Daily Limit:</span>
              <span className="text-purple-400">₹{(method.dailyLimit / 1000)}K</span>
            </div>
          </div>
        </div>
      ))}
    </div>
  )

  const renderSubscriptionPlans = () => (
    <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
      {subscriptionPlans.map((plan) => (
        <div key={plan.id} className={`relative bg-gray-800/40 backdrop-blur-sm border rounded-xl p-6 ${
          plan.isPopular ? 'border-cyan-400 ring-2 ring-cyan-400/20' : 'border-gray-600'
        }`}>
          {plan.isPopular && (
            <div className="absolute -top-3 left-1/2 transform -translate-x-1/2 bg-gradient-to-r from-cyan-400 to-purple-400 text-black px-4 py-1 rounded-full text-sm font-bold">
              MOST POPULAR
            </div>
          )}
          
          <div className="text-center mb-6">
            <h3 className="text-xl font-bold text-white mb-2">{plan.name}</h3>
            <p className="text-gray-400 text-sm mb-4">{plan.description}</p>
            
            <div className="flex items-center justify-center space-x-1 mb-2">
              <IndianRupee className="w-6 h-6 text-cyan-400" />
              <span className="text-3xl font-bold text-white">{plan.price}</span>
              <span className="text-gray-400">/{plan.billing}</span>
            </div>
            
            {plan.discount && (
              <div className="text-green-400 text-sm">
                {plan.discount.percentage}% OFF - Limited Time!
              </div>
            )}
          </div>
          
          <div className="space-y-3 mb-6">
            {plan.features.map((feature, index) => (
              <div key={index} className="flex items-center space-x-2">
                <CheckCircle className="w-4 h-4 text-green-400 flex-shrink-0" />
                <span className="text-sm text-gray-300">{feature}</span>
              </div>
            ))}
          </div>
          
          <button
            onClick={() => setSelectedPlan(plan)}
            className={`w-full py-3 rounded-lg font-medium transition-all ${
              plan.isPopular
                ? 'bg-gradient-to-r from-cyan-500 to-purple-500 text-black hover:shadow-lg hover:shadow-cyan-500/25'
                : 'bg-gray-700 text-white hover:bg-gray-600'
            }`}
          >
            Choose Plan
          </button>
          
          <div className="text-center mt-3">
            <span className="text-xs text-gray-400">{plan.trialDays} days free trial</span>
          </div>
        </div>
      ))}
    </div>
  )

  const renderAnalytics = () => (
    <div className="space-y-6">
      {/* Revenue Overview */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="bg-gradient-to-br from-green-500/20 to-green-600/20 border border-green-500/30 rounded-xl p-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-green-300 text-sm">Total Revenue</p>
              <p className="text-2xl font-bold text-white">₹{(analytics!.totalRevenue / 100000).toFixed(1)}L</p>
              <p className="text-green-400 text-xs">+{analytics!.monthlyGrowth}% this month</p>
            </div>
            <TrendingUp className="w-8 h-8 text-green-400" />
          </div>
        </div>
        
        <div className="bg-gradient-to-br from-blue-500/20 to-blue-600/20 border border-blue-500/30 rounded-xl p-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-blue-300 text-sm">Monthly Recurring</p>
              <p className="text-2xl font-bold text-white">₹{(analytics!.monthlyRecurring / 1000).toFixed(0)}K</p>
              <p className="text-blue-400 text-xs">ARR: ₹{((analytics!.monthlyRecurring * 12) / 100000).toFixed(1)}L</p>
            </div>
            <Activity className="w-8 h-8 text-blue-400" />
          </div>
        </div>
        
        <div className="bg-gradient-to-br from-purple-500/20 to-purple-600/20 border border-purple-500/30 rounded-xl p-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-purple-300 text-sm">Success Rate</p>
              <p className="text-2xl font-bold text-white">{analytics!.successRate}%</p>
              <p className="text-purple-400 text-xs">{analytics!.totalTransactions} transactions</p>
            </div>
            <CheckCircle className="w-8 h-8 text-purple-400" />
          </div>
        </div>
        
        <div className="bg-gradient-to-br from-yellow-500/20 to-yellow-600/20 border border-yellow-500/30 rounded-xl p-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-yellow-300 text-sm">Avg Transaction</p>
              <p className="text-2xl font-bold text-white">₹{analytics!.avgTransactionValue}</p>
              <p className="text-yellow-400 text-xs">Customer LTV: ₹{analytics!.customerLifetimeValue}</p>
            </div>
            <BarChart3 className="w-8 h-8 text-yellow-400" />
          </div>
        </div>
      </div>

      {/* Payment Methods Distribution */}
      <div className="bg-gray-800/40 backdrop-blur-sm border border-gray-600 rounded-xl p-6">
        <h3 className="text-lg font-semibold text-white mb-4">Payment Methods Distribution</h3>
        <div className="space-y-4">
          {analytics!.topPaymentMethods.map((method) => (
            <div key={method.method} className="flex items-center justify-between">
              <div className="flex items-center space-x-3">
                <div className="w-8 h-2 bg-gradient-to-r from-cyan-400 to-purple-400 rounded-full" style={{
                  width: `${Math.max(method.percentage / 2, 8)}px`
                }} />
                <span className="text-white font-medium">{method.method}</span>
              </div>
              <div className="text-right">
                <div className="text-white font-bold">{method.percentage}%</div>
                <div className="text-gray-400 text-sm">₹{(method.revenue / 1000).toFixed(0)}K</div>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Gateway Performance */}
      <div className="bg-gray-800/40 backdrop-blur-sm border border-gray-600 rounded-xl p-6">
        <h3 className="text-lg font-semibold text-white mb-4">Gateway Performance</h3>
        <div className="overflow-x-auto">
          <table className="w-full text-left">
            <thead>
              <tr className="border-b border-gray-600">
                <th className="pb-3 text-gray-400">Gateway</th>
                <th className="pb-3 text-gray-400">Revenue</th>
                <th className="pb-3 text-gray-400">Transactions</th>
                <th className="pb-3 text-gray-400">Fees Paid</th>
                <th className="pb-3 text-gray-400">Avg Fee Rate</th>
              </tr>
            </thead>
            <tbody>
              {analytics!.revenueByGateway.map((gateway) => (
                <tr key={gateway.gateway} className="border-b border-gray-700">
                  <td className="py-3 text-white font-medium">{gateway.gateway}</td>
                  <td className="py-3 text-green-400">₹{(gateway.revenue / 1000).toFixed(0)}K</td>
                  <td className="py-3 text-blue-400">{gateway.transactions}</td>
                  <td className="py-3 text-red-400">₹{(gateway.fees / 1000).toFixed(1)}K</td>
                  <td className="py-3 text-yellow-400">{((gateway.fees / gateway.revenue) * 100).toFixed(2)}%</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )

  const renderTransactionHistory = () => (
    <div className="bg-gray-800/40 backdrop-blur-sm border border-gray-600 rounded-xl p-6">
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-lg font-semibold text-white">Recent Transactions</h3>
        <button
          onClick={() => generateMockTransactions()}
          className="flex items-center space-x-2 bg-cyan-500/20 hover:bg-cyan-500/30 border border-cyan-500/30 rounded-lg px-3 py-1 text-cyan-400 text-sm transition-colors"
        >
          <RefreshCw className="w-4 h-4" />
          <span>Refresh</span>
        </button>
      </div>
      
      <div className="overflow-x-auto">
        <table className="w-full text-left">
          <thead>
            <tr className="border-b border-gray-600">
              <th className="pb-3 text-gray-400">Transaction ID</th>
              <th className="pb-3 text-gray-400">Amount</th>
              <th className="pb-3 text-gray-400">Method</th>
              <th className="pb-3 text-gray-400">Status</th>
              <th className="pb-3 text-gray-400">Gateway</th>
              <th className="pb-3 text-gray-400">Date</th>
              <th className="pb-3 text-gray-400">Actions</th>
            </tr>
          </thead>
          <tbody>
            {transactions.slice(0, 10).map((txn) => (
              <tr key={txn.id} className="border-b border-gray-700">
                <td className="py-3 text-gray-300 font-mono text-sm">{txn.id.slice(0, 12)}...</td>
                <td className="py-3 text-white font-bold">₹{txn.amount}</td>
                <td className="py-3 text-blue-400">{txn.paymentMethod}</td>
                <td className="py-3">
                  <span className={`px-2 py-1 rounded-full text-xs font-medium ${
                    txn.status === 'success' ? 'bg-green-500/20 text-green-400' :
                    txn.status === 'failed' ? 'bg-red-500/20 text-red-400' :
                    txn.status === 'pending' ? 'bg-yellow-500/20 text-yellow-400' :
                    'bg-blue-500/20 text-blue-400'
                  }`}>
                    {txn.status.toUpperCase()}
                  </span>
                </td>
                <td className="py-3 text-purple-400">{txn.gateway}</td>
                <td className="py-3 text-gray-400 text-sm">
                  {txn.createdAt.toLocaleDateString()}
                </td>
                <td className="py-3">
                  {txn.status === 'failed' && txn.retryAttempts < 3 && (
                    <button
                      onClick={() => retryFailedPayment(txn.id)}
                      disabled={isLoading}
                      className="bg-orange-500/20 hover:bg-orange-500/30 border border-orange-500/30 rounded px-2 py-1 text-orange-400 text-xs transition-colors"
                    >
                      Retry
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-900 via-gray-800 to-gray-900">
      {/* Header */}
      <div className="border-b border-gray-700 bg-gray-800/50 backdrop-blur-sm">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between h-16">
            <div className="flex items-center space-x-3">
              <div className="w-10 h-10 bg-gradient-to-r from-cyan-400 to-purple-400 rounded-lg flex items-center justify-center">
                <CreditCard className="w-6 h-6 text-black" />
              </div>
              <div>
                <h1 className="text-xl font-bold text-white">Payment Gateway</h1>
                <p className="text-sm text-gray-400">Manage payments and subscriptions</p>
              </div>
            </div>

            <div className="flex items-center space-x-4">
              <div className="flex items-center space-x-2 bg-green-500/20 border border-green-500/30 rounded-lg px-3 py-1">
                <div className="w-2 h-2 bg-green-400 rounded-full animate-pulse" />
                <span className="text-green-400 text-sm font-medium">Live</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Navigation Tabs */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
        <div className="flex items-center space-x-1 bg-gray-800/40 backdrop-blur-sm border border-gray-600 rounded-xl p-1 mb-6">
          {[
            { id: 'payments', label: 'Payment Methods', icon: CreditCard },
            { id: 'subscriptions', label: 'Subscription Plans', icon: Calendar },
            { id: 'analytics', label: 'Revenue Analytics', icon: BarChart3 },
            { id: 'settings', label: 'Gateway Settings', icon: Settings }
          ].map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id as any)}
              className={`flex items-center space-x-2 px-4 py-2 rounded-lg font-medium transition-all ${
                activeTab === tab.id
                  ? 'bg-gradient-to-r from-cyan-500 to-purple-500 text-black'
                  : 'text-gray-400 hover:text-white hover:bg-gray-700/50'
              }`}
            >
              <tab.icon className="w-4 h-4" />
              <span className="hidden sm:inline">{tab.label}</span>
            </button>
          ))}
        </div>

        {/* Tab Content */}
        <div className="space-y-6">
          {activeTab === 'payments' && (
            <>
              <div className="mb-6">
                <h2 className="text-2xl font-bold text-white mb-2">Payment Methods</h2>
                <p className="text-gray-400">Manage available payment options and processing settings</p>
              </div>
              {renderPaymentMethods()}
              {renderTransactionHistory()}
            </>
          )}

          {activeTab === 'subscriptions' && (
            <>
              <div className="mb-6">
                <h2 className="text-2xl font-bold text-white mb-2">Subscription Plans</h2>
                <p className="text-gray-400">Choose the perfect plan for your trading needs</p>
              </div>
              {renderSubscriptionPlans()}
              
              {selectedPlan && (
                <div className="bg-gray-800/60 backdrop-blur-sm border border-cyan-500/30 rounded-xl p-6">
                  <h3 className="text-lg font-semibold text-white mb-4">Complete Payment</h3>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    <div>
                      <h4 className="font-medium text-white mb-2">Selected Plan</h4>
                      <div className="bg-gray-700/50 rounded-lg p-4">
                        <div className="flex items-center justify-between">
                          <span className="text-white">{selectedPlan.name}</span>
                          <span className="text-cyan-400 font-bold">₹{selectedPlan.price}</span>
                        </div>
                        {selectedPlan.discount && (
                          <div className="text-green-400 text-sm mt-1">
                            {selectedPlan.discount.percentage}% discount applied!
                          </div>
                        )}
                      </div>
                    </div>
                    
                    <div>
                      <h4 className="font-medium text-white mb-2">Payment Method</h4>
                      <div className="grid grid-cols-2 gap-2">
                        {mockPaymentMethods.map((method) => (
                          <button
                            key={method.id}
                            onClick={() => initiatePayment(
                              selectedPlan.discount 
                                ? selectedPlan.price * (1 - selectedPlan.discount.percentage / 100)
                                : selectedPlan.price,
                              selectedPlan.id,
                              method.name
                            )}
                            disabled={isLoading}
                            className="flex items-center space-x-2 bg-gray-700/50 hover:bg-gray-700 border border-gray-600 rounded-lg p-3 transition-colors"
                          >
                            {method.icon}
                            <span className="text-white text-sm">{method.name}</span>
                          </button>
                        ))}
                      </div>
                    </div>
                  </div>
                </div>
              )}
            </>
          )}

          {activeTab === 'analytics' && (
            <>
              <div className="mb-6">
                <h2 className="text-2xl font-bold text-white mb-2">Revenue Analytics</h2>
                <p className="text-gray-400">Track payment performance and business metrics</p>
              </div>
              {analytics && renderAnalytics()}
            </>
          )}

          {activeTab === 'settings' && (
            <div className="space-y-6">
              <div className="mb-6">
                <h2 className="text-2xl font-bold text-white mb-2">Gateway Settings</h2>
                <p className="text-gray-400">Configure payment gateways and security settings</p>
              </div>
              
              <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                {mockGateways.map((gateway) => (
                  <div key={gateway.id} className="bg-gray-800/40 backdrop-blur-sm border border-gray-600 rounded-xl p-6">
                    <div className="flex items-center justify-between mb-4">
                      <h3 className="text-lg font-semibold text-white">{gateway.name}</h3>
                      <div className={`w-3 h-3 rounded-full ${gateway.isActive ? 'bg-green-400' : 'bg-red-400'}`} />
                    </div>
                    
                    <div className="space-y-3 text-sm">
                      <div className="flex justify-between">
                        <span className="text-gray-400">Provider:</span>
                        <span className="text-white">{gateway.provider}</span>
                      </div>
                      <div className="flex justify-between">
                        <span className="text-gray-400">Processing Fee:</span>
                        <span className="text-yellow-400">{gateway.processingFee}%</span>
                      </div>
                      <div className="flex justify-between">
                        <span className="text-gray-400">Settlement:</span>
                        <span className="text-blue-400">{gateway.settlementTime}</span>
                      </div>
                      <div className="flex justify-between">
                        <span className="text-gray-400">Currency:</span>
                        <span className="text-purple-400">{gateway.currency}</span>
                      </div>
                      <div className="flex justify-between">
                        <span className="text-gray-400">Test Mode:</span>
                        <span className={gateway.testMode ? 'text-orange-400' : 'text-green-400'}>
                          {gateway.testMode ? 'Enabled' : 'Disabled'}
                        </span>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Loading Overlay */}
      {isLoading && (
        <div className="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center z-50">
          <div className="bg-gray-800 border border-cyan-500/30 rounded-xl p-6 flex items-center space-x-4">
            <div className="animate-spin rounded-full h-8 w-8 border-2 border-cyan-400 border-t-transparent" />
            <span className="text-white font-medium">Processing payment...</span>
          </div>
        </div>
      )}
    </div>
  )
}