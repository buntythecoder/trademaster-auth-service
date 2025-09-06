import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Grid, Box, Typography, Paper, Card, CardContent, IconButton, Button,
  Chip, Dialog, DialogTitle, DialogContent, DialogActions, TextField,
  FormControl, InputLabel, Select, MenuItem, Switch, FormControlLabel,
  Tabs, Tab, Divider, List, ListItem, ListItemText, ListItemIcon,
  ListItemSecondary, Avatar, Badge, Menu, Collapse, Tooltip, Alert,
  LinearProgress, CircularProgress, Accordion, AccordionSummary, AccordionDetails,
  Checkbox, FormGroup, RadioGroup, Radio, FormLabel, Slider, Autocomplete
} from '@mui/material';
import {
  Add as AddIcon,
  Dashboard as DashboardIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Share as ShareIcon,
  Save as SaveIcon,
  Download as DownloadIcon,
  Upload as UploadIcon,
  Settings as SettingsIcon,
  Visibility as VisibilityIcon,
  VisibilityOff as VisibilityOffIcon,
  DragHandle as DragHandleIcon,
  GridView as GridViewIcon,
  ViewList as ViewListIcon,
  FilterList as FilterListIcon,
  Schedule as ScheduleIcon,
  Notifications as NotificationsIcon,
  TrendingUp as TrendingUpIcon,
  Analytics as AnalyticsIcon,
  Assessment as AssessmentIcon,
  PieChart as PieChartIcon,
  ShowChart as ShowChartIcon,
  BarChart as BarChartIcon,
  Timeline as TimelineIcon,
  TableChart as TableChartIcon,
  DonutSmall as DonutSmallIcon,
  MultilineChart as MultilineChartIcon,
  ExpandMore as ExpandMoreIcon,
  MoreVert as MoreVertIcon,
  Refresh as RefreshIcon,
  CloudDownload as CloudDownloadIcon,
  Email as EmailIcon,
  Print as PrintIcon,
  Fullscreen as FullscreenIcon,
  FullscreenExit as FullscreenExitIcon,
  ColorLens as ColorLensIcon,
  Category as CategoryIcon
} from '@mui/icons-material';
import { DateTimePicker } from '@mui/x-date-pickers/DateTimePicker';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { AdapterDateFns } from '@mui/x-date-pickers/AdapterDateFns';

// Types and Interfaces
interface ReportTemplate {
  id: string;
  name: string;
  description: string;
  category: 'PERFORMANCE' | 'RISK' | 'ATTRIBUTION' | 'COMPLIANCE' | 'EXECUTIVE' | 'CUSTOM';
  layout: DashboardLayout;
  widgets: ReportWidget[];
  schedule?: ReportSchedule;
  recipients?: string[];
  lastGenerated?: Date;
  createdBy: string;
  createdAt: Date;
  isPublic: boolean;
  tags: string[];
  thumbnail?: string;
  version: number;
}

interface ReportWidget {
  id: string;
  type: WidgetType;
  title: string;
  position: { x: number; y: number; width: number; height: number };
  configuration: WidgetConfiguration;
  dataSource: DataSource;
  refreshRate?: number; // minutes
  isVisible: boolean;
  style: WidgetStyle;
  filters?: WidgetFilter[];
}

interface DashboardLayout {
  id: string;
  name: string;
  type: 'GRID' | 'FLEXIBLE' | 'TABBED';
  columns: number;
  rows: number;
  responsive: boolean;
  theme: 'LIGHT' | 'DARK' | 'AUTO';
  spacing: number;
}

interface WidgetConfiguration {
  chartType?: 'LINE' | 'BAR' | 'PIE' | 'AREA' | 'SCATTER' | 'HEATMAP' | 'TABLE' | 'METRIC';
  timeRange?: string;
  metrics?: string[];
  dimensions?: string[];
  aggregation?: 'SUM' | 'AVG' | 'COUNT' | 'MAX' | 'MIN';
  formatting?: WidgetFormatting;
  thresholds?: WidgetThreshold[];
  customSettings?: Record<string, any>;
}

interface WidgetFormatting {
  numberFormat: 'DECIMAL' | 'PERCENTAGE' | 'CURRENCY' | 'INTEGER';
  decimalPlaces: number;
  showLegend: boolean;
  showGrid: boolean;
  colors: string[];
  fontSize: 'SMALL' | 'MEDIUM' | 'LARGE';
}

interface WidgetThreshold {
  id: string;
  name: string;
  value: number;
  operator: '>' | '<' | '=' | '>=' | '<=';
  color: string;
  action: 'HIGHLIGHT' | 'ALERT' | 'HIDE';
}

interface DataSource {
  id: string;
  name: string;
  type: 'PORTFOLIO' | 'BENCHMARK' | 'MARKET' | 'RISK' | 'ATTRIBUTION' | 'TRADE';
  connection: string;
  query?: string;
  parameters?: Record<string, any>;
  cache?: boolean;
  cacheTtl?: number; // minutes
}

interface ReportSchedule {
  id: string;
  frequency: 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'QUARTERLY' | 'ON_DEMAND';
  dayOfWeek?: number; // 1-7, Monday = 1
  dayOfMonth?: number; // 1-31
  time: string; // HH:mm format
  timezone: string;
  isActive: boolean;
  nextRun?: Date;
  lastRun?: Date;
}

interface WidgetFilter {
  id: string;
  field: string;
  operator: 'EQUALS' | 'CONTAINS' | 'GREATER_THAN' | 'LESS_THAN' | 'IN' | 'BETWEEN';
  value: any;
  isActive: boolean;
}

interface WidgetStyle {
  backgroundColor: string;
  borderColor: string;
  borderWidth: number;
  borderRadius: number;
  padding: number;
  shadow: boolean;
  opacity: number;
}

interface ReportGeneration {
  id: string;
  templateId: string;
  status: 'PENDING' | 'GENERATING' | 'COMPLETED' | 'FAILED';
  progress: number;
  startTime: Date;
  endTime?: Date;
  outputFormat: 'PDF' | 'EXCEL' | 'HTML' | 'PNG';
  filePath?: string;
  error?: string;
}

type WidgetType = 'PERFORMANCE_CHART' | 'RISK_METRICS' | 'ATTRIBUTION_TABLE' | 
                  'BENCHMARK_COMPARISON' | 'HOLDINGS_TABLE' | 'TRADE_SUMMARY' | 
                  'NEWS_FEED' | 'CUSTOM_METRIC' | 'HEATMAP' | 'GAUGE' | 
                  'TREEMAP' | 'WATERFALL' | 'SCORECARD';

