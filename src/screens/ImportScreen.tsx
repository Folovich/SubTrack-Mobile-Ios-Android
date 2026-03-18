import axios from "axios";
import React, { useCallback, useEffect, useMemo, useState } from "react";
import { Linking, ScrollView, StyleSheet, Text, View } from "react-native";
import { importApi } from "../api/importApi";
import AppButton from "../components/AppButton";
import { useI18n } from "../context/SettingsContext";
import type {
  ImportConsentStatus,
  ImportErrorItem,
  ImportHistoryItem,
  ImportItemResult,
  ImportResult,
  IntegrationStatus
} from "../types/import";
import type { AppPalette } from "../theme/theme";

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

const ImportScreen = () => {
  const { colors } = useI18n();
  const styles = createStyles(colors);
  const [consent, setConsent] = useState<ImportConsentStatus | null>(null);
  const [integration, setIntegration] = useState<IntegrationStatus | null>(null);
  const [history, setHistory] = useState<ImportHistoryItem[]>([]);
  const [result, setResult] = useState<ImportResult | null>(null);
  const [notice, setNotice] = useState<{ kind: "success" | "error"; text: string } | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [busyAction, setBusyAction] = useState<"connect" | "sync" | "disconnect" | "details" | null>(null);

  const loadImportPage = useCallback(async () => {
    setError(null);
    setIsLoading(true);

    try {
      const [consentResponse, integrationResponse, historyResponse] = await Promise.all([
        importApi.getConsentStatus(),
        importApi.getIntegrationStatus(),
        importApi.getHistory()
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
    setError(null);
    setNotice(null);
    setBusyAction("connect");

    try {
      const response = await importApi.startOAuth();
      const supported = await Linking.canOpenURL(response.authorizationUrl);

      if (!supported) {
        throw new Error("Cannot open Gmail authorization URL.");
      }

      await Linking.openURL(response.authorizationUrl);
    } catch (connectError) {
      setError(getErrorMessage(connectError));
      setBusyAction(null);
    }
  };

  const handleDisconnect = async () => {
    setError(null);
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
    setError(null);
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
    setError(null);
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
    <ScrollView contentContainerStyle={styles.container}>
      <Text style={styles.title}>Import</Text>
      <Text style={styles.meta}>
        Connect Gmail with read-only access, sync relevant billing emails, and review which subscriptions were imported,
        skipped, unsupported, or failed to parse.
      </Text>

      {notice ? (
        <Text style={notice.kind === "success" ? styles.success : styles.error}>{notice.text}</Text>
      ) : null}
      {error ? <Text style={styles.error}>{error}</Text> : null}

      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Gmail mailbox import</Text>

        <View style={styles.card}>
          <Text style={styles.itemTitle}>Consent</Text>
          <Text style={styles.meta}>Status: {consent?.status ?? (isLoading ? "Loading..." : "Unknown")}</Text>
          <Text style={styles.meta}>Scope: {consent?.scope ?? "Not granted yet"}</Text>
          <Text style={styles.meta}>Granted: {formatDateTime(consent?.grantedAt ?? null)}</Text>
        </View>

        <View style={styles.card}>
          <Text style={styles.itemTitle}>Connection</Text>
          <Text style={styles.meta}>Status: {integration?.status ?? (isLoading ? "Loading..." : "NOT_CONNECTED")}</Text>
          <Text style={styles.meta}>Mailbox: {integration?.externalAccountEmail ?? "Not connected"}</Text>
          <Text style={styles.meta}>Last sync: {formatDateTime(integration?.lastSyncAt ?? null)}</Text>
        </View>

        <Text style={styles.meta}>{connectionHint}</Text>
        {integration?.lastErrorMessage ? (
          <Text style={styles.meta}>Last Gmail error: {integration.lastErrorMessage}</Text>
        ) : null}

        <View style={styles.actions}>
          <AppButton
            title={busyAction === "connect" ? "Redirecting..." : isConnected ? "Reconnect Gmail" : "Connect Gmail"}
            onPress={() => void handleConnect()}
            disabled={busyAction !== null}
          />
          <AppButton
            title={busyAction === "sync" ? "Syncing..." : "Sync mailbox"}
            variant="ghost"
            onPress={() => void handleSync()}
            disabled={busyAction !== null || !canSync}
          />
          <AppButton
            title={busyAction === "disconnect" ? "Disconnecting..." : "Disconnect"}
            variant="ghost"
            onPress={() => void handleDisconnect()}
            disabled={busyAction !== null || (!integration?.id && !consent)}
          />
        </View>
      </View>

      <View style={styles.section}>
        <Text style={styles.sectionTitle}>History</Text>
        {history.length ? (
          history.map((item) => (
            <View key={item.id} style={styles.card}>
              <Text style={styles.itemTitle}>
                {item.provider} | {item.status}
              </Text>
              <Text style={styles.meta}>
                Started: {formatDateTime(item.startedAt)}
                {item.finishedAt ? ` | Finished: ${formatDateTime(item.finishedAt)}` : ""}
              </Text>
              <AppButton
                title="View details"
                variant="ghost"
                onPress={() => void handleLoadDetails(item.id)}
                disabled={busyAction === "details"}
              />
            </View>
          ))
        ) : (
          <Text style={styles.meta}>{isLoading ? "Loading..." : "No import jobs."}</Text>
        )}
      </View>

      {result ? (
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Result #{result.jobId}</Text>
          <View style={styles.card}>
            <Text style={styles.itemTitle}>Status: {result.status}</Text>
            <Text style={styles.meta}>
              Processed: {result.processed} | Created: {result.created} | Skipped: {result.skipped} | Errors: {result.errors}
            </Text>
            <Text style={styles.meta}>Started: {formatDateTime(result.startedAt)}</Text>
            <Text style={styles.meta}>Finished: {formatDateTime(result.finishedAt)}</Text>
          </View>

          {result.items.length ? (
            result.items.map((item) => (
              <View key={`${item.externalId}-${item.status}`} style={styles.card}>
                <Text style={styles.itemTitle}>{getItemSummary(item)}</Text>
                <Text style={styles.meta}>Status: {item.status}</Text>
                <Text style={styles.meta}>
                  Source: {item.sourceProvider ?? "Unknown"}
                  {item.receivedAt ? ` | Received: ${formatDateTime(item.receivedAt)}` : ""}
                </Text>
                <Text style={styles.meta}>
                  {item.billingPeriod ?? "Period unknown"}
                  {item.nextBillingDate ? ` | Next billing: ${item.nextBillingDate}` : ""}
                </Text>
                {item.reason ? <Text style={styles.meta}>Reason: {item.reason}</Text> : null}
              </View>
            ))
          ) : null}

          {result.errorItems.length ? (
            <>
              <Text style={styles.sectionTitle}>Errors</Text>
              {result.errorItems.map((item: ImportErrorItem) => (
                <View key={`${item.externalId ?? "message"}-${item.reason}`} style={styles.card}>
                  <Text style={styles.itemTitle}>{item.externalId ?? "message"}</Text>
                  <Text style={styles.meta}>{item.reason}</Text>
                </View>
              ))}
            </>
          ) : null}
        </View>
      ) : null}
    </ScrollView>
  );
};

const createStyles = (colors: AppPalette) =>
  StyleSheet.create({
    container: {
      padding: 16,
      gap: 14,
      backgroundColor: colors.bg
    },
    title: {
      fontSize: 30,
      fontWeight: "900",
      color: colors.text
    },
    section: {
      gap: 8,
      backgroundColor: colors.bgElevated,
      borderWidth: 1,
      borderColor: colors.border,
      borderRadius: 16,
      padding: 16
    },
    sectionTitle: {
      color: colors.text,
      fontWeight: "800",
      textTransform: "uppercase",
      letterSpacing: 0.6
    },
    card: {
      borderWidth: 1,
      borderColor: colors.border,
      backgroundColor: colors.bgSoft,
      borderRadius: 14,
      padding: 12,
      gap: 6
    },
    actions: {
      flexDirection: "row",
      gap: 8
    },
    error: {
      color: colors.danger
    },
    success: {
      color: colors.accent
    },
    meta: {
      color: colors.textMuted
    },
    itemTitle: {
      color: colors.text,
      fontWeight: "800"
    }
  });

export default ImportScreen;
