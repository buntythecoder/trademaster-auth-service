import React, { useState, useEffect } from 'react';
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Area,
  AreaChart,
} from 'recharts';
import { formatCurrency, formatTime } from '../../utils/formatting';

interface ChartDataPoint {
  timestamp: string;
  price: number;
  volume?: number;
  change?: number;
}

interface PriceChartProps {
  data: ChartDataPoint[];
  symbol: string;
  currentPrice?: number;
  height?: number;
  showVolume?: boolean;
  timeframe?: string;
  isLoading?: boolean;
}

const CustomTooltip: React.FC<any> = ({ active, payload, label }) => {
  if (active && payload && payload.length) {
    const data = payload[0].payload;
    return (
      <div className="bg-white p-3 border border-gray-200 rounded-lg shadow-lg">
        <p className="text-sm text-gray-600 mb-1">
          {formatTime(label, 'full')}
        </p>
        <div className="space-y-1">
          <div className="flex justify-between items-center gap-4">
            <span className="text-sm text-gray-600">Price:</span>
            <span className="font-semibold text-gray-900">
              {formatCurrency(data.price)}
            </span>
          </div>
          {data.volume && (
            <div className="flex justify-between items-center gap-4">
              <span className="text-sm text-gray-600">Volume:</span>
              <span className="font-semibold text-gray-900">
                {data.volume.toLocaleString()}
              </span>
            </div>
          )}
          {data.change !== undefined && (
            <div className="flex justify-between items-center gap-4">
              <span className="text-sm text-gray-600">Change:</span>
              <span className={`font-semibold ${data.change >= 0 ? 'text-bull' : 'text-bear'}`}>
                {data.change >= 0 ? '+' : ''}{data.change.toFixed(2)}%
              </span>
            </div>
          )}
        </div>
      </div>
    );
  }
  return null;
};

const LoadingSkeleton: React.FC<{ height: number }> = ({ height }) => (
  <div 
    className="animate-pulse bg-gray-200 rounded-lg flex items-center justify-center"
    style={{ height }}
  >
    <div className="text-gray-400 text-center">
      <div className="w-8 h-8 mx-auto mb-2">
        <svg className="animate-spin" fill="none" viewBox="0 0 24 24">
          <circle 
            className="opacity-25" 
            cx="12" 
            cy="12" 
            r="10" 
            stroke="currentColor" 
            strokeWidth="4"
          />
          <path 
            className="opacity-75" 
            fill="currentColor" 
            d="m4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
          />
        </svg>
      </div>
      <p className="text-sm">Loading chart data...</p>
    </div>
  </div>
);

const PriceChart: React.FC<PriceChartProps> = ({
  data,
  symbol,
  currentPrice,
  height = 300,
  showVolume = false,
  timeframe = '1D',
  isLoading = false,
}) => {
  const [chartType, setChartType] = useState<'line' | 'area'>('area');
  const [isPositive, setIsPositive] = useState(true);

  useEffect(() => {
    if (data.length > 1) {
      const firstPrice = data[0].price;
      const lastPrice = data[data.length - 1].price;
      setIsPositive(lastPrice >= firstPrice);
    }
  }, [data]);

  if (isLoading) {
    return <LoadingSkeleton height={height} />;
  }

  if (!data || data.length === 0) {
    return (
      <div 
        className="flex items-center justify-center bg-gray-50 rounded-lg border-2 border-dashed border-gray-200"
        style={{ height }}
      >
        <div className="text-center text-gray-500">
          <div className="w-12 h-12 mx-auto mb-3 text-gray-400">
            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path 
                strokeLinecap="round" 
                strokeLinejoin="round" 
                strokeWidth={1.5} 
                d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" 
              />
            </svg>
          </div>
          <p className="text-sm">No chart data available</p>
          <p className="text-xs text-gray-400 mt-1">
            Data for {symbol} will appear here when available
          </p>
        </div>
      </div>
    );
  }

  const strokeColor = isPositive ? '#22c55e' : '#ef4444';
  const fillColor = isPositive ? 'url(#colorPositive)' : 'url(#colorNegative)';

  return (
    <div className="bg-white rounded-lg border border-gray-200">
      {/* Header */}
      <div className="px-4 py-3 border-b border-gray-200 flex items-center justify-between">
        <div className="flex items-center space-x-3">
          <h3 className="text-lg font-semibold text-gray-900">
            {symbol} Chart
          </h3>
          <span className="text-sm text-gray-500">
            {timeframe}
          </span>
          {currentPrice && (
            <span className="text-sm font-medium text-gray-900">
              {formatCurrency(currentPrice)}
            </span>
          )}
        </div>

        {/* Chart type toggle */}
        <div className="flex items-center space-x-2">
          <button
            onClick={() => setChartType('line')}
            className={`px-3 py-1 rounded text-xs font-medium transition-colors ${
              chartType === 'line'
                ? 'bg-primary text-white'
                : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
            }`}
          >
            Line
          </button>
          <button
            onClick={() => setChartType('area')}
            className={`px-3 py-1 rounded text-xs font-medium transition-colors ${
              chartType === 'area'
                ? 'bg-primary text-white'
                : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
            }`}
          >
            Area
          </button>
        </div>
      </div>

      {/* Chart */}
      <div className="p-4">
        <ResponsiveContainer width="100%" height={height}>
          {chartType === 'area' ? (
            <AreaChart data={data} margin={{ top: 5, right: 30, left: 20, bottom: 5 }}>
              <defs>
                <linearGradient id="colorPositive" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#22c55e" stopOpacity={0.3} />
                  <stop offset="95%" stopColor="#22c55e" stopOpacity={0.05} />
                </linearGradient>
                <linearGradient id="colorNegative" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#ef4444" stopOpacity={0.3} />
                  <stop offset="95%" stopColor="#ef4444" stopOpacity={0.05} />
                </linearGradient>
              </defs>
              <CartesianGrid strokeDasharray="3 3" stroke="#f3f4f6" />
              <XAxis 
                dataKey="timestamp"
                axisLine={false}
                tickLine={false}
                tick={{ fontSize: 12, fill: '#6b7280' }}
                tickFormatter={(value) => formatTime(value, 'short')}
              />
              <YAxis 
                axisLine={false}
                tickLine={false}
                tick={{ fontSize: 12, fill: '#6b7280' }}
                tickFormatter={(value) => formatCurrency(value, { compact: true })}
              />
              <Tooltip content={<CustomTooltip />} />
              <Area
                type="monotone"
                dataKey="price"
                stroke={strokeColor}
                strokeWidth={2}
                fill={fillColor}
                dot={false}
                activeDot={{ r: 6, fill: strokeColor }}
              />
            </AreaChart>
          ) : (
            <LineChart data={data} margin={{ top: 5, right: 30, left: 20, bottom: 5 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="#f3f4f6" />
              <XAxis 
                dataKey="timestamp"
                axisLine={false}
                tickLine={false}
                tick={{ fontSize: 12, fill: '#6b7280' }}
                tickFormatter={(value) => formatTime(value, 'short')}
              />
              <YAxis 
                axisLine={false}
                tickLine={false}
                tick={{ fontSize: 12, fill: '#6b7280' }}
                tickFormatter={(value) => formatCurrency(value, { compact: true })}
              />
              <Tooltip content={<CustomTooltip />} />
              <Line
                type="monotone"
                dataKey="price"
                stroke={strokeColor}
                strokeWidth={2}
                dot={false}
                activeDot={{ r: 6, fill: strokeColor }}
              />
            </LineChart>
          )}
        </ResponsiveContainer>
      </div>
    </div>
  );
};

export default PriceChart;