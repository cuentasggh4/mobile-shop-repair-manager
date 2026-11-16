import { useEffect, useMemo, useState } from 'react';
import {
  Bell,
  Building2,
  Check,
  CheckCircle2,
  Cloud,
  Copy,
  ExternalLink,
  FolderOpen,
  History,
  Link2,
  LogOut,
  Monitor,
  Moon,
  RefreshCw,
  Save,
  Search,
  Send,
  Sheet,
  ShieldCheck,
  Sun,
} from 'lucide-react';
import { extractSpreadsheetId } from '../services/googleSheetsSync';
import { AppLanguage, SETTINGS_PREFERENCES_STRINGS } from '../services/i18n';
import { BrowserNotificationPermissionState } from '../services/notifications';
import { ThemeMode } from '../services/theme';
import {
  AuditLogEntry,
  createDefaultShopProfile,
  DRIVE_FOLDER_NAME,
  DriveConnectionMeta,
  generateNextJobNumber,
  ShopProfile,
  SPREADSHEET_NAME,
  SyncStatus,
} from '../types/repairShop';

export type SettingsTab = 'profile' | 'sync' | 'audit';

interface SettingsHubProps {
  activeTab: SettingsTab;
  shopProfile: ShopProfile | null;
  auditLogs: AuditLogEntry[];
  syncStatus: SyncStatus;
  connectionMeta: DriveConnectionMeta | null;
  errorMessage: string | null;
  isSaving: boolean;
  autoSyncEnabled: boolean;
  themeMode: ThemeMode;
  language: AppLanguage;
  notificationsEnabled: boolean;
  notificationPermission: BrowserNotificationPermissionState;
  onTabChange: (tab: SettingsTab) => void;
  onSaveProfile: (updated: ShopProfile) => Promise<void>;
  onConnectAndSync: () => Promise<void>;
  onLinkExistingSheet: (spreadsheetId: string) => Promise<void>;
  onDisconnect: () => void;
  onToggleAutoSync: (enabled: boolean) => void;
  onChangeThemeMode: (mode: ThemeMode) => void;
  onChangeLanguage: (lang: AppLanguage) => void;
  onToggleNotifications: (enabled: boolean) => void;
  onRequestNotificationPermission: () => Promise<void>;
  onSendTestNotification: () => Promise<void>;
}

