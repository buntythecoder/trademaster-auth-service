# UI Wireframes & Design Specifications
## TradeMaster Orchestrator - Multi-Broker Trading Platform

**Document Version:** 1.0  
**Last Updated:** August 23, 2025  
**Document Owner:** Design Team  
**Status:** Draft  

---

## Design System Analysis

### Current TradeMaster Aesthetic
Based on the existing codebase analysis, TradeMaster uses a sophisticated fintech design system:

**Color Palette:**
- **Primary Dark**: `#0F0D23` (Dark purple navy background)
- **Card Background**: `#151324` (Dark purple cards) 
- **Accent Purple**: `#8B5CF6` (Neon purple - primary CTA)
- **Text Primary**: `#F8FAFC` (Light text)
- **Text Secondary**: `#94A3B8` (Slate-400 muted text)
- **Success**: `#22C55E` (Neon green)
- **Error**: `#EF4444` (Neon red)

**UI Components:**
- **Glass Cards**: `backdrop-filter: blur(20px)` with purple borders
- **Cyber Buttons**: Gradient backgrounds with hover animations
- **Typography**: Inter font with gradient text effects
- **Animations**: Particle systems, floating shapes, smooth transitions

**Mobile-First Approach:**
- Touch targets: minimum 44px height
- Responsive containers with proper spacing
- PWA-ready with gesture support

---

## Core Wireframes

### 1. Multi-Broker Dashboard (Main Hub)

```
╭─────────────────────────────────────────────────────────────╮
│  ≡  🏠 TradeMaster           💰 ₹12,45,000    🔔 [3]  👤    │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  📊 Portfolio Overview               🎯 Quick Actions        │
│  ╭─────────────────────────╮        ╭─────────────────────╮ │
│  │ Total Value: ₹12,45,000 │        │ [💰 Buy] [📈 Sell] │ │
│  │ Day P&L: +₹15,240 ↗    │        │ [🔍 Search]        │ │
│  │ Total P&L: +₹2,45,000   │        │ [⚡ Quick Order]    │ │
│  │ ═══════════════════     │        │ [📊 Analytics]     │ │
│  │ ████████████▓▓▓ 78%     │        ╰─────────────────────╯ │
│  ╰─────────────────────────╯                                │
│                                                             │
│  🏦 Broker Status                   📈 Market Overview      │
│  ╭─────────────────────────╮        ╭─────────────────────╮ │
│  │ ✅ Zerodha    ₹4.2L     │        │ NIFTY: 19,435 +0.8% │ │
│  │ ✅ Groww      ₹3.8L     │        │ SENSEX: 65,123 +1.2%│ │
│  │ ✅ Angel One  ₹2.5L     │        │ BANKNIFTY: 44,891   │ │
│  │ ⚠️  ICICI     ₹1.8L     │        │ Market: 🟢 OPEN     │ │
│  │ 🔄 Upstox     ₹0.5L     │        ╰─────────────────────╯ │
│  ╰─────────────────────────╯                                │
│                                                             │
│  📊 Top Holdings                    🎯 AI Recommendations   │
│  ╭─────────────────────────────────────────────────────────╮ │
│  │ RELIANCE  ₹2.4L  +2.3%  [40%] ████████████████▓▓▓▓     │ │
│  │ TCS       ₹1.8L  -1.2%  [30%] ████████████▓▓▓▓▓▓▓▓     │ │
│  │ INFY      ₹1.2L  +0.8%  [20%] ████████▓▓▓▓▓▓▓▓▓▓▓▓     │ │
│  │ HDFC      ₹0.6L  +3.1%  [10%] ████▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓     │ │
│  ╰─────────────────────────────────────────────────────────╯ │
│                                                             │
│  🤖 AI Insights: "Consider profit booking in RELIANCE"     │
│  📈 Trend Alert: "Banking sector showing strong momentum"   │
╰─────────────────────────────────────────────────────────────╯
```

