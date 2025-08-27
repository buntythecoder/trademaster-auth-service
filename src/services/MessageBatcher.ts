import React, { useEffect, useMemo, useRef, useCallback, useState } from 'react'

// Message batching utility for reducing render frequency
export class MessageBatcher {
  private batches = new Map<string, any[]>()
  private timeouts = new Map<string, NodeJS.Timeout>()
  private callbacks = new Map<string, (batch: any[]) => void>()
  private readonly batchDelay: number

  constructor(batchDelay: number = 50) {
    this.batchDelay = batchDelay
  }

  public addMessage(channel: string, data: any, callback: (batch: any[]) => void): void {
    // Initialize batch array for channel
    if (!this.batches.has(channel)) {
      this.batches.set(channel, [])
    }

    // Add message to batch
    this.batches.get(channel)!.push(data)
    this.callbacks.set(channel, callback)

    // Clear existing timeout
    const existingTimeout = this.timeouts.get(channel)
    if (existingTimeout) {
      clearTimeout(existingTimeout)
    }

    // Set new timeout for batch processing
    const timeout = setTimeout(() => {
      this.processBatch(channel)
    }, this.batchDelay)

    this.timeouts.set(channel, timeout)
  }

  private processBatch(channel: string): void {
    const batch = this.batches.get(channel)
    const callback = this.callbacks.get(channel)

    if (batch && batch.length > 0 && callback) {
      try {
        callback([...batch]) // Send copy of batch
      } catch (error) {
        console.error(`Error processing batch for channel ${channel}:`, error)
      }

      // Clean up
      this.batches.delete(channel)
      this.timeouts.delete(channel)
      this.callbacks.delete(channel)
    }
  }

  public flush(channel?: string): void {
    if (channel) {
      this.processBatch(channel)
    } else {
      // Flush all channels
      this.batches.forEach((_, ch) => this.processBatch(ch))
    }
  }

  public clear(): void {
    // Clear all timeouts
    this.timeouts.forEach(timeout => clearTimeout(timeout))
    
    // Clear all maps
    this.batches.clear()
    this.timeouts.clear()
    this.callbacks.clear()
  }

  public getStats(): { channels: number; pendingMessages: number } {
    let pendingMessages = 0
    this.batches.forEach(batch => {
      pendingMessages += batch.length
    })

    return {
      channels: this.batches.size,
      pendingMessages
    }
  }
}

// React hook for message batching
export const useMessageBatcher = (batchDelay: number = 50) => {
  const batcher = useMemo(() => new MessageBatcher(batchDelay), [batchDelay])

  useEffect(() => {
    return () => {
      batcher.clear()
    }
  }, [batcher])

  return batcher
}

// Throttling utility for high-frequency updates
export class UpdateThrottler {
  private lastUpdate = new Map<string, number>()
  private pendingUpdates = new Map<string, any>()
  private callbacks = new Map<string, (data: any) => void>()
  private readonly throttleDelay: number

  constructor(throttleDelay: number = 100) {
    this.throttleDelay = throttleDelay
  }

  public throttleUpdate(key: string, data: any, callback: (data: any) => void): void {
    const now = Date.now()
    const lastTime = this.lastUpdate.get(key) || 0
    const timeDiff = now - lastTime

    this.pendingUpdates.set(key, data)
    this.callbacks.set(key, callback)

    if (timeDiff >= this.throttleDelay) {
      this.executeUpdate(key)
    } else {
      // Schedule update for later
      const delay = this.throttleDelay - timeDiff
      setTimeout(() => {
        if (this.pendingUpdates.has(key)) {
          this.executeUpdate(key)
        }
      }, delay)
    }
  }

  private executeUpdate(key: string): void {
    const data = this.pendingUpdates.get(key)
    const callback = this.callbacks.get(key)

    if (data && callback) {
      try {
        callback(data)
        this.lastUpdate.set(key, Date.now())
      } catch (error) {
        console.error(`Error executing throttled update for ${key}:`, error)
      }

      // Clean up
      this.pendingUpdates.delete(key)
      this.callbacks.delete(key)
    }
  }

