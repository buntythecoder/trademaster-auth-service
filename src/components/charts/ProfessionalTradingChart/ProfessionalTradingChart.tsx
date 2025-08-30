import React, { useState, useEffect, useRef, useCallback, useMemo } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { 
  TrendingUp, 
  TrendingDown, 
  BarChart3, 
  LineChart, 
  Activity,
  Settings,
  Maximize2,
  Minimize2,
  ZoomIn,
  ZoomOut,
  Move,
  Crosshair,
  Download,
  Share2,
  Eye,
  EyeOff,
  Plus,
  Minus,
  RotateCcw,
  Grid3x3,
  Layers,
  Target,
  Wand2,
  Calculator,
  BookOpen,
  AlertTriangle,
  X,
  ChevronDown,
  Search,
  Star,
  Filter,
  Palette,
  Volume2
} from 'lucide-react';

interface CandlestickData {
  timestamp: string;
  date: Date;
  open: number;
  high: number;
  low: number;
  close: number;
  volume: number;
}

interface TechnicalIndicator {
  id: string;
  name: string;
  shortName: string;
  category: 'trend' | 'momentum' | 'volatility' | 'volume' | 'support_resistance' | 'custom';
  enabled: boolean;
  color: string;
  settings: Record<string, number | string | boolean>;
  values?: number[];
  overlayChart: boolean;
  description: string;
}

interface ChartSettings {
  chartType: 'candlestick' | 'ohlc' | 'line' | 'area' | 'heikin-ashi' | 'renko' | 'kagi' | 'point-figure';
  timeframe: '1m' | '5m' | '15m' | '30m' | '1h' | '4h' | '1d' | '1w' | '1M';
  showVolume: boolean;
  showGrid: boolean;
  crosshair: boolean;
  logScale: boolean;
  theme: 'dark' | 'light';
  candleColors: {
    upColor: string;
    downColor: string;
    upWickColor: string;
    downWickColor: string;
  };
  backgroundColor: string;
  gridColor: string;
  textColor: string;
}

interface DrawingTool {
  id: string;
  type: 'trendline' | 'horizontal' | 'vertical' | 'rectangle' | 'circle' | 'fibonacci' | 'gann' | 'pitchfork' | 'text';
  name: string;
  icon: React.ReactNode;
  active: boolean;
}

interface ChartAlert {
  id: string;
  type: 'price' | 'indicator' | 'pattern';
  condition: string;
  value: number;
  triggered: boolean;
  message: string;
  timestamp: Date;
}

