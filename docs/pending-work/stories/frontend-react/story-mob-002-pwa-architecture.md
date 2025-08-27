# Story MOB-002: PWA Architecture Implementation

## Epic
Epic 4: Mobile-First Design & PWA

## Story Overview
**As a** TradeMaster user  
**I want** a Progressive Web App experience that works offline and can be installed  
**So that** I can access trading functionality even with poor connectivity and have an app-like experience

## Business Value
- **App Store Independence**: No dependency on app store approvals and fees
- **Instant Updates**: Immediate deployment of new features without app store delays
- **Offline Capability**: Continue trading preparations even without network connectivity
- **Reduced Development Cost**: Single codebase for web and mobile app experience

## Technical Requirements

### Service Worker Implementation
```typescript
// Service Worker Registration
export const registerServiceWorker = async (): Promise<ServiceWorkerRegistration | null> => {
  if ('serviceWorker' in navigator) {
    try {
      const registration = await navigator.serviceWorker.register('/sw.js', {
        scope: '/',
        updateViaCache: 'none'
      })
      
      console.log('ServiceWorker registration successful:', registration.scope)
      
      // Handle updates
      registration.addEventListener('updatefound', () => {
        const newWorker = registration.installing
        if (newWorker) {
          newWorker.addEventListener('statechange', () => {
            if (newWorker.state === 'installed' && navigator.serviceWorker.controller) {
              // New version available
              showUpdateAvailableNotification()
            }
          })
        }
      })
      
      return registration
    } catch (error) {
      console.error('ServiceWorker registration failed:', error)
      return null
    }
  }
  return null
}

// Service Worker Update Handler
export const handleServiceWorkerUpdate = () => {
  const updateButton = document.getElementById('update-app-button')
  
  if (updateButton) {
    updateButton.addEventListener('click', () => {
      navigator.serviceWorker.controller?.postMessage({ type: 'SKIP_WAITING' })
      window.location.reload()
    })
  }
}
```

