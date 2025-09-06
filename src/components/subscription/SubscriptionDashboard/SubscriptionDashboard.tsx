import React, { useState, useEffect, useCallback } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Grid,
  Button,
  Chip,
  LinearProgress,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Tabs,
  Tab,
  List,
  ListItem,
  ListItemText,
  ListItemIcon,
  Divider,
  Alert,
  AlertTitle,
  CircularProgress,
  Avatar,
  Badge,
  IconButton,
  Tooltip,
  Stack,
  Switch,
  FormControlLabel,
  TextField,
  MenuItem,
  Accordion,
  AccordionSummary,
  AccordionDetails
} from '@mui/material';
import {
  StarIcon,
  CrownIcon,
  DiamondIcon,
  BusinessIcon,
  CheckCircleIcon,
  CancelIcon,
  PaymentIcon,
  HistoryIcon,
  SettingsIcon,
  NotificationsIcon,
  SecurityIcon,
  HelpIcon,
  ExpandMoreIcon,
  DownloadIcon,
  EditIcon,
  DeleteIcon,
  AddCardIcon,
  AccountBalanceWalletIcon,
  TrendingUpIcon,
  AnalyticsIcon,
  SmartToyIcon,
  GroupIcon,
  SpeedIcon,
  SupportAgentIcon,
  EmojiEventsIcon,
  WarningIcon,
  InfoIcon
} from '@mui/icons-material';
import { motion } from 'framer-motion';

// Enhanced Interfaces for Subscription Management
interface SubscriptionPlan {
  id: string;
  name: string;
  displayName: string;
  description: string;
  monthlyPrice: number;
  yearlyPrice: number;
  features: PlanFeature[];
  limits: PlanLimits;
  tier: 'free' | 'basic' | 'pro' | 'premium' | 'enterprise';
  popular: boolean;
  recommended: boolean;
  icon: React.ReactNode;
  color: string;
  badge?: string;
}

interface PlanFeature {
  name: string;
  description: string;
  included: boolean;
  limit?: string;
  premium?: boolean;
}

interface PlanLimits {
  maxBrokers: number;
  maxWatchlists: number;
  maxAlerts: number;
  apiCallsPerDay: number;
  dataRetention: string;
  supportLevel: 'community' | 'email' | 'priority' | 'dedicated';
  advancedFeatures: boolean;
}

interface UserSubscription {
  id: string;
  planId: string;
  status: 'active' | 'cancelled' | 'expired' | 'trial' | 'suspended';
  billingCycle: 'monthly' | 'yearly';
  startDate: Date;
  endDate: Date;
  renewalDate: Date;
  autoRenew: boolean;
  amount: number;
  currency: string;
  trialDaysRemaining?: number;
  usage: UsageStats;
  paymentMethod: PaymentMethod;
}

interface UsageStats {
  brokersConnected: number;
  watchlistsCreated: number;
  alertsSet: number;
  apiCallsToday: number;
  apiCallsThisMonth: number;
  dataUsageGB: number;
  lastActiveDate: Date;
}

interface PaymentMethod {
  id: string;
  type: 'card' | 'upi' | 'netbanking' | 'wallet';
  details: string; // masked details
  expiry?: string;
  isDefault: boolean;
  provider: string;
}

interface BillingHistory {
  id: string;
  invoiceNumber: string;
  date: Date;
  amount: number;
  currency: string;
  status: 'paid' | 'pending' | 'failed' | 'refunded';
  description: string;
  downloadUrl?: string;
  paymentMethod: string;
  taxAmount: number;
  discountAmount?: number;
}

interface PlanComparison {
  feature: string;
  category: string;
  free: string | boolean;
  basic: string | boolean;
  pro: string | boolean;
  premium: string | boolean;
  enterprise: string | boolean;
}

