import { httpClient } from "./httpClient";
import type { Category } from "../types/category";

export const categoryApi = {
  getAll: async (): Promise<Category[]> => {
    const response = await httpClient.get<Category[]>("/api/v1/categories");
    return response.data;
  }
};
