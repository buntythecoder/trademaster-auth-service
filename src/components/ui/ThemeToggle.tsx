import React from 'react';
import { Monitor, Moon, Sun } from 'lucide-react';
import { useTheme } from '@/contexts/ThemeContext';
import { cn } from '@/lib/utils';

interface ThemeToggleProps {
  className?: string;
  variant?: 'button' | 'dropdown';
  showLabel?: boolean;
}

export function ThemeToggle({ className, variant = 'button', showLabel = false }: ThemeToggleProps) {
  const { theme, actualTheme, setTheme, toggleTheme } = useTheme();

  const themeConfig = {
    light: {
      icon: Sun,
      label: 'Light',
      color: 'text-yellow-500'
    },
    dark: {
      icon: Moon,
      label: 'Dark',
      color: 'text-blue-400'
    },
    auto: {
      icon: Monitor,
      label: 'Auto',
      color: 'text-purple-400'
    }
  };

  const currentConfig = themeConfig[theme];
  const CurrentIcon = currentConfig.icon;

  if (variant === 'dropdown') {
    return (
      <div className={cn("relative group", className)}>
        <button
          onClick={toggleTheme}
          className={cn(
            "flex items-center gap-2 px-3 py-2 rounded-xl transition-all duration-200",
            "bg-slate-800/50 hover:bg-slate-700/50 border border-slate-700 hover:border-slate-600",
            "text-slate-300 hover:text-white cyber-glow-hover",
            currentConfig.color
          )}
          title={`Current theme: ${currentConfig.label}`}
        >
          <CurrentIcon className="w-4 h-4" />
          {showLabel && <span className="text-sm font-medium">{currentConfig.label}</span>}
        </button>

        {/* Dropdown menu */}
        <div className="absolute right-0 top-full mt-2 opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all duration-200 z-50">
          <div className="bg-slate-800 border border-slate-700 rounded-xl p-2 min-w-[120px] shadow-2xl">
            {Object.entries(themeConfig).map(([themeKey, config]) => {
              const Icon = config.icon;
              const isActive = theme === themeKey;
              
              return (
                <button
                  key={themeKey}
                  onClick={() => setTheme(themeKey as any)}
                  className={cn(
                    "flex items-center gap-3 w-full px-3 py-2 rounded-lg transition-all duration-150 text-sm",
                    isActive
                      ? "bg-slate-700 text-white"
                      : "text-slate-400 hover:text-white hover:bg-slate-750"
                  )}
                >
                  <Icon className={cn("w-4 h-4", isActive ? config.color : "")} />
                  <span>{config.label}</span>
                  {isActive && <div className="ml-auto w-2 h-2 rounded-full bg-cyan-400" />}
                </button>
              );
            })}
          </div>
        </div>
      </div>
    );
  }

  return (
    <button
      onClick={toggleTheme}
      className={cn(
        "p-2 rounded-lg transition-colors",
        "text-slate-400 hover:text-white hover:bg-slate-800/50",
        currentConfig.color,
        className
      )}
      title={`Current theme: ${currentConfig.label} (${actualTheme})`}
    >
      <CurrentIcon className="w-4 h-4" />
    </button>
  );
}

// Advanced theme selector with visual preview
interface ThemeSelectionPanelProps {
  className?: string;
  onThemeSelect?: (theme: string) => void;
}

export function ThemeSelectionPanel({ className, onThemeSelect }: ThemeSelectionPanelProps) {
  const { theme, setTheme } = useTheme();

  const themes = [
    {
      id: 'light',
      name: 'Light',
      icon: Sun,
      description: 'Clean and bright interface',
      preview: 'bg-gradient-to-br from-white to-slate-100',
      accentColor: 'from-yellow-400 to-orange-500'
    },
    {
      id: 'dark',
      name: 'Dark',
      icon: Moon,
      description: 'Easy on the eyes, perfect for trading',
      preview: 'bg-gradient-to-br from-slate-900 to-slate-800',
      accentColor: 'from-blue-400 to-cyan-500'
    },
    {
      id: 'auto',
      name: 'System',
      icon: Monitor,
      description: 'Follows your system preference',
      preview: 'bg-gradient-to-br from-slate-600 via-slate-700 to-slate-900',
      accentColor: 'from-purple-400 to-pink-500'
    }
  ];

  const handleThemeSelect = (themeId: string) => {
    setTheme(themeId as any);
    onThemeSelect?.(themeId);
  };

  return (
    <div className={cn("space-y-4", className)}>
      <div className="text-sm font-medium text-slate-300 mb-6">Choose your interface theme</div>
      
      <div className="grid grid-cols-1 gap-4">
        {themes.map((themeOption) => {
          const Icon = themeOption.icon;
          const isSelected = theme === themeOption.id;
          
          return (
            <button
              key={themeOption.id}
              onClick={() => handleThemeSelect(themeOption.id)}
              className={cn(
                "relative p-4 rounded-2xl border-2 transition-all duration-300 text-left",
                "hover:scale-[1.02] transform group",
                isSelected
                  ? "border-cyan-400 bg-cyan-400/10 cyber-glow"
                  : "border-slate-700 bg-slate-800/50 hover:border-slate-600 hover:bg-slate-800/70"
              )}
            >
              <div className="flex items-start gap-4">
                {/* Theme Preview */}
                <div className={cn(
                  "w-12 h-12 rounded-xl relative overflow-hidden",
                  themeOption.preview
                )}>
                  <div className={cn(
                    "absolute inset-0 bg-gradient-to-br opacity-20",
                    themeOption.accentColor
                  )} />
                  <div className="absolute inset-0 flex items-center justify-center">
                    <Icon className="w-5 h-5 text-white/80" />
                  </div>
                </div>
                
                {/* Theme Info */}
                <div className="flex-1">
                  <div className="flex items-center gap-2 mb-1">
                    <span className="font-medium text-white">{themeOption.name}</span>
                    {isSelected && (
                      <div className="w-2 h-2 rounded-full bg-cyan-400 animate-pulse" />
                    )}
                  </div>
                  <p className="text-sm text-slate-400">{themeOption.description}</p>
                </div>
              </div>
              
              {/* Selection indicator */}
              {isSelected && (
                <div className="absolute top-2 right-2">
                  <div className="w-6 h-6 rounded-full bg-cyan-400 flex items-center justify-center">
                    <svg className="w-3 h-3 text-slate-900" fill="currentColor" viewBox="0 0 20 20">
                      <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                    </svg>
                  </div>
                </div>
              )}
            </button>
          );
        })}
      </div>
    </div>
  );
}