import { OrderRequest, OrderType, TimeInForce, OrderStatus } from '../types/trading';

// Order validation utilities
export const validateOrder = (order: OrderRequest): { isValid: boolean; errors: string[] } => {
  const errors: string[] = [];

  // Basic validations
  if (!order.symbol || order.symbol.trim().length === 0) {
    errors.push('Symbol is required');
  }

  if (!order.quantity || order.quantity <= 0) {
    errors.push('Quantity must be greater than 0');
  }

  if (!order.side) {
    errors.push('Order side (BUY/SELL) is required');
  }

  if (!order.type) {
    errors.push('Order type is required');
  }

  // Price validations for market orders
  if (order.type === OrderType.LIMIT || order.type === OrderType.STOP_LIMIT) {
    if (!order.price || order.price <= 0) {
      errors.push(`Price is required for ${order.type} orders`);
    }
  }

  // Stop price validations
  if (order.type === OrderType.STOP || order.type === OrderType.STOP_LIMIT) {
    if (!order.stopPrice || order.stopPrice <= 0) {
      errors.push(`Stop price is required for ${order.type} orders`);
    }
  }

  // Time in force validations
  if (order.timeInForce === TimeInForce.GTD) {
    if (!order.expireTime) {
      errors.push('Expire time is required for GTD orders');
    }
  }

  return {
    isValid: errors.length === 0,
    errors
  };
};

// Order formatting utilities
export const formatOrderValue = (value: number, precision: number = 2): string => {
  return value.toFixed(precision);
};

export const formatOrderPrice = (price: number, symbol: string): string => {
  // Different symbols may have different price precision
  const precision = getPricePrecision(symbol);
  return price.toFixed(precision);
};

export const formatOrderQuantity = (quantity: number, symbol: string): string => {
  // Different symbols may have different quantity precision
  const precision = getQuantityPrecision(symbol);
  return quantity.toFixed(precision);
};

export const getPricePrecision = (symbol: string): number => {
  // This would typically come from market data or exchange specifications
  // For now, using default values
  if (symbol.includes('USD') || symbol.includes('EUR')) {
    return 2;
  }
  if (symbol.includes('BTC') || symbol.includes('ETH')) {
    return 6;
  }
  return 2;
};

export const getQuantityPrecision = (symbol: string): number => {
  // This would typically come from market data or exchange specifications
  if (symbol.includes('BTC') || symbol.includes('ETH')) {
    return 8;
  }
  return 0; // Most stocks use whole numbers
};

// Order status utilities
export const getOrderStatusColor = (status: OrderStatus): string => {
  switch (status) {
    case OrderStatus.FILLED:
      return 'green';
    case OrderStatus.PARTIALLY_FILLED:
      return 'orange';
    case OrderStatus.CANCELLED:
    case OrderStatus.REJECTED:
      return 'red';
    case OrderStatus.EXPIRED:
      return 'gray';
    default:
      return 'blue';
  }
};

export const getOrderStatusIcon = (status: OrderStatus): string => {
  switch (status) {
    case OrderStatus.FILLED:
      return '✓';
    case OrderStatus.PARTIALLY_FILLED:
      return '◐';
    case OrderStatus.CANCELLED:
      return '✗';
    case OrderStatus.REJECTED:
      return '⚠';
    case OrderStatus.EXPIRED:
      return '⏰';
    default:
      return '○';
  }
};

// Time utilities
export const formatTimeRemaining = (expireTime: Date): string => {
  const now = new Date();
  const diff = expireTime.getTime() - now.getTime();
  
  if (diff <= 0) {
    return 'Expired';
  }
  
  const hours = Math.floor(diff / (1000 * 60 * 60));
  const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
  const seconds = Math.floor((diff % (1000 * 60)) / 1000);
  
  if (hours > 0) {
    return `${hours}h ${minutes}m`;
  } else if (minutes > 0) {
    return `${minutes}m ${seconds}s`;
  } else {
    return `${seconds}s`;
  }
};

// Risk calculations
export const calculateOrderValue = (quantity: number, price: number): number => {
  return quantity * price;
};

export const calculateCommission = (orderValue: number, commissionRate: number = 0.001): number => {
  return orderValue * commissionRate;
};

export const calculateTotalCost = (quantity: number, price: number, commissionRate?: number): number => {
  const orderValue = calculateOrderValue(quantity, price);
  const commission = commissionRate ? calculateCommission(orderValue, commissionRate) : 0;
  return orderValue + commission;
};

// Portfolio impact calculations
export const calculatePositionAfterOrder = (
  currentPosition: number,
  orderQuantity: number,
  orderSide: 'BUY' | 'SELL'
): number => {
  return orderSide === 'BUY' 
    ? currentPosition + orderQuantity 
    : currentPosition - orderQuantity;
};

// Advanced order type utilities
export const validateBracketOrder = (
  parentPrice: number,
  profitTarget: number,
  stopLoss: number,
  side: 'BUY' | 'SELL'
): { isValid: boolean; errors: string[] } => {
  const errors: string[] = [];
  
  if (side === 'BUY') {
    if (profitTarget <= parentPrice) {
      errors.push('Profit target must be above parent order price for BUY orders');
    }
    if (stopLoss >= parentPrice) {
      errors.push('Stop loss must be below parent order price for BUY orders');
    }
  } else {
    if (profitTarget >= parentPrice) {
      errors.push('Profit target must be below parent order price for SELL orders');
    }
    if (stopLoss <= parentPrice) {
      errors.push('Stop loss must be above parent order price for SELL orders');
    }
  }
  
  return {
    isValid: errors.length === 0,
    errors
  };
};

// CSV parsing utilities for bulk orders
export const parseOrderFromCsvRow = (row: string[], headers: string[]): Partial<OrderRequest> | null => {
  try {
    const order: Partial<OrderRequest> = {};
    
    headers.forEach((header, index) => {
      const value = row[index]?.trim();
      if (!value) return;
      
      switch (header.toLowerCase()) {
        case 'symbol':
          order.symbol = value.toUpperCase();
          break;
        case 'quantity':
          order.quantity = parseFloat(value);
          break;
        case 'price':
          order.price = parseFloat(value);
          break;
        case 'side':
          order.side = value.toUpperCase() as 'BUY' | 'SELL';
          break;
        case 'type':
          order.type = value.toUpperCase() as OrderType;
          break;
        case 'tif':
        case 'timeinforce':
          order.timeInForce = value.toUpperCase() as TimeInForce;
          break;
        default:
          break;
      }
    });
    
    return order;
  } catch (error) {
    console.error('Error parsing CSV row:', error);
    return null;
  }
};

// Export all utilities
export default {
  validateOrder,
  formatOrderValue,
  formatOrderPrice,
  formatOrderQuantity,
  getPricePrecision,
  getQuantityPrecision,
  getOrderStatusColor,
  getOrderStatusIcon,
  formatTimeRemaining,
  calculateOrderValue,
  calculateCommission,
  calculateTotalCost,
  calculatePositionAfterOrder,
  validateBracketOrder,
  parseOrderFromCsvRow
};