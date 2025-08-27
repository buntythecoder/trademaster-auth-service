# Story MOB-004: Mobile Performance Optimization

## Epic
Epic 4: Mobile-First Design & PWA

## Story Overview
**As a** TradeMaster mobile user  
**I want** lightning-fast performance on my mobile device  
**So that** I can trade efficiently without delays, even on slower devices and networks

## Business Value
- **User Retention**: 85% of users abandon apps that take >3 seconds to load
- **Market Advantage**: Sub-second interactions enable faster trading decisions
- **Accessibility**: Optimized performance for budget devices expands market reach
- **Battery Efficiency**: Optimized code reduces battery drain during trading sessions

## Technical Requirements

### Performance Monitoring & Metrics
```typescript
// Performance Monitoring Service
class MobilePerformanceMonitor {
  private metrics: PerformanceMetric[] = []
  private observer: PerformanceObserver | null = null
  private vitalsReported = false
  
  constructor() {
    this.initializeObserver()
    this.setupWebVitals()
    this.monitorMemoryUsage()
    this.trackNetworkSpeed()
  }
  
  private initializeObserver(): void {
    if ('PerformanceObserver' in window) {
      this.observer = new PerformanceObserver((list) => {
        for (const entry of list.getEntries()) {
          this.recordMetric({
            name: entry.name,
            type: entry.entryType,
            startTime: entry.startTime,
            duration: entry.duration,
            timestamp: Date.now()
          })
        }
      })
      
      // Observe different performance entry types
      this.observer.observe({ 
        entryTypes: ['navigation', 'resource', 'measure', 'paint'] 
      })
    }
  }
  
  private async setupWebVitals(): Promise<void> {
    try {
      const { getCLS, getFID, getFCP, getLCP, getTTFB } = await import('web-vitals')
      
      // Core Web Vitals
      getCLS((metric) => this.reportWebVital(metric))
      getFID((metric) => this.reportWebVital(metric))
      getFCP((metric) => this.reportWebVital(metric))
      getLCP((metric) => this.reportWebVital(metric))
      getTTFB((metric) => this.reportWebVital(metric))
      
      this.vitalsReported = true
    } catch (error) {
      console.error('Failed to load web-vitals:', error)
    }
  }
  
  private reportWebVital(metric: any): void {
    const performanceMetric: PerformanceMetric = {
      name: metric.name,
      type: 'web-vital',
      value: metric.value,
      rating: metric.rating,
      delta: metric.delta,
      timestamp: Date.now(),
      id: metric.id
    }
    
    this.recordMetric(performanceMetric)
    
    // Send critical metrics immediately
    if (metric.name === 'LCP' || metric.name === 'FID') {
      this.sendMetricToAnalytics(performanceMetric)
    }
  }
  
  private monitorMemoryUsage(): void {
    if ('memory' in performance) {
      setInterval(() => {
        const memory = (performance as any).memory
        this.recordMetric({
          name: 'memory-usage',
          type: 'memory',
          value: memory.usedJSHeapSize,
          limit: memory.jsHeapSizeLimit,
          timestamp: Date.now()
        })
      }, 30000) // Every 30 seconds
    }
  }
  
  private trackNetworkSpeed(): void {
    if ('connection' in navigator) {
      const connection = (navigator as any).connection
      
      this.recordMetric({
        name: 'network-info',
        type: 'network',
        effectiveType: connection.effectiveType,
        downlink: connection.downlink,
        rtt: connection.rtt,
        saveData: connection.saveData,
        timestamp: Date.now()
      })
      
      // Listen for changes
      connection.addEventListener('change', () => {
        this.recordMetric({
          name: 'network-change',
          type: 'network',
          effectiveType: connection.effectiveType,
          downlink: connection.downlink,
          rtt: connection.rtt,
          timestamp: Date.now()
        })
      })
    }
  }
  
  public measureInteraction(name: string): PerformanceMeasurement {
    const startTime = performance.now()
    const startMark = `${name}-start`
    
    performance.mark(startMark)
    
    return {
      end: () => {
        const endTime = performance.now()
        const endMark = `${name}-end`
        performance.mark(endMark)
        performance.measure(name, startMark, endMark)
        
        const duration = endTime - startTime
        this.recordMetric({
          name,
          type: 'interaction',
          duration,
          timestamp: Date.now()
        })
        
        return duration
      }
    }
  }
  
  public getPerformanceReport(): PerformanceReport {
    const now = Date.now()
    const recentMetrics = this.metrics.filter(
      m => now - m.timestamp < 300000 // Last 5 minutes
    )
    
    return {
      timestamp: now,
      deviceInfo: this.getDeviceInfo(),
      networkInfo: this.getLatestNetworkInfo(),
      memoryInfo: this.getLatestMemoryInfo(),
      webVitals: this.getWebVitalsScore(),
      interactionMetrics: this.getInteractionMetrics(recentMetrics),
      recommendations: this.generateRecommendations()
    }
  }
  
  private recordMetric(metric: PerformanceMetric): void {
    this.metrics.push(metric)
    
    // Keep only last 1000 metrics
    if (this.metrics.length > 1000) {
      this.metrics = this.metrics.slice(-1000)
    }
    
    // Check performance thresholds
    this.checkPerformanceThresholds(metric)
  }
  
  private checkPerformanceThresholds(metric: PerformanceMetric): void {
    const thresholds = {
      'LCP': 2500,    // Largest Contentful Paint
      'FID': 100,     // First Input Delay
      'CLS': 0.1,     // Cumulative Layout Shift
      'interaction': 100 // Custom interaction timing
    }
    
    const threshold = thresholds[metric.name as keyof typeof thresholds]
    
    if (threshold && metric.value && metric.value > threshold) {
      console.warn(`Performance threshold exceeded: ${metric.name} = ${metric.value}ms`)
      
      // Send alert for critical metrics
      if (metric.name === 'LCP' && metric.value > 4000) {
        this.sendPerformanceAlert(metric)
      }
    }
  }
}

// Performance Hook
export const usePerformanceMonitoring = () => {
  const [monitor] = useState(() => new MobilePerformanceMonitor())
  
  const measurePageLoad = useCallback((pageName: string) => {
    const measurement = monitor.measureInteraction(`page-load-${pageName}`)
    
    useEffect(() => {
      return () => measurement.end()
    }, [])
    
    return measurement
  }, [monitor])
  
  const measureComponentRender = useCallback((componentName: string) => {
    const measurement = monitor.measureInteraction(`component-render-${componentName}`)
    
    useEffect(() => {
      return () => measurement.end()
    }, [])
    
    return measurement
  }, [monitor])
  
  const getPerformanceReport = useCallback(() => {
    return monitor.getPerformanceReport()
  }, [monitor])
  
  return {
    measurePageLoad,
    measureComponentRender,
    getPerformanceReport
  }
}
```

