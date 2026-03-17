import React, { useCallback, useEffect, useState } from "react";
import { useFocusEffect } from "@react-navigation/native";
import { ScrollView, StyleSheet, Text, TouchableOpacity, View } from "react-native";
import { ApiError } from "../api/httpClient";
import { notificationApi } from "../api/notificationApi";
import { subscriptionApi } from "../api/subscriptionApi";
import AppButton from "../components/AppButton";
import { useI18n } from "../context/SettingsContext";
import type { NotificationItem } from "../types/notification";
import type { Subscription } from "../types/subscription";
import type { AppPalette } from "../theme/theme";

const dayOptions = [7, 30, 90];

const filterOrphanNotifications = (
  notifications: NotificationItem[],
  subscriptions: Subscription[]
) => {
  const subscriptionIds = new Set(subscriptions.map((item) => item.id));
  return notifications.filter((notification) =>
    notification.subscriptionId == null ? true : subscriptionIds.has(notification.subscriptionId)
  );
};

const formatDateTime = (value: string) => {
  const parsed = new Date(value);

  if (Number.isNaN(parsed.getTime())) {
    return value;
  }

  return parsed.toLocaleString();
};

const statusColor = (status: string, colors: AppPalette) => {
  const normalizedStatus = status.toUpperCase();

  if (normalizedStatus.includes("PENDING")) {
    return colors.accent;
  }

  if (normalizedStatus.includes("FAILED") || normalizedStatus.includes("ERROR")) {
    return colors.danger;
  }

  return colors.textMuted;
};

const RemindersScreen = () => {
  const { tr, colors } = useI18n();
  const styles = createStyles(colors);
  const [days, setDays] = useState(7);
  const [items, setItems] = useState<NotificationItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async (targetDays: number) => {
    try {
      setIsLoading(true);
      setError(null);
      const [notificationsResponse, subscriptionsResponse] = await Promise.all([
        notificationApi.getByDays(targetDays),
        subscriptionApi.getAll()
      ]);
      setItems(filterOrphanNotifications(notificationsResponse, subscriptionsResponse));
    } catch (loadError) {
      setError(
        loadError instanceof ApiError && loadError.status === 400
          ? "Invalid days value for reminders."
          : loadError instanceof Error
            ? loadError.message
            : tr("failedLoadDetails")
      );
    } finally {
      setIsLoading(false);
    }
  }, [tr]);

  useEffect(() => {
    void load(days);
  }, [days, load]);

  useFocusEffect(
    useCallback(() => {
      void load(days);
    }, [days, load])
  );

  return (
    <ScrollView contentContainerStyle={styles.container}>
      <View style={styles.hero}>
        <Text style={styles.eyebrow}>{tr("notifications")}</Text>
        <Text style={styles.title}>{tr("tabReminders")}</Text>
        <Text style={styles.subtitle}>Delivery queue and billing reminders for the selected window.</Text>
      </View>

      <View style={styles.segmentRow}>
        {dayOptions.map((option) => (
          <TouchableOpacity
            key={option}
            style={[styles.segment, days === option ? styles.segmentActive : null]}
            onPress={() => setDays(option)}
          >
            <Text style={[styles.segmentText, days === option ? styles.segmentTextActive : null]}>
              {option}d
            </Text>
          </TouchableOpacity>
        ))}
      </View>

      <View style={styles.summaryRow}>
        <View style={styles.summaryCard}>
          <Text style={styles.summaryLabel}>{tr("days")}</Text>
          <Text style={styles.summaryValue}>{days}</Text>
        </View>
        <View style={styles.summaryCard}>
          <Text style={styles.summaryLabel}>Events</Text>
          <Text style={styles.summaryValue}>{items.length}</Text>
        </View>
      </View>

      {isLoading ? <Text style={styles.meta}>{tr("loading")}</Text> : null}
      {error ? <Text style={styles.error}>{error}</Text> : null}
      {items.length === 0 && !isLoading ? <Text style={styles.meta}>{tr("noReminders")}</Text> : null}

      {items.map((item) => (
        <View key={item.id} style={styles.card}>
          <View style={styles.cardHeader}>
            <Text style={styles.cardTitle}>{item.message}</Text>
            <View
              style={[
                styles.badge,
                {
                  borderColor: statusColor(item.status, colors),
                  backgroundColor: colors.bgSoft
                }
              ]}
            >
              <Text style={[styles.badgeText, { color: statusColor(item.status, colors) }]}>{item.status}</Text>
            </View>
          </View>
          <View style={styles.metaRow}>
            <Text style={styles.meta}>{item.type}</Text>
            <Text style={styles.meta}>#{item.id}</Text>
          </View>
          <Text style={styles.dateText}>{formatDateTime(item.scheduledAt)}</Text>
          {item.subscriptionId != null ? (
            <Text style={styles.meta}>Subscription ID: {item.subscriptionId}</Text>
          ) : null}
        </View>
      ))}

      <AppButton fullWidth title={tr("reload")} onPress={() => void load(days)} />
    </ScrollView>
  );
};

