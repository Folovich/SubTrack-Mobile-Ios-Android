import type { Category } from "../types/category";
import { httpClient } from "./httpClient";

export const categoryApi = {
  list: async () => {
    const { data } = await httpClient.get<Category[]>("/categories");
    return data;
  }
};
