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
    return null;
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
  const { tr, colors } = useI18n();
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
      return tr("mailHintExpired");
    }
    if (isConnected) {
      return tr("mailHintConnected");
    }
    if (consent?.status === "GRANTED") {
      return tr("mailHintConsentOnly");
    }
    return tr("mailHintDefault");
  }, [consent?.status, isConnected, needsReauth, tr]);

  const handleConnect = async () => {
    setError(null);
    setNotice(null);
    setBusyAction("connect");

    try {
      const response = await importApi.startOAuth();
      const supported = await Linking.canOpenURL(response.authorizationUrl);

      if (!supported) {
        throw new Error(tr("supportDraftOpenError"));
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
        text: tr("mailDisconnectSuccess")
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
        text: tr("mailSyncSuccess")
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
      <Text style={styles.title}>{tr("mailTitle")}</Text>
      <Text style={styles.meta}>{tr("mailSubtitle")}</Text>

      {notice ? (
        <Text style={notice.kind === "success" ? styles.success : styles.error}>{notice.text}</Text>
      ) : null}
      {error ? <Text style={styles.error}>{error}</Text> : null}

      <View style={styles.section}>
        <Text style={styles.sectionTitle}>{tr("mailSectionConnection")}</Text>

        <View style={styles.card}>
          <Text style={styles.itemTitle}>{tr("mailConsentCard")}</Text>
          <Text style={styles.meta}>
            {tr("mailStatus")}: {consent?.status ?? (isLoading ? tr("mailLoadingState") : tr("mailUnknown"))}
          </Text>
          <Text style={styles.meta}>{tr("mailScope")}: {consent?.scope ?? tr("mailNotGranted")}</Text>
          <Text style={styles.meta}>
            {tr("mailGranted")}: {formatDateTime(consent?.grantedAt ?? null) ?? tr("mailNotAvailable")}
          </Text>
        </View>

        <View style={styles.card}>
          <Text style={styles.itemTitle}>{tr("mailConnectionCard")}</Text>
          <Text style={styles.meta}>
            {tr("mailStatus")}: {integration?.status ?? (isLoading ? tr("mailLoadingState") : tr("mailNotConnected"))}
          </Text>
          <Text style={styles.meta}>{tr("mailMailbox")}: {integration?.externalAccountEmail ?? tr("mailNotConnected")}</Text>
          <Text style={styles.meta}>
            {tr("mailLastSync")}: {formatDateTime(integration?.lastSyncAt ?? null) ?? tr("mailNotAvailable")}
          </Text>
        </View>

        <Text style={styles.meta}>{connectionHint}</Text>
        {integration?.lastErrorMessage ? (
          <Text style={styles.meta}>
            {tr("mailLastError")}: {integration.lastErrorMessage}
          </Text>
        ) : null}

        <View style={styles.actions}>
          <View style={styles.actionItem}>
            <AppButton
              fullWidth
              title={
                busyAction === "connect"
                  ? tr("mailRedirecting")
                  : isConnected
                    ? tr("mailReconnect")
                    : tr("mailConnect")
              }
              onPress={() => void handleConnect()}
              disabled={busyAction !== null}
            />
          </View>
          <View style={styles.actionItem}>
            <AppButton
              fullWidth
              title={busyAction === "sync" ? tr("mailSyncing") : tr("mailSync")}
              variant="ghost"
              onPress={() => void handleSync()}
              disabled={busyAction !== null || !canSync}
            />
          </View>
          <View style={styles.actionItem}>
            <AppButton
              fullWidth
              title={busyAction === "disconnect" ? tr("mailDisconnecting") : tr("mailDisconnect")}
              variant="ghost"
              onPress={() => void handleDisconnect()}
              disabled={busyAction !== null || (!integration?.id && !consent)}
            />
          </View>
        </View>
      </View>

      <View style={styles.section}>
        <Text style={styles.sectionTitle}>{tr("mailSectionHistory")}</Text>
        {history.length ? (
          history.map((item) => (
            <View key={item.id} style={styles.card}>
              <Text style={styles.itemTitle}>
                {item.provider} | {item.status}
              </Text>
              <Text style={styles.meta}>
                {tr("mailStarted")}: {formatDateTime(item.startedAt) ?? tr("mailNotAvailable")}
                {item.finishedAt
                  ? ` | ${tr("mailFinished")}: ${formatDateTime(item.finishedAt) ?? tr("mailNotAvailable")}`
                  : ""}
              </Text>
              <AppButton
                title={tr("mailViewDetails")}
                variant="ghost"
                onPress={() => void handleLoadDetails(item.id)}
                disabled={busyAction === "details"}
              />
            </View>
          ))
        ) : (
          <Text style={styles.meta}>{isLoading ? tr("mailLoadingState") : tr("mailNoJobs")}</Text>
        )}
      </View>

      {result ? (
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>{tr("mailSectionResult")} #{result.jobId}</Text>
          <View style={styles.card}>
            <Text style={styles.itemTitle}>
              {tr("mailStatus")}: {result.status}
            </Text>
            <Text style={styles.meta}>
              {tr("mailProcessed")}: {result.processed} | {tr("mailCreated")}: {result.created} | {tr("mailSkipped")}:{" "}
              {result.skipped} | {tr("mailErrors")}: {result.errors}
            </Text>
            <Text style={styles.meta}>
              {tr("mailStarted")}: {formatDateTime(result.startedAt) ?? tr("mailNotAvailable")}
            </Text>
            <Text style={styles.meta}>
              {tr("mailFinished")}: {formatDateTime(result.finishedAt) ?? tr("mailNotAvailable")}
            </Text>
          </View>

          {result.items.length ? (
            result.items.map((item) => (
              <View key={`${item.externalId}-${item.status}`} style={styles.card}>
                <Text style={styles.itemTitle}>{getItemSummary(item)}</Text>
                <Text style={styles.meta}>{tr("mailStatus")}: {item.status}</Text>
                <Text style={styles.meta}>
                  {tr("mailSource")}: {item.sourceProvider ?? tr("mailUnknown")}
                  {item.receivedAt
                    ? ` | ${tr("mailReceived")}: ${formatDateTime(item.receivedAt) ?? tr("mailNotAvailable")}`
                    : ""}
                </Text>
                <Text style={styles.meta}>
                  {item.billingPeriod ?? tr("mailPeriodUnknown")}
                  {item.nextBillingDate ? ` | ${tr("mailNextBilling")}: ${item.nextBillingDate}` : ""}
                </Text>
                {item.reason ? <Text style={styles.meta}>{tr("mailReason")}: {item.reason}</Text> : null}
              </View>
            ))
          ) : null}

          {result.errorItems.length ? (
            <>
              <Text style={styles.sectionTitle}>{tr("mailErrors")}</Text>
              {result.errorItems.map((item: ImportErrorItem) => (
                <View key={`${item.externalId ?? "message"}-${item.reason}`} style={styles.card}>
                  <Text style={styles.itemTitle}>{item.externalId ?? tr("mailMessage")}</Text>
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
      flexWrap: "wrap",
      gap: 10
    },
    actionItem: {
      width: "100%"
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
