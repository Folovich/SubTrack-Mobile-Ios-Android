import React, { useState } from "react";
import { ScrollView, StyleSheet, Text, TouchableOpacity, View } from "react-native";
import { useAuth } from "../hooks/useAuth";
import { useI18n } from "../context/SettingsContext";
import type { AppLanguage, AppTheme } from "../types/settings";
import AppButton from "../components/AppButton";
import type { AppPalette } from "../theme/theme";

const languageOptions: Array<{ code: AppLanguage; label: string }> = [
  { code: "en", label: "English" },
  { code: "ru", label: "\u0420\u0443\u0441\u0441\u043a\u0438\u0439" }
];

const SettingsScreen = () => {
  const { signOut } = useAuth();
  const { language, theme, colors, setLanguage, setTheme, tr } = useI18n();
  const styles = createStyles(colors);
  const [error, setError] = useState<string | null>(null);

  const onSelectLanguage = async (nextLanguage: AppLanguage) => {
    try {
      setError(null);
      await setLanguage(nextLanguage);
    } catch (saveError) {
      setError(saveError instanceof Error ? saveError.message : tr("failedSaveLanguage"));
    }
  };

  const onSelectTheme = async (nextTheme: AppTheme) => {
    try {
      setError(null);
      await setTheme(nextTheme);
    } catch (saveError) {
      setError(saveError instanceof Error ? saveError.message : tr("failedSaveTheme"));
    }
  };

  return (
    <ScrollView contentContainerStyle={styles.container}>
      <Text style={styles.title}>{tr("settingsTitle")}</Text>
      <View style={styles.panel}>
        <Text style={styles.sectionTitle}>{tr("language")}</Text>
        <View style={styles.row}>
          {languageOptions.map((option) => (
            <TouchableOpacity
              key={option.code}
              style={[styles.pill, language === option.code ? styles.pillActive : null]}
              onPress={() => void onSelectLanguage(option.code)}
            >
              <Text style={[styles.pillLabel, language === option.code ? styles.pillLabelActive : null]}>
                {option.label}
              </Text>
            </TouchableOpacity>
          ))}
        </View>
        <Text style={styles.subtitle}>
          {tr("selected")}: {language.toUpperCase()}
        </Text>
      </View>
      <View style={styles.panel}>
        <Text style={styles.sectionTitle}>{tr("theme")}</Text>
        <View style={styles.row}>
          {(["light", "dark"] as const).map((option) => (
            <TouchableOpacity
              key={option}
              style={[styles.pill, theme === option ? styles.pillActive : null]}
              onPress={() => void onSelectTheme(option)}
            >
              <Text style={[styles.pillLabel, theme === option ? styles.pillLabelActive : null]}>
                {option === "light" ? tr("themeLight") : tr("themeDark")}
              </Text>
            </TouchableOpacity>
          ))}
        </View>
        <Text style={styles.subtitle}>
          {tr("selected")}: {theme === "light" ? tr("themeLight") : tr("themeDark")}
        </Text>
      </View>
      <View style={styles.panel}>
        <Text style={styles.sectionTitle}>{tr("session")}</Text>
        <Text style={styles.subtitle}>{tr("sessionDescription")}</Text>
        <View style={styles.signOutWrap}>
          <AppButton title={tr("signOut")} fullWidth onPress={() => void signOut()} />
        </View>
      </View>
      {error ? <Text style={styles.error}>{error}</Text> : null}
    </ScrollView>
  );
};

const createStyles = (colors: AppPalette) =>
  StyleSheet.create({
  container: {
    padding: 16,
    gap: 14,
    backgroundColor: colors.bg
  },
  title: {
    fontSize: 30,
    fontWeight: "900",
    color: colors.text
  },
  panel: {
    backgroundColor: colors.bgElevated,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 16,
    padding: 16,
    gap: 12
  },
  sectionTitle: {
    color: colors.text,
    fontWeight: "800",
    textTransform: "uppercase",
    letterSpacing: 0.6
  },
  subtitle: {
    color: colors.textMuted
  },
  row: {
    flexDirection: "row",
    gap: 8
  },
  pill: {
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 12,
    backgroundColor: colors.bgSoft,
    paddingHorizontal: 12,
    paddingVertical: 12,
    flex: 1,
    alignItems: "center"
  },
  pillActive: {
    backgroundColor: colors.accent,
    borderColor: colors.accent
  },
  pillLabel: {
    color: colors.text,
    fontWeight: "700"
  },
  pillLabelActive: {
    color: colors.accentText
  },
  signOutWrap: {
    marginTop: 8
  },
  error: {
    color: colors.danger
  }
});

export default SettingsScreen;
