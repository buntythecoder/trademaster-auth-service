import React, { useState, useEffect, useCallback, useRef, useMemo } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { 
  Grid3x3,
  Maximize2,
  Minimize2,
  RotateCw,
  Link,
  Unlink,
  Settings,
  Eye,
  EyeOff,
  Plus,
  X,
  ArrowUpDown,
  ArrowLeftRight,
  Layout,
  Monitor,
  Tablet,
  Smartphone,
  Clock,
  TrendingUp,
  Activity,
  BarChart3,
  LineChart,
  Target,
  Layers,
  Sync,
  Zap
} from 'lucide-react';

interface TimeframeData {
  timeframe: string;
  interval: number; // in minutes
  displayName: string;
  data: CandlestickData[];
  volume: number[];
  indicators: Record<string, number[]>;
}

interface CandlestickData {
  timestamp: string;
  date: Date;
  open: number;
  high: number;
  low: number;
  close: number;
  volume: number;
}

interface ChartWindow {
  id: string;
  symbol: string;
  timeframe: string;
  position: { x: number; y: number };
  size: { width: number; height: number };
  isMaximized: boolean;
  isMinimized: boolean;
  isVisible: boolean;
  syncGroup: number;
  chartType: 'candlestick' | 'line' | 'area' | 'ohlc';
  indicators: string[];
  overlays: string[];
  settings: {
    showVolume: boolean;
    showGrid: boolean;
    theme: 'dark' | 'light';
    autoScale: boolean;
  };
}

interface SyncGroup {
  id: number;
  name: string;
  color: string;
  charts: string[];
  syncType: 'crosshair' | 'zoom' | 'time' | 'all';
  masterChart?: string;
}

interface LayoutTemplate {
  id: string;
  name: string;
  description: string;
  chartCount: number;
  layout: 'single' | 'dual_horizontal' | 'dual_vertical' | 'quad' | 'triple_left' | 'triple_right' | 'custom';
  windows: ChartWindow[];
  syncGroups: SyncGroup[];
}

const TIMEFRAMES = [
  { value: '1m', label: '1 Minute', interval: 1 },
  { value: '5m', label: '5 Minutes', interval: 5 },
  { value: '15m', label: '15 Minutes', interval: 15 },
  { value: '30m', label: '30 Minutes', interval: 30 },
  { value: '1h', label: '1 Hour', interval: 60 },
  { value: '4h', label: '4 Hours', interval: 240 },
  { value: '1d', label: '1 Day', interval: 1440 },
  { value: '1w', label: '1 Week', interval: 10080 },
  { value: '1M', label: '1 Month', interval: 43200 }
];

