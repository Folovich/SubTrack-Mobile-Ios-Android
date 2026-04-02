import { httpClient } from "./httpClient";
import type { AnalyticsByCategory, AnalyticsForecast, AnalyticsPeriod, AnalyticsSummary } from "../types/analytics";

export const analyticsApi = {
  getSummary: async (period: AnalyticsPeriod): Promise<AnalyticsSummary> => {
    const response = await httpClient.get<AnalyticsSummary>("/api/v1/analytics/summary", {
      params: { period }
    });
    return response.data;
  },
  getByCategory: async (period: AnalyticsPeriod): Promise<AnalyticsByCategory> => {
    const response = await httpClient.get<AnalyticsByCategory>("/api/v1/analytics/by-category", {
      params: { period }
    });
    return response.data;
  },
  getForecast: async (): Promise<AnalyticsForecast> => {
    const response = await httpClient.get<AnalyticsForecast>("/api/v1/analytics/forecast");
    return response.data;
  }
};
