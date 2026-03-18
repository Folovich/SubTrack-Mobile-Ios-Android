import React, { useCallback, useEffect, useState } from "react";
import { ScrollView, StyleSheet, Text, TouchableOpacity, View } from "react-native";
import { categoryApi } from "../api/categoryApi";
import { ApiError } from "../api/httpClient";
import { notificationApi } from "../api/notificationApi";
import { subscriptionApi } from "../api/subscriptionApi";
import AppButton from "../components/AppButton";
import { useI18n } from "../context/SettingsContext";
import type { Category } from "../types/category";
import type { NotificationItem } from "../types/notification";
import type { SubscriptionStatus, UpcomingSubscription } from "../types/subscription";
import type { AppPalette } from "../theme/theme";

const dayOptions = [7, 30, 90];

const UpcomingScreen = () => {
  const { tr, colors } = useI18n();
  const styles = createStyles(colors);
  const [days, setDays] = useState(7);
  const [items, setItems] = useState<UpcomingSubscription[]>([]);
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<number | null>(null);

  const formatStatus = (status: SubscriptionStatus) => {
    if (status === "ACTIVE") return tr("statusActive");
    if (status === "PAUSED") return tr("statusPaused");
    return tr("statusCanceled");
  };

  const load = useCallback(async (targetDays: number) => {
    try {
      setIsLoading(true);
      setError(null);
      const [upcomingResponse, notificationsResponse, categoriesResponse] = await Promise.all([
        subscriptionApi.getUpcoming(targetDays),
        notificationApi.getByDays(targetDays),
        categoryApi.getAll()
      ]);
      setItems(upcomingResponse);
      setNotifications(notificationsResponse);
      setCategories(categoriesResponse);
    } catch (loadError) {

      setError(loadError instanceof ApiError ? loadError.message : tr("failedLoadSubscriptions"));

    } finally {
      setIsLoading(false);
    }
  }, [tr]);

  useEffect(() => {
    void load(days);
  }, [days, load]);

  const onQuickStatusChange = async (item: UpcomingSubscription, nextStatus: SubscriptionStatus) => {
    try {
      setBusyId(item.id);
      setError(null);
      const categoryId = categories.find((category) => category.name === item.category)?.id ?? null;
      await subscriptionApi.update(item.id, {
        serviceName: item.serviceName,
        categoryId,
        amount: item.amount,
        currency: item.currency,
        billingPeriod: item.billingPeriod,
        nextBillingDate: item.nextBillingDate,
        status: nextStatus
      });
      await load(days);
    } catch (updateError) {

      setError(updateError instanceof ApiError ? updateError.message : tr("failedSaveSubscription"));

    } finally {
      setBusyId(null);
    }
  };

  return (
    <ScrollView contentContainerStyle={styles.container}>
      <View style={styles.hero}>
        <Text style={styles.title}>{tr("tabSchedule")}</Text>
        <Text style={styles.meta}>{tr("scheduleSubtitle")}</Text>
      </View>
      <View style={styles.row}>
        {dayOptions.map((option) => (
          <TouchableOpacity
            key={option}
            style={[styles.pill, days === option ? styles.pillActive : null]}
            onPress={() => setDays(option)}
          >
            <Text style={[styles.pillText, days === option ? styles.pillTextActive : null]}>{option}</Text>
          </TouchableOpacity>
        ))}
      </View>
      <Text style={styles.meta}>
        {tr("days")}: {days}
      </Text>
      <View style={styles.summaryRow}>
        <View style={styles.summaryCard}>
          <Text style={styles.summaryLabel}>{tr("upcomingChargesTitle")}</Text>
          <Text style={styles.summaryValue}>{items.length}</Text>
        </View>
        <View style={styles.summaryCard}>
          <Text style={styles.summaryLabel}>{tr("events")}</Text>
          <Text style={styles.summaryValue}>{notifications.length}</Text>
        </View>
      </View>
      {isLoading ? <Text style={styles.meta}>{tr("loading")}</Text> : null}
      {error ? <Text style={styles.error}>{error}</Text> : null}
      <Text style={styles.sectionTitle}>{tr("upcomingChargesTitle")}</Text>
      {items.length === 0 && !isLoading ? <Text style={styles.meta}>{tr("noUpcomingCharges")}</Text> : null}
      {items.map((item) => (
        <View key={item.id} style={styles.card}>
          <Text style={styles.cardTitle}>{item.serviceName}</Text>
          <Text style={styles.meta}>
            {item.amount} {item.currency}
          </Text>

          <Text style={styles.meta}>
            {tr("status")}: {formatStatus(item.status)}
          </Text>
          <Text style={styles.meta}>
            {tr("inDays")} {item.daysUntilBilling}
            {tr("dayShort")}
          </Text>
          <View style={styles.actions}>
            <View style={styles.actionItem}>
              <AppButton
                fullWidth
                disabled={busyId === item.id || item.status === "PAUSED"}
                title={tr("pause")}
                variant="ghost"
                onPress={() => void onQuickStatusChange(item, "PAUSED")}
              />
            </View>
            <View style={styles.actionItem}>
              <AppButton
                fullWidth
                disabled={busyId === item.id || item.status === "CANCELED"}
                title={tr("cancel")}
                variant="ghost"
                onPress={() => void onQuickStatusChange(item, "CANCELED")}
              />
            </View>
            <View style={styles.actionItem}>
              <AppButton
                fullWidth
                disabled={busyId === item.id || item.status === "ACTIVE"}
                title={tr("activate")}
                onPress={() => void onQuickStatusChange(item, "ACTIVE")}
              />
            </View>
          </View>
        </View>
      ))}

      <Text style={styles.sectionTitle}>{tr("reminderEventsTitle")}</Text>
      {notifications.length === 0 && !isLoading ? <Text style={styles.meta}>{tr("noReminderEvents")}</Text> : null}
      {notifications.map((item) => (
        <View key={item.id} style={styles.card}>
          <Text style={styles.cardTitle}>{item.message}</Text>
          <Text style={styles.meta}>{item.type}</Text>
          <Text style={styles.meta}>{item.scheduledAt}</Text>
          <Text style={styles.meta}>
            {tr("status")}: {item.status}
          </Text>
        </View>
      ))}
    </ScrollView>
  );
};

