# Story MOB-003: Device Integration Features

## Epic
Epic 4: Mobile-First Design & PWA

## Story Overview
**As a** TradeMaster mobile user  
**I want** seamless integration with device features like camera, biometrics, and notifications  
**So that** I can have a secure, convenient, and native-like trading experience

## Business Value
- **Enhanced Security**: Biometric authentication reduces account compromise risk
- **User Convenience**: Camera integration streamlines KYC and document upload processes
- **Real-time Engagement**: Push notifications ensure users never miss market opportunities
- **Competitive Advantage**: Native app-like experience without app store dependency

## Technical Requirements

### Biometric Authentication Integration
```typescript
// Biometric Authentication Service
class BiometricAuthService {
  private isSupported: boolean = false
  private availableAuthenticators: AuthenticatorInfo[] = []
  
  constructor() {
    this.checkSupport()
  }
  
  private async checkSupport(): Promise<void> {
    // Check WebAuthn support
    if (!window.PublicKeyCredential) {
      console.log('WebAuthn not supported')
      return
    }
    
    // Check platform authenticator availability
    try {
      const available = await PublicKeyCredential.isUserVerifyingPlatformAuthenticatorAvailable()
      this.isSupported = available
      
      if (available) {
        await this.detectAvailableAuthenticators()
      }
    } catch (error) {
      console.error('Error checking biometric support:', error)
    }
  }
  
  private async detectAvailableAuthenticators(): Promise<void> {
    const authenticators: AuthenticatorInfo[] = []
    
    // Check for Touch ID (iOS)
    if (/iPhone|iPad|iPod/.test(navigator.userAgent)) {
      authenticators.push({
        type: 'touchid',
        name: 'Touch ID',
        icon: '👆',
        supported: true
      })
    }
    
    // Check for Face ID (iOS)
    if (/iPhone/.test(navigator.userAgent) && window.screen.height >= 812) {
      authenticators.push({
        type: 'faceid',
        name: 'Face ID',
        icon: '👤',
        supported: true
      })
    }
    
    // Check for Android biometrics
    if (/Android/.test(navigator.userAgent)) {
      authenticators.push({
        type: 'fingerprint',
        name: 'Fingerprint',
        icon: '🔒',
        supported: true
      })
    }
    
    this.availableAuthenticators = authenticators
  }
  
  async registerBiometric(userId: string): Promise<BiometricRegistrationResult> {
    if (!this.isSupported) {
      throw new Error('Biometric authentication not supported')
    }
    
    try {
      // Create credential options
      const credentialOptions: CredentialCreationOptions = {
        publicKey: {
          challenge: await this.getChallenge(),
          rp: {
            name: 'TradeMaster',
            id: window.location.hostname
          },
          user: {
            id: new TextEncoder().encode(userId),
            name: userId,
            displayName: 'TradeMaster User'
          },
          pubKeyCredParams: [
            { alg: -7, type: 'public-key' }, // ES256
            { alg: -257, type: 'public-key' } // RS256
          ],
          authenticatorSelection: {
            authenticatorAttachment: 'platform',
            userVerification: 'required',
            requireResidentKey: false
          },
          timeout: 60000,
          attestation: 'direct'
        }
      }
      
      // Create credential
      const credential = await navigator.credentials.create(credentialOptions) as PublicKeyCredential
      
      if (!credential) {
        throw new Error('Failed to create biometric credential')
      }
      
      // Register with backend
      const registrationResult = await this.sendRegistrationToServer(credential, userId)
      
      // Store credential ID for future use
      localStorage.setItem('biometric-credential-id', credential.id)
      localStorage.setItem('biometric-enabled', 'true')
      
      return {
        success: true,
        credentialId: credential.id,
        authenticatorType: this.detectAuthenticatorType()
      }
      
    } catch (error) {
      console.error('Biometric registration failed:', error)
      return {
        success: false,
        error: error.message
      }
    }
  }
  
  async authenticateWithBiometric(): Promise<BiometricAuthResult> {
    if (!this.isSupported) {
      throw new Error('Biometric authentication not supported')
    }
    
    const credentialId = localStorage.getItem('biometric-credential-id')
    if (!credentialId) {
      throw new Error('No biometric credential registered')
    }
    
    try {
      // Create assertion options
      const assertionOptions: CredentialRequestOptions = {
        publicKey: {
          challenge: await this.getChallenge(),
          allowCredentials: [{
            id: base64ToArrayBuffer(credentialId),
            type: 'public-key',
            transports: ['internal']
          }],
          userVerification: 'required',
          timeout: 60000
        }
      }
      
      // Get assertion
      const assertion = await navigator.credentials.get(assertionOptions) as PublicKeyCredential
      
      if (!assertion) {
        throw new Error('Biometric authentication failed')
      }
      
      // Verify with backend
      const authResult = await this.verifyAssertionWithServer(assertion)
      
      return {
        success: true,
        token: authResult.token,
        expiresAt: authResult.expiresAt
      }
      
    } catch (error) {
      console.error('Biometric authentication failed:', error)
      return {
        success: false,
        error: error.message
      }
    }
  }
  
  private async getChallenge(): Promise<ArrayBuffer> {
    const response = await fetch('/api/auth/challenge', { method: 'POST' })
    const data = await response.json()
    return base64ToArrayBuffer(data.challenge)
  }
  
  private detectAuthenticatorType(): string {
    if (/iPhone|iPad|iPod/.test(navigator.userAgent)) {
      return window.screen.height >= 812 ? 'faceid' : 'touchid'
    } else if (/Android/.test(navigator.userAgent)) {
      return 'fingerprint'
    }
    return 'unknown'
  }
}

// Biometric Login Component
export const BiometricLogin: React.FC = () => {
  const [isSupported, setIsSupported] = useState(false)
  const [isEnabled, setIsEnabled] = useState(false)
  const [isAuthenticating, setIsAuthenticating] = useState(false)
  const biometricService = useMemo(() => new BiometricAuthService(), [])
  
  useEffect(() => {
    const checkBiometricStatus = async () => {
      const supported = await biometricService.isSupported
      const enabled = localStorage.getItem('biometric-enabled') === 'true'
      
      setIsSupported(supported)
      setIsEnabled(enabled)
    }
    
    checkBiometricStatus()
  }, [biometricService])
  
  const handleBiometricAuth = async () => {
    setIsAuthenticating(true)
    
    try {
      const result = await biometricService.authenticateWithBiometric()
      
      if (result.success) {
        // Store token and redirect
        localStorage.setItem('auth-token', result.token)
        window.location.href = '/dashboard'
      } else {
        toast.error('Biometric authentication failed. Please try again.')
      }
    } catch (error) {
      toast.error('Biometric authentication not available')
    } finally {
      setIsAuthenticating(false)
    }
  }
  
  if (!isSupported || !isEnabled) return null
  
  return (
    <div className="biometric-login">
      <Button
        onClick={handleBiometricAuth}
        disabled={isAuthenticating}
        className="biometric-auth-button"
      >
        {isAuthenticating ? (
          <Loader2 className="w-4 h-4 animate-spin" />
        ) : (
          <Fingerprint className="w-4 h-4" />
        )}
        Sign in with Biometrics
      </Button>
    </div>
  )
}
```

