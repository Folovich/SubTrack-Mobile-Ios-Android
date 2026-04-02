import type { AnalyticsPeriod } from "../types/analytics";
import type { AnalyticsUsageResponse } from "../types/usageSignal";
import { httpClient } from "./httpClient";

export const usageInsightsApi = {
  get: async (period: AnalyticsPeriod, subscriptionId?: number | null) => {
    const { data } = await httpClient.get<AnalyticsUsageResponse>("/analytics/usage", {
      params: {
        period,
        ...(subscriptionId ? { subscriptionId } : {})
      }
    });
    return data;
  }
};
