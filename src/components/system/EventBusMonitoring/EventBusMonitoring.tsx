import React, { useState, useEffect, useCallback } from 'react'
import { motion } from 'framer-motion'
import {
  Monitor, Activity, Server, Database, Wifi, HardDrive, Cpu, CheckCircle, XCircle,
  AlertTriangle, BarChart3, TrendingUp, TrendingDown, Clock, Zap, Globe, Shield,
  MessageSquare, RefreshCw, Eye, Settings, Filter, Search, Bell, Play, Pause,
  ArrowUp, ArrowDown, Hash, Target, Gauge, Signal, CloudLightning
} from 'lucide-react'

interface SystemService {
  id: string
  name: string
  type: 'microservice' | 'database' | 'message_queue' | 'cache' | 'gateway' | 'monitoring'
  status: 'healthy' | 'degraded' | 'down' | 'maintenance'
  uptime: string
  responseTime: number
  memoryUsage: number
  cpuUsage: number
  lastCheck: Date
  endpoint?: string
  version: string
  replicas?: number
  queueMetrics?: {
    messagesInQueue: number
    messagesProcessed: number
    errorRate: number
    throughput: number
  }
}

interface SystemEvent {
  id: string
  timestamp: Date
  service: string
  eventType: 'info' | 'warning' | 'error' | 'critical'
  category: 'performance' | 'security' | 'data_sync' | 'user_action' | 'system'
  message: string
  details: Record<string, any>
  resolved: boolean
}

interface EventBusMonitoringProps {
  initialTab?: 'dashboard' | 'services' | 'events' | 'queues' | 'performance'
}

