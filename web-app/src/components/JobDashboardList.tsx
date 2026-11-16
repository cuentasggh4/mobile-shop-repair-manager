import { useState } from 'react';
import {
  Check,
  Copy,
  Database,
  LayoutGrid,
  MessageCircle,
  RotateCcw,
  Smartphone,
  Table2,
} from 'lucide-react';
import {
  computeJobFinancials,
  JobStage,
  RepairJob,
} from '../types/repairShop';
import { StageBadge } from './StageBadge';

export type StageFilterValue = 'ALL' | JobStage;
export type JobViewMode = 'table' | 'cards';

interface JobDashboardListProps {
  allActiveJobs: RepairJob[];
  filteredJobs: RepairJob[];
  stageFilter: StageFilterValue;
  searchQuery: string;
  viewMode: JobViewMode;
  currencySymbol: string;
  isConnected: boolean;
  onStageFilterChange: (stage: StageFilterValue) => void;
  onViewModeChange: (mode: JobViewMode) => void;
  onClearFilters: () => void;
  onSelectJob: (job: RepairJob) => void;
  onOpenDriveConnect: () => void;
  onOpenWhatsApp: (job: RepairJob) => void;
}

const FILTER_OPTIONS: Array<{ value: StageFilterValue; label: string }> = [
  { value: 'ALL', label: 'All' },
  { value: JobStage.DIAGNOSTIC, label: 'Diagnostics' },
  { value: JobStage.WAITING_FOR_PARTS, label: 'Waiting Parts' },
  { value: JobStage.IN_PROGRESS, label: 'In Progress' },
  { value: JobStage.READY_FOR_PICKUP, label: 'Ready for Pickup' },
  { value: JobStage.DELIVERED, label: 'Delivered' },
  { value: JobStage.CANCELLED, label: 'Cancelled' },
];

