import type { IntegrationConnection } from "../types/integration";
import { httpClient } from "./httpClient";

export const integrationApi = {
  list: async () => {
    const { data } = await httpClient.get<IntegrationConnection[]>("/integrations");
    return data;
  }
};
