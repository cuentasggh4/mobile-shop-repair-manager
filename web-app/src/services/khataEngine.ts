import {
  appendStatusLog,
  digitsOnly,
  JobStage,
  RepairJob,
  ShopProfile,
} from '../types/repairShop';

export interface KhataCustomerSummary {
  customerKey: string;
  customerName: string;
  customerPhone: string;
  totalJobsCount: number;
  unpaidJobsCount: number;
  totalBilled: number;
  totalPaid: number;
  totalPendingDue: number;
  lastVisitAt: number;
  jobs: RepairJob[];
}

export interface KhataJobBillingSnapshot {
  effectiveTotal: number;
  discount: number;
  netBill: number;
  advancePaid: number;
  balanceDue: number;
  isSettled: boolean;
}

export interface KhataPaymentAllocation {
  originalJob: RepairJob;
  updatedJob: RepairJob;
  appliedAmount: number;
  previousDue: number;
  remainingDue: number;
  auditDetails: string;
}

function roundCurrency(value: number): number {
  return Math.round(value * 100) / 100;
}

export function getCustomerKhataKey(customerName: string, customerPhone: string): string {
  const digits = digitsOnly(customerPhone || '');
  if (digits.length >= 7) {
    return `P:${digits.slice(-10)}`;
  }
  return `N:${(customerName || '').trim().toLowerCase()}`;
}

export function computeKhataJobBilling(job: RepairJob): KhataJobBillingSnapshot {
  const effectiveTotal = Math.max(
    0,
    job.finalCost > 0 ? job.finalCost : job.estimatedCost
  );
  const discount = Math.max(0, job.discount);
  const netBill = roundCurrency(Math.max(0, effectiveTotal - discount));
  const advancePaid = roundCurrency(Math.max(0, job.advancePaid));
  const rawDue = roundCurrency(Math.max(0, netBill - advancePaid));
  const balanceDue = job.isPaid || rawDue <= 0.01 ? 0 : rawDue;
  const isSettled = job.isPaid || balanceDue <= 0.01;

  return {
    effectiveTotal,
    discount,
    netBill,
    advancePaid,
    balanceDue,
    isSettled,
  };
}

export function buildKhataCustomers(jobs: RepairJob[]): KhataCustomerSummary[] {
  const grouped = new Map<string, RepairJob[]>();

  for (const job of jobs) {
    if (job.isDeleted) continue;
    const key = getCustomerKhataKey(job.customerName, job.customerPhone);
    const list = grouped.get(key);
    if (list) {
      list.push(job);
    } else {
      grouped.set(key, [job]);
    }
  }

  const summaries: KhataCustomerSummary[] = [];

  for (const [customerKey, customerJobs] of grouped.entries()) {
    const sortedJobs = [...customerJobs].sort(
      (a, b) => (b.createdAt || b.updatedAt) - (a.createdAt || a.updatedAt) || b.updatedAt - a.updatedAt
    );

    const latestName =
      sortedJobs.find((j) => j.customerName.trim().length > 0)?.customerName.trim() ||
      'Walk-in Customer';
    const latestPhone =
      sortedJobs.find((j) => j.customerPhone.trim().length > 0)?.customerPhone.trim() ||
      '';

    let totalBilled = 0;
    let totalPendingDue = 0;
    let unpaidJobsCount = 0;
    let lastVisitAt = 0;

    for (const job of sortedJobs) {
      const visitTime = Math.max(job.createdAt || 0, job.updatedAt || 0);
      if (visitTime > lastVisitAt) {
        lastVisitAt = visitTime;
      }

      if (job.stage === JobStage.CANCELLED) {
        continue;
      }

      const billing = computeKhataJobBilling(job);
      totalBilled += billing.netBill;

      if (!job.isPaid && billing.balanceDue > 0.01) {
        totalPendingDue += billing.balanceDue;
        unpaidJobsCount += 1;
      }
    }

    totalBilled = roundCurrency(totalBilled);
    totalPendingDue = roundCurrency(totalPendingDue);
    const totalPaid = roundCurrency(Math.max(0, totalBilled - totalPendingDue));

    summaries.push({
      customerKey,
      customerName: latestName,
      customerPhone: latestPhone,
      totalJobsCount: sortedJobs.length,
      unpaidJobsCount,
      totalBilled,
      totalPaid,
      totalPendingDue,
      lastVisitAt,
      jobs: sortedJobs,
    });
  }

  // Sort: pending-due customers first (highest due first), then by most recent visit
  return summaries.sort((a, b) => {
    const aHasDue = a.totalPendingDue > 0.01;
    const bHasDue = b.totalPendingDue > 0.01;
    if (aHasDue !== bHasDue) {
      return aHasDue ? -1 : 1;
    }
    if (aHasDue && bHasDue && Math.abs(b.totalPendingDue - a.totalPendingDue) > 0.01) {
      return b.totalPendingDue - a.totalPendingDue;
    }
    return b.lastVisitAt - a.lastVisitAt;
  });
}

export function findMatchingKhataCustomer(
  customers: KhataCustomerSummary[],
  customerName: string,
  customerPhone: string
): KhataCustomerSummary | null {
  const digits = digitsOnly(customerPhone || '');
  if (digits.length >= 7) {
    const phoneKey = `P:${digits.slice(-10)}`;
    const phoneMatch = customers.find((c) => c.customerKey === phoneKey);
    if (phoneMatch) return phoneMatch;
  }

  const trimmedName = customerName.trim().toLowerCase();
  if (!trimmedName) return null;

  const exactKeyMatch = customers.find((c) => c.customerKey === `N:${trimmedName}`);
  if (exactKeyMatch) return exactKeyMatch;

  return (
    customers.find((c) => c.customerName.trim().toLowerCase() === trimmedName) ?? null
  );
}

