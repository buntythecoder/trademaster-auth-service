import React from 'react';

export function FloatingShapes() {
  return (
    <>
      {/* Floating geometric shapes */}
      <div className="absolute inset-0 pointer-events-none overflow-hidden">
        {/* Large circle - top right */}
        <div className="absolute -top-32 -right-32 w-64 h-64 rounded-full bg-gradient-to-br from-blue-500/10 to-purple-500/10 animate-float-slow" />
        
        {/* Medium triangle - bottom left */}
        <div className="absolute -bottom-16 -left-16 w-32 h-32 bg-gradient-to-tr from-cyan-500/10 to-blue-500/10 transform rotate-45 animate-float-reverse" />
        
        {/* Small hexagon - center right */}
        <div className="absolute top-1/2 -right-8 w-16 h-16 bg-gradient-to-r from-purple-500/10 to-pink-500/10 transform rotate-12 animate-pulse-slow" />
        
        {/* Floating rectangle - top center */}
        <div className="absolute top-20 left-1/2 -translate-x-1/2 w-24 h-8 bg-gradient-to-r from-green-500/10 to-cyan-500/10 transform -rotate-12 animate-float" />
        
        {/* Small circles scattered */}
        <div className="absolute top-1/4 left-1/4 w-4 h-4 rounded-full bg-blue-400/20 animate-ping-slow" />
        <div className="absolute top-3/4 right-1/4 w-6 h-6 rounded-full bg-purple-400/20 animate-ping-slow animation-delay-1000" />
        <div className="absolute top-1/3 right-1/3 w-3 h-3 rounded-full bg-cyan-400/20 animate-ping-slow animation-delay-2000" />
        
        {/* Grid pattern overlay */}
        <div className="absolute inset-0 opacity-5">
          <div 
            className="w-full h-full" 
            style={{
              backgroundImage: `
                linear-gradient(rgba(59, 130, 246, 0.1) 1px, transparent 1px),
                linear-gradient(90deg, rgba(59, 130, 246, 0.1) 1px, transparent 1px)
              `,
              backgroundSize: '50px 50px'
            }}
          />
        </div>
        
        {/* Radial gradient overlay */}
        <div className="absolute inset-0 bg-gradient-radial from-transparent via-transparent to-black/20" />
      </div>

      {/* CSS animations */}
      <style jsx>{`
        @keyframes float {
          0%, 100% { transform: translateY(0px) rotate(0deg); }
          33% { transform: translateY(-10px) rotate(5deg); }
          66% { transform: translateY(5px) rotate(-3deg); }
        }
        
        @keyframes float-reverse {
          0%, 100% { transform: translateY(0px) rotate(45deg); }
          33% { transform: translateY(8px) rotate(50deg); }
          66% { transform: translateY(-12px) rotate(40deg); }
        }
        
        @keyframes float-slow {
          0%, 100% { transform: translateY(0px); }
          50% { transform: translateY(-20px); }
        }
        
        @keyframes ping-slow {
          0% { transform: scale(1); opacity: 0.5; }
          50% { transform: scale(1.1); opacity: 0.8; }
          100% { transform: scale(1); opacity: 0.5; }
        }
        
        @keyframes pulse-slow {
          0%, 100% { opacity: 0.1; transform: rotate(12deg) scale(1); }
          50% { opacity: 0.2; transform: rotate(18deg) scale(1.05); }
        }
        
        .animate-float {
          animation: float 6s ease-in-out infinite;
        }
        
        .animate-float-reverse {
          animation: float-reverse 8s ease-in-out infinite;
        }
        
        .animate-float-slow {
          animation: float-slow 10s ease-in-out infinite;
        }
        
        .animate-ping-slow {
          animation: ping-slow 4s ease-in-out infinite;
        }
        
        .animate-pulse-slow {
          animation: pulse-slow 5s ease-in-out infinite;
        }
        
        .animation-delay-1000 {
          animation-delay: 1s;
        }
        
        .animation-delay-2000 {
          animation-delay: 2s;
        }
      `}</style>
    </>
  );
}