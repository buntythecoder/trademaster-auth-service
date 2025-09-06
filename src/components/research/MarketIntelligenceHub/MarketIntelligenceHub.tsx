import React, { useState, useEffect, useMemo } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Brain, TrendingUp, TrendingDown, Activity, AlertTriangle,
  Target, Zap, Eye, BarChart3, PieChart, LineChart,
  Calendar, Clock, Globe, Users, Star, Filter,
  RefreshCw, Settings, Download, Share2, BookOpen,
  Lightbulb, Shield, Gauge, Network, Atom,
  ChevronRight, ChevronDown, Plus, Minus
} from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';

interface AIInsight {
  id: string;
  type: 'BULLISH' | 'BEARISH' | 'NEUTRAL' | 'ALERT';
  symbol: string;
  title: string;
  description: string;
  confidence: number; // 0-1
  impact: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  timeframe: '1H' | '1D' | '1W' | '1M' | '3M';
  category: 'TECHNICAL' | 'FUNDAMENTAL' | 'SENTIMENT' | 'NEWS' | 'FLOW' | 'RISK';
  signals: {
    technicalScore: number;
    fundamentalScore: number;
    sentimentScore: number;
    momentumScore: number;
  };
  keyFactors: string[];
  timestamp: Date;
  actionable: {
    recommendation: 'BUY' | 'SELL' | 'HOLD' | 'WATCH';
    targetPrice?: number;
    stopLoss?: number;
    timeHorizon?: string;
  };
}

interface MarketRegime {
  id: string;
  name: string;
  type: 'BULL_MARKET' | 'BEAR_MARKET' | 'SIDEWAYS' | 'VOLATILE' | 'LOW_VOL';
  confidence: number;
  duration: number; // days
  characteristics: string[];
  sectors: {
    outperforming: string[];
    underperforming: string[];
  };
  strategies: string[];
  riskLevel: number; // 0-1
}

interface TrendAnalysis {
  symbol: string;
  trend: {
    shortTerm: 'UP' | 'DOWN' | 'SIDEWAYS';
    mediumTerm: 'UP' | 'DOWN' | 'SIDEWAYS';
    longTerm: 'UP' | 'DOWN' | 'SIDEWAYS';
  };
  strength: {
    momentum: number; // 0-100
    volume: number; // 0-100
    breadth: number; // 0-100
  };
  support: number[];
  resistance: number[];
  keyLevels: {
    price: number;
    type: 'SUPPORT' | 'RESISTANCE' | 'PIVOT';
    strength: number;
  }[];
}

interface MacroIndicator {
  name: string;
  value: number;
  change: number;
  impact: 'POSITIVE' | 'NEGATIVE' | 'NEUTRAL';
  importance: number; // 0-1
  description: string;
  trend: number[]; // historical values
  nextUpdate: Date;
}

interface MarketIntelligenceProps {
  userId: string;
  selectedSymbols?: string[];
  onInsightClick?: (insight: AIInsight) => void;
  onRegimeChange?: (regime: MarketRegime) => void;
}

