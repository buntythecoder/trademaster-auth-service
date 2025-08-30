import React, { useState, useCallback, useRef, useEffect, useMemo } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { 
  Plus,
  Minus,
  X,
  Save,
  Play,
  Pause,
  RotateCcw,
  Code,
  Eye,
  Settings,
  Layers,
  Zap,
  Calculator,
  TrendingUp,
  Activity,
  BarChart3,
  LineChart,
  Target,
  Cpu,
  Database,
  Filter,
  Shuffle,
  GitBranch,
  Download,
  Upload,
  Copy,
  Edit3,
  Trash2,
  AlertTriangle,
  CheckCircle2,
  Clock,
  Palette,
  MousePointer,
  Move,
  ArrowRight
} from 'lucide-react';

interface IndicatorNode {
  id: string;
  type: 'input' | 'operator' | 'function' | 'output';
  category: 'price' | 'volume' | 'math' | 'logic' | 'technical' | 'custom';
  name: string;
  displayName: string;
  inputs: string[];
  outputs: string[];
  parameters: Record<string, any>;
  position: { x: number; y: number };
  color: string;
  description: string;
  formula?: string;
}

interface IndicatorConnection {
  id: string;
  sourceNodeId: string;
  sourceOutput: string;
  targetNodeId: string;
  targetInput: string;
}

interface CustomIndicator {
  id: string;
  name: string;
  description: string;
  category: 'trend' | 'momentum' | 'volatility' | 'volume' | 'custom';
  nodes: IndicatorNode[];
  connections: IndicatorConnection[];
  parameters: Record<string, any>;
  style: {
    color: string;
    thickness: number;
    lineStyle: 'solid' | 'dashed' | 'dotted';
  };
  overlayChart: boolean;
  createdAt: Date;
  modifiedAt: Date;
  author: string;
  version: string;
  isPublic: boolean;
}

interface BacktestResult {
  returns: number[];
  maxDrawdown: number;
  sharpeRatio: number;
  winRate: number;
  totalTrades: number;
  avgWin: number;
  avgLoss: number;
  profitFactor: number;
}

interface StrategySignal {
  timestamp: Date;
  type: 'buy' | 'sell' | 'hold';
  price: number;
  confidence: number;
  reason: string;
}

