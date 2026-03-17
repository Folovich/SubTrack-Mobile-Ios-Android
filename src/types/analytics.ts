export type AnalyticsPeriod = "month" | "year";

export interface AnalyticsSummary {
  period: AnalyticsPeriod;
  from: string;
  to: string;
  totalAmount: number;
  activeSubscriptions: number;
  currency?: string | null;
}

export interface AnalyticsCategoryItem {
  category: string;
  amount: number;
  sharePercent: number;
  subscriptionsCount: number;
}

export interface AnalyticsByCategory {
  period: AnalyticsPeriod;
  from: string;
  to: string;
  totalAmount: number;
  currency?: string | null;
  items: AnalyticsCategoryItem[];
}

export interface AnalyticsForecast {
  monthForecast: number;
  yearForecast: number;
  currency?: string | null;
}