const ReportingStudio: React.FC = () => {
  // State Management
  const [templates, setTemplates] = useState<ReportTemplate[]>([]);
  const [selectedTemplate, setSelectedTemplate] = useState<ReportTemplate | null>(null);
  const [isEditing, setIsEditing] = useState(false);
  const [activeTab, setActiveTab] = useState(0);
  const [showTemplateDialog, setShowTemplateDialog] = useState(false);
  const [showWidgetDialog, setShowWidgetDialog] = useState(false);
  const [showScheduleDialog, setShowScheduleDialog] = useState(false);
  const [showShareDialog, setShowShareDialog] = useState(false);
  const [draggedWidget, setDraggedWidget] = useState<ReportWidget | null>(null);
  const [viewMode, setViewMode] = useState<'grid' | 'list'>('grid');
  const [filterCategory, setFilterCategory] = useState<string>('ALL');
  const [searchTerm, setSearchTerm] = useState('');
  const [reportGenerations, setReportGenerations] = useState<ReportGeneration[]>([]);
  const [selectedWidget, setSelectedWidget] = useState<ReportWidget | null>(null);
  const [previewMode, setPreviewMode] = useState(false);
  const [fullscreenWidget, setFullscreenWidget] = useState<string | null>(null);

  // Form States
  const [newTemplate, setNewTemplate] = useState<Partial<ReportTemplate>>({
    name: '',
    description: '',
    category: 'CUSTOM',
    layout: {
      id: 'default',
      name: 'Default Grid',
      type: 'GRID',
      columns: 12,
      rows: 8,
      responsive: true,
      theme: 'LIGHT',
      spacing: 2
    },
    widgets: [],
    tags: [],
    isPublic: false,
    version: 1
  });

  const [newWidget, setNewWidget] = useState<Partial<ReportWidget>>({
    title: '',
    type: 'PERFORMANCE_CHART',
    position: { x: 0, y: 0, width: 4, height: 3 },
    configuration: {
      chartType: 'LINE',
      timeRange: '1M',
      metrics: ['return'],
      formatting: {
        numberFormat: 'PERCENTAGE',
        decimalPlaces: 2,
        showLegend: true,
        showGrid: true,
        colors: ['#1976d2', '#dc004e'],
        fontSize: 'MEDIUM'
      }
    },
    dataSource: {
      id: 'portfolio',
      name: 'Portfolio Data',
      type: 'PORTFOLIO',
      connection: 'default'
    },
    isVisible: true,
    style: {
      backgroundColor: '#ffffff',
      borderColor: '#e0e0e0',
      borderWidth: 1,
      borderRadius: 4,
      padding: 16,
      shadow: true,
      opacity: 1
    }
  });

  // Mock Data
  const mockTemplates: ReportTemplate[] = [
    {
      id: 'perf-001',
      name: 'Monthly Performance Report',
      description: 'Comprehensive monthly performance analysis with benchmark comparison',
      category: 'PERFORMANCE',
      layout: {
        id: 'grid-12',
        name: '12-Column Grid',
        type: 'GRID',
        columns: 12,
        rows: 6,
        responsive: true,
        theme: 'LIGHT',
        spacing: 2
      },
      widgets: [
        {
          id: 'w1',
          type: 'PERFORMANCE_CHART',
          title: 'Portfolio Performance',
          position: { x: 0, y: 0, width: 8, height: 3 },
          configuration: {
            chartType: 'LINE',
            timeRange: '1Y',
            metrics: ['cumulative_return', 'benchmark_return']
          },
          dataSource: { id: 'portfolio', name: 'Portfolio', type: 'PORTFOLIO', connection: 'default' },
          isVisible: true,
          style: {
            backgroundColor: '#ffffff',
            borderColor: '#e0e0e0',
            borderWidth: 1,
            borderRadius: 4,
            padding: 16,
            shadow: true,
            opacity: 1
          }
        }
      ],
      schedule: {
        id: 'sch-001',
        frequency: 'MONTHLY',
        dayOfMonth: 1,
        time: '09:00',
        timezone: 'UTC',
        isActive: true
      },
      recipients: ['portfolio.manager@company.com'],
      lastGenerated: new Date('2024-08-25'),
      createdBy: 'John Smith',
      createdAt: new Date('2024-01-15'),
      isPublic: true,
      tags: ['performance', 'monthly', 'standard'],
      version: 3
    },
    {
      id: 'risk-002',
      name: 'Risk Monitoring Dashboard',
      description: 'Real-time risk metrics and alerts for portfolio monitoring',
      category: 'RISK',
      layout: {
        id: 'grid-12',
        name: '12-Column Grid',
        type: 'GRID',
        columns: 12,
        rows: 8,
        responsive: true,
        theme: 'LIGHT',
        spacing: 2
      },
      widgets: [],
      createdBy: 'Sarah Johnson',
      createdAt: new Date('2024-02-10'),
      isPublic: false,
      tags: ['risk', 'monitoring', 'alerts'],
      version: 1
    },
    {
      id: 'attr-003',
      name: 'Attribution Analysis Report',
      description: 'Detailed performance attribution analysis across sectors and securities',
      category: 'ATTRIBUTION',
      layout: {
        id: 'flexible',
        name: 'Flexible Layout',
        type: 'FLEXIBLE',
        columns: 12,
        rows: 10,
        responsive: true,
        theme: 'LIGHT',
        spacing: 3
      },
      widgets: [],
      schedule: {
        id: 'sch-003',
        frequency: 'WEEKLY',
        dayOfWeek: 1,
        time: '08:00',
        timezone: 'UTC',
        isActive: true
      },
      createdBy: 'Michael Chen',
      createdAt: new Date('2024-03-05'),
      isPublic: true,
      tags: ['attribution', 'analysis', 'weekly'],
      version: 2
    }
  ];

  const widgetTypes = [
    { value: 'PERFORMANCE_CHART', label: 'Performance Chart', icon: TrendingUpIcon, category: 'Charts' },
    { value: 'RISK_METRICS', label: 'Risk Metrics', icon: AssessmentIcon, category: 'Risk' },
    { value: 'ATTRIBUTION_TABLE', label: 'Attribution Table', icon: TableChartIcon, category: 'Attribution' },
    { value: 'BENCHMARK_COMPARISON', label: 'Benchmark Comparison', icon: BarChartIcon, category: 'Performance' },
    { value: 'HOLDINGS_TABLE', label: 'Holdings Table', icon: GridViewIcon, category: 'Portfolio' },
    { value: 'TRADE_SUMMARY', label: 'Trade Summary', icon: TimelineIcon, category: 'Trading' },
    { value: 'HEATMAP', label: 'Risk Heatmap', icon: DonutSmallIcon, category: 'Risk' },
    { value: 'GAUGE', label: 'Performance Gauge', icon: PieChartIcon, category: 'Metrics' },
    { value: 'SCORECARD', label: 'Key Metrics Scorecard', icon: AnalyticsIcon, category: 'Summary' }
  ];

  const reportCategories = [
    { value: 'ALL', label: 'All Templates', count: mockTemplates.length },
    { value: 'PERFORMANCE', label: 'Performance', count: 1 },
    { value: 'RISK', label: 'Risk Management', count: 1 },
    { value: 'ATTRIBUTION', label: 'Attribution', count: 1 },
    { value: 'COMPLIANCE', label: 'Compliance', count: 0 },
    { value: 'EXECUTIVE', label: 'Executive Summary', count: 0 },
    { value: 'CUSTOM', label: 'Custom Reports', count: 0 }
  ];

  // Initialize Templates
  useEffect(() => {
    setTemplates(mockTemplates);
  }, []);

  // Filtered Templates
  const filteredTemplates = useMemo(() => {
    return templates.filter(template => {
      const matchesCategory = filterCategory === 'ALL' || template.category === filterCategory;
      const matchesSearch = template.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
                           template.description.toLowerCase().includes(searchTerm.toLowerCase()) ||
                           template.tags.some(tag => tag.toLowerCase().includes(searchTerm.toLowerCase()));
      return matchesCategory && matchesSearch;
    });
  }, [templates, filterCategory, searchTerm]);

  // Template Management
  const handleCreateTemplate = useCallback(() => {
    const template: ReportTemplate = {
      id: `template-${Date.now()}`,
      ...newTemplate as ReportTemplate,
      createdBy: 'Current User',
      createdAt: new Date()
    };
    setTemplates(prev => [...prev, template]);
    setNewTemplate({
      name: '',
      description: '',
      category: 'CUSTOM',
      layout: {
        id: 'default',
        name: 'Default Grid',
        type: 'GRID',
        columns: 12,
        rows: 8,
        responsive: true,
        theme: 'LIGHT',
        spacing: 2
      },
      widgets: [],
      tags: [],
      isPublic: false,
      version: 1
    });
    setShowTemplateDialog(false);
  }, [newTemplate]);

  const handleDeleteTemplate = useCallback((templateId: string) => {
    setTemplates(prev => prev.filter(t => t.id !== templateId));
    if (selectedTemplate?.id === templateId) {
      setSelectedTemplate(null);
    }
  }, [selectedTemplate]);

  const handleCloneTemplate = useCallback((template: ReportTemplate) => {
    const clonedTemplate: ReportTemplate = {
      ...template,
      id: `template-${Date.now()}`,
      name: `${template.name} (Copy)`,
      createdBy: 'Current User',
      createdAt: new Date(),
      version: 1
    };
    setTemplates(prev => [...prev, clonedTemplate]);
  }, []);

  // Widget Management
  const handleAddWidget = useCallback(() => {
    if (!selectedTemplate) return;
    
    const widget: ReportWidget = {
      id: `widget-${Date.now()}`,
      ...newWidget as ReportWidget
    };
    
    const updatedTemplate = {
      ...selectedTemplate,
      widgets: [...selectedTemplate.widgets, widget]
    };
    
    setTemplates(prev => prev.map(t => t.id === selectedTemplate.id ? updatedTemplate : t));
    setSelectedTemplate(updatedTemplate);
    setNewWidget({
      title: '',
      type: 'PERFORMANCE_CHART',
      position: { x: 0, y: 0, width: 4, height: 3 },
      configuration: {
        chartType: 'LINE',
        timeRange: '1M',
        metrics: ['return'],
        formatting: {
          numberFormat: 'PERCENTAGE',
          decimalPlaces: 2,
          showLegend: true,
          showGrid: true,
          colors: ['#1976d2', '#dc004e'],
          fontSize: 'MEDIUM'
        }
      },
      dataSource: {
        id: 'portfolio',
        name: 'Portfolio Data',
        type: 'PORTFOLIO',
        connection: 'default'
      },
      isVisible: true,
      style: {
        backgroundColor: '#ffffff',
        borderColor: '#e0e0e0',
        borderWidth: 1,
        borderRadius: 4,
        padding: 16,
        shadow: true,
        opacity: 1
      }
    });
    setShowWidgetDialog(false);
  }, [selectedTemplate, newWidget]);

  const handleRemoveWidget = useCallback((widgetId: string) => {
    if (!selectedTemplate) return;
    
    const updatedTemplate = {
      ...selectedTemplate,
      widgets: selectedTemplate.widgets.filter(w => w.id !== widgetId)
    };
    
    setTemplates(prev => prev.map(t => t.id === selectedTemplate.id ? updatedTemplate : t));
    setSelectedTemplate(updatedTemplate);
  }, [selectedTemplate]);

  // Report Generation
  const handleGenerateReport = useCallback((template: ReportTemplate, format: 'PDF' | 'EXCEL' | 'HTML' | 'PNG' = 'PDF') => {
    const generation: ReportGeneration = {
      id: `gen-${Date.now()}`,
      templateId: template.id,
      status: 'GENERATING',
      progress: 0,
      startTime: new Date(),
      outputFormat: format
    };
    
    setReportGenerations(prev => [...prev, generation]);
    
    // Simulate report generation
    const interval = setInterval(() => {
      setReportGenerations(prev => prev.map(gen => {
        if (gen.id === generation.id) {
          const newProgress = Math.min(gen.progress + 10, 100);
          if (newProgress === 100) {
            clearInterval(interval);
            return {
              ...gen,
              status: 'COMPLETED',
              progress: 100,
              endTime: new Date(),
              filePath: `/reports/${template.name.toLowerCase().replace(/\s+/g, '-')}.${format.toLowerCase()}`
            };
          }
          return { ...gen, progress: newProgress };
        }
        return gen;
      }));
    }, 500);
  }, []);

  // Drag and Drop
  const handleDragStart = useCallback((widget: ReportWidget) => {
    setDraggedWidget(widget);
  }, []);

  const handleDragEnd = useCallback(() => {
    setDraggedWidget(null);
  }, []);

  const handleDrop = useCallback((x: number, y: number) => {
    if (!draggedWidget || !selectedTemplate) return;
    
    const updatedWidget = {
      ...draggedWidget,
      position: { ...draggedWidget.position, x, y }
    };
    
    const updatedTemplate = {
      ...selectedTemplate,
      widgets: selectedTemplate.widgets.map(w => w.id === draggedWidget.id ? updatedWidget : w)
    };
    
    setTemplates(prev => prev.map(t => t.id === selectedTemplate.id ? updatedTemplate : t));
    setSelectedTemplate(updatedTemplate);
    setDraggedWidget(null);
  }, [draggedWidget, selectedTemplate]);

  return (
    <LocalizationProvider dateAdapter={AdapterDateFns}>
      <Box sx={{ flexGrow: 1, height: '100vh', display: 'flex', flexDirection: 'column' }}>
        {/* Header */}
        <Paper 
          elevation={1} 
          sx={{ 
            p: 2, 
            borderRadius: 0, 
            borderBottom: '1px solid',
            borderBottomColor: 'divider',
            zIndex: 1000
          }}
        >
          <Box sx={{ display: 'flex', justifyContent: 'between', alignItems: 'center', mb: 1 }}>
            <Typography variant="h4" sx={{ fontWeight: 600, display: 'flex', alignItems: 'center', gap: 1 }}>
              <AssessmentIcon color="primary" sx={{ fontSize: 32 }} />
              Reporting Studio
            </Typography>
            <Box sx={{ display: 'flex', gap: 1 }}>
              <Button
                variant="contained"
                startIcon={<AddIcon />}
                onClick={() => setShowTemplateDialog(true)}
              >
                New Template
              </Button>
              <Button
                variant="outlined"
                startIcon={<UploadIcon />}
              >
                Import
              </Button>
              <Button
                variant="outlined"
                startIcon={<DownloadIcon />}
              >
                Export
              </Button>
            </Box>
          </Box>
          
          <Tabs value={activeTab} onChange={(_, newValue) => setActiveTab(newValue)}>
            <Tab label="Templates" icon={<DashboardIcon />} />
            <Tab label="Designer" icon={<EditIcon />} />
            <Tab label="Generations" icon={<CloudDownloadIcon />} />
            <Tab label="Schedules" icon={<ScheduleIcon />} />
          </Tabs>
        </Paper>

        {/* Content Area */}
        <Box sx={{ flexGrow: 1, overflow: 'hidden' }}>
          <AnimatePresence mode="wait">
            {activeTab === 0 && (
              <motion.div
                key="templates"
                initial={{ opacity: 0, x: -20 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: 20 }}
                style={{ height: '100%' }}
              >
                <TemplatesView
                  templates={filteredTemplates}
                  selectedTemplate={selectedTemplate}
                  onSelectTemplate={setSelectedTemplate}
                  onDeleteTemplate={handleDeleteTemplate}
                  onCloneTemplate={handleCloneTemplate}
                  onGenerateReport={handleGenerateReport}
                  viewMode={viewMode}
                  onViewModeChange={setViewMode}
                  filterCategory={filterCategory}
                  onFilterChange={setFilterCategory}
                  searchTerm={searchTerm}
                  onSearchChange={setSearchTerm}
                  categories={reportCategories}
                />
              </motion.div>
            )}

            {activeTab === 1 && (
              <motion.div
                key="designer"
                initial={{ opacity: 0, x: -20 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: 20 }}
                style={{ height: '100%' }}
              >
                <DesignerView
                  selectedTemplate={selectedTemplate}
                  onTemplateChange={setSelectedTemplate}
                  onAddWidget={() => setShowWidgetDialog(true)}
                  onRemoveWidget={handleRemoveWidget}
                  onDragStart={handleDragStart}
                  onDragEnd={handleDragEnd}
                  onDrop={handleDrop}
                  previewMode={previewMode}
                  onPreviewModeChange={setPreviewMode}
                  fullscreenWidget={fullscreenWidget}
                  onFullscreenWidget={setFullscreenWidget}
                />
              </motion.div>
            )}

            {activeTab === 2 && (
              <motion.div
                key="generations"
                initial={{ opacity: 0, x: -20 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: 20 }}
                style={{ height: '100%' }}
              >
                <GenerationsView
                  generations={reportGenerations}
                  templates={templates}
                  onRegenerateReport={(templateId) => {
                    const template = templates.find(t => t.id === templateId);
                    if (template) handleGenerateReport(template);
                  }}
                />
              </motion.div>
            )}

            {activeTab === 3 && (
              <motion.div
                key="schedules"
                initial={{ opacity: 0, x: -20 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: 20 }}
                style={{ height: '100%' }}
              >
                <SchedulesView
                  templates={templates}
                  onUpdateSchedule={(templateId, schedule) => {
                    setTemplates(prev => prev.map(t => 
                      t.id === templateId ? { ...t, schedule } : t
                    ));
                  }}
                />
              </motion.div>
            )}
          </AnimatePresence>
        </Box>

        {/* Template Creation Dialog */}
        <Dialog
          open={showTemplateDialog}
          onClose={() => setShowTemplateDialog(false)}
          maxWidth="md"
          fullWidth
        >
          <DialogTitle>Create New Template</DialogTitle>
          <DialogContent sx={{ pt: 2 }}>
            <Grid container spacing={3}>
              <Grid item xs={12} md={6}>
                <TextField
                  fullWidth
                  label="Template Name"
                  value={newTemplate.name}
                  onChange={(e) => setNewTemplate(prev => ({ ...prev, name: e.target.value }))}
                  required
                />
              </Grid>
              <Grid item xs={12} md={6}>
                <FormControl fullWidth>
                  <InputLabel>Category</InputLabel>
                  <Select
                    value={newTemplate.category}
                    label="Category"
                    onChange={(e) => setNewTemplate(prev => ({ ...prev, category: e.target.value as any }))}
                  >
                    {reportCategories.filter(cat => cat.value !== 'ALL').map((category) => (
                      <MenuItem key={category.value} value={category.value}>
                        {category.label}
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>
              </Grid>
              <Grid item xs={12}>
                <TextField
                  fullWidth
                  multiline
                  rows={3}
                  label="Description"
                  value={newTemplate.description}
                  onChange={(e) => setNewTemplate(prev => ({ ...prev, description: e.target.value }))}
                />
              </Grid>
              <Grid item xs={12} md={6}>
                <Autocomplete
                  multiple
                  freeSolo
                  options={['performance', 'risk', 'attribution', 'compliance', 'monthly', 'weekly', 'daily']}
                  value={newTemplate.tags || []}
                  onChange={(_, value) => setNewTemplate(prev => ({ ...prev, tags: value }))}
                  renderInput={(params) => (
                    <TextField {...params} label="Tags" placeholder="Add tags" />
                  )}
                />
              </Grid>
              <Grid item xs={12} md={6}>
                <FormControlLabel
                  control={
                    <Switch
                      checked={newTemplate.isPublic}
                      onChange={(e) => setNewTemplate(prev => ({ ...prev, isPublic: e.target.checked }))}
                    />
                  }
                  label="Make template public"
                />
              </Grid>
            </Grid>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setShowTemplateDialog(false)}>Cancel</Button>
            <Button 
              onClick={handleCreateTemplate} 
              variant="contained"
              disabled={!newTemplate.name}
            >
              Create Template
            </Button>
          </DialogActions>
        </Dialog>

        {/* Widget Addition Dialog */}
        <Dialog
          open={showWidgetDialog}
          onClose={() => setShowWidgetDialog(false)}
          maxWidth="lg"
          fullWidth
        >
          <DialogTitle>Add New Widget</DialogTitle>
          <DialogContent sx={{ pt: 2 }}>
            <WidgetConfigurationForm
              widget={newWidget}
              onChange={setNewWidget}
              widgetTypes={widgetTypes}
            />
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setShowWidgetDialog(false)}>Cancel</Button>
            <Button 
              onClick={handleAddWidget} 
              variant="contained"
              disabled={!newWidget.title}
            >
              Add Widget
            </Button>
          </DialogActions>
        </Dialog>
      </Box>
    </LocalizationProvider>
  );
};

// Templates View Component
interface TemplatesViewProps {
  templates: ReportTemplate[];
  selectedTemplate: ReportTemplate | null;
  onSelectTemplate: (template: ReportTemplate) => void;
  onDeleteTemplate: (id: string) => void;
  onCloneTemplate: (template: ReportTemplate) => void;
  onGenerateReport: (template: ReportTemplate) => void;
  viewMode: 'grid' | 'list';
  onViewModeChange: (mode: 'grid' | 'list') => void;
  filterCategory: string;
  onFilterChange: (category: string) => void;
  searchTerm: string;
  onSearchChange: (term: string) => void;
  categories: Array<{ value: string; label: string; count: number }>;
}

const TemplatesView: React.FC<TemplatesViewProps> = ({
  templates,
  selectedTemplate,
  onSelectTemplate,
  onDeleteTemplate,
  onCloneTemplate,
  onGenerateReport,
  viewMode,
  onViewModeChange,
  filterCategory,
  onFilterChange,
  searchTerm,
  onSearchChange,
  categories
}) => {
  return (
    <Box sx={{ height: '100%', display: 'flex' }}>
      {/* Sidebar */}
      <Paper sx={{ width: 280, borderRadius: 0, borderRight: '1px solid', borderRightColor: 'divider' }}>
        <Box sx={{ p: 2 }}>
          <TextField
            fullWidth
            size="small"
            placeholder="Search templates..."
            value={searchTerm}
            onChange={(e) => onSearchChange(e.target.value)}
            InputProps={{
              startAdornment: <FilterListIcon sx={{ mr: 1, color: 'action.active' }} />
            }}
            sx={{ mb: 2 }}
          />
          
          <Typography variant="subtitle2" sx={{ mb: 1, fontWeight: 600, color: 'text.secondary' }}>
            CATEGORIES
          </Typography>
          <List dense>
            {categories.map((category) => (
              <ListItem
                key={category.value}
                button
                selected={filterCategory === category.value}
                onClick={() => onFilterChange(category.value)}
                sx={{ borderRadius: 1, mb: 0.5 }}
              >
                <ListItemText 
                  primary={category.label}
                  secondary={`${category.count} template${category.count !== 1 ? 's' : ''}`}
                />
              </ListItem>
            ))}
          </List>
        </Box>
      </Paper>

      {/* Main Content */}
      <Box sx={{ flexGrow: 1, p: 2 }}>
        <Box sx={{ display: 'flex', justifyContent: 'between', alignItems: 'center', mb: 2 }}>
          <Typography variant="h6">
            Templates ({templates.length})
          </Typography>
          <Box sx={{ display: 'flex', gap: 1 }}>
            <IconButton
              color={viewMode === 'grid' ? 'primary' : 'default'}
              onClick={() => onViewModeChange('grid')}
            >
              <GridViewIcon />
            </IconButton>
            <IconButton
              color={viewMode === 'list' ? 'primary' : 'default'}
              onClick={() => onViewModeChange('list')}
            >
              <ViewListIcon />
            </IconButton>
          </Box>
        </Box>

        {viewMode === 'grid' ? (
          <Grid container spacing={2}>
            {templates.map((template) => (
              <Grid item xs={12} sm={6} md={4} lg={3} key={template.id}>
                <motion.div
                  whileHover={{ y: -4 }}
                  transition={{ duration: 0.2 }}
                >
                  <Card 
                    sx={{ 
                      height: '100%',
                      cursor: 'pointer',
                      border: selectedTemplate?.id === template.id ? 2 : 1,
                      borderColor: selectedTemplate?.id === template.id ? 'primary.main' : 'divider'
                    }}
                    onClick={() => onSelectTemplate(template)}
                  >
                    <CardContent>
                      <Box sx={{ display: 'flex', justifyContent: 'between', alignItems: 'flex-start', mb: 1 }}>
                        <Typography variant="h6" sx={{ fontSize: '1.1rem', fontWeight: 600 }}>
                          {template.name}
                        </Typography>
                        <IconButton size="small">
                          <MoreVertIcon />
                        </IconButton>
                      </Box>
                      
                      <Chip 
                        label={template.category}
                        size="small" 
                        sx={{ mb: 1 }}
                        color={template.category === 'PERFORMANCE' ? 'primary' : 
                               template.category === 'RISK' ? 'error' : 'default'}
                      />
                      
                      <Typography variant="body2" color="text.secondary" sx={{ mb: 2, minHeight: 40 }}>
                        {template.description}
                      </Typography>
                      
                      <Box sx={{ display: 'flex', justifyContent: 'between', alignItems: 'center' }}>
                        <Typography variant="caption" color="text.secondary">
                          {template.widgets?.length || 0} widgets
                        </Typography>
                        <Box sx={{ display: 'flex', gap: 0.5 }}>
                          <IconButton size="small" onClick={(e) => { e.stopPropagation(); onGenerateReport(template); }}>
                            <CloudDownloadIcon />
                          </IconButton>
                          <IconButton size="small" onClick={(e) => { e.stopPropagation(); onCloneTemplate(template); }}>
                            <FileCopyIcon />
                          </IconButton>
                        </Box>
                      </Box>
                    </CardContent>
                  </Card>
                </motion.div>
              </Grid>
            ))}
          </Grid>
        ) : (
          <List>
            {templates.map((template) => (
              <ListItem 
                key={template.id}
                button
                selected={selectedTemplate?.id === template.id}
                onClick={() => onSelectTemplate(template)}
                sx={{ borderRadius: 1, mb: 1, border: '1px solid', borderColor: 'divider' }}
              >
                <ListItemIcon>
                  <AssessmentIcon color={
                    template.category === 'PERFORMANCE' ? 'primary' : 
                    template.category === 'RISK' ? 'error' : 'action'
                  } />
                </ListItemIcon>
                <ListItemText
                  primary={template.name}
                  secondary={
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mt: 0.5 }}>
                      <Chip label={template.category} size="small" />
                      <Typography variant="caption">
                        {template.widgets?.length || 0} widgets
                      </Typography>
                      <Typography variant="caption">
                        Modified: {template.lastGenerated?.toLocaleDateString() || 'Never'}
                      </Typography>
                    </Box>
                  }
                />
                <Box sx={{ display: 'flex', gap: 1 }}>
                  <IconButton onClick={(e) => { e.stopPropagation(); onGenerateReport(template); }}>
                    <CloudDownloadIcon />
                  </IconButton>
                  <IconButton onClick={(e) => { e.stopPropagation(); onCloneTemplate(template); }}>
                    <FileCopyIcon />
                  </IconButton>
                  <IconButton onClick={(e) => { e.stopPropagation(); onDeleteTemplate(template.id); }}>
                    <DeleteIcon />
                  </IconButton>
                </Box>
              </ListItem>
            ))}
          </List>
        )}
      </Box>
    </Box>
  );
};

