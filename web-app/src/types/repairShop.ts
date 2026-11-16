export enum JobStage {
  DIAGNOSTIC = 'DIAGNOSTIC',
  WAITING_FOR_PARTS = 'WAITING_FOR_PARTS',
  IN_PROGRESS = 'IN_PROGRESS',
  READY_FOR_PICKUP = 'READY_FOR_PICKUP',
  DELIVERED = 'DELIVERED',
  CANCELLED = 'CANCELLED',
}

export const ALL_JOB_STAGES: JobStage[] = [
  JobStage.DIAGNOSTIC,
  JobStage.WAITING_FOR_PARTS,
  JobStage.IN_PROGRESS,
  JobStage.READY_FOR_PICKUP,
  JobStage.DELIVERED,
  JobStage.CANCELLED,
];

export const STAGE_LABELS: Record<JobStage, string> = {
  [JobStage.DIAGNOSTIC]: 'Diagnostic',
  [JobStage.WAITING_FOR_PARTS]: 'Waiting for Parts',
  [JobStage.IN_PROGRESS]: 'In Progress',
  [JobStage.READY_FOR_PICKUP]: 'Ready for Pickup',
  [JobStage.DELIVERED]: 'Delivered',
  [JobStage.CANCELLED]: 'Cancelled',
};

export const JOBS_TAB_NAME = 'Jobs';
export const SHOP_PROFILE_TAB_NAME = 'ShopProfile';
export const AUDIT_LOG_TAB_NAME = 'AuditLog';

export const DRIVE_FOLDER_NAME = 'Mobile Repair Shop';
export const SPREADSHEET_NAME = 'MobileRepairShop_DB';

export const JOBS_HEADERS = [
  'JobNumber',
  'CustomerName',
  'CustomerPhone',
  'DeviceBrand',
  'DeviceModel',
  'DeviceColor',
  'ImeiOrSerial',
  'SecurityLockCode',
  'IssueDescription',
  'IssueTagsCsv',
  'Stage',
  'EstimatedCost',
  'AdvancePaid',
  'Discount',
  'FinalCost',
  'IsPaid',
  'PaymentMode',
  'TechnicianNotes',
  'PickedUpByOriginalCustomer',
  'RecipientName',
  'RecipientPhone',
  'ChecklistJson',
  'StatusLogsJson',
  'CreatedAt',
  'UpdatedAt',
  'DeliveredAt',
  'IsDeleted',
  'RecordUuid',
] as const;

export const SHOP_PROFILE_HEADERS = [
  'ShopName',
  'Tagline',
  'ContactPhone',
  'WhatsappNumber',
  'Email',
  'Address',
  'CurrencySymbol',
  'WarrantyDays',
  'TermsAndConditions',
  'NextJobNumberSequence',
  'PreferredWhatsAppPackage',
  'UpdatedAt',
  'JobNumberPrefix',
  'LastJobMonth',
] as const;

export const AUDIT_LOG_HEADERS = [
  'Timestamp',
  'IsoTime',
  'Action',
  'RecordUuid',
  'JobNumber',
  'Details',
] as const;

export interface RepairJob {
  jobNumber: string;
  customerName: string;
  customerPhone: string;
  deviceBrand: string;
  deviceModel: string;
  deviceColor: string;
  imeiOrSerial: string;
  securityLockCode: string;
  issueDescription: string;
  issueTagsCsv: string;
  stage: JobStage;
  estimatedCost: number;
  advancePaid: number;
  discount: number;
  finalCost: number;
  isPaid: boolean;
  paymentMode: string;
  technicianNotes: string;
  pickedUpByOriginalCustomer: boolean;
  recipientName: string;
  recipientPhone: string;
  checklistJson: string;
  statusLogsJson: string;
  createdAt: number;
  updatedAt: number;
  deliveredAt: number | null;
  isDeleted: boolean;
  recordUuid: string;
  /** 1-indexed sheet row in Google Sheets `Jobs` tab (row >= 2). */
  sheetRow?: number;
}

