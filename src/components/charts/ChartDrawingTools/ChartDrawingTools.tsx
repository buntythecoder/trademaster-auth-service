import React, { useState, useRef, useCallback, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { 
  Minus,
  Square,
  Circle,
  Type,
  TrendingUp,
  Target,
  Triangle,
  Zap,
  Ruler,
  RotateCcw,
  Save,
  Upload,
  Download,
  Trash2,
  Copy,
  Eye,
  EyeOff,
  Lock,
  Unlock,
  Layers,
  Palette,
  Settings,
  Move,
  Edit3,
  X,
  ChevronDown,
  Grid3x3
} from 'lucide-react';

interface Point {
  x: number;
  y: number;
  price: number;
  timestamp: Date;
}

interface DrawingObject {
  id: string;
  type: 'trendline' | 'horizontal' | 'vertical' | 'rectangle' | 'circle' | 'ellipse' | 
        'fibonacci_retracement' | 'fibonacci_extension' | 'fibonacci_arc' | 'fibonacci_fan' |
        'gann_fan' | 'gann_box' | 'andrew_pitchfork' | 'linear_regression' |
        'parallel_channel' | 'equidistant_channel' | 'arrow_up' | 'arrow_down' | 
        'text_note' | 'price_label' | 'time_label' | 'support_resistance' |
        'head_shoulders' | 'triangle_pattern' | 'flag_pattern' | 'wedge_pattern';
  points: Point[];
  style: {
    color: string;
    thickness: number;
    lineStyle: 'solid' | 'dashed' | 'dotted';
    fillColor?: string;
    fillOpacity?: number;
    showLabels: boolean;
    showPrices: boolean;
    fontSize?: number;
    fontWeight?: 'normal' | 'bold';
  };
  properties: {
    name?: string;
    description?: string;
    isVisible: boolean;
    isLocked: boolean;
    isSelected: boolean;
    layer: number;
    alertEnabled: boolean;
    alertCondition?: 'cross_above' | 'cross_below' | 'touch' | 'break';
  };
  text?: string;
  createdAt: Date;
  modifiedAt: Date;
}

interface DrawingToolConfig {
  id: string;
  name: string;
  icon: React.ReactNode;
  category: 'lines' | 'shapes' | 'fibonacci' | 'gann' | 'channels' | 'annotations' | 'patterns';
  requiresPoints: number; // -1 for unlimited
  description: string;
  hotkey?: string;
}

interface Template {
  id: string;
  name: string;
  description: string;
  objects: DrawingObject[];
  thumbnail?: string;
  category: 'support_resistance' | 'patterns' | 'analysis' | 'custom';
}

const DRAWING_TOOLS: DrawingToolConfig[] = [
  // Lines
  { 
    id: 'trendline', 
    name: 'Trend Line', 
    icon: <TrendingUp size={16} />, 
    category: 'lines', 
    requiresPoints: 2, 
    description: 'Draw trend lines to identify price direction',
    hotkey: 'T'
  },
  { 
    id: 'horizontal', 
    name: 'Horizontal Line', 
    icon: <Minus size={16} />, 
    category: 'lines', 
    requiresPoints: 1, 
    description: 'Draw horizontal support/resistance levels',
    hotkey: 'H'
  },
  { 
    id: 'vertical', 
    name: 'Vertical Line', 
    icon: <div className="rotate-90"><Minus size={16} /></div>, 
    category: 'lines', 
    requiresPoints: 1, 
    description: 'Mark important time events',
    hotkey: 'V'
  },
  { 
    id: 'linear_regression', 
    name: 'Linear Regression', 
    icon: <TrendingUp size={16} />, 
    category: 'lines', 
    requiresPoints: 2, 
    description: 'Statistical trend line with confidence bands'
  },

  // Shapes
  { 
    id: 'rectangle', 
    name: 'Rectangle', 
    icon: <Square size={16} />, 
    category: 'shapes', 
    requiresPoints: 2, 
    description: 'Draw rectangular areas for consolidation zones',
    hotkey: 'R'
  },
  { 
    id: 'circle', 
    name: 'Circle', 
    icon: <Circle size={16} />, 
    category: 'shapes', 
    requiresPoints: 2, 
    description: 'Circular areas for cycle analysis'
  },
  { 
    id: 'ellipse', 
    name: 'Ellipse', 
    icon: <Circle size={16} />, 
    category: 'shapes', 
    requiresPoints: 3, 
    description: 'Elliptical areas for advanced cycle analysis'
  },

  // Fibonacci Tools
  { 
    id: 'fibonacci_retracement', 
    name: 'Fibonacci Retracement', 
    icon: <Target size={16} />, 
    category: 'fibonacci', 
    requiresPoints: 2, 
    description: 'Key retracement levels (23.6%, 38.2%, 50%, 61.8%)',
    hotkey: 'F'
  },
  { 
    id: 'fibonacci_extension', 
    name: 'Fibonacci Extension', 
    icon: <Target size={16} />, 
    category: 'fibonacci', 
    requiresPoints: 3, 
    description: 'Extension levels for target projection'
  },
  { 
    id: 'fibonacci_arc', 
    name: 'Fibonacci Arc', 
    icon: <Target size={16} />, 
    category: 'fibonacci', 
    requiresPoints: 2, 
    description: 'Time-based Fibonacci arcs'
  },
  { 
    id: 'fibonacci_fan', 
    name: 'Fibonacci Fan', 
    icon: <Target size={16} />, 
    category: 'fibonacci', 
    requiresPoints: 2, 
    description: 'Angular support and resistance levels'
  },

  // Gann Tools
  { 
    id: 'gann_fan', 
    name: 'Gann Fan', 
    icon: <Triangle size={16} />, 
    category: 'gann', 
    requiresPoints: 2, 
    description: 'Gann angles for time/price relationships'
  },
  { 
    id: 'gann_box', 
    name: 'Gann Box', 
    icon: <Grid3x3 size={16} />, 
    category: 'gann', 
    requiresPoints: 2, 
    description: 'Square of nine for time/price analysis'
  },

  // Channels
  { 
    id: 'parallel_channel', 
    name: 'Parallel Channel', 
    icon: <Ruler size={16} />, 
    category: 'channels', 
    requiresPoints: 3, 
    description: 'Parallel support and resistance lines'
  },
  { 
    id: 'andrew_pitchfork', 
    name: "Andrew's Pitchfork", 
    icon: <Zap size={16} />, 
    category: 'channels', 
    requiresPoints: 3, 
    description: 'Three-point pitchfork for trend channels'
  },
  { 
    id: 'equidistant_channel', 
    name: 'Equidistant Channel', 
    icon: <Ruler size={16} />, 
    category: 'channels', 
    requiresPoints: 4, 
    description: 'Channel with equal distance bands'
  },

  // Annotations
  { 
    id: 'text_note', 
    name: 'Text Note', 
    icon: <Type size={16} />, 
    category: 'annotations', 
    requiresPoints: 1, 
    description: 'Add text annotations to chart',
    hotkey: 'N'
  },
  { 
    id: 'arrow_up', 
    name: 'Arrow Up', 
    icon: <TrendingUp size={16} />, 
    category: 'annotations', 
    requiresPoints: 1, 
    description: 'Bullish signal arrow'
  },
  { 
    id: 'arrow_down', 
    name: 'Arrow Down', 
    icon: <TrendingUp className="rotate-180" size={16} />, 
    category: 'annotations', 
    requiresPoints: 1, 
    description: 'Bearish signal arrow'
  },
  { 
    id: 'price_label', 
    name: 'Price Label', 
    icon: <Type size={16} />, 
    category: 'annotations', 
    requiresPoints: 1, 
    description: 'Show price at specific level'
  }
];

const FIBONACCI_LEVELS = [0, 0.236, 0.382, 0.5, 0.618, 0.786, 1, 1.272, 1.414, 1.618, 2.618];
const GANN_ANGLES = [1/8, 1/4, 1/3, 1/2, 1, 2, 3, 4, 8];

const ChartDrawingTools: React.FC<{
  width: number;
  height: number;
  priceRange: { min: number; max: number };
  timeRange: { start: Date; end: Date };
  onDrawingChange?: (drawings: DrawingObject[]) => void;
  readOnly?: boolean;
}> = ({ 
  width, 
  height, 
  priceRange, 
  timeRange, 
  onDrawingChange,
  readOnly = false 
}) => {
  const [drawings, setDrawings] = useState<DrawingObject[]>([]);
  const [activeToolId, setActiveToolId] = useState<string | null>(null);
  const [isDrawing, setIsDrawing] = useState(false);
  const [currentDrawing, setCurrentDrawing] = useState<DrawingObject | null>(null);
  const [selectedDrawings, setSelectedDrawings] = useState<Set<string>>(new Set());
  const [showStylePanel, setShowStylePanel] = useState(false);
  const [showLayersPanel, setShowLayersPanel] = useState(false);
  const [showTemplatesPanel, setShowTemplatesPanel] = useState(false);
  const [dragOffset, setDragOffset] = useState<Point | null>(null);
  const [isEditingText, setIsEditingText] = useState<string | null>(null);
  const [textInput, setTextInput] = useState('');

  const canvasRef = useRef<HTMLCanvasElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);

  // Convert screen coordinates to price/time
  const screenToChart = useCallback((screenX: number, screenY: number): Point => {
    const rect = canvasRef.current?.getBoundingClientRect();
    if (!rect) return { x: screenX, y: screenY, price: 0, timestamp: new Date() };

    const chartX = screenX - rect.left;
    const chartY = screenY - rect.top;

    const pricePerPixel = (priceRange.max - priceRange.min) / height;
    const timePerPixel = (timeRange.end.getTime() - timeRange.start.getTime()) / width;

    const price = priceRange.max - (chartY * pricePerPixel);
    const timestamp = new Date(timeRange.start.getTime() + (chartX * timePerPixel));

    return { x: chartX, y: chartY, price, timestamp };
  }, [priceRange, timeRange, width, height]);

  // Convert price/time to screen coordinates
  const chartToScreen = useCallback((point: Point): { x: number; y: number } => {
    const pricePerPixel = (priceRange.max - priceRange.min) / height;
    const timePerPixel = (timeRange.end.getTime() - timeRange.start.getTime()) / width;

    const x = (point.timestamp.getTime() - timeRange.start.getTime()) / timePerPixel;
    const y = (priceRange.max - point.price) / pricePerPixel;

    return { x, y };
  }, [priceRange, timeRange, width, height]);

  // Handle mouse events
  const handleMouseDown = useCallback((event: React.MouseEvent) => {
    if (readOnly || !activeToolId) return;

    const point = screenToChart(event.clientX, event.clientY);
    const activeTool = DRAWING_TOOLS.find(tool => tool.id === activeToolId);
    
    if (!activeTool) return;

    if (currentDrawing && currentDrawing.points.length < activeTool.requiresPoints) {
      // Continue existing drawing
      const updatedDrawing = {
        ...currentDrawing,
        points: [...currentDrawing.points, point]
      };

      if (updatedDrawing.points.length === activeTool.requiresPoints) {
        // Complete the drawing
        setDrawings(prev => [...prev, updatedDrawing]);
        setCurrentDrawing(null);
        setActiveToolId(null);
        setIsDrawing(false);
      } else {
        setCurrentDrawing(updatedDrawing);
      }
    } else {
      // Start new drawing
      const newDrawing: DrawingObject = {
        id: `drawing_${Date.now()}`,
        type: activeToolId as any,
        points: [point],
        style: {
          color: '#3B82F6',
          thickness: 2,
          lineStyle: 'solid',
          showLabels: true,
          showPrices: true,
          fontSize: 12,
          fontWeight: 'normal'
        },
        properties: {
          name: `${activeTool.name} ${drawings.length + 1}`,
          isVisible: true,
          isLocked: false,
          isSelected: true,
          layer: 1,
          alertEnabled: false
        },
        createdAt: new Date(),
        modifiedAt: new Date()
      };

      if (activeTool.requiresPoints === 1) {
        // Single-point tool, complete immediately
        setDrawings(prev => [...prev, newDrawing]);
        setActiveToolId(null);
        
        if (activeToolId === 'text_note') {
          setIsEditingText(newDrawing.id);
          setTextInput('');
        }
      } else {
        // Multi-point tool, start drawing
        setCurrentDrawing(newDrawing);
        setIsDrawing(true);
      }
    }
  }, [activeToolId, currentDrawing, drawings.length, readOnly, screenToChart]);

  const handleMouseMove = useCallback((event: React.MouseEvent) => {
    if (!isDrawing || !currentDrawing) return;

    const point = screenToChart(event.clientX, event.clientY);
    setCurrentDrawing(prev => prev ? {
      ...prev,
      points: prev.points.length > 0 ? [...prev.points.slice(0, -1), point] : [point]
    } : null);
  }, [isDrawing, currentDrawing, screenToChart]);

  const handleKeyDown = useCallback((event: KeyboardEvent) => {
    if (readOnly) return;

    // Hotkey handling
    const tool = DRAWING_TOOLS.find(t => t.hotkey === event.key.toUpperCase());
    if (tool) {
      setActiveToolId(activeToolId === tool.id ? null : tool.id);
      return;
    }

    // Other shortcuts
    switch (event.key) {
      case 'Escape':
        setActiveToolId(null);
        setCurrentDrawing(null);
        setIsDrawing(false);
        setSelectedDrawings(new Set());
        break;
      case 'Delete':
        if (selectedDrawings.size > 0) {
          setDrawings(prev => prev.filter(d => !selectedDrawings.has(d.id)));
          setSelectedDrawings(new Set());
        }
        break;
      case 'c':
        if (event.ctrlKey && selectedDrawings.size > 0) {
          // Copy selected drawings
          const selectedObjects = drawings.filter(d => selectedDrawings.has(d.id));
          // Implement clipboard functionality
        }
        break;
    }
  }, [activeToolId, selectedDrawings, drawings, readOnly]);

  useEffect(() => {
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [handleKeyDown]);

  // Drawing functions
  const drawTrendLine = (ctx: CanvasRenderingContext2D, drawing: DrawingObject) => {
    if (drawing.points.length < 2) return;

    const start = chartToScreen(drawing.points[0]);
    const end = chartToScreen(drawing.points[1]);

    ctx.beginPath();
    ctx.moveTo(start.x, start.y);
    ctx.lineTo(end.x, end.y);
    ctx.strokeStyle = drawing.style.color;
    ctx.lineWidth = drawing.style.thickness;
    ctx.setLineDash(drawing.style.lineStyle === 'dashed' ? [5, 5] : []);
    ctx.stroke();

    if (drawing.style.showPrices) {
      ctx.fillStyle = drawing.style.color;
      ctx.font = `${drawing.style.fontSize}px Arial`;
      ctx.fillText(
        drawing.points[1].price.toFixed(2),
        end.x + 5,
        end.y - 5
      );
    }
  };

  const drawFibonacciRetracement = (ctx: CanvasRenderingContext2D, drawing: DrawingObject) => {
    if (drawing.points.length < 2) return;

    const start = chartToScreen(drawing.points[0]);
    const end = chartToScreen(drawing.points[1]);
    
    const priceRange = Math.abs(drawing.points[1].price - drawing.points[0].price);
    const isUptrend = drawing.points[1].price > drawing.points[0].price;

    FIBONACCI_LEVELS.forEach(level => {
      const levelPrice = isUptrend 
        ? drawing.points[1].price - (priceRange * level)
        : drawing.points[0].price + (priceRange * level);
      
      const y = (priceRange.max - levelPrice) / ((priceRange.max - priceRange.min) / height);
      
      if (y >= 0 && y <= height) {
        ctx.beginPath();
        ctx.moveTo(Math.min(start.x, end.x), y);
        ctx.lineTo(Math.max(start.x, end.x), y);
        ctx.strokeStyle = level === 0.5 ? '#F59E0B' : drawing.style.color;
        ctx.lineWidth = level === 0.5 ? 2 : 1;
        ctx.setLineDash(level === 0.5 ? [] : [3, 3]);
        ctx.stroke();

        if (drawing.style.showLabels) {
          ctx.fillStyle = drawing.style.color;
          ctx.font = `${drawing.style.fontSize}px Arial`;
          ctx.fillText(
            `${(level * 100).toFixed(1)}% (${levelPrice.toFixed(2)})`,
            Math.max(start.x, end.x) + 5,
            y - 2
          );
        }
      }
    });
  };

  const drawRectangle = (ctx: CanvasRenderingContext2D, drawing: DrawingObject) => {
    if (drawing.points.length < 2) return;

    const start = chartToScreen(drawing.points[0]);
    const end = chartToScreen(drawing.points[1]);
    
    const x = Math.min(start.x, end.x);
    const y = Math.min(start.y, end.y);
    const w = Math.abs(end.x - start.x);
    const h = Math.abs(end.y - start.y);

    if (drawing.style.fillColor && drawing.style.fillOpacity) {
      ctx.fillStyle = `${drawing.style.fillColor}${Math.floor(drawing.style.fillOpacity * 255).toString(16).padStart(2, '0')}`;
      ctx.fillRect(x, y, w, h);
    }

    ctx.strokeStyle = drawing.style.color;
    ctx.lineWidth = drawing.style.thickness;
    ctx.setLineDash(drawing.style.lineStyle === 'dashed' ? [5, 5] : []);
    ctx.strokeRect(x, y, w, h);
  };

  const drawTextNote = (ctx: CanvasRenderingContext2D, drawing: DrawingObject) => {
    if (drawing.points.length < 1) return;

    const point = chartToScreen(drawing.points[0]);
    
    ctx.fillStyle = drawing.style.color;
    ctx.font = `${drawing.style.fontWeight} ${drawing.style.fontSize}px Arial`;
    
    const text = drawing.text || 'Note';
    const lines = text.split('\n');
    
    lines.forEach((line, index) => {
      ctx.fillText(
        line,
        point.x + 5,
        point.y + (index * (drawing.style.fontSize! + 2))
      );
    });
  };

  // Render all drawings
  const renderDrawings = useCallback(() => {
    const canvas = canvasRef.current;
    const ctx = canvas?.getContext('2d');
    if (!canvas || !ctx) return;

    ctx.clearRect(0, 0, width, height);
    ctx.setLineDash([]);

    // Draw completed drawings
    drawings.forEach(drawing => {
      if (!drawing.properties.isVisible) return;

      ctx.save();
      
      if (drawing.properties.isSelected) {
        ctx.shadowColor = '#3B82F6';
        ctx.shadowBlur = 3;
      }

      switch (drawing.type) {
        case 'trendline':
        case 'horizontal':
        case 'vertical':
          drawTrendLine(ctx, drawing);
          break;
        case 'fibonacci_retracement':
          drawFibonacciRetracement(ctx, drawing);
          break;
        case 'rectangle':
          drawRectangle(ctx, drawing);
          break;
        case 'text_note':
          drawTextNote(ctx, drawing);
          break;
        default:
          // Default to trend line for unsupported types
          drawTrendLine(ctx, drawing);
      }
      
      ctx.restore();
    });

    // Draw current drawing in progress
    if (currentDrawing && isDrawing) {
      ctx.save();
      ctx.globalAlpha = 0.7;
      ctx.strokeStyle = '#3B82F6';
      ctx.lineWidth = 2;
      ctx.setLineDash([5, 5]);
      
      switch (currentDrawing.type) {
        case 'trendline':
          drawTrendLine(ctx, currentDrawing);
          break;
        case 'rectangle':
          drawRectangle(ctx, currentDrawing);
          break;
        default:
          drawTrendLine(ctx, currentDrawing);
      }
      
      ctx.restore();
    }
  }, [drawings, currentDrawing, isDrawing, width, height, chartToScreen]);

  useEffect(() => {
    renderDrawings();
  }, [renderDrawings]);

  useEffect(() => {
    onDrawingChange?.(drawings);
  }, [drawings, onDrawingChange]);

  const deleteSelectedDrawings = () => {
    setDrawings(prev => prev.filter(d => !selectedDrawings.has(d.id)));
    setSelectedDrawings(new Set());
  };

  const saveTemplate = () => {
    if (drawings.length === 0) return;

    const template: Template = {
      id: `template_${Date.now()}`,
      name: `Template ${new Date().toLocaleDateString()}`,
      description: 'Custom drawing template',
      objects: drawings,
      category: 'custom'
    };

    // Save to localStorage or send to backend
    const savedTemplates = JSON.parse(localStorage.getItem('drawing_templates') || '[]');
    savedTemplates.push(template);
    localStorage.setItem('drawing_templates', JSON.stringify(savedTemplates));
  };

  const clearAllDrawings = () => {
    setDrawings([]);
    setSelectedDrawings(new Set());
    setCurrentDrawing(null);
    setActiveToolId(null);
  };

  const groupedTools = DRAWING_TOOLS.reduce((acc, tool) => {
    if (!acc[tool.category]) acc[tool.category] = [];
    acc[tool.category].push(tool);
    return acc;
  }, {} as Record<string, DrawingToolConfig[]>);

  return (
    <div className="relative w-full h-full">
      {/* Drawing Canvas */}
      <canvas
        ref={canvasRef}
        width={width}
        height={height}
        className="absolute inset-0 cursor-crosshair"
        onMouseDown={handleMouseDown}
        onMouseMove={handleMouseMove}
        onMouseUp={() => setIsDrawing(false)}
      />

      {/* Toolbar */}
      <div className="absolute top-4 left-4 bg-slate-800/95 backdrop-blur rounded-lg border border-slate-700 p-2">
        <div className="flex flex-wrap gap-1">
          {Object.entries(groupedTools).map(([category, tools]) => (
            <div key={category} className="flex items-center space-x-1">
              {tools.map((tool) => (
                <button
                  key={tool.id}
                  onClick={() => setActiveToolId(activeToolId === tool.id ? null : tool.id)}
                  className={`p-2 rounded transition-colors ${
                    activeToolId === tool.id
                      ? 'bg-blue-600 text-white'
                      : 'bg-slate-700/50 hover:bg-slate-600 text-gray-300'
                  }`}
                  title={`${tool.name}${tool.hotkey ? ` (${tool.hotkey})` : ''}`}
                >
                  {tool.icon}
                </button>
              ))}
              {category !== 'annotations' && <div className="w-px h-6 bg-slate-600 mx-1" />}
            </div>
          ))}
        </div>
      </div>

      {/* Quick Actions */}
      <div className="absolute top-4 right-4 bg-slate-800/95 backdrop-blur rounded-lg border border-slate-700 p-2">
        <div className="flex items-center space-x-2">
          <button
            onClick={() => setShowStylePanel(!showStylePanel)}
            className="p-2 bg-slate-700/50 hover:bg-slate-600 rounded transition-colors"
            title="Style Options"
          >
            <Palette size={16} />
          </button>
          
          <button
            onClick={() => setShowLayersPanel(!showLayersPanel)}
            className="p-2 bg-slate-700/50 hover:bg-slate-600 rounded transition-colors"
            title="Manage Layers"
          >
            <Layers size={16} />
          </button>
          
          <button
            onClick={saveTemplate}
            disabled={drawings.length === 0}
            className="p-2 bg-slate-700/50 hover:bg-slate-600 rounded transition-colors disabled:opacity-50"
            title="Save as Template"
          >
            <Save size={16} />
          </button>
          
          <button
            onClick={deleteSelectedDrawings}
            disabled={selectedDrawings.size === 0}
            className="p-2 bg-red-600/20 hover:bg-red-600/40 text-red-400 rounded transition-colors disabled:opacity-50"
            title="Delete Selected"
          >
            <Trash2 size={16} />
          </button>
          
          <button
            onClick={clearAllDrawings}
            disabled={drawings.length === 0}
            className="p-2 bg-red-600/20 hover:bg-red-600/40 text-red-400 rounded transition-colors disabled:opacity-50"
            title="Clear All"
          >
            <RotateCcw size={16} />
          </button>
        </div>
      </div>

      {/* Drawing Count */}
      {drawings.length > 0 && (
        <div className="absolute bottom-4 left-4 bg-slate-800/95 backdrop-blur rounded-lg border border-slate-700 px-3 py-1">
          <span className="text-sm text-gray-300">
            {drawings.length} drawing{drawings.length !== 1 ? 's' : ''}
            {selectedDrawings.size > 0 && ` (${selectedDrawings.size} selected)`}
          </span>
        </div>
      )}

      {/* Active Tool Indicator */}
      {activeToolId && (
        <div className="absolute bottom-4 right-4 bg-blue-600 text-white px-3 py-2 rounded-lg">
          <div className="flex items-center space-x-2">
            {DRAWING_TOOLS.find(t => t.id === activeToolId)?.icon}
            <span className="text-sm font-medium">
              {DRAWING_TOOLS.find(t => t.id === activeToolId)?.name}
            </span>
          </div>
          <div className="text-xs opacity-80 mt-1">
            {DRAWING_TOOLS.find(t => t.id === activeToolId)?.description}
          </div>
        </div>
      )}

      {/* Text Input Modal */}
      <AnimatePresence>
        {isEditingText && (
          <motion.div
            className="absolute inset-0 bg-black/50 flex items-center justify-center"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
          >
            <motion.div
              className="bg-slate-800 rounded-lg p-6 border border-slate-700 max-w-md w-full"
              initial={{ scale: 0.9 }}
              animate={{ scale: 1 }}
              exit={{ scale: 0.9 }}
            >
              <h3 className="text-lg font-semibold mb-4">Add Text Note</h3>
              <textarea
                value={textInput}
                onChange={(e) => setTextInput(e.target.value)}
                placeholder="Enter your note..."
                className="w-full h-32 p-3 bg-slate-700 border border-slate-600 rounded resize-none"
                autoFocus
              />
              <div className="flex justify-end space-x-3 mt-4">
                <button
                  onClick={() => {
                    setIsEditingText(null);
                    setTextInput('');
                  }}
                  className="px-4 py-2 bg-slate-600 hover:bg-slate-500 rounded transition-colors"
                >
                  Cancel
                </button>
                <button
                  onClick={() => {
                    setDrawings(prev => prev.map(d => 
                      d.id === isEditingText ? { ...d, text: textInput } : d
                    ));
                    setIsEditingText(null);
                    setTextInput('');
                  }}
                  className="px-4 py-2 bg-blue-600 hover:bg-blue-700 rounded transition-colors"
                >
                  Add Note
                </button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
};

export default ChartDrawingTools;