// Designer View Component
interface DesignerViewProps {
  selectedTemplate: ReportTemplate | null;
  onTemplateChange: (template: ReportTemplate) => void;
  onAddWidget: () => void;
  onRemoveWidget: (widgetId: string) => void;
  onDragStart: (widget: ReportWidget) => void;
  onDragEnd: () => void;
  onDrop: (x: number, y: number) => void;
  previewMode: boolean;
  onPreviewModeChange: (preview: boolean) => void;
  fullscreenWidget: string | null;
  onFullscreenWidget: (widgetId: string | null) => void;
}

const DesignerView: React.FC<DesignerViewProps> = ({
  selectedTemplate,
  onTemplateChange,
  onAddWidget,
  onRemoveWidget,
  onDragStart,
  onDragEnd,
  onDrop,
  previewMode,
  onPreviewModeChange,
  fullscreenWidget,
  onFullscreenWidget
}) => {
  if (!selectedTemplate) {
    return (
      <Box sx={{ 
        height: '100%', 
        display: 'flex', 
        alignItems: 'center', 
        justifyContent: 'center',
        flexDirection: 'column',
        gap: 2
      }}>
        <DashboardIcon sx={{ fontSize: 64, color: 'text.disabled' }} />
        <Typography variant="h5" color="text.secondary">
          Select a template to start designing
        </Typography>
        <Typography variant="body1" color="text.disabled">
          Choose a template from the Templates tab or create a new one
        </Typography>
      </Box>
    );
  }

  return (
    <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      {/* Designer Toolbar */}
      <Paper sx={{ p: 2, borderRadius: 0, borderBottom: '1px solid', borderBottomColor: 'divider' }}>
        <Box sx={{ display: 'flex', justifyContent: 'between', alignItems: 'center' }}>
          <Typography variant="h6">
            {selectedTemplate.name} - Designer
          </Typography>
          <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
            <FormControlLabel
              control={
                <Switch
                  checked={previewMode}
                  onChange={(e) => onPreviewModeChange(e.target.checked)}
                />
              }
              label="Preview Mode"
            />
            <Button
              variant="outlined"
              startIcon={<AddIcon />}
              onClick={onAddWidget}
            >
              Add Widget
            </Button>
            <Button
              variant="contained"
              startIcon={<SaveIcon />}
            >
              Save Changes
            </Button>
          </Box>
        </Box>
      </Paper>

      {/* Design Canvas */}
      <Box sx={{ flexGrow: 1, overflow: 'auto', p: 2, backgroundColor: 'grey.50' }}>
        <Box 
          sx={{ 
            minHeight: '100%',
            display: 'grid',
            gridTemplateColumns: `repeat(${selectedTemplate.layout.columns}, 1fr)`,
            gridTemplateRows: `repeat(${selectedTemplate.layout.rows}, minmax(100px, 1fr))`,
            gap: selectedTemplate.layout.spacing,
            position: 'relative'
          }}
        >
          {selectedTemplate.widgets.map((widget) => (
            <motion.div
              key={widget.id}
              layout
              style={{
                gridColumn: `${widget.position.x + 1} / span ${widget.position.width}`,
                gridRow: `${widget.position.y + 1} / span ${widget.position.height}`,
                cursor: previewMode ? 'default' : 'move'
              }}
              whileHover={previewMode ? {} : { scale: 1.02 }}
              onMouseDown={previewMode ? undefined : () => onDragStart(widget)}
              onMouseUp={onDragEnd}
            >
              <WidgetPreview
                widget={widget}
                isEditing={!previewMode}
                onRemove={() => onRemoveWidget(widget.id)}
                onFullscreen={() => onFullscreenWidget(widget.id)}
                isFullscreen={fullscreenWidget === widget.id}
              />
            </motion.div>
          ))}

          {/* Empty Grid Indicators */}
          {!previewMode && selectedTemplate.widgets.length === 0 && (
            <Box 
              sx={{ 
                gridColumn: '1 / -1', 
                gridRow: '1 / -1',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                border: '2px dashed',
                borderColor: 'divider',
                borderRadius: 2,
                minHeight: 400
              }}
            >
              <Box sx={{ textAlign: 'center' }}>
                <AddIcon sx={{ fontSize: 48, color: 'text.disabled', mb: 1 }} />
                <Typography variant="h6" color="text.secondary">
                  Add your first widget
                </Typography>
                <Typography variant="body2" color="text.disabled">
                  Click "Add Widget" to start building your report
                </Typography>
              </Box>
            </Box>
          )}
        </Box>
      </Box>
    </Box>
  );
};

