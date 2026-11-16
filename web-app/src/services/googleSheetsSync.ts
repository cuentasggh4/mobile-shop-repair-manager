import {
  AUDIT_LOG_HEADERS,
  AUDIT_LOG_TAB_NAME,
  AuditLogEntry,
  createDefaultShopProfile,
  DRIVE_FOLDER_NAME,
  DriveConnectionMeta,
  JOBS_HEADERS,
  JOBS_TAB_NAME,
  parseJobRow,
  parseShopProfileRow,
  RepairJob,
  serializeAuditLogRow,
  serializeJobRow,
  serializeShopProfileRow,
  SHOP_PROFILE_HEADERS,
  SHOP_PROFILE_TAB_NAME,
  ShopProfile,
  SPREADSHEET_NAME,
} from '../types/repairShop';
import {
  getSavedFolderId,
  getSavedSpreadsheetId,
  saveFolderId,
  saveSpreadsheetId,
} from './localCache';

const DRIVE_API_BASE = 'https://www.googleapis.com/drive/v3';
const SHEETS_API_BASE = 'https://sheets.googleapis.com/v4';

export class GoogleApiError extends Error {
  status: number;
  constructor(message: string, status: number) {
    super(message);
    this.name = 'GoogleApiError';
    this.status = status;
  }
}

async function fetchGoogleJson<T>(
  url: string,
  accessToken: string,
  init?: RequestInit
): Promise<T> {
  const res = await fetch(url, {
    ...init,
    headers: {
      Authorization: `Bearer ${accessToken}`,
      'Content-Type': 'application/json',
      ...(init?.headers ?? {}),
    },
  });

  if (!res.ok) {
    let message = `Google API error (${res.status})`;
    try {
      const errBody = await res.json();
      if (errBody?.error?.message) {
        message = errBody.error.message;
      }
    } catch {
      // Fallback to status text
    }
    throw new GoogleApiError(message, res.status);
  }

  return (await res.json()) as T;
}

export function extractSpreadsheetId(input: string): string | null {
  const trimmed = input.trim();
  if (!trimmed) return null;

  const urlMatch = trimmed.match(/\/spreadsheets\/d\/([a-zA-Z0-9-_]+)/);
  if (urlMatch && urlMatch[1]) {
    return urlMatch[1];
  }

  if (/^[a-zA-Z0-9-_]{20,}$/.test(trimmed)) {
    return trimmed;
  }

  return null;
}

interface DriveFilesListResponse {
  files?: Array<{ id: string; name: string; parents?: string[] }>;
}

interface DriveFileResponse {
  id: string;
  name: string;
}

export async function findOrCreateShopFolder(accessToken: string): Promise<{
  id: string;
  name: string;
}> {
  const cachedFolderId = getSavedFolderId();
  if (cachedFolderId) {
    try {
      const existing = await fetchGoogleJson<{ id: string; name: string; trashed?: boolean }>(
        `${DRIVE_API_BASE}/files/${encodeURIComponent(cachedFolderId)}?fields=id,name,trashed`,
        accessToken
      );
      if (existing.id && !existing.trashed) {
        return { id: existing.id, name: existing.name };
      }
    } catch {
      // Folder inaccessible or deleted; fall back to search/create
    }
  }

  const q = encodeURIComponent(
    `name = '${DRIVE_FOLDER_NAME}' and mimeType = 'application/vnd.google-apps.folder' and trashed = false`
  );
  const searchRes = await fetchGoogleJson<DriveFilesListResponse>(
    `${DRIVE_API_BASE}/files?q=${q}&fields=files(id,name)&spaces=drive`,
    accessToken
  );

  if (searchRes.files && searchRes.files.length > 0) {
    const folder = searchRes.files[0];
    saveFolderId(folder.id);
    return { id: folder.id, name: folder.name };
  }

  const created = await fetchGoogleJson<DriveFileResponse>(
    `${DRIVE_API_BASE}/files?fields=id,name`,
    accessToken,
    {
      method: 'POST',
      body: JSON.stringify({
        name: DRIVE_FOLDER_NAME,
        mimeType: 'application/vnd.google-apps.folder',
      }),
    }
  );
  saveFolderId(created.id);
  return { id: created.id, name: created.name };
}

interface SpreadsheetMetadata {
  spreadsheetId: string;
  properties?: { title?: string };
  sheets?: Array<{
    properties?: {
      sheetId: number;
      title: string;
      index?: number;
    };
  }>;
  newlyCreated?: boolean;
}

