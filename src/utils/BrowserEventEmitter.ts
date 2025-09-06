// Browser-compatible EventEmitter implementation
export class BrowserEventEmitter {
  private listeners: { [event: string]: Function[] } = {}
  private maxListeners = 10

  setMaxListeners(max: number): this {
    this.maxListeners = max
    return this
  }

  on(event: string, listener: Function): this {
    if (!this.listeners[event]) {
      this.listeners[event] = []
    }
    this.listeners[event].push(listener)
    
    // Warn if too many listeners
    if (this.listeners[event].length > this.maxListeners) {
      console.warn(`MaxListenersExceededWarning: Possible EventEmitter memory leak detected. ${this.listeners[event].length} ${event} listeners added.`)
    }
    
    return this
  }

  once(event: string, listener: Function): this {
    const onceWrapper = (...args: any[]) => {
      this.off(event, onceWrapper)
      listener(...args)
    }
    return this.on(event, onceWrapper)
  }

  off(event: string, listener: Function): this {
    if (this.listeners[event]) {
      this.listeners[event] = this.listeners[event].filter(l => l !== listener)
      if (this.listeners[event].length === 0) {
        delete this.listeners[event]
      }
    }
    return this
  }

  emit(event: string, ...args: any[]): boolean {
    if (this.listeners[event]) {
      // Create a copy to avoid issues if listeners modify the array during iteration
      const listenersToCall = [...this.listeners[event]]
      listenersToCall.forEach(listener => {
        try {
          listener(...args)
        } catch (error) {
          console.error(`Error in event listener for "${event}":`, error)
        }
      })
      return true
    }
    return false
  }

  removeAllListeners(event?: string): this {
    if (event) {
      delete this.listeners[event]
    } else {
      this.listeners = {}
    }
    return this
  }

  listenerCount(event: string): number {
    return this.listeners[event] ? this.listeners[event].length : 0
  }

  eventNames(): string[] {
    return Object.keys(this.listeners)
  }
}

// Default export for convenience
export default BrowserEventEmitter