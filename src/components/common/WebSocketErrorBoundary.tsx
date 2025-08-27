import React, { Component, ReactNode, ErrorInfo } from 'react'
import { motion } from 'framer-motion'
import { AlertTriangle, RefreshCw, Wifi, Settings } from 'lucide-react'
import { cn } from '../../lib/utils'

interface Props {
  children: ReactNode
  fallback?: ReactNode
  onError?: (error: Error, errorInfo: ErrorInfo) => void
  enableFallback?: boolean
}

interface State {
  hasError: boolean
  error: Error | null
  errorInfo: ErrorInfo | null
  retryCount: number
}

export class WebSocketErrorBoundary extends Component<Props, State> {
  private retryTimeout: NodeJS.Timeout | null = null
  private maxRetries = 3

  constructor(props: Props) {
    super(props)
    this.state = { 
      hasError: false, 
      error: null, 
      errorInfo: null,
      retryCount: 0 
    }
  }

  static getDerivedStateFromError(error: Error): Partial<State> {
    return { hasError: true, error }
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('WebSocket Error Boundary caught an error:', error, errorInfo)
    
    this.setState({ errorInfo })
    
    // Call custom error handler if provided
    if (this.props.onError) {
      this.props.onError(error, errorInfo)
    }

    // Log to error reporting service
    this.logError(error, errorInfo)
  }

  private logError = (error: Error, errorInfo: ErrorInfo) => {
    // In a real application, you would send this to your error reporting service
    const errorReport = {
      message: error.message,
      stack: error.stack,
      componentStack: errorInfo.componentStack,
      context: 'WebSocket',
      timestamp: new Date().toISOString(),
      userAgent: navigator.userAgent,
      url: window.location.href
    }

    console.error('Error Report:', errorReport)
    
    // Example: Send to error reporting service
    // errorReportingService.reportError(errorReport)
  }

  private handleRetry = () => {
    if (this.state.retryCount < this.maxRetries) {
      this.setState(prevState => ({ 
        retryCount: prevState.retryCount + 1,
        hasError: false,
        error: null,
        errorInfo: null
      }))
    }
  }

  private handleRefresh = () => {
    window.location.reload()
  }

  componentWillUnmount() {
    if (this.retryTimeout) {
      clearTimeout(this.retryTimeout)
    }
  }

  render() {
    if (this.state.hasError) {
      // Use custom fallback if provided
      if (this.props.fallback) {
        return this.props.fallback
      }

      // Default error UI
      return (
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="glass-card p-8 rounded-2xl border border-red-500/30 bg-red-500/5"
        >
          <div className="text-center space-y-6">
            <div className="flex justify-center">
              <div className="p-4 rounded-full bg-red-500/20">
                <AlertTriangle className="w-8 h-8 text-red-400" />
              </div>
            </div>

            <div>
              <h3 className="text-xl font-bold text-white mb-2">
                Real-time Connection Error
              </h3>
              <p className="text-slate-400 max-w-md mx-auto">
                There was a problem with the real-time data connection. 
                Your trading functionality may be limited.
              </p>
            </div>

            {/* Error details (development only) */}
            {import.meta.env.DEV && this.state.error && (
              <details className="text-left max-w-2xl mx-auto">
                <summary className="cursor-pointer text-sm text-slate-400 hover:text-slate-300">
                  Error Details
                </summary>
                <div className="mt-2 p-3 bg-slate-900/50 rounded-lg text-xs font-mono text-slate-300 overflow-auto">
                  <div className="text-red-400 mb-2">{this.state.error.message}</div>
                  {this.state.error.stack && (
                    <pre className="whitespace-pre-wrap">{this.state.error.stack}</pre>
                  )}
                </div>
              </details>
            )}

            {/* Action buttons */}
            <div className="flex justify-center space-x-4">
              {this.state.retryCount < this.maxRetries && (
                <button
                  onClick={this.handleRetry}
                  className="cyber-button px-6 py-3 rounded-xl flex items-center space-x-2"
                >
                  <RefreshCw className="w-4 h-4" />
                  <span>Retry Connection</span>
                </button>
              )}

              <button
                onClick={this.handleRefresh}
                className="glass-card px-6 py-3 rounded-xl text-slate-400 hover:text-white transition-colors flex items-center space-x-2"
              >
                <Settings className="w-4 h-4" />
                <span>Refresh Page</span>
              </button>
            </div>

            {/* Retry count indicator */}
            {this.state.retryCount > 0 && (
              <div className="text-sm text-slate-500">
                Retry attempt: {this.state.retryCount}/{this.maxRetries}
              </div>
            )}
          </div>
        </motion.div>
      )
    }

    return this.props.children
  }
}

// Fallback component for WebSocket connection issues
export const WebSocketFallback: React.FC<{
  onRetry?: () => void
  isRetrying?: boolean
}> = ({ onRetry, isRetrying = false }) => {
  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.95 }}
      animate={{ opacity: 1, scale: 1 }}
      className="glass-card p-6 rounded-xl border border-yellow-500/30 bg-yellow-500/5"
    >
      <div className="flex items-start space-x-4">
        <div className="p-2 rounded-lg bg-yellow-500/20">
          <Wifi className="w-5 h-5 text-yellow-400" />
        </div>
        
        <div className="flex-1">
          <h4 className="text-white font-medium mb-1">
            Using Cached Data
          </h4>
          <p className="text-sm text-slate-400 mb-3">
            Real-time updates are temporarily unavailable. Showing last known data.
          </p>
          
          {onRetry && (
            <button
              onClick={onRetry}
              disabled={isRetrying}
              className="text-yellow-400 hover:text-yellow-300 text-sm flex items-center space-x-1 disabled:opacity-50"
            >
              <RefreshCw className={cn("w-3 h-3", isRetrying && "animate-spin")} />
              <span>{isRetrying ? 'Reconnecting...' : 'Reconnect'}</span>
            </button>
          )}
        </div>
      </div>
    </motion.div>
  )
}

// Higher-order component for WebSocket error handling
export const withWebSocketErrorBoundary = <P extends object>(
  WrappedComponent: React.ComponentType<P>,
  fallback?: ReactNode
) => {
  const WithErrorBoundary = (props: P) => (
    <WebSocketErrorBoundary fallback={fallback}>
      <WrappedComponent {...props} />
    </WebSocketErrorBoundary>
  )

  WithErrorBoundary.displayName = `withWebSocketErrorBoundary(${
    WrappedComponent.displayName || WrappedComponent.name
  })`

  return WithErrorBoundary
}