import React, { useState, useEffect, useCallback } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { 
  CheckCircle2, 
  XCircle, 
  AlertTriangle, 
  PlayCircle,
  RefreshCw,
  Bug,
  Zap,
  Shield,
  Target,
  Clock,
  Activity,
  TrendingUp,
  Settings,
  FileText,
  Users,
  Smartphone,
  Monitor,
  Keyboard,
  Eye,
  AlertCircle,
  ThumbsUp,
  ThumbsDown
} from 'lucide-react';

interface TestCase {
  id: string;
  category: 'functionality' | 'performance' | 'security' | 'usability' | 'compatibility';
  name: string;
  description: string;
  priority: 'high' | 'medium' | 'low';
  status: 'pending' | 'running' | 'passed' | 'failed' | 'warning';
  duration?: number;
  error?: string;
  details?: any;
  automated: boolean;
}

interface TestSuite {
  id: string;
  name: string;
  category: string;
  tests: TestCase[];
  progress: number;
  status: 'pending' | 'running' | 'completed' | 'failed';
}

interface ValidationReport {
  totalTests: number;
  passedTests: number;
  failedTests: number;
  warningTests: number;
  coverage: number;
  overallScore: number;
  recommendations: string[];
}

const TradingFeatureValidator: React.FC = () => {
  const [testSuites, setTestSuites] = useState<TestSuite[]>([]);
  const [activeCategory, setActiveCategory] = useState<string>('all');
  const [runningTests, setRunningTests] = useState<Set<string>>(new Set());
  const [validationReport, setValidationReport] = useState<ValidationReport | null>(null);
  const [showDetails, setShowDetails] = useState<string | null>(null);
  const [autoRun, setAutoRun] = useState(false);
  const [filterStatus, setFilterStatus] = useState<string>('all');

  // Initialize test suites
  useEffect(() => {
    const suites: TestSuite[] = [
      {
        id: 'order-management',
        name: 'Advanced Order Management',
        category: 'functionality',
        status: 'pending',
        progress: 0,
        tests: [
          {
            id: 'order-creation',
            category: 'functionality',
            name: 'Order Creation',
            description: 'Test creation of various order types (Market, Limit, Stop, OCO, Bracket)',
            priority: 'high',
            status: 'pending',
            automated: true
          },
          {
            id: 'order-validation',
            category: 'functionality',
            name: 'Order Validation',
            description: 'Validate order parameters, risk limits, and business rules',
            priority: 'high',
            status: 'pending',
            automated: true
          },
          {
            id: 'order-modification',
            category: 'functionality',
            name: 'Order Modification',
            description: 'Test order updates, cancellation, and replacement',
            priority: 'medium',
            status: 'pending',
            automated: true
          },
          {
            id: 'order-templates',
            category: 'usability',
            name: 'Order Templates',
            description: 'Test saving, loading, and managing order templates',
            priority: 'medium',
            status: 'pending',
            automated: false
          }
        ]
      },
      {
        id: 'trading-tools',
        name: 'Professional Trading Tools',
        category: 'functionality',
        status: 'pending',
        progress: 0,
        tests: [
          {
            id: 'position-calculator',
            category: 'functionality',
            name: 'Position Size Calculator',
            description: 'Test position sizing calculations with risk management',
            priority: 'high',
            status: 'pending',
            automated: true
          },
          {
            id: 'risk-analyzer',
            category: 'functionality',
            name: 'Risk Analyzer',
            description: 'Test portfolio risk metrics (VaR, Beta, correlation)',
            priority: 'high',
            status: 'pending',
            automated: true
          },
          {
            id: 'strategy-backtester',
            category: 'performance',
            name: 'Strategy Backtester',
            description: 'Test backtesting engine with historical data',
            priority: 'medium',
            status: 'pending',
            automated: true
          }
        ]
      },
      {
        id: 'multi-asset-trading',
        name: 'Multi-Asset Trading Interface',
        category: 'functionality',
        status: 'pending',
        progress: 0,
        tests: [
          {
            id: 'asset-switching',
            category: 'functionality',
            name: 'Asset Type Switching',
            description: 'Test switching between equity, options, crypto, futures, forex',
            priority: 'high',
            status: 'pending',
            automated: true
          },
          {
            id: 'options-chain',
            category: 'functionality',
            name: 'Options Chain Display',
            description: 'Test options chain rendering and Greeks calculations',
            priority: 'high',
            status: 'pending',
            automated: true
          },
          {
            id: 'crypto-trading',
            category: 'functionality',
            name: 'Crypto Trading Features',
            description: 'Test crypto-specific features like pair selection and fees',
            priority: 'medium',
            status: 'pending',
            automated: true
          }
        ]
      },
      {
        id: 'workspace-customization',
        name: 'Customizable Workspace',
        category: 'usability',
        status: 'pending',
        progress: 0,
        tests: [
          {
            id: 'drag-drop',
            category: 'usability',
            name: 'Drag & Drop Functionality',
            description: 'Test widget repositioning and resizing',
            priority: 'medium',
            status: 'pending',
            automated: false
          },
          {
            id: 'layout-saving',
            category: 'functionality',
            name: 'Layout Persistence',
            description: 'Test saving and loading workspace layouts',
            priority: 'medium',
            status: 'pending',
            automated: true
          },
          {
            id: 'theme-switching',
            category: 'usability',
            name: 'Theme Customization',
            description: 'Test theme switching and color customization',
            priority: 'low',
            status: 'pending',
            automated: false
          }
        ]
      },
      {
        id: 'hotkeys',
        name: 'Trading Hotkeys',
        category: 'usability',
        status: 'pending',
        progress: 0,
        tests: [
          {
            id: 'hotkey-registration',
            category: 'functionality',
            name: 'Hotkey Registration',
            description: 'Test hotkey binding and event handling',
            priority: 'high',
            status: 'pending',
            automated: true
          },
          {
            id: 'quick-trading',
            category: 'performance',
            name: 'Quick Order Execution',
            description: 'Test rapid order placement via hotkeys',
            priority: 'high',
            status: 'pending',
            automated: false
          },
          {
            id: 'hotkey-conflicts',
            category: 'security',
            name: 'Hotkey Conflict Resolution',
            description: 'Test handling of conflicting hotkey combinations',
            priority: 'medium',
            status: 'pending',
            automated: true
          }
        ]
      },
      {
        id: 'mobile-trading',
        name: 'Mobile Trading Interface',
        category: 'compatibility',
        status: 'pending',
        progress: 0,
        tests: [
          {
            id: 'responsive-design',
            category: 'compatibility',
            name: 'Responsive Design',
            description: 'Test interface adaptation across screen sizes',
            priority: 'high',
            status: 'pending',
            automated: false
          },
          {
            id: 'touch-gestures',
            category: 'usability',
            name: 'Touch Gesture Support',
            description: 'Test swipe, tap, and pinch gestures',
            priority: 'medium',
            status: 'pending',
            automated: false
          },
          {
            id: 'mobile-performance',
            category: 'performance',
            name: 'Mobile Performance',
            description: 'Test loading times and resource usage on mobile',
            priority: 'high',
            status: 'pending',
            automated: true
          }
        ]
      },
      {
        id: 'security',
        name: 'Security & Risk Management',
        category: 'security',
        status: 'pending',
        progress: 0,
        tests: [
          {
            id: 'order-confirmation',
            category: 'security',
            name: 'High-Risk Order Confirmation',
            description: 'Test confirmation dialogs for high-risk orders',
            priority: 'high',
            status: 'pending',
            automated: false
          },
          {
            id: 'position-limits',
            category: 'security',
            name: 'Position Limit Enforcement',
            description: 'Test enforcement of position and risk limits',
            priority: 'high',
            status: 'pending',
            automated: true
          },
          {
            id: 'emergency-stop',
            category: 'security',
            name: 'Emergency Stop Functionality',
            description: 'Test emergency stop and panic close features',
            priority: 'high',
            status: 'pending',
            automated: false
          }
        ]
      }
    ];
    
    setTestSuites(suites);
  }, []);

  // Mock test execution
  const executeTest = useCallback(async (testId: string): Promise<{ status: 'passed' | 'failed' | 'warning'; duration: number; error?: string; details?: any }> => {
    const mockDelay = 1000 + Math.random() * 2000; // 1-3 seconds
    
    return new Promise((resolve) => {
      setTimeout(() => {
        const random = Math.random();
        
        // Mock test results with weighted probabilities
        if (random < 0.8) { // 80% pass rate
          resolve({
            status: 'passed',
            duration: mockDelay,
            details: {
              assertions: Math.floor(Math.random() * 10) + 5,
              coverage: Math.floor(Math.random() * 30) + 70
            }
          });
        } else if (random < 0.95) { // 15% warning rate
          resolve({
            status: 'warning',
            duration: mockDelay,
            error: 'Performance threshold exceeded',
            details: {
              assertions: Math.floor(Math.random() * 8) + 3,
              warnings: ['Slow response time', 'High memory usage']
            }
          });
        } else { // 5% failure rate
          resolve({
            status: 'failed',
            duration: mockDelay,
            error: 'Assertion failed: Expected value to be truthy',
            details: {
              assertions: Math.floor(Math.random() * 5) + 1,
              failedAssertion: 'element.isVisible() === true'
            }
          });
        }
      }, mockDelay);
    });
  }, []);

  const runTest = useCallback(async (suiteId: string, testId: string) => {
    setRunningTests(prev => new Set(prev).add(testId));
    
    // Update test status to running
    setTestSuites(prev => prev.map(suite => 
      suite.id === suiteId 
        ? {
            ...suite,
            tests: suite.tests.map(test => 
              test.id === testId ? { ...test, status: 'running' } : test
            )
          }
        : suite
    ));
    
    try {
      const result = await executeTest(testId);
      
      // Update test with results
      setTestSuites(prev => prev.map(suite => 
        suite.id === suiteId 
          ? {
              ...suite,
              tests: suite.tests.map(test => 
                test.id === testId 
                  ? { 
                      ...test, 
                      status: result.status,
                      duration: result.duration,
                      error: result.error,
                      details: result.details
                    }
                  : test
              )
            }
          : suite
      ));
      
    } catch (error) {
      // Handle test execution error
      setTestSuites(prev => prev.map(suite => 
        suite.id === suiteId 
          ? {
              ...suite,
              tests: suite.tests.map(test => 
                test.id === testId 
                  ? { ...test, status: 'failed', error: 'Test execution failed' }
                  : test
              )
            }
          : suite
      ));
    } finally {
      setRunningTests(prev => {
        const newSet = new Set(prev);
        newSet.delete(testId);
        return newSet;
      });
    }
  }, [executeTest]);

  const runSuite = useCallback(async (suiteId: string) => {
    const suite = testSuites.find(s => s.id === suiteId);
    if (!suite) return;
    
    setTestSuites(prev => prev.map(s => 
      s.id === suiteId ? { ...s, status: 'running', progress: 0 } : s
    ));
    
    for (let i = 0; i < suite.tests.length; i++) {
      const test = suite.tests[i];
      await runTest(suiteId, test.id);
      
      // Update progress
      setTestSuites(prev => prev.map(s => 
        s.id === suiteId 
          ? { ...s, progress: ((i + 1) / suite.tests.length) * 100 }
          : s
      ));
    }
    
    setTestSuites(prev => prev.map(s => 
      s.id === suiteId ? { ...s, status: 'completed' } : s
    ));
  }, [testSuites, runTest]);

  const runAllTests = useCallback(async () => {
    for (const suite of testSuites) {
      await runSuite(suite.id);
    }
    generateReport();
  }, [testSuites, runSuite]);

  const generateReport = useCallback(() => {
    const allTests = testSuites.flatMap(suite => suite.tests);
    const totalTests = allTests.length;
    const passedTests = allTests.filter(test => test.status === 'passed').length;
    const failedTests = allTests.filter(test => test.status === 'failed').length;
    const warningTests = allTests.filter(test => test.status === 'warning').length;
    
    const coverage = totalTests > 0 ? (passedTests + warningTests) / totalTests * 100 : 0;
    const overallScore = totalTests > 0 ? passedTests / totalTests * 100 : 0;
    
    const recommendations: string[] = [];
    if (failedTests > 0) {
      recommendations.push(`Address ${failedTests} failed test${failedTests > 1 ? 's' : ''}`);
    }
    if (warningTests > 0) {
      recommendations.push(`Review ${warningTests} test${warningTests > 1 ? 's' : ''} with warnings`);
    }
    if (coverage < 90) {
      recommendations.push('Increase test coverage to meet 90% target');
    }
    if (overallScore >= 95) {
      recommendations.push('Excellent! All systems operating within specifications');
    }
    
    const report: ValidationReport = {
      totalTests,
      passedTests,
      failedTests,
      warningTests,
      coverage,
      overallScore,
      recommendations
    };
    
    setValidationReport(report);
  }, [testSuites]);

  // Auto-run tests when enabled
  useEffect(() => {
    if (autoRun && testSuites.length > 0) {
      const interval = setInterval(() => {
        const pendingTests = testSuites.flatMap(suite => 
          suite.tests.filter(test => test.status === 'pending')
        );
        if (pendingTests.length > 0) {
          const randomTest = pendingTests[Math.floor(Math.random() * pendingTests.length)];
          const suite = testSuites.find(s => s.tests.some(t => t.id === randomTest.id));
          if (suite) {
            runTest(suite.id, randomTest.id);
          }
        }
      }, 3000);
      
      return () => clearInterval(interval);
    }
  }, [autoRun, testSuites, runTest]);

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'passed': return <CheckCircle2 className="text-green-400" size={20} />;
      case 'failed': return <XCircle className="text-red-400" size={20} />;
      case 'warning': return <AlertTriangle className="text-yellow-400" size={20} />;
      case 'running': return <RefreshCw className="text-blue-400 animate-spin" size={20} />;
      default: return <Clock className="text-gray-400" size={20} />;
    }
  };

  const getCategoryIcon = (category: string) => {
    const icons = {
      functionality: <Zap className="text-blue-400" size={16} />,
      performance: <TrendingUp className="text-green-400" size={16} />,
      security: <Shield className="text-red-400" size={16} />,
      usability: <Users className="text-purple-400" size={16} />,
      compatibility: <Smartphone className="text-orange-400" size={16} />
    };
    return icons[category as keyof typeof icons] || <Activity className="text-gray-400" size={16} />;
  };

  const filteredSuites = testSuites.filter(suite => {
    if (activeCategory !== 'all' && suite.category !== activeCategory) return false;
    if (filterStatus !== 'all') {
      const hasMatchingTests = suite.tests.some(test => 
        filterStatus === 'all' || test.status === filterStatus
      );
      if (!hasMatchingTests) return false;
    }
    return true;
  });

  return (
    <div className="h-full bg-gradient-to-br from-gray-900 to-black text-white p-6 overflow-auto">
      <div className="max-w-6xl mx-auto">
        {/* Header */}
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center space-x-3">
            <Bug size={32} className="text-blue-400" />
            <div>
              <h1 className="text-2xl font-bold">Trading Feature Validator</h1>
              <p className="text-gray-400">Comprehensive testing and validation dashboard</p>
            </div>
          </div>
          
          <div className="flex items-center space-x-3">
            <div className="flex items-center space-x-2">
              <input
                type="checkbox"
                id="auto-run"
                checked={autoRun}
                onChange={(e) => setAutoRun(e.target.checked)}
                className="rounded"
              />
              <label htmlFor="auto-run" className="text-sm">Auto-run tests</label>
            </div>
            
            <button
              onClick={runAllTests}
              disabled={runningTests.size > 0}
              className="flex items-center space-x-2 px-4 py-2 bg-green-600 hover:bg-green-700 
                disabled:bg-gray-600 disabled:cursor-not-allowed rounded-lg transition-colors"
            >
              <PlayCircle size={16} />
              <span>Run All Tests</span>
            </button>
          </div>
        </div>

        {/* Validation Report */}
        {validationReport && (
          <motion.div
            className="bg-gray-800/50 rounded-xl p-6 mb-6 border border-gray-700"
            initial={{ opacity: 0, y: -20 }}
            animate={{ opacity: 1, y: 0 }}
          >
            <h2 className="text-xl font-bold mb-4">Validation Report</h2>
            
            <div className="grid grid-cols-4 gap-4 mb-4">
              <div className="text-center">
                <div className="text-2xl font-bold text-blue-400">{validationReport.totalTests}</div>
                <div className="text-sm text-gray-400">Total Tests</div>
              </div>
              <div className="text-center">
                <div className="text-2xl font-bold text-green-400">{validationReport.passedTests}</div>
                <div className="text-sm text-gray-400">Passed</div>
              </div>
              <div className="text-center">
                <div className="text-2xl font-bold text-red-400">{validationReport.failedTests}</div>
                <div className="text-sm text-gray-400">Failed</div>
              </div>
              <div className="text-center">
                <div className="text-2xl font-bold text-yellow-400">{validationReport.warningTests}</div>
                <div className="text-sm text-gray-400">Warnings</div>
              </div>
            </div>
            
            <div className="flex items-center justify-between mb-4">
              <div>
                <div className="flex items-center space-x-2 mb-1">
                  <span className="text-sm text-gray-400">Coverage:</span>
                  <span className="font-bold">{validationReport.coverage.toFixed(1)}%</span>
                </div>
                <div className="w-64 bg-gray-700 rounded-full h-2">
                  <div 
                    className="bg-blue-400 h-2 rounded-full transition-all duration-300"
                    style={{ width: `${validationReport.coverage}%` }}
                  ></div>
                </div>
              </div>
              
              <div className="text-right">
                <div className="text-2xl font-bold">
                  <span className={validationReport.overallScore >= 90 ? 'text-green-400' : 
                                 validationReport.overallScore >= 70 ? 'text-yellow-400' : 'text-red-400'}>
                    {validationReport.overallScore.toFixed(0)}%
                  </span>
                </div>
                <div className="text-sm text-gray-400">Overall Score</div>
              </div>
            </div>
            
            {validationReport.recommendations.length > 0 && (
              <div>
                <h4 className="font-medium mb-2">Recommendations:</h4>
                <ul className="space-y-1">
                  {validationReport.recommendations.map((rec, index) => (
                    <li key={index} className="flex items-center space-x-2 text-sm">
                      <div className="w-1 h-1 bg-blue-400 rounded-full"></div>
                      <span>{rec}</span>
                    </li>
                  ))}
                </ul>
              </div>
            )}
          </motion.div>
        )}

        {/* Filters */}
        <div className="flex items-center space-x-4 mb-6">
          <div className="flex items-center space-x-2">
            <span className="text-sm text-gray-400">Category:</span>
            <select
              value={activeCategory}
              onChange={(e) => setActiveCategory(e.target.value)}
              className="bg-gray-800 border border-gray-600 rounded px-3 py-1 text-sm"
            >
              <option value="all">All Categories</option>
              <option value="functionality">Functionality</option>
              <option value="performance">Performance</option>
              <option value="security">Security</option>
              <option value="usability">Usability</option>
              <option value="compatibility">Compatibility</option>
            </select>
          </div>
          
          <div className="flex items-center space-x-2">
            <span className="text-sm text-gray-400">Status:</span>
            <select
              value={filterStatus}
              onChange={(e) => setFilterStatus(e.target.value)}
              className="bg-gray-800 border border-gray-600 rounded px-3 py-1 text-sm"
            >
              <option value="all">All Status</option>
              <option value="pending">Pending</option>
              <option value="running">Running</option>
              <option value="passed">Passed</option>
              <option value="failed">Failed</option>
              <option value="warning">Warning</option>
            </select>
          </div>
        </div>

        {/* Test Suites */}
        <div className="space-y-4">
          {filteredSuites.map((suite) => (
            <motion.div
              key={suite.id}
              className="bg-gray-800/30 rounded-xl border border-gray-700 overflow-hidden"
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
            >
              <div className="p-4 border-b border-gray-700">
                <div className="flex items-center justify-between">
                  <div className="flex items-center space-x-3">
                    {getCategoryIcon(suite.category)}
                    <div>
                      <h3 className="font-semibold text-lg">{suite.name}</h3>
                      <p className="text-sm text-gray-400 capitalize">{suite.category}</p>
                    </div>
                  </div>
                  
                  <div className="flex items-center space-x-3">
                    {suite.status === 'running' && (
                      <div className="flex items-center space-x-2">
                        <div className="w-32 bg-gray-700 rounded-full h-2">
                          <div 
                            className="bg-blue-400 h-2 rounded-full transition-all duration-300"
                            style={{ width: `${suite.progress}%` }}
                          ></div>
                        </div>
                        <span className="text-sm">{suite.progress.toFixed(0)}%</span>
                      </div>
                    )}
                    
                    <button
                      onClick={() => runSuite(suite.id)}
                      disabled={suite.status === 'running'}
                      className="flex items-center space-x-2 px-3 py-1.5 bg-blue-600 hover:bg-blue-700 
                        disabled:bg-gray-600 disabled:cursor-not-allowed rounded text-sm transition-colors"
                    >
                      <PlayCircle size={14} />
                      <span>Run Suite</span>
                    </button>
                  </div>
                </div>
              </div>
              
              <div className="p-4">
                <div className="space-y-2">
                  {suite.tests.map((test) => (
                    <div
                      key={test.id}
                      className="flex items-center justify-between p-3 bg-gray-700/30 rounded-lg"
                    >
                      <div className="flex items-center space-x-3">
                        {getStatusIcon(test.status)}
                        <div>
                          <div className="flex items-center space-x-2">
                            <span className="font-medium">{test.name}</span>
                            <span className={`px-2 py-1 text-xs rounded ${
                              test.priority === 'high' ? 'bg-red-900 text-red-300' :
                              test.priority === 'medium' ? 'bg-yellow-900 text-yellow-300' :
                              'bg-green-900 text-green-300'
                            }`}>
                              {test.priority}
                            </span>
                            {test.automated && (
                              <span className="px-2 py-1 text-xs bg-blue-900 text-blue-300 rounded">
                                Automated
                              </span>
                            )}
                          </div>
                          <p className="text-sm text-gray-400">{test.description}</p>
                          {test.error && (
                            <p className="text-sm text-red-400 mt-1">{test.error}</p>
                          )}
                        </div>
                      </div>
                      
                      <div className="flex items-center space-x-2">
                        {test.duration && (
                          <span className="text-sm text-gray-400">
                            {(test.duration / 1000).toFixed(1)}s
                          </span>
                        )}
                        
                        <button
                          onClick={() => setShowDetails(showDetails === test.id ? null : test.id)}
                          className="p-1 hover:bg-gray-600 rounded"
                        >
                          <Eye size={14} />
                        </button>
                        
                        <button
                          onClick={() => runTest(suite.id, test.id)}
                          disabled={runningTests.has(test.id)}
                          className="p-1 hover:bg-gray-600 rounded disabled:cursor-not-allowed"
                        >
                          <PlayCircle size={14} />
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </motion.div>
          ))}
        </div>

        {/* Test Details Modal */}
        <AnimatePresence>
          {showDetails && (
            <motion.div
              className="fixed inset-0 z-50 bg-black/50 flex items-center justify-center"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              onClick={() => setShowDetails(null)}
            >
              <motion.div
                className="bg-gray-900 rounded-lg p-6 max-w-2xl w-full mx-4 border border-gray-700"
                initial={{ scale: 0.9 }}
                animate={{ scale: 1 }}
                exit={{ scale: 0.9 }}
                onClick={(e) => e.stopPropagation()}
              >
                {(() => {
                  const test = testSuites
                    .flatMap(suite => suite.tests)
                    .find(t => t.id === showDetails);
                  
                  if (!test) return null;
                  
                  return (
                    <>
                      <div className="flex items-center justify-between mb-4">
                        <h3 className="text-lg font-semibold">{test.name}</h3>
                        <button
                          onClick={() => setShowDetails(null)}
                          className="p-2 hover:bg-gray-800 rounded"
                        >
                          <X size={16} />
                        </button>
                      </div>
                      
                      <div className="space-y-4">
                        <div>
                          <h4 className="font-medium mb-2">Description</h4>
                          <p className="text-gray-300">{test.description}</p>
                        </div>
                        
                        <div className="grid grid-cols-2 gap-4">
                          <div>
                            <h4 className="font-medium mb-2">Status</h4>
                            <div className="flex items-center space-x-2">
                              {getStatusIcon(test.status)}
                              <span className="capitalize">{test.status}</span>
                            </div>
                          </div>
                          <div>
                            <h4 className="font-medium mb-2">Duration</h4>
                            <span>{test.duration ? `${(test.duration / 1000).toFixed(2)}s` : 'N/A'}</span>
                          </div>
                        </div>
                        
                        {test.details && (
                          <div>
                            <h4 className="font-medium mb-2">Details</h4>
                            <pre className="bg-gray-800 rounded p-3 text-sm overflow-auto">
                              {JSON.stringify(test.details, null, 2)}
                            </pre>
                          </div>
                        )}
                        
                        {test.error && (
                          <div>
                            <h4 className="font-medium mb-2 text-red-400">Error</h4>
                            <div className="bg-red-900/20 border border-red-500 rounded p-3 text-sm">
                              {test.error}
                            </div>
                          </div>
                        )}
                      </div>
                    </>
                  );
                })()}
              </motion.div>
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    </div>
  );
};

export default TradingFeatureValidator;