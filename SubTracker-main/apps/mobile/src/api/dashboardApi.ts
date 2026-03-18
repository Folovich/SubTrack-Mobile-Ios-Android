import { httpClient } from "./httpClient";
import type { DashboardResponse } from "../types/dashboard";
import type { AnalyticsPeriod } from "../types/analytics";

export const dashboardApi = {
  getDashboard: async (period: AnalyticsPeriod = "month", days = 7): Promise<DashboardResponse> => {
    const response = await httpClient.get<DashboardResponse>("/api/v1/dashboard", {
      params: { period, days }
    });
    return response.data;
  }
};
