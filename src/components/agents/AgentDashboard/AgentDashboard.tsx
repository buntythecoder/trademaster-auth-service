import React, { useState, useEffect, useMemo, useCallback, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Box, Typography, Paper, Card, CardContent, Grid, Chip, Avatar,
  LinearProgress, CircularProgress, IconButton, Button, Tabs, Tab,
  List, ListItem, ListItemText, ListItemAvatar, ListItemIcon,
  TextField, Dialog, DialogTitle, DialogContent, DialogActions,
  Switch, FormControlLabel, Slider, Badge, Tooltip, Divider,
  Accordion, AccordionSummary, AccordionDetails, Menu, MenuItem,
  Select, FormControl, InputLabel, Autocomplete, Alert, AlertTitle
} from '@mui/material';
import {
  SmartToy as SmartToyIcon,
  Chat as ChatIcon,
  Person as PersonIcon,
  Settings as SettingsIcon,
  PlayArrow as PlayArrowIcon,
  Pause as PauseIcon,
  Stop as StopIcon,
  Refresh as RefreshIcon,
  Add as AddIcon,
  Delete as DeleteIcon,
  Edit as EditIcon,
  Visibility as VisibilityIcon,
  VisibilityOff as VisibilityOffIcon,
  Send as SendIcon,
  Mic as MicIcon,
  MicOff as MicOffIcon,
  VolumeUp as VolumeUpIcon,
  VolumeOff as VolumeOffIcon,
  CheckCircle as CheckCircleIcon,
  Warning as WarningIcon,
  Error as ErrorIcon,
  Info as InfoIcon,
  Schedule as ScheduleIcon,
  Assignment as AssignmentIcon,
  TrendingUp as TrendingUpIcon,
  TrendingDown as TrendingDownIcon,
  Analytics as AnalyticsIcon,
  Psychology as PsychologyIcon,
  AccountBalance as AccountBalanceIcon,
  ShowChart as ShowChartIcon,
  Assessment as AssessmentIcon,
  Speed as SpeedIcon,
  Memory as MemoryIcon,
  Storage as StorageIcon,
  NetworkCheck as NetworkCheckIcon,
  Group as GroupIcon,
  Business as BusinessIcon,
  AutoMode as AutoModeIcon,
  Code as CodeIcon,
  BugReport as BugReportIcon,
  Science as ScienceIcon,
  Close as CloseIcon,
  ExpandMore as ExpandMoreIcon,
  MoreVert as MoreVertIcon,
  FilterList as FilterListIcon,
  Search as SearchIcon,
  Download as DownloadIcon,
  Upload as UploadIcon
} from '@mui/icons-material';

// Types and Interfaces
interface Agent {
  id: string;
  name: string;
  type: AgentType;
  status: AgentStatus;
  avatar?: string;
  description: string;
  capabilities: string[];
  configuration: AgentConfiguration;
  performance: AgentPerformance;
  resources: AgentResources;
  createdAt: Date;
  lastActive: Date;
  version: string;
  isActive: boolean;
  priority: 'low' | 'medium' | 'high' | 'critical';
  owner: string;
  tags: string[];
}

interface AgentConfiguration {
  parameters: Record<string, any>;
  endpoints: string[];
  permissions: string[];
  maxConcurrentTasks: number;
  timeoutMs: number;
  retryAttempts: number;
  learningEnabled: boolean;
  loggingLevel: 'debug' | 'info' | 'warn' | 'error';
}

interface AgentPerformance {
  totalTasks: number;
  completedTasks: number;
  failedTasks: number;
  averageExecutionTime: number;
  successRate: number;
  errorRate: number;
  throughput: number; // tasks per hour
  responseTime: number; // ms
  accuracy: number; // percentage
  userSatisfaction: number; // 1-5 rating
  costEfficiency: number; // cost per task
  learningProgress: number; // percentage
}

interface AgentResources {
  cpu: {
    usage: number;
    allocated: number;
    limit: number;
  };
  memory: {
    usage: number;
    allocated: number;
    limit: number;
  };
  storage: {
    usage: number;
    allocated: number;
    limit: number;
  };
  network: {
    bandwidth: number;
    latency: number;
    requests: number;
  };
}

interface Task {
  id: string;
  title: string;
  description: string;
  type: TaskType;
  status: TaskStatus;
  priority: 'low' | 'medium' | 'high' | 'urgent';
  assignedAgents: string[];
  createdBy: string;
  createdAt: Date;
  startedAt?: Date;
  completedAt?: Date;
  estimatedDuration: number; // minutes
  actualDuration?: number;
  progress: number; // 0-100
  result?: any;
  error?: string;
  dependencies: string[];
  tags: string[];
  metadata: Record<string, any>;
}

interface ChatMessage {
  id: string;
  sender: 'user' | 'agent' | 'system';
  agentId?: string;
  content: string;
  timestamp: Date;
  type: 'text' | 'command' | 'result' | 'error' | 'attachment';
  metadata?: {
    taskId?: string;
    command?: string;
    confidence?: number;
    suggestions?: string[];
  };
  reactions?: Array<{
    type: 'thumbs_up' | 'thumbs_down' | 'helpful' | 'not_helpful';
    userId: string;
  }>;
}

interface AgentConversation {
  id: string;
  title: string;
  participants: Array<{
    type: 'user' | 'agent';
    id: string;
    name: string;
  }>;
  messages: ChatMessage[];
  isActive: boolean;
  createdAt: Date;
  lastMessage: Date;
  tags: string[];
}

type AgentType = 'trading' | 'analysis' | 'research' | 'risk' | 'portfolio' | 'market_data' | 'behavioral' | 'custom';
type AgentStatus = 'active' | 'idle' | 'busy' | 'error' | 'offline' | 'initializing' | 'updating';
type TaskType = 'analysis' | 'trading' | 'research' | 'monitoring' | 'alert' | 'report' | 'optimization' | 'custom';
type TaskStatus = 'pending' | 'assigned' | 'running' | 'paused' | 'completed' | 'failed' | 'cancelled';