**Component Mapping:**
- Glass cards with `glass-card` class
- Purple accent colors for positive P&L
- Status indicators with color coding
- Progress bars using existing progress components
- AI insights with neon text effects

### 2. Intelligent Order Placement Interface

```
╭─────────────────────────────────────────────────────────────╮
│ ← Smart Order Placement                    🎯 Route Order   │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  🔍 Symbol Search                                           │
│  ╭─────────────────────────────────────────────────────────╮ │
│  │ > RELIANCE                                    ₹2,450.50 │ │
│  │   Reliance Industries Ltd • NSE              +10.50    │ │
│  │   💹 Vol: 12.5M  📊 Avg: ₹2,440  🎯 52W: ₹2,856       │ │
│  ╰─────────────────────────────────────────────────────────╯ │
│                                                             │
│  📊 Order Details                                           │
│  ┌─ Order Type ──┐  ┌─ Side ──────┐  ┌─ Quantity ─────────┐ │
│  │ [●] Market    │  │ [●] Buy      │  │        100         │ │
│  │ [ ] Limit     │  │ [ ] Sell     │  │                   │ │
│  │ [ ] SL        │  │              │  │ Lot Size: 1       │ │
│  └───────────────┘  └──────────────┘  └───────────────────┘ │
│                                                             │
│  🏦 Smart Routing (AI Optimized)                            │
│  ╭─────────────────────────────────────────────────────────╮ │
│  │ Optimal Execution Plan:                                 │ │
│  │                                                         │ │
│  │ 🥇 Zerodha    60 shares  Est: ₹1,47,030  ⚡ Fast       │ │
│  │    • Low brokerage • High liquidity                    │ │
│  │                                                         │ │
│  │ 🥈 Groww      30 shares  Est: ₹73,515   ⚡ Fast        │ │
│  │    • Best price available                              │ │
│  │                                                         │ │
│  │ 🥉 Angel One  10 shares  Est: ₹24,505   ⚡ Medium      │ │
│  │    • Backup allocation                                 │ │
│  │                                                         │ │
│  │ 💰 Total Cost: ₹2,45,050  💸 Savings: ₹150            │ │
│  │ ⚡ Est. Execution: 3.5 seconds                          │ │
│  ╰─────────────────────────────────────────────────────────╯ │
│                                                             │
│  ⚙️ Advanced Options                                        │
│  ┌─ Validity ──┐  ┌─ Special ──────┐  ┌─ Risk ──────────┐  │
│  │ [●] Day     │  │ [ ] After Mkt   │  │ Stop Loss: --   │  │
│  │ [ ] IOC     │  │ [ ] Pre Market  │  │ Target: --      │  │
│  │ [ ] GTC     │  │ [ ] Cover Order │  │ Max Risk: 2%    │  │
│  └─────────────┘  └─────────────────┘  └─────────────────┘  │
│                                                             │
│  ╭─────────────────────────────────────────────────────────╮ │
│  │           🚀 PLACE ORDER - ₹2,45,050                   │ │
│  │                                                         │ │
│  │    [📱 Review & Confirm]  [⚡ Quick Execute]           │ │
│  ╰─────────────────────────────────────────────────────────╯ │
╰─────────────────────────────────────────────────────────────╯
```

**Component Features:**
- Real-time symbol search with auto-complete
- AI-powered broker routing visualization
- Interactive order type selection
- Estimated execution time and cost
- Risk management controls

### 3. Real-Time Portfolio Analytics

