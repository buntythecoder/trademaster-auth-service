# Story FE-006: Integration Testing & Performance Optimization

## Epic
Epic 2: Market Data & Trading Foundation

## Story Overview
**As a** TradeMaster development team  
**I want** comprehensive integration testing and performance monitoring  
**So that** the frontend delivers consistent, fast, and reliable user experience

## Business Value
- **Quality Assurance**: 99.5% uptime and reliability for trading operations
- **Performance Guarantee**: Sub-3-second load times ensure user retention
- **Risk Mitigation**: Comprehensive testing reduces production issues by 80%
- **Competitive Advantage**: Superior performance vs competitor platforms

## Technical Requirements

### Integration Testing Framework
```typescript
// End-to-End Testing Setup
import { test, expect, Page } from '@playwright/test'
import { TradingTestFixtures } from '../fixtures/trading-fixtures'
import { MockWebSocketServer } from '../mocks/websocket-server'

class TradingE2ETests {
  private page: Page
  private mockWsServer: MockWebSocketServer
  private fixtures: TradingTestFixtures
  
  constructor(page: Page) {
    this.page = page
    this.mockWsServer = new MockWebSocketServer()
    this.fixtures = new TradingTestFixtures()
  }
  
  async setup(): Promise<void> {
    // Start mock WebSocket server
    await this.mockWsServer.start()
    
    // Login user
    await this.fixtures.loginUser('test@trademaster.com', 'password123')
    
    // Navigate to trading dashboard
    await this.page.goto('/dashboard')
    await this.page.waitForLoadState('networkidle')
  }
  
  async teardown(): Promise<void> {
    await this.mockWsServer.stop()
    await this.fixtures.cleanup()
  }
}

// Critical User Journey Tests
describe('Trading Platform Integration', () => {
  let tradingTests: TradingE2ETests
  
  beforeEach(async ({ page }) => {
    tradingTests = new TradingE2ETests(page)
    await tradingTests.setup()
  })
  
  afterEach(async () => {
    await tradingTests.teardown()
  })
  
  test('Complete trading workflow - Market order', async ({ page }) => {
    // Step 1: Verify dashboard loads with real-time data
    await expect(page.locator('[data-testid="market-data-table"]')).toBeVisible()
    await expect(page.locator('[data-testid="connection-status"]')).toHaveText('Connected')
    
    // Step 2: Search and select a stock
    await page.fill('[data-testid="stock-search"]', 'RELIANCE')
    await page.click('[data-testid="stock-RELIANCE"]')
    
    // Step 3: Verify stock details load
    await expect(page.locator('[data-testid="stock-details"]')).toBeVisible()
    await expect(page.locator('[data-testid="current-price"]')).toContainText('₹')
    
    // Step 4: Open buy order form
    await page.click('[data-testid="buy-button"]')
    await expect(page.locator('[data-testid="order-form"]')).toBeVisible()
    
    // Step 5: Fill order details
    await page.fill('[data-testid="quantity-input"]', '10')
    await page.click('[data-testid="market-order"]')
    
    // Step 6: Verify order preview
    const orderPreview = page.locator('[data-testid="order-preview"]')
    await expect(orderPreview).toContainText('10 shares')
    await expect(orderPreview).toContainText('Market Order')
    
    // Step 7: Place order
    await page.click('[data-testid="place-order-button"]')
    
    // Step 8: Verify order confirmation
    await expect(page.locator('[data-testid="order-success"]')).toBeVisible()
    await expect(page.locator('[data-testid="order-id"]')).toBeVisible()
    
    // Step 9: Verify order appears in orders list
    await page.click('[data-testid="orders-tab"]')
    await expect(page.locator('[data-testid="order-list"]')).toContainText('RELIANCE')
    await expect(page.locator('[data-testid="order-list"]')).toContainText('10 shares')
    
    // Step 10: Verify portfolio update
    await page.click('[data-testid="portfolio-tab"]')
    await expect(page.locator('[data-testid="portfolio-value"]')).toBeVisible()
  })
  
  test('Real-time data streaming integration', async ({ page }) => {
    // Mock WebSocket price updates
    const priceUpdates = [
      { symbol: 'NIFTY', price: 19500.50, change: 125.30 },
      { symbol: 'NIFTY', price: 19525.75, change: 150.55 },
      { symbol: 'NIFTY', price: 19510.25, change: 135.05 }
    ]
    
    // Send price updates via mock WebSocket
    for (const update of priceUpdates) {
      await tradingTests.mockWsServer.sendPriceUpdate(update)
      
      // Verify UI updates
      await expect(page.locator(`[data-testid="price-${update.symbol}"]`))
        .toHaveText(`₹${update.price.toFixed(2)}`)
      
      await expect(page.locator(`[data-testid="change-${update.symbol}"]`))
        .toHaveText(`+₹${update.change.toFixed(2)}`)
      
      // Wait for animation to complete
      await page.waitForTimeout(100)
    }
  })
  
  test('Error handling and recovery', async ({ page }) => {
    // Test network disconnection
    await page.route('**/*', route => route.abort())
    
    // Verify error state
    await expect(page.locator('[data-testid="error-banner"]')).toBeVisible()
    await expect(page.locator('[data-testid="error-message"]'))
      .toContainText('Connection lost')
    
    // Restore network
    await page.unroute('**/*')
    
    // Verify recovery
    await page.click('[data-testid="retry-button"]')
    await expect(page.locator('[data-testid="connection-status"]'))
      .toHaveText('Connected')
  })
})
```

