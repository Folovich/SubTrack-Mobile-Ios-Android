import axios from "axios";
import { useCallback, useEffect, useMemo, useState, type ChangeEvent } from "react";
import { useSearchParams } from "react-router-dom";
import { importApi } from "../api/importApi";
import { useLanguage } from "../i18n/LanguageProvider";
import type {
  ImportConsentStatus,
  ImportHistoryItem,
  ImportItemResult,
  ImportResult,
  IntegrationStatus,
  MailMessageRequest
} from "../types/import";

const formatDateTime = (value: string | null) => {
  if (!value) {
    return "";
  }

  return new Intl.DateTimeFormat(undefined, {
    dateStyle: "medium",
    timeStyle: "short"
  }).format(new Date(value));
};

const getErrorMessage = (error: unknown) => {
  if (axios.isAxiosError<{ message?: string; errors?: Record<string, string>; code?: string }>(error)) {
    const fieldError = error.response?.data?.errors
      ? Object.values(error.response.data.errors)[0]
      : null;

    return fieldError ?? error.response?.data?.message ?? error.response?.data?.code ?? "Request failed";
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

const createMessageDraft = () => ({
  externalId: `manual-${Date.now()}`,
  from: "billing@example.com",
  subject: "Subscription renewal",
  body: "",
  receivedAt: new Date().toISOString()
});

type ConsentModalAction = "grant" | "connect";

const GMAIL_READONLY_SCOPE = "https://www.googleapis.com/auth/gmail.readonly";

const ImportCsvPage = () => {
  const { t } = useLanguage();
  const [searchParams, setSearchParams] = useSearchParams();
  const [consent, setConsent] = useState<ImportConsentStatus | null>(null);
  const [integration, setIntegration] = useState<IntegrationStatus | null>(null);
  const [history, setHistory] = useState<ImportHistoryItem[]>([]);
  const [result, setResult] = useState<ImportResult | null>(null);
  const [messages, setMessages] = useState<MailMessageRequest[]>([createMessageDraft()]);
  const [notice, setNotice] = useState<{ kind: "success" | "error"; text: string } | null>(null);
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [consentModalAction, setConsentModalAction] = useState<ConsentModalAction | null>(null);
  const [busyAction, setBusyAction] = useState<
    "connect" | "sync" | "disconnect" | "grant" | "revoke" | "start" | "details" | null
  >(null);

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
        text: t("import_connected_notice")
      });
    } else {
      const reason = searchParams.get("reason");
      setNotice({
        kind: "error",
        text: reason
          ? t("import_connection_failed_reason").replace("{reason}", reason)
          : t("import_connection_failed")
      });
    }

    setSearchParams({});
    void loadImportPage();
  }, [loadImportPage, searchParams, setSearchParams]);

  const isConnected = integration?.status === "ACTIVE";
  const needsReauth = integration?.status === "REAUTH_REQUIRED";
  const hasConsent = consent?.status === "GRANTED";
  const canSync = hasConsent && isConnected;
  const canStartImport = hasConsent && messages.every((item) => item.body.trim() && item.subject.trim() && item.from.trim());
  const isConsentModalBusy = busyAction === "grant" || busyAction === "connect";
  const requestedScope = consent?.scope ?? GMAIL_READONLY_SCOPE;

  const connectionHint = useMemo(() => {
    if (needsReauth) {
      return t("import_hint_reauth");
    }
    if (isConnected) {
      return t("import_hint_connected");
    }
    if (hasConsent) {
      return t("import_hint_consent_only");
    }
    return t("import_hint_default");
  }, [hasConsent, isConnected, needsReauth, t]);

  const updateMessage =
    (index: number, field: keyof MailMessageRequest) =>
    (event: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
      const value = event.target.value;
      setMessages((current) =>
        current.map((message, messageIndex) =>
          messageIndex === index ? { ...message, [field]: value } : message
        )
      );
    };

  const addMessage = () => {
    setMessages((current) => [...current, createMessageDraft()]);
  };

  const removeMessage = (index: number) => {
    setMessages((current) => (current.length === 1 ? current : current.filter((_, messageIndex) => messageIndex !== index)));
  };

  const openConsentModal = (action: ConsentModalAction) => {
    setError("");
    setNotice(null);
    setConsentModalAction(action);
  };

  const closeConsentModal = () => {
    if (isConsentModalBusy) {
      return;
    }

    setConsentModalAction(null);
  };

  const startOAuthRedirect = async () => {
    const response = await importApi.startOAuth();
    setConsentModalAction(null);
    window.location.assign(response.authorizationUrl);
  };

  const handleConnect = async () => {
    setError("");
    setNotice(null);

    if (!hasConsent) {
      openConsentModal("connect");
      return;
    }

    setBusyAction("connect");

    try {
      await startOAuthRedirect();
    } catch (connectError) {
      setError(getErrorMessage(connectError));
    } finally {
      setBusyAction(null);
    }
  };

  const handleGrantConsent = async (continueWithConnect = false) => {
    setError("");
    setNotice(null);
    setBusyAction("grant");

    try {
      const response = await importApi.grantConsent();
      setConsent(response);

      if (continueWithConnect) {
        setBusyAction("connect");
        await startOAuthRedirect();
        return;
      }

      setConsentModalAction(null);
      setNotice({ kind: "success", text: t("import_granted_notice") });
    } catch (grantError) {
      setError(getErrorMessage(grantError));
    } finally {
      setBusyAction(null);
    }
  };

  const handleRevokeConsent = async () => {
    setError("");
    setNotice(null);
    setBusyAction("revoke");

    try {
      const response = await importApi.revokeConsent();
      setConsent(response);
      setNotice({ kind: "success", text: t("import_revoked_notice") });
    } catch (revokeError) {
      setError(getErrorMessage(revokeError));
    } finally {
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
              integrationStatus: response.status
            }
          : previous
      );
      setResult(null);
      await loadImportPage();
      setNotice({
        kind: "success",
        text: t("import_disconnected_notice")
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
        text: t("import_sync_finished_notice").replace("{status}", syncResult.status)
      });
    } catch (syncError) {
      setError(getErrorMessage(syncError));
    } finally {
      setBusyAction(null);
    }
  };

  const handleStartImport = async () => {
    setError("");
    setNotice(null);
    setBusyAction("start");

    try {
      const startResult = await importApi.start({
        provider: "GMAIL",
        messages: messages.map((message) => ({
          ...message,
          externalId: message.externalId.trim() || `manual-${Date.now()}`,
          from: message.from.trim(),
          subject: message.subject.trim(),
          body: message.body.trim()
        }))
      });
      setResult(startResult);
      await loadImportPage();
      setNotice({
        kind: "success",
        text: t("import_stable_finished_notice").replace("{status}", startResult.status)
      });
    } catch (startError) {
      setError(getErrorMessage(startError));
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

      {notice ? (
        <p className={`import-banner import-banner--${notice.kind}`}>{notice.text}</p>
      ) : null}
      {error ? <p className="form-message form-message--error">{error}</p> : null}

      <section className="section">
        <h2 className="section__title">{t("import_consent_mailbox")}</h2>

        <div className="import-grid">
          <div className="card-row">
            <strong>{t("import_consent")}</strong>
            <span>{t("import_status")}: {consent?.status ?? (isLoading ? t("common_loading") : t("import_unknown_source"))}</span>
            <span>{t("import_scope")}: {consent?.scope ?? t("import_not_granted_yet")}</span>
            <span>{t("import_granted")}: {formatDateTime(consent?.grantedAt ?? null) || t("import_not_available")}</span>
            <span>{t("import_revoked")}: {formatDateTime(consent?.revokedAt ?? null) || t("import_not_available")}</span>
          </div>

          <div className="card-row">
            <strong>{t("import_connection")}</strong>
            <span>{t("import_status")}: {integration?.status ?? (isLoading ? t("common_loading") : "NOT_CONNECTED")}</span>
            <span>{t("import_mailbox")}: {integration?.externalAccountEmail ?? t("import_not_connected")}</span>
            <span>{t("integrations_last_sync")}: {formatDateTime(integration?.lastSyncAt ?? null) || t("import_not_available")}</span>
          </div>
        </div>

        <p>{connectionHint}</p>
        {integration?.lastErrorMessage ? <p className="muted">{t("import_last_gmail_error")}: {integration.lastErrorMessage}</p> : null}

        <div className="inline-actions import-actions">
          <button
            type="button"
            className="button button--secondary"
            onClick={() => openConsentModal("grant")}
            disabled={busyAction !== null || hasConsent}
          >
            {busyAction === "grant" ? t("import_granting") : t("import_grant")}
          </button>
          <button
            type="button"
            className="button button--secondary"
            onClick={() => void handleRevokeConsent()}
            disabled={busyAction !== null}
          >
            {busyAction === "revoke" ? t("import_revoking") : t("import_revoke")}
          </button>
          <button type="button" className="button" onClick={() => void handleConnect()} disabled={busyAction !== null}>
            {busyAction === "connect" ? t("import_redirecting") : isConnected ? t("import_reconnect_gmail") : t("import_connect_gmail")}
          </button>
          <button
            type="button"
            className="button button--secondary"
            onClick={() => void handleSync()}
            disabled={busyAction !== null || !canSync}
          >
            {busyAction === "sync" ? t("import_syncing") : t("import_sync_mailbox")}
          </button>
          <button
            type="button"
            className="button button--secondary"
            onClick={() => void handleDisconnect()}
            disabled={busyAction !== null || (!integration?.id && !consent)}
          >
            {busyAction === "disconnect" ? t("import_disconnecting") : t("import_disconnect")}
          </button>
        </div>
      </section>

      <section className="section">
        <h2 className="section__title">{t("import_stable_start")}</h2>

        <div className="stack-list">
          {messages.map((message, index) => (
            <div key={`${message.externalId}-${index}`} className="card-row">
              <strong>{t("import_message")} #{index + 1}</strong>
              <input className="input" value={message.externalId} onChange={updateMessage(index, "externalId")} />
              <input className="input" value={message.from} onChange={updateMessage(index, "from")} />
              <input className="input" value={message.subject} onChange={updateMessage(index, "subject")} />
              <textarea
                className="input input--textarea"
                value={message.body}
                onChange={updateMessage(index, "body")}
              />
              <input className="input" value={message.receivedAt} onChange={updateMessage(index, "receivedAt")} />
              <button
                type="button"
                className="text-action"
                onClick={() => removeMessage(index)}
                disabled={messages.length === 1}
              >
                {t("import_remove_message")}
              </button>
            </div>
          ))}
        </div>

        <div className="inline-actions import-actions">
          <button type="button" className="button button--secondary" onClick={addMessage}>
            {t("import_add_message")}
          </button>
          <button
            type="button"
            className={`button${canStartImport ? "" : " button--disabled"}`}
            onClick={() => void handleStartImport()}
            disabled={busyAction !== null || !canStartImport}
          >
            {busyAction === "start" ? t("import_starting") : t("import_start_button")}
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
                  {t("import_started")}: {formatDateTime(item.startedAt) || t("import_not_available")}
                  {item.finishedAt ? ` | ${t("import_finished")}: ${formatDateTime(item.finishedAt)}` : ""}
                </span>
                <button
                  type="button"
                  className="text-action"
                  onClick={() => void handleLoadDetails(item.id)}
                  disabled={busyAction === "details"}
                >
                  {t("import_view_details")}
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
              <span>{t("import_status")}: {result.status}</span>
              <span>
                {t("import_processed")}: {result.processed} | {t("import_created")}: {result.created} | {t("import_skipped")}: {result.skipped} | {t("import_errors")}: {result.errors}
              </span>
            </div>
            <div className="card-row">
              <strong>{t("import_window")}</strong>
              <span>{t("import_started")}: {formatDateTime(result.startedAt) || t("import_not_available")}</span>
              <span>{t("import_finished")}: {formatDateTime(result.finishedAt) || t("import_not_available")}</span>
            </div>
          </div>

          {result.items.length ? (
            <div className="stack-list import-items">
              {result.items.map((item) => (
                <div key={`${item.externalId}-${item.status}`} className="card-row">
                  <strong>{getItemSummary(item)}</strong>
                  <span>
                    {t("import_status")}: <span className={`status-chip status-chip--${item.status.toLowerCase()}`}>{item.status}</span>
                  </span>
                  <span>
                    {t("import_source")}: {item.sourceProvider ?? t("import_unknown_source")}
                    {item.receivedAt ? ` | ${t("import_received")}: ${formatDateTime(item.receivedAt)}` : ""}
                  </span>
                  <span>
                    {item.billingPeriod ?? t("import_period_unknown")}
                    {item.nextBillingDate ? ` | ${t("import_next_billing")}: ${item.nextBillingDate}` : ""}
                  </span>
                  {item.reason ? <span>{t("import_reason")}: {item.reason}</span> : null}
                </div>
              ))}
            </div>
          ) : null}

          {result.errorItems.length ? (
            <div className="stack-list import-errors">
              {result.errorItems.map((item) => (
                <div key={`${item.externalId}-${item.reason}`} className="card-row">
                  <strong>{item.externalId ?? t("import_message_fallback")}</strong>
                  <span>{item.reason}</span>
                </div>
              ))}
            </div>
          ) : null}
        </section>
      ) : null}

      {consentModalAction ? (
        <div className="modal-backdrop" role="presentation" onClick={closeConsentModal}>
          <div
            className="modal-card"
            role="dialog"
            aria-modal="true"
            aria-label={t("import_consent")}
            onClick={(event) => event.stopPropagation()}
          >
            <h2 className="section__title">{t("import_consent")}</h2>
            <p>{t("import_hint_connected")}</p>
            <p className="muted">{t("import_hint_default")}</p>

            <div className="card-row">
              <strong>{t("import_scope")}</strong>
              <span>{requestedScope}</span>
              <span>{t("import_connection")}: GMAIL</span>
              <span>
                {t("import_status")}: {consent?.status ?? (isLoading ? t("common_loading") : t("import_not_granted_yet"))}
              </span>
            </div>

            {error ? <p className="form-message form-message--error">{error}</p> : null}

            <div className="pill-row modal-actions">
              <button
                type="button"
                className="button button--inline"
                onClick={() => void handleGrantConsent(consentModalAction === "connect")}
                disabled={isConsentModalBusy}
              >
                {busyAction === "connect"
                  ? t("import_redirecting")
                  : busyAction === "grant"
                    ? t("import_granting")
                    : consentModalAction === "connect"
                      ? t("import_connect_gmail")
                      : t("import_grant")}
              </button>
              <button
                type="button"
                className="button button--secondary button--inline"
                onClick={closeConsentModal}
                disabled={isConsentModalBusy}
              >
                {t("subscriptions_cancel")}
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </div>
  );
};

export default ImportCsvPage;
