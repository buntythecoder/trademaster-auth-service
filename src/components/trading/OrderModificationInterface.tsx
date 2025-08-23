import React, { useState, useEffect } from 'react';
import { Edit3, X, AlertTriangle, Clock, CheckCircle, History, DollarSign } from 'lucide-react';

interface Order {
  id: string;
  symbol: string;
  side: 'BUY' | 'SELL';
  orderType: 'MARKET' | 'LIMIT' | 'STOP' | 'STOP_LIMIT';
  status: OrderStatus;
  quantity: number;
  price?: number;
  stopPrice?: number;
  filledQuantity: number;
  averagePrice?: number;
  createdAt: Date;
  lastModified?: Date;
}

type OrderStatus = 
  | 'PENDING'
  | 'ACKNOWLEDGED' 
  | 'PARTIALLY_FILLED'
  | 'FILLED'
  | 'CANCELLED'
  | 'REJECTED'
  | 'EXPIRED';

interface OrderModificationRequest {
  orderId: string;
  newQuantity?: number;
  newPrice?: number;
  newStopPrice?: number;
  modificationType: 'QUANTITY' | 'PRICE' | 'STOP_PRICE' | 'CANCEL';
  reason?: string;
}

interface ModificationHistory {
  id: string;
  orderId: string;
  modificationType: string;
  oldValue: any;
  newValue: any;
  reason?: string;
  timestamp: Date;
  status: 'SUCCESS' | 'FAILED' | 'PENDING';
  errorMessage?: string;
}

interface OrderModificationInterfaceProps {
  order: Order;
  onModify: (modification: OrderModificationRequest) => Promise<void>;
  onCancel: (orderId: string) => Promise<void>;
  modificationHistory?: ModificationHistory[];
  disabled?: boolean;
}

interface ConfirmationModalProps {
  isOpen: boolean;
  title: string;
  message: string;
  onConfirm: () => void;
  onCancel: () => void;
  loading?: boolean;
  confirmText?: string;
  cancelText?: string;
}

function ConfirmationModal({
  isOpen,
  title,
  message,
  onConfirm,
  onCancel,
  loading = false,
  confirmText = "Confirm",
  cancelText = "Cancel"
}: ConfirmationModalProps) {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
      <div className="bg-slate-800 rounded-xl border border-slate-700 p-6 max-w-md w-full">
        <div className="flex items-center space-x-3 mb-4">
          <AlertTriangle className="w-6 h-6 text-yellow-400" />
          <h3 className="text-lg font-semibold text-white">{title}</h3>
        </div>
        
        <p className="text-slate-300 mb-6">{message}</p>
        
        <div className="flex space-x-3">
          <button
            onClick={onCancel}
            disabled={loading}
            className="flex-1 px-4 py-2 bg-slate-700 hover:bg-slate-600 disabled:opacity-50 text-white rounded-lg transition-colors"
          >
            {cancelText}
          </button>
          <button
            onClick={onConfirm}
            disabled={loading}
            className="flex-1 px-4 py-2 bg-red-500 hover:bg-red-600 disabled:opacity-50 text-white rounded-lg transition-colors flex items-center justify-center"
          >
            {loading ? (
              <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
            ) : (
              confirmText
            )}
          </button>
        </div>
      </div>
    </div>
  );
}

