import { createSlice, PayloadAction } from '@reduxjs/toolkit';

// Theme types
export type ThemeMode = 'light' | 'dark' | 'system';

// UI notification types
export interface Notification {
  id: string;
  type: 'success' | 'error' | 'warning' | 'info';
  title: string;
  message: string;
  timestamp: string;
  isRead: boolean;
  autoClose?: number; // Auto close after X milliseconds
}

// Modal types
export interface Modal {
  id: string;
  type: string;
  isOpen: boolean;
  data?: any;
}

// Sidebar types
export interface SidebarState {
  isCollapsed: boolean;
  activeSection: string | null;
}

// Layout types
export interface LayoutState {
  sidebar: SidebarState;
  headerHeight: number;
  footerHeight: number;
}

// UI state interface
interface UIState {
  // Theme and appearance
  theme: ThemeMode;
  
  // Notifications
  notifications: Notification[];
  unreadNotificationCount: number;
  
  // Modals
  modals: Modal[];
  
  // Loading states for different UI components
  loadingStates: Record<string, boolean>;
  
  // Layout state
  layout: LayoutState;
  
  // Navigation
  activeRoute: string;
  breadcrumbs: Array<{ label: string; path: string }>;
  
  // Trading interface specific state
  tradingInterface: {
    selectedSymbol: string | null;
    watchlistExpanded: boolean;
    orderFormExpanded: boolean;
    chartInterval: string;
    chartType: 'line' | 'candle' | 'area';
  };
  
  // Portfolio interface state
  portfolioInterface: {
    selectedView: 'overview' | 'positions' | 'history' | 'analytics';
    sortBy: string;
    sortDirection: 'asc' | 'desc';
    filterBy: {
      status?: string;
      symbol?: string;
      positionType?: string;
    };
  };
  
  // Market data interface state
  marketInterface: {
    selectedMarket: 'stocks' | 'crypto' | 'forex' | 'commodities';
    displayMode: 'grid' | 'list';
    sortBy: 'symbol' | 'price' | 'change' | 'volume';
    sortDirection: 'asc' | 'desc';
  };
  
  // Responsive breakpoint state
  breakpoint: 'xs' | 'sm' | 'md' | 'lg' | 'xl' | '2xl';
  isMobile: boolean;
}

// Initial state
const initialState: UIState = {
  theme: 'system',
  notifications: [],
  unreadNotificationCount: 0,
  modals: [],
  loadingStates: {},
  layout: {
    sidebar: {
      isCollapsed: false,
      activeSection: null,
    },
    headerHeight: 64,
    footerHeight: 48,
  },
  activeRoute: '/',
  breadcrumbs: [],
  tradingInterface: {
    selectedSymbol: null,
    watchlistExpanded: true,
    orderFormExpanded: false,
    chartInterval: '1D',
    chartType: 'line',
  },
  portfolioInterface: {
    selectedView: 'overview',
    sortBy: 'symbol',
    sortDirection: 'asc',
    filterBy: {},
  },
  marketInterface: {
    selectedMarket: 'stocks',
    displayMode: 'grid',
    sortBy: 'symbol',
    sortDirection: 'asc',
  },
  breakpoint: 'lg',
  isMobile: false,
};