const AgentDashboard: React.FC = () => {
  // State Management
  const [agents, setAgents] = useState<Agent[]>([]);
  const [tasks, setTasks] = useState<Task[]>([]);
  const [conversations, setConversations] = useState<AgentConversation[]>([]);
  const [activeTab, setActiveTab] = useState(0);
  const [selectedAgent, setSelectedAgent] = useState<Agent | null>(null);
  const [selectedConversation, setSelectedConversation] = useState<AgentConversation | null>(null);
  const [showAgentDialog, setShowAgentDialog] = useState(false);
  const [showTaskDialog, setShowTaskDialog] = useState(false);
  const [newAgentConfig, setNewAgentConfig] = useState<Partial<Agent>>({});
  const [newTaskConfig, setNewTaskConfig] = useState<Partial<Task>>({});
  const [chatInput, setChatInput] = useState('');
  const [isVoiceEnabled, setIsVoiceEnabled] = useState(false);
  const [filterAgentType, setFilterAgentType] = useState<string>('all');
  const [filterAgentStatus, setFilterAgentStatus] = useState<string>('all');
  const [searchQuery, setSearchQuery] = useState('');
  const [sortBy, setSortBy] = useState<'name' | 'performance' | 'activity' | 'created'>('name');
  const [sortDirection, setSortDirection] = useState<'asc' | 'desc'>('asc');
  const [autoAssignTasks, setAutoAssignTasks] = useState(true);
  const [realTimeUpdates, setRealTimeUpdates] = useState(true);
  
  const chatContainerRef = useRef<HTMLDivElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // Mock Data Generation
  const generateMockAgents = (): Agent[] => {
    return [
      {
        id: 'agent-001',
        name: 'Market Analyst Alpha',
        type: 'analysis',
        status: 'active',
        description: 'Advanced market analysis and pattern recognition agent',
        capabilities: ['Technical Analysis', 'Pattern Recognition', 'Sentiment Analysis', 'News Processing'],
        configuration: {
          parameters: { confidence_threshold: 0.8, lookback_period: 30 },
          endpoints: ['/api/market-data', '/api/news'],
          permissions: ['read_market_data', 'generate_alerts'],
          maxConcurrentTasks: 5,
          timeoutMs: 30000,
          retryAttempts: 3,
          learningEnabled: true,
          loggingLevel: 'info'
        },
        performance: {
          totalTasks: 1247,
          completedTasks: 1189,
          failedTasks: 58,
          averageExecutionTime: 2.3,
          successRate: 95.3,
          errorRate: 4.7,
          throughput: 24.5,
          responseTime: 450,
          accuracy: 92.1,
          userSatisfaction: 4.2,
          costEfficiency: 0.15,
          learningProgress: 78.5
        },
        resources: {
          cpu: { usage: 45, allocated: 2, limit: 4 },
          memory: { usage: 1.2, allocated: 2, limit: 4 },
          storage: { usage: 0.8, allocated: 5, limit: 10 },
          network: { bandwidth: 50, latency: 12, requests: 1250 }
        },
        createdAt: new Date('2024-01-15'),
        lastActive: new Date(Date.now() - 1000 * 60 * 5),
        version: '2.1.3',
        isActive: true,
        priority: 'high',
        owner: 'System',
        tags: ['analysis', 'market', 'ai', 'active']
      },
      {
        id: 'agent-002',
        name: 'Portfolio Guardian',
        type: 'risk',
        status: 'active',
        description: 'Real-time risk monitoring and portfolio protection agent',
        capabilities: ['Risk Assessment', 'Position Monitoring', 'Stop Loss Management', 'Stress Testing'],
        configuration: {
          parameters: { risk_tolerance: 0.02, max_drawdown: 0.15 },
          endpoints: ['/api/portfolio', '/api/positions'],
          permissions: ['read_portfolio', 'manage_risk', 'execute_orders'],
          maxConcurrentTasks: 10,
          timeoutMs: 5000,
          retryAttempts: 2,
          learningEnabled: true,
          loggingLevel: 'warn'
        },
        performance: {
          totalTasks: 2156,
          completedTasks: 2089,
          failedTasks: 67,
          averageExecutionTime: 0.8,
          successRate: 96.9,
          errorRate: 3.1,
          throughput: 45.2,
          responseTime: 180,
          accuracy: 94.7,
          userSatisfaction: 4.5,
          costEfficiency: 0.08,
          learningProgress: 85.2
        },
        resources: {
          cpu: { usage: 28, allocated: 1, limit: 2 },
          memory: { usage: 0.8, allocated: 1.5, limit: 3 },
          storage: { usage: 0.3, allocated: 2, limit: 5 },
          network: { bandwidth: 30, latency: 8, requests: 2890 }
        },
        createdAt: new Date('2024-01-10'),
        lastActive: new Date(Date.now() - 1000 * 30),
        version: '1.8.7',
        isActive: true,
        priority: 'critical',
        owner: 'System',
        tags: ['risk', 'portfolio', 'guardian', 'active']
      },
      {
        id: 'agent-003',
        name: 'Trade Executor Beta',
        type: 'trading',
        status: 'busy',
        description: 'Intelligent order execution and trade management agent',
        capabilities: ['Order Execution', 'TWAP/VWAP', 'Slippage Minimization', 'Market Timing'],
        configuration: {
          parameters: { max_order_size: 10000, slippage_tolerance: 0.005 },
          endpoints: ['/api/trading', '/api/orders'],
          permissions: ['execute_orders', 'manage_positions', 'access_market_data'],
          maxConcurrentTasks: 8,
          timeoutMs: 15000,
          retryAttempts: 5,
          learningEnabled: true,
          loggingLevel: 'info'
        },
        performance: {
          totalTasks: 856,
          completedTasks: 823,
          failedTasks: 33,
          averageExecutionTime: 3.7,
          successRate: 96.1,
          errorRate: 3.9,
          throughput: 18.3,
          responseTime: 680,
          accuracy: 93.8,
          userSatisfaction: 4.1,
          costEfficiency: 0.22,
          learningProgress: 72.1
        },
        resources: {
          cpu: { usage: 72, allocated: 3, limit: 4 },
          memory: { usage: 2.1, allocated: 3, limit: 6 },
          storage: { usage: 1.2, allocated: 3, limit: 8 },
          network: { bandwidth: 80, latency: 15, requests: 1780 }
        },
        createdAt: new Date('2024-02-01'),
        lastActive: new Date(Date.now() - 1000 * 10),
        version: '3.0.1',
        isActive: true,
        priority: 'high',
        owner: 'User',
        tags: ['trading', 'execution', 'beta', 'busy']
      },
      {
        id: 'agent-004',
        name: 'Research Assistant',
        type: 'research',
        status: 'idle',
        description: 'Comprehensive market research and data collection agent',
        capabilities: ['Data Mining', 'Report Generation', 'Competitive Analysis', 'Trend Identification'],
        configuration: {
          parameters: { research_depth: 'comprehensive', sources: 'multiple' },
          endpoints: ['/api/research', '/api/news', '/api/fundamentals'],
          permissions: ['read_market_data', 'generate_reports', 'access_research'],
          maxConcurrentTasks: 3,
          timeoutMs: 60000,
          retryAttempts: 2,
          learningEnabled: false,
          loggingLevel: 'debug'
        },
        performance: {
          totalTasks: 324,
          completedTasks: 298,
          failedTasks: 26,
          averageExecutionTime: 12.5,
          successRate: 92.0,
          errorRate: 8.0,
          throughput: 6.7,
          responseTime: 2400,
          accuracy: 89.3,
          userSatisfaction: 3.8,
          costEfficiency: 0.45,
          learningProgress: 45.6
        },
        resources: {
          cpu: { usage: 15, allocated: 1, limit: 2 },
          memory: { usage: 0.5, allocated: 1, limit: 2 },
          storage: { usage: 2.8, allocated: 10, limit: 20 },
          network: { bandwidth: 25, latency: 20, requests: 450 }
        },
        createdAt: new Date('2024-01-20'),
        lastActive: new Date(Date.now() - 1000 * 60 * 15),
        version: '1.5.2',
        isActive: false,
        priority: 'medium',
        owner: 'User',
        tags: ['research', 'assistant', 'data', 'idle']
      },
      {
        id: 'agent-005',
        name: 'Behavioral Coach',
        type: 'behavioral',
        status: 'active',
        description: 'Trading psychology and behavioral pattern analysis agent',
        capabilities: ['Emotion Detection', 'Behavioral Analysis', 'Coaching', 'Pattern Recognition'],
        configuration: {
          parameters: { sensitivity: 0.7, intervention_threshold: 0.8 },
          endpoints: ['/api/behavioral', '/api/user-activity'],
          permissions: ['read_user_data', 'send_notifications', 'generate_insights'],
          maxConcurrentTasks: 4,
          timeoutMs: 10000,
          retryAttempts: 3,
          learningEnabled: true,
          loggingLevel: 'info'
        },
        performance: {
          totalTasks: 567,
          completedTasks: 534,
          failedTasks: 33,
          averageExecutionTime: 4.2,
          successRate: 94.2,
          errorRate: 5.8,
          throughput: 12.8,
          responseTime: 520,
          accuracy: 91.5,
          userSatisfaction: 4.3,
          costEfficiency: 0.18,
          learningProgress: 82.3
        },
        resources: {
          cpu: { usage: 38, allocated: 2, limit: 3 },
          memory: { usage: 1.5, allocated: 2, limit: 4 },
          storage: { usage: 0.6, allocated: 3, limit: 6 },
          network: { bandwidth: 35, latency: 10, requests: 890 }
        },
        createdAt: new Date('2024-02-15'),
        lastActive: new Date(Date.now() - 1000 * 60 * 2),
        version: '2.2.1',
        isActive: true,
        priority: 'medium',
        owner: 'System',
        tags: ['behavioral', 'coach', 'psychology', 'active']
      }
    ];
  };

  const generateMockTasks = (): Task[] => {
    return [
      {
        id: 'task-001',
        title: 'Analyze NIFTY 50 Trends',
        description: 'Comprehensive technical analysis of NIFTY 50 index with pattern recognition',
        type: 'analysis',
        status: 'running',
        priority: 'high',
        assignedAgents: ['agent-001'],
        createdBy: 'user@example.com',
        createdAt: new Date(Date.now() - 1000 * 60 * 30),
        startedAt: new Date(Date.now() - 1000 * 60 * 25),
        estimatedDuration: 45,
        progress: 68,
        dependencies: [],
        tags: ['analysis', 'nifty', 'technical'],
        metadata: { symbol: 'NIFTY50', timeframe: '1D' }
      },
      {
        id: 'task-002',
        title: 'Portfolio Risk Assessment',
        description: 'Real-time risk evaluation of current portfolio positions',
        type: 'monitoring',
        status: 'completed',
        priority: 'critical',
        assignedAgents: ['agent-002'],
        createdBy: 'system',
        createdAt: new Date(Date.now() - 1000 * 60 * 60),
        startedAt: new Date(Date.now() - 1000 * 60 * 58),
        completedAt: new Date(Date.now() - 1000 * 60 * 5),
        estimatedDuration: 15,
        actualDuration: 12,
        progress: 100,
        result: { riskScore: 0.23, recommendation: 'Moderate risk, within limits' },
        dependencies: [],
        tags: ['risk', 'portfolio', 'monitoring'],
        metadata: { portfolioValue: 1500000, positions: 12 }
      },
      {
        id: 'task-003',
        title: 'Execute RELIANCE Buy Order',
        description: 'Execute buy order for 100 shares of RELIANCE with optimal timing',
        type: 'trading',
        status: 'running',
        priority: 'high',
        assignedAgents: ['agent-003'],
        createdBy: 'user@example.com',
        createdAt: new Date(Date.now() - 1000 * 60 * 10),
        startedAt: new Date(Date.now() - 1000 * 60 * 8),
        estimatedDuration: 20,
        progress: 45,
        dependencies: [],
        tags: ['trading', 'reliance', 'buy'],
        metadata: { symbol: 'RELIANCE', quantity: 100, orderType: 'MARKET' }
      },
      {
        id: 'task-004',
        title: 'Banking Sector Research',
        description: 'Comprehensive analysis of banking sector performance and outlook',
        type: 'research',
        status: 'pending',
        priority: 'medium',
        assignedAgents: ['agent-004'],
        createdBy: 'user@example.com',
        createdAt: new Date(Date.now() - 1000 * 60 * 5),
        estimatedDuration: 120,
        progress: 0,
        dependencies: [],
        tags: ['research', 'banking', 'sector'],
        metadata: { sector: 'BANKING', analysisType: 'COMPREHENSIVE' }
      },
      {
        id: 'task-005',
        title: 'Behavioral Pattern Analysis',
        description: 'Analyze recent trading patterns for emotional bias detection',
        type: 'analysis',
        status: 'assigned',
        priority: 'medium',
        assignedAgents: ['agent-005'],
        createdBy: 'system',
        createdAt: new Date(Date.now() - 1000 * 60 * 15),
        estimatedDuration: 30,
        progress: 0,
        dependencies: [],
        tags: ['behavioral', 'pattern', 'analysis'],
        metadata: { analysisWindow: '7days', focusArea: 'emotion_detection' }
      }
    ];
  };

  const generateMockConversations = (): AgentConversation[] => {
    return [
      {
        id: 'conv-001',
        title: 'Market Analysis Discussion',
        participants: [
          { type: 'user', id: 'user-1', name: 'You' },
          { type: 'agent', id: 'agent-001', name: 'Market Analyst Alpha' }
        ],
        messages: [
          {
            id: 'msg-001',
            sender: 'user',
            content: 'What do you think about the current market trend?',
            timestamp: new Date(Date.now() - 1000 * 60 * 10),
            type: 'text'
          },
          {
            id: 'msg-002',
            sender: 'agent',
            agentId: 'agent-001',
            content: 'Based on my analysis, the market is showing bullish momentum with strong support at 19,800 levels. I\'ve identified a potential breakout pattern forming.',
            timestamp: new Date(Date.now() - 1000 * 60 * 9),
            type: 'text',
            metadata: {
              confidence: 87,
              suggestions: ['Consider long positions', 'Watch for volume confirmation']
            }
          },
          {
            id: 'msg-003',
            sender: 'user',
            content: 'Can you analyze RELIANCE specifically?',
            timestamp: new Date(Date.now() - 1000 * 60 * 8),
            type: 'command',
            metadata: { command: 'analyze RELIANCE' }
          },
          {
            id: 'msg-004',
            sender: 'agent',
            agentId: 'agent-001',
            content: 'RELIANCE is showing strong fundamentals with a price target of ₹2,850. Technical indicators suggest a buy signal with RSI at 58 and MACD turning positive.',
            timestamp: new Date(Date.now() - 1000 * 60 * 7),
            type: 'result',
            metadata: {
              taskId: 'task-006',
              confidence: 92
            }
          }
        ],
        isActive: true,
        createdAt: new Date(Date.now() - 1000 * 60 * 60),
        lastMessage: new Date(Date.now() - 1000 * 60 * 7),
        tags: ['analysis', 'market', 'reliance']
      },
      {
        id: 'conv-002',
        title: 'Risk Management Chat',
        participants: [
          { type: 'user', id: 'user-1', name: 'You' },
          { type: 'agent', id: 'agent-002', name: 'Portfolio Guardian' }
        ],
        messages: [
          {
            id: 'msg-005',
            sender: 'agent',
            agentId: 'agent-002',
            content: '⚠️ Alert: Your portfolio exposure to banking sector has increased to 35%. Consider rebalancing.',
            timestamp: new Date(Date.now() - 1000 * 60 * 30),
            type: 'text'
          },
          {
            id: 'msg-006',
            sender: 'user',
            content: 'What do you recommend?',
            timestamp: new Date(Date.now() - 1000 * 60 * 28),
            type: 'text'
          },
          {
            id: 'msg-007',
            sender: 'agent',
            agentId: 'agent-002',
            content: 'I recommend reducing banking exposure by 10% and diversifying into IT or FMCG sectors. Would you like me to suggest specific trades?',
            timestamp: new Date(Date.now() - 1000 * 60 * 27),
            type: 'text',
            metadata: {
              suggestions: ['Sell HDFCBANK partial', 'Buy INFY', 'Buy HINDUNILVR']
            }
          }
        ],
        isActive: true,
        createdAt: new Date(Date.now() - 1000 * 60 * 60 * 2),
        lastMessage: new Date(Date.now() - 1000 * 60 * 27),
        tags: ['risk', 'portfolio', 'rebalancing']
      }
    ];
  };

  // Initialize Mock Data
  useEffect(() => {
    setAgents(generateMockAgents());
    setTasks(generateMockTasks());
    setConversations(generateMockConversations());
    setSelectedConversation(generateMockConversations()[0]);
  }, []);

  // Real-time updates simulation
  useEffect(() => {
    if (!realTimeUpdates) return;

    const interval = setInterval(() => {
      // Update agent performance and resources
      setAgents(prev => prev.map(agent => ({
        ...agent,
        resources: {
          ...agent.resources,
          cpu: {
            ...agent.resources.cpu,
            usage: Math.max(5, Math.min(95, agent.resources.cpu.usage + (Math.random() - 0.5) * 10))
          },
          memory: {
            ...agent.resources.memory,
            usage: Math.max(0.1, Math.min(agent.resources.memory.limit * 0.9, 
              agent.resources.memory.usage + (Math.random() - 0.5) * 0.2))
          }
        },
        lastActive: agent.status === 'active' ? new Date() : agent.lastActive
      })));

      // Update task progress
      setTasks(prev => prev.map(task => {
        if (task.status === 'running' && task.progress < 100) {
          const newProgress = Math.min(100, task.progress + Math.random() * 15);
          return {
            ...task,
            progress: newProgress,
            status: newProgress >= 100 ? 'completed' : 'running',
            completedAt: newProgress >= 100 ? new Date() : undefined,
            actualDuration: newProgress >= 100 ? 
              Math.round((Date.now() - (task.startedAt?.getTime() || Date.now())) / 1000 / 60) : 
              undefined
          };
        }
        return task;
      }));
    }, 5000); // Update every 5 seconds

    return () => clearInterval(interval);
  }, [realTimeUpdates]);

  // Filtered and sorted data
  const filteredAgents = useMemo(() => {
    let filtered = agents;

    if (filterAgentType !== 'all') {
      filtered = filtered.filter(agent => agent.type === filterAgentType);
    }

    if (filterAgentStatus !== 'all') {
      filtered = filtered.filter(agent => agent.status === filterAgentStatus);
    }

    if (searchQuery) {
      filtered = filtered.filter(agent => 
        agent.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
        agent.description.toLowerCase().includes(searchQuery.toLowerCase()) ||
        agent.tags.some(tag => tag.toLowerCase().includes(searchQuery.toLowerCase()))
      );
    }

    // Sort
    filtered.sort((a, b) => {
      let aVal, bVal;
      switch (sortBy) {
        case 'name':
          aVal = a.name;
          bVal = b.name;
          break;
        case 'performance':
          aVal = a.performance.successRate;
          bVal = b.performance.successRate;
          break;
        case 'activity':
          aVal = a.lastActive.getTime();
          bVal = b.lastActive.getTime();
          break;
        case 'created':
          aVal = a.createdAt.getTime();
          bVal = b.createdAt.getTime();
          break;
        default:
          aVal = a.name;
          bVal = b.name;
      }

      if (typeof aVal === 'string' && typeof bVal === 'string') {
        return sortDirection === 'desc' ? bVal.localeCompare(aVal) : aVal.localeCompare(bVal);
      }

      return sortDirection === 'desc' ? Number(bVal) - Number(aVal) : Number(aVal) - Number(bVal);
    });

    return filtered;
  }, [agents, filterAgentType, filterAgentStatus, searchQuery, sortBy, sortDirection]);

  // Helper Functions
  const getStatusColor = (status: AgentStatus) => {
    switch (status) {
      case 'active': return '#4caf50';
      case 'busy': return '#ff9800';
      case 'idle': return '#2196f3';
      case 'error': return '#f44336';
      case 'offline': return '#9e9e9e';
      case 'initializing': return '#673ab7';
      case 'updating': return '#00bcd4';
      default: return '#9e9e9e';
    }
  };

  const getStatusIcon = (status: AgentStatus) => {
    switch (status) {
      case 'active': return <CheckCircleIcon sx={{ color: '#4caf50' }} />;
      case 'busy': return <SpeedIcon sx={{ color: '#ff9800' }} />;
      case 'idle': return <PauseIcon sx={{ color: '#2196f3' }} />;
      case 'error': return <ErrorIcon sx={{ color: '#f44336' }} />;
      case 'offline': return <VisibilityOffIcon sx={{ color: '#9e9e9e' }} />;
      case 'initializing': return <CircularProgress size={16} sx={{ color: '#673ab7' }} />;
      case 'updating': return <RefreshIcon sx={{ color: '#00bcd4' }} />;
      default: return <InfoIcon sx={{ color: '#9e9e9e' }} />;
    }
  };

  const getTypeIcon = (type: AgentType) => {
    switch (type) {
      case 'trading': return <ShowChartIcon />;
      case 'analysis': return <AnalyticsIcon />;
      case 'research': return <AssignmentIcon />;
      case 'risk': return <WarningIcon />;
      case 'portfolio': return <AccountBalanceIcon />;
      case 'market_data': return <TrendingUpIcon />;
      case 'behavioral': return <PsychologyIcon />;
      case 'custom': return <CodeIcon />;
      default: return <SmartToyIcon />;
    }
  };

  const getTaskStatusColor = (status: TaskStatus) => {
    switch (status) {
      case 'completed': return '#4caf50';
      case 'running': return '#ff9800';
      case 'failed': return '#f44336';
      case 'pending': return '#9e9e9e';
      case 'assigned': return '#2196f3';
      case 'paused': return '#673ab7';
      case 'cancelled': return '#795548';
      default: return '#9e9e9e';
    }
  };

  const handleSendMessage = useCallback(() => {
    if (!chatInput.trim() || !selectedConversation) return;

    const newMessage: ChatMessage = {
      id: `msg-${Date.now()}`,
      sender: 'user',
      content: chatInput,
      timestamp: new Date(),
      type: chatInput.startsWith('/') ? 'command' : 'text',
      metadata: chatInput.startsWith('/') ? { command: chatInput.slice(1) } : undefined
    };

    setConversations(prev => prev.map(conv => 
      conv.id === selectedConversation.id 
        ? { ...conv, messages: [...conv.messages, newMessage], lastMessage: new Date() }
        : conv
    ));

    setSelectedConversation(prev => prev ? {
      ...prev,
      messages: [...prev.messages, newMessage],
      lastMessage: new Date()
    } : null);

    setChatInput('');

    // Simulate agent response
    setTimeout(() => {
      const agentResponse: ChatMessage = {
        id: `msg-${Date.now()}-response`,
        sender: 'agent',
        agentId: selectedConversation.participants.find(p => p.type === 'agent')?.id,
        content: `I understand your request: "${chatInput}". Let me process that for you.`,
        timestamp: new Date(),
        type: 'text',
        metadata: { confidence: 85 }
      };

      setConversations(prev => prev.map(conv => 
        conv.id === selectedConversation.id 
          ? { ...conv, messages: [...conv.messages, agentResponse], lastMessage: new Date() }
          : conv
      ));

      setSelectedConversation(prev => prev ? {
        ...prev,
        messages: [...prev.messages, agentResponse],
        lastMessage: new Date()
      } : null);
    }, 1000 + Math.random() * 2000);
  }, [chatInput, selectedConversation]);

  const handleKeyPress = (event: React.KeyboardEvent) => {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      handleSendMessage();
    }
  };

  // Auto-scroll chat to bottom
  useEffect(() => {
    if (chatContainerRef.current) {
      chatContainerRef.current.scrollTop = chatContainerRef.current.scrollHeight;
    }
  }, [selectedConversation?.messages]);

  const tabs = [
    { label: 'Agent Overview', icon: <SmartToyIcon /> },
    { label: 'Task Management', icon: <AssignmentIcon /> },
    { label: 'Agent Chat', icon: <ChatIcon /> },
    { label: 'Performance Analytics', icon: <AnalyticsIcon /> },
    { label: 'System Health', icon: <NetworkCheckIcon /> },
    { label: 'Agent Marketplace', icon: <BusinessIcon /> }
  ];

  return (
    <Box sx={{ flexGrow: 1, p: 3, backgroundColor: 'background.default', minHeight: '100vh' }}>
      {/* Header */}
      <Box sx={{ mb: 4 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
          <Box>
            <Typography variant="h4" sx={{ fontWeight: 600, display: 'flex', alignItems: 'center', gap: 1 }}>
              <SmartToyIcon color="primary" sx={{ fontSize: 36 }} />
              Agent Dashboard & Chat
            </Typography>
            <Typography variant="subtitle1" color="text.secondary">
              AI agent orchestration, management, and natural language interaction
            </Typography>
          </Box>
          
          <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
            <FormControlLabel
              control={
                <Switch
                  checked={realTimeUpdates}
                  onChange={(e) => setRealTimeUpdates(e.target.checked)}
                  color="primary"
                />
              }
              label="Real-time"
            />
            <FormControlLabel
              control={
                <Switch
                  checked={autoAssignTasks}
                  onChange={(e) => setAutoAssignTasks(e.target.checked)}
                  color="primary"
                />
              }
              label="Auto-assign"
            />
            <Button
              variant="outlined"
              startIcon={<AddIcon />}
              onClick={() => setShowAgentDialog(true)}
            >
              New Agent
            </Button>
            <Button
              variant="contained"
              startIcon={<RefreshIcon />}
              onClick={() => window.location.reload()}
            >
              Refresh
            </Button>
          </Box>
        </Box>

        {/* Quick Stats */}
        <Grid container spacing={2}>
          <Grid item xs={12} sm={6} md={2}>
            <Card sx={{ p: 2, textAlign: 'center' }}>
              <Typography variant="h6" color="success.main" sx={{ fontWeight: 600 }}>
                {agents.filter(a => a.status === 'active').length}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                Active Agents
              </Typography>
            </Card>
          </Grid>
          <Grid item xs={12} sm={6} md={2}>
            <Card sx={{ p: 2, textAlign: 'center' }}>
              <Typography variant="h6" color="warning.main" sx={{ fontWeight: 600 }}>
                {tasks.filter(t => t.status === 'running').length}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                Running Tasks
              </Typography>
            </Card>
          </Grid>
          <Grid item xs={12} sm={6} md={2}>
            <Card sx={{ p: 2, textAlign: 'center' }}>
              <Typography variant="h6" color="info.main" sx={{ fontWeight: 600 }}>
                {Math.round(agents.reduce((sum, a) => sum + a.performance.successRate, 0) / agents.length)}%
              </Typography>
              <Typography variant="caption" color="text.secondary">
                Avg Success Rate
              </Typography>
            </Card>
          </Grid>
          <Grid item xs={12} sm={6} md={2}>
            <Card sx={{ p: 2, textAlign: 'center' }}>
              <Typography variant="h6" color="secondary.main" sx={{ fontWeight: 600 }}>
                {Math.round(agents.reduce((sum, a) => sum + a.performance.throughput, 0))}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                Tasks/Hour
              </Typography>
            </Card>
          </Grid>
          <Grid item xs={12} sm={6} md={2}>
            <Card sx={{ p: 2, textAlign: 'center' }}>
              <Typography variant="h6" color="primary.main" sx={{ fontWeight: 600 }}>
                {conversations.filter(c => c.isActive).length}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                Active Chats
              </Typography>
            </Card>
          </Grid>
          <Grid item xs={12} sm={6} md={2}>
            <Card sx={{ p: 2, textAlign: 'center' }}>
              <Typography variant="h6" color="text.primary" sx={{ fontWeight: 600 }}>
                {Math.round(agents.reduce((sum, a) => sum + a.resources.cpu.usage, 0) / agents.length)}%
              </Typography>
              <Typography variant="caption" color="text.secondary">
                Avg CPU Usage
              </Typography>
            </Card>
          </Grid>
        </Grid>
      </Box>

      {/* Tab Navigation */}
      <Paper sx={{ mb: 3 }}>
        <Tabs 
          value={activeTab} 
          onChange={(_, newValue) => setActiveTab(newValue)}
          variant="scrollable"
          scrollButtons="auto"
        >
          {tabs.map((tab, index) => (
            <Tab
              key={index}
              label={tab.label}
              icon={tab.icon}
              iconPosition="start"
            />
          ))}
        </Tabs>
      </Paper>

      {/* Tab Content */}
      <AnimatePresence mode="wait">
        {activeTab === 0 && (
          <motion.div
            key="agent-overview"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -20 }}
          >
            <AgentOverviewTab
              agents={filteredAgents}
              onAgentSelect={setSelectedAgent}
              filters={{
                type: filterAgentType,
                status: filterAgentStatus,
                search: searchQuery,
                sortBy,
                sortDirection
              }}
              onFiltersChange={{
                setType: setFilterAgentType,
                setStatus: setFilterAgentStatus,
                setSearch: setSearchQuery,
                setSortBy,
                setSortDirection
              }}
            />
          </motion.div>
        )}

        {activeTab === 1 && (
          <motion.div
            key="task-management"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -20 }}
          >
            <TaskManagementTab
              tasks={tasks}
              agents={agents}
              onCreateTask={() => setShowTaskDialog(true)}
              autoAssign={autoAssignTasks}
            />
          </motion.div>
        )}

        {activeTab === 2 && (
          <motion.div
            key="agent-chat"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -20 }}
          >
            <AgentChatTab
              conversations={conversations}
              selectedConversation={selectedConversation}
              onConversationSelect={setSelectedConversation}
              agents={agents}
              chatInput={chatInput}
              onChatInputChange={setChatInput}
              onSendMessage={handleSendMessage}
              onKeyPress={handleKeyPress}
              chatContainerRef={chatContainerRef}
              isVoiceEnabled={isVoiceEnabled}
              onToggleVoice={setIsVoiceEnabled}
            />
          </motion.div>
        )}

        {activeTab === 3 && (
          <motion.div
            key="performance-analytics"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -20 }}
          >
            <PerformanceAnalyticsTab
              agents={agents}
              tasks={tasks}
            />
          </motion.div>
        )}

        {activeTab === 4 && (
          <motion.div
            key="system-health"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -20 }}
          >
            <SystemHealthTab
              agents={agents}
            />
          </motion.div>
        )}

        {activeTab === 5 && (
          <motion.div
            key="agent-marketplace"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -20 }}
          >
            <AgentMarketplaceTab />
          </motion.div>
        )}
      </AnimatePresence>

      {/* Agent Creation Dialog */}
      <Dialog
        open={showAgentDialog}
        onClose={() => setShowAgentDialog(false)}
        maxWidth="md"
        fullWidth
      >
        <DialogTitle>Create New Agent</DialogTitle>
        <DialogContent>
          <Box sx={{ pt: 1 }}>
            <TextField
              fullWidth
              label="Agent Name"
              value={newAgentConfig.name || ''}
              onChange={(e) => setNewAgentConfig(prev => ({ ...prev, name: e.target.value }))}
              sx={{ mb: 2 }}
            />
            <FormControl fullWidth sx={{ mb: 2 }}>
              <InputLabel>Agent Type</InputLabel>
              <Select
                value={newAgentConfig.type || ''}
                label="Agent Type"
                onChange={(e) => setNewAgentConfig(prev => ({ ...prev, type: e.target.value as AgentType }))}
              >
                <MenuItem value="trading">Trading Agent</MenuItem>
                <MenuItem value="analysis">Analysis Agent</MenuItem>
                <MenuItem value="research">Research Agent</MenuItem>
                <MenuItem value="risk">Risk Management Agent</MenuItem>
                <MenuItem value="portfolio">Portfolio Agent</MenuItem>
                <MenuItem value="behavioral">Behavioral Agent</MenuItem>
                <MenuItem value="custom">Custom Agent</MenuItem>
              </Select>
            </FormControl>
            <TextField
              fullWidth
              multiline
              rows={3}
              label="Description"
              value={newAgentConfig.description || ''}
              onChange={(e) => setNewAgentConfig(prev => ({ ...prev, description: e.target.value }))}
              sx={{ mb: 2 }}
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setShowAgentDialog(false)}>Cancel</Button>
          <Button 
            variant="contained" 
            onClick={() => {
              alert('Agent creation will be implemented with backend integration');
              setShowAgentDialog(false);
            }}
            disabled={!newAgentConfig.name || !newAgentConfig.type}
          >
            Create Agent
          </Button>
        </DialogActions>
      </Dialog>

      {/* Task Creation Dialog */}
      <Dialog
        open={showTaskDialog}
        onClose={() => setShowTaskDialog(false)}
        maxWidth="md"
        fullWidth
      >
        <DialogTitle>Create New Task</DialogTitle>
        <DialogContent>
          <Box sx={{ pt: 1 }}>
            <TextField
              fullWidth
              label="Task Title"
              value={newTaskConfig.title || ''}
              onChange={(e) => setNewTaskConfig(prev => ({ ...prev, title: e.target.value }))}
              sx={{ mb: 2 }}
            />
            <FormControl fullWidth sx={{ mb: 2 }}>
              <InputLabel>Task Type</InputLabel>
              <Select
                value={newTaskConfig.type || ''}
                label="Task Type"
                onChange={(e) => setNewTaskConfig(prev => ({ ...prev, type: e.target.value as TaskType }))}
              >
                <MenuItem value="analysis">Analysis</MenuItem>
                <MenuItem value="trading">Trading</MenuItem>
                <MenuItem value="research">Research</MenuItem>
                <MenuItem value="monitoring">Monitoring</MenuItem>
                <MenuItem value="alert">Alert</MenuItem>
                <MenuItem value="report">Report</MenuItem>
                <MenuItem value="optimization">Optimization</MenuItem>
              </Select>
            </FormControl>
            <TextField
              fullWidth
              multiline
              rows={3}
              label="Description"
              value={newTaskConfig.description || ''}
              onChange={(e) => setNewTaskConfig(prev => ({ ...prev, description: e.target.value }))}
              sx={{ mb: 2 }}
            />
            <FormControl fullWidth sx={{ mb: 2 }}>
              <InputLabel>Priority</InputLabel>
              <Select
                value={newTaskConfig.priority || 'medium'}
                label="Priority"
                onChange={(e) => setNewTaskConfig(prev => ({ ...prev, priority: e.target.value as any }))}
              >
                <MenuItem value="low">Low</MenuItem>
                <MenuItem value="medium">Medium</MenuItem>
                <MenuItem value="high">High</MenuItem>
                <MenuItem value="urgent">Urgent</MenuItem>
              </Select>
            </FormControl>
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setShowTaskDialog(false)}>Cancel</Button>
          <Button 
            variant="contained" 
            onClick={() => {
              alert('Task creation will be implemented with backend integration');
              setShowTaskDialog(false);
            }}
            disabled={!newTaskConfig.title || !newTaskConfig.type}
          >
            Create Task
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