### Performance Testing Suite
```typescript
// Performance Test Utilities
class PerformanceTestRunner {
  private page: Page
  private metrics: PerformanceMetrics = {}
  
  constructor(page: Page) {
    this.page = page
  }
  
  async measurePageLoad(url: string): Promise<PageLoadMetrics> {
    const startTime = performance.now()
    
    // Navigate and measure load events
    await this.page.goto(url)
    
    const domContentLoaded = await this.page.evaluate(() => 
      performance.getEntriesByType('navigation')[0].domContentLoadedEventEnd -
      performance.getEntriesByType('navigation')[0].navigationStart
    )
    
    const fullyLoaded = await this.page.evaluate(() =>
      performance.getEntriesByType('navigation')[0].loadEventEnd -
      performance.getEntriesByType('navigation')[0].navigationStart
    )
    
    // Measure Web Vitals
    const webVitals = await this.measureWebVitals()
    
    return {
      domContentLoaded,
      fullyLoaded,
      ...webVitals
    }
  }
  
  async measureWebVitals(): Promise<WebVitalMetrics> {
    const vitals = await this.page.evaluate(async () => {
      const { getCLS, getFID, getFCP, getLCP, getTTFB } = await import('web-vitals')
      
      return new Promise((resolve) => {
        const metrics: any = {}
        
        getCLS((metric) => metrics.cls = metric.value)
        getFID((metric) => metrics.fid = metric.value)
        getFCP((metric) => metrics.fcp = metric.value)
        getLCP((metric) => metrics.lcp = metric.value)
        getTTFB((metric) => metrics.ttfb = metric.value)
        
        // Wait for all metrics to be collected
        setTimeout(() => resolve(metrics), 3000)
      })
    })
    
    return vitals
  }
  
  async measureRealTimeDataPerformance(): Promise<RealTimeMetrics> {
    const startTime = performance.now()
    let updateCount = 0
    let totalLatency = 0
    
    // Listen for price updates
    await this.page.exposeFunction('onPriceUpdate', (timestamp: number) => {
      const latency = performance.now() - timestamp
      totalLatency += latency
      updateCount++
    })
    
    // Inject performance tracking
    await this.page.addInitScript(() => {
      // Override WebSocket message handler to measure latency
      const originalWebSocket = window.WebSocket
      window.WebSocket = class extends originalWebSocket {
        constructor(url: string, protocols?: string | string[]) {
          super(url, protocols)
          
          this.addEventListener('message', (event) => {
            const data = JSON.parse(event.data)
            if (data.type === 'price_update') {
              window.onPriceUpdate(data.timestamp)
            }
          })
        }
      }
    })
    
    // Wait for data streaming
    await this.page.waitForTimeout(10000) // 10 seconds
    
    const averageLatency = updateCount > 0 ? totalLatency / updateCount : 0
    
    return {
      updateCount,
      averageLatency,
      updatesPerSecond: updateCount / 10
    }
  }
}

// Performance Test Suite
describe('Performance Testing', () => {
  let performanceRunner: PerformanceTestRunner
  
  beforeEach(async ({ page }) => {
    performanceRunner = new PerformanceTestRunner(page)
  })
  
  test('Dashboard load performance', async () => {
    const metrics = await performanceRunner.measurePageLoad('/dashboard')
    
    // Performance assertions
    expect(metrics.domContentLoaded).toBeLessThan(2000) // 2 seconds
    expect(metrics.fullyLoaded).toBeLessThan(3000)      // 3 seconds
    expect(metrics.lcp).toBeLessThan(2500)              // LCP < 2.5s
    expect(metrics.fcp).toBeLessThan(1800)              // FCP < 1.8s
    expect(metrics.cls).toBeLessThan(0.1)               // CLS < 0.1
    expect(metrics.fid).toBeLessThan(100)               // FID < 100ms
  })
  
  test('Real-time data streaming performance', async ({ page }) => {
    await page.goto('/dashboard')
    
    const metrics = await performanceRunner.measureRealTimeDataPerformance()
    
    // Real-time performance assertions
    expect(metrics.averageLatency).toBeLessThan(100)    // < 100ms latency
    expect(metrics.updatesPerSecond).toBeGreaterThan(5) // > 5 updates/sec
    expect(metrics.updateCount).toBeGreaterThan(50)     // 50+ updates in 10s
  })
  
  test('Memory usage during extended trading session', async ({ page }) => {
    await page.goto('/dashboard')
    
    // Measure initial memory
    const initialMemory = await page.evaluate(() => {
      return (performance as any).memory?.usedJSHeapSize || 0
    })
    
    // Simulate extended trading session (30 minutes of data)
    await page.waitForTimeout(30000) // 30 seconds simulation
    
    // Measure final memory
    const finalMemory = await page.evaluate(() => {
      return (performance as any).memory?.usedJSHeapSize || 0
    })
    
    const memoryIncrease = finalMemory - initialMemory
    const memoryIncreaseMB = memoryIncrease / (1024 * 1024)
    
    // Memory leak assertion
    expect(memoryIncreaseMB).toBeLessThan(50) // < 50MB increase
  })
})
```

