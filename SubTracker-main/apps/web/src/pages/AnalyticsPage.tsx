import { useEffect, useState } from "react";
import { analyticsApi } from "../api/analyticsApi";
import { usageInsightsApi } from "../api/usageInsightsApi";
import { useLanguage } from "../i18n/LanguageProvider";
import type {
  AnalyticsByCategory,
  AnalyticsForecast,
  AnalyticsPeriod,
  AnalyticsSummary
} from "../types/analytics";
import type { UsageInsights } from "../types/usageInsights";
import { formatCurrency } from "../utils/formatCurrency";

const PERIODS: AnalyticsPeriod[] = ["month", "year"];
const CHART_TYPES = ["bar", "line"] as const;
type ChartType = (typeof CHART_TYPES)[number];

const AnalyticsPage = () => {
  const { t } = useLanguage();
  const [period, setPeriod] = useState<AnalyticsPeriod>("month");
  const [summary, setSummary] = useState<AnalyticsSummary | null>(null);
  const [byCategory, setByCategory] = useState<AnalyticsByCategory | null>(null);
  const [forecast, setForecast] = useState<AnalyticsForecast | null>(null);
  const [usageInsights, setUsageInsights] = useState<UsageInsights | null>(null);
  const [chartType, setChartType] = useState<ChartType>("bar");
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const loadAnalytics = async () => {
      setError("");
      setIsLoading(true);

      try {
        const [summaryResponse, byCategoryResponse, forecastResponse, usageInsightsResponse] =
          await Promise.all([
          analyticsApi.summary(period),
          analyticsApi.byCategory(period),
          analyticsApi.forecast(),
          usageInsightsApi.get()
        ]);

        setSummary(summaryResponse);
        setByCategory(byCategoryResponse);
        setForecast(forecastResponse);
        setUsageInsights(usageInsightsResponse);
      } catch (loadError) {
        setError(loadError instanceof Error ? loadError.message : "Failed to load analytics");
      } finally {
        setIsLoading(false);
      }
    };

    void loadAnalytics();
  }, [period]);

  return (
    <div className="page">
      <h1 className="page__title">{t("analytics_title")}</h1>
      <div className="pill-row pill-row--links">
        {PERIODS.map((value) => (
          <button
            key={value}
            type="button"
            className={`link-tab${period === value ? " link-tab--active" : ""}`}
            onClick={() => setPeriod(value)}
          >
            {value === "month" ? t("dashboard_month") : t("dashboard_year")}
          </button>
        ))}
      </div>

      {error ? <p className="form-message form-message--error">{error}</p> : null}

      <section className="section">
        <h2 className="section__title">{t("analytics_usage_title")}</h2>
        <div className="usage-metrics">
          <div className="card-row">
            <strong>{usageInsights?.metrics.totalSignals ?? (isLoading ? "..." : 0)}</strong>
            <span>{t("analytics_usage_metric_total_signals")}</span>
          </div>
          <div className="card-row">
            <strong>{usageInsights?.metrics.activeSubscriptions ?? (isLoading ? "..." : 0)}</strong>
            <span>{t("analytics_usage_metric_active_subscriptions")}</span>
          </div>
          <div className="card-row">
            <strong>
              {usageInsights
                ? usageInsights.metrics.averageSignalsPerSubscription.toFixed(1)
                : isLoading
                  ? "..."
                  : "0.0"}
            </strong>
            <span>{t("analytics_usage_metric_avg_signals")}</span>
          </div>
        </div>

        <div className="pill-row">
          {CHART_TYPES.map((type) => (
            <button
              key={type}
              type="button"
              className={`pill-button${chartType === type ? " pill-button--active" : ""}`}
              onClick={() => setChartType(type)}
            >
              {type === "bar" ? t("analytics_usage_chart_bar") : t("analytics_usage_chart_line")}
            </button>
          ))}
        </div>

        {usageInsights?.subscriptions.length ? (
          chartType === "bar" ? (
            <div className="usage-chart usage-chart--bar">
              {usageInsights.subscriptions.map((item) => (
                <div key={item.id} className="usage-bar-row">
                  <span className="usage-bar-row__label">{item.serviceName}</span>
                  <div className="usage-bar-row__track">
                    <div
                      className="usage-bar-row__fill"
                      style={{
                        width: `${Math.max(
                          8,
                          (item.signalsCount /
                            Math.max(...usageInsights.subscriptions.map((entry) => entry.signalsCount), 1)) *
                            100
                        )}%`
                      }}
                    />
                  </div>
                  <span className="usage-bar-row__value">{item.signalsCount}</span>
                </div>
              ))}
            </div>
          ) : (
            <div className="usage-chart usage-chart--line">
              {usageInsights.subscriptions.map((item) => (
                <div key={item.id} className="usage-line-row">
                  <span>{item.serviceName}</span>
                  <div className="usage-line-row__dots">
                    <span className="usage-line-row__dot" />
                    <span>{item.signalsCount}</span>
                  </div>
                </div>
              ))}
            </div>
          )
        ) : (
          <p>{isLoading ? t("common_loading") : t("analytics_usage_empty")}</p>
        )}

        {usageInsights?.subscriptions.length ? (
          <div className="stack-list">
            {usageInsights.subscriptions.map((item) => (
              <div key={`usage-${item.id}`} className="card-row">
                <strong>{item.serviceName}</strong>
                <span>
                  {t("analytics_usage_list_signals")}: {item.signalsCount} ·{" "}
                  {t("analytics_usage_list_last_signal")}: {item.lastSignalAt ?? t("common_none")}
                </span>
              </div>
            ))}
          </div>
        ) : null}
      </section>

      <section className="section">
        <h2 className="section__title">
          {t("analytics_summary")} ({period === "month" ? t("dashboard_month") : t("dashboard_year")})
        </h2>
        <p>
          {t("dashboard_total")}:{" "}
          {summary ? formatCurrency(summary.totalAmount, summary.currency) : isLoading ? "..." : "0"}
        </p>
        <p>{t("dashboard_active")}: {summary?.activeSubscriptions ?? (isLoading ? "..." : 0)}</p>
        <p>{t("dashboard_period")}: {summary ? `${summary.from} - ${summary.to}` : isLoading ? t("common_loading") : "-"}</p>
      </section>

      <section className="section">
        <h2 className="section__title">{t("analytics_by_category")}</h2>
        {byCategory?.items.length ? (
          <div className="stack-list">
            {byCategory.items.map((item) => (
              <div key={item.category} className="card-row">
                <strong>{item.category}</strong>
                <span>
                  {formatCurrency(item.amount, byCategory.currency)} · {item.sharePercent}% ·{" "}
                  {item.subscriptionsCount}
                </span>
              </div>
            ))}
          </div>
        ) : (
          <p>{isLoading ? t("common_loading") : t("analytics_empty_categories")}</p>
        )}
      </section>

      <section className="section">
        <h2 className="section__title">{t("analytics_forecast")}</h2>
        <p>
          {t("dashboard_month")}:{" "}
          {forecast ? formatCurrency(forecast.monthForecast, forecast.currency) : isLoading ? "..." : "0"}
        </p>
        <p>
          {t("dashboard_year")}:{" "}
          {forecast ? formatCurrency(forecast.yearForecast, forecast.currency) : isLoading ? "..." : "0"}
        </p>
      </section>
    </div>
  );
};

export default AnalyticsPage;
