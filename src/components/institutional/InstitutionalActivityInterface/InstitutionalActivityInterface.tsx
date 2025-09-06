import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Box, Typography, Paper, Card, CardContent, Grid, Chip, Avatar,
  LinearProgress, CircularProgress, IconButton, Button, Tabs, Tab,
  List, ListItem, ListItemText, ListItemAvatar, ListItemIcon,
  Alert, AlertTitle, Dialog, DialogTitle, DialogContent, DialogActions,
  Switch, FormControlLabel, Slider, Badge, Tooltip, Table, TableBody,
  TableCell, TableContainer, TableHead, TableRow, TablePagination,
  Accordion, AccordionSummary, AccordionDetails, Divider, Select,
  MenuItem, FormControl, InputLabel, TextField, Autocomplete
} from '@mui/material';
import {
  TrendingUp as TrendingUpIcon,
  TrendingDown as TrendingDownIcon,
  ShowChart as ShowChartIcon,
  Assessment as AssessmentIcon,
  Notifications as NotificationsIcon,
  Settings as SettingsIcon,
  Business as BusinessIcon,
  AccountBalance as AccountBalanceIcon,
  MonetizationOn as MonetizationOnIcon,
  Speed as SpeedIcon,
  Timeline as TimelineIcon,
  BarChart as BarChartIcon,
  PieChart as PieChartIcon,
  Visibility as VisibilityIcon,
  VisibilityOff as VisibilityOffIcon,
  FilterList as FilterListIcon,
  Search as SearchIcon,
  Refresh as RefreshIcon,
  Download as DownloadIcon,
  Share as ShareIcon,
  Star as StarIcon,
  Warning as WarningIcon,
  CheckCircle as CheckCircleIcon,
  RadioButtonUnchecked as RadioButtonUncheckedIcon,
  FiberManualRecord as FiberManualRecordIcon,
  ExpandMore as ExpandMoreIcon,
  Close as CloseIcon,
  PlayArrow as PlayArrowIcon,
  Pause as PauseIcon,
  VolumeUp as VolumeUpIcon,
  TrendingFlat as TrendingFlatIcon,
  WorkspacePremium as WorkspacePremiumIcon,
  Psychology as PsychologyIcon
} from '@mui/icons-material';

// Types and Interfaces
interface InstitutionalFlow {
  id: string;
  timestamp: Date;
  symbol: string;
  sector: string;
  flowType: 'buying' | 'selling' | 'neutral';
  volume: number;
  value: number; // in crores
  intensity: 'low' | 'medium' | 'high' | 'extreme';
  confidence: number; // 0-100
  institution: {
    type: 'fii' | 'dii' | 'mutual_fund' | 'insurance' | 'pension_fund' | 'hedge_fund' | 'unknown';
    name?: string;
    size: 'small' | 'medium' | 'large' | 'whale';
  };
  impact: {
    priceChange: number;
    volumeImpact: number;
    marketCapImpact: number;
  };
  pattern: string;
  darkPoolActivity?: boolean;
  crossTrade?: boolean;
}

interface MarketHeatMap {
  symbol: string;
  sector: string;
  institutionalBuying: number;
  institutionalSelling: number;
  netFlow: number;
  priceChange: number;
  volumeRatio: number; // institutional volume / total volume
  alertLevel: 'none' | 'low' | 'medium' | 'high' | 'critical';
  trend: 'accumulation' | 'distribution' | 'neutral' | 'rotation';
  momentum: number; // -100 to +100
}

interface InstitutionalAlert {
  id: string;
  type: 'large_order' | 'unusual_volume' | 'dark_pool' | 'block_deal' | 'bulk_deal' | 'fii_activity' | 'dii_activity';
  symbol: string;
  title: string;
  description: string;
  timestamp: Date;
  severity: 'info' | 'warning' | 'critical';
  value: number;
  threshold: number;
  impact: 'positive' | 'negative' | 'neutral';
  actionable: boolean;
  relatedFlows: string[];
  isRead: boolean;
  effectiveness?: number;
}

