# UI Story 2.2: Trading Interface

**Epic**: 2 - Market Data & Trading Foundation  
**Story**: Advanced Trading Interface with Order Management  
**Priority**: Revenue Critical  
**Complexity**: High  
**Duration**: 2 weeks  

## 📋 Story Overview

**As a** retail trader using TradeMaster mobile app  
**I want** an intuitive trading interface with advanced order types and risk management  
**So that** I can execute trades quickly and safely with institutional-grade features

## 🎯 Business Value

- **Revenue Generation**: Core feature for trading fee income
- **User Retention**: Professional trading tools increase platform stickiness
- **Risk Management**: Reduces platform liability through built-in safety features
- **Mobile Optimization**: 85% of retail trades happen on mobile devices

## 🖼️ UI Requirements

### Design System Consistency
- **Theme**: Extend existing dark fintech theme with trading-specific colors
- **Risk Indicators**: Red/amber/green color coding for risk levels
- **Components**: Build on established `cyber-button`, `glass-card` patterns
- **Mobile-First**: Single-thumb operation for quick trades
- **Accessibility**: Voice commands and screen reader support

### Trading-Specific Color System
```css
:root {
  /* Trading Actions */
  --trade-buy: #22C55E;        /* Buy orders */
  --trade-sell: #EF4444;       /* Sell orders */
  --trade-pending: #F59E0B;    /* Pending orders */
  --trade-executed: #8B5CF6;   /* Completed trades */
  
  /* Risk Levels */
  --risk-low: #22C55E;         /* Green - Safe */
  --risk-medium: #F59E0B;      /* Amber - Caution */
  --risk-high: #EF4444;        /* Red - Danger */
  --risk-critical: #DC2626;    /* Dark red - Stop */
  
  /* Order Types */
  --order-market: #3B82F6;     /* Market orders */
  --order-limit: #8B5CF6;      /* Limit orders */
  --order-stop: #F59E0B;       /* Stop orders */
  --order-bracket: #10B981;    /* Bracket orders */
}
```

## 🏗️ Component Architecture

### Core Trading Components
```typescript
// Primary Trading Components
- OrderForm: Multi-type order placement
- QuickTradeButtons: One-tap buy/sell
- PositionManager: Active positions overview
- OrderHistory: Trade execution history
- RiskMeter: Real-time risk assessment
- BrokerSelector: Multi-broker switching
- TradeConfirmation: Order confirmation dialog
```

## 📱 Component Specifications

### 1. Order Form Component

#### Visual Design & Props
```typescript
interface OrderFormProps {
  symbol: string;
  currentPrice: number;
  orderTypes: OrderType[];
  maxQuantity: number;
  availableBalance: number;
  riskLimits: RiskLimits;
  brokerAccounts: BrokerAccount[];
  onOrderSubmit: (order: OrderRequest) => Promise<void>;
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
```

#### Mobile Layout Specification
```
┌─────────────────────────┐
│ RELIANCE • ₹2,345.60    │ 48px - Symbol header
│ NSE • Last: 14:30       │        with live price
├─────────────────────────┤
│ [  BUY  ] [  SELL  ]    │ 56px - Toggle buttons
│   Active     Inactive   │        (green/red theme)
├─────────────────────────┤
│ Quantity                │ 
│ [  -  ] [ 100 ] [ + ]   │ 56px - Stepper control
│ [25] [50] [100] [Max]   │ 44px - Quick amounts
├─────────────────────────┤
│ Order Type              │
│ [ Market Order    ▼ ]   │ 48px - Dropdown
├─────────────────────────┤
│ Price (Optional)        │ Only for limit orders
│ [ ₹2,345.60 ]          │ 48px - Price input
├─────────────────────────┤
│ Est. Value: ₹2,34,560   │ 32px - Auto-calculation
│ Available: ₹5,00,000    │        Balance check
├─────────────────────────┤
│ Risk Level: ●●○○○       │ 32px - Risk indicator
│ Low Risk • 2% of port.  │        with explanation
├─────────────────────────┤
│ [ Place Buy Order ]     │ 56px - Primary action
│   ₹2,34,560            │        with amount
└─────────────────────────┘
Total Height: ~440px (optimized for mobile)
```

