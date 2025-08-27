import React, { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Plus, Clock, CheckCircle, AlertTriangle, BarChart3, Target, Zap, Bot, Send, Eye } from 'lucide-react';

interface UserTask {
  taskId: number;
  taskName: string;
  taskType: 'MARKET_ANALYSIS' | 'PORTFOLIO_OPTIMIZATION' | 'RISK_ASSESSMENT' | 'TRADE_EXECUTION';
  status: 'PENDING' | 'QUEUED' | 'IN_PROGRESS' | 'COMPLETED' | 'FAILED';
  priority: 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT';
  submittedAt: string;
  assignedAgent?: string;
  estimatedCompletion?: string;
  queuePosition?: number;
  progressPercentage: number;
  results?: string;
}

interface AgentInfo {
  agentType: string;
  availableAgents: number;
  queueLength: number;
  averageWaitTime: string;
  status: 'AVAILABLE' | 'BUSY' | 'OVERLOADED';
}

const TraderTaskInterface: React.FC = () => {
  const [userTasks, setUserTasks] = useState<UserTask[]>([]);
  const [agentQueue, setAgentQueue] = useState<AgentInfo[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedTab, setSelectedTab] = useState('submit');

  // Mock data for trader tasks
  const mockUserTasks: UserTask[] = [
    {
      taskId: 101,
      taskName: 'BTCUSD Technical Analysis',
      taskType: 'MARKET_ANALYSIS',
      status: 'IN_PROGRESS',
      priority: 'HIGH',
      submittedAt: new Date(Date.now() - 300000).toISOString(),
      assignedAgent: 'MarketAnalyzer-Alpha',
      estimatedCompletion: new Date(Date.now() + 180000).toISOString(),
      progressPercentage: 65
    },
    {
      taskId: 102,
      taskName: 'Portfolio Rebalancing Analysis',
      taskType: 'PORTFOLIO_OPTIMIZATION',
      status: 'QUEUED',
      priority: 'NORMAL',
      submittedAt: new Date(Date.now() - 120000).toISOString(),
      queuePosition: 3,
      progressPercentage: 0
    },
    {
      taskId: 103,
      taskName: 'Risk Assessment - Crypto Holdings',
      taskType: 'RISK_ASSESSMENT',
      status: 'COMPLETED',
      priority: 'HIGH',
      submittedAt: new Date(Date.now() - 900000).toISOString(),
      assignedAgent: 'RiskManager-Delta',
      progressPercentage: 100,
      results: 'Medium risk exposure detected. Recommended position reduction of 15%.'
    }
  ];

  const mockAgentQueue: AgentInfo[] = [
    {
      agentType: 'MARKET_ANALYSIS',
      availableAgents: 2,
      queueLength: 4,
      averageWaitTime: '3-5 min',
      status: 'AVAILABLE'
    },
    {
      agentType: 'PORTFOLIO_OPTIMIZATION',
      availableAgents: 1,
      queueLength: 8,
      averageWaitTime: '8-12 min',
      status: 'BUSY'
    },
    {
      agentType: 'RISK_ASSESSMENT',
      availableAgents: 1,
      queueLength: 2,
      averageWaitTime: '2-4 min',
      status: 'AVAILABLE'
    },
    {
      agentType: 'TRADE_EXECUTION',
      availableAgents: 3,
      queueLength: 1,
      averageWaitTime: '< 1 min',
      status: 'AVAILABLE'
    }
  ];

  useEffect(() => {
    // Load mock data
    setUserTasks(mockUserTasks);
    setAgentQueue(mockAgentQueue);
    setLoading(false);
  }, []);

  const getStatusColor = (status: string): string => {
    switch (status.toLowerCase()) {
      case 'completed': return 'bg-green-500';
      case 'in_progress': return 'bg-blue-500';
      case 'queued': case 'pending': return 'bg-amber-500';
      case 'failed': return 'bg-red-500';
      default: return 'bg-slate-500';
    }
  };

  const getPriorityColor = (priority: string): string => {
    switch (priority.toLowerCase()) {
      case 'urgent': return 'bg-red-500';
      case 'high': return 'bg-orange-500';
      case 'normal': return 'bg-blue-500';
      case 'low': return 'bg-slate-500';
      default: return 'bg-slate-400';
    }
  };

  const getTaskTypeIcon = (type: string) => {
    switch (type) {
      case 'MARKET_ANALYSIS': return <BarChart3 className="h-4 w-4" />;
      case 'PORTFOLIO_OPTIMIZATION': return <Target className="h-4 w-4" />;
      case 'RISK_ASSESSMENT': return <AlertTriangle className="h-4 w-4" />;
      case 'TRADE_EXECUTION': return <Zap className="h-4 w-4" />;
      default: return <Bot className="h-4 w-4" />;
    }
  };

  const getQueueStatusColor = (status: string): string => {
    switch (status) {
      case 'AVAILABLE': return 'text-green-400';
      case 'BUSY': return 'text-amber-400';
      case 'OVERLOADED': return 'text-red-400';
      default: return 'text-slate-400';
    }
  };

  const formatTimeAgo = (dateString: string): string => {
    const date = new Date(dateString);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    
    if (diffMins < 60) return `${diffMins}m ago`;
    const diffHours = Math.floor(diffMins / 60);
    if (diffHours < 24) return `${diffHours}h ago`;
    return `${Math.floor(diffHours / 24)}d ago`;
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-slate-900">
      <div className="container mx-auto p-6 space-y-6">
        {/* Header */}
        <div className="flex justify-between items-center mb-8">
          <div>
            <h1 className="text-3xl font-bold text-white mb-2">Trading Task Center</h1>
            <p className="text-slate-400 text-lg">Submit and track your trading analysis tasks</p>
          </div>
          <div className="flex items-center gap-4">
            <div className="text-right">
              <div className="text-sm text-slate-400">Active Tasks</div>
              <div className="text-xl font-bold text-white">{userTasks.filter(t => t.status === 'IN_PROGRESS' || t.status === 'QUEUED').length}</div>
            </div>
          </div>
        </div>

        {/* Main Content */}
        <div className="bg-slate-800 border border-slate-700 rounded-xl">
          <Tabs value={selectedTab} onValueChange={setSelectedTab} className="w-full">
            <div className="p-6 pb-0">
              <TabsList className="grid w-full grid-cols-3 bg-slate-700/50 p-1 rounded-lg">
                <TabsTrigger value="submit" className="data-[state=active]:bg-slate-600 data-[state=active]:text-white text-slate-400 hover:text-white rounded-md transition-colors">
                  <Plus className="h-4 w-4 mr-2" />
                  Submit Task
                </TabsTrigger>
                <TabsTrigger value="tasks" className="data-[state=active]:bg-slate-600 data-[state=active]:text-white text-slate-400 hover:text-white rounded-md transition-colors">
                  <Eye className="h-4 w-4 mr-2" />
                  My Tasks
                </TabsTrigger>
                <TabsTrigger value="queue" className="data-[state=active]:bg-slate-600 data-[state=active]:text-white text-slate-400 hover:text-white rounded-md transition-colors">
                  <Clock className="h-4 w-4 mr-2" />
                  Queue Status
                </TabsTrigger>
              </TabsList>
            </div>

            {/* Submit Task Tab */}
            <TabsContent value="submit" className="p-6 space-y-6">
              <Card className="bg-slate-800 border-slate-700">
                <CardHeader>
                  <CardTitle className="text-white">Submit New Analysis Task</CardTitle>
                </CardHeader>
                <CardContent className="space-y-6">
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    {/* Task Type Selection */}
                    <div className="space-y-4">
                      <h3 className="text-white font-semibold">Select Analysis Type</h3>
                      {mockAgentQueue.map((agent) => (
                        <div key={agent.agentType} className="bg-slate-700/50 rounded-lg p-4 cursor-pointer hover:bg-slate-700 transition-colors">
                          <div className="flex items-center justify-between">
                            <div className="flex items-center gap-3">
                              <div className="p-2 bg-primary/20 rounded-lg">
                                <div className="text-primary">{getTaskTypeIcon(agent.agentType)}</div>
                              </div>
                              <div>
                                <div className="text-white font-medium">{agent.agentType.replace('_', ' ')}</div>
                                <div className="text-sm text-slate-400">{agent.availableAgents} agents • {agent.queueLength} in queue</div>
                              </div>
                            </div>
                            <div className="text-right">
                              <div className={`text-sm font-medium ${getQueueStatusColor(agent.status)}`}>{agent.status}</div>
                              <div className="text-xs text-slate-400">~{agent.averageWaitTime}</div>
                            </div>
                          </div>
                        </div>
                      ))}
                    </div>

                    {/* Task Configuration */}
                    <div className="space-y-4">
                      <h3 className="text-white font-semibold">Task Configuration</h3>
                      <div className="space-y-4">
                        <div>
                          <label className="block text-sm font-medium text-slate-300 mb-2">Task Name</label>
                          <input
                            type="text"
                            placeholder="e.g., BTCUSD Technical Analysis"
                            className="w-full bg-slate-700 border border-slate-600 rounded-lg px-3 py-2 text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent"
                          />
                        </div>
                        <div>
                          <label className="block text-sm font-medium text-slate-300 mb-2">Priority</label>
                          <select className="w-full bg-slate-700 border border-slate-600 rounded-lg px-3 py-2 text-white focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent">
                            <option value="NORMAL">Normal</option>
                            <option value="HIGH">High</option>
                            <option value="URGENT">Urgent</option>
                            <option value="LOW">Low</option>
                          </select>
                        </div>
                        <div>
                          <label className="block text-sm font-medium text-slate-300 mb-2">Additional Parameters</label>
                          <textarea
                            placeholder="Symbol: BTCUSD, Timeframe: 4H, Indicators: RSI, MACD"
                            className="w-full bg-slate-700 border border-slate-600 rounded-lg px-3 py-2 text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent h-24 resize-none"
                          />
                        </div>
                        <Button className="w-full tm-button-primary rounded-lg py-2">
                          <Send className="h-4 w-4 mr-2" />
                          Submit Task
                        </Button>
                      </div>
                    </div>
                  </div>
                </CardContent>
              </Card>
            </TabsContent>

            {/* My Tasks Tab */}
            <TabsContent value="tasks" className="p-6 space-y-6">
              <div className="space-y-4">
                {userTasks.map((task) => (
                  <Card key={task.taskId} className="bg-slate-800 border-slate-700">
                    <CardContent className="p-6">
                      <div className="flex justify-between items-start mb-4">
                        <div className="flex items-center gap-3">
                          <div className="p-2 bg-primary/20 rounded-lg">
                            <div className="text-primary">{getTaskTypeIcon(task.taskType)}</div>
                          </div>
                          <div>
                            <h3 className="font-bold text-lg text-white">{task.taskName}</h3>
                            <p className="text-slate-400 text-sm">{task.taskType.replace('_', ' ')}</p>
                          </div>
                        </div>
                        <div className="flex items-center gap-2">
                          <Badge className={`${getStatusColor(task.status)} text-white border-0 px-2 py-1 text-xs font-medium rounded-md`}>
                            {task.status}
                          </Badge>
                          <Badge className={`${getPriorityColor(task.priority)} text-white border-0 px-2 py-1 text-xs font-medium rounded-md`}>
                            {task.priority}
                          </Badge>
                        </div>
                      </div>

                      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-4">
                        <div className="bg-slate-700/50 rounded-lg p-3">
                          <div className="text-xs text-slate-300 font-medium uppercase tracking-wide">Submitted</div>
                          <div className="text-sm font-bold text-white">{formatTimeAgo(task.submittedAt)}</div>
                        </div>
                        {task.assignedAgent && (
                          <div className="bg-slate-700/50 rounded-lg p-3">
                            <div className="text-xs text-slate-300 font-medium uppercase tracking-wide">Agent</div>
                            <div className="text-sm font-bold text-white">{task.assignedAgent}</div>
                          </div>
                        )}
                        {task.queuePosition && (
                          <div className="bg-slate-700/50 rounded-lg p-3">
                            <div className="text-xs text-slate-300 font-medium uppercase tracking-wide">Queue Position</div>
                            <div className="text-sm font-bold text-amber-400">#{task.queuePosition}</div>
                          </div>
                        )}
                        {task.estimatedCompletion && (
                          <div className="bg-slate-700/50 rounded-lg p-3">
                            <div className="text-xs text-slate-300 font-medium uppercase tracking-wide">ETA</div>
                            <div className="text-sm font-bold text-white">{formatTimeAgo(task.estimatedCompletion)}</div>
                          </div>
                        )}
                      </div>

                      <div className="space-y-2">
                        <div className="flex justify-between text-sm font-medium text-slate-400">
                          <span>Progress</span>
                          <span>{task.progressPercentage}% Complete</span>
                        </div>
                        <div className="w-full bg-slate-700 rounded-full h-2">
                          <div 
                            className={`h-2 rounded-full transition-all duration-1000 ${
                              task.status === 'COMPLETED' ? 'bg-green-500' :
                              task.status === 'IN_PROGRESS' ? 'bg-blue-500' :
                              task.status === 'FAILED' ? 'bg-red-500' :
                              'bg-slate-500'
                            }`}
                            style={{width: `${task.progressPercentage}%`}}
                          ></div>
                        </div>
                      </div>

                      {task.results && (
                        <div className="mt-4 p-4 bg-slate-700/30 rounded-lg">
                          <div className="text-sm font-medium text-slate-300 mb-2">Results:</div>
                          <div className="text-white text-sm">{task.results}</div>
                        </div>
                      )}
                    </CardContent>
                  </Card>
                ))}
              </div>
            </TabsContent>

            {/* Queue Status Tab */}
            <TabsContent value="queue" className="p-6 space-y-6">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                {mockAgentQueue.map((agent) => (
                  <Card key={agent.agentType} className="bg-slate-800 border-slate-700">
                    <CardHeader>
                      <CardTitle className="flex items-center gap-3 text-white">
                        <div className="p-2 bg-primary/20 rounded-lg">
                          <div className="text-primary">{getTaskTypeIcon(agent.agentType)}</div>
                        </div>
                        {agent.agentType.replace('_', ' ')}
                      </CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-4">
                      <div className="grid grid-cols-2 gap-4">
                        <div className="bg-slate-700/50 rounded-lg p-3">
                          <div className="text-xs text-slate-300 font-medium uppercase tracking-wide">Available Agents</div>
                          <div className="text-xl font-bold text-green-400">{agent.availableAgents}</div>
                        </div>
                        <div className="bg-slate-700/50 rounded-lg p-3">
                          <div className="text-xs text-slate-300 font-medium uppercase tracking-wide">Queue Length</div>
                          <div className="text-xl font-bold text-white">{agent.queueLength}</div>
                        </div>
                      </div>
                      <div className="flex justify-between items-center">
                        <span className="text-slate-300">Status</span>
                        <span className={`font-semibold ${getQueueStatusColor(agent.status)}`}>{agent.status}</span>
                      </div>
                      <div className="flex justify-between items-center">
                        <span className="text-slate-300">Avg Wait Time</span>
                        <span className="font-semibold text-white">{agent.averageWaitTime}</span>
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

export default TraderTaskInterface;