import { create } from 'zustand';
import type { AssessmentYear } from '../api/types';

type View = 'dashboard' | 'clients' | 'filing' | 'reconciliation' | 'sync' | 'jobs' | 'notices' | 'calendar' | 'tasks' | 'billing' | 'accounting' | 'reports' | 'communication';

interface AppStore {
  activeView: View;
  setActiveView: (v: View) => void;
  activeAY: AssessmentYear;
  setActiveAY: (ay: AssessmentYear) => void;
  globalSearch: string;
  setGlobalSearch: (q: string) => void;
  selectedClientId: number | null;
  setSelectedClientId: (id: number | null) => void;
  computationClientId: number | null;
  openComputation: (id: number) => void;
  closeComputation: () => void;
}

export const useAppStore = create<AppStore>((set) => ({
  activeView: 'dashboard',
  setActiveView: (v) => set({ activeView: v }),
  activeAY: '2025-26',
  setActiveAY: (ay) => set({ activeAY: ay }),
  globalSearch: '',
  setGlobalSearch: (q) => set({ globalSearch: q }),
  selectedClientId: null,
  setSelectedClientId: (id) => set({ selectedClientId: id }),
  computationClientId: null,
  openComputation: (id) => set({ computationClientId: id }),
  closeComputation: () => set({ computationClientId: null }),
}));
