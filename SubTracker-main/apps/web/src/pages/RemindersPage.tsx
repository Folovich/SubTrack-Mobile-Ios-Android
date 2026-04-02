import { useEffect, useState } from "react";
import { notificationApi } from "../api/notificationApi";
import { useLanguage } from "../i18n/LanguageProvider";
import type { Notification } from "../types/notification";

const DAY_OPTIONS = [7, 30, 90];
const SETTINGS_STORAGE_KEY = "subtrack.notification-settings";

type NotificationSettings = {
  email: boolean;
  browser: boolean;
  digest: boolean;
  reminderDays: number;
  priceChangePercent: number;
  inactivityDays: number;
};

const defaultSettings: NotificationSettings = {
  email: true,
  browser: true,
  digest: false,
  reminderDays: 3,
  priceChangePercent: 15,
  inactivityDays: 21
};

const readSettings = (): NotificationSettings => {
  if (typeof window === "undefined") {
    return defaultSettings;
  }

  const raw = window.localStorage.getItem(SETTINGS_STORAGE_KEY);
  if (!raw) {
    return defaultSettings;
  }

  try {
    return { ...defaultSettings, ...(JSON.parse(raw) as Partial<NotificationSettings>) };
  } catch {
    window.localStorage.removeItem(SETTINGS_STORAGE_KEY);
    return defaultSettings;
  }
};

const RemindersPage = () => {
  const [days, setDays] = useState(7);
  const { t } = useLanguage();
  const [items, setItems] = useState<Notification[]>([]);
  const [settings, setSettings] = useState<NotificationSettings>(readSettings);
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    if (typeof window !== "undefined") {
      window.localStorage.setItem(SETTINGS_STORAGE_KEY, JSON.stringify(settings));
    }
  }, [settings]);

  useEffect(() => {
    const loadNotifications = async () => {
      setError("");
      setIsLoading(true);

      try {
        const response = await notificationApi.list(days);
        setItems(response);
      } catch (loadError) {
        setItems([]);
        setError(loadError instanceof Error ? loadError.message : t("common_error"));
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
        <h2 className="section__title">{t("reminders_settings_title")}</h2>
        <div className="stack-list">
          <div className="card-row">
            <strong>{t("reminders_channels_title")}</strong>
            <div className="pill-row">
              <button
                type="button"
                className={`pill-button${settings.email ? " pill-button--active" : ""}`}
                onClick={() => setSettings((current) => ({ ...current, email: !current.email }))}
              >
                Email
              </button>
              <button
                type="button"
                className={`pill-button${settings.browser ? " pill-button--active" : ""}`}
                onClick={() => setSettings((current) => ({ ...current, browser: !current.browser }))}
              >
                Browser
              </button>
              <button
                type="button"
                className={`pill-button${settings.digest ? " pill-button--active" : ""}`}
                onClick={() => setSettings((current) => ({ ...current, digest: !current.digest }))}
              >
                Digest
              </button>
            </div>
          </div>

          <div className="smart-grid">
            <label className="smart-field">
              <span>{t("reminders_days_before_charge")}</span>
              <input
                className="input"
                type="number"
                min="1"
                value={settings.reminderDays}
                onChange={(event) =>
                  setSettings((current) => ({
                    ...current,
                    reminderDays: Number(event.target.value) || 1
                  }))
                }
              />
            </label>
            <label className="smart-field">
              <span>{t("reminders_price_threshold")}</span>
              <input
                className="input"
                type="number"
                min="1"
                value={settings.priceChangePercent}
                onChange={(event) =>
                  setSettings((current) => ({
                    ...current,
                    priceChangePercent: Number(event.target.value) || 1
                  }))
                }
              />
            </label>
            <label className="smart-field">
              <span>{t("reminders_inactivity_threshold")}</span>
              <input
                className="input"
                type="number"
                min="1"
                value={settings.inactivityDays}
                onChange={(event) =>
                  setSettings((current) => ({
                    ...current,
                    inactivityDays: Number(event.target.value) || 1
                  }))
                }
              />
            </label>
          </div>

          <div className="card-row">
            <strong>{t("reminders_preview_title")}</strong>
            <span>{t("reminders_channels_title")}: {[settings.email && "Email", settings.browser && "Browser", settings.digest && "Digest"].filter(Boolean).join(", ") || t("common_none")}</span>
            <span>{t("reminders_days_before_charge")}: {settings.reminderDays}</span>
            <span>{t("reminders_price_threshold")}: {settings.priceChangePercent}%</span>
            <span>{t("reminders_inactivity_threshold")}: {settings.inactivityDays}</span>
          </div>
        </div>
      </section>
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