const NODE_TYPES: Record<string, IndicatorNode[]> = {
  inputs: [
    {
      id: 'price_open',
      type: 'input',
      category: 'price',
      name: 'Open',
      displayName: 'Open Price',
      inputs: [],
      outputs: ['value'],
      parameters: {},
      position: { x: 0, y: 0 },
      color: '#3B82F6',
      description: 'Opening price of each candle'
    },
    {
      id: 'price_high',
      type: 'input',
      category: 'price',
      name: 'High',
      displayName: 'High Price',
      inputs: [],
      outputs: ['value'],
      parameters: {},
      position: { x: 0, y: 0 },
      color: '#10B981',
      description: 'Highest price of each candle'
    },
    {
      id: 'price_low',
      type: 'input',
      category: 'price',
      name: 'Low',
      displayName: 'Low Price',
      inputs: [],
      outputs: ['value'],
      parameters: {},
      position: { x: 0, y: 0 },
      color: '#EF4444',
      description: 'Lowest price of each candle'
    },
    {
      id: 'price_close',
      type: 'input',
      category: 'price',
      name: 'Close',
      displayName: 'Close Price',
      inputs: [],
      outputs: ['value'],
      parameters: {},
      position: { x: 0, y: 0 },
      color: '#F59E0B',
      description: 'Closing price of each candle'
    },
    {
      id: 'volume',
      type: 'input',
      category: 'volume',
      name: 'Volume',
      displayName: 'Volume',
      inputs: [],
      outputs: ['value'],
      parameters: {},
      position: { x: 0, y: 0 },
      color: '#8B5CF6',
      description: 'Trading volume for each period'
    }
  ],
  functions: [
    {
      id: 'sma',
      type: 'function',
      category: 'technical',
      name: 'SMA',
      displayName: 'Simple Moving Average',
      inputs: ['source', 'period'],
      outputs: ['value'],
      parameters: { period: 20 },
      position: { x: 0, y: 0 },
      color: '#3B82F6',
      description: 'Calculate simple moving average',
      formula: 'SUM(source, period) / period'
    },
    {
      id: 'ema',
      type: 'function',
      category: 'technical',
      name: 'EMA',
      displayName: 'Exponential Moving Average',
      inputs: ['source', 'period'],
      outputs: ['value'],
      parameters: { period: 12 },
      position: { x: 0, y: 0 },
      color: '#10B981',
      description: 'Calculate exponential moving average',
      formula: 'EMA = (K × [C - EMA prev]) + EMA prev'
    },
    {
      id: 'rsi',
      type: 'function',
      category: 'technical',
      name: 'RSI',
      displayName: 'Relative Strength Index',
      inputs: ['source', 'period'],
      outputs: ['value'],
      parameters: { period: 14 },
      position: { x: 0, y: 0 },
      color: '#F59E0B',
      description: 'Calculate RSI momentum oscillator',
      formula: 'RSI = 100 - (100 / (1 + RS))'
    },
    {
      id: 'bollinger_bands',
      type: 'function',
      category: 'technical',
      name: 'BB',
      displayName: 'Bollinger Bands',
      inputs: ['source', 'period', 'deviation'],
      outputs: ['upper', 'middle', 'lower'],
      parameters: { period: 20, deviation: 2 },
      position: { x: 0, y: 0 },
      color: '#8B5CF6',
      description: 'Calculate Bollinger Bands',
      formula: 'Upper = MA + (StdDev × multiplier)'
    },
    {
      id: 'macd',
      type: 'function',
      category: 'technical',
      name: 'MACD',
      displayName: 'MACD',
      inputs: ['source', 'fast', 'slow', 'signal'],
      outputs: ['macd', 'signal', 'histogram'],
      parameters: { fast: 12, slow: 26, signal: 9 },
      position: { x: 0, y: 0 },
      color: '#EC4899',
      description: 'Moving Average Convergence Divergence',
      formula: 'MACD = EMA12 - EMA26'
    }
  ],
  operators: [
    {
      id: 'add',
      type: 'operator',
      category: 'math',
      name: 'Add',
      displayName: 'Addition',
      inputs: ['a', 'b'],
      outputs: ['result'],
      parameters: {},
      position: { x: 0, y: 0 },
      color: '#6B7280',
      description: 'Add two values',
      formula: 'a + b'
    },
    {
      id: 'subtract',
      type: 'operator',
      category: 'math',
      name: 'Subtract',
      displayName: 'Subtraction',
      inputs: ['a', 'b'],
      outputs: ['result'],
      parameters: {},
      position: { x: 0, y: 0 },
      color: '#6B7280',
      description: 'Subtract b from a',
      formula: 'a - b'
    },
    {
      id: 'multiply',
      type: 'operator',
      category: 'math',
      name: 'Multiply',
      displayName: 'Multiplication',
      inputs: ['a', 'b'],
      outputs: ['result'],
      parameters: {},
      position: { x: 0, y: 0 },
      color: '#6B7280',
      description: 'Multiply two values',
      formula: 'a × b'
    },
    {
      id: 'divide',
      type: 'operator',
      category: 'math',
      name: 'Divide',
      displayName: 'Division',
      inputs: ['a', 'b'],
      outputs: ['result'],
      parameters: {},
      position: { x: 0, y: 0 },
      color: '#6B7280',
      description: 'Divide a by b',
      formula: 'a ÷ b'
    },
    {
      id: 'greater_than',
      type: 'operator',
      category: 'logic',
      name: 'GT',
      displayName: 'Greater Than',
      inputs: ['a', 'b'],
      outputs: ['result'],
      parameters: {},
      position: { x: 0, y: 0 },
      color: '#F59E0B',
      description: 'Check if a > b',
      formula: 'a > b'
    },
    {
      id: 'cross_above',
      type: 'operator',
      category: 'logic',
      name: 'CrossUp',
      displayName: 'Cross Above',
      inputs: ['a', 'b'],
      outputs: ['signal'],
      parameters: {},
      position: { x: 0, y: 0 },
      color: '#10B981',
      description: 'Signal when a crosses above b',
      formula: 'a[0] > b[0] AND a[1] <= b[1]'
    }
  ],
  outputs: [
    {
      id: 'line_output',
      type: 'output',
      category: 'custom',
      name: 'Line',
      displayName: 'Line Output',
      inputs: ['value'],
      outputs: [],
      parameters: { color: '#3B82F6', thickness: 2, style: 'solid' },
      position: { x: 0, y: 0 },
      color: '#3B82F6',
      description: 'Display as line on chart'
    },
    {
      id: 'signal_output',
      type: 'output',
      category: 'custom',
      name: 'Signal',
      displayName: 'Signal Output',
      inputs: ['condition'],
      outputs: [],
      parameters: { buyColor: '#10B981', sellColor: '#EF4444' },
      position: { x: 0, y: 0 },
      color: '#F59E0B',
      description: 'Display buy/sell signals'
    },
    {
      id: 'histogram_output',
      type: 'output',
      category: 'custom',
      name: 'Histogram',
      displayName: 'Histogram Output',
      inputs: ['value'],
      outputs: [],
      parameters: { color: '#8B5CF6', fillOpacity: 0.7 },
      position: { x: 0, y: 0 },
      color: '#8B5CF6',
      description: 'Display as histogram'
    }
  ]
};

