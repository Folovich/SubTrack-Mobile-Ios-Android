import type { ImportConsentProvider, ImportConsentStatus } from "../types/import";
import { httpClient } from "./httpClient";

export const consentApi = {
  getImportConsentStatus: async (provider: ImportConsentProvider) => {
    const { data } = await httpClient.get<ImportConsentStatus>(`/consents/imports/${provider}`);
    return data;
  },
  grantImportConsent: async (provider: ImportConsentProvider) => {
    const { data } = await httpClient.post<ImportConsentStatus>(`/consents/imports/${provider}/grant`);
    return data;
  },
  revokeImportConsent: async (provider: ImportConsentProvider) => {
    const { data } = await httpClient.post<ImportConsentStatus>(`/consents/imports/${provider}/revoke`);
    return data;
  }
};
