import React, { useState } from "react";
import { Pressable, StyleSheet, Text, TextInput, View } from "react-native";
import { useNavigation } from "@react-navigation/native";
import type { NativeStackNavigationProp } from "@react-navigation/native-stack";
import { useAuth } from "../hooks/useAuth";
import { useI18n } from "../context/SettingsContext";
import type { AuthStackParamList } from "../types/navigation";
import AppButton from "../components/AppButton";
import type { AppPalette } from "../theme/theme";

type LoginScreenNavigationProp = NativeStackNavigationProp<AuthStackParamList, "Login">;

const LoginScreen = () => {
  const navigation = useNavigation<LoginScreenNavigationProp>();
  const { signIn } = useAuth();
  const { tr, colors } = useI18n();
  const styles = createStyles(colors);
  const [email, setEmail] = useState("demo@subtrack.app");
  const [password, setPassword] = useState("demo1234");
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const onLogin = async () => {
    if (!email.trim() || !password.trim()) {
      setError(tr("enterEmailPassword"));
      return;
    }

    try {
      setIsSubmitting(true);
      setError(null);
      await signIn({ email: email.trim(), password });
    } catch (loginError) {
      const message = loginError instanceof Error ? loginError.message : tr("loginFailed");
      setError(message);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <View style={styles.container}>
      <View style={styles.hero}>
        <View style={styles.brandRow}>
          <View style={styles.brandDark}>
            <Text style={styles.brandDarkText}>Sub</Text>
          </View>
          <View style={styles.brandAccent}>
            <Text style={styles.brandAccentText}>Track</Text>
          </View>
        </View>
        <Text style={styles.kicker}>Premium control for recurring spend</Text>
        <Text style={styles.title}>{tr("login")}</Text>
      </View>
      <TextInput
        autoCapitalize="none"
        keyboardType="email-address"
        placeholder={tr("email")}
        placeholderTextColor={colors.textMuted}
        style={styles.input}
        value={email}
        onChangeText={setEmail}
      />
      <View style={styles.passwordRow}>
        <TextInput
          placeholder={tr("password")}
          placeholderTextColor={colors.textMuted}
          secureTextEntry={!showPassword}
          style={[styles.input, styles.passwordInput]}
          value={password}
          onChangeText={setPassword}
        />
        <AppButton
          title={showPassword ? tr("hide") : tr("show")}
          variant="ghost"
          onPress={() => setShowPassword((prev) => !prev)}
        />
      </View>
      {error ? <Text style={styles.error}>{error}</Text> : null}
      <View style={styles.action}>
        <AppButton
          disabled={isSubmitting}
          fullWidth
          title={isSubmitting ? tr("signingIn") : tr("signIn")}
          onPress={onLogin}
        />
      </View>
      <Pressable onPress={() => navigation.navigate("Register")}>
        <Text style={styles.link}>{tr("goToRegister")}</Text>
      </Pressable>
    </View>
  );
};

const createStyles = (colors: AppPalette) =>
  StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: "center",
    paddingHorizontal: 24,
    gap: 14,
    backgroundColor: colors.bg
  },
  hero: {
    gap: 10,
    marginBottom: 8
  },
  brandRow: {
    flexDirection: "row",
    alignSelf: "flex-start"
  },
  brandDark: {
    backgroundColor: colors.sidebar,
    borderTopLeftRadius: 12,
    borderBottomLeftRadius: 12,
    paddingHorizontal: 14,
    paddingVertical: 10
  },
  brandAccent: {
    backgroundColor: colors.accent,
    borderTopRightRadius: 12,
    borderBottomRightRadius: 12,
    paddingHorizontal: 14,
    paddingVertical: 10
  },
  brandDarkText: {
    color: colors.sidebarText,
    fontSize: 28,
    fontWeight: "900",
    letterSpacing: 0.4
  },
  brandAccentText: {
    color: colors.accentText,
    fontSize: 28,
    fontWeight: "900",
    letterSpacing: 0.4
  },
  kicker: {
    color: colors.textMuted,
    fontSize: 13,
    textTransform: "uppercase",
    letterSpacing: 1.1
  },
  title: {
    fontSize: 32,
    fontWeight: "900",
    color: colors.text
  },
  input: {
    width: "100%",
    borderWidth: 1,
    borderColor: colors.border,
    backgroundColor: colors.bgElevated,
    color: colors.text,
    borderRadius: 12,
    paddingHorizontal: 14,
    paddingVertical: 14
  },
  passwordRow: {
    width: "100%",
    flexDirection: "row",
    alignItems: "center",
    gap: 8
  },
  passwordInput: {
    flex: 1
  },
  action: {
    width: "100%"
  },
  error: {
    width: "100%",
    color: colors.danger
  },
  link: {
    color: colors.accent,
    fontWeight: "700",
    letterSpacing: 0.3
  }
});

export default LoginScreen;
