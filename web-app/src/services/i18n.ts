export type AppLanguage = 'en' | 'hi' | 'gu';

const LANGUAGE_STORAGE_KEY = 'mrs_pos_language';

export function getSavedLanguage(): AppLanguage {
  try {
    const raw = localStorage.getItem(LANGUAGE_STORAGE_KEY);
    if (raw === 'en' || raw === 'hi' || raw === 'gu') return raw;
    return 'en';
  } catch {
    return 'en';
  }
}

export function saveLanguage(lang: AppLanguage): void {
  try {
    localStorage.setItem(LANGUAGE_STORAGE_KEY, lang);
  } catch {
    // Ignore storage errors
  }
}

export interface SettingsPreferencesStrings {
  appearanceTitle: string;
  appearanceSubtitle: string;
  themeLabel: string;
  themeSystem: string;
  themeLight: string;
  themeDark: string;
  languageLabel: string;
  notificationsTitle: string;
  notificationsSubtitle: string;
  notificationsToggleTitle: string;
  notificationsToggleDesc: string;
  permissionStatusLabel: string;
  permissionGranted: string;
  permissionDenied: string;
  permissionDefault: string;
  permissionUnsupported: string;
  allowNotificationsBtn: string;
  sendTestNotificationBtn: string;
}

export const SETTINGS_PREFERENCES_STRINGS: Record<
  AppLanguage,
  SettingsPreferencesStrings
> = {
  en: {
    appearanceTitle: 'Appearance (Theme)',
    appearanceSubtitle:
      'Choose how the POS interface looks on your desktop or counter display.',
    themeLabel: 'Theme Mode',
    themeSystem: 'System',
    themeLight: 'Light',
    themeDark: 'Dark',
    languageLabel: 'Preferences Language',
    notificationsTitle: 'Notifications',
    notificationsSubtitle:
      'Get browser alerts when new jobs are created, stages update (Ready for Pickup), or Google Sheets sync finishes.',
    notificationsToggleTitle: 'Enable Browser Notifications',
    notificationsToggleDesc:
      'Show desktop alerts for job updates and cloud sync status.',
    permissionStatusLabel: 'Browser Permission',
    permissionGranted: 'Granted',
    permissionDenied: 'Blocked',
    permissionDefault: 'Not Requested / Default',
    permissionUnsupported: 'Unsupported',
    allowNotificationsBtn: 'Allow Notifications',
    sendTestNotificationBtn: 'Send Test Notification',
  },
  hi: {
    appearanceTitle: 'दिखावट (थीम)',
    appearanceSubtitle:
      'अपने डेस्कटॉप या काउंटर डिस्प्ले के लिए लाइट या डार्क थीम चुनें।',
    themeLabel: 'थीम मोड',
    themeSystem: 'सिस्टम',
    themeLight: 'लाइट',
    themeDark: 'डार्क',
    languageLabel: 'सेटिंग्स भाषा',
    notificationsTitle: 'नोटिफ़िकेशन (सूचनाएं)',
    notificationsSubtitle:
      'नया जॉब कार्ड बनने, स्टेटस बदलने (रेडी फॉर पिकअप), या Google Sheets सिंक होने पर ब्राउज़र अलर्ट पाएं।',
    notificationsToggleTitle: 'ब्राउज़र नोटिफ़िकेशन चालू करें',
    notificationsToggleDesc:
      'जॉब अपडेट और क्लाउड सिंक के लिए डेस्कटॉप अलर्ट दिखाएं।',
    permissionStatusLabel: 'ब्राउज़र अनुमति',
    permissionGranted: 'अनुमति प्राप्त',
    permissionDenied: 'ब्लॉक किया गया',
    permissionDefault: 'अनुमति बाकी है',
    permissionUnsupported: 'सपोर्टेड नहीं',
    allowNotificationsBtn: 'नोटिफ़िकेशन की अनुमति दें',
    sendTestNotificationBtn: 'टेस्ट नोटिफ़िकेशन भेजें',
  },
  gu: {
    appearanceTitle: 'દેખાવ (થીમ)',
    appearanceSubtitle:
      'તમારા ડેસ્કટોપ અથવા કાઉન્ટર ડિસ્પ્લે માટે લાઇટ કે ડાર્ક થીમ પસંદ કરો.',
    themeLabel: 'થીમ મોડ',
    themeSystem: 'સિસ્ટમ',
    themeLight: 'લાઇટ',
    themeDark: 'ડાર્ક',
    languageLabel: 'સેટિંગ્સ ભાષા',
    notificationsTitle: 'નોટિફિકેશન (સૂચનાઓ)',
    notificationsSubtitle:
      'નવું જોબ કાર્ડ બને, સ્ટેટસ બદલાય (પિકઅપ માટે તૈયાર), અથવા Google Sheets સિંક પૂર્ણ થાય ત્યારે બ્રાઉઝર એલર્ટ મેળવો.',
    notificationsToggleTitle: 'બ્રાઉઝર નોટિફિકેશન ચાલુ કરો',
    notificationsToggleDesc:
      'જોબ અપડેટ અને ક્લાઉડ સિંક માટે ડેસ્કટોપ એલર્ટ બતાવો.',
    permissionStatusLabel: 'બ્રાઉઝર પરવાનગી',
    permissionGranted: 'મંજૂર',
    permissionDenied: 'બ્લોક કરેલ',
    permissionDefault: 'બાકી / ડિફોલ્ટ',
    permissionUnsupported: 'સપોર્ટેડ નથી',
    allowNotificationsBtn: 'નોટિફિકેશન મંજૂર કરો',
    sendTestNotificationBtn: 'ટેસ્ટ નોટિફિકેશન મોકલો',
  },
};
