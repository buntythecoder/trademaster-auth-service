// Core TradeMaster TypeScript Types
// Consistent with backend Java DTOs and entities

export interface User {
  id: string;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  role: UserRole;
  roles?: string[]; // For role-based access control
  isActive: boolean;
  lastLoginAt?: string;
  createdAt: string;
  updatedAt: string;
}

export enum UserRole {
  ADMIN = 'ADMIN',
  PORTFOLIO_MANAGER = 'PORTFOLIO_MANAGER',
  TRADER = 'TRADER',
  ANALYST = 'ANALYST',
  USER = 'USER'
}

export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  tokenType: string;
}

export interface LoginRequest {
  username: string;
  password: string;
  rememberMe?: boolean;
}

export interface Portfolio {
  id: string;
  name: string;
  description: string;
  status: PortfolioStatus;
  totalValue: number;
  cashBalance: number;
  unrealizedPnL: number;
  realizedPnL: number;
  dayChange: number;
  dayChangePercent: number;
  positions: Position[];
  createdAt: string;
  updatedAt: string;
}

export enum PortfolioStatus {
  ACTIVE = 'ACTIVE',
  INACTIVE = 'INACTIVE',
  CLOSED = 'CLOSED',
  SUSPENDED = 'SUSPENDED'
}

export interface Position {
  id: string;
  portfolioId: string;
  symbol: string;
  quantity: number;
  averagePrice: number;
  currentPrice: number;
  marketValue: number;
  unrealizedPnL: number;
  unrealizedPnLPercent: number;
  dayChange: number;
  dayChangePercent: number;
  positionType: PositionType;
  createdAt: string;
  updatedAt: string;
}

export enum PositionType {
  LONG = 'LONG',
  SHORT = 'SHORT'
}

export interface MarketData {
  symbol: string;
  name?: string;
  currentPrice: number;
  price?: number; // alias for currentPrice for compatibility
  previousClose?: number;
  change: number;
  changePercent: number;
  volume: number;
  dayHigh: number;
  dayLow: number;
  high?: number; // alias for dayHigh
  low?: number; // alias for dayLow
  open?: number;
  bid?: number;
  ask?: number;
  marketCap?: number;
  timestamp: string;
}

export interface Order {
  id: string;
  portfolioId: string;
  symbol: string;
  side: OrderSide;
  type: OrderType;
  quantity: number;
  price?: number;
  stopPrice?: number;
  status: OrderStatus;
  filledQuantity: number;
  averageFillPrice?: number;
  timeInForce: TimeInForce;
  createdAt: string;
  updatedAt: string;
}

export enum OrderSide {
  BUY = 'BUY',
  SELL = 'SELL'
}

export enum OrderType {
  MARKET = 'MARKET',
  LIMIT = 'LIMIT',
  STOP = 'STOP',
  STOP_LIMIT = 'STOP_LIMIT'
}

export enum OrderStatus {
  PENDING = 'PENDING',
  SUBMITTED = 'SUBMITTED',
  PARTIALLY_FILLED = 'PARTIALLY_FILLED',
  FILLED = 'FILLED',
  CANCELLED = 'CANCELLED',
  REJECTED = 'REJECTED'
}

export enum TimeInForce {
  DAY = 'DAY',
  GTC = 'GTC', // Good Till Cancelled
  IOC = 'IOC', // Immediate or Cancel
  FOK = 'FOK'  // Fill or Kill
}

export interface Trade {
  id: string;
  orderId: string;
  symbol: string;
  side: OrderSide;
  quantity: number;
  price: number;
  value: number;
  commission: number;
  timestamp: string;
}

export interface RiskAlert {
  id: string;
  portfolioId: string;
  type: RiskAlertType;
  severity: AlertSeverity;
  message: string;
  threshold: number;
  currentValue: number;
  isActive: boolean;
  createdAt: string;
}

export enum RiskAlertType {
  POSITION_LIMIT = 'POSITION_LIMIT',
  LOSS_LIMIT = 'LOSS_LIMIT',
  MARGIN_CALL = 'MARGIN_CALL',
  VAR_BREACH = 'VAR_BREACH',
  CONCENTRATION_RISK = 'CONCENTRATION_RISK'
}

export enum AlertSeverity {
  LOW = 'LOW',
  MEDIUM = 'MEDIUM',
  HIGH = 'HIGH',
  CRITICAL = 'CRITICAL'
}

export interface PortfolioSummary {
  totalValue: number;
  cashBalance: number;
  investedAmount: number;
  unrealizedPnL: number;
  realizedPnL: number;
  totalPnL: number;
  dayChange: number;
  dayChangePercent: number;
  positionsCount: number;
  topPerformers: Position[];
  worstPerformers: Position[];
}

export interface PerformanceMetrics {
  period: string;
  returns: number;
  benchmark: string;
  benchmarkReturns: number;
  alpha: number;
  beta: number;
  sharpeRatio: number;
  maxDrawdown: number;
  volatility: number;
  winRate: number;
  averageWin: number;
  averageLoss: number;
}

export interface ChartDataPoint {
  timestamp: string;
  value: number;
  volume?: number;
}

export interface OrderBookEntry {
  price: number;
  quantity: number;
  total: number;
}

export interface OrderBook {
  symbol: string;
  bids: OrderBookEntry[];
  asks: OrderBookEntry[];
  timestamp: string;
}

// API Response Types
export interface ApiResponse<T = any> {
  success: boolean;
  data?: T;
  message?: string;
  errors?: Record<string, string[]>;
  timestamp: string;
}

export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

// Form Types
export interface CreatePortfolioRequest {
  name: string;
  description: string;
  initialCash: number;
  riskProfile: RiskProfile;
}

export enum RiskProfile {
  CONSERVATIVE = 'CONSERVATIVE',
  MODERATE = 'MODERATE',
  AGGRESSIVE = 'AGGRESSIVE'
}

export interface PlaceOrderRequest {
  portfolioId: string;
  symbol: string;
  side: OrderSide;
  type: OrderType;
  quantity: number;
  price?: number;
  stopPrice?: number;
  timeInForce: TimeInForce;
}

// WebSocket Types
export interface WebSocketMessage<T = any> {
  type: string;
  data: T;
  timestamp: string;
}

export interface PriceUpdate {
  symbol: string;
  price: number;
  change: number;
  changePercent: number;
  volume: number;
  timestamp: string;
}

export interface OrderUpdate {
  orderId: string;
  status: OrderStatus;
  filledQuantity: number;
  averageFillPrice?: number;
  timestamp: string;
}

// UI State Types
export interface LoadingState {
  isLoading: boolean;
  error?: string;
}

export interface TableColumn<T = any> {
  key: keyof T;
  label: string;
  sortable?: boolean;
  width?: string;
  align?: 'left' | 'center' | 'right';
  render?: (value: any, row: T) => React.ReactNode;
}

export interface FilterOption {
  label: string;
  value: string;
}

export interface DateRange {
  start: Date;
  end: Date;
}

// Theme Types
export interface ThemeConfig {
  isDark: boolean;
  primaryColor: string;
  fontSize: 'small' | 'medium' | 'large';
}

// Navigation Types
export interface NavItem {
  label: string;
  href: string;
  icon?: React.ComponentType<{ className?: string }>;
  badge?: string | number;
  children?: NavItem[];
}

// Error Types
export interface AppError {
  code: string;
  message: string;
  details?: Record<string, any>;
  timestamp: string;
}