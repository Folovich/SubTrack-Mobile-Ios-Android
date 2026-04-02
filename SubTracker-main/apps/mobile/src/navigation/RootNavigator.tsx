import React from "react";
import { ActivityIndicator, StyleSheet, View } from "react-native";
import { createNativeStackNavigator } from "@react-navigation/native-stack";
import type { RootStackParamList } from "../types/navigation";
import { useAuth } from "../hooks/useAuth";
import AuthStack from "./AuthStack";
import MainTabs from "./MainTabs";

const Stack = createNativeStackNavigator<RootStackParamList>();

const RootNavigator = () => {
  const { isAuthenticated, isAuthLoading } = useAuth();

  if (isAuthLoading) {
    return (
      <View style={styles.loaderContainer}>
        <ActivityIndicator size="large" />
      </View>
    );
  }

  return (
    <Stack.Navigator screenOptions={{ headerShown: false }}>
      {isAuthenticated ? (
        <Stack.Screen name="Main" component={MainTabs} />
      ) : (
        <Stack.Screen name="Auth" component={AuthStack} />
      )}
    </Stack.Navigator>
  );
};

const styles = StyleSheet.create({
  loaderContainer: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center"
  }
});

export default RootNavigator;
