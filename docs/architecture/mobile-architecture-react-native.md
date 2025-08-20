# Mobile Architecture (React Native)

## Application Structure

```
trademaster-mobile/
├── src/
│   ├── components/          # Reusable UI components
│   ├── screens/            # Screen components
│   ├── navigation/         # Navigation configuration
│   ├── services/           # API and business logic
│   ├── store/             # Redux state management
│   ├── utils/             # Utility functions
│   └── types/             # TypeScript type definitions
├── android/               # Android-specific code
├── ios/                   # iOS-specific code
└── assets/               # Images, fonts, etc.
```

## Key Components

**Trading Interface:**
```typescript
interface TradingScreenProps {
  symbol: string;
  marketData: MarketData;
  behavioralAlert?: BehavioralAlert;
}

const TradingScreen: React.FC<TradingScreenProps> = ({
  symbol,
  marketData,
  behavioralAlert
}) => {
  // One-thumb trading interface
  // Real-time price updates
  // Behavioral intervention overlays
  // Swipe gestures for quick actions
};
```

**Real-time Data Management:**
```typescript
class WebSocketService {
  private socket: WebSocket;
  private subscriptions: Map<string, Subscription>;
  
  connect(): void {
    // WebSocket connection with reconnection logic
    // Subscribe to market data streams
    // Handle behavioral alerts and notifications
  }
  
  subscribeToSymbol(symbol: string): void {
    // Real-time price updates
    // Volume and institutional activity alerts
  }
}
```

## Performance Optimizations

**React Native Optimizations:**
- **Lazy Loading:** Screen-based code splitting
- **Memoization:** React.memo for expensive components
- **Virtual Lists:** FlatList for large data sets
- **Image Optimization:** WebP format with caching
- **Bundle Splitting:** Separate bundles for core and premium features

**Offline Capabilities:**
- Portfolio data caching with Redux Persist
- Offline queue for pending trades
- Sync mechanism when connection restored
