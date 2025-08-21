// Utility functions for formatting data display

interface CurrencyOptions {
  compact?: boolean;
  showSign?: boolean;
  minimumFractionDigits?: number;
  maximumFractionDigits?: number;
}

export const formatCurrency = (
  value: number,
  options: CurrencyOptions = {}
): string => {
  const {
    compact = false,
    showSign = false,
    minimumFractionDigits = 2,
    maximumFractionDigits = 2,
  } = options;

  // Handle edge cases
  if (isNaN(value) || !isFinite(value)) {
    return '$--';
  }

  const absValue = Math.abs(value);
  const sign = value < 0 ? '-' : showSign && value > 0 ? '+' : '';

  if (compact && absValue >= 1e9) {
    return `${sign}$${(absValue / 1e9).toFixed(1)}B`;
  } else if (compact && absValue >= 1e6) {
    return `${sign}$${(absValue / 1e6).toFixed(1)}M`;
  } else if (compact && absValue >= 1e3) {
    return `${sign}$${(absValue / 1e3).toFixed(1)}K`;
  }

  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    minimumFractionDigits,
    maximumFractionDigits,
    signDisplay: showSign ? 'exceptZero' : 'auto',
  }).format(value);
};

export const formatPercentage = (
  value: number,
  decimalPlaces: number = 2
): string => {
  if (isNaN(value) || !isFinite(value)) {
    return '--';
  }

  return new Intl.NumberFormat('en-US', {
    style: 'percent',
    minimumFractionDigits: decimalPlaces,
    maximumFractionDigits: decimalPlaces,
    signDisplay: 'exceptZero',
  }).format(value / 100);
};

export const formatNumber = (
  value: number,
  options: {
    compact?: boolean;
    decimals?: number;
    showSign?: boolean;
  } = {}
): string => {
  const { compact = false, decimals = 2, showSign = false } = options;

  if (isNaN(value) || !isFinite(value)) {
    return '--';
  }

  const absValue = Math.abs(value);
  const sign = value < 0 ? '-' : showSign && value > 0 ? '+' : '';

  if (compact) {
    if (absValue >= 1e12) {
      return `${sign}${(absValue / 1e12).toFixed(1)}T`;
    } else if (absValue >= 1e9) {
      return `${sign}${(absValue / 1e9).toFixed(1)}B`;
    } else if (absValue >= 1e6) {
      return `${sign}${(absValue / 1e6).toFixed(1)}M`;
    } else if (absValue >= 1e3) {
      return `${sign}${(absValue / 1e3).toFixed(1)}K`;
    }
  }

  return new Intl.NumberFormat('en-US', {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals,
    signDisplay: showSign ? 'exceptZero' : 'auto',
  }).format(value);
};

export const formatVolume = (volume: number): string => {
  return formatNumber(volume, { compact: true, decimals: 1 });
};

export const formatMarketCap = (marketCap: number): string => {
  return formatCurrency(marketCap, { compact: true });
};

interface TimeFormatOptions {
  includeSeconds?: boolean;
  use24Hour?: boolean;
  includeDate?: boolean;
}

export const formatTime = (
  timestamp: string | Date | number,
  format: 'short' | 'medium' | 'long' | 'full' | 'time-only' = 'medium'
): string => {
  const date = new Date(timestamp);

  if (isNaN(date.getTime())) {
    return '--';
  }

  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffMinutes = Math.floor(diffMs / (1000 * 60));
  const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
  const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));

  // Relative time for recent timestamps
  if (format === 'short' && diffMs < 1000 * 60 * 60 * 24) {
    if (diffMinutes < 1) return 'now';
    if (diffMinutes < 60) return `${diffMinutes}m`;
    if (diffHours < 24) return `${diffHours}h`;
  }

  // Format options for different display types
  const formatOptions: Record<string, Intl.DateTimeFormatOptions> = {
    'time-only': {
      hour: 'numeric',
      minute: '2-digit',
      hour12: true,
    },
    short: {
      month: 'short',
      day: 'numeric',
      hour: 'numeric',
      minute: '2-digit',
    },
    medium: {
      month: 'short',
      day: 'numeric',
      year: diffDays > 365 ? 'numeric' : undefined,
      hour: 'numeric',
      minute: '2-digit',
      hour12: true,
    },
    long: {
      month: 'long',
      day: 'numeric',
      year: 'numeric',
      hour: 'numeric',
      minute: '2-digit',
      hour12: true,
    },
    full: {
      weekday: 'long',
      month: 'long',
      day: 'numeric',
      year: 'numeric',
      hour: 'numeric',
      minute: '2-digit',
      second: '2-digit',
      hour12: true,
    },
  };

  return new Intl.DateTimeFormat('en-US', formatOptions[format]).format(date);
};

