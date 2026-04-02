import axios from "axios";
import { authApi } from "../api/authApi";
import type { ApiErrorResponse, AuthResponse, LoginRequest, RegisterRequest, User } from "../types/auth";

const AUTH_STORAGE_KEY = "subtrack.auth";
const MOCK_USERS_STORAGE_KEY = "subtrack.mock-users";

type StoredMockUser = User & {
  password: string;
};

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

const isBackendUnavailable = (error: unknown) =>
  axios.isAxiosError(error) && (!error.response || error.code === "ERR_NETWORK");

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

const readMockUsers = (): StoredMockUser[] => {
  if (typeof window === "undefined") {
    return [];
  }

  const raw = window.localStorage.getItem(MOCK_USERS_STORAGE_KEY);
  if (!raw) {
    return [];
  }

  try {
    const parsed = JSON.parse(raw) as StoredMockUser[];
    if (Array.isArray(parsed)) {
      return parsed;
    }
  } catch {
    window.localStorage.removeItem(MOCK_USERS_STORAGE_KEY);
  }

  return [];
};

const writeMockUsers = (users: StoredMockUser[]) => {
  window.localStorage.setItem(MOCK_USERS_STORAGE_KEY, JSON.stringify(users));
};

const createMockSession = (user: User): AuthResponse => ({
  token: `mock-token-${user.id}`,
  user
});

const loginLocally = (payload: LoginRequest) => {
  const email = payload.email.trim().toLowerCase();
  const users = readMockUsers();
  const user = users.find((item) => item.email === email);

  if (!user || user.password !== payload.password) {
    throw new Error("Неверный email или пароль");
  }

  const session = createMockSession({
    id: user.id,
    email: user.email,
    createdAt: user.createdAt
  });

  writeSession(session);
  return session;
};

const registerLocally = (payload: RegisterRequest) => {
  const email = payload.email.trim().toLowerCase();
  const users = readMockUsers();

  if (users.some((item) => item.email === email)) {
    throw new Error("Пользователь с таким email уже существует");
  }

  const user: StoredMockUser = {
    id: users.reduce((maxId, item) => Math.max(maxId, item.id), 0) + 1,
    email,
    password: payload.password,
    createdAt: new Date().toISOString()
  };

  writeMockUsers([...users, user]);

  const session = createMockSession({
    id: user.id,
    email: user.email,
    createdAt: user.createdAt
  });

  writeSession(session);
  return session;
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
      if (isBackendUnavailable(error)) {
        return loginLocally(payload);
      }

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
      if (isBackendUnavailable(error)) {
        return registerLocally(payload);
      }

      throw new Error(getErrorMessage(error));
    }
  },
  logout: () => {
    if (typeof window !== "undefined") {
      window.localStorage.removeItem(AUTH_STORAGE_KEY);
    }
  }
};
