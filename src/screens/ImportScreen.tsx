import axios from "axios";
import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Alert, AppState, Linking, ScrollView, StyleSheet, Text, View } from "react-native";
import { consentApi } from "../api/consentApi";
import { importApi } from "../api/importApi";
import AppButton from "../components/AppButton";
import { useI18n } from "../context/SettingsContext";
import type { TranslationKey } from "../i18n/translations";
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

const mapMailMessage = (message: string | undefined, tr: (key: TranslationKey) => string) => {
  if (!message) {
    return tr("mailUnknownError");
  }

  const normalized = message.toLowerCase();

  if (normalized.includes("open") && (normalized.includes("url") || normalized.includes("browser"))) {
    return tr("mailConnectOpenError");
  }

  if (normalized.includes("gmail oauth is not configured")) {
    return tr("mailBackendConfigError");
  }

  if (normalized.includes("oauth") || normalized.includes("gmail") || normalized.includes("authorization")) {
    return tr("mailConnectFailed");
  }

  return message;
};

const mapOAuthReason = (reason: string | null, tr: (key: TranslationKey) => string) => {
  switch (reason) {
    case "access_denied":
      return tr("mailReasonAccessDenied");
    case "invalid_state":
      return tr("mailReasonInvalidState");
    case "expired_state":
      return tr("mailReasonExpiredState");
    case "state_replay":
      return tr("mailReasonStateReplay");
    case "provider_mismatch":
      return tr("mailReasonProviderMismatch");
    case "code_exchange_failed":
      return tr("mailReasonCodeExchangeFailed");
    case "missing_code":
      return tr("mailReasonMissingCode");
    default:
      return reason ?? tr("mailUnknown");
  }
};

const getErrorMessage = (error: unknown, tr: (key: TranslationKey) => string) => {
  if (axios.isAxiosError<{ message?: string; errors?: Record<string, string> }>(error)) {
    const fieldError = error.response?.data?.errors
      ? Object.values(error.response.data.errors)[0]
      : null;

    return mapMailMessage(fieldError ?? error.response?.data?.message, tr);
  }

  if (error instanceof Error) {
    return mapMailMessage(error.message, tr);
  }

  return tr("mailUnknownError");
};

const getFriendlyLastError = (message: string, tr: (key: TranslationKey) => string) => {
  const normalized = message.toLowerCase();

  if (
    normalized.includes("oauth") ||
    normalized.includes("token") ||
    normalized.includes("gmail") ||
    normalized.includes("consent") ||
    normalized.includes("authorization")
  ) {
    return tr("mailLastErrorFriendly");
  }

  return message;
};