#### Advanced Order Types Interface
```typescript
// For LIMIT orders - additional fields
interface LimitOrderFields {
  limitPrice: number;
  timeInForce: 'DAY' | 'IOC' | 'GTD';
  expiryDate?: Date;
}

// For STOP_LOSS orders
interface StopLossFields {
  triggerPrice: number;
  limitPrice?: number; // For stop-limit orders
}

// For BRACKET orders
interface BracketOrderFields {
  targetPrice: number;
  stopLossPrice: number;
  trailingStopLoss?: boolean;
}
```

### 2. Quick Trade Buttons

#### One-Thumb Trading Interface
```typescript
interface QuickTradeProps {
  symbol: string;
  presetAmounts: number[];
  defaultAmount: number;
  quickOrderType: 'MARKET' | 'LIMIT_BEST';
  riskWarnings: boolean;
}
```

#### Mobile Quick Trade Layout
```
┌─────────────────────────┐
│ Quick Trade Mode        │ 40px header
├─────────────────────────┤
│                         │
│    [    QUICK BUY    ]  │ 120px
│      Swipe up ↑         │ Large touch area
│    ₹50,000 • Market     │ Amount/type
│                         │
├─────────────────────────┤
│                         │
│   [   QUICK SELL   ]    │ 120px  
│     Swipe down ↓        │ Large touch area
│   Current Holding       │ Position info
│                         │
├─────────────────────────┤
│ [₹10K] [₹25K] [₹50K]   │ 48px - Quick amounts
├─────────────────────────┤
│ Last: RELIANCE +₹25.60  │ 32px - Last order
└─────────────────────────┘
```

#### Gesture Controls
```typescript
interface GestureConfig {
  swipeUpBuy: boolean;      // Swipe up for buy
  swipeDownSell: boolean;   // Swipe down for sell
  longPressAdvanced: boolean; // Long press for advanced options
  doubleTapConfirm: boolean;  // Double tap to confirm
  hapticFeedback: boolean;    // Vibration feedback
}
```

### 3. Position Manager

#### Active Positions Display
```typescript
interface PositionCardProps {
  symbol: string;
  companyName: string;
  quantity: number;
  avgPrice: number;
  currentPrice: number;
  pnl: number;
  pnlPercent: number;
  marketValue: number;
  lastUpdate: Date;
  onClose: () => void;
  onModify: () => void;
}
```

#### Position Card Layout
```
┌─────────────────────────┐
│ RELIANCE                │ Symbol & name
│ Reliance Industries     │
├─────────────────────────┤
│ Qty: 100 @ ₹2,320.00   │ Position details
│ CMP: ₹2,345.60          │ Current price
├─────────────────────────┤
│ P&L: +₹2,560 (+1.1%)   │ Profit/Loss
│ Value: ₹2,34,560        │ Market value
├─────────────────────────┤
│ [Sell 25%] [Sell All]  │ Quick actions
│ [Set Alert] [Stop Loss] │
└─────────────────────────┘
```

#### Swipe Actions
```typescript
// Swipe gestures for position management
interface SwipeActions {
  swipeLeft: 'close_position' | 'set_stop_loss';
  swipeRight: 'add_more' | 'set_target';
  longPress: 'show_details' | 'modify_order';
}
```

### 4. Risk Management Interface

#### Real-time Risk Assessment
```typescript
interface RiskMeterProps {
  currentRisk: RiskLevel;
  portfolioExposure: number;
  sectorConcentration: SectorExposure[];
  dayTradingLimit: number;
  marginUtilization: number;
  maxDrawdown: number;
}

type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

interface RiskWarning {
  level: RiskLevel;
  message: string;
  action: 'BLOCK' | 'WARN' | 'MONITOR';
  suggestions: string[];
}
```

#### Risk Meter Display
```
┌─────────────────────────┐
│ Portfolio Risk Level    │ Header
├─────────────────────────┤
│     ●●●○○○○             │ Risk gauge (3/7)
│    MEDIUM RISK          │ Level indicator
├─────────────────────────┤
│ Exposure: 75% of port.  │ Risk metrics
│ Margin Used: 45%        │
│ Sector Risk: Tech 40%   │
├─────────────────────────┤
│ ⚠️ Consider reducing    │ Recommendations
│    technology exposure  │
│ 📊 Set stop losses     │
└─────────────────────────┘
```

