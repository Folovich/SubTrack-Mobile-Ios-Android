import { httpClient } from "./httpClient";
import type { IntegrationConnection } from "../types/integration";

export const integrationApi = {
  getAll: async (): Promise<IntegrationConnection[]> => {
    const response = await httpClient.get<IntegrationConnection[]>("/api/v1/integrations");
    return response.data;
  }
};
