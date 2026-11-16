import {
  CheckCircle2,
  Clock,
  PackageCheck,
  SearchCode,
  Wrench,
  XCircle,
} from 'lucide-react';
import { JobStage, STAGE_LABELS } from '../types/repairShop';

interface StageBadgeProps {
  stage: JobStage;
}

export function StageBadge({ stage }: StageBadgeProps) {
  const label = STAGE_LABELS[stage] ?? stage;

  switch (stage) {
    case JobStage.READY_FOR_PICKUP:
      return (
        <span className="inline-flex items-center gap-1.5 rounded-md border border-emerald-300 bg-emerald-100 px-2 py-0.5 text-xs font-semibold text-emerald-900 dark:border-emerald-500/35 dark:bg-emerald-500/15 dark:font-medium dark:text-emerald-300 whitespace-nowrap shrink-0">
          <CheckCircle2 className="h-3.5 w-3.5 text-emerald-700 dark:text-emerald-400 shrink-0" />
          <span>{label}</span>
        </span>
      );
    case JobStage.DIAGNOSTIC:
      return (
        <span className="inline-flex items-center gap-1.5 rounded-md border border-amber-300 bg-amber-100 px-2 py-0.5 text-xs font-semibold text-amber-900 dark:border-amber-500/35 dark:bg-amber-500/15 dark:font-medium dark:text-amber-300 whitespace-nowrap shrink-0">
          <SearchCode className="h-3.5 w-3.5 text-amber-700 dark:text-amber-400 shrink-0" />
          <span>{label}</span>
        </span>
      );
    case JobStage.WAITING_FOR_PARTS:
      return (
        <span className="inline-flex items-center gap-1.5 rounded-md border border-amber-300 bg-amber-100 px-2 py-0.5 text-xs font-semibold text-amber-900 dark:border-amber-500/35 dark:bg-amber-500/15 dark:font-medium dark:text-amber-300 whitespace-nowrap shrink-0">
          <Clock className="h-3.5 w-3.5 text-amber-700 dark:text-amber-400 shrink-0" />
          <span>{label}</span>
        </span>
      );
    case JobStage.IN_PROGRESS:
      return (
        <span className="inline-flex items-center gap-1.5 rounded-md border border-amber-300 bg-amber-100 px-2 py-0.5 text-xs font-semibold text-amber-900 dark:border-amber-500/35 dark:bg-amber-500/15 dark:font-medium dark:text-amber-300 whitespace-nowrap shrink-0">
          <Wrench className="h-3.5 w-3.5 text-amber-700 dark:text-amber-400 shrink-0" />
          <span>{label}</span>
        </span>
      );
    case JobStage.CANCELLED:
      return (
        <span className="inline-flex items-center gap-1.5 rounded-md border border-red-300 bg-red-100 px-2 py-0.5 text-xs font-semibold text-red-900 dark:border-red-500/35 dark:bg-red-500/15 dark:font-medium dark:text-red-300 whitespace-nowrap shrink-0">
          <XCircle className="h-3.5 w-3.5 text-red-700 dark:text-red-400 shrink-0" />
          <span>{label}</span>
        </span>
      );
    case JobStage.DELIVERED:
    default:
      return (
        <span className="inline-flex items-center gap-1.5 rounded-md border border-slate-300 bg-slate-200 px-2 py-0.5 text-xs font-semibold text-slate-800 dark:border-slate-600/50 dark:bg-slate-700/40 dark:font-medium dark:text-slate-300 whitespace-nowrap shrink-0">
          <PackageCheck className="h-3.5 w-3.5 text-slate-600 dark:text-slate-400 shrink-0" />
          <span>{label}</span>
        </span>
      );
  }
}