### 5. Order History & Status

#### Order Tracking Interface
```typescript
interface OrderHistoryProps {
  orders: OrderRecord[];
  filters: OrderFilter;
  sortBy: 'timestamp' | 'symbol' | 'status';
  groupBy: 'day' | 'symbol' | 'none';
}

interface OrderRecord {
  orderId: string;
  symbol: string;
  side: 'BUY' | 'SELL';
  orderType: string;
  quantity: number;
  price: number;
  status: OrderStatus;
  timestamp: Date;
  executionDetails?: ExecutionDetails;
}

type OrderStatus = 'PENDING' | 'PARTIALLY_FILLED' | 'FILLED' | 'CANCELLED' | 'REJECTED';
```

#### Order History Layout
```
┌─────────────────────────┐
│ Today's Orders (5)      │ Header with count
├─────────────────────────┤
│ ✅ RELIANCE BUY         │ Status icon
│ 100 @ ₹2,345.60         │ Order details  
│ Filled • 14:25          │ Status & time
├─────────────────────────┤
│ ⏳ INFY SELL            │ Pending order
│ 50 @ ₹1,250.00          │ (orange icon)
│ Pending • 14:30         │
├─────────────────────────┤
│ ❌ TCS BUY              │ Rejected order
│ Insufficient balance    │ Error reason
│ Rejected • 14:15        │ (red icon)
├─────────────────────────┤
│ [View All Orders]       │ Expand button
└─────────────────────────┘
```

## 🎨 Interaction Design

### Mobile-First Touch Patterns

#### Primary Actions (44px+ touch targets)
```css
.primary-action-btn {
  min-height: 56px;
  min-width: 100%;
  font-size: 18px;
  font-weight: 600;
  border-radius: 16px;
  background: linear-gradient(135deg, var(--trade-buy), #16A34A);
  box-shadow: 0 4px 12px rgba(34, 197, 94, 0.3);
  transition: all 0.2s ease;
}

.primary-action-btn:active {
  transform: scale(0.98);
  box-shadow: 0 2px 8px rgba(34, 197, 94, 0.4);
}
```

#### Quantity Stepper Control
```typescript
interface QuantityStepperProps {
  value: number;
  min: number;
  max: number;
  step: number;
  quickValues: number[];
  onValueChange: (value: number) => void;
}

// Touch-friendly stepper with haptic feedback
const QuantityStepper = ({ value, onValueChange }) => (
  <div className="flex items-center gap-4">
    <button 
      className="w-12 h-12 rounded-full bg-slate-700 text-white text-xl"
      onClick={() => onValueChange(Math.max(min, value - step))}
    >
      -
    </button>
    <input 
      className="w-20 h-12 text-center text-lg bg-slate-800 rounded-lg"
      value={value}
      onChange={(e) => onValueChange(Number(e.target.value))}
    />
    <button 
      className="w-12 h-12 rounded-full bg-slate-700 text-white text-xl"
      onClick={() => onValueChange(Math.min(max, value + step))}
    >
      +
    </button>
  </div>
);
```

### Loading & Feedback States

#### Order Submission Flow
```typescript
// Order submission states
type OrderSubmissionState = 
  | { status: 'idle' }
  | { status: 'validating'; progress: number }
  | { status: 'submitting'; orderId?: string }
  | { status: 'success'; orderId: string; execution: ExecutionDetails }
  | { status: 'error'; error: string; retryable: boolean };

// Loading button with progress
const OrderSubmitButton = ({ state, onSubmit }) => {
  if (state.status === 'submitting') {
    return (
      <button disabled className="submit-btn submitting">
        <Spinner className="w-5 h-5 mr-2" />
        Placing Order...
      </button>
    );
  }
  
  return (
    <button onClick={onSubmit} className="submit-btn">
      Place Order
    </button>
  );
};
```