### Automated Visual Testing
```typescript
// Visual Regression Testing
describe('Visual Regression Tests', () => {
  test('Trading dashboard visual comparison', async ({ page }) => {
    await page.goto('/dashboard')
    await page.waitForLoadState('networkidle')
    
    // Hide dynamic content for consistent screenshots
    await page.addStyleTag({
      content: `
        [data-testid="current-time"],
        [data-testid="live-prices"] {
          visibility: hidden !important;
        }
      `
    })
    
    // Take full page screenshot
    await expect(page).toHaveScreenshot('trading-dashboard-full.png', {
      fullPage: true,
      threshold: 0.2
    })
  })
  
  test('Mobile responsive design', async ({ page }) => {
    // Test different viewport sizes
    const viewports = [
      { width: 375, height: 667, name: 'mobile-portrait' },
      { width: 667, height: 375, name: 'mobile-landscape' },
      { width: 768, height: 1024, name: 'tablet-portrait' },
      { width: 1024, height: 768, name: 'tablet-landscape' }
    ]
    
    for (const viewport of viewports) {
      await page.setViewportSize({ width: viewport.width, height: viewport.height })
      await page.goto('/dashboard')
      await page.waitForLoadState('networkidle')
      
      await expect(page).toHaveScreenshot(`dashboard-${viewport.name}.png`, {
        threshold: 0.2
      })
    }
  })
})
```

### Continuous Performance Monitoring
```typescript
// Performance Monitoring Service
class PerformanceMonitoringService {
  private metricsCollection: PerformanceMetric[] = []
  private thresholds: PerformanceThresholds
  
  constructor(thresholds: PerformanceThresholds) {
    this.thresholds = thresholds
    this.initializeMonitoring()
  }
  
  private initializeMonitoring(): void {
    // Real User Monitoring (RUM)
    if (typeof window !== 'undefined') {
      // Web Vitals monitoring
      import('web-vitals').then(({ getCLS, getFID, getFCP, getLCP, getTTFB }) => {
        getCLS(this.recordMetric.bind(this))
        getFID(this.recordMetric.bind(this))
        getFCP(this.recordMetric.bind(this))
        getLCP(this.recordMetric.bind(this))
        getTTFB(this.recordMetric.bind(this))
      })
      
      // Custom performance monitoring
      this.monitorResourceTiming()
      this.monitorUserInteractions()
      this.monitorWebSocketPerformance()
    }
  }
  
  private recordMetric(metric: any): void {
    const performanceMetric: PerformanceMetric = {
      name: metric.name,
      value: metric.value,
      timestamp: Date.now(),
      userId: getCurrentUserId(),
      sessionId: getSessionId(),
      userAgent: navigator.userAgent,
      connectionType: getConnectionType()
    }
    
    this.metricsCollection.push(performanceMetric)
    
    // Check thresholds
    this.checkThreshold(performanceMetric)
    
    // Send to analytics
    this.sendToAnalytics(performanceMetric)
  }
  
  private checkThreshold(metric: PerformanceMetric): void {
    const threshold = this.thresholds[metric.name]
    if (!threshold) return
    
    if (metric.value > threshold.warning) {
      console.warn(`Performance warning: ${metric.name} = ${metric.value}ms`)
    }
    
    if (metric.value > threshold.critical) {
      console.error(`Performance critical: ${metric.name} = ${metric.value}ms`)
      
      // Send alert
      this.sendAlert({
        level: 'critical',
        metric: metric.name,
        value: metric.value,
        threshold: threshold.critical
      })
    }
  }
  
  public generatePerformanceReport(): PerformanceReport {
    const now = Date.now()
    const last24Hours = this.metricsCollection.filter(
      m => now - m.timestamp < 24 * 60 * 60 * 1000
    )
    
    const report: PerformanceReport = {
      timestamp: now,
      totalMetrics: last24Hours.length,
      metrics: {}
    }
    
    // Group by metric name
    const grouped = groupBy(last24Hours, 'name')
    
    Object.entries(grouped).forEach(([name, metrics]) => {
      const values = metrics.map(m => m.value)
      report.metrics[name] = {
        count: values.length,
        average: values.reduce((a, b) => a + b, 0) / values.length,
        median: calculateMedian(values),
        p95: calculatePercentile(values, 95),
        p99: calculatePercentile(values, 99),
        min: Math.min(...values),
        max: Math.max(...values)
      }
    })
    
    return report
  }
}

// Integration with monitoring dashboard
const performanceService = new PerformanceMonitoringService({
  LCP: { warning: 2000, critical: 4000 },
  FCP: { warning: 1500, critical: 3000 },
  CLS: { warning: 0.1, critical: 0.25 },
  FID: { warning: 100, critical: 300 },
  TTFB: { warning: 600, critical: 1200 }
})
```