// Placeholder components for tabs (detailed implementations would follow)
const AgentOverviewTab: React.FC<any> = ({ agents, onAgentSelect, filters, onFiltersChange }) => {
  const getStatusColor = (status: AgentStatus) => {
    switch (status) {
      case 'active': return '#4caf50';
      case 'busy': return '#ff9800';
      case 'idle': return '#2196f3';
      case 'error': return '#f44336';
      case 'offline': return '#9e9e9e';
      default: return '#9e9e9e';
    }
  };

  const getTypeIcon = (type: AgentType) => {
    switch (type) {
      case 'trading': return <ShowChartIcon />;
      case 'analysis': return <AnalyticsIcon />;
      case 'research': return <AssignmentIcon />;
      case 'risk': return <WarningIcon />;
      case 'behavioral': return <PsychologyIcon />;
      default: return <SmartToyIcon />;
    }
  };

  return (
    <Box>
      {/* Filters */}
      <Paper sx={{ p: 3, mb: 3 }}>
        <Grid container spacing={2} alignItems="center">
          <Grid item xs={12} md={3}>
            <TextField
              fullWidth
              label="Search Agents"
              value={filters.search}
              onChange={(e) => onFiltersChange.setSearch(e.target.value)}
              InputProps={{
                startAdornment: <SearchIcon sx={{ mr: 1, color: 'action.active' }} />
              }}
            />
          </Grid>
          <Grid item xs={12} md={2}>
            <FormControl fullWidth>
              <InputLabel>Type</InputLabel>
              <Select
                value={filters.type}
                label="Type"
                onChange={(e) => onFiltersChange.setType(e.target.value)}
              >
                <MenuItem value="all">All Types</MenuItem>
                <MenuItem value="trading">Trading</MenuItem>
                <MenuItem value="analysis">Analysis</MenuItem>
                <MenuItem value="research">Research</MenuItem>
                <MenuItem value="risk">Risk</MenuItem>
                <MenuItem value="behavioral">Behavioral</MenuItem>
              </Select>
            </FormControl>
          </Grid>
          <Grid item xs={12} md={2}>
            <FormControl fullWidth>
              <InputLabel>Status</InputLabel>
              <Select
                value={filters.status}
                label="Status"
                onChange={(e) => onFiltersChange.setStatus(e.target.value)}
              >
                <MenuItem value="all">All Status</MenuItem>
                <MenuItem value="active">Active</MenuItem>
                <MenuItem value="busy">Busy</MenuItem>
                <MenuItem value="idle">Idle</MenuItem>
                <MenuItem value="error">Error</MenuItem>
                <MenuItem value="offline">Offline</MenuItem>
              </Select>
            </FormControl>
          </Grid>
        </Grid>
      </Paper>

      {/* Agent Grid */}
      <Grid container spacing={3}>
        {agents.map((agent) => (
          <Grid item xs={12} md={6} lg={4} key={agent.id}>
            <Card 
              sx={{ 
                p: 3, 
                cursor: 'pointer',
                '&:hover': { transform: 'translateY(-2px)', boxShadow: 3 },
                transition: 'all 0.2s',
                borderLeft: `4px solid ${getStatusColor(agent.status)}`
              }}
              onClick={() => onAgentSelect(agent)}
            >
              <Box sx={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', mb: 2 }}>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                  <Avatar sx={{ bgcolor: getStatusColor(agent.status) }}>
                    {getTypeIcon(agent.type)}
                  </Avatar>
                  <Box>
                    <Typography variant="h6" sx={{ fontWeight: 600 }}>
                      {agent.name}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      {agent.type.charAt(0).toUpperCase() + agent.type.slice(1)} Agent
                    </Typography>
                  </Box>
                </Box>
                <Chip 
                  label={agent.status}
                  size="small"
                  sx={{ 
                    backgroundColor: getStatusColor(agent.status) + '20',
                    color: getStatusColor(agent.status),
                    textTransform: 'capitalize'
                  }}
                />
              </Box>

              <Typography variant="body2" color="text.secondary" sx={{ mb: 2, minHeight: 40 }}>
                {agent.description}
              </Typography>

              <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
                <Box sx={{ textAlign: 'center' }}>
                  <Typography variant="h6" color="success.main">
                    {agent.performance.successRate.toFixed(1)}%
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
                    Success Rate
                  </Typography>
                </Box>
                <Box sx={{ textAlign: 'center' }}>
                  <Typography variant="h6" color="info.main">
                    {agent.performance.completedTasks}
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
                    Tasks Done
                  </Typography>
                </Box>
                <Box sx={{ textAlign: 'center' }}>
                  <Typography variant="h6" color="warning.main">
                    {agent.performance.responseTime}ms
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
                    Avg Response
                  </Typography>
                </Box>
              </Box>

              <LinearProgress 
                variant="determinate" 
                value={agent.resources.cpu.usage} 
                sx={{ mb: 1 }}
              />
              <Typography variant="caption" color="text.secondary">
                CPU: {agent.resources.cpu.usage}% | Memory: {agent.resources.memory.usage.toFixed(1)}GB
              </Typography>

              <Box sx={{ mt: 2, display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                {agent.capabilities.slice(0, 3).map((capability, index) => (
                  <Chip key={index} label={capability} size="small" variant="outlined" />
                ))}
                {agent.capabilities.length > 3 && (
                  <Chip label={`+${agent.capabilities.length - 3}`} size="small" variant="outlined" />
                )}
              </Box>
            </Card>
          </Grid>
        ))}
      </Grid>
    </Box>
  );
};

const TaskManagementTab: React.FC<any> = () => <Box>Task management interface coming soon...</Box>;
const AgentChatTab: React.FC<any> = () => <Box>Agent chat interface coming soon...</Box>;
const PerformanceAnalyticsTab: React.FC<any> = () => <Box>Performance analytics coming soon...</Box>;
const SystemHealthTab: React.FC<any> = () => <Box>System health monitoring coming soon...</Box>;
const AgentMarketplaceTab: React.FC<any> = () => <Box>Agent marketplace coming soon...</Box>;

export default AgentDashboard;