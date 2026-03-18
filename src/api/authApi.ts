import axios, { isAxiosError } from "axios";
import { httpClient } from "./httpClient";
import type { AuthCredentials, AuthResponse, RegisterPayload } from "../types/auth";

type RawAuthResponse = {
  token?: string;
  accessToken?: string;
  jwt?: string;
  jwtToken?: string;
  user?: AuthResponse["user"];
};

const extractToken = (data: RawAuthResponse): string => {
  const token = data.token ?? data.accessToken ?? data.jwt ?? data.jwtToken;

  if (!token) {
    throw new Error("Auth token is missing in API response.");
  }

  return token;
};

const authClient = axios.create({
  baseURL: httpClient.defaults.baseURL,
  timeout: httpClient.defaults.timeout
});

const postAuth = async <T>(paths: string[], payload: unknown): Promise<T> => {
  let lastError: unknown;

  for (const path of paths) {
    try {
      const response = await authClient.post<T>(path, payload);
      return response.data;
    } catch (error) {
      if (isAxiosError(error) && error.response?.status === 404) {
        lastError = error;
        continue;
      }
      throw error;
    }
  }

  throw lastError ?? new Error("Auth endpoint is not available.");
};

export const authApi = {
  login: async (payload: AuthCredentials): Promise<AuthResponse> => {
    const data = await postAuth<RawAuthResponse>(["/api/v1/auth/login", "/auth/login"], {
      email: payload.email.trim().toLowerCase(),
      password: payload.password
    });
    return { token: extractToken(data), user: data.user };
  },
  register: async (payload: RegisterPayload): Promise<AuthResponse> => {
    const data = await postAuth<RawAuthResponse>(["/api/v1/auth/register", "/auth/register"], {
      email: payload.email.trim().toLowerCase(),
      password: payload.password
    });
    return { token: extractToken(data), user: data.user };
  }
};