  public flush(key?: string): void {
    if (key) {
      this.executeUpdate(key)
    } else {
      // Flush all pending updates
      this.pendingUpdates.forEach((_, k) => this.executeUpdate(k))
    }
  }

  public clear(): void {
    this.lastUpdate.clear()
    this.pendingUpdates.clear()
    this.callbacks.clear()
  }
}

// React hook for throttling updates
export const useUpdateThrottler = (throttleDelay: number = 100) => {
  const throttler = useMemo(() => new UpdateThrottler(throttleDelay), [throttleDelay])

  useEffect(() => {
    return () => {
      throttler.clear()
    }
  }, [throttler])

  return throttler
}

// Debouncing utility for search and input fields
export const useDebounce = <T>(value: T, delay: number): T => {
  const [debouncedValue, setDebouncedValue] = React.useState<T>(value)

  useEffect(() => {
    const handler = setTimeout(() => {
      setDebouncedValue(value)
    }, delay)

    return () => {
      clearTimeout(handler)
    }
  }, [value, delay])

  return debouncedValue
}

// Performance monitoring utility
export class PerformanceMonitor {
  private metrics = new Map<string, {
    count: number
    totalTime: number
    minTime: number
    maxTime: number
    lastTime: number
  }>()

  public startTimer(name: string): () => number {
    const startTime = performance.now()

    return () => {
      const endTime = performance.now()
      const duration = endTime - startTime
      this.recordMetric(name, duration)
      return duration
    }
  }

  private recordMetric(name: string, duration: number): void {
    const existing = this.metrics.get(name)

    if (existing) {
      existing.count++
      existing.totalTime += duration
      existing.minTime = Math.min(existing.minTime, duration)
      existing.maxTime = Math.max(existing.maxTime, duration)
      existing.lastTime = duration
    } else {
      this.metrics.set(name, {
        count: 1,
        totalTime: duration,
        minTime: duration,
        maxTime: duration,
        lastTime: duration
      })
    }
  }

  public getMetrics(name: string) {
    const metric = this.metrics.get(name)
    if (!metric) return null

    return {
      count: metric.count,
      average: metric.totalTime / metric.count,
      min: metric.minTime,
      max: metric.maxTime,
      last: metric.lastTime,
      total: metric.totalTime
    }
  }

  public getAllMetrics() {
    const result: Record<string, any> = {}
    this.metrics.forEach((metric, name) => {
      result[name] = this.getMetrics(name)
    })
    return result
  }

  public clear(name?: string): void {
    if (name) {
      this.metrics.delete(name)
    } else {
      this.metrics.clear()
    }
  }
}

// React hook for performance monitoring
export const usePerformanceMonitor = () => {
  const monitor = useMemo(() => new PerformanceMonitor(), [])

  const measureTime = useCallback((name: string, fn: () => void) => {
    const endTimer = monitor.startTimer(name)
    fn()
    return endTimer()
  }, [monitor])

  const measureAsyncTime = useCallback(async (name: string, fn: () => Promise<void>) => {
    const endTimer = monitor.startTimer(name)
    await fn()
    return endTimer()
  }, [monitor])

  return {
    monitor,
    measureTime,
    measureAsyncTime,
    getMetrics: monitor.getMetrics.bind(monitor),
    getAllMetrics: monitor.getAllMetrics.bind(monitor)
  }
}

// Memory usage monitoring
export const useMemoryMonitor = () => {
  const [memoryUsage, setMemoryUsage] = useState<{
    used: number
    total: number
    percentage: number
  } | null>(null)

  useEffect(() => {
    const checkMemory = () => {
      if ('memory' in performance) {
        const memory = (performance as any).memory
        setMemoryUsage({
          used: memory.usedJSHeapSize,
          total: memory.totalJSHeapSize,
          percentage: (memory.usedJSHeapSize / memory.totalJSHeapSize) * 100
        })
      }
    }

    checkMemory()
    const interval = setInterval(checkMemory, 5000) // Check every 5 seconds

    return () => clearInterval(interval)
  }, [])

  return memoryUsage
}