const LAYOUT_TEMPLATES: LayoutTemplate[] = [
  {
    id: 'single',
    name: 'Single Chart',
    description: 'One full-size chart',
    chartCount: 1,
    layout: 'single',
    windows: [
      {
        id: 'chart_1',
        symbol: 'AAPL',
        timeframe: '1h',
        position: { x: 0, y: 0 },
        size: { width: 100, height: 100 },
        isMaximized: false,
        isMinimized: false,
        isVisible: true,
        syncGroup: 0,
        chartType: 'candlestick',
        indicators: ['sma_20', 'rsi'],
        overlays: [],
        settings: { showVolume: true, showGrid: true, theme: 'dark', autoScale: true }
      }
    ],
    syncGroups: []
  },
  {
    id: 'dual_horizontal',
    name: 'Dual Horizontal',
    description: 'Two charts side by side',
    chartCount: 2,
    layout: 'dual_horizontal',
    windows: [
      {
        id: 'chart_1',
        symbol: 'AAPL',
        timeframe: '1h',
        position: { x: 0, y: 0 },
        size: { width: 50, height: 100 },
        isMaximized: false,
        isMinimized: false,
        isVisible: true,
        syncGroup: 1,
        chartType: 'candlestick',
        indicators: ['sma_20'],
        overlays: [],
        settings: { showVolume: true, showGrid: true, theme: 'dark', autoScale: true }
      },
      {
        id: 'chart_2',
        symbol: 'AAPL',
        timeframe: '15m',
        position: { x: 50, y: 0 },
        size: { width: 50, height: 100 },
        isMaximized: false,
        isMinimized: false,
        isVisible: true,
        syncGroup: 1,
        chartType: 'candlestick',
        indicators: ['ema_12'],
        overlays: [],
        settings: { showVolume: false, showGrid: true, theme: 'dark', autoScale: true }
      }
    ],
    syncGroups: [
      { id: 1, name: 'Main Sync', color: '#3B82F6', charts: ['chart_1', 'chart_2'], syncType: 'crosshair' }
    ]
  },
  {
    id: 'quad',
    name: 'Quad Layout',
    description: 'Four charts in grid',
    chartCount: 4,
    layout: 'quad',
    windows: [
      {
        id: 'chart_1',
        symbol: 'AAPL',
        timeframe: '1d',
        position: { x: 0, y: 0 },
        size: { width: 50, height: 50 },
        isMaximized: false,
        isMinimized: false,
        isVisible: true,
        syncGroup: 1,
        chartType: 'candlestick',
        indicators: ['sma_20', 'sma_50'],
        overlays: [],
        settings: { showVolume: false, showGrid: true, theme: 'dark', autoScale: true }
      },
      {
        id: 'chart_2',
        symbol: 'AAPL',
        timeframe: '4h',
        position: { x: 50, y: 0 },
        size: { width: 50, height: 50 },
        isMaximized: false,
        isMinimized: false,
        isVisible: true,
        syncGroup: 1,
        chartType: 'candlestick',
        indicators: ['ema_12', 'ema_26'],
        overlays: [],
        settings: { showVolume: false, showGrid: true, theme: 'dark', autoScale: true }
      },
      {
        id: 'chart_3',
        symbol: 'AAPL',
        timeframe: '1h',
        position: { x: 0, y: 50 },
        size: { width: 50, height: 50 },
        isMaximized: false,
        isMinimized: false,
        isVisible: true,
        syncGroup: 1,
        chartType: 'candlestick',
        indicators: ['rsi'],
        overlays: [],
        settings: { showVolume: true, showGrid: true, theme: 'dark', autoScale: true }
      },
      {
        id: 'chart_4',
        symbol: 'AAPL',
        timeframe: '15m',
        position: { x: 50, y: 50 },
        size: { width: 50, height: 50 },
        isMaximized: false,
        isMinimized: false,
        isVisible: true,
        syncGroup: 1,
        chartType: 'line',
        indicators: ['macd'],
        overlays: [],
        settings: { showVolume: false, showGrid: true, theme: 'dark', autoScale: true }
      }
    ],
    syncGroups: [
      { id: 1, name: 'Multi-TF Analysis', color: '#10B981', charts: ['chart_1', 'chart_2', 'chart_3', 'chart_4'], syncType: 'time' }
    ]
  }
];

const SYNC_GROUP_COLORS = ['#3B82F6', '#10B981', '#F59E0B', '#EF4444', '#8B5CF6', '#EC4899'];