const SAMPLE_STRATEGIES = [
  {
    name: 'Golden Cross Strategy',
    description: 'Buy when fast MA crosses above slow MA, sell when it crosses below',
    nodes: ['sma', 'sma', 'cross_above', 'cross_below', 'signal_output'],
    parameters: { fast_period: 50, slow_period: 200 }
  },
  {
    name: 'RSI Divergence',
    description: 'Identify bullish/bearish divergences between price and RSI',
    nodes: ['rsi', 'price_close', 'divergence_detector', 'signal_output'],
    parameters: { rsi_period: 14, lookback: 20 }
  },
  {
    name: 'Bollinger Band Squeeze',
    description: 'Detect low volatility periods and breakout signals',
    nodes: ['bollinger_bands', 'bandwidth', 'percentile', 'breakout_detector'],
    parameters: { bb_period: 20, bb_deviation: 2, squeeze_threshold: 20 }
  }
];

const CustomIndicatorBuilder: React.FC<{
  onIndicatorCreate?: (indicator: CustomIndicator) => void;
  onStrategyBacktest?: (strategy: CustomIndicator) => Promise<BacktestResult>;
}> = ({ onIndicatorCreate, onStrategyBacktest }) => {
  const [nodes, setNodes] = useState<IndicatorNode[]>([]);
  const [connections, setConnections] = useState<IndicatorConnection[]>([]);
  const [selectedNodeId, setSelectedNodeId] = useState<string | null>(null);
  const [draggedNodeId, setDraggedNodeId] = useState<string | null>(null);
  const [isConnecting, setIsConnecting] = useState(false);
  const [connectionStart, setConnectionStart] = useState<{
    nodeId: string;
    output: string;
    position: { x: number; y: number };
  } | null>(null);
  const [showNodeLibrary, setShowNodeLibrary] = useState(true);
  const [showCodeView, setShowCodeView] = useState(false);
  const [showBacktest, setShowBacktest] = useState(false);
  const [isRunning, setIsRunning] = useState(false);
  const [backtestResults, setBacktestResults] = useState<BacktestResult | null>(null);
  const [indicatorName, setIndicatorName] = useState('Custom Indicator');
  const [indicatorDescription, setIndicatorDescription] = useState('');
  const [mousePosition, setMousePosition] = useState({ x: 0, y: 0 });
  const [zoomLevel, setZoomLevel] = useState(1);
  const [panOffset, setPanOffset] = useState({ x: 0, y: 0 });
  
  const canvasRef = useRef<HTMLDivElement>(null);
  const svgRef = useRef<SVGSVGElement>(null);

  // Generate indicator code
  const generateCode = useMemo(() => {
    if (nodes.length === 0) return '';

    let code = `// ${indicatorName}\n// ${indicatorDescription}\n\n`;
    code += 'function calculateIndicator(data) {\n';
    code += '  const result = [];\n\n';

    // Generate code for each node
    nodes.forEach(node => {
      switch (node.type) {
        case 'input':
          code += `  // Input: ${node.displayName}\n`;
          break;
        case 'function':
          code += `  // ${node.displayName}\n`;
          if (node.formula) {
            code += `  // Formula: ${node.formula}\n`;
          }
          break;
        case 'operator':
          code += `  // ${node.displayName}: ${node.formula}\n`;
          break;
        case 'output':
          code += `  // Output: ${node.displayName}\n`;
          break;
      }
    });

    code += '\n  return result;\n}';
    return code;
  }, [nodes, indicatorName, indicatorDescription]);

  // Add node to canvas
  const addNode = useCallback((nodeType: IndicatorNode) => {
    const newNode: IndicatorNode = {
      ...nodeType,
      id: `${nodeType.id}_${Date.now()}`,
      position: {
        x: 200 + Math.random() * 300,
        y: 100 + Math.random() * 200
      }
    };
    setNodes(prev => [...prev, newNode]);
  }, []);

  // Remove node and its connections
  const removeNode = useCallback((nodeId: string) => {
    setNodes(prev => prev.filter(n => n.id !== nodeId));
    setConnections(prev => prev.filter(c => 
      c.sourceNodeId !== nodeId && c.targetNodeId !== nodeId
    ));
    if (selectedNodeId === nodeId) {
      setSelectedNodeId(null);
    }
  }, [selectedNodeId]);

  // Update node position
  const updateNodePosition = useCallback((nodeId: string, position: { x: number; y: number }) => {
    setNodes(prev => prev.map(node => 
      node.id === nodeId ? { ...node, position } : node
    ));
  }, []);

  // Start connection
  const startConnection = useCallback((nodeId: string, output: string, event: React.MouseEvent) => {
    const rect = canvasRef.current?.getBoundingClientRect();
    if (!rect) return;

    setIsConnecting(true);
    setConnectionStart({
      nodeId,
      output,
      position: {
        x: event.clientX - rect.left,
        y: event.clientY - rect.top
      }
    });
  }, []);

  // Complete connection
  const completeConnection = useCallback((nodeId: string, input: string) => {
    if (!connectionStart) return;

    // Check if connection already exists
    const existingConnection = connections.find(c =>
      c.sourceNodeId === connectionStart.nodeId &&
      c.sourceOutput === connectionStart.output &&
      c.targetNodeId === nodeId &&
      c.targetInput === input
    );

    if (!existingConnection) {
      const newConnection: IndicatorConnection = {
        id: `conn_${Date.now()}`,
        sourceNodeId: connectionStart.nodeId,
        sourceOutput: connectionStart.output,
        targetNodeId: nodeId,
        targetInput: input
      };
      setConnections(prev => [...prev, newConnection]);
    }

    setIsConnecting(false);
    setConnectionStart(null);
  }, [connectionStart, connections]);

  // Mouse event handlers
  const handleMouseMove = useCallback((event: React.MouseEvent) => {
    const rect = canvasRef.current?.getBoundingClientRect();
    if (!rect) return;

    const x = (event.clientX - rect.left - panOffset.x) / zoomLevel;
    const y = (event.clientY - rect.top - panOffset.y) / zoomLevel;
    setMousePosition({ x, y });

    if (draggedNodeId) {
      updateNodePosition(draggedNodeId, { x, y });
    }
  }, [draggedNodeId, updateNodePosition, zoomLevel, panOffset]);

  const handleMouseUp = useCallback(() => {
    setDraggedNodeId(null);
    if (isConnecting && !connectionStart) {
      setIsConnecting(false);
    }
  }, [isConnecting, connectionStart]);

  // Backtest the strategy
  const runBacktest = useCallback(async () => {
    if (!onStrategyBacktest) return;

    setIsRunning(true);
    try {
      const strategy: CustomIndicator = {
        id: `strategy_${Date.now()}`,
        name: indicatorName,
        description: indicatorDescription,
        category: 'custom',
        nodes,
        connections,
        parameters: {},
        style: { color: '#3B82F6', thickness: 2, lineStyle: 'solid' },
        overlayChart: true,
        createdAt: new Date(),
        modifiedAt: new Date(),
        author: 'User',
        version: '1.0.0',
        isPublic: false
      };

      const results = await onStrategyBacktest(strategy);
      setBacktestResults(results);
    } catch (error) {
      console.error('Backtest failed:', error);
    } finally {
      setIsRunning(false);
    }
  }, [nodes, connections, indicatorName, indicatorDescription, onStrategyBacktest]);

  // Save indicator
  const saveIndicator = useCallback(() => {
    if (!onIndicatorCreate) return;

    const indicator: CustomIndicator = {
      id: `indicator_${Date.now()}`,
      name: indicatorName,
      description: indicatorDescription,
      category: 'custom',
      nodes,
      connections,
      parameters: {},
      style: { color: '#3B82F6', thickness: 2, lineStyle: 'solid' },
      overlayChart: true,
      createdAt: new Date(),
      modifiedAt: new Date(),
      author: 'User',
      version: '1.0.0',
      isPublic: false
    };

    onIndicatorCreate(indicator);
  }, [nodes, connections, indicatorName, indicatorDescription, onIndicatorCreate]);

  // Render node
  const renderNode = (node: IndicatorNode) => {
    const isSelected = selectedNodeId === node.id;
    
    return (
      <motion.div
        key={node.id}
        className={`absolute bg-slate-800 border-2 rounded-lg shadow-lg cursor-move select-none ${
          isSelected ? 'border-blue-500 ring-2 ring-blue-500/30' : 'border-slate-600'
        }`}
        style={{
          left: node.position.x,
          top: node.position.y,
          transform: `scale(${zoomLevel})`,
          transformOrigin: 'top left'
        }}
        onMouseDown={(e) => {
          e.stopPropagation();
          setSelectedNodeId(node.id);
          setDraggedNodeId(node.id);
        }}
        whileHover={{ scale: zoomLevel * 1.05 }}
        layout
      >
        {/* Node Header */}
        <div 
          className="px-3 py-2 text-white text-sm font-medium border-b border-slate-600"
          style={{ backgroundColor: node.color }}
        >
          <div className="flex items-center justify-between">
            <span>{node.displayName}</span>
            <button
              onClick={(e) => {
                e.stopPropagation();
                removeNode(node.id);
              }}
              className="p-0.5 hover:bg-white/20 rounded"
            >
              <X size={12} />
            </button>
          </div>
        </div>

        {/* Node Body */}
        <div className="p-3">
          {/* Input Ports */}
          {node.inputs.map((input, index) => (
            <div key={input} className="flex items-center space-x-2 mb-2">
              <div
                className="w-3 h-3 bg-slate-400 rounded-full border-2 border-slate-600 cursor-pointer hover:bg-blue-400"
                onClick={() => {
                  if (isConnecting) {
                    completeConnection(node.id, input);
                  }
                }}
              />
              <span className="text-xs text-gray-300">{input}</span>
            </div>
          ))}

          {/* Parameters */}
          {Object.entries(node.parameters).map(([key, value]) => (
            <div key={key} className="mb-2">
              <label className="text-xs text-gray-400 block mb-1">{key}</label>
              <input
                type="number"
                value={value}
                onChange={(e) => {
                  const newValue = parseFloat(e.target.value) || 0;
                  setNodes(prev => prev.map(n => 
                    n.id === node.id 
                      ? { ...n, parameters: { ...n.parameters, [key]: newValue }}
                      : n
                  ));
                }}
                className="w-full px-2 py-1 bg-slate-700 border border-slate-600 rounded text-xs"
              />
            </div>
          ))}

          {/* Output Ports */}
          {node.outputs.map((output, index) => (
            <div key={output} className="flex items-center justify-end space-x-2 mt-2">
              <span className="text-xs text-gray-300">{output}</span>
              <div
                className="w-3 h-3 bg-slate-400 rounded-full border-2 border-slate-600 cursor-pointer hover:bg-green-400"
                onMouseDown={(e) => {
                  e.stopPropagation();
                  startConnection(node.id, output, e);
                }}
              />
            </div>
          ))}
        </div>
      </motion.div>
    );
  };

  // Render connection
  const renderConnection = (connection: IndicatorConnection) => {
    const sourceNode = nodes.find(n => n.id === connection.sourceNodeId);
    const targetNode = nodes.find(n => n.id === connection.targetNodeId);

    if (!sourceNode || !targetNode) return null;

    const sourceOutputIndex = sourceNode.outputs.indexOf(connection.sourceOutput);
    const targetInputIndex = targetNode.inputs.indexOf(connection.targetInput);

    const startX = sourceNode.position.x + 120;
    const startY = sourceNode.position.y + 60 + (sourceOutputIndex * 25);
    const endX = targetNode.position.x;
    const endY = targetNode.position.y + 60 + (targetInputIndex * 25);

    const controlX1 = startX + (endX - startX) * 0.5;
    const controlY1 = startY;
    const controlX2 = startX + (endX - startX) * 0.5;
    const controlY2 = endY;

    return (
      <path
        key={connection.id}
        d={`M ${startX} ${startY} C ${controlX1} ${controlY1}, ${controlX2} ${controlY2}, ${endX} ${endY}`}
        stroke="#3B82F6"
        strokeWidth="2"
        fill="none"
        className="pointer-events-none"
      />
    );
  };

  return (
    <div className="w-full h-full bg-gradient-to-br from-slate-900 to-slate-800 relative overflow-hidden">
      {/* Header */}
      <div className="absolute top-0 left-0 right-0 z-30 bg-slate-800/90 backdrop-blur border-b border-slate-700 p-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-4">
            <div className="flex items-center space-x-2">
              <Cpu className="text-purple-400" size={24} />
              <div>
                <input
                  type="text"
                  value={indicatorName}
                  onChange={(e) => setIndicatorName(e.target.value)}
                  className="bg-transparent text-xl font-bold border-none outline-none text-white"
                  placeholder="Indicator Name"
                />
                <input
                  type="text"
                  value={indicatorDescription}
                  onChange={(e) => setIndicatorDescription(e.target.value)}
                  className="bg-transparent text-sm text-gray-400 border-none outline-none block mt-1"
                  placeholder="Description..."
                />
              </div>
            </div>
          </div>

          <div className="flex items-center space-x-2">
            <button
              onClick={() => setShowNodeLibrary(!showNodeLibrary)}
              className={`p-2 rounded transition-colors ${
                showNodeLibrary ? 'bg-blue-600' : 'bg-slate-700 hover:bg-slate-600'
              }`}
              title="Node Library"
            >
              <Layers size={16} />
            </button>
            
            <button
              onClick={() => setShowCodeView(!showCodeView)}
              className={`p-2 rounded transition-colors ${
                showCodeView ? 'bg-blue-600' : 'bg-slate-700 hover:bg-slate-600'
              }`}
              title="Code View"
            >
              <Code size={16} />
            </button>
            
            <button
              onClick={() => setShowBacktest(!showBacktest)}
              className={`p-2 rounded transition-colors ${
                showBacktest ? 'bg-blue-600' : 'bg-slate-700 hover:bg-slate-600'
              }`}
              title="Backtest"
            >
              <Activity size={16} />
            </button>
            
            <button
              onClick={runBacktest}
              disabled={isRunning || nodes.length === 0}
              className="flex items-center space-x-2 px-4 py-2 bg-green-600 hover:bg-green-700 
                disabled:bg-slate-600 disabled:cursor-not-allowed rounded transition-colors"
            >
              {isRunning ? <Clock size={16} className="animate-spin" /> : <Play size={16} />}
              <span>Run</span>
            </button>
            
            <button
              onClick={saveIndicator}
              disabled={nodes.length === 0}
              className="flex items-center space-x-2 px-4 py-2 bg-blue-600 hover:bg-blue-700 
                disabled:bg-slate-600 disabled:cursor-not-allowed rounded transition-colors"
            >
              <Save size={16} />
              <span>Save</span>
            </button>
          </div>
        </div>
      </div>

      <div className="flex h-full pt-20">
        {/* Node Library */}
        <AnimatePresence>
          {showNodeLibrary && (
            <motion.div
              className="w-80 bg-slate-800/95 backdrop-blur border-r border-slate-700 overflow-y-auto"
              initial={{ x: -320 }}
              animate={{ x: 0 }}
              exit={{ x: -320 }}
            >
              <div className="p-4">
                <h3 className="font-semibold mb-4">Node Library</h3>
                
                {Object.entries(NODE_TYPES).map(([category, nodeTypes]) => (
                  <div key={category} className="mb-6">
                    <h4 className="text-sm font-medium text-gray-400 uppercase tracking-wide mb-3">
                      {category}
                    </h4>
                    <div className="space-y-2">
                      {nodeTypes.map((nodeType) => (
                        <button
                          key={nodeType.id}
                          onClick={() => addNode(nodeType)}
                          className="w-full p-3 bg-slate-700/50 hover:bg-slate-700 rounded-lg border border-slate-600 
                            transition-colors text-left"
                        >
                          <div className="flex items-center space-x-3">
                            <div 
                              className="w-3 h-3 rounded-full"
                              style={{ backgroundColor: nodeType.color }}
                            />
                            <div className="flex-1">
                              <div className="font-medium text-sm">{nodeType.displayName}</div>
                              <div className="text-xs text-gray-400">{nodeType.description}</div>
                            </div>
                          </div>
                        </button>
                      ))}
                    </div>
                  </div>
                ))}

                {/* Sample Strategies */}
                <div className="mb-6">
                  <h4 className="text-sm font-medium text-gray-400 uppercase tracking-wide mb-3">
                    Sample Strategies
                  </h4>
                  <div className="space-y-2">
                    {SAMPLE_STRATEGIES.map((strategy, index) => (
                      <button
                        key={index}
                        className="w-full p-3 bg-purple-900/30 hover:bg-purple-900/50 rounded-lg border border-purple-600/30 
                          transition-colors text-left"
                      >
                        <div className="font-medium text-sm text-purple-300">{strategy.name}</div>
                        <div className="text-xs text-gray-400 mt-1">{strategy.description}</div>
                      </button>
                    ))}
                  </div>
                </div>
              </div>
            </motion.div>
          )}
        </AnimatePresence>

        {/* Main Canvas */}
        <div className="flex-1 relative">
          <div
            ref={canvasRef}
            className="w-full h-full relative bg-slate-900 overflow-hidden cursor-crosshair"
            onMouseMove={handleMouseMove}
            onMouseUp={handleMouseUp}
            onMouseLeave={handleMouseUp}
          >
            {/* Grid Background */}
            <div className="absolute inset-0 opacity-10">
              <svg width="100%" height="100%">
                <defs>
                  <pattern id="grid" width="20" height="20" patternUnits="userSpaceOnUse">
                    <path d="M 20 0 L 0 0 0 20" fill="none" stroke="#475569" strokeWidth="1"/>
                  </pattern>
                </defs>
                <rect width="100%" height="100%" fill="url(#grid)" />
              </svg>
            </div>

            {/* Connections SVG */}
            <svg
              ref={svgRef}
              className="absolute inset-0 pointer-events-none"
              style={{ transform: `translate(${panOffset.x}px, ${panOffset.y}px) scale(${zoomLevel})` }}
            >
              {connections.map(renderConnection)}
              
              {/* Active connection line */}
              {isConnecting && connectionStart && (
                <line
                  x1={connectionStart.position.x}
                  y1={connectionStart.position.y}
                  x2={mousePosition.x}
                  y2={mousePosition.y}
                  stroke="#3B82F6"
                  strokeWidth="2"
                  strokeDasharray="5,5"
                  className="pointer-events-none"
                />
              )}
            </svg>

            {/* Nodes */}
            <div
              className="absolute inset-0"
              style={{ transform: `translate(${panOffset.x}px, ${panOffset.y}px)` }}
            >
              {nodes.map(renderNode)}
            </div>

            {/* Empty State */}
            {nodes.length === 0 && (
              <div className="absolute inset-0 flex items-center justify-center">
                <div className="text-center text-gray-400">
                  <Cpu size={48} className="mx-auto mb-4 opacity-50" />
                  <h3 className="text-lg font-medium mb-2">Build Your Custom Indicator</h3>
                  <p className="text-sm">Drag nodes from the library to get started</p>
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Code View Panel */}
        <AnimatePresence>
          {showCodeView && (
            <motion.div
              className="w-96 bg-slate-800/95 backdrop-blur border-l border-slate-700 overflow-hidden"
              initial={{ x: 384 }}
              animate={{ x: 0 }}
              exit={{ x: 384 }}
            >
              <div className="p-4 border-b border-slate-700">
                <h3 className="font-semibold flex items-center space-x-2">
                  <Code size={18} />
                  <span>Generated Code</span>
                </h3>
              </div>
              <div className="p-4 overflow-y-auto h-full">
                <pre className="text-sm bg-slate-900 p-4 rounded border border-slate-600 overflow-auto">
                  <code className="text-green-400">{generateCode}</code>
                </pre>
              </div>
            </motion.div>
          )}
        </AnimatePresence>

        {/* Backtest Panel */}
        <AnimatePresence>
          {showBacktest && (
            <motion.div
              className="w-96 bg-slate-800/95 backdrop-blur border-l border-slate-700 overflow-hidden"
              initial={{ x: 384 }}
              animate={{ x: 0 }}
              exit={{ x: 384 }}
            >
              <div className="p-4 border-b border-slate-700">
                <h3 className="font-semibold flex items-center space-x-2">
                  <Activity size={18} />
                  <span>Backtest Results</span>
                </h3>
              </div>
              
              <div className="p-4 overflow-y-auto">
                {backtestResults ? (
                  <div className="space-y-4">
                    <div className="grid grid-cols-2 gap-4">
                      <div className="bg-slate-700/50 rounded p-3">
                        <div className="text-sm text-gray-400">Total Return</div>
                        <div className="text-lg font-bold text-green-400">
                          {(backtestResults.returns.reduce((a, b) => a + b, 0) * 100).toFixed(2)}%
                        </div>
                      </div>
                      <div className="bg-slate-700/50 rounded p-3">
                        <div className="text-sm text-gray-400">Sharpe Ratio</div>
                        <div className="text-lg font-bold">{backtestResults.sharpeRatio.toFixed(2)}</div>
                      </div>
                      <div className="bg-slate-700/50 rounded p-3">
                        <div className="text-sm text-gray-400">Max Drawdown</div>
                        <div className="text-lg font-bold text-red-400">
                          {(backtestResults.maxDrawdown * 100).toFixed(2)}%
                        </div>
                      </div>
                      <div className="bg-slate-700/50 rounded p-3">
                        <div className="text-sm text-gray-400">Win Rate</div>
                        <div className="text-lg font-bold">
                          {(backtestResults.winRate * 100).toFixed(1)}%
                        </div>
                      </div>
                      <div className="bg-slate-700/50 rounded p-3">
                        <div className="text-sm text-gray-400">Total Trades</div>
                        <div className="text-lg font-bold">{backtestResults.totalTrades}</div>
                      </div>
                      <div className="bg-slate-700/50 rounded p-3">
                        <div className="text-sm text-gray-400">Profit Factor</div>
                        <div className="text-lg font-bold">{backtestResults.profitFactor.toFixed(2)}</div>
                      </div>
                    </div>
                  </div>
                ) : (
                  <div className="text-center text-gray-400 py-12">
                    <Activity size={48} className="mx-auto mb-4 opacity-50" />
                    <p>Run a backtest to see results</p>
                  </div>
                )}
              </div>
            </motion.div>
          )}
        </AnimatePresence>
      </div>

      {/* Zoom Controls */}
      <div className="absolute bottom-4 left-4 flex items-center space-x-2 bg-slate-800/90 backdrop-blur rounded-lg p-2 border border-slate-700">
        <button
          onClick={() => setZoomLevel(prev => Math.max(0.5, prev - 0.1))}
          className="p-1 hover:bg-slate-700 rounded"
        >
          <Minus size={16} />
        </button>
        <span className="text-sm px-2">{Math.round(zoomLevel * 100)}%</span>
        <button
          onClick={() => setZoomLevel(prev => Math.min(2, prev + 0.1))}
          className="p-1 hover:bg-slate-700 rounded"
        >
          <Plus size={16} />
        </button>
        <button
          onClick={() => {
            setZoomLevel(1);
            setPanOffset({ x: 0, y: 0 });
          }}
          className="p-1 hover:bg-slate-700 rounded ml-2"
        >
          <Target size={16} />
        </button>
      </div>

      {/* Node Count */}
      {nodes.length > 0 && (
        <div className="absolute bottom-4 right-4 bg-slate-800/90 backdrop-blur rounded-lg px-3 py-2 border border-slate-700">
          <span className="text-sm text-gray-300">
            {nodes.length} node{nodes.length !== 1 ? 's' : ''}, {connections.length} connection{connections.length !== 1 ? 's' : ''}
          </span>
        </div>
      )}
    </div>
  );
};

export default CustomIndicatorBuilder;