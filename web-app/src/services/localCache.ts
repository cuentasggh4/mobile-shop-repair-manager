import {
  AuditLogEntry,
  DriveConnectionMeta,
  RepairJob,
  ShopProfile,
} from '../types/repairShop';

const DB_NAME = 'mobile_repair_shop_pos_db';
const DB_VERSION = 1;
const STORE_KV = 'kv_store';

const STORAGE_KEYS = {
  CLIENT_ID: 'mrs_pos_google_client_id',
  SPREADSHEET_ID: 'mrs_pos_spreadsheet_id',
  FOLDER_ID: 'mrs_pos_folder_id',
  CACHE_FALLBACK_JOBS: 'mrs_pos_cache_jobs',
  CACHE_FALLBACK_PROFILE: 'mrs_pos_cache_profile',
  CACHE_FALLBACK_META: 'mrs_pos_cache_meta',
  CACHE_FALLBACK_AUDIT: 'mrs_pos_cache_audit',
} as const;

export interface CachedWorkspaceState {
  jobs: RepairJob[];
  shopProfile: ShopProfile | null;
  auditLogs: AuditLogEntry[];
  connectionMeta: DriveConnectionMeta | null;
}

function openDatabase(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    if (typeof window === 'undefined' || !('indexedDB' in window)) {
      reject(new Error('IndexedDB is unavailable'));
      return;
    }
    const request = window.indexedDB.open(DB_NAME, DB_VERSION);
    request.onupgradeneeded = () => {
      const db = request.result;
      if (!db.objectStoreNames.contains(STORE_KV)) {
        db.createObjectStore(STORE_KV);
      }
    };
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error ?? new Error('Failed to open IndexedDB'));
  });
}

async function idbGet<T>(key: string): Promise<T | null> {
  try {
    const db = await openDatabase();
    return await new Promise<T | null>((resolve, reject) => {
      const tx = db.transaction(STORE_KV, 'readonly');
      const store = tx.objectStore(STORE_KV);
      const req = store.get(key);
      req.onsuccess = () => resolve((req.result as T) ?? null);
      req.onerror = () => reject(req.error);
    });
  } catch {
    return null;
  }
}

async function idbSet<T>(key: string, value: T): Promise<boolean> {
  try {
    const db = await openDatabase();
    return await new Promise<boolean>((resolve, reject) => {
      const tx = db.transaction(STORE_KV, 'readwrite');
      const store = tx.objectStore(STORE_KV);
      const req = store.put(value, key);
      req.onsuccess = () => resolve(true);
      req.onerror = () => reject(req.error);
    });
  } catch {
    return false;
  }
}

export function getSavedClientId(): string {
  try {
    return localStorage.getItem(STORAGE_KEYS.CLIENT_ID)?.trim() ?? '';
  } catch {
    return '';
  }
}

export function saveClientId(clientId: string): void {
  try {
    const trimmed = clientId.trim();
    if (trimmed) {
      localStorage.setItem(STORAGE_KEYS.CLIENT_ID, trimmed);
    } else {
      localStorage.removeItem(STORAGE_KEYS.CLIENT_ID);
    }
  } catch {
    // Ignore storage quota errors
  }
}

export function getSavedSpreadsheetId(): string {
  try {
    return localStorage.getItem(STORAGE_KEYS.SPREADSHEET_ID)?.trim() ?? '';
  } catch {
    return '';
  }
}

export function saveSpreadsheetId(spreadsheetId: string): void {
  try {
    const trimmed = spreadsheetId.trim();
    if (trimmed) {
      localStorage.setItem(STORAGE_KEYS.SPREADSHEET_ID, trimmed);
    } else {
      localStorage.removeItem(STORAGE_KEYS.SPREADSHEET_ID);
    }
  } catch {
    // Ignore storage quota errors
  }
}

export function getSavedFolderId(): string {
  try {
    return localStorage.getItem(STORAGE_KEYS.FOLDER_ID)?.trim() ?? '';
  } catch {
    return '';
  }
}

export function saveFolderId(folderId: string): void {
  try {
    const trimmed = folderId.trim();
    if (trimmed) {
      localStorage.setItem(STORAGE_KEYS.FOLDER_ID, trimmed);
    } else {
      localStorage.removeItem(STORAGE_KEYS.FOLDER_ID);
    }
  } catch {
    // Ignore storage quota errors
  }
}

export async function loadCachedWorkspace(): Promise<CachedWorkspaceState> {
  const [jobs, shopProfile, auditLogs, connectionMeta] = await Promise.all([
    idbGet<RepairJob[]>('jobs'),
    idbGet<ShopProfile>('shopProfile'),
    idbGet<AuditLogEntry[]>('auditLogs'),
    idbGet<DriveConnectionMeta>('connectionMeta'),
  ]);

  if (jobs || shopProfile || connectionMeta) {
    return {
      jobs: jobs ?? [],
      shopProfile: shopProfile ?? null,
      auditLogs: auditLogs ?? [],
      connectionMeta: connectionMeta ?? null,
    };
  }

  // Fallback to localStorage if IndexedDB had nothing
  try {
    const lsJobs = localStorage.getItem(STORAGE_KEYS.CACHE_FALLBACK_JOBS);
    const lsProfile = localStorage.getItem(STORAGE_KEYS.CACHE_FALLBACK_PROFILE);
    const lsMeta = localStorage.getItem(STORAGE_KEYS.CACHE_FALLBACK_META);
    const lsAudit = localStorage.getItem(STORAGE_KEYS.CACHE_FALLBACK_AUDIT);
    return {
      jobs: lsJobs ? (JSON.parse(lsJobs) as RepairJob[]) : [],
      shopProfile: lsProfile ? (JSON.parse(lsProfile) as ShopProfile) : null,
      auditLogs: lsAudit ? (JSON.parse(lsAudit) as AuditLogEntry[]) : [],
      connectionMeta: lsMeta ? (JSON.parse(lsMeta) as DriveConnectionMeta) : null,
    };
  } catch {
    return {
      jobs: [],
      shopProfile: null,
      auditLogs: [],
      connectionMeta: null,
    };
  }
}

export async function saveCachedJobs(jobs: RepairJob[]): Promise<void> {
  const ok = await idbSet('jobs', jobs);
  if (!ok) {
    try {
      localStorage.setItem(STORAGE_KEYS.CACHE_FALLBACK_JOBS, JSON.stringify(jobs));
    } catch {
      // Ignore
    }
  }
}

export async function saveCachedShopProfile(profile: ShopProfile): Promise<void> {
  const ok = await idbSet('shopProfile', profile);
  if (!ok) {
    try {
      localStorage.setItem(STORAGE_KEYS.CACHE_FALLBACK_PROFILE, JSON.stringify(profile));
    } catch {
      // Ignore
    }
  }
}

export async function saveCachedAuditLogs(logs: AuditLogEntry[]): Promise<void> {
  const ok = await idbSet('auditLogs', logs);
  if (!ok) {
    try {
      localStorage.setItem(STORAGE_KEYS.CACHE_FALLBACK_AUDIT, JSON.stringify(logs));
    } catch {
      // Ignore
    }
  }
}

export async function saveCachedConnectionMeta(meta: DriveConnectionMeta | null): Promise<void> {
  const ok = await idbSet('connectionMeta', meta);
  if (!ok) {
    try {
      if (meta) {
        localStorage.setItem(STORAGE_KEYS.CACHE_FALLBACK_META, JSON.stringify(meta));
      } else {
        localStorage.removeItem(STORAGE_KEYS.CACHE_FALLBACK_META);
      }
    } catch {
      // Ignore
    }
  }
}