```
╭─────────────────────────────────────────────────────────────╮
│ 📊 Portfolio Analytics              📅 1D 1W [1M] 3M 1Y    │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  💰 Performance Overview                                     │
│  ╭─────────────────────────╮  ╭─────────────────────────────╮ │
│  │ Portfolio Value         │  │      📈 P&L Chart          │ │
│  │                         │  │   ╭─╮                      │ │
│  │ ₹12,45,000              │  │   │ │╭─╮              ╭─╮  │ │
│  │ ↗ +₹2,45,000 (24.5%)   │  │ ╭─╯ ╰╯ ╰─╮          ╭─╯ ╰╮ │ │
│  │                         │  │ │        ╰─╮      ╭─╯    │ │ │
│  │ Today: +₹15,240         │  │ │          ╰──────╯      ╰ │ │
│  │ This Month: +₹45,600    │  │ ╰──────────────────────────╯ │ │
│  ╰─────────────────────────╯  ╰─────────────────────────────╯ │
│                                                             │
│  🎯 Asset Allocation                                        │
│  ╭─────────────────────────────────────────────────────────╮ │
│  │ 🔵 Large Cap     65%  ██████████████▓▓▓                │ │
│  │ 🟡 Mid Cap       20%  ████▓▓▓▓▓▓▓▓▓▓▓▓▓                │ │
│  │ 🟠 Small Cap     10%  ██▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓                │ │
│  │ 🟢 Cash           5%  █▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓                │ │
│  ╰─────────────────────────────────────────────────────────╯ │
│                                                             │
│  🏦 Broker-wise Breakdown                                   │
│  ╭─────────────────────────────────────────────────────────╮ │
│  │ Zerodha   ₹4,20,000  33.7%  ██████████████▓▓▓▓▓▓       │ │
│  │ Groww     ₹3,80,000  30.5%  ████████████▓▓▓▓▓▓▓▓       │ │
│  │ Angel One ₹2,50,000  20.1%  ████████▓▓▓▓▓▓▓▓▓▓▓▓       │ │
│  │ ICICI Dir ₹1,80,000  14.5%  ██████▓▓▓▓▓▓▓▓▓▓▓▓▓▓       │ │
│  │ Upstox    ₹15,000    1.2%   █▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓       │ │
│  ╰─────────────────────────────────────────────────────────╯ │
│                                                             │
│  📈 Risk Metrics                    🎯 AI Recommendations  │
│  ╭─────────────────────────╮        ╭─────────────────────╮ │
│  │ Portfolio Beta: 1.2     │        │ 🤖 Rebalance Alert │ │
│  │ Max Drawdown: -8.5%     │        │ Large cap exposure  │ │
│  │ Sharpe Ratio: 1.8       │        │ is high. Consider   │ │
│  │ VaR (95%): ₹45,000      │        │ adding mid-caps.    │ │
│  │ Risk Score: 6/10 📊     │        │                     │ │
│  ╰─────────────────────────╯        │ 📊 Sector Analysis  │ │
│                                     │ 🏦 Banking: 35%     │ │
│                                     │ 💻 IT: 25%          │ │
│                                     │ ⚡ Auto: 15%        │ │
│                                     ╰─────────────────────╯ │
╰─────────────────────────────────────────────────────────────╯
```

**Interactive Elements:**
- Live updating P&L charts
- Clickable asset allocation segments
- Hover tooltips for detailed metrics
- Expandable broker sections
- AI recommendation cards

### 4. Mobile Trading Interface (PWA)

```
╭─────────────────────╮
│ 🏠 TradeMaster      │
│    ₹12.45L  +2.4%   │
├─────────────────────┤
│                     │
│ 📊 Quick Stats      │
│ ┌─────────────────┐ │
│ │ Day P&L: +15.2K │ │
│ │ 📈 78% Accuracy │ │
│ └─────────────────┘ │
│                     │
│ 🏦 Brokers          │
│ ┌─────────────────┐ │
│ │ ✅ Zerodha  4.2L│ │
│ │ ✅ Groww    3.8L│ │
│ │ ✅ Angel    2.5L│ │
│ │ ⚠️  ICICI   1.8L│ │
│ └─────────────────┘ │
│                     │
│ 🎯 Quick Actions    │
│ ┌─────────────────┐ │
│ │    💰  📈       │ │
│ │   BUY  SELL     │ │
│ │                 │ │
│ │    🔍  📊       │ │
│ │  SEARCH CHART   │ │
│ └─────────────────┘ │
│                     │
│ 📊 Holdings         │
│ RELIANCE    +2.3%   │
│ TCS         -1.2%   │
│ INFY        +0.8%   │
│ HDFC        +3.1%   │
│                     │
│ ≡ ═══ 🏠 ═══ 👤     │
╰─────────────────────╯
```

