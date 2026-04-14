import React from 'react';

interface SlidePanelProps {
  isOpen: boolean;
  onClose: () => void;
  title: string;
  children: React.ReactNode;
  width?: string;
}

export default function SlidePanel({ isOpen, onClose, title, children, width = '480px' }: SlidePanelProps) {
  if (!isOpen) return null;

  return (
    <>
      <div className="fixed inset-0 bg-black/20 z-[199]" onClick={onClose} />
      <div
        className="fixed top-0 right-0 h-screen bg-bg-card shadow-2xl z-[200] flex flex-col animate-slide-in"
        style={{ width }}
      >
        <div className="flex items-center justify-between p-5 px-6 border-b border-border flex-shrink-0">
          <h2 className="font-serif text-[18px] font-semibold text-text-primary">{title}</h2>
          <button
            onClick={onClose}
            className="text-text-muted hover:text-text-primary transition-colors p-1"
          >
            <svg width="20" height="20" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M5 5l10 10M15 5L5 15" />
            </svg>
          </button>
        </div>
        <div className="flex-1 overflow-y-auto p-6">
          {children}
        </div>
      </div>
    </>
  );
}
