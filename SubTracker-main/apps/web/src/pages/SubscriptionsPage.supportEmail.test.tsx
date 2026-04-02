import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { categoryApi } from "../api/categoryApi";
import { notificationApi } from "../api/notificationApi";
import { recommendationApi } from "../api/recommendationApi";
import { subscriptionApi } from "../api/subscriptionApi";
import { LanguageProvider } from "../i18n/LanguageProvider";
import SubscriptionsPage from "./SubscriptionsPage";

vi.mock("../api/categoryApi", () => ({
  categoryApi: {
    list: vi.fn()
  }
}));

vi.mock("../api/subscriptionApi", () => ({
  subscriptionApi: {
    list: vi.fn(),
    upcoming: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    remove: vi.fn(),
    getSupportEmailDraft: vi.fn(),
    trackSupportEmailEvent: vi.fn()
  }
}));

vi.mock("../api/notificationApi", () => ({
  notificationApi: {
    list: vi.fn()
  }
}));

vi.mock("../api/recommendationApi", () => ({
  recommendationApi: {
    listByCategory: vi.fn()
  }
}));

const mockedCategoryApi = vi.mocked(categoryApi);
const mockedSubscriptionApi = vi.mocked(subscriptionApi);
const mockedNotificationApi = vi.mocked(notificationApi);
const mockedRecommendationApi = vi.mocked(recommendationApi);

describe("SubscriptionsPage support email flow", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    vi.clearAllMocks();
    window.localStorage.setItem("subtrack.language", "EN");

    mockedCategoryApi.list.mockResolvedValue([{ id: 1, name: "Entertainment" }]);
    mockedSubscriptionApi.list.mockResolvedValue([
      {
        id: 7,
        serviceName: "Netflix",
        category: "Entertainment",
        amount: 9.99,
        currency: "USD",
        billingPeriod: "MONTHLY",
        nextBillingDate: "2026-03-28",
        status: "ACTIVE"
      }
    ]);
    mockedSubscriptionApi.upcoming.mockResolvedValue([]);
    mockedNotificationApi.list.mockResolvedValue([]);
    mockedRecommendationApi.listByCategory.mockResolvedValue([]);
    mockedSubscriptionApi.getSupportEmailDraft.mockResolvedValue({
      subscriptionId: 7,
      action: "CANCEL",
      provider: "GMAIL",
      draft: {
        to: "support@netflix.com",
        subject: "Cancel my Netflix subscription",
        body: "Hello Support Team",
        mailtoUrl: "mailto:support@netflix.com?subject=Cancel&body=Hello",
        plainTextForCopy: "To: support@netflix.com\nSubject: Cancel my Netflix subscription\n\nHello Support Team"
      }
    });
    mockedSubscriptionApi.trackSupportEmailEvent.mockResolvedValue(undefined);
  });

  const renderPage = () =>
    render(
      <LanguageProvider>
        <SubscriptionsPage />
      </LanguageProvider>
    );

  it("renders quick support buttons on subscription cards", async () => {
    renderPage();

    expect(await screen.findByRole("button", { name: "Quick cancel" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Quick pause" })).toBeInTheDocument();
  });

  it("opens support draft, copies text and opens mailto client", async () => {
    const user = userEvent.setup();
    const openSpy = vi.spyOn(window, "open").mockReturnValue(null);
    const writeText = vi.fn().mockResolvedValue(undefined);
    Object.defineProperty(navigator, "clipboard", {
      configurable: true,
      value: { writeText }
    });

    renderPage();

    await user.click(await screen.findByRole("button", { name: "Quick cancel" }));

    expect(await screen.findByRole("dialog", { name: "Support email draft" })).toBeInTheDocument();
    expect(screen.getByDisplayValue("support@netflix.com")).toBeInTheDocument();
    expect(screen.getByDisplayValue("Cancel my Netflix subscription")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Copy email text" }));
    expect(writeText).toHaveBeenCalled();
    expect(await screen.findByText("Email text copied.")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Open mail client" }));
    expect(openSpy).toHaveBeenCalledWith("mailto:support@netflix.com?subject=Cancel&body=Hello", "_self");

    await waitFor(() => {
      expect(mockedSubscriptionApi.trackSupportEmailEvent).toHaveBeenCalledWith(7, "CANCEL", "DRAFT_OPENED");
      expect(mockedSubscriptionApi.trackSupportEmailEvent).toHaveBeenCalledWith(7, "CANCEL", "TEXT_COPIED");
      expect(mockedSubscriptionApi.trackSupportEmailEvent).toHaveBeenCalledWith(7, "CANCEL", "MAILTO_OPENED");
    });
  });

  it("shows API error if draft cannot be loaded", async () => {
    const user = userEvent.setup();
    mockedSubscriptionApi.getSupportEmailDraft.mockRejectedValueOnce(new Error("Draft failed"));

    renderPage();

    await user.click(await screen.findByRole("button", { name: "Quick pause" }));

    expect(await screen.findByText("Draft failed")).toBeInTheDocument();
  });
});