### Advanced Service Worker (sw.js)
```javascript
// Service Worker - Advanced Caching Strategy
const CACHE_NAME = 'trademaster-v1.0.0'
const RUNTIME_CACHE = 'trademaster-runtime'
const API_CACHE = 'trademaster-api'
const IMAGE_CACHE = 'trademaster-images'

// Static assets to cache immediately
const STATIC_ASSETS = [
  '/',
  '/dashboard',
  '/portfolio',
  '/static/js/main.js',
  '/static/css/main.css',
  '/manifest.json',
  '/icons/icon-192x192.png',
  '/icons/icon-512x512.png',
  '/offline.html'
]

// Cache strategies
const CACHE_STRATEGIES = {
  static: 'CacheFirst',
  api: 'NetworkFirst', 
  images: 'CacheFirst',
  runtime: 'StaleWhileRevalidate'
}

// Install event - Cache static assets
self.addEventListener('install', (event) => {
  console.log('ServiceWorker installing...')
  
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then((cache) => {
        console.log('Caching static assets')
        return cache.addAll(STATIC_ASSETS)
      })
      .then(() => {
        console.log('Static assets cached successfully')
        return self.skipWaiting() // Activate immediately
      })
  )
})

// Activate event - Clean old caches
self.addEventListener('activate', (event) => {
  console.log('ServiceWorker activating...')
  
  event.waitUntil(
    caches.keys()
      .then((cacheNames) => {
        return Promise.all(
          cacheNames.map((cacheName) => {
            if (cacheName !== CACHE_NAME && 
                cacheName !== RUNTIME_CACHE && 
                cacheName !== API_CACHE && 
                cacheName !== IMAGE_CACHE) {
              console.log('Deleting old cache:', cacheName)
              return caches.delete(cacheName)
            }
          })
        )
      })
      .then(() => {
        console.log('ServiceWorker activated')
        return self.clients.claim() // Take control immediately
      })
  )
})

// Fetch event - Smart caching strategy
self.addEventListener('fetch', (event) => {
  const { request } = event
  const url = new URL(request.url)
  
  // Handle different resource types
  if (request.method === 'GET') {
    if (url.pathname.startsWith('/api/')) {
      // API requests - NetworkFirst with offline support
      event.respondWith(handleApiRequest(request))
    } else if (request.destination === 'image') {
      // Images - CacheFirst
      event.respondWith(handleImageRequest(request))
    } else if (isNavigationRequest(request)) {
      // Navigation requests - StaleWhileRevalidate
      event.respondWith(handleNavigationRequest(request))
    } else {
      // Static assets - CacheFirst
      event.respondWith(handleStaticRequest(request))
    }
  } else if (request.method === 'POST' && url.pathname.startsWith('/api/orders')) {
    // Handle offline order queue
    event.respondWith(handleOfflineOrder(request))
  }
})

// API Request Handler - Network First with offline fallback
async function handleApiRequest(request) {
  const cache = await caches.open(API_CACHE)
  
  try {
    // Try network first
    const networkResponse = await fetch(request.clone())
    
    // Cache successful responses
    if (networkResponse.ok) {
      cache.put(request, networkResponse.clone())
    }
    
    return networkResponse
  } catch (error) {
    console.log('Network failed, trying cache:', request.url)
    
    // Fallback to cache
    const cachedResponse = await cache.match(request)
    if (cachedResponse) {
      return cachedResponse
    }
    
    // Return offline page for critical API failures
    if (request.url.includes('/api/user') || request.url.includes('/api/portfolio')) {
      return new Response(JSON.stringify({
        error: 'Offline mode',
        message: 'You are currently offline. Some features may be limited.'
      }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' }
      })
    }
    
    throw error
  }
}

// Navigation Request Handler
async function handleNavigationRequest(request) {
  const cache = await caches.open(CACHE_NAME)
  
  try {
    // Try network first
    const networkResponse = await fetch(request)
    
    // Cache the response
    cache.put(request, networkResponse.clone())
    return networkResponse
    
  } catch (error) {
    // Fallback to cache
    const cachedResponse = await cache.match(request)
    if (cachedResponse) {
      return cachedResponse
    }
    
    // Fallback to offline page
    return cache.match('/offline.html')
  }
}

// Background Sync for Offline Orders
self.addEventListener('sync', (event) => {
  if (event.tag === 'offline-orders') {
    event.waitUntil(syncOfflineOrders())
  }
})

async function syncOfflineOrders() {
  console.log('Syncing offline orders...')
  
  try {
    // Get offline orders from IndexedDB
    const offlineOrders = await getOfflineOrders()
    
    for (const order of offlineOrders) {
      try {
        const response = await fetch('/api/orders', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': order.authToken
          },
          body: JSON.stringify(order.data)
        })
        
        if (response.ok) {
          // Remove from offline storage
          await removeOfflineOrder(order.id)
          
          // Notify user of successful sync
          self.registration.showNotification('Order Placed', {
            body: `Your ${order.data.orderType} order for ${order.data.symbol} has been placed successfully.`,
            icon: '/icons/icon-192x192.png',
            badge: '/icons/badge-72x72.png',
            tag: 'order-sync'
          })
        }
      } catch (error) {
        console.error('Failed to sync order:', order.id, error)
      }
    }
  } catch (error) {
    console.error('Failed to sync offline orders:', error)
  }
}
```

