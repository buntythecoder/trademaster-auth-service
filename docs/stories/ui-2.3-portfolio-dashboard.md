# UI Story 2.3: Portfolio Performance Dashboard

**Epic**: 2 - Market Data & Trading Foundation  
**Story**: Comprehensive Portfolio Performance & Analytics Interface  
**Priority**: High - User Experience Critical  
**Complexity**: Medium-High  
**Duration**: 2 weeks  

## 📋 Story Overview

**As a** retail trader using TradeMaster  
**I want** a comprehensive portfolio dashboard with real-time performance analytics  
**So that** I can track my investments, analyze performance, and make informed decisions

## 🎯 Business Value

- **User Retention**: Comprehensive portfolio tracking increases platform stickiness
- **Premium Features**: Advanced analytics drive subscription upgrades
- **Decision Support**: Better tools lead to more confident trading and higher volume
- **Competitive Edge**: Institutional-grade portfolio analytics for retail users

## 🖼️ UI Requirements

### Design System Consistency
- **Theme**: Continue established dark fintech aesthetic with glassmorphism
- **Performance Colors**: Green gains, red losses, amber warnings
- **Charts**: Interactive charts matching market data dashboard style
- **Mobile-First**: Touch-optimized for mobile portfolio monitoring
- **Animations**: Smooth transitions for value changes and chart interactions

### Portfolio-Specific Color System
```css
:root {
  /* Performance Colors */
  --portfolio-gain: #22C55E;      /* Positive returns */
  --portfolio-loss: #EF4444;      /* Negative returns */
  --portfolio-neutral: #94A3B8;   /* Break-even */
  
  /* Asset Allocation Colors */
  --asset-equity: #3B82F6;        /* Stocks */
  --asset-etf: #8B5CF6;           /* ETFs */
  --asset-mutual: #10B981;        /* Mutual Funds */
  --asset-cash: #F59E0B;          /* Cash/FD */
  --asset-crypto: #EC4899;        /* Cryptocurrency */
  
  /* Risk Categories */
  --risk-conservative: #22C55E;    /* Low risk */
  --risk-moderate: #F59E0B;        /* Medium risk */
  --risk-aggressive: #EF4444;      /* High risk */
  
  /* Time Period Colors */
  --period-day: #06B6D4;          /* 1D performance */
  --period-week: #8B5CF6;         /* 1W performance */
  --period-month: #10B981;        /* 1M performance */
  --period-year: #F59E0B;         /* 1Y performance */
}
```

## 🏗️ Component Architecture

### Core Portfolio Components
```typescript
// Main Portfolio Components
- PortfolioOverview: Total value and day change summary
- PerformanceChart: Historical performance visualization
- HoldingsTable: Detailed position breakdown
- AssetAllocation: Pie/donut charts for diversification
- PerformanceMetrics: Key performance indicators
- TaxSummary: Capital gains and tax implications
- Watchlist: Tracked symbols and alerts
- RecentActivity: Latest trades and actions
```

## 📱 Component Specifications

### 1. Portfolio Overview Component

#### Visual Design & Props
```typescript
interface PortfolioOverviewProps {
  totalValue: number;
  invested: number;
  dayChange: number;
  dayChangePercent: number;
  totalGainLoss: number;
  totalGainLossPercent: number;
  lastUpdated: Date;
  isMarketOpen: boolean;
}

interface PortfolioSummary {
  currentValue: number;
  totalInvested: number;
  totalGains: number;
  totalLosses: number;
  unrealizedPnL: number;
  realizedPnL: number;
  portfolioReturn: number;
  annualizedReturn: number;
}
```

#### Portfolio Overview Layout (Mobile)
```
┌─────────────────────────┐
│ Portfolio Value         │ 
│ ₹12,45,678             │ Large, prominent number
│ +₹23,456 (+1.92%)      │ Day change (green/red)
├─────────────────────────┤
│ Invested: ₹11,22,222    │ Original investment
│ Returns: ₹1,23,456      │ Total gains
│ Return %: +11.02%       │ Overall percentage
├─────────────────────────┤
│ ●●●●○○○ 67% Goal        │ Progress to financial goal
│ Target: ₹18,50,000      │ Goal amount
├─────────────────────────┤
│ Last Updated: 2:45 PM   │ Real-time indicator
│ 🔴 Market Closed        │ Market status
└─────────────────────────┘
```

