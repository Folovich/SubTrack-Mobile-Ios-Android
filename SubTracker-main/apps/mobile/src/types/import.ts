export type ImportProvider = "GMAIL";
export type ImportStatus = "IN_PROGRESS" | "COMPLETED" | "COMPLETED_WITH_ERRORS" | "FAILED";

export interface MailMessageRequest {
  externalId: string;
  from: string;
  subject: string;
  body: string;
  receivedAt: string;
}

export interface ImportStartRequest {
  provider: ImportProvider;
  messages: MailMessageRequest[];
}

export interface ImportErrorItem {
  externalId: string;
  reason: string;
}

export interface ImportResult {
  jobId: number;
  provider: string;
  status: ImportStatus;
  processed: number;
  created: number;
  skipped: number;
  errors: number;
  startedAt?: string | null;
  finishedAt?: string | null;
  errorItems: ImportErrorItem[];
}

export interface ImportHistoryItem {
  id: number;
  provider: string;
  status: ImportStatus;
  startedAt?: string | null;
  finishedAt?: string | null;
}
