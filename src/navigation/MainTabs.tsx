import React from "react";
import { createBottomTabNavigator } from "@react-navigation/bottom-tabs";
import type { MainTabsParamList } from "../types/navigation";
import DashboardScreen from "../screens/DashboardScreen";
import SubscriptionsScreen from "../screens/SubscriptionsScreen";
import AnalyticsScreen from "../screens/AnalyticsScreen";
import ImportScreen from "../screens/ImportScreen";
import SettingsScreen from "../screens/SettingsScreen";
import { useI18n } from "../context/SettingsContext";

const Tab = createBottomTabNavigator<MainTabsParamList>();

const MainTabs = () => {
  const { tr, colors, theme } = useI18n();

  return (
    <Tab.Navigator
      initialRouteName="Dashboard"
      screenOptions={{
        headerStyle: {
          backgroundColor: theme === "light" ? "rgba(255,255,255,0.92)" : "#111111"
        },
        headerTitleStyle: {
          color: colors.text,
          fontWeight: "800",
          letterSpacing: 0.4
        },
        headerTintColor: colors.accent,
        tabBarStyle: {
          backgroundColor: theme === "light" ? "rgba(255,255,255,0.96)" : "#111111",
          borderTopColor: colors.border,
          height: 68,
          paddingBottom: 8,
          paddingTop: 8
        },
        tabBarActiveTintColor: colors.accent,
        tabBarInactiveTintColor: colors.textMuted,
        tabBarLabelStyle: {
          fontSize: 11,
          fontWeight: "700",
          letterSpacing: 0.2
        }
      }}
    >
      <Tab.Screen name="Dashboard" component={DashboardScreen} options={{ title: tr("tabOverview") }} />
      <Tab.Screen
        name="Subscriptions"
        component={SubscriptionsScreen}
        options={{ title: tr("tabSubscriptions") }}
      />
      <Tab.Screen name="Analytics" component={AnalyticsScreen} options={{ title: tr("tabAnalytics") }} />
      <Tab.Screen name="Import" component={ImportScreen} options={{ title: tr("tabMail") }} />
      <Tab.Screen name="Settings" component={SettingsScreen} options={{ title: tr("tabSettings") }} />
    </Tab.Navigator>
  );
};

export default MainTabs;
