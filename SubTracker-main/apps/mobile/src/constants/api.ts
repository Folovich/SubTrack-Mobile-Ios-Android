const apiBaseUrl = process.env.EXPO_PUBLIC_API_URL?.trim();

if (!apiBaseUrl) {
  throw new Error(
    "EXPO_PUBLIC_API_URL is not set. Define it in apps/mobile/.env (for Android emulator use http://10.0.2.2:8080)."
  );
}

export const API_BASE_URL = apiBaseUrl;
