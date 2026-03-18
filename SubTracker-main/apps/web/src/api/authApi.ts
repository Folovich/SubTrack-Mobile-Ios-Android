import type { AuthResponse, LoginRequest, RegisterRequest } from "../types/auth";
import { httpClient } from "./httpClient";

export const authApi = {
  login: async (payload: LoginRequest) => {
    const { data } = await httpClient.post<AuthResponse>("/auth/login", payload);
    return data;
  },
  register: async (payload: RegisterRequest) => {
    const { data } = await httpClient.post<AuthResponse>("/auth/register", payload);
    return data;
  }
};
