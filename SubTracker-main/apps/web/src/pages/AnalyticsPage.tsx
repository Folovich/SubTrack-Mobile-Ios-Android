import { useEffect, useState } from "react";
import { analyticsApi } from "../api/analyticsApi";
import ForecastChart from "../components/analytics/ForecastChart";
import { subscriptionApi } from "../api/subscriptionApi";
import { usageInsightsApi } from "../api/usageInsightsApi";
import ExpenseChart from "../components/analytics/ExpenseChart";
import { useLanguage } from "../i18n/LanguageProvider";
import type {
  AnalyticsByCategory,
  AnalyticsForecast,
  AnalyticsPeriod,
  AnalyticsSummary,
  AnalyticsCategoryItem
} from "../types/analytics";
import type { Subscription } from "../types/subscription";
import type { AnalyticsUsageResponse } from "../types/usageSignal";
import { formatCurrency } from "../utils/formatCurrency";

const PERIODS: AnalyticsPeriod[] = ["month", "year"];
const CHART_TYPES = ["bar", "line"] as const;
const ANALYTICS_VISUALS = ["categories", "services", "forecast"] as const;
type ChartType = (typeof CHART_TYPES)[number];
type AnalyticsVisual = (typeof ANALYTICS_VISUALS)[number];

const getPeriodMultiplier = (billingPeriod: Subscription["billingPeriod"], period: AnalyticsPeriod) => {
  if (period === "month") {
    switch (billingPeriod) {
      case "WEEKLY":
        return 4;
      case "MONTHLY":
        return 1;
      case "QUARTERLY":
        return 1 / 3;
      case "YEARLY":
        return 1 / 12;
    }
  }

  switch (billingPeriod) {
    case "WEEKLY":
      return 52;
    case "MONTHLY":
      return 12;
    case "QUARTERLY":
      return 4;
    case "YEARLY":
      return 1;
  }
};

const buildServiceAnalytics = (
  subscriptions: Subscription[],
  period: AnalyticsPeriod
): { currency: string | null; items: AnalyticsCategoryItem[] } => {
  const activeSubscriptions = subscriptions.filter((item) => item.status === "ACTIVE");
  const groups = new Map<string, { amount: number; subscriptionsCount: number }>();

  activeSubscriptions.forEach((item) => {
    const current = groups.get(item.serviceName) ?? { amount: 0, subscriptionsCount: 0 };
    groups.set(item.serviceName, {
      amount: current.amount + item.amount * getPeriodMultiplier(item.billingPeriod, period),
      subscriptionsCount: current.subscriptionsCount + 1
    });
  });

  const items = Array.from(groups.entries())
    .map(([serviceName, value]) => ({
      category: serviceName,
      amount: Number(value.amount.toFixed(2)),
      sharePercent: 0,
      subscriptionsCount: value.subscriptionsCount
    }))
    .sort((left, right) => right.amount - left.amount);

  const total = items.reduce((sum, item) => sum + item.amount, 0);

  return {
    currency: activeSubscriptions[0]?.currency ?? "USD",
    items: items.map((item) => ({
      ...item,
      sharePercent: total > 0 ? Number(((item.amount / total) * 100).toFixed(2)) : 0
    }))
  };
};

