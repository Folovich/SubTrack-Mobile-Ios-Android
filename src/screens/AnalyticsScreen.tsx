import React, { useCallback, useEffect, useState } from "react";
import { ScrollView, StyleSheet, Text, TouchableOpacity, View } from "react-native";
import type { DimensionValue } from "react-native";
import { analyticsApi } from "../api/analyticsApi";
import { ApiError } from "../api/httpClient";
import AppButton from "../components/AppButton";
import { useI18n } from "../context/SettingsContext";
import type {
  AnalyticsByCategory,
  AnalyticsCategoryItem,
  AnalyticsForecast,
  AnalyticsPeriod,
  AnalyticsSummary
} from "../types/analytics";
import type { AppPalette } from "../theme/theme";

const chartPalette = ["#f5a623", "#ffca57", "#54cbff", "#34d399", "#f97316", "#8b5cf6"];

const formatAmount = (amount: number, currency?: string | null) => {
  const normalizedCurrency = currency ?? "USD";
  return `${amount.toFixed(2)} ${normalizedCurrency}`;
};

const AnalyticsScreen = () => {
  const { tr, colors } = useI18n();
  const styles = createStyles(colors);
  const [period, setPeriod] = useState<AnalyticsPeriod>("month");
  const [summary, setSummary] = useState<AnalyticsSummary | null>(null);
  const [byCategory, setByCategory] = useState<AnalyticsByCategory | null>(null);
  const [forecast, setForecast] = useState<AnalyticsForecast | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadAnalytics = useCallback(async (targetPeriod: AnalyticsPeriod) => {
    try {
      setIsLoading(true);
      setError(null);
      const [summaryResponse, byCategoryResponse, forecastResponse] = await Promise.all([
        analyticsApi.getSummary(targetPeriod),
        analyticsApi.getByCategory(targetPeriod),
        analyticsApi.getForecast()
      ]);
      setSummary(summaryResponse);
      setByCategory(byCategoryResponse);
      setForecast(forecastResponse);
    } catch (loadError) {
      setError(
        loadError instanceof ApiError && loadError.status === 400
          ? tr("analyticsPeriodError")
          : loadError instanceof Error
            ? loadError.message
            : tr("failedLoadAnalytics")
      );
    } finally {
      setIsLoading(false);
    }
  }, [tr]);

  useEffect(() => {
    void loadAnalytics(period);
  }, [loadAnalytics, period]);

  const categoryItems = byCategory?.items ?? [];
  const maxCategoryAmount = categoryItems.reduce((max, item) => Math.max(max, item.amount), 0);
  const topCategory = categoryItems[0];
  const topVisualItems = categoryItems.slice(0, 5);

  return (
    <ScrollView contentContainerStyle={styles.container}>
      <View style={styles.hero}>
        <Text style={styles.eyebrow}>{tr("analyticsTitle")}</Text>
        <Text style={styles.title}>{tr("analyticsTitle")}</Text>
        <Text style={styles.subtitle}>
          {summary
            ? `${summary.from} - ${summary.to}`
            : tr("analyticsSubtitle")}
        </Text>
      </View>

      <View style={styles.segmentRow}>
        {(["month", "year"] as const).map((option) => (
          <TouchableOpacity
            key={option}
            style={[styles.segment, period === option ? styles.segmentActive : null]}
            onPress={() => setPeriod(option)}
          >
            <Text style={[styles.segmentText, period === option ? styles.segmentTextActive : null]}>
              {option === "month" ? tr("month") : tr("year")}
            </Text>
          </TouchableOpacity>
        ))}
      </View>

      {isLoading ? <Text style={styles.meta}>{tr("loading")}</Text> : null}
      {error ? <Text style={styles.error}>{error}</Text> : null}

      {summary ? (
        <View style={styles.metricsGrid}>
          <View style={[styles.metricCard, styles.metricCardWide]}>
            <Text style={styles.metricLabel}>{tr("total")}</Text>
            <Text style={styles.metricValue}>{formatAmount(summary.totalAmount, summary.currency)}</Text>
            <Text style={styles.metricHint}>
              {tr("period")}: {summary.period}
            </Text>
          </View>
          <View style={styles.metricCard}>
            <Text style={styles.metricLabel}>{tr("activeSubscriptions")}</Text>
            <Text style={styles.metricValue}>{summary.activeSubscriptions}</Text>
          </View>
          <View style={styles.metricCard}>
            <Text style={styles.metricLabel}>{tr("analyticsTopCategory")}</Text>
            <Text style={styles.metricValueSmall}>{topCategory?.category ?? "-"}</Text>
            <Text style={styles.metricHint}>
              {topCategory ? `${topCategory.sharePercent}% ${tr("analyticsShareOfTotal")}` : tr("noCategories")}
            </Text>
          </View>
        </View>
      ) : null}

      {byCategory ? (
        <View style={styles.section}>
          <View style={styles.sectionHeader}>
            <Text style={styles.sectionTitle}>{tr("byCategory")}</Text>
            <Text style={styles.sectionMeta}>
              {formatAmount(byCategory.totalAmount, byCategory.currency)}
            </Text>
          </View>

          {categoryItems.length === 0 ? <Text style={styles.meta}>{tr("noCategories")}</Text> : null}

          {categoryItems.length > 0 ? (
            <View style={styles.stackBar}>
              {categoryItems.map((item, index) => (
                <View
                  key={`${item.category}-stack`}
                  style={[
                    styles.stackSegment,
                    {
                      backgroundColor: chartPalette[index % chartPalette.length],
                      flex: Math.max(item.sharePercent, 4)
                    }
                  ]}
                />
              ))}
            </View>
          ) : null}

          {topVisualItems.length > 0 ? (
            <View style={styles.visualPanel}>
              <View style={styles.visualHeader}>
                <Text style={styles.visualTitle}>{tr("analyticsExpenseChart")}</Text>
                <Text style={styles.visualMeta}>
                  {tr("analyticsTopCategories")}: {topVisualItems.length}
                </Text>
              </View>
              <View style={styles.columnsWrap}>
                {topVisualItems.map((item, index) => {
                  const columnHeight = maxCategoryAmount > 0 ? (item.amount / maxCategoryAmount) * 140 : 12;
                  return (
                    <View key={`${item.category}-column`} style={styles.columnItem}>
                      <Text style={styles.columnValue}>{Math.round(item.sharePercent)}%</Text>
                      <View style={styles.columnTrack}>
                        <View
                          style={[
                            styles.columnFill,
                            {
                              height: Math.max(columnHeight, 12),
                              backgroundColor: chartPalette[index % chartPalette.length]
                            }
                          ]}
                        />
                      </View>
                      <Text style={styles.columnLabel} numberOfLines={2}>
                        {item.category}
                      </Text>
                    </View>
                  );
                })}
              </View>
            </View>
          ) : null}

          {categoryItems.map((item, index) => {
            const width: DimensionValue =
              maxCategoryAmount > 0 ? `${(item.amount / maxCategoryAmount) * 100}%` : "0%";
            return (
              <View key={item.category} style={styles.chartRow}>
                <View style={styles.chartLabelRow}>
                  <View style={styles.categoryHeading}>
                    <View
                      style={[
                        styles.dot,
                        { backgroundColor: chartPalette[index % chartPalette.length] }
                      ]}
                    />
                    <Text style={styles.chartLabel}>{item.category}</Text>
                  </View>
                  <Text style={styles.chartValue}>
                    {formatAmount(item.amount, byCategory.currency)}
                  </Text>
                </View>
                <View style={styles.track}>
                  <View
                    style={[
                      styles.fill,
                      {
                        width,
                        backgroundColor: chartPalette[index % chartPalette.length]
                      }
                    ]}
                  />
                </View>
                <View style={styles.chartMetaRow}>
                  <Text style={styles.meta}>{item.sharePercent}%</Text>
                  <Text style={styles.meta}>{item.subscriptionsCount} {tr("subscriptionsShort")}</Text>
                </View>
              </View>
            );
          })}
        </View>
      ) : null}

      {forecast ? (
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>{tr("forecast")}</Text>
          <View style={styles.forecastGrid}>
            <View style={styles.forecastCard}>
              <Text style={styles.forecastLabel}>{tr("month")}</Text>
              <Text style={styles.forecastValue}>
                {formatAmount(forecast.monthForecast, forecast.currency)}
              </Text>
            </View>
            <View style={styles.forecastCard}>
              <Text style={styles.forecastLabel}>{tr("year")}</Text>
              <Text style={styles.forecastValue}>
                {formatAmount(forecast.yearForecast, forecast.currency)}
              </Text>
            </View>
          </View>
        </View>
      ) : null}

      <AppButton fullWidth title={tr("reload")} onPress={() => void loadAnalytics(period)} />
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
      borderRadius: 14,
      borderWidth: 1,
      borderColor: colors.border,
      backgroundColor: colors.bgElevated,
      alignItems: "center",
      paddingVertical: 12
    },
    segmentActive: {
      backgroundColor: colors.accent,
      borderColor: colors.accent
    },
    segmentText: {
      color: colors.text,
      fontWeight: "800",
      letterSpacing: 0.5,
      textTransform: "uppercase"
    },
    segmentTextActive: {
      color: colors.accentText
    },
    metricsGrid: {
      gap: 10
    },
    metricCard: {
      gap: 6,
      padding: 16,
      borderRadius: 18,
      borderWidth: 1,
      borderColor: colors.border,
      backgroundColor: colors.bgElevated
    },
    metricCardWide: {
      backgroundColor: colors.card
    },
    metricLabel: {
      color: colors.textMuted,
      fontSize: 12,
      fontWeight: "800",
      textTransform: "uppercase",
      letterSpacing: 0.8
    },
    metricValue: {
      color: colors.text,
      fontSize: 28,
      fontWeight: "900"
    },
    metricValueSmall: {
      color: colors.text,
      fontSize: 22,
      fontWeight: "900"
    },
    metricHint: {
      color: colors.textMuted
    },
    section: {
      gap: 12,
      backgroundColor: colors.bgElevated,
      borderRadius: 20,
      borderWidth: 1,
      borderColor: colors.border,
      padding: 16
    },
    sectionHeader: {
      flexDirection: "row",
      alignItems: "center",
      justifyContent: "space-between",
      gap: 12
    },
    sectionTitle: {
      color: colors.text,
      fontWeight: "800",
      textTransform: "uppercase",
      letterSpacing: 0.8
    },
    sectionMeta: {
      color: colors.accent,
      fontWeight: "800"
    },
    stackBar: {
      flexDirection: "row",
      height: 14,
      overflow: "hidden",
      borderRadius: 999,
      backgroundColor: colors.bgSoft
    },
    stackSegment: {
      height: "100%"
    },
    visualPanel: {
      gap: 12,
      padding: 14,
      borderRadius: 18,
      backgroundColor: colors.card,
      borderWidth: 1,
      borderColor: colors.border
    },
    visualHeader: {
      flexDirection: "row",
      alignItems: "center",
      justifyContent: "space-between",
      gap: 12
    },
    visualTitle: {
      color: colors.text,
      fontWeight: "800",
      textTransform: "uppercase",
      letterSpacing: 0.8
    },
    visualMeta: {
      color: colors.textMuted,
      fontSize: 12
    },
    columnsWrap: {
      flexDirection: "row",
      alignItems: "flex-end",
      justifyContent: "space-between",
      gap: 10,
      minHeight: 190
    },
    columnItem: {
      flex: 1,
      alignItems: "center",
      gap: 8
    },
    columnValue: {
      color: colors.textMuted,
      fontSize: 11,
      fontWeight: "700"
    },
    columnTrack: {
      width: "100%",
      maxWidth: 42,
      height: 140,
      borderRadius: 14,
      backgroundColor: colors.bgSoft,
      justifyContent: "flex-end",
      overflow: "hidden"
    },
    columnFill: {
      width: "100%",
      borderRadius: 14
    },
    columnLabel: {
      color: colors.text,
      fontSize: 11,
      fontWeight: "700",
      textAlign: "center"
    },
    chartRow: {
      gap: 8
    },
    chartLabelRow: {
      flexDirection: "row",
      justifyContent: "space-between",
      alignItems: "center",
      gap: 10
    },
    categoryHeading: {
      flexDirection: "row",
      alignItems: "center",
      gap: 8,
      flexShrink: 1
    },
    dot: {
      width: 10,
      height: 10,
      borderRadius: 999
    },
    chartLabel: {
      color: colors.text,
      fontWeight: "700",
      flexShrink: 1
    },
    chartValue: {
      color: colors.text,
      fontWeight: "800"
    },
    track: {
      height: 12,
      borderRadius: 999,
      overflow: "hidden",
      backgroundColor: colors.bgSoft
    },
    fill: {
      height: "100%",
      borderRadius: 999,
      minWidth: 6
    },
    chartMetaRow: {
      flexDirection: "row",
      justifyContent: "space-between",
      alignItems: "center"
    },
    forecastGrid: {
      gap: 10
    },
    forecastCard: {
      padding: 14,
      borderRadius: 16,
      backgroundColor: colors.bgSoft,
      borderWidth: 1,
      borderColor: colors.border,
      gap: 6
    },
    forecastLabel: {
      color: colors.textMuted,
      textTransform: "uppercase",
      letterSpacing: 0.7,
      fontWeight: "800"
    },
    forecastValue: {
      color: colors.accent,
      fontSize: 24,
      fontWeight: "900"
    },
    meta: {
      color: colors.textMuted
    },
    error: {
      color: colors.danger
    }
  });

export default AnalyticsScreen;