export function JobDashboardList({
  allActiveJobs,
  filteredJobs,
  stageFilter,
  searchQuery,
  viewMode,
  currencySymbol,
  isConnected,
  onStageFilterChange,
  onViewModeChange,
  onClearFilters,
  onSelectJob,
  onOpenDriveConnect,
  onOpenWhatsApp,
}: JobDashboardListProps) {
  const [copiedUuid, setCopiedUuid] = useState<string | null>(null);

  const hasActiveFilters = stageFilter !== 'ALL' || searchQuery.trim().length > 0;

  const stageCounts: Record<StageFilterValue, number> = {
    ALL: allActiveJobs.length,
    [JobStage.DIAGNOSTIC]: 0,
    [JobStage.WAITING_FOR_PARTS]: 0,
    [JobStage.IN_PROGRESS]: 0,
    [JobStage.READY_FOR_PICKUP]: 0,
    [JobStage.DELIVERED]: 0,
    [JobStage.CANCELLED]: 0,
  };

  for (const job of allActiveJobs) {
    stageCounts[job.stage] = (stageCounts[job.stage] || 0) + 1;
  }

  const handleCopyPhone = async (e: React.MouseEvent, job: RepairJob) => {
    e.stopPropagation();
    if (!job.customerPhone) return;
    try {
      await navigator.clipboard.writeText(job.customerPhone);
      setCopiedUuid(job.recordUuid);
      window.setTimeout(() => {
        setCopiedUuid((prev) => (prev === job.recordUuid ? null : prev));
      }, 1500);
    } catch {
      // Clipboard unavailable
    }
  };

  const renderIssueTags = (csv: string) => {
    const tags = csv
      .split(',')
      .map((t) => t.trim())
      .filter(Boolean)
      .slice(0, 3);
    if (tags.length === 0) return null;
    return (
      <div className="mt-1 flex flex-wrap items-center gap-1.5">
        {tags.map((tag) => (
          <span
            key={tag}
            className="rounded border border-slate-200 bg-slate-100 px-1.5 py-0.5 text-[11px] font-medium text-slate-700 dark:border-slate-700/80 dark:bg-[#0F172A] dark:text-slate-300"
          >
            {tag}
          </span>
        ))}
      </div>
    );
  };

  const renderFinancialBadge = (job: RepairJob) => {
    const fin = computeJobFinancials(job);
    if (job.stage === JobStage.CANCELLED) {
      return (
        <span className="font-mono-tabular text-xs font-medium text-slate-500 dark:text-slate-400">
          Cancelled
        </span>
      );
    }
    if (fin.isFullyPaid) {
      return (
        <span className="inline-flex items-center rounded-md border border-emerald-200 bg-emerald-50 px-2 py-0.5 font-mono-tabular text-[11px] font-semibold text-emerald-700 dark:border-emerald-500/35 dark:bg-emerald-500/15 dark:text-emerald-300">
          PAID
        </span>
      );
    }
    if (fin.balanceDue > 0) {
      return (
        <span className="font-mono-tabular text-xs font-semibold text-amber-700 dark:text-amber-300">
          Due {currencySymbol}
          {fin.balanceDue.toLocaleString()}
        </span>
      );
    }
    return (
      <span className="font-mono-tabular text-xs text-slate-500 dark:text-slate-400">
        {currencySymbol}0 due
      </span>
    );
  };

  return (
    <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm dark:border-slate-800 dark:bg-[#1E293B] dark:shadow-none">
      {/* Toolbar: Stage Filter Chips + View Toggle */}
      <div className="flex flex-col gap-4 border-b border-slate-200 px-5 py-4 dark:border-slate-800">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-2 overflow-x-auto pb-1 sm:pb-0">
            {FILTER_OPTIONS.map((opt) => {
              const active = stageFilter === opt.value;
              const count = stageCounts[opt.value] ?? 0;
              return (
                <button
                  key={opt.value}
                  type="button"
                  onClick={() => onStageFilterChange(opt.value)}
                  className={`inline-flex items-center gap-1.5 rounded-xl border px-3 py-1.5 text-xs font-medium transition-colors whitespace-nowrap shrink-0 ${
                    active
                      ? 'border-blue-600 bg-[#2563EB] text-white shadow-sm dark:border-blue-500/60'
                      : 'border-slate-200 bg-slate-50 text-slate-700 hover:border-slate-300 hover:text-slate-900 dark:border-slate-700/80 dark:bg-[#0F172A]/70 dark:text-slate-300 dark:hover:border-slate-600 dark:hover:text-white'
                  }`}
                >
                  <span>{opt.label}</span>
                  <span
                    className={`font-mono-tabular text-[11px] ${
                      active ? 'text-blue-100' : 'text-slate-500 dark:text-slate-400'
                    }`}
                  >
                    {count}
                  </span>
                </button>
              );
            })}
          </div>

          {/* View Mode Switcher */}
          <div
            className="inline-flex items-center rounded-xl border border-slate-200 bg-slate-100 p-1 shrink-0 dark:border-slate-700/80 dark:bg-[#0F172A]"
            role="group"
            aria-label="Toggle job list view"
          >
            <button
              type="button"
              onClick={() => onViewModeChange('table')}
              className={`inline-flex items-center gap-1.5 rounded-lg px-2.5 py-1.5 text-xs font-medium transition-colors whitespace-nowrap ${
                viewMode === 'table'
                  ? 'bg-white text-slate-900 shadow-sm dark:bg-[#1E293B] dark:text-white'
                  : 'text-slate-500 hover:text-slate-900 dark:text-slate-400 dark:hover:text-slate-200'
              }`}
            >
              <Table2 className="h-3.5 w-3.5" />
              <span>Table</span>
            </button>
            <button
              type="button"
              onClick={() => onViewModeChange('cards')}
              className={`inline-flex items-center gap-1.5 rounded-lg px-2.5 py-1.5 text-xs font-medium transition-colors whitespace-nowrap ${
                viewMode === 'cards'
                  ? 'bg-white text-slate-900 shadow-sm dark:bg-[#1E293B] dark:text-white'
                  : 'text-slate-500 hover:text-slate-900 dark:text-slate-400 dark:hover:text-slate-200'
              }`}
            >
              <LayoutGrid className="h-3.5 w-3.5" />
              <span>Cards</span>
            </button>
          </div>
        </div>

        {/* Matching Count & Clear Filters */}
        <div className="flex flex-wrap items-center justify-between gap-3 text-xs">
          <div className="flex items-center gap-2 text-slate-500 dark:text-slate-400">
            <span className="font-mono-tabular">
              Showing <strong className="text-slate-900 dark:text-slate-200">{filteredJobs.length}</strong> of{' '}
              <strong className="text-slate-900 dark:text-slate-200">{allActiveJobs.length}</strong> repair{' '}
              {allActiveJobs.length === 1 ? 'job' : 'jobs'}
            </span>
            {searchQuery.trim() && (
              <>
                <span aria-hidden="true">·</span>
                <span>
                  Query:{' '}
                  <span className="font-medium text-blue-600 dark:text-blue-300">
                    &ldquo;{searchQuery.trim()}&rdquo;
                  </span>
                </span>
              </>
            )}
          </div>

          {hasActiveFilters && (
            <button
              type="button"
              onClick={onClearFilters}
              className="inline-flex items-center gap-1.5 rounded-lg border border-slate-200 bg-slate-50 px-2.5 py-1 text-xs font-medium text-slate-700 transition-colors hover:border-slate-300 hover:text-slate-900 dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-300 dark:hover:border-slate-600 dark:hover:text-white"
            >
              <RotateCcw className="h-3 w-3" />
              <span>Clear filters</span>
            </button>
          )}
        </div>
      </div>

      {/* Empty States */}
      {allActiveJobs.length === 0 ? (
        <div className="px-6 py-14 text-center">
          <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-slate-100 text-slate-500 dark:bg-[#0F172A] dark:text-slate-400">
            <Database className="h-6 w-6" />
          </div>
          <h3 className="mt-4 font-display text-base font-semibold text-slate-900 dark:text-slate-100">
            {isConnected
              ? 'No Active Repair Jobs in Google Sheet'
              : 'No Repair Jobs Cached Locally'}
          </h3>
          <p className="mx-auto mt-1.5 max-w-md text-xs leading-relaxed text-slate-500 dark:text-slate-400">
            {isConnected
              ? 'Your Google Sheet is connected. Click "+ New Job" in the header to log a ticket or sync jobs created from the Android app.'
              : 'Connect your Google Drive account to pull your existing MobileRepairShop_DB repair tickets.'}
          </p>
          {!isConnected && (
            <button
              type="button"
              onClick={onOpenDriveConnect}
              className="mt-5 inline-flex items-center gap-2 rounded-xl bg-[#2563EB] px-4 py-2 text-xs font-semibold text-white hover:bg-blue-500"
            >
              Connect Google Drive
            </button>
          )}
        </div>
      ) : filteredJobs.length === 0 ? (
        <div className="px-6 py-14 text-center">
          <p className="text-sm font-medium text-slate-800 dark:text-slate-200">
            No repair jobs match your active search or stage filter
          </p>
          <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">
            Try clearing filters or searching by partial phone number, IMEI, or Job #.
          </p>
          <button
            type="button"
            onClick={onClearFilters}
            className="mt-4 inline-flex items-center gap-1.5 rounded-xl border border-slate-200 bg-slate-50 px-4 py-2 text-xs font-medium text-slate-700 hover:bg-slate-100 dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-200 dark:hover:bg-slate-800"
          >
            <RotateCcw className="h-3.5 w-3.5" />
            <span>Reset Filters</span>
          </button>
        </div>
      ) : viewMode === 'table' ? (
        /* Compact Table View */
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="border-b border-slate-200 bg-slate-50 text-xs font-semibold text-slate-500 dark:border-slate-800 dark:bg-[#0F172A]/60 dark:font-medium dark:text-slate-400">
                <th className="px-5 py-3">Job # &amp; Date</th>
                <th className="px-5 py-3">Status</th>
                <th className="px-5 py-3">Customer</th>
                <th className="px-5 py-3">Device &amp; Fault</th>
                <th className="px-5 py-3 text-right">Financials</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200 text-sm dark:divide-slate-800/80">
              {filteredJobs.map((job) => {
                const fin = computeJobFinancials(job);
                const deviceLabel = [
                  job.deviceBrand,
                  job.deviceModel,
                  job.deviceColor ? `(${job.deviceColor})` : '',
                ]
                  .filter(Boolean)
                  .join(' ');

                return (
                  <tr
                    key={job.recordUuid}
                    onClick={() => onSelectJob(job)}
                    className="cursor-pointer transition-colors hover:bg-slate-50 dark:hover:bg-slate-800/50"
                  >
                    <td className="px-5 py-3.5 align-top whitespace-nowrap">
                      <span className="inline-block rounded-md border border-blue-200 bg-blue-50 px-2 py-0.5 font-mono-tabular text-xs font-semibold text-blue-700 dark:border-blue-500/30 dark:bg-blue-500/10 dark:text-blue-300">
                        {job.jobNumber}
                      </span>
                      <div className="mt-1.5 font-mono-tabular text-[11px] text-slate-500 dark:text-slate-400">
                        {new Date(job.createdAt).toLocaleDateString()}
                      </div>
                    </td>

                    <td className="px-5 py-3.5 align-top whitespace-nowrap">
                      <StageBadge stage={job.stage} />
                    </td>

                    <td className="px-5 py-3.5 align-top">
                      <div className="font-medium text-slate-900 dark:text-slate-100">
                        {job.customerName || 'Walk-in Customer'}
                      </div>
                      <div className="mt-1 flex items-center gap-1.5">
                        <span className="font-mono-tabular text-xs text-slate-500 dark:text-slate-400">
                          {job.customerPhone || 'No phone'}
                        </span>
                        {job.customerPhone && (
                          <button
                            type="button"
                            onClick={(e) => handleCopyPhone(e, job)}
                            title="Copy phone number"
                            aria-label={`Copy phone ${job.customerPhone}`}
                            className="rounded p-1 text-slate-400 hover:bg-slate-200 hover:text-slate-700 dark:hover:bg-slate-700 dark:hover:text-slate-100"
                          >
                            {copiedUuid === job.recordUuid ? (
                              <Check className="h-3 w-3 text-emerald-600 dark:text-emerald-400" />
                            ) : (
                              <Copy className="h-3 w-3" />
                            )}
                          </button>
                        )}
                        <button
                          type="button"
                          onClick={(e) => {
                            e.stopPropagation();
                            onOpenWhatsApp(job);
                          }}
                          title="Send WhatsApp update"
                          aria-label={`WhatsApp ${job.customerName || job.jobNumber}`}
                          className="rounded p-1 text-emerald-600 hover:bg-emerald-50 hover:text-emerald-700 dark:text-emerald-400/85 dark:hover:bg-emerald-500/15 dark:hover:text-emerald-300"
                        >
                          <MessageCircle className="h-3.5 w-3.5" />
                        </button>
                      </div>
                    </td>

                    <td className="px-5 py-3.5 align-top max-w-sm">
                      <div className="font-medium text-slate-900 dark:text-slate-200 truncate">
                        {deviceLabel || 'Unspecified Device'}
                      </div>
                      <p className="mt-0.5 text-xs text-slate-500 dark:text-slate-400 line-clamp-1">
                        {job.issueDescription || 'No issue description'}
                      </p>
                      {renderIssueTags(job.issueTagsCsv)}
                    </td>

                    <td className="px-5 py-3.5 align-top text-right whitespace-nowrap">
                      <div className="font-mono-tabular text-xs text-slate-800 dark:text-slate-200">
                        Total: {currencySymbol}
                        {fin.totalCost.toLocaleString()}
                      </div>
                      <div className="mt-0.5 font-mono-tabular text-[11px] text-slate-500 dark:text-slate-400">
                        Adv/Paid: {currencySymbol}
                        {fin.advancePaid.toLocaleString()}
                      </div>
                      <div className="mt-1">{renderFinancialBadge(job)}</div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      ) : (
        /* Card Grid View */
        <div className="grid grid-cols-1 gap-4 p-5 md:grid-cols-2 xl:grid-cols-3">
          {filteredJobs.map((job) => {
            const fin = computeJobFinancials(job);
            const deviceLabel = [
              job.deviceBrand,
              job.deviceModel,
              job.deviceColor ? `(${job.deviceColor})` : '',
            ]
              .filter(Boolean)
              .join(' ');

            return (
              <article
                key={job.recordUuid}
                onClick={() => onSelectJob(job)}
                className="flex cursor-pointer flex-col justify-between rounded-2xl border border-slate-200 bg-slate-50/70 p-4 transition-colors hover:border-blue-400 hover:bg-white dark:border-slate-700/70 dark:bg-[#0F172A]/80 dark:hover:border-blue-500/50 dark:hover:bg-[#0F172A]"
              >
                <div>
                  {/* Card Top: JobNumber + Date + Stage */}
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <span className="inline-block rounded-md border border-blue-200 bg-blue-50 px-2 py-0.5 font-mono-tabular text-xs font-semibold text-blue-700 dark:border-blue-500/30 dark:bg-blue-500/10 dark:text-blue-300">
                        {job.jobNumber}
                      </span>
                      <p className="mt-1 font-mono-tabular text-[11px] text-slate-500 dark:text-slate-400">
                        {new Date(job.createdAt).toLocaleDateString()}
                      </p>
                    </div>
                    <StageBadge stage={job.stage} />
                  </div>

                  {/* Customer */}
                  <div className="mt-3.5 border-t border-slate-200 pt-3 dark:border-slate-800">
                    <h3 className="font-display text-sm font-semibold text-slate-900 dark:text-slate-100 truncate">
                      {job.customerName || 'Walk-in Customer'}
                    </h3>
                    <div className="mt-1 flex items-center gap-1.5">
                      <span className="font-mono-tabular text-xs text-slate-500 dark:text-slate-400">
                        {job.customerPhone || 'No phone'}
                      </span>
                      {job.customerPhone && (
                        <button
                          type="button"
                          onClick={(e) => handleCopyPhone(e, job)}
                          title="Copy phone"
                          className="rounded p-1 text-slate-400 hover:bg-slate-200 hover:text-slate-700 dark:hover:bg-slate-800 dark:hover:text-slate-100"
                        >
                          {copiedUuid === job.recordUuid ? (
                            <Check className="h-3 w-3 text-emerald-600 dark:text-emerald-400" />
                          ) : (
                            <Copy className="h-3 w-3" />
                          )}
                        </button>
                      )}
                      <button
                        type="button"
                        onClick={(e) => {
                          e.stopPropagation();
                          onOpenWhatsApp(job);
                        }}
                        title="Send WhatsApp update"
                        aria-label={`WhatsApp ${job.customerName || job.jobNumber}`}
                        className="rounded p-1 text-emerald-600 hover:bg-emerald-50 hover:text-emerald-700 dark:text-emerald-400/85 dark:hover:bg-emerald-500/15 dark:hover:text-emerald-300"
                      >
                        <MessageCircle className="h-3.5 w-3.5" />
                      </button>
                    </div>
                  </div>

                  {/* Device & Issue */}
                  <div className="mt-3">
                    <div className="flex items-center gap-1.5 text-xs font-medium text-slate-800 dark:text-slate-200">
                      <Smartphone className="h-3.5 w-3.5 text-slate-400 shrink-0" />
                      <span className="truncate">{deviceLabel || 'Unspecified Device'}</span>
                    </div>
                    <p className="mt-1 text-xs text-slate-500 dark:text-slate-400 line-clamp-2">
                      {job.issueDescription || 'No issue description'}
                    </p>
                    {renderIssueTags(job.issueTagsCsv)}
                  </div>
                </div>

                {/* Financials Footer */}
                <div className="mt-4 flex items-center justify-between gap-3 border-t border-slate-200 pt-3 dark:border-slate-800">
                  <div className="font-mono-tabular text-[11px] text-slate-500 dark:text-slate-400">
                    <span>
                      Est {currencySymbol}
                      {fin.totalCost.toLocaleString()}
                    </span>
                    <span className="mx-1.5">·</span>
                    <span>
                      Paid {currencySymbol}
                      {fin.advancePaid.toLocaleString()}
                    </span>
                  </div>
                  <div>{renderFinancialBadge(job)}</div>
                </div>
              </article>
            );
          })}
        </div>
      )}
    </section>
  );
}
