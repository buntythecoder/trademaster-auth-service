import React, { useState, useRef, useEffect, useCallback } from 'react';
import { marketDataApi } from '../../lib/api';
import { useDebounce } from '../../hooks/useDebounce';
import LoadingSpinner from '../common/LoadingSpinner';

interface SearchResult {
  symbol: string;
  name: string;
  type: string;
  exchange: string;
  currency?: string;
  country?: string;
}

interface SymbolSearchProps {
  onSymbolSelect: (symbol: string, name?: string) => void;
  placeholder?: string;
  className?: string;
  autoFocus?: boolean;
  showDetails?: boolean;
}

const SymbolSearch: React.FC<SymbolSearchProps> = ({
  onSymbolSelect,
  placeholder = "Search stocks, ETFs, crypto...",
  className = "",
  autoFocus = false,
  showDetails = true,
}) => {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<SearchResult[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [showResults, setShowResults] = useState(false);
  const [selectedIndex, setSelectedIndex] = useState(-1);
  const [error, setError] = useState<string | null>(null);

  const inputRef = useRef<HTMLInputElement>(null);
  const resultsRef = useRef<HTMLDivElement>(null);
  const debouncedQuery = useDebounce(query, 300);

  // Search for symbols
  const searchSymbols = useCallback(async (searchQuery: string) => {
    if (!searchQuery.trim() || searchQuery.length < 2) {
      setResults([]);
      setShowResults(false);
      return;
    }

    setIsLoading(true);
    setError(null);

    try {
      const searchResults = await marketDataApi.searchSymbols(searchQuery);
      
      // Transform API results to our interface
      const formattedResults: SearchResult[] = searchResults.map(result => ({
        symbol: result.symbol || result.ticker || '',
        name: result.name || result.description || '',
        type: result.type || 'Stock',
        exchange: result.exchange || result.market || '',
        currency: result.currency,
        country: result.country,
      }));

      setResults(formattedResults);
      setShowResults(true);
      setSelectedIndex(-1);
    } catch (err) {
      console.error('Symbol search error:', err);
      setError('Unable to search symbols. Please try again.');
      setResults([]);
      setShowResults(false);
    } finally {
      setIsLoading(false);
    }
  }, []);

  // Effect to search when debounced query changes
  useEffect(() => {
    searchSymbols(debouncedQuery);
  }, [debouncedQuery, searchSymbols]);

  // Handle input change
  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    setQuery(value);
    
    if (!value.trim()) {
      setShowResults(false);
      setSelectedIndex(-1);
    }
  };

  // Handle result selection
  const handleResultSelect = (result: SearchResult) => {
    setQuery(result.symbol);
    setShowResults(false);
    setSelectedIndex(-1);
    onSymbolSelect(result.symbol, result.name);
  };

  // Handle keyboard navigation
  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (!showResults || results.length === 0) return;

    switch (e.key) {
      case 'ArrowDown':
        e.preventDefault();
        setSelectedIndex(prev => 
          prev < results.length - 1 ? prev + 1 : prev
        );
        break;
      
      case 'ArrowUp':
        e.preventDefault();
        setSelectedIndex(prev => prev > 0 ? prev - 1 : -1);
        break;
      
      case 'Enter':
        e.preventDefault();
        if (selectedIndex >= 0 && selectedIndex < results.length) {
          handleResultSelect(results[selectedIndex]);
        } else if (results.length > 0) {
          handleResultSelect(results[0]);
        }
        break;
      
      case 'Escape':
        setShowResults(false);
        setSelectedIndex(-1);
        inputRef.current?.blur();
        break;
    }
  };

  // Handle input focus
  const handleInputFocus = () => {
    if (query.trim() && results.length > 0) {
      setShowResults(true);
    }
  };

  // Handle click outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (
        resultsRef.current &&
        !resultsRef.current.contains(event.target as Node) &&
        !inputRef.current?.contains(event.target as Node)
      ) {
        setShowResults(false);
        setSelectedIndex(-1);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const getTypeIcon = (type: string) => {
    switch (type.toLowerCase()) {
      case 'stock':
      case 'equity':
        return '📈';
      case 'etf':
        return '🗂️';
      case 'crypto':
      case 'cryptocurrency':
        return '₿';
      case 'forex':
        return '💱';
      case 'commodity':
        return '🥇';
      default:
        return '📊';
    }
  };

  return (
    <div className={`relative ${className}`}>
      {/* Search Input */}
      <div className="relative">
        <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
          <svg className="h-5 w-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="m21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
          </svg>
        </div>
        
        <input
          ref={inputRef}
          type="text"
          value={query}
          onChange={handleInputChange}
          onKeyDown={handleKeyDown}
          onFocus={handleInputFocus}
          placeholder={placeholder}
          autoFocus={autoFocus}
          className="block w-full pl-10 pr-10 py-3 border border-gray-300 rounded-lg text-sm placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent bg-white shadow-sm"
        />

        {/* Loading Spinner */}
        {isLoading && (
          <div className="absolute inset-y-0 right-0 pr-3 flex items-center">
            <LoadingSpinner size="sm" />
          </div>
        )}

        {/* Clear Button */}
        {query && !isLoading && (
          <button
            onClick={() => {
              setQuery('');
              setShowResults(false);
              setSelectedIndex(-1);
              inputRef.current?.focus();
            }}
            className="absolute inset-y-0 right-0 pr-3 flex items-center text-gray-400 hover:text-gray-600"
          >
            <svg className="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        )}
      </div>

      {/* Search Results */}
      {showResults && (
        <div
          ref={resultsRef}
          className="absolute z-50 w-full mt-1 bg-white border border-gray-200 rounded-lg shadow-lg max-h-64 overflow-y-auto"
        >
          {error ? (
            <div className="p-4 text-center text-red-600">
              <p className="text-sm">{error}</p>
            </div>
          ) : results.length === 0 && !isLoading ? (
            <div className="p-4 text-center text-gray-500">
              <p className="text-sm">No symbols found</p>
              <p className="text-xs mt-1">Try a different search term</p>
            </div>
          ) : (
            <div className="py-1">
              {results.map((result, index) => (
                <button
                  key={`${result.symbol}-${result.exchange}`}
                  onClick={() => handleResultSelect(result)}
                  className={`w-full px-4 py-3 text-left hover:bg-gray-50 focus:bg-gray-50 focus:outline-none transition-colors ${
                    index === selectedIndex ? 'bg-primary/5' : ''
                  }`}
                >
                  <div className="flex items-center justify-between">
                    <div className="flex items-center space-x-3">
                      <span className="text-lg">{getTypeIcon(result.type)}</span>
                      <div>
                        <div className="font-medium text-gray-900">
                          {result.symbol}
                        </div>
                        <div className="text-sm text-gray-600 truncate max-w-xs">
                          {result.name}
                        </div>
                      </div>
                    </div>
                    
                    {showDetails && (
                      <div className="text-right">
                        <div className="text-xs text-gray-500">
                          {result.type}
                        </div>
                        <div className="text-xs text-gray-400">
                          {result.exchange}
                        </div>
                      </div>
                    )}
                  </div>
                </button>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default SymbolSearch;