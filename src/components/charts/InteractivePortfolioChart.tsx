import React, { useState, useMemo, useRef, useEffect } from 'react';
import { motion } from 'framer-motion';
import { 
  TrendingUp, 
  TrendingDown, 
  BarChart3, 
  LineChart, 
  Activity,
  Maximize2,
  Download,
  Settings
} from 'lucide-react';

interface ChartDataPoint {
  date: string;
  portfolioValue: number;
  portfolioReturn: number;
  benchmark?: number;
  benchmarkReturn?: number;
}

interface InteractivePortfolioChartProps {
  data: ChartDataPoint[];
  timeRange: '1D' | '1W' | '1M' | '3M' | '6M' | '1Y' | 'ALL';
  showBenchmark?: boolean;
  height?: number;
  className?: string;
}

export const InteractivePortfolioChart: React.FC<InteractivePortfolioChartProps> = ({
  data,
  timeRange,
  showBenchmark = true,
  height = 320,
  className = ''
}) => {
  const [chartType, setChartType] = useState<'line' | 'area'>('area');
  const [hoveredPoint, setHoveredPoint] = useState<number | null>(null);
  const [isFullscreen, setIsFullscreen] = useState(false);
  const svgRef = useRef<SVGSVGElement>(null);

  // Chart dimensions
  const margin = { top: 20, right: 80, bottom: 40, left: 80 };
  const width = 800;
  const chartWidth = width - margin.left - margin.right;
  const chartHeight = height - margin.top - margin.bottom;

  // Calculate scales
  const { xScale, yScale, portfolioPath, benchmarkPath } = useMemo(() => {
    if (!data.length) return { xScale: [], yScale: [], portfolioPath: '', benchmarkPath: '' };

    const portfolioValues = data.map(d => d.portfolioValue);
    const benchmarkValues = data.map(d => d.benchmark || 0);
    const allValues = [...portfolioValues, ...benchmarkValues].filter(v => !isNaN(v) && isFinite(v));

    if (allValues.length === 0) return { xScale: [], yScale: [], portfolioPath: '', benchmarkPath: '' };

    let minValue = Math.min(...allValues) * 0.98;
    let maxValue = Math.max(...allValues) * 1.02;
    
    // Prevent division by zero when all values are the same
    if (minValue === maxValue) {
      minValue = maxValue - Math.abs(maxValue * 0.1) || -1;
      maxValue = maxValue + Math.abs(maxValue * 0.1) || 1;
    }

    const valueRange = maxValue - minValue;
    
    const xScale = data.map((_, i) => {
      const ratio = data.length > 1 ? i / (data.length - 1) : 0;
      return ratio * chartWidth;
    });
    
    const yScale = portfolioValues.map(value => {
      if (isNaN(value) || !isFinite(value)) return chartHeight / 2;
      return chartHeight - ((value - minValue) / valueRange) * chartHeight;
    });

    // Create SVG path for portfolio
    const portfolioPath = data.reduce((path, point, index) => {
      const x = xScale[index];
      const value = point.portfolioValue;
      
      if (isNaN(value) || !isFinite(value) || isNaN(x) || !isFinite(x)) {
        return path; // Skip invalid points
      }
      
      const y = chartHeight - ((value - minValue) / valueRange) * chartHeight;
      
      if (isNaN(y) || !isFinite(y)) {
        return path; // Skip invalid calculations
      }
      
      return index === 0 ? `M ${x} ${y}` : `${path} L ${x} ${y}`;
    }, '');

    // Create SVG path for benchmark
    const benchmarkPath = showBenchmark && data[0]?.benchmark ? data.reduce((path, point, index) => {
      const x = xScale[index];
      const value = point.benchmark!;
      
      if (isNaN(value) || !isFinite(value) || isNaN(x) || !isFinite(x)) {
        return path; // Skip invalid points
      }
      
      const y = chartHeight - ((value - minValue) / valueRange) * chartHeight;
      
      if (isNaN(y) || !isFinite(y)) {
        return path; // Skip invalid calculations
      }
      
      return index === 0 ? `M ${x} ${y}` : `${path} L ${x} ${y}`;
    }, '') : '';

    return { xScale, yScale, portfolioPath, benchmarkPath };
  }, [data, chartHeight, chartWidth, showBenchmark]);

  // Create area path for filled chart
  const portfolioAreaPath = useMemo(() => {
    if (!data.length || chartType !== 'area') return '';
    return `${portfolioPath} L ${chartWidth} ${chartHeight} L 0 ${chartHeight} Z`;
  }, [portfolioPath, chartWidth, chartHeight, chartType, data.length]);

  const formatValue = (value: number) => {
    if (value >= 10000000) return `₹${(value / 10000000).toFixed(1)}Cr`;
    if (value >= 100000) return `₹${(value / 100000).toFixed(1)}L`;
    if (value >= 1000) return `₹${(value / 1000).toFixed(1)}K`;
    return `₹${value.toFixed(0)}`;
  };

  const formatPercentage = (value: number) => {
    return `${value >= 0 ? '+' : ''}${value.toFixed(2)}%`;
  };

  const getReturnColor = (value: number) => {
    return value >= 0 ? '#10b981' : '#ef4444';
  };

  if (!data.length) {
    return (
      <div className={`glass-panel rounded-xl flex items-center justify-center ${className}`} style={{ height }}>
        <div className="text-center">
          <BarChart3 className="w-16 h-16 text-slate-400 mx-auto mb-4" />
          <p className="text-slate-400">No chart data available</p>
        </div>
      </div>
    );
  }

  const latestData = data[data.length - 1];
  const firstData = data[0];
  const totalReturn = ((latestData.portfolioValue - firstData.portfolioValue) / firstData.portfolioValue) * 100;

  return (
    <motion.div
      className={`glass-card p-6 rounded-2xl ${className} ${isFullscreen ? 'fixed inset-4 z-50' : ''}`}
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      style={isFullscreen ? { height: 'calc(100vh - 2rem)' } : undefined}
    >
      {/* Chart Header */}
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center space-x-4">
          <div>
            <h3 className="text-lg font-semibold text-white">Portfolio Performance</h3>
            <div className="flex items-center space-x-4 text-sm">
              <div className="flex items-center space-x-2">
                <div className="w-3 h-3 rounded-full bg-purple-400"></div>
                <span className="text-slate-300">Portfolio</span>
                <span className={`font-semibold ${totalReturn >= 0 ? 'text-green-400' : 'text-red-400'}`}>
                  {formatPercentage(totalReturn)}
                </span>
              </div>
              {showBenchmark && (
                <div className="flex items-center space-x-2">
                  <div className="w-3 h-3 rounded-full bg-cyan-400"></div>
                  <span className="text-slate-300">Nifty 50</span>
                </div>
              )}
            </div>
          </div>
        </div>

        <div className="flex items-center space-x-2">
          <div className="flex items-center space-x-1 bg-slate-800/50 rounded-lg p-1">
            <button
              onClick={() => setChartType('line')}
              className={`p-2 rounded-lg transition-colors ${
                chartType === 'line' ? 'bg-purple-500/20 text-purple-400' : 'text-slate-400 hover:text-white'
              }`}
              title="Line chart"
            >
              <LineChart className="w-4 h-4" />
            </button>
            <button
              onClick={() => setChartType('area')}
              className={`p-2 rounded-lg transition-colors ${
                chartType === 'area' ? 'bg-purple-500/20 text-purple-400' : 'text-slate-400 hover:text-white'
              }`}
              title="Area chart"
            >
              <BarChart3 className="w-4 h-4" />
            </button>
          </div>

          <button
            onClick={() => setIsFullscreen(!isFullscreen)}
            className="p-2 rounded-lg bg-slate-800/50 text-slate-400 hover:text-white transition-colors"
            title="Toggle fullscreen"
          >
            <Maximize2 className="w-4 h-4" />
          </button>

          <button
            className="p-2 rounded-lg bg-slate-800/50 text-slate-400 hover:text-white transition-colors"
            title="Download chart"
          >
            <Download className="w-4 h-4" />
          </button>
        </div>
      </div>

      {/* Chart Container */}
      <div className="relative" style={{ height: isFullscreen ? 'calc(100% - 120px)' : height }}>
        <svg
          ref={svgRef}
          width="100%"
          height="100%"
          viewBox={`0 0 ${width} ${height}`}
          className="overflow-visible"
        >
          {/* Grid lines */}
          <defs>
            <pattern id="grid" width="40" height="40" patternUnits="userSpaceOnUse">
              <path d="M 40 0 L 0 0 0 40" fill="none" stroke="rgba(148, 163, 184, 0.1)" strokeWidth="1"/>
            </pattern>
            <linearGradient id="portfolioGradient" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor="rgb(139, 92, 246)" stopOpacity="0.3"/>
              <stop offset="100%" stopColor="rgb(139, 92, 246)" stopOpacity="0"/>
            </linearGradient>
          </defs>

          <rect width="100%" height="100%" fill="url(#grid)" />

          {/* Chart area */}
          <g transform={`translate(${margin.left}, ${margin.top})`}>
            {/* Portfolio area fill */}
            {chartType === 'area' && portfolioAreaPath && (
              <path
                d={portfolioAreaPath}
                fill="url(#portfolioGradient)"
                className="transition-all duration-300"
              />
            )}

            {/* Benchmark line */}
            {showBenchmark && benchmarkPath && (
              <path
                d={benchmarkPath}
                fill="none"
                stroke="#06b6d4"
                strokeWidth="2"
                strokeDasharray="5,5"
                opacity="0.7"
                className="transition-all duration-300"
              />
            )}

            {/* Portfolio line */}
            <path
              d={portfolioPath}
              fill="none"
              stroke="#8b5cf6"
              strokeWidth="3"
              className="transition-all duration-300"
            />

            {/* Data points */}
            {data.map((point, index) => {
              const x = xScale[index];
              const y = yScale[index];
              
              // Skip invalid points
              if (isNaN(x) || isNaN(y) || !isFinite(x) || !isFinite(y)) {
                return null;
              }
              
              return (
                <g key={index}>
                  <circle
                    cx={x}
                    cy={y}
                    r={hoveredPoint === index ? 6 : 3}
                    fill="#8b5cf6"
                    stroke="#1e293b"
                    strokeWidth="2"
                    className="transition-all duration-200 cursor-pointer"
                    onMouseEnter={() => setHoveredPoint(index)}
                    onMouseLeave={() => setHoveredPoint(null)}
                  />
                  
                  {/* Tooltip */}
                  {hoveredPoint === index && (
                    <g>
                      <foreignObject x={Math.max(0, x - 80)} y={Math.max(0, y - 80)} width="160" height="60">
                        <motion.div
                          className="glass-panel p-3 rounded-lg shadow-lg border border-purple-400/20"
                          initial={{ opacity: 0, scale: 0.8 }}
                          animate={{ opacity: 1, scale: 1 }}
                        >
                          <div className="text-xs text-white text-center">
                            <div className="font-semibold mb-1">{point.date}</div>
                            <div className="text-purple-400">{formatValue(point.portfolioValue)}</div>
                            <div className={`${point.portfolioReturn >= 0 ? 'text-green-400' : 'text-red-400'}`}>
                              {formatPercentage(point.portfolioReturn)}
                            </div>
                          </div>
                        </motion.div>
                      </foreignObject>
                    </g>
                  )}
                </g>
              );
            })}

            {/* Y-axis labels */}
            {data.length >= 2 && firstData && latestData && [0, 0.25, 0.5, 0.75, 1].map((ratio, index) => {
              const firstValue = firstData.portfolioValue || 0;
              const latestValue = latestData.portfolioValue || 0;
              const value = firstValue + (latestValue - firstValue) * ratio;
              
              if (isNaN(value) || !isFinite(value)) return null;
              
              return (
                <g key={index}>
                  <text
                    x={-10}
                    y={chartHeight - ratio * chartHeight}
                    textAnchor="end"
                    alignmentBaseline="middle"
                    className="text-xs fill-slate-400"
                  >
                    {formatValue(value)}
                  </text>
                  <line
                    x1={0}
                    y1={chartHeight - ratio * chartHeight}
                    x2={chartWidth}
                    y2={chartHeight - ratio * chartHeight}
                    stroke="rgba(148, 163, 184, 0.1)"
                    strokeWidth="1"
                  />
                </g>
              );
            })}

            {/* X-axis labels */}
            {data.length > 0 && [0, 0.5, 1].map((ratio, index) => {
              const dataIndex = Math.floor(ratio * (data.length - 1));
              const point = data[dataIndex];
              
              if (!point || !point.date) return null;
              
              return (
                <text
                  key={index}
                  x={ratio * chartWidth}
                  y={chartHeight + 20}
                  textAnchor="middle"
                  className="text-xs fill-slate-400"
                >
                  {new Date(point.date).toLocaleDateString('en-US', { 
                    month: 'short', 
                    day: 'numeric' 
                  })}
                </text>
              );
            })}
          </g>
        </svg>

        {/* Performance Stats */}
        <div className="absolute top-4 right-4 glass-panel p-3 rounded-lg">
          <div className="text-xs text-slate-400 mb-1">Current Value</div>
          <div className="text-lg font-bold text-white">{formatValue(latestData.portfolioValue)}</div>
          <div className={`text-sm font-semibold ${totalReturn >= 0 ? 'text-green-400' : 'text-red-400'}`}>
            {formatPercentage(totalReturn)} ({timeRange})
          </div>
        </div>
      </div>
    </motion.div>
  );
};