export function SettingsHub({
  activeTab,
  shopProfile,
  auditLogs,
  syncStatus,
  connectionMeta,
  errorMessage,
  isSaving,
  autoSyncEnabled,
  themeMode,
  language,
  notificationsEnabled,
  notificationPermission,
  onTabChange,
  onSaveProfile,
  onConnectAndSync,
  onLinkExistingSheet,
  onDisconnect,
  onToggleAutoSync,
  onChangeThemeMode,
  onChangeLanguage,
  onToggleNotifications,
  onRequestNotificationPermission,
  onSendTestNotification,
}: SettingsHubProps) {
  const [profileDraft, setProfileDraft] = useState<ShopProfile>(
    () => shopProfile ?? createDefaultShopProfile()
  );
  const [copiedOrigin, setCopiedOrigin] = useState(false);
  const [copiedSheetId, setCopiedSheetId] = useState(false);
  const [sheetUrlInput, setSheetUrlInput] = useState('');
  const [sheetUrlError, setSheetUrlError] = useState<string | null>(null);
  const [auditSearch, setAuditSearch] = useState('');

  useEffect(() => {
    setProfileDraft(shopProfile ?? createDefaultShopProfile());
  }, [shopProfile]);

  const prefixPreview = useMemo(
    () => generateNextJobNumber(profileDraft, []),
    [profileDraft]
  );

  const filteredAuditLogs = useMemo(() => {
    const q = auditSearch.trim().toLowerCase();
    if (!q) return auditLogs;
    return auditLogs.filter((entry) =>
      [entry.isoTime, entry.action, entry.jobNumber, entry.details, entry.recordUuid]
        .join(' ')
        .toLowerCase()
        .includes(q)
    );
  }, [auditLogs, auditSearch]);

  const t = SETTINGS_PREFERENCES_STRINGS[language];
  const isSyncBusy = syncStatus === 'syncing';
  const currentOrigin = typeof window !== 'undefined' ? window.location.origin : '';

  const handleProfileSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    await onSaveProfile({
      ...profileDraft,
      updatedAt: Date.now(),
    });
  };

  const handleCopyOrigin = async () => {
    try {
      await navigator.clipboard.writeText(currentOrigin);
      setCopiedOrigin(true);
      window.setTimeout(() => setCopiedOrigin(false), 1800);
    } catch {
      // Ignore
    }
  };

  const handleCopySheetId = async () => {
    if (!connectionMeta?.spreadsheetId) return;
    try {
      await navigator.clipboard.writeText(connectionMeta.spreadsheetId);
      setCopiedSheetId(true);
      window.setTimeout(() => setCopiedSheetId(false), 1800);
    } catch {
      // Ignore
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

  const permissionBadge = useMemo(() => {
    switch (notificationPermission) {
      case 'granted':
        return {
          label: t.permissionGranted,
          classes:
            'border-emerald-200 bg-emerald-100 text-emerald-800 dark:border-emerald-500/35 dark:bg-emerald-500/15 dark:text-emerald-300',
        };
      case 'denied':
        return {
          label: t.permissionDenied,
          classes:
            'border-red-200 bg-red-100 text-red-800 dark:border-red-500/35 dark:bg-red-500/15 dark:text-red-300',
        };
      case 'unsupported':
        return {
          label: t.permissionUnsupported,
          classes:
            'border-slate-200 bg-slate-100 text-slate-700 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-300',
        };
      default:
        return {
          label: t.permissionDefault,
          classes:
            'border-amber-200 bg-amber-100 text-amber-800 dark:border-amber-500/35 dark:bg-amber-500/15 dark:text-amber-300',
        };
    }
  }, [notificationPermission, t]);

  return (
    <div className="space-y-6">
      {/* Header & 3-Section Tabs */}
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="font-display text-2xl font-bold tracking-tight text-slate-900 dark:text-slate-100">
            Settings &amp; Sync Hub
          </h1>
          <p className="mt-0.5 text-xs text-slate-500 dark:text-slate-400">
            Configure theme, browser notifications, shop policies, Google Sheets synchronization, and audit logs.
          </p>
        </div>

        <div
          className="inline-flex flex-wrap items-center gap-1 rounded-xl border border-slate-200 bg-white p-1 shadow-sm dark:border-slate-800 dark:bg-[#1E293B] dark:shadow-none"
          role="tablist"
          aria-label="Settings sections"
        >
          <button
            type="button"
            role="tab"
            aria-selected={activeTab === 'profile'}
            onClick={() => onTabChange('profile')}
            className={`inline-flex items-center gap-2 rounded-lg px-3.5 py-2 text-xs font-medium transition-colors ${
              activeTab === 'profile'
                ? 'bg-[#2563EB] text-white'
                : 'text-slate-600 hover:text-slate-900 dark:text-slate-300 dark:hover:text-white'
            }`}
          >
            <Building2 className="h-3.5 w-3.5" />
            <span>Shop Profile &amp; Preferences</span>
          </button>

          <button
            type="button"
            role="tab"
            aria-selected={activeTab === 'sync'}
            onClick={() => onTabChange('sync')}
            className={`inline-flex items-center gap-2 rounded-lg px-3.5 py-2 text-xs font-medium transition-colors ${
              activeTab === 'sync'
                ? 'bg-[#2563EB] text-white'
                : 'text-slate-600 hover:text-slate-900 dark:text-slate-300 dark:hover:text-white'
            }`}
          >
            <Cloud className="h-3.5 w-3.5" />
            <span>Google Sheets Sync</span>
          </button>

          <button
            type="button"
            role="tab"
            aria-selected={activeTab === 'audit'}
            onClick={() => onTabChange('audit')}
            className={`inline-flex items-center gap-2 rounded-lg px-3.5 py-2 text-xs font-medium transition-colors ${
              activeTab === 'audit'
                ? 'bg-[#2563EB] text-white'
                : 'text-slate-600 hover:text-slate-900 dark:text-slate-300 dark:hover:text-white'
            }`}
          >
            <History className="h-3.5 w-3.5" />
            <span>Audit Log ({auditLogs.length})</span>
          </button>
        </div>
      </div>

      {/* SECTION A: Appearance, Notifications & Shop Profile */}
      {activeTab === 'profile' && (
        <div className="space-y-6">
          <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
            {/* Appearance (Theme) Selector Card */}
            <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm space-y-5 dark:border-slate-800 dark:bg-[#1E293B] dark:shadow-none">
              <div className="flex flex-wrap items-start justify-between gap-3 border-b border-slate-200 pb-4 dark:border-slate-800">
                <div>
                  <h2 className="font-display text-base font-semibold text-slate-900 dark:text-slate-100">
                    {t.appearanceTitle}
                  </h2>
                  <p className="mt-0.5 text-xs text-slate-500 dark:text-slate-400">
                    {t.appearanceSubtitle}
                  </p>
                </div>
              </div>

              <div>
                <span className="mb-2 block text-xs font-medium text-slate-600 dark:text-slate-400">
                  {t.themeLabel}
                </span>
                <div className="grid grid-cols-3 gap-2.5" role="radiogroup" aria-label={t.themeLabel}>
                  {(
                    [
                      { id: 'system' as const, label: t.themeSystem, Icon: Monitor },
                      { id: 'light' as const, label: t.themeLight, Icon: Sun },
                      { id: 'dark' as const, label: t.themeDark, Icon: Moon },
                    ] as const
                  ).map(({ id, label, Icon }) => {
                    const active = themeMode === id;
                    return (
                      <button
                        key={id}
                        type="button"
                        role="radio"
                        aria-checked={active}
                        onClick={() => onChangeThemeMode(id)}
                        className={`flex flex-col items-center justify-center gap-1.5 rounded-xl border px-3 py-3 text-xs font-semibold transition-colors ${
                          active
                            ? 'border-blue-600 bg-[#2563EB] text-white shadow-sm'
                            : 'border-slate-200 bg-slate-50 text-slate-700 hover:border-slate-300 hover:bg-slate-100 dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-300 dark:hover:border-slate-600 dark:hover:text-white'
                        }`}
                      >
                        <Icon className="h-4 w-4" />
                        <span>{label}</span>
                      </button>
                    );
                  })}
                </div>
              </div>

              <div className="border-t border-slate-200 pt-4 dark:border-slate-800">
                <span className="mb-2 block text-xs font-medium text-slate-600 dark:text-slate-400">
                  {t.languageLabel}
                </span>
                <div className="grid grid-cols-3 gap-2">
                  {(
                    [
                      { code: 'en' as const, label: 'English' },
                      { code: 'hi' as const, label: 'हिन्दी' },
                      { code: 'gu' as const, label: 'ગુજરાતી' },
                    ] as const
                  ).map((item) => {
                    const active = language === item.code;
                    return (
                      <button
                        key={item.code}
                        type="button"
                        onClick={() => onChangeLanguage(item.code)}
                        className={`rounded-xl border px-3 py-2 text-xs font-semibold transition-colors ${
                          active
                            ? 'border-blue-600 bg-blue-50 text-blue-800 dark:border-blue-500/60 dark:bg-blue-500/20 dark:text-blue-200'
                            : 'border-slate-200 bg-slate-50 text-slate-700 hover:border-slate-300 dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-300 dark:hover:border-slate-600'
                        }`}
                      >
                        {item.label}
                      </button>
                    );
                  })}
                </div>
              </div>
            </section>

            {/* Browser Notifications Settings Card */}
            <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm space-y-5 dark:border-slate-800 dark:bg-[#1E293B] dark:shadow-none">
              <div className="flex flex-wrap items-start justify-between gap-3 border-b border-slate-200 pb-4 dark:border-slate-800">
                <div className="flex items-start gap-2.5">
                  <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-blue-100 text-blue-700 shrink-0 dark:bg-[#2563EB]/20 dark:text-blue-400">
                    <Bell className="h-4 w-4" />
                  </div>
                  <div>
                    <h2 className="font-display text-base font-semibold text-slate-900 dark:text-slate-100">
                      {t.notificationsTitle}
                    </h2>
                    <p className="mt-0.5 text-xs text-slate-500 dark:text-slate-400">
                      {t.notificationsSubtitle}
                    </p>
                  </div>
                </div>
              </div>

              {/* Enable/Disable Notifications Toggle */}
              <div className="flex items-center justify-between gap-3 rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 dark:border-slate-700/80 dark:bg-[#0F172A]/80">
                <div>
                  <p className="text-xs font-semibold text-slate-800 dark:text-slate-200">
                    {t.notificationsToggleTitle}
                  </p>
                  <p className="mt-0.5 text-[11px] text-slate-500 dark:text-slate-400">
                    {t.notificationsToggleDesc}
                  </p>
                </div>
                <button
                  type="button"
                  role="switch"
                  aria-checked={notificationsEnabled}
                  onClick={() => onToggleNotifications(!notificationsEnabled)}
                  className={`relative inline-flex h-6 w-11 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors ${
                    notificationsEnabled
                      ? 'bg-emerald-600 dark:bg-emerald-500'
                      : 'bg-slate-300 dark:bg-slate-700'
                  }`}
                >
                  <span
                    className={`pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow transition-transform ${
                      notificationsEnabled ? 'translate-x-5' : 'translate-x-0'
                    }`}
                  />
                </button>
              </div>

              {/* Current Browser Permission Status Indicator */}
              <div className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-xs dark:border-slate-700/80 dark:bg-[#0F172A]/80">
                <div className="flex items-center gap-2">
                  <ShieldCheck className="h-4 w-4 text-slate-500 dark:text-slate-400" />
                  <span className="font-medium text-slate-700 dark:text-slate-300">
                    {t.permissionStatusLabel}:
                  </span>
                </div>
                <span
                  className={`inline-flex items-center rounded-md border px-2.5 py-1 text-[11px] font-semibold ${permissionBadge.classes}`}
                >
                  {permissionBadge.label}
                </span>
              </div>

              {/* Action Buttons */}
              <div className="flex flex-wrap items-center gap-2.5">
                {notificationPermission !== 'granted' && (
                  <button
                    type="button"
                    onClick={() => void onRequestNotificationPermission()}
                    disabled={notificationPermission === 'unsupported'}
                    className="inline-flex items-center gap-2 rounded-xl bg-[#2563EB] px-4 py-2 text-xs font-semibold text-white transition-colors hover:bg-blue-500 disabled:opacity-50"
                  >
                    <Bell className="h-3.5 w-3.5" />
                    <span>{t.allowNotificationsBtn}</span>
                  </button>
                )}

                <button
                  type="button"
                  onClick={() => void onSendTestNotification()}
                  className="inline-flex items-center gap-2 rounded-xl border border-slate-300 bg-white px-4 py-2 text-xs font-semibold text-slate-700 transition-colors hover:border-blue-500/50 hover:text-slate-900 dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-200 dark:hover:border-blue-500/40 dark:hover:text-white"
                >
                  <Send className="h-3.5 w-3.5 text-blue-600 dark:text-blue-400" />
                  <span>{t.sendTestNotificationBtn}</span>
                </button>
              </div>
            </section>
          </div>

          {/* Shop Profile & Policies Form */}
          <form
            onSubmit={handleProfileSubmit}
            className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm space-y-6 dark:border-slate-800 dark:bg-[#1E293B] dark:shadow-none"
          >
            <div className="flex flex-wrap items-center justify-between gap-4 border-b border-slate-200 pb-4 dark:border-slate-800">
              <div>
                <h2 className="font-display text-base font-semibold text-slate-900 dark:text-slate-100">
                  Shop Profile &amp; Policies (ShopProfile!A2:N2)
                </h2>
                <p className="mt-0.5 text-xs text-slate-500 dark:text-slate-400">
                  Shared across Android and Desktop POS for receipts, WhatsApp messages, and job numbering.
                </p>
              </div>
              {shopProfile?.updatedAt ? (
                <span className="font-mono-tabular text-xs text-slate-500 dark:text-slate-400">
                  Last updated {new Date(shopProfile.updatedAt).toLocaleString()}
                </span>
              ) : null}
            </div>

            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
              <div>
                <label className="mb-1.5 block text-xs font-medium text-slate-600 dark:text-slate-400">
                  Shop Name <span className="text-red-500 dark:text-red-400">*</span>
                </label>
                <input
                  type="text"
                  required
                  value={profileDraft.shopName}
                  onChange={(e) =>
                    setProfileDraft({ ...profileDraft, shopName: e.target.value })
                  }
                  placeholder="Mobile Repair Shop"
                  className="w-full rounded-xl border border-slate-300 bg-slate-50 px-3.5 py-2.5 text-sm text-slate-900 placeholder:text-slate-400 focus:border-blue-500 focus:outline-none dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-100 dark:placeholder:text-slate-500"
                />
              </div>

              <div>
                <label className="mb-1.5 block text-xs font-medium text-slate-600 dark:text-slate-400">
                  Tagline
                </label>
                <input
                  type="text"
                  value={profileDraft.tagline}
                  onChange={(e) =>
                    setProfileDraft({ ...profileDraft, tagline: e.target.value })
                  }
                  placeholder="Optional shop tagline"
                  className="w-full rounded-xl border border-slate-300 bg-slate-50 px-3.5 py-2.5 text-sm text-slate-900 placeholder:text-slate-400 focus:border-blue-500 focus:outline-none dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-100 dark:placeholder:text-slate-500"
                />
              </div>

              <div>
                <label className="mb-1.5 block text-xs font-medium text-slate-600 dark:text-slate-400">
                  Contact Phone
                </label>
                <input
                  type="tel"
                  value={profileDraft.contactPhone}
                  onChange={(e) =>
                    setProfileDraft({ ...profileDraft, contactPhone: e.target.value })
                  }
                  placeholder="Shop phone number"
                  className="w-full rounded-xl border border-slate-300 bg-slate-50 px-3.5 py-2.5 font-mono-tabular text-sm text-slate-900 placeholder:text-slate-400 focus:border-blue-500 focus:outline-none dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-100 dark:placeholder:text-slate-500"
                />
              </div>

              <div>
                <label className="mb-1.5 block text-xs font-medium text-slate-600 dark:text-slate-400">
                  WhatsApp Number
                </label>
                <input
                  type="tel"
                  value={profileDraft.whatsappNumber}
                  onChange={(e) =>
                    setProfileDraft({ ...profileDraft, whatsappNumber: e.target.value })
                  }
                  placeholder="Shop WhatsApp number"
                  className="w-full rounded-xl border border-slate-300 bg-slate-50 px-3.5 py-2.5 font-mono-tabular text-sm text-slate-900 placeholder:text-slate-400 focus:border-blue-500 focus:outline-none dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-100 dark:placeholder:text-slate-500"
                />
              </div>

              <div>
                <label className="mb-1.5 block text-xs font-medium text-slate-600 dark:text-slate-400">
                  Email
                </label>
                <input
                  type="email"
                  value={profileDraft.email}
                  onChange={(e) =>
                    setProfileDraft({ ...profileDraft, email: e.target.value })
                  }
                  placeholder="Shop email address"
                  className="w-full rounded-xl border border-slate-300 bg-slate-50 px-3.5 py-2.5 text-sm text-slate-900 placeholder:text-slate-400 focus:border-blue-500 focus:outline-none dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-100 dark:placeholder:text-slate-500"
                />
              </div>

              <div>
                <label className="mb-1.5 block text-xs font-medium text-slate-600 dark:text-slate-400">
                  Shop Address
                </label>
                <input
                  type="text"
                  value={profileDraft.address}
                  onChange={(e) =>
                    setProfileDraft({ ...profileDraft, address: e.target.value })
                  }
                  placeholder="Shop address"
                  className="w-full rounded-xl border border-slate-300 bg-slate-50 px-3.5 py-2.5 text-sm text-slate-900 placeholder:text-slate-400 focus:border-blue-500 focus:outline-none dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-100 dark:placeholder:text-slate-500"
                />
              </div>

              <div>
                <label className="mb-1.5 block text-xs font-medium text-slate-600 dark:text-slate-400">
                  Currency Symbol
                </label>
                <input
                  type="text"
                  value={profileDraft.currencySymbol}
                  onChange={(e) =>
                    setProfileDraft({ ...profileDraft, currencySymbol: e.target.value })
                  }
                  className="w-full rounded-xl border border-slate-300 bg-slate-50 px-3.5 py-2.5 font-mono-tabular text-sm text-slate-900 focus:border-blue-500 focus:outline-none dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-100"
                />
              </div>

              <div>
                <label className="mb-1.5 block text-xs font-medium text-slate-600 dark:text-slate-400">
                  Warranty Days
                </label>
                <input
                  type="number"
                  min={0}
                  value={profileDraft.warrantyDays}
                  onChange={(e) =>
                    setProfileDraft({
                      ...profileDraft,
                      warrantyDays: Math.max(0, Number(e.target.value) || 0),
                    })
                  }
                  className="w-full rounded-xl border border-slate-300 bg-slate-50 px-3.5 py-2.5 font-mono-tabular text-sm text-slate-900 focus:border-blue-500 focus:outline-none dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-100"
                />
              </div>

              <div>
                <label className="mb-1.5 block text-xs font-medium text-slate-600 dark:text-slate-400">
                  Job Number Prefix
                </label>
                <input
                  type="text"
                  value={profileDraft.jobNumberPrefix}
                  onChange={(e) =>
                    setProfileDraft({
                      ...profileDraft,
                      jobNumberPrefix: e.target.value.toUpperCase(),
                    })
                  }
                  placeholder="JOB"
                  className="w-full rounded-xl border border-slate-300 bg-slate-50 px-3.5 py-2.5 font-mono-tabular text-sm text-slate-900 focus:border-blue-500 focus:outline-none dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-100"
                />
                <p className="mt-1.5 text-[11px] text-slate-500 dark:text-slate-400">
                  Preview:{' '}
                  <span className="font-mono-tabular font-semibold text-blue-700 dark:text-blue-300">
                    {prefixPreview.jobNumber}
                  </span>{' '}
                  · Sequence auto-resets to 001 on the 1st of each month.
                </p>
              </div>
            </div>

            <div>
              <label className="mb-1.5 block text-xs font-medium text-slate-600 dark:text-slate-400">
                Terms &amp; Conditions (Printed on Invoices &amp; Job Cards)
              </label>
              <textarea
                rows={3}
                value={profileDraft.termsAndConditions}
                onChange={(e) =>
                  setProfileDraft({
                    ...profileDraft,
                    termsAndConditions: e.target.value,
                  })
                }
                placeholder="Enter shop warranty and repair terms for printed receipts (optional)"
                className="w-full rounded-xl border border-slate-300 bg-slate-50 px-3.5 py-2.5 text-sm text-slate-900 placeholder:text-slate-400 focus:border-blue-500 focus:outline-none dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-100 dark:placeholder:text-slate-500"
              />
            </div>

            <div className="flex items-center justify-end gap-3 border-t border-slate-200 pt-4 dark:border-slate-800">
              <button
                type="submit"
                disabled={isSaving}
                className="inline-flex items-center gap-2 rounded-xl bg-[#2563EB] px-5 py-2.5 text-xs font-semibold text-white transition-colors hover:bg-blue-500 disabled:opacity-50"
              >
                <Save className="h-4 w-4" />
                <span>
                  {isSaving
                    ? 'Saving...'
                    : syncStatus === 'connected'
                      ? 'Save & Sync ShopProfile'
                      : 'Save ShopProfile Locally'}
                </span>
              </button>
            </div>
          </form>
        </div>
      )}

      {/* SECTION B: Google Sheets Sync Manager */}
      {activeTab === 'sync' && (
        <div className="space-y-5">
          {errorMessage && (
            <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-xs text-red-800 dark:border-red-500/35 dark:bg-red-500/10 dark:text-red-200">
              <strong className="text-red-900 dark:text-red-300">Sync Error: </strong>
              {errorMessage}
            </div>
          )}

          <div className="grid grid-cols-1 gap-5 lg:grid-cols-2">
            {/* Folder & Spreadsheet Card */}
            <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm space-y-4 dark:border-slate-800 dark:bg-[#1E293B] dark:shadow-none">
              <div className="flex items-center justify-between">
                <h2 className="font-display text-base font-semibold text-slate-900 dark:text-slate-100">
                  Drive Folder &amp; Spreadsheet
                </h2>
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
                    {connectionMeta?.folderId || 'Not connected'}
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
                      {connectionMeta?.spreadsheetId || 'Pending connection'}
                    </p>
                    {connectionMeta?.spreadsheetId && (
                      <button
                        type="button"
                        onClick={handleCopySheetId}
                        className="text-slate-500 hover:text-slate-900 shrink-0 dark:text-slate-400 dark:hover:text-slate-200"
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

              <div className="grid grid-cols-3 gap-2 text-xs">
                {[
                  { label: 'Jobs (28)', ok: connectionMeta?.schemaVerified.jobs },
                  { label: 'ShopProfile (14)', ok: connectionMeta?.schemaVerified.shopProfile },
                  { label: 'AuditLog (6)', ok: connectionMeta?.schemaVerified.auditLog },
                ].map((item) => (
                  <div
                    key={item.label}
                    className={`flex items-center gap-1.5 rounded-lg border px-2.5 py-2 ${
                      item.ok
                        ? 'border-emerald-200 bg-emerald-100 text-emerald-800 dark:border-emerald-500/30 dark:bg-emerald-500/10 dark:text-emerald-300'
                        : 'border-slate-200 bg-slate-50 text-slate-500 dark:border-slate-700/70 dark:bg-[#0F172A]/60 dark:text-slate-400'
                    }`}
                  >
                    <CheckCircle2
                      className={`h-3.5 w-3.5 shrink-0 ${
                        item.ok
                          ? 'text-emerald-600 dark:text-emerald-400'
                          : 'text-slate-400 dark:text-slate-500'
                      }`}
                    />
                    <span className="truncate">{item.label}</span>
                  </div>
                ))}
              </div>

              <div className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-slate-200 bg-slate-50 px-3.5 py-2.5 text-xs dark:border-slate-700/70 dark:bg-[#0F172A]/80">
                <div>
                  <p className="font-medium text-slate-800 dark:text-slate-200">
                    Real-Time Auto-Sync (Every 10s + Tab Focus)
                  </p>
                  <p className="text-[11px] text-slate-500 dark:text-slate-400">
                    Automatically pulls Mobile app updates from Google Sheets and pushes Laptop edits immediately.
                  </p>
                </div>
                <button
                  type="button"
                  role="switch"
                  aria-checked={autoSyncEnabled}
                  onClick={() => onToggleAutoSync(!autoSyncEnabled)}
                  className={`relative inline-flex h-6 w-11 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors ${
                    autoSyncEnabled
                      ? 'bg-emerald-600 dark:bg-emerald-500'
                      : 'bg-slate-300 dark:bg-slate-700'
                  }`}
                >
                  <span
                    className={`pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow transition-transform ${
                      autoSyncEnabled ? 'translate-x-5' : 'translate-x-0'
                    }`}
                  />
                </button>
              </div>

              <div className="flex flex-wrap items-center gap-2.5 pt-1">
                <button
                  type="button"
                  onClick={() => onConnectAndSync()}
                  disabled={isSyncBusy}
                  className="inline-flex items-center gap-2 rounded-xl bg-[#2563EB] px-4 py-2 text-xs font-semibold text-white hover:bg-blue-500 disabled:opacity-50"
                >
                  <RefreshCw className={`h-3.5 w-3.5 ${isSyncBusy ? 'animate-spin' : ''}`} />
                  <span>{syncStatus === 'connected' ? 'Sync Now' : 'Connect & Sync Now'}</span>
                </button>

                {connectionMeta?.spreadsheetUrl && (
                  <a
                    href={connectionMeta.spreadsheetUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="inline-flex items-center gap-1.5 rounded-xl border border-slate-300 bg-white px-3.5 py-2 text-xs font-medium text-blue-700 hover:bg-slate-100 dark:border-slate-700 dark:bg-[#0F172A] dark:text-blue-300 dark:hover:text-blue-200"
                  >
                    <ExternalLink className="h-3.5 w-3.5" />
                    <span>Open in Google Sheets</span>
                  </a>
                )}

                {syncStatus === 'connected' && (
                  <button
                    type="button"
                    onClick={onDisconnect}
                    className="inline-flex items-center gap-1.5 rounded-xl border border-red-200 bg-red-50 px-3.5 py-2 text-xs font-medium text-red-700 hover:bg-red-100 dark:border-slate-700 dark:bg-[#0F172A] dark:text-red-300 dark:hover:border-red-500/40"
                  >
                    <LogOut className="h-3.5 w-3.5" />
                    <span>Disconnect</span>
                  </button>
                )}
              </div>
            </section>

            {/* Link Existing Sheet & Origin Reference Card */}
            <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm space-y-5 dark:border-slate-800 dark:bg-[#1E293B] dark:shadow-none">
              <div className="space-y-2.5">
                <div className="flex items-center gap-2">
                  <Link2 className="h-4 w-4 text-blue-600 dark:text-blue-400" />
                  <h3 className="font-display text-sm font-semibold text-slate-900 dark:text-slate-100">
                    Link Existing Google Sheet URL or ID
                  </h3>
                </div>
                <p className="text-xs leading-relaxed text-slate-500 dark:text-slate-400">
                  Already have a <span className="font-medium text-slate-700 dark:text-slate-200">{SPREADSHEET_NAME}</span> created by
                  your Android app? Paste its URL or ID below to bind directly.
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
                    disabled={isSyncBusy || !sheetUrlInput.trim()}
                    className="rounded-xl border border-slate-300 bg-white px-4 py-2 text-xs font-semibold text-slate-800 hover:bg-slate-100 disabled:opacity-50 whitespace-nowrap dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-100 dark:hover:bg-slate-800"
                  >
                    Link &amp; Sync
                  </button>
                </div>
                {sheetUrlError && <p className="text-xs text-red-600 dark:text-red-400">{sheetUrlError}</p>}
              </div>

              <div className="border-t border-slate-200 pt-4 space-y-2 text-xs text-slate-500 dark:border-slate-800 dark:text-slate-400">
                <p className="font-medium text-slate-700 dark:text-slate-300">
                  Current App Origin (for Google Cloud Authorized JavaScript origins)
                </p>
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <code className="truncate font-mono-tabular text-slate-700 dark:text-slate-300">{currentOrigin}</code>
                  <button
                    type="button"
                    onClick={handleCopyOrigin}
                    className="inline-flex items-center gap-1 rounded-lg border border-slate-300 bg-white px-2.5 py-1 text-xs text-slate-700 hover:bg-slate-100 dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-300 dark:hover:text-white"
                  >
                    {copiedOrigin ? (
                      <Check className="h-3 w-3 text-emerald-600 dark:text-emerald-400" />
                    ) : (
                      <Copy className="h-3 w-3" />
                    )}
                    <span>{copiedOrigin ? 'Copied' : 'Copy Origin'}</span>
                  </button>
                </div>
              </div>
            </section>
          </div>
        </div>
      )}

      {/* SECTION C: Audit Log Viewer */}
      {activeTab === 'audit' && (
        <section className="rounded-2xl border border-slate-200 bg-white shadow-sm overflow-hidden dark:border-slate-800 dark:bg-[#1E293B] dark:shadow-none">
          <div className="flex flex-wrap items-center justify-between gap-4 border-b border-slate-200 px-6 py-4 dark:border-slate-800">
            <div>
              <h2 className="font-display text-base font-semibold text-slate-900 dark:text-slate-100">
                Audit Log Viewer (AuditLog!A:F)
              </h2>
              <p className="text-xs text-slate-500 dark:text-slate-400">
                Read-only chronological trail of all Mobile &amp; Desktop writes ({filteredAuditLogs.length} entries)
              </p>
            </div>

            <div className="flex items-center gap-2.5 w-full sm:w-auto">
              <div className="relative flex-1 sm:w-72">
                <Search className="pointer-events-none absolute left-3 top-2.5 h-3.5 w-3.5 text-slate-400" />
                <input
                  type="text"
                  value={auditSearch}
                  onChange={(e) => setAuditSearch(e.target.value)}
                  placeholder="Filter by action, Job #, UUID, details..."
                  className="w-full rounded-xl border border-slate-300 bg-slate-50 pl-9 pr-3 py-2 text-xs text-slate-900 placeholder:text-slate-400 focus:border-blue-500 focus:outline-none dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-100 dark:placeholder:text-slate-500"
                />
              </div>
              <button
                type="button"
                onClick={() => onConnectAndSync()}
                disabled={isSyncBusy}
                className="inline-flex items-center gap-1.5 rounded-xl border border-slate-300 bg-white px-3 py-2 text-xs font-medium text-slate-700 hover:border-blue-500/50 disabled:opacity-50 whitespace-nowrap dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-200 dark:hover:border-blue-500/40"
              >
                <RefreshCw className={`h-3.5 w-3.5 ${isSyncBusy ? 'animate-spin' : ''}`} />
                <span>Refresh</span>
              </button>
            </div>
          </div>

          {filteredAuditLogs.length === 0 ? (
            <div className="px-6 py-12 text-center text-xs text-slate-500 dark:text-slate-400">
              {auditLogs.length === 0
                ? 'No audit log entries loaded yet. Connect Google Drive & Sync to fetch AuditLog!A:F.'
                : `No audit entries match "${auditSearch}".`}
            </div>
          ) : (
            <div className="overflow-x-auto max-h-[540px]">
              <table className="w-full text-left border-collapse">
                <thead className="sticky top-0 bg-slate-50 text-xs font-medium text-slate-500 border-b border-slate-200 dark:bg-[#0F172A] dark:text-slate-400 dark:border-slate-800">
                  <tr>
                    <th className="px-5 py-3">IsoTime</th>
                    <th className="px-5 py-3">Action</th>
                    <th className="px-5 py-3">Job #</th>
                    <th className="px-5 py-3">Details</th>
                    <th className="px-5 py-3">RecordUuid</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-200 text-xs dark:divide-slate-800/80">
                  {filteredAuditLogs.map((entry, idx) => (
                    <tr
                      key={`${entry.timestamp}-${entry.recordUuid}-${idx}`}
                      className="hover:bg-slate-50 dark:hover:bg-slate-800/40"
                    >
                      <td className="px-5 py-3 font-mono-tabular text-slate-500 whitespace-nowrap dark:text-slate-400">
                        {entry.isoTime
                          ? new Date(entry.isoTime).toLocaleString()
                          : entry.timestamp
                            ? new Date(entry.timestamp).toLocaleString()
                            : '—'}
                      </td>
                      <td className="px-5 py-3 whitespace-nowrap">
                        <span
                          className={`inline-block rounded-md border px-2 py-0.5 font-mono-tabular text-[11px] font-semibold ${
                            entry.action.includes('DELETE')
                              ? 'border-red-200 bg-red-100 text-red-800 dark:border-red-500/35 dark:bg-red-500/15 dark:text-red-300'
                              : entry.action.includes('CREATE')
                                ? 'border-emerald-200 bg-emerald-100 text-emerald-800 dark:border-emerald-500/35 dark:bg-emerald-500/15 dark:text-emerald-300'
                                : 'border-blue-200 bg-blue-100 text-blue-800 dark:border-blue-500/35 dark:bg-blue-500/15 dark:text-blue-300'
                          }`}
                        >
                          {entry.action}
                        </span>
                      </td>
                      <td className="px-5 py-3 font-mono-tabular font-semibold text-slate-800 whitespace-nowrap dark:text-slate-200">
                        {entry.jobNumber || '—'}
                      </td>
                      <td className="px-5 py-3 text-slate-700 max-w-md dark:text-slate-300">
                        {entry.details || '—'}
                      </td>
                      <td className="px-5 py-3 font-mono-tabular text-[11px] text-slate-500 whitespace-nowrap dark:text-slate-400">
                        {entry.recordUuid || '—'}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
      )}
    </div>
  );
}