### Optimized Component Architecture
```tsx
// High-Performance Mobile Components
import { memo, useMemo, useCallback, startTransition } from 'react'
import { FixedSizeList as List } from 'react-window'

// Optimized Market Data List
export const OptimizedMarketDataList: React.FC<{
  data: MarketData[]
  height: number
}> = memo(({ data, height }) => {
  const { measureComponentRender } = usePerformanceMonitoring()
  
  useEffect(() => {
    const measurement = measureComponentRender('MarketDataList')
    return () => measurement.end()
  }, [measureComponentRender])
  
  const memoizedData = useMemo(() => {
    return data.map(item => ({
      ...item,
      formattedPrice: `₹${item.price.toFixed(2)}`,
      formattedChange: item.change >= 0 ? `+${item.change.toFixed(2)}` : item.change.toFixed(2),
      changeClass: item.change >= 0 ? 'positive' : 'negative'
    }))
  }, [data])
  
  const renderRow = useCallback(({ index, style }: any) => {
    const item = memoizedData[index]
    
    return (
      <div style={style} className="market-data-row">
        <OptimizedMarketDataRow data={item} />
      </div>
    )
  }, [memoizedData])
  
  return (
    <List
      height={height}
      itemCount={memoizedData.length}
      itemSize={64}
      width="100%"
      overscanCount={5}
    >
      {renderRow}
    </List>
  )
})

// Optimized Individual Row Component
const OptimizedMarketDataRow: React.FC<{ data: FormattedMarketData }> = memo(({ data }) => {
  const handleRowClick = useCallback(() => {
    // Use startTransition for non-urgent updates
    startTransition(() => {
      // Navigate to stock details
      window.location.href = `/stock/${data.symbol}`
    })
  }, [data.symbol])
  
  return (
    <div className="market-row" onClick={handleRowClick}>
      <div className="symbol-section">
        <span className="symbol">{data.symbol}</span>
        <span className="company">{data.companyName}</span>
      </div>
      
      <div className="price-section">
        <span className="price">{data.formattedPrice}</span>
        <span className={`change ${data.changeClass}`}>
          {data.formattedChange}
        </span>
      </div>
      
      <div className="chart-section">
        <MiniChart data={data.priceHistory} />
      </div>
    </div>
  )
}, (prevProps, nextProps) => {
  // Custom comparison for better performance
  return (
    prevProps.data.symbol === nextProps.data.symbol &&
    prevProps.data.price === nextProps.data.price &&
    prevProps.data.change === nextProps.data.change
  )
})

// Optimized Chart Component
const MiniChart: React.FC<{ data: number[] }> = memo(({ data }) => {
  const chartPath = useMemo(() => {
    if (data.length < 2) return ''
    
    const width = 60
    const height = 30
    const padding = 2
    
    const min = Math.min(...data)
    const max = Math.max(...data)
    const range = max - min || 1
    
    const points = data.map((value, index) => {
      const x = padding + (index / (data.length - 1)) * (width - 2 * padding)
      const y = height - padding - ((value - min) / range) * (height - 2 * padding)
      return `${x},${y}`
    }).join(' ')
    
    return `M ${points.replace(/,/g, ' L ').substring(2)}`
  }, [data])
  
  if (!chartPath) return null
  
  return (
    <svg width="60" height="30" className="mini-chart">
      <path
        d={chartPath}
        fill="none"
        stroke="currentColor"
        strokeWidth="1"
        vectorEffect="non-scaling-stroke"
      />
    </svg>
  )
}, (prevProps, nextProps) => {
  return JSON.stringify(prevProps.data) === JSON.stringify(nextProps.data)
})
```

