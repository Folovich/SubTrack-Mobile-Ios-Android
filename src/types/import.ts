export interface ImportConsentStatus {
  provider: string;
  status: string;
  scope: string | null;
  grantedAt: string | null;
  revokedAt: string | null;
  integrationStatus: string | null;
}

export interface IntegrationStatus {
  id: number | null;
  provider: string;
  status: string;
  externalAccountEmail: string | null;
  connectedAt: string | null;
  updatedAt: string | null;
  lastSyncAt: string | null;
  lastErrorCode: string | null;
  lastErrorMessage: string | null;
}

export interface OAuthStartResponse {
  provider: string;
  authorizationUrl: string;
}

export interface ImportHistoryItem {
  id: number;
  provider: string;
  status: string;
  startedAt: string | null;
  finishedAt: string | null;
}

export interface ImportErrorItem {
  externalId: string | null;
  reason: string;
}

export interface ImportItemResult {
  externalId: string;
  status: string;
  reason: string | null;
  sourceProvider: string | null;
  serviceName: string | null;
  amount: number | null;
  currency: string | null;
  billingPeriod: string | null;
  nextBillingDate: string | null;
  category: string | null;
  receivedAt: string | null;
}

export interface ImportResult {
  jobId: number;
  provider: string;
  status: string;
  processed: number;
  created: number;
  skipped: number;
  errors: number;
  startedAt: string | null;
  finishedAt: string | null;
  errorItems: ImportErrorItem[];
  items: ImportItemResult[];
}
