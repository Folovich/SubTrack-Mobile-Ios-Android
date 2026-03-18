import axios from "axios";

export const httpClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL || "http://localhost:8080/api/v1"
});

httpClient.interceptors.request.use((config) => {
  if (typeof window === "undefined") {
    return config;
  }

  const rawSession = window.localStorage.getItem("subtrack.auth");
  if (!rawSession) {
    return config;
  }

  try {
    const session = JSON.parse(rawSession) as { token?: string };
    if (session.token) {
      config.headers = {
        ...(config.headers ?? {}),
        Authorization: `Bearer ${session.token}`
      };
    }
  } catch {
    window.localStorage.removeItem("subtrack.auth");
  }

  return config;
});
