import type { UserProfile } from "../types/user";
import { httpClient } from "./httpClient";

export const userApi = {
  getMe: async () => {
    const { data } = await httpClient.get<UserProfile>("/users/me");
    return data;
  }
};
