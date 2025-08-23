import React, { useState } from 'react';
import { 
  Settings, 
  Bell, 
  Globe, 
  Palette, 
  Shield, 
  DollarSign, 
  Clock,
  Volume2,
  VolumeX,
  Mail,
  MessageSquare,
  Phone,
  Monitor,
  Sun,
  Moon,
  Smartphone,
  Save,
  RotateCcw,
  AlertTriangle
} from 'lucide-react';
import { useAuthStore } from '../../stores/auth.store';

interface UserPreferencesData {
  display: {
    theme: 'light' | 'dark' | 'auto';
    language: string;
    currency: string;
    timezone: string;
    dateFormat: string;
    numberFormat: string;
  };
  notifications: {
    email: {
      orderUpdates: boolean;
      marketAlerts: boolean;
      accountActivity: boolean;
      promotions: boolean;
      securityAlerts: boolean;
    };
    push: {
      orderUpdates: boolean;
      marketAlerts: boolean;
      accountActivity: boolean;
      promotions: boolean;
      securityAlerts: boolean;
    };
    sms: {
      orderUpdates: boolean;
      marketAlerts: boolean;
      accountActivity: boolean;
      securityAlerts: boolean;
    };
    inApp: {
      sound: boolean;
      desktop: boolean;
      frequency: 'all' | 'important' | 'critical';
    };
  };
  trading: {
    defaultOrderType: 'market' | 'limit' | 'stop';
    confirmations: {
      orderPlacement: boolean;
      orderCancellation: boolean;
      orderModification: boolean;
    };
    riskWarnings: {
      enabled: boolean;
      thresholds: {
        dailyLoss: number;
        positionSize: number;
        portfolioRisk: number;
      };
    };
    autoLogout: {
      enabled: boolean;
      timeoutMinutes: number;
    };
  };
  privacy: {
    dataSharing: {
      analytics: boolean;
      marketing: boolean;
      performance: boolean;
    };
    visibility: {
      profile: 'public' | 'private';
      tradingActivity: 'public' | 'friends' | 'private';
      portfolio: 'public' | 'friends' | 'private';
    };
  };
}

const languages = [
  { code: 'en', name: 'English', flag: '🇺🇸' },
  { code: 'hi', name: 'हिन्दी', flag: '🇮🇳' },
  { code: 'es', name: 'Español', flag: '🇪🇸' },
  { code: 'fr', name: 'Français', flag: '🇫🇷' },
  { code: 'de', name: 'Deutsch', flag: '🇩🇪' },
  { code: 'ja', name: '日本語', flag: '🇯🇵' },
  { code: 'zh', name: '中文', flag: '🇨🇳' }
];

const currencies = [
  { code: 'USD', name: 'US Dollar', symbol: '$' },
  { code: 'INR', name: 'Indian Rupee', symbol: '₹' },
  { code: 'EUR', name: 'Euro', symbol: '€' },
  { code: 'GBP', name: 'British Pound', symbol: '£' },
  { code: 'JPY', name: 'Japanese Yen', symbol: '¥' },
  { code: 'AUD', name: 'Australian Dollar', symbol: 'A$' }
];

const timezones = [
  'UTC',
  'America/New_York',
  'America/Los_Angeles',
  'Europe/London',
  'Europe/Berlin',
  'Asia/Tokyo',
  'Asia/Shanghai',
  'Asia/Mumbai',
  'Asia/Singapore',
  'Australia/Sydney'
];

