import type { AnalyticsByCategory, AnalyticsForecast, AnalyticsSummary } from "./analytics";
import type { NotificationItem } from "./notification";
import type { UpcomingSubscription } from "./subscription";

export interface DashboardResponse {
  summary: AnalyticsSummary;
  forecast: AnalyticsForecast;
  byCategory: AnalyticsByCategory;
  upcoming: UpcomingSubscription[];
  notifications: NotificationItem[];
}
