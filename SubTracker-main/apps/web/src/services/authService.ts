import axios from "axios";
import { authApi } from "../api/authApi";
import type { ApiErrorResponse, AuthResponse, LoginRequest, RegisterRequest } from "../types/auth";

const AUTH_STORAGE_KEY = "subtrack.auth";

const getErrorMessage = (error: unknown) => {
  if (axios.isAxiosError<ApiErrorResponse>(error)) {
    const fieldError = error.response?.data?.errors
      ? Object.values(error.response.data.errors)[0]
      : null;

    return fieldError ?? error.response?.data?.message ?? "Request failed";
  }

  if (error instanceof Error) {
    return error.message;
  }

  return "Unexpected error";
};

const readSession = (): AuthResponse | null => {
  if (typeof window === "undefined") {
    return null;
  }

  const raw = window.localStorage.getItem(AUTH_STORAGE_KEY);
  if (!raw) {
    return null;
  }

  try {
    return JSON.parse(raw) as AuthResponse;
  } catch {
    window.localStorage.removeItem(AUTH_STORAGE_KEY);
    return null;
  }
};

const writeSession = (session: AuthResponse) => {
  window.localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(session));
};

export const authService = {
  storageKey: AUTH_STORAGE_KEY,
  getSession: readSession,
  login: async (payload: LoginRequest) => {
    try {
      const session = await authApi.login({
        email: payload.email.trim().toLowerCase(),
        password: payload.password
      });
      writeSession(session);
      return session;
    } catch (error) {
      throw new Error(getErrorMessage(error));
    }
  },
  register: async (payload: RegisterRequest) => {
    try {
      const session = await authApi.register({
        email: payload.email.trim().toLowerCase(),
        password: payload.password
      });
      writeSession(session);
      return session;
    } catch (error) {
      throw new Error(getErrorMessage(error));
    }
  },
  logout: () => {
    if (typeof window !== "undefined") {
      window.localStorage.removeItem(AUTH_STORAGE_KEY);
    }
  }
};
