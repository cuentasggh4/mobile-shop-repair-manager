export const GOOGLE_OAUTH_SCOPES = [
  'https://www.googleapis.com/auth/spreadsheets',
  'https://www.googleapis.com/auth/drive.file',
].join(' ');

interface TokenResponse {
  access_token?: string;
  expires_in?: number;
  error?: string;
  error_description?: string;
  scope?: string;
}

interface TokenClient {
  requestAccessToken: (overrideConfig?: { prompt?: string }) => void;
}

declare global {
  interface Window {
    google?: {
      accounts?: {
        oauth2?: {
          initTokenClient: (config: {
            client_id: string;
            scope: string;
            callback: (response: TokenResponse) => void;
            error_callback?: (err: { type: string; message?: string }) => void;
          }) => TokenClient;
          revoke: (token: string, done?: () => void) => void;
        };
      };
    };
  }
}

// Access token kept strictly in memory (never localStorage/sessionStorage).
let cachedAccessToken: string | null = null;
let tokenExpiresAt = 0;

export const DEFAULT_GOOGLE_CLIENT_ID =
  '934704963727-oimeeoieqdrd3f1s7hca579l5si50jsk.apps.googleusercontent.com';

export function sanitizeClientId(raw: string | undefined | null): string {
  if (!raw) return '';
  return raw.trim().replace(/^["']+|["']+$/g, '').trim();
}

export function getEffectiveClientId(): {
  clientId: string;
  source: 'env' | 'default';
} {
  const envClientId = sanitizeClientId(
    import.meta.env.VITE_GOOGLE_CLIENT_ID as string | undefined
  );

  if (envClientId) {
    return { clientId: envClientId, source: 'env' };
  }
  return { clientId: DEFAULT_GOOGLE_CLIENT_ID, source: 'default' };
}

export function getInMemoryAccessToken(): string | null {
  if (!cachedAccessToken) return null;
  if (Date.now() >= tokenExpiresAt - 30_000) {
    return null;
  }
  return cachedAccessToken;
}

export function clearInMemoryAccessToken(): void {
  const token = cachedAccessToken;
  cachedAccessToken = null;
  tokenExpiresAt = 0;
  if (token && window.google?.accounts?.oauth2?.revoke) {
    try {
      window.google.accounts.oauth2.revoke(token);
    } catch {
      // Ignore revoke errors
    }
  }
}

export function isGisLoaded(): boolean {
  return Boolean(window.google?.accounts?.oauth2?.initTokenClient);
}

export async function waitForGisLoaded(timeoutMs = 6000): Promise<boolean> {
  if (isGisLoaded()) return true;
  const start = Date.now();
  return new Promise((resolve) => {
    const interval = window.setInterval(() => {
      if (isGisLoaded()) {
        window.clearInterval(interval);
        resolve(true);
      } else if (Date.now() - start >= timeoutMs) {
        window.clearInterval(interval);
        resolve(false);
      }
    }, 120);
  });
}

export async function requestGoogleAccessToken(options?: {
  clientId?: string;
  prompt?: '' | 'consent' | 'select_account';
  timeoutMs?: number;
}): Promise<string> {
  const loaded = await waitForGisLoaded();
  if (!loaded || !window.google?.accounts?.oauth2) {
    throw new Error(
      'Google Identity Services script failed to load. Check network access to accounts.google.com.'
    );
  }

  const effective =
    sanitizeClientId(options?.clientId) || getEffectiveClientId().clientId;
  if (!effective) {
    throw new Error(
      'Google OAuth Client ID is required. Paste your Web OAuth Client ID in Settings or set VITE_GOOGLE_CLIENT_ID.'
    );
  }
  if (!effective.endsWith('.apps.googleusercontent.com')) {
    throw new Error(
      `Invalid OAuth Client ID format ("${effective}"). It must be a Web Client ID ending with .apps.googleusercontent.com.`
    );
  }

  return new Promise<string>((resolve, reject) => {
    let settled = false;
    const timer =
      options?.timeoutMs && options.timeoutMs > 0
        ? window.setTimeout(() => {
            if (!settled) {
              settled = true;
              reject(new Error('Silent OAuth token refresh timed out.'));
            }
          }, options.timeoutMs)
        : null;

    const finishResolve = (token: string) => {
      if (settled) return;
      settled = true;
      if (timer !== null) window.clearTimeout(timer);
      resolve(token);
    };

    const finishReject = (err: Error) => {
      if (settled) return;
      settled = true;
      if (timer !== null) window.clearTimeout(timer);
      reject(err);
    };

    const tokenClient = window.google!.accounts!.oauth2!.initTokenClient({
      client_id: effective,
      scope: GOOGLE_OAUTH_SCOPES,
      callback: (response: TokenResponse) => {
        if (response.error) {
          finishReject(new Error(response.error_description || response.error));
          return;
        }
        if (!response.access_token) {
          finishReject(new Error('Google did not return an OAuth access token.'));
          return;
        }
        const expiresInSec = response.expires_in ?? 3599;
        cachedAccessToken = response.access_token;
        tokenExpiresAt = Date.now() + expiresInSec * 1000;
        finishResolve(response.access_token);
      },
      error_callback: (err) => {
        finishReject(new Error(err.message || `OAuth popup error (${err.type})`));
      },
    });

    tokenClient.requestAccessToken({
      prompt: options?.prompt ?? 'consent',
    });
  });
}

export async function trySilentRefreshAccessToken(
  clientId?: string
): Promise<string | null> {
  try {
    return await requestGoogleAccessToken({
      clientId,
      prompt: '',
      timeoutMs: 6500,
    });
  } catch {
    return null;
  }
}