### Camera Integration for KYC
```typescript
// Camera Service for Document Capture
class CameraService {
  private stream: MediaStream | null = null
  private videoElement: HTMLVideoElement | null = null
  
  async requestCameraPermission(): Promise<boolean> {
    try {
      const constraints = {
        video: {
          facingMode: 'environment', // Use back camera for documents
          width: { ideal: 1920 },
          height: { ideal: 1080 }
        }
      }
      
      this.stream = await navigator.mediaDevices.getUserMedia(constraints)
      return true
    } catch (error) {
      console.error('Camera permission denied:', error)
      return false
    }
  }
  
  async initializeCamera(videoElement: HTMLVideoElement): Promise<void> {
    if (!this.stream) {
      const hasPermission = await this.requestCameraPermission()
      if (!hasPermission) {
        throw new Error('Camera permission required')
      }
    }
    
    this.videoElement = videoElement
    videoElement.srcObject = this.stream
    await videoElement.play()
  }
  
  async captureImage(): Promise<CapturedImage> {
    if (!this.videoElement) {
      throw new Error('Camera not initialized')
    }
    
    const canvas = document.createElement('canvas')
    const context = canvas.getContext('2d')!
    
    canvas.width = this.videoElement.videoWidth
    canvas.height = this.videoElement.videoHeight
    
    // Draw current video frame to canvas
    context.drawImage(this.videoElement, 0, 0, canvas.width, canvas.height)
    
    // Get image data
    const imageBlob = await new Promise<Blob>((resolve) => {
      canvas.toBlob((blob) => resolve(blob!), 'image/jpeg', 0.8)
    })
    
    // Generate preview URL
    const previewUrl = canvas.toDataURL('image/jpeg', 0.8)
    
    return {
      blob: imageBlob,
      previewUrl,
      width: canvas.width,
      height: canvas.height,
      size: imageBlob.size
    }
  }
  
  async detectDocumentInFrame(): Promise<DocumentDetectionResult> {
    if (!this.videoElement) {
      throw new Error('Camera not initialized')
    }
    
    const canvas = document.createElement('canvas')
    const context = canvas.getContext('2d')!
    
    canvas.width = this.videoElement.videoWidth
    canvas.height = this.videoElement.videoHeight
    context.drawImage(this.videoElement, 0, 0, canvas.width, canvas.height)
    
    // Use computer vision for document detection
    // This would typically use a service like Google Vision API or AWS Rekognition
    const imageData = context.getImageData(0, 0, canvas.width, canvas.height)
    
    // Mock document detection for demonstration
    const mockDetection: DocumentDetectionResult = {
      detected: true,
      confidence: 0.85,
      boundingBox: {
        x: canvas.width * 0.1,
        y: canvas.height * 0.2,
        width: canvas.width * 0.8,
        height: canvas.height * 0.6
      },
      quality: 'good',
      recommendations: []
    }
    
    return mockDetection
  }
  
  stopCamera(): void {
    if (this.stream) {
      this.stream.getTracks().forEach(track => track.stop())
      this.stream = null
    }
    
    if (this.videoElement) {
      this.videoElement.srcObject = null
    }
  }
}

// Document Camera Component
export const DocumentCamera: React.FC<{
  onCapture: (image: CapturedImage) => void
  onClose: () => void
}> = ({ onCapture, onClose }) => {
  const videoRef = useRef<HTMLVideoElement>(null)
  const [cameraService] = useState(() => new CameraService())
  const [isInitialized, setIsInitialized] = useState(false)
  const [detection, setDetection] = useState<DocumentDetectionResult | null>(null)
  const [isCapturing, setIsCapturing] = useState(false)
  
  useEffect(() => {
    let detectionInterval: NodeJS.Timeout
    
    const initCamera = async () => {
      try {
        if (videoRef.current) {
          await cameraService.initializeCamera(videoRef.current)
          setIsInitialized(true)
          
          // Start document detection
          detectionInterval = setInterval(async () => {
            try {
              const result = await cameraService.detectDocumentInFrame()
              setDetection(result)
            } catch (error) {
              console.error('Document detection failed:', error)
            }
          }, 500) // Check every 500ms
        }
      } catch (error) {
        console.error('Camera initialization failed:', error)
        toast.error('Camera access required for document capture')
      }
    }
    
    initCamera()
    
    return () => {
      clearInterval(detectionInterval)
      cameraService.stopCamera()
    }
  }, [cameraService])
  
  const handleCapture = async () => {
    setIsCapturing(true)
    
    try {
      const image = await cameraService.captureImage()
      onCapture(image)
    } catch (error) {
      console.error('Image capture failed:', error)
      toast.error('Failed to capture image. Please try again.')
    } finally {
      setIsCapturing(false)
    }
  }
  
  return (
    <div className="document-camera">
      <div className="camera-header">
        <h3>Capture Document</h3>
        <Button variant="ghost" onClick={onClose}>
          <X />
        </Button>
      </div>
      
      <div className="camera-container">
        <video
          ref={videoRef}
          className="camera-video"
          playsInline
          muted
        />
        
        {/* Document Detection Overlay */}
        {detection && detection.detected && (
          <div
            className="document-detection-overlay"
            style={{
              left: detection.boundingBox.x,
              top: detection.boundingBox.y,
              width: detection.boundingBox.width,
              height: detection.boundingBox.height
            }}
          >
            <div className="detection-corners">
              <div className="corner top-left" />
              <div className="corner top-right" />
              <div className="corner bottom-left" />
              <div className="corner bottom-right" />
            </div>
          </div>
        )}
        
        {/* Capture Guide */}
        <div className="capture-guide">
          <div className="guide-frame" />
          <p>Position your document within the frame</p>
        </div>
      </div>
      
      {/* Controls */}
      <div className="camera-controls">
        <Button
          onClick={handleCapture}
          disabled={!isInitialized || isCapturing || !detection?.detected}
          size="lg"
          className="capture-button"
        >
          {isCapturing ? (
            <Loader2 className="w-6 h-6 animate-spin" />
          ) : (
            <Camera className="w-6 h-6" />
          )}
          {isCapturing ? 'Capturing...' : 'Capture'}
        </Button>
      </div>
      
      {/* Status Messages */}
      <div className="camera-status">
        {!isInitialized && (
          <div className="status-message">
            <Loader2 className="w-4 h-4 animate-spin" />
            Initializing camera...
          </div>
        )}
        
        {detection && !detection.detected && (
          <div className="status-message warning">
            <AlertTriangle className="w-4 h-4" />
            No document detected. Please position your document in the frame.
          </div>
        )}
        
        {detection && detection.detected && detection.quality !== 'good' && (
          <div className="status-message info">
            <Info className="w-4 h-4" />
            {detection.recommendations.join('. ')}
          </div>
        )}
      </div>
    </div>
  )
}
```