### PWA Configuration & Manifest
```json
{
  "name": "TradeMaster - Smart Trading Platform",
  "short_name": "TradeMaster",
  "description": "India's most advanced trading platform with AI-powered insights",
  "start_url": "/dashboard?source=pwa",
  "display": "standalone",
  "orientation": "portrait-primary",
  "theme_color": "#10B981",
  "background_color": "#FFFFFF",
  "scope": "/",
  "categories": ["finance", "productivity", "business"],
  "lang": "en-IN",
  "icons": [
    {
      "src": "/icons/icon-72x72.png",
      "sizes": "72x72",
      "type": "image/png",
      "purpose": "any maskable"
    },
    {
      "src": "/icons/icon-96x96.png",
      "sizes": "96x96",
      "type": "image/png",
      "purpose": "any maskable"
    },
    {
      "src": "/icons/icon-128x128.png",
      "sizes": "128x128",
      "type": "image/png",
      "purpose": "any maskable"
    },
    {
      "src": "/icons/icon-144x144.png",
      "sizes": "144x144",
      "type": "image/png",
      "purpose": "any maskable"
    },
    {
      "src": "/icons/icon-152x152.png",
      "sizes": "152x152",
      "type": "image/png",
      "purpose": "any maskable"
    },
    {
      "src": "/icons/icon-192x192.png",
      "sizes": "192x192",
      "type": "image/png",
      "purpose": "any maskable"
    },
    {
      "src": "/icons/icon-384x384.png",
      "sizes": "384x384",
      "type": "image/png",
      "purpose": "any maskable"
    },
    {
      "src": "/icons/icon-512x512.png",
      "sizes": "512x512",
      "type": "image/png",
      "purpose": "any maskable"
    }
  ],
  "shortcuts": [
    {
      "name": "Trading Dashboard",
      "short_name": "Trade",
      "description": "Access your trading dashboard",
      "url": "/dashboard",
      "icons": [{ "src": "/icons/shortcut-trade.png", "sizes": "96x96" }]
    },
    {
      "name": "Portfolio",
      "short_name": "Portfolio",
      "description": "View your portfolio performance",
      "url": "/portfolio",
      "icons": [{ "src": "/icons/shortcut-portfolio.png", "sizes": "96x96" }]
    },
    {
      "name": "Market Analysis",
      "short_name": "Analysis",
      "description": "View market analysis and insights",
      "url": "/analysis",
      "icons": [{ "src": "/icons/shortcut-analysis.png", "sizes": "96x96" }]
    }
  ],
  "screenshots": [
    {
      "src": "/screenshots/dashboard-mobile.png",
      "sizes": "375x812",
      "type": "image/png",
      "platform": "narrow",
      "label": "Trading Dashboard on Mobile"
    },
    {
      "src": "/screenshots/dashboard-desktop.png",
      "sizes": "1280x720",
      "type": "image/png",
      "platform": "wide",
      "label": "Trading Dashboard on Desktop"
    }
  ],
  "prefer_related_applications": false,
  "related_applications": [],
  "protocol_handlers": [
    {
      "protocol": "web+trademaster",
      "url": "/trade?symbol=%s"
    }
  ]
}
```

### Offline Functionality
```typescript
// Offline Order Queue System
class OfflineOrderManager {
  private db: IDBDatabase | null = null
  
  async initialize(): Promise<void> {
    return new Promise((resolve, reject) => {
      const request = indexedDB.open('TradeMasterOffline', 1)
      
      request.onerror = () => reject(request.error)
      request.onsuccess = () => {
        this.db = request.result
        resolve()
      }
      
      request.onupgradeneeded = (event) => {
        const db = (event.target as IDBOpenDBRequest).result
        
        // Create orders store
        if (!db.objectStoreNames.contains('orders')) {
          const ordersStore = db.createObjectStore('orders', { keyPath: 'id' })
          ordersStore.createIndex('timestamp', 'timestamp', { unique: false })
          ordersStore.createIndex('symbol', 'symbol', { unique: false })
        }
        
        // Create market data store
        if (!db.objectStoreNames.contains('marketData')) {
          const marketStore = db.createObjectStore('marketData', { keyPath: 'symbol' })
          marketStore.createIndex('timestamp', 'lastUpdated', { unique: false })
        }
      }
    })
  }
  
  async queueOrder(orderData: OfflineOrder): Promise<void> {
    if (!this.db) await this.initialize()
    
    const transaction = this.db!.transaction(['orders'], 'readwrite')
    const store = transaction.objectStore('orders')
    
    const order: StoredOfflineOrder = {
      id: generateOrderId(),
      data: orderData,
      timestamp: Date.now(),
      authToken: await getAuthToken(),
      retryCount: 0,
      status: 'queued'
    }
    
    await store.add(order)
    
    // Register background sync if supported
    if ('serviceWorker' in navigator && 'sync' in window.ServiceWorkerRegistration.prototype) {
      const registration = await navigator.serviceWorker.ready
      await registration.sync.register('offline-orders')
    }
  }
  
  async getQueuedOrders(): Promise<StoredOfflineOrder[]> {
    if (!this.db) await this.initialize()
    
    return new Promise((resolve, reject) => {
      const transaction = this.db!.transaction(['orders'], 'readonly')
      const store = transaction.objectStore('orders')
      const request = store.getAll()
      
      request.onsuccess = () => resolve(request.result)
      request.onerror = () => reject(request.error)
    })
  }
  
  async removeOrder(orderId: string): Promise<void> {
    if (!this.db) await this.initialize()
    
    const transaction = this.db!.transaction(['orders'], 'readwrite')
    const store = transaction.objectStore('orders')
    await store.delete(orderId)
  }
}

// Offline Market Data Cache
class OfflineMarketDataCache {
  private db: IDBDatabase | null = null
  private maxAge = 5 * 60 * 1000 // 5 minutes
  
  async cacheMarketData(symbol: string, data: MarketData): Promise<void> {
    if (!this.db) await this.initialize()
    
    const transaction = this.db!.transaction(['marketData'], 'readwrite')
    const store = transaction.objectStore('marketData')
    
    const cachedData: CachedMarketData = {
      symbol,
      data,
      lastUpdated: Date.now()
    }
    
    await store.put(cachedData)
  }
  
  async getMarketData(symbol: string): Promise<MarketData | null> {
    if (!this.db) await this.initialize()
    
    return new Promise((resolve, reject) => {
      const transaction = this.db!.transaction(['marketData'], 'readonly')
      const store = transaction.objectStore('marketData')
      const request = store.get(symbol)
      
      request.onsuccess = () => {
        const result = request.result as CachedMarketData
        
        if (result && (Date.now() - result.lastUpdated) < this.maxAge) {
          resolve(result.data)
        } else {
          resolve(null)
        }
      }
      
      request.onerror = () => reject(request.error)
    })
  }
}
```