export interface ShopProfile {
  shopName: string;
  tagline: string;
  contactPhone: string;
  whatsappNumber: string;
  email: string;
  address: string;
  currencySymbol: string;
  warrantyDays: number;
  termsAndConditions: string;
  nextJobNumberSequence: number;
  preferredWhatsAppPackage: string;
  updatedAt: number;
  jobNumberPrefix: string;
  lastJobMonth: string;
}

export interface AuditLogEntry {
  timestamp: number;
  isoTime: string;
  action: string;
  recordUuid: string;
  jobNumber: string;
  details: string;
}

export interface DriveConnectionMeta {
  folderId: string;
  folderName: string;
  spreadsheetId: string;
  spreadsheetTitle: string;
  spreadsheetUrl: string;
  lastSyncedAt: number | null;
  schemaVerified: {
    jobs: boolean;
    shopProfile: boolean;
    auditLog: boolean;
  };
}

export type SyncStatus = 'disconnected' | 'connected' | 'syncing' | 'error';

export function createDefaultShopProfile(): ShopProfile {
  return {
    shopName: 'Mobile Repair Shop',
    tagline: '',
    contactPhone: '',
    whatsappNumber: '',
    email: '',
    address: '',
    currencySymbol: '₹',
    warrantyDays: 90,
    termsAndConditions: '',
    nextJobNumberSequence: 1,
    preferredWhatsAppPackage: 'com.whatsapp',
    updatedAt: Date.now(),
    jobNumberPrefix: 'JOB',
    lastJobMonth: '',
  };
}

export function normalizeJobStage(raw: string | undefined): JobStage {
  const upper = (raw ?? '').trim().toUpperCase();
  if (ALL_JOB_STAGES.includes(upper as JobStage)) {
    return upper as JobStage;
  }
  return JobStage.DIAGNOSTIC;
}

function parseBool(value: string | undefined, defaultValue = false): boolean {
  if (value === undefined || value === null || value === '') return defaultValue;
  return String(value).trim().toLowerCase() === 'true' || String(value).trim() === '1';
}

function parseNum(value: string | undefined, defaultValue = 0): number {
  if (value === undefined || value === null || value === '') return defaultValue;
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : defaultValue;
}

function parseOptionalEpoch(value: string | undefined): number | null {
  if (value === undefined || value === null || String(value).trim() === '') return null;
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : null;
}

export function parseJobRow(row: string[], sheetRow: number): RepairJob {
  const get = (idx: number): string => (row[idx] !== undefined && row[idx] !== null ? String(row[idx]) : '');
  const now = Date.now();
  return {
    jobNumber: get(0),
    customerName: get(1),
    customerPhone: get(2),
    deviceBrand: get(3),
    deviceModel: get(4),
    deviceColor: get(5),
    imeiOrSerial: get(6),
    securityLockCode: get(7),
    issueDescription: get(8),
    issueTagsCsv: get(9),
    stage: normalizeJobStage(get(10)),
    estimatedCost: parseNum(get(11), 0),
    advancePaid: parseNum(get(12), 0),
    discount: parseNum(get(13), 0),
    finalCost: parseNum(get(14), 0),
    isPaid: parseBool(get(15), false),
    paymentMode: get(16),
    technicianNotes: get(17),
    pickedUpByOriginalCustomer: parseBool(get(18), true),
    recipientName: get(19),
    recipientPhone: get(20),
    checklistJson: get(21),
    statusLogsJson: get(22) || '[]',
    createdAt: parseNum(get(23), now),
    updatedAt: parseNum(get(24), now),
    deliveredAt: parseOptionalEpoch(get(25)),
    isDeleted: parseBool(get(26), false),
    recordUuid: get(27).trim() || `row-${sheetRow}-${get(0).trim() || 'unassigned'}`,
    sheetRow,
  };
}

