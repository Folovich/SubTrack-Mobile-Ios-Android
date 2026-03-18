import React, { useCallback, useEffect, useState } from "react";
import { ScrollView, StyleSheet, Text, TouchableOpacity, View } from "react-native";
import { categoryApi } from "../api/categoryApi";
import { ApiError } from "../api/httpClient";
import { recommendationApi } from "../api/recommendationApi";
import AppButton from "../components/AppButton";
import { useI18n } from "../context/SettingsContext";
import type { AppPalette } from "../theme/theme";
import type { Category } from "../types/category";
import type { Recommendation } from "../types/recommendation";

const RecommendationsScreen = () => {
  const { tr, colors } = useI18n();
  const styles = createStyles(colors);
  const [categories, setCategories] = useState<Category[]>([]);
  const [selectedCategory, setSelectedCategory] = useState<string | null>(null);
  const [items, setItems] = useState<Recommendation[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadCategories = useCallback(async () => {
    const response = await categoryApi.getAll();
    setCategories(response);
    if (response.length > 0 && !selectedCategory) {
      setSelectedCategory(response[0].name);
    }
  }, [selectedCategory]);

  const loadRecommendations = useCallback(async (category: string) => {
    const response = await recommendationApi.getByCategory(category);
    setItems(response);
  }, []);

  const load = useCallback(async () => {
    try {
      setIsLoading(true);
      setError(null);
      await loadCategories();
    } catch (loadError) {
      setError(loadError instanceof ApiError ? loadError.message : tr("failedLoadDetails"));
    } finally {
      setIsLoading(false);
    }
  }, [loadCategories, tr]);

  useEffect(() => {
    void load();
  }, [load]);

  useEffect(() => {
    if (!selectedCategory) return;
    void (async () => {
      try {
        setError(null);
        await loadRecommendations(selectedCategory);
      } catch (loadError) {
        setError(loadError instanceof ApiError ? loadError.message : tr("failedLoadDetails"));
      }
    })();
  }, [loadRecommendations, selectedCategory, tr]);

  return (
    <ScrollView contentContainerStyle={styles.container}>
      <Text style={styles.title}>Recommendations</Text>
      {isLoading ? <Text style={styles.meta}>{tr("loading")}</Text> : null}
      {error ? <Text style={styles.error}>{error}</Text> : null}

      <View style={styles.row}>
        {categories.map((category) => (
          <TouchableOpacity
            key={category.id}
            style={[styles.pill, selectedCategory === category.name ? styles.pillActive : null]}
            onPress={() => setSelectedCategory(category.name)}
          >
            <Text>{category.name}</Text>
          </TouchableOpacity>
        ))}
      </View>

      {items.length === 0 && !isLoading ? <Text style={styles.meta}>No recommendations.</Text> : null}
      {items.map((item, index) => (
        <View key={`${item.currentService}-${item.alternativeService}-${index}`} style={styles.card}>
          <Text style={styles.cardTitle}>{item.category}</Text>
          <Text style={styles.meta}>{item.currentService} → {item.alternativeService}</Text>
          <Text style={styles.meta}>{item.reason}</Text>
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
    row: {
      flexDirection: "row",
      flexWrap: "wrap",
      gap: 8
    },
    pill: {
      borderWidth: 1,
      borderColor: colors.border,
      backgroundColor: colors.bgSoft,
      borderRadius: 999,
      paddingHorizontal: 12,
      paddingVertical: 8
    },
    pillActive: {
      backgroundColor: colors.accent,
      borderColor: colors.accent
    },
    card: {
      borderWidth: 1,
      borderColor: colors.border,
      backgroundColor: colors.bgElevated,
      borderRadius: 14,
      padding: 12,
      gap: 6
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

export default RecommendationsScreen;
