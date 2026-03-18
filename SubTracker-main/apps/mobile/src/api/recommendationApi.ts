import { httpClient } from "./httpClient";
import type { Recommendation } from "../types/recommendation";

export const recommendationApi = {
  getByCategory: async (category: string): Promise<Recommendation[]> => {
    const response = await httpClient.get<Recommendation[]>("/api/v1/recommendations", {
      params: { category }
    });
    return response.data;
  }
};
