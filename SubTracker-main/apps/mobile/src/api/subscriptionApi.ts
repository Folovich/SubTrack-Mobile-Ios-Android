import { httpClient } from "./httpClient";
import type {
  Subscription,
  SubscriptionRequest,
  SupportEmailAction,
  SupportEmailDraftResponse,
  SupportEmailEvent,
  UpcomingSubscription
} from "../types/subscription";

export const subscriptionApi = {
  getAll: async (): Promise<Subscription[]> => {
    const response = await httpClient.get<Subscription[]>("/api/v1/subscriptions");
    return response.data;
  },
  getById: async (id: number): Promise<Subscription> => {
    const response = await httpClient.get<Subscription>(`/api/v1/subscriptions/${id}`);
    return response.data;
  },
  create: async (payload: SubscriptionRequest): Promise<Subscription> => {
    const response = await httpClient.post<Subscription>("/api/v1/subscriptions", payload);
    return response.data;
  },
  update: async (id: number, payload: SubscriptionRequest): Promise<Subscription> => {
    const response = await httpClient.put<Subscription>(`/api/v1/subscriptions/${id}`, payload);
    return response.data;
  },
  remove: async (id: number): Promise<void> => {
    await httpClient.delete(`/api/v1/subscriptions/${id}`);
  },
  getUpcoming: async (days = 7): Promise<UpcomingSubscription[]> => {
    const response = await httpClient.get<UpcomingSubscription[]>("/api/v1/subscriptions/upcoming", {
      params: { days }
    });
    return response.data;
  },
  getSupportEmailDraft: async (
    id: number,
    action: SupportEmailAction
  ): Promise<SupportEmailDraftResponse> => {
    const response = await httpClient.get<SupportEmailDraftResponse>(
      `/api/v1/subscriptions/${id}/support-email-draft`,
      { params: { action } }
    );
    return response.data;
  },
  trackSupportEmailEvent: async (
    id: number,
    action: SupportEmailAction,
    event: SupportEmailEvent
  ): Promise<void> => {
    await httpClient.post(`/api/v1/subscriptions/${id}/support-email-events`, {
      action,
      event
    });
  }
};
