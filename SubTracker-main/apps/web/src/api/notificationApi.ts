import type { Notification } from "../types/notification";
import { httpClient } from "./httpClient";

export const notificationApi = {
  list: async (days = 7) => {
    const { data } = await httpClient.get<Notification[]>("/notifications", {
      params: { days }
    });
    return data;
  }
};
