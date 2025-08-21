import React, { useEffect } from 'react';
import { useAppSelector, useAppDispatch } from '../../store';
import { selectSidebar, selectIsMobile, setBreakpoint } from '../../store/slices/uiSlice';
import Header from './Header';
import Sidebar from './Sidebar';

interface DashboardLayoutProps {
  children: React.ReactNode;
}

export const DashboardLayout: React.FC<DashboardLayoutProps> = ({ children }) => {
  const dispatch = useAppDispatch();
  const sidebar = useAppSelector(selectSidebar);
  const isMobile = useAppSelector(selectIsMobile);

  // Handle responsive breakpoints
  useEffect(() => {
    const handleResize = () => {
      const width = window.innerWidth;
      let breakpoint: 'xs' | 'sm' | 'md' | 'lg' | 'xl' | '2xl' = 'lg';
      
      if (width < 640) breakpoint = 'xs';
      else if (width < 768) breakpoint = 'sm';
      else if (width < 1024) breakpoint = 'md';
      else if (width < 1280) breakpoint = 'lg';
      else if (width < 1536) breakpoint = 'xl';
      else breakpoint = '2xl';
      
      dispatch(setBreakpoint(breakpoint));
    };

    // Set initial breakpoint
    handleResize();
    
    // Add event listener
    window.addEventListener('resize', handleResize);
    
    // Cleanup
    return () => window.removeEventListener('resize', handleResize);
  }, [dispatch]);

  return (
    <div className="h-screen bg-gray-50 flex overflow-hidden">
      {/* Mobile Sidebar Overlay */}
      {isMobile && !sidebar.isCollapsed && (
        <div 
          className="fixed inset-0 bg-black bg-opacity-50 z-40 lg:hidden"
          onClick={() => dispatch(setBreakpoint(isMobile ? 'xs' : 'lg'))}
        />
      )}

      {/* Sidebar */}
      <div
        className={`fixed inset-y-0 left-0 z-50 lg:static lg:z-auto lg:flex-shrink-0 transition-all duration-300 ease-in-out ${
          isMobile && sidebar.isCollapsed ? '-translate-x-full' : 'translate-x-0'
        }`}
      >
        <Sidebar />
      </div>

      {/* Main Content Area */}
      <div className="flex-1 flex flex-col min-w-0 h-screen">
        {/* Header */}
        <Header />

        {/* Page Content */}
        <main className="flex-1 overflow-hidden">
          <div className="h-full">
            {children}
          </div>
        </main>
      </div>
    </div>
  );
};

export default DashboardLayout;