#### Hero Card Animation
```css
.portfolio-hero-card {
  background: linear-gradient(135deg, 
    rgba(30, 27, 75, 0.4) 0%, 
    rgba(139, 92, 246, 0.1) 100%);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(139, 92, 246, 0.2);
  border-radius: 24px;
  padding: 24px;
  transition: all 0.3s ease;
}

.value-counter {
  font-size: 2.5rem;
  font-weight: 700;
  line-height: 1.2;
  background: linear-gradient(135deg, #F8FAFC, #E2E8F0);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.change-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 1.25rem;
  font-weight: 600;
}

.change-positive {
  color: var(--portfolio-gain);
}

.change-negative {
  color: var(--portfolio-loss);
}

/* Animated number counter */
@keyframes countUp {
  from { transform: translateY(10px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}

.value-animate {
  animation: countUp 0.5s ease-out;
}
```

### 2. Performance Chart Component

#### Interactive Performance Chart
```typescript
interface PerformanceChartProps {
  data: PerformanceDataPoint[];
  timeframe: '1D' | '1W' | '1M' | '3M' | '6M' | '1Y' | 'ALL';
  showBenchmark: boolean;
  benchmarkIndex?: 'NIFTY' | 'SENSEX' | 'NIFTY_NEXT50';
  chartType: 'line' | 'area' | 'candle';
  showReturns: 'absolute' | 'percentage';
}

interface PerformanceDataPoint {
  timestamp: Date;
  portfolioValue: number;
  invested: number;
  returns: number;
  benchmarkValue?: number;
}
```

#### Performance Chart Layout
```
┌─────────────────────────┐
│ Performance vs NIFTY    │ Chart header
├─────────────────────────┤
│                         │
│    📈 Chart Area        │ 200px height
│  (Touch/pinch enabled)  │ Interactive
│                         │
├─────────────────────────┤
│ [1D][1W][1M][3M][1Y]   │ 40px - Time selector
├─────────────────────────┤
│ Portfolio: +11.2%       │ Performance vs
│ NIFTY 50:  +8.7%       │ benchmark
│ Alpha:     +2.5%        │ (outperformance)
└─────────────────────────┘
```

#### Chart Interaction Features
```typescript
// Chart interaction capabilities
interface ChartInteraction {
  pinchToZoom: boolean;
  dragToPan: boolean;
  tapForDetails: boolean;
  crosshairOnHover: boolean;
  timeRangeSelection: boolean;
}

// Chart tooltip content
interface ChartTooltip {
  date: string;
  portfolioValue: string;
  dayChange: string;
  benchmarkValue?: string;
  annotations?: ChartAnnotation[];
}

interface ChartAnnotation {
  type: 'trade' | 'dividend' | 'split' | 'news';
  timestamp: Date;
  title: string;
  description: string;
  impact: 'positive' | 'negative' | 'neutral';
}
```

### 3. Holdings Table Component

#### Detailed Holdings Breakdown
```typescript
interface HoldingsTableProps {
  holdings: HoldingPosition[];
  sortBy: 'name' | 'value' | 'returns' | 'allocation';
  sortOrder: 'asc' | 'desc';
  groupBy: 'none' | 'sector' | 'asset_type';
  showColumns: ColumnConfig;
}

interface HoldingPosition {
  symbol: string;
  companyName: string;
  quantity: number;
  avgPrice: number;
  currentPrice: number;
  marketValue: number;
  dayChange: number;
  dayChangePercent: number;
  totalReturn: number;
  totalReturnPercent: number;
  allocation: number; // Percentage of portfolio
  sector: string;
  assetType: 'EQUITY' | 'ETF' | 'MUTUAL_FUND';
  lastUpdated: Date;
}
```