interface VolumePattern {
  id: string;
  symbol: string;
  pattern: 'accumulation' | 'distribution' | 'breakout' | 'breakdown' | 'rotation' | 'squeeze';
  confidence: number;
  timeframe: '5m' | '15m' | '1h' | '4h' | '1d';
  startTime: Date;
  expectedDuration: number; // minutes
  volumeProfile: Array<{
    price: number;
    volume: number;
    institutionalRatio: number;
  }>;
  keyLevels: {
    support: number[];
    resistance: number[];
    institutionalInterest: number[];
  };
  priceTarget?: number;
  stopLoss?: number;
  riskReward?: number;
}

interface InstitutionalAnalytics {
  totalFiiFlow: number;
  totalDiiFlow: number;
  netInstitutionalFlow: number;
  sectorsActive: string[];
  topBuyingStocks: Array<{ symbol: string; flow: number }>;
  topSellingStocks: Array<{ symbol: string; flow: number }>;
  darkPoolActivity: number;
  blockDealsCount: number;
  bulkDealsCount: number;
  averageOrderSize: number;
  marketImpactScore: number;
  institutionalSentiment: 'bullish' | 'bearish' | 'neutral';
}

interface HeatMapCell {
  symbol: string;
  value: number;
  color: string;
  intensity: number;
  details: {
    netFlow: number;
    priceChange: number;
    volume: number;
    institutionalRatio: number;
  };
}

