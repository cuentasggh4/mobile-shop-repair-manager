import { CheckCircle2, IndianRupee, PackageCheck, Wrench } from 'lucide-react';
import { computeJobFinancials, JobStage, RepairJob } from '../types/repairShop';

interface KpiSummaryRowProps {
  jobs: RepairJob[];
  currencySymbol: string;
  onSelectStageFilter?: (stage: JobStage | 'ACTIVE_ALL' | 'ALL') => void;
  onOpenKhataBook?: () => void;
}

export function KpiSummaryRow({
  jobs,
  currencySymbol,
  onSelectStageFilter,
  onOpenKhataBook,
}: KpiSummaryRowProps) {
  const activeJobs = jobs.filter((j) => !j.isDeleted);

  const activeRepairsCount = activeJobs.filter(
    (j) => j.stage !== JobStage.DELIVERED && j.stage !== JobStage.CANCELLED
  ).length;

  const readyForPickupCount = activeJobs.filter(
    (j) => j.stage === JobStage.READY_FOR_PICKUP
  ).length;

  const pendingDuesSum = activeJobs.reduce((sum, job) => {
    if (job.stage === JobStage.CANCELLED) return sum;
    const { balanceDue } = computeJobFinancials(job);
    return sum + balanceDue;
  }, 0);

  const unpaidNonCancelledCount = activeJobs.filter((job) => {
    if (job.stage === JobStage.CANCELLED) return false;
    return computeJobFinancials(job).balanceDue > 0;
  }).length;

  const deliveredCount = activeJobs.filter(
    (j) => j.stage === JobStage.DELIVERED
  ).length;

  return (
    <section
      aria-label="Workshop KPI summary"
      className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4"
    >
      {/* Card 1: Active Repairs */}
      <button
        type="button"
        onClick={() => onSelectStageFilter?.('ALL')}
        className="group rounded-2xl border border-slate-200 bg-white p-5 text-left shadow-sm transition-colors hover:border-blue-400 dark:border-slate-800 dark:bg-[#1E293B] dark:shadow-none dark:hover:border-blue-500/40 focus-visible:outline-2 focus-visible:outline-blue-500"
      >
        <div className="flex items-center justify-between gap-3">
          <span className="text-xs font-medium text-slate-600 dark:text-slate-400">Active Repairs</span>
          <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-blue-100 text-blue-700 dark:bg-blue-500/15 dark:text-blue-400">
            <Wrench className="h-4 w-4" />
          </div>
        </div>
        <p className="mt-3 font-mono-tabular text-3xl font-bold tracking-tight text-slate-900 dark:text-slate-100">
          {activeRepairsCount}
        </p>
        <p className="mt-1.5 text-xs text-slate-500 dark:text-slate-400">
          Diagnostic, parts wait &amp; in progress
        </p>
      </button>

      {/* Card 2: Ready for Pickup (Emerald Accent) */}
      <button
        type="button"
        onClick={() => onSelectStageFilter?.(JobStage.READY_FOR_PICKUP)}
        className="group rounded-2xl border border-emerald-200 bg-white p-5 text-left shadow-sm transition-colors hover:border-emerald-400 dark:border-emerald-500/30 dark:bg-[#1E293B] dark:shadow-none dark:hover:border-emerald-400/60 focus-visible:outline-2 focus-visible:outline-emerald-500"
      >
        <div className="flex items-center justify-between gap-3">
          <span className="text-xs font-semibold text-emerald-800 dark:font-medium dark:text-emerald-300">Ready for Pickup</span>
          <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-emerald-100 text-emerald-700 dark:bg-emerald-500/15 dark:text-emerald-400">
            <CheckCircle2 className="h-4 w-4" />
          </div>
        </div>
        <p className="mt-3 font-mono-tabular text-3xl font-bold tracking-tight text-emerald-700 dark:text-emerald-300">
          {readyForPickupCount}
        </p>
        <p className="mt-1.5 text-xs text-slate-500 dark:text-slate-400">
          Repaired devices awaiting customer
        </p>
      </button>

      {/* Card 3: Pending Dues (Opens Khata Book) */}
      <button
        type="button"
        onClick={() => onOpenKhataBook?.()}
        className="group rounded-2xl border border-amber-200 bg-white p-5 text-left shadow-sm transition-colors hover:border-amber-400 dark:border-amber-500/25 dark:bg-[#1E293B] dark:shadow-none dark:hover:border-amber-400/60 focus-visible:outline-2 focus-visible:outline-amber-500"
      >
        <div className="flex items-center justify-between gap-3">
          <span className="text-xs font-semibold text-amber-800 dark:font-medium dark:text-amber-300">Pending Dues</span>
          <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-amber-100 text-amber-700 dark:bg-amber-500/15 dark:text-amber-400">
            {currencySymbol === '₹' ? (
              <IndianRupee className="h-4 w-4" />
            ) : (
              <span className="font-mono-tabular text-sm font-bold">{currencySymbol}</span>
            )}
          </div>
        </div>
        <p className="mt-3 font-mono-tabular text-3xl font-bold tracking-tight text-slate-900 dark:text-slate-100">
          {currencySymbol}
          {pendingDuesSum.toLocaleString()}
        </p>
        <p className="mt-1.5 font-mono-tabular text-xs text-slate-500 dark:text-slate-400">
          Across {unpaidNonCancelledCount} unpaid {unpaidNonCancelledCount === 1 ? 'job' : 'jobs'} ·{' '}
          <span className="font-sans font-semibold text-amber-700 dark:font-medium dark:text-amber-300 group-hover:underline">
            Open Khata Book
          </span>
        </p>
      </button>

      {/* Card 4: Delivered / Completed */}
      <button
        type="button"
        onClick={() => onSelectStageFilter?.(JobStage.DELIVERED)}
        className="group rounded-2xl border border-slate-200 bg-white p-5 text-left shadow-sm transition-colors hover:border-slate-400 dark:border-slate-800 dark:bg-[#1E293B] dark:shadow-none dark:hover:border-slate-600 focus-visible:outline-2 focus-visible:outline-blue-500"
      >
        <div className="flex items-center justify-between gap-3">
          <span className="text-xs font-medium text-slate-600 dark:text-slate-400">Delivered / Completed</span>
          <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-slate-100 text-slate-700 dark:bg-slate-700/60 dark:text-slate-300">
            <PackageCheck className="h-4 w-4" />
          </div>
        </div>
        <p className="mt-3 font-mono-tabular text-3xl font-bold tracking-tight text-slate-900 dark:text-slate-100">
          {deliveredCount}
        </p>
        <p className="mt-1.5 text-xs text-slate-500 dark:text-slate-400">
          Handed over &amp; closed tickets
        </p>
      </button>
    </section>
  );
}
