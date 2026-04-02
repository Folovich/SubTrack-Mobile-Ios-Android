import { httpClient } from "./httpClient";

export type ImportConsentProvider = "GMAIL";
export type ImportConsentState = "GRANTED" | "REVOKED" | "NOT_GRANTED";

export interface ImportConsentStatus {
  provider: ImportConsentProvider;
  status: ImportConsentState;
  scope?: string;
  grantedAt?: string | null;
  revokedAt?: string | null;
  integrationStatus?: string;
}

export const consentApi = {
  getImportConsentStatus: async (provider: ImportConsentProvider): Promise<ImportConsentStatus> => {
    const response = await httpClient.get<ImportConsentStatus>(`/api/v1/consents/imports/${provider}`);
    return response.data;
  },
  grantImportConsent: async (provider: ImportConsentProvider): Promise<ImportConsentStatus> => {
    const response = await httpClient.post<ImportConsentStatus>(`/api/v1/consents/imports/${provider}/grant`);
    return response.data;
  },
  revokeImportConsent: async (provider: ImportConsentProvider): Promise<ImportConsentStatus> => {
    const response = await httpClient.post<ImportConsentStatus>(`/api/v1/consents/imports/${provider}/revoke`);
    return response.data;
  }
};
