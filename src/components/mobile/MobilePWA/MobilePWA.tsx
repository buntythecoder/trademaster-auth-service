import React, { useState, useEffect, useCallback } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Button,
  IconButton,
  Fab,
  Drawer,
  List,
  ListItem,
  ListItemText,
  ListItemIcon,
  SwipeableDrawer,
  BottomNavigation,
  BottomNavigationAction,
  SpeedDial,
  SpeedDialAction,
  SpeedDialIcon,
  Alert,
  Snackbar,
  Paper,
  Grid,
  Chip,
  Avatar,
  Badge,
  LinearProgress,
  CircularProgress,
  AppBar,
  Toolbar,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Slide,
  Zoom,
  Collapse
} from '@mui/material';
import {
  HomeIcon,
  TrendingUpIcon,
  AccountBalanceWalletIcon,
  NotificationsIcon,
  SettingsIcon,
  MenuIcon,
  FlashOnIcon,
  SearchIcon,
  RefreshIcon,
  WifiOffIcon,
  CloudOffIcon,
  SyncIcon,
  AddIcon,
  RemoveIcon,
  PlayArrowIcon,
  OfflineBoltIcon,
  InstallMobileIcon,
  GetAppIcon,
  ShareIcon,
  BookmarkIcon,
  StarIcon,
  TrendingDownIcon,
  ShowChartIcon,
  PieChartIcon,
  BarChartIcon,
  TimelineIcon,
  AssessmentIcon,
  BusinessCenterIcon
} from '@mui/icons-material';
import { motion, AnimatePresence, PanInfo } from 'framer-motion';
import { TransitionProps } from '@mui/material/transitions';

// Enhanced Mobile PWA Interfaces
interface PWAInstallPrompt {
  platforms: string[];
  userChoice: 'accepted' | 'dismissed' | null;
  prompt: () => Promise<void>;
}

interface OfflineState {
  isOnline: boolean;
  lastSync: Date;
  pendingActions: OfflineAction[];
  cachedData: CachedData;
  syncStatus: 'idle' | 'syncing' | 'error' | 'success';
}

interface OfflineAction {
  id: string;
  type: 'order' | 'watchlist' | 'alert' | 'portfolio';
  data: any;
  timestamp: Date;
  retryCount: number;
}

interface CachedData {
  portfolio: any[];
  watchlists: any[];
  marketData: any[];
  orders: any[];
  lastUpdate: Date;
}

interface MobileQuickAction {
  id: string;
  label: string;
  icon: React.ReactNode;
  action: () => void;
  color?: string;
  disabled?: boolean;
}

interface TouchGesture {
  startX: number;
  startY: number;
  deltaX: number;
  deltaY: number;
  direction: 'left' | 'right' | 'up' | 'down' | null;
  velocity: number;
}

interface PushNotification {
  id: string;
  title: string;
  body: string;
  icon?: string;
  data?: any;
  timestamp: Date;
  actions?: NotificationAction[];
}

interface NotificationAction {
  action: string;
  title: string;
  icon?: string;
}

interface MobileWidget {
  id: string;
  type: 'portfolio' | 'watchlist' | 'chart' | 'orders' | 'news';
  title: string;
  size: 'small' | 'medium' | 'large';
  position: { x: number; y: number };
  data: any;
  visible: boolean;
}

interface ServiceWorkerUpdate {
  waiting: ServiceWorker | null;
  updateAvailable: boolean;
  updating: boolean;
}

const Transition = React.forwardRef(function Transition(
  props: TransitionProps & {
    children: React.ReactElement;
  },
  ref: React.Ref<unknown>
) {
  return <Slide direction="up" ref={ref} {...props} />;
});