export const MarketIntelligenceHub: React.FC<MarketIntelligenceProps> = ({
  userId,
  selectedSymbols = ['NIFTY', 'RELIANCE', 'TCS', 'INFY'],
  onInsightClick,
  onRegimeChange
}) => {
  const [insights, setInsights] = useState<AIInsight[]>([]);
  const [marketRegime, setMarketRegime] = useState<MarketRegime | null>(null);
  const [trendAnalysis, setTrendAnalysis] = useState<Record<string, TrendAnalysis>>({});
  const [macroIndicators, setMacroIndicators] = useState<MacroIndicator[]>([]);
  const [activeTab, setActiveTab] = useState<'insights' | 'trends' | 'regime' | 'macro' | 'alerts'>('insights');
  const [filterCategory, setFilterCategory] = useState<string>('ALL');
  const [timeframe, setTimeframe] = useState<'1H' | '1D' | '1W' | '1M' | '3M'>('1D');
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [expandedInsights, setExpandedInsights] = useState<Set<string>>(new Set());

  useEffect(() => {
    initializeMockData();
    const interval = setInterval(refreshInsights, 30000); // Update every 30 seconds
    return () => clearInterval(interval);
  }, []);

  const initializeMockData = () => {
    const mockInsights: AIInsight[] = [
      {
        id: 'AI-001',
        type: 'BULLISH',
        symbol: 'RELIANCE',
        title: 'Strong Breakout Pattern Detected',
        description: 'RELIANCE has broken above key resistance at ₹2,450 with strong volume confirmation. Technical indicators show bullish momentum building.',
        confidence: 0.85,
        impact: 'HIGH',
        timeframe: '1D',
        category: 'TECHNICAL',
        signals: {
          technicalScore: 85,
          fundamentalScore: 72,
          sentimentScore: 78,
          momentumScore: 89
        },
        keyFactors: [
          'Volume surge (+45% above average)',
          'RSI breaking above 60',
          'Moving average golden cross',
          'Bullish price action pattern'
        ],
        timestamp: new Date(Date.now() - 300000),
        actionable: {
          recommendation: 'BUY',
          targetPrice: 2580,
          stopLoss: 2420,
          timeHorizon: '2-3 weeks'
        }
      },
      {
        id: 'AI-002',
        type: 'BEARISH',
        symbol: 'TCS',
        title: 'Earnings Disappointment Risk',
        description: 'AI models predict potential earnings miss based on sector headwinds and management commentary analysis. Consider defensive positioning.',
        confidence: 0.73,
        impact: 'MEDIUM',
        timeframe: '1W',
        category: 'FUNDAMENTAL',
        signals: {
          technicalScore: 45,
          fundamentalScore: 38,
          sentimentScore: 42,
          momentumScore: 35
        },
        keyFactors: [
          'IT sector margin pressure',
          'Client spending cuts in BFSI',
          'Currency headwinds',
          'Guidance revision probability: 68%'
        ],
        timestamp: new Date(Date.now() - 600000),
        actionable: {
          recommendation: 'HOLD',
          targetPrice: 3200,
          stopLoss: 3380,
          timeHorizon: '1-2 weeks'
        }
      },
      {
        id: 'AI-003',
        type: 'ALERT',
        symbol: 'NIFTY',
        title: 'Market Volatility Spike Expected',
        description: 'VIX patterns and option flow analysis suggest increased volatility over next 3-5 trading sessions. Adjust position sizing accordingly.',
        confidence: 0.91,
        impact: 'CRITICAL',
        timeframe: '1W',
        category: 'RISK',
        signals: {
          technicalScore: 65,
          fundamentalScore: 72,
          sentimentScore: 58,
          momentumScore: 61
        },
        keyFactors: [
          'VIX term structure inversion',
          'Unusual options activity',
          'FII selling pressure',
          'Global macro uncertainty'
        ],
        timestamp: new Date(Date.now() - 900000),
        actionable: {
          recommendation: 'WATCH',
          timeHorizon: '3-5 days'
        }
      },
      {
        id: 'AI-004',
        type: 'BULLISH',
        symbol: 'INFY',
        title: 'AI Transformation Catalyst',
        description: 'Strong positioning in AI services and digital transformation. Recent client wins and margin expansion suggest outperformance potential.',
        confidence: 0.79,
        impact: 'HIGH',
        timeframe: '3M',
        category: 'FUNDAMENTAL',
        signals: {
          technicalScore: 78,
          fundamentalScore: 92,
          sentimentScore: 85,
          momentumScore: 74
        },
        keyFactors: [
          'AI services revenue growth +28%',
          'Large deal pipeline strong',
          'Margin expansion trajectory',
          'Digital transformation demand'
        ],
        timestamp: new Date(Date.now() - 1200000),
        actionable: {
          recommendation: 'BUY',
          targetPrice: 1620,
          stopLoss: 1380,
          timeHorizon: '2-3 months'
        }
      }
    ];

    const mockRegime: MarketRegime = {
      id: 'REGIME-001',
      name: 'Cautious Optimism',
      type: 'BULL_MARKET',
      confidence: 0.72,
      duration: 45,
      characteristics: [
        'Selective stock picking environment',
        'Sector rotation active',
        'Earnings growth supporting prices',
        'Moderate volatility regime'
      ],
      sectors: {
        outperforming: ['Technology', 'Healthcare', 'Consumer Discretionary'],
        underperforming: ['Utilities', 'Real Estate', 'Energy']
      },
      strategies: [
        'Growth at reasonable price (GARP)',
        'Quality momentum',
        'Sector rotation',
        'Earnings revision strategies'
      ],
      riskLevel: 0.35
    };

    const mockTrends: Record<string, TrendAnalysis> = {
      RELIANCE: {
        symbol: 'RELIANCE',
        trend: {
          shortTerm: 'UP',
          mediumTerm: 'UP',
          longTerm: 'SIDEWAYS'
        },
        strength: {
          momentum: 78,
          volume: 65,
          breadth: 82
        },
        support: [2420, 2380, 2340],
        resistance: [2480, 2520, 2580],
        keyLevels: [
          { price: 2450, type: 'RESISTANCE', strength: 0.8 },
          { price: 2400, type: 'SUPPORT', strength: 0.9 },
          { price: 2380, type: 'SUPPORT', strength: 0.7 }
        ]
      },
      TCS: {
        symbol: 'TCS',
        trend: {
          shortTerm: 'DOWN',
          mediumTerm: 'SIDEWAYS',
          longTerm: 'UP'
        },
        strength: {
          momentum: 35,
          volume: 42,
          breadth: 38
        },
        support: [3180, 3150, 3100],
        resistance: [3280, 3320, 3380],
        keyLevels: [
          { price: 3250, type: 'RESISTANCE', strength: 0.75 },
          { price: 3200, type: 'PIVOT', strength: 0.85 },
          { price: 3180, type: 'SUPPORT', strength: 0.8 }
        ]
      }
    };

    const mockMacroIndicators: MacroIndicator[] = [
      {
        name: 'GDP Growth Rate',
        value: 6.8,
        change: 0.2,
        impact: 'POSITIVE',
        importance: 0.9,
        description: 'Quarterly GDP growth showing resilience',
        trend: [6.2, 6.4, 6.6, 6.8],
        nextUpdate: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000)
      },
      {
        name: 'Inflation Rate',
        value: 5.2,
        change: -0.3,
        impact: 'POSITIVE',
        importance: 0.85,
        description: 'CPI inflation cooling down, supportive for monetary policy',
        trend: [6.1, 5.8, 5.5, 5.2],
        nextUpdate: new Date(Date.now() + 10 * 24 * 60 * 60 * 1000)
      },
      {
        name: 'USD/INR',
        value: 83.15,
        change: 0.25,
        impact: 'NEGATIVE',
        importance: 0.7,
        description: 'Rupee under pressure, watch for FII flows',
        trend: [82.75, 82.90, 83.05, 83.15],
        nextUpdate: new Date()
      },
      {
        name: 'FII Net Flow',
        value: -1250,
        change: -850,
        impact: 'NEGATIVE',
        importance: 0.8,
        description: 'Foreign institutional investor outflows continue',
        trend: [500, -200, -400, -1250],
        nextUpdate: new Date()
      }
    ];

    setInsights(mockInsights);
    setMarketRegime(mockRegime);
    setTrendAnalysis(mockTrends);
    setMacroIndicators(mockMacroIndicators);
  };

  const refreshInsights = async () => {
    setIsRefreshing(true);
    // Simulate API call delay
    setTimeout(() => {
      // Update insights with new data (in real app, this would fetch from API)
      setInsights(prevInsights => 
        prevInsights.map(insight => ({
          ...insight,
          confidence: Math.max(0.1, Math.min(1.0, insight.confidence + (Math.random() - 0.5) * 0.1)),
          signals: {
            ...insight.signals,
            technicalScore: Math.max(0, Math.min(100, insight.signals.technicalScore + (Math.random() - 0.5) * 10)),
            sentimentScore: Math.max(0, Math.min(100, insight.signals.sentimentScore + (Math.random() - 0.5) * 8))
          }
        }))
      );
      setIsRefreshing(false);
    }, 1500);
  };

  const filteredInsights = useMemo(() => {
    let filtered = insights;
    
    if (filterCategory !== 'ALL') {
      filtered = filtered.filter(insight => insight.category === filterCategory);
    }
    
    return filtered.sort((a, b) => {
      // Sort by impact and confidence
      const impactOrder = { 'CRITICAL': 4, 'HIGH': 3, 'MEDIUM': 2, 'LOW': 1 };
      const impactDiff = impactOrder[b.impact] - impactOrder[a.impact];
      if (impactDiff !== 0) return impactDiff;
      
      return b.confidence - a.confidence;
    });
  }, [insights, filterCategory]);

  const getInsightTypeColor = (type: AIInsight['type']) => {
    switch (type) {
      case 'BULLISH': return 'bg-green-100 text-green-800 border-green-200';
      case 'BEARISH': return 'bg-red-100 text-red-800 border-red-200';
      case 'NEUTRAL': return 'bg-gray-100 text-gray-800 border-gray-200';
      case 'ALERT': return 'bg-orange-100 text-orange-800 border-orange-200';
      default: return 'bg-gray-100 text-gray-800 border-gray-200';
    }
  };

  const getImpactColor = (impact: AIInsight['impact']) => {
    switch (impact) {
      case 'CRITICAL': return 'bg-red-600 text-white';
      case 'HIGH': return 'bg-orange-600 text-white';
      case 'MEDIUM': return 'bg-yellow-600 text-white';
      case 'LOW': return 'bg-blue-600 text-white';
      default: return 'bg-gray-600 text-white';
    }
  };

  const getTrendColor = (trend: string) => {
    switch (trend) {
      case 'UP': return 'text-green-600';
      case 'DOWN': return 'text-red-600';
      case 'SIDEWAYS': return 'text-gray-600';
      default: return 'text-gray-600';
    }
  };

  const toggleInsightExpansion = (insightId: string) => {
    const newExpanded = new Set(expandedInsights);
    if (newExpanded.has(insightId)) {
      newExpanded.delete(insightId);
    } else {
      newExpanded.add(insightId);
    }
    setExpandedInsights(newExpanded);
  };

  return (
    <div className="p-6 space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Market Intelligence Hub</h1>
          <p className="text-gray-600">AI-powered market insights and analysis</p>
        </div>
        <div className="flex items-center space-x-3">
          <div className="flex items-center space-x-2 border border-gray-300 rounded-lg p-1">
            {['1H', '1D', '1W', '1M', '3M'].map((tf) => (
              <Button
                key={tf}
                size="sm"
                variant={timeframe === tf ? "default" : "ghost"}
                onClick={() => setTimeframe(tf as any)}
              >
                {tf}
              </Button>
            ))}
          </div>
          <Button
            variant="outline"
            onClick={refreshInsights}
            disabled={isRefreshing}
            className="flex items-center space-x-2"
          >
            <RefreshCw className={`h-4 w-4 ${isRefreshing ? 'animate-spin' : ''}`} />
            <span>Refresh</span>
          </Button>
          <Button variant="outline" className="flex items-center space-x-2">
            <Settings className="h-4 w-4" />
            <span>Settings</span>
          </Button>
        </div>
      </div>

      {/* Market Regime Overview */}
      {marketRegime && (
        <Card className="bg-gradient-to-r from-blue-50 to-indigo-50 border-blue-200">
          <CardHeader>
            <div className="flex items-center justify-between">
              <div className="flex items-center space-x-3">
                <Brain className="h-8 w-8 text-blue-600" />
                <div>
                  <CardTitle className="text-xl text-blue-900">Current Market Regime</CardTitle>
                  <p className="text-blue-700 text-sm">AI-detected market environment</p>
                </div>
              </div>
              <Badge className="bg-blue-600 text-white px-3 py-1">
                {(marketRegime.confidence * 100).toFixed(0)}% confidence
              </Badge>
            </div>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              <div>
                <h4 className="font-medium text-blue-900 mb-2">{marketRegime.name}</h4>
                <p className="text-sm text-blue-700 mb-3">Duration: {marketRegime.duration} days</p>
                <div className="space-y-1">
                  {marketRegime.characteristics.slice(0, 3).map((char, index) => (
                    <div key={index} className="flex items-center space-x-2 text-sm text-blue-700">
                      <ChevronRight className="h-3 w-3" />
                      <span>{char}</span>
                    </div>
                  ))}
                </div>
              </div>
              
              <div>
                <h4 className="font-medium text-blue-900 mb-2">Sector Performance</h4>
                <div className="space-y-2">
                  <div>
                    <p className="text-xs text-blue-600 mb-1">Outperforming</p>
                    <div className="flex flex-wrap gap-1">
                      {marketRegime.sectors.outperforming.slice(0, 3).map((sector, index) => (
                        <Badge key={index} variant="outline" className="text-xs border-green-300 text-green-700">
                          {sector}
                        </Badge>
                      ))}
                    </div>
                  </div>
                  <div>
                    <p className="text-xs text-blue-600 mb-1">Underperforming</p>
                    <div className="flex flex-wrap gap-1">
                      {marketRegime.sectors.underperforming.slice(0, 3).map((sector, index) => (
                        <Badge key={index} variant="outline" className="text-xs border-red-300 text-red-700">
                          {sector}
                        </Badge>
                      ))}
                    </div>
                  </div>
                </div>
              </div>
              
              <div>
                <h4 className="font-medium text-blue-900 mb-2">Recommended Strategies</h4>
                <div className="space-y-1">
                  {marketRegime.strategies.slice(0, 3).map((strategy, index) => (
                    <div key={index} className="flex items-center space-x-2 text-sm text-blue-700">
                      <Target className="h-3 w-3" />
                      <span>{strategy}</span>
                    </div>
                  ))}
                </div>
                <div className="mt-3">
                  <div className="flex items-center justify-between text-xs text-blue-600 mb-1">
                    <span>Risk Level</span>
                    <span>{(marketRegime.riskLevel * 100).toFixed(0)}%</span>
                  </div>
                  <div className="w-full bg-blue-200 rounded-full h-2">
                    <div
                      className="bg-blue-600 h-2 rounded-full"
                      style={{ width: `${marketRegime.riskLevel * 100}%` }}
                    />
                  </div>
                </div>
              </div>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Main Content */}
      <Tabs value={activeTab} onValueChange={(value) => setActiveTab(value as any)}>
        <TabsList className="grid w-full grid-cols-5">
          <TabsTrigger value="insights" className="flex items-center space-x-2">
            <Brain className="h-4 w-4" />
            <span>AI Insights</span>
          </TabsTrigger>
          <TabsTrigger value="trends" className="flex items-center space-x-2">
            <TrendingUp className="h-4 w-4" />
            <span>Trend Analysis</span>
          </TabsTrigger>
          <TabsTrigger value="regime" className="flex items-center space-x-2">
            <Network className="h-4 w-4" />
            <span>Market Regime</span>
          </TabsTrigger>
          <TabsTrigger value="macro" className="flex items-center space-x-2">
            <Globe className="h-4 w-4" />
            <span>Macro</span>
          </TabsTrigger>
          <TabsTrigger value="alerts" className="flex items-center space-x-2">
            <AlertTriangle className="h-4 w-4" />
            <span>Alerts</span>
          </TabsTrigger>
        </TabsList>

        <TabsContent value="insights" className="space-y-4">
          {/* Filters */}
          <Card>
            <CardContent className="p-4">
              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-4">
                  <span className="text-sm font-medium text-gray-700">Filter by:</span>
                  <select
                    value={filterCategory}
                    onChange={(e) => setFilterCategory(e.target.value)}
                    className="px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                  >
                    <option value="ALL">All Categories</option>
                    <option value="TECHNICAL">Technical</option>
                    <option value="FUNDAMENTAL">Fundamental</option>
                    <option value="SENTIMENT">Sentiment</option>
                    <option value="NEWS">News</option>
                    <option value="FLOW">Flow</option>
                    <option value="RISK">Risk</option>
                  </select>
                </div>
                <div className="text-sm text-gray-600">
                  {filteredInsights.length} insights found
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Insights List */}
          <div className="space-y-4">
            {filteredInsights.map((insight) => (
              <Card 
                key={insight.id} 
                className="hover:shadow-lg transition-shadow cursor-pointer"
                onClick={() => onInsightClick?.(insight)}
              >
                <CardHeader>
                  <div className="flex items-center justify-between">
                    <div className="flex items-center space-x-4">
                      <Badge className={getInsightTypeColor(insight.type)}>
                        {insight.type}
                      </Badge>
                      <Badge variant="outline">{insight.symbol}</Badge>
                      <Badge className={getImpactColor(insight.impact)}>
                        {insight.impact}
                      </Badge>
                    </div>
                    <div className="flex items-center space-x-2">
                      <div className="text-sm text-gray-600">
                        Confidence: {(insight.confidence * 100).toFixed(0)}%
                      </div>
                      <Button
                        size="sm"
                        variant="ghost"
                        onClick={(e) => {
                          e.stopPropagation();
                          toggleInsightExpansion(insight.id);
                        }}
                      >
                        {expandedInsights.has(insight.id) ? 
                          <ChevronDown className="h-4 w-4" /> : 
                          <ChevronRight className="h-4 w-4" />
                        }
                      </Button>
                    </div>
                  </div>
                  <div>
                    <CardTitle className="text-lg">{insight.title}</CardTitle>
                    <p className="text-gray-600 text-sm mt-1">{insight.description}</p>
                  </div>
                </CardHeader>

                <CardContent>
                  <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-4">
                    <div className="text-center">
                      <div className="text-2xl font-bold text-blue-600">{insight.signals.technicalScore}</div>
                      <div className="text-xs text-gray-600">Technical</div>
                    </div>
                    <div className="text-center">
                      <div className="text-2xl font-bold text-green-600">{insight.signals.fundamentalScore}</div>
                      <div className="text-xs text-gray-600">Fundamental</div>
                    </div>
                    <div className="text-center">
                      <div className="text-2xl font-bold text-purple-600">{insight.signals.sentimentScore}</div>
                      <div className="text-xs text-gray-600">Sentiment</div>
                    </div>
                    <div className="text-center">
                      <div className="text-2xl font-bold text-orange-600">{insight.signals.momentumScore}</div>
                      <div className="text-xs text-gray-600">Momentum</div>
                    </div>
                  </div>

                  <AnimatePresence>
                    {expandedInsights.has(insight.id) && (
                      <motion.div
                        initial={{ opacity: 0, height: 0 }}
                        animate={{ opacity: 1, height: 'auto' }}
                        exit={{ opacity: 0, height: 0 }}
                        className="border-t pt-4"
                      >
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                          <div>
                            <h4 className="font-medium text-gray-900 mb-2">Key Factors</h4>
                            <ul className="space-y-1">
                              {insight.keyFactors.map((factor, index) => (
                                <li key={index} className="flex items-start space-x-2 text-sm">
                                  <ChevronRight className="h-3 w-3 mt-0.5 text-gray-400" />
                                  <span>{factor}</span>
                                </li>
                              ))}
                            </ul>
                          </div>
                          
                          <div>
                            <h4 className="font-medium text-gray-900 mb-2">Actionable Recommendation</h4>
                            <div className="bg-gray-50 p-3 rounded-lg">
                              <div className="flex items-center space-x-2 mb-2">
                                <Badge className={
                                  insight.actionable.recommendation === 'BUY' ? 'bg-green-600 text-white' :
                                  insight.actionable.recommendation === 'SELL' ? 'bg-red-600 text-white' :
                                  insight.actionable.recommendation === 'HOLD' ? 'bg-yellow-600 text-white' :
                                  'bg-blue-600 text-white'
                                }>
                                  {insight.actionable.recommendation}
                                </Badge>
                                {insight.actionable.timeHorizon && (
                                  <span className="text-sm text-gray-600">{insight.actionable.timeHorizon}</span>
                                )}
                              </div>
                              {insight.actionable.targetPrice && (
                                <div className="text-sm space-y-1">
                                  <div>Target: ₹{insight.actionable.targetPrice}</div>
                                  {insight.actionable.stopLoss && (
                                    <div>Stop Loss: ₹{insight.actionable.stopLoss}</div>
                                  )}
                                </div>
                              )}
                            </div>
                          </div>
                        </div>
                      </motion.div>
                    )}
                  </AnimatePresence>

                  <div className="flex items-center justify-between mt-4 pt-4 border-t">
                    <div className="flex items-center space-x-4 text-sm text-gray-600">
                      <span>{insight.category}</span>
                      <span>•</span>
                      <span>{insight.timeframe}</span>
                      <span>•</span>
                      <span>{new Date(insight.timestamp).toLocaleTimeString()}</span>
                    </div>
                    <div className="flex items-center space-x-2">
                      <Button size="sm" variant="ghost">
                        <Share2 className="h-4 w-4" />
                      </Button>
                      <Button size="sm" variant="ghost">
                        <BookOpen className="h-4 w-4" />
                      </Button>
                    </div>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        </TabsContent>

        <TabsContent value="trends" className="space-y-4">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {Object.entries(trendAnalysis).map(([symbol, analysis]) => (
              <Card key={symbol}>
                <CardHeader>
                  <CardTitle className="flex items-center space-x-2">
                    <TrendingUp className="h-5 w-5" />
                    <span>{analysis.symbol} Trend Analysis</span>
                  </CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="space-y-4">
                    <div className="grid grid-cols-3 gap-4">
                      <div className="text-center">
                        <div className="text-sm text-gray-600 mb-1">Short Term</div>
                        <div className={`font-medium ${getTrendColor(analysis.trend.shortTerm)}`}>
                          {analysis.trend.shortTerm}
                        </div>
                      </div>
                      <div className="text-center">
                        <div className="text-sm text-gray-600 mb-1">Medium Term</div>
                        <div className={`font-medium ${getTrendColor(analysis.trend.mediumTerm)}`}>
                          {analysis.trend.mediumTerm}
                        </div>
                      </div>
                      <div className="text-center">
                        <div className="text-sm text-gray-600 mb-1">Long Term</div>
                        <div className={`font-medium ${getTrendColor(analysis.trend.longTerm)}`}>
                          {analysis.trend.longTerm}
                        </div>
                      </div>
                    </div>

                    <div className="grid grid-cols-3 gap-4">
                      <div>
                        <div className="text-sm text-gray-600 mb-1">Momentum</div>
                        <div className="w-full bg-gray-200 rounded-full h-2">
                          <div
                            className="bg-blue-600 h-2 rounded-full"
                            style={{ width: `${analysis.strength.momentum}%` }}
                          />
                        </div>
                        <div className="text-xs text-gray-600 mt-1">{analysis.strength.momentum}%</div>
                      </div>
                      <div>
                        <div className="text-sm text-gray-600 mb-1">Volume</div>
                        <div className="w-full bg-gray-200 rounded-full h-2">
                          <div
                            className="bg-green-600 h-2 rounded-full"
                            style={{ width: `${analysis.strength.volume}%` }}
                          />
                        </div>
                        <div className="text-xs text-gray-600 mt-1">{analysis.strength.volume}%</div>
                      </div>
                      <div>
                        <div className="text-sm text-gray-600 mb-1">Breadth</div>
                        <div className="w-full bg-gray-200 rounded-full h-2">
                          <div
                            className="bg-purple-600 h-2 rounded-full"
                            style={{ width: `${analysis.strength.breadth}%` }}
                          />
                        </div>
                        <div className="text-xs text-gray-600 mt-1">{analysis.strength.breadth}%</div>
                      </div>
                    </div>

                    <div className="grid grid-cols-2 gap-4">
                      <div>
                        <h4 className="font-medium text-green-700 mb-2">Support Levels</h4>
                        <div className="space-y-1">
                          {analysis.support.map((level, index) => (
                            <div key={index} className="text-sm">₹{level.toFixed(2)}</div>
                          ))}
                        </div>
                      </div>
                      <div>
                        <h4 className="font-medium text-red-700 mb-2">Resistance Levels</h4>
                        <div className="space-y-1">
                          {analysis.resistance.map((level, index) => (
                            <div key={index} className="text-sm">₹{level.toFixed(2)}</div>
                          ))}
                        </div>
                      </div>
                    </div>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        </TabsContent>

        <TabsContent value="macro" className="space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {macroIndicators.map((indicator, index) => (
              <Card key={index}>
                <CardHeader>
                  <div className="flex items-center justify-between">
                    <CardTitle className="text-lg">{indicator.name}</CardTitle>
                    <Badge className={
                      indicator.impact === 'POSITIVE' ? 'bg-green-100 text-green-800' :
                      indicator.impact === 'NEGATIVE' ? 'bg-red-100 text-red-800' :
                      'bg-gray-100 text-gray-800'
                    }>
                      {indicator.impact}
                    </Badge>
                  </div>
                </CardHeader>
                <CardContent>
                  <div className="space-y-4">
                    <div className="flex items-center justify-between">
                      <div className="text-3xl font-bold">{indicator.value}%</div>
                      <div className={`flex items-center space-x-1 ${
                        indicator.change > 0 ? 'text-green-600' : indicator.change < 0 ? 'text-red-600' : 'text-gray-600'
                      }`}>
                        {indicator.change > 0 ? <TrendingUp className="h-4 w-4" /> : 
                         indicator.change < 0 ? <TrendingDown className="h-4 w-4" /> : 
                         <Minus className="h-4 w-4" />}
                        <span className="font-medium">
                          {indicator.change > 0 ? '+' : ''}{indicator.change}%
                        </span>
                      </div>
                    </div>

                    <p className="text-sm text-gray-600">{indicator.description}</p>

                    <div>
                      <div className="flex items-center justify-between text-xs text-gray-600 mb-1">
                        <span>Market Importance</span>
                        <span>{(indicator.importance * 100).toFixed(0)}%</span>
                      </div>
                      <div className="w-full bg-gray-200 rounded-full h-2">
                        <div
                          className="bg-blue-600 h-2 rounded-full"
                          style={{ width: `${indicator.importance * 100}%` }}
                        />
                      </div>
                    </div>

                    <div className="text-xs text-gray-500">
                      Next update: {indicator.nextUpdate.toLocaleDateString()}
                    </div>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        </TabsContent>

        <TabsContent value="alerts" className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center space-x-2">
                <AlertTriangle className="h-5 w-5 text-orange-600" />
                <span>Active Market Alerts</span>
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                {insights.filter(insight => insight.type === 'ALERT').map((alert) => (
                  <div key={alert.id} className="flex items-start space-x-4 p-4 border border-orange-200 bg-orange-50 rounded-lg">
                    <AlertTriangle className="h-5 w-5 text-orange-600 mt-0.5" />
                    <div className="flex-1">
                      <div className="flex items-center space-x-2 mb-2">
                        <h4 className="font-medium text-orange-900">{alert.title}</h4>
                        <Badge variant="outline">{alert.symbol}</Badge>
                      </div>
                      <p className="text-sm text-orange-800 mb-2">{alert.description}</p>
                      <div className="flex items-center space-x-4 text-xs text-orange-700">
                        <span>Confidence: {(alert.confidence * 100).toFixed(0)}%</span>
                        <span>•</span>
                        <span>{alert.timeframe}</span>
                        <span>•</span>
                        <span>{new Date(alert.timestamp).toLocaleTimeString()}</span>
                      </div>
                    </div>
                  </div>
                ))}

                <div className="text-center py-8 text-gray-500">
                  <Shield className="h-12 w-12 mx-auto mb-4 text-gray-400" />
                  <p>No additional alerts at this time</p>
                  <p className="text-sm">Our AI is continuously monitoring for new opportunities and risks</p>
                </div>
              </div>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  );
};

export default MarketIntelligenceHub;