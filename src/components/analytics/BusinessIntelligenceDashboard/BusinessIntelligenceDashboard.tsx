import React, { useState, useEffect, useCallback } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Grid,
  Tabs,
  Tab,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  Button,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Chip,
  LinearProgress,
  CircularProgress,
  Alert,
  AlertTitle,
  IconButton,
  Tooltip,
  Stack,
  Divider,
  Avatar,
  List,
  ListItem,
  ListItemText,
  ListItemIcon,
  Badge,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Switch,
  FormControlLabel
} from '@mui/material';
import {
  TrendingUpIcon,
  TrendingDownIcon,
  AnalyticsIcon,
  MonetizationOnIcon,
  PeopleIcon,
  AssessmentIcon,
  InsightsIcon,
  WarningIcon,
  CheckCircleIcon,
  CancelIcon,
  FilterListIcon,
  DownloadIcon,
  RefreshIcon,
  SettingsIcon,
  NotificationsIcon,
  ShareIcon,
  TimelineIcon,
  BarChartIcon,
  PieChartIcon,
  ShowChartIcon,
  BusinessCenterIcon,
  CampaignIcon,
  ScienceIcon,
  PredictionsIcon,
  SpeedIcon,
  TargetIcon,
  GroupsIcon,
  EventIcon,
  CalendarTodayIcon
} from '@mui/icons-material';
import {
  LineChart,
  Line,
  AreaChart,
  Area,
  BarChart,
  Bar,
  PieChart,
  Pie,
  Cell,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip as RechartsTooltip,
  ResponsiveContainer,
  ComposedChart,
  ReferenceLine,
  Scatter,
  ScatterChart,
  RadialBarChart,
  RadialBar,
  FunnelChart,
  Funnel,
  LabelList
} from 'recharts';
import { motion } from 'framer-motion';

// Enhanced Interfaces for Business Intelligence
interface RevenueMetrics {
  mrr: number;
  arr: number;
  growth: number;
  churn: number;
  ltv: number;
  cac: number;
  paybackPeriod: number;
  netRevenue: number;
  recurringRevenue: number;
  oneTimeRevenue: number;
}

interface UserMetrics {
  totalUsers: number;
  activeUsers: number;
  newUsers: number;
  churnedUsers: number;
  retentionRate: number;
  engagementScore: number;
  conversionRate: number;
  averageSessionTime: number;
  sessionsPerUser: number;
  mobileUsers: number;
  premiumUsers: number;
}

interface TradingMetrics {
  totalTrades: number;
  tradeVolume: number;
  successRate: number;
  averageReturn: number;
  riskAdjustedReturn: number;
  volatility: number;
  sharpeRatio: number;
  maxDrawdown: number;
  profitFactor: number;
  winLossRatio: number;
  averageTradeSize: number;
}

interface ConversionFunnel {
  stage: string;
  visitors: number;
  conversions: number;
  rate: number;
  dropOff: number;
}

interface ABTestResult {
  id: string;
  name: string;
  status: 'running' | 'completed' | 'paused' | 'draft';
  startDate: Date;
  endDate?: Date;
  participants: number;
  conversionGoal: string;
  variants: ABTestVariant[];
  significance: number;
  winner?: string;
  impact: number;
}

interface ABTestVariant {
  id: string;
  name: string;
  traffic: number;
  conversions: number;
  conversionRate: number;
  improvement: number;
  confidence: number;
}

interface ChurnPrediction {
  userId: string;
  username: string;
  churnProbability: number;
  riskLevel: 'low' | 'medium' | 'high' | 'critical';
  factors: ChurnFactor[];
  lastActiveDate: Date;
  ltv: number;
  recommendedActions: string[];
}

interface ChurnFactor {
  factor: string;
  impact: number;
  description: string;
}

interface BusinessKPI {
  name: string;
  value: number;
  target: number;
  unit: string;
  trend: number;
  status: 'good' | 'warning' | 'critical';
  description: string;
  category: 'revenue' | 'growth' | 'user' | 'trading' | 'efficiency';
}

interface RevenueForecasting {
  period: string;
  predictedRevenue: number;
  confidence: number;
  factors: ForecastFactor[];
  scenarios: {
    optimistic: number;
    realistic: number;
    pessimistic: number;
  };
}

interface ForecastFactor {
  name: string;
  impact: number;
  confidence: number;
  description: string;
}

interface CohortAnalysis {
  cohort: string;
  size: number;
  retention: { [key: string]: number };
  ltv: number;
  cac: number;
  paybackMonths: number;
}

interface BusinessIntelligenceData {
  revenue: RevenueMetrics;
  users: UserMetrics;
  trading: TradingMetrics;
  kpis: BusinessKPI[];
  conversionFunnel: ConversionFunnel[];
  abTests: ABTestResult[];
  churnPredictions: ChurnPrediction[];
  forecasting: RevenueForecasting[];
  cohorts: CohortAnalysis[];
  historicalData: any[];
}

