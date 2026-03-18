import type { DashboardResponse } from "../types/analytics";
import { httpClient } from "./httpClient";

export const dashboardApi = {
  get: async (period: "month" | "year" = "month", days = 7) => {
    const { data } = await httpClient.get<DashboardResponse>("/dashboard", {
      params: { period, days }
    });
    return data;
  }
};
