import "react-native-gesture-handler";
import React from "react";
import { NavigationContainer } from "@react-navigation/native";
import { StatusBar } from "expo-status-bar";
import { AuthProvider } from "./src/context/AuthContext";
import { SettingsProvider, useI18n } from "./src/context/SettingsContext";
import RootNavigator from "./src/navigation/RootNavigator";
import { getNavigationTheme } from "./src/theme/theme";

const AppShell = () => {
  const { theme } = useI18n();

  return (
    <AuthProvider>
      <NavigationContainer theme={getNavigationTheme(theme)}>
        <StatusBar style={theme === "light" ? "dark" : "light"} />
        <RootNavigator />
      </NavigationContainer>
    </AuthProvider>
  );
};

export default function App() {
  return (
    <SettingsProvider>
      <AppShell />
    </SettingsProvider>
  );
}