### PWA Installation Prompt
```tsx
// PWA Install Prompt Component
export const PWAInstallPrompt: React.FC = () => {
  const [deferredPrompt, setDeferredPrompt] = useState<any>(null)
  const [showPrompt, setShowPrompt] = useState(false)
  const [isInstalled, setIsInstalled] = useState(false)
  
  useEffect(() => {
    // Check if already installed
    const checkInstalled = () => {
      const isStandalone = window.matchMedia('(display-mode: standalone)').matches
      const isIOS = /iPad|iPhone|iPod/.test(navigator.userAgent)
      const isInWebAppiOS = (window.navigator as any).standalone === true
      
      setIsInstalled(isStandalone || (isIOS && isInWebAppiOS))
    }
    
    checkInstalled()
    
    // Listen for beforeinstallprompt event
    const handleBeforeInstallPrompt = (e: any) => {
      e.preventDefault()
      setDeferredPrompt(e)
      
      // Show install prompt after user has used the app for a bit
      setTimeout(() => {
        if (!isInstalled) {
          setShowPrompt(true)
        }
      }, 30000) // Show after 30 seconds
    }
    
    // Listen for app installed event
    const handleAppInstalled = () => {
      setIsInstalled(true)
      setShowPrompt(false)
      setDeferredPrompt(null)
      
      // Analytics tracking
      analytics.track('PWA Installed', {
        source: 'install_prompt',
        timestamp: Date.now()
      })
    }
    
    window.addEventListener('beforeinstallprompt', handleBeforeInstallPrompt)
    window.addEventListener('appinstalled', handleAppInstalled)
    
    return () => {
      window.removeEventListener('beforeinstallprompt', handleBeforeInstallPrompt)
      window.removeEventListener('appinstalled', handleAppInstalled)
    }
  }, [isInstalled])
  
  const handleInstallClick = async () => {
    if (deferredPrompt) {
      deferredPrompt.prompt()
      
      const choiceResult = await deferredPrompt.userChoice
      
      if (choiceResult.outcome === 'accepted') {
        console.log('User accepted the install prompt')
        analytics.track('PWA Install Accepted')
      } else {
        console.log('User dismissed the install prompt')
        analytics.track('PWA Install Dismissed')
      }
      
      setDeferredPrompt(null)
      setShowPrompt(false)
    }
  }
  
  const handleDismiss = () => {
    setShowPrompt(false)
    // Don't show again for 7 days
    localStorage.setItem('pwa-install-dismissed', Date.now().toString())
  }
  
  // Don't show if already installed or recently dismissed
  if (isInstalled || !showPrompt) return null
  
  const dismissedTime = localStorage.getItem('pwa-install-dismissed')
  if (dismissedTime && Date.now() - parseInt(dismissedTime) < 7 * 24 * 60 * 60 * 1000) {
    return null
  }
  
  return (
    <motion.div
      initial={{ opacity: 0, y: 50 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, y: 50 }}
      className="pwa-install-prompt"
    >
      <div className="prompt-content">
        <div className="prompt-icon">
          <Smartphone className="w-8 h-8" />
        </div>
        
        <div className="prompt-text">
          <h3>Install TradeMaster</h3>
          <p>Get faster access and offline capabilities. Install our app for the best trading experience.</p>
        </div>
        
        <div className="prompt-actions">
          <Button variant="outline" onClick={handleDismiss}>
            Maybe Later
          </Button>
          <Button onClick={handleInstallClick}>
            Install App
          </Button>
        </div>
      </div>
    </motion.div>
  )
}

// PWA Status Indicator
export const PWAStatusIndicator: React.FC = () => {
  const [isOnline, setIsOnline] = useState(navigator.onLine)
  const [isUpdating, setIsUpdating] = useState(false)
  const [updateAvailable, setUpdateAvailable] = useState(false)
  
  useEffect(() => {
    const handleOnline = () => setIsOnline(true)
    const handleOffline = () => setIsOnline(false)
    
    window.addEventListener('online', handleOnline)
    window.addEventListener('offline', handleOffline)
    
    return () => {
      window.removeEventListener('online', handleOnline)
      window.removeEventListener('offline', handleOffline)
    }
  }, [])
  
  return (
    <div className="pwa-status-bar">
      <div className={`connection-status ${isOnline ? 'online' : 'offline'}`}>
        {isOnline ? (
          <>
            <Wifi className="w-4 h-4" />
            <span>Online</span>
          </>
        ) : (
          <>
            <WifiOff className="w-4 h-4" />
            <span>Offline</span>
          </>
        )}
      </div>
      
      {updateAvailable && (
        <button
          className="update-available-button"
          onClick={() => window.location.reload()}
        >
          <Download className="w-4 h-4" />
          Update Available
        </button>
      )}
    </div>
  )
}
```

