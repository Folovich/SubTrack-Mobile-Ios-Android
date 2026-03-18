import { useEffect, useMemo, useState, type ChangeEvent, type FormEvent } from "react";
import { categoryApi } from "../api/categoryApi";
import { notificationApi } from "../api/notificationApi";
import { recommendationApi } from "../api/recommendationApi";
import { subscriptionApi } from "../api/subscriptionApi";
import { useLanguage } from "../i18n/LanguageProvider";
import type { Category } from "../types/category";
import type { Notification } from "../types/notification";
import type { Recommendation } from "../types/recommendation";
import type {
  BillingPeriod,
  SupportEmailAction,
  SupportEmailDraftResponse,
  Subscription,
  SubscriptionPayload,
  SubscriptionStatus,
  UpcomingSubscription
} from "../types/subscription";
import { formatCurrency } from "../utils/formatCurrency";

const CURRENCIES = ["USD", "RUB"];
const BILLING_PERIODS: BillingPeriod[] = ["WEEKLY", "MONTHLY", "QUARTERLY", "YEARLY"];
const STATUS_OPTIONS: Array<"ALL" | SubscriptionStatus> = ["ALL", "ACTIVE", "PAUSED", "CANCELED"];
const SORT_OPTIONS = ["NEXT_DATE", "AMOUNT_DESC"] as const;

type SortOption = (typeof SORT_OPTIONS)[number];

type FormState = {
  serviceName: string;
  amount: string;
  currency: string;
  nextBillingDate: string;
  billingPeriod: BillingPeriod;
  categoryId: number | null;
  status: SubscriptionStatus;
};

type SupportDraftModalState = {
  subscriptionId: number;
  serviceName: string;
  action: SupportEmailAction;
  provider: string;
  draft: SupportEmailDraftResponse["draft"];
};

const initialFormState: FormState = {
  serviceName: "",
  amount: "",
  currency: "USD",
  nextBillingDate: "",
  billingPeriod: "MONTHLY",
  categoryId: null,
  status: "ACTIVE"
};

const getBillingPeriodLabel = (
  period: BillingPeriod,
  t: (key:
    | "billing_weekly"
    | "billing_monthly"
    | "billing_quarterly"
    | "billing_yearly") => string
) => {
  switch (period) {
    case "WEEKLY":
      return t("billing_weekly");
    case "MONTHLY":
      return t("billing_monthly");
    case "QUARTERLY":
      return t("billing_quarterly");
    case "YEARLY":
      return t("billing_yearly");
  }
};

const getStatusLabel = (
  status: "ALL" | SubscriptionStatus,
  t: (key:
    | "subscriptions_status_all"
    | "subscriptions_status_active"
    | "subscriptions_status_paused"
    | "subscriptions_status_canceled") => string
) => {
  switch (status) {
    case "ALL":
      return t("subscriptions_status_all");
    case "ACTIVE":
      return t("subscriptions_status_active");
    case "PAUSED":
      return t("subscriptions_status_paused");
    case "CANCELED":
      return t("subscriptions_status_canceled");
  }
};

