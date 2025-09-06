import React, { useState, useEffect, useMemo } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  BarChart3, TrendingUp, TrendingDown, Target, Clock,
  DollarSign, Activity, Zap, AlertTriangle, CheckCircle,
  PieChart, LineChart, BarChart, Calendar, Filter,
  Download, RefreshCw, Settings, Eye, MoreVertical,
  ArrowUpRight, ArrowDownRight, Minus, Award,
  Shield, Gauge, Users, MapPin, Globe
} from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';

interface ExecutionRecord {
  id: string;
  orderId: string;
  symbol: string;
  side: 'BUY' | 'SELL';
  requestedQuantity: number;
  filledQuantity: number;
  averagePrice: number;
  benchmarkPrice: number;
  slippage: number; // in bps
  marketImpact: number; // in bps
  timing: number; // in bps
  implementationShortfall: number; // in bps
  venue: string;
  algorithm: string;
  executionTime: number; // in seconds
  commission: number;
  timestamp: Date;
  marketCondition: 'NORMAL' | 'VOLATILE' | 'QUIET' | 'STRESSED';
  orderType: string;
  participationRate: number;
}

interface VenuePerformance {
  venue: string;
  totalVolume: number;
  orderCount: number;
  averageSlippage: number;
  fillRate: number;
  averageExecutionTime: number;
  marketShare: number;
  costSavings: number;
  reliability: number;
}

interface AlgorithmPerformance {
  algorithm: string;
  totalOrders: number;
  averageSlippage: number;
  implementationShortfall: number;
  fillRate: number;
  averageExecutionTime: number;
  successRate: number;
  riskAdjustedReturn: number;
  marketImpact: number;
}

interface TimeAnalysis {
  hour: number;
  volume: number;
  averageSlippage: number;
  orderCount: number;
  volatility: number;
  spread: number;
}

interface ExecutionAnalyticsProps {
  userId: string;
  dateRange?: { start: Date; end: Date };
  onMetricsUpdate?: (metrics: any) => void;
}

