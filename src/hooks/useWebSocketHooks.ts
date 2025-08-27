import { useState, useEffect, useRef, useMemo, useCallback } from 'react'
import { getWebSocketService, ConnectionStatus } from '../services/WebSocketService'

// Basic WebSocket hook
export const useWebSocket = () => {
  const wsService = useMemo(() => getWebSocketService(), [])
  const [connectionStatus, setConnectionStatus] = useState<ConnectionStatus>(() => 
    wsService.getConnectionStatus()
  )

  useEffect(() => {
    const handleStatusChange = (status: ConnectionStatus) => {
      setConnectionStatus(status)
    }

    wsService.on('connectionStatusChanged', handleStatusChange)

    return () => {
      wsService.off('connectionStatusChanged', handleStatusChange)
    }
  }, [wsService])

  const subscribe = useCallback((channel: string, callback: (data: any) => void) => {
    return wsService.subscribe(channel, callback)
  }, [wsService])

  const reconnect = useCallback(() => {
    wsService.reconnect()
  }, [wsService])

  return {
    subscribe,
    reconnect,
    connectionStatus,
    isConnected: connectionStatus === 'connected',
    service: wsService
  }
}

// WebSocket subscription hook with automatic cleanup
export const useWebSocketSubscription = (
  channel: string | null,
  callback: (data: any) => void,
  deps: React.DependencyList = []
) => {
  const { subscribe, connectionStatus } = useWebSocket()
  const callbackRef = useRef(callback)
  
  // Update callback ref when it changes
  useEffect(() => {
    callbackRef.current = callback
  })

  useEffect(() => {
    if (!channel) return

    const unsubscribe = subscribe(channel, (data: any) => {
      callbackRef.current(data)
    })

    return unsubscribe
  }, [channel, subscribe, ...deps])

  return {
    connectionStatus,
    isConnected: connectionStatus === 'connected'
  }
}