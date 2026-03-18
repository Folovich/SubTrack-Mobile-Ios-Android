import type {
  ImportConsentStatus,
  ImportHistoryItem,
  ImportResult,
  IntegrationStatus,
  OAuthStartResponse
} from "../types/import";
import { httpClient } from "./httpClient";

const PROVIDER = "GMAIL";

export const importApi = {
  getConsentStatus: async () => {
    const { data } = await httpClient.get<ImportConsentStatus>(`/consents/imports/${PROVIDER}`);
    return data;
  },
  getIntegrationStatus: async () => {
    const { data } = await httpClient.get<IntegrationStatus>(`/integrations/${PROVIDER}`);
    return data;
  },
  startOAuth: async () => {
    const { data } = await httpClient.post<OAuthStartResponse>(`/integrations/${PROVIDER}/oauth/start`);
    return data;
  },
  disconnect: async () => {
    const { data } = await httpClient.post<IntegrationStatus>(`/integrations/${PROVIDER}/disconnect`);
    return data;
  },
  syncMailbox: async () => {
    const { data } = await httpClient.post<ImportResult>(`/imports/${PROVIDER}/sync`);
    return data;
  },
  history: async () => {
    const { data } = await httpClient.get<ImportHistoryItem[]>("/imports");
    return data;
  },
  getById: async (id: number) => {
    const { data } = await httpClient.get<ImportResult>(`/imports/${id}`);
    return data;
  }
};
