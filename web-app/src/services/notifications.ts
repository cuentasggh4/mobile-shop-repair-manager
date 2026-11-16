import { JobStage, RepairJob, STAGE_LABELS } from '../types/repairShop';

const NOTIFICATIONS_ENABLED_KEY = 'mrs_pos_notifications_enabled';

export type BrowserNotificationPermissionState =
  | 'granted'
  | 'denied'
  | 'default'
  | 'unsupported';

export function isBrowserNotificationSupported(): boolean {
  return typeof window !== 'undefined' && 'Notification' in window;
}

export function getNotificationPermissionState(): BrowserNotificationPermissionState {
  if (!isBrowserNotificationSupported()) {
    return 'unsupported';
  }
  return window.Notification.permission;
}

export function getNotificationsEnabled(): boolean {
  try {
    const raw = localStorage.getItem(NOTIFICATIONS_ENABLED_KEY);
    return raw !== 'false';
  } catch {
    return true;
  }
}

export function setNotificationsEnabled(enabled: boolean): void {
  try {
    localStorage.setItem(NOTIFICATIONS_ENABLED_KEY, enabled ? 'true' : 'false');
  } catch {
    // Ignore storage errors
  }
}

export async function requestBrowserNotificationPermission(): Promise<BrowserNotificationPermissionState> {
  if (!isBrowserNotificationSupported()) {
    return 'unsupported';
  }
  try {
    if (window.Notification.permission !== 'default') {
      return window.Notification.permission;
    }
    const result = await window.Notification.requestPermission();
    return result;
  } catch {
    return getNotificationPermissionState();
  }
}

export async function showBrowserNotification(payload: {
  title: string;
  body: string;
  tag?: string;
}): Promise<boolean> {
  if (!getNotificationsEnabled()) return false;
  if (!isBrowserNotificationSupported()) return false;
  if (window.Notification.permission !== 'granted') return false;

  try {
    if (typeof navigator !== 'undefined' && 'serviceWorker' in navigator) {
      const registration = await navigator.serviceWorker.getRegistration();
      if (registration && typeof registration.showNotification === 'function') {
        await registration.showNotification(payload.title, {
          body: payload.body,
          tag: payload.tag,
        });
        return true;
      }
    }

    new window.Notification(payload.title, {
      body: payload.body,
      tag: payload.tag,
    });
    return true;
  } catch {
    return false;
  }
}

export async function notifyJobCreated(job: RepairJob): Promise<boolean> {
  const device =
    [job.deviceBrand, job.deviceModel].filter(Boolean).join(' ') || 'Device';
  return await showBrowserNotification({
    title: `New Job Card: ${job.jobNumber}`,
    body: `${job.customerName || 'Customer'} — ${device}`,
    tag: `job-created-${job.recordUuid}`,
  });
}

export async function notifyJobStageChanged(
  job: RepairJob,
  previousStage: JobStage,
  nextStage: JobStage
): Promise<boolean> {
  const device =
    [job.deviceBrand, job.deviceModel].filter(Boolean).join(' ') || 'Device';

  if (nextStage === JobStage.READY_FOR_PICKUP) {
    return await showBrowserNotification({
      title: `Ready for Pickup: ${job.jobNumber}`,
      body: `${job.customerName || 'Customer'} (${device}) is repaired and ready for delivery.`,
      tag: `job-stage-${job.recordUuid}-${nextStage}`,
    });
  }

  return await showBrowserNotification({
    title: `Status Updated: ${job.jobNumber}`,
    body: `${job.customerName || 'Customer'} (${device}) moved from ${STAGE_LABELS[previousStage]} to ${STAGE_LABELS[nextStage]}.`,
    tag: `job-stage-${job.recordUuid}-${nextStage}`,
  });
}

export async function notifySyncResult(options: {
  success: boolean;
  summary: string;
}): Promise<boolean> {
  return await showBrowserNotification({
    title: options.success
      ? 'Google Sheets Sync Complete'
      : 'Google Sheets Sync Failed',
    body: options.summary,
    tag: options.success ? 'sync-success' : 'sync-error',
  });
}

export async function sendTestBrowserNotification(): Promise<{
  ok: boolean;
  reason?: string;
}> {
  if (!isBrowserNotificationSupported()) {
    return {
      ok: false,
      reason: 'Browser notifications are not supported in this browser.',
    };
  }
  if (!getNotificationsEnabled()) {
    return {
      ok: false,
      reason: 'Notifications are currently disabled in Settings. Enable the toggle first.',
    };
  }
  let permission = getNotificationPermissionState();
  if (permission === 'default') {
    permission = await requestBrowserNotificationPermission();
  }
  if (permission !== 'granted') {
    return {
      ok: false,
      reason:
        permission === 'denied'
          ? 'Browser permission is blocked. Allow notifications from your browser address bar site settings.'
          : 'Notification permission has not been granted yet.',
    };
  }

  const sent = await showBrowserNotification({
    title: 'Mobile Repair Shop POS',
    body: 'Test notification working! Job updates, Ready for Pickup alerts, and Sync notifications will appear here.',
    tag: `test-notification-${Date.now()}`,
  });

  return sent
    ? { ok: true }
    : { ok: false, reason: 'Could not display notification in this environment.' };
}
