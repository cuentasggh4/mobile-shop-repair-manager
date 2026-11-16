import { useEffect, useState } from 'react';
import { CreditCard, X } from 'lucide-react';
import { PAYMENT_MODES, RepairJob } from '../types/repairShop';

interface BillingPaymentModalProps {
  isOpen: boolean;
  job: RepairJob;
  currencySymbol: string;
  isSaving: boolean;
  onClose: () => void;
  onSaveBilling: (payload: {
    finalCost: number;
    discount: number;
    isPaid: boolean;
    paymentMode: string;
  }) => Promise<void>;
}

export function BillingPaymentModal({
  isOpen,
  job,
  currencySymbol,
  isSaving,
  onClose,
  onSaveBilling,
}: BillingPaymentModalProps) {
  const [finalCost, setFinalCost] = useState<string>('0');
  const [discount, setDiscount] = useState<string>('0');
  const [isPaid, setIsPaid] = useState(false);
  const [paymentMode, setPaymentMode] = useState<string>('Cash');

  useEffect(() => {
    if (isOpen) {
      setFinalCost(String(job.finalCost));
      setDiscount(String(job.discount));
      setIsPaid(job.isPaid);
      setPaymentMode(job.paymentMode || 'Cash');
    }
  }, [isOpen, job.discount, job.finalCost, job.isPaid, job.paymentMode]);

  if (!isOpen) return null;

  const parsedFinal = Math.max(0, Number(finalCost) || 0);
  const parsedDiscount = Math.max(0, Number(discount) || 0);
  const total = parsedFinal > 0 ? parsedFinal : job.estimatedCost;
  const balanceDue = isPaid
    ? 0
    : Math.max(0, total - parsedDiscount - Math.max(0, job.advancePaid));

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    await onSaveBilling({
      finalCost: parsedFinal,
      discount: parsedDiscount,
      isPaid,
      paymentMode,
    });
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 p-4 backdrop-blur-sm dark:bg-slate-950/75"
      role="dialog"
      aria-modal="true"
      aria-labelledby="billing-modal-title"
      onClick={(e) => {
        if (e.target === e.currentTarget && !isSaving) onClose();
      }}
    >
      <div className="w-full max-w-lg overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-2xl dark:border-slate-700/80 dark:bg-[#1E293B]">
        <div className="flex items-center justify-between border-b border-slate-200 px-6 py-4 dark:border-slate-800">
          <div className="flex items-center gap-2.5">
            <CreditCard className="h-4 w-4 text-emerald-600 dark:text-emerald-400" />
            <h3 id="billing-modal-title" className="font-display text-base font-semibold text-slate-900 dark:text-slate-100">
              Billing &amp; Payment
            </h3>
            <span className="font-mono-tabular text-xs text-slate-500 dark:text-slate-400">
              {job.jobNumber}
            </span>
          </div>
          <button
            type="button"
            onClick={onClose}
            disabled={isSaving}
            className="rounded-lg p-1.5 text-slate-500 hover:bg-slate-100 hover:text-slate-900 dark:text-slate-400 dark:hover:bg-slate-800 dark:hover:text-slate-100"
            aria-label="Close billing modal"
          >
            <X className="h-4 w-4" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-5 p-6">
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div>
              <label
                htmlFor="billing-final-cost"
                className="mb-1.5 block text-xs font-medium text-slate-600 dark:text-slate-400"
              >
                Final Cost ({currencySymbol})
              </label>
              <input
                id="billing-final-cost"
                type="number"
                min={0}
                step="any"
                value={finalCost}
                onChange={(e) => setFinalCost(e.target.value)}
                className="w-full rounded-xl border border-slate-300 bg-slate-50 px-3.5 py-2.5 font-mono-tabular text-sm text-slate-900 focus:border-blue-500 focus:bg-white focus:outline-none dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-100 dark:focus:bg-[#0F172A]"
              />
              <p className="mt-1 font-mono-tabular text-[11px] text-slate-500 dark:text-slate-400">
                0 uses Estimate ({currencySymbol}
                {job.estimatedCost.toLocaleString()})
              </p>
            </div>

            <div>
              <label
                htmlFor="billing-discount"
                className="mb-1.5 block text-xs font-medium text-slate-600 dark:text-slate-400"
              >
                Discount ({currencySymbol})
              </label>
              <input
                id="billing-discount"
                type="number"
                min={0}
                step="any"
                value={discount}
                onChange={(e) => setDiscount(e.target.value)}
                className="w-full rounded-xl border border-slate-300 bg-slate-50 px-3.5 py-2.5 font-mono-tabular text-sm text-slate-900 focus:border-blue-500 focus:bg-white focus:outline-none dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-100 dark:focus:bg-[#0F172A]"
              />
            </div>
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
                      ? 'border-blue-600 bg-[#2563EB] text-white dark:border-blue-500'
                      : 'border-slate-200 bg-slate-50 text-slate-700 hover:border-slate-300 dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-300 dark:hover:border-slate-600'
                  }`}
                >
                  {mode}
                </button>
              ))}
            </div>
          </div>

          <label className="flex cursor-pointer items-center justify-between rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 dark:border-slate-700/80 dark:bg-[#0F172A]/80">
            <div>
              <span className="block text-sm font-medium text-slate-900 dark:text-slate-100">
                Mark as Paid
              </span>
              <span className="text-xs text-slate-500 dark:text-slate-400">
                Sets IsPaid = true and clears pending balance
              </span>
            </div>
            <input
              type="checkbox"
              checked={isPaid}
              onChange={(e) => setIsPaid(e.target.checked)}
              className="h-4 w-4 rounded border-slate-300 accent-emerald-600 dark:border-slate-600 dark:accent-emerald-500"
            />
          </label>

          {/* Live Financial Breakdown */}
          <div className="space-y-2 rounded-xl border border-slate-200 bg-slate-50 p-4 text-xs dark:border-slate-800 dark:bg-[#0F172A]">
            <div className="flex justify-between text-slate-500 dark:text-slate-400">
              <span>Effective Total</span>
              <span className="font-mono-tabular text-slate-900 dark:text-slate-200">
                {currencySymbol}
                {total.toLocaleString()}
              </span>
            </div>
            <div className="flex justify-between text-slate-500 dark:text-slate-400">
              <span>Less Advance Paid &amp; Discount</span>
              <span className="font-mono-tabular text-slate-900 dark:text-slate-200">
                − {currencySymbol}
                {(Math.max(0, job.advancePaid) + parsedDiscount).toLocaleString()}
              </span>
            </div>
            <div className="flex justify-between border-t border-slate-200 pt-2 text-sm font-semibold dark:border-slate-800">
              <span className="text-slate-800 dark:text-slate-200">Balance Due</span>
              <span
                className={`font-mono-tabular ${
                  balanceDue === 0
                    ? 'text-emerald-700 dark:text-emerald-400'
                    : 'text-amber-700 dark:text-amber-300'
                }`}
              >
                {isPaid
                  ? 'PAID'
                  : `${currencySymbol}${balanceDue.toLocaleString()}`}
              </span>
            </div>
          </div>

          <div className="flex items-center justify-end gap-3 border-t border-slate-200 pt-4 dark:border-slate-800">
            <button
              type="button"
              onClick={onClose}
              disabled={isSaving}
              className="rounded-xl border border-slate-300 bg-white px-4 py-2 text-xs font-medium text-slate-700 hover:bg-slate-50 disabled:opacity-50 dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-200 dark:hover:bg-slate-800"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={isSaving}
              className="rounded-xl bg-[#2563EB] px-5 py-2 text-xs font-semibold text-white hover:bg-blue-500 disabled:opacity-50"
            >
              {isSaving ? 'Saving...' : 'Save Billing'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
