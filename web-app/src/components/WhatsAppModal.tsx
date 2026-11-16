import { useEffect, useState } from 'react';
import { Check, Copy, ExternalLink, MessageCircle, X } from 'lucide-react';
import {
  buildKhataWhatsAppReminderMessage,
  KhataCustomerSummary,
} from '../services/khataEngine';
import {
  computeJobFinancials,
  digitsOnly,
  JobStage,
  RepairJob,
  ShopProfile,
  STAGE_LABELS,
} from '../types/repairShop';

interface WhatsAppModalProps {
  isOpen: boolean;
  job: RepairJob | null;
  khataCustomer?: KhataCustomerSummary | null;
  shopProfile: ShopProfile | null;
  onClose: () => void;
}

function buildDefaultMessage(
  job: RepairJob,
  shopProfile: ShopProfile | null,
  template: 'auto' | 'status' | 'ready' | 'delivered'
): string {
  const shopName = shopProfile?.shopName || 'Mobile Repair Shop';
  const shopContact = shopProfile?.contactPhone || shopProfile?.whatsappNumber || '';
  const currency = shopProfile?.currencySymbol || '₹';
  const warrantyDays = shopProfile?.warrantyDays ?? 90;
  const fin = computeJobFinancials(job);
  const device = [job.deviceBrand, job.deviceModel].filter(Boolean).join(' ') || 'your device';
  const stageLabel = STAGE_LABELS[job.stage] || job.stage;

  const effectiveMode =
    template === 'auto'
      ? job.stage === JobStage.READY_FOR_PICKUP
        ? 'ready'
        : job.stage === JobStage.DELIVERED
          ? 'delivered'
          : 'status'
      : template;

  const greeting = `Hello ${job.customerName || 'Customer'},`;
  const footer = [
    `Warranty: ${warrantyDays} days on replaced parts (as per shop terms).`,
    shopContact ? `Contact: ${shopContact}` : '',
    `— ${shopName}`,
  ]
    .filter(Boolean)
    .join('\n');

  const billingBlock = [
    `Total/Est Cost: ${currency}${fin.totalCost.toLocaleString()}`,
    `Advance Paid: ${currency}${fin.advancePaid.toLocaleString()}`,
    fin.discount > 0 ? `Discount: ${currency}${fin.discount.toLocaleString()}` : '',
    fin.isFullyPaid
      ? `Payment Status: PAID (${job.paymentMode || 'Cash'})`
      : `Balance Due: ${currency}${fin.balanceDue.toLocaleString()}`,
  ]
    .filter(Boolean)
    .join('\n');

  if (effectiveMode === 'ready') {
    return `${greeting}

Your device *${device}* (Job *${job.jobNumber}*) is *Ready for Pickup*!
Issue: ${job.issueDescription || 'Repair completed'}

${billingBlock}

Please visit ${shopName} to collect your device.
${footer}`;
  }

  if (effectiveMode === 'delivered') {
    return `${greeting}

Thank you for choosing *${shopName}*. Your device *${device}* (Job *${job.jobNumber}*) has been *Delivered*.
Issue: ${job.issueDescription || 'Repaired'}

${billingBlock}

${footer}`;
  }

  return `${greeting}

Update for your repair ticket *${job.jobNumber}* at *${shopName}*:
Device: ${device}
Current Stage: *${stageLabel}*
Reported Issue: ${job.issueDescription || '—'}

${billingBlock}

${footer}`;
}

function normalizeWhatsAppPhone(rawPhone: string, defaultCountryCode: string): string {
  const digits = digitsOnly(rawPhone);
  if (!digits) return '';
  if (digits.length === 10 && defaultCountryCode.trim()) {
    return `${digitsOnly(defaultCountryCode)}${digits}`;
  }
  return digits;
}

