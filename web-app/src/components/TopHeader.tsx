import { RefObject } from 'react';
import {
  AlertCircle,
  BookOpen,
  Cloud,
  CloudOff,
  Plus,
  RefreshCw,
  Search,
  Settings,
  Smartphone,
  X,
} from 'lucide-react';
import { SyncStatus } from '../types/repairShop';

interface TopHeaderProps {
  shopName: string;
  searchQuery: string;
  searchInputRef: RefObject<HTMLInputElement | null>;
  syncStatus: SyncStatus;
  lastSyncedAt: number | null;
  justSynced: boolean;
  autoSyncEnabled: boolean;
  isKhataActive: boolean;
  pendingKhataCount: number;
  onSearchChange: (value: string) => void;
  onGoDashboard: () => void;
  onOpenKhataBook: () => void;
  onOpenDriveModal: () => void;
  onOpenSettings: () => void;
  onQuickSync: () => void;
  onNewJobClick: () => void;
}

export function TopHeader({
  shopName,
  searchQuery,
  searchInputRef,
  syncStatus,
  lastSyncedAt,
  justSynced,
  autoSyncEnabled,
  isKhataActive,
  pendingKhataCount,
  onSearchChange,
  onGoDashboard,
  onOpenKhataBook,
  onOpenDriveModal,
  onOpenSettings,
  onQuickSync,
  onNewJobClick,
}: TopHeaderProps) {
  const formattedSyncTime = lastSyncedAt
    ? new Date(lastSyncedAt).toLocaleTimeString([], {
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
      })
    : null;

  const renderSyncControl = () => {
    if (syncStatus === 'syncing') {
      return (
        <button
          type="button"
          onClick={onOpenDriveModal}
          className="inline-flex h-9 items-center gap-2 rounded-xl border border-blue-300/40 bg-blue-500/25 px-3 text-xs font-medium text-white transition-colors hover:bg-blue-500/35 focus-visible:outline-2 focus-visible:outline-white whitespace-nowrap shrink-0"
          title="Syncing with Google Sheets"
        >
          <RefreshCw className="h-3.5 w-3.5 animate-spin text-blue-200 shrink-0" />
          <span>Syncing...</span>
        </button>
      );
    }

    if (syncStatus === 'connected') {
      return (
        <div className="flex items-center gap-1.5 shrink-0">
          <button
            type="button"
            onClick={onOpenDriveModal}
            className={`inline-flex h-9 items-center gap-2 rounded-xl border px-3 text-xs font-medium transition-colors focus-visible:outline-2 focus-visible:outline-white whitespace-nowrap shrink-0 ${
              justSynced
                ? 'border-emerald-300/60 bg-emerald-500/30 text-white'
                : 'border-emerald-300/40 bg-emerald-500/20 text-emerald-50 hover:bg-emerald-500/30'
            }`}
            title={
              lastSyncedAt
                ? `Connected · Last synced at ${new Date(lastSyncedAt).toLocaleString()}`
                : 'Connected to Google Sheets'
            }
          >
            <Cloud className="h-3.5 w-3.5 text-emerald-300 shrink-0" />
            <span>
              {justSynced
                ? 'Synced'
                : autoSyncEnabled
                  ? 'Live Auto-Sync On (10s)'
                  : 'Connected'}
            </span>
            {formattedSyncTime && (
              <span className="hidden xl:inline font-mono-tabular text-[11px] text-emerald-200">
                · {formattedSyncTime}
              </span>
            )}
          </button>
          <button
            type="button"
            onClick={onQuickSync}
            aria-label="Sync Now"
            title={
              formattedSyncTime
                ? `Sync Now (Last synced ${formattedSyncTime})`
                : 'Sync Now'
            }
            className="inline-flex h-9 w-9 items-center justify-center rounded-xl border border-white/25 bg-white/10 text-white transition-colors hover:bg-white/20 focus-visible:outline-2 focus-visible:outline-white shrink-0"
          >
            <RefreshCw className="h-3.5 w-3.5" />
          </button>
        </div>
      );
    }

    if (syncStatus === 'error') {
      return (
        <button
          type="button"
          onClick={onOpenDriveModal}
          className="inline-flex h-9 items-center gap-2 rounded-xl border border-red-300/50 bg-red-500/25 px-3 text-xs font-medium text-white transition-colors hover:bg-red-500/35 focus-visible:outline-2 focus-visible:outline-white whitespace-nowrap shrink-0"
          title="Sync error — click to inspect Drive connection"
        >
          <AlertCircle className="h-3.5 w-3.5 text-red-200 shrink-0" />
          <span>Sync Error</span>
        </button>
      );
    }

    return (
      <button
        type="button"
        onClick={onOpenDriveModal}
        className="inline-flex h-9 items-center gap-2 rounded-xl border border-amber-300/45 bg-amber-500/20 px-3 text-xs font-medium text-amber-50 transition-colors hover:bg-amber-500/30 focus-visible:outline-2 focus-visible:outline-white whitespace-nowrap shrink-0"
        title={
          formattedSyncTime
            ? `Disconnected · Cached from ${formattedSyncTime}`
            : 'Connect Google Drive to enable 2-way sync'
        }
      >
        <CloudOff className="h-3.5 w-3.5 text-amber-200 shrink-0" />
        <span>Disconnected</span>
      </button>
    );
  };

  return (
    <header className="sticky top-0 z-30 border-b border-blue-950/60 bg-[#1E3A8A] text-white shadow-md">
      <div className="mx-auto flex min-h-16 max-w-[1440px] flex-wrap items-center justify-between gap-3 px-4 py-2.5 sm:px-6 lg:flex-nowrap lg:gap-5 lg:px-8">
        {/* Left: Shop Name */}
        <button
          type="button"
          onClick={onGoDashboard}
          className="flex items-center gap-3 min-w-0 shrink-0 text-left rounded-xl focus-visible:outline-2 focus-visible:outline-white"
          title="Return to Dashboard"
        >
          <div className="flex h-9 w-9 items-center justify-center rounded-xl border border-white/25 bg-white/15 text-white shadow-sm shrink-0">
            <Smartphone className="h-5 w-5" />
          </div>
          <span className="font-display text-base sm:text-lg font-bold tracking-tight text-white truncate max-w-[200px] sm:max-w-[260px] whitespace-nowrap">
            {shopName || 'Mobile Repair Shop POS'}
          </span>
        </button>

        {/* Khata Book Button placed to the left of Search */}
        <button
          type="button"
          onClick={onOpenKhataBook}
          className={`order-2 inline-flex h-9 items-center gap-2 rounded-xl border px-3 text-xs font-semibold transition-colors focus-visible:outline-2 focus-visible:outline-white whitespace-nowrap shrink-0 lg:order-2 ${
            isKhataActive
              ? 'border-white bg-white text-[#1E3A8A] shadow-sm'
              : 'border-white/25 bg-white/10 text-white hover:bg-white/20'
          }`}
          title="Open Khata Book (Customer Ledger)"
        >
          <BookOpen
            className={`h-3.5 w-3.5 shrink-0 ${
              isKhataActive ? 'text-[#1E3A8A]' : 'text-amber-300'
            }`}
          />
          <span>Khata Book</span>
          {pendingKhataCount > 0 && (
            <span
              className={`rounded-md px-1.5 py-0.5 font-mono-tabular text-[10px] font-bold ${
                isKhataActive
                  ? 'bg-red-100 text-red-800'
                  : 'bg-red-500/80 text-white'
              }`}
            >
              {pendingKhataCount}
            </span>
          )}
        </button>

        {/* Search -> Cloud Sync Status -> Settings -> + New Job */}
        <div className="order-4 w-full lg:order-3 lg:flex-1 lg:max-w-lg">
          <div className="relative flex items-center">
            <Search className="pointer-events-none absolute left-3.5 h-4 w-4 text-blue-200" />
            <input
              ref={searchInputRef}
              type="text"
              value={searchQuery}
              onChange={(e) => onSearchChange(e.target.value)}
              placeholder="Search Job #, customer, phone, device, IMEI..."
              aria-label="Search repair jobs"
              className="h-9 w-full rounded-xl border border-white/20 bg-[#172554]/80 pl-10 pr-16 text-sm text-white placeholder:text-blue-200/75 transition-colors focus:border-blue-300 focus:bg-[#172554] focus:outline-none"
            />
            {searchQuery ? (
              <button
                type="button"
                onClick={() => onSearchChange('')}
                aria-label="Clear search"
                className="absolute right-2.5 rounded-md p-1 text-blue-200 hover:bg-white/15 hover:text-white"
              >
                <X className="h-3.5 w-3.5" />
              </button>
            ) : (
              <kbd className="pointer-events-none absolute right-3 hidden sm:inline-block rounded border border-white/25 bg-white/10 px-1.5 py-0.5 font-mono-tabular text-[11px] text-blue-100">
                /
              </kbd>
            )}
          </div>
        </div>

        {/* Right Controls: Cloud Sync Status | Settings | + New Job */}
        <div className="order-3 flex items-center gap-2 lg:order-4 shrink-0">
          {renderSyncControl()}

          <button
            type="button"
            onClick={onOpenSettings}
            aria-label="Shop Settings & Audit Log"
            title="Shop Settings & Audit Log"
            className="inline-flex h-9 w-9 items-center justify-center rounded-xl border border-white/25 bg-white/10 text-white transition-colors hover:bg-white/20 focus-visible:outline-2 focus-visible:outline-white shrink-0"
          >
            <Settings className="h-4 w-4 text-white" />
          </button>

          <button
            type="button"
            onClick={onNewJobClick}
            className="inline-flex h-9 items-center gap-1.5 rounded-xl bg-white px-3.5 text-xs font-bold text-[#1E3A8A] shadow-sm transition-colors hover:bg-blue-50 focus-visible:outline-2 focus-visible:outline-white whitespace-nowrap shrink-0"
          >
            <Plus className="h-4 w-4 shrink-0" />
            <span>New Job</span>
          </button>
        </div>
      </div>
    </header>
  );
}
