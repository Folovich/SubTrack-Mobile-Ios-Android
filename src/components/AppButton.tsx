import React from "react";
import { ActivityIndicator, StyleSheet, Text, TouchableOpacity, View } from "react-native";
import { useI18n } from "../context/SettingsContext";

type AppButtonProps = {
  title: string;
  onPress: () => void;
  disabled?: boolean;
  variant?: "accent" | "ghost";
  fullWidth?: boolean;
};

const AppButton = ({
  title,
  onPress,
  disabled = false,
  variant = "accent",
  fullWidth = false
}: AppButtonProps) => {
  const { colors } = useI18n();
  const accent = variant === "accent";

  return (
    <TouchableOpacity
      activeOpacity={0.85}
      disabled={disabled}
      onPress={onPress}
      style={[
        styles.button,
        accent
          ? { backgroundColor: colors.accent }
          : { backgroundColor: colors.bgSoft, borderWidth: 1, borderColor: colors.border },
        fullWidth ? styles.fullWidth : null,
        disabled ? styles.disabled : null
      ]}
    >
      <View style={styles.content}>
        {disabled && accent ? <ActivityIndicator color={colors.accentText} size="small" /> : null}
        <Text style={[styles.label, { color: accent ? colors.accentText : colors.text }]}>{title}</Text>
      </View>
    </TouchableOpacity>
  );
};

const styles = StyleSheet.create({
  button: {
    minHeight: 48,
    borderRadius: 10,
    alignItems: "center",
    justifyContent: "center",
    paddingHorizontal: 16
  },
  fullWidth: {
    width: "100%"
  },
  disabled: {
    opacity: 0.6
  },
  content: {
    flexDirection: "row",
    alignItems: "center",
    gap: 8
  },
  label: {
    fontSize: 14,
    fontWeight: "800",
    letterSpacing: 0.4,
    textTransform: "uppercase"
  }
});

export default AppButton;
