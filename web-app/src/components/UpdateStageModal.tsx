import { useEffect, useState } from 'react';
import { ArrowRight, RefreshCw, X } from 'lucide-react';
import {
  ALL_JOB_STAGES,
  JobStage,
  RepairJob,
  STAGE_LABELS,
} from '../types/repairShop';
import { StageBadge } from './StageBadge';

interface UpdateStageModalProps {
  isOpen: boolean;
  job: RepairJob;
  isSaving: boolean;
  onClose: () => void;
  onSwitchToDeliveryHandover: () => void;
  onSaveStage: (nextStage: JobStage, note: string) => Promise<void>;
}

export function UpdateStageModal({
  isOpen,
  job,
  isSaving,
  onClose,
  onSwitchToDeliveryHandover,
  onSaveStage,
}: UpdateStageModalProps) {
  const [selectedStage, setSelectedStage] = useState<JobStage>(job.stage);
  const [note, setNote] = useState('');

  useEffect(() => {
    if (isOpen) {
      setSelectedStage(job.stage);
      setNote('');
    }
  }, [isOpen, job.stage]);

  if (!isOpen) return null;

  const handleConfirm = async (e: React.FormEvent) => {
    e.preventDefault();
    if (selectedStage === JobStage.DELIVERED) {
      onClose();
      onSwitchToDeliveryHandover();
      return;
    }
    await onSaveStage(selectedStage, note.trim());
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 p-4 backdrop-blur-sm dark:bg-slate-950/75"
      role="dialog"
      aria-modal="true"
      aria-labelledby="update-stage-title"
      onClick={(e) => {
        if (e.target === e.currentTarget && !isSaving) onClose();
      }}
    >
      <div className="w-full max-w-lg overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-2xl dark:border-slate-700/80 dark:bg-[#1E293B]">
        <div className="flex items-center justify-between border-b border-slate-200 px-6 py-4 dark:border-slate-800">
          <div className="flex items-center gap-2.5">
            <RefreshCw className="h-4 w-4 text-blue-600 dark:text-blue-400" />
            <h3 id="update-stage-title" className="font-display text-base font-semibold text-slate-900 dark:text-slate-100">
              Update Repair Stage
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
            aria-label="Close update stage modal"
          >
            <X className="h-4 w-4" />
          </button>
        </div>

        <form onSubmit={handleConfirm} className="space-y-5 p-6">
          <div>
            <p className="mb-2.5 text-xs font-medium text-slate-600 dark:text-slate-400">
              Select Next Stage
            </p>
            <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
              {ALL_JOB_STAGES.map((stage) => {
                const isSelected = selectedStage === stage;
                const isCurrent = job.stage === stage;
                return (
                  <button
                    key={stage}
                    type="button"
                    onClick={() => {
                      if (stage === JobStage.DELIVERED) {
                        onClose();
                        onSwitchToDeliveryHandover();
                        return;
                      }
                      setSelectedStage(stage);
                    }}
                    className={`flex items-center justify-between gap-2 rounded-xl border px-3.5 py-2.5 text-left transition-colors ${
                      isSelected
                        ? 'border-blue-500 bg-blue-50 text-slate-900 dark:bg-blue-600/20 dark:text-white'
                        : 'border-slate-200 bg-slate-50 text-slate-700 hover:border-slate-300 hover:text-slate-900 dark:border-slate-700/80 dark:bg-[#0F172A]/80 dark:text-slate-300 dark:hover:border-slate-600 dark:hover:text-white'
                    }`}
                  >
                    <StageBadge stage={stage} />
                    {isCurrent && (
                      <span className="text-[11px] font-medium text-slate-500 dark:text-slate-400">
                        Current
                      </span>
                    )}
                    {stage === JobStage.DELIVERED && !isCurrent && (
                      <ArrowRight className="h-3.5 w-3.5 text-slate-400" />
                    )}
                  </button>
                );
              })}
            </div>
            <p className="mt-2 text-[11px] text-slate-500 dark:text-slate-400">
              Selecting <strong className="text-slate-800 dark:text-slate-200">{STAGE_LABELS[JobStage.DELIVERED]}</strong> opens the Delivery Handover &amp; Settlement modal.
            </p>
          </div>

          <div>
            <label
              htmlFor="stage-update-note"
              className="mb-1.5 block text-xs font-medium text-slate-600 dark:text-slate-400"
            >
              Technician Note (Optional — appended to Status Timeline)
            </label>
            <textarea
              id="stage-update-note"
              rows={3}
              value={note}
              onChange={(e) => setNote(e.target.value)}
              placeholder={`Add note for ${STAGE_LABELS[selectedStage]} stage...`}
              className="w-full rounded-xl border border-slate-300 bg-slate-50 px-3.5 py-2.5 text-sm text-slate-900 placeholder:text-slate-400 focus:border-blue-500 focus:bg-white focus:outline-none dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-100 dark:placeholder:text-slate-500 dark:focus:bg-[#0F172A]"
            />
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
              {isSaving ? 'Updating...' : 'Update Stage'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