// UI slice
const uiSlice = createSlice({
  name: 'ui',
  initialState,
  reducers: {
    // Theme management
    setTheme: (state, action: PayloadAction<ThemeMode>) => {
      state.theme = action.payload;
    },
    
    // Notification management
    addNotification: (state, action: PayloadAction<Omit<Notification, 'id' | 'timestamp' | 'isRead'>>) => {
      const notification: Notification = {
        ...action.payload,
        id: `notification-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
        timestamp: new Date().toISOString(),
        isRead: false,
      };
      
      state.notifications.unshift(notification);
      state.unreadNotificationCount += 1;
      
      // Limit notifications to 50 to prevent memory issues
      if (state.notifications.length > 50) {
        const removed = state.notifications.pop();
        if (removed && !removed.isRead) {
          state.unreadNotificationCount -= 1;
        }
      }
    },
    
    markNotificationAsRead: (state, action: PayloadAction<string>) => {
      const notification = state.notifications.find(n => n.id === action.payload);
      if (notification && !notification.isRead) {
        notification.isRead = true;
        state.unreadNotificationCount = Math.max(0, state.unreadNotificationCount - 1);
      }
    },
    
    markAllNotificationsAsRead: (state) => {
      state.notifications.forEach(notification => {
        notification.isRead = true;
      });
      state.unreadNotificationCount = 0;
    },
    
    removeNotification: (state, action: PayloadAction<string>) => {
      const index = state.notifications.findIndex(n => n.id === action.payload);
      if (index !== -1) {
        const notification = state.notifications[index];
        if (!notification.isRead) {
          state.unreadNotificationCount = Math.max(0, state.unreadNotificationCount - 1);
        }
        state.notifications.splice(index, 1);
      }
    },
    
    clearAllNotifications: (state) => {
      state.notifications = [];
      state.unreadNotificationCount = 0;
    },
    
    // Modal management
    openModal: (state, action: PayloadAction<{ type: string; data?: any }>) => {
      const modal: Modal = {
        id: `modal-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
        type: action.payload.type,
        isOpen: true,
        data: action.payload.data,
      };
      state.modals.push(modal);
    },
    
    closeModal: (state, action: PayloadAction<string>) => {
      const index = state.modals.findIndex(modal => modal.id === action.payload);
      if (index !== -1) {
        state.modals.splice(index, 1);
      }
    },
    
    closeAllModals: (state) => {
      state.modals = [];
    },
    
    // Loading states
    setLoadingState: (state, action: PayloadAction<{ key: string; isLoading: boolean }>) => {
      if (action.payload.isLoading) {
        state.loadingStates[action.payload.key] = true;
      } else {
        delete state.loadingStates[action.payload.key];
      }
    },
    
    clearAllLoadingStates: (state) => {
      state.loadingStates = {};
    },
    
    // Layout management
    toggleSidebar: (state) => {
      state.layout.sidebar.isCollapsed = !state.layout.sidebar.isCollapsed;
    },
    
    setSidebarCollapsed: (state, action: PayloadAction<boolean>) => {
      state.layout.sidebar.isCollapsed = action.payload;
    },
    
    setActiveSidebarSection: (state, action: PayloadAction<string | null>) => {
      state.layout.sidebar.activeSection = action.payload;
    },
    
    // Navigation
    setActiveRoute: (state, action: PayloadAction<string>) => {
      state.activeRoute = action.payload;
    },
    
    setBreadcrumbs: (state, action: PayloadAction<Array<{ label: string; path: string }>>) => {
      state.breadcrumbs = action.payload;
    },
    
    // Trading interface state
    setSelectedSymbol: (state, action: PayloadAction<string | null>) => {
      state.tradingInterface.selectedSymbol = action.payload;
    },
    
    toggleWatchlist: (state) => {
      state.tradingInterface.watchlistExpanded = !state.tradingInterface.watchlistExpanded;
    },
    
    setWatchlistExpanded: (state, action: PayloadAction<boolean>) => {
      state.tradingInterface.watchlistExpanded = action.payload;
    },
    
    toggleOrderForm: (state) => {
      state.tradingInterface.orderFormExpanded = !state.tradingInterface.orderFormExpanded;
    },
    
    setOrderFormExpanded: (state, action: PayloadAction<boolean>) => {
      state.tradingInterface.orderFormExpanded = action.payload;
    },
    
    setChartInterval: (state, action: PayloadAction<string>) => {
      state.tradingInterface.chartInterval = action.payload;
    },
    
    setChartType: (state, action: PayloadAction<'line' | 'candle' | 'area'>) => {
      state.tradingInterface.chartType = action.payload;
    },
    
    // Portfolio interface state
    setPortfolioView: (state, action: PayloadAction<'overview' | 'positions' | 'history' | 'analytics'>) => {
      state.portfolioInterface.selectedView = action.payload;
    },
    
    setPortfolioSort: (state, action: PayloadAction<{ sortBy: string; direction: 'asc' | 'desc' }>) => {
      state.portfolioInterface.sortBy = action.payload.sortBy;
      state.portfolioInterface.sortDirection = action.payload.direction;
    },
    
    setPortfolioFilter: (state, action: PayloadAction<{ status?: string; symbol?: string; positionType?: string }>) => {
      state.portfolioInterface.filterBy = { ...state.portfolioInterface.filterBy, ...action.payload };
    },
    
    clearPortfolioFilter: (state) => {
      state.portfolioInterface.filterBy = {};
    },
    
    // Market interface state
    setMarketType: (state, action: PayloadAction<'stocks' | 'crypto' | 'forex' | 'commodities'>) => {
      state.marketInterface.selectedMarket = action.payload;
    },
    
    setMarketDisplayMode: (state, action: PayloadAction<'grid' | 'list'>) => {
      state.marketInterface.displayMode = action.payload;
    },
    
    setMarketSort: (state, action: PayloadAction<{ sortBy: 'symbol' | 'price' | 'change' | 'volume'; direction: 'asc' | 'desc' }>) => {
      state.marketInterface.sortBy = action.payload.sortBy;
      state.marketInterface.sortDirection = action.payload.direction;
    },
    
    // Responsive breakpoint management
    setBreakpoint: (state, action: PayloadAction<'xs' | 'sm' | 'md' | 'lg' | 'xl' | '2xl'>) => {
      state.breakpoint = action.payload;
      state.isMobile = ['xs', 'sm'].includes(action.payload);
      
      // Auto-collapse sidebar on mobile
      if (state.isMobile && !state.layout.sidebar.isCollapsed) {
        state.layout.sidebar.isCollapsed = true;
      }
    },
  },
});