const AnalyticsPage = () => {
  const { t } = useLanguage();
  const [period, setPeriod] = useState<AnalyticsPeriod>("month");
  const [summary, setSummary] = useState<AnalyticsSummary | null>(null);
  const [byCategory, setByCategory] = useState<AnalyticsByCategory | null>(null);
  const [forecast, setForecast] = useState<AnalyticsForecast | null>(null);
  const [usageInsights, setUsageInsights] = useState<AnalyticsUsageResponse | null>(null);
  const [serviceAnalytics, setServiceAnalytics] = useState<{ currency: string | null; items: AnalyticsCategoryItem[] }>({
    currency: null,
    items: []
  });
  const [chartType, setChartType] = useState<ChartType>("bar");
  const [analyticsVisual, setAnalyticsVisual] = useState<AnalyticsVisual>("categories");
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const loadAnalytics = async () => {
      setError("");
      setIsLoading(true);

      try {
        const [summaryResponse, byCategoryResponse, forecastResponse, usageInsightsResponse, subscriptionsResponse] =
          await Promise.all([
            analyticsApi.summary(period),
            analyticsApi.byCategory(period),
            analyticsApi.forecast(),
            usageInsightsApi.get(period),
            subscriptionApi.list()
          ]);

        setSummary(summaryResponse);
        setByCategory(byCategoryResponse);
        setForecast(forecastResponse);
        setUsageInsights(usageInsightsResponse);
        setServiceAnalytics(buildServiceAnalytics(subscriptionsResponse, period));
      } catch (loadError) {
        setSummary(null);
        setByCategory(null);
        setForecast(null);
        setUsageInsights(null);
        setServiceAnalytics({ currency: null, items: [] });
        setError(loadError instanceof Error ? loadError.message : t("common_error"));
      } finally {
        setIsLoading(false);
      }
    };

    void loadAnalytics();
  }, [period]);

  const averageSignalsPerSubscription = usageInsights
    ? usageInsights.totalSignals / Math.max(usageInsights.subscriptionsWithSignals, 1)
    : 0;

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
            <strong>{usageInsights?.totalSignals ?? (isLoading ? "..." : 0)}</strong>
            <span>{t("analytics_usage_metric_total_signals")}</span>
          </div>
          <div className="card-row">
            <strong>{usageInsights?.activeSubscriptions ?? (isLoading ? "..." : 0)}</strong>
            <span>{t("analytics_usage_metric_active_subscriptions")}</span>
          </div>
          <div className="card-row">
            <strong>{usageInsights ? averageSignalsPerSubscription.toFixed(1) : isLoading ? "..." : "0.0"}</strong>
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

        {usageInsights?.items.length ? (
          chartType === "bar" ? (
            <div className="usage-chart usage-chart--bar">
              {usageInsights.items.map((item) => (
                <div key={item.subscriptionId} className="usage-bar-row">
                  <span className="usage-bar-row__label">{item.serviceName}</span>
                  <div className="usage-bar-row__track">
                    <div
                      className="usage-bar-row__fill"
                      style={{
                        width: `${Math.max(
                          8,
                          (item.signalsCount /
                            Math.max(...usageInsights.items.map((entry) => entry.signalsCount), 1)) *
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
              {usageInsights.items.map((item) => (
                <div key={item.subscriptionId} className="usage-line-row">
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

        {usageInsights?.items.length ? (
          <details className="usage-accordion">
            <summary className="usage-accordion__summary">
              <div className="usage-accordion__summary-main">
                <span className="usage-accordion__chevron" aria-hidden="true">
                  <svg viewBox="0 0 20 20" fill="none">
                    <path
                      d="m6 8 4 4 4-4"
                      stroke="currentColor"
                      strokeWidth="1.8"
                      strokeLinecap="round"
                      strokeLinejoin="round"
                    />
                  </svg>
                </span>
                <strong>{t("analytics_usage_metric_subscriptions_with_signals")}</strong>
              </div>
              <span className="usage-accordion__meta">{usageInsights.items.length}</span>
            </summary>
            <div className="stack-list usage-accordion__list">
              {usageInsights.items.map((item) => (
                <div key={`usage-${item.subscriptionId}`} className="card-row">
                  <strong>{item.serviceName}</strong>
                  <span>
                    {t("analytics_usage_list_signals")}: {item.signalsCount} ·{" "}
                    {t("analytics_usage_list_last_signal")}: {item.lastSignalAt ?? t("common_none")}
                  </span>
                  <span>
                    {t("analytics_usage_metric_subscriptions_with_signals")}: {usageInsights.subscriptionsWithSignals}
                    {" · "}
                    {usageInsights.from} - {usageInsights.to}
                  </span>
                </div>
              ))}
            </div>
          </details>
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
        <p>
          {t("dashboard_period")}:{" "}
          {summary ? `${summary.from} - ${summary.to}` : isLoading ? t("common_loading") : "-"}
        </p>
      </section>

      <section className="section">
        <h2 className="section__title">{t("analytics_visual_title")}</h2>
        <div className="pill-row">
          <button
            type="button"
            className={`pill-button${analyticsVisual === "categories" ? " pill-button--active" : ""}`}
            onClick={() => setAnalyticsVisual("categories")}
          >
            {t("analytics_by_category")}
          </button>
          <button
            type="button"
            className={`pill-button${analyticsVisual === "services" ? " pill-button--active" : ""}`}
            onClick={() => setAnalyticsVisual("services")}
          >
            {t("analytics_by_service")}
          </button>
          <button
            type="button"
            className={`pill-button${analyticsVisual === "forecast" ? " pill-button--active" : ""}`}
            onClick={() => setAnalyticsVisual("forecast")}
          >
            {t("analytics_forecast")}
          </button>
        </div>

        {analyticsVisual === "categories" ? (
          byCategory?.items.length ? (
            <>
              <ExpenseChart items={byCategory.items} currency={byCategory.currency} />
              <details className="usage-accordion">
                <summary className="usage-accordion__summary">
                  <div className="usage-accordion__summary-main">
                    <span className="usage-accordion__chevron" aria-hidden="true">
                      <svg viewBox="0 0 20 20" fill="none">
                        <path
                          d="m6 8 4 4 4-4"
                          stroke="currentColor"
                          strokeWidth="1.8"
                          strokeLinecap="round"
                          strokeLinejoin="round"
                        />
                      </svg>
                    </span>
                    <strong>{t("analytics_by_category")}</strong>
                  </div>
                  <span className="usage-accordion__meta">{byCategory.items.length}</span>
                </summary>
                <div className="stack-list usage-accordion__list">
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
              </details>
            </>
          ) : (
            <p>{isLoading ? t("common_loading") : t("analytics_empty_categories")}</p>
          )
        ) : analyticsVisual === "services" ? (
          serviceAnalytics.items.length ? (
            <>
              <ExpenseChart items={serviceAnalytics.items} currency={serviceAnalytics.currency} />
              <details className="usage-accordion">
                <summary className="usage-accordion__summary">
                  <div className="usage-accordion__summary-main">
                    <span className="usage-accordion__chevron" aria-hidden="true">
                      <svg viewBox="0 0 20 20" fill="none">
                        <path d="m6 8 4 4 4-4" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
                      </svg>
                    </span>
                    <strong>{t("analytics_by_service")}</strong>
                  </div>
                  <span className="usage-accordion__meta">{serviceAnalytics.items.length}</span>
                </summary>
                <div className="stack-list usage-accordion__list">
                  {serviceAnalytics.items.map((item) => (
                    <div key={item.category} className="card-row">
                      <strong>{item.category}</strong>
                      <span>
                        {formatCurrency(item.amount, serviceAnalytics.currency)} · {item.sharePercent}% ·{" "}
                        {item.subscriptionsCount}
                      </span>
                    </div>
                  ))}
                </div>
              </details>
            </>
          ) : (
            <p>{isLoading ? t("common_loading") : t("common_none")}</p>
          )
        ) : forecast ? (
          <>
            <ForecastChart
              data={forecast}
              monthLabel={t("dashboard_month")}
              yearLabel={t("dashboard_year")}
            />
            <div className="stack-list">
              <div className="card-row">
                <strong>{t("dashboard_month")}</strong>
                <span>{formatCurrency(forecast.monthForecast, forecast.currency)}</span>
              </div>
              <div className="card-row">
                <strong>{t("dashboard_year")}</strong>
                <span>{formatCurrency(forecast.yearForecast, forecast.currency)}</span>
              </div>
            </div>
          </>
        ) : (
          <p>{isLoading ? t("common_loading") : t("common_none")}</p>
        )}
      </section>
    </div>
  );
};

export default AnalyticsPage;
