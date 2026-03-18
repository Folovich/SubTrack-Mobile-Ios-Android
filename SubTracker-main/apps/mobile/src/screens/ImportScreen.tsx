import React, { useCallback, useEffect, useMemo, useState } from "react";

import { ScrollView, StyleSheet, Text, TextInput, View } from "react-native";

import { consentApi, type ImportConsentStatus } from "../api/consentApi";
import { ApiError } from "../api/httpClient";
import { importApi } from "../api/importApi";
import type { ImportHistoryItem, ImportResult, MailMessageRequest } from "../types/import";
import { useI18n } from "../context/SettingsContext";
import AppButton from "../components/AppButton";
import type { AppPalette } from "../theme/theme";

const IMPORT_PROVIDER = "GMAIL";

type ImportConsentErrorPayload = {
  code?: string;
  provider?: string;
  message?: string;
};

const getImportConsentError = (error: ApiError): ImportConsentErrorPayload | null => {
  const details = error.details as ImportConsentErrorPayload | undefined;
  if (error.status === 403 && details?.code === "IMPORT_CONSENT_REQUIRED") {
    return details;
  }

  return null;
};

const ImportScreen = () => {
  const { colors } = useI18n();
  const styles = createStyles(colors);
  const [history, setHistory] = useState<ImportHistoryItem[]>([]);
  const [details, setDetails] = useState<ImportResult | null>(null);

  const [consent, setConsent] = useState<ImportConsentStatus | null>(null);
  const [isConsentLoading, setIsConsentLoading] = useState(true);
  const [isConsentUpdating, setIsConsentUpdating] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [isConsentLoading, setIsConsentLoading] = useState(true);
  const [isConsentUpdating, setIsConsentUpdating] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState("");
  const [subject, setSubject] = useState("Subscription renewal");
  const [fromEmail, setFromEmail] = useState("billing@example.com");
  const [pendingRetryPayload, setPendingRetryPayload] = useState<MailMessageRequest | null>(null);

  const hasGrantedConsent = consentStatus?.status === "GRANTED";
  const canSubmit = useMemo(
    () => Boolean(message.trim() && subject.trim() && fromEmail.trim() && hasGrantedConsent),
    [fromEmail, hasGrantedConsent, message, subject]
  );

  const loadHistory = useCallback(async () => {
    try {
      setIsLoading(true);
      setError(null);
      const response = await importApi.getHistory();
      setHistory(response);
    } catch (historyError) {
      setError(
        historyError instanceof ApiError && historyError.status === 400
          ? "Invalid import history request."
          : historyError instanceof Error
            ? historyError.message
            : "Failed to load import history."
      );
    } finally {
      setIsLoading(false);
    }
  }, []);

  const loadConsentStatus = useCallback(async () => {
    try {
      setIsConsentLoading(true);
      const status = await consentApi.getImportConsentStatus(IMPORT_PROVIDER);
      setConsentStatus(status);
    } catch (consentError) {
      setError(consentError instanceof Error ? consentError.message : "Failed to load import consent status.");
    } finally {
      setIsConsentLoading(false);
    }
  }, []);

  const runImport = useCallback(
    async (payload: MailMessageRequest) => {
      const result = await importApi.start({
        provider: IMPORT_PROVIDER,
        messages: [payload]
      });
      setDetails(result);
      setPendingRetryPayload(null);
      await loadHistory();
    },
    [loadHistory]
  );

  const onGrantConsent = useCallback(async () => {
    try {
      setError(null);
      setIsConsentUpdating(true);
      const status = await consentApi.grantImportConsent(IMPORT_PROVIDER);
      setConsentStatus(status);
    } catch (grantError) {
      setError(grantError instanceof Error ? grantError.message : "Failed to grant import consent.");
    } finally {
      setIsConsentUpdating(false);
    }
  }, []);

  const onRevokeConsent = useCallback(async () => {
    try {
      setError(null);
      setIsConsentUpdating(true);
      const status = await consentApi.revokeImportConsent(IMPORT_PROVIDER);
      setConsentStatus(status);
      setPendingRetryPayload(null);
    } catch (revokeError) {
      setError(revokeError instanceof Error ? revokeError.message : "Failed to revoke import consent.");
    } finally {
      setIsConsentUpdating(false);
    }
  }, []);

  const handleImportError = useCallback((startError: unknown, payload: MailMessageRequest) => {
    if (startError instanceof ApiError) {
      const consentError = getImportConsentError(startError);
      if (consentError) {
        setPendingRetryPayload(payload);
        setConsentStatus((prev) => ({
          provider: IMPORT_PROVIDER,
          status: "NOT_GRANTED",
          scope: prev?.scope,
          grantedAt: prev?.grantedAt ?? null,
          revokedAt: prev?.revokedAt ?? null,
          integrationStatus: prev?.integrationStatus
        }));
        setError(consentError.message ?? "Import consent is required before import for provider GMAIL.");
        return;
      }
    }

    setError(
      startError instanceof ApiError && startError.status === 400
        ? "Import payload validation failed."
        : startError instanceof Error
          ? startError.message
          : "Failed to start import."
    );
  }, []);

  useEffect(() => {
    void loadHistory();
    void loadConsentStatus();
  }, [loadConsentStatus, loadHistory]);

  const loadConsent = useCallback(async () => {
    try {
      setIsConsentLoading(true);
      const response = await consentApi.getImportConsentStatus("GMAIL");
      setConsent(response);
    } catch (loadError) {
      setError(loadError instanceof ApiError ? loadError.message : "Failed to load consent status.");
    } finally {
      setIsConsentLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadConsent();
  }, [loadConsent]);

  const onStartImport = async () => {
    if (!hasGrantedConsent) {
      setError("Import consent is required before import. Tap \"Grant consent\" to continue.");
      return;
    }

    const payload: MailMessageRequest = {
      externalId: `manual-${Date.now()}`,
      from: fromEmail.trim(),
      subject: subject.trim(),
      body: message.trim(),
      receivedAt: new Date().toISOString()
    };

    try {
      if (consent?.status !== "GRANTED") {
        setError("Grant consent before import.");
        return;
      }
      setError(null);
      setIsSubmitting(true);
      await runImport(payload);
    } catch (startError) {
      handleImportError(startError, payload);
    } finally {
      setIsSubmitting(false);
    }
  };

  const onGrantAndRetry = async () => {
    if (!pendingRetryPayload) {
      await onGrantConsent();
      return;
    }

    try {
      setError(null);
      setIsConsentUpdating(true);
      setIsSubmitting(true);
      const status = await consentApi.grantImportConsent(IMPORT_PROVIDER);
      setConsentStatus(status);
      await runImport(pendingRetryPayload);
    } catch (retryError) {
      setError(retryError instanceof Error ? retryError.message : "Failed to grant consent and retry import.");
    } finally {
      setIsConsentUpdating(false);
      setIsSubmitting(false);
    }
  };

  const onGrantConsent = async () => {
    try {
      setError(null);
      setIsConsentUpdating(true);
      const response = await consentApi.grantImportConsent("GMAIL");
      setConsent(response);
    } catch (updateError) {
      setError(updateError instanceof ApiError ? updateError.message : "Failed to grant consent.");
    } finally {
      setIsConsentUpdating(false);
    }
  };

  const onRevokeConsent = async () => {
    try {
      setError(null);
      setIsConsentUpdating(true);
      const response = await consentApi.revokeImportConsent("GMAIL");
      setConsent(response);
    } catch (updateError) {
      setError(updateError instanceof ApiError ? updateError.message : "Failed to revoke consent.");
    } finally {
      setIsConsentUpdating(false);
    }
  };

  const onLoadDetails = async (id: number) => {
    try {
      setError(null);
      const result = await importApi.getById(id);
      setDetails(result);
    } catch (detailsError) {
      setError(
        detailsError instanceof ApiError && detailsError.status === 400
          ? "Invalid import id."
          : detailsError instanceof Error
            ? detailsError.message
            : "Failed to load import details."
      );
    }
  };

  return (
    <ScrollView contentContainerStyle={styles.container}>
      <Text style={styles.title}>Import</Text>
      {isLoading ? <Text style={styles.meta}>Loading history...</Text> : null}
      {error ? <Text style={styles.error}>{error}</Text> : null}

      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Import consent</Text>

        {isConsentLoading ? <Text style={styles.meta}>Loading consent...</Text> : null}
        <Text style={styles.meta}>Status: {consent?.status ?? "UNKNOWN"}</Text>
        <View style={styles.actions}>
          <AppButton
            title={isConsentUpdating ? "Granting..." : "Grant consent"}
            onPress={() => void onGrantConsent()}
            disabled={isConsentLoading || isConsentUpdating}
          />
          <AppButton
            title={isConsentUpdating ? "Revoking..." : "Revoke consent"}
            variant="ghost"
            onPress={() => void onRevokeConsent()}
            disabled={isConsentLoading || isConsentUpdating}
          />
        </View>

      </View>

      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Start import (MVP)</Text>
        <TextInput
          style={styles.input}
          placeholder="From"
          placeholderTextColor={colors.textMuted}
          value={fromEmail}
          onChangeText={setFromEmail}
        />
        <TextInput
          style={styles.input}
          placeholder="Subject"
          placeholderTextColor={colors.textMuted}
          value={subject}
          onChangeText={setSubject}
        />
        <TextInput
          style={[styles.input, styles.textArea]}
          multiline
          placeholder="Email body"
          placeholderTextColor={colors.textMuted}
          value={message}
          onChangeText={setMessage}
        />

        <AppButton
          disabled={!canSubmit || isSubmitting}
          fullWidth

          title={isSubmitting ? "Starting..." : "Start import"}
          onPress={() => void onStartImport()}
        />
      </View>

      <View style={styles.section}>
        <Text style={styles.sectionTitle}>History</Text>
        {history.length === 0 ? <Text>No import jobs.</Text> : null}
        {history.map((item) => (
          <View key={item.id} style={styles.card}>
            <Text style={styles.itemTitle}>#{item.id}</Text>
            <Text style={styles.meta}>
              {item.provider} | {item.status}
            </Text>
            <AppButton title="Details" variant="ghost" onPress={() => void onLoadDetails(item.id)} />
          </View>
        ))}
      </View>

      {details ? (
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Details #{details.jobId}</Text>
          <Text style={styles.meta}>Status: {details.status}</Text>
          <Text style={styles.meta}>
            Processed: {details.processed}, Created: {details.created}, Skipped: {details.skipped}, Errors: {details.errors}
          </Text>
          {details.errorItems.length > 0 ? <Text style={styles.sectionTitle}>Errors</Text> : null}
          {details.errorItems.map((item) => (
            <Text key={`${item.externalId}-${item.reason}`} style={styles.meta}>
              {item.externalId}: {item.reason}
            </Text>
          ))}
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
  input: {
    borderWidth: 1,
    borderColor: colors.border,
    backgroundColor: colors.bgSoft,
    color: colors.text,
    borderRadius: 12,
    paddingHorizontal: 12,
    paddingVertical: 10
  },
  textArea: {
    minHeight: 100,
    textAlignVertical: "top"
  },
  card: {
    borderWidth: 1,
    borderColor: colors.border,
    backgroundColor: colors.bgElevated,
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
  meta: {
    color: colors.textMuted
  },
  itemTitle: {
    color: colors.text,
    fontWeight: "800"
  }
});

export default ImportScreen;