const createStyles = (colors: AppPalette) =>
  StyleSheet.create({
    container: {
      padding: 16,
      gap: 16,
      backgroundColor: colors.bg
    },
    hero: {
      gap: 6,
      padding: 18,
      borderRadius: 24,
      backgroundColor: colors.bgElevated,
      borderWidth: 1,
      borderColor: colors.border
    },
    eyebrow: {
      color: colors.accent,
      fontSize: 12,
      fontWeight: "800",
      textTransform: "uppercase",
      letterSpacing: 1.1
    },
    title: {
      fontSize: 30,
      fontWeight: "900",
      color: colors.text
    },
    subtitle: {
      color: colors.textMuted,
      lineHeight: 20
    },
    segmentRow: {
      flexDirection: "row",
      gap: 8
    },
    segment: {
      flex: 1,
      alignItems: "center",
      paddingVertical: 12,
      borderRadius: 14,
      borderWidth: 1,
      borderColor: colors.border,
      backgroundColor: colors.bgElevated
    },
    segmentActive: {
      backgroundColor: colors.accent,
      borderColor: colors.accent
    },
    segmentText: {
      color: colors.text,
      fontWeight: "800",
      letterSpacing: 0.4,
      textTransform: "uppercase"
    },
    segmentTextActive: {
      color: colors.accentText
    },
    summaryRow: {
      flexDirection: "row",
      gap: 10
    },
    summaryCard: {
      flex: 1,
      padding: 14,
      borderRadius: 16,
      backgroundColor: colors.bgElevated,
      borderWidth: 1,
      borderColor: colors.border,
      gap: 4
    },
    summaryLabel: {
      color: colors.textMuted,
      textTransform: "uppercase",
      letterSpacing: 0.7,
      fontWeight: "800",
      fontSize: 12
    },
    summaryValue: {
      color: colors.text,
      fontSize: 26,
      fontWeight: "900"
    },
    card: {
      gap: 8,
      borderWidth: 1,
      borderColor: colors.border,
      backgroundColor: colors.bgElevated,
      borderRadius: 18,
      padding: 14
    },
    cardHeader: {
      flexDirection: "row",
      alignItems: "flex-start",
      justifyContent: "space-between",
      gap: 10
    },
    cardTitle: {
      flex: 1,
      color: colors.text,
      fontWeight: "800",
      fontSize: 16,
      lineHeight: 22
    },
    badge: {
      borderWidth: 1,
      borderRadius: 999,
      paddingHorizontal: 10,
      paddingVertical: 6
    },
    badgeText: {
      fontSize: 11,
      fontWeight: "800",
      textTransform: "uppercase",
      letterSpacing: 0.6
    },
    metaRow: {
      flexDirection: "row",
      alignItems: "center",
      justifyContent: "space-between",
      gap: 8
    },
    dateText: {
      color: colors.text,
      fontWeight: "700"
    },
    meta: {
      color: colors.textMuted
    },
    error: {
      color: colors.danger
    }
  });

export default RemindersScreen;
