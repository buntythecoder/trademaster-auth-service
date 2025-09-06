import React, { useState, useEffect, useMemo } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  BookOpen, BarChart3, PieChart, LineChart, TrendingUp,
  TrendingDown, Calculator, Target, Award, Eye,
  Search, Filter, Download, Share2, Star, Clock,
  DollarSign, Percent, ArrowUpRight, ArrowDownRight,
  ChevronRight, ChevronDown, Plus, Minus, Info,
  AlertCircle, CheckCircle, XCircle, Users, Globe
} from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Input } from '@/components/ui/input';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';

interface FinancialMetrics {
  symbol: string;
  companyName: string;
  sector: string;
  marketCap: number;
  currentPrice: number;
  // Valuation Metrics
  peRatio: number;
  pbRatio: number;
  pegRatio: number;
  priceToSales: number;
  enterpriseValue: number;
  evToEbitda: number;
  // Profitability
  roe: number; // Return on Equity
  roa: number; // Return on Assets
  roic: number; // Return on Invested Capital
  grossMargin: number;
  operatingMargin: number;
  netMargin: number;
  // Growth
  revenueGrowth: number;
  earningsGrowth: number;
  bookValueGrowth: number;
  // Financial Health
  currentRatio: number;
  quickRatio: number;
  debtToEquity: number;
  interestCoverage: number;
  // Efficiency
  assetTurnover: number;
  inventoryTurnover: number;
  receivablesTurnover: number;
}

interface TechnicalMetrics {
  symbol: string;
  price: number;
  change: number;
  changePercent: number;
  volume: number;
  avgVolume: number;
  // Moving Averages
  sma20: number;
  sma50: number;
  sma200: number;
  ema12: number;
  ema26: number;
  // Oscillators
  rsi: number;
  stochastic: number;
  williamsR: number;
  cciValue: number;
  // Momentum
  macd: number;
  macdSignal: number;
  macdHistogram: number;
  adx: number; // Average Directional Index
  // Volatility
  bollingerUpper: number;
  bollingerLower: number;
  atr: number; // Average True Range
  // Support/Resistance
  pivotPoint: number;
  resistance1: number;
  resistance2: number;
  support1: number;
  support2: number;
}

interface ResearchReport {
  id: string;
  symbol: string;
  title: string;
  analyst: string;
  firm: string;
  rating: 'STRONG_BUY' | 'BUY' | 'HOLD' | 'SELL' | 'STRONG_SELL';
  targetPrice: number;
  currentPrice: number;
  publishDate: Date;
  summary: string;
  keyPoints: string[];
  riskFactors: string[];
  confidenceLevel: number; // 0-1
  priceImpliedReturn: number;
  timeHorizon: string;
}

interface CompetitorAnalysis {
  symbol: string;
  competitors: {
    symbol: string;
    name: string;
    marketCap: number;
    peRatio: number;
    revenueGrowth: number;
    netMargin: number;
    debtToEquity: number;
    roe: number;
    relativeStrength: number; // vs primary symbol
  }[];
  industryAverages: {
    peRatio: number;
    revenueGrowth: number;
    netMargin: number;
    roe: number;
  };
}

interface ResearchDashboardProps {
  symbol: string;
  onSymbolChange?: (symbol: string) => void;
  onReportSelect?: (report: ResearchReport) => void;
}