const MultiTimeframeAnalysis: React.FC<{
  symbol: string;
  onSymbolChange?: (symbol: string) => void;
  initialLayout?: string;
}> = ({ 
  symbol = 'AAPL', 
  onSymbolChange,
  initialLayout = 'quad' 
}) => {
  const [currentLayout, setCurrentLayout] = useState<LayoutTemplate>(
    LAYOUT_TEMPLATES.find(t => t.id === initialLayout) || LAYOUT_TEMPLATES[0]
  );
  const [chartWindows, setChartWindows] = useState<ChartWindow[]>(currentLayout.windows);
  const [syncGroups, setSyncGroups] = useState<SyncGroup[]>(currentLayout.syncGroups);
  const [activeChartId, setActiveChartId] = useState<string | null>(null);
  const [showLayoutPanel, setShowLayoutPanel] = useState(false);
  const [showSyncPanel, setShowSyncPanel] = useState(false);
  const [crosshairPosition, setCrosshairPosition] = useState<{ x: number; y: number } | null>(null);
  const [zoomLevel, setZoomLevel] = useState<number>(1);
  const [timePosition, setTimePosition] = useState<Date>(new Date());
  const [isAnalysisMode, setIsAnalysisMode] = useState(false);
  const [correlationData, setCorrelationData] = useState<Record<string, number>>({});
  
  const containerRef = useRef<HTMLDivElement>(null);
  const chartsDataRef = useRef<Map<string, TimeframeData>>(new Map());

  // Generate mock data for different timeframes
  const generateMockData = useCallback((timeframe: string, count: number = 100): CandlestickData[] => {
    const data: CandlestickData[] = [];
    const tf = TIMEFRAMES.find(t => t.value === timeframe);
    const intervalMinutes = tf?.interval || 60;
    
    let basePrice = 150;
    const now = new Date();
    
    for (let i = count; i >= 0; i--) {
      const timestamp = new Date(now.getTime() - (i * intervalMinutes * 60 * 1000));
      
      // Add some correlation between timeframes
      const trend = Math.sin((count - i) * 0.1) * 2;
      const volatility = 0.02 + (Math.random() * 0.01);
      
      basePrice += (Math.random() - 0.5) * volatility * basePrice + trend * 0.1;
      
      const open = basePrice + (Math.random() - 0.5) * volatility * basePrice;
      const close = basePrice + (Math.random() - 0.5) * volatility * basePrice;
      const high = Math.max(open, close) + Math.random() * volatility * basePrice;
      const low = Math.min(open, close) - Math.random() * volatility * basePrice;
      const volume = Math.floor(Math.random() * 1000000) + 100000;
      
      data.push({
        timestamp: timestamp.toISOString(),
        date: timestamp,
        open: Number(open.toFixed(2)),
        high: Number(high.toFixed(2)),
        low: Number(low.toFixed(2)),
        close: Number(close.toFixed(2)),
        volume
      });
    }
    
    return data;
  }, []);

  // Initialize chart data
  useEffect(() => {
    chartWindows.forEach(window => {
      const data = generateMockData(window.timeframe);
      chartsDataRef.current.set(window.id, {
        timeframe: window.timeframe,
        interval: TIMEFRAMES.find(t => t.value === window.timeframe)?.interval || 60,
        displayName: TIMEFRAMES.find(t => t.value === window.timeframe)?.label || window.timeframe,
        data,
        volume: data.map(d => d.volume),
        indicators: {}
      });
    });
  }, [chartWindows, generateMockData]);

  // Synchronization logic
  const syncCharts = useCallback((sourceChartId: string, action: 'crosshair' | 'zoom' | 'time', data: any) => {
    const sourceChart = chartWindows.find(c => c.id === sourceChartId);
    if (!sourceChart) return;

    const syncGroup = syncGroups.find(g => g.charts.includes(sourceChartId));
    if (!syncGroup) return;

    const shouldSync = (type: string) => syncGroup.syncType === 'all' || syncGroup.syncType === type;

    if (action === 'crosshair' && shouldSync('crosshair')) {
      setCrosshairPosition(data);
    } else if (action === 'zoom' && shouldSync('zoom')) {
      setZoomLevel(data);
    } else if (action === 'time' && shouldSync('time')) {
      setTimePosition(data);
    }
  }, [chartWindows, syncGroups]);

  // Layout management
  const applyLayout = (layoutId: string) => {
    const layout = LAYOUT_TEMPLATES.find(t => t.id === layoutId);
    if (!layout) return;

    setCurrentLayout(layout);
    setChartWindows(layout.windows.map(w => ({ ...w, symbol })));
    setSyncGroups(layout.syncGroups);
    setActiveChartId(null);
  };

  const addChart = () => {
    const newChart: ChartWindow = {
      id: `chart_${Date.now()}`,
      symbol,
      timeframe: '1h',
      position: { x: Math.random() * 30, y: Math.random() * 30 },
      size: { width: 40, height: 40 },
      isMaximized: false,
      isMinimized: false,
      isVisible: true,
      syncGroup: 0,
      chartType: 'candlestick',
      indicators: [],
      overlays: [],
      settings: { showVolume: true, showGrid: true, theme: 'dark', autoScale: true }
    };
    
    setChartWindows(prev => [...prev, newChart]);
  };

  const removeChart = (chartId: string) => {
    setChartWindows(prev => prev.filter(c => c.id !== chartId));
    setSyncGroups(prev => prev.map(group => ({
      ...group,
      charts: group.charts.filter(id => id !== chartId)
    })));
  };

  const updateChart = (chartId: string, updates: Partial<ChartWindow>) => {
    setChartWindows(prev => prev.map(chart => 
      chart.id === chartId ? { ...chart, ...updates } : chart
    ));
  };

  const createSyncGroup = (chartIds: string[]) => {
    const newGroup: SyncGroup = {
      id: Date.now(),
      name: `Sync Group ${syncGroups.length + 1}`,
      color: SYNC_GROUP_COLORS[syncGroups.length % SYNC_GROUP_COLORS.length],
      charts: chartIds,
      syncType: 'crosshair'
    };
    
    setSyncGroups(prev => [...prev, newGroup]);
    
    // Update charts to use the new sync group
    setChartWindows(prev => prev.map(chart => 
      chartIds.includes(chart.id) 
        ? { ...chart, syncGroup: newGroup.id }
        : chart
    ));
  };

  // Calculate correlations between timeframes
  const calculateCorrelations = useCallback(() => {
    const correlations: Record<string, number> = {};
    
    chartWindows.forEach((chart, index) => {
      chartWindows.forEach((otherChart, otherIndex) => {
        if (index >= otherIndex) return;
        
        const data1 = chartsDataRef.current.get(chart.id)?.data;
        const data2 = chartsDataRef.current.get(otherChart.id)?.data;
        
        if (data1 && data2) {
          // Simple correlation calculation
          const returns1 = data1.slice(1).map((d, i) => (d.close - data1[i].close) / data1[i].close);
          const returns2 = data2.slice(1).map((d, i) => (d.close - data2[i].close) / data2[i].close);
          
          const minLength = Math.min(returns1.length, returns2.length);
          const corr1 = returns1.slice(-minLength);
          const corr2 = returns2.slice(-minLength);
          
          // Pearson correlation coefficient
          const mean1 = corr1.reduce((a, b) => a + b, 0) / corr1.length;
          const mean2 = corr2.reduce((a, b) => a + b, 0) / corr2.length;
          
          let numerator = 0;
          let sum1Sq = 0;
          let sum2Sq = 0;
          
          for (let i = 0; i < corr1.length; i++) {
            const diff1 = corr1[i] - mean1;
            const diff2 = corr2[i] - mean2;
            numerator += diff1 * diff2;
            sum1Sq += diff1 * diff1;
            sum2Sq += diff2 * diff2;
          }
          
          const correlation = numerator / Math.sqrt(sum1Sq * sum2Sq);
          correlations[`${chart.timeframe}_${otherChart.timeframe}`] = correlation;
        }
      });
    });
    
    setCorrelationData(correlations);
  }, [chartWindows]);

  useEffect(() => {
    if (isAnalysisMode) {
      calculateCorrelations();
    }
  }, [isAnalysisMode, calculateCorrelations]);

  const renderChart = (chartWindow: ChartWindow) => {
    const chartData = chartsDataRef.current.get(chartWindow.id);
    if (!chartData) return null;

    const syncGroup = syncGroups.find(g => g.id === chartWindow.syncGroup);
    const currentPrice = chartData.data[chartData.data.length - 1]?.close || 0;
    const priceChange = chartData.data.length > 1 
      ? currentPrice - chartData.data[chartData.data.length - 2].close
      : 0;

    return (
      <motion.div
        key={chartWindow.id}
        className={`absolute bg-slate-800 rounded-lg border-2 overflow-hidden ${
          syncGroup 
            ? `border-[${syncGroup.color}]` 
            : 'border-slate-700'
        } ${
          activeChartId === chartWindow.id ? 'ring-2 ring-blue-500' : ''
        }`}
        style={{
          left: `${chartWindow.position.x}%`,
          top: `${chartWindow.position.y}%`,
          width: `${chartWindow.size.width}%`,
          height: `${chartWindow.size.height}%`,
          borderColor: syncGroup?.color
        }}
        onClick={() => setActiveChartId(chartWindow.id)}
        whileHover={{ scale: 1.01 }}
        layout
      >
        {/* Chart Header */}
        <div className="flex items-center justify-between p-2 bg-slate-700/50 border-b border-slate-600">
          <div className="flex items-center space-x-2">
            <div className="flex items-center space-x-1">
              <BarChart3 size={14} className="text-blue-400" />
              <span className="font-medium text-sm">{chartWindow.symbol}</span>
            </div>
            <div className="px-2 py-1 bg-slate-600 rounded text-xs">
              {chartData.displayName}
            </div>
            {syncGroup && (
              <div 
                className="w-2 h-2 rounded-full"
                style={{ backgroundColor: syncGroup.color }}
                title={`Sync Group: ${syncGroup.name}`}
              />
            )}
          </div>
          
          <div className="flex items-center space-x-1">
            <button
              onClick={(e) => {
                e.stopPropagation();
                updateChart(chartWindow.id, { isMinimized: !chartWindow.isMinimized });
              }}
              className="p-1 hover:bg-slate-600 rounded"
            >
              {chartWindow.isMinimized ? <Maximize2 size={12} /> : <Minimize2 size={12} />}
            </button>
            <button
              onClick={(e) => {
                e.stopPropagation();
                removeChart(chartWindow.id);
              }}
              className="p-1 hover:bg-red-600/20 text-red-400 rounded"
            >
              <X size={12} />
            </button>
          </div>
        </div>

        {!chartWindow.isMinimized && (
          <>
            {/* Price Display */}
            <div className="p-2 border-b border-slate-600/50">
              <div className="flex items-center justify-between">
                <div className="text-lg font-bold">
                  ${currentPrice.toFixed(2)}
                </div>
                <div className={`text-sm ${priceChange >= 0 ? 'text-green-400' : 'text-red-400'}`}>
                  {priceChange >= 0 ? '+' : ''}{priceChange.toFixed(2)} ({((priceChange / (currentPrice - priceChange)) * 100).toFixed(2)}%)
                </div>
              </div>
            </div>

            {/* Chart Area */}
            <div className="relative flex-1 min-h-32">
              {/* Mock Chart Visualization */}
              <div className="absolute inset-2 border border-slate-600/30 rounded bg-slate-900/50">
                {/* Grid */}
                <div className="absolute inset-0 opacity-20">
                  <div className="grid grid-cols-8 grid-rows-6 h-full w-full">
                    {Array.from({ length: 48 }).map((_, i) => (
                      <div key={i} className="border border-slate-600/20"></div>
                    ))}
                  </div>
                </div>

                {/* Candlestick representation */}
                <div className="absolute inset-2 flex items-end space-x-1 overflow-hidden">
                  {chartData.data.slice(-20).map((candle, index) => {
                    const isGreen = candle.close > candle.open;
                    const bodyHeight = Math.abs(candle.close - candle.open) * 2;
                    
                    return (
                      <div key={index} className="flex flex-col items-center justify-end flex-1">
                        <div 
                          className={`w-0.5 ${isGreen ? 'bg-green-400' : 'bg-red-400'}`}
                          style={{ height: `${Math.random() * 20 + 10}px` }}
                        />
                        <div 
                          className={`w-1.5 ${isGreen ? 'bg-green-400' : 'bg-red-400'}`}
                          style={{ height: `${Math.max(bodyHeight, 2)}px` }}
                        />
                        <div 
                          className={`w-0.5 ${isGreen ? 'bg-green-400' : 'bg-red-400'}`}
                          style={{ height: `${Math.random() * 15 + 5}px` }}
                        />
                      </div>
                    );
                  })}
                </div>

                {/* Crosshair */}
                {crosshairPosition && syncGroup && syncGroups.find(g => g.charts.includes(chartWindow.id)) && (
                  <div className="absolute inset-0 pointer-events-none">
                    <div 
                      className="absolute w-full h-px bg-blue-400/50"
                      style={{ top: `${crosshairPosition.y}%` }}
                    />
                    <div 
                      className="absolute h-full w-px bg-blue-400/50"
                      style={{ left: `${crosshairPosition.x}%` }}
                    />
                  </div>
                )}
              </div>

              {/* Volume Chart */}
              {chartWindow.settings.showVolume && (
                <div className="absolute bottom-2 left-2 right-2 h-8 border border-slate-600/30 rounded bg-slate-900/30">
                  <div className="flex items-end h-full p-1 space-x-px">
                    {chartData.volume.slice(-20).map((vol, index) => (
                      <div 
                        key={index}
                        className="flex-1 bg-blue-400/60"
                        style={{ 
                          height: `${(vol / Math.max(...chartData.volume)) * 100}%` 
                        }}
                      />
                    ))}
                  </div>
                </div>
              )}
            </div>

            {/* Timeframe Selector */}
            <div className="p-2 border-t border-slate-600/50">
              <select
                value={chartWindow.timeframe}
                onChange={(e) => updateChart(chartWindow.id, { timeframe: e.target.value })}
                className="w-full bg-slate-700 border border-slate-600 rounded px-2 py-1 text-xs"
              >
                {TIMEFRAMES.map(tf => (
                  <option key={tf.value} value={tf.value}>
                    {tf.label}
                  </option>
                ))}
              </select>
            </div>
          </>
        )}
      </motion.div>
    );
  };

  return (
    <div 
      ref={containerRef}
      className="relative w-full h-full bg-gradient-to-br from-slate-900 to-slate-800 overflow-hidden"
    >
      {/* Header Controls */}
      <div className="absolute top-4 left-4 right-4 z-30 flex items-center justify-between">
        <div className="flex items-center space-x-3">
          <div className="flex items-center space-x-2 bg-slate-800/90 backdrop-blur rounded-lg p-2 border border-slate-700">
            <Activity className="text-green-400" size={20} />
            <span className="font-bold">Multi-Timeframe Analysis</span>
            <span className="text-sm text-gray-400">({symbol})</span>
          </div>
          
          <div className="flex items-center space-x-1 bg-slate-800/90 backdrop-blur rounded-lg p-1 border border-slate-700">
            <button
              onClick={() => setShowLayoutPanel(!showLayoutPanel)}
              className={`p-2 rounded transition-colors ${
                showLayoutPanel ? 'bg-blue-600' : 'hover:bg-slate-700'
              }`}
              title="Layout Templates"
            >
              <Layout size={16} />
            </button>
            <button
              onClick={() => setShowSyncPanel(!showSyncPanel)}
              className={`p-2 rounded transition-colors ${
                showSyncPanel ? 'bg-blue-600' : 'hover:bg-slate-700'
              }`}
              title="Sync Groups"
            >
              <Link size={16} />
            </button>
            <button
              onClick={addChart}
              className="p-2 hover:bg-slate-700 rounded transition-colors"
              title="Add Chart"
            >
              <Plus size={16} />
            </button>
            <button
              onClick={() => setIsAnalysisMode(!isAnalysisMode)}
              className={`p-2 rounded transition-colors ${
                isAnalysisMode ? 'bg-purple-600' : 'hover:bg-slate-700'
              }`}
              title="Analysis Mode"
            >
              <Target size={16} />
            </button>
          </div>
        </div>

        <div className="flex items-center space-x-2">
          {/* Sync Status Indicators */}
          {syncGroups.map(group => (
            <div key={group.id} className="flex items-center space-x-1 bg-slate-800/90 backdrop-blur rounded-lg p-2 border border-slate-700">
              <div 
                className="w-3 h-3 rounded-full"
                style={{ backgroundColor: group.color }}
              />
              <span className="text-xs text-gray-300">{group.name}</span>
              <span className="text-xs text-gray-500">({group.charts.length})</span>
              <Sync size={12} className="text-gray-400" />
            </div>
          ))}
        </div>
      </div>

      {/* Chart Windows Container */}
      <div className="absolute inset-0 pt-20">
        {chartWindows.map(renderChart)}
      </div>

      {/* Layout Panel */}
      <AnimatePresence>
        {showLayoutPanel && (
          <motion.div
            className="absolute top-20 left-4 w-80 bg-slate-800/95 backdrop-blur rounded-lg border border-slate-700 p-4 z-40"
            initial={{ opacity: 0, y: -20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -20 }}
          >
            <div className="flex items-center justify-between mb-4">
              <h3 className="font-semibold">Layout Templates</h3>
              <button
                onClick={() => setShowLayoutPanel(false)}
                className="p-1 hover:bg-slate-700 rounded"
              >
                <X size={16} />
              </button>
            </div>
            
            <div className="space-y-3">
              {LAYOUT_TEMPLATES.map(layout => (
                <button
                  key={layout.id}
                  onClick={() => applyLayout(layout.id)}
                  className={`w-full p-3 rounded-lg border text-left transition-colors ${
                    currentLayout.id === layout.id
                      ? 'border-blue-500 bg-blue-900/30'
                      : 'border-slate-600 hover:border-slate-500 hover:bg-slate-700/30'
                  }`}
                >
                  <div className="font-medium">{layout.name}</div>
                  <div className="text-sm text-gray-400">{layout.description}</div>
                  <div className="text-xs text-gray-500 mt-1">
                    {layout.chartCount} chart{layout.chartCount !== 1 ? 's' : ''}
                  </div>
                </button>
              ))}
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Sync Groups Panel */}
      <AnimatePresence>
        {showSyncPanel && (
          <motion.div
            className="absolute top-20 right-4 w-72 bg-slate-800/95 backdrop-blur rounded-lg border border-slate-700 p-4 z-40"
            initial={{ opacity: 0, y: -20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -20 }}
          >
            <div className="flex items-center justify-between mb-4">
              <h3 className="font-semibold">Synchronization</h3>
              <button
                onClick={() => setShowSyncPanel(false)}
                className="p-1 hover:bg-slate-700 rounded"
              >
                <X size={16} />
              </button>
            </div>
            
            <div className="space-y-3">
              {syncGroups.map(group => (
                <div key={group.id} className="p-3 bg-slate-700/50 rounded-lg border border-slate-600">
                  <div className="flex items-center justify-between mb-2">
                    <div className="flex items-center space-x-2">
                      <div 
                        className="w-3 h-3 rounded-full"
                        style={{ backgroundColor: group.color }}
                      />
                      <span className="font-medium text-sm">{group.name}</span>
                    </div>
                    <span className="text-xs text-gray-400">{group.charts.length} charts</span>
                  </div>
                  
                  <select
                    value={group.syncType}
                    onChange={(e) => setSyncGroups(prev => prev.map(g => 
                      g.id === group.id ? { ...g, syncType: e.target.value as any } : g
                    ))}
                    className="w-full bg-slate-800 border border-slate-600 rounded px-2 py-1 text-xs"
                  >
                    <option value="crosshair">Crosshair Only</option>
                    <option value="zoom">Zoom Level</option>
                    <option value="time">Time Position</option>
                    <option value="all">All Synchronization</option>
                  </select>
                </div>
              ))}
            </div>
            
            <button
              onClick={() => {
                if (chartWindows.length > 1) {
                  createSyncGroup(chartWindows.map(c => c.id));
                }
              }}
              disabled={chartWindows.length < 2}
              className="w-full mt-3 p-2 bg-blue-600 hover:bg-blue-700 disabled:bg-slate-600 disabled:cursor-not-allowed rounded transition-colors text-sm"
            >
              Sync All Charts
            </button>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Analysis Panel */}
      <AnimatePresence>
        {isAnalysisMode && (
          <motion.div
            className="absolute bottom-4 left-4 right-4 bg-slate-800/95 backdrop-blur rounded-lg border border-slate-700 p-4 z-30"
            initial={{ opacity: 0, y: 50 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: 50 }}
          >
            <div className="flex items-center justify-between mb-4">
              <h3 className="font-semibold flex items-center space-x-2">
                <Target className="text-purple-400" size={18} />
                <span>Multi-Timeframe Correlation Analysis</span>
              </h3>
              <button
                onClick={() => setIsAnalysisMode(false)}
                className="p-1 hover:bg-slate-700 rounded"
              >
                <X size={16} />
              </button>
            </div>
            
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
              {Object.entries(correlationData).map(([pair, correlation]) => (
                <div key={pair} className="bg-slate-700/50 rounded p-3 text-center">
                  <div className="text-sm font-medium text-gray-300 mb-1">
                    {pair.replace('_', ' vs ')}
                  </div>
                  <div className={`text-lg font-bold ${
                    correlation > 0.7 ? 'text-green-400' :
                    correlation > 0.3 ? 'text-yellow-400' :
                    correlation > -0.3 ? 'text-gray-400' :
                    correlation > -0.7 ? 'text-orange-400' : 'text-red-400'
                  }`}>
                    {(correlation * 100).toFixed(1)}%
                  </div>
                  <div className="text-xs text-gray-500">
                    {correlation > 0.7 ? 'Strong +' :
                     correlation > 0.3 ? 'Moderate +' :
                     correlation > -0.3 ? 'Weak' :
                     correlation > -0.7 ? 'Moderate -' : 'Strong -'}
                  </div>
                </div>
              ))}
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
};

export default MultiTimeframeAnalysis;