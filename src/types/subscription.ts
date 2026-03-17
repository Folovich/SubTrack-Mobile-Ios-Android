export type BillingPeriod = "WEEKLY" | "MONTHLY" | "QUARTERLY" | "YEARLY";
export type SubscriptionStatus = "ACTIVE" | "PAUSED" | "CANCELED";

export interface SubscriptionRequest {
  serviceName: string;
  categoryId?: number | null;
  amount: number;
  currency: string;
  billingPeriod: BillingPeriod;
  nextBillingDate: string;
  status?: SubscriptionStatus;
}

export interface Subscription {
  id: number;
  serviceName: string;
  category?: string | null;
  amount: number;
  currency: string;
  billingPeriod: BillingPeriod;
  nextBillingDate: string;
  status: SubscriptionStatus;
}

export interface UpcomingSubscription extends Subscription {
  daysUntilBilling: number;
}