// Widget Preview Component
interface WidgetPreviewProps {
  widget: ReportWidget;
  isEditing: boolean;
  onRemove: () => void;
  onFullscreen: () => void;
  isFullscreen: boolean;
}

const WidgetPreview: React.FC<WidgetPreviewProps> = ({
  widget,
  isEditing,
  onRemove,
  onFullscreen,
  isFullscreen
}) => {
  const getWidgetIcon = (type: WidgetType) => {
    switch (type) {
      case 'PERFORMANCE_CHART': return <TrendingUpIcon />;
      case 'RISK_METRICS': return <AssessmentIcon />;
      case 'ATTRIBUTION_TABLE': return <TableChartIcon />;
      case 'BENCHMARK_COMPARISON': return <BarChartIcon />;
      case 'HOLDINGS_TABLE': return <GridViewIcon />;
      case 'TRADE_SUMMARY': return <TimelineIcon />;
      case 'HEATMAP': return <DonutSmallIcon />;
      case 'GAUGE': return <PieChartIcon />;
      case 'SCORECARD': return <AnalyticsIcon />;
      default: return <ShowChartIcon />;
    }
  };

  const getWidgetContent = () => {
    // Mock widget content based on type
    switch (widget.type) {
      case 'PERFORMANCE_CHART':
        return (
          <Box sx={{ p: 2 }}>
            <Typography variant="h6" gutterBottom>Portfolio vs Benchmark</Typography>
            <Box sx={{ height: 200, display: 'flex', alignItems: 'center', justifyContent: 'center', backgroundColor: 'grey.100', borderRadius: 1 }}>
              <Typography color="text.secondary">Performance Chart Visualization</Typography>
            </Box>
          </Box>
        );
      case 'RISK_METRICS':
        return (
          <Box sx={{ p: 2 }}>
            <Typography variant="h6" gutterBottom>Risk Metrics</Typography>
            <Grid container spacing={2}>
              <Grid item xs={6}>
                <Typography variant="body2" color="text.secondary">VaR (95%)</Typography>
                <Typography variant="h5">-2.34%</Typography>
              </Grid>
              <Grid item xs={6}>
                <Typography variant="body2" color="text.secondary">Volatility</Typography>
                <Typography variant="h5">14.2%</Typography>
              </Grid>
              <Grid item xs={6}>
                <Typography variant="body2" color="text.secondary">Sharpe Ratio</Typography>
                <Typography variant="h5">1.42</Typography>
              </Grid>
              <Grid item xs={6}>
                <Typography variant="body2" color="text.secondary">Max Drawdown</Typography>
                <Typography variant="h5" color="error.main">-8.7%</Typography>
              </Grid>
            </Grid>
          </Box>
        );
      case 'SCORECARD':
        return (
          <Box sx={{ p: 2 }}>
            <Typography variant="h6" gutterBottom>Key Metrics</Typography>
            <Grid container spacing={1}>
              <Grid item xs={4}>
                <Card variant="outlined" sx={{ p: 1, textAlign: 'center' }}>
                  <Typography variant="caption" color="text.secondary">Return</Typography>
                  <Typography variant="h6" color="success.main">+12.4%</Typography>
                </Card>
              </Grid>
              <Grid item xs={4}>
                <Card variant="outlined" sx={{ p: 1, textAlign: 'center' }}>
                  <Typography variant="caption" color="text.secondary">Alpha</Typography>
                  <Typography variant="h6">+2.1%</Typography>
                </Card>
              </Grid>
              <Grid item xs={4}>
                <Card variant="outlined" sx={{ p: 1, textAlign: 'center' }}>
                  <Typography variant="caption" color="text.secondary">Beta</Typography>
                  <Typography variant="h6">0.85</Typography>
                </Card>
              </Grid>
            </Grid>
          </Box>
        );
      default:
        return (
          <Box sx={{ p: 2, height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <Box sx={{ textAlign: 'center' }}>
              {getWidgetIcon(widget.type)}
              <Typography variant="body1" sx={{ mt: 1 }}>
                {widget.type.replace(/_/g, ' ')}
              </Typography>
            </Box>
          </Box>
        );
    }
  };

  return (
    <Card 
      sx={{ 
        height: '100%', 
        display: 'flex', 
        flexDirection: 'column',
        ...widget.style,
        border: isEditing ? '2px dashed' : '1px solid',
        borderColor: isEditing ? 'primary.main' : 'divider',
        '&:hover': isEditing ? {
          borderColor: 'primary.dark',
          '& .widget-controls': {
            opacity: 1
          }
        } : {}
      }}
    >
      {/* Widget Header */}
      <Box sx={{ 
        display: 'flex', 
        justifyContent: 'between', 
        alignItems: 'center',
        p: 1,
        borderBottom: '1px solid',
        borderBottomColor: 'divider',
        minHeight: 48
      }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          {isEditing && <DragHandleIcon sx={{ color: 'text.disabled' }} />}
          {getWidgetIcon(widget.type)}
          <Typography variant="subtitle2" sx={{ fontWeight: 600 }}>
            {widget.title}
          </Typography>
        </Box>
        
        {isEditing && (
          <Box className="widget-controls" sx={{ opacity: 0, transition: 'opacity 0.2s', display: 'flex', gap: 0.5 }}>
            <IconButton size="small" onClick={onFullscreen}>
              <FullscreenIcon />
            </IconButton>
            <IconButton size="small">
              <SettingsIcon />
            </IconButton>
            <IconButton size="small" onClick={onRemove} color="error">
              <DeleteIcon />
            </IconButton>
          </Box>
        )}
      </Box>

      {/* Widget Content */}
      <Box sx={{ flexGrow: 1, overflow: 'hidden' }}>
        {getWidgetContent()}
      </Box>
    </Card>
  );
};

// Generations View Component
interface GenerationsViewProps {
  generations: ReportGeneration[];
  templates: ReportTemplate[];
  onRegenerateReport: (templateId: string) => void;
}

const GenerationsView: React.FC<GenerationsViewProps> = ({
  generations,
  templates,
  onRegenerateReport
}) => {
  const getStatusColor = (status: ReportGeneration['status']) => {
    switch (status) {
      case 'COMPLETED': return 'success';
      case 'GENERATING': return 'warning';
      case 'FAILED': return 'error';
      default: return 'default';
    }
  };

  const getStatusIcon = (status: ReportGeneration['status']) => {
    switch (status) {
      case 'COMPLETED': return <CheckIcon />;
      case 'GENERATING': return <CircularProgress size={16} />;
      case 'FAILED': return <ErrorIcon />;
      default: return <HourglassEmptyIcon />;
    }
  };

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h6" sx={{ mb: 3 }}>
        Report Generations ({generations.length})
      </Typography>

      {generations.length === 0 ? (
        <Box sx={{ 
          display: 'flex', 
          alignItems: 'center', 
          justifyContent: 'center',
          minHeight: 400,
          flexDirection: 'column',
          gap: 2
        }}>
          <CloudDownloadIcon sx={{ fontSize: 64, color: 'text.disabled' }} />
          <Typography variant="h6" color="text.secondary">
            No reports generated yet
          </Typography>
          <Typography variant="body1" color="text.disabled">
            Generate your first report from the Templates tab
          </Typography>
        </Box>
      ) : (
        <List>
          {generations.map((generation) => {
            const template = templates.find(t => t.id === generation.templateId);
            return (
              <ListItem 
                key={generation.id}
                sx={{ 
                  border: '1px solid',
                  borderColor: 'divider',
                  borderRadius: 1,
                  mb: 1
                }}
              >
                <ListItemIcon>
                  {getStatusIcon(generation.status)}
                </ListItemIcon>
                <ListItemText
                  primary={template?.name || 'Unknown Template'}
                  secondary={
                    <Box sx={{ mt: 1 }}>
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 1 }}>
                        <Chip 
                          label={generation.status} 
                          size="small"
                          color={getStatusColor(generation.status)}
                        />
                        <Typography variant="caption">
                          Format: {generation.outputFormat}
                        </Typography>
                        <Typography variant="caption">
                          Started: {generation.startTime.toLocaleString()}
                        </Typography>
                        {generation.endTime && (
                          <Typography variant="caption">
                            Completed: {generation.endTime.toLocaleString()}
                          </Typography>
                        )}
                      </Box>
                      {generation.status === 'GENERATING' && (
                        <LinearProgress 
                          variant="determinate" 
                          value={generation.progress} 
                          sx={{ width: 200 }}
                        />
                      )}
                      {generation.status === 'FAILED' && generation.error && (
                        <Alert severity="error" sx={{ mt: 1 }}>
                          {generation.error}
                        </Alert>
                      )}
                    </Box>
                  }
                />
                <Box sx={{ display: 'flex', gap: 1 }}>
                  {generation.status === 'COMPLETED' && (
                    <IconButton color="primary">
                      <DownloadIcon />
                    </IconButton>
                  )}
                  <IconButton 
                    onClick={() => onRegenerateReport(generation.templateId)}
                    disabled={generation.status === 'GENERATING'}
                  >
                    <RefreshIcon />
                  </IconButton>
                </Box>
              </ListItem>
            );
          })}
        </List>
      )}
    </Box>
  );
};

