import { useEffect, useState } from 'react';
import { PackageCheck, UserCheck, Users, X } from 'lucide-react';
import { PAYMENT_MODES, RepairJob } from '../types/repairShop';

interface DeliveryHandoverModalProps {
  isOpen: boolean;
  job: RepairJob;
  currencySymbol: string;
  isSaving: boolean;
  onClose: () => void;
  onConfirmDelivery: (payload: {
    finalCost: number;
    discount: number;
    isPaid: boolean;
    paymentMode: string;
    pickedUpByOriginalCustomer: boolean;
    recipientName: string;
    recipientPhone: string;
    deliveryNote: string;
  }) => Promise<void>;
}

export function DeliveryHandoverModal({
  isOpen,
  job,
  currencySymbol,
  isSaving,
  onClose,
  onConfirmDelivery,
}: DeliveryHandoverModalProps) {
  const [finalCost, setFinalCost] = useState<string>('0');
  const [discount, setDiscount] = useState<string>('0');
  const [isPaid, setIsPaid] = useState(true);
  const [paymentMode, setPaymentMode] = useState<string>('Cash');
  const [pickedUpByOriginalCustomer, setPickedUpByOriginalCustomer] = useState(true);
  const [recipientName, setRecipientName] = useState('');
  const [recipientPhone, setRecipientPhone] = useState('');
  const [deliveryNote, setDeliveryNote] = useState('');
  const [formError, setFormError] = useState<string | null>(null);

  useEffect(() => {
    if (isOpen) {
      const initialFinal = job.finalCost > 0 ? job.finalCost : job.estimatedCost;
      setFinalCost(String(initialFinal));
      setDiscount(String(job.discount));
      setIsPaid(true);
      setPaymentMode(job.paymentMode || 'Cash');
      setPickedUpByOriginalCustomer(job.pickedUpByOriginalCustomer);
      setRecipientName(job.recipientName);
      setRecipientPhone(job.recipientPhone);
      setDeliveryNote('');
      setFormError(null);
    }
  }, [
    isOpen,
    job.discount,
    job.estimatedCost,
    job.finalCost,
    job.paymentMode,
    job.pickedUpByOriginalCustomer,
    job.recipientName,
    job.recipientPhone,
  ]);

  if (!isOpen) return null;

  const parsedFinal = Math.max(0, Number(finalCost) || 0);
  const parsedDiscount = Math.max(0, Number(discount) || 0);
  const total = parsedFinal > 0 ? parsedFinal : job.estimatedCost;
  const netPayable = Math.max(0, total - parsedDiscount);
  const balanceDue = isPaid
    ? 0
    : Math.max(0, netPayable - Math.max(0, job.advancePaid));

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);

    if (!pickedUpByOriginalCustomer && !recipientName.trim()) {
      setFormError('Recipient Name is required when picked up by a representative.');
      return;
    }

    await onConfirmDelivery({
      finalCost: parsedFinal,
      discount: parsedDiscount,
      isPaid,
      paymentMode,
      pickedUpByOriginalCustomer,
      recipientName: pickedUpByOriginalCustomer ? '' : recipientName.trim(),
      recipientPhone: pickedUpByOriginalCustomer ? '' : recipientPhone.trim(),
      deliveryNote: deliveryNote.trim(),
    });
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 p-4 backdrop-blur-sm dark:bg-slate-950/80"
      role="dialog"
      aria-modal="true"
      aria-labelledby="delivery-handover-title"
      onClick={(e) => {
        if (e.target === e.currentTarget && !isSaving) onClose();
      }}
    >
      <div className="flex max-h-[92vh] w-full max-w-xl flex-col overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-2xl dark:border-slate-700/80 dark:bg-[#1E293B]">
        <div className="flex items-center justify-between border-b border-slate-200 px-6 py-4 dark:border-slate-800">
          <div className="flex items-center gap-2.5">
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-emerald-50 text-emerald-600 dark:bg-emerald-500/15 dark:text-emerald-400">
              <PackageCheck className="h-4 w-4" />
            </div>
            <div>
              <h3 id="delivery-handover-title" className="font-display text-base font-semibold text-slate-900 dark:text-slate-100">
                Delivery Handover &amp; Settlement
              </h3>
              <p className="font-mono-tabular text-xs text-slate-500 dark:text-slate-400">
                {job.jobNumber} · {job.customerName}
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            disabled={isSaving}
            className="rounded-lg p-1.5 text-slate-500 hover:bg-slate-100 hover:text-slate-900 dark:text-slate-400 dark:hover:bg-slate-800 dark:hover:text-slate-100"
            aria-label="Close delivery modal"
          >
            <X className="h-4 w-4" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="flex-1 space-y-5 overflow-y-auto p-6">
          {formError && (
            <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-2.5 text-xs font-medium text-red-700 dark:border-red-500/40 dark:bg-red-500/12 dark:text-red-200">
              {formError}
            </div>
          )}

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div>
              <label
                htmlFor="deliver-final-cost"
                className="mb-1.5 block text-xs font-medium text-slate-600 dark:text-slate-400"
              >
                Final Cost ({currencySymbol})
              </label>
              <input
                id="deliver-final-cost"
                type="number"
                min={0}
                step="any"
                value={finalCost}
                onChange={(e) => setFinalCost(e.target.value)}
                className="w-full rounded-xl border border-slate-300 bg-slate-50 px-3.5 py-2.5 font-mono-tabular text-sm text-slate-900 focus:border-blue-500 focus:bg-white focus:outline-none dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-100 dark:focus:bg-[#0F172A]"
              />
            </div>
            <div>
              <label
                htmlFor="deliver-discount"
                className="mb-1.5 block text-xs font-medium text-slate-600 dark:text-slate-400"
              >
                Discount ({currencySymbol})
              </label>
              <input
                id="deliver-discount"
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
                Payment Collected (Mark as Paid)
              </span>
              <span className="font-mono-tabular text-xs text-slate-500 dark:text-slate-400">
                Net after discount: {currencySymbol}
                {netPayable.toLocaleString()} · Advance: {currencySymbol}
                {job.advancePaid.toLocaleString()}
              </span>
            </div>
            <input
              type="checkbox"
              checked={isPaid}
              onChange={(e) => setIsPaid(e.target.checked)}
              className="h-4 w-4 rounded border-slate-300 accent-emerald-600 dark:border-slate-600 dark:accent-emerald-500"
            />
          </label>

          {/* Pickup By Selector */}
          <div className="space-y-3">
            <span className="block text-xs font-medium text-slate-600 dark:text-slate-400">
              Picked Up By
            </span>
            <div className="grid grid-cols-2 gap-2.5">
              <button
                type="button"
                onClick={() => setPickedUpByOriginalCustomer(true)}
                className={`flex items-center justify-center gap-2 rounded-xl border px-3 py-2.5 text-xs font-semibold transition-colors ${
                  pickedUpByOriginalCustomer
                    ? 'border-emerald-300 bg-emerald-50 text-emerald-700 dark:border-emerald-500/60 dark:bg-emerald-500/15 dark:text-emerald-200'
                    : 'border-slate-200 bg-slate-50 text-slate-700 hover:border-slate-300 dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-300 dark:hover:border-slate-600'
                }`}
              >
                <UserCheck className="h-4 w-4" />
                <span>Original Customer</span>
              </button>
              <button
                type="button"
                onClick={() => setPickedUpByOriginalCustomer(false)}
                className={`flex items-center justify-center gap-2 rounded-xl border px-3 py-2.5 text-xs font-semibold transition-colors ${
                  !pickedUpByOriginalCustomer
                    ? 'border-blue-300 bg-blue-50 text-blue-700 dark:border-blue-500/60 dark:bg-blue-600/20 dark:text-blue-200'
                    : 'border-slate-200 bg-slate-50 text-slate-700 hover:border-slate-300 dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-300 dark:hover:border-slate-600'
                }`}
              >
                <Users className="h-4 w-4" />
                <span>Representative</span>
              </button>
            </div>

            {!pickedUpByOriginalCustomer && (
              <div className="grid grid-cols-1 gap-3 rounded-xl border border-slate-200 bg-slate-50 p-3.5 sm:grid-cols-2 dark:border-slate-700/80 dark:bg-[#0F172A]/70">
                <div>
                  <label
                    htmlFor="deliver-recipient-name"
                    className="mb-1 block text-xs font-medium text-slate-600 dark:text-slate-400"
                  >
                    Recipient Name <span className="text-red-500 dark:text-red-400">*</span>
                  </label>
                  <input
                    id="deliver-recipient-name"
                    type="text"
                    required
                    value={recipientName}
                    onChange={(e) => setRecipientName(e.target.value)}
                    placeholder="Full name"
                    className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-xs text-slate-900 focus:border-blue-500 focus:outline-none dark:border-slate-700 dark:bg-[#1E293B] dark:text-slate-100"
                  />
                </div>
                <div>
                  <label
                    htmlFor="deliver-recipient-phone"
                    className="mb-1 block text-xs font-medium text-slate-600 dark:text-slate-400"
                  >
                    Recipient Phone
                  </label>
                  <input
                    id="deliver-recipient-phone"
                    type="tel"
                    value={recipientPhone}
                    onChange={(e) => setRecipientPhone(e.target.value)}
                    placeholder="Phone number"
                    className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 font-mono-tabular text-xs text-slate-900 focus:border-blue-500 focus:outline-none dark:border-slate-700 dark:bg-[#1E293B] dark:text-slate-100"
                  />
                </div>
              </div>
            )}
          </div>

          <div>
            <label
              htmlFor="deliver-note"
              className="mb-1.5 block text-xs font-medium text-slate-600 dark:text-slate-400"
            >
              Delivery Note (Optional)
            </label>
            <input
              id="deliver-note"
              type="text"
              value={deliveryNote}
              onChange={(e) => setDeliveryNote(e.target.value)}
              placeholder="Counter inspection or handover remarks"
              className="w-full rounded-xl border border-slate-300 bg-slate-50 px-3.5 py-2.5 text-sm text-slate-900 placeholder:text-slate-400 focus:border-blue-500 focus:bg-white focus:outline-none dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-100 dark:placeholder:text-slate-500 dark:focus:bg-[#0F172A]"
            />
          </div>

          <div className="flex items-center justify-between rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-xs dark:border-slate-800 dark:bg-[#0F172A]">
            <span className="text-slate-500 dark:text-slate-400">Remaining Balance on Handover</span>
            <span
              className={`font-mono-tabular text-sm font-bold ${
                balanceDue === 0
                  ? 'text-emerald-700 dark:text-emerald-400'
                  : 'text-amber-700 dark:text-amber-300'
              }`}
            >
              {isPaid
                ? `SETTLED (${paymentMode})`
                : `${currencySymbol}${balanceDue.toLocaleString()} DUE`}
            </span>
          </div>

          <div className="flex items-center justify-end gap-3 border-t border-slate-200 pt-4 dark:border-slate-800">
            <button
              type="button"
              onClick={onClose}
              disabled={isSaving}
              className="rounded-xl border border-slate-300 bg-white px-4 py-2.5 text-xs font-medium text-slate-700 hover:bg-slate-50 disabled:opacity-50 dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-200 dark:hover:bg-slate-800"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={isSaving}
              className="inline-flex items-center gap-2 rounded-xl bg-emerald-600 px-5 py-2.5 text-xs font-semibold text-white hover:bg-emerald-500 disabled:opacity-50"
            >
              <PackageCheck className="h-4 w-4" />
              <span>{isSaving ? 'Completing...' : 'Complete & Deliver Device'}</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