#### Holdings Table Layout (Mobile)
```
┌─────────────────────────┐
│ Holdings (12 stocks)    │ Header
│ Sort: Value ↓  Filter   │ Controls
├─────────────────────────┤
│ RELIANCE                │ 
│ Reliance Industries     │ Company name
│ 100 × ₹2,345.60        │ Qty × Price
│ ₹2,34,560 (18.8%)      │ Value (allocation)
│ +₹2,560 (+1.1%) 📈     │ Returns + trend
├─────────────────────────┤
│ INFY                    │
│ Infosys Limited         │ Swipe left for
│ 50 × ₹1,234.50         │ actions:
│ ₹61,725 (4.9%)         │ • Sell
│ -₹1,275 (-2.0%) 📉     │ • Add more
├─────────────────────────┤ • Set alert
│ TCS                     │
│ Tata Consultancy       │
│ 25 × ₹3,456.78         │
│ ₹86,420 (6.9%)         │
│ +₹4,320 (+5.2%) 📈     │
├─────────────────────────┤
│ ⋮ More holdings         │
│ [View All 12 Stocks]    │ Expand to full list
└─────────────────────────┘
```

#### Holdings Interaction Features
```typescript
// Swipe actions for holdings
interface HoldingSwipeActions {
  swipeLeft: {
    actions: ('sell' | 'add_more' | 'set_alert' | 'view_details')[];
    threshold: number; // Swipe distance
  };
  swipeRight: {
    actions: ('quick_trade' | 'analyze' | 'news')[];
    threshold: number;
  };
}

// Long press context menu
interface HoldingContextMenu {
  actions: {
    label: string;
    icon: string;
    action: () => void;
    destructive?: boolean;
  }[];
}
```

### 4. Asset Allocation Visualization

#### Diversification Analysis
```typescript
interface AssetAllocationProps {
  allocations: AllocationBreakdown[];
  chartType: 'donut' | 'pie' | 'treemap' | 'bar';
  groupBy: 'sector' | 'asset_type' | 'market_cap' | 'geography';
  showPercentages: boolean;
  interactive: boolean;
}

interface AllocationBreakdown {
  category: string;
  value: number;
  percentage: number;
  color: string;
  holdings: string[]; // List of symbols in this category
  target?: number; // Target allocation percentage
  deviation?: number; // Deviation from target
}
```

#### Asset Allocation Layout
```
┌─────────────────────────┐
│ Asset Allocation        │
├─────────────────────────┤
│       🍩 Donut Chart    │ Interactive chart
│     (Tap segments for   │ 180px height
│      detailed view)     │
├─────────────────────────┤
│ 📊 Technology    45.2%  │ Legend with colors
│ 🏦 Banking      23.8%  │ and percentages
│ 🏭 Manufacturing 15.6% │
│ 💊 Pharma       10.1%  │
│ 💰 Cash          5.3%  │
├─────────────────────────┤
│ ⚠️ High Tech exposure   │ Diversification
│ Consider rebalancing    │ recommendations
└─────────────────────────┘
```

### 5. Performance Metrics Dashboard

#### Key Performance Indicators
```typescript
interface PerformanceMetricsProps {
  metrics: PortfolioMetrics;
  benchmarkComparison: BenchmarkComparison;
  riskMetrics: RiskMetrics;
  timeframe: string;
}

interface PortfolioMetrics {
  totalReturn: number;
  annualizedReturn: number;
  volatility: number;
  sharpeRatio: number;
  maxDrawdown: number;
  winRate: number;
  avgWin: number;
  avgLoss: number;
  profitFactor: number;
}

interface BenchmarkComparison {
  benchmarkReturn: number;
  alpha: number;
  beta: number;
  correlation: number;
  informationRatio: number;
  trackingError: number;
}

interface RiskMetrics {
  valueAtRisk: number; // VaR 95%
  expectedShortfall: number; // CVaR
  maxDrawdown: number;
  volatility: number;
  riskScore: number; // 1-10 scale
}
```

#### Metrics Dashboard Layout
```
┌─────────────────────────┐
│ Performance Metrics     │
├─────────────────────────┤
│ Returns                 │
│ 1 Year:  +15.2% 📈      │ Time-based returns
│ 3 Year:  +12.8% 📈      │ with trend indicators
│ 5 Year:  +9.4%  📈      │
├─────────────────────────┤
│ Risk Metrics            │
│ Volatility:   18.5%     │ Risk measurements
│ Max Drawdown: -12.3%    │ with explanations
│ Sharpe Ratio: 1.25      │ (tap for details)
├─────────────────────────┤
│ vs NIFTY 50             │
│ Alpha:   +2.4%  ✅      │ Benchmark comparison
│ Beta:    1.15   ⚠️      │ with color coding
│ Correlation: 0.78       │
└─────────────────────────┘
```

