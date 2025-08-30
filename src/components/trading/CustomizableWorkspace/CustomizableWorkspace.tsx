import React, { useState, useRef, useCallback, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { 
  Monitor,
  Plus,
  Settings,
  Save,
  FolderOpen,
  Grid3x3,
  Maximize2,
  Minimize2,
  X,
  MoreVertical,
  Eye,
  EyeOff,
  Copy,
  Move,
  Trash2,
  Layout,
  Split,
  Columns2,
  Rows2
} from 'lucide-react';

interface WorkspaceWidget {
  id: string;
  type: 'chart' | 'watchlist' | 'orderbook' | 'trades' | 'orders' | 'positions' | 'news' | 'calculator' | 'analysis';
  title: string;
  position: { x: number; y: number };
  size: { width: number; height: number };
  isVisible: boolean;
  isMinimized: boolean;
  config?: any;
}

interface WorkspaceLayout {
  id: string;
  name: string;
  widgets: WorkspaceWidget[];
  createdAt: Date;
  lastModified: Date;
  isDefault?: boolean;
}

interface WorkspaceTheme {
  id: string;
  name: string;
  colors: {
    primary: string;
    secondary: string;
    accent: string;
    background: string;
    surface: string;
    text: string;
    border: string;
  };
  chartStyle: {
    candleUp: string;
    candleDown: string;
    volume: string;
    grid: string;
  };
}

const defaultThemes: WorkspaceTheme[] = [
  {
    id: 'dark-professional',
    name: 'Dark Professional',
    colors: {
      primary: '#1a1a1a',
      secondary: '#2d2d2d',
      accent: '#00d4ff',
      background: '#0f0f0f',
      surface: '#1e1e1e',
      text: '#ffffff',
      border: '#333333'
    },
    chartStyle: {
      candleUp: '#00ff88',
      candleDown: '#ff4444',
      volume: '#666666',
      grid: '#2a2a2a'
    }
  },
  {
    id: 'light-clean',
    name: 'Light Clean',
    colors: {
      primary: '#ffffff',
      secondary: '#f5f5f5',
      accent: '#0066cc',
      background: '#fafafa',
      surface: '#ffffff',
      text: '#333333',
      border: '#e0e0e0'
    },
    chartStyle: {
      candleUp: '#00aa44',
      candleDown: '#cc3333',
      volume: '#999999',
      grid: '#f0f0f0'
    }
  }
];

const widgetTemplates: Omit<WorkspaceWidget, 'id' | 'position'>[] = [
  {
    type: 'chart',
    title: 'Price Chart',
    size: { width: 600, height: 400 },
    isVisible: true,
    isMinimized: false,
    config: { timeframe: '1D', indicators: ['SMA', 'RSI'] }
  },
  {
    type: 'watchlist',
    title: 'Watchlist',
    size: { width: 300, height: 500 },
    isVisible: true,
    isMinimized: false,
    config: { symbols: ['AAPL', 'GOOGL', 'TSLA', 'MSFT'] }
  },
  {
    type: 'orderbook',
    title: 'Order Book',
    size: { width: 250, height: 400 },
    isVisible: true,
    isMinimized: false,
    config: { depth: 20 }
  },
  {
    type: 'trades',
    title: 'Recent Trades',
    size: { width: 300, height: 300 },
    isVisible: true,
    isMinimized: false
  },
  {
    type: 'orders',
    title: 'Open Orders',
    size: { width: 400, height: 250 },
    isVisible: true,
    isMinimized: false
  },
  {
    type: 'positions',
    title: 'Positions',
    size: { width: 400, height: 200 },
    isVisible: true,
    isMinimized: false
  },
  {
    type: 'news',
    title: 'Market News',
    size: { width: 350, height: 300 },
    isVisible: true,
    isMinimized: false
  },
  {
    type: 'calculator',
    title: 'Position Calculator',
    size: { width: 300, height: 250 },
    isVisible: true,
    isMinimized: false
  },
  {
    type: 'analysis',
    title: 'Technical Analysis',
    size: { width: 400, height: 350 },
    isVisible: true,
    isMinimized: false
  }
];

const CustomizableWorkspace: React.FC = () => {
  const [currentLayout, setCurrentLayout] = useState<WorkspaceLayout | null>(null);
  const [savedLayouts, setSavedLayouts] = useState<WorkspaceLayout[]>([]);
  const [selectedTheme, setSelectedTheme] = useState<WorkspaceTheme>(defaultThemes[0]);
  const [isDragging, setIsDragging] = useState<string | null>(null);
  const [dragOffset, setDragOffset] = useState({ x: 0, y: 0 });
  const [isResizing, setIsResizing] = useState<string | null>(null);
  const [showLayoutManager, setShowLayoutManager] = useState(false);
  const [showWidgetPalette, setShowWidgetPalette] = useState(false);
  const [showThemeSelector, setShowThemeSelector] = useState(false);
  const [workspaceMode, setWorkspaceMode] = useState<'single' | 'split-h' | 'split-v' | 'grid'>('single');
  const workspaceRef = useRef<HTMLDivElement>(null);

  // Initialize with default layout
  useEffect(() => {
    const defaultLayout: WorkspaceLayout = {
      id: 'default',
      name: 'Default Layout',
      widgets: [
        {
          ...widgetTemplates[0],
          id: 'chart-1',
          position: { x: 20, y: 20 }
        },
        {
          ...widgetTemplates[1],
          id: 'watchlist-1',
          position: { x: 650, y: 20 }
        },
        {
          ...widgetTemplates[2],
          id: 'orderbook-1',
          position: { x: 970, y: 20 }
        },
        {
          ...widgetTemplates[4],
          id: 'orders-1',
          position: { x: 20, y: 450 }
        }
      ],
      createdAt: new Date(),
      lastModified: new Date(),
      isDefault: true
    };
    setCurrentLayout(defaultLayout);
    setSavedLayouts([defaultLayout]);
  }, []);

  const handleMouseDown = useCallback((e: React.MouseEvent, widgetId: string, action: 'drag' | 'resize') => {
    e.preventDefault();
    if (!currentLayout) return;

    const widget = currentLayout.widgets.find(w => w.id === widgetId);
    if (!widget) return;

    if (action === 'drag') {
      setIsDragging(widgetId);
      const rect = (e.target as HTMLElement).closest('.widget')?.getBoundingClientRect();
      if (rect) {
        setDragOffset({
          x: e.clientX - rect.left,
          y: e.clientY - rect.top
        });
      }
    } else if (action === 'resize') {
      setIsResizing(widgetId);
    }
  }, [currentLayout]);

  const handleMouseMove = useCallback((e: MouseEvent) => {
    if (!currentLayout || (!isDragging && !isResizing)) return;

    const workspaceRect = workspaceRef.current?.getBoundingClientRect();
    if (!workspaceRect) return;

    const newWidgets = [...currentLayout.widgets];

    if (isDragging) {
      const widgetIndex = newWidgets.findIndex(w => w.id === isDragging);
      if (widgetIndex >= 0) {
        newWidgets[widgetIndex] = {
          ...newWidgets[widgetIndex],
          position: {
            x: Math.max(0, Math.min(workspaceRect.width - newWidgets[widgetIndex].size.width, 
                e.clientX - workspaceRect.left - dragOffset.x)),
            y: Math.max(0, Math.min(workspaceRect.height - newWidgets[widgetIndex].size.height, 
                e.clientY - workspaceRect.top - dragOffset.y))
          }
        };
      }
    } else if (isResizing) {
      const widgetIndex = newWidgets.findIndex(w => w.id === isResizing);
      if (widgetIndex >= 0) {
        const widget = newWidgets[widgetIndex];
        newWidgets[widgetIndex] = {
          ...widget,
          size: {
            width: Math.max(200, e.clientX - workspaceRect.left - widget.position.x),
            height: Math.max(150, e.clientY - workspaceRect.top - widget.position.y)
          }
        };
      }
    }

    setCurrentLayout({
      ...currentLayout,
      widgets: newWidgets,
      lastModified: new Date()
    });
  }, [currentLayout, isDragging, isResizing, dragOffset]);

  const handleMouseUp = useCallback(() => {
    setIsDragging(null);
    setIsResizing(null);
    setDragOffset({ x: 0, y: 0 });
  }, []);

  useEffect(() => {
    if (isDragging || isResizing) {
      document.addEventListener('mousemove', handleMouseMove);
      document.addEventListener('mouseup', handleMouseUp);
      return () => {
        document.removeEventListener('mousemove', handleMouseMove);
        document.removeEventListener('mouseup', handleMouseUp);
      };
    }
  }, [isDragging, isResizing, handleMouseMove, handleMouseUp]);

  const addWidget = (template: Omit<WorkspaceWidget, 'id' | 'position'>) => {
    if (!currentLayout) return;

    const newWidget: WorkspaceWidget = {
      ...template,
      id: `${template.type}-${Date.now()}`,
      position: { x: 50 + (currentLayout.widgets.length * 30), y: 50 + (currentLayout.widgets.length * 30) }
    };

    setCurrentLayout({
      ...currentLayout,
      widgets: [...currentLayout.widgets, newWidget],
      lastModified: new Date()
    });
  };

  const removeWidget = (widgetId: string) => {
    if (!currentLayout) return;

    setCurrentLayout({
      ...currentLayout,
      widgets: currentLayout.widgets.filter(w => w.id !== widgetId),
      lastModified: new Date()
    });
  };

  const toggleWidgetVisibility = (widgetId: string) => {
    if (!currentLayout) return;

    const newWidgets = currentLayout.widgets.map(widget =>
      widget.id === widgetId
        ? { ...widget, isVisible: !widget.isVisible }
        : widget
    );

    setCurrentLayout({
      ...currentLayout,
      widgets: newWidgets,
      lastModified: new Date()
    });
  };

  const minimizeWidget = (widgetId: string) => {
    if (!currentLayout) return;

    const newWidgets = currentLayout.widgets.map(widget =>
      widget.id === widgetId
        ? { ...widget, isMinimized: !widget.isMinimized }
        : widget
    );

    setCurrentLayout({
      ...currentLayout,
      widgets: newWidgets,
      lastModified: new Date()
    });
  };

  const saveLayout = (name: string) => {
    if (!currentLayout) return;

    const newLayout: WorkspaceLayout = {
      ...currentLayout,
      id: `layout-${Date.now()}`,
      name,
      lastModified: new Date()
    };

    setSavedLayouts(prev => [...prev, newLayout]);
    setCurrentLayout(newLayout);
  };

  const loadLayout = (layout: WorkspaceLayout) => {
    setCurrentLayout(layout);
  };

  const deleteLayout = (layoutId: string) => {
    setSavedLayouts(prev => prev.filter(l => l.id !== layoutId));
    if (currentLayout?.id === layoutId) {
      const defaultLayout = savedLayouts.find(l => l.isDefault);
      setCurrentLayout(defaultLayout || null);
    }
  };

  const applyWorkspaceLayout = (mode: typeof workspaceMode) => {
    if (!currentLayout) return;

    const workspaceRect = workspaceRef.current?.getBoundingClientRect();
    if (!workspaceRect) return;

    const { width, height } = workspaceRect;
    let newWidgets = [...currentLayout.widgets];

    switch (mode) {
      case 'split-h':
        // Split workspace horizontally
        newWidgets = newWidgets.map((widget, index) => ({
          ...widget,
          position: { 
            x: index % 2 === 0 ? 10 : width / 2 + 10, 
            y: Math.floor(index / 2) * (height / Math.ceil(newWidgets.length / 2)) + 10 
          },
          size: { 
            width: width / 2 - 30, 
            height: Math.min(widget.size.height, height / Math.ceil(newWidgets.length / 2) - 30) 
          }
        }));
        break;
      case 'split-v':
        // Split workspace vertically
        newWidgets = newWidgets.map((widget, index) => ({
          ...widget,
          position: { 
            x: index * (width / newWidgets.length) + 10, 
            y: 10 
          },
          size: { 
            width: width / newWidgets.length - 20, 
            height: height - 100 
          }
        }));
        break;
      case 'grid':
        // Grid layout
        const cols = Math.ceil(Math.sqrt(newWidgets.length));
        const rows = Math.ceil(newWidgets.length / cols);
        newWidgets = newWidgets.map((widget, index) => {
          const col = index % cols;
          const row = Math.floor(index / cols);
          return {
            ...widget,
            position: { 
              x: col * (width / cols) + 10, 
              y: row * (height / rows) + 10 
            },
            size: { 
              width: width / cols - 20, 
              height: height / rows - 20 
            }
          };
        });
        break;
    }

    setCurrentLayout({
      ...currentLayout,
      widgets: newWidgets,
      lastModified: new Date()
    });
    setWorkspaceMode(mode);
  };

  const renderWidget = (widget: WorkspaceWidget) => {
    if (!widget.isVisible) return null;

    return (
      <motion.div
        key={widget.id}
        className={`absolute bg-white/5 backdrop-blur-sm border border-white/10 rounded-lg 
          shadow-lg ${isDragging === widget.id ? 'z-50' : 'z-10'} widget`}
        style={{
          left: widget.position.x,
          top: widget.position.y,
          width: widget.size.width,
          height: widget.isMinimized ? 40 : widget.size.height,
          borderColor: selectedTheme.colors.border,
          backgroundColor: selectedTheme.colors.surface + '20'
        }}
        animate={{ scale: isDragging === widget.id ? 1.02 : 1 }}
        transition={{ duration: 0.1 }}
      >
        {/* Widget Header */}
        <div
          className="flex items-center justify-between p-2 border-b border-white/10 cursor-move bg-white/5"
          onMouseDown={(e) => handleMouseDown(e, widget.id, 'drag')}
          style={{ borderColor: selectedTheme.colors.border }}
        >
          <div className="flex items-center space-x-2">
            <div className="w-2 h-2 rounded-full bg-green-400"></div>
            <span className="text-sm font-medium" style={{ color: selectedTheme.colors.text }}>
              {widget.title}
            </span>
          </div>
          <div className="flex items-center space-x-1">
            <button
              onClick={() => minimizeWidget(widget.id)}
              className="p-1 rounded hover:bg-white/10"
            >
              {widget.isMinimized ? <Maximize2 size={12} /> : <Minimize2 size={12} />}
            </button>
            <button
              onClick={() => toggleWidgetVisibility(widget.id)}
              className="p-1 rounded hover:bg-white/10"
            >
              <EyeOff size={12} />
            </button>
            <button
              onClick={() => removeWidget(widget.id)}
              className="p-1 rounded hover:bg-red-500/20 text-red-400"
            >
              <X size={12} />
            </button>
          </div>
        </div>

        {/* Widget Content */}
        {!widget.isMinimized && (
          <div className="p-4 h-full overflow-auto">
            <div className="text-center text-gray-400 text-sm">
              {widget.type} widget content
            </div>
          </div>
        )}

        {/* Resize Handle */}
        {!widget.isMinimized && (
          <div
            className="absolute bottom-0 right-0 w-3 h-3 cursor-se-resize opacity-50 hover:opacity-100"
            onMouseDown={(e) => handleMouseDown(e, widget.id, 'resize')}
          >
            <div className="w-full h-full bg-white/30 rounded-tl-lg"></div>
          </div>
        )}
      </motion.div>
    );
  };

  return (
    <div className="h-full bg-gradient-to-br from-gray-900 to-black relative overflow-hidden"
         style={{ backgroundColor: selectedTheme.colors.background }}>
      
      {/* Toolbar */}
      <div className="absolute top-0 left-0 right-0 z-40 bg-black/50 backdrop-blur-sm border-b border-white/10 p-2">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <button
              onClick={() => setShowWidgetPalette(true)}
              className="flex items-center space-x-2 px-3 py-1.5 bg-blue-600 hover:bg-blue-700 
                rounded text-sm font-medium transition-colors"
            >
              <Plus size={16} />
              <span>Add Widget</span>
            </button>
            
            <div className="flex items-center space-x-1 bg-white/10 rounded p-1">
              <button
                onClick={() => applyWorkspaceLayout('single')}
                className={`p-1.5 rounded ${workspaceMode === 'single' ? 'bg-blue-600' : 'hover:bg-white/10'}`}
              >
                <Layout size={14} />
              </button>
              <button
                onClick={() => applyWorkspaceLayout('split-v')}
                className={`p-1.5 rounded ${workspaceMode === 'split-v' ? 'bg-blue-600' : 'hover:bg-white/10'}`}
              >
                <Columns2 size={14} />
              </button>
              <button
                onClick={() => applyWorkspaceLayout('split-h')}
                className={`p-1.5 rounded ${workspaceMode === 'split-h' ? 'bg-blue-600' : 'hover:bg-white/10'}`}
              >
                <Rows2 size={14} />
              </button>
              <button
                onClick={() => applyWorkspaceLayout('grid')}
                className={`p-1.5 rounded ${workspaceMode === 'grid' ? 'bg-blue-600' : 'hover:bg-white/10'}`}
              >
                <Grid3x3 size={14} />
              </button>
            </div>
          </div>

          <div className="flex items-center space-x-2">
            <button
              onClick={() => setShowThemeSelector(true)}
              className="flex items-center space-x-2 px-3 py-1.5 bg-gray-700 hover:bg-gray-600 
                rounded text-sm transition-colors"
            >
              <Settings size={16} />
              <span>Theme</span>
            </button>

            <button
              onClick={() => setShowLayoutManager(true)}
              className="flex items-center space-x-2 px-3 py-1.5 bg-gray-700 hover:bg-gray-600 
                rounded text-sm transition-colors"
            >
              <FolderOpen size={16} />
              <span>Layouts</span>
            </button>

            {currentLayout && (
              <button
                onClick={() => {
                  const name = prompt('Enter layout name:');
                  if (name) saveLayout(name);
                }}
                className="flex items-center space-x-2 px-3 py-1.5 bg-green-600 hover:bg-green-700 
                  rounded text-sm transition-colors"
              >
                <Save size={16} />
                <span>Save</span>
              </button>
            )}
          </div>
        </div>
      </div>

      {/* Workspace */}
      <div
        ref={workspaceRef}
        className="pt-16 w-full h-full relative"
        style={{ backgroundColor: selectedTheme.colors.background }}
      >
        {currentLayout?.widgets.map(renderWidget)}
      </div>

      {/* Widget Palette Modal */}
      <AnimatePresence>
        {showWidgetPalette && (
          <motion.div
            className="fixed inset-0 z-50 bg-black/50 flex items-center justify-center"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
          >
            <motion.div
              className="bg-gray-900 rounded-lg p-6 max-w-2xl w-full mx-4 border border-gray-700"
              initial={{ scale: 0.9 }}
              animate={{ scale: 1 }}
              exit={{ scale: 0.9 }}
            >
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-lg font-semibold">Add Widget</h3>
                <button
                  onClick={() => setShowWidgetPalette(false)}
                  className="p-2 hover:bg-gray-800 rounded"
                >
                  <X size={16} />
                </button>
              </div>
              
              <div className="grid grid-cols-3 gap-4">
                {widgetTemplates.map((template, index) => (
                  <button
                    key={index}
                    onClick={() => {
                      addWidget(template);
                      setShowWidgetPalette(false);
                    }}
                    className="p-4 bg-gray-800 hover:bg-gray-700 rounded-lg border border-gray-600 
                      transition-colors text-left"
                  >
                    <div className="font-medium mb-1">{template.title}</div>
                    <div className="text-sm text-gray-400">
                      {template.size.width} × {template.size.height}
                    </div>
                  </button>
                ))}
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Layout Manager Modal */}
      <AnimatePresence>
        {showLayoutManager && (
          <motion.div
            className="fixed inset-0 z-50 bg-black/50 flex items-center justify-center"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
          >
            <motion.div
              className="bg-gray-900 rounded-lg p-6 max-w-xl w-full mx-4 border border-gray-700"
              initial={{ scale: 0.9 }}
              animate={{ scale: 1 }}
              exit={{ scale: 0.9 }}
            >
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-lg font-semibold">Workspace Layouts</h3>
                <button
                  onClick={() => setShowLayoutManager(false)}
                  className="p-2 hover:bg-gray-800 rounded"
                >
                  <X size={16} />
                </button>
              </div>
              
              <div className="space-y-3 max-h-96 overflow-y-auto">
                {savedLayouts.map((layout) => (
                  <div
                    key={layout.id}
                    className={`p-4 rounded-lg border transition-colors ${
                      currentLayout?.id === layout.id
                        ? 'bg-blue-900/30 border-blue-600'
                        : 'bg-gray-800 border-gray-600 hover:border-gray-500'
                    }`}
                  >
                    <div className="flex items-center justify-between">
                      <div>
                        <div className="font-medium flex items-center space-x-2">
                          <span>{layout.name}</span>
                          {layout.isDefault && (
                            <span className="text-xs bg-green-600 px-2 py-1 rounded">Default</span>
                          )}
                        </div>
                        <div className="text-sm text-gray-400">
                          {layout.widgets.length} widgets • Modified {layout.lastModified.toLocaleDateString()}
                        </div>
                      </div>
                      <div className="flex items-center space-x-2">
                        {currentLayout?.id !== layout.id && (
                          <button
                            onClick={() => {
                              loadLayout(layout);
                              setShowLayoutManager(false);
                            }}
                            className="px-3 py-1 bg-blue-600 hover:bg-blue-700 rounded text-sm"
                          >
                            Load
                          </button>
                        )}
                        {!layout.isDefault && (
                          <button
                            onClick={() => deleteLayout(layout.id)}
                            className="px-3 py-1 bg-red-600 hover:bg-red-700 rounded text-sm"
                          >
                            Delete
                          </button>
                        )}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Theme Selector Modal */}
      <AnimatePresence>
        {showThemeSelector && (
          <motion.div
            className="fixed inset-0 z-50 bg-black/50 flex items-center justify-center"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
          >
            <motion.div
              className="bg-gray-900 rounded-lg p-6 max-w-md w-full mx-4 border border-gray-700"
              initial={{ scale: 0.9 }}
              animate={{ scale: 1 }}
              exit={{ scale: 0.9 }}
            >
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-lg font-semibold">Select Theme</h3>
                <button
                  onClick={() => setShowThemeSelector(false)}
                  className="p-2 hover:bg-gray-800 rounded"
                >
                  <X size={16} />
                </button>
              </div>
              
              <div className="space-y-3">
                {defaultThemes.map((theme) => (
                  <button
                    key={theme.id}
                    onClick={() => {
                      setSelectedTheme(theme);
                      setShowThemeSelector(false);
                    }}
                    className={`w-full p-4 rounded-lg border transition-colors text-left ${
                      selectedTheme.id === theme.id
                        ? 'border-blue-600 bg-blue-900/30'
                        : 'border-gray-600 bg-gray-800 hover:border-gray-500'
                    }`}
                  >
                    <div className="flex items-center justify-between">
                      <div>
                        <div className="font-medium">{theme.name}</div>
                        <div className="flex items-center space-x-1 mt-2">
                          {Object.entries(theme.colors).slice(0, 4).map(([key, color]) => (
                            <div
                              key={key}
                              className="w-4 h-4 rounded-full border border-gray-500"
                              style={{ backgroundColor: color }}
                            ></div>
                          ))}
                        </div>
                      </div>
                      {selectedTheme.id === theme.id && (
                        <div className="text-blue-400">✓</div>
                      )}
                    </div>
                  </button>
                ))}
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
};

export default CustomizableWorkspace;