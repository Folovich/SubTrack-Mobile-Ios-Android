import React from "react";
import { createNativeStackNavigator } from "@react-navigation/native-stack";
import type { AuthStackParamList } from "../types/navigation";
import LoginScreen from "../screens/LoginScreen";
import RegisterScreen from "../screens/RegisterScreen";
import { useI18n } from "../context/SettingsContext";

const Stack = createNativeStackNavigator<AuthStackParamList>();

const AuthStack = () => {
  const { tr } = useI18n();

  return (
    <Stack.Navigator initialRouteName="Login">
      <Stack.Screen name="Login" component={LoginScreen} options={{ title: tr("login") }} />
      <Stack.Screen name="Register" component={RegisterScreen} options={{ title: tr("register") }} />
    </Stack.Navigator>
  );
};

export default AuthStack;
