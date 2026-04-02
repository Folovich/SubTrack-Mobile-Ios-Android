import type { AnalyticsPeriod } from "./analytics";

export interface UsageSignal {
  id: number;
  subscriptionId: number;
  signalType: string;
  value: string;
  createdAt: string;
}

export interface UsageSignalCreateRequest {
  subscriptionId: number;
  signalType: string;
  value: string;
}

export interface AnalyticsUsageItem {
  subscriptionId: number;
  serviceName: string;
  category: string;
  signalsCount: number;
  lastSignalAt: string | null;
}

export interface AnalyticsUsageResponse {
  period: AnalyticsPeriod;
  from: string;
  to: string;
  totalSignals: number;
  activeSubscriptions: number;
  subscriptionsWithSignals: number;
  items: AnalyticsUsageItem[];
}
