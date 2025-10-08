import { useState, useEffect } from 'react';
import { Button } from '@haven/ui';

interface QuickHideButtonProps {
  onClick: () => void;
  variant?: 'default' | 'outline' | 'ghost';
  size?: 'sm' | 'md' | 'lg';
  showText?: boolean;
}

const QuickHideButton: React.FC<QuickHideButtonProps> = ({
  onClick,
  variant = 'default',
  size = 'sm',
  showText = false
}) => {
  const [isHovering, setIsHovering] = useState(false);
  const [doubleClickTimer, setDoubleClickTimer] = useState<NodeJS.Timeout | null>(null);
  const [clickCount, setClickCount] = useState(0);

  // Clear timer on unmount
  useEffect(() => {
    return () => {
      if (doubleClickTimer) {
        clearTimeout(doubleClickTimer);
      }
    };
  }, [doubleClickTimer]);

  const handleClick = () => {
    setClickCount(prev => prev + 1);
    
    if (clickCount === 0) {
      // First click - start timer
      const timer = setTimeout(() => {
        setClickCount(0);
      }, 300); // 300ms window for double-click
      setDoubleClickTimer(timer);
    } else {
      // Second click - execute quick hide
      if (doubleClickTimer) {
        clearTimeout(doubleClickTimer);
        setDoubleClickTimer(null);
      }
      setClickCount(0);
      handleQuickHide();
    }
  };

  const handleQuickHide = () => {
    // Create overlay to mask screen immediately
    const overlay = document.createElement('div');
    overlay.style.cssText = `
      position: fixed;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      background: #f9fafb;
      z-index: 9999;
      display: flex;
      align-items: center;
      justify-content: center;
      font-family: system-ui, sans-serif;
    `;
    overlay.innerHTML = `
      <div style="text-align: center;">
        <div style="margin-bottom: 20px;">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect>
            <line x1="16" y1="2" x2="16" y2="6"></line>
            <line x1="8" y1="2" x2="8" y2="6"></line>
            <line x1="3" y1="10" x2="21" y2="10"></line>
          </svg>
        </div>
        <h2 style="margin: 0 0 8px 0; font-size: 20px; font-weight: 600;">Calendar</h2>
        <p style="margin: 0; color: #6b7280;">Loading events...</p>
      </div>
    `;
    
    document.body.appendChild(overlay);
    
    // Hide the actual content
    const appContainer = document.getElementById('app-container') || document.body;
    const originalDisplay = appContainer.style.display;
    appContainer.style.display = 'none';
    
    // Trigger the actual navigation
    setTimeout(() => {
      onClick();
      
      // Clean up after a short delay
      setTimeout(() => {
        if (overlay.parentNode) {
          overlay.parentNode.removeChild(overlay);
        }
        appContainer.style.display = originalDisplay;
      }, 500);
    }, 100);
  };

  return (
    <Button
      variant={variant}
      size={size}
      onClick={handleClick}
      onMouseEnter={() => setIsHovering(true)}
      onMouseLeave={() => setIsHovering(false)}
      className={`
        relative
        ${variant === 'default' ? 'bg-red-600 hover:bg-red-700 text-white' : ''}
        ${isHovering ? 'animate-pulse' : ''}
      `}
      title="Double-click to quickly hide screen and redirect to calendar (Shortcut: Ctrl+Shift+H)"
    >
      <div className="flex items-center space-x-2">
        <svg 
          className="w-4 h-4" 
          fill="none" 
          viewBox="0 0 24 24" 
          stroke="currentColor"
        >
          <path 
            strokeLinecap="round" 
            strokeLinejoin="round" 
            strokeWidth={2} 
            d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21" 
          />
        </svg>
        {showText && <span>Quick-Hide</span>}
        <svg className="w-3 h-3" fill="currentColor" viewBox="0 0 20 20">
          <path fillRule="evenodd" d="M5 9V7a5 5 0 0110 0v2a2 2 0 012 2v5a2 2 0 01-2 2H5a2 2 0 01-2-2v-5a2 2 0 012-2zm8-2v2H7V7a3 3 0 016 0z" clipRule="evenodd" />
        </svg>
      </div>
      
      {/* Visual indicator for double-click requirement */}
      {clickCount === 1 && (
        <div className="absolute -top-2 -right-2">
          <div className="w-4 h-4 bg-yellow-400 rounded-full animate-ping"></div>
          <div className="absolute inset-0 w-4 h-4 bg-yellow-400 rounded-full"></div>
        </div>
      )}
    </Button>
  );
};

export default QuickHideButton;