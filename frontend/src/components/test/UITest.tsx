import React from 'react';

const UITest: React.FC = () => {
  return (
    <div className="min-h-screen bg-neutral-50 p-8">
      <div className="max-w-4xl mx-auto space-y-8">
        {/* Header */}
        <div className="text-center">
          <h1 className="text-3xl font-bold text-gray-900 mb-4">
            TradeMaster UI Test
          </h1>
          <p className="text-gray-600">
            Testing TradeMaster components and styling
          </p>
        </div>

        {/* Color Palette Test */}
        <div className="bg-white rounded-lg shadow-lg p-6">
          <h2 className="text-xl font-semibold text-gray-900 mb-4">
            Color Palette Test
          </h2>
          <div className="grid grid-cols-4 gap-4">
            <div className="space-y-2">
              <div className="h-16 w-full bg-primary-500 rounded"></div>
              <p className="text-sm text-gray-600">Primary</p>
            </div>
            <div className="space-y-2">
              <div className="h-16 w-full bg-success-500 rounded"></div>
              <p className="text-sm text-gray-600">Success</p>
            </div>
            <div className="space-y-2">
              <div className="h-16 w-full bg-danger-500 rounded"></div>
              <p className="text-sm text-gray-600">Danger</p>
            </div>
            <div className="space-y-2">
              <div className="h-16 w-full bg-warning-500 rounded"></div>
              <p className="text-sm text-gray-600">Warning</p>
            </div>
          </div>
        </div>

        {/* Market Colors Test */}
        <div className="bg-white rounded-lg shadow-lg p-6">
          <h2 className="text-xl font-semibold text-gray-900 mb-4">
            Market Colors Test
          </h2>
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <div className="h-16 w-full bg-bull rounded"></div>
              <p className="text-sm text-gray-600">Bull Market</p>
            </div>
            <div className="space-y-2">
              <div className="h-16 w-full bg-bear rounded"></div>
              <p className="text-sm text-gray-600">Bear Market</p>
            </div>
          </div>
        </div>

        {/* Typography Test */}
        <div className="bg-white rounded-lg shadow-lg p-6">
          <h2 className="text-xl font-semibold text-gray-900 mb-4">
            Typography Test
          </h2>
          <div className="space-y-4">
            <div>
              <h3 className="text-lg font-medium text-gray-900 mb-2">Inter Font (UI)</h3>
              <p className="font-sans">This is the Inter font used for UI elements.</p>
            </div>
            <div>
              <h3 className="text-lg font-medium text-gray-900 mb-2">JetBrains Mono (Financial Data)</h3>
              <p className="font-mono">$1,234.56 +2.45% (12,345 shares)</p>
            </div>
          </div>
        </div>

        {/* Button Test */}
        <div className="bg-white rounded-lg shadow-lg p-6">
          <h2 className="text-xl font-semibold text-gray-900 mb-4">
            Button Components
          </h2>
          <div className="flex space-x-4">
            <button className="btn-primary">
              Primary Button
            </button>
            <button className="btn-success">
              Success Button
            </button>
            <button className="btn-danger">
              Danger Button
            </button>
          </div>
        </div>

        {/* Card Test */}
        <div className="bg-white rounded-lg shadow-lg p-6">
          <h2 className="text-xl font-semibold text-gray-900 mb-4">
            Market Card Test
          </h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="card p-4">
              <div className="flex items-center justify-between mb-4">
                <div>
                  <h3 className="text-lg font-semibold text-gray-900">AAPL</h3>
                  <p className="text-sm text-gray-600">Apple Inc.</p>
                </div>
                <div className="px-3 py-1 rounded-full text-bull bg-bull/10">
                  <span className="text-sm font-medium">+2.45%</span>
                </div>
              </div>
              <div className="space-y-2">
                <div className="flex justify-between items-center">
                  <span className="text-sm text-gray-600">Price</span>
                  <span className="text-xl font-bold text-gray-900">$182.52</span>
                </div>
                <div className="flex justify-between items-center">
                  <span className="text-sm text-gray-600">Change</span>
                  <span className="font-medium text-bull">+$4.37</span>
                </div>
              </div>
            </div>
            
            <div className="card p-4">
              <div className="flex items-center justify-between mb-4">
                <div>
                  <h3 className="text-lg font-semibold text-gray-900">TSLA</h3>
                  <p className="text-sm text-gray-600">Tesla, Inc.</p>
                </div>
                <div className="px-3 py-1 rounded-full text-bear bg-bear/10">
                  <span className="text-sm font-medium">-1.23%</span>
                </div>
              </div>
              <div className="space-y-2">
                <div className="flex justify-between items-center">
                  <span className="text-sm text-gray-600">Price</span>
                  <span className="text-xl font-bold text-gray-900">$248.73</span>
                </div>
                <div className="flex justify-between items-center">
                  <span className="text-sm text-gray-600">Change</span>
                  <span className="font-medium text-bear">-$3.09</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Animation Test */}
        <div className="bg-white rounded-lg shadow-lg p-6">
          <h2 className="text-xl font-semibold text-gray-900 mb-4">
            Animation Test
          </h2>
          <div className="space-y-4">
            <div className="animate-fade-in bg-primary-100 p-4 rounded">
              Fade in animation
            </div>
            <div className="animate-pulse-slow bg-success-100 p-4 rounded">
              Slow pulse animation
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default UITest;