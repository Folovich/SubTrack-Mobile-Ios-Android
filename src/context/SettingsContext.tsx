import React, { createContext, useContext, useEffect, useMemo, useState } from "react";
import { settingsStorage } from "../storage/settingsStorage";
import type { AppLanguage, AppTheme } from "../types/settings";
import { t, type TranslationKey } from "../i18n/translations";
import { getPalette, type AppPalette } from "../theme/theme";

type SettingsContextValue = {
  language: AppLanguage;
  theme: AppTheme;
  colors: AppPalette;
  setLanguage: (language: AppLanguage) => Promise<void>;
  setTheme: (theme: AppTheme) => Promise<void>;
  tr: (key: TranslationKey) => string;
};

const SettingsContext = createContext<SettingsContextValue | undefined>(undefined);

export const SettingsProvider = ({ children }: { children: React.ReactNode }) => {
  const [language, setLanguageState] = useState<AppLanguage>("en");
  const [theme, setThemeState] = useState<AppTheme>("dark");

  useEffect(() => {
    let isMounted = true;
    const load = async () => {
      const settings = await settingsStorage.getSettings();
      if (isMounted) {
        setLanguageState(settings.language);
        setThemeState(settings.theme);
      }
    };
    void load();
    return () => {
      isMounted = false;
    };
  }, []);

  const setLanguage = async (nextLanguage: AppLanguage) => {
    await settingsStorage.setLanguage(nextLanguage);
    setLanguageState(nextLanguage);
  };

  const setTheme = async (nextTheme: AppTheme) => {
    await settingsStorage.setTheme(nextTheme);
    setThemeState(nextTheme);
  };

  const value = useMemo(
    () => ({
      language,
      theme,
      colors: getPalette(theme),
      setLanguage,
      setTheme,
      tr: (key: TranslationKey) => t(language, key)
    }),
    [language, theme]
  );

  return <SettingsContext.Provider value={value}>{children}</SettingsContext.Provider>;
};

export const useI18n = () => {
  const context = useContext(SettingsContext);
  if (!context) {
    throw new Error("useI18n must be used within SettingsProvider");
  }
  return context;
};
