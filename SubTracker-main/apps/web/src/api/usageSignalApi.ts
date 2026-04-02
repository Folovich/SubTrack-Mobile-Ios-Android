import type { UsageSignal, UsageSignalCreateRequest } from "../types/usageSignal";
import { httpClient } from "./httpClient";

export const usageSignalApi = {
  list: async (subscriptionId?: number | null) => {
    const { data } = await httpClient.get<UsageSignal[]>("/usage-signals", {
      params: subscriptionId ? { subscriptionId } : undefined
    });
    return data;
  },
  create: async (payload: UsageSignalCreateRequest) => {
    const { data } = await httpClient.post<UsageSignal>("/usage-signals", payload);
    return data;
  }
};
