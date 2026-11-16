import { useEffect, useState } from 'react';
import {
  Check,
  CheckCircle2,
  Cloud,
  Copy,
  ExternalLink,
  FolderOpen,
  Link2,
  LogOut,
  RefreshCw,
  Sheet,
  X,
} from 'lucide-react';
import {
  DRIVE_FOLDER_NAME,
  DriveConnectionMeta,
  SPREADSHEET_NAME,
  SyncStatus,
} from '../types/repairShop';
import { extractSpreadsheetId } from '../services/googleSheetsSync';

interface DriveConnectModalProps {
  isOpen: boolean;
  syncStatus: SyncStatus;
  connectionMeta: DriveConnectionMeta | null;
  errorMessage: string | null;
  onClose: () => void;
  onConnectAndSync: () => Promise<void>;
  onLinkExistingSheet: (spreadsheetId: string) => Promise<void>;
  onDisconnect: () => void;
}

export function DriveConnectModal({
  isOpen,
  syncStatus,
  connectionMeta,
  errorMessage,
  onClose,
  onConnectAndSync,
  onLinkExistingSheet,
  onDisconnect,
}: DriveConnectModalProps) {
  const [sheetUrlInput, setSheetUrlInput] = useState('');
  const [sheetUrlError, setSheetUrlError] = useState<string | null>(null);
  const [copiedSheetId, setCopiedSheetId] = useState(false);

  useEffect(() => {
    if (isOpen) {
      setSheetUrlError(null);
    }
  }, [isOpen]);

  if (!isOpen) return null;

  const isBusy = syncStatus === 'syncing';

  const handleCopySheetId = async () => {
    if (!connectionMeta?.spreadsheetId) return;
    try {
      await navigator.clipboard.writeText(connectionMeta.spreadsheetId);
      setCopiedSheetId(true);
      window.setTimeout(() => setCopiedSheetId(false), 1800);
    } catch {
      // Clipboard unavailable
    }
  };

  const handleLinkSheet = async () => {
    setSheetUrlError(null);
    const parsedId = extractSpreadsheetId(sheetUrlInput);
    if (!parsedId) {
      setSheetUrlError('Enter a valid Google Sheets URL or Spreadsheet ID.');
      return;
    }
    await onLinkExistingSheet(parsedId);
    setSheetUrlInput('');
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-end bg-slate-950/75 backdrop-blur-sm"
      role="dialog"
      aria-modal="true"
      aria-labelledby="drive-modal-title"
      onClick={(e) => {
        if (e.target === e.currentTarget && !isBusy) onClose();
      }}
    >
      <div className="flex h-full w-full max-w-xl flex-col border-l border-slate-200 bg-slate-50 shadow-2xl dark:border-slate-800 dark:bg-[#0F172A]">
        {/* Drawer Header */}
        <div className="flex items-center justify-between border-b border-slate-200 bg-white px-6 py-4 dark:border-slate-800 dark:bg-[#0F172A]">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-blue-100 text-blue-700 dark:bg-[#2563EB]/15 dark:text-blue-400">
              <Cloud className="h-5 w-5" />
            </div>
            <div>
              <h2 id="drive-modal-title" className="font-display text-base font-semibold text-slate-900 dark:text-slate-100">
                Google Drive &amp; Sheets Sync
              </h2>
              <p className="text-xs text-slate-500 dark:text-slate-400">
                Android-compatible 3-tab schema ({SPREADSHEET_NAME})
              </p>
            </div>
          </div>

          <button
            type="button"
            onClick={onClose}
            disabled={isBusy}
            className="rounded-lg p-2 text-slate-500 transition-colors hover:bg-slate-100 hover:text-slate-900 dark:text-slate-400 dark:hover:bg-slate-800 dark:hover:text-slate-100 disabled:opacity-50"
            aria-label="Close drawer"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Drawer Body */}
        <div className="flex-1 space-y-6 overflow-y-auto px-6 py-6">
          {errorMessage && (
            <div className="rounded-xl border border-red-300 bg-red-50 px-4 py-3 text-xs leading-relaxed text-red-800 dark:border-red-500/35 dark:bg-red-500/10 dark:text-red-200">
              <span className="font-semibold text-red-900 dark:text-red-300">Sync Error: </span>
              {errorMessage}
            </div>
          )}

          {/* Section 1: Drive Folder & Spreadsheet Details */}
          <section className="space-y-4 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm dark:border-slate-800 dark:bg-[#1E293B] dark:shadow-none">
            <div className="flex items-center justify-between">
              <h3 className="font-display text-sm font-semibold text-slate-900 dark:text-slate-100">
                Drive Storage Target
              </h3>
              {connectionMeta?.lastSyncedAt && (
                <span className="font-mono-tabular text-xs text-slate-500 dark:text-slate-400">
                  Synced {new Date(connectionMeta.lastSyncedAt).toLocaleTimeString()}
                </span>
              )}
            </div>

            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
              <div className="rounded-xl border border-slate-200 bg-slate-50 p-3.5 dark:border-slate-700/70 dark:bg-[#0F172A]/80">
                <div className="flex items-center gap-2 text-xs text-slate-500 dark:text-slate-400">
                  <FolderOpen className="h-3.5 w-3.5 text-amber-600 dark:text-amber-400" />
                  <span>Drive Folder</span>
                </div>
                <p className="mt-1.5 text-sm font-semibold text-slate-900 dark:text-slate-100">
                  {connectionMeta?.folderName || DRIVE_FOLDER_NAME}
                </p>
                <p className="mt-0.5 truncate font-mono-tabular text-[11px] text-slate-500 dark:text-slate-400">
                  {connectionMeta?.folderId ? `ID: ${connectionMeta.folderId}` : 'Pending connection'}
                </p>
              </div>

              <div className="rounded-xl border border-slate-200 bg-slate-50 p-3.5 dark:border-slate-700/70 dark:bg-[#0F172A]/80">
                <div className="flex items-center gap-2 text-xs text-slate-500 dark:text-slate-400">
                  <Sheet className="h-3.5 w-3.5 text-emerald-600 dark:text-emerald-400" />
                  <span>Spreadsheet</span>
                </div>
                <p className="mt-1.5 text-sm font-semibold text-slate-900 dark:text-slate-100">
                  {connectionMeta?.spreadsheetTitle || SPREADSHEET_NAME}
                </p>
                <div className="mt-0.5 flex items-center justify-between gap-2">
                  <p className="truncate font-mono-tabular text-[11px] text-slate-500 dark:text-slate-400">
                    {connectionMeta?.spreadsheetId
                      ? connectionMeta.spreadsheetId
                      : 'Auto-created or linked'}
                  </p>
                  {connectionMeta?.spreadsheetId && (
                    <button
                      type="button"
                      onClick={handleCopySheetId}
                      className="text-slate-500 hover:text-slate-900 dark:text-slate-400 dark:hover:text-slate-200 shrink-0"
                      title="Copy Spreadsheet ID"
                    >
                      {copiedSheetId ? (
                        <Check className="h-3.5 w-3.5 text-emerald-600 dark:text-emerald-400" />
                      ) : (
                        <Copy className="h-3.5 w-3.5" />
                      )}
                    </button>
                  )}
                </div>
              </div>
            </div>

            {connectionMeta?.spreadsheetUrl && (
              <a
                href={connectionMeta.spreadsheetUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex w-full items-center justify-center gap-2 rounded-xl border border-blue-200 bg-blue-50 px-4 py-2.5 text-xs font-medium text-blue-700 transition-colors hover:border-blue-300 hover:bg-blue-100 dark:border-slate-700 dark:bg-[#0F172A] dark:text-blue-300 dark:hover:border-blue-500/40 dark:hover:bg-blue-950/30 dark:hover:text-blue-200"
              >
                <ExternalLink className="h-4 w-4" />
                <span>Open in Google Sheets</span>
              </a>
            )}

            {/* Schema verification checklist */}
            <div className="border-t border-slate-200 pt-3 dark:border-slate-700/60">
              <p className="mb-2 text-xs font-medium text-slate-500 dark:text-slate-400">Schema Verification</p>
              <div className="grid grid-cols-3 gap-2 text-xs">
                {[
                  { label: 'Jobs (28 cols)', ok: connectionMeta?.schemaVerified.jobs },
                  { label: 'ShopProfile (14)', ok: connectionMeta?.schemaVerified.shopProfile },
                  { label: 'AuditLog (6)', ok: connectionMeta?.schemaVerified.auditLog },
                ].map((item) => (
                  <div
                    key={item.label}
                    className={`flex items-center gap-1.5 rounded-lg border px-2.5 py-1.5 ${
                      item.ok
                        ? 'border-emerald-300 bg-emerald-100 text-emerald-900 dark:border-emerald-500/30 dark:bg-emerald-500/10 dark:text-emerald-300'
                        : 'border-slate-200 bg-slate-50 text-slate-600 dark:border-slate-700/70 dark:bg-[#0F172A]/60 dark:text-slate-400'
                    }`}
                  >
                    <CheckCircle2
                      className={`h-3.5 w-3.5 shrink-0 ${
                        item.ok ? 'text-emerald-700 dark:text-emerald-400' : 'text-slate-400 dark:text-slate-500'
                      }`}
                    />
                    <span className="truncate">{item.label}</span>
                  </div>
                ))}
              </div>
            </div>
          </section>

          {/* Section 2: Paste Existing Sheet URL / ID */}
          <section className="space-y-3 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm dark:border-slate-800 dark:bg-[#1E293B] dark:shadow-none">
            <div className="flex items-center gap-2">
              <Link2 className="h-4 w-4 text-blue-600 dark:text-blue-400" />
              <h3 className="font-display text-sm font-semibold text-slate-900 dark:text-slate-100">
                Paste Existing Google Sheet URL or ID
              </h3>
            </div>
            <p className="text-xs leading-relaxed text-slate-500 dark:text-slate-400">
              Already have a <span className="font-medium text-slate-800 dark:text-slate-200">{SPREADSHEET_NAME}</span> created by
              your Android app? Paste its URL or ID below to bind directly without creating a new
              file.
            </p>
            <div className="flex flex-col gap-2 sm:flex-row">
              <input
                type="text"
                value={sheetUrlInput}
                onChange={(e) => {
                  setSheetUrlInput(e.target.value);
                  setSheetUrlError(null);
                }}
                placeholder="https://docs.google.com/spreadsheets/d/..."
                className="flex-1 rounded-xl border border-slate-300 bg-slate-50 px-3.5 py-2 font-mono-tabular text-xs text-slate-900 placeholder:text-slate-400 focus:border-blue-500 focus:outline-none dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-100 dark:placeholder:text-slate-500"
              />
              <button
                type="button"
                onClick={handleLinkSheet}
                disabled={isBusy || !sheetUrlInput.trim()}
                className="rounded-xl border border-slate-300 bg-slate-100 px-4 py-2 text-xs font-semibold text-slate-900 transition-colors hover:bg-slate-200 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-100 dark:hover:bg-slate-700 disabled:opacity-50 whitespace-nowrap shrink-0"
              >
                Link &amp; Sync
              </button>
            </div>
            {sheetUrlError && <p className="text-xs text-red-600 dark:text-red-400">{sheetUrlError}</p>}
          </section>
        </div>

        {/* Drawer Footer Actions */}
        <div className="flex items-center justify-between gap-3 border-t border-slate-200 bg-white px-6 py-4 dark:border-slate-800 dark:bg-[#0F172A]">
          {syncStatus === 'connected' ? (
            <button
              type="button"
              onClick={onDisconnect}
              disabled={isBusy}
              className="inline-flex items-center gap-2 rounded-xl border border-slate-300 bg-slate-100 px-4 py-2.5 text-xs font-medium text-slate-700 transition-colors hover:border-red-300 hover:bg-red-50 hover:text-red-700 dark:border-slate-700 dark:bg-slate-800/70 dark:text-slate-300 dark:hover:border-red-500/40 dark:hover:bg-red-500/10 dark:hover:text-red-300 disabled:opacity-50 whitespace-nowrap"
            >
              <LogOut className="h-4 w-4" />
              <span>Disconnect</span>
            </button>
          ) : (
            <span className="text-xs text-slate-500 dark:text-slate-400">
              Scopes: <span className="font-mono-tabular text-slate-700 dark:text-slate-300">spreadsheets, drive.file</span>
            </span>
          )}

          <button
            type="button"
            onClick={() => onConnectAndSync()}
            disabled={isBusy}
            className="inline-flex items-center gap-2 rounded-xl bg-[#2563EB] px-5 py-2.5 text-xs font-semibold text-white shadow-sm transition-colors hover:bg-blue-500 disabled:opacity-50 whitespace-nowrap"
          >
            <RefreshCw className={`h-4 w-4 ${isBusy ? 'animate-spin' : ''}`} />
            <span>
              {isBusy
                ? 'Syncing...'
                : syncStatus === 'connected'
                  ? 'Sync Now'
                  : 'Connect & Sync Now'}
            </span>
          </button>
        </div>
      </div>
    </div>
  );
}
