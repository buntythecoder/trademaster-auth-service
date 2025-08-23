import React, { useState, useRef } from 'react';
import { Upload, Download, Play, Pause, AlertTriangle, CheckCircle, XCircle, Clock, TrendingUp, BarChart3, Settings, FileText } from 'lucide-react';

interface OrderRequest {
  symbol: string;
  side: 'BUY' | 'SELL';
  orderType: 'MARKET' | 'LIMIT' | 'STOP' | 'STOP_LIMIT';
  quantity: number;
  price?: number;
  stopPrice?: number;
  timeInForce?: 'DAY' | 'GTC' | 'IOC' | 'FOK';
  notes?: string;
}

interface BulkOrderRequest {
  orders: OrderRequest[];
  executionStrategy: 'SIMULTANEOUS' | 'SEQUENTIAL' | 'SCHEDULED';
  scheduledTime?: Date;
  positionSizing: 'EQUAL_WEIGHT' | 'PERCENTAGE' | 'FIXED_AMOUNT';
  totalAmount?: number;
  delayBetweenOrders?: number; // seconds
  stopOnFailure?: boolean;
  riskChecks?: boolean;
}

interface BulkOrderStatus {
  orderId: string;
  symbol: string;
  status: 'PENDING' | 'SUBMITTED' | 'FILLED' | 'REJECTED' | 'CANCELLED';
  errorMessage?: string;
  submittedAt?: Date;
  filledAt?: Date;
  executionPrice?: number;
  fees?: number;
}

interface BulkExecutionStatus {
  requestId: string;
  totalOrders: number;
  successfulOrders: number;
  failedOrders: number;
  pendingOrders: number;
  executionProgress: number;
  orders: BulkOrderStatus[];
  startTime: Date;
  endTime?: Date;
  totalValue?: number;
  totalFees?: number;
}

interface CsvUploaderProps {
  onUpload: (file: File) => void;
  accept: string;
  className?: string;
  disabled?: boolean;
}

function CsvUploader({ onUpload, accept, className, disabled }: CsvUploaderProps) {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [dragActive, setDragActive] = useState(false);

  const handleDrag = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === "dragenter" || e.type === "dragover") {
      setDragActive(true);
    } else if (e.type === "dragleave") {
      setDragActive(false);
    }
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);
    
    if (disabled) return;
    
    const files = e.dataTransfer.files;
    if (files && files[0]) {
      onUpload(files[0]);
    }
  };

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (files && files[0]) {
      onUpload(files[0]);
    }
  };

  return (
    <div className={`${className}`}>
      <div
        className={`relative border-2 border-dashed rounded-xl p-8 text-center transition-colors ${
          dragActive 
            ? 'border-purple-500 bg-purple-500/10' 
            : 'border-slate-600 hover:border-slate-500'
        } ${disabled ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer'}`}
        onDragEnter={handleDrag}
        onDragLeave={handleDrag}
        onDragOver={handleDrag}
        onDrop={handleDrop}
        onClick={() => !disabled && fileInputRef.current?.click()}
      >
        <Upload className="w-12 h-12 text-slate-400 mx-auto mb-4" />
        <div className="text-white font-medium mb-2">
          Drop CSV file here or click to browse
        </div>
        <div className="text-sm text-slate-400">
          Maximum file size: 10MB
        </div>
        
        <input
          ref={fileInputRef}
          type="file"
          accept={accept}
          onChange={handleFileSelect}
          className="hidden"
          disabled={disabled}
        />
      </div>
    </div>
  );
}

