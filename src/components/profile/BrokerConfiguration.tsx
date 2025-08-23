import React, { useState } from 'react';
import { 
  Settings, 
  Plus, 
  Edit, 
  Trash2, 
  Eye, 
  EyeOff, 
  CheckCircle, 
  AlertTriangle, 
  Clock,
  Wifi,
  WifiOff,
  Key,
  Shield,
  DollarSign,
  TrendingUp,
  BarChart,
  Users,
  Globe,
  Zap
} from 'lucide-react';
import LoadingSpinner from '../common/LoadingSpinner';
import ConfirmationModal from '../common/ConfirmationModal';
import StatusBadge from '../common/StatusBadge';

interface BrokerConfig {
  id: string;
  name: string;
  brokerType: 'interactive_brokers' | 'td_ameritrade' | 'alpaca' | 'fidelity' | 'schwab' | 'etrade' | 'zerodha' | 'upstox';
  displayName: string;
  status: 'connected' | 'disconnected' | 'connecting' | 'error';
  isDefault: boolean;
  isEnabled: boolean;
  credentials: {
    apiKey: string;
    secretKey: string;
    accountId: string;
    clientId?: string;
    additionalFields: Record<string, string>;
  };
  configuration: {
    environment: 'sandbox' | 'live';
    orderRouting: 'smart' | 'direct' | 'market_maker';
    riskLimits: {
      dailyTradeLimit: number;
      positionSizeLimit: number;
      stopLossRequired: boolean;
    };
    permissions: {
      equities: boolean;
      options: boolean;
      futures: boolean;
      forex: boolean;
      crypto: boolean;
    };
    notifications: {
      orderFills: boolean;
      accountAlerts: boolean;
      systemStatus: boolean;
    };
    advanced: {
      autoReconnect: boolean;
      timeout: number;
      retryAttempts: number;
      compressionEnabled: boolean;
    };
  };
  performance: {
    avgExecutionTime: number;
    successRate: number;
    totalOrders: number;
    lastConnected: Date | null;
    uptime: number;
  };
}

const brokerTemplates = [
  {
    type: 'interactive_brokers',
    name: 'Interactive Brokers',
    logo: '🏛️',
    description: 'Professional trading platform with global market access',
    features: ['Global Markets', 'Options', 'Futures', 'Forex'],
    fields: ['Client ID', 'Username', 'Password', 'Account ID']
  },
  {
    type: 'zerodha',
    name: 'Zerodha Kite',
    logo: '🪁',
    description: 'India\'s largest discount broker with advanced trading tools',
    features: ['NSE/BSE', 'F&O', 'Commodities', 'Currency'],
    fields: ['API Key', 'API Secret', 'Request Token', 'User ID']
  },
  {
    type: 'upstox',
    name: 'Upstox Pro',
    logo: '📈',
    description: 'Technology-driven stockbroker with competitive pricing',
    features: ['NSE/BSE', 'F&O', 'Commodities', 'IPO'],
    fields: ['API Key', 'API Secret', 'Redirect URI', 'Client ID']
  },
  {
    type: 'alpaca',
    name: 'Alpaca Markets',
    logo: '🦙',
    description: 'Commission-free API-first stock trading platform',
    features: ['US Stocks', 'ETFs', 'Crypto', 'Paper Trading'],
    fields: ['API Key', 'Secret Key', 'Base URL']
  },
  {
    type: 'td_ameritrade',
    name: 'TD Ameritrade',
    logo: '🏢',
    description: 'Full-service broker with comprehensive research tools',
    features: ['US Stocks', 'Options', 'ETFs', 'Mutual Funds'],
    fields: ['Consumer Key', 'Refresh Token', 'Account ID']
  }
];

const orderRoutingOptions = [
  { value: 'smart', label: 'Smart Order Routing', description: 'Automatically find best execution' },
  { value: 'direct', label: 'Direct Market Access', description: 'Route directly to exchanges' },
  { value: 'market_maker', label: 'Market Maker', description: 'Route to market makers for better fills' }
];

