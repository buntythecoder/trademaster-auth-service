import { useState, useEffect, useCallback } from 'react'

export interface PWAInstallPrompt extends Event {
  readonly platforms: string[]
  readonly userChoice: Promise<{
    outcome: 'accepted' | 'dismissed'
    platform: string
  }>
  prompt(): Promise<void>
}

export interface NotificationPermission {
  permission: NotificationPermission['permission']
  subscribe: () => Promise<PushSubscription | null>
  unsubscribe: () => Promise<boolean>
}

export interface PWAState {
  isInstalled: boolean
  isInstallable: boolean
  isOffline: boolean
  isUpdateAvailable: boolean
  showInstallPrompt: boolean
  notificationPermission: NotificationPermission['permission']
  registration: ServiceWorkerRegistration | null
}

export interface PWAActions {
  installApp: () => Promise<boolean>
  dismissInstallPrompt: () => void
  updateApp: () => Promise<void>
  subscribeToNotifications: () => Promise<PushSubscription | null>
  unsubscribeFromNotifications: () => Promise<boolean>
  showNotification: (title: string, options?: NotificationOptions) => Promise<void>
  cacheMarketData: (symbols: any[]) => void
  registerPriceAlert: (alert: any) => void
}

export interface UsePWAReturn extends PWAState, PWAActions {}

