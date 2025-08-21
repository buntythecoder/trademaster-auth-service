import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { Provider } from 'react-redux';
import { store } from './store';
import ProtectedRoute from './components/auth/ProtectedRoute';
import AuthPage from './components/auth/AuthPage';
import Dashboard from './components/dashboard/Dashboard';
import ErrorBoundary from './components/error/ErrorBoundary';
import ToastContainer from './components/common/Toast';
import { NotFoundPage } from './components/error/ErrorPages';
import useAppInitialization from './hooks/useAppInitialization';
import useNetworkStatus from './hooks/useNetworkStatus';
import './index.css';

// Import DashboardLayout
import DashboardLayout from './components/layout/DashboardLayout';
import { MarketSummary, MarketOverview } from './components/market';
import UITest from './components/test/UITest';
import { HeroSection } from './components/landing/HeroSection';
import TradingInterface from './components/trading/TradingInterface';
import TradersPage from './pages/TradersPage';

const Portfolio = () => (
  <DashboardLayout>
    <div className="p-6 max-w-7xl mx-auto">
      <div className="bg-white rounded-lg shadow-lg p-8 text-center">
        <h1 className="text-2xl font-bold text-gray-900 mb-4">Portfolio Management</h1>
        <p className="text-gray-600">Portfolio interface coming soon...</p>
      </div>
    </div>
  </DashboardLayout>
);

const Trading = () => (
  <DashboardLayout>
    <TradingInterface />
  </DashboardLayout>
);

const Markets = () => (
  <DashboardLayout>
    <div style={{
      minHeight: '100vh',
      background: 'radial-gradient(ellipse at top, #0f0f23 0%, #1a0b2e 25%, #0a0a0a 100%)',
      padding: '40px 20px',
      position: 'relative'
    }}>
      {/* Background Elements */}
      <div style={{
        position: 'absolute',
        top: '10%',
        left: '5%',
        width: '200px',
        height: '200px',
        background: 'radial-gradient(circle, rgba(139, 92, 246, 0.1) 0%, transparent 70%)',
        borderRadius: '50%',
        filter: 'blur(60px)'
      }} />
      <div style={{
        position: 'absolute',
        top: '60%',
        right: '10%',
        width: '150px',
        height: '150px',
        background: 'radial-gradient(circle, rgba(6, 182, 212, 0.08) 0%, transparent 70%)',
        borderRadius: '50%',
        filter: 'blur(50px)'
      }} />

      <div style={{
        maxWidth: '1400px',
        margin: '0 auto',
        position: 'relative',
        zIndex: 2,
        display: 'flex',
        flexDirection: 'column',
        gap: '48px'
      }}>
        <MarketSummary />
        <MarketOverview />
      </div>
    </div>
  </DashboardLayout>
);


// AppContent component to use hooks inside Provider
const AppContent: React.FC = () => {
  // Initialize app-wide functionality
  useAppInitialization();
  useNetworkStatus();

  return (
    <BrowserRouter>
      <div className="App">
            <Routes>
            {/* Public routes */}
            <Route
              path="/auth"
              element={
                <ProtectedRoute requireAuth={false}>
                  <AuthPage />
                </ProtectedRoute>
              }
            />

            {/* Protected routes */}
            <Route
              path="/dashboard"
              element={
                <ProtectedRoute>
                  <Dashboard />
                </ProtectedRoute>
              }
            />

            <Route
              path="/portfolio"
              element={
                <ProtectedRoute>
                  <Portfolio />
                </ProtectedRoute>
              }
            />

            <Route
              path="/trading"
              element={
                <ProtectedRoute>
                  <Trading />
                </ProtectedRoute>
              }
            />

            <Route
              path="/markets"
              element={
                <ProtectedRoute>
                  <Markets />
                </ProtectedRoute>
              }
            />

            <Route
              path="/analytics"
              element={
                <ProtectedRoute>
                  <DashboardLayout>
                    <div className="p-6 max-w-7xl mx-auto">
                      <div className="bg-white rounded-lg shadow-lg p-8 text-center">
                        <h1 className="text-2xl font-bold text-gray-900 mb-4">Analytics</h1>
                        <p className="text-gray-600">Analytics interface coming soon...</p>
                      </div>
                    </div>
                  </DashboardLayout>
                </ProtectedRoute>
              }
            />

            <Route
              path="/watchlist"
              element={
                <ProtectedRoute>
                  <DashboardLayout>
                    <div className="p-6 max-w-7xl mx-auto">
                      <div className="bg-white rounded-lg shadow-lg p-8 text-center">
                        <h1 className="text-2xl font-bold text-gray-900 mb-4">Watchlist</h1>
                        <p className="text-gray-600">Watchlist interface coming soon...</p>
                      </div>
                    </div>
                  </DashboardLayout>
                </ProtectedRoute>
              }
            />

            <Route
              path="/reports"
              element={
                <ProtectedRoute>
                  <DashboardLayout>
                    <div className="p-6 max-w-7xl mx-auto">
                      <div className="bg-white rounded-lg shadow-lg p-8 text-center">
                        <h1 className="text-2xl font-bold text-gray-900 mb-4">Reports</h1>
                        <p className="text-gray-600">Reports interface coming soon...</p>
                      </div>
                    </div>
                  </DashboardLayout>
                </ProtectedRoute>
              }
            />

            <Route
              path="/traders"
              element={
                <ProtectedRoute>
                  <TradersPage />
                </ProtectedRoute>
              }
            />

            {/* Landing page */}
            <Route path="/" element={<HeroSection />} />

            {/* Test route */}
            <Route path="/test" element={<UITest />} />

            {/* Redirects */}
            <Route path="/login" element={<Navigate to="/auth" replace />} />
            <Route path="/register" element={<Navigate to="/auth" replace />} />

            {/* 404 fallback */}
            <Route path="*" element={<NotFoundPage />} />
          </Routes>
          
          {/* Global Toast Container */}
          <ToastContainer />
        </div>
      </BrowserRouter>
  );
};

function App() {
  return (
    <Provider store={store}>
      <ErrorBoundary>
        <AppContent />
      </ErrorBoundary>
    </Provider>
  );
}

export default App;
