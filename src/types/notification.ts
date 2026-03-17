export interface NotificationItem {
  id: number;
  subscriptionId?: number | null;
  type: string;
  message: string;
  scheduledAt: string;
  status: string;
}
