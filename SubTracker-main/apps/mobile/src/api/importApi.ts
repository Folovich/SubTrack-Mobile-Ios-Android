import { httpClient } from "./httpClient";
import type { ImportHistoryItem, ImportResult, ImportStartRequest } from "../types/import";

export const importApi = {
  start: async (payload: ImportStartRequest): Promise<ImportResult> => {
    const response = await httpClient.post<ImportResult>("/api/v1/imports/start", payload);
    return response.data;
  },
  getHistory: async (): Promise<ImportHistoryItem[]> => {
    const response = await httpClient.get<ImportHistoryItem[]>("/api/v1/imports");
    return response.data;
  },
  getById: async (id: number): Promise<ImportResult> => {
    const response = await httpClient.get<ImportResult>(`/api/v1/imports/${id}`);
    return response.data;
  }
};
