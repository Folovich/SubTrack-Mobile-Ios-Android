export interface UsageMetricSet {
  totalSignals: number;
  activeSubscriptions: number;
  averageSignalsPerSubscription: number;
}

export interface UsageSubscriptionItem {
  id: number;
  serviceName: string;
  signalsCount: number;
  lastSignalAt: string | null;
}

export interface UsageInsights {
  metrics: UsageMetricSet;
  subscriptions: UsageSubscriptionItem[];
}