export function serializeJobRow(job: RepairJob): string[] {
  return [
    job.jobNumber,
    job.customerName,
    job.customerPhone,
    job.deviceBrand,
    job.deviceModel,
    job.deviceColor,
    job.imeiOrSerial,
    job.securityLockCode,
    job.issueDescription,
    job.issueTagsCsv,
    job.stage,
    String(job.estimatedCost),
    String(job.advancePaid),
    String(job.discount),
    String(job.finalCost),
    job.isPaid ? 'true' : 'false',
    job.paymentMode,
    job.technicianNotes,
    job.pickedUpByOriginalCustomer ? 'true' : 'false',
    job.recipientName,
    job.recipientPhone,
    job.checklistJson || DEFAULT_CHECKLIST_STRING,
    job.statusLogsJson || '[]',
    String(job.createdAt),
    String(job.updatedAt),
    job.deliveredAt ? String(job.deliveredAt) : '',
    job.isDeleted ? 'true' : 'false',
    job.recordUuid,
  ];
}

export function parseShopProfileRow(row: string[]): ShopProfile {
  const get = (idx: number): string => (row[idx] !== undefined && row[idx] !== null ? String(row[idx]) : '');
  const defaults = createDefaultShopProfile();
  return {
    shopName: get(0) || defaults.shopName,
    tagline: get(1) || defaults.tagline,
    contactPhone: get(2),
    whatsappNumber: get(3),
    email: get(4),
    address: get(5),
    currencySymbol: get(6) || defaults.currencySymbol,
    warrantyDays: parseNum(get(7), defaults.warrantyDays),
    termsAndConditions: get(8) || defaults.termsAndConditions,
    nextJobNumberSequence: parseNum(get(9), defaults.nextJobNumberSequence),
    preferredWhatsAppPackage: get(10) || defaults.preferredWhatsAppPackage,
    updatedAt: parseNum(get(11), Date.now()),
    jobNumberPrefix: get(12) || defaults.jobNumberPrefix,
    lastJobMonth: get(13),
  };
}

export function serializeShopProfileRow(profile: ShopProfile): string[] {
  return [
    profile.shopName,
    profile.tagline,
    profile.contactPhone,
    profile.whatsappNumber,
    profile.email,
    profile.address,
    profile.currencySymbol,
    String(profile.warrantyDays),
    profile.termsAndConditions,
    String(profile.nextJobNumberSequence),
    profile.preferredWhatsAppPackage,
    String(profile.updatedAt),
    profile.jobNumberPrefix,
    profile.lastJobMonth,
  ];
}

export function serializeAuditLogRow(entry: AuditLogEntry): string[] {
  return [
    String(entry.timestamp),
    entry.isoTime,
    entry.action,
    entry.recordUuid,
    entry.jobNumber,
    entry.details,
  ];
}

export interface JobFinancialSummary {
  totalCost: number;
  advancePaid: number;
  discount: number;
  balanceDue: number;
  isFullyPaid: boolean;
}

export function computeJobFinancials(job: RepairJob): JobFinancialSummary {
  const totalCost = job.finalCost > 0 ? job.finalCost : job.estimatedCost;
  const advancePaid = Math.max(0, job.advancePaid);
  const discount = Math.max(0, job.discount);
  const balanceDue = job.isPaid ? 0 : Math.max(0, totalCost - discount - advancePaid);
  const isFullyPaid = job.isPaid || (totalCost > 0 && balanceDue === 0);

  return {
    totalCost,
    advancePaid,
    discount,
    balanceDue,
    isFullyPaid,
  };
}

export function digitsOnly(value: string): string {
  return value.replace(/\D+/g, '');
}

export const DEVICE_BRAND_CHIPS = [
  'Apple',
  'Samsung',
  'Xiaomi',
  'Vivo',
  'Oppo',
  'OnePlus',
  'Realme',
  'Google',
  'Motorola',
  'Other',
] as const;

export const ISSUE_TAG_OPTIONS = [
  'Broken Screen',
  'No Display',
  'Battery Drain',
  'Charging Port',
  'Water Damage',
  'Camera Blur',
  'No Sound/Mic',
  'Broken Back',
  'Dead / No Power',
  'Bootloop/Software',
] as const;

export const CHECKLIST_ITEMS = [
  'Screen Touch',
  'Display / LCD',
  'Front Camera',
  'Rear Camera',
  'Microphone',
  'Speaker / Audio',
  'Charging Port',
  'Power / Vol Buttons',
  'Wi-Fi / BT',
  'Fingerprint / FaceID',
  'Body Housing / Dent',
  'SIM / Cellular',
] as const;