function ModificationHistoryTable({ history }: { history: ModificationHistory[] }) {
  if (!history || history.length === 0) {
    return (
      <div className="text-center py-8">
        <History className="w-12 h-12 text-slate-600 mx-auto mb-3" />
        <div className="text-slate-400">No modification history</div>
        <div className="text-sm text-slate-500 mt-1">
          Order modifications will appear here
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-3">
      {history.map((mod) => (
        <div
          key={mod.id}
          className="flex items-center justify-between p-3 bg-slate-800/50 rounded-lg border border-slate-700/50"
        >
          <div className="flex items-center space-x-3">
            <div className={`w-2 h-2 rounded-full ${
              mod.status === 'SUCCESS' ? 'bg-green-400' :
              mod.status === 'FAILED' ? 'bg-red-400' : 'bg-yellow-400'
            }`} />
            <div>
              <div className="text-white font-medium">
                {mod.modificationType.replace('_', ' ')}
              </div>
              <div className="text-xs text-slate-400">
                {mod.oldValue} → {mod.newValue}
              </div>
              {mod.reason && (
                <div className="text-xs text-slate-500 mt-1">
                  {mod.reason}
                </div>
              )}
            </div>
          </div>
          <div className="text-right">
            <div className="text-sm text-slate-400">
              {mod.timestamp.toLocaleTimeString('en-IN', { 
                hour12: false,
                hour: '2-digit',
                minute: '2-digit'
              })}
            </div>
            {mod.status === 'FAILED' && mod.errorMessage && (
              <div className="text-xs text-red-400 mt-1">
                {mod.errorMessage}
              </div>
            )}
          </div>
        </div>
      ))}
    </div>
  );
}

export function OrderModificationInterface({ 
  order, 
  onModify, 
  onCancel,
  modificationHistory = [],
  disabled = false
}: OrderModificationInterfaceProps) {
  const [modificationForm, setModificationForm] = useState<Partial<OrderModificationRequest>>({
    orderId: order.id,
    newQuantity: order.quantity - order.filledQuantity, // Only remaining quantity can be modified
    newPrice: order.price,
    newStopPrice: order.stopPrice
  });
  const [showConfirmation, setShowConfirmation] = useState(false);
  const [modifying, setModifying] = useState(false);
  const [activeTab, setActiveTab] = useState<'modify' | 'history'>('modify');
  const [modificationType, setModificationType] = useState<'QUANTITY' | 'PRICE' | 'STOP_PRICE'>('QUANTITY');

  useEffect(() => {
    // Reset form when order changes
    setModificationForm({
      orderId: order.id,
      newQuantity: order.quantity - order.filledQuantity,
      newPrice: order.price,
      newStopPrice: order.stopPrice
    });
  }, [order]);

  const canModify = !disabled && (
    order.status === 'ACKNOWLEDGED' || 
    order.status === 'PARTIALLY_FILLED' || 
    order.status === 'PENDING'
  );

  const hasChanges = () => {
    switch (modificationType) {
      case 'QUANTITY':
        return modificationForm.newQuantity !== (order.quantity - order.filledQuantity);
      case 'PRICE':
        return modificationForm.newPrice !== order.price;
      case 'STOP_PRICE':
        return modificationForm.newStopPrice !== order.stopPrice;
      default:
        return false;
    }
  };

  const handleModification = async () => {
    if (!hasChanges()) {
      return;
    }

    setModifying(true);
    try {
      const modification: OrderModificationRequest = {
        orderId: order.id,
        modificationType,
        reason: `Modified ${modificationType.toLowerCase().replace('_', ' ')} via trading interface`
      };

      switch (modificationType) {
        case 'QUANTITY':
          modification.newQuantity = modificationForm.newQuantity;
          break;
        case 'PRICE':
          modification.newPrice = modificationForm.newPrice;
          break;
        case 'STOP_PRICE':
          modification.newStopPrice = modificationForm.newStopPrice;
          break;
      }

      await onModify(modification);
      setShowConfirmation(false);
    } catch (error) {
      console.error('Order modification failed:', error);
    } finally {
      setModifying(false);
    }
  };

  const handleCancelOrder = async () => {
    setModifying(true);
    try {
      await onCancel(order.id);
    } catch (error) {
      console.error('Order cancellation failed:', error);
    } finally {
      setModifying(false);
    }
  };

  const formatPrice = (price: number | undefined) => {
    if (price === undefined) return '';
    return price.toString();
  };

  const remainingQuantity = order.quantity - order.filledQuantity;

  return (
    <div className="space-y-4">
      {/* Order Summary */}
      <div className="glass-card rounded-xl p-4">
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center space-x-3">
            <h3 className="text-lg font-semibold text-white">
              {order.symbol} • {order.side}
            </h3>
            <div className={`px-2 py-1 rounded text-xs font-medium ${
              order.status === 'ACKNOWLEDGED' ? 'bg-blue-500/20 text-blue-400' :
              order.status === 'PARTIALLY_FILLED' ? 'bg-yellow-500/20 text-yellow-400' :
              'bg-slate-500/20 text-slate-400'
            }`}>
              {order.status}
            </div>
          </div>
          <div className="text-right">
            <div className="text-white font-medium">
              {order.filledQuantity} / {order.quantity}
            </div>
            <div className="text-xs text-slate-400">Filled / Total</div>
          </div>
        </div>
        
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
          <div>
            <div className="text-slate-400">Order Type</div>
            <div className="text-white font-medium">{order.orderType}</div>
          </div>
          <div>
            <div className="text-slate-400">Price</div>
            <div className="text-white font-medium">
              {order.price ? `₹${order.price}` : 'Market'}
            </div>
          </div>
          <div>
            <div className="text-slate-400">Remaining</div>
            <div className="text-white font-medium">{remainingQuantity}</div>
          </div>
          <div>
            <div className="text-slate-400">Avg. Price</div>
            <div className="text-white font-medium">
              {order.averagePrice ? `₹${order.averagePrice}` : '-'}
            </div>
          </div>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex space-x-1 bg-slate-800/50 rounded-lg p-1">
        <button
          onClick={() => setActiveTab('modify')}
          className={`flex-1 px-4 py-2 rounded-md text-sm font-medium transition-colors ${
            activeTab === 'modify'
              ? 'bg-purple-500/20 text-purple-400'
              : 'text-slate-400 hover:text-white'
          }`}
        >
          <Edit3 className="w-4 h-4 mr-2 inline" />
          Modify Order
        </button>
        <button
          onClick={() => setActiveTab('history')}
          className={`flex-1 px-4 py-2 rounded-md text-sm font-medium transition-colors ${
            activeTab === 'history'
              ? 'bg-purple-500/20 text-purple-400'
              : 'text-slate-400 hover:text-white'
          }`}
        >
          <History className="w-4 h-4 mr-2 inline" />
          History ({modificationHistory.length})
        </button>
      </div>

      {activeTab === 'modify' && (
        <div className="glass-card rounded-xl p-6">
          {!canModify && (
            <div className="bg-yellow-500/10 border border-yellow-500/20 rounded-lg p-3 mb-4">
              <div className="flex items-center space-x-2">
                <AlertTriangle className="w-4 h-4 text-yellow-400" />
                <span className="text-yellow-400 text-sm">
                  Order cannot be modified in its current status: {order.status}
                </span>
              </div>
            </div>
          )}

          {/* Modification Type Selector */}
          <div className="mb-6">
            <label className="block text-sm font-medium text-slate-300 mb-2">
              Modification Type
            </label>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-2">
              <button
                onClick={() => setModificationType('QUANTITY')}
                disabled={!canModify}
                className={`p-3 rounded-lg text-sm font-medium transition-all border ${
                  modificationType === 'QUANTITY'
                    ? 'bg-purple-500/20 text-purple-400 border-purple-500/50'
                    : 'bg-slate-700/50 text-slate-400 hover:text-white border-slate-600/50'
                } disabled:opacity-50`}
              >
                Quantity
              </button>
              {order.orderType !== 'MARKET' && (
                <button
                  onClick={() => setModificationType('PRICE')}
                  disabled={!canModify}
                  className={`p-3 rounded-lg text-sm font-medium transition-all border ${
                    modificationType === 'PRICE'
                      ? 'bg-purple-500/20 text-purple-400 border-purple-500/50'
                      : 'bg-slate-700/50 text-slate-400 hover:text-white border-slate-600/50'
                  } disabled:opacity-50`}
                >
                  Price
                </button>
              )}
              {(order.orderType === 'STOP' || order.orderType === 'STOP_LIMIT') && (
                <button
                  onClick={() => setModificationType('STOP_PRICE')}
                  disabled={!canModify}
                  className={`p-3 rounded-lg text-sm font-medium transition-all border ${
                    modificationType === 'STOP_PRICE'
                      ? 'bg-purple-500/20 text-purple-400 border-purple-500/50'
                      : 'bg-slate-700/50 text-slate-400 hover:text-white border-slate-600/50'
                  } disabled:opacity-50`}
                >
                  Stop Price
                </button>
              )}
            </div>
          </div>

          {/* Modification Input */}
          <div className="space-y-4">
            {modificationType === 'QUANTITY' && (
              <div>
                <label className="block text-sm font-medium text-slate-300 mb-2">
                  New Quantity
                </label>
                <input
                  type="number"
                  min="1"
                  max={remainingQuantity}
                  value={modificationForm.newQuantity || ''}
                  onChange={(e) => setModificationForm(prev => ({
                    ...prev,
                    newQuantity: parseInt(e.target.value) || 0
                  }))}
                  disabled={!canModify}
                  className="w-full px-4 py-3 bg-slate-800/50 border border-slate-600/50 rounded-lg text-white placeholder-slate-400 focus:outline-none focus:border-purple-500/50 focus:ring-1 focus:ring-purple-500/50 disabled:opacity-50"
                  placeholder="Enter new quantity"
                />
                <div className="text-xs text-slate-400 mt-1">
                  Original: {remainingQuantity} (remaining quantity)
                </div>
              </div>
            )}

            {modificationType === 'PRICE' && order.orderType !== 'MARKET' && (
              <div>
                <label className="block text-sm font-medium text-slate-300 mb-2">
                  New Price (₹)
                </label>
                <input
                  type="number"
                  step="0.01"
                  min="0.01"
                  value={formatPrice(modificationForm.newPrice)}
                  onChange={(e) => setModificationForm(prev => ({
                    ...prev,
                    newPrice: parseFloat(e.target.value) || 0
                  }))}
                  disabled={!canModify}
                  className="w-full px-4 py-3 bg-slate-800/50 border border-slate-600/50 rounded-lg text-white placeholder-slate-400 focus:outline-none focus:border-purple-500/50 focus:ring-1 focus:ring-purple-500/50 disabled:opacity-50"
                  placeholder="Enter new price"
                />
                <div className="text-xs text-slate-400 mt-1">
                  Original: ₹{order.price}
                </div>
              </div>
            )}

            {modificationType === 'STOP_PRICE' && (order.orderType === 'STOP' || order.orderType === 'STOP_LIMIT') && (
              <div>
                <label className="block text-sm font-medium text-slate-300 mb-2">
                  New Stop Price (₹)
                </label>
                <input
                  type="number"
                  step="0.01"
                  min="0.01"
                  value={formatPrice(modificationForm.newStopPrice)}
                  onChange={(e) => setModificationForm(prev => ({
                    ...prev,
                    newStopPrice: parseFloat(e.target.value) || 0
                  }))}
                  disabled={!canModify}
                  className="w-full px-4 py-3 bg-slate-800/50 border border-slate-600/50 rounded-lg text-white placeholder-slate-400 focus:outline-none focus:border-purple-500/50 focus:ring-1 focus:ring-purple-500/50 disabled:opacity-50"
                  placeholder="Enter new stop price"
                />
                <div className="text-xs text-slate-400 mt-1">
                  Original: ₹{order.stopPrice}
                </div>
              </div>
            )}
          </div>

          {/* Action Buttons */}
          <div className="flex space-x-3 mt-8">
            <button
              onClick={() => setShowConfirmation(true)}
              disabled={!canModify || !hasChanges() || modifying}
              className="flex-1 px-6 py-3 bg-purple-500 hover:bg-purple-600 disabled:opacity-50 disabled:cursor-not-allowed text-white rounded-lg font-medium transition-colors flex items-center justify-center"
            >
              {modifying ? (
                <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
              ) : (
                <>
                  <Edit3 className="w-4 h-4 mr-2" />
                  Modify Order
                </>
              )}
            </button>
            <button
              onClick={handleCancelOrder}
              disabled={!canModify || modifying}
              className="px-6 py-3 bg-red-500 hover:bg-red-600 disabled:opacity-50 disabled:cursor-not-allowed text-white rounded-lg font-medium transition-colors flex items-center justify-center"
            >
              <X className="w-4 h-4 mr-2" />
              Cancel Order
            </button>
          </div>

          {/* Modification Impact */}
          {hasChanges() && (
            <div className="mt-4 p-4 bg-blue-500/10 border border-blue-500/20 rounded-lg">
              <div className="flex items-start space-x-2">
                <AlertTriangle className="w-5 h-5 text-blue-400 mt-0.5" />
                <div>
                  <div className="text-blue-400 font-medium text-sm">
                    Order Modification Impact
                  </div>
                  <div className="text-blue-300 text-sm mt-1">
                    {modificationType === 'QUANTITY' && (
                      `Quantity will change from ${remainingQuantity} to ${modificationForm.newQuantity}`
                    )}
                    {modificationType === 'PRICE' && (
                      `Price will change from ₹${order.price} to ₹${modificationForm.newPrice}`
                    )}
                    {modificationType === 'STOP_PRICE' && (
                      `Stop price will change from ₹${order.stopPrice} to ₹${modificationForm.newStopPrice}`
                    )}
                  </div>
                </div>
              </div>
            </div>
          )}
        </div>
      )}

      {activeTab === 'history' && (
        <div className="glass-card rounded-xl p-6">
          <h4 className="text-lg font-semibold text-white mb-4 flex items-center">
            <History className="w-5 h-5 mr-2" />
            Modification History
          </h4>
          <ModificationHistoryTable history={modificationHistory} />
        </div>
      )}

      {/* Confirmation Modal */}
      <ConfirmationModal
        isOpen={showConfirmation}
        title="Confirm Order Modification"
        message={`Are you sure you want to modify the ${modificationType.toLowerCase().replace('_', ' ')} of this order? This action cannot be undone.`}
        onConfirm={handleModification}
        onCancel={() => setShowConfirmation(false)}
        loading={modifying}
        confirmText="Modify Order"
      />
    </div>
  );
}

export default OrderModificationInterface;