export async function findOrCreateSpreadsheet(
  accessToken: string,
  folderId: string,
  explicitSpreadsheetId?: string
): Promise<SpreadsheetMetadata> {
  const targetId = explicitSpreadsheetId || getSavedSpreadsheetId();

  if (targetId) {
    try {
      const meta = await fetchGoogleJson<SpreadsheetMetadata>(
        `${SHEETS_API_BASE}/spreadsheets/${encodeURIComponent(targetId)}?fields=spreadsheetId,properties.title,sheets.properties(sheetId,title,index)`,
        accessToken
      );
      saveSpreadsheetId(meta.spreadsheetId);
      return { ...meta, newlyCreated: false };
    } catch (err) {
      if (explicitSpreadsheetId) {
        throw err;
      }
      // Saved sheet ID no longer accessible; continue to search in folder
    }
  }

  const q = encodeURIComponent(
    `name = '${SPREADSHEET_NAME}' and '${folderId}' in parents and mimeType = 'application/vnd.google-apps.spreadsheet' and trashed = false`
  );
  const searchRes = await fetchGoogleJson<DriveFilesListResponse>(
    `${DRIVE_API_BASE}/files?q=${q}&fields=files(id,name)&spaces=drive`,
    accessToken
  );

  if (searchRes.files && searchRes.files.length > 0) {
    const sheetId = searchRes.files[0].id;
    const meta = await fetchGoogleJson<SpreadsheetMetadata>(
      `${SHEETS_API_BASE}/spreadsheets/${encodeURIComponent(sheetId)}?fields=spreadsheetId,properties.title,sheets.properties(sheetId,title,index)`,
      accessToken
    );
    saveSpreadsheetId(meta.spreadsheetId);
    return { ...meta, newlyCreated: false };
  }

  // Create spreadsheet directly inside the Mobile Repair Shop Drive folder
  const createdFile = await fetchGoogleJson<DriveFileResponse>(
    `${DRIVE_API_BASE}/files?fields=id,name`,
    accessToken,
    {
      method: 'POST',
      body: JSON.stringify({
        name: SPREADSHEET_NAME,
        mimeType: 'application/vnd.google-apps.spreadsheet',
        parents: [folderId],
      }),
    }
  );

  saveSpreadsheetId(createdFile.id);
  const createdMeta = await fetchGoogleJson<SpreadsheetMetadata>(
    `${SHEETS_API_BASE}/spreadsheets/${encodeURIComponent(createdFile.id)}?fields=spreadsheetId,properties.title,sheets.properties(sheetId,title,index)`,
    accessToken
  );
  return { ...createdMeta, newlyCreated: true };
}

interface BatchGetResponse {
  spreadsheetId: string;
  valueRanges?: Array<{
    range: string;
    values?: string[][];
  }>;
}