### 6. Tax Summary & Reporting

#### Tax Implications Dashboard
```typescript
interface TaxSummaryProps {
  taxYear: string;
  capitalGains: CapitalGainsBreakdown;
  dividendIncome: number;
  taxableEvents: TaxableEvent[];
  projectedTax: number;
  harvesting: TaxHarvestingOpportunity[];
}

interface CapitalGainsBreakdown {
  shortTermGains: number;
  longTermGains: number;
  shortTermLosses: number;
  longTermLosses: number;
  netShortTerm: number;
  netLongTerm: number;
}

interface TaxHarvestingOpportunity {
  symbol: string;
  unrealizedLoss: number;
  taxSaving: number;
  recommendation: string;
  confidence: number;
}
```

#### Tax Summary Layout
```
┌─────────────────────────┐
│ Tax Summary FY 2024-25  │
├─────────────────────────┤
│ Capital Gains           │
│ STCG: +₹45,000         │ Short-term gains
│ LTCG: +₹1,25,000       │ Long-term gains
│ Net Tax: ₹18,750       │ Estimated tax
├─────────────────────────┤
│ Tax Saving Opportunities│
│ Sell TCS for ₹8,500 loss│ Tax harvesting
│ Save ₹2,550 in taxes   │ suggestions
├─────────────────────────┤
│ [Generate Tax Report]   │ Export options
│ [Download ITR Helper]   │
└─────────────────────────┘
```

## 🎨 Interactive Features

### Real-time Value Updates
```typescript
// Live portfolio value updates
const usePortfolioRealtime = (holdings: HoldingPosition[]) => {
  const [portfolioValue, setPortfolioValue] = useState(0);
  const [dayChange, setDayChange] = useState(0);
  
  useEffect(() => {
    const ws = new WebSocket(MARKET_DATA_WS_URL);
    
    ws.onmessage = (event) => {
      const priceUpdate = JSON.parse(event.data);
      
      // Update portfolio value calculation
      const newValue = calculatePortfolioValue(holdings, priceUpdate);
      const change = newValue - portfolioValue;
      
      // Animate value changes
      setPortfolioValue(newValue);
      setDayChange(change);
      
      // Trigger flash animation for positive/negative changes
      triggerValueFlashAnimation(change > 0 ? 'positive' : 'negative');
    };
    
    return () => ws.close();
  }, [holdings]);
  
  return { portfolioValue, dayChange };
};

// Value flash animation
const triggerValueFlashAnimation = (type: 'positive' | 'negative') => {
  const element = document.querySelector('.portfolio-value');
  element?.classList.add(`flash-${type}`);
  setTimeout(() => {
    element?.classList.remove(`flash-${type}`);
  }, 300);
};
```

### Drag and Drop Reordering
```typescript
// Holdings list reordering
interface DragDropHoldingsProps {
  holdings: HoldingPosition[];
  onReorder: (newOrder: HoldingPosition[]) => void;
}

const DragDropHoldings = ({ holdings, onReorder }) => {
  const handleDragEnd = (result: DropResult) => {
    if (!result.destination) return;
    
    const items = Array.from(holdings);
    const [reorderedItem] = items.splice(result.source.index, 1);
    items.splice(result.destination.index, 0, reorderedItem);
    
    onReorder(items);
  };
  
  return (
    <DragDropContext onDragEnd={handleDragEnd}>
      <Droppable droppableId="holdings">
        {(provided) => (
          <div {...provided.droppableProps} ref={provided.innerRef}>
            {holdings.map((holding, index) => (
              <Draggable key={holding.symbol} draggableId={holding.symbol} index={index}>
                {(provided, snapshot) => (
                  <HoldingCard
                    ref={provided.innerRef}
                    {...provided.draggableProps}
                    {...provided.dragHandleProps}
                    holding={holding}
                    isDragging={snapshot.isDragging}
                  />
                )}
              </Draggable>
            ))}
            {provided.placeholder}
          </div>
        )}
      </Droppable>
    </DragDropContext>
  );
};
```