export const usePWA = (): UsePWAReturn => {
  const [state, setState] = useState<PWAState>({
    isInstalled: false,
    isInstallable: false,
    isOffline: !navigator.onLine,
    isUpdateAvailable: false,
    showInstallPrompt: false,
    notificationPermission: 'default',
    registration: null
  })

  const [deferredPrompt, setDeferredPrompt] = useState<PWAInstallPrompt | null>(null)

  // Initialize service worker and PWA features
  useEffect(() => {
    const initializePWA = async () => {
      // Register service worker
      if ('serviceWorker' in navigator) {
        try {
          const registration = await navigator.serviceWorker.register('/sw.js', {
            scope: '/',
            updateViaCache: 'none'
          })

          console.log('Service Worker registered:', registration)
          
          setState(prev => ({ ...prev, registration }))

          // Listen for updates
          registration.addEventListener('updatefound', () => {
            const newWorker = registration.installing
            if (newWorker) {
              newWorker.addEventListener('statechange', () => {
                if (newWorker.state === 'installed' && navigator.serviceWorker.controller) {
                  setState(prev => ({ ...prev, isUpdateAvailable: true }))
                }
              })
            }
          })

          // Listen for messages from service worker
          navigator.serviceWorker.addEventListener('message', (event) => {
            const { type, data } = event.data
            
            switch (type) {
              case 'CACHE_UPDATED':
                console.log('Cache updated:', data)
                break
              case 'BACKGROUND_SYNC':
                console.log('Background sync completed:', data)
                break
              case 'NOTIFICATION_CLICK':
                console.log('Notification clicked:', data)
                // Handle notification click data
                break
            }
          })

        } catch (error) {
          console.error('Service Worker registration failed:', error)
        }
      }

      // Check if app is already installed
      if (window.matchMedia('(display-mode: standalone)').matches || 
          (window.navigator as any).standalone === true) {
        setState(prev => ({ ...prev, isInstalled: true }))
      }

      // Get initial notification permission
      if ('Notification' in window) {
        setState(prev => ({ 
          ...prev, 
          notificationPermission: Notification.permission 
        }))
      }
    }

    initializePWA()
  }, [])

  // Listen for install prompt
  useEffect(() => {
    const handleBeforeInstallPrompt = (e: Event) => {
      e.preventDefault()
      const promptEvent = e as PWAInstallPrompt
      setDeferredPrompt(promptEvent)
      setState(prev => ({ 
        ...prev, 
        isInstallable: true,
        showInstallPrompt: true 
      }))
    }

    const handleAppInstalled = () => {
      setDeferredPrompt(null)
      setState(prev => ({ 
        ...prev, 
        isInstalled: true,
        isInstallable: false,
        showInstallPrompt: false 
      }))
    }

    window.addEventListener('beforeinstallprompt', handleBeforeInstallPrompt)
    window.addEventListener('appinstalled', handleAppInstalled)

    return () => {
      window.removeEventListener('beforeinstallprompt', handleBeforeInstallPrompt)
      window.removeEventListener('appinstalled', handleAppInstalled)
    }
  }, [])

  // Listen for online/offline status
  useEffect(() => {
    const handleOnline = () => setState(prev => ({ ...prev, isOffline: false }))
    const handleOffline = () => setState(prev => ({ ...prev, isOffline: true }))

    window.addEventListener('online', handleOnline)
    window.addEventListener('offline', handleOffline)

    return () => {
      window.removeEventListener('online', handleOnline)
      window.removeEventListener('offline', handleOffline)
    }
  }, [])

  // Install app
  const installApp = useCallback(async (): Promise<boolean> => {
    if (!deferredPrompt) {
      return false
    }

    try {
      await deferredPrompt.prompt()
      const { outcome } = await deferredPrompt.userChoice
      
      if (outcome === 'accepted') {
        setState(prev => ({ 
          ...prev, 
          isInstalled: true,
          showInstallPrompt: false,
          isInstallable: false 
        }))
        setDeferredPrompt(null)
        return true
      }
    } catch (error) {
      console.error('Failed to install app:', error)
    }

    return false
  }, [deferredPrompt])

  // Dismiss install prompt
  const dismissInstallPrompt = useCallback(() => {
    setState(prev => ({ ...prev, showInstallPrompt: false }))
  }, [])

  // Update app
  const updateApp = useCallback(async (): Promise<void> => {
    if (!state.registration || !state.isUpdateAvailable) {
      return
    }

    try {
      const waitingWorker = state.registration.waiting
      if (waitingWorker) {
        // Send message to skip waiting
        waitingWorker.postMessage({ type: 'SKIP_WAITING' })
        
        // Listen for controlling change
        navigator.serviceWorker.addEventListener('controllerchange', () => {
          window.location.reload()
        })
      }
    } catch (error) {
      console.error('Failed to update app:', error)
    }
  }, [state.registration, state.isUpdateAvailable])

  // Subscribe to push notifications
  const subscribeToNotifications = useCallback(async (): Promise<PushSubscription | null> => {
    if (!('Notification' in window) || !state.registration) {
      return null
    }

    try {
      const permission = await Notification.requestPermission()
      setState(prev => ({ ...prev, notificationPermission: permission }))

      if (permission === 'granted') {
        const subscription = await state.registration.pushManager.subscribe({
          userVisibleOnly: true,
          applicationServerKey: urlBase64ToUint8Array(
            process.env.REACT_APP_VAPID_PUBLIC_KEY || ''
          )
        })

        // Send subscription to server
        await fetch('/api/notifications/subscribe', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify(subscription),
        })

        return subscription
      }
    } catch (error) {
      console.error('Failed to subscribe to notifications:', error)
    }

    return null
  }, [state.registration])

  // Unsubscribe from push notifications
  const unsubscribeFromNotifications = useCallback(async (): Promise<boolean> => {
    if (!state.registration) {
      return false
    }

    try {
      const subscription = await state.registration.pushManager.getSubscription()
      if (subscription) {
        await subscription.unsubscribe()
        
        // Notify server
        await fetch('/api/notifications/unsubscribe', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({ endpoint: subscription.endpoint }),
        })
        
        return true
      }
    } catch (error) {
      console.error('Failed to unsubscribe from notifications:', error)
    }

    return false
  }, [state.registration])

  // Show local notification
  const showNotification = useCallback(async (
    title: string, 
    options?: NotificationOptions
  ): Promise<void> => {
    if (!('Notification' in window)) {
      return
    }

    if (Notification.permission === 'granted') {
      if (state.registration) {
        // Use service worker to show notification
        await state.registration.showNotification(title, {
          badge: '/icons/badge-96x96.png',
          icon: '/icons/icon-192x192.png',
          vibrate: [200, 100, 200],
          ...options
        })
      } else {
        // Fallback to regular notification
        new Notification(title, options)
      }
    } else if (Notification.permission !== 'denied') {
      const permission = await Notification.requestPermission()
      setState(prev => ({ ...prev, notificationPermission: permission }))
      
      if (permission === 'granted') {
        await showNotification(title, options)
      }
    }
  }, [state.registration])

  // Cache market data for offline use
  const cacheMarketData = useCallback((symbols: any[]) => {
    if (state.registration && navigator.serviceWorker.controller) {
      navigator.serviceWorker.controller.postMessage({
        type: 'CACHE_MARKET_DATA',
        data: { symbols }
      })
    }
  }, [state.registration])

  // Register price alert
  const registerPriceAlert = useCallback((alert: any) => {
    if (state.registration && navigator.serviceWorker.controller) {
      navigator.serviceWorker.controller.postMessage({
        type: 'REGISTER_PRICE_ALERT',
        data: alert
      })
    }
  }, [state.registration])

  return {
    ...state,
    installApp,
    dismissInstallPrompt,
    updateApp,
    subscribeToNotifications,
    unsubscribeFromNotifications,
    showNotification,
    cacheMarketData,
    registerPriceAlert
  }
}

// Helper function to convert VAPID key
function urlBase64ToUint8Array(base64String: string): Uint8Array {
  const padding = '='.repeat((4 - base64String.length % 4) % 4)
  const base64 = (base64String + padding)
    .replace(/\-/g, '+')
    .replace(/_/g, '/')

  const rawData = window.atob(base64)
  const outputArray = new Uint8Array(rawData.length)

  for (let i = 0; i < rawData.length; ++i) {
    outputArray[i] = rawData.charCodeAt(i)
  }
  return outputArray
}

export default usePWA