const SubscriptionsPage = () => {
  const { t } = useLanguage();
  const [categories, setCategories] = useState<Category[]>([]);
  const [subscriptions, setSubscriptions] = useState<Subscription[]>([]);
  const [upcoming, setUpcoming] = useState<UpcomingSubscription[]>([]);
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [recommendations, setRecommendations] = useState<Recommendation[]>([]);
  const [form, setForm] = useState<FormState>(initialFormState);
  const [query, setQuery] = useState("");
  const [statusFilter, setStatusFilter] = useState<"ALL" | SubscriptionStatus>("ALL");
  const [sortBy, setSortBy] = useState<SortOption>("NEXT_DATE");
  const [editingId, setEditingId] = useState<number | null>(null);
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [isRecommendationsLoading, setIsRecommendationsLoading] = useState(false);
  const [recommendationsError, setRecommendationsError] = useState("");
  const [supportDraftModal, setSupportDraftModal] = useState<SupportDraftModalState | null>(null);
  const [supportActionLoadingKey, setSupportActionLoadingKey] = useState<string | null>(null);
  const [supportActionMessage, setSupportActionMessage] = useState("");

  const loadPage = async () => {
    setError("");
    setIsLoading(true);

    try {
      const [categoriesResponse, subscriptionsResponse, upcomingResponse, notificationsResponse] =
        await Promise.all([
          categoryApi.list(),
          subscriptionApi.list(),
          subscriptionApi.upcoming(7),
          notificationApi.list(7)
        ]);

      setCategories(categoriesResponse);
      setSubscriptions(subscriptionsResponse);
      setUpcoming(upcomingResponse);
      setNotifications(notificationsResponse);
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : "Failed to load subscriptions");
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    void loadPage();
  }, []);

  const visibleSubscriptions = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase();

    const filtered = subscriptions.filter((item) => {
      const matchesQuery =
        !normalizedQuery ||
        item.serviceName.toLowerCase().includes(normalizedQuery) ||
        (item.category ?? "").toLowerCase().includes(normalizedQuery);

      const matchesStatus = statusFilter === "ALL" || item.status === statusFilter;

      return matchesQuery && matchesStatus;
    });

    return filtered.sort((left, right) => {
      if (sortBy === "AMOUNT_DESC") {
        return Number(right.amount) - Number(left.amount);
      }

      return new Date(left.nextBillingDate).getTime() - new Date(right.nextBillingDate).getTime();
    });
  }, [query, sortBy, statusFilter, subscriptions]);

  const selectedCategoryName = useMemo(
    () => categories.find((item) => item.id === form.categoryId)?.name ?? "",
    [categories, form.categoryId]
  );

  useEffect(() => {
    let canceled = false;

    if (!selectedCategoryName) {
      setRecommendations([]);
      setRecommendationsError("");
      setIsRecommendationsLoading(false);
      return () => {
        canceled = true;
      };
    }

    setIsRecommendationsLoading(true);
    setRecommendationsError("");

    void recommendationApi
      .listByCategory(selectedCategoryName)
      .then((response) => {
        if (!canceled) {
          setRecommendations(response);
        }
      })
      .catch((loadError) => {
        if (!canceled) {
          setRecommendations([]);
          setRecommendationsError(
            loadError instanceof Error
              ? loadError.message
              : t("subscriptions_recommendations_load_error")
          );
        }
      })
      .finally(() => {
        if (!canceled) {
          setIsRecommendationsLoading(false);
        }
      });

    return () => {
      canceled = true;
    };
  }, [selectedCategoryName, t]);

  const resetForm = () => {
    setForm(initialFormState);
    setEditingId(null);
  };

  const handleChange =
    (field: keyof FormState) => (event: ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
      setForm((current) => ({ ...current, [field]: event.target.value }));
    };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError("");

    if (!form.serviceName.trim() || !form.amount || !form.nextBillingDate) {
      setError("Fill in service, amount, and next billing date");
      return;
    }

    setIsSaving(true);

    const payload: SubscriptionPayload = {
      serviceName: form.serviceName.trim(),
      categoryId: form.categoryId,
      amount: Number(form.amount),
      currency: form.currency,
      billingPeriod: form.billingPeriod,
      nextBillingDate: form.nextBillingDate,
      status: form.status
    };

    try {
      if (editingId) {
        await subscriptionApi.update(editingId, payload);
      } else {
        await subscriptionApi.create(payload);
      }

      resetForm();
      await loadPage();
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : "Failed to save subscription");
    } finally {
      setIsSaving(false);
    }
  };

  const startEdit = (item: Subscription) => {
    const category = categories.find((entry) => entry.name === item.category) ?? null;

    setEditingId(item.id);
    setForm({
      serviceName: item.serviceName,
      amount: String(item.amount),
      currency: item.currency,
      nextBillingDate: item.nextBillingDate,
      billingPeriod: item.billingPeriod,
      categoryId: category?.id ?? null,
      status: item.status
    });
  };

  const handleDelete = async (id: number) => {
    setError("");

    try {
      await subscriptionApi.remove(id);
      if (editingId === id) {
        resetForm();
      }
      await loadPage();
    } catch (deleteError) {
      setError(deleteError instanceof Error ? deleteError.message : "Failed to delete subscription");
    }
  };

  const supportLoadingKey = (subscriptionId: number, action: SupportEmailAction) =>
    `${subscriptionId}:${action}`;

  const trackSupportEvent = async (
    subscriptionId: number,
    action: SupportEmailAction,
    event: "DRAFT_OPENED" | "MAILTO_OPENED" | "TEXT_COPIED"
  ) => {
    try {
      await subscriptionApi.trackSupportEmailEvent(subscriptionId, action, event);
    } catch {
      // Draft flow must continue even if analytics endpoint is temporarily unavailable.
    }
  };

  const openSupportDraft = async (subscription: Subscription, action: SupportEmailAction) => {
    const loadingKey = supportLoadingKey(subscription.id, action);
    setSupportActionLoadingKey(loadingKey);
    setSupportActionMessage("");
    setError("");

    try {
      const response = await subscriptionApi.getSupportEmailDraft(subscription.id, action);
      setSupportDraftModal({
        subscriptionId: response.subscriptionId,
        serviceName: subscription.serviceName,
        action: response.action,
        provider: response.provider,
        draft: response.draft
      });
      await trackSupportEvent(subscription.id, action, "DRAFT_OPENED");
    } catch (draftError) {
      setError(
        draftError instanceof Error ? draftError.message : t("subscriptions_support_load_error")
      );
    } finally {
      setSupportActionLoadingKey(null);
    }
  };

  const closeSupportModal = () => {
    setSupportDraftModal(null);
    setSupportActionMessage("");
  };

  const copyText = async (text: string) => {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(text);
      return;
    }

    const textarea = document.createElement("textarea");
    textarea.value = text;
    textarea.style.position = "fixed";
    textarea.style.opacity = "0";
    document.body.appendChild(textarea);
    textarea.focus();
    textarea.select();
    document.execCommand("copy");
    document.body.removeChild(textarea);
  };

  const handleOpenMailClient = async () => {
    if (!supportDraftModal) {
      return;
    }

    try {
      window.open(supportDraftModal.draft.mailtoUrl, "_self");
      setSupportActionMessage("");
      await trackSupportEvent(supportDraftModal.subscriptionId, supportDraftModal.action, "MAILTO_OPENED");
    } catch {
      setSupportActionMessage(t("subscriptions_support_open_error"));
    }
  };

  const handleCopySupportText = async () => {
    if (!supportDraftModal) {
      return;
    }

    try {
      await copyText(supportDraftModal.draft.plainTextForCopy);
      setSupportActionMessage(t("subscriptions_support_copy_success"));
      await trackSupportEvent(supportDraftModal.subscriptionId, supportDraftModal.action, "TEXT_COPIED");
    } catch {
      setSupportActionMessage(t("subscriptions_support_copy_error"));
    }
  };

  return (
    <div className="page">
      <h1 className="page__title">{t("subscriptions_title")}</h1>

      {error ? <p className="form-message form-message--error">{error}</p> : null}

      <section className="section">
        <h2 className="section__title">{editingId ? t("subscriptions_edit") : t("subscriptions_create")}</h2>
        <form onSubmit={handleSubmit}>
          <input
            className="input"
            placeholder={t("subscriptions_service")}
            value={form.serviceName}
            onChange={handleChange("serviceName")}
          />
          <input
            className="input"
            type="number"
            min="0.01"
            step="0.01"
            placeholder={t("subscriptions_amount")}
            value={form.amount}
            onChange={handleChange("amount")}
          />

          <p className="label">{t("subscriptions_currency")}</p>
          <div className="pill-row">
            {CURRENCIES.map((currency) => (
              <button
                key={currency}
                type="button"
                className={`pill-button${form.currency === currency ? " pill-button--active" : ""}`}
                onClick={() => setForm((current) => ({ ...current, currency }))}
              >
                {currency}
              </button>
            ))}
          </div>

          <input
            className="input"
            type="date"
            aria-label={t("subscriptions_next_billing")}
            value={form.nextBillingDate}
            onChange={handleChange("nextBillingDate")}
          />

          <p className="label">
            {t("subscriptions_billing_period")}: {getBillingPeriodLabel(form.billingPeriod, t)}
          </p>
          <div className="pill-row">
            {BILLING_PERIODS.map((period) => (
              <button
                key={period}
                type="button"
                className={`pill-button${form.billingPeriod === period ? " pill-button--active" : ""}`}
                onClick={() => setForm((current) => ({ ...current, billingPeriod: period }))}
              >
                {getBillingPeriodLabel(period, t)}
              </button>
            ))}
          </div>

          <p className="label">
            {t("subscriptions_category")}:{" "}
            {selectedCategoryName || t("subscriptions_no_category")}
          </p>
          <div className="pill-row">
            <button
              type="button"
              className={`pill-button${form.categoryId === null ? " pill-button--active" : ""}`}
              onClick={() => setForm((current) => ({ ...current, categoryId: null }))}
            >
              {t("subscriptions_no_category")}
            </button>
            {categories.map((category) => (
              <button
                key={category.id}
                type="button"
                className={`pill-button${form.categoryId === category.id ? " pill-button--active" : ""}`}
                onClick={() => setForm((current) => ({ ...current, categoryId: category.id }))}
              >
                {category.name}
              </button>
            ))}
          </div>

          <div className="pill-row">
            <button type="submit" className="button" disabled={isSaving}>
              {isSaving ? t("subscriptions_saving") : editingId ? t("subscriptions_save") : t("subscriptions_create")}
            </button>
            {editingId ? (
              <button type="button" className="button button--secondary" onClick={resetForm}>
                {t("subscriptions_cancel")}
              </button>
            ) : null}
          </div>
        </form>
      </section>

      <section className="section">
        <h2 className="section__title">{t("subscriptions_recommendations_title")}</h2>
        {!selectedCategoryName ? (
          <p>{t("subscriptions_recommendations_select_category")}</p>
        ) : isRecommendationsLoading ? (
          <p>{t("common_loading")}</p>
        ) : recommendationsError ? (
          <p className="form-message form-message--error">{recommendationsError}</p>
        ) : recommendations.length ? (
          <div className="stack-list">
            {recommendations.map((item, index) => (
              <div
                key={`${item.category}-${item.currentService}-${item.alternativeService}-${index}`}
                className="card-row"
              >
                <strong>
                  {item.currentService}
                  {" -> "}
                  {item.alternativeService}
                </strong>
                <span>{item.reason}</span>
              </div>
            ))}
          </div>
        ) : (
          <p>{t("subscriptions_recommendations_empty")}</p>
        )}
      </section>

      <section className="section">
        <h2 className="section__title">{t("subscriptions_list")}</h2>
        <input
          className="input"
          placeholder={t("subscriptions_search")}
          value={query}
          onChange={(event) => setQuery(event.target.value)}
        />

        <p className="label">{t("subscriptions_status_filter")}</p>
        <div className="pill-row">
          {STATUS_OPTIONS.map((status) => (
            <button
              key={status}
              type="button"
              className={`pill-button${statusFilter === status ? " pill-button--active" : ""}`}
              onClick={() => setStatusFilter(status)}
            >
              {getStatusLabel(status, t)}
            </button>
          ))}
        </div>

        <p className="label">{t("subscriptions_sort_by")}</p>
        <div className="pill-row">
          <button
            type="button"
            className={`pill-button${sortBy === "NEXT_DATE" ? " pill-button--active" : ""}`}
            onClick={() => setSortBy("NEXT_DATE")}
          >
            {t("subscriptions_sort_next_date")}
          </button>
          <button
            type="button"
            className={`pill-button${sortBy === "AMOUNT_DESC" ? " pill-button--active" : ""}`}
            onClick={() => setSortBy("AMOUNT_DESC")}
          >
            {t("subscriptions_sort_amount_desc")}
          </button>
        </div>

        {visibleSubscriptions.length ? (
          <div className="stack-list">
            {visibleSubscriptions.map((item) => (
              <div key={item.id} className="card-row">
                <strong>
                  {item.serviceName} · {getStatusLabel(item.status, t)}
                </strong>
                <span>
                  {formatCurrency(item.amount, item.currency)} · {item.nextBillingDate} ·{" "}
                  {item.category ?? t("subscriptions_no_category")}
                </span>
                <div className="inline-actions">
                  <button
                    type="button"
                    className="text-action"
                    disabled={supportActionLoadingKey === supportLoadingKey(item.id, "CANCEL")}
                    onClick={() => void openSupportDraft(item, "CANCEL")}
                  >
                    {supportActionLoadingKey === supportLoadingKey(item.id, "CANCEL")
                      ? t("subscriptions_support_loading")
                      : t("subscriptions_quick_cancel")}
                  </button>
                  <button
                    type="button"
                    className="text-action"
                    disabled={supportActionLoadingKey === supportLoadingKey(item.id, "PAUSE")}
                    onClick={() => void openSupportDraft(item, "PAUSE")}
                  >
                    {supportActionLoadingKey === supportLoadingKey(item.id, "PAUSE")
                      ? t("subscriptions_support_loading")
                      : t("subscriptions_quick_pause")}
                  </button>
                  <button type="button" className="text-action" onClick={() => startEdit(item)}>
                    {t("subscriptions_edit_action")}
                  </button>
                  <button
                    type="button"
                    className="text-action text-action--danger"
                    onClick={() => void handleDelete(item.id)}
                  >
                    {t("subscriptions_delete_action")}
                  </button>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <p>{isLoading ? t("common_loading") : t("subscriptions_no_items")}</p>
        )}
      </section>

      <section className="section">
        <h2 className="section__title">{t("subscriptions_upcoming_7")}</h2>
        {upcoming.length ? (
          <div className="stack-list">
            {upcoming.map((item) => (
              <div key={item.id} className="card-row">
                <strong>{item.serviceName}</strong>
                <span>
                  {formatCurrency(item.amount, item.currency)} · {item.nextBillingDate}
                </span>
              </div>
            ))}
          </div>
        ) : (
          <p>{isLoading ? t("common_loading") : t("upcoming_empty")}</p>
        )}
      </section>

      <section className="section">
        <h2 className="section__title">{t("subscriptions_notifications_7")}</h2>
        {notifications.length ? (
          <div className="stack-list">
            {notifications.map((item) => (
              <div key={item.id} className="card-row">
                <strong>{item.type}</strong>
                <span>{item.message}</span>
              </div>
            ))}
          </div>
        ) : (
          <p>{isLoading ? t("common_loading") : t("reminders_empty")}</p>
        )}
      </section>

      {supportDraftModal ? (
        <div className="modal-backdrop" role="presentation" onClick={closeSupportModal}>
          <div
            className="modal-card"
            role="dialog"
            aria-modal="true"
            aria-label={t("subscriptions_support_title")}
            onClick={(event) => event.stopPropagation()}
          >
            <h2 className="section__title">{t("subscriptions_support_title")}</h2>
            <p className="muted">
              {supportDraftModal.action === "CANCEL"
                ? t("subscriptions_quick_cancel")
                : t("subscriptions_quick_pause")}
              {" · "}
              {supportDraftModal.serviceName}
              {" · "}
              {supportDraftModal.provider}
            </p>

            <p className="label">{t("subscriptions_support_to")}</p>
            <input className="input" value={supportDraftModal.draft.to} readOnly />

            <p className="label">{t("subscriptions_support_subject")}</p>
            <input className="input" value={supportDraftModal.draft.subject} readOnly />

            <p className="label">{t("subscriptions_support_body")}</p>
            <textarea className="input input--textarea" value={supportDraftModal.draft.body} readOnly />

            {supportActionMessage ? <p className="form-message">{supportActionMessage}</p> : null}

            <div className="pill-row modal-actions">
              <button type="button" className="button button--inline" onClick={() => void handleOpenMailClient()}>
                {t("subscriptions_support_open_mail_client")}
              </button>
              <button type="button" className="button button--secondary button--inline" onClick={() => void handleCopySupportText()}>
                {t("subscriptions_support_copy_text")}
              </button>
              <button type="button" className="button button--secondary button--inline" onClick={closeSupportModal}>
                {t("subscriptions_support_close")}
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </div>
  );
};

export default SubscriptionsPage;
