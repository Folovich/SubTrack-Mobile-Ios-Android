import type { Theme } from "@react-navigation/native";
import type { AppTheme } from "../types/settings";

export type AppPalette = {
  bg: string;
  bgElevated: string;
  bgSoft: string;
  card: string;
  border: string;
  text: string;
  textMuted: string;
  accent: string;
  accentStrong: string;
  accentText: string;
  danger: string;
  shadow: string;
  overlay: string;
  sidebar: string;
  sidebarText: string;
  secondaryButton: string;
  focus: string;
};

const darkPalette: AppPalette = {
  bg: "#0b0b0b",
  bgElevated: "#171717",
  bgSoft: "#232323",
  card: "#151515",
  border: "#2c2c2c",
  text: "#f5f5f5",
  textMuted: "#9f9f9f",
  accent: "#f5a623",
  accentStrong: "#ffb938",
  accentText: "#0b0b0b",
  danger: "#ff6b6b",
  shadow: "rgba(0,0,0,0.32)",
  overlay: "#111111",
  sidebar: "#101317",
  sidebarText: "#f5f5f5",
  secondaryButton: "#2b2b2b",
  focus: "#ffcf76"
};

const lightPalette: AppPalette = {
  bg: "#f3f9ff",
  bgElevated: "rgba(255,255,255,0.92)",
  bgSoft: "#eef7ff",
  card: "#ffffff",
  border: "rgba(40, 96, 164, 0.14)",
  text: "#0f1728",
  textMuted: "#6a7a96",
  accent: "#00aff0",
  accentStrong: "#54cbff",
  accentText: "#ffffff",
  danger: "#d9465f",
  shadow: "rgba(0, 136, 214, 0.12)",
  overlay: "#ffffff",
  sidebar: "#111827",
  sidebarText: "#f8fbff",
  secondaryButton: "#f4f8fc",
  focus: "#7fd9ff"
};

export const getPalette = (theme: AppTheme): AppPalette => (theme === "light" ? lightPalette : darkPalette);

export const getNavigationTheme = (theme: AppTheme): Theme => {
  const palette = getPalette(theme);

  return {
    dark: theme === "dark",
    colors: {
      primary: palette.accent,
      background: palette.bg,
      card: theme === "light" ? "rgba(255,255,255,0.96)" : "#111111",
      text: palette.text,
      border: palette.border,
      notification: palette.accent
    }
  };
};