export const formatTimeAgo = (timestamp: string | Date | number): string => {
  const date = new Date(timestamp);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();

  const units = [
    { name: 'year', ms: 1000 * 60 * 60 * 24 * 365 },
    { name: 'month', ms: 1000 * 60 * 60 * 24 * 30 },
    { name: 'week', ms: 1000 * 60 * 60 * 24 * 7 },
    { name: 'day', ms: 1000 * 60 * 60 * 24 },
    { name: 'hour', ms: 1000 * 60 * 60 },
    { name: 'minute', ms: 1000 * 60 },
    { name: 'second', ms: 1000 },
  ];

  for (const unit of units) {
    const value = Math.floor(diffMs / unit.ms);
    if (value >= 1) {
      return `${value} ${unit.name}${value > 1 ? 's' : ''} ago`;
    }
  }

  return 'just now';
};

export const formatDate = (
  date: string | Date | number,
  format: 'short' | 'medium' | 'long' = 'medium'
): string => {
  const dateObj = new Date(date);

  if (isNaN(dateObj.getTime())) {
    return '--';
  }

  const formatOptions: Record<string, Intl.DateTimeFormatOptions> = {
    short: {
      month: 'numeric',
      day: 'numeric',
      year: '2-digit',
    },
    medium: {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
    },
    long: {
      month: 'long',
      day: 'numeric',
      year: 'numeric',
    },
  };

  return new Intl.DateTimeFormat('en-US', formatOptions[format]).format(dateObj);
};

// Format price change with appropriate styling classes
export const formatPriceChange = (
  change: number,
  changePercent: number
): {
  changeText: string;
  percentText: string;
  colorClass: string;
  isPositive: boolean;
} => {
  const isPositive = change >= 0;
  const colorClass = isPositive ? 'text-bull' : 'text-bear';
  const sign = isPositive ? '+' : '';

  return {
    changeText: `${sign}${formatCurrency(change)}`,
    percentText: `${sign}${formatPercentage(changePercent)}`,
    colorClass,
    isPositive,
  };
};

// Format large numbers with appropriate units
export const formatLargeNumber = (value: number): string => {
  return formatNumber(value, { compact: true, decimals: 1 });
};

// Format trading-specific values
export const formatShares = (shares: number): string => {
  if (shares % 1 === 0) {
    return formatNumber(shares, { compact: true, decimals: 0 });
  }
  return formatNumber(shares, { compact: true, decimals: 3 });
};

export const formatOrderSize = (size: number, price: number): string => {
  const value = size * price;
  return `${formatShares(size)} (${formatCurrency(value)})`;
};

// Format ratios and metrics
export const formatRatio = (ratio: number, decimals: number = 2): string => {
  if (isNaN(ratio) || !isFinite(ratio)) {
    return '--';
  }
  return ratio.toFixed(decimals);
};

export const formatBasisPoints = (bps: number): string => {
  if (isNaN(bps) || !isFinite(bps)) {
    return '--';
  }
  return `${bps.toFixed(0)} bps`;
};

// Utility to truncate text with ellipsis
export const truncateText = (text: string, maxLength: number): string => {
  if (text.length <= maxLength) return text;
  return `${text.slice(0, maxLength - 3)}...`;
};

// Format file sizes
export const formatFileSize = (bytes: number): string => {
  const units = ['B', 'KB', 'MB', 'GB', 'TB'];
  let size = bytes;
  let unitIndex = 0;

  while (size >= 1024 && unitIndex < units.length - 1) {
    size /= 1024;
    unitIndex++;
  }

  return `${size.toFixed(1)} ${units[unitIndex]}`;
};