### Bundle Optimization & Code Splitting
```typescript
// Dynamic Import Strategy
const loadComponentWithPerformanceTracking = (
  componentName: string,
  importFunc: () => Promise<any>
) => {
  return lazy(async () => {
    const startTime = performance.now()
    
    try {
      const component = await importFunc()
      const loadTime = performance.now() - startTime
      
      // Track component load time
      performance.measure(`component-load-${componentName}`, {
        start: startTime,
        end: startTime + loadTime
      })
      
      console.log(`Component ${componentName} loaded in ${loadTime.toFixed(2)}ms`)
      
      return component
    } catch (error) {
      console.error(`Failed to load component ${componentName}:`, error)
      throw error
    }
  })
}

// Optimized Route Splitting
export const TradingDashboard = loadComponentWithPerformanceTracking(
  'TradingDashboard',
  () => import('./pages/TradingDashboard')
)

export const PortfolioDashboard = loadComponentWithPerformanceTracking(
  'PortfolioDashboard',
  () => import('./pages/PortfolioDashboard')
)

export const MarketAnalysis = loadComponentWithPerformanceTracking(
  'MarketAnalysis',
  () => import('./pages/MarketAnalysis')
)

// Preloading Strategy
class ComponentPreloader {
  private preloadedComponents = new Set<string>()
  
  async preloadComponent(name: string, importFunc: () => Promise<any>): Promise<void> {
    if (this.preloadedComponents.has(name)) return
    
    try {
      // Use requestIdleCallback for non-critical preloading
      if ('requestIdleCallback' in window) {
        requestIdleCallback(async () => {
          await importFunc()
          this.preloadedComponents.add(name)
          console.log(`Component ${name} preloaded`)
        })
      } else {
        // Fallback for browsers without requestIdleCallback
        setTimeout(async () => {
          await importFunc()
          this.preloadedComponents.add(name)
        }, 100)
      }
    } catch (error) {
      console.error(`Failed to preload component ${name}:`, error)
    }
  }
  
  preloadCriticalComponents(): void {
    // Preload components likely to be needed soon
    this.preloadComponent('TradingForm', () => import('./components/TradingForm'))
    this.preloadComponent('OrderBook', () => import('./components/OrderBook'))
    this.preloadComponent('ChartWidget', () => import('./components/ChartWidget'))
  }
}

// Memory Management Utilities
export class MemoryManager {
  private componentRefs = new WeakMap()
  private imageCache = new Map<string, string>()
  private maxCacheSize = 50 // Maximum cached images
  
  trackComponent(component: React.Component, name: string): void {
    this.componentRefs.set(component, { name, mountTime: Date.now() })
  }
  
  untrackComponent(component: React.Component): void {
    this.componentRefs.delete(component)
  }
  
  optimizeImageCache(url: string, blob: Blob): string {
    // Clean old entries if cache is full
    if (this.imageCache.size >= this.maxCacheSize) {
      const oldestKey = this.imageCache.keys().next().value
      const oldUrl = this.imageCache.get(oldestKey)
      if (oldUrl) {
        URL.revokeObjectURL(oldUrl)
        this.imageCache.delete(oldestKey)
      }
    }
    
    const objectUrl = URL.createObjectURL(blob)
    this.imageCache.set(url, objectUrl)
    
    return objectUrl
  }
  
  cleanup(): void {
    // Cleanup object URLs
    this.imageCache.forEach(url => URL.revokeObjectURL(url))
    this.imageCache.clear()
    
    // Force garbage collection if available
    if ('gc' in window) {
      (window as any).gc()
    }
  }
  
  getMemoryUsage(): MemoryInfo | null {
    if ('memory' in performance) {
      return (performance as any).memory
    }
    return null
  }
}
```