function BulkOrderPreview({ orders }: { orders: OrderRequest[] }) {
  const totalValue = orders.reduce((sum, order) => {
    const price = order.price || 0; // Market orders will need current price
    return sum + (order.quantity * price);
  }, 0);

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h4 className="text-lg font-semibold text-white">Order Preview</h4>
        <div className="text-sm text-slate-400">
          {orders.length} orders • ₹{totalValue.toFixed(2)} total value
        </div>
      </div>
      
      <div className="max-h-60 overflow-y-auto space-y-2">
        {orders.slice(0, 10).map((order, index) => (
          <div
            key={index}
            className="flex items-center justify-between p-3 bg-slate-800/50 rounded-lg border border-slate-700/50"
          >
            <div className="flex items-center space-x-4">
              <div className="w-8 h-8 bg-purple-500/20 rounded-lg flex items-center justify-center">
                <span className="text-xs text-purple-400 font-medium">{index + 1}</span>
              </div>
              <div>
                <div className="text-white font-medium">
                  {order.symbol} • {order.side}
                </div>
                <div className="text-sm text-slate-400">
                  {order.quantity} @ {order.price ? `₹${order.price}` : 'Market'}
                </div>
              </div>
            </div>
            <div className="text-right">
              <div className="text-white font-medium">
                ₹{((order.price || 0) * order.quantity).toFixed(2)}
              </div>
              <div className="text-xs text-slate-400">{order.orderType}</div>
            </div>
          </div>
        ))}
        
        {orders.length > 10 && (
          <div className="text-center py-2">
            <div className="text-sm text-slate-400">
              ... and {orders.length - 10} more orders
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

function BulkExecutionStatusDisplay({ status }: { status: BulkExecutionStatus }) {
  const progress = (status.executionProgress * 100).toFixed(1);
  const duration = status.endTime 
    ? (status.endTime.getTime() - status.startTime.getTime()) / 1000
    : (Date.now() - status.startTime.getTime()) / 1000;

  return (
    <div className="glass-card rounded-2xl p-6">
      <div className="flex items-center justify-between mb-6">
        <h3 className="text-xl font-bold text-white">Execution Status</h3>
        <div className="text-right">
          <div className="text-2xl font-bold text-white">{progress}%</div>
          <div className="text-sm text-slate-400">Complete</div>
        </div>
      </div>

      {/* Progress Bar */}
      <div className="w-full bg-slate-700/50 rounded-full h-4 mb-6">
        <div
          className="bg-gradient-to-r from-purple-500 to-blue-500 h-4 rounded-full transition-all duration-300"
          style={{ width: `${Math.min(parseFloat(progress), 100)}%` }}
        />
      </div>

      {/* Status Cards */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
        <div className="bg-slate-800/50 rounded-lg p-4 border border-slate-700/50">
          <div className="flex items-center space-x-2 mb-2">
            <Clock className="w-4 h-4 text-yellow-400" />
            <span className="text-sm text-slate-400">Pending</span>
          </div>
          <div className="text-2xl font-bold text-white">{status.pendingOrders}</div>
        </div>
        
        <div className="bg-slate-800/50 rounded-lg p-4 border border-slate-700/50">
          <div className="flex items-center space-x-2 mb-2">
            <CheckCircle className="w-4 h-4 text-green-400" />
            <span className="text-sm text-slate-400">Successful</span>
          </div>
          <div className="text-2xl font-bold text-green-400">{status.successfulOrders}</div>
        </div>
        
        <div className="bg-slate-800/50 rounded-lg p-4 border border-slate-700/50">
          <div className="flex items-center space-x-2 mb-2">
            <XCircle className="w-4 h-4 text-red-400" />
            <span className="text-sm text-slate-400">Failed</span>
          </div>
          <div className="text-2xl font-bold text-red-400">{status.failedOrders}</div>
        </div>
        
        <div className="bg-slate-800/50 rounded-lg p-4 border border-slate-700/50">
          <div className="flex items-center space-x-2 mb-2">
            <TrendingUp className="w-4 h-4 text-blue-400" />
            <span className="text-sm text-slate-400">Total</span>
          </div>
          <div className="text-2xl font-bold text-white">{status.totalOrders}</div>
        </div>
      </div>

      {/* Execution Details */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
        <div>
          <div className="text-sm text-slate-400">Execution Time</div>
          <div className="text-lg font-semibold text-white">
            {duration.toFixed(1)}s
          </div>
        </div>
        <div>
          <div className="text-sm text-slate-400">Total Value</div>
          <div className="text-lg font-semibold text-white">
            ₹{(status.totalValue || 0).toFixed(2)}
          </div>
        </div>
        <div>
          <div className="text-sm text-slate-400">Total Fees</div>
          <div className="text-lg font-semibold text-white">
            ₹{(status.totalFees || 0).toFixed(2)}
          </div>
        </div>
      </div>

      {/* Order Status List */}
      <div className="space-y-2 max-h-60 overflow-y-auto">
        {status.orders.map((order, index) => (
          <div
            key={order.orderId || index}
            className="flex items-center justify-between p-3 bg-slate-800/50 rounded-lg border border-slate-700/50"
          >
            <div className="flex items-center space-x-3">
              <div className={`w-2 h-2 rounded-full ${
                order.status === 'FILLED' ? 'bg-green-400' :
                order.status === 'REJECTED' || order.status === 'CANCELLED' ? 'bg-red-400' :
                order.status === 'SUBMITTED' ? 'bg-blue-400' : 'bg-yellow-400'
              }`} />
              <div>
                <div className="text-white font-medium">{order.symbol}</div>
                <div className="text-xs text-slate-400">
                  {order.status}
                  {order.errorMessage && ` • ${order.errorMessage}`}
                </div>
              </div>
            </div>
            <div className="text-right">
              {order.executionPrice && (
                <div className="text-white font-medium">₹{order.executionPrice}</div>
              )}
              {order.filledAt && (
                <div className="text-xs text-slate-400">
                  {order.filledAt.toLocaleTimeString('en-IN', { hour12: false })}
                </div>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

export function BulkOrderManagement() {
  const [bulkRequest, setBulkRequest] = useState<BulkOrderRequest>({
    orders: [],
    executionStrategy: 'SIMULTANEOUS',
    positionSizing: 'EQUAL_WEIGHT',
    delayBetweenOrders: 1,
    stopOnFailure: true,
    riskChecks: true
  });
  const [executionStatus, setExecutionStatus] = useState<BulkExecutionStatus | null>(null);
  const [processing, setProcessing] = useState(false);
  const [showAdvanced, setShowAdvanced] = useState(false);
  const [uploadError, setUploadError] = useState<string | null>(null);

  const parseCsvFile = async (file: File): Promise<any[]> => {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = (e) => {
        try {
          const csv = e.target?.result as string;
          const lines = csv.split('\n').filter(line => line.trim());
          const headers = lines[0].split(',').map(h => h.trim());
          
          const data = lines.slice(1).map(line => {
            const values = line.split(',').map(v => v.trim());
            const row: any = {};
            headers.forEach((header, index) => {
              row[header] = values[index];
            });
            return row;
          });
          
          resolve(data);
        } catch (error) {
          reject(error);
        }
      };
      reader.onerror = () => reject(new Error('Failed to read file'));
      reader.readAsText(file);
    });
  };

  const parseOrderFromCsv = (row: any): OrderRequest => {
    return {
      symbol: row.Symbol || row.symbol,
      side: (row.Side || row.side)?.toUpperCase() as 'BUY' | 'SELL',
      orderType: (row.OrderType || row.orderType || 'LIMIT').toUpperCase() as 'MARKET' | 'LIMIT' | 'STOP' | 'STOP_LIMIT',
      quantity: parseInt(row.Quantity || row.quantity) || 0,
      price: row.Price || row.price ? parseFloat(row.Price || row.price) : undefined,
      stopPrice: row.StopPrice || row.stopPrice ? parseFloat(row.StopPrice || row.stopPrice) : undefined,
      timeInForce: (row.TimeInForce || row.timeInForce || 'DAY').toUpperCase() as 'DAY' | 'GTC' | 'IOC' | 'FOK',
      notes: row.Notes || row.notes
    };
  };

  const handleCsvUpload = async (file: File) => {
    setUploadError(null);
    try {
      const csvData = await parseCsvFile(file);
      const orders = csvData.map(row => parseOrderFromCsv(row)).filter(order => 
        order.symbol && order.side && order.quantity > 0
      );
      
      if (orders.length === 0) {
        setUploadError('No valid orders found in CSV file');
        return;
      }
      
      setBulkRequest(prev => ({ ...prev, orders }));
    } catch (error) {
      setUploadError('Invalid CSV format. Please check the file structure.');
    }
  };

  const downloadTemplate = () => {
    const template = `Symbol,Side,OrderType,Quantity,Price,StopPrice,TimeInForce,Notes
RELIANCE,BUY,LIMIT,100,2500.00,,DAY,Sample order 1
TCS,SELL,MARKET,50,,,GTC,Sample order 2
INFY,BUY,STOP_LIMIT,75,1500.00,1520.00,DAY,Sample order 3`;
    
    const blob = new Blob([template], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'bulk_orders_template.csv';
    a.click();
    window.URL.revokeObjectURL(url);
  };

  const executeBulkOrders = async () => {
    if (bulkRequest.orders.length === 0) {
      return;
    }

    setProcessing(true);
    try {
      // Simulate API call - replace with actual service call
      const response = await new Promise<BulkExecutionStatus>((resolve) => {
        setTimeout(() => {
          const mockStatus: BulkExecutionStatus = {
            requestId: `bulk_${Date.now()}`,
            totalOrders: bulkRequest.orders.length,
            successfulOrders: 0,
            failedOrders: 0,
            pendingOrders: bulkRequest.orders.length,
            executionProgress: 0,
            orders: bulkRequest.orders.map((order, index) => ({
              orderId: `order_${index}`,
              symbol: order.symbol,
              status: 'PENDING'
            })),
            startTime: new Date(),
            totalValue: bulkRequest.orders.reduce((sum, order) => 
              sum + (order.quantity * (order.price || 0)), 0),
            totalFees: 0
          };
          resolve(mockStatus);
        }, 1000);
      });
      
      setExecutionStatus(response);
      
      // Simulate progressive execution
      monitorBulkExecution(response.requestId);
    } catch (error) {
      console.error('Bulk order execution failed:', error);
    } finally {
      setProcessing(false);
    }
  };

  const monitorBulkExecution = (requestId: string) => {
    let completed = 0;
    const total = bulkRequest.orders.length;
    
    const interval = setInterval(() => {
      completed += Math.floor(Math.random() * 3) + 1;
      
      if (completed >= total) {
        completed = total;
        clearInterval(interval);
      }
      
      const successRate = 0.85; // 85% success rate simulation
      const successful = Math.floor(completed * successRate);
      const failed = completed - successful;
      
      setExecutionStatus(prev => prev ? {
        ...prev,
        successfulOrders: successful,
        failedOrders: failed,
        pendingOrders: total - completed,
        executionProgress: completed / total,
        endTime: completed === total ? new Date() : undefined,
        orders: prev.orders.map((order, index) => ({
          ...order,
          status: index < completed 
            ? (Math.random() > successRate ? 'REJECTED' : 'FILLED')
            : 'PENDING',
          filledAt: index < successful ? new Date() : undefined,
          executionPrice: index < successful ? Math.random() * 1000 + 500 : undefined
        }))
      } : null);
    }, 1000);
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-white">Bulk Order Management</h1>
          <p className="text-slate-400 mt-1">Execute multiple orders efficiently</p>
        </div>
        <button
          onClick={downloadTemplate}
          className="px-4 py-2 bg-slate-700 hover:bg-slate-600 text-white rounded-lg transition-colors flex items-center space-x-2"
        >
          <Download className="w-4 h-4" />
          <span>Download Template</span>
        </button>
      </div>

      {/* Bulk Order Configuration */}
      <div className="glass-card rounded-2xl p-6">
        <h2 className="text-xl font-bold text-white mb-6">Order Configuration</h2>
        
        {/* CSV Upload */}
        <div className="mb-6">
          <label className="block text-sm font-medium text-slate-300 mb-2">
            Upload Orders CSV
          </label>
          <CsvUploader
            onUpload={handleCsvUpload}
            accept=".csv"
            className="w-full"
            disabled={processing}
          />
          {uploadError && (
            <div className="mt-2 text-sm text-red-400 flex items-center space-x-1">
              <AlertTriangle className="w-4 h-4" />
              <span>{uploadError}</span>
            </div>
          )}
          <div className="text-xs text-slate-400 mt-2">
            Required columns: Symbol, Side, Quantity. Optional: Price, OrderType, StopPrice, TimeInForce, Notes
          </div>
        </div>

        {/* Execution Strategy */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-6">
          <div>
            <label className="block text-sm font-medium text-slate-300 mb-2">
              Execution Strategy
            </label>
            <select
              value={bulkRequest.executionStrategy}
              onChange={(e) => setBulkRequest(prev => ({
                ...prev,
                executionStrategy: e.target.value as any
              }))}
              className="w-full px-4 py-3 bg-slate-800/50 border border-slate-600/50 rounded-lg text-white"
              disabled={processing}
            >
              <option value="SIMULTANEOUS">Simultaneous</option>
              <option value="SEQUENTIAL">Sequential</option>
              <option value="SCHEDULED">Scheduled</option>
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-300 mb-2">
              Position Sizing
            </label>
            <select
              value={bulkRequest.positionSizing}
              onChange={(e) => setBulkRequest(prev => ({
                ...prev,
                positionSizing: e.target.value as any
              }))}
              className="w-full px-4 py-3 bg-slate-800/50 border border-slate-600/50 rounded-lg text-white"
              disabled={processing}
            >
              <option value="EQUAL_WEIGHT">Equal Weight</option>
              <option value="PERCENTAGE">Percentage</option>
              <option value="FIXED_AMOUNT">Fixed Amount</option>
            </select>
          </div>
        </div>

        {/* Advanced Options */}
        <div className="mb-6">
          <button
            onClick={() => setShowAdvanced(!showAdvanced)}
            className="flex items-center space-x-2 text-purple-400 hover:text-purple-300 transition-colors"
          >
            <Settings className="w-4 h-4" />
            <span>Advanced Options</span>
          </button>
          
          {showAdvanced && (
            <div className="mt-4 p-4 bg-slate-800/50 rounded-lg border border-slate-700/50 space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-slate-300 mb-2">
                    Delay Between Orders (seconds)
                  </label>
                  <input
                    type="number"
                    min="0"
                    max="60"
                    value={bulkRequest.delayBetweenOrders}
                    onChange={(e) => setBulkRequest(prev => ({
                      ...prev,
                      delayBetweenOrders: parseInt(e.target.value) || 0
                    }))}
                    className="w-full px-3 py-2 bg-slate-700/50 border border-slate-600/50 rounded-lg text-white"
                    disabled={processing}
                  />
                </div>
                
                <div>
                  <label className="block text-sm font-medium text-slate-300 mb-2">
                    Total Amount (₹)
                  </label>
                  <input
                    type="number"
                    min="0"
                    value={bulkRequest.totalAmount || ''}
                    onChange={(e) => setBulkRequest(prev => ({
                      ...prev,
                      totalAmount: parseFloat(e.target.value) || undefined
                    }))}
                    className="w-full px-3 py-2 bg-slate-700/50 border border-slate-600/50 rounded-lg text-white"
                    disabled={processing}
                    placeholder="Optional for position sizing"
                  />
                </div>
              </div>
              
              <div className="flex items-center space-x-6">
                <label className="flex items-center space-x-2 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={bulkRequest.stopOnFailure}
                    onChange={(e) => setBulkRequest(prev => ({
                      ...prev,
                      stopOnFailure: e.target.checked
                    }))}
                    className="rounded border-slate-600 bg-slate-700 text-purple-500 focus:ring-purple-500"
                    disabled={processing}
                  />
                  <span className="text-sm text-slate-300">Stop on failure</span>
                </label>
                
                <label className="flex items-center space-x-2 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={bulkRequest.riskChecks}
                    onChange={(e) => setBulkRequest(prev => ({
                      ...prev,
                      riskChecks: e.target.checked
                    }))}
                    className="rounded border-slate-600 bg-slate-700 text-purple-500 focus:ring-purple-500"
                    disabled={processing}
                  />
                  <span className="text-sm text-slate-300">Enable risk checks</span>
                </label>
              </div>
            </div>
          )}
        </div>

        {/* Order Preview */}
        {bulkRequest.orders.length > 0 && (
          <div className="mb-6">
            <BulkOrderPreview orders={bulkRequest.orders} />
          </div>
        )}

        {/* Execute Button */}
        <button
          onClick={executeBulkOrders}
          disabled={bulkRequest.orders.length === 0 || processing}
          className="w-full px-6 py-4 bg-purple-500 hover:bg-purple-600 disabled:opacity-50 disabled:cursor-not-allowed text-white rounded-lg font-medium transition-colors flex items-center justify-center space-x-2"
        >
          {processing ? (
            <>
              <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
              <span>Processing...</span>
            </>
          ) : (
            <>
              <Play className="w-5 h-5" />
              <span>Execute {bulkRequest.orders.length} Orders</span>
            </>
          )}
        </button>
      </div>

      {/* Execution Status */}
      {executionStatus && (
        <BulkExecutionStatusDisplay status={executionStatus} />
      )}

      {/* Help Section */}
      <div className="glass-card rounded-xl p-6">
        <h3 className="text-lg font-semibold text-white mb-4 flex items-center">
          <FileText className="w-5 h-5 mr-2" />
          Bulk Order Help
        </h3>
        
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div>
            <h4 className="text-white font-medium mb-2">Execution Strategies</h4>
            <ul className="space-y-1 text-sm text-slate-400">
              <li><strong>Simultaneous:</strong> All orders placed at once</li>
              <li><strong>Sequential:</strong> Orders placed one after another</li>
              <li><strong>Scheduled:</strong> Orders placed at specified time</li>
            </ul>
          </div>
          
          <div>
            <h4 className="text-white font-medium mb-2">Position Sizing</h4>
            <ul className="space-y-1 text-sm text-slate-400">
              <li><strong>Equal Weight:</strong> Equal amount per order</li>
              <li><strong>Percentage:</strong> Based on portfolio percentage</li>
              <li><strong>Fixed Amount:</strong> Use specified quantities</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  );
}

export default BulkOrderManagement;