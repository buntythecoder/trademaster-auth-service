import React, { useState, useEffect } from 'react'
import { motion } from 'framer-motion'
import {
  Brain, Cpu, BarChart3, TrendingUp, Settings, Eye, Zap, Target,
  Activity, Database, Cloud, Shield, CheckCircle, AlertTriangle
} from 'lucide-react'

interface AIIntegrationManagementProps {
  initialTab?: 'dashboard' | 'models' | 'pipelines' | 'monitoring'
}

export const AIIntegrationManagement: React.FC<AIIntegrationManagementProps> = ({ initialTab = 'dashboard' }) => {
  const [activeTab, setActiveTab] = useState<'dashboard' | 'models' | 'pipelines' | 'monitoring'>(initialTab)

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-purple-900 to-slate-900 p-6">
      <div className="mb-8">
        <h1 className="text-4xl font-bold bg-gradient-to-r from-purple-400 to-cyan-400 bg-clip-text text-transparent mb-2">
          AI & ML Integration Management
        </h1>
        <p className="text-slate-400 text-lg">
          Machine learning model configuration and behavioral AI management
        </p>
      </div>

      {/* Navigation Tabs */}
      <div className="mb-8">
        <div className="flex flex-wrap gap-2 p-1 bg-slate-800/50 rounded-xl border border-slate-700/50">
          {[
            { id: 'dashboard', label: 'AI Dashboard', icon: Brain },
            { id: 'models', label: 'ML Models', icon: Cpu },
            { id: 'pipelines', label: 'Data Pipelines', icon: Activity },
            { id: 'monitoring', label: 'Model Monitoring', icon: BarChart3 }
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
        {activeTab === 'dashboard' && <AIDashboardTab />}
        {activeTab === 'models' && <ModelsTab />}
        {activeTab === 'pipelines' && <PipelinesTab />}
        {activeTab === 'monitoring' && <MonitoringTab />}
      </div>
    </div>
  )
}

const AIDashboardTab = () => (
  <div className="space-y-6">
    <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-4">
      <div className="glass-card p-6 rounded-2xl">
        <div className="flex items-center justify-between mb-4">
          <div className="p-3 rounded-xl bg-gradient-to-br from-purple-500/20 to-purple-600/20">
            <Brain className="h-6 w-6 text-purple-400" />
          </div>
          <div className="text-right">
            <div className="text-2xl font-bold text-white">12</div>
          </div>
        </div>
        <h3 className="text-purple-400 font-semibold mb-1">Active Models</h3>
        <p className="text-slate-400 text-sm">behavioral AI systems</p>
      </div>

      <div className="glass-card p-6 rounded-2xl">
        <div className="flex items-center justify-between mb-4">
          <div className="p-3 rounded-xl bg-gradient-to-br from-green-500/20 to-green-600/20">
            <TrendingUp className="h-6 w-6 text-green-400" />
          </div>
          <div className="text-right">
            <div className="text-2xl font-bold text-white">94.2%</div>
          </div>
        </div>
        <h3 className="text-green-400 font-semibold mb-1">Model Accuracy</h3>
        <p className="text-slate-400 text-sm">average performance</p>
      </div>

      <div className="glass-card p-6 rounded-2xl">
        <div className="flex items-center justify-between mb-4">
          <div className="p-3 rounded-xl bg-gradient-to-br from-blue-500/20 to-blue-600/20">
            <Activity className="h-6 w-6 text-blue-400" />
          </div>
          <div className="text-right">
            <div className="text-2xl font-bold text-white">2.4M</div>
          </div>
        </div>
        <h3 className="text-blue-400 font-semibold mb-1">Predictions Today</h3>
        <p className="text-slate-400 text-sm">trading insights</p>
      </div>

      <div className="glass-card p-6 rounded-2xl">
        <div className="flex items-center justify-between mb-4">
          <div className="p-3 rounded-xl bg-gradient-to-br from-cyan-500/20 to-cyan-600/20">
            <Database className="h-6 w-6 text-cyan-400" />
          </div>
          <div className="text-right">
            <div className="text-2xl font-bold text-white">847GB</div>
          </div>
        </div>
        <h3 className="text-cyan-400 font-semibold mb-1">Training Data</h3>
        <p className="text-slate-400 text-sm">processed daily</p>
      </div>
    </div>

    <div className="glass-card p-6 rounded-2xl">
      <h3 className="text-xl font-bold text-white mb-6">Active AI Models</h3>
      <div className="space-y-4">
        {[
          { name: 'Behavioral Analysis Engine', type: 'Deep Learning', status: 'active', accuracy: '94.2%', uptime: '99.8%' },
          { name: 'Market Sentiment Analyzer', type: 'NLP', status: 'active', accuracy: '89.7%', uptime: '99.5%' },
          { name: 'Risk Assessment Model', type: 'Ensemble', status: 'training', accuracy: '92.1%', uptime: '98.9%' },
          { name: 'Price Prediction Engine', type: 'Time Series', status: 'active', accuracy: '87.4%', uptime: '99.2%' },
          { name: 'Fraud Detection System', type: 'Anomaly Detection', status: 'active', accuracy: '96.8%', uptime: '99.9%' }
        ].map((model) => (
          <div key={model.name} className="flex items-center justify-between p-4 bg-slate-800/30 rounded-xl">
            <div>
              <div className="text-white font-semibold">{model.name}</div>
              <div className="text-slate-400 text-sm">{model.type}</div>
            </div>
            <div className="text-right">
              <div className={`text-sm font-medium mb-1 ${
                model.status === 'active' ? 'text-green-400' : 
                model.status === 'training' ? 'text-yellow-400' : 'text-red-400'
              }`}>
                {model.status.toUpperCase()}
              </div>
              <div className="text-slate-400 text-sm">Accuracy: {model.accuracy}</div>
            </div>
          </div>
        ))}
      </div>
    </div>
  </div>
)

const ModelsTab = () => (
  <div className="space-y-6">
    <div className="glass-card rounded-2xl p-6">
      <h3 className="text-xl font-bold text-white mb-6">ML Model Configuration</h3>
      <div className="space-y-4">
        {[
          { name: 'Behavioral Analysis Engine', version: 'v2.1.0', lastTrained: '2 hours ago', nextTraining: '6 hours', dataSize: '2.4TB' },
          { name: 'Market Sentiment Analyzer', version: 'v1.8.3', lastTrained: '4 hours ago', nextTraining: '8 hours', dataSize: '847GB' },
          { name: 'Risk Assessment Model', version: 'v3.0.1', lastTrained: '1 hour ago', nextTraining: '12 hours', dataSize: '1.2TB' },
          { name: 'Price Prediction Engine', version: 'v1.5.2', lastTrained: '6 hours ago', nextTraining: '18 hours', dataSize: '3.1TB' }
        ].map((model) => (
          <div key={model.name} className="p-4 bg-slate-800/30 rounded-xl">
            <div className="flex items-center justify-between mb-3">
              <div>
                <div className="text-white font-semibold">{model.name}</div>
                <div className="text-slate-400 text-sm">Version {model.version}</div>
              </div>
              <div className="flex space-x-2">
                <button className="px-3 py-1 bg-blue-500/20 text-blue-400 border border-blue-500/30 rounded text-sm hover:bg-blue-500/30 transition-colors">
                  Retrain
                </button>
                <button className="px-3 py-1 bg-purple-500/20 text-purple-400 border border-purple-500/30 rounded text-sm hover:bg-purple-500/30 transition-colors">
                  Deploy
                </button>
              </div>
            </div>
            <div className="grid grid-cols-3 gap-4 text-sm">
              <div>
                <div className="text-slate-400">Last Trained</div>
                <div className="text-white">{model.lastTrained}</div>
              </div>
              <div>
                <div className="text-slate-400">Next Training</div>
                <div className="text-white">{model.nextTraining}</div>
              </div>
              <div>
                <div className="text-slate-400">Training Data</div>
                <div className="text-white">{model.dataSize}</div>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>

    <div className="glass-card rounded-2xl p-6">
      <h3 className="text-xl font-bold text-white mb-6">Model Performance Metrics</h3>
      <div className="grid gap-4 md:grid-cols-2">
        <div className="p-4 bg-slate-800/30 rounded-xl">
          <div className="text-2xl font-bold text-green-400 mb-2">94.2%</div>
          <div className="text-slate-400">Average Accuracy</div>
        </div>
        <div className="p-4 bg-slate-800/30 rounded-xl">
          <div className="text-2xl font-bold text-blue-400 mb-2">2.4M</div>
          <div className="text-slate-400">Daily Predictions</div>
        </div>
        <div className="p-4 bg-slate-800/30 rounded-xl">
          <div className="text-2xl font-bold text-purple-400 mb-2">23ms</div>
          <div className="text-slate-400">Avg Response Time</div>
        </div>
        <div className="p-4 bg-slate-800/30 rounded-xl">
          <div className="text-2xl font-bold text-cyan-400 mb-2">99.7%</div>
          <div className="text-slate-400">Model Uptime</div>
        </div>
      </div>
    </div>
  </div>
)

const PipelinesTab = () => (
  <div className="space-y-6">
    <div className="glass-card rounded-2xl p-6">
      <h3 className="text-xl font-bold text-white mb-6">Data Processing Pipelines</h3>
      <div className="space-y-4">
        {[
          { name: 'Market Data Ingestion', status: 'running', throughput: '2.4M records/hour', latency: '45ms', lastRun: '2 minutes ago' },
          { name: 'User Behavior Analytics', status: 'running', throughput: '847K events/hour', latency: '12ms', lastRun: '5 minutes ago' },
          { name: 'Risk Calculation Pipeline', status: 'running', throughput: '1.2M calculations/hour', latency: '23ms', lastRun: '3 minutes ago' },
          { name: 'Model Training Data Prep', status: 'completed', throughput: '3.1TB processed', latency: '2.4s', lastRun: '2 hours ago' },
          { name: 'Real-time Feature Engineering', status: 'running', throughput: '5.6M features/hour', latency: '8ms', lastRun: '1 minute ago' }
        ].map((pipeline) => (
          <div key={pipeline.name} className="flex items-center justify-between p-4 bg-slate-800/30 rounded-xl">
            <div>
              <div className="text-white font-semibold">{pipeline.name}</div>
              <div className="text-slate-400 text-sm">Throughput: {pipeline.throughput}</div>
            </div>
            <div className="text-right">
              <div className={`text-sm font-medium mb-1 ${
                pipeline.status === 'running' ? 'text-green-400' : 
                pipeline.status === 'completed' ? 'text-blue-400' : 'text-red-400'
              }`}>
                {pipeline.status.toUpperCase()}
              </div>
              <div className="text-slate-400 text-sm">Latency: {pipeline.latency}</div>
              <div className="text-slate-400 text-sm">Last run: {pipeline.lastRun}</div>
            </div>
          </div>
        ))}
      </div>
    </div>

    <div className="glass-card rounded-2xl p-6">
      <h3 className="text-xl font-bold text-white mb-6">Pipeline Health Metrics</h3>
      <div className="grid gap-4 md:grid-cols-4">
        <div className="p-4 bg-slate-800/30 rounded-xl text-center">
          <div className="text-2xl font-bold text-green-400 mb-2">99.8%</div>
          <div className="text-slate-400">Success Rate</div>
        </div>
        <div className="p-4 bg-slate-800/30 rounded-xl text-center">
          <div className="text-2xl font-bold text-blue-400 mb-2">18ms</div>
          <div className="text-slate-400">Avg Latency</div>
        </div>
        <div className="p-4 bg-slate-800/30 rounded-xl text-center">
          <div className="text-2xl font-bold text-purple-400 mb-2">847GB</div>
          <div className="text-slate-400">Daily Volume</div>
        </div>
        <div className="p-4 bg-slate-800/30 rounded-xl text-center">
          <div className="text-2xl font-bold text-cyan-400 mb-2">5</div>
          <div className="text-slate-400">Active Pipelines</div>
        </div>
      </div>
    </div>
  </div>
)

const MonitoringTab = () => (
  <div className="space-y-6">
    <div className="glass-card rounded-2xl p-6">
      <h3 className="text-xl font-bold text-white mb-6">Model Performance Monitoring</h3>
      <div className="space-y-4">
        {[
          { model: 'Behavioral Analysis Engine', accuracy: '94.2%', drift: 'Low', alerts: 0, lastCheck: '5 minutes ago' },
          { model: 'Market Sentiment Analyzer', accuracy: '89.7%', drift: 'Medium', alerts: 1, lastCheck: '10 minutes ago' },
          { model: 'Risk Assessment Model', accuracy: '92.1%', drift: 'Low', alerts: 0, lastCheck: '15 minutes ago' },
          { model: 'Price Prediction Engine', accuracy: '87.4%', drift: 'High', alerts: 2, lastCheck: '20 minutes ago' },
          { model: 'Fraud Detection System', accuracy: '96.8%', drift: 'Low', alerts: 0, lastCheck: '8 minutes ago' }
        ].map((model) => (
          <div key={model.model} className="flex items-center justify-between p-4 bg-slate-800/30 rounded-xl">
            <div>
              <div className="text-white font-semibold">{model.model}</div>
              <div className="text-slate-400 text-sm">Last checked: {model.lastCheck}</div>
            </div>
            <div className="flex items-center space-x-6 text-sm">
              <div className="text-center">
                <div className="text-green-400 font-semibold">{model.accuracy}</div>
                <div className="text-slate-400">Accuracy</div>
              </div>
              <div className="text-center">
                <div className={`font-semibold ${
                  model.drift === 'Low' ? 'text-green-400' : 
                  model.drift === 'Medium' ? 'text-yellow-400' : 'text-red-400'
                }`}>
                  {model.drift}
                </div>
                <div className="text-slate-400">Drift</div>
              </div>
              <div className="text-center">
                <div className={`font-semibold ${
                  model.alerts === 0 ? 'text-green-400' : 
                  model.alerts === 1 ? 'text-yellow-400' : 'text-red-400'
                }`}>
                  {model.alerts}
                </div>
                <div className="text-slate-400">Alerts</div>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>

    <div className="glass-card rounded-2xl p-6">
      <h3 className="text-xl font-bold text-white mb-6">System Alerts</h3>
      <div className="space-y-3">
        {[
          { id: 'ALT-001', type: 'Model Drift', model: 'Price Prediction Engine', severity: 'high', time: '2 hours ago', message: 'Model drift detected above threshold' },
          { id: 'ALT-002', type: 'Performance Drop', model: 'Market Sentiment Analyzer', severity: 'medium', time: '4 hours ago', message: 'Accuracy dropped below 90%' },
          { id: 'ALT-003', type: 'Resource Usage', model: 'Behavioral Analysis Engine', severity: 'low', time: '6 hours ago', message: 'High memory usage detected' }
        ].map((alert) => (
          <div key={alert.id} className="flex items-center justify-between p-4 bg-slate-800/30 rounded-xl">
            <div>
              <div className="text-white font-medium">{alert.type}</div>
              <div className="text-slate-400 text-sm">{alert.model}</div>
              <div className="text-slate-300 text-sm">{alert.message}</div>
            </div>
            <div className="text-right">
              <div className={`text-sm font-medium mb-1 ${
                alert.severity === 'high' ? 'text-red-400' :
                alert.severity === 'medium' ? 'text-yellow-400' : 'text-green-400'
              }`}>
                {alert.severity.toUpperCase()}
              </div>
              <div className="text-slate-400 text-sm">{alert.time}</div>
            </div>
          </div>
        ))}
      </div>
    </div>
  </div>
)

export default AIIntegrationManagement