export async function ensureExactSheetSchema(
  accessToken: string,
  meta: SpreadsheetMetadata
): Promise<{
  jobs: boolean;
  shopProfile: boolean;
  auditLog: boolean;
}> {
  const spreadsheetId = meta.spreadsheetId;
  const existingTitles = new Set(
    (meta.sheets ?? []).map((s) => s.properties?.title).filter(Boolean) as string[]
  );

  const requiredTabs = [JOBS_TAB_NAME, SHOP_PROFILE_TAB_NAME, AUDIT_LOG_TAB_NAME];
  const missingTabs = requiredTabs.filter((tab) => !existingTitles.has(tab));

  if (missingTabs.length > 0) {
    const batchRequests: Array<Record<string, unknown>> = missingTabs.map((title) => ({
      addSheet: {
        properties: { title },
      },
    }));

    // Remove the empty default Sheet1 only when we created a brand-new spreadsheet
    if (meta.newlyCreated && existingTitles.size === 1) {
      const defaultSheet = meta.sheets?.find((s) => s.properties?.title === 'Sheet1');
      if (defaultSheet?.properties?.sheetId !== undefined) {
        batchRequests.push({
          deleteSheet: {
            sheetId: defaultSheet.properties.sheetId,
          },
        });
      }
    }

    await fetchGoogleJson(
      `${SHEETS_API_BASE}/spreadsheets/${encodeURIComponent(spreadsheetId)}:batchUpdate`,
      accessToken,
      {
        method: 'POST',
        body: JSON.stringify({
          requests: batchRequests,
        }),
      }
    );
  }

  // Non-destructively read row 1 of all 3 tabs before ever writing headers
  const rangesParam = [
    `${JOBS_TAB_NAME}!A1:AB1`,
    `${SHOP_PROFILE_TAB_NAME}!A1:N2`,
    `${AUDIT_LOG_TAB_NAME}!A1:F1`,
  ]
    .map((r) => `ranges=${encodeURIComponent(r)}`)
    .join('&');

  const headerCheck = await fetchGoogleJson<BatchGetResponse>(
    `${SHEETS_API_BASE}/spreadsheets/${encodeURIComponent(spreadsheetId)}/values:batchGet?${rangesParam}`,
    accessToken
  );

  const ranges = headerCheck.valueRanges ?? [];
  const jobsRow1 = ranges[0]?.values?.[0] ?? [];
  const profileRows = ranges[1]?.values ?? [];
  const profileRow1 = profileRows[0] ?? [];
  const profileRow2 = profileRows[1] ?? [];
  const auditRow1 = ranges[2]?.values?.[0] ?? [];

  const updates: Array<{ range: string; values: string[][] }> = [];

  if (jobsRow1.length === 0) {
    updates.push({
      range: `${JOBS_TAB_NAME}!A1:AB1`,
      values: [[...JOBS_HEADERS]],
    });
  }

  if (profileRow1.length === 0) {
    updates.push({
      range: `${SHOP_PROFILE_TAB_NAME}!A1:N1`,
      values: [[...SHOP_PROFILE_HEADERS]],
    });
  }

  if (profileRow2.length === 0) {
    const defaultProfile = createDefaultShopProfile();
    updates.push({
      range: `${SHOP_PROFILE_TAB_NAME}!A2:N2`,
      values: [serializeShopProfileRow(defaultProfile)],
    });
  }

  if (auditRow1.length === 0) {
    updates.push({
      range: `${AUDIT_LOG_TAB_NAME}!A1:F1`,
      values: [[...AUDIT_LOG_HEADERS]],
    });
  }

  if (updates.length > 0) {
    await fetchGoogleJson(
      `${SHEETS_API_BASE}/spreadsheets/${encodeURIComponent(spreadsheetId)}/values:batchUpdate`,
      accessToken,
      {
        method: 'POST',
        body: JSON.stringify({
          valueInputOption: 'USER_ENTERED',
          data: updates,
        }),
      }
    );
  }

  return {
    jobs: true,
    shopProfile: true,
    auditLog: true,
  };
}

export interface FullSyncResult {
  connectionMeta: DriveConnectionMeta;
  jobs: RepairJob[];
  shopProfile: ShopProfile;
  auditLogs: AuditLogEntry[];
  pushedLocalChangesCount: number;
}

