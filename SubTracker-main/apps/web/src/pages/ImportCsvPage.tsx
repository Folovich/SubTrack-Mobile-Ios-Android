import axios from "axios";
import { useCallback, useEffect, useMemo, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { importApi } from "../api/importApi";
import { useLanguage } from "../i18n/LanguageProvider";
import type { ImportConsentStatus, ImportHistoryItem, ImportItemResult, ImportResult, IntegrationStatus } from "../types/import";

const formatDateTime = (value: string | null) => {
  if (!value) {
    return "Not available";
  }

  return new Intl.DateTimeFormat(undefined, {
    dateStyle: "medium",
    timeStyle: "short"
  }).format(new Date(value));
};

const getErrorMessage = (error: unknown) => {
  if (axios.isAxiosError<{ message?: string; errors?: Record<string, string> }>(error)) {
    const fieldError = error.response?.data?.errors
      ? Object.values(error.response.data.errors)[0]
      : null;

    return fieldError ?? error.response?.data?.message ?? "Request failed";
  }

  if (error instanceof Error) {
    return error.message;
  }

  return "Unexpected error";
};

const getItemSummary = (item: ImportItemResult) => {
  if (item.serviceName && item.amount && item.currency) {
    return `${item.serviceName} | ${item.amount.toFixed(2)} ${item.currency}`;
  }

  if (item.serviceName) {
    return item.serviceName;
  }

  return item.externalId;
};

const ImportCsvPage = () => {
  const { t } = useLanguage();
  const [searchParams, setSearchParams] = useSearchParams();
  const [consent, setConsent] = useState<ImportConsentStatus | null>(null);
  const [integration, setIntegration] = useState<IntegrationStatus | null>(null);
  const [history, setHistory] = useState<ImportHistoryItem[]>([]);
  const [result, setResult] = useState<ImportResult | null>(null);
  const [notice, setNotice] = useState<{ kind: "success" | "error"; text: string } | null>(null);
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [busyAction, setBusyAction] = useState<"connect" | "sync" | "disconnect" | "details" | null>(null);

  const loadImportPage = useCallback(async () => {
    setError("");
    setIsLoading(true);

    try {
      const [consentResponse, integrationResponse, historyResponse] = await Promise.all([
        importApi.getConsentStatus(),
        importApi.getIntegrationStatus(),
        importApi.history()
      ]);

      setConsent(consentResponse);
      setIntegration(integrationResponse);
      setHistory(historyResponse);
    } catch (loadError) {
      setError(getErrorMessage(loadError));
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadImportPage();
  }, [loadImportPage]);

  useEffect(() => {
    const gmailStatus = searchParams.get("gmail");
    if (!gmailStatus) {
      return;
    }

    if (gmailStatus === "connected") {
      setNotice({
        kind: "success",
        text: "Gmail is connected. You can start mailbox sync now."
      });
    } else {
      const reason = searchParams.get("reason");
      setNotice({
        kind: "error",
        text: reason ? `Gmail connection failed: ${reason}.` : "Gmail connection failed."
      });
    }

    setSearchParams({});
    void loadImportPage();
  }, [loadImportPage, searchParams, setSearchParams]);

  const isConnected = integration?.status === "ACTIVE";
  const needsReauth = integration?.status === "REAUTH_REQUIRED";
  const canSync = consent?.status === "GRANTED" && isConnected;

  const connectionHint = useMemo(() => {
    if (needsReauth) {
      return "Google access expired or was revoked. Reconnect Gmail before the next sync.";
    }
    if (isConnected) {
      return "Mailbox sync will fetch relevant billing emails directly from Gmail with read-only access.";
    }
    if (consent?.status === "GRANTED") {
      return "Consent exists, but mailbox is not connected yet. Finish Gmail OAuth to enable sync.";
    }
    return "Connect Gmail to grant explicit consent and import billing emails end-to-end.";
  }, [consent?.status, isConnected, needsReauth]);

  const handleConnect = async () => {
    setError("");
    setNotice(null);
    setBusyAction("connect");

    try {
      const response = await importApi.startOAuth();
      window.location.assign(response.authorizationUrl);
    } catch (connectError) {
      setError(getErrorMessage(connectError));
      setBusyAction(null);
    }
  };

  const handleDisconnect = async () => {
    setError("");
    setNotice(null);
    setBusyAction("disconnect");

    try {
      const response = await importApi.disconnect();
      setIntegration(response);
      setConsent((previous) =>
        previous
          ? {
              ...previous,
              status: "REVOKED",
              integrationStatus: response.status
            }
          : previous
      );
      setResult(null);
      await loadImportPage();
      setNotice({
        kind: "success",
        text: "Gmail was disconnected and consent was revoked."
      });
    } catch (disconnectError) {
      setError(getErrorMessage(disconnectError));
    } finally {
      setBusyAction(null);
    }
  };

  const handleSync = async () => {
    setError("");
    setNotice(null);
    setBusyAction("sync");

    try {
      const syncResult = await importApi.syncMailbox();
      setResult(syncResult);
      await loadImportPage();
      setNotice({
        kind: "success",
        text: `Mailbox sync finished with status ${syncResult.status}.`
      });
    } catch (syncError) {
      setError(getErrorMessage(syncError));
    } finally {
      setBusyAction(null);
    }
  };

  const handleLoadDetails = async (id: number) => {
    setError("");
    setBusyAction("details");

    try {
      const details = await importApi.getById(id);
      setResult(details);
    } catch (detailsError) {
      setError(getErrorMessage(detailsError));
    } finally {
      setBusyAction(null);
    }
  };

  return (
    <div className="page import-page">
      <h1 className="page__title">{t("import_title")}</h1>
      <p className="muted">
        Connect Gmail with read-only access, sync relevant billing emails, and review which subscriptions were imported,
        skipped, unsupported, or failed to parse.
      </p>

      {notice ? (
        <p className={`import-banner import-banner--${notice.kind}`}>{notice.text}</p>
      ) : null}
      {error ? <p className="form-message form-message--error">{error}</p> : null}

      <section className="section">
        <h2 className="section__title">Gmail mailbox import</h2>

        <div className="import-grid">
          <div className="card-row">
            <strong>Consent</strong>
            <span>{consent?.status ?? (isLoading ? t("common_loading") : "Unknown")}</span>
            <span>Scope: {consent?.scope ?? "Not granted yet"}</span>
            <span>Granted: {formatDateTime(consent?.grantedAt ?? null)}</span>
          </div>

          <div className="card-row">
            <strong>Connection</strong>
            <span>Status: {integration?.status ?? (isLoading ? t("common_loading") : "NOT_CONNECTED")}</span>
            <span>Mailbox: {integration?.externalAccountEmail ?? "Not connected"}</span>
            <span>Last sync: {formatDateTime(integration?.lastSyncAt ?? null)}</span>
          </div>
        </div>

        <p>{connectionHint}</p>
        {integration?.lastErrorMessage ? <p className="muted">Last Gmail error: {integration.lastErrorMessage}</p> : null}

        <div className="inline-actions import-actions">
          <button type="button" className="button" onClick={() => void handleConnect()} disabled={busyAction !== null}>
            {busyAction === "connect" ? "Redirecting..." : isConnected ? "Reconnect Gmail" : "Connect Gmail"}
          </button>
          <button
            type="button"
            className="button button--secondary"
            onClick={() => void handleSync()}
            disabled={busyAction !== null || !canSync}
          >
            {busyAction === "sync" ? "Syncing..." : "Sync mailbox"}
          </button>
          <button
            type="button"
            className="button button--secondary"
            onClick={() => void handleDisconnect()}
            disabled={busyAction !== null || (!integration?.id && !consent)}
          >
            {busyAction === "disconnect" ? "Disconnecting..." : "Disconnect"}
          </button>
        </div>
      </section>

      <section className="section">
        <h2 className="section__title">{t("import_history")}</h2>
        {history.length ? (
          <div className="stack-list">
            {history.map((item) => (
              <div key={item.id} className="card-row">
                <strong>
                  {item.provider} | {item.status}
                </strong>
                <span>
                  Started: {formatDateTime(item.startedAt)}{item.finishedAt ? ` | Finished: ${formatDateTime(item.finishedAt)}` : ""}
                </span>
                <button
                  type="button"
                  className="text-action"
                  onClick={() => void handleLoadDetails(item.id)}
                  disabled={busyAction === "details"}
                >
                  View details
                </button>
              </div>
            ))}
          </div>
        ) : (
          <p>{isLoading ? t("common_loading") : t("import_no_jobs")}</p>
        )}
      </section>

      {result ? (
        <section className="section">
          <h2 className="section__title">{t("import_last_result")}</h2>
          <div className="import-grid">
            <div className="card-row">
              <strong>{t("import_job")} #{result.jobId}</strong>
              <span>Status: {result.status}</span>
              <span>
                Processed: {result.processed} | Created: {result.created} | Skipped: {result.skipped} | Errors: {result.errors}
              </span>
            </div>
            <div className="card-row">
              <strong>Window</strong>
              <span>Started: {formatDateTime(result.startedAt)}</span>
              <span>Finished: {formatDateTime(result.finishedAt)}</span>
            </div>
          </div>

          {result.items.length ? (
            <div className="stack-list import-items">
              {result.items.map((item) => (
                <div key={`${item.externalId}-${item.status}`} className="card-row">
                  <strong>{getItemSummary(item)}</strong>
                  <span>
                    Status: <span className={`status-chip status-chip--${item.status.toLowerCase()}`}>{item.status}</span>
                  </span>
                  <span>
                    Source: {item.sourceProvider ?? "Unknown"}{item.receivedAt ? ` | Received: ${formatDateTime(item.receivedAt)}` : ""}
                  </span>
                  <span>
                    {item.billingPeriod ?? "Period unknown"}{item.nextBillingDate ? ` | Next billing: ${item.nextBillingDate}` : ""}
                  </span>
                  {item.reason ? <span>Reason: {item.reason}</span> : null}
                </div>
              ))}
            </div>
          ) : null}

          {result.errorItems.length ? (
            <div className="stack-list import-errors">
              {result.errorItems.map((item) => (
                <div key={`${item.externalId}-${item.reason}`} className="card-row">
                  <strong>{item.externalId ?? "message"}</strong>
                  <span>{item.reason}</span>
                </div>
              ))}
            </div>
          ) : null}
        </section>
      ) : null}
    </div>
  );
};

export default ImportCsvPage;
