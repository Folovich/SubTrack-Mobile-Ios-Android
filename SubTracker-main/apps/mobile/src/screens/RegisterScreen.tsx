import React, { useState } from "react";
import { Pressable, StyleSheet, Text, TextInput, View } from "react-native";
import { useNavigation } from "@react-navigation/native";
import type { NativeStackNavigationProp } from "@react-navigation/native-stack";
import { useAuth } from "../hooks/useAuth";
import { useI18n } from "../context/SettingsContext";
import type { AuthStackParamList } from "../types/navigation";
import AppButton from "../components/AppButton";
import type { AppPalette } from "../theme/theme";
import { mapAuthError, type AuthFieldErrors } from "../utils/authError";

type RegisterScreenNavigationProp = NativeStackNavigationProp<AuthStackParamList, "Register">;

const RegisterScreen = () => {
  const navigation = useNavigation<RegisterScreenNavigationProp>();
  const { signUp } = useAuth();
  const { tr, colors } = useI18n();
  const styles = createStyles(colors);
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [generalError, setGeneralError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<AuthFieldErrors>({});
  const [isSubmitting, setIsSubmitting] = useState(false);

  const onRegister = async () => {
    const nextFieldErrors: AuthFieldErrors = {};
    if (!email.trim()) {
      nextFieldErrors.email = tr("emailRequired");
    }
    if (!password.trim()) {
      nextFieldErrors.password = tr("passwordRequired");
    }
    if (Object.keys(nextFieldErrors).length > 0) {
      setFieldErrors(nextFieldErrors);
      setGeneralError(null);
      return;
    }

    try {
      setIsSubmitting(true);
      setGeneralError(null);
      setFieldErrors({});
      await signUp({ name: name.trim(), email: email.trim(), password });
    } catch (registerError) {
      const { generalError: nextGeneralError, fieldErrors: nextFieldErrors } = mapAuthError(
        registerError,
        tr
      );
      setGeneralError(nextGeneralError);
      setFieldErrors(nextFieldErrors);
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
        <Text style={styles.kicker}>Create a direct, high-contrast account profile</Text>
        <Text style={styles.title}>{tr("register")}</Text>
      </View>
      <TextInput
        placeholder={tr("name")}
        placeholderTextColor={colors.textMuted}
        style={styles.input}
        value={name}
        onChangeText={setName}
      />
      <View style={styles.fieldBlock}>
        <TextInput
          autoCapitalize="none"
          keyboardType="email-address"
          placeholder={tr("email")}
          placeholderTextColor={colors.textMuted}
          style={[styles.input, fieldErrors.email ? styles.inputError : null]}
          value={email}
          onChangeText={(value) => {
            setEmail(value);
            if (fieldErrors.email) {
              setFieldErrors((prev) => ({ ...prev, email: undefined }));
            }
          }}
        />
        {fieldErrors.email ? <Text style={styles.fieldError}>{fieldErrors.email}</Text> : null}
      </View>
      <View style={styles.fieldBlock}>
        <View style={styles.passwordRow}>
          <TextInput
            placeholder={tr("password")}
            placeholderTextColor={colors.textMuted}
            secureTextEntry={!showPassword}
            style={[styles.input, styles.passwordInput, fieldErrors.password ? styles.inputError : null]}
            value={password}
            onChangeText={(value) => {
              setPassword(value);
              if (fieldErrors.password) {
                setFieldErrors((prev) => ({ ...prev, password: undefined }));
              }
            }}
          />
          <AppButton
            title={showPassword ? tr("hide") : tr("show")}
            variant="ghost"
            onPress={() => setShowPassword((prev) => !prev)}
          />
        </View>
        <Text style={styles.hint}>{tr("passwordRequirements")}</Text>
        {fieldErrors.password ? <Text style={styles.fieldError}>{fieldErrors.password}</Text> : null}
      </View>
      {generalError ? <Text style={styles.error}>{generalError}</Text> : null}
      <View style={styles.action}>
        <AppButton
          disabled={isSubmitting}
          fullWidth
          title={isSubmitting ? tr("creating") : tr("createAccount")}
          onPress={onRegister}
        />
      </View>
      <Pressable onPress={() => navigation.navigate("Login")}>
        <Text style={styles.link}>{tr("backToLogin")}</Text>
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
  inputError: {
    borderColor: colors.danger
  },
  fieldBlock: {
    width: "100%",
    gap: 6
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
  hint: {
    width: "100%",
    color: colors.textMuted,
    fontSize: 12,
    lineHeight: 18
  },
  fieldError: {
    color: colors.danger,
    fontSize: 12,
    lineHeight: 17
  },
  link: {
    color: colors.accent,
    fontWeight: "700",
    letterSpacing: 0.3
  }
});

export default RegisterScreen;
