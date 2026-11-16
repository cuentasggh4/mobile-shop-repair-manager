import { useEffect, useMemo, useRef, useState } from 'react';
import {
  AlertTriangle,
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
  findMatchingKhataCustomer,
  KhataCustomerSummary,
} from '../services/khataEngine';
import {
  CHECKLIST_ITEMS,
  ChecklistState,
  createDefaultChecklist,
  DEVICE_BRAND_CHIPS,
  digitsOnly,
  generateNextJobNumber,
  GeneratedJobNumberInfo,
  ISSUE_TAG_OPTIONS,
  JobStage,
  RepairJob,
  serializeChecklist,
  ShopProfile,
} from '../types/repairShop';

interface NewJobModalProps {
  isOpen: boolean;
  shopProfile: ShopProfile | null;
  existingJobs: RepairJob[];
  khataCustomers: KhataCustomerSummary[];
  initialCustomer?: { name: string; phone: string } | null;
  isConnected: boolean;
  isSubmitting: boolean;
  onClose: () => void;
  onOpenDriveConnect: () => void;
  onSubmitNewJob: (
    job: RepairJob,
    jobNumberInfo: GeneratedJobNumberInfo
  ) => Promise<void>;
}

export function NewJobModal({
  isOpen,
  shopProfile,
  existingJobs,
  khataCustomers,
  initialCustomer = null,
  isConnected,
  isSubmitting,
  onClose,
  onOpenDriveConnect,
  onSubmitNewJob,
}: NewJobModalProps) {
  const nameInputRef = useRef<HTMLInputElement | null>(null);
  const autocompleteContainerRef = useRef<HTMLDivElement | null>(null);

  const [customerName, setCustomerName] = useState('');
  const [customerPhone, setCustomerPhone] = useState('');
  const [showSuggestions, setShowSuggestions] = useState(false);
  const [selectedBrandChip, setSelectedBrandChip] = useState<string>('Apple');
  const [customBrand, setCustomBrand] = useState('');
  const [deviceModel, setDeviceModel] = useState('');
  const [deviceColor, setDeviceColor] = useState('');
  const [imeiOrSerial, setImeiOrSerial] = useState('');
  const [securityLockCode, setSecurityLockCode] = useState('');
  const [selectedIssueTags, setSelectedIssueTags] = useState<string[]>([]);
  const [issueDescription, setIssueDescription] = useState('');
  const [estimatedCost, setEstimatedCost] = useState<string>('0');
  const [advancePaid, setAdvancePaid] = useState<string>('0');
  const [technicianNotes, setTechnicianNotes] = useState('');
  const [checklist, setChecklist] = useState<ChecklistState[]>(() =>
    createDefaultChecklist()
  );
  const [formError, setFormError] = useState<string | null>(null);

  // Reset form cleanly each time the modal opens
  useEffect(() => {
    if (isOpen) {
      setCustomerName(initialCustomer?.name ?? '');
      setCustomerPhone(initialCustomer?.phone ?? '');
      setShowSuggestions(false);
      setSelectedBrandChip('Apple');
      setCustomBrand('');
      setDeviceModel('');
      setDeviceColor('');
      setImeiOrSerial('');
      setSecurityLockCode('');
      setSelectedIssueTags([]);
      setIssueDescription('');
      setEstimatedCost('0');
      setAdvancePaid('0');
      setTechnicianNotes('');
      setChecklist(createDefaultChecklist());
      setFormError(null);

      const timer = window.setTimeout(() => {
        nameInputRef.current?.focus();
      }, 60);
      return () => window.clearTimeout(timer);
    }
  }, [isOpen, initialCustomer]);

  // Close autocomplete dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (
        autocompleteContainerRef.current &&
        !autocompleteContainerRef.current.contains(e.target as Node)
      ) {
        setShowSuggestions(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const jobNumberInfo = useMemo(
    () => generateNextJobNumber(shopProfile, existingJobs),
    [shopProfile, existingJobs]
  );

  const currencySymbol = shopProfile?.currencySymbol || '₹';

  const parsedEstimate = Math.max(0, Number(estimatedCost) || 0);
  const parsedAdvance = Math.max(0, Number(advancePaid) || 0);
  const estimatedBalance = Math.max(0, parsedEstimate - parsedAdvance);

  const checklistStats = useMemo(() => {
    let working = 0;
    let defective = 0;
    let untested = 0;
    for (const s of checklist) {
      if (s === 'WORKING') working += 1;
      else if (s === 'DEFECTIVE') defective += 1;
      else untested += 1;
    }
    return { working, defective, untested };
  }, [checklist]);

  const customerSuggestions = useMemo(() => {
    const query = customerName.trim().toLowerCase();
    const queryDigits = digitsOnly(customerName);
    if (!query) return [];

    return khataCustomers
      .filter((c) => {
        if (c.customerName.toLowerCase().includes(query)) return true;
        if (c.customerPhone.toLowerCase().includes(query)) return true;
        if (queryDigits.length >= 2 && digitsOnly(c.customerPhone).includes(queryDigits)) {
          return true;
        }
        return false;
      })
      .slice(0, 5);
  }, [customerName, khataCustomers]);

  const matchedKhataCustomer = useMemo(
    () => findMatchingKhataCustomer(khataCustomers, customerName, customerPhone),
    [khataCustomers, customerName, customerPhone]
  );

  if (!isOpen) return null;

  const handleSelectCustomerSuggestion = (customer: KhataCustomerSummary) => {
    setCustomerName(customer.customerName);
    setCustomerPhone(customer.customerPhone);
    setShowSuggestions(false);
  };

  const toggleIssueTag = (tag: string) => {
    setSelectedIssueTags((prev) =>
      prev.includes(tag) ? prev.filter((t) => t !== tag) : [...prev, tag]
    );
  };

  const setChecklistItemState = (index: number, state: ChecklistState) => {
    setChecklist((prev) => {
      const next = [...prev];
      next[index] = state;
      return next;
    });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);

    const trimmedName = customerName.trim();
    const trimmedPhone = customerPhone.trim();
    const resolvedBrand =
      selectedBrandChip === 'Other'
        ? customBrand.trim() || 'Other'
        : selectedBrandChip;
    const trimmedModel = deviceModel.trim();
    const trimmedIssue = issueDescription.trim();

    if (!trimmedName) {
      setFormError('Customer Name is required.');
      return;
    }
    if (!trimmedPhone) {
      setFormError('Customer Phone is required.');
      return;
    }
    if (!trimmedModel) {
      setFormError('Device Model is required.');
      return;
    }
    if (!trimmedIssue) {
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

    const now = Date.now();
    const newJob: RepairJob = {
      jobNumber: jobNumberInfo.jobNumber,
      customerName: trimmedName,
      customerPhone: trimmedPhone,
      deviceBrand: resolvedBrand,
      deviceModel: trimmedModel,
      deviceColor: deviceColor.trim(),
      imeiOrSerial: imeiOrSerial.trim(),
      securityLockCode: securityLockCode.trim(),
      issueDescription: trimmedIssue,
      issueTagsCsv: selectedIssueTags.join(', '),
      stage: JobStage.DIAGNOSTIC,
      estimatedCost: estNum,
      advancePaid: advNum,
      discount: 0,
      finalCost: 0,
      isPaid: false,
      paymentMode: 'Cash',
      technicianNotes: technicianNotes.trim(),
      pickedUpByOriginalCustomer: true,
      recipientName: '',
      recipientPhone: '',
      checklistJson: serializeChecklist(checklist),
      statusLogsJson: JSON.stringify([
        {
          stage: JobStage.DIAGNOSTIC,
          timestamp: now,
          note: 'Job intake registered',
        },
      ]),
      createdAt: now,
      updatedAt: now,
      deliveredAt: null,
      isDeleted: false,
      recordUuid: crypto.randomUUID(),
    };

    await onSubmitNewJob(newJob, jobNumberInfo);
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 p-3 sm:p-5 backdrop-blur-sm dark:bg-slate-950/80"
      role="dialog"
      aria-modal="true"
      aria-labelledby="new-job-intake-title"
      onClick={(e) => {
        if (e.target === e.currentTarget && !isSubmitting) onClose();
      }}
    >
      <div className="flex max-h-[94vh] w-full max-w-6xl flex-col overflow-hidden rounded-2xl border border-slate-200 bg-slate-50 shadow-2xl dark:border-slate-700/80 dark:bg-[#0F172A]">
        {/* Modal Top Header */}
        <div className="flex flex-wrap items-center justify-between gap-4 border-b border-slate-200 bg-white px-6 py-4 dark:border-slate-800 dark:bg-[#1E293B]">
          <div className="flex flex-wrap items-center gap-3">
            <h2
              id="new-job-intake-title"
              className="font-display text-lg font-bold text-slate-900 dark:text-slate-100"
            >
              New Repair Job Intake
            </h2>
            <span className="inline-flex items-center rounded-lg border border-blue-200 bg-blue-50 px-3 py-1 font-mono-tabular text-xs font-semibold text-blue-700 dark:border-blue-500/40 dark:bg-blue-600/20 dark:text-blue-200">
              Next Job ID: {jobNumberInfo.jobNumber}
            </span>
          </div>

          <button
            type="button"
            onClick={onClose}
            disabled={isSubmitting}
            className="rounded-lg p-2 text-slate-500 transition-colors hover:bg-slate-100 hover:text-slate-900 disabled:opacity-50 dark:text-slate-400 dark:hover:bg-slate-800 dark:hover:text-slate-100"
            aria-label="Close intake modal"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Scrollable Form Body */}
        <form onSubmit={handleSubmit} className="flex flex-1 flex-col overflow-hidden">
          <div className="flex-1 overflow-y-auto px-6 py-5">
            {!isConnected && (
              <div className="mb-5 flex flex-wrap items-center justify-between gap-3 rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-xs text-amber-800 dark:border-amber-500/35 dark:bg-amber-500/10 dark:text-amber-200">
                <div className="flex items-center gap-2">
                  <AlertCircle className="h-4 w-4 text-amber-600 dark:text-amber-400 shrink-0" />
                  <span>
                    Google Drive is currently disconnected. Submitting will queue this job in local
                    cache and sync automatically when connected.
                  </span>
                </div>
                <button
                  type="button"
                  onClick={() => {
                    onClose();
                    onOpenDriveConnect();
                  }}
                  className="rounded-lg border border-amber-300 bg-amber-100 px-3 py-1 font-semibold text-amber-900 hover:bg-amber-200 whitespace-nowrap dark:border-amber-400/40 dark:bg-amber-500/15 dark:text-amber-100 dark:hover:bg-amber-500/25"
                >
                  Connect Drive
                </button>
              </div>
            )}

            {formError && (
              <div className="mb-5 flex items-center gap-2 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-xs font-medium text-red-700 dark:border-red-500/40 dark:bg-red-500/12 dark:text-red-200">
                <AlertCircle className="h-4 w-4 text-red-600 dark:text-red-400 shrink-0" />
                <span>{formError}</span>
              </div>
            )}

            <div className="grid grid-cols-1 gap-6 lg:grid-cols-12">
              {/* LEFT COLUMN: Customer, Device, Fault, Financials */}
              <div className="space-y-5 lg:col-span-7">
                {/* 1. Customer Info */}
                <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm dark:border-slate-800 dark:bg-[#1E293B] dark:shadow-none">
                  <div className="mb-3.5 flex items-center gap-2 text-xs font-semibold text-slate-700 dark:text-slate-300">
                    <User className="h-4 w-4 text-blue-600 dark:text-blue-400" />
                    <span>Customer Information</span>
                  </div>
                  <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                    <div ref={autocompleteContainerRef} className="relative">
                      <label
                        htmlFor="intake-customer-name"
                        className="mb-1.5 block text-xs font-medium text-slate-600 dark:text-slate-400"
                      >
                        Customer Name <span className="text-red-500 dark:text-red-400">*</span>
                      </label>
                      <input
                        id="intake-customer-name"
                        ref={nameInputRef}
                        type="text"
                        required
                        autoComplete="off"
                        value={customerName}
                        onFocus={() => {
                          if (customerName.trim().length > 0) setShowSuggestions(true);
                        }}
                        onChange={(e) => {
                          setCustomerName(e.target.value);
                          setShowSuggestions(e.target.value.trim().length > 0);
                        }}
                        onKeyDown={(e) => {
                          if (e.key === 'Escape' && showSuggestions) {
                            e.stopPropagation();
                            setShowSuggestions(false);
                          }
                        }}
                        placeholder="Customer full name"
                        className="w-full rounded-xl border border-slate-300 bg-slate-50 px-3.5 py-2.5 text-sm text-slate-900 placeholder:text-slate-400 focus:border-blue-500 focus:bg-white focus:outline-none dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-100 dark:placeholder:text-slate-500 dark:focus:bg-[#0F172A]"
                      />

                      {showSuggestions && customerSuggestions.length > 0 && (
                        <div
                          role="listbox"
                          aria-label="Matching repeat customers"
                          className="absolute left-0 right-0 top-full z-30 mt-1.5 overflow-hidden rounded-xl border border-slate-200 bg-white shadow-2xl dark:border-slate-700 dark:bg-[#0F172A]"
                        >
                          <div className="border-b border-slate-200 px-3 py-1.5 text-[10px] font-semibold uppercase tracking-wider text-slate-500 dark:border-slate-800 dark:text-slate-400">
                            Repeat Customers ({customerSuggestions.length})
                          </div>
                          <div className="divide-y divide-slate-200 dark:divide-slate-800/80">
                            {customerSuggestions.map((customer) => {
                              const hasDue = customer.totalPendingDue > 0.01;
                              return (
                                <button
                                  key={customer.customerKey}
                                  type="button"
                                  onMouseDown={(e) => {
                                    e.preventDefault();
                                    handleSelectCustomerSuggestion(customer);
                                  }}
                                  className="flex w-full items-center justify-between gap-2 px-3 py-2.5 text-left transition-colors hover:bg-slate-50 dark:hover:bg-[#1E293B]"
                                >
                                  <div className="min-w-0">
                                    <div className="truncate text-xs font-semibold text-slate-900 dark:text-slate-100">
                                      {customer.customerName}
                                    </div>
                                    <div className="mt-0.5 font-mono-tabular text-[11px] text-slate-500 dark:text-slate-400">
                                      {customer.customerPhone || 'No phone'} ·{' '}
                                      {customer.totalJobsCount}{' '}
                                      {customer.totalJobsCount === 1 ? 'visit' : 'visits'}
                                    </div>
                                  </div>
                                  {hasDue ? (
                                    <span className="shrink-0 rounded-md border border-red-200 bg-red-50 px-2 py-0.5 font-mono-tabular text-[11px] font-semibold text-red-700 dark:border-red-500/40 dark:bg-red-500/15 dark:text-red-300">
                                      Due: {currencySymbol}
                                      {customer.totalPendingDue.toLocaleString()}
                                    </span>
                                  ) : (
                                    <span className="shrink-0 rounded-md border border-emerald-200 bg-emerald-50 px-2 py-0.5 text-[11px] font-semibold text-emerald-700 dark:border-emerald-500/35 dark:bg-emerald-500/15 dark:text-emerald-300">
                                      Settled
                                    </span>
                                  )}
                                </button>
                              );
                            })}
                          </div>
                        </div>
                      )}
                    </div>
                    <div>
                      <label
                        htmlFor="intake-customer-phone"
                        className="mb-1.5 block text-xs font-medium text-slate-600 dark:text-slate-400"
                      >
                        Phone / WhatsApp <span className="text-red-500 dark:text-red-400">*</span>
                      </label>
                      <input
                        id="intake-customer-phone"
                        type="tel"
                        required
                        value={customerPhone}
                        onChange={(e) => setCustomerPhone(e.target.value)}
                        placeholder="10-digit mobile number"
                        className="w-full rounded-xl border border-slate-300 bg-slate-50 px-3.5 py-2.5 font-mono-tabular text-sm text-slate-900 placeholder:text-slate-400 focus:border-blue-500 focus:bg-white focus:outline-none dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-100 dark:placeholder:text-slate-500 dark:focus:bg-[#0F172A]"
                      />
                    </div>
                  </div>

                  {matchedKhataCustomer && matchedKhataCustomer.totalPendingDue > 0.01 && (
                    <div
                      role="alert"
                      className="mt-3 flex items-center gap-2.5 rounded-xl border border-amber-200 bg-amber-50 px-3.5 py-2.5 text-xs font-semibold text-amber-800 dark:border-amber-500/45 dark:bg-amber-500/15 dark:text-amber-200"
                    >
                      <AlertTriangle className="h-4 w-4 text-amber-600 dark:text-amber-400 shrink-0" />
                      <span>
                        Previous Khata Due: {currencySymbol}
                        {matchedKhataCustomer.totalPendingDue.toFixed(2)} (
                        {matchedKhataCustomer.unpaidJobsCount} unpaid job(s))
                      </span>
                    </div>
                  )}
                </section>

                {/* 2. Device Info */}
                <section className="space-y-4 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm dark:border-slate-800 dark:bg-[#1E293B] dark:shadow-none">
                  <div className="flex items-center gap-2 text-xs font-semibold text-slate-700 dark:text-slate-300">
                    <Smartphone className="h-4 w-4 text-blue-600 dark:text-blue-400" />
                    <span>Device Details</span>
                  </div>

                  <div>
                    <span className="mb-2 block text-xs font-medium text-slate-600 dark:text-slate-400">
                      Device Brand
                    </span>
                    <div className="flex flex-wrap gap-2">
                      {DEVICE_BRAND_CHIPS.map((brand) => {
                        const active = selectedBrandChip === brand;
                        return (
                          <button
                            key={brand}
                            type="button"
                            onClick={() => setSelectedBrandChip(brand)}
                            className={`rounded-xl border px-3 py-1.5 text-xs font-medium transition-colors whitespace-nowrap ${
                              active
                                ? 'border-blue-600 bg-[#2563EB] text-white dark:border-blue-500/60'
                                : 'border-slate-200 bg-slate-50 text-slate-700 hover:border-slate-300 hover:text-slate-900 dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-300 dark:hover:border-slate-600 dark:hover:text-white'
                            }`}
                          >
                            {brand}
                          </button>
                        );
                      })}
                    </div>

                    {selectedBrandChip === 'Other' && (
                      <input
                        type="text"
                        value={customBrand}
                        onChange={(e) => setCustomBrand(e.target.value)}
                        placeholder="Enter brand name"
                        className="mt-2.5 w-full rounded-xl border border-slate-300 bg-slate-50 px-3.5 py-2 text-sm text-slate-900 placeholder:text-slate-400 focus:border-blue-500 focus:bg-white focus:outline-none dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-100 dark:placeholder:text-slate-500 dark:focus:bg-[#0F172A]"
                      />
                    )}
                  </div>

                  <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                    <div>
                      <label
                        htmlFor="intake-device-model"
                        className="mb-1.5 block text-xs font-medium text-slate-600 dark:text-slate-400"
                      >
                        Device Model <span className="text-red-500 dark:text-red-400">*</span>
                      </label>
                      <input
                        id="intake-device-model"
                        type="text"
                        required
                        value={deviceModel}
                        onChange={(e) => setDeviceModel(e.target.value)}
                        placeholder="Model name or number"
                        className="w-full rounded-xl border border-slate-300 bg-slate-50 px-3.5 py-2.5 text-sm text-slate-900 placeholder:text-slate-400 focus:border-blue-500 focus:bg-white focus:outline-none dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-100 dark:placeholder:text-slate-500 dark:focus:bg-[#0F172A]"
                      />
                    </div>
                    <div>
                      <label
                        htmlFor="intake-device-color"
                        className="mb-1.5 block text-xs font-medium text-slate-600 dark:text-slate-400"
                      >
                        Color / Variant
                      </label>
                      <input
                        id="intake-device-color"
                        type="text"
                        value={deviceColor}
                        onChange={(e) => setDeviceColor(e.target.value)}
                        placeholder="Color or storage variant"
                        className="w-full rounded-xl border border-slate-300 bg-slate-50 px-3.5 py-2.5 text-sm text-slate-900 placeholder:text-slate-400 focus:border-blue-500 focus:bg-white focus:outline-none dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-100 dark:placeholder:text-slate-500 dark:focus:bg-[#0F172A]"
                      />
                    </div>
                    <div>
                      <label
                        htmlFor="intake-imei"
                        className="mb-1.5 block text-xs font-medium text-slate-600 dark:text-slate-400"
                      >
                        IMEI / Serial Number
                      </label>
                      <input
                        id="intake-imei"
                        type="text"
                        value={imeiOrSerial}
                        onChange={(e) => setImeiOrSerial(e.target.value)}
                        placeholder="15-digit IMEI or Serial"
                        className="w-full rounded-xl border border-slate-300 bg-slate-50 px-3.5 py-2.5 font-mono-tabular text-sm text-slate-900 placeholder:text-slate-400 focus:border-blue-500 focus:bg-white focus:outline-none dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-100 dark:placeholder:text-slate-500 dark:focus:bg-[#0F172A]"
                      />
                    </div>
                    <div>
                      <label
                        htmlFor="intake-lock-code"
                        className="mb-1.5 flex items-center gap-1.5 text-xs font-medium text-slate-600 dark:text-slate-400"
                      >
                        <Lock className="h-3 w-3" />
                        <span>Screen Lock (PIN / Pattern)</span>
                      </label>
                      <input
                        id="intake-lock-code"
                        type="text"
                        value={securityLockCode}
                        onChange={(e) => setSecurityLockCode(e.target.value)}
                        placeholder="PIN, password, or pattern"
                        className="w-full rounded-xl border border-slate-300 bg-slate-50 px-3.5 py-2.5 font-mono-tabular text-sm text-slate-900 placeholder:text-slate-400 focus:border-blue-500 focus:bg-white focus:outline-none dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-100 dark:placeholder:text-slate-500 dark:focus:bg-[#0F172A]"
                      />
                    </div>
                  </div>
                </section>

                {/* 3. Fault & Complaint */}
                <section className="space-y-4 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm dark:border-slate-800 dark:bg-[#1E293B] dark:shadow-none">
                  <div className="flex items-center gap-2 text-xs font-semibold text-slate-700 dark:text-slate-300">
                    <Wrench className="h-4 w-4 text-amber-600 dark:text-amber-400" />
                    <span>Fault &amp; Complaint</span>
                  </div>

                  <div>
                    <span className="mb-2 block text-xs font-medium text-slate-600 dark:text-slate-400">
                      Quick Issue Tags (multi-select)
                    </span>
                    <div className="flex flex-wrap gap-2">
                      {ISSUE_TAG_OPTIONS.map((tag) => {
                        const active = selectedIssueTags.includes(tag);
                        return (
                          <button
                            key={tag}
                            type="button"
                            onClick={() => toggleIssueTag(tag)}
                            className={`rounded-xl border px-3 py-1.5 text-xs font-medium transition-colors whitespace-nowrap ${
                              active
                                ? 'border-amber-300 bg-amber-50 text-amber-800 dark:border-amber-500/60 dark:bg-amber-500/20 dark:text-amber-200'
                                : 'border-slate-200 bg-slate-50 text-slate-700 hover:border-slate-300 hover:text-slate-900 dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-300 dark:hover:border-slate-600 dark:hover:text-white'
                            }`}
                          >
                            {tag}
                          </button>
                        );
                      })}
                    </div>
                  </div>

                  <div>
                    <label
                      htmlFor="intake-issue-desc"
                      className="mb-1.5 block text-xs font-medium text-slate-600 dark:text-slate-400"
                    >
                      Issue Description <span className="text-red-500 dark:text-red-400">*</span>
                    </label>
                    <textarea
                      id="intake-issue-desc"
                      required
                      rows={3}
                      value={issueDescription}
                      onChange={(e) => setIssueDescription(e.target.value)}
                      placeholder="Describe customer complaint, visible damage, or requested repair..."
                      className="w-full rounded-xl border border-slate-300 bg-slate-50 px-3.5 py-2.5 text-sm text-slate-900 placeholder:text-slate-400 focus:border-blue-500 focus:bg-white focus:outline-none dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-100 dark:placeholder:text-slate-500 dark:focus:bg-[#0F172A]"
                    />
                  </div>
                </section>

                {/* 4. Estimate & Advance */}
                <section className="space-y-4 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm dark:border-slate-800 dark:bg-[#1E293B] dark:shadow-none">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2 text-xs font-semibold text-slate-700 dark:text-slate-300">
                      <DollarSign className="h-4 w-4 text-emerald-600 dark:text-emerald-400" />
                      <span>Estimate &amp; Advance</span>
                    </div>
                    <span className="font-mono-tabular text-xs text-slate-500 dark:text-slate-400">
                      Est. Balance:{' '}
                      <strong className="text-amber-700 dark:text-amber-300">
                        {currencySymbol}
                        {estimatedBalance.toLocaleString()}
                      </strong>
                    </span>
                  </div>

                  <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                    <div>
                      <label
                        htmlFor="intake-est-cost"
                        className="mb-1.5 block text-xs font-medium text-slate-600 dark:text-slate-400"
                      >
                        Estimated Cost ({currencySymbol})
                      </label>
                      <input
                        id="intake-est-cost"
                        type="number"
                        min={0}
                        step="any"
                        value={estimatedCost}
                        onChange={(e) => setEstimatedCost(e.target.value)}
                        className="w-full rounded-xl border border-slate-300 bg-slate-50 px-3.5 py-2.5 font-mono-tabular text-sm text-slate-900 focus:border-blue-500 focus:bg-white focus:outline-none dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-100 dark:focus:bg-[#0F172A]"
                      />
                    </div>
                    <div>
                      <label
                        htmlFor="intake-adv-paid"
                        className="mb-1.5 block text-xs font-medium text-slate-600 dark:text-slate-400"
                      >
                        Advance Paid ({currencySymbol})
                      </label>
                      <input
                        id="intake-adv-paid"
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
                    <label
                      htmlFor="intake-tech-notes"
                      className="mb-1.5 block text-xs font-medium text-slate-600 dark:text-slate-400"
                    >
                      Technician Notes (Internal)
                    </label>
                    <input
                      id="intake-tech-notes"
                      type="text"
                      value={technicianNotes}
                      onChange={(e) => setTechnicianNotes(e.target.value)}
                      placeholder="Internal repair notes or counter remarks"
                      className="w-full rounded-xl border border-slate-300 bg-slate-50 px-3.5 py-2.5 text-sm text-slate-900 placeholder:text-slate-400 focus:border-blue-500 focus:bg-white focus:outline-none dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-100 dark:placeholder:text-slate-500 dark:focus:bg-[#0F172A]"
                    />
                  </div>
                </section>
              </div>

              {/* RIGHT COLUMN: 12-Point Pre-Repair Inspection Checklist */}
              <div className="lg:col-span-5">
                <section className="flex h-full flex-col rounded-2xl border border-slate-200 bg-white p-5 shadow-sm dark:border-slate-800 dark:bg-[#1E293B] dark:shadow-none">
                  <div className="flex flex-wrap items-center justify-between gap-2 border-b border-slate-200 pb-3.5 dark:border-slate-800">
                    <div className="flex items-center gap-2 text-xs font-semibold text-slate-800 dark:text-slate-200">
                      <ClipboardCheck className="h-4 w-4 text-emerald-600 dark:text-emerald-400" />
                      <span>12-Point Pre-Repair Checklist</span>
                    </div>

                    <div className="flex items-center gap-1.5">
                      <button
                        type="button"
                        onClick={() =>
                          setChecklist(
                            Array<ChecklistState>(CHECKLIST_ITEMS.length).fill('WORKING')
                          )
                        }
                        className="rounded-lg border border-emerald-200 bg-emerald-50 px-2.5 py-1 text-[11px] font-medium text-emerald-700 hover:bg-emerald-100 dark:border-emerald-500/35 dark:bg-emerald-500/10 dark:text-emerald-300 dark:hover:bg-emerald-500/20"
                      >
                        All OK
                      </button>
                      <button
                        type="button"
                        onClick={() => setChecklist(createDefaultChecklist())}
                        className="rounded-lg border border-slate-200 bg-slate-50 px-2.5 py-1 text-[11px] font-medium text-slate-700 hover:text-slate-900 dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-300 dark:hover:text-white"
                      >
                        Reset N/A
                      </button>
                    </div>
                  </div>

                  <div className="mt-3 flex items-center justify-between text-xs text-slate-500 dark:text-slate-400">
                    <span>Android-compatible semicolon format</span>
                    <span className="font-mono-tabular">
                      <span className="text-emerald-700 dark:text-emerald-400">{checklistStats.working} OK</span>
                      {' · '}
                      <span className="text-red-700 dark:text-red-400">{checklistStats.defective} Issue</span>
                      {' · '}
                      <span>{checklistStats.untested} N/A</span>
                    </span>
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

                          <div
                            className="inline-flex items-center rounded-lg border border-slate-200 bg-white p-0.5 shrink-0 dark:border-slate-800 dark:bg-[#1E293B]"
                            role="group"
                            aria-label={`${itemLabel} inspection status`}
                          >
                            <button
                              type="button"
                              onClick={() => setChecklistItemState(idx, 'WORKING')}
                              className={`rounded-md px-2 py-1 text-[11px] font-semibold transition-colors ${
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
                              className={`rounded-md px-2 py-1 text-[11px] font-semibold transition-colors ${
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
                              className={`rounded-md px-2 py-1 text-[11px] font-semibold transition-colors ${
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

                  <div className="mt-4 rounded-xl border border-slate-200 bg-slate-50 p-3 dark:border-slate-800 dark:bg-[#0F172A]">
                    <p className="text-[11px] font-medium text-slate-500 dark:text-slate-400">
                      Serialized ChecklistJson Preview
                    </p>
                    <p className="mt-1 break-all font-mono-tabular text-[11px] text-slate-700 dark:text-slate-300">
                      {serializeChecklist(checklist)}
                    </p>
                  </div>
                </section>
              </div>
            </div>
          </div>

          {/* Modal Footer */}
          <div className="flex flex-wrap items-center justify-between gap-3 border-t border-slate-200 bg-white px-6 py-4 dark:border-slate-800 dark:bg-[#1E293B]">
            <div className="font-mono-tabular text-xs text-slate-500 dark:text-slate-400">
              Stage: <span className="font-semibold text-blue-700 dark:text-blue-300">DIAGNOSTIC</span> · Payment:{' '}
              <span className="text-slate-800 dark:text-slate-200">Cash</span> · Seq:{' '}
              <span className="text-slate-800 dark:text-slate-200">
                #{jobNumberInfo.sequence} ({jobNumberInfo.currentMonth})
              </span>
            </div>

            <div className="flex items-center gap-3">
              <button
                type="button"
                onClick={onClose}
                disabled={isSubmitting}
                className="rounded-xl border border-slate-300 bg-white px-4 py-2.5 text-xs font-medium text-slate-700 transition-colors hover:bg-slate-50 disabled:opacity-50 dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-200 dark:hover:bg-slate-800"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={isSubmitting}
                className="inline-flex items-center gap-2 rounded-xl bg-[#2563EB] px-5 py-2.5 text-xs font-semibold text-white shadow-sm transition-colors hover:bg-blue-500 disabled:opacity-50"
              >
                <CheckCircle2 className="h-4 w-4" />
                <span>
                  {isSubmitting
                    ? 'Saving Job...'
                    : isConnected
                      ? 'Create Job & Sync to Sheet'
                      : 'Create Job (Queue Offline)'}
                </span>
              </button>
            </div>
          </div>
        </form>
      </div>
    </div>
  );
}
