import React, { useEffect, useState, useCallback, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { 
  Keyboard, 
  Settings, 
  X, 
  Plus, 
  Edit3, 
  Trash2,
  Save,
  RotateCcw,
  Zap,
  AlertTriangle,
  Command,
  ArrowUp,
  ArrowDown,
  Escape,
  Space
} from 'lucide-react';

interface HotkeyAction {
  id: string;
  name: string;
  description: string;
  category: 'trading' | 'navigation' | 'analysis' | 'workspace' | 'orders';
  keys: string[];
  action: () => void;
  isEnabled: boolean;
  requiresConfirmation: boolean;
}

interface HotkeyConfig {
  id: string;
  name: string;
  keys: string[];
  category: string;
  requiresConfirmation: boolean;
  isEnabled: boolean;
}

interface QuickOrderConfig {
  symbol: string;
  quantity: number;
  orderType: 'market' | 'limit' | 'stop';
  side: 'buy' | 'sell';
  price?: number;
}

const TradingHotkeys: React.FC = () => {
  const [hotkeys, setHotkeys] = useState<HotkeyAction[]>([]);
  const [showSettings, setShowSettings] = useState(false);
  const [showHelp, setShowHelp] = useState(false);
  const [recordingKey, setRecordingKey] = useState<string | null>(null);
  const [pressedKeys, setPressedKeys] = useState<Set<string>>(new Set());
  const [lastAction, setLastAction] = useState<string | null>(null);
  const [quickOrderMode, setQuickOrderMode] = useState(false);
  const [quickOrder, setQuickOrder] = useState<QuickOrderConfig>({
    symbol: 'AAPL',
    quantity: 100,
    orderType: 'market',
    side: 'buy'
  });
  const [confirmationPending, setConfirmationPending] = useState<string | null>(null);
  const [hotkeyStats, setHotkeyStats] = useState<Record<string, number>>({});
  const hotkeyTimeouts = useRef<Map<string, NodeJS.Timeout>>(new Map());
  const lastPressTime = useRef<Map<string, number>>(new Map());

  // Default hotkey configurations
  const defaultHotkeys: HotkeyConfig[] = [
    // Trading Actions
    { id: 'quick-buy', name: 'Quick Buy Market', keys: ['b'], category: 'trading', requiresConfirmation: true, isEnabled: true },
    { id: 'quick-sell', name: 'Quick Sell Market', keys: ['s'], category: 'trading', requiresConfirmation: true, isEnabled: true },
    { id: 'quick-buy-limit', name: 'Quick Buy Limit', keys: ['shift', 'b'], category: 'trading', requiresConfirmation: false, isEnabled: true },
    { id: 'quick-sell-limit', name: 'Quick Sell Limit', keys: ['shift', 's'], category: 'trading', requiresConfirmation: false, isEnabled: true },
    { id: 'cancel-all-orders', name: 'Cancel All Orders', keys: ['ctrl', 'shift', 'c'], category: 'orders', requiresConfirmation: true, isEnabled: true },
    { id: 'flatten-position', name: 'Flatten Position', keys: ['f'], category: 'trading', requiresConfirmation: true, isEnabled: true },
    { id: 'reverse-position', name: 'Reverse Position', keys: ['r'], category: 'trading', requiresConfirmation: true, isEnabled: true },
    
    // Order Management
    { id: 'increase-quantity', name: 'Increase Quantity', keys: ['arrowup'], category: 'orders', requiresConfirmation: false, isEnabled: true },
    { id: 'decrease-quantity', name: 'Decrease Quantity', keys: ['arrowdown'], category: 'orders', requiresConfirmation: false, isEnabled: true },
    { id: 'switch-order-type', name: 'Switch Order Type', keys: ['t'], category: 'orders', requiresConfirmation: false, isEnabled: true },
    { id: 'toggle-tif', name: 'Toggle Time In Force', keys: ['shift', 't'], category: 'orders', requiresConfirmation: false, isEnabled: true },
    
    // Navigation
    { id: 'focus-symbol-search', name: 'Focus Symbol Search', keys: ['ctrl', 'k'], category: 'navigation', requiresConfirmation: false, isEnabled: true },
    { id: 'switch-workspace', name: 'Switch Workspace', keys: ['ctrl', 'tab'], category: 'workspace', requiresConfirmation: false, isEnabled: true },
    { id: 'toggle-orders-panel', name: 'Toggle Orders Panel', keys: ['o'], category: 'workspace', requiresConfirmation: false, isEnabled: true },
    { id: 'toggle-positions-panel', name: 'Toggle Positions Panel', keys: ['p'], category: 'workspace', requiresConfirmation: false, isEnabled: true },
    { id: 'toggle-watchlist', name: 'Toggle Watchlist', keys: ['w'], category: 'workspace', requiresConfirmation: false, isEnabled: true },
    
    // Analysis
    { id: 'next-timeframe', name: 'Next Timeframe', keys: [']'], category: 'analysis', requiresConfirmation: false, isEnabled: true },
    { id: 'prev-timeframe', name: 'Previous Timeframe', keys: ['['], category: 'analysis', requiresConfirmation: false, isEnabled: true },
    { id: 'toggle-chart-type', name: 'Toggle Chart Type', keys: ['c'], category: 'analysis', requiresConfirmation: false, isEnabled: true },
    { id: 'zoom-in', name: 'Zoom In Chart', keys: ['='], category: 'analysis', requiresConfirmation: false, isEnabled: true },
    { id: 'zoom-out', name: 'Zoom Out Chart', keys: ['-'], category: 'analysis', requiresConfirmation: false, isEnabled: true },
    
    // System
    { id: 'emergency-stop', name: 'Emergency Stop All', keys: ['ctrl', 'shift', 'x'], category: 'trading', requiresConfirmation: true, isEnabled: true },
    { id: 'show-hotkeys', name: 'Show Hotkey Help', keys: ['?'], category: 'workspace', requiresConfirmation: false, isEnabled: true },
    { id: 'quick-order-mode', name: 'Quick Order Mode', keys: ['q'], category: 'trading', requiresConfirmation: false, isEnabled: true }
  ];

  // Initialize hotkeys with actions
  useEffect(() => {
    const actions: HotkeyAction[] = defaultHotkeys.map(config => ({
      id: config.id,
      name: config.name,
      description: getActionDescription(config.id),
      category: config.category as any,
      keys: config.keys,
      action: getActionFunction(config.id),
      isEnabled: config.isEnabled,
      requiresConfirmation: config.requiresConfirmation
    }));
    
    setHotkeys(actions);
  }, []);

  const getActionDescription = (actionId: string): string => {
    const descriptions: Record<string, string> = {
      'quick-buy': 'Execute market buy order with preset quantity',
      'quick-sell': 'Execute market sell order with preset quantity',
      'quick-buy-limit': 'Place limit buy order at current bid',
      'quick-sell-limit': 'Place limit sell order at current ask',
      'cancel-all-orders': 'Cancel all pending orders for current symbol',
      'flatten-position': 'Close entire position at market price',
      'reverse-position': 'Close current position and open opposite position',
      'increase-quantity': 'Increase order quantity by preset amount',
      'decrease-quantity': 'Decrease order quantity by preset amount',
      'switch-order-type': 'Cycle through order types (Market/Limit/Stop)',
      'toggle-tif': 'Toggle Time In Force (GTC/IOC/FOK/DAY)',
      'focus-symbol-search': 'Focus cursor on symbol search input',
      'switch-workspace': 'Switch to next workspace layout',
      'toggle-orders-panel': 'Show/hide orders panel',
      'toggle-positions-panel': 'Show/hide positions panel',
      'toggle-watchlist': 'Show/hide watchlist panel',
      'next-timeframe': 'Switch to next chart timeframe',
      'prev-timeframe': 'Switch to previous chart timeframe',
      'toggle-chart-type': 'Cycle chart types (Candlestick/Line/Bar)',
      'zoom-in': 'Zoom in on chart',
      'zoom-out': 'Zoom out on chart',
      'emergency-stop': 'Stop all trading activity and cancel orders',
      'show-hotkeys': 'Display hotkey reference guide',
      'quick-order-mode': 'Enter quick order placement mode'
    };
    return descriptions[actionId] || 'No description available';
  };

  const getActionFunction = (actionId: string) => {
    return () => {
      const actions: Record<string, () => void> = {
        'quick-buy': () => executeQuickOrder('buy', 'market'),
        'quick-sell': () => executeQuickOrder('sell', 'market'),
        'quick-buy-limit': () => executeQuickOrder('buy', 'limit'),
        'quick-sell-limit': () => executeQuickOrder('sell', 'limit'),
        'cancel-all-orders': () => cancelAllOrders(),
        'flatten-position': () => flattenPosition(),
        'reverse-position': () => reversePosition(),
        'increase-quantity': () => adjustQuantity(100),
        'decrease-quantity': () => adjustQuantity(-100),
        'switch-order-type': () => switchOrderType(),
        'toggle-tif': () => toggleTimeInForce(),
        'focus-symbol-search': () => focusSymbolSearch(),
        'switch-workspace': () => switchWorkspace(),
        'toggle-orders-panel': () => togglePanel('orders'),
        'toggle-positions-panel': () => togglePanel('positions'),
        'toggle-watchlist': () => togglePanel('watchlist'),
        'next-timeframe': () => switchTimeframe(1),
        'prev-timeframe': () => switchTimeframe(-1),
        'toggle-chart-type': () => toggleChartType(),
        'zoom-in': () => adjustChartZoom(1),
        'zoom-out': () => adjustChartZoom(-1),
        'emergency-stop': () => emergencyStop(),
        'show-hotkeys': () => setShowHelp(true),
        'quick-order-mode': () => setQuickOrderMode(!quickOrderMode)
      };
      
      const action = actions[actionId];
      if (action) {
        updateHotkeyStats(actionId);
        setLastAction(actionId);
        action();
      }
    };
  };

  const updateHotkeyStats = (actionId: string) => {
    setHotkeyStats(prev => ({
      ...prev,
      [actionId]: (prev[actionId] || 0) + 1
    }));
  };

  // Trading action implementations
  const executeQuickOrder = (side: 'buy' | 'sell', type: 'market' | 'limit') => {
    console.log(`Executing ${type} ${side} order:`, {
      symbol: quickOrder.symbol,
      quantity: quickOrder.quantity,
      side,
      type,
      price: type === 'limit' ? (side === 'buy' ? 150 : 155) : undefined
    });
    
    // Mock order execution
    setTimeout(() => {
      console.log(`${type} ${side} order executed successfully`);
    }, 100);
  };

  const cancelAllOrders = () => {
    console.log('Cancelling all orders for symbol:', quickOrder.symbol);
  };

  const flattenPosition = () => {
    console.log('Flattening position for:', quickOrder.symbol);
  };

  const reversePosition = () => {
    console.log('Reversing position for:', quickOrder.symbol);
  };

  const adjustQuantity = (amount: number) => {
    setQuickOrder(prev => ({
      ...prev,
      quantity: Math.max(1, prev.quantity + amount)
    }));
  };

  const switchOrderType = () => {
    const types: ('market' | 'limit' | 'stop')[] = ['market', 'limit', 'stop'];
    const currentIndex = types.indexOf(quickOrder.orderType);
    const nextIndex = (currentIndex + 1) % types.length;
    setQuickOrder(prev => ({ ...prev, orderType: types[nextIndex] }));
  };

  const toggleTimeInForce = () => {
    console.log('Toggling Time In Force');
  };

  const focusSymbolSearch = () => {
    const symbolInput = document.querySelector('input[placeholder*="symbol" i], input[placeholder*="search" i]') as HTMLInputElement;
    if (symbolInput) {
      symbolInput.focus();
      symbolInput.select();
    }
  };

  const switchWorkspace = () => {
    console.log('Switching workspace');
  };

  const togglePanel = (panel: string) => {
    console.log('Toggling panel:', panel);
  };

  const switchTimeframe = (direction: number) => {
    console.log('Switching timeframe:', direction > 0 ? 'next' : 'previous');
  };

  const toggleChartType = () => {
    console.log('Toggling chart type');
  };

  const adjustChartZoom = (direction: number) => {
    console.log('Adjusting chart zoom:', direction > 0 ? 'in' : 'out');
  };

  const emergencyStop = () => {
    console.log('EMERGENCY STOP - Cancelling all orders and closing positions');
    cancelAllOrders();
    flattenPosition();
  };

  // Keyboard event handling
  const handleKeyDown = useCallback((event: KeyboardEvent) => {
    if (event.target && ['input', 'textarea', 'select'].includes((event.target as HTMLElement).tagName.toLowerCase())) {
      return;
    }

    const key = normalizeKey(event.key);
    const modifiers = getModifiers(event);
    const keyCombo = [...modifiers, key].filter(Boolean);

    // Update pressed keys
    setPressedKeys(prev => new Set([...prev, key]));

    // Find matching hotkey
    const matchingHotkey = hotkeys.find(hotkey => 
      hotkey.isEnabled && 
      arraysEqual(hotkey.keys.map(k => k.toLowerCase()), keyCombo.map(k => k.toLowerCase()))
    );

    if (matchingHotkey) {
      event.preventDefault();
      event.stopPropagation();

      if (matchingHotkey.requiresConfirmation) {
        setConfirmationPending(matchingHotkey.id);
      } else {
        matchingHotkey.action();
      }
    }
  }, [hotkeys]);

  const handleKeyUp = useCallback((event: KeyboardEvent) => {
    const key = normalizeKey(event.key);
    setPressedKeys(prev => {
      const newSet = new Set(prev);
      newSet.delete(key);
      return newSet;
    });
  }, []);

  const normalizeKey = (key: string): string => {
    const keyMap: Record<string, string> = {
      'Control': 'ctrl',
      'Shift': 'shift',
      'Alt': 'alt',
      'Meta': 'cmd',
      'ArrowUp': 'arrowup',
      'ArrowDown': 'arrowdown',
      'ArrowLeft': 'arrowleft',
      'ArrowRight': 'arrowright',
      'Escape': 'escape',
      ' ': 'space'
    };
    return keyMap[key] || key.toLowerCase();
  };

  const getModifiers = (event: KeyboardEvent): string[] => {
    const modifiers: string[] = [];
    if (event.ctrlKey) modifiers.push('ctrl');
    if (event.shiftKey) modifiers.push('shift');
    if (event.altKey) modifiers.push('alt');
    if (event.metaKey) modifiers.push('cmd');
    return modifiers;
  };

  const arraysEqual = (a: string[], b: string[]): boolean => {
    return a.length === b.length && a.every(val => b.includes(val));
  };

  // Event listeners
  useEffect(() => {
    document.addEventListener('keydown', handleKeyDown);
    document.addEventListener('keyup', handleKeyUp);
    
    return () => {
      document.removeEventListener('keydown', handleKeyDown);
      document.removeEventListener('keyup', handleKeyUp);
    };
  }, [handleKeyDown, handleKeyUp]);

  const executeConfirmedAction = () => {
    if (confirmationPending) {
      const hotkey = hotkeys.find(h => h.id === confirmationPending);
      if (hotkey) {
        hotkey.action();
      }
      setConfirmationPending(null);
    }
  };

  const cancelConfirmation = () => {
    setConfirmationPending(null);
  };

  const formatKeys = (keys: string[]): string => {
    return keys.map(key => {
      const keyLabels: Record<string, string> = {
        'ctrl': '⌃',
        'shift': '⇧',
        'alt': '⌥',
        'cmd': '⌘',
        'arrowup': '↑',
        'arrowdown': '↓',
        'arrowleft': '←',
        'arrowright': '→',
        'space': 'Space',
        'escape': 'Esc'
      };
      return keyLabels[key.toLowerCase()] || key.toUpperCase();
    }).join(' + ');
  };

  const getCategoryIcon = (category: string) => {
    const icons: Record<string, React.ReactNode> = {
      trading: <Zap size={16} className="text-green-400" />,
      orders: <Edit3 size={16} className="text-blue-400" />,
      navigation: <Command size={16} className="text-purple-400" />,
      analysis: <ArrowUp size={16} className="text-orange-400" />,
      workspace: <Settings size={16} className="text-gray-400" />
    };
    return icons[category] || <Settings size={16} className="text-gray-400" />;
  };

  return (
    <>
      {/* Quick Order Mode Overlay */}
      <AnimatePresence>
        {quickOrderMode && (
          <motion.div
            className="fixed top-20 left-1/2 transform -translate-x-1/2 z-50 bg-black/90 backdrop-blur-sm
              border border-blue-500 rounded-lg p-4 min-w-96"
            initial={{ opacity: 0, y: -20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -20 }}
          >
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center space-x-2">
                <Zap size={18} className="text-blue-400" />
                <span className="font-semibold text-blue-400">Quick Order Mode</span>
              </div>
              <button onClick={() => setQuickOrderMode(false)} className="text-gray-400 hover:text-white">
                <X size={16} />
              </button>
            </div>
            
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="text-sm text-gray-300 block mb-1">Symbol</label>
                <input
                  type="text"
                  value={quickOrder.symbol}
                  onChange={(e) => setQuickOrder(prev => ({ ...prev, symbol: e.target.value }))}
                  className="w-full bg-gray-800 border border-gray-600 rounded px-3 py-2 text-sm"
                />
              </div>
              <div>
                <label className="text-sm text-gray-300 block mb-1">Quantity</label>
                <input
                  type="number"
                  value={quickOrder.quantity}
                  onChange={(e) => setQuickOrder(prev => ({ ...prev, quantity: parseInt(e.target.value) || 1 }))}
                  className="w-full bg-gray-800 border border-gray-600 rounded px-3 py-2 text-sm"
                />
              </div>
            </div>
            
            <div className="mt-3 text-xs text-gray-400">
              Press <kbd className="bg-gray-700 px-1 rounded">B</kbd> to buy, <kbd className="bg-gray-700 px-1 rounded">S</kbd> to sell, 
              or <kbd className="bg-gray-700 px-1 rounded">Esc</kbd> to exit
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Confirmation Dialog */}
      <AnimatePresence>
        {confirmationPending && (
          <motion.div
            className="fixed inset-0 z-50 bg-black/50 flex items-center justify-center"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
          >
            <motion.div
              className="bg-gray-900 rounded-lg p-6 max-w-md w-full mx-4 border border-red-500"
              initial={{ scale: 0.9 }}
              animate={{ scale: 1 }}
              exit={{ scale: 0.9 }}
            >
              <div className="flex items-center space-x-3 mb-4">
                <AlertTriangle size={24} className="text-red-400" />
                <h3 className="text-lg font-semibold">Confirm Action</h3>
              </div>
              
              <p className="text-gray-300 mb-6">
                Are you sure you want to execute: <strong>
                  {hotkeys.find(h => h.id === confirmationPending)?.name}
                </strong>?
              </p>
              
              <div className="flex space-x-3">
                <button
                  onClick={executeConfirmedAction}
                  className="flex-1 bg-red-600 hover:bg-red-700 text-white px-4 py-2 rounded transition-colors"
                >
                  Execute
                </button>
                <button
                  onClick={cancelConfirmation}
                  className="flex-1 bg-gray-600 hover:bg-gray-700 text-white px-4 py-2 rounded transition-colors"
                >
                  Cancel
                </button>
              </div>
              
              <div className="mt-3 text-xs text-gray-400 text-center">
                Press <kbd className="bg-gray-700 px-1 rounded">Enter</kbd> to execute, 
                <kbd className="bg-gray-700 px-1 rounded">Esc</kbd> to cancel
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Help Modal */}
      <AnimatePresence>
        {showHelp && (
          <motion.div
            className="fixed inset-0 z-50 bg-black/50 flex items-center justify-center"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
          >
            <motion.div
              className="bg-gray-900 rounded-lg p-6 max-w-4xl w-full max-h-[80vh] mx-4 border border-gray-700 overflow-hidden"
              initial={{ scale: 0.9 }}
              animate={{ scale: 1 }}
              exit={{ scale: 0.9 }}
            >
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-lg font-semibold flex items-center space-x-2">
                  <Keyboard size={20} />
                  <span>Keyboard Shortcuts</span>
                </h3>
                <button
                  onClick={() => setShowHelp(false)}
                  className="p-2 hover:bg-gray-800 rounded"
                >
                  <X size={16} />
                </button>
              </div>
              
              <div className="overflow-y-auto max-h-96">
                {Object.entries(
                  hotkeys.reduce((acc, hotkey) => {
                    if (!acc[hotkey.category]) acc[hotkey.category] = [];
                    acc[hotkey.category].push(hotkey);
                    return acc;
                  }, {} as Record<string, HotkeyAction[]>)
                ).map(([category, categoryHotkeys]) => (
                  <div key={category} className="mb-6">
                    <h4 className="text-sm font-medium text-gray-300 uppercase tracking-wide mb-3 flex items-center space-x-2">
                      {getCategoryIcon(category)}
                      <span>{category}</span>
                    </h4>
                    <div className="space-y-2">
                      {categoryHotkeys.map(hotkey => (
                        <div key={hotkey.id} className="flex items-center justify-between py-2 px-3 bg-gray-800/50 rounded">
                          <div className="flex-1">
                            <div className="font-medium text-sm">{hotkey.name}</div>
                            <div className="text-xs text-gray-400">{hotkey.description}</div>
                          </div>
                          <div className="flex items-center space-x-2">
                            {hotkey.requiresConfirmation && (
                              <span className="text-xs bg-red-500/20 text-red-400 px-2 py-1 rounded">
                                Confirm
                              </span>
                            )}
                            <kbd className="bg-gray-700 px-2 py-1 rounded text-xs font-mono">
                              {formatKeys(hotkey.keys)}
                            </kbd>
                            {hotkeyStats[hotkey.id] && (
                              <span className="text-xs text-gray-500">
                                {hotkeyStats[hotkey.id]}×
                              </span>
                            )}
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                ))}
              </div>
              
              <div className="mt-4 pt-4 border-t border-gray-700 flex items-center justify-between">
                <div className="text-sm text-gray-400">
                  Press <kbd className="bg-gray-700 px-1 rounded">?</kbd> anytime to show this help
                </div>
                <button
                  onClick={() => setShowSettings(true)}
                  className="flex items-center space-x-2 px-3 py-1.5 bg-gray-700 hover:bg-gray-600 rounded text-sm"
                >
                  <Settings size={14} />
                  <span>Customize</span>
                </button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Last Action Indicator */}
      <AnimatePresence>
        {lastAction && (
          <motion.div
            className="fixed top-4 right-4 z-40 bg-green-600 text-white px-3 py-2 rounded shadow-lg"
            initial={{ opacity: 0, x: 100 }}
            animate={{ opacity: 1, x: 0 }}
            exit={{ opacity: 0, x: 100 }}
            onAnimationComplete={() => {
              setTimeout(() => setLastAction(null), 2000);
            }}
          >
            <div className="flex items-center space-x-2">
              <Zap size={16} />
              <span className="text-sm font-medium">
                {hotkeys.find(h => h.id === lastAction)?.name}
              </span>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Pressed Keys Indicator */}
      {pressedKeys.size > 0 && (
        <motion.div
          className="fixed bottom-4 left-4 z-40 bg-black/80 backdrop-blur-sm border border-gray-600 
            rounded px-3 py-2"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
        >
          <div className="flex items-center space-x-2">
            <Keyboard size={16} className="text-blue-400" />
            <span className="text-sm font-mono">
              {Array.from(pressedKeys).map(key => formatKeys([key])).join(' + ')}
            </span>
          </div>
        </motion.div>
      )}
    </>
  );
};

export default TradingHotkeys;