const MobilePWA: React.FC = () => {
  const [bottomNavValue, setBottomNavValue] = useState(0);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [speedDialOpen, setSpeedDialOpen] = useState(false);
  const [installPrompt, setInstallPrompt] = useState<PWAInstallPrompt | null>(null);
  const [offlineState, setOfflineState] = useState<OfflineState>({
    isOnline: navigator.onLine,
    lastSync: new Date(),
    pendingActions: [],
    cachedData: {
      portfolio: [],
      watchlists: [],
      marketData: [],
      orders: [],
      lastUpdate: new Date()
    },
    syncStatus: 'idle'
  });
  const [notifications, setNotifications] = useState<PushNotification[]>([]);
  const [widgets, setWidgets] = useState<MobileWidget[]>([]);
  const [serviceWorker, setServiceWorker] = useState<ServiceWorkerUpdate>({
    waiting: null,
    updateAvailable: false,
    updating: false
  });
  const [quickTradeOpen, setQuickTradeOpen] = useState(false);
  const [touchGesture, setTouchGesture] = useState<TouchGesture | null>(null);
  const [isFullscreen, setIsFullscreen] = useState(false);
  const [batteryLevel, setBatteryLevel] = useState<number | null>(null);
  const [networkInfo, setNetworkInfo] = useState<any>(null);

  // PWA Installation handling
  useEffect(() => {
    const handleInstallPrompt = (e: Event) => {
      e.preventDefault();
      const installEvent = e as any;
      setInstallPrompt({
        platforms: installEvent.platforms || ['web'],
        userChoice: null,
        prompt: async () => {
          const result = await installEvent.prompt();
          setInstallPrompt(prev => prev ? { ...prev, userChoice: result.outcome } : null);
        }
      });
    };

    window.addEventListener('beforeinstallprompt', handleInstallPrompt);
    
    // Check if already installed
    if (window.matchMedia('(display-mode: standalone)').matches) {
      setInstallPrompt(null);
    }

    return () => {
      window.removeEventListener('beforeinstallprompt', handleInstallPrompt);
    };
  }, []);

  // Service Worker registration and updates
  useEffect(() => {
    const registerServiceWorker = async () => {
      if ('serviceWorker' in navigator) {
        try {
          const registration = await navigator.serviceWorker.register('/sw.js');
          
          registration.addEventListener('updatefound', () => {
            const newWorker = registration.installing;
            if (newWorker) {
              newWorker.addEventListener('statechange', () => {
                if (newWorker.state === 'installed' && navigator.serviceWorker.controller) {
                  setServiceWorker(prev => ({
                    ...prev,
                    waiting: newWorker,
                    updateAvailable: true
                  }));
                }
              });
            }
          });

          // Handle messages from service worker
          navigator.serviceWorker.addEventListener('message', (event) => {
            if (event.data.type === 'CACHE_UPDATED') {
              setOfflineState(prev => ({
                ...prev,
                cachedData: event.data.data,
                lastSync: new Date()
              }));
            }
          });

        } catch (error) {
          console.error('Service worker registration failed:', error);
        }
      }
    };

    registerServiceWorker();
  }, []);

  // Network status monitoring
  useEffect(() => {
    const updateOnlineStatus = () => {
      setOfflineState(prev => ({
        ...prev,
        isOnline: navigator.onLine
      }));

      if (navigator.onLine && offlineState.pendingActions.length > 0) {
        syncOfflineActions();
      }
    };

    window.addEventListener('online', updateOnlineStatus);
    window.addEventListener('offline', updateOnlineStatus);

    return () => {
      window.removeEventListener('online', updateOnlineStatus);
      window.removeEventListener('offline', updateOnlineStatus);
    };
  }, [offlineState.pendingActions]);

  // Battery API
  useEffect(() => {
    const getBatteryInfo = async () => {
      if ('getBattery' in navigator) {
        try {
          const battery = await (navigator as any).getBattery();
          setBatteryLevel(battery.level * 100);
          
          battery.addEventListener('levelchange', () => {
            setBatteryLevel(battery.level * 100);
          });
        } catch (error) {
          console.log('Battery API not supported');
        }
      }
    };

    getBatteryInfo();
  }, []);

  // Network Information API
  useEffect(() => {
    if ('connection' in navigator) {
      const connection = (navigator as any).connection;
      setNetworkInfo({
        effectiveType: connection.effectiveType,
        downlink: connection.downlink,
        saveData: connection.saveData
      });

      const updateNetworkInfo = () => {
        setNetworkInfo({
          effectiveType: connection.effectiveType,
          downlink: connection.downlink,
          saveData: connection.saveData
        });
      };

      connection.addEventListener('change', updateNetworkInfo);
      return () => {
        connection.removeEventListener('change', updateNetworkInfo);
      };
    }
  }, []);

  // Push notifications setup
  useEffect(() => {
    const setupPushNotifications = async () => {
      if ('serviceWorker' in navigator && 'PushManager' in window) {
        try {
          const registration = await navigator.serviceWorker.ready;
          const permission = await Notification.requestPermission();
          
          if (permission === 'granted') {
            const subscription = await registration.pushManager.subscribe({
              userVisibleOnly: true,
              applicationServerKey: 'your-vapid-key' // Replace with actual VAPID key
            });
            
            // Send subscription to server
            console.log('Push subscription:', subscription);
          }
        } catch (error) {
          console.error('Push notification setup failed:', error);
        }
      }
    };

    setupPushNotifications();
  }, []);

  // Touch gestures handling
  const handleTouchStart = useCallback((e: React.TouchEvent) => {
    const touch = e.touches[0];
    setTouchGesture({
      startX: touch.clientX,
      startY: touch.clientY,
      deltaX: 0,
      deltaY: 0,
      direction: null,
      velocity: 0
    });
  }, []);

  const handleTouchMove = useCallback((e: React.TouchEvent) => {
    if (!touchGesture) return;
    
    const touch = e.touches[0];
    const deltaX = touch.clientX - touchGesture.startX;
    const deltaY = touch.clientY - touchGesture.startY;
    
    let direction: 'left' | 'right' | 'up' | 'down' | null = null;
    if (Math.abs(deltaX) > Math.abs(deltaY)) {
      direction = deltaX > 0 ? 'right' : 'left';
    } else {
      direction = deltaY > 0 ? 'down' : 'up';
    }

    setTouchGesture(prev => prev ? {
      ...prev,
      deltaX,
      deltaY,
      direction,
      velocity: Math.sqrt(deltaX * deltaX + deltaY * deltaY)
    } : null);
  }, [touchGesture]);

  const handleTouchEnd = useCallback(() => {
    if (touchGesture && touchGesture.velocity > 50) {
      // Handle gesture actions
      switch (touchGesture.direction) {
        case 'right':
          if (Math.abs(touchGesture.deltaX) > 100) {
            setDrawerOpen(true);
          }
          break;
        case 'left':
          if (Math.abs(touchGesture.deltaX) > 100) {
            setDrawerOpen(false);
          }
          break;
        case 'up':
          if (Math.abs(touchGesture.deltaY) > 100) {
            setQuickTradeOpen(true);
          }
          break;
        case 'down':
          if (Math.abs(touchGesture.deltaY) > 100) {
            setQuickTradeOpen(false);
          }
          break;
      }
    }
    setTouchGesture(null);
  }, [touchGesture]);

  // Offline actions sync
  const syncOfflineActions = useCallback(async () => {
    if (offlineState.pendingActions.length === 0) return;
    
    setOfflineState(prev => ({ ...prev, syncStatus: 'syncing' }));
    
    try {
      // Process pending actions
      for (const action of offlineState.pendingActions) {
        await processOfflineAction(action);
      }
      
      setOfflineState(prev => ({
        ...prev,
        pendingActions: [],
        syncStatus: 'success',
        lastSync: new Date()
      }));
      
      setTimeout(() => {
        setOfflineState(prev => ({ ...prev, syncStatus: 'idle' }));
      }, 2000);
      
    } catch (error) {
      setOfflineState(prev => ({ ...prev, syncStatus: 'error' }));
      console.error('Sync failed:', error);
    }
  }, [offlineState.pendingActions]);

  const processOfflineAction = async (action: OfflineAction) => {
    // Mock processing of offline actions
    await new Promise(resolve => setTimeout(resolve, 500));
    console.log('Processing offline action:', action);
  };

  const addOfflineAction = (action: Omit<OfflineAction, 'id' | 'timestamp' | 'retryCount'>) => {
    const newAction: OfflineAction = {
      ...action,
      id: Date.now().toString(),
      timestamp: new Date(),
      retryCount: 0
    };
    
    setOfflineState(prev => ({
      ...prev,
      pendingActions: [...prev.pendingActions, newAction]
    }));
  };

  // Quick actions for speed dial
  const quickActions: MobileQuickAction[] = [
    {
      id: 'buy',
      label: 'Quick Buy',
      icon: <TrendingUpIcon />,
      color: '#4CAF50',
      action: () => {
        setQuickTradeOpen(true);
        setSpeedDialOpen(false);
      }
    },
    {
      id: 'sell',
      label: 'Quick Sell',
      icon: <TrendingDownIcon />,
      color: '#F44336',
      action: () => {
        setQuickTradeOpen(true);
        setSpeedDialOpen(false);
      }
    },
    {
      id: 'watchlist',
      label: 'Add to Watchlist',
      icon: <BookmarkIcon />,
      color: '#2196F3',
      action: () => {
        if (!offlineState.isOnline) {
          addOfflineAction({
            type: 'watchlist',
            data: { symbol: 'RELIANCE', action: 'add' }
          });
        }
        setSpeedDialOpen(false);
      }
    },
    {
      id: 'alert',
      label: 'Price Alert',
      icon: <NotificationsIcon />,
      color: '#FF9800',
      action: () => {
        setSpeedDialOpen(false);
      }
    },
    {
      id: 'share',
      label: 'Share',
      icon: <ShareIcon />,
      color: '#9C27B0',
      action: () => {
        if (navigator.share) {
          navigator.share({
            title: 'TradeMaster',
            text: 'Check out my portfolio performance!',
            url: window.location.href
          });
        }
        setSpeedDialOpen(false);
      }
    }
  ];

  const handleInstallApp = async () => {
    if (installPrompt) {
      await installPrompt.prompt();
    }
  };

  const handleUpdateApp = async () => {
    if (serviceWorker.waiting) {
      setServiceWorker(prev => ({ ...prev, updating: true }));
      serviceWorker.waiting.postMessage({ type: 'SKIP_WAITING' });
      
      setTimeout(() => {
        window.location.reload();
      }, 1000);
    }
  };

  const toggleFullscreen = () => {
    if (!document.fullscreenElement) {
      document.documentElement.requestFullscreen();
      setIsFullscreen(true);
    } else {
      document.exitFullscreen();
      setIsFullscreen(false);
    }
  };

  // Mock portfolio data for mobile display
  const portfolioSummary = {
    totalValue: 487500,
    dayChange: 12450,
    dayChangePercent: 2.62,
    totalPnL: 65430,
    totalPnLPercent: 15.5
  };

  const topStocks = [
    { symbol: 'RELIANCE', change: 2.45, value: 125000 },
    { symbol: 'TCS', change: -1.23, value: 98500 },
    { symbol: 'HDFCBANK', change: 3.67, value: 87500 },
    { symbol: 'INFY', change: 1.89, value: 76500 }
  ];

  return (
    <Box 
      sx={{ 
        minHeight: '100vh', 
        bgcolor: 'background.default',
        pb: 7 // Space for bottom navigation
      }}
      onTouchStart={handleTouchStart}
      onTouchMove={handleTouchMove}
      onTouchEnd={handleTouchEnd}
    >
      {/* Status Bar */}
      <AppBar position="sticky" sx={{ bgcolor: 'primary.main' }}>
        <Toolbar sx={{ minHeight: '48px !important', px: 2 }}>
          <IconButton
            edge="start"
            color="inherit"
            aria-label="menu"
            onClick={() => setDrawerOpen(true)}
            sx={{ mr: 1 }}
          >
            <MenuIcon />
          </IconButton>
          
          <Typography variant="h6" sx={{ flexGrow: 1, fontSize: '1rem' }}>
            TradeMaster
          </Typography>

          {/* Network Status */}
          <Box display="flex" alignItems="center" gap={1}>
            {!offlineState.isOnline && (
              <WifiOffIcon fontSize="small" color="error" />
            )}
            
            {offlineState.syncStatus === 'syncing' && (
              <SyncIcon fontSize="small" className="spinning" />
            )}
            
            {batteryLevel !== null && batteryLevel < 20 && (
              <Typography variant="caption" color="error">
                {Math.round(batteryLevel)}%
              </Typography>
            )}

            <IconButton color="inherit" size="small">
              <Badge badgeContent={notifications.length} color="error">
                <NotificationsIcon fontSize="small" />
              </Badge>
            </IconButton>
          </Box>
        </Toolbar>
      </AppBar>

      {/* PWA Install Banner */}
      <AnimatePresence>
        {installPrompt && (
          <motion.div
            initial={{ y: -100, opacity: 0 }}
            animate={{ y: 0, opacity: 1 }}
            exit={{ y: -100, opacity: 0 }}
          >
            <Alert
              severity="info"
              action={
                <Button color="inherit" size="small" onClick={handleInstallApp}>
                  Install
                </Button>
              }
              sx={{ m: 1, borderRadius: 2 }}
            >
              Install TradeMaster app for better experience
            </Alert>
          </motion.div>
        )}
      </AnimatePresence>

      {/* App Update Banner */}
      <AnimatePresence>
        {serviceWorker.updateAvailable && (
          <motion.div
            initial={{ y: -100, opacity: 0 }}
            animate={{ y: 0, opacity: 1 }}
            exit={{ y: -100, opacity: 0 }}
          >
            <Alert
              severity="success"
              action={
                <Button 
                  color="inherit" 
                  size="small" 
                  onClick={handleUpdateApp}
                  disabled={serviceWorker.updating}
                >
                  {serviceWorker.updating ? <CircularProgress size={16} color="inherit" /> : 'Update'}
                </Button>
              }
              sx={{ m: 1, borderRadius: 2 }}
            >
              New version available!
            </Alert>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Offline Status Banner */}
      <Collapse in={!offlineState.isOnline}>
        <Alert
          severity="warning"
          icon={<CloudOffIcon />}
          sx={{ m: 1, borderRadius: 2 }}
        >
          You're offline. {offlineState.pendingActions.length} actions pending sync.
        </Alert>
      </Collapse>

      {/* Main Content */}
      <Box sx={{ p: 2 }}>
        {/* Portfolio Summary Card */}
        <motion.div
          initial={{ scale: 0.95, opacity: 0 }}
          animate={{ scale: 1, opacity: 1 }}
          transition={{ duration: 0.3 }}
        >
          <Card sx={{ mb: 2, borderRadius: 3 }}>
            <CardContent>
              <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
                <Typography variant="h6" fontWeight="bold">
                  Portfolio
                </Typography>
                <IconButton size="small" onClick={() => window.location.reload()}>
                  <RefreshIcon />
                </IconButton>
              </Box>
              
              <Grid container spacing={2}>
                <Grid item xs={6}>
                  <Typography variant="body2" color="text.secondary">
                    Total Value
                  </Typography>
                  <Typography variant="h5" fontWeight="bold">
                    ₹{portfolioSummary.totalValue.toLocaleString()}
                  </Typography>
                </Grid>
                <Grid item xs={6}>
                  <Typography variant="body2" color="text.secondary">
                    Today's P&L
                  </Typography>
                  <Typography 
                    variant="h5" 
                    fontWeight="bold"
                    color={portfolioSummary.dayChange >= 0 ? 'success.main' : 'error.main'}
                  >
                    {portfolioSummary.dayChange >= 0 ? '+' : ''}₹{portfolioSummary.dayChange.toLocaleString()}
                  </Typography>
                  <Typography 
                    variant="caption"
                    color={portfolioSummary.dayChangePercent >= 0 ? 'success.main' : 'error.main'}
                  >
                    ({portfolioSummary.dayChangePercent >= 0 ? '+' : ''}{portfolioSummary.dayChangePercent}%)
                  </Typography>
                </Grid>
              </Grid>
            </CardContent>
          </Card>
        </motion.div>

        {/* Top Stocks */}
        <motion.div
          initial={{ y: 20, opacity: 0 }}
          animate={{ y: 0, opacity: 1 }}
          transition={{ duration: 0.3, delay: 0.1 }}
        >
          <Card sx={{ mb: 2, borderRadius: 3 }}>
            <CardContent>
              <Typography variant="h6" fontWeight="bold" mb={2}>
                Top Holdings
              </Typography>
              
              {topStocks.map((stock, index) => (
                <Box
                  key={stock.symbol}
                  display="flex"
                  justifyContent="space-between"
                  alignItems="center"
                  py={1}
                  sx={{ 
                    borderBottom: index < topStocks.length - 1 ? '1px solid #f0f0f0' : 'none'
                  }}
                >
                  <Box display="flex" alignItems="center" gap={2}>
                    <Avatar sx={{ width: 32, height: 32, bgcolor: 'primary.light' }}>
                      {stock.symbol.charAt(0)}
                    </Avatar>
                    <Box>
                      <Typography variant="body2" fontWeight="bold">
                        {stock.symbol}
                      </Typography>
                      <Typography 
                        variant="caption" 
                        color={stock.change >= 0 ? 'success.main' : 'error.main'}
                      >
                        {stock.change >= 0 ? '+' : ''}{stock.change}%
                      </Typography>
                    </Box>
                  </Box>
                  <Typography variant="body2" fontWeight="bold">
                    ₹{stock.value.toLocaleString()}
                  </Typography>
                </Box>
              ))}
            </CardContent>
          </Card>
        </motion.div>

        {/* Quick Actions Grid */}
        <motion.div
          initial={{ y: 20, opacity: 0 }}
          animate={{ y: 0, opacity: 1 }}
          transition={{ duration: 0.3, delay: 0.2 }}
        >
          <Card sx={{ borderRadius: 3 }}>
            <CardContent>
              <Typography variant="h6" fontWeight="bold" mb={2}>
                Quick Actions
              </Typography>
              
              <Grid container spacing={2}>
                {quickActions.slice(0, 4).map((action) => (
                  <Grid item xs={6} sm={3} key={action.id}>
                    <Paper
                      sx={{
                        p: 2,
                        textAlign: 'center',
                        cursor: 'pointer',
                        '&:hover': { bgcolor: 'action.hover' },
                        '&:active': { transform: 'scale(0.98)' },
                        borderRadius: 2
                      }}
                      onClick={action.action}
                    >
                      <Avatar 
                        sx={{ 
                          bgcolor: action.color,
                          mx: 'auto',
                          mb: 1,
                          width: 48,
                          height: 48
                        }}
                      >
                        {action.icon}
                      </Avatar>
                      <Typography variant="caption" fontWeight="bold">
                        {action.label}
                      </Typography>
                    </Paper>
                  </Grid>
                ))}
              </Grid>
            </CardContent>
          </Card>
        </motion.div>
      </Box>

      {/* Side Drawer */}
      <SwipeableDrawer
        anchor="left"
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
        onOpen={() => setDrawerOpen(true)}
        swipeAreaWidth={20}
      >
        <Box sx={{ width: 280, pt: 2 }}>
          <Box sx={{ px: 2, pb: 2 }}>
            <Typography variant="h6" fontWeight="bold">
              Menu
            </Typography>
          </Box>
          
          <List>
            {[
              { label: 'Portfolio', icon: <PieChartIcon />, path: '/portfolio' },
              { label: 'Trading', icon: <TrendingUpIcon />, path: '/trading' },
              { label: 'Watchlist', icon: <BookmarkIcon />, path: '/watchlist' },
              { label: 'Orders', icon: <AssessmentIcon />, path: '/orders' },
              { label: 'Analytics', icon: <BarChartIcon />, path: '/analytics' },
              { label: 'Agents', icon: <BusinessCenterIcon />, path: '/agents' },
              { label: 'Settings', icon: <SettingsIcon />, path: '/settings' }
            ].map((item) => (
              <ListItem 
                key={item.label}
                onClick={() => {
                  setDrawerOpen(false);
                  // Navigate to path
                }}
                sx={{ cursor: 'pointer' }}
              >
                <ListItemIcon>{item.icon}</ListItemIcon>
                <ListItemText primary={item.label} />
              </ListItem>
            ))}
          </List>
          
          <Box sx={{ px: 2, py: 2, borderTop: '1px solid #f0f0f0', mt: 2 }}>
            <Typography variant="body2" color="text.secondary" gutterBottom>
              App Status
            </Typography>
            <Box display="flex" alignItems="center" gap={1} mb={1}>
              <Box
                sx={{
                  width: 8,
                  height: 8,
                  borderRadius: '50%',
                  bgcolor: offlineState.isOnline ? 'success.main' : 'error.main'
                }}
              />
              <Typography variant="caption">
                {offlineState.isOnline ? 'Online' : 'Offline'}
              </Typography>
            </Box>
            
            {offlineState.pendingActions.length > 0 && (
              <Typography variant="caption" color="warning.main">
                {offlineState.pendingActions.length} pending actions
              </Typography>
            )}
            
            <Typography variant="caption" color="text.secondary" display="block">
              Last sync: {offlineState.lastSync.toLocaleTimeString()}
            </Typography>
            
            {networkInfo && (
              <Typography variant="caption" color="text.secondary" display="block">
                Network: {networkInfo.effectiveType}
                {networkInfo.saveData && ' (Data Saver)'}
              </Typography>
            )}
          </Box>
        </Box>
      </SwipeableDrawer>

      {/* Speed Dial for Quick Actions */}
      <SpeedDial
        ariaLabel="Quick Actions"
        sx={{ 
          position: 'fixed', 
          bottom: 80, 
          right: 16,
          '& .MuiSpeedDial-fab': {
            bgcolor: 'primary.main',
            '&:hover': { bgcolor: 'primary.dark' }
          }
        }}
        icon={<SpeedDialIcon />}
        open={speedDialOpen}
        onClose={() => setSpeedDialOpen(false)}
        onOpen={() => setSpeedDialOpen(true)}
      >
        {quickActions.map((action) => (
          <SpeedDialAction
            key={action.id}
            icon={action.icon}
            tooltipTitle={action.label}
            onClick={action.action}
            FabProps={{
              sx: { bgcolor: action.color, '&:hover': { bgcolor: action.color, filter: 'brightness(0.9)' } }
            }}
          />
        ))}
      </SpeedDial>

      {/* Bottom Navigation */}
      <Paper 
        sx={{ 
          position: 'fixed', 
          bottom: 0, 
          left: 0, 
          right: 0, 
          zIndex: 1000,
          borderTop: '1px solid #e0e0e0'
        }} 
        elevation={3}
      >
        <BottomNavigation
          value={bottomNavValue}
          onChange={(event, newValue) => setBottomNavValue(newValue)}
          showLabels
        >
          <BottomNavigationAction
            label="Home"
            icon={<HomeIcon />}
          />
          <BottomNavigationAction
            label="Trading"
            icon={<TrendingUpIcon />}
          />
          <BottomNavigationAction
            label="Portfolio"
            icon={<AccountBalanceWalletIcon />}
          />
          <BottomNavigationAction
            label="Charts"
            icon={<ShowChartIcon />}
          />
          <BottomNavigationAction
            label="More"
            icon={<MenuIcon />}
          />
        </BottomNavigation>
      </Paper>

      {/* Quick Trade Dialog */}
      <Dialog
        open={quickTradeOpen}
        onClose={() => setQuickTradeOpen(false)}
        TransitionComponent={Transition}
        fullScreen
        sx={{ '& .MuiDialog-paper': { bgcolor: 'background.default' } }}
      >
        <AppBar position="relative">
          <Toolbar>
            <Typography sx={{ ml: 2, flex: 1 }} variant="h6" component="div">
              Quick Trade
            </Typography>
            <Button color="inherit" onClick={() => setQuickTradeOpen(false)}>
              Close
            </Button>
          </Toolbar>
        </AppBar>
        
        <Box sx={{ p: 3 }}>
          <Typography variant="h6" gutterBottom>
            Place Quick Order
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Quick trade functionality would be implemented here with simplified order placement.
          </Typography>
          
          {!offlineState.isOnline && (
            <Alert severity="warning" sx={{ mt: 2 }}>
              <Typography variant="body2">
                You're offline. This order will be queued and executed when connection is restored.
              </Typography>
            </Alert>
          )}
        </Box>
      </Dialog>

      {/* Offline Sync Status */}
      <Snackbar
        open={offlineState.syncStatus === 'success'}
        autoHideDuration={3000}
        onClose={() => setOfflineState(prev => ({ ...prev, syncStatus: 'idle' }))}
      >
        <Alert severity="success" variant="filled">
          Sync completed successfully!
        </Alert>
      </Snackbar>

      <Snackbar
        open={offlineState.syncStatus === 'error'}
        autoHideDuration={5000}
        onClose={() => setOfflineState(prev => ({ ...prev, syncStatus: 'idle' }))}
      >
        <Alert severity="error" variant="filled">
          Sync failed. Will retry when connection is available.
        </Alert>
      </Snackbar>

      {/* CSS for animations */}
      <style jsx global>{`
        @keyframes spin {
          from { transform: rotate(0deg); }
          to { transform: rotate(360deg); }
        }
        
        .spinning {
          animation: spin 1s linear infinite;
        }
        
        /* PWA specific styles */
        @media (display-mode: standalone) {
          body {
            -webkit-user-select: none;
            -webkit-touch-callout: none;
            -webkit-tap-highlight-color: transparent;
          }
        }
        
        /* Pull-to-refresh indicator */
        .ptr-element {
          pointer-events: none;
          font-size: 0.85rem;
          font-weight: bold;
          top: 0;
          height: 60px;
          text-align: center;
          width: 100%;
          position: absolute;
          z-index: 1001;
          background-color: #fff;
          color: #777;
        }
        
        .ptr-element .loading-icon {
          margin-top: 15px;
          font-size: 1.2rem;
        }
      `}</style>
    </Box>
  );
};

export default MobilePWA;