const TECHNICAL_INDICATORS: TechnicalIndicator[] = [
  // Trend Indicators
  {
    id: 'sma_20',
    name: 'Simple Moving Average (20)',
    shortName: 'SMA(20)',
    category: 'trend',
    enabled: false,
    color: '#3B82F6',
    settings: { period: 20 },
    overlayChart: true,
    description: '20-period simple moving average for trend identification'
  },
  {
    id: 'ema_12',
    name: 'Exponential Moving Average (12)',
    shortName: 'EMA(12)',
    category: 'trend',
    enabled: false,
    color: '#10B981',
    settings: { period: 12 },
    overlayChart: true,
    description: 'Fast EMA for trend and signal detection'
  },
  {
    id: 'ema_26',
    name: 'Exponential Moving Average (26)',
    shortName: 'EMA(26)',
    category: 'trend',
    enabled: false,
    color: '#F59E0B',
    settings: { period: 26 },
    overlayChart: true,
    description: 'Slow EMA for trend confirmation'
  },
  {
    id: 'bollinger_bands',
    name: 'Bollinger Bands',
    shortName: 'BB(20,2)',
    category: 'volatility',
    enabled: false,
    color: '#8B5CF6',
    settings: { period: 20, deviation: 2 },
    overlayChart: true,
    description: 'Volatility bands for overbought/oversold conditions'
  },
  {
    id: 'vwap',
    name: 'Volume Weighted Average Price',
    shortName: 'VWAP',
    category: 'volume',
    enabled: false,
    color: '#EC4899',
    settings: {},
    overlayChart: true,
    description: 'Volume-weighted average price for institutional levels'
  },
  {
    id: 'parabolic_sar',
    name: 'Parabolic SAR',
    shortName: 'PSAR',
    category: 'trend',
    enabled: false,
    color: '#F97316',
    settings: { acceleration: 0.02, maximum: 0.2 },
    overlayChart: true,
    description: 'Stop and reverse system for trend following'
  },
  
  // Momentum Indicators
  {
    id: 'rsi',
    name: 'Relative Strength Index',
    shortName: 'RSI(14)',
    category: 'momentum',
    enabled: false,
    color: '#3B82F6',
    settings: { period: 14, overbought: 70, oversold: 30 },
    overlayChart: false,
    description: 'Momentum oscillator measuring speed of price changes'
  },
  {
    id: 'macd',
    name: 'MACD',
    shortName: 'MACD(12,26,9)',
    category: 'momentum',
    enabled: false,
    color: '#10B981',
    settings: { fast: 12, slow: 26, signal: 9 },
    overlayChart: false,
    description: 'Moving average convergence divergence for trend changes'
  },
  {
    id: 'stochastic',
    name: 'Stochastic Oscillator',
    shortName: 'STOCH(14,3)',
    category: 'momentum',
    enabled: false,
    color: '#F59E0B',
    settings: { k_period: 14, d_period: 3, overbought: 80, oversold: 20 },
    overlayChart: false,
    description: 'Momentum indicator comparing closing price to price range'
  },
  {
    id: 'williams_r',
    name: 'Williams %R',
    shortName: '%R(14)',
    category: 'momentum',
    enabled: false,
    color: '#8B5CF6',
    settings: { period: 14 },
    overlayChart: false,
    description: 'Momentum indicator measuring overbought/oversold levels'
  },
  {
    id: 'cci',
    name: 'Commodity Channel Index',
    shortName: 'CCI(20)',
    category: 'momentum',
    enabled: false,
    color: '#EC4899',
    settings: { period: 20 },
    overlayChart: false,
    description: 'Cyclical indicator for identifying trend changes'
  },
  
  // Volume Indicators
  {
    id: 'on_balance_volume',
    name: 'On Balance Volume',
    shortName: 'OBV',
    category: 'volume',
    enabled: false,
    color: '#10B981',
    settings: {},
    overlayChart: false,
    description: 'Volume-price trend indicator'
  },
  {
    id: 'volume_oscillator',
    name: 'Volume Oscillator',
    shortName: 'VO(5,10)',
    category: 'volume',
    enabled: false,
    color: '#F59E0B',
    settings: { short_period: 5, long_period: 10 },
    overlayChart: false,
    description: 'Oscillator showing volume momentum'
  },
  {
    id: 'accumulation_distribution',
    name: 'Accumulation/Distribution Line',
    shortName: 'A/D',
    category: 'volume',
    enabled: false,
    color: '#8B5CF6',
    settings: {},
    overlayChart: false,
    description: 'Volume indicator measuring accumulation and distribution'
  },
  
  // Volatility Indicators
  {
    id: 'atr',
    name: 'Average True Range',
    shortName: 'ATR(14)',
    category: 'volatility',
    enabled: false,
    color: '#F97316',
    settings: { period: 14 },
    overlayChart: false,
    description: 'Volatility indicator measuring price range'
  },
  {
    id: 'keltner_channels',
    name: 'Keltner Channels',
    shortName: 'KC(20,2)',
    category: 'volatility',
    enabled: false,
    color: '#EC4899',
    settings: { period: 20, multiplier: 2 },
    overlayChart: true,
    description: 'Volatility-based trading bands using ATR'
  },
  {
    id: 'donchian_channels',
    name: 'Donchian Channels',
    shortName: 'DC(20)',
    category: 'volatility',
    enabled: false,
    color: '#6366F1',
    settings: { period: 20 },
    overlayChart: true,
    description: 'Trend-following indicator using highest/lowest prices'
  },
  
  // Support/Resistance
  {
    id: 'pivot_points',
    name: 'Pivot Points',
    shortName: 'PP',
    category: 'support_resistance',
    enabled: false,
    color: '#64748B',
    settings: { type: 'standard' },
    overlayChart: true,
    description: 'Support and resistance levels based on previous period'
  },
  {
    id: 'fibonacci_retracement',
    name: 'Fibonacci Retracement',
    shortName: 'FIB',
    category: 'support_resistance',
    enabled: false,
    color: '#F59E0B',
    settings: {},
    overlayChart: true,
    description: 'Key retracement levels based on Fibonacci ratios'
  }
];

const DRAWING_TOOLS: DrawingTool[] = [
  { id: 'trendline', type: 'trendline', name: 'Trend Line', icon: <TrendingUp size={16} />, active: false },
  { id: 'horizontal', type: 'horizontal', name: 'Horizontal Line', icon: <Minus size={16} />, active: false },
  { id: 'vertical', type: 'vertical', name: 'Vertical Line', icon: <div className="rotate-90"><Minus size={16} /></div>, active: false },
  { id: 'rectangle', type: 'rectangle', name: 'Rectangle', icon: <Grid3x3 size={16} />, active: false },
  { id: 'fibonacci', type: 'fibonacci', name: 'Fibonacci', icon: <Target size={16} />, active: false },
  { id: 'text', type: 'text', name: 'Text', icon: <BookOpen size={16} />, active: false }
];

