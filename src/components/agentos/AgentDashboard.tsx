import React, { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Activity, Users, CheckCircle, AlertTriangle, Clock, BarChart3, Bot, Zap, Target, TrendingUp } from 'lucide-react';

interface Agent {
  agentId: number;
  agentName: string;
  agentType: string;
  status: string;
  currentLoad: number;
  maxConcurrentTasks: number;
  successRate: number;
  averageResponseTime: number;
  totalTasksCompleted: number;
  lastHeartbeat: string;
}

interface Task {
  taskId: number;
  taskName: string;
  taskType: string;
  status: string;
  priority: string;
  agentId?: number;
  progressPercentage: number;
  createdAt: string;
  estimatedDuration?: number;
}

interface OrchestrationMetrics {
  totalAgents: number;
  activeAgents: number;
  busyAgents: number;
  errorAgents: number;
  totalTasks: number;
  pendingTasks: number;
  inProgressTasks: number;
  completedTasks: number;
  failedTasks: number;
  systemUtilization: number;
  averageSuccessRate: number;
}

const AdminAgentDashboard: React.FC = () => {
  const [agents, setAgents] = useState<Agent[]>([]);
  const [tasks, setTasks] = useState<Task[]>([]);
  const [metrics, setMetrics] = useState<OrchestrationMetrics | null>(null);
  const [loading, setLoading] = useState(true);
  const [selectedTab, setSelectedTab] = useState('overview');

  // Mock data - replace with real API calls later
  const mockMetrics: OrchestrationMetrics = {
    totalAgents: 12,
    activeAgents: 8,
    busyAgents: 3,
    errorAgents: 1,
    totalTasks: 156,
    pendingTasks: 4,
    inProgressTasks: 12,
    completedTasks: 135,
    failedTasks: 5,
    systemUtilization: 67.5,
    averageSuccessRate: 0.947
  };

  const mockAgents: Agent[] = [
    {
      agentId: 1,
      agentName: 'MarketAnalyzer-Alpha',
      agentType: 'MARKET_ANALYSIS',
      status: 'ACTIVE',
      currentLoad: 2,
      maxConcurrentTasks: 5,
      successRate: 0.98,
      averageResponseTime: 125,
      totalTasksCompleted: 342,
      lastHeartbeat: new Date().toISOString()
    },
    {
      agentId: 2,
      agentName: 'PortfolioOptimizer-Beta',
      agentType: 'PORTFOLIO_MANAGEMENT',
      status: 'BUSY',
      currentLoad: 4,
      maxConcurrentTasks: 4,
      successRate: 0.94,
      averageResponseTime: 230,
      totalTasksCompleted: 187,
      lastHeartbeat: new Date().toISOString()
    },
    {
      agentId: 3,
      agentName: 'TradeExecutor-Gamma',
      agentType: 'TRADING_EXECUTION',
      status: 'ACTIVE',
      currentLoad: 1,
      maxConcurrentTasks: 10,
      successRate: 0.99,
      averageResponseTime: 89,
      totalTasksCompleted: 892,
      lastHeartbeat: new Date().toISOString()
    },
    {
      agentId: 4,
      agentName: 'RiskManager-Delta',
      agentType: 'RISK_MANAGEMENT',
      status: 'ACTIVE',
      currentLoad: 0,
      maxConcurrentTasks: 3,
      successRate: 0.91,
      averageResponseTime: 156,
      totalTasksCompleted: 234,
      lastHeartbeat: new Date().toISOString()
    },
    {
      agentId: 5,
      agentName: 'SentimentAnalyzer-Echo',
      agentType: 'MARKET_ANALYSIS',
      status: 'ERROR',
      currentLoad: 0,
      maxConcurrentTasks: 8,
      successRate: 0.87,
      averageResponseTime: 445,
      totalTasksCompleted: 156,
      lastHeartbeat: new Date(Date.now() - 300000).toISOString()
    },
    {
      agentId: 6,
      agentName: 'ArbitrageDetector-Zeta',
      agentType: 'TRADING_EXECUTION',
      status: 'BUSY',
      currentLoad: 7,
      maxConcurrentTasks: 8,
      successRate: 0.96,
      averageResponseTime: 178,
      totalTasksCompleted: 445,
      lastHeartbeat: new Date().toISOString()
    }
  ];

  const mockTasks: Task[] = [
    {
      taskId: 1,
      taskName: 'Analyze BTCUSD Market Trends',
      taskType: 'MARKET_ANALYSIS',
      status: 'IN_PROGRESS',
      priority: 'HIGH',
      agentId: 1,
      progressPercentage: 75,
      createdAt: new Date(Date.now() - 120000).toISOString(),
      estimatedDuration: 300
    },
    {
      taskId: 2,
      taskName: 'Optimize Portfolio Allocation',
      taskType: 'PORTFOLIO_MANAGEMENT',
      status: 'IN_PROGRESS',
      priority: 'MEDIUM',
      agentId: 2,
      progressPercentage: 45,
      createdAt: new Date(Date.now() - 180000).toISOString(),
      estimatedDuration: 600
    },
    {
      taskId: 3,
      taskName: 'Execute Large Order - ETHUSDT',
      taskType: 'TRADING_EXECUTION',
      status: 'COMPLETED',
      priority: 'CRITICAL',
      agentId: 3,
      progressPercentage: 100,
      createdAt: new Date(Date.now() - 300000).toISOString(),
      estimatedDuration: 45
    },
    {
      taskId: 4,
      taskName: 'Risk Assessment - Leveraged Positions',
      taskType: 'RISK_MANAGEMENT',
      status: 'PENDING',
      priority: 'HIGH',
      progressPercentage: 0,
      createdAt: new Date(Date.now() - 60000).toISOString(),
      estimatedDuration: 180
    },
    {
      taskId: 5,
      taskName: 'Social Sentiment Analysis - Crypto Market',
      taskType: 'MARKET_ANALYSIS',
      status: 'FAILED',
      priority: 'LOW',
      progressPercentage: 30,
      createdAt: new Date(Date.now() - 900000).toISOString(),
      estimatedDuration: 240
    }
  ];

  useEffect(() => {
    // Load mock data
    setMetrics(mockMetrics);
    setAgents(mockAgents);
    setTasks(mockTasks);
    setLoading(false);
  }, []);

  const fetchDashboardData = () => {
    setLoading(true);
    // Simulate API delay
    setTimeout(() => {
      setMetrics(mockMetrics);
      setAgents(mockAgents);
      setTasks(mockTasks);
      setLoading(false);
    }, 1000);
  };

  const getStatusColor = (status: string): string => {
    switch (status.toLowerCase()) {
      case 'active': return 'bg-green-500';
      case 'busy': return 'bg-amber-500';
      case 'error': case 'failed': return 'bg-red-500';
      case 'completed': return 'bg-green-500';
      case 'in_progress': return 'bg-blue-500';
      case 'pending': return 'bg-slate-500';
      default: return 'bg-slate-400';
    }
  };

  const getAgentTypeIcon = (type: string) => {
    switch (type) {
      case 'MARKET_ANALYSIS': return <BarChart3 className="h-5 w-5" />;
      case 'PORTFOLIO_MANAGEMENT': return <Target className="h-5 w-5" />;
      case 'TRADING_EXECUTION': return <Zap className="h-5 w-5" />;
      case 'RISK_MANAGEMENT': return <AlertTriangle className="h-5 w-5" />;
      default: return <Bot className="h-5 w-5" />;
    }
  };

  const formatDuration = (seconds: number): string => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  };

  if (loading && !metrics) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-slate-900">
      <div className="container mx-auto p-6 space-y-6">
        {/* Header - Clean professional style */}
        <div className="flex justify-between items-center mb-8">
          <div>
            <h1 className="text-3xl font-bold text-white mb-2">Admin - Agent OS Dashboard</h1>
            <p className="text-slate-400 text-lg">Monitor and manage AI agents in the TradeMaster ecosystem</p>
          </div>
          <Button 
            onClick={fetchDashboardData} 
            disabled={loading}
            className="tm-button px-6 py-2 rounded-lg flex items-center gap-2"
          >
            {loading ? (
              <>
                <div className="animate-spin rounded-full h-4 w-4 border-2 border-white border-t-transparent"></div>
                Refreshing...
              </>
            ) : (
              <>
                <TrendingUp className="h-4 w-4" />
                Refresh Data
              </>
            )}
          </Button>
        </div>

        {/* System Metrics Overview */}
        {metrics && (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
            <Card className="bg-slate-800 border-slate-700 hover:bg-slate-800/80 transition-colors">
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-sm font-medium text-slate-300">Total Agents</CardTitle>
                <div className="p-2 bg-primary/20 rounded-lg">
                  <Users className="h-4 w-4 text-primary" />
                </div>
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold text-white">
                  {metrics.totalAgents}
                </div>
                <p className="text-xs text-slate-400 mt-1">
                  <span className="text-green-400 font-medium">{metrics.activeAgents} active</span> • {metrics.busyAgents} busy
                </p>
              </CardContent>
            </Card>

            <Card className="bg-slate-800 border-slate-700 hover:bg-slate-800/80 transition-colors">
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-sm font-medium text-slate-300">System Utilization</CardTitle>
                <div className="p-2 bg-amber-500/20 rounded-lg">
                  <BarChart3 className="h-4 w-4 text-amber-400" />
                </div>
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold text-white">
                  {metrics.systemUtilization.toFixed(1)}%
                </div>
                <p className="text-xs text-slate-400 mt-1">
                  <span className="text-amber-400 font-medium">{metrics.busyAgents}/{metrics.totalAgents}</span> agents busy
                </p>
              </CardContent>
            </Card>

            <Card className="bg-slate-800 border-slate-700 hover:bg-slate-800/80 transition-colors">
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-sm font-medium text-slate-300">Tasks</CardTitle>
                <div className="p-2 bg-purple-500/20 rounded-lg">
                  <Activity className="h-4 w-4 text-purple-400" />
                </div>
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold text-white">
                  {metrics.totalTasks}
                </div>
                <p className="text-xs text-slate-400 mt-1">
                  <span className="text-purple-400 font-medium">{metrics.pendingTasks} pending</span> • {metrics.inProgressTasks} running
                </p>
              </CardContent>
            </Card>

            <Card className="bg-slate-800 border-slate-700 hover:bg-slate-800/80 transition-colors">
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-sm font-medium text-slate-300">Success Rate</CardTitle>
                <div className="p-2 bg-green-500/20 rounded-lg">
                  <CheckCircle className="h-4 w-4 text-green-400" />
                </div>
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold text-white">
                  {(metrics.averageSuccessRate * 100).toFixed(1)}%
                </div>
                <p className="text-xs text-slate-400 mt-1">
                  <span className="text-green-400 font-medium">{metrics.completedTasks} completed</span> • {metrics.failedTasks} failed
                </p>
              </CardContent>
            </Card>
          </div>
        )}

        {/* Main Content Tabs */}
        <div className="bg-slate-800 border border-slate-700 rounded-xl">
          <Tabs value={selectedTab} onValueChange={setSelectedTab} className="w-full">
            <div className="p-6 pb-0">
              <TabsList className="grid w-full grid-cols-3 bg-slate-700/50 p-1 rounded-lg">
                <TabsTrigger value="overview" className="data-[state=active]:bg-slate-600 data-[state=active]:text-white text-slate-400 hover:text-white rounded-md transition-colors">
                  <BarChart3 className="h-4 w-4 mr-2" />
                  Overview
                </TabsTrigger>
                <TabsTrigger value="agents" className="data-[state=active]:bg-slate-600 data-[state=active]:text-white text-slate-400 hover:text-white rounded-md transition-colors">
                  <Bot className="h-4 w-4 mr-2" />
                  Agents
                </TabsTrigger>
                <TabsTrigger value="tasks" className="data-[state=active]:bg-slate-600 data-[state=active]:text-white text-slate-400 hover:text-white rounded-md transition-colors">
                  <Activity className="h-4 w-4 mr-2" />
                  Tasks
                </TabsTrigger>
              </TabsList>
            </div>

            <TabsContent value="overview" className="p-6 space-y-6">
              {metrics && (
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                  {/* Agent Status Distribution */}
                  <Card className="bg-slate-800 border-slate-700">
                    <CardHeader>
                      <CardTitle className="flex items-center gap-2 text-white">
                        <Users className="h-5 w-5 text-primary" />
                        Agent Status Distribution
                      </CardTitle>
                    </CardHeader>
                    <CardContent>
                  <div className="space-y-4">
                    <div className="flex justify-between items-center">
                      <span className="text-sm text-slate-300">Active</span>
                      <div className="flex items-center gap-3">
                        <div className="w-24 bg-slate-700 rounded-full h-2">
                          <div 
                            className="bg-green-400 h-2 rounded-full transition-all duration-500" 
                            style={{width: `${(metrics.activeAgents / metrics.totalAgents) * 100}%`}}
                          ></div>
                        </div>
                        <span className="text-sm font-medium text-white w-6">{metrics.activeAgents}</span>
                      </div>
                    </div>
                    <div className="flex justify-between items-center">
                      <span className="text-sm text-slate-300">Busy</span>
                      <div className="flex items-center gap-3">
                        <div className="w-24 bg-slate-700 rounded-full h-2">
                          <div 
                            className="bg-amber-400 h-2 rounded-full transition-all duration-500" 
                            style={{width: `${(metrics.busyAgents / metrics.totalAgents) * 100}%`}}
                          ></div>
                        </div>
                        <span className="text-sm font-medium text-white w-6">{metrics.busyAgents}</span>
                      </div>
                    </div>
                    <div className="flex justify-between items-center">
                      <span className="text-sm text-slate-300">Error</span>
                      <div className="flex items-center gap-3">
                        <div className="w-24 bg-slate-700 rounded-full h-2">
                          <div 
                            className="bg-red-400 h-2 rounded-full transition-all duration-500" 
                            style={{width: `${(metrics.errorAgents / metrics.totalAgents) * 100}%`}}
                          ></div>
                        </div>
                        <span className="text-sm font-medium text-white w-6">{metrics.errorAgents}</span>
                      </div>
                    </div>
                  </div>
                </CardContent>
              </Card>

                  {/* Task Status Distribution */}
                  <Card className="bg-slate-800 border-slate-700">
                    <CardHeader>
                      <CardTitle className="flex items-center gap-2 text-white">
                        <Activity className="h-5 w-5 text-primary" />
                        Task Status Distribution
                      </CardTitle>
                    </CardHeader>
                    <CardContent>
                  <div className="space-y-4">
                    <div className="flex justify-between items-center">
                      <span className="text-sm text-slate-300">Completed</span>
                      <div className="flex items-center gap-3">
                        <div className="w-24 bg-slate-700 rounded-full h-2">
                          <div 
                            className="bg-green-400 h-2 rounded-full transition-all duration-500" 
                            style={{width: `${(metrics.completedTasks / metrics.totalTasks) * 100}%`}}
                          ></div>
                        </div>
                        <span className="text-sm font-medium text-white w-8">{metrics.completedTasks}</span>
                      </div>
                    </div>
                    <div className="flex justify-between items-center">
                      <span className="text-sm text-slate-300">In Progress</span>
                      <div className="flex items-center gap-3">
                        <div className="w-24 bg-slate-700 rounded-full h-2">
                          <div 
                            className="bg-primary h-2 rounded-full transition-all duration-500" 
                            style={{width: `${(metrics.inProgressTasks / metrics.totalTasks) * 100}%`}}
                          ></div>
                        </div>
                        <span className="text-sm font-medium text-white w-8">{metrics.inProgressTasks}</span>
                      </div>
                    </div>
                    <div className="flex justify-between items-center">
                      <span className="text-sm text-slate-300">Pending</span>
                      <div className="flex items-center gap-3">
                        <div className="w-24 bg-slate-700 rounded-full h-2">
                          <div 
                            className="bg-slate-400 h-2 rounded-full transition-all duration-500" 
                            style={{width: `${(metrics.pendingTasks / metrics.totalTasks) * 100}%`}}
                          ></div>
                        </div>
                        <span className="text-sm font-medium text-white w-8">{metrics.pendingTasks}</span>
                      </div>
                    </div>
                    <div className="flex justify-between items-center">
                      <span className="text-sm text-slate-300">Failed</span>
                      <div className="flex items-center gap-3">
                        <div className="w-24 bg-slate-700 rounded-full h-2">
                          <div 
                            className="bg-red-400 h-2 rounded-full transition-all duration-500" 
                            style={{width: `${(metrics.failedTasks / metrics.totalTasks) * 100}%`}}
                          ></div>
                        </div>
                        <span className="text-sm font-medium text-white w-8">{metrics.failedTasks}</span>
                      </div>
                    </div>
                  </div>
                </CardContent>
              </Card>
            </div>
          )}
        </TabsContent>

            <TabsContent value="agents" className="p-6 space-y-6">
              <div className="grid grid-cols-1 lg:grid-cols-2 xl:grid-cols-3 gap-6">
                {agents.map((agent) => (
                  <Card key={agent.agentId} className="bg-slate-800 border-slate-700 hover:bg-slate-750 transition-all duration-300">
                    <CardHeader className="pb-3">
                      <div className="flex justify-between items-start">
                        <div className="flex items-center gap-3">
                          <div className="p-2 bg-primary/20 rounded-lg">
                            <div className="text-primary">{getAgentTypeIcon(agent.agentType)}</div>
                          </div>
                          <div>
                            <CardTitle className="text-lg text-white">{agent.agentName}</CardTitle>
                            <p className="text-sm text-slate-400 font-medium">{agent.agentType.replace('_', ' ')}</p>
                          </div>
                        </div>
                        <Badge className={`${getStatusColor(agent.status)} text-white border-0 px-2 py-1 text-xs font-medium rounded-md`}>
                          {agent.status}
                        </Badge>
                      </div>
                    </CardHeader>
                    <CardContent className="space-y-4">
                      <div className="grid grid-cols-2 gap-4">
                        <div className="bg-blue-50 rounded-lg p-3">
                          <div className="text-xs text-blue-600 font-medium uppercase tracking-wide">Current Load</div>
                          <div className="text-lg font-bold text-blue-800">
                            {agent.currentLoad}<span className="text-sm text-blue-600">/{agent.maxConcurrentTasks}</span>
                          </div>
                        </div>
                        <div className="bg-green-50 rounded-lg p-3">
                          <div className="text-xs text-green-600 font-medium uppercase tracking-wide">Success Rate</div>
                          <div className="text-lg font-bold text-green-800">{(agent.successRate * 100).toFixed(1)}%</div>
                        </div>
                      </div>
                      
                      <div className="grid grid-cols-2 gap-4">
                        <div className="bg-purple-50 rounded-lg p-3">
                          <div className="text-xs text-purple-600 font-medium uppercase tracking-wide">Avg Response</div>
                          <div className="text-lg font-bold text-purple-800">{agent.averageResponseTime}ms</div>
                        </div>
                        <div className="bg-amber-50 rounded-lg p-3">
                          <div className="text-xs text-amber-600 font-medium uppercase tracking-wide">Completed</div>
                          <div className="text-lg font-bold text-amber-800">{agent.totalTasksCompleted.toLocaleString()}</div>
                        </div>
                      </div>
                      
                      <div className="space-y-2">
                        <div className="flex justify-between text-sm font-medium text-gray-600">
                          <span>Capacity Utilization</span>
                          <span>{((agent.currentLoad / agent.maxConcurrentTasks) * 100).toFixed(0)}%</span>
                        </div>
                        <div className="w-full bg-gray-200 rounded-full h-3 overflow-hidden">
                          <div 
                            className="bg-gradient-to-r from-blue-500 to-indigo-500 h-3 rounded-full transition-all duration-500 ease-out" 
                            style={{width: `${(agent.currentLoad / agent.maxConcurrentTasks) * 100}%`}}
                          ></div>
                        </div>
                      </div>
                    </CardContent>
                  </Card>
                ))}
              </div>
            </TabsContent>

            <TabsContent value="tasks" className="p-6 space-y-6">
              <div className="space-y-4">
                {tasks.map((task) => (
                  <Card key={task.taskId} className="bg-slate-800 border-slate-700 hover:bg-slate-800/80 transition-all duration-300">
                    <CardContent className="p-6">
                      <div className="flex justify-between items-start mb-4">
                        <div className="flex items-center gap-3">
                          <div className="p-2 bg-primary/20 rounded-lg">
                            <div className="text-primary">{getAgentTypeIcon(task.taskType)}</div>
                          </div>
                          <div>
                            <h3 className="font-bold text-xl text-white">{task.taskName}</h3>
                            <p className="text-slate-400 font-medium">{task.taskType.replace('_', ' ')}</p>
                          </div>
                        </div>
                        <div className="flex items-center gap-2">
                          <Badge className={`${getStatusColor(task.status)} text-white border-0 shadow-lg`}>
                            {task.status}
                          </Badge>
                          <Badge 
                            className={task.priority === 'CRITICAL' || task.priority === 'HIGH' 
                              ? 'bg-gradient-to-r from-red-500 to-rose-600 text-white border-0' 
                              : 'bg-gradient-to-r from-gray-500 to-slate-600 text-white border-0'
                            }
                          >
                            {task.priority}
                          </Badge>
                        </div>
                      </div>
                      
                      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-4">
                        <div className="bg-blue-50 rounded-lg p-3">
                          <div className="text-xs text-blue-600 font-medium uppercase tracking-wide">Progress</div>
                          <div className="text-lg font-bold text-blue-800">{task.progressPercentage}%</div>
                        </div>
                        <div className="bg-green-50 rounded-lg p-3">
                          <div className="text-xs text-green-600 font-medium uppercase tracking-wide">Agent ID</div>
                          <div className="text-lg font-bold text-green-800">{task.agentId || 'Unassigned'}</div>
                        </div>
                        <div className="bg-purple-50 rounded-lg p-3">
                          <div className="text-xs text-purple-600 font-medium uppercase tracking-wide">Created</div>
                          <div className="text-sm font-bold text-purple-800">
                            {new Date(task.createdAt).toLocaleTimeString()}
                          </div>
                        </div>
                        {task.estimatedDuration && (
                          <div className="bg-amber-50 rounded-lg p-3">
                            <div className="text-xs text-amber-600 font-medium uppercase tracking-wide">Duration</div>
                            <div className="text-lg font-bold text-amber-800">
                              {formatDuration(task.estimatedDuration)}
                            </div>
                          </div>
                        )}
                      </div>
                      
                      <div className="space-y-2">
                        <div className="flex justify-between text-sm font-medium text-gray-600">
                          <span>Task Progress</span>
                          <span>{task.progressPercentage}% Complete</span>
                        </div>
                        <div className="w-full bg-gray-200 rounded-full h-3 overflow-hidden">
                          <div 
                            className={`h-3 rounded-full transition-all duration-1000 ease-out ${
                              task.status === 'COMPLETED' ? 'bg-gradient-to-r from-green-500 to-emerald-500' :
                              task.status === 'IN_PROGRESS' ? 'bg-gradient-to-r from-blue-500 to-indigo-500' :
                              task.status === 'FAILED' ? 'bg-gradient-to-r from-red-500 to-rose-500' :
                              'bg-gradient-to-r from-gray-400 to-gray-500'
                            }`}
                            style={{width: `${task.progressPercentage}%`}}
                          ></div>
                        </div>
                      </div>
                    </CardContent>
                  </Card>
                ))}
              </div>
            </TabsContent>
          </Tabs>
        </div>
      </div>
    </div>
  );
};

export default AdminAgentDashboard;