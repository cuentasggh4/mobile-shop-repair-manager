import {
  ArrowLeft,
  CheckCircle2,
  CreditCard,
  MessageCircle,
  Phone,
  Plus,
  User,
} from 'lucide-react';
import {
  computeKhataJobBilling,
  KhataCustomerSummary,
} from '../services/khataEngine';
import { JobStage, RepairJob } from '../types/repairShop';
import { StageBadge } from './StageBadge';

interface KhataCustomerDetailViewProps {
  customer: KhataCustomerSummary;
  currencySymbol: string;
  isSaving: boolean;
  onBack: () => void;
  onSelectJob: (job: RepairJob) => void;
  onOpenWhatsAppReminder: (customer: KhataCustomerSummary) => void;
  onOpenNewJobForCustomer: (customer: { name: string; phone: string }) => void;
  onOpenReceivePayment: () => void;
}

export function KhataCustomerDetailView({
  customer,
  currencySymbol,
  isSaving,
  onBack,
  onSelectJob,
  onOpenWhatsAppReminder,
  onOpenNewJobForCustomer,
  onOpenReceivePayment,
}: KhataCustomerDetailViewProps) {
  const hasDue = customer.totalPendingDue > 0.01;

  return (
    <div className="space-y-6">
      {/* Top Action Bar */}
      <div className="flex flex-wrap items-center justify-between gap-3">
        <button
          type="button"
          onClick={onBack}
          className="inline-flex items-center gap-2 rounded-xl border border-slate-300 bg-white px-3.5 py-2 text-xs font-medium text-slate-700 shadow-sm transition-colors hover:border-slate-400 hover:text-slate-900 dark:border-slate-700/80 dark:bg-[#1E293B] dark:text-slate-200 dark:shadow-none dark:hover:border-slate-600 dark:hover:text-white"
        >
          <ArrowLeft className="h-4 w-4" />
          <span>Back to Khata Book</span>
        </button>

        <div className="flex flex-wrap items-center gap-2.5">
          <button
            type="button"
            onClick={() => onOpenWhatsAppReminder(customer)}
            className="inline-flex items-center gap-1.5 rounded-xl border border-emerald-200 bg-emerald-50 px-3.5 py-2 text-xs font-semibold text-emerald-800 transition-colors hover:bg-emerald-100 dark:border-emerald-500/40 dark:bg-emerald-500/12 dark:text-emerald-200 dark:hover:bg-emerald-500/20"
          >
            <MessageCircle className="h-4 w-4 text-emerald-600 dark:text-emerald-400" />
            <span>WhatsApp Reminder</span>
          </button>

          <button
            type="button"
            onClick={() =>
              onOpenNewJobForCustomer({
                name: customer.customerName,
                phone: customer.customerPhone,
              })
            }
            className="inline-flex items-center gap-1.5 rounded-xl border border-blue-200 bg-blue-50 px-3.5 py-2 text-xs font-semibold text-blue-800 transition-colors hover:bg-blue-100 dark:border-blue-500/40 dark:bg-blue-500/15 dark:text-blue-200 dark:hover:bg-blue-500/25"
          >
            <Plus className="h-4 w-4 text-blue-600 dark:text-blue-400" />
            <span>New Job</span>
          </button>

          <button
            type="button"
            onClick={onOpenReceivePayment}
            disabled={!hasDue || isSaving}
            className="inline-flex items-center gap-1.5 rounded-xl bg-[#2563EB] px-4 py-2 text-xs font-semibold text-white shadow-sm transition-colors hover:bg-blue-500 disabled:cursor-not-allowed disabled:opacity-45"
          >
            <CreditCard className="h-4 w-4" />
            <span>Receive Payment</span>
          </button>
        </div>
      </div>

      {/* Customer Summary Header Card */}
      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-[#1E293B] dark:shadow-none">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div className="flex items-start gap-3.5">
            <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-blue-100 text-blue-700 shrink-0 dark:bg-[#2563EB]/20 dark:text-blue-400">
              <User className="h-6 w-6" />
            </div>
            <div>
              <h1 className="font-display text-2xl font-bold tracking-tight text-slate-900 dark:text-slate-100">
                {customer.customerName}
              </h1>
              <div className="mt-1 flex flex-wrap items-center gap-3 text-xs text-slate-500 dark:text-slate-400">
                <span className="inline-flex items-center gap-1.5 font-mono-tabular text-slate-700 dark:text-slate-300">
                  <Phone className="h-3.5 w-3.5 text-slate-400" />
                  {customer.customerPhone || 'No phone number'}
                </span>
                <span>·</span>
                <span className="font-mono-tabular">
                  Last visit:{' '}
                  {customer.lastVisitAt
                    ? new Date(customer.lastVisitAt).toLocaleDateString()
                    : '—'}
                </span>
              </div>
            </div>
          </div>

          {hasDue ? (
            <span className="inline-flex items-center gap-2 rounded-xl border border-red-200 bg-red-100 px-3.5 py-2 font-mono-tabular text-sm font-bold text-red-800 dark:border-red-500/40 dark:bg-red-500/15 dark:text-red-300">
              Due: {currencySymbol}
              {customer.totalPendingDue.toLocaleString()} ({customer.unpaidJobsCount}{' '}
              {customer.unpaidJobsCount === 1 ? 'job' : 'jobs'})
            </span>
          ) : (
            <span className="inline-flex items-center gap-1.5 rounded-xl border border-emerald-200 bg-emerald-100 px-3.5 py-2 text-sm font-bold text-emerald-800 dark:border-emerald-500/35 dark:bg-emerald-500/15 dark:text-emerald-300">
              <CheckCircle2 className="h-4 w-4" />
              <span>Account Settled</span>
            </span>
          )}
        </div>

        <div className="mt-5 grid grid-cols-2 gap-3 sm:grid-cols-4">
          <div className="rounded-xl border border-slate-200 bg-slate-50 p-3.5 dark:border-slate-800 dark:bg-[#0F172A]/80">
            <span className="text-[11px] font-medium text-slate-500 dark:text-slate-400">Total Visits</span>
            <p className="mt-1 font-mono-tabular text-xl font-bold text-slate-900 dark:text-slate-100">
              {customer.totalJobsCount}
            </p>
          </div>
          <div className="rounded-xl border border-slate-200 bg-slate-50 p-3.5 dark:border-slate-800 dark:bg-[#0F172A]/80">
            <span className="text-[11px] font-medium text-slate-500 dark:text-slate-400">Total Billed</span>
            <p className="mt-1 font-mono-tabular text-xl font-bold text-slate-900 dark:text-slate-100">
              {currencySymbol}
              {customer.totalBilled.toLocaleString()}
            </p>
          </div>
          <div className="rounded-xl border border-slate-200 bg-slate-50 p-3.5 dark:border-slate-800 dark:bg-[#0F172A]/80">
            <span className="text-[11px] font-medium text-slate-500 dark:text-slate-400">Total Paid</span>
            <p className="mt-1 font-mono-tabular text-xl font-bold text-emerald-700 dark:text-emerald-300">
              {currencySymbol}
              {customer.totalPaid.toLocaleString()}
            </p>
          </div>
          <div
            className={`rounded-xl border p-3.5 ${
              hasDue
                ? 'border-red-200 bg-red-50 dark:border-red-500/35 dark:bg-red-500/10'
                : 'border-emerald-200 bg-emerald-50 dark:border-emerald-500/25 dark:bg-emerald-500/10'
            }`}
          >
            <span
              className={`text-[11px] font-medium ${
                hasDue ? 'text-red-700 dark:text-red-200' : 'text-emerald-700 dark:text-emerald-200'
              }`}
            >
              Balance Due
            </span>
            <p
              className={`mt-1 font-mono-tabular text-xl font-bold ${
                hasDue ? 'text-red-800 dark:text-red-300' : 'text-emerald-800 dark:text-emerald-300'
              }`}
            >
              {currencySymbol}
              {customer.totalPendingDue.toLocaleString()}
            </p>
          </div>
        </div>
      </section>

      {/* Job-by-Job History Table */}
      <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm dark:border-slate-800 dark:bg-[#1E293B] dark:shadow-none">
        <div className="flex items-center justify-between border-b border-slate-200 px-5 py-4 dark:border-slate-800">
          <h2 className="font-display text-base font-bold text-slate-900 dark:text-slate-100">
            Job-by-Job Ledger History ({customer.jobs.length})
          </h2>
          <span className="text-xs text-slate-500 dark:text-slate-400">
            Click any row to open Job Detail
          </span>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full border-collapse text-left text-xs">
            <thead>
              <tr className="border-b border-slate-200 bg-slate-50 text-[11px] font-semibold uppercase tracking-wider text-slate-500 dark:border-slate-800 dark:bg-[#0F172A]/70 dark:text-slate-400">
                <th className="px-5 py-3">Job ID</th>
                <th className="px-4 py-3">Device &amp; Issue</th>
                <th className="px-4 py-3">Date</th>
                <th className="px-4 py-3 text-right">Bill</th>
                <th className="px-4 py-3 text-right">Due</th>
                <th className="px-5 py-3 text-right">Stage</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200 dark:divide-slate-800/80">
              {customer.jobs.map((job) => {
                const isCancelled = job.stage === JobStage.CANCELLED;
                const billing = computeKhataJobBilling(job);
                const jobHasDue = !isCancelled && !job.isPaid && billing.balanceDue > 0.01;
                const deviceLabel =
                  [job.deviceBrand, job.deviceModel].filter(Boolean).join(' ') || 'Device';

                return (
                  <tr
                    key={job.recordUuid}
                    onClick={() => onSelectJob(job)}
                    className="cursor-pointer transition-colors hover:bg-slate-50 dark:hover:bg-slate-800/50"
                  >
                    <td className="px-5 py-3.5 font-mono-tabular font-semibold text-blue-700 whitespace-nowrap dark:text-blue-300">
                      {job.jobNumber}
                    </td>
                    <td className="px-4 py-3.5">
                      <div className="font-medium text-slate-900 dark:text-slate-100">{deviceLabel}</div>
                      {job.issueDescription && (
                        <div className="mt-0.5 max-w-md truncate text-[11px] text-slate-500 dark:text-slate-400">
                          {job.issueDescription}
                        </div>
                      )}
                    </td>
                    <td className="px-4 py-3.5 font-mono-tabular text-slate-600 whitespace-nowrap dark:text-slate-300">
                      {new Date(job.createdAt || job.updatedAt).toLocaleDateString()}
                    </td>
                    <td className="px-4 py-3.5 text-right font-mono-tabular text-slate-800 whitespace-nowrap dark:text-slate-200">
                      {isCancelled ? (
                        <span className="text-slate-400 line-through dark:text-slate-500">
                          {currencySymbol}
                          {billing.netBill.toLocaleString()}
                        </span>
                      ) : (
                        `${currencySymbol}${billing.netBill.toLocaleString()}`
                      )}
                    </td>
                    <td className="px-4 py-3.5 text-right font-mono-tabular whitespace-nowrap">
                      {isCancelled ? (
                        <span className="text-slate-400 dark:text-slate-500">—</span>
                      ) : jobHasDue ? (
                        <span className="font-semibold text-red-700 dark:text-red-300">
                          {currencySymbol}
                          {billing.balanceDue.toLocaleString()}
                        </span>
                      ) : (
                        <span className="font-semibold text-emerald-700 dark:text-emerald-300">Paid</span>
                      )}
                    </td>
                    <td className="px-5 py-3.5 text-right">
                      <div className="inline-flex justify-end">
                        <StageBadge stage={job.stage} />
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  );
}