**Gesture Controls:**
- Swipe right: Quick buy
- Swipe left: Quick sell
- Long press: Detailed view
- Pull to refresh: Update data
- Pinch zoom: Chart navigation

### 5. Broker Connection Management

```
╭─────────────────────────────────────────────────────────────╮
│ 🏦 Broker Connections                    ⚙️ Settings  📊    │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  📊 Connection Overview                                      │
│  ╭─────────────────────────────────────────────────────────╮ │
│  │ Active Brokers: 4/6    Total Value: ₹12,45,000         │ │
│  │ Sync Status: 🟢 Live    Last Update: 2 mins ago        │ │
│  ╰─────────────────────────────────────────────────────────╯ │
│                                                             │
│  🔗 Connected Brokers                                       │
│                                                             │
│  ┌─ Zerodha ──────────────────────────────────────────────┐ │
│  │ 🟢 Connected    Portfolio: ₹4,20,000    API: Active    │ │
│  │ 📊 Orders: 145   Holdings: 12   Last Sync: 30s ago    │ │
│  │ [⚙️ Settings] [🔄 Refresh] [📊 Details] [❌ Disconnect] │ │
│  └─────────────────────────────────────────────────────────┘ │
│                                                             │
│  ┌─ Groww ────────────────────────────────────────────────┐ │
│  │ 🟢 Connected    Portfolio: ₹3,80,000    API: Active    │ │
│  │ 📊 Orders: 89    Holdings: 8    Last Sync: 1m ago     │ │
│  │ [⚙️ Settings] [🔄 Refresh] [📊 Details] [❌ Disconnect] │ │
│  └─────────────────────────────────────────────────────────┘ │
│                                                             │
│  ┌─ Angel One ────────────────────────────────────────────┐ │
│  │ 🟡 Limited      Portfolio: ₹2,50,000    API: Rate Limit│ │
│  │ 📊 Orders: 67    Holdings: 6    Last Sync: 5m ago     │ │
│  │ ⚠️ API limit reached. Upgrade to premium access        │ │
│  │ [⬆️ Upgrade] [⚙️ Settings] [📊 Details] [❌ Disconnect]  │ │
│  └─────────────────────────────────────────────────────────┘ │
│                                                             │
│  ┌─ ICICI Direct ─────────────────────────────────────────┐ │
│  │ 🔴 Error        Portfolio: ₹1,80,000    API: Failed    │ │
│  │ ❌ Authentication failed. Token expired.               │ │
│  │ [🔄 Reconnect] [⚙️ Settings] [📞 Support] [❌ Remove]   │ │
│  └─────────────────────────────────────────────────────────┘ │
│                                                             │
│  🔌 Add New Broker                                          │
│  ╭─────────────────────────────────────────────────────────╮ │
│  │ Available Brokers:                                      │ │
│  │                                                         │ │
│  │ [+ Upstox]    [+ 5paisa]    [+ Fyers]    [+ Alice Blue]│ │
│  │ [+ Kotak]     [+ HDFC Sec]  [+ Motilal]  [+ Paytm]     │ │
│  ╰─────────────────────────────────────────────────────────╯ │
│                                                             │
│  🛡️ Security & Permissions                                  │
│  ╭─────────────────────────────────────────────────────────╮ │
│  │ 🔐 All tokens encrypted with AES-256                   │ │
│  │ 🔄 Auto-refresh enabled for all brokers                │ │
│  │ ⏰ Token expiry monitoring: Active                      │ │
│  │ 📱 2FA required for sensitive operations               │ │
│  ╰─────────────────────────────────────────────────────────╯ │
╰─────────────────────────────────────────────────────────────╯
```

**Status Indicators:**
- Green: Fully functional
- Yellow: Limited functionality
- Red: Connection issues
- Real-time status updates

---

## Component Library Extensions

