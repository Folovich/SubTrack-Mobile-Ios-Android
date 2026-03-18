import type { UsageInsights, UsageSubscriptionItem } from "../types/usageInsights";
import { httpClient } from "./httpClient";

const toNumber = (value: unknown) => (typeof value === "number" ? value : Number(value) || 0);

const normalizeSubscription = (item: any, index: number): UsageSubscriptionItem => ({
  id: toNumber(item?.id ?? item?.subscriptionId ?? index + 1),
  serviceName: String(item?.serviceName ?? item?.name ?? `Subscription ${index + 1}`),
  signalsCount: toNumber(item?.signalsCount ?? item?.signalCount ?? item?.signals_count),
  lastSignalAt:
    typeof item?.lastSignalAt === "string"
      ? item.lastSignalAt
      : typeof item?.last_signal_at === "string"
        ? item.last_signal_at
        : null
});

export const usageInsightsApi = {
  get: async () => {
    const { data } = await httpClient.get<any>("/analytics/usage-signals");

    const metricsSource = data?.metrics ?? data?.topMetrics ?? data ?? {};
    const subscriptionsSource = Array.isArray(data?.subscriptions)
      ? data.subscriptions
      : Array.isArray(data?.items)
        ? data.items
        : [];

    const normalized: UsageInsights = {
      metrics: {
        totalSignals: toNumber(
          metricsSource?.totalSignals ?? metricsSource?.total_signals ?? metricsSource?.signalsTotal
        ),
        activeSubscriptions: toNumber(
          metricsSource?.activeSubscriptions ??
            metricsSource?.active_subscriptions ??
            metricsSource?.subscriptionsActive
        ),
        averageSignalsPerSubscription: toNumber(
          metricsSource?.averageSignalsPerSubscription ??
            metricsSource?.avgSignalsPerSubscription ??
            metricsSource?.avg_signals_per_subscription
        )
      },
      subscriptions: subscriptionsSource.map(normalizeSubscription)
    };

    return normalized;
  }
};
