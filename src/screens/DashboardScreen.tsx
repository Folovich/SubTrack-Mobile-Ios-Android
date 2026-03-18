import React, { useCallback, useEffect, useState } from "react";
import { Pressable, ScrollView, StyleSheet, Text, View } from "react-native";
import { useNavigation } from "@react-navigation/native";
import type { BottomTabNavigationProp } from "@react-navigation/bottom-tabs";
import { ApiError } from "../api/httpClient";
import { dashboardApi } from "../api/dashboardApi";
import { importApi } from "../api/importApi";
import { userApi } from "../api/userApi";
import type { DashboardResponse } from "../types/dashboard";
import type { MainTabsParamList } from "../types/navigation";
import type { UserProfile } from "../types/user";
import { useAuth } from "../hooks/useAuth";
import { useI18n } from "../context/SettingsContext";
import AppButton from "../components/AppButton";
import type { AppPalette } from "../theme/theme";

type DashboardNavigationProp = BottomTabNavigationProp<MainTabsParamList, "Dashboard">;

const DashboardScreen = () => {
  const navigation = useNavigation<DashboardNavigationProp>();
  const { profileName } = useAuth();
  const { tr, colors } = useI18n();
  const styles = createStyles(colors);
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [dashboard, setDashboard] = useState<DashboardResponse | null>(null);
  const [mailConnected, setMailConnected] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadDashboard = useCallback(async () => {
    try {
      setIsLoading(true);
      setError(null);
      const [profileResponse, dashboardResponse, integrationResponse] = await Promise.all([
        userApi.getMe(),
        dashboardApi.getDashboard("month", 7),
        importApi.getIntegrationStatus()
      ]);
      setProfile(profileResponse);
      setDashboard(dashboardResponse);
      setMailConnected(integrationResponse.status === "ACTIVE");
    } catch (loadError) {
      const message =
        loadError instanceof ApiError && loadError.status === 400
          ? tr("dashboardPeriodError")
          : loadError instanceof Error
            ? loadError.message
            : tr("failedLoadDashboard");
      setError(message);
    } finally {
      setIsLoading(false);
    }
  }, [tr]);

  useEffect(() => {
    void loadDashboard();
  }, [loadDashboard]);

  return (
    <ScrollView contentContainerStyle={styles.container}>
      <View style={styles.hero}>
        <Text style={styles.kicker}>{tr("dashboardKicker")}</Text>
        <Text style={styles.title}>{tr("tabOverview")}</Text>
        <Text style={styles.subtitle}>
          {profile ? `${tr("signedInAs")} ${profile.email}` : tr("loadingProfile")}
        </Text>
        <Text style={styles.heroMeta}>{tr("dashboardSubtitle")}</Text>
        {profileName ? <Text style={styles.nameTag}>{tr("name")}: {profileName}</Text> : null}
      </View>

      {isLoading ? <Text style={styles.subtitle}>{tr("loading")}</Text> : null}
      {error ? <Text style={styles.error}>{error}</Text> : null}

      {dashboard ? (
        <>
          <View style={styles.statGrid}>
            <View style={[styles.statCard, styles.accentCard]}>
              <Text style={styles.statLabel}>{tr("total")}</Text>
              <Text style={styles.accentValue}>{dashboard.summary.totalAmount}</Text>
            </View>
            <View style={styles.statCard}>
              <Text style={styles.statLabel}>{tr("activeSubscriptions")}</Text>
              <Text style={styles.statValue}>{dashboard.summary.activeSubscriptions}</Text>
            </View>
          </View>

          <View style={styles.section}>
            <Text style={styles.sectionTitle}>{tr("quickActions")}</Text>
            <View style={styles.quickGrid}>
              <Pressable style={styles.quickCard} onPress={() => navigation.navigate("Subscriptions")}>
                <Text style={styles.quickTitle}>{tr("openSubscriptions")}</Text>
                <Text style={styles.quickMeta}>{tr("list")}</Text>
              </Pressable>
              <Pressable style={styles.quickCard} onPress={() => navigation.navigate("Upcoming")}>
                <Text style={styles.quickTitle}>{tr("openSchedule")}</Text>
                <Text style={styles.quickMeta}>{dashboard.upcoming.length} {tr("upcomingChargesTitle").toLowerCase()}</Text>
              </Pressable>
              <Pressable style={[styles.quickCard, styles.quickCardWide]} onPress={() => navigation.navigate("Import")}>
                <View style={styles.quickHeader}>
                  <Text style={styles.quickTitle}>{tr("openMail")}</Text>
                  <View style={[styles.mailBadge, mailConnected ? styles.mailBadgeActive : null]}>
                    <Text style={[styles.mailBadgeText, mailConnected ? styles.mailBadgeTextActive : null]}>
                      {mailConnected ? tr("dashboardConnected") : tr("dashboardNotConnected")}
                    </Text>
                  </View>
                </View>
                <Text style={styles.quickMeta}>
                  {mailConnected ? tr("dashboardReadyToSync") : tr("dashboardNeedsSetup")}
                </Text>
              </Pressable>
            </View>
          </View>

          <View style={styles.section}>
            <Text style={styles.sectionTitle}>{tr("summary")}</Text>
            <Text style={styles.bodyText}>
              {tr("period")}: {dashboard.summary.from} - {dashboard.summary.to}
            </Text>
          </View>

          <View style={styles.section}>
            <Text style={styles.sectionTitle}>{tr("forecast")}</Text>
            <View style={styles.rowCard}>
              <Text style={styles.bodyText}>{tr("month")}</Text>
              <Text style={styles.rowValue}>{dashboard.forecast.monthForecast}</Text>
            </View>
            <View style={styles.rowCard}>
              <Text style={styles.bodyText}>{tr("year")}</Text>
              <Text style={styles.rowValue}>{dashboard.forecast.yearForecast}</Text>
            </View>
          </View>

          <View style={styles.section}>
            <Text style={styles.sectionTitle}>{tr("upcoming")}</Text>
            {dashboard.upcoming.length === 0 ? <Text style={styles.emptyText}>{tr("noUpcomingCharges")}</Text> : null}
            {dashboard.upcoming.map((item) => (
              <View key={item.id} style={styles.rowCard}>
                <View>
                  <Text style={styles.bodyTextStrong}>{item.serviceName}</Text>
                  <Text style={styles.bodyText}>{item.amount} {item.currency}</Text>
                </View>
                <Text style={styles.rowValue}>
                  {item.daysUntilBilling}
                  {tr("dayShort")}
                </Text>
              </View>
            ))}
          </View>

          <View style={styles.section}>
            <Text style={styles.sectionTitle}>{tr("notifications")}</Text>
            {dashboard.notifications.length === 0 ? <Text style={styles.emptyText}>{tr("noReminders")}</Text> : null}
            {dashboard.notifications.map((item) => (
              <View key={item.id} style={styles.noticeCard}>
                <Text style={styles.bodyTextStrong}>{item.type}</Text>
                <Text style={styles.bodyText}>{item.message}</Text>
              </View>
            ))}
          </View>
        </>
      ) : null}

      <View style={styles.actions}>
        <AppButton title={tr("reload")} fullWidth onPress={() => void loadDashboard()} />
      </View>
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
  hero: {
    backgroundColor: colors.bgElevated,
    borderRadius: 18,
    padding: 18,
    borderWidth: 1,
    borderColor: colors.border,
    gap: 6
  },
  kicker: {
    color: colors.accent,
    textTransform: "uppercase",
    letterSpacing: 1.2,
    fontSize: 12,
    fontWeight: "800"
  },
  title: {
    fontSize: 30,
    fontWeight: "900",
    color: colors.text
  },
  subtitle: {
    color: colors.textMuted
  },
  heroMeta: {
    color: colors.textMuted,
    lineHeight: 20
  },
  nameTag: {
    alignSelf: "flex-start",
    backgroundColor: colors.accent,
    color: colors.accentText,
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: 999,
    fontWeight: "800",
    marginTop: 6
  },
  statGrid: {
    flexDirection: "row",
    gap: 12
  },
  statCard: {
    flex: 1,
    backgroundColor: colors.bgElevated,
    borderRadius: 16,
    padding: 16,
    borderWidth: 1,
    borderColor: colors.border,
    gap: 8
  },
  accentCard: {
    backgroundColor: colors.accent
  },
  statLabel: {
    color: colors.textMuted,
    fontSize: 12,
    textTransform: "uppercase",
    letterSpacing: 1
  },
  statValue: {
    color: colors.text,
    fontSize: 28,
    fontWeight: "900"
  },
  accentValue: {
    color: colors.accentText,
    fontSize: 28,
    fontWeight: "900"
  },
  section: {
    gap: 8,
    backgroundColor: colors.bgElevated,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 16,
    padding: 16
  },
  quickGrid: {
    gap: 10
  },
  quickCard: {
    gap: 6,
    backgroundColor: colors.bgSoft,
    borderRadius: 14,
    padding: 14,
    borderWidth: 1,
    borderColor: colors.border
  },
  quickCardWide: {
    backgroundColor: colors.card
  },
  quickHeader: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    gap: 10
  },
  quickTitle: {
    color: colors.text,
    fontWeight: "800",
    fontSize: 16
  },
  quickMeta: {
    color: colors.textMuted
  },
  mailBadge: {
    borderRadius: 999,
    paddingHorizontal: 10,
    paddingVertical: 5,
    borderWidth: 1,
    borderColor: colors.border,
    backgroundColor: colors.bgElevated
  },
  mailBadgeActive: {
    backgroundColor: colors.accent,
    borderColor: colors.accent
  },
  mailBadgeText: {
    color: colors.textMuted,
    fontWeight: "800",
    fontSize: 11,
    textTransform: "uppercase",
    letterSpacing: 0.5
  },
  mailBadgeTextActive: {
    color: colors.accentText
  },
  sectionTitle: {
    color: colors.text,
    fontWeight: "800",
    fontSize: 16,
    textTransform: "uppercase",
    letterSpacing: 0.6
  },
  bodyText: {
    color: colors.textMuted
  },
  bodyTextStrong: {
    color: colors.text,
    fontWeight: "700"
  },
  rowCard: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    backgroundColor: colors.bgSoft,
    borderRadius: 12,
    padding: 12
  },
  rowValue: {
    color: colors.accent,
    fontWeight: "900",
    fontSize: 18
  },
  noticeCard: {
    backgroundColor: colors.bgSoft,
    borderRadius: 12,
    padding: 12,
    gap: 4
  },
  emptyText: {
    color: colors.textMuted
  },
  actions: {
    gap: 8
  },
  error: {
    color: colors.danger
  }
});

export default DashboardScreen;
