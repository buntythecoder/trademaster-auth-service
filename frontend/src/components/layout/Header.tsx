import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '../../store';
import { logout, selectCurrentUser } from '../../store/slices/authSlice';
import {
  selectNotifications,
  selectUnreadNotificationCount,
  toggleSidebar,
  selectSidebar,
  addNotification,
} from '../../store/slices/uiSlice';

export const Header: React.FC = () => {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const currentUser = useAppSelector(selectCurrentUser);
  const notifications = useAppSelector(selectNotifications);
  const unreadCount = useAppSelector(selectUnreadNotificationCount);
  const sidebar = useAppSelector(selectSidebar);

  const handleLogout = async () => {
    try {
      await dispatch(logout()).unwrap();
      dispatch(addNotification({
        type: 'success',
        title: 'Logged Out',
        message: 'You have been successfully logged out.',
      }));
      navigate('/auth');
    } catch (error) {
      console.error('Logout failed:', error);
    }
  };

  const handleToggleSidebar = () => {
    dispatch(toggleSidebar());
  };

  return (
    <header className="bg-white border-b border-gray-200 shadow-sm h-16 flex items-center justify-between px-6 sticky top-0 z-40">
      {/* Left Section - Logo and Sidebar Toggle */}
      <div className="flex items-center space-x-4">
        {/* Sidebar Toggle */}
        <button
          onClick={handleToggleSidebar}
          className="p-2 rounded-lg hover:bg-gray-100 focus:outline-none focus:ring-2 focus:ring-primary-500 lg:hidden"
        >
          <svg className="h-5 w-5 text-gray-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
          </svg>
        </button>

        {/* Logo */}
        <div className="flex items-center space-x-3">
          <div className="bg-primary-600 rounded-lg p-2">
            <svg className="h-6 w-6 text-white" fill="currentColor" viewBox="0 0 24 24">
              <path d="M13 2.05v2.02c4.39.54 7.5 4.53 6.96 8.92-.47 3.96-3.96 7.14-7.92 7.54V22c6.05-.55 10.5-5.75 9.95-11.8-.47-5.37-4.63-9.53-9.99-10.15zM12.5 7v5.5l4.5 2.5-.8 1.4-5.2-2.9V7h1.5zm-6.5 5c0 3.31 2.69 6 6 6s6-2.69 6-6-2.69-6-6-6-6 2.69-6 6z"/>
            </svg>
          </div>
          <div className="hidden sm:block">
            <h1 className="text-xl font-bold text-gray-900">TradeMaster</h1>
            <p className="text-xs text-gray-500">Professional Trading</p>
          </div>
        </div>
      </div>

      {/* Center Section - Quick Stats or Search */}
      <div className="hidden md:flex items-center space-x-6">
        <div className="flex items-center space-x-4 text-sm">
          <div className="flex items-center space-x-1">
            <span className="text-gray-600">Market:</span>
            <span className="font-semibold text-success-600">OPEN</span>
            <div className="w-2 h-2 bg-success-500 rounded-full animate-pulse"></div>
          </div>
          <div className="flex items-center space-x-1">
            <span className="text-gray-600">S&P 500:</span>
            <span className="font-mono text-success-600">+0.85%</span>
          </div>
          <div className="flex items-center space-x-1">
            <span className="text-gray-600">NASDAQ:</span>
            <span className="font-mono text-success-600">+1.24%</span>
          </div>
        </div>
      </div>

      {/* Right Section - User Actions */}
      <div className="flex items-center space-x-4">
        {/* Notifications */}
        <div className="relative">
          <button className="p-2 rounded-lg hover:bg-gray-100 focus:outline-none focus:ring-2 focus:ring-primary-500 relative">
            <svg className="h-5 w-5 text-gray-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 17h5l-5 5v-5zM8.828 15H5.5a2.5 2.5 0 110-5h7c.375 0 .745.03 1.106.088L10.5 6.5H4a2 2 0 00-2 2v7a2 2 0 002 2h4.828zM17 8.5l-7-7v4L17 8.5z" />
            </svg>
            {unreadCount > 0 && (
              <span className="absolute -top-1 -right-1 bg-danger-500 text-white text-xs rounded-full h-5 w-5 flex items-center justify-center font-medium">
                {unreadCount > 9 ? '9+' : unreadCount}
              </span>
            )}
          </button>
        </div>

        {/* Quick Actions */}
        <button className="hidden sm:flex items-center space-x-2 px-3 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2 transition-colors">
          <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
          </svg>
          <span className="text-sm font-medium">New Order</span>
        </button>

        {/* User Menu */}
        <div className="relative">
          <button className="flex items-center space-x-3 p-1 rounded-lg hover:bg-gray-100 focus:outline-none focus:ring-2 focus:ring-primary-500">
            <div className="flex-shrink-0">
              <div className="h-8 w-8 bg-primary-600 rounded-full flex items-center justify-center">
                <span className="text-sm font-medium text-white">
                  {currentUser?.firstName?.[0]}{currentUser?.lastName?.[0]}
                </span>
              </div>
            </div>
            <div className="hidden sm:block text-left">
              <p className="text-sm font-medium text-gray-900">
                {currentUser?.firstName} {currentUser?.lastName}
              </p>
              <p className="text-xs text-gray-500 capitalize">
                {currentUser?.role?.toLowerCase().replace('_', ' ')}
              </p>
            </div>
            <svg className="h-4 w-4 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
            </svg>
          </button>

          {/* User Dropdown Menu */}
          <div className="hidden absolute right-0 mt-2 w-56 bg-white rounded-lg shadow-lg border border-gray-200 py-2 z-50">
            <div className="px-4 py-3 border-b border-gray-200">
              <p className="text-sm font-medium text-gray-900">
                {currentUser?.firstName} {currentUser?.lastName}
              </p>
              <p className="text-sm text-gray-500">{currentUser?.email}</p>
            </div>
            
            <a href="#" className="block px-4 py-2 text-sm text-gray-700 hover:bg-gray-100">
              <div className="flex items-center space-x-2">
                <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                </svg>
                <span>Profile Settings</span>
              </div>
            </a>
            
            <a href="#" className="block px-4 py-2 text-sm text-gray-700 hover:bg-gray-100">
              <div className="flex items-center space-x-2">
                <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                </svg>
                <span>Account Settings</span>
              </div>
            </a>
            
            <a href="#" className="block px-4 py-2 text-sm text-gray-700 hover:bg-gray-100">
              <div className="flex items-center space-x-2">
                <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8.228 9c.549-1.165 2.03-2 3.772-2 2.21 0 4 1.343 4 3 0 1.4-1.278 2.575-3.006 2.907-.542.104-.994.54-.994 1.093m0 3h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                <span>Help & Support</span>
              </div>
            </a>
            
            <div className="border-t border-gray-200 my-2"></div>
            
            <button
              onClick={handleLogout}
              className="block w-full text-left px-4 py-2 text-sm text-danger-600 hover:bg-danger-50"
            >
              <div className="flex items-center space-x-2">
                <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
                </svg>
                <span>Sign Out</span>
              </div>
            </button>
          </div>
        </div>
      </div>
    </header>
  );
};

export default Header;