export const ResearchDashboard: React.FC<ResearchDashboardProps> = ({
  symbol,
  onSymbolChange,
  onReportSelect
}) => {
  const [financialMetrics, setFinancialMetrics] = useState<FinancialMetrics | null>(null);
  const [technicalMetrics, setTechnicalMetrics] = useState<TechnicalMetrics | null>(null);
  const [researchReports, setResearchReports] = useState<ResearchReport[]>([]);
  const [competitorAnalysis, setCompetitorAnalysis] = useState<CompetitorAnalysis | null>(null);
  const [activeTab, setActiveTab] = useState<'overview' | 'fundamentals' | 'technicals' | 'reports' | 'peers'>('overview');
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedTimeframe, setSelectedTimeframe] = useState<'1M' | '3M' | '6M' | '1Y' | '3Y'>('1Y');
  const [watchlist, setWatchlist] = useState<string[]>(['RELIANCE', 'TCS', 'INFY', 'HDFC']);

  useEffect(() => {
    if (symbol) {
      loadResearchData(symbol);
    }
  }, [symbol]);

  const loadResearchData = async (targetSymbol: string) => {
    // Simulate API calls - in real app, these would be actual API calls
    const mockFinancialMetrics: FinancialMetrics = {
      symbol: targetSymbol,
      companyName: getCompanyName(targetSymbol),
      sector: getSector(targetSymbol),
      marketCap: 1654000000000, // 16.54 lakh crores
      currentPrice: targetSymbol === 'RELIANCE' ? 2456.75 : 3245.80,
      // Valuation
      peRatio: targetSymbol === 'RELIANCE' ? 24.5 : 28.3,
      pbRatio: targetSymbol === 'RELIANCE' ? 1.8 : 3.2,
      pegRatio: targetSymbol === 'RELIANCE' ? 1.2 : 1.5,
      priceToSales: targetSymbol === 'RELIANCE' ? 2.1 : 5.4,
      enterpriseValue: 1789000000000,
      evToEbitda: targetSymbol === 'RELIANCE' ? 12.4 : 18.6,
      // Profitability
      roe: targetSymbol === 'RELIANCE' ? 8.2 : 14.5,
      roa: targetSymbol === 'RELIANCE' ? 4.1 : 8.3,
      roic: targetSymbol === 'RELIANCE' ? 6.8 : 12.1,
      grossMargin: targetSymbol === 'RELIANCE' ? 42.3 : 68.2,
      operatingMargin: targetSymbol === 'RELIANCE' ? 15.4 : 26.8,
      netMargin: targetSymbol === 'RELIANCE' ? 7.8 : 18.4,
      // Growth
      revenueGrowth: targetSymbol === 'RELIANCE' ? 12.4 : 8.7,
      earningsGrowth: targetSymbol === 'RELIANCE' ? 18.6 : 15.2,
      bookValueGrowth: targetSymbol === 'RELIANCE' ? 9.3 : 12.8,
      // Financial Health
      currentRatio: targetSymbol === 'RELIANCE' ? 1.2 : 2.8,
      quickRatio: targetSymbol === 'RELIANCE' ? 0.8 : 2.1,
      debtToEquity: targetSymbol === 'RELIANCE' ? 0.4 : 0.1,
      interestCoverage: targetSymbol === 'RELIANCE' ? 8.5 : 45.2,
      // Efficiency
      assetTurnover: targetSymbol === 'RELIANCE' ? 0.52 : 0.45,
      inventoryTurnover: targetSymbol === 'RELIANCE' ? 12.4 : 18.7,
      receivablesTurnover: targetSymbol === 'RELIANCE' ? 8.9 : 6.2
    };

    const mockTechnicalMetrics: TechnicalMetrics = {
      symbol: targetSymbol,
      price: targetSymbol === 'RELIANCE' ? 2456.75 : 3245.80,
      change: targetSymbol === 'RELIANCE' ? 12.45 : -23.20,
      changePercent: targetSymbol === 'RELIANCE' ? 0.51 : -0.71,
      volume: 2847563,
      avgVolume: 2156890,
      // Moving Averages
      sma20: targetSymbol === 'RELIANCE' ? 2435.20 : 3268.40,
      sma50: targetSymbol === 'RELIANCE' ? 2398.80 : 3298.60,
      sma200: targetSymbol === 'RELIANCE' ? 2234.50 : 3156.90,
      ema12: targetSymbol === 'RELIANCE' ? 2448.90 : 3258.70,
      ema26: targetSymbol === 'RELIANCE' ? 2421.30 : 3289.40,
      // Oscillators
      rsi: targetSymbol === 'RELIANCE' ? 68.4 : 42.8,
      stochastic: targetSymbol === 'RELIANCE' ? 72.1 : 35.6,
      williamsR: targetSymbol === 'RELIANCE' ? -27.9 : -64.4,
      cciValue: targetSymbol === 'RELIANCE' ? 145.2 : -89.7,
      // Momentum
      macd: targetSymbol === 'RELIANCE' ? 15.8 : -12.3,
      macdSignal: targetSymbol === 'RELIANCE' ? 12.4 : -8.9,
      macdHistogram: targetSymbol === 'RELIANCE' ? 3.4 : -3.4,
      adx: targetSymbol === 'RELIANCE' ? 28.5 : 15.2,
      // Volatility
      bollingerUpper: targetSymbol === 'RELIANCE' ? 2485.60 : 3298.70,
      bollingerLower: targetSymbol === 'RELIANCE' ? 2398.40 : 3198.30,
      atr: targetSymbol === 'RELIANCE' ? 45.8 : 67.2,
      // Support/Resistance
      pivotPoint: targetSymbol === 'RELIANCE' ? 2425.50 : 3225.80,
      resistance1: targetSymbol === 'RELIANCE' ? 2468.20 : 3289.50,
      resistance2: targetSymbol === 'RELIANCE' ? 2510.90 : 3353.20,
      support1: targetSymbol === 'RELIANCE' ? 2382.80 : 3162.10,
      support2: targetSymbol === 'RELIANCE' ? 2340.10 : 3098.40
    };

    const mockReports: ResearchReport[] = [
      {
        id: 'RPT-001',
        symbol: targetSymbol,
        title: `${targetSymbol} Q3 Results: Margin Expansion Continues`,
        analyst: 'Rajesh Kumar',
        firm: 'Kotak Securities',
        rating: 'BUY',
        targetPrice: targetSymbol === 'RELIANCE' ? 2650 : 3500,
        currentPrice: targetSymbol === 'RELIANCE' ? 2456.75 : 3245.80,
        publishDate: new Date(Date.now() - 2 * 24 * 60 * 60 * 1000),
        summary: `Strong operational performance with margin expansion. Digital business showing robust growth. Maintain BUY rating.`,
        keyPoints: [
          'EBITDA margin improved by 180 bps YoY',
          'Digital subscriber base grew 12% QoQ',
          'Debt reduction ahead of schedule',
          'Strong free cash flow generation',
          'Expansion in retail segment showing traction'
        ],
        riskFactors: [
          'Volatile crude oil prices impact refining margins',
          'Regulatory challenges in telecom segment',
          'Competition in retail market',
          'Global economic slowdown risk'
        ],
        confidenceLevel: 0.82,
        priceImpliedReturn: 7.9,
        timeHorizon: '12 months'
      },
      {
        id: 'RPT-002',
        symbol: targetSymbol,
        title: `${targetSymbol}: Sector Leadership Position Intact`,
        analyst: 'Priya Sharma',
        firm: 'ICICI Securities',
        rating: 'STRONG_BUY',
        targetPrice: targetSymbol === 'RELIANCE' ? 2750 : 3600,
        currentPrice: targetSymbol === 'RELIANCE' ? 2456.75 : 3245.80,
        publishDate: new Date(Date.now() - 5 * 24 * 60 * 60 * 1000),
        summary: `Market leadership position strengthening. Strong execution on strategic initiatives. Upgrade to STRONG BUY.`,
        keyPoints: [
          'Market share gains in core segments',
          'Technology transformation yielding results',
          'ESG initiatives creating long-term value',
          'Strong balance sheet provides flexibility',
          'Management guidance remains optimistic'
        ],
        riskFactors: [
          'Execution risk on large capex projects',
          'Currency volatility impact',
          'Geopolitical tensions affecting supply chain'
        ],
        confidenceLevel: 0.89,
        priceImpliedReturn: 11.9,
        timeHorizon: '12-18 months'
      }
    ];

    const mockCompetitorAnalysis: CompetitorAnalysis = {
      symbol: targetSymbol,
      competitors: [
        {
          symbol: 'COMPETITOR1',
          name: 'Major Competitor 1',
          marketCap: 985000000000,
          peRatio: 22.4,
          revenueGrowth: 10.2,
          netMargin: 8.9,
          debtToEquity: 0.6,
          roe: 12.4,
          relativeStrength: -0.15
        },
        {
          symbol: 'COMPETITOR2',
          name: 'Major Competitor 2',
          marketCap: 754000000000,
          peRatio: 18.7,
          revenueGrowth: 7.8,
          netMargin: 6.2,
          debtToEquity: 0.8,
          roe: 9.8,
          relativeStrength: -0.28
        }
      ],
      industryAverages: {
        peRatio: 21.5,
        revenueGrowth: 9.4,
        netMargin: 7.8,
        roe: 11.2
      }
    };

    setFinancialMetrics(mockFinancialMetrics);
    setTechnicalMetrics(mockTechnicalMetrics);
    setResearchReports(mockReports);
    setCompetitorAnalysis(mockCompetitorAnalysis);
  };

  const getCompanyName = (sym: string) => {
    const names: Record<string, string> = {
      'RELIANCE': 'Reliance Industries Limited',
      'TCS': 'Tata Consultancy Services',
      'INFY': 'Infosys Limited',
      'HDFC': 'HDFC Bank Limited'
    };
    return names[sym] || `${sym} Limited`;
  };

  const getSector = (sym: string) => {
    const sectors: Record<string, string> = {
      'RELIANCE': 'Oil & Gas / Petrochemicals',
      'TCS': 'Information Technology',
      'INFY': 'Information Technology',
      'HDFC': 'Banking & Financial Services'
    };
    return sectors[sym] || 'Diversified';
  };

  const getRatingColor = (rating: ResearchReport['rating']) => {
    switch (rating) {
      case 'STRONG_BUY': return 'bg-green-600 text-white';
      case 'BUY': return 'bg-green-500 text-white';
      case 'HOLD': return 'bg-yellow-500 text-white';
      case 'SELL': return 'bg-red-500 text-white';
      case 'STRONG_SELL': return 'bg-red-600 text-white';
      default: return 'bg-gray-500 text-white';
    }
  };

  const getMetricStatus = (value: number, benchmark: number, higherIsBetter: boolean = true) => {
    const ratio = value / benchmark;
    if (higherIsBetter) {
      if (ratio > 1.1) return 'excellent';
      if (ratio > 1.0) return 'good';
      if (ratio > 0.9) return 'fair';
      return 'poor';
    } else {
      if (ratio < 0.9) return 'excellent';
      if (ratio < 1.0) return 'good';
      if (ratio < 1.1) return 'fair';
      return 'poor';
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'excellent': return 'text-green-600 bg-green-50';
      case 'good': return 'text-blue-600 bg-blue-50';
      case 'fair': return 'text-yellow-600 bg-yellow-50';
      case 'poor': return 'text-red-600 bg-red-50';
      default: return 'text-gray-600 bg-gray-50';
    }
  };

  const technicalSignals = useMemo(() => {
    if (!technicalMetrics) return [];
    
    const signals = [];
    
    // Price vs Moving Averages
    if (technicalMetrics.price > technicalMetrics.sma20) {
      signals.push({ type: 'BULLISH', indicator: 'Price > SMA20', strength: 0.7 });
    }
    if (technicalMetrics.price > technicalMetrics.sma200) {
      signals.push({ type: 'BULLISH', indicator: 'Price > SMA200', strength: 0.8 });
    }
    
    // RSI Analysis
    if (technicalMetrics.rsi > 70) {
      signals.push({ type: 'BEARISH', indicator: 'RSI Overbought', strength: 0.6 });
    } else if (technicalMetrics.rsi < 30) {
      signals.push({ type: 'BULLISH', indicator: 'RSI Oversold', strength: 0.6 });
    }
    
    // MACD
    if (technicalMetrics.macd > technicalMetrics.macdSignal) {
      signals.push({ type: 'BULLISH', indicator: 'MACD Bullish', strength: 0.7 });
    }
    
    // Volume
    if (technicalMetrics.volume > technicalMetrics.avgVolume * 1.5) {
      signals.push({ type: 'NEUTRAL', indicator: 'High Volume', strength: 0.8 });
    }
    
    return signals;
  }, [technicalMetrics]);

  return (
    <div className="p-6 space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Research Dashboard</h1>
          <p className="text-gray-600">Comprehensive fundamental and technical analysis</p>
        </div>
        <div className="flex items-center space-x-3">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 h-4 w-4" />
            <Input
              placeholder="Search symbols..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pl-10 w-64"
            />
          </div>
          <Button variant="outline" className="flex items-center space-x-2">
            <Download className="h-4 w-4" />
            <span>Export</span>
          </Button>
          <Button variant="outline" className="flex items-center space-x-2">
            <Share2 className="h-4 w-4" />
            <span>Share</span>
          </Button>
        </div>
      </div>

      {/* Symbol Header */}
      {financialMetrics && (
        <Card className="bg-gradient-to-r from-blue-50 to-indigo-50 border-blue-200">
          <CardContent className="p-6">
            <div className="flex items-center justify-between">
              <div className="flex items-center space-x-4">
                <div>
                  <h2 className="text-2xl font-bold text-blue-900">{financialMetrics.symbol}</h2>
                  <p className="text-blue-700">{financialMetrics.companyName}</p>
                  <p className="text-sm text-blue-600">{financialMetrics.sector}</p>
                </div>
                <div className="text-right">
                  <div className="text-3xl font-bold text-blue-900">
                    ₹{financialMetrics.currentPrice.toFixed(2)}
                  </div>
                  <div className="flex items-center space-x-2">
                    <Badge className="bg-blue-600 text-white">
                      PE: {financialMetrics.peRatio.toFixed(1)}
                    </Badge>
                    <Badge className="bg-indigo-600 text-white">
                      MC: ₹{(financialMetrics.marketCap / 10000000).toFixed(0)}Cr
                    </Badge>
                  </div>
                </div>
              </div>
              
              <div className="flex items-center space-x-2">
                <Button
                  variant={watchlist.includes(symbol) ? "default" : "outline"}
                  onClick={() => {
                    if (watchlist.includes(symbol)) {
                      setWatchlist(watchlist.filter(s => s !== symbol));
                    } else {
                      setWatchlist([...watchlist, symbol]);
                    }
                  }}
                  className="flex items-center space-x-2"
                >
                  <Star className={`h-4 w-4 ${watchlist.includes(symbol) ? 'fill-current' : ''}`} />
                  <span>{watchlist.includes(symbol) ? 'Remove' : 'Add to Watchlist'}</span>
                </Button>
              </div>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Main Content */}
      <Tabs value={activeTab} onValueChange={(value) => setActiveTab(value as any)}>
        <TabsList className="grid w-full grid-cols-5">
          <TabsTrigger value="overview" className="flex items-center space-x-2">
            <Eye className="h-4 w-4" />
            <span>Overview</span>
          </TabsTrigger>
          <TabsTrigger value="fundamentals" className="flex items-center space-x-2">
            <Calculator className="h-4 w-4" />
            <span>Fundamentals</span>
          </TabsTrigger>
          <TabsTrigger value="technicals" className="flex items-center space-x-2">
            <BarChart3 className="h-4 w-4" />
            <span>Technical</span>
          </TabsTrigger>
          <TabsTrigger value="reports" className="flex items-center space-x-2">
            <BookOpen className="h-4 w-4" />
            <span>Reports</span>
          </TabsTrigger>
          <TabsTrigger value="peers" className="flex items-center space-x-2">
            <Users className="h-4 w-4" />
            <span>Peer Analysis</span>
          </TabsTrigger>
        </TabsList>

        <TabsContent value="overview" className="space-y-6">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Key Metrics Overview */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center space-x-2">
                  <Target className="h-5 w-5" />
                  <span>Key Metrics</span>
                </CardTitle>
              </CardHeader>
              <CardContent>
                {financialMetrics && (
                  <div className="grid grid-cols-2 gap-4">
                    <div className="space-y-3">
                      <div className="flex justify-between">
                        <span className="text-sm text-gray-600">P/E Ratio</span>
                        <span className="font-medium">{financialMetrics.peRatio.toFixed(1)}</span>
                      </div>
                      <div className="flex justify-between">
                        <span className="text-sm text-gray-600">P/B Ratio</span>
                        <span className="font-medium">{financialMetrics.pbRatio.toFixed(1)}</span>
                      </div>
                      <div className="flex justify-between">
                        <span className="text-sm text-gray-600">ROE</span>
                        <span className="font-medium">{financialMetrics.roe.toFixed(1)}%</span>
                      </div>
                      <div className="flex justify-between">
                        <span className="text-sm text-gray-600">Debt/Equity</span>
                        <span className="font-medium">{financialMetrics.debtToEquity.toFixed(1)}</span>
                      </div>
                    </div>
                    <div className="space-y-3">
                      <div className="flex justify-between">
                        <span className="text-sm text-gray-600">Rev Growth</span>
                        <span className="font-medium text-green-600">
                          {financialMetrics.revenueGrowth.toFixed(1)}%
                        </span>
                      </div>
                      <div className="flex justify-between">
                        <span className="text-sm text-gray-600">Net Margin</span>
                        <span className="font-medium">{financialMetrics.netMargin.toFixed(1)}%</span>
                      </div>
                      <div className="flex justify-between">
                        <span className="text-sm text-gray-600">Current Ratio</span>
                        <span className="font-medium">{financialMetrics.currentRatio.toFixed(1)}</span>
                      </div>
                      <div className="flex justify-between">
                        <span className="text-sm text-gray-600">EV/EBITDA</span>
                        <span className="font-medium">{financialMetrics.evToEbitda.toFixed(1)}</span>
                      </div>
                    </div>
                  </div>
                )}
              </CardContent>
            </Card>

            {/* Technical Signals */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center space-x-2">
                  <BarChart3 className="h-5 w-5" />
                  <span>Technical Signals</span>
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-3">
                  {technicalSignals.slice(0, 6).map((signal, index) => (
                    <div key={index} className="flex items-center justify-between">
                      <div className="flex items-center space-x-2">
                        {signal.type === 'BULLISH' ? (
                          <TrendingUp className="h-4 w-4 text-green-600" />
                        ) : signal.type === 'BEARISH' ? (
                          <TrendingDown className="h-4 w-4 text-red-600" />
                        ) : (
                          <Minus className="h-4 w-4 text-gray-600" />
                        )}
                        <span className="text-sm">{signal.indicator}</span>
                      </div>
                      <div className="w-16 bg-gray-200 rounded-full h-2">
                        <div
                          className={`h-2 rounded-full ${
                            signal.type === 'BULLISH' ? 'bg-green-600' :
                            signal.type === 'BEARISH' ? 'bg-red-600' : 'bg-gray-600'
                          }`}
                          style={{ width: `${signal.strength * 100}%` }}
                        />
                      </div>
                    </div>
                  ))}
                </div>
              </CardContent>
            </Card>
          </div>

          {/* Recent Research Reports */}
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center space-x-2">
                <BookOpen className="h-5 w-5" />
                <span>Recent Research</span>
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                {researchReports.slice(0, 2).map((report) => (
                  <div key={report.id} className="border border-gray-200 rounded-lg p-4">
                    <div className="flex items-center justify-between mb-2">
                      <div className="flex items-center space-x-3">
                        <h4 className="font-medium">{report.title}</h4>
                        <Badge className={getRatingColor(report.rating)}>
                          {report.rating.replace('_', ' ')}
                        </Badge>
                      </div>
                      <div className="text-sm text-gray-600">
                        {report.publishDate.toLocaleDateString()}
                      </div>
                    </div>
                    <p className="text-sm text-gray-600 mb-2">{report.summary}</p>
                    <div className="flex items-center justify-between text-sm">
                      <span className="text-gray-600">{report.analyst} • {report.firm}</span>
                      <span className="font-medium text-green-600">
                        Target: ₹{report.targetPrice} ({report.priceImpliedReturn.toFixed(1)}%)
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="fundamentals" className="space-y-6">
          {financialMetrics && competitorAnalysis && (
            <>
              {/* Valuation Metrics */}
              <Card>
                <CardHeader>
                  <CardTitle>Valuation Metrics</CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                    {[
                      { name: 'P/E Ratio', value: financialMetrics.peRatio, benchmark: competitorAnalysis.industryAverages.peRatio, suffix: 'x', higherIsBetter: false },
                      { name: 'P/B Ratio', value: financialMetrics.pbRatio, benchmark: 2.5, suffix: 'x', higherIsBetter: false },
                      { name: 'PEG Ratio', value: financialMetrics.pegRatio, benchmark: 1.5, suffix: 'x', higherIsBetter: false },
                      { name: 'Price/Sales', value: financialMetrics.priceToSales, benchmark: 3.0, suffix: 'x', higherIsBetter: false },
                      { name: 'EV/EBITDA', value: financialMetrics.evToEbitda, benchmark: 15.0, suffix: 'x', higherIsBetter: false },
                      { name: 'Market Cap', value: financialMetrics.marketCap / 10000000, benchmark: 100000, suffix: 'Cr', higherIsBetter: true }
                    ].map((metric, index) => {
                      const status = getMetricStatus(metric.value, metric.benchmark, metric.higherIsBetter);
                      return (
                        <div key={index} className="text-center">
                          <div className="text-2xl font-bold">{metric.value.toFixed(1)}{metric.suffix}</div>
                          <div className="text-sm text-gray-600 mb-2">{metric.name}</div>
                          <Badge className={getStatusColor(status)}>
                            {status.charAt(0).toUpperCase() + status.slice(1)}
                          </Badge>
                        </div>
                      );
                    })}
                  </div>
                </CardContent>
              </Card>

              {/* Profitability Analysis */}
              <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                <Card>
                  <CardHeader>
                    <CardTitle>Profitability</CardTitle>
                  </CardHeader>
                  <CardContent>
                    <div className="space-y-4">
                      {[
                        { name: 'Gross Margin', value: financialMetrics.grossMargin, benchmark: 45 },
                        { name: 'Operating Margin', value: financialMetrics.operatingMargin, benchmark: 20 },
                        { name: 'Net Margin', value: financialMetrics.netMargin, benchmark: competitorAnalysis.industryAverages.netMargin },
                        { name: 'Return on Equity', value: financialMetrics.roe, benchmark: competitorAnalysis.industryAverages.roe },
                        { name: 'Return on Assets', value: financialMetrics.roa, benchmark: 8 },
                        { name: 'ROIC', value: financialMetrics.roic, benchmark: 12 }
                      ].map((metric, index) => (
                        <div key={index} className="flex items-center justify-between">
                          <span className="text-sm text-gray-600">{metric.name}</span>
                          <div className="flex items-center space-x-2">
                            <span className="font-medium w-12 text-right">{metric.value.toFixed(1)}%</span>
                            <div className="w-20 bg-gray-200 rounded-full h-2">
                              <div
                                className={`h-2 rounded-full ${
                                  metric.value > metric.benchmark ? 'bg-green-600' : 
                                  metric.value > metric.benchmark * 0.8 ? 'bg-yellow-600' : 'bg-red-600'
                                }`}
                                style={{ width: `${Math.min((metric.value / metric.benchmark) * 100, 100)}%` }}
                              />
                            </div>
                          </div>
                        </div>
                      ))}
                    </div>
                  </CardContent>
                </Card>

                <Card>
                  <CardHeader>
                    <CardTitle>Financial Health</CardTitle>
                  </CardHeader>
                  <CardContent>
                    <div className="space-y-4">
                      {[
                        { name: 'Current Ratio', value: financialMetrics.currentRatio, benchmark: 1.5, format: 'ratio' },
                        { name: 'Quick Ratio', value: financialMetrics.quickRatio, benchmark: 1.0, format: 'ratio' },
                        { name: 'Debt/Equity', value: financialMetrics.debtToEquity, benchmark: 0.5, format: 'ratio', higherIsBetter: false },
                        { name: 'Interest Coverage', value: financialMetrics.interestCoverage, benchmark: 5.0, format: 'ratio' },
                        { name: 'Asset Turnover', value: financialMetrics.assetTurnover, benchmark: 0.6, format: 'ratio' },
                        { name: 'Inventory Turnover', value: financialMetrics.inventoryTurnover, benchmark: 10, format: 'ratio' }
                      ].map((metric, index) => {
                        const isGood = metric.higherIsBetter !== false ? 
                          metric.value > metric.benchmark : 
                          metric.value < metric.benchmark;
                        
                        return (
                          <div key={index} className="flex items-center justify-between">
                            <span className="text-sm text-gray-600">{metric.name}</span>
                            <div className="flex items-center space-x-2">
                              <span className="font-medium w-12 text-right">{metric.value.toFixed(1)}</span>
                              {isGood ? (
                                <CheckCircle className="h-4 w-4 text-green-600" />
                              ) : (
                                <AlertCircle className="h-4 w-4 text-red-600" />
                              )}
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  </CardContent>
                </Card>
              </div>

              {/* Growth Metrics */}
              <Card>
                <CardHeader>
                  <CardTitle>Growth Analysis</CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                    {[
                      { name: 'Revenue Growth', value: financialMetrics.revenueGrowth, benchmark: competitorAnalysis.industryAverages.revenueGrowth },
                      { name: 'Earnings Growth', value: financialMetrics.earningsGrowth, benchmark: 15 },
                      { name: 'Book Value Growth', value: financialMetrics.bookValueGrowth, benchmark: 12 }
                    ].map((growth, index) => (
                      <div key={index} className="text-center">
                        <div className={`text-3xl font-bold ${
                          growth.value > growth.benchmark ? 'text-green-600' : 
                          growth.value > 0 ? 'text-yellow-600' : 'text-red-600'
                        }`}>
                          {growth.value > 0 ? '+' : ''}{growth.value.toFixed(1)}%
                        </div>
                        <div className="text-sm text-gray-600">{growth.name}</div>
                        <div className="text-xs text-gray-500 mt-1">
                          vs {growth.benchmark.toFixed(1)}% benchmark
                        </div>
                      </div>
                    ))}
                  </div>
                </CardContent>
              </Card>
            </>
          )}
        </TabsContent>

        <TabsContent value="technicals" className="space-y-6">
          {technicalMetrics && (
            <>
              {/* Price and Moving Averages */}
              <Card>
                <CardHeader>
                  <CardTitle>Price & Moving Averages</CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                    <div className="text-center">
                      <div className="text-2xl font-bold">₹{technicalMetrics.price.toFixed(2)}</div>
                      <div className="text-sm text-gray-600">Current Price</div>
                      <div className={`text-xs font-medium ${
                        technicalMetrics.change > 0 ? 'text-green-600' : 'text-red-600'
                      }`}>
                        {technicalMetrics.change > 0 ? '+' : ''}
                        {technicalMetrics.change.toFixed(2)} ({technicalMetrics.changePercent.toFixed(2)}%)
                      </div>
                    </div>
                    
                    {[
                      { name: 'SMA 20', value: technicalMetrics.sma20 },
                      { name: 'SMA 50', value: technicalMetrics.sma50 },
                      { name: 'SMA 200', value: technicalMetrics.sma200 }
                    ].map((ma, index) => (
                      <div key={index} className="text-center">
                        <div className="text-xl font-bold">₹{ma.value.toFixed(2)}</div>
                        <div className="text-sm text-gray-600">{ma.name}</div>
                        <div className={`text-xs font-medium ${
                          technicalMetrics.price > ma.value ? 'text-green-600' : 'text-red-600'
                        }`}>
                          {((technicalMetrics.price / ma.value - 1) * 100).toFixed(1)}%
                        </div>
                      </div>
                    ))}
                  </div>
                </CardContent>
              </Card>

              {/* Technical Indicators */}
              <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                <Card>
                  <CardHeader>
                    <CardTitle>Momentum Oscillators</CardTitle>
                  </CardHeader>
                  <CardContent>
                    <div className="space-y-6">
                      {[
                        { name: 'RSI (14)', value: technicalMetrics.rsi, max: 100, zones: [30, 70] },
                        { name: 'Stochastic', value: technicalMetrics.stochastic, max: 100, zones: [20, 80] },
                        { name: 'Williams %R', value: Math.abs(technicalMetrics.williamsR), max: 100, zones: [20, 80] },
                        { name: 'CCI', value: Math.min(Math.max(technicalMetrics.cciValue + 200, 0), 400), max: 400, zones: [100, 300] }
                      ].map((indicator, index) => {
                        const percentage = (indicator.value / indicator.max) * 100;
                        const isOverbought = indicator.value > indicator.zones[1];
                        const isOversold = indicator.value < indicator.zones[0];
                        
                        return (
                          <div key={index}>
                            <div className="flex justify-between items-center mb-2">
                              <span className="text-sm font-medium">{indicator.name}</span>
                              <div className="flex items-center space-x-2">
                                <span className="font-bold">{indicator.value.toFixed(1)}</span>
                                {isOverbought && <Badge className="bg-red-100 text-red-800">Overbought</Badge>}
                                {isOversold && <Badge className="bg-green-100 text-green-800">Oversold</Badge>}
                              </div>
                            </div>
                            <div className="relative w-full bg-gray-200 rounded-full h-3">
                              <div
                                className={`h-3 rounded-full ${
                                  isOverbought ? 'bg-red-600' : 
                                  isOversold ? 'bg-green-600' : 'bg-blue-600'
                                }`}
                                style={{ width: `${percentage}%` }}
                              />
                              <div 
                                className="absolute top-0 h-3 w-0.5 bg-yellow-500"
                                style={{ left: `${(indicator.zones[0] / indicator.max) * 100}%` }}
                              />
                              <div 
                                className="absolute top-0 h-3 w-0.5 bg-yellow-500"
                                style={{ left: `${(indicator.zones[1] / indicator.max) * 100}%` }}
                              />
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  </CardContent>
                </Card>

                <Card>
                  <CardHeader>
                    <CardTitle>Support & Resistance</CardTitle>
                  </CardHeader>
                  <CardContent>
                    <div className="space-y-4">
                      <div>
                        <h4 className="font-medium text-green-700 mb-2">Resistance Levels</h4>
                        <div className="space-y-2">
                          {[
                            { level: technicalMetrics.resistance2, label: 'R2', strength: 0.7 },
                            { level: technicalMetrics.resistance1, label: 'R1', strength: 0.8 },
                            { level: technicalMetrics.pivotPoint, label: 'Pivot', strength: 0.9 }
                          ].map((res, index) => (
                            <div key={index} className="flex items-center justify-between">
                              <div className="flex items-center space-x-2">
                                <span className="text-sm font-medium text-red-700">{res.label}</span>
                                <div className="w-16 bg-gray-200 rounded-full h-1">
                                  <div
                                    className="bg-red-600 h-1 rounded-full"
                                    style={{ width: `${res.strength * 100}%` }}
                                  />
                                </div>
                              </div>
                              <span className="font-medium">₹{res.level.toFixed(2)}</span>
                            </div>
                          ))}
                        </div>
                      </div>

                      <div className="border-t pt-4">
                        <h4 className="font-medium text-green-700 mb-2">Support Levels</h4>
                        <div className="space-y-2">
                          {[
                            { level: technicalMetrics.support1, label: 'S1', strength: 0.8 },
                            { level: technicalMetrics.support2, label: 'S2', strength: 0.7 }
                          ].map((sup, index) => (
                            <div key={index} className="flex items-center justify-between">
                              <div className="flex items-center space-x-2">
                                <span className="text-sm font-medium text-green-700">{sup.label}</span>
                                <div className="w-16 bg-gray-200 rounded-full h-1">
                                  <div
                                    className="bg-green-600 h-1 rounded-full"
                                    style={{ width: `${sup.strength * 100}%` }}
                                  />
                                </div>
                              </div>
                              <span className="font-medium">₹{sup.level.toFixed(2)}</span>
                            </div>
                          ))}
                        </div>
                      </div>

                      <div className="border-t pt-4">
                        <div className="grid grid-cols-2 gap-4 text-sm">
                          <div>
                            <span className="text-gray-600">ATR (14):</span>
                            <span className="ml-2 font-medium">₹{technicalMetrics.atr.toFixed(2)}</span>
                          </div>
                          <div>
                            <span className="text-gray-600">ADX:</span>
                            <span className="ml-2 font-medium">{technicalMetrics.adx.toFixed(1)}</span>
                          </div>
                        </div>
                      </div>
                    </div>
                  </CardContent>
                </Card>
              </div>

              {/* Volume Analysis */}
              <Card>
                <CardHeader>
                  <CardTitle>Volume Analysis</CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                    <div className="text-center">
                      <div className="text-2xl font-bold">
                        {(technicalMetrics.volume / 1000000).toFixed(2)}M
                      </div>
                      <div className="text-sm text-gray-600">Current Volume</div>
                      <div className={`text-xs font-medium mt-1 ${
                        technicalMetrics.volume > technicalMetrics.avgVolume ? 'text-green-600' : 'text-red-600'
                      }`}>
                        {((technicalMetrics.volume / technicalMetrics.avgVolume - 1) * 100).toFixed(1)}% vs avg
                      </div>
                    </div>
                    
                    <div className="text-center">
                      <div className="text-2xl font-bold">
                        {(technicalMetrics.avgVolume / 1000000).toFixed(2)}M
                      </div>
                      <div className="text-sm text-gray-600">Average Volume (20d)</div>
                    </div>
                    
                    <div className="text-center">
                      <div className="w-full bg-gray-200 rounded-full h-4 mb-2">
                        <div
                          className={`h-4 rounded-full ${
                            technicalMetrics.volume > technicalMetrics.avgVolume * 1.5 ? 'bg-green-600' :
                            technicalMetrics.volume > technicalMetrics.avgVolume ? 'bg-yellow-600' : 'bg-red-600'
                          }`}
                          style={{ width: `${Math.min((technicalMetrics.volume / technicalMetrics.avgVolume) * 100, 200)}%` }}
                        />
                      </div>
                      <div className="text-sm text-gray-600">Volume Strength</div>
                    </div>
                  </div>
                </CardContent>
              </Card>
            </>
          )}
        </TabsContent>

        <TabsContent value="reports" className="space-y-4">
          <div className="space-y-4">
            {researchReports.map((report) => (
              <Card key={report.id} className="hover:shadow-lg transition-shadow">
                <CardHeader>
                  <div className="flex items-center justify-between">
                    <div className="flex items-center space-x-4">
                      <div>
                        <CardTitle className="text-lg">{report.title}</CardTitle>
                        <p className="text-sm text-gray-600">{report.analyst} • {report.firm}</p>
                      </div>
                      <div className="flex items-center space-x-2">
                        <Badge className={getRatingColor(report.rating)}>
                          {report.rating.replace('_', ' ')}
                        </Badge>
                        <Badge variant="outline">
                          {(report.confidenceLevel * 100).toFixed(0)}% confidence
                        </Badge>
                      </div>
                    </div>
                    <div className="text-right">
                      <div className="text-lg font-bold text-green-600">
                        ₹{report.targetPrice}
                      </div>
                      <div className="text-sm text-gray-600">
                        {report.priceImpliedReturn > 0 ? '+' : ''}{report.priceImpliedReturn.toFixed(1)}% upside
                      </div>
                    </div>
                  </div>
                </CardHeader>
                <CardContent>
                  <div className="space-y-4">
                    <p className="text-gray-700">{report.summary}</p>
                    
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                      <div>
                        <h4 className="font-medium text-gray-900 mb-2">Key Investment Points</h4>
                        <ul className="space-y-1">
                          {report.keyPoints.map((point, index) => (
                            <li key={index} className="flex items-start space-x-2 text-sm">
                              <CheckCircle className="h-3 w-3 mt-0.5 text-green-600" />
                              <span>{point}</span>
                            </li>
                          ))}
                        </ul>
                      </div>
                      
                      <div>
                        <h4 className="font-medium text-gray-900 mb-2">Risk Factors</h4>
                        <ul className="space-y-1">
                          {report.riskFactors.map((risk, index) => (
                            <li key={index} className="flex items-start space-x-2 text-sm">
                              <AlertCircle className="h-3 w-3 mt-0.5 text-red-600" />
                              <span>{risk}</span>
                            </li>
                          ))}
                        </ul>
                      </div>
                    </div>
                    
                    <div className="flex items-center justify-between pt-4 border-t">
                      <div className="flex items-center space-x-4 text-sm text-gray-600">
                        <span>Published: {report.publishDate.toLocaleDateString()}</span>
                        <span>•</span>
                        <span>Time Horizon: {report.timeHorizon}</span>
                      </div>
                      <div className="flex items-center space-x-2">
                        <Button size="sm" variant="outline">
                          <Download className="h-4 w-4" />
                        </Button>
                        <Button size="sm" variant="outline">
                          <Share2 className="h-4 w-4" />
                        </Button>
                      </div>
                    </div>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        </TabsContent>

        <TabsContent value="peers" className="space-y-6">
          {competitorAnalysis && financialMetrics && (
            <Card>
              <CardHeader>
                <CardTitle>Peer Comparison</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="overflow-x-auto">
                  <table className="w-full">
                    <thead className="bg-gray-50">
                      <tr>
                        <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Company</th>
                        <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Market Cap</th>
                        <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">P/E Ratio</th>
                        <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Revenue Growth</th>
                        <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Net Margin</th>
                        <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">ROE</th>
                        <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Debt/Equity</th>
                        <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Rel. Strength</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-gray-200">
                      {/* Current Company */}
                      <tr className="bg-blue-50">
                        <td className="px-4 py-2 font-medium text-blue-900">
                          {financialMetrics.symbol} (Current)
                        </td>
                        <td className="px-4 py-2">₹{(financialMetrics.marketCap / 10000000).toFixed(0)}Cr</td>
                        <td className="px-4 py-2">{financialMetrics.peRatio.toFixed(1)}</td>
                        <td className="px-4 py-2 text-green-600">{financialMetrics.revenueGrowth.toFixed(1)}%</td>
                        <td className="px-4 py-2">{financialMetrics.netMargin.toFixed(1)}%</td>
                        <td className="px-4 py-2">{financialMetrics.roe.toFixed(1)}%</td>
                        <td className="px-4 py-2">{financialMetrics.debtToEquity.toFixed(1)}</td>
                        <td className="px-4 py-2">—</td>
                      </tr>
                      
                      {/* Competitors */}
                      {competitorAnalysis.competitors.map((competitor, index) => (
                        <tr key={index}>
                          <td className="px-4 py-2 font-medium">{competitor.name}</td>
                          <td className="px-4 py-2">₹{(competitor.marketCap / 10000000).toFixed(0)}Cr</td>
                          <td className="px-4 py-2">{competitor.peRatio.toFixed(1)}</td>
                          <td className="px-4 py-2 text-green-600">{competitor.revenueGrowth.toFixed(1)}%</td>
                          <td className="px-4 py-2">{competitor.netMargin.toFixed(1)}%</td>
                          <td className="px-4 py-2">{competitor.roe.toFixed(1)}%</td>
                          <td className="px-4 py-2">{competitor.debtToEquity.toFixed(1)}</td>
                          <td className="px-4 py-2">
                            <div className={`flex items-center ${
                              competitor.relativeStrength > 0 ? 'text-green-600' : 'text-red-600'
                            }`}>
                              {competitor.relativeStrength > 0 ? 
                                <ArrowUpRight className="h-3 w-3" /> : 
                                <ArrowDownRight className="h-3 w-3" />
                              }
                              <span className="ml-1">{Math.abs(competitor.relativeStrength * 100).toFixed(0)}%</span>
                            </div>
                          </td>
                        </tr>
                      ))}
                      
                      {/* Industry Average */}
                      <tr className="bg-gray-50 font-medium">
                        <td className="px-4 py-2">Industry Average</td>
                        <td className="px-4 py-2">—</td>
                        <td className="px-4 py-2">{competitorAnalysis.industryAverages.peRatio.toFixed(1)}</td>
                        <td className="px-4 py-2">{competitorAnalysis.industryAverages.revenueGrowth.toFixed(1)}%</td>
                        <td className="px-4 py-2">{competitorAnalysis.industryAverages.netMargin.toFixed(1)}%</td>
                        <td className="px-4 py-2">{competitorAnalysis.industryAverages.roe.toFixed(1)}%</td>
                        <td className="px-4 py-2">—</td>
                        <td className="px-4 py-2">—</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </CardContent>
            </Card>
          )}
        </TabsContent>
      </Tabs>
    </div>
  );
};

export default ResearchDashboard;