const createStyles = (colors: AppPalette) =>
  StyleSheet.create({
    container: {
      padding: 16,
      gap: 12,
      backgroundColor: colors.bg
    },
    title: {
      fontSize: 30,
      fontWeight: "900",
      color: colors.text
    },
    hero: {
      gap: 6,
      backgroundColor: colors.bgElevated,
      borderWidth: 1,
      borderColor: colors.border,
      borderRadius: 18,
      padding: 16
    },
    row: {
      flexDirection: "row",
      gap: 8
    },
    summaryRow: {
      flexDirection: "row",
      gap: 10
    },
    summaryCard: {
      flex: 1,
      borderRadius: 14,
      borderWidth: 1,
      borderColor: colors.border,
      backgroundColor: colors.bgElevated,
      padding: 12,
      gap: 4
    },
    summaryLabel: {
      color: colors.textMuted,
      fontSize: 12,
      textTransform: "uppercase",
      fontWeight: "800",
      letterSpacing: 0.6
    },
    summaryValue: {
      color: colors.text,
      fontSize: 24,
      fontWeight: "900"
    },
    pill: {
      flex: 1,
      borderRadius: 12,
      borderWidth: 1,
      borderColor: colors.border,
      backgroundColor: colors.bgElevated,
      alignItems: "center",
      paddingVertical: 12
    },
    pillActive: {
      backgroundColor: colors.accent,
      borderColor: colors.accent
    },
    pillText: {
      color: colors.text,
      fontWeight: "800"
    },
    pillTextActive: {
      color: colors.accentText
    },
    card: {
      borderWidth: 1,
      borderColor: colors.border,
      backgroundColor: colors.card,
      borderRadius: 14,
      padding: 12,
      gap: 6
    },
    cardTitle: {
      color: colors.text,
      fontWeight: "800"
    },
    sectionTitle: {
      color: colors.text,
      fontWeight: "800",
      textTransform: "uppercase",
      letterSpacing: 0.6
    },
    meta: {
      color: colors.textMuted
    },
    actions: {
      flexDirection: "row",
      flexWrap: "wrap",
      gap: 8
    },
    actionItem: {
      width: "100%"
    },
    error: {
      color: colors.danger
    }
  });

export default UpcomingScreen;
