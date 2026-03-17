import React, { useCallback, useEffect, useMemo, useState } from "react";

import { Alert, Button, ScrollView, StyleSheet, Text, TextInput, TouchableOpacity, View } from "react-native";
import { ApiError } from "../api/httpClient";
import { categoryApi } from "../api/categoryApi";
import { recommendationApi } from "../api/recommendationApi";
import { subscriptionApi } from "../api/subscriptionApi";
import { useI18n } from "../context/SettingsContext";
import type { Category } from "../types/category";
import type { Recommendation } from "../types/recommendation";
import type {
  BillingPeriod,
  Subscription,
  SubscriptionRequest,
  SubscriptionStatus
} from "../types/subscription";
import type { AppPalette } from "../theme/theme";


const billingPeriods: BillingPeriod[] = ["WEEKLY", "MONTHLY", "QUARTERLY", "YEARLY"];
const statusOptions: Array<SubscriptionStatus | "ALL"> = ["ALL", "ACTIVE", "PAUSED", "CANCELED"];
const currencies = [
  { code: "USD", label: "USD" },
  { code: "RUB", label: "RUB" }
];

const initialForm: SubscriptionRequest = {
  serviceName: "",
  amount: 9.99,
  currency: "USD",
  billingPeriod: "MONTHLY",
  nextBillingDate: ""
};

