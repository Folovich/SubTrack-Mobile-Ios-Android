import AsyncStorage from "@react-native-async-storage/async-storage";

const TOKEN_KEY = "subtrack_token";

export const tokenStorage = {
  setToken: async (token: string) => AsyncStorage.setItem(TOKEN_KEY, token),
  getToken: async () => AsyncStorage.getItem(TOKEN_KEY),
  clearToken: async () => AsyncStorage.removeItem(TOKEN_KEY)
};
