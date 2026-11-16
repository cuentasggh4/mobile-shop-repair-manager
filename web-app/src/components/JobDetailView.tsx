import { useEffect, useState } from 'react';
import {
  ArrowLeft,
  Calendar,
  Check,
  ClipboardCheck,
  Copy,
  CreditCard,
  Edit3,
  History,
  Lock,
  MessageCircle,
  PackageCheck,
  Printer,
  RefreshCw,
  Save,
  Smartphone,
  Trash2,
  User,
  UserCheck,
  Wrench,
} from 'lucide-react';
import {
  appendStatusLog,
  CHECKLIST_ITEMS,
  computeJobFinancials,
  JobStage,
  parseChecklist,
  parseStatusLogs,
  RepairJob,
} from '../types/repairShop';
import { BillingPaymentModal } from './BillingPaymentModal';
import { DeliveryHandoverModal } from './DeliveryHandoverModal';
import { EditJobModal } from './EditJobModal';
import { StageBadge } from './StageBadge';
import { UpdateStageModal } from './UpdateStageModal';

interface JobDetailViewProps {
  job: RepairJob;
  currencySymbol: string;
  isSaving: boolean;
  onBack: () => void;
  onUpdateJob: (
    updatedJob: RepairJob,
    auditDetails: string
  ) => Promise<boolean>;
  onRequestSoftDelete: (job: RepairJob) => void;
  onOpenPrintReceipt: (job: RepairJob) => void;
  onOpenWhatsApp: (job: RepairJob) => void;
}