export const EventBusMonitoring: React.FC<EventBusMonitoringProps> = ({ initialTab = 'dashboard' }) => {
  const [activeTab, setActiveTab] = useState<'dashboard' | 'services' | 'events' | 'queues' | 'performance'>(initialTab)
  const [services, setServices] = useState<SystemService[]>([])
  const [events, setEvents] = useState<SystemEvent[]>([])

  useEffect(() => {
    loadMockData()
    const interval = setInterval(loadMockData, 5000) // Refresh every 5 seconds
    return () => clearInterval(interval)
  }, [])

  const loadMockData = useCallback(() => {
    // Mock Services with Real-time Updates
    const mockServices: SystemService[] = [
      {
        id: 'kafka-cluster',
        name: 'Kafka Message Broker',
        type: 'message_queue',
        status: 'healthy',
        uptime: '99.8%',
        responseTime: Math.floor(Math.random() * 50) + 10,
        memoryUsage: Math.floor(Math.random() * 30) + 45,
        cpuUsage: Math.floor(Math.random() * 25) + 20,
        lastCheck: new Date(),
        version: '3.2.0',
        replicas: 3,
        queueMetrics: {
          messagesInQueue: Math.floor(Math.random() * 1000) + 100,
          messagesProcessed: Math.floor(Math.random() * 10000) + 50000,
          errorRate: Math.random() * 2,
          throughput: Math.floor(Math.random() * 500) + 200
        }
      },
      {
        id: 'websocket-gateway',
        name: 'WebSocket Gateway',
        type: 'gateway',
        status: 'healthy',
        uptime: '99.9%',
        responseTime: Math.floor(Math.random() * 30) + 5,
        memoryUsage: Math.floor(Math.random() * 25) + 35,
        cpuUsage: Math.floor(Math.random() * 20) + 15,
        lastCheck: new Date(),
        endpoint: 'wss://api.trademaster.com',
        version: '1.4.2',
        replicas: 2
      },
      {
        id: 'trading-service',
        name: 'Trading Service',
        type: 'microservice',
        status: Math.random() > 0.8 ? 'degraded' : 'healthy',
        uptime: '99.7%',
        responseTime: Math.floor(Math.random() * 100) + 50,
        memoryUsage: Math.floor(Math.random() * 40) + 50,
        cpuUsage: Math.floor(Math.random() * 35) + 30,
        lastCheck: new Date(),
        endpoint: 'https://trading.trademaster.com',
        version: '2.1.3',
        replicas: 4
      },
      {
        id: 'market-data-service',
        name: 'Market Data Service',
        type: 'microservice',
        status: 'healthy',
        uptime: '99.5%',
        responseTime: Math.floor(Math.random() * 80) + 20,
        memoryUsage: Math.floor(Math.random() * 35) + 60,
        cpuUsage: Math.floor(Math.random() * 45) + 40,
        lastCheck: new Date(),
        endpoint: 'https://market.trademaster.com',
        version: '1.8.1',
        replicas: 5
      },
      {
        id: 'redis-cache',
        name: 'Redis Cache Cluster',
        type: 'cache',
        status: 'healthy',
        uptime: '99.95%',
        responseTime: Math.floor(Math.random() * 10) + 1,
        memoryUsage: Math.floor(Math.random() * 30) + 40,
        cpuUsage: Math.floor(Math.random() * 15) + 10,
        lastCheck: new Date(),
        version: '7.0.5'
      }
    ]

    // Mock Recent Events
    const eventTypes = ['info', 'warning', 'error', 'critical'] as const
    const categories = ['performance', 'security', 'data_sync', 'user_action', 'system'] as const
    const mockEvents: SystemEvent[] = Array.from({ length: 20 }, (_, i) => ({
      id: `event-${i + 1}`,
      timestamp: new Date(Date.now() - Math.floor(Math.random() * 24 * 60 * 60 * 1000)),
      service: mockServices[Math.floor(Math.random() * mockServices.length)].name,
      eventType: eventTypes[Math.floor(Math.random() * eventTypes.length)],
      category: categories[Math.floor(Math.random() * categories.length)],
      message: `System event ${i + 1} - ${eventTypes[Math.floor(Math.random() * eventTypes.length)]} level`,
      details: {
        correlationId: `corr-${Math.random().toString(36).substring(7)}`,
        duration: Math.floor(Math.random() * 1000),
        affectedUsers: Math.floor(Math.random() * 100)
      },
      resolved: Math.random() > 0.3
    }))

    setServices(mockServices)
    setEvents(mockEvents)
  }, [])

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-purple-900 to-slate-900 p-6">
      <div className="mb-8">
        <h1 className="text-4xl font-bold bg-gradient-to-r from-purple-400 to-cyan-400 bg-clip-text text-transparent mb-2">
          Event Bus & System Monitoring
        </h1>
        <p className="text-slate-400 text-lg">
          Real-time system monitoring and operational visibility
        </p>
      </div>

      {/* Navigation Tabs */}
      <div className="mb-8">
        <div className="flex flex-wrap gap-2 p-1 bg-slate-800/50 rounded-xl border border-slate-700/50">
          {[
            { id: 'dashboard', label: 'System Dashboard', icon: Monitor },
            { id: 'services', label: 'Service Health', icon: Server },
            { id: 'events', label: 'Event Stream', icon: Activity },
            { id: 'queues', label: 'Message Queues', icon: MessageSquare },
            { id: 'performance', label: 'Performance', icon: BarChart3 }
          ].map(tab => {
            const Icon = tab.icon
            return (
              <motion.button
                key={tab.id}
                whileHover={{ scale: 1.02 }}
                whileTap={{ scale: 0.98 }}
                onClick={() => setActiveTab(tab.id as any)}
                className={`flex items-center space-x-2 px-4 py-3 rounded-lg font-medium transition-all duration-200 ${
                  activeTab === tab.id
                    ? 'bg-gradient-to-r from-purple-500 to-cyan-500 text-white shadow-lg'
                    : 'text-slate-400 hover:text-white hover:bg-slate-700/50'
                }`}
              >
                <Icon className="w-4 h-4" />
                <span className="whitespace-nowrap">{tab.label}</span>
              </motion.button>
            )
          })}
        </div>
      </div>

      {/* Content */}
      <div className="space-y-6">
        {activeTab === 'dashboard' && <DashboardTab services={services} events={events} />}
        {activeTab === 'services' && <ServicesTab services={services} />}
        {activeTab === 'events' && <EventsTab events={events} />}
        {activeTab === 'queues' && <QueuesTab services={services} />}
        {activeTab === 'performance' && <PerformanceTab services={services} />}
      </div>
    </div>
  )
}

