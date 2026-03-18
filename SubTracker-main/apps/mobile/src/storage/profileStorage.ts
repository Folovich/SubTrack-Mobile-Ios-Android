import AsyncStorage from "@react-native-async-storage/async-storage";

const PROFILE_NAME_KEY = "subtrack_profile_name";

export const profileStorage = {
  setProfileName: async (name: string) => AsyncStorage.setItem(PROFILE_NAME_KEY, name),
  getProfileName: async () => AsyncStorage.getItem(PROFILE_NAME_KEY),
  clearProfileName: async () => AsyncStorage.removeItem(PROFILE_NAME_KEY)
};