const SubscriptionDashboard: React.FC = () => {
  const [activeTab, setActiveTab] = useState(0);
  const [userSubscription, setUserSubscription] = useState<UserSubscription | null>(null);
  const [availablePlans, setAvailablePlans] = useState<SubscriptionPlan[]>([]);
  const [billingHistory, setBillingHistory] = useState<BillingHistory[]>([]);
  const [planComparisonOpen, setPlanComparisonOpen] = useState(false);
  const [upgradeDialogOpen, setUpgradeDialogOpen] = useState(false);
  const [selectedPlan, setSelectedPlan] = useState<SubscriptionPlan | null>(null);
  const [billingCycle, setBillingCycle] = useState<'monthly' | 'yearly'>('monthly');
  const [loading, setLoading] = useState(true);
  const [paymentMethods, setPaymentMethods] = useState<PaymentMethod[]>([]);
  const [addPaymentMethodOpen, setAddPaymentMethodOpen] = useState(false);

  // Mock data generation
  const generateMockPlans = useCallback((): SubscriptionPlan[] => {
    return [
      {
        id: 'free',
        name: 'Free',
        displayName: 'Free Starter',
        description: 'Perfect for beginners exploring the platform',
        monthlyPrice: 0,
        yearlyPrice: 0,
        tier: 'free',
        popular: false,
        recommended: false,
        icon: <StarIcon />,
        color: '#757575',
        features: [
          { name: 'Basic Trading', description: 'Simple buy/sell orders', included: true },
          { name: 'Real-time Data', description: '15-minute delayed data', included: true, limit: 'Delayed' },
          { name: 'Portfolio Tracking', description: 'Basic portfolio overview', included: true },
          { name: 'Mobile App', description: 'iOS and Android apps', included: true },
          { name: 'Community Support', description: 'Forum-based support', included: true }
        ],
        limits: {
          maxBrokers: 1,
          maxWatchlists: 3,
          maxAlerts: 5,
          apiCallsPerDay: 100,
          dataRetention: '30 days',
          supportLevel: 'community',
          advancedFeatures: false
        }
      },
      {
        id: 'pro',
        name: 'Pro',
        displayName: 'Pro Trader',
        description: 'Advanced features for serious traders',
        monthlyPrice: 999,
        yearlyPrice: 9990,
        tier: 'pro',
        popular: true,
        recommended: true,
        icon: <TrendingUpIcon />,
        color: '#2196F3',
        badge: 'Most Popular',
        features: [
          { name: 'Advanced Trading', description: 'Complex order types and strategies', included: true },
          { name: 'Real-time Data', description: 'Live market data feeds', included: true },
          { name: 'Multi-Broker Support', description: 'Connect multiple brokers', included: true },
          { name: 'Advanced Charts', description: 'Professional charting tools', included: true },
          { name: 'Price Alerts', description: 'Unlimited price notifications', included: true },
          { name: 'Portfolio Analytics', description: 'Detailed performance analysis', included: true },
          { name: 'API Access', description: 'Programmatic trading access', included: true, limit: '10,000 calls/day' },
          { name: 'Email Support', description: '24/7 email support', included: true }
        ],
        limits: {
          maxBrokers: 5,
          maxWatchlists: 20,
          maxAlerts: 100,
          apiCallsPerDay: 10000,
          dataRetention: '1 year',
          supportLevel: 'email',
          advancedFeatures: true
        }
      },
      {
        id: 'ai_premium',
        name: 'AI Premium',
        displayName: 'AI Premium',
        description: 'AI-powered trading insights and automation',
        monthlyPrice: 2999,
        yearlyPrice: 29990,
        tier: 'premium',
        popular: false,
        recommended: false,
        icon: <SmartToyIcon />,
        color: '#9C27B0',
        badge: 'AI Powered',
        features: [
          { name: 'Everything in Pro', description: 'All Pro features included', included: true },
          { name: 'Behavioral AI', description: 'Trading psychology analysis', included: true },
          { name: 'AI Market Insights', description: 'AI-generated market analysis', included: true },
          { name: 'Institutional Flow Detection', description: 'Track institutional activity', included: true },
          { name: 'Risk Management AI', description: 'AI-powered risk assessment', included: true },
          { name: 'Personalized Coaching', description: 'AI trading coach', included: true },
          { name: 'Advanced Backtesting', description: 'AI-enhanced strategy testing', included: true },
          { name: 'Priority Support', description: 'Priority email and chat support', included: true }
        ],
        limits: {
          maxBrokers: 10,
          maxWatchlists: 50,
          maxAlerts: 500,
          apiCallsPerDay: 50000,
          dataRetention: '3 years',
          supportLevel: 'priority',
          advancedFeatures: true
        }
      },
      {
        id: 'enterprise',
        name: 'Enterprise',
        displayName: 'Enterprise',
        description: 'Complete solution for institutions and teams',
        monthlyPrice: 9999,
        yearlyPrice: 99990,
        tier: 'enterprise',
        popular: false,
        recommended: false,
        icon: <BusinessIcon />,
        color: '#FF9800',
        badge: 'Enterprise',
        features: [
          { name: 'Everything in AI Premium', description: 'All AI Premium features', included: true },
          { name: 'Multi-User Access', description: 'Team collaboration tools', included: true },
          { name: 'White-label Solution', description: 'Branded platform access', included: true },
          { name: 'Custom Integrations', description: 'Tailored API integrations', included: true },
          { name: 'Dedicated Support', description: 'Dedicated account manager', included: true },
          { name: 'Advanced Reporting', description: 'Custom analytics and reports', included: true },
          { name: 'Compliance Tools', description: 'Regulatory compliance features', included: true },
          { name: 'SLA Guarantee', description: '99.9% uptime guarantee', included: true }
        ],
        limits: {
          maxBrokers: -1, // unlimited
          maxWatchlists: -1,
          maxAlerts: -1,
          apiCallsPerDay: -1,
          dataRetention: 'unlimited',
          supportLevel: 'dedicated',
          advancedFeatures: true
        }
      }
    ];
  }, []);

  const generateMockSubscription = useCallback((): UserSubscription => {
    return {
      id: 'sub_123456',
      planId: 'pro',
      status: 'active',
      billingCycle: 'monthly',
      startDate: new Date('2024-01-15'),
      endDate: new Date('2024-02-15'),
      renewalDate: new Date('2024-02-15'),
      autoRenew: true,
      amount: 999,
      currency: 'INR',
      usage: {
        brokersConnected: 3,
        watchlistsCreated: 12,
        alertsSet: 45,
        apiCallsToday: 2847,
        apiCallsThisMonth: 45623,
        dataUsageGB: 2.4,
        lastActiveDate: new Date()
      },
      paymentMethod: {
        id: 'pm_1',
        type: 'card',
        details: '**** **** **** 1234',
        expiry: '12/25',
        isDefault: true,
        provider: 'Visa'
      }
    };
  }, []);

  const generateBillingHistory = useCallback((): BillingHistory[] => {
    return [
      {
        id: 'inv_001',
        invoiceNumber: 'TM-2024-001',
        date: new Date('2024-01-15'),
        amount: 999,
        currency: 'INR',
        status: 'paid',
        description: 'Pro Trader - Monthly Subscription',
        downloadUrl: '/invoices/TM-2024-001.pdf',
        paymentMethod: 'Visa *1234',
        taxAmount: 179.82,
        discountAmount: 100
      },
      {
        id: 'inv_002',
        invoiceNumber: 'TM-2023-012',
        date: new Date('2023-12-15'),
        amount: 999,
        currency: 'INR',
        status: 'paid',
        description: 'Pro Trader - Monthly Subscription',
        downloadUrl: '/invoices/TM-2023-012.pdf',
        paymentMethod: 'Visa *1234',
        taxAmount: 179.82
      },
      {
        id: 'inv_003',
        invoiceNumber: 'TM-2023-011',
        date: new Date('2023-11-15'),
        amount: 999,
        currency: 'INR',
        status: 'paid',
        description: 'Pro Trader - Monthly Subscription',
        paymentMethod: 'UPI',
        taxAmount: 179.82
      }
    ];
  }, []);

  const generatePaymentMethods = useCallback((): PaymentMethod[] => {
    return [
      {
        id: 'pm_1',
        type: 'card',
        details: 'Visa ending in 1234',
        expiry: '12/25',
        isDefault: true,
        provider: 'Visa'
      },
      {
        id: 'pm_2',
        type: 'upi',
        details: 'user@paytm',
        isDefault: false,
        provider: 'Paytm'
      },
      {
        id: 'pm_3',
        type: 'card',
        details: 'Mastercard ending in 5678',
        expiry: '08/26',
        isDefault: false,
        provider: 'Mastercard'
      }
    ];
  }, []);

  useEffect(() => {
    const loadData = async () => {
      setLoading(true);
      // Simulate API call
      await new Promise(resolve => setTimeout(resolve, 1000));
      
      const plans = generateMockPlans();
      const subscription = generateMockSubscription();
      const history = generateBillingHistory();
      const payments = generatePaymentMethods();
      
      setAvailablePlans(plans);
      setUserSubscription(subscription);
      setBillingHistory(history);
      setPaymentMethods(payments);
      setLoading(false);
    };

    loadData();
  }, [generateMockPlans, generateMockSubscription, generateBillingHistory, generatePaymentMethods]);

  const handleTabChange = (event: React.SyntheticEvent, newValue: number) => {
    setActiveTab(newValue);
  };

  const handleUpgrade = (plan: SubscriptionPlan) => {
    setSelectedPlan(plan);
    setUpgradeDialogOpen(true);
  };

  const handleCancelSubscription = () => {
    // Handle subscription cancellation
    console.log('Cancelling subscription...');
  };

  const handleDownloadInvoice = (invoice: BillingHistory) => {
    // Handle invoice download
    console.log('Downloading invoice:', invoice.invoiceNumber);
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'active': return 'success';
      case 'trial': return 'info';
      case 'cancelled': return 'warning';
      case 'expired': return 'error';
      case 'suspended': return 'error';
      default: return 'default';
    }
  };

  const getUsagePercentage = (used: number, limit: number) => {
    if (limit === -1) return 0; // unlimited
    return Math.min((used / limit) * 100, 100);
  };

  const formatLimit = (limit: number) => {
    if (limit === -1) return 'Unlimited';
    return limit.toLocaleString();
  };

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress size={60} />
      </Box>
    );
  }

  if (!userSubscription) {
    return (
      <Alert severity="error">
        <AlertTitle>Error</AlertTitle>
        Failed to load subscription data. Please try again later.
      </Alert>
    );
  }

  const currentPlan = availablePlans.find(plan => plan.id === userSubscription.planId);

  return (
    <Box sx={{ p: 3 }}>
      {/* Header Section */}
      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid item xs={12} md={8}>
          <Card sx={{ background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)', color: 'white' }}>
            <CardContent>
              <Box display="flex" alignItems="center" gap={2}>
                <Avatar sx={{ width: 60, height: 60, bgcolor: currentPlan?.color }}>
                  {currentPlan?.icon}
                </Avatar>
                <Box flex={1}>
                  <Typography variant="h4" fontWeight="bold">
                    {currentPlan?.displayName}
                  </Typography>
                  <Box display="flex" alignItems="center" gap={2} mt={1}>
                    <Chip 
                      label={userSubscription.status.toUpperCase()} 
                      color={getStatusColor(userSubscription.status) as any}
                      size="small"
                    />
                    <Typography variant="body2" sx={{ opacity: 0.9 }}>
                      {userSubscription.billingCycle === 'monthly' ? 'Monthly' : 'Yearly'} billing
                    </Typography>
                  </Box>
                  <Typography variant="body2" sx={{ opacity: 0.8, mt: 1 }}>
                    Next billing: {userSubscription.renewalDate.toLocaleDateString()}
                  </Typography>
                </Box>
                <Box textAlign="right">
                  <Typography variant="h5" fontWeight="bold">
                    ₹{userSubscription.amount.toLocaleString()}
                  </Typography>
                  <Typography variant="body2" sx={{ opacity: 0.9 }}>
                    per {userSubscription.billingCycle === 'monthly' ? 'month' : 'year'}
                  </Typography>
                </Box>
              </Box>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={4}>
          <Grid container spacing={2}>
            <Grid item xs={12}>
              <Card>
                <CardContent sx={{ textAlign: 'center' }}>
                  <EmojiEventsIcon sx={{ fontSize: 40, color: 'gold', mb: 1 }} />
                  <Typography variant="h6" fontWeight="bold">
                    {userSubscription.usage.brokersConnected} / {formatLimit(currentPlan?.limits.maxBrokers || 0)}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    Brokers Connected
                  </Typography>
                </CardContent>
              </Card>
            </Grid>
            <Grid item xs={6}>
              <Card sx={{ textAlign: 'center' }}>
                <CardContent sx={{ pb: '16px !important' }}>
                  <AnalyticsIcon color="primary" />
                  <Typography variant="body2" fontWeight="bold">
                    {userSubscription.usage.apiCallsToday.toLocaleString()}
                  </Typography>
                  <Typography variant="caption">API Calls Today</Typography>
                </CardContent>
              </Card>
            </Grid>
            <Grid item xs={6}>
              <Card sx={{ textAlign: 'center' }}>
                <CardContent sx={{ pb: '16px !important' }}>
                  <NotificationsIcon color="secondary" />
                  <Typography variant="body2" fontWeight="bold">
                    {userSubscription.usage.alertsSet}
                  </Typography>
                  <Typography variant="caption">Active Alerts</Typography>
                </CardContent>
              </Card>
            </Grid>
          </Grid>
        </Grid>
      </Grid>

      {/* Navigation Tabs */}
      <Card sx={{ mb: 3 }}>
        <Tabs
          value={activeTab}
          onChange={handleTabChange}
          variant="scrollable"
          scrollButtons="auto"
        >
          <Tab icon={<StarIcon />} label="Current Plan" />
          <Tab icon={<TrendingUpIcon />} label="Upgrade Plans" />
          <Tab icon={<SpeedIcon />} label="Usage & Limits" />
          <Tab icon={<HistoryIcon />} label="Billing History" />
          <Tab icon={<PaymentIcon />} label="Payment Methods" />
          <Tab icon={<SettingsIcon />} label="Settings" />
        </Tabs>
      </Card>

      {/* Tab Content */}
      <Box>
        {/* Current Plan Tab */}
        {activeTab === 0 && (
          <Grid container spacing={3}>
            <Grid item xs={12} md={8}>
              <Card>
                <CardContent>
                  <Typography variant="h6" gutterBottom>
                    Plan Features
                  </Typography>
                  <List>
                    {currentPlan?.features.map((feature, index) => (
                      <ListItem key={index}>
                        <ListItemIcon>
                          <CheckCircleIcon color="success" />
                        </ListItemIcon>
                        <ListItemText
                          primary={feature.name}
                          secondary={
                            <Box>
                              <Typography variant="body2" color="text.secondary">
                                {feature.description}
                              </Typography>
                              {feature.limit && (
                                <Chip 
                                  label={feature.limit} 
                                  size="small" 
                                  variant="outlined" 
                                  sx={{ mt: 0.5 }}
                                />
                              )}
                            </Box>
                          }
                        />
                      </ListItem>
                    ))}
                  </List>
                </CardContent>
              </Card>
            </Grid>
            <Grid item xs={12} md={4}>
              <Card>
                <CardContent>
                  <Typography variant="h6" gutterBottom>
                    Subscription Details
                  </Typography>
                  <Box sx={{ mt: 2 }}>
                    <Typography variant="body2" color="text.secondary">
                      Started: {userSubscription.startDate.toLocaleDateString()}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      Next billing: {userSubscription.renewalDate.toLocaleDateString()}
                    </Typography>
                    <FormControlLabel
                      control={
                        <Switch 
                          checked={userSubscription.autoRenew} 
                          onChange={() => {/* handle toggle */}}
                        />
                      }
                      label="Auto-renew"
                      sx={{ mt: 2 }}
                    />
                  </Box>
                  <Divider sx={{ my: 2 }} />
                  <Stack spacing={2}>
                    <Button 
                      variant="contained" 
                      fullWidth
                      onClick={() => setPlanComparisonOpen(true)}
                    >
                      Compare Plans
                    </Button>
                    <Button 
                      variant="outlined" 
                      color="error" 
                      fullWidth
                      onClick={handleCancelSubscription}
                    >
                      Cancel Subscription
                    </Button>
                  </Stack>
                </CardContent>
              </Card>
            </Grid>
          </Grid>
        )}

        {/* Upgrade Plans Tab */}
        {activeTab === 1 && (
          <Box>
            {/* Billing Toggle */}
            <Box display="flex" justifyContent="center" mb={3}>
              <Paper sx={{ p: 1 }}>
                <Button
                  variant={billingCycle === 'monthly' ? 'contained' : 'outlined'}
                  onClick={() => setBillingCycle('monthly')}
                  sx={{ mr: 1 }}
                >
                  Monthly
                </Button>
                <Button
                  variant={billingCycle === 'yearly' ? 'contained' : 'outlined'}
                  onClick={() => setBillingCycle('yearly')}
                >
                  Yearly (Save 17%)
                </Button>
              </Paper>
            </Box>

            {/* Plan Cards */}
            <Grid container spacing={3}>
              {availablePlans.map((plan) => (
                <Grid item xs={12} sm={6} lg={3} key={plan.id}>
                  <motion.div whileHover={{ scale: 1.02 }}>
                    <Card 
                      sx={{ 
                        height: '100%',
                        border: plan.popular ? `2px solid ${plan.color}` : '1px solid #e0e0e0',
                        position: 'relative',
                        '&:hover': { boxShadow: 6 }
                      }}
                    >
                      {plan.badge && (
                        <Chip 
                          label={plan.badge}
                          size="small"
                          sx={{ 
                            position: 'absolute',
                            top: 16,
                            right: 16,
                            bgcolor: plan.color,
                            color: 'white'
                          }}
                        />
                      )}
                      <CardContent sx={{ textAlign: 'center', height: '100%', display: 'flex', flexDirection: 'column' }}>
                        <Avatar sx={{ bgcolor: plan.color, width: 60, height: 60, mx: 'auto', mb: 2 }}>
                          {plan.icon}
                        </Avatar>
                        <Typography variant="h5" fontWeight="bold" gutterBottom>
                          {plan.displayName}
                        </Typography>
                        <Typography variant="body2" color="text.secondary" mb={3}>
                          {plan.description}
                        </Typography>
                        
                        <Box sx={{ mt: 'auto' }}>
                          <Typography variant="h4" fontWeight="bold" color="primary">
                            ₹{billingCycle === 'monthly' ? plan.monthlyPrice : plan.yearlyPrice}
                          </Typography>
                          <Typography variant="body2" color="text.secondary">
                            per {billingCycle === 'monthly' ? 'month' : 'year'}
                          </Typography>
                          
                          {billingCycle === 'yearly' && plan.monthlyPrice > 0 && (
                            <Typography variant="caption" color="success.main" display="block" sx={{ mt: 1 }}>
                              Save ₹{(plan.monthlyPrice * 12 - plan.yearlyPrice).toLocaleString()}
                            </Typography>
                          )}
                          
                          <Button
                            variant={plan.id === userSubscription.planId ? 'outlined' : 'contained'}
                            fullWidth
                            sx={{ mt: 2 }}
                            disabled={plan.id === userSubscription.planId}
                            onClick={() => handleUpgrade(plan)}
                          >
                            {plan.id === userSubscription.planId ? 'Current Plan' : 'Upgrade'}
                          </Button>
                        </Box>
                      </CardContent>
                    </Card>
                  </motion.div>
                </Grid>
              ))}
            </Grid>
          </Box>
        )}

        {/* Usage & Limits Tab */}
        {activeTab === 2 && (
          <Grid container spacing={3}>
            <Grid item xs={12} md={6}>
              <Card>
                <CardContent>
                  <Typography variant="h6" gutterBottom>
                    Current Usage
                  </Typography>
                  <Stack spacing={3}>
                    {/* Brokers */}
                    <Box>
                      <Box display="flex" justifyContent="space-between" alignItems="center" mb={1}>
                        <Typography variant="body2">Brokers Connected</Typography>
                        <Typography variant="body2">
                          {userSubscription.usage.brokersConnected} / {formatLimit(currentPlan?.limits.maxBrokers || 0)}
                        </Typography>
                      </Box>
                      <LinearProgress
                        variant="determinate"
                        value={getUsagePercentage(userSubscription.usage.brokersConnected, currentPlan?.limits.maxBrokers || 0)}
                        sx={{ height: 8, borderRadius: 4 }}
                      />
                    </Box>

                    {/* Watchlists */}
                    <Box>
                      <Box display="flex" justifyContent="space-between" alignItems="center" mb={1}>
                        <Typography variant="body2">Watchlists</Typography>
                        <Typography variant="body2">
                          {userSubscription.usage.watchlistsCreated} / {formatLimit(currentPlan?.limits.maxWatchlists || 0)}
                        </Typography>
                      </Box>
                      <LinearProgress
                        variant="determinate"
                        value={getUsagePercentage(userSubscription.usage.watchlistsCreated, currentPlan?.limits.maxWatchlists || 0)}
                        sx={{ height: 8, borderRadius: 4 }}
                      />
                    </Box>

                    {/* Alerts */}
                    <Box>
                      <Box display="flex" justifyContent="space-between" alignItems="center" mb={1}>
                        <Typography variant="body2">Price Alerts</Typography>
                        <Typography variant="body2">
                          {userSubscription.usage.alertsSet} / {formatLimit(currentPlan?.limits.maxAlerts || 0)}
                        </Typography>
                      </Box>
                      <LinearProgress
                        variant="determinate"
                        value={getUsagePercentage(userSubscription.usage.alertsSet, currentPlan?.limits.maxAlerts || 0)}
                        sx={{ height: 8, borderRadius: 4 }}
                      />
                    </Box>

                    {/* API Calls */}
                    <Box>
                      <Box display="flex" justifyContent="space-between" alignItems="center" mb={1}>
                        <Typography variant="body2">API Calls (Today)</Typography>
                        <Typography variant="body2">
                          {userSubscription.usage.apiCallsToday.toLocaleString()} / {formatLimit(currentPlan?.limits.apiCallsPerDay || 0)}
                        </Typography>
                      </Box>
                      <LinearProgress
                        variant="determinate"
                        value={getUsagePercentage(userSubscription.usage.apiCallsToday, currentPlan?.limits.apiCallsPerDay || 0)}
                        sx={{ height: 8, borderRadius: 4 }}
                      />
                    </Box>
                  </Stack>
                </CardContent>
              </Card>
            </Grid>
            <Grid item xs={12} md={6}>
              <Card>
                <CardContent>
                  <Typography variant="h6" gutterBottom>
                    Plan Limits
                  </Typography>
                  <List>
                    <ListItem>
                      <ListItemText
                        primary="Data Retention"
                        secondary={currentPlan?.limits.dataRetention}
                      />
                    </ListItem>
                    <ListItem>
                      <ListItemText
                        primary="Support Level"
                        secondary={currentPlan?.limits.supportLevel.replace('_', ' ').replace(/\b\w/g, l => l.toUpperCase())}
                      />
                    </ListItem>
                    <ListItem>
                      <ListItemText
                        primary="Advanced Features"
                        secondary={currentPlan?.limits.advancedFeatures ? 'Enabled' : 'Disabled'}
                      />
                    </ListItem>
                    <ListItem>
                      <ListItemText
                        primary="Monthly API Calls"
                        secondary={userSubscription.usage.apiCallsThisMonth.toLocaleString()}
                      />
                    </ListItem>
                  </List>
                </CardContent>
              </Card>
            </Grid>
          </Grid>
        )}

        {/* Billing History Tab */}
        {activeTab === 3 && (
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Billing History
              </Typography>
              <TableContainer component={Paper} variant="outlined">
                <Table>
                  <TableHead>
                    <TableRow>
                      <TableCell>Invoice</TableCell>
                      <TableCell>Date</TableCell>
                      <TableCell>Description</TableCell>
                      <TableCell align="right">Amount</TableCell>
                      <TableCell>Status</TableCell>
                      <TableCell>Payment Method</TableCell>
                      <TableCell align="center">Actions</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {billingHistory.map((invoice) => (
                      <TableRow key={invoice.id}>
                        <TableCell fontWeight="bold">{invoice.invoiceNumber}</TableCell>
                        <TableCell>{invoice.date.toLocaleDateString()}</TableCell>
                        <TableCell>{invoice.description}</TableCell>
                        <TableCell align="right">
                          <Box>
                            <Typography variant="body2" fontWeight="bold">
                              ₹{invoice.amount.toLocaleString()}
                            </Typography>
                            <Typography variant="caption" color="text.secondary">
                              +₹{invoice.taxAmount} tax
                            </Typography>
                            {invoice.discountAmount && (
                              <Typography variant="caption" color="success.main" display="block">
                                -₹{invoice.discountAmount} discount
                              </Typography>
                            )}
                          </Box>
                        </TableCell>
                        <TableCell>
                          <Chip 
                            label={invoice.status} 
                            color={invoice.status === 'paid' ? 'success' : invoice.status === 'pending' ? 'warning' : 'error'}
                            size="small"
                          />
                        </TableCell>
                        <TableCell>{invoice.paymentMethod}</TableCell>
                        <TableCell align="center">
                          {invoice.downloadUrl && (
                            <Tooltip title="Download Invoice">
                              <IconButton onClick={() => handleDownloadInvoice(invoice)}>
                                <DownloadIcon />
                              </IconButton>
                            </Tooltip>
                          )}
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            </CardContent>
          </Card>
        )}

        {/* Payment Methods Tab */}
        {activeTab === 4 && (
          <Grid container spacing={3}>
            <Grid item xs={12} md={8}>
              <Card>
                <CardContent>
                  <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
                    <Typography variant="h6">
                      Payment Methods
                    </Typography>
                    <Button 
                      variant="contained" 
                      startIcon={<AddCardIcon />}
                      onClick={() => setAddPaymentMethodOpen(true)}
                    >
                      Add Payment Method
                    </Button>
                  </Box>
                  <Stack spacing={2}>
                    {paymentMethods.map((method) => (
                      <Paper key={method.id} sx={{ p: 2, border: method.isDefault ? '2px solid #2196f3' : '1px solid #e0e0e0' }}>
                        <Box display="flex" alignItems="center" gap={2}>
                          <Avatar sx={{ bgcolor: method.type === 'card' ? '#4caf50' : '#ff9800' }}>
                            {method.type === 'card' ? <PaymentIcon /> : <AccountBalanceWalletIcon />}
                          </Avatar>
                          <Box flex={1}>
                            <Typography variant="subtitle1" fontWeight="bold">
                              {method.details}
                            </Typography>
                            <Typography variant="body2" color="text.secondary">
                              {method.provider}
                              {method.expiry && ` • Expires ${method.expiry}`}
                            </Typography>
                          </Box>
                          {method.isDefault && (
                            <Chip label="Default" color="primary" size="small" />
                          )}
                          <IconButton>
                            <EditIcon />
                          </IconButton>
                          <IconButton color="error">
                            <DeleteIcon />
                          </IconButton>
                        </Box>
                      </Paper>
                    ))}
                  </Stack>
                </CardContent>
              </Card>
            </Grid>
            <Grid item xs={12} md={4}>
              <Card>
                <CardContent>
                  <Typography variant="h6" gutterBottom>
                    Payment Security
                  </Typography>
                  <List>
                    <ListItem>
                      <ListItemIcon>
                        <SecurityIcon color="success" />
                      </ListItemIcon>
                      <ListItemText
                        primary="256-bit SSL Encryption"
                        secondary="Your payment data is secure"
                      />
                    </ListItem>
                    <ListItem>
                      <ListItemIcon>
                        <CheckCircleIcon color="success" />
                      </ListItemIcon>
                      <ListItemText
                        primary="PCI Compliant"
                        secondary="Industry standard security"
                      />
                    </ListItem>
                    <ListItem>
                      <ListItemIcon>
                        <SecurityIcon color="success" />
                      </ListItemIcon>
                      <ListItemText
                        primary="No Card Storage"
                        secondary="We never store your card details"
                      />
                    </ListItem>
                  </List>
                </CardContent>
              </Card>
            </Grid>
          </Grid>
        )}

        {/* Settings Tab */}
        {activeTab === 5 && (
          <Grid container spacing={3}>
            <Grid item xs={12} md={6}>
              <Card>
                <CardContent>
                  <Typography variant="h6" gutterBottom>
                    Billing Settings
                  </Typography>
                  <Stack spacing={3}>
                    <FormControlLabel
                      control={
                        <Switch 
                          checked={userSubscription.autoRenew} 
                          onChange={() => {/* handle toggle */}}
                        />
                      }
                      label="Auto-renew subscription"
                    />
                    <FormControlLabel
                      control={<Switch defaultChecked />}
                      label="Email billing notifications"
                    />
                    <FormControlLabel
                      control={<Switch />}
                      label="SMS billing alerts"
                    />
                    <TextField
                      label="Billing Email"
                      defaultValue="user@example.com"
                      fullWidth
                      variant="outlined"
                    />
                  </Stack>
                </CardContent>
              </Card>
            </Grid>
            <Grid item xs={12} md={6}>
              <Card>
                <CardContent>
                  <Typography variant="h6" gutterBottom>
                    Support & Help
                  </Typography>
                  <Stack spacing={2}>
                    <Button variant="outlined" fullWidth startIcon={<SupportAgentIcon />}>
                      Contact Support
                    </Button>
                    <Button variant="outlined" fullWidth startIcon={<HelpIcon />}>
                      FAQ & Help Center
                    </Button>
                    <Button variant="outlined" fullWidth startIcon={<DownloadIcon />}>
                      Download Usage Report
                    </Button>
                  </Stack>
                  
                  <Divider sx={{ my: 3 }} />
                  
                  <Typography variant="h6" gutterBottom>
                    Account Actions
                  </Typography>
                  <Stack spacing={2}>
                    <Button variant="outlined" color="warning" fullWidth>
                      Pause Subscription
                    </Button>
                    <Button variant="outlined" color="error" fullWidth>
                      Delete Account
                    </Button>
                  </Stack>
                </CardContent>
              </Card>
            </Grid>
          </Grid>
        )}
      </Box>

      {/* Plan Comparison Dialog */}
      <Dialog
        open={planComparisonOpen}
        onClose={() => setPlanComparisonOpen(false)}
        maxWidth="lg"
        fullWidth
      >
        <DialogTitle>
          <Typography variant="h5" fontWeight="bold">
            Compare Plans
          </Typography>
        </DialogTitle>
        <DialogContent>
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Feature</TableCell>
                  {availablePlans.map((plan) => (
                    <TableCell key={plan.id} align="center">
                      <Box>
                        <Avatar sx={{ bgcolor: plan.color, mx: 'auto', mb: 1 }}>
                          {plan.icon}
                        </Avatar>
                        <Typography variant="subtitle2" fontWeight="bold">
                          {plan.displayName}
                        </Typography>
                        <Typography variant="h6" color="primary">
                          ₹{plan.monthlyPrice}/mo
                        </Typography>
                      </Box>
                    </TableCell>
                  ))}
                </TableRow>
              </TableHead>
              <TableBody>
                <TableRow>
                  <TableCell fontWeight="bold">Max Brokers</TableCell>
                  {availablePlans.map((plan) => (
                    <TableCell key={plan.id} align="center">
                      {formatLimit(plan.limits.maxBrokers)}
                    </TableCell>
                  ))}
                </TableRow>
                <TableRow>
                  <TableCell fontWeight="bold">Max Watchlists</TableCell>
                  {availablePlans.map((plan) => (
                    <TableCell key={plan.id} align="center">
                      {formatLimit(plan.limits.maxWatchlists)}
                    </TableCell>
                  ))}
                </TableRow>
                <TableRow>
                  <TableCell fontWeight="bold">Max Alerts</TableCell>
                  {availablePlans.map((plan) => (
                    <TableCell key={plan.id} align="center">
                      {formatLimit(plan.limits.maxAlerts)}
                    </TableCell>
                  ))}
                </TableRow>
                <TableRow>
                  <TableCell fontWeight="bold">API Calls/Day</TableCell>
                  {availablePlans.map((plan) => (
                    <TableCell key={plan.id} align="center">
                      {formatLimit(plan.limits.apiCallsPerDay)}
                    </TableCell>
                  ))}
                </TableRow>
                <TableRow>
                  <TableCell fontWeight="bold">Data Retention</TableCell>
                  {availablePlans.map((plan) => (
                    <TableCell key={plan.id} align="center">
                      {plan.limits.dataRetention}
                    </TableCell>
                  ))}
                </TableRow>
                <TableRow>
                  <TableCell fontWeight="bold">Support Level</TableCell>
                  {availablePlans.map((plan) => (
                    <TableCell key={plan.id} align="center">
                      {plan.limits.supportLevel.replace('_', ' ').replace(/\b\w/g, l => l.toUpperCase())}
                    </TableCell>
                  ))}
                </TableRow>
              </TableBody>
            </Table>
          </TableContainer>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setPlanComparisonOpen(false)}>Close</Button>
        </DialogActions>
      </Dialog>

      {/* Upgrade Dialog */}
      <Dialog
        open={upgradeDialogOpen}
        onClose={() => setUpgradeDialogOpen(false)}
        maxWidth="sm"
        fullWidth
      >
        {selectedPlan && (
          <>
            <DialogTitle>
              <Box textAlign="center">
                <Avatar sx={{ bgcolor: selectedPlan.color, width: 60, height: 60, mx: 'auto', mb: 2 }}>
                  {selectedPlan.icon}
                </Avatar>
                <Typography variant="h5" fontWeight="bold">
                  Upgrade to {selectedPlan.displayName}
                </Typography>
              </Box>
            </DialogTitle>
            <DialogContent>
              <Box textAlign="center" mb={3}>
                <Typography variant="h4" fontWeight="bold" color="primary">
                  ₹{billingCycle === 'monthly' ? selectedPlan.monthlyPrice : selectedPlan.yearlyPrice}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  per {billingCycle === 'monthly' ? 'month' : 'year'}
                </Typography>
              </Box>
              
              <Alert severity="info" sx={{ mb: 3 }}>
                <AlertTitle>Upgrade Benefits</AlertTitle>
                You'll get immediate access to all {selectedPlan.displayName} features.
                {billingCycle === 'yearly' && selectedPlan.monthlyPrice > 0 && (
                  <Typography variant="body2" sx={{ mt: 1 }}>
                    Save ₹{(selectedPlan.monthlyPrice * 12 - selectedPlan.yearlyPrice).toLocaleString()} with yearly billing!
                  </Typography>
                )}
              </Alert>

              <Typography variant="subtitle2" gutterBottom>
                Key Features:
              </Typography>
              <List dense>
                {selectedPlan.features.slice(0, 5).map((feature, index) => (
                  <ListItem key={index}>
                    <ListItemIcon>
                      <CheckCircleIcon color="success" fontSize="small" />
                    </ListItemIcon>
                    <ListItemText primary={feature.name} />
                  </ListItem>
                ))}
              </List>
            </DialogContent>
            <DialogActions>
              <Button onClick={() => setUpgradeDialogOpen(false)}>Cancel</Button>
              <Button variant="contained" size="large">
                Upgrade Now
              </Button>
            </DialogActions>
          </>
        )}
      </Dialog>

      {/* Add Payment Method Dialog */}
      <Dialog
        open={addPaymentMethodOpen}
        onClose={() => setAddPaymentMethodOpen(false)}
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle>Add Payment Method</DialogTitle>
        <DialogContent>
          <Stack spacing={3} sx={{ mt: 1 }}>
            <TextField
              label="Payment Type"
              select
              fullWidth
              defaultValue="card"
            >
              <MenuItem value="card">Credit/Debit Card</MenuItem>
              <MenuItem value="upi">UPI</MenuItem>
              <MenuItem value="netbanking">Net Banking</MenuItem>
              <MenuItem value="wallet">Wallet</MenuItem>
            </TextField>
            <TextField
              label="Card Number"
              placeholder="1234 5678 9012 3456"
              fullWidth
            />
            <Grid container spacing={2}>
              <Grid item xs={6}>
                <TextField
                  label="Expiry Date"
                  placeholder="MM/YY"
                  fullWidth
                />
              </Grid>
              <Grid item xs={6}>
                <TextField
                  label="CVV"
                  placeholder="123"
                  fullWidth
                />
              </Grid>
            </Grid>
            <TextField
              label="Cardholder Name"
              fullWidth
            />
            <FormControlLabel
              control={<Switch />}
              label="Set as default payment method"
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setAddPaymentMethodOpen(false)}>Cancel</Button>
          <Button variant="contained">Add Payment Method</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default SubscriptionDashboard;