const InstitutionalActivityInterface: React.FC = () => {
  // State Management
  const [institutionalFlows, setInstitutionalFlows] = useState<InstitutionalFlow[]>([]);
  const [marketHeatMap, setMarketHeatMap] = useState<MarketHeatMap[]>([]);
  const [institutionalAlerts, setInstitutionalAlerts] = useState<InstitutionalAlert[]>([]);
  const [volumePatterns, setVolumePatterns] = useState<VolumePattern[]>([]);
  const [analytics, setAnalytics] = useState<InstitutionalAnalytics | null>(null);
  const [activeTab, setActiveTab] = useState(0);
  const [isRealTimeEnabled, setIsRealTimeEnabled] = useState(true);
  const [selectedTimeframe, setSelectedTimeframe] = useState<'5m' | '15m' | '1h' | '4h' | '1d'>('15m');
  const [filterSector, setFilterSector] = useState<string>('all');
  const [filterInstitution, setFilterInstitution] = useState<string>('all');
  const [searchSymbol, setSearchSymbol] = useState('');
  const [selectedAlert, setSelectedAlert] = useState<InstitutionalAlert | null>(null);
  const [showAlertDialog, setShowAlertDialog] = useState(false);
  const [heatMapView, setHeatMapView] = useState<'sector' | 'stock' | 'flow'>('sector');
  const [sortBy, setSortBy] = useState<'flow' | 'impact' | 'volume' | 'time'>('flow');
  const [sortDirection, setSortDirection] = useState<'asc' | 'desc'>('desc');

  // Mock Data Generation
  const generateMockInstitutionalFlows = (): InstitutionalFlow[] => {
    const symbols = ['RELIANCE', 'TCS', 'HDFCBANK', 'INFY', 'ICICIBANK', 'HINDUNILVR', 'ITC', 'SBIN', 'BHARTIARTL', 'ASIANPAINT'];
    const sectors = ['Banking', 'IT', 'Oil & Gas', 'FMCG', 'Telecom', 'Chemicals'];
    const institutionTypes: InstitutionalFlow['institution']['type'][] = ['fii', 'dii', 'mutual_fund', 'insurance', 'hedge_fund'];
    const patterns = ['Block Accumulation', 'Stealth Distribution', 'Momentum Building', 'Support Testing', 'Breakout Preparation'];
    
    return Array.from({ length: 50 }, (_, i) => ({
      id: `flow-${i}`,
      timestamp: new Date(Date.now() - Math.random() * 1000 * 60 * 60 * 8),
      symbol: symbols[Math.floor(Math.random() * symbols.length)],
      sector: sectors[Math.floor(Math.random() * sectors.length)],
      flowType: Math.random() > 0.5 ? 'buying' : 'selling',
      volume: Math.floor(Math.random() * 1000000) + 50000,
      value: Math.floor(Math.random() * 500) + 10,
      intensity: ['low', 'medium', 'high', 'extreme'][Math.floor(Math.random() * 4)] as any,
      confidence: Math.floor(Math.random() * 30) + 70,
      institution: {
        type: institutionTypes[Math.floor(Math.random() * institutionTypes.length)],
        name: `Institution ${i + 1}`,
        size: ['small', 'medium', 'large', 'whale'][Math.floor(Math.random() * 4)] as any
      },
      impact: {
        priceChange: (Math.random() - 0.5) * 10,
        volumeImpact: Math.random() * 50 + 10,
        marketCapImpact: Math.random() * 1000 + 100
      },
      pattern: patterns[Math.floor(Math.random() * patterns.length)],
      darkPoolActivity: Math.random() > 0.7,
      crossTrade: Math.random() > 0.8
    }));
  };

  const generateMockHeatMap = (): MarketHeatMap[] => {
    const symbols = ['RELIANCE', 'TCS', 'HDFCBANK', 'INFY', 'ICICIBANK', 'HINDUNILVR', 'ITC', 'SBIN', 'BHARTIARTL', 'ASIANPAINT',
                    'LT', 'WIPRO', 'MARUTI', 'HCLTECH', 'KOTAKBANK', 'BAJFINANCE', 'TITAN', 'NESTLEIND', 'ULTRACEMCO', 'POWERGRID'];
    const sectors = ['Banking', 'IT', 'Oil & Gas', 'FMCG', 'Telecom', 'Chemicals', 'Auto', 'Cement', 'Power'];
    
    return symbols.map(symbol => ({
      symbol,
      sector: sectors[Math.floor(Math.random() * sectors.length)],
      institutionalBuying: Math.random() * 1000 + 100,
      institutionalSelling: Math.random() * 800 + 50,
      netFlow: (Math.random() - 0.5) * 500,
      priceChange: (Math.random() - 0.5) * 8,
      volumeRatio: Math.random() * 0.6 + 0.1,
      alertLevel: ['none', 'low', 'medium', 'high', 'critical'][Math.floor(Math.random() * 5)] as any,
      trend: ['accumulation', 'distribution', 'neutral', 'rotation'][Math.floor(Math.random() * 4)] as any,
      momentum: (Math.random() - 0.5) * 200
    }));
  };

  const generateMockAlerts = (): InstitutionalAlert[] => {
    const symbols = ['RELIANCE', 'TCS', 'HDFCBANK', 'INFY', 'ICICIBANK'];
    const types: InstitutionalAlert['type'][] = ['large_order', 'unusual_volume', 'dark_pool', 'block_deal', 'fii_activity'];
    
    return Array.from({ length: 15 }, (_, i) => ({
      id: `alert-${i}`,
      type: types[Math.floor(Math.random() * types.length)],
      symbol: symbols[Math.floor(Math.random() * symbols.length)],
      title: `Unusual ${types[Math.floor(Math.random() * types.length)].replace('_', ' ')} Activity`,
      description: `Significant institutional activity detected in ${symbols[Math.floor(Math.random() * symbols.length)]}`,
      timestamp: new Date(Date.now() - Math.random() * 1000 * 60 * 60 * 4),
      severity: ['info', 'warning', 'critical'][Math.floor(Math.random() * 3)] as any,
      value: Math.random() * 1000 + 100,
      threshold: 500,
      impact: ['positive', 'negative', 'neutral'][Math.floor(Math.random() * 3)] as any,
      actionable: Math.random() > 0.5,
      relatedFlows: [`flow-${i}`, `flow-${i+1}`],
      isRead: Math.random() > 0.3,
      effectiveness: Math.floor(Math.random() * 30) + 70
    }));
  };

  const generateMockAnalytics = (): InstitutionalAnalytics => ({
    totalFiiFlow: Math.floor(Math.random() * 2000) - 1000,
    totalDiiFlow: Math.floor(Math.random() * 1500) - 750,
    netInstitutionalFlow: Math.floor(Math.random() * 1000) - 500,
    sectorsActive: ['Banking', 'IT', 'FMCG', 'Auto', 'Pharma'],
    topBuyingStocks: [
      { symbol: 'RELIANCE', flow: 345 },
      { symbol: 'TCS', flow: 287 },
      { symbol: 'HDFCBANK', flow: 234 }
    ],
    topSellingStocks: [
      { symbol: 'ICICIBANK', flow: -189 },
      { symbol: 'INFY', flow: -156 },
      { symbol: 'ITC', flow: -134 }
    ],
    darkPoolActivity: Math.random() * 100,
    blockDealsCount: Math.floor(Math.random() * 20),
    bulkDealsCount: Math.floor(Math.random() * 15),
    averageOrderSize: Math.floor(Math.random() * 50) + 10,
    marketImpactScore: Math.floor(Math.random() * 40) + 60,
    institutionalSentiment: ['bullish', 'bearish', 'neutral'][Math.floor(Math.random() * 3)] as any
  });

  // Initialize Mock Data
  useEffect(() => {
    setInstitutionalFlows(generateMockInstitutionalFlows());
    setMarketHeatMap(generateMockHeatMap());
    setInstitutionalAlerts(generateMockAlerts());
    setAnalytics(generateMockAnalytics());
  }, []);

  // Real-time updates simulation
  useEffect(() => {
    if (!isRealTimeEnabled) return;

    const interval = setInterval(() => {
      // Update flows
      const newFlow = generateMockInstitutionalFlows()[0];
      setInstitutionalFlows(prev => [newFlow, ...prev.slice(0, 49)]);

      // Update heat map
      setMarketHeatMap(prev => prev.map(item => ({
        ...item,
        netFlow: item.netFlow + (Math.random() - 0.5) * 10,
        priceChange: item.priceChange + (Math.random() - 0.5) * 0.5,
        momentum: Math.max(-100, Math.min(100, item.momentum + (Math.random() - 0.5) * 20))
      })));

      // Occasionally add new alerts
      if (Math.random() > 0.8) {
        const newAlert = generateMockAlerts()[0];
        setInstitutionalAlerts(prev => [newAlert, ...prev.slice(0, 14)]);
      }
    }, 15000); // Update every 15 seconds

    return () => clearInterval(interval);
  }, [isRealTimeEnabled]);

  // Filtered and sorted data
  const filteredFlows = useMemo(() => {
    let filtered = institutionalFlows;

    if (filterSector !== 'all') {
      filtered = filtered.filter(flow => flow.sector === filterSector);
    }

    if (filterInstitution !== 'all') {
      filtered = filtered.filter(flow => flow.institution.type === filterInstitution);
    }

    if (searchSymbol) {
      filtered = filtered.filter(flow => 
        flow.symbol.toLowerCase().includes(searchSymbol.toLowerCase())
      );
    }

    // Sort
    filtered.sort((a, b) => {
      let aVal, bVal;
      switch (sortBy) {
        case 'flow':
          aVal = a.value;
          bVal = b.value;
          break;
        case 'impact':
          aVal = Math.abs(a.impact.priceChange);
          bVal = Math.abs(b.impact.priceChange);
          break;
        case 'volume':
          aVal = a.volume;
          bVal = b.volume;
          break;
        case 'time':
          aVal = a.timestamp.getTime();
          bVal = b.timestamp.getTime();
          break;
        default:
          aVal = a.value;
          bVal = b.value;
      }

      return sortDirection === 'desc' ? bVal - aVal : aVal - bVal;
    });

    return filtered;
  }, [institutionalFlows, filterSector, filterInstitution, searchSymbol, sortBy, sortDirection]);

  // Helper Functions
  const getFlowColor = (flowType: InstitutionalFlow['flowType']) => {
    switch (flowType) {
      case 'buying': return '#4caf50';
      case 'selling': return '#f44336';
      default: return '#9e9e9e';
    }
  };

  const getIntensityColor = (intensity: InstitutionalFlow['intensity']) => {
    switch (intensity) {
      case 'extreme': return '#d32f2f';
      case 'high': return '#f57c00';
      case 'medium': return '#fbc02d';
      case 'low': return '#388e3c';
    }
  };

  const getAlertSeverityColor = (severity: InstitutionalAlert['severity']) => {
    switch (severity) {
      case 'critical': return 'error';
      case 'warning': return 'warning';
      case 'info': return 'info';
    }
  };

  const getInstitutionIcon = (type: InstitutionalFlow['institution']['type']) => {
    switch (type) {
      case 'fii': return <BusinessIcon />;
      case 'dii': return <AccountBalanceIcon />;
      case 'mutual_fund': return <WorkspacePremiumIcon />;
      case 'insurance': return <AccountBalanceIcon />;
      case 'hedge_fund': return <TrendingUpIcon />;
      default: return <BusinessIcon />;
    }
  };

  const handleMarkAlertRead = (alertId: string) => {
    setInstitutionalAlerts(prev => 
      prev.map(alert => 
        alert.id === alertId ? { ...alert, isRead: true } : alert
      )
    );
  };

  const tabs = [
    { label: 'Live Activity Feed', icon: <TimelineIcon /> },
    { label: 'Market Heat Map', icon: <BarChartIcon /> },
    { label: 'Volume Patterns', icon: <ShowChartIcon /> },
    { label: 'Alerts & Notifications', icon: <NotificationsIcon /> },
    { label: 'Analytics Dashboard', icon: <AssessmentIcon /> },
    { label: 'Dark Pool Activity', icon: <VisibilityOffIcon /> }
  ];

  return (
    <Box sx={{ flexGrow: 1, p: 3, backgroundColor: 'background.default', minHeight: '100vh' }}>
      {/* Header */}
      <Box sx={{ mb: 4 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
          <Box>
            <Typography variant="h4" sx={{ fontWeight: 600, display: 'flex', alignItems: 'center', gap: 1 }}>
              <BusinessIcon color="primary" sx={{ fontSize: 36 }} />
              Institutional Activity Interface
            </Typography>
            <Typography variant="subtitle1" color="text.secondary">
              Real-time institutional flow detection and market intelligence
            </Typography>
          </Box>
          
          <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
            <FormControlLabel
              control={
                <Switch
                  checked={isRealTimeEnabled}
                  onChange={(e) => setIsRealTimeEnabled(e.target.checked)}
                  color="primary"
                />
              }
              label="Real-time"
            />
            <Button
              variant="outlined"
              startIcon={<DownloadIcon />}
              onClick={() => alert('Export feature coming soon!')}
            >
              Export
            </Button>
            <Button
              variant="outlined"
              startIcon={<SettingsIcon />}
              onClick={() => alert('Settings coming soon!')}
            >
              Settings
            </Button>
            <Button
              variant="contained"
              startIcon={<RefreshIcon />}
              onClick={() => window.location.reload()}
            >
              Refresh
            </Button>
          </Box>
        </Box>

        {/* Quick Stats */}
        {analytics && (
          <Grid container spacing={2}>
            <Grid item xs={12} sm={6} md={2}>
              <Card sx={{ p: 2, textAlign: 'center' }}>
                <Typography variant="h6" color={analytics.totalFiiFlow >= 0 ? 'success.main' : 'error.main'} sx={{ fontWeight: 600 }}>
                  ₹{Math.abs(analytics.totalFiiFlow)}Cr
                </Typography>
                <Typography variant="caption" color="text.secondary">
                  FII Flow
                </Typography>
                {analytics.totalFiiFlow >= 0 ? <TrendingUpIcon color="success" /> : <TrendingDownIcon color="error" />}
              </Card>
            </Grid>
            <Grid item xs={12} sm={6} md={2}>
              <Card sx={{ p: 2, textAlign: 'center' }}>
                <Typography variant="h6" color={analytics.totalDiiFlow >= 0 ? 'success.main' : 'error.main'} sx={{ fontWeight: 600 }}>
                  ₹{Math.abs(analytics.totalDiiFlow)}Cr
                </Typography>
                <Typography variant="caption" color="text.secondary">
                  DII Flow
                </Typography>
                {analytics.totalDiiFlow >= 0 ? <TrendingUpIcon color="success" /> : <TrendingDownIcon color="error" />}
              </Card>
            </Grid>
            <Grid item xs={12} sm={6} md={2}>
              <Card sx={{ p: 2, textAlign: 'center' }}>
                <Typography variant="h6" color="primary" sx={{ fontWeight: 600 }}>
                  {analytics.blockDealsCount}
                </Typography>
                <Typography variant="caption" color="text.secondary">
                  Block Deals
                </Typography>
              </Card>
            </Grid>
            <Grid item xs={12} sm={6} md={2}>
              <Card sx={{ p: 2, textAlign: 'center' }}>
                <Typography variant="h6" color="secondary.main" sx={{ fontWeight: 600 }}>
                  {Math.round(analytics.darkPoolActivity)}%
                </Typography>
                <Typography variant="caption" color="text.secondary">
                  Dark Pool
                </Typography>
              </Card>
            </Grid>
            <Grid item xs={12} sm={6} md={2}>
              <Card sx={{ p: 2, textAlign: 'center' }}>
                <Typography variant="h6" color="info.main" sx={{ fontWeight: 600 }}>
                  {institutionalAlerts.filter(a => !a.isRead).length}
                </Typography>
                <Typography variant="caption" color="text.secondary">
                  New Alerts
                </Typography>
              </Card>
            </Grid>
            <Grid item xs={12} sm={6} md={2}>
              <Card sx={{ p: 2, textAlign: 'center' }}>
                <Typography variant="h6" color="text.primary" sx={{ fontWeight: 600 }}>
                  {analytics.institutionalSentiment.charAt(0).toUpperCase() + analytics.institutionalSentiment.slice(1)}
                </Typography>
                <Typography variant="caption" color="text.secondary">
                  Sentiment
                </Typography>
              </Card>
            </Grid>
          </Grid>
        )}
      </Box>

      {/* Tab Navigation */}
      <Paper sx={{ mb: 3 }}>
        <Tabs 
          value={activeTab} 
          onChange={(_, newValue) => setActiveTab(newValue)}
          variant="scrollable"
          scrollButtons="auto"
        >
          {tabs.map((tab, index) => (
            <Tab
              key={index}
              label={tab.label}
              icon={tab.icon}
              iconPosition="start"
            />
          ))}
        </Tabs>
      </Paper>

      {/* Tab Content */}
      <AnimatePresence mode="wait">
        {activeTab === 0 && (
          <motion.div
            key="activity-feed"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -20 }}
          >
            <LiveActivityFeed
              flows={filteredFlows}
              onFlowSelect={(flow) => alert(`Flow details for ${flow.symbol} coming soon!`)}
              filters={{
                sector: filterSector,
                institution: filterInstitution,
                search: searchSymbol,
                sortBy,
                sortDirection
              }}
              onFiltersChange={{
                setSector: setFilterSector,
                setInstitution: setFilterInstitution,
                setSearch: setSearchSymbol,
                setSortBy,
                setSortDirection
              }}
            />
          </motion.div>
        )}

        {activeTab === 1 && (
          <motion.div
            key="heat-map"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -20 }}
          >
            <MarketHeatMapView
              heatMapData={marketHeatMap}
              viewType={heatMapView}
              onViewTypeChange={setHeatMapView}
              onCellSelect={(cell) => alert(`Heat map details for ${cell.symbol} coming soon!`)}
            />
          </motion.div>
        )}

        {activeTab === 2 && (
          <motion.div
            key="volume-patterns"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -20 }}
          >
            <VolumePatternsView
              patterns={volumePatterns}
              timeframe={selectedTimeframe}
              onTimeframeChange={setSelectedTimeframe}
            />
          </motion.div>
        )}

        {activeTab === 3 && (
          <motion.div
            key="alerts"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -20 }}
          >
            <AlertsNotificationsView
              alerts={institutionalAlerts}
              onAlertSelect={setSelectedAlert}
              onMarkRead={handleMarkAlertRead}
            />
          </motion.div>
        )}

        {activeTab === 4 && (
          <motion.div
            key="analytics"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -20 }}
          >
            <AnalyticsDashboardView
              analytics={analytics}
              flows={institutionalFlows}
              timeframe={selectedTimeframe}
            />
          </motion.div>
        )}

        {activeTab === 5 && (
          <motion.div
            key="dark-pool"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -20 }}
          >
            <DarkPoolActivityView
              flows={institutionalFlows.filter(f => f.darkPoolActivity)}
              analytics={analytics}
            />
          </motion.div>
        )}
      </AnimatePresence>

      {/* Alert Details Dialog */}
      <Dialog
        open={showAlertDialog}
        onClose={() => setShowAlertDialog(false)}
        maxWidth="md"
        fullWidth
      >
        <DialogTitle>
          <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <Box>
              <Typography variant="h6">{selectedAlert?.title}</Typography>
              <Typography variant="body2" color="text.secondary">
                {selectedAlert?.symbol} - {selectedAlert?.timestamp.toLocaleString()}
              </Typography>
            </Box>
            <IconButton onClick={() => setShowAlertDialog(false)}>
              <CloseIcon />
            </IconButton>
          </Box>
        </DialogTitle>
        <DialogContent>
          {selectedAlert && (
            <AlertDetailsContent alert={selectedAlert} />
          )}
        </DialogContent>
      </Dialog>
    </Box>
  );
};

