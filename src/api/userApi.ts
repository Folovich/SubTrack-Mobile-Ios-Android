import { httpClient } from "./httpClient";
import type { UserProfile } from "../types/user";

export const userApi = {
  getMe: async (): Promise<UserProfile> => {
    const response = await httpClient.get<UserProfile>("/api/v1/users/me");
    return response.data;
  }
};
