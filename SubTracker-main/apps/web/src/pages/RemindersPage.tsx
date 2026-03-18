import { useEffect, useState } from "react";
import { notificationApi } from "../api/notificationApi";
import { useLanguage } from "../i18n/LanguageProvider";
import type { Notification } from "../types/notification";

const DAY_OPTIONS = [7, 30, 90];

const RemindersPage = () => {
  const [days, setDays] = useState(7);
  const { t } = useLanguage();
  const [items, setItems] = useState<Notification[]>([]);
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const loadNotifications = async () => {
      setError("");
      setIsLoading(true);

      try {
        setItems(await notificationApi.list(days));
      } catch (loadError) {
        setError(loadError instanceof Error ? loadError.message : "Не удалось загрузить уведомления");
      } finally {
        setIsLoading(false);
      }
    };

    void loadNotifications();
  }, [days]);

  return (
    <div className="page">
      <h1 className="page__title">{t("reminders_title")}</h1>
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
                <strong>{item.type}</strong>
                <span>
                  {item.message} · {item.scheduledAt}
                </span>
              </div>
            ))}
          </div>
        ) : (
          <p>{isLoading ? t("common_loading") : t("reminders_empty")}</p>
        )}
      </section>
    </div>
  );
};

export default RemindersPage;