const SubscriptionsScreen = () => {
  const { tr, colors } = useI18n();
  const styles = createStyles(colors);
  const [subscriptions, setSubscriptions] = useState<Subscription[]>([]);

  const [categories, setCategories] = useState<Category[]>([]);

  const [selectedSubscription, setSelectedSubscription] = useState<Subscription | null>(null);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [statusFilter, setStatusFilter] = useState<SubscriptionStatus | "ALL">("ALL");
  const [searchQuery, setSearchQuery] = useState("");
  const [sortMode, setSortMode] = useState<"NEXT_ASC" | "AMOUNT_DESC">("NEXT_ASC");
  const [form, setForm] = useState<SubscriptionRequest>(initialForm);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [isRecommendationsLoading, setIsRecommendationsLoading] = useState(false);
  const [recommendations, setRecommendations] = useState<Recommendation[]>([]);
  const [error, setError] = useState<string | null>(null);

  const currentCategoryName = useMemo(
    () => categories.find((item) => item.id === form.categoryId)?.name ?? tr("none"),
    [categories, form.categoryId, tr]
  );

  const formatBillingPeriod = useCallback(
    (period: BillingPeriod) => {
      if (period === "WEEKLY") return tr("periodWeekly");
      if (period === "MONTHLY") return tr("periodMonthly");
      if (period === "QUARTERLY") return tr("periodQuarterly");
      if (period === "YEARLY") return tr("periodYearly");
      return period;
    },
    [tr]
  );

  const formatStatus = useCallback(
    (status: SubscriptionStatus | "ALL") => {
      if (status === "ALL") return tr("statusAll");
      if (status === "ACTIVE") return tr("statusActive");
      if (status === "PAUSED") return tr("statusPaused");
      return tr("statusCanceled");
    },
    [tr]
  );

  const visibleSubscriptions = useMemo(() => {
    const normalizedQuery = searchQuery.trim().toLowerCase();

    const filtered = subscriptions.filter((subscription) => {
      const byStatus = statusFilter === "ALL" || subscription.status === statusFilter;
      const bySearch =
        normalizedQuery.length === 0 ||
        subscription.serviceName.toLowerCase().includes(normalizedQuery);
      return byStatus && bySearch;
    });

    const sorted = [...filtered].sort((a, b) => {
      if (sortMode === "AMOUNT_DESC") {
        return b.amount - a.amount;
      }
      return a.nextBillingDate.localeCompare(b.nextBillingDate);
    });

    return sorted;
  }, [searchQuery, sortMode, statusFilter, subscriptions]);

  const loadAll = useCallback(async () => {
    try {
      setIsLoading(true);
      setError(null);

      const [subsRes, catsRes] = await Promise.all([
        subscriptionApi.getAll(),
        categoryApi.getAll()
      ]);
      setSubscriptions(subsRes);
      setCategories(catsRes);

    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : tr("failedLoadSubscriptions"));
    } finally {
      setIsLoading(false);
    }
  }, [tr]);

  useEffect(() => {
    void loadAll();
  }, [loadAll]);

  const resetForm = () => {
    setForm(initialForm);
    setEditingId(null);
    setIsFormOpen(false);
  };

  const onSubmit = async () => {
    try {
      setError(null);
      if (!form.serviceName.trim() || !form.nextBillingDate.trim()) {
        setError(tr("requiredFields"));
        return;
      }
      if (form.amount <= 0) {
        setError(tr("amountPositive"));
        return;
      }

      const payload: SubscriptionRequest = {
        serviceName: form.serviceName.trim(),
        amount: form.amount,
        currency: form.currency,
        billingPeriod: form.billingPeriod,
        nextBillingDate: form.nextBillingDate.trim(),
        status: form.status
      };

      if (editingId) {
        await subscriptionApi.update(editingId, payload);
      } else {
        await subscriptionApi.create(payload);
      }

      resetForm();
      await loadAll();
    } catch (submitError) {
      setError(
        submitError instanceof Error ? submitError.message : tr("failedSaveSubscription")
      );
    }
  };

  const onEdit = (subscription: Subscription) => {
    setEditingId(subscription.id);
    setForm({
      serviceName: subscription.serviceName,
      amount: subscription.amount,
      currency: subscription.currency,
      billingPeriod: subscription.billingPeriod,
      nextBillingDate: subscription.nextBillingDate,
      status: subscription.status
    });
    setIsFormOpen(true);
  };

  const onDelete = async (id: number) => {
    try {
      setError(null);
      await subscriptionApi.remove(id);
      await loadAll();
    } catch (deleteError) {
      setError(
        deleteError instanceof Error ? deleteError.message : tr("failedDeleteSubscription")
      );
    }
  };

  const onDeletePress = (id: number) => {
    Alert.alert(tr("confirmDeleteTitle"), tr("confirmDeleteMessage"), [
      { text: tr("cancel"), style: "cancel" },
      {
        text: tr("delete"),
        style: "destructive",
        onPress: () => void onDelete(id)
      }
    ]);
  };

  const onLoadDetails = async (id: number) => {
    try {
      setError(null);
      const details = await subscriptionApi.getById(id);
      setSelectedSubscription(details);
    } catch (detailsError) {
      setError(
        detailsError instanceof Error ? detailsError.message : tr("failedLoadDetails")
      );
    }
  };

  useEffect(() => {
    if (!selectedSubscription?.category) {
      setRecommendations([]);
      return;
    }

    void (async () => {
      try {
        setIsRecommendationsLoading(true);
        const response = await recommendationApi.getByCategory(selectedSubscription.category ?? "");
        setRecommendations(response);
      } catch (recommendationError) {
        if (recommendationError instanceof ApiError && recommendationError.status === 404) {
          setRecommendations([]);
          return;
        }
        setError(
          recommendationError instanceof Error
            ? recommendationError.message
            : tr("failedLoadDetails")
        );
      } finally {
        setIsRecommendationsLoading(false);
      }
    })();
  }, [selectedSubscription?.category, tr]);

  return (
    <ScrollView contentContainerStyle={styles.container}>
      <Text style={styles.title}>{tr("subscriptionsTitle")}</Text>
      {isLoading ? <Text style={styles.meta}>{tr("loading")}</Text> : null}
      {error ? <Text style={styles.error}>{error}</Text> : null}

      <View style={styles.section}>

        <Text style={styles.sectionTitle}>{editingId ? `${tr("editWithId")}${editingId}` : tr("createSubscription")}</Text>
        <TextInput
          placeholder={tr("serviceName")}
          style={styles.input}
          value={form.serviceName}
          onChangeText={(value) => setForm((prev) => ({ ...prev, serviceName: value }))}
        />
        <TextInput
          placeholder={tr("amount")}
          keyboardType="numeric"
          style={styles.input}
          value={String(form.amount)}
          onChangeText={(value) =>
            setForm((prev) => ({ ...prev, amount: Number(value.replace(",", ".")) || 0 }))
          }
        />
        <Text style={styles.label}>{tr("currency")}</Text>
        <View style={styles.row}>
          {currencies.map((currency) => (
            <TouchableOpacity
              key={currency.code}
              style={[styles.pill, form.currency === currency.code ? styles.pillActive : null]}
              onPress={() => setForm((prev) => ({ ...prev, currency: currency.code }))}
            >
              <Text>{currency.label}</Text>
            </TouchableOpacity>
          ))}
        </View>
        <TextInput
          placeholder={tr("nextBillingDate")}
          style={styles.input}
          value={form.nextBillingDate}
          onChangeText={(value) => setForm((prev) => ({ ...prev, nextBillingDate: value }))}
        />

        <Text style={styles.label}>{tr("billingPeriod")}: {form.billingPeriod}</Text>
        <View style={styles.row}>
          {billingPeriods.map((period) => (
            <TouchableOpacity
              key={period}
              style={[styles.pill, form.billingPeriod === period ? styles.pillActive : null]}
              onPress={() => setForm((prev) => ({ ...prev, billingPeriod: period }))}
            >
              <Text>{period}</Text>
            </TouchableOpacity>
          ))}
        </View>

        <Text style={styles.label}>{tr("category")}: {currentCategoryName}</Text>
        <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.row}>
          <TouchableOpacity
            style={[styles.pill, form.categoryId === null ? styles.pillActive : null]}
            onPress={() => setForm((prev) => ({ ...prev, categoryId: null }))}
          >
            <Text>{tr("none")}</Text>
          </TouchableOpacity>
          {categories.map((category) => (
            <TouchableOpacity
              key={category.id}
              style={[styles.pill, form.categoryId === category.id ? styles.pillActive : null]}
              onPress={() => setForm((prev) => ({ ...prev, categoryId: category.id }))}
            >
              <Text>{category.name}</Text>
            </TouchableOpacity>
          ))}
        </ScrollView>

        <View style={styles.row}>
          <Button title={editingId ? tr("update") : tr("create")} onPress={() => void onSubmit()} />
          {editingId ? <Button title={tr("cancel")} onPress={resetForm} /> : null}
        </View>
      </View>

      <View style={styles.section}>

        <Text style={styles.sectionTitle}>{tr("list")}</Text>
        <TextInput
          placeholder={tr("searchByService")}
          style={styles.input}
          value={searchQuery}
          onChangeText={setSearchQuery}
        />

        <Text style={styles.label}>Status filter</Text>

        <View style={styles.row}>
          {statusOptions.map((option) => (
            <TouchableOpacity
              key={option}
              style={[styles.pill, statusFilter === option ? styles.pillActive : null]}
              onPress={() => setStatusFilter(option)}
            >
              <Text>{formatStatus(option)}</Text>
            </TouchableOpacity>
          ))}
        </View>

        <Text style={styles.label}>Sort by</Text>

        <View style={styles.row}>
          <TouchableOpacity
            style={[styles.pill, sortMode === "NEXT_ASC" ? styles.pillActive : null]}
            onPress={() => setSortMode("NEXT_ASC")}
          >
            <Text>{tr("sortNextDate")}</Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={[styles.pill, sortMode === "AMOUNT_DESC" ? styles.pillActive : null]}
            onPress={() => setSortMode("AMOUNT_DESC")}
          >
            <Text>{tr("sortAmountDesc")}</Text>
          </TouchableOpacity>
        </View>

        {visibleSubscriptions.length === 0 ? <Text>{tr("noSubscriptions")}</Text> : null}
        {visibleSubscriptions.map((subscription) => (
          <View key={subscription.id} style={styles.card}>

            <Text style={styles.cardTitle}>{subscription.serviceName}</Text>
            <Text style={styles.meta}>
              {subscription.amount} {subscription.currency} | {subscription.billingPeriod}
            </Text>
            <Text style={styles.meta}>Next: {subscription.nextBillingDate}</Text>
            <Text style={styles.meta}>{tr("status")}: {subscription.status}</Text>

            <View style={styles.row}>
              <Button title={tr("details")} onPress={() => void onLoadDetails(subscription.id)} />
              <Button title={tr("edit")} onPress={() => onEdit(subscription)} />
              <Button title={tr("delete")} onPress={() => onDeletePress(subscription.id)} />
            </View>
          </View>
        ))}
      </View>

      {selectedSubscription ? (
        <View style={styles.section}>

          <Text style={styles.sectionTitle}>{tr("detailsWithId")}{selectedSubscription.id}</Text>
          <Text style={styles.meta}>{tr("service")}: {selectedSubscription.serviceName}</Text>
          <Text style={styles.meta}>{tr("category")}: {selectedSubscription.category || tr("none")}</Text>
          <Text style={styles.meta}>
            {tr("amount")}: {selectedSubscription.amount} {selectedSubscription.currency}
          </Text>
          <Text style={styles.meta}>{tr("period")}: {selectedSubscription.billingPeriod}</Text>
          <Text style={styles.meta}>{tr("status")}: {selectedSubscription.status}</Text>

          <Text style={styles.sectionTitle}>Recommendations by category</Text>
          <Text style={styles.meta}>
            Category: {selectedSubscription.category || tr("none")}
          </Text>
          {isRecommendationsLoading ? <Text style={styles.meta}>{tr("loading")}</Text> : null}
          {!isRecommendationsLoading && recommendations.length === 0 ? (
            <Text style={styles.meta}>No recommendations for this category.</Text>
          ) : null}
          {recommendations.map((item, index) => (
            <View key={`${item.currentService}-${item.alternativeService}-${index}`} style={styles.card}>
              <Text style={styles.cardTitle}>{`${item.currentService} -> ${item.alternativeService}`}</Text>
              <Text style={styles.meta}>{item.reason}</Text>
            </View>
          ))}
        </View>
      ) : null}

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
  section: {
    gap: 8,
    backgroundColor: colors.bgElevated,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 16,
    padding: 16
  },
  sectionTitle: {
    color: colors.text,
    fontWeight: "800",
    textTransform: "uppercase",
    letterSpacing: 0.6
  },
  input: {
    borderWidth: 1,
    borderColor: colors.border,
    backgroundColor: colors.bgSoft,
    color: colors.text,
    borderRadius: 12,
    paddingHorizontal: 12,
    paddingVertical: 10
  },
  row: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 8
  },
  pill: {
    borderWidth: 1,
    borderColor: colors.border,
    backgroundColor: colors.bgSoft,
    borderRadius: 999,
    paddingHorizontal: 12,
    paddingVertical: 6
  },
  pillActive: {
    backgroundColor: colors.accent,
    borderColor: colors.accent
  },
  card: {
    borderWidth: 1,
    borderColor: colors.border,
    backgroundColor: colors.card,
    borderRadius: 14,
    padding: 12,
    gap: 6
  },
  error: {

    color: colors.danger
  },
  label: {
    color: colors.text,
    fontWeight: "700"
  },
  cardTitle: {
    color: colors.text,
    fontWeight: "800",
    fontSize: 16
  },
  meta: {
    color: colors.textMuted

  }
});

export default SubscriptionsScreen;