export type ChecklistState = 'WORKING' | 'DEFECTIVE' | 'UNTESTED';

export const DEFAULT_CHECKLIST_STRING = Array<ChecklistState>(CHECKLIST_ITEMS.length)
  .fill('UNTESTED')
  .join(';');

export function createDefaultChecklist(): ChecklistState[] {
  return Array<ChecklistState>(CHECKLIST_ITEMS.length).fill('UNTESTED');
}

export function serializeChecklist(states: ChecklistState[]): string {
  return CHECKLIST_ITEMS.map((_, idx) => states[idx] ?? 'UNTESTED').join(';');
}

export function parseChecklist(raw: string | undefined): ChecklistState[] {
  const valid = new Set<ChecklistState>(['WORKING', 'DEFECTIVE', 'UNTESTED']);
  if (!raw || !raw.trim()) {
    return createDefaultChecklist();
  }
  const parts = raw.split(';').map((s) => s.trim().toUpperCase() as ChecklistState);
  return CHECKLIST_ITEMS.map((_, idx) => (valid.has(parts[idx]) ? parts[idx] : 'UNTESTED'));
}

export interface GeneratedJobNumberInfo {
  jobNumber: string;
  sequence: number;
  currentMonth: string;
  dateCode: string;
  prefix: string;
}

export function generateNextJobNumber(
  profile: ShopProfile | null,
  jobs: RepairJob[],
  nowDate: Date = new Date()
): GeneratedJobNumberInfo {
  const yy = String(nowDate.getFullYear()).slice(-2);
  const mm = String(nowDate.getMonth() + 1).padStart(2, '0');
  const dd = String(nowDate.getDate()).padStart(2, '0');

  const currentMonth = `${yy}${mm}`;
  const dateCode = `${yy}${mm}${dd}`;
  const prefix = (profile?.jobNumberPrefix ?? '').trim() || 'JOB';

  let seq =
    profile?.lastJobMonth === currentMonth && (profile?.nextJobNumberSequence ?? 0) >= 1
      ? profile.nextJobNumberSequence
      : 1;

  const existingJobNumbers = new Set<string>();
  const usedMonthSequences = new Set<number>();
  const pattern = /^.*-(\d{6})-(\d+)$/;

  for (const job of jobs) {
    const jn = job.jobNumber.trim();
    if (!jn) continue;
    existingJobNumbers.add(jn.toUpperCase());
    const match = jn.match(pattern);
    if (match) {
      const matchedDate = match[1];
      const matchedSeq = Number(match[2]);
      if (matchedDate.startsWith(currentMonth) && Number.isFinite(matchedSeq)) {
        usedMonthSequences.add(matchedSeq);
      }
    }
  }

  const formatCandidate = (s: number) => `${prefix}-${dateCode}-${String(s).padStart(3, '0')}`;

  while (
    usedMonthSequences.has(seq) ||
    existingJobNumbers.has(formatCandidate(seq).toUpperCase())
  ) {
    seq += 1;
  }

  return {
    jobNumber: formatCandidate(seq),
    sequence: seq,
    currentMonth,
    dateCode,
    prefix,
  };
}

export const PAYMENT_MODES = ['Cash', 'UPI', 'Card', 'Transfer'] as const;
export type PaymentMode = (typeof PAYMENT_MODES)[number];

export interface StatusLogEntry {
  stage: JobStage;
  timestamp: number;
  note: string;
}

export function parseStatusLogs(raw: string | undefined): StatusLogEntry[] {
  if (!raw || !raw.trim()) return [];
  try {
    const parsed = JSON.parse(raw);
    if (!Array.isArray(parsed)) return [];
    return parsed
      .map((item) => ({
        stage: normalizeJobStage(item?.stage),
        timestamp: Number(item?.timestamp) || 0,
        note: typeof item?.note === 'string' ? item.note : '',
      }))
      .sort((a, b) => a.timestamp - b.timestamp);
  } catch {
    return [];
  }
}

export function appendStatusLog(
  existingRaw: string | undefined,
  entry: StatusLogEntry
): string {
  const logs = parseStatusLogs(existingRaw);
  logs.push(entry);
  return JSON.stringify(logs);
}