// Schedules View Component
interface SchedulesViewProps {
  templates: ReportTemplate[];
  onUpdateSchedule: (templateId: string, schedule: ReportSchedule) => void;
}

const SchedulesView: React.FC<SchedulesViewProps> = ({
  templates,
  onUpdateSchedule
}) => {
  const scheduledTemplates = templates.filter(t => t.schedule);

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h6" sx={{ mb: 3 }}>
        Scheduled Reports ({scheduledTemplates.length})
      </Typography>

      {scheduledTemplates.length === 0 ? (
        <Box sx={{ 
          display: 'flex', 
          alignItems: 'center', 
          justifyContent: 'center',
          minHeight: 400,
          flexDirection: 'column',
          gap: 2
        }}>
          <ScheduleIcon sx={{ fontSize: 64, color: 'text.disabled' }} />
          <Typography variant="h6" color="text.secondary">
            No scheduled reports
          </Typography>
          <Typography variant="body1" color="text.disabled">
            Set up schedules for automatic report generation
          </Typography>
        </Box>
      ) : (
        <List>
          {scheduledTemplates.map((template) => (
            <ListItem 
              key={template.id}
              sx={{ 
                border: '1px solid',
                borderColor: 'divider',
                borderRadius: 1,
                mb: 1
              }}
            >
              <ListItemIcon>
                <ScheduleIcon color={template.schedule!.isActive ? 'primary' : 'disabled'} />
              </ListItemIcon>
              <ListItemText
                primary={template.name}
                secondary={
                  <Box sx={{ mt: 1 }}>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 1 }}>
                      <Chip 
                        label={template.schedule!.frequency} 
                        size="small"
                        color={template.schedule!.isActive ? 'primary' : 'default'}
                      />
                      <Typography variant="caption">
                        Time: {template.schedule!.time} {template.schedule!.timezone}
                      </Typography>
                      {template.schedule!.nextRun && (
                        <Typography variant="caption">
                          Next: {template.schedule!.nextRun.toLocaleString()}
                        </Typography>
                      )}
                    </Box>
                    {template.recipients && template.recipients.length > 0 && (
                      <Typography variant="caption" color="text.secondary">
                        Recipients: {template.recipients.join(', ')}
                      </Typography>
                    )}
                  </Box>
                }
              />
              <Box sx={{ display: 'flex', gap: 1 }}>
                <Switch
                  checked={template.schedule!.isActive}
                  onChange={(e) => {
                    const updatedSchedule = { ...template.schedule!, isActive: e.target.checked };
                    onUpdateSchedule(template.id, updatedSchedule);
                  }}
                />
                <IconButton>
                  <EditIcon />
                </IconButton>
              </Box>
            </ListItem>
          ))}
        </List>
      )}
    </Box>
  );
};