async function syncSpreadsheetData(
  accessToken: string,
  connectionBase: Omit<DriveConnectionMeta, 'lastSyncedAt'>,
  localJobs: RepairJob[],
  dataOnlyFromRow2 = false
): Promise<FullSyncResult> {
  const rangesParam = (
    dataOnlyFromRow2
      ? [
          `${JOBS_TAB_NAME}!A2:AB`,
          `${SHOP_PROFILE_TAB_NAME}!A2:N2`,
          `${AUDIT_LOG_TAB_NAME}!A2:F`,
        ]
      : [
          `${JOBS_TAB_NAME}!A1:AB`,
          `${SHOP_PROFILE_TAB_NAME}!A1:N2`,
          `${AUDIT_LOG_TAB_NAME}!A1:F`,
        ]
  )
    .map((r) => `ranges=${encodeURIComponent(r)}`)
    .join('&');

  const batchData = await fetchGoogleJson<BatchGetResponse>(
    `${SHEETS_API_BASE}/spreadsheets/${encodeURIComponent(connectionBase.spreadsheetId)}/values:batchGet?${rangesParam}`,
    accessToken
  );

  const valueRanges = batchData.valueRanges ?? [];
  const rawJobsRows = valueRanges[0]?.values ?? [];
  const rawProfileRows = valueRanges[1]?.values ?? [];
  const rawAuditRows = valueRanges[2]?.values ?? [];

  const startIndex = dataOnlyFromRow2 ? 0 : 1;
  const rowOffset = dataOnlyFromRow2 ? 2 : 1;

  // Parse remote jobs starting from row 2
  const remoteJobs: RepairJob[] = [];
  for (let i = startIndex; i < rawJobsRows.length; i++) {
    const row = rawJobsRows[i];
    if (!row || row.every((cell) => String(cell ?? '').trim() === '')) {
      continue;
    }
    remoteJobs.push(parseJobRow(row, i + rowOffset));
  }

  // Zero-data-loss merge by RecordUuid and UpdatedAt
  const mergedMap = new Map<string, RepairJob>();
  for (const rJob of remoteJobs) {
    mergedMap.set(rJob.recordUuid, rJob);
  }

  let pushedLocalChangesCount = 0;
  const rowsToUpdate: Array<{ range: string; values: string[][] }> = [];
  const jobsToAppend: RepairJob[] = [];

  for (const lJob of localJobs) {
    const remote = mergedMap.get(lJob.recordUuid);
    if (!remote) {
      if (!lJob.sheetRow) {
        jobsToAppend.push(lJob);
      }
    } else if (lJob.updatedAt > remote.updatedAt && remote.sheetRow) {
      const updatedJob: RepairJob = { ...lJob, sheetRow: remote.sheetRow };
      mergedMap.set(updatedJob.recordUuid, updatedJob);
      rowsToUpdate.push({
        range: `${JOBS_TAB_NAME}!A${remote.sheetRow}:AB${remote.sheetRow}`,
        values: [serializeJobRow(updatedJob)],
      });
    }
  }

  if (rowsToUpdate.length > 0) {
    await fetchGoogleJson(
      `${SHEETS_API_BASE}/spreadsheets/${encodeURIComponent(connectionBase.spreadsheetId)}/values:batchUpdate`,
      accessToken,
      {
        method: 'POST',
        body: JSON.stringify({
          valueInputOption: 'USER_ENTERED',
          data: rowsToUpdate,
        }),
      }
    );
    pushedLocalChangesCount += rowsToUpdate.length;
  }

  for (const newJob of jobsToAppend) {
    const { job: appended } = await appendJobToSheet(
      accessToken,
      connectionBase.spreadsheetId,
      newJob
    );
    mergedMap.set(appended.recordUuid, appended);
    pushedLocalChangesCount += 1;
  }

  const profileDataRow = dataOnlyFromRow2 ? rawProfileRows[0] : rawProfileRows[1];
  const shopProfile =
    profileDataRow && profileDataRow.length > 0
      ? parseShopProfileRow(profileDataRow)
      : createDefaultShopProfile();

  const auditLogs: AuditLogEntry[] = [];
  for (let i = startIndex; i < rawAuditRows.length; i++) {
    const row = rawAuditRows[i];
    if (!row || row.length === 0) continue;
    auditLogs.push({
      timestamp: Number(row[0] ?? 0) || 0,
      isoTime: String(row[1] ?? ''),
      action: String(row[2] ?? ''),
      recordUuid: String(row[3] ?? ''),
      jobNumber: String(row[4] ?? ''),
      details: String(row[5] ?? ''),
    });
  }

  const connectionMeta: DriveConnectionMeta = {
    ...connectionBase,
    lastSyncedAt: Date.now(),
  };

  return {
    connectionMeta,
    jobs: Array.from(mergedMap.values()).sort((a, b) => b.updatedAt - a.updatedAt),
    shopProfile,
    auditLogs: auditLogs.reverse(),
    pushedLocalChangesCount,
  };
}

export async function connectAndSyncWorkspace(
  accessToken: string,
  options?: {
    explicitSpreadsheetId?: string;
    localJobs?: RepairJob[];
  }
): Promise<FullSyncResult> {
  const folder = await findOrCreateShopFolder(accessToken);
  const meta = await findOrCreateSpreadsheet(
    accessToken,
    folder.id,
    options?.explicitSpreadsheetId
  );
  const schemaVerified = await ensureExactSheetSchema(accessToken, meta);

  return await syncSpreadsheetData(
    accessToken,
    {
      folderId: folder.id,
      folderName: folder.name,
      spreadsheetId: meta.spreadsheetId,
      spreadsheetTitle: meta.properties?.title || SPREADSHEET_NAME,
      spreadsheetUrl: `https://docs.google.com/spreadsheets/d/${meta.spreadsheetId}/edit`,
      schemaVerified,
    },
    options?.localJobs ?? [],
    false
  );
}

