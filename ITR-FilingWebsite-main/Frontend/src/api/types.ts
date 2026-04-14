export type AssessmentYear = '2025-26' | '2026-27';
export type ITRForm = 'ITR-1' | 'ITR-2' | 'ITR-3' | 'ITR-4';
export type ClientType = 'Individual' | 'HUF' | 'Firm' | 'Company' | 'AOP' | 'BOI';
export type FilingStatus = 'Doc Pending' | 'Reconciliation' | 'Mismatch Review' | 'Ready to File' | 'Filed' | 'E-Verify Pending';
export type ReconStatus = 'Clean' | 'Mismatch' | 'Pending' | 'In Progress';
export type NoticeSection = '143(1)(a)' | '143(2)' | '144' | '148' | '156' | '154' | '139(9)';
export type NoticePriority = 'Urgent' | 'High' | 'Normal';

export interface Client {
  id: number;
  name: string;
  pan: string;
  aadhaarMasked: string;
  type: ClientType;
  mobile: string;
  email: string;
  dateOfBirth: string;
  residencyStatus: 'ROR' | 'RNOR' | 'NR';
  itrForm: ITRForm;
  assessmentYear: AssessmentYear;
  filingStatus: FilingStatus;
  reconStatus: ReconStatus;
  evStatus: 'Verified' | 'Pending' | 'Not Filed';
  lastSynced: string;
  watchList: boolean;
  initials: string;
  communicationChannel: 'WhatsApp' | 'Email' | 'SMS';
}

export interface Notice {
  id: number;
  clientId: number;
  clientName: string;
  pan: string;
  section: NoticeSection;
  assessmentYear: string;
  receivedDate: string;
  dueDate: string;
  priority: NoticePriority;
  amount?: number;
  status: 'Open' | 'Under Review' | 'Responded' | 'Closed';
  notes?: string;
}

export interface BatchJob {
  jobId: number;
  jobName: string;
  startedAt: string;
  scope: string;
  assessmentYear: string;
  total: number;
  success: number;
  failed: number;
  duration?: string;
  status: 'RUNNING' | 'COMPLETED' | 'FAILED' | 'QUEUED' | 'PARTIAL';
  progressPct: number;
}

export interface Invoice {
  id: string;
  clientId: number;
  clientName: string;
  pan: string;
  service: string;
  amount: number;
  status: 'Paid' | 'Pending' | 'Overdue' | 'Partial';
  createdAt: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}
