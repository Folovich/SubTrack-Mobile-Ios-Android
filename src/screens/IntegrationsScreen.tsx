import React, { useCallback, useEffect, useState } from "react";
import { ScrollView, StyleSheet, Text, View } from "react-native";
import { integrationApi } from "../api/integrationApi";
import { ApiError } from "../api/httpClient";
import AppButton from "../components/AppButton";
import { useI18n } from "../context/SettingsContext";
import type { AppPalette } from "../theme/theme";
import type { IntegrationConnection } from "../types/integration";

const IntegrationsScreen = () => {
  const { tr, colors } = useI18n();
  const styles = createStyles(colors);
  const [items, setItems] = useState<IntegrationConnection[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      setIsLoading(true);
      setError(null);
      const response = await integrationApi.getAll();
      setItems(response);
    } catch (loadError) {
      setError(loadError instanceof ApiError ? loadError.message : tr("failedLoadDetails"));
    } finally {
      setIsLoading(false);
    }
  }, [tr]);

  useEffect(() => {
    void load();
  }, [load]);

  return (
    <ScrollView contentContainerStyle={styles.container}>
      <Text style={styles.title}>{tr("tabIntegrations")}</Text>
      {isLoading ? <Text style={styles.meta}>{tr("loading")}</Text> : null}
      {error ? <Text style={styles.error}>{error}</Text> : null}
      {items.length === 0 && !isLoading ? <Text style={styles.meta}>{tr("noIntegrations")}</Text> : null}
      {items.map((item) => (
        <View key={`${item.provider}-${item.id ?? "none"}`} style={styles.card}>
          <Text style={styles.cardTitle}>{item.provider}</Text>
          <Text style={styles.meta}>{tr("status")}: {item.status}</Text>
          {item.externalAccountEmail ? <Text style={styles.meta}>{item.externalAccountEmail}</Text> : null}
          {item.lastSyncAt ? <Text style={styles.meta}>Last sync: {item.lastSyncAt}</Text> : null}
        </View>
      ))}
      <AppButton title={tr("reload")} fullWidth onPress={() => void load()} />
    </ScrollView>
  );
};

const createStyles = (colors: AppPalette) =>
  StyleSheet.create({
    container: {
      padding: 16,
      gap: 12,
      backgroundColor: colors.bg
    },
    title: {
      fontSize: 30,
      fontWeight: "900",
      color: colors.text
    },
    card: {
      borderWidth: 1,
      borderColor: colors.border,
      backgroundColor: colors.bgElevated,
      borderRadius: 14,
      padding: 12,
      gap: 4
    },
    cardTitle: {
      color: colors.text,
      fontWeight: "800"
    },
    meta: {
      color: colors.textMuted
    },
    error: {
      color: colors.danger
    }
  });

export default IntegrationsScreen;
