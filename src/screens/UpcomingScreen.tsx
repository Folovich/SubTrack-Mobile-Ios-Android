import React, { useCallback, useEffect, useState } from "react";

import { ScrollView, StyleSheet, Text, TouchableOpacity, View } from "react-native";
import { categoryApi } from "../api/categoryApi";
import { ApiError } from "../api/httpClient";

import { subscriptionApi } from "../api/subscriptionApi";
import AppButton from "../components/AppButton";
import { useI18n } from "../context/SettingsContext";
import type { Category } from "../types/category";
import type { SubscriptionStatus, UpcomingSubscription } from "../types/subscription";
import type { AppPalette } from "../theme/theme";

const dayOptions = [7, 30, 90];

const UpcomingScreen = () => {
  const { tr, colors } = useI18n();
  const styles = createStyles(colors);
  const [days, setDays] = useState(7);
  const [items, setItems] = useState<UpcomingSubscription[]>([]);
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
      const [upcomingResponse, categoriesResponse] = await Promise.all([
        subscriptionApi.getUpcoming(targetDays),
        categoryApi.getAll()
      ]);
      setItems(upcomingResponse);
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
      <Text style={styles.title}>{tr("tabUpcoming")}</Text>
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
      {isLoading ? <Text style={styles.meta}>{tr("loading")}</Text> : null}
      {error ? <Text style={styles.error}>{error}</Text> : null}
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
            <AppButton
              disabled={busyId === item.id || item.status === "PAUSED"}
              title={tr("pause")}
              variant="ghost"
              onPress={() => void onQuickStatusChange(item, "PAUSED")}
            />
            <AppButton
              disabled={busyId === item.id || item.status === "CANCELED"}
              title={tr("cancel")}
              variant="ghost"
              onPress={() => void onQuickStatusChange(item, "CANCELED")}
            />
            <AppButton
              disabled={busyId === item.id || item.status === "ACTIVE"}
              title={tr("activate")}
              onPress={() => void onQuickStatusChange(item, "ACTIVE")}
            />

          </View>
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
    row: {
      flexDirection: "row",
      gap: 8
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
      backgroundColor: colors.bgElevated,
      borderRadius: 14,
      padding: 12,
      gap: 6
    },
    cardTitle: {
      color: colors.text,
      fontWeight: "800"
    },
    meta: {
      color: colors.textMuted
    },
    actions: {
      flexDirection: "row",
      gap: 8
    },
    error: {
      color: colors.danger
    }
  });

export default UpcomingScreen;