// Live Activity Feed Component
interface LiveActivityFeedProps {
  flows: InstitutionalFlow[];
  onFlowSelect: (flow: InstitutionalFlow) => void;
  filters: {
    sector: string;
    institution: string;
    search: string;
    sortBy: string;
    sortDirection: string;
  };
  onFiltersChange: {
    setSector: (sector: string) => void;
    setInstitution: (institution: string) => void;
    setSearch: (search: string) => void;
    setSortBy: (sortBy: any) => void;
    setSortDirection: (direction: any) => void;
  };
}

const LiveActivityFeed: React.FC<LiveActivityFeedProps> = ({
  flows,
  onFlowSelect,
  filters,
  onFiltersChange
}) => {
  const getFlowColor = (flowType: InstitutionalFlow['flowType']) => {
    switch (flowType) {
      case 'buying': return '#4caf50';
      case 'selling': return '#f44336';
      default: return '#9e9e9e';
    }
  };

  const getIntensityColor = (intensity: InstitutionalFlow['intensity']) => {
    switch (intensity) {
      case 'extreme': return '#d32f2f';
      case 'high': return '#f57c00';
      case 'medium': return '#fbc02d';
      case 'low': return '#388e3c';
    }
  };

  return (
    <Box>
      {/* Filters */}
      <Paper sx={{ p: 3, mb: 3 }}>
        <Typography variant="h6" gutterBottom>Filters & Search</Typography>
        <Grid container spacing={2} alignItems="center">
          <Grid item xs={12} sm={6} md={3}>
            <TextField
              fullWidth
              label="Search Symbol"
              value={filters.search}
              onChange={(e) => onFiltersChange.setSearch(e.target.value)}
              InputProps={{
                startAdornment: <SearchIcon sx={{ color: 'action.active', mr: 1 }} />
              }}
            />
          </Grid>
          <Grid item xs={12} sm={6} md={2}>
            <FormControl fullWidth>
              <InputLabel>Sector</InputLabel>
              <Select
                value={filters.sector}
                label="Sector"
                onChange={(e) => onFiltersChange.setSector(e.target.value)}
              >
                <MenuItem value="all">All Sectors</MenuItem>
                <MenuItem value="Banking">Banking</MenuItem>
                <MenuItem value="IT">IT</MenuItem>
                <MenuItem value="Oil & Gas">Oil & Gas</MenuItem>
                <MenuItem value="FMCG">FMCG</MenuItem>
                <MenuItem value="Telecom">Telecom</MenuItem>
              </Select>
            </FormControl>
          </Grid>
          <Grid item xs={12} sm={6} md={2}>
            <FormControl fullWidth>
              <InputLabel>Institution</InputLabel>
              <Select
                value={filters.institution}
                label="Institution"
                onChange={(e) => onFiltersChange.setInstitution(e.target.value)}
              >
                <MenuItem value="all">All Types</MenuItem>
                <MenuItem value="fii">FII</MenuItem>
                <MenuItem value="dii">DII</MenuItem>
                <MenuItem value="mutual_fund">Mutual Fund</MenuItem>
                <MenuItem value="insurance">Insurance</MenuItem>
                <MenuItem value="hedge_fund">Hedge Fund</MenuItem>
              </Select>
            </FormControl>
          </Grid>
          <Grid item xs={12} sm={6} md={2}>
            <FormControl fullWidth>
              <InputLabel>Sort By</InputLabel>
              <Select
                value={filters.sortBy}
                label="Sort By"
                onChange={(e) => onFiltersChange.setSortBy(e.target.value)}
              >
                <MenuItem value="flow">Flow Value</MenuItem>
                <MenuItem value="impact">Price Impact</MenuItem>
                <MenuItem value="volume">Volume</MenuItem>
                <MenuItem value="time">Time</MenuItem>
              </Select>
            </FormControl>
          </Grid>
          <Grid item xs={12} sm={6} md={1}>
            <IconButton
              onClick={() => onFiltersChange.setSortDirection(filters.sortDirection === 'desc' ? 'asc' : 'desc')}
              color="primary"
            >
              {filters.sortDirection === 'desc' ? <TrendingDownIcon /> : <TrendingUpIcon />}
            </IconButton>
          </Grid>
        </Grid>
      </Paper>

      {/* Activity Feed */}
      <Grid container spacing={2}>
        {flows.slice(0, 20).map((flow) => (
          <Grid item xs={12} key={flow.id}>
            <Card 
              sx={{ 
                p: 2, 
                cursor: 'pointer', 
                '&:hover': { backgroundColor: 'action.hover' },
                borderLeft: `4px solid ${getFlowColor(flow.flowType)}`
              }}
              onClick={() => onFlowSelect(flow)}
            >
              <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                  <Avatar sx={{ bgcolor: getFlowColor(flow.flowType) }}>
                    {flow.flowType === 'buying' ? <TrendingUpIcon /> : <TrendingDownIcon />}
                  </Avatar>
                  
                  <Box>
                    <Typography variant="h6" sx={{ fontWeight: 600 }}>
                      {flow.symbol}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      {flow.sector} • {flow.institution.type.toUpperCase()}
                    </Typography>
                  </Box>
                </Box>

                <Box sx={{ display: 'flex', alignItems: 'center', gap: 3 }}>
                  <Box sx={{ textAlign: 'right' }}>
                    <Typography variant="h6" sx={{ color: getFlowColor(flow.flowType), fontWeight: 600 }}>
                      ₹{flow.value}Cr
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      {flow.volume.toLocaleString()} shares
                    </Typography>
                  </Box>

                  <Box sx={{ textAlign: 'center' }}>
                    <Chip 
                      label={flow.intensity}
                      size="small"
                      sx={{ 
                        backgroundColor: getIntensityColor(flow.intensity) + '20',
                        color: getIntensityColor(flow.intensity),
                        fontWeight: 600
                      }}
                    />
                    <Typography variant="caption" display="block" color="text.secondary">
                      {flow.confidence}% confidence
                    </Typography>
                  </Box>

                  <Box sx={{ textAlign: 'right' }}>
                    <Typography variant="body2" sx={{ 
                      color: flow.impact.priceChange >= 0 ? 'success.main' : 'error.main',
                      fontWeight: 600
                    }}>
                      {flow.impact.priceChange >= 0 ? '+' : ''}{flow.impact.priceChange.toFixed(2)}%
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      {flow.timestamp.toLocaleTimeString()}
                    </Typography>
                  </Box>

                  {(flow.darkPoolActivity || flow.crossTrade) && (
                    <Box sx={{ display: 'flex', gap: 0.5 }}>
                      {flow.darkPoolActivity && (
                        <Tooltip title="Dark Pool Activity">
                          <VisibilityOffIcon fontSize="small" color="warning" />
                        </Tooltip>
                      )}
                      {flow.crossTrade && (
                        <Tooltip title="Cross Trade">
                          <CompareArrowsIcon fontSize="small" color="info" />
                        </Tooltip>
                      )}
                    </Box>
                  )}
                </Box>
              </Box>
            </Card>
          </Grid>
        ))}
      </Grid>
    </Box>
  );
};

// Placeholder components for other tabs
const MarketHeatMapView: React.FC<any> = () => <Box>Market heat map visualization coming soon...</Box>;
const VolumePatternsView: React.FC<any> = () => <Box>Volume patterns analysis coming soon...</Box>;
const AlertsNotificationsView: React.FC<any> = () => <Box>Alerts and notifications coming soon...</Box>;
const AnalyticsDashboardView: React.FC<any> = () => <Box>Analytics dashboard coming soon...</Box>;
const DarkPoolActivityView: React.FC<any> = () => <Box>Dark pool activity analysis coming soon...</Box>;
const AlertDetailsContent: React.FC<any> = () => <Box>Alert details coming soon...</Box>;

export default InstitutionalActivityInterface;