export function JobDetailView({
  job,
  currencySymbol,
  isSaving,
  onBack,
  onUpdateJob,
  onRequestSoftDelete,
  onOpenPrintReceipt,
  onOpenWhatsApp,
}: JobDetailViewProps) {
  const [notesDraft, setNotesDraft] = useState(job.technicianNotes);
  const [copiedPhone, setCopiedPhone] = useState(false);

  const [isUpdateStageOpen, setIsUpdateStageOpen] = useState(false);
  const [isBillingOpen, setIsBillingOpen] = useState(false);
  const [isDeliveryOpen, setIsDeliveryOpen] = useState(false);
  const [isEditOpen, setIsEditOpen] = useState(false);

  useEffect(() => {
    setNotesDraft(job.technicianNotes);
  }, [job.recordUuid, job.technicianNotes]);

  const fin = computeJobFinancials(job);
  const parsedChecklist = parseChecklist(job.checklistJson);
  const statusLogs = parseStatusLogs(job.statusLogsJson);
  const issueTags = job.issueTagsCsv
    .split(',')
    .map((t) => t.trim())
    .filter(Boolean);

  const notesDirty = notesDraft !== job.technicianNotes;

  const handleCopyPhone = async () => {
    if (!job.customerPhone) return;
    try {
      await navigator.clipboard.writeText(job.customerPhone);
      setCopiedPhone(true);
      window.setTimeout(() => setCopiedPhone(false), 1500);
    } catch {
      // Clipboard unavailable
    }
  };

  const handleSaveNotes = async () => {
    const updated: RepairJob = {
      ...job,
      technicianNotes: notesDraft.trim(),
      updatedAt: Date.now(),
    };
    await onUpdateJob(updated, `Updated technician notes on ${job.jobNumber}`);
  };

  const handleSaveStage = async (nextStage: JobStage, note: string) => {
    const now = Date.now();
    const updated: RepairJob = {
      ...job,
      stage: nextStage,
      statusLogsJson: appendStatusLog(job.statusLogsJson, {
        stage: nextStage,
        timestamp: now,
        note: note || `Stage updated to ${nextStage}`,
      }),
      updatedAt: now,
    };
    const ok = await onUpdateJob(
      updated,
      `Stage changed from ${job.stage} to ${nextStage}`
    );
    if (ok) setIsUpdateStageOpen(false);
  };

  const handleSaveBilling = async (payload: {
    finalCost: number;
    discount: number;
    isPaid: boolean;
    paymentMode: string;
  }) => {
    const updated: RepairJob = {
      ...job,
      finalCost: payload.finalCost,
      discount: payload.discount,
      isPaid: payload.isPaid,
      paymentMode: payload.paymentMode,
      updatedAt: Date.now(),
    };
    const ok = await onUpdateJob(
      updated,
      `Updated billing (Final: ${currencySymbol}${payload.finalCost}, Discount: ${currencySymbol}${payload.discount}, Paid: ${payload.isPaid}, Mode: ${payload.paymentMode})`
    );
    if (ok) setIsBillingOpen(false);
  };

  const handleConfirmDelivery = async (payload: {
    finalCost: number;
    discount: number;
    isPaid: boolean;
    paymentMode: string;
    pickedUpByOriginalCustomer: boolean;
    recipientName: string;
    recipientPhone: string;
    deliveryNote: string;
  }) => {
    const now = Date.now();
    const effectiveFinal = payload.finalCost > 0 ? payload.finalCost : job.estimatedCost;
    const netAmount = Math.max(0, effectiveFinal - payload.discount);
    const recipientLabel = payload.pickedUpByOriginalCustomer
      ? `${job.customerName || 'Original Customer'} (Owner)`
      : `${payload.recipientName}${payload.recipientPhone ? ` (${payload.recipientPhone})` : ''} (Representative)`;

    const noteParts = [
      `Handed over to: ${recipientLabel}`,
      `Final: ${currencySymbol}${effectiveFinal.toLocaleString()}`,
      `Net: ${currencySymbol}${netAmount.toLocaleString()}`,
      payload.isPaid ? `Paid (${payload.paymentMode})` : 'Pending Balance',
    ];
    if (payload.deliveryNote) {
      noteParts.push(payload.deliveryNote);
    }

    const updated: RepairJob = {
      ...job,
      stage: JobStage.DELIVERED,
      finalCost: payload.finalCost,
      discount: payload.discount,
      isPaid: payload.isPaid,
      paymentMode: payload.paymentMode,
      pickedUpByOriginalCustomer: payload.pickedUpByOriginalCustomer,
      recipientName: payload.recipientName,
      recipientPhone: payload.recipientPhone,
      deliveredAt: now,
      updatedAt: now,
      statusLogsJson: appendStatusLog(job.statusLogsJson, {
        stage: JobStage.DELIVERED,
        timestamp: now,
        note: noteParts.join(' • '),
      }),
    };

    const ok = await onUpdateJob(
      updated,
      `Delivered ${job.jobNumber} to ${recipientLabel}`
    );
    if (ok) setIsDeliveryOpen(false);
  };

  const handleSaveEdit = async (edited: RepairJob) => {
    const ok = await onUpdateJob(edited, `Edited job details for ${edited.jobNumber}`);
    if (ok) setIsEditOpen(false);
  };

  return (
    <div className="space-y-6">
      {/* Job Detail Header Bar */}
      <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm dark:border-slate-800 dark:bg-[#1E293B] dark:shadow-none">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
          <div className="space-y-2">
            <div className="flex flex-wrap items-center gap-3">
              <button
                type="button"
                onClick={onBack}
                className="inline-flex items-center gap-1.5 rounded-xl border border-slate-200 bg-slate-50 px-3 py-1.5 text-xs font-medium text-slate-700 transition-colors hover:border-slate-300 hover:text-slate-900 dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-200 dark:hover:border-slate-600 dark:hover:text-white"
              >
                <ArrowLeft className="h-3.5 w-3.5" />
                <span>Back</span>
              </button>

              <span className="rounded-lg border border-blue-200 bg-blue-50 px-3 py-1 font-mono-tabular text-sm font-bold text-blue-700 dark:border-blue-500/40 dark:bg-blue-600/20 dark:text-blue-200">
                {job.jobNumber}
              </span>

              <StageBadge stage={job.stage} />
            </div>

            <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-slate-500 dark:text-slate-400">
              <span className="inline-flex items-center gap-1.5 font-mono-tabular">
                <Calendar className="h-3.5 w-3.5 text-slate-400 dark:text-slate-500" />
                <span>Created: {new Date(job.createdAt).toLocaleString()}</span>
              </span>
              {job.deliveredAt && (
                <span className="inline-flex items-center gap-1.5 font-mono-tabular text-emerald-700 dark:text-emerald-300">
                  <PackageCheck className="h-3.5 w-3.5 text-emerald-600 dark:text-emerald-400" />
                  <span>Delivered: {new Date(job.deliveredAt).toLocaleString()}</span>
                </span>
              )}
              <span className="font-mono-tabular">
                {job.sheetRow ? `Sheet Row ${job.sheetRow}` : 'Queued for Sheet Sync'}
              </span>
            </div>
          </div>

          {/* Action Toolbar */}
          <div className="flex flex-wrap items-center gap-2 shrink-0">
            <button
              type="button"
              onClick={() => setIsUpdateStageOpen(true)}
              disabled={isSaving}
              className="inline-flex items-center gap-1.5 rounded-xl border border-slate-200 bg-slate-50 px-3.5 py-2 text-xs font-medium text-slate-700 transition-colors hover:border-blue-300 hover:text-slate-900 disabled:opacity-50 dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-200 dark:hover:border-blue-500/40 dark:hover:text-white"
            >
              <RefreshCw className="h-3.5 w-3.5 text-blue-600 dark:text-blue-400" />
              <span>Update Stage</span>
            </button>

            <button
              type="button"
              onClick={() => setIsEditOpen(true)}
              disabled={isSaving}
              className="inline-flex items-center gap-1.5 rounded-xl border border-slate-200 bg-slate-50 px-3.5 py-2 text-xs font-medium text-slate-700 transition-colors hover:border-slate-300 hover:text-slate-900 disabled:opacity-50 dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-200 dark:hover:border-slate-600 dark:hover:text-white"
            >
              <Edit3 className="h-3.5 w-3.5 text-slate-500 dark:text-slate-300" />
              <span>Edit Job</span>
            </button>

            <button
              type="button"
              onClick={() => onOpenPrintReceipt(job)}
              className="inline-flex items-center gap-1.5 rounded-xl border border-slate-200 bg-slate-50 px-3.5 py-2 text-xs font-medium text-slate-700 transition-colors hover:border-slate-300 hover:text-slate-900 dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-200 dark:hover:border-slate-600 dark:hover:text-white"
            >
              <Printer className="h-3.5 w-3.5 text-slate-500 dark:text-slate-300" />
              <span>Print Receipt</span>
            </button>

            <button
              type="button"
              onClick={() => onOpenWhatsApp(job)}
              className="inline-flex items-center gap-1.5 rounded-xl border border-emerald-200 bg-emerald-50 px-3.5 py-2 text-xs font-medium text-emerald-700 transition-colors hover:bg-emerald-100 dark:border-emerald-500/35 dark:bg-emerald-500/10 dark:text-emerald-300 dark:hover:bg-emerald-500/20"
            >
              <MessageCircle className="h-3.5 w-3.5" />
              <span>WhatsApp</span>
            </button>

            {job.stage !== JobStage.DELIVERED && (
              <button
                type="button"
                onClick={() => setIsDeliveryOpen(true)}
                disabled={isSaving}
                className="inline-flex items-center gap-1.5 rounded-xl bg-emerald-600 px-4 py-2 text-xs font-semibold text-white shadow-sm transition-colors hover:bg-emerald-500 disabled:opacity-50"
              >
                <PackageCheck className="h-4 w-4" />
                <span>Deliver Device</span>
              </button>
            )}

            <button
              type="button"
              onClick={() => onRequestSoftDelete(job)}
              disabled={isSaving}
              className="inline-flex items-center gap-1.5 rounded-xl border border-red-200 bg-red-50 px-3.5 py-2 text-xs font-medium text-red-700 transition-colors hover:bg-red-100 disabled:opacity-50 dark:border-red-500/35 dark:bg-red-500/10 dark:text-red-300 dark:hover:bg-red-500/20"
            >
              <Trash2 className="h-3.5 w-3.5" />
              <span>Delete Job</span>
            </button>
          </div>
        </div>
      </section>

      {/* 2-Column Desktop Overview */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-12">
        {/* LEFT COLUMN: Customer & Device, 12-Point Checklist, Technician Notes */}
        <div className="space-y-6 lg:col-span-7">
          {/* Customer & Device Panel */}
          <section className="space-y-5 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-[#1E293B] dark:shadow-none">
            <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
              <div className="space-y-2">
                <div className="flex items-center gap-2 text-xs font-medium text-slate-500 dark:text-slate-400">
                  <User className="h-4 w-4 text-blue-600 dark:text-blue-400" />
                  <span>Customer Information</span>
                </div>
                <h2 className="font-display text-lg font-semibold text-slate-900 dark:text-slate-100">
                  {job.customerName || 'Walk-in Customer'}
                </h2>
                <div className="flex flex-wrap items-center gap-2">
                  <span className="font-mono-tabular text-sm text-slate-700 dark:text-slate-300">
                    {job.customerPhone || 'No phone number'}
                  </span>
                  {job.customerPhone && (
                    <button
                      type="button"
                      onClick={handleCopyPhone}
                      className="inline-flex items-center gap-1 rounded-lg border border-slate-200 bg-slate-50 px-2 py-1 text-[11px] font-medium text-slate-700 hover:text-slate-900 dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-300 dark:hover:text-white"
                    >
                      {copiedPhone ? (
                        <>
                          <Check className="h-3 w-3 text-emerald-600 dark:text-emerald-400" />
                          <span>Copied</span>
                        </>
                      ) : (
                        <>
                          <Copy className="h-3 w-3" />
                          <span>Copy</span>
                        </>
                      )}
                    </button>
                  )}
                  <button
                    type="button"
                    onClick={() => onOpenWhatsApp(job)}
                    className="inline-flex items-center gap-1 rounded-lg border border-emerald-200 bg-emerald-50 px-2 py-1 text-[11px] font-medium text-emerald-700 hover:bg-emerald-100 dark:border-emerald-500/35 dark:bg-emerald-500/10 dark:text-emerald-300 dark:hover:bg-emerald-500/20"
                  >
                    <MessageCircle className="h-3 w-3" />
                    <span>Message</span>
                  </button>
                </div>
              </div>

              <div className="space-y-2">
                <div className="flex items-center gap-2 text-xs font-medium text-slate-500 dark:text-slate-400">
                  <Smartphone className="h-4 w-4 text-blue-600 dark:text-blue-400" />
                  <span>Device Information</span>
                </div>
                <p className="font-display text-lg font-semibold text-slate-900 dark:text-slate-100">
                  {[job.deviceBrand, job.deviceModel].filter(Boolean).join(' ') || 'Unspecified Device'}
                </p>
                <div className="flex flex-wrap gap-x-4 gap-y-1 text-xs text-slate-600 dark:text-slate-300">
                  <span>
                    Color: <strong className="font-medium text-slate-900 dark:text-slate-100">{job.deviceColor || '—'}</strong>
                  </span>
                  <span className="font-mono-tabular">
                    IMEI/SN: <strong className="font-medium text-slate-900 dark:text-slate-100">{job.imeiOrSerial || '—'}</strong>
                  </span>
                  <span className="inline-flex items-center gap-1 font-mono-tabular">
                    <Lock className="h-3 w-3 text-slate-400" />
                    <span>{job.securityLockCode || 'No lock'}</span>
                  </span>
                </div>
              </div>
            </div>

            <div className="space-y-2.5 border-t border-slate-200 pt-4 dark:border-slate-800">
              <div className="flex items-center gap-2 text-xs font-medium text-slate-500 dark:text-slate-400">
                <Wrench className="h-4 w-4 text-amber-600 dark:text-amber-400" />
                <span>Reported Fault &amp; Issue Description</span>
              </div>
              <p className="text-sm leading-relaxed text-slate-800 dark:text-slate-200">
                {job.issueDescription || 'No issue description recorded.'}
              </p>
              {issueTags.length > 0 && (
                <div className="flex flex-wrap gap-1.5 pt-1">
                  {issueTags.map((tag) => (
                    <span
                      key={tag}
                      className="rounded-lg border border-amber-200 bg-amber-50 px-2.5 py-1 text-xs font-medium text-amber-800 dark:border-amber-500/30 dark:bg-amber-500/10 dark:text-amber-200"
                    >
                      {tag}
                    </span>
                  ))}
                </div>
              )}
            </div>

            {job.stage === JobStage.DELIVERED && (
              <div className="border-t border-slate-200 pt-4 dark:border-slate-800">
                <div className="flex items-center gap-2 text-xs font-medium text-emerald-700 dark:text-emerald-300">
                  <UserCheck className="h-4 w-4" />
                  <span>
                    Handed Over To:{' '}
                    {job.pickedUpByOriginalCustomer
                      ? `${job.customerName} (Original Customer)`
                      : `${job.recipientName || 'Representative'}${job.recipientPhone ? ` · ${job.recipientPhone}` : ''}`}
                  </span>
                </div>
              </div>
            )}
          </section>

          {/* 12-Point Inspection Checklist Summary */}
          <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-[#1E293B] dark:shadow-none">
            <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-200 pb-3.5 dark:border-slate-800">
              <div className="flex items-center gap-2">
                <ClipboardCheck className="h-4 w-4 text-emerald-600 dark:text-emerald-400" />
                <h3 className="font-display text-sm font-semibold text-slate-900 dark:text-slate-100">
                  12-Point Inspection Checklist
                </h3>
              </div>
              <div className="flex items-center gap-3 font-mono-tabular text-xs">
                <span className="text-emerald-700 dark:text-emerald-400">
                  {parsedChecklist.filter((s) => s === 'WORKING').length} OK
                </span>
                <span className="text-red-700 dark:text-red-400">
                  {parsedChecklist.filter((s) => s === 'DEFECTIVE').length} Issue
                </span>
                <span className="text-slate-500 dark:text-slate-400">
                  {parsedChecklist.filter((s) => s === 'UNTESTED').length} N/A
                </span>
                <button
                  type="button"
                  onClick={() => setIsEditOpen(true)}
                  className="font-sans font-medium text-blue-600 hover:text-blue-700 dark:text-blue-400 dark:hover:text-blue-300"
                >
                  Edit
                </button>
              </div>
            </div>

            <div className="mt-4 grid grid-cols-1 gap-2 sm:grid-cols-2">
              {CHECKLIST_ITEMS.map((label, idx) => {
                const state = parsedChecklist[idx] ?? 'UNTESTED';
                return (
                  <div
                    key={label}
                    className="flex items-center justify-between gap-2 rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 dark:border-slate-800 dark:bg-[#0F172A]/80"
                  >
                    <span className="text-xs font-medium text-slate-800 dark:text-slate-200 truncate">
                      {label}
                    </span>
                    {state === 'WORKING' ? (
                      <span className="rounded-md border border-emerald-200 bg-emerald-50 px-2 py-0.5 font-mono-tabular text-[11px] font-semibold text-emerald-700 shrink-0 dark:border-emerald-500/35 dark:bg-emerald-500/15 dark:text-emerald-300">
                        OK
                      </span>
                    ) : state === 'DEFECTIVE' ? (
                      <span className="rounded-md border border-red-200 bg-red-50 px-2 py-0.5 font-mono-tabular text-[11px] font-semibold text-red-700 shrink-0 dark:border-red-500/35 dark:bg-red-500/15 dark:text-red-300">
                        Issue
                      </span>
                    ) : (
                      <span className="rounded-md border border-slate-200 bg-slate-100 px-2 py-0.5 font-mono-tabular text-[11px] font-semibold text-slate-600 shrink-0 dark:border-slate-700 dark:bg-slate-800/70 dark:text-slate-400">
                        N/A
                      </span>
                    )}
                  </div>
                );
              })}
            </div>
          </section>

          {/* Inline Technician Notes */}
          <section className="space-y-3 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-[#1E293B] dark:shadow-none">
            <div className="flex items-center justify-between">
              <h3 className="font-display text-sm font-semibold text-slate-900 dark:text-slate-100">
                Technician Notes
              </h3>
              <button
                type="button"
                onClick={handleSaveNotes}
                disabled={!notesDirty || isSaving}
                className="inline-flex items-center gap-1.5 rounded-xl bg-[#2563EB] px-3.5 py-1.5 text-xs font-semibold text-white transition-colors hover:bg-blue-500 disabled:cursor-not-allowed disabled:opacity-45"
              >
                <Save className="h-3.5 w-3.5" />
                <span>{isSaving ? 'Saving...' : 'Save Notes'}</span>
              </button>
            </div>
            <textarea
              rows={3}
              value={notesDraft}
              onChange={(e) => setNotesDraft(e.target.value)}
              placeholder="Add repair diagnostics, spare part serials, or bench remarks..."
              className="w-full rounded-xl border border-slate-300 bg-slate-50 px-3.5 py-2.5 text-sm text-slate-900 placeholder:text-slate-400 focus:border-blue-500 focus:bg-white focus:outline-none dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-100 dark:placeholder:text-slate-500 dark:focus:bg-[#0F172A]"
            />
          </section>
        </div>

        {/* RIGHT COLUMN: Billing & Payment + Chronological Status Timeline */}
        <div className="space-y-6 lg:col-span-5">
          {/* Billing Summary Card */}
          <section className="space-y-4 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-[#1E293B] dark:shadow-none">
            <div className="flex items-center justify-between gap-3 border-b border-slate-200 pb-3.5 dark:border-slate-800">
              <div className="flex items-center gap-2">
                <CreditCard className="h-4 w-4 text-emerald-600 dark:text-emerald-400" />
                <h3 className="font-display text-sm font-semibold text-slate-900 dark:text-slate-100">
                  Billing &amp; Payment
                </h3>
              </div>

              <button
                type="button"
                onClick={() => setIsBillingOpen(true)}
                disabled={isSaving}
                className="rounded-xl border border-slate-200 bg-slate-50 px-3 py-1.5 text-xs font-medium text-slate-700 hover:border-blue-300 hover:text-slate-900 dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-200 dark:hover:border-blue-500/40 dark:hover:text-white"
              >
                Edit Billing
              </button>
            </div>

            <div className="grid grid-cols-2 gap-3 text-xs">
              <div className="rounded-xl border border-slate-200 bg-slate-50 p-3 dark:border-slate-800 dark:bg-[#0F172A]/80">
                <span className="text-slate-500 dark:text-slate-400">Estimated Cost</span>
                <p className="mt-1 font-mono-tabular text-base font-semibold text-slate-900 dark:text-slate-100">
                  {currencySymbol}
                  {job.estimatedCost.toLocaleString()}
                </p>
              </div>
              <div className="rounded-xl border border-slate-200 bg-slate-50 p-3 dark:border-slate-800 dark:bg-[#0F172A]/80">
                <span className="text-slate-500 dark:text-slate-400">Final Cost</span>
                <p className="mt-1 font-mono-tabular text-base font-semibold text-slate-900 dark:text-slate-100">
                  {job.finalCost > 0
                    ? `${currencySymbol}${job.finalCost.toLocaleString()}`
                    : '— (Using Est.)'}
                </p>
              </div>
              <div className="rounded-xl border border-slate-200 bg-slate-50 p-3 dark:border-slate-800 dark:bg-[#0F172A]/80">
                <span className="text-slate-500 dark:text-slate-400">Advance Paid</span>
                <p className="mt-1 font-mono-tabular text-base font-semibold text-slate-900 dark:text-slate-100">
                  {currencySymbol}
                  {fin.advancePaid.toLocaleString()}
                </p>
              </div>
              <div className="rounded-xl border border-slate-200 bg-slate-50 p-3 dark:border-slate-800 dark:bg-[#0F172A]/80">
                <span className="text-slate-500 dark:text-slate-400">Discount</span>
                <p className="mt-1 font-mono-tabular text-base font-semibold text-slate-900 dark:text-slate-100">
                  {currencySymbol}
                  {fin.discount.toLocaleString()}
                </p>
              </div>
            </div>

            <div className="flex items-center justify-between rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 dark:border-slate-800 dark:bg-[#0F172A]">
              <div>
                <span className="block text-xs text-slate-500 dark:text-slate-400">Balance Due</span>
                <span className="font-mono-tabular text-xs text-slate-500 dark:text-slate-400">
                  Mode: {job.paymentMode || 'Cash'}
                </span>
              </div>
              {fin.isFullyPaid ? (
                <span className="rounded-lg border border-emerald-200 bg-emerald-50 px-3 py-1 font-mono-tabular text-xs font-bold text-emerald-700 dark:border-emerald-500/40 dark:bg-emerald-500/15 dark:text-emerald-300">
                  PAID IN FULL
                </span>
              ) : (
                <span className="font-mono-tabular text-xl font-bold text-amber-700 dark:text-amber-300">
                  {currencySymbol}
                  {fin.balanceDue.toLocaleString()}
                </span>
              )}
            </div>
          </section>

          {/* Status History Timeline */}
          <section className="space-y-4 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-[#1E293B] dark:shadow-none">
            <div className="flex items-center justify-between gap-3 border-b border-slate-200 pb-3.5 dark:border-slate-800">
              <div className="flex items-center gap-2">
                <History className="h-4 w-4 text-blue-600 dark:text-blue-400" />
                <h3 className="font-display text-sm font-semibold text-slate-900 dark:text-slate-100">
                  Repair Timeline ({statusLogs.length})
                </h3>
              </div>
              <button
                type="button"
                onClick={() => setIsUpdateStageOpen(true)}
                disabled={isSaving}
                className="rounded-xl border border-slate-200 bg-slate-50 px-3 py-1.5 text-xs font-medium text-slate-700 hover:border-blue-300 hover:text-slate-900 dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-200 dark:hover:border-blue-500/40 dark:hover:text-white"
              >
                + Update Stage
              </button>
            </div>

            {statusLogs.length === 0 ? (
              <p className="py-4 text-xs text-slate-500 dark:text-slate-400">
                No status log entries recorded yet.
              </p>
            ) : (
              <ol className="space-y-3">
                {[...statusLogs].reverse().map((entry, idx) => (
                  <li
                    key={`${entry.timestamp}-${idx}`}
                    className="space-y-2 rounded-xl border border-slate-200 bg-slate-50 p-3.5 dark:border-slate-800 dark:bg-[#0F172A]/80"
                  >
                    <div className="flex flex-wrap items-center justify-between gap-2">
                      <StageBadge stage={entry.stage} />
                      <span className="font-mono-tabular text-[11px] text-slate-500 dark:text-slate-400">
                        {entry.timestamp > 0
                          ? new Date(entry.timestamp).toLocaleString()
                          : '—'}
                      </span>
                    </div>
                    {entry.note && (
                      <p className="text-xs leading-relaxed text-slate-700 dark:text-slate-300">
                        {entry.note}
                      </p>
                    )}
                  </li>
                ))}
              </ol>
            )}
          </section>
        </div>
      </div>

      {/* Step 4 Modals */}
      <UpdateStageModal
        isOpen={isUpdateStageOpen}
        job={job}
        isSaving={isSaving}
        onClose={() => setIsUpdateStageOpen(false)}
        onSwitchToDeliveryHandover={() => setIsDeliveryOpen(true)}
        onSaveStage={handleSaveStage}
      />

      <BillingPaymentModal
        isOpen={isBillingOpen}
        job={job}
        currencySymbol={currencySymbol}
        isSaving={isSaving}
        onClose={() => setIsBillingOpen(false)}
        onSaveBilling={handleSaveBilling}
      />

      <DeliveryHandoverModal
        isOpen={isDeliveryOpen}
        job={job}
        currencySymbol={currencySymbol}
        isSaving={isSaving}
        onClose={() => setIsDeliveryOpen(false)}
        onConfirmDelivery={handleConfirmDelivery}
      />

      <EditJobModal
        isOpen={isEditOpen}
        job={job}
        currencySymbol={currencySymbol}
        isSaving={isSaving}
        onClose={() => setIsEditOpen(false)}
        onSaveEdit={handleSaveEdit}
      />
    </div>
  );
}