### Network Optimization
```typescript
// Intelligent Network Management
class NetworkOptimizer {
  private connectionInfo: NetworkInformation | null = null
  private adaptiveQuality = true
  
  constructor() {
    this.initializeNetworkMonitoring()
  }
  
  private initializeNetworkMonitoring(): void {
    if ('connection' in navigator) {
      this.connectionInfo = (navigator as any).connection
      
      this.connectionInfo?.addEventListener('change', () => {
        this.handleConnectionChange()
      })
    }
  }
  
  private handleConnectionChange(): void {
    if (!this.connectionInfo) return
    
    const { effectiveType, saveData } = this.connectionInfo
    
    // Adjust quality based on connection
    switch (effectiveType) {
      case 'slow-2g':
      case '2g':
        this.setDataSavingMode(true)
        this.setImageQuality('low')
        this.setUpdateFrequency('conservative')
        break
      
      case '3g':
        this.setDataSavingMode(saveData)
        this.setImageQuality('medium')
        this.setUpdateFrequency('normal')
        break
      
      case '4g':
      default:
        this.setDataSavingMode(saveData)
        this.setImageQuality('high')
        this.setUpdateFrequency('aggressive')
        break
    }
  }
  
  private setDataSavingMode(enabled: boolean): void {
    document.documentElement.classList.toggle('data-saving-mode', enabled)
    
    // Reduce WebSocket update frequency
    if (enabled) {
      this.throttleWebSocketUpdates(2000) // 2 second throttle
    } else {
      this.throttleWebSocketUpdates(500)  // 500ms throttle
    }
  }
  
  private setImageQuality(quality: 'low' | 'medium' | 'high'): void {
    const qualityMap = {
      low: 0.3,
      medium: 0.6,
      high: 0.8
    }
    
    document.documentElement.style.setProperty(
      '--image-quality',
      qualityMap[quality].toString()
    )
  }
  
  private setUpdateFrequency(frequency: 'conservative' | 'normal' | 'aggressive'): void {
    const frequencyMap = {
      conservative: 5000,  // 5 seconds
      normal: 2000,        // 2 seconds
      aggressive: 1000     // 1 second
    }
    
    const interval = frequencyMap[frequency]
    
    // Communicate with WebSocket service
    window.dispatchEvent(new CustomEvent('updateFrequencyChange', {
      detail: { interval }
    }))
  }
  
  private throttleWebSocketUpdates(interval: number): void {
    window.dispatchEvent(new CustomEvent('webSocketThrottle', {
      detail: { interval }
    }))
  }
  
  public getOptimalImageSize(baseWidth: number, baseHeight: number): ImageDimensions {
    if (!this.connectionInfo) return { width: baseWidth, height: baseHeight }
    
    const { effectiveType, saveData } = this.connectionInfo
    
    let scaleFactor = 1
    
    if (saveData || effectiveType === 'slow-2g' || effectiveType === '2g') {
      scaleFactor = 0.5
    } else if (effectiveType === '3g') {
      scaleFactor = 0.75
    }
    
    return {
      width: Math.round(baseWidth * scaleFactor),
      height: Math.round(baseHeight * scaleFactor)
    }
  }
  
  public shouldPreloadData(): boolean {
    if (!this.connectionInfo) return true
    
    const { effectiveType, saveData } = this.connectionInfo
    
    return !saveData && (effectiveType === '4g' || effectiveType === undefined)
  }
}

// Resource Loading Optimization
export const optimizedImageLoader = (src: string, options: ImageLoadOptions = {}): Promise<string> => {
  return new Promise((resolve, reject) => {
    const img = new Image()
    const networkOptimizer = new NetworkOptimizer()
    
    // Get optimal dimensions based on network
    const { width, height } = networkOptimizer.getOptimalImageSize(
      options.width || 300,
      options.height || 200
    )
    
    // Create optimized URL with query parameters
    const url = new URL(src)
    url.searchParams.set('w', width.toString())
    url.searchParams.set('h', height.toString())
    url.searchParams.set('q', options.quality?.toString() || '80')
    
    img.onload = () => resolve(url.toString())
    img.onerror = reject
    img.src = url.toString()
  })
}
```

