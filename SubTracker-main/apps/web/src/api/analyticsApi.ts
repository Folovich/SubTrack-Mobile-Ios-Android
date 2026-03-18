import type {
  AnalyticsByCategory,
  AnalyticsForecast,
  AnalyticsPeriod,
  AnalyticsSummary
} from "../types/analytics";
import { httpClient } from "./httpClient";

export const analyticsApi = {
  summary: async (period: AnalyticsPeriod) => {
    const { data } = await httpClient.get<AnalyticsSummary>("/analytics/summary", {
      params: { period }
    });
    return data;
  },
  byCategory: async (period: AnalyticsPeriod) => {
    const { data } = await httpClient.get<AnalyticsByCategory>("/analytics/by-category", {
      params: { period }
    });
    return data;
  },
  forecast: async () => {
    const { data } = await httpClient.get<AnalyticsForecast>("/analytics/forecast");
    return data;
  }
};