const normalizeConsent = (consentResponse: import("../api/consentApi").ImportConsentStatus): ImportConsentStatus => ({
  provider: consentResponse.provider,
  status: consentResponse.status,
  scope: consentResponse.scope ?? null,
  grantedAt: consentResponse.grantedAt ?? null,
  revokedAt: consentResponse.revokedAt ?? null,
  integrationStatus: consentResponse.integrationStatus ?? null
});

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
  const [busyAction, setBusyAction] = useState<"connect" | "sync" | "disconnect" | "details" | "refresh" | null>(null);
  const [pendingOAuth, setPendingOAuth] = useState(false);
  const appStateRef = useRef(AppState.currentState);

  const refreshImportState = useCallback(async () => {
    const [consentResponse, integrationResponse, historyResponse] = await Promise.all([
      importApi.getConsentStatus(),
      importApi.getIntegrationStatus(),
      importApi.getHistory()
    ]);

    setConsent(consentResponse);
    setIntegration(integrationResponse);
    setHistory(historyResponse);

    return { consentResponse, integrationResponse, historyResponse };
  }, []);

  const loadImportPage = useCallback(async () => {
    setError(null);
    setIsLoading(true);

    try {
      await refreshImportState();
    } catch (loadError) {
      setError(getErrorMessage(loadError, tr));
    } finally {
      setIsLoading(false);
    }
  }, [refreshImportState, tr]);

  useEffect(() => {
    void loadImportPage();
  }, [loadImportPage]);

  const isConnected = integration?.status === "ACTIVE";
  const needsReauth = integration?.status === "REAUTH_REQUIRED";
  const hasConsent = consent?.status === "GRANTED";
  const canSync = consent?.status === "GRANTED" && isConnected;
  const connectedCount = isConnected ? 1 : 0;

  const connectionHint = useMemo(() => {
    if (pendingOAuth && !isConnected) {
      return tr("mailAwaitingBrowserReturn");
    }
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
  }, [consent?.status, isConnected, needsReauth, pendingOAuth, tr]);

  const refreshAfterOAuth = useCallback(
    async (manual = false) => {
      setError(null);
      if (manual) {
        setBusyAction("refresh");
      }

      try {
        const { integrationResponse } = await refreshImportState();

        if (integrationResponse.status === "ACTIVE") {
          setNotice({
            kind: "success",
            text: tr("mailConnectedNotice")
          });
          setPendingOAuth(false);
          return;
        }

        if (integrationResponse.status === "REAUTH_REQUIRED") {
          setNotice({
            kind: "error",
            text: tr("mailConnectionFailed")
          });
          setPendingOAuth(false);
          return;
        }

        if (manual) {
          setNotice({
            kind: "error",
            text: tr("mailStillWaiting")
          });
        }
      } catch (refreshError) {
        if (manual) {
          setError(getErrorMessage(refreshError, tr));
        }
      } finally {
        if (manual) {
          setBusyAction(null);
        }
      }
    },
    [refreshImportState, tr]
  );

  const handleConnect = async () => {
    setError(null);
    setNotice(null);
    setBusyAction("connect");

    try {
      if (!hasConsent) {
        const consentResponse = await consentApi.grantImportConsent("GMAIL");
        setConsent(normalizeConsent(consentResponse));
      }

      const response = await importApi.startOAuth();
      setNotice({
        kind: "success",
        text: tr("mailConnectStarted")
      });
      setPendingOAuth(true);
      await Linking.openURL(response.authorizationUrl);
    } catch (connectError) {
      setError(getErrorMessage(connectError, tr));
      setPendingOAuth(false);
    } finally {
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
      setError(getErrorMessage(disconnectError, tr));
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
      setError(getErrorMessage(syncError, tr));
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
      setError(getErrorMessage(detailsError, tr));
    } finally {
      setBusyAction(null);
    }
  };

  const handleCheckConnection = async () => {
    await refreshAfterOAuth(true);
  };

  const onConnectPress = () => {
    Alert.alert(tr("mailConnectConfirmTitle"), tr("mailConnectConfirmMessage"), [
      { text: tr("cancel"), style: "cancel" },
      {
        text: tr("mailConnectConfirmAction"),
        onPress: () => void handleConnect()
      }
    ]);
  };

  const applyOAuthCallback = useCallback(
    (url: string) => {
      try {
        const parsed = new URL(url);
        const gmailStatus = parsed.searchParams.get("gmail");

        if (!gmailStatus) {
          return;
        }

        const reason = parsed.searchParams.get("reason");

        if (gmailStatus === "connected") {
          setNotice({
            kind: "success",
            text: tr("mailConnectedNotice")
          });
          setError(null);
          setPendingOAuth(false);
        } else {
          setNotice({
            kind: "error",
            text: reason
              ? tr("mailConnectionFailedReason").replace("{reason}", mapOAuthReason(reason, tr))
              : tr("mailConnectionFailed")
          });
          setPendingOAuth(false);
        }

        void loadImportPage();
      } catch {
        // Ignore unrelated incoming URLs.
      }
    },
    [loadImportPage, tr]
  );

  useEffect(() => {
    if (!pendingOAuth) {
      return;
    }

    const subscription = AppState.addEventListener("change", (nextState) => {
      const previousState = appStateRef.current;
      appStateRef.current = nextState;

      if (nextState !== "active" || (previousState !== "background" && previousState !== "inactive")) {
        return;
      }

      void refreshAfterOAuth();
    });

    return () => {
      subscription.remove();
    };
  }, [pendingOAuth, refreshAfterOAuth]);

  useEffect(() => {
    void (async () => {
      const initialUrl = await Linking.getInitialURL();
      if (initialUrl) {
        applyOAuthCallback(initialUrl);
      }
    })();

    const subscription = Linking.addEventListener("url", ({ url }) => {
      applyOAuthCallback(url);
    });

    return () => {
      subscription.remove();
    };
  }, [applyOAuthCallback]);

  return (
    <ScrollView contentContainerStyle={styles.container}>
      <View style={styles.hero}>
        <Text style={styles.title}>{tr("mailTitle")}</Text>
        <Text style={styles.meta}>{tr("mailSubtitle")}</Text>
      </View>

      {notice ? (
        <Text style={notice.kind === "success" ? styles.success : styles.error}>{notice.text}</Text>
      ) : null}
      {error ? <Text style={styles.error}>{error}</Text> : null}

      <View style={styles.summaryRow}>
        <View style={styles.summaryCard}>
          <Text style={styles.summaryLabel}>{tr("mailConnectedCount")}</Text>
          <Text style={styles.summaryValue}>{connectedCount}</Text>
        </View>
        <View style={styles.summaryCard}>
          <Text style={styles.summaryLabel}>{tr("mailJobsCount")}</Text>
          <Text style={styles.summaryValue}>{history.length}</Text>
        </View>
      </View>

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
            {tr("mailLastError")}: {getFriendlyLastError(integration.lastErrorMessage, tr)}
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
              onPress={onConnectPress}
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
          {pendingOAuth && !isConnected ? (
            <View style={styles.actionItem}>
              <AppButton
                fullWidth
                title={busyAction === "refresh" ? tr("loading") : tr("mailCheckConnection")}
                variant="ghost"
                onPress={() => void handleCheckConnection()}
                disabled={busyAction !== null}
              />
            </View>
          ) : null}
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
    hero: {
      gap: 6,
      backgroundColor: colors.bgElevated,
      borderWidth: 1,
      borderColor: colors.border,
      borderRadius: 18,
      padding: 16
    },
    summaryRow: {
      flexDirection: "row",
      gap: 10
    },
    summaryCard: {
      flex: 1,
      borderWidth: 1,
      borderColor: colors.border,
      backgroundColor: colors.bgElevated,
      borderRadius: 16,
      padding: 14,
      gap: 4
    },
    summaryLabel: {
      color: colors.textMuted,
      fontSize: 12,
      fontWeight: "800",
      textTransform: "uppercase",
      letterSpacing: 0.6
    },
    summaryValue: {
      color: colors.text,
      fontSize: 28,
      fontWeight: "900"
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
      backgroundColor: colors.card,
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