/**
 * Fast single-request sync for live automatic polling & tab-focus sync.
 * Reads Jobs!A2:AB, ShopProfile!A2:N2, and AuditLog!A2:F directly.
 */
export async function quickSyncWorkspace(
  accessToken: string,
  existingMeta: DriveConnectionMeta,
  localJobs: RepairJob[]
): Promise<FullSyncResult> {
  return await syncSpreadsheetData(
    accessToken,
    {
      folderId: existingMeta.folderId,
      folderName: existingMeta.folderName,
      spreadsheetId: existingMeta.spreadsheetId,
      spreadsheetTitle: existingMeta.spreadsheetTitle,
      spreadsheetUrl: existingMeta.spreadsheetUrl,
      schemaVerified: existingMeta.schemaVerified,
    },
    localJobs,
    true
  );
}

interface AppendValuesResponse {
  spreadsheetId: string;
  updates?: {
    updatedRange?: string;
  };
}

function parseRowFromUpdatedRange(updatedRange?: string): number | undefined {
  if (!updatedRange) return undefined;
  const match = updatedRange.match(/![A-Z]+(\d+):/);
  if (match && match[1]) {
    const n = Number(match[1]);
    return Number.isFinite(n) ? n : undefined;
  }
  return undefined;
}

export async function appendAuditLog(
  accessToken: string,
  spreadsheetId: string,
  entry: AuditLogEntry
): Promise<AuditLogEntry> {
  const range = encodeURIComponent(`${AUDIT_LOG_TAB_NAME}!A:F`);
  await fetchGoogleJson(
    `${SHEETS_API_BASE}/spreadsheets/${encodeURIComponent(spreadsheetId)}/values/${range}:append?valueInputOption=USER_ENTERED&insertDataOption=INSERT_ROWS`,
    accessToken,
    {
      method: 'POST',
      body: JSON.stringify({
        values: [serializeAuditLogRow(entry)],
      }),
    }
  );
  return entry;
}

export async function appendJobToSheet(
  accessToken: string,
  spreadsheetId: string,
  job: RepairJob,
  auditAction = 'CREATE'
): Promise<{ job: RepairJob; auditEntry: AuditLogEntry }> {
  const range = encodeURIComponent(`${JOBS_TAB_NAME}!A:AB`);
  const res = await fetchGoogleJson<AppendValuesResponse>(
    `${SHEETS_API_BASE}/spreadsheets/${encodeURIComponent(spreadsheetId)}/values/${range}:append?valueInputOption=USER_ENTERED&insertDataOption=INSERT_ROWS`,
    accessToken,
    {
      method: 'POST',
      body: JSON.stringify({
        values: [serializeJobRow(job)],
      }),
    }
  );

  const sheetRow = parseRowFromUpdatedRange(res.updates?.updatedRange);
  const auditEntry = await appendAuditLog(accessToken, spreadsheetId, {
    timestamp: Date.now(),
    isoTime: new Date().toISOString(),
    action: auditAction,
    recordUuid: job.recordUuid,
    jobNumber: job.jobNumber,
    details: `Created job for ${job.customerName} (${job.deviceBrand} ${job.deviceModel}) at row ${sheetRow ?? 'unknown'}`,
  });

  return { job: { ...job, sheetRow }, auditEntry };
}