export function buildKhataPaymentPlan(
  customer: KhataCustomerSummary,
  paymentAmount: number,
  paymentMode: string,
  note: string,
  currencySymbol: string,
  now: number = Date.now()
): {
  allocations: KhataPaymentAllocation[];
  totalApplied: number;
  unappliedRemainder: number;
} {
  let remaining = roundCurrency(Math.max(0, paymentAmount));
  const trimmedNote = note.trim();

  // Distribute oldest-first across unpaid non-cancelled jobs
  const unpaidOldestFirst = [...customer.jobs]
    .filter((job) => {
      if (job.isDeleted || job.stage === JobStage.CANCELLED || job.isPaid) {
        return false;
      }
      return computeKhataJobBilling(job).balanceDue > 0.01;
    })
    .sort((a, b) => (a.createdAt || a.updatedAt) - (b.createdAt || b.updatedAt));

  const allocations: KhataPaymentAllocation[] = [];
  let totalApplied = 0;

  for (let idx = 0; idx < unpaidOldestFirst.length; idx++) {
    if (remaining <= 0.005) break;
    const job = unpaidOldestFirst[idx];
    const billing = computeKhataJobBilling(job);
    const applied = roundCurrency(Math.min(remaining, billing.balanceDue));
    if (applied <= 0) continue;

    const newAdvancePaid = roundCurrency(billing.advancePaid + applied);
    const newDue = roundCurrency(Math.max(0, billing.netBill - newAdvancePaid));
    const isNowPaid = newDue <= 0.01;

    const noteSuffix = trimmedNote ? ` — ${trimmedNote}` : '';
    const logMessage = `Khata payment received: ${currencySymbol}${applied.toLocaleString()} via ${paymentMode}${noteSuffix}`;
    const timestamp = now + idx;

    const updatedJob: RepairJob = {
      ...job,
      finalCost: billing.effectiveTotal,
      advancePaid: newAdvancePaid,
      isPaid: isNowPaid,
      paymentMode,
      updatedAt: timestamp,
      statusLogsJson: appendStatusLog(job.statusLogsJson, {
        stage: job.stage,
        timestamp,
        note: logMessage,
      }),
    };

    const auditDetails = `${logMessage} (${job.jobNumber}: due ${currencySymbol}${billing.balanceDue.toLocaleString()} -> ${
      isNowPaid ? 'PAID' : `${currencySymbol}${newDue.toLocaleString()}`
    })`;

    allocations.push({
      originalJob: job,
      updatedJob,
      appliedAmount: applied,
      previousDue: billing.balanceDue,
      remainingDue: isNowPaid ? 0 : newDue,
      auditDetails,
    });

    totalApplied = roundCurrency(totalApplied + applied);
    remaining = roundCurrency(remaining - applied);
  }

  return {
    allocations,
    totalApplied,
    unappliedRemainder: remaining,
  };
}

export function buildKhataWhatsAppReminderMessage(
  customer: KhataCustomerSummary,
  shopProfile: ShopProfile | null
): string {
  const shopName = shopProfile?.shopName || 'Mobile Repair Shop';
  const shopContact = shopProfile?.contactPhone || shopProfile?.whatsappNumber || '';
  const currency = shopProfile?.currencySymbol || '₹';

  const unpaidJobs = [...customer.jobs]
    .filter(
      (j) =>
        !j.isDeleted &&
        j.stage !== JobStage.CANCELLED &&
        !j.isPaid &&
        computeKhataJobBilling(j).balanceDue > 0.01
    )
    .sort((a, b) => (a.createdAt || a.updatedAt) - (b.createdAt || b.updatedAt));

  if (unpaidJobs.length === 0) {
    return [
      `Hello ${customer.customerName},`,
      '',
      `Thank you for visiting *${shopName}*. Your Khata account currently has *no pending dues* (all bills settled).`,
      '',
      shopContact ? `Contact: ${shopContact}` : '',
      `— ${shopName}`,
    ]
      .filter((line, idx, arr) => !(line === '' && arr[idx - 1] === ''))
      .join('\n');
  }

  const itemizedLines = unpaidJobs.map((job) => {
    const billing = computeKhataJobBilling(job);
    const device = [job.deviceBrand, job.deviceModel].filter(Boolean).join(' ') || 'Device';
    const dateStr = new Date(job.createdAt || job.updatedAt).toLocaleDateString();
    return `• *${job.jobNumber}* (${device}, ${dateStr}) — Bill: ${currency}${billing.netBill.toLocaleString()}, Paid: ${currency}${billing.advancePaid.toLocaleString()}, *Due: ${currency}${billing.balanceDue.toLocaleString()}*`;
  });

  return [
    `Hello ${customer.customerName},`,
    '',
    `Friendly Khata payment reminder from *${shopName}* for your repair ${
      unpaidJobs.length === 1 ? 'bill' : 'bills'
    }:`,
    '',
    ...itemizedLines,
    '',
    `*Total Balance Due: ${currency}${customer.totalPendingDue.toLocaleString()}*`,
    '',
    `Please let us know once paid or visit the counter for settlement.`,
    shopContact ? `Contact: ${shopContact}` : '',
    `— ${shopName}`,
  ]
    .filter((line, idx, arr) => !(line === '' && arr[idx - 1] === ''))
    .join('\n');
}