const BusinessIntelligenceDashboard: React.FC = () => {
  const [activeTab, setActiveTab] = useState(0);
  const [timeRange, setTimeRange] = useState('30d');
  const [biData, setBiData] = useState<BusinessIntelligenceData | null>(null);
  const [loading, setLoading] = useState(true);
  const [selectedMetrics, setSelectedMetrics] = useState<string[]>(['revenue', 'users', 'trading']);
  const [reportDialogOpen, setReportDialogOpen] = useState(false);
  const [alertsDialogOpen, setAlertsDialogOpen] = useState(false);
  const [selectedChurnUser, setSelectedChurnUser] = useState<ChurnPrediction | null>(null);

  // Color schemes for charts
  const colors = {
    primary: '#2196F3',
    secondary: '#FF9800',
    success: '#4CAF50',
    error: '#F44336',
    warning: '#FFC107',
    info: '#00BCD4',
    purple: '#9C27B0',
    indigo: '#3F51B5'
  };

  // Mock data generation
  const generateMockData = useCallback((): BusinessIntelligenceData => {
    const mockRevenue: RevenueMetrics = {
      mrr: 2485000, // Monthly Recurring Revenue
      arr: 29820000, // Annual Recurring Revenue
      growth: 18.5, // Growth rate %
      churn: 3.2, // Churn rate %
      ltv: 48500, // Lifetime Value
      cac: 8500, // Customer Acquisition Cost
      paybackPeriod: 6.2, // Months
      netRevenue: 2234500,
      recurringRevenue: 2150000,
      oneTimeRevenue: 335000
    };

    const mockUsers: UserMetrics = {
      totalUsers: 25487,
      activeUsers: 18756,
      newUsers: 1247,
      churnedUsers: 89,
      retentionRate: 94.8,
      engagementScore: 78.5,
      conversionRate: 12.4,
      averageSessionTime: 42.5, // minutes
      sessionsPerUser: 8.7,
      mobileUsers: 15842,
      premiumUsers: 4536
    };

    const mockTrading: TradingMetrics = {
      totalTrades: 1458760,
      tradeVolume: 245789000, // in rupees
      successRate: 68.4,
      averageReturn: 14.7,
      riskAdjustedReturn: 1.85,
      volatility: 18.3,
      sharpeRatio: 2.14,
      maxDrawdown: -8.7,
      profitFactor: 1.94,
      winLossRatio: 2.16,
      averageTradeSize: 168500
    };

    const mockKPIs: BusinessKPI[] = [
      {
        name: 'Monthly Recurring Revenue',
        value: 2485000,
        target: 2500000,
        unit: '₹',
        trend: 18.5,
        status: 'good',
        description: 'Steady growth in subscription revenue',
        category: 'revenue'
      },
      {
        name: 'Customer Acquisition Cost',
        value: 8500,
        target: 7000,
        unit: '₹',
        trend: -5.2,
        status: 'warning',
        description: 'CAC higher than target, optimize marketing',
        category: 'efficiency'
      },
      {
        name: 'User Retention Rate',
        value: 94.8,
        target: 90,
        unit: '%',
        trend: 2.3,
        status: 'good',
        description: 'Excellent retention above industry benchmark',
        category: 'user'
      },
      {
        name: 'Trading Success Rate',
        value: 68.4,
        target: 65,
        unit: '%',
        trend: 3.8,
        status: 'good',
        description: 'Users achieving consistent profitability',
        category: 'trading'
      },
      {
        name: 'Conversion Rate',
        value: 12.4,
        target: 15,
        unit: '%',
        trend: -1.2,
        status: 'warning',
        description: 'Conversion below target, review onboarding',
        category: 'growth'
      }
    ];

    const mockFunnel: ConversionFunnel[] = [
      { stage: 'Visitors', visitors: 45780, conversions: 45780, rate: 100, dropOff: 0 },
      { stage: 'Sign Up', visitors: 45780, conversions: 12547, rate: 27.4, dropOff: 72.6 },
      { stage: 'Email Verified', visitors: 12547, conversions: 10892, rate: 86.8, dropOff: 13.2 },
      { stage: 'Profile Complete', visitors: 10892, conversions: 8934, rate: 82.0, dropOff: 18.0 },
      { stage: 'First Trade', visitors: 8934, conversions: 5678, rate: 63.5, dropOff: 36.5 },
      { stage: 'Active User', visitors: 5678, conversions: 4521, rate: 79.6, dropOff: 20.4 },
      { stage: 'Premium Subscriber', visitors: 4521, conversions: 1247, rate: 27.6, dropOff: 72.4 }
    ];

    const mockABTests: ABTestResult[] = [
      {
        id: 'test_001',
        name: 'Onboarding Flow Optimization',
        status: 'running',
        startDate: new Date('2024-01-15'),
        participants: 2847,
        conversionGoal: 'Complete Profile',
        significance: 95.2,
        impact: 12.4,
        variants: [
          { id: 'control', name: 'Control', traffic: 50, conversions: 834, conversionRate: 58.6, improvement: 0, confidence: 0 },
          { id: 'variant_a', name: 'Simplified Flow', traffic: 50, conversions: 967, conversionRate: 67.9, improvement: 15.9, confidence: 95.2 }
        ]
      },
      {
        id: 'test_002',
        name: 'Premium Pricing Strategy',
        status: 'completed',
        startDate: new Date('2024-01-01'),
        endDate: new Date('2024-01-31'),
        participants: 5634,
        conversionGoal: 'Premium Subscription',
        significance: 98.7,
        winner: 'variant_b',
        impact: 23.8,
        variants: [
          { id: 'control', name: 'Current Pricing', traffic: 33, conversions: 234, conversionRate: 12.4, improvement: 0, confidence: 0 },
          { id: 'variant_a', name: 'Lower Price', traffic: 33, conversions: 267, conversionRate: 14.1, improvement: 13.7, confidence: 78.3 },
          { id: 'variant_b', name: 'Value Bundle', traffic: 34, conversions: 345, conversionRate: 18.2, improvement: 46.8, confidence: 98.7 }
        ]
      }
    ];

    const mockChurnPredictions: ChurnPrediction[] = [
      {
        userId: 'user_12345',
        username: 'TradePro2024',
        churnProbability: 89.5,
        riskLevel: 'critical',
        lastActiveDate: new Date('2024-01-20'),
        ltv: 25000,
        factors: [
          { factor: 'Low Trading Activity', impact: 35, description: 'No trades in last 14 days' },
          { factor: 'Support Tickets', impact: 25, description: 'Multiple unresolved issues' },
          { factor: 'Feature Usage Drop', impact: 20, description: 'Decreased platform engagement' },
          { factor: 'Payment Issues', impact: 20, description: 'Failed payment attempts' }
        ],
        recommendedActions: [
          'Personal outreach by account manager',
          'Offer trading consultation session',
          'Provide priority support',
          'Custom retention offer'
        ]
      },
      {
        userId: 'user_67890',
        username: 'InvestorABC',
        churnProbability: 72.3,
        riskLevel: 'high',
        lastActiveDate: new Date('2024-01-25'),
        ltv: 18500,
        factors: [
          { factor: 'Competitor Activity', impact: 40, description: 'Similar profiles churning' },
          { factor: 'Performance Issues', impact: 30, description: 'Trading losses last month' },
          { factor: 'Limited Feature Usage', impact: 30, description: 'Using basic features only' }
        ],
        recommendedActions: [
          'Send educational content',
          'Offer advanced features trial',
          'Performance improvement tips'
        ]
      }
    ];

    const mockForecasting: RevenueForecasting[] = [
      {
        period: 'Q1 2024',
        predictedRevenue: 7845000,
        confidence: 87.4,
        scenarios: { optimistic: 8234000, realistic: 7845000, pessimistic: 7156000 },
        factors: [
          { name: 'Seasonal Growth', impact: 15, confidence: 92, description: 'Historical Q1 growth pattern' },
          { name: 'New Feature Launch', impact: 8, confidence: 75, description: 'AI features driving premium subscriptions' },
          { name: 'Market Conditions', impact: -3, confidence: 68, description: 'Economic uncertainty impact' }
        ]
      },
      {
        period: 'Q2 2024',
        predictedRevenue: 9156000,
        confidence: 78.2,
        scenarios: { optimistic: 9834000, realistic: 9156000, pessimistic: 8445000 },
        factors: [
          { name: 'User Base Growth', impact: 12, confidence: 85, description: 'Expected user acquisition' },
          { name: 'Price Optimization', impact: 6, confidence: 80, description: 'A/B test results implementation' }
        ]
      }
    ];

    const mockCohorts: CohortAnalysis[] = [
      {
        cohort: 'Jan 2024',
        size: 1247,
        retention: { 'Month 0': 100, 'Month 1': 87, 'Month 2': 74, 'Month 3': 68 },
        ltv: 28500,
        cac: 7800,
        paybackMonths: 4.2
      },
      {
        cohort: 'Dec 2023',
        size: 1156,
        retention: { 'Month 0': 100, 'Month 1': 89, 'Month 2': 78, 'Month 3': 71, 'Month 4': 67 },
        ltv: 31200,
        cac: 8100,
        paybackMonths: 4.8
      }
    ];

    // Generate historical data for charts
    const mockHistoricalData = Array.from({ length: 30 }, (_, i) => ({
      date: new Date(Date.now() - (29 - i) * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
      revenue: 75000 + Math.random() * 25000,
      users: 850 + Math.random() * 200,
      trades: 12000 + Math.random() * 8000,
      conversion: 10 + Math.random() * 5,
      churn: 2 + Math.random() * 2,
      cac: 7000 + Math.random() * 2000,
      ltv: 45000 + Math.random() * 10000
    }));

    return {
      revenue: mockRevenue,
      users: mockUsers,
      trading: mockTrading,
      kpis: mockKPIs,
      conversionFunnel: mockFunnel,
      abTests: mockABTests,
      churnPredictions: mockChurnPredictions,
      forecasting: mockForecasting,
      cohorts: mockCohorts,
      historicalData: mockHistoricalData
    };
  }, []);

  useEffect(() => {
    const loadData = async () => {
      setLoading(true);
      // Simulate API call
      await new Promise(resolve => setTimeout(resolve, 1200));
      
      const data = generateMockData();
      setBiData(data);
      setLoading(false);
    };

    loadData();
  }, [generateMockData, timeRange]);

  const handleTabChange = (event: React.SyntheticEvent, newValue: number) => {
    setActiveTab(newValue);
  };

  const formatCurrency = (amount: number) => {
    if (amount >= 10000000) {
      return `₹${(amount / 10000000).toFixed(1)}Cr`;
    } else if (amount >= 100000) {
      return `₹${(amount / 100000).toFixed(1)}L`;
    } else if (amount >= 1000) {
      return `₹${(amount / 1000).toFixed(1)}K`;
    }
    return `₹${amount.toLocaleString()}`;
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'good': return colors.success;
      case 'warning': return colors.warning;
      case 'critical': return colors.error;
      default: return colors.primary;
    }
  };

  const getRiskColor = (riskLevel: string) => {
    switch (riskLevel) {
      case 'low': return colors.success;
      case 'medium': return colors.warning;
      case 'high': return colors.error;
      case 'critical': return '#B71C1C';
      default: return colors.primary;
    }
  };

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress size={60} />
      </Box>
    );
  }

  if (!biData) {
    return (
      <Alert severity="error">
        <AlertTitle>Error</AlertTitle>
        Failed to load business intelligence data. Please try again later.
      </Alert>
    );
  }

  return (
    <Box sx={{ p: 3 }}>
      {/* Header Section */}
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Box>
          <Typography variant="h4" fontWeight="bold" gutterBottom>
            Business Intelligence
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Executive dashboard with advanced analytics and insights
          </Typography>
        </Box>
        <Stack direction="row" spacing={2}>
          <FormControl size="small" sx={{ minWidth: 120 }}>
            <InputLabel>Time Range</InputLabel>
            <Select
              value={timeRange}
              label="Time Range"
              onChange={(e) => setTimeRange(e.target.value)}
            >
              <MenuItem value="7d">Last 7 days</MenuItem>
              <MenuItem value="30d">Last 30 days</MenuItem>
              <MenuItem value="90d">Last 3 months</MenuItem>
              <MenuItem value="1y">Last year</MenuItem>
            </Select>
          </FormControl>
          <Button variant="outlined" startIcon={<RefreshIcon />}>
            Refresh
          </Button>
          <Button variant="outlined" startIcon={<DownloadIcon />} onClick={() => setReportDialogOpen(true)}>
            Export Report
          </Button>
          <Button variant="contained" startIcon={<NotificationsIcon />} onClick={() => setAlertsDialogOpen(true)}>
            Alerts
          </Button>
        </Stack>
      </Box>

      {/* KPI Cards */}
      <Grid container spacing={3} sx={{ mb: 3 }}>
        {biData.kpis.slice(0, 5).map((kpi, index) => (
          <Grid item xs={12} sm={6} md={2.4} key={index}>
            <Card>
              <CardContent>
                <Box display="flex" alignItems="center" justifyContent="space-between" mb={1}>
                  <Typography variant="body2" color="text.secondary">
                    {kpi.name}
                  </Typography>
                  <Chip 
                    size="small" 
                    label={kpi.status} 
                    sx={{ bgcolor: getStatusColor(kpi.status), color: 'white' }}
                  />
                </Box>
                <Typography variant="h5" fontWeight="bold">
                  {kpi.unit === '₹' ? formatCurrency(kpi.value) : `${kpi.value.toLocaleString()}${kpi.unit}`}
                </Typography>
                <Box display="flex" alignItems="center" gap={1} mt={1}>
                  {kpi.trend >= 0 ? <TrendingUpIcon color="success" fontSize="small" /> : <TrendingDownIcon color="error" fontSize="small" />}
                  <Typography 
                    variant="caption" 
                    color={kpi.trend >= 0 ? 'success.main' : 'error.main'}
                  >
                    {kpi.trend >= 0 ? '+' : ''}{kpi.trend}%
                  </Typography>
                </Box>
                <LinearProgress
                  variant="determinate"
                  value={(kpi.value / kpi.target) * 100}
                  sx={{ mt: 1, height: 4, borderRadius: 2 }}
                />
                <Typography variant="caption" color="text.secondary">
                  Target: {kpi.unit === '₹' ? formatCurrency(kpi.target) : `${kpi.target.toLocaleString()}${kpi.unit}`}
                </Typography>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>

      {/* Navigation Tabs */}
      <Card sx={{ mb: 3 }}>
        <Tabs
          value={activeTab}
          onChange={handleTabChange}
          variant="scrollable"
          scrollButtons="auto"
        >
          <Tab icon={<AssessmentIcon />} label="Executive Overview" />
          <Tab icon={<MonetizationOnIcon />} label="Revenue Analytics" />
          <Tab icon={<PeopleIcon />} label="User Behavior" />
          <Tab icon={<TimelineIcon />} label="Conversion Funnel" />
          <Tab icon={<ScienceIcon />} label="A/B Testing" />
          <Tab icon={<WarningIcon />} label="Churn Prediction" />
          <Tab icon={<PredictionsIcon />} label="Forecasting" />
          <Tab icon={<GroupsIcon />} label="Cohort Analysis" />
        </Tabs>
      </Card>

      {/* Tab Content */}
      <Box>
        {/* Executive Overview Tab */}
        {activeTab === 0 && (
          <Grid container spacing={3}>
            {/* Revenue Overview */}
            <Grid item xs={12} md={8}>
              <Card>
                <CardContent>
                  <Typography variant="h6" gutterBottom display="flex" alignItems="center" gap={1}>
                    <AnalyticsIcon color="primary" />
                    Revenue Trends (Last 30 Days)
                  </Typography>
                  <ResponsiveContainer width="100%" height={300}>
                    <ComposedChart data={biData.historicalData}>
                      <CartesianGrid strokeDasharray="3 3" />
                      <XAxis dataKey="date" />
                      <YAxis yAxisId="revenue" />
                      <YAxis yAxisId="users" orientation="right" />
                      <RechartsTooltip 
                        formatter={(value: any, name: string) => [
                          name === 'revenue' ? formatCurrency(value) : value.toLocaleString(),
                          name.charAt(0).toUpperCase() + name.slice(1)
                        ]}
                      />
                      <Area yAxisId="revenue" type="monotone" dataKey="revenue" fill={colors.primary} fillOpacity={0.3} />
                      <Bar yAxisId="users" dataKey="users" fill={colors.secondary} />
                      <Line yAxisId="revenue" type="monotone" dataKey="revenue" stroke={colors.primary} strokeWidth={2} />
                    </ComposedChart>
                  </ResponsiveContainer>
                </CardContent>
              </Card>
            </Grid>

            {/* Key Metrics */}
            <Grid item xs={12} md={4}>
              <Grid container spacing={2}>
                <Grid item xs={12}>
                  <Card>
                    <CardContent>
                      <Typography variant="h6" gutterBottom color="primary">
                        Monthly Recurring Revenue
                      </Typography>
                      <Typography variant="h4" fontWeight="bold">
                        {formatCurrency(biData.revenue.mrr)}
                      </Typography>
                      <Typography variant="body2" color="success.main">
                        +{biData.revenue.growth}% growth
                      </Typography>
                    </CardContent>
                  </Card>
                </Grid>
                <Grid item xs={12}>
                  <Card>
                    <CardContent>
                      <Typography variant="h6" gutterBottom color="secondary">
                        Active Users
                      </Typography>
                      <Typography variant="h4" fontWeight="bold">
                        {biData.users.activeUsers.toLocaleString()}
                      </Typography>
                      <Typography variant="body2" color="text.secondary">
                        {biData.users.retentionRate}% retention
                      </Typography>
                    </CardContent>
                  </Card>
                </Grid>
                <Grid item xs={12}>
                  <Card>
                    <CardContent>
                      <Typography variant="h6" gutterBottom color="success.main">
                        Trading Volume
                      </Typography>
                      <Typography variant="h4" fontWeight="bold">
                        {formatCurrency(biData.trading.tradeVolume)}
                      </Typography>
                      <Typography variant="body2" color="text.secondary">
                        {biData.trading.successRate}% success rate
                      </Typography>
                    </CardContent>
                  </Card>
                </Grid>
              </Grid>
            </Grid>

            {/* Performance Metrics */}
            <Grid item xs={12}>
              <Card>
                <CardContent>
                  <Typography variant="h6" gutterBottom>
                    Key Performance Indicators
                  </Typography>
                  <Grid container spacing={3}>
                    <Grid item xs={12} md={3}>
                      <Box textAlign="center" p={2} sx={{ backgroundColor: '#f5f5f5', borderRadius: 2 }}>
                        <Typography variant="h4" fontWeight="bold" color="primary">
                          {biData.revenue.ltv / 1000}K
                        </Typography>
                        <Typography variant="body2">Customer LTV</Typography>
                        <Typography variant="caption" color="text.secondary">
                          vs CAC: {formatCurrency(biData.revenue.cac)}
                        </Typography>
                      </Box>
                    </Grid>
                    <Grid item xs={12} md={3}>
                      <Box textAlign="center" p={2} sx={{ backgroundColor: '#f5f5f5', borderRadius: 2 }}>
                        <Typography variant="h4" fontWeight="bold" color="secondary">
                          {biData.revenue.paybackPeriod}
                        </Typography>
                        <Typography variant="body2">Payback Period</Typography>
                        <Typography variant="caption" color="text.secondary">
                          months
                        </Typography>
                      </Box>
                    </Grid>
                    <Grid item xs={12} md={3}>
                      <Box textAlign="center" p={2} sx={{ backgroundColor: '#f5f5f5', borderRadius: 2 }}>
                        <Typography variant="h4" fontWeight="bold" color="success.main">
                          {biData.users.conversionRate}%
                        </Typography>
                        <Typography variant="body2">Conversion Rate</Typography>
                        <Typography variant="caption" color="text.secondary">
                          visitor to paid
                        </Typography>
                      </Box>
                    </Grid>
                    <Grid item xs={12} md={3}>
                      <Box textAlign="center" p={2} sx={{ backgroundColor: '#f5f5f5', borderRadius: 2 }}>
                        <Typography variant="h4" fontWeight="bold" color="error.main">
                          {biData.revenue.churn}%
                        </Typography>
                        <Typography variant="body2">Churn Rate</Typography>
                        <Typography variant="caption" color="text.secondary">
                          monthly
                        </Typography>
                      </Box>
                    </Grid>
                  </Grid>
                </CardContent>
              </Card>
            </Grid>
          </Grid>
        )}

        {/* Revenue Analytics Tab */}
        {activeTab === 1 && (
          <Grid container spacing={3}>
            {/* Revenue Breakdown */}
            <Grid item xs={12} md={8}>
              <Card>
                <CardContent>
                  <Typography variant="h6" gutterBottom>
                    Revenue Analysis
                  </Typography>
                  <ResponsiveContainer width="100%" height={300}>
                    <AreaChart data={biData.historicalData}>
                      <CartesianGrid strokeDasharray="3 3" />
                      <XAxis dataKey="date" />
                      <YAxis />
                      <RechartsTooltip formatter={(value) => formatCurrency(value as number)} />
                      <Area type="monotone" dataKey="revenue" stackId="1" stroke={colors.primary} fill={colors.primary} />
                    </AreaChart>
                  </ResponsiveContainer>
                </CardContent>
              </Card>
            </Grid>

            {/* Revenue Metrics */}
            <Grid item xs={12} md={4}>
              <Card>
                <CardContent>
                  <Typography variant="h6" gutterBottom>
                    Revenue Breakdown
                  </Typography>
                  <ResponsiveContainer width="100%" height={300}>
                    <PieChart>
                      <Pie
                        data={[
                          { name: 'Recurring', value: biData.revenue.recurringRevenue, color: colors.primary },
                          { name: 'One-time', value: biData.revenue.oneTimeRevenue, color: colors.secondary }
                        ]}
                        cx="50%"
                        cy="50%"
                        outerRadius={80}
                        fill="#8884d8"
                        dataKey="value"
                        label={({ name, percent }) => `${name}: ${(percent * 100).toFixed(0)}%`}
                      >
                        {[{ name: 'Recurring', value: biData.revenue.recurringRevenue }, { name: 'One-time', value: biData.revenue.oneTimeRevenue }]
                          .map((entry, index) => (
                          <Cell key={`cell-${index}`} fill={index === 0 ? colors.primary : colors.secondary} />
                        ))}
                      </Pie>
                      <RechartsTooltip formatter={(value) => formatCurrency(value as number)} />
                    </PieChart>
                  </ResponsiveContainer>
                </CardContent>
              </Card>
            </Grid>

            {/* Revenue Metrics Table */}
            <Grid item xs={12}>
              <Card>
                <CardContent>
                  <Typography variant="h6" gutterBottom>
                    Detailed Revenue Metrics
                  </Typography>
                  <TableContainer>
                    <Table>
                      <TableHead>
                        <TableRow>
                          <TableCell>Metric</TableCell>
                          <TableCell align="right">Value</TableCell>
                          <TableCell align="right">Target</TableCell>
                          <TableCell align="right">Trend</TableCell>
                          <TableCell>Status</TableCell>
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        <TableRow>
                          <TableCell>Monthly Recurring Revenue</TableCell>
                          <TableCell align="right">{formatCurrency(biData.revenue.mrr)}</TableCell>
                          <TableCell align="right">{formatCurrency(2500000)}</TableCell>
                          <TableCell align="right" sx={{ color: 'success.main' }}>+{biData.revenue.growth}%</TableCell>
                          <TableCell><Chip label="Good" color="success" size="small" /></TableCell>
                        </TableRow>
                        <TableRow>
                          <TableCell>Annual Recurring Revenue</TableCell>
                          <TableCell align="right">{formatCurrency(biData.revenue.arr)}</TableCell>
                          <TableCell align="right">{formatCurrency(30000000)}</TableCell>
                          <TableCell align="right" sx={{ color: 'success.main' }}>+{biData.revenue.growth}%</TableCell>
                          <TableCell><Chip label="Good" color="success" size="small" /></TableCell>
                        </TableRow>
                        <TableRow>
                          <TableCell>Customer Lifetime Value</TableCell>
                          <TableCell align="right">{formatCurrency(biData.revenue.ltv)}</TableCell>
                          <TableCell align="right">{formatCurrency(50000)}</TableCell>
                          <TableCell align="right" sx={{ color: 'success.main' }}>+5.2%</TableCell>
                          <TableCell><Chip label="Good" color="success" size="small" /></TableCell>
                        </TableRow>
                        <TableRow>
                          <TableCell>Customer Acquisition Cost</TableCell>
                          <TableCell align="right">{formatCurrency(biData.revenue.cac)}</TableCell>
                          <TableCell align="right">{formatCurrency(7000)}</TableCell>
                          <TableCell align="right" sx={{ color: 'error.main' }}>+8.5%</TableCell>
                          <TableCell><Chip label="Warning" color="warning" size="small" /></TableCell>
                        </TableRow>
                      </TableBody>
                    </Table>
                  </TableContainer>
                </CardContent>
              </Card>
            </Grid>
          </Grid>
        )}

        {/* User Behavior Tab */}
        {activeTab === 2 && (
          <Grid container spacing={3}>
            {/* User Growth */}
            <Grid item xs={12} md={6}>
              <Card>
                <CardContent>
                  <Typography variant="h6" gutterBottom>
                    User Growth Trend
                  </Typography>
                  <ResponsiveContainer width="100%" height={300}>
                    <LineChart data={biData.historicalData}>
                      <CartesianGrid strokeDasharray="3 3" />
                      <XAxis dataKey="date" />
                      <YAxis />
                      <RechartsTooltip />
                      <Line type="monotone" dataKey="users" stroke={colors.primary} strokeWidth={2} />
                    </LineChart>
                  </ResponsiveContainer>
                </CardContent>
              </Card>
            </Grid>

            {/* User Engagement */}
            <Grid item xs={12} md={6}>
              <Card>
                <CardContent>
                  <Typography variant="h6" gutterBottom>
                    User Engagement Metrics
                  </Typography>
                  <Stack spacing={3}>
                    <Box>
                      <Box display="flex" justifyContent="space-between" alignItems="center" mb={1}>
                        <Typography variant="body2">Engagement Score</Typography>
                        <Typography variant="body2" fontWeight="bold">
                          {biData.users.engagementScore}/100
                        </Typography>
                      </Box>
                      <LinearProgress
                        variant="determinate"
                        value={biData.users.engagementScore}
                        sx={{ height: 8, borderRadius: 4 }}
                      />
                    </Box>
                    <Box>
                      <Box display="flex" justifyContent="space-between" alignItems="center" mb={1}>
                        <Typography variant="body2">Session Duration</Typography>
                        <Typography variant="body2" fontWeight="bold">
                          {biData.users.averageSessionTime} min
                        </Typography>
                      </Box>
                      <LinearProgress
                        variant="determinate"
                        value={(biData.users.averageSessionTime / 60) * 100}
                        sx={{ height: 8, borderRadius: 4 }}
                      />
                    </Box>
                    <Box>
                      <Box display="flex" justifyContent="space-between" alignItems="center" mb={1}>
                        <Typography variant="body2">Sessions per User</Typography>
                        <Typography variant="body2" fontWeight="bold">
                          {biData.users.sessionsPerUser}
                        </Typography>
                      </Box>
                      <LinearProgress
                        variant="determinate"
                        value={(biData.users.sessionsPerUser / 10) * 100}
                        sx={{ height: 8, borderRadius: 4 }}
                      />
                    </Box>
                  </Stack>
                </CardContent>
              </Card>
            </Grid>

            {/* User Distribution */}
            <Grid item xs={12}>
              <Card>
                <CardContent>
                  <Typography variant="h6" gutterBottom>
                    User Distribution
                  </Typography>
                  <Grid container spacing={4}>
                    <Grid item xs={12} md={4}>
                      <Box textAlign="center">
                        <Typography variant="h4" fontWeight="bold" color="primary">
                          {biData.users.totalUsers.toLocaleString()}
                        </Typography>
                        <Typography variant="body2">Total Users</Typography>
                        <Typography variant="caption" color="success.main">
                          +{biData.users.newUsers.toLocaleString()} this month
                        </Typography>
                      </Box>
                    </Grid>
                    <Grid item xs={12} md={4}>
                      <Box textAlign="center">
                        <Typography variant="h4" fontWeight="bold" color="success.main">
                          {biData.users.premiumUsers.toLocaleString()}
                        </Typography>
                        <Typography variant="body2">Premium Users</Typography>
                        <Typography variant="caption" color="text.secondary">
                          {((biData.users.premiumUsers / biData.users.totalUsers) * 100).toFixed(1)}% conversion
                        </Typography>
                      </Box>
                    </Grid>
                    <Grid item xs={12} md={4}>
                      <Box textAlign="center">
                        <Typography variant="h4" fontWeight="bold" color="secondary">
                          {biData.users.mobileUsers.toLocaleString()}
                        </Typography>
                        <Typography variant="body2">Mobile Users</Typography>
                        <Typography variant="caption" color="text.secondary">
                          {((biData.users.mobileUsers / biData.users.totalUsers) * 100).toFixed(1)}% mobile traffic
                        </Typography>
                      </Box>
                    </Grid>
                  </Grid>
                </CardContent>
              </Card>
            </Grid>
          </Grid>
        )}

        {/* Conversion Funnel Tab */}
        {activeTab === 3 && (
          <Grid container spacing={3}>
            <Grid item xs={12} md={8}>
              <Card>
                <CardContent>
                  <Typography variant="h6" gutterBottom>
                    Conversion Funnel Analysis
                  </Typography>
                  <ResponsiveContainer width="100%" height={400}>
                    <FunnelChart>
                      <RechartsTooltip />
                      <Funnel
                        dataKey="conversions"
                        data={biData.conversionFunnel}
                        isAnimationActive
                      >
                        <LabelList position="center" fill="#fff" stroke="none" />
                      </Funnel>
                    </FunnelChart>
                  </ResponsiveContainer>
                </CardContent>
              </Card>
            </Grid>
            <Grid item xs={12} md={4}>
              <Card>
                <CardContent>
                  <Typography variant="h6" gutterBottom>
                    Funnel Metrics
                  </Typography>
                  <List>
                    {biData.conversionFunnel.map((stage, index) => (
                      <ListItem key={index} sx={{ py: 2 }}>
                        <ListItemText
                          primary={
                            <Box display="flex" justifyContent="space-between" alignItems="center">
                              <Typography variant="body1" fontWeight="bold">
                                {stage.stage}
                              </Typography>
                              <Chip 
                                label={`${stage.rate.toFixed(1)}%`} 
                                size="small" 
                                color={stage.rate > 50 ? 'success' : stage.rate > 25 ? 'warning' : 'error'}
                              />
                            </Box>
                          }
                          secondary={
                            <Box>
                              <Typography variant="body2" color="text.secondary">
                                {stage.conversions.toLocaleString()} conversions
                              </Typography>
                              {index > 0 && (
                                <Typography variant="caption" color="error.main">
                                  {stage.dropOff.toFixed(1)}% drop-off
                                </Typography>
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
          </Grid>
        )}

        {/* A/B Testing Tab */}
        {activeTab === 4 && (
          <Grid container spacing={3}>
            {biData.abTests.map((test, index) => (
              <Grid item xs={12} md={6} key={index}>
                <Card>
                  <CardContent>
                    <Box display="flex" justifyContent="space-between" alignItems="flex-start" mb={2}>
                      <Box>
                        <Typography variant="h6" fontWeight="bold">
                          {test.name}
                        </Typography>
                        <Typography variant="body2" color="text.secondary">
                          Goal: {test.conversionGoal}
                        </Typography>
                      </Box>
                      <Chip 
                        label={test.status} 
                        color={test.status === 'completed' ? 'success' : test.status === 'running' ? 'primary' : 'default'}
                        size="small"
                      />
                    </Box>
                    
                    <Typography variant="body2" gutterBottom>
                      Participants: {test.participants.toLocaleString()}
                    </Typography>
                    
                    {test.significance && (
                      <Typography variant="body2" gutterBottom>
                        Statistical Significance: {test.significance}%
                      </Typography>
                    )}
                    
                    <Divider sx={{ my: 2 }} />
                    
                    <Typography variant="subtitle2" gutterBottom>
                      Variants Performance:
                    </Typography>
                    
                    {test.variants.map((variant, vIndex) => (
                      <Box key={vIndex} sx={{ mb: 2 }}>
                        <Box display="flex" justifyContent="space-between" alignItems="center" mb={1}>
                          <Typography variant="body2">{variant.name}</Typography>
                          <Box display="flex" gap={1}>
                            <Chip 
                              label={`${variant.conversionRate.toFixed(1)}%`} 
                              size="small" 
                              color={variant.improvement > 0 ? 'success' : 'default'}
                            />
                            {variant.improvement !== 0 && (
                              <Chip 
                                label={`${variant.improvement > 0 ? '+' : ''}${variant.improvement.toFixed(1)}%`}
                                size="small"
                                color={variant.improvement > 0 ? 'success' : 'error'}
                              />
                            )}
                          </Box>
                        </Box>
                        {variant.confidence > 0 && (
                          <LinearProgress
                            variant="determinate"
                            value={variant.confidence}
                            sx={{ height: 6, borderRadius: 3 }}
                          />
                        )}
                      </Box>
                    ))}
                    
                    {test.winner && (
                      <Alert severity="success" sx={{ mt: 2 }}>
                        Winner: {test.variants.find(v => v.id === test.winner)?.name} 
                        {test.impact && ` (+${test.impact.toFixed(1)}% impact)`}
                      </Alert>
                    )}
                  </CardContent>
                </Card>
              </Grid>
            ))}
          </Grid>
        )}

        {/* Churn Prediction Tab */}
        {activeTab === 5 && (
          <Grid container spacing={3}>
            <Grid item xs={12} md={8}>
              <Card>
                <CardContent>
                  <Typography variant="h6" gutterBottom>
                    High-Risk Users for Churn
                  </Typography>
                  <TableContainer>
                    <Table>
                      <TableHead>
                        <TableRow>
                          <TableCell>User</TableCell>
                          <TableCell align="center">Risk Level</TableCell>
                          <TableCell align="right">Churn Probability</TableCell>
                          <TableCell align="right">LTV</TableCell>
                          <TableCell align="center">Last Active</TableCell>
                          <TableCell align="center">Actions</TableCell>
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {biData.churnPredictions.map((user, index) => (
                          <TableRow key={index}>
                            <TableCell>
                              <Box display="flex" alignItems="center" gap={2}>
                                <Avatar>{user.username.charAt(0)}</Avatar>
                                <Typography variant="body2" fontWeight="bold">
                                  {user.username}
                                </Typography>
                              </Box>
                            </TableCell>
                            <TableCell align="center">
                              <Chip 
                                label={user.riskLevel.toUpperCase()} 
                                size="small"
                                sx={{ bgcolor: getRiskColor(user.riskLevel), color: 'white' }}
                              />
                            </TableCell>
                            <TableCell align="right">
                              <Typography variant="body2" fontWeight="bold">
                                {user.churnProbability.toFixed(1)}%
                              </Typography>
                            </TableCell>
                            <TableCell align="right">
                              {formatCurrency(user.ltv)}
                            </TableCell>
                            <TableCell align="center">
                              <Typography variant="body2">
                                {user.lastActiveDate.toLocaleDateString()}
                              </Typography>
                            </TableCell>
                            <TableCell align="center">
                              <Button 
                                size="small" 
                                variant="outlined"
                                onClick={() => setSelectedChurnUser(user)}
                              >
                                View Details
                              </Button>
                            </TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </TableContainer>
                </CardContent>
              </Card>
            </Grid>
            <Grid item xs={12} md={4}>
              <Card>
                <CardContent>
                  <Typography variant="h6" gutterBottom>
                    Churn Risk Distribution
                  </Typography>
                  <ResponsiveContainer width="100%" height={200}>
                    <PieChart>
                      <Pie
                        data={[
                          { name: 'Low Risk', value: 15847, color: colors.success },
                          { name: 'Medium Risk', value: 4521, color: colors.warning },
                          { name: 'High Risk', value: 1247, color: colors.error },
                          { name: 'Critical Risk', value: 234, color: '#B71C1C' }
                        ]}
                        cx="50%"
                        cy="50%"
                        outerRadius={80}
                        dataKey="value"
                        label={({ name, percent }) => `${name}: ${(percent * 100).toFixed(0)}%`}
                      >
                        {[
                          { name: 'Low Risk', value: 15847 },
                          { name: 'Medium Risk', value: 4521 },
                          { name: 'High Risk', value: 1247 },
                          { name: 'Critical Risk', value: 234 }
                        ].map((entry, index) => (
                          <Cell 
                            key={`cell-${index}`} 
                            fill={[colors.success, colors.warning, colors.error, '#B71C1C'][index]} 
                          />
                        ))}
                      </Pie>
                      <RechartsTooltip />
                    </PieChart>
                  </ResponsiveContainer>
                </CardContent>
              </Card>
            </Grid>
          </Grid>
        )}

        {/* Forecasting Tab */}
        {activeTab === 6 && (
          <Grid container spacing={3}>
            <Grid item xs={12} md={8}>
              <Card>
                <CardContent>
                  <Typography variant="h6" gutterBottom>
                    Revenue Forecasting
                  </Typography>
                  <ResponsiveContainer width="100%" height={300}>
                    <ComposedChart 
                      data={[
                        { period: 'Q4 2023', actual: 6234000, predicted: null },
                        { period: 'Q1 2024', actual: null, predicted: 7845000, optimistic: 8234000, pessimistic: 7156000 },
                        { period: 'Q2 2024', actual: null, predicted: 9156000, optimistic: 9834000, pessimistic: 8445000 }
                      ]}
                    >
                      <CartesianGrid strokeDasharray="3 3" />
                      <XAxis dataKey="period" />
                      <YAxis />
                      <RechartsTooltip formatter={(value) => formatCurrency(value as number)} />
                      <Bar dataKey="actual" fill={colors.primary} />
                      <Line type="monotone" dataKey="predicted" stroke={colors.success} strokeWidth={3} strokeDasharray="5 5" />
                      <Line type="monotone" dataKey="optimistic" stroke={colors.info} strokeWidth={1} strokeDasharray="2 2" />
                      <Line type="monotone" dataKey="pessimistic" stroke={colors.error} strokeWidth={1} strokeDasharray="2 2" />
                    </ComposedChart>
                  </ResponsiveContainer>
                </CardContent>
              </Card>
            </Grid>
            <Grid item xs={12} md={4}>
              <Card>
                <CardContent>
                  <Typography variant="h6" gutterBottom>
                    Forecast Details
                  </Typography>
                  {biData.forecasting.map((forecast, index) => (
                    <Box key={index} sx={{ mb: 3 }}>
                      <Typography variant="subtitle1" fontWeight="bold">
                        {forecast.period}
                      </Typography>
                      <Typography variant="h5" color="primary">
                        {formatCurrency(forecast.predictedRevenue)}
                      </Typography>
                      <Typography variant="body2" color="text.secondary">
                        Confidence: {forecast.confidence}%
                      </Typography>
                      
                      <Box mt={2}>
                        <Typography variant="caption" display="block">
                          Optimistic: {formatCurrency(forecast.scenarios.optimistic)}
                        </Typography>
                        <Typography variant="caption" display="block">
                          Pessimistic: {formatCurrency(forecast.scenarios.pessimistic)}
                        </Typography>
                      </Box>
                      
                      <Divider sx={{ my: 2 }} />
                      
                      <Typography variant="caption" color="text.secondary">
                        Key Factors:
                      </Typography>
                      {forecast.factors.slice(0, 2).map((factor, fIndex) => (
                        <Typography key={fIndex} variant="caption" display="block">
                          • {factor.name}: {factor.impact > 0 ? '+' : ''}{factor.impact}%
                        </Typography>
                      ))}
                    </Box>
                  ))}
                </CardContent>
              </Card>
            </Grid>
          </Grid>
        )}

        {/* Cohort Analysis Tab */}
        {activeTab === 7 && (
          <Grid container spacing={3}>
            <Grid item xs={12}>
              <Card>
                <CardContent>
                  <Typography variant="h6" gutterBottom>
                    Cohort Retention Analysis
                  </Typography>
                  <TableContainer>
                    <Table>
                      <TableHead>
                        <TableRow>
                          <TableCell>Cohort</TableCell>
                          <TableCell align="right">Size</TableCell>
                          <TableCell align="center">Month 0</TableCell>
                          <TableCell align="center">Month 1</TableCell>
                          <TableCell align="center">Month 2</TableCell>
                          <TableCell align="center">Month 3</TableCell>
                          <TableCell align="center">Month 4</TableCell>
                          <TableCell align="right">LTV</TableCell>
                          <TableCell align="right">CAC</TableCell>
                          <TableCell align="right">Payback</TableCell>
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {biData.cohorts.map((cohort, index) => (
                          <TableRow key={index}>
                            <TableCell fontWeight="bold">{cohort.cohort}</TableCell>
                            <TableCell align="right">{cohort.size.toLocaleString()}</TableCell>
                            {Object.entries(cohort.retention).map(([month, retention], rIndex) => (
                              <TableCell key={rIndex} align="center">
                                <Box
                                  sx={{
                                    bgcolor: retention > 80 ? colors.success : retention > 60 ? colors.warning : colors.error,
                                    color: 'white',
                                    p: 1,
                                    borderRadius: 1,
                                    fontWeight: 'bold'
                                  }}
                                >
                                  {retention}%
                                </Box>
                              </TableCell>
                            ))}
                            <TableCell align="right">{formatCurrency(cohort.ltv)}</TableCell>
                            <TableCell align="right">{formatCurrency(cohort.cac)}</TableCell>
                            <TableCell align="right">{cohort.paybackMonths} mo</TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </TableContainer>
                </CardContent>
              </Card>
            </Grid>
          </Grid>
        )}
      </Box>

      {/* Churn User Details Dialog */}
      <Dialog
        open={!!selectedChurnUser}
        onClose={() => setSelectedChurnUser(null)}
        maxWidth="md"
        fullWidth
      >
        {selectedChurnUser && (
          <>
            <DialogTitle>
              <Box display="flex" alignItems="center" gap={2}>
                <Avatar>{selectedChurnUser.username.charAt(0)}</Avatar>
                <Box>
                  <Typography variant="h6">{selectedChurnUser.username}</Typography>
                  <Chip 
                    label={`${selectedChurnUser.churnProbability.toFixed(1)}% churn risk`}
                    size="small"
                    sx={{ bgcolor: getRiskColor(selectedChurnUser.riskLevel), color: 'white' }}
                  />
                </Box>
              </Box>
            </DialogTitle>
            <DialogContent>
              <Grid container spacing={3}>
                <Grid item xs={12} md={6}>
                  <Typography variant="subtitle1" gutterBottom>
                    Risk Factors
                  </Typography>
                  {selectedChurnUser.factors.map((factor, index) => (
                    <Box key={index} sx={{ mb: 2 }}>
                      <Box display="flex" justifyContent="space-between" alignItems="center" mb={1}>
                        <Typography variant="body2">{factor.factor}</Typography>
                        <Typography variant="body2" fontWeight="bold">
                          {factor.impact}%
                        </Typography>
                      </Box>
                      <LinearProgress
                        variant="determinate"
                        value={factor.impact}
                        sx={{ height: 6, borderRadius: 3 }}
                      />
                      <Typography variant="caption" color="text.secondary">
                        {factor.description}
                      </Typography>
                    </Box>
                  ))}
                </Grid>
                <Grid item xs={12} md={6}>
                  <Typography variant="subtitle1" gutterBottom>
                    Recommended Actions
                  </Typography>
                  <List>
                    {selectedChurnUser.recommendedActions.map((action, index) => (
                      <ListItem key={index}>
                        <ListItemIcon>
                          <TargetIcon color="primary" />
                        </ListItemIcon>
                        <ListItemText primary={action} />
                      </ListItem>
                    ))}
                  </List>
                  
                  <Box mt={3}>
                    <Typography variant="subtitle2" gutterBottom>
                      User Value
                    </Typography>
                    <Typography variant="body2">
                      LTV: {formatCurrency(selectedChurnUser.ltv)}
                    </Typography>
                    <Typography variant="body2">
                      Last Active: {selectedChurnUser.lastActiveDate.toLocaleDateString()}
                    </Typography>
                  </Box>
                </Grid>
              </Grid>
            </DialogContent>
            <DialogActions>
              <Button onClick={() => setSelectedChurnUser(null)}>Close</Button>
              <Button variant="contained">Create Retention Campaign</Button>
            </DialogActions>
          </>
        )}
      </Dialog>

      {/* Export Report Dialog */}
      <Dialog open={reportDialogOpen} onClose={() => setReportDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Export Business Intelligence Report</DialogTitle>
        <DialogContent>
          <Stack spacing={3} sx={{ mt: 1 }}>
            <FormControl fullWidth>
              <InputLabel>Report Type</InputLabel>
              <Select defaultValue="executive" label="Report Type">
                <MenuItem value="executive">Executive Summary</MenuItem>
                <MenuItem value="detailed">Detailed Analytics</MenuItem>
                <MenuItem value="financial">Financial Report</MenuItem>
                <MenuItem value="user">User Behavior Report</MenuItem>
              </Select>
            </FormControl>
            <FormControl fullWidth>
              <InputLabel>Format</InputLabel>
              <Select defaultValue="pdf" label="Format">
                <MenuItem value="pdf">PDF</MenuItem>
                <MenuItem value="excel">Excel</MenuItem>
                <MenuItem value="csv">CSV</MenuItem>
                <MenuItem value="powerpoint">PowerPoint</MenuItem>
              </Select>
            </FormControl>
            <FormControl fullWidth>
              <InputLabel>Date Range</InputLabel>
              <Select defaultValue="30d" label="Date Range">
                <MenuItem value="7d">Last 7 days</MenuItem>
                <MenuItem value="30d">Last 30 days</MenuItem>
                <MenuItem value="90d">Last 3 months</MenuItem>
                <MenuItem value="1y">Last year</MenuItem>
              </Select>
            </FormControl>
            <FormControlLabel
              control={<Switch defaultChecked />}
              label="Include charts and visualizations"
            />
            <FormControlLabel
              control={<Switch />}
              label="Include sensitive data"
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setReportDialogOpen(false)}>Cancel</Button>
          <Button variant="contained" startIcon={<DownloadIcon />}>
            Generate Report
          </Button>
        </DialogActions>
      </Dialog>

      {/* Alerts Dialog */}
      <Dialog open={alertsDialogOpen} onClose={() => setAlertsDialogOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>Business Intelligence Alerts</DialogTitle>
        <DialogContent>
          <Stack spacing={2}>
            <Alert severity="warning">
              <AlertTitle>CAC Threshold Exceeded</AlertTitle>
              Customer Acquisition Cost has increased to ₹8,500, exceeding target of ₹7,000. Consider optimizing marketing spend.
            </Alert>
            <Alert severity="info">
              <AlertTitle>Strong User Growth</AlertTitle>
              User growth is 15% above forecast. Current trajectory suggests Q1 targets will be exceeded.
            </Alert>
            <Alert severity="error">
              <AlertTitle>High Churn Risk Users</AlertTitle>
              2 users with LTV &gt; ₹20,000 are at critical churn risk. Immediate intervention recommended.
            </Alert>
            <Alert severity="success">
              <AlertTitle>Revenue Target Achievement</AlertTitle>
              MRR has reached 99.4% of monthly target with 5 days remaining in the period.
            </Alert>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setAlertsDialogOpen(false)}>Close</Button>
          <Button variant="contained">Configure Alerts</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default BusinessIntelligenceDashboard;