### Portfolio Goal Tracking
```typescript
interface PortfolioGoalProps {
  currentValue: number;
  targetValue: number;
  targetDate: Date;
  monthlyInvestment: number;
  projectedValue: number;
}

const PortfolioGoalTracker = ({ 
  currentValue, 
  targetValue, 
  targetDate, 
  monthlyInvestment 
}) => {
  const progress = (currentValue / targetValue) * 100;
  const monthsRemaining = differenceInMonths(targetDate, new Date());
  const requiredMonthly = (targetValue - currentValue) / monthsRemaining;
  
  return (
    <div className="goal-tracker-card">
      <div className="goal-header">
        <h3>Financial Goal</h3>
        <span className="target-amount">₹{formatCurrency(targetValue)}</span>
      </div>
      
      <div className="progress-section">
        <div className="progress-bar">
          <div 
            className="progress-fill"
            style={{ width: `${Math.min(progress, 100)}%` }}
          />
        </div>
        <div className="progress-stats">
          <span>{progress.toFixed(1)}% Complete</span>
          <span>{monthsRemaining} months left</span>
        </div>
      </div>
      
      <div className="recommendation">
        {requiredMonthly > monthlyInvestment ? (
          <div className="increase-investment">
            ⚠️ Increase monthly investment to ₹{formatCurrency(requiredMonthly)}
          </div>
        ) : (
          <div className="on-track">
            ✅ On track to meet your goal!
          </div>
        )}
      </div>
    </div>
  );
};
```

## ✅ Acceptance Criteria

### Functional Requirements
- [ ] **Real-time Updates**: Portfolio value updates within 1 second of price changes
- [ ] **Performance Tracking**: Historical performance charts for multiple timeframes
- [ ] **Holdings Management**: Detailed view of all positions with real-time P&L
- [ ] **Asset Allocation**: Visual breakdown by sector, asset type, market cap
- [ ] **Tax Reporting**: Capital gains calculation and tax harvesting suggestions
- [ ] **Goal Tracking**: Progress monitoring towards financial goals
- [ ] **Benchmark Comparison**: Performance vs market indices (NIFTY, SENSEX)

### Performance Requirements
- [ ] **Load Time**: Dashboard loads within 3 seconds on 4G
- [ ] **Chart Rendering**: Performance charts render within 1 second
- [ ] **Scroll Performance**: 60fps scrolling through holdings list
- [ ] **Memory Usage**: <200MB RAM for full portfolio dashboard
- [ ] **Battery Efficiency**: <5% battery per hour for portfolio monitoring

### Visual Requirements
- [ ] **Design Consistency**: Matches TradeMaster design system
- [ ] **Color Coding**: Consistent green/red for gains/losses throughout
- [ ] **Charts**: Interactive charts with touch gestures (pinch, pan, tap)
- [ ] **Mobile Optimization**: All content readable and interactive on 375px+ screens
- [ ] **Loading States**: Skeleton loaders for all data sections

### Data Requirements
- [ ] **Accuracy**: 99.9% accuracy for portfolio calculations and P&L
- [ ] **Completeness**: Support for stocks, ETFs, mutual funds, FDs
- [ ] **Historical Data**: Performance tracking back to account inception
- [ ] **Tax Compliance**: Accurate STCG/LTCG calculations per Indian tax law
- [ ] **Benchmark Data**: Real-time index values for comparison

### Accessibility Requirements
- [ ] **Screen Reader**: Full VoiceOver/TalkBack support for all charts and data
- [ ] **Color Blind Support**: Patterns and icons supplement color coding
- [ ] **Large Text**: Readable with system font scaling up to 200%
- [ ] **Voice Control**: Basic voice commands for navigation
- [ ] **Motor Accessibility**: Support for switch control and assistive devices

## 🧪 Testing Strategy

### Unit Testing
```typescript
// Portfolio calculation tests
describe('Portfolio Calculations', () => {
  it('should calculate total portfolio value correctly', () => {
    const holdings = [
      { symbol: 'RELIANCE', quantity: 100, currentPrice: 2350 },
      { symbol: 'INFY', quantity: 50, currentPrice: 1234.50 }
    ];
    
    const totalValue = calculatePortfolioValue(holdings);
    expect(totalValue).toBe(296725); // 235000 + 61725
  });
  
  it('should calculate day change percentage correctly', () => {
    const dayChange = calculateDayChangePercent(296725, 295000);
    expect(dayChange).toBeCloseTo(0.58, 2);
  });
  
  it('should handle zero division in percentage calculations', () => {
    const result = calculateReturnPercent(1000, 0);
    expect(result).toBe(0);
  });
});
```

### Performance Testing
```typescript
// Chart rendering performance
describe('Chart Performance', () => {
  it('should render 1000 data points within 500ms', async () => {
    const startTime = performance.now();
    const data = generatePerformanceData(1000);
    
    render(<PerformanceChart data={data} />);
    
    await waitFor(() => {
      const endTime = performance.now();
      expect(endTime - startTime).toBeLessThan(500);
    });
  });
  
  it('should handle real-time updates without frame drops', async () => {
    // Test 60fps performance during live updates
  });
});
```

### Integration Testing
```typescript
// Real-time data integration
describe('Portfolio Real-time Updates', () => {
  it('should update portfolio value when stock prices change', async () => {
    const mockWebSocket = createMockWebSocket();
    render(<PortfolioOverview />);
    
    mockWebSocket.sendPriceUpdate({
      symbol: 'RELIANCE',
      price: 2400, // +50 from current
      change: 50
    });
    
    await waitFor(() => {
      expect(screen.getByText(/₹3,01,725/)).toBeInTheDocument();
    });
  });
});
```

### Visual Testing
```typescript
// Storybook visual tests
export default {
  title: 'Portfolio/Dashboard',
  component: PortfolioDashboard,
} as ComponentMeta<typeof PortfolioDashboard>;

export const Default = () => (
  <PortfolioDashboard portfolio={mockPortfolio} />
);

export const ProfitablePortfolio = () => (
  <PortfolioDashboard portfolio={profitablePortfolio} />
);

export const LossPortfolio = () => (
  <PortfolioDashboard portfolio={lossPortfolio} />
);

export const EmptyPortfolio = () => (
  <PortfolioDashboard portfolio={emptyPortfolio} />
);
```

### Accessibility Testing
- **Screen Reader**: Test with VoiceOver (iOS) and TalkBack (Android)
- **Keyboard Navigation**: Ensure all interactive elements are accessible via keyboard
- **Color Contrast**: Verify 4.5:1 contrast ratio for all text
- **Focus Management**: Logical focus order through dashboard components
- **Voice Control**: Test basic voice navigation commands

## 🚀 Implementation Plan

### Week 1: Core Dashboard
- **Day 1-2**: Portfolio overview component with real-time values
- **Day 3-4**: Holdings table with sorting and filtering
- **Day 5**: Performance chart with basic timeframe selection

### Week 2: Advanced Features
- **Day 1-2**: Asset allocation visualization and interactive charts
- **Day 3-4**: Performance metrics and benchmark comparison
- **Day 5**: Tax summary and goal tracking components

### Week 3: Polish & Integration
- **Day 1-2**: Real-time WebSocket integration and animations
- **Day 3-4**: Mobile optimization and gesture controls
- **Day 5**: Testing, accessibility, and performance optimization

## 📊 Success Metrics

### User Engagement
- **Dashboard Usage**: >80% of users visit portfolio dashboard daily
- **Time on Dashboard**: >3 minutes average session time
- **Feature Adoption**: >60% users interact with performance charts
- **Goal Setting**: >40% users set up financial goals

### Technical Performance
- **Load Performance**: <3 seconds to load complete dashboard
- **Update Latency**: <1 second for real-time portfolio value updates
- **Chart Performance**: 60fps for all chart interactions
- **Error Rate**: <0.5% for portfolio calculations

### Business Impact
- **User Retention**: 8% improvement in 30-day user retention
- **Premium Upgrades**: 12% increase in users upgrading for advanced analytics
- **Trading Activity**: 18% increase in trades from portfolio insights
- **Support Reduction**: 25% fewer support tickets about portfolio tracking

---

**Dependencies**: 
- Epic 2.1 Market Data Service (for real-time pricing)
- Epic 2.2 Trading Service (for trade history and positions)
- Tax calculation service integration

**Blockers**: None identified  
**Risk Level**: Medium - Complex calculations and real-time data handling  
**Review Required**: Financial accuracy validation and compliance review