### Trading-Specific Components

#### 1. BrokerStatusCard Component
```typescript
interface BrokerStatusCardProps {
  brokerId: string
  status: 'connected' | 'limited' | 'error' | 'disconnected'
  portfolioValue: number
  holdings: number
  orders: number
  lastSync: Date
  onRefresh: () => void
  onSettings: () => void
  onDisconnect: () => void
}
```

#### 2. SmartOrderRoute Component  
```typescript
interface OrderRouteProps {
  symbol: string
  quantity: number
  orderType: 'MARKET' | 'LIMIT' | 'SL'
  routes: {
    brokerId: string
    allocation: number
    estimatedCost: number
    executionSpeed: 'fast' | 'medium' | 'slow'
    reasoning: string[]
  }[]
}
```

#### 3. PortfolioMetrics Component
```typescript
interface PortfolioMetricsProps {
  totalValue: number
  dayPnL: number
  totalPnL: number
  riskMetrics: {
    beta: number
    sharpeRatio: number
    maxDrawdown: number
    valueAtRisk: number
  }
  allocation: {
    category: string
    percentage: number
    value: number
  }[]
}
```

### Design System Extensions

#### CSS Classes for Trading UI
```css
/* Trading-specific utility classes */
.pnl-positive { @apply text-green-400 font-semibold; }
.pnl-negative { @apply text-red-400 font-semibold; }
.broker-status-active { @apply border-green-500/50 bg-green-500/10; }
.broker-status-error { @apply border-red-500/50 bg-red-500/10; }
.order-route-card { @apply glass-card-dark p-4 rounded-xl border-purple-500/30; }
.metric-card { @apply glass-card p-6 rounded-2xl hover:scale-105 transition-transform; }
.trading-button { @apply cyber-button-sm px-6 py-3 text-sm font-medium; }
```

---

## Responsive Design Specifications

### Mobile Breakpoints
- **Mobile**: 320px - 767px (Primary focus)
- **Tablet**: 768px - 1023px
- **Desktop**: 1024px+ 

### Touch-Friendly Design
- **Minimum tap target**: 44px × 44px
- **Gesture support**: Swipe, long-press, pinch-zoom
- **Thumb-zone optimization**: Bottom navigation, quick actions
- **One-handed usage**: Easy reach to primary functions

### PWA Features
- **Offline capability**: Cached portfolio data viewing
- **Push notifications**: Price alerts, order status
- **App-like experience**: Full-screen mode, splash screen
- **Background sync**: Queue orders when offline

---

## Accessibility Compliance

### WCAG 2.1 AA Standards
- **Color contrast**: 4.5:1 minimum ratio
- **Keyboard navigation**: Tab order, focus indicators
- **Screen reader**: ARIA labels, semantic HTML
- **Motion reduction**: Respect user preferences

### Financial Data Accessibility
- **Currency formatting**: Proper locale support
- **Percentage changes**: Clear positive/negative indicators
- **Data tables**: Column headers, row labels
- **Charts**: Alt text descriptions, data tables fallback

---

## Implementation Priority

### Phase 1: Core Dashboard (Weeks 1-2)
- Multi-broker portfolio overview
- Basic broker status display  
- Quick action buttons
- Responsive layout foundation

### Phase 2: Trading Interface (Weeks 3-4)
- Order placement form
- Smart routing visualization
- Real-time order status
- Mobile gesture controls

### Phase 3: Analytics & Portfolio (Weeks 5-6)
- Portfolio performance charts
- Risk metrics dashboard
- Asset allocation visualization
- AI recommendations display

### Phase 4: Polish & PWA (Weeks 7-8)
- Broker connection management
- Advanced settings
- PWA implementation
- Accessibility improvements

---

**✅ Wireframes Complete**

These wireframes maintain TradeMaster's existing design aesthetic while introducing the multi-broker orchestration features specified in the PRD. The designs prioritize mobile-first usage with sophisticated desktop capabilities, leveraging the existing glassmorphism and cyber-themed component library.