### Push Notifications Service
```typescript
// Push Notification Service
class PushNotificationService {
  private registration: ServiceWorkerRegistration | null = null
  private subscription: PushSubscription | null = null
  
  async initialize(): Promise<void> {
    if (!('serviceWorker' in navigator) || !('PushManager' in window)) {
      throw new Error('Push notifications not supported')
    }
    
    this.registration = await navigator.serviceWorker.ready
  }
  
  async requestPermission(): Promise<NotificationPermission> {
    if (!('Notification' in window)) {
      throw new Error('Notifications not supported')
    }
    
    let permission = Notification.permission
    
    if (permission === 'default') {
      permission = await Notification.requestPermission()
    }
    
    return permission
  }
  
  async subscribeToPush(): Promise<PushSubscription> {
    if (!this.registration) {
      await this.initialize()
    }
    
    const permission = await this.requestPermission()
    if (permission !== 'granted') {
      throw new Error('Notification permission not granted')
    }
    
    // Check for existing subscription
    this.subscription = await this.registration!.pushManager.getSubscription()
    
    if (!this.subscription) {
      // Create new subscription
      this.subscription = await this.registration!.pushManager.subscribe({
        userVisibleOnly: true,
        applicationServerKey: urlBase64ToUint8Array(process.env.REACT_APP_VAPID_PUBLIC_KEY!)
      })
    }
    
    // Send subscription to server
    await this.sendSubscriptionToServer(this.subscription)
    
    return this.subscription
  }
  
  private async sendSubscriptionToServer(subscription: PushSubscription): Promise<void> {
    const response = await fetch('/api/notifications/subscribe', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${getAuthToken()}`
      },
      body: JSON.stringify({
        endpoint: subscription.endpoint,
        keys: {
          p256dh: arrayBufferToBase64(subscription.getKey('p256dh')!),
          auth: arrayBufferToBase64(subscription.getKey('auth')!)
        }
      })
    })
    
    if (!response.ok) {
      throw new Error('Failed to register push subscription')
    }
  }
  
  async unsubscribeFromPush(): Promise<void> {
    if (this.subscription) {
      await this.subscription.unsubscribe()
      
      // Notify server
      await fetch('/api/notifications/unsubscribe', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${getAuthToken()}`
        }
      })
      
      this.subscription = null
    }
  }
  
  async sendTestNotification(): Promise<void> {
    await fetch('/api/notifications/test', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${getAuthToken()}`
      }
    })
  }
  
  // Local notifications for immediate alerts
  showLocalNotification(title: string, options: NotificationOptions): void {
    if (Notification.permission === 'granted') {
      const notification = new Notification(title, {
        icon: '/icons/icon-192x192.png',
        badge: '/icons/badge-72x72.png',
        tag: 'trading-alert',
        renotify: true,
        requireInteraction: true,
        ...options
      })
      
      // Auto-close after 10 seconds
      setTimeout(() => notification.close(), 10000)
      
      // Handle click
      notification.onclick = () => {
        window.focus()
        notification.close()
        if (options.data?.url) {
          window.location.href = options.data.url
        }
      }
    }
  }
}

