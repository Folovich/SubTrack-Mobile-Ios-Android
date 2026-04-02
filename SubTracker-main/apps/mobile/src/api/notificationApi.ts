import { httpClient } from "./httpClient";
import type { NotificationItem } from "../types/notification";

export const notificationApi = {
  getByDays: async (days = 7): Promise<NotificationItem[]> => {
    const response = await httpClient.get<NotificationItem[]>("/api/v1/notifications", {
      params: { days }
    });
    return response.data;
  }
};
