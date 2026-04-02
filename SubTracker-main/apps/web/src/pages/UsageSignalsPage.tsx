import { useEffect, useMemo, useState, type ChangeEvent, type FormEvent } from "react";
import { subscriptionApi } from "../api/subscriptionApi";
import { usageSignalApi } from "../api/usageSignalApi";
import { useLanguage } from "../i18n/LanguageProvider";
import type { Subscription } from "../types/subscription";
import type { UsageSignal } from "../types/usageSignal";

type FormState = {
  subscriptionId: string;
  signalType: string;
  value: string;
};

const initialFormState: FormState = {
  subscriptionId: "",
  signalType: "",
  value: ""
};

const formatDateTime = (value: string) =>
  new Intl.DateTimeFormat(undefined, {
    dateStyle: "medium",
    timeStyle: "short"
  }).format(new Date(value));

const UsageSignalsPage = () => {
  const { t } = useLanguage();
  const [subscriptions, setSubscriptions] = useState<Subscription[]>([]);
  const [signals, setSignals] = useState<UsageSignal[]>([]);
  const [form, setForm] = useState<FormState>(initialFormState);
  const [filterSubscriptionId, setFilterSubscriptionId] = useState("");
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const subscriptionNameById = useMemo(
    () =>
      new Map(
        subscriptions.map((subscription) => [subscription.id, subscription.serviceName] as const)
      ),
    [subscriptions]
  );

  const loadPage = async (subscriptionId?: number | null) => {
    setError("");
    setIsLoading(true);

    try {
      const [subscriptionsResponse, signalsResponse] = await Promise.all([
        subscriptionApi.list(),
        usageSignalApi.list(subscriptionId)
      ]);

      setSubscriptions(subscriptionsResponse);
      setSignals(signalsResponse);
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : t("usage_signals_load_error"));
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    void loadPage();
  }, []);

  const handleInputChange =
    (field: keyof FormState) =>
    (event: ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
      setForm((current) => ({ ...current, [field]: event.target.value }));
    };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError("");

    if (!form.subscriptionId || !form.signalType.trim() || !form.value.trim()) {
      setError(t("usage_signals_fill_required"));
      return;
    }

    setIsSubmitting(true);

    try {
      await usageSignalApi.create({
        subscriptionId: Number(form.subscriptionId),
        signalType: form.signalType.trim(),
        value: form.value.trim()
      });
      setForm({
        subscriptionId: form.subscriptionId,
        signalType: "",
        value: ""
      });
      await loadPage(filterSubscriptionId ? Number(filterSubscriptionId) : null);
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : t("usage_signals_create_error"));
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleApplyFilter = async () => {
    await loadPage(filterSubscriptionId ? Number(filterSubscriptionId) : null);
  };

  return (
    <div className="page">
      <h1 className="page__title">{t("usage_signals_title")}</h1>

      {error ? <p className="form-message form-message--error">{error}</p> : null}

      <section className="section">
        <h2 className="section__title">{t("usage_signals_create")}</h2>
        <form onSubmit={handleSubmit}>
          <select className="input" value={form.subscriptionId} onChange={handleInputChange("subscriptionId")}>
            <option value="">{t("usage_signals_select_subscription")}</option>
            {subscriptions.map((subscription) => (
              <option key={subscription.id} value={subscription.id}>
                {subscription.serviceName}
              </option>
            ))}
          </select>
          <input
            className="input"
            placeholder={t("usage_signals_signal_type")}
            value={form.signalType}
            onChange={handleInputChange("signalType")}
          />
          <input
            className="input"
            placeholder={t("usage_signals_value")}
            value={form.value}
            onChange={handleInputChange("value")}
          />
          <button type="submit" className="button" disabled={isSubmitting}>
            {isSubmitting ? t("usage_signals_saving") : t("usage_signals_create_button")}
          </button>
        </form>
      </section>

      <section className="section">
        <h2 className="section__title">{t("usage_signals_history")}</h2>
        <div className="pill-row usage-signals-toolbar">
          <select
            className="input input--compact"
            value={filterSubscriptionId}
            onChange={(event) => setFilterSubscriptionId(event.target.value)}
          >
            <option value="">{t("usage_signals_all_subscriptions")}</option>
            {subscriptions.map((subscription) => (
              <option key={subscription.id} value={subscription.id}>
                {subscription.serviceName}
              </option>
            ))}
          </select>
          <button
            type="button"
            className="button button--secondary button--inline"
            onClick={() => void handleApplyFilter()}
          >
            {t("usage_signals_apply_filter")}
          </button>
        </div>

        {signals.length ? (
          <div className="stack-list">
            {signals.map((signal) => (
              <div key={signal.id} className="card-row">
                <strong>{subscriptionNameById.get(signal.subscriptionId) ?? `Subscription #${signal.subscriptionId}`}</strong>
                <span>{t("usage_signals_type")}: {signal.signalType}</span>
                <span>{t("usage_signals_value")}: {signal.value}</span>
                <span>{t("usage_signals_created")}: {formatDateTime(signal.createdAt)}</span>
              </div>
            ))}
          </div>
        ) : (
          <p>{isLoading ? t("common_loading") : t("usage_signals_empty")}</p>
        )}
      </section>
    </div>
  );
};

export default UsageSignalsPage;
