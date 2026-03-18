import { useEffect, useState } from "react";
import { subscriptionApi } from "../api/subscriptionApi";
import { useLanguage } from "../i18n/LanguageProvider";
import type { UpcomingSubscription } from "../types/subscription";
import { formatCurrency } from "../utils/formatCurrency";

const DAY_OPTIONS = [7, 30, 90];

const UpcomingPage = () => {
  const [days, setDays] = useState(7);
  const { t } = useLanguage();
  const [items, setItems] = useState<UpcomingSubscription[]>([]);
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const loadUpcoming = async () => {
      setError("");
      setIsLoading(true);

      try {
        setItems(await subscriptionApi.upcoming(days));
      } catch (loadError) {
        setError(loadError instanceof Error ? loadError.message : "Не удалось загрузить данные");
      } finally {
        setIsLoading(false);
      }
    };

    void loadUpcoming();
  }, [days]);

  return (
    <div className="page">
      <h1 className="page__title">{t("upcoming_title")}</h1>
      <div className="pill-row pill-row--links">
        {DAY_OPTIONS.map((value) => (
          <button
            key={value}
            type="button"
            className={`link-tab${days === value ? " link-tab--active" : ""}`}
            onClick={() => setDays(value)}
          >
            {value}
          </button>
        ))}
      </div>
      <section className="section">
        <p className="muted">{t("upcoming_days")}: {days}</p>
        {error ? <p className="form-message form-message--error">{error}</p> : null}
        {items.length ? (
          <div className="stack-list">
            {items.map((item) => (
              <div key={item.id} className="card-row">
                <strong>{item.serviceName}</strong>
                <span>
                  {formatCurrency(item.amount, item.currency)} · {item.nextBillingDate} · через{" "}
                  {item.daysUntilBilling} дн.
                </span>
              </div>
            ))}
          </div>
        ) : (
          <p>{isLoading ? t("common_loading") : t("upcoming_empty")}</p>
        )}
      </section>
    </div>
  );
};

export default UpcomingPage;
