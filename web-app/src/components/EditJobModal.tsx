import { useEffect, useState } from 'react';
import {
  AlertCircle,
  CheckCircle2,
  ClipboardCheck,
  DollarSign,
  Lock,
  Smartphone,
  User,
  Wrench,
  X,
} from 'lucide-react';
import {
  CHECKLIST_ITEMS,
  ChecklistState,
  createDefaultChecklist,
  DEVICE_BRAND_CHIPS,
  ISSUE_TAG_OPTIONS,
  parseChecklist,
  RepairJob,
  serializeChecklist,
} from '../types/repairShop';

interface EditJobModalProps {
  isOpen: boolean;
  job: RepairJob;
  currencySymbol: string;
  isSaving: boolean;
  onClose: () => void;
  onSaveEdit: (updatedJob: RepairJob) => Promise<void>;
}

export function EditJobModal({
  isOpen,
  job,
  currencySymbol,
  isSaving,
  onClose,
  onSaveEdit,
}: EditJobModalProps) {
  const [customerName, setCustomerName] = useState('');
  const [customerPhone, setCustomerPhone] = useState('');
  const [selectedBrandChip, setSelectedBrandChip] = useState<string>('Apple');
  const [customBrand, setCustomBrand] = useState('');
  const [deviceModel, setDeviceModel] = useState('');
  const [deviceColor, setDeviceColor] = useState('');
  const [imeiOrSerial, setImeiOrSerial] = useState('');
  const [securityLockCode, setSecurityLockCode] = useState('');
  const [selectedIssueTags, setSelectedIssueTags] = useState<string[]>([]);
  const [issueDescription, setIssueDescription] = useState('');
  const [estimatedCost, setEstimatedCost] = useState('0');
  const [advancePaid, setAdvancePaid] = useState('0');
  const [technicianNotes, setTechnicianNotes] = useState('');
  const [checklist, setChecklist] = useState<ChecklistState[]>(() =>
    createDefaultChecklist()
  );
  const [formError, setFormError] = useState<string | null>(null);

  useEffect(() => {
    if (isOpen) {
      setCustomerName(job.customerName);
      setCustomerPhone(job.customerPhone);
      const isKnownBrand = DEVICE_BRAND_CHIPS.includes(
        job.deviceBrand as (typeof DEVICE_BRAND_CHIPS)[number]
      );
      if (isKnownBrand && job.deviceBrand !== 'Other') {
        setSelectedBrandChip(job.deviceBrand);
        setCustomBrand('');
      } else {
        setSelectedBrandChip('Other');
        setCustomBrand(job.deviceBrand);
      }
      setDeviceModel(job.deviceModel);
      setDeviceColor(job.deviceColor);
      setImeiOrSerial(job.imeiOrSerial);
      setSecurityLockCode(job.securityLockCode);
      setSelectedIssueTags(
        job.issueTagsCsv
          .split(',')
          .map((t) => t.trim())
          .filter(Boolean)
      );
      setIssueDescription(job.issueDescription);
      setEstimatedCost(String(job.estimatedCost));
      setAdvancePaid(String(job.advancePaid));
      setTechnicianNotes(job.technicianNotes);
      setChecklist(parseChecklist(job.checklistJson));
      setFormError(null);
    }
  }, [isOpen, job]);

  if (!isOpen) return null;

  const toggleIssueTag = (tag: string) => {
    setSelectedIssueTags((prev) =>
      prev.includes(tag) ? prev.filter((t) => t !== tag) : [...prev, tag]
    );
  };

  const setChecklistItemState = (idx: number, state: ChecklistState) => {
    setChecklist((prev) => {
      const next = [...prev];
      next[idx] = state;
      return next;
    });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);

    if (!customerName.trim()) {
      setFormError('Customer Name is required.');
      return;
    }
    if (!customerPhone.trim()) {
      setFormError('Customer Phone is required.');
      return;
    }
    if (!deviceModel.trim()) {
      setFormError('Device Model is required.');
      return;
    }
    if (!issueDescription.trim()) {
      setFormError('Issue Description is required.');
      return;
    }

    const estNum = Number(estimatedCost);
    const advNum = Number(advancePaid);
    if (!Number.isFinite(estNum) || estNum < 0) {
      setFormError('Estimated Cost must be a non-negative number.');
      return;
    }
    if (!Number.isFinite(advNum) || advNum < 0) {
      setFormError('Advance Paid must be a non-negative number.');
      return;
    }

    const resolvedBrand =
      selectedBrandChip === 'Other'
        ? customBrand.trim() || 'Other'
        : selectedBrandChip;

    const updated: RepairJob = {
      ...job,
      customerName: customerName.trim(),
      customerPhone: customerPhone.trim(),
      deviceBrand: resolvedBrand,
      deviceModel: deviceModel.trim(),
      deviceColor: deviceColor.trim(),
      imeiOrSerial: imeiOrSerial.trim(),
      securityLockCode: securityLockCode.trim(),
      issueDescription: issueDescription.trim(),
      issueTagsCsv: selectedIssueTags.join(', '),
      estimatedCost: estNum,
      advancePaid: advNum,
      technicianNotes: technicianNotes.trim(),
      checklistJson: serializeChecklist(checklist),
      updatedAt: Date.now(),
    };

    await onSaveEdit(updated);
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 p-3 sm:p-5 backdrop-blur-sm dark:bg-slate-950/80"
      role="dialog"
      aria-modal="true"
      aria-labelledby="edit-job-modal-title"
      onClick={(e) => {
        if (e.target === e.currentTarget && !isSaving) onClose();
      }}
    >
      <div className="flex max-h-[94vh] w-full max-w-6xl flex-col overflow-hidden rounded-2xl border border-slate-200 bg-slate-50 shadow-2xl dark:border-slate-700/80 dark:bg-[#0F172A]">
        <div className="flex items-center justify-between border-b border-slate-200 bg-white px-6 py-4 dark:border-slate-800 dark:bg-[#1E293B]">
          <div className="flex items-center gap-3">
            <h2 id="edit-job-modal-title" className="font-display text-lg font-bold text-slate-900 dark:text-slate-100">
              Edit Repair Job
            </h2>
            <span className="rounded-lg border border-blue-200 bg-blue-50 px-3 py-1 font-mono-tabular text-xs font-semibold text-blue-700 dark:border-blue-500/40 dark:bg-blue-600/20 dark:text-blue-200">
              {job.jobNumber}
            </span>
          </div>

          <button
            type="button"
            onClick={onClose}
            disabled={isSaving}
            className="rounded-lg p-2 text-slate-500 hover:bg-slate-100 hover:text-slate-900 dark:text-slate-400 dark:hover:bg-slate-800 dark:hover:text-slate-100"
            aria-label="Close edit job modal"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="flex flex-1 flex-col overflow-hidden">
          <div className="flex-1 overflow-y-auto px-6 py-5">
            {formError && (
              <div className="mb-5 flex items-center gap-2 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-xs font-medium text-red-700 dark:border-red-500/40 dark:bg-red-500/12 dark:text-red-200">
                <AlertCircle className="h-4 w-4 text-red-600 dark:text-red-400 shrink-0" />
                <span>{formError}</span>
              </div>
            )}

            <div className="grid grid-cols-1 gap-6 lg:grid-cols-12">
              <div className="space-y-5 lg:col-span-7">
                <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm dark:border-slate-800 dark:bg-[#1E293B] dark:shadow-none">
                  <div className="mb-3.5 flex items-center gap-2 text-xs font-semibold text-slate-700 dark:text-slate-300">
                    <User className="h-4 w-4 text-blue-600 dark:text-blue-400" />
                    <span>Customer Information</span>
                  </div>
                  <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                    <div>
                      <label className="mb-1.5 block text-xs font-medium text-slate-600 dark:text-slate-400">
                        Customer Name <span className="text-red-500 dark:text-red-400">*</span>
                      </label>
                      <input
                        type="text"
                        required
                        value={customerName}
                        onChange={(e) => setCustomerName(e.target.value)}
                        className="w-full rounded-xl border border-slate-300 bg-slate-50 px-3.5 py-2.5 text-sm text-slate-900 focus:border-blue-500 focus:bg-white focus:outline-none dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-100 dark:focus:bg-[#0F172A]"
                      />
                    </div>
                    <div>
                      <label className="mb-1.5 block text-xs font-medium text-slate-600 dark:text-slate-400">
                        Phone / WhatsApp <span className="text-red-500 dark:text-red-400">*</span>
                      </label>
                      <input
                        type="tel"
                        required
                        value={customerPhone}
                        onChange={(e) => setCustomerPhone(e.target.value)}
                        className="w-full rounded-xl border border-slate-300 bg-slate-50 px-3.5 py-2.5 font-mono-tabular text-sm text-slate-900 focus:border-blue-500 focus:bg-white focus:outline-none dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-100 dark:focus:bg-[#0F172A]"
                      />
                    </div>
                  </div>
                </section>

                <section className="space-y-4 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm dark:border-slate-800 dark:bg-[#1E293B] dark:shadow-none">
                  <div className="flex items-center gap-2 text-xs font-semibold text-slate-700 dark:text-slate-300">
                    <Smartphone className="h-4 w-4 text-blue-600 dark:text-blue-400" />
                    <span>Device Details</span>
                  </div>

                  <div className="flex flex-wrap gap-2">
                    {DEVICE_BRAND_CHIPS.map((brand) => (
                      <button
                        key={brand}
                        type="button"
                        onClick={() => setSelectedBrandChip(brand)}
                        className={`rounded-xl border px-3 py-1.5 text-xs font-medium transition-colors ${
                          selectedBrandChip === brand
                            ? 'border-blue-600 bg-[#2563EB] text-white dark:border-blue-500/60'
                            : 'border-slate-200 bg-slate-50 text-slate-700 hover:border-slate-300 dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-300 dark:hover:border-slate-600'
                        }`}
                      >
                        {brand}
                      </button>
                    ))}
                  </div>

                  {selectedBrandChip === 'Other' && (
                    <input
                      type="text"
                      value={customBrand}
                      onChange={(e) => setCustomBrand(e.target.value)}
                      placeholder="Custom brand name"
                      className="w-full rounded-xl border border-slate-300 bg-slate-50 px-3.5 py-2 text-sm text-slate-900 focus:border-blue-500 focus:bg-white focus:outline-none dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-100 dark:focus:bg-[#0F172A]"
                    />
                  )}

                  <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                    <div>
                      <label className="mb-1.5 block text-xs font-medium text-slate-600 dark:text-slate-400">
                        Device Model <span className="text-red-500 dark:text-red-400">*</span>
                      </label>
                      <input
                        type="text"
                        required
                        value={deviceModel}
                        onChange={(e) => setDeviceModel(e.target.value)}
                        className="w-full rounded-xl border border-slate-300 bg-slate-50 px-3.5 py-2.5 text-sm text-slate-900 focus:border-blue-500 focus:bg-white focus:outline-none dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-100 dark:focus:bg-[#0F172A]"
                      />
                    </div>
                    <div>
                      <label className="mb-1.5 block text-xs font-medium text-slate-600 dark:text-slate-400">
                        Color / Variant
                      </label>
                      <input
                        type="text"
                        value={deviceColor}
                        onChange={(e) => setDeviceColor(e.target.value)}
                        className="w-full rounded-xl border border-slate-300 bg-slate-50 px-3.5 py-2.5 text-sm text-slate-900 focus:border-blue-500 focus:bg-white focus:outline-none dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-100 dark:focus:bg-[#0F172A]"
                      />
                    </div>
                    <div>
                      <label className="mb-1.5 block text-xs font-medium text-slate-600 dark:text-slate-400">
                        IMEI / Serial
                      </label>
                      <input
                        type="text"
                        value={imeiOrSerial}
                        onChange={(e) => setImeiOrSerial(e.target.value)}
                        className="w-full rounded-xl border border-slate-300 bg-slate-50 px-3.5 py-2.5 font-mono-tabular text-sm text-slate-900 focus:border-blue-500 focus:bg-white focus:outline-none dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-100 dark:focus:bg-[#0F172A]"
                      />
                    </div>
                    <div>
                      <label className="mb-1.5 flex items-center gap-1.5 text-xs font-medium text-slate-600 dark:text-slate-400">
                        <Lock className="h-3 w-3" />
                        <span>Screen Lock</span>
                      </label>
                      <input
                        type="text"
                        value={securityLockCode}
                        onChange={(e) => setSecurityLockCode(e.target.value)}
                        className="w-full rounded-xl border border-slate-300 bg-slate-50 px-3.5 py-2.5 font-mono-tabular text-sm text-slate-900 focus:border-blue-500 focus:bg-white focus:outline-none dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-100 dark:focus:bg-[#0F172A]"
                      />
                    </div>
                  </div>
                </section>

                <section className="space-y-4 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm dark:border-slate-800 dark:bg-[#1E293B] dark:shadow-none">
                  <div className="flex items-center gap-2 text-xs font-semibold text-slate-700 dark:text-slate-300">
                    <Wrench className="h-4 w-4 text-amber-600 dark:text-amber-400" />
                    <span>Fault &amp; Complaint</span>
                  </div>
                  <div className="flex flex-wrap gap-2">
                    {ISSUE_TAG_OPTIONS.map((tag) => (
                      <button
                        key={tag}
                        type="button"
                        onClick={() => toggleIssueTag(tag)}
                        className={`rounded-xl border px-3 py-1.5 text-xs font-medium transition-colors ${
                          selectedIssueTags.includes(tag)
                            ? 'border-amber-300 bg-amber-50 text-amber-800 dark:border-amber-500/60 dark:bg-amber-500/20 dark:text-amber-200'
                            : 'border-slate-200 bg-slate-50 text-slate-700 hover:border-slate-300 dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-300 dark:hover:border-slate-600'
                        }`}
                      >
                        {tag}
                      </button>
                    ))}
                  </div>
                  <div>
                    <label className="mb-1.5 block text-xs font-medium text-slate-600 dark:text-slate-400">
                      Issue Description <span className="text-red-500 dark:text-red-400">*</span>
                    </label>
                    <textarea
                      required
                      rows={3}
                      value={issueDescription}
                      onChange={(e) => setIssueDescription(e.target.value)}
                      className="w-full rounded-xl border border-slate-300 bg-slate-50 px-3.5 py-2.5 text-sm text-slate-900 focus:border-blue-500 focus:bg-white focus:outline-none dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-100 dark:focus:bg-[#0F172A]"
                    />
                  </div>
                </section>

                <section className="space-y-4 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm dark:border-slate-800 dark:bg-[#1E293B] dark:shadow-none">
                  <div className="flex items-center gap-2 text-xs font-semibold text-slate-700 dark:text-slate-300">
                    <DollarSign className="h-4 w-4 text-emerald-600 dark:text-emerald-400" />
                    <span>Estimate, Advance &amp; Notes</span>
                  </div>
                  <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                    <div>
                      <label className="mb-1.5 block text-xs font-medium text-slate-600 dark:text-slate-400">
                        Estimated Cost ({currencySymbol})
                      </label>
                      <input
                        type="number"
                        min={0}
                        step="any"
                        value={estimatedCost}
                        onChange={(e) => setEstimatedCost(e.target.value)}
                        className="w-full rounded-xl border border-slate-300 bg-slate-50 px-3.5 py-2.5 font-mono-tabular text-sm text-slate-900 focus:border-blue-500 focus:bg-white focus:outline-none dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-100 dark:focus:bg-[#0F172A]"
                      />
                    </div>
                    <div>
                      <label className="mb-1.5 block text-xs font-medium text-slate-600 dark:text-slate-400">
                        Advance Paid ({currencySymbol})
                      </label>
                      <input
                        type="number"
                        min={0}
                        step="any"
                        value={advancePaid}
                        onChange={(e) => setAdvancePaid(e.target.value)}
                        className="w-full rounded-xl border border-slate-300 bg-slate-50 px-3.5 py-2.5 font-mono-tabular text-sm text-slate-900 focus:border-blue-500 focus:bg-white focus:outline-none dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-100 dark:focus:bg-[#0F172A]"
                      />
                    </div>
                  </div>
                  <div>
                    <label className="mb-1.5 block text-xs font-medium text-slate-600 dark:text-slate-400">
                      Technician Notes
                    </label>
                    <textarea
                      rows={2}
                      value={technicianNotes}
                      onChange={(e) => setTechnicianNotes(e.target.value)}
                      className="w-full rounded-xl border border-slate-300 bg-slate-50 px-3.5 py-2.5 text-sm text-slate-900 focus:border-blue-500 focus:bg-white focus:outline-none dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-100 dark:focus:bg-[#0F172A]"
                    />
                  </div>
                </section>
              </div>

              {/* Right Column: Editable 12-Point Checklist */}
              <div className="lg:col-span-5">
                <section className="flex h-full flex-col rounded-2xl border border-slate-200 bg-white p-5 shadow-sm dark:border-slate-800 dark:bg-[#1E293B] dark:shadow-none">
                  <div className="flex items-center justify-between gap-2 border-b border-slate-200 pb-3.5 dark:border-slate-800">
                    <div className="flex items-center gap-2 text-xs font-semibold text-slate-800 dark:text-slate-200">
                      <ClipboardCheck className="h-4 w-4 text-emerald-600 dark:text-emerald-400" />
                      <span>12-Point Inspection Checklist</span>
                    </div>
                    <div className="flex items-center gap-1.5">
                      <button
                        type="button"
                        onClick={() =>
                          setChecklist(
                            Array<ChecklistState>(CHECKLIST_ITEMS.length).fill('WORKING')
                          )
                        }
                        className="rounded-lg border border-emerald-200 bg-emerald-50 px-2.5 py-1 text-[11px] font-medium text-emerald-700 dark:border-emerald-500/35 dark:bg-emerald-500/10 dark:text-emerald-300"
                      >
                        All OK
                      </button>
                      <button
                        type="button"
                        onClick={() => setChecklist(createDefaultChecklist())}
                        className="rounded-lg border border-slate-200 bg-slate-50 px-2.5 py-1 text-[11px] font-medium text-slate-700 dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-300"
                      >
                        Reset
                      </button>
                    </div>
                  </div>

                  <div className="mt-3.5 flex-1 space-y-2">
                    {CHECKLIST_ITEMS.map((itemLabel, idx) => {
                      const state = checklist[idx] ?? 'UNTESTED';
                      return (
                        <div
                          key={itemLabel}
                          className="flex items-center justify-between gap-3 rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 dark:border-slate-800 dark:bg-[#0F172A]/80"
                        >
                          <div className="flex items-center gap-2.5 min-w-0">
                            <span className="font-mono-tabular text-[11px] text-slate-400 dark:text-slate-500 w-5 shrink-0">
                              {String(idx + 1).padStart(2, '0')}
                            </span>
                            <span className="text-xs font-medium text-slate-800 dark:text-slate-200 truncate">
                              {itemLabel}
                            </span>
                          </div>
                          <div className="inline-flex items-center rounded-lg border border-slate-200 bg-white p-0.5 shrink-0 dark:border-slate-800 dark:bg-[#1E293B]">
                            <button
                              type="button"
                              onClick={() => setChecklistItemState(idx, 'WORKING')}
                              className={`rounded-md px-2 py-1 text-[11px] font-semibold ${
                                state === 'WORKING'
                                  ? 'bg-emerald-600 text-white'
                                  : 'text-slate-500 hover:text-emerald-700 dark:text-slate-400 dark:hover:text-emerald-300'
                              }`}
                            >
                              OK
                            </button>
                            <button
                              type="button"
                              onClick={() => setChecklistItemState(idx, 'DEFECTIVE')}
                              className={`rounded-md px-2 py-1 text-[11px] font-semibold ${
                                state === 'DEFECTIVE'
                                  ? 'bg-red-600 text-white'
                                  : 'text-slate-500 hover:text-red-700 dark:text-slate-400 dark:hover:text-red-300'
                              }`}
                            >
                              Issue
                            </button>
                            <button
                              type="button"
                              onClick={() => setChecklistItemState(idx, 'UNTESTED')}
                              className={`rounded-md px-2 py-1 text-[11px] font-semibold ${
                                state === 'UNTESTED'
                                  ? 'bg-slate-600 text-white'
                                  : 'text-slate-500 hover:text-slate-900 dark:text-slate-400 dark:hover:text-slate-200'
                              }`}
                            >
                              N/A
                            </button>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </section>
              </div>
            </div>
          </div>

          <div className="flex items-center justify-end gap-3 border-t border-slate-200 bg-white px-6 py-4 dark:border-slate-800 dark:bg-[#1E293B]">
            <button
              type="button"
              onClick={onClose}
              disabled={isSaving}
              className="rounded-xl border border-slate-300 bg-white px-4 py-2.5 text-xs font-medium text-slate-700 hover:bg-slate-50 dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-200 dark:hover:bg-slate-800"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={isSaving}
              className="inline-flex items-center gap-2 rounded-xl bg-[#2563EB] px-5 py-2.5 text-xs font-semibold text-white hover:bg-blue-500 disabled:opacity-50"
            >
              <CheckCircle2 className="h-4 w-4" />
              <span>{isSaving ? 'Saving...' : 'Save Changes'}</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
