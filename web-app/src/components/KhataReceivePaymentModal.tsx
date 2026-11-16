import { useEffect, useMemo, useState } from 'react';
import { AlertCircle, CheckCircle2, CreditCard, X } from 'lucide-react';
import {
  buildKhataPaymentPlan,
  KhataCustomerSummary,
} from '../services/khataEngine';
import { PAYMENT_MODES } from '../types/repairShop';

interface KhataReceivePaymentModalProps {
  isOpen: boolean;
  customer: KhataCustomerSummary | null;
  currencySymbol: string;
  isSaving: boolean;
  onClose: () => void;
  onConfirmPayment: (payload: {
    amount: number;
    paymentMode: string;
    note: string;
  }) => Promise<void>;
}

export function KhataReceivePaymentModal({
  isOpen,
  customer,
  currencySymbol,
  isSaving,
  onClose,
  onConfirmPayment,
}: KhataReceivePaymentModalProps) {
  const [amountInput, setAmountInput] = useState('0');
  const [paymentMode, setPaymentMode] = useState<string>('Cash');
  const [note, setNote] = useState('');
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (isOpen && customer) {
      setAmountInput(String(customer.totalPendingDue));
      setPaymentMode('Cash');
      setNote('');
      setError(null);
    }
  }, [isOpen, customer]);

  const parsedAmount = Number(amountInput);
  const validAmount =
    Number.isFinite(parsedAmount) && parsedAmount > 0 ? parsedAmount : 0;

  const previewPlan = useMemo(() => {
    if (!customer || validAmount <= 0) {
      return { allocations: [], totalApplied: 0, unappliedRemainder: 0 };
    }
    return buildKhataPaymentPlan(
      customer,
      validAmount,
      paymentMode,
      note,
      currencySymbol
    );
  }, [customer, validAmount, paymentMode, note, currencySymbol]);

  if (!isOpen || !customer) return null;

  const exceedsDue = validAmount > customer.totalPendingDue + 0.01;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!Number.isFinite(parsedAmount) || parsedAmount <= 0) {
      setError('Enter a payment amount greater than 0.');
      return;
    }
    if (parsedAmount > customer.totalPendingDue + 0.01) {
      setError(
        `Amount cannot exceed pending balance (${currencySymbol}${customer.totalPendingDue.toLocaleString()}).`
      );
      return;
    }
    if (previewPlan.allocations.length === 0) {
      setError('No unpaid jobs available to apply this payment.');
      return;
    }

    await onConfirmPayment({
      amount: parsedAmount,
      paymentMode,
      note: note.trim(),
    });
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 p-4 backdrop-blur-sm dark:bg-slate-950/80"
      role="dialog"
      aria-modal="true"
      aria-labelledby="khata-receive-payment-title"
      onClick={(e) => {
        if (e.target === e.currentTarget && !isSaving) onClose();
      }}
    >
      <div className="flex max-h-[92vh] w-full max-w-lg flex-col overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-2xl dark:border-slate-700/80 dark:bg-[#1E293B]">
        <div className="flex items-center justify-between border-b border-slate-200 px-6 py-4 dark:border-slate-800">
          <div className="flex items-center gap-2.5">
            <CreditCard className="h-4 w-4 text-emerald-600 dark:text-emerald-400" />
            <div>
              <h3
                id="khata-receive-payment-title"
                className="font-display text-base font-semibold text-slate-900 dark:text-slate-100"
              >
                Receive Khata Payment
              </h3>
              <p className="text-xs text-slate-500 dark:text-slate-400">
                {customer.customerName} · Pending:{' '}
                <span className="font-mono-tabular font-semibold text-red-700 dark:text-red-300">
                  {currencySymbol}
                  {customer.totalPendingDue.toLocaleString()}
                </span>
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            disabled={isSaving}
            className="rounded-lg p-1.5 text-slate-500 hover:bg-slate-100 hover:text-slate-900 disabled:opacity-50 dark:text-slate-400 dark:hover:bg-slate-800 dark:hover:text-slate-100"
            aria-label="Close receive payment modal"
          >
            <X className="h-4 w-4" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="flex flex-1 flex-col overflow-hidden">
          <div className="flex-1 space-y-5 overflow-y-auto p-6">
            {error && (
              <div className="flex items-center gap-2 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-xs font-medium text-red-800 dark:border-red-500/40 dark:bg-red-500/12 dark:text-red-200">
                <AlertCircle className="h-4 w-4 text-red-600 dark:text-red-400 shrink-0" />
                <span>{error}</span>
              </div>
            )}

            <div>
              <div className="mb-1.5 flex items-center justify-between">
                <label
                  htmlFor="khata-payment-amount"
                  className="text-xs font-medium text-slate-600 dark:text-slate-400"
                >
                  Amount Received ({currencySymbol})
                </label>
                <div className="flex items-center gap-1.5">
                  <button
                    type="button"
                    onClick={() => setAmountInput(String(customer.totalPendingDue))}
                    className="rounded-md border border-slate-300 bg-slate-50 px-2 py-0.5 text-[11px] font-medium text-blue-700 hover:border-blue-500/60 dark:border-slate-700 dark:bg-[#0F172A] dark:text-blue-300 dark:hover:border-blue-500/50"
                  >
                    Full ({currencySymbol}
                    {customer.totalPendingDue.toLocaleString()})
                  </button>
                </div>
              </div>
              <input
                id="khata-payment-amount"
                type="number"
                min={0.01}
                step="any"
                required
                value={amountInput}
                onChange={(e) => {
                  setAmountInput(e.target.value);
                  setError(null);
                }}
                className="w-full rounded-xl border border-slate-300 bg-slate-50 px-3.5 py-2.5 font-mono-tabular text-base font-semibold text-slate-900 focus:border-blue-500 focus:outline-none dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-100"
              />
              {exceedsDue && (
                <p className="mt-1 text-xs text-red-700 dark:text-red-300">
                  Entered amount exceeds total pending due of {currencySymbol}
                  {customer.totalPendingDue.toLocaleString()}.
                </p>
              )}
            </div>

            <div>
              <span className="mb-2 block text-xs font-medium text-slate-600 dark:text-slate-400">
                Payment Mode
              </span>
              <div className="grid grid-cols-4 gap-2">
                {PAYMENT_MODES.map((mode) => (
                  <button
                    key={mode}
                    type="button"
                    onClick={() => setPaymentMode(mode)}
                    className={`rounded-xl border px-3 py-2 text-xs font-semibold transition-colors ${
                      paymentMode === mode
                        ? 'border-blue-600 bg-[#2563EB] text-white'
                        : 'border-slate-300 bg-slate-50 text-slate-700 hover:border-slate-400 dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-300 dark:hover:border-slate-600'
                    }`}
                  >
                    {mode}
                  </button>
                ))}
              </div>
            </div>

            <div>
              <label
                htmlFor="khata-payment-note"
                className="mb-1.5 block text-xs font-medium text-slate-600 dark:text-slate-400"
              >
                Note (Optional)
              </label>
              <input
                id="khata-payment-note"
                type="text"
                value={note}
                onChange={(e) => setNote(e.target.value)}
                placeholder="e.g. GPay ref #, settled at counter"
                className="w-full rounded-xl border border-slate-300 bg-slate-50 px-3.5 py-2.5 text-sm text-slate-900 placeholder:text-slate-400 focus:border-blue-500 focus:outline-none dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-100 dark:placeholder:text-slate-500"
              />
            </div>

            {/* Oldest-First Distribution Preview */}
            <div className="rounded-xl border border-slate-200 bg-slate-50 p-4 space-y-2.5 dark:border-slate-800 dark:bg-[#0F172A]">
              <div className="flex items-center justify-between text-xs">
                <span className="font-semibold text-slate-700 dark:text-slate-300">
                  Oldest-First Allocation Preview
                </span>
                <span className="font-mono-tabular text-slate-500 dark:text-slate-400">
                  {previewPlan.allocations.length}{' '}
                  {previewPlan.allocations.length === 1 ? 'job' : 'jobs'} updated
                </span>
              </div>

              {previewPlan.allocations.length === 0 ? (
                <p className="text-xs text-slate-500">
                  Enter a positive amount to preview how it distributes across unpaid jobs.
                </p>
              ) : (
                <div className="space-y-1.5">
                  {previewPlan.allocations.map((alloc) => (
                    <div
                      key={alloc.originalJob.recordUuid}
                      className="flex items-center justify-between gap-3 rounded-lg border border-slate-200 bg-white px-3 py-2 text-xs dark:border-slate-800/80 dark:bg-[#1E293B]/70"
                    >
                      <div className="min-w-0">
                        <span className="font-mono-tabular font-semibold text-blue-700 dark:text-blue-300">
                          {alloc.originalJob.jobNumber}
                        </span>
                        <span className="ml-2 text-slate-500 dark:text-slate-400 truncate">
                          {alloc.originalJob.deviceBrand} {alloc.originalJob.deviceModel}
                        </span>
                      </div>
                      <div className="flex items-center gap-2 shrink-0 font-mono-tabular">
                        <span className="font-semibold text-emerald-700 dark:text-emerald-300">
                          +{currencySymbol}
                          {alloc.appliedAmount.toLocaleString()}
                        </span>
                        {alloc.remainingDue <= 0.01 ? (
                          <span className="rounded bg-emerald-100 px-1.5 py-0.5 text-[10px] font-semibold text-emerald-800 dark:bg-emerald-500/20 dark:text-emerald-300">
                            PAID
                          </span>
                        ) : (
                          <span className="rounded bg-amber-100 px-1.5 py-0.5 text-[10px] font-semibold text-amber-800 dark:bg-amber-500/15 dark:text-amber-300">
                            Rem {currencySymbol}
                            {alloc.remainingDue.toLocaleString()}
                          </span>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>

          <div className="flex items-center justify-end gap-3 border-t border-slate-200 bg-slate-50 px-6 py-4 dark:border-slate-800 dark:bg-[#0F172A]">
            <button
              type="button"
              onClick={onClose}
              disabled={isSaving}
              className="rounded-xl border border-slate-300 bg-white px-4 py-2 text-xs font-medium text-slate-700 hover:bg-slate-100 disabled:opacity-50 dark:border-slate-700 dark:bg-[#1E293B] dark:text-slate-200 dark:hover:bg-slate-800"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={isSaving || validAmount <= 0 || exceedsDue}
              className="inline-flex items-center gap-2 rounded-xl bg-emerald-600 px-5 py-2 text-xs font-semibold text-white transition-colors hover:bg-emerald-500 disabled:opacity-50"
            >
              <CheckCircle2 className="h-4 w-4" />
              <span>
                {isSaving
                  ? 'Saving & Syncing...'
                  : `Confirm ${currencySymbol}${validAmount.toLocaleString()}`}
              </span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