// Widget Configuration Form Component
interface WidgetConfigurationFormProps {
  widget: Partial<ReportWidget>;
  onChange: (widget: Partial<ReportWidget>) => void;
  widgetTypes: Array<{
    value: WidgetType;
    label: string;
    icon: React.ComponentType;
    category: string;
  }>;
}

const WidgetConfigurationForm: React.FC<WidgetConfigurationFormProps> = ({
  widget,
  onChange,
  widgetTypes
}) => {
  return (
    <Grid container spacing={3}>
      <Grid item xs={12} md={6}>
        <TextField
          fullWidth
          label="Widget Title"
          value={widget.title || ''}
          onChange={(e) => onChange({ ...widget, title: e.target.value })}
          required
        />
      </Grid>
      <Grid item xs={12} md={6}>
        <FormControl fullWidth>
          <InputLabel>Widget Type</InputLabel>
          <Select
            value={widget.type || ''}
            label="Widget Type"
            onChange={(e) => onChange({ ...widget, type: e.target.value as WidgetType })}
          >
            {widgetTypes.map((type) => (
              <MenuItem key={type.value} value={type.value}>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                  <type.icon />
                  {type.label}
                </Box>
              </MenuItem>
            ))}
          </Select>
        </FormControl>
      </Grid>

      <Grid item xs={12}>
        <Typography variant="subtitle1" sx={{ mb: 2 }}>Position & Size</Typography>
        <Grid container spacing={2}>
          <Grid item xs={3}>
            <TextField
              fullWidth
              type="number"
              label="X Position"
              value={widget.position?.x || 0}
              onChange={(e) => onChange({
                ...widget,
                position: { ...widget.position!, x: parseInt(e.target.value) || 0 }
              })}
            />
          </Grid>
          <Grid item xs={3}>
            <TextField
              fullWidth
              type="number"
              label="Y Position"
              value={widget.position?.y || 0}
              onChange={(e) => onChange({
                ...widget,
                position: { ...widget.position!, y: parseInt(e.target.value) || 0 }
              })}
            />
          </Grid>
          <Grid item xs={3}>
            <TextField
              fullWidth
              type="number"
              label="Width"
              value={widget.position?.width || 4}
              onChange={(e) => onChange({
                ...widget,
                position: { ...widget.position!, width: parseInt(e.target.value) || 4 }
              })}
            />
          </Grid>
          <Grid item xs={3}>
            <TextField
              fullWidth
              type="number"
              label="Height"
              value={widget.position?.height || 3}
              onChange={(e) => onChange({
                ...widget,
                position: { ...widget.position!, height: parseInt(e.target.value) || 3 }
              })}
            />
          </Grid>
        </Grid>
      </Grid>

      <Grid item xs={12}>
        <Typography variant="subtitle1" sx={{ mb: 2 }}>Configuration</Typography>
        <Grid container spacing={2}>
          <Grid item xs={12} md={4}>
            <FormControl fullWidth>
              <InputLabel>Chart Type</InputLabel>
              <Select
                value={widget.configuration?.chartType || 'LINE'}
                label="Chart Type"
                onChange={(e) => onChange({
                  ...widget,
                  configuration: { ...widget.configuration!, chartType: e.target.value as any }
                })}
              >
                <MenuItem value="LINE">Line Chart</MenuItem>
                <MenuItem value="BAR">Bar Chart</MenuItem>
                <MenuItem value="PIE">Pie Chart</MenuItem>
                <MenuItem value="AREA">Area Chart</MenuItem>
                <MenuItem value="TABLE">Table</MenuItem>
                <MenuItem value="METRIC">Single Metric</MenuItem>
              </Select>
            </FormControl>
          </Grid>
          <Grid item xs={12} md={4}>
            <FormControl fullWidth>
              <InputLabel>Time Range</InputLabel>
              <Select
                value={widget.configuration?.timeRange || '1M'}
                label="Time Range"
                onChange={(e) => onChange({
                  ...widget,
                  configuration: { ...widget.configuration!, timeRange: e.target.value }
                })}
              >
                <MenuItem value="1D">1 Day</MenuItem>
                <MenuItem value="1W">1 Week</MenuItem>
                <MenuItem value="1M">1 Month</MenuItem>
                <MenuItem value="3M">3 Months</MenuItem>
                <MenuItem value="6M">6 Months</MenuItem>
                <MenuItem value="1Y">1 Year</MenuItem>
                <MenuItem value="YTD">Year to Date</MenuItem>
              </Select>
            </FormControl>
          </Grid>
          <Grid item xs={12} md={4}>
            <FormControl fullWidth>
              <InputLabel>Number Format</InputLabel>
              <Select
                value={widget.configuration?.formatting?.numberFormat || 'DECIMAL'}
                label="Number Format"
                onChange={(e) => onChange({
                  ...widget,
                  configuration: {
                    ...widget.configuration!,
                    formatting: { 
                      ...widget.configuration!.formatting!,
                      numberFormat: e.target.value as any 
                    }
                  }
                })}
              >
                <MenuItem value="DECIMAL">Decimal</MenuItem>
                <MenuItem value="PERCENTAGE">Percentage</MenuItem>
                <MenuItem value="CURRENCY">Currency</MenuItem>
                <MenuItem value="INTEGER">Integer</MenuItem>
              </Select>
            </FormControl>
          </Grid>
        </Grid>
      </Grid>

      <Grid item xs={12}>
        <FormGroup>
          <FormControlLabel
            control={
              <Checkbox
                checked={widget.configuration?.formatting?.showLegend || false}
                onChange={(e) => onChange({
                  ...widget,
                  configuration: {
                    ...widget.configuration!,
                    formatting: { 
                      ...widget.configuration!.formatting!,
                      showLegend: e.target.checked 
                    }
                  }
                })}
              />
            }
            label="Show Legend"
          />
          <FormControlLabel
            control={
              <Checkbox
                checked={widget.configuration?.formatting?.showGrid || false}
                onChange={(e) => onChange({
                  ...widget,
                  configuration: {
                    ...widget.configuration!,
                    formatting: { 
                      ...widget.configuration!.formatting!,
                      showGrid: e.target.checked 
                    }
                  }
                })}
              />
            }
            label="Show Grid"
          />
          <FormControlLabel
            control={
              <Switch
                checked={widget.isVisible !== false}
                onChange={(e) => onChange({ ...widget, isVisible: e.target.checked })}
              />
            }
            label="Widget Visible"
          />
        </FormGroup>
      </Grid>
    </Grid>
  );
};

export default ReportingStudio;