// Export actions
export const {
  setTheme,
  addNotification,
  markNotificationAsRead,
  markAllNotificationsAsRead,
  removeNotification,
  clearAllNotifications,
  openModal,
  closeModal,
  closeAllModals,
  setLoadingState,
  clearAllLoadingStates,
  toggleSidebar,
  setSidebarCollapsed,
  setActiveSidebarSection,
  setActiveRoute,
  setBreadcrumbs,
  setSelectedSymbol,
  toggleWatchlist,
  setWatchlistExpanded,
  toggleOrderForm,
  setOrderFormExpanded,
  setChartInterval,
  setChartType,
  setPortfolioView,
  setPortfolioSort,
  setPortfolioFilter,
  clearPortfolioFilter,
  setMarketType,
  setMarketDisplayMode,
  setMarketSort,
  setBreakpoint,
} = uiSlice.actions;

// Export selectors
export const selectTheme = (state: { ui: UIState }) => state.ui.theme;
export const selectNotifications = (state: { ui: UIState }) => state.ui.notifications;
export const selectUnreadNotificationCount = (state: { ui: UIState }) => state.ui.unreadNotificationCount;
export const selectModals = (state: { ui: UIState }) => state.ui.modals;
export const selectLoadingStates = (state: { ui: UIState }) => state.ui.loadingStates;
export const selectLayout = (state: { ui: UIState }) => state.ui.layout;
export const selectSidebar = (state: { ui: UIState }) => state.ui.layout.sidebar;
export const selectActiveRoute = (state: { ui: UIState }) => state.ui.activeRoute;
export const selectBreadcrumbs = (state: { ui: UIState }) => state.ui.breadcrumbs;
export const selectTradingInterface = (state: { ui: UIState }) => state.ui.tradingInterface;
export const selectPortfolioInterface = (state: { ui: UIState }) => state.ui.portfolioInterface;
export const selectMarketInterface = (state: { ui: UIState }) => state.ui.marketInterface;
export const selectBreakpoint = (state: { ui: UIState }) => state.ui.breakpoint;
export const selectIsMobile = (state: { ui: UIState }) => state.ui.isMobile;

// Helper selectors
export const selectIsLoading = (key: string) => (state: { ui: UIState }) => 
  state.ui.loadingStates[key] || false;

export const selectActiveModal = (type: string) => (state: { ui: UIState }) => 
  state.ui.modals.find(modal => modal.type === type && modal.isOpen);

export const selectUnreadNotifications = (state: { ui: UIState }) => 
  state.ui.notifications.filter(notification => !notification.isRead);

// Export reducer
export default uiSlice.reducer;