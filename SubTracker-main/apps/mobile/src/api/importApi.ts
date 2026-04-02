import { httpClient } from "./httpClient";
import type {
  ImportConsentStatus,
  ImportHistoryItem,
  ImportResult,
  IntegrationStatus,
  OAuthStartResponse
} from "../types/import";

const PROVIDER = "GMAIL";

export const importApi = {
  getConsentStatus: async (): Promise<ImportConsentStatus> => {
    const response = await httpClient.get<ImportConsentStatus>(`/api/v1/consents/imports/${PROVIDER}`);
    return response.data;
  },
  getIntegrationStatus: async (): Promise<IntegrationStatus> => {
    const response = await httpClient.get<IntegrationStatus>(`/api/v1/integrations/${PROVIDER}`);
    return response.data;
  },
  startOAuth: async (): Promise<OAuthStartResponse> => {
    const response = await httpClient.post<OAuthStartResponse>(`/api/v1/integrations/${PROVIDER}/oauth/start`);
    return response.data;
  },
  disconnect: async (): Promise<IntegrationStatus> => {
    const response = await httpClient.post<IntegrationStatus>(`/api/v1/integrations/${PROVIDER}/disconnect`);
    return response.data;
  },
  syncMailbox: async (): Promise<ImportResult> => {
    const response = await httpClient.post<ImportResult>(`/api/v1/imports/${PROVIDER}/sync`);
    return response.data;
  },
  getHistory: async (): Promise<ImportHistoryItem[]> => {
    const response = await httpClient.get<ImportHistoryItem[]>("/api/v1/imports");
    return response.data;
  },
  getById: async (id: number): Promise<ImportResult> => {
    const response = await httpClient.get<ImportResult>(`/api/v1/imports/${id}`);
    return response.data;
  }
};
