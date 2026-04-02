import type { Recommendation } from "../types/recommendation";
import { httpClient } from "./httpClient";

export const recommendationApi = {
  listByCategory: async (category: string) => {
    const { data } = await httpClient.get<Recommendation[]>("/recommendations", {
      params: { category }
    });
    return data;
  }
};