## Acceptance Criteria

### Integration Testing
- [ ] **End-to-End Coverage**: Complete user journeys tested (login → trade → portfolio)
- [ ] **API Integration**: All backend API endpoints tested with frontend
- [ ] **WebSocket Integration**: Real-time data streaming validated
- [ ] **Error Scenarios**: Network failures and recovery paths tested

### Performance Standards
- [ ] **Load Time**: Dashboard loads in <3 seconds on 3G networks
- [ ] **Web Vitals**: LCP <2.5s, FCP <1.8s, CLS <0.1, FID <100ms
- [ ] **Real-time Latency**: Market data updates in <100ms
- [ ] **Memory Usage**: <100MB for extended trading sessions

### Visual Testing
- [ ] **Cross-Browser**: Consistent appearance in Chrome, Firefox, Safari, Edge
- [ ] **Responsive Design**: Proper layouts across all device sizes
- [ ] **Visual Regression**: Automated detection of UI changes
- [ ] **Accessibility**: WCAG 2.1 AA compliance verified

### Monitoring & Alerting
- [ ] **Real User Monitoring**: Performance data collected from all users
- [ ] **Alerting System**: Automatic alerts for performance degradation
- [ ] **Performance Dashboard**: Real-time monitoring of key metrics
- [ ] **Trend Analysis**: Historical performance trend tracking

## Testing Strategy

### Automated Testing Pipeline
- Unit tests (Jest) - 90% code coverage
- Integration tests (Playwright) - Critical user paths
- Visual regression tests - UI consistency
- Performance tests - Load time and resource usage
- Accessibility tests - WCAG compliance

### Continuous Monitoring
- Real User Monitoring (RUM) in production
- Synthetic monitoring with scheduled tests
- Performance budgets with CI/CD enforcement
- Error tracking and alerting system

### Performance Optimization
- Bundle analysis and code splitting
- Image optimization and lazy loading
- CDN configuration for static assets
- Service worker for caching strategy

### Quality Gates
- Performance budgets in CI/CD pipeline
- Automated lighthouse audits
- Bundle size limits enforcement
- Core Web Vitals threshold validation

## Definition of Done
- [ ] Comprehensive E2E test suite covering all critical user journeys
- [ ] Performance testing pipeline integrated with CI/CD
- [ ] Visual regression testing preventing UI inconsistencies
- [ ] Real User Monitoring deployed and collecting metrics
- [ ] Performance dashboard providing real-time insights
- [ ] Automated alerting for performance degradation
- [ ] Cross-browser testing covering 95% of user base
- [ ] Mobile performance optimization validated on real devices
- [ ] Accessibility audit passed with WCAG 2.1 AA compliance
- [ ] Documentation for testing procedures and performance standards

## Story Points: 21

## Dependencies
- Backend API stability for integration testing
- WebSocket server for real-time data testing
- Monitoring infrastructure for metrics collection
- CI/CD pipeline for automated testing integration

## Notes
- Integration with existing analytics platforms (Google Analytics, Mixpanel)
- Performance budgets should be enforced in development workflow
- Consider implementing performance monitoring for competitor analysis
- Regular performance audit schedule (monthly) for continuous optimization