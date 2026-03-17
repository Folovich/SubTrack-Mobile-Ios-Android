import axios from "axios";
import { API_BASE_URL } from "../constants/api";

export class ApiError extends Error {
  status?: number;
  details?: unknown;
}

type ApiErrorPayload = {
  code?: string;
  provider?: string;
  message?: string;
  error?: string;
  errors?: Record<string, string>;
};

export const mapApiError = (error: unknown): ApiError => {
  if (!axios.isAxiosError(error)) {
    const unknownError = new ApiError("Unexpected error.");
    unknownError.details = error;
    return unknownError;
  }

  const status = error.response?.status;
  const responseData = error.response?.data as ApiErrorPayload | undefined;
  const messageFromBody = responseData?.message ?? responseData?.error;
  const validationMessage =
    responseData?.errors && Object.keys(responseData.errors).length > 0
      ? Object.entries(responseData.errors)
          .map(([field, reason]) => `${field}: ${reason}`)
          .join("; ")
      : null;

  const message =
    responseData?.code === "IMPORT_CONSENT_REQUIRED"
      ? responseData.message || "Import consent is required before import."
      : status === 401
      ? "Unauthorized. Please sign in again."
      : status === 400
        ? validationMessage || messageFromBody || "Invalid request payload."
        : messageFromBody || error.message || "Network error.";

  const apiError = new ApiError(message);
  apiError.status = status;
  apiError.details = responseData;
  return apiError;
};

export const httpClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000
});

let unauthorizedHandler: (() => void | Promise<void>) | null = null;

export const setUnauthorizedHandler = (handler: (() => void | Promise<void>) | null) => {
  unauthorizedHandler = handler;
};

httpClient.interceptors.response.use(
  (response) => response,
  (error: unknown) => {
    if (axios.isAxiosError(error) && error.response?.status === 401 && unauthorizedHandler) {
      void unauthorizedHandler();
    }
    return Promise.reject(mapApiError(error));
  }
);

export const setAuthToken = (token: string | null) => {
  if (token) {
    httpClient.defaults.headers.common.Authorization = `Bearer ${token}`;
    return;
  }

  delete httpClient.defaults.headers.common.Authorization;
};