export function WhatsAppModal({
  isOpen,
  job,
  khataCustomer = null,
  shopProfile,
  onClose,
}: WhatsAppModalProps) {
  const [countryCode, setCountryCode] = useState('91');
  const [phoneInput, setPhoneInput] = useState('');
  const [message, setMessage] = useState('');
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    if (!isOpen) return;
    if (khataCustomer) {
      setPhoneInput(khataCustomer.customerPhone || '');
      setMessage(buildKhataWhatsAppReminderMessage(khataCustomer, shopProfile));
      setCopied(false);
    } else if (job) {
      setPhoneInput(job.customerPhone || '');
      setMessage(buildDefaultMessage(job, shopProfile, 'auto'));
      setCopied(false);
    }
  }, [isOpen, job, khataCustomer, shopProfile]);

  if (!isOpen || (!job && !khataCustomer)) return null;

  const fullDigits = normalizeWhatsAppPhone(phoneInput, countryCode);
  const whatsappUrl = fullDigits
    ? `https://api.whatsapp.com/send?phone=${encodeURIComponent(fullDigits)}&text=${encodeURIComponent(message)}`
    : `https://api.whatsapp.com/send?text=${encodeURIComponent(message)}`;

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(message);
      setCopied(true);
      window.setTimeout(() => setCopied(false), 1800);
    } catch {
      // Clipboard unavailable
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 p-4 backdrop-blur-sm dark:bg-slate-950/80"
      role="dialog"
      aria-modal="true"
      aria-labelledby="whatsapp-modal-title"
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div className="w-full max-w-xl rounded-2xl border border-slate-200 bg-white shadow-2xl overflow-hidden dark:border-slate-700/80 dark:bg-[#1E293B]">
        <div className="flex items-center justify-between border-b border-slate-200 px-6 py-4 dark:border-slate-800">
          <div className="flex items-center gap-2.5">
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-emerald-100 text-emerald-700 dark:bg-emerald-500/15 dark:text-emerald-400">
              <MessageCircle className="h-4 w-4" />
            </div>
            <div>
              <h3 id="whatsapp-modal-title" className="font-display text-base font-semibold text-slate-900 dark:text-slate-100">
                {khataCustomer ? 'Khata Payment Reminder' : 'WhatsApp Web Dispatcher'}
              </h3>
              <p className="font-mono-tabular text-xs text-slate-500 dark:text-slate-400">
                {khataCustomer
                  ? `${khataCustomer.customerName} · ${khataCustomer.unpaidJobsCount} unpaid`
                  : `${job?.jobNumber ?? ''} · ${job?.customerName ?? ''}`}
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="rounded-lg p-1.5 text-slate-500 hover:bg-slate-100 hover:text-slate-900 dark:text-slate-400 dark:hover:bg-slate-800 dark:hover:text-slate-100"
            aria-label="Close WhatsApp modal"
          >
            <X className="h-4 w-4" />
          </button>
        </div>

        <div className="space-y-4 p-6">
          {/* Recipient Phone + Country Code */}
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
            <div>
              <label className="mb-1 block text-xs font-medium text-slate-600 dark:text-slate-400">
                Country Code
              </label>
              <input
                type="text"
                value={countryCode}
                onChange={(e) => setCountryCode(e.target.value)}
                placeholder="91"
                className="w-full rounded-xl border border-slate-300 bg-slate-50 px-3 py-2 font-mono-tabular text-sm text-slate-900 placeholder:text-slate-400 focus:border-blue-500 focus:outline-none dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-100 dark:placeholder:text-slate-500"
              />
            </div>
            <div className="sm:col-span-2">
              <label className="mb-1 block text-xs font-medium text-slate-600 dark:text-slate-400">
                Customer WhatsApp Number
              </label>
              <input
                type="tel"
                value={phoneInput}
                onChange={(e) => setPhoneInput(e.target.value)}
                placeholder="10-digit WhatsApp number"
                className="w-full rounded-xl border border-slate-300 bg-slate-50 px-3.5 py-2 font-mono-tabular text-sm text-slate-900 placeholder:text-slate-400 focus:border-blue-500 focus:outline-none dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-100 dark:placeholder:text-slate-500"
              />
            </div>
          </div>

          {/* Quick Template Switcher */}
          {job && !khataCustomer && (
            <div>
              <span className="mb-1.5 block text-xs font-medium text-slate-600 dark:text-slate-400">
                Quick Templates
              </span>
              <div className="flex flex-wrap gap-2">
                {[
                  { key: 'status' as const, label: 'Status Update' },
                  { key: 'ready' as const, label: 'Ready for Pickup' },
                  { key: 'delivered' as const, label: 'Delivered & Warranty' },
                ].map((tpl) => (
                  <button
                    key={tpl.key}
                    type="button"
                    onClick={() => setMessage(buildDefaultMessage(job, shopProfile, tpl.key))}
                    className="rounded-lg border border-slate-300 bg-slate-50 px-3 py-1.5 text-xs font-medium text-slate-700 transition-colors hover:border-emerald-500/50 hover:text-emerald-700 dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-300 dark:hover:border-emerald-500/40 dark:hover:text-emerald-200"
                  >
                    {tpl.label}
                  </button>
                ))}
              </div>
            </div>
          )}

          {/* Editable Message */}
          <div>
            <div className="mb-1.5 flex items-center justify-between text-xs">
              <label htmlFor="wa-message-body" className="font-medium text-slate-600 dark:text-slate-400">
                Message Preview (Editable)
              </label>
              <span className="font-mono-tabular text-[11px] text-slate-500 dark:text-slate-400">
                Target: {fullDigits ? `+${fullDigits}` : 'Select recipient in WhatsApp'}
              </span>
            </div>
            <textarea
              id="wa-message-body"
              rows={9}
              value={message}
              onChange={(e) => setMessage(e.target.value)}
              className="w-full rounded-xl border border-slate-300 bg-slate-50 px-3.5 py-2.5 text-xs leading-relaxed text-slate-900 focus:border-emerald-500 focus:outline-none dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-100"
            />
          </div>
        </div>

        <div className="flex flex-wrap items-center justify-between gap-3 border-t border-slate-200 bg-slate-50 px-6 py-4 dark:border-slate-800 dark:bg-[#0F172A]">
          <button
            type="button"
            onClick={handleCopy}
            className="inline-flex items-center gap-2 rounded-xl border border-slate-300 bg-white px-4 py-2 text-xs font-medium text-slate-700 hover:bg-slate-100 hover:text-slate-900 dark:border-slate-700 dark:bg-[#1E293B] dark:text-slate-200 dark:hover:text-white"
          >
            {copied ? (
              <>
                <Check className="h-3.5 w-3.5 text-emerald-600 dark:text-emerald-400" />
                <span>Copied</span>
              </>
            ) : (
              <>
                <Copy className="h-3.5 w-3.5" />
                <span>Copy Message</span>
              </>
            )}
          </button>

          <div className="flex items-center gap-2.5">
            <button
              type="button"
              onClick={onClose}
              className="rounded-xl border border-slate-300 bg-white px-4 py-2 text-xs font-medium text-slate-700 hover:bg-slate-100 hover:text-slate-900 dark:border-slate-700 dark:bg-[#1E293B] dark:text-slate-300 dark:hover:text-white"
            >
              Close
            </button>
            <a
              href={whatsappUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center gap-2 rounded-xl bg-emerald-600 px-4 py-2 text-xs font-semibold text-white transition-colors hover:bg-emerald-500"
            >
              <span>Send via WhatsApp Web</span>
              <ExternalLink className="h-3.5 w-3.5" />
            </a>
          </div>
        </div>
      </div>
    </div>
  );
}