export const UserPreferences: React.FC = () => {
  const { user } = useAuthStore();
  const [hasChanges, setHasChanges] = useState(false);
  const [saving, setSaving] = useState(false);
  const [activeSection, setActiveSection] = useState('display');
  
  const [preferences, setPreferences] = useState<UserPreferencesData>({
    display: {
      theme: 'dark',
      language: 'en',
      currency: 'INR',
      timezone: 'Asia/Mumbai',
      dateFormat: 'DD/MM/YYYY',
      numberFormat: 'en-IN'
    },
    notifications: {
      email: {
        orderUpdates: true,
        marketAlerts: true,
        accountActivity: true,
        promotions: false,
        securityAlerts: true
      },
      push: {
        orderUpdates: true,
        marketAlerts: true,
        accountActivity: false,
        promotions: false,
        securityAlerts: true
      },
      sms: {
        orderUpdates: true,
        marketAlerts: false,
        accountActivity: false,
        securityAlerts: true
      },
      inApp: {
        sound: true,
        desktop: true,
        frequency: 'important'
      }
    },
    trading: {
      defaultOrderType: 'limit',
      confirmations: {
        orderPlacement: true,
        orderCancellation: false,
        orderModification: true
      },
      riskWarnings: {
        enabled: true,
        thresholds: {
          dailyLoss: 5000,
          positionSize: 10000,
          portfolioRisk: 25
        }
      },
      autoLogout: {
        enabled: true,
        timeoutMinutes: 30
      }
    },
    privacy: {
      dataSharing: {
        analytics: true,
        marketing: false,
        performance: true
      },
      visibility: {
        profile: 'public',
        tradingActivity: 'private',
        portfolio: 'private'
      }
    }
  });

  const sections = [
    { id: 'display', label: 'Display & Language', icon: Monitor },
    { id: 'notifications', label: 'Notifications', icon: Bell },
    { id: 'trading', label: 'Trading Preferences', icon: DollarSign },
    { id: 'privacy', label: 'Privacy & Security', icon: Shield }
  ];

  const updatePreference = (path: string[], value: any) => {
    setPreferences(prev => {
      const newPrefs = { ...prev };
      let current = newPrefs as any;
      
      for (let i = 0; i < path.length - 1; i++) {
        current[path[i]] = { ...current[path[i]] };
        current = current[path[i]];
      }
      
      current[path[path.length - 1]] = value;
      return newPrefs;
    });
    setHasChanges(true);
  };

  const handleSave = async () => {
    setSaving(true);
    try {
      // Simulate API call
      await new Promise(resolve => setTimeout(resolve, 1000));
      setHasChanges(false);
      // Show success message
    } catch (error) {
      console.error('Failed to save preferences:', error);
    } finally {
      setSaving(false);
    }
  };

  const handleReset = () => {
    // Reset to default values
    window.location.reload();
  };

  const renderDisplaySection = () => (
    <div className="space-y-6">
      <div>
        <h4 className="text-lg font-semibold text-white mb-4">Appearance</h4>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-slate-300 mb-2">Theme</label>
            <div className="grid grid-cols-3 gap-2">
              {[
                { value: 'light', label: 'Light', icon: Sun },
                { value: 'dark', label: 'Dark', icon: Moon },
                { value: 'auto', label: 'Auto', icon: Monitor }
              ].map(({ value, label, icon: Icon }) => (
                <button
                  key={value}
                  onClick={() => updatePreference(['display', 'theme'], value)}
                  className={`flex flex-col items-center p-3 rounded-lg border transition-all ${
                    preferences.display.theme === value
                      ? 'border-blue-500 bg-blue-500/10 text-blue-400'
                      : 'border-slate-600 bg-slate-700/30 text-slate-400 hover:border-slate-500'
                  }`}
                >
                  <Icon className="w-5 h-5 mb-1" />
                  <span className="text-sm">{label}</span>
                </button>
              ))}
            </div>
          </div>
          
          <div>
            <label className="block text-sm font-medium text-slate-300 mb-2">Language</label>
            <select
              value={preferences.display.language}
              onChange={(e) => updatePreference(['display', 'language'], e.target.value)}
              className="w-full bg-slate-700 border border-slate-600 text-white rounded-lg px-3 py-2"
            >
              {languages.map(lang => (
                <option key={lang.code} value={lang.code}>
                  {lang.flag} {lang.name}
                </option>
              ))}
            </select>
          </div>
        </div>
      </div>

      <div>
        <h4 className="text-lg font-semibold text-white mb-4">Regional Settings</h4>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-slate-300 mb-2">Currency</label>
            <select
              value={preferences.display.currency}
              onChange={(e) => updatePreference(['display', 'currency'], e.target.value)}
              className="w-full bg-slate-700 border border-slate-600 text-white rounded-lg px-3 py-2"
            >
              {currencies.map(currency => (
                <option key={currency.code} value={currency.code}>
                  {currency.symbol} {currency.name}
                </option>
              ))}
            </select>
          </div>
          
          <div>
            <label className="block text-sm font-medium text-slate-300 mb-2">Timezone</label>
            <select
              value={preferences.display.timezone}
              onChange={(e) => updatePreference(['display', 'timezone'], e.target.value)}
              className="w-full bg-slate-700 border border-slate-600 text-white rounded-lg px-3 py-2"
            >
              {timezones.map(tz => (
                <option key={tz} value={tz}>{tz}</option>
              ))}
            </select>
          </div>
          
          <div>
            <label className="block text-sm font-medium text-slate-300 mb-2">Date Format</label>
            <select
              value={preferences.display.dateFormat}
              onChange={(e) => updatePreference(['display', 'dateFormat'], e.target.value)}
              className="w-full bg-slate-700 border border-slate-600 text-white rounded-lg px-3 py-2"
            >
              <option value="DD/MM/YYYY">DD/MM/YYYY</option>
              <option value="MM/DD/YYYY">MM/DD/YYYY</option>
              <option value="YYYY-MM-DD">YYYY-MM-DD</option>
            </select>
          </div>
          
          <div>
            <label className="block text-sm font-medium text-slate-300 mb-2">Number Format</label>
            <select
              value={preferences.display.numberFormat}
              onChange={(e) => updatePreference(['display', 'numberFormat'], e.target.value)}
              className="w-full bg-slate-700 border border-slate-600 text-white rounded-lg px-3 py-2"
            >
              <option value="en-IN">Indian (1,23,45,678.90)</option>
              <option value="en-US">US (1,234,567.90)</option>
              <option value="de-DE">German (1.234.567,90)</option>
              <option value="fr-FR">French (1 234 567,90)</option>
            </select>
          </div>
        </div>
      </div>
    </div>
  );

  const renderNotificationsSection = () => (
    <div className="space-y-6">
      <div className="bg-slate-800/30 rounded-xl p-4">
        <h4 className="text-lg font-semibold text-white mb-4 flex items-center">
          <Mail className="w-5 h-5 mr-2 text-blue-400" />
          Email Notifications
        </h4>
        <div className="space-y-3">
          {Object.entries(preferences.notifications.email).map(([key, value]) => (
            <div key={key} className="flex items-center justify-between">
              <span className="text-slate-300 capitalize">{key.replace(/([A-Z])/g, ' $1')}</span>
              <label className="relative inline-flex items-center cursor-pointer">
                <input
                  type="checkbox"
                  checked={value}
                  onChange={(e) => updatePreference(['notifications', 'email', key], e.target.checked)}
                  className="sr-only peer"
                />
                <div className="w-11 h-6 bg-slate-600 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-blue-600"></div>
              </label>
            </div>
          ))}
        </div>
      </div>

      <div className="bg-slate-800/30 rounded-xl p-4">
        <h4 className="text-lg font-semibold text-white mb-4 flex items-center">
          <Smartphone className="w-5 h-5 mr-2 text-green-400" />
          Push Notifications
        </h4>
        <div className="space-y-3">
          {Object.entries(preferences.notifications.push).map(([key, value]) => (
            <div key={key} className="flex items-center justify-between">
              <span className="text-slate-300 capitalize">{key.replace(/([A-Z])/g, ' $1')}</span>
              <label className="relative inline-flex items-center cursor-pointer">
                <input
                  type="checkbox"
                  checked={value}
                  onChange={(e) => updatePreference(['notifications', 'push', key], e.target.checked)}
                  className="sr-only peer"
                />
                <div className="w-11 h-6 bg-slate-600 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-green-600"></div>
              </label>
            </div>
          ))}
        </div>
      </div>

      <div className="bg-slate-800/30 rounded-xl p-4">
        <h4 className="text-lg font-semibold text-white mb-4 flex items-center">
          <Phone className="w-5 h-5 mr-2 text-purple-400" />
          SMS Notifications
        </h4>
        <div className="space-y-3">
          {Object.entries(preferences.notifications.sms).map(([key, value]) => (
            <div key={key} className="flex items-center justify-between">
              <span className="text-slate-300 capitalize">{key.replace(/([A-Z])/g, ' $1')}</span>
              <label className="relative inline-flex items-center cursor-pointer">
                <input
                  type="checkbox"
                  checked={value}
                  onChange={(e) => updatePreference(['notifications', 'sms', key], e.target.checked)}
                  className="sr-only peer"
                />
                <div className="w-11 h-6 bg-slate-600 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-purple-600"></div>
              </label>
            </div>
          ))}
        </div>
      </div>

      <div className="bg-slate-800/30 rounded-xl p-4">
        <h4 className="text-lg font-semibold text-white mb-4 flex items-center">
          <Volume2 className="w-5 h-5 mr-2 text-orange-400" />
          In-App Settings
        </h4>
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <span className="text-slate-300">Sound Notifications</span>
            <label className="relative inline-flex items-center cursor-pointer">
              <input
                type="checkbox"
                checked={preferences.notifications.inApp.sound}
                onChange={(e) => updatePreference(['notifications', 'inApp', 'sound'], e.target.checked)}
                className="sr-only peer"
              />
              <div className="w-11 h-6 bg-slate-600 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-orange-600"></div>
            </label>
          </div>
          
          <div className="flex items-center justify-between">
            <span className="text-slate-300">Desktop Notifications</span>
            <label className="relative inline-flex items-center cursor-pointer">
              <input
                type="checkbox"
                checked={preferences.notifications.inApp.desktop}
                onChange={(e) => updatePreference(['notifications', 'inApp', 'desktop'], e.target.checked)}
                className="sr-only peer"
              />
              <div className="w-11 h-6 bg-slate-600 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-orange-600"></div>
            </label>
          </div>
          
          <div>
            <label className="block text-sm font-medium text-slate-300 mb-2">Notification Frequency</label>
            <select
              value={preferences.notifications.inApp.frequency}
              onChange={(e) => updatePreference(['notifications', 'inApp', 'frequency'], e.target.value)}
              className="w-full bg-slate-700 border border-slate-600 text-white rounded-lg px-3 py-2"
            >
              <option value="all">All Notifications</option>
              <option value="important">Important Only</option>
              <option value="critical">Critical Only</option>
            </select>
          </div>
        </div>
      </div>
    </div>
  );

  const renderTradingSection = () => (
    <div className="space-y-6">
      <div className="bg-slate-800/30 rounded-xl p-4">
        <h4 className="text-lg font-semibold text-white mb-4">Trading Defaults</h4>
        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-slate-300 mb-2">Default Order Type</label>
            <select
              value={preferences.trading.defaultOrderType}
              onChange={(e) => updatePreference(['trading', 'defaultOrderType'], e.target.value)}
              className="w-full bg-slate-700 border border-slate-600 text-white rounded-lg px-3 py-2"
            >
              <option value="market">Market Order</option>
              <option value="limit">Limit Order</option>
              <option value="stop">Stop Order</option>
            </select>
          </div>
        </div>
      </div>

      <div className="bg-slate-800/30 rounded-xl p-4">
        <h4 className="text-lg font-semibold text-white mb-4">Order Confirmations</h4>
        <div className="space-y-3">
          {Object.entries(preferences.trading.confirmations).map(([key, value]) => (
            <div key={key} className="flex items-center justify-between">
              <span className="text-slate-300 capitalize">{key.replace(/([A-Z])/g, ' $1')}</span>
              <label className="relative inline-flex items-center cursor-pointer">
                <input
                  type="checkbox"
                  checked={value}
                  onChange={(e) => updatePreference(['trading', 'confirmations', key], e.target.checked)}
                  className="sr-only peer"
                />
                <div className="w-11 h-6 bg-slate-600 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-blue-600"></div>
              </label>
            </div>
          ))}
        </div>
      </div>

      <div className="bg-slate-800/30 rounded-xl p-4">
        <h4 className="text-lg font-semibold text-white mb-4 flex items-center">
          <AlertTriangle className="w-5 h-5 mr-2 text-yellow-400" />
          Risk Management
        </h4>
        <div className="space-y-4">
          <div className="flex items-center justify-between mb-4">
            <span className="text-slate-300">Enable Risk Warnings</span>
            <label className="relative inline-flex items-center cursor-pointer">
              <input
                type="checkbox"
                checked={preferences.trading.riskWarnings.enabled}
                onChange={(e) => updatePreference(['trading', 'riskWarnings', 'enabled'], e.target.checked)}
                className="sr-only peer"
              />
              <div className="w-11 h-6 bg-slate-600 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-yellow-600"></div>
            </label>
          </div>
          
          {preferences.trading.riskWarnings.enabled && (
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div>
                <label className="block text-sm font-medium text-slate-300 mb-2">Daily Loss Limit (₹)</label>
                <input
                  type="number"
                  value={preferences.trading.riskWarnings.thresholds.dailyLoss}
                  onChange={(e) => updatePreference(['trading', 'riskWarnings', 'thresholds', 'dailyLoss'], parseInt(e.target.value))}
                  className="w-full bg-slate-700 border border-slate-600 text-white rounded-lg px-3 py-2"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-300 mb-2">Position Size Limit (₹)</label>
                <input
                  type="number"
                  value={preferences.trading.riskWarnings.thresholds.positionSize}
                  onChange={(e) => updatePreference(['trading', 'riskWarnings', 'thresholds', 'positionSize'], parseInt(e.target.value))}
                  className="w-full bg-slate-700 border border-slate-600 text-white rounded-lg px-3 py-2"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-300 mb-2">Portfolio Risk (%)</label>
                <input
                  type="number"
                  value={preferences.trading.riskWarnings.thresholds.portfolioRisk}
                  onChange={(e) => updatePreference(['trading', 'riskWarnings', 'thresholds', 'portfolioRisk'], parseInt(e.target.value))}
                  className="w-full bg-slate-700 border border-slate-600 text-white rounded-lg px-3 py-2"
                />
              </div>
            </div>
          )}
        </div>
      </div>

      <div className="bg-slate-800/30 rounded-xl p-4">
        <h4 className="text-lg font-semibold text-white mb-4 flex items-center">
          <Clock className="w-5 h-5 mr-2 text-blue-400" />
          Session Management
        </h4>
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <span className="text-slate-300">Auto Logout</span>
            <label className="relative inline-flex items-center cursor-pointer">
              <input
                type="checkbox"
                checked={preferences.trading.autoLogout.enabled}
                onChange={(e) => updatePreference(['trading', 'autoLogout', 'enabled'], e.target.checked)}
                className="sr-only peer"
              />
              <div className="w-11 h-6 bg-slate-600 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-blue-600"></div>
            </label>
          </div>
          
          {preferences.trading.autoLogout.enabled && (
            <div>
              <label className="block text-sm font-medium text-slate-300 mb-2">Timeout (minutes)</label>
              <select
                value={preferences.trading.autoLogout.timeoutMinutes}
                onChange={(e) => updatePreference(['trading', 'autoLogout', 'timeoutMinutes'], parseInt(e.target.value))}
                className="w-full bg-slate-700 border border-slate-600 text-white rounded-lg px-3 py-2"
              >
                <option value={15}>15 minutes</option>
                <option value={30}>30 minutes</option>
                <option value={60}>1 hour</option>
                <option value={120}>2 hours</option>
                <option value={240}>4 hours</option>
              </select>
            </div>
          )}
        </div>
      </div>
    </div>
  );

  const renderPrivacySection = () => (
    <div className="space-y-6">
      <div className="bg-slate-800/30 rounded-xl p-4">
        <h4 className="text-lg font-semibold text-white mb-4">Data Sharing</h4>
        <div className="space-y-3">
          {Object.entries(preferences.privacy.dataSharing).map(([key, value]) => (
            <div key={key} className="flex items-center justify-between">
              <div>
                <span className="text-slate-300 capitalize">{key.replace(/([A-Z])/g, ' $1')}</span>
                <p className="text-sm text-slate-400">
                  {key === 'analytics' && 'Help improve the platform with usage analytics'}
                  {key === 'marketing' && 'Receive personalized offers and recommendations'}
                  {key === 'performance' && 'Share anonymous performance data for benchmarking'}
                </p>
              </div>
              <label className="relative inline-flex items-center cursor-pointer">
                <input
                  type="checkbox"
                  checked={value}
                  onChange={(e) => updatePreference(['privacy', 'dataSharing', key], e.target.checked)}
                  className="sr-only peer"
                />
                <div className="w-11 h-6 bg-slate-600 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-green-600"></div>
              </label>
            </div>
          ))}
        </div>
      </div>

      <div className="bg-slate-800/30 rounded-xl p-4">
        <h4 className="text-lg font-semibold text-white mb-4">Profile Visibility</h4>
        <div className="space-y-4">
          {Object.entries(preferences.privacy.visibility).map(([key, value]) => (
            <div key={key}>
              <label className="block text-sm font-medium text-slate-300 mb-2 capitalize">
                {key.replace(/([A-Z])/g, ' $1')}
              </label>
              <select
                value={value}
                onChange={(e) => updatePreference(['privacy', 'visibility', key], e.target.value)}
                className="w-full bg-slate-700 border border-slate-600 text-white rounded-lg px-3 py-2"
              >
                <option value="public">Public</option>
                <option value="friends">Friends Only</option>
                <option value="private">Private</option>
              </select>
            </div>
          ))}
        </div>
      </div>
    </div>
  );

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h3 className="text-2xl font-bold text-white mb-2">User Preferences</h3>
          <p className="text-slate-400">Customize your TradeMaster experience</p>
        </div>
        
        <div className="flex items-center space-x-3">
          {hasChanges && (
            <button
              onClick={handleReset}
              className="flex items-center space-x-2 px-4 py-2 text-slate-400 hover:text-white border border-slate-600 rounded-lg hover:border-slate-500 transition-colors"
            >
              <RotateCcw className="w-4 h-4" />
              <span>Reset</span>
            </button>
          )}
          
          <button
            onClick={handleSave}
            disabled={!hasChanges || saving}
            className="flex items-center space-x-2 px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            <Save className="w-4 h-4" />
            <span>{saving ? 'Saving...' : 'Save Changes'}</span>
          </button>
        </div>
      </div>

      {/* Section Tabs */}
      <div className="flex space-x-1 bg-slate-800/30 rounded-xl p-1">
        {sections.map(section => {
          const Icon = section.icon;
          return (
            <button
              key={section.id}
              onClick={() => setActiveSection(section.id)}
              className={`flex items-center space-x-2 px-4 py-2 rounded-lg font-medium transition-all ${
                activeSection === section.id
                  ? 'bg-blue-600 text-white'
                  : 'text-slate-400 hover:text-white hover:bg-slate-700/50'
              }`}
            >
              <Icon className="w-4 h-4" />
              <span className="hidden md:block">{section.label}</span>
            </button>
          );
        })}
      </div>

      {/* Section Content */}
      <div className="min-h-[600px]">
        {activeSection === 'display' && renderDisplaySection()}
        {activeSection === 'notifications' && renderNotificationsSection()}
        {activeSection === 'trading' && renderTradingSection()}
        {activeSection === 'privacy' && renderPrivacySection()}
      </div>

      {/* Changes Indicator */}
      {hasChanges && (
        <div className="fixed bottom-6 right-6 bg-orange-500 text-white px-4 py-2 rounded-lg shadow-lg flex items-center space-x-2">
          <AlertTriangle className="w-4 h-4" />
          <span className="text-sm">You have unsaved changes</span>
        </div>
      )}
    </div>
  );
};

export default UserPreferences;