## Acceptance Criteria

### Core PWA Features
- [ ] **App Installation**: Installable via browser prompt and add to home screen
- [ ] **Offline Functionality**: Basic functionality available without network connection
- [ ] **Service Worker**: Proper caching strategy for static and dynamic content
- [ ] **Manifest File**: Complete web app manifest with proper metadata

### Offline Capabilities
- [ ] **Cached UI**: All interface elements available offline
- [ ] **Offline Orders**: Queue orders when offline, sync when online
- [ ] **Market Data Cache**: 5-minute cache for market data offline access
- [ ] **Background Sync**: Automatic sync when connection restored

### Performance Requirements
- [ ] **Cache Hit Rate**: >80% for static assets
- [ ] **Offline Load Time**: <1 second for cached content
- [ ] **Update Mechanism**: Smooth app updates without user disruption
- [ ] **Storage Management**: Efficient cache storage with size limits

### User Experience
- [ ] **Install Prompt**: Smart install prompt after user engagement
- [ ] **App Shortcuts**: Quick access to key features from home screen
- [ ] **Offline Indicators**: Clear offline status and limited functionality messaging
- [ ] **Update Notifications**: User-friendly update available notifications

## Testing Strategy

### PWA Functionality Testing
- Service worker installation and activation
- Offline functionality validation
- Cache strategy effectiveness
- Background sync operation

### Cross-Platform Testing
- iOS Safari installation and functionality
- Android Chrome installation and functionality
- Desktop PWA installation (Chrome, Edge)
- Manifest validation across platforms

### Performance Testing
- Cache performance optimization
- Offline load time measurement
- Storage usage monitoring
- Update mechanism efficiency

### User Experience Testing
- Installation flow usability
- Offline feature discovery
- Update notification handling
- App shortcuts functionality

## Definition of Done
- [ ] Service worker implemented with advanced caching strategies
- [ ] PWA manifest configured with all required metadata
- [ ] Offline functionality working for core trading features
- [ ] Background sync operational for offline orders
- [ ] Install prompt integrated with smart timing
- [ ] App shortcuts configured for key features
- [ ] Cross-platform PWA testing completed (iOS, Android, Desktop)
- [ ] Performance optimization validated (cache hit rates, load times)
- [ ] Update mechanism tested and user-friendly
- [ ] Analytics tracking for PWA usage and installation

## Story Points: 18

## Dependencies
- Service worker browser support validation
- Backend API optimization for offline scenarios
- IndexedDB for offline data storage
- Push notification service integration

## Notes
- Consider implementing app store deployment alongside PWA for maximum reach
- Integration with device features (camera, notifications, geolocation) for enhanced functionality
- Regular PWA audit using Lighthouse for compliance and performance optimization
- Documentation for users on PWA installation and offline capabilities