export async function createNewJobAtomic(
  accessToken: string,
  spreadsheetId: string,
  job: RepairJob,
  nextProfile: ShopProfile
): Promise<{
  job: RepairJob;
  profile: ShopProfile;
  auditEntry: AuditLogEntry;
}> {
  // 1. Append job row to Jobs!A:AB (never overwrite sheet)
  const range = encodeURIComponent(`${JOBS_TAB_NAME}!A:AB`);
  const appendRes = await fetchGoogleJson<AppendValuesResponse>(
    `${SHEETS_API_BASE}/spreadsheets/${encodeURIComponent(spreadsheetId)}/values/${range}:append?valueInputOption=USER_ENTERED&insertDataOption=INSERT_ROWS`,
    accessToken,
    {
      method: 'POST',
      body: JSON.stringify({
        values: [serializeJobRow(job)],
      }),
    }
  );
  const sheetRow = parseRowFromUpdatedRange(appendRes.updates?.updatedRange);
  const createdJob: RepairJob = { ...job, sheetRow };

  // 2. Update ShopProfile!A2:N2 with incremented NextJobNumberSequence, LastJobMonth, UpdatedAt
  const updatedProfile: ShopProfile = {
    ...nextProfile,
    updatedAt: job.createdAt,
  };
  await fetchGoogleJson(
    `${SHEETS_API_BASE}/spreadsheets/${encodeURIComponent(spreadsheetId)}/values:batchUpdate`,
    accessToken,
    {
      method: 'POST',
      body: JSON.stringify({
        valueInputOption: 'USER_ENTERED',
        data: [
          {
            range: `${SHOP_PROFILE_TAB_NAME}!A2:N2`,
            values: [serializeShopProfileRow(updatedProfile)],
          },
        ],
      }),
    }
  );

  // 3. Append "CREATE" row to AuditLog!A:F
  const auditEntry = await appendAuditLog(accessToken, spreadsheetId, {
    timestamp: job.createdAt,
    isoTime: new Date(job.createdAt).toISOString(),
    action: 'CREATE',
    recordUuid: createdJob.recordUuid,
    jobNumber: createdJob.jobNumber,
    details: `Created job ${createdJob.jobNumber} for ${createdJob.customerName} (${createdJob.deviceBrand} ${createdJob.deviceModel}) at row ${sheetRow ?? 'unknown'}`,
  });

  return { job: createdJob, profile: updatedProfile, auditEntry };
}

export async function updateJobInSheet(
  accessToken: string,
  spreadsheetId: string,
  job: RepairJob,
  auditAction = 'UPDATE',
  auditDetails?: string
): Promise<{ job: RepairJob; auditEntry: AuditLogEntry }> {
  if (!job.sheetRow || job.sheetRow < 2) {
    throw new Error(
      `Cannot batchUpdate job ${job.jobNumber}: missing valid sheetRow reference. Run Sync first.`
    );
  }

  const updatedJob: RepairJob = {
    ...job,
    updatedAt: Date.now(),
  };

  await fetchGoogleJson(
    `${SHEETS_API_BASE}/spreadsheets/${encodeURIComponent(spreadsheetId)}/values:batchUpdate`,
    accessToken,
    {
      method: 'POST',
      body: JSON.stringify({
        valueInputOption: 'USER_ENTERED',
        data: [
          {
            range: `${JOBS_TAB_NAME}!A${updatedJob.sheetRow}:AB${updatedJob.sheetRow}`,
            values: [serializeJobRow(updatedJob)],
          },
        ],
      }),
    }
  );

  const auditEntry = await appendAuditLog(accessToken, spreadsheetId, {
    timestamp: Date.now(),
    isoTime: new Date().toISOString(),
    action: auditAction,
    recordUuid: updatedJob.recordUuid,
    jobNumber: updatedJob.jobNumber,
    details:
      auditDetails ||
      `Updated row ${updatedJob.sheetRow} (Stage: ${updatedJob.stage}, IsDeleted: ${updatedJob.isDeleted})`,
  });

  return { job: updatedJob, auditEntry };
}

export async function appendAuditLogsBatch(
  accessToken: string,
  spreadsheetId: string,
  entries: AuditLogEntry[]
): Promise<AuditLogEntry[]> {
  if (entries.length === 0) return [];
  const range = encodeURIComponent(`${AUDIT_LOG_TAB_NAME}!A:F`);
  await fetchGoogleJson(
    `${SHEETS_API_BASE}/spreadsheets/${encodeURIComponent(spreadsheetId)}/values/${range}:append?valueInputOption=USER_ENTERED&insertDataOption=INSERT_ROWS`,
    accessToken,
    {
      method: 'POST',
      body: JSON.stringify({
        values: entries.map(serializeAuditLogRow),
      }),
    }
  );
  return entries;
}