// Notification Settings Component
export const NotificationSettings: React.FC = () => {
  const [permission, setPermission] = useState<NotificationPermission>('default')
  const [isSubscribed, setIsSubscribed] = useState(false)
  const [isLoading, setIsLoading] = useState(false)
  const [settings, setSettings] = useState<NotificationPreferences>({
    priceAlerts: true,
    orderUpdates: true,
    marketNews: false,
    portfolioSummary: true
  })
  
  const notificationService = useMemo(() => new PushNotificationService(), [])
  
  useEffect(() => {
    const checkNotificationStatus = async () => {
      if ('Notification' in window) {
        setPermission(Notification.permission)
        
        if (Notification.permission === 'granted') {
          try {
            await notificationService.initialize()
            const subscription = await notificationService.registration?.pushManager.getSubscription()
            setIsSubscribed(!!subscription)
          } catch (error) {
            console.error('Error checking notification status:', error)
          }
        }
      }
    }
    
    checkNotificationStatus()
  }, [notificationService])
  
  const handleEnableNotifications = async () => {
    setIsLoading(true)
    
    try {
      await notificationService.subscribeToPush()
      setPermission('granted')
      setIsSubscribed(true)
      toast.success('Push notifications enabled!')
    } catch (error) {
      console.error('Failed to enable notifications:', error)
      toast.error('Failed to enable notifications. Please check your browser settings.')
    } finally {
      setIsLoading(false)
    }
  }
  
  const handleDisableNotifications = async () => {
    setIsLoading(true)
    
    try {
      await notificationService.unsubscribeFromPush()
      setIsSubscribed(false)
      toast.success('Push notifications disabled')
    } catch (error) {
      console.error('Failed to disable notifications:', error)
      toast.error('Failed to disable notifications')
    } finally {
      setIsLoading(false)
    }
  }
  
  const handleTestNotification = async () => {
    try {
      await notificationService.sendTestNotification()
      toast.success('Test notification sent!')
    } catch (error) {
      toast.error('Failed to send test notification')
    }
  }
  
  return (
    <div className="notification-settings">
      <div className="settings-section">
        <h3>Push Notifications</h3>
        
        {permission === 'denied' && (
          <div className="permission-denied">
            <AlertTriangle className="w-5 h-5" />
            <p>Notifications are blocked. Please enable them in your browser settings.</p>
          </div>
        )}
        
        {permission !== 'granted' ? (
          <Button
            onClick={handleEnableNotifications}
            disabled={isLoading || permission === 'denied'}
          >
            {isLoading ? (
              <Loader2 className="w-4 h-4 animate-spin" />
            ) : (
              <Bell className="w-4 h-4" />
            )}
            Enable Push Notifications
          </Button>
        ) : (
          <div className="notification-controls">
            <div className="subscription-status">
              <CheckCircle className="w-5 h-5 text-green-500" />
              <span>Push notifications enabled</span>
            </div>
            
            <div className="control-buttons">
              <Button variant="outline" onClick={handleTestNotification}>
                Test Notification
              </Button>
              
              <Button
                variant="outline"
                onClick={handleDisableNotifications}
                disabled={isLoading}
              >
                Disable
              </Button>
            </div>
          </div>
        )}
      </div>
      
      {isSubscribed && (
        <div className="settings-section">
          <h4>Notification Preferences</h4>
          
          <div className="preference-list">
            <div className="preference-item">
              <label>
                <input
                  type="checkbox"
                  checked={settings.priceAlerts}
                  onChange={(e) => setSettings(prev => ({
                    ...prev,
                    priceAlerts: e.target.checked
                  }))}
                />
                Price Alerts
              </label>
              <p>Get notified when your watchlist stocks hit target prices</p>
            </div>
            
            <div className="preference-item">
              <label>
                <input
                  type="checkbox"
                  checked={settings.orderUpdates}
                  onChange={(e) => setSettings(prev => ({
                    ...prev,
                    orderUpdates: e.target.checked
                  }))}
                />
                Order Updates
              </label>
              <p>Notifications for order executions and status changes</p>
            </div>
            
            <div className="preference-item">
              <label>
                <input
                  type="checkbox"
                  checked={settings.marketNews}
                  onChange={(e) => setSettings(prev => ({
                    ...prev,
                    marketNews: e.target.checked
                  }))}
                />
                Market News
              </label>
              <p>Breaking market news and analysis updates</p>
            </div>
            
            <div className="preference-item">
              <label>
                <input
                  type="checkbox"
                  checked={settings.portfolioSummary}
                  onChange={(e) => setSettings(prev => ({
                    ...prev,
                    portfolioSummary: e.target.checked
                  }))}
                />
                Portfolio Summary
              </label>
              <p>Daily portfolio performance summaries</p>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
```

## Acceptance Criteria

### Biometric Authentication
- [ ] **WebAuthn Support**: Fingerprint and Face ID authentication on supported devices
- [ ] **Secure Storage**: Biometric credentials stored securely using platform authenticators
- [ ] **Fallback Options**: Password fallback when biometrics unavailable
- [ ] **Registration Flow**: Easy biometric setup with clear user guidance

### Camera Integration
- [ ] **Document Capture**: High-quality document photography for KYC
- [ ] **Real-time Detection**: Automatic document detection and framing assistance
- [ ] **Image Quality**: Optimized image capture with quality validation
- [ ] **Privacy Compliance**: No image storage without user consent

### Push Notifications
- [ ] **Permission Management**: Proper notification permission handling
- [ ] **Subscription Management**: Enable/disable push notifications easily
- [ ] **Notification Types**: Support for price alerts, order updates, market news
- [ ] **Offline Queuing**: Queue notifications when app is closed

### Device Feature Support
- [ ] **Cross-Platform**: Consistent functionality across iOS and Android browsers
- [ ] **Permission Handling**: Graceful permission request and denial handling
- [ ] **Error Recovery**: Clear error messages and recovery options
- [ ] **Privacy Indicators**: Respect browser privacy indicators and user choice

## Testing Strategy

### Device Compatibility Testing
- iOS Safari biometric authentication testing
- Android Chrome fingerprint authentication testing
- Camera functionality across different devices
- Push notification delivery across platforms

### Permission Testing
- Permission grant/deny scenarios
- Permission revocation handling
- Browser-specific permission flows
- Privacy indicator integration

### Security Testing
- Biometric data security validation
- Camera image handling security
- Push notification payload security
- Credential storage security audit

### User Experience Testing
- Biometric authentication user flow
- Camera capture user guidance
- Notification settings management
- Error state handling and recovery

## Definition of Done
- [ ] Biometric authentication working on iOS and Android browsers
- [ ] Camera integration functional for document capture
- [ ] Push notification system operational with preference management
- [ ] Device permission handling implemented with fallback options
- [ ] Security audit completed for all device integrations
- [ ] Cross-platform testing validated on real devices
- [ ] Privacy compliance verified (no unauthorized data collection)
- [ ] User documentation for device feature setup
- [ ] Error handling and recovery flows tested
- [ ] Performance impact assessment completed

## Story Points: 20

## Dependencies
- Backend WebAuthn server implementation
- Document processing service for KYC validation
- Push notification server setup with VAPID keys
- Certificate authority setup for WebAuthn

## Notes
- Consider progressive enhancement approach for unsupported devices
- Implement fallback authentication methods for all biometric flows
- Regular security audits for device integration features
- Integration with existing KYC workflow and compliance requirements