export const OrderExecutionAnalytics: React.FC<ExecutionAnalyticsProps> = ({
  userId,
  dateRange,
  onMetricsUpdate
}) => {
  const [executions, setExecutions] = useState<ExecutionRecord[]>([]);
  const [venuePerformance, setVenuePerformance] = useState<VenuePerformance[]>([]);
  const [algorithmPerformance, setAlgorithmPerformance] = useState<AlgorithmPerformance[]>([]);
  const [timeAnalysis, setTimeAnalysis] = useState<TimeAnalysis[]>([]);
  const [activeTab, setActiveTab] = useState<'overview' | 'slippage' | 'venues' | 'algorithms' | 'timing' | 'costs'>('overview');
  const [selectedMetric, setSelectedMetric] = useState<'slippage' | 'volume' | 'timing' | 'cost'>('slippage');
  const [filterPeriod, setFilterPeriod] = useState<'1D' | '1W' | '1M' | '3M' | '1Y'>('1M');
  const [selectedSymbol, setSelectedSymbol] = useState<string>('ALL');

  useEffect(() => {
    initializeMockData();
  }, []);

  const initializeMockData = () => {
    // Generate mock execution records
    const mockExecutions: ExecutionRecord[] = Array.from({ length: 50 }, (_, i) => {
      const symbols = ['RELIANCE', 'TCS', 'INFY', 'HDFC', 'ICICI', 'SBI', 'LT', 'WIPRO', 'ITC', 'ONGC'];
      const venues = ['NSE', 'BSE', 'CITADEL_DP', 'GOLDMAN_MM', 'INSTINET_ECN'];
      const algorithms = ['SMART', 'VWAP', 'TWAP', 'IMPLEMENTATION_SHORTFALL', 'ARRIVAL_PRICE'];
      const marketConditions = ['NORMAL', 'VOLATILE', 'QUIET', 'STRESSED'] as const;
      
      const symbol = symbols[Math.floor(Math.random() * symbols.length)];
      const side: 'BUY' | 'SELL' = Math.random() > 0.5 ? 'BUY' : 'SELL';
      const requestedQuantity = Math.floor(Math.random() * 5000) + 100;
      const filledQuantity = Math.floor(requestedQuantity * (0.95 + Math.random() * 0.05));
      const basePrice = Math.random() * 3000 + 500;
      const slippage = (Math.random() - 0.5) * 20; // -10 to +10 bps
      const marketImpact = Math.random() * 15;
      const timing = (Math.random() - 0.5) * 10;
      
      return {
        id: `EXEC-${i + 1}`,
        orderId: `ORD-${Math.floor(Math.random() * 10000)}`,
        symbol,
        side,
        requestedQuantity,
        filledQuantity,
        averagePrice: basePrice + (slippage / 10000) * basePrice,
        benchmarkPrice: basePrice,
        slippage,
        marketImpact,
        timing,
        implementationShortfall: slippage + marketImpact + timing,
        venue: venues[Math.floor(Math.random() * venues.length)],
        algorithm: algorithms[Math.floor(Math.random() * algorithms.length)],
        executionTime: Math.random() * 300 + 10,
        commission: Math.random() * 50 + 5,
        timestamp: new Date(Date.now() - Math.random() * 30 * 24 * 60 * 60 * 1000),
        marketCondition: marketConditions[Math.floor(Math.random() * marketConditions.length)],
        orderType: 'LIMIT',
        participationRate: Math.random() * 0.3 + 0.05
      };
    });

    // Generate venue performance data
    const venues = ['NSE', 'BSE', 'CITADEL_DP', 'GOLDMAN_MM', 'INSTINET_ECN'];
    const mockVenuePerformance: VenuePerformance[] = venues.map(venue => {
      const venueExecutions = mockExecutions.filter(ex => ex.venue === venue);
      return {
        venue,
        totalVolume: venueExecutions.reduce((sum, ex) => sum + ex.filledQuantity * ex.averagePrice, 0),
        orderCount: venueExecutions.length,
        averageSlippage: venueExecutions.reduce((sum, ex) => sum + ex.slippage, 0) / venueExecutions.length || 0,
        fillRate: venueExecutions.reduce((sum, ex) => sum + (ex.filledQuantity / ex.requestedQuantity), 0) / venueExecutions.length || 0,
        averageExecutionTime: venueExecutions.reduce((sum, ex) => sum + ex.executionTime, 0) / venueExecutions.length || 0,
        marketShare: (venueExecutions.length / mockExecutions.length) * 100,
        costSavings: Math.random() * 0.5 + 0.1,
        reliability: Math.random() * 0.15 + 0.85
      };
    });

    // Generate algorithm performance data
    const algorithms = ['SMART', 'VWAP', 'TWAP', 'IMPLEMENTATION_SHORTFALL', 'ARRIVAL_PRICE'];
    const mockAlgorithmPerformance: AlgorithmPerformance[] = algorithms.map(algorithm => {
      const algoExecutions = mockExecutions.filter(ex => ex.algorithm === algorithm);
      return {
        algorithm,
        totalOrders: algoExecutions.length,
        averageSlippage: algoExecutions.reduce((sum, ex) => sum + ex.slippage, 0) / algoExecutions.length || 0,
        implementationShortfall: algoExecutions.reduce((sum, ex) => sum + ex.implementationShortfall, 0) / algoExecutions.length || 0,
        fillRate: algoExecutions.reduce((sum, ex) => sum + (ex.filledQuantity / ex.requestedQuantity), 0) / algoExecutions.length || 0,
        averageExecutionTime: algoExecutions.reduce((sum, ex) => sum + ex.executionTime, 0) / algoExecutions.length || 0,
        successRate: Math.random() * 0.2 + 0.8,
        riskAdjustedReturn: Math.random() * 0.3 + 0.1,
        marketImpact: algoExecutions.reduce((sum, ex) => sum + ex.marketImpact, 0) / algoExecutions.length || 0
      };
    });

    // Generate time analysis data
    const mockTimeAnalysis: TimeAnalysis[] = Array.from({ length: 24 }, (_, hour) => ({
      hour,
      volume: Math.random() * 100000000 + 10000000,
      averageSlippage: (Math.random() - 0.5) * 10,
      orderCount: Math.floor(Math.random() * 50) + 5,
      volatility: Math.random() * 0.5 + 0.1,
      spread: Math.random() * 20 + 2
    }));

    setExecutions(mockExecutions);
    setVenuePerformance(mockVenuePerformance);
    setAlgorithmPerformance(mockAlgorithmPerformance);
    setTimeAnalysis(mockTimeAnalysis);
  };

  const overallMetrics = useMemo(() => {
    if (executions.length === 0) return null;

    const totalVolume = executions.reduce((sum, ex) => sum + ex.filledQuantity * ex.averagePrice, 0);
    const totalCommission = executions.reduce((sum, ex) => sum + ex.commission, 0);
    const averageSlippage = executions.reduce((sum, ex) => sum + ex.slippage, 0) / executions.length;
    const averageImplementationShortfall = executions.reduce((sum, ex) => sum + ex.implementationShortfall, 0) / executions.length;
    const averageFillRate = executions.reduce((sum, ex) => sum + (ex.filledQuantity / ex.requestedQuantity), 0) / executions.length;
    const averageExecutionTime = executions.reduce((sum, ex) => sum + ex.executionTime, 0) / executions.length;

    // Calculate best execution savings
    const benchmarkCost = executions.reduce((sum, ex) => {
      const worstSlippage = 5; // bps
      return sum + (ex.filledQuantity * ex.benchmarkPrice * worstSlippage / 10000);
    }, 0);
    const actualCost = executions.reduce((sum, ex) => {
      return sum + Math.abs(ex.slippage * ex.filledQuantity * ex.averagePrice / 10000);
    }, 0);
    const bestExecutionSavings = benchmarkCost - actualCost;

    return {
      totalOrders: executions.length,
      totalVolume,
      totalCommission,
      averageSlippage,
      averageImplementationShortfall,
      averageFillRate,
      averageExecutionTime,
      bestExecutionSavings,
      costSavingsRate: bestExecutionSavings / benchmarkCost,
      marketImpactScore: executions.reduce((sum, ex) => sum + ex.marketImpact, 0) / executions.length
    };
  }, [executions]);

  const getSlippageColor = (slippage: number) => {
    if (slippage < -2) return 'text-green-600 bg-green-100';
    if (slippage < 2) return 'text-yellow-600 bg-yellow-100';
    return 'text-red-600 bg-red-100';
  };

  const getPerformanceGrade = (score: number) => {
    if (score >= 0.9) return { grade: 'A+', color: 'bg-green-600 text-white' };
    if (score >= 0.8) return { grade: 'A', color: 'bg-green-500 text-white' };
    if (score >= 0.7) return { grade: 'B', color: 'bg-blue-500 text-white' };
    if (score >= 0.6) return { grade: 'C', color: 'bg-yellow-500 text-white' };
    return { grade: 'D', color: 'bg-red-500 text-white' };
  };

  return (
    <div className="p-6 space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Order Execution Analytics</h1>
          <p className="text-gray-600">Comprehensive execution quality analysis and benchmarking</p>
        </div>
        <div className="flex items-center space-x-3">
          <div className="flex items-center space-x-2 border border-gray-300 rounded-lg p-1">
            {['1D', '1W', '1M', '3M', '1Y'].map((period) => (
              <Button
                key={period}
                size="sm"
                variant={filterPeriod === period ? "default" : "ghost"}
                onClick={() => setFilterPeriod(period as any)}
              >
                {period}
              </Button>
            ))}
          </div>
          <Button variant="outline" className="flex items-center space-x-2">
            <Download className="h-4 w-4" />
            <span>Export</span>
          </Button>
          <Button variant="outline" className="flex items-center space-x-2">
            <RefreshCw className="h-4 w-4" />
            <span>Refresh</span>
          </Button>
        </div>
      </div>

      {/* Key Performance Indicators */}
      {overallMetrics && (
        <div className="grid grid-cols-1 md:grid-cols-6 gap-4">
          <Card>
            <CardContent className="p-6">
              <div className="flex items-center">
                <Activity className="h-8 w-8 text-blue-600" />
                <div className="ml-4">
                  <p className="text-sm font-medium text-gray-600">Total Orders</p>
                  <p className="text-2xl font-bold text-gray-900">
                    {overallMetrics.totalOrders.toLocaleString()}
                  </p>
                </div>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardContent className="p-6">
              <div className="flex items-center">
                <Target className="h-8 w-8 text-green-600" />
                <div className="ml-4">
                  <p className="text-sm font-medium text-gray-600">Avg Slippage</p>
                  <p className={`text-2xl font-bold ${overallMetrics.averageSlippage < 0 ? 'text-green-600' : 'text-red-600'}`}>
                    {overallMetrics.averageSlippage > 0 ? '+' : ''}
                    {overallMetrics.averageSlippage.toFixed(2)} bps
                  </p>
                </div>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardContent className="p-6">
              <div className="flex items-center">
                <CheckCircle className="h-8 w-8 text-purple-600" />
                <div className="ml-4">
                  <p className="text-sm font-medium text-gray-600">Fill Rate</p>
                  <p className="text-2xl font-bold text-gray-900">
                    {(overallMetrics.averageFillRate * 100).toFixed(1)}%
                  </p>
                </div>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardContent className="p-6">
              <div className="flex items-center">
                <Clock className="h-8 w-8 text-yellow-600" />
                <div className="ml-4">
                  <p className="text-sm font-medium text-gray-600">Avg Execution</p>
                  <p className="text-2xl font-bold text-gray-900">
                    {overallMetrics.averageExecutionTime.toFixed(1)}s
                  </p>
                </div>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardContent className="p-6">
              <div className="flex items-center">
                <DollarSign className="h-8 w-8 text-indigo-600" />
                <div className="ml-4">
                  <p className="text-sm font-medium text-gray-600">Cost Savings</p>
                  <p className="text-2xl font-bold text-green-600">
                    ₹{(overallMetrics.bestExecutionSavings / 1000).toFixed(0)}K
                  </p>
                </div>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardContent className="p-6">
              <div className="flex items-center">
                <Award className="h-8 w-8 text-orange-600" />
                <div className="ml-4">
                  <p className="text-sm font-medium text-gray-600">Overall Grade</p>
                  <div className="flex items-center space-x-2">
                    <Badge className={getPerformanceGrade(overallMetrics.averageFillRate).color}>
                      {getPerformanceGrade(overallMetrics.averageFillRate).grade}
                    </Badge>
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      )}

      {/* Main Analytics */}
      <Tabs value={activeTab} onValueChange={(value) => setActiveTab(value as any)}>
        <TabsList className="grid w-full grid-cols-6">
          <TabsTrigger value="overview" className="flex items-center space-x-2">
            <BarChart3 className="h-4 w-4" />
            <span>Overview</span>
          </TabsTrigger>
          <TabsTrigger value="slippage" className="flex items-center space-x-2">
            <Target className="h-4 w-4" />
            <span>Slippage</span>
          </TabsTrigger>
          <TabsTrigger value="venues" className="flex items-center space-x-2">
            <MapPin className="h-4 w-4" />
            <span>Venues</span>
          </TabsTrigger>
          <TabsTrigger value="algorithms" className="flex items-center space-x-2">
            <Zap className="h-4 w-4" />
            <span>Algorithms</span>
          </TabsTrigger>
          <TabsTrigger value="timing" className="flex items-center space-x-2">
            <Clock className="h-4 w-4" />
            <span>Timing</span>
          </TabsTrigger>
          <TabsTrigger value="costs" className="flex items-center space-x-2">
            <DollarSign className="h-4 w-4" />
            <span>Costs</span>
          </TabsTrigger>
        </TabsList>

        <TabsContent value="overview" className="space-y-6">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Execution Quality Heatmap */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center space-x-2">
                  <Gauge className="h-5 w-5" />
                  <span>Execution Quality Matrix</span>
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-4">
                  <div className="grid grid-cols-4 gap-2">
                    <div className="text-xs font-medium text-gray-500">Metric</div>
                    <div className="text-xs font-medium text-gray-500 text-center">Score</div>
                    <div className="text-xs font-medium text-gray-500 text-center">Grade</div>
                    <div className="text-xs font-medium text-gray-500 text-center">Trend</div>
                  </div>
                  
                  {[
                    { name: 'Slippage Control', score: 0.85, trend: 'up' },
                    { name: 'Fill Rate', score: 0.94, trend: 'up' },
                    { name: 'Execution Speed', score: 0.78, trend: 'down' },
                    { name: 'Cost Efficiency', score: 0.82, trend: 'up' },
                    { name: 'Market Impact', score: 0.76, trend: 'stable' },
                    { name: 'Best Execution', score: 0.89, trend: 'up' }
                  ].map((metric, index) => {
                    const grade = getPerformanceGrade(metric.score);
                    return (
                      <div key={metric.name} className="grid grid-cols-4 gap-2 items-center py-2 border-b border-gray-100 last:border-0">
                        <div className="text-sm font-medium">{metric.name}</div>
                        <div className="text-center">
                          <div className="w-full bg-gray-200 rounded-full h-2">
                            <div
                              className="bg-blue-600 h-2 rounded-full"
                              style={{ width: `${metric.score * 100}%` }}
                            />
                          </div>
                        </div>
                        <div className="text-center">
                          <Badge className={grade.color}>{grade.grade}</Badge>
                        </div>
                        <div className="text-center">
                          {metric.trend === 'up' && <ArrowUpRight className="h-4 w-4 text-green-600 mx-auto" />}
                          {metric.trend === 'down' && <ArrowDownRight className="h-4 w-4 text-red-600 mx-auto" />}
                          {metric.trend === 'stable' && <Minus className="h-4 w-4 text-gray-600 mx-auto" />}
                        </div>
                      </div>
                    );
                  })}
                </div>
              </CardContent>
            </Card>

            {/* Market Conditions Impact */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center space-x-2">
                  <Globe className="h-5 w-5" />
                  <span>Market Conditions Analysis</span>
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-4">
                  {[
                    { condition: 'NORMAL', count: 28, avgSlippage: 1.2, color: 'bg-green-100 text-green-800' },
                    { condition: 'VOLATILE', count: 15, avgSlippage: 4.5, color: 'bg-orange-100 text-orange-800' },
                    { condition: 'QUIET', count: 12, avgSlippage: 0.8, color: 'bg-blue-100 text-blue-800' },
                    { condition: 'STRESSED', count: 8, avgSlippage: 7.2, color: 'bg-red-100 text-red-800' }
                  ].map((condition) => (
                    <div key={condition.condition} className="flex items-center justify-between p-4 rounded-lg border">
                      <div className="flex items-center space-x-3">
                        <Badge className={condition.color}>
                          {condition.condition}
                        </Badge>
                        <div>
                          <div className="font-medium">{condition.count} orders</div>
                          <div className="text-sm text-gray-500">
                            Avg slippage: {condition.avgSlippage.toFixed(1)} bps
                          </div>
                        </div>
                      </div>
                      <div className="text-right">
                        <div className="w-24 bg-gray-200 rounded-full h-2">
                          <div
                            className="bg-blue-600 h-2 rounded-full"
                            style={{ width: `${(condition.count / 63) * 100}%` }}
                          />
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </CardContent>
            </Card>
          </div>

          {/* Recent Executions */}
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center space-x-2">
                <Activity className="h-5 w-5" />
                <span>Recent Executions</span>
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead className="bg-gray-50 border-b">
                    <tr>
                      <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Order ID</th>
                      <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Symbol</th>
                      <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Venue</th>
                      <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Algorithm</th>
                      <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Slippage</th>
                      <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Fill Rate</th>
                      <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Time</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-200">
                    {executions.slice(0, 10).map((execution) => (
                      <tr key={execution.id} className="hover:bg-gray-50">
                        <td className="px-4 py-2">
                          <Badge variant="outline">{execution.orderId}</Badge>
                        </td>
                        <td className="px-4 py-2">
                          <div className="flex items-center space-x-2">
                            <span className="font-medium">{execution.symbol}</span>
                            <Badge variant={execution.side === 'BUY' ? 'default' : 'destructive'}>
                              {execution.side}
                            </Badge>
                          </div>
                        </td>
                        <td className="px-4 py-2">
                          <span className="text-sm">{execution.venue}</span>
                        </td>
                        <td className="px-4 py-2">
                          <span className="text-sm">{execution.algorithm}</span>
                        </td>
                        <td className="px-4 py-2">
                          <Badge className={getSlippageColor(execution.slippage)}>
                            {execution.slippage > 0 ? '+' : ''}{execution.slippage.toFixed(1)} bps
                          </Badge>
                        </td>
                        <td className="px-4 py-2">
                          <span className="text-sm font-medium">
                            {((execution.filledQuantity / execution.requestedQuantity) * 100).toFixed(1)}%
                          </span>
                        </td>
                        <td className="px-4 py-2">
                          <span className="text-sm">{execution.executionTime.toFixed(1)}s</span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="venues" className="space-y-6">
          <div className="grid grid-cols-1 lg:grid-cols-2 xl:grid-cols-3 gap-6">
            {venuePerformance.map((venue) => (
              <Card key={venue.venue}>
                <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                  <CardTitle className="text-sm font-medium">{venue.venue}</CardTitle>
                  <Badge className={getPerformanceGrade(venue.fillRate).color}>
                    {getPerformanceGrade(venue.fillRate).grade}
                  </Badge>
                </CardHeader>
                <CardContent>
                  <div className="space-y-3">
                    <div className="flex items-center justify-between">
                      <span className="text-sm text-gray-600">Market Share</span>
                      <span className="text-sm font-medium">{venue.marketShare.toFixed(1)}%</span>
                    </div>
                    
                    <div className="flex items-center justify-between">
                      <span className="text-sm text-gray-600">Avg Slippage</span>
                      <Badge className={getSlippageColor(venue.averageSlippage)}>
                        {venue.averageSlippage > 0 ? '+' : ''}{venue.averageSlippage.toFixed(1)} bps
                      </Badge>
                    </div>
                    
                    <div className="flex items-center justify-between">
                      <span className="text-sm text-gray-600">Fill Rate</span>
                      <span className="text-sm font-medium">{(venue.fillRate * 100).toFixed(1)}%</span>
                    </div>
                    
                    <div className="flex items-center justify-between">
                      <span className="text-sm text-gray-600">Avg Execution</span>
                      <span className="text-sm font-medium">{venue.averageExecutionTime.toFixed(1)}s</span>
                    </div>
                    
                    <div className="flex items-center justify-between">
                      <span className="text-sm text-gray-600">Total Volume</span>
                      <span className="text-sm font-medium">₹{(venue.totalVolume / 10000000).toFixed(1)}Cr</span>
                    </div>
                    
                    <div className="flex items-center justify-between">
                      <span className="text-sm text-gray-600">Cost Savings</span>
                      <span className="text-sm font-medium text-green-600">
                        {(venue.costSavings * 100).toFixed(2)}%
                      </span>
                    </div>
                    
                    <div className="flex items-center justify-between pt-2 border-t">
                      <span className="text-sm text-gray-600">Reliability</span>
                      <div className="flex items-center space-x-2">
                        <span className="text-sm font-medium">{(venue.reliability * 100).toFixed(0)}%</span>
                        <div className="w-12 bg-gray-200 rounded-full h-2">
                          <div
                            className="bg-green-600 h-2 rounded-full"
                            style={{ width: `${venue.reliability * 100}%` }}
                          />
                        </div>
                      </div>
                    </div>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        </TabsContent>

        <TabsContent value="algorithms" className="space-y-6">
          <div className="space-y-6">
            {algorithmPerformance.map((algorithm) => (
              <Card key={algorithm.algorithm}>
                <CardHeader>
                  <div className="flex items-center justify-between">
                    <div className="flex items-center space-x-3">
                      <CardTitle>{algorithm.algorithm}</CardTitle>
                      <Badge className={getPerformanceGrade(algorithm.successRate).color}>
                        {getPerformanceGrade(algorithm.successRate).grade}
                      </Badge>
                    </div>
                    <div className="text-sm text-gray-600">
                      {algorithm.totalOrders} orders
                    </div>
                  </div>
                </CardHeader>
                <CardContent>
                  <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
                    <div className="space-y-2">
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Avg Slippage</span>
                        <Badge className={getSlippageColor(algorithm.averageSlippage)}>
                          {algorithm.averageSlippage > 0 ? '+' : ''}{algorithm.averageSlippage.toFixed(1)} bps
                        </Badge>
                      </div>
                      
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Implementation Shortfall</span>
                        <span className="text-sm font-medium">{algorithm.implementationShortfall.toFixed(1)} bps</span>
                      </div>
                    </div>
                    
                    <div className="space-y-2">
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Fill Rate</span>
                        <span className="text-sm font-medium">{(algorithm.fillRate * 100).toFixed(1)}%</span>
                      </div>
                      
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Success Rate</span>
                        <span className="text-sm font-medium text-green-600">
                          {(algorithm.successRate * 100).toFixed(1)}%
                        </span>
                      </div>
                    </div>
                    
                    <div className="space-y-2">
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Avg Execution Time</span>
                        <span className="text-sm font-medium">{algorithm.averageExecutionTime.toFixed(1)}s</span>
                      </div>
                      
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Market Impact</span>
                        <span className="text-sm font-medium">{algorithm.marketImpact.toFixed(1)} bps</span>
                      </div>
                    </div>
                    
                    <div className="space-y-2">
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Risk Adj. Return</span>
                        <span className="text-sm font-medium text-blue-600">
                          {(algorithm.riskAdjustedReturn * 100).toFixed(2)}%
                        </span>
                      </div>
                      
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Overall Score</span>
                        <div className="flex items-center space-x-2">
                          <span className="text-sm font-medium">{(algorithm.successRate * 100).toFixed(0)}</span>
                          <div className="w-16 bg-gray-200 rounded-full h-2">
                            <div
                              className="bg-blue-600 h-2 rounded-full"
                              style={{ width: `${algorithm.successRate * 100}%` }}
                            />
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        </TabsContent>

        <TabsContent value="timing" className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center space-x-2">
                <Clock className="h-5 w-5" />
                <span>Intraday Performance Analysis</span>
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-6">
                {/* Peak hours analysis */}
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                  <Card className="p-4">
                    <div className="text-center">
                      <div className="text-2xl font-bold text-green-600">09:15-10:00</div>
                      <div className="text-sm text-gray-600">Best Execution Window</div>
                      <div className="text-xs text-gray-500 mt-1">
                        Avg slippage: 0.8 bps
                      </div>
                    </div>
                  </Card>
                  
                  <Card className="p-4">
                    <div className="text-center">
                      <div className="text-2xl font-bold text-blue-600">14:00-15:00</div>
                      <div className="text-sm text-gray-600">Highest Volume</div>
                      <div className="text-xs text-gray-500 mt-1">
                        ₹125Cr traded
                      </div>
                    </div>
                  </Card>
                  
                  <Card className="p-4">
                    <div className="text-center">
                      <div className="text-2xl font-bold text-red-600">11:30-12:30</div>
                      <div className="text-sm text-gray-600">Avoid Window</div>
                      <div className="text-xs text-gray-500 mt-1">
                        High volatility: 3.2%
                      </div>
                    </div>
                  </Card>
                </div>

                {/* Hourly breakdown */}
                <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-6 gap-2">
                  {timeAnalysis.map((hour) => (
                    <div
                      key={hour.hour}
                      className={`p-3 rounded border text-center ${
                        hour.averageSlippage < 1 
                          ? 'bg-green-50 border-green-200' 
                          : hour.averageSlippage > 3 
                          ? 'bg-red-50 border-red-200' 
                          : 'bg-yellow-50 border-yellow-200'
                      }`}
                    >
                      <div className="font-medium">
                        {hour.hour.toString().padStart(2, '0')}:00
                      </div>
                      <div className="text-xs text-gray-600 mt-1">
                        {hour.orderCount} orders
                      </div>
                      <div className={`text-xs font-medium ${
                        hour.averageSlippage < 1 
                          ? 'text-green-600' 
                          : hour.averageSlippage > 3 
                          ? 'text-red-600' 
                          : 'text-yellow-600'
                      }`}>
                        {hour.averageSlippage > 0 ? '+' : ''}{hour.averageSlippage.toFixed(1)} bps
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="costs" className="space-y-6">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center space-x-2">
                  <DollarSign className="h-5 w-5" />
                  <span>Cost Breakdown</span>
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-4">
                  {[
                    { category: 'Commission', amount: 15250, percentage: 45, color: 'bg-blue-600' },
                    { category: 'Slippage Cost', amount: 8750, percentage: 26, color: 'bg-red-600' },
                    { category: 'Market Impact', amount: 6800, percentage: 20, color: 'bg-yellow-600' },
                    { category: 'Timing Cost', amount: 3200, percentage: 9, color: 'bg-green-600' }
                  ].map((cost) => (
                    <div key={cost.category} className="flex items-center justify-between">
                      <div className="flex items-center space-x-3">
                        <div className={`w-4 h-4 rounded ${cost.color}`} />
                        <span className="font-medium">{cost.category}</span>
                      </div>
                      <div className="text-right">
                        <div className="font-medium">₹{cost.amount.toLocaleString()}</div>
                        <div className="text-sm text-gray-500">{cost.percentage}%</div>
                      </div>
                    </div>
                  ))}
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle className="flex items-center space-x-2">
                  <TrendingUp className="h-5 w-5" />
                  <span>Cost Savings Analysis</span>
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-4">
                  <div className="text-center py-4">
                    <div className="text-3xl font-bold text-green-600">₹45,750</div>
                    <div className="text-sm text-gray-600">Total savings this month</div>
                    <div className="text-xs text-gray-500 mt-1">
                      Compared to industry average execution
                    </div>
                  </div>

                  <div className="space-y-3">
                    {[
                      { source: 'Smart Routing', saving: 18500 },
                      { source: 'Dark Pool Access', saving: 12250 },
                      { source: 'Algorithm Optimization', saving: 9800 },
                      { source: 'Venue Selection', saving: 5200 }
                    ].map((saving) => (
                      <div key={saving.source} className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">{saving.source}</span>
                        <span className="text-sm font-medium text-green-600">
                          ₹{saving.saving.toLocaleString()}
                        </span>
                      </div>
                    ))}
                  </div>
                </div>
              </CardContent>
            </Card>
          </div>
        </TabsContent>
      </Tabs>
    </div>
  );
};

export default OrderExecutionAnalytics;