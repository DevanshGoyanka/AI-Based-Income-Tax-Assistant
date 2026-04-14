import { createContext, useContext, useState, type ReactNode } from 'react';

interface AYContextType {
  ay: string;
  ayParam: string | null;
  setAY: (ay: string) => void;
}

const AYContext = createContext<AYContextType>({
  ay: 'AY 2025–26', ayParam: '2025-26', setAY: () => {}
});

export const AYProvider = ({ children }: { children: ReactNode }) => {
  const [ay, setAYState] = useState('AY 2025–26');
  const ayParamMap: Record<string, string | null> = {
    'AY 2025–26': '2025-26',
    'AY 2026–27': '2026-27',
    'Both AYs': null,
  };
  const setAY = (v: string) => setAYState(v);
  return (
    <AYContext.Provider value={{ ay, ayParam: ayParamMap[ay], setAY }}>
      {children}
    </AYContext.Provider>
  );
};

export const useAY = () => useContext(AYContext);