#### Success/Error Feedback
```typescript
// Toast notifications for order status
const OrderToast = ({ order, status }) => {
  const toastConfig = {
    success: {
      icon: '✅',
      color: 'green',
      message: `${order.side} order placed successfully`
    },
    error: {
      icon: '❌', 
      color: 'red',
      message: `Order failed: ${status.error}`
    },
    warning: {
      icon: '⚠️',
      color: 'amber',
      message: `Order partially filled: ${status.filledQty}/${order.quantity}`
    }
  };
  
  return (
    <div className={`toast toast-${toastConfig[status].color}`}>
      <span className="toast-icon">{toastConfig[status].icon}</span>
      <span className="toast-message">{toastConfig[status].message}</span>
    </div>
  );
};
```

## 🔧 Advanced Features

### Voice Trading Commands
```typescript
interface VoiceCommandsProps {
  enabled: boolean;
  supportedCommands: VoiceCommand[];
  confirmationRequired: boolean;
}

interface VoiceCommand {
  trigger: string;
  action: 'BUY' | 'SELL' | 'CANCEL' | 'STATUS';
  parameters?: VoiceParameters;
}

// Example voice commands:
// "Buy 100 Reliance at market"
// "Sell all Infosys"
// "Cancel last order"
// "What's my Reliance position?"
```

### Smart Order Suggestions
```typescript
interface OrderSuggestion {
  type: 'BETTER_PRICE' | 'STOP_LOSS' | 'PROFIT_BOOKING' | 'AVERAGING';
  suggestion: string;
  impact: 'POSITIVE' | 'NEUTRAL' | 'NEGATIVE';
  confidence: number; // 0-100
}

// Example suggestions:
const suggestions = [
  {
    type: 'BETTER_PRICE',
    suggestion: 'Consider limit order at ₹2,340 instead of market',
    impact: 'POSITIVE',
    confidence: 85
  },
  {
    type: 'STOP_LOSS', 
    suggestion: 'Set stop loss at ₹2,250 to limit downside',
    impact: 'POSITIVE',
    confidence: 90
  }
];
```

### Broker Integration Interface
```typescript
interface BrokerSwitcherProps {
  accounts: BrokerAccount[];
  activeAccount: string;
  onAccountSwitch: (accountId: string) => void;
}

interface BrokerAccount {
  id: string;
  brokerName: 'ZERODHA' | 'UPSTOX' | 'ANGEL' | 'PAYTM_MONEY';
  displayName: string;
  balance: number;
  marginAvailable: number;
  status: 'ACTIVE' | 'INACTIVE' | 'MAINTENANCE';
}
```

## ✅ Acceptance Criteria

### Functional Requirements
- [ ] **Order Placement**: All 15+ order types (Market, Limit, SL, Bracket, etc.)
- [ ] **Multi-Broker Support**: Switch between 3+ broker accounts
- [ ] **Real-time Updates**: Order status updates within 200ms
- [ ] **Risk Management**: Pre-trade risk checks and warnings
- [ ] **Position Tracking**: Real-time P&L updates for all holdings
- [ ] **Quick Trading**: One-tap buy/sell for frequent traders

### Mobile-Specific Requirements
- [ ] **Touch Optimization**: All elements have 44px+ touch targets
- [ ] **Gesture Support**: Swipe up/down for quick buy/sell
- [ ] **One-Handed Use**: Primary actions reachable with thumb
- [ ] **Haptic Feedback**: Vibration feedback for key actions
- [ ] **Voice Commands**: Basic voice trading support
- [ ] **Offline Handling**: Queue orders when connection lost

### Performance Requirements
- [ ] **Order Execution**: <200ms from submission to broker
- [ ] **UI Responsiveness**: <16ms frame time (60fps)
- [ ] **Memory Usage**: <150MB RAM on mid-range devices
- [ ] **Battery Efficiency**: <8% battery per hour of active trading
- [ ] **Network Resilience**: Handle 3G/4G/WiFi switching

### Security Requirements
- [ ] **Order Confirmation**: Mandatory confirmation for large orders
- [ ] **Biometric Auth**: Fingerprint/Face ID for order placement
- [ ] **Session Management**: Auto-logout after inactivity
- [ ] **Risk Limits**: Hard stops for position size/concentration
- [ ] **Audit Trail**: Complete logging of all trading actions