export async function batchUpdateJobsInSheet(
  accessToken: string,
  spreadsheetId: string,
  updates: Array<{ job: RepairJob; auditDetails: string }>
): Promise<{ jobs: RepairJob[]; auditEntries: AuditLogEntry[] }> {
  if (updates.length === 0) {
    return { jobs: [], auditEntries: [] };
  }

  const rowTargetedData: Array<{ range: string; values: string[][] }> = [];
  const resultingJobs: RepairJob[] = [];
  const auditEntries: AuditLogEntry[] = [];
  const baseTime = Date.now();

  for (let i = 0; i < updates.length; i++) {
    const { job, auditDetails } = updates[i];
    const timestamp = job.updatedAt || baseTime + i;

    if (job.sheetRow && job.sheetRow >= 2) {
      const updatedJob: RepairJob = { ...job, updatedAt: timestamp };
      rowTargetedData.push({
        range: `${JOBS_TAB_NAME}!A${updatedJob.sheetRow}:AB${updatedJob.sheetRow}`,
        values: [serializeJobRow(updatedJob)],
      });
      resultingJobs.push(updatedJob);
      auditEntries.push({
        timestamp,
        isoTime: new Date(timestamp).toISOString(),
        action: 'UPDATE',
        recordUuid: updatedJob.recordUuid,
        jobNumber: updatedJob.jobNumber,
        details: auditDetails,
      });
    } else {
      const range = encodeURIComponent(`${JOBS_TAB_NAME}!A:AB`);
      const appendRes = await fetchGoogleJson<AppendValuesResponse>(
        `${SHEETS_API_BASE}/spreadsheets/${encodeURIComponent(spreadsheetId)}/values/${range}:append?valueInputOption=USER_ENTERED&insertDataOption=INSERT_ROWS`,
        accessToken,
        {
          method: 'POST',
          body: JSON.stringify({
            values: [serializeJobRow({ ...job, updatedAt: timestamp })],
          }),
        }
      );
      const sheetRow = parseRowFromUpdatedRange(appendRes.updates?.updatedRange);
      const appendedJob: RepairJob = { ...job, updatedAt: timestamp, sheetRow };
      resultingJobs.push(appendedJob);
      auditEntries.push({
        timestamp,
        isoTime: new Date(timestamp).toISOString(),
        action: 'UPDATE',
        recordUuid: appendedJob.recordUuid,
        jobNumber: appendedJob.jobNumber,
        details: auditDetails,
      });
    }
  }

  if (rowTargetedData.length > 0) {
    await fetchGoogleJson(
      `${SHEETS_API_BASE}/spreadsheets/${encodeURIComponent(spreadsheetId)}/values:batchUpdate`,
      accessToken,
      {
        method: 'POST',
        body: JSON.stringify({
          valueInputOption: 'USER_ENTERED',
          data: rowTargetedData,
        }),
      }
    );
  }

  await appendAuditLogsBatch(accessToken, spreadsheetId, auditEntries);

  return {
    jobs: resultingJobs,
    auditEntries,
  };
}

export async function softDeleteJobInSheet(
  accessToken: string,
  spreadsheetId: string,
  job: RepairJob
): Promise<{ job: RepairJob; auditEntry: AuditLogEntry }> {
  const deletedJob: RepairJob = {
    ...job,
    isDeleted: true,
    updatedAt: Date.now(),
  };
  return await updateJobInSheet(
    accessToken,
    spreadsheetId,
    deletedJob,
    'DELETE',
    `Soft-deleted job ${job.jobNumber} at row ${job.sheetRow ?? 'unknown'}`
  );
}

export async function saveShopProfileToSheet(
  accessToken: string,
  spreadsheetId: string,
  profile: ShopProfile
): Promise<{ profile: ShopProfile; auditEntry: AuditLogEntry }> {
  const updatedProfile: ShopProfile = {
    ...profile,
    updatedAt: Date.now(),
  };

  await fetchGoogleJson(
    `${SHEETS_API_BASE}/spreadsheets/${encodeURIComponent(spreadsheetId)}/values:batchUpdate`,
    accessToken,
    {
      method: 'POST',
      body: JSON.stringify({
        valueInputOption: 'USER_ENTERED',
        data: [
          {
            range: `${SHOP_PROFILE_TAB_NAME}!A2:N2`,
            values: [serializeShopProfileRow(updatedProfile)],
          },
        ],
      }),
    }
  );

  const auditEntry = await appendAuditLog(accessToken, spreadsheetId, {
    timestamp: Date.now(),
    isoTime: new Date().toISOString(),
    action: 'UPDATE_SHOP_PROFILE',
    recordUuid: 'SHOP_PROFILE',
    jobNumber: '-',
    details: `Updated ShopProfile (${updatedProfile.shopName}, Prefix: ${updatedProfile.jobNumberPrefix}, Seq: ${updatedProfile.nextJobNumberSequence})`,
  });

  return { profile: updatedProfile, auditEntry };
}