const DashboardTab: React.FC<any> = ({ services, events }) => {
  const healthyServices = services.filter((s: any) => s.status === 'healthy').length
  const criticalEvents = events.filter((e: any) => e.eventType === 'critical' && !e.resolved).length
  const avgResponseTime = Math.round(services.reduce((sum: number, s: any) => sum + s.responseTime, 0) / services.length)
  
  return (
    <div className="space-y-6">
      {/* Key Metrics */}
      <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-4">
        <div className="glass-card p-6 rounded-2xl">
          <div className="flex items-center justify-between mb-4">
            <div className="p-3 rounded-xl bg-gradient-to-br from-green-500/20 to-green-600/20">
              <CheckCircle className="h-6 w-6 text-green-400" />
            </div>
            <div className="text-right">
              <div className="text-2xl font-bold text-white">{healthyServices}/{services.length}</div>
            </div>
          </div>
          <h3 className="text-green-400 font-semibold mb-1">Healthy Services</h3>
          <p className="text-slate-400 text-sm">system availability</p>
        </div>

        <div className="glass-card p-6 rounded-2xl">
          <div className="flex items-center justify-between mb-4">
            <div className="p-3 rounded-xl bg-gradient-to-br from-blue-500/20 to-blue-600/20">
              <Zap className="h-6 w-6 text-blue-400" />
            </div>
            <div className="text-right">
              <div className="text-2xl font-bold text-white">{avgResponseTime}ms</div>
            </div>
          </div>
          <h3 className="text-blue-400 font-semibold mb-1">Avg Response Time</h3>
          <p className="text-slate-400 text-sm">system performance</p>
        </div>

        <div className="glass-card p-6 rounded-2xl">
          <div className="flex items-center justify-between mb-4">
            <div className="p-3 rounded-xl bg-gradient-to-br from-red-500/20 to-red-600/20">
              <AlertTriangle className="h-6 w-6 text-red-400" />
            </div>
            <div className="text-right">
              <div className="text-2xl font-bold text-white">{criticalEvents}</div>
            </div>
          </div>
          <h3 className="text-red-400 font-semibold mb-1">Critical Alerts</h3>
          <p className="text-slate-400 text-sm">requires attention</p>
        </div>

        <div className="glass-card p-6 rounded-2xl">
          <div className="flex items-center justify-between mb-4">
            <div className="p-3 rounded-xl bg-gradient-to-br from-purple-500/20 to-purple-600/20">
              <Activity className="h-6 w-6 text-purple-400" />
            </div>
            <div className="text-right">
              <div className="text-2xl font-bold text-white">{events.length}</div>
            </div>
          </div>
          <h3 className="text-purple-400 font-semibold mb-1">Recent Events</h3>
          <p className="text-slate-400 text-sm">last 24 hours</p>
        </div>
      </div>

      {/* System Status Grid */}
      <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
        {services.map((service: any, index: number) => (
          <motion.div
            key={service.id}
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ delay: index * 0.1 }}
            className="glass-card p-6 rounded-2xl"
          >
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center space-x-3">
                <div className={`w-3 h-3 rounded-full ${
                  service.status === 'healthy' ? 'bg-green-400' :
                  service.status === 'degraded' ? 'bg-yellow-400' :
                  service.status === 'down' ? 'bg-red-400' :
                  'bg-blue-400'
                }`} />
                <h3 className="text-white font-semibold">{service.name}</h3>
              </div>
              <span className={`px-2 py-1 rounded text-xs font-medium ${
                service.status === 'healthy' ? 'bg-green-500/20 text-green-400' :
                service.status === 'degraded' ? 'bg-yellow-500/20 text-yellow-400' :
                service.status === 'down' ? 'bg-red-500/20 text-red-400' :
                'bg-blue-500/20 text-blue-400'
              }`}>
                {service.status}
              </span>
            </div>

            <div className="space-y-3">
              <div className="flex justify-between items-center">
                <span className="text-slate-400">Uptime</span>
                <span className="text-white font-medium">{service.uptime}</span>
              </div>

              <div className="flex justify-between items-center">
                <span className="text-slate-400">Response Time</span>
                <span className={`font-medium ${
                  service.responseTime < 50 ? 'text-green-400' :
                  service.responseTime < 200 ? 'text-yellow-400' :
                  'text-red-400'
                }`}>
                  {service.responseTime}ms
                </span>
              </div>

              {service.queueMetrics && (
                <div className="flex justify-between items-center">
                  <span className="text-slate-400">Queue Size</span>
                  <span className="text-white font-medium">{service.queueMetrics.messagesInQueue}</span>
                </div>
              )}
            </div>
          </motion.div>
        ))}
      </div>
    </div>
  )
}

const ServicesTab: React.FC<any> = ({ services }) => (
  <div className="glass-card rounded-2xl p-6">
    <h3 className="text-xl font-bold text-white mb-6">Service Health Details</h3>
    <p className="text-slate-400">Detailed service monitoring interface coming soon...</p>
  </div>
)

const EventsTab: React.FC<any> = ({ events }) => (
  <div className="glass-card rounded-2xl p-6">
    <h3 className="text-xl font-bold text-white mb-6">Event Stream ({events.length} events)</h3>
    <p className="text-slate-400">Real-time event monitoring interface coming soon...</p>
  </div>
)

const QueuesTab: React.FC<any> = ({ services }) => (
  <div className="glass-card rounded-2xl p-6">
    <h3 className="text-xl font-bold text-white mb-6">Message Queue Monitoring</h3>
    <p className="text-slate-400">Message queue monitoring interface coming soon...</p>
  </div>
)

const PerformanceTab: React.FC<any> = ({ services }) => (
  <div className="glass-card rounded-2xl p-6">
    <h3 className="text-xl font-bold text-white mb-6">Performance Analytics</h3>
    <p className="text-slate-400">Performance analytics interface coming soon...</p>
  </div>
)

export default EventBusMonitoring