import { useEffect, useState } from "react";
import { dashboardApi } from "../api/dashboardApi";
import { useAuth } from "../hooks/useAuth";
import { useLanguage } from "../i18n/LanguageProvider";
import type { DashboardResponse } from "../types/analytics";
import { formatCurrency } from "../utils/formatCurrency";

const DashboardPage = () => {
  const { user } = useAuth();
  const { t } = useLanguage();
  const [data, setData] = useState<DashboardResponse | null>(null);
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(true);

  const loadDashboard = async () => {
    setError("");
    setIsLoading(true);

    try {
      const response = await dashboardApi.get("month", 7);
      setData(response);
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : "Failed to load dashboard");
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    void loadDashboard();
  }, []);

  return (
    <div className="page">
      <h1 className="page__title">{t("dashboard_title")}</h1>
      <p className="muted">
        {t("dashboard_logged_in")} {user?.email ?? "..."}
      </p>

      {error ? <p className="form-message form-message--error">{error}</p> : null}

      <section className="section">
        <h2 className="section__title">{t("dashboard_summary")}</h2>
        <p>
          {t("dashboard_total")}:{" "}
          {data
            ? formatCurrency(data.summary.totalAmount, data.summary.currency)
            : isLoading
              ? t("common_loading")
              : "0"}
        </p>
        <p>{t("dashboard_active")}: {data?.summary.activeSubscriptions ?? (isLoading ? "..." : 0)}</p>
        <p>
          {t("dashboard_period")}:{" "}
          {data ? `${data.summary.from} - ${data.summary.to}` : isLoading ? t("common_loading") : "-"}
        </p>
      </section>

      <section className="section">
        <h2 className="section__title">{t("dashboard_forecast")}</h2>
        <p>
          {t("dashboard_month")}:{" "}
          {data ? formatCurrency(data.forecast.monthForecast, data.forecast.currency) : isLoading ? "..." : "0"}
        </p>
        <p>
          {t("dashboard_year")}:{" "}
          {data ? formatCurrency(data.forecast.yearForecast, data.forecast.currency) : isLoading ? "..." : "0"}
        </p>
      </section>

      <section className="section">
        <h2 className="section__title">{t("dashboard_upcoming")}</h2>
        {(data?.upcoming?.length ?? 0) > 0 ? (
          <div className="stack-list">
            {(data?.upcoming ?? []).map((item) => (
              <div key={item.id} className="card-row">
                <strong>{item.serviceName}</strong>
                <span>
                  {formatCurrency(item.amount, item.currency)} - {item.nextBillingDate}
                </span>
              </div>
            ))}
          </div>
        ) : (
          <p>{isLoading ? t("common_loading") : t("dashboard_no_upcoming")}</p>
        )}
      </section>

      <section className="section">
        <h2 className="section__title">{t("dashboard_notifications")}</h2>
        {(data?.notifications?.length ?? 0) > 0 ? (
          <div className="stack-list">
            {(data?.notifications ?? []).map((item) => (
              <div key={item.id} className="card-row">
                <strong>{item.type}</strong>
                <span>{item.message}</span>
              </div>
            ))}
          </div>
        ) : (
          <p>{isLoading ? t("common_loading") : t("dashboard_no_notifications")}</p>
        )}
      </section>

      <button type="button" className="text-action text-action--center" onClick={() => void loadDashboard()}>
        {t("dashboard_refresh")}
      </button>
    </div>
  );
};

export default DashboardPage;
