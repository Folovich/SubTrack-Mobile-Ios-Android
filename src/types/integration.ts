export interface IntegrationConnection {
  id: number | null;
  provider: string;
  status: string;
  externalAccountEmail?: string | null;
  connectedAt?: string | null;
  updatedAt?: string | null;
  lastSyncAt?: string | null;
  lastErrorCode?: string | null;
  lastErrorMessage?: string | null;
}