export const BrokerConfiguration: React.FC = () => {
  const [brokers, setBrokers] = useState<BrokerConfig[]>([
    {
      id: '1',
      name: 'Primary-IB',
      brokerType: 'interactive_brokers',
      displayName: 'Interactive Brokers - Main Account',
      status: 'connected',
      isDefault: true,
      isEnabled: true,
      credentials: {
        apiKey: '****MASKED****',
        secretKey: '****MASKED****',
        accountId: 'DU123456',
        clientId: 'IBKR123',
        additionalFields: {}
      },
      configuration: {
        environment: 'live',
        orderRouting: 'smart',
        riskLimits: {
          dailyTradeLimit: 100000,
          positionSizeLimit: 50000,
          stopLossRequired: true
        },
        permissions: {
          equities: true,
          options: true,
          futures: true,
          forex: true,
          crypto: false
        },
        notifications: {
          orderFills: true,
          accountAlerts: true,
          systemStatus: true
        },
        advanced: {
          autoReconnect: true,
          timeout: 30000,
          retryAttempts: 3,
          compressionEnabled: true
        }
      },
      performance: {
        avgExecutionTime: 250,
        successRate: 98.5,
        totalOrders: 1247,
        lastConnected: new Date(),
        uptime: 99.2
      }
    },
    {
      id: '2',
      name: 'Zerodha-Primary',
      brokerType: 'zerodha',
      displayName: 'Zerodha Kite - Trading Account',
      status: 'connected',
      isDefault: false,
      isEnabled: true,
      credentials: {
        apiKey: '****MASKED****',
        secretKey: '****MASKED****',
        accountId: 'AB1234',
        additionalFields: { userId: 'AB1234' }
      },
      configuration: {
        environment: 'live',
        orderRouting: 'direct',
        riskLimits: {
          dailyTradeLimit: 50000,
          positionSizeLimit: 25000,
          stopLossRequired: true
        },
        permissions: {
          equities: true,
          options: true,
          futures: false,
          forex: false,
          crypto: false
        },
        notifications: {
          orderFills: true,
          accountAlerts: true,
          systemStatus: false
        },
        advanced: {
          autoReconnect: true,
          timeout: 15000,
          retryAttempts: 5,
          compressionEnabled: false
        }
      },
      performance: {
        avgExecutionTime: 180,
        successRate: 99.1,
        totalOrders: 856,
        lastConnected: new Date(Date.now() - 5 * 60 * 1000),
        uptime: 97.8
      }
    }
  ]);

  const [showAddModal, setShowAddModal] = useState(false);
  const [editingBroker, setEditingBroker] = useState<BrokerConfig | null>(null);
  const [showCredentials, setShowCredentials] = useState<Record<string, boolean>>({});
  const [deleteConfirm, setDeleteConfirm] = useState<string | null>(null);
  const [selectedTemplate, setSelectedTemplate] = useState<string>('');

  const handleAddBroker = (template: any) => {
    const newBroker: BrokerConfig = {
      id: Date.now().toString(),
      name: `${template.name}-${Date.now()}`,
      brokerType: template.type,
      displayName: template.name,
      status: 'disconnected',
      isDefault: false,
      isEnabled: true,
      credentials: {
        apiKey: '',
        secretKey: '',
        accountId: '',
        additionalFields: {}
      },
      configuration: {
        environment: 'sandbox',
        orderRouting: 'smart',
        riskLimits: {
          dailyTradeLimit: 10000,
          positionSizeLimit: 5000,
          stopLossRequired: true
        },
        permissions: {
          equities: true,
          options: false,
          futures: false,
          forex: false,
          crypto: false
        },
        notifications: {
          orderFills: true,
          accountAlerts: true,
          systemStatus: true
        },
        advanced: {
          autoReconnect: true,
          timeout: 30000,
          retryAttempts: 3,
          compressionEnabled: true
        }
      },
      performance: {
        avgExecutionTime: 0,
        successRate: 0,
        totalOrders: 0,
        lastConnected: null,
        uptime: 0
      }
    };

    setBrokers(prev => [...prev, newBroker]);
    setEditingBroker(newBroker);
    setShowAddModal(false);
  };

  const handleUpdateBroker = (updatedBroker: BrokerConfig) => {
    setBrokers(prev => 
      prev.map(broker => 
        broker.id === updatedBroker.id ? updatedBroker : broker
      )
    );
    setEditingBroker(null);
  };

  const handleDeleteBroker = (brokerId: string) => {
    setBrokers(prev => prev.filter(broker => broker.id !== brokerId));
    setDeleteConfirm(null);
  };

  const toggleBrokerStatus = (brokerId: string) => {
    setBrokers(prev => 
      prev.map(broker => {
        if (broker.id === brokerId) {
          const newStatus = broker.status === 'connected' ? 'disconnected' : 'connecting';
          // Simulate connection process
          if (newStatus === 'connecting') {
            setTimeout(() => {
              setBrokers(current => 
                current.map(b => 
                  b.id === brokerId 
                    ? { ...b, status: 'connected', performance: { ...b.performance, lastConnected: new Date() } }
                    : b
                )
              );
            }, 2000);
          }
          return { ...broker, status: newStatus };
        }
        return broker;
      })
    );
  };

  const setDefaultBroker = (brokerId: string) => {
    setBrokers(prev => 
      prev.map(broker => ({
        ...broker,
        isDefault: broker.id === brokerId
      }))
    );
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'connected': return 'text-green-400';
      case 'connecting': return 'text-yellow-400';
      case 'error': return 'text-red-400';
      default: return 'text-gray-400';
    }
  };

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'connected': return <CheckCircle className="w-5 h-5 text-green-400" />;
      case 'connecting': return <Clock className="w-5 h-5 text-yellow-400 animate-spin" />;
      case 'error': return <AlertTriangle className="w-5 h-5 text-red-400" />;
      default: return <WifiOff className="w-5 h-5 text-gray-400" />;
    }
  };

  const getBrokerLogo = (brokerType: string) => {
    const template = brokerTemplates.find(t => t.type === brokerType);
    return template?.logo || '🏦';
  };

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h3 className="text-2xl font-bold text-white mb-2">Broker Configuration</h3>
          <p className="text-slate-400">Manage your trading broker connections and settings</p>
        </div>
        
        <button
          onClick={() => setShowAddModal(true)}
          className="flex items-center space-x-2 bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition-colors"
        >
          <Plus className="w-4 h-4" />
          <span>Add Broker</span>
        </button>
      </div>

      {/* Broker Cards */}
      <div className="space-y-4">
        {brokers.map(broker => (
          <div key={broker.id} className="bg-slate-800/30 rounded-xl p-6 border border-slate-700/50">
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center space-x-4">
                <div className="text-3xl">{getBrokerLogo(broker.brokerType)}</div>
                <div>
                  <div className="flex items-center space-x-3">
                    <h4 className="text-lg font-semibold text-white">{broker.displayName}</h4>
                    {broker.isDefault && (
                      <span className="bg-blue-500/20 text-blue-400 px-2 py-1 rounded-full text-xs font-medium">
                        Default
                      </span>
                    )}
                    {!broker.isEnabled && (
                      <span className="bg-gray-500/20 text-gray-400 px-2 py-1 rounded-full text-xs font-medium">
                        Disabled
                      </span>
                    )}
                  </div>
                  <div className="flex items-center space-x-2 text-sm text-slate-400">
                    <span>Account: {broker.credentials.accountId}</span>
                    <span>•</span>
                    <span className="capitalize">{broker.configuration.environment}</span>
                    <span>•</span>
                    <StatusBadge status={broker.status} size="sm" />
                  </div>
                </div>
              </div>

              <div className="flex items-center space-x-2">
                {broker.status === 'connected' ? (
                  <button
                    onClick={() => toggleBrokerStatus(broker.id)}
                    className="flex items-center space-x-2 bg-red-500/20 text-red-400 px-3 py-1 rounded-lg hover:bg-red-500/30 transition-colors text-sm"
                  >
                    <WifiOff className="w-4 h-4" />
                    <span>Disconnect</span>
                  </button>
                ) : (
                  <button
                    onClick={() => toggleBrokerStatus(broker.id)}
                    disabled={broker.status === 'connecting'}
                    className="flex items-center space-x-2 bg-green-500/20 text-green-400 px-3 py-1 rounded-lg hover:bg-green-500/30 transition-colors text-sm disabled:opacity-50"
                  >
                    <Wifi className="w-4 h-4" />
                    <span>{broker.status === 'connecting' ? 'Connecting...' : 'Connect'}</span>
                  </button>
                )}

                {!broker.isDefault && broker.status === 'connected' && (
                  <button
                    onClick={() => setDefaultBroker(broker.id)}
                    className="bg-blue-500/20 text-blue-400 px-3 py-1 rounded-lg hover:bg-blue-500/30 transition-colors text-sm"
                  >
                    Set Default
                  </button>
                )}

                <button
                  onClick={() => setEditingBroker(broker)}
                  className="p-2 text-slate-400 hover:text-blue-400 transition-colors"
                  title="Configure"
                >
                  <Settings className="w-4 h-4" />
                </button>

                <button
                  onClick={() => setDeleteConfirm(broker.id)}
                  className="p-2 text-slate-400 hover:text-red-400 transition-colors"
                  title="Delete"
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              </div>
            </div>

            {/* Performance Metrics */}
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
              <div className="text-center p-3 bg-slate-700/30 rounded-lg">
                <div className="text-lg font-bold text-white">{broker.performance.avgExecutionTime}ms</div>
                <div className="text-xs text-slate-400">Avg Execution</div>
              </div>
              <div className="text-center p-3 bg-slate-700/30 rounded-lg">
                <div className="text-lg font-bold text-green-400">{broker.performance.successRate}%</div>
                <div className="text-xs text-slate-400">Success Rate</div>
              </div>
              <div className="text-center p-3 bg-slate-700/30 rounded-lg">
                <div className="text-lg font-bold text-blue-400">{broker.performance.totalOrders.toLocaleString()}</div>
                <div className="text-xs text-slate-400">Total Orders</div>
              </div>
              <div className="text-center p-3 bg-slate-700/30 rounded-lg">
                <div className="text-lg font-bold text-purple-400">{broker.performance.uptime}%</div>
                <div className="text-xs text-slate-400">Uptime</div>
              </div>
            </div>

            {/* Capabilities */}
            <div className="mt-4 pt-4 border-t border-slate-700/50">
              <div className="text-sm text-slate-400 mb-2">Enabled Markets:</div>
              <div className="flex flex-wrap gap-2">
                {Object.entries(broker.configuration.permissions)
                  .filter(([_, enabled]) => enabled)
                  .map(([market, _]) => (
                    <span
                      key={market}
                      className="bg-green-500/20 text-green-400 px-2 py-1 rounded text-xs uppercase font-medium"
                    >
                      {market}
                    </span>
                  ))}
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Add Broker Modal */}
      {showAddModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-slate-800 rounded-xl max-w-4xl w-full mx-4 max-h-[90vh] overflow-y-auto">
            <div className="p-6 border-b border-slate-700">
              <h3 className="text-xl font-bold text-white">Add New Broker</h3>
              <p className="text-slate-400">Choose a broker to connect to your trading account</p>
            </div>
            
            <div className="p-6">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {brokerTemplates.map(template => (
                  <div
                    key={template.type}
                    className="border border-slate-600 rounded-lg p-4 hover:border-blue-500 transition-colors cursor-pointer"
                    onClick={() => handleAddBroker(template)}
                  >
                    <div className="flex items-center space-x-4 mb-3">
                      <div className="text-2xl">{template.logo}</div>
                      <div>
                        <h4 className="font-semibold text-white">{template.name}</h4>
                        <p className="text-sm text-slate-400">{template.description}</p>
                      </div>
                    </div>
                    
                    <div className="mb-3">
                      <div className="text-xs text-slate-400 mb-1">Features:</div>
                      <div className="flex flex-wrap gap-1">
                        {template.features.map(feature => (
                          <span
                            key={feature}
                            className="bg-blue-500/20 text-blue-400 px-2 py-1 rounded text-xs"
                          >
                            {feature}
                          </span>
                        ))}
                      </div>
                    </div>
                    
                    <div>
                      <div className="text-xs text-slate-400 mb-1">Required Fields:</div>
                      <div className="text-xs text-slate-300">
                        {template.fields.join(', ')}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
            
            <div className="p-6 border-t border-slate-700 flex justify-end">
              <button
                onClick={() => setShowAddModal(false)}
                className="px-4 py-2 text-slate-400 hover:text-white transition-colors"
              >
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Broker Configuration Modal */}
      {editingBroker && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-slate-800 rounded-xl max-w-2xl w-full mx-4 max-h-[90vh] overflow-y-auto">
            <div className="p-6 border-b border-slate-700">
              <h3 className="text-xl font-bold text-white">Configure {editingBroker.displayName}</h3>
            </div>
            
            <div className="p-6 space-y-6">
              {/* Basic Settings */}
              <div>
                <h4 className="font-semibold text-white mb-3">Basic Settings</h4>
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-slate-300 mb-1">Display Name</label>
                    <input
                      type="text"
                      value={editingBroker.displayName}
                      onChange={(e) => setEditingBroker({...editingBroker, displayName: e.target.value})}
                      className="w-full bg-slate-700 border border-slate-600 text-white rounded-lg px-3 py-2"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-slate-300 mb-1">Environment</label>
                    <select
                      value={editingBroker.configuration.environment}
                      onChange={(e) => setEditingBroker({
                        ...editingBroker,
                        configuration: {
                          ...editingBroker.configuration,
                          environment: e.target.value as 'sandbox' | 'live'
                        }
                      })}
                      className="w-full bg-slate-700 border border-slate-600 text-white rounded-lg px-3 py-2"
                    >
                      <option value="sandbox">Sandbox</option>
                      <option value="live">Live Trading</option>
                    </select>
                  </div>
                </div>
              </div>

              {/* Risk Limits */}
              <div>
                <h4 className="font-semibold text-white mb-3">Risk Limits</h4>
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-slate-300 mb-1">Daily Trade Limit (₹)</label>
                    <input
                      type="number"
                      value={editingBroker.configuration.riskLimits.dailyTradeLimit}
                      onChange={(e) => setEditingBroker({
                        ...editingBroker,
                        configuration: {
                          ...editingBroker.configuration,
                          riskLimits: {
                            ...editingBroker.configuration.riskLimits,
                            dailyTradeLimit: parseInt(e.target.value)
                          }
                        }
                      })}
                      className="w-full bg-slate-700 border border-slate-600 text-white rounded-lg px-3 py-2"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-slate-300 mb-1">Position Size Limit (₹)</label>
                    <input
                      type="number"
                      value={editingBroker.configuration.riskLimits.positionSizeLimit}
                      onChange={(e) => setEditingBroker({
                        ...editingBroker,
                        configuration: {
                          ...editingBroker.configuration,
                          riskLimits: {
                            ...editingBroker.configuration.riskLimits,
                            positionSizeLimit: parseInt(e.target.value)
                          }
                        }
                      })}
                      className="w-full bg-slate-700 border border-slate-600 text-white rounded-lg px-3 py-2"
                    />
                  </div>
                </div>
              </div>

              {/* Market Permissions */}
              <div>
                <h4 className="font-semibold text-white mb-3">Market Permissions</h4>
                <div className="grid grid-cols-2 gap-3">
                  {Object.entries(editingBroker.configuration.permissions).map(([market, enabled]) => (
                    <div key={market} className="flex items-center justify-between">
                      <span className="text-slate-300 capitalize">{market}</span>
                      <label className="relative inline-flex items-center cursor-pointer">
                        <input
                          type="checkbox"
                          checked={enabled}
                          onChange={(e) => setEditingBroker({
                            ...editingBroker,
                            configuration: {
                              ...editingBroker.configuration,
                              permissions: {
                                ...editingBroker.configuration.permissions,
                                [market]: e.target.checked
                              }
                            }
                          })}
                          className="sr-only peer"
                        />
                        <div className="w-11 h-6 bg-slate-600 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-green-600"></div>
                      </label>
                    </div>
                  ))}
                </div>
              </div>
            </div>
            
            <div className="p-6 border-t border-slate-700 flex justify-end space-x-3">
              <button
                onClick={() => setEditingBroker(null)}
                className="px-4 py-2 text-slate-400 hover:text-white transition-colors"
              >
                Cancel
              </button>
              <button
                onClick={() => handleUpdateBroker(editingBroker)}
                className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
              >
                Save Changes
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Delete Confirmation Modal */}
      <ConfirmationModal
        isOpen={deleteConfirm !== null}
        onClose={() => setDeleteConfirm(null)}
        onConfirm={() => deleteConfirm && handleDeleteBroker(deleteConfirm)}
        title="Delete Broker Configuration"
        message="Are you sure you want to delete this broker configuration? This action cannot be undone and will disconnect any active sessions."
        variant="danger"
        confirmText="Delete"
      />
    </div>
  );
};

export default BrokerConfiguration;