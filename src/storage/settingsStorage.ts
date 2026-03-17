import AsyncStorage from "@react-native-async-storage/async-storage";
import type { AppLanguage, AppSettings, AppTheme } from "../types/settings";

const SETTINGS_KEY = "subtrack_settings";

const defaultSettings: AppSettings = {
  language: "en",
  theme: "dark"
};

export const settingsStorage = {
  getSettings: async (): Promise<AppSettings> => {
    const raw = await AsyncStorage.getItem(SETTINGS_KEY);
    if (!raw) {
      return defaultSettings;
    }
    try {
      const parsed = JSON.parse(raw) as AppSettings;
      return {
        ...defaultSettings,
        ...parsed
      };
    } catch {
      return defaultSettings;
    }
  },
  setSettings: async (value: AppSettings) => AsyncStorage.setItem(SETTINGS_KEY, JSON.stringify(value)),
  setLanguage: async (language: AppLanguage) => {
    const current = await settingsStorage.getSettings();
    const next = { ...current, language };
    await settingsStorage.setSettings(next);
    return next;
  },
  setTheme: async (theme: AppTheme) => {
    const current = await settingsStorage.getSettings();
    const next = { ...current, theme };
    await settingsStorage.setSettings(next);
    return next;
  }
};
