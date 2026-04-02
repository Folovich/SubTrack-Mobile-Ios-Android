import axios from "axios";
import { API_BASE_URL } from "../constants/api";

export class ApiError extends Error {
  status?: number;
  details?: unknown;
  code?: string;
  fieldErrors?: Record<string, string>;
  retryAfterSeconds?: number;
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
  const retryAfterHeader =
    error.response?.headers?.["retry-after"] ?? error.response?.headers?.RetryAfter;
  const retryAfterSeconds =
    typeof retryAfterHeader === "string"
      ? Number.parseInt(retryAfterHeader, 10)
      : Array.isArray(retryAfterHeader) && retryAfterHeader[0]
        ? Number.parseInt(retryAfterHeader[0], 10)
        : undefined;
  const messageFromBody = responseData?.message ?? responseData?.error;
  const fieldErrors =
    responseData?.errors && Object.keys(responseData.errors).length > 0 ? responseData.errors : undefined;

  const message =
    responseData?.code === "IMPORT_CONSENT_REQUIRED"
      ? responseData.message || "Import consent is required before import."
      : status === 401
      ? "Please check your credentials and try again."
      : status === 429
        ? retryAfterSeconds
          ? `Too many requests. Please wait ${retryAfterSeconds} seconds and try again.`
          : "Too many requests. Please wait and try again."
      : status === 400
        ? fieldErrors
          ? "Please check the highlighted fields."
          : messageFromBody || "Please check your data and try again."
        : status && status >= 500
          ? "Server is temporarily unavailable. Please try again later."
          : messageFromBody || error.message || "Network error.";

  const apiError = new ApiError(message);
  apiError.status = status;
  apiError.code = responseData?.code;
  apiError.details = responseData;
  apiError.fieldErrors = fieldErrors;
  apiError.retryAfterSeconds = Number.isFinite(retryAfterSeconds) ? retryAfterSeconds : undefined;
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
