import type {
  SupportEmailAction,
  SupportEmailDraftResponse,
  SupportEmailEvent,
  Subscription,
  SubscriptionPayload,
  UpcomingSubscription
} from "../types/subscription";
import { httpClient } from "./httpClient";

export const subscriptionApi = {
  list: async () => {
    const { data } = await httpClient.get<Subscription[]>("/subscriptions");
    return data;
  },
  upcoming: async (days = 7) => {
    const { data } = await httpClient.get<UpcomingSubscription[]>("/subscriptions/upcoming", {
      params: { days }
    });
    return data;
  },
  create: async (payload: SubscriptionPayload) => {
    const { data } = await httpClient.post<Subscription>("/subscriptions", payload);
    return data;
  },
  update: async (id: number, payload: SubscriptionPayload) => {
    const { data } = await httpClient.put<Subscription>(`/subscriptions/${id}`, payload);
    return data;
  },
  getSupportEmailDraft: async (id: number, action: SupportEmailAction) => {
    const { data } = await httpClient.get<SupportEmailDraftResponse>(
      `/subscriptions/${id}/support-email-draft`,
      { params: { action } }
    );
    return data;
  },
  trackSupportEmailEvent: async (id: number, action: SupportEmailAction, event: SupportEmailEvent) => {
    await httpClient.post(`/subscriptions/${id}/support-email-events`, {
      action,
      event
    });
  },
  remove: async (id: number) => {
    await httpClient.delete(`/subscriptions/${id}`);
  }
};