const ProfessionalTradingChart: React.FC<{
  symbol: string;
  data?: CandlestickData[];
  height?: number;
  onTimeframeChange?: (timeframe: string) => void;
  onSymbolChange?: (symbol: string) => void;
}> = ({ 
  symbol = 'AAPL', 
  data: externalData,
  height = 600,
  onTimeframeChange,
  onSymbolChange 
}) => {
  const [settings, setSettings] = useState<ChartSettings>({
    chartType: 'candlestick',
    timeframe: '1h',
    showVolume: true,
    showGrid: true,
    crosshair: true,
    logScale: false,
    theme: 'dark',
    candleColors: {
      upColor: '#10B981',
      downColor: '#EF4444',
      upWickColor: '#10B981',
      downWickColor: '#EF4444'
    },
    backgroundColor: '#0F172A',
    gridColor: '#334155',
    textColor: '#E2E8F0'
  });
  
  const [indicators, setIndicators] = useState<TechnicalIndicator[]>(TECHNICAL_INDICATORS);
  const [drawingTools, setDrawingTools] = useState<DrawingTool[]>(DRAWING_TOOLS);
  const [activeDrawingTool, setActiveDrawingTool] = useState<string | null>(null);
  const [showIndicatorPanel, setShowIndicatorPanel] = useState(false);
  const [showDrawingPanel, setShowDrawingPanel] = useState(false);
  const [showSettingsPanel, setShowSettingsPanel] = useState(false);
  const [isFullscreen, setIsFullscreen] = useState(false);
  const [chartAlerts, setChartAlerts] = useState<ChartAlert[]>([]);
  const [searchIndicator, setSearchIndicator] = useState('');
  const [indicatorCategory, setIndicatorCategory] = useState<string>('all');
  
  const chartRef = useRef<HTMLDivElement>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  
  // Generate mock data for demonstration
  const chartData = useMemo(() => {
    if (externalData && externalData.length > 0) return externalData;
    
    const mockData: CandlestickData[] = [];
    const basePrice = 150;
    const now = new Date();
    
    for (let i = 200; i >= 0; i--) {
      const date = new Date(now.getTime() - i * 60 * 60 * 1000);
      const price = basePrice + (Math.random() - 0.5) * 10 + Math.sin(i * 0.1) * 5;
      const volatility = 0.02;
      
      const open = price + (Math.random() - 0.5) * volatility * price;
      const close = price + (Math.random() - 0.5) * volatility * price;
      const high = Math.max(open, close) + Math.random() * volatility * price;
      const low = Math.min(open, close) - Math.random() * volatility * price;
      const volume = Math.floor(Math.random() * 1000000) + 100000;
      
      mockData.push({
        timestamp: date.toISOString(),
        date,
        open: Number(open.toFixed(2)),
        high: Number(high.toFixed(2)),
        low: Number(low.toFixed(2)),
        close: Number(close.toFixed(2)),
        volume
      });
    }
    
    return mockData;
  }, [externalData, symbol]);

  // Calculate technical indicators
  const calculateIndicators = useCallback(() => {
    const updatedIndicators = indicators.map(indicator => {
      if (!indicator.enabled) return indicator;
      
      let values: number[] = [];
      
      switch (indicator.id) {
        case 'sma_20':
          values = calculateSMA(chartData, 20);
          break;
        case 'ema_12':
          values = calculateEMA(chartData, 12);
          break;
        case 'ema_26':
          values = calculateEMA(chartData, 26);
          break;
        case 'rsi':
          values = calculateRSI(chartData, 14);
          break;
        case 'macd':
          values = calculateMACD(chartData);
          break;
        case 'bollinger_bands':
          values = calculateBollingerBands(chartData, 20, 2);
          break;
        case 'atr':
          values = calculateATR(chartData, 14);
          break;
        case 'vwap':
          values = calculateVWAP(chartData);
          break;
        default:
          values = chartData.map((_, i) => Math.random() * 100); // Mock calculation
      }
      
      return { ...indicator, values };
    });
    
    setIndicators(updatedIndicators);
  }, [chartData, indicators]);

  useEffect(() => {
    calculateIndicators();
  }, [calculateIndicators]);

  // Technical indicator calculation functions
  const calculateSMA = (data: CandlestickData[], period: number): number[] => {
    const values: number[] = [];
    for (let i = 0; i < data.length; i++) {
      if (i < period - 1) {
        values.push(NaN);
        continue;
      }
      const sum = data.slice(i - period + 1, i + 1).reduce((acc, candle) => acc + candle.close, 0);
      values.push(sum / period);
    }
    return values;
  };

  const calculateEMA = (data: CandlestickData[], period: number): number[] => {
    const values: number[] = [];
    const k = 2 / (period + 1);
    let ema = data[0]?.close || 0;
    
    for (let i = 0; i < data.length; i++) {
      if (i === 0) {
        values.push(ema);
        continue;
      }
      ema = data[i].close * k + ema * (1 - k);
      values.push(ema);
    }
    return values;
  };

  const calculateRSI = (data: CandlestickData[], period: number): number[] => {
    const values: number[] = [];
    let gains = 0;
    let losses = 0;
    
    for (let i = 0; i < data.length; i++) {
      if (i === 0) {
        values.push(50);
        continue;
      }
      
      const change = data[i].close - data[i - 1].close;
      const gain = change > 0 ? change : 0;
      const loss = change < 0 ? -change : 0;
      
      if (i < period) {
        gains += gain;
        losses += loss;
        if (i === period - 1) {
          gains /= period;
          losses /= period;
        }
        values.push(50);
      } else {
        gains = (gains * (period - 1) + gain) / period;
        losses = (losses * (period - 1) + loss) / period;
        const rs = losses === 0 ? 100 : gains / losses;
        const rsi = 100 - (100 / (1 + rs));
        values.push(rsi);
      }
    }
    return values;
  };

  const calculateMACD = (data: CandlestickData[]): number[] => {
    const ema12 = calculateEMA(data, 12);
    const ema26 = calculateEMA(data, 26);
    const macdLine = ema12.map((value, i) => value - ema26[i]);
    return macdLine;
  };

  const calculateBollingerBands = (data: CandlestickData[], period: number, deviation: number): number[] => {
    const sma = calculateSMA(data, period);
    const values: number[] = [];
    
    for (let i = 0; i < data.length; i++) {
      if (i < period - 1) {
        values.push(NaN, NaN, NaN); // upper, middle, lower
        continue;
      }
      
      const slice = data.slice(i - period + 1, i + 1);
      const mean = sma[i];
      const variance = slice.reduce((acc, candle) => acc + Math.pow(candle.close - mean, 2), 0) / period;
      const stdDev = Math.sqrt(variance);
      
      values.push(
        mean + (stdDev * deviation), // upper band
        mean, // middle band (SMA)
        mean - (stdDev * deviation)  // lower band
      );
    }
    return values;
  };

  const calculateATR = (data: CandlestickData[], period: number): number[] => {
    const trueRanges: number[] = [];
    for (let i = 1; i < data.length; i++) {
      const tr = Math.max(
        data[i].high - data[i].low,
        Math.abs(data[i].high - data[i - 1].close),
        Math.abs(data[i].low - data[i - 1].close)
      );
      trueRanges.push(tr);
    }
    
    const atrValues: number[] = [NaN];
    let atr = trueRanges.slice(0, period - 1).reduce((sum, tr) => sum + tr, 0) / (period - 1);
    
    for (let i = period; i < data.length; i++) {
      atr = (atr * (period - 1) + trueRanges[i - 1]) / period;
      atrValues.push(atr);
    }
    
    return atrValues;
  };

  const calculateVWAP = (data: CandlestickData[]): number[] => {
    const values: number[] = [];
    let cumulativeTPV = 0;
    let cumulativeVolume = 0;
    
    for (let i = 0; i < data.length; i++) {
      const typicalPrice = (data[i].high + data[i].low + data[i].close) / 3;
      cumulativeTPV += typicalPrice * data[i].volume;
      cumulativeVolume += data[i].volume;
      values.push(cumulativeTPV / cumulativeVolume);
    }
    
    return values;
  };

  const toggleIndicator = (indicatorId: string) => {
    setIndicators(prev => prev.map(indicator => 
      indicator.id === indicatorId 
        ? { ...indicator, enabled: !indicator.enabled }
        : indicator
    ));
  };

  const filteredIndicators = indicators.filter(indicator => {
    const matchesSearch = indicator.name.toLowerCase().includes(searchIndicator.toLowerCase()) ||
                         indicator.shortName.toLowerCase().includes(searchIndicator.toLowerCase());
    const matchesCategory = indicatorCategory === 'all' || indicator.category === indicatorCategory;
    return matchesSearch && matchesCategory;
  });

  const enabledOverlayIndicators = indicators.filter(ind => ind.enabled && ind.overlayChart);
  const enabledSubchartIndicators = indicators.filter(ind => ind.enabled && !ind.overlayChart);

  return (
    <div className={`w-full h-full bg-slate-900 text-white relative ${isFullscreen ? 'fixed inset-0 z-50' : ''}`}>
      {/* Chart Header */}
      <div className="flex items-center justify-between p-4 bg-slate-800/50 border-b border-slate-700">
        <div className="flex items-center space-x-4">
          <div className="flex items-center space-x-2">
            <BarChart3 className="text-blue-400" size={20} />
            <span className="font-bold text-lg">{symbol}</span>
            <div className="px-2 py-1 bg-green-600 text-xs rounded">
              {settings.timeframe}
            </div>
          </div>
          
          {/* Timeframe Selector */}
          <div className="flex items-center space-x-1 bg-slate-700 rounded p-1">
            {['1m', '5m', '15m', '30m', '1h', '4h', '1d', '1w'].map((tf) => (
              <button
                key={tf}
                onClick={() => {
                  setSettings(prev => ({ ...prev, timeframe: tf as any }));
                  onTimeframeChange?.(tf);
                }}
                className={`px-2 py-1 text-xs rounded transition-colors ${
                  settings.timeframe === tf 
                    ? 'bg-blue-600 text-white' 
                    : 'text-gray-300 hover:text-white hover:bg-slate-600'
                }`}
              >
                {tf}
              </button>
            ))}
          </div>
        </div>

        {/* Chart Controls */}
        <div className="flex items-center space-x-2">
          <button
            onClick={() => setShowIndicatorPanel(!showIndicatorPanel)}
            className={`p-2 rounded transition-colors ${
              showIndicatorPanel ? 'bg-blue-600' : 'bg-slate-700 hover:bg-slate-600'
            }`}
            title="Technical Indicators"
          >
            <Activity size={16} />
          </button>
          
          <button
            onClick={() => setShowDrawingPanel(!showDrawingPanel)}
            className={`p-2 rounded transition-colors ${
              showDrawingPanel ? 'bg-blue-600' : 'bg-slate-700 hover:bg-slate-600'
            }`}
            title="Drawing Tools"
          >
            <Wand2 size={16} />
          </button>
          
          <button
            onClick={() => setShowSettingsPanel(!showSettingsPanel)}
            className="p-2 bg-slate-700 hover:bg-slate-600 rounded transition-colors"
            title="Chart Settings"
          >
            <Settings size={16} />
          </button>
          
          <button
            onClick={() => setIsFullscreen(!isFullscreen)}
            className="p-2 bg-slate-700 hover:bg-slate-600 rounded transition-colors"
            title={isFullscreen ? 'Exit Fullscreen' : 'Fullscreen'}
          >
            {isFullscreen ? <Minimize2 size={16} /> : <Maximize2 size={16} />}
          </button>
        </div>
      </div>

      <div className="flex h-full">
        {/* Main Chart Area */}
        <div className="flex-1 relative">
          <div 
            ref={chartRef}
            className="w-full bg-slate-900 relative"
            style={{ height: `${height}px` }}
          >
            {/* Chart Canvas */}
            <canvas
              ref={canvasRef}
              className="absolute inset-0 w-full h-full"
              onMouseMove={() => {/* Handle crosshair */}}
              onClick={() => {/* Handle drawing tools */}}
            />
            
            {/* Mock Chart Content */}
            <div className="absolute inset-4 flex flex-col">
              {/* Price Display */}
              <div className="flex items-center justify-between mb-4">
                <div className="flex items-center space-x-4">
                  <div className="text-2xl font-bold">
                    ${chartData[chartData.length - 1]?.close.toFixed(2)}
                  </div>
                  <div className="flex items-center space-x-2">
                    <TrendingUp className="text-green-400" size={16} />
                    <span className="text-green-400">+2.45 (+1.63%)</span>
                  </div>
                </div>
                
                <div className="text-sm text-gray-400">
                  Vol: 2.3M | Avg Vol: 1.8M
                </div>
              </div>

              {/* Chart Visualization Area */}
              <div className="flex-1 border border-slate-700 rounded bg-slate-800/30 relative overflow-hidden">
                {/* Grid */}
                {settings.showGrid && (
                  <div className="absolute inset-0 opacity-20">
                    <div className="grid grid-cols-12 grid-rows-8 h-full w-full">
                      {Array.from({ length: 96 }).map((_, i) => (
                        <div key={i} className="border border-slate-600/30"></div>
                      ))}
                    </div>
                  </div>
                )}
                
                {/* Chart Content */}
                <div className="absolute inset-4">
                  {/* Mock candlestick chart representation */}
                  <div className="h-full flex items-end space-x-1">
                    {chartData.slice(-50).map((candle, index) => {
                      const isGreen = candle.close > candle.open;
                      const bodyHeight = Math.abs(candle.close - candle.open) / 2;
                      const wickHeight = (candle.high - candle.low) / 2;
                      
                      return (
                        <div key={index} className="flex flex-col items-center justify-end flex-1">
                          {/* Upper wick */}
                          <div 
                            className={`w-0.5 ${isGreen ? 'bg-green-400' : 'bg-red-400'}`}
                            style={{ height: `${wickHeight}px` }}
                          />
                          {/* Body */}
                          <div 
                            className={`w-2 ${isGreen ? 'bg-green-400' : 'bg-red-400'}`}
                            style={{ height: `${Math.max(bodyHeight, 2)}px` }}
                          />
                          {/* Lower wick */}
                          <div 
                            className={`w-0.5 ${isGreen ? 'bg-green-400' : 'bg-red-400'}`}
                            style={{ height: `${wickHeight}px` }}
                          />
                        </div>
                      );
                    })}
                  </div>
                  
                  {/* Overlay Indicators */}
                  {enabledOverlayIndicators.map((indicator) => (
                    <div key={indicator.id} className="absolute inset-0">
                      <div 
                        className="h-0.5 w-full absolute top-1/2"
                        style={{ backgroundColor: indicator.color, opacity: 0.7 }}
                      />
                    </div>
                  ))}
                </div>
                
                {/* Price Scale */}
                <div className="absolute right-0 top-0 bottom-0 w-16 bg-slate-800/50 border-l border-slate-700">
                  <div className="h-full flex flex-col justify-between text-xs text-gray-400 p-2">
                    {Array.from({ length: 6 }).map((_, i) => (
                      <div key={i}>${(150 + i * 2).toFixed(2)}</div>
                    ))}
                  </div>
                </div>
                
                {/* Time Scale */}
                <div className="absolute bottom-0 left-0 right-16 h-8 bg-slate-800/50 border-t border-slate-700">
                  <div className="h-full flex justify-between items-center text-xs text-gray-400 px-4">
                    {Array.from({ length: 6 }).map((_, i) => (
                      <div key={i}>{new Date(Date.now() - (5-i) * 3600000).toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' })}</div>
                    ))}
                  </div>
                </div>
              </div>

              {/* Volume Chart */}
              {settings.showVolume && (
                <div className="h-24 mt-2 border border-slate-700 rounded bg-slate-800/30 relative">
                  <div className="absolute inset-2 flex items-end space-x-1">
                    {chartData.slice(-50).map((candle, index) => (
                      <div 
                        key={index}
                        className="flex-1 bg-blue-400/60"
                        style={{ 
                          height: `${(candle.volume / Math.max(...chartData.map(c => c.volume))) * 60}px` 
                        }}
                      />
                    ))}
                  </div>
                  <div className="absolute top-2 left-2 text-xs text-gray-400">Volume</div>
                </div>
              )}

              {/* Subchart Indicators */}
              {enabledSubchartIndicators.map((indicator, index) => (
                <div key={indicator.id} className="h-32 mt-2 border border-slate-700 rounded bg-slate-800/30 relative">
                  <div className="absolute inset-2">
                    <div 
                      className="h-0.5 w-full absolute top-1/2"
                      style={{ backgroundColor: indicator.color, opacity: 0.8 }}
                    />
                    {/* Mock oscillator */}
                    <div className="absolute top-2 left-2 text-xs text-gray-400">
                      {indicator.shortName}
                    </div>
                    <div className="absolute top-2 right-2 text-xs" style={{ color: indicator.color }}>
                      {indicator.id === 'rsi' ? '67.3' : '0.45'}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Technical Indicators Panel */}
        <AnimatePresence>
          {showIndicatorPanel && (
            <motion.div
              className="w-80 bg-slate-800/95 backdrop-blur border-l border-slate-700 overflow-hidden"
              initial={{ x: 320 }}
              animate={{ x: 0 }}
              exit={{ x: 320 }}
            >
              <div className="p-4 border-b border-slate-700">
                <div className="flex items-center justify-between mb-4">
                  <h3 className="font-semibold">Technical Indicators</h3>
                  <button
                    onClick={() => setShowIndicatorPanel(false)}
                    className="p-1 hover:bg-slate-700 rounded"
                  >
                    <X size={16} />
                  </button>
                </div>
                
                {/* Search */}
                <div className="relative mb-3">
                  <Search className="absolute left-3 top-2.5 text-gray-400" size={16} />
                  <input
                    type="text"
                    placeholder="Search indicators..."
                    value={searchIndicator}
                    onChange={(e) => setSearchIndicator(e.target.value)}
                    className="w-full pl-10 pr-4 py-2 bg-slate-700 border border-slate-600 rounded text-sm"
                  />
                </div>
                
                {/* Category Filter */}
                <select
                  value={indicatorCategory}
                  onChange={(e) => setIndicatorCategory(e.target.value)}
                  className="w-full p-2 bg-slate-700 border border-slate-600 rounded text-sm mb-4"
                >
                  <option value="all">All Categories</option>
                  <option value="trend">Trend</option>
                  <option value="momentum">Momentum</option>
                  <option value="volatility">Volatility</option>
                  <option value="volume">Volume</option>
                  <option value="support_resistance">Support/Resistance</option>
                </select>
              </div>
              
              <div className="overflow-y-auto max-h-96">
                {filteredIndicators.map((indicator) => (
                  <div key={indicator.id} className="p-3 border-b border-slate-700/50 hover:bg-slate-700/30">
                    <div className="flex items-center justify-between">
                      <div className="flex-1">
                        <div className="flex items-center space-x-2">
                          <button
                            onClick={() => toggleIndicator(indicator.id)}
                            className={`w-4 h-4 rounded border-2 flex items-center justify-center ${
                              indicator.enabled 
                                ? 'bg-blue-600 border-blue-600' 
                                : 'border-slate-500'
                            }`}
                          >
                            {indicator.enabled && <div className="w-2 h-2 bg-white rounded-sm" />}
                          </button>
                          <div>
                            <div className="font-medium text-sm">{indicator.shortName}</div>
                            <div className="text-xs text-gray-400">{indicator.category}</div>
                          </div>
                        </div>
                        <div className="text-xs text-gray-500 mt-1 leading-tight">
                          {indicator.description}
                        </div>
                      </div>
                      <div 
                        className="w-3 h-3 rounded-full ml-2"
                        style={{ backgroundColor: indicator.color }}
                      />
                    </div>
                  </div>
                ))}
              </div>
            </motion.div>
          )}
        </AnimatePresence>

        {/* Drawing Tools Panel */}
        <AnimatePresence>
          {showDrawingPanel && (
            <motion.div
              className="w-64 bg-slate-800/95 backdrop-blur border-l border-slate-700"
              initial={{ x: 256 }}
              animate={{ x: 0 }}
              exit={{ x: 256 }}
            >
              <div className="p-4 border-b border-slate-700">
                <div className="flex items-center justify-between mb-4">
                  <h3 className="font-semibold">Drawing Tools</h3>
                  <button
                    onClick={() => setShowDrawingPanel(false)}
                    className="p-1 hover:bg-slate-700 rounded"
                  >
                    <X size={16} />
                  </button>
                </div>
              </div>
              
              <div className="p-4 space-y-2">
                {drawingTools.map((tool) => (
                  <button
                    key={tool.id}
                    onClick={() => setActiveDrawingTool(activeDrawingTool === tool.id ? null : tool.id)}
                    className={`w-full flex items-center space-x-3 p-3 rounded transition-colors ${
                      activeDrawingTool === tool.id
                        ? 'bg-blue-600 text-white'
                        : 'bg-slate-700/50 hover:bg-slate-700 text-gray-300'
                    }`}
                  >
                    {tool.icon}
                    <span className="text-sm">{tool.name}</span>
                  </button>
                ))}
                
                <div className="pt-4 border-t border-slate-700">
                  <button className="w-full p-2 bg-red-600/20 text-red-400 rounded text-sm hover:bg-red-600/30">
                    Clear All Drawings
                  </button>
                </div>
              </div>
            </motion.div>
          )}
        </AnimatePresence>

        {/* Settings Panel */}
        <AnimatePresence>
          {showSettingsPanel && (
            <motion.div
              className="w-72 bg-slate-800/95 backdrop-blur border-l border-slate-700"
              initial={{ x: 288 }}
              animate={{ x: 0 }}
              exit={{ x: 288 }}
            >
              <div className="p-4 border-b border-slate-700">
                <div className="flex items-center justify-between mb-4">
                  <h3 className="font-semibold">Chart Settings</h3>
                  <button
                    onClick={() => setShowSettingsPanel(false)}
                    className="p-1 hover:bg-slate-700 rounded"
                  >
                    <X size={16} />
                  </button>
                </div>
              </div>
              
              <div className="p-4 space-y-4">
                {/* Chart Type */}
                <div>
                  <label className="block text-sm font-medium mb-2">Chart Type</label>
                  <select
                    value={settings.chartType}
                    onChange={(e) => setSettings(prev => ({ ...prev, chartType: e.target.value as any }))}
                    className="w-full p-2 bg-slate-700 border border-slate-600 rounded text-sm"
                  >
                    <option value="candlestick">Candlestick</option>
                    <option value="ohlc">OHLC Bars</option>
                    <option value="line">Line</option>
                    <option value="area">Area</option>
                    <option value="heikin-ashi">Heikin-Ashi</option>
                    <option value="renko">Renko</option>
                  </select>
                </div>

                {/* Chart Options */}
                <div className="space-y-3">
                  <div className="flex items-center justify-between">
                    <span className="text-sm">Show Volume</span>
                    <button
                      onClick={() => setSettings(prev => ({ ...prev, showVolume: !prev.showVolume }))}
                      className={`w-10 h-5 rounded-full transition-colors ${
                        settings.showVolume ? 'bg-blue-600' : 'bg-slate-600'
                      }`}
                    >
                      <div className={`w-4 h-4 rounded-full bg-white transition-transform ${
                        settings.showVolume ? 'translate-x-5' : 'translate-x-0.5'
                      }`} />
                    </button>
                  </div>
                  
                  <div className="flex items-center justify-between">
                    <span className="text-sm">Show Grid</span>
                    <button
                      onClick={() => setSettings(prev => ({ ...prev, showGrid: !prev.showGrid }))}
                      className={`w-10 h-5 rounded-full transition-colors ${
                        settings.showGrid ? 'bg-blue-600' : 'bg-slate-600'
                      }`}
                    >
                      <div className={`w-4 h-4 rounded-full bg-white transition-transform ${
                        settings.showGrid ? 'translate-x-5' : 'translate-x-0.5'
                      }`} />
                    </button>
                  </div>
                  
                  <div className="flex items-center justify-between">
                    <span className="text-sm">Crosshair</span>
                    <button
                      onClick={() => setSettings(prev => ({ ...prev, crosshair: !prev.crosshair }))}
                      className={`w-10 h-5 rounded-full transition-colors ${
                        settings.crosshair ? 'bg-blue-600' : 'bg-slate-600'
                      }`}
                    >
                      <div className={`w-4 h-4 rounded-full bg-white transition-transform ${
                        settings.crosshair ? 'translate-x-5' : 'translate-x-0.5'
                      }`} />
                    </button>
                  </div>
                  
                  <div className="flex items-center justify-between">
                    <span className="text-sm">Log Scale</span>
                    <button
                      onClick={() => setSettings(prev => ({ ...prev, logScale: !prev.logScale }))}
                      className={`w-10 h-5 rounded-full transition-colors ${
                        settings.logScale ? 'bg-blue-600' : 'bg-slate-600'
                      }`}
                    >
                      <div className={`w-4 h-4 rounded-full bg-white transition-transform ${
                        settings.logScale ? 'translate-x-5' : 'translate-x-0.5'
                      }`} />
                    </button>
                  </div>
                </div>

                {/* Colors */}
                <div>
                  <label className="block text-sm font-medium mb-2">Candle Colors</label>
                  <div className="grid grid-cols-2 gap-2">
                    <div>
                      <label className="block text-xs text-gray-400 mb-1">Up Color</label>
                      <input
                        type="color"
                        value={settings.candleColors.upColor}
                        onChange={(e) => setSettings(prev => ({
                          ...prev,
                          candleColors: { ...prev.candleColors, upColor: e.target.value }
                        }))}
                        className="w-full h-8 rounded cursor-pointer"
                      />
                    </div>
                    <div>
                      <label className="block text-xs text-gray-400 mb-1">Down Color</label>
                      <input
                        type="color"
                        value={settings.candleColors.downColor}
                        onChange={(e) => setSettings(prev => ({
                          ...prev,
                          candleColors: { ...prev.candleColors, downColor: e.target.value }
                        }))}
                        className="w-full h-8 rounded cursor-pointer"
                      />
                    </div>
                  </div>
                </div>
              </div>
            </motion.div>
          )}
        </AnimatePresence>
      </div>

      {/* Active Drawing Tool Indicator */}
      {activeDrawingTool && (
        <div className="absolute top-16 left-4 bg-blue-600 text-white px-3 py-1 rounded text-sm">
          Drawing: {drawingTools.find(t => t.id === activeDrawingTool)?.name}
        </div>
      )}

      {/* Chart Alerts */}
      {chartAlerts.length > 0 && (
        <div className="absolute top-16 right-4 space-y-2">
          {chartAlerts.slice(0, 3).map((alert) => (
            <motion.div
              key={alert.id}
              className="bg-yellow-600 text-black px-4 py-2 rounded shadow-lg max-w-xs"
              initial={{ opacity: 0, x: 100 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: 100 }}
            >
              <div className="flex items-center space-x-2">
                <AlertTriangle size={16} />
                <div className="text-sm font-medium">Price Alert</div>
              </div>
              <div className="text-xs mt-1">{alert.message}</div>
            </motion.div>
          ))}
        </div>
      )}
    </div>
  );
};

export default ProfessionalTradingChart;