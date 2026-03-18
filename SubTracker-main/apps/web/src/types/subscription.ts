export type BillingPeriod = "WEEKLY" | "MONTHLY" | "QUARTERLY" | "YEARLY";

export type SubscriptionStatus = "ACTIVE" | "PAUSED" | "CANCELED";
export type SupportEmailAction = "CANCEL" | "PAUSE";
export type SupportEmailEvent = "DRAFT_OPENED" | "MAILTO_OPENED" | "TEXT_COPIED";

export interface Subscription {
  id: number;
  serviceName: string;
  category: string | null;
  amount: number;
  currency: string;
  billingPeriod: BillingPeriod;
  nextBillingDate: string;
  status: SubscriptionStatus;
}

export interface UpcomingSubscription extends Subscription {
  daysUntilBilling: number;
}

export interface SubscriptionPayload {
  serviceName: string;
  categoryId: number | null;
  amount: number;
  currency: string;
  billingPeriod: BillingPeriod;
  nextBillingDate: string;
  status: SubscriptionStatus;
}

export interface SupportEmailDraftDetails {
  to: string;
  subject: string;
  body: string;
  mailtoUrl: string;
  plainTextForCopy: string;
}

export interface SupportEmailDraftResponse {
  subscriptionId: number;
  action: SupportEmailAction;
  provider: string;
  draft: SupportEmailDraftDetails;
}
