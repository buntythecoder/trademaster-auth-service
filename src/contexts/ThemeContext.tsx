import React, { createContext, useContext, useEffect, useState } from 'react';

export type Theme = 'light' | 'dark' | 'auto';

interface ThemeContextType {
  theme: Theme;
  actualTheme: 'light' | 'dark';
  setTheme: (theme: Theme) => void;
  toggleTheme: () => void;
}

const ThemeContext = createContext<ThemeContextType | undefined>(undefined);

export function useTheme() {
  const context = useContext(ThemeContext);
  if (!context) {
    throw new Error('useTheme must be used within a ThemeProvider');
  }
  return context;
}

interface ThemeProviderProps {
  children: React.ReactNode;
}

export function ThemeProvider({ children }: ThemeProviderProps) {
  const [theme, setTheme] = useState<Theme>(() => {
    // Get theme from localStorage or default to 'auto'
    const savedTheme = localStorage.getItem('trademaster-theme') as Theme;
    return savedTheme || 'auto';
  });

  const [actualTheme, setActualTheme] = useState<'light' | 'dark'>('dark');

  // Function to get system preference
  const getSystemTheme = (): 'light' | 'dark' => {
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
  };

  // Update actual theme based on theme preference
  useEffect(() => {
    const updateActualTheme = () => {
      let newActualTheme: 'light' | 'dark';
      
      switch (theme) {
        case 'light':
          newActualTheme = 'light';
          break;
        case 'dark':
          newActualTheme = 'dark';
          break;
        case 'auto':
        default:
          newActualTheme = getSystemTheme();
          break;
      }
      
      setActualTheme(newActualTheme);
      
      // Update document class
      document.documentElement.classList.remove('light', 'dark');
      document.documentElement.classList.add(newActualTheme);
      
      // Update meta theme-color
      const metaThemeColor = document.querySelector('meta[name="theme-color"]');
      if (metaThemeColor) {
        metaThemeColor.setAttribute(
          'content', 
          newActualTheme === 'dark' ? '#0f172a' : '#ffffff'
        );
      }
    };

    updateActualTheme();

    // Save theme preference
    localStorage.setItem('trademaster-theme', theme);

    // Listen for system theme changes when in auto mode
    if (theme === 'auto') {
      const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
      const handleChange = () => updateActualTheme();
      
      mediaQuery.addEventListener('change', handleChange);
      return () => mediaQuery.removeEventListener('change', handleChange);
    }
  }, [theme]);

  // Toggle between themes
  const toggleTheme = () => {
    const themeOrder: Theme[] = ['light', 'dark', 'auto'];
    const currentIndex = themeOrder.indexOf(theme);
    const nextIndex = (currentIndex + 1) % themeOrder.length;
    setTheme(themeOrder[nextIndex]);
  };

  const handleSetTheme = (newTheme: Theme) => {
    setTheme(newTheme);
  };

  const contextValue: ThemeContextType = {
    theme,
    actualTheme,
    setTheme: handleSetTheme,
    toggleTheme,
  };

  return (
    <ThemeContext.Provider value={contextValue}>
      {children}
    </ThemeContext.Provider>
  );
}