### Accessibility Requirements  
- [ ] **Screen Reader**: VoiceOver/TalkBack support for all elements
- [ ] **Voice Control**: Integration with system voice commands
- [ ] **High Contrast**: Support for accessibility color schemes
- [ ] **Large Text**: Readable with system font scaling
- [ ] **Motor Impairment**: Switch control and alternative inputs

## 🧪 Testing Strategy

### Unit Testing
```typescript
// Order form validation tests
describe('OrderForm Validation', () => {
  it('should prevent orders exceeding available balance', () => {
    const { getByRole } = render(
      <OrderForm 
        symbol="RELIANCE"
        availableBalance={10000}
        currentPrice={2350}
      />
    );
    
    fireEvent.change(getByRole('spinbutton'), { target: { value: '10' } });
    fireEvent.click(getByRole('button', { name: /place buy order/i }));
    
    expect(screen.getByText(/insufficient balance/i)).toBeInTheDocument();
  });
  
  it('should calculate order value correctly', () => {
    // Test order value calculation logic
  });
  
  it('should validate quantity within limits', () => {
    // Test quantity validation
  });
});
```

### Integration Testing
```typescript
// Broker integration tests
describe('Multi-Broker Integration', () => {
  it('should place order via Zerodha API', async () => {
    const mockZerodhaAPI = createMockBrokerAPI('ZERODHA');
    render(<TradingInterface brokerAPI={mockZerodhaAPI} />);
    
    // Simulate order placement
    fireEvent.click(getByText('Place Order'));
    
    await waitFor(() => {
      expect(mockZerodhaAPI.placeOrder).toHaveBeenCalledWith({
        symbol: 'RELIANCE',
        side: 'BUY',
        quantity: 100,
        price: 2350
      });
    });
  });
});
```

### Performance Testing
```typescript
// Real-time update performance
describe('Real-time Performance', () => {
  it('should update 100 positions within 100ms', async () => {
    const startTime = performance.now();
    
    // Simulate 100 position updates
    mockWebSocket.sendPositionUpdates(generatePositionUpdates(100));
    
    await waitFor(() => {
      const endTime = performance.now();
      expect(endTime - startTime).toBeLessThan(100);
    });
  });
});
```

### Mobile Testing
- **Device Testing**: iOS (iPhone 12+), Android (Samsung, OnePlus)
- **Network Testing**: 2G/3G/4G/5G/WiFi conditions
- **Battery Testing**: Background app behavior and power consumption
- **Gesture Testing**: Swipe, pinch, long press accuracy
- **Accessibility**: VoiceOver, TalkBack, Switch Control

## 🚀 Implementation Phases

### Phase 1: Core Trading (Week 1)
- **Day 1-2**: OrderForm component with basic order types
- **Day 3-4**: Quick trade buttons and gesture controls
- **Day 5**: Risk meter and validation logic

### Phase 2: Position Management (Week 2)  
- **Day 1-2**: Position cards and P&L tracking
- **Day 3-4**: Order history and status tracking
- **Day 5**: Broker integration interface

### Phase 3: Advanced Features (Week 3)
- **Day 1-2**: Advanced order types (Bracket, Cover)
- **Day 3-4**: Voice commands and smart suggestions
- **Day 5**: Performance optimization and testing

## 📊 Success Metrics

### User Engagement
- **Order Conversion**: >5% of chart views result in orders
- **Quick Trade Usage**: >40% of orders use quick trade feature
- **Order Completion**: >95% successful order placement rate
- **User Satisfaction**: >4.5/5 rating for trading interface

### Technical Performance
- **Order Latency**: <200ms average order submission time
- **UI Performance**: 60fps during high-frequency updates
- **Error Rate**: <0.5% for order placement failures
- **Uptime**: 99.95% availability during trading hours

### Business Impact
- **Trading Volume**: 25% increase in daily trading volume
- **Revenue per User**: 15% increase in trading fees
- **Feature Adoption**: 70% of users try advanced order types
- **Support Reduction**: 30% fewer trading-related support tickets

---

**Dependencies**: 
- Epic 2.2 Trading Service API completion
- Epic 2.1 Market Data Dashboard (for pricing)
- Broker API integrations (Zerodha, Upstox, etc.)

**Blockers**: None identified  
**Risk Level**: High - Critical revenue feature with complex broker integrations  
**Review Required**: Trading compliance and risk management approval