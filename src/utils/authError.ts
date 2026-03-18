import { ApiError } from "../api/httpClient";
import type { TranslationKey } from "../i18n/translations";

type Translate = (key: TranslationKey) => string;

export type AuthFieldErrors = {
  email?: string;
  password?: string;
};

type AuthErrorState = {
  generalError: string | null;
  fieldErrors: AuthFieldErrors;
};

const looksLikeDuplicateEmail = (message: string) => {
  const normalized = message.toLowerCase();
  return (
    normalized.includes("already") ||
    normalized.includes("exists") ||
    normalized.includes("taken") ||
    normalized.includes("duplicate") ||
    normalized.includes("used")
  );
};

const buildRateLimitMessage = (error: ApiError, tr: Translate) => {
  if (!error.retryAfterSeconds) {
    return tr("authTooManyAttempts");
  }

  return `${tr("authTooManyAttempts")} ${error.retryAfterSeconds} ${tr("secondsShort")}.`;
};

export const mapAuthError = (error: unknown, tr: Translate): AuthErrorState => {
  if (!(error instanceof ApiError)) {
    return {
      generalError: tr("authUnexpectedError"),
      fieldErrors: {}
    };
  }

  if (error.status === 400 && error.fieldErrors) {
    const fieldErrors: AuthFieldErrors = {};

    if (error.fieldErrors.email) {
      fieldErrors.email = looksLikeDuplicateEmail(error.fieldErrors.email)
        ? tr("emailAlreadyUsed")
        : tr("emailInvalid");
    }

    if (error.fieldErrors.password) {
      fieldErrors.password = tr("passwordRequirements");
    }

    return {
      generalError: Object.keys(fieldErrors).length > 0 ? tr("checkHighlightedFields") : error.message,
      fieldErrors
    };
  }

  if (error.status === 401) {
    return {
      generalError: tr("authInvalidCredentials"),
      fieldErrors: {}
    };
  }

  if (error.status === 429) {
    return {
      generalError: buildRateLimitMessage(error, tr),
      fieldErrors: {}
    };
  }

  if (error.status && error.status >= 500) {
    return {
      generalError: tr("authTryLater"),
      fieldErrors: {}
    };
  }

  return {
    generalError: error.message || tr("authUnexpectedError"),
    fieldErrors: {}
  };
};