## Acceptance Criteria

### Load Performance
- [ ] **Initial Load**: <3 seconds on 3G networks
- [ ] **Time to Interactive**: <5 seconds on mid-range devices
- [ ] **Bundle Size**: JavaScript bundle <500KB compressed
- [ ] **Critical Path**: Above-the-fold content loads in <2 seconds

### Runtime Performance
- [ ] **Frame Rate**: 60fps scrolling on lists with 1000+ items
- [ ] **Memory Usage**: <100MB for extended trading sessions
- [ ] **CPU Usage**: <30% average, <80% peak for smooth interactions
- [ ] **Battery Life**: <5% battery drain per hour of active usage

### Network Optimization
- [ ] **Adaptive Loading**: Automatic quality adjustment based on connection speed
- [ ] **Data Savings**: 50% reduction in data usage on slow connections
- [ ] **Offline Support**: Core functionality available without network
- [ ] **Progressive Loading**: Content loads progressively with network improvements

### User Experience
- [ ] **Interaction Response**: <100ms response time for all user interactions
- [ ] **Visual Feedback**: Immediate visual feedback for all user actions
- [ ] **Smooth Animations**: 60fps animations with hardware acceleration
- [ ] **Error Recovery**: Graceful handling of performance issues

## Testing Strategy

### Performance Testing
- Real device testing on budget Android devices (Redmi, Realme)
- Network throttling simulation (2G, 3G, slow 3G)
- Battery usage monitoring during extended sessions
- Memory leak detection and prevention

### Load Testing
- Bundle size analysis and optimization
- Critical rendering path optimization
- Code splitting effectiveness validation
- Resource loading sequence optimization

### User Experience Testing
- Touch response time measurement
- Scroll performance validation
- Animation smoothness verification
- Multi-tasking performance assessment

### Regression Testing
- Performance benchmark comparison
- Automated performance testing in CI/CD
- Real user monitoring alerts
- Performance budget enforcement

## Definition of Done
- [ ] Load time <3 seconds on 3G networks validated
- [ ] 60fps scrolling achieved on lists with 1000+ items
- [ ] Memory usage <100MB during extended sessions
- [ ] Bundle size optimized to <500KB compressed
- [ ] Network-adaptive loading implemented and tested
- [ ] Performance monitoring integrated with real user data
- [ ] Battery usage optimized to <5% drain per hour
- [ ] Smooth animations with hardware acceleration enabled
- [ ] Performance regression testing integrated in CI/CD
- [ ] Cross-device performance validation completed

## Story Points: 25

## Dependencies
- CDN configuration for optimized asset delivery
- Performance monitoring infrastructure
- Real user monitoring integration
- Bundle analyzer tooling integration

## Notes
- Consider implementing performance budgets in CI/CD pipeline
- Regular performance audits should be scheduled monthly
- Integration with error monitoring for performance-